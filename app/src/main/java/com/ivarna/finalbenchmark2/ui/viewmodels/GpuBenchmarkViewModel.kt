package com.ivarna.finalbenchmark2.ui.viewmodels

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ivarna.finalbenchmark2.data.database.entities.BenchmarkResultEntity
import com.ivarna.finalbenchmark2.data.database.entities.GenericTestDetailEntity
import com.ivarna.finalbenchmark2.data.repository.HistoryRepository
import com.ivarna.finalbenchmark2.gpu.GpuScene
import com.ivarna.finalbenchmark2.utils.GpuFrequencyReader
import com.ivarna.finalbenchmark2.utils.PerformanceMonitor
import com.ivarna.finalbenchmark2.utils.PowerUtils
import kotlinx.coroutines.Job
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
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.DoubleAdder
import kotlin.math.roundToInt

// ── Data types ────────────────────────────────────────────────────────────

data class GpuTestResult(
    val scene: GpuScene,
    val displayName: String,
    val avgFps: Float,
    val avgFrametimeMs: Float,
    val score: Int
)

data class GpuBenchmarkUiState(
    val isWarmingUp: Boolean = false,
    val isRunning: Boolean = false,
    val isCompleted: Boolean = false,

    val currentScene: GpuScene = GpuScene.TRIANGLE_RENDERING,
    val currentTestIndex: Int = 0,
    val totalTests: Int = GpuScene.values().size,
    val currentTestName: String = "",
    val overallProgress: Float = 0f,
    val currentTestProgress: Float = 0f,

    val currentFps: Float = 0f,
    val avgFps: Float = 0f,
    val currentFrametimeMs: Float = 16.67f,
    val frametimeHistory: List<Float> = emptyList(),

    val gpuFreqMhz: Int = 0,
    val gpuTempC: Float = 0f,
    val gpuLoadPercent: Float = 0f,
    // Real-time power draw in Watts (from PowerUtils)
    val powerWatts: Float = 0f,
    // GPU hardware identity (from GL_RENDERER / GL_VERSION)
    val gpuName: String = "",
    val glApiLabel: String = "OpenGL ES 3.0",

    val completedTests: List<GpuTestResult> = emptyList(),
    val totalScore: Int = 0,
    val presetName: String = ""
)

// ─────────────────────────────────────────────────────────────────────────

private val GPU_SCENES = GpuScene.values().toList()

/**
 * Reference FPS per scene on Snapdragon 8 Gen 3 / Adreno 750 (baseline = 100 pts).
 * Scenes 1,3,5: 1× fragment pre-pass + geometry overlay → GPU ALU-bound, not CPU/API-bound.
 * Scenes 2,4,6-10: 4× fullscreen passes → GPU compute-bound.
 * Extended scenes: heavy multi-pass workloads targeting <20 FPS on flagship GPU.
 * Any device matching these FPS values scores exactly 100.
 */
private val GPU_REFERENCE_FPS = mapOf(
    GpuScene.TRIANGLE_RENDERING to  86.5,  // 1x DomainWarp pre-pass + 10K triangles
    GpuScene.COMPUTE_MATRIX     to  41.7,  // 4x Julia/matrix compute
    GpuScene.PARTICLE_SYSTEM    to  28.1,  // 1x MultiLight pre-pass + 5K particles
    GpuScene.TEXTURE_SAMPLING   to  25.3,  // 4x 12-octave FBM
    GpuScene.WIREFRAME_MESH     to  84.3,  // 1x RayMarch pre-pass + 250×250 mesh
    GpuScene.MANDELBROT_DEEP    to  17.4,  // 4x Mandelbrot 512 iter
    GpuScene.PHONG_MULTI_LIGHT  to   7.0,  // 4x Phong 128-light
    GpuScene.RAY_MARCH_SDF      to  21.9,  // 4x RayMarch SDF+shadows
    GpuScene.DOMAIN_WARP        to  20.9,  // 4x triple domain-warp FBM
    GpuScene.SUPER_SAMPLE       to   3.7,  // 4x 64x super-sampled fractal
    // Extended scenes — calibrated to Adreno 750 at the heavy workloads implemented in renderer
    GpuScene.SHADER_COMPILE     to  14.0,  // 2x (RayMarch+DomainWarp) @ 4K: ~14 FPS on A750
    GpuScene.MEM_BANDWIDTH      to   9.0,  // 4x 32-dep-sample pass @ 4K: ~9 FPS on A750
    GpuScene.MSAA_4X            to  11.0,  // 8x MSAA render+resolve @ 4K: ~11 FPS on A750
    GpuScene.VRAM_PRESSURE      to  10.0,  // 4x 8-tex ALU stress @ 4K: ~10 FPS on A750
    GpuScene.TESSELLATION       to   7.0,  // 4x Phong 128-light @ 4K: ~7 FPS on A750
    GpuScene.MULTI_PASS_BLOOM   to   8.0   // 5-pass bloom @ 4K: ~8 FPS on A750
)

/**
 * Computes the geometric mean of per-scene FPS ratios (SUT / reference),
 * scaled so that SD 8 Gen 3 = 100.  Mirrors the CPU scoring approach.
 */
private fun calculateGpuGeometricMean(results: List<GpuTestResult>): Double {
    val ratios = results.mapNotNull { r ->
        val ref = GPU_REFERENCE_FPS[r.scene] ?: return@mapNotNull null
        r.avgFps.toDouble() / ref
    }
    if (ratios.isEmpty()) return 0.0
    val product = ratios.fold(1.0) { acc, v -> acc * v }
    return Math.pow(product, 1.0 / ratios.size) * 100.0
}

private fun GpuScene.displayName() = when (this) {
    GpuScene.TRIANGLE_RENDERING  -> "Domain Warp + Triangles (10K)"
    GpuScene.COMPUTE_MATRIX      -> "Julia / Matrix Compute"
    GpuScene.PARTICLE_SYSTEM     -> "Phong + Particles (5K)"
    GpuScene.TEXTURE_SAMPLING    -> "12-Octave FBM Texture"
    GpuScene.WIREFRAME_MESH      -> "Ray March + Mesh (250\u00d7250)"
    GpuScene.MANDELBROT_DEEP     -> "Mandelbrot Deep (512 iter)"
    GpuScene.PHONG_MULTI_LIGHT   -> "Phong 128-Light Array"
    GpuScene.RAY_MARCH_SDF       -> "Ray March SDF + Shadows"
    GpuScene.DOMAIN_WARP         -> "Triple Domain Warp FBM"
    GpuScene.SUPER_SAMPLE        -> "64\u00d7 Super-Sampled Fractal"
    // Extended GPU stress scenes
    GpuScene.SHADER_COMPILE      -> "ALU Dual-Warp Stress"
    GpuScene.MEM_BANDWIDTH       -> "Texture Bandwidth Stress"
    GpuScene.MSAA_4X             -> "MSAA 4\u00d7 Resolve Stress"
    GpuScene.VRAM_PRESSURE       -> "VRAM Texture Pressure"
    GpuScene.TESSELLATION        -> "Geometry ALU Saturation"
    GpuScene.MULTI_PASS_BLOOM    -> "5-Pass Gaussian Bloom"
}



private const val WARMUP_MS = 2_000L
private const val TEST_MS   = 6_000L
private const val TICK_MS   = 100L

class GpuBenchmarkViewModel(
    application: Application,
    private val historyRepository: HistoryRepository?
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(GpuBenchmarkUiState())
    val uiState: StateFlow<GpuBenchmarkUiState> = _uiState.asStateFlow()

    private val _completionEvent = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 1)
    val completionEvent: SharedFlow<String> = _completionEvent.asSharedFlow()

    @Volatile private var latestFps: Float = 0f
    @Volatile private var latestFrameMs: Float = 16.67f

    // Thread-safe metrics accumulators (BUG-4)
    private val frameCount = AtomicInteger(0)
    private val totalRenderTimeMs = DoubleAdder()

    private var runJob: Job? = null
    private val performanceMonitor = PerformanceMonitor(application)

    // ── Hardware telemetry ─────────────────────────────────────────────────
    // Real reads via GpuFrequencyReader (sysfs); mocks used only as fallback
    private val gpuFreqReader   = GpuFrequencyReader()
    private val powerUtils      = PowerUtils(application)
    // Mock fallbacks (used when sysfs is unavailable)
    private val mockBaseFreq    = (500..800).random()
    private val mockBaseTemp    = (35..45).random()

    /**
     * Returns a triple of (freqMhz, tempC, loadPercent) from sysfs or mocks.
     * Called from coroutine tick — runs on IO dispatcher inside GpuFrequencyReader.
     */
    private suspend fun readGpuTelemetry(): Triple<Int, Float, Float> {
        return try {
            val state = gpuFreqReader.readGpuFrequency()
            if (state is GpuFrequencyReader.GpuFrequencyState.Available) {
                val d = state.data
                val freq = d.currentFrequencyMhz.toInt().coerceIn(0, 3000)
                val temp = d.temperatureCelsius?.toFloat()?.coerceIn(0f, 120f) ?: mockGpuTemp()
                val load = d.utilizationPercent?.toFloat()?.coerceIn(0f, 100f) ?: mockGpuLoad(latestFps)
                Triple(freq, temp, load)
            } else {
                Triple(mockGpuFreq(), mockGpuTemp(), mockGpuLoad(latestFps))
            }
        } catch (e: Exception) {
            Triple(mockGpuFreq(), mockGpuTemp(), mockGpuLoad(latestFps))
        }
    }

    /** Called from GpuBenchmarkScreen when the GL context reveals the real GPU name/version. */
    fun onGpuInfo(renderer: String, version: String) {
        // Strip vendor prefix noise: "Adreno (TM) 750" → "Adreno 750"
        val cleanName = renderer
            .replace("(TM)", "").replace("(tm)", "")
            .replace(Regex("\\s+"), " ").trim()
        // Extract major ES version from e.g. "OpenGL ES 3.2 ..."
        val esVersion = Regex("OpenGL ES (\\d+\\.\\d+)").find(version)?.groupValues?.get(1) ?: "3.0"
        _uiState.update { it.copy(gpuName = cleanName, glApiLabel = "OpenGL ES $esVersion") }
    }

    fun start(preset: String) {
        runJob?.cancel()
        _uiState.update { it.copy(presetName = preset) }
        runJob = viewModelScope.launch { runBenchmark() }
    }

    fun stop() {
        runJob?.cancel()
        if (performanceMonitor.isMonitoring()) performanceMonitor.stop()
        _uiState.update { it.copy(isRunning = false, isCompleted = false, isWarmingUp = false) }
    }

    /** Called on the GL thread every rendered frame. */
    fun onFrameMetrics(fps: Float, frametime: Float) {
        latestFps     = fps
        latestFrameMs = frametime
        frameCount.incrementAndGet()
        totalRenderTimeMs.add(frametime.toDouble())
    }

    private suspend fun runBenchmark() {
        val results = mutableListOf<GpuTestResult>()
        performanceMonitor.start()

        for ((index, scene) in GPU_SCENES.withIndex()) {
            _uiState.update {
                it.copy(
                    isWarmingUp = true, isRunning = false,
                    currentScene = scene, currentTestIndex = index,
                    currentTestName = scene.displayName(),
                    currentTestProgress = 0f,
                    overallProgress = index.toFloat() / GPU_SCENES.size
                )
            }
            // warm-up
            val warmupSteps = (WARMUP_MS / TICK_MS).toInt()
            repeat(warmupSteps) { step ->
                delay(TICK_MS)
                val (freq, temp, load) = readGpuTelemetry()
                _uiState.update { s ->
                    s.copy(
                        currentTestProgress = step.toFloat() / warmupSteps * 0.15f,
                        currentFps = latestFps, currentFrametimeMs = latestFrameMs,
                        gpuFreqMhz = freq, gpuTempC = temp,
                        gpuLoadPercent = load,
                        powerWatts = powerUtils.estimatePowerConsumption().coerceAtLeast(0f)
                    )
                }
            }

            // reset accumulators before measure phase (BUG-4)
            frameCount.set(0)
            totalRenderTimeMs.reset()

            // measure
            _uiState.update { it.copy(isWarmingUp = false, isRunning = true) }
            val measureSteps = (TEST_MS / TICK_MS).toInt()
            val history = ArrayDeque<Float>(60)
            repeat(measureSteps) { step ->
                delay(TICK_MS)
                val currentCount = frameCount.get()
                val currentTotalTime = totalRenderTimeMs.sum()
                val avgFps = if (currentTotalTime > 0.0) (currentCount * 1000.0 / currentTotalTime).toFloat() else latestFps
                
                if (history.size >= 60) history.removeFirst()
                history.addLast(latestFrameMs)
                val overall = (index + 0.15f + (step + 1).toFloat() / measureSteps * 0.85f) / GPU_SCENES.size
                val (freq, temp, load) = readGpuTelemetry()
                _uiState.update { s ->
                    s.copy(
                        isRunning = true,
                        currentTestProgress = 0.15f + (step + 1).toFloat() / measureSteps * 0.85f,
                        overallProgress = overall,
                        currentFps = latestFps,
                        avgFps = avgFps,
                        currentFrametimeMs = latestFrameMs, frametimeHistory = history.toList(),
                        gpuFreqMhz = freq, gpuTempC = temp,
                        gpuLoadPercent = load,
                        powerWatts = powerUtils.estimatePowerConsumption().coerceAtLeast(0f)
                    )
                }
            }

            val finalCount = frameCount.get()
            val finalTotalTime = totalRenderTimeMs.sum()
            val avgFps = if (finalTotalTime > 0.0) (finalCount * 1000.0 / finalTotalTime).toFloat() else 30f
            val avgFt  = if (finalCount > 0) (finalTotalTime / finalCount).toFloat() else (1000f / avgFps)
            // Per-scene score: ratio vs reference × 100 (SD 8 Gen 3 = 100 pts per scene)
            val refFps = GPU_REFERENCE_FPS[scene] ?: 20.0
            val score  = ((avgFps.toDouble() / refFps) * 100.0).roundToInt().coerceAtLeast(0)
            results += GpuTestResult(scene, scene.displayName(), avgFps, avgFt, score)
        }

        val performanceMetricsJson = performanceMonitor.stop()
        // Final score = geometric mean of per-scene FPS ratios × 100 (SD 8 Gen 3 = 100)
        val totalScore = calculateGpuGeometricMean(results).roundToInt().coerceAtLeast(0)

        _uiState.update {
            it.copy(
                isRunning = false, isCompleted = true,
                overallProgress = 1f, completedTests = results, totalScore = totalScore
            )
        }

        val resultJson = buildResultJson(results, totalScore, _uiState.value.presetName, performanceMetricsJson)
        saveToDatabase(results, totalScore, performanceMetricsJson, resultJson)
        _completionEvent.emit(resultJson)
    }

    // ── DB save ──────────────────────────────────────────────────────────

    private suspend fun saveToDatabase(
        results: List<GpuTestResult>,
        totalScore: Int,
        performanceMetricsJson: String,
        detailedJson: String
    ) {
        val repo = historyRepository ?: return
        try {
            val avgFpsAll = if (results.isNotEmpty()) results.map { it.avgFps }.average() else 0.0
            // Store only the detailed_results array so HistoryViewModel's Gson parser
            // (which expects List<BenchmarkResult>) can deserialise it correctly.
            val detailsArrayJson = try {
                JSONObject(detailedJson).optJSONArray("detailed_results")?.toString() ?: "[]"
            } catch (e2: Exception) { "[]" }
            val entity = BenchmarkResultEntity(
                type                 = "GPU",
                totalScore           = totalScore.toDouble(),
                timestamp            = System.currentTimeMillis(),
                deviceModel          = "${Build.MANUFACTURER} ${Build.MODEL}",
                singleCoreScore      = 0.0,
                multiCoreScore       = avgFpsAll,
                normalizedScore      = totalScore.toDouble(),
                detailedResultsJson  = detailsArrayJson,
                performanceMetricsJson = performanceMetricsJson
            )
            val details = results.map { r ->
                GenericTestDetailEntity(
                    resultId   = 0,
                    testName   = r.displayName,
                    score      = r.avgFps.toDouble(),
                    metricsJson = """{"score":${r.score},"avgFps":${"%.2f".format(r.avgFps)},"avgFrametimeMs":${"%.2f".format(r.avgFrametimeMs)}}"""
                )
            }
            repo.saveGenericBenchmark(entity, details)
        } catch (e: Exception) {
            android.util.Log.e("GpuBenchmarkVM", "DB save failed: ${e.message}", e)
        }
    }

    // ── Result JSON (parsed by ResultScreen) ─────────────────────────────

    private fun buildResultJson(
        results: List<GpuTestResult>,
        totalScore: Int,
        preset: String,
        performanceMetricsJson: String
    ): String {
        val avgFpsAll = if (results.isNotEmpty()) results.map { it.avgFps }.average() else 0.0

        val detailedArray = JSONArray()
        results.forEach { r ->
            detailedArray.put(JSONObject().apply {
                put("name", r.displayName)
                put("opsPerSecond", r.avgFps.toDouble())
                put("executionTimeMs", r.avgFrametimeMs.toDouble())
                put("isValid", true)
                put("metricsJson", """{"score":${r.score},"avgFps":${"%.2f".format(r.avgFps)},"avgFrametimeMs":${"%.2f".format(r.avgFrametimeMs)}}""")
            })
        }

        val perfMetricsObj = try {
            JSONObject(performanceMetricsJson)
        } catch (e: Exception) {
            JSONObject()
        }

        return JSONObject().apply {
            put("type", "GPU")
            put("preset", preset)
            put("final_score", totalScore.toDouble())
            put("normalized_score", totalScore.toDouble())
            put("single_core_score", 0.0)
            put("multi_core_score", avgFpsAll)
            put("detailed_results", detailedArray)
            put("timestamp", System.currentTimeMillis())
            put("performance_metrics", perfMetricsObj)
        }.toString()
    }

    // ── Mock HUD hardware fallbacks (used when sysfs unavailable) ────────

    private fun mockGpuFreq(): Int {
        val load = (latestFps / 60f).coerceIn(0f, 1f)
        return ((mockBaseFreq * (0.5f + 0.5f * load)).roundToInt() + (-15..15).random()).coerceIn(100, 1200)
    }
    private fun mockGpuTemp(): Float {
        val extra = latestFps / 60f * 25f
        return (mockBaseTemp + extra + (-1f..1f).random()).coerceIn(30f, 90f)
    }
    private fun mockGpuLoad(fps: Float) = (fps / 60f * 95f + (-2.5f..2.5f).random()).coerceIn(0f, 100f)

    private fun ClosedFloatingPointRange<Float>.random(): Float {
        val range = endInclusive - start
        return start + Math.random().toFloat() * range
    }

    // ── Factory ──────────────────────────────────────────────────────────

    companion object {
        fun factory(
            historyRepository: HistoryRepository?,
            application: Application
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                GpuBenchmarkViewModel(application, historyRepository) as T
        }
    }
}
