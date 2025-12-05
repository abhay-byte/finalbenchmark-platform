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
    
    suspend fun deleteResultById(id: Long) {
        benchmarkDao.deleteResultById(id)
    }
    
    suspend fun deleteAllResults() {
        benchmarkDao.deleteAllResults()
    }
}