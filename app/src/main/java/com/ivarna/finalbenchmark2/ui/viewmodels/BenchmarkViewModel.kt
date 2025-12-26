package com.ivarna.finalbenchmark2.ui.viewmodels

// Import SystemStats from SystemModels
import android.app.ActivityManager
import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.gson.Gson
import com.ivarna.finalbenchmark2.BenchmarkForegroundService
import com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkResult
import com.ivarna.finalbenchmark2.cpuBenchmark.CpuAffinityManager
import com.ivarna.finalbenchmark2.ui.models.SystemStats
import com.ivarna.finalbenchmark2.utils.CpuUtilizationUtils
import com.ivarna.finalbenchmark2.utils.PowerUtils
import com.ivarna.finalbenchmark2.utils.TemperatureUtils
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update // Required for thread-safe updates
import kotlinx.coroutines.launch
import org.json.JSONObject

// Test state tracking - UPDATED with timeText field
data class TestState(
        val name: String,
        val status: TestStatus,
        val timeText: String = "", // ADDED: Will contain timing like "342ms"
        val durationMs: Long = 0L, // ADDED: Raw duration for calculation
        val result: com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkResult? = null,
        val accelerationMode: String? = null // ADDED: NPU/GPU/CPU
)

enum class TestStatus {
        PENDING,
        RUNNING,
        COMPLETED
}

// Updated BenchmarkUiState to hold granular state
data class BenchmarkUiState(
        val currentTestName: String = "",
        val completedTests: List<com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkResult> =
                emptyList(),
        val progress: Float = 0f,
        val isSingleCoreFinished: Boolean = false,
        val systemStats: SystemStats = SystemStats(),
        val isRunning: Boolean = false,
        val benchmarkResults: BenchmarkResults? = null,
        val error: String? = null,
        val testStates: List<TestState> =
                emptyList(), // CHANGED: from allTestStates to testStates for clarity
        val workloadPreset: String =
                "Auto", // ADDED: Track the current workload preset for UI display
        val estimatedTimeRemaining: String = "--:--",
        val elapsedTime: String = "00:00" // ADDED: Elapsed time
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
        private val historyRepository:
                com.ivarna.finalbenchmark2.data.repository.HistoryRepository? =
                null,
        private val application: Application
) : ViewModel() {
        private val _benchmarkState = MutableStateFlow<BenchmarkState>(BenchmarkState.Idle)
        val benchmarkState: StateFlow<BenchmarkState> = _benchmarkState

        // SharedFlow for navigation events (One-time events)
        private val _completionEvent = kotlinx.coroutines.flow.MutableSharedFlow<String>()
        val completionEvent: kotlinx.coroutines.flow.SharedFlow<String> = _completionEvent

        // New state flow for granular benchmark UI state
        private val _uiState = MutableStateFlow(BenchmarkUiState())
        val uiState: StateFlow<BenchmarkUiState> = _uiState

        // New state flow for warm-up status
        private val _isWarmingUp = MutableStateFlow(false)
        val isWarmingUp: StateFlow<Boolean> = _isWarmingUp

        private val benchmarkManager =
                com.ivarna.finalbenchmark2.cpuBenchmark.KotlinBenchmarkManager(
                    context = application,
                    aiManager = com.ivarna.finalbenchmark2.aiBenchmark.AiBenchmarkManager(application)
                )
        private val cpuUtils = CpuUtilizationUtils(application)
        private val powerUtils = PowerUtils(application)
        private val tempUtils = TemperatureUtils(application)

        // Performance monitor for collecting metrics during benchmark run
        private val performanceMonitor =
                com.ivarna.finalbenchmark2.utils.PerformanceMonitor(application)
        private var lastPerformanceMetricsJson: String = ""

        // Job for system monitoring to prevent multiple instances
        private var monitorJob: Job? = null

        // Guard to prevent double-execution on screen rotation
        private var isBenchmarkRunning = false
        
        // Countdown state

        private var currentCountdownSeconds = 0
        private var startTimeMillis = 0L

        init {
                // Start the system monitoring loop
                startSystemMonitoring()
        }

        private fun startSystemMonitoring() {
                // Safety: Cancel any previous job to be 100% sure
                monitorJob?.cancel()

                val activityManager = application.getSystemService(ActivityManager::class.java)

                monitorJob =
                        viewModelScope.launch(Dispatchers.IO) {
                                while (true) { // Changed from 'isActive' to 'true' for continuous
                                        // monitoring
                                        // LOG 1: Monitor wakes up
                                        val currentSizeBefore = _uiState.value.completedTests.size
                                        
                                        // Calculate memory usage percentage
                                        val memoryLoad =
                                                try {
                                                        val memoryInfo =
                                                                ActivityManager.MemoryInfo()
                                                        activityManager.getMemoryInfo(memoryInfo)
                                                        val totalMem = memoryInfo.totalMem
                                                        val availMem = memoryInfo.availMem
                                                        if (totalMem > 0) {
                                                                ((totalMem - availMem).toFloat() /
                                                                        totalMem.toFloat()) * 100f
                                                        } else {
                                                                0f
                                                        }
                                                } catch (e: Exception) {
                                                        Log.e(
                                                                "BenchmarkViewModel",
                                                                "Error getting memory info: ${e.message}"
                                                        )
                                                        0f
                                                }

                                        val stats =
                                                SystemStats(
                                                        cpuLoad =
                                                                cpuUtils.getCpuUtilizationPercentage(),
                                                        power =
                                                                powerUtils.getPowerConsumptionInfo()
                                                                        .power,
                                                        temp = tempUtils.getCpuTemperature(),
                                                        memoryLoad = memoryLoad
                                                )

                                        // CRITICAL MOMENT: Updating State
                                        _uiState.update { currentState ->
                                                
                                                // Calculate Estimated Time Remaining
                                                var timeRemainingStr = currentState.estimatedTimeRemaining
                                                if (currentState.isRunning) {
                                                    // Countdown Logic: Decrement
                                                    if (currentCountdownSeconds > 0) {
                                                        currentCountdownSeconds--
                                                    }
                                                    
                                                    // Format: MM:SS
                                                    val minutes = currentCountdownSeconds / 60
                                                    val seconds = currentCountdownSeconds % 60
                                                    timeRemainingStr = String.format(Locale.US, "%02d:%02d", minutes, seconds)
                                                    
                                                    // If explicit "Wait..." needed when 0 but still running:
                                                    if (currentCountdownSeconds == 0) {
                                                        // User requested: Keep at 00:00.
                                                        // Only show "Finalizing..." if we are unreasonably far (99%)
                                                        if (currentState.progress > 0.99f) {
                                                            timeRemainingStr = "Finalizing..."
                                                        } else {
                                                            timeRemainingStr = "00:00"
                                                        }
                                                    }
                                                    
                                                    // Elapsed Time Logic
                                                    if (startTimeMillis > 0) {
                                                        val elapsedSecs = ((System.currentTimeMillis() - startTimeMillis) / 1000)
                                                        val eMin = elapsedSecs / 60
                                                        val eSec = elapsedSecs % 60
                                                        val elapsedStr = String.format(Locale.US, "%02d:%02d", eMin, eSec)
                                                        
                                                        currentState.copy(
                                                            systemStats = stats,
                                                            estimatedTimeRemaining = timeRemainingStr,
                                                            elapsedTime = elapsedStr
                                                        )
                                                    } else {
                                                         currentState.copy(
                                                            systemStats = stats,
                                                            estimatedTimeRemaining = timeRemainingStr
                                                        )
                                                    }
                                                } else if (currentState.progress == 1f) {
                                                    // Finished
                                                    timeRemainingStr = "00:00"
                                                    currentState.copy(
                                                        systemStats = stats,
                                                        estimatedTimeRemaining = timeRemainingStr
                                                    )
                                                } else {
                                                    // Idle
                                                    currentState.copy(
                                                        systemStats = stats,
                                                        estimatedTimeRemaining = timeRemainingStr
                                                    )
                                                }
                                        }

                                        delay(1000)
                                }
                        }
        }

        // Job for benchmark execution
        private var benchmarkJob: Job? = null

        // NEW IMPLEMENTATION: Delegate to KotlinBenchmarkManager for Single Source of Truth
        fun runBenchmarks(preset: String = "Auto", category: com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkCategory = com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkCategory.CPU) {
                // FIX: Reset state immediately to prevent stale navigation
                _benchmarkState.value = BenchmarkState.Idle

                // Prevent restarting if already running or finished
                if (isBenchmarkRunning) return
                isBenchmarkRunning = true

                viewModelScope.launch {
                        try {
                                // Start foreground service to maintain high priority during
                                // benchmarks
                                BenchmarkForegroundService.start(application)

                                // Log CPU topology using the new CpuAffinityManager
                                CpuAffinityManager.logTopology()
                                val bigCores = CpuAffinityManager.getBigCores()
                                val littleCores = CpuAffinityManager.getLittleCores()

                                Log.i(
                                        "BenchmarkViewModel",
                                        "Detected CPU topology: ${bigCores.size} big cores (${bigCores}), ${littleCores.size} little cores (${littleCores})"
                                )

                                // With the pure Kotlin implementation, we rely on Android's thread
                                // priority system
                                // instead of native CPU affinity control
                                Log.i(
                                        "BenchmarkViewModel",
                                        "Using Android thread priority for performance optimization"
                                )

                                // Start performance monitoring for metrics collection
                                performanceMonitor.start()

                                // Initialize Countdown
                                currentCountdownSeconds = if (category == com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkCategory.AI) {
                                    100
                                } else {
                                    when (preset.lowercase()) {
                                        "flagship" -> 200
                                        "mid" -> 120
                                        "slow" -> 60
                                        else -> 120
                                    }
                                }
                                startTimeMillis = System.currentTimeMillis()

                                // Initial Warm-up Phase
                                _isWarmingUp.value = true
                                _uiState.value = _uiState.value.copy(currentTestName = "WARMING UP...")
                                delay(2000) // Simulate warm-up
                                // Do NOT turn off warm-up here. Wait for first test to start.

                                // 1. RESET STATE - Initialize test states
                                // category is already passed in
                                
                                val names = com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkName.getByCategory(category)
                                val testNames = if (category == com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkCategory.CPU) {
                                     // Group all Single-Core first, then all Multi-Core
                                     names.map { it.singleCore() } + names.map { it.multiCore() }
                                } else {
                                     names.map { it.displayName() }
                                }

                                _uiState.update {
                                        it.copy(
                                                isRunning = true,
                                                progress = 0f,
                                                currentTestName = "Initializing...",
                                                testStates =
                                                        testNames.map { name ->
                                                                TestState(
                                                                        name = name,
                                                                        status = TestStatus.PENDING
                                                                )
                                                        },
                                                error = null,
                                                workloadPreset =
                                                        preset // FIXED: Store the actual preset
                                                // parameter
                                                )
                                }

                                // 2. RUN BENCHMARKS: Start listening for completion BEFORE running
                                // benchmarks
                                val summaryDeferred =
                                        async(Dispatchers.IO) {
                                                benchmarkManager.benchmarkComplete.first()
                                        }

                                val eventJob = launch {
                                        // Listen to benchmark events for UI updates
                                        try {
                                                benchmarkManager.benchmarkEvents.collect { event ->
                                                        // Handle TEST mode separately (warm-up workload)
                                                        if (event.mode == "TEST") {
                                                                when (event.state) {
                                                                        "STARTED" -> {
                                                                                _uiState.update { state ->
                                                                                        state.copy(
                                                                                                currentTestName = "Warming up device..."
                                                                                        )
                                                                                }
                                                                        }
                                                                        "COMPLETED" -> {
                                                                                _uiState.update { state ->
                                                                                        state.copy(
                                                                                                currentTestName = "Warm-up complete, starting benchmarks..."
                                                                                        )
                                                                                }
                                                                        }
                                                                }
                                                                return@collect // Skip normal processing for TEST events
                                                        }

                                                        // Normal benchmark event handling (SINGLE and MULTI modes)
                                                        when (event.state) {
                                                                "STARTED" -> {
                                                                        if (_isWarmingUp.value) {
                                                                            _isWarmingUp.value = false
                                                                        }
                                                                        
                                                                        // RECALCULATE TIMER if needed
                                                                        val remainingTests = _uiState.value.testStates.count { it.status == TestStatus.PENDING }
                                                                        // Simple heuristic: If 00:00 (or close), add time based on remaining count
                                                                        if (currentCountdownSeconds < 5 && remainingTests > 0) {
                                                                             val timePerTest = when (preset.lowercase()) {
                                                                                 "flagship" -> 10 // 10s per test (20 tests = 200s total was estimate)
                                                                                 "mid" -> 6
                                                                                 "slow" -> 3
                                                                                 else -> 6
                                                                             }
                                                                             currentCountdownSeconds += (remainingTests * timePerTest)
                                                                             Log.d("BenchmarkViewModel", "Timer extended by ${remainingTests * timePerTest}s")
                                                                        }

                                                                        _uiState.update { state ->
                                                                                state.copy(
                                                                                        currentTestName =
                                                                                                event.testName,
                                                                                        testStates =
                                                                                                state.testStates
                                                                                                        .map {
                                                                                                                if (it.name ==
                                                                                                                                event.testName
                                                                                                                )
                                                                                                                        it.copy(
                                                                                                                                status =
                                                                                                                                        TestStatus
                                                                                                                                                .RUNNING,
                                                                                                                                accelerationMode = event.accelerationMode // Capture mode early
                                                                                                                        )
                                                                                                                else
                                                                                                                        it
                                                                                                        }
                                                                                )
                                                                        }
                                                                }
                                                                "COMPLETED" -> {
                                                                        val currentProgress =
                                                                                _uiState.value
                                                                                        .progress +
                                                                                        (1f /
                                                                                                testNames
                                                                                                        .size
                                                                                                        .toFloat())
                                                                        _uiState.update { state ->
                                                                                val updatedTestStates =
                                                                                        state.testStates
                                                                                                .map {
                                                                                                        if (it.name ==
                                                                                                                        event.testName
                                                                                                        )
                                                                                                                it.copy(
                                                                                                                        status =
                                                                                                                                TestStatus
                                                                                                                                        .COMPLETED,
                                                                                                                        timeText =
                                                                                                                                String.format(
                                                                                                                                        Locale.US,
                                                                                                                                        "%.2f s",
                                                                                                                                        event.timeMs /
                                                                                                                                                1000.0
                                                                                                                                ),
                                                                                                                        durationMs = event.timeMs, // Capture duration
                                                                                                                        accelerationMode = event.accelerationMode // Capture mode
                                                                                                                )
                                                                                                        else
                                                                                                                it
                                                                                                }
                                                                                val completedCount =
                                                                                        updatedTestStates
                                                                                                .count {
                                                                                                        it.status ==
                                                                                                                TestStatus
                                                                                                                        .COMPLETED
                                                                                                }

                                                                                // Update legacy
                                                                                // state for
                                                                                // compatibility
                                                                                _benchmarkState
                                                                                        .value =
                                                                                        BenchmarkState
                                                                                                .Running(
                                                                                                        BenchmarkProgress(
                                                                                                                currentBenchmark =
                                                                                                                        event.testName,
                                                                                                                progress =
                                                                                                                        ((completedCount
                                                                                                                                        .toFloat() /
                                                                                                                                        testNames
                                                                                                                                                .size
                                                                                                                                                .toFloat()) *
                                                                                                                                        100)
                                                                                                                                .toInt(),
                                                                                                                completedBenchmarks =
                                                                                                                        completedCount,
                                                                                                                totalBenchmarks =
                                                                                                                        testNames
                                                                                                                                .size
                                                                                                        )
                                                                                                )

                                                                                state.copy(
                                                                                        progress =
                                                                                                currentProgress,
                                                                                        currentTestName =
                                                                                                event.testName,
                                                                                        testStates =
                                                                                                updatedTestStates
                                                                                )
                                                                        }
                                                                }
                                                        }
                                                }
                                        } catch (e: Exception) {
                                                Log.e(
                                                        "BenchmarkViewModel",
                                                        "Error in event collection: ${e.message}",
                                                        e
                                                )
                                        }
                                }

                                // Start the benchmark execution
                                benchmarkJob =
                                        launch(Dispatchers.IO) {
                                                benchmarkManager.runBenchmarks(preset, category)
                                        }

                                // Wait for the completion result (the event from benchmark will be
                                // caught by summaryDeferred)
                                val summaryJson = summaryDeferred.await()
                                Log.d(
                                        "BenchmarkViewModel",
                                        "Received summary JSON from manager: $summaryJson"
                                )

                                // Parse results from the collected summary
                                val finalResults =
                                        try {
                                                parseResultsFromManagerJson(summaryJson)
                                        } catch (e: Exception) {
                                                Log.e(
                                                        "BenchmarkViewModel",
                                                        "Error parsing results from JSON: ${e.message}",
                                                        e
                                                )
                                                throw e
                                        } finally {
                                                eventJob.cancel() // Cancel event collection
                                                Log.d(
                                                        "BenchmarkViewModel",
                                                        "Event collection cancelled"
                                                )
                                        }

                                // 3. UPDATE FINAL STATE
                                _uiState.update {
                                        it.copy(isRunning = false, benchmarkResults = finalResults)
                                }

                                _benchmarkState.value = BenchmarkState.Completed(finalResults)

                                // Log completion to help debug navigation issues
                                Log.d(
                                        "BenchmarkViewModel",
                                        "Benchmark completed! State set to Completed with results: ${finalResults.finalWeightedScore}"
                                )

                                // Stop performance monitoring and capture metrics BEFORE creating
                                // summary JSON
                                lastPerformanceMetricsJson = performanceMonitor.stop()
                                Log.d(
                                        "BenchmarkViewModel",
                                        "Captured performance metrics: ${lastPerformanceMetricsJson.take(100)}..."
                                )

                                // DIRECT CALLBACK: Emit event regardless of callback presence
                                // if (onBenchmarkCompleteCallback != null) { // REMOVED
                                        try {
                                                // Create summary JSON similar to what
                                                // BenchmarkScreen expects
                                                val gson = com.google.gson.Gson()
                                                
                                                fun sanitize(value: Double): Double =
                                                        when {
                                                                value.isInfinite() -> 0.0
                                                                value.isNaN() -> 0.0
                                                                else -> value
                                                        }

                                                // Parse performance metrics JSON into a Map for clean
                                                // Gson serialization
                                                val performanceMetricsMap: Map<String, Any> =
                                                        try {
                                                                if (lastPerformanceMetricsJson
                                                                                .isNotBlank() &&
                                                                                lastPerformanceMetricsJson !=
                                                                                        "{}"
                                                                ) {
                                                                        val type =
                                                                                object :
                                                                                        com.google.gson
                                                                                        .reflect
                                                                                        .TypeToken<
                                                                                                Map<
                                                                                                        String,
                                                                                                        Any
                                                                                                >
                                                                                        >() {}
                                                                                        .type
                                                                        gson.fromJson(
                                                                                lastPerformanceMetricsJson,
                                                                                type
                                                                        )
                                                                } else {
                                                                        emptyMap()
                                                                }
                                                        } catch (e: Exception) {
                                                                Log.e(
                                                                        "BenchmarkViewModel",
                                                                        "Error parsing performance metrics JSON",
                                                                        e
                                                                )
                                                                emptyMap()
                                                        }

                                                val summaryData =
                                                        mapOf(
                                                                "type" to category.name,
                                                                "single_core_score" to
                                                                        sanitize(
                                                                                finalResults
                                                                                        .singleCoreScore
                                                                        ),
                                                                "multi_core_score" to
                                                                        sanitize(
                                                                                finalResults
                                                                                        .multiCoreScore
                                                                        ),
                                                                "final_score" to
                                                                        sanitize(
                                                                                finalResults
                                                                                        .finalWeightedScore
                                                                        ),
                                                                "normalized_score" to
                                                                        sanitize(
                                                                                finalResults
                                                                                        .normalizedScore
                                                                        ),
                                                                "rating" to
                                                                        "Good", // Simple rating for
                                                                // now
                                                                "timestamp" to
                                                                        System.currentTimeMillis(), // Capture completion time
                                                                "performance_metrics" to
                                                                        performanceMetricsMap, // FIXED: Use Map for correct JSON structure
                                                                "detailed_results" to
                                                                        finalResults.detailedResults
                                                                                .map { result ->
                                                                                        mapOf(
                                                                                                "name" to
                                                                                                        result.name,
                                                                                                "executionTimeMs" to
                                                                                                        sanitize(
                                                                                                                result.executionTimeMs
                                                                                                        ),
                                                                                                "opsPerSecond" to
                                                                                                        sanitize(
                                                                                                                result.opsPerSecond
                                                                                                        ),
                                                                                                "isValid" to
                                                                                                        result.isValid,
                                                                                                "metricsJson" to
                                                                                                        result.metricsJson
                                                                                        )
                                                                                }
                                                        )

                                                val summaryJson = gson.toJson(summaryData)

                                                Log.d(
                                                        "BenchmarkViewModel",
                                                        "Timestamp in summaryData: ${summaryData["timestamp"]}"
                                                )
                                                Log.d(
                                                        "BenchmarkViewModel",
                                                        "Emitting completion event with JSON: ${summaryJson.take(300)}..."
                                                )
                                                
                                                // Emit event for UI to handle navigation
                                                // Using SharedFlow ensures the UI (even if recreated) can receive it if it's listening
                                                _completionEvent.emit(summaryJson)
                                                
                                                Log.d(
                                                        "BenchmarkViewModel",
                                                        "Completion event emitted successfully"
                                                )
                                        } catch (e: Exception) {
                                                Log.e(
                                                        "BenchmarkViewModel",
                                                        "Error emitting completion event: ${e.message}",
                                                        e
                                                )
                                        }

                                // Save to database with performance metrics
                                if (historyRepository != null) {
                                    if (category == com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkCategory.CPU) {
                                        saveCpuBenchmarkResult(
                                            finalResults,
                                            lastPerformanceMetricsJson
                                        )
                                    } else {
                                        saveGenericBenchmarkResult(
                                           finalResults,
                                           category.name,
                                           lastPerformanceMetricsJson
                                        )
                                    }
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
                                _benchmarkState.value =
                                        BenchmarkState.Error(e.message ?: "Unknown error occurred")

                                BenchmarkForegroundService.stop(application)
                        } finally {
                                isBenchmarkRunning = false
                        }
                }
        }

        // Parse results from KotlinBenchmarkManager JSON - TRUST THE MANAGER'S SCORES
        private suspend fun parseResultsFromManager(): BenchmarkResults {
                Log.d(
                        "BenchmarkViewModel",
                        "parseResultsFromManager: Listening for manager completion..."
                )

                return try {
                        // Listen to the manager's completion flow for the summary JSON
                        val summaryJson = benchmarkManager.benchmarkComplete.first()
                        Log.d(
                                "BenchmarkViewModel",
                                "Received summary JSON from manager: $summaryJson"
                        )

                        // Parse the JSON using org.json
                        val jsonObject = org.json.JSONObject(summaryJson)
                        val singleCoreScore = jsonObject.getDouble("single_core_score")
                        val multiCoreScore = jsonObject.getDouble("multi_core_score")
                        val finalScore = jsonObject.getDouble("final_score")
                        val normalizedScore = jsonObject.getDouble("normalized_score")

                        // Parse detailed results from the JSON
                        val detailedResultsArray = jsonObject.getJSONArray("detailed_results")
                        val detailedResults =
                                mutableListOf<
                                        com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkResult>()

                        for (i in 0 until detailedResultsArray.length()) {
                                val resultObject = detailedResultsArray.getJSONObject(i)
                                val result =
                                        com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkResult(
                                                name = resultObject.getString("name"),
                                                executionTimeMs =
                                                        resultObject.getDouble("executionTimeMs"),
                                                opsPerSecond =
                                                        resultObject.getDouble("opsPerSecond"),
                                                isValid = resultObject.getBoolean("isValid"),
                                                metricsJson = resultObject.getString("metricsJson")
                                        )
                                detailedResults.add(result)
                        }

                        // Calculate core ratio
                        val coreRatio =
                                if (singleCoreScore > 0) multiCoreScore / singleCoreScore else 0.0

                        Log.d(
                                "BenchmarkViewModel",
                                "PARSED SCORES: single=$singleCoreScore, multi=$multiCoreScore, weighted=$finalScore, normalized=$normalizedScore"
                        )

                        // Return results - TRUST THE MANAGER'S CALCULATIONS
                        BenchmarkResults(
                                individualScores = detailedResults,
                                singleCoreScore = singleCoreScore,
                                multiCoreScore = multiCoreScore,
                                coreRatio = coreRatio,
                                finalWeightedScore = finalScore,
                                normalizedScore = normalizedScore,
                                detailedResults = detailedResults
                        )
                } catch (e: Exception) {
                        Log.e(
                                "BenchmarkViewModel",
                                "Error parsing results from manager: ${e.message}",
                                e
                        )
                        // Fallback: Use the completed tests list with zero scores if JSON parsing
                        // fails
                        val completedResults = _uiState.value.completedTests
                        BenchmarkResults(
                                individualScores = completedResults,
                                singleCoreScore = 0.0,
                                multiCoreScore = 0.0,
                                coreRatio = 0.0,
                                finalWeightedScore = 0.0,
                                normalizedScore = 0.0,
                                detailedResults = completedResults
                        )
                }
        }

        // Parse results from provided JSON string - TRUST THE MANAGER'S SCORES
        private fun parseResultsFromManagerJson(summaryJson: String): BenchmarkResults {
                Log.d(
                        "BenchmarkViewModel",
                        "parseResultsFromManagerJson: Parsing provided JSON: $summaryJson"
                )

                return try {
                        // Parse the JSON using org.json
                        val jsonObject = JSONObject(summaryJson)
                        val singleCoreScore = jsonObject.getDouble("single_core_score")
                        val multiCoreScore = jsonObject.getDouble("multi_core_score")
                        val finalScore = jsonObject.getDouble("final_score")
                        val normalizedScore = jsonObject.getDouble("normalized_score")

                        // Parse detailed results from the JSON
                        val detailedResultsArray = jsonObject.getJSONArray("detailed_results")
                        Log.d("BenchmarkViewModel", "parseResultsFromManagerJson: Parsing ${detailedResultsArray.length()} detailed results")
                        val detailedResults =
                                mutableListOf<
                                        com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkResult>()

                        for (i in 0 until detailedResultsArray.length()) {
                                val resultObject = detailedResultsArray.getJSONObject(i)
                                val result =
                                        com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkResult(
                                                name = resultObject.getString("name"),
                                                executionTimeMs =
                                                        resultObject.getDouble("executionTimeMs"),
                                                opsPerSecond =
                                                        resultObject.getDouble("opsPerSecond"),
                                                isValid = resultObject.getBoolean("isValid"),
                                                metricsJson = resultObject.getString("metricsJson")
                                        )
                                detailedResults.add(result)
                        }

                        // Calculate core ratio
                        val coreRatio =
                                if (singleCoreScore > 0) multiCoreScore / singleCoreScore else 0.0

                        Log.d(
                                "BenchmarkViewModel",
                                "PARSED SCORES: single=$singleCoreScore, multi=$multiCoreScore, weighted=$finalScore, normalized=$normalizedScore"
                        )

                        // Return results - TRUST THE MANAGER'S CALCULATIONS
                        BenchmarkResults(
                                individualScores = detailedResults,
                                singleCoreScore = singleCoreScore,
                                multiCoreScore = multiCoreScore,
                                coreRatio = coreRatio,
                                finalWeightedScore = finalScore,
                                normalizedScore = normalizedScore,
                                detailedResults = detailedResults
                        )
                } catch (e: Exception) {
                        Log.e(
                                "BenchmarkViewModel",
                                "Error parsing results from manager JSON: ${e.message}",
                                e
                        )
                        // Fallback: Return results with zero scores if JSON parsing
                        // fails
                        BenchmarkResults(
                                individualScores = emptyList(),
                                singleCoreScore = 0.0,
                                multiCoreScore = 0.0,
                                coreRatio = 0.0,
                                finalWeightedScore = 0.0,
                                normalizedScore = 0.0,
                                detailedResults = emptyList()
                        )
                }
        }

        // Legacy function kept for compatibility
        fun startBenchmark(preset: String = "Auto", category: com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkCategory = com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkCategory.CPU) {
                runBenchmarks(preset, category)
        }

        fun stopBenchmark() {
                // Cancel the actual execution job
                benchmarkJob?.cancel()
                
                // Stop the foreground service
                BenchmarkForegroundService.stop(application)
                
                // Reset state
                isBenchmarkRunning = false
                _uiState.update { 
                    it.copy(isRunning = false) 
                }
                _benchmarkState.value = BenchmarkState.Idle
        }

        fun saveCpuBenchmarkResult(results: BenchmarkResults, performanceMetricsJson: String = "") {
                if (historyRepository == null) return

                viewModelScope.launch(Dispatchers.IO) {
                        try {
                                val detailedJson = Gson().toJson(results.individualScores)
                                val masterEntity =
                                        com.ivarna.finalbenchmark2.data.database.entities
                                                .BenchmarkResultEntity(
                                                        timestamp = System.currentTimeMillis(),
                                                        type = "CPU",
                                                        deviceModel = android.os.Build.MODEL,
                                                        totalScore = results.finalWeightedScore,
                                                        singleCoreScore = results.singleCoreScore,
                                                        multiCoreScore = results.multiCoreScore,
                                                        normalizedScore = results.normalizedScore,
                                                        detailedResultsJson = detailedJson,
                                                        performanceMetricsJson = performanceMetricsJson
                                                )

                                fun extractScore(testName: String): Double {
                                        return results.individualScores
                                                .filter { it.name.contains(testName, ignoreCase = true) }
                                                .sumOf { it.opsPerSecond }
                                }

                                val detailEntity =
                                        com.ivarna.finalbenchmark2.data.database.entities
                                                .CpuTestDetailEntity(
                                                        resultId = 0,
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

                                historyRepository.saveCpuBenchmark(masterEntity, detailEntity)
                        } catch (e: Exception) {
                                Log.e("BenchmarkViewModel", "Error saving CPU result: ${e.message}", e)
                        }
                }
        }

        fun saveGenericBenchmarkResult(
            results: BenchmarkResults, 
            type: String, 
            performanceMetricsJson: String = ""
        ) {
            if (historyRepository == null) return

            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val detailedJson = Gson().toJson(results.individualScores)
                    val masterEntity =
                        com.ivarna.finalbenchmark2.data.database.entities
                            .BenchmarkResultEntity(
                                timestamp = System.currentTimeMillis(),
                                type = type,
                                deviceModel = android.os.Build.MODEL,
                                totalScore = results.finalWeightedScore,
                                singleCoreScore = results.singleCoreScore, // Might be 0 for AI
                                multiCoreScore = results.multiCoreScore,   // Might be 0 for AI
                                normalizedScore = results.normalizedScore,
                                detailedResultsJson = detailedJson,
                                performanceMetricsJson = performanceMetricsJson
                            )

                    val genericDetails = results.individualScores.map { result ->
                        com.ivarna.finalbenchmark2.data.database.entities.GenericTestDetailEntity(
                            resultId = 0,
                            testName = result.name,
                            score = result.opsPerSecond, // Using opsPerSecond as canonical score
                            metricsJson = result.metricsJson
                        )
                    }

                    historyRepository.saveGenericBenchmark(masterEntity, genericDetails)
                     Log.d("BenchmarkViewModel", "Saved generic benchmark result for type: $type")
                } catch (e: Exception) {
                    Log.e("BenchmarkViewModel", "Error saving generic result: ${e.message}", e)
                }
            }
        }

        companion object {
                val Factory: ViewModelProvider.Factory = viewModelFactory {
                        initializer {
                                // This retrieves the "Application" from the Android System
                                val application =
                                        (this[
                                                ViewModelProvider.AndroidViewModelFactory
                                                        .APPLICATION_KEY] as
                                                Application)

                                // Creates the ViewModel ensuring only ONE exists
                                BenchmarkViewModel(application = application)
                        }
                }
        }
}
