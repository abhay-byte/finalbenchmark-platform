# CPU Benchmark Implementations

Detailed documentation of all 10 benchmarks in the FinalBenchmark2 CPU suite.

---

## 1. Prime Generation

### Overview

| Property | Value |
|----------|-------|
| **Algorithm** | Sieve of Eratosthenes |
| **Complexity** | O(N log log N) |
| **Type** | Integer |
| **Tests** | Memory access patterns, cache efficiency, integer arithmetic |

### Implementation

```kotlin
// BenchmarkHelpers.kt
fun sieveOfEratosthenes(limit: Int): Long {
    if (limit < 2) return 0L
    
    val sieve = BooleanArray(limit + 1) { true }
    sieve[0] = false
    sieve[1] = false
    
    var i = 2
    while (i * i <= limit) {
        if (sieve[i]) {
            var j = i * i
            while (j <= limit) {
                sieve[j] = false
                j += i
            }
        }
        i++
    }
    
    return sieve.count { it }.toLong()
}
```

### Workload Parameters (Flagship)

| Parameter | Value |
|-----------|-------|
| `primeRange` | 900,000,000 |

### Operations Calculation

```kotlin
// Single-core
val ops = params.primeRange.toDouble()

// Multi-core (Fixed Work Per Core)
val totalRange = rangePerThread.toLong() * numThreads
```

---

## 2. Fibonacci Iterative

### Overview

| Property | Value |
|----------|-------|
| **Algorithm** | Iterative Fibonacci |
| **Complexity** | O(n) per iteration |
| **Type** | Integer |
| **Tests** | Pure ALU arithmetic, register usage |

### Implementation

```kotlin
// SingleCoreBenchmarks.kt - Inlined for JIT stability
repeat(iterations) {
    var prev = 0L
    var curr = 1L
    for (i in 2..targetN) {
        val next = prev + curr
        prev = curr
        curr = next
    }
    totalResult += curr
}
```

### Workload Parameters (Flagship)

| Parameter | Value |
|-----------|-------|
| `targetN` | 35 |
| `fibonacciIterations` | 120,000,000 |

### Operations Calculation

```kotlin
val actualOps = params.fibonacciIterations.toDouble()
```

---

## 3. Matrix Multiplication

### Overview

| Property | Value |
|----------|-------|
| **Algorithm** | Cache-optimized i-k-j matrix multiplication |
| **Complexity** | O(n³ × iterations) |
| **Type** | Floating-Point |
| **Tests** | FMA operations, cache efficiency |

### Implementation

```kotlin
// BenchmarkHelpers.kt
fun performMatrixMultiplication(size: Int, repetitions: Int): Long {
    val a = Array(size) { DoubleArray(size) }
    val b = Array(size) { DoubleArray(size) }
    val c = Array(size) { DoubleArray(size) }
    
    repeat(repetitions) { rep ->
        // DETERMINISTIC INITIALIZATION: Eliminates RNG overhead
        // Makes benchmark purely test FPU/matrix computation
        val offset = rep.toDouble()
        
        for (i in 0 until size) {
            for (j in 0 until size) {
                // Deterministic pattern: varies with position and iteration
                a[i][j] = ((i * size + j + offset) % 1000) / 1000.0
                b[i][j] = ((j * size + i + offset) % 1000) / 1000.0
                c[i][j] = 0.0
            }
        }
        
        // Cache-optimized i-k-j order multiplication
        for (i in 0 until size) {
            for (k in 0 until size) {
                val aik = a[i][k]
                for (j in 0 until size) {
                    c[i][j] += aik * b[k][j]
                }
            }
        }
    }
    
    return checksum
}
```

### Workload Parameters (Flagship)

| Parameter | Value |
|-----------|-------|
| `matrixSize` | 128 |
| `matrixIterations` | 3,000 |

### Operations Calculation

```kotlin
// 2 operations per element (multiply + add)
val totalOps = size.toLong() * size * size * 2 * iterations
```

### Cache Strategy

Uses 128×128 matrices (128 KB) that fit in L2/L3 cache to prevent memory bottlenecks.

---

## 4. Hash Computing

### Overview

| Property | Value |
|----------|-------|
| **Algorithm** | FNV-1a Hash |
| **Complexity** | O(bufferSize × iterations) |
| **Type** | Integer |
| **Tests** | Byte manipulation, XOR, multiplication |

### Implementation

```kotlin
// BenchmarkHelpers.kt
fun performHashComputing(bufferSize: Int, iterations: Int): Long {
    val data = ByteArray(bufferSize) { (it % 255).toByte() }
    var currentState = 0x811C9DC5.toInt()  // FNV offset basis
    
    repeat(iterations) {
        for (i in 0 until bufferSize step 4) {
            currentState = (currentState xor data[i].toInt()) * 16777619
        }
    }
    
    return bufferSize.toLong() * iterations
}
```

### Workload Parameters (Flagship)

| Parameter | Value |
|-----------|-------|
| Buffer Size | 4 KB |
| `hashIterations` | 10,000 |

### Operations Calculation

```kotlin
val opsPerSecond = iterations.toDouble() / (timeMs / 1000.0)
```

---

## 5. String Sorting

### Overview

| Property | Value |
|----------|-------|
| **Algorithm** | Collections.sort() (TimSort) |
| **Complexity** | O(n log n) per iteration |
| **Type** | Integer |
| **Tests** | String comparison, memory allocation, sorting |

### Implementation

```kotlin
// BenchmarkHelpers.kt
fun runStringSortWorkload(sourceList: List<String>, iterations: Int): Int {
    var checkSum = 0
    repeat(iterations) {
        val workingList = ArrayList(sourceList)
        workingList.sort()
        if (workingList.isNotEmpty()) {
            checkSum += workingList.last().hashCode()
        }
    }
    return checkSum
}
```

### Workload Parameters (Flagship)

| Parameter | Value |
|-----------|-------|
| List Size | 4,096 strings |
| String Length | 16 characters |
| `stringSortIterations` | 5,000 |

### Operations Calculation

```kotlin
val comparisonsPerSort = cacheResidentSize * log(cacheResidentSize.toDouble(), 2.0)
val totalComparisons = iterations * comparisonsPerSort
val opsPerSecond = totalComparisons / (timeMs / 1000.0)
```

### Cache Strategy

Uses small list (4,096 strings) that fits in CPU cache for consistent performance.

---

## 6. Ray Tracing

### Overview

| Property | Value |
|----------|-------|
| **Algorithm** | Simple ray-sphere intersection |
| **Complexity** | O(width × height × depth × iterations) |
| **Type** | Floating-Point |
| **Tests** | Vector math, square root, 3D graphics operations |

### Implementation

```kotlin
// BenchmarkHelpers.kt
fun performRayTracing(width: Int, height: Int, maxDepth: Int, iterations: Int): Double {
    var totalEnergy = 0.0
    
    repeat(iterations) {
        for (y in 0 until height) {
            for (x in 0 until width) {
                // Generate ray
                val rayDirX = (x - width / 2.0) / width
                val rayDirY = (y - height / 2.0) / height
                val rayDirZ = 1.0
                
                // Normalize direction
                val len = sqrt(rayDirX * rayDirX + rayDirY * rayDirY + rayDirZ * rayDirZ)
                
                // Check sphere intersections
                // Accumulate lighting
                totalEnergy += computePixelColor(...)
            }
        }
    }
    
    return totalEnergy
}
```

### Workload Parameters (Flagship)

| Parameter | Value |
|-----------|-------|
| Resolution | 256 × 256 |
| Max Depth | 3 |
| `rayTracingIterations` | 800 |

### Operations Calculation

```kotlin
val totalRays = (width * height * iterations).toLong()
val raysPerSecond = totalRays / (timeMs / 1000.0)
```

---

## 7. Compression

### Overview

| Property | Value |
|----------|-------|
| **Algorithm** | Run-Length Encoding (RLE) |
| **Complexity** | O(bufferSize × iterations) |
| **Type** | Integer |
| **Tests** | Sequential byte processing, comparison, counting |

### Implementation

```kotlin
// BenchmarkHelpers.kt
fun performCompression(bufferSize: Int, iterations: Int): Long {
    val data = ByteArray(bufferSize) { (it % 256).toByte() }
    val outputBuffer = ByteArray(bufferSize * 2)
    var totalCompressedSize = 0L
    
    repeat(iterations) {
        val compressedSize = compressRLE(data, outputBuffer)
        totalCompressedSize += compressedSize
    }
    
    return bufferSize.toLong() * iterations
}

private fun compressRLE(input: ByteArray, output: ByteArray): Int {
    var i = 0
    var outputIndex = 0
    
    while (i < input.size) {
        val currentByte = input[i]
        var count = 1
        
        while (i + count < input.size && 
               input[i + count] == currentByte && 
               count < 255) {
            count++
        }
        
        output[outputIndex++] = count.toByte()
        output[outputIndex++] = currentByte
        i += count
    }
    
    return outputIndex
}
```

### Workload Parameters (Flagship)

| Parameter | Value |
|-----------|-------|
| `compressionDataSizeMb` | 2 MB |
| `compressionIterations` | 2,000 |

### Operations Calculation

```kotlin
val throughput = totalBytes.toDouble() / (timeMs / 1000.0)
```

---

## 8. Monte Carlo π (Leibniz)

### Overview

| Property | Value |
|----------|-------|
| **Algorithm** | Leibniz formula: π/4 = 1 - 1/3 + 1/5 - 1/7 + ... |
| **Complexity** | O(iterations) |
| **Type** | Floating-Point |
| **Tests** | Division, alternating series, accumulation |

### Implementation

```kotlin
// SingleCoreBenchmarks.kt
val (partialSum, timeMs) = BenchmarkHelpers.measureBenchmark {
    var sum = 0.0
    var term = 0L
    
    while (term < iterations) {
        // Leibniz: (-1)^n / (2n + 1)
        val denominator = 2.0 * term + 1.0
        val sign = if (term % 2 == 0L) 1.0 else -1.0
        sum += sign / denominator
        term++
    }
    
    sum
}

val piEstimate = partialSum * 4.0
```

### Why Leibniz Instead of Monte Carlo?

1. **Deterministic**: No random numbers, no caching variance
2. **Predictable**: Same iterations = same result
3. **Pure arithmetic**: Tests raw CPU throughput
4. **Fair comparison**: Same algorithm for single/multi-core

### Workload Parameters (Flagship)

| Parameter | Value |
|-----------|-------|
| `monteCarloSamples` | 5,000,000,000 |

### Validation

```kotlin
val accuracy = abs(piEstimate - PI)
val accuracyThreshold = when {
    iterations >= 100_000_000 -> 0.00001
    iterations >= 10_000_000 -> 0.0001
    else -> 0.01
}
isValid = accuracy < accuracyThreshold
```

---

## 9. JSON Parsing

### Overview

| Property | Value |
|----------|-------|
| **Algorithm** | org.json.JSONObject parsing |
| **Complexity** | O(jsonSize × iterations) |
| **Type** | Integer |
| **Tests** | String parsing, object creation, memory allocation |

### Implementation

```kotlin
// BenchmarkHelpers.kt
fun performJsonParsingWorkload(jsonData: String, iterations: Int): Long {
    var totalElementCount = 0L
    
    repeat(iterations) {
        val jsonObject = JSONObject(jsonData)
        totalElementCount += countJsonElements(jsonObject)
    }
    
    return totalElementCount
}
```

### Workload Parameters (Flagship)

| Parameter | Value |
|-----------|-------|
| `jsonDataSizeMb` | 1 MB |
| `jsonParsingIterations` | 10,000 |

### Cache Strategy

JSON data is generated **once** outside the timing block, then parsed multiple times from cache.

---

## 10. N-Queens

### Overview

| Property | Value |
|----------|-------|
| **Algorithm** | Backtracking with bitwise optimization |
| **Complexity** | O(N!) theoretical, optimized in practice |
| **Type** | Integer |
| **Tests** | Recursion, backtracking, branch prediction |

### Implementation

```kotlin
// BenchmarkHelpers.kt
fun solveNQueens(n: Int): NQueensResult {
    var solutions = 0L
    var iterations = 0L
    
    fun solve(row: Int, cols: Int, diag1: Int, diag2: Int) {
        iterations++
        if (row == n) {
            solutions++
            return
        }
        
        var availablePositions = ((1 shl n) - 1) and (cols or diag1 or diag2).inv()
        while (availablePositions != 0) {
            val position = availablePositions and -availablePositions
            availablePositions -= position
            solve(row + 1, 
                  cols or position, 
                  (diag1 or position) shl 1, 
                  (diag2 or position) shr 1)
        }
    }
    
    solve(0, 0, 0, 0)
    return NQueensResult(solutions, iterations)
}
```

### Workload Parameters (Flagship)

| Parameter | Value |
|-----------|-------|
| `nqueensSize` | 15 |

### Known Solutions

| Board Size | Solutions |
|------------|-----------|
| N = 10 | 724 |
| N = 12 | 14,200 |
| N = 15 | 2,279,184 |

### Operations Calculation

```kotlin
val opsPerSecond = iterations.toDouble() / (timeMs / 1000.0)
```
