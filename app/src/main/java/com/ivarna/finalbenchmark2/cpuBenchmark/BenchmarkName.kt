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
    SPEECH_TO_TEXT(BenchmarkCategory.AI);

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
            LLM_INFERENCE -> "LLM Inference (llama.cpp)"
            IMAGE_CLASSIFICATION -> "Image Classification (ONNX)"
            OBJECT_DETECTION -> "Object Detection (YOLO)"
            TEXT_EMBEDDING -> "Text Embedding (Transformer)"
            SPEECH_TO_TEXT -> "Speech-to-Text (Whisper)"
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
                        cleanName.startsWith("LLM Inference") -> "LLM Inference (llama.cpp)"
                        cleanName.startsWith("Image Classification") -> "Image Classification (ONNX)"
                        cleanName.startsWith("Object Detection") -> "Object Detection (YOLO)"
                        cleanName.startsWith("Text Embedding") -> "Text Embedding (Transformer)"
                        cleanName.startsWith("Speech-to-Text") -> "Speech-to-Text (Whisper)"
                        else -> cleanName
                    }

            return values().find { it.displayName() == normalizedName }
        }
        
        fun getByCategory(category: BenchmarkCategory): List<BenchmarkName> {
            return values().filter { it.category == category }
        }
    }
}
