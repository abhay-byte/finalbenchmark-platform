#!/bin/bash

# Script to build Rust native library for Android
# This script compiles the Rust CPU benchmark library for different Android architectures

set -e  # Exit immediately if a command exits with a non-zero status

echo "Building Rust native library for Android..."

# Check if we're in the correct directory
if [ ! -d "benchmark/cpu_benchmark" ]; then
    echo "Error: Not in the project root directory"
    echo "Please run this script from the project root directory"
    exit 1
fi

# Define Android NDK standalone toolchain directory
NDK_HOME=${ANDROID_NDK_HOME:-$ANDROID_NDK_ROOT}

if [ -z "$NDK_HOME" ]; then
    echo "Error: ANDROID_NDK_HOME or ANDROID_NDK_ROOT environment variable not set"
    echo "Please set the environment variable to point to your Android NDK installation"
    echo "Example: export ANDROID_NDK_HOME=/path/to/your/ndk"
    exit 1
fi

echo "NDK Home: $NDK_HOME"

# Define target architectures
TARGETS=(
    "aarch64-linux-android"    # ARM64-v8a
    "armv7-linux-androideabi"  # armeabi-v7a
    "x86_64-linux-android"     # x86_64
    "i686-linux-android"       # x86
)

# Define target architectures for cargo
CARGO_TARGETS=(
    "aarch64-linux-android"
    "armv7-linux-androideabi"
    "x86_64-linux-android"
    "i686-linux-android"
)

# Rust toolchain targets to install
RUST_TARGETS=(
    "aarch64-linux-android"
    "armv7-linux-androideabi"
    "x86_64-linux-android"
    "i686-linux-android"
)

# Install required Rust targets
echo "Installing required Rust targets..."
for target in "${RUST_TARGETS[@]}"; do
    echo "Installing Rust target: $target"
    rustup target add "$target" || echo "Target $target already installed or failed to install"
done

# Install cargo-ndk if not already installed
if ! command -v cargo-ndk &> /dev/null; then
    echo "Installing cargo-ndk..."
    cargo install cargo-ndk
fi

# Install rust-src component which is required for Android builds
rustup component add rust-src

echo "Building for Android architectures..."

# Build using cargo-ndk for all targets
echo "Using cargo-ndk to build for all Android targets..."
cd benchmark/cpu_benchmark
cargo ndk --target arm64-v8a --output-dir ../../app/src/main/jniLibs/arm64-v8a build --release
cargo ndk --target armeabi-v7a --output-dir ../../app/src/main/jniLibs/armeabi-v7a build --release
cargo ndk --target x86_64 --output-dir ../../app/src/main/jniLibs/x86_64 build --release
cargo ndk --target x86 --output-dir ../../app/src/main/jniLibs/x86 build --release
cd ../..
echo "Moving libraries to correct locations..."
# Move libraries from nested directories to correct locations
if [ -f "app/src/main/jniLibs/arm64-v8a/arm64-v8a/libcpu_benchmark.so" ]; then
    mv app/src/main/jniLibs/arm64-v8a/arm64-v8a/libcpu_benchmark.so app/src/main/jniLibs/arm64-v8a/libcpu_benchmark.so
    rm -rf app/src/main/jniLibs/arm64-v8a/arm64-v8a
fi
if [ -f "app/src/main/jniLibs/armeabi-v7a/armeabi-v7a/libcpu_benchmark.so" ]; then
    mv app/src/main/jniLibs/armeabi-v7a/armeabi-v7a/libcpu_benchmark.so app/src/main/jniLibs/armeabi-v7a/libcpu_benchmark.so
    rm -rf app/src/main/jniLibs/armeabi-v7a/armeabi-v7a
fi
if [ -f "app/src/main/jniLibs/x86_64/x86_64/libcpu_benchmark.so" ]; then
    mv app/src/main/jniLibs/x86_64/x86_64/libcpu_benchmark.so app/src/main/jniLibs/x86_64/libcpu_benchmark.so
    rm -rf app/src/main/jniLibs/x86_64/x86_64
fi
if [ -f "app/src/main/jniLibs/x86/x86/libcpu_benchmark.so" ]; then
    mv app/src/main/jniLibs/x86/x86/libcpu_benchmark.so app/src/main/jniLibs/x86/libcpu_benchmark.so
    rm -rf app/src/main/jniLibs/x86/x86
fi

echo "Native library build completed!"
echo "Libraries are located in app/src/main/jniLibs/"

# Verify that the libraries were built successfully
echo "Verifying built libraries..."
for target_dir in app/src/main/jniLibs/*/; do
    if [ -f "$target_dir/libcpu_benchmark.so" ]; then
        echo "✓ Found library: $(basename $target_dir)/libcpu_benchmark.so"
    else
        echo "✗ Missing library: $(basename $target_dir)/libcpu_benchmark.so"
    fi
done

echo ""
echo "Next steps:"
echo "1. Run './gradlew installDebug' to build and install the Android app"
echo "2. The app will now use the actual Rust CPU benchmark functions instead of simulation"