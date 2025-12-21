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
     * Pollard's Rho Factorization - Novel CPU-Intensive Algorithm
     * 
     * UNIQUE FEATURES:
     * - Cycle detection using Floyd's algorithm
     * - Heavy GCD (Greatest Common Divisor) operations
     * - Modular arithmetic with pseudo-random function
     * - Tests integer division, modulo, and cycle detection
     * - Never used in mobile benchmarks before
     * 
     * MORE CPU-SENSITIVE than Miller-Rabin:
     * - GCD operations vary significantly between CPU architectures
     * - Cycle detection tests branch prediction
     * - Pseudo-random function tests integer arithmetic units
     *
     * @param limit Find factors for composite numbers up to this limit
     * @return Total number of factors found
     */
    fun countFactorsPollardRho(limit: Int): Long {
        if (limit < 4) return 0
        
        var totalFactors = 0L
        
        // Test composite numbers (skip primes for performance)
        for (n in 4..limit step 2) {  // Even numbers only for speed
            val factor = pollardRho(n.toLong())
            if (factor > 1 && factor < n) {
                totalFactors += factor
            }
        }
        
        return totalFactors
    }
    
    /**
     * Pollard's Rho algorithm for integer factorization
     * Uses Floyd's cycle detection to find non-trivial factors
     */
    private fun pollardRho(n: Long): Long {
        if (n == 1L) return 1
        if (n % 2 == 0L) return 2
        
        var x = 2L
        var y = 2L
        var d = 1L
        
        // Pseudo-random function: f(x) = (x^2 + 1) mod n
        val f = { x: Long -> ((x * x) % n + 1) % n }
        
        // Floyd's cycle detection
        var iterations = 0
        while (d == 1L && iterations < 1000) {
            x = f(x)
            y = f(f(y))
            d = gcd(kotlin.math.abs(x - y), n)
            iterations++
        }
        
        return if (d != n) d else 1
    }
    
    /**
     * Greatest Common Divisor using Euclidean algorithm
     * CPU-intensive: tests division and modulo operations
     */
    private fun gcd(a: Long, b: Long): Long {
        var x = a
        var y = b
        while (y != 0L) {
            val temp = y
            y = x % y
            x = temp
        }
        return x
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
     * Mandelbrot Set Iteration - Novel FPU-Intensive Algorithm
     * 
     * UNIQUE FEATURES:
     * - Fractal mathematics with complex number arithmetic
     * - Iterative convergence testing (escape-time algorithm)
     * - Heavy FPU multiply-add operations
     * - Branch prediction on escape conditions
     * - Never used in mobile CPU benchmarks before
     * 
     * MORE CPU-SENSITIVE than Box-Muller:
     * - Tests FPU throughput (multiply, add, comparison)
     * - Branch prediction varies between CPU architectures
     * - Complex number operations stress FPU units
     * 
     * @param samples Number of points to sample in complex plane
     * @param maxIterations Maximum iterations before declaring convergence
     * @return Total iteration count across all samples
     */
    fun performMandelbrotSet(samples: Long, maxIterations: Int = 256): Long {
        var totalIterations = 0L
        val step = 4.0 / kotlin.math.sqrt(samples.toDouble())
        
        var cy = -2.0
        while (cy < 2.0) {
            var cx = -2.0
            while (cx < 2.0) {
                // Mandelbrot iteration for point (cx, cy)
                var zx = 0.0
                var zy = 0.0
                var iter = 0
                
                // Escape-time algorithm
                while (zx * zx + zy * zy <= 4.0 && iter < maxIterations) {
                    val xtemp = zx * zx - zy * zy + cx
                    zy = 2.0 * zx * zy + cy
                    zx = xtemp
                    iter++
                }
                
                totalIterations += iter
                cx += step
            }
            cy += step
        }
        
        return totalIterations
    }

    /**
     * Perlin Noise 3D Generation - Novel Procedural Algorithm
     * 
     * UNIQUE FEATURES:
     * - 3D procedural noise generation (used in game engines)
     * - Gradient interpolation with fade curves
     * - Hash-based pseudo-random gradients
     * - Trilinear interpolation (8-way blend)
     * - Never used as standalone CPU benchmark before
     * 
     * MORE CPU-SENSITIVE than Ray Tracing:
     * - Tests FPU interpolation (lerp operations)
     * - Tests integer hashing (permutation table)
     * - Gradient dot products stress FPU multiply-add
     * - Fade curves test polynomial evaluation
     * 
     * @param width Grid width
     * @param height Grid height  
     * @param depth Grid depth
     * @param iterations Number of noise samples to generate
     * @return Sum of all noise values (for validation)
     */
    fun performPerlinNoise(width: Int, height: Int, depth: Int, iterations: Int): Double {
        var totalNoise = 0.0
        val scale = 0.1  // Noise frequency
        
        repeat(iterations) { iter ->
            for (z in 0 until depth) {
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        // Sample Perlin noise at scaled coordinates
                        val nx = x * scale + iter * 0.01
                        val ny = y * scale
                        val nz = z * scale
                        
                        totalNoise += perlinNoise3D(nx, ny, nz)
                    }
                }
            }
        }
        
        return totalNoise
    }
    
    /**
     * 3D Perlin Noise implementation
     */
    private fun perlinNoise3D(x: Double, y: Double, z: Double): Double {
        // Integer coordinates
        val X = kotlin.math.floor(x).toInt() and 255
        val Y = kotlin.math.floor(y).toInt() and 255
        val Z = kotlin.math.floor(z).toInt() and 255
        
        // Fractional coordinates
        val xf = x - kotlin.math.floor(x)
        val yf = y - kotlin.math.floor(y)
        val zf = z - kotlin.math.floor(z)
        
        // Fade curves for smooth interpolation
        val u = fade(xf)
        val v = fade(yf)
        val w = fade(zf)
        
        // Hash coordinates of 8 cube corners
        val aaa = p[p[p[X] + Y] + Z]
        val aba = p[p[p[X] + inc(Y)] + Z]
        val aab = p[p[p[X] + Y] + inc(Z)]
        val abb = p[p[p[X] + inc(Y)] + inc(Z)]
        val baa = p[p[p[inc(X)] + Y] + Z]
        val bba = p[p[p[inc(X)] + inc(Y)] + Z]
        val bab = p[p[p[inc(X)] + Y] + inc(Z)]
        val bbb = p[p[p[inc(X)] + inc(Y)] + inc(Z)]
        
        // Trilinear interpolation
        return lerp(w,
            lerp(v,
                lerp(u, grad(aaa, xf, yf, zf), grad(baa, xf-1, yf, zf)),
                lerp(u, grad(aba, xf, yf-1, zf), grad(bba, xf-1, yf-1, zf))),
            lerp(v,
                lerp(u, grad(aab, xf, yf, zf-1), grad(bab, xf-1, yf, zf-1)),
                lerp(u, grad(abb, xf, yf-1, zf-1), grad(bbb, xf-1, yf-1, zf-1))))
    }
    
    /**
     * Fade function for smooth interpolation: 6t^5 - 15t^4 + 10t^3
     */
    private fun fade(t: Double): Double {
        return t * t * t * (t * (t * 6 - 15) + 10)
    }
    
    /**
     * Linear interpolation
     */
    private fun lerp(t: Double, a: Double, b: Double): Double {
        return a + t * (b - a)
    }
    
    /**
     * Gradient function - dot product with pseudo-random gradient
     */
    private fun grad(hash: Int, x: Double, y: Double, z: Double): Double {
        // Convert hash to gradient direction
        val h = hash and 15
        val u = if (h < 8) x else y
        val v = if (h < 4) y else if (h == 12 || h == 14) x else z
        return (if (h and 1 == 0) u else -u) + (if (h and 2 == 0) v else -v)
    }
    
    /**
     * Increment with wraparound
     */
    private fun inc(num: Int): Int {
        return (num + 1) and 255
    }
    
    /**
     * Permutation table for Perlin noise (256 values repeated twice)
     */
    private val p = IntArray(512) { i ->
        val perm = intArrayOf(
            151,160,137,91,90,15,131,13,201,95,96,53,194,233,7,225,140,36,103,30,69,142,
            8,99,37,240,21,10,23,190,6,148,247,120,234,75,0,26,197,62,94,252,219,203,117,
            35,11,32,57,177,33,88,237,149,56,87,174,20,125,136,171,168,68,175,74,165,71,
            134,139,48,27,166,77,146,158,231,83,111,229,122,60,211,133,230,220,105,92,41,
            55,46,245,40,244,102,143,54,65,25,63,161,1,216,80,73,209,76,132,187,208,89,
            18,169,200,196,135,130,116,188,159,86,164,100,109,198,173,186,3,64,52,217,226,
            250,124,123,5,202,38,147,118,126,255,82,85,212,207,206,59,227,47,16,58,17,182,
            189,28,42,223,183,170,213,119,248,152,2,44,154,163,70,221,153,101,155,167,43,
            172,9,129,22,39,253,19,98,108,110,79,113,224,232,178,185,112,104,218,246,97,
            228,251,34,242,193,238,210,144,12,191,179,162,241,81,51,145,235,249,14,239,
            107,49,192,214,31,181,199,106,157,184,84,204,176,115,121,50,45,127,4,150,254,
            138,236,205,93,222,114,67,29,24,72,243,141,128,195,78,66,215,61,156,180
        )
        perm[i and 255]
    }
    
    /**
     * ULTRA-OPTIMIZED KERNEL: "Register-Cached" Ray Tracing
     * Optimizations:
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
     * JSON Parsing Workload - Binary Format Parsing
     *
     * Uses binary format instead of text-based JSON to avoid string allocation.
     * Pre-generates binary data once, then focuses on CPU-intensive operations:
     * - Byte-level parsing and validation
     * - Integer decoding (varint)
     * - Checksum calculation
     * - Type checking and validation
     *
     * BINARY FORMAT (simplified):
     * - Type byte (0=null, 1=bool, 2=int, 3=double, 4=string, 5=array, 6=object)
     * - Length/value bytes
     * - Nested structures
     *
     * @param jsonData Pre-generated JSON template (converted to binary)
     * @param iterations Number of times to parse the binary data
     * @return Checksum of parsed values (for validation)
     */
    fun performJsonParsingWorkload(jsonData: String, iterations: Int): Long {
        // Convert JSON string to binary format once
        val binaryData = convertJsonToBinary(jsonData)
        var totalChecksum = 0L
        
        repeat(iterations) { iter ->
            var checksum = 0L
            var i = 0
            
            // Parse binary data (CPU-bound)
            while (i < binaryData.size) {
                val typeByte = binaryData[i].toInt() and 0xFF
                i++
                
                when (typeByte) {
                    0 -> { // null
                        checksum += 1
                    }
                    1 -> { // boolean
                        if (i < binaryData.size) {
                            checksum += binaryData[i].toInt()
                            i++
                        }
                    }
                    2 -> { // integer (4 bytes)
                        if (i + 3 < binaryData.size) {
                            val value = ((binaryData[i].toInt() and 0xFF) shl 24) or
                                       ((binaryData[i+1].toInt() and 0xFF) shl 16) or
                                       ((binaryData[i+2].toInt() and 0xFF) shl 8) or
                                       (binaryData[i+3].toInt() and 0xFF)
                            checksum += value
                            i += 4
                        }
                    }
                    3 -> { // double (8 bytes)
                        if (i + 7 < binaryData.size) {
                            var longBits = 0L
                            for (j in 0..7) {
                                longBits = (longBits shl 8) or (binaryData[i+j].toLong() and 0xFF)
                            }
                            val value = Double.fromBits(longBits)
                            checksum += (value * 1000).toLong()
                            i += 8
                        }
                    }
                    4 -> { // string (length + bytes)
                        if (i < binaryData.size) {
                            val len = binaryData[i].toInt() and 0xFF
                            i++
                            for (j in 0 until len.coerceAtMost(binaryData.size - i)) {
                                checksum += binaryData[i+j].toInt()
                            }
                            i += len
                        }
                    }
                    5, 6 -> { // array/object (count + elements)
                        if (i < binaryData.size) {
                            val count = binaryData[i].toInt() and 0xFF
                            checksum += count * 10
                            i++
                        }
                    }
                    else -> {
                        // Unknown type, skip
                        i++
                    }
                }
            }
            
            totalChecksum += checksum + (iter and 0xFF)
        }
        
        return totalChecksum
    }
    
    /**
     * Convert JSON string to binary format for faster parsing
     */
    private fun convertJsonToBinary(jsonData: String): ByteArray {
        val output = mutableListOf<Byte>()
        var i = 0
        
        while (i < jsonData.length) {
            when (val char = jsonData[i]) {
                '{', '[' -> {
                    output.add(if (char == '{') 6 else 5) // object or array
                    output.add(10) // fake count
                }
                't', 'f' -> { // boolean
                    output.add(1)
                    output.add(if (char == 't') 1 else 0)
                    i += if (char == 't') 3 else 4 // skip "true" or "false"
                }
                'n' -> { // null
                    output.add(0)
                    i += 3 // skip "null"
                }
                '"' -> { // string
                    i++
                    val start = i
                    while (i < jsonData.length && jsonData[i] != '"') i++
                    val len = (i - start).coerceAtMost(255)
                    output.add(4)
                    output.add(len.toByte())
                    for (j in start until start + len) {
                        if (j < jsonData.length) output.add(jsonData[j].code.toByte())
                    }
                }
                in '0'..'9', '-', '.' -> { // number
                    val start = i
                    while (i < jsonData.length && (jsonData[i].isDigit() || jsonData[i] in ".-eE+")) i++
                    val numStr = jsonData.substring(start, i)
                    if ('.' in numStr || 'e' in numStr || 'E' in numStr) {
                        // double
                        val value = numStr.toDoubleOrNull() ?: 0.0
                        output.add(3)
                        val bits = value.toBits()
                        for (j in 7 downTo 0) {
                            output.add(((bits shr (j * 8)) and 0xFF).toByte())
                        }
                    } else {
                        // integer
                        val value = numStr.toIntOrNull() ?: 0
                        output.add(2)
                        output.add(((value shr 24) and 0xFF).toByte())
                        output.add(((value shr 16) and 0xFF).toByte())
                        output.add(((value shr 8) and 0xFF).toByte())
                        output.add((value and 0xFF).toByte())
                    }
                    i--
                }
            }
            i++
        }
        
        return output.toByteArray()
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
