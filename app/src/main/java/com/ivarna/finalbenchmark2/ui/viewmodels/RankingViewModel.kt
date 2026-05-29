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
        val tag: String? = null,
        val category: String = "CPU"
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

    // --- Per-category reference devices ---

    private val cpuReferenceDevices = listOf(
            RankingItem(name = "Snapdragon 8 Gen 3", normalizedScore = 313, singleCore = 100, multiCore = 420, tag = "Baseline",
                    benchmarkDetails = BenchmarkDetails(
                            singleCorePrimeNumberMops = 749.24, singleCoreFibonacciMops = 5.08,
                            singleCoreMatrixMultiplicationMops = 3866.91, singleCoreHashComputingMops = 145.33,
                            singleCoreStringSortingMops = 128.87, singleCoreRayTracingMops = 9.57,
                            singleCoreCompressionMops = 761.08, singleCoreMonteCarloMops = 288.75,
                            singleCoreJsonParsingMops = 191777.09, singleCoreNQueensMops = 162.15,
                            multiCorePrimeNumberMops = 3719.17, multiCoreFibonacciMops = 12.47,
                            multiCoreMatrixMultiplicationMops = 14650.46, multiCoreHashComputingMops = 868.06,
                            multiCoreStringSortingMops = 417.69, multiCoreRayTracingMops = 34.00,
                            multiCoreCompressionMops = 3003.44, multiCoreMonteCarloMops = 1677.13,
                            multiCoreJsonParsingMops = 911354.73, multiCoreNQueensMops = 705.80)),
            RankingItem(name = "MediaTek Dimensity 8300", normalizedScore = 229, singleCore = 78, multiCore = 308,
                    benchmarkDetails = BenchmarkDetails(
                            singleCorePrimeNumberMops = 625.40, singleCoreFibonacciMops = 2.88,
                            singleCoreMatrixMultiplicationMops = 3298.27, singleCoreHashComputingMops = 144.73,
                            singleCoreStringSortingMops = 85.99, singleCoreRayTracingMops = 6.54,
                            singleCoreCompressionMops = 599.36, singleCoreMonteCarloMops = 287.48,
                            singleCoreJsonParsingMops = 179443.60, singleCoreNQueensMops = 135.89,
                            multiCorePrimeNumberMops = 2737.43, multiCoreFibonacciMops = 10.27,
                            multiCoreMatrixMultiplicationMops = 9338.83, multiCoreHashComputingMops = 677.19,
                            multiCoreStringSortingMops = 326.97, multiCoreRayTracingMops = 24.19,
                            multiCoreCompressionMops = 2025.99, multiCoreMonteCarloMops = 1029.77,
                            multiCoreJsonParsingMops = 653679.47, multiCoreNQueensMops = 547.86)),
            RankingItem(name = "Snapdragon 8s Gen 3", normalizedScore = 241, singleCore = 87, multiCore = 324,
                    benchmarkDetails = BenchmarkDetails(
                            singleCorePrimeNumberMops = 658.16, singleCoreFibonacciMops = 4.24,
                            singleCoreMatrixMultiplicationMops = 4147.30, singleCoreHashComputingMops = 127.95,
                            singleCoreStringSortingMops = 113.73, singleCoreRayTracingMops = 4.42,
                            singleCoreCompressionMops = 698.35, singleCoreMonteCarloMops = 254.83,
                            singleCoreJsonParsingMops = 165987.21, singleCoreNQueensMops = 149.82,
                            multiCorePrimeNumberMops = 2885.54, multiCoreFibonacciMops = 11.51,
                            multiCoreMatrixMultiplicationMops = 11969.48, multiCoreHashComputingMops = 694.42,
                            multiCoreStringSortingMops = 302.24, multiCoreRayTracingMops = 14.79,
                            multiCoreCompressionMops = 2470.87, multiCoreMonteCarloMops = 1299.32,
                            multiCoreJsonParsingMops = 757936.81, multiCoreNQueensMops = 586.43)),
            RankingItem(name = "MediaTek Dimensity 6300", normalizedScore = 107, singleCore = 50, multiCore = 137,
                    benchmarkDetails = BenchmarkDetails(
                            singleCorePrimeNumberMops = 464.02, singleCoreFibonacciMops = 1.80,
                            singleCoreMatrixMultiplicationMops = 1986.88, singleCoreHashComputingMops = 114.44,
                            singleCoreStringSortingMops = 50.67, singleCoreRayTracingMops = 2.36,
                            singleCoreCompressionMops = 314.09, singleCoreMonteCarloMops = 202.73,
                            singleCoreJsonParsingMops = 108521.36, singleCoreNQueensMops = 92.03,
                            multiCorePrimeNumberMops = 1211.56, multiCoreFibonacciMops = 4.42,
                            multiCoreMatrixMultiplicationMops = 3742.13, multiCoreHashComputingMops = 380.52,
                            multiCoreStringSortingMops = 137.24, multiCoreRayTracingMops = 5.98,
                            multiCoreCompressionMops = 915.59, multiCoreMonteCarloMops = 690.68,
                            multiCoreJsonParsingMops = 283666.70, multiCoreNQueensMops = 292.12))
    )

    /**
     * Per-category reference scores and names for non-CPU benchmarks.
     * GPU: GPU names. Storage: UFS names, only UFS 4.0 = 100 pts.
     * RAM: RAM type names (LPDDR5X = 100, LPDDR4X = 60).
     * AI/Productivity: SoC names. FULL: SoC names. */
    private val gpuReferenceDevices = mapOf(
            "Adreno 750" to 100,
            "Mali-G615 MC6" to 73,
            "Adreno 735" to 77,
            "Mali-G57 MC2" to 34
    )

    private val storageReferenceDevices = mapOf(
            "UFS 4.0" to 100
    )

    private val ramReferenceDevices = mapOf(
            "LPDDR5X" to 100,
            "LPDDR4X" to 60
    )

    private val nonCpuReferenceScores = mapOf(
            "Snapdragon 8 Gen 3" to 100,
            "MediaTek Dimensity 8300" to 73,
            "Snapdragon 8s Gen 3" to 77,
            "MediaTek Dimensity 6300" to 34
    )

    private val fullReferenceScores = mapOf(
            "Snapdragon 8 Gen 3" to 1000,
            "MediaTek Dimensity 8300" to 730,
            "Snapdragon 8s Gen 3" to 770,
            "MediaTek Dimensity 6300" to 340
    )

    private fun getReferenceDevices(category: String): List<RankingItem> {
        val cat = category.uppercase()
        return when (cat) {
            "CPU" -> cpuReferenceDevices
            "FULL" -> fullReferenceScores.asIterable().map { (name, score) ->
                RankingItem(name = name, normalizedScore = score, singleCore = 0, multiCore = 0, category = "FULL")
            }
            "GPU" -> gpuReferenceDevices.asIterable().map { (name, score) ->
                RankingItem(name = name, normalizedScore = score, singleCore = 0, multiCore = 0, category = "GPU")
            }
            "STORAGE" -> storageReferenceDevices.asIterable().map { (name, score) ->
                RankingItem(name = name, normalizedScore = score, singleCore = 0, multiCore = 0, category = "STORAGE")
            }
            "RAM" -> ramReferenceDevices.asIterable().map { (name, score) ->
                RankingItem(name = name, normalizedScore = score, singleCore = 0, multiCore = 0, category = "RAM")
            }
            else -> nonCpuReferenceScores.asIterable().map { (name, score) ->
                RankingItem(name = name, normalizedScore = score, singleCore = 0, multiCore = 0, category = cat)
            }
        }
    }

    init {
        loadRankings()
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
        loadRankings()
    }

    private fun loadRankings() {
        viewModelScope.launch {
            try {
                _screenState.value = RankingScreenState.Loading
                val category = _selectedCategory.value.uppercase()
                val userDeviceName = "Your Device (${Build.MODEL})"

                repository.getAllResults().collect { benchmarkResults ->
                    val filtered = benchmarkResults.filter {
                        if (category == "FULL")
                            it.benchmarkResult.type.equals("FULL", ignoreCase = true)
                        else
                            it.benchmarkResult.type.equals(category, ignoreCase = true)
                    }
                    val highestScore = filtered.maxByOrNull { it.benchmarkResult.normalizedScore }

                    var userScore: RankingItem? = null
                    if (highestScore != null) {
                        val details = if (category == "CPU") parseCpuDetails(highestScore) else null
                        userScore = RankingItem(
                                name = userDeviceName,
                                normalizedScore = highestScore.benchmarkResult.normalizedScore.toInt(),
                                singleCore = highestScore.benchmarkResult.singleCoreScore.toInt(),
                                multiCore = highestScore.benchmarkResult.multiCoreScore.toInt(),
                                isCurrentUser = true,
                                benchmarkDetails = details,
                                category = category
                        )
                    }

                    val refDevices = getReferenceDevices(category.uppercase())
                    val allDevices = mutableListOf<RankingItem>().apply {
                        addAll(refDevices)
                        if (userScore != null) add(userScore)
                    }

                    val rankedItems = allDevices.sortedByDescending { it.normalizedScore }
                            .mapIndexed { index, item -> item.copy(rank = index + 1) }

                    _screenState.value = RankingScreenState.Success(rankedItems)
                }
            } catch (e: Exception) {
                _screenState.value = RankingScreenState.Error
            }
        }
    }

    private fun parseCpuDetails(highestScore: com.ivarna.finalbenchmark2.data.database.entities.BenchmarkWithCpuData): BenchmarkDetails? {
        return try {
            val gson = com.google.gson.Gson()
            val results = gson.fromJson(
                    highestScore.benchmarkResult.detailedResultsJson,
                    Array<com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkResult>::class.java
            ).toList()

            fun findMops(prefix: String, testName: String): Double {
                val ops = results.firstOrNull { it.name == "$prefix $testName" }?.opsPerSecond ?: 0.0
                return ops / 1_000_000.0
            }

            BenchmarkDetails(
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
