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
import com.ivarna.finalbenchmark2.utils.PerformanceMonitor
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
    val cpuTempC: Float = 0f,

    val completedTests: List<GpuTestResult> = emptyList(),
    val totalScore: Int = 0,
    val presetName: String = ""
)

// ─────────────────────────────────────────────────────────────────────────

private val GPU_SCENES = GpuScene.values().toList()

/**
 * Reference FPS per scene on Snapdragon 8 Gen 3 / Adreno 750 (baseline = 100 pts).
 * Derived from measured results on the reference device with 4× draw passes.
 * Any device matching these FPS values scores exactly 100.
 */
private val GPU_REFERENCE_FPS = mapOf(
    GpuScene.TRIANGLE_RENDERING to 20.0,
    GpuScene.COMPUTE_MATRIX     to 15.0,  // after branchless Julia fix
    GpuScene.PARTICLE_SYSTEM    to  7.0,
    GpuScene.TEXTURE_SAMPLING   to 24.0,
    GpuScene.WIREFRAME_MESH     to 23.0,
    GpuScene.MANDELBROT_DEEP    to 16.0,
    GpuScene.PHONG_MULTI_LIGHT  to  7.0,
    GpuScene.RAY_MARCH_SDF      to 24.0,
    GpuScene.DOMAIN_WARP        to 20.0,
    GpuScene.SUPER_SAMPLE       to  3.5
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
    GpuScene.TRIANGLE_RENDERING -> "Triangle Rendering (10K)"
    GpuScene.COMPUTE_MATRIX     -> "Julia / Matrix Compute"
    GpuScene.PARTICLE_SYSTEM    -> "Particle System (50K)"
    GpuScene.TEXTURE_SAMPLING   -> "12-Octave FBM Texture"
    GpuScene.WIREFRAME_MESH     -> "Wave Mesh (250×250)"
    GpuScene.MANDELBROT_DEEP    -> "Mandelbrot Deep (512 iter)"
    GpuScene.PHONG_MULTI_LIGHT  -> "Phong 128-Light Array"
    GpuScene.RAY_MARCH_SDF      -> "Ray March SDF + Shadows"
    GpuScene.DOMAIN_WARP        -> "Triple Domain Warp FBM"
    GpuScene.SUPER_SAMPLE       -> "64× Super-Sampled Fractal"
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

    private var runJob: Job? = null
    private val performanceMonitor = PerformanceMonitor(application)

    // Simple mock hw values (for display in HUD only — real perf metrics come from PerformanceMonitor)
    private val mockBaseFreq    = (500..800).random()
    private val mockBaseTemp    = (35..45).random()
    private val mockBaseCpuTemp = (38..48).random()
    private var fpsAccum = 0.0; private var ftAccum = 0.0; private var fpsCount = 0

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
            fpsAccum = 0.0; ftAccum = 0.0; fpsCount = 0

            // warm-up
            val warmupSteps = (WARMUP_MS / TICK_MS).toInt()
            repeat(warmupSteps) { step ->
                delay(TICK_MS)
                _uiState.update { s ->
                    s.copy(
                        currentTestProgress = step.toFloat() / warmupSteps * 0.15f,
                        currentFps = latestFps, currentFrametimeMs = latestFrameMs,
                        gpuFreqMhz = mockGpuFreq(), gpuTempC = mockGpuTemp(),
                        gpuLoadPercent = mockGpuLoad(latestFps), cpuTempC = mockCpuTemp()
                    )
                }
            }

            // measure
            _uiState.update { it.copy(isWarmingUp = false, isRunning = true) }
            val measureSteps = (TEST_MS / TICK_MS).toInt()
            val history = ArrayDeque<Float>(60)
            repeat(measureSteps) { step ->
                delay(TICK_MS)
                val fps = latestFps; val ft = latestFrameMs
                if (fps > 1f) { fpsAccum += fps; ftAccum += ft; fpsCount++ }
                if (history.size >= 60) history.removeFirst()
                history.addLast(ft)
                val overall = (index + 0.15f + (step + 1).toFloat() / measureSteps * 0.85f) / GPU_SCENES.size
                _uiState.update { s ->
                    s.copy(
                        isRunning = true,
                        currentTestProgress = 0.15f + (step + 1).toFloat() / measureSteps * 0.85f,
                        overallProgress = overall,
                        currentFps = fps,
                        avgFps = if (fpsCount > 0) (fpsAccum / fpsCount).toFloat() else fps,
                        currentFrametimeMs = ft, frametimeHistory = history.toList(),
                        gpuFreqMhz = mockGpuFreq(), gpuTempC = mockGpuTemp(),
                        gpuLoadPercent = mockGpuLoad(fps), cpuTempC = mockCpuTemp()
                    )
                }
            }

            val avgFps = if (fpsCount > 0) (fpsAccum / fpsCount).toFloat() else 30f
            val avgFt  = if (fpsCount > 0) (ftAccum  / fpsCount).toFloat() else (1000f / avgFps)
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

    // ── Mock HUD hardware values ──────────────────────────────────────────

    private fun mockGpuFreq(): Int {
        val load = (latestFps / 60f).coerceIn(0f, 1f)
        return ((mockBaseFreq * (0.5f + 0.5f * load)).roundToInt() + (-15..15).random()).coerceIn(100, 1200)
    }
    private fun mockGpuTemp(): Float {
        val extra = latestFps / 60f * 25f
        return (mockBaseTemp + extra + (-1f..1f).random()).coerceIn(30f, 90f)
    }
    private fun mockGpuLoad(fps: Float) = (fps / 60f * 95f + (-2.5f..2.5f).random()).coerceIn(0f, 100f)
    private fun mockCpuTemp() = (mockBaseCpuTemp + (-2f..2f).random()).coerceIn(30f, 85f)

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
