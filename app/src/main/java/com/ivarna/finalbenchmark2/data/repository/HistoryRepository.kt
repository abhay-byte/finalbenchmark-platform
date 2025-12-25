package com.ivarna.finalbenchmark2.data.repository

import com.ivarna.finalbenchmark2.data.database.dao.BenchmarkDao
import com.ivarna.finalbenchmark2.data.database.entities.BenchmarkResultEntity
import com.ivarna.finalbenchmark2.data.database.entities.CpuTestDetailEntity
import com.ivarna.finalbenchmark2.data.database.entities.BenchmarkWithCpuData
import kotlinx.coroutines.flow.Flow

class HistoryRepository(
    private val benchmarkDao: BenchmarkDao
) {
    
    fun getAllResults(): Flow<List<BenchmarkWithCpuData>> {
        return benchmarkDao.getAllResults()
    }
    
    fun getResultsByType(benchmarkType: String): Flow<List<BenchmarkWithCpuData>> {
        return benchmarkDao.getResultsByType(benchmarkType)
    }
    
    fun getResultById(id: Long): Flow<BenchmarkWithCpuData?> {
        return benchmarkDao.getResultById(id)
    }
    
    suspend fun saveCpuBenchmark(
        benchmarkResult: BenchmarkResultEntity,
        cpuDetail: CpuTestDetailEntity
    ) {
        benchmarkDao.saveCpuBenchmark(benchmarkResult, cpuDetail)
    }

    suspend fun saveGenericBenchmark(
        benchmarkResult: BenchmarkResultEntity,
        genericDetails: List<com.ivarna.finalbenchmark2.data.database.entities.GenericTestDetailEntity>
    ) {
        benchmarkDao.saveBenchmark(benchmarkResult, genericDetails)
    }
    
    suspend fun deleteResultById(id: Long) {
        android.util.Log.d("HistoryRepository", "deleteResultById called with ID: $id")
        try {
            benchmarkDao.deleteResultById(id)
            android.util.Log.d("HistoryRepository", "Successfully deleted benchmark with ID: $id")
        } catch (e: Exception) {
            android.util.Log.e("HistoryRepository", "Error deleting benchmark with ID: $id", e)
            throw e
        }
    }
    
    suspend fun deleteAllResults() {
        benchmarkDao.deleteAllResults()
    }
}