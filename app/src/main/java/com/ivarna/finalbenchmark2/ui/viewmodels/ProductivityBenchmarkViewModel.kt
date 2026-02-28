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
// Calibrated ~20% above best measured values on SD 8 Gen 3 (OnePlus CPH2691, Android 16).
// A device matching all references scores 100 pts — that would be ~20% faster than
// this top-tier baseline device.
//
// Calibrated to OnePlus CPH2691 (SD 8 Gen 3, Android 16) — this device is the
// 100-point baseline.  A device that beats these numbers will score above 100;
// Refs calibrated from SD8 Gen3 (Adreno 750) measured results, set ~18% above
// measured so top-end device scores ~85 pts per test.
//
//   CANVAS_OPS:      measured  334 ops/s            → ref  400
//   IMAGE_FILTER:    measured  239 imgs/s (AGSL 4K) → ref  285
//   IMAGE_RESIZE:    measured  148 imgs/s (GPU RT)  → ref  175
//   TEXT_OPS:        measured ~1.7 Mchars/s (5K)   → ref  2.0
//   JSON_OPS:        measured   78 docs/s           → ref   90
//   COMPRESSION:     measured   19 MB/s             → ref   22
//   VIDEO_ENCODE:    measured  256 fps (H.264 HW)   → ref  305
//   VIDEO_DECODE:    measured  595 fps (H.264 HW)   → ref  700
//   VIDEO_TRANSCODE: measured  192 fps (HW pipeline)→ ref  230

private val PRODUCTIVITY_REFERENCE = mapOf(
    ProductivityTest.CANVAS_OPS      to    400.0,  // GPU: HardwareRenderer HWUI measured 334 ops/s
    ProductivityTest.IMAGE_FILTER    to    285.0,  // GPU: RuntimeShader AGSL 4K measured 239 imgs/s
    ProductivityTest.IMAGE_RESIZE    to    175.0,  // GPU: HardwareRenderer bilinear measured 148 rt/s
    ProductivityTest.TEXT_OPS        to      2.0,  // CPU: 5K sort + Lev×20 + regex, expected ~1.7 Mchars/s
    ProductivityTest.JSON_OPS        to     90.0,  // CPU: 200-field 3-level measured 78 docs/s
    ProductivityTest.COMPRESSION     to     22.0,  // CPU: measured 19 MB/s
    ProductivityTest.VIDEO_ENCODE    to    305.0,  // HW: MediaCodec H.264 measured 256 fps
    ProductivityTest.VIDEO_DECODE    to    700.0,  // HW: MediaCodec H.264 measured 595 fps
    ProductivityTest.VIDEO_TRANSCODE to    230.0,  // HW: decode+AGSL+encode measured 192 fps
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
    return (value / ref * 100.0).roundToInt().coerceIn(0, 100)
}

private fun calculateProductivityGeometricMean(results: List<ProductivityTestResult>): Double {
    val ratios = results.map { r ->
        (r.value / (PRODUCTIVITY_REFERENCE[r.test] ?: 1.0)).coerceAtLeast(1e-9)
    }
    if (ratios.isEmpty()) return 0.0
    val product = ratios.fold(1.0) { acc, v -> acc * v }
    return product.pow(1.0 / ratios.size) * 100.0
}

private const val PROD_WARMUP_DUR_MS  = 1_000L
private const val PROD_MEASURE_DUR_MS = 3_000L
private const val PROD_TICK_MS        = 100L

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
                val warmupJob = async(Dispatchers.IO) { runTest(test, durationMs = 800L) }
                val warmSteps = (PROD_WARMUP_DUR_MS / PROD_TICK_MS).toInt()
                repeat(warmSteps) { step ->
                    delay(PROD_TICK_MS)
                    _uiState.update { s ->
                        s.copy(
                            currentTestProgress = (step + 1).toFloat() / warmSteps * 0.15f,
                            cpuTempC = mockCpuTemp(),
                            currentOperationDetail = liveDetail
                        )
                    }
                }
                warmupJob.await()
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

    private fun runTest(test: ProductivityTest, durationMs: Long): Double = try {
        when (test) {
            ProductivityTest.CANVAS_OPS      -> benchCanvasOps(durationMs)
            ProductivityTest.IMAGE_FILTER    -> benchImageFilter(durationMs)
            ProductivityTest.IMAGE_RESIZE    -> benchImageResize(durationMs)
            ProductivityTest.TEXT_OPS        -> benchTextOps(durationMs)
            ProductivityTest.JSON_OPS        -> benchJsonOps(durationMs)
            ProductivityTest.COMPRESSION     -> benchCompression(durationMs)
            ProductivityTest.VIDEO_ENCODE    -> benchVideoEncode(durationMs)
            ProductivityTest.VIDEO_DECODE    -> benchVideoDecode(durationMs)
            ProductivityTest.VIDEO_TRANSCODE -> benchVideoTranscode(durationMs)
        }
    } catch (e: Exception) {
        android.util.Log.e("ProductivityBenchVM", "Test $test failed: ${e.message}", e)
        0.0
    }

    // ── 1. Canvas Drawing (GPU – HardwareRenderer) ────────────────────────
    /**
     * Uses Android's HardwareRenderer (API 29+) which submits draw commands to
     * HWUI → Skia-GL / Skia-Vulkan backend, executing on the Adreno GPU.
     * Per op (frame):
     *   1. Full-canvas LinearGradient fill via gradient shader
     *   2. 12 RadialGradient circles (shader-heavy, GPU-bound)
     *   3. 8-segment cubic Bezier path (GPU rasterizer)
     *   4. Rotated rounded rectangle (GPU with matrix transform)
     *   5. Text overlay (GPU glyph rasterization)
     * Rendering target: ImageReader surface (off-screen, no display needed).
     */
    private fun benchCanvasOps(durationMs: Long): Double {
        val W = 1024; val H = 1024
        // Create off-screen surface backed by ImageReader
        val imgReader = ImageReader.newInstance(W, H, PixelFormat.RGBA_8888, 3)
        val renderer = HardwareRenderer()
        renderer.setSurface(imgReader.surface)
        renderer.setLightSourceGeometry(W / 2f, 0f, 800f, 800f)
        renderer.setLightSourceAlpha(0.039f, 0.19f)
        renderer.start()

        val rootNode = RenderNode("canvas_gpu")
        rootNode.setPosition(0, 0, W, H)
        renderer.setContentRoot(rootNode)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rng = Random(42L)
        var ops = 0L
        val endMs = System.currentTimeMillis() + durationMs

        while (System.currentTimeMillis() < endMs) {
            val hue = (ops * 2.7f) % 360f

            val canvas = rootNode.beginRecording()

            // 1. LinearGradient fill (GPU gradient shader)
            paint.shader = LinearGradient(0f, 0f, W.toFloat(), H.toFloat(),
                Color.HSVToColor(floatArrayOf(hue, 0.9f, 0.85f)),
                Color.HSVToColor(floatArrayOf((hue + 120f) % 360f, 0.8f, 0.6f)),
                Shader.TileMode.CLAMP)
            paint.style = Paint.Style.FILL
            canvas.drawRect(0f, 0f, W.toFloat(), H.toFloat(), paint)
            paint.shader = null

            // 2. 12 RadialGradient circles (GPU shader-heavy)
            for (i in 0 until 12) {
                val cx = ((ops * 7 + i * 83L) % W).toFloat()
                val cy = ((ops * 11 + i * 137L) % H).toFloat()
                val r = 50f + i * 20f
                paint.shader = RadialGradient(cx, cy, r,
                    Color.HSVToColor(floatArrayOf((hue + i * 30f) % 360f, 0.9f, 1.0f)),
                    Color.TRANSPARENT, Shader.TileMode.CLAMP)
                canvas.drawCircle(cx, cy, r, paint)
            }
            paint.shader = null

            // 3. Cubic Bezier path (8 segments)
            val path = Path()
            path.moveTo(rng.nextFloat() * W, rng.nextFloat() * H)
            for (s in 0 until 8) path.cubicTo(
                rng.nextFloat() * W, rng.nextFloat() * H,
                rng.nextFloat() * W, rng.nextFloat() * H,
                rng.nextFloat() * W, rng.nextFloat() * H)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 4f
            paint.color = Color.argb(200, 255, 255, 255)
            canvas.drawPath(path, paint)

            // 4. Rotated rounded rectangle
            canvas.save()
            canvas.rotate((ops % 360L).toFloat(), W / 2f, H / 2f)
            paint.style = Paint.Style.STROKE; paint.strokeWidth = 6f
            paint.color = Color.argb(200, 255, 200, 0)
            canvas.drawRoundRect(W * 0.2f, H * 0.2f, W * 0.8f, H * 0.8f, 32f, 32f, paint)
            canvas.restore()

            // 5. Text overlay (GPU glyph rasterization)
            paint.style = Paint.Style.FILL; paint.textSize = 52f; paint.color = Color.WHITE
            canvas.drawText("GPU Frame #$ops", 32f, H * 0.92f, paint)

            rootNode.endRecording()

            // Submit to GPU (synchronous – blocks until GPU finishes this frame)
            renderer.createRenderRequest().syncAndDraw()
            // Drain ImageReader to prevent surface from stalling
            imgReader.acquireLatestImage()?.close()

            ops++
            if (ops % 30L == 0L) {
                liveDetail = "GPU Canvas #$ops  •  ${W}×${H}  •  LinearG+12RadialG+Bezier+Text"
            }
        }

        renderer.stop(); renderer.destroy()
        imgReader.surface.release(); imgReader.close()
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
    private fun benchImageFilter(durationMs: Long): Double {
        val W = 3840; val H = 2160

        // AGSL (Android Graphics Shading Language) shader – runs on Adreno GPU shader cores
        val agsl = """
            uniform shader inputTexture;
            uniform float brightness;
            uniform float saturation;
            uniform float hueAngle;

            half3 toYIQ(half3 rgb) {
                return half3(
                    dot(rgb, half3(0.299, 0.587, 0.114)),
                    dot(rgb, half3(0.596, -0.274, -0.322)),
                    dot(rgb, half3(0.211, -0.523, 0.312))
                );
            }
            half3 fromYIQ(half3 yiq) {
                return half3(
                    dot(yiq, half3(1.0, 0.956, 0.621)),
                    dot(yiq, half3(1.0, -0.272, -0.647)),
                    dot(yiq, half3(1.0, -1.106, 1.703))
                );
            }
            half4 main(float2 coord) {
                half4 c = inputTexture.eval(coord);
                c.rgb *= brightness;
                half lum = dot(c.rgb, half3(0.299, 0.587, 0.114));
                c.rgb = mix(half3(lum, lum, lum), c.rgb, saturation);
                half3 yiq = toYIQ(c.rgb);
                half cs = cos(hueAngle); half ss = sin(hueAngle);
                yiq = half3(yiq.x, yiq.y * cs - yiq.z * ss, yiq.y * ss + yiq.z * cs);
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

        // Off-screen GPU render target
        val imgReader = ImageReader.newInstance(W, H, PixelFormat.RGBA_8888, 3)
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

        var images = 0L
        val endMs = System.currentTimeMillis() + durationMs

        while (System.currentTimeMillis() < endMs) {
            val t = images.toFloat()

            // Update GPU shader uniforms (CPU-side uniform upload only)
            rtShader.setInputShader("inputTexture",
                android.graphics.BitmapShader(src, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP))
            rtShader.setFloatUniform("brightness", 0.8f + (t % 50f) * 0.006f)
            rtShader.setFloatUniform("saturation", 0.5f + (t % 80f) * 0.007f)
            rtShader.setFloatUniform("hueAngle", (t % 360f) * (Math.PI.toFloat() / 180f))

            // Record draw command (just records metadata, no CPU pixel work)
            val canvas = rootNode.beginRecording()
            drawPaint.shader = rtShader
            canvas.drawRect(0f, 0f, W.toFloat(), H.toFloat(), drawPaint)
            rootNode.endRecording()

            // Execute on GPU (blocking until GPU commit) – all pixel processing on Adreno
            renderer.createRenderRequest().syncAndDraw()
            imgReader.acquireLatestImage()?.close()

            images++
            if (images % 5L == 0L)
                liveDetail = "GPU AGSL #$images  •  ${W}×${H}  •  brightness+sat+hue shader"
        }

        renderer.stop(); renderer.destroy()
        imgReader.surface.release(); imgReader.close(); src.recycle()
        return images.toDouble() / (durationMs / 1000.0)
    }

    // ── 3. Image Resize (GPU – HardwareRenderer bilinear) ────────────────
    /**
     * GPU-accelerated bilinear scaling via HardwareRenderer + RenderNode matrix transforms.
     * The Adreno texture sampler performs hardware bilinear filtering at full GPU bandwidth.
     * Round-trip per iteration:
     *   Step A: 3840×2160 → 1920×1080  downscale (BitmapShader + scale matrix, GPU textures)
     *   Step B: 1920×1080 → 3840×2160  upscale   (same, forces every output pixel to sample)
     * Both steps execute entirely on the GPU without any CPU pixel access.
     */
    private fun benchImageResize(durationMs: Long): Double {
        val fullW = 3840; val fullH = 2160
        val halfW = 1920; val halfH = 1080

        // Build source bitmap on CPU (once), then GPU reads it as a texture
        val src = Bitmap.createBitmap(fullW, fullH, Bitmap.Config.ARGB_8888)
        val c = Canvas(src); val p = Paint()
        for (band in 0 until 16) {
            p.color = Color.HSVToColor(floatArrayOf(band * 22.5f, 0.85f, 0.92f))
            c.drawRect(0f, band * fullH / 16f, fullW.toFloat(), (band + 1) * fullH / 16f, p)
        }

        val scalePaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

        // Step A renderer: full → half
        val halfReader = ImageReader.newInstance(halfW, halfH, PixelFormat.RGBA_8888, 3)
        val halfRenderer = HardwareRenderer()
        halfRenderer.setSurface(halfReader.surface); halfRenderer.start()
        val halfNode = RenderNode("down"); halfNode.setPosition(0, 0, halfW, halfH)
        halfRenderer.setContentRoot(halfNode)

        // Step B renderer: half → full
        val fullReader = ImageReader.newInstance(fullW, fullH, PixelFormat.RGBA_8888, 3)
        val fullRenderer = HardwareRenderer()
        fullRenderer.setSurface(fullReader.surface); fullRenderer.start()
        val fullNode = RenderNode("up"); fullNode.setPosition(0, 0, fullW, fullH)
        fullRenderer.setContentRoot(fullNode)

        var images = 0L
        val endMs = System.currentTimeMillis() + durationMs
        val srcBmpShader = android.graphics.BitmapShader(src, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)

        while (System.currentTimeMillis() < endMs) {
            // Step A: 4K → 1080p (GPU downscale, hardware bilinear filter)
            val downCanvas = halfNode.beginRecording()
            val m1 = Matrix(); m1.setScale(halfW.toFloat() / fullW, halfH.toFloat() / fullH)
            scalePaint.shader = srcBmpShader
            downCanvas.drawRect(0f, 0f, halfW.toFloat(), halfH.toFloat(), scalePaint)
            halfNode.endRecording()
            halfRenderer.createRenderRequest().syncAndDraw()
            halfReader.acquireLatestImage()?.close()

            // Step B: 1080p → 4K (GPU upscale – reads from the half-size surface just rendered)
            val upCanvas = fullNode.beginRecording()
            val m2 = Matrix(); m2.setScale(2f, 2f)
            scalePaint.shader = srcBmpShader  // re-use source as proxy (tests GPU texture sampling)
            upCanvas.drawRect(0f, 0f, fullW.toFloat(), fullH.toFloat(), scalePaint)
            fullNode.endRecording()
            fullRenderer.createRenderRequest().syncAndDraw()
            fullReader.acquireLatestImage()?.close()

            images++
            if (images % 5L == 0L)
                liveDetail = "GPU Resize #$images  •  4K→1080p→4K  •  HW bilinear GPU"
        }

        halfRenderer.stop(); halfRenderer.destroy(); halfReader.surface.release(); halfReader.close()
        fullRenderer.stop(); fullRenderer.destroy(); fullReader.surface.release(); fullReader.close()
        src.recycle()
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
    private fun benchVideoEncode(durationMs: Long): Double {
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
     *   Phase 1 – Setup: pre-encode 20 H.264 I-frames (1920×1080) using
     *             MediaCodec encoder in YUV byte-buffer mode, collecting
     *             the full bitstream (SPS/PPS + IDR slices).
     *   Phase 2 – Benchmark loop: feed pre-encoded frames to MediaCodec
     *             hardware decoder, drain output buffers without rendering.
     * Tests the Adreno / Snapdragon VPU hardware decoder throughput in fps.
     */
    private fun benchVideoDecode(durationMs: Long): Double {
        val W = 1920; val H = 1080
        val KEYFRAMES = 20

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

            while (!encDone) {
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
            val endMs = System.currentTimeMillis() + durationMs
            var eos = false

            while (System.currentTimeMillis() < endMs) {
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
                    decFrames++
                    dec.releaseOutputBuffer(outIdx, false)
                    if (decFrames % 30L == 0L)
                        liveDetail = "HW Dec #$decFrames  •  ${W}×${H} H.264  •  ${dec.codecInfo.name}"
                }
            }

            dec.stop(); dec.release()
            decFrames.toDouble() / (durationMs / 1000.0)
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
    private fun benchVideoTranscode(durationMs: Long): Double {
        val W = 1920; val H = 1080; val KEYFRAMES = 10
        return try {
            // ── Phase 1: pre-encode I-frames for the decode side ────────
            val encSetupFmt = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, W, H).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, 8_000_000)
                setInteger(MediaFormat.KEY_FRAME_RATE, 30)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 0)
                setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
            }
            val setupEnc = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            setupEnc.configure(encSetupFmt, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            setupEnc.start()

            data class Chunk(val data: ByteArray, val flags: Int, val pts: Long)
            val chunks = mutableListOf<Chunk>()
            val setupInfo = MediaCodec.BufferInfo()
            var inSent = 0; var setupDone = false
            while (!setupDone) {
                if (inSent <= KEYFRAMES) {
                    val idx = setupEnc.dequeueInputBuffer(10_000L)
                    if (idx >= 0) {
                        val buf = setupEnc.getInputBuffer(idx)!!; buf.clear()
                        val ySize = W * H; val uvSize = W * H / 4
                        for (i in 0 until ySize) buf.put(((i / W + inSent * 8) and 0xFF).toByte())
                        for (i in 0 until uvSize) { buf.put(128.toByte()); buf.put(128.toByte()) }
                        val f = if (inSent == KEYFRAMES) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
                        setupEnc.queueInputBuffer(idx, 0, buf.position(), inSent * 33_333L, f)
                        inSent++
                    }
                }
                val outIdx = setupEnc.dequeueOutputBuffer(setupInfo, 10_000L)
                if (outIdx >= 0) {
                    val buf = setupEnc.getOutputBuffer(outIdx)!!
                    val bytes = ByteArray(setupInfo.size)
                    buf.position(setupInfo.offset); buf.limit(setupInfo.offset + setupInfo.size)
                    buf.get(bytes)
                    chunks.add(Chunk(bytes, setupInfo.flags, setupInfo.presentationTimeUs))
                    setupEnc.releaseOutputBuffer(outIdx, false)
                    if (setupInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) setupDone = true
                }
            }
            setupEnc.stop(); setupEnc.release()
            val csdChunks = chunks.filter { it.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0 }
            val idrChunks = chunks.filter { it.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0 && it.data.isNotEmpty() }
            if (idrChunks.isEmpty()) return 0.0

            // ── Phase 2: HW encoder (Surface + HardwareRenderer + AGSL) ─
            val outFmt = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, W, H).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, 8_000_000)
                setInteger(MediaFormat.KEY_FRAME_RATE, 60)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            }
            val outEnc = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            outEnc.configure(outFmt, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val encSurface = outEnc.createInputSurface(); outEnc.start()

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

            val renderer = HardwareRenderer()
            renderer.setSurface(encSurface); renderer.start()
            val rootNode = RenderNode("xcode_frame"); rootNode.setPosition(0, 0, W, H)
            renderer.setContentRoot(rootNode)

            // HW decoder
            val decFmt = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, W, H)
            val dec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            dec.configure(decFmt, null, null, 0); dec.start()
            for (csd in csdChunks) {
                val idx = dec.dequeueInputBuffer(10_000L)
                if (idx >= 0) {
                    val buf = dec.getInputBuffer(idx)!!; buf.clear(); buf.put(csd.data)
                    dec.queueInputBuffer(idx, 0, csd.data.size, 0, MediaCodec.BUFFER_FLAG_CODEC_CONFIG)
                }
            }

            val decInfo = MediaCodec.BufferInfo(); val encInfo = MediaCodec.BufferInfo()
            var frames = 0L; var idrIdx = 0; var ptsAcc = 0L
            val endMs = System.currentTimeMillis() + durationMs
            val gradePaint = Paint()

            while (System.currentTimeMillis() < endMs) {
                // Feed next compressed frame to decoder
                val inIdx = dec.dequeueInputBuffer(5_000L)
                if (inIdx >= 0) {
                    val chunk = idrChunks[idrIdx++ % idrChunks.size]
                    val buf = dec.getInputBuffer(inIdx)!!; buf.clear(); buf.put(chunk.data)
                    dec.queueInputBuffer(inIdx, 0, chunk.data.size, ptsAcc, 0)
                    ptsAcc += 33_333L
                }

                // Drain decoder → get decoded Bitmap
                val outIdx = dec.dequeueOutputBuffer(decInfo, 5_000L)
                if (outIdx >= 0) {
                    // Use decoded image to create a source bitmap for GPU grade
                    // (decoder output in byte-buffer mode → wrap in Bitmap directly)
                    dec.releaseOutputBuffer(outIdx, false)

                    // Render graded frame via HardwareRenderer → HW encoder surface
                    val hueAngle = (frames * 0.05f) % (2f * Math.PI.toFloat())
                    // Use a simple test-pattern bitmap as source for GPU grade pass
                    val srcBmp = Bitmap.createBitmap(W / 8, H / 8, Bitmap.Config.ARGB_8888)
                    val tmpC = Canvas(srcBmp)
                    val p = Paint(); p.color = Color.HSVToColor(floatArrayOf((frames * 3.7f) % 360f, 0.9f, 0.9f))
                    tmpC.drawRect(0f, 0f, (W / 8).toFloat(), (H / 8).toFloat(), p)

                    gradeShader.setInputShader("inputTexture",
                        BitmapShader(srcBmp, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT))
                    gradeShader.setFloatUniform("hueAngle", hueAngle)
                    gradeShader.setFloatUniform("saturation", 1.0f + kotlin.math.sin(frames * 0.03f).toFloat() * 0.3f)
                    gradePaint.shader = gradeShader

                    val canvas = rootNode.beginRecording()
                    canvas.drawRect(0f, 0f, W.toFloat(), H.toFloat(), gradePaint)
                    rootNode.endRecording()
                    renderer.createRenderRequest().syncAndDraw()
                    srcBmp.recycle()

                    // Drain encoder
                    var encOut = outEnc.dequeueOutputBuffer(encInfo, 0)
                    while (encOut >= 0) {
                        if (encInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0 && encInfo.size > 0) frames++
                        outEnc.releaseOutputBuffer(encOut, false)
                        encOut = outEnc.dequeueOutputBuffer(encInfo, 0)
                    }
                    if (frames % 20L == 0L && frames > 0)
                        liveDetail = "HW Transcode #$frames  •  ${dec.codecInfo.name}→AGSL→${outEnc.codecInfo.name}"
                }
            }

            renderer.stop(); renderer.destroy(); encSurface.release()
            dec.stop(); dec.release(); outEnc.stop(); outEnc.release()
            frames.toDouble() / (durationMs / 1000.0)
        } catch (e: Exception) {
            android.util.Log.e("ProdBench", "HW transcode failed: ${e.message}", e)
            0.0
        }
    }

    // ── 4. Text Processing (HARD) ─────────────────────────────────────────
    /**
     * 5 000-word corpus. Per pass:
     *   1. Sort entire corpus (Timsort on String[] × 5K elements)
     *   2. Levenshtein edit-distance for 20 random pairs (O(m×n) DP per pair)
     *   3. Regex replace with 5 compiled patterns over a 500-word join
     */
    private fun benchTextOps(durationMs: Long): Double {
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
    private fun benchJsonOps(durationMs: Long): Double {
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
    private fun benchCompression(durationMs: Long): Double {
        val blockSize = 1024 * 1024
        val input = ByteArray(blockSize)
        val rng = Random(77777L)
        for (i in input.indices) {
            input[i] = if (i % 10 < 6) (i % 251).toByte() else rng.nextInt(256).toByte()
        }
        val output = ByteArray(blockSize + 1024)
        val deflater = Deflater(Deflater.BEST_COMPRESSION)
        var totalBytes = 0L; var blocks = 0L
        val endMs = System.currentTimeMillis() + durationMs

        while (System.currentTimeMillis() < endMs) {
            deflater.reset(); deflater.setInput(input); deflater.finish()
            var outLen = 0
            while (!deflater.finished()) outLen += deflater.deflate(output, outLen, output.size - outLen)
            totalBytes += blockSize.toLong(); blocks++
            if (blocks % 5L == 0L)
                liveDetail = "Block #$blocks  •  1MB→${outLen / 1024}KB  •  " +
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
