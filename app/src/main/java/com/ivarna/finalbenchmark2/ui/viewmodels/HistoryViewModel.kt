package com.ivarna.finalbenchmark2.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ivarna.finalbenchmark2.data.database.entities.BenchmarkWithCpuData
import com.ivarna.finalbenchmark2.data.repository.HistoryRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HistoryUiModel(
    val id: Long,
    val timestamp: Long,
    val finalScore: Double,
    val singleCoreScore: Double,
    val multiCoreScore: Double,
    val testName: String
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
                        HistoryUiModel(
                            id = benchmark.benchmarkResult.id,
                            timestamp = benchmark.benchmarkResult.timestamp,
                            finalScore = benchmark.benchmarkResult.totalScore,
                            singleCoreScore = benchmark.benchmarkResult.singleCoreScore,
                            multiCoreScore = benchmark.benchmarkResult.multiCoreScore,
                            testName = benchmark.benchmarkResult.type
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
}