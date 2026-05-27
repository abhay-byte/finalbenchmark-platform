package com.ivarna.finalbenchmark2.utils

/**
 * JNI bridge for storage (UFS) I/O benchmarks.
 *
 * The native functions in storage_benchmark.c use:
 *   • posix_fadvise(POSIX_FADV_DONTNEED) — evict page-cache before reads
 *   • fdatasync()                         — flush dirty pages to UFS for writes
 *
 * Both ensure measurements reflect real UFS 4.0 throughput rather than
 * Linux page-cache (RAM) speed.
 *
 * The library is the same shared object used by RamNativeBridge ("vulkan_native").
 */
object StorageNativeBridge {

    private var loaded = false

    /** Load the native library; idempotent — safe to call multiple times. */
    fun load(): Boolean {
        if (loaded) return true
        return try {
            System.loadLibrary("vulkan_native")
            loaded = true
            true
        } catch (e: UnsatisfiedLinkError) {
            loaded = false
            false
        }
    }

    val isAvailable: Boolean get() = loaded

    /**
     * Measure sequential read speed by repeatedly reading a pre-created file
     * after evicting its pages from the Linux page cache via posix_fadvise.
     *
     * @param path          Absolute path for the temporary test file (in cacheDir)
     * @param fileSizeBytes Test file size in bytes (e.g. 64 * 1024 * 1024L)
     * @param chunkSize     Read buffer size in bytes (e.g. 1 * 1024 * 1024)
     * @param durationMs    Measurement window in milliseconds
     * @return Throughput in MB/s
     */
    external fun nativeStorageSeqRead(
        path: String,
        fileSizeBytes: Long,
        chunkSize: Int,
        durationMs: Long
    ): Double

    /**
     * Measure sequential write speed by repeatedly writing a full file and
     * calling fdatasync() to flush dirty pages to UFS before the next pass.
     *
     * @param path          Absolute path for the temporary test file (in cacheDir)
     * @param fileSizeBytes Test file size in bytes (e.g. 64 * 1024 * 1024L)
     * @param chunkSize     Write buffer size in bytes (e.g. 1 * 1024 * 1024)
     * @param durationMs    Measurement window in milliseconds
     * @return Throughput in MB/s
     */
    external fun nativeStorageSeqWrite(
        path: String,
        fileSizeBytes: Long,
        chunkSize: Int,
        durationMs: Long
    ): Double

    /** Evict page cache for a file path. */
    external fun nativeEvictCache(path: String): Boolean
}
