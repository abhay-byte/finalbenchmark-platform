package com.ivarna.finalbenchmark2.cpuBenchmark

/**
 * Enum representing all CPU benchmark types. This ensures type-safety and prevents string mismatch
 * errors.
 */
/**
 * Enum representing all benchmark algorithms.
 */
enum class BenchmarkName(val category: BenchmarkCategory) {
    // CPU Benchmarks
    PRIME_GENERATION(BenchmarkCategory.CPU),
    FIBONACCI_ITERATIVE(BenchmarkCategory.CPU),
    MATRIX_MULTIPLICATION(BenchmarkCategory.CPU),
    HASH_COMPUTING(BenchmarkCategory.CPU),
    STRING_SORTING(BenchmarkCategory.CPU),
    RAY_TRACING(BenchmarkCategory.CPU),
    COMPRESSION(BenchmarkCategory.CPU),
    MONTE_CARLO(BenchmarkCategory.CPU),
    JSON_PARSING(BenchmarkCategory.CPU),
    N_QUEENS(BenchmarkCategory.CPU),

    // AI Benchmarks
    LLM_INFERENCE(BenchmarkCategory.AI),
    IMAGE_CLASSIFICATION(BenchmarkCategory.AI),
    OBJECT_DETECTION(BenchmarkCategory.AI),
    TEXT_EMBEDDING(BenchmarkCategory.AI),

    SPEECH_TO_TEXT(BenchmarkCategory.AI),

    // GPU Native Benchmarks
    GPU_TRIANGLE_RENDERING(BenchmarkCategory.GPU),
    GPU_COMPUTE_MATRIX(BenchmarkCategory.GPU),
    GPU_PARTICLE_SYSTEM(BenchmarkCategory.GPU),
    GPU_TEXTURE_SAMPLING(BenchmarkCategory.GPU),
    GPU_TESSELLATION(BenchmarkCategory.GPU),

    // External GPU Benchmarks
    UNITY_SCENE_1(BenchmarkCategory.EXTERNAL_GPU),
    UNITY_SCENE_2(BenchmarkCategory.EXTERNAL_GPU),
    UNREAL_SCENE_1(BenchmarkCategory.EXTERNAL_GPU),
    UNREAL_SCENE_2(BenchmarkCategory.EXTERNAL_GPU),
    UNREAL_SCENE_3(BenchmarkCategory.EXTERNAL_GPU),

    // RAM Benchmarks
    RAM_SEQUENTIAL_RW(BenchmarkCategory.RAM),
    RAM_RANDOM_ACCESS(BenchmarkCategory.RAM),
    RAM_MEMORY_COPY(BenchmarkCategory.RAM),
    RAM_MULTI_THREADED(BenchmarkCategory.RAM),
    RAM_CACHE_HIERARCHY(BenchmarkCategory.RAM),

    // Storage Benchmarks
    STORAGE_SEQUENTIAL_READ(BenchmarkCategory.STORAGE),
    STORAGE_SEQUENTIAL_WRITE(BenchmarkCategory.STORAGE),
    STORAGE_RANDOM_RW(BenchmarkCategory.STORAGE),
    STORAGE_SMALL_FILE_OPS(BenchmarkCategory.STORAGE),
    STORAGE_DATABASE(BenchmarkCategory.STORAGE),
    STORAGE_MIXED_WORKLOAD(BenchmarkCategory.STORAGE),

    // Productivity Benchmarks
    PROD_UI_RENDERING(BenchmarkCategory.PRODUCTIVITY),
    PROD_RECYCLER_VIEW(BenchmarkCategory.PRODUCTIVITY),
    PROD_CANVAS_DRAWING(BenchmarkCategory.PRODUCTIVITY),
    PROD_IMAGE_FILTERS(BenchmarkCategory.PRODUCTIVITY),
    PROD_IMAGE_RESIZE(BenchmarkCategory.PRODUCTIVITY),
    PROD_VIDEO_ENCODING(BenchmarkCategory.PRODUCTIVITY),
    PROD_VIDEO_TRANSCODING(BenchmarkCategory.PRODUCTIVITY),
    PROD_PDF_RENDERING(BenchmarkCategory.PRODUCTIVITY),
    PROD_TEXT_RENDERING(BenchmarkCategory.PRODUCTIVITY),
    PROD_MULTI_TASKING(BenchmarkCategory.PRODUCTIVITY);

    /** Returns the display name for this benchmark. Used in UI and logging. */
    fun displayName(): String {
        return when (this) {
            PRIME_GENERATION -> "Prime Generation"
            FIBONACCI_ITERATIVE -> "Fibonacci Iterative"
            MATRIX_MULTIPLICATION -> "Matrix Multiplication"
            HASH_COMPUTING -> "Hash Computing"
            STRING_SORTING -> "String Sorting"
            RAY_TRACING -> "Ray Tracing"
            COMPRESSION -> "Compression"
            MONTE_CARLO -> "Monte Carlo π"
            JSON_PARSING -> "JSON Parsing"
            N_QUEENS -> "N-Queens"
            LLM_INFERENCE -> "LLM Inference (Gemma 3)"
            IMAGE_CLASSIFICATION -> "Image Classification (MobileNet V3)"
            OBJECT_DETECTION -> "Object Detection (EfficientDet)"
            TEXT_EMBEDDING -> "Text Embedding (MiniLM)"
            SPEECH_TO_TEXT -> "Speech-to-Text (Whisper)"

            
            // GPU Native
            GPU_TRIANGLE_RENDERING -> "Triangle Rendering Stress Test"
            GPU_COMPUTE_MATRIX -> "Compute Shader Matrix Multiplication"
            GPU_PARTICLE_SYSTEM -> "Particle System Simulation"
            GPU_TEXTURE_SAMPLING -> "Texture Sampling & Fillrate"
            GPU_TESSELLATION -> "Tessellation & Geometry Shader"

            // External GPU
            UNITY_SCENE_1 -> "Unity Benchmark Scene 1"
            UNITY_SCENE_2 -> "Unity Benchmark Scene 2"
            UNREAL_SCENE_1 -> "Unreal Benchmark Scene 1"
            UNREAL_SCENE_2 -> "Unreal Benchmark Scene 2"
            UNREAL_SCENE_3 -> "Unreal Benchmark Scene 3"

            // RAM
            RAM_SEQUENTIAL_RW -> "Sequential Check Read/Write Speed"
            RAM_RANDOM_ACCESS -> "Random Access Latency"
            RAM_MEMORY_COPY -> "Memory Copy Bandwidth"
            RAM_MULTI_THREADED -> "Multi-threaded Memory Bandwidth"
            RAM_CACHE_HIERARCHY -> "Examine Cache Hierarchy"

            // Storage
            STORAGE_SEQUENTIAL_READ -> "Sequential Read Speed"
            STORAGE_SEQUENTIAL_WRITE -> "Sequential Write Speed"
            STORAGE_RANDOM_RW -> "Random Read/Write (4K)"
            STORAGE_SMALL_FILE_OPS -> "Small File Operations"
            STORAGE_DATABASE -> "Database Performance (SQLite)"
            STORAGE_MIXED_WORKLOAD -> "Mixed Workload Test"

            // Productivity
            PROD_UI_RENDERING -> "UI Rendering Performance"
            PROD_RECYCLER_VIEW -> "RecyclerView Stress Test"
            PROD_CANVAS_DRAWING -> "Canvas Drawing Performance"
            PROD_IMAGE_FILTERS -> "Image Processing - Filters"
            PROD_IMAGE_RESIZE -> "Image Processing - Batch Resize"
            PROD_VIDEO_ENCODING -> "Video Encoding Test"
            PROD_VIDEO_TRANSCODING -> "Video Transcoding"
            PROD_PDF_RENDERING -> "PDF Rendering & Generation"
            PROD_TEXT_RENDERING -> "Text Rendering & Typography"
            PROD_MULTI_TASKING -> "Multi-tasking Simulation"
        }
    }

    /** Returns the single-core prefixed name. Valid primarily for CPU. */
    fun singleCore(): String = if (category == BenchmarkCategory.CPU) "Single-Core ${displayName()}" else displayName()

    /** Returns the multi-core prefixed name. Valid primarily for CPU. */
    fun multiCore(): String = if (category == BenchmarkCategory.CPU) "Multi-Core ${displayName()}" else displayName()

    companion object {
        /**
         * Parse a benchmark name from a string (e.g., from BenchmarkResult). Removes "Single-Core "
         * or "Multi-Core " prefix and matches to enum.
         */
        fun fromString(name: String): BenchmarkName? {
            val cleanName = name.replace("Single-Core ", "").replace("Multi-Core ", "").trim()

            // Special handling for variations
            val normalizedName =
                    when {
                        cleanName.startsWith("Monte Carlo") -> "Monte Carlo π"
                        cleanName.startsWith("LLM Inference") -> "LLM Inference (Gemma 3)"
                        cleanName.startsWith("Image Classification") -> "Image Classification (MobileNet V3)"
                        cleanName.startsWith("Object Detection") -> "Object Detection (EfficientDet)"
                        cleanName.startsWith("Text Embedding") -> "Text Embedding (MiniLM)"
                        cleanName.startsWith("Speech-to-Text") -> "Speech-to-Text (Whisper)"
                        
                        // GPU Partial Matches
                        cleanName.startsWith("Triangle Rendering") -> "Triangle Rendering Stress Test"
                        cleanName.startsWith("Compute Shader") -> "Compute Shader Matrix Multiplication"
                        cleanName.startsWith("Particle System") -> "Particle System Simulation"
                        cleanName.startsWith("Texture Sampling") -> "Texture Sampling & Fillrate"
                        cleanName.startsWith("Tessellation") -> "Tessellation & Geometry Shader"
                        
                        else -> cleanName
                    }

            return values().find { it.displayName() == normalizedName }
        }
        
        fun getByCategory(category: BenchmarkCategory): List<BenchmarkName> {
            return values().filter { it.category == category }
        }
    }
}
