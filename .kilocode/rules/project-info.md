# Project Information

## Overview
This is a comprehensive Android benchmarking application named "FinalBenchmark2" that tests CPU, GPU, RAM, and Storage performance with detailed scoring and visualization. The project is developed using Kotlin and Jetpack Compose, following modern Android development practices with the latest Android SDK.

## Project Structure
- **Root directory**: Contains top-level build configurations (build.gradle.kts, settings.gradle.kts)
- **app/**: Main application module with source code, resources, and manifests
- **benchmark/**: Core benchmarking framework with CPU, GPU, RAM, and Storage tests
- **docs/**: Documentation for benchmarking modules and database schema
- **gradle/**: Gradle wrapper and dependency management files
- **.kilocode/**: Project-specific rules and workflow documentation

## Technology Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (declarative UI)
- **SDK**: Android API level 36 (compileSdk), targetSdk 36, minSdk 24
- **Build System**: Gradle with Kotlin DSL (build.gradle.kts)
- **Database**: Room for local storage of benchmark results
- **Threading**: Kotlin Coroutines for asynchronous operations
- **JSON Handling**: Moshi for serialization/deserialization
- **Native Code**: Rust with JNI for CPU-intensive benchmarking operations

## Key Features
- CPU Benchmarking: Tests integer operations, floating-point calculations, multi-core performance, compression algorithms, and cryptographic operations
- AI/ML Benchmarking: Tests LLM inference, image classification, object detection, text embedding, and speech-to-text
- GPU Benchmarking: Tests rendering performance, compute operations, and memory bandwidth
- RAM Benchmarking: Tests memory read/write speeds, latency, and bandwidth
- Storage Benchmarking: Tests storage read/write speeds, IOPS, and latency
- Productivity Tests: Tests UI rendering, image processing, video encoding, and multi-tasking
- Scoring System: Normalized scores across all components with overall performance rating
- Results History: Stores and displays historical benchmark results
- Export Functionality: Export results in JSON, CSV, or text formats
- Modern Android architecture using Compose for UI with Material Design 3 components
- Thermal management to prevent device overheating during intensive tests
- Unit and instrumentation testing capabilities

## Application Details
- **Package Name**: com.ivarna.finalbenchmark2
- **Application ID**: com.ivarna.finalbenchmark2
- **Version**: 1.0 (versionCode: 1)
- **Main Activity**: MainActivity.kt with dashboard for test selection and device info
- **Architecture**: MVVM Pattern with clean separation of concerns
- **Navigation**: Single Activity architecture with Compose navigation
- **Benchmark Modes**: Full Benchmark (46 tests), Throttle Test, and Efficiency Test

## Dependencies
- AndroidX Core KTX
- Lifecycle Runtime KTX
- Activity Compose
- Jetpack Compose BOM (Bill of Materials)
- Compose UI components (graphics, tooling, material3)
- Room Database with DAO and TypeConverters
- JUnit for testing
- Espresso for Android testing
- Rust dependencies via JNI (rayon, sha2, md5, serde, serde_json, rand, num_cpus, jni)
- ONNX Runtime for AI/ML tests
- llama.cpp for LLM inference

## File Organization
- **Source Code**: app/src/main/java/com/ivarna/finalbenchmark2/
- **Resources**: app/src/main/res/ (drawables, values, mipmap, xml)
- **Tests**: app/src/test/ (unit tests) and app/src/androidTest/ (instrumentation tests)
- **Themes**: app/src/main/java/com/ivarna/finalbenchmark2/ui/theme/
- **Benchmarking Framework**: benchmark/ directory with separate modules for each component test
- **Documentation**: docs/ directory with detailed documentation for each benchmark type

## Build Configuration
- JVM Target: 11
- Compile Options: Java 11 compatibility
- ProGuard: Enabled for release builds
- Compose: Enabled as build feature
- NDK: Configured for JNI support with native Rust libraries