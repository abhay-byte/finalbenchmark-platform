# CPU Benchmark Algorithms & Methodology

## Summary Table

| Test Category | Test Name | Algorithm | Complexity | Primary Focus |
|---------------|-----------|-----------|------------|---------------|
| Single-Core | Prime Number Generation | Sieve of Eratosthenes | O(n log log n) | Integer arithmetic, memory access |
| Single-Core | Fibonacci Sequence (Recursive) | Recursive Fibonacci | O(2^n) | Function calls, stack performance |
| Single-Core | Matrix Multiplication | Triple-nested loops | O(n³) | Floating-point operations, cache efficiency |
| Single-Core | Hash Computing | SHA-256, MD5 | O(n) | Cryptographic operations, memory bandwidth |
| Single-Core | String Sorting | Introsort (Hybrid) | O(n log n) | String comparisons, memory management |
| Single-Core | Ray Tracing | Recursive ray tracing | O(w×h×r) | Floating-point math, recursion |
| Single-Core | Compression/Decompression | LZ77 with sliding window | O(n×m) | Data processing, memory bandwidth |
| Single-Core | Monte Carlo Simulation | π calculation | O(n) | Random number generation, floating-point |
| Single-Core | JSON Parsing | Recursive descent | O(n) | String processing, data structures |
| Single-Core | N-Queens Problem | Backtracking | O(N!) | Branch prediction, algorithmic efficiency |
| Multi-Core | Parallel Prime Generation | Parallel Sieve | O(n log log n) | Multi-core scaling, cache coherency |
| Multi-Core | Parallel Fibonacci | Recursive with memoization | O(n) | Thread synchronization, shared cache |
| Multi-Core | Parallel Matrix Multiply | Parallel block decomposition | O(n³) | Multi-core FPU, memory bandwidth |
| Multi-Core | Parallel Hash Computing | Multiple block processing | O(n) | Multi-core crypto instructions |
| Multi-Core | Parallel String Sorting | Parallel Mergesort | O(n log n) | Multi-core string ops, memory alloc |
| Multi-Core | Parallel Ray Tracing | Tile-based distribution | O(w×h×r) | Multi-core FP, load balancing |
| Multi-Core | Parallel Compression | Block-parallel processing | O(n×m) | Multi-core memory, dictionary sharing |
| Multi-Core | Parallel Monte Carlo | Independent sample sets | O(n) | Multi-core FP, result aggregation |
| Multi-Core | Parallel JSON Parsing | Chunk-based processing | O(n) | Multi-core string ops, validation |
| Multi-Core | Parallel N-Queens | Search space division | O(N!) | Multi-core search, work-stealing |

All CPU tests in this benchmark suite are implemented in Rust for optimal performance and memory safety. Rust provides the low-level control needed for accurate performance measurements while preventing common memory-related errors that could affect benchmark results.

## 1. Introduction

### Purpose of CPU Benchmarking

CPU benchmarking is a critical component of comprehensive system performance evaluation that measures the computational capabilities of a device's processor. Within the overall system benchmarking framework, CPU tests provide insights into the core processing power that affects virtually every aspect of device performance, from application responsiveness to gaming performance and multimedia processing.

The CPU is the primary computational engine of any computing device, responsible for executing instructions, performing calculations, and managing data flow. Understanding its performance characteristics is essential for:
- Assessing device capabilities for demanding applications
- Comparing performance across different hardware configurations
- Identifying bottlenecks in system performance
- Validating the effectiveness of CPU optimizations and features

### Performance Aspects Evaluated

CPU benchmark tests evaluate multiple dimensions of processor performance:

- **Integer Arithmetic Performance**: Measures the CPU's ability to perform basic mathematical operations on whole numbers, which is fundamental to most computing tasks
- **Floating Point Operations**: Tests precision mathematical calculations essential for graphics rendering, scientific computing, and multimedia applications
- **Compute Throughput**: Evaluates the rate at which the CPU can execute instructions and process data
- **Multi-threading Efficiency**: Assesses how effectively the CPU can utilize multiple cores and threads for parallel processing
- **Memory Access Patterns**: Tests the interaction between CPU and memory subsystems, including cache efficiency and memory bandwidth utilization
- **Branch Prediction**: Evaluates how well the CPU predicts conditional branches to maintain execution pipeline efficiency
- **Instruction Pipeline Utilization**: Measures how effectively the CPU utilizes its internal execution units

### Test Selection Rationale

The selected CPU tasks were chosen based on their real-world relevance and computational complexity:

- **Real-world Relevance**: Each test represents common computational patterns found in actual applications, from cryptographic operations in security software to matrix calculations in graphics applications
- **Computational Complexity**: Tests cover a range of algorithmic complexities to stress different aspects of CPU architecture
- **Diverse Workload Characteristics**: Tests vary in their memory access patterns, computational intensity, and parallelization potential to provide comprehensive coverage
- **Measurable Performance Differences**: Tests are designed to show meaningful performance differences across different CPU architectures and configurations

## 2. Test List

The CPU tests are divided into two main categories: Single-Core Performance Tests and Multi-Core Performance Tests. This division allows for comprehensive evaluation of both per-core performance and multi-threading efficiency.

### Single-Core Performance Tests

These tests are designed to measure the performance of individual CPU cores, focusing on per-core capabilities without the influence of multi-threading:

### Single-Core Test 1: Prime Number Generation

**Algorithm Used**: Sieve of Eratosthenes
The Sieve of Eratosthenes is an ancient algorithm for finding all prime numbers up to a specified integer. The algorithm works by iteratively marking the multiples of each prime number as composite, starting from 2. It begins by marking all multiples of 2 (except 2 itself), then moves to the next unmarked number (3) and marks its multiples, and continues this process until all numbers up to the limit have been processed.

**Complexity Analysis**: O(n log log n)
The algorithm has a time complexity of O(n log log n) where n is the upper limit of the range. This is because for each prime number p, the algorithm marks approximately n/p multiples. The sum of reciprocals of primes up to n is approximately log log n, leading to the overall complexity. The space complexity is O(n) for storing the boolean array that tracks which numbers are prime.

**Dataset/Workload Details**: 
- Range: Calculate all primes up to 10 million (10,000,000)
- Memory allocation: Boolean array of size 10,000,000
- Iterations: Up to √n iterations where n = 10,000,000
- Threading model: Can be parallelized using segmented sieve approach for multi-core systems

**Measurements Captured**:
- Total execution time from start to completion
- Operations per second (sieve operations)
- Memory access patterns and cache efficiency
- Correctness validation through prime count verification
- Memory bandwidth utilization during array operations

**Hardware Behavior Targeted**:
- Memory subsystem performance (cache hits/misses, memory bandwidth)
- Integer arithmetic units and ALU utilization
- Memory access pattern efficiency
- Branch prediction for conditional marking operations

**Notes on Deterministic Behavior**: The algorithm is completely deterministic and produces the same results regardless of execution environment. The performance differences arise from hardware capabilities rather than algorithmic variations.

### Single-Core Test 2: Fibonacci Sequence (Recursive)

**Algorithm Used**: Recursive Fibonacci Implementation
The recursive Fibonacci algorithm calculates Fibonacci numbers using the classic recursive definition: F(n) = F(n-1) + F(n-2), with base cases F(0) = 0 and F(1) = 1. This implementation has exponential time complexity due to redundant calculations of the same Fibonacci numbers multiple times. Each call to F(n) results in two additional calls, creating a binary tree of function calls.

**Complexity Analysis**: O(2^n)
The time complexity is exponential, specifically O(2^n), because each call to fibonacci(n) results in two more calls: fibonacci(n-1) and fibonacci(n-2). This creates a binary tree of function calls with approximately 2^n nodes. The space complexity is O(n) due to the maximum recursion depth of the call stack.

**Dataset/Workload Details**:
- Input range: Calculate fibonacci(n) for n = 35 to 45
- Recursive depth: Up to n levels of function calls
- Function call overhead: Each recursive call adds overhead to the stack
- Threading model: Single-threaded execution to measure pure function call performance

**Measurements Captured**:
- Total execution time for each Fibonacci calculation
- Function call overhead and stack management performance
- Recursion depth handling capability
- Correctness validation through result verification
- Call stack memory usage patterns

**Hardware Behavior Targeted**:
- Function call overhead and stack performance
- CPU instruction pipeline efficiency
- Memory subsystem performance for stack operations
- Branch prediction for recursive function calls

**Notes on Deterministic Behavior**: The algorithm produces identical results but with exponential performance degradation as input size increases, making it ideal for measuring CPU performance differences.

### Single-Core Test 3: Matrix Multiplication

**Algorithm Used**: Standard Triple-Nested Loop Matrix Multiplication
The algorithm implements the standard matrix multiplication formula C = A × B where each element c[i][j] is calculated as the dot product of row i from matrix A and column j from matrix B. The implementation uses three nested loops to iterate through the rows of A, columns of B, and the inner dimension to compute the dot products.

**Complexity Analysis**: O(n³) for n×n matrices
For square matrices of size n×n, the algorithm performs n³ multiplications and additions. With three nested loops each running n times, the time complexity is O(n³). The space complexity is O(n²) for storing the three matrices (input A, input B, and result C).

**Dataset/Workload Details**:
- Matrix size: 100×1000 floating-point matrices
- Total operations: ~2 billion floating-point operations
- Memory usage: ~24 MB for three matrices (assuming 8 bytes per double)
- Threading model: Can be parallelized using OpenMP or similar threading models

**Measurements Captured**:
- Total execution time for matrix multiplication
- Floating-point operations per second (FLOPS)
- Memory bandwidth utilization during matrix access
- Cache efficiency and hit/miss ratios
- Correctness validation through partial result verification

**Hardware Behavior Targeted**:
- Floating-point unit performance (FPU/SIMD)
- Memory bandwidth and cache hierarchy efficiency
- CPU cache utilization and memory access patterns
- SIMD instruction utilization for vectorized operations

**Notes on Deterministic Behavior**: Results are mathematically deterministic, though floating-point precision may vary slightly across different architectures due to rounding differences.

### Single-Core Test 4: Hash Computing (SHA-256, MD5)

**Algorithm Used**: Cryptographic Hash Function Implementation
The test implements cryptographic hash functions SHA-256 and MD5 to process large data blocks. SHA-256 processes data in 512-bit chunks using a series of logical functions and modular arithmetic operations. MD5 processes data in 512-bit blocks through four rounds of operations involving bitwise functions, modular addition, and left rotations.

**Complexity Analysis**: O(n) where n is the input data size
Both SHA-256 and MD5 have linear time complexity relative to the input size. Each block of data requires a fixed number of operations regardless of content. The space complexity is O(1) as only a fixed amount of state data is maintained throughout the process.

**Dataset/Workload Details**:
- Data size: 100 MB of random data for hashing
- Block processing: 512-bit blocks for both algorithms
- Iterations: Process entire data set in sequence
- Threading model: Single-threaded for consistent cryptographic performance measurement

**Measurements Captured**:
- Total execution time for hash computation
- Data processing throughput (MB/s)
- Operations per second for cryptographic functions
- Correctness validation through known hash value verification
- Cryptographic instruction utilization (if available)

**Hardware Behavior Targeted**:
- Cryptographic instruction performance (AES-NI, SHA extensions)
- Integer arithmetic and bitwise operation units
- Memory bandwidth for data input/output
- CPU instruction pipeline efficiency for cryptographic operations

**Notes on Deterministic Behavior**: Hash functions are deterministic by design, producing identical outputs for identical inputs across all platforms.

### Single-Core Test 5: String Sorting

**Algorithm Used**: Hybrid Sorting Algorithm (Introsort - Introspective Sort)
The implementation uses Introsort, which begins with quicksort and switches to heapsort when the recursion depth exceeds a certain threshold to guarantee O(n log n) worst-case performance. For small arrays, it may use insertion sort for optimization. The algorithm partitions the array around a pivot element and recursively sorts the partitions.

**Complexity Analysis**: O(n log n) average and O(n log n) worst case
Introsort maintains O(n log n) average performance like quicksort but guarantees O(n log n) worst-case performance by switching to heapsort when necessary. The space complexity is O(log n) for the recursion stack in average cases.

**Dataset/Workload Details**:
- Data size: 1 million random strings of varying lengths (10-1000 characters)
- String comparison operations: Leverages string comparison algorithms
- Memory usage: String objects and sorting overhead
- Threading model: Single-threaded to measure pure sorting performance

**Measurements Captured**:
- Total execution time for sorting operation
- String comparison operations per second
- Memory allocation and management performance
- Correctness validation through sorted order verification
- Comparison function efficiency

**Hardware Behavior Targeted**:
- String comparison and memory access patterns
- Branch prediction for comparison operations
- Memory subsystem performance for object allocation
- CPU cache efficiency for pointer-based operations

**Notes on Deterministic Behavior**: Sorting algorithms are deterministic in terms of final output, though the specific sequence of operations may vary slightly.

### Single-Core Test 6: Ray Tracing

**Algorithm Used**: Basic Ray Tracing with Recursive Reflection
The algorithm implements a simplified ray tracer that calculates the color of each pixel by casting rays from a virtual camera through the image plane into a 3D scene. For each ray, it determines which object it intersects first, calculates lighting using Phong reflection model, and optionally traces reflected rays for mirror-like surfaces. The implementation includes sphere objects and basic lighting calculations.

**Complexity Analysis**: O(w×h×r) where w=width, h=height, r=recursion depth
For an image of width w and height h with maximum recursion depth r, the complexity depends on the number of rays cast (w×h) and the average number of objects tested per ray. The space complexity is O(r) for the recursion stack depth.

**Dataset/Workload Details**:
- Scene complexity: 10-20 spheres with different materials
- Image resolution: 512×512 pixels
- Ray depth: 3-5 levels of recursion for reflections
- Threading model: Can be parallelized per scanline or tile-based

**Measurements Captured**:
- Total rendering time for the scene
- Rays processed per second
- Floating-point operations for intersection calculations
- Correctness validation through visual consistency
- Memory bandwidth for scene data access

**Hardware Behavior Targeted**:
- Floating-point performance for geometric calculations
- Branch prediction for intersection tests
- Memory subsystem for scene data access
- CPU cache efficiency for recursive function calls

**Notes on Deterministic Behavior**: The algorithm produces deterministic results for the same scene configuration, though floating-point precision may cause minor variations across platforms.

### Single-Core Test 7: Compression/Decompression

**Algorithm Used**: LZ77-based Compression with Sliding Window
The implementation uses an LZ77-style compression algorithm that maintains a sliding window of previously seen data and encodes new data as references to previous occurrences. The algorithm searches for the longest match between the current input and the sliding window, encoding matches as (offset, length) pairs and literal characters when no matches are found.

**Complexity Analysis**: O(n×m) average where n is input size and m is the average search window size
In the worst case, when searching the entire window for each input character, the complexity becomes O(n²). The space complexity is O(w) where w is the window size, typically a few hundred KB.

**Dataset/Workload Details**:
- Input size: 50 MB of mixed content (text, binary data)
- Window size: 64 KB sliding window
- Compression ratio: Expected 2:1 to 5:1 depending on input
- Threading model: Single-threaded for consistent compression performance

**Measurements Captured**:
- Total compression time and throughput (MB/s)
- Total decompression time and throughput (MB/s)
- Compression ratio achieved
- Memory bandwidth utilization during processing
- Correctness validation through round-trip verification

**Hardware Behavior Targeted**:
- Memory bandwidth for sliding window operations
- Branch prediction for match finding algorithms
- CPU cache efficiency for data access patterns
- Integer arithmetic for offset calculations

**Notes on Deterministic Behavior**: Compression algorithms are deterministic for the same input and algorithm parameters, producing identical compressed output.

### Single-Core Test 8: Monte Carlo Simulation

**Algorithm Used**: Monte Carlo Method for π Calculation
The algorithm implements a classic Monte Carlo simulation to estimate π by randomly sampling points in a unit square and determining the ratio of points that fall inside a unit circle. For each random point (x,y), it calculates x² + y² and compares to 1 to determine if the point is inside the circle. The ratio of points inside the circle to total points approximates π/4.

**Complexity Analysis**: O(n) where n is the number of random samples
The algorithm performs a constant amount of work for each sample: generating random coordinates, performing arithmetic operations, and comparisons. The space complexity is O(1) as only counters are maintained.

**Dataset/Workload Details**:
- Sample count: 10 million random points
- Random number generation: High-quality PRNG for statistical validity
- Arithmetic operations: Squaring, addition, and comparison per sample
- Threading model: Can be parallelized with independent sample sets

**Measurements Captured**:
- Total execution time for simulation
- Random number generation throughput
- Floating-point arithmetic operations per second
- Statistical accuracy of π estimation
- Correctness validation through convergence to known value

**Hardware Behavior Targeted**:
- Floating-point unit performance for arithmetic operations
- Random number generator performance
- Branch prediction for comparison operations
- CPU pipeline efficiency for arithmetic-intensive workloads

**Notes on Deterministic Behavior**: With fixed random seeds, the algorithm produces reproducible results, though statistical convergence improves with more samples.

### Single-Core Test 9: JSON Parsing

**Algorithm Used**: Recursive Descent JSON Parser
The parser implements a recursive descent algorithm that processes JSON tokens according to the JSON grammar. It recognizes JSON values (objects, arrays, strings, numbers, booleans, null) and recursively parses nested structures. The parser maintains state for current position, handles string escaping, number formatting, and validates JSON syntax throughout the parsing process.

**Complexity Analysis**: O(n) where n is the input JSON size
JSON parsing requires examining each character in the input once, making it linear in the input size. The space complexity is O(d) where d is the maximum nesting depth of the JSON structure.

**Dataset/Workload Details**:
- Input size: 10 MB of complex, nested JSON data
- Structure depth: 5-10 levels of nesting
- Data types: Mix of objects, arrays, strings, numbers, booleans
- Threading model: Single-threaded for consistent parsing performance

**Measurements Captured**:
- Total parsing time and throughput (MB/s)
- JSON elements parsed per second
- Memory allocation for parsed data structures
- Correctness validation through round-trip serialization
- Error detection and handling performance

**Hardware Behavior Targeted**:
- String processing and character comparison performance
- Branch prediction for state machine transitions
- Memory subsystem for dynamic data structure allocation
- CPU cache efficiency for string operations

**Notes on Deterministic Behavior**: JSON parsing is deterministic for valid input, producing identical data structures across platforms.

### Single-Core Test 10: N-Queens Problem

**Algorithm Used**: Backtracking Algorithm with Constraint Propagation
The algorithm implements a backtracking solution to the N-Queens problem, which places N queens on an N×N chessboard so that no two queens threaten each other. The algorithm places queens row by row, checking for conflicts with previously placed queens in columns and diagonals. When a conflict is detected, it backtracks to try alternative positions.

**Complexity Analysis**: O(N!) worst case, but typically much less due to pruning
In the worst case, the algorithm might explore all possible arrangements, but constraint checking significantly prunes the search space. The space complexity is O(N) for storing the board state and recursion stack.

**Dataset/Workload Details**:
- Board size: 15×15 (N=15) for substantial computational challenge
- Solution search: Find first valid solution or count all solutions
- Constraint checking: Column, row, and diagonal conflict detection
- Threading model: Single-threaded to measure pure algorithmic performance

**Measurements Captured**:
- Total execution time to find solution(s)
- Number of board positions evaluated
- Backtracking operations performed
- Correctness validation through solution verification
- Search space pruning efficiency

**Hardware Behavior Targeted**:
- Branch prediction for conditional logic
- Integer arithmetic for position calculations
- Memory subsystem for board state management
- CPU cache efficiency for constraint checking

**Notes on Deterministic Behavior**: The algorithm finds the same solutions in the same order given the same search strategy, though total execution time varies based on hardware performance.

### Multi-Core Performance Tests

These tests are designed to measure the performance of multiple CPU cores working together, focusing on multi-threading efficiency and scalability:

### Multi-Core Test 1: Parallel Prime Number Generation

**Algorithm Used**: Parallel Sieve of Eratosthenes
The parallel implementation divides the range of numbers into segments that can be processed concurrently by different threads. Each thread handles a segment of the range, and synchronization mechanisms ensure that results are properly combined. The algorithm uses shared memory for the boolean array and employs atomic operations or locks for thread-safe marking of composite numbers.

**Complexity Analysis**: O(n log log n) with potential for parallelization speedup
The algorithm maintains the same time complexity as the sequential version but can achieve significant speedup through parallel execution. The theoretical speedup is limited by Amdahl's law and the synchronization overhead required to ensure thread safety. The space complexity remains O(n) but may require additional memory for thread coordination.

**Dataset/Workload Details**: 
- Range: Calculate all primes up to 20 million (20,000) for multi-core systems
- Memory allocation: Boolean array of size 20,000,000 shared across threads
- Threading model: Uses thread pool with number of threads equal to physical cores
- Work distribution: Segmented range processing with load balancing

**Measurements Captured**:
- Total execution time from start to completion
- Parallel speedup compared to single-core implementation
- Thread utilization and synchronization overhead
- Memory access patterns and cache coherency
- Scaling efficiency across different thread counts

**Hardware Behavior Targeted**:
- Multi-core utilization and thread scheduling efficiency
- Cache coherency protocols and inter-core communication
- Memory subsystem performance under concurrent access
- Branch prediction with multi-threaded execution patterns

**Notes on Deterministic Behavior**: The algorithm produces identical results to the single-core version but with improved performance through parallel execution.

### Multi-Core Test 2: Parallel Fibonacci Sequence (Recursive with Memoization)

**Algorithm Used**: Parallel Recursive Fibonacci with Memoization
The algorithm uses a concurrent memoization table to cache computed Fibonacci values, allowing multiple threads to share results and avoid redundant calculations. The implementation may use techniques like work-stealing queues to distribute recursive calculations across threads, with synchronization mechanisms to protect the shared memoization cache.

**Complexity Analysis**: O(n) with shared memoization, but with multi-threading overhead
While the theoretical complexity is reduced from O(2^n) to O(n) through memoization, the parallel implementation adds synchronization overhead. The space complexity increases to O(n) for the memoization table, which must be thread-safe.

**Dataset/Workload Details**:
- Input range: Calculate fibonacci(n) for n = 40 to 50 with parallel computation
- Thread coordination: Shared memoization table with concurrent access
- Work distribution: Dynamic task distribution based on recursive branches
- Threading model: Work-stealing or thread pool approach

**Measurements Captured**:
- Total execution time compared to single-core implementation
- Cache hit/miss rates for memoized values
- Thread synchronization overhead
- Speedup achieved through parallelization
- Memory bandwidth utilization during concurrent access

**Hardware Behavior Targeted**:
- Thread synchronization and lock contention performance
- Cache performance with concurrent access patterns
- Memory subsystem behavior under multi-threaded loads
- CPU instruction pipeline efficiency with parallel execution

**Notes on Deterministic Behavior**: Produces identical results to single-core implementation with improved performance through parallel execution and memoization.

### Multi-Core Test 3: Parallel Matrix Multiplication

**Algorithm Used**: Parallel Matrix Multiplication with Thread Pool
The algorithm partitions the computation of the result matrix across multiple threads using techniques like block decomposition or row/column parallelization. Each thread computes a portion of the result matrix, with synchronization to ensure proper memory access. Advanced implementations may use cache-aware algorithms to optimize memory access patterns across threads.

**Complexity Analysis**: O(n³) with potential for near-linear speedup with multiple cores
The time complexity remains O(n³) but can achieve significant speedup proportional to the number of available cores. The space complexity is O(n²) for the matrices, with potential additional memory for thread coordination and cache optimization.

**Dataset/Workload Details**:
- Matrix size: 1500×1500 floating-point matrices for multi-core systems
- Total operations: ~6.75 billion floating-point operations
- Memory usage: ~54 MB for three matrices (assuming 8 bytes per double)
- Threading model: Thread pool with work-stealing or block distribution

**Measurements Captured**:
- Total execution time and parallel speedup
- Floating-point operations per second (FLOPS) across all cores
- Memory bandwidth utilization during parallel access
- Cache efficiency and inter-core data sharing
- Scaling efficiency across different thread counts

**Hardware Behavior Targeted**:
- Multi-core floating-point unit utilization
- Memory bandwidth and cache hierarchy under parallel loads
- CPU cache coherency and inter-core communication
- SIMD instruction utilization across multiple cores

**Notes on Deterministic Behavior**: Results are mathematically identical to single-core implementation with improved performance through parallel execution.

### Multi-Core Test 4: Parallel Hash Computing (SHA-256, MD5)

**Algorithm Used**: Parallel Processing of Multiple Data Blocks
The implementation processes multiple independent data blocks concurrently, with each thread handling a separate block of data. Alternatively, the algorithm may use parallel processing techniques for very large data sets, dividing the input into chunks that can be processed in parallel before being combined according to the hash function requirements.

**Complexity Analysis**: O(n) where n is the total input data size, with parallel speedup potential
The time complexity remains linear with respect to input size, but can achieve significant speedup by processing multiple blocks concurrently. The space complexity remains O(1) per thread for the hash state.

**Dataset/Workload Details**:
- Data size: 500 MB of random data for multi-core processing
- Block distribution: Multiple data blocks processed in parallel
- Threading model: Each thread processes independent data blocks
- Work distribution: Load-balanced block assignment

**Measurements Captured**:
- Total execution time and throughput improvement
- Parallel processing efficiency and scaling
- Cryptographic instruction utilization across cores
- Memory bandwidth during concurrent processing
- Speedup compared to single-core implementation

**Hardware Behavior Targeted**:
- Multi-core cryptographic instruction performance
- Memory bandwidth utilization under parallel loads
- CPU pipeline efficiency with parallel cryptographic operations
- Cache performance with concurrent data access

**Notes on Deterministic Behavior**: Produces identical hash values to single-core implementation with improved throughput through parallel processing.

### Multi-Core Test 5: Parallel String Sorting

**Algorithm Used**: Parallel Sorting Algorithm (Parallel Mergesort or Samplesort)
The algorithm uses parallel divide-and-conquer techniques to sort large arrays of strings. The implementation may use parallel mergesort, samplesort, or other parallel sorting algorithms that can efficiently distribute work across multiple threads. The algorithm handles string comparison operations in parallel while maintaining correct ordering.

**Complexity Analysis**: O(n log n) with parallel speedup potential
The algorithm maintains O(n log n) complexity but can achieve significant speedup through parallel execution. The space complexity may increase slightly due to temporary storage needed for parallel operations.

**Dataset/Workload Details**:
- Data size: 5 million random strings of varying lengths (10-1000 characters)
- String comparison: Parallelized comparison operations
- Memory usage: String objects and parallel sorting overhead
- Threading model: Thread pool with work-stealing for load balancing

**Measurements Captured**:
- Total execution time and parallel speedup
- String comparison operations per second across all cores
- Memory allocation performance under parallel loads
- Load balancing efficiency across threads
- Sorting algorithm efficiency with parallel execution

**Hardware Behavior Targeted**:
- Multi-core string comparison and memory access performance
- Memory subsystem behavior with concurrent allocations
- CPU cache efficiency with parallel pointer operations
- Branch prediction with parallel comparison operations

**Notes on Deterministic Behavior**: Produces identical sorted output as single-core implementation with improved performance through parallel execution.

### Multi-Core Test 6: Parallel Ray Tracing

**Algorithm Used**: Parallel Ray Tracing with Scanline/Tile-based Distribution
The algorithm distributes pixel calculations across multiple threads, either by assigning scanlines, tiles, or individual rays to different threads. The implementation may use techniques like dynamic load balancing to handle the varying computational complexity of different regions of the image.

**Complexity Analysis**: O(w×h×r) where w=width, h=height, r=recursion depth, with parallel speedup potential
The complexity remains the same but can be distributed across multiple cores for improved performance. The space complexity is O(r) per thread for recursion stack depth.

**Dataset/Workload Details**:
- Scene complexity: 20-30 spheres with different materials for multi-core systems
- Image resolution: 1024×1024 pixels for multi-core rendering
- Ray depth: 5-7 levels of recursion for reflections
- Threading model: Tile-based or scanline-based parallelization

**Measurements Captured**:
- Total rendering time and frames per second improvement
- Rays processed per second across all cores
- Parallel rendering efficiency and scaling
- Memory bandwidth during concurrent scene access
- Load balancing effectiveness across threads

**Hardware Behavior Targeted**:
- Multi-core floating-point performance for geometric calculations
- Memory subsystem behavior with concurrent scene access
- CPU cache coherency during parallel rendering
- Branch prediction with parallel intersection tests

**Notes on Deterministic Behavior**: Produces identical rendered output to single-core implementation with improved performance through parallel execution.

### Multi-Core Test 7: Parallel Compression/Decompression

**Algorithm Used**: Parallel LZ77-based Compression with Block Processing
The implementation divides the input data into blocks that can be compressed independently, with potential for dictionary sharing between blocks. Advanced implementations may use parallel parsing techniques or multi-threaded entropy coding stages.

**Complexity Analysis**: O(n×m) average where n is input size and m is the average search window size, with limited parallelization potential
While the compression algorithm has inherent sequential dependencies, certain stages like entropy coding or block-level processing can be parallelized. The space complexity increases due to multiple compression contexts.

**Dataset/Workload Details**:
- Input size: 200 MB of mixed content for multi-core processing
- Block size: Configurable block sizes for parallel processing
- Compression ratio: Expected 2:1 to 5:1 depending on input
- Threading model: Block-parallel with potential dictionary sharing

**Measurements Captured**:
- Total compression/decompression time and throughput
- Parallel compression efficiency and scaling
- Memory bandwidth during concurrent processing
- Compression ratio maintenance with parallel execution
- Synchronization overhead impact

**Hardware Behavior Targeted**:
- Multi-core memory bandwidth utilization for sliding window operations
- CPU cache performance with concurrent data access
- Integer arithmetic performance across multiple cores
- Thread synchronization overhead for shared dictionaries

**Notes on Deterministic Behavior**: Produces identical compressed output to single-core implementation with improved throughput through parallel processing.

### Multi-Core Test 8: Parallel Monte Carlo Simulation

**Algorithm Used**: Parallel Monte Carlo with Independent Sample Sets
The algorithm divides the total sample count across multiple threads, with each thread generating and processing independent sets of random samples. The results from all threads are then combined to produce the final estimate. Each thread maintains its own random number generator state.

**Complexity Analysis**: O(n) where n is the number of random samples, with linear parallel speedup potential
The algorithm complexity remains linear but can achieve near-linear speedup by distributing samples across multiple cores. The space complexity is O(1) per thread.

**Dataset/Workload Details**:
- Sample count: 50 million random points for multi-core systems
- Random generation: Thread-local PRNG for statistical validity
- Arithmetic operations: Distributed across multiple cores
- Threading model: Embarrassingly parallel with result aggregation

**Measurements Captured**:
- Total execution time and speedup achieved
- Random number generation throughput across cores
- Floating-point operations per second improvement
- Statistical accuracy maintenance with parallel execution
- Result aggregation efficiency

**Hardware Behavior Targeted**:
- Multi-core floating-point unit utilization
- Random number generator performance across cores
- CPU pipeline efficiency with parallel arithmetic workloads
- Memory subsystem with concurrent counter updates

**Notes on Deterministic Behavior**: With fixed random seeds per thread, produces reproducible results with improved performance through parallel execution.

### Multi-Core Test 9: Parallel JSON Parsing

**Algorithm Used**: Parallel JSON Parsing with Chunk-based Processing
The implementation divides large JSON documents into chunks that can be parsed independently, with special handling for JSON structures that span chunk boundaries. Alternatively, the algorithm may parse independent sub-objects in parallel or use parallel validation techniques.

**Complexity Analysis**: O(n) where n is the input JSON size, with limited parallelization potential
While JSON parsing has inherent sequential dependencies due to the grammar structure, certain operations like validation or processing of independent sub-objects can be parallelized. The space complexity is O(d) per thread where d is the local nesting depth.

**Dataset/Workload Details**:
- Input size: 50 MB of complex, nested JSON data
- Structure depth: 10-15 levels of nesting for multi-core systems
- Data types: Mix of objects, arrays, strings, numbers, booleans
- Threading model: Chunk-parallel or sub-object parallel

**Measurements Captured**:
- Total parsing time and throughput improvement
- Parallel parsing efficiency and scaling
- Memory allocation performance under parallel loads
- Error detection and handling with parallel processing
- Chunk boundary handling overhead

**Hardware Behavior Targeted**:
- Multi-core string processing and character comparison
- Memory subsystem with concurrent data structure allocation
- CPU cache efficiency for parallel string operations
- Branch prediction with parallel state machine transitions

**Notes on Deterministic Behavior**: Produces identical data structures to single-core implementation with improved performance through parallel processing where possible.

### Multi-Core Test 10: Parallel N-Queens Problem

**Algorithm Used**: Parallel Backtracking with Search Space Division
The algorithm divides the initial search space among multiple threads, with each thread exploring a different portion of the solution space. Advanced implementations may use techniques like work-stealing to balance the load when some threads finish their assigned portion earlier than others.

**Complexity Analysis**: O(N!) worst case, but typically much less due to pruning, with parallel speedup potential
The algorithm complexity remains the same but can achieve speedup by exploring different branches of the search tree in parallel. The space complexity is O(N) per thread for local board state.

**Dataset/Workload Details**:
- Board size: 16×16 (N=16) for multi-core computational challenge
- Solution search: Distributed search space exploration across threads
- Constraint checking: Parallel column, row, and diagonal conflict detection
- Threading model: Search space division with work-stealing

**Measurements Captured**:
- Total execution time and parallel speedup
- Search space exploration rate across all cores
- Load balancing efficiency and work-stealing effectiveness
- Solution discovery rate improvement
- Thread synchronization overhead for shared results

**Hardware Behavior Targeted**:
- Multi-core branch prediction with parallel conditional logic
- Memory subsystem with concurrent board state management
- CPU cache efficiency for parallel constraint checking
- Thread scheduling efficiency with irregular workloads

**Notes on Deterministic Behavior**: Finds the same solutions as single-core implementation with improved performance through parallel search space exploration.

## 3. Scaling & Load Adjustment

### Rules for Adapting Workload to Device Tier

The benchmark automatically adjusts workload parameters based on device performance tier to ensure meaningful results across all device classes:

**Slow Tier Devices** (entry-level smartphones, budget tablets):
- Prime number generation: Reduce range to 1 million (1,000,000)
- Fibonacci sequence: Calculate for n = 30 to 38
- Matrix multiplication: Use 500×500 matrices
- Hash computing: Process 25 MB of data
- String sorting: Sort 250,000 strings
- Ray tracing: 256×256 resolution, 2 reflection levels
- Compression: Process 25 MB of data
- Monte Carlo: 25 million samples
- JSON parsing: 2.5 MB of JSON data
- N-Queens: Solve for N=12

**Mid Tier Devices** (mid-range smartphones, mainstream tablets):
- Prime number generation: Standard 10 million (10,000,000) range
- Fibonacci sequence: Calculate for n = 35 to 42
- Matrix multiplication: Use 800×800 matrices
- Hash computing: Process 60 MB of data
- String sorting: Sort 750,000 strings
- Ray tracing: 400×400 resolution, 3 reflection levels
- Compression: Process 40 MB of data
- Monte Carlo: 60 million samples
- JSON parsing: 6 MB of JSON data
- N-Queens: Solve for N=14

**Flagship Tier Devices** (high-end smartphones, premium tablets):
- Prime number generation: Extended to 20 million (20,000) range
- Fibonacci sequence: Calculate for n = 40 to 47
- Matrix multiplication: Use 1200×1200 matrices
- Hash computing: Process 150 MB of data
- String sorting: Sort 1.5 million strings
- Ray tracing: 600×600 resolution, 5 reflection levels
- Compression: Process 75 MB of data
- Monte Carlo: 150 million samples
- JSON parsing: 15 MB of JSON data
- N-Queens: Solve for N=16

### Threading Model

The benchmark implements three distinct threading approaches to comprehensively test CPU capabilities:

**Single-Core Test**:
- All tests run on a single thread to measure peak single-core performance
- Uses the main application thread or a dedicated single thread
- Focuses on per-core performance metrics
- Essential for understanding peak computational capability per core

**Multi-Core Test**:
- Distributes workload across multiple cores using thread pools
- Typically uses number of physical cores as thread count
- Measures effective multi-core utilization
- Evaluates synchronization overhead and inter-core communication

**Saturation Test**:
- Creates more threads than available cores to test scheduler efficiency
- Uses thread count of 2× physical cores or higher
- Measures system's ability to handle thread contention
- Evaluates thermal and power management under sustained load

### Dynamic Warm-up Rules

To ensure accurate measurements and account for system optimizations:

**JIT Compilation Warm-up**:
- Execute each test algorithm multiple times (5-10 iterations) before timing begins
- Allow Just-In-Time compiler to optimize frequently used code paths
- Monitor performance stabilization before starting timed measurements
- Perform 3 warm-up iterations specifically for JIT optimization

**CPU Governor Adjustment**:
- Temporarily set CPU governor to "performance" mode during benchmark
- Monitor and log any governor changes during testing
- Restore original settings after benchmark completion
- Validate that frequency scaling does not affect results

**Cache Warming**:
- Pre-load test data into CPU caches before performance measurement
- Execute preliminary operations to populate instruction and data caches
- Account for cache miss penalties in performance calculations
- Measure both cold and warm cache performance where applicable

### Iteration Count for Each Benchmark

Each benchmark test is executed 3 times for statistical validity, and the results are averaged to produce the final score. The best practice for benchmarking suggests that multiple runs help account for system noise, thermal throttling, and other variables that might affect performance measurements. The median result is used for scoring to reduce the impact of outliers.

**Single-Core Tests**:
- Each test runs 3 times with warm-up iterations before timing
- Results are averaged after removing outliers (values >15% deviation from median)
- Minimum run time of 1 second per iteration to ensure measurement accuracy

**Multi-Core Tests**:
- Each test runs 3 times with warm-up iterations before timing
- Results are averaged after removing outliers (values >15% deviation from median)
- Minimum run time of 2 seconds per iteration to account for thread synchronization overhead

## 4. Performance Metrics

### Execution Time

Execution time is the primary metric for all CPU benchmark tests, measured from the start of the actual computation to completion. The system captures:

- **Wall-clock time**: Total elapsed time from start to finish
- **CPU time**: Actual processor time consumed by the benchmark process
- **Clock cycles**: Processor cycles consumed (where hardware counters available)
- **Jitter measurements**: Variations in execution time across multiple runs

### Throughput (Operations per Second)

Throughput metrics measure the computational rate achieved by the device:

- **Basic operations per second**: Simple arithmetic operations executed
- **Complex operations per second**: Algorithm-specific operations (e.g., comparisons for sorting)
- **Memory operations per second**: Memory reads/writes for memory-bound algorithms
- **Floating-point operations per second (FLOPS)**: For floating-point intensive tests

### Correctness Check / Checksum Validation

To ensure results are meaningful and not due to errors or cheating:

- **Result verification**: Validate algorithm outputs against known correct values
- **Checksum validation**: Calculate checksums of final results for integrity checking
- **Statistical validation**: Verify Monte Carlo results converge to expected values
- **Cross-validation**: Compare results between different implementations where possible

### Multi-thread Scaling Efficiency (%)

For multi-threaded tests, efficiency is calculated as:

```
Scaling Efficiency % = (Multi-thread Performance / Single-thread Performance) / Number of Threads × 100
```

- **Perfect scaling**: 10% efficiency indicates linear scaling
- **Sub-linear scaling**: Below 100% due to synchronization overhead
- **Super-linear scaling**: Above 100% due to cache effects (rare)

## 5. Scoring Conversion

### Formula for Converting Raw Performance to Normalized Score

The raw performance metrics are converted to normalized scores using:

```
Normalized Score = (Device Performance / Baseline Performance) × 10
```

For time-based metrics (lower is better):
```
Normalized Score = (Baseline Time / Device Time) × 100
```

For throughput-based metrics (higher is better):
```
Normalized Score = (Device Throughput / Baseline Throughput) × 100
```

### Baseline Reference Device Detail

The baseline device used for normalization is the Google Pixel 3 (2018) with:
- CPU: Snapdragon 845 (4×2.8 GHz Kryo 385 Gold + 4×1.8 GHz Kryo 385 Silver)
- Cores: 8-core (4+4 big.LITTLE configuration)
- Architecture: ARM64 with 64-bit support
- Baseline performance values established through extensive testing

### CPU Category Weight in Global Scoring

CPU benchmarks contribute 35% to the overall system benchmark score:
- Integer Operations: 12% of total score
- Floating Point Operations: 10% of total score
- Multi-threading: 8% of total score
- Memory Interface: 5% of total score

## 6. Optimization & Anti-Cheat Policy

### Detection of Governor Manipulation

The system monitors for performance manipulation through:

**CPU Frequency Monitoring**:
- Check CPU frequency at benchmark start and end
- Log any changes in frequency scaling behavior
- Flag unusual frequency spikes during testing
- Compare against expected thermal throttling patterns

**Thread Priority Verification**:
- Monitor process and thread priority levels
- Detect attempts to elevate benchmark priority
- Flag use of root-level priority manipulation
- Validate that normal scheduling applies

### Detection of Background Process Interference

**Resource Usage Monitoring**:
- Track CPU usage by other processes during benchmark
- Monitor memory pressure from background applications
- Detect I/O interference from other processes
- Log system load during benchmark execution

**Thermal State Monitoring**:
- Monitor CPU temperature throughout benchmark
- Detect thermal throttling events
- Account for pre-existing thermal conditions
- Adjust scoring based on thermal limitations

### Rules for Rejecting Invalid Runs

A benchmark run is rejected if:

- CPU frequency changes significantly during testing
- More than 5% of CPU time is consumed by other processes
- Thermal throttling occurs during performance-critical sections
- Memory allocation fails or garbage collection interferes
- Unexpected system events interrupt the benchmark
- Results deviate significantly (>20%) from previous runs
- Governor mode changes during testing
- Root or system-level performance modifications detected

### Integrity Validation

**Code Integrity Checks**:
- Verify that benchmark code has not been modified
- Check for hooking or interception of system calls
- Validate that timing functions return accurate measurements
- Ensure no external performance enhancement tools are active

**Result Validation**:
- Verify results fall within expected ranges
- Check for impossible performance results
- Cross-reference with device hardware capabilities
- Validate consistency across multiple test runs