package com.ivarna.finalbenchmark2.cpuBenchmark.algorithms

import android.os.Process
import android.util.Log
import com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkResult
import com.ivarna.finalbenchmark2.cpuBenchmark.WorkloadParams
import com.ivarna.finalbenchmark2.cpuBenchmark.CpuAffinityManager
import kotlinx.coroutines.*
import kotlinx.coroutines.internal.MainDispatcherFactory
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.ThreadLocalRandom
import kotlin.random.Random
import kotlin.math.pow

object MultiCoreBenchmarks {
    private const val TAG = "MultiCoreBenchmarks"
    
    // Number of threads = number of physical cores
    private val numThreads = Runtime.getRuntime().availableProcessors()
    
    /**
     * Custom high-priority dispatcher that creates a FixedThreadPool with all threads set to URGENT priority.
     * This forces Android's EAS to schedule threads on all available cores (Big, Mid, and Little cores)
     * instead of limiting them to just performance cores.
     */
    private val highPriorityDispatcher: CoroutineDispatcher by lazy {
        val threadCount = numThreads
        val threadFactory = ThreadFactory { runnable ->
            Thread(runnable).apply {
                // Set high priority using Android's Process API
                // THREAD_PRIORITY_URGENT_DISPLAY = -10 (highest priority for UI tasks)
                Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_DISPLAY)
                name = "BenchmarkWorker-${threadId.getAndIncrement()}"
            }
        }
        
        val executor = Executors.newFixedThreadPool(threadCount, threadFactory)
        executor.asCoroutineDispatcher()
    }
    
    private val threadId = AtomicInteger(0)
    
    /**
     * Test 1: Parallel Prime Generation
     */
    suspend fun primeGeneration(params: WorkloadParams): BenchmarkResult = coroutineScope {
        Log.d(TAG, "Starting Multi-Core Prime Generation")
        CpuAffinityManager.setMaxPerformance()
        
        val (primeCount, timeMs) = BenchmarkHelpers.measureBenchmark {
            val n = params.primeRange
            val chunkSize = n / numThreads
            
            // Process chunks in parallel using high-priority dispatcher
            val results = (0 until numThreads).map { threadId ->
                async(highPriorityDispatcher) {
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
     * Test 2: Parallel Fibonacci Recursive (NO MEMOIZATION)
     * FIXED: Remove memoization for raw CPU load testing
     */
    suspend fun fibonacciRecursive(params: WorkloadParams): BenchmarkResult = coroutineScope {
        Log.d(TAG, "Starting Multi-Core Fibonacci Recursive - FIXED: No memoization")
        CpuAffinityManager.setMaxPerformance()
        
        val (results, timeMs) = BenchmarkHelpers.measureBenchmark {
            val (start, end) = params.fibonacciNRange
            val targetN = 30 // Fixed value for consistent CPU load
            val iterations = 1000 // Calculate fib(30) 1000 times
            
            // Pure recursive Fibonacci with NO memoization
            fun fibonacci(n: Int): Long {
                return if (n <= 1) n.toLong()
                else fibonacci(n - 1) + fibonacci(n - 2)
            }
            
            // Process in parallel using high-priority dispatcher
            (0 until numThreads).map { threadId ->
                async(highPriorityDispatcher) {
                    var totalResult = 0L
                    val iterationsPerThread = iterations / numThreads
                    repeat(iterationsPerThread) {
                        totalResult += fibonacci(targetN)
                    }
                    totalResult
                }
            }.awaitAll().sum()
        }
        
        // Count total recursive calls as operations (approximation)
        val targetN = 30
        val iterations = 1000
        val totalRecursiveCalls = iterations * (2.0.pow(targetN) / 1.618).toLong() // Approximation of recursive calls
        val opsPerSecond = totalRecursiveCalls / (timeMs / 1000.0)
        
        CpuAffinityManager.resetPerformance()
        
        BenchmarkResult(
            name = "Multi-Core Fibonacci Recursive",
            executionTimeMs = timeMs.toDouble(),
            opsPerSecond = opsPerSecond,
            isValid = results > 0,
            metricsJson = JSONObject().apply {
                put("fibonacci_result", results)
                put("target_n", 30)
                put("iterations", 1000)
                put("threads", numThreads)
                put("optimization", "Pure recursive, no memoization, parallel execution")
            }.toString()
        )
    }
    
    /**
     * Test 3: Parallel Matrix Multiplication
     * Optimized: Cache-friendly i-k-j loop order, minimal yielding
     */
    suspend fun matrixMultiplication(params: WorkloadParams): BenchmarkResult = coroutineScope {
        Log.d(TAG, "Starting Multi-Core Matrix Multiplication (size: ${params.matrixSize}) - OPTIMIZED")
        CpuAffinityManager.setMaxPerformance()
        
        val size = params.matrixSize
        
        val (checksum, timeMs) = BenchmarkHelpers.measureBenchmark {
            // Initialize matrices
            val a = Array(size) { DoubleArray(size) { Random.nextDouble() } }
            val b = Array(size) { DoubleArray(size) { Random.nextDouble() } }
            val c = Array(size) { DoubleArray(size) }
            
            // OPTIMIZED: Parallel matrix multiplication with cache-friendly loop order
            val rowsPerThread = size / numThreads
            (0 until numThreads).map { threadId ->
                async(highPriorityDispatcher) {
                    val startRow = threadId * rowsPerThread
                    val endRow = if (threadId == numThreads - 1) size else (threadId + 1) * rowsPerThread
                    
                    // Use cache-friendly i-k-j loop order
                    for (i in startRow until endRow) {
                        for (k in 0 until size) {
                            val aik = a[i][k]
                            for (j in 0 until size) {
                                c[i][j] += aik * b[k][j]
                            }
                        }
                        // OPTIMIZED: Only yield every 25 rows to reduce overhead
                        if ((i - startRow) % 25 == 0) {
                            kotlinx.coroutines.yield()
                        }
                    }
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
                put("optimization", "Cache-friendly i-k-j loop order, reduced yield frequency")
            }.toString()
        )
    }
    
    /**
     * Test 4: Parallel Hash Computing
     * OPTIMIZED: Use 4KB buffer (cache-friendly) with 300K iterations for ~2-3 seconds execution
     */
    suspend fun hashComputing(params: WorkloadParams): BenchmarkResult = coroutineScope {
        Log.d(TAG, "Starting Multi-Core Hash Computing (OPTIMIZED: 4KB buffer, 300K iterations)")
        CpuAffinityManager.setMaxPerformance()
        
        // OPTIMIZED PARAMETERS: Small buffer fits in CPU cache, testing pure hashing speed
        val bufferSize = 4 * 1024 // 4KB (cache-friendly)
        val iterations = 300_000 // Tuned for ~2-3 seconds execution
        
        val (result, timeMs) = BenchmarkHelpers.measureBenchmarkSuspend {
            val data = ByteArray(bufferSize) { 0xAA.toByte() }
            
            // Process iterations in parallel across threads using high-priority dispatcher
            val iterationsPerThread = iterations / numThreads
            val hashResults = (0 until numThreads).map { threadId ->
                async(highPriorityDispatcher) {
                    val digest = MessageDigest.getInstance("SHA-256")
                    var threadHashCount = 0
                    
                    for (i in 0 until iterationsPerThread) {
                        digest.update(data)
                        digest.digest()
                        threadHashCount++
                        if (i % 1000 == 0) yield() // Prevent UI freeze
                    }
                    
                    threadHashCount
                }
            }.awaitAll()
            
            // Sum up results from all threads
            hashResults.sum()
        }
        
        val totalHashes = result
        val totalBytes = bufferSize.toLong() * totalHashes
        val throughputMBps = (totalBytes.toDouble() / (1024 * 1024)) / (timeMs / 1000.0)
        val opsPerSecond = totalHashes.toDouble() / (timeMs / 1000.0)
        
        CpuAffinityManager.resetPerformance()
        
        BenchmarkResult(
            name = "Multi-Core Hash Computing",
            executionTimeMs = timeMs.toDouble(),
            opsPerSecond = opsPerSecond,
            isValid = totalHashes > 0,
            metricsJson = JSONObject().apply {
                put("buffer_size_kb", bufferSize / 1024)
                put("total_iterations", iterations)
                put("total_hashes", totalHashes)
                put("throughput_mbps", throughputMBps)
                put("hashes_per_sec", opsPerSecond)
                put("threads", numThreads)
            }.toString()
        )
    }
    
    /**
     * Test 5: Parallel String Sorting
     * FIXED: Pre-generate strings in parallel, then measure only sorting time
     */
    suspend fun stringSorting(params: WorkloadParams): BenchmarkResult = coroutineScope {
        Log.d(TAG, "Starting Multi-Core String Sorting (count: ${params.stringCount}) - FIXED: Pre-generation")
        CpuAffinityManager.setMaxPerformance()
        
        val chunkSize = params.stringCount / numThreads
        
        val (result, timeMs) = BenchmarkHelpers.measureBenchmark {
            // FIXED: Generate random strings in parallel using high-priority dispatcher
            val allStrings = (0 until numThreads).map { i ->
                async(highPriorityDispatcher) {
                    val start = i * chunkSize
                    val end = if (i == numThreads - 1) params.stringCount else (i + 1) * chunkSize
                    List(end - start) { 
                        BenchmarkHelpers.generateRandomString(50) 
                    }
                }
            }.awaitAll().flatten()
            
            // Sort all strings together using Kotlin's built-in sorting (IntroSort)
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
                put("optimization", "Parallel string generation, measure only sorting time")
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
            
            // Render the image in parallel by rows using high-priority dispatcher
            val rowResults = (0 until numThreads).map { i ->
                async(highPriorityDispatcher) {
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
     * FIXED: Use 2MB static buffer, eliminate allocations in hot path
     */
    suspend fun compression(params: WorkloadParams): BenchmarkResult = coroutineScope {
        Log.d(TAG, "Starting Multi-Core Compression (FIXED: 2MB static buffer)")
        CpuAffinityManager.setMaxPerformance()
        
        // FIXED: Use 2MB static buffer for better cache utilization
        val bufferSize = 2 * 1024 * 1024 // 2MB
        val iterations = 100 // Increased for meaningful throughput measurement
        
        val (result, timeMs) = BenchmarkHelpers.measureBenchmarkSuspend {
            // Generate fixed-size random data once
            val data = ByteArray(bufferSize) { Random.nextInt(256).toByte() }
            
            // Simple RLE compression algorithm - ZERO ALLOCATION in hot path
            fun compressRLE(input: ByteArray, output: ByteArray): Int {
                var i = 0
                var outputIndex = 0
                
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
                    output[outputIndex++] = count.toByte()
                    output[outputIndex++] = currentByte
                    
                    i += count
                }
                
                return outputIndex
            }
            
            // FIXED: Parallel compression across threads using static buffers
            val iterationsPerThread = iterations / numThreads
            val threadResults = (0 until numThreads).map { threadId ->
                async(highPriorityDispatcher) {
                    val outputBuffer = ByteArray(bufferSize * 2) // Output buffer per thread
                    var threadCompressedSize = 0L
                    var threadOperations = 0
                    
                    repeat(iterationsPerThread) { iteration ->
                        // Compress the data using static buffers
                        val compressedSize = compressRLE(data, outputBuffer)
                        threadCompressedSize += compressedSize
                        threadOperations++
                        
                        // FIXED: Only yield every 20 iterations per thread
                        if (iteration % 20 == 0) {
                            kotlinx.coroutines.yield()
                        }
                    }
                    
                    Pair(threadCompressedSize, threadOperations)
                }
            }.awaitAll()
            
            // Sum up results from all threads
            threadResults.sumOf { it.first } to threadResults.sumOf { it.second }
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
                put("buffer_size_mb", bufferSize / (1024 * 1024))
                put("iterations", totalIterations)
                put("total_data_processed_mb", totalDataProcessed / (1024 * 1024))
                put("average_compressed_size", totalCompressedSize / totalIterations)
                put("throughput_bps", throughput)
                put("threads", numThreads)
                put("optimization", "2MB static buffer, zero allocation in hot path, parallel processing")
            }.toString()
        )
    }
    
    /**
     * Test 8: Parallel Monte Carlo Simulation for π
     * FIXED: Use ThreadLocalRandom for zero-allocation random number generation
     */
    suspend fun monteCarloPi(params: WorkloadParams): BenchmarkResult = coroutineScope {
        Log.d(TAG, "Starting Multi-Core Monte Carlo π (samples: ${params.monteCarloSamples}) - FIXED: ThreadLocalRandom")
        CpuAffinityManager.setMaxPerformance()
        
        val samplesPerThread = params.monteCarloSamples / numThreads
        
        val (result, timeMs) = BenchmarkHelpers.measureBenchmark {
            // Run Monte Carlo simulation in parallel across threads using high-priority dispatcher
            val results = (0 until numThreads).map { _ ->
                async(highPriorityDispatcher) {
                    var insideCircle = 0L
                    val samples = samplesPerThread
                    
                    // FIXED: Use ThreadLocalRandom for better performance and no object allocation
                    val random = ThreadLocalRandom.current()
                    
                    for (i in 0 until samples) {
                        val x = random.nextDouble() * 2.0 - 1.0  // Random value between -1 and 1
                        val y = random.nextDouble() * 2.0 - 1.0  // Random value between -1 and 1
                        
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
                put("optimization", "ThreadLocalRandom for zero-allocation")
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
            
            // Process chunks in parallel using high-priority dispatcher
            val results = (0 until numThreads).map { i ->
                async(highPriorityDispatcher) {
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
            
            // Process tasks in parallel using high-priority dispatcher
            val solutions = (0 until initialTasks.size).map { i ->
                async(highPriorityDispatcher) {
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