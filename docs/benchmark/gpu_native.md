# GPU Native Benchmark

## Overview

The GPU Native benchmark measures **OpenGL ES 2.0 rendering performance** across 10 distinct workloads, each stressing a different aspect of mobile GPU architecture. The benchmark runs entirely on-device using the native OpenGL pipeline without any CPU assist beyond scene setup.

**Benchmark Type:** `GPU`  
**Renderer:** `GpuBenchmarkRenderer` (GLSurfaceView + OpenGL ES 2.0)  
**Scene Count:** 10  
**Duration per scene:** 2 s warm-up + 6 s measurement  
**Total duration:** ~80 s  
**Orientation:** Forces landscape during run; restores portrait on result screen  

---

## 1. Architecture

```
GpuBenchmarkScreen  <->  GpuBenchmarkViewModel  <->  GpuBenchmarkRenderer
   (Compose UI)            (coroutine loop)           (GL thread)
        |                        |                         |
        |     onFrameMetrics(fps, frameMs)                 |
        |<-------------------------------------------------|
        |                        |                         |
        |     currentScene ------>|                        |
        `------------------------'                         |
                                         glFinish() after every draw
                                         -> uncapped wall-clock render time
```

### Timing Methodology

Each frame's render time is measured as pure GPU wall-clock time:

```kotlin
val drawStart = System.nanoTime()
// draw scene x 4 passes
GLES20.glFinish()          // block until GPU fully completes
val renderMs = (System.nanoTime() - drawStart) / 1_000_000f
val fps = 1000f / renderMs // uncapped, not vsync-gated
```

`glFinish()` ensures the CPU blocks until all submitted GPU commands complete, giving an accurate per-frame GPU render time independent of vsync.

---

## 2. Ten Rendered Scenes

Each scene draws its fullscreen quad **4 times per frame** to amplify GPU load and push frame time above 33 ms (target <30 FPS on high-end devices).

| # | Scene | Focus | Draw Passes |
|---|-------|-------|------------|
| 1 | **Triangle Rendering (10K)** | Vertex throughput | 4x Domain Warp + triangle overlay |
| 2 | **Julia / Matrix Compute** | Shader ALU | 4x branchless 128-iter Julia + 16 mat4 chains |
| 3 | **Particle System (5K)** | Fill rate + geometry | 4x 128-light Phong + particle overlay |
| 4 | **12-Octave FBM Texture** | Texture/ALU fill rate | 4x 6-chain dependent FBM (72 noise octaves/px) |
| 5 | **Wave Mesh (250x250)** | Geometry throughput | 4x Ray March SDF + mesh overlay |
| 6 | **Mandelbrot Deep (512 iter)** | Fragment ALU | 4x 512-iteration smooth Mandelbrot zoom |
| 7 | **Phong 128-Light Array** | Lighting ALU | 4x analytic Phong with 128 dynamic point lights |
| 8 | **Ray March SDF + Shadows** | Ray marching | 4x 100-step SDF + 32-step soft shadow + AO |
| 9 | **Triple Domain Warp FBM** | Noise ALU | 4x 3-stage x 12-octave domain-warp noise |
| 10 | **64x Super-Sampled Fractal** | Super-sampling | 4x Newton fractal with 64 Halton samples x 48 Newton steps |

### Scene Detail

**Scene 1 – Triangle Rendering (10K)**  
10,000 orbit-animated triangles with per-vertex orbit radius, phase, speed, and rotation speed baked into a single VBO. Pre-pass runs Domain Warp FBM shader for fill-rate pressure.

**Scene 2 – Julia / Matrix Compute**  
Per-pixel Julia set with **branchless** iteration (no early `break` — all 128 iterations always execute via `step()`/`mix()`), preventing the GPU shader compiler from short-circuiting the loop. Also chains 16 mat4 x mat4 multiplications (1,024 matrix muls + 768 adds per pixel).

**Scene 3 – Particle System (5K)**  
5,000 CPU-simulated physics particles (gravity + life) uploaded to GPU as GL_POINTS each frame. Pre-pass runs 128-analytic-light Phong shading for fill-rate load.

**Scene 4 – 12-Octave FBM Texture**  
Six cascaded dependent FBM lookups where each lookup's UV depends on the output of the previous, creating a fully serial data-dependency chain: `n1 -> n2 -> n3 -> n4 -> n5 -> n6`. Each FBM is 12 octaves = 72 noise evaluations per pixel total.

**Scene 5 – Wave Mesh (250x250)**  
250x250 grid (~125,000 triangles) with double-frequency sinusoidal wave displacement in the vertex shader. Pre-pass runs Ray March SDF shader.

**Scene 6 – Mandelbrot Deep Zoom**  
512-iteration smooth Mandelbrot with animated zoom and pan. Uses `log2(log2(dot(z,z)))` smooth colouring.

**Scene 7 – Phong 128-Light Array**  
128 analytic Phong lights computed per pixel each frame, with positions animated by per-light random speed/phase. Full diffuse + specular + distance attenuation.

**Scene 8 – Ray March SDF + Shadows**  
100-step sphere-tracing of a scene with 2 spheres + 1 box + ground plane (merged with `smin` smooth-union). Includes 32-step soft-shadow rays and 5-tap ambient-occlusion per hit. Objects centered on screen (UV range [-1..1]^2, camera at (0, 0.6, 2.8)).

**Scene 9 – Triple Domain Warp FBM**  
Three nested domain-warp passes, each using 12-octave FBM. Fully dependent evaluation chain of 36 FBM calls (432 noise-octave evaluations per pixel).

**Scene 10 – 64x Super-Sampled Newton Fractal**  
Newton fractal evaluated at 64 Halton low-discrepancy sample offsets per pixel, each with up to 48 Newton iterations. Total: up to 3,072 Newton steps per pixel.

---

## 3. Scoring

### Reference Device

The scoring baseline is **Snapdragon 8 Gen 3 / Adreno 750** = **100 points**.

Reference FPS values (measured with 4x draw passes per frame):

| Scene | Reference FPS (SD 8 Gen 3) |
|-------|--------------------------|
| Triangle Rendering (10K) | 20.0 |
| Julia / Matrix Compute | 15.0 |
| Particle System (5K) | 7.0 |
| 12-Octave FBM Texture | 24.0 |
| Wave Mesh (250x250) | 23.0 |
| Mandelbrot Deep (512 iter) | 16.0 |
| Phong 128-Light Array | 7.0 |
| Ray March SDF + Shadows | 24.0 |
| Triple Domain Warp FBM | 20.0 |
| 64x Super-Sampled Fractal | 3.5 |

### Per-Scene Score

```
SceneScore = (SUT_FPS / Reference_FPS) x 100
```

A scene where the device achieves exactly the reference FPS scores 100 pts.

### Final GPU Score

The final score uses the **geometric mean** of all 10 per-scene FPS ratios, matching the CPU benchmark's methodology:

```
ratio_i = SUT_FPS_i / Reference_FPS_i   (for scene i = 1..10)

GeometricMean = (product of all ratio_i) ^ (1/10)

FinalGpuScore = GeometricMean x 100
```

**Why geometric mean?**
- Prevents a single fast scene from inflating the total score
- Balanced performance across all workloads is required for a high score
- Consistent with CPU benchmark methodology (same formula)
- Fair cross-architecture comparison

**Score ranges:**

| Score | Performance Level |
|-------|------------------|
| < 30 | Low-end GPU |
| 30-70 | Mid-range GPU |
| 70-130 | High-end (SD 8 Gen 3 class) |
| > 130 | Flagship / overclocked |

### Score Display in Result Screen

The **Detailed Data** tab shows:
1. **GPU score card** — final geometric-mean score + average FPS
2. **FPS Per Scene bar chart** — Canvas-drawn bar chart of all 10 scenes, color-coded (red <10 fps, orange <20 fps, green <40 fps, blue 40+ fps)
3. **GPU Rendering Results** — expandable list with per-scene FPS, frame time, individual score
4. **Performance Monitoring** — CPU / power / temperature graphs (if monitoring data is available)

---

## 4. Result JSON Format

```json
{
  "type": "GPU",
  "preset": "flagship",
  "final_score": 98.3,
  "normalized_score": 98.3,
  "single_core_score": 0.0,
  "multi_core_score": 17.4,
  "timestamp": 1709000000000,
  "detailed_results": [
    {
      "name": "Triangle Rendering (10K)",
      "opsPerSecond": 20.6,
      "executionTimeMs": 48.6,
      "isValid": true,
      "metricsJson": "{\"score\":103,\"avgFps\":20.60,\"avgFrametimeMs\":48.60}"
    }
  ],
  "performance_metrics": {
    "powerConsumption":  { "watts": [],      "timestamp": [] },
    "cpuUsage":          { "usage": [],      "timestamp": [] },
    "cpuTemperature":    { "temperature": [], "timestamp": [] },
    "batteryTemperature":{ "temperature": [], "timestamp": [] }
  }
}
```

`opsPerSecond` holds average FPS. `multi_core_score` holds arithmetic-mean FPS across all scenes.

---

## 5. Database Storage

| Column | Value |
|--------|-------|
| `type` | `"GPU"` |
| `totalScore` | Geometric mean x 100 (rounded) |
| `singleCoreScore` | 0.0 |
| `multiCoreScore` | Arithmetic mean FPS across all scenes |
| `normalizedScore` | Same as totalScore |
| `detailedResultsJson` | JSON array of 10 BenchmarkResult objects |
| `performanceMetricsJson` | Full monitoring JSON (power/CPU/temp) |

---

## 6. History View

`HistoryViewModel` deserialises `detailedResultsJson` (always a JSON array) into `List<BenchmarkResult>` via Gson. The result JSON is reconstructed from the history entity with the same keys as a fresh run, so the Detailed Data tab and share functionality work identically.

---

## 7. Share Output Format

```
FinalBenchmark 2 Results
========================

Device: OnePlus CPH2691
OS: Android 16 (API 36)
GPU: Adreno (TM) 750

TOTAL SCORE: 98
(Normalized: 98)

[GPU Scene Results]
Triangle Rendering (10K): 103.0 pts  |  20.6 FPS  |  48.6 ms
Julia / Matrix Compute:    87.3 pts  |  13.1 FPS  |  76.4 ms
...
```

---

## 8. Implementation Files

| File | Role |
|------|------|
| `gpu/GpuBenchmarkRenderer.kt` | OpenGL ES 2.0 renderer, 10-scene dispatch, glFinish() timing |
| `gpu/GpuBenchmarkShaders.kt` | All GLSL ES 2.0 shader sources |
| `ui/viewmodels/GpuBenchmarkViewModel.kt` | Coroutine benchmark loop, geometric-mean scoring, DB save |
| `ui/screens/GpuBenchmarkScreen.kt` | Compose UI, landscape lock, HUD overlays |
| `ui/screens/ResultScreen.kt` | GpuFpsBarChart, BenchmarkSection(isGpu=true), GPU detailed tab |
| `navigation/MainNavigation.kt` | Routes GPU start/complete through nav graph |
