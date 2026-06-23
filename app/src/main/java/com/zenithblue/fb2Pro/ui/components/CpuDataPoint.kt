package com.zenithblue.fb2Pro.ui.components

data class CpuDataPoint(
    val timestamp: Long,        // Unix timestamp in milliseconds
    val utilization: Float      // CPU utilization percentage (0-100)
)