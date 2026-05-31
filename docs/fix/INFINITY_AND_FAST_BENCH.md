# Fix 1: Infinity crash on Mediatek
# Fix 2: Benchmarks too fast on all devices

---

## FIX 1: AI Benchmark Infinity → JSON Crash

### File: app/src/main/cpp/ai_benchmark_native.cpp

ADD after `double ms = ...` line in ALL THREE backends (ocl_matmul ~line 121, gles_matmul ~line 171, cpu_matmul ~line 216):

```cpp
double ms = std::chrono::duration<double,std::milli>(t1-t0).count()/iters;
if (ms <= 0.0) ms = 0.001;  // ADD THIS LINE - prevent Infinity from sub-μs timing
double tps = 2.0*(double)N*(double)N*(double)N/(ms/1000.0);
```

### File: app/src/main/java/com/ivarna/finalbenchmark2/utils/FormatUtils.kt

ADD at bottom of file (before closing brace of package):

```kotlin
/** Replace NaN/Infinity with 0.0 to prevent JSON serialization crash */
fun sanitizeDouble(value: Double): Double = when {
    value.isNaN() -> 0.0
    value.isInfinite() -> 0.0
    else -> value
}
```

### File: app/src/main/java/com/ivarna/finalbenchmark2/cpuBenchmark/KotlinBenchmarkManager.kt

ADD import at top:
```kotlin
import com.ivarna.finalbenchmark2.utils.sanitizeDouble
```

CHANGE in AI section (~line 274-287):
```kotlin
// BEFORE:
put("opsPerSecond", result.opsPerSecond)
put("executionTimeMs", result.executionTimeMs)
// ...
put("final_score", totalScore)
put("normalized_score", totalScore)

// AFTER:
put("opsPerSecond", sanitizeDouble(result.opsPerSecond))
put("executionTimeMs", sanitizeDouble(result.executionTimeMs))
// ...
put("final_score", sanitizeDouble(totalScore))
put("normalized_score", sanitizeDouble(totalScore))
```

CHANGE in CPU section (~lines 740, 756, 771-774):
```kotlin
// BEFORE at line ~740 and ~756:
put("opsPerSecond", result.opsPerSecond)
// AFTER:
put("opsPerSecond", sanitizeDouble(result.opsPerSecond))

// BEFORE at lines ~771-774:
put("single_core_score", calculatedSingleCoreScore)
put("multi_core_score", calculatedMultiCoreScore)
put("final_score", calculatedFinalScore)
put("normalized_score", calculatedNormalizedScore)
// AFTER:
put("single_core_score", sanitizeDouble(calculatedSingleCoreScore))
put("multi_core_score", sanitizeDouble(calculatedMultiCoreScore))
put("final_score", sanitizeDouble(calculatedFinalScore))
put("normalized_score", sanitizeDouble(calculatedNormalizedScore))
```

### File: app/src/main/java/com/ivarna/finalbenchmark2/ui/viewmodels/RamBenchmarkViewModel.kt

ADD import:
```kotlin
import com.ivarna.finalbenchmark2.utils.sanitizeDouble
```

CHANGE line ~467:
```kotlin
put("opsPerSecond", sanitizeDouble(r.value))
```

### File: app/src/main/java/com/ivarna/finalbenchmark2/ui/viewmodels/StorageBenchmarkViewModel.kt

ADD import. CHANGE line ~688:
```kotlin
put("opsPerSecond", sanitizeDouble(r.value))
```

### File: app/src/main/java/com/ivarna/finalbenchmark2/ui/viewmodels/ProductivityBenchmarkViewModel.kt

ADD import. CHANGE line ~1346:
```kotlin
put("opsPerSecond", sanitizeDouble(r.value))
```

---

## FIX 2: Benchmarks Too Fast

### File: app/src/main/java/com/ivarna/finalbenchmark2/cpuBenchmark/algorithms/BenchmarkHelpers.kt

CHANGE `countFactorsPollardRho` loop:
```kotlin
// BEFORE (line ~107):
for (n in 4..limit step 2) {  // Even numbers only for speed

// AFTER:
for (n in 4..limit) {  // Process ALL numbers for real CPU work
```

CHANGE comment on `pollardRho` (~line 126): Remove early-return for evens is fine - keep it. The fix above feeds odd numbers through the real Rho loop.

### File: app/src/main/java/com/ivarna/finalbenchmark2/cpuBenchmark/KotlinBenchmarkManager.kt

MULTIPLY flagship params by 3× (lines ~874-912):
```kotlin
"flagship" -> WorkloadParams(
    primeRange = 2_940_000_000,          // 3×
    fibonacciIterations = 125_000_000,   // 3×
    matrixIterations = 9_000,            // 3×
    hashIterations = 1_576_500_000,      // 3×
    stringSortIterations = 15_000,       // 3×
    rayTracingIterations = 2_400,        // 3×
    compressionIterations = 6_000,       // 3×
    monteCarloSamples = 150_000_000L,    // 3×
    jsonParsingIterations = 7_500,       // 3×
    nqueensSize = 17,                    // +1 (exponential)
    // rest unchanged
)
```

MULTIPLY mid params by 2× (lines ~850-872):
```kotlin
"mid" -> WorkloadParams(
    primeRange = 980_000_000,            // 2× (was 490M)
    fibonacciIterations = 41_666_667,    // 2×
    matrixIterations = 3_000,            // 2×
    hashIterations = 525_500_000,        // 2×
    stringSortIterations = 5_000,        // 2×
    rayTracingIterations = 800,          // 2×
    compressionIterations = 2_000,       // 2×
    monteCarloSamples = 50_000_000L,     // 2×
    jsonParsingIterations = 2_500,       // 2×
    nqueensSize = 16,                    // +1
    // rest unchanged
)
```

MULTIPLY slow params by 2× (lines ~830-848):
```kotlin
"slow" -> WorkloadParams(
    primeRange = 245_000_000,            // 2×
    fibonacciIterations = 10_416_667,    // 2×
    matrixIterations = 750,              // 2×
    hashIterations = 131_375_000,        // 2×
    stringSortIterations = 1_250,        // 2×
    rayTracingIterations = 200,          // 2×
    compressionIterations = 500,         // 2×
    monteCarloSamples = 12_500_000L,     // 2×
    jsonParsingIterations = 625,         // 2×
    nqueensSize = 13,                    // +1
    // rest unchanged
)
```

### AFTER BOTH FIXES: Recalibrate REFERENCE values

Run flagship benchmark on OnePlus CPH2691. Capture new ops/s values. Update REFERENCE_MOPS in KotlinBenchmarkManager.kt (~line 38):

```kotlin
val REFERENCE_MOPS = mapOf(
    BenchmarkName.PRIME_GENERATION to <NEW_VALUE>,
    BenchmarkName.FIBONACCI_ITERATIVE to <NEW_VALUE>,
    // ... all 10 entries updated
)
```

---

## Total

| Fix | Files | Lines | Risk |
|-----|-------|-------|------|
| Fix 1 — Infinity | 6 | ~25 | Low |
| Fix 2 — Fast bench | 2 | ~30 | Medium (scores change) |
| Recalibration | 1 | ~10 | Manual step |
