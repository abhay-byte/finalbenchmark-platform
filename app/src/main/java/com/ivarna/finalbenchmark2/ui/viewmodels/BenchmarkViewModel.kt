package com.ivarna.finalbenchmark2.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewModelScope
import com.ivarna.finalbenchmark2.BenchmarkForegroundService
import com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkEvent
import com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkManager  // Keeping for compatibility
import com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkResult
import com.ivarna.finalbenchmark2.cpuBenchmark.CpuAffinityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withTimeout
import android.util.Log
import com.google.gson.Gson
import android.app.Application
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update // Required for thread-safe updates
import kotlinx.coroutines.Job
import kotlinx.coroutines.yield
import android.app.ActivityManager
import kotlin.system.measureNanoTime

// Import SystemStats from SystemModels
import com.ivarna.finalbenchmark2.ui.models.SystemStats
import com.ivarna.finalbenchmark2.utils.CpuUtilizationUtils
import com.ivarna.finalbenchmark2.utils.PowerUtils
import com.ivarna.finalbenchmark2.utils.TemperatureUtils

// Test state tracking - UPDATED with timeText field
data class TestState(
    val name: String,
    val status: TestStatus,
    val timeText: String = "", // ADDED: Will contain timing like "342ms"
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
    val testStates: List<TestState> = emptyList(), // CHANGED: from allTestStates to testStates for clarity
    val workloadPreset: String = "Auto" // ADDED: Track the current workload preset for UI display
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
    private val application: Application
) : ViewModel() {
    private val _benchmarkState = MutableStateFlow<BenchmarkState>(BenchmarkState.Idle)
    val benchmarkState: StateFlow<BenchmarkState> = _benchmarkState
    
    // New state flow for granular benchmark UI state
    private val _uiState = MutableStateFlow(BenchmarkUiState())
    val uiState: StateFlow<BenchmarkUiState> = _uiState
    
    private val benchmarkManager = com.ivarna.finalbenchmark2.cpuBenchmark.KotlinBenchmarkManager()
    private val cpuUtils = CpuUtilizationUtils(application)
    private val powerUtils = PowerUtils(application)
    private val tempUtils = TemperatureUtils(application)
    
    // Job for system monitoring to prevent multiple instances
    private var monitorJob: Job? = null
    
    // Guard to prevent double-execution on screen rotation
    private var isBenchmarkRunning = false

    init {
        // Start the system monitoring loop
        startSystemMonitoring()
    }

    private fun startSystemMonitoring() {
        // Safety: Cancel any previous job to be 100% sure
        monitorJob?.cancel()
        
        val activityManager = application.getSystemService(ActivityManager::class.java)
        
        monitorJob = viewModelScope.launch(Dispatchers.IO) {
            while (true) { // Changed from 'isActive' to 'true' for continuous monitoring
                // LOG 1: Monitor wakes up
                val currentSizeBefore = _uiState.value.completedTests.size
                Log.d("BENCH_DEBUG", "[Monitor] Waking up. Current List Size: $currentSizeBefore")

                // Calculate memory usage percentage
                val memoryLoad = try {
                    val memoryInfo = ActivityManager.MemoryInfo()
                    activityManager.getMemoryInfo(memoryInfo)
                    val totalMem = memoryInfo.totalMem
                    val availMem = memoryInfo.availMem
                    if (totalMem > 0) {
                        ((totalMem - availMem).toFloat() / totalMem.toFloat()) * 100f
                    } else {
                        0f
                    }
                } catch (e: Exception) {
                    Log.e("BenchmarkViewModel", "Error getting memory info: ${e.message}")
                    0f
                }

                val stats = SystemStats(
                    cpuLoad = cpuUtils.getCpuUtilizationPercentage(),
                    power = powerUtils.getPowerConsumptionInfo().power,
                    temp = tempUtils.getCpuTemperature(),
                    memoryLoad = memoryLoad
                )
                
                // CRITICAL MOMENT: Updating State
                _uiState.update { currentState ->
                    // LOG 2: Inside the atomic update block for Monitor
                    if (currentState.completedTests.size != currentSizeBefore) {
                        Log.d("BENCH_DEBUG", "[Monitor] !!! RACE CONDITION DETECTED !!! I saw size $currentSizeBefore, but inside update it is ${currentState.completedTests.size}")
                    }
                    currentState.copy(systemStats = stats)
                }
                
                delay(1000)
            }
        }
    }
    
    // NEW IMPLEMENTATION: Simplified, reactive benchmark runner
    fun runBenchmarks(preset: String = "Auto") {
        // FIX: Reset state immediately to prevent stale navigation
        _benchmarkState.value = BenchmarkState.Idle
        
        // Prevent restarting if already running or finished
        if (isBenchmarkRunning) return
        isBenchmarkRunning = true
        
        viewModelScope.launch {
            try {
                // Start foreground service to maintain high priority during benchmarks
                BenchmarkForegroundService.start(application)
                
                // Log CPU topology using the new CpuAffinityManager
                CpuAffinityManager.logTopology()
                val bigCores = CpuAffinityManager.getBigCores()
                val littleCores = CpuAffinityManager.getLittleCores()
                
                Log.i("BenchmarkViewModel", "Detected CPU topology: ${bigCores.size} big cores (${bigCores}), ${littleCores.size} little cores (${littleCores})")
                
                // With the pure Kotlin implementation, we rely on Android's thread priority system
                // instead of native CPU affinity control
                Log.i("BenchmarkViewModel", "Using Android thread priority for performance optimization")
                
                // 1. RESET STATE - Initialize test states
                val testNames = listOf(
                    "Single-Core Prime Generation",
                    "Single-Core Fibonacci Recursive", 
                    "Single-Core Matrix Multiplication",
                    "Single-Core Hash Computing",
                    "Single-Core String Sorting",
                    "Single-Core Ray Tracing",
                    "Single-Core Compression",
                    "Single-Core Monte Carlo π",
                    "Single-Core JSON Parsing", 
                    "Single-Core N-Queens",
                    "Multi-Core Prime Generation",
                    "Multi-Core Fibonacci Memoized",
                    "Multi-Core Matrix Multiplication", 
                    "Multi-Core Hash Computing",
                    "Multi-Core String Sorting",
                    "Multi-Core Ray Tracing",
                    "Multi-Core Compression",
                    "Multi-Core Monte Carlo π",
                    "Multi-Core JSON Parsing",
                    "Multi-Core N-Queens"
                )
                
                _uiState.update { it.copy(
                    isRunning = true,
                    progress = 0f,
                    currentTestName = "Initializing...",
                    testStates = testNames.map { name -> TestState(name = name, status = TestStatus.PENDING) },
                    error = null,
                    workloadPreset = preset // FIXED: Store the actual preset parameter
                ) }
                
                val totalTests = testNames.size
                var completedTests = 0

                // Execute each benchmark sequentially with UI breathing room
                for ((index, testName) in testNames.withIndex()) {
                    // A. UPDATE UI: Mark this specific test as RUNNING
                    _uiState.update { state ->
                        state.copy(
                            currentTestName = testName,
                            testStates = state.testStates.map { 
                                if (it.name == testName) it.copy(status = TestStatus.RUNNING) else it 
                            }
                        )
                    }
                    
                    // Give UI time to render the "Spinner" - CRITICAL for UI updates
                    delay(50)

                    // C. RUN the benchmark and get the result (with crash protection)
                    val benchmarkResult = safeBenchmarkRun(testName) {
                        withContext(Dispatchers.Default) {
                            runSingleBenchmark(testName)
                        }
                    }
                    
                    // D. UPDATE UI: Mark as COMPLETED with TIME and RESULT
                    completedTests++
                    val newProgress = completedTests.toFloat() / totalTests.toFloat()
                    
                    _uiState.update { state ->
                        state.copy(
                            progress = newProgress,
                            currentTestName = testName,
                            completedTests = state.completedTests + benchmarkResult, // Store actual result
                            testStates = state.testStates.map { 
                                if (it.name == testName) it.copy(
                                    status = TestStatus.COMPLETED, 
                                    timeText = "${benchmarkResult.executionTimeMs.toInt()}ms", // Use actual execution time from result
                                    result = benchmarkResult // Store the actual BenchmarkResult
                                ) else it 
                            }
                        )
                    }
                    
                    // Update legacy state for compatibility
                    _benchmarkState.value = BenchmarkState.Running(
                        BenchmarkProgress(
                            currentBenchmark = testName,
                            progress = (newProgress * 100).toInt(),
                            completedBenchmarks = completedTests,
                            totalBenchmarks = totalTests
                        )
                    )
                    
                    // YIELD CONTROL: Allow UI thread to process state updates
                    yield() // This gives the main thread a chance to recompose
                }
                
                // D. FINALIZE: Calculate final results
                val benchmarkResults = calculateFinalResults()
                
                _uiState.update { it.copy(
                    isRunning = false,
                    benchmarkResults = benchmarkResults
                ) }
                
                _benchmarkState.value = BenchmarkState.Completed(benchmarkResults)
                
                // Save to database
                if (historyRepository != null) {
                    saveCpuBenchmarkResult(benchmarkResults)
                }
                
                // Stop foreground service
                BenchmarkForegroundService.stop(application)
                
            } catch (e: Exception) {
                Log.e("BenchmarkViewModel", "Error during benchmark execution", e)
                _uiState.update { currentState ->
                    currentState.copy(
                        error = e.message ?: "Unknown error occurred",
                        isRunning = false
                    )
                }
                _benchmarkState.value = BenchmarkState.Error(e.message ?: "Unknown error occurred")
                
                BenchmarkForegroundService.stop(application)
            } finally {
                isBenchmarkRunning = false
            }
        }
    }
    
    private suspend fun safeBenchmarkRun(testName: String, block: suspend () -> com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkResult): com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkResult {
        return try {
            val result = block()
            Log.d("BenchmarkViewModel", "✓ $testName completed successfully: ${result.opsPerSecond} ops/sec")
            result
        } catch (e: Exception) {
            Log.e("BenchmarkViewModel", "✗ $testName failed with exception: ${e.message}", e)
            // Return a dummy result so the benchmark suite can continue
            com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkResult(
                name = testName,
                executionTimeMs = 0.0,
                opsPerSecond = 0.0,
                isValid = false,
                metricsJson = "{\"error\": \"${e.message}\"}"
            )
        }
    }
    
    // Helper function to run individual benchmark based on name
    private suspend fun runSingleBenchmark(testName: String): com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkResult {
        val deviceTier = _uiState.value.workloadPreset // FIXED: Use the actual preset from UI state instead of hardcoded "Flagship"
        val params = getWorkloadParams(deviceTier)
        
        return when {
            testName.contains("Single-Core Prime Generation") -> {
                com.ivarna.finalbenchmark2.cpuBenchmark.algorithms.SingleCoreBenchmarks.primeGeneration(params)
            }
            testName.contains("Single-Core Fibonacci Recursive") -> {
                com.ivarna.finalbenchmark2.cpuBenchmark.algorithms.SingleCoreBenchmarks.fibonacciRecursive(params)
            }
            testName.contains("Single-Core Matrix Multiplication") -> {
                com.ivarna.finalbenchmark2.cpuBenchmark.algorithms.SingleCoreBenchmarks.matrixMultiplication(params)
            }
            testName.contains("Single-Core Hash Computing") -> {
                com.ivarna.finalbenchmark2.cpuBenchmark.algorithms.SingleCoreBenchmarks.hashComputing(params)
            }
            testName.contains("Single-Core String Sorting") -> {
                com.ivarna.finalbenchmark2.cpuBenchmark.algorithms.SingleCoreBenchmarks.stringSorting(params)
            }
            testName.contains("Single-Core Ray Tracing") -> {
                com.ivarna.finalbenchmark2.cpuBenchmark.algorithms.SingleCoreBenchmarks.rayTracing(params)
            }
            testName.contains("Single-Core Compression") -> {
                com.ivarna.finalbenchmark2.cpuBenchmark.algorithms.SingleCoreBenchmarks.compression(params)
            }
            testName.contains("Single-Core Monte Carlo") -> {
                com.ivarna.finalbenchmark2.cpuBenchmark.algorithms.SingleCoreBenchmarks.monteCarloPi(params)
            }
            testName.contains("Single-Core JSON Parsing") -> {
                com.ivarna.finalbenchmark2.cpuBenchmark.algorithms.SingleCoreBenchmarks.jsonParsing(params)
            }
            testName.contains("Single-Core N-Queens") -> {
                com.ivarna.finalbenchmark2.cpuBenchmark.algorithms.SingleCoreBenchmarks.nqueens(params)
            }
            testName.contains("Multi-Core Prime Generation") -> {
                com.ivarna.finalbenchmark2.cpuBenchmark.algorithms.MultiCoreBenchmarks.primeGeneration(params)
            }
            testName.contains("Multi-Core Fibonacci Recursive") -> {
                com.ivarna.finalbenchmark2.cpuBenchmark.algorithms.MultiCoreBenchmarks.fibonacciRecursive(params)
            }
            testName.contains("Multi-Core Matrix Multiplication") -> {
                com.ivarna.finalbenchmark2.cpuBenchmark.algorithms.MultiCoreBenchmarks.matrixMultiplication(params)
            }
            testName.contains("Multi-Core Hash Computing") -> {
                com.ivarna.finalbenchmark2.cpuBenchmark.algorithms.MultiCoreBenchmarks.hashComputing(params)
            }
            testName.contains("Multi-Core String Sorting") -> {
                com.ivarna.finalbenchmark2.cpuBenchmark.algorithms.MultiCoreBenchmarks.stringSorting(params)
            }
            testName.contains("Multi-Core Ray Tracing") -> {
                com.ivarna.finalbenchmark2.cpuBenchmark.algorithms.MultiCoreBenchmarks.rayTracing(params)
            }
            testName.contains("Multi-Core Compression") -> {
                com.ivarna.finalbenchmark2.cpuBenchmark.algorithms.MultiCoreBenchmarks.compression(params)
            }
            testName.contains("Multi-Core Monte Carlo") -> {
                com.ivarna.finalbenchmark2.cpuBenchmark.algorithms.MultiCoreBenchmarks.monteCarloPi(params)
            }
            testName.contains("Multi-Core JSON Parsing") -> {
                com.ivarna.finalbenchmark2.cpuBenchmark.algorithms.MultiCoreBenchmarks.jsonParsing(params)
            }
            testName.contains("Multi-Core N-Queens") -> {
                com.ivarna.finalbenchmark2.cpuBenchmark.algorithms.MultiCoreBenchmarks.nqueens(params)
            }
            else -> {
                throw IllegalArgumentException("Unknown benchmark: $testName")
            }
        }
    }
    
    // Helper function to get workload parameters
    private fun getWorkloadParams(deviceTier: String): com.ivarna.finalbenchmark2.cpuBenchmark.WorkloadParams {
        return when (deviceTier.lowercase()) {
            "slow" -> com.ivarna.finalbenchmark2.cpuBenchmark.WorkloadParams(
                primeRange = 10_000,
                fibonacciNRange = Pair(20, 25),
                matrixSize = 100,
                hashDataSizeMb = 5,
                stringCount = 50_000,
                rayTracingResolution = Pair(128, 128),
                rayTracingDepth = 1,
                compressionDataSizeMb = 5,
                monteCarloSamples = 5_000,
                jsonDataSizeMb = 1,
                nqueensSize = 8
            )
            "mid" -> com.ivarna.finalbenchmark2.cpuBenchmark.WorkloadParams(
                primeRange = 8_000_000,
                fibonacciNRange = Pair(32, 38),
                matrixSize = 700,
                hashDataSizeMb = 50,
                stringCount = 700_000,
                rayTracingResolution = Pair(350, 350),
                rayTracingDepth = 3,
                compressionDataSizeMb = 30,
                monteCarloSamples = 60_000_000,
                jsonDataSizeMb = 5,
                nqueensSize = 13
            )
            "flagship" -> com.ivarna.finalbenchmark2.cpuBenchmark.WorkloadParams(
                primeRange = 20_000_000,
                fibonacciNRange = Pair(35, 42),
                matrixSize = 1200,
                hashDataSizeMb = 150,
                stringCount = 2_000_000,
                rayTracingResolution = Pair(600, 600),
                rayTracingDepth = 5,
                compressionDataSizeMb = 80,
                monteCarloSamples = 150_000_000,
                jsonDataSizeMb = 15,
                nqueensSize = 14
            )
            else -> com.ivarna.finalbenchmark2.cpuBenchmark.WorkloadParams() // Default values
        }
    }
    
    // Calculate final results from completed tests
    private fun calculateFinalResults(): BenchmarkResults {
        // FIXED: Use the completedTests list that now contains actual BenchmarkResult objects
        val completedResults = _uiState.value.completedTests
        
        Log.d("BenchmarkViewModel", "calculateFinalResults: completedResults size = ${completedResults.size}")
        completedResults.forEach { result ->
            Log.d("BenchmarkViewModel", "  - ${result.name}: opsPerSecond=${result.opsPerSecond}")
        }
            
        // Calculate scores using the existing scoring logic
        val singleCoreResults = completedResults.filter { it.name.contains("Single-Core", ignoreCase = true) }
        val multiCoreResults = completedResults.filter { it.name.contains("Multi-Core", ignoreCase = true) }
        
        Log.d("BenchmarkViewModel", "Single-Core results: ${singleCoreResults.size}, Multi-Core results: ${multiCoreResults.size}")
        
        val singleCoreScore = calculateWeightedScore(singleCoreResults, "SINGLE")
        val multiCoreScore = calculateWeightedScore(multiCoreResults, "MULTI")
        val finalWeightedScore = (singleCoreScore * 0.35) + (multiCoreScore * 0.65)
        val normalizedScore = finalWeightedScore / 2500.0 // Apply normalization factor as mentioned in the requirements
        val coreRatio = if (singleCoreScore > 0) multiCoreScore / singleCoreScore else 0.0
        
        Log.d("BenchmarkViewModel", "FINAL SCORES: single=$singleCoreScore, multi=$multiCoreScore, weighted=$finalWeightedScore, normalized=$normalizedScore")
        
        return BenchmarkResults(
            individualScores = completedResults,
            singleCoreScore = singleCoreScore,
            multiCoreScore = multiCoreScore,
            coreRatio = coreRatio,
            finalWeightedScore = finalWeightedScore,
            normalizedScore = normalizedScore,
            detailedResults = completedResults
        )
    }
    
    // Legacy function kept for compatibility
    fun startBenchmark(preset: String = "Auto") {
        runBenchmarks(preset)
    }
    
    fun saveCpuBenchmarkResult(results: BenchmarkResults) {
        if (historyRepository == null) {
            Log.w("BenchmarkViewModel", "HistoryRepository is null, cannot save results")
            return
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Prepare the JSON for the "Individual Test Results" list
                val detailedJson = Gson().toJson(results.individualScores)

                // 2. Create the Parent Entity
                val masterEntity = com.ivarna.finalbenchmark2.data.database.entities.BenchmarkResultEntity(
                    timestamp = System.currentTimeMillis(),
                    type = "CPU",
                    deviceModel = android.os.Build.MODEL,
                    totalScore = results.finalWeightedScore, // Ensure this is not 0
                    singleCoreScore = results.singleCoreScore,
                    multiCoreScore = results.multiCoreScore,
                    normalizedScore = results.normalizedScore,
                    detailedResultsJson = detailedJson
                )

                // 3. Helper to extract scores safely from the new List<BenchmarkResult>
                fun extractScore(testName: String): Double {
                    // Sum Single + Multi ops for the specific test category
                    return results.individualScores
                        .filter { it.name.contains(testName, ignoreCase = true) }
                        .sumOf { it.opsPerSecond }
                }

                // 4. Create the Detail Entity (Mapping specific tests to DB columns)
                val detailEntity = com.ivarna.finalbenchmark2.data.database.entities.CpuTestDetailEntity(
                    resultId = 0, // Room handles this
                    primeNumberScore = extractScore("Prime"),
                    fibonacciScore = extractScore("Fibonacci"),
                    matrixMultiplicationScore = extractScore("Matrix"),
                    hashComputingScore = extractScore("Hash"),
                    stringSortingScore = extractScore("String"),
                    rayTracingScore = extractScore("Ray"),
                    compressionScore = extractScore("Compression"),
                    monteCarloScore = extractScore("Monte Carlo"),
                    jsonParsingScore = extractScore("JSON"),
                    nQueensScore = extractScore("Queens")
                )

                // 5. Commit to Repository
                historyRepository.saveCpuBenchmark(masterEntity, detailEntity)
                
                Log.d("BenchmarkViewModel", "Successfully saved CPU benchmark result to database")
                Log.d("BenchmarkViewModel", "Saved scores - Prime: ${detailEntity.primeNumberScore}, Fibonacci: ${detailEntity.fibonacciScore}, Matrix: ${detailEntity.matrixMultiplicationScore}")
            } catch (e: Exception) {
                Log.e("BenchmarkViewModel", "Error saving benchmark result to database: ${e.message}", e)
            }
        }
    }
    
    private fun calculateWeightedScore(results: List<com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkResult>, benchmarkType: String): Double {
        // Scaling factors for each test type (keyed by partial test name)
        val scalingFactors = mapOf(
            "Prime" to 0.00001,
            "Fibonacci" to 0.012,
            "Matrix" to 0.025,
            "Hash" to 0.01,
            "String" to 0.015,
            "Ray" to 0.006,
            "Compression" to 0.07,
            "Monte Carlo" to 0.07,
            "JSON" to 0.00004,
            "Queens" to 0.07
        )
        
        // FIX Bug A: Use case-insensitive filtering with correct prefix
        val typePrefix = if (benchmarkType == "SINGLE") "Single-Core" else "Multi-Core"
        val filteredResults = results.filter { it.name.contains(typePrefix, ignoreCase = true) }
        
        if (filteredResults.isEmpty()) {
            Log.w("BenchmarkViewModel", "No results found for type: $benchmarkType (prefix: $typePrefix)")
            return 0.0
        }
        
        // Calculate weighted score
        var totalWeightedScore = 0.0
        for (result in filteredResults) {
            // FIX Bug B: Find scaling factor by matching test name, not benchmarkType
            val scalingFactor = scalingFactors.entries
                .find { (key, _) -> result.name.contains(key, ignoreCase = true) }
                ?.value ?: 0.0001
            
            // Sanitize opsPerSecond to prevent Infinity/NaN propagation
            val sanitizedOps = when {
                result.opsPerSecond.isInfinite() -> 0.0
                result.opsPerSecond.isNaN() -> 0.0
                else -> result.opsPerSecond
            }
            
            totalWeightedScore += sanitizedOps * scalingFactor
            Log.d("BenchmarkViewModel", "Score calc: ${result.name} -> ops=$sanitizedOps, factor=$scalingFactor, contribution=${sanitizedOps * scalingFactor}")
        }
        
        Log.d("BenchmarkViewModel", "Total $benchmarkType score: $totalWeightedScore")
        return totalWeightedScore
    }
    
    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                // This retrieves the "Application" from the Android System
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application)
                
                // Creates the ViewModel ensuring only ONE exists
                BenchmarkViewModel(application = application)
            }
        }
    }
}