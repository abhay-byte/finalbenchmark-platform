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
        val isCurrentUser: Boolean = false
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
                            name = "Snapdragon 8 Elite",
                            normalizedScore = 1744,
                            singleCore = 344,
                            multiCore = 2499,
                            isCurrentUser = false
                    ),
                    RankingItem(
                            name = "Snapdragon 8 Gen 3",
                            normalizedScore = 1110,
                            singleCore = 335,
                            multiCore = 1530,
                            isCurrentUser = false
                    ),
                    RankingItem(
                            name = "Snapdragon 8s Gen 3",
                            normalizedScore = 981,
                            singleCore = 310,
                            multiCore = 1343,
                            isCurrentUser = false
                    ),
                    RankingItem(
                            name = "Dimensity 8300",
                            normalizedScore = 950,
                            singleCore = 265,
                            multiCore = 1325,
                            isCurrentUser = false
                    ),
                    RankingItem(
                            name = "MediaTek Helio G95",
                            normalizedScore = 454,
                            singleCore = 135,
                            multiCore = 625,
                            isCurrentUser = false
                    ),
                    RankingItem(
                            name = "Snapdragon 845",
                            normalizedScore = 270,
                            singleCore = 80,
                            multiCore = 370,
                            isCurrentUser = false
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
                                        isCurrentUser = true
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
