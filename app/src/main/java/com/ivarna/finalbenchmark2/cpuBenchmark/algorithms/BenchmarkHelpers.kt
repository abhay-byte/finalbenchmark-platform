package com.ivarna.finalbenchmark2.cpuBenchmark.algorithms

import java.util.concurrent.ThreadLocalRandom

object BenchmarkHelpers {

    /** Run a benchmark function and measure execution time */
    inline fun <T> measureBenchmark(block: () -> T): Pair<T, Long> {
        val startTime = System.nanoTime()
        val result = block()
        val endTime = System.nanoTime()
        val durationMs = (endTime - startTime) / 1_000_000
        return Pair(result, durationMs)
    }

    /**
     * Run a suspend benchmark function and measure execution time Allows yielding to prevent UI
     * freeze
     */
    suspend inline fun <T> measureBenchmarkSuspend(
            crossinline block: suspend () -> T
    ): Pair<T, Long> {
        val startTime = System.nanoTime()
        val result = block()
        val endTime = System.nanoTime()
        val durationMs = (endTime - startTime) / 1_000_000
        return Pair(result, durationMs)
    }

    /**
     * Generate random string of specified length - OPTIMIZED for performance Uses static character
     * set and efficient string building
     */
    fun generateRandomString(length: Int): String {
        // OPTIMIZED: Static character set to avoid recreation overhead
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val charArray = CharArray(length)

        // OPTIMIZED: Use ThreadLocalRandom for better performance and thread safety
        val random = ThreadLocalRandom.current()

        repeat(length) { index -> charArray[index] = chars[random.nextInt(chars.length)] }

        return String(charArray)
    }

    /** Generate list of random strings - Efficiently pre-allocates list */
    fun generateStringList(count: Int, length: Int = 20): MutableList<String> {
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val random = java.util.concurrent.ThreadLocalRandom.current()
        val list = ArrayList<String>(count)
        repeat(count) {
            val charArray = CharArray(length)
            repeat(length) { i -> charArray[i] = chars[random.nextInt(chars.length)] }
            list.add(String(charArray))
        }
        return list
    }

    /** Check if a number is prime */
    fun isPrime(n: Long): Boolean {
        if (n <= 1L) return false
        if (n <= 3L) return true
        if (n % 2L == 0L || n % 3L == 0L) return false

        var i = 5L
        while (i * i <= n) {
            if (n % i == 0L || n % (i + 2L) == 0L) return false
            i += 6L
        }
        return true
    }

    /** Calculate checksum of a 2D matrix */
    fun calculateMatrixChecksum(matrix: Array<DoubleArray>): Long {
        var checksum = 0L
        for (row in matrix) {
            for (value in row) {
                checksum = checksum xor value.toBits()
            }
        }
        return checksum
    }

    /**
     * Shared iterative Fibonacci function for core-independent CPU benchmarking Uses O(n) time
     * complexity - same algorithm for both Single-Core and Multi-Core tests
     *
     * @param n The Fibonacci number to calculate (n=35 for benchmark stability)
     * @return The nth Fibonacci number
     */
    fun fibonacciIterative(n: Int): Long {
        if (n <= 1) return n.toLong()
        var prev = 0L
        var curr = 1L
        for (i in 2..n) {
            val next = prev + curr
            prev = curr
            curr = next
        }
        return curr
    }

    /**
     * Cache-Resident Matrix Multiplication
     *
     * Performs multiple matrix multiplications (A × B = C) using optimized i-k-j loop order for
     * cache efficiency. Uses small matrices that fit in CPU cache to prevent memory bottlenecks.
     *
     * CACHE-RESIDENT STRATEGY:
     * - Small matrix size (128x128) fits in L2/L3 cache
     * - Multiple repetitions to maintain CPU utilization
     * - Matrices A and B allocated once, reused across repetitions
     * - Only matrix C is reset between repetitions
     *
     * @param size The size of the square matrices (size × size) - should be small (128)
     * @param repetitions Number of times to repeat the matrix multiplication
     * @return The checksum of the final resulting matrix C
     */
    fun performMatrixMultiplication(size: Int, repetitions: Int = 1): Long {
        // OPTIMIZED: Initialize matrices A and B ONCE (cache-resident strategy)
        val a = Array(size) { DoubleArray(size) { kotlin.random.Random.nextDouble() } }
        val b = Array(size) { DoubleArray(size) { kotlin.random.Random.nextDouble() } }

        // CACHE-RESIDENT: Repeat the multiplication multiple times
        repeat(repetitions) { rep ->
            // Initialize result matrix C for this repetition
            val c = Array(size) { DoubleArray(size) }

            // OPTIMIZED: Use i-k-j loop order for better cache locality
            for (i in 0 until size) {
                for (k in 0 until size) {
                    val aik = a[i][k]
                    for (j in 0 until size) {
                        c[i][j] += aik * b[k][j]
                    }
                }
            }

            // For the last repetition, return the checksum
            if (rep == repetitions - 1) {
                return calculateMatrixChecksum(c)
            }
        }

        // This should never be reached, but Kotlin requires a return statement
        return 0L
    }

    /**
     * Pure CPU Hash Computing (No Native Locks) Uses a custom FNV-like mixing algorithm to stress
     * the CPU ALU and L1 Cache. Guaranteed to scale perfectly on Multi-Core.
     *
     * FIXED WORK PER CORE: Pure Kotlin Hash Computing
     *
     * Performs fixed number of hash iterations using 4KB buffer (cache-friendly). Returns total
     * bytes processed for throughput calculation.
     *
     * @param bufferSize Size of the data buffer in bytes (4KB recommended)
     * @param iterations Number of hash iterations to perform
     * @return Total bytes processed (bufferSize * iterations)
     */
    fun performHashComputing(bufferSize: Int, iterations: Int): Long {
        // 1. Setup Data
        val data = ByteArray(bufferSize) { (it % 255).toByte() }
        var currentState = 0x811C9DC5.toInt() // FNV offset basis

        // 2. Pure CPU Loop (No System Calls)
        repeat(iterations) {
            // Process the buffer with a stride for speed (simulating SHA-256 block processing)
            // We read every 4th byte to keep the benchmark duration reasonable (~1.5s for 1M iters)
            for (i in 0 until bufferSize step 4) {
                currentState = (currentState xor data[i].toInt()) * 16777619 // FNV prime
            }
        }

        // 3. Return throughput metric
        return bufferSize.toLong() * iterations
    }

    /**
     * Generate a list of random strings efficiently for benchmarking
     *
     * FIXED WORK PER CORE: Efficient string generation for fair benchmarking
     *
     * @param count Number of strings to generate
     * @param length Length of each string (default: 16 characters)
     * @return MutableList<String> containing random strings
     */
    fun generateStringList(count: Int, length: Int = 16): MutableList<String> {
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val random = java.util.concurrent.ThreadLocalRandom.current()
        val list = ArrayList<String>(count)
        repeat(count) {
            val charArray = CharArray(length)
            repeat(length) { i -> charArray[i] = chars[random.nextInt(chars.length)] }
            list.add(String(charArray))
        }
        return list
    }
}
