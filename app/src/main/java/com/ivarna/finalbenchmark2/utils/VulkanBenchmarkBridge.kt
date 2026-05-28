package com.ivarna.finalbenchmark2.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Phase 2: Kotlin bridge to the Vulkan compute/render benchmark JNI layer.
 *
 * init() must be called from a background thread before runScene().
 * Gracefully returns false/−1f if Vulkan is unavailable.
 */
object VulkanBenchmarkBridge {

    private const val TAG = "VulkanBridge"
    private var initialized = false
    private var available   = false

    /**
     * Initialize Vulkan: create instance, pick GPU, build compute pipeline.
     * @return true if Vulkan is available and pipeline created successfully.
     */
    suspend fun init(): Boolean = withContext(Dispatchers.IO) {
        if (initialized) return@withContext available
        initialized = true
        try {
            available = nativeInit()
            if (available) Log.i(TAG, "Vulkan init OK — GPU: ${nativeGetGpuName()}")
            else           Log.w(TAG, "Vulkan not available on this device")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "JNI link error: ${e.message}")
            available = false
        } catch (e: Exception) {
            Log.e(TAG, "Vulkan init exception: ${e.message}")
            available = false
        }
        available
    }

    /**
     * Run a Vulkan benchmark scene.
     * @param sceneId 0 = Julia fractal (1920×1080, 240×135 dispatches × 10 avg)
     *                1 = Mandelbrot (2× dispatch size)
     * @return FPS equivalent, or -1f if Vulkan unavailable.
     */
    suspend fun runScene(sceneId: Int): Float = withContext(Dispatchers.IO) {
        if (!available) return@withContext -1f
        try {
            val result = nativeRunScene(sceneId)
            if (result.isNaN() || result.isInfinite()) -1f else result
        } catch (e: Exception) {
            Log.e(TAG, "runScene($sceneId) exception: ${e.message}")
            -1f
        }
    }

    /** @return GPU device name from Vulkan VkPhysicalDeviceProperties. */
    fun getGpuName(): String = try {
        if (available) nativeGetGpuName() else "Vulkan N/A"
    } catch (e: Exception) { "Vulkan N/A" }

    /** Release all Vulkan resources. */
    fun destroy() {
        if (!available) return
        try { nativeDestroy() } catch (e: Exception) { /* ignore */ }
        available = false; initialized = false
    }

    // ─── JNI declarations ──────────────────────────────────────────────────
    private external fun nativeInit(): Boolean
    private external fun nativeRunScene(sceneId: Int): Float
    private external fun nativeGetGpuName(): String
    private external fun nativeDestroy()

    init {
        try {
            System.loadLibrary("vulkan_native")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load vulkan_native: ${e.message}")
        }
    }
}
