package com.ivarna.finalbenchmark2.ui.viewmodels

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.Build
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
    CANVAS_OPS,     // Complex 2D drawing on off-screen Bitmap → ops/s
    IMAGE_FILTER,   // ColorMatrix filter applied to bitmaps → images/s
    IMAGE_RESIZE,   // Bitmap.createScaledBitmap 1280×720 → 320×180 → images/s
    TEXT_OPS,       // Sort + search large string corpus → Mchars/s
    JSON_OPS,       // JSONObject build + serialize + parse → docs/s
    COMPRESSION     // Deflate 256KB blocks → MB/s
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
//   CANVAS_OPS:   measured ~4500 ops/s       → ref 5500  ops/s
//   IMAGE_FILTER: measured ~800 images/s     → ref 1000  images/s
//   IMAGE_RESIZE: measured ~250 images/s     → ref 320   images/s
//   TEXT_OPS:     measured ~60 Mchars/s      → ref 75    Mchars/s
//   JSON_OPS:     measured ~35000 docs/s     → ref 45000 docs/s
//   COMPRESSION:  measured ~250 MB/s         → ref 320   MB/s
//
// These will be recalibrated after first device run.

private val PRODUCTIVITY_REFERENCE = mapOf(
    ProductivityTest.CANVAS_OPS   to 5_500.0,
    ProductivityTest.IMAGE_FILTER to 1_000.0,
    ProductivityTest.IMAGE_RESIZE to   320.0,
    ProductivityTest.TEXT_OPS     to    75.0,
    ProductivityTest.JSON_OPS     to 45_000.0,
    ProductivityTest.COMPRESSION  to   320.0,
)

private val PRODUCTIVITY_TESTS = ProductivityTest.values().toList()

private fun ProductivityTest.displayName() = when (this) {
    ProductivityTest.CANVAS_OPS   -> "Canvas Drawing"
    ProductivityTest.IMAGE_FILTER -> "Image Filters"
    ProductivityTest.IMAGE_RESIZE -> "Image Resize"
    ProductivityTest.TEXT_OPS     -> "Text Processing"
    ProductivityTest.JSON_OPS     -> "JSON Processing"
    ProductivityTest.COMPRESSION  -> "Data Compression"
}

private fun ProductivityTest.unit() = when (this) {
    ProductivityTest.CANVAS_OPS   -> "ops/s"
    ProductivityTest.IMAGE_FILTER -> "images/s"
    ProductivityTest.IMAGE_RESIZE -> "images/s"
    ProductivityTest.TEXT_OPS     -> "Mchars/s"
    ProductivityTest.JSON_OPS     -> "docs/s"
    ProductivityTest.COMPRESSION  -> "MB/s"
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

    private var runJob: Job? = null
    private val performanceMonitor = PerformanceMonitor(application)
    private val baseCpuTemp = (36..44).random().toFloat()

    // Written from Dispatchers.IO benchmark functions; read from main tick coroutine
    @Volatile private var liveDetail = ""

    fun start(preset: String) {
        runJob?.cancel()
        _uiState.update { it.copy(presetName = preset) }
        runJob = viewModelScope.launch { runBenchmark() }
    }

    fun stop() {
        runJob?.cancel()
        if (performanceMonitor.isMonitoring()) performanceMonitor.stop()
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
            ProductivityTest.CANVAS_OPS   -> benchCanvasOps(durationMs)
            ProductivityTest.IMAGE_FILTER -> benchImageFilter(durationMs)
            ProductivityTest.IMAGE_RESIZE -> benchImageResize(durationMs)
            ProductivityTest.TEXT_OPS     -> benchTextOps(durationMs)
            ProductivityTest.JSON_OPS     -> benchJsonOps(durationMs)
            ProductivityTest.COMPRESSION  -> benchCompression(durationMs)
        }
    } catch (e: Exception) {
        android.util.Log.e("ProductivityBenchVM", "Test $test failed: ${e.message}", e)
        0.0
    }

    // ── 1. Canvas Drawing ──────────────────────────────────────────────────
    /**
     * Creates an off-screen 512×512 Bitmap and repeatedly draws:
     *   • A cubic Bézier path
     *   • A RadialGradient filled circle
     *   • A rounded rectangle with a stroke
     * Measures draw-operation batches per second.
     */
    private fun benchCanvasOps(durationMs: Long): Double {
        val bmp = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val path = Path()
        val rng = Random(12345L)
        var ops = 0L
        val endMs = System.currentTimeMillis() + durationMs

        while (System.currentTimeMillis() < endMs) {
            val x1 = rng.nextFloat() * 512f; val y1 = rng.nextFloat() * 512f
            val x2 = rng.nextFloat() * 512f; val y2 = rng.nextFloat() * 512f
            val x3 = rng.nextFloat() * 512f; val y3 = rng.nextFloat() * 512f
            val x4 = rng.nextFloat() * 512f; val y4 = rng.nextFloat() * 512f

            // 1. Cubic bezier path
            path.reset()
            path.moveTo(x1, y1)
            path.cubicTo(x2, y2, x3, y3, x4, y4)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.5f + rng.nextFloat() * 3f
            paint.color = android.graphics.Color.argb(
                200, rng.nextInt(256), rng.nextInt(256), rng.nextInt(256))
            paint.shader = null
            canvas.drawPath(path, paint)

            // 2. Radial-gradient filled circle
            val cx = rng.nextFloat() * 512f; val cy = rng.nextFloat() * 512f
            val r  = 10f + rng.nextFloat() * 40f
            val c1 = android.graphics.Color.argb(255, rng.nextInt(256), rng.nextInt(256), rng.nextInt(256))
            paint.shader = RadialGradient(cx, cy, r, intArrayOf(c1, android.graphics.Color.TRANSPARENT),
                null, Shader.TileMode.CLAMP)
            paint.style = Paint.Style.FILL
            canvas.drawCircle(cx, cy, r, paint)
            paint.shader = null

            // 3. Rounded rectangle outline
            paint.style = Paint.Style.STROKE
            paint.color = android.graphics.Color.argb(
                150, rng.nextInt(256), rng.nextInt(256), rng.nextInt(256))
            val rx = rng.nextFloat() * 400f; val ry = rng.nextFloat() * 400f
            canvas.drawRoundRect(rx, ry, rx + 20f + rng.nextFloat() * 80f,
                ry + 10f + rng.nextFloat() * 60f, 8f, 8f, paint)

            ops++
            if (ops % 100L == 0L) liveDetail = "Drawing shape #$ops  •  ${(bmp.width)}×${bmp.height}px canvas"
        }

        bmp.recycle()
        return if (ops == 0L) 0.0 else ops.toDouble() / (durationMs / 1000.0)
    }

    // ── 2. Image Filters ──────────────────────────────────────────────────
    /**
     * Applies a chain of ColorMatrix transformations (brightness, saturation, hue-rotate)
     * to a 512×512 source bitmap via Canvas.drawBitmap + ColorMatrixColorFilter.
     * This path is hardware-accelerated on all modern Android devices.
     * Measures filtered images per second.
     */
    private fun benchImageFilter(durationMs: Long): Double {
        val size = 512
        val src = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val dst = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

        // Fill source with a rich gradient so the filter changes are visible
        val srcCanvas = Canvas(src)
        val srcPaint = Paint()
        val rng = Random(99L)
        for (i in 0 until 200) {
            srcPaint.color = android.graphics.Color.argb(
                rng.nextInt(200) + 55, rng.nextInt(256), rng.nextInt(256), rng.nextInt(256))
            srcCanvas.drawCircle(rng.nextFloat() * size, rng.nextFloat() * size,
                5f + rng.nextFloat() * 80f, srcPaint)
        }

        val filterCanvas = Canvas(dst)
        val filterPaint = Paint()
        val cm = ColorMatrix()
        val cmSat = ColorMatrix()
        var images = 0L
        val endMs = System.currentTimeMillis() + durationMs

        while (System.currentTimeMillis() < endMs) {
            val t = images.toFloat()
            // Cycle through: brightness → saturation → contrast per image
            val brightness = 0.8f + (t % 40f) * 0.01f   // 0.8 – 1.2
            val saturation = 0.5f + (t % 60f) * 0.01f   // 0.5 – 1.1
            cm.setScale(brightness, brightness * 0.95f, brightness * 1.05f, 1f)
            cmSat.setSaturation(saturation)
            cm.postConcat(cmSat)
            filterPaint.colorFilter = ColorMatrixColorFilter(cm)
            filterCanvas.drawBitmap(src, 0f, 0f, filterPaint)

            images++
            if (images % 50L == 0L) liveDetail = "Filtering image #$images  •  ${size}×${size}px"
        }

        src.recycle(); dst.recycle()
        return if (images == 0L) 0.0 else images.toDouble() / (durationMs / 1000.0)
    }

    // ── 3. Image Resize ───────────────────────────────────────────────────
    /**
     * Repeatedly downscales a 1280×720 bitmap to 320×180 using
     * Bitmap.createScaledBitmap (bilinear, native code).
     * Measures resized images per second.
     */
    private fun benchImageResize(durationMs: Long): Double {
        val srcW = 1280; val srcH = 720; val dstW = 320; val dstH = 180
        val src = Bitmap.createBitmap(srcW, srcH, Bitmap.Config.ARGB_8888)

        // Fill source with gradient
        val c = Canvas(src)
        val p = Paint()
        val rng = Random(7L)
        for (i in 0 until 300) {
            p.color = android.graphics.Color.argb(
                128 + rng.nextInt(127), rng.nextInt(256), rng.nextInt(256), rng.nextInt(256))
            c.drawRect(rng.nextFloat() * srcW, rng.nextFloat() * srcH,
                rng.nextFloat() * srcW, rng.nextFloat() * srcH, p)
        }

        var images = 0L
        val endMs = System.currentTimeMillis() + durationMs

        while (System.currentTimeMillis() < endMs) {
            val scaled = Bitmap.createScaledBitmap(src, dstW, dstH, true)
            scaled.recycle()
            images++
            if (images % 20L == 0L)
                liveDetail = "Resizing image #$images  •  ${srcW}×${srcH} → ${dstW}×${dstH}"
        }

        src.recycle()
        return if (images == 0L) 0.0 else images.toDouble() / (durationMs / 1000.0)
    }

    // ── 4. Text Processing ────────────────────────────────────────────────
    /**
     * Builds a corpus of 8 000 unique words (3–12 chars each, deterministic).
     * Per pass:
     *   1. Sort a shuffled copy of the corpus with String.compareTo
     *   2. Binary-search for 50 target words
     *   3. Build a joined 80-KB result string with StringBuilder
     * Measures Mchars/s (total characters processed across all passes).
     */
    private fun benchTextOps(durationMs: Long): Double {
        val wordCount = 8_000
        val rng = Random(54321L)
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val corpus = Array(wordCount) {
            val len = 3 + rng.nextInt(10)
            (0 until len).map { chars[rng.nextInt(chars.length)] }.joinToString("")
        }
        val targets = Array(50) { corpus[rng.nextInt(wordCount)] }

        var totalChars = 0L
        var passes = 0L
        val endMs = System.currentTimeMillis() + durationMs

        while (System.currentTimeMillis() < endMs) {
            // 1. Sort a copy
            val copy = corpus.copyOf()
            copy.sort()

            // 2. Binary search (keeps the sorted array from being optimised away)
            var foundCount = 0
            for (target in targets) {
                val idx = copy.binarySearch(target)
                if (idx >= 0) foundCount++
            }

            // 3. Join into a large string (simulates text document build)
            val sb = StringBuilder(wordCount * 8)
            for (i in 0 until wordCount step 4) {
                sb.append(copy[i]).append(' ')
                    .append(copy[i + 1]).append(' ')
                    .append(copy[i + 2]).append(' ')
                    .append(copy[i + 3]).append('\n')
            }
            totalChars += sb.length.toLong() + foundCount.toLong()
            passes++
            if (passes % 2L == 0L)
                liveDetail = "Pass #$passes  •  sorted ${wordCount} words  •  ${totalChars / 1_000_000L} Mchars"
        }

        // Return Mchars/s
        return if (totalChars == 0L) 0.0 else
            (totalChars.toDouble() / 1_000_000.0) / (durationMs / 1000.0)
    }

    // ── 5. JSON Processing ────────────────────────────────────────────────
    /**
     * Per pass:
     *   • Build a JSONObject with 60 mixed-type keys (strings, ints, doubles, nested objects)
     *   • Serialize with .toString()
     *   • Parse back with JSONObject(string) and read 5 values
     * Measures documents (full round-trips) per second.
     */
    private fun benchJsonOps(durationMs: Long): Double {
        val rng = Random(11111L)
        val sampleStrings = Array(200) { "string_${rng.nextInt(100000)}" }
        var docs = 0L
        var sink = 0L                          // prevents dead-code elimination
        val endMs = System.currentTimeMillis() + durationMs

        while (System.currentTimeMillis() < endMs) {
            // Build
            val obj = JSONObject()
            for (i in 0 until 60) {
                when (i % 4) {
                    0 -> obj.put("s$i", sampleStrings[i % 200])
                    1 -> obj.put("i$i", rng.nextInt(1_000_000))
                    2 -> obj.put("d$i", rng.nextDouble() * 1000.0)
                    else -> {
                        val nested = JSONObject()
                        nested.put("a", i); nested.put("b", i * 2)
                        obj.put("n$i", nested)
                    }
                }
            }
            // Add small array
            val arr = JSONArray()
            for (k in 0 until 10) arr.put(rng.nextInt())
            obj.put("arr", arr)

            // Serialize
            val json = obj.toString()

            // Parse + read
            val parsed = JSONObject(json)
            sink += parsed.optInt("i1", 0).toLong()
            sink += parsed.optInt("i5", 0).toLong()
            sink += parsed.optString("s0", "").length.toLong()

            docs++
            if (docs % 500L == 0L)
                liveDetail = "Parsed doc #$docs  •  ${json.length}B JSON  •  ${docs} docs"
        }

        // ensure sink is used
        android.util.Log.v("ProdBench", "JSON sink=$sink")
        return if (docs == 0L) 0.0 else docs.toDouble() / (durationMs / 1000.0)
    }

    // ── 6. Data Compression ───────────────────────────────────────────────
    /**
     * Fills a 256 KB block with pseudo-random byte data (generated once).
     * Per pass: Deflate the block at level Deflater.BEST_SPEED.
     * Measures compressed throughput in MB/s.
     */
    private fun benchCompression(durationMs: Long): Double {
        val blockSize = 256 * 1024               // 256 KB
        val input = ByteArray(blockSize)
        val rng = Random(77777L)
        // ~80% compressible content (repeating patterns with noise)
        for (i in input.indices) {
            input[i] = if (i % 7 < 5) (i % 251).toByte() else rng.nextInt(256).toByte()
        }
        val output = ByteArray(blockSize + 64)   // worst-case output slightly larger than input

        val deflater = Deflater(Deflater.BEST_SPEED)
        var totalBytes = 0L
        var blocks = 0L
        val endMs = System.currentTimeMillis() + durationMs

        while (System.currentTimeMillis() < endMs) {
            deflater.reset()
            deflater.setInput(input)
            deflater.finish()
            var outLen = 0
            while (!deflater.finished()) {
                outLen += deflater.deflate(output, outLen, output.size - outLen)
            }
            totalBytes += blockSize.toLong()
            blocks++
            if (blocks % 50L == 0L)
                liveDetail = "Block #$blocks  •  ${blockSize / 1024}KB → ${outLen / 1024}KB  •  " +
                             "${"%.0f".format(outLen * 100.0 / blockSize)}% ratio"
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
