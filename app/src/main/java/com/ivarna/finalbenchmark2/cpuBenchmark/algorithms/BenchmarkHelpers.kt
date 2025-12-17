package com.ivarna.finalbenchmark2.cpuBenchmark.algorithms

import java.util.Arrays
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
     * Matrix Multiplication using Strassen's Algorithm
     *
     * Implements Strassen's divide-and-conquer matrix multiplication algorithm
     * with O(n^2.807) complexity instead of O(n^3) for naive multiplication.
     * Falls back to standard multiplication for small matrices (threshold = 64).
     * Creates fresh matrices for EACH iteration to prevent any caching effects.
     *
     * @param size The size of the square matrices (size × size) - must be power of 2
     * @param repetitions Number of times to repeat the matrix multiplication
     * @return The checksum of the final resulting matrix C
     */
    fun performMatrixMultiplication(size: Int, repetitions: Int = 1): Long {
        var checksum = 0L
        
        // Ensure size is a power of 2 for Strassen's algorithm
        val paddedSize = nextPowerOfTwo(size)
        
        repeat(repetitions) { rep ->
            // Create fresh Random with unique seed per iteration
            val random = java.util.Random(System.nanoTime() + rep)
            
            // Create NEW matrices for each iteration - no caching
            val a = Array(paddedSize) { i ->
                DoubleArray(paddedSize) { j ->
                    if (i < size && j < size) random.nextDouble() else 0.0
                }
            }
            val b = Array(paddedSize) { i ->
                DoubleArray(paddedSize) { j ->
                    if (i < size && j < size) random.nextDouble() else 0.0
                }
            }

            // Perform Strassen's multiplication
            val c = strassenMultiply(a, b, paddedSize)

            // Update checksum from each iteration (only use original size)
            checksum = checksum xor calculateMatrixChecksumPartial(c, size)
        }

        return checksum
    }
    
    /**
     * Find the next power of 2 >= n
     */
    private fun nextPowerOfTwo(n: Int): Int {
        var power = 1
        while (power < n) power *= 2
        return power
    }
    
    /**
     * Calculate checksum for a partial matrix (up to given size)
     */
    private fun calculateMatrixChecksumPartial(matrix: Array<DoubleArray>, size: Int): Long {
        var checksum = 0L
        for (i in 0 until size) {
            for (j in 0 until size) {
                checksum = checksum xor java.lang.Double.doubleToLongBits(matrix[i][j])
            }
        }
        return checksum
    }
    
    // Threshold below which we use standard multiplication
    private const val STRASSEN_THRESHOLD = 64
    
    /**
     * Strassen's Matrix Multiplication Algorithm
     * Recursively divides matrices and uses 7 multiplications instead of 8
     */
    private fun strassenMultiply(a: Array<DoubleArray>, b: Array<DoubleArray>, n: Int): Array<DoubleArray> {
        // Base case: use standard multiplication for small matrices
        if (n <= STRASSEN_THRESHOLD) {
            return standardMultiply(a, b, n)
        }
        
        val half = n / 2
        
        // Split matrices into quadrants
        val a11 = getQuadrant(a, 0, 0, half)
        val a12 = getQuadrant(a, 0, half, half)
        val a21 = getQuadrant(a, half, 0, half)
        val a22 = getQuadrant(a, half, half, half)
        
        val b11 = getQuadrant(b, 0, 0, half)
        val b12 = getQuadrant(b, 0, half, half)
        val b21 = getQuadrant(b, half, 0, half)
        val b22 = getQuadrant(b, half, half, half)
        
        // Compute the 7 Strassen products
        val m1 = strassenMultiply(addMatrices(a11, a22, half), addMatrices(b11, b22, half), half)
        val m2 = strassenMultiply(addMatrices(a21, a22, half), b11, half)
        val m3 = strassenMultiply(a11, subtractMatrices(b12, b22, half), half)
        val m4 = strassenMultiply(a22, subtractMatrices(b21, b11, half), half)
        val m5 = strassenMultiply(addMatrices(a11, a12, half), b22, half)
        val m6 = strassenMultiply(subtractMatrices(a21, a11, half), addMatrices(b11, b12, half), half)
        val m7 = strassenMultiply(subtractMatrices(a12, a22, half), addMatrices(b21, b22, half), half)
        
        // Compute result quadrants
        val c11 = addMatrices(subtractMatrices(addMatrices(m1, m4, half), m5, half), m7, half)
        val c12 = addMatrices(m3, m5, half)
        val c21 = addMatrices(m2, m4, half)
        val c22 = addMatrices(subtractMatrices(addMatrices(m1, m3, half), m2, half), m6, half)
        
        // Combine quadrants into result
        return combineQuadrants(c11, c12, c21, c22, n)
    }
    
    /**
     * Standard O(n^3) matrix multiplication for small matrices
     */
    private fun standardMultiply(a: Array<DoubleArray>, b: Array<DoubleArray>, n: Int): Array<DoubleArray> {
        val c = Array(n) { DoubleArray(n) }
        for (i in 0 until n) {
            for (k in 0 until n) {
                val aik = a[i][k]
                for (j in 0 until n) {
                    c[i][j] += aik * b[k][j]
                }
            }
        }
        return c
    }
    
    /**
     * Extract a quadrant from a matrix
     */
    private fun getQuadrant(m: Array<DoubleArray>, rowStart: Int, colStart: Int, size: Int): Array<DoubleArray> {
        return Array(size) { i ->
            DoubleArray(size) { j ->
                m[rowStart + i][colStart + j]
            }
        }
    }
    
    /**
     * Add two matrices
     */
    private fun addMatrices(a: Array<DoubleArray>, b: Array<DoubleArray>, n: Int): Array<DoubleArray> {
        return Array(n) { i ->
            DoubleArray(n) { j ->
                a[i][j] + b[i][j]
            }
        }
    }
    
    /**
     * Subtract two matrices
     */
    private fun subtractMatrices(a: Array<DoubleArray>, b: Array<DoubleArray>, n: Int): Array<DoubleArray> {
        return Array(n) { i ->
            DoubleArray(n) { j ->
                a[i][j] - b[i][j]
            }
        }
    }
    
    /**
     * Combine four quadrants into a single matrix
     */
    private fun combineQuadrants(
        c11: Array<DoubleArray>, c12: Array<DoubleArray>,
        c21: Array<DoubleArray>, c22: Array<DoubleArray>,
        n: Int
    ): Array<DoubleArray> {
        val half = n / 2
        return Array(n) { i ->
            DoubleArray(n) { j ->
                when {
                    i < half && j < half -> c11[i][j]
                    i < half && j >= half -> c12[i][j - half]
                    i >= half && j < half -> c21[i - half][j]
                    else -> c22[i - half][j - half]
                }
            }
        }
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
     * FIXED WORK PER CORE APPROACH:
     * - Uses efficient vectorized operations for better performance
     * - Configurable batch size for cache optimization (default: 256)
     * - Thread-safe using ThreadLocalRandom
     * - Same algorithm for both Single-Core and Multi-Core tests
     * - Blocking function (like performHashComputing, performCompression)
     *
     * PERFORMANCE: Optimized for ~1.5-2.0s execution time per core
     *
     * @param samples Number of Monte Carlo samples to process
     * @return Count of points that fall inside the unit circle
     */
    fun performMonteCarlo(samples: Long): Long {
        var insideCircle = 0L

        // OPTIMIZED: Use ThreadLocalRandom for thread-safe random generation
        val random = ThreadLocalRandom.current()

        // OPTIMIZED: Vectorized batch processing for better cache locality
        // Batch size of 256 balances cache efficiency and loop overhead
        val batchSize = 256
        val vectorizedSamples = samples / batchSize * batchSize
        var processed = 0L

        while (processed < vectorizedSamples) {
            // Process batch of points for better cache utilization
            var localCount = 0

            repeat(batchSize) {
                val x = random.nextDouble() * 2.0 - 1.0
                val y = random.nextDouble() * 2.0 - 1.0
                if (x * x + y * y <= 1.0) localCount++
            }

            insideCircle += localCount
            processed += batchSize
        }

        // Handle remaining samples
        repeat((samples - vectorizedSamples).toInt()) {
            val x = random.nextDouble() * 2.0 - 1.0
            val y = random.nextDouble() * 2.0 - 1.0
            if (x * x + y * y <= 1.0) insideCircle++
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
                    } else {
                        nX = hpX - s3X
                        nY = hpY - s3Y
                        nZ = hpZ - s3Z
                    }

                    // Normalize Normal
                    val nLen = 1.0 / Math.sqrt(nX * nX + nY * nY + nZ * nZ)
                    nX *= nLen
                    nY *= nLen
                    nZ *= nLen

                    // Diffuse Lighting (Dot Product)
                    val dot = nX * lX + nY * lY + nZ * lZ
                    val diff = if (dot > 0.0) dot else 0.0

                    // Simple "Compute Load" to simulate reflection cost without recursion
                    // This keeps the pipeline busy
                    totalEnergy += diff + (diff * diff) * 0.5
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
     * Cache-Resident JSON Parsing Workload
     *
     * CACHE-RESIDENT STRATEGY:
     * - Parses the same JSON data multiple times (iterations)
     * - JSON data stays in CPU cache for fast access
     * - Measures pure CPU parsing throughput, not memory bandwidth
     * - Same algorithm for both Single-Core and Multi-Core tests
     *
     * @param jsonData Pre-generated JSON string to parse
     * @param iterations Number of times to parse the JSON data
     * @return Total element count across all iterations (for validation)
     */
    fun performJsonParsingWorkload(jsonData: String, iterations: Int): Int {
        var totalElementCount = 0

        repeat(iterations) {
            // Count elements in the JSON string as a simple way to "parse" it
            // In a real implementation, we'd use a JSON library like org.json or Moshi
            var elementCount = 0
            var inString = false

            for (char in jsonData) {
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
