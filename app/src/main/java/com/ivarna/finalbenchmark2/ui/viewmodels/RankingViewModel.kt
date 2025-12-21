package com.ivarna.finalbenchmark2.ui.viewmodels

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ivarna.finalbenchmark2.data.repository.HistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RankingItem(
        val rank: Int = 0,
        val name: String,
        val normalizedScore: Int,
        val singleCore: Int,
        val multiCore: Int,
        val isCurrentUser: Boolean = false,
        val benchmarkDetails: BenchmarkDetails? = null,
        val tag: String? = null
)

data class BenchmarkDetails(
        // Single-Core Mops/s values
        val singleCorePrimeNumberMops: Double = 0.0,
        val singleCoreFibonacciMops: Double = 0.0,
        val singleCoreMatrixMultiplicationMops: Double = 0.0,
        val singleCoreHashComputingMops: Double = 0.0,
        val singleCoreStringSortingMops: Double = 0.0,
        val singleCoreRayTracingMops: Double = 0.0,
        val singleCoreCompressionMops: Double = 0.0,
        val singleCoreMonteCarloMops: Double = 0.0,
        val singleCoreJsonParsingMops: Double = 0.0,
        val singleCoreNQueensMops: Double = 0.0,
        // Multi-Core Mops/s values
        val multiCorePrimeNumberMops: Double = 0.0,
        val multiCoreFibonacciMops: Double = 0.0,
        val multiCoreMatrixMultiplicationMops: Double = 0.0,
        val multiCoreHashComputingMops: Double = 0.0,
        val multiCoreStringSortingMops: Double = 0.0,
        val multiCoreRayTracingMops: Double = 0.0,
        val multiCoreCompressionMops: Double = 0.0,
        val multiCoreMonteCarloMops: Double = 0.0,
        val multiCoreJsonParsingMops: Double = 0.0,
        val multiCoreNQueensMops: Double = 0.0
)

sealed interface RankingScreenState {
    object Loading : RankingScreenState
    data class Success(val rankings: List<RankingItem>) : RankingScreenState
    object Error : RankingScreenState
}

class RankingViewModel(private val repository: HistoryRepository) : ViewModel() {

    private val _selectedCategory = MutableStateFlow("CPU")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _screenState = MutableStateFlow<RankingScreenState>(RankingScreenState.Loading)
    val screenState: StateFlow<RankingScreenState> = _screenState.asStateFlow()

    private val hardcodedReferenceDevices =
            listOf(
                    RankingItem(
                            name = "Snapdragon 8 Gen 3",
                            normalizedScore = 313,
                            singleCore = 100,
                            multiCore = 420,
                            isCurrentUser = false,
                            tag = "Baseline",
                            benchmarkDetails = BenchmarkDetails(
                                    // Single-Core Mops/s values
                                    singleCorePrimeNumberMops = 749.24,
                                    singleCoreFibonacciMops = 5.08,
                                    singleCoreMatrixMultiplicationMops = 3866.91,
                                    singleCoreHashComputingMops = 145.33,
                                    singleCoreStringSortingMops = 128.87,
                                    singleCoreRayTracingMops = 9.57,
                                    singleCoreCompressionMops = 761.08,
                                    singleCoreMonteCarloMops = 288.75,
                                    singleCoreJsonParsingMops = 191777.09,
                                    singleCoreNQueensMops = 162.15,
                                    // Multi-Core Mops/s values
                                    multiCorePrimeNumberMops = 3719.17,
                                    multiCoreFibonacciMops = 12.47,
                                    multiCoreMatrixMultiplicationMops = 14650.46,
                                    multiCoreHashComputingMops = 868.06,
                                    multiCoreStringSortingMops = 417.69,
                                    multiCoreRayTracingMops = 34.00,
                                    multiCoreCompressionMops = 3003.44,
                                    multiCoreMonteCarloMops = 1677.13,
                                    multiCoreJsonParsingMops = 911354.73,
                                    multiCoreNQueensMops = 705.80
                            )
                    ),
                    RankingItem(
                            name = "MediaTek Dimensity 8300",
                            normalizedScore = 229,
                            singleCore = 78,
                            multiCore = 308,
                            isCurrentUser = false,
                            benchmarkDetails = BenchmarkDetails(
                                    // Single-Core Mops/s values
                                    singleCorePrimeNumberMops = 625.40,
                                    singleCoreFibonacciMops = 2.88,
                                    singleCoreMatrixMultiplicationMops = 3298.27,
                                    singleCoreHashComputingMops = 144.73,
                                    singleCoreStringSortingMops = 85.99,
                                    singleCoreRayTracingMops = 6.54,
                                    singleCoreCompressionMops = 599.36,
                                    singleCoreMonteCarloMops = 287.48,
                                    singleCoreJsonParsingMops = 179443.60,
                                    singleCoreNQueensMops = 135.89,
                                    // Multi-Core Mops/s values
                                    multiCorePrimeNumberMops = 2737.43,
                                    multiCoreFibonacciMops = 10.27,
                                    multiCoreMatrixMultiplicationMops = 9338.83,
                                    multiCoreHashComputingMops = 677.19,
                                    multiCoreStringSortingMops = 326.97,
                                    multiCoreRayTracingMops = 24.19,
                                    multiCoreCompressionMops = 2025.99,
                                    multiCoreMonteCarloMops = 1029.77,
                                    multiCoreJsonParsingMops = 653679.47,
                                    multiCoreNQueensMops = 547.86
                            )
                    )
            )

    init {
        loadRankings()
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    private fun loadRankings() {
        viewModelScope.launch {
            try {
                _screenState.value = RankingScreenState.Loading

                // Fetch the highest CPU score from the user's device
                val userDeviceName = "Your Device (${Build.MODEL})"
                var userScore: RankingItem? = null

                // Collect the latest results to find the highest CPU score
                repository.getAllResults().collect { benchmarkResults ->
                    val highestCpuScore =
                            benchmarkResults
                                    .filter {
                                        it.benchmarkResult.type.contains("CPU", ignoreCase = true)
                                    }
                                    .maxByOrNull { it.benchmarkResult.normalizedScore }

                    if (highestCpuScore != null) {
                        // Parse detailed results JSON to extract separate single-core and multi-core Mops/s
                        val details = try {
                            val gson = com.google.gson.Gson()
                            val detailedResultsJson = highestCpuScore.benchmarkResult.detailedResultsJson
                            val benchmarkResults = gson.fromJson(
                                detailedResultsJson,
                                Array<com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkResult>::class.java
                            ).toList()
                            
                            // Helper function to find Mops/s for a specific benchmark
                            // Note: Database stores opsPerSecond, need to convert to Mops/s
                            fun findMops(prefix: String, testName: String): Double {
                                val opsPerSecond = benchmarkResults
                                    .firstOrNull { it.name == "$prefix $testName" }
                                    ?.opsPerSecond ?: 0.0
                                return opsPerSecond / 1_000_000.0  // Convert ops/s to Mops/s
                            }
                            
                            BenchmarkDetails(
                                // Single-Core Mops/s values
                                singleCorePrimeNumberMops = findMops("Single-Core", "Prime Generation"),
                                singleCoreFibonacciMops = findMops("Single-Core", "Fibonacci Iterative"),
                                singleCoreMatrixMultiplicationMops = findMops("Single-Core", "Matrix Multiplication"),
                                singleCoreHashComputingMops = findMops("Single-Core", "Hash Computing"),
                                singleCoreStringSortingMops = findMops("Single-Core", "String Sorting"),
                                singleCoreRayTracingMops = findMops("Single-Core", "Ray Tracing"),
                                singleCoreCompressionMops = findMops("Single-Core", "Compression"),
                                singleCoreMonteCarloMops = findMops("Single-Core", "Monte Carlo π"),
                                singleCoreJsonParsingMops = findMops("Single-Core", "JSON Parsing"),
                                singleCoreNQueensMops = findMops("Single-Core", "N-Queens"),
                                // Multi-Core Mops/s values
                                multiCorePrimeNumberMops = findMops("Multi-Core", "Prime Generation"),
                                multiCoreFibonacciMops = findMops("Multi-Core", "Fibonacci Iterative"),
                                multiCoreMatrixMultiplicationMops = findMops("Multi-Core", "Matrix Multiplication"),
                                multiCoreHashComputingMops = findMops("Multi-Core", "Hash Computing"),
                                multiCoreStringSortingMops = findMops("Multi-Core", "String Sorting"),
                                multiCoreRayTracingMops = findMops("Multi-Core", "Ray Tracing"),
                                multiCoreCompressionMops = findMops("Multi-Core", "Compression"),
                                multiCoreMonteCarloMops = findMops("Multi-Core", "Monte Carlo π"),
                                multiCoreJsonParsingMops = findMops("Multi-Core", "JSON Parsing"),
                                multiCoreNQueensMops = findMops("Multi-Core", "N-Queens")
                            )
                        } catch (e: Exception) {
                            null
                        }
                        
                        userScore =
                                RankingItem(
                                        name = userDeviceName,
                                        normalizedScore =
                                                highestCpuScore.benchmarkResult.normalizedScore
                                                        .toInt(),
                                        singleCore =
                                                highestCpuScore.benchmarkResult.singleCoreScore
                                                        .toInt(),
                                        multiCore =
                                                highestCpuScore.benchmarkResult.multiCoreScore
                                                        .toInt(),
                                        isCurrentUser = true,
                                        benchmarkDetails = details
                                )
                    }

                    // Merge and sort
                    val allDevices =
                            mutableListOf<RankingItem>().apply {
                                addAll(hardcodedReferenceDevices)
                                if (userScore != null) {
                                    add(userScore!!)
                                }
                            }

                    // Sort by normalized score in descending order and assign ranks
                    val rankedItems =
                            allDevices.sortedByDescending { it.normalizedScore }.mapIndexed {
                                    index,
                                    item ->
                                item.copy(rank = index + 1)
                            }

                    _screenState.value = RankingScreenState.Success(rankedItems)
                }
            } catch (e: Exception) {
                _screenState.value = RankingScreenState.Error
            }
        }
    }
}

class RankingViewModelFactory(private val repository: HistoryRepository) :
        ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RankingViewModel::class.java)) {
            return RankingViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
