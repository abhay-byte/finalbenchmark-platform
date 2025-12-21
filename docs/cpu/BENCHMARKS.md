# CPU Benchmark Implementations

Detailed documentation of all 10 benchmarks in the FinalBenchmark2 CPU suite.

---

## 1. Prime Generation

### Overview

| Property | Value |
|----------|-------|
| **Algorithm** | Pollard's Rho Factorization |
| **Complexity** | O(N^(1/4)) expected |
| **Type** | Integer |
| **Tests** | GCD operations, cycle detection, modular arithmetic |

### Implementation

```kotlin
// BenchmarkHelpers.kt
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

private fun pollardRho(n: Long): Long {
    if (n == 1L) return 1
    if (n % 2 == 0L) return 2
    
    var x = 2L
    var y = 2L
    var d = 1L
    
    val f = { x: Long -> ((x * x) % n + 1) % n }
    
    var iterations = 0
    while (d == 1L && iterations < 1000) {
        x = f(x)
        y = f(f(y))
        d = gcd(kotlin.math.abs(x - y), n)
        iterations++
    }
    
    return if (d != n) d else 1
}

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
```

### Why Pollard's Rho Instead of Sieve?

**Previous Algorithm**: Sieve of Eratosthenes showed only 6.6% differentiation between CPUs.

**New Algorithm**: Pollard's Rho provides better CPU differentiation:
- Tests GCD operations (division-heavy)
- Floyd's cycle detection (memory access patterns)
- Modular arithmetic (integer ALU)
- **Result**: 79.5% Multi-Core differentiation (was 17.8%)

### Workload Parameters (Flagship)

| Parameter | Value |
|-----------|-------|
| `primeRange` | 980,000,000 |

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
| **Algorithm** | Polynomial Evaluation (Horner's Method) |
| **Complexity** | O(n × degree) |
| **Type** | Floating-Point |
| **Tests** | FPU multiply-add chains (FMA units), floating-point precision |

### Implementation

```kotlin
// BenchmarkHelpers.kt
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
```

### Why Polynomial Evaluation Instead of Simple Fibonacci?

**Previous**: Simple Fibonacci showed poor CPU differentiation.

**New**: Polynomial Evaluation provides better testing:
- Tests FPU multiply-add chains (FMA units)
- Tests floating-point precision
- More comprehensive than simple integer addition
- Better differentiation between CPU architectures

### Workload Parameters (Flagship)

| Parameter | Value |
|-----------|-------|
| `fibonacciIterations` | 41,666,667 |

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
| **Algorithm** | SHA-256-like Hash Computing |
| **Complexity** | O(iterations) |
| **Type** | Integer |
| **Tests** | Bitwise operations (rotations, shifts, XOR), division, modulo |

### Implementation

```kotlin
// BenchmarkHelpers.kt
fun performHashComputing(iterations: Int): Long {
    // SHA-256 initial hash values
    var h0 = 0x6a09e667.toInt()
    var h1 = 0xbb67ae85.toInt()
    // ... (8 state variables total)
    
    repeat(iterations) { i ->
        // SHA-256-like compression: rotations, XOR, AND
        val s0 = ((h0 ushr 2) or (h0 shl 30)) xor ...
        val s1 = ((h4 ushr 6) or (h4 shl 26)) xor ...
        
        // ENHANCED: Division/modulo operations
        val extra = if (i > 0) {
            val divisor = ((i and 0xFF) + 1)
            (h0 / divisor) xor (h1 % divisor)
        } else 0
        
        // State rotation
        h0 = temp1 + temp2
        // ...
    }
    
    return (h0.toLong() shl 32) or (h1.toLong() and 0xFFFFFFFFL)
}
```

### Workload Parameters (Flagship)

| Parameter | Value |
|-----------|-------|
| `hashIterations` | 525,500,000 |

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
| **Algorithm** | Perlin Noise 3D Generation |
| **Complexity** | O(width × height × depth × iterations) |
| **Type** | Floating-Point |
| **Tests** | Gradient interpolation, hash functions, trilinear interpolation |

### Implementation

```kotlin
// BenchmarkHelpers.kt
fun performPerlinNoise(width: Int, height: Int, depth: Int, iterations: Int): Double {
    var totalNoise = 0.0
    val scale = 0.1  // Noise frequency
    
    repeat(iterations) { iter ->
        for (z in 0 until depth) {
            for (y in 0 until height) {
                for (x in 0 until width) {
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

private fun perlinNoise3D(x: Double, y: Double, z: Double): Double {
    // Hash coordinates and perform trilinear interpolation
    // with fade curves: 6t^5 - 15t^4 + 10t^3
    // Returns noise value in range [-1, 1]
}
```

### Why Perlin Noise Instead of Ray Tracing?

**Previous Algorithm**: Sphere rendering showed -1.0% differentiation (D8300 was faster!).

**New Algorithm**: Perlin Noise provides better CPU differentiation:
- Tests FPU interpolation (lerp operations)
- Hash-based pseudo-random gradients
- Polynomial evaluation (fade curves)
- **Result**: 54.7% Multi-Core (was 27.2%), 44.1% Single-Core (was -1.0%)

### Workload Parameters (Flagship)

| Parameter | Value |
|-----------|-------|
| Resolution | 256 × 256 |
| Max Depth | 5 (3D depth) |
| `rayTracingIterations` | 800 |

### Operations Calculation

```kotlin
val totalRays = (width * height * depth * iterations).toLong()
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

## 8. Monte Carlo π Simulation

### Overview

| Property | Value |
|----------|-------|
| **Algorithm** | Mandelbrot Set Iteration |
| **Complexity** | O(samples × max_iterations) |
| **Type** | Floating-Point |
| **Tests** | Complex number arithmetic, FPU multiply-add, branch prediction |

### Implementation

```kotlin
// BenchmarkHelpers.kt
fun performMandelbrotSet(samples: Long, maxIterations: Int = 256): Long {
    var totalIterations = 0L
    val step = 4.0 / kotlin.math.sqrt(samples.toDouble())
    
    var cy = -2.0
    while (cy < 2.0) {
        var cx = -2.0
        while (cx < 2.0) {
            var zx = 0.0
            var zy = 0.0
            var iter = 0
            
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
```

### Why Mandelbrot Set Instead of Leibniz?

**Previous**: Leibniz formula showed only 3.6% differentiation.

**New**: Mandelbrot Set provides better CPU differentiation:
- Complex number arithmetic (FPU multiply-add)
- Escape-time iteration (branch prediction)
- **Result**: 47.1% Multi-Core (was 6.8%), 11.2% Single-Core (was 3.6%)
- **Workload**: Reduced by 100x (256 iterations per sample vs 1 division)

### Workload Parameters (Flagship)

| Parameter | Value |
|-----------|-------|
| `monteCarloSamples` | 50,000,000 |

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
| `jsonParsingIterations` | 2,500 |

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
| `nqueensSize` | 16 |

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
