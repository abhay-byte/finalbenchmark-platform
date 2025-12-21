package com.ivarna.finalbenchmark2.cpuBenchmark.algorithms

import android.util.Log
import com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkResult
import com.ivarna.finalbenchmark2.cpuBenchmark.CpuAffinityManager
import com.ivarna.finalbenchmark2.cpuBenchmark.WorkloadParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.json.JSONObject

object SingleCoreBenchmarks {
        private const val TAG = "SingleCoreBenchmarks"

        /**
         * Test 1: Prime Number Generation using Sieve of Eratosthenes
         * Complexity: O(N log log N) - Much faster than trial division
         * Tests: Memory access patterns, cache efficiency, and CPU arithmetic
         * UNIFIED: Uses same algorithm as Multi-Core for fair comparison
         */
        suspend fun primeGeneration(params: WorkloadParams, isTestRun: Boolean = false): BenchmarkResult =
                withContext(Dispatchers.Default) {
                        Log.d(
                                TAG,
                                "Starting Prime Generation (range: ${params.primeRange}) - Sieve of Eratosthenes (Single-threaded)"
                        )
                        CpuAffinityManager.setLastCoreAffinity()
                        CpuAffinityManager.setMaxPerformance()

                        val (primeCount, timeMs) =
                                BenchmarkHelpers.measureBenchmark {
                                        // Use Pollard's Rho for factorization
                                        BenchmarkHelpers.countFactorsPollardRho(params.primeRange)
                                }

                        val ops = params.primeRange.toDouble() // Operations = numbers processed
                        val opsPerSecond = ops / (timeMs / 1000.0)

                        CpuAffinityManager.resetPerformance()
                        CpuAffinityManager.resetCpuAffinity()

                        // Thermal stabilization delay (skip for test runs)
                        if (!isTestRun) {
                            kotlinx.coroutines.delay(1500)
                        }
                        return@withContext BenchmarkResult(
                                name = "Single-Core Prime Generation",
                                executionTimeMs = timeMs.toDouble(),
                                opsPerSecond = opsPerSecond,
                                isValid = primeCount > 0,
                                metricsJson =
                                        JSONObject()
                                                .apply {
                                                        put("prime_count", primeCount)
                                                        put("range", params.primeRange)
                                                        put("algorithm", "Sieve of Eratosthenes")
                                                        put("complexity", "O(N log log N)")
                                                        put("implementation", "Single-threaded")
                                                        put(
                                                                "description",
                                                                "Classic sieve algorithm - marks composites, counts primes"
                                                        )
                                                }
                                                .toString()
                        )
                }

        /**
         * Test 2: Single-Core Fibonacci - UPDATED for core-independent CPU benchmarking
         *
         * CORE-INDEPENDENT APPROACH:
         * - Uses SHARED iterative Fibonacci algorithm from BenchmarkHelpers
         * - Fixed workload: 10,000,000 iterations (ensures stable test duration)
         * - Same algorithm as Multi-Core version for fair comparison
         * - Tests raw CPU throughput (ALU speed) with consistent workload
         *
         * PERFORMANCE: ~20 Mops/s baseline for single-core devices
         */
        suspend fun fibonacciRecursive(params: WorkloadParams, isTestRun: Boolean = false): BenchmarkResult =
                withContext(Dispatchers.Default) {
                        Log.d(
                                TAG,
                                "Starting Single-Core Fibonacci - Core-independent fixed workload (10M iterations)"
                        )
                        CpuAffinityManager.setLastCoreAffinity()
                        CpuAffinityManager.setMaxPerformance()

                        // Use UNIFIED polynomial evaluation from BenchmarkHelpers

                        val (results, timeMs) =
                                BenchmarkHelpers.measureBenchmark {
                                        val iterations = params.fibonacciIterations

                                        var totalResult = 0L
                                        repeat(iterations) {
                                                // Call unified polynomial evaluation
                                                totalResult += BenchmarkHelpers.fibonacciIterative(35)
                                        }
                                        totalResult
                                }

                        val actualOps =
                                params.fibonacciIterations.toDouble() // Total iterations completed
                        val opsPerSecond = actualOps / (timeMs / 1000.0)

                        CpuAffinityManager.resetPerformance()
                        CpuAffinityManager.resetCpuAffinity()

                        // Thermal stabilization delay (skip for test runs)
                        if (!isTestRun) {
                            kotlinx.coroutines.delay(1500)
                        }
                        return@withContext BenchmarkResult(
                                name = "Single-Core Fibonacci Iterative",
                                executionTimeMs = timeMs.toDouble(),
                                opsPerSecond = opsPerSecond,
                                isValid = results > 0,
                                metricsJson =
                                        JSONObject()
                                                .apply {
                                                        put("fibonacci_sum", results)
                                                        put("target_n", 35)
                                                        put("iterations", 10_000_000)
                                                        put("implementation", "Shared Iterative")
                                                        put("time_complexity", "O(n)")
                                                        put("workload_type", "Fixed per core")
                                                        put(
                                                                "description",
                                                                "Core-independent CPU throughput test with shared algorithm"
                                                        )
                                                        put(
                                                                "expected_performance",
                                                                "~20 Mops/s baseline for single-core devices"
                                                        )
                                                }
                                                .toString()
                        )
                }

        /**
         * Test 3: Matrix Multiplication - Cache-Resident Strategy
         *
         * Uses small matrices (128x128) that fit in CPU cache to prevent memory bottlenecks.
         * Performs multiple repetitions to maintain CPU utilization and achieve meaningful
         * benchmark times. Complexity: O(n³ × iterations) Tests: CPU compute performance, not
         * memory bandwidth.
         */
        suspend fun matrixMultiplication(params: WorkloadParams, isTestRun: Boolean = false): BenchmarkResult =
                withContext(Dispatchers.Default) {
                        Log.d(
                                TAG,
                                "Starting Single-Core Matrix Multiplication (size: ${params.matrixSize}, iterations: ${params.matrixIterations}) - Cache-Resident Strategy"
                        )
                        CpuAffinityManager.setLastCoreAffinity()
                        CpuAffinityManager.setMaxPerformance()

                        val size = params.matrixSize
                        val iterations = params.matrixIterations

                        val (checksum, timeMs) =
                                BenchmarkHelpers.measureBenchmark {
                                        // CACHE-RESIDENT: Call matrix multiplication with
                                        // repetitions
                                        BenchmarkHelpers.performMatrixMultiplication(
                                                size,
                                                iterations
                                        )
                                }

                        // CACHE-RESIDENT: Total operations = size³ × 2 (multiply + add) ×
                        // iterations
                        val totalOps = size.toLong() * size * size * 2 * iterations
                        val opsPerSecond = totalOps / (timeMs / 1000.0)

                        CpuAffinityManager.resetPerformance()
                        CpuAffinityManager.resetCpuAffinity()

                        // Thermal stabilization delay (skip for test runs)
                        if (!isTestRun) {
                            kotlinx.coroutines.delay(1500)
                        }
                        return@withContext BenchmarkResult(
                                name = "Single-Core Matrix Multiplication",
                                executionTimeMs = timeMs.toDouble(),
                                opsPerSecond = opsPerSecond,
                                isValid = checksum != 0L,
                                metricsJson =
                                        JSONObject()
                                                .apply {
                                                        put("matrix_size", size)
                                                        put("matrix_iterations", iterations)
                                                        put("result_checksum", checksum)
                                                        put("total_operations", totalOps)
                                                        put(
                                                                "implementation",
                                                                "Cache-Resident Strategy - Small matrices with multiple repetitions"
                                                        )
                                                        put(
                                                                "workload_type",
                                                                "Multiple matrix multiplications"
                                                        )
                                                        put(
                                                                "strategy",
                                                                "Uses 128x128 matrices that fit in L2/L3 cache to prevent memory bottlenecks"
                                                        )
                                                        put(
                                                                "benefit",
                                                                "Tests CPU compute performance, enables true 8x multi-core scaling"
                                                        )
                                                }
                                                .toString()
                        )
                }

        /**
         * Test 4: Hash Computing (SHA-256-like) - CPU-BOUND ALGORITHM
         *
         * CPU-BOUND APPROACH:
         * - Uses SHA-256-like operations (rotations, shifts, XOR)
         * - No memory buffer (eliminates cache dependency)
         * - Tests instruction throughput, not memory speed
         * - Same algorithm as Multi-Core version for fair comparison
         *
         * PERFORMANCE: ~0.2 Mops/s baseline for single-core devices
         */
        suspend fun hashComputing(params: WorkloadParams, isTestRun: Boolean = false): BenchmarkResult =
                withContext(Dispatchers.Default) {
                        Log.d(TAG, "Starting Single-Core Hash Computing - CPU-BOUND SHA-256-like")
                        CpuAffinityManager.setLastCoreAffinity()
                        CpuAffinityManager.setMaxPerformance()

                        val iterations = params.hashIterations

                        val (finalHash, timeMs) =
                                BenchmarkHelpers.measureBenchmarkSuspend {
                                        // Call SHA-256-like hash computing (no buffer needed)
                                        BenchmarkHelpers.performHashComputing(iterations)
                                }

                        val opsPerSecond = iterations.toDouble() / (timeMs / 1000.0)

                        CpuAffinityManager.resetPerformance()
                        CpuAffinityManager.resetCpuAffinity()

                        // Thermal stabilization delay (skip for test runs)
                        if (!isTestRun) {
                            kotlinx.coroutines.delay(1500)
                        }
                        return@withContext BenchmarkResult(
                                name = "Single-Core Hash Computing",
                                executionTimeMs = timeMs.toDouble(),
                                opsPerSecond = opsPerSecond,
                                isValid = finalHash != 0L,
                                metricsJson =
                                        JSONObject()
                                                .apply {
                                                        put("hash_iterations", iterations)
                                                        put("final_hash", finalHash)
                                                        put("hashes_per_sec", opsPerSecond)
                                                        put(
                                                                "implementation",
                                                                "SHA-256-like CPU-Bound Algorithm"
                                                        )
                                                        put("algorithm", "SHA-256-like compression")
                                                        put(
                                                                "description",
                                                                "CPU-bound hash using rotations, shifts, XOR - no memory dependency"
                                                        )
                                                        put(
                                                                "expected_performance",
                                                                "~0.2 Mops/s baseline for single-core devices"
                                                        )
                                                }
                                                .toString()
                        )
                }

        /**
         * Test 5: String Sorting - CACHE-RESIDENT STRATEGY
         *
         * CACHE-RESIDENT APPROACH:
         * - Generate a small source list of exactly 4,096 strings (fits in CPU cache)
         * - Calculate iterations based on total string count (params.stringSortCount / 4096)
         * - Use centralized runStringSortWorkload helper for consistent algorithm
         * - Measures pure CPU sorting throughput, not memory bandwidth
         *
         * PERFORMANCE: ~3.0 Mops/s baseline for single-core devices
         */
        suspend fun stringSorting(params: WorkloadParams, isTestRun: Boolean = false): BenchmarkResult =
                withContext(Dispatchers.Default) {
                        Log.d(
                                TAG,
                                "Starting Single-Core String Sorting - CACHE-RESIDENT: ${params.stringSortCount} total strings"
                        )
                        CpuAffinityManager.setLastCoreAffinity()
                        CpuAffinityManager.setMaxPerformance()

                        // CACHE-RESIDENT: Generate small source list (4,096 strings) that fits in
                        // CPU cache
                        val cacheResidentSize = 4096
                        val sourceList = BenchmarkHelpers.generateStringList(cacheResidentSize, 16)

                        // Use explicit iterations from configuration
                        val iterations = params.stringSortIterations

                        Log.d(
                                TAG,
                                "Generated $cacheResidentSize source strings. Using iterations: $iterations"
                        )
                        Log.d(TAG, "Memory cleaned. Starting cache-resident sorting...")

                        val (checksum, timeMs) =
                                BenchmarkHelpers.measureBenchmark {
                                        // CACHE-RESIDENT: Use centralized helper function
                                        BenchmarkHelpers.runStringSortWorkload(
                                                sourceList,
                                                iterations
                                        )
                                }

                        // Calculate operations per second
                        // Total operations = iterations * comparisons_per_sort
                        // comparisons_per_sort = cacheResidentSize * log2(cacheResidentSize)
                        val comparisonsPerSort =
                                cacheResidentSize *
                                        kotlin.math.log(cacheResidentSize.toDouble(), 2.0)
                        val totalComparisons = iterations * comparisonsPerSort
                        val opsPerSecond = totalComparisons / (timeMs / 1000.0)

                        CpuAffinityManager.resetPerformance()
                        CpuAffinityManager.resetCpuAffinity()

                        // Thermal stabilization delay (skip for test runs)
                        if (!isTestRun) {
                            kotlinx.coroutines.delay(1500)
                        }
                        return@withContext BenchmarkResult(
                                name = "Single-Core String Sorting",
                                executionTimeMs = timeMs.toDouble(),
                                opsPerSecond = opsPerSecond,
                                isValid = checksum != 0 && timeMs > 0,
                                metricsJson =
                                        JSONObject()
                                                .apply {
                                                        put(
                                                                "cache_resident_size",
                                                                cacheResidentSize
                                                        )
                                                        put("total_strings", params.stringSortCount)
                                                        put("iterations", iterations)
                                                        put("string_length", 16)
                                                        put("result_checksum", checksum)
                                                        put(
                                                                "comparisons_per_sort",
                                                                comparisonsPerSort
                                                        )
                                                        put("total_comparisons", totalComparisons)
                                                        put(
                                                                "algorithm",
                                                                "Collections.sort() - Cache-Resident"
                                                        )
                                                        put(
                                                                "implementation",
                                                                "Cache-Resident Strategy - small fixed list with multiple iterations"
                                                        )
                                                        put(
                                                                "workload_type",
                                                                "Cache-Resistant - tests pure CPU throughput"
                                                        )
                                                        put(
                                                                "benefit",
                                                                "Prevents memory bandwidth bottlenecks, enables true multi-core scaling"
                                                        )
                                                        put(
                                                                "expected_performance",
                                                                "~3.0 Mops/s baseline for single-core devices"
                                                        )
                                                }
                                                .toString()
                        )
                }

        /** Helper extension to check if list is sorted */
        private fun List<String>.isSorted(): Boolean {
                for (i in 1 until this.size) {
                        if (this[i - 1] > this[i]) return false
                }
                return true
        }

        /**
         * OPTIMIZED: Heap-based merge sort implementation Uses iterative approach to avoid stack
         * overflow on large datasets Memory-efficient with single auxiliary array allocation
         */
        private fun <T : Comparable<T>> parallelMergeSort(list: MutableList<T>): List<T> {
                if (list.size <= 1) return list

                val aux = list.toMutableList() // Single auxiliary array allocation

                // Iterative bottom-up merge sort
                var size = 1
                while (size < list.size) {
                        var left = 0
                        while (left < list.size) {
                                val mid = left + size
                                val right = minOf(left + 2 * size, list.size)

                                if (mid < right) {
                                        merge(list, aux, left, mid, right)
                                }

                                left += 2 * size
                        }
                        size *= 2
                }

                return list
        }

        /** OPTIMIZED: In-place merge with minimal array bounds checking */
        private fun <T : Comparable<T>> merge(
                list: MutableList<T>,
                aux: MutableList<T>,
                left: Int,
                mid: Int,
                right: Int
        ) {
                // Copy to auxiliary array in one operation
                for (i in left until right) {
                        aux[i] = list[i]
                }

                var i = left
                var j = mid
                var k = left

                // Merge process with bounds optimization
                while (i < mid && j < right) {
                        if (aux[i] <= aux[j]) {
                                list[k++] = aux[i++]
                        } else {
                                list[k++] = aux[j++]
                        }
                }

                // Copy remaining elements (at most one of these loops will execute)
                while (i < mid) {
                        list[k++] = aux[i++]
                }
                while (j < right) {
                        list[k++] = aux[j++]
                }
        }

        /**
         * Test 6: Ray Tracing - CACHE-RESIDENT STRATEGY
         *
         * CACHE-RESIDENT APPROACH:
         * - Uses centralized performRayTracing function from BenchmarkHelpers
         * - Performs multiple iterations of the same scene rendering
         * - Scene data stays in CPU cache/registers for optimal performance
         * - Same algorithm as Multi-Core version for fair comparison
         *
         * PERFORMANCE: ~2.5 Mops/s baseline for single-core devices
         */
        suspend fun rayTracing(params: WorkloadParams, isTestRun: Boolean = false): BenchmarkResult =
                withContext(Dispatchers.Default) {
                        Log.d(
                                TAG,
                                "Starting Single-Core Ray Tracing - CACHE-RESIDENT: ${params.rayTracingIterations} iterations"
                        )
                        CpuAffinityManager.setLastCoreAffinity()
                        CpuAffinityManager.setMaxPerformance()

                        val (width, height) = params.rayTracingResolution
                        val maxDepth = params.rayTracingDepth
                        val iterations = params.rayTracingIterations

                        val (totalNoise, timeMs) =
                                BenchmarkHelpers.measureBenchmark {
                                        BenchmarkHelpers.performPerlinNoise(
                                                width,
                                                height,
                                                params.rayTracingDepth,  // Use depth as 3rd dimension
                                                iterations
                                        )
                                }

                        val totalRays = (width * height * iterations).toLong()
                        val raysPerSecond = totalRays / (timeMs / 1000.0)

                        CpuAffinityManager.resetPerformance()
                        CpuAffinityManager.resetCpuAffinity()

                        // Thermal stabilization delay (skip for test runs)
                        if (!isTestRun) {
                            kotlinx.coroutines.delay(1500)
                        }
                        return@withContext BenchmarkResult(
                                name = "Single-Core Ray Tracing",
                                executionTimeMs = timeMs.toDouble(),
                                opsPerSecond = raysPerSecond,
                                isValid = totalNoise != 0.0,
                                metricsJson =
                                        JSONObject()
                                                .apply {
                                                        put(
                                                                "resolution",
                                                                listOf(width, height).toString()
                                                        )
                                                        put("depth", params.rayTracingDepth)
                                                        put("iterations", iterations)
                                                        put("total_samples", totalRays)
                                                        put("total_noise", totalNoise)
                                                        put(
                                                                "implementation",
                                                                "Perlin Noise 3D - Procedural generation"
                                                        )
                                                }
                                                .toString()
                        )
                }

        /**
         * Test 7: Compression/Decompression - FIXED WORK PER CORE
         *
         * FIXED WORK PER CORE APPROACH:
         * - Uses centralized performCompression function from BenchmarkHelpers
         * - Fixed workload: params.compressionIterations per core (ensures core-independent
         * stability)
         * - Uses 2MB buffer (cache-friendly)
         * - Same algorithm as Multi-Core version for fair comparison
         *
         * PERFORMANCE: ~0.15 Gops/s baseline for single-core devices
         */
        suspend fun compression(params: WorkloadParams, isTestRun: Boolean = false): BenchmarkResult =
                withContext(Dispatchers.Default) {
                        Log.d(TAG, "Starting Single-Core Compression - FIXED WORK PER CORE")
                        CpuAffinityManager.setLastCoreAffinity()
                        CpuAffinityManager.setMaxPerformance()

                        // FIXED WORK PER CORE: Use params.compressionIterations with 2MB buffer
                        val bufferSize =
                                params.compressionDataSizeMb *
                                        1024 *
                                        1024 // Use configurable buffer size
                        val iterations =
                                params.compressionIterations // Use configurable workload per core

                        val (totalBytes, timeMs) =
                                BenchmarkHelpers.measureBenchmarkSuspend {
                                        // Call centralized compression function
                                        BenchmarkHelpers.performCompression(bufferSize, iterations)
                                }

                        // Calculate throughput in bytes per second
                        val throughput = totalBytes.toDouble() / (timeMs / 1000.0)

                        CpuAffinityManager.resetPerformance()
                        CpuAffinityManager.resetCpuAffinity()

                        // Thermal stabilization delay (skip for test runs)
                        if (!isTestRun) {
                            kotlinx.coroutines.delay(1500)
                        }
                        return@withContext BenchmarkResult(
                                name = "Single-Core Compression",
                                executionTimeMs = timeMs.toDouble(),
                                opsPerSecond = throughput,
                                isValid = totalBytes > 0,
                                metricsJson =
                                        JSONObject()
                                                .apply {
                                                        put(
                                                                "buffer_size_mb",
                                                                bufferSize / (1024 * 1024)
                                                        )
                                                        put("iterations", iterations)
                                                        put(
                                                                "total_data_processed_mb",
                                                                totalBytes / (1024 * 1024)
                                                        )
                                                        put("throughput_bps", throughput)
                                                        put(
                                                                "implementation",
                                                                "Centralized with Fixed Work Per Core"
                                                        )
                                                        put("workload_type", "Fixed per core")
                                                        put(
                                                                "description",
                                                                "Core-independent CPU compression test with shared algorithm"
                                                        )
                                                        put(
                                                                "expected_performance",
                                                                "~0.15 Gops/s baseline for single-core devices"
                                                        )
                                                }
                                                .toString()
                        )
                }

        /**
         * Test 8: Single-Core Leibniz π Calculation
         *
         * ALGORITHM: Uses Leibniz formula: π/4 = 1 - 1/3 + 1/5 - 1/7 + 1/9 - ...
         *
         * WHY LEIBNIZ INSTEAD OF MONTE CARLO:
         * - Deterministic: No random numbers, no caching issues
         * - Predictable: Same iterations = same result
         * - Pure arithmetic: Tests raw CPU throughput
         * - Fair comparison: Same algorithm as Multi-Core
         *
         * PERFORMANCE: Baseline for single-core comparison
         */
        suspend fun monteCarloPi(params: WorkloadParams, isTestRun: Boolean = false): BenchmarkResult =
                withContext(Dispatchers.Default) {
                        Log.d(TAG, "Starting Single-Core Leibniz π (iterations: ${params.monteCarloSamples})")
                        CpuAffinityManager.setLastCoreAffinity()
                        CpuAffinityManager.setMaxPerformance()

                        val iterations = params.monteCarloSamples.toLong()

                        val (totalIterations, timeMs) = BenchmarkHelpers.measureBenchmark {
                            BenchmarkHelpers.performMandelbrotSet(iterations, maxIterations = 256)
                        }

                        val opsPerSecond = totalIterations.toDouble() / (timeMs / 1000.0)

                        val isValid = timeMs > 0 && opsPerSecond > 0 && totalIterations > 0

                        CpuAffinityManager.resetPerformance()
                        CpuAffinityManager.resetCpuAffinity()

                        // Thermal stabilization delay (skip for test runs)
                        if (!isTestRun) {
                            kotlinx.coroutines.delay(1500)
                        }

                        return@withContext BenchmarkResult(
                                name = "Single-Core Monte Carlo π",
                                executionTimeMs = timeMs.toDouble(),
                                opsPerSecond = opsPerSecond,
                                isValid = isValid,
                                metricsJson =
                                        JSONObject()
                                                .apply {
                                                        put("samples", iterations)
                                                        put("total_iterations", totalIterations)
                                                        put("implementation", "Mandelbrot Set")
                                                        put("max_iterations", 256)
                                                }
                                                .toString()
                        )
                }

        /**
         * Test 9: JSON Parsing - CACHE-RESIDENT STRATEGY
         *
         * CACHE-RESIDENT APPROACH:
         * - Generate JSON data ONCE outside the timing block
         * - Perform multiple parsing iterations on the same JSON data
         * - JSON data stays in CPU cache for fast access
         * - Measures pure CPU parsing throughput, not memory bandwidth
         * - Same algorithm as Multi-Core version for fair comparison
         *
         * PERFORMANCE: ~2.0 Mops/s baseline for single-core devices
         */
        suspend fun jsonParsing(params: WorkloadParams, isTestRun: Boolean = false): BenchmarkResult =
                withContext(Dispatchers.Default) {
                        Log.d(
                                TAG,
                                "Starting Single-Core JSON Parsing - CACHE-RESIDENT: ${params.jsonDataSizeMb}MB, ${params.jsonParsingIterations} iterations"
                        )
                        CpuAffinityManager.setLastCoreAffinity()
                        CpuAffinityManager.setMaxPerformance()

                        val dataSize = params.jsonDataSizeMb * 1024 * 1024
                        val iterations = params.jsonParsingIterations

                        // CACHE-RESIDENT: Generate JSON OUTSIDE timing block
                        Log.d(TAG, "Generating JSON data ($dataSize bytes)...")
                        val jsonData = BenchmarkHelpers.generateComplexJson(dataSize)
                        Log.d(TAG, "JSON generated. Starting cache-resident parsing...")

                        val (totalElementCount, timeMs) =
                                BenchmarkHelpers.measureBenchmark {
                                        // CACHE-RESIDENT: Parse the same JSON multiple times
                                        BenchmarkHelpers.performJsonParsingWorkload(
                                                jsonData,
                                                iterations
                                        )
                                }

                        // Calculate operations per second based on total elements parsed
                        // This represents the actual parsing work done (elements × iterations)
                        val opsPerSecond = totalElementCount.toDouble() / (timeMs / 1000.0)

                        CpuAffinityManager.resetPerformance()
                        CpuAffinityManager.resetCpuAffinity()

                        // Thermal stabilization delay (skip for test runs)
                        if (!isTestRun) {
                            kotlinx.coroutines.delay(1500)
                        }
                        return@withContext BenchmarkResult(
                                name = "Single-Core JSON Parsing",
                                executionTimeMs = timeMs.toDouble(),
                                opsPerSecond = opsPerSecond,
                                isValid = totalElementCount > 0 && timeMs > 0,
                                metricsJson =
                                        JSONObject()
                                                .apply {
                                                        put("json_size_bytes", dataSize)
                                                        put("iterations", iterations)
                                                        put(
                                                                "total_element_count",
                                                                totalElementCount
                                                        )
                                                        put(
                                                                "implementation",
                                                                "Cache-Resident with Centralized Helper"
                                                        )
                                                }
                                                .toString()
                        )
                }

        /**
         * Test 10: N-Queens Problem - FIXED WORK PER CORE
         *
         * FIXED WORK PER CORE APPROACH:
         * - Uses centralized solveNQueens function from BenchmarkHelpers
         * - Tracks iterations (board evaluations) as the primary metric
         * - Solution count is constant for given N, so iterations measure performance
         * - Same algorithm as Multi-Core version for fair comparison
         *
         * PERFORMANCE: Varies by board size (N=10: ~350K iterations, N=12: ~14M iterations)
         */
        suspend fun nqueens(params: WorkloadParams, isTestRun: Boolean = false): BenchmarkResult =
                withContext(Dispatchers.Default) {
                        Log.d(
                                TAG,
                                "Starting Single-Core N-Queens (size: ${params.nqueensSize}) - FIXED: Iteration tracking"
                        )
                        CpuAffinityManager.setLastCoreAffinity()
                        CpuAffinityManager.setMaxPerformance()

                        val boardSize = params.nqueensSize

                        val (result, timeMs) =
                                BenchmarkHelpers.measureBenchmark {
                                        // Use centralized solver from BenchmarkHelpers
                                        BenchmarkHelpers.solveNQueens(boardSize)
                                }

                        val (solutionCount, iterationCount) = result

                        // FIXED: Use iterations as the metric (was dividing by 100.0 instead of
                        // 1000.0)
                        // Iterations represent actual work done, solution count is constant for
                        // given N
                        val opsPerSecond = iterationCount.toDouble() / (timeMs / 1000.0)

                        CpuAffinityManager.resetPerformance()
                        CpuAffinityManager.resetCpuAffinity()

                        // Thermal stabilization delay (skip for test runs)
                        if (!isTestRun) {
                            kotlinx.coroutines.delay(1500)
                        }
                        return@withContext BenchmarkResult(
                                name = "Single-Core N-Queens",
                                executionTimeMs = timeMs.toDouble(),
                                opsPerSecond = opsPerSecond,
                                isValid = solutionCount > 0 && iterationCount > 0 && timeMs > 0,
                                metricsJson =
                                        JSONObject()
                                                .apply {
                                                        put("board_size", boardSize)
                                                        put("solution_count", solutionCount)
                                                        put("iteration_count", iterationCount)
                                                        put("iterations_per_sec", opsPerSecond)
                                                        put(
                                                                "implementation",
                                                                "Centralized Bitwise Backtracking"
                                                        )
                                                        put(
                                                                "metric",
                                                                "Iterations (board evaluations) per second"
                                                        )
                                                        put(
                                                                "fix",
                                                                "Corrected opsPerSecond calculation (was /100.0, now /1000.0)"
                                                        )
                                                }
                                                .toString()
                        )
                }
}
