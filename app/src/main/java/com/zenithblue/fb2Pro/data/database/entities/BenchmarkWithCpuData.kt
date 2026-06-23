package com.zenithblue.fb2Pro.data.database.entities

import androidx.room.Embedded
import androidx.room.Relation
import com.zenithblue.fb2Pro.data.database.entities.BenchmarkResultEntity
import com.zenithblue.fb2Pro.data.database.entities.CpuTestDetailEntity

import com.zenithblue.fb2Pro.data.database.entities.GenericTestDetailEntity

data class BenchmarkWithCpuData(
    @Embedded val benchmarkResult: BenchmarkResultEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "result_id"
    )
    val cpuTestDetail: CpuTestDetailEntity?,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "result_id"
    )
    val genericTestDetails: List<GenericTestDetailEntity> = emptyList()
)