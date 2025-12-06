package com.ivarna.finalbenchmark2.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkEvent
import com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkManager
import com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import android.util.Log
import com.google.gson.Gson
import android.app.Application
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update // Required for thread-safe updates

// Import SystemStats from SystemModels
import com.ivarna.finalbenchmark2.ui.models.SystemStats
import com.ivarna.finalbenchmark2.utils.CpuUtilizationUtils
import com.ivarna.finalbenchmark2.utils.PowerUtils
import com.ivarna.finalbenchmark2.utils.TemperatureUtils

// Test state tracking
data class TestState(
    val name: String,
    val status: TestStatus,
    val result: com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkResult? = null
)

enum class TestStatus {
    PENDING,
    RUNNING,
    COMPLETED
}

// Updated BenchmarkUiState to hold granular state
data class BenchmarkUiState(
    val currentTestName: String = "",
    val completedTests: List<com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkResult> = emptyList(),
    val progress: Float = 0f,
    val isSingleCoreFinished: Boolean = false,
    val systemStats: SystemStats = SystemStats(),
    val isRunning: Boolean = false,
    val benchmarkResults: BenchmarkResults? = null,
    val error: String? = null,
    val allTestStates: List<TestState> = emptyList()
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
    private val historyRepository: com.ivarna.finalbenchmark2.data.repository.HistoryRepository? = null,
    application: Application
) : ViewModel() {
    private val _benchmarkState = MutableStateFlow<BenchmarkState>(BenchmarkState.Idle)
    val benchmarkState: StateFlow<BenchmarkState> = _benchmarkState
    
    // New state flow for granular benchmark UI state
    private val _uiState = MutableStateFlow(BenchmarkUiState())
    val uiState: StateFlow<BenchmarkUiState> = _uiState
    
    private val benchmarkManager = BenchmarkManager()
    private val cpuUtils = CpuUtilizationUtils(application)
    private val powerUtils = PowerUtils(application)
    private val tempUtils = TemperatureUtils(application)
    
    // Guard to prevent double-execution on screen rotation
    private var isBenchmarkRunning = false

    init {
        // Start the system monitoring loop
        startSystemMonitoring()
    }

    private fun startSystemMonitoring() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) { // Changed from 'isActive' to 'true' for continuous monitoring
                val stats = SystemStats(
                    cpuLoad = cpuUtils.getCpuUtilizationPercentage(),
                    power = powerUtils.getPowerConsumptionInfo().power,
                    temp = tempUtils.getCpuTemperature()
                )
                
                // FIX: Use .update for thread safety
                // This ensures we modify the *current* state, not an old copy
                _uiState.update { currentState ->
                    currentState.copy(systemStats = stats)
                }
                
                delay(10)
            }
        }
    }
    
    fun startBenchmark(preset: String = "Auto") {
        // Prevent restarting if already running or finished
        if (isBenchmarkRunning) return
        isBenchmarkRunning = true
        
        viewModelScope.launch {
            try {
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
                
                // Initialize all test states
                val initialTestStates = benchmarks.map { (name, _) ->
                    TestState(
                        name = name,
                        status = TestStatus.PENDING,
                        result = null
                    )
                }
                
                Log.d("BenchmarkViewModel", "Initializing benchmark with ${initialTestStates.size} tests: ${initialTestStates.map { it.name }}")
                
                // Initialize the new UI state
                _uiState.update { currentState ->
                    BenchmarkUiState(
                        currentTestName = "Initializing...",
                        completedTests = emptyList(),
                        progress = 0f,
                        isSingleCoreFinished = false,
                        isRunning = true,
                        error = null,
                        allTestStates = initialTestStates
                    )
                }
                
                Log.d("BenchmarkViewModel", "Initial UI state set with allTestStates: ${_uiState.value.allTestStates.map { "${it.name}(${it.status})" }}")
                
                _benchmarkState.value = BenchmarkState.Running(
                    BenchmarkProgress(
                        currentBenchmark = "Initializing...",
                        progress = 0,
                        completedBenchmarks = 0,
                        totalBenchmarks = 20 // 10 single-core + 10 multi-core
                    )
                )
                
                val results = mutableListOf<com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkResult>()
                val totalBenchmarks = benchmarks.size
                var singleCoreCompleted = 0
                val singleCoreTotal = 10  // First 10 benchmarks are single-core
                
                // Run benchmarks sequentially with progress updates
                for ((index, benchmarkPair) in benchmarks.withIndex()) {
                    val (name, functionName) = benchmarkPair
                    
                    // Update state to RUNNING
                    Log.d("BenchmarkViewModel", "Setting test $index ($name) to RUNNING status")
                    _uiState.update { currentState ->
                        currentState.copy(
                            currentTestName = name,
                            allTestStates = currentState.allTestStates.mapIndexed { i, state ->
                                when {
                                    i < index && state.status == TestStatus.COMPLETED -> {
                                        Log.d("BenchmarkViewModel", "Test $i remains COMPLETED: ${state.name}")
                                        state // Keep completed tests as completed
                                    }
                                    i == index -> {
                                        Log.d("BenchmarkViewModel", "Test $i now RUNNING: ${state.name}")
                                        state.copy(status = TestStatus.RUNNING) // Set current test to running
                                    }
                                    else -> {
                                        Log.d("BenchmarkViewModel", "Test $i remains PENDING: ${state.name}")
                                        state // Keep pending tests as pending
                                    }
                                }
                            }
                        )
                    }
                    Log.d("BenchmarkViewModel", "Current allTestStates after setting RUNNING: ${_uiState.value.allTestStates.map { "${it.name}(${it.status})" }}")
                    
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
                    
                    // Update completed tests in UI state - accumulate properly
                    // Update UI state with completed test
                    Log.d("BenchmarkViewModel", "Setting test $index ($name) to COMPLETED status with result: ${result.executionTimeMs}ms")
                    _uiState.update { currentState ->
                        val updatedCompletedTests = currentState.completedTests + result
                        val isSingleCoreFinished = if (index >= singleCoreTotal - 1) {
                            true
                        } else {
                            currentState.isSingleCoreFinished
                        }
                        
                        // Update single core completed counter
                        if (!name.contains("Multi")) {
                            singleCoreCompleted++
                        }
                        
                        currentState.copy(
                            completedTests = updatedCompletedTests,
                            progress = (index + 1).toFloat() / totalBenchmarks.toFloat(),
                            isSingleCoreFinished = isSingleCoreFinished,
                            allTestStates = currentState.allTestStates.mapIndexed { i, state ->
                                when {
                                    i < index && state.status == TestStatus.COMPLETED -> {
                                        Log.d("BenchmarkViewModel", "Test $i remains COMPLETED: ${state.name}")
                                        state // Keep already completed tests as completed
                                    }
                                    i == index -> {
                                        Log.d("BenchmarkViewModel", "Test $i now COMPLETED: ${state.name}, time: ${result.executionTimeMs}ms")
                                        state.copy(status = TestStatus.COMPLETED, result = result) // Set current test to completed
                                    }
                                    else -> {
                                        Log.d("BenchmarkViewModel", "Test $i remains PENDING: ${state.name}")
                                        state // Keep pending tests as pending
                                    }
                                }
                            }
                        )
                    }
                    Log.d("BenchmarkViewModel", "Current allTestStates after setting COMPLETED: ${_uiState.value.allTestStates.map { "${it.name}(${it.status})" }}")
                    
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
                            progress = ((index + 1) * 100 / totalBenchmarks),
                            completedBenchmarks = index + 1,
                            totalBenchmarks = totalBenchmarks
                        )
                    )
                    
                    // UI Breathing Room: Give Compose 50ms to render the "Checkmark"
                    // before the next heavy native test spikes the CPU.
                    delay(50)
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
                _uiState.update { currentState ->
                    currentState.copy(
                        benchmarkResults = benchmarkResults,
                        isRunning = false
                    )
                }
                
                _benchmarkState.value = BenchmarkState.Completed(benchmarkResults)
                
                // Reset the running flag on completion
                isBenchmarkRunning = false
                
                // Save the benchmark results to the database
                if (historyRepository != null) {
                    saveCpuBenchmarkResult(benchmarkResults)
                }
                
            } catch (e: Exception) {
                Log.e("BenchmarkViewModel", "Error during benchmark execution", e)
                _uiState.update { currentState ->
                    currentState.copy(
                        error = e.message ?: "Unknown error occurred",
                        isRunning = false
                    )
                }
                _benchmarkState.value = BenchmarkState.Error(e.message ?: "Unknown error occurred")
                
                // Reset the running flag on error
                isBenchmarkRunning = false
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
            "Prime Generation" to 0.00001,
            "Fibonacci" to 0.012,
            "Matrix Multiplication" to 0.025,
            "Hash Computing" to 0.01,
            "String Sorting" to 0.015,
            "Ray Tracing" to 0.0006,
            "Compression" to 0.07,
            "Monte Carlo" to 0.07,
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
            val scalingFactor = scalingFactors[benchmarkName] ?: 0.0001
            totalWeightedScore += result.opsPerSecond * scalingFactor
        }
        
        return totalWeightedScore
    }
}