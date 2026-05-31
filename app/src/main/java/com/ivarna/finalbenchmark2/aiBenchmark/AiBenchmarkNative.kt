package com.ivarna.finalbenchmark2.aiBenchmark

import android.content.Context
import android.util.Log
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

/**
 * AI benchmark runner using native compute kernels (no TFLite/LiteRT/MediaPipe).
 *
 * Backend fallback (hard requirement): Vulkan → OpenCL → OpenGL ES 3.1 → NEON CPU
 * All benchmarks are matmul variants at different sizes simulating AI workloads:
 *   0=ImageClass(224), 1=ObjDet(320), 2=TextEmbed(384), 3=ASR(1024),
 *   4=LLM(512), 5=YOLO(640), 6=MobileBERT(768), 7=DTLN(512)
 *
 * THREADING: All JNI calls run on a single dedicated OS thread via [benchmarkDispatcher].
 * EGL/OpenGL ES contexts are thread-affine — eglMakeCurrent binds a context to a specific
 * OS thread. Dispatchers.IO uses a thread pool, so consecutive coroutine calls may land
 * on different threads, causing eglMakeCurrent to fail and GPU to silently fall back to CPU.
 * Pinning to one thread prevents this.
 */
object AiBenchmarkNative {

    private val TAG = "[FinalBenchmark]"
    private var initialized = false
    private var appContext: Context? = null

    // Single-thread dispatcher: all EGL/GL/OCL JNI calls stay on same OS thread.
    // This prevents eglMakeCurrent failures from thread-affinity violations.
    private val benchmarkExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "ai-benchmark-thread").also { it.isDaemon = true }
    }
    private val benchmarkDispatcher = benchmarkExecutor.asCoroutineDispatcher()

    init {
        System.loadLibrary("vulkan_native")
    }

    // JNI declarations
    private external fun nativeInit(): Boolean
    private external fun nativeDestroy()
    private external fun nativeRunBenchmark(benchmarkId: Int, iterations: Int, warmupIterations: Int): DoubleArray
    private external fun nativeGetMode(): String

    fun init() {
        if (!initialized) {
            initialized = nativeInit()
            Log.d(TAG, "AI Native init: ${if (initialized) "OK" else "CPU NEON only"}")
        }
    }

    /** Provide application context for power sampling via BatteryManager. */
    fun setContext(context: Context) {
        appContext = context.applicationContext
    }

    fun destroy() {
        if (initialized) {
            nativeDestroy()
            initialized = false
        }
    }

    fun getBackendMode(): String = nativeGetMode()

    suspend fun runBenchmark(
        benchmarkId: Int,
        iterations: Int,
        warmupIterations: Int = 2,
        name: String
    ): AiBenchmarkResult = withContext(benchmarkDispatcher) {
        // Runs on dedicated single thread — EGL context binding is stable across calls
        try {
            val result = nativeRunBenchmark(benchmarkId, iterations, warmupIterations)
            val timeMs = result[0]
            val tps = result[1]
            val success = result[2] > 0.5
            val mode = nativeGetMode()
            Log.d(TAG, "$name: ${"%.2f".format(timeMs)}ms, ${"%.2f".format(tps / 1_000_000.0)} MOPS/s, $mode")
            AiBenchmarkResult(name, timeMs, tps, mode, 0.0, success)
        } catch (e: Exception) {
            Log.e(TAG, "$name failed: ${e.message}")
            AiBenchmarkResult(name, 0.0, 0.0, "Error", 0.0, false, e.message)
        }
    }

    // Benchmark ID mapping (matches native switch)
    const val IMAGE_CLASSIFICATION = 0
    const val OBJECT_DETECTION = 1
    const val TEXT_EMBEDDING = 2
    const val SPEECH_TO_TEXT = 3
    const val LLM_INFERENCE = 4
    const val YOLO_DETECTION = 5
    const val MOBILE_BERT = 6
    const val DTLN = 7
}
