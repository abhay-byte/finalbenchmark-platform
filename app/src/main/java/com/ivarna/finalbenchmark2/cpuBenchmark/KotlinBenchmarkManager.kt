package com.ivarna.finalbenchmark2.cpuBenchmark

import android.util.Log
import com.ivarna.finalbenchmark2.cpuBenchmark.algorithms.MultiCoreBenchmarks
import com.ivarna.finalbenchmark2.cpuBenchmark.algorithms.SingleCoreBenchmarks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class KotlinBenchmarkManager {
        private val _benchmarkEvents = MutableSharedFlow<BenchmarkEvent>()
        val benchmarkEvents: SharedFlow<BenchmarkEvent> = _benchmarkEvents.asSharedFlow()

        private val _benchmarkComplete = MutableSharedFlow<String>()
        val benchmarkComplete: SharedFlow<String> = _benchmarkComplete.asSharedFlow()

        companion object {
                private const val TAG = "KotlinBenchmarkManager"


        // Reference device: Snapdragon 8 Gen 3 (OnePlus Pad 2)
        // These are the baseline ops/s values used for geometric mean calculation
        // Note: Values are in ops/s, not Mops/s, to match benchmark result format
        val REFERENCE_MOPS = mapOf(
                BenchmarkName.PRIME_GENERATION to 72_000_000.0,           // 72.0 Mops/s
                BenchmarkName.FIBONACCI_ITERATIVE to 45_300_000.0,        // 45.3 Mops/s
                BenchmarkName.MATRIX_MULTIPLICATION to 4_887_000_000.0,   // 4887.0 Mops/s (deterministic init)
                BenchmarkName.HASH_COMPUTING to 780_000.0,                // 0.78 Mops/s
                BenchmarkName.STRING_SORTING to 124_600_000.0,            // 124.6 Mops/s
                BenchmarkName.RAY_TRACING to 2_840_000.0,                 // 2.84 Mops/s
                BenchmarkName.COMPRESSION to 750_300_000.0,               // 750.3 Mops/s
                BenchmarkName.MONTE_CARLO to 801_600_000.0,               // 801.6 Mops/s
                BenchmarkName.JSON_PARSING to 1_330_000.0,                // 1.33 Mops/s
                BenchmarkName.N_QUEENS to 160_500_000.0                   // 160.5 Mops/s
        )

        val SCORING_FACTORS =
        mapOf(
                // Target 20 / Performance (Mops/s)
                BenchmarkName.PRIME_GENERATION to 1.7985e-6/12.5,        // 20 / 2.90e6 ops/s        
                BenchmarkName.FIBONACCI_ITERATIVE to 4.365e-7,     // 20 / 22.91 Mops/s
                BenchmarkName.MATRIX_MULTIPLICATION to 1.56465e-8/7.2,  // 20 / 639.13 Mops/s
                BenchmarkName.HASH_COMPUTING to 2.778e-5/2,          // 20 / 0.36 Mops/s
                BenchmarkName.STRING_SORTING to 1.602e-7/2,          // 20 / 62.42 Mops/s
                BenchmarkName.RAY_TRACING to 4.902e-6,             // 20 / 2.04 Mops/s
                BenchmarkName.COMPRESSION to 1.5243e-8,            // 20 / 656.04 Mops/s
                BenchmarkName.MONTE_CARLO to 0.6125e-6/50,             // 20 / 16.32 Mops/s
                BenchmarkName.JSON_PARSING to 1.56e-6*4,            // 20 / 6.41 Mops/s
                BenchmarkName.N_QUEENS to 2.011e-7/2                 // 20 / 66.18e6 ops/s
        )

        }

        /**
         * Run test workload before actual benchmarks
         * - Uses minimal "test" workload parameters
         * - No delays between benchmarks
         * - Results are NOT recorded
         * - Purpose: Warm up device and stabilize performance
         */
        private suspend fun runTestWorkload() {
                Log.d(TAG, "=== STARTING TEST WORKLOAD (Warm-up) ===")
                val testParams = getWorkloadParams("test")
                val isTestRun = true

                // Emit test workload start event
                _benchmarkEvents.emit(
                        BenchmarkEvent(
                                testName = "Test Workload",
                                mode = "TEST",
                                state = "STARTED",
                                timeMs = 0,
                                score = 0.0
                        )
                )

                // Run all benchmarks with test parameters (no recording, no delays)
                try {
                        // Single-core test benchmarks
                        SingleCoreBenchmarks.primeGeneration(testParams, isTestRun)
                        SingleCoreBenchmarks.fibonacciRecursive(testParams, isTestRun)
                        SingleCoreBenchmarks.matrixMultiplication(testParams, isTestRun)
                        SingleCoreBenchmarks.hashComputing(testParams, isTestRun)
                        SingleCoreBenchmarks.stringSorting(testParams, isTestRun)
                        SingleCoreBenchmarks.rayTracing(testParams, isTestRun)
                        SingleCoreBenchmarks.compression(testParams, isTestRun)
                        SingleCoreBenchmarks.monteCarloPi(testParams, isTestRun)
                        SingleCoreBenchmarks.jsonParsing(testParams, isTestRun)
                        SingleCoreBenchmarks.nqueens(testParams, isTestRun)

                        // Multi-core test benchmarks
                        MultiCoreBenchmarks.primeGeneration(testParams, isTestRun)
                        MultiCoreBenchmarks.fibonacciRecursive(testParams, isTestRun)
                        MultiCoreBenchmarks.matrixMultiplication(testParams, isTestRun)
                        MultiCoreBenchmarks.hashComputing(testParams, isTestRun)
                        MultiCoreBenchmarks.stringSorting(testParams, isTestRun)
                        MultiCoreBenchmarks.rayTracing(testParams, isTestRun)
                        MultiCoreBenchmarks.compression(testParams, isTestRun)
                        MultiCoreBenchmarks.monteCarloPi(testParams, isTestRun)
                        MultiCoreBenchmarks.jsonParsing(testParams, isTestRun)
                        MultiCoreBenchmarks.nqueens(testParams, isTestRun)

                        Log.d(TAG, "=== TEST WORKLOAD COMPLETE ===")
                } catch (e: Exception) {
                        Log.w(TAG, "Test workload encountered error (non-critical): ${e.message}")
                }

                // Emit test workload complete event
                _benchmarkEvents.emit(
                        BenchmarkEvent(
                                testName = "Test Workload",
                                mode = "TEST",
                                state = "COMPLETED",
                                timeMs = 0,
                                score = 0.0
                        )
                )
        }

        suspend fun runAllBenchmarks(deviceTier: String = "Flagship") {
                Log.d(
                        TAG,
                        "SINGLE_SOURCE_OF_TRUTH: Starting benchmark execution with device tier: $deviceTier"
                )

                // Run test workload first (warm-up)
                runTestWorkload()

                val params = getWorkloadParams(deviceTier)

                // Log CPU topology
                CpuAffinityManager.logTopology()

                // Run single-core benchmarks
                val singleResults = mutableListOf<BenchmarkResult>()

                // Prime Generation
                emitBenchmarkStart("Single-Core Prime Generation", "SINGLE")
                val singlePrimeResult =
                        safeBenchmarkRun("Single-Core Prime Generation") {
                                SingleCoreBenchmarks.primeGeneration(params)
                        }
                singleResults.add(singlePrimeResult)
                emitBenchmarkComplete(
                        "Single-Core Prime Generation",
                        "SINGLE",
                        singlePrimeResult.executionTimeMs.toLong(),
                        singlePrimeResult.opsPerSecond
                )

                // Fibonacci Iterative
                emitBenchmarkStart(BenchmarkName.FIBONACCI_ITERATIVE.singleCore(), "SINGLE")
                val singleFibResult =
                        safeBenchmarkRun(BenchmarkName.FIBONACCI_ITERATIVE.singleCore()) {
                                SingleCoreBenchmarks.fibonacciRecursive(params)
                        }
                singleResults.add(singleFibResult)
                emitBenchmarkComplete(
                        BenchmarkName.FIBONACCI_ITERATIVE.singleCore(),
                        "SINGLE",
                        singleFibResult.executionTimeMs.toLong(),
                        singleFibResult.opsPerSecond
                )

                // Matrix Multiplication
                emitBenchmarkStart("Single-Core Matrix Multiplication", "SINGLE")
                val singleMatrixResult =
                        safeBenchmarkRun("Single-Core Matrix Multiplication") {
                                SingleCoreBenchmarks.matrixMultiplication(params)
                        }
                singleResults.add(singleMatrixResult)
                emitBenchmarkComplete(
                        "Single-Core Matrix Multiplication",
                        "SINGLE",
                        singleMatrixResult.executionTimeMs.toLong(),
                        singleMatrixResult.opsPerSecond
                )

                // Hash Computing
                emitBenchmarkStart("Single-Core Hash Computing", "SINGLE")
                val singleHashResult =
                        safeBenchmarkRun("Single-Core Hash Computing") {
                                SingleCoreBenchmarks.hashComputing(params)
                        }
                singleResults.add(singleHashResult)
                emitBenchmarkComplete(
                        "Single-Core Hash Computing",
                        "SINGLE",
                        singleHashResult.executionTimeMs.toLong(),
                        singleHashResult.opsPerSecond
                )

                // String Sorting
                emitBenchmarkStart("Single-Core String Sorting", "SINGLE")
                val singleStringResult =
                        safeBenchmarkRun("Single-Core String Sorting") {
                                SingleCoreBenchmarks.stringSorting(params)
                        }
                singleResults.add(singleStringResult)
                emitBenchmarkComplete(
                        "Single-Core String Sorting",
                        "SINGLE",
                        singleStringResult.executionTimeMs.toLong(),
                        singleStringResult.opsPerSecond
                )

                // Ray Tracing
                emitBenchmarkStart("Single-Core Ray Tracing", "SINGLE")
                val singleRayResult =
                        safeBenchmarkRun("Single-Core Ray Tracing") {
                                SingleCoreBenchmarks.rayTracing(params)
                        }
                singleResults.add(singleRayResult)
                emitBenchmarkComplete(
                        "Single-Core Ray Tracing",
                        "SINGLE",
                        singleRayResult.executionTimeMs.toLong(),
                        singleRayResult.opsPerSecond
                )

                // Compression
                emitBenchmarkStart("Single-Core Compression", "SINGLE")
                val singleCompressionResult =
                        safeBenchmarkRun("Single-Core Compression") {
                                SingleCoreBenchmarks.compression(params)
                        }
                singleResults.add(singleCompressionResult)
                emitBenchmarkComplete(
                        "Single-Core Compression",
                        "SINGLE",
                        singleCompressionResult.executionTimeMs.toLong(),
                        singleCompressionResult.opsPerSecond
                )

                // Monte Carlo Pi
                emitBenchmarkStart("Single-Core Monte Carlo π", "SINGLE")
                val singleMonteResult =
                        safeBenchmarkRun("Single-Core Monte Carlo π") {
                                SingleCoreBenchmarks.monteCarloPi(params)
                        }
                singleResults.add(singleMonteResult)
                emitBenchmarkComplete(
                        "Single-Core Monte Carlo π",
                        "SINGLE",
                        singleMonteResult.executionTimeMs.toLong(),
                        singleMonteResult.opsPerSecond
                )

                // JSON Parsing
                emitBenchmarkStart("Single-Core JSON Parsing", "SINGLE")
                val singleJsonResult =
                        safeBenchmarkRun("Single-Core JSON Parsing") {
                                SingleCoreBenchmarks.jsonParsing(params)
                        }
                singleResults.add(singleJsonResult)
                emitBenchmarkComplete(
                        "Single-Core JSON Parsing",
                        "SINGLE",
                        singleJsonResult.executionTimeMs.toLong(),
                        singleJsonResult.opsPerSecond
                )

                // N-Queens
                emitBenchmarkStart("Single-Core N-Queens", "SINGLE")
                val singleNqueensResult =
                        safeBenchmarkRun("Single-Core N-Queens") {
                                SingleCoreBenchmarks.nqueens(params)
                        }
                singleResults.add(singleNqueensResult)
                emitBenchmarkComplete(
                        "Single-Core N-Queens",
                        "SINGLE",
                        singleNqueensResult.executionTimeMs.toLong(),
                        singleNqueensResult.opsPerSecond
                )

                // Run multi-core benchmarks
                val multiResults = mutableListOf<BenchmarkResult>()

                // Prime Generation
                emitBenchmarkStart("Multi-Core Prime Generation", "MULTI")
                val multiPrimeResult =
                        safeBenchmarkRun("Multi-Core Prime Generation") {
                                MultiCoreBenchmarks.primeGeneration(params)
                        }
                multiResults.add(multiPrimeResult)
                emitBenchmarkComplete(
                        "Multi-Core Prime Generation",
                        "MULTI",
                        multiPrimeResult.executionTimeMs.toLong(),
                        multiPrimeResult.opsPerSecond
                )

                // Fibonacci Iterative
                emitBenchmarkStart(BenchmarkName.FIBONACCI_ITERATIVE.multiCore(), "MULTI")
                val multiFibResult =
                        safeBenchmarkRun(BenchmarkName.FIBONACCI_ITERATIVE.multiCore()) {
                                MultiCoreBenchmarks.fibonacciRecursive(params)
                        }
                multiResults.add(multiFibResult)
                emitBenchmarkComplete(
                        BenchmarkName.FIBONACCI_ITERATIVE.multiCore(),
                        "MULTI",
                        multiFibResult.executionTimeMs.toLong(),
                        multiFibResult.opsPerSecond
                )

                // Matrix Multiplication
                emitBenchmarkStart("Multi-Core Matrix Multiplication", "MULTI")
                val multiMatrixResult =
                        safeBenchmarkRun("Multi-Core Matrix Multiplication") {
                                MultiCoreBenchmarks.matrixMultiplication(params)
                        }
                multiResults.add(multiMatrixResult)
                emitBenchmarkComplete(
                        "Multi-Core Matrix Multiplication",
                        "MULTI",
                        multiMatrixResult.executionTimeMs.toLong(),
                        multiMatrixResult.opsPerSecond
                )

                // Hash Computing
                emitBenchmarkStart("Multi-Core Hash Computing", "MULTI")
                val multiHashResult =
                        safeBenchmarkRun("Multi-Core Hash Computing") {
                                MultiCoreBenchmarks.hashComputing(params)
                        }
                multiResults.add(multiHashResult)
                emitBenchmarkComplete(
                        "Multi-Core Hash Computing",
                        "MULTI",
                        multiHashResult.executionTimeMs.toLong(),
                        multiHashResult.opsPerSecond
                )

                // String Sorting
                emitBenchmarkStart("Multi-Core String Sorting", "MULTI")
                val multiStringResult =
                        safeBenchmarkRun("Multi-Core String Sorting") {
                                MultiCoreBenchmarks.stringSorting(params)
                        }
                multiResults.add(multiStringResult)
                emitBenchmarkComplete(
                        "Multi-Core String Sorting",
                        "MULTI",
                        multiStringResult.executionTimeMs.toLong(),
                        multiStringResult.opsPerSecond
                )

                // Ray Tracing
                emitBenchmarkStart("Multi-Core Ray Tracing", "MULTI")
                val multiRayResult =
                        safeBenchmarkRun("Multi-Core Ray Tracing") {
                                MultiCoreBenchmarks.rayTracing(params)
                        }
                multiResults.add(multiRayResult)
                emitBenchmarkComplete(
                        "Multi-Core Ray Tracing",
                        "MULTI",
                        multiRayResult.executionTimeMs.toLong(),
                        multiRayResult.opsPerSecond
                )

                // Compression
                emitBenchmarkStart("Multi-Core Compression", "MULTI")
                val multiCompressionResult =
                        safeBenchmarkRun("Multi-Core Compression") {
                                MultiCoreBenchmarks.compression(params)
                        }
                multiResults.add(multiCompressionResult)
                emitBenchmarkComplete(
                        "Multi-Core Compression",
                        "MULTI",
                        multiCompressionResult.executionTimeMs.toLong(),
                        multiCompressionResult.opsPerSecond
                )

                // Monte Carlo Pi
                emitBenchmarkStart("Multi-Core Monte Carlo π", "MULTI")
                val multiMonteResult =
                        safeBenchmarkRun("Multi-Core Monte Carlo π") {
                                MultiCoreBenchmarks.monteCarloPi(params)
                        }
                multiResults.add(multiMonteResult)
                emitBenchmarkComplete(
                        "Multi-Core Monte Carlo π",
                        "MULTI",
                        multiMonteResult.executionTimeMs.toLong(),
                        multiMonteResult.opsPerSecond
                )

                // JSON Parsing
                emitBenchmarkStart("Multi-Core JSON Parsing", "MULTI")
                val multiJsonResult =
                        safeBenchmarkRun("Multi-Core JSON Parsing") {
                                MultiCoreBenchmarks.jsonParsing(params)
                        }
                multiResults.add(multiJsonResult)
                emitBenchmarkComplete(
                        "Multi-Core JSON Parsing",
                        "MULTI",
                        multiJsonResult.executionTimeMs.toLong(),
                        multiJsonResult.opsPerSecond
                )

                // N-Queens
                emitBenchmarkStart("Multi-Core N-Queens", "MULTI")
                val multiNqueensResult =
                        safeBenchmarkRun("Multi-Core N-Queens") {
                                MultiCoreBenchmarks.nqueens(params)
                        }
                multiResults.add(multiNqueensResult)
                emitBenchmarkComplete(
                        "Multi-Core N-Queens",
                        "MULTI",
                        multiNqueensResult.executionTimeMs.toLong(),
                        multiNqueensResult.opsPerSecond
                )

                // Calculate and emit final results
                val summaryJson = calculateSummary(singleResults, multiResults)
                Log.d(TAG, "SINGLE_SOURCE_OF_TRUTH: Generated summary JSON: $summaryJson")
                Log.d(
                        TAG,
                        "SINGLE_SOURCE_OF_TRUTH: Emitting completion signal with calculated scores"
                )
                _benchmarkComplete.emit(summaryJson)
                Log.d(TAG, "SINGLE_SOURCE_OF_TRUTH: Completion signal emitted successfully")
        }

        private suspend fun safeBenchmarkRun(
                testName: String,
                block: suspend () -> BenchmarkResult
        ): BenchmarkResult {
                return try {
                        withContext(Dispatchers.Default) {
                                val result = block()
                                Log.d(
                                        TAG,
                                        "✓ $testName completed successfully: ${result.opsPerSecond} ops/sec"
                                )
                                result
                        }
                } catch (e: Exception) {
                        Log.e(TAG, "✗ $testName failed with exception: ${e.message}", e)
                        // Return a dummy result so the benchmark suite can continue
                        BenchmarkResult(
                                name = testName,
                                executionTimeMs = 0.0,
                                opsPerSecond = 0.0,
                                isValid = false,
                                metricsJson = "{\"error\": \"${e.message}\"}"
                        )
                }
        }

        /**
         * Calculate geometric mean score for benchmark results.
         * 
         * Formula: GeometricMean = (∏ ratios)^(1/n)
         * where ratio = SUT_Mops / Reference_Mops
         * 
         * This prevents one fast benchmark from dominating the score and provides
         * fair comparison across different device architectures.
         * 
         * @param results List of benchmark results
         * @return Geometric mean score scaled to 100-point baseline (SD 8 Gen 3 = 100)
         */
        private fun calculateGeometricMean(results: List<BenchmarkResult>): Double {
                if (results.isEmpty()) return 0.0
                
                // Calculate performance ratios for each benchmark
                val ratios = results.mapNotNull { result ->
                        val benchmarkName = BenchmarkName.fromString(result.name)
                        val refMops = benchmarkName?.let { REFERENCE_MOPS[it] }
                        
                        if (refMops != null && refMops > 0.0) {
                                // Ratio = SUT performance / Reference performance
                                result.opsPerSecond / refMops
                        } else {
                                Log.w(TAG, "No reference value for ${result.name}, skipping in geometric mean")
                                null
                        }
                }
                
                if (ratios.isEmpty()) {
                        Log.e(TAG, "No valid ratios calculated for geometric mean")
                        return 0.0
                }
                
                // Calculate geometric mean: (product)^(1/n)
                var product = 1.0
                for (ratio in ratios) {
                        product *= ratio
                }
                
                val geometricMean = Math.pow(product, 1.0 / ratios.size)
                
                // Scale to 100-point baseline (SD 8 Gen 3 = 100)
                val score = geometricMean * 100.0
                
                Log.d(TAG, "Geometric mean calculation: ${ratios.size} benchmarks, GM=${geometricMean}, Score=${score}")
                
                return score
        }

        private fun calculateSummary(
                singleResults: List<BenchmarkResult>,
                multiResults: List<BenchmarkResult>
        ): String {
                // Calculate single-core score using geometric mean
                val calculatedSingleCoreScore = calculateGeometricMean(singleResults)

                // Calculate multi-core score using geometric mean
                val calculatedMultiCoreScore = calculateGeometricMean(multiResults)

                // Calculate final weighted score (35% single, 65% multi)
                val calculatedFinalScore =
                        (calculatedSingleCoreScore * 0.35) + (calculatedMultiCoreScore * 0.65)

                // Normalize the score to a reasonable range
                val calculatedNormalizedScore = calculatedFinalScore

                // Determine rating based on normalized score
                val rating =
                        when {
                                calculatedNormalizedScore >= 1600.0 ->
                                        "★★★★★ (Exceptional Performance)"
                                calculatedNormalizedScore >= 1200.0 -> "★★★★☆ (High Performance)"
                                calculatedNormalizedScore >= 800.0 -> "★★★☆☆ (Good Performance)"
                                calculatedNormalizedScore >= 500.0 -> "★★☆☆☆ (Moderate Performance)"
                                calculatedNormalizedScore >= 250.0 -> "★☆☆☆☆ (Basic Performance)"
                                else -> "☆☆☆☆☆ (Low Performance)"
                        }

                Log.d(
                        TAG,
                        "Final scoring - Single: $calculatedSingleCoreScore, Multi: $calculatedMultiCoreScore, Final: $calculatedFinalScore, Normalized: $calculatedNormalizedScore"
                )

                // CRITICAL FIX: Validation to ensure multi-core scores are higher than single-core
                // scores
                if (calculatedMultiCoreScore <= calculatedSingleCoreScore) {
                        Log.w(
                                TAG,
                                "WARNING: Multi-core score ($calculatedMultiCoreScore) is not higher than single-core score ($calculatedSingleCoreScore)"
                        )
                        Log.w(
                                TAG,
                                "This indicates a critical issue with benchmark implementations or scaling factors"
                        )
                } else {
                        Log.i(
                                TAG,
                                "✓ VALIDATED: Multi-core score ($calculatedMultiCoreScore) is higher than single-core score ($calculatedSingleCoreScore)"
                        )
                        Log.i(
                                TAG,
                                "Multi-core advantage: ${String.format("%.2fx", calculatedMultiCoreScore / calculatedSingleCoreScore)}"
                        )
                }

                // CRITICAL FIX: Include detailed_results array that ResultScreen expects
                val detailedResultsArray =
                        JSONArray().apply {
                                // Add single core results
                                singleResults.forEach { result ->
                                        put(
                                                JSONObject().apply {
                                                        put("name", result.name)
                                                        put("opsPerSecond", result.opsPerSecond)
                                                        put(
                                                                "executionTimeMs",
                                                                result.executionTimeMs
                                                        )
                                                        put("isValid", result.isValid)
                                                        put("metricsJson", result.metricsJson)
                                                }
                                        )
                                }
                                // Add multi core results
                                multiResults.forEach { result ->
                                        put(
                                                JSONObject().apply {
                                                        put("name", result.name)
                                                        put("opsPerSecond", result.opsPerSecond)
                                                        put(
                                                                "executionTimeMs",
                                                                result.executionTimeMs
                                                        )
                                                        put("isValid", result.isValid)
                                                        put("metricsJson", result.metricsJson)
                                                }
                                        )
                                }
                        }

                return JSONObject()
                        .apply {
                                put("single_core_score", calculatedSingleCoreScore)
                                put("multi_core_score", calculatedMultiCoreScore)
                                put("final_score", calculatedFinalScore)
                                put("normalized_score", calculatedNormalizedScore)
                                put("rating", rating)
                                put("detailed_results", detailedResultsArray)
                        }
                        .toString()
        }

        private suspend fun emitBenchmarkStart(testName: String, mode: String) {
                _benchmarkEvents.emit(
                        BenchmarkEvent(
                                testName = testName,
                                mode = mode,
                                state = "STARTED",
                                timeMs = 0,
                                score = 0.0
                        )
                )
        }

        private suspend fun emitBenchmarkComplete(
                testName: String,
                mode: String,
                timeMs: Long,
                score: Double
        ) {
                _benchmarkEvents.emit(
                        BenchmarkEvent(
                                testName = testName,
                                mode = mode,
                                state = "COMPLETED",
                                timeMs = timeMs,
                                score = score
                        )
                )
        }

        private fun getWorkloadParams(deviceTier: String): WorkloadParams {
                return when (deviceTier.lowercase()) {
                        "test" ->
                                WorkloadParams(
                                        primeRange = 1_000_000,
                                        fibonacciNRange = Pair(92, 92),
                                        fibonacciIterations = 5_000_000,
                                        matrixSize = 128,
                                        matrixIterations = 100,
                                        hashDataSizeMb = 8,
                                        hashIterations = 10_000,
                                        stringSortIterations = 100,
                                        rayTracingIterations = 10,
                                        rayTracingResolution = Pair(100, 100),
                                        rayTracingDepth = 2,
                                        compressionDataSizeMb = 1,
                                        compressionIterations = 100,
                                        monteCarloSamples = 1_000_000L,
                                        jsonDataSizeMb = 1,
                                        jsonParsingIterations = 100,
                                        nqueensSize = 11
                                )
                        "slow" ->
                                WorkloadParams(
                                        primeRange = 10_000_000,

                                        // ~1.0-2.0s
                                        fibonacciNRange = Pair(92, 92),
                                        fibonacciIterations = 75_000_000,
                                        matrixSize = 128, // CACHE-RESIDENT: Fixed small size
                                        matrixIterations =
                                                800, // CACHE-RESIDENT: Low iterations for slow
                                        // devices
                                        hashDataSizeMb = 8,
                                        hashIterations =
                                                800_000, // FIXED WORK PER CORE: Target ~1.5-2.0
                                        // seconds
                                        stringSortIterations =
                                                800, // CACHE-RESIDENT: Explicit control - target
                                        rayTracingIterations = 50,
                                        rayTracingResolution = Pair(100, 100),
                                        rayTracingDepth = 2,
                                        compressionDataSizeMb = 1,
                                        compressionIterations =
                                                600, // INCREASED: Target ~15s execution (was 20)
                                        monteCarloSamples =
                                                10_000_000L, // FIXED WORK PER CORE: Target ~1.5s
                                        jsonDataSizeMb = 1,
                                        jsonParsingIterations =
                                                400, // CACHE-RESIDENT: Low iterations for slow
                                        // devices (~1-2s)
                                        nqueensSize = 12 // INCREASED: 92 solutions, ~1.5s (was 8)
                                )
                        "mid" ->
                                WorkloadParams(
                                        primeRange = 25_000_000,
                                        // ~1.0-2.0s
                                        fibonacciNRange = Pair(96, 96),
                                        fibonacciIterations = 75_000_000,
                                        matrixSize = 128, // CACHE-RESIDENT: Fixed small size
                                        matrixIterations =
                                                1500, // CACHE-RESIDENT: Medium iterations for mid
                                        // devices
                                        hashDataSizeMb = 2,
                                        hashIterations =
                                                1_500_000, // FIXED WORK PER CORE: Target ~1.5-2.0
                                        // seconds
                                        stringSortIterations =
                                                2_500, // CACHE-RESIDENT: Explicit control - target
                                        rayTracingIterations =
                                                200, // FIXED: Medium iterations for mid devices
                                        rayTracingResolution = Pair(100, 100),
                                        rayTracingDepth = 3,
                                        compressionDataSizeMb = 1,
                                        compressionIterations =
                                                1_000, // INCREASED: Target ~15s execution (was 50)
                                        monteCarloSamples =
                                                200_000_000L, // FIXED WORK PER CORE: Target ~1.5s
                                        jsonDataSizeMb = 1,
                                        jsonParsingIterations =
                                                600, // CACHE-RESIDENT: Medium iterations for mid
                                        // devices (~1-2s)
                                        nqueensSize = 13 // INCREASED: 341 solutions, ~5s (was 9)
                                )
                        "flagship" ->
                                WorkloadParams(
                                        // CACHE-RESIDENT STRATEGY: Small matrices with high
                                        // iterations

                                        primeRange = 900_000_000,
                                        fibonacciNRange = Pair(92, 92), // Use fixed max safe value
                                        fibonacciIterations = 125_000_000,
                                        matrixSize =
                                                128, // CACHE-RESIDENT: Fixed small size for cache
                                        // efficiency
                                        matrixIterations =
                                                3000, // CACHE-RESIDENT: High iterations for
                                        // flagship devices
                                        hashDataSizeMb = 8,
                                        hashIterations =
                                                2_500_000, // FIXED WORK PER CORE: Target ~1.5-2.0
                                        // seconds

                                        stringSortIterations =
                                                5_000, // CACHE-RESIDENT: Explicit control - target
                                        // ~1.0-2.0s
                                        rayTracingIterations =
                                                800, // FIXED: Increased to 2000 to reach ~5s
                                        // target
                                        // duration for better thermal testing
                                        rayTracingResolution =
                                                Pair(
                                                        256, // REVERTED: 256×256 caused thermal
                                                        // throttling
                                                        256
                                                ), // OPTIMIZED: Increased from 100×100 to 256×256
                                        // for better multi-core scaling
                                        rayTracingDepth = 5,
                                        compressionDataSizeMb = 2,
                                        compressionIterations =
                                                2_000, // INCREASED: Target ~15s execution (was
                                        // 100)
                                        monteCarloSamples =
                                                4_999_000_000L, // FIXED WORK PER CORE: Target ~1.5s
                                        // (was
                                        // 15M)
                                        jsonDataSizeMb = 1,
                                        jsonParsingIterations =
                                                800, // CACHE-RESIDENT: High iterations for flagship
                                        // devices (~1-2s)
                                        nqueensSize =
                                                15 // INCREASED: 14,200 solutions, ~20s (was 10)
                                )
                        else -> WorkloadParams() // Default values
                }
        }
}
