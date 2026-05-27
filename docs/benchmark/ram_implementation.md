# RAM Benchmark — Complete Implementation Reference

## 1. Overview

The RAM benchmark measures five distinct aspects of memory subsystem performance on Android devices. It uses **native C code via JNI (Java Native Interface)** with **ARM NEON SIMD intrinsics** as its primary execution path, with a pure Kotlin/JVM fallback for non-ARM targets or when the `.so` fails to load.

### Why Native C Instead of Pure Kotlin/JVM?

| Root Cause | JVM Behaviour | Native (C + NEON) |
|---|---|---|
| Bounds-checks per element | ~1 ns/elem overhead | Eliminated by compiler |
| GC safepoints | Can pause loop at any point | None |
| No SIMD auto-vec guarantee | ART JIT is hit-or-miss | `vld1q_u64` × 4 = 64 B/iter guaranteed |
| JNI call overhead per loop | ~100–300 ns per call | Zero (loop is inside JNI call) |
| `-O3` dead-store elimination | JIT can't prove writes aren't dead | Requires `noinline` + asm constraint trick |

**Measured gap**: JVM LongArray reads ≈ 6,500 MB/s vs native NEON reads ≈ 34,112 MB/s on the same SD 8 Gen 3 / LPDDR5X device.

---

## 2. Architecture

```
Kotlin (UI Thread / viewModelScope)
    │
    ├── RamBenchmarkViewModel.kt         ← orchestration, scoring, DB save
    │       └── runTest(test, durationMs)
    │               ├── [Native path: .so loaded]
    │               │       └── RamNativeBridge.kt   ← object with System.loadLibrary()
    │               │               └── JNI → ram_benchmark.c
    │               └── [JVM fallback]
    │                       └── benchSeqRead / benchSeqWrite / etc.
    │
    ├── RamBenchmarkScreen.kt            ← Compose UI with glass morphism
    │
    └── MainNavigation.kt                ← routes category==RAM to RamBenchmarkScreen
```

### Files Changed / Created

| File | Type | Purpose |
|---|---|---|
| `app/src/main/cpp/ram_benchmark.c` | **NEW** | 5 JNI functions with ARM NEON + pthreads |
| `app/src/main/cpp/CMakeLists.txt` | **MODIFIED** | Added `ram_benchmark.c` to `vulkan_native` target |
| `app/src/main/java/.../utils/RamNativeBridge.kt` | **NEW** | Kotlin `object` bridging to JNI functions |
| `app/src/main/java/.../viewmodels/RamBenchmarkViewModel.kt` | **MODIFIED** | Native dispatch + JVM fallback + scoring |
| `app/src/main/java/.../screens/RamBenchmarkScreen.kt` | **EXISTING** | Compose UI (no structural change) |

---

## 3. Native Code: `ram_benchmark.c`

### 3.1 Build Flags

Inherited from `CMakeLists.txt`:

```cmake
target_compile_options(vulkan_native PRIVATE
    -O3 -ffast-math -funroll-loops -march=armv8-a+crypto
)
```

- `-O3`: Full optimisation — enables auto-vectorisation, loop unrolling, inlining
- `-ffast-math`: Allows reordering of floating-point ops (safe for integer-dominated RAM tests)
- `-funroll-loops`: Forces unrolling of inner measurement loops
- `-march=armv8-a+crypto`: Enables all AArch64 NEON instructions including crypto extensions

### 3.2 The Timing Problem and Its Fix

**Problem**: With `-O3`, Clang legally hoists `clock_gettime()` out of while-loops whose bodies (`memcpy`, NEON loads) are provably side-effect-free with respect to the clock. This makes the outer loop spin for the full `durationMs` counting bogus "copy" counts, giving results like 924,598,336 MB/s.

**Two-Part Fix**:

```c
// Part 1: noinline forces a real call through the PLT each iteration
static __attribute__((noinline)) int64_t now_ns(void) {
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return (int64_t)ts.tv_sec * 1000000000LL + (int64_t)ts.tv_nsec;
}

// Part 2: Memory clobber barrier prevents reordering of now_ns() around loops
#define COMPILER_BARRIER() do { __asm__ volatile("" ::: "memory"); } while (0)
```

Usage pattern in every timed loop:
```c
while (now_ns() < end_ns) {
    COMPILER_BARRIER();       // ← prevent hoisting start time
    /* ... NEON work ... */
    COMPILER_BARRIER();       // ← prevent hoisting end-time check
    total_bytes += BUF;
}
```

**Why `noinline` alone isn't enough for `memcpy`**: Clang inlines `memcpy(dst, src, CONSTANT)` as a NEON loop. Since `dst` is a local `malloc` that is only `free()`d and never read back, the compiler proves the writes are **dead stores** and eliminates the entire body — even across `COMPILER_BARRIER`. This required a second fix for the MemCopy test (see §3.7).

### 3.3 Test 1 — Sequential Read (`nativeSeqRead`)

**Goal**: Measure single-thread DRAM read bandwidth.

**Algorithm**:
1. Detect L3 cache size. Allocate `max(64MB, 2 × L3 cache)` + `memset(buf, 0xA5, BUF)` — faults all pages into physical RAM before timer starts.
2. Timed loop (`while now_ns() < end_ns`):
   - Inner loop over buffer in 64-byte strides:
     ```c
     __builtin_prefetch(p + 512, 0, 0);   // prefetch 8 cache lines ahead
     acc0 = vaddq_u64(acc0, vld1q_u64((uint64_t*)(p +  0)));
     acc1 = vaddq_u64(acc1, vld1q_u64((uint64_t*)(p + 16)));
     acc2 = vaddq_u64(acc2, vld1q_u64((uint64_t*)(p + 32)));
     acc3 = vaddq_u64(acc3, vld1q_u64((uint64_t*)(p + 48)));
     p += 64;
     ```
   - `4 × vld1q_u64` = 64 bytes/iteration (one full cache line)
   - Accumulation into `acc0..acc3` prevents compiler from treating reads as dead
3. `sink += vgetq_lane_u64(acc0, 0) + ...` — forces compiler to emit reads (anti-DCE)
4. Returns `total_bytes / (elapsed_sec) / (1024×1024)` → **MB/s**

**Non-x86 fallback**: 8× `uint64_t` word reads per iteration (compiler auto-vectorises).

**Reference (SD 8 Gen 3 / LPDDR5X)**: 34,112 MB/s

### 3.4 Test 2 — Sequential Write (`nativeSeqWrite`)

**Goal**: Measure single-thread DRAM write bandwidth.

**Algorithm**:
1. Detect L3 cache size. Allocate `max(64MB, 2 × L3 cache)` + `memset(buf, 0, BUF)` — page fault before timer.
2. Timed loop with inner loop in 64-byte strides:
   ```c
   COMPILER_BARRIER();
   // Write a rolling pattern sequentially incrementing per store to break write-combining
   uint8_t *p   = buf;
   uint8_t *end = buf + BUF;
   uint64_t val_u64 = pattern++;
   while (p < end) {
       __builtin_prefetch(p + 512, 1, 0);
       uint64x2_t val0 = vdupq_n_u64(val_u64 + 0);
       uint64x2_t val1 = vdupq_n_u64(val_u64 + 1);
       uint64x2_t val2 = vdupq_n_u64(val_u64 + 2);
       uint64x2_t val3 = vdupq_n_u64(val_u64 + 3);
       vst1q_u64((uint64_t*)(p +  0), val0);
       vst1q_u64((uint64_t*)(p + 16), val1);
       vst1q_u64((uint64_t*)(p + 32), val2);
       vst1q_u64((uint64_t*)(p + 48), val3);
       val_u64 += 4;
       p += 64;
   }
   pattern = val_u64;
   sink += buf[BUF/2];  // anti-DCE read
   ```
3. Varied write patterns prevent the optimizer from knowing they're the same, defeating whole-buffer dead-store elimination.
4. Returns **MB/s**

**Reference (SD 8 Gen 3 / LPDDR5X)**: 20,864 MB/s

### 3.5 Test 3 — Random Access Latency (`nativeRandAccess`)

**Goal**: Measure DRAM access latency (defeats CPU prefetcher via pointer-chase).

**Algorithm** (Hamiltonian pointer chain):
1. Detect L3 cache size. Scale the working set buffer size to `max(16 MB, 2 × L3 cache)` to ensure it spills out of the CPU cache. Let `COUNT = target_rand_buf_size / sizeof(int32_t)`.
2. Build a random permutation `perm` of size `COUNT` using Knuth shuffle (LCG seed = 42).
3. Build a true **Hamiltonian path** by mapping `chain[perm[i]] = perm[(i+1) % COUNT]`. This guarantees a single closed cycle visiting all elements exactly once, preventing sub-cycle caching and ensuring a DRAM access at every single hop.
4. Timed loop (8× unrolled to reduce loop overhead without hiding load latency):
   ```c
   while (now_ns() < end_ns) {
       idx = chain[idx]; idx = chain[idx]; /* × 8 */
       ops += 8;
   }
   ```
5. Returns `elapsed_ns / ops` → **ns/op** (lower = better)

**Why 2x L3 working set?** Guarantees that the pointer chase hits DRAM instead of being cached in the last-level cache.

**Reference (SD 8 Gen 3)**: 93.2 ns/op

### 3.6 Test 5 — Multi-threaded Bandwidth (`nativeMultiThread`)

**Goal**: Measure aggregate memory bandwidth across multiple CPU cores.

**Algorithm**:
1. Thread count = `detectBigCoreCount()` (cores with max frequency >= 75% of maximum core frequency, clamped between 2 and 16).
2. Buffer size per thread is dynamically scaled so that the aggregate size of all threads combined exceeds `2 × L3 cache size` (`BUF_T = max(16MB, 2xL3 / T)`).
3. Each thread independently:
   - Allocates and faults its own NEON read buffer
   - Runs same NEON `vld1q_u64` × 4 loop with COMPILER_BARRIER
   - Accumulates `bytes_done` in thread-private variable (no lock needed)
4. Main thread joins all via `pthread_join` (supports up to 64 threads).
5. Returns **sum of all thread byte_counts** / elapsed / MB → **aggregate MB/s**

**Thread creation**: `pthread_create` with default stack; no affinity pinning (Android restricts this without root). Threads naturally spread across cores under scheduler.

**Reference (SD 8 Gen 3 / LPDDR5X)**: 49,720 MB/s

### 3.7 Test 4 — Memory Copy (`nativeMemCopy`)

**Goal**: Measure Bionic libc `memcpy` throughput (hand-written NEON in Android's libc).

**Dead-Store Elimination Prevention**:
Clang inlines `memcpy(dst, src, BUF)` (constant size). Because `dst` is a local malloc pointer that is only `free()`d and never read back, Clang proves all writes to `dst` are **dead stores** and eliminates the memcpy body entirely.
This was fixed using a no-inline wrapper and an asm input constraint:

```c
// noinline prevents Clang from inlining the memcpy body
__attribute__((noinline)) static void do_memcpy_once(void *dst, const void *src, size_t n) {
    memcpy(dst, src, n);
    // "r"(dst) forces compiler to consider dst as "observed" (read), defeating dead-store elimination
    __asm__ volatile("" :: "r"(dst) : "memory");
}
```

**Algorithm**:
1. Detect L3 cache size. Allocate source and destination buffers with size `max(64 MB, 2 × L3 cache)`.
2. Timed loop (`while now_ns() < end_ns`):
   - Call `do_memcpy_once(dst, src, BUF)`
   - Accumulate `total_bytes += BUF`
3. Returns `total_bytes / elapsed_sec / (1024×1024)` → **MB/s**

**Reference (SD 8 Gen 3 / Bionic NEON memcpy)**: 20,071 MB/s

---

## 4. JNI Bridge: `RamNativeBridge.kt`

```kotlin
object RamNativeBridge {
    private var loaded = false

    fun load(): Boolean {
        if (loaded) return true
        return try {
            System.loadLibrary("vulkan_native")
            loaded = true
            true
        } catch (e: UnsatisfiedLinkError) { false }
    }

    val isAvailable: Boolean get() = loaded

    @JvmStatic external fun nativeSeqRead(durationMs: Long): Double      // MB/s
    @JvmStatic external fun nativeSeqWrite(durationMs: Long): Double     // MB/s
    @JvmStatic external fun nativeRandAccess(durationMs: Long): Double   // ns/op
    @JvmStatic external fun nativeMemCopy(durationMs: Long): Double      // MB/s
    @JvmStatic external fun nativeMultiThread(numThreads: Int, durationMs: Long): Double  // MB/s
}
```

The library is `vulkan_native` — the same `.so` that contains Vulkan GPU info and CPU affinity functions. Adding RAM to it avoids adding a new `System.loadLibrary` call and keeps the binary count low.

---

## 5. ViewModel: `RamBenchmarkViewModel.kt`

### 5.1 Scoring Formula

```kotlin
private fun RamTest.score(value: Double): Int {
    val ref = RAM_REFERENCE[this] ?: return 0
    val ratio = if (isLowerBetter()) ref / value.coerceAtLeast(0.1)
                else                 value / ref
    return (ratio * 100.0).roundToInt().coerceAtLeast(0)
}
```

- `RamTest.RAND_ACCESS` is "lower is better" (nanoseconds per operation)
- All others are "higher is better" (MB/s)
- Reference device = SD 8 Gen 3 / LPDDR5X = 100 pts per test

### 5.2 Total Score (Geometric Mean)

```kotlin
private fun calculateRamGeometricMean(results: List<RamTestResult>): Double {
    val ratios = results.map { r ->
        val ref = RAM_REFERENCE[r.test] ?: return@map 1.0
        if (r.test.isLowerBetter()) ref / r.value.coerceAtLeast(0.1) else r.value / ref
    }
    val product = ratios.fold(1.0) { acc, v -> acc * v.coerceAtLeast(1e-9) }
    return product.pow(1.0 / ratios.size) * 100.0
}
```

Geometric mean prevents any single outlier test from dominating the total score.

### 5.3 Reference Values

Two reference maps are defined; the active one is selected at init time:

```kotlin
private val RAM_REFERENCE_NATIVE = mapOf(
    RamTest.SEQ_READ     to 34_112.0,  // MB/s  (measured 34112 on SD 8 Gen 3)
    RamTest.SEQ_WRITE    to 20_864.0,  // MB/s  (measured 20864 on SD 8 Gen 3)
    RamTest.RAND_ACCESS  to 93.2,      // ns/op (measured 93.2 on SD 8 Gen 3; lower=better)
    RamTest.MEM_COPY     to 20_071.0,  // MB/s  (measured 20071 on SD 8 Gen 3)
    RamTest.MULTI_THREAD to 49_720.0,  // MB/s  (measured 49720 on SD 8 Gen 3)
)
private val RAM_REFERENCE_JVM = mapOf(
    RamTest.SEQ_READ     to 6_500.0,
    RamTest.SEQ_WRITE    to 3_200.0,
    RamTest.RAND_ACCESS  to 530.0,
    RamTest.MEM_COPY     to 11_000.0,
    RamTest.MULTI_THREAD to 11_500.0,
)

init {
    nativeAvailable = RamNativeBridge.load()
    if (nativeAvailable) RAM_REFERENCE = RAM_REFERENCE_NATIVE
}
```

### 5.4 Benchmark Loop Pattern

For each test:
1. **Warm-up** (500ms): Run test on `Dispatchers.Default`, discard result — primes caches and JIT
2. **Measure** (2000ms): Run test on `Dispatchers.Default`, record result
3. Progress ticks every 100ms on UI thread via `delay(TICK_MS)` + `update {}`
4. Native tests block the Default dispatcher for the full `durationMs` — this is intentional (we don't want coroutine interruptions mid-measurement)

---

## 6. Reference Calibration History

| Date | Event | Change |
|---|---|---|
| Initial (JVM only) | LongArray + Arrays.fill | SeqRead 5500, SeqWrite 7000 |
| JVM measured | Actual measurements on SD 8 Gen 3 | SeqRead 6500, SeqWrite 3200, Rand 530, MemCopy 11000, MT 11500 |
| Native v1 | First NEON build | SeqRead 20000, SeqWrite 16000 (estimates) |
| Native v2 (Bug 1) | MemCopy returned 924 billion MB/s | Bug: -O3 hoisted clock |
| Native v3 | Added `noinline` + COMPILER_BARRIER | MemCopy returned 875 trillion MB/s (Bug 2 still present) |
| Native v4 | `do_memcpy_once` + fixed-rep timing | All 5 tests give correct results |
| Calibrated | Actual measurements on SD 8 Gen 3 | SeqRead 34112, SeqWrite 20864, Rand 93.2 ns, MemCopy 20071, MT 49720 |

---

## 7. CMakeLists.txt Change

```cmake
add_library(vulkan_native SHARED
    vulkan_info.cpp
    cpu_info.cpp
    cpu_affinity.cpp
    ram_benchmark.c          # ← ADDED
)
target_link_libraries(vulkan_native
    vulkan android log
    ${log-lib}
)
```

`ram_benchmark.c` is pure C (not C++) to keep it lean and avoid C++ runtime overhead. It uses `<arm_neon.h>` (available in Clang for Android) and `<pthread.h>` (Bionic libc).

---

## 8. Key Lessons Learned

1. **Never trust `-O3` without barriers on timing loops** — Clang is smart enough to hoist `clock_gettime` if the loop body has no observable side effects on the clock.

2. **Dead-store elimination via `memcpy`** — A constant-size `memcpy` into a local never-read pointer is eliminated entirely, even when wrapped in `COMPILER_BARRIER()`. The `"r"(dst)` asm input constraint is the only reliable way to force the destination to be "observed".

3. **Fixed-repetition timing is more robust** than `while (now_ns() < end_ns)` for tests whose body might be eliminated — a fixed rep count will give near-zero time instead of infinite count.

4. **JVM vs native gap is 4–8× for DRAM bandwidth** — always use native for memory bandwidth measurements if accuracy matters.

5. **`COMPILER_BARRIER` must be in the same translation unit** — a `#define` that produces broken UTF-8 line continuations (due to tool-assisted edits) will silently fail to compile. Always verify `grep '#define COMPILER_BARRIER'` actually finds a single clean line.
