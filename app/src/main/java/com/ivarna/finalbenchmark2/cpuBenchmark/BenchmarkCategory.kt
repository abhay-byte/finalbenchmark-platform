package com.ivarna.finalbenchmark2.cpuBenchmark

enum class BenchmarkCategory {
    CPU,
    AI,
    GPU,
    RAM,
    STORAGE,
    PRODUCTIVITY,
    EXTERNAL_GPU,
    FULL          // Runs all supported categories sequentially and produces a combined score
}

/** Human-readable display label shown in the HomeScreen benchmark-type dropdown. */
fun BenchmarkCategory.displayLabel(): String = when (this) {
    BenchmarkCategory.CPU          -> "CPU"
    BenchmarkCategory.AI           -> "AI / ML"
    BenchmarkCategory.GPU          -> "GPU"
    BenchmarkCategory.RAM          -> "RAM"
    BenchmarkCategory.STORAGE      -> "Storage"
    BenchmarkCategory.PRODUCTIVITY -> "Productivity"
    BenchmarkCategory.EXTERNAL_GPU -> "GPU (External)"
    BenchmarkCategory.FULL         -> "Final Benchmark"
}
