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
                // Target: ~10,000 total points for flagship devices
                // HEAVY WORKLOAD: Updated for 1.5-2.0s execution times with increased workload
                // parameters
                private val SINGLE_CORE_FACTORS =
                        mapOf(
                                "Prime Generation" to
                                        2.0e-3, // HEAVY: Increased from 4.0e-4 to 2.0e-3 (5x) for
                                // Trial
                                // Division with 2M range
                                "Fibonacci Recursive" to 1.2e-5, // Fib: 1.2e-5
                                "Matrix Multiplication" to 4.0e-6, // Matrix: 4.0e-6
                                "Hash Computing" to 6.0e-3, // Hash: 6.0e-3
                                "String Sorting" to 5.0e-3, // String: 5.0e-3
                                "Ray Tracing" to 1.5e-3, // Ray: 1.5e-3
                                "Compression" to 5.0e-4, // Compression: 5.0e-4
                                "Monte Carlo" to 2.0e-4, // Monte Carlo: 2.0e-4
                                "JSON Parsing" to 1.8e-3, // JSON: 1.8e-3
                                "N-Queens" to 8.0e-2 // N-Queens: 8.0e-2
                        )

                // Multi-core factors updated for strided loop implementation with HEAVY workload
                private val MULTI_CORE_FACTORS =
                        mapOf(
                                "Prime Generation" to
                                        4.0e-4, // HEAVY: Increased from 8.0e-5 to 4.0e-4 (5x) for
                                // strided loop
                                "Fibonacci Recursive" to 1.0e-5, // Fib: 1.0e-5
                                "Matrix Multiplication" to 3.5e-6, // Matrix: 3.5e-6
                                "Hash Computing" to 3.0e-3, // Hash: 3.0e-3
                                "String Sorting" to 3.0e-3, // String: 3.0e-3
                                "Ray Tracing" to 1.0e-3, // Ray: 1.0e-3
                                "Compression" to 6.0e-4, // Compression: 6.0e-4
                                "Monte Carlo" to 3.5e-4, // Monte Carlo: 3.5e-4
                                "JSON Parsing" to 3.5e-3, // JSON: 3.5e-3
                                "N-Queens" to 3.0e-3 // N-Queens: 3.0e-3
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
                                        monteCarloSamples = 200_000,
                                        jsonDataSizeMb = 1,
                                        nqueensSize = 8
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
                                        compressionDataSizeMb = 2,
                                        monteCarloSamples = 500_000,
                                        jsonDataSizeMb = 1,
                                        nqueensSize = 9
                                )
                        "flagship" ->
                                WorkloadParams(
                                        // CACHE-RESIDENT STRATEGY: Small matrices with high
                                        // iterations

                                        primeRange = 1_000_000,
                                        fibonacciNRange = Pair(92, 92), // Use fixed max safe value
                                        fibonacciIterations = 1_000_000,
                                        matrixSize =
                                                128, // CACHE-RESIDENT: Fixed small size for cache
                                        // efficiency
                                        matrixIterations =
                                                500, // CACHE-RESIDENT: High iterations for
                                        // flagship devices
                                        hashDataSizeMb = 8,
                                        hashIterations =
                                                100_000, // FIXED WORK PER CORE: Target ~1.5-2.0
                                        // seconds

                                        stringSortIterations =
                                                1_000, // CACHE-RESIDENT: Explicit control - target
                                        // ~1.0-2.0s
                                        rayTracingIterations =
                                                400, // FIXED: Increased from 200 to 400 to reach 5s
                                        // duration
                                        // (target ~5s with new primitives kernel)
                                        rayTracingResolution = Pair(100, 100),
                                        rayTracingDepth = 5,
                                        compressionDataSizeMb = 2,
                                        monteCarloSamples = 15_000_000,
                                        jsonDataSizeMb = 1,
                                        nqueensSize = 10
                                )
                        else -> WorkloadParams() // Default values
                }
        }
}
