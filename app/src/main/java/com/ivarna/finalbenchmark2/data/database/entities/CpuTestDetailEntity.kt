package com.ivarna.finalbenchmark2.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey
import com.ivarna.finalbenchmark2.data.database.entities.BenchmarkResultEntity

@Entity(
    tableName = "cpu_test_details",
    foreignKeys = [ForeignKey(
        entity = BenchmarkResultEntity::class,
        parentColumns = ["id"],
        childColumns = ["result_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [androidx.room.Index(value = ["result_id"])]
)
data class CpuTestDetailEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    
    @ColumnInfo(name = "result_id")
    val resultId: Long, // Foreign key to BenchmarkResultEntity
    
    @ColumnInfo(name = "prime_number_score")
    val primeNumberScore: Double = 0.0,
    
    @ColumnInfo(name = "fibonacci_score")
    val fibonacciScore: Double = 0.0,
    
    @ColumnInfo(name = "matrix_multiplication_score")
    val matrixMultiplicationScore: Double = 0.0,
    
    @ColumnInfo(name = "hash_computing_score")
    val hashComputingScore: Double = 0.0,
    
    @ColumnInfo(name = "string_sorting_score")
    val stringSortingScore: Double = 0.0,
    
    @ColumnInfo(name = "ray_tracing_score")
    val rayTracingScore: Double = 0.0,
    
    @ColumnInfo(name = "compression_score")
    val compressionScore: Double = 0.0,
    
    @ColumnInfo(name = "monte_carlo_score")
    val monteCarloScore: Double = 0.0,
    
    @ColumnInfo(name = "json_parsing_score")
    val jsonParsingScore: Double = 0.0,
    
    @ColumnInfo(name = "n_queens_score")
    val nQueensScore: Double = 0.0
)