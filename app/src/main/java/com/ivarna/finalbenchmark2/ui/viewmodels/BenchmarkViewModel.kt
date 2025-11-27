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

data class BenchmarkProgress(
    val currentBenchmark: String = "",
    val progress: Int = 0,
    val completedBenchmarks: Int = 0,
    val totalBenchmarks: Int = 0
)

sealed class BenchmarkState {
    object Idle : BenchmarkState()
    data class Running(val progress: BenchmarkProgress) : BenchmarkState()
    data class Completed(val results: String) : BenchmarkState()
    data class Error(val message: String) : BenchmarkState()
}

class BenchmarkViewModel : ViewModel() {
    private val _benchmarkState = MutableStateFlow<BenchmarkState>(BenchmarkState.Idle)
    val benchmarkState: StateFlow<BenchmarkState> = _benchmarkState
    
    private val benchmarkManager = BenchmarkManager()
    
    fun startBenchmark() {
        if (_benchmarkState.value is BenchmarkState.Running) return
        
        viewModelScope.launch {
            try {
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
                
                val results = mutableListOf<BenchmarkResult>()
                val totalBenchmarks = benchmarks.size
                
                // Run benchmarks sequentially with progress updates
                for ((index, benchmarkPair) in benchmarks.withIndex()) {
                    val (name, functionName) = benchmarkPair
                    _benchmarkState.value = BenchmarkState.Running(
                        BenchmarkProgress(
                            currentBenchmark = name,
                            progress = ((index + 1) * 100 / totalBenchmarks),
                            completedBenchmarks = index + 1,
                            totalBenchmarks = totalBenchmarks
                        )
                    )
                    
                    // Run the benchmark in the default dispatcher (background thread)
                    val result = withContext(Dispatchers.Default) {
                        try {
                            benchmarkManager.runNativeBenchmarkFunction(functionName)
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
                    
                    // Emit benchmark event for UI updates
                    benchmarkManager._benchmarkEvents.emit(
                        BenchmarkEvent(
                            testName = name,
                            mode = if (name.contains("Multi")) "MULTI" else "SINGLE",
                            state = "COMPLETED",
                            timeMs = result.executionTimeMs.toLong(),
                            score = result.opsPerSecond
                        )
                    )
                }
                
                // Calculate summary from the actual results
                val singleCoreResults = results.filter { !it.name.contains("Multi") }
                    .map { result ->
                        """{
                            "name": "${result.name}",
                            "execution_time_ms": ${result.executionTimeMs},
                            "ops_per_second": ${result.opsPerSecond},
                            "is_valid": ${result.isValid},
                            "metrics_json": ${result.metricsJson}
                        }"""
                    }.joinToString(",", "[", "]")
                
                val multiCoreResults = results.filter { it.name.contains("Multi") }
                    .map { result ->
                        """{
                            "name": "${result.name}",
                            "execution_time_ms": ${result.executionTimeMs},
                            "ops_per_second": ${result.opsPerSecond},
                            "is_valid": ${result.isValid},
                            "metrics_json": ${result.metricsJson}
                        }"""
                    }.joinToString(",", "[", "]")
                
                val resultsJson = """{
                    "single_core_results": $singleCoreResults,
                    "multi_core_results": $multiCoreResults
                }"""
                
                val summaryJson = benchmarkManager.calculateSummaryFromResults(resultsJson)
                _benchmarkState.value = BenchmarkState.Completed(summaryJson)
                
            } catch (e: Exception) {
                Log.e("BenchmarkViewModel", "Error during benchmark execution", e)
                _benchmarkState.value = BenchmarkState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }
}