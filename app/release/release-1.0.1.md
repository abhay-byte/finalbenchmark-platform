# FinalBenchmark v1.0.1

**Release Date:** May 30, 2026
**Version Code:** 9
**APK Size:** ~33 MB (arm64-v8a + armeabi-v7a, R8 minified)

---

## Changelog

### History
- Fix blank detailed data in history results (R8 obfuscation breaking Gson deserialization)
- Fall back to genericTestDetails table when detailedResultsJson is empty

### Rankings
- Per-category reference scores: GPU shows GPU names, Storage shows UFS/eMMC, RAM shows LPDDR type

### UI
- Remove performance tier tags (ELITE/POWERHOUSE/COMPETITIVE)
- Remove entry animations for snappier navigation

