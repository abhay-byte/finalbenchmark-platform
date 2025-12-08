# CPU Benchmark Technical Specification

## Overview
This document provides comprehensive technical details for the CPU benchmarking algorithms used in the FinalBenchmark2 Android application. The benchmarking suite has been optimized for realistic, comparable scores across different device tiers.

## Scoring Formula
**Final Score** = (`SingleCore_Total` × 0.35) + (`MultiCore_Total` × 0.65)

## Target Score Ranges
- **Low-End Device:** ~1,000 - 3,000 points
- **Mid-Range Device:** ~4,000 - 7,000 points  
- **Flagship Device:** ~8,000 - 12,000+ points

## Benchmark Details & Calibration

### 1. Integer Performance Benchmarks

#### Prime Generation (Sieve of Eratosthenes)
- **Algorithm:** Optimized Sieve of Eratosthenes with reduced yield frequency
- **Complexity:** O(n log log n)
- **Workload:** Range of 250,000 numbers
- **Single-Core Scaling Factor:** `3.2e-9`
- **Multi-Core Scaling Factor:** `2.8e-9`
- **Expected Flagship Score:** ~1,250 points (400M operations/second)
- **Optimization:** Only yield every 10,000 iterations vs. original 1,000

#### Fibonacci Recursive
- **Algorithm:** Pure recursive implementation with memoization in multi-core
- **Complexity:** O(2^n) single-core, O(n) with memoization multi-core
- **Workload:** Calculate Fibonacci(30) to Fibonacci(32)
- **Single-Core Scaling Factor:** `2.9e-9` (Reference calibration)
- **Multi-Core Scaling Factor:** `1.2e-9`
- **Expected Flagship Score:** ~1,000 points single, ~1,200 points multi-core
- **Reference Data:** 343B ops/s → 1000pts (factor: 2.9e-9)

### 2. Floating-Point Performance Benchmarks

#### Matrix Multiplication
- **Algorithm:** Cache-optimized i-k-j loop order
- **Complexity:** O(n³)
- **Workload:** 350×350 matrix multiplication
- **Single-Core Scaling Factor:** `5.5e-8`
- **Multi-Core Scaling Factor:** `4.2e-8`
- **Expected Flagship Score:** ~1,100 points single, ~1,200 points multi-core
- **Optimization:** i-k-j loop order for better cache locality, yield every 50 rows (single) / 25 rows (multi)

#### Ray Tracing (Sphere Intersection)
- **Algorithm:** Recursive ray-sphere intersection with reflection
- **Workload:** 192×192 resolution, depth 3 recursion
- **Single-Core Scaling Factor:** `1.2e-4`
- **Multi-Core Scaling Factor:** `9.5e-5`
- **Expected Flagship Score:** ~1,020 points single, ~1,150 points multi-core
- **Expected Performance:** ~8.5M rays/second single, ~12M rays/second multi-core

### 3. Cryptographic Operations

#### Hash Computing (SHA-256)
- **Algorithm:** SHA-256 hashing with cache-friendly 4KB buffer
- **Complexity:** O(n)
- **Workload:** 300,000 iterations of 4KB data hashing
- **Single-Core Scaling Factor:** `1.5e-7`
- **Multi-Core Scaling Factor:** `1.1e-7`
- **Expected Flagship Score:** ~1,000 points single, ~1,100 points multi-core
- **Expected Performance:** ~6.5M hashes/second single, ~10M hashes/second multi-core

### 4. Data Processing Benchmarks

#### String Sorting (IntroSort)
- **Algorithm:** Kotlin's built-in sorted() (IntroSort implementation)
- **Complexity:** O(n log n) average case
- **Workload:** 15,000 random 50-character strings
- **Single-Core Scaling Factor:** `2.1e-5`
- **Multi-Core Scaling Factor:** `1.8e-5`
- **Expected Flagship Score:** ~1,050 points single, ~1,200 points multi-core
- **Expected Performance:** ~50k comparisons/second single, ~65k comparisons/second multi-core
- **Optimization:** Parallel string generation in multi-core version

#### Compression/Decompression (RLE)
- **Algorithm:** Run-Length Encoding with parallel processing
- **Workload:** 2MB buffer, 100 iterations
- **Single-Core Scaling Factor:** `1.4e-7`
- **Multi-Core Scaling Factor:** `1.2e-7`
- **Expected Flagship Score:** ~1,000 points single, ~1,200 points multi-core
- **Expected Performance:** ~7MB/s single, ~10MB/s multi-core
- **Optimization:** Increased buffer to 2MB for better cache utilization, reduced yield frequency

### 5. Statistical Computing

#### Monte Carlo π Estimation
- **Algorithm:** Monte Carlo simulation with random point generation
- **Workload:** 1,000,000 samples
- **Single-Core Scaling Factor:** `6.0e-7`
- **Multi-Core Scaling Factor:** `4.5e-7`
- **Expected Flagship Score:** ~1,000 points single, ~1,300 points multi-core
- **Expected Performance:** ~1.6M samples/second single, ~2.8M samples/second multi-core

#### JSON Parsing
- **Algorithm:** Custom JSON element counting parser
- **Workload:** 1MB complex nested JSON data
- **Single-Core Scaling Factor:** `2.2e-6`
- **Multi-Core Scaling Factor:** `1.8e-6`
- **Expected Flagship Score:** ~1,000 points single, ~1,200 points multi-core
- **Expected Performance:** ~450k elements/second single, ~650k elements/second multi-core

### 6. Combinatorial Optimization

#### N-Queens Problem
- **Algorithm:** Backtracking with constraint propagation
- **Workload:** 10×10 board size
- **Single-Core Scaling Factor:** `0.0025` (Reference calibration)
- **Multi-Core Scaling Factor:** `0.0018`
- **Expected Flagship Score:** ~1,000 points single, ~1,100 points multi-core
- **Reference Data:** 400k ops/s → 1000pts (factor: 0.0025)
- **Optimization:** Work-stealing approach in multi-core version

## Workload Parameters by Device Tier

### Standardized Parameters (Official Scoring)
```kotlin
WorkloadParams(
    primeRange = 250_000,              // Consistent across all tiers
    fibonacciNRange = Pair(30, 32),    // Consistent across all tiers
    matrixSize = 350,                  // Consistent across all tiers
    hashDataSizeMb = 2,                // Consistent across all tiers
    stringCount = 15_000,              // Consistent across all tiers
    rayTracingResolution = Pair(192, 192), // Consistent across all tiers
    rayTracingDepth = 3,               // Consistent across all tiers
    compressionDataSizeMb = 2,         // Consistent across all tiers
    monteCarloSamples = 1_000_000,     // Consistent across all tiers
    jsonDataSizeMb = 1,                // Consistent across all tiers
    nqueensSize = 10                   // Consistent across all tiers
)
```

### Legacy Device-Specific Parameters (Deprecated)
The old tier-based parameters (Slow/Mid/Flagship presets) are maintained for backward compatibility but are no longer used for official scoring to ensure fair comparison across devices.

## Optimization Changes Made

### Algorithm Optimizations
1. **Removed "Crisis Fixes":** Eliminated excessive `yield()` calls in tight loops
2. **Cache Optimization:** Implemented i-k-j loop order for matrix multiplication
3. **Memory Efficiency:** Increased buffer sizes to 2MB for better cache utilization
4. **Parallel Efficiency:** Improved work distribution across threads in multi-core benchmarks

### Performance Improvements
1. **Reduced Yield Frequency:** From every 32 rows to every 50 rows (single-core), every 25 rows (multi-core)
2. **Static Buffer Usage:** Pre-allocated buffers to prevent GC thrashing
3. **Workload Standardization:** Same parameters for all devices to ensure fair comparison
4. **Execution Time Target:** 1.5-2.0 seconds per benchmark on flagship devices

### Scoring Calibrations
- **Reference Points:** Based on real device performance data
- **Scaling Factors:** Calibrated to produce realistic 8K-12K scores for flagship devices
- **Weight Distribution:** 35% single-core, 65% multi-core reflects real-world usage patterns

## Quality Assurance

### Validation Checks
- All benchmarks include result validation (checksums, mathematical accuracy)
- Execution time monitoring to detect anomalies
- Memory usage optimization to prevent OOM errors
- Thermal management considerations for sustained performance

### Error Handling
- Graceful degradation for resource-constrained devices
- Fallback mechanisms for failed benchmarks
- Detailed metrics logging for performance analysis

## Performance Expectations

### Flagship Device (8+ Cores, 3GHz+)
- **Single-Core Score:** 3,500-4,500 points
- **Multi-Core Score:** 5,500-7,500 points  
- **Final Score:** 8,000-12,000 points
- **Benchmark Duration:** 1.5-2.0 seconds each

### Mid-Range Device (4-6 Cores, 2-2.5GHz)
- **Single-Core Score:** 2,000-3,000 points
- **Multi-Core Score:** 3,000-4,500 points
- **Final Score:** 4,000-7,000 points
- **Benchmark Duration:** 2-3 seconds each

### Low-End Device (2-4 Cores, 1.5-2GHz)
- **Single-Core Score:** 800-1,500 points
- **Multi-Core Score:** 1,200-2,500 points
- **Final Score:** 1,000-3,000 points
- **Benchmark Duration:** 3-5 seconds each

## Maintenance and Updates

### Version History
- **v2.0:** Major refactoring with crisis fixes removal and optimization
- **v1.0:** Initial implementation with tier-based parameters

### Future Considerations
- Adaptive workload sizing based on device capabilities
- Additional benchmark algorithms for comprehensive coverage
- Machine learning inference benchmarks
- Real-world application simulation tests

---

*This documentation reflects the optimized benchmarking system as of FinalBenchmark2 v2.0. For questions or contributions, please refer to the project repository.*