# FinalBenchmark v1.0.2

**Release Date:** May 31, 2026
**Version Code:** 10
**APK Size:** ~15 MB (arm64-v8a + armeabi-v7a, R8 minified)

---

## Changelog

### AI Benchmark
- Replaced the entire Litert/TFLite backend with a custom Native C++ implementation to resolve infinite freezing during LLM inference
- AI backend now correctly attempts to load Vulkan/OpenCL/OpenGL ES accelerators before falling back to CPU
- Fixed the AI threading model to prevent silent fallback to CPU mode caused by EGL context thread affinity issues
- Recalibrated AI scoring baselines using actual NEON CPU measurements from the Snapdragon 8 Gen 3 reference device

### CPU Benchmark
- Restored optimized CPU iteration targets for `mid` and `flagship` tiers to ensure the entire benchmark suite completes within 3 minutes

### UI
- AI benchmark badges now accurately display the utilized accelerator backend (Vulkan, OpenCL, OpenGL ES, or CPU)
