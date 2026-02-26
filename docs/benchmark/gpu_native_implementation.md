# GPU Benchmark — Implementation Reference

> This document describes the **actual current implementation** in the codebase.
> For the planned algorithm design and methodology spec, see [`gpu_native.md`](gpu_native.md).

---

## Summary Table

| Test Name (Enum) | Display Name | Category |
|---|---|---|
| `GPU_TRIANGLE_RENDERING` | Triangle Rendering | `BenchmarkCategory.GPU` |
| `GPU_COMPUTE_MATRIX` | Compute Matrix | `BenchmarkCategory.GPU` |
| `GPU_PARTICLE_SYSTEM` | Particle System | `BenchmarkCategory.GPU` |
| `GPU_TEXTURE_SAMPLING` | Texture Sampling | `BenchmarkCategory.GPU` |
| `GPU_TESSELLATION` | Tessellation | `BenchmarkCategory.GPU` |

Defined in [`BenchmarkName.kt`](../../app/src/main/java/com/ivarna/finalbenchmark2/cpuBenchmark/BenchmarkName.kt).

---

## 1. Benchmark Categories

Seven categories exist in [`BenchmarkCategory.kt`](../../app/src/main/java/com/ivarna/finalbenchmark2/cpuBenchmark/BenchmarkCategory.kt):

```kotlin
enum class BenchmarkCategory {
    CPU, AI, GPU, RAM, STORAGE, PRODUCTIVITY, EXTERNAL_GPU
}
```

GPU-category tests are separate from `EXTERNAL_GPU` (Unity/Unreal scenes).

---

## 2. Execution Flow

```
BenchmarkScreen(preset, BenchmarkCategory.GPU)
    └─▶ BenchmarkViewModel.startBenchmark(preset, GPU)
            ├─ 2 s warm-up (isWarmingUp = true)
            ├─ BenchmarkName.getByCategory(GPU)
            │       → [GPU_TRIANGLE_RENDERING, GPU_COMPUTE_MATRIX,
            │            GPU_PARTICLE_SYSTEM, GPU_TEXTURE_SAMPLING, GPU_TESSELLATION]
            ├─ testNames = names.map { it.displayName() }
            │       (no Single-Core / Multi-Core split — that only applies to CPU category)
            ├─ initializes TestState list → all PENDING
            └─ KotlinBenchmarkManager.runBenchmarks(preset, GPU)
                    emits BenchmarkEvents → ViewModel updates UI state
                    → completionEvent fires → onBenchmarkComplete(summaryJson)
```

**Key difference from CPU:** In `BenchmarkViewModel`, `testNames` are split into singleCore + multiCore only when `category == CPU`. For `GPU` (and all other categories), names are listed flat via `displayName()`.

---

## 3. UI During Benchmark

`BenchmarkScreen` is shared across all categories. For GPU runs:

| Component | Behaviour |
|---|---|
| `ReactorProgress` | Animated arc dial — progress 0→1 |
| `TimelineTestRow` | One row per GPU test — PENDING / RUNNING / COMPLETED states |
| `SectionHeader` | Shows `"SINGLE CORE OPERATIONS"` label (GPU has no multi-core split, so only one section header appears) |
| `HUDMonitor` | Fixed bottom pill — live CPU%, Temp°C, PowerW from `PerformanceMonitor` |
| `GlassTimerPill` | Estimated time remaining + elapsed time |

The AI-specific live results card is not shown for GPU category (guarded by `benchmarkCategory == BenchmarkCategory.AI`).

---

## 4. GPU Monitoring Stack (Device Info Screen)

A separate monitoring system provides real-time GPU data in the **Device Info** tab and the **GPU Frequency Card** composable. This runs independently of benchmark execution.

### Architecture

```
GpuInfoViewModel  (1-second polling loop)
    ├─ GpuFrequencyReader.readGpuFrequency()
    │       ├─ RootAccessManager.isRootGranted()  ← libsu root check
    │       ├─ GpuFrequencyCache (5 s TTL ConcurrentHashMap)
    │       ├─ tryCommonPaths()  ← fast path, no vendor detection needed
    │       │       /sys/class/kgsl/kgsl-3d0/gpuclk
    │       │       /sys/class/kgsl/kgsl-3d0/devfreq/cur_freq
    │       │       /sys/class/devfreq/gpufreq/cur_freq
    │       │       /sys/kernel/gpu/gpu_clock
    │       │       /sys/devices/platform/galcore/gpu/gpu0/gpufreq/cur_freq
    │       ├─ GpuVendorDetector.detectVendor()
    │       │       → sysfs gpu_model → /proc/gpuinfo → device fingerprint
    │       └─ GpuPaths.getPathsForVendor(vendor, type)
    │               ADRENO / MALI / POWERVR / TEGRA / GENERIC
    ├─ GpuFrequencyFallback  ← non-root attempt if root unavailable
    └─ emits StateFlow<GpuFrequencyState> + List<GpuDataPoint> (30 s window)

GpuInfoUtils  (one-shot snapshot, used for Device Info screen)
    ├─ getGpuBasicInfo()   → EGL PBuffer context → GL_RENDERER / GL_VENDOR / GL_VERSION
    ├─ getGpuFrequency()   → direct sysfs File reads (no root)
    ├─ getOpenGLInfo()     → EGL PBuffer → extensions, GL caps, GLSL version
    └─ getVulkanInfo()     → VulkanNativeBridge JNI → libvulkan_native.so
```

### Source Files

| File | Purpose |
|---|---|
| [`GpuMonitor.kt`](../../app/src/main/java/com/ivarna/finalbenchmark2/utils/GpuMonitor.kt) | Simple root (`libsu`) reader. Caches first working path from 5 common sysfs paths. |
| [`GpuFrequencyReader.kt`](../../app/src/main/java/com/ivarna/finalbenchmark2/utils/GpuFrequencyReader.kt) | Full reader with root check, cache, vendor detection, wildcard path resolution. |
| [`GpuFrequencyFallback.kt`](../../app/src/main/java/com/ivarna/finalbenchmark2/utils/GpuFrequencyFallback.kt) | Non-root fallback: `/proc/gpufreq/gpufreq_cur_freq`, `/sys/kernel/gpu/gpu_clock`. |
| [`GpuFrequencyCache.kt`](../../app/src/main/java/com/ivarna/finalbenchmark2/utils/GpuFrequencyCache.kt) | 5-second TTL cache for vendor, success path, max/min/available freqs, file content. |
| [`GpuFrequencyMonitor.kt`](../../app/src/main/java/com/ivarna/finalbenchmark2/utils/GpuFrequencyMonitor.kt) | Coroutine polling loop. Default `refreshRateMs = 500`. `StateFlow<GpuFrequencyState>`. |
| [`GpuVendorDetector.kt`](../../app/src/main/java/com/ivarna/finalbenchmark2/utils/GpuVendorDetector.kt) | Detects `GpuVendor` enum via sysfs → `/proc` → device fingerprint (stub). |
| [`GpuPaths.kt`](../../app/src/main/java/com/ivarna/finalbenchmark2/utils/GpuPaths.kt) | Per-vendor sysfs path tables for `current_frequency`, `max_frequency`, `min_frequency`, `available_frequencies`, `gpu_info`. Wildcard resolution via root `ls`. |
| [`GpuInfoUtils.kt`](../../app/src/main/java/com/ivarna/finalbenchmark2/utils/GpuInfoUtils.kt) | One-shot EGL/GLES2 context for renderer info + OpenGL caps. |
| [`GpuInfoDataClasses.kt`](../../app/src/main/java/com/ivarna/finalbenchmark2/utils/GpuInfoDataClasses.kt) | All data models (see Section 7). |
| [`VulkanNativeBridge.kt`](../../app/src/main/java/com/ivarna/finalbenchmark2/utils/VulkanNativeBridge.kt) | JNI wrapper for `libvulkan_native.so`. Safe fallback on load failure. |

---

## 5. GPU Vendor Detection

```kotlin
enum class GpuVendor { ADRENO, MALI, POWERVR, TEGRA, UNKNOWN }
```

`GpuVendorDetector.detectVendor()` tries methods in order:

| # | Method | Notes |
|---|---|---|
| 1 | `GL_RENDERER` string | **Always returns UNKNOWN** — cannot call GL from background coroutine |
| 2 | sysfs `gpu_model` | `/sys/class/kgsl/kgsl-3d0/gpu_model` (Adreno), `/sys/kernel/gpu/gpu_model` (Mali GED_SKI), `/sys/class/misc/mali0/device/gpu_model` (Mali) |
| 3 | `/proc/gpuinfo` | String-match against keyword lists |
| 4 | Device fingerprint | `Build.HARDWARE` / `Build.BOARD` — **stub, always UNKNOWN** |

---

## 6. Frequency Reading & Parsing

### `GpuFrequencyReader.readGpuFrequency()` Steps

1. `RootAccessManager.isRootGranted()` — no root → try `GpuFrequencyFallback` → return `RequiresRoot`
2. **Common-path fast path** — try 5 universal sysfs paths
3. **Vendor-specific paths** — `GpuPaths.getPathsForVendor(vendor, "current_frequency")`
4. Wildcard `*` paths → `GpuPaths.resolveWildcardPath()` via root `ls`
5. On success → also read max freq, min freq, available freq list, governor
6. Return `GpuFrequencyState.Available(data)` or `GpuFrequencyState.Error`

### `parseFrequency()` Logic

| Raw sysfs value | Rule | Output |
|---|---|---|
| Integer > 10,000,000 | ÷ 1,000,000 | Hz → MHz |
| Integer > 10,000 | ÷ 1,000 | KHz → MHz |
| Integer ≤ 10,000 | as-is | already MHz |
| String with `"MHz"` | strip non-digits | MHz |
| String with `"KHz"` | strip non-digits ÷ 1000 | MHz |

### Sysfs Path Coverage by Vendor

**Adreno (Qualcomm)** — base: `/sys/class/kgsl/kgsl-3d0/`
- Current: `devfreq/cur_freq`, `gpuclk`, `gpuclk_gpu_hz`
- Max: `devfreq/max_freq`, `max_gpuclk`
- Governor: `devfreq/governor`
- Extra info: `gpu_busy_percentage`, `thermal_mitigation`, `default_pwrlevel`, `num_pwrlevels`

**Mali (ARM)** — two driver families:
- devfreq: `/sys/class/misc/mali0/device/devfreq/devfreq*/cur_freq` (wildcard-resolved)
- GED_SKI (MediaTek): `/sys/kernel/gpu/gpu_clock`, `gpu_load`, `gpu_tmu`, `gpu_governor`

**PowerVR** — `/sys/devices/platform/pvrsrvkm/sgx_clk_freq`

**Tegra (NVIDIA)** — `/sys/kernel/debug/clock/gbus/rate`, `/sys/devices/57000000.gpu/devfreq/57000000.gpu/cur_freq`

**Generic fallback** — `/sys/class/devfreq/*/cur_freq`, `/sys/kernel/gpu/gpu_clock`

---

## 7. OpenGL & Vulkan Information

### OpenGL (`GpuInfoUtils.getOpenGLInfo`)

Creates a 1×1 px EGL PBuffer surface (GLES 2.0), queries:
- `GL_VERSION`, `GL_SHADING_LANGUAGE_VERSION`
- `GL_EXTENSIONS` → `List<String>`
- `GL_MAX_TEXTURE_SIZE`, `GL_MAX_VIEWPORT_DIMS`, `GL_MAX_FRAGMENT_UNIFORM_VECTORS`, `GL_MAX_VERTEX_ATTRIBS`
- Compression formats detected from extension strings: **ETC2**, **ASTC**, **DXT/S3TC**, **PVRTC**

Context is created and destroyed within the call — no persistent GL state.

### Vulkan (`VulkanNativeBridge`)

Loads `libvulkan_native.so` via `System.loadLibrary`. Native function `getVulkanInfoNative()` returns JSON:

```json
{
  "supported": true,
  "apiVersion": "1.3.0",
  "driverVersion": "...",
  "physicalDeviceName": "Adreno (TM) 750",
  "physicalDeviceType": "Integrated GPU",
  "instanceExtensions": ["VK_KHR_surface", "..."],
  "deviceExtensions": ["VK_KHR_swapchain", "..."],
  "features": { "samplerAnisotropy": true, "geometryShader": false, "..." : "..." },
  "memoryHeaps": [{ "size": 8589934592, "flags": "DEVICE_LOCAL" }]
}
```

`VulkanFeatures` has all 55 Vulkan 1.0 physical-device feature flags.
`VulkanFeatures.toSortedList()` → alphabetically sorted `List<Pair<String, Boolean>>` for the Device Info screen.

**Fallback if `.so` fails to load:** `VulkanInfo(supported = false)` + `PackageManager.FEATURE_VULKAN_HARDWARE_VERSION` presence check.

---

## 8. UI Components

### `GpuFrequencyCard`
[`GpuFrequencyCard.kt`](../../app/src/main/java/com/ivarna/finalbenchmark2/ui/components/GpuFrequencyCard.kt)

Material 3 `Card` observing `GpuInfoViewModel.gpuFrequencyState`:

| State | Shown |
|---|---|
| `Available` | Current freq (large monospace), min/max row, governor, source path, optional utilization bar |
| `RequiresRoot` | Error text |
| `NotSupported` | Info text |
| `Error` | Error message |

### `GpuUtilizationGraph`
[`GpuUtilizationGraph.kt`](../../app/src/main/java/com/ivarna/finalbenchmark2/ui/components/GpuUtilizationGraph.kt)

`GlassCard` + Canvas line graph — 30-second utilization history. Checks root via `RootAccessManager.getCachedRootAccess()` (avoids re-prompt on recomposition). Display flow: checking → no-root error → graph.

### `GpuDataPoint`
```kotlin
data class GpuDataPoint(val timestamp: Long, val utilization: Float)
```

### `GpuInfoViewModel`
[`GpuInfoViewModel.kt`](../../app/src/main/java/com/ivarna/finalbenchmark2/ui/viewmodels/GpuInfoViewModel.kt)

| Flow | Description |
|---|---|
| `gpuFrequencyState` | Latest reading, updated every 1 s |
| `gpuHistory` | 30-second sliding window of `GpuDataPoint` |

Utilization formula: `(currentFreq / maxFreq) * 100` → fallback to `availableFrequencies.max()` → fallback to constant 1000 MHz.

---

## 9. Data Models

```
GpuInfoState (sealed)
    Loading
    Success(gpuInfo: GpuInfo)
    Error(message: String)

GpuInfo
    basicInfo: GpuBasicInfo
        name, vendor, driverVersion, openGLVersion, vulkanVersion
    frequencyInfo: GpuFrequencyInfo?
        currentFrequency: Long?   (MHz)
        maxFrequency: Long?       (MHz)
    openGLInfo: OpenGLInfo?
        version, glslVersion, extensions: List<String>
        capabilities: OpenGLCapabilities
            maxTextureSize, maxViewportWidth/Height,
            maxFragmentUniformVectors, maxVertexAttributes,
            maxRenderbufferSize, supportedTextureCompressionFormats
    vulkanInfo: VulkanInfo?
        supported, apiVersion, driverVersion, physicalDeviceName, physicalDeviceType
        instanceExtensions: List<String>, deviceExtensions: List<String>
        features: VulkanFeatures?     (55 Vulkan 1.0 feature flags)
        memoryHeaps: List<VulkanMemoryHeap>?

GpuFrequencyReader.GpuFrequencyData
    currentFrequencyMhz: Long
    maxFrequencyMhz: Long?
    minFrequencyMhz: Long?
    availableFrequencies: List<Long>?
    governor: String?
    utilizationPercent: Int?    ← TODO: always null (not yet implemented)
    temperatureCelsius: Int?    ← TODO: always null (not yet implemented)
    vendor: GpuVendor
    sourcePath: String          ← which sysfs path was used

GpuFrequencyReader.GpuFrequencyState (sealed)
    Available(data: GpuFrequencyData)
    RequiresRoot
    NotSupported
    Error(message: String)
```

---

## 10. Known Limitations / TODOs

| Item | Status |
|---|---|
| `utilizationPercent` in `GpuFrequencyData` | Always `null` — sysfs GPU usage reading not implemented |
| `temperatureCelsius` in `GpuFrequencyData` | Always `null` — temperature reading not implemented in reader |
| `detectVendorFromOpenGL()` | Always returns `UNKNOWN` — can't call GL from background coroutine |
| `detectVendorFromDevice()` | Stub — always returns `UNKNOWN` |
| `FrameTimingEstimator.estimateFrequencyFromFrameTiming()` | Stub — always returns `null` |
| GPU workload implementations | `GPU_TRIANGLE_RENDERING`, `GPU_COMPUTE_MATRIX`, etc. exist in `BenchmarkName` enum but actual rendering workloads are inside `KotlinBenchmarkManager` / native layer |
