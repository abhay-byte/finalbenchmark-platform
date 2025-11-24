# Android Benchmarking App

A comprehensive Android application for benchmarking device performance across CPU, GPU, RAM, and Storage components.

## Features

- **CPU Benchmarking**: Tests integer operations, floating-point calculations, multi-core performance, compression algorithms, and cryptographic operations
- **GPU Benchmarking**: Tests rendering performance, compute operations, and memory bandwidth
- **RAM Benchmarking**: Tests memory read/write speeds, latency, and bandwidth
- **Storage Benchmarking**: Tests storage read/write speeds, IOPS, and latency
- **Scoring System**: Normalized scores across all components with overall performance rating
- **Results History**: Stores and displays historical benchmark results
- **Export Functionality**: Export results in JSON, CSV, or text formats

## Architecture

The app follows modern Android development practices:

- **MVVM Pattern**: Clean separation of concerns with ViewModels
- **Jetpack Compose**: Modern UI toolkit for building native Android interfaces
- **Room Database**: Local storage for benchmark results
- **Coroutines**: Asynchronous programming for background operations
- **Navigation Component**: Single Activity architecture with Compose navigation

## Components

### Core Framework
- `BenchmarkTest`: Base class for all benchmark tests
- `BenchmarkManager`: Orchestrates execution of multiple tests
- `ScoringSystem`: Calculates component and overall scores

### Benchmark Tests
- **CPU Tests**:
  - Integer Operations Test
  - Floating Point Operations Test
 - Multi-Core Test
  - Compression Test
  - Cryptography Test

- **GPU Tests**:
  - GPU Info Test
  - 2D Rendering Test
  - 3D Rendering Test
  - Compute Shader Test
  - GPU Memory Test

- **RAM Tests**:
  - Sequential Memory Test
  - Random Access Memory Test
  - Memory Bandwidth Test
  - Cache Performance Test
  - Memory Pool Test

- **Storage Tests**:
  - Sequential Storage Test
  - Random Access IOPS Test
  - Storage Latency Test
  - Write Amplification Test
 - Storage Comparison Test

### UI Components
- Dashboard Screen: Main screen for test selection and device info
- Execution Screen: Real-time progress indicators
- Results Screen: Detailed scores and visualizations
- History Screen: Previous benchmark results
- Settings Screen: Configuration options

## Getting Started

1. Clone the repository
2. Open in Android Studio
3. Build and run on your Android device

## Permissions

The app requires the following permissions:
- `WRITE_EXTERNAL_STORAGE` (for Android versions below 13)
- `READ_EXTERNAL_STORAGE` (for accessing storage benchmarks)

## Testing

The project includes:
- Unit tests for scoring algorithms
- Integration tests for benchmark workflow
- UI tests for core functionality

## Performance Considerations

- Thermal management to prevent device overheating
- Battery level monitoring
- Memory management during intensive operations
- Storage space validation

## Contributing

We welcome contributions to improve the benchmarking accuracy and add new tests. Please follow the existing code patterns and submit pull requests for review.

## License

This project is licensed under the MIT License - see the LICENSE file for details.