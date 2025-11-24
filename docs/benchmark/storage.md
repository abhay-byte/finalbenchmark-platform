# Storage Benchmark Algorithms & Methodology

## Summary Table

| Test Name | Primary Focus | Performance Metric | Storage Type |
|-----------|---------------|-------------------|--------------|
| Sequential Read Speed | Sequential read performance | MB/s read speed | Internal/External |
| Sequential Write Speed | Sequential write performance | MB/s write speed | Internal/External |
| Random Read/Write (4K blocks) | Small block random access | IOPS, latency | Internal/External |
| Small File Operations | File system metadata | Files/second, operation latency | Internal/External |
| Database Performance (SQLite) | Database operations | Transactions/second, query time | Internal |
| Mixed Workload Test | Real-world usage patterns | Overall throughput, consistency | Internal |

## 1. Introduction

### Purpose of Storage Benchmarking

Storage benchmarking evaluates the performance of a device's storage subsystem, which is critical for application loading times, data access speed, and overall system responsiveness. These tests measure how effectively the device can read and write data to its storage media, which directly impacts user experience in terms of app launch times, file operations, and data processing capabilities.

The storage benchmarks focus on:
- Sequential read/write performance for large file operations
- Random access performance for small block operations
- File system metadata handling for small file operations
- Database performance for application data management
- Mixed workload performance simulating real-world usage
- Storage controller and media efficiency

### Performance Aspects Evaluated

Storage benchmark tests evaluate multiple dimensions of storage subsystem performance:

- **Sequential Access Performance**: Measures throughput for large file operations
- **Random Access Performance**: Tests performance for small block random operations
- **IOPS (Input/Output Operations Per Second)**: Evaluates the number of small operations per second
- **Latency Characteristics**: Tests response times for different storage operations
- **File System Performance**: Measures metadata handling efficiency
- **Database Performance**: Assesses real-world data management capabilities
- **Consistency Under Load**: Tests performance stability under sustained operations

### Test Selection Rationale

The selected storage tests were chosen based on their real-world relevance and computational diversity:

- **Real-world Relevance**: Each test represents common storage access patterns found in actual applications, from media playback to database operations
- **Access Pattern Diversity**: Tests cover sequential, random, small file, and database access patterns
- **Resource Utilization Patterns**: Tests vary in their storage bandwidth, IOPS, and metadata usage to provide comprehensive coverage
- **Measurable Performance Differences**: Tests are designed to show meaningful performance differences across different storage technologies (UFS, eMMC, NVMe, etc.)

## 2. Test List

### Storage Test 1: Sequential Read Speed

**Algorithm Used**: Large File Sequential Reading
The test reads large files sequentially from internal and external storage to measure maximum read throughput. The implementation creates files of appropriate size (100MB-1GB depending on device tier) and reads them in large chunks (1MB blocks) to maximize sequential read performance. The algorithm measures the time taken to read the entire file to determine read speed.

**Complexity Analysis**: O(n) where n is the file size
The computational complexity is linear with respect to the file size. The space complexity is O(c) where c is the chunk size used for reading (typically 1MB). The test is designed to be I/O-bound rather than CPU-bound to accurately measure storage performance.

**Dataset/Workload Details**: 
- File size: 100MB to 1GB depending on device tier
- Read chunk size: 1MB blocks for optimal sequential performance
- File content: Pseudo-random data to prevent compression
- Storage targets: Internal storage and external storage (if available)
- Threading model: Single-threaded for consistent measurements

**Measurements Captured**:
- Read speed in MB/s
- I/O throughput consistency
- Storage controller efficiency
- File system overhead for large reads
- Variance in read performance across file sections

**Hardware Behavior Targeted**:
- Storage controller bandwidth
- Storage media sequential read performance
- File system efficiency for large files
- Storage bus performance (UFS, eMMC, etc.)
- Cache effectiveness for sequential access

**Notes on Deterministic Behavior**: Results are deterministic for the same storage hardware, though performance may vary slightly due to storage wear leveling and other factors.

### Storage Test 2: Sequential Write Speed

**Algorithm Used**: Large File Sequential Writing
The test writes large files sequentially to internal and external storage to measure maximum write throughput. The implementation creates files of appropriate size (100MB-1GB depending on device tier) and writes them in large chunks (1MB blocks) to maximize sequential write performance. The algorithm measures the time taken to write the entire file to determine write speed.

**Complexity Analysis**: O(n) where n is the file size
The computational complexity is linear with respect to the file size. The space complexity is O(c) where c is the chunk size used for writing (typically 1MB). The test is designed to be I/O-bound rather than CPU-bound to accurately measure storage performance.

**Dataset/Workload Details**:
- File size: 100MB to 1GB depending on device tier
- Write chunk size: 1MB blocks for optimal sequential performance
- File content: Pseudo-random data to prevent compression artifacts
- Storage targets: Internal storage and external storage (if available)
- Threading model: Single-threaded for consistent measurements

**Measurements Captured**:
- Write speed in MB/s
- I/O throughput consistency
- Storage controller efficiency
- File system overhead for large writes
- Performance impact of wear leveling algorithms

**Hardware Behavior Targeted**:
- Storage controller bandwidth for writes
- Storage media sequential write performance
- File system efficiency for large files
- Storage bus performance (UFS, eMMC, etc.)
- Cache effectiveness for sequential writes

**Notes on Deterministic Behavior**: Results are deterministic for the same storage hardware, though performance may vary due to storage wear leveling and cache state.

### Storage Test 3: Random Read/Write (4K blocks)

**Algorithm Used**: Small Block Random Access Pattern
The test performs random access with small 4KB blocks to simulate real-world app usage patterns. The implementation creates a large file (100MB-500MB) and performs random reads and writes to different 4KB blocks within the file. The algorithm uses random position generation to access different blocks and measures both IOPS and latency.

**Complexity Analysis**: O(n) where n is the number of operations
The computational complexity is linear with respect to the number of operations. The space complexity is O(f) where f is the file size. The test is designed to stress random access performance and measure IOPS capabilities.

**Dataset/Workload Details**:
- File size: 100MB to 500MB depending on device tier
- Block size: 4KB blocks for random access
- Operation count: 50,00 to 200,000 random operations
- Access pattern: Random positions within the file
- Storage targets: Internal storage and external storage (if available)

**Measurements Captured**:
- IOPS (Input/Output Operations Per Second)
- Average read/write latency in microseconds
- Consistency of performance across operations
- Storage controller arbitration efficiency
- File system metadata handling performance

**Hardware Behavior Targeted**:
- Storage media random access performance
- Storage controller random I/O efficiency
- File system block allocation algorithms
- Cache effectiveness for small random operations
- Storage bus efficiency for small transfers

**Notes on Deterministic Behavior**: Results are statistically consistent across runs, though individual operation times will vary due to the random nature of the access pattern.

### Storage Test 4: Small File Operations

**Algorithm Used**: File System Metadata Testing
The test creates, reads, and deletes thousands of small files (1-10KB) to measure file system metadata performance. The implementation creates a directory structure and performs sequential operations on small files to stress the file system's ability to handle metadata operations efficiently.

**Complexity Analysis**: O(n) where n is the number of files
The computational complexity is linear with respect to the number of files. The space complexity is O(n×s) where s is the average file size. The test measures file system metadata handling efficiency.

**Dataset/Workload Details**:
- File count: 10,000 to 100,000 files depending on device tier
- File size: 1KB to 10KB per file
- Operations: Create, read, delete sequences
- Directory structure: Hierarchical structure to test metadata depth
- Storage targets: Internal storage and external storage (if available)

**Measurements Captured**:
- Files processed per second (create/read/delete)
- Average operation latency
- File system metadata performance
- Directory traversal efficiency
- Consistency of performance across operations

**Hardware Behavior Targeted**:
- File system metadata handling
- Directory structure efficiency
- Storage controller small file handling
- File allocation table performance
- Storage media efficiency for small files

**Notes on Deterministic Behavior**: Results are deterministic for the same file system and storage hardware, allowing for consistent comparison across devices.

### Storage Test 5: Database Performance (SQLite)

**Algorithm Used**: Real-world Database Operations
The test performs insert, query, update, and delete operations on SQLite database to simulate real-world database usage. The implementation creates a database with multiple tables and performs various operations with different complexity levels to measure database performance.

**Complexity Analysis**: O(n) for inserts/updates, O(log n) for indexed queries
The computational complexity varies based on operation type. Inserts and updates are typically O(1) to O(n) depending on indexing, while indexed queries are typically O(log n). The space complexity is O(d) where d is the database size.

**Dataset/Workload Details**:
- Database size: 10MB to 500MB depending on device tier
- Table structure: Multiple tables with relationships
- Operations: Insert, query, update, delete transactions
- Indexing: Multiple indexes to simulate real applications
- Storage target: Internal storage only

**Measurements Captured**:
- Transactions per second
- Query response time
- Database operation latency
- Index performance
- Consistency of database operations

**Hardware Behavior Targeted**:
- Storage performance for database workloads
- File system efficiency for database operations
- Storage controller transaction handling
- Database cache effectiveness
- Journal and WAL performance

**Notes on Deterministic Behavior**: Results are deterministic for the same database schema and operations, though performance will vary based on storage characteristics.

### Storage Test 6: Mixed Workload Test

**Algorithm Used**: Real-world Usage Simulation
The test combines sequential and random operations to simulate real app usage patterns. The implementation performs a mixture of large file operations, small random access, and database operations simultaneously to measure overall storage performance under realistic conditions.

**Complexity Analysis**: O(n) where n is the total amount of data processed
The computational complexity depends on the combination of operations performed. The space complexity varies based on the specific operations in the workload mix.

**Dataset/Workload Details**:
- Duration: 60 seconds of continuous mixed operations
- Operation mix: 40% sequential, 40% random, 20% database operations
- Concurrency: Multiple operation types running simultaneously
- Storage target: Internal storage only
- Workload pattern: Simulates real application usage

**Measurements Captured**:
- Overall throughput in MB/s
- Consistency of performance over time
- IOPS under mixed workload
- Storage controller switching efficiency
- Performance degradation under sustained load

**Hardware Behavior Targeted**:
- Storage controller workload management
- Mixed operation efficiency
- Thermal throttling under sustained load
- Storage performance consistency
- Quality of Service handling

**Notes on Deterministic Behavior**: Results are deterministic for the same workload pattern and storage hardware, though performance may vary due to thermal effects over time.

## 3. Scaling & Load Adjustment

### Rules for Adapting Workload to Device Tier

The benchmark automatically adjusts workload parameters based on device performance tier to ensure meaningful results across all device classes:

**Slow Tier Devices** (entry-level smartphones, budget tablets):
- Sequential Read/Write: 100MB files, 64KB chunk size
- Random Access: 100MB file, 25,000 operations
- Small Files: 10,000 files of 1KB each
- Database: 10MB database, basic operations
- Mixed Workload: 30 seconds duration

**Mid Tier Devices** (mid-range smartphones, mainstream tablets):
- Sequential Read/Write: 500MB files, 1MB chunk size
- Random Access: 250MB file, 100,000 operations
- Small Files: 50,000 files of 5KB each
- Database: 100MB database, complex queries
- Mixed Workload: 60 seconds duration

**Flagship Tier Devices** (high-end smartphones, premium tablets):
- Sequential Read/Write: 1GB files, 1MB chunk size
- Random Access: 500MB file, 200,000 operations
- Small Files: 100,000 files of 10KB each
- Database: 500MB database, advanced queries
- Mixed Workload: 90 seconds duration

### Storage Access Strategies

**Internal vs External Storage**:
- Internal storage tested for all operations
- External storage (SD cards, USB) tested where available
- Performance comparison between storage types
- File system differences accounted for

**File System Considerations**:
- Different file system performance characteristics
- Block size optimization for different storage types
- File system journaling effects
- Wear leveling algorithms for flash storage

### Dynamic Warm-up Rules

To ensure accurate measurements and account for system optimizations:

**Storage Warm-up**:
- Execute each test algorithm multiple times (3-5 iterations) before timing begins
- Allow storage caches to stabilize before measurements
- Monitor performance stabilization before starting timed measurements
- Perform warm-up runs to prime storage subsystem

**File System Pre-allocation**:
- Pre-allocate files before timing to prevent allocation overhead
- Clear storage caches before benchmark to ensure consistency
- Monitor storage pressure during testing

**Iteration Count for Each Benchmark**:
Each storage benchmark test is executed 3 times for statistical validity, and the results are averaged to produce the final score. The median result is used for scoring to reduce the impact of outliers.

**Sequential Tests**:
- Each test runs 3 times with warm-up iterations before timing
- Results are averaged after removing outliers (values >15% deviation from median)
- Minimum run time of 5 seconds per iteration to ensure measurement accuracy

**Random Access Tests**:
- Each test runs 3 times with warm-up iterations before timing
- Results are averaged after removing outliers (values >15% deviation from median)
- Minimum run time of 10 seconds per iteration for statistical significance

**Database Tests**:
- Each test runs 3 times with warm-up iterations before timing
- Results are averaged after removing outliers (values >15% deviation from median)
- Multiple transaction types tested for comprehensive results

**Mixed Workload Tests**:
- Each test runs once for 60-90 seconds depending on device tier
- Results averaged across the entire test duration
- Performance consistency measured over time

## 4. Performance Metrics

### Throughput Metrics

Throughput metrics measure the rate of data transfer for storage operations:

- **Sequential Read/Write Speed**: MB/s for large file operations
- **Random Access Speed**: MB/s for small block operations
- **Mixed Workload Throughput**: Overall MB/s under mixed usage
- **File Operation Rate**: Files per second for small file operations

### IOPS Metrics

IOPS metrics measure the number of operations per second for small block operations:

- **Random Read IOPS**: Operations per second for random reads
- **Random Write IOPS**: Operations per second for random writes
- **Mixed IOPS**: Operations per second under mixed workload
- **Small File IOPS**: Operations per second for file system operations

### Latency Metrics

Latency metrics measure the response time for storage operations:

- **Read Latency**: Average time for read operations
- **Write Latency**: Average time for write operations
- **Random Access Latency**: Time for small block random access
- **Database Query Time**: Response time for database operations

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
- Storage: UFS 3.1 (Universal Flash Storage)
- Capacity: 128GB internal storage
- Interface: UFS 3.1 with 2-lane configuration
- File System: ext4 with f2fs support
- Baseline performance values established through extensive testing

### Storage Category Weight in Global Scoring

Storage benchmarks contribute 10% to the overall system benchmark score:
- Sequential Performance: 3% of total score
- Random Access: 3% of total score
- Small File Operations: 2% of total score
- Database Performance: 1% of total score
- Mixed Workload: 1% of total score

## 6. Optimization & Anti-Cheat Policy

### Storage State Verification

The system monitors for performance manipulation through:

**Storage Cache Monitoring**:
- Check that storage caches are in normal state
- Monitor for forced cache flushing or warming
- Validate that standard cache policies are used
- Ensure no external cache manipulation

**File System Integrity**:
- Monitor for modified file system parameters
- Check for performance-enhancing file system modifications
- Validate that standard file system is used
- Ensure no custom file system drivers

### Performance Monitoring

**Resource Usage Monitoring**:
- Track storage usage by other processes during benchmark
- Monitor background processes that might affect storage
- Detect interference from other storage-intensive processes
- Log system load during benchmark execution

**Thermal State Monitoring**:
- Monitor storage temperature throughout benchmark
- Detect thermal throttling events
- Account for pre-existing thermal conditions
- Adjust scoring based on thermal limitations

### Rules for Rejecting Invalid Runs

A storage benchmark run is rejected if:

- Storage allocation fails during testing
- More than 5% of storage operations are affected by other processes
- Thermal throttling occurs during performance-critical sections
- Unexpected system events interrupt the benchmark
- Results deviate significantly (>20%) from previous runs
- Storage management errors occur during testing
- File system errors occur during testing
- External storage is removed during external storage tests

### Integrity Validation

**Code Integrity Checks**:
- Verify that benchmark code has not been modified
- Check for hooking or interception of storage API calls
- Validate that timing functions return accurate measurements
- Ensure no external storage performance enhancement tools are active

**Result Validation**:
- Verify results fall within expected ranges
- Check for impossible performance results
- Cross-reference with device hardware capabilities
- Validate consistency across multiple test runs