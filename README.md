# FinalBenchmark2 - CPU Benchmark Application

A comprehensive CPU benchmarking application that tests processor performance with various computational tasks using Rust-powered native libraries.

## 🚀 Features

- **Real-time Benchmarking**: Visual progress updates for each CPU test
- **Comprehensive Tests**: 10 single-core and 10 multi-core benchmark tests
- **Native Performance**: Rust-powered CPU-intensive operations via JNI
- **Modern UI**: Jetpack Compose with Material3 design
- **Detailed Reports**: Final score calculation with ratings

## 📋 Benchmark Tests

### Single-Core Tests
- Prime Generation
- Fibonacci Calculation (Recursive)
- Matrix Multiplication
- Hash Computing (SHA-2/MD5)
- String Sorting
- Ray Tracing
- Compression (LZMA)
- Monte Carlo Pi Estimation
- JSON Parsing
- N-Queens Problem

### Multi-Core Tests
- Same algorithms optimized for multi-core execution

## 🛠️ Project Structure

```
app/src/main/java/com/ivarna/finalbenchmark2/
├── cpuBenchmark/           # Native interface and managers
│   ├── BenchmarkEvent.kt   # Data classes
│   ├── CpuBenchmarkNative.kt # JNI interface
│   └── BenchmarkManager.kt # Benchmark orchestrator
├── ui/screens/            # Compose UI screens
│   ├── WelcomeScreen.kt   # Intro screen
│   ├── BenchmarkScreen.kt # Real-time progress
│   └── ResultScreen.kt    # Final report
├── navigation/            # Navigation graph
└── MainActivity.kt        # Entry point
```

## 🔧 Building the Native Library

The application uses a Rust-based CPU benchmark engine. To build the native library for Android:

1. **Setup Environment**:
   ```bash
   export ANDROID_NDK_HOME=/path/to/your/ndk
   rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android i686-linux-android
   cargo install cargo-ndk
   rustup component add rust-src
   ```

2. **Build Native Libraries**:
   ```bash
   ./build_android_native.sh
   ```

3. **Build Android App**:
   ```bash
   ./gradlew assembleDebug
   ```

For detailed instructions, see [README_NATIVE_BUILD.md](README_NATIVE_BUILD.md).

## 📱 Running the Application

After building both the native library and the Android app:

```bash
# Install the app
adb install app/build/outputs/apk/debug/app-debug.apk

# Or run directly from Gradle
./gradlew installDebug
```

## 🏗️ Architecture

- **Frontend**: Kotlin + Jetpack Compose with Material3
- **Backend**: Rust-powered native library via JNI
- **Communication**: JSON-based parameter passing and result retrieval
- **UI Flow**: Welcome → Benchmark → Result screens
- **State Management**: Kotlin Coroutines + Flows for real-time updates

## 🧪 Testing Flow

1. User starts benchmark from Welcome screen
2. App attempts to call native Rust functions
3. If native library is available, runs actual CPU-intensive tests
4. If native library is missing, falls back to simulation mode
5. Real-time progress updates shown during execution
6. Final results displayed with scores and ratings

## 🎯 Implementation Status

✅ **Complete CPU Benchmark Flow**
- Data classes for benchmark events and results
- JNI interface for Rust FFI
- Welcome, Benchmark, and Result screens
- Real-time progress updates
- Navigation between screens
- Proper error handling with graceful fallback
- App icon integration

✅ **Native Library Integration Ready**
- Proper JNI structure with all function declarations
- Error handling for missing native libraries
- Simulation fallback when native functions unavailable
- Build scripts and documentation for native compilation

The implementation is complete and ready for integration with the compiled Rust native library. When the native library is included, the app will execute actual Rust CPU benchmark functions rather than simulations.