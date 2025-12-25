package com.ivarna.finalbenchmark2.data.database.dao

import androidx.room.*
import com.ivarna.finalbenchmark2.data.database.entities.BenchmarkResultEntity
import com.ivarna.finalbenchmark2.data.database.entities.CpuTestDetailEntity
import com.ivarna.finalbenchmark2.data.database.entities.GenericTestDetailEntity
import com.ivarna.finalbenchmark2.data.database.entities.BenchmarkWithCpuData
import kotlinx.coroutines.flow.Flow

@Dao
interface BenchmarkDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResult(result: BenchmarkResultEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCpuDetail(detail: CpuTestDetailEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGenericDetails(details: List<GenericTestDetailEntity>)
    
    @Transaction
    suspend fun saveCpuBenchmark(benchmarkResult: BenchmarkResultEntity, cpuDetail: CpuTestDetailEntity) {
        val resultId = insertResult(benchmarkResult)
        insertCpuDetail(cpuDetail.copy(resultId = resultId))
    }

    @Transaction
    suspend fun saveBenchmark(benchmarkResult: BenchmarkResultEntity, genericDetails: List<GenericTestDetailEntity>) {
        val resultId = insertResult(benchmarkResult)
        val detailsWithId = genericDetails.map { it.copy(resultId = resultId) }
        insertGenericDetails(detailsWithId)
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