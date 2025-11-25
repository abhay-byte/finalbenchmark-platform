package com.ivarna.finalbenchmark2.cpuBenchmark

/**
 * Represents an event during benchmark execution
 */
data class BenchmarkEvent(
    val testName: String,
    val mode: String, // "SINGLE" | "MULTI"
    val state: String, // "STARTED" | "FINISHED"
    val timeMs: Long,
    val score: Double
)

/**
 * Represents the final benchmark summary
 */
data class BenchmarkSummary(
    val singleCoreScore: Double,
    val multiCoreScore: Double,
    val finalScore: Double,
    val normalizedScore: Double,
    val rating: String
)

/**
 * Represents a single benchmark result
 */
data class BenchmarkResult(
    val name: String,
    val executionTimeMs: Double,
    val opsPerSecond: Double,
    val isValid: Boolean,
    val metricsJson: String
)

/**
 * Represents benchmark configuration
 */
data class BenchmarkConfig(
    val iterations: Int = 3,
    val warmup: Boolean = true,
    val warmupCount: Int = 3,
    val deviceTier: String = "Mid" // "Slow", "Mid", or "Flagship"
)

/**
 * Represents workload parameters
 */
data class WorkloadParams(
    val primeRange: Int = 10000,
    val fibonacciNRange: Pair<Int, Int> = Pair(10, 15),
    val matrixSize: Int = 50,
    val hashDataSizeMb: Int = 1,
    val stringCount: Int = 1000,
    val rayTracingResolution: Pair<Int, Int> = Pair(64, 64),
    val rayTracingDepth: Int = 2,
    val compressionDataSizeMb: Int = 1,
    val monteCarloSamples: Int = 10000,
    val jsonDataSizeMb: Int = 1,
    val nqueensSize: Int = 8
)