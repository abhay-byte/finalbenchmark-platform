package com.ivarna.finalbenchmark2.data.database.dao

import androidx.room.*
import com.ivarna.finalbenchmark2.data.database.entities.BenchmarkResultEntity
import com.ivarna.finalbenchmark2.data.database.entities.CpuTestDetailEntity
import com.ivarna.finalbenchmark2.data.database.entities.BenchmarkWithCpuData
import kotlinx.coroutines.flow.Flow

@Dao
interface BenchmarkDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResult(result: BenchmarkResultEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCpuDetail(detail: CpuTestDetailEntity)
    
    @Transaction
    suspend fun saveCpuBenchmark(benchmarkResult: BenchmarkResultEntity, cpuDetail: CpuTestDetailEntity) {
        val resultId = insertResult(benchmarkResult)
        insertCpuDetail(cpuDetail.copy(resultId = resultId))
    }
    
    @Transaction
    @Query("SELECT * FROM benchmark_results ORDER BY timestamp DESC")
    fun getAllResults(): Flow<List<BenchmarkWithCpuData>>
    
    @Transaction
    @Query("SELECT * FROM benchmark_results WHERE type = :benchmarkType ORDER BY timestamp DESC")
    fun getResultsByType(benchmarkType: String): Flow<List<BenchmarkWithCpuData>>
    
    @Transaction
    @Query("SELECT * FROM benchmark_results WHERE id = :id")
    fun getResultById(id: Long): Flow<BenchmarkWithCpuData?>
    
    @Query("DELETE FROM benchmark_results WHERE id = :id")
    suspend fun deleteResultById(id: Long)
    
    @Query("DELETE FROM benchmark_results")
    suspend fun deleteAllResults()
}