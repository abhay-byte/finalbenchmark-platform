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
import com.ivarna.finalbenchmark2.utils.CpuUtilizationUtils
import com.ivarna.finalbenchmark2.utils.GpuFrequencyReader
import com.ivarna.finalbenchmark2.utils.PerformanceMonitor
import com.ivarna.finalbenchmark2.utils.PowerUtils
import com.ivarna.finalbenchmark2.utils.VulkanBenchmarkBridge
import com.ivarna.finalbenchmark2.utils.OpenCLBenchmarkBridge
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
    val apiLabel: String,
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
    val totalTests: Int = GPU_SCENE_COUNT,
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
    val cpuLoadPercent: Float = 0f,
    // Real-time power draw in Watts (from PowerUtils) — absolute value
    val powerWatts: Float = 0f,
    // GPU hardware identity (from GL_RENDERER / GL_VERSION)
    val gpuName: String = "",
    val glApiLabel: String = "OpenGL ES 3.0",
    val currentApiLabel: String = "OpenGL ES 3.0",

    val completedTests: List<GpuTestResult> = emptyList(),
    val totalScore: Int = 0,
    val presetName: String = ""
)

// ─────────────────────────────────────────────────────────────────────────

/** Total number of benchmark scenes — used in UI state before GPU_SCENES is accessible. */
private const val GPU_SCENE_COUNT = 12

/**
 * 10-scene benchmark split across three GPU compute APIs:
 *  - OpenGL ES 3.0  (2 scenes): triangle/geometry + fragment shader workload
 *  - OpenGL ES 3.2  (2 scenes): advanced texture sampling + ALU-heavy fractal
 *  - Vulkan 1.1     (3 scenes): Julia compute, Mandelbrot compute, GEMM
 *  - OpenCL 2.0     (3 scenes): memory bandwidth, Julia, GEMM
 */
private val GPU_SCENES = listOf(
    // ── OpenGL ES 3.0 (1-4) ──────────────────────────────────────────────
    GpuScene.TRIANGLE_RENDERING,   // Domain Warp + 10K triangles
    GpuScene.COMPUTE_MATRIX,       // Julia / Matrix fragment compute
    GpuScene.PARTICLE_SYSTEM,      // Phong-lit particle system (5K)
    GpuScene.TEXTURE_SAMPLING,     // 12-octave FBM texture bandwidth
    // ── Vulkan 1.1 (5-7) ─────────────────────────────────────────────────
    GpuScene.VULKAN_JULIA_COMPUTE,
    GpuScene.VULKAN_MANDELBROT_COMPUTE,
    GpuScene.VULKAN_GEMM_COMPUTE,
    // ── OpenCL 2.0 (8-10) ────────────────────────────────────────────────
    GpuScene.OPENCL_MEM_BW,
    GpuScene.OPENCL_JULIA_COMPUTE,
    GpuScene.OPENCL_GEMM_COMPUTE,
    // ── OpenGL ES 3.2 Extended (11-12) ────────────────────────────────────
    GpuScene.RAY_MARCH_SDF,        // "Ray March SDF + Shadows"
    GpuScene.SUPER_SAMPLE          // "32× Super-Sampled Fractal"
)

/**
 * Reference FPS per scene on Snapdragon 8 Gen 3 / Adreno 750 (baseline = 100 pts).
 * Scenes 1,3,5: 1× fragment pre-pass + geometry overlay → GPU ALU-bound, not CPU/API-bound.
 * Scenes 2,4,6-10: 4× fullscreen passes → GPU compute-bound.
 * Extended scenes: heavy multi-pass workloads targeting <20 FPS on flagship GPU.
 * Any device matching these FPS values scores exactly 100.
 */
private val GPU_REFERENCE_FPS = mapOf(
    GpuScene.TRIANGLE_RENDERING to  86.5,
    GpuScene.COMPUTE_MATRIX     to  38.0,
    GpuScene.PARTICLE_SYSTEM    to  28.1,
    GpuScene.TEXTURE_SAMPLING   to  23.0,
    GpuScene.WIREFRAME_MESH     to  84.3,
    GpuScene.MANDELBROT_DEEP    to  17.4,
    GpuScene.PHONG_MULTI_LIGHT  to   7.0,
    GpuScene.RAY_MARCH_SDF      to  21.9,
    GpuScene.DOMAIN_WARP        to  20.9,
    GpuScene.SUPER_SAMPLE       to   7.4,
    GpuScene.SHADER_COMPILE     to  14.0,
    GpuScene.MEM_BANDWIDTH      to   9.0,
    GpuScene.MSAA_4X            to  11.0,
    GpuScene.VRAM_PRESSURE      to  10.0,
    GpuScene.GEOMETRY_ALU_SATURATION to   7.0,
    GpuScene.MULTI_PASS_BLOOM   to   8.0,
    // Vulkan 1.1 compute — ref calibrated on Adreno 750 / SD8 Gen3
    GpuScene.VULKAN_JULIA_COMPUTE      to  78.8,  // 4K Julia 512 iter
    GpuScene.VULKAN_MANDELBROT_COMPUTE to  20.0,  // 4K Mandelbrot 2048 iter
    GpuScene.VULKAN_GEMM_COMPUTE       to  25.0,  // 1024×1024 GEMM
    GpuScene.VULKAN_N_BODY_COMPUTE     to  22.0,
    // OpenCL 2.0 compute — ref calibrated on Adreno 750 / SD8 Gen3
    GpuScene.OPENCL_MEM_BW             to  18.0,  // 64 MB D2D bandwidth GB/s
    GpuScene.OPENCL_JULIA_COMPUTE      to  68.0,  // 4K Julia 512 iter
    GpuScene.OPENCL_GEMM_COMPUTE       to  76.0,  // 1024×1024 GEMM GFLOPS
    GpuScene.OPENCL_N_BODY_COMPUTE     to  20.0
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

private fun GpuScene.apiLabel(glApiLabel: String) = when (this) {
    // OpenGL ES 3.0 — core geometry + fragment shader workloads
    GpuScene.TRIANGLE_RENDERING, GpuScene.COMPUTE_MATRIX -> "OpenGL ES 3.0"
    // OpenGL ES 3.2 — advanced texture + high-quality sampling
    GpuScene.PARTICLE_SYSTEM, GpuScene.TEXTURE_SAMPLING,
    GpuScene.WIREFRAME_MESH, GpuScene.MANDELBROT_DEEP,
    GpuScene.PHONG_MULTI_LIGHT, GpuScene.RAY_MARCH_SDF, GpuScene.DOMAIN_WARP,
    GpuScene.SUPER_SAMPLE, GpuScene.SHADER_COMPILE, GpuScene.MEM_BANDWIDTH,
    GpuScene.MSAA_4X, GpuScene.VRAM_PRESSURE, GpuScene.GEOMETRY_ALU_SATURATION,
    GpuScene.MULTI_PASS_BLOOM -> "OpenGL ES 3.2"
    // Vulkan 1.1 compute scenes
    GpuScene.VULKAN_JULIA_COMPUTE, GpuScene.VULKAN_MANDELBROT_COMPUTE,
    GpuScene.VULKAN_GEMM_COMPUTE, GpuScene.VULKAN_N_BODY_COMPUTE -> "Vulkan 1.1"
    // OpenCL 2.0 compute scenes
    GpuScene.OPENCL_MEM_BW, GpuScene.OPENCL_JULIA_COMPUTE,
    GpuScene.OPENCL_GEMM_COMPUTE, GpuScene.OPENCL_N_BODY_COMPUTE -> "OpenCL 2.0"
}

private fun GpuScene.displayName() = when (this) {
    // OpenGL ES
    GpuScene.TRIANGLE_RENDERING  -> "Domain Warp + Triangles (10K)"
    GpuScene.COMPUTE_MATRIX      -> "Julia / Matrix Compute"
    GpuScene.PARTICLE_SYSTEM     -> "Phong + Particles (5K)"
    GpuScene.TEXTURE_SAMPLING    -> "12-Octave FBM Texture"
    GpuScene.WIREFRAME_MESH      -> "Ray March + Mesh (250\u00d7250)"
    GpuScene.MANDELBROT_DEEP     -> "Mandelbrot Deep (512 iter)"
    GpuScene.PHONG_MULTI_LIGHT   -> "Phong 128-Light Array"
    GpuScene.RAY_MARCH_SDF       -> "Ray March SDF + Shadows"
    GpuScene.DOMAIN_WARP         -> "Triple Domain Warp FBM"
    GpuScene.SUPER_SAMPLE        -> "32\u00d7 Super-Sampled Fractal"
    GpuScene.SHADER_COMPILE      -> "ALU Dual-Warp Stress"
    GpuScene.MEM_BANDWIDTH       -> "Texture Bandwidth Stress"
    GpuScene.MSAA_4X             -> "MSAA 4\u00d7 Resolve Stress"
    GpuScene.VRAM_PRESSURE       -> "VRAM Texture Pressure"
    GpuScene.GEOMETRY_ALU_SATURATION  -> "Geometry ALU Saturation"
    GpuScene.MULTI_PASS_BLOOM    -> "5-Pass Gaussian Bloom"
    // Vulkan
    GpuScene.VULKAN_JULIA_COMPUTE      -> "Vulkan Julia Compute"
    GpuScene.VULKAN_MANDELBROT_COMPUTE -> "Vulkan Mandelbrot Compute"
    GpuScene.VULKAN_GEMM_COMPUTE       -> "Vulkan GEMM Compute"
    GpuScene.VULKAN_N_BODY_COMPUTE     -> "Vulkan N-Body Compute"
    // OpenCL
    GpuScene.OPENCL_MEM_BW             -> "OpenCL Memory Bandwidth"
    GpuScene.OPENCL_JULIA_COMPUTE      -> "OpenCL Julia Compute"
    GpuScene.OPENCL_GEMM_COMPUTE       -> "OpenCL GEMM Compute"
    GpuScene.OPENCL_N_BODY_COMPUTE     -> "OpenCL N-Body Compute"
}



private const val WARMUP_MS = 2_000L
private const val TEST_MS   = 10_000L
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
    private val cpuUtilizationUtils = CpuUtilizationUtils(application)
    // Mock fallbacks (used when sysfs is unavailable)
    private val mockBaseFreq    = (500..800).random()
    private val mockBaseTemp    = (35..45).random()

    /**
     * Returns (freqMhz, tempC, gpuLoadPercent, cpuLoadPercent) from sysfs or mocks.
     * Called from coroutine tick — runs on IO dispatcher inside GpuFrequencyReader.
     */
    private suspend fun readGpuTelemetry(): GpuTelemetry {
        return try {
            val state = gpuFreqReader.readGpuFrequency()
            val cpuLoad = cpuUtilizationUtils.getCpuUtilizationPercentage().coerceIn(0f, 100f)
            if (state is GpuFrequencyReader.GpuFrequencyState.Available) {
                val d = state.data
                val freq = d.currentFrequencyMhz.toInt().coerceIn(0, 3000)
                val temp = d.temperatureCelsius?.toFloat()?.coerceIn(0f, 120f) ?: mockGpuTemp()
                val load = d.utilizationPercent?.toFloat()?.coerceIn(0f, 100f) ?: mockGpuLoad(latestFps)
                GpuTelemetry(freq, temp, load, cpuLoad)
            } else {
                GpuTelemetry(mockGpuFreq(), mockGpuTemp(), mockGpuLoad(latestFps), cpuLoad)
            }
        } catch (e: Exception) {
            val cpuLoad = cpuUtilizationUtils.getCpuUtilizationPercentage().coerceIn(0f, 100f)
            GpuTelemetry(mockGpuFreq(), mockGpuTemp(), mockGpuLoad(latestFps), cpuLoad)
        }
    }

    private data class GpuTelemetry(
        val freqMhz: Int,
        val tempC: Float,
        val gpuLoad: Float,
        val cpuLoad: Float
    )

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

        // Initialize compute API bridges once
        val vulkanOk = VulkanBenchmarkBridge.init()
        val openclOk = OpenCLBenchmarkBridge.init()
        if (vulkanOk) android.util.Log.i("GpuBenchmarkVM", "Vulkan ready: ${VulkanBenchmarkBridge.getGpuName()}")
        if (openclOk) android.util.Log.i("GpuBenchmarkVM", "OpenCL ready")

        for ((index, scene) in GPU_SCENES.withIndex()) {
            _uiState.update {
                it.copy(
                    isWarmingUp = true, isRunning = false,
                    currentScene = scene, currentTestIndex = index,
                    currentTestName = scene.displayName(),
                    currentTestProgress = 0f,
                    overallProgress = index.toFloat() / GPU_SCENES.size,
                    currentApiLabel = scene.apiLabel(it.glApiLabel)
                )
            }

            // ── Vulkan / OpenCL compute scenes (run via native bridge, not GL) ──
            if (scene.isComputeScene()) {
                delay(WARMUP_MS) // brief "warm-up" delay so UI stays consistent
                _uiState.update { it.copy(isWarmingUp = false, isRunning = true) }

                val scoreValue = when (scene) {
                    // Vulkan scenes
                    GpuScene.VULKAN_JULIA_COMPUTE      -> if (vulkanOk) VulkanBenchmarkBridge.runScene(0) else -1f
                    GpuScene.VULKAN_MANDELBROT_COMPUTE -> if (vulkanOk) VulkanBenchmarkBridge.runScene(1) else -1f
                    GpuScene.VULKAN_GEMM_COMPUTE       -> if (vulkanOk) VulkanBenchmarkBridge.runScene(2) else -1f
                    GpuScene.VULKAN_N_BODY_COMPUTE     -> if (vulkanOk) VulkanBenchmarkBridge.runScene(3) else -1f
                    // OpenCL scenes
                    GpuScene.OPENCL_MEM_BW             -> if (openclOk) OpenCLBenchmarkBridge.runScene(0) else -1f
                    GpuScene.OPENCL_JULIA_COMPUTE      -> if (openclOk) OpenCLBenchmarkBridge.runScene(1) else -1f
                    GpuScene.OPENCL_GEMM_COMPUTE       -> if (openclOk) OpenCLBenchmarkBridge.runScene(2) else -1f
                    GpuScene.OPENCL_N_BODY_COMPUTE     -> if (openclOk) OpenCLBenchmarkBridge.runScene(3) else -1f
                    else -> -1f
                }

                // Convert score to common FPS-like metric for scoring
                // Sanity: reject impossible values (NaN, Inf, negative, or >1000 FPS
                // which would indicate broken timing on Mali/other GPUs)
                val rawFps = scoreValue
                val avgFps = if (rawFps.isNaN() || rawFps.isInfinite() || rawFps < 0f || rawFps > 1000f) 0f
                             else rawFps.coerceAtLeast(0f)
                val avgFt  = if (avgFps > 0f) 1000f / avgFps else 0f
                val refFps = GPU_REFERENCE_FPS[scene] ?: 20.0
                val score  = ((avgFps.toDouble() / refFps) * 100.0).roundToInt().coerceAtLeast(0)

                _uiState.update { s ->
                    s.copy(
                        isRunning = false,
                        currentTestProgress = 1f,
                        overallProgress = (index + 1).toFloat() / GPU_SCENES.size,
                        currentFps = avgFps,
                        avgFps = avgFps,
                        currentFrametimeMs = avgFt,
                        frametimeHistory = listOf(avgFt)
                    )
                }
                results += GpuTestResult(scene, scene.displayName(), scene.apiLabel(_uiState.value.glApiLabel), avgFps, avgFt, score)
                continue
            }

            // ── OpenGL ES scenes (existing GL renderer path) ──
            val warmupSteps = (WARMUP_MS / TICK_MS).toInt()
            repeat(warmupSteps) { step ->
                delay(TICK_MS)
                val telem = readGpuTelemetry()
                _uiState.update { s ->
                    s.copy(
                        currentTestProgress = step.toFloat() / warmupSteps * 0.15f,
                        currentFps = latestFps, currentFrametimeMs = latestFrameMs,
                        gpuFreqMhz = telem.freqMhz, gpuTempC = telem.tempC,
                        gpuLoadPercent = telem.gpuLoad,
                        cpuLoadPercent = telem.cpuLoad,
                        powerWatts = powerUtils.estimatePowerConsumption()
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
                val telem = readGpuTelemetry()
                _uiState.update { s ->
                    s.copy(
                        isRunning = true,
                        currentTestProgress = 0.15f + (step + 1).toFloat() / measureSteps * 0.85f,
                        overallProgress = overall,
                        currentFps = latestFps,
                        avgFps = avgFps,
                        currentFrametimeMs = latestFrameMs, frametimeHistory = history.toList(),
                        gpuFreqMhz = telem.freqMhz, gpuTempC = telem.tempC,
                        gpuLoadPercent = telem.gpuLoad,
                        cpuLoadPercent = telem.cpuLoad,
                        powerWatts = powerUtils.estimatePowerConsumption()
                    )
                }
            }

            val finalCount = frameCount.get()
            val finalTotalTime = totalRenderTimeMs.sum()
            val avgFps = if (finalTotalTime > 0.0) (finalCount * 1000.0 / finalTotalTime).toFloat() else 30f
            val avgFt  = if (finalCount > 0) (finalTotalTime / finalCount).toFloat() else (1000f / avgFps)
            val refFps = GPU_REFERENCE_FPS[scene] ?: 20.0
            val score  = ((avgFps.toDouble() / refFps) * 100.0).roundToInt().coerceAtLeast(0)
            results += GpuTestResult(scene, scene.displayName(), scene.apiLabel(_uiState.value.glApiLabel), avgFps, avgFt, score)
        }

        VulkanBenchmarkBridge.destroy()
        OpenCLBenchmarkBridge.destroy()

        val performanceMetricsJson = performanceMonitor.stop()
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

    private fun GpuScene.isComputeScene() = when (this) {
        GpuScene.VULKAN_JULIA_COMPUTE, GpuScene.VULKAN_MANDELBROT_COMPUTE,
        GpuScene.VULKAN_GEMM_COMPUTE, GpuScene.VULKAN_N_BODY_COMPUTE,
        GpuScene.OPENCL_MEM_BW, GpuScene.OPENCL_JULIA_COMPUTE,
        GpuScene.OPENCL_GEMM_COMPUTE, GpuScene.OPENCL_N_BODY_COMPUTE -> true
        else -> false
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
                put("metricsJson", """{"score":${r.score},"avgFps":${"%.2f".format(r.avgFps)},"avgFrametimeMs":${"%.2f".format(r.avgFrametimeMs)},"api":"${r.apiLabel}"}""")
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
