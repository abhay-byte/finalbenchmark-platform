# Reference Device Management

This document explains how to add new reference devices to the FinalBenchmark2 ranking and comparison system.

## Overview

The app maintains a hardcoded list of reference devices (flagship processors) that users can compare their benchmark results against. These devices appear in:
- **Rankings Screen**: Shows all reference devices ranked by performance
- **CPU Comparison Screen**: Allows detailed benchmark-by-benchmark comparison

## Data Format Requirements

### Understanding the Data Flow

The system uses two different units for performance data:
- **ops/s (operations per second)**: Raw benchmark results stored in database
- **Mops/s (millions of operations per second)**: Display format used in `BenchmarkDetails`
- **Points**: Calculated scores using `SCORING_FACTORS` for fair comparison

**Critical**: The `BenchmarkDetails` class stores values in **Mops/s**, but the database stores **ops/s**. Proper conversion is essential.

## Adding a New Reference Device

### Step 1: Gather Benchmark Data

You need the following data from a benchmark run:
1. **Overall Scores**:
   - Total/Final Score (normalized)
   - Single-Core Score
   - Multi-Core Score

2. **Individual Benchmark Results** (in Mops/s):
   - Single-Core: Prime Generation, Fibonacci, Matrix Multiplication, Hash Computing, String Sorting, Ray Tracing, Compression, Monte Carlo, JSON Parsing, N-Queens
   - Multi-Core: Same 10 benchmarks

### Step 2: Update RankingViewModel.kt

**File**: `app/src/main/java/com/ivarna/finalbenchmark2/ui/viewmodels/RankingViewModel.kt`

Add a new `RankingItem` to the `hardcodedReferenceDevices` list (around line 64):

```kotlin
private val hardcodedReferenceDevices =
    listOf(
        // Existing devices...
        
        RankingItem(
            name = "Device Name",  // e.g., "Snapdragon 8 Gen 3"
            normalizedScore = 313,  // Total score
            singleCore = 100,       // Single-core score
            multiCore = 420,        // Multi-core score
            isCurrentUser = false,
            tag = "Baseline",       // Optional: "Baseline" for reference device
            benchmarkDetails = BenchmarkDetails(
                // Single-Core Mops/s values
                singleCorePrimeNumberMops = 749.24,
                singleCoreFibonacciMops = 5.08,
                singleCoreMatrixMultiplicationMops = 3866.91,
                singleCoreHashComputingMops = 145.33,
                singleCoreStringSortingMops = 128.87,
                singleCoreRayTracingMops = 9.57,
                singleCoreCompressionMops = 761.08,
                singleCoreMonteCarloMops = 288.75,
                singleCoreJsonParsingMops = 191777.09,
                singleCoreNQueensMops = 162.15,
                // Multi-Core Mops/s values
                multiCorePrimeNumberMops = 3719.17,
                multiCoreFibonacciMops = 12.47,
                multiCoreMatrixMultiplicationMops = 14650.46,
                multiCoreHashComputingMops = 868.06,
                multiCoreStringSortingMops = 417.69,
                multiCoreRayTracingMops = 34.00,
                multiCoreCompressionMops = 3003.44,
                multiCoreMonteCarloMops = 1677.13,
                multiCoreJsonParsingMops = 911354.73,
                multiCoreNQueensMops = 705.80
            )
        )
    )
```

### Step 3: Update ResultScreen.kt

**File**: `app/src/main/java/com/ivarna/finalbenchmark2/ui/screens/ResultScreen.kt`

Update the `hardcodedReferenceDevices` list in the `RankingsTab` function (around line 1040):

```kotlin
val hardcodedReferenceDevices =
    listOf(
        // Existing devices...
        
        RankingItem(
            name = "Device Name",
            normalizedScore = 313,
            singleCore = 100,
            multiCore = 420,
            isCurrentUser = false,
            tag = "Baseline"  // Optional
        )
    )
```

**Note**: `ResultScreen.kt` doesn't need `benchmarkDetails` since it only displays overall scores.

## Data Conversion Guide

### Converting Database Results to Mops/s

When extracting data from benchmark results:

```kotlin
// Database stores ops/s, need to convert to Mops/s
val mopsValue = opsPerSecond / 1_000_000.0
```

Example:
- Database: `749_240_000.0` ops/s
- Display: `749.24` Mops/s

### How Scoring Works

The comparison system calculates points using:

```kotlin
// 1. Convert Mops/s to ops/s
val opsPerSecond = mops * 1_000_000.0

// 2. Apply scoring factor
val points = opsPerSecond * SCORING_FACTORS[benchmarkName]
```

**Example**:
- Prime Generation: `749.24 Mops/s`
- Convert: `749.24 * 1,000,000 = 749,240,000 ops/s`
- Score: `749,240,000 * 1.3563e-8 = 10.16 pts`

## Important Implementation Details

### Three Data Loading Paths

The system loads benchmark data in three places, each requiring proper conversion:

1. **CpuComparisonScreen.kt** (lines 87-93):
   ```kotlin
   fun findMops(prefix: String, testName: String): Double {
       val opsPerSecond = benchmarkResults
           .firstOrNull { it.name == "$prefix $testName" }
           ?.opsPerSecond ?: 0.0
       return opsPerSecond / 1_000_000.0  // Convert to Mops/s
   }
   ```

2. **RankingViewModel.kt** (lines 166-173):
   ```kotlin
   fun findMops(prefix: String, testName: String): Double {
       val opsPerSecond = benchmarkResults
           .firstOrNull { it.name == "$prefix $testName" }
           ?.opsPerSecond ?: 0.0
       return opsPerSecond / 1_000_000.0  // Convert to Mops/s
   }
   ```

3. **Hardcoded devices**: Manually enter Mops/s values in `benchmarkDetails`

### Score Calculation Functions

Both `getSingleCoreBenchmarkItems` and `getMultiCoreBenchmarkItems` in `CpuComparisonScreen.kt` use:

```kotlin
fun calculateScore(mops: Double, benchmarkName: BenchmarkName): Double {
    val opsPerSecond = mops * 1_000_000.0  // Convert Mops/s to ops/s
    return opsPerSecond * (KotlinBenchmarkManager.SCORING_FACTORS[benchmarkName] ?: 0.0)
}
```

## Testing New Devices

After adding a new device:

1. **Build the app**: `./gradlew assembleDebug`
2. **Test Rankings Screen**: Verify device appears in correct rank position
3. **Test Comparison**: 
   - Compare user device vs new device
   - Compare new device vs itself (self-comparison test)
   - Verify all individual benchmark scores display correctly as "X.XX pts"

## Common Issues and Solutions

### Issue: Scores showing as "0.00 pts"
**Cause**: `benchmarkDetails` is null or all values are 0.0  
**Solution**: Ensure `benchmarkDetails` is populated with correct Mops/s values

### Issue: Scores showing huge numbers like "8681960.45 pts"
**Cause**: Missing conversion from ops/s to Mops/s  
**Solution**: Check that `findMops()` functions include `/ 1_000_000.0` conversion

### Issue: Self-comparison shows incorrect scores on one side
**Cause**: `RankingViewModel.kt` `findMops()` not converting properly  
**Solution**: Verify conversion is applied in all three data loading paths

## Baseline Device Tag

The `tag` field allows marking special devices:
- `tag = "Baseline"`: Indicates the reference device used for scoring calculations
- Displays as a small badge next to device name in UI
- Optional field, can be `null` for regular devices

## File Locations Summary

- **RankingViewModel.kt**: Main reference device list with full benchmark details
- **ResultScreen.kt**: Simplified list for results comparison
- **CpuComparisonScreen.kt**: Handles user device data loading and score calculations
- **KotlinBenchmarkManager.kt**: Contains `SCORING_FACTORS` (read-only reference)

## Example: Complete Device Entry

```kotlin
RankingItem(
    name = "Snapdragon 8 Gen 3",
    normalizedScore = 313,
    singleCore = 100,
    multiCore = 420,
    isCurrentUser = false,
    tag = "Baseline",
    benchmarkDetails = BenchmarkDetails(
        singleCorePrimeNumberMops = 749.24,
        singleCoreFibonacciMops = 5.08,
        singleCoreMatrixMultiplicationMops = 3866.91,
        singleCoreHashComputingMops = 145.33,
        singleCoreStringSortingMops = 128.87,
        singleCoreRayTracingMops = 9.57,
        singleCoreCompressionMops = 761.08,
        singleCoreMonteCarloMops = 288.75,
        singleCoreJsonParsingMops = 191777.09,
        singleCoreNQueensMops = 162.15,
        multiCorePrimeNumberMops = 3719.17,
        multiCoreFibonacciMops = 12.47,
        multiCoreMatrixMultiplicationMops = 14650.46,
        multiCoreHashComputingMops = 868.06,
        multiCoreStringSortingMops = 417.69,
        multiCoreRayTracingMops = 34.00,
        multiCoreCompressionMops = 3003.44,
        multiCoreMonteCarloMops = 1677.13,
        multiCoreJsonParsingMops = 911354.73,
        multiCoreNQueensMops = 705.80
    )
)
```
