# CPU Benchmark Documentation

Welcome to the FinalBenchmark2 CPU Benchmark documentation.

## Documentation Structure

| Document | Description |
|----------|-------------|
| [OVERVIEW.md](OVERVIEW.md) | High-level overview of the CPU benchmark suite |
| [RUN_RULES.md](RUN_RULES.md) | Execution order, scoring methodology, and run guidelines |
| [SCORING.md](SCORING.md) | Reference machine, scaling factors, and score calculation |
| [BENCHMARKS.md](BENCHMARKS.md) | Detailed documentation of all 10 benchmarks |
| [CURRENT_PROBLEMS.md](CURRENT_PROBLEMS.md) | Known issues and recommended fixes |

## Quick Start

The CPU benchmark runs 20 tests in total:
- 10 Single-Core tests (pinned to fastest core)
- 10 Multi-Core tests (parallel across all cores)

### Execution Order

1. **Test Workload** (warm-up) - All 20 benchmarks with minimal parameters
2. **Single-Core Suite** - 10 individual benchmarks
3. **Multi-Core Suite** - 10 individual benchmarks
4. **Score Calculation** - Weighted sum with unified scaling factors

### Reference Machine

**Snapdragon 8 Gen 3** (OnePlus 12) is used as the reference device:
- Each benchmark is calibrated to score **~10 points** on the reference
- Same scaling factors used for both single-core and multi-core
- Enables easy understanding of multi-core scaling (8 cores = ~8× score)

## Architecture

```
cpuBenchmark/
├── KotlinBenchmarkManager.kt   # Main orchestrator, scoring, execution flow
├── BenchmarkEvent.kt           # Data classes for events and results
├── WorkloadParams              # Configurable workload parameters
├── CpuAffinityManager.kt       # CPU core pinning and priority management
└── algorithms/
    ├── BenchmarkHelpers.kt     # Shared algorithms used by both suites
    ├── SingleCoreBenchmarks.kt # 10 single-core benchmark implementations
    └── MultiCoreBenchmarks.kt  # 10 multi-core benchmark implementations
```

## Design Principles

1. **Unified Algorithms**: Single-core and multi-core use the same algorithms from `BenchmarkHelpers.kt`
2. **Fixed Work Per Core**: Multi-core runs the same workload on each core (total work scales with cores)
3. **Cache-Resident Strategy**: Small data sizes that fit in CPU cache to test compute, not memory bandwidth
4. **Thermal Management**: 1.5 second delays between benchmarks to prevent thermal throttling

## See Also

- [benchmark/](../benchmark/) - Additional benchmark documentation

