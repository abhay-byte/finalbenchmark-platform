# AI Benchmark Inefficiency Review

**Date:** 2026-05-28  
**Files Reviewed:** `AiBenchmarkManager.kt`, `ModelDownloader.kt`  
**Device:** Snapdragon 8 Gen 3 / Adreno 750 / OnePlus CPH2691  
**Scope:** All AI tests — MobileNet V3, EfficientDet, MiniLM, Whisper, Gemma LLM, YOLOv8, MobileBERT, DTLN

---

## Critical Bugs & Crashes

### 1. NNAPI Delegate Leak — Memory Leak Every Inference
**File:** `AiBenchmarkManager.kt:693-704`  
`NnApiDelegate` is created but never saved to a variable and never closed. If NNAPI succeeds, the delegate leaks. Only `gpuDelegate` and `interpreter` are closed in the `finally` block (line 886-888).  
**Fix:** Store `nnApiDelegate` and close it in `finally`.

### 2. Interpreter Wastefully Recreated 3 Times per Test
**File:** `AiBenchmarkManager.kt:693-747`  
Each benchmark creates up to 3 separate `Interpreter` instances: NNAPI attempt, GPU attempt, CPU attempt. Each creation involves model parsing, memory allocation, and delegate init — O(100ms) each.  
**Fix:** Create one `Interpreter` with fallback at delegate level. TFLite supports adding multiple delegates with automatic fallback via `Interpreter.Options.addDelegate()`.

### 3. LLM Fallback — 134M Iteration Nested Loop in Kotlin
**File:** `AiBenchmarkManager.kt:383-398`  
When LlmInference fails, the synthetic fallback does `128 * 1024 * 1024 = 134,217,728` loop iterations of floating-point arithmetic in pure Kotlin. On ART (Android Runtime), this is 50-100× slower than native. The loop computes `sum += (i * j * 0.0001)` which JIT cannot vectorize.  
**Fix:** Use a single TensorFlow Lite matrix multiply model instead, or run the computation in native C++ via JNI.

### 4. LLM Token Count Estimation is Wrong
**File:** `AiBenchmarkManager.kt:353`  
Token count estimated as `response.length / 4`. Actual LLM tokenization has no fixed character-to-token ratio. LlmInference API already returns token counts — use the API.  
**Fix:** Use `LlmInference.getLastTokensCount()` or equivalent API.

---

## Performance Inefficiencies

### 5. No Interpreter Reuse Across Benchmark Suite
**File:** `AiBenchmarkManager.kt` (all `run*` functions)  
Each of the 8 benchmark tests creates its own `Interpreter` from scratch. A full benchmark run creates and destroys 8 interpreters, wasting 200-400ms on redundant model loading/parsing.  
**Fix:** Cache interpreters per model file, create once on first use, reuse across benchmarks.

### 6. No `MappedByteBuffer` for Model Loading
**File:** `AiBenchmarkManager.kt` (all model loads use `Interpreter(File)`)  
Loading `.tflite` files via `Interpreter(File)` copies the entire model into heap memory. Using `MappedByteBuffer` (memory-mapped I/O) avoids the copy and is 2-5× faster for model loading.  
**Reference:** [TFLite Android docs](https://github.com/tensorflow/tensorflow/blob/master/tensorflow/lite/g3doc/android/java.md) recommend `FileChannel.map()` for model loading.  
**Fix:** Load all models via `MappedByteBuffer` using `FileInputStream` + `FileChannel.map()`.

### 7. Random Input Generation in Hot Path — Blocks Hottest Code
**File:** `AiBenchmarkManager.kt:774-776,799-801,908-949`  
`createDummyMobileNetInput()`, `createDummyEfficientDetInput()`, `createDummyYoloInput()` create new `Random` instances and fill megabytes of buffers byte-by-byte each time. For a 640×640×3×4 buffer (4.9MB float), this takes ~50ms per call.  
**Fix:** Pre-generate one static input buffer per model and reuse it. Filling with constant data (e.g., `0.5f`) is equally valid for benchmarking.

### 8. `java.util.Random` per Buffer — 3 Unique Instances Created
**File:** `AiBenchmarkManager.kt:774,799,908,921,934,943`  
Each buffer fill creates a new `Random()` instance (syscall for seed). `ThreadLocalRandom` or a single static `Random` would avoid this overhead.  
**Fix:** Use a single static `Random` or `ThreadLocalRandom.current()`.

### 9. Delegate Creation Per Test — Shader Recompilation Cost
**File:** `AiBenchmarkManager.kt:693-747`  
GPU delegate initialization triggers OpenCL shader compilation in the Adreno driver. This is a one-time cost of 200-500ms. Creating a new GPU delegate per benchmark wastes this cost 8 times.  
**Fix:** Create GPU delegate once at startup, reuse across all benchmarks.

### 10. Models Force CPU Even When NPU/GPU Would Work
**File:** `AiBenchmarkManager.kt:199-201,464-468,547-549`  
MiniLM, MobileBERT, and DTLN all force `XNNPACK=true, NumThreads=4` with CPU only. Comments claim "stability" but modern TFLite + Qualcomm drivers work fine with NNAPI for these models on SD8Gen3. This means these benchmarks measure CPU inference, not NPU/GPU inference — making them inaccurate for AI accelerator benchmarking.  
**Fix:** Attempt NNAPI/GPU first with fallback, same as the generic inference path.

### 11. No `CompatibilityList` Check Before GPU Attempt
**File:** `AiBenchmarkManager.kt:709-718,733-738`  
GPU delegate is attempted via try/catch. The proper approach is `CompatibilityList().isDelegateSupportedOnThisDevice` + `compatList.bestOptionsForThisDevice`. The try/catch approach wastes 100-300ms on devices without GPU support.  
**Fix:** Use `org.tensorflow.lite.gpu.CompatibilityList` for pre-check.

### 12. `System.nanoTime()` Instead of `SystemClock.elapsedRealtimeNanos()`
**File:** `AiBenchmarkManager.kt:852,858`  
`System.nanoTime()` can be affected by CPU frequency scaling and core migration. On Android, `SystemClock.elapsedRealtimeNanos()` is monotonic and immune to DVFS.  
**Fix:** Use `SystemClock.elapsedRealtimeNanos()` for all benchmark timing.

### 13. `Dispatchers.Default` Used for IO-Bound Model Loading
**File:** `AiBenchmarkManager.kt:119,148,184,272,302,436,458,543`  
All `run*` functions use `withContext(Dispatchers.Default)` which is CPU-optimized and limited to `CPU_COUNT` threads. Model loading is IO-bound (disk read).  
**Fix:** Use `withContext(Dispatchers.IO)` for model loading, switching to `Dispatchers.Default` only for inference.

### 14. Warmup Iterations Too Few for GPU/NPU
**File:** `AiBenchmarkManager.kt:117,147,182,271,301,429,458,542`  
GPU and NNAPI delegates need 5-10 warmup inferences before reaching steady-state performance (shader compilation, memory allocation, etc.). Current warmup is 0-2 for most tests.  
**Fix:** Increase warmup to 5 for GPU/NPU modes, keep 2 for CPU.

### 15. Buffer Rewind Overhead per Iteration
**File:** `AiBenchmarkManager.kt:843-846,862-865`  
Every iteration rewinds ALL input and output buffers. This is necessary for output buffers but wasteful for static input buffers that haven't changed.  
**Fix:** Only rewind output buffers. Input buffers don't change between iterations.

### 16. `logTensorDetails` Called per Benchmark — Thread-Safe Logging Cost
**File:** `AiBenchmarkManager.kt:202,485,562,894-906`  
`logTensorDetails()` calls `android.util.Log.d()` synchronously for every input/output tensor. On a benchmark with 3 inputs + 2 outputs, that's 5 Log.d calls per benchmark — not huge, but unnecessary in production benchmark.  
**Fix:** Guard with a DEBUG flag or remove from non-debug builds.

---

## Architecture & Design Issues

### 17. Single-File Monolith — 960 Lines of Mixed Concerns
**File:** `AiBenchmarkManager.kt` (960 lines)  
Single class handles model downloading, delegate selection, input creation, inference, timing, scoring, and error handling for 8 distinct AI models.  
**Fix:** Split into `AiModelRegistry` (model metadata), `AiInterpreterFactory` (delegate + interpreter creation), `AiBenchmarkRunner` (timing loop).

### 18. `ModelDownloader` Uses Static URLs Hardcoded
**File:** `ModelDownloader.kt:110-176`  
GitHub release URLs are hardcoded. If the repo moves, all models break. File sizes are approximate strings ("~50MB") not actual byte counts — progress reporting is impossible.  
**Fix:** Host models on a CDN with checksum validation. Store model metadata (size, SHA256) in a JSON manifest.

### 19. No Progress Reporting During Download
**File:** `ModelDownloader.kt:43-93`  
`okhttp3.Request.Builder().build()` downloads the entire file in one blocking call. On slow connections (300MB Gemma model), the UI appears frozen for minutes.  
**Fix:** Use streaming download with `ResponseBody.source()` + progress callback, or use Android `DownloadManager`.

### 20. Gemma Model — 300MB Download with No Streaming
**File:** `ModelDownloader.kt:128-129`  
The Gemma LLM model is 300MB. `okhttp3` Response is loaded into memory entirely before writing to disk. On a 6GB RAM device, this can cause OOM (Out of Memory).  
**Fix:** Stream the response body directly to file using `response.body?.source()?.let { file.sink().buffer().writeAll(it) }`.

---

## Summary — Priority Order

| Priority | Issue | Impact | Effort |
|----------|-------|--------|--------|
| **CRIT** | #1 NNAPI delegate leak | Memory leak, eventual OOM | Low |
| **CRIT** | #3 LLM fallback Kotlin loop | 134M iter = 5-10 sec wasted | Medium |
| **CRIT** | #20 Gemma 300MB OOM risk | Crash on low RAM devices | Low |
| **HIGH** | #2 Interpreter created 3×/test | 200-400ms wasted per test | Medium |
| **HIGH** | #5 No interpreter reuse | 200-400ms across suite | Medium |
| **HIGH** | #6 Missing MappedByteBuffer | 2-5× slower model loads | Low |
| **HIGH** | #10 Models force CPU | Inaccurate NPU/GPU scoring | Medium |
| **HIGH** | #9 Delegate per test | 200-500ms GPU shader cost ×8 | Medium |
| **MED** | #7 Random input hot path | 50ms per buffer fill | Low |
| **MED** | #12 System.nanoTime | Unstable timing | Low |
| **MED** | #14 Warmup too few | Unstable first-run scores | Low |
| **MED** | #13 IO on Default dispatcher | Thread pool contention | Low |
| **LOW** | #4 Token count estimation | Inaccurate LLM scoring | Medium |
| **LOW** | #15 Buffer rewind all | Marginal improvement | Low |
| **LOW** | #8 Multiple Random instances | Marginal improvement | Low |
| **LOW** | #11 Missing CompatibilityList | Slow GPU detection | Low |
| **LOW** | #16 logTensorDetails | Minor log overhead | Low |
| **LOW** | #17 960-line monolith | Maintainability | High |
| **LOW** | #18 Hardcoded URLs | Resilience | Medium |
| **LOW** | #19 No download progress | UX freeze | Medium |

---

## Reference Implementations

- **TFLite GPU delegate best practices:** [docs](https://github.com/tensorflow/tensorflow/blob/master/tensorflow/lite/g3doc/android/delegates/gpu.md)
  - Always use `CompatibilityList().isDelegateSupportedOnThisDevice` before creating GPU delegate
  - Use `compatList.bestOptionsForThisDevice` for optimal settings
  - Delegate can be reused across multiple interpreters

- **TFLite NNAPI delegate:** [docs](https://github.com/tensorflow/tensorflow/blob/master/tensorflow/lite/g3doc/android/delegates/nnapi.md)
  - `NnApiDelegate` must be explicitly closed with `.close()`
  - `setAllowFp16(true)` improves performance on Qualcomm devices

- **TFLite model loading:** [docs](https://github.com/tensorflow/tensorflow/blob/master/tensorflow/lite/g3doc/android/java.md)
  - Use `MappedByteBuffer` via `FileChannel.map()` for fastest model loading
  - Avoid `Interpreter(File)` — it copies the entire model to heap

- **Benchmark measurement:** [TFLite benchmark tool](https://github.com/tensorflow/tensorflow/blob/master/tensorflow/lite/g3doc/performance/measurement.md)
  - Minimum 50 runs for statistical significance
  - `--warmup_runs=1` minimum, more for GPU/NPU
  - Use `--use_gpu=true` flag for GPU benchmarking

