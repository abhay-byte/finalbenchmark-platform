package com.ivarna.finalbenchmark2.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.ivarna.finalbenchmark2.data.database.entities.BenchmarkResultEntity

@Entity(
    tableName = "generic_test_details",
    foreignKeys = [ForeignKey(
        entity = BenchmarkResultEntity::class,
        parentColumns = ["id"],
        childColumns = ["result_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [androidx.room.Index(value = ["result_id"])]
)
data class GenericTestDetailEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    
    @ColumnInfo(name = "result_id")
    val resultId: Long,
    
    @ColumnInfo(name = "test_name")
    val testName: String,
    
    @ColumnInfo(name = "score")
    val score: Double,
    
    @ColumnInfo(name = "metrics_json")
    val metricsJson: String = ""
)
