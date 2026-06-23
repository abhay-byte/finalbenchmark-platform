package com.zenithblue.fb2Pro.ui.models

// Data class to hold system statistics
data class SystemStats(
    val cpuLoad: Float = 0f,
    val power: Float = 0f,
    val temp: Float = 0f,
    val memoryLoad: Float = 0f
)