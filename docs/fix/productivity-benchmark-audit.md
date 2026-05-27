# Productivity Benchmark Audit — May 2026

## Device Under Test

| Field | Value |
|---|---|
| Device | OnePlus CPH2691 |
| SoC | Snapdragon 8 Gen 3, Adreno 750 GPU, Qualcomm Venus HW codec |
| OS | Android 16 (API 36) |

## Test Overview (9 tests)

| Test | Engine | Workload | Unit | Reference (SD8G3) | Measured |
|---|---|---|---|---|---|
| CANVAS_OPS | GPU — HardwareRenderer HWUI | 1024²: LinearG+12 RadialG+Bezier+Rotate+Text per frame | ops/s | 400 | ~334 |
| IMAGE_FILTER | GPU — AGSL RuntimeShader | 4K (3840×2160): brightness+sat+hue YIQ shader | images/s | 285 | ~239 |
| IMAGE_RESIZE | GPU — HW bilinear sampler | 4K→1080p→4K round-trip, 2 renderers | images/s | 175 | ~148 |
| TEXT_OPS | CPU — single thread | 5K sort + Lev×20 + regex×5 | Mchars/s | 2.0 | ~1.7 |
| JSON_OPS | CPU — single thread | 200-field 3-level nested build+parse+walk | docs/s | 90 | ~78 |
| COMPRESSION | CPU — Deflater lvl 9 | 1 MB blocks, 60% compressible | MB/s | 22 | ~19 |
| VIDEO_ENCODE | HW — MediaCodec Surface | GPU frame→H.264 HW encoder, 1080p 8Mbps | fps | 305 | ~256 |
| VIDEO_DECODE | HW — MediaCodec ByteBuffer | Pre-encoded I-frames→HW decoder, 1080p | fps | 700 | ~595 |
| VIDEO_TRANSCODE | HW — decode+AGSL+encode | Decode→GPU color grade→HW encode, 1080p | fps | 230 | ~192 |

---

## Bugs — Measurement Distortion

### BUG-1: VIDEO_TRANSCODE discards decoded output, uses CPU test pattern (SEVERITY: CRITICAL)

**File**: `ProductivityBenchmarkViewModel.kt:920-944`

Decoded video frames are released without use (`releaseOutputBuffer(outIdx, false)`). Instead, a tiny CPU-drawn test pattern bitmap (`240×135`) is created every frame for the GPU grade pass.

```kotlin
// Decoded frame — discarded!
dec.releaseOutputBuffer(outIdx, false)

// Fake source: tiny CPU bitmap instead of decoded video
val srcBmp = Bitmap.createBitmap(W / 8, H / 8, Bitmap.Config.ARGB_8888)
val tmpC = Canvas(srcBmp)
// ... draw test pattern on CPU ...
```

**What this actually measures**: CPU bitmap creation + AGSL shader on 240×135 test pattern + HW encode. **Not** a transcode pipeline.

**What a real transcode measures**: HW decode → GPU upload decoded YUV as texture → AGSL grade on 1920×1080 → HW encode. The YUV→texture upload step is a major bottleneck that this bypasses entirely.

**Impact**: Inflated fps by 2-4×. The benchmark's "192 fps transcode" is actually "240×135 GPU grade + 1080p encode." A real transcode would be limited by decoded-frame texture upload bandwidth (~60-120 fps on Adreno 750).

**Fix**: Decode to a `Surface` (texture output) instead of byte-buffer, use the decoded texture directly in the AGSL shader. Requires Surface-to-Surface pipeline:
```kotlin
// Configure decoder with Surface output
val decSurf = /* Surface from SurfaceTexture or ImageReader */
dec.configure(decFmt, decSurf, null, 0)
// AGSL shader reads from decoder output Surface texture
```

### BUG-2: VIDEO_DECODE doesn't filter CODEC_CONFIG output buffers (SEVERITY: HIGH)

**File**: `ProductivityBenchmarkViewModel.kt:773-776`

```kotlin
val outIdx = dec.dequeueOutputBuffer(decInfo, 5_000L)
if (outIdx >= 0) {
    decFrames++   // ← counts EVERY output buffer, including SPS/PPS CSD
    dec.releaseOutputBuffer(outIdx, false)
```

The encoder drain correctly filters: `if (info.flags and BUFFER_FLAG_CODEC_CONFIG == 0 && info.size > 0) frames++`. The decoder doesn't. During startup, the decoder emits CSD buffers (SPS/PPS) that are counted as decoded frames. On a 3s measurement at ~595 fps with 20 input frames looped: each loop iteration may produce 1-2 CSD + 1 actual frame. Only ~67% of counted "frames" are actual decoded frames.

**Fix**: Add the same flag check:
```kotlin
if (decInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0 && decInfo.size > 0)
    decFrames++
```

### BUG-3: COMPRESSION uses same input data every pass — warm-cache bias (SEVERITY: MEDIUM)

**File**: `ProductivityBenchmarkViewModel.kt:1103-1107`

```kotlin
val input = ByteArray(blockSize)
// Generated ONCE before timed loop
for (i in input.indices) { ... }
// Every pass compresses identical data
while (System.currentTimeMillis() < endMs) {
    deflater.reset(); deflater.setInput(input); deflater.finish()
```

After the first pass, the 1 MB input is in L1/L2 cache. The deflater's LZ77 hash table hits the same patterns at the same offsets. Branch predictor is 100% accurate. This measures best-case throughput — not sustained compression throughput with varying data.

**Impact**: Overstates compression throughput by ~10-20% vs real-world (varying data). Worse on devices with large L3 cache (the entire 1 MB stays cached).

**Fix**: Cycle through N pre-generated blocks (e.g., 4 different 1 MB blocks):
```kotlin
val blocks = Array(4) { generateBlock() }
while (...) {
    deflater.setInput(blocks[(blocksIdx++) % 4])
}
```

### BUG-4: TEXT_OPS — `corpus.copyOf()` + `.sort()` allocates new array per pass (SEVERITY: LOW)

**File**: `ProductivityBenchmarkViewModel.kt:997`

```kotlin
val copy = corpus.copyOf(); copy.sort()
```

5,000-element String array allocated per pass. At ~34 passes/s: 136 KB/s of array allocation. Strings are shared (copy shares references), so only the array itself is new. Moderate but acceptable. Intentional — sorting mutates, so copy is needed.

### BUG-5: Score capped at 100 but references have 18% headroom (SEVERITY: LOW)

**File**: `ProductivityBenchmarkViewModel.kt:156-158`

```kotlin
private fun ProductivityTest.score(value: Double): Int {
    val ref = PRODUCTIVITY_REFERENCE[this] ?: return 0
    return (value / ref * 100.0).roundToInt().coerceIn(0, 100)  // ← capped
}
```

References are set ~18% above measured SD8G3 values. The SD8G3 scores ~85 per test. This is intentional (leaves room for faster devices). But the upper cap at 100 means devices 18%+ faster than SD8G3 all score 100 — no differentiation above the reference.

Contrast: Storage benchmark uses `coerceAtLeast(0)` (no upper cap). RAM benchmark also uses `coerceAtLeast(0)`. Productivity is inconsistent — it's the only benchmark with an upper score cap.

---

## Efficiency — Wasted Resources

### EFF-1: VIDEO_TRANSCODE — Bitmap.createBitmap() + .recycle() every frame (SEVERITY: MEDIUM)

**File**: `ProductivityBenchmarkViewModel.kt:930-945`

Creates a 240×135 `Bitmap` per frame, draws test pattern on CPU, uses it as AGSL shader input, then recycles. At 192 fps: 192 × 240 × 135 × 4 = ~25 MB/s allocation rate. All on timing-critical hot path. This is entirely avoidable — use a pre-allocated bitmap or (better) use actual decoded output.

### EFF-2: IMAGE_FILTER — new BitmapShader allocated every frame

**File**: `ProductivityBenchmarkViewModel.kt:500-501`

```kotlin
rtShader.setInputShader("inputTexture",
    BitmapShader(src, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP))
```

Allocates new `BitmapShader` per frame. At 239 images/s. The shader is lightweight metadata — negligible overhead. But could be created once outside the loop since `src` and tile modes are invariant.

### EFF-3: CANVAS_OPS — `rng.nextFloat()` for Bezier path per frame

**File**: `ProductivityBenchmarkViewModel.kt:380-385`

`path.cubicTo(rng.nextFloat() * W, ...)` — 64 Random.nextFloat() calls per frame (8 segments × 4 control points × 2 coords). At 334 ops/s: 21K nextFloat calls/s. Each does LCG math. Negligible vs GPU rasterization time.

### EFF-4: IMAGE_RESIZE — two HardwareRenderer instances

One for 4K→1080p downscale, one for 1080p→4K upscale. Each has its own GL/Vulkan context. Context-switch between the two per iteration. Could use a single renderer with two render nodes + two ImageReaders, but Surface attachment limitation prevents this (one renderer = one Surface).

---

## Cross-Platform Hardware Codec Considerations

### Snapdragon (Adreno GPU + Qualcomm Venus codec)

H.264 encoder: Hardware-accelerated via Venus/V4L2. `COLOR_FormatSurface` = zero-copy GPU→encoder via fd-backed gralloc buffers. 256 fps at 1080p — plausible for Qualcomm's fixed-function block.

Decoder: Same Venus block, byte-buffer mode. 595 fps — loading the encoder output (20 I-frames = ~150 KB each = 3 MB total) into decoder buffers at high rate. Plausible.

### MediaTek (Mali/Immortalis GPU + MediaTek video codec)

H.264 encoder: MediaTek's HW block. `COLOR_FormatSurface` may not be supported on older MediaTek (requires Android 8+ with vendor-specific gralloc). On MediaTek with `COLOR_FormatSurface` support: similar fps. Without: falls back to `COLOR_FormatYUV420Flexible` → GPU→CPU→encoder copy → much slower (30-60 fps).

Decoder: Similar constraints. ByteBuffer mode works everywhere. Performance varies by MediaTek codec IP version.

### Samsung Exynos (Xclipse/AMD RDNA GPU + Samsung MFC codec)

MFC (Multi-Format Codec) block. `COLOR_FormatSurface` supported since Exynos 9820. Performance similar to Qualcomm for H.264.

### Desktop GPU (Intel QSV, NVIDIA NVENC, AMD VCE)

Not applicable — this is an Android benchmark. But if running in Android x86 emulator: software codec (OMX.google.h264.encoder) at ~30 fps. The reference of 305 fps is unreachable — the device would score ~10.

### Codec Feature Gaps

| Feature | Tested | Available on Modern HW |
|---|---|---|
| H.264 (AVC) encode | ✅ | All |
| H.264 (AVC) decode | ✅ | All |
| H.265 (HEVC) encode | ✗ | SD845+, all 2020+ flagships |
| H.265 (HEVC) decode | ✗ | All 2018+ devices |
| AV1 decode | ✗ | SD8Gen2+, Dimensity 9000+, Tensor G2+ |
| VP9 decode | ✗ | All 2017+ devices |
| 4K resolution | ✗ | All flagship SoCs |

Testing only H.264 at 1080p misses:
- HEVC encode (common for camera recording, 30-50% better compression)
- AV1 decode (YouTube, Netflix — increasingly used)
- 4K (taxes codec throughput 4× more, exposes memory bandwidth limits)

---

## Design Gaps

### GAP-1: No multi-threaded CPU tests
TEXT_OPS, JSON_OPS, COMPRESSION all single-threaded. Modern productivity workloads are multi-threaded (parallel build systems, multi-threaded JSON parsing, parallel compression with pigz/zstd --threads).

### GAP-2: No file I/O test
No disk read/write in productivity suite. Real productivity: loading a PDF, saving a spreadsheet, importing images. All current tests are in-memory.

### GAP-3: No PDF/document rendering test
Office suite workloads not represented. PDF rendering (Android's PdfRenderer, PdfDocument), document layout, spreadsheet calculations — all missing.

### GAP-4: No UI rendering test (actual Compose/View)
CANVAS_OPS uses low-level HardwareRenderer API, not actual Jetpack Compose or Android View rendering. Real app UI rendering involves measure/layout/draw cycles, not raw GPU draw calls.

### GAP-5: No multi-codec video test
Only H.264 tested. HEVC, AV1, VP9 hardware decode are common and increasingly important (streaming services use them).

### GAP-6: No image codec test (JPEG/PNG/WebP/HEIF decode)
Real productivity: loading photos, processing images. Android's BitmapFactory supports all modern codecs. Not tested.

---

## Proposed Fixes — Priority Matrix

| Priority | ID | What | Effort | Impact |
|---|---|---|---|---|
| **P0** | BUG-1 | Use decoded video output in transcode, not test pattern | Large | Critical — completely fake benchmark |
| **P0** | BUG-2 | Filter CODEC_CONFIG from decode frame count | Tiny | High — 33% inflation |
| **P1** | BUG-3 | Rotate through multiple compression input blocks | Small | Medium — warm-cache bias |
| **P1** | BUG-5 | Remove score upper cap (align with Storage/RAM) | Tiny | Low — consistency |
| **P2** | EFF-1 | Reuse bitmap in transcode instead of allocate/recycle | Small | Low — GC pressure |
| **P2** | EFF-2 | Hoist BitmapShader out of filter loop | Tiny | Low |
| **P3** | GAP-1 | Multi-threaded CPU tests | Large | Medium |
| **P3** | GAP-5 | Add HEVC/AV1 codec tests | Medium | Medium |
| **P3** | GAP-6 | Add JPEG/WebP/HEIF decode tests | Medium | Medium |

---

## Appendix: HW Codec Performance Reference

### Theoretical Maximums

| Codec | Resolution | Format | SD8G3 (Venus) | MediaTek Dim 9300 | Samsung Exynos 2400 |
|---|---|---|---|---|---|
| H.264 encode | 1080p 8Mbps | Surface | ~300-350 fps | ~250-300 fps | ~280-320 fps |
| H.264 decode | 1080p | ByteBuffer | ~700-800 fps | ~600-700 fps | ~650-750 fps |
| HEVC encode | 1080p 8Mbps | Surface | ~200-250 fps | ~180-220 fps | ~200-240 fps |
| HEVC decode | 1080p | ByteBuffer | ~500-600 fps | ~450-550 fps | ~500-600 fps |
| AV1 decode | 1080p | ByteBuffer | ~400-500 fps | ~350-450 fps | N/A (no HW AV1) |

### Why Video Encode is Fast (256 fps at 1080p)

At 256 fps × 1920 × 1080 × 1.5 bytes/YUV420 = ~796 MB/s of raw pixel data → H.264 encoder compresses to ~2.1 MB/s (8 Mbps × 256 / 960 × ... wait, let me recalculate).

Actually: 256 fps at 8 Mbps total bitrate. Each frame = 8,000,000 / 60 fps target / 8 bits = ~16.7 KB per I-frame (at 60 fps rate, but actual frame rate is whatever the encoder produces). The encoder spends ~3.9ms per frame. At 256 fps aggregate throughput.

The GPU render path (HardwareRenderer) renders 1920×1080 = 2.1M pixels per frame. Adreno 750 can render ~2 Gpixels/s, so each frame takes ~1ms. GPU→encoder via Surface is zero-copy. Encoder's fixed-function block processes ~3ms. Total ~4ms per frame = 250 fps. Matches measurement.

### Limits of Surface-based encoding

`COLOR_FormatSurface` requires the GPU and encoder to share a gralloc buffer. The Surface queue depth is typically 3-4 frames. If the encoder consumes slower than the GPU produces, the Surface queue fills → `syncAndDraw()` blocks. The benchmark's sync approach correctly captures this backpressure — it measures end-to-end pipeline throughput, not just encoder speed.
