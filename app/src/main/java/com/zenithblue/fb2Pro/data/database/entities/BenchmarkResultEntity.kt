package com.zenithblue.fb2Pro.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "benchmark_results")
data class BenchmarkResultEntity(
        @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
        @ColumnInfo(name = "type")
        val type: String, // "Full", "CPU", "Throttle", "Efficiency", etc.
        @ColumnInfo(name = "total_score") val totalScore: Double,
        @ColumnInfo(name = "timestamp") val timestamp: Long = System.currentTimeMillis(),
        @ColumnInfo(name = "device_model") val deviceModel: String = "",
        @ColumnInfo(name = "single_core_score") val singleCoreScore: Double = 0.0,
        @ColumnInfo(name = "multi_core_score") val multiCoreScore: Double = 0.0,
        @ColumnInfo(name = "normalized_score") val normalizedScore: Double = 0.0,
        @ColumnInfo(name = "detailed_results_json") val detailedResultsJson: String = "",
        @ColumnInfo(name = "performance_metrics_json") val performanceMetricsJson: String = ""
)
