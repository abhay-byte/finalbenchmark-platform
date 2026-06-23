package com.zenithblue.fb2Pro.ui.components

data class MemoryDataPoint(
    val timestamp: Long,        // Unix timestamp in milliseconds
    val utilization: Float      // Memory utilization percentage (0-100)
)