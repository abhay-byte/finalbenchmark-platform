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
        
        // Scaling factors for scoring (from your earlier specifications)
        private val SINGLE_CORE_FACTORS = mapOf(
            "Prime Generation" to 4.0e-8,
            "Fibonacci Recursive" to 0.02,
            "Matrix Multiplication" to 6.0e-8,
            "Hash Computing" to 1.6e-7,
            "String Sorting" to 2.0e-6,
            "Ray Tracing" to 1.2e-6,
            "Compression" to 1.4e-7,
            "Monte Carlo" to 6.0e-7,
            "JSON Parsing" to 2.2e-6,
            "N-Queens" to 8.0e-5
        )
        
        private val MULTI_CORE_FACTORS = mapOf(
            "Prime Generation" to 4.0e-9,
            "Fibonacci Memoized" to 0.008,
            "Matrix Multiplication" to 8.0e-8,
            "Hash Computing" to 1.2e-7,
            "String Sorting" to 3.2e-6,
            "Ray Tracing" to 2.4e-6,
            "Compression" to 2.0e-7,
            "Monte Carlo" to 4.0e-7,
            "JSON Parsing" to 0.028,
            "N-Queens" to 2.0e-4
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
        val singlePrimeResult = withContext(Dispatchers.Default) {
            SingleCoreBenchmarks.primeGeneration(params)
        }
        singleResults.add(singlePrimeResult)
        emitBenchmarkComplete("Single-Core Prime Generation", "SINGLE", 
            singlePrimeResult.executionTimeMs.toLong(), singlePrimeResult.opsPerSecond)
        
        // Fibonacci Recursive
        emitBenchmarkStart("Single-Core Fibonacci Recursive", "SINGLE")
        val singleFibResult = withContext(Dispatchers.Default) {
            SingleCoreBenchmarks.fibonacciRecursive(params)
        }
        singleResults.add(singleFibResult)
        emitBenchmarkComplete("Single-Core Fibonacci Recursive", "SINGLE",
            singleFibResult.executionTimeMs.toLong(), singleFibResult.opsPerSecond)
        
        // Matrix Multiplication
        emitBenchmarkStart("Single-Core Matrix Multiplication", "SINGLE")
        val singleMatrixResult = withContext(Dispatchers.Default) {
            SingleCoreBenchmarks.matrixMultiplication(params)
        }
        singleResults.add(singleMatrixResult)
        emitBenchmarkComplete("Single-Core Matrix Multiplication", "SINGLE",
            singleMatrixResult.executionTimeMs.toLong(), singleMatrixResult.opsPerSecond)
        
        // Hash Computing
        emitBenchmarkStart("Single-Core Hash Computing", "SINGLE")
        val singleHashResult = withContext(Dispatchers.Default) {
            SingleCoreBenchmarks.hashComputing(params)
        }
        singleResults.add(singleHashResult)
        emitBenchmarkComplete("Single-Core Hash Computing", "SINGLE",
            singleHashResult.executionTimeMs.toLong(), singleHashResult.opsPerSecond)
        
        // String Sorting
        emitBenchmarkStart("Single-Core String Sorting", "SINGLE")
        val singleStringResult = withContext(Dispatchers.Default) {
            SingleCoreBenchmarks.stringSorting(params)
        }
        singleResults.add(singleStringResult)
        emitBenchmarkComplete("Single-Core String Sorting", "SINGLE",
            singleStringResult.executionTimeMs.toLong(), singleStringResult.opsPerSecond)
        
        // Ray Tracing
        emitBenchmarkStart("Single-Core Ray Tracing", "SINGLE")
        val singleRayResult = withContext(Dispatchers.Default) {
            SingleCoreBenchmarks.rayTracing(params)
        }
        singleResults.add(singleRayResult)
        emitBenchmarkComplete("Single-Core Ray Tracing", "SINGLE",
            singleRayResult.executionTimeMs.toLong(), singleRayResult.opsPerSecond)
        
        // Compression
        emitBenchmarkStart("Single-Core Compression", "SINGLE")
        val singleCompressionResult = withContext(Dispatchers.Default) {
            SingleCoreBenchmarks.compression(params)
        }
        singleResults.add(singleCompressionResult)
        emitBenchmarkComplete("Single-Core Compression", "SINGLE",
            singleCompressionResult.executionTimeMs.toLong(), singleCompressionResult.opsPerSecond)
        
        // Monte Carlo Pi
        emitBenchmarkStart("Single-Core Monte Carlo π", "SINGLE")
        val singleMonteResult = withContext(Dispatchers.Default) {
            SingleCoreBenchmarks.monteCarloPi(params)
        }
        singleResults.add(singleMonteResult)
        emitBenchmarkComplete("Single-Core Monte Carlo π", "SINGLE",
            singleMonteResult.executionTimeMs.toLong(), singleMonteResult.opsPerSecond)
        
        // JSON Parsing
        emitBenchmarkStart("Single-Core JSON Parsing", "SINGLE")
        val singleJsonResult = withContext(Dispatchers.Default) {
            SingleCoreBenchmarks.jsonParsing(params)
        }
        singleResults.add(singleJsonResult)
        emitBenchmarkComplete("Single-Core JSON Parsing", "SINGLE",
            singleJsonResult.executionTimeMs.toLong(), singleJsonResult.opsPerSecond)
        
        // N-Queens
        emitBenchmarkStart("Single-Core N-Queens", "SINGLE")
        val singleNqueensResult = withContext(Dispatchers.Default) {
            SingleCoreBenchmarks.nqueens(params)
        }
        singleResults.add(singleNqueensResult)
        emitBenchmarkComplete("Single-Core N-Queens", "SINGLE",
            singleNqueensResult.executionTimeMs.toLong(), singleNqueensResult.opsPerSecond)
        
        // Run multi-core benchmarks
        val multiResults = mutableListOf<BenchmarkResult>()
        
        // Prime Generation
        emitBenchmarkStart("Multi-Core Prime Generation", "MULTI")
        val multiPrimeResult = withContext(Dispatchers.Default) {
            MultiCoreBenchmarks.primeGeneration(params)
        }
        multiResults.add(multiPrimeResult)
        emitBenchmarkComplete("Multi-Core Prime Generation", "MULTI",
            multiPrimeResult.executionTimeMs.toLong(), multiPrimeResult.opsPerSecond)
        
        // Fibonacci Memoized
        emitBenchmarkStart("Multi-Core Fibonacci Memoized", "MULTI")
        val multiFibResult = withContext(Dispatchers.Default) {
            MultiCoreBenchmarks.fibonacciMemoized(params)
        }
        multiResults.add(multiFibResult)
        emitBenchmarkComplete("Multi-Core Fibonacci Memoized", "MULTI",
            multiFibResult.executionTimeMs.toLong(), multiFibResult.opsPerSecond)
        
        // Matrix Multiplication
        emitBenchmarkStart("Multi-Core Matrix Multiplication", "MULTI")
        val multiMatrixResult = withContext(Dispatchers.Default) {
            MultiCoreBenchmarks.matrixMultiplication(params)
        }
        multiResults.add(multiMatrixResult)
        emitBenchmarkComplete("Multi-Core Matrix Multiplication", "MULTI",
            multiMatrixResult.executionTimeMs.toLong(), multiMatrixResult.opsPerSecond)
        
        // Hash Computing
        emitBenchmarkStart("Multi-Core Hash Computing", "MULTI")
        val multiHashResult = withContext(Dispatchers.Default) {
            MultiCoreBenchmarks.hashComputing(params)
        }
        multiResults.add(multiHashResult)
        emitBenchmarkComplete("Multi-Core Hash Computing", "MULTI",
            multiHashResult.executionTimeMs.toLong(), multiHashResult.opsPerSecond)
        
        // String Sorting
        emitBenchmarkStart("Multi-Core String Sorting", "MULTI")
        val multiStringResult = withContext(Dispatchers.Default) {
            MultiCoreBenchmarks.stringSorting(params)
        }
        multiResults.add(multiStringResult)
        emitBenchmarkComplete("Multi-Core String Sorting", "MULTI",
            multiStringResult.executionTimeMs.toLong(), multiStringResult.opsPerSecond)
        
        // Ray Tracing
        emitBenchmarkStart("Multi-Core Ray Tracing", "MULTI")
        val multiRayResult = withContext(Dispatchers.Default) {
            MultiCoreBenchmarks.rayTracing(params)
        }
        multiResults.add(multiRayResult)
        emitBenchmarkComplete("Multi-Core Ray Tracing", "MULTI",
            multiRayResult.executionTimeMs.toLong(), multiRayResult.opsPerSecond)
        
        // Compression
        emitBenchmarkStart("Multi-Core Compression", "MULTI")
        val multiCompressionResult = withContext(Dispatchers.Default) {
            MultiCoreBenchmarks.compression(params)
        }
        multiResults.add(multiCompressionResult)
        emitBenchmarkComplete("Multi-Core Compression", "MULTI",
            multiCompressionResult.executionTimeMs.toLong(), multiCompressionResult.opsPerSecond)
        
        // Monte Carlo Pi
        emitBenchmarkStart("Multi-Core Monte Carlo π", "MULTI")
        val multiMonteResult = withContext(Dispatchers.Default) {
            MultiCoreBenchmarks.monteCarloPi(params)
        }
        multiResults.add(multiMonteResult)
        emitBenchmarkComplete("Multi-Core Monte Carlo π", "MULTI",
            multiMonteResult.executionTimeMs.toLong(), multiMonteResult.opsPerSecond)
        
        // JSON Parsing
        emitBenchmarkStart("Multi-Core JSON Parsing", "MULTI")
        val multiJsonResult = withContext(Dispatchers.Default) {
            MultiCoreBenchmarks.jsonParsing(params)
        }
        multiResults.add(multiJsonResult)
        emitBenchmarkComplete("Multi-Core JSON Parsing", "MULTI",
            multiJsonResult.executionTimeMs.toLong(), multiJsonResult.opsPerSecond)
        
        // N-Queens
        emitBenchmarkStart("Multi-Core N-Queens", "MULTI")
        val multiNqueensResult = withContext(Dispatchers.Default) {
            MultiCoreBenchmarks.nqueens(params)
        }
        multiResults.add(multiNqueensResult)
        emitBenchmarkComplete("Multi-Core N-Queens", "MULTI",
            multiNqueensResult.executionTimeMs.toLong(), multiNqueensResult.opsPerSecond)
        
        // Calculate and emit final results
        val summaryJson = calculateSummary(singleResults, multiResults)
        _benchmarkComplete.emit(summaryJson)
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
        
        // CRITICAL FIX: Include the result arrays that BenchmarkManager expects
        val singleCoreResultsArray = JSONArray().apply {
            singleResults.forEach { result ->
                put(JSONObject().apply {
                    put("name", result.name)
                    put("ops_per_second", result.opsPerSecond)
                    put("execution_time_ms", result.executionTimeMs)
                    put("is_valid", result.isValid)
                    put("metrics_json", result.metricsJson)
                })
            }
        }
        
        val multiCoreResultsArray = JSONArray().apply {
            multiResults.forEach { result ->
                put(JSONObject().apply {
                    put("name", result.name)
                    put("ops_per_second", result.opsPerSecond)
                    put("execution_time_ms", result.executionTimeMs)
                    put("is_valid", result.isValid)
                    put("metrics_json", result.metricsJson)
                })
            }
        }
        
        return JSONObject().apply {
            put("single_core_score", calculatedSingleCoreScore)
            put("multi_core_score", calculatedMultiCoreScore)
            put("final_score", calculatedFinalScore)
            put("normalized_score", calculatedNormalizedScore)
            put("rating", rating)
            put("single_core_results", singleCoreResultsArray)
            put("multi_core_results", multiCoreResultsArray)
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
                primeRange = 10_000,
                fibonacciNRange = Pair(20, 25),
                matrixSize = 100,
                hashDataSizeMb = 5,
                stringCount = 50_000,
                rayTracingResolution = Pair(128, 128),
                rayTracingDepth = 1,
                compressionDataSizeMb = 1,
                monteCarloSamples = 5_000,
                jsonDataSizeMb = 1,
                nqueensSize = 2
            )
            "mid" -> WorkloadParams(
                primeRange = 150_000,
                fibonacciNRange = Pair(28, 32),
                matrixSize = 300,
                hashDataSizeMb = 15,
                stringCount = 150_000,
                rayTracingResolution = Pair(168, 168),
                rayTracingDepth = 2,
                compressionDataSizeMb = 2,
                monteCarloSamples = 10_000,
                jsonDataSizeMb = 1,
                nqueensSize = 4
            )
            "flagship" -> WorkloadParams(
                // MOBILE-SAFE: Ultra-conservative parameters for 2-5 second tests
                primeRange = 300_000,           // Reduced from 500K - mobile-safe range
                fibonacciNRange = Pair(32, 34), // Reduced from 35-38 for speed
                matrixSize = 300,               // Reduced from 350 (O(N^3) complexity - critical)
                hashDataSizeMb = 2,             // Increased to 2MB for meaningful hash tests
                stringCount = 20_000,           // Increased from 12K for meaningful sort tests
                rayTracingResolution = Pair(200, 200), // Reduced from 400x400 for speed
                rayTracingDepth = 3,            // Kept at 3 for speed
                compressionDataSizeMb = 3,      // Set to 1MB for meaningful compression tests
                monteCarloSamples = 500_000,    // Reduced from 2M for speed
                jsonDataSizeMb = 1,             // Reduced from 2MB for speed
                nqueensSize = 10                // Reduced from 12 for speed
            )
            else -> WorkloadParams() // Default values
        }
    }
}