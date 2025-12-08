package com.ivarna.finalbenchmark2.cpuBenchmark

/**
 * Represents an event during benchmark execution
 */
data class BenchmarkEvent(
    val testName: String,
    val mode: String, // "SINGLE" | "MULTI"
    val state: String, // "STARTED" | "COMPLETED"
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
 * Represents workload parameters for standardized benchmarking
 * Optimized for consistent 1.5-2.0 second execution times on flagship devices
 */
data class WorkloadParams(
    val primeRange: Int = 250000,
    val fibonacciNRange: Pair<Int, Int> = Pair(30, 32),
    val matrixSize: Int = 350,
    val hashDataSizeMb: Int = 2,
    val stringCount: Int = 15000,
    val rayTracingResolution: Pair<Int, Int> = Pair(192, 192),
    val rayTracingDepth: Int = 3,
    val compressionDataSizeMb: Int = 2,
    val monteCarloSamples: Int = 1000000,
    val jsonDataSizeMb: Int = 1,
    val nqueensSize: Int = 10
)