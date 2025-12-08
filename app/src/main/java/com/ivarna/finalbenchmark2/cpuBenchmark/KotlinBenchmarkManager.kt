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
        
        // UPDATED SCALING FACTORS - Optimized algorithms with new factors
        // Target: ~10,000 total points for flagship devices with optimized algorithms
        private val SINGLE_CORE_FACTORS = mapOf(
            "Prime Generation" to 2.5e-5,     // Optimized: Higher due to reduced allocations
            "Fibonacci Recursive" to 1.2e-5,  // Optimized: Raw CPU usage without memoization
            "Matrix Multiplication" to 4.0e-6, // Optimized: Cache-friendly algorithms
            "Hash Computing" to 6.0e-3,       // Optimized: SHA-256 throughput focus
            "String Sorting" to 5.0e-3,       // Optimized: Pre-generated strings, measure only sort
            "Ray Tracing" to 1.5e-3,          // Optimized: Parallel ray computation
            "Compression" to 5.0e-4,          // Optimized: Static buffer, zero allocation
            "Monte Carlo" to 2.0e-4,          // Optimized: ThreadLocalRandom, primitive only
            "JSON Parsing" to 1.8e-3,         // Optimized: Element counting focus
            "N-Queens" to 8.0e-2              // Optimized: Backtracking efficiency
        )
        
        private val MULTI_CORE_FACTORS = mapOf(
            "Prime Generation" to 6.0e-6,     // Optimized: Parallel prime counting
            "Fibonacci Recursive" to 1.0e-5,  // Optimized: No memoization, pure parallel
            "Matrix Multiplication" to 3.5e-6, // Optimized: Parallel matrix operations
            "Hash Computing" to 3.0e-3,       // Optimized: Parallel SHA-256 hashing
            "String Sorting" to 3.0e-3,       // Optimized: Pre-generated, parallel sort
            "Ray Tracing" to 1.0e-3,          // Optimized: Parallel ray tracing
            "Compression" to 6.0e-4,          // Optimized: Static buffer, parallel compression
            "Monte Carlo" to 3.5e-4,          // Optimized: ThreadLocalRandom, parallel samples
            "JSON Parsing" to 3.5e-3,         // Optimized: Parallel JSON parsing
            "N-Queens" to 3.0e-3              // Optimized: Work-stealing parallel backtracking
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
        
        // Fibonacci Recursive
        emitBenchmarkStart("Multi-Core Fibonacci Recursive", "MULTI")
        val multiFibResult = withContext(Dispatchers.Default) {
            MultiCoreBenchmarks.fibonacciRecursive(params)
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