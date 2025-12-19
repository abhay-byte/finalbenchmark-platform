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
        val benchmarkDetails: BenchmarkDetails? = null
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
                            normalizedScore = 400,
                            singleCore = 128,
                            multiCore = 550,
                            isCurrentUser = false,
                            benchmarkDetails = BenchmarkDetails(
                                    // Single-Core ops/s values (converted from Mops/s)
                                    singleCorePrimeNumberMops = 3_080_000.0,
                                    singleCoreFibonacciMops = 45_410_000.0,
                                    singleCoreMatrixMultiplicationMops = 3_866_910_000.0,
                                    singleCoreHashComputingMops = 780_000.0,
                                    singleCoreStringSortingMops = 125_010_000.0,
                                    singleCoreRayTracingMops = 2_850_000.0,
                                    singleCoreCompressionMops = 757_920_000.0,
                                    singleCoreMonteCarloMops = 807_330_000.0,
                                    singleCoreJsonParsingMops = 1_360_000.0,
                                    singleCoreNQueensMops = 162_820_000.0,
                                    // Multi-Core ops/s values (converted from Mops/s)
                                    multiCorePrimeNumberMops = 11_470_000.0,
                                    multiCoreFibonacciMops = 161_840_000.0,
                                    multiCoreMatrixMultiplicationMops = 15_827_560_000.0,
                                    multiCoreHashComputingMops = 5_070_000.0,
                                    multiCoreStringSortingMops = 420_640_000.0,
                                    multiCoreRayTracingMops = 15_900_000.0,
                                    multiCoreCompressionMops = 2_935_130_000.0,
                                    multiCoreMonteCarloMops = 3_784_610_000.0,
                                    multiCoreJsonParsingMops = 4_590_000.0,
                                    multiCoreNQueensMops = 737_630_000.0
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
                            fun findMops(prefix: String, testName: String): Double {
                                return benchmarkResults
                                    .firstOrNull { it.name == "$prefix $testName" }
                                    ?.opsPerSecond ?: 0.0
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
