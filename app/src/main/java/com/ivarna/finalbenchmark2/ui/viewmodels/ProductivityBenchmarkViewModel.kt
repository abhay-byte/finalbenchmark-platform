package com.ivarna.finalbenchmark2.ui.viewmodels

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
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
// refs are set 5% above measured so a perfectly matched device scores ~95.
//
//   CANVAS_OPS:      measured 12400 ops/s          → ref 13000
//   IMAGE_FILTER:    measured 17   images/s (4K)   → ref 18
//   IMAGE_RESIZE:    measured 163  images/s (4K→Q) → ref 170
//   TEXT_OPS:        measured 12.5 Mchars/s         → ref 13
//   JSON_OPS:        measured 1968 docs/s            → ref 2100
//   COMPRESSION:     measured 49   MB/s              → ref 52
//   VIDEO_ENCODE:    measured 32   fps               → ref 34
//   VIDEO_DECODE:    estimated 130 fps (1080p JPEG)  → ref 140   (recalibrate)
//   VIDEO_TRANSCODE: estimated 21  fps (1080→720)    → ref 22    (recalibrate)

private val PRODUCTIVITY_REFERENCE = mapOf(
    ProductivityTest.CANVAS_OPS      to  3_500.0,  // hard: 1024px 6-layer+shadow ops ~3200/s
    ProductivityTest.IMAGE_FILTER    to      4.5,  // hard: 3-pass 4K ColorMatrix chain ~4.2/s
    ProductivityTest.IMAGE_RESIZE    to     28.0,  // hard: 4K↔1080p round-trip ~26/s
    ProductivityTest.TEXT_OPS        to      4.5,  // hard: 50K words + Levenshtein×200 ~4.2 Mc/s
    ProductivityTest.JSON_OPS        to    420.0,  // hard: 200-field 3-level deep ~400 docs/s
    ProductivityTest.COMPRESSION     to      9.0,  // hard: 1MB BEST_COMPRESSION ~8.5 MB/s
    ProductivityTest.VIDEO_ENCODE    to      8.0,  // hard: 4K JPEG encode ~7.5 fps
    ProductivityTest.VIDEO_DECODE    to     55.0,  // hard: 24×1080p q100 pool ~52 fps
    ProductivityTest.VIDEO_TRANSCODE to      5.0,  // hard: 4K→1080p+3-pass grade+q95 ~4.7 fps
)

private val PRODUCTIVITY_TESTS = ProductivityTest.values().toList()

private fun ProductivityTest.displayName() = when (this) {
    ProductivityTest.CANVAS_OPS      -> "Canvas Drawing"
    ProductivityTest.IMAGE_FILTER    -> "Image Filter 3-Pass"
    ProductivityTest.IMAGE_RESIZE    -> "Image Resize RT"
    ProductivityTest.TEXT_OPS        -> "Text Processing"
    ProductivityTest.JSON_OPS        -> "JSON Processing"
    ProductivityTest.COMPRESSION     -> "Data Compression"
    ProductivityTest.VIDEO_ENCODE    -> "Video Encode (4K)"
    ProductivityTest.VIDEO_DECODE    -> "Video Decode"
    ProductivityTest.VIDEO_TRANSCODE -> "Video Transcode (4K)"
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

    // ── 1. Canvas Drawing (HARD) ──────────────────────────────────────────
    /**
     * 1024×1024 off-screen bitmap. Per op:
     *   1. Full-canvas LinearGradient fill (forces GPU rasteriser path)
     *   2. Shadow-layer cubic Bezier with 20 segments
     *   3. Radial-gradient filled circle with shadow
     *   4. Matrix-transformed (rotate+scale) rounded rectangle
     *   5. Anti-aliased text overlay
     * Shadow layers prevent Skia fast-paths and force full compositing pipeline.
     */
    private fun benchCanvasOps(durationMs: Long): Double {
        val SIZE = 1024
        val bmp = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val path = Path()
        val matrix = android.graphics.Matrix()
        val rng = Random(12345L)
        var ops = 0L
        val endMs = System.currentTimeMillis() + durationMs

        while (System.currentTimeMillis() < endMs) {
            val angle = (ops % 360L).toFloat()

            // 1. Full-canvas LinearGradient fill
            val gc1 = Color.HSVToColor(floatArrayOf(angle % 360f, 0.9f, 0.8f))
            val gc2 = Color.HSVToColor(floatArrayOf((angle + 180f) % 360f, 0.7f, 0.6f))
            paint.shader = android.graphics.LinearGradient(
                0f, 0f, SIZE.toFloat(), SIZE.toFloat(), gc1, gc2, Shader.TileMode.CLAMP)
            paint.style = Paint.Style.FILL
            paint.clearShadowLayer()
            canvas.drawRect(0f, 0f, SIZE.toFloat(), SIZE.toFloat(), paint)
            paint.shader = null

            // 2. 20-segment cubic Bezier with shadow layer
            paint.setShadowLayer(12f, 4f, 4f,
                Color.argb(180, rng.nextInt(256), rng.nextInt(256), rng.nextInt(256)))
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f + rng.nextFloat() * 4f
            paint.color = Color.argb(220, rng.nextInt(256), rng.nextInt(256), rng.nextInt(256))
            path.reset()
            path.moveTo(rng.nextFloat() * SIZE, rng.nextFloat() * SIZE)
            for (s in 0 until 20) {
                path.cubicTo(
                    rng.nextFloat() * SIZE, rng.nextFloat() * SIZE,
                    rng.nextFloat() * SIZE, rng.nextFloat() * SIZE,
                    rng.nextFloat() * SIZE, rng.nextFloat() * SIZE
                )
            }
            canvas.drawPath(path, paint)

            // 3. Radial-gradient circle with shadow
            val cx = rng.nextFloat() * SIZE; val cy = rng.nextFloat() * SIZE
            val r = 40f + rng.nextFloat() * 160f
            paint.setShadowLayer(8f, 2f, 2f, Color.argb(160, 0, 0, 0))
            paint.shader = RadialGradient(
                cx, cy, r,
                intArrayOf(
                    Color.argb(255, rng.nextInt(256), rng.nextInt(256), rng.nextInt(256)),
                    Color.argb(100, rng.nextInt(256), rng.nextInt(256), rng.nextInt(256)),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP
            )
            paint.style = Paint.Style.FILL
            canvas.drawCircle(cx, cy, r, paint)
            paint.shader = null

            // 4. Matrix-transformed rounded rectangle
            paint.clearShadowLayer()
            matrix.reset()
            matrix.postRotate(angle, SIZE / 2f, SIZE / 2f)
            matrix.postScale(0.8f + rng.nextFloat() * 0.4f, 0.8f + rng.nextFloat() * 0.4f,
                SIZE / 2f, SIZE / 2f)
            canvas.save()
            canvas.concat(matrix)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f
            paint.color = Color.argb(180, rng.nextInt(256), rng.nextInt(256), rng.nextInt(256))
            canvas.drawRoundRect(SIZE * 0.2f, SIZE * 0.2f, SIZE * 0.8f, SIZE * 0.8f, 24f, 24f, paint)
            canvas.restore()

            // 5. Anti-aliased text overlay
            paint.style = Paint.Style.FILL
            paint.color = Color.WHITE
            paint.textSize = 48f
            paint.clearShadowLayer()
            canvas.drawText("Op #%06d".format(ops), 32f, SIZE * 0.92f, paint)

            ops++
            if (ops % 50L == 0L) {
                _previewBitmap.value = Bitmap.createScaledBitmap(bmp, 384, 384, false)
                liveDetail = "Op #$ops  •  ${SIZE}×${SIZE}px  •  5 layers/op + shadow"
            }
        }

        bmp.recycle()
        return if (ops == 0L) 0.0 else ops.toDouble() / (durationMs / 1000.0)
    }

    // ── 2. Image Filter (HARD) ────────────────────────────────────────────
    /**
     * 3-pass ColorMatrix pipeline on full 4K per "image":
     *   Pass 1 — brightness + per-channel contrast (Canvas.drawBitmap via ColorMatrixColorFilter)
     *   Pass 2 — saturation shift
     *   Pass 3 — hue rotation (full 5×4 YUV approximation matrix)
     * Three allocations ping-ponged: src → mid → dst → src.
     * 3× the pixel work of a single-pass filter.
     */
    private fun benchImageFilter(durationMs: Long): Double {
        val W = 3840; val H = 2160
        val src = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val mid = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val dst = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)

        val srcCanvas = Canvas(src)
        val sp = Paint()
        val rng = Random(99L)
        for (band in 0 until 12) {
            sp.color = Color.HSVToColor(floatArrayOf(band * 30f, 0.85f, 0.9f))
            srcCanvas.drawRect(0f, band * H / 12f, W.toFloat(), (band + 1) * H / 12f, sp)
        }
        for (i in 0 until 120) {
            sp.color = Color.argb(100 + rng.nextInt(120),
                rng.nextInt(256), rng.nextInt(256), rng.nextInt(256))
            srcCanvas.drawCircle(rng.nextFloat() * W, rng.nextFloat() * H,
                80f + rng.nextFloat() * 500f, sp)
        }

        val cMid = Canvas(mid); val cDst = Canvas(dst); val cSrc = Canvas(src)
        val paint = Paint()
        val cm1 = ColorMatrix(); val cm2 = ColorMatrix(); val cm3 = ColorMatrix()
        var images = 0L
        val endMs = System.currentTimeMillis() + durationMs

        while (System.currentTimeMillis() < endMs) {
            val t = images.toFloat()

            // Pass 1: brightness + contrast
            val br = 0.7f + (t % 50f) * 0.008f
            cm1.setScale(br * 1.1f, br * 0.95f, br * 1.05f, 1f)
            paint.colorFilter = ColorMatrixColorFilter(cm1)
            cMid.drawBitmap(src, 0f, 0f, paint)

            // Pass 2: saturation
            val sat = 0.4f + (t % 80f) * 0.008f
            cm2.setSaturation(sat.coerceIn(0f, 2f))
            paint.colorFilter = ColorMatrixColorFilter(cm2)
            cDst.drawBitmap(mid, 0f, 0f, paint)

            // Pass 3: hue rotation matrix (YUV approx)
            val hueRad = (t % 360f) * (Math.PI / 180.0).toFloat()
            val cos = kotlin.math.cos(hueRad.toDouble()).toFloat()
            val sin = kotlin.math.sin(hueRad.toDouble()).toFloat()
            cm3.set(floatArrayOf(
                0.213f + cos * 0.787f - sin * 0.213f,
                0.715f - cos * 0.715f - sin * 0.715f,
                0.072f - cos * 0.072f + sin * 0.928f, 0f, 0f,
                0.213f - cos * 0.213f + sin * 0.143f,
                0.715f + cos * 0.285f + sin * 0.140f,
                0.072f - cos * 0.072f - sin * 0.283f, 0f, 0f,
                0.213f - cos * 0.213f - sin * 0.787f,
                0.715f - cos * 0.715f + sin * 0.715f,
                0.072f + cos * 0.928f + sin * 0.072f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
            paint.colorFilter = ColorMatrixColorFilter(cm3)
            cSrc.drawBitmap(dst, 0f, 0f, paint)  // ping back to src

            images++
            if (images % 2L == 0L) {
                _previewBitmap.value = Bitmap.createScaledBitmap(dst, 384, 216, false)
                liveDetail = "3-pass filter #$images  •  ${W}×${H}  •  pass1/2/3 done"
            }
        }

        src.recycle(); mid.recycle(); dst.recycle()
        return if (images == 0L) 0.0 else images.toDouble() / (durationMs / 1000.0)
    }

    // ── 3. Image Resize (HARD) ────────────────────────────────────────────
    /**
     * Full round-trip resize per iteration:
     *   Step A: 3840×2160 → 1920×1080  (Skia bilinear, filter=true)
     *   Step B: 1920×1080 → 3840×2160  (upscale back, filter=true)
     * Upscale forces every output pixel to sample 4 source pixels.
     * Measures round-trip pairs (down+up) per second.
     */
    private fun benchImageResize(durationMs: Long): Double {
        val fullW = 3840; val fullH = 2160
        val halfW = 1920; val halfH = 1080
        val src = Bitmap.createBitmap(fullW, fullH, Bitmap.Config.ARGB_8888)

        val c = Canvas(src); val p = Paint(); val rng = Random(7L)
        for (band in 0 until 16) {
            p.color = Color.HSVToColor(floatArrayOf(band * 22.5f, 0.85f, 0.92f))
            c.drawRect(0f, band * fullH / 16f, fullW.toFloat(), (band + 1) * fullH / 16f, p)
        }
        for (i in 0 until 300) {
            p.color = Color.argb(140 + rng.nextInt(100),
                rng.nextInt(256), rng.nextInt(256), rng.nextInt(256))
            c.drawOval(rng.nextFloat() * fullW - 150f, rng.nextFloat() * fullH - 150f,
                rng.nextFloat() * fullW + 150f, rng.nextFloat() * fullH + 150f, p)
        }

        var images = 0L
        val endMs = System.currentTimeMillis() + durationMs

        while (System.currentTimeMillis() < endMs) {
            val half = Bitmap.createScaledBitmap(src, halfW, halfH, true)   // 4K→1080p
            val up   = Bitmap.createScaledBitmap(half, fullW, fullH, true)  // 1080p→4K
            half.recycle()
            images++
            if (images % 2L == 0L) {
                _previewBitmap.value = Bitmap.createScaledBitmap(up, 384, 216, false)
                liveDetail = "Round-trip #$images  •  4K→1080p→4K  •  bilinear both ways"
            }
            up.recycle()
        }

        src.recycle()
        return if (images == 0L) 0.0 else images.toDouble() / (durationMs / 1000.0)
    }

    // ── Video Encode (HARD) ───────────────────────────────────────────────
    /**
     * Upgrades from 1920×1080 to 3840×2160 (4K UHD) per frame:
     *   1. Render 16-band HSV sweep + 80 radial-gradient circles + diagonal stripes
     *   2. Compress at JPEG quality 85 — ~1–2 MB per 4K frame (8× data vs 1080p)
     * Measures 4K frames per second.
     */
    private fun benchVideoEncode(durationMs: Long): Double {
        val W = 3840; val H = 2160
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val frameCanvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rng = Random(42L)
        val out = ByteArrayOutputStream(W * H / 2)
        var frames = 0L
        val endMs = System.currentTimeMillis() + durationMs

        while (System.currentTimeMillis() < endMs) {
            val hueShift = (frames * 1.2f) % 360f
            // 16 HSV colour bands
            for (band in 0 until 16) {
                paint.color = Color.HSVToColor(floatArrayOf((hueShift + band * 22.5f) % 360f, 0.88f, 0.92f))
                paint.style = Paint.Style.FILL; paint.shader = null
                frameCanvas.drawRect(0f, band * H / 16f, W.toFloat(), (band + 1) * H / 16f, paint)
            }
            // 80 radial-gradient circles
            for (i in 0 until 80) {
                val cx = rng.nextFloat() * W; val cy = rng.nextFloat() * H
                val r = 100f + rng.nextFloat() * 600f
                val c1 = Color.HSVToColor(floatArrayOf((hueShift + i * 4.5f) % 360f, 0.9f, 1.0f))
                paint.shader = RadialGradient(cx, cy, r,
                    intArrayOf(c1, Color.TRANSPARENT), null, Shader.TileMode.CLAMP)
                paint.style = Paint.Style.FILL
                frameCanvas.drawCircle(cx, cy, r, paint)
            }
            paint.shader = null
            // Diagonal stripes
            paint.color = Color.argb(50, 0, 0, 0); paint.strokeWidth = 12f
            paint.style = Paint.Style.STROKE
            val offset = (frames % 80L * 20L).toFloat()
            var x = -H.toFloat() + offset
            while (x < W.toFloat()) { frameCanvas.drawLine(x, 0f, x + H.toFloat(), H.toFloat(), paint); x += 60f }
            // Counter
            paint.style = Paint.Style.FILL; paint.shader = null
            paint.color = Color.argb(210, 0, 0, 0)
            frameCanvas.drawRect(80f, H * 0.44f, 780f, H * 0.60f, paint)
            paint.color = Color.WHITE; paint.textSize = 96f
            frameCanvas.drawText("4K Frame %06d".format(frames), 100f, H * 0.57f, paint)
            // Compress
            out.reset(); bmp.compress(Bitmap.CompressFormat.JPEG, 85, out)
            frames++
            if (frames % 2L == 0L) {
                _previewBitmap.value = Bitmap.createScaledBitmap(bmp, 384, 216, false)
                liveDetail = "4K Encode #$frames  •  ${W}×${H}  •  ${out.size() / 1024}KB/frame"
            }
        }
        bmp.recycle()
        return if (frames == 0L) 0.0 else frames.toDouble() / (durationMs / 1000.0)
    }

    // ── 7. Video Decode (HARD) ────────────────────────────────────────────
    /**
     * Pre-encodes 24 × 1920×1080 JPEG keyframes at quality 100
     * (max data ~800KB–1.2MB each vs ~400KB at q85).
     * Quality 100 forces the JPEG decoder to handle more Huffman codes + DCT
     * coefficients per frame, making decode ~30–40% slower than q85.
     * Measures decoded frames per second.
     */
    private fun benchVideoDecode(durationMs: Long): Double {
        val W = 1920; val H = 1080
        val keyframeCount = 24

        val encoded = Array(keyframeCount) { fi ->
            val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp); val p = Paint(Paint.ANTI_ALIAS_FLAG)
            val rng = Random(fi.toLong() * 1337L)
            val hueBase = fi * 360f / keyframeCount
            for (band in 0 until 16) {
                val c1 = Color.HSVToColor(floatArrayOf((hueBase + band * 22.5f) % 360f, 0.9f, 0.95f))
                val c2 = Color.HSVToColor(floatArrayOf((hueBase + band * 22.5f + 30f) % 360f, 0.7f, 0.75f))
                p.shader = android.graphics.LinearGradient(0f, band * H / 16f, W.toFloat(), (band + 1) * H / 16f,
                    c1, c2, Shader.TileMode.CLAMP)
                c.drawRect(0f, band * H / 16f, W.toFloat(), (band + 1) * H / 16f, p)
            }
            p.shader = null
            for (s in 0 until 40) {
                p.color = Color.argb(120 + rng.nextInt(120), rng.nextInt(256), rng.nextInt(256), rng.nextInt(256))
                p.style = Paint.Style.FILL
                c.drawOval(rng.nextFloat() * W - 80f, rng.nextFloat() * H - 80f,
                    rng.nextFloat() * W + 80f, rng.nextFloat() * H + 80f, p)
            }
            val out = ByteArrayOutputStream(1024 * 1024)
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, out)  // q100 = max file size
            bmp.recycle()
            out.toByteArray()
        }

        var frames = 0L
        val endMs = System.currentTimeMillis() + durationMs
        while (System.currentTimeMillis() < endMs) {
            val idx = (frames % keyframeCount).toInt()
            val bytes = encoded[idx]
            val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            frames++
            if (frames % 5L == 0L && decoded != null && !decoded.isRecycled) {
                _previewBitmap.value = Bitmap.createScaledBitmap(decoded, 384, 216, false)
                liveDetail = "q100 decode #$frames  •  ${W}×${H}  •  ${bytes.size / 1024}KB JPEG"
            }
            decoded?.recycle()
        }
        return if (frames == 0L) 0.0 else frames.toDouble() / (durationMs / 1000.0)
    }

    // ── 8. Video Transcode (HARD) ─────────────────────────────────────────
    /**
     * Per frame pipeline: 4K→1080p with 3-pass colour grade:
     *   1. Decode 3840×2160 q100 JPEG keyframe
     *   2. Scale 4K→1080p  (bilinear)
     *   3. Grade pass 1: brightness+contrast ColorMatrix
     *   4. Grade pass 2: saturation ColorMatrix
     *   5. Grade pass 3: hue rotation matrix
     *   6. Encode 1080p output JPEG quality 95
     * Decode ~2MB + resize 8M→2M pixels + 3 full-frame passes + encode ~600KB.
     */
    private fun benchVideoTranscode(durationMs: Long): Double {
        val srcW = 3840; val srcH = 2160; val dstW = 1920; val dstH = 1080
        val keyframeCount = 8

        val encoded = Array(keyframeCount) { fi ->
            val bmp = Bitmap.createBitmap(srcW, srcH, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp); val p = Paint(Paint.ANTI_ALIAS_FLAG)
            val rng = Random(fi.toLong() * 2233L)
            val hueBase = fi * 45f
            for (band in 0 until 16) {
                p.color = Color.HSVToColor(floatArrayOf((hueBase + band * 22.5f) % 360f, 0.88f, 0.93f))
                c.drawRect(0f, band * srcH / 16f, srcW.toFloat(), (band + 1) * srcH / 16f, p)
            }
            for (s in 0 until 60) {
                p.color = Color.argb(100 + rng.nextInt(130), rng.nextInt(256), rng.nextInt(256), rng.nextInt(256))
                c.drawOval(rng.nextFloat() * srcW - 200f, rng.nextFloat() * srcH - 200f,
                    rng.nextFloat() * srcW + 200f, rng.nextFloat() * srcH + 200f, p)
            }
            p.textSize = 120f; p.color = Color.WHITE; p.shader = null
            c.drawText("4K FRAME %02d".format(fi), 80f, srcH * 0.54f, p)
            val out = ByteArrayOutputStream(srcW * srcH / 2)
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, out)
            bmp.recycle(); out.toByteArray()
        }

        val gradeA = Bitmap.createBitmap(dstW, dstH, Bitmap.Config.ARGB_8888)
        val gradeB = Bitmap.createBitmap(dstW, dstH, Bitmap.Config.ARGB_8888)
        val cA = Canvas(gradeA); val cB = Canvas(gradeB)
        val gp = Paint()
        val cm1 = ColorMatrix(); val cm2 = ColorMatrix(); val cm3 = ColorMatrix()
        val outBuf = ByteArrayOutputStream(dstW * dstH / 3)
        var frames = 0L
        val endMs = System.currentTimeMillis() + durationMs

        while (System.currentTimeMillis() < endMs) {
            val idx = (frames % keyframeCount).toInt()
            val bytes = encoded[idx]
            val src4k = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: continue
            val scaled = Bitmap.createScaledBitmap(src4k, dstW, dstH, true)
            src4k.recycle()

            val t = frames.toFloat()
            // Grade 1: brightness
            val br = 0.75f + (t % 60f) * 0.005f
            cm1.setScale(br * 1.1f, br, br * 0.95f, 1f)
            gp.colorFilter = ColorMatrixColorFilter(cm1)
            cA.drawBitmap(scaled, 0f, 0f, gp)
            scaled.recycle()
            // Grade 2: saturation
            val sat = 0.6f + (t % 90f) * 0.006f
            cm2.setSaturation(sat.coerceIn(0.4f, 1.8f))
            gp.colorFilter = ColorMatrixColorFilter(cm2)
            cB.drawBitmap(gradeA, 0f, 0f, gp)
            // Grade 3: hue rotation
            val hueRad = (t % 360f) * (Math.PI / 180.0).toFloat()
            val cos = kotlin.math.cos(hueRad.toDouble()).toFloat()
            val sin = kotlin.math.sin(hueRad.toDouble()).toFloat()
            cm3.set(floatArrayOf(
                0.213f + cos * 0.787f - sin * 0.213f,
                0.715f - cos * 0.715f - sin * 0.715f,
                0.072f - cos * 0.072f + sin * 0.928f, 0f, 0f,
                0.213f - cos * 0.213f + sin * 0.143f,
                0.715f + cos * 0.285f + sin * 0.140f,
                0.072f - cos * 0.072f - sin * 0.283f, 0f, 0f,
                0.213f - cos * 0.213f - sin * 0.787f,
                0.715f - cos * 0.715f + sin * 0.715f,
                0.072f + cos * 0.928f + sin * 0.072f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
            gp.colorFilter = ColorMatrixColorFilter(cm3)
            cA.drawBitmap(gradeB, 0f, 0f, gp)  // final in gradeA
            // Encode q95
            outBuf.reset(); gradeA.compress(Bitmap.CompressFormat.JPEG, 95, outBuf)
            frames++
            if (frames % 2L == 0L && !gradeA.isRecycled) {
                _previewBitmap.value = Bitmap.createScaledBitmap(gradeA, 384, 216, false)
                liveDetail = "4K→1080p transcode #$frames  •  3-pass grade  •  ${outBuf.size() / 1024}KB out"
            }
        }

        gradeA.recycle(); gradeB.recycle()
        return if (frames == 0L) 0.0 else frames.toDouble() / (durationMs / 1000.0)
    }

    // ── 4. Text Processing (HARD) ─────────────────────────────────────────
    /**
     * 50 000-word corpus. Per pass:
     *   1. Sort entire corpus (Timsort on String[] × 50K elements)
     *   2. Levenshtein edit-distance for 200 random pairs (O(m×n) DP per pair)
     *   3. Regex replace with 5 compiled patterns over a 500-word join
     * ~6× more words + expensive DP + regex vs. the easy version.
     */
    private fun benchTextOps(durationMs: Long): Double {
        val wordCount = 50_000
        val rng = Random(54321L)
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val corpus = Array(wordCount) {
            val len = 3 + rng.nextInt(12)
            (0 until len).map { chars[rng.nextInt(chars.length)] }.joinToString("")
        }
        val pairsA = Array(200) { corpus[rng.nextInt(wordCount)] }
        val pairsB = Array(200) { corpus[rng.nextInt(wordCount)] }
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

            // 2. Levenshtein for 200 pairs
            var levenSum = 0L
            for (k in 0 until 200) {
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
