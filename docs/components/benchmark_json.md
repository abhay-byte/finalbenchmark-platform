# Benchmark JSON Structure Documentation

## Overview
This document describes the JSON structure used throughout the FinalBenchmark2 app for storing and transmitting benchmark results between components.

## JSON Structure

### Top-Level Structure
The benchmark system uses a single JSON object with the following keys:

```json
{
  "single_core_score": <double>,
  "multi_core_score": <double>,
  "final_score": <double>,
  "normalized_score": <double>,
  "rating": <string>,
  "detailed_results": [<array of BenchmarkResult objects>]
}
```

### Field Descriptions

#### Score Fields (Numbers)
- **`single_core_score`** (double): Weighted score from single-core benchmarks
- **`multi_core_score`** (double): Weighted score from multi-core benchmarks  
- **`final_score`** (double): Combined weighted score (35% single + 65% multi)
- **`normalized_score`** (double): Final normalized score for rankings

#### Rating Field (String)
- **`rating`** (string): Human-readable performance rating
  - Possible values:
    - `"★★★★★ (Exceptional Performance)"` - Score ≥ 1600.0
    - `"★★★★☆ (High Performance)"` - Score ≥ 1200.0
    - `"★★★☆☆ (Good Performance)"` - Score ≥ 800.0
    - `"★★☆☆☆ (Moderate Performance)"` - Score ≥ 500.0
    - `"★☆☆☆☆ (Basic Performance)"` - Score ≥ 250.0
    - `"☆☆☆☆☆ (Low Performance)"` - Score < 250.0

#### Detailed Results (Array)
- **`detailed_results`** (array): Array of individual benchmark test results

### Individual Benchmark Result Structure
Each object in the `detailed_results` array has the following structure:

```json
{
  "name": <string>,
  "opsPerSecond": <double>,
  "executionTimeMs": <double>,
  "isValid": <boolean>,
  "metricsJson": <string>
}
```

#### Benchmark Result Fields
- **`name`** (string): Display name of the benchmark test
  - Examples: `"Single-Core Prime Generation"`, `"Multi-Core Hash Computing"`
- **`opsPerSecond`** (double): Operations per second achieved
- **`executionTimeMs`** (double): Total execution time in milliseconds
- **`isValid`** (boolean): Whether the benchmark completed successfully
- **`metricsJson`** (string): JSON string containing test-specific metrics

## Complete Example

```json
{
  "single_core_score": 996596859.39,
  "multi_core_score": 6718628.80,
  "final_score": 353176009.51,
  "normalized_score": 141270.40,
  "rating": "★★★★★ (Exceptional Performance)",
  "detailed_results": [
    {
      "name": "Single-Core Prime Generation",
      "opsPerSecond": 625000.0,
      "executionTimeMs": 16.0,
      "isValid": true,
      "metricsJson": "{\"prime_count\":1229,\"range\":10000,\"optimization\":\"Reduced yield frequency for better performance\"}"
    },
    {
      "name": "Single-Core Fibonacci Recursive", 
      "opsPerSecond": 82458111580.52,
      "executionTimeMs": 8048.0,
      "isValid": true,
      "metricsJson": "{\"fibonacci_result\":832040000,\"target_n\":30,\"iterations\":1000,\"optimization\":\"Pure recursive, no memoization, repeated calculation for CPU load\"}"
    },
    {
      "name": "Multi-Core Prime Generation",
      "opsPerSecond": 322580.65,
      "executionTimeMs": 31.0,
      "isValid": true,
      "metricsJson": "{\"prime_count\":1229,\"range\":10000,\"threads\":8}"
    }
  ]
}
```

## Usage in Code

### Generation (KotlinBenchmarkManager.kt)
```kotlin
private fun calculateSummary(
    singleResults: List<BenchmarkResult>,
    multiResults: List<BenchmarkResult>
): String {
    // ... calculate scores ...
    
    val detailedResultsArray = JSONArray().apply {
        singleResults.forEach { result ->
            put(JSONObject().apply {
                put("name", result.name)
                put("opsPerSecond", result.opsPerSecond)
                put("executionTimeMs", result.executionTimeMs)
                put("isValid", result.isValid)
                put("metricsJson", result.metricsJson)
            })
        }
        multiResults.forEach { result -> /* same structure */ }
    }
    
    return JSONObject().apply {
        put("single_core_score", calculatedSingleCoreScore)
        put("multi_core_score", calculatedMultiCoreScore)
        put("final_score", calculatedFinalScore)
        put("normalized_score", calculatedNormalizedScore)
        put("rating", rating)
        put("detailed_results", detailedResultsArray)
    }.toString()
}
```

### Parsing (ResultScreen.kt)
```kotlin
val jsonObject = JSONObject(summaryJson)
val detailedResultsArray = jsonObject.optJSONArray("detailed_results")

if (detailedResultsArray != null) {
    for (i in 0 until detailedResultsArray.length()) {
        val resultObj = detailedResultsArray.getJSONObject(i)
        detailedResults.add(
            BenchmarkResult(
                name = resultObj.optString("name", "Unknown"),
                executionTimeMs = resultObj.optDouble("executionTimeMs", 0.0),
                opsPerSecond = resultObj.optDouble("opsPerSecond", 0.0),
                isValid = resultObj.optBoolean("isValid", false),
                metricsJson = resultObj.optString("metricsJson", "{}")
            )
        )
    }
}

val summary = BenchmarkSummary(
    singleCoreScore = jsonObject.optDouble("single_core_score", 0.0),
    multiCoreScore = jsonObject.optDouble("multi_core_score", 0.0),
    finalScore = jsonObject.optDouble("final_score", 0.0),
    normalizedScore = jsonObject.optDouble("normalized_score", 0.0),
    detailedResults = detailedResults
)
```

## Benchmarks Included

### Single-Core Tests (10 tests)
1. Single-Core Prime Generation
2. Single-Core Fibonacci Recursive
3. Single-Core Matrix Multiplication
4. Single-Core Hash Computing
5. Single-Core String Sorting
6. Single-Core Ray Tracing
7. Single-Core Compression
8. Single-Core Monte Carlo π
9. Single-Core JSON Parsing
10. Single-Core N-Queens

### Multi-Core Tests (10 tests)
1. Multi-Core Prime Generation
2. Multi-Core Fibonacci Recursive
3. Multi-Core Matrix Multiplication
4. Multi-Core Hash Computing
5. Multi-Core String Sorting
6. Multi-Core Ray Tracing
7. Multi-Core Compression
8. Multi-Core Monte Carlo π
9. Multi-Core JSON Parsing
10. Multi-Core N-Queens

## Error Handling

### Failed Benchmarks
When a benchmark fails, it returns a result with:
- `isValid`: `false`
- `opsPerSecond`: `0.0`
- `executionTimeMs`: `0.0`
- `metricsJson`: `"{\"error\": \"<exception message>\"}"`

### JSON Parsing Errors
If JSON parsing fails, the ResultScreen falls back to:
- All scores default to `0.0`
- Empty detailed results array
- Logs error details for debugging

## Validation Rules

### Score Ranges
- **single_core_score**: Typically 100,000 to 1,000,000,000+
- **multi_core_score**: Typically 1,000,000 to 50,000,000+
- **final_score**: Weighted combination of single and multi-core
- **normalized_score**: Typically 100 to 200,000+

### Required Fields
- All top-level score fields must be present (can be 0.0)
- `detailed_results` array must be present (can be empty)
- Each benchmark result must have all 5 fields

### Naming Convention
- Test names follow pattern: `"{CORE_TYPE}-{Test Name}"`
- CORE_TYPE: `"Single-Core"` or `"Multi-Core"`
- Test names match exactly with benchmark algorithm names

## Performance Considerations

### JSON Size
- Full result with 20 benchmarks: ~5-10KB
- Detailed metrics can increase size significantly
- Stored in Room database as TEXT field

### Parsing Performance
- JSONObject parsing is synchronous
- Should be done off-main thread for large datasets
- Caching parsed results recommended

## Version History

### v1.0 (Current)
- Initial JSON structure implementation
- 6 top-level fields + detailed_results array
- Support for both single and multi-core results
- Error handling with isValid flag

### Future Considerations
- Potential addition of timestamp field
- Device information embedding
- Compression for large metric datasets
- Version field for schema evolution