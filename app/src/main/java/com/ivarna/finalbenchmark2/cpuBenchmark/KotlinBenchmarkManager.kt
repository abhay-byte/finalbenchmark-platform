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

                // CONSOLIDATED: Single source of truth for scaling factors
                // Target: Single-core ~200, Multi-core ~800 (each benchmark contributes ~20/~80)
                // Raw opsPerSecond is in ops/s, UI displays as Mops/s (divide by 1e6)
                private val SINGLE_CORE_FACTORS =
                        mapOf(
                                "Prime Generation" to 9.22e-6, // 20 / 2.17e6 ops/s
                                "Fibonacci Recursive" to 1.16e-6, // 20 / 17.23e6 ops/s
                                "Matrix Multiplication" to 3.93e-8, // 20 / 508.85e6 ops/s
                                "Hash Computing" to 7.14e-5, // 20 / 0.28e6 ops/s
                                "String Sorting" to 5.04e-7, // 20 / 39.65e6 ops/s
                                "Ray Tracing" to 7.78e-6, // 20 / 2.57e6 ops/s
                                "Compression" to 4.27e-8, // 20 / 468.69e6 ops/s
                                "Monte Carlo" to 1.58e-6, // 20 / 12.68e6 ops/s
                                "JSON Parsing" to 4.47e-6, // 20 / 4.47e6 ops/s
                                "N-Queens" to 4.31e-7 // 20 / 46.36e6 ops/s
                        )

                // Multi-core factors: Target ~80 per benchmark for total ~800
                private val MULTI_CORE_FACTORS =
                        mapOf(
                                "Prime Generation" to 9.76e-6, // 80 / 8.20e6 ops/s
                                "Fibonacci Recursive" to 9.02e-7, // 80 / 88.64e6 ops/s
                                "Matrix Multiplication" to 2.09e-8, // 80 / 3826.63e6 ops/s
                                "Hash Computing" to 3.92e-5, // 80 / 2.04e6 ops/s
                                "String Sorting" to 5.74e-7, // 80 / 139.33e6 ops/s
                                "Ray Tracing" to 9.09e-5, // 80 / 0.88e6 ops/s
                                "Compression" to 4.56e-8, // 80 / 1755.67e6 ops/s
                                "Monte Carlo" to 1.88e-6, // 80 / 42.49e6 ops/s
                                "JSON Parsing" to 4.81e-6, // 80 / 16.62e6 ops/s
                                "N-Queens" to 4.96e-7 // 80 / 161.21e6 ops/s
                        )
        }

        suspend fun runAllBenchmarks(deviceTier: String = "Flagship") {
                Log.d(
                        TAG,
                        "SINGLE_SOURCE_OF_TRUTH: Starting benchmark execution with device tier: $deviceTier"
                )
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

                // Fibonacci Recursive
                emitBenchmarkStart("Single-Core Fibonacci Recursive", "SINGLE")
                val singleFibResult =
                        safeBenchmarkRun("Single-Core Fibonacci Recursive") {
                                SingleCoreBenchmarks.fibonacciRecursive(params)
                        }
                singleResults.add(singleFibResult)
                emitBenchmarkComplete(
                        "Single-Core Fibonacci Recursive",
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

                // Fibonacci Recursive
                emitBenchmarkStart("Multi-Core Fibonacci Recursive", "MULTI")
                val multiFibResult =
                        safeBenchmarkRun("Multi-Core Fibonacci Recursive") {
                                MultiCoreBenchmarks.fibonacciRecursive(params)
                        }
                multiResults.add(multiFibResult)
                emitBenchmarkComplete(
                        "Multi-Core Fibonacci Recursive",
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

        private fun calculateSummary(
                singleResults: List<BenchmarkResult>,
                multiResults: List<BenchmarkResult>
        ): String {
                // Calculate single-core score using weighted scoring
                var calculatedSingleCoreScore = 0.0
                for (result in singleResults) {
                        val cleanName = result.name.replace("Single-Core ", "").trim()
                        val factor =
                                SINGLE_CORE_FACTORS[cleanName] ?: SINGLE_CORE_FACTORS.values.first()
                        calculatedSingleCoreScore += result.opsPerSecond * factor
                }

                // Calculate multi-core score using weighted scoring
                var calculatedMultiCoreScore = 0.0
                for (result in multiResults) {
                        val cleanName = result.name.replace("Multi-Core ", "").trim()
                        val factor =
                                MULTI_CORE_FACTORS[cleanName] ?: MULTI_CORE_FACTORS.values.first()
                        calculatedMultiCoreScore += result.opsPerSecond * factor
                }

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
                        "slow" ->
                                WorkloadParams(
                                        primeRange = 100_000,
                                        stringSortCount =
                                                2_000_000, // LEGACY: Kept for compatibility
                                        stringSortIterations =
                                                500, // CACHE-RESIDENT: Explicit control - target
                                        // ~1.0-2.0s
                                        fibonacciNRange = Pair(25, 27),
                                        fibonacciIterations = 2_000_000,
                                        matrixSize = 128, // CACHE-RESIDENT: Fixed small size
                                        matrixIterations =
                                                50, // CACHE-RESIDENT: Low iterations for slow
                                        // devices
                                        hashDataSizeMb = 1,
                                        hashIterations =
                                                200_000, // FIXED WORK PER CORE: Target ~1.5-2.0
                                        // seconds
                                        rayTracingIterations =
                                                10, // FIXED: Low iterations for slow devices
                                        rayTracingResolution = Pair(128, 128),
                                        rayTracingDepth = 2,
                                        compressionDataSizeMb = 1,
                                        compressionIterations =
                                                600, // INCREASED: Target ~15s execution (was 20)
                                        monteCarloSamples =
                                                200_000, // FIXED WORK PER CORE: Target ~1.5s
                                        jsonDataSizeMb = 1,
                                        jsonParsingIterations =
                                                50, // CACHE-RESIDENT: Low iterations for slow
                                        // devices (~1-2s)
                                        nqueensSize = 10 // INCREASED: 92 solutions, ~1.5s (was 8)
                                )
                        "mid" ->
                                WorkloadParams(
                                        primeRange = 200_000,
                                        stringSortCount =
                                                10_000_000, // LEGACY: Kept for compatibility
                                        stringSortIterations =
                                                2_500, // CACHE-RESIDENT: Explicit control - target
                                        // ~1.0-2.0s
                                        fibonacciNRange = Pair(28, 30),
                                        fibonacciIterations = 10_000_000,
                                        matrixSize = 128, // CACHE-RESIDENT: Fixed small size
                                        matrixIterations =
                                                200, // CACHE-RESIDENT: Medium iterations for mid
                                        // devices
                                        hashDataSizeMb = 2,
                                        hashIterations =
                                                500_000, // FIXED WORK PER CORE: Target ~1.5-2.0
                                        // seconds
                                        rayTracingIterations =
                                                40, // FIXED: Medium iterations for mid devices
                                        rayTracingResolution = Pair(160, 160),
                                        rayTracingDepth = 3,
                                        compressionDataSizeMb = 1,
                                        compressionIterations =
                                                1_000, // INCREASED: Target ~15s execution (was 50)
                                        monteCarloSamples =
                                                500_000, // FIXED WORK PER CORE: Target ~1.5s
                                        jsonDataSizeMb = 1,
                                        jsonParsingIterations =
                                                100, // CACHE-RESIDENT: Medium iterations for mid
                                        // devices (~1-2s)
                                        nqueensSize = 11 // INCREASED: 341 solutions, ~5s (was 9)
                                )
                        "flagship" ->
                                WorkloadParams(
                                        // CACHE-RESIDENT STRATEGY: Small matrices with high
                                        // iterations

                                        primeRange = 50_000_000,
                                        fibonacciNRange = Pair(92, 92), // Use fixed max safe value
                                        fibonacciIterations = 125_000_000,
                                        matrixSize =
                                                128, // CACHE-RESIDENT: Fixed small size for cache
                                        // efficiency
                                        matrixIterations =
                                                1500, // CACHE-RESIDENT: High iterations for
                                        // flagship devices
                                        hashDataSizeMb = 8,
                                        hashIterations =
                                                2_500_000, // FIXED WORK PER CORE: Target ~1.5-2.0
                                        // seconds

                                        stringSortIterations =
                                                5_000, // CACHE-RESIDENT: Explicit control - target
                                        // ~1.0-2.0s
                                        rayTracingIterations =
                                                100, // FIXED: Increased to 2000 to reach ~5s
                                        // target
                                        // duration for better thermal testing
                                        rayTracingResolution =
                                                Pair(
                                                        100, // REVERTED: 256×256 caused thermal
                                                        // throttling
                                                        100
                                                ), // OPTIMIZED: Increased from 100×100 to 256×256
                                        // for better multi-core scaling
                                        rayTracingDepth = 5,
                                        compressionDataSizeMb = 2,
                                        compressionIterations =
                                                2_000, // INCREASED: Target ~15s execution (was
                                        // 100)
                                        monteCarloSamples =
                                                100_000_000, // FIXED WORK PER CORE: Target ~1.5s
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
