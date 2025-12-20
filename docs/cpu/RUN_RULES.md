# CPU Benchmark Run Rules

This document defines the execution order, requirements, and guidelines for running FinalBenchmark2 CPU benchmarks.

---

## Execution Order

The benchmark suite runs in a **fixed, deterministic order**:

### Phase 1: Test Workload (Warm-up)

**Purpose**: JIT compilation stabilization, cache warming, thread pool initialization

```kotlin
// KotlinBenchmarkManager.kt - runTestWorkload()
private suspend fun runTestWorkload() {
    val testParams = getWorkloadParams("test")  // Minimal parameters
    
    // Single-core warm-up (10 benchmarks)
    SingleCoreBenchmarks.primeGeneration(testParams, isTestRun = true)
    SingleCoreBenchmarks.fibonacciRecursive(testParams, isTestRun = true)
    SingleCoreBenchmarks.matrixMultiplication(testParams, isTestRun = true)
    SingleCoreBenchmarks.hashComputing(testParams, isTestRun = true)
    SingleCoreBenchmarks.stringSorting(testParams, isTestRun = true)
    SingleCoreBenchmarks.rayTracing(testParams, isTestRun = true)
    SingleCoreBenchmarks.compression(testParams, isTestRun = true)
    SingleCoreBenchmarks.monteCarloPi(testParams, isTestRun = true)
    SingleCoreBenchmarks.jsonParsing(testParams, isTestRun = true)
    SingleCoreBenchmarks.nqueens(testParams, isTestRun = true)
    
    // Multi-core warm-up (10 benchmarks)
    MultiCoreBenchmarks.primeGeneration(testParams, isTestRun = true)
    // ... same order
}
```

### Phase 2: Single-Core Suite

**Order**: Fixed sequence of 10 benchmarks

| Order | Benchmark | Function Call |
|-------|-----------|---------------|
| 1 | Prime Generation | `SingleCoreBenchmarks.primeGeneration(params)` |
| 2 | Fibonacci Iterative | `SingleCoreBenchmarks.fibonacciRecursive(params)` |
| 3 | Matrix Multiplication | `SingleCoreBenchmarks.matrixMultiplication(params)` |
| 4 | Hash Computing | `SingleCoreBenchmarks.hashComputing(params)` |
| 5 | String Sorting | `SingleCoreBenchmarks.stringSorting(params)` |
| 6 | Ray Tracing | `SingleCoreBenchmarks.rayTracing(params)` |
| 7 | Compression | `SingleCoreBenchmarks.compression(params)` |
| 8 | Monte Carlo π | `SingleCoreBenchmarks.monteCarloPi(params)` |
| 9 | JSON Parsing | `SingleCoreBenchmarks.jsonParsing(params)` |
| 10 | N-Queens | `SingleCoreBenchmarks.nqueens(params)` |

### Phase 3: Multi-Core Suite

**Order**: Same sequence as single-core, parallel execution

| Order | Benchmark | Function Call |
|-------|-----------|---------------|
| 1 | Prime Generation | `MultiCoreBenchmarks.primeGeneration(params)` |
| 2 | Fibonacci Iterative | `MultiCoreBenchmarks.fibonacciRecursive(params)` |
| 3 | Matrix Multiplication | `MultiCoreBenchmarks.matrixMultiplication(params)` |
| 4 | Hash Computing | `MultiCoreBenchmarks.hashComputing(params)` |
| 5 | String Sorting | `MultiCoreBenchmarks.stringSorting(params)` |
| 6 | Ray Tracing | `MultiCoreBenchmarks.rayTracing(params)` |
| 7 | Compression | `MultiCoreBenchmarks.compression(params)` |
| 8 | Monte Carlo π | `MultiCoreBenchmarks.monteCarloPi(params)` |
| 9 | JSON Parsing | `MultiCoreBenchmarks.jsonParsing(params)` |
| 10 | N-Queens | `MultiCoreBenchmarks.nqueens(params)` |

### Phase 4: Score Calculation

After all benchmarks complete, scores are calculated:

```kotlin
// Calculate single-core score
var calculatedSingleCoreScore = 0.0
for (result in singleResults) {
    val factor = SCORING_FACTORS[benchmarkName] ?: 0.0
    calculatedSingleCoreScore += result.opsPerSecond * factor
}

// Calculate multi-core score (same factors)
var calculatedMultiCoreScore = 0.0
for (result in multiResults) {
    val factor = SCORING_FACTORS[benchmarkName] ?: 0.0
    calculatedMultiCoreScore += result.opsPerSecond * factor
}

// Final weighted score
val calculatedFinalScore = (calculatedSingleCoreScore * 0.35) + (calculatedMultiCoreScore * 0.65)
```

---

## System Requirements

### Minimum Requirements

| Requirement | Value |
|-------------|-------|
| Android Version | 7.0 (API 24) |
| RAM | 2 GB |
| Free Storage | 100 MB |
| CPU Cores | 4 (8 recommended) |

### Recommended Conditions

For consistent, reliable results:

1. **Device State**:
   - Device plugged into power
   - Screen on (prevents deep sleep states)
   - No background apps running
   - Airplane mode enabled (optional)

2. **Thermal State**:
   - Device at room temperature
   - Not recently used for intensive tasks
   - Case removed for better cooling (optional)

3. **System State**:
   - Recent device restart recommended
   - Close all background applications
   - Disable adaptive battery features

---

## Single Run vs Multiple Runs

### Current Implementation

FinalBenchmark2 currently runs each benchmark **once** per session:

```kotlin
val singlePrimeResult = safeBenchmarkRun("Single-Core Prime Generation") {
    SingleCoreBenchmarks.primeGeneration(params)
}
```

### Why No Multiple Runs?

1. **Test Duration**: Full benchmark suite already takes 5-10 minutes
2. **User Experience**: Longer tests lead to user abandonment
3. **Thermal Impact**: Multiple runs cause thermal throttling
4. **Mobile Context**: Different expectations than desktop benchmarks

### Manual Repeatability

Users who want multiple runs can:
1. Run the full benchmark multiple times manually
2. Compare results across runs
3. Use median or minimum values for comparison

---

## Validation

Each benchmark includes basic validation:

```kotlin
// Example: Prime Generation
isValid = primeCount > 0

// Example: Fibonacci  
isValid = results > 0

// Example: Matrix Multiplication
isValid = checksum != 0L

// Example: Monte Carlo
isValid = timeMs > 0 && opsPerSecond > 0 && accuracy < accuracyThreshold
```

### What is Validated

| Benchmark | Validation Check |
|-----------|------------------|
| Prime | primeCount > 0 |
| Fibonacci | fibonacciSum > 0 |
| Matrix | checksum ≠ 0 |
| Hash | bytesProcessed > 0 |
| String Sort | checksum ≠ 0, timeMs > 0 |
| Ray Tracing | totalEnergy > 0 |
| Compression | bytesProcessed > 0 |
| Monte Carlo | accuracy < threshold |
| JSON | elementCount > 0, timeMs > 0 |
| N-Queens | solutions > 0 |

### What is NOT Validated

- Output correctness (no reference answers checked)
- Exact values (checksums are not compared to known values)
- Consistency (no cross-run comparison)

---

## Error Handling

Benchmarks are wrapped in `safeBenchmarkRun`:

```kotlin
private suspend fun safeBenchmarkRun(
    testName: String,
    block: suspend () -> BenchmarkResult
): BenchmarkResult {
    return try {
        withContext(Dispatchers.Default) {
            val result = block()
            Log.d(TAG, "✓ $testName completed successfully")
            result
        }
    } catch (e: Exception) {
        Log.e(TAG, "✗ $testName failed: ${e.message}", e)
        // Return dummy result so suite continues
        BenchmarkResult(
            name = testName,
            executionTimeMs = 0.0,
            opsPerSecond = 0.0,
            isValid = false,
            metricsJson = "{\"error\": \"${e.message}\"}"
        )
    }
}
```

If a benchmark fails:
- Error is logged
- Suite continues with remaining benchmarks
- Failed benchmark returns 0 ops/s (minimal impact on total score)

---

## Timing Considerations

### Measurement Method

All benchmarks use `System.currentTimeMillis()` or equivalent:

```kotlin
inline fun <T> measureBenchmark(block: () -> T): Pair<T, Long> {
    val startTime = System.currentTimeMillis()
    val result = block()
    val endTime = System.currentTimeMillis()
    return Pair(result, endTime - startTime)
}
```

### Thermal Delays

1.5 second delay after each benchmark:

```kotlin
if (!isTestRun) {
    kotlinx.coroutines.delay(1500)
}
```

### Expected Durations (Flagship Tier)

| Benchmark | Single-Core | Multi-Core |
|-----------|-------------|------------|
| Prime Generation | 12-25s | 17-25s |
| Fibonacci | 3-9s | 6-9s |
| Matrix Multiplication | 4-13s | 7-10s |
| Hash Computing | 3-6s | 4-6s |
| String Sorting | 2-7s | 5-7s |
| Ray Tracing | 18-21s | 3-5s |
| Compression | 6-16s | 12-16s |
| Monte Carlo | 6-18s | 10-18s |
| JSON Parsing | 8-19s | 17-20s |
| N-Queens | 1-3s | 2-3s |

**Total Duration**: ~5-10 minutes (including warm-up and delays)
