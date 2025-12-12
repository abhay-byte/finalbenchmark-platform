package com.ivarna.finalbenchmark2.ui.screens

/** Data class to hold device summary information for the results screen */
data class BenchmarkDeviceSummary(
        val deviceName: String,
        val os: String,
        val kernel: String,
        val cpuName: String,
        val cpuCores: Int,
        val cpuArchitecture: String,
        val cpuGovernor: String,
        val gpuName: String,
        val gpuVendor: String,
        val gpuDriver: String,
        val batteryLevel: Float?,
        val batteryTemp: Float?,
        val totalRam: Long,
        val totalSwap: Long,
        val completedTimestamp: Long // Unix timestamp when benchmark completed
)
