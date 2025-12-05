package com.ivarna.finalbenchmark2.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkEvent
import com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkManager
import com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// Import SystemStats from SystemModels
import com.ivarna.finalbenchmark2.ui.models.SystemStats

// Updated BenchmarkUiState to hold granular state
data class BenchmarkUiState(
    val currentTestName: String = "",
    val completedTests: List<com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkResult> = emptyList(),
    val progress: Float = 0f,
    val isSingleCoreFinished: Boolean = false,
    val systemStats: SystemStats = SystemStats(),
    val isRunning: Boolean = false,
    val benchmarkResults: BenchmarkResults? = null,
    val error: String? = null
)

// Old data class kept for compatibility
data class BenchmarkProgress(
    val currentBenchmark: String = "",
    val progress: Int = 0,
    val completedBenchmarks: Int = 0,
    val totalBenchmarks: Int = 0
)

data class BenchmarkResults(
    val individualScores: List<BenchmarkResult>,
    val singleCoreScore: Double,
    val multiCoreScore: Double,
    val coreRatio: Double,
    val finalWeightedScore: Double,
    val normalizedScore: Double,
    val detailedResults: List<BenchmarkResult> = emptyList() // Added for detailed view
)

// Keep the old BenchmarkState for compatibility if needed
sealed class BenchmarkState {
    object Idle : BenchmarkState()
    data class Running(val progress: BenchmarkProgress) : BenchmarkState()
    data class Completed(val results: BenchmarkResults) : BenchmarkState()
    data class Error(val message: String) : BenchmarkState()
}

class BenchmarkViewModel(
    private val historyRepository: com.ivarna.finalbenchmark2.data.repository.HistoryRepository? = null
) : ViewModel() {
    private val _benchmarkState = MutableStateFlow<BenchmarkState>(BenchmarkState.Idle)
    val benchmarkState: StateFlow<BenchmarkState> = _benchmarkState
    
    // New state flow for granular benchmark UI state
    private val _uiState = MutableStateFlow(BenchmarkUiState())
    val uiState: StateFlow<BenchmarkUiState> = _uiState
    
    private val benchmarkManager = BenchmarkManager()
    
    fun startBenchmark(preset: String = "Auto") {
        if (_benchmarkState.value is BenchmarkState.Running) return
        
        viewModelScope.launch {
            try {
                // Initialize the new UI state
                _uiState.value = BenchmarkUiState(
                    currentTestName = "Initializing...",
                    completedTests = emptyList(),
                    progress = 0f,
                    isSingleCoreFinished = false,
                    isRunning = true,
                    error = null
                )
                
                _benchmarkState.value = BenchmarkState.Running(
                    BenchmarkProgress(
                        currentBenchmark = "Initializing...",
                        progress = 0,
                        completedBenchmarks = 0,
                        totalBenchmarks = 20 // 10 single-core + 10 multi-core
                    )
                )
                
                // Define all benchmark functions with names
                val benchmarks = listOf(
                    "Single-Core Prime Generation" to "runSingleCorePrimeGeneration",
                    "Single-Core Fibonacci Recursive" to "runSingleCoreFibonacciRecursive",
                    "Single-Core Matrix Multiplication" to "runSingleCoreMatrixMultiplication",
                    "Single-Core Hash Computing" to "runSingleCoreHashComputing",
                    "Single-Core String Sorting" to "runSingleCoreStringSorting",
                    "Single-Core Ray Tracing" to "runSingleCoreRayTracing",
                    "Single-Core Compression" to "runSingleCoreCompression",
                    "Single-Core Monte Carlo Pi" to "runSingleCoreMonteCarloPi",
                    "Single-Core JSON Parsing" to "runSingleCoreJsonParsing",
                    "Single-Core N-Queens" to "runSingleCoreNqueens",
                    "Multi-Core Prime Generation" to "runMultiCorePrimeGeneration",
                    "Multi-Core Fibonacci Memoized" to "runMultiCoreFibonacciMemoized",
                    "Multi-Core Matrix Multiplication" to "runMultiCoreMatrixMultiplication",
                    "Multi-Core Hash Computing" to "runMultiCoreHashComputing",
                    "Multi-Core String Sorting" to "runMultiCoreStringSorting",
                    "Multi-Core Ray Tracing" to "runMultiCoreRayTracing",
                    "Multi-Core Compression" to "runMultiCoreCompression",
                    "Multi-Core Monte Carlo Pi" to "runMultiCoreMonteCarloPi",
                    "Multi-Core JSON Parsing" to "runMultiCoreJsonParsing",
                    "Multi-Core N-Queens" to "runMultiCoreNqueens"
                )
                
                val results = mutableListOf<com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkResult>()
                val totalBenchmarks = benchmarks.size
                var singleCoreCompleted = 0
                val singleCoreTotal = 10  // First 10 benchmarks are single-core
                
                // Run benchmarks sequentially with progress updates
                for ((index, benchmarkPair) in benchmarks.withIndex()) {
                    val (name, functionName) = benchmarkPair
                    
                    // Update UI state with current test
                    _uiState.value = _uiState.value.copy(
                        currentTestName = name
                    )
                    
                    // Emit STARTED event
                    benchmarkManager.emitBenchmarkStart(
                        testName = name,
                        mode = if (name.contains("Multi")) "MULTI" else "SINGLE"
                    )
                    
                    // Run the benchmark in the default dispatcher (background thread)
                    val result = withContext(Dispatchers.Default) {
                        try {
                            benchmarkManager.runNativeBenchmarkFunction(functionName, preset)
                        } catch (e: Exception) {
                            Log.e("BenchmarkViewModel", "Error running benchmark $name: ${e.message}", e)
                            // Return a default result in case of error
                            com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkResult(
                                name = name,
                                executionTimeMs = 0.0,
                                opsPerSecond = 0.0,
                                isValid = false,
                                metricsJson = "{}"
                            )
                        }
                    }
                    
                    results.add(result)
                    
                    // Update completed tests in UI state
                    val updatedCompletedTests = _uiState.value.completedTests + result
                    val isSingleCoreFinished = if (index >= singleCoreTotal - 1) {
                        true
                    } else {
                        _uiState.value.isSingleCoreFinished
                    }
                    
                    // Update single core completed counter
                    if (!name.contains("Multi")) {
                        singleCoreCompleted++
                    }
                    
                    // Update UI state with completed test
                    _uiState.value = _uiState.value.copy(
                        completedTests = updatedCompletedTests,
                        progress = (index + 1).toFloat() / totalBenchmarks.toFloat(),
                        isSingleCoreFinished = isSingleCoreFinished
                    )
                    
                    // Emit COMPLETED event
                    benchmarkManager.emitBenchmarkComplete(
                        testName = name,
                        mode = if (name.contains("Multi")) "MULTI" else "SINGLE",
                        timeMs = result.executionTimeMs.toLong(),
                        score = result.opsPerSecond
                    )
                    
                    // Update the old benchmark state for compatibility
                    _benchmarkState.value = BenchmarkState.Running(
                        BenchmarkProgress(
                            currentBenchmark = name,
                            progress = ((index + 1) * 10 / totalBenchmarks),
                            completedBenchmarks = index + 1,
                            totalBenchmarks = totalBenchmarks
                        )
                    )
                }
                
                // Use the BenchmarkManager's weighted scoring logic instead of averaging
                // Construct the JSON result format that BenchmarkManager expects
                val singleCoreResultsJson = results.filter { !it.name.contains("Multi") }.map { result ->
                    """{"name":"${result.name}","ops_per_second":${result.opsPerSecond}}"""
                }.joinToString(prefix = "[", postfix = "]")
                
                val multiCoreResultsJson = results.filter { it.name.contains("Multi") }.map { result ->
                    """{"name":"${result.name}","ops_per_second":${result.opsPerSecond}}"""
                }.joinToString(prefix = "[", postfix = "]")
                
                val combinedResultsJson = """{
                    "single_core_results": $singleCoreResultsJson,
                    "multi_core_results": $multiCoreResultsJson
                }"""
                
                // Use the BenchmarkManager's correct weighted scoring method
                val summaryJson = benchmarkManager.calculateSummaryFromResults(combinedResultsJson)
                
                // Parse the summary JSON to extract scores
                var singleCoreScore = 0.0
                var multiCoreScore = 0.0
                var finalWeightedScore = 0.0
                var normalizedScore = 0.0
                var coreRatio = 0.0
                
                try {
                    val gson = Gson()
                    val summaryMap = gson.fromJson(summaryJson, Map::class.java)
                    singleCoreScore = (summaryMap["single_core_score"] as Double?) ?: 0.0
                    multiCoreScore = (summaryMap["multi_core_score"] as Double?) ?: 0.0
                    finalWeightedScore = (summaryMap["final_score"] as Double?) ?: 0.0
                    normalizedScore = (summaryMap["normalized_score"] as Double?) ?: 0.0
                    
                    coreRatio = if (singleCoreScore > 0) {
                        multiCoreScore / singleCoreScore
                    } else {
                        0.0
                    }
                    
                    Log.d("BenchmarkViewModel", "Using weighted scoring - Single: $singleCoreScore, Multi: $multiCoreScore")
                } catch (e: Exception) {
                    Log.e("BenchmarkViewModel", "Error parsing summary JSON: ${e.message}", e)
                    // Keep default values (already set to 0.0 above)
                }
                
                Log.d("BenchmarkViewModel", "Individual Results: $results")
                Log.d("BenchmarkViewModel", "Single-Core Score: $singleCoreScore")
                Log.d("BenchmarkViewModel", "Multi-Core Score: $multiCoreScore")
                Log.d("BenchmarkViewModel", "Core Ratio: $coreRatio")
                Log.d("BenchmarkViewModel", "Final Weighted Score: $finalWeightedScore")
                Log.d("BenchmarkViewModel", "Normalized Score: $normalizedScore")
                
                val benchmarkResults = BenchmarkResults(
                    individualScores = results,
                    singleCoreScore = singleCoreScore,
                    multiCoreScore = multiCoreScore,
                    coreRatio = coreRatio,
                    finalWeightedScore = finalWeightedScore,
                    normalizedScore = normalizedScore,
                    detailedResults = results
                )
                
                // Update UI state with final results
                _uiState.value = _uiState.value.copy(
                    benchmarkResults = benchmarkResults,
                    isRunning = false
                )
                
                _benchmarkState.value = BenchmarkState.Completed(benchmarkResults)
                
                // Save the benchmark results to the database
                if (historyRepository != null) {
                    saveCpuBenchmarkResult(benchmarkResults)
                }
                
            } catch (e: Exception) {
                Log.e("BenchmarkViewModel", "Error during benchmark execution", e)
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Unknown error occurred",
                    isRunning = false
                )
                _benchmarkState.value = BenchmarkState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }
    
    fun saveCpuBenchmarkResult(results: BenchmarkResults) {
        if (historyRepository == null) {
            Log.w("BenchmarkViewModel", "HistoryRepository is null, cannot save results")
            return
        }
        
        viewModelScope.launch {
            try {
                // Serialize detailed results to JSON string
                val gson = Gson()
                val detailedResultsJson = gson.toJson(results.detailedResults)
                
                // Create the main benchmark result entity
                val benchmarkResultEntity = com.ivarna.finalbenchmark2.data.database.entities.BenchmarkResultEntity(
                    type = "CPU",  // Set the type as CPU
                    totalScore = results.finalWeightedScore,
                    timestamp = System.currentTimeMillis(),
                    deviceModel = android.os.Build.MODEL,
                    singleCoreScore = results.singleCoreScore,
                    multiCoreScore = results.multiCoreScore,
                    normalizedScore = results.normalizedScore, // Add normalized score
                    detailedResultsJson = detailedResultsJson // Add detailed results as JSON string
                )
                
                // Extract detailed scores for each benchmark type
                val singleCoreResults = results.detailedResults.filter { !it.name.contains("Multi") }
                val multiCoreResults = results.detailedResults.filter { it.name.contains("Multi") }
                
                // Calculate weighted scores for each benchmark type using BenchmarkManager logic
                val primeNumberScore = calculateWeightedScore(results.detailedResults, "Prime Generation")
                val fibonacciScore = calculateWeightedScore(results.detailedResults, "Fibonacci")
                val matrixMultiplicationScore = calculateWeightedScore(results.detailedResults, "Matrix Multiplication")
                val hashComputingScore = calculateWeightedScore(results.detailedResults, "Hash Computing")
                val stringSortingScore = calculateWeightedScore(results.detailedResults, "String Sorting")
                val rayTracingScore = calculateWeightedScore(results.detailedResults, "Ray Tracing")
                val compressionScore = calculateWeightedScore(results.detailedResults, "Compression")
                val monteCarloScore = calculateWeightedScore(results.detailedResults, "Monte Carlo")
                val jsonParsingScore = calculateWeightedScore(results.detailedResults, "JSON Parsing")
                val nQueensScore = calculateWeightedScore(results.detailedResults, "N-Queens")
                
                // Create the CPU test detail entity with actual scores
                val cpuTestDetailEntity = com.ivarna.finalbenchmark2.data.database.entities.CpuTestDetailEntity(
                    resultId = 0, // Will be set by the repository function
                    primeNumberScore = primeNumberScore,
                    fibonacciScore = fibonacciScore,
                    matrixMultiplicationScore = matrixMultiplicationScore,
                    hashComputingScore = hashComputingScore,
                    stringSortingScore = stringSortingScore,
                    rayTracingScore = rayTracingScore,
                    compressionScore = compressionScore,
                    monteCarloScore = monteCarloScore,
                    jsonParsingScore = jsonParsingScore,
                    nQueensScore = nQueensScore
                )
                
                // Save the benchmark result and details to the database
                historyRepository.saveCpuBenchmark(benchmarkResultEntity, cpuTestDetailEntity)
                
                Log.d("BenchmarkViewModel", "Successfully saved CPU benchmark result to database")
            } catch (e: Exception) {
                Log.e("BenchmarkViewModel", "Error saving benchmark result to database: ${e.message}", e)
            }
        }
    }
    
    private fun calculateWeightedScore(results: List<com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkResult>, benchmarkName: String): Double {
        // Use the same scaling factors as BenchmarkManager
        val scalingFactors = mapOf(
            "Prime Generation" to 0.000001,
            "Fibonacci" to 0.012,
            "Matrix Multiplication" to 0.0000025,
            "Hash Computing" to 0.000001,
            "String Sorting" to 0.000015,
            "Ray Tracing" to 0.00006,
            "Compression" to 0.000007,
            "Monte Carlo" to 0.00007,
            "JSON Parsing" to 0.00004,
            "N-Queens" to 0.007
        )
        
        val filteredResults = results.filter { it.name.contains(benchmarkName) }
        if (filteredResults.isEmpty()) {
            return 0.0
        }
        
        // Calculate weighted score
        var totalWeightedScore = 0.0
        for (result in filteredResults) {
            val scalingFactor = scalingFactors[benchmarkName] ?: 0.00001
            totalWeightedScore += result.opsPerSecond * scalingFactor
        }
        
        return totalWeightedScore
    }
}