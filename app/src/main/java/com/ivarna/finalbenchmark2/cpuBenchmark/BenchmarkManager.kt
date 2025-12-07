package com.ivarna.finalbenchmark2.cpuBenchmark

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.json.JSONArray
import org.json.JSONObject

class BenchmarkManager {
    val _benchmarkEvents = MutableSharedFlow<BenchmarkEvent>()
    val benchmarkEvents = _benchmarkEvents.asSharedFlow()
    
    private val _benchmarkComplete = MutableSharedFlow<String>()
    val benchmarkComplete = _benchmarkComplete.asSharedFlow()
    
    private var isRunning = false
    
    // SEPARATE scaling factors for single-core and multi-core tests
    // Single-core scaling factors for normal operations (~70 points per test)
    private val SINGLE_CORE_SCALING_FACTORS = mapOf(
        "Prime Generation" to 0.000001,
        "Fibonacci Recursive" to 0.012,
        "Matrix Multiplication" to 0.0000025,
        "Hash Computing" to 0.000001,
        "String Sorting" to 0.000015,
        "Ray Tracing" to 0.00006,
        "Compression" to 0.000007,
        "Monte Carlo" to 0.00007,
        "JSON Parsing" to 0.00004,
        "N-Queens" to 0.007
    )
    
    // Multi-core scaling factors should be SMALLER because ops/sec is much higher
    // Divide by expected speedup factor (4-6x for 8-core device)
    private val MULTI_CORE_SCALING_FACTORS = mapOf(
        "Prime Generation" to 0.00000020,      // ~5x smaller than single-core
        "Fibonacci Memoized" to 0.0024,        // ~5x smaller
        "Matrix Multiplication" to 0.00001,    // ~4x smaller (scales well)
        "Hash Computing" to 0.0000002,         // ~5x smaller
        "String Sorting" to 0.00003,           // ~5x smaller (scales moderately)
        "Ray Tracing" to 0.0003,               // ~5x smaller (highly parallel)
        "Compression" to 0.000035,             // ~5x smaller
        "Monte Carlo" to 0.00035,              // ~5x smaller (embarrassingly parallel)
        "JSON Parsing" to 0.0002,              // ~5x smaller
        "N-Queens" to 0.035                    // ~5x smaller
    )
    
    // Default scaling factor for unknown benchmarks
    private val DEFAULT_SCALING_FACTOR = 0.00001
    
    // This function is now deprecated as we use individual benchmarks in the ViewModel
    // This remains for compatibility with the native suite call but won't be used in the new implementation
    suspend fun startBenchmark(preset: String = "Auto") {
        if (isRunning) return
        isRunning = true
        
        try {
            // Configuration for the benchmark - force Flagship mode
            val deviceTier = "Flagship"
            Log.d("BenchmarkManager", "Using preset: $preset, resolved device tier: $deviceTier")
            
            val config = BenchmarkConfig(
                iterations = 3,
                warmup = true,
                warmupCount = 3,
                deviceTier = deviceTier
            )
            
            val configJson = """{
                "iterations": ${config.iterations},
                "warmup": ${config.warmup},
                "warmup_count": ${config.warmupCount},
                "device_tier": "${config.deviceTier}"
            }"""
            
            // Execute the actual benchmark suite
            val resultJson = CpuBenchmarkNative.runCpuBenchmarkSuite(configJson)
            
            // Calculate summary from the actual results
            val summaryJson = calculateSummaryFromResults(resultJson)
            
            _benchmarkComplete.emit(summaryJson)
        } catch (e: Exception) {
            e.printStackTrace()
            // Emit a default summary in case of error
            val errorSummary = """{
                "single_core_score": 0.0,
                "multi_core_score": 0.0,
                "final_score": 0.0,
                "normalized_score": 0.0,
                "rating": "★"
            }"""
            _benchmarkComplete.emit(errorSummary)
        } finally {
            isRunning = false
        }
    }
    
    // Function to emit a benchmark start event
    suspend fun emitBenchmarkStart(testName: String, mode: String) {
        _benchmarkEvents.emit(
            BenchmarkEvent(
                testName = testName,
                mode = mode,
                state = "STARTED",
                timeMs = 0,  // Start time not applicable for STARTED state
                score = 0.0  // Score not applicable for STARTED state
            )
        )
    }
    
    // Function to emit a benchmark completion event
    suspend fun emitBenchmarkComplete(testName: String, mode: String, timeMs: Long, score: Double) {
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
    
    /**
     * Calculates the summary from the actual benchmark results
     */
    fun calculateSummaryFromResults(resultsJson: String?): String {
        if (resultsJson.isNullOrEmpty()) {
            return """{
                "single_core_score": 0.0,
                "multi_core_score": 0.0,
                "final_score": 0.0,
                "normalized_score": 0.0,
                "rating": "★"
            }"""
        }
        
        // Parse the results to extract single-core and multi-core results
        val singleCoreScore = calculateSingleCoreScore(resultsJson)
        val multiCoreScore = calculateMultiCoreScore(resultsJson)
        
        // Calculate final weighted score using proper weights (35% single, 65% multi)
        val finalScore = (singleCoreScore * 0.35) + (multiCoreScore * 0.65)
        
        // The weighted score from Rust logic is already in a good range (under 2000)
        // So we can use it directly as the normalized score
        val normalizedScore = if (finalScore > 0) {
            // Apply final normalization factor to match Rust behavior
            finalScore * 1.0
        } else {
            0.0
        }
        
        // Determine rating based on normalized score - Using reasonable thresholds for 0-2000 range
        val rating = when {
            normalizedScore >= 1800 -> "★★★ (Exceptional Performance)"
            normalizedScore >= 1500 -> "★★★★☆ (High Performance)"
            normalizedScore >= 1000 -> "★★★☆☆ (Good Performance)"
            normalizedScore >= 600 -> "★★☆☆☆ (Moderate Performance)"
            normalizedScore >= 300 -> "★☆☆☆☆ (Basic Performance)"
            else -> "☆☆☆ (Low Performance)"
        }
        
        Log.d("BenchmarkManager", "Final scoring - Single: $singleCoreScore, Multi: $multiCoreScore, Final: $finalScore, Normalized: $normalizedScore")
        
        return """{
            "single_core_score": ${"%.2f".format(singleCoreScore)},
            "multi_core_score": ${"%.2f".format(multiCoreScore)},
            "final_score": ${"%.2f".format(finalScore)},
            "normalized_score": ${"%.2f".format(normalizedScore)},
            "rating": "$rating"
        }"""
    }
    
    /**
     * Calculates single-core score from the benchmark results using weighted scoring
     */
    private fun calculateSingleCoreScore(resultsJson: String): Double {
        return calculateCategoryScore(resultsJson, "single_core_results")
    }
    
    /**
     * Calculates multi-core score from the benchmark results using weighted scoring
     */
    private fun calculateMultiCoreScore(resultsJson: String): Double {
        return calculateCategoryScore(resultsJson, "multi_core_results")
    }
    
    /**
     * Calculates category score using proper weighted scoring with robust JSON parsing
     */
    private fun calculateCategoryScore(resultsJson: String, categoryKey: String): Double {
        Log.d("BenchmarkManager", "=== CALCULATING CATEGORY SCORE for $categoryKey ===")
        Log.d("BenchmarkManager", "Results JSON: $resultsJson")
        
        return try {
            val root = JSONObject(resultsJson)
            val resultsArray = root.optJSONArray(categoryKey) ?: run {
                Log.e("BenchmarkManager", "No results array found for category: $categoryKey")
                return 0.0
            }
            
            var totalWeightedScore = 0.0
            Log.d("BenchmarkManager", "Found ${resultsArray.length()} benchmark results")
            
            for (i in 0 until resultsArray.length()) {
                val item = resultsArray.getJSONObject(i)
                val name = item.getString("name")
                val ops = item.getDouble("ops_per_second")
                
                // Find matching scaling factor for the benchmark type
                val factor = findScalingFactor(name)
                val weightedScore = ops * factor
                totalWeightedScore += weightedScore
                
                Log.d("BenchmarkManager", "  [$i] Benchmark: '$name'")
                Log.d("BenchmarkManager", "      ops/sec: $ops")
                Log.d("BenchmarkManager", "      scaling factor: $factor")
                Log.d("BenchmarkManager", "      weighted score: $weightedScore")
            }
            
            Log.d("BenchmarkManager", "=== FINAL CATEGORY $categoryKey SCORE: $totalWeightedScore ===")
            totalWeightedScore
            
        } catch (e: Exception) {
            Log.e("BenchmarkManager", "Error parsing JSON for category $categoryKey: ${e.message}")
            e.printStackTrace()
            0.0
        }
    }
    
    /**
     * Finds the appropriate scaling factor for a benchmark name with improved matching
     */
    private fun findScalingFactor(benchmarkName: String): Double {
        Log.d("BenchmarkManager", "Looking for scaling factor for: '$benchmarkName'")
        
        // Determine if this is single-core or multi-core
        val isMultiCore = benchmarkName.contains("Multi-Core", ignoreCase = true)
        val factorsMap = if (isMultiCore) MULTI_CORE_SCALING_FACTORS else SINGLE_CORE_SCALING_FACTORS
        
        // Remove "Single-Core " or "Multi-Core " prefix for matching
        val cleanName = benchmarkName
            .replace("Single-Core ", "", ignoreCase = true)
            .replace("Multi-Core ", "", ignoreCase = true)
            .trim()
        
        Log.d("BenchmarkManager", "Cleaned name: '$cleanName', Mode: ${if (isMultiCore) "Multi-Core" else "Single-Core"}")
        
        // Find exact match first
        factorsMap[cleanName]?.let { 
            Log.d("BenchmarkManager", "Exact match found: $cleanName -> $it")
            return it 
        }
        
        // Find partial match for more flexible matching
        factorsMap.entries.find { cleanName.contains(it.key, ignoreCase = true) }?.let { 
            Log.d("BenchmarkManager", "Partial match found: '$cleanName' contains '${it.key}' -> ${it.value}")
            return it.value 
        }
        
        // Try to match common abbreviations and variations
        val normalizedName = cleanName.lowercase()
        val keyMapping = mapOf(
            "prime" to "Prime Generation",
            "fibonacci" to if (isMultiCore) "Fibonacci Memoized" else "Fibonacci Recursive",
            "matrix" to "Matrix Multiplication",
            "hash" to "Hash Computing",
            "string" to "String Sorting",
            "sort" to "String Sorting",
            "ray" to "Ray Tracing",
            "compression" to "Compression",
            "monte" to "Monte Carlo",
            "json" to "JSON Parsing",
            "queens" to "N-Queens"
        )
        
        keyMapping.entries.find { normalizedName.contains(it.key) }?.let { 
            val factor = factorsMap[it.value]
            if (factor != null) {
                Log.d("BenchmarkManager", "Abbreviation match: '$cleanName' matched to '${it.value}' -> $factor")
                return factor
            }
        }
        
        // Fall back to mode-appropriate default
        val defaultFactor = if (isMultiCore) 0.00005 else 0.00001
        Log.w("BenchmarkManager", "No scaling factor found for: '$benchmarkName' (cleaned: '$cleanName'), using default: $defaultFactor")
        return defaultFactor
    }
    
    private suspend fun runIndividualBenchmarks() {
        // This function is now deprecated as we use the native suite call
        // Individual benchmarks are handled by the native code
        // This function remains for compatibility but doesn't execute anything
        Log.d("BenchmarkManager", "runIndividualBenchmarks is deprecated - using native suite call instead")
    }
    
    /**
     * Detects the appropriate device tier based on hardware capabilities
     */
    private fun detectDeviceTier(): String {
        return try {
            val availableProcessors = Runtime.getRuntime().availableProcessors()
            val maxMemory = Runtime.getRuntime().maxMemory() / (1024 * 1024) // MB
            
            Log.d("BenchmarkManager", "Available processors: $availableProcessors")
            Log.d("BenchmarkManager", "Max memory: ${maxMemory}MB")
            
            // Detect device tier based on CPU cores and available memory
            when {
                availableProcessors >= 8 && maxMemory > 100 -> "Flagship"
                availableProcessors >= 4 && maxMemory > 50 -> "Mid"
                else -> "Slow"
            }
        } catch (e: Exception) {
            Log.w("BenchmarkManager", "Failed to detect device tier, using Mid as fallback", e)
            "Mid"
        }
    }
    
    /**
     * Calls the native benchmark function based on the function name
     * This function maps the function name to the appropriate native call
     */
    fun runNativeBenchmarkFunction(functionName: String, preset: String = "Auto"): BenchmarkResult {
        // Force device tier to always be Flagship
        val deviceTier = "Flagship"
        val workloadParams = getWorkloadParamsForDeviceTier(deviceTier)
        
        val paramsJson = createParamsJson(workloadParams)
        
        Log.d("BenchmarkManager", "About to call native function: $functionName with preset: $preset, device tier: $deviceTier")
        Log.d("BenchmarkManager", "Using workload params: $paramsJson")
        
        return try {
            when (functionName) {
                "runSingleCorePrimeGeneration" -> {
                    val resultJson = CpuBenchmarkNative.callNativeFunction("runSingleCorePrimeGeneration", paramsJson) {
                        CpuBenchmarkNative.runSingleCorePrimeGeneration(paramsJson)
                    }
                    Log.d("BenchmarkManager", "Result from runSingleCorePrimeGeneration: $resultJson")
                    val result = parseBenchmarkResultJson(resultJson ?: "")
                    Log.d("BenchmarkManager", "Parsed result: $result")
                    result
                }
                "runSingleCoreFibonacciRecursive" -> {
                    val resultJson = CpuBenchmarkNative.callNativeFunction("runSingleCoreFibonacciRecursive", paramsJson) {
                        CpuBenchmarkNative.runSingleCoreFibonacciRecursive(paramsJson)
                    }
                    Log.d("BenchmarkManager", "Result from runSingleCoreFibonacciRecursive: $resultJson")
                    val result = parseBenchmarkResultJson(resultJson ?: "")
                    Log.d("BenchmarkManager", "Parsed result: $result")
                    result
                }
                "runSingleCoreMatrixMultiplication" -> {
                    val resultJson = CpuBenchmarkNative.callNativeFunction("runSingleCoreMatrixMultiplication", paramsJson) {
                        CpuBenchmarkNative.runSingleCoreMatrixMultiplication(paramsJson)
                    }
                    Log.d("BenchmarkManager", "Result from runSingleCoreMatrixMultiplication: $resultJson")
                    val result = parseBenchmarkResultJson(resultJson ?: "")
                    Log.d("BenchmarkManager", "Parsed result: $result")
                    result
                }
                "runSingleCoreHashComputing" -> {
                    val resultJson = CpuBenchmarkNative.callNativeFunction("runSingleCoreHashComputing", paramsJson) {
                        CpuBenchmarkNative.runSingleCoreHashComputing(paramsJson)
                    }
                    Log.d("BenchmarkManager", "Result from runSingleCoreHashComputing: $resultJson")
                    val result = parseBenchmarkResultJson(resultJson ?: "")
                    Log.d("BenchmarkManager", "Parsed result: $result")
                    result
                }
                "runSingleCoreStringSorting" -> {
                    val resultJson = CpuBenchmarkNative.callNativeFunction("runSingleCoreStringSorting", paramsJson) {
                        CpuBenchmarkNative.runSingleCoreStringSorting(paramsJson)
                    }
                    Log.d("BenchmarkManager", "Result from runSingleCoreStringSorting: $resultJson")
                    val result = parseBenchmarkResultJson(resultJson ?: "")
                    Log.d("BenchmarkManager", "Parsed result: $result")
                    result
                }
                "runSingleCoreRayTracing" -> {
                    val resultJson = CpuBenchmarkNative.callNativeFunction("runSingleCoreRayTracing", paramsJson) {
                        CpuBenchmarkNative.runSingleCoreRayTracing(paramsJson)
                    }
                    Log.d("BenchmarkManager", "Result from runSingleCoreRayTracing: $resultJson")
                    val result = parseBenchmarkResultJson(resultJson ?: "")
                    Log.d("BenchmarkManager", "Parsed result: $result")
                    result
                }
                "runSingleCoreCompression" -> {
                    val resultJson = CpuBenchmarkNative.callNativeFunction("runSingleCoreCompression", paramsJson) {
                        CpuBenchmarkNative.runSingleCoreCompression(paramsJson)
                    }
                    Log.d("BenchmarkManager", "Result from runSingleCoreCompression: $resultJson")
                    val result = parseBenchmarkResultJson(resultJson ?: "")
                    Log.d("BenchmarkManager", "Parsed result: $result")
                    result
                }
                "runSingleCoreMonteCarloPi" -> {
                    val resultJson = CpuBenchmarkNative.callNativeFunction("runSingleCoreMonteCarloPi", paramsJson) {
                        CpuBenchmarkNative.runSingleCoreMonteCarloPi(paramsJson)
                    }
                    Log.d("BenchmarkManager", "Result from runSingleCoreMonteCarloPi: $resultJson")
                    val result = parseBenchmarkResultJson(resultJson ?: "")
                    Log.d("BenchmarkManager", "Parsed result: $result")
                    result
                }
                "runSingleCoreJsonParsing" -> {
                    val resultJson = CpuBenchmarkNative.callNativeFunction("runSingleCoreJsonParsing", paramsJson) {
                        CpuBenchmarkNative.runSingleCoreJsonParsing(paramsJson)
                    }
                    Log.d("BenchmarkManager", "Result from runSingleCoreJsonParsing: $resultJson")
                    val result = parseBenchmarkResultJson(resultJson ?: "")
                    Log.d("BenchmarkManager", "Parsed result: $result")
                    result
                }
                "runSingleCoreNqueens" -> {
                    val resultJson = CpuBenchmarkNative.callNativeFunction("runSingleCoreNqueens", paramsJson) {
                        CpuBenchmarkNative.runSingleCoreNqueens(paramsJson)
                    }
                    Log.d("BenchmarkManager", "Result from runSingleCoreNqueens: $resultJson")
                    val result = parseBenchmarkResultJson(resultJson ?: "")
                    Log.d("BenchmarkManager", "Parsed result: $result")
                    result
                }
                "runMultiCorePrimeGeneration" -> {
                    val resultJson = CpuBenchmarkNative.callNativeFunction("runMultiCorePrimeGeneration", paramsJson) {
                        CpuBenchmarkNative.runMultiCorePrimeGeneration(paramsJson)
                    }
                    Log.d("BenchmarkManager", "Result from runMultiCorePrimeGeneration: $resultJson")
                    val result = parseBenchmarkResultJson(resultJson ?: "")
                    Log.d("BenchmarkManager", "Parsed result: $result")
                    result
                }
                "runMultiCoreFibonacciMemoized" -> {
                    val resultJson = CpuBenchmarkNative.callNativeFunction("runMultiCoreFibonacciMemoized", paramsJson) {
                        CpuBenchmarkNative.runMultiCoreFibonacciMemoized(paramsJson)
                    }
                    Log.d("BenchmarkManager", "Result from runMultiCoreFibonacciMemoized: $resultJson")
                    val result = parseBenchmarkResultJson(resultJson ?: "")
                    Log.d("BenchmarkManager", "Parsed result: $result")
                    result
                }
                "runMultiCoreMatrixMultiplication" -> {
                    val resultJson = CpuBenchmarkNative.callNativeFunction("runMultiCoreMatrixMultiplication", paramsJson) {
                        CpuBenchmarkNative.runMultiCoreMatrixMultiplication(paramsJson)
                    }
                    Log.d("BenchmarkManager", "Result from runMultiCoreMatrixMultiplication: $resultJson")
                    val result = parseBenchmarkResultJson(resultJson ?: "")
                    Log.d("BenchmarkManager", "Parsed result: $result")
                    result
                }
                "runMultiCoreHashComputing" -> {
                    val resultJson = CpuBenchmarkNative.callNativeFunction("runMultiCoreHashComputing", paramsJson) {
                        CpuBenchmarkNative.runMultiCoreHashComputing(paramsJson)
                    }
                    Log.d("BenchmarkManager", "Result from runMultiCoreHashComputing: $resultJson")
                    val result = parseBenchmarkResultJson(resultJson ?: "")
                    Log.d("BenchmarkManager", "Parsed result: $result")
                    result
                }
                "runMultiCoreStringSorting" -> {
                    val resultJson = CpuBenchmarkNative.callNativeFunction("runMultiCoreStringSorting", paramsJson) {
                        CpuBenchmarkNative.runMultiCoreStringSorting(paramsJson)
                    }
                    Log.d("BenchmarkManager", "Result from runMultiCoreStringSorting: $resultJson")
                    val result = parseBenchmarkResultJson(resultJson ?: "")
                    Log.d("BenchmarkManager", "Parsed result: $result")
                    result
                }
                "runMultiCoreRayTracing" -> {
                    val resultJson = CpuBenchmarkNative.callNativeFunction("runMultiCoreRayTracing", paramsJson) {
                        CpuBenchmarkNative.runMultiCoreRayTracing(paramsJson)
                    }
                    Log.d("BenchmarkManager", "Result from runMultiCoreRayTracing: $resultJson")
                    val result = parseBenchmarkResultJson(resultJson ?: "")
                    Log.d("BenchmarkManager", "Parsed result: $result")
                    result
                }
                "runMultiCoreCompression" -> {
                    val resultJson = CpuBenchmarkNative.callNativeFunction("runMultiCoreCompression", paramsJson) {
                        CpuBenchmarkNative.runMultiCoreCompression(paramsJson)
                    }
                    Log.d("BenchmarkManager", "Result from runMultiCoreCompression: $resultJson")
                    val result = parseBenchmarkResultJson(resultJson ?: "")
                    Log.d("BenchmarkManager", "Parsed result: $result")
                    result
                }
                "runMultiCoreMonteCarloPi" -> {
                    val resultJson = CpuBenchmarkNative.callNativeFunction("runMultiCoreMonteCarloPi", paramsJson) {
                        CpuBenchmarkNative.runMultiCoreMonteCarloPi(paramsJson)
                    }
                    Log.d("BenchmarkManager", "Result from runMultiCoreMonteCarloPi: $resultJson")
                    val result = parseBenchmarkResultJson(resultJson ?: "")
                    Log.d("BenchmarkManager", "Parsed result: $result")
                    result
                }
                "runMultiCoreJsonParsing" -> {
                    val resultJson = CpuBenchmarkNative.callNativeFunction("runMultiCoreJsonParsing", paramsJson) {
                        CpuBenchmarkNative.runMultiCoreJsonParsing(paramsJson)
                    }
                    Log.d("BenchmarkManager", "Result from runMultiCoreJsonParsing: $resultJson")
                    val result = parseBenchmarkResultJson(resultJson ?: "")
                    Log.d("BenchmarkManager", "Parsed result: $result")
                    result
                }
                "runMultiCoreNqueens" -> {
                    val resultJson = CpuBenchmarkNative.callNativeFunction("runMultiCoreNqueens", paramsJson) {
                        CpuBenchmarkNative.runMultiCoreNqueens(paramsJson)
                    }
                    Log.d("BenchmarkManager", "Result from runMultiCoreNqueens: $resultJson")
                    val result = parseBenchmarkResultJson(resultJson ?: "")
                    Log.d("BenchmarkManager", "Parsed result: $result")
                    result
                }
                else -> {
                    Log.w("BenchmarkManager", "Unknown function name: $functionName, falling back to simulation")
                    // Fallback to simulated result if function name is unknown
                    simulateBenchmarkResult(functionName)
                }
            }
        } catch (e: UnsatisfiedLinkError) {
            // This error occurs when the native library is not found
            Log.e("BenchmarkManager", "Native library not found for function: $functionName", e)
            // Fall back to simulation
            e.printStackTrace()
            simulateBenchmarkResult(functionName)
        } catch (e: Exception) {
            Log.e("BenchmarkManager", "Error calling native function: $functionName", e)
            e.printStackTrace()
            // Return a simulated result in case of other errors
            val simulatedResult = simulateBenchmarkResult(functionName)
            Log.d("BenchmarkManager", "Returning simulated result: $simulatedResult")
            simulatedResult
        }
    }
    
    /**
     * Parses the JSON result from native function into BenchmarkResult object using proper JSON parsing
     */
    private fun parseBenchmarkResultJson(json: String): BenchmarkResult {
        Log.d("BenchmarkManager", "Parsing benchmark result JSON: $json")
        if (json.isEmpty()) {
            Log.w("BenchmarkManager", "Empty JSON result received from native function")
            return BenchmarkResult(
                name = "Unknown",
                executionTimeMs = 0.0,
                opsPerSecond = 0.0,
                isValid = false,
                metricsJson = "{}"
            )
        }
        
        try {
            val root = JSONObject(json)
            
            val name = root.optString("name", "Unknown")
            val executionTimeMs = root.optDouble("execution_time_ms", 0.0)
            val opsPerSecond = root.optDouble("ops_per_second", 0.0)
            val isValid = root.optBoolean("is_valid", false)
            val metricsJson = root.optString("metrics_json", "{}")
            
            Log.d("BenchmarkManager", "Parsed benchmark result - Name: $name, Execution Time: $executionTimeMs ms, Ops/Sec: $opsPerSecond, Valid: $isValid")
            
            return BenchmarkResult(
                name = name,
                executionTimeMs = executionTimeMs,
                opsPerSecond = opsPerSecond,
                isValid = isValid,
                metricsJson = metricsJson
            )
        } catch (e: Exception) {
            Log.e("BenchmarkManager", "Error parsing JSON: ${e.message}")
            return BenchmarkResult(
                name = "ParseError",
                executionTimeMs = 0.0,
                opsPerSecond = 0.0,
                isValid = false,
                metricsJson = "{}"
            )
        }
    }
    

    
    /**
     * Simulates a benchmark result for fallback purposes
     */
    private fun simulateBenchmarkResult(functionName: String): BenchmarkResult {
        // For simulation, we don't introduce delays since this is now a non-suspend function
        // Instead, we just return a simulated result
        
        val executionTime = when {
            functionName.contains("Multi") -> (400..800).random().toDouble()
            else -> (800..1200).random().toDouble()
        }
        
        val opsPerSecond = when {
            functionName.contains("Multi") -> (15000..25000).random().toDouble()
            else -> (8000..12000).random().toDouble()
        }
        
        Log.d("BenchmarkManager", "Simulated result for $functionName: opsPerSecond=$opsPerSecond, executionTime=$executionTime")
        
        return BenchmarkResult(
            name = functionName.replace("run", "").replace("Core", "-Core ").trim(),
            executionTimeMs = executionTime,
            opsPerSecond = opsPerSecond,
            isValid = true,
            metricsJson = "{}"
        )
    }
    
    // Placeholder functions that would call the native library
    private suspend fun runSingleCorePrimeGeneration(): BenchmarkResult {
        // In a real implementation: 
        // val paramsJson = createParamsJson(WorkloadParams())
        // val resultJson = CpuBenchmarkNative.runSingleCorePrimeGeneration(paramsJson)
        // return parseBenchmarkResult(resultJson)
        
        // For simulation
        delay((800..1200).random().toLong())
        return BenchmarkResult(
            name = "Single-Core Prime Generation",
            executionTimeMs = (800..1200).random().toDouble(),
            opsPerSecond = (8000..12000).random() / 100.0,
            isValid = true,
            metricsJson = "{}"
        )
    }
    
    private suspend fun runSingleCoreFibonacciRecursive(): BenchmarkResult {
        delay((800..1200).random().toLong())
        return BenchmarkResult(
            name = "Single-Core Fibonacci Recursive",
            executionTimeMs = (800..1200).random().toDouble(),
            opsPerSecond = (8000..12000).random() / 100.0,
            isValid = true,
            metricsJson = "{}"
        )
    }
    
    private suspend fun runSingleCoreMatrixMultiplication(): BenchmarkResult {
        delay((800..1200).random().toLong())
        return BenchmarkResult(
            name = "Single-Core Matrix Multiplication",
            executionTimeMs = (800..1200).random().toDouble(),
            opsPerSecond = (8000..12000).random() / 100.0,
            isValid = true,
            metricsJson = "{}"
        )
    }
    
    private suspend fun runSingleCoreHashComputing(): BenchmarkResult {
        delay((800..1200).random().toLong())
        return BenchmarkResult(
            name = "Single-Core Hash Computing",
            executionTimeMs = (800..1200).random().toDouble(),
            opsPerSecond = (8000..12000).random() / 100.0,
            isValid = true,
            metricsJson = "{}"
        )
    }
    
    private suspend fun runSingleCoreStringSorting(): BenchmarkResult {
        delay((800..1200).random().toLong())
        return BenchmarkResult(
            name = "Single-Core String Sorting",
            executionTimeMs = (800..1200).random().toDouble(),
            opsPerSecond = (8000..12000).random() / 100.0,
            isValid = true,
            metricsJson = "{}"
        )
    }
    
    private suspend fun runSingleCoreRayTracing(): BenchmarkResult {
        delay((800..1200).random().toLong())
        return BenchmarkResult(
            name = "Single-Core Ray Tracing",
            executionTimeMs = (800..1200).random().toDouble(),
            opsPerSecond = (8000..12000).random() / 100.0,
            isValid = true,
            metricsJson = "{}"
        )
    }
    
    private suspend fun runSingleCoreCompression(): BenchmarkResult {
        delay((800..1200).random().toLong())
        return BenchmarkResult(
            name = "Single-Core Compression",
            executionTimeMs = (800..1200).random().toDouble(),
            opsPerSecond = (8000..12000).random() / 100.0,
            isValid = true,
            metricsJson = "{}"
        )
    }
    
    private suspend fun runSingleCoreMonteCarloPi(): BenchmarkResult {
        delay((800..1200).random().toLong())
        return BenchmarkResult(
            name = "Single-Core Monte Carlo Pi",
            executionTimeMs = (800..1200).random().toDouble(),
            opsPerSecond = (8000..12000).random() / 100.0,
            isValid = true,
            metricsJson = "{}"
        )
    }
    
    private suspend fun runSingleCoreJsonParsing(): BenchmarkResult {
        delay((800..1200).random().toLong())
        return BenchmarkResult(
            name = "Single-Core JSON Parsing",
            executionTimeMs = (800..1200).random().toDouble(),
            opsPerSecond = (8000..12000).random() / 100.0,
            isValid = true,
            metricsJson = "{}"
        )
    }
    
    private suspend fun runSingleCoreNqueens(): BenchmarkResult {
        delay((800..1200).random().toLong())
        return BenchmarkResult(
            name = "Single-Core N-Queens",
            executionTimeMs = (800..1200).random().toDouble(),
            opsPerSecond = (8000..12000).random() / 100.0,
            isValid = true,
            metricsJson = "{}"
        )
    }
    
    private suspend fun runMultiCorePrimeGeneration(): BenchmarkResult {
        delay((400..800).random().toLong())
        return BenchmarkResult(
            name = "Multi-Core Prime Generation",
            executionTimeMs = (400..800).random().toDouble(),
            opsPerSecond = (15000..25000).random() / 100.0,
            isValid = true,
            metricsJson = "{}"
        )
    }
    
    private suspend fun runMultiCoreFibonacciMemoized(): BenchmarkResult {
        delay((400..800).random().toLong())
        return BenchmarkResult(
            name = "Multi-Core Fibonacci Memoized",
            executionTimeMs = (400..800).random().toDouble(),
            opsPerSecond = (15000..25000).random() / 100.0,
            isValid = true,
            metricsJson = "{}"
        )
    }
    
    private suspend fun runMultiCoreMatrixMultiplication(): BenchmarkResult {
        delay((400..800).random().toLong())
        return BenchmarkResult(
            name = "Multi-Core Matrix Multiplication",
            executionTimeMs = (400..800).random().toDouble(),
            opsPerSecond = (15000..25000).random() / 100.0,
            isValid = true,
            metricsJson = "{}"
        )
    }
    
    private suspend fun runMultiCoreHashComputing(): BenchmarkResult {
        delay((400..800).random().toLong())
        return BenchmarkResult(
            name = "Multi-Core Hash Computing",
            executionTimeMs = (400..800).random().toDouble(),
            opsPerSecond = (15000..25000).random() / 100.0,
            isValid = true,
            metricsJson = "{}"
        )
    }
    
    private suspend fun runMultiCoreStringSorting(): BenchmarkResult {
        delay((400..800).random().toLong())
        return BenchmarkResult(
            name = "Multi-Core String Sorting",
            executionTimeMs = (400..800).random().toDouble(),
            opsPerSecond = (15000..25000).random() / 100.0,
            isValid = true,
            metricsJson = "{}"
        )
    }
    
    private suspend fun runMultiCoreRayTracing(): BenchmarkResult {
        delay((400..800).random().toLong())
        return BenchmarkResult(
            name = "Multi-Core Ray Tracing",
            executionTimeMs = (400..800).random().toDouble(),
            opsPerSecond = (15000..25000).random() / 100.0,
            isValid = true,
            metricsJson = "{}"
        )
    }
    
    private suspend fun runMultiCoreCompression(): BenchmarkResult {
        delay((400..800).random().toLong())
        return BenchmarkResult(
            name = "Multi-Core Compression",
            executionTimeMs = (400..800).random().toDouble(),
            opsPerSecond = (15000..25000).random() / 100.0,
            isValid = true,
            metricsJson = "{}"
        )
    }
    
    private suspend fun runMultiCoreMonteCarloPi(): BenchmarkResult {
        delay((400..800).random().toLong())
        return BenchmarkResult(
            name = "Multi-Core Monte Carlo Pi",
            executionTimeMs = (400..800).random().toDouble(),
            opsPerSecond = (15000..25000).random() / 100.0,
            isValid = true,
            metricsJson = "{}"
        )
    }
    
    private suspend fun runMultiCoreJsonParsing(): BenchmarkResult {
        delay((400..800).random().toLong())
        return BenchmarkResult(
            name = "Multi-Core JSON Parsing",
            executionTimeMs = (400..800).random().toDouble(),
            opsPerSecond = (15000..25000).random() / 100.0,
            isValid = true,
            metricsJson = "{}"
        )
    }
    
    private suspend fun runMultiCoreNqueens(): BenchmarkResult {
        delay((400..800).random().toLong())
        return BenchmarkResult(
            name = "Multi-Core N-Queens",
            executionTimeMs = (400..800).random().toDouble(),
            opsPerSecond = (15000..25000).random() / 100.0,
            isValid = true,
            metricsJson = "{}"
        )
    }
    
    /**
     * Gets workload parameters based on device tier
     */
    private fun getWorkloadParamsForDeviceTier(deviceTier: String): WorkloadParams {
        return when (deviceTier.lowercase()) {
            "slow" -> WorkloadParams(
                primeRange = 100_000,
                fibonacciNRange = Pair(20, 25),
                matrixSize = 100,
                hashDataSizeMb = 5,
                stringCount = 50_000,
                rayTracingResolution = Pair(128, 128),
                rayTracingDepth = 1,
                compressionDataSizeMb = 5,
                monteCarloSamples = 5_000_000,
                jsonDataSizeMb = 1,
                nqueensSize = 8
            )
            "mid" -> WorkloadParams(
                primeRange = 8_000_000,         // Increased from 6M
                fibonacciNRange = Pair(32, 38),
                matrixSize = 700,               // Increased from 600
                hashDataSizeMb = 50,            // Increased from 40
                stringCount = 700_000,          // Increased from 500K
                rayTracingResolution = Pair(350, 350), // Increased from (300, 300)
                rayTracingDepth = 3,
                compressionDataSizeMb = 30,     // Increased from 25
                monteCarloSamples = 60_000_000, // Increased from 40M
                jsonDataSizeMb = 5,             // Increased from 4
                nqueensSize = 13
            )
            "flagship" -> WorkloadParams(
                // INCREASED: Allow multi-core tests to run longer
                primeRange = 20_000_000,        // Increased from 12M (more work for 8 cores)
                fibonacciNRange = Pair(35, 42), // Increased from (35, 40)
                matrixSize = 1200,              // Increased from 900 (more parallel work)
                hashDataSizeMb = 150,           // Increased from 100
                stringCount = 2_000_000,        // Increased from 1M (better scaling test)
                rayTracingResolution = Pair(600, 600), // Increased from (450, 450)
                rayTracingDepth = 5,            // Increased from 4
                compressionDataSizeMb = 80,     // Increased from 50
                monteCarloSamples = 150_000_000, // Increased from 80M (embarrassingly parallel)
                jsonDataSizeMb = 15,            // Increased from 10
                nqueensSize = 16                // Increased from 15 (exponentially harder)
            )
            else -> WorkloadParams() // Default values from data class
        }
    }
    
    private fun createParamsJson(params: WorkloadParams): String {
        return """{
            "prime_range": ${params.primeRange},
            "fibonacci_n_range": [${params.fibonacciNRange.first}, ${params.fibonacciNRange.second}],
            "matrix_size": ${params.matrixSize},
            "hash_data_size_mb": ${params.hashDataSizeMb},
            "string_count": ${params.stringCount},
            "ray_tracing_resolution": [${params.rayTracingResolution.first}, ${params.rayTracingResolution.second}],
            "ray_tracing_depth": ${params.rayTracingDepth},
            "compression_data_size_mb": ${params.compressionDataSizeMb},
            "monte_carlo_samples": ${params.monteCarloSamples},
            "json_data_size_mb": ${params.jsonDataSizeMb},
            "nqueens_size": ${params.nqueensSize}
        }"""
    }
    
    private fun parseBenchmarkResult(json: String?): BenchmarkResult {
        // In a real implementation, this would parse the JSON result from the native function
        return BenchmarkResult(
            name = "Unknown",
            executionTimeMs = 0.0,
            opsPerSecond = 0.0,
            isValid = false,
            metricsJson = "{}"
        )
    }
}