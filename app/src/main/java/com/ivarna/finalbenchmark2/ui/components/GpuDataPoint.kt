package com.ivarna.finalbenchmark2.ui.components

data class GpuDataPoint(
    val timestamp: Long,        // Unix timestamp in milliseconds
    val utilization: Float      // GPU utilization percentage (0-100)
)