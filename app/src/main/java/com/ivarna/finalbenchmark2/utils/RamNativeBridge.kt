package com.ivarna.finalbenchmark2.utils

/**
 * JNI bridge to the native RAM benchmark functions in ram_benchmark.c.
 *
 * All functions are thread-safe.  Call them from Dispatchers.Default
 * (or any background thread) — they block for exactly durationMs ms.
 *
 * Functions return 0.0 on allocation failure, which the ViewModel treats
 * gracefully (score = 0).
 */
object RamNativeBridge {

    private var loaded = false

    fun load(): Boolean {
        if (loaded) return true
        return try {
            System.loadLibrary("vulkan_native")   // same .so as CPU/Vulkan tests
            loaded = true
            true
        } catch (e: UnsatisfiedLinkError) {
            loaded = false
            false
        }
    }

    val isAvailable: Boolean get() = loaded

    /**
     * Sequential read benchmark.
     * Allocates 64 MB on native heap, faults all pages, then reads 64 B/iter
     * using ARM NEON vld1q_u64 pairs with __builtin_prefetch().
     * @return MB/s
     */
    @JvmStatic external fun nativeSeqRead(durationMs: Long): Double

    /**
     * Sequential write benchmark.
     * Allocates 64 MB, writes 64 B/iter using ARM NEON vst1q_u64 pairs.
     * Pattern is varied each outer iteration to prevent store-elision.
     * @return MB/s
     */
    @JvmStatic external fun nativeSeqWrite(durationMs: Long): Double

    /**
     * Random-access latency via pointer-chase.
     * 16 MB int array (exceeds L2, hits L3/DRAM), Knuth-shuffled.
     * Each "operation" is one random array dereference.
     * @return ns/op
     */
    @JvmStatic external fun nativeRandAccess(durationMs: Long): Double

    /**
     * Memory-copy throughput (Bionic libc memcpy — hand-written NEON on arm64).
     * @return MB/s
     */
    @JvmStatic external fun nativeMemCopy(durationMs: Long): Double

    /**
     * Multi-threaded read bandwidth using pthreads.
     * Each thread gets a private 16 MB buffer; NEON reads run in parallel.
     * @param numThreads number of reader threads (clamped 1..8)
     * @return aggregate MB/s across all threads
     */
    @JvmStatic external fun nativeMultiThread(numThreads: Int, durationMs: Long): Double
}
