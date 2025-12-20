# Current Problems in CPU Benchmark Implementation

This document lists known issues in the current FinalBenchmark2 CPU benchmark implementation and proposed fixes.

---

## Summary

| Issue | Severity | Status |
|-------|----------|--------|
| [Arithmetic Sum Instead of Geometric Mean](#1-arithmetic-sum-instead-of-geometric-mean) | 🔴 Critical | ✅ **Resolved** |
| [No Output Validation](#2-no-output-validation) | 🔴 Critical | Open |
| [Single Run Without Repeatability](#3-single-run-without-repeatability) | 🟡 Medium | By Design |
| [Compressed Single-Core Variance](#4-compressed-single-core-variance) | 🟡 Medium | Under Analysis |
| [Random Number Generation Overhead](#5-random-number-generation-overhead) | 🟡 Medium | ✅ **Resolved** |
| [Memory-Bound Benchmarks](#6-memory-bound-benchmarks) | 🟡 Medium | Open |
| [Branch Predictor Sensitivity](#7-branch-predictor-sensitivity) | 🟢 Low | Open |

---

## Real-World Benchmark Comparison (3 Devices)

### Devices Tested

| Device | SoC | Prime Core | Big Cores | Little Cores | L3 Cache |
|--------|-----|------------|-----------|--------------|----------|
| **OnePlus Pad 2** | Snapdragon 8 Gen 3 | 1× X4 @ 3.3 GHz | 3× A720 @ 3.15 GHz + 2× A720 @ 2.96 GHz | 2× A520 @ 2.27 GHz | 12 MB |
| **Poco F6** | Snapdragon 8s Gen 3 | 1× X4 @ 3.0 GHz | 4× A720 @ 2.8 GHz | 3× A520 @ 2.0 GHz | 8 MB |
| **Poco X6 Pro** | Dimensity 8300 | 1× A715 @ 3.35 GHz | 3× A715 @ 3.2 GHz | 4× A510 @ 2.2 GHz | 4 MB |


### Overall Scores

| Device | Total Score | Single-Core | Multi-Core |
|--------|-------------|-------------|------------|
| **SD 8 Gen 3** | 403 | 123 | 553 |
| **SD 8s Gen 3** | 298 | 98 | 406 |
| **Dimensity 8300** | 327 | 105 | 446 |

**Expected Ranking**: 8 Gen 3 > 8s Gen 3 > Dimensity 8300 (X4 core is architecturally superior to A715)

**Actual Ranking**: ✓ Correct, but variance is too compressed

---

### Per-Benchmark Single-Core Analysis

| Benchmark | SD 8s Gen 3 | D8300 | SD 8 Gen 3 | D8300 vs 8s Gen 3 | Status |
|-----------|-------------|-------|------------|-------------------|--------|
| **Prime Generation** | 54.70 Mops/s | 71.91 Mops/s | 71.97 Mops/s | +31.5% | ✓ Good |
| **Fibonacci** | 26.71 Mops/s | 29.95 Mops/s | 45.29 Mops/s | +12.1% | ❌ Too low |
| **Matrix Multiplication** | 2882.68 Mops/s | 3320.91 Mops/s | 3144.94 Mops/s | +15.2% | ❌ Too low |
| **Hash Computing** | 0.65 Mops/s | 0.80 Mops/s | 0.78 Mops/s | +23.1% | ❌ Too low |
| **String Sorting** | 86.99 Mops/s | 88.15 Mops/s | 124.62 Mops/s | +1.3% | ❌ Way too low |
| **Ray Tracing** | 2.47 Mops/s | 2.84 Mops/s | 2.84 Mops/s | +15.0% | ❌ Too low |
| **Compression** | 693.50 Mops/s | 609.64 Mops/s | 750.32 Mops/s | **-12.1%** | ❌ Negative! |
| **Monte Carlo** | 482.06 Mops/s | 520.57 Mops/s | 801.64 Mops/s | +8.0% | ❌ Too low |
| **JSON Parsing** | 1.29 Mops/s | 1.26 Mops/s | 1.33 Mops/s | **-2.3%** | ❌ Negative! |
| **N-Queens** | 147.14 Mops/s | 132.56 Mops/s | 160.53 Mops/s | **-9.9%** | ❌ Negative! |

### Key Observations

1. **Only Prime Generation works correctly** (31.5% variance - matches architectural expectations)
2. **7 benchmarks show <20% variance** (too low - tests are not CPU-bound)
3. **3 benchmarks show NEGATIVE variance** (D8300 is slower despite higher frequency!)

### What Each Benchmark Actually Tests

| Benchmark | What It SHOULD Test | What It ACTUALLY Tests | Why Variance Is Wrong |
|-----------|---------------------|------------------------|----------------------|
| Prime Generation | CPU integer | ✓ CPU integer | ✓ Correct |
| Fibonacci | CPU arithmetic | Pure ALU only | Too narrow - no memory/cache |
| Matrix Mult | FPU compute | Random number generation | RNG dominates execution time |
| Hash Computing | Hashing speed | L1 cache speed | 4KB buffer fits in L1 |
| String Sorting | Sorting algorithm | L3 cache size | Large dataset, different L3 sizes |
| Ray Tracing | Graphics FPU | sqrt() hardware | Divider unit, not FPU |
| Compression | Compression speed | Memory bandwidth | Sequential 2MB buffer access |
| Monte Carlo | Math sampling | Leibniz series | Different than expected |
| JSON Parsing | Parsing speed | GC + memory allocator | String objects cause GC |
| N-Queens | Backtracking | Branch predictor | X4 has better branch predictor |

### Why Negative Variance Occurs

For 3 benchmarks, the Dimensity 8300 (3.35 GHz) is **slower** than SD 8s Gen 3 (3.0 GHz):

1. **Compression (-12.1%)**: Memory bandwidth bound - Qualcomm's memory controller is more aggressive
2. **JSON Parsing (-2.3%)**: GC-bound - Different garbage collector implementations
3. **N-Queens (-9.9%)**: Branch predictor bound - X4 has superior branch prediction


## 1. Arithmetic Sum Instead of Geometric Mean

### Severity: 🔴 Critical

### Problem

The current scoring uses **arithmetic sum** instead of **geometric mean**:

```kotlin
// Current implementation (KotlinBenchmarkManager.kt, lines 449-467)
var calculatedSingleCoreScore = 0.0
for (result in singleResults) {
    calculatedSingleCoreScore += result.opsPerSecond * factor  // SUM!
}

val calculatedFinalScore = 
    (calculatedSingleCoreScore * 0.35) + (calculatedMultiCoreScore * 0.65)  // WEIGHTED AVERAGE!
```

### Why This Matters

- **SPEC CPU2017** and all industry benchmarks use geometric mean
- Arithmetic sum allows one fast benchmark to dominate the score
- Not comparable to industry standards

### SPEC-Compliant Approach

```kotlin
// Correct implementation using geometric mean
fun calculateGeometricMean(results: List<BenchmarkResult>): Double {
    val ratios = results.map { result ->
        val refOps = referenceOpsPerSecond[result.name]!!
        result.opsPerSecond / refOps  // Performance ratio
    }
    
    // Geometric mean = (product)^(1/n)
    val product = ratios.reduce { acc, ratio -> acc * ratio }
    return product.pow(1.0 / ratios.size)
}
```

### Recommended Fix

Replace arithmetic sum with geometric mean calculation:

1. Store reference Mops/s for each benchmark (based on 8 Gen 3)
2. Calculate ratio: `SUT_Mops / Reference_Mops`
3. Compute geometric mean of all ratios
4. Scale to desired score range

### ✅ Resolution (Implemented)

**Date**: 2025-12-20

**Implementation**: Replaced arithmetic sum with SPEC CPU2017-compliant geometric mean calculation in `KotlinBenchmarkManager.kt`:

1. **Added Reference Values**: Created `REFERENCE_MOPS` map with SD 8 Gen 3 baseline performance for all 10 benchmarks
2. **Implemented Geometric Mean Function**: 
   - Calculates performance ratios (SUT/Reference) for each benchmark
   - Computes geometric mean: `(∏ ratios)^(1/n)`
   - Scales to 100-point baseline (SD 8 Gen 3 = 100)
3. **Updated Score Calculation**: Modified `calculateSummary()` to use geometric mean for both single-core and multi-core scores
4. **Updated Documentation**: Updated `SCORING.md` with new methodology and examples

**Benefits**:
- ✅ SPEC CPU2017 compliant
- ✅ No single benchmark can dominate the score
- ✅ Industry-standard comparability
- ✅ Fair comparison across different device architectures
- ✅ Reference device (SD 8 Gen 3) scores exactly 100 points

**Files Modified**:
- `app/src/main/java/com/ivarna/finalbenchmark2/cpuBenchmark/KotlinBenchmarkManager.kt`
- `docs/cpu/SCORING.md`
- `docs/cpu/CURRENT_PROBLEMS.md`

---

## 2. No Output Validation

### Severity: 🔴 Critical

### Problem

Benchmark results are not validated for correctness. A buggy implementation could return fast but wrong results:

```kotlin
// Current validation (too permissive)
isValid = primeCount > 0        // ❌ Could be wrong count
isValid = fibonacciSum > 0      // ❌ Could overflow/underflow
isValid = checksum != 0L        // ❌ Wrong checksum still passes
```

### Why This Matters

- A bug could make benchmarks complete 10× faster with wrong answers
- No way to verify computational correctness
- Results are meaningless if work wasn't done correctly

### How to Implement Output Validation

Add known correct answers for verification:

```kotlin
// Known correct values
object BenchmarkValidation {
    // Prime count for known limits
    val PRIME_COUNTS = mapOf(
        1_000_000 to 78_498L,
        10_000_000 to 664_579L,
        100_000_000 to 5_761_455L,
        900_000_000 to 45_086_079L  // Add this
    )
    
    // Fibonacci values
    const val FIB_35 = 9227465L
    const val FIB_40 = 102334155L
    
    // N-Queens solutions
    val NQUEENS_SOLUTIONS = mapOf(
        10 to 724L,
        12 to 14_200L,
        15 to 2_279_184L
    )
}

// Validation example
fun validatePrimeGeneration(result: Long, limit: Int): Boolean {
    val expected = BenchmarkValidation.PRIME_COUNTS[limit]
    return expected == null || result == expected
}
```

### Recommended Fix

1. Define known correct values for each benchmark
2. Validate results after computation
3. Mark as invalid if results don't match expected values
4. Log validation failures for debugging

---

## 3. Single Run Without Repeatability

### Severity: 🟡 Medium

### Problem

Each benchmark runs only once per session. SPEC CPU2017 requires 2-3 runs with median/minimum selection.

### Why This Is By Design

1. **Long Duration**: Full suite already takes 5-10 minutes
2. **User Experience**: Mobile users expect quick results
3. **Thermal Constraints**: Multiple runs cause throttling
4. **Mobile Context**: Different expectations than desktop benchmarks

### SPEC Approach (Not Recommended for Mobile)

```kotlin
// SPEC-style repeatability (adds 10-20 minutes)
suspend fun runBenchmarkWithRepeatability(
    benchmark: suspend () -> BenchmarkResult,
    iterations: Int = 3
): BenchmarkResult {
    val results = mutableListOf<BenchmarkResult>()
    
    repeat(iterations) {
        results.add(benchmark())
        delay(5000)  // Thermal cooldown
    }
    
    // Select median
    return results.sortedBy { it.executionTimeMs }[1]
}
```

### Current Mitigation

Users can run benchmarks multiple times manually and compare results.

---

## 4. Compressed Single-Core Variance

### Severity: 🟡 Medium

### Problem

Single-core scores show insufficient variance between devices:

| Comparison | Expected Variance | Actual Variance |
|------------|-------------------|-----------------|
| 8 Gen 3 vs 8s Gen 3 | 25-30% | ~25% ✓ |
| 8s Gen 3 vs D8300 | 20-30% | ~7% ❌ |

Only 7% difference between SD 8s Gen 3 (98 pts) and Dimensity 8300 (105 pts), despite significant architectural differences.

### Root Cause Analysis

9 out of 10 benchmarks test the wrong thing:

| Benchmark | Expected Test | Actual Test | Variance |
|-----------|---------------|-------------|----------|
| Prime | CPU integer | ✓ CPU integer | 31.5% ✓ |
| Fibonacci | CPU arithmetic | ALU only | 12.1% |
| Matrix | FPU compute | RNG + memory | 15.2% |
| Hash | Hashing | L1 cache | 23.1% |
| String Sort | Sorting | L3 cache | 1.3% |
| Ray Tracing | Graphics FPU | sqrt hardware | 15.0% |
| Compression | Compression | Memory bandwidth | -12.1% ❌ |
| Monte Carlo | Sampling | RNG algorithm | 8.0% |
| JSON | Parsing | GC overhead | -2.3% ❌ |
| N-Queens | Backtracking | Branch predictor | -9.9% ❌ |

### Recommended Fixes

See individual benchmark issues below for specific recommendations.

---

## 5. Random Number Generation Overhead

### Severity: 🟡 Medium

### Problem

**Matrix Multiplication** spends more time in RNG than in actual matrix math:

```kotlin
// Current implementation (BenchmarkHelpers.kt)
repeat(repetitions) { rep ->
    random.setSeed(System.nanoTime() + rep)  // ⚠️ System call
    
    for (i in 0 until size) {
        for (j in 0 until size) {
            a[i][j] = random.nextDouble()  // ⚠️ RNG call
            b[i][j] = random.nextDouble()  // ⚠️ RNG call
        }
    }
    
    // Actual matrix multiplication (only ~30% of time)
    // ...
}
```

For 128×128 matrices with 3000 iterations:
- RNG calls: 128 × 128 × 2 × 3000 = **98.3 million**
- Matrix ops: 128³ × 2 × 3000 = **12.6 billion**

But RNG is **memory-bound** and dominates execution time.

### Recommended Fix

Initialize matrices once or use deterministic values:

```kotlin
// Option 1: Initialize once outside timing
val a = Array(size) { i -> DoubleArray(size) { j -> (i * size + j).toDouble() / (size * size) } }
val b = Array(size) { i -> DoubleArray(size) { j -> (j * size + i).toDouble() / (size * size) } }

val (checksum, timeMs) = measureBenchmark {
    repeat(repetitions) {
        // Reset C matrix only
        for (i in 0 until size) {
            for (j in 0 until size) {
                c[i][j] = 0.0
            }
        }
        
        // Multiply (now dominates execution)
        for (i in 0 until size) {
            for (k in 0 until size) {
                val aik = a[i][k]
                for (j in 0 until size) {
                    c[i][j] += aik * b[k][j]
                }
            }
        }
    }
}
```

---

## 6. Memory-Bound Benchmarks

### Severity: 🟡 Medium

### Problem

Several benchmarks are memory-bound rather than CPU-bound:

| Benchmark | Issue | Impact |
|-----------|-------|--------|
| Compression | Sequential 2MB buffer access | Tests memory bandwidth |
| JSON Parsing | String allocation + GC | Tests memory allocator |
| String Sorting | Large dataset copies | Tests L3 cache size |

### Why This Matters

- All modern SoCs have similar memory bandwidth (~50 GB/s)
- Memory-bound tests show minimal variance between CPUs
- Tests L3 cache and memory controller, not CPU cores

### Recommended Fixes

1. **Compression**: Use smaller buffer (256 KB fits in L2 cache)
2. **JSON**: Pre-parse JSON, test traversal only
3. **String Sorting**: Use smaller dataset (already mitigated with 4096 strings)

---

## 7. Branch Predictor Sensitivity

### Severity: 🟢 Low

### Problem

**N-Queens** backtracking heavily tests branch prediction:

```kotlin
fun solve(row: Int, cols: Int, diag1: Int, diag2: Int) {
    iterations++
    if (row == n) {          // ← Branch 1
        solutions++
        return
    }
    
    var availablePositions = ...
    while (availablePositions != 0) {  // ← Branch 2
        // ... 
        solve(row + 1, ...)  // ← Recursive call (return stack)
    }
}
```

Different CPUs have different branch predictor designs:
- Qualcomm X4: Superior branch predictor
- Arm A715: Different predictor architecture

This causes anomalous results where higher-frequency chips perform worse.

### Impact

D8300 (3.35 GHz A715) is **9.9% slower** than SD 8s Gen 3 (3.0 GHz X4) on N-Queens, despite higher frequency.

### Possible Mitigations

1. Add iterative version alongside recursive
2. Reduce recursion depth by solving partial board first
3. Use this as a separate "branch prediction" sub-score

---

## Action Items

### High Priority

1. ✅ ~~Change scoring from arithmetic sum to geometric mean~~ **COMPLETED 2025-12-20**
2. ☐ Add output validation with known correct values
3. ☐ Fix Matrix Multiplication RNG overhead

### Medium Priority

4. ☐ Reduce Compression buffer size
5. ☐ Optimize JSON parsing memory allocation
6. ☐ Review Hash Computing stride and buffer size

### Low Priority

7. ☐ Consider N-Queens iterative implementation
8. ☐ Add optional repeatability mode for power users
9. ☐ Document expected variance for each benchmark

---

## See Also

- [SCORING.md](SCORING.md) - Current scoring methodology
- [BENCHMARKS.md](BENCHMARKS.md) - Benchmark implementations

