# FinalBenchmark v1.1.0

**Release Date:** June 15, 2026
**Version Code:** 11
**APK Size:** ~15 MB (arm64-v8a + armeabi-v7a, R8 minified)

---

## Changelog

### Splash & Theme
- Eliminated the bright splash flash on cold-launch in dark mode by introducing a `values-night` theme variant with a dark window background, matching system bars in night mode (#8)
- Cleaned up stale "Light Monet" / "Dark Monet" labels in Settings — they are now simply "Light" and "Dark", reflecting the current Material 3 dynamic-color implementation (#9)

### Layout
- Calibrate Power screen is now vertically scrollable, fixing clipped/overflowed content on 4:3 and squarish aspect ratios such as the MediaTek 1080x1500 test device and the Ayaneo Pocket Air Mini (#9)

### App Icon
- Added Android 13+ Material You Themed Icon support. The OS tints the existing monochrome icon with the wallpaper-derived color when Themed Icons are enabled in launcher settings (#10)

### Productivity Benchmark
- Fixed video-decode warmup that previously never ran warm: warmup is now 2000 ms, bounded by a 40% setup-time budget with 10 keyframes, drain is wall-clock capped at 100 ms, and a degenerate warmup emits a `Log.w` so it can no longer silently pass
