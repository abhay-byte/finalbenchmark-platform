package com.ivarna.finalbenchmark2.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Phase 3: Kotlin bridge to the OpenCL benchmark JNI layer.
 *
 * Uses dlopen("libOpenCL.so") at runtime — no link-time dependency.
 * Gracefully unavailable on devices without OpenCL runtime.
 *
 * Scene 0: Memory bandwidth — 64 MB device-to-device copy × 20 iters → GB/s
 * Scene 1: Julia fractal compute — 1920×1080 × 128 iter, 5-frame avg → FPS
 */
object OpenCLBenchmarkBridge {

    private const val TAG = "OpenCLBridge"
    private var initialized = false
    private var available   = false

    /** Initialize OpenCL runtime via dlopen. */
    suspend fun init(): Boolean = withContext(Dispatchers.IO) {
        if (initialized) return@withContext available
        initialized = true
        try {
            available = nativeInit()
            if (available) Log.i(TAG, "OpenCL init OK")
            else           Log.w(TAG, "OpenCL not available (libOpenCL.so absent or no GPU device)")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "JNI link error: ${e.message}"); available = false
        } catch (e: Exception) {
            Log.e(TAG, "OpenCL init exception: ${e.message}"); available = false
        }
        available
    }

    /**
     * Run OpenCL benchmark scene.
     * @return Score (GB/s for scene 0, FPS for scene 1), or -1f if unavailable.
     */
    suspend fun runScene(sceneId: Int): Float = withContext(Dispatchers.IO) {
        if (!available) return@withContext -1f
        try {
            val result = nativeRunScene(sceneId)
            if (result.isNaN() || result.isInfinite()) -1f else result
        } catch (e: Exception) { Log.e(TAG, "runScene($sceneId): ${e.message}"); -1f }
    }

    fun destroy() {
        if (!available) return
        try { nativeDestroy() } catch (e: Exception) { /* ignore */ }
        available = false; initialized = false
    }

    private external fun nativeInit(): Boolean
    private external fun nativeRunScene(sceneId: Int): Float
    private external fun nativeDestroy()

    init {
        try { System.loadLibrary("vulkan_native") }
        catch (e: UnsatisfiedLinkError) { Log.e(TAG, "load vulkan_native: ${e.message}") }
    }
}
