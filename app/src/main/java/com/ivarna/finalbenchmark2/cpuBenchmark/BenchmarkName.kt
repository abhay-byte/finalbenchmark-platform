package com.ivarna.finalbenchmark2.cpuBenchmark

/**
 * Enum representing all CPU benchmark types. This ensures type-safety and prevents string mismatch
 * errors.
 */
enum class BenchmarkName {
    PRIME_GENERATION,
    FIBONACCI_ITERATIVE,
    MATRIX_MULTIPLICATION,
    HASH_COMPUTING,
    STRING_SORTING,
    RAY_TRACING,
    COMPRESSION,
    MONTE_CARLO,
    JSON_PARSING,
    N_QUEENS;

    /** Returns the display name for this benchmark. Used in UI and logging. */
    fun displayName(): String {
        return when (this) {
            PRIME_GENERATION -> "Prime Generation"
            FIBONACCI_ITERATIVE -> "Fibonacci Iterative"
            MATRIX_MULTIPLICATION -> "Matrix Multiplication"
            HASH_COMPUTING -> "Hash Computing"
            STRING_SORTING -> "String Sorting"
            RAY_TRACING -> "Ray Tracing"
            COMPRESSION -> "Compression"
            MONTE_CARLO -> "Monte Carlo π"
            JSON_PARSING -> "JSON Parsing"
            N_QUEENS -> "N-Queens"
        }
    }

    /** Returns the single-core prefixed name. */
    fun singleCore(): String = "Single-Core ${displayName()}"

    /** Returns the multi-core prefixed name. */
    fun multiCore(): String = "Multi-Core ${displayName()}"

    companion object {
        /**
         * Parse a benchmark name from a string (e.g., from BenchmarkResult). Removes "Single-Core "
         * or "Multi-Core " prefix and matches to enum.
         */
        fun fromString(name: String): BenchmarkName? {
            val cleanName = name.replace("Single-Core ", "").replace("Multi-Core ", "").trim()

            return values().find { it.displayName() == cleanName }
        }
    }
}
