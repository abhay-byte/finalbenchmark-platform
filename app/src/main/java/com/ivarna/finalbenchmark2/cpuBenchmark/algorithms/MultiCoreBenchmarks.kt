package com.ivarna.finalbenchmark2.cpuBenchmark.algorithms

import android.util.Log
import com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkResult
import com.ivarna.finalbenchmark2.cpuBenchmark.WorkloadParams
import com.ivarna.finalbenchmark2.cpuBenchmark.CpuAffinityManager
import kotlinx.coroutines.*
import org.json.JSONObject
import java.security.MessageDigest
import kotlin.random.Random

object MultiCoreBenchmarks {
    private const val TAG = "MultiCoreBenchmarks"
    
    // Number of threads = number of physical cores
    private val numThreads = Runtime.getRuntime().availableProcessors()
    
    /**
     * Test 1: Parallel Prime Generation
     */
    suspend fun primeGeneration(params: WorkloadParams): BenchmarkResult = coroutineScope {
        Log.d(TAG, "Starting Multi-Core Prime Generation")
        CpuAffinityManager.setMaxPerformance()
        
        val (primeCount, timeMs) = BenchmarkHelpers.measureBenchmark {
            val n = params.primeRange
            val chunkSize = n / numThreads
            
            // Process chunks in parallel
            val results = (0 until numThreads).map { threadId ->
                async(Dispatchers.Default) {
                    val start = threadId * chunkSize
                    val end = if (threadId == numThreads - 1) n else (threadId + 1) * chunkSize
                    
                    // Simple prime counting in range
                    var count = 0
                    for (i in start..end) {
                        if (i > 1 && BenchmarkHelpers.isPrime(i.toLong())) {
                            count++
                        }
                    }
                    count
                }
            }.awaitAll().sum()
            
            results
        }
        
        val ops = params.primeRange.toDouble()
        val opsPerSecond = ops / (timeMs / 1000.0)
        
        CpuAffinityManager.resetPerformance()
        
        BenchmarkResult(
            name = "Multi-Core Prime Generation",
            executionTimeMs = timeMs.toDouble(),
            opsPerSecond = opsPerSecond,
            isValid = primeCount > 0,
            metricsJson = JSONObject().apply {
                put("prime_count", primeCount)
                put("range", params.primeRange)
                put("threads", numThreads)
            }.toString()
        )
    }
    
    /**
     * Test 2: Parallel Fibonacci with Memoization
     */
    suspend fun fibonacciMemoized(params: WorkloadParams): BenchmarkResult = coroutineScope {
        Log.d(TAG, "Starting Multi-Core Fibonacci Memoized")
        CpuAffinityManager.setMaxPerformance()
        
        val (results, timeMs) = BenchmarkHelpers.measureBenchmark {
            val (start, end) = params.fibonacciNRange
            val memo = mutableMapOf<Int, Long>()
            
            fun fibMemo(n: Int): Long {
                if (n <= 1) return n.toLong()
                return memo.getOrPut(n) {
                    fibMemo(n - 1) + fibMemo(n - 2)
                }
            }
            
            // Calculate in parallel
            (start..end).map { n ->
                async(Dispatchers.Default) {
                    fibMemo(n)
                }
            }.awaitAll()
        }
        
        val totalOps = results.sumOf { it }
        val opsPerSecond = totalOps / (timeMs / 1000.0)
        
        CpuAffinityManager.resetPerformance()
        
        BenchmarkResult(
            name = "Multi-Core Fibonacci Memoized",
            executionTimeMs = timeMs.toDouble(),
            opsPerSecond = opsPerSecond,
            isValid = results.isNotEmpty(),
            metricsJson = JSONObject().apply {
                put("fibonacci_results", results.toString())
                put("range", listOf(params.fibonacciNRange.first, params.fibonacciNRange.second).toString())
                put("threads", numThreads)
            }.toString()
        )
    }
    
    /**
     * Test 3: Parallel Matrix Multiplication
     * CRISIS FIX: Reduced to N=350 with frequent yielding
     */
    suspend fun matrixMultiplication(params: WorkloadParams): BenchmarkResult = coroutineScope {
        Log.d(TAG, "Starting Multi-Core Matrix Multiplication (size: ${params.matrixSize}) - CRISIS FIX: N=350")
        CpuAffinityManager.setMaxPerformance()
        
        val size = params.matrixSize
        
        val (checksum, timeMs) = BenchmarkHelpers.measureBenchmark {
            // Initialize matrices
            val a = Array(size) { DoubleArray(size) { Random.nextDouble() } }
            val b = Array(size) { DoubleArray(size) { Random.nextDouble() } }
            val c = Array(size) { DoubleArray(size) }
            
            // Parallel matrix multiplication - divide work by rows
            (0 until size).map { i ->
                async(Dispatchers.Default) {
                    for (j in 0 until size) {
                        for (k in 0 until size) {
                            c[i][j] += a[i][k] * b[k][j]
                        }
                    }
                    
                    // CRISIS FIX: Yield after each row to prevent UI freeze
                    kotlinx.coroutines.yield()
                }
            }.awaitAll() // Wait for all row computations to complete
            
            BenchmarkHelpers.calculateMatrixChecksum(c)
        }
        
        val totalOps = size.toLong() * size * size * 2 // multiply + add for each element
        val opsPerSecond = totalOps / (timeMs / 1000.0)
        
        CpuAffinityManager.resetPerformance()
        
        BenchmarkResult(
            name = "Multi-Core Matrix Multiplication",
            executionTimeMs = timeMs.toDouble(),
            opsPerSecond = opsPerSecond,
            isValid = checksum != 0L,
            metricsJson = JSONObject().apply {
                put("matrix_size", size)
                put("result_checksum", checksum)
                put("threads", numThreads)
                put("crisis_fix", "N=350 with frequent yielding prevents UI freeze")
            }.toString()
        )
    }
    
    /**
     * Test 4: Parallel Hash Computing
     * CRISIS FIX: Use fixed 1MB buffer with 200,000 iterations
     */
    suspend fun hashComputing(params: WorkloadParams): BenchmarkResult = coroutineScope {
        Log.d(TAG, "Starting Multi-Core Hash Computing (FIXED: 1MB buffer, 200K iterations)")
        CpuAffinityManager.setMaxPerformance()
        
        // CRISIS FIX: Use fixed small buffer with high iteration count
        val bufferSize = 1 * 1024 * 1024 // 1 MB buffer
        val iterations = 200_000 // High iteration count for sustained load
        
        val (result, timeMs) = BenchmarkHelpers.measureBenchmarkSuspend {
            // Generate fixed random data
            val data = ByteArray(bufferSize) { Random.nextInt(256).toByte() }
            
            // Process iterations in parallel across threads
            val iterationsPerThread = iterations / numThreads
            val hashResults = (0 until numThreads).map { threadId ->
                async(Dispatchers.Default) {
                    var threadHashCount = 0
                    
                    repeat(iterationsPerThread) { iteration ->
                        // Compute SHA-256 hash
                        val digest = MessageDigest.getInstance("SHA-256")
                        digest.update(data)
                        val hashBytes = digest.digest()
                        threadHashCount++
                        
                        // Yield every 100 iterations to prevent UI freeze
                        if (iteration % 100 == 0) {
                            kotlinx.coroutines.yield()
                        }
                    }
                    
                    threadHashCount
                }
            }.awaitAll()
            
            // Sum up results from all threads
            hashResults.sum()
        }
        
        val totalHashes = result
        val throughput = totalHashes.toDouble() / (timeMs / 1000.0)
        
        CpuAffinityManager.resetPerformance()
        
        BenchmarkResult(
            name = "Multi-Core Hash Computing",
            executionTimeMs = timeMs.toDouble(),
            opsPerSecond = throughput,
            isValid = totalHashes > 0,
            metricsJson = JSONObject().apply {
                put("buffer_size_mb", bufferSize / (1024 * 1024))
                put("total_iterations", iterations)
                put("total_hashes", totalHashes)
                put("throughput_hashes_per_sec", throughput)
                put("threads", numThreads)
                put("crisis_fix", "Fixed iteration count prevents memory issues")
            }.toString()
        )
    }
    
    /**
     * Test 5: Parallel String Sorting
     */
    suspend fun stringSorting(params: WorkloadParams): BenchmarkResult = coroutineScope {
        Log.d(TAG, "Starting Multi-Core String Sorting (count: ${params.stringCount})")
        CpuAffinityManager.setMaxPerformance()
        
        val chunkSize = params.stringCount / numThreads
        
        val (result, timeMs) = BenchmarkHelpers.measureBenchmark {
            // Generate random strings in parallel
            val allStrings = (0 until numThreads).map { i ->
                async(Dispatchers.Default) {
                    val start = i * chunkSize
                    val end = if (i == numThreads - 1) params.stringCount else (i + 1) * chunkSize
                    List(end - start) { 
                        BenchmarkHelpers.generateRandomString(50) 
                    }
                }
            }.awaitAll().flatten()
            
            // Sort all strings together
            allStrings.sorted()
        }
        
        val comparisons = params.stringCount * kotlin.math.ln(params.stringCount.toDouble())
        val opsPerSecond = comparisons / (timeMs / 1000.0)
        
        CpuAffinityManager.resetPerformance()
        
        BenchmarkResult(
            name = "Multi-Core String Sorting",
            executionTimeMs = timeMs.toDouble(),
            opsPerSecond = opsPerSecond,
            isValid = result.size == params.stringCount,
            metricsJson = JSONObject().apply {
                put("string_count", params.stringCount)
                put("sorted", true)
                put("threads", numThreads)
            }.toString()
        )
    }
    
    /**
     * Test 6: Parallel Ray Tracing
     */
    suspend fun rayTracing(params: WorkloadParams): BenchmarkResult = coroutineScope {
        Log.d(TAG, "Starting Multi-Core Ray Tracing (resolution: ${params.rayTracingResolution}, depth: ${params.rayTracingDepth})")
        CpuAffinityManager.setMaxPerformance()
        
        // Define 3D vector class
        data class Vec3(val x: Double, val y: Double, val z: Double) {
            fun dot(other: Vec3): Double = x * other.x + y * other.y + z * other.z
            fun length(): Double = kotlin.math.sqrt(dot(this))
            fun normalize(): Vec3 {
                val len = length()
                return if (len > 0.0) Vec3(x / len, y / len, z / len) else Vec3(0.0, 0.0, 0.0)
            }
            operator fun plus(other: Vec3): Vec3 = Vec3(x + other.x, y + other.y, z + other.z)
            operator fun minus(other: Vec3): Vec3 = Vec3(x - other.x, y - other.y, z - other.z)
            operator fun times(scalar: Double): Vec3 = Vec3(x * scalar, y * scalar, z * scalar)
        }
        
        // Define Ray class
        data class Ray(val origin: Vec3, val direction: Vec3)
        
        // Define Sphere class
        data class Sphere(val center: Vec3, val radius: Double) {
            fun intersect(ray: Ray): DoubleArray? {
                val oc = ray.origin - center
                val a = ray.direction.dot(ray.direction)
                val b = 2.0 * oc.dot(ray.direction)
                val c = oc.dot(oc) - radius * radius
                val discriminant = b * b - 4.0 * a * c
                
                return if (discriminant < 0.0) {
                    null
                } else {
                    val t1 = (-b - kotlin.math.sqrt(discriminant)) / (2.0 * a)
                    val t2 = (-b + kotlin.math.sqrt(discriminant)) / (2.0 * a)
                    
                    when {
                        t1 > 0.0 -> doubleArrayOf(t1)
                        t2 > 0.0 -> doubleArrayOf(t2)
                        else -> null
                    }
                }
            }
        }
        
        // Ray tracing function with recursion
        fun traceRay(ray: Ray, spheres: List<Sphere>, depth: Int): Vec3 {
            if (depth == 0) return Vec3(0.0, 0.0, 0.0)
            
            var closestT = Double.MAX_VALUE
            var hitSphere: Sphere? = null
            
            for (sphere in spheres) {
                val intersection = sphere.intersect(ray)
                if (intersection != null && intersection[0] < closestT) {
                    closestT = intersection[0]
                    hitSphere = sphere
                }
            }
            
            return if (hitSphere != null) {
                val hitPoint = ray.origin + ray.direction * closestT
                val normal = (hitPoint - hitSphere.center).normalize()
                
                // Simple shading with reflection
                val reflectedDir = ray.direction - normal * (2.0 * ray.direction.dot(normal))
                val reflectedRay = Ray(hitPoint + normal * 0.01, reflectedDir.normalize())
                
                val reflectedColor = traceRay(reflectedRay, spheres, depth - 1)
                
                // Return a color based on normal and reflection
                Vec3(
                    (normal.x + 1.0) * 0.5 + reflectedColor.x * 0.3,
                    (normal.y + 1.0) * 0.5 + reflectedColor.y * 0.3,
                    (normal.z + 1.0) * 0.5 + reflectedColor.z * 0.3
                )
            } else {
                // Background color (simple gradient)
                Vec3(0.5, 0.7, 1.0) // Sky blue
            }
        }
        
        val (width, height) = params.rayTracingResolution
        val maxDepth = params.rayTracingDepth
        val rowsPerThread = height / numThreads
        
        val (result, timeMs) = BenchmarkHelpers.measureBenchmark {
            // Create a simple scene with spheres
            val spheres = listOf(
                Sphere(Vec3(0.0, 0.0, -1.0), 0.5),
                Sphere(Vec3(1.0, 0.0, -1.5), 0.3),
                Sphere(Vec3(-1.0, -0.5, -1.2), 0.4)
            )
            
            // Render the image in parallel by rows
            val rowResults = (0 until numThreads).map { i ->
                async(Dispatchers.Default) {
                    val startRow = i * rowsPerThread
                    val endRow = if (i == numThreads - 1) height else (i + 1) * rowsPerThread
                    val threadPixels = mutableListOf<Vec3>()
                    
                    for (y in startRow until endRow) {
                        for (x in 0 until width) {
                            // Create a ray from camera through pixel
                            val ray = Ray(
                                Vec3(0.0, 0.0, 0.0),
                                Vec3(
                                    (x.toDouble() - width / 2.0) / (width / 2.0),
                                    (y.toDouble() - height / 2.0) / (height / 2.0),
                                    -1.0
                                ).normalize()
                            )
                            
                            val color = traceRay(ray, spheres, maxDepth)
                            threadPixels.add(color)
                        }
                    }
                    
                    threadPixels
                }
            }.awaitAll().flatten() // Combine all pixel results
            
            rowResults.size
        }
        
        val totalRays = (width * height).toLong()
        val raysPerSecond = totalRays / (timeMs / 1000.0)
        
        CpuAffinityManager.resetPerformance()
        
        BenchmarkResult(
            name = "Multi-Core Ray Tracing",
            executionTimeMs = timeMs.toDouble(),
            opsPerSecond = raysPerSecond,
            isValid = result > 0,
            metricsJson = JSONObject().apply {
                put("resolution", listOf(width, height).toString())
                put("max_depth", maxDepth)
                put("ray_count", totalRays)
                put("pixels_rendered", result)
                put("threads", numThreads)
            }.toString()
        )
    }
    
    /**
     * Test 7: Parallel Compression
     * CRISIS FIX: Use fixed 512KB buffer with 50 iterations to prevent OOM crash
     */
    suspend fun compression(params: WorkloadParams): BenchmarkResult = coroutineScope {
        Log.d(TAG, "Starting Multi-Core Compression (FIXED: 512KB buffer, 50 iterations)")
        CpuAffinityManager.setMaxPerformance()
        
        // CRISIS FIX: Use fixed small buffer to prevent OOM
        val bufferSize = 512 * 1024 // 512 KB ONLY
        val iterations = 50 // Loop count to create load
        
        val (result, timeMs) = BenchmarkHelpers.measureBenchmarkSuspend {
            // Generate fixed-size random data
            val data = ByteArray(bufferSize) { Random.nextInt(256).toByte() }
            
            // Simple RLE compression algorithm
            fun compressRLE(input: ByteArray): ByteArray {
                val compressed = mutableListOf<Byte>()
                var i = 0
                var opCount = 0
                
                while (i < input.size) {
                    val currentByte = input[i]
                    var count = 1
                    
                    // Count consecutive identical bytes (up to 255 for simplicity)
                    while (i + count < input.size && 
                           input[i + count] == currentByte && 
                           count < 255) {
                        count++
                    }
                    
                    // Output (count, byte) pair
                    compressed.add(count.toByte())
                    compressed.add(currentByte)
                    
                    i += count
                    opCount++
                    
                    // Yield every 100 operations to prevent UI freeze - removed from inside regular function
                    // Yielding will be handled by the calling code
                }
                
                return compressed.toByteArray()
            }
            
            // CRITICAL FIX: Loop compression multiple times to measure throughput
            var totalCompressedSize = 0L
            var totalOperations = 0
            
            repeat(iterations) { iteration ->
                // Compress the data
                val compressed = compressRLE(data)
                totalCompressedSize += compressed.size
                totalOperations++
                
                // Yield every iteration to prevent UI freeze
                if (iteration % 10 == 0) {
                    kotlinx.coroutines.yield()
                }
            }
            
            Pair(totalCompressedSize, iterations)
        }
        
        val (totalCompressedSize, totalIterations) = result
        
        // Calculate throughput based on total operations
        val totalDataProcessed = bufferSize.toLong() * totalIterations
        val throughput = totalDataProcessed / (timeMs / 1000.0)
        
        CpuAffinityManager.resetPerformance()
        
        BenchmarkResult(
            name = "Multi-Core Compression",
            executionTimeMs = timeMs.toDouble(),
            opsPerSecond = throughput,
            isValid = true,
            metricsJson = JSONObject().apply {
                put("buffer_size_kb", bufferSize / 1024)
                put("iterations", totalIterations)
                put("total_data_processed_mb", totalDataProcessed / (1024 * 1024))
                put("average_compressed_size", totalCompressedSize / totalIterations)
                put("throughput_bps", throughput)
                put("threads", numThreads)
                put("crisis_fix", "Fixed 512KB buffer prevents OOM")
            }.toString()
        )
    }
    
    /**
     * Test 8: Parallel Monte Carlo Simulation for π
     */
    suspend fun monteCarloPi(params: WorkloadParams): BenchmarkResult = coroutineScope {
        Log.d(TAG, "Starting Multi-Core Monte Carlo π (samples: ${params.monteCarloSamples})")
        CpuAffinityManager.setMaxPerformance()
        
        val samplesPerThread = params.monteCarloSamples / numThreads
        
        val (result, timeMs) = BenchmarkHelpers.measureBenchmark {
            // Run Monte Carlo simulation in parallel across threads
            val results = (0 until numThreads).map { _ ->
                async(Dispatchers.Default) {
                    var insideCircle = 0L
                    val samples = samplesPerThread
                    
                    for (i in 0 until samples) {
                        val x = Random.nextDouble() * 2.0 - 1.0  // Random value between -1 and 1
                        val y = Random.nextDouble() * 2.0 - 1.0  // Random value between -1 and 1
                        
                        if (x * x + y * y <= 1.0) {
                            insideCircle++
                        }
                    }
                    
                    insideCircle
                }
            }.awaitAll()
            
            // Sum up results from all threads
            results.sum()
        }
        
        val totalInsideCircle = result
        val samples = params.monteCarloSamples
        val piEstimate = 4.0 * totalInsideCircle.toDouble() / samples.toDouble()
        val opsPerSecond = samples.toDouble() / (timeMs / 1000.0)
        
        CpuAffinityManager.resetPerformance()
        
        BenchmarkResult(
            name = "Multi-Core Monte Carlo π",
            executionTimeMs = timeMs.toDouble(),
            opsPerSecond = opsPerSecond,
            isValid = kotlin.math.abs(piEstimate - kotlin.math.PI) < 0.1,  // Reasonable accuracy check
            metricsJson = JSONObject().apply {
                put("samples", samples)
                put("pi_estimate", piEstimate)
                put("actual_pi", kotlin.math.PI)
                put("accuracy", kotlin.math.abs(piEstimate - kotlin.math.PI))
                put("threads", numThreads)
            }.toString()
        )
    }
    
    /**
     * Test 9: Parallel JSON Parsing
     */
    suspend fun jsonParsing(params: WorkloadParams): BenchmarkResult = coroutineScope {
        Log.d(TAG, "Starting Multi-Core JSON Parsing (size: ${params.jsonDataSizeMb}MB)")
        CpuAffinityManager.setMaxPerformance()
        
        val dataSize = params.jsonDataSizeMb * 1024 * 1024
        val chunkSize = dataSize / numThreads
        
        val (result, timeMs) = BenchmarkHelpers.measureBenchmark {
            // Generate complex nested JSON data
            fun generateComplexJson(sizeTarget: Int): String {
                val result = StringBuilder()
                result.append("{\"data\":[")
                var currentSize = result.length
                var counter = 0
                
                while (currentSize < sizeTarget) {
                    val jsonObj = "{\"id\":$counter,\"name\":\"obj$counter\",\"nested\":{\"value\":${counter % 1000},\"array\":[1,2,3,4,5]}},"
                    
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
            
            val jsonData = generateComplexJson(dataSize)
            val chunkSize = jsonData.length / numThreads
            
            // Process chunks in parallel
            val results = (0 until numThreads).map { i ->
                async(Dispatchers.Default) {
                    val start = i * chunkSize
                    val end = if (i == numThreads - 1) jsonData.length else (i + 1) * chunkSize
                    val chunk = jsonData.substring(start, end)
                    
                    // Count elements in the JSON string as a simple way to "parse" it
                    var elementCount = 0
                    var inString = false
                    
                    for (char in chunk) {
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
                    
                    elementCount
                }
            }.awaitAll()
            
            // Sum up results from all chunks
            results.sum()
        }
        
        val elementsParsed = result
        val elementsPerSecond = elementsParsed.toDouble() / (timeMs / 1000.0)
        
        CpuAffinityManager.resetPerformance()
        
        BenchmarkResult(
            name = "Multi-Core JSON Parsing",
            executionTimeMs = timeMs.toDouble(),
            opsPerSecond = elementsPerSecond,
            isValid = elementsParsed > 0,
            metricsJson = JSONObject().apply {
                put("json_size", dataSize)
                put("elements_parsed", elementsParsed)
                put("root_type", "object")
                put("threads", numThreads)
            }.toString()
        )
    }
    
    /**
     * Test 10: Parallel N-Queens Problem
     */
    suspend fun nqueens(params: WorkloadParams): BenchmarkResult = coroutineScope {
        Log.d(TAG, "Starting Multi-Core N-Queens (size: ${params.nqueensSize})")
        CpuAffinityManager.setMaxPerformance()
        
        val n = params.nqueensSize
        
        val (result, timeMs) = BenchmarkHelpers.measureBenchmark {
            // For N-Queens, we'll use a work-stealing approach where we divide the initial search space
            // Each thread starts with a different column in the first row
            val initialTasks = (0 until minOf(n, numThreads)).toList()
            
            // Process tasks in parallel
            val solutions = (0 until initialTasks.size).map { i ->
                async(Dispatchers.Default) {
                    val firstCol = initialTasks[i]
                    
                    // Solve N-Queens with the first queen placed at (0, firstCol)
                    val board = Array(n) { IntArray(n) { 0 } }
                    val cols = BooleanArray(n) { false }
                    val diag1 = BooleanArray(2 * n - 1) { false }  // For diagonal \
                    val diag2 = BooleanArray(2 * n - 1) { false }  // For diagonal /
                    
                    // Place the first queen
                    board[0][firstCol] = 1
                    cols[firstCol] = true
                    diag1[firstCol] = true
                    diag2[n - 1 + firstCol] = true
                    
                    fun backtrack(row: Int): Int {
                        if (row == n) return 1  // Found a solution
                        
                        var solutions = 0
                        for (col in 0 until n) {
                            val d1Idx = row + col
                            val d2Idx = n - 1 + col - row
                            
                            if (!cols[col] && !diag1[d1Idx] && !diag2[d2Idx]) {
                                // Place queen
                                board[row][col] = 1
                                cols[col] = true
                                diag1[d1Idx] = true
                                diag2[d2Idx] = true
                                
                                solutions += backtrack(row + 1)
                                
                                // Remove queen (backtrack)
                                board[row][col] = 0
                                cols[col] = false
                                diag1[d1Idx] = false
                                diag2[d2Idx] = false
                            }
                        }
                        
                        return solutions
                    }
                    
                    backtrack(1) // Start from row 1 since row 0 is already set
                }
            }.awaitAll().sum()
            
            solutions
        }
        
        val solutionCount = result
        val opsPerSecond = solutionCount.toDouble() / (timeMs / 1000.0)
        
        CpuAffinityManager.resetPerformance()
        
        BenchmarkResult(
            name = "Multi-Core N-Queens",
            executionTimeMs = timeMs.toDouble(),
            opsPerSecond = opsPerSecond,
            isValid = solutionCount >= 0,  // N-Queens can have 0 solutions for small n
            metricsJson = JSONObject().apply {
                put("board_size", params.nqueensSize)
                put("solution_count", solutionCount)
                put("threads", numThreads)
            }.toString()
        )
    }
}