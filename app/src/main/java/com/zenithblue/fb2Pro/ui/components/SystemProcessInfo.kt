package com.zenithblue.fb2Pro.ui.components

data class SystemInfoSummary(
    val runningProcesses: Int = 0,
    val totalPackages: Int = 0,
    val totalServices: Int = 0,
    val processes: List<ProcessItem> = emptyList()
)

data class ProcessItem(
    val name: String,
    val pid: Int,
    val ramUsage: Int, // in MB
    val state: String,
    val packageName: String = ""
)