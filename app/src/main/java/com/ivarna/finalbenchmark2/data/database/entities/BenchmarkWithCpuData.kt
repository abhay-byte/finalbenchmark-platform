package com.ivarna.finalbenchmark2.data.database.entities

import androidx.room.Embedded
import androidx.room.Relation
import com.ivarna.finalbenchmark2.data.database.entities.BenchmarkResultEntity
import com.ivarna.finalbenchmark2.data.database.entities.CpuTestDetailEntity

data class BenchmarkWithCpuData(
    @Embedded val benchmarkResult: BenchmarkResultEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "result_id"
    )
    val cpuTestDetail: CpuTestDetailEntity?
)