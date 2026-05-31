package com.ivarna.finalbenchmark2.aiBenchmark

/**
 * AI benchmark result data class.
 *
 * @property modelName Human-readable benchmark name (e.g. "Conv Proxy 224×224")
 * @property inferenceTimeMs Average per-iteration time in milliseconds
 * @property computeFlops Raw FLOP/s throughput (not MFLOPS or GFLOPS — raw ops/sec).
 *           Computed as 2*N³ / avg_ms * 1000. Use [gflops] extension for GFLOPS.
 * @property accelerationMode Backend that ran: "GPU-Vulkan", "GPU-OpenCL", "GPU-OpenGLES", "CPU-NEON"
 * @property avgPowerWatts Average SOC power draw during benchmark in Watts (0.0 if not measured)
 * @property success True if benchmark completed with valid results
 * @property errorMessage Error description if [success] is false
 */
data class AiBenchmarkResult(
    val modelName: String,
    val inferenceTimeMs: Double = 0.0,
    val computeFlops: Double = 0.0,
    val accelerationMode: String = "Unknown",
    val avgPowerWatts: Double = 0.0,
    val success: Boolean,
    val errorMessage: String? = null
) {
    /** Convenience: throughput in GFLOPS */
    val gflops: Double get() = computeFlops / 1e9

    /** @deprecated Use [computeFlops] instead. Kept for JSON serialisation compatibility. */
    @Deprecated("Use computeFlops instead", ReplaceWith("computeFlops"))
    val throughput: Double get() = computeFlops
}
