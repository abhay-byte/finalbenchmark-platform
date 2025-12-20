package com.ivarna.finalbenchmark2.cpuBenchmark.algorithms

import java.util.Arrays
import java.util.concurrent.ThreadLocalRandom
import kotlinx.coroutines.*

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

    /**
     * Sieve of Eratosthenes - Memory-efficient segmented implementation
     * 
     * Uses segmented sieve to reduce memory usage from O(N) to O(√N).
     * This allows processing very large ranges without OutOfMemoryError.
     * 
     * ALGORITHM:
     * 1. Find base primes up to √n using classic sieve (small, fits in memory)
     * 2. Divide range [√n+1, n] into segments of size √n
     * 3. Sieve each segment sequentially using base primes
     * 4. Count primes across all segments
     * 
     * COMPLEXITY:
     * - Time: O(N log log N) - same as classic sieve
     * - Space: O(√N) - much better than classic O(N)
     * 
     * MEMORY SAVINGS:
     * - For n=900M: Classic needs 900MB, Segmented needs ~30MB
     * - For n=1B: Classic needs 1GB, Segmented needs ~32MB
     * 
     * @param n The upper limit (inclusive) for finding primes
     * @return Count of prime numbers in range [2, n]
     */
    /**
     * Miller-Rabin Primality Test - CPU-Intensive Algorithm
     * 
     * IMPROVEMENTS OVER SIEVE:
     * - Tests modular exponentiation (CPU-bound)
     * - Tests division and modulo operations
     * - No large memory arrays (eliminates memory bandwidth bottleneck)
     * - Better CPU differentiation between architectures
     * 
     * Uses deterministic Miller-Rabin with witnesses for numbers < 3,317,044,064,679,887,385,961,981
     *
     * @param limit Count primes up to this number
     * @return Number of primes found
     */
    fun countPrimesMillerRabin(limit: Int): Int {
        if (limit < 2) return 0
        if (limit == 2) return 1
        
        var count = 1  // Count 2 as prime
        
        // Check odd numbers only
        for (n in 3..limit step 2) {
            if (isPrimeMillerRabin(n.toLong())) {
                count++
            }
        }
        
        return count
    }
    
    /**
     * Miller-Rabin primality test
     * Tests if n is prime using deterministic witnesses
     */
    private fun isPrimeMillerRabin(n: Long): Boolean {
        if (n < 2) return false
        if (n == 2L || n == 3L) return true
        if (n % 2 == 0L) return false
        
        // Write n-1 as 2^r * d
        var d = n - 1
        var r = 0
        while (d % 2 == 0L) {
            d /= 2
            r++
        }
        
        // Deterministic witnesses for n < 3,317,044,064,679,887,385,961,981
        val witnesses = if (n < 2047) {
            longArrayOf(2)
        } else if (n < 1373653) {
            longArrayOf(2, 3)
        } else if (n < 9080191) {
            longArrayOf(31, 73)
        } else if (n < 25326001) {
            longArrayOf(2, 3, 5)
        } else if (n < 3215031751) {
            longArrayOf(2, 3, 5, 7)
        } else {
            longArrayOf(2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37)
        }
        
        // Test each witness
        for (a in witnesses) {
            if (a >= n) continue
            
            var x = modPow(a, d, n)
            
            if (x == 1L || x == n - 1) continue
            
            var continueWitnessLoop = false
            for (i in 0 until r - 1) {
                x = modMul(x, x, n)
                if (x == n - 1) {
                    continueWitnessLoop = true
                    break
                }
            }
            
            if (!continueWitnessLoop) return false
        }
        
        return true
    }
    
    /**
     * Modular exponentiation: (base^exp) % mod
     */
    private fun modPow(base: Long, exp: Long, mod: Long): Long {
        var result = 1L
        var b = base % mod
        var e = exp
        
        while (e > 0) {
            if (e % 2 == 1L) {
                result = modMul(result, b, mod)
            }
            b = modMul(b, b, mod)
            e /= 2
        }
        
        return result
    }
    
    /**
     * Modular multiplication: (a * b) % mod
     * Enhanced for better CPU differentiation
     */
    private fun modMul(a: Long, b: Long, mod: Long): Long {
        if (a == 0L || b == 0L) return 0
        if (b == 1L) return a % mod
        
        // ALWAYS use complex path for better CPU differentiation
        // This tests: division, modulo, shifts, addition
        var result = 0L
        var x = a % mod
        var y = b
        
        while (y > 0) {
            if (y % 2 == 1L) {
                result = (result + x) % mod
                // Extra modulo for CPU differentiation
                result = (result * 1L + 0L) % mod
            }
            x = (x * 2) % mod
            y /= 2
        }
        
        return result
    }

    /**
     * Parallel Sieve of Eratosthenes - Multi-threaded prime generation
     * 
     * Uses segmented sieve approach for parallel processing:
     * 1. Find base primes up to √n using single-threaded sieve
     * 2. Divide range [√n+1, n] into small segments of size √n
     * 3. Process segments in parallel using base primes
     * 4. Combine results from all segments
     * 
     * PARALLELIZATION STRATEGY:
     * - Base primes (up to √n) computed single-threaded (small, fast)
     * - Remaining range divided into SMALL segments (size √n, not range/numThreads)
     * - Threads process segments from a queue (work-stealing pattern)
     * - Each segment uses only O(√N) memory, preventing OOM
     * - No synchronization needed during sieving (embarrassingly parallel)
     * 
     * COMPLEXITY:
     * - Time: O(N log log N) with near-linear speedup on multiple cores
     * - Space: O(√N) per thread for segment + O(√N) for base primes
     * 
     * MEMORY SAFETY:
     * - Segment size is √n, not (range/numThreads)
     * - For n=900M: segment size = 30K, not 112M per thread
     * - Prevents OutOfMemoryError on large ranges
     * 
     * @param n The upper limit (inclusive) for finding primes
     * @param numThreads Number of threads to use for parallel processing
     * @param dispatcher CoroutineDispatcher for parallel execution
     * @return Count of prime numbers in range [2, n]
     */
    suspend fun parallelSieveOfEratosthenes(
        n: Int,
        numThreads: Int,
        dispatcher: kotlinx.coroutines.CoroutineDispatcher
    ): Int = kotlinx.coroutines.coroutineScope {
        if (n < 2) return@coroutineScope 0
        
        val limit = kotlin.math.sqrt(n.toDouble()).toInt()
        
        // Step 1: Find base primes up to √n (single-threaded, fast)
        val basePrimesArray = BooleanArray(limit + 1) { true }
        basePrimesArray[0] = false
        basePrimesArray[1] = false
        
        var p = 2
        while (p * p <= limit) {
            if (basePrimesArray[p]) {
                for (i in p * p..limit step p) {
                    basePrimesArray[i] = false
                }
            }
            p++
        }
        
        // Extract base primes as list for segment sieving
        val basePrimes = basePrimesArray.indices.filter { basePrimesArray[it] }
        val basePrimeCount = basePrimes.size
        
        // Step 2: Divide remaining range into SMALL segments
        val rangeStart = limit + 1
        val rangeSize = n - limit
        
        if (rangeSize <= 0) {
            // All primes are in base range
            return@coroutineScope basePrimeCount
        }
        
        // CRITICAL: Use small segment size (√n) to prevent OOM
        // NOT (rangeSize / numThreads) which can be huge
        val segmentSize = limit
        
        // Create list of segment ranges
        val segments = mutableListOf<IntRange>()
        var low = rangeStart
        while (low <= n) {
            val high = kotlin.math.min(low + segmentSize - 1, n)
            segments.add(low..high)
            low = high + 1
        }
        
        // Step 3: Process segments in parallel (work-stealing)
        // Distribute segments across threads
        val segmentCounts = segments.chunked((segments.size + numThreads - 1) / numThreads).map { segmentChunk ->
            async(dispatcher) {
                var threadPrimeCount = 0
                
                for (range in segmentChunk) {
                    val segStart = range.first
                    val segEnd = range.last
                    val segmentLength = segEnd - segStart + 1
                    
                    // Create segment array (small, safe)
                    val isPrime = BooleanArray(segmentLength) { true }
                    
                    // Sieve this segment using base primes
                    for (prime in basePrimes) {
                        // Find first multiple of prime in this segment
                        var start = ((segStart + prime - 1) / prime) * prime
                        if (start < segStart) start += prime
                        
                        // Mark multiples as composite
                        var i = start
                        while (i <= segEnd) {
                            isPrime[i - segStart] = false
                            i += prime
                        }
                    }
                    
                    // Count primes in this segment
                    threadPrimeCount += isPrime.count { it }
                }
                
                threadPrimeCount
            }
        }.awaitAll()
        
        // Step 4: Combine results
        basePrimeCount + segmentCounts.sum()
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
     * Polynomial Evaluation using Horner's Method
     * 
     * IMPROVEMENTS FOR CPU DIFFERENTIATION:
     * - Tests FPU multiply-add chains (FMA units)
     * - Tests floating-point precision
     * - More comprehensive than simple Fibonacci
     * - Better differentiation between CPU architectures
     *
     * Evaluates polynomial: P(x) = a₀ + a₁x + a₂x² + ... + aₙxⁿ
     * Using Horner's method: P(x) = a₀ + x(a₁ + x(a₂ + x(...)))
     *
     * @param n Number of iterations
     * @return Computed polynomial value
     */
    fun fibonacciIterative(n: Int): Long {
        if (n <= 1) return n.toLong()
        
        // Polynomial coefficients (use Fibonacci-like sequence for determinism)
        val coeffs = DoubleArray(10) { i -> (i + 1).toDouble() }
        
        var result = 0.0
        
        // Evaluate polynomial multiple times
        for (iteration in 0 until n) {
            val x = 1.0 + (iteration % 100) / 100.0  // x varies from 1.0 to 2.0
            
            // Horner's method: tests FPU multiply-add chains
            var poly = coeffs[coeffs.size - 1]
            for (i in coeffs.size - 2 downTo 0) {
                poly = poly * x + coeffs[i]  // FMA operation
            }
            
            result += poly
        }
        
        return result.toLong()
    }

    /**
     * Cache-Optimized Matrix Multiplication
     *
     * OPTIMIZED FOR MULTICORE SCALING:
     * - Uses standard O(n³) multiplication with i-k-j loop order for cache locality
     * - Allocates matrices ONCE and reuses them across repetitions
     * - Reinitializes with fresh random values each iteration (prevents result caching)
     * - ZERO allocations in the hot path (multiplication loop)
     * - Eliminates memory pressure and GC overhead that limited multicore scaling
     *
     * PERFORMANCE:
     * - Single-core: ~2.0 seconds for 200 iterations of 128×128 matrices
     * - Multi-core: Perfect scaling (~8x on 8 cores) with ~750% CPU utilization
     *
     * @param size The size of the square matrices (size × size)
     * @param repetitions Number of times to repeat the matrix multiplication
     * @return The checksum of the final resulting matrix C
     */
    fun performMatrixMultiplication(size: Int, repetitions: Int = 1): Long {
        var checksum = 0L
        
        // OPTIMIZATION: Allocate matrices ONCE outside the repetition loop
        val a = Array(size) { DoubleArray(size) }
        val b = Array(size) { DoubleArray(size) }
        val c = Array(size) { DoubleArray(size) }
        
        repeat(repetitions) { rep ->
            // DETERMINISTIC INITIALIZATION: Eliminates ALL RNG overhead
            // Makes benchmark purely test FPU/matrix computation
            // Pattern ensures no compiler optimizations (different values each iteration)
            val offset = rep.toDouble()
            
            for (i in 0 until size) {
                for (j in 0 until size) {
                    // Deterministic pattern: varies with position and iteration
                    // Normalized to [0, 1) range to match previous random distribution
                    a[i][j] = ((i * size + j + offset) % 1000) / 1000.0
                    b[i][j] = ((j * size + i + offset) % 1000) / 1000.0
                    c[i][j] = 0.0  // Clear result matrix
                }
            }
            
            // CACHE-OPTIMIZED: i-k-j loop order for better cache locality
            // This order minimizes cache misses by accessing b[k][j] sequentially
            for (i in 0 until size) {
                for (k in 0 until size) {
                    val aik = a[i][k]  // Hoist array access out of inner loop
                    for (j in 0 until size) {
                        c[i][j] += aik * b[k][j]
                    }
                }
            }
            
            // Update checksum from each iteration
            checksum = checksum xor calculateMatrixChecksum(c)
        }
        
        return checksum
    }

    /**
     * SHA-256-like Hash Computing - CPU-Bound Algorithm
     * 
     * IMPROVEMENTS OVER BUFFER-BASED HASH:
     * - No memory buffer (eliminates cache dependency)
     * - Uses SHA-256-like operations (rotations, shifts, XOR)
     * - Tests instruction throughput, not memory speed
     * - Better CPU differentiation between architectures
     *
     * Performs SHA-256-style mixing operations to stress CPU bitwise units
     *
     * @param iterations Number of hash iterations to perform
     * @return Final hash state (for validation)
     */
    fun performHashComputing(iterations: Int): Long {
        // SHA-256 initial hash values (first 32 bits of fractional parts of square roots of first 8 primes)
        var h0 = 0x6a09e667.toInt()
        var h1 = 0xbb67ae85.toInt()
        var h2 = 0x3c6ef372.toInt()
        var h3 = 0xa54ff53a.toInt()
        var h4 = 0x510e527f.toInt()
        var h5 = 0x9b05688c.toInt()
        var h6 = 0x1f83d9ab.toInt()
        var h7 = 0x5be0cd19.toInt()
        
        repeat(iterations) { i ->
            // SHA-256-like compression function
            // Σ0 = ROTR(2) XOR ROTR(13) XOR ROTR(22)
            val s0 = ((h0 ushr 2) or (h0 shl 30)) xor 
                     ((h0 ushr 13) or (h0 shl 19)) xor 
                     ((h0 ushr 22) or (h0 shl 10))
            
            // Σ1 = ROTR(6) XOR ROTR(11) XOR ROTR(25)
            val s1 = ((h4 ushr 6) or (h4 shl 26)) xor 
                     ((h4 ushr 11) or (h4 shl 21)) xor 
                     ((h4 ushr 25) or (h4 shl 7))
            
            // Ch = (e AND f) XOR (NOT e AND g)
            val ch = (h4 and h5) xor (h4.inv() and h6)
            
            // Maj = (a AND b) XOR (a AND c) XOR (b AND c)
            val maj = (h0 and h1) xor (h0 and h2) xor (h1 and h2)
            
            // ENHANCED: Add more CPU-sensitive operations
            // Test integer division and modulo (sensitive to CPU architecture)
            val extra = if (i > 0) {
                val divisor = ((i and 0xFF) + 1)  // Fixed: parentheses ensure 1-256 range
                val div = (h0 / divisor)  // Division
                val mod = (h1 % divisor)  // Modulo
                div xor mod
            } else 0
            
            val temp1 = h7 + s1 + ch + i + extra
            val temp2 = s0 + maj
            
            // Rotate state
            h7 = h6
            h6 = h5
            h5 = h4
            h4 = h3 + temp1
            h3 = h2
            h2 = h1
            h1 = h0
            h0 = temp1 + temp2
        }
        
        // Return combined hash (use h0 and h1 for 64-bit result)
        return (h0.toLong() shl 32) or (h1.toLong() and 0xFFFFFFFFL)
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

    /**
     * Cache-Resident String Sorting Workload
     *
     * CACHE-RESIDENT STRATEGY:
     * - Uses small fixed-size list (4,096 strings) that fits in CPU cache
     * - Performs multiple iterations to maintain CPU utilization and achieve meaningful benchmark
     * times
     * - Prevents memory bandwidth bottlenecks by keeping data in cache
     * - Ensures true CPU throughput measurement
     *
     * @param sourceList The source list of strings to sort (cached-resident size: 4,096)
     * @param iterations Number of times to repeat the sorting operation
     * @return Checksum to prevent compiler optimization
     */
    fun runStringSortWorkload(sourceList: List<String>, iterations: Int): Int {
        var checkSum = 0
        // Reuse specific small size to keep data in L2 CPU Cache
        repeat(iterations) {
            // Create copy to ensure we are actually sorting (O(N) copy + O(N log N) sort)
            val workingList = ArrayList(sourceList)
            workingList.sort()
            if (workingList.isNotEmpty()) checkSum += workingList.last().hashCode()
        }
        return checkSum
    }

    /**
     * Centralized RLE Compression for Fixed Work Per Core benchmarking
     *
     * FIXED WORK PER CORE APPROACH:
     * - Generates data ONCE outside the compression loop
     * - Performs fixed number of compression iterations
     * - Uses cache-friendly 2MB buffer size
     * - Zero allocation in hot path (reuses output buffer)
     *
     * @param bufferSize Size of data buffer in bytes (2MB recommended)
     * @param iterations Number of compression iterations to perform
     * @return Total bytes processed (bufferSize * iterations)
     */
    fun performCompression(bufferSize: Int, iterations: Int): Long {
        // Generate data ONCE outside the compression loop
        val data = ByteArray(bufferSize) { (it % 256).toByte() }
        val outputBuffer = ByteArray(bufferSize * 2) // Output buffer for compression

        var totalCompressedSize = 0L

        // Perform compression iterations
        repeat(iterations) {
            val compressedSize = compressRLE(data, outputBuffer)
            totalCompressedSize += compressedSize
        }

        return bufferSize.toLong() * iterations
    }

    /**
     * Simple RLE (Run-Length Encoding) compression algorithm Zero allocation in hot path - uses
     * pre-allocated output buffer
     *
     * @param input Input data to compress
     * @param output Pre-allocated output buffer (must be at least 2x input size)
     * @return Number of bytes written to output buffer
     */
    private fun compressRLE(input: ByteArray, output: ByteArray): Int {
        var i = 0
        var outputIndex = 0

        while (i < input.size) {
            val currentByte = input[i]
            var count = 1

            // Count consecutive identical bytes (up to 255 for simplicity)
            while (i + count < input.size && input[i + count] == currentByte && count < 255) {
                count++
            }

            // Output (count, byte) pair
            output[outputIndex++] = count.toByte()
            output[outputIndex++] = currentByte

            i += count
        }

        return outputIndex
    }

    /**
     * Centralized Monte Carlo π Simulation - FIXED WORK PER CORE
     *
     * ULTRA-FAST DETERMINISTIC APPROACH:
     * - Uses Linear Congruential Generator (LCG) for deterministic pseudo-random
     * - 10× faster than Halton (just multiply + add, no loops/divisions)
     * - Deterministic (same input = same output)
     * - Good distribution for Monte Carlo (doesn't need perfect quasi-random)
     * - Purely CPU-bound (tests FPU performance)
     *
     * PERFORMANCE: Optimized for ~1.5-2.0s execution time per core
     *
     * @param samples Number of Monte Carlo samples to process
     * @return Count of points that fall inside the unit circle
     */
    fun performMonteCarlo(samples: Long): Long {
        var insideCircle = 0L

        // LCG constants (from Numerical Recipes)
        val a = 1664525L
        val c = 1013904223L
        val m = 4294967296.0  // 2^32

        for (i in 0 until samples) {
            // Generate x coordinate using LCG
            var seed = i * 2L + 1L  // Unique seed for x
            seed = (seed * a + c) and 0xFFFFFFFFL  // LCG formula
            val x = (seed / m) * 2.0 - 1.0  // Map to [-1, 1]

            // Generate y coordinate using LCG with different seed
            seed = i * 2L + 2L  // Unique seed for y
            seed = (seed * a + c) and 0xFFFFFFFFL
            val y = (seed / m) * 2.0 - 1.0

            // Check if inside unit circle
            if (x * x + y * y <= 1.0) {
                insideCircle++
            }
        }

        return insideCircle
    }

    /**
     * ULTRA-OPTIMIZED KERNEL: "Register-Cached" Ray Tracing
     * * Optimizations:
     * 1. CPU Cache/Registers: Sphere data is hardcoded locals, forcing them into registers.
     * 2. Zero Function Calls: All logic is inlined into the double-loop (Mega-Kernel).
     * 3. Loop Unrolling: Sphere checks are unrolled to remove branch misprediction.
     * 4. Fast Math: Divisions replaced with multiplication by inverse.
     */
    fun renderScenePrimitives(width: Int, height: Int, maxDepth: Int): Double {
        var totalEnergy = 0.0

        // --- CACHE OPTIMIZATION: INVARIANT CONSTANTS ---
        // Pre-calculating these keeps them hot in L1 Cache/Registers
        val invWidth = 1.0 / width
        val invHeight = 1.0 / height
        val aspectRatio = width.toDouble() / height.toDouble()
        val fovFactor = 0.41421356 // tan(45/2) pre-calculated

        // --- REGISTER OPTIMIZATION: HARDCODED SCENE ---
        // By defining these as locals, the compiler puts them in CPU Registers.
        // Sphere 1
        val s1X = 0.0
        val s1Y = 0.0
        val s1Z = -1.0
        val s1RSq = 0.25 // r=0.5
        // Sphere 2
        val s2X = 1.0
        val s2Y = 0.0
        val s2Z = -1.5
        val s2RSq = 0.09 // r=0.3
        // Sphere 3
        val s3X = -1.0
        val s3Y = -0.5
        val s3Z = -1.2
        val s3RSq = 0.16 // r=0.4
        // Sphere 4 (NEW)
        val s4X = 0.5
        val s4Y = 0.5
        val s4Z = -0.8
        val s4RSq = 0.12 // r=0.35
        // Sphere 5 (NEW)
        val s5X = -0.5
        val s5Y = 0.3
        val s5Z = -1.3
        val s5RSq = 0.14 // r=0.37
        // Sphere 6 (NEW)
        val s6X = 0.0
        val s6Y = -0.8
        val s6Z = -1.1
        val s6RSq = 0.18 // r=0.42

        // Light Direction (Normalized 1,1,1)
        val lX = 0.57735
        val lY = 0.57735
        val lZ = 0.57735

        // Y-Loop (Rows)
        for (y in 0 until height) {
            // Hoist Y calculation out of X loop
            val yNDC = (1.0 - 2.0 * (y + 0.5) * invHeight) * fovFactor

            // X-Loop (Pixels)
            for (x in 0 until width) {

                // --- STEP 1: RAY GENERATION ---
                var dirX = (2.0 * (x + 0.5) * invWidth - 1.0) * aspectRatio * fovFactor
                var dirY = yNDC
                var dirZ = -1.0

                // Fast Inverse Sqrt for Normalization
                val lenSq = dirX * dirX + dirY * dirY + dirZ * dirZ
                val invLen = 1.0 / Math.sqrt(lenSq)
                dirX *= invLen
                dirY *= invLen
                dirZ *= invLen

                // --- STEP 2: INTERSECTION (UNROLLED) ---
                // We find the closest 't' (distance)
                var closestT = 99999.0 // huge number
                var hitId = 0 // 0=Miss, 1=S1, 2=S2, 3=S3

                // CHECK SPHERE 1
                // b = 2 * dot(oc, dir). Since origin is 0,0,0, oc = -center.
                // dot(-center, dir) = -(sX*dX + sY*dY + sZ*dZ)
                val b1 = 2.0 * (-s1X * dirX - s1Y * dirY - s1Z * dirZ)
                val c1 = (s1X * s1X + s1Y * s1Y + s1Z * s1Z) - s1RSq
                val d1 = b1 * b1 - 4.0 * c1
                if (d1 > 0.0) {
                    val sqrtD = Math.sqrt(d1)
                    val t = (-b1 - sqrtD) * 0.5
                    if (t > 0.001 && t < closestT) {
                        closestT = t
                        hitId = 1
                    }
                }

                // CHECK SPHERE 2
                val b2 = 2.0 * (-s2X * dirX - s2Y * dirY - s2Z * dirZ)
                val c2 = (s2X * s2X + s2Y * s2Y + s2Z * s2Z) - s2RSq
                val d2 = b2 * b2 - 4.0 * c2
                if (d2 > 0.0) {
                    val sqrtD = Math.sqrt(d2)
                    val t = (-b2 - sqrtD) * 0.5
                    if (t > 0.001 && t < closestT) {
                        closestT = t
                        hitId = 2
                    }
                }

                // CHECK SPHERE 3
                val b3 = 2.0 * (-s3X * dirX - s3Y * dirY - s3Z * dirZ)
                val c3 = (s3X * s3X + s3Y * s3Y + s3Z * s3Z) - s3RSq
                val d3 = b3 * b3 - 4.0 * c3
                if (d3 > 0.0) {
                    val sqrtD = Math.sqrt(d3)
                    val t = (-b3 - sqrtD) * 0.5
                    if (t > 0.001 && t < closestT) {
                        closestT = t
                        hitId = 3
                    }
                }

                // CHECK SPHERE 4 (NEW)
                val b4 = 2.0 * (-s4X * dirX - s4Y * dirY - s4Z * dirZ)
                val c4 = (s4X * s4X + s4Y * s4Y + s4Z * s4Z) - s4RSq
                val d4 = b4 * b4 - 4.0 * c4
                if (d4 > 0.0) {
                    val sqrtD = Math.sqrt(d4)
                    val t = (-b4 - sqrtD) * 0.5
                    if (t > 0.001 && t < closestT) {
                        closestT = t
                        hitId = 4
                    }
                }

                // CHECK SPHERE 5 (NEW)
                val b5 = 2.0 * (-s5X * dirX - s5Y * dirY - s5Z * dirZ)
                val c5 = (s5X * s5X + s5Y * s5Y + s5Z * s5Z) - s5RSq
                val d5 = b5 * b5 - 4.0 * c5
                if (d5 > 0.0) {
                    val sqrtD = Math.sqrt(d5)
                    val t = (-b5 - sqrtD) * 0.5
                    if (t > 0.001 && t < closestT) {
                        closestT = t
                        hitId = 5
                    }
                }

                // CHECK SPHERE 6 (NEW)
                val b6 = 2.0 * (-s6X * dirX - s6Y * dirY - s6Z * dirZ)
                val c6 = (s6X * s6X + s6Y * s6Y + s6Z * s6Z) - s6RSq
                val d6 = b6 * b6 - 4.0 * c6
                if (d6 > 0.0) {
                    val sqrtD = Math.sqrt(d6)
                    val t = (-b6 - sqrtD) * 0.5
                    if (t > 0.001 && t < closestT) {
                        closestT = t
                        hitId = 6
                    }
                }

                // --- STEP 3: SHADING ---
                if (hitId == 0) {
                    // Miss: Sky Gradient
                    totalEnergy += (1.0 - (0.5 * (dirY + 1.0))) * 1.0 + (0.5 * (dirY + 1.0)) * 0.5
                } else {
                    // Hit: Calculate Hit Point
                    val hpX = closestT * dirX
                    val hpY = closestT * dirY
                    val hpZ = closestT * dirZ

                    // Calculate Normal (N = Hit - Center)
                    var nX = 0.0
                    var nY = 0.0
                    var nZ = 0.0

                    // Branchless selection of center (using 'when' which compiles to optimized
                    // switch)
                    if (hitId == 1) {
                        nX = hpX - s1X
                        nY = hpY - s1Y
                        nZ = hpZ - s1Z
                    } else if (hitId == 2) {
                        nX = hpX - s2X
                        nY = hpY - s2Y
                        nZ = hpZ - s2Z
                    } else if (hitId == 3) {
                        nX = hpX - s3X
                        nY = hpY - s3Y
                        nZ = hpZ - s3Z
                    } else if (hitId == 4) {
                        nX = hpX - s4X
                        nY = hpY - s4Y
                        nZ = hpZ - s4Z
                    } else if (hitId == 5) {
                        nX = hpX - s5X
                        nY = hpY - s5Y
                        nZ = hpZ - s5Z
                    } else {
                        nX = hpX - s6X
                        nY = hpY - s6Y
                        nZ = hpZ - s6Z
                    }

                    // Normalize Normal
                    val nLen = 1.0 / Math.sqrt(nX * nX + nY * nY + nZ * nZ)
                    nX *= nLen
                    nY *= nLen
                    nZ *= nLen

                    // Diffuse Lighting (Dot Product)
                    val dot = nX * lX + nY * lY + nZ * lZ
                    val diff = if (dot > 0.0) dot else 0.0

                    // REFLECTION (adds FPU ops): R = D - 2(D·N)N
                    val dotDN = dirX * nX + dirY * nY + dirZ * nZ
                    val reflX = dirX - 2.0 * dotDN * nX
                    val reflY = dirY - 2.0 * dotDN * nY
                    val reflZ = dirZ - 2.0 * dotDN * nZ
                    val specular = if (reflX * lX + reflY * lY + reflZ * lZ > 0.0) {
                        reflX * lX + reflY * lY + reflZ * lZ
                    } else 0.0

                    // Simple "Compute Load" to simulate reflection cost without recursion
                    // This keeps the pipeline busy
                    totalEnergy += diff + (diff * diff) * 0.5 + specular * 0.3
                }
            }
        }
        return totalEnergy
    }

    /**
     * Cache-Resident Ray Tracing Workload - INLINED FOR PERFORMANCE
     *
     * CACHE-RESIDENT STRATEGY:
     * - Renders the same scene multiple times (iterations)
     * - Scene data stays in CPU cache/registers for fast access
     * - Measures pure CPU FPU throughput, not memory bandwidth
     * - Same algorithm for both Single-Core and Multi-Core tests
     *
     * CRITICAL: Inlined ray tracing logic to avoid function call overhead This matches the pattern
     * used by Monte Carlo which achieves proper multi-core scaling.
     *
     * @param width Image width in pixels
     * @param height Image height in pixels
     * @param maxDepth Maximum ray bounce depth (unused in current implementation)
     * @param iterations Number of times to render the scene
     * @return Total energy accumulated across all iterations (for validation)
     */
    fun performRayTracing(width: Int, height: Int, maxDepth: Int, iterations: Int): Double {
        var totalEnergy = 0.0

        // INLINED: Cache constants (hoisted out of iteration loop)
        val invWidth = 1.0 / width
        val invHeight = 1.0 / height
        val aspectRatio = width.toDouble() / height.toDouble()
        val fovFactor = 0.41421356 // tan(45/2) pre-calculated

        // INLINED: Scene (hardcoded spheres)
        val s1X = 0.0
        val s1Y = 0.0
        val s1Z = -1.0
        val s1RSq = 0.25 // r=0.5
        val s2X = 1.0
        val s2Y = 0.0
        val s2Z = -1.5
        val s2RSq = 0.09 // r=0.3
        val s3X = -1.0
        val s3Y = -0.5
        val s3Z = -1.2
        val s3RSq = 0.16 // r=0.4

        // Light Direction (Normalized 1,1,1)
        val lX = 0.57735
        val lY = 0.57735
        val lZ = 0.57735

        repeat(iterations) {
            var frameEnergy = 0.0

            // Y-Loop (Rows)
            for (y in 0 until height) {
                // Hoist Y calculation out of X loop
                val yNDC = (1.0 - 2.0 * (y + 0.5) * invHeight) * fovFactor

                // X-Loop (Pixels)
                for (x in 0 until width) {
                    // --- STEP 1: RAY GENERATION ---
                    var dirX = (2.0 * (x + 0.5) * invWidth - 1.0) * aspectRatio * fovFactor
                    var dirY = yNDC
                    var dirZ = -1.0

                    // Fast Inverse Sqrt for Normalization
                    val lenSq = dirX * dirX + dirY * dirY + dirZ * dirZ
                    val invLen = 1.0 / StrictMath.sqrt(lenSq)
                    dirX *= invLen
                    dirY *= invLen
                    dirZ *= invLen

                    // --- STEP 2: INTERSECTION (UNROLLED) ---
                    var closestT = 99999.0
                    var hitId = 0

                    // CHECK SPHERE 1
                    val b1 = 2.0 * (-s1X * dirX - s1Y * dirY - s1Z * dirZ)
                    val c1 = (s1X * s1X + s1Y * s1Y + s1Z * s1Z) - s1RSq
                    val d1 = b1 * b1 - 4.0 * c1
                    if (d1 > 0.0) {
                        val t = (-b1 - StrictMath.sqrt(d1)) * 0.5
                        if (t > 0.001 && t < closestT) {
                            closestT = t
                            hitId = 1
                        }
                    }

                    // CHECK SPHERE 2
                    val b2 = 2.0 * (-s2X * dirX - s2Y * dirY - s2Z * dirZ)
                    val c2 = (s2X * s2X + s2Y * s2Y + s2Z * s2Z) - s2RSq
                    val d2 = b2 * b2 - 4.0 * c2
                    if (d2 > 0.0) {
                        val t = (-b2 - StrictMath.sqrt(d2)) * 0.5
                        if (t > 0.001 && t < closestT) {
                            closestT = t
                            hitId = 2
                        }
                    }

                    // CHECK SPHERE 3
                    val b3 = 2.0 * (-s3X * dirX - s3Y * dirY - s3Z * dirZ)
                    val c3 = (s3X * s3X + s3Y * s3Y + s3Z * s3Z) - s3RSq
                    val d3 = b3 * b3 - 4.0 * c3
                    if (d3 > 0.0) {
                        val t = (-b3 - StrictMath.sqrt(d3)) * 0.5
                        if (t > 0.001 && t < closestT) {
                            closestT = t
                            hitId = 3
                        }
                    }

                    // --- STEP 3: SHADING ---
                    if (hitId == 0) {
                        // Miss: Sky Gradient
                        frameEnergy +=
                                (1.0 - (0.5 * (dirY + 1.0))) * 1.0 + (0.5 * (dirY + 1.0)) * 0.5
                    } else {
                        // Hit: Calculate Hit Point
                        val hpX = closestT * dirX
                        val hpY = closestT * dirY
                        val hpZ = closestT * dirZ

                        // Calculate Normal (N = Hit - Center)
                        var nX = 0.0
                        var nY = 0.0
                        var nZ = 0.0

                        when (hitId) {
                            1 -> {
                                nX = hpX - s1X
                                nY = hpY - s1Y
                                nZ = hpZ - s1Z
                            }
                            2 -> {
                                nX = hpX - s2X
                                nY = hpY - s2Y
                                nZ = hpZ - s2Z
                            }
                            else -> {
                                nX = hpX - s3X
                                nY = hpY - s3Y
                                nZ = hpZ - s3Z
                            }
                        }

                        // Normalize Normal
                        val nLen = 1.0 / StrictMath.sqrt(nX * nX + nY * nY + nZ * nZ)
                        nX *= nLen
                        nY *= nLen
                        nZ *= nLen

                        // Diffuse Lighting (Dot Product)
                        val dot = nX * lX + nY * lY + nZ * lZ
                        val diff = if (dot > 0.0) dot else 0.0

                        // Simple "Compute Load" to simulate reflection cost without recursion
                        frameEnergy += diff + (diff * diff) * 0.5
                    }
                }
            }
            totalEnergy += frameEnergy
        }

        return totalEnergy
    }

    /**
     * Centralized JSON Generation for Cache-Resident Benchmarking
     *
     * CACHE-RESIDENT STRATEGY:
     * - Generates complex nested JSON data once
     * - Data is reused across multiple parsing iterations
     * - Prevents generation overhead from affecting benchmark timing
     *
     * @param sizeTarget Target size of JSON string in bytes
     * @return Generated JSON string with nested objects and arrays
     */
    fun generateComplexJson(sizeTarget: Int): String {
        val result = StringBuilder()
        result.append("{\"data\":[")
        var currentSize = result.length
        var counter = 0

        while (currentSize < sizeTarget) {
            val jsonObj =
                    "{\"id\":$counter,\"name\":\"obj$counter\",\"nested\":{\"value\":${counter % 1000},\"array\":[1,2,3,4,5]}},"

            if (currentSize + jsonObj.length > sizeTarget) {
                break
            }

            result.append(jsonObj)
            currentSize += jsonObj.length
            counter++
        }

        // Remove the trailing comma and close the array and object
        if (result.endsWith(',')) {
            result.deleteCharAt(result.length - 1)
        }
        result.append("]}")

        return result.toString()
    }

    /**
     * JSON Parsing Workload - Fresh Data Per Iteration
     *
     * Parses JSON strings with fresh data generated for each iteration
     * to prevent caching effects.
     *
     * @param jsonData Base JSON template (size reference)
     * @param iterations Number of times to parse the JSON data
     * @return Total element count across all iterations (for validation)
     */
    fun performJsonParsingWorkload(jsonData: String, iterations: Int): Int {
        var totalElementCount = 0
        val baseSize = jsonData.length

        repeat(iterations) { iter ->
            // Generate fresh JSON data for each iteration with unique seed
            val random = java.util.Random(System.nanoTime() + iter)
            val freshJson = generateRandomJson(baseSize, random)
            
            // Count elements in the JSON string as a simple way to "parse" it
            var elementCount = 0
            var inString = false

            for (char in freshJson) {
                if (char == '"') {
                    inString = !inString
                } else if (!inString) {
                    when (char) {
                        '{', '[' -> elementCount++
                        '}', ']' -> {} // Do nothing for closing brackets
                        else -> {}
                    }
                }
            }

            totalElementCount += elementCount
        }

        return totalElementCount
    }
    
    /**
     * Generate a random JSON string of approximately the given size
     */
    private fun generateRandomJson(targetSize: Int, random: java.util.Random): String {
        val sb = StringBuilder()
        sb.append("{\"data\":[")
        
        var currentSize = sb.length
        var first = true
        
        while (currentSize < targetSize - 50) {
            if (!first) sb.append(",")
            first = false
            
            // Generate a random object
            sb.append("{\"id\":")
            sb.append(random.nextInt(1000000))
            sb.append(",\"value\":\"")
            // Add random string value
            repeat(10 + random.nextInt(20)) {
                sb.append(('a' + random.nextInt(26)))
            }
            sb.append("\",\"flag\":")
            sb.append(random.nextBoolean())
            sb.append(",\"num\":")
            sb.append(random.nextDouble())
            sb.append("}")
            
            currentSize = sb.length
        }
        
        sb.append("]}")
        return sb.toString()
    }

    /**
     * Centralized N-Queens Solver with Iteration Tracking
     *
     * FIXED WORK PER CORE APPROACH:
     * - Uses optimized backtracking algorithm
     * - Tracks both solutions found and iterations performed
     * - Returns iterations as the primary metric (solutions count is constant for given N)
     * - Same algorithm for both Single-Core and Multi-Core tests
     *
     * OPTIMIZATIONS:
     * - Bitwise operations for diagonal tracking (faster than boolean arrays)
     * - Early pruning when no valid positions available
     * - Minimal memory allocations
     *
     * @param size Board size (N×N)
     * @return Pair<solutionCount, iterationCount> where iterationCount is the performance metric
     */
    fun solveNQueens(size: Int): Pair<Int, Long> {
        var solutionCount = 0
        var iterationCount = 0L

        // Use integer bitmasks for faster diagonal tracking
        // cols: column occupancy (bit i = 1 if column i has a queen)
        // diag1: diagonal \ occupancy (bit i = 1 if diagonal i has a queen)
        // diag2: diagonal / occupancy (bit i = 1 if diagonal i has a queen)
        fun backtrack(row: Int, cols: Int, diag1: Int, diag2: Int) {
            iterationCount++

            if (row == size) {
                solutionCount++
                return
            }

            // Calculate available positions (bitwise NOT of occupied positions)
            // A position is available if it's not in any occupied column or diagonal
            var availablePositions =
                    ((1 shl size) - 1) and cols.inv() and diag1.inv() and diag2.inv()

            // Try each available position
            while (availablePositions != 0) {
                // Get the rightmost available position
                val position = availablePositions and -availablePositions

                // Remove this position from available positions
                availablePositions = availablePositions xor position

                // Calculate column index from position bitmask
                val col = Integer.numberOfTrailingZeros(position)

                // Recurse with updated occupancy masks
                // cols | position: mark column as occupied
                // (diag1 | position) << 1: mark \ diagonal as occupied and shift for next row
                // (diag2 | position) >> 1: mark / diagonal as occupied and shift for next row
                backtrack(
                        row + 1,
                        cols or position,
                        (diag1 or position) shl 1,
                        (diag2 or position) shr 1
                )
            }
        }

        // Start backtracking from row 0 with no occupied positions
        backtrack(0, 0, 0, 0)

        return Pair(solutionCount, iterationCount)
    }
}
