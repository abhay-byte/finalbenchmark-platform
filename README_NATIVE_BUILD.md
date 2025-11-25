# Native Library Build Instructions

This project includes a Rust-based CPU benchmark engine that is integrated into the Android application via JNI (Java Native Interface). This document explains how to build the native library for Android.

## Prerequisites

Before building the native library, ensure you have:

1. **Android NDK** (Native Development Kit) installed
2. **Rust** installed with the necessary target toolchains
3. **Cargo** (Rust's package manager)
4. **Cargo NDK** (for easier Android builds)

### Setting up Environment Variables

First, you need to set up the required environment variables. In your terminal, run:

```bash
export ANDROID_NDK_HOME=/path/to/your/ndk
# or
export ANDROID_NDK_ROOT=/path/to/your/ndk
```

Replace `/path/to/your/ndk` with the actual path to your Android NDK installation. You can find this in Android Studio under SDK Manager > SDK Tools > NDK (Side by side).

To make this permanent, add the export command to your shell profile (e.g., `~/.bashrc`, `~/.zshrc`, etc.).

### Installing Rust Targets and Cargo NDK

Install the required Rust cross-compilation targets and Cargo NDK:

```bash
# Install Rust targets for Android
rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android i686-linux-android

# Install cargo-ndk for easier Android builds
cargo install cargo-ndk

# Install additionally required tools
rustup component add rust-src
```

## Building the Native Library

### Using the Build Script

Run the provided build script to compile the Rust library for all supported Android architectures:

```bash
./build_android_native.sh
```

This script will:
1. Build the Rust CPU benchmark library for ARM64, ARMv7, x86_64, and x86 architectures
2. Copy the compiled `.so` files to the appropriate directories in the Android project (`app/src/main/jniLibs/`)

### Manual Build

Alternatively, you can manually build the library for each target architecture:

```bash
# Build for ARM64 (recommended for most modern Android devices)
cargo build --target aarch64-linux-android --release -p cpu_benchmark

# Build for ARMv7 (for older 32-bit ARM devices)
cargo build --target armv7-linux-androideabi --release -p cpu_benchmark

# Build for x86_64 (for 64-bit emulators)
cargo build --target x86_64-linux-android --release -p cpu_benchmark

# Build for x86 (for 32-bit emulators)
cargo build --target i686-linux-android --release -p cpu_benchmark
```

After building, copy the resulting `.so` files from `benchmark/cpu_benchmark/target/<target>/release/` to the corresponding directories in `app/src/main/jniLibs/`.

## Android Project Structure

The native libraries are expected in the following structure:

```
app/src/main/jniLibs/
├── arm64-v8a/
│   └── libcpu_benchmark.so
├── armeabi-v7a/
│   └── libcpu_benchmark.so
├── x86_64/
│   └── libcpu_benchmark.so
└── x86/
    └── libcpu_benchmark.so
```

## Integration with Android App

The Android application loads the native library using:

```kotlin
System.loadLibrary("cpu_benchmark")
```

This automatically loads the appropriate architecture-specific library based on the device.

## JNI Interface

The Kotlin code in `CpuBenchmarkNative.kt` provides the interface to the Rust functions. The Rust FFI functions are defined in `benchmark/cpu_benchmark/src/ffi.rs` and follow the JNI naming convention.

## Testing

After building the native library, you can run the Android application normally using:

```bash
./gradlew run
# or
./gradlew installDebug
```

## Troubleshooting

### Common Issues

1. **"Could not find libcpu_benchmark.so"**
   - Ensure the native library was built for the correct architecture
   - Verify the library is in the correct `jniLibs` subdirectory
   - Check that the library name matches what's specified in `System.loadLibrary()`

2. **"UnsatisfiedLinkError"**
   - Verify that the function signatures in Kotlin match those in Rust
   - Ensure the Rust functions are marked with `#[no_mangle]` and `extern "C"`

3. **Build fails with NDK path issues**
   - Verify that `ANDROID_NDK_HOME` or `ANDROID_NDK_ROOT` is set correctly
   - Ensure the NDK version is compatible with your build tools

### Verifying the Build

You can verify that the native library was built correctly by checking:

```bash
# Check if the library exists and its architecture
file app/src/main/jniLibs/arm64-v8a/libcpu_benchmark.so

# List symbols in the library to verify JNI functions exist
nm -D app/src/main/jniLibs/arm64-v8a/libcpu_benchmark.so | grep jni