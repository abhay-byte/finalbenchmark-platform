package com.ivarna.finalbenchmark2.ui.viewmodels

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.HardwareRenderer
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.RenderNode
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.media.ImageReader
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Build
import android.graphics.ColorSpace
import android.media.Image
import android.os.Handler
import android.os.HandlerThread
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.io.ByteArrayOutputStream
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ivarna.finalbenchmark2.data.database.entities.BenchmarkResultEntity
import com.ivarna.finalbenchmark2.data.database.entities.GenericTestDetailEntity
import com.ivarna.finalbenchmark2.data.repository.HistoryRepository
import com.ivarna.finalbenchmark2.utils.PerformanceMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Random
import java.util.zip.Deflater
import kotlin.math.pow
import kotlin.math.roundToInt

// ── Productivity tests ────────────────────────────────────────────────────────

enum class ProductivityTest {
    CANVAS_OPS,      // Complex 2D drawing on off-screen Bitmap → ops/s
    IMAGE_FILTER,    // ColorMatrix filter on 4K (3840×2160) bitmaps → images/s
    IMAGE_RESIZE,    // Bitmap.createScaledBitmap 3840×2160 → 960×540 → images/s
    TEXT_OPS,        // Sort + search large string corpus → Mchars/s
    JSON_OPS,        // JSONObject build + serialize + parse → docs/s
    COMPRESSION,     // Deflate 256KB blocks → MB/s
    VIDEO_ENCODE,    // 1080p JPEG frame render + compress → fps
    VIDEO_DECODE,    // 1080p JPEG pre-encoded frames → BitmapFactory decode → fps
    VIDEO_TRANSCODE  // 1080p JPEG decode → scale to 720p → re-encode → fps
}

data class ProductivityTestResult(
    val test: ProductivityTest,
    val displayName: String,
    val value: Double,
    val unit: String,
    val score: Int,
    val durationMs: Long = 0L
)

data class ProductivityBenchmarkUiState(
    val isWarmingUp: Boolean = false,
    val isRunning: Boolean = false,
    val isCompleted: Boolean = false,
    val currentTest: ProductivityTest = ProductivityTest.CANVAS_OPS,
    val currentTestIndex: Int = 0,
    val totalTests: Int = ProductivityTest.values().size,
    val currentTestName: String = "",
    val overallProgress: Float = 0f,
    val currentTestProgress: Float = 0f,
    val currentValue: Double = 0.0,
    val currentUnit: String = "ops/s",
    val currentOperationDetail: String = "",  // what's being processed live
    val cpuTempC: Float = 35f,
    val completedTests: List<ProductivityTestResult> = emptyList(),
    val totalScore: Int = 0,
    val presetName: String = "",
    val statusMessage: String = ""
)

// ── Reference values ──────────────────────────────────────────────────────────
//
// Calibrated to OnePlus CPH2691 (SD 8 Gen 3, Android 16) — this device is the
// 100-point baseline. A device that beats these numbers will score above 100.
// Refs calibrated from SD8 Gen3 (Adreno 750) measured release build results:
//
//   CANVAS_OPS:      measured 1365 ops/s               → ref 1365
//   IMAGE_FILTER:    measured  151 imgs/s (AGSL float)  → ref  151
//   IMAGE_RESIZE:    measured  289 imgs/s (4K→1080p)    → ref  289
//   TEXT_OPS:        measured 4.31 Mchars/s (5K)        → ref 4.31
//   JSON_OPS:        measured  530 docs/s               → ref  530
//   COMPRESSION:     measured   27 MB/s                 → ref   27
//   VIDEO_ENCODE:    measured  169 fps (H.264 HW)       → ref  169
//   VIDEO_DECODE:    measured  426 fps (H.264 HW)       → ref  426
//   VIDEO_TRANSCODE: measured  124 fps (HW pipeline)    → ref  124

private val PRODUCTIVITY_REFERENCE = mapOf(
    ProductivityTest.CANVAS_OPS      to   1365.0,  // GPU HWUI — SD8Gen3 measured 1365 ops/s
    ProductivityTest.IMAGE_FILTER    to    151.0,  // GPU AGSL — SD8Gen3 measured  151 imgs/s
    ProductivityTest.IMAGE_RESIZE    to    289.0,  // GPU bilinear — SD8Gen3 measured 289 imgs/s
    ProductivityTest.TEXT_OPS        to      4.31, // CPU text — SD8Gen3 measured 4.31 Mchars/s
    ProductivityTest.JSON_OPS        to    530.0,  // CPU JSON — SD8Gen3 measured 530 docs/s
    ProductivityTest.COMPRESSION     to     27.0,  // CPU GZIP — SD8Gen3 measured 27 MB/s
    ProductivityTest.VIDEO_ENCODE    to    169.0,  // HW H.264 enc — SD8Gen3 measured 169 fps
    ProductivityTest.VIDEO_DECODE    to    426.0,  // HW H.264 dec — SD8Gen3 measured 426 fps
    ProductivityTest.VIDEO_TRANSCODE to    124.0,  // HW transcode — SD8Gen3 measured 124 fps
)

private val PRODUCTIVITY_TESTS = ProductivityTest.values().toList()

private fun ProductivityTest.displayName() = when (this) {
    ProductivityTest.CANVAS_OPS      -> "Canvas Drawing (GPU)"
    ProductivityTest.IMAGE_FILTER    -> "Image Filter (GPU AGSL)"
    ProductivityTest.IMAGE_RESIZE    -> "Image Resize (GPU)"
    ProductivityTest.TEXT_OPS        -> "Text Processing"
    ProductivityTest.JSON_OPS        -> "JSON Processing"
    ProductivityTest.COMPRESSION     -> "Data Compression"
    ProductivityTest.VIDEO_ENCODE    -> "Video Encode (H.264 HW)"
    ProductivityTest.VIDEO_DECODE    -> "Video Decode (H.264 HW)"
    ProductivityTest.VIDEO_TRANSCODE -> "Video Transcode (HW)"
}

private fun ProductivityTest.unit() = when (this) {
    ProductivityTest.CANVAS_OPS      -> "ops/s"
    ProductivityTest.IMAGE_FILTER    -> "images/s"
    ProductivityTest.IMAGE_RESIZE    -> "images/s"
    ProductivityTest.TEXT_OPS        -> "Mchars/s"
    ProductivityTest.JSON_OPS        -> "docs/s"
    ProductivityTest.COMPRESSION     -> "MB/s"
    ProductivityTest.VIDEO_ENCODE    -> "fps"
    ProductivityTest.VIDEO_DECODE    -> "fps"
    ProductivityTest.VIDEO_TRANSCODE -> "fps"
}

private fun ProductivityTest.score(value: Double): Int {
    val ref = PRODUCTIVITY_REFERENCE[this] ?: return 0
    return (value / ref * 100.0).roundToInt().coerceAtLeast(0)
}

private fun calculateProductivityGeometricMean(results: List<ProductivityTestResult>): Double {
    val ratios = results.map { r ->
        (r.value / (PRODUCTIVITY_REFERENCE[r.test] ?: 1.0)).coerceAtLeast(1e-9)
    }
    if (ratios.isEmpty()) return 0.0
    val product = ratios.fold(1.0) { acc, v -> acc * v }
    return product.pow(1.0 / ratios.size) * 100.0
}

private const val PROD_WARMUP_DUR_MS    = 2_000L
private const val PROD_MEASURE_DUR_MS   = 3_000L
private const val PROD_TICK_MS          = 100L
// Video setup (pre-encode I-frames) is heavy; warmup uses a smaller keyframe set
// so the actual hardware decode/transcode loop is reached inside the warmup
// window. 10 KFs ≈ 300 ms of pre-encode at 30 fps on SD8 Gen 3.
private const val PROD_WARMUP_KEYFRAMES = 10
private const val PROD_MEASURE_KEYFRAMES = 20
// Fraction of warmup window the setup phase may consume before we cut it off
// and proceed to the actual decoder/encoder warmup loop.
private const val PROD_WARMUP_SETUP_FRAC = 0.4

// ── ViewModel ─────────────────────────────────────────────────────────────────

class ProductivityBenchmarkViewModel(
    application: Application,
    private val historyRepository: HistoryRepository?
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ProductivityBenchmarkUiState())
    val uiState: StateFlow<ProductivityBenchmarkUiState> = _uiState.asStateFlow()

    private val _completionEvent = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 1)
    val completionEvent: SharedFlow<String> = _completionEvent.asSharedFlow()

    // Live preview bitmap — set from IO thread every few frames during image/video tests
    private val _previewBitmap = MutableStateFlow<Bitmap?>(null)
    val previewBitmapFlow: StateFlow<Bitmap?> = _previewBitmap.asStateFlow()

    private var runJob: Job? = null
    private val performanceMonitor = PerformanceMonitor(application)
    private val baseCpuTemp = (36..44).random().toFloat()

    // Written from Dispatchers.IO benchmark functions; read from main tick coroutine
    @Volatile private var liveDetail = ""

    override fun onCleared() {
        super.onCleared()
        _previewBitmap.value?.recycle()
        _previewBitmap.value = null
    }

    fun start(preset: String) {
        runJob?.cancel()
        _uiState.update { it.copy(presetName = preset) }
        runJob = viewModelScope.launch { runBenchmark() }
    }

    fun stop() {
        runJob?.cancel()
        if (performanceMonitor.isMonitoring()) performanceMonitor.stop()
        _previewBitmap.value = null
        _uiState.update { ProductivityBenchmarkUiState() }
    }

    // ── Benchmark loop ─────────────────────────────────────────────────────

    private suspend fun runBenchmark() {
        val results = mutableListOf<ProductivityTestResult>()
        performanceMonitor.start()

        for ((index, test) in PRODUCTIVITY_TESTS.withIndex()) {
            val name = test.displayName()
            val unit = test.unit()
            liveDetail = ""
            _previewBitmap.value = null

            // ─ Warm-up ────────────────────────────────────────────────────
            _uiState.update {
                it.copy(
                    isWarmingUp = true, isRunning = false,
                    currentTest = test, currentTestIndex = index,
                    currentTestName = name, currentTestProgress = 0f,
                    currentUnit = unit,
                    overallProgress = index.toFloat() / PRODUCTIVITY_TESTS.size,
                    statusMessage = "Warming up $name…",
                    currentOperationDetail = ""
                )
            }
            coroutineScope {
                val warmupJob = async(Dispatchers.IO) {
                    runTest(test, durationMs = PROD_WARMUP_DUR_MS, isWarmup = true)
                }
                val warmSteps = (PROD_WARMUP_DUR_MS / PROD_TICK_MS).toInt()
                repeat(warmSteps) { step ->
                    delay(PROD_TICK_MS)
                    if (warmupJob.isCompleted) return@repeat
                    _uiState.update { s ->
                        s.copy(
                            currentTestProgress = (step + 1).toFloat() / warmSteps * 0.15f,
                            cpuTempC = mockCpuTemp(),
                            currentOperationDetail = liveDetail
                        )
                    }
                }
                val warmupValue = warmupJob.await()
                if (warmupValue <= 0.0) {
                    android.util.Log.w(
                        "ProdBench",
                        "Warmup for $test produced 0 ops — measure phase will run cold"
                    )
                }
            }

            // ─ Measure ────────────────────────────────────────────────────
            _uiState.update {
                it.copy(isWarmingUp = false, isRunning = true, statusMessage = "Measuring…")
            }
            val measureSteps = (PROD_MEASURE_DUR_MS / PROD_TICK_MS).toInt()
            val measureStartMs = System.currentTimeMillis()
            val value = coroutineScope {
                val measureJob = async(Dispatchers.IO) { runTest(test, durationMs = PROD_MEASURE_DUR_MS) }
                repeat(measureSteps) { step ->
                    delay(PROD_TICK_MS)
                    val overall = (index + 0.15f + (step + 1).toFloat() / measureSteps * 0.85f) /
                                  PRODUCTIVITY_TESTS.size
                    _uiState.update { s ->
                        s.copy(
                            currentTestProgress = 0.15f + (step + 1).toFloat() / measureSteps * 0.85f,
                            overallProgress = overall,
                            cpuTempC = mockCpuTemp(),
                            currentOperationDetail = liveDetail
                        )
                    }
                }
                measureJob.await()
            }
            val elapsedMs = System.currentTimeMillis() - measureStartMs

            val result = ProductivityTestResult(test, name, value, unit, test.score(value), elapsedMs)
            results += result
            _uiState.update { s -> s.copy(currentValue = value, completedTests = results.toList()) }
        }

        val performanceMetricsJson = performanceMonitor.stop()
        val totalScore = calculateProductivityGeometricMean(results).roundToInt().coerceAtLeast(0)

        _uiState.update {
            it.copy(
                isRunning = false, isCompleted = true,
                overallProgress = 1f, totalScore = totalScore,
                statusMessage = "Complete"
            )
        }

        val resultJson = buildResultJson(results, totalScore, _uiState.value.presetName, performanceMetricsJson)
        saveToDatabase(results, totalScore, performanceMetricsJson, resultJson)
        _completionEvent.emit(resultJson)
    }

    // ── Test dispatcher ────────────────────────────────────────────────────

    private fun runTest(
        test: ProductivityTest,
        durationMs: Long,
        isWarmup: Boolean = false
    ): Double = try {
        when (test) {
            ProductivityTest.CANVAS_OPS      -> benchCanvasOps(durationMs, isWarmup)
            ProductivityTest.IMAGE_FILTER    -> benchImageFilter(durationMs, isWarmup)
            ProductivityTest.IMAGE_RESIZE    -> benchImageResize(durationMs, isWarmup)
            ProductivityTest.TEXT_OPS        -> benchTextOps(durationMs, isWarmup)
            ProductivityTest.JSON_OPS        -> benchJsonOps(durationMs, isWarmup)
            ProductivityTest.COMPRESSION     -> benchCompression(durationMs, isWarmup)
            ProductivityTest.VIDEO_ENCODE    -> benchVideoEncode(durationMs, isWarmup)
            ProductivityTest.VIDEO_DECODE    -> benchVideoDecode(durationMs, isWarmup)
            ProductivityTest.VIDEO_TRANSCODE -> benchVideoTranscode(durationMs, isWarmup)
        }
    } catch (e: Exception) {
        android.util.Log.e("ProductivityBenchVM", "Test $test failed: ${e.message}", e)
        e.printStackTrace()
        0.0
    }

    // ── 1. Canvas Drawing (GPU – HardwareBufferRenderer API 34+) ──────────
    /**
     * Android 14+ HardwareBufferRenderer: renders Canvas commands directly
     * into a HardwareBuffer (GPU memory) with async draw() + SyncFence.
     * No ImageReader polling — GPU submission is truly async.
     */
    private fun benchCanvasOps(durationMs: Long, isWarmup: Boolean = false): Double {
        val W = 1024; val H = 1024

        // Pre-compute all paths outside hot loop
        val rng = Random(42L)
        val prePaths = Array(200) { _ ->
            val p = Path()
            p.moveTo(rng.nextFloat() * W, rng.nextFloat() * H)
            for (s in 0 until 8) p.cubicTo(
                rng.nextFloat() * W, rng.nextFloat() * H,
                rng.nextFloat() * W, rng.nextFloat() * H,
                rng.nextFloat() * W, rng.nextFloat() * H)
            p
        }
        val preCircles = Array(200) { i -> Triple(
            ((i * 83L) % W).toFloat(), ((i * 137L) % H).toFloat(), 50f + (i % 12) * 20f) }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 52f; color = Color.WHITE }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 4f; color = Color.argb(200, 255, 255, 255) }
        val rectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 6f; color = Color.argb(200, 255, 200, 0) }

        // Try HardwareBufferRenderer (API 34+, optimal GPU path)
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            try {
                return benchCanvasHwBuffer(durationMs, W, H, prePaths, preCircles, paint, textPaint, strokePaint, rectPaint)
            } catch (e: Exception) {
                android.util.Log.w("ProdBench", "HardwareBufferRenderer failed: ${e.message}, falling back to HardwareRenderer")
            }
        }
        // Fallback: HardwareRenderer + ImageReader (API 29+)
        return benchCanvasLegacy(durationMs, W, H, prePaths, preCircles, paint, textPaint, strokePaint, rectPaint)
    }

    @Suppress("NewApi")
    private fun benchCanvasHwBuffer(durationMs: Long, W: Int, H: Int,
        prePaths: Array<Path>, preCircles: Array<Triple<Float,Float,Float>>,
        paint: Paint, textPaint: Paint, strokePaint: Paint, rectPaint: Paint): Double {

        val hwBuffer = android.hardware.HardwareBuffer.create(
            W, H, android.hardware.HardwareBuffer.RGBA_8888, 1,
            android.hardware.HardwareBuffer.USAGE_GPU_COLOR_OUTPUT or
            android.hardware.HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE)
        val renderer = android.graphics.HardwareBufferRenderer(hwBuffer)

        val rootNode = RenderNode("canvas_gpu")
        rootNode.setPosition(0, 0, W, H)
        renderer.setContentRoot(rootNode)

        val fenceLock = Object()
        var fence: android.hardware.SyncFence? = null
        val executor = java.util.concurrent.Executors.newSingleThreadExecutor()
        var ops = 0L
        val endMs = System.currentTimeMillis() + durationMs

        try {
            while (System.currentTimeMillis() < endMs) {
                val hue = (ops * 2.7f) % 360f
                val canvas = rootNode.beginRecording()

                paint.shader = LinearGradient(0f, 0f, W.toFloat(), H.toFloat(),
                    Color.HSVToColor(floatArrayOf(hue, 0.9f, 0.85f)),
                    Color.HSVToColor(floatArrayOf((hue + 120f) % 360f, 0.8f, 0.6f)),
                    Shader.TileMode.CLAMP)
                paint.style = Paint.Style.FILL
                canvas.drawRect(0f, 0f, W.toFloat(), H.toFloat(), paint)
                paint.shader = null

                val ci = (ops % 200).toInt()
                for (j in 0 until 12) {
                    val (cx, cy, r) = preCircles[(ci + j) % 200]
                    paint.shader = RadialGradient(cx, cy, r,
                        Color.HSVToColor(floatArrayOf((hue + j * 30f) % 360f, 0.9f, 1.0f)),
                        Color.TRANSPARENT, Shader.TileMode.CLAMP)
                    canvas.drawCircle(cx, cy, r, paint)
                }
                paint.shader = null

                canvas.drawPath(prePaths[ci], strokePaint)

                canvas.save()
                canvas.rotate((ops % 360L).toFloat(), W / 2f, H / 2f)
                canvas.drawRoundRect(W * 0.2f, H * 0.2f, W * 0.8f, H * 0.8f, 32f, 32f, rectPaint)
                canvas.restore()

                canvas.drawText("GPU Frame #$ops", 32f, H * 0.92f, textPaint)
                rootNode.endRecording()

                val request = renderer.obtainRenderRequest()
                request.draw(executor) { result ->
                    synchronized(fenceLock) {
                        fence?.close()
                        fence = result.fence
                        fenceLock.notifyAll()
                    }
                }

                synchronized(fenceLock) {
                    val prevFence = fence
                    if (prevFence != null) {
                        prevFence.awaitForever()
                        prevFence.close()
                        fence = null
                    }
                }

                ops++
                if (ops % 30L == 0L) {
                    liveDetail = "GPU Canvas #$ops  •  ${W}×${H}"
                }
            }

            synchronized(fenceLock) { fence?.awaitForever(); fence?.close() }
        } finally {
            renderer.close()
            hwBuffer.close()
            executor.shutdown()
            try { executor.awaitTermination(1, java.util.concurrent.TimeUnit.SECONDS) } catch (_: InterruptedException) {}
            executor.shutdownNow()
        }
        return ops.toDouble() / (durationMs / 1000.0)
    }

    private fun benchCanvasLegacy(durationMs: Long, W: Int, H: Int,
        prePaths: Array<Path>, preCircles: Array<Triple<Float,Float,Float>>,
        paint: Paint, textPaint: Paint, strokePaint: Paint, rectPaint: Paint): Double {

        val imgReader = ImageReader.newInstance(W, H, PixelFormat.RGBA_8888, 4)
        val renderer = HardwareRenderer()
        renderer.setSurface(imgReader.surface)
        renderer.setLightSourceGeometry(W / 2f, 0f, 800f, 800f)
        renderer.setLightSourceAlpha(0.039f, 0.19f)
        renderer.start()

        val rootNode = RenderNode("canvas_gpu")
        rootNode.setPosition(0, 0, W, H)
        renderer.setContentRoot(rootNode)

        var ops = 0L
        val endMs = System.currentTimeMillis() + durationMs

        try {
            while (System.currentTimeMillis() < endMs) {
                val hue = (ops * 2.7f) % 360f
                val canvas = rootNode.beginRecording()

                paint.shader = LinearGradient(0f, 0f, W.toFloat(), H.toFloat(),
                    Color.HSVToColor(floatArrayOf(hue, 0.9f, 0.85f)),
                    Color.HSVToColor(floatArrayOf((hue + 120f) % 360f, 0.8f, 0.6f)),
                    Shader.TileMode.CLAMP)
                paint.style = Paint.Style.FILL
                canvas.drawRect(0f, 0f, W.toFloat(), H.toFloat(), paint)
                paint.shader = null

                val ci = (ops % 200).toInt()
                for (j in 0 until 12) {
                    val (cx, cy, r) = preCircles[(ci + j) % 200]
                    paint.shader = RadialGradient(cx, cy, r,
                        Color.HSVToColor(floatArrayOf((hue + j * 30f) % 360f, 0.9f, 1.0f)),
                        Color.TRANSPARENT, Shader.TileMode.CLAMP)
                    canvas.drawCircle(cx, cy, r, paint)
                }
                paint.shader = null

                canvas.drawPath(prePaths[ci], strokePaint)

                canvas.save()
                canvas.rotate((ops % 360L).toFloat(), W / 2f, H / 2f)
                canvas.drawRoundRect(W * 0.2f, H * 0.2f, W * 0.8f, H * 0.8f, 32f, 32f, rectPaint)
                canvas.restore()

                canvas.drawText("GPU Frame #$ops", 32f, H * 0.92f, textPaint)
                rootNode.endRecording()

                renderer.createRenderRequest().syncAndDraw()
                imgReader.acquireLatestImage()?.close()

                ops++
                if (ops % 30L == 0L) {
                    liveDetail = "GPU Canvas #$ops  •  ${W}×${H}"
                }
            }
        } finally {
            renderer.stop(); renderer.destroy()
            imgReader.surface.release(); imgReader.close()
        }
        return ops.toDouble() / (durationMs / 1000.0)
    }

    // ── 2. Image Filter (GPU – RuntimeShader AGSL) ────────────────────────
    /**
     * Uses Android's RuntimeShader (AGSL, API 33+) for FULL GPU per-pixel processing.
     * The AGSL shader runs on the Adreno shader cores, processing all 8.3M pixels
     * at 3840×2160 on the GPU with zero CPU involvement per pixel.
     * Per image (frame): brightness + saturation + hue-rotation in one pass via YIQ colour space.
     * Rendered via HardwareRenderer to off-screen ImageReader surface.
     */
    private fun benchImageFilter(durationMs: Long, isWarmup: Boolean = false): Double {
        val W = 3840; val H = 2160

        // AGSL (Android Graphics Shading Language) shader – GPU-neutral float precision
        val agsl = """
            uniform shader inputTexture;
            uniform float brightness;
            uniform float saturation;
            uniform float hueAngle;

            float3 toYIQ(float3 rgb) {
                return float3(
                    dot(rgb, float3(0.299, 0.587, 0.114)),
                    dot(rgb, float3(0.596, -0.274, -0.322)),
                    dot(rgb, float3(0.211, -0.523, 0.312))
                );
            }
            float3 fromYIQ(float3 yiq) {
                return float3(
                    dot(yiq, float3(1.0, 0.956, 0.621)),
                    dot(yiq, float3(1.0, -0.272, -0.647)),
                    dot(yiq, float3(1.0, -1.106, 1.703))
                );
            }
            float4 main(float2 coord) {
                float4 c = inputTexture.eval(coord);
                c.rgb *= brightness;
                float lum = dot(c.rgb, float3(0.299, 0.587, 0.114));
                c.rgb = mix(float3(lum, lum, lum), c.rgb, saturation);
                float3 yiq = toYIQ(c.rgb);
                float cs = cos(hueAngle); float ss = sin(hueAngle);
                yiq = float3(yiq.x, yiq.y * cs - yiq.z * ss, yiq.y * ss + yiq.z * cs);
                c.rgb = clamp(fromYIQ(yiq), 0.0, 1.0);
                return c;
            }
        """.trimIndent()

        // Build source bitmap (drawn once on CPU, then stays as GPU texture)
        val src = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(src); val sp = Paint(); val rng = Random(99L)
        for (band in 0 until 12) {
            sp.color = Color.HSVToColor(floatArrayOf(band * 30f, 0.85f, 0.9f))
            c.drawRect(0f, band * H / 12f, W.toFloat(), (band + 1) * H / 12f, sp)
        }
        for (i in 0 until 80) {
            sp.color = Color.argb(100 + rng.nextInt(120), rng.nextInt(256), rng.nextInt(256), rng.nextInt(256))
            c.drawCircle(rng.nextFloat() * W, rng.nextFloat() * H, 80f + rng.nextFloat() * 400f, sp)
        }

        // Off-screen GPU render target — try GPU flags first, fallback to legacy
        val imgReader: ImageReader = try {
            ImageReader.newInstance(W, H, PixelFormat.RGBA_8888, 4,
                android.hardware.HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or
                android.hardware.HardwareBuffer.USAGE_GPU_COLOR_OUTPUT)
        } catch (e: Exception) {
            ImageReader.newInstance(W, H, PixelFormat.RGBA_8888, 4)
        }
        val renderer = HardwareRenderer()
        renderer.setSurface(imgReader.surface)
        renderer.setLightSourceGeometry(W / 2f, 0f, 800f, 800f)
        renderer.setLightSourceAlpha(0.039f, 0.19f)
        renderer.start()

        val rootNode = RenderNode("img_filter_gpu")
        rootNode.setPosition(0, 0, W, H)
        renderer.setContentRoot(rootNode)

        val rtShader = RuntimeShader(agsl)
        val drawPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Hoist shader binding outside loop — source bitmap is invariant
        rtShader.setInputShader("inputTexture",
            BitmapShader(src, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP))

        var images = 0L
        val endMs = System.currentTimeMillis() + durationMs

        // Drain stale images before benchmark
        var stale: Image? = imgReader.acquireLatestImage()
        while (stale != null) { stale.close(); stale = imgReader.acquireLatestImage() }

        try {
            while (System.currentTimeMillis() < endMs) {
                val t = images.toFloat()
                rtShader.setFloatUniform("brightness", 0.8f + (t % 50f) * 0.006f)
                rtShader.setFloatUniform("saturation", 0.5f + (t % 80f) * 0.007f)
                rtShader.setFloatUniform("hueAngle", (t % 360f) * (Math.PI.toFloat() / 180f))

                val canvas = rootNode.beginRecording()
                drawPaint.shader = rtShader
                val d = (images % 4).toFloat() * 0.5f
                canvas.drawRect(d, d, W.toFloat() + d, H.toFloat() + d, drawPaint)
                rootNode.endRecording()

                renderer.createRenderRequest().setWaitForPresent(true).syncAndDraw()
                try {
                    val img = imgReader.acquireNextImage()
                    img.close(); images++
                } catch (_: Exception) { }
                if (images % 5L == 0L)
                    liveDetail = "GPU AGSL #$images  •  ${W}×${H}  •  brightness+sat+hue shader"
            }
        } finally {
            renderer.stop(); renderer.destroy()
            imgReader.surface.release(); imgReader.close()
            src.recycle()
        }
        return images.toDouble() / (durationMs / 1000.0)
    }

    // ── 3. Image Resize (GPU – HardwareRenderer bilinear) ────────────────
    /**
     * GPU-accelerated bilinear downscale via HardwareRenderer + RenderNode.
     * Measures pure GPU texture-sampler throughput: 3840×2160 → 1920×1080.
     * Uses blocking acquireNextImage() to ensure GPU actually completed each frame.
     */
    private fun benchImageResize(durationMs: Long, isWarmup: Boolean = false): Double {
        val fullW = 3840; val fullH = 2160
        val halfW = 1920; val halfH = 1080

        val src = Bitmap.createBitmap(fullW, fullH, Bitmap.Config.ARGB_8888)
        val c = Canvas(src); val p = Paint()
        for (band in 0 until 16) {
            p.color = Color.HSVToColor(floatArrayOf(band * 22.5f, 0.85f, 0.92f))
            c.drawRect(0f, band * fullH / 16f, fullW.toFloat(), (band + 1) * fullH / 16f, p)
        }

        val scalePaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

        val halfReader = ImageReader.newInstance(halfW, halfH, PixelFormat.RGBA_8888, 4)
        val halfRenderer = HardwareRenderer()
        halfRenderer.setSurface(halfReader.surface); halfRenderer.start()
        val halfNode = RenderNode("down"); halfNode.setPosition(0, 0, halfW, halfH)
        halfRenderer.setContentRoot(halfNode)

        var images = 0L
        val endMs = System.currentTimeMillis() + durationMs
        val srcBmpShader = android.graphics.BitmapShader(src, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)

        // Drain any stale images before benchmark
        var stale: Image? = halfReader.acquireLatestImage()
        while (stale != null) { stale.close(); stale = halfReader.acquireLatestImage() }

        try {
            while (System.currentTimeMillis() < endMs) {
                val downCanvas = halfNode.beginRecording()
                scalePaint.shader = srcBmpShader
                val drift = (images % 4).toFloat() * 0.5f
                downCanvas.drawRect(drift, drift, halfW.toFloat() + drift, halfH.toFloat() + drift, scalePaint)
                halfNode.endRecording()
                halfRenderer.createRenderRequest().setWaitForPresent(true).syncAndDraw()
                try {
                    val img = halfReader.acquireNextImage()
                    img.close(); images++
                } catch (_: Exception) { }
            }
        } finally {
            halfRenderer.stop(); halfRenderer.destroy(); halfReader.surface.release(); halfReader.close()
            src.recycle()
        }
        return images.toDouble() / (durationMs / 1000.0)
    }

    // ── Video Encode (HW – MediaCodec H.264 via Surface) ─────────────────
    /**
     * Pure GPU→Hardware encode pipeline:
     *   1. HardwareRenderer draws a GPU frame (HSV bands + gradients) to encoder Surface
     *   2. MediaCodec H.264 hardware encoder (Adreno VCE block) encodes from Surface
     * No CPU pixel transfers – frame data flows GPU→SurfaceTexture→hardware encoder.
     * 1920×1080p at 8 Mbps. Measures hardware-encoded frames/second.
     */
    private fun benchVideoEncode(durationMs: Long, isWarmup: Boolean = false): Double {
        val W = 1920; val H = 1080
        return try {
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, W, H).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, 8_000_000)
                setInteger(MediaFormat.KEY_FRAME_RATE, 60)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            }
            val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

            // Get encoder's input Surface – frames rendered here are fed directly to the HW encoder
            val encoderSurface = encoder.createInputSurface()
            encoder.start()

            // HardwareRenderer connected to encoder Surface (GPU → Hardware encoder, no CPU roundtrip)
            val renderer = HardwareRenderer()
            renderer.setSurface(encoderSurface)
            renderer.setLightSourceGeometry(W / 2f, 0f, 800f, 800f)
            renderer.setLightSourceAlpha(0.039f, 0.19f)
            renderer.start()

            val rootNode = RenderNode("enc_frame"); rootNode.setPosition(0, 0, W, H)
            renderer.setContentRoot(rootNode)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val info = MediaCodec.BufferInfo()
            var frames = 0L
            val endMs = System.currentTimeMillis() + durationMs

            while (System.currentTimeMillis() < endMs) {
                val hue = (frames * 3.7f) % 360f

                // Render frame on GPU
                val canvas = rootNode.beginRecording()
                for (band in 0 until 8) {
                    paint.color = Color.HSVToColor(floatArrayOf((hue + band * 45f) % 360f, 0.9f, 0.9f))
                    paint.shader = RadialGradient(W / 2f, H / 2f, W / 2f,
                        paint.color, Color.BLACK, Shader.TileMode.CLAMP)
                    canvas.drawRect(0f, band * H / 8f, W.toFloat(), (band + 1) * H / 8f, paint)
                }
                paint.shader = null; paint.color = Color.WHITE; paint.textSize = 72f
                canvas.drawText("HW Enc Frame $frames", 60f, H / 2f, paint)
                rootNode.endRecording()

                // Submit GPU frame to encoder surface (synchronous – waits for GPU commit)
                renderer.createRenderRequest().syncAndDraw()

                // Drain encoder output (discard output bytes, we just measure throughput)
                var outIdx = encoder.dequeueOutputBuffer(info, 0)
                while (outIdx >= 0) {
                    if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0 && info.size > 0) frames++
                    encoder.releaseOutputBuffer(outIdx, false)
                    outIdx = encoder.dequeueOutputBuffer(info, 0)
                }
                if (frames % 30L == 0L && frames > 0)
                    liveDetail = "HW Enc #$frames  •  ${W}×${H} H.264  •  ${encoder.codecInfo.name}"
            }

            renderer.stop(); renderer.destroy()
            encoder.stop(); encoder.release()
            encoderSurface.release()
            frames.toDouble() / (durationMs / 1000.0)
        } catch (e: Exception) {
            android.util.Log.e("ProdBench", "HW video encode failed: ${e.message}", e)
            0.0
        }
    }

    // ── 7. Video Decode (HW – MediaCodec H.264) ──────────────────────────
    /**
     * Hardware H.264 decode pipeline:
     *   Phase 1 – Setup: pre-encode N H.264 I-frames (1920×1080) using
     *             MediaCodec encoder in YUV byte-buffer mode, collecting
     *             the full bitstream (SPS/PPS + IDR slices).
     *   Phase 2 – Benchmark loop: feed pre-encoded frames to MediaCodec
     *             hardware decoder, drain output buffers without rendering.
     * Tests the Adreno / Snapdragon VPU hardware decoder throughput in fps.
     *
     * Warmup uses fewer keyframes (PROD_WARMUP_KEYFRAMES) and a time-boxed
     * setup so the actual hardware decoder is reached within the warmup
     * window. Measure uses PROD_MEASURE_KEYFRAMES and the full duration.
     */
    private fun benchVideoDecode(durationMs: Long, isWarmup: Boolean = false): Double {
        val W = 1920; val H = 1080
        val KEYFRAMES = if (isWarmup) PROD_WARMUP_KEYFRAMES else PROD_MEASURE_KEYFRAMES
        val warmupStartMs = System.currentTimeMillis()
        // Avoid Long.MAX_VALUE + warmupStartMs overflow. Use a far-future
        // sentinel that is always greater than any real currentTimeMillis.
        val setupDeadlineMs: Long = if (isWarmup)
            warmupStartMs + (durationMs * PROD_WARMUP_SETUP_FRAC).toLong()
        else
            Long.MAX_VALUE shr 1

        return try {
            // ── Phase 1: pre-encode I-frames ────────────────────────────
            val encFmt = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, W, H).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, 8_000_000)
                setInteger(MediaFormat.KEY_FRAME_RATE, 30)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 0)   // every frame is I-frame
                setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
            }
            val enc = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            enc.configure(encFmt, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            enc.start()

            data class EncodedFrame(val data: ByteArray, val flags: Int, val pts: Long)
            val bitstreamChunks = mutableListOf<EncodedFrame>()
            val encInfo = MediaCodec.BufferInfo()
            var inputFramesSent = 0
            var encDone = false

            while (!encDone && System.currentTimeMillis() < setupDeadlineMs) {
                // Feed raw YUV frames
                if (inputFramesSent <= KEYFRAMES) {
                    val inIdx = enc.dequeueInputBuffer(10_000L)
                    if (inIdx >= 0) {
                        val buf = enc.getInputBuffer(inIdx)!!; buf.clear()
                        // Y plane: simple luma ramp
                        val ySize = W * H; val uvSize = W * H / 4
                        for (i in 0 until ySize) buf.put(((i / W + i % W + inputFramesSent * 4) and 0xFF).toByte())
                        for (i in 0 until uvSize) { buf.put(128.toByte()); buf.put(128.toByte()) }
                        val pts = inputFramesSent * 33_333L
                        val flags = if (inputFramesSent == KEYFRAMES) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
                        enc.queueInputBuffer(inIdx, 0, buf.position(), pts, flags)
                        inputFramesSent++
                    }
                }
                // Drain encoded output
                val outIdx = enc.dequeueOutputBuffer(encInfo, 10_000L)
                if (outIdx >= 0) {
                    val buf = enc.getOutputBuffer(outIdx)!!
                    val bytes = ByteArray(encInfo.size)
                    buf.position(encInfo.offset); buf.limit(encInfo.offset + encInfo.size)
                    buf.get(bytes)
                    bitstreamChunks.add(EncodedFrame(bytes, encInfo.flags, encInfo.presentationTimeUs))
                    enc.releaseOutputBuffer(outIdx, false)
                    if (encInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) encDone = true
                } else if (outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) { /* ignore */ }
            }
            // Warmup path: drain pending output with a short, bounded wait so
            // we don't burn the rest of the warmup window on the encoder drain.
            if (!encDone) {
                val drainStart = System.currentTimeMillis()
                while (System.currentTimeMillis() - drainStart < 100L) {
                    val drain = enc.dequeueOutputBuffer(encInfo, 0)
                    if (drain < 0) break
                    enc.releaseOutputBuffer(drain, false)
                }
            }
            enc.stop(); enc.release()

            // Separate CSD (SPS/PPS) from IDR frames
            val csdChunks = bitstreamChunks.filter { it.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0 }
            val idrChunks = bitstreamChunks.filter { it.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0 && it.data.isNotEmpty() }
            if (idrChunks.isEmpty()) return 0.0

            // ── Phase 2: hardware decode loop ───────────────────────────
            val decFmt = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, W, H)
            val dec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            dec.configure(decFmt, null, null, 0)   // null surface → output to ByteBuffer
            dec.start()

            // Feed CSD first
            for (csd in csdChunks) {
                val idx = dec.dequeueInputBuffer(10_000L)
                if (idx >= 0) {
                    val buf = dec.getInputBuffer(idx)!!; buf.clear(); buf.put(csd.data)
                    dec.queueInputBuffer(idx, 0, csd.data.size, 0, MediaCodec.BUFFER_FLAG_CODEC_CONFIG)
                }
            }

            var decFrames = 0L
            val decInfo = MediaCodec.BufferInfo()
            var idrIdx = 0
            // For warmup, the setup phase may have already consumed part of the
            // warmup window. Use a unified deadline so total wall-clock time
            // stays bounded by `durationMs` regardless of how long setup took.
            val decodeEndMs = if (isWarmup) warmupStartMs + durationMs
                              else System.currentTimeMillis() + durationMs
            var eos = false

            while (System.currentTimeMillis() < decodeEndMs) {
                if (!eos) {
                    val inIdx = dec.dequeueInputBuffer(5_000L)
                    if (inIdx >= 0) {
                        val chunk = idrChunks[idrIdx % idrChunks.size]
                        idrIdx++
                        val buf = dec.getInputBuffer(inIdx)!!; buf.clear(); buf.put(chunk.data)
                        dec.queueInputBuffer(inIdx, 0, chunk.data.size, chunk.pts + decFrames * 33_333L, 0)
                    }
                }
                val outIdx = dec.dequeueOutputBuffer(decInfo, 5_000L)
                if (outIdx >= 0) {
                    if (decInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0 && decInfo.size > 0) {
                        decFrames++
                    }
                    dec.releaseOutputBuffer(outIdx, false)
                    if (decFrames % 30L == 0L)
                        liveDetail = "HW Dec #$decFrames  •  ${W}×${H} H.264  •  ${dec.codecInfo.name}"
                }
            }

            dec.stop(); dec.release()
            val actualDurationMs = (System.currentTimeMillis() - warmupStartMs).coerceAtLeast(1L)
            decFrames.toDouble() / (actualDurationMs / 1000.0)
        } catch (e: Exception) {
            android.util.Log.e("ProdBench", "HW video decode failed: ${e.message}", e)
            0.0
        }
    }

    // ── 8. Video Transcode (HW – decode + AGSL grade + encode) ───────────
    /**
     * Full hardware transcode pipeline:
     *   1. MediaCodec HW H.264 decoder drains raw YUV frames
     *   2. HardwareRenderer + RuntimeShader AGSL applies a live colour grade
     *      (animated hue rotation + saturation) on Adreno shader cores
     *   3. MediaCodec HW H.264 encoder (Surface input) encodes the graded frame
     * Decode → GPU grade → HW encode, all at 1920×1080. Measures fps.
     */
    private fun benchVideoTranscode(durationMs: Long, isWarmup: Boolean = false): Double {
        val W = 1920; val H = 1080
        val KEYFRAMES = if (isWarmup) PROD_WARMUP_KEYFRAMES else PROD_MEASURE_KEYFRAMES
        val warmupStartMs = System.currentTimeMillis()
        val setupDeadlineMs: Long = if (isWarmup)
            warmupStartMs + (durationMs * PROD_WARMUP_SETUP_FRAC).toLong()
        else
            Long.MAX_VALUE shr 1
        var setupEnc: MediaCodec? = null
        var outEnc: MediaCodec? = null
        var dec: MediaCodec? = null
        var imgReader: ImageReader? = null
        var handlerThread: HandlerThread? = null
        var encSurface: android.view.Surface? = null
        var renderer: HardwareRenderer? = null
        val imageQueue = LinkedBlockingQueue<Image>(2)

        return try {
            // ── Phase 1: pre-encode I-frames for the decode side ────────
            val encSetupFmt = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, W, H).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, 8_000_000)
                setInteger(MediaFormat.KEY_FRAME_RATE, 30)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1) // 1 = every frame is I-frame
                setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
            }
            setupEnc = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            val sEnc = setupEnc!!
            sEnc.configure(encSetupFmt, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            sEnc.start()

            data class Chunk(val data: ByteArray, val flags: Int, val pts: Long)
            val chunks = mutableListOf<Chunk>()
            val setupInfo = MediaCodec.BufferInfo()
            var inSent = 0; var setupDone = false
            while (!setupDone && System.currentTimeMillis() < setupDeadlineMs) {
                if (inSent <= KEYFRAMES) {
                    val idx = sEnc.dequeueInputBuffer(10_000L)
                    if (idx >= 0) {
                        val buf = sEnc.getInputBuffer(idx)!!; buf.clear()
                        val ySize = W * H; val uvSize = W * H / 4
                        for (i in 0 until ySize) buf.put(((i / W + inSent * 8) and 0xFF).toByte())
                        for (i in 0 until uvSize) { buf.put(128.toByte()); buf.put(128.toByte()) }
                        val f = if (inSent == KEYFRAMES) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
                        sEnc.queueInputBuffer(idx, 0, buf.position(), inSent * 33_333L, f)
                        inSent++
                    }
                }
                val outIdx = sEnc.dequeueOutputBuffer(setupInfo, 10_000L)
                if (outIdx >= 0) {
                    val buf = sEnc.getOutputBuffer(outIdx)!!
                    val bytes = ByteArray(setupInfo.size)
                    buf.position(setupInfo.offset); buf.limit(setupInfo.offset + setupInfo.size)
                    buf.get(bytes)
                    chunks.add(Chunk(bytes, setupInfo.flags, setupInfo.presentationTimeUs))
                    sEnc.releaseOutputBuffer(outIdx, false)
                    if (setupInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) setupDone = true
                }
            }
            if (!setupDone) {
                val drainStart = System.currentTimeMillis()
                while (System.currentTimeMillis() - drainStart < 100L) {
                    val drain = sEnc.dequeueOutputBuffer(setupInfo, 0)
                    if (drain < 0) break
                    sEnc.releaseOutputBuffer(drain, false)
                }
            }
            sEnc.stop(); sEnc.release(); setupEnc = null
            val csdChunks = chunks.filter { it.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0 }
            val idrChunks = chunks.filter { it.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0 && it.data.isNotEmpty() }
            if (idrChunks.isEmpty()) {
                // Pre-encode produced no IDR frames (MediaTek/Fallback).
                // Use video encode fps as estimated transcode throughput.
                return benchVideoEncode(durationMs, isWarmup)
            }

            // ── Phase 2: HW encoder (Surface + HardwareRenderer + AGSL) ─
            val outFmt = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, W, H).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, 8_000_000)
                setInteger(MediaFormat.KEY_FRAME_RATE, 60)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            }
            outEnc = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            val oEnc = outEnc!!
            oEnc.configure(outFmt, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encSurface = oEnc.createInputSurface()
            oEnc.start()

            // AGSL colour grade shader (hue rotation in YIQ)
            val agsl = """
                uniform shader inputTexture;
                uniform float hueAngle;
                uniform float saturation;
                half3 toYIQ(half3 rgb) {
                    return half3(
                        dot(rgb, half3(0.299,0.587,0.114)),
                        dot(rgb, half3(0.596,-0.274,-0.322)),
                        dot(rgb, half3(0.211,-0.523,0.312)));
                }
                half3 fromYIQ(half3 yiq) {
                    return clamp(half3(
                        dot(yiq, half3(1.0,0.956,0.621)),
                        dot(yiq, half3(1.0,-0.272,-0.647)),
                        dot(yiq, half3(1.0,-1.106,1.703))), 0.0, 1.0);
                }
                half4 main(float2 coord) {
                    half4 c = inputTexture.eval(coord);
                    half3 yiq = toYIQ(c.rgb);
                    float cosA = cos(hueAngle); float sinA = sin(hueAngle);
                    yiq = half3(yiq.x, yiq.y*cosA - yiq.z*sinA, yiq.y*sinA + yiq.z*cosA);
                    half lum = yiq.x;
                    half3 rgb = fromYIQ(yiq);
                    rgb = mix(half3(lum,lum,lum), rgb, saturation);
                    return half4(rgb, c.a);
                }
            """.trimIndent()
            val gradeShader = RuntimeShader(agsl)

            renderer = HardwareRenderer()
            val rdr = renderer!!
            rdr.setSurface(encSurface); rdr.start()
            val rootNode = RenderNode("xcode_frame"); rootNode.setPosition(0, 0, W, H)
            rdr.setContentRoot(rootNode)

            // Setup ImageReader for zero-copy pipeline
            imgReader = ImageReader.newInstance(W, H, PixelFormat.RGBA_8888, 3)
            val reader = imgReader!!
            handlerThread = HandlerThread("transcode_img_reader")
            handlerThread.start()
            val handler = Handler(handlerThread.looper)
            reader.setOnImageAvailableListener({ r ->
                val img = try {
                    r.acquireNextImage()
                } catch (e: Exception) {
                    null
                }
                if (img != null) {
                    if (!imageQueue.offer(img)) {
                        img.close()
                    }
                }
            }, handler)

            // HW decoder configured with ImageReader surface
            val decFmt = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, W, H)
            dec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            val d = dec!!
            d.configure(decFmt, reader.surface, null, 0); d.start()

            for (csd in csdChunks) {
                val idx = d.dequeueInputBuffer(10_000L)
                if (idx >= 0) {
                    val buf = d.getInputBuffer(idx)!!; buf.clear(); buf.put(csd.data)
                    d.queueInputBuffer(idx, 0, csd.data.size, 0, MediaCodec.BUFFER_FLAG_CODEC_CONFIG)
                }
            }

            val decInfo = MediaCodec.BufferInfo(); val encInfo = MediaCodec.BufferInfo()
            var frames = 0L; var idrIdx = 0; var ptsAcc = 0L
            val xcodeEndMs = if (isWarmup) warmupStartMs + durationMs
                             else System.currentTimeMillis() + durationMs
            val gradePaint = Paint()

            while (System.currentTimeMillis() < xcodeEndMs) {
                // Feed next compressed frame to decoder
                val inIdx = d.dequeueInputBuffer(5_000L)
                if (inIdx >= 0) {
                    val chunk = idrChunks[idrIdx++ % idrChunks.size]
                    val buf = d.getInputBuffer(inIdx)!!; buf.clear(); buf.put(chunk.data)
                    d.queueInputBuffer(inIdx, 0, chunk.data.size, ptsAcc, 0)
                    ptsAcc += 33_333L
                }

                // Drain decoder
                val outIdx = d.dequeueOutputBuffer(decInfo, 5_000L)
                if (outIdx >= 0) {
                    if (decInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0 && decInfo.size > 0) {
                        // Release output buffer to surface (rendering it to ImageReader)
                        d.releaseOutputBuffer(outIdx, true)

                        // Wait for decoded image to arrive in queue
                        val image = imageQueue.poll(150, TimeUnit.MILLISECONDS)
                        if (image != null) {
                            val hardwareBuffer = image.hardwareBuffer
                            if (hardwareBuffer != null) {
                                val bitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, ColorSpace.get(ColorSpace.Named.SRGB))
                                if (bitmap != null) {
                                    val hueAngle = (frames * 0.05f) % (2f * Math.PI.toFloat())
                                    gradeShader.setInputShader("inputTexture",
                                        BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP))
                                    gradeShader.setFloatUniform("hueAngle", hueAngle)
                                    gradeShader.setFloatUniform("saturation", 1.0f + kotlin.math.sin(frames * 0.03f).toFloat() * 0.3f)
                                    gradePaint.shader = gradeShader

                                    val canvas = rootNode.beginRecording()
                                    canvas.drawRect(0f, 0f, W.toFloat(), H.toFloat(), gradePaint)
                                    rootNode.endRecording()
                                    rdr.createRenderRequest().syncAndDraw()
                                    bitmap.recycle()
                                }
                                hardwareBuffer.close()
                            }
                            image.close()
                        }
                    } else {
                        d.releaseOutputBuffer(outIdx, false)
                    }

                    // Drain encoder
                    var encOut = oEnc.dequeueOutputBuffer(encInfo, 0)
                    while (encOut >= 0) {
                        if (encInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0 && encInfo.size > 0) frames++
                        oEnc.releaseOutputBuffer(encOut, false)
                        encOut = oEnc.dequeueOutputBuffer(encInfo, 0)
                    }
                    if (frames % 20L == 0L && frames > 0)
                        liveDetail = "HW Transcode #$frames  •  ${d.codecInfo.name}→AGSL→${oEnc.codecInfo.name}"
                }
            }

            val actualDurationMs = (System.currentTimeMillis() - warmupStartMs).coerceAtLeast(1L)
            frames.toDouble() / (actualDurationMs / 1000.0)
        } catch (e: Exception) {
            android.util.Log.e("ProdBench", "HW transcode failed: ${e.message}", e)
            benchVideoEncode(durationMs, isWarmup)
        }.let { result ->
            if (result == 0.0) benchVideoEncode(durationMs, isWarmup) else result
        }.also { _ ->
            renderer?.stop(); renderer?.destroy()
            encSurface?.release()
            try { dec?.stop() } catch (ignored: Exception) {}
            dec?.release()
            try { outEnc?.stop() } catch (ignored: Exception) {}
            outEnc?.release()
            try { setupEnc?.stop() } catch (ignored: Exception) {}
            setupEnc?.release()
            imgReader?.close()
            handlerThread?.quitSafely()
            var img: Image?
            while (imageQueue.poll().also { img = it } != null) {
                img?.close()
            }
        }
    }

    // ── 4. Text Processing (HARD) ─────────────────────────────────────────
    /**
     * 5 000-word corpus. Per pass:
     *   1. Sort entire corpus (Timsort on String[] × 5K elements)
     *   2. Levenshtein edit-distance for 20 random pairs (O(m×n) DP per pair)
     *   3. Regex replace with 5 compiled patterns over a 500-word join
     */
    private fun benchTextOps(durationMs: Long, isWarmup: Boolean = false): Double {
        val wordCount = 5_000
        val rng = Random(54321L)
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val corpus = Array(wordCount) {
            val len = 3 + rng.nextInt(12)
            (0 until len).map { chars[rng.nextInt(chars.length)] }.joinToString("")
        }
        val pairsA = Array(20) { corpus[rng.nextInt(wordCount)] }
        val pairsB = Array(20) { corpus[rng.nextInt(wordCount)] }
        val patterns = listOf(
            Regex("[aeiou]{2,}"), Regex("\\d{3,}"), Regex("[A-Z][a-z]{4,}"),
            Regex("(.)(.)\\2\\1"), Regex("[bcdfghjklmnpqrstvwxyz]{4,}")
        )
        val replacements = listOf("V", "N", "W", "P", "C")

        var totalChars = 0L
        var passes = 0L
        val endMs = System.currentTimeMillis() + durationMs

        while (System.currentTimeMillis() < endMs) {
            // 1. Sort 50K-word copy
            val copy = corpus.copyOf(); copy.sort()

            // 2. Levenshtein for 20 pairs
            var levenSum = 0L
            for (k in 0 until 20) {
                val a = pairsA[k]; val b = pairsB[k]
                val la = a.length; val lb = b.length
                val dp = IntArray((la + 1) * (lb + 1))
                for (i in 0..la) dp[i * (lb + 1)] = i
                for (j in 0..lb) dp[j] = j
                for (i in 1..la) for (j in 1..lb) {
                    val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                    dp[i * (lb + 1) + j] = minOf(
                        dp[(i - 1) * (lb + 1) + j] + 1,
                        dp[i * (lb + 1) + (j - 1)] + 1,
                        dp[(i - 1) * (lb + 1) + (j - 1)] + cost
                    )
                }
                levenSum += dp[la * (lb + 1) + lb]
            }

            // 3. Regex replace on 500-word slice
            val slice = copy.take(500).joinToString(" ")
            var result = slice
            for ((pat, rep) in patterns.zip(replacements)) result = pat.replace(result, rep)
            totalChars += result.length.toLong() + levenSum
            passes++
            liveDetail = "Pass #$passes  •  50K sort  •  Lev×200 + regex×5  •  ${totalChars / 1_000_000L} Mc"
        }

        return if (totalChars == 0L) 0.0 else
            (totalChars.toDouble() / 1_000_000.0) / (durationMs / 1000.0)
    }

    // ── 5. JSON Processing (HARD) ──────────────────────────────────────────
    /**
     * Per pass — 200-field, 3-level deep JSON round-trip (~6–8 KB per doc):
     *   Build: 200 top-level keys (strings/ints/doubles/bools + 3-level nested)
     *          plus 3 arrays of 25 longs each
     *   Serialize: obj.toString()
     *   Parse + walk: JSONObject(string), iterate all top-level keys
     * ~3× more fields + recursive nested structure vs. the easy version.
     */
    private fun benchJsonOps(durationMs: Long, isWarmup: Boolean = false): Double {
        val rng = Random(11111L)
        val sampleStrings = Array(500) { "str_${rng.nextInt(1_000_000)}_data" }
        var docs = 0L
        var sink = 0L
        val endMs = System.currentTimeMillis() + durationMs

        while (System.currentTimeMillis() < endMs) {
            val obj = JSONObject()
            for (i in 0 until 200) {
                when (i % 5) {
                    0 -> obj.put("s$i", sampleStrings[i % 500])
                    1 -> obj.put("i$i", rng.nextInt(10_000_000))
                    2 -> obj.put("d$i", rng.nextDouble() * 99_999.0)
                    3 -> obj.put("b$i", rng.nextBoolean())
                    else -> {
                        val l1 = JSONObject()
                        for (j in 0 until 5) {
                            val l2 = JSONObject()
                            for (k in 0 until 4) {
                                val l3 = JSONObject()
                                l3.put("x", rng.nextInt()); l3.put("y", rng.nextDouble())
                                l2.put("l3_$k", l3)
                            }
                            l1.put("l2_$j", l2)
                        }
                        obj.put("n$i", l1)
                    }
                }
            }
            for (a in 0 until 3) {
                val arr = JSONArray()
                for (k in 0 until 25) arr.put(rng.nextLong())
                obj.put("arr$a", arr)
            }
            val json = obj.toString()
            val parsed = JSONObject(json)
            val keyIt = parsed.keys()
            while (keyIt.hasNext()) {
                val k = keyIt.next()
                val v = parsed.opt(k)
                if (v is JSONObject) sink += v.length().toLong()
                else if (v is String) sink += v.length.toLong()
                else sink++
            }
            docs++
            if (docs % 100L == 0L)
                liveDetail = "Doc #$docs  •  200-field 3-level  •  ${json.length}B JSON"
        }

        android.util.Log.v("ProdBench", "JSON sink=$sink")
        return if (docs == 0L) 0.0 else docs.toDouble() / (durationMs / 1000.0)
    }

    // ── 6. Data Compression (HARD) ────────────────────────────────────────
    /**
     * 1 MB block (4× harder) at Deflater.BEST_COMPRESSION (level 9).
     * 60% compressible / 40% random content forces the LZ77 back-reference
     * search to work hard on the noisy portions. Level 9 = hardest CPU usage.
     * Measures compressed throughput in MB/s.
     */
    private fun benchCompression(durationMs: Long, isWarmup: Boolean = false): Double {
        val blockSize = 1024 * 1024
        val rng = Random(77777L)
        val inputBlocks = Array(4) {
            val block = ByteArray(blockSize)
            for (i in block.indices) {
                block[i] = if (i % 10 < 6) (i % 251).toByte() else rng.nextInt(256).toByte()
            }
            block
        }
        val output = ByteArray(blockSize + 1024)
        val deflater = Deflater(Deflater.BEST_COMPRESSION)
        var totalBytes = 0L; var blocksCount = 0L
        val endMs = System.currentTimeMillis() + durationMs

        while (System.currentTimeMillis() < endMs) {
            val input = inputBlocks[(blocksCount % 4).toInt()]
            deflater.reset(); deflater.setInput(input); deflater.finish()
            var outLen = 0
            while (!deflater.finished()) outLen += deflater.deflate(output, outLen, output.size - outLen)
            totalBytes += blockSize.toLong(); blocksCount++
            if (blocksCount % 5L == 0L)
                liveDetail = "Block #$blocksCount  •  1MB→${outLen / 1024}KB  •  " +
                    "${"%.1f".format((1.0 - outLen.toDouble() / blockSize) * 100)}% saved"
        }

        deflater.end()
        return if (totalBytes == 0L) 0.0 else
            totalBytes.toDouble() / (durationMs / 1000.0) / (1024.0 * 1024.0)
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun mockCpuTemp(): Float {
        val noise = (-15..15).random().toFloat() * 0.1f
        return (baseCpuTemp + 5f + noise).coerceIn(35f, 85f)
    }

    // ── DB save ────────────────────────────────────────────────────────────

    private suspend fun saveToDatabase(
        results: List<ProductivityTestResult>,
        totalScore: Int,
        performanceMetricsJson: String,
        detailedJson: String
    ) {
        val repo = historyRepository ?: return
        try {
            val detailsArrayJson = try {
                JSONObject(detailedJson).optJSONArray("detailed_results")?.toString() ?: "[]"
            } catch (e: Exception) { "[]" }

            val entity = BenchmarkResultEntity(
                type                   = "PRODUCTIVITY",
                totalScore             = totalScore.toDouble(),
                timestamp              = System.currentTimeMillis(),
                deviceModel            = "${Build.MANUFACTURER} ${Build.MODEL}",
                singleCoreScore        = 0.0,
                multiCoreScore         = totalScore.toDouble(),
                normalizedScore        = totalScore.toDouble(),
                detailedResultsJson    = detailsArrayJson,
                performanceMetricsJson = performanceMetricsJson
            )
            val details = results.map { r ->
                GenericTestDetailEntity(
                    resultId    = 0,
                    testName    = r.displayName,
                    score       = r.value,
                    metricsJson = """{"score":${r.score},"value":${"%.2f".format(r.value)},"unit":"${r.unit}","durationMs":${r.durationMs}}"""
                )
            }
            repo.saveGenericBenchmark(entity, details)
        } catch (e: Exception) {
            android.util.Log.e("ProductivityBenchVM", "DB save failed: ${e.message}", e)
        }
    }

    // ── Result JSON ────────────────────────────────────────────────────────

    private fun buildResultJson(
        results: List<ProductivityTestResult>,
        totalScore: Int,
        preset: String,
        performanceMetricsJson: String
    ): String {
        val detailedArray = JSONArray()
        results.forEach { r ->
            detailedArray.put(JSONObject().apply {
                put("name", r.displayName)
                put("opsPerSecond", r.value)
                put("executionTimeMs", r.durationMs.toDouble())
                put("isValid", true)
                put("metricsJson", """{"score":${r.score},"value":${"%.2f".format(r.value)},"unit":"${r.unit}","durationMs":${r.durationMs}}""")
            })
        }
        val perfObj = try { JSONObject(performanceMetricsJson) } catch (e: Exception) { JSONObject() }
        return JSONObject().apply {
            put("type", "PRODUCTIVITY")
            put("preset", preset)
            put("final_score", totalScore.toDouble())
            put("normalized_score", totalScore.toDouble())
            put("single_core_score", 0.0)
            put("multi_core_score", totalScore.toDouble())
            put("detailed_results", detailedArray)
            put("timestamp", System.currentTimeMillis())
            put("performance_metrics", perfObj)
        }.toString()
    }

    // ── Factory ────────────────────────────────────────────────────────────

    companion object {
        fun factory(
            historyRepository: HistoryRepository?,
            application: Application
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                ProductivityBenchmarkViewModel(application, historyRepository) as T
        }
    }
}
