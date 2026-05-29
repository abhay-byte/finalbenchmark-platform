package com.ivarna.finalbenchmark2.ui.screens

import androidx.compose.runtime.Composable
import com.ivarna.finalbenchmark2.data.repository.HistoryRepository
import com.ivarna.finalbenchmark2.ui.screens.comparison.BaseComparisonScreen

@Composable
fun AiComparisonScreen(
    selectedDeviceJson: String,
    historyRepository: HistoryRepository,
    onBackClick: () -> Unit
) {
    BaseComparisonScreen(
        category = "AI",
        selectedDeviceJson = selectedDeviceJson,
        historyRepository = historyRepository,
        onBackClick = onBackClick
    )
}
