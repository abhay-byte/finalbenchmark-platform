# RAM Benchmark Audit — May 2026

## Device Under Test

| Field | Value |
|---|---|
| Device | OnePlus CPH2691 (OnePlus 12) |
| SoC | Snapdragon 8 Gen 3 (SM8650-AB, "pineapple") |
| RAM | LPDDR5X, ~76.8 GB/s peak |
| OS | Android 16 (API 36) |

## Test Overview

| Test | What It Measures | Unit | Native Method | Reference (SD8G3) |
|---|---|---|---|---|
| SEQ_READ | Single-core sequential read BW | MB/s | ARM NEON `vld1q_u64×4` + `__builtin_prefetch` on 64 MB | 35,936 |
| SEQ_WRITE | Single-core sequential write BW | MB/s | ARM NEON `vst1q_u64×4` + `__builtin_prefetch` on 64 MB | 20,832 |
| RAND_ACCESS | Random access latency (pointer chase) | ns/op | Knuth-shuffled `int32[4M]` = 16 MB, unrolled 8× | 71.7 |
| MEM_COPY | Memory copy throughput (libc memcpy) | MB/s | Bionic's hand-written NEON memcpy, `noinline` wrapper | 21,217 |
| MULTI_THREAD | Multi-core aggregate read BW | MB/s | 4× pthreads, each private 16 MB NEON read loop | 57,504 |

## Measured Results — Plausibility Analysis

### SEQ_READ — 35,936 MB/s (47% of LPDDR5X peak)

Single-core NEON reads 64 bytes/iteration with `__builtin_prefetch(p+512)`. At 35.9 GB/s → 560M iterations/s → inner loop throughput = 64B × 560M = 35.8 GB/s. CPU load unit can issue 2× 128-bit loads/cycle at 3.3 GHz = 105.6 GB/s load bandwidth. Actual 35.9 GB/s = 34% of load-unit theoretical. Gap from L1/L2/TLB misses + memory controller contention. **Plausible.**

### SEQ_WRITE — 20,832 MB/s (27% of LPDDR5X peak)

Store bandwidth is lower than load (limited store buffer depth, write-combining window). Ratio seq_write/seq_read = 0.58. Typical. **Plausible.**

But: `vdupq_n_u64(pattern++)` writes same 128-bit value across entire 64 MB. This is the best case for write-combining — CPU merges all stores into minimal DRAM bursts. Real workloads with diverse data patterns achieve lower write BW. This overstates realistic performance.

### RAND_ACCESS — 71.7 ns/op

LPDDR5X DRAM latency is 60-90 ns. 71.7 ns is in-range. **Plausible** — but see BUG-1 below.

### MEM_COPY — 21,217 MB/s

Bionic libc `memcpy` is hand-tuned NEON. Memcpy does 1 read + 1 write = 42.4 GB/s total memory traffic. 42.4/76.8 = 55% of peak. **Plausible.**

But: rep-based timing (clamped to 4-64 reps = 256 MB − 4 GB). On fast hardware, reps=64 completes in ~50ms, leaving 1950ms of measurement window unused → result based on 50ms sample → jitter-prone. See BUG-2.

### MULTI_THREAD — 57,504 MB/s (75% of LPDDR5X peak)

4 threads reading in parallel. 57.5/76.8 = 75% utilization. Memory controller near-saturation. **Plausible.**

But: thread count hard-capped at 4 (Kotlin side). On 8-core homogeneous (Dimensity 9300 all-big), leaves 50% bandwidth unused. On PC DDR5 with 16+ cores, severely underutilized. See BUG-3.

---

## Bugs — Measurement Distortion

### BUG-1: RAND_ACCESS pointer chain uses random permutation — no guaranteed Hamiltonian path (SEVERITY: HIGH)

**File**: `ram_benchmark.c:180-190`

**Root cause**: The chain is built by Knuth-shuffling an identity array. A random permutation of N elements forms disjoint cycles. The cycle containing idx=0 has expected length N/2 (2M entries = 8 MB for N=4M). On SD8G3 with 12 MB L3 cache, 8 MB fits entirely → the pointer chase hits L3 (~15-25 ns) not DRAM (~70 ns).

```c
// Current — chain is just a shuffled permutation, forms random cycles
for (size_t i = 0; i < COUNT; i++) chain[i] = (int32_t)i;
// Knuth shuffle in-place
// ... results in random permutation with disjoint cycles

// Pointer chase follows a single cycle
idx = chain[idx]; idx = chain[idx];  // cycle length unknown
```

The code's own comment acknowledges the correct approach but doesn't implement it:
```c
/* chain[perm[i]] = perm[(i+1) % COUNT] already forms a Hamiltonian path */
/* We directly use chain[idx] = chain[chain[idx]] as the next hop */
```

**Impact by platform**:

| Platform | L3/SLC | Cycle (8 MB expected) | What's Measured |
|---|---|---|---|
| SD 8 Gen 3 (12 MB L3) | 12 MB | 8 MB < 12 MB | L3 cache latency |
| Dimensity 9300 (8 MB L3) | 8 MB | 8 MB = 8 MB | borderline L3/DRAM |
| Apple A17 Pro (24 MB SLC) | 24 MB | 8 MB < 24 MB | SLC cache latency |
| AMD 7950X3D (96 MB L3) | 96 MB | 8 MB < 96 MB | L3 cache latency |
| Intel i9-14900K (36 MB L3) | 36 MB | 8 MB < 36 MB | L3 cache latency |

On devices with >12 MB last-level cache, this test measures **cache latency, not DRAM latency**. The measured 71.7 ns on SD8G3 is itself suspect: if cycle = 8 MB hits L3 at ~20 ns, why does it measure 71.7 ns? Possible explanations:
1. OS cache pressure evicts pages from L3
2. The cycle length random variance sometimes produces cycles >> 8 MB that spill to DRAM
3. ARM SLC on SD8G3 is shared across CPU+GPU+NPU → effective available SLC < 12 MB

Either way, **non-deterministic**. The cycle length varies per run based on the random permutation. Two consecutive benchmark runs on the same device can produce different `ns/op` results because different cycle lengths → different cache hit rates.

**Fix**: Build explicit Hamiltonian cycle:
```c
// After shuffling chain (which = perm):
int32_t *chain_hamilton = malloc(COUNT * sizeof(int32_t));
for (size_t i = 0; i < COUNT; i++)
    chain_hamilton[chain[i]] = chain[(i + 1) % COUNT];
// Now chain_hamilton is a single cycle visiting all COUNT entries.
// Every pointer-chase step is guaranteed to need a new cache line → true DRAM latency.
```

### BUG-2: MEM_COPY rep-based timing unreliable on fast hardware (SEVERITY: MEDIUM)

**File**: `ram_benchmark.c:255-265`

```c
int reps = ... clamped to [4, 64];  // max 64 × 64 MB = 4 GB
```

**Problem**: On fast memory (PC DDR5-6000 dual channel, ~80 GB/s memcpy), 4 GB completes in ~50ms. The measurement wall clock is 2000ms (durationMs). Result is based on a 50ms time sample. Jitter from scheduler preemption, DVFS transitions, or thermal throttling can swing the result ±10%.

The intent (avoiding DCE with a fixed rep count) is correct, but the rep cap at 64 is too low for fast hardware.

**Fix**: Use time-based loop with DCE protection, same pattern as seq-read:
```c
const int64_t end_ns = now_ns() + (int64_t)durationMs * 1000000LL;
int64_t total_bytes = 0;
volatile uint8_t *sink = dst;  /* prevent DCE */
while (now_ns() < end_ns) {
    COMPILER_BARRIER();
    do_memcpy_once(dst, src, BUF);
    total_bytes += BUF;
    COMPILER_BARRIER();
}
```

### BUG-3: MULTI_THREAD capped at 4 threads regardless of hardware (SEVERITY: MEDIUM)

**File**: `RamBenchmarkViewModel.kt:257-258`

```kotlin
Runtime.getRuntime().availableProcessors().coerceIn(2, 4)
```

**Intent**: Avoid efficiency cores on big.LITTLE (SD8G3 = 1+5+2). Using 6+ threads would place some on efficiency cores, dragging down aggregate BW.

**Problem**: No runtime detection of core topology. On homogeneous architectures (Dimensity 9300 all-big 8-core, most PC CPUs), capping at 4 leaves bandwidth unused. On a 16-core PC with DDR5-6000 dual channel, using only 4 threads severely underutilizes the memory controller.

**Fix**: Detect big cluster size at runtime:
```kotlin
// Read /sys/devices/system/cpu/cpu*/cpufreq/cpuinfo_max_freq
// Threads spawning above median frequency = big cores
val bigCoreCount = detectBigCoreCount()
RamNativeBridge.nativeMultiThread(bigCoreCount.coerceIn(2, 8), durationMs)
```

### BUG-4: Buffer size 64 MB constant — fits in large L3 caches on PC (SEVERITY: HIGH for PC)

**File**: `ram_benchmark.c:67` (SEQ_READ), line 123 (SEQ_WRITE), line 245 (MEM_COPY), line 342 (MULTI_THREAD uses 16 MB/thread)

| Platform | L3 Cache | 64 MB Buffer | What's Measured |
|---|---|---|---|
| SD 8 Gen 3 | 12 MB | 64 MB > 12 MB ✓ | DRAM |
| Apple M2 | 24 MB SLC | 64 MB > 24 MB ✓ | DRAM |
| AMD 7950X3D | 96 MB L3 per CCD | 64 MB < 96 MB ✗ | **L3 cache** (~1.5 TB/s) |
| Intel i9-14900K | 36 MB L3 | 64 MB > 36 MB ✓ | DRAM |

On AMD 3D V-Cache and Apple M2 Ultra (96 MB SLC), the entire test buffer fits in the last-level cache. The benchmark would report ~1,500 GB/s instead of ~80 GB/s — a **19× inflation**.

**Fix**: Scale buffer to `max(64 MB, 2 × L3 cache size)`. On PC: 256-512 MB. Implementation reads `/sys/devices/system/cpu/cpu0/cache/index3/size` (Linux) or `/proc/cpuinfo` (cache size field).

---

## Efficiency — Wasted Resources (No Measurement Distortion)

### EFF-1: SEQ_WRITE write-combining ideal case overstates real-world write BW

`vdupq_n_u64(pattern++)` writes the same 128-bit value across the entire 64 MB buffer. This is the best-case scenario for CPU store merging and write-combining buffers. Real workloads (memcpy, memset, structure copies) have diverse data patterns that break write-combining, achieving 20-40% lower write bandwidth.

**Mitigation**: Use a rolling pattern (different value per cache line) instead of broadcasting one value. Cost: `memset`-like pattern fill before timing loop (one-time cost, outside measurement).

### EFF-2: MULTI_THREAD pthread_create/pthread_join per benchmark run

`ram_benchmark.c:348-355`. Thread creation + destruction overhead ~1ms on Android. Acceptable for 2000ms measurement (~0.05% overhead). Thread pool would be cleaner but current approach is correct.

### EFF-3: No inter-test cache isolation

Each test inherits cache state from previous test. SEQ_READ fills the cache with read data. SEQ_WRITE dirties it. RAND_ACCESS walks through both. No explicit cache flush (`__builtin___clear_cache` on ARM, `_mm_clflush` on x86) between tests.

**Mitigation**: Tests are largely independent (different buffers, different access patterns). Cache interference is minor. Full isolation would require `memset` on a large buffer between tests, adding ~1ms overhead.

---

## Design Gaps

### GAP-1: No cache hierarchy test
No L1/L2/L3/DRAM bandwidth stair-step. Can't see whether a bottleneck is at L2 → L3 or L3 → DRAM. A strided-access test with increasing working set size would reveal cache sizes and per-level bandwidth.

### GAP-2: No NUMA awareness
On multi-socket or big.LITTLE with separate memory controllers, threads may land on wrong cluster. The MULTI_THREAD test spawns pthreads without CPU affinity (`sched_setaffinity`), so the kernel freely migrates threads, potentially measuring cross-cluster latency.

### GAP-3: No page-size effects tested
Android supports 4K, 16K (Android 15+), and some devices use 64K pages on kernel side. TLB miss rate heavily depends on page size. No test accounts for this.

### GAP-4: No stride-copy / scatter-gather test
memcpy (sequential) doesn't represent GPU texture uploads (strided copies), database joins (scatter/gather), or IPC (discontiguous buffer copies). Real-world memory BW is often limited by address generation, not raw copy speed.

### GAP-5: No bandwidth-per-core scaling
MULTI_THREAD uses a fixed thread count. A sweep from 1→N threads would reveal memory controller saturation point and per-core efficiency.

---

## Cross-Platform Readiness

| Concern | Mobile (LPDDR5X) | PC (DDR5-6000) |
|---|---|---|
| Buffer fits in L3? | 64 MB > 12 MB ✓ | 64 MB < 96 MB AMD 3D-V ✗ |
| Thread count adequate? | 4 on 8-core (big.LITTLE) | 4 on 16+ core ✗ |
| NEON intrinsics compile? | ✓ (ARM) | ✗ falls back to plain C |
| `__builtin_prefetch` works? | ✓ | ✓ |
| `clock_gettime` overhead OK? | ✓ (~200ns) | ✓ (~50ns) |
| `pthread` available? | ✓ (Bionic) | ✓ (glibc) |

---

## Proposed Fixes — Priority Matrix

| Priority | ID | What | Effort | Impact |
|---|---|---|---|---|
| **P0** | BUG-1 | Build Hamiltonian cycle for RAND_ACCESS pointer chase | Small | High — fixes L3 vs DRAM ambiguity |
| **P0** | BUG-4 | Scale buffer to 2× L3 cache for PC | Medium | High — prevents 19× inflation on PC |
| **P1** | BUG-2 | Time-based loop for MEM_COPY instead of rep count | Small | Medium — removes jitter on fast HW |
| **P1** | BUG-3 | Detect big-core count for MULTI_THREAD | Medium | Medium — utilizes available bandwidth |
| **P2** | EFF-1 | Use rolling pattern per cache line for SEQ_WRITE | Small | Low — more realistic write BW |
| **P2** | EFF-2 | Thread pool for MULTI_THREAD | Small | Low |
| **P3** | GAP-1 | Cache hierarchy stair-step test | Large | Medium |
| **P3** | GAP-2 | CPU affinity for MULTI_THREAD threads | Small | Low |
| **P3** | GAP-5 | Per-core bandwidth scaling sweep | Medium | Low |

---

## Appendix: Platform RAM Specs

### Mobile (LPDDR)

| Standard | Speed | Peak BW (64-bit) | Typical Latency |
|---|---|---|---|
| LPDDR5X | 8533 MT/s | 76.8 GB/s | 60-90 ns |
| LPDDR5 | 6400 MT/s | 51.2 GB/s | 70-100 ns |
| LPDDR4X | 4266 MT/s | 34.1 GB/s | 90-120 ns |
| LPDDR4 | 3733 MT/s | 29.9 GB/s | 100-130 ns |

### PC (DDR)

| Standard | Speed | Peak BW (single ch) | Peak BW (dual ch) | Typical Latency |
|---|---|---|---|---|
| DDR5 | 6000 MT/s | 48 GB/s | 96 GB/s | 65-85 ns |
| DDR5 | 5600 MT/s | 44.8 GB/s | 89.6 GB/s | 70-90 ns |
| DDR4 | 3200 MT/s | 25.6 GB/s | 51.2 GB/s | 55-75 ns |
| DDR4 | 2666 MT/s | 21.3 GB/s | 42.7 GB/s | 60-80 ns |

Note: DDR4 has lower absolute latency than DDR5 despite higher MT/s in DDR5, because DDR5 CAS latency in nanoseconds is comparable or higher.

### Why LPDDR5X Has Higher Latency Than DDR4

| Metric | LPDDR5X | DDR4-3200 |
|---|---|---|
| CAS Latency (cycles) | 32-40 | 14-18 |
| tCK (ns) | ~0.234 (8533 MT/s) | ~0.625 (3200 MT/s) |
| CAS Latency (ns) | ~7.5-9.4 ns | ~8.75-11.25 ns |
| Total Round-Trip | 60-90 ns | 55-75 ns |

LPDDR5X has lower CAS in ns but higher round-trip due to longer command bus arbitration (shared across multiple ranks / channels) and power-saving states (deep-sleep exit penalty). Desktop DDR4 keeps ranks powered up continuously.

### Single-Core Memory Bandwidth Limits

A single ARM Cortex-X4 core at 3.3 GHz:
- Load bandwidth: 2× 128-bit NEON loads/cycle = 32 bytes/cycle = 105.6 GB/s load BW
- Store bandwidth: 1× 128-bit NEON store/cycle = 16 bytes/cycle = 52.8 GB/s store BW
- Achievable: ~30-40% of theoretical due to L1/L2/TLB misses and memory-level parallelism limits

A single Zen 4 / Raptor Cove core at 5+ GHz:
- Load bandwidth: 3× 256-bit loads/cycle (Zen 4) or 2× 256-bit (Raptor Cove) = 48-96 bytes/cycle = 240-480 GB/s
- Achievable single-core: ~40-50 GB/s (Zen 4), ~35-40 GB/s (Raptor Cove)
