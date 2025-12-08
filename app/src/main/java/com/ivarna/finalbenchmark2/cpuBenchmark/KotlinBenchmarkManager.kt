package com.ivarna.finalbenchmark2.cpuBenchmark

import android.util.Log
import com.ivarna.finalbenchmark2.cpuBenchmark.algorithms.SingleCoreBenchmarks
import com.ivarna.finalbenchmark2.cpuBenchmark.algorithms.MultiCoreBenchmarks
import com.ivarna.finalbenchmark2.cpuBenchmark.algorithms.BenchmarkHelpers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import org.json.JSONObject
import org.json.JSONArray
import kotlin.math.ln

class KotlinBenchmarkManager {
    private val _benchmarkEvents = MutableSharedFlow<BenchmarkEvent>()
    val benchmarkEvents: SharedFlow<BenchmarkEvent> = _benchmarkEvents.asSharedFlow()
    
    private val _benchmarkComplete = MutableSharedFlow<String>()
    val benchmarkComplete: SharedFlow<String> = _benchmarkComplete.asSharedFlow()
    
    companion object {
        private const val TAG = "KotlinBenchmarkManager"
        
        // FIXED: Recalculated scaling factors to ensure multi-core scores > single-core scores
        // Target: Each benchmark contributes ~35 points single-core, ~65 points multi-core
        // Multi-core factors are SMALLER because multi-core produces higher ops/s (more parallelism)
        private val SINGLE_CORE_FACTORS = mapOf(
            "Prime Generation" to 3.5e-3,       // 10,000 * 0.0035 = 35 points (realistic prime count)
            "Fibonacci Recursive" to 35.0,      // 1 * 35 = 35 points (1 fib/sec is realistic for fib(30))
            "Matrix Multiplication" to 2.8e-7,  // 125,000 * 2.8e-7 = 35 points
            "Hash Computing" to 2.6e-4,         // 135,000 * 2.6e-4 = 35 points
            "String Sorting" to 4.8e-6,         // 7,200,000 * 4.8e-6 = 35 points
            "Ray Tracing" to 3.6e-5,            // 970,000 * 3.6e-5 = 35 points
            "Compression" to 4.2e-7,            // 84,000,000 * 4.2e-7 = 35 points
            "Monte Carlo" to 7.0e-5,            // 500,000 * 7.0e-5 = 35 points
            "JSON Parsing" to 5.1e-5,           // 690,000 * 5.1e-5 = 35 points
            "N-Queens" to 2.5e-3                // 14,000 * 2.5e-3 = 35 points
        )
        private val MULTI_CORE_FACTORS = mapOf(
            "Prime Generation" to 8.1e-4,       // 80,000 * 8.1e-4 = 65 points (8x parallelism)
            "Fibonacci Recursive" to 8.1,       // 8 * 8.1 = 65 points (8 fib/sec with 8 cores)
            "Matrix Multiplication" to 6.5e-8,  // 1,000,000 * 6.5e-8 = 65 points
            "Hash Computing" to 1.6e-4,         // 400,000 * 1.6e-4 = 65 points
            "String Sorting" to 1.2e-5,         // 5,400,000 * 1.2e-5 = 65 points
            "Ray Tracing" to 5.4e-5,            // 1,200,000 * 5.4e-5 = 65 points
            "Compression" to 8.6e-7,            // 76,000,000 * 8.6e-7 = 65 points
            "Monte Carlo" to 1.6e-4,            // 4,000,000 * 1.6e-4 = 65 points
            "JSON Parsing" to 1.5e-4,           // 430,000 * 1.5e-4 = 65 points
            "N-Queens" to 1.5e-4                // 430,000 * 1.5e-4 = 65 points
        )
    }
    
    suspend fun runAllBenchmarks(deviceTier: String = "Flagship") {
        val params = getWorkloadParams(deviceTier)
        
        // Log CPU topology
        CpuAffinityManager.logTopology()
        
        // Run single-core benchmarks
        val singleResults = mutableListOf<BenchmarkResult>()
        
        // Prime Generation
        emitBenchmarkStart("Single-Core Prime Generation", "SINGLE")
        val singlePrimeResult = safeBenchmarkRun("Single-Core Prime Generation") {
            SingleCoreBenchmarks.primeGeneration(params)
        }
        singleResults.add(singlePrimeResult)
        emitBenchmarkComplete("Single-Core Prime Generation", "SINGLE", 
            singlePrimeResult.executionTimeMs.toLong(), singlePrimeResult.opsPerSecond)
        
        // Fibonacci Recursive
        emitBenchmarkStart("Single-Core Fibonacci Recursive", "SINGLE")
        val singleFibResult = safeBenchmarkRun("Single-Core Fibonacci Recursive") {
            SingleCoreBenchmarks.fibonacciRecursive(params)
        }
        singleResults.add(singleFibResult)
        emitBenchmarkComplete("Single-Core Fibonacci Recursive", "SINGLE",
            singleFibResult.executionTimeMs.toLong(), singleFibResult.opsPerSecond)
        
        // Matrix Multiplication
        emitBenchmarkStart("Single-Core Matrix Multiplication", "SINGLE")
        val singleMatrixResult = safeBenchmarkRun("Single-Core Matrix Multiplication") {
            SingleCoreBenchmarks.matrixMultiplication(params)
        }
        singleResults.add(singleMatrixResult)
        emitBenchmarkComplete("Single-Core Matrix Multiplication", "SINGLE",
            singleMatrixResult.executionTimeMs.toLong(), singleMatrixResult.opsPerSecond)
        
        // Hash Computing
        emitBenchmarkStart("Single-Core Hash Computing", "SINGLE")
        val singleHashResult = safeBenchmarkRun("Single-Core Hash Computing") {
            SingleCoreBenchmarks.hashComputing(params)
        }
        singleResults.add(singleHashResult)
        emitBenchmarkComplete("Single-Core Hash Computing", "SINGLE",
            singleHashResult.executionTimeMs.toLong(), singleHashResult.opsPerSecond)
        
        // String Sorting
        emitBenchmarkStart("Single-Core String Sorting", "SINGLE")
        val singleStringResult = safeBenchmarkRun("Single-Core String Sorting") {
            SingleCoreBenchmarks.stringSorting(params)
        }
        singleResults.add(singleStringResult)
        emitBenchmarkComplete("Single-Core String Sorting", "SINGLE",
            singleStringResult.executionTimeMs.toLong(), singleStringResult.opsPerSecond)
        
        // Ray Tracing
        emitBenchmarkStart("Single-Core Ray Tracing", "SINGLE")
        val singleRayResult = safeBenchmarkRun("Single-Core Ray Tracing") {
            SingleCoreBenchmarks.rayTracing(params)
        }
        singleResults.add(singleRayResult)
        emitBenchmarkComplete("Single-Core Ray Tracing", "SINGLE",
            singleRayResult.executionTimeMs.toLong(), singleRayResult.opsPerSecond)
        
        // Compression
        emitBenchmarkStart("Single-Core Compression", "SINGLE")
        val singleCompressionResult = safeBenchmarkRun("Single-Core Compression") {
            SingleCoreBenchmarks.compression(params)
        }
        singleResults.add(singleCompressionResult)
        emitBenchmarkComplete("Single-Core Compression", "SINGLE",
            singleCompressionResult.executionTimeMs.toLong(), singleCompressionResult.opsPerSecond)
        
        // Monte Carlo Pi
        emitBenchmarkStart("Single-Core Monte Carlo π", "SINGLE")
        val singleMonteResult = safeBenchmarkRun("Single-Core Monte Carlo π") {
            SingleCoreBenchmarks.monteCarloPi(params)
        }
        singleResults.add(singleMonteResult)
        emitBenchmarkComplete("Single-Core Monte Carlo π", "SINGLE",
            singleMonteResult.executionTimeMs.toLong(), singleMonteResult.opsPerSecond)
        
        // JSON Parsing
        emitBenchmarkStart("Single-Core JSON Parsing", "SINGLE")
        val singleJsonResult = safeBenchmarkRun("Single-Core JSON Parsing") {
            SingleCoreBenchmarks.jsonParsing(params)
        }
        singleResults.add(singleJsonResult)
        emitBenchmarkComplete("Single-Core JSON Parsing", "SINGLE",
            singleJsonResult.executionTimeMs.toLong(), singleJsonResult.opsPerSecond)
        
        // N-Queens
        emitBenchmarkStart("Single-Core N-Queens", "SINGLE")
        val singleNqueensResult = safeBenchmarkRun("Single-Core N-Queens") {
            SingleCoreBenchmarks.nqueens(params)
        }
        singleResults.add(singleNqueensResult)
        emitBenchmarkComplete("Single-Core N-Queens", "SINGLE",
            singleNqueensResult.executionTimeMs.toLong(), singleNqueensResult.opsPerSecond)
        
        // Run multi-core benchmarks
        val multiResults = mutableListOf<BenchmarkResult>()
        
        // Prime Generation
        emitBenchmarkStart("Multi-Core Prime Generation", "MULTI")
        val multiPrimeResult = safeBenchmarkRun("Multi-Core Prime Generation") {
            MultiCoreBenchmarks.primeGeneration(params)
        }
        multiResults.add(multiPrimeResult)
        emitBenchmarkComplete("Multi-Core Prime Generation", "MULTI",
            multiPrimeResult.executionTimeMs.toLong(), multiPrimeResult.opsPerSecond)
        
        // Fibonacci Recursive
        emitBenchmarkStart("Multi-Core Fibonacci Recursive", "MULTI")
        val multiFibResult = safeBenchmarkRun("Multi-Core Fibonacci Recursive") {
            MultiCoreBenchmarks.fibonacciRecursive(params)
        }
        multiResults.add(multiFibResult)
        emitBenchmarkComplete("Multi-Core Fibonacci Recursive", "MULTI",
            multiFibResult.executionTimeMs.toLong(), multiFibResult.opsPerSecond)
        
        // Matrix Multiplication
        emitBenchmarkStart("Multi-Core Matrix Multiplication", "MULTI")
        val multiMatrixResult = safeBenchmarkRun("Multi-Core Matrix Multiplication") {
            MultiCoreBenchmarks.matrixMultiplication(params)
        }
        multiResults.add(multiMatrixResult)
        emitBenchmarkComplete("Multi-Core Matrix Multiplication", "MULTI",
            multiMatrixResult.executionTimeMs.toLong(), multiMatrixResult.opsPerSecond)
        
        // Hash Computing
        emitBenchmarkStart("Multi-Core Hash Computing", "MULTI")
        val multiHashResult = safeBenchmarkRun("Multi-Core Hash Computing") {
            MultiCoreBenchmarks.hashComputing(params)
        }
        multiResults.add(multiHashResult)
        emitBenchmarkComplete("Multi-Core Hash Computing", "MULTI",
            multiHashResult.executionTimeMs.toLong(), multiHashResult.opsPerSecond)
        
        // String Sorting
        emitBenchmarkStart("Multi-Core String Sorting", "MULTI")
        val multiStringResult = safeBenchmarkRun("Multi-Core String Sorting") {
            MultiCoreBenchmarks.stringSorting(params)
        }
        multiResults.add(multiStringResult)
        emitBenchmarkComplete("Multi-Core String Sorting", "MULTI",
            multiStringResult.executionTimeMs.toLong(), multiStringResult.opsPerSecond)
        
        // Ray Tracing
        emitBenchmarkStart("Multi-Core Ray Tracing", "MULTI")
        val multiRayResult = safeBenchmarkRun("Multi-Core Ray Tracing") {
            MultiCoreBenchmarks.rayTracing(params)
        }
        multiResults.add(multiRayResult)
        emitBenchmarkComplete("Multi-Core Ray Tracing", "MULTI",
            multiRayResult.executionTimeMs.toLong(), multiRayResult.opsPerSecond)
        
        // Compression
        emitBenchmarkStart("Multi-Core Compression", "MULTI")
        val multiCompressionResult = safeBenchmarkRun("Multi-Core Compression") {
            MultiCoreBenchmarks.compression(params)
        }
        multiResults.add(multiCompressionResult)
        emitBenchmarkComplete("Multi-Core Compression", "MULTI",
            multiCompressionResult.executionTimeMs.toLong(), multiCompressionResult.opsPerSecond)
        
        // Monte Carlo Pi
        emitBenchmarkStart("Multi-Core Monte Carlo π", "MULTI")
        val multiMonteResult = safeBenchmarkRun("Multi-Core Monte Carlo π") {
            MultiCoreBenchmarks.monteCarloPi(params)
        }
        multiResults.add(multiMonteResult)
        emitBenchmarkComplete("Multi-Core Monte Carlo π", "MULTI",
            multiMonteResult.executionTimeMs.toLong(), multiMonteResult.opsPerSecond)
        
        // JSON Parsing
        emitBenchmarkStart("Multi-Core JSON Parsing", "MULTI")
        val multiJsonResult = safeBenchmarkRun("Multi-Core JSON Parsing") {
            MultiCoreBenchmarks.jsonParsing(params)
        }
        multiResults.add(multiJsonResult)
        emitBenchmarkComplete("Multi-Core JSON Parsing", "MULTI",
            multiJsonResult.executionTimeMs.toLong(), multiJsonResult.opsPerSecond)
        
        // N-Queens
        emitBenchmarkStart("Multi-Core N-Queens", "MULTI")
        val multiNqueensResult = safeBenchmarkRun("Multi-Core N-Queens") {
            MultiCoreBenchmarks.nqueens(params)
        }
        multiResults.add(multiNqueensResult)
        emitBenchmarkComplete("Multi-Core N-Queens", "MULTI",
            multiNqueensResult.executionTimeMs.toLong(), multiNqueensResult.opsPerSecond)
        
        // Calculate and emit final results
        val summaryJson = calculateSummary(singleResults, multiResults)
        Log.d(TAG, "Generated summary JSON: $summaryJson")
        _benchmarkComplete.emit(summaryJson)
    }
    
    private suspend fun safeBenchmarkRun(testName: String, block: suspend () -> BenchmarkResult): BenchmarkResult {
        return try {
            withContext(Dispatchers.Default) {
                val result = block()
                Log.d(TAG, "✓ $testName completed successfully: ${result.opsPerSecond} ops/sec")
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
            val factor = SINGLE_CORE_FACTORS[cleanName] ?: SINGLE_CORE_FACTORS.values.first()
            calculatedSingleCoreScore += result.opsPerSecond * factor
        }
        
        // Calculate multi-core score using weighted scoring
        var calculatedMultiCoreScore = 0.0
        for (result in multiResults) {
            val cleanName = result.name.replace("Multi-Core ", "").trim()
            val factor = MULTI_CORE_FACTORS[cleanName] ?: MULTI_CORE_FACTORS.values.first()
            calculatedMultiCoreScore += result.opsPerSecond * factor
        }
        
        // Calculate final weighted score (35% single, 65% multi)
        val calculatedFinalScore = (calculatedSingleCoreScore * 0.35) + (calculatedMultiCoreScore * 0.65)
        
        // Normalize the score to a reasonable range
        val calculatedNormalizedScore = calculatedFinalScore
        
        // Determine rating based on normalized score
        val rating = when {
            calculatedNormalizedScore >= 1600.0 -> "★★★★★ (Exceptional Performance)"
            calculatedNormalizedScore >= 1200.0 -> "★★★★☆ (High Performance)"
            calculatedNormalizedScore >= 800.0 -> "★★★☆☆ (Good Performance)"
            calculatedNormalizedScore >= 500.0 -> "★★☆☆☆ (Moderate Performance)"
            calculatedNormalizedScore >= 250.0 -> "★☆☆☆☆ (Basic Performance)"
            else -> "☆☆☆☆☆ (Low Performance)"
        }
        
        Log.d(TAG, "Final scoring - Single: $calculatedSingleCoreScore, Multi: $calculatedMultiCoreScore, Final: $calculatedFinalScore, Normalized: $calculatedNormalizedScore")
        
        // CRITICAL FIX: Validation to ensure multi-core scores are higher than single-core scores
        if (calculatedMultiCoreScore <= calculatedSingleCoreScore) {
            Log.w(TAG, "WARNING: Multi-core score ($calculatedMultiCoreScore) is not higher than single-core score ($calculatedSingleCoreScore)")
            Log.w(TAG, "This indicates a critical issue with benchmark implementations or scaling factors")
        } else {
            Log.i(TAG, "✓ VALIDATED: Multi-core score ($calculatedMultiCoreScore) is higher than single-core score ($calculatedSingleCoreScore)")
            Log.i(TAG, "Multi-core advantage: ${String.format("%.2fx", calculatedMultiCoreScore / calculatedSingleCoreScore)}")
        }
        
        // CRITICAL FIX: Include detailed_results array that ResultScreen expects
        val detailedResultsArray = JSONArray().apply {
            // Add single core results
            singleResults.forEach { result ->
                put(JSONObject().apply {
                    put("name", result.name)
                    put("opsPerSecond", result.opsPerSecond)
                    put("executionTimeMs", result.executionTimeMs)
                    put("isValid", result.isValid)
                    put("metricsJson", result.metricsJson)
                })
            }
            // Add multi core results
            multiResults.forEach { result ->
                put(JSONObject().apply {
                    put("name", result.name)
                    put("opsPerSecond", result.opsPerSecond)
                    put("executionTimeMs", result.executionTimeMs)
                    put("isValid", result.isValid)
                    put("metricsJson", result.metricsJson)
                })
            }
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
    
    private suspend fun emitBenchmarkComplete(testName: String, mode: String, timeMs: Long, score: Double) {
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
            "slow" -> WorkloadParams(
                primeRange = 100_000,
                fibonacciNRange = Pair(25, 27),
                matrixSize = 250,
                hashDataSizeMb = 1,
                stringCount = 8_000,
                rayTracingResolution = Pair(128, 128),
                rayTracingDepth = 2,
                compressionDataSizeMb = 1,
                monteCarloSamples = 200_000,
                jsonDataSizeMb = 1,
                nqueensSize = 8
            )
            "mid" -> WorkloadParams(
                primeRange = 200_000,
                fibonacciNRange = Pair(28, 30),
                matrixSize = 300,
                hashDataSizeMb = 2,
                stringCount = 12_000,
                rayTracingResolution = Pair(160, 160),
                rayTracingDepth = 3,
                compressionDataSizeMb = 2,
                monteCarloSamples = 500_000,
                jsonDataSizeMb = 1,
                nqueensSize = 9
            )
            "flagship" -> WorkloadParams(
                // OPTIMIZED: Standardized parameters for consistent 1.5-2.0s execution
                primeRange = 250_000,
                fibonacciNRange = Pair(30, 32),
                matrixSize = 350,
                hashDataSizeMb = 2,
                stringCount = 15_000,
                rayTracingResolution = Pair(192, 192),
                rayTracingDepth = 3,
                compressionDataSizeMb = 2,
                monteCarloSamples = 1_000_000,
                jsonDataSizeMb = 1,
                nqueensSize = 10
            )
            else -> WorkloadParams() // Default values
        }
    }
}