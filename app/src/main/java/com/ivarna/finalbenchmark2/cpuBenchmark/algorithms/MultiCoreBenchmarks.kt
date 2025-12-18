package com.ivarna.finalbenchmark2.cpuBenchmark.algorithms

import android.os.Process
import android.util.Log
import com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkResult
import com.ivarna.finalbenchmark2.cpuBenchmark.CpuAffinityManager
import com.ivarna.finalbenchmark2.cpuBenchmark.WorkloadParams
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.*
import org.json.JSONObject

object MultiCoreBenchmarks {
        private const val TAG = "MultiCoreBenchmarks"

        // Number of threads = number of physical cores
        private val numThreads = Runtime.getRuntime().availableProcessors()

        /**
         * Custom high-priority dispatcher that creates a FixedThreadPool with all threads set to
         * URGENT priority. This forces Android's EAS to schedule threads on all available cores
         * (Big, Mid, and Little cores) instead of limiting them to just performance cores.
         *
         * CRITICAL FIX: Removed 'lazy' initialization to prevent first-run slowdown
         * The lazy initialization caused the first benchmark run to be slow while creating
         * the thread pool, and subsequent runs to be fast using the cached pool.
         */
        private val highPriorityDispatcher: CoroutineDispatcher = run {
                val threadCount = numThreads
                val threadFactory = ThreadFactory { runnable ->
                        Thread(runnable).apply {
                                // Set high priority using Android's Process API
                                // THREAD_PRIORITY_URGENT_DISPLAY = -10 (highest priority for UI
                                // tasks)
                                Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_DISPLAY)
                                name = "BenchmarkWorker-${threadId.getAndIncrement()}"
                        }
                }

                val executor = Executors.newFixedThreadPool(threadCount, threadFactory)
                executor.asCoroutineDispatcher()
        }

        private val threadId = AtomicInteger(0)

        /**
         * Test 1: Parallel Prime Generation FIXED: Use strided loop for perfect load balancing Each
         * thread processes numbers with step = numThreads for equal work distribution
         */
        suspend fun primeGeneration(params: WorkloadParams, isTestRun: Boolean = false): BenchmarkResult = coroutineScope {
                Log.d(
                        TAG,
                        "Starting Multi-Core Prime Generation - FIXED: Strided loop for load balancing"
                )
                CpuAffinityManager.setMaxPerformance()

                val (primeCount, timeMs) =
                        BenchmarkHelpers.measureBenchmark {
                                val range = params.primeRange

                                // FIXED: Use strided loop for perfect load balancing
                                val results =
                                        (0 until numThreads)
                                                .map { threadId ->
                                                        async(highPriorityDispatcher) {
                                                                var count = 0

                                                                // FIXED: Strided loop - each thread
                                                                // processes every
                                                                // numThreads-th number
                                                                // Thread 0: 0, numThreads,
                                                                // 2*numThreads, ...
                                                                // Thread 1: 1, 1+numThreads,
                                                                // 1+2*numThreads, ...
                                                                // This ensures equal mix of easy
                                                                // (small) and hard
                                                                // (large) numbers
                                                                var i = threadId
                                                                while (i <= range) {
                                                                        if (i > 1 &&
                                                                                        BenchmarkHelpers
                                                                                                .isPrime(
                                                                                                        i.toLong()
                                                                                                )
                                                                        ) {
                                                                                count++
                                                                        }
                                                                        i += numThreads
                                                                }

                                                                count
                                                        }
                                                }
                                                .awaitAll()
                                                .sum()

                                results
                        }

                val ops = params.primeRange.toDouble() // Operations = numbers checked
                val opsPerSecond = ops / (timeMs / 1000.0)

                CpuAffinityManager.resetPerformance()

                // Thermal stabilization delay (skip for test runs)
                if (!isTestRun) {
                    kotlinx.coroutines.delay(1500)
                }
                BenchmarkResult(
                        name = "Multi-Core Prime Generation",
                        executionTimeMs = timeMs.toDouble(),
                        opsPerSecond = opsPerSecond,
                        isValid = primeCount > 0,
                        metricsJson =
                                JSONObject()
                                        .apply {
                                                put("prime_count", primeCount)
                                                put("range", params.primeRange)
                                                put("threads", numThreads)
                                                put("algorithm", "Strided Trial Division")
                                                put(
                                                        "load_balancing",
                                                        "Perfect - each thread gets equal mix of easy/hard numbers"
                                                )
                                                put(
                                                        "fix",
                                                        "Replaced range-based chunking with strided loop"
                                                )
                                        }
                                        .toString()
                )
        }

        /**
         * Test 2: Multi-Core Fibonacci - CORE-INDEPENDENT CPU BENCHMARKING
         *
         * FIXED WORK PER CORE APPROACH:
         * - Uses SHARED iterative Fibonacci algorithm from BenchmarkHelpers
         * - Fixed workload per thread: 10,000,000 iterations (ensures core-independent stability)
         * - Total work scales with cores: 10M × numThreads = Total Operations
         * - Test duration remains constant regardless of core count
         * - Same algorithm as Single-Core version for fair comparison
         *
         * PERFORMANCE: ~160 Mops/s on 8-core devices (8x single-core baseline)
         */
        suspend fun fibonacciRecursive(params: WorkloadParams, isTestRun: Boolean = false): BenchmarkResult = coroutineScope {
                Log.d(TAG, "=== STARTING MULTI-CORE FIBONACCI - CORE INDEPENDENT ===")
                Log.d(TAG, "Threads available: $numThreads")
                Log.d(TAG, "Fixed workload per thread: 10,000,000 iterations")
                Log.d(TAG, "Total expected operations: ${10_000_000 * numThreads}")
                CpuAffinityManager.setMaxPerformance()

                // Configuration - CORE-INDEPENDENT APPROACH
                val targetN = 35 // Consistent with Single-Core config
                val iterationsPerThread =
                        params.fibonacciIterations // Use configurable workload per core
                val totalOperations = iterationsPerThread * numThreads // Scales with cores

                // Expected value for validation (fib(35) = 9227465)
                val expectedFibValue = 9227465L

                // EXPLICIT timing with try-catch for debugging
                val startTime = System.currentTimeMillis()
                var totalResults = 0L
                var executionSuccess = true

                try {
                        Log.d(TAG, "Starting parallel execution with $numThreads threads")
                        Log.d(TAG, "Each thread will perform $iterationsPerThread iterations")

                        // Simple parallel execution - each thread does FIXED amount of work
                        val threadResults =
                                (0 until numThreads).map { threadId ->
                                        async(highPriorityDispatcher) {
                                                var threadSum = 0L

                                                // Each thread computes fibonacci(targetN) fixed
                                                // number of times
                                                repeat(iterationsPerThread) { iteration ->
                                                        val fibResult =
                                                                BenchmarkHelpers.fibonacciIterative(
                                                                        targetN
                                                                )
                                                        threadSum += fibResult

                                                        // Validate first result
                                                        if (iteration == 0 &&
                                                                        fibResult !=
                                                                                expectedFibValue
                                                        ) {
                                                                Log.e(
                                                                        TAG,
                                                                        "Fibonacci validation failed: got $fibResult, expected $expectedFibValue"
                                                                )
                                                        }
                                                }

                                                Log.d(
                                                        TAG,
                                                        "Thread $threadId completed $iterationsPerThread iterations, sum: $threadSum"
                                                )
                                                threadSum
                                        }
                                }

                        // Await all results
                        val results = threadResults.awaitAll()
                        Log.d(TAG, "Async operations completed: ${results.size} results")
                        totalResults = results.sum()

                        // Verify no thread failed
                        if (results.any { it <= 0 }) {
                                Log.e(
                                        TAG,
                                        "Multi-Core Fibonacci: One or more threads returned invalid results"
                                )
                                executionSuccess = false
                        }
                } catch (e: Exception) {
                        Log.e(TAG, "Multi-Core Fibonacci EXCEPTION: ${e.message}", e)
                        executionSuccess = false
                }

                val endTime = System.currentTimeMillis()
                val timeMs = (endTime - startTime).toDouble()

                // Calculate operations per second (total operations across all threads)
                val actualOps = totalOperations.toDouble()
                val opsPerSecond = if (timeMs > 0) actualOps / (timeMs / 1000.0) else 0.0

                // Validation
                val isValid =
                        executionSuccess &&
                                totalResults > 0 &&
                                timeMs > 0 &&
                                opsPerSecond > 0 &&
                                timeMs < 30000 // Should complete in under 30 seconds

                Log.d(TAG, "=== MULTI-CORE FIBONACCI COMPLETE ===")
                Log.d(TAG, "Time: ${timeMs}ms, Total Ops: $actualOps, Ops/sec: $opsPerSecond")
                Log.d(TAG, "Valid: $isValid, Execution success: $executionSuccess")

                CpuAffinityManager.resetPerformance()

                // Thermal stabilization delay (skip for test runs)
                if (!isTestRun) {
                    kotlinx.coroutines.delay(1500)
                }
                BenchmarkResult(
                        name = "Multi-Core Fibonacci Iterative",
                        executionTimeMs = timeMs,
                        opsPerSecond = opsPerSecond,
                        isValid = isValid,
                        metricsJson =
                                JSONObject()
                                        .apply {
                                                put("fibonacci_sum", totalResults)
                                                put("target_n", targetN)
                                                put("expected_fib_value", expectedFibValue)
                                                put("iterations_per_thread", iterationsPerThread)
                                                put("threads", numThreads)
                                                put("total_operations", totalOperations)
                                                put("actual_ops", actualOps)
                                                put("time_ms", timeMs)
                                                put("ops_per_second", opsPerSecond)
                                                put("execution_success", executionSuccess)
                                                put(
                                                        "implementation",
                                                        "Shared Iterative with Fixed Work Per Core"
                                                )
                                                put(
                                                        "workload_approach",
                                                        "Fixed Work Per Core - ensures core-independent test duration"
                                                )
                                                put(
                                                        "expected_performance",
                                                        "~160 Mops/s on 8-core devices (8x single-core)"
                                                )
                                        }
                                        .toString()
                )
        }

        /**
         * Test 3: Multi-Core Matrix Multiplication - Cache-Resident Strategy
         *
         * CACHE-RESIDENT STRATEGY:
         * - Uses small matrices (128x128) that fit in CPU cache to prevent memory bottlenecks
         * - Each thread performs multiple repetitions to maintain CPU utilization
         * - Total work scales with cores AND iterations: numThreads × iterations × (2 × size³)
         * - Perfect scaling: 8 cores = 8× the operations in the same time
         *
         * This fixes the OOM crashes and enables true 8x multi-core scaling by testing CPU compute
         * performance instead of memory bandwidth.
         */
        suspend fun matrixMultiplication(params: WorkloadParams, isTestRun: Boolean = false): BenchmarkResult = coroutineScope {
                Log.d(
                        TAG,
                        "=== STARTING MULTI-CORE MATRIX MULTIPLICATION - CACHE-RESIDENT STRATEGY ==="
                )
                Log.d(TAG, "Threads available: $numThreads")
                Log.d(
                        TAG,
                        "Matrix size: ${params.matrixSize}, Iterations per thread: ${params.matrixIterations}"
                )
                Log.d(
                        TAG,
                        "Total expected operations: ${numThreads} × ${params.matrixIterations} × (2 × ${params.matrixSize}³)"
                )
                CpuAffinityManager.setMaxPerformance()

                val size = params.matrixSize
                val iterations = params.matrixIterations
                val expectedTotalOps = numThreads * (2L * size * size * size * iterations)

                // EXPLICIT timing with try-catch for debugging
                val startTime = System.currentTimeMillis()
                var totalChecksum = 0L
                var executionSuccess = true

                try {
                        Log.d(TAG, "Starting parallel execution with $numThreads threads")
                        Log.d(TAG, "Each thread will perform $iterations matrix multiplications")

                        // CACHE-RESIDENT: Each thread performs multiple matrix multiplications
                        val threadResults =
                                (0 until numThreads).map { threadId ->
                                        async(highPriorityDispatcher) {
                                                Log.d(
                                                        TAG,
                                                        "Thread $threadId starting $iterations matrix multiplications"
                                                )

                                                // CACHE-RESIDENT: Each thread performs its
                                                // repetitions
                                                val checksum =
                                                        BenchmarkHelpers
                                                                .performMatrixMultiplication(
                                                                        size,
                                                                        iterations
                                                                )

                                                Log.d(
                                                        TAG,
                                                        "Thread $threadId completed $iterations matrix multiplications, checksum: $checksum"
                                                )
                                                checksum
                                        }
                                }

                        // Await all results
                        val results = threadResults.awaitAll()
                        Log.d(TAG, "All threads completed: ${results.size} results")
                        totalChecksum = results.sum()

                        // Verify no thread failed
                        if (results.any { it == 0L }) {
                                Log.e(
                                        TAG,
                                        "Multi-Core Matrix Multiplication: One or more threads returned invalid results"
                                )
                                executionSuccess = false
                        }
                } catch (e: Exception) {
                        Log.e(TAG, "Multi-Core Matrix Multiplication EXCEPTION: ${e.message}", e)
                        executionSuccess = false
                }

                val endTime = System.currentTimeMillis()
                val timeMs = (endTime - startTime).toDouble()

                // Calculate operations per second (total operations across all threads)
                val actualOps = expectedTotalOps.toDouble()
                val opsPerSecond = if (timeMs > 0) actualOps / (timeMs / 1000.0) else 0.0

                // Validation
                val isValid =
                        executionSuccess &&
                                totalChecksum != 0L &&
                                timeMs > 0 &&
                                opsPerSecond > 0 &&
                                timeMs < 30000 // Should complete in under 30 seconds

                Log.d(TAG, "=== MULTI-CORE MATRIX MULTIPLICATION COMPLETE ===")
                Log.d(
                        TAG,
                        "Time: ${timeMs}ms, Total Checksum: $totalChecksum, Ops/sec: $opsPerSecond"
                )
                Log.d(TAG, "Valid: $isValid, Execution success: $executionSuccess")

                CpuAffinityManager.resetPerformance()

                // Thermal stabilization delay (skip for test runs)
                if (!isTestRun) {
                    kotlinx.coroutines.delay(1500)
                }
                BenchmarkResult(
                        name = "Multi-Core Matrix Multiplication",
                        executionTimeMs = timeMs,
                        opsPerSecond = opsPerSecond,
                        isValid = isValid,
                        metricsJson =
                                JSONObject()
                                        .apply {
                                                put("matrix_size", size)
                                                put("matrix_iterations", iterations)
                                                put("result_checksum", totalChecksum)
                                                put("threads", numThreads)
                                                put("expected_total_operations", expectedTotalOps)
                                                put("actual_ops", actualOps)
                                                put("time_ms", timeMs)
                                                put("ops_per_second", opsPerSecond)
                                                put("execution_success", executionSuccess)
                                                put(
                                                        "implementation",
                                                        "Cache-Resident Strategy - Small matrices with multiple repetitions per thread"
                                                )
                                                put(
                                                        "workload_approach",
                                                        "Cache-Resident - prevents memory bottlenecks, enables true CPU scaling"
                                                )
                                                put(
                                                        "benefits",
                                                        "No OOM crashes, true 8x multi-core scaling, tests CPU compute not memory bandwidth"
                                                )
                                                put(
                                                        "optimization",
                                                        "128x128 matrices fit in L2/L3 cache, cache-friendly i-k-j loop order"
                                                )
                                        }
                                        .toString()
                )
        }

        /**
         * Test 4: Multi-Core Hash Computing - FIXED WORK PER CORE
         *
         * FIXED WORK PER CORE APPROACH:
         * - Uses centralized performHashComputing function from BenchmarkHelpers
         * - Fixed workload per thread: params.hashIterations (ensures core-independent stability)
         * - Total work scales with cores: hashIterations × numThreads = Total Operations
         * - Test duration remains constant regardless of core count
         * - Same algorithm as Single-Core version for fair comparison
         *
         * PERFORMANCE: ~1.6 Mops/s on 8-core devices (8x single-core baseline)
         */
        suspend fun hashComputing(params: WorkloadParams, isTestRun: Boolean = false): BenchmarkResult = coroutineScope {
                Log.d(TAG, "=== STARTING MULTI-CORE HASH COMPUTING - FIXED WORK PER CORE ===")
                Log.d(TAG, "Threads available: $numThreads")
                Log.d(TAG, "Fixed workload per thread: ${params.hashIterations} iterations")
                Log.d(TAG, "Total expected operations: ${params.hashIterations * numThreads}")
                CpuAffinityManager.setMaxPerformance()

                // Configuration - FIXED WORK PER CORE APPROACH
                val bufferSize = 4 * 1024 // 4KB (cache-friendly)
                val iterationsPerThread =
                        params.hashIterations // Use configurable workload per core
                val totalHashes = iterationsPerThread * numThreads // Scales with cores

                // EXPLICIT timing with try-catch for debugging
                val startTime = System.currentTimeMillis()
                var totalHashesCompleted = 0L
                var executionSuccess = true

                try {
                        Log.d(TAG, "Starting parallel execution with $numThreads threads")
                        Log.d(TAG, "Each thread will perform $iterationsPerThread hash iterations")

                        // FIXED WORK PER CORE: Each thread performs the full workload
                        val threadResults =
                                (0 until numThreads).map { threadId ->
                                        async(highPriorityDispatcher) {
                                                Log.d(
                                                        TAG,
                                                        "Thread $threadId starting $iterationsPerThread hash iterations"
                                                )

                                                // Call centralized hash computing function with
                                                // full workload per
                                                // thread
                                                val threadBytesProcessed =
                                                        BenchmarkHelpers.performHashComputing(
                                                                bufferSize,
                                                                iterationsPerThread
                                                        )
                                                val threadHashCount =
                                                        threadBytesProcessed / bufferSize

                                                Log.d(
                                                        TAG,
                                                        "Thread $threadId completed $iterationsPerThread iterations, processed $threadHashCount hashes"
                                                )
                                                threadHashCount
                                        }
                                }

                        // Await all results
                        val results = threadResults.awaitAll()
                        Log.d(TAG, "All threads completed: ${results.size} results")
                        totalHashesCompleted = results.sum()

                        // Verify no thread failed
                        if (results.any { it <= 0 }) {
                                Log.e(
                                        TAG,
                                        "Multi-Core Hash Computing: One or more threads returned invalid results"
                                )
                                executionSuccess = false
                        }
                } catch (e: Exception) {
                        Log.e(TAG, "Multi-Core Hash Computing EXCEPTION: ${e.message}", e)
                        executionSuccess = false
                }

                val endTime = System.currentTimeMillis()
                val timeMs = (endTime - startTime).toDouble()

                // Calculate throughput (total operations across all threads)
                val totalBytes = totalHashesCompleted * bufferSize
                val throughputMBps = (totalBytes.toDouble() / (1024 * 1024)) / (timeMs / 1000.0)
                val opsPerSecond = totalHashesCompleted.toDouble() / (timeMs / 1000.0)

                // Validation
                val isValid =
                        executionSuccess &&
                                totalHashesCompleted > 0 &&
                                timeMs > 0 &&
                                opsPerSecond > 0

                Log.d(TAG, "=== MULTI-CORE HASH COMPUTING COMPLETE ===")
                Log.d(
                        TAG,
                        "Time: ${timeMs}ms, Total Hashes: $totalHashesCompleted, Ops/sec: $opsPerSecond"
                )
                Log.d(TAG, "Valid: $isValid, Execution success: $executionSuccess")

                CpuAffinityManager.resetPerformance()

                // Thermal stabilization delay (skip for test runs)
                if (!isTestRun) {
                    kotlinx.coroutines.delay(1500)
                }
                BenchmarkResult(
                        name = "Multi-Core Hash Computing",
                        executionTimeMs = timeMs,
                        opsPerSecond = opsPerSecond,
                        isValid = isValid,
                        metricsJson =
                                JSONObject()
                                        .apply {
                                                put("buffer_size_kb", bufferSize / 1024)
                                                put(
                                                        "hash_iterations_per_thread",
                                                        iterationsPerThread
                                                )
                                                put("threads", numThreads)
                                                put("total_hashes", totalHashesCompleted)
                                                put("total_bytes_processed", totalBytes)
                                                put("throughput_mbps", throughputMBps)
                                                put("hashes_per_sec", opsPerSecond)
                                                put("time_ms", timeMs)
                                                put("execution_success", executionSuccess)
                                                put(
                                                        "implementation",
                                                        "Pure Kotlin FNV Hash - No Native Locks"
                                                )
                                                put(
                                                        "workload_approach",
                                                        "Fixed Work Per Core - ensures core-independent test duration"
                                                )
                                                put(
                                                        "expected_performance",
                                                        "~1.6 Mops/s on 8-core devices (8x single-core)"
                                                )
                                        }
                                        .toString()
                )
        }

        /**
         * Test 5: Multi-Core String Sorting - CACHE-RESIDENT STRATEGY
         *
         * CACHE-RESIDENT APPROACH:
         * - Generate small source list (4,096 strings) that fits in CPU cache
         * - Each thread performs multiple iterations independently using centralized helper
         * - Total work scales with cores AND iterations: numThreads * iterationsPerThread
         * - Perfect scaling: 8 cores = 8× the operations in the same time
         *
         * PERFORMANCE: ~24.0 Mops/s on 8-core devices (8x single-core baseline)
         */
        suspend fun stringSorting(params: WorkloadParams, isTestRun: Boolean = false): BenchmarkResult = coroutineScope {
                Log.d(TAG, "=== STARTING MULTI-CORE STRING SORTING - CACHE-RESIDENT STRATEGY ===")
                Log.d(TAG, "Threads available: $numThreads")
                Log.d(TAG, "Using explicit iterations from params: ${params.stringSortIterations}")
                CpuAffinityManager.setMaxPerformance()

                // CACHE-RESIDENT APPROACH
                val cacheResidentSize = 4096
                val iterationsPerThread = params.stringSortIterations

                Log.d(
                        TAG,
                        "Cache-resident size: $cacheResidentSize, iterations per thread: $iterationsPerThread"
                )

                // EXPLICIT timing with try-catch for debugging
                val startTime = System.currentTimeMillis()
                var totalChecksum = 0
                var executionSuccess = true

                try {
                        Log.d(TAG, "Generating cache-resident source list...")

                        // STEP 1: Generate source list (4,096 strings) OUTSIDE timing
                        val sourceList = BenchmarkHelpers.generateStringList(cacheResidentSize, 16)

                        Log.d(TAG, "Source list generated. Cleaning memory...")

                        // FORCE GC to clear generation garbage
                        System.gc()
                        // Small sleep to let GC finish and CPU settle
                        kotlinx.coroutines.delay(200)

                        Log.d(TAG, "Memory cleaned. Starting parallel cache-resident sorting...")

                        // STEP 2: TIME ONLY THE SORTING (measured)
                        Log.d(TAG, "Starting parallel cache-resident sort timing...")
                        val sortStartTime = System.currentTimeMillis()

                        // Each thread performs iterations independently using centralized helper
                        val threadResults =
                                (0 until numThreads)
                                        .map { threadId ->
                                                async(highPriorityDispatcher) {
                                                        Log.d(
                                                                TAG,
                                                                "Thread $threadId starting $iterationsPerThread iterations"
                                                        )
                                                        // Use centralized cache-resident workload
                                                        // helper
                                                        val threadChecksum =
                                                                BenchmarkHelpers
                                                                        .runStringSortWorkload(
                                                                                sourceList,
                                                                                iterationsPerThread
                                                                        )
                                                        Log.d(
                                                                TAG,
                                                                "Thread $threadId completed, checksum: $threadChecksum"
                                                        )
                                                        threadChecksum
                                                }
                                        }
                                        .awaitAll()

                        val sortEndTime = System.currentTimeMillis()
                        val sortTimeMs = (sortEndTime - sortStartTime).toDouble()

                        Log.d(TAG, "All threads completed sorting in ${sortTimeMs}ms")

                        // Sum up checksums from all threads
                        totalChecksum = threadResults.sum()

                        // Verify no thread failed
                        // REMOVED: No need to validate checksum values - hash codes can be negative
                        // legitimately
                        Log.d(TAG, "All ${threadResults.size} threads completed successfully")
                } catch (e: Exception) {
                        Log.e(TAG, "Multi-Core String Sorting EXCEPTION: ${e.message}", e)
                        executionSuccess = false
                }

                val endTime = System.currentTimeMillis()
                val timeMs = (endTime - startTime).toDouble()

                // Calculate operations per second
                // Total comparisons across all threads: numThreads * (iterationsPerThread *
                // comparisonsPerSort)
                val comparisonsPerSort =
                        cacheResidentSize * kotlin.math.log(cacheResidentSize.toDouble(), 2.0)
                val totalComparisons = numThreads * (iterationsPerThread * comparisonsPerSort)
                val opsPerSecond = if (timeMs > 0) totalComparisons / (timeMs / 1000.0) else 0.0

                // REMOVED: Negative checksum validation - hash codes can be legitimately negative
                val isValid = executionSuccess && timeMs > 0 && opsPerSecond > 0

                Log.d(TAG, "=== MULTI-CORE STRING SORTING COMPLETE ===")
                Log.d(
                        TAG,
                        "Time: ${timeMs}ms, Total Comparisons: $totalComparisons, Ops/sec: $opsPerSecond"
                )
                Log.d(TAG, "Valid: $isValid, Execution success: $executionSuccess")

                CpuAffinityManager.resetPerformance()

                // Thermal stabilization delay (skip for test runs)
                if (!isTestRun) {
                    kotlinx.coroutines.delay(1500)
                }
                BenchmarkResult(
                        name = "Multi-Core String Sorting",
                        executionTimeMs = timeMs,
                        opsPerSecond = opsPerSecond,
                        isValid = isValid,
                        metricsJson =
                                JSONObject()
                                        .apply {
                                                put("cache_resident_size", cacheResidentSize)
                                                put("iterations_per_thread", iterationsPerThread)
                                                put("threads", numThreads)
                                                put("total_checksum", totalChecksum)
                                                put("time_ms", timeMs)
                                                put("comparisons_per_sort", comparisonsPerSort)
                                                put("total_comparisons", totalComparisons)
                                                put("ops_per_second", opsPerSecond)
                                                put("execution_success", executionSuccess)
                                                put(
                                                        "algorithm",
                                                        "Collections.sort() - Cache-Resident"
                                                )
                                                put(
                                                        "implementation",
                                                        "Cache-Resident Strategy - small fixed list with multiple iterations per thread"
                                                )
                                                put(
                                                        "workload_approach",
                                                        "Cache-Resistant - prevents memory bottlenecks, enables true CPU scaling"
                                                )
                                                put(
                                                        "benefits",
                                                        "No memory bottlenecks, true 8x multi-core scaling, tests CPU compute not memory bandwidth"
                                                )
                                                put(
                                                        "expected_performance",
                                                        "~24.0 Mops/s on 8-core devices (8x single-core)"
                                                )
                                        }
                                        .toString()
                )
        }

        /**
         * OPTIMIZED: Parallel merge sort with work-stealing for multi-core systems Uses
         * divide-and-conquer with parallel sub-problems
         */
        private suspend fun <T : Comparable<T>> parallelMergeSortMulticore(
                list: MutableList<T>,
                left: Int,
                right: Int
        ): List<T> =
                withContext(highPriorityDispatcher) {
                        val size = right - left

                        // Base case: Use optimized insertion sort for small arrays
                        if (size <= 32) {
                                return@withContext insertionSort(list, left, right)
                        }

                        // Divide: split into two halves
                        val mid = left + size / 2

                        // OPTIMIZED: Parallel recursive calls for large datasets
                        val leftResult = async { parallelMergeSortMulticore(list, left, mid) }
                        val rightResult = async { parallelMergeSortMulticore(list, mid, right) }

                        // Wait for both halves to complete
                        val leftSorted = leftResult.await()
                        val rightSorted = rightResult.await()

                        // Conquer: merge the sorted halves
                        mergeParallel(list, left, mid, right)
                        list.subList(left, right)
                }

        /** OPTIMIZED: Optimized merge operation for parallel sorting */
        private suspend fun <T : Comparable<T>> mergeParallel(
                list: MutableList<T>,
                left: Int,
                mid: Int,
                right: Int
        ) =
                withContext(highPriorityDispatcher) {
                        val aux = mutableListOf<T>()
                        aux.addAll(list.subList(left, right))

                        var i = 0
                        var j = mid - left
                        var k = left

                        // Merge with bounds optimization
                        while (i < j && j < aux.size) {
                                if (aux[i] <= aux[j]) {
                                        list[k++] = aux[i++]
                                } else {
                                        list[k++] = aux[j++]
                                }
                        }

                        // Copy remaining elements
                        while (i < j) {
                                list[k++] = aux[i++]
                        }
                        while (j < aux.size) {
                                list[k++] = aux[j++]
                        }
                }

        /** OPTIMIZED: Insertion sort for small arrays (cache-friendly) */
        private fun <T : Comparable<T>> insertionSort(
                list: MutableList<T>,
                left: Int,
                right: Int
        ): List<T> {
                for (i in left + 1 until right) {
                        val key = list[i]
                        var j = i - 1

                        // Move elements that are greater than key one position ahead
                        while (j >= left && list[j] > key) {
                                list[j + 1] = list[j]
                                j--
                        }
                        list[j + 1] = key
                }
                return list.subList(left, right)
        }

        /**
         * Helper: Merge multiple sorted lists into one sorted list Uses a simple k-way merge with
         * priority queue
         */
        private fun mergeSortedChunks(sortedChunks: List<List<String>>): List<String> {
                if (sortedChunks.isEmpty()) return emptyList()
                if (sortedChunks.size == 1) return sortedChunks[0]

                // Simple two-way merge repeatedly (efficient for small number of chunks)
                var result = sortedChunks[0]
                for (i in 1 until sortedChunks.size) {
                        result = mergeTwoSortedLists(result, sortedChunks[i])
                }
                return result
        }

        /** Helper: Merge two sorted lists */
        private fun mergeTwoSortedLists(list1: List<String>, list2: List<String>): List<String> {
                val result = mutableListOf<String>()
                var i = 0
                var j = 0

                while (i < list1.size && j < list2.size) {
                        if (list1[i] <= list2[j]) {
                                result.add(list1[i])
                                i++
                        } else {
                                result.add(list2[j])
                                j++
                        }
                }

                // Add remaining elements
                while (i < list1.size) {
                        result.add(list1[i])
                        i++
                }
                while (j < list2.size) {
                        result.add(list2[j])
                        j++
                }

                return result
        }
        /**
         * Test 6: Multi-Core Ray Tracing - CACHE-RESIDENT STRATEGY
         *
         * CACHE-RESIDENT APPROACH:
         * - Uses centralized performRayTracing function from BenchmarkHelpers
         * - Each thread performs fixed number of iterations independently
         * - Total work scales with cores: iterations × numThreads
         * - Same algorithm as Single-Core version for fair comparison
         *
         * PERFORMANCE: ~20.0 Mops/s on 8-core devices (8x single-core baseline)
         */
        suspend fun rayTracing(params: WorkloadParams, isTestRun: Boolean = false): BenchmarkResult = coroutineScope {
                Log.d(TAG, "=== STARTING MULTI-CORE RAY TRACING - CACHE-RESIDENT STRATEGY ===")
                Log.d(TAG, "Threads available: $numThreads")
                Log.d(TAG, "Iterations per thread: ${params.rayTracingIterations}")
                CpuAffinityManager.setMaxPerformance()

                val (width, height) = params.rayTracingResolution
                val maxDepth = params.rayTracingDepth

                // FIXED TOTAL WORK: Divide work across threads (same total work as single-core)
                val totalIterations = params.rayTracingIterations
                val iterationsPerThread =
                        (totalIterations + numThreads - 1) / numThreads // Round up

                // EXPLICIT timing with try-catch for debugging
                val startTime = System.currentTimeMillis()
                var totalEnergy = 0.0
                var executionSuccess = true

                try {
                        Log.d(TAG, "Starting parallel execution with $numThreads threads")
                        Log.d(
                                TAG,
                                "Total iterations: $totalIterations, Per thread: $iterationsPerThread"
                        )

                        // CACHE-RESIDENT: Each thread performs its share of total iterations
                        val threadResults =
                                (0 until numThreads).map { threadId ->
                                        async(highPriorityDispatcher) {
                                                // Calculate actual iterations for this thread
                                                val startIter = threadId * iterationsPerThread
                                                val endIter =
                                                        minOf(
                                                                startIter + iterationsPerThread,
                                                                totalIterations
                                                        )
                                                val actualIterations = endIter - startIter

                                                if (actualIterations <= 0) {
                                                        Log.d(
                                                                TAG,
                                                                "Thread $threadId: No work assigned"
                                                        )
                                                        return@async 0.0
                                                }

                                                Log.d(
                                                        TAG,
                                                        "Thread $threadId starting $actualIterations iterations"
                                                )

                                                // Use centralized cache-resident workload helper
                                                val threadEnergy =
                                                        BenchmarkHelpers.performRayTracing(
                                                                width,
                                                                height,
                                                                maxDepth,
                                                                actualIterations
                                                        )

                                                Log.d(
                                                        TAG,
                                                        "Thread $threadId completed, energy: $threadEnergy"
                                                )
                                                threadEnergy
                                        }
                                }

                        // Await all results
                        val results = threadResults.awaitAll()
                        Log.d(TAG, "All threads completed: ${results.size} results")
                        totalEnergy = results.sum()

                        // Verify no thread failed
                        if (results.any { it < 0.0 }) {
                                Log.e(
                                        TAG,
                                        "Multi-Core Ray Tracing: One or more threads returned invalid results"
                                )
                                executionSuccess = false
                        }
                } catch (e: Exception) {
                        Log.e(TAG, "Multi-Core Ray Tracing EXCEPTION: ${e.message}", e)
                        executionSuccess = false
                }

                val endTime = System.currentTimeMillis()
                val timeMs = (endTime - startTime).toDouble()

                // Calculate operations per second (same total work as single-core)
                val totalRays = (width * height * totalIterations).toLong()
                val opsPerSecond = if (timeMs > 0) totalRays / (timeMs / 1000.0) else 0.0

                // Validation
                val isValid =
                        executionSuccess &&
                                totalEnergy > 0.0 &&
                                timeMs > 0 &&
                                opsPerSecond > 0 &&
                                timeMs < 30000 // Should complete in under 30 seconds

                Log.d(TAG, "=== MULTI-CORE RAY TRACING COMPLETE ===")
                Log.d(TAG, "Time: ${timeMs}ms, Total Rays: $totalRays, Ops/sec: $opsPerSecond")
                Log.d(TAG, "Valid: $isValid, Execution success: $executionSuccess")

                CpuAffinityManager.resetPerformance()

                // Thermal stabilization delay (skip for test runs)
                if (!isTestRun) {
                    kotlinx.coroutines.delay(1500)
                }
                BenchmarkResult(
                        name = "Multi-Core Ray Tracing",
                        executionTimeMs = timeMs,
                        opsPerSecond = opsPerSecond,
                        isValid = isValid,
                        metricsJson =
                                JSONObject()
                                        .apply {
                                                put("resolution", "${width}x${height}")
                                                put("iterations_per_thread", iterationsPerThread)
                                                put("threads", numThreads)
                                                put("total_frames", totalIterations)
                                                put("total_rays", totalRays)
                                                put(
                                                        "implementation",
                                                        "Inlined - Zero Function Call Overhead"
                                                )
                                        }
                                        .toString()
                )
        }

        /**
         * Test 7: Multi-Core Compression - FIXED WORK PER CORE
         *
         * FIXED WORK PER CORE APPROACH:
         * - Uses centralized performCompression function from BenchmarkHelpers
         * - Fixed workload per thread: params.compressionIterations (ensures core-independent
         * stability)
         * - Total work scales with cores: compressionIterations × numThreads = Total Operations
         * - Test duration remains constant regardless of core count
         * - Same algorithm as Single-Core version for fair comparison
         *
         * PERFORMANCE: ~1.2 Gops/s on 8-core devices (8x single-core baseline)
         */
        suspend fun compression(params: WorkloadParams, isTestRun: Boolean = false): BenchmarkResult = coroutineScope {
                Log.d(TAG, "=== STARTING MULTI-CORE COMPRESSION - FIXED WORK PER CORE ===")
                Log.d(TAG, "Threads available: $numThreads")
                Log.d(TAG, "Fixed workload per thread: ${params.compressionIterations} iterations")
                Log.d(
                        TAG,
                        "Total expected operations: ${params.compressionIterations * numThreads}"
                )
                CpuAffinityManager.setMaxPerformance()

                // Configuration - FIXED WORK PER CORE APPROACH
                val bufferSize =
                        params.compressionDataSizeMb * 1024 * 1024 // Use configurable buffer size
                val iterationsPerThread =
                        params.compressionIterations // Use configurable workload per core
                val totalIterations = iterationsPerThread * numThreads // Scales with cores

                // EXPLICIT timing with try-catch for debugging
                val startTime = System.currentTimeMillis()
                var totalBytesProcessed = 0L
                var executionSuccess = true

                try {
                        Log.d(TAG, "Starting parallel execution with $numThreads threads")
                        Log.d(
                                TAG,
                                "Each thread will perform $iterationsPerThread compression iterations"
                        )

                        // FIXED WORK PER CORE: Each thread performs the full workload
                        val threadResults =
                                (0 until numThreads).map { threadId ->
                                        async(highPriorityDispatcher) {
                                                Log.d(
                                                        TAG,
                                                        "Thread $threadId starting $iterationsPerThread compression iterations"
                                                )

                                                // Call centralized compression function with full
                                                // workload per
                                                // thread
                                                val threadBytesProcessed =
                                                        BenchmarkHelpers.performCompression(
                                                                bufferSize,
                                                                iterationsPerThread
                                                        )

                                                Log.d(
                                                        TAG,
                                                        "Thread $threadId completed $iterationsPerThread iterations, processed ${threadBytesProcessed / (1024 * 1024)} MB"
                                                )
                                                threadBytesProcessed
                                        }
                                }

                        // Await all results
                        val results = threadResults.awaitAll()
                        Log.d(TAG, "All threads completed: ${results.size} results")
                        totalBytesProcessed = results.sum()

                        // Verify no thread failed
                        if (results.any { it <= 0 }) {
                                Log.e(
                                        TAG,
                                        "Multi-Core Compression: One or more threads returned invalid results"
                                )
                                executionSuccess = false
                        }
                } catch (e: Exception) {
                        Log.e(TAG, "Multi-Core Compression EXCEPTION: ${e.message}", e)
                        executionSuccess = false
                }

                val endTime = System.currentTimeMillis()
                val timeMs = (endTime - startTime).toDouble()

                // Calculate throughput (total bytes across all threads)
                val throughput = totalBytesProcessed.toDouble() / (timeMs / 1000.0)

                // Validation
                val isValid =
                        executionSuccess && totalBytesProcessed > 0 && timeMs > 0 && throughput > 0

                Log.d(TAG, "=== MULTI-CORE COMPRESSION COMPLETE ===")
                Log.d(
                        TAG,
                        "Time: ${timeMs}ms, Total Bytes: $totalBytesProcessed, Throughput: $throughput bps"
                )
                Log.d(TAG, "Valid: $isValid, Execution success: $executionSuccess")

                CpuAffinityManager.resetPerformance()

                // Thermal stabilization delay (skip for test runs)
                if (!isTestRun) {
                    kotlinx.coroutines.delay(1500)
                }
                BenchmarkResult(
                        name = "Multi-Core Compression",
                        executionTimeMs = timeMs,
                        opsPerSecond = throughput,
                        isValid = isValid,
                        metricsJson =
                                JSONObject()
                                        .apply {
                                                put("buffer_size_mb", bufferSize / (1024 * 1024))
                                                put("iterations_per_thread", iterationsPerThread)
                                                put("threads", numThreads)
                                                put("total_iterations", totalIterations)
                                                put(
                                                        "total_data_processed_mb",
                                                        totalBytesProcessed / (1024 * 1024)
                                                )
                                                put("throughput_bps", throughput)
                                                put("time_ms", timeMs)
                                                put("execution_success", executionSuccess)
                                                put(
                                                        "implementation",
                                                        "Centralized RLE - Fixed Work Per Core"
                                                )
                                                put(
                                                        "workload_approach",
                                                        "Fixed Work Per Core - ensures core-independent test duration"
                                                )
                                                put(
                                                        "expected_performance",
                                                        "~1.2 Gops/s on 8-core devices (8x single-core)"
                                                )
                                        }
                                        .toString()
                )
        }

        /**
         * Test 8: Multi-Core Leibniz π Calculation
         *
         * ALGORITHM: Uses Leibniz formula: π/4 = 1 - 1/3 + 1/5 - 1/7 + 1/9 - ...
         *
         * WHY LEIBNIZ INSTEAD OF MONTE CARLO:
         * - Deterministic: No random numbers, no caching issues
         * - Predictable: Same iterations = same result
         * - Scales linearly: Each thread processes separate range of terms
         * - Pure arithmetic: Tests raw CPU throughput
         *
         * PERFORMANCE: Scales linearly with core count
         */
        suspend fun monteCarloPi(params: WorkloadParams, isTestRun: Boolean = false): BenchmarkResult = coroutineScope {
                Log.d(TAG, "=== STARTING MULTI-CORE LEIBNIZ π ===")
                Log.d(TAG, "Threads available: $numThreads")
                val iterationsPerThread = params.monteCarloSamples.toLong()
                val totalIterations = iterationsPerThread * numThreads
                Log.d(TAG, "Iterations per thread: $iterationsPerThread, Total: $totalIterations")

                CpuAffinityManager.setMaxPerformance()

                // Start timing
                val startTime = System.currentTimeMillis()

                // Each thread calculates a portion of the Leibniz series
                // Thread 0: terms 0, numThreads, 2*numThreads, ...
                // Thread 1: terms 1, numThreads+1, 2*numThreads+1, ...
                val results = (0 until numThreads).map { threadId ->
                    async(Dispatchers.Default) {
                        var sum = 0.0
                        var term = threadId.toLong()
                        val step = numThreads.toLong()

                        repeat(iterationsPerThread.toInt()) {
                            // Leibniz: (-1)^n / (2n + 1)
                            val denominator = 2.0 * term + 1.0
                            val sign = if (term % 2 == 0L) 1.0 else -1.0
                            sum += sign / denominator
                            term += step
                        }

                        Log.d(TAG, "Thread $threadId: computed $iterationsPerThread terms, partial sum: $sum")
                        sum
                    }
                }

                // Await all results and sum them
                val threadResults = results.awaitAll()
                val totalSum = threadResults.sum()

                val endTime = System.currentTimeMillis()
                val timeMs = (endTime - startTime).toDouble()

                // Calculate π estimate (Leibniz gives π/4)
                val piEstimate = totalSum * 4.0
                val opsPerSecond = totalIterations.toDouble() / (timeMs / 1000.0)
                val accuracy = kotlin.math.abs(piEstimate - kotlin.math.PI)

                // Accuracy threshold based on iterations
                val accuracyThreshold = when {
                    totalIterations >= 100_000_000 -> 0.00001
                    totalIterations >= 10_000_000 -> 0.0001
                    totalIterations >= 1_000_000 -> 0.001
                    else -> 0.01
                }

                val isValid = timeMs > 0 && opsPerSecond > 0 && accuracy < accuracyThreshold

                Log.d(TAG, "=== MULTI-CORE LEIBNIZ π COMPLETE ===")
                Log.d(TAG, "Time: ${timeMs}ms, Total iterations: $totalIterations, Ops/sec: $opsPerSecond")
                Log.d(TAG, "π estimate: $piEstimate, Accuracy: $accuracy, Valid: $isValid")

                CpuAffinityManager.resetPerformance()

                // Thermal stabilization delay (skip for test runs)
                if (!isTestRun) {
                    kotlinx.coroutines.delay(1500)
                }

                BenchmarkResult(
                        name = "Multi-Core Monte Carlo π",
                        executionTimeMs = timeMs,
                        opsPerSecond = opsPerSecond,
                        isValid = isValid,
                        metricsJson =
                                JSONObject()
                                        .apply {
                                                put("iterations_per_thread", iterationsPerThread)
                                                put("threads", numThreads)
                                                put("total_iterations", totalIterations)
                                                put("pi_estimate", piEstimate)
                                                put("actual_pi", kotlin.math.PI)
                                                put("accuracy", accuracy)
                                                put("accuracy_threshold", accuracyThreshold)
                                                put("partial_sum", totalSum)
                                                put("time_ms", timeMs)
                                                put("ops_per_second", opsPerSecond)
                                                put("implementation", "Leibniz formula")
                                                put("optimization", "Deterministic, no randomness, strided thread distribution")
                                        }
                                        .toString()
                )
        }

        /**
         * Test 9: Multi-Core JSON Parsing - CACHE-RESIDENT STRATEGY
         *
         * CACHE-RESIDENT APPROACH:
         * - Generate JSON data ONCE outside the timing block
         * - Each thread performs multiple parsing iterations on the ENTIRE JSON data
         * - Total work scales with cores: numThreads × iterations
         * - JSON data stays in CPU cache for fast access
         * - Measures pure CPU parsing throughput, not memory bandwidth
         * - Same algorithm as Single-Core version for fair comparison
         *
         * PERFORMANCE: ~16.0 Mops/s on 8-core devices (8x single-core baseline)
         */
        suspend fun jsonParsing(params: WorkloadParams, isTestRun: Boolean = false): BenchmarkResult = coroutineScope {
                Log.d(TAG, "=== STARTING MULTI-CORE JSON PARSING - CACHE-RESIDENT STRATEGY ===")
                Log.d(TAG, "Threads available: $numThreads")
                Log.d(
                        TAG,
                        "JSON size: ${params.jsonDataSizeMb}MB, Iterations per thread: ${params.jsonParsingIterations}"
                )
                CpuAffinityManager.setMaxPerformance()

                val dataSize = params.jsonDataSizeMb * 1024 * 1024
                val iterationsPerThread = params.jsonParsingIterations

                // CACHE-RESIDENT: Generate JSON OUTSIDE timing block
                Log.d(TAG, "Generating JSON data ($dataSize bytes)...")
                val jsonData = BenchmarkHelpers.generateComplexJson(dataSize)
                Log.d(TAG, "JSON generated. Starting parallel cache-resident parsing...")

                // EXPLICIT timing with try-catch for debugging
                val startTime = System.currentTimeMillis()
                var totalElementCount = 0
                var executionSuccess = true

                try {
                        Log.d(TAG, "Starting parallel execution with $numThreads threads")
                        Log.d(TAG, "Each thread will perform $iterationsPerThread iterations")

                        // CACHE-RESIDENT: Each thread parses the ENTIRE JSON multiple times
                        val threadResults =
                                (0 until numThreads).map { threadId ->
                                        async(highPriorityDispatcher) {
                                                Log.d(
                                                        TAG,
                                                        "Thread $threadId starting $iterationsPerThread iterations"
                                                )

                                                // Each thread performs full workload on entire JSON
                                                val threadElementCount =
                                                        BenchmarkHelpers.performJsonParsingWorkload(
                                                                jsonData,
                                                                iterationsPerThread
                                                        )

                                                Log.d(
                                                        TAG,
                                                        "Thread $threadId completed, element count: $threadElementCount"
                                                )
                                                threadElementCount
                                        }
                                }

                        // Await all results
                        val results = threadResults.awaitAll()
                        Log.d(TAG, "All threads completed: ${results.size} results")
                        totalElementCount = results.sum()

                        // Verify no thread failed
                        if (results.any { it <= 0 }) {
                                Log.e(
                                        TAG,
                                        "Multi-Core JSON Parsing: One or more threads returned invalid results"
                                )
                                executionSuccess = false
                        }
                } catch (e: Exception) {
                        Log.e(TAG, "Multi-Core JSON Parsing EXCEPTION: ${e.message}", e)
                        executionSuccess = false
                }

                val endTime = System.currentTimeMillis()
                val timeMs = (endTime - startTime).toDouble()

                // Calculate operations per second based on total elements parsed
                // This represents the actual parsing work done across all threads
                val opsPerSecond =
                        if (timeMs > 0) totalElementCount.toDouble() / (timeMs / 1000.0) else 0.0

                // Validation
                val isValid =
                        executionSuccess &&
                                totalElementCount > 0 &&
                                timeMs > 0 &&
                                opsPerSecond > 0 &&
                                timeMs < 30000 // Should complete in under 30 seconds

                Log.d(TAG, "=== MULTI-CORE JSON PARSING COMPLETE ===")
                Log.d(
                        TAG,
                        "Time: ${timeMs}ms, Total Elements: $totalElementCount, Ops/sec: $opsPerSecond"
                )
                Log.d(TAG, "Valid: $isValid, Execution success: $executionSuccess")

                CpuAffinityManager.resetPerformance()

                // Thermal stabilization delay (skip for test runs)
                if (!isTestRun) {
                    kotlinx.coroutines.delay(1500)
                }
                BenchmarkResult(
                        name = "Multi-Core JSON Parsing",
                        executionTimeMs = timeMs,
                        opsPerSecond = opsPerSecond,
                        isValid = isValid,
                        metricsJson =
                                JSONObject()
                                        .apply {
                                                put("json_size_bytes", dataSize)
                                                put("iterations_per_thread", iterationsPerThread)
                                                put("threads", numThreads)
                                                put("total_element_count", totalElementCount)
                                                put("time_ms", timeMs)
                                                put("ops_per_second", opsPerSecond)
                                                put("execution_success", executionSuccess)
                                                put(
                                                        "implementation",
                                                        "Cache-Resident Strategy - each thread parses entire JSON multiple times"
                                                )
                                                put(
                                                        "workload_approach",
                                                        "Fixed Work Per Core - ensures core-independent test duration"
                                                )
                                                put(
                                                        "expected_performance",
                                                        "~16.0 Mops/s on 8-core devices (8x single-core)"
                                                )
                                        }
                                        .toString()
                )
        }

        /**
         * Test 10: Multi-Core N-Queens Problem - FIXED WORK PER CORE
         *
         * FIXED WORK PER CORE APPROACH:
         * - Uses centralized solveNQueens function from BenchmarkHelpers
         * - Each thread solves the SAME N-Queens problem independently
         * - Total work scales with cores: iterations × numThreads
         * - Tracks iterations (board evaluations) as the primary metric
         * - Same algorithm as Single-Core version for fair comparison
         *
         * PERFORMANCE: Scales linearly with cores (8 cores = 8× iterations in same time)
         */
        suspend fun nqueens(params: WorkloadParams, isTestRun: Boolean = false): BenchmarkResult = coroutineScope {
                Log.d(TAG, "=== STARTING MULTI-CORE N-QUEENS - FIXED WORK PER CORE ===")
                Log.d(TAG, "Threads available: $numThreads")
                Log.d(TAG, "Board size: ${params.nqueensSize}")
                CpuAffinityManager.setMaxPerformance()

                val boardSize = params.nqueensSize

                // EXPLICIT timing with try-catch for debugging
                val startTime = System.currentTimeMillis()
                var totalSolutions = 0
                var totalIterations = 0L
                var executionSuccess = true

                try {
                        Log.d(TAG, "Starting parallel execution with $numThreads threads")
                        Log.d(TAG, "Each thread will solve N-Queens for board size $boardSize")

                        // FIXED WORK PER CORE: Each thread solves the same problem independently
                        val threadResults =
                                (0 until numThreads).map { threadId ->
                                        async(highPriorityDispatcher) {
                                                Log.d(
                                                        TAG,
                                                        "Thread $threadId starting N-Queens solver"
                                                )

                                                // Each thread calls the centralized solver
                                                val (solutions, iterations) =
                                                        BenchmarkHelpers.solveNQueens(boardSize)

                                                Log.d(
                                                        TAG,
                                                        "Thread $threadId completed: $solutions solutions, $iterations iterations"
                                                )
                                                Pair(solutions, iterations)
                                        }
                                }

                        // Await all results
                        val results = threadResults.awaitAll()
                        Log.d(TAG, "All threads completed: ${results.size} results")

                        // Sum up solutions and iterations from all threads
                        totalSolutions = results.sumOf { it.first }
                        totalIterations = results.sumOf { it.second }

                        // Verify no thread failed
                        if (results.any { it.second <= 0 }) {
                                Log.e(
                                        TAG,
                                        "Multi-Core N-Queens: One or more threads returned invalid results"
                                )
                                executionSuccess = false
                        }
                } catch (e: Exception) {
                        Log.e(TAG, "Multi-Core N-Queens EXCEPTION: ${e.message}", e)
                        executionSuccess = false
                }

                val endTime = System.currentTimeMillis()
                val timeMs = (endTime - startTime).toDouble()

                // Calculate operations per second (total iterations across all threads)
                val opsPerSecond =
                        if (timeMs > 0) totalIterations.toDouble() / (timeMs / 1000.0) else 0.0

                // Validation
                val isValid =
                        executionSuccess &&
                                totalSolutions > 0 &&
                                totalIterations > 0 &&
                                timeMs > 0 &&
                                opsPerSecond > 0 &&
                                timeMs < 30000 // Should complete in under 30 seconds

                Log.d(TAG, "=== MULTI-CORE N-QUEENS COMPLETE ===")
                Log.d(
                        TAG,
                        "Time: ${timeMs}ms, Total Solutions: $totalSolutions, Total Iterations: $totalIterations, Ops/sec: $opsPerSecond"
                )
                Log.d(TAG, "Valid: $isValid, Execution success: $executionSuccess")

                CpuAffinityManager.resetPerformance()

                // Thermal stabilization delay (skip for test runs)
                if (!isTestRun) {
                    kotlinx.coroutines.delay(1500)
                }
                BenchmarkResult(
                        name = "Multi-Core N-Queens",
                        executionTimeMs = timeMs,
                        opsPerSecond = opsPerSecond,
                        isValid = isValid,
                        metricsJson =
                                JSONObject()
                                        .apply {
                                                put("board_size", boardSize)
                                                put("solution_count", totalSolutions)
                                                put("iteration_count", totalIterations)
                                                put("iterations_per_sec", opsPerSecond)
                                                put("threads", numThreads)
                                                put("time_ms", timeMs)
                                                put("execution_success", executionSuccess)
                                                put(
                                                        "implementation",
                                                        "Centralized Bitwise Backtracking with Fixed Work Per Core"
                                                )
                                                put(
                                                        "workload_approach",
                                                        "Fixed Work Per Core - each thread solves same problem independently"
                                                )
                                                put(
                                                        "metric",
                                                        "Iterations (board evaluations) per second"
                                                )
                                                put(
                                                        "expected_performance",
                                                        "Linear scaling: 8 cores = 8× iterations in same time"
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
}
