package com.ivarna.finalbenchmark2.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ivarna.finalbenchmark2.data.database.entities.BenchmarkWithCpuData
import com.ivarna.finalbenchmark2.data.repository.HistoryRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.util.Log

import com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkResult

data class HistoryUiModel(
    val id: Long,
    val timestamp: Long,
    val finalScore: Double,
    val singleCoreScore: Double,
    val multiCoreScore: Double,
    val testName: String,
    val normalizedScore: Double = 0.0,
    val detailedResults: List<BenchmarkResult> = emptyList()
)

class HistoryViewModel(
    private val repository: HistoryRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<List<HistoryUiModel>>(emptyList())
    val uiState: StateFlow<List<HistoryUiModel>> = _uiState.asStateFlow()
    
    init {
        loadHistoryData()
    }
    
    private fun loadHistoryData() {
        viewModelScope.launch {
            repository.getAllResults()
                .map { benchmarkList ->
                    benchmarkList.map { benchmark ->
                        // Parse detailed results from JSON string if available
                        val detailedResults = try {
                            if (benchmark.benchmarkResult.detailedResultsJson.isNotEmpty()) {
                                val gson = Gson()
                                val listType = object : TypeToken<List<com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkResult>>() {}.type
                                gson.fromJson(benchmark.benchmarkResult.detailedResultsJson, listType) as List<com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkResult>
                            } else {
                                emptyList()
                            }
                        } catch (e: Exception) {
                            Log.e("HistoryViewModel", "Error parsing detailed results JSON: ${e.message}")
                            emptyList()
                        }
                        
                        HistoryUiModel(
                            id = benchmark.benchmarkResult.id,
                            timestamp = benchmark.benchmarkResult.timestamp,
                            finalScore = benchmark.benchmarkResult.totalScore,
                            singleCoreScore = benchmark.benchmarkResult.singleCoreScore,
                            multiCoreScore = benchmark.benchmarkResult.multiCoreScore,
                            testName = benchmark.benchmarkResult.type,
                            normalizedScore = benchmark.benchmarkResult.normalizedScore,
                            detailedResults = detailedResults
                        )
                    }
                }
                .catch { throwable ->
                    // Handle error appropriately
                    println("Error loading history: ${throwable.message}")
                }
                .collect { historyList ->
                    _uiState.value = historyList
                }
        }
    }
    
    fun deleteResult(id: Long) {
        viewModelScope.launch {
            repository.deleteResultById(id)
        }
    }
    
    fun deleteAllResults() {
        viewModelScope.launch {
            repository.deleteAllResults()
        }
    }
    
    fun getBenchmarkDetail(id: Long) = repository.getResultById(id)
}