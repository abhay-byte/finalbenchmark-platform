# CPU Benchmark Overview

## What is FinalBenchmark2 CPU Suite?

FinalBenchmark2 CPU Suite is an Android benchmarking tool that measures CPU performance through 10 different computational workloads. Each workload is run in both single-core and multi-core modes to provide a comprehensive view of CPU capabilities.

---

## Suite Structure

### Two Main Suites

| Suite | Description | Core Usage |
|-------|-------------|------------|
| **Single-Core** | Sequential execution, pinned to fastest core | 1 core (Prime/X4) |
| **Multi-Core** | Parallel execution across all cores | All cores (8 typical) |

### 10 Benchmark Categories

| # | Benchmark | Type | What It Tests |
|---|-----------|------|---------------|
| 1 | Prime Generation | Integer | Pollard's Rho factorization (GCD + cycle detection) |
| 2 | Fibonacci Iterative | Integer | Pure ALU arithmetic throughput |
| 3 | Matrix Multiplication | Floating-Point | FMA operations, cache efficiency |
| 4 | Hash Computing | Integer | FNV-1a hash algorithm |
| 5 | String Sorting | Integer | Comparison-based sorting |
| 6 | Ray Tracing | Floating-Point | Perlin Noise 3D generation (gradient interpolation) |
| 7 | Compression | Integer | RLE compression algorithm |
| 8 | Monte Carlo π | Floating-Point | Mandelbrot Set iteration (fractal mathematics) |
| 9 | JSON Parsing | Integer | Binary format parsing |
| 10 | N-Queens | Integer | Backtracking algorithm |

---

## Execution Flow

```
┌─────────────────────────────────────────────────────────┐
│                    BENCHMARK EXECUTION                   │
├─────────────────────────────────────────────────────────┤
│  1. TEST WORKLOAD (Warm-up)                             │
│     ├── All 20 benchmarks with minimal parameters       │
│     ├── JIT compilation stabilization                   │
│     └── No scores recorded                              │
├─────────────────────────────────────────────────────────┤
│  2. SINGLE-CORE SUITE                                   │
│     ├── CpuAffinityManager.setLastCoreAffinity()        │
│     ├── Run each of 10 benchmarks                       │
│     ├── 1.5s thermal delay after each                   │
│     └── Record: ops/s, time, validity                   │
├─────────────────────────────────────────────────────────┤
│  3. MULTI-CORE SUITE                                    │
│     ├── Use highPriorityDispatcher (all cores)          │
│     ├── Run each of 10 benchmarks in parallel           │
│     ├── 1.5s thermal delay after each                   │
│     └── Record: ops/s, time, validity                   │
├─────────────────────────────────────────────────────────┤
│  4. SCORE CALCULATION                                   │
│     ├── Apply unified SCORING_FACTORS                   │
│     ├── Single-Core Score = Σ(ops/s × factor)           │
│     ├── Multi-Core Score = Σ(ops/s × factor)            │
│     └── Final Score = SC×0.35 + MC×0.65                 │
└─────────────────────────────────────────────────────────┘
```

---

## Core Affinity Management

### Single-Core Mode

```kotlin
// Pins thread to the highest-numbered core (typically Prime/X4)
CpuAffinityManager.setLastCoreAffinity()
CpuAffinityManager.setMaxPerformance()

// After benchmark
CpuAffinityManager.resetPerformance()
CpuAffinityManager.resetCpuAffinity()
```

### Multi-Core Mode

```kotlin
// Uses high-priority thread pool across all cores
private val highPriorityDispatcher = run {
    val threadFactory = ThreadFactory { runnable ->
        Thread(runnable).apply {
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_DISPLAY)
        }
    }
    Executors.newFixedThreadPool(numThreads, threadFactory)
        .asCoroutineDispatcher()
}
```

---

## Workload Strategy

### Fixed Work Per Core

FinalBenchmark2 uses a **Fixed Work Per Core** strategy:

- **Single-Core**: Runs N iterations on 1 core
- **Multi-Core**: Runs N iterations on EACH core (N × cores total)

This means:
- Execution time stays similar between modes
- Ops/second scales linearly with core count
- Multi-core score = ~8× single-core score on 8-core devices

### Cache-Resident Strategy

To prevent memory bandwidth bottlenecks:

| Benchmark | Data Size | Fits in Cache |
|-----------|-----------|---------------|
| Matrix | 128×128 (128 KB) | L2/L3 |
| Hash | 4 KB | L1 |
| String Sort | 4096 strings | L2 |
| JSON | 1 MB | L3 |

---

## Device Tier System

Workload parameters scale based on device tier:

| Tier | Target Runtime | Example Device |
|------|----------------|----------------|
| `test` | <1 second | Warm-up phase |
| `slow` | 1-5 seconds | Low-end devices |
| `mid` | 3-10 seconds | Mid-range devices |
| `flagship` | 5-20 seconds | High-end devices |

The tier is automatically detected or can be manually specified.

---

## Thermal Management

Each benchmark includes thermal stabilization:

```kotlin
// After benchmark completion (skip for test runs)
if (!isTestRun) {
    kotlinx.coroutines.delay(1500)  // 1.5 second delay
}
```

This prevents:
- Thermal throttling affecting subsequent benchmarks
- Inconsistent results due to accumulated heat
- Device damage from sustained high load

---

## Result Structure

Each benchmark returns a `BenchmarkResult`:

```kotlin
data class BenchmarkResult(
    val name: String,           // e.g., "Single-Core Matrix Multiplication"
    val executionTimeMs: Double, // Measured execution time
    val opsPerSecond: Double,   // Performance metric
    val isValid: Boolean,       // Validity check passed
    val metricsJson: String     // Detailed metrics as JSON
)
```

The `metricsJson` contains benchmark-specific details like:
- Algorithm used
- Iteration count
- Workload parameters
- Checksum/validation data
