# Storage Benchmark — Complete Implementation Reference

## 1. Overview

The Storage benchmark measures six distinct aspects of the device's I/O subsystem. Unlike the RAM benchmark it does **not** use native C / JNI for the primary path (with the exception of `posix_fadvise` / `fdatasync` for sequential tests — see §4). All work is dispatched to Kotlin coroutines on `Dispatchers.IO` with a `MEASURE_DUR_MS = 3 000 ms` timed window per test.

### Why JNI only for seq-read / seq-write?

| Problem | Java/Kotlin behaviour | JNI solution |
|---|---|---|
| Sequential read hits page cache | After first pass the file lives in Linux page cache; FileInputStream re-reads from RAM at ~6 GB/s instead of from real UFS | `posix_fadvise(fd, 0, 0, POSIX_FADV_DONTNEED)` before each read pass evicts the file's pages so every pass hits real UFS |
| Sequential write never reaches UFS | `FileOutputStream.flush()` only flushes Java buffers; data lives in Linux dirty-page cache | `fdatasync(fd)` after each write pass flushes all dirty pages to UFS hardware |
| `Os.posix_fadvise` is a hidden API | `android.system.Os.posix_fadvise` resolves with reflection but is **not** in the public SDK stubs — `./gradlew compileDebugSources` fails | Call `posix_fadvise()` from a C function loaded via JNI instead |

All other four tests (random 4K, small files, SQLite, mixed) are pure Kotlin on `Dispatchers.IO` because the page-cache effect doesn't distort their measurements.

---

## 2. Architecture

```
Kotlin (viewModelScope)
    │
    ├── StorageBenchmarkViewModel.kt      ← orchestration, scoring, DB save, JSON build
    │       └── runBenchmark() coroutine
    │               ├── warm-up pass (800 ms, Dispatchers.IO)
    │               └── measure pass (3 000 ms, Dispatchers.IO)
    │                       ├── benchSeqRead()   → StorageNativeBridge.nativeStorageSeqRead()
    │                       ├── benchSeqWrite()  → StorageNativeBridge.nativeStorageSeqWrite()
    │                       ├── benchRand4K()    → pure Kotlin
    │                       ├── benchSmallFiles()→ pure Kotlin
    │                       ├── benchSqlite()    → SQLiteDatabase (WAL mode)
    │                       └── benchMixed()     → pure Kotlin composite
    │
    ├── StorageNativeBridge.kt            ← Kotlin object, System.loadLibrary("vulkan_native")
    │       ├── nativeStorageSeqRead(...)
    │       └── nativeStorageSeqWrite(...)
    │
    ├── storage_benchmark.c               ← JNI C: posix_fadvise + fdatasync
    │       ├── Java_..._nativeStorageSeqRead()
    │       └── Java_..._nativeStorageSeqWrite()
    │
    ├── CMakeLists.txt                    ← storage_benchmark.c added to vulkan_native target
    │
    ├── StorageBenchmarkScreen.kt         ← Compose benchmark-running UI
    │
    └── ResultScreen.kt                   ← STORAGE branch in DetailedDataTab
```

### Files changed / created

| File | Type | Change |
|---|---|---|
| `app/src/main/cpp/storage_benchmark.c` | **NEW** | 2 JNI functions — seq read (posix_fadvise) and seq write (fdatasync) |
| `app/src/main/cpp/CMakeLists.txt` | **MODIFIED** | Added `storage_benchmark.c` to `add_library(vulkan_native …)` |
| `app/src/main/java/.../utils/StorageNativeBridge.kt` | **NEW** | Kotlin JNI bridge (same `.so` as RAM: `vulkan_native`) |
| `app/src/main/java/.../viewmodels/StorageBenchmarkViewModel.kt` | **NEW** | Full benchmark VM: 6 tests, scoring, DB save, JSON build |
| `app/src/main/java/.../screens/StorageBenchmarkScreen.kt` | **NEW** | Benchmark-running screen (amber/orange glass UI) |
| `app/src/main/java/.../screens/ResultScreen.kt` | **MODIFIED** | Added `STORAGE` branch in `DetailedDataTab` + `StorageThroughputChart` |
| `app/src/main/java/.../navigation/MainNavigation.kt` | **MODIFIED** | Added `BenchmarkCategory.STORAGE` dispatch block |
| `app/src/main/java/.../cpuBenchmark/BenchmarkCategory.kt` | **PRE-EXISTING** | `STORAGE` enum value already present |

---

## 3. Test Descriptions

### Test 1 — Sequential Read (`SEQ_READ`)

**Goal**: Measure single-threaded sequential read throughput from UFS storage, bypassing the Linux page cache.

**Algorithm**:
1. Pre-create a `SEQ_FILE_MB = 256 MB` file filled with a non-zero pattern **outside** the timed loop (using `fdatasync` to ensure bytes are on storage before reading begins).
2. Timed loop (`while now_ms() < endMs`):
   - Open the file → call `posix_fadvise(fd, 0, 0, POSIX_FADV_DONTNEED)` → close.  
     This evicts all file pages from the Linux page cache.
   - Reopen → call `posix_fadvise(fd, 0, 0, POSIX_FADV_SEQUENTIAL)` to hint prefetch.
   - Read file in `SEQ_CHUNK_BYTES = 1 MB` chunks, counting `totalBytes`.
3. Returns `totalBytes / (durationMs / 1000) / (1024 × 1024)` → **MB/s**

**JNI function**: `Java_com_ivarna_finalbenchmark2_utils_StorageNativeBridge_nativeStorageSeqRead`

**Why not Java?**: `FileInputStream.read()` cannot call `posix_fadvise`. Without page-cache eviction each subsequent pass reads from RAM (~6 GB/s), not from UFS (~1 400 MB/s).

**Unit**: MB/s  
**Reference** (SD 8 Gen 3 + UFS 4.0 baseline): 2 133 MB/s

---

### Test 2 — Sequential Write (`SEQ_WRITE`)

**Goal**: Measure single-threaded sequential write throughput to UFS storage, including the time to flush dirty pages.

**Algorithm**:
1. Timed loop (`while now_ms() < endMs`):
   - `open(path, O_WRONLY | O_CREAT | O_TRUNC, 0600)`
   - Write `256 MB` in `1 MB` chunks.
   - Call `fdatasync(fd)` — flushes all dirty pages to UFS hardware.
   - `close(fd)` → `unlink(path)`.
   - `totalBytes += written`
2. Returns `totalBytes / (durationMs / 1000) / (1024 × 1024)` → **MB/s**

**JNI function**: `Java_com_ivarna_finalbenchmark2_utils_StorageNativeBridge_nativeStorageSeqWrite`

**Why `fdatasync` is inside the timed loop**: Without it we only measure how fast we can fill the Linux dirty-page writeback queue (RAM speed). With `fdatasync` we measure what the UFS controller must actually sustain.

**Unit**: MB/s  
**Reference**: 939 MB/s

---

### Test 3 — Random 4 K Read (`RAND_4K`)

**Goal**: Measure random small-block read throughput (IOPS × 4 KB).

**Algorithm**:
1. Pre-create or reuse a `RAND_FILE_MB = 128 MB` file.
2. Open with `RandomAccessFile(file, "r")`.
3. Timed loop: LCG seed (`seed = seed * 6364136223846793005L + 1442695040888963407L`) to generate a 4 KB-aligned offset → `raf.seek(offset)` → `raf.read(buf, 0, 4096)`.
4. Returns `totalBytes / (durationMs / 1000) / (1024 × 1024)` → **MB/s**

**Note on page cache**: The page cache is periodically evicted every 512 reads (2 MB) using the JNI call `nativeEvictCache` to prevent RAM caching, measuring true UFS controller read latency and hardware speed rather than operating system caching.

**Unit**: MB/s  
**Reference**: 42 MB/s

---

### Test 4 — Small File Operations (`SMALL_FILES`)

**Goal**: Measure file-system metadata throughput: create, write, sync, and delete small files.

**Algorithm**:
1. Timed loop: create a `bench_small/` directory once.
2. Each pass: create `SMALL_FILE_COUNT = 300` files of `SMALL_FILE_SIZE = 8 KB`:
   - `FileOutputStream(file).write(buf)` + `fos.fd.sync()`
3. After each batch of 300 creations, delete all 300 files.
4. Count total files created; divide by `(durationMs / 1000)` → **files/s**

**Unit**: files/s  
**Reference**: 4 300 files/s

---

### Test 5 — SQLite Performance (`SQLITE`)

**Goal**: Measure real-world database transaction throughput.

**Algorithm**:
1. Create a fresh SQLite database at `bench_sqlite.db`.
2. Configure with WAL mode: `db.rawQuery("PRAGMA journal_mode=WAL", null).use { it.moveToFirst() }`  
   *(Note: `execSQL` throws on Android 11+ for PRAGMAs that return a result row — must use `rawQuery`.)*
3. `PRAGMA synchronous=NORMAL`
4. Pre-compile `INSERT INTO items(name,value,ts) VALUES(?,?,?)` once.
5. Timed loop, alternating:
   - **INSERT batch**: `beginTransaction` → insert `SQLITE_ROWS = 50` rows → `setTransactionSuccessful()` → `endTransaction`. txns++
   - **SELECT query**: `beginTransaction` → `rawQuery("SELECT * FROM items WHERE ts > ? ORDER BY ts DESC LIMIT 50", ...)` → `moveToFirst()` to materialize result → `endTransaction`. txns++
6. Returns `txns / (durationMs / 1000)` → **txn/s**

**Unit**: txn/s  
**Reference**: 10 626 txn/s

---

### Test 6 — Mixed Workload (`MIXED`)

**Goal**: Simulate a composite real-world I/O pattern: sequential read + random 4K reads + small file creation.

**Algorithm**:
1. Each pass (within timed loop):
   - Sequential read: read `16 MB` from a pre-created file in `1 MB` chunks. `seqBytes += n`
   - Random 4K: 200 random seeks on a `64 MB` file. `randBytes += 4096`
   - Small file: create + write + delete 50 × `8 KB` files. `sfBytes += 50 × 8192`
2. `composite_MB_per_s = (seqBytes + randBytes + sfBytes) / (elapsedMs / 1000) / (1024 × 1024)`

**Unit**: MB/s  
**Reference**: 620 MB/s

---

## 4. Native Code: `storage_benchmark.c`

### Timing helper

```c
static int64_t now_ms(void) {
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return (int64_t)ts.tv_sec * 1000LL + (int64_t)ts.tv_nsec / 1000000LL;
}
```

Uses `CLOCK_MONOTONIC` (same as the JVM baseline) for consistency. No `noinline` needed here because the C compiler cannot hoist a `clock_gettime` syscall.

### Buffer fill pattern

Both functions fill their working buffer with `i ^ 0xA5` (read) or `i * 7 ^ 0x55` (write). Non-zero, non-repeating patterns prevent compression by the NAND FTL and prevent the kernel from optimising writes as zero-page deduplication.

### Key POSIX calls

```c
/* Evict file from page cache */
posix_fadvise(fd, 0, 0, POSIX_FADV_DONTNEED);

/* Hint sequential prefetch */
posix_fadvise(fd, 0, 0, POSIX_FADV_SEQUENTIAL);

/* Flush dirty pages to UFS */
fdatasync(fd);
```

`posix_fadvise` is defined in `<fcntl.h>` on Android (bionic libc) — no extra headers required. `fdatasync` is in `<unistd.h>`.

---

## 5. `StorageNativeBridge.kt`

```kotlin
object StorageNativeBridge {
    private var loaded = false

    fun load(): Boolean {
        if (loaded) return true
        return try {
            System.loadLibrary("vulkan_native")   // same .so as RAM benchmark
            loaded = true; true
        } catch (e: UnsatisfiedLinkError) { loaded = false; false }
    }

    val isAvailable: Boolean get() = loaded

    external fun nativeStorageSeqRead(
        path: String, fileSizeBytes: Long, chunkSize: Int, durationMs: Long): Double

    external fun nativeStorageSeqWrite(
        path: String, fileSizeBytes: Long, chunkSize: Int, durationMs: Long): Double
}
```

The library is the same `vulkan_native.so` built from `CMakeLists.txt` — no new `.so` is created. `storage_benchmark.c` is simply added to the existing `add_library(vulkan_native SHARED …)` target.

---

## 6. Benchmark Loop in `StorageBenchmarkViewModel`

```
for each test in [SEQ_READ, SEQ_WRITE, RAND_4K, SMALL_FILES, SQLITE, MIXED]:
    1. Update UI state → show "Preparing …" + warm-up phase indicator
    2. Launch warm-up job on Dispatchers.IO (800 ms): runTest(test, 800L)
       - Tick 100 ms × 10 on main coroutine, updating currentTestProgress 0%→15%
    3. Record measureStartMs = System.currentTimeMillis()
    4. Launch measure job on Dispatchers.IO (3 000 ms): runTest(test, 3000L)
       - Tick 100 ms × 30, updating currentTestProgress 15%→100%
    5. elapsedMs = System.currentTimeMillis() - measureStartMs
    6. Create StorageTestResult(test, name, value, unit, test.score(value), elapsedMs)
    7. Append to results list; update UI completedTests
```

Both warm-up and measure are launched with `async(Dispatchers.IO)` in a `coroutineScope {}`. The UI progress tick runs on the calling coroutine (viewModelScope / main dispatcher) concurrently with the IO job.

---

## 7. Scoring

### Per-test score

```kotlin
private fun StorageTest.score(value: Double): Int {
    val ref = STORAGE_REFERENCE[this] ?: return 0
    return (value / ref * 100.0).roundToInt().coerceIn(0, 100)
}
```

`coerceIn(0, 100)` caps at 100 so no single test can exceed 100 pts and distort the geometric mean total.

### Total score

```kotlin
private fun calculateStorageGeometricMean(results: List<StorageTestResult>): Double {
    val ratios = results.map { r ->
        (r.value / (STORAGE_REFERENCE[r.test] ?: 1.0)).coerceAtLeast(1e-9)
    }
    val product = ratios.fold(1.0) { acc, v -> acc * v }
    return product.pow(1.0 / ratios.size) * 100.0
}
```

Geometric mean of 6 test ratios × 100. A device exactly matching all references → score = 100. The SD 8 Gen 3 / UFS 4.0 baseline (OnePlus CPH2691) scores 100.

### Reference values

| Test | Reference | Basis |
|---|---|---|
| SEQ_READ | 2 133 MB/s | Calibrated to 100 pts on OnePlus SD 8 Gen 3 |
| SEQ_WRITE | 939 MB/s | Calibrated to 100 pts on OnePlus SD 8 Gen 3 |
| RAND_4K | 42 MB/s | Calibrated to 100 pts on OnePlus SD 8 Gen 3 (with native cache eviction) |
| SMALL_FILES | 4 300 files/s | Calibrated to 100 pts on OnePlus SD 8 Gen 3 (with fsync) |
| SQLITE | 10 626 txn/s | Calibrated to 100 pts on OnePlus SD 8 Gen 3 (WAL, NORMAL, un-transacted SELECT) |
| MIXED | 620 MB/s | Calibrated to 100 pts on OnePlus SD 8 Gen 3 (with fsync + native evict) |

All references are ~20% above the best measured ceiling on the baseline device.  
A device scoring exactly 100 on every test would be ~20% faster than a flagship SD 8 Gen 3.

---

## 8. Data Flow: from ViewModel to DB to ResultScreen

```
StorageTestResult (in-memory)
    │
    ├── buildResultJson()
    │   ├─ detailed_results: JSONArray
    │   │   each entry: { name, opsPerSecond(=value), executionTimeMs(=durationMs),
    │   │                 isValid:true,
    │   │                 metricsJson: {"score":N,"value":V,"unit":"MB/s","durationMs":T} }
    │   └─ top-level: type, preset, final_score, normalized_score, multi_core_score(=avgBW)
    │
    │  → emitted via _completionEvent → MainNavigation URL-encodes JSON → result screen
    │
    └── saveToDatabase()
        ├─ BenchmarkResultEntity(type="STORAGE", totalScore, multiCoreScore=avgBW,
        │                        detailedResultsJson = detailed_results array)
        └─ GenericTestDetailEntity per test:
               score = r.value   (raw measurement, not pts score)
               metricsJson = {"score":N,"value":V,"unit":"U","durationMs":T}
```

**Why `opsPerSecond = r.value` (not `r.score`)?** `BenchmarkResult.opsPerSecond` is used as a fallback value in chart/display code. Storing the raw measurement there (1 451 MB/s) means charts work even if `metricsJson` parsing fails.

**`executionTimeMs` per test**: Stored as `r.durationMs` in the JSON. `BenchmarkResultItem` reads it for the `"%.3fs"` timing display. The `0.000s` bug (resolved) was caused by hardcoding `0.0` before `durationMs` was tracked.

---

## 9. ResultScreen: `STORAGE` Branch

### `DetailedDataTab` (tab index 1)

```kotlin
} else if (summary.type == "STORAGE") {
    // Score card (displays summary.finalScore as big number)
    item { AnimatedEntranceContainer(0) { StorageScoreCard(summary) } }

    // Bar chart with 6 coloured bars (MB/s / files/s / txn/s per test)
    item { AnimatedEntranceContainer(1) {
        if (summary.detailedResults.isNotEmpty())
            StorageThroughputChart(results = summary.detailedResults)
    }}

    // List of individual results using BenchmarkSection(isRam = true)
    item { AnimatedEntranceContainer(2) {
        BenchmarkSection(
            title = "Storage Test Results",
            score = summary.finalScore,
            results = summary.detailedResults,
            isRam = true   // ← tells BenchmarkResultItem to parse metricsJson for score/value/unit
        )
    }}
}
```

`isRam = true` routes to the `isRam` branch in `BenchmarkResultItem`:

```kotlin
} else if (isRam) {
    val metricsObj = JSONObject(result.metricsJson)
    val unit  = metricsObj.optString("unit", "MB/s")
    val value = metricsObj.optDouble("value", result.opsPerSecond)
    displayThroughput = "%.0f %s".format(value, unit)
    individualScore   = metricsObj.optDouble("score", 0.0)
    // executionTimeMs from result.executionTimeMs (≡ durationMs from JSON)
}
```

### `StorageThroughputChart`

A custom bar chart Composable that:
1. Reads `metricsObj.optDouble("value", r.opsPerSecond)` for each result.
2. For `files/s` and `txn/s` tests, uses a secondary Y axis so MB/s tests don't dwarf them.
3. Colour-codes bars by test type (amber = SEQ_READ, deep-orange = SEQ_WRITE, etc.).

### Share text (`STORAGE` branch in `formatBenchmarkShareData`)

```
[Storage Test Results]
Sequential Read: 83 pts  |  1451 MB/s
Sequential Write: 82 pts  |  939 MB/s
Random 4K Read: 83 pts  |  702 MB/s
Small File Ops: 85 pts  |  16900 files/s
SQLite Performance: 81 pts  |  3636 txn/s
Mixed Workload: 83 pts  |  2576 MB/s
```

---

## 10. Navigation Wiring

```kotlin
// MainNavigation.kt — inside composable("benchmark/{type}/{preset}")
} else if (category == BenchmarkCategory.STORAGE) {
    val storageHistoryRepository = remember { HistoryRepository(db.benchmarkDao()) }
    StorageBenchmarkScreen(
        preset = preset,
        historyRepository = storageHistoryRepository,
        onBenchmarkComplete = { json ->
            val encoded = URLEncoder.encode(json, "UTF-8")
            navController.navigate("result/$encoded") {
                popUpTo("home") { inclusive = false }
            }
        },
        onNavBack = { navController.popBackStack() }
    )
}
```

---

## 11. Key Bug Fixes Applied During Development

| Bug | Symptom | Root Cause | Fix |
|---|---|---|---|
| SEQ_READ showed 6646 MB/s | Page-cache speed, not UFS | FileInputStream re-reads from RAM after first pass | JNI + `posix_fadvise(POSIX_FADV_DONTNEED)` |
| `Os.posix_fadvise` compile error | Build fails | Hidden API not in public SDK stubs | JNI C function instead |
| Fresh-file approach gave 683 MB/s | File creation counted in timing | `System.nanoTime()` file naming + `createTestFile()` inside timed loop | Pre-create file once outside loop in C code |
| SEQ_WRITE underestimated | 911 MB/s instead of ~1100 MB/s | No `fdatasync`; only OS page-cache fill | `fdatasync(fd)` per pass in JNI |
| SQLite = 0 txn/s | Crash during setup | `db.execSQL("PRAGMA journal_mode=WAL")` throws on Android 11+ (PRAGMA returns result row) | `db.rawQuery("PRAGMA …").use { it.moveToFirst() }` |
| All scores 100 pts | Score capped but refs too low | References calibrated below actual measurements | Raised to ~20% above measured ceiling |
| `0.000s` timing display | History screen showed all times as 0 | `executionTimeMs = 0.0` hardcoded in `buildResultJson` | Track `measureStartMs`, store `elapsedMs` in `StorageTestResult.durationMs` |
| `".score"` key typo | `individualScore` read as 0.0 from DB path | Failed string replacement introduced `".score"` instead of `"score"` | Fixed key to `"score"` |
