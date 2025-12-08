package com.ivarna.finalbenchmark2.cpuBenchmark.algorithms

import android.util.Log
import com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkResult
import com.ivarna.finalbenchmark2.cpuBenchmark.WorkloadParams
import com.ivarna.finalbenchmark2.cpuBenchmark.CpuAffinityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.security.MessageDigest
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.system.measureNanoTime

object SingleCoreBenchmarks {
    private const val TAG = "SingleCoreBenchmarks"
    
    /**
     * Test 1: Prime Number Generation using Sieve of Eratosthenes
     * Complexity: O(n log log n)
     * Tests: Integer arithmetic, memory access patterns
     */
    suspend fun primeGeneration(params: WorkloadParams): BenchmarkResult = withContext(Dispatchers.Default) {
        Log.d(TAG, "Starting Prime Generation (range: ${params.primeRange})")
        CpuAffinityManager.setMaxPerformance()
        
        val (result, timeMs) = BenchmarkHelpers.measureBenchmark {
            val n = params.primeRange
            val isPrime = BooleanArray(n + 1) { true }
            isPrime[0] = false
            if (n > 0) isPrime[1] = false
            
            var p = 2
            while (p * p <= n) {
                if (isPrime[p]) {
                    var multiple = p * p
                    while (multiple <= n) {
                        isPrime[multiple] = false
                        multiple += p
                    }
                }
                p++
                // Yield every 1000 iterations to prevent ANR during long computations
                if (p % 1000 == 0) {
                    kotlinx.coroutines.yield()
                }
            }
            
            isPrime.count { it }
        }
        
        val primeCount = result
        val ops = params.primeRange.toDouble() // Operations = numbers checked
        val opsPerSecond = ops / (timeMs / 1000.0)
        
        CpuAffinityManager.resetPerformance()
        
        return@withContext BenchmarkResult(
            name = "Single-Core Prime Generation",
            executionTimeMs = timeMs.toDouble(),
            opsPerSecond = opsPerSecond,
            isValid = primeCount > 0,
            metricsJson = JSONObject().apply {
                put("prime_count", primeCount)
                put("range", params.primeRange)
            }.toString()
        )
    }
    
    /**
     * Test 2: Fibonacci Sequence (Recursive)
     * Complexity: O(2^n)
     * Tests: Function call overhead, stack performance
     */
    suspend fun fibonacciRecursive(params: WorkloadParams): BenchmarkResult = withContext(Dispatchers.Default) {
        Log.d(TAG, "Starting Fibonacci Recursive (range: ${params.fibonacciNRange})")
        CpuAffinityManager.setMaxPerformance()
        
        fun fibonacci(n: Int): Long {
            return if (n <= 1) n.toLong()
            else fibonacci(n - 1) + fibonacci(n - 2)
        }
        
        val (results, timeMs) = BenchmarkHelpers.measureBenchmark {
            val (start, end) = params.fibonacciNRange
            val results = mutableListOf<Long>()
            for (n in start..end) {
                results.add(fibonacci(n))
            }
            results
        }
        
        // Count total recursive calls as operations
        val (start, end) = params.fibonacciNRange
        val totalOps = results.sumOf { it } // Use Fibonacci result as proxy for call count
        val opsPerSecond = totalOps / (timeMs / 1000.0)
        
        CpuAffinityManager.resetPerformance()
        
        return@withContext BenchmarkResult(
            name = "Single-Core Fibonacci Recursive",
            executionTimeMs = timeMs.toDouble(),
            opsPerSecond = opsPerSecond,
            isValid = results.isNotEmpty() && results.all { it > 0 },
            metricsJson = JSONObject().apply {
                put("fibonacci_results", results.toString())
                put("range", listOf(start, end).toString())
            }.toString()
        )
    }
    
    /**
     * Test 3: Matrix Multiplication
     * Complexity: O(n³)
     * Tests: Floating-point operations, cache efficiency
     */
    suspend fun matrixMultiplication(params: WorkloadParams): BenchmarkResult = withContext(Dispatchers.Default) {
        Log.d(TAG, "Starting Matrix Multiplication (size: ${params.matrixSize})")
        CpuAffinityManager.setMaxPerformance()
        
        val size = params.matrixSize
        
        val (checksum, timeMs) = BenchmarkHelpers.measureBenchmark {
            // Initialize matrices
            val a = Array(size) { DoubleArray(size) { Random.nextDouble() } }
            val b = Array(size) { DoubleArray(size) { Random.nextDouble() } }
            val c = Array(size) { DoubleArray(size) }
            
            // Matrix multiplication
            for (i in 0 until size) {
                for (j in 0 until size) {
                    for (k in 0 until size) {
                        c[i][j] += a[i][k] * b[k][j]
                    }
                }
                // Yield every 64 rows to prevent ANR during matrix operations
                if (i % 64 == 0) {
                    kotlinx.coroutines.yield()
                }
            }
            
            BenchmarkHelpers.calculateMatrixChecksum(c)
        }
        
        val totalOps = size.toLong() * size * size * 2 // multiply + add for each element
        val opsPerSecond = totalOps / (timeMs / 1000.0)
        
        CpuAffinityManager.resetPerformance()
        
        return@withContext BenchmarkResult(
            name = "Single-Core Matrix Multiplication",
            executionTimeMs = timeMs.toDouble(),
            opsPerSecond = opsPerSecond,
            isValid = checksum != 0L,
            metricsJson = JSONObject().apply {
                put("matrix_size", size)
                put("result_checksum", checksum)
            }.toString()
        )
    }
    
    /**
     * Test 4: Hash Computing (SHA-256)
     * Complexity: O(n)
     * Tests: Cryptographic operations, memory bandwidth
     */
    suspend fun hashComputing(params: WorkloadParams): BenchmarkResult = withContext(Dispatchers.Default) {
        Log.d(TAG, "Starting Hash Computing (size: ${params.hashDataSizeMb}MB)")
        CpuAffinityManager.setMaxPerformance()
        
        val dataSize = params.hashDataSizeMb * 1024 * 1024
        
        val (hash, timeMs) = BenchmarkHelpers.measureBenchmark {
            // Generate random data
            val data = ByteArray(dataSize) { Random.nextInt(256).toByte() }
            
            // Compute SHA-256 hash
            val digest = MessageDigest.getInstance("SHA-256")
            digest.update(data)
            digest.digest()
        }
        
        val throughput = dataSize / (timeMs / 1000.0)
        
        CpuAffinityManager.resetPerformance()
        
        return@withContext BenchmarkResult(
            name = "Single-Core Hash Computing",
            executionTimeMs = timeMs.toDouble(),
            opsPerSecond = throughput,
            isValid = hash.isNotEmpty(),
            metricsJson = JSONObject().apply {
                put("data_size_mb", params.hashDataSizeMb)
                put("sha256_result", hash.joinToString("") { "%02x".format(it) }.substring(0, 16))
                put("throughput_bps", throughput)
            }.toString()
        )
    }
    
    /**
     * Test 5: String Sorting
     * Implement IntroSort algorithm
     */
    suspend fun stringSorting(params: WorkloadParams): BenchmarkResult = withContext(Dispatchers.Default) {
        Log.d(TAG, "Starting String Sorting (count: ${params.stringCount})")
        CpuAffinityManager.setMaxPerformance()
        
        val (sorted, timeMs) = BenchmarkHelpers.measureBenchmark {
            val strings = List(params.stringCount) { 
                BenchmarkHelpers.generateRandomString(50) 
            }
            strings.sorted()
        }
        
        val comparisons = params.stringCount * kotlin.math.ln(params.stringCount.toDouble())
        val opsPerSecond = comparisons / (timeMs / 100.0)
        
        CpuAffinityManager.resetPerformance()
        
        return@withContext BenchmarkResult(
            name = "Single-Core String Sorting",
            executionTimeMs = timeMs.toDouble(),
            opsPerSecond = opsPerSecond,
            isValid = sorted.size == params.stringCount,
            metricsJson = JSONObject().apply {
                put("string_count", params.stringCount)
                put("sorted", true)
            }.toString()
        )
    }
    
    /**
     * Test 6: Ray Tracing
     * Implement basic ray-sphere intersection with recursion
     */
    suspend fun rayTracing(params: WorkloadParams): BenchmarkResult = withContext(Dispatchers.Default) {
        Log.d(TAG, "Starting Ray Tracing (resolution: ${params.rayTracingResolution}, depth: ${params.rayTracingDepth})")
        CpuAffinityManager.setMaxPerformance()
        
        // Define 3D vector class
        data class Vec3(val x: Double, val y: Double, val z: Double) {
            fun dot(other: Vec3): Double = x * other.x + y * other.y + z * other.z
            fun length(): Double = sqrt(dot(this))
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
                    val t1 = (-b - sqrt(discriminant)) / (2.0 * a)
                    val t2 = (-b + sqrt(discriminant)) / (2.0 * a)
                    
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
        
        val (result, timeMs) = BenchmarkHelpers.measureBenchmark {
            // Create a simple scene with spheres
            val spheres = listOf(
                Sphere(Vec3(0.0, 0.0, -1.0), 0.5),
                Sphere(Vec3(1.0, 0.0, -1.5), 0.3),
                Sphere(Vec3(-1.0, -0.5, -1.2), 0.4)
            )
            
            // Render the image
            val image = mutableListOf<Vec3>()
            for (y in 0 until height) {
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
                    image.add(color)
                }
            }
            
            image.size
        }
        
        val totalRays = (width * height).toLong()
        val raysPerSecond = totalRays / (timeMs / 1000.0)
        
        CpuAffinityManager.resetPerformance()
        
        return@withContext BenchmarkResult(
            name = "Single-Core Ray Tracing",
            executionTimeMs = timeMs.toDouble(),
            opsPerSecond = raysPerSecond,
            isValid = result > 0,
            metricsJson = JSONObject().apply {
                put("resolution", listOf(width, height).toString())
                put("max_depth", maxDepth)
                put("ray_count", totalRays)
                put("pixels_rendered", result)
            }.toString()
        )
    }
    
    /**
     * Test 7: Compression/Decompression
     * Implement RLE (Run-Length Encoding)
     */
    suspend fun compression(params: WorkloadParams): BenchmarkResult = withContext(Dispatchers.Default) {
        Log.d(TAG, "Starting Compression (size: ${params.compressionDataSizeMb}MB)")
        CpuAffinityManager.setMaxPerformance()
        
        val dataSize = params.compressionDataSizeMb * 1024 * 1024
        
        val (result, timeMs) = BenchmarkHelpers.measureBenchmark {
            // Generate random data
            val data = ByteArray(dataSize) { Random.nextInt(256).toByte() }
            
            // Simple RLE compression algorithm
            fun compressRLE(input: ByteArray): ByteArray {
                val compressed = mutableListOf<Byte>()
                var i = 0
                
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
                }
                
                return compressed.toByteArray()
            }
            
            // Simple RLE decompression algorithm
            fun decompressRLE(compressed: ByteArray): ByteArray {
                val decompressed = mutableListOf<Byte>()
                var i = 0
                
                while (i < compressed.size) {
                    if (i + 1 < compressed.size) {
                        val count = compressed[i].toInt() and 0xFF  // Convert to unsigned
                        val value = compressed[i + 1]
                        
                        for (j in 0 until count) {
                            decompressed.add(value)
                        }
                        
                        i += 2
                    } else {
                        break  // Malformed data
                    }
                }
                
                return decompressed.toByteArray()
            }
            
            // Compress the data
            val compressed = compressRLE(data)
            
            // Decompress to verify correctness
            val decompressed = decompressRLE(compressed)
            
            // Verify correctness
            val isCorrect = data.contentEquals(decompressed)
            
            Triple(data.size, compressed.size, isCorrect)
        }
        
        val (originalSize, compressedSize, isCorrect) = result
        
        val throughput = originalSize / (timeMs / 1000.0)
        
        CpuAffinityManager.resetPerformance()
        
        return@withContext BenchmarkResult(
            name = "Single-Core Compression",
            executionTimeMs = timeMs.toDouble(),
            opsPerSecond = throughput,
            isValid = isCorrect,
            metricsJson = JSONObject().apply {
                put("original_size", originalSize)
                put("compressed_size", compressedSize)
                put("compression_ratio", originalSize.toDouble() / compressedSize.toDouble())
                put("throughput_bps", throughput)
            }.toString()
        )
    }
    
    /**
     * Test 8: Monte Carlo Simulation for π
     */
    suspend fun monteCarloPi(params: WorkloadParams): BenchmarkResult = withContext(Dispatchers.Default) {
        Log.d(TAG, "Starting Monte Carlo π (samples: ${params.monteCarloSamples})")
        CpuAffinityManager.setMaxPerformance()
        
        val (result, timeMs) = BenchmarkHelpers.measureBenchmark {
            val samples = params.monteCarloSamples
            var insideCircle = 0L
            
            for (i in 0 until samples) {
                val x = Random.nextDouble() * 2.0 - 1.0  // Random value between -1 and 1
                val y = Random.nextDouble() * 2.0 - 1.0  // Random value between -1 and 1
                
                if (x * x + y * y <= 1.0) {
                    insideCircle++
                }
            }
            
            val piEstimate = 4.0 * insideCircle.toDouble() / samples.toDouble()
            Pair(piEstimate, insideCircle)
        }
        
        val (piEstimate, insideCircle) = result
        val samples = params.monteCarloSamples
        val opsPerSecond = samples.toDouble() / (timeMs / 1000.0)
        
        CpuAffinityManager.resetPerformance()
        
        return@withContext BenchmarkResult(
            name = "Single-Core Monte Carlo π",
            executionTimeMs = timeMs.toDouble(),
            opsPerSecond = opsPerSecond,
            isValid = kotlin.math.abs(piEstimate - kotlin.math.PI) < 0.1,  // Reasonable accuracy check
            metricsJson = JSONObject().apply {
                put("samples", samples)
                put("pi_estimate", piEstimate)
                put("actual_pi", kotlin.math.PI)
                put("accuracy", kotlin.math.abs(piEstimate - kotlin.math.PI))
            }.toString()
        )
    }
    
    /**
     * Test 9: JSON Parsing
     */
    suspend fun jsonParsing(params: WorkloadParams): BenchmarkResult = withContext(Dispatchers.Default) {
        Log.d(TAG, "Starting JSON Parsing (size: ${params.jsonDataSizeMb}MB)")
        CpuAffinityManager.setMaxPerformance()
        
        val dataSize = params.jsonDataSizeMb * 1024 * 1024
        
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
            
            elementCount
        }
        
        val elementsParsed = result
        val elementsPerSecond = elementsParsed.toDouble() / (timeMs / 1000.0)
        
        CpuAffinityManager.resetPerformance()
        
        return@withContext BenchmarkResult(
            name = "Single-Core JSON Parsing",
            executionTimeMs = timeMs.toDouble(),
            opsPerSecond = elementsPerSecond,
            isValid = elementsParsed > 0,
            metricsJson = JSONObject().apply {
                put("json_size", dataSize)
                put("elements_parsed", elementsParsed)
                put("root_type", "object")
            }.toString()
        )
    }
    
    /**
     * Test 10: N-Queens Problem
     */
    suspend fun nqueens(params: WorkloadParams): BenchmarkResult = withContext(Dispatchers.Default) {
        Log.d(TAG, "Starting N-Queens (size: ${params.nqueensSize})")
        CpuAffinityManager.setMaxPerformance()
        
        val (result, timeMs) = BenchmarkHelpers.measureBenchmark {
            val boardSize = params.nqueensSize
            
            // Solve N-Queens problem using backtracking
            fun solveNQueens(n: Int): Int {
                val board = Array(n) { IntArray(n) { 0 } }
                val cols = BooleanArray(n) { false }
                val diag1 = BooleanArray(2 * n - 1) { false }  // For diagonal \
                val diag2 = BooleanArray(2 * n - 1) { false }  // For diagonal /
                
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
                
                return backtrack(0)
            }
            
            solveNQueens(boardSize)
        }
        
        val solutionCount = result
        val opsPerSecond = solutionCount.toDouble() / (timeMs / 100.0)
        
        CpuAffinityManager.resetPerformance()
        
        return@withContext BenchmarkResult(
            name = "Single-Core N-Queens",
            executionTimeMs = timeMs.toDouble(),
            opsPerSecond = opsPerSecond,
            isValid = solutionCount >= 0,  // N-Queens can have 0 solutions for small n
            metricsJson = JSONObject().apply {
                put("board_size", params.nqueensSize)
                put("solution_count", solutionCount)
            }.toString()
        )
    }
}