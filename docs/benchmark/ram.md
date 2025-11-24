# RAM Benchmark Algorithms & Methodology

## Summary Table

| Test Name | Primary Focus | Performance Metric | Implementation Method |
|-----------|---------------|-------------------|----------------------|
| Sequential Read/Write Speed | Memory bandwidth, sequential access | MB/s read/write speed | ByteBuffer or native arrays |
| Random Access Latency | Random access speed, cache misses | Average latency (ns), ops/sec | Random index generation |
| Memory Copy Bandwidth | Memory copy throughput | MB/s for different methods | System.arraycopy(), ByteBuffer.put(), etc. |
| Multi-threaded Memory Bandwidth | Multi-channel performance | Total bandwidth scaling, peak throughput | Parallel operations across threads |
| Cache Hierarchy Test | Cache detection and performance | Access time vs array size | Arrays of increasing sizes |

## 1. Introduction

### Purpose of RAM Benchmarking

RAM benchmarking evaluates the memory subsystem performance of a device, which is critical for overall system performance. These tests measure how effectively the device can access and manipulate data in its main memory, which directly impacts application responsiveness, multitasking capabilities, and overall system performance.

The RAM benchmarks focus on:
- Memory bandwidth utilization for sequential and random access patterns
- Latency characteristics of different memory access patterns
- Cache hierarchy performance and size detection
- Multi-threaded memory access efficiency
- Memory copy operation optimization

### Performance Aspects Evaluated

RAM benchmark tests evaluate multiple dimensions of memory subsystem performance:

- **Sequential Access Performance**: Measures bandwidth for sequential read/write operations
- **Random Access Latency**: Tests memory access time for random access patterns
- **Memory Copy Efficiency**: Evaluates different methods of copying data between memory locations
- **Multi-threaded Memory Performance**: Assesses memory bandwidth under concurrent access
- **Cache Hierarchy Performance**: Determines cache sizes and performance characteristics
- **Memory Controller Efficiency**: Tests how effectively the memory controller handles requests

### Test Selection Rationale

The selected RAM tests were chosen based on their real-world relevance and computational diversity:

- **Real-world Relevance**: Each test represents common memory access patterns found in actual applications, from streaming data to random lookups
- **Computational Diversity**: Tests cover different memory access patterns (sequential, random, cached vs. main memory)
- **Resource Utilization Patterns**: Tests vary in their memory bandwidth, latency, and cache usage to provide comprehensive coverage
- **Measurable Performance Differences**: Tests are designed to show meaningful performance differences across different memory subsystem configurations

## 2. Test List

### RAM Test 1: Sequential Read/Write Speed

**Algorithm Used**: Sequential Memory Access Pattern
The test allocates large buffers (100MB - 1GB) and performs sequential read/write operations to measure memory bandwidth. The implementation uses either ByteBuffer or native arrays with sequential access patterns to maximize memory throughput. The algorithm iterates through the allocated buffer in order, performing read and write operations to stress the memory bandwidth.

**Complexity Analysis**: O(n) where n is the buffer size
The computational complexity is linear with respect to the buffer size. The space complexity is O(n) for the allocated buffer. The test is designed to be memory-bound rather than CPU-bound to accurately measure memory performance.

**Dataset/Workload Details**: 
- Buffer size: 100MB to 1GB depending on device tier
- Access pattern: Sequential read/write from start to end
- Data pattern: Pseudo-random data to prevent compression
- Implementation: ByteBuffer or native arrays with sequential iteration
- Threading model: Single-threaded for consistent measurements

**Measurements Captured**:
- Read speed in MB/s
- Write speed in MB/s
- Memory bandwidth utilization percentage
- Cache hit/miss ratios during sequential access
- Consistency of performance across buffer sections

**Hardware Behavior Targeted**:
- Memory controller bandwidth
- Memory bus performance
- Cache line utilization efficiency
- DRAM access timing parameters
- Memory subsystem pipeline efficiency

**Notes on Deterministic Behavior**: Results are deterministic for the same hardware configuration, though performance will vary based on available memory bandwidth.

### RAM Test 2: Random Access Latency

**Algorithm Used**: Random Index Memory Access
The test creates a large array and performs random read/write operations at random indices to measure access latency. The implementation uses Random() to generate indices and measures the time taken for each access operation. The algorithm maintains a large working set that exceeds cache size to ensure memory access rather than cache access.

**Complexity Analysis**: O(n) where n is the number of operations
The computational complexity is linear with respect to the number of operations. The space complexity is O(s) where s is the size of the working array. The test is designed to stress random access patterns and cache miss penalties.

**Dataset/Workload Details**:
- Array size: 512MB to 2GB to exceed cache sizes
- Access pattern: Random indices generated by PRNG
- Operation count: 1 million random accesses
- Data pattern: Pre-filled array to prevent initialization overhead
- Threading model: Single-threaded to isolate latency measurements

**Measurements Captured**:
- Average access latency in nanoseconds
- Operations per second for random access
- Cache miss rate during random access
- Memory access time distribution
- Consistency of latency measurements

**Hardware Behavior Targeted**:
- Memory access latency
- Cache miss penalties
- Memory controller arbitration
- DRAM row/column access timing
- TLB (Translation Lookaside Buffer) performance

**Notes on Deterministic Behavior**: Results are statistically consistent across runs, though individual access times will vary due to the random nature of the access pattern.

### RAM Test 3: Memory Copy Bandwidth

**Algorithm Used**: Multiple Memory Copy Methods Comparison
The test copies large blocks of data between arrays using different methods to compare their efficiency. The implementation tests System.arraycopy(), ByteBuffer.put(), manual loops, and native memcpy (where available) to determine the most efficient method for different scenarios.

**Complexity Analysis**: O(n) where n is the data size being copied
The computational complexity is linear with respect to the data size. The space complexity is O(2n) for source and destination arrays. The test measures the efficiency of different copy mechanisms.

**Dataset/Workload Details**:
- Data size: 100MB to 1GB depending on device tier
- Copy methods: System.arraycopy(), ByteBuffer.put(), manual loops, native memcpy
- Source/destination: Pre-allocated arrays of equal size
- Operation count: Multiple iterations for each method
- Threading model: Single-threaded for consistent measurements

**Measurements Captured**:
- Copy speed in MB/s for each method
- CPU overhead for each copy method
- Memory bandwidth utilization for each method
- Comparison of efficiency between methods
- Consistency of performance across methods

**Hardware Behavior Targeted**:
- Memory copy performance characteristics
- DMA (Direct Memory Access) efficiency
- Memory controller optimization for bulk transfers
- CPU cache coherency during copy operations
- Memory subsystem pipeline efficiency

**Notes on Deterministic Behavior**: Results are deterministic for each copy method, allowing for direct comparison between different approaches.

### RAM Test 4: Multi-threaded Memory Bandwidth

**Algorithm Used**: Parallel Memory Access Pattern
The test runs parallel read/write operations across multiple threads to measure multi-channel memory performance. The implementation gradually increases thread count (1, 2, 4, 8, 16 threads) to test memory controller saturation and determine optimal thread count for memory operations.

**Complexity Analysis**: O(n×t) where n is buffer size per thread and t is thread count
The computational complexity scales with both buffer size and thread count. The space complexity is O(n×t) for thread-specific buffers. The test measures scalability of memory performance with concurrent access.

**Dataset/Workload Details**:
- Thread count: 1, 2, 4, 8, 16 threads
- Buffer size per thread: 50MB to 200MB depending on thread count
- Access pattern: Sequential read/write for each thread
- Synchronization: Minimal synchronization to prevent artificial bottlenecks
- Memory allocation: Thread-local buffers to minimize contention

**Measurements Captured**:
- Total bandwidth scaling with thread count
- Peak throughput achieved
- Thread scaling efficiency
- Memory controller saturation point
- Per-thread performance degradation

**Hardware Behavior Targeted**:
- Multi-channel memory performance
- Memory controller arbitration efficiency
- Cache coherency with multiple threads
- Memory bandwidth sharing between threads
- Thread synchronization overhead impact

**Notes on Deterministic Behavior**: Results are deterministic for the same thread count and hardware configuration, though scaling patterns will be consistent across runs.

### RAM Test 5: Cache Hierarchy Test

**Algorithm Used**: Cache Boundary Detection
The test accesses data in arrays of increasing sizes (4KB → 1GB) to measure access speed and detect cache boundaries. The implementation measures access time for each array size to identify L1, L2, and L3 cache sizes and performance characteristics.

**Complexity Analysis**: O(n) for each array size where n is the array size
The computational complexity is linear with respect to array size for each test. The space complexity varies from O(4KB) to O(1GB) as different array sizes are tested. The test creates an access time vs. array size profile.

**Dataset/Workload Details**:
- Array sizes: 4KB, 8KB, 16KB, 32KB, 64KB, 128KB, 256KB, 512KB, 1MB, 2MB, 4MB, 8MB, 16MB, 32MB, 64MB, 128MB, 256MB, 512MB, 1GB
- Access pattern: Sequential access to detect cache behavior
- Operation count: Fixed number of operations per array size
- Data pattern: Pre-filled to prevent initialization overhead
- Threading model: Single-threaded to isolate cache effects

**Measurements Captured**:
- Access time vs. array size graph
- Cache size detection (L1, L2, L3 boundaries)
- Cache access speed vs. main memory speed
- Cache hit/miss ratios at different sizes
- Performance degradation beyond cache sizes

**Hardware Behavior Targeted**:
- L1 cache size and speed
- L2 cache size and speed
- L3 cache size and speed
- Main memory access performance
- Cache line size and efficiency

**Notes on Deterministic Behavior**: Results are deterministic and will consistently show cache boundaries for the same hardware configuration.

## 3. Scaling & Load Adjustment

### Rules for Adapting Workload to Device Tier

The benchmark automatically adjusts workload parameters based on device performance tier to ensure meaningful results across all device classes:

**Slow Tier Devices** (entry-level smartphones, budget tablets):
- Sequential Read/Write: 100MB buffer size
- Random Access: 512MB array size, 100K operations
- Memory Copy: 100MB data size
- Multi-threaded: Up to 4 threads
- Cache Test: Up to 128MB maximum array size

**Mid Tier Devices** (mid-range smartphones, mainstream tablets):
- Sequential Read/Write: 500MB buffer size
- Random Access: 1GB array size, 500K operations
- Memory Copy: 500MB data size
- Multi-threaded: Up to 8 threads
- Cache Test: Up to 512MB maximum array size

**Flagship Tier Devices** (high-end smartphones, premium tablets):
- Sequential Read/Write: 1GB buffer size
- Random Access: 2GB array size, 1M operations
- Memory Copy: 1GB data size
- Multi-threaded: Up to 16 threads
- Cache Test: Up to 1GB maximum array size

### Memory Management Strategies

**Garbage Collection Management**:
- Perform garbage collection before each test to ensure consistent memory state
- Monitor GC activity during testing to detect interference
- Use memory allocation patterns that minimize GC pressure
- Implement manual memory management where possible

**Memory Pressure Handling**:
- Check available memory before starting tests
- Adjust buffer sizes based on available memory
- Implement fallback mechanisms for low-memory situations
- Monitor system memory pressure during testing

### Dynamic Warm-up Rules

To ensure accurate measurements and account for system optimizations:

**Memory Warm-up**:
- Execute each test algorithm multiple times (3-5 iterations) before timing begins
- Allow memory caches to stabilize before measurements
- Monitor performance stabilization before starting timed measurements
- Perform warm-up runs to prime memory subsystem

**Memory Pre-allocation**:
- Pre-allocate all required buffers before timing
- Clear memory caches before benchmark to ensure consistency
- Monitor memory pressure during testing

**Iteration Count for Each Benchmark**:
Each RAM benchmark test is executed 3 times for statistical validity, and the results are averaged to produce the final score. The median result is used for scoring to reduce the impact of outliers.

**Sequential/Random Tests**:
- Each test runs 3 times with warm-up iterations before timing
- Results are averaged after removing outliers (values >15% deviation from median)
- Minimum run time of 2 seconds per iteration to ensure measurement accuracy

**Multi-threaded Tests**:
- Each test runs 3 times with warm-up iterations before timing
- Results are averaged after removing outliers (values >15% deviation from median)
- Minimum run time of 3 seconds per iteration to account for thread synchronization overhead

**Cache Tests**:
- Each array size tested multiple times for statistical validity
- Results averaged to reduce measurement noise
- Multiple passes to ensure cache state consistency

## 4. Performance Metrics

### Memory Bandwidth

Memory bandwidth is a primary metric for RAM benchmark tests, measuring the rate at which data can be read from or written to memory:

- **Read Bandwidth**: MB/s for sequential read operations
- **Write Bandwidth**: MB/s for sequential write operations
- **Copy Bandwidth**: MB/s for memory copy operations
- **Random Access Bandwidth**: Effective bandwidth for random access patterns

### Memory Latency

Memory latency measures the time required to access memory locations:

- **Sequential Access Latency**: Time per operation for sequential access
- **Random Access Latency**: Average time per operation for random access
- **Cache Hit Latency**: Time for cache-resident data access
- **Cache Miss Latency**: Time for main memory access after cache miss

### Resource Utilization

Resource metrics measure the efficiency of memory operations:

- **Memory Usage**: Peak and average memory consumption during tests
- **CPU Usage**: Processor time consumed during memory operations
- **Power Consumption**: Energy usage during memory testing (where available)
- **Cache Hit Rates**: Percentage of memory accesses satisfied by cache

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

The baseline device used for normalization is the Google Pixel 6 (2021) with:
- RAM: 8GB LPDDR5
- Memory Bus: 4266 MT/s (25.6 GB/s theoretical bandwidth)
- Cache: L1 (64KB per core), L2 (256KB per core), L3 (4MB shared)
- Baseline performance values established through extensive testing

### RAM Category Weight in Global Scoring

RAM benchmarks contribute 10% to the overall system benchmark score:
- Sequential Performance: 3% of total score
- Random Access: 2% of total score
- Memory Copy: 2% of total score
- Multi-threaded: 2% of total score
- Cache Performance: 1% of total score

## 6. Optimization & Anti-Cheat Policy

### Memory Management Verification

The system monitors for performance manipulation through:

**Memory Allocation Monitoring**:
- Verify that appropriate buffer sizes are allocated
- Check that memory is properly initialized before testing
- Monitor for memory leaks during testing
- Validate that memory access patterns are as expected

**Cache State Verification**:
- Monitor for cache warming effects
- Check that cache states are consistent between runs
- Validate that cache flushes occur when expected
- Ensure no external cache manipulation

### Performance Monitoring

**Resource Usage Monitoring**:
- Track memory usage by other processes during benchmark
- Monitor memory pressure from background applications
- Detect interference from other memory-intensive processes
- Log system load during benchmark execution

**Thermal State Monitoring**:
- Monitor memory temperature throughout benchmark
- Detect thermal throttling events
- Account for pre-existing thermal conditions
- Adjust scoring based on thermal limitations

### Rules for Rejecting Invalid Runs

A RAM benchmark run is rejected if:

- Memory allocation fails during testing
- More than 5% of memory operations are affected by other processes
- Thermal throttling occurs during performance-critical sections
- Unexpected system events interrupt the benchmark
- Results deviate significantly (>20%) from previous runs
- Memory management errors occur during testing
- Garbage collection significantly impacts measurements
- Memory pressure changes during testing

### Integrity Validation

**Code Integrity Checks**:
- Verify that benchmark code has not been modified
- Check for hooking or interception of memory API calls
- Validate that timing functions return accurate measurements
- Ensure no external memory performance enhancement tools are active

**Result Validation**:
- Verify results fall within expected ranges
- Check for impossible performance results
- Cross-reference with device hardware capabilities
- Validate consistency across multiple test runs

## Data Collected

During the RAM benchmark tests, the following data is collected and stored in the database:

### Benchmark Results
- Overall RAM score and performance grade
- Test duration and completion timestamps
- App version and verification status
- Global and category rankings

### Test Environment Data
- Ambient temperature during testing
- Battery levels at start and end
- Charging status and screen brightness
- WiFi, Bluetooth, and mobile data status
- Number of running applications
- Available RAM and storage
- Kernel version and build fingerprint
- Device temperature at start (CPU, GPU, battery)

### RAM Test Results
- Sequential Read/Write Speed test MB/s read/write speed
- Random Access Latency test average latency in nanoseconds
- Memory Copy Bandwidth test MB/s for different copy methods
- Multi-threaded Memory Bandwidth test total bandwidth scaling and peak throughput
- Cache Hierarchy Test access time vs array size and cache boundary detection
- All detailed metrics stored in JSON format in the `ram_test_results` column

### Full Benchmark Details
- CPU, AI/ML, GPU, Storage, and Productivity scores for comparison
- Detailed test results for all categories in JSON format

### Telemetry Data
- CPU, GPU, and battery temperature timelines
- CPU and GPU frequency timelines
- Battery level and memory usage timelines
- Power consumption timeline
- Thermal throttle events and performance state timeline
- Average and maximum temperature/frequency values
- Total throttle events and battery drain percentage
- Average and peak power consumption values