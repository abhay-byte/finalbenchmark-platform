# FinalBenchmark v1.0.0

**Release Date:** May 29, 2026
**Version Code:** 8
**APK Size:** ~33 MB (arm64-v8a + armeabi-v7a, R8 minified)

---

## Changelog

### AI / ML
- LiteRT 1.4.2 with 16KB page alignment, NNAPI-first delegate chain
- LLM TPS scoring with geometric mean, recalibrated to SD8G3

### Storage
- JNI posix_fadvise + fdatasync for true UFS 4.0 measurement
- 6 tests: SeqRead, SeqWrite, Rand4K, SmallFiles, SQLite, Mixed

### RAM
- JNI native benchmark with NEON + pthreads
- Recalibrated to actual SD8G3 measurements

### Productivity
- 9 GPU-accelerated benchmarks via HardwareRenderer/AGSL/MediaCodec
- Video encode/decode/transcode, 4K canvas, image processing, text, JSON, compression

### Full Benchmark
- Sequential CPU -> AI -> RAM -> Storage -> GPU -> Productivity
- Weighted scoring with per-category drill-down and performance graphs

### Rankings
- Per-category ranking data and multi-category comparison pages

---

## ABI Support
- arm64-v8a, armeabi-v7a
