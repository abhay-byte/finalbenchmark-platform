# CPU Benchmark Scoring Methodology

This document explains how FinalBenchmark2 calculates benchmark scores.

---

## Reference Machine

### Device: **Snapdragon 8 Gen 3** (OnePlus 12)

The Snapdragon 8 Gen 3 serves as the **reference device** for calibrating scoring factors.

| Specification | Value |
|--------------|-------|
| **Prime Core** | 1× Cortex-X4 @ 3.3 GHz |
| **Performance Cores** | 3× Cortex-A720 @ 3.15 GHz + 2× Cortex-A720 @ 2.96 GHz |
| **Efficiency Cores** | 2× Cortex-A520 @ 2.27 GHz |
| **L3 Cache** | 12 MB |
| **TDP** | 8 W |
| **Process** | TSMC 4nm |

### Calibration Target

Each benchmark is calibrated so that the **reference device scores approximately 10 points** per benchmark:

```
Single-Core: 10 benchmarks × 10 points = ~100 points
Multi-Core: 10 benchmarks × 10 points × 8 cores = ~800 points (ideal scaling)
```

---

## Scaling Factors

### Unified Factors

The same scaling factors are used for both single-core and multi-core:

```kotlin
val SCORING_FACTORS = mapOf(
    BenchmarkName.PRIME_GENERATION to 1.7985e-6/12.5,      // ~1.44e-7
    BenchmarkName.FIBONACCI_ITERATIVE to 4.365e-7,         // ~4.37e-7
    BenchmarkName.MATRIX_MULTIPLICATION to 1.56465e-8/4,   // ~3.91e-9
    BenchmarkName.HASH_COMPUTING to 2.778e-5/2,            // ~1.39e-5
    BenchmarkName.STRING_SORTING to 1.602e-7/2,            // ~8.01e-8
    BenchmarkName.RAY_TRACING to 4.902e-6,                 // ~4.90e-6
    BenchmarkName.COMPRESSION to 1.5243e-8,                // ~1.52e-8
    BenchmarkName.MONTE_CARLO to 0.6125e-6/50,             // ~1.23e-8
    BenchmarkName.JSON_PARSING to 1.56e-6*4,               // ~6.24e-6
    BenchmarkName.N_QUEENS to 2.011e-7/2                   // ~1.01e-7
)
```

### Why Unified Factors?

Using the same factors for both modes provides:

1. **Easy Scaling Comparison**: 8-core device should score ~8× single-core
2. **Fair Comparison**: Same algorithm weighted equally in both modes
3. **Intuitive Understanding**: Score difference = performance difference

---

## Score Calculation

### SPEC CPU2017 Compliant Methodology

FinalBenchmark2 uses **geometric mean** for score calculation, following industry standards like SPEC CPU2017.

### Why Geometric Mean?

1. **Fair Comparison**: No single benchmark can dominate the overall score
2. **Industry Standard**: Used by SPEC CPU2017, Geekbench, and other professional benchmarks
3. **Mathematically Sound**: Properly handles ratios and relative performance
4. **Prevents Gaming**: Can't artificially boost score by optimizing one benchmark

### Step 1: Measure Operations Per Second

Each benchmark returns `opsPerSecond`:

```kotlin
val opsPerSecond = totalOperations.toDouble() / (timeMs / 1000.0)
```

### Step 2: Calculate Performance Ratios

For each benchmark, calculate the ratio of System Under Test (SUT) to Reference:

```kotlin
val ratio = sutMopsPerSecond / referenceMopsPerSecond
```

**Reference Device**: Snapdragon 8 Gen 3 (OnePlus Pad 2)

| Benchmark | Reference Mops/s |
|-----------|------------------|
| Prime Generation | 72.0 |
| Fibonacci | 45.3 |
| Matrix Multiplication | 4887.0 |
| Hash Computing | 0.78 |
| String Sorting | 124.6 |
| Ray Tracing | 2.84 |
| Compression | 750.3 |
| Monte Carlo | 801.6 |
| JSON Parsing | 1.33 |
| N-Queens | 160.5 |

### Step 3: Calculate Geometric Mean

```kotlin
// Geometric mean = (product of all ratios)^(1/n)
val product = ratios.reduce { acc, ratio -> acc * ratio }
val geometricMean = product.pow(1.0 / ratios.size)
```

### Step 4: Scale to 100-Point Baseline

```kotlin
// SD 8 Gen 3 = 100 points (baseline)
val score = geometricMean * 100.0
```

### Step 5: Calculate Final Score

```kotlin
// Weighted combination: 35% single-core, 65% multi-core
val finalScore = (singleCoreScore * 0.35) + (multiCoreScore * 0.65)
```

---

## Example Score Breakdown

### Snapdragon 8 Gen 3 (Reference)

With the **geometric mean** methodology, the reference device scores **100 points** by definition for both single-core and multi-core:

| Benchmark | Single-Core Mops/s | Ratio | Multi-Core Mops/s | Ratio |
|-----------|-------------------|-------|-------------------|-------|
| Prime Generation | 72.0 | 1.00 | 409.4 | 1.00 |
| Fibonacci | 45.3 | 1.00 | 160.9 | 1.00 |
| Matrix Mult | 3145 | 1.00 | 13714 | 1.00 |
| Hash Computing | 0.78 | 1.00 | 5.16 | 1.00 |
| String Sorting | 124.6 | 1.00 | 409.5 | 1.00 |
| Ray Tracing | 2.84 | 1.00 | 15.59 | 1.00 |
| Compression | 750.3 | 1.00 | 2874 | 1.00 |
| Monte Carlo | 801.6 | 1.00 | 3745 | 1.00 |
| JSON Parsing | 1.33 | 1.00 | 4.33 | 1.00 |
| N-Queens | 160.5 | 1.00 | 726.3 | 1.00 |
| **Geometric Mean** | | **1.00** | | **1.00** |
| **Score (×100)** | | **100** | | **100** |

**Final Score**: (100 × 0.35) + (100 × 0.65) = **100**

### Other Devices

Devices faster than SD 8 Gen 3 will score > 100, slower devices will score < 100.

**Example**: If a device is 20% faster on average across all benchmarks:
- Geometric mean of ratios = 1.20
- Score = 1.20 × 100 = **120 points**

---

## Multi-Core Scaling

### Expected Scaling

With **Fixed Work Per Core** strategy and unified factors:

| Cores | Expected MC/SC Ratio | Actual (8 Gen 3) |
|-------|---------------------|------------------|
| 1 | 1.0× | - |
| 4 | 4.0× | - |
| 8 | 8.0× | 4.5× |

### Why Not 8× Scaling?

Actual scaling is lower than ideal due to:

1. **Heterogeneous Cores**: Not all cores are equal (Prime + Big + Little)
2. **Memory Contention**: All cores share L3 cache and RAM bandwidth
3. **Thermal Throttling**: Heat from 8 cores reduces individual performance
4. **OS Overhead**: Thread scheduling, context switching

---

## Score Interpretation

### Rating System

| Score Range | Rating |
|-------------|--------|
| ≥1600 | ★★★★★ (Exceptional Performance) |
| ≥1200 | ★★★★☆ (High Performance) |
| ≥800 | ★★★☆☆ (Good Performance) |
| ≥500 | ★★☆☆☆ (Moderate Performance) |
| ≥250 | ★☆☆☆☆ (Basic Performance) |
| <250 | ☆☆☆☆☆ (Low Performance) |

### Device Examples

| Device | Total Score | Single-Core | Multi-Core |
|--------|-------------|-------------|------------|
| SD 8 Gen 3 | 403 | 123 | 553 |
| SD 8s Gen 3 | 298 | 98 | 406 |
| Dimensity 8300 | 327 | 105 | 446 |

---

## Validation Warning

The benchmark manager includes a validation check:

```kotlin
if (calculatedMultiCoreScore <= calculatedSingleCoreScore) {
    Log.w(TAG, "WARNING: Multi-core score is not higher than single-core score")
    Log.w(TAG, "This indicates a critical issue with benchmark implementations")
} else {
    Log.i(TAG, "✓ VALIDATED: Multi-core score is higher than single-core score")
    Log.i(TAG, "Multi-core advantage: ${multiCoreScore / singleCoreScore}x")
}
```

If multi-core ≤ single-core, something is wrong:
- Thread pool not working
- Benchmarks not parallelizing
- Thermal throttling too severe
