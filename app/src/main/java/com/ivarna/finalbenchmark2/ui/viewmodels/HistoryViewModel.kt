package com.ivarna.finalbenchmark2.ui.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkResult
import com.ivarna.finalbenchmark2.data.database.AppDatabase
import com.ivarna.finalbenchmark2.data.repository.HistoryRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HistoryUiModel(
        val id: Long,
        val timestamp: Long,
        val finalScore: Double,
        val singleCoreScore: Double,
        val multiCoreScore: Double,
        val testName: String,
        val normalizedScore: Double = 0.0,
        val detailedResults: List<BenchmarkResult> = emptyList(),
        val performanceMetricsJson: String = ""
)

sealed interface HistoryScreenState {
    object Loading : HistoryScreenState
    data class Success(val results: List<HistoryUiModel>) : HistoryScreenState
    object Empty : HistoryScreenState
}

enum class HistorySort {
    DATE_NEWEST,
    DATE_OLDEST,
    SCORE_HIGH_TO_LOW,
    SCORE_LOW_TO_HIGH
}

class HistoryViewModel(private val repository: HistoryRepository) : ViewModel() {

    private val _selectedCategory = MutableStateFlow("All")
    private val _sortOption = MutableStateFlow(HistorySort.DATE_NEWEST)

    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()
    val sortOption: StateFlow<HistorySort> = _sortOption.asStateFlow()

    val screenState: StateFlow<HistoryScreenState> =
            combine(repository.getAllResults(), _selectedCategory, _sortOption) {
                            rawList,
                            category,
                            sort ->
                        val list =
                                rawList.map { benchmark ->
                                    // Parse detailed results from JSON string if available
                                    val detailedResults =
                                            try {
                                                if (benchmark.benchmarkResult.detailedResultsJson
                                                                .isNotEmpty()
                                                ) {
                                                    val gson = Gson()
                                                    val listType =
                                                            object :
                                                                            TypeToken<
                                                                                    List<
                                                                                            com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkResult>>() {}
                                                                    .type
                                                    gson.fromJson(
                                                            benchmark
                                                                    .benchmarkResult
                                                                    .detailedResultsJson,
                                                            listType
                                                    ) as
                                                            List<
                                                                    com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkResult>
                                                } else {
                                                    emptyList()
                                                }
                                            } catch (e: Exception) {
                                                Log.e(
                                                        "HistoryViewModel",
                                                        "Error parsing detailed results JSON: ${e.message}"
                                                )
                                                emptyList()
                                            }

                                    HistoryUiModel(
                                            id = benchmark.benchmarkResult.id,
                                            timestamp = benchmark.benchmarkResult.timestamp,
                                            finalScore = benchmark.benchmarkResult.totalScore,
                                            singleCoreScore =
                                                    benchmark.benchmarkResult.singleCoreScore,
                                            multiCoreScore =
                                                    benchmark.benchmarkResult.multiCoreScore,
                                            testName = benchmark.benchmarkResult.type,
                                            normalizedScore =
                                                    benchmark.benchmarkResult.normalizedScore,
                                            detailedResults = detailedResults,
                                            performanceMetricsJson =
                                                    benchmark.benchmarkResult.performanceMetricsJson
                                    )
                                }

                        // 1. Filter
                        var filteredList = list
                        if (category != "All") {
                            filteredList =
                                    filteredList.filter {
                                        it.testName.contains(category, ignoreCase = true)
                                    }
                        }

                        // 2. Sort
                        val sortedList =
                                when (sort) {
                                    HistorySort.DATE_NEWEST ->
                                            filteredList.sortedWith(compareByDescending<HistoryUiModel> { it.timestamp }.thenByDescending { it.id })
                                    HistorySort.DATE_OLDEST ->
                                            filteredList.sortedWith(compareBy<HistoryUiModel> { it.timestamp }.thenBy { it.id })
                                    HistorySort.SCORE_HIGH_TO_LOW ->
                                            filteredList.sortedWith(compareByDescending<HistoryUiModel> { it.finalScore }.thenByDescending { it.id })
                                    HistorySort.SCORE_LOW_TO_HIGH ->
                                            filteredList.sortedWith(compareBy<HistoryUiModel> { it.finalScore }.thenBy { it.id })
                                }

                        // Return appropriate state based on the results
                        if (sortedList.isEmpty()) {
                            HistoryScreenState.Empty
                        } else {
                            HistoryScreenState.Success(sortedList)
                        }
                    }
                    .stateIn(
                            scope = viewModelScope,
                            started = SharingStarted.WhileSubscribed(5000),
                            initialValue = HistoryScreenState.Loading
                    )

    companion object {
        class Factory(private val context: Context) : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
                    val database = AppDatabase.getDatabase(context)
                    val dao = database.benchmarkDao()
                    val repository = HistoryRepository(dao)
                    @Suppress("UNCHECKED_CAST") return HistoryViewModel(repository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }

    fun updateCategory(category: String) {
        _selectedCategory.value = category
    }

    fun updateSortOption(sort: HistorySort) {
        _sortOption.value = sort
    }

    fun deleteResult(id: Long) {
        viewModelScope.launch { repository.deleteResultById(id) }
    }

    fun deleteAllResults() {
        viewModelScope.launch { repository.deleteAllResults() }
    }

    fun getBenchmarkDetail(id: Long) = repository.getResultById(id)
}
