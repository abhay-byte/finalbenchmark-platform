package com.zenithblue.fb2Pro.ui.screens

import androidx.compose.runtime.Composable
import com.zenithblue.fb2Pro.data.repository.HistoryRepository
import com.zenithblue.fb2Pro.ui.screens.comparison.BaseComparisonScreen

@Composable
fun StorageComparisonScreen(
    selectedDeviceJson: String,
    historyRepository: HistoryRepository,
    onBackClick: () -> Unit
) {
    BaseComparisonScreen(
        category = "STORAGE",
        selectedDeviceJson = selectedDeviceJson,
        historyRepository = historyRepository,
        onBackClick = onBackClick
    )
}
