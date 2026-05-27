package com.ivarna.finalbenchmark2.ui.viewmodels

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ivarna.finalbenchmark2.data.database.entities.BenchmarkResultEntity
import com.ivarna.finalbenchmark2.data.database.entities.GenericTestDetailEntity
import com.ivarna.finalbenchmark2.data.repository.HistoryRepository
import com.ivarna.finalbenchmark2.utils.PerformanceMonitor
import com.ivarna.finalbenchmark2.utils.RamNativeBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
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
import kotlin.math.pow
import kotlin.math.roundToInt

// ── Data types ────────────────────────────────────────────────────────────────

enum class RamTest {
    SEQ_READ, SEQ_WRITE, RAND_ACCESS, MEM_COPY, MULTI_THREAD
}

data class RamTestResult(
    val test: RamTest,
    val displayName: String,
    val value: Double,   // MB/s for bandwidth tests; ns/op for RAND_ACCESS
    val unit: String,
    val score: Int       // normalised to reference; reference device = 100 pts
)

data class RamBenchmarkUiState(
    val isWarmingUp: Boolean = false,
    val isRunning: Boolean = false,
    val isCompleted: Boolean = false,
    val currentTest: RamTest = RamTest.SEQ_READ,
    val currentTestIndex: Int = 0,
    val totalTests: Int = RamTest.values().size,
    val currentTestName: String = "",
    val overallProgress: Float = 0f,
    val currentTestProgress: Float = 0f,
    val currentValue: Double = 0.0,
    val currentUnit: String = "MB/s",
    val memUsageMB: Int = 0,
    val cpuTempC: Float = 0f,
    val completedTests: List<RamTestResult> = emptyList(),
    val totalScore: Int = 0,
    val presetName: String = ""
)

// ── Constants ─────────────────────────────────────────────────────────────────

private val RAM_TESTS = RamTest.values().toList()

/**
 * Reference values calibrated for NATIVE (JNI/NEON) code on SD 8 Gen 3 / LPDDR5X.
 *
 * Methodology:
 *   SeqRead  – 64 MB, 64 B/iter NEON vld1q_u64 × 4 + prefetch (single thread)
 *   SeqWrite – 64 MB, 64 B/iter NEON vst1q_u64 × 4 (single thread)
 *   RandAccess – 16 MB Knuth-shuffled int32 pointer-chase (no JVM bounds-checks)
 *   MemCopy  – Bionic libc memcpy (hand-written NEON in Android's libc)
 *   MultiThread – same NEON read loop spread over 4 perf cores via pthreads
 *
 * JVM fallback (used if .so fails to load) keeps the old JVM values.
 *
 * LPDDR5X peak: 76.8 GB/s.  Native single-thread effective BW typically 20-35 GB/s.
 * These reference values target SD 8 Gen 3 = 100 pts with native code.
 * Adjust after first run if needed.
 */
// Reference values calibrated from ACTUAL SD 8 Gen 3 measurements with native code.
// Targeting SD 8 Gen 3 (LPDDR5X) = 100 pts on each test.
private val RAM_REFERENCE_NATIVE = mapOf(
    RamTest.SEQ_READ     to 34_112.0,  // MB/s  (measured 34112 on SD 8 Gen 3)
    RamTest.SEQ_WRITE    to 20_864.0,  // MB/s  (measured 20864 on SD 8 Gen 3)
    RamTest.RAND_ACCESS  to 93.2,      // ns/op (measured 93.2 on SD 8 Gen 3; lower=better)
    RamTest.MEM_COPY     to 20_071.0,  // MB/s  (measured 20071 on SD 8 Gen 3)
    RamTest.MULTI_THREAD to 49_720.0,  // MB/s  (measured 49720 on SD 8 Gen 3)
)

private val RAM_REFERENCE_JVM = mapOf(
    RamTest.SEQ_READ     to 6_500.0,   // MB/s  (LongArray word-read, measured ~6624)
    RamTest.SEQ_WRITE    to 3_200.0,   // MB/s  (Arrays.fill LongArray, ART gets ~3000-3400 on 64MB)
    RamTest.RAND_ACCESS  to 530.0,     // ns/op (JVM pointer-chase, measured ~529 ns)
    RamTest.MEM_COPY     to 11_000.0,  // MB/s  (System.arraycopy, measured ~11488)
    RamTest.MULTI_THREAD to 11_500.0,  // MB/s  (4-thread LongArray, measured ~11392)
)

// Selected at runtime based on whether JNI .so loaded successfully
private var RAM_REFERENCE = RAM_REFERENCE_JVM

private fun RamTest.displayName() = when (this) {
    RamTest.SEQ_READ     -> "Sequential Read"
    RamTest.SEQ_WRITE    -> "Sequential Write"
    RamTest.RAND_ACCESS  -> "Random Access Latency"
    RamTest.MEM_COPY     -> "Memory Copy (arraycopy)"
    RamTest.MULTI_THREAD -> "Multi-threaded Bandwidth"
}

private fun RamTest.unit() = if (this == RamTest.RAND_ACCESS) "ns/op" else "MB/s"
private fun RamTest.isLowerBetter() = this == RamTest.RAND_ACCESS

private fun RamTest.score(value: Double): Int {
    val ref = RAM_REFERENCE[this] ?: return 0
    val ratio = if (isLowerBetter()) ref / value.coerceAtLeast(0.1) else value / ref
    return (ratio * 100.0).roundToInt().coerceAtLeast(0)
}

private fun calculateRamGeometricMean(results: List<RamTestResult>): Double {
    val ratios = results.map { r ->
        val ref = RAM_REFERENCE[r.test] ?: return@map 1.0
        if (r.test.isLowerBetter()) ref / r.value.coerceAtLeast(0.1) else r.value / ref
    }
    if (ratios.isEmpty()) return 0.0
    val product = ratios.fold(1.0) { acc, v -> acc * v.coerceAtLeast(1e-9) }
    return product.pow(1.0 / ratios.size) * 100.0
}

private const val WARMUP_DUR_MS  = 600L
private const val MEASURE_DUR_MS = 2_000L
private const val TICK_MS        = 100L

// ── ViewModel ─────────────────────────────────────────────────────────────────

class RamBenchmarkViewModel(
    application: Application,
    private val historyRepository: HistoryRepository?
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(RamBenchmarkUiState())
    val uiState: StateFlow<RamBenchmarkUiState> = _uiState.asStateFlow()

    private val _completionEvent = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 1)
    val completionEvent: SharedFlow<String> = _completionEvent.asSharedFlow()

    private var runJob: Job? = null
    private val performanceMonitor = PerformanceMonitor(application)
    private val baseCpuTemp = (38..45).random().toFloat()
    private val nativeAvailable: Boolean

    init {
        nativeAvailable = RamNativeBridge.load()
        if (nativeAvailable) {
            RAM_REFERENCE = RAM_REFERENCE_NATIVE
        }
    }

    fun start(preset: String) {
        runJob?.cancel()
        _uiState.update { it.copy(presetName = preset) }
        runJob = viewModelScope.launch { runBenchmark() }
    }

    fun stop() {
        runJob?.cancel()
        if (performanceMonitor.isMonitoring()) performanceMonitor.stop()
        _uiState.update { RamBenchmarkUiState() }
    }

    // ── Benchmark loop ────────────────────────────────────────────────────

    private suspend fun runBenchmark() {
        val results = mutableListOf<RamTestResult>()
        performanceMonitor.start()

        for ((index, test) in RAM_TESTS.withIndex()) {
            val name = test.displayName()
            val unit = test.unit()

            // warm-up phase
            _uiState.update {
                it.copy(
                    isWarmingUp = true, isRunning = false,
                    currentTest = test, currentTestIndex = index,
                    currentTestName = name, currentTestProgress = 0f,
                    currentUnit = unit,
                    overallProgress = index.toFloat() / RAM_TESTS.size
                )
            }
            // run warm-up computation on background thread (discard result)
            kotlinx.coroutines.coroutineScope {
                val warmupJob = async(Dispatchers.Default) { runTest(test, durationMs = 500L) }
                val warmSteps = (WARMUP_DUR_MS / TICK_MS).toInt()
                repeat(warmSteps) { step ->
                    delay(TICK_MS)
                    _uiState.update { s ->
                        s.copy(
                            currentTestProgress = (step + 1).toFloat() / warmSteps * 0.15f,
                            cpuTempC = mockCpuTemp(),
                            memUsageMB = mockMemUsage()
                        )
                    }
                }
                warmupJob.await()  // don't leak Deferred
            }

            // measure phase
            _uiState.update { it.copy(isWarmingUp = false, isRunning = true) }
            val measureSteps = (MEASURE_DUR_MS / TICK_MS).toInt()
            val value = kotlinx.coroutines.coroutineScope {
                val measureJob = async(Dispatchers.Default) { runTest(test, durationMs = MEASURE_DUR_MS) }
                repeat(measureSteps) { step ->
                    delay(TICK_MS)
                    val overall = (index + 0.15f + (step + 1).toFloat() / measureSteps * 0.85f) / RAM_TESTS.size
                    _uiState.update { s ->
                        s.copy(
                            currentTestProgress = 0.15f + (step + 1).toFloat() / measureSteps * 0.85f,
                            overallProgress = overall,
                            cpuTempC = mockCpuTemp(),
                            memUsageMB = mockMemUsage()
                        )
                    }
                }
                measureJob.await()
            }
            val result = RamTestResult(test, name, value, unit, test.score(value))
            results += result
            _uiState.update { s -> s.copy(currentValue = value, completedTests = results.toList()) }
        }

        val performanceMetricsJson = performanceMonitor.stop()
        val totalScore = calculateRamGeometricMean(results).roundToInt().coerceAtLeast(0)

        _uiState.update {
            it.copy(isRunning = false, isCompleted = true, overallProgress = 1f, totalScore = totalScore)
        }

        val resultJson = buildResultJson(results, totalScore, _uiState.value.presetName, performanceMetricsJson)
        saveToDatabase(results, totalScore, performanceMetricsJson, resultJson)
        _completionEvent.emit(resultJson)
    }

    private fun detectBigCoreCount(): Int {
        val numCores = Runtime.getRuntime().availableProcessors()
        val freqs = mutableListOf<Long>()
        for (i in 0 until numCores) {
            val file = java.io.File("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_max_freq")
            if (file.exists()) {
                try {
                    file.readText().trim().toLongOrNull()?.let { freqs.add(it) }
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
        if (freqs.isEmpty()) {
            return (numCores / 2).coerceIn(2, 8)
        }
        val maxFreq = freqs.maxOrNull() ?: return (numCores / 2).coerceIn(2, 8)
        val threshold = (maxFreq * 0.75).toLong()
        val bigCores = freqs.count { it >= threshold }
        return bigCores.coerceIn(2, 16)
    }

    // ── Benchmark implementations (called on Dispatchers.Default) ─────────

    private fun runTest(test: RamTest, durationMs: Long): Double {
        // Use native NEON/pthreads path when the .so is available (arm64 devices);
        // fall back gracefully to JVM LongArray/arraycopy on ART-only targets.
        if (nativeAvailable) {
            return when (test) {
                RamTest.SEQ_READ     -> RamNativeBridge.nativeSeqRead(durationMs)
                RamTest.SEQ_WRITE    -> RamNativeBridge.nativeSeqWrite(durationMs)
                RamTest.RAND_ACCESS  -> RamNativeBridge.nativeRandAccess(durationMs)
                RamTest.MEM_COPY     -> RamNativeBridge.nativeMemCopy(durationMs)
                RamTest.MULTI_THREAD -> RamNativeBridge.nativeMultiThread(
                    detectBigCoreCount(), durationMs
                )
            }
        }
        return when (test) {
            RamTest.SEQ_READ     -> benchSeqRead(durationMs)
            RamTest.SEQ_WRITE    -> benchSeqWrite(durationMs)
            RamTest.RAND_ACCESS  -> benchRandAccess(durationMs)
            RamTest.MEM_COPY     -> benchMemCopy(durationMs)
            RamTest.MULTI_THREAD -> benchMultiThread(durationMs)
        }
    }

    /**
     * Sequential read using 64 MB LongArray – 8 bytes per iteration so ART can
     * auto-vectorise the accumulation loop.  Byte-by-byte loops (~700 MB/s) only
     * measure JVM overhead; word reads reach ~5-8 GB/s which reflects real DRAM BW.
     * Returns MB/s.
     */
    private fun benchSeqRead(durationMs: Long): Double {
        val count = 64 * 1024 * 1024 / 8          // 64 MB as LongArray
        val buf = LongArray(count) { it.toLong() } // initialise so data is in RAM, not zeroed COW pages
        var totalBytes = 0L
        var sum = 0L
        val endNs = System.nanoTime() + durationMs * 1_000_000L
        while (System.nanoTime() < endNs) {
            var s = 0L
            for (v in buf) s += v         // word-reads – 8 bytes / iteration
            sum += s
            totalBytes += count.toLong() * 8L
        }
        neverEliminate = sum
        return totalBytes.toDouble() / (durationMs / 1000.0) / 1024.0 / 1024.0
    }

    /**
     * Sequential write using java.util.Arrays.fill() on a 64 MB LongArray.
     * Arrays.fill is a JVM intrinsic that ART compiles to an optimised SIMD store
     * loop – far faster than index-by-index byte writes.
     * Returns MB/s.
     */
    private fun benchSeqWrite(durationMs: Long): Double {
        val count = 64 * 1024 * 1024 / 8          // 64 MB as LongArray
        val buf = LongArray(count)
        var totalBytes = 0L
        var v = 1L
        val endNs = System.nanoTime() + durationMs * 1_000_000L
        while (System.nanoTime() < endNs) {
            java.util.Arrays.fill(buf, v++)        // JVM intrinsic vectorised store
            totalBytes += count.toLong() * 8L
        }
        neverEliminate += buf[buf.size / 2]
        return totalBytes.toDouble() / (durationMs / 1000.0) / 1024.0 / 1024.0
    }

    /**
     * Random access via a pre-built pointer-chase chain (defeats CPU prefetcher).
     * Working set = 16 MB (exceeds typical L2, hits L3/DRAM).
     * Returns average ns per access.
     */
    private fun benchRandAccess(durationMs: Long): Double {
        val count = 16 * 1024 * 1024 / 8   // 16 MB as LongArray
        val buf = LongArray(count) { it.toLong() }
        val rng = java.util.Random(42)
        // Build a random permutation so every access is truly random
        val perm = IntArray(count) { it }
        for (i in count - 1 downTo 1) {
            val j = rng.nextInt(i + 1)  // Int, fine
            val tmp = perm[i]; perm[i] = perm[j]; perm[j] = tmp
        }
        // Chain: chain[i] = where to go after visiting i
        val chain = IntArray(count)
        for (k in 0 until count) chain[k] = perm[(k + 1) % count]

        var idx = 0; var sum = 0L; var ops = 0L
        val t0 = System.nanoTime()
        val endNs = t0 + durationMs * 1_000_000L
        while (System.nanoTime() < endNs) {
            idx = chain[idx]
            sum += buf[idx]
            ops++
        }
        val elapsedNs = System.nanoTime() - t0
        neverEliminate += sum
        return if (ops == 0L) 999.0 else elapsedNs.toDouble() / ops  // ns/op
    }

    /** System.arraycopy on a 64 MB buffer. Returns MB/s. */
    private fun benchMemCopy(durationMs: Long): Double {
        val size = 64 * 1024 * 1024
        val src = ByteArray(size) { (it % 251).toByte() }
        val dst = ByteArray(size)
        var totalBytes = 0L
        val endNs = System.nanoTime() + durationMs * 1_000_000L
        while (System.nanoTime() < endNs) {
            System.arraycopy(src, 0, dst, 0, size)
            totalBytes += size.toLong()
        }
        neverEliminate += dst[dst.size / 2].toLong()
        return totalBytes.toDouble() / (durationMs / 1000.0) / 1024.0 / 1024.0
    }

    /**
     * Multi-threaded read: each thread sums a private 16 MB LongArray (word reads).
     * Using LongArray instead of ByteArray removes JVM per-byte overhead and lets
     * ART vectorise the inner loop.  16 MB per thread > L2 so we hit L3 / DRAM.
     * Returns aggregate MB/s across all threads.
     */
    private fun benchMultiThread(durationMs: Long): Double {
        val threads = detectBigCoreCount()
        val longCount = 16 * 1024 * 1024 / 8     // 16 MB per thread as LongArray
        val sizeBytes = longCount * 8L
        // Allocate & touch pages BEFORE starting the clock
        val buffers = Array(threads) { t -> LongArray(longCount) { (it + t * 13L) } }
        val byteCounts = LongArray(threads)
        val endNs = System.nanoTime() + durationMs * 1_000_000L
        val latch = java.util.concurrent.CountDownLatch(threads)
        for (ti in 0 until threads) {
            Thread {
                val buf = buffers[ti]; var s = 0L; var total = 0L
                while (System.nanoTime() < endNs) {
                    for (v in buf) s += v         // word-reads
                    total += sizeBytes
                }
                neverEliminate += s; byteCounts[ti] = total; latch.countDown()
            }.also { it.isDaemon = true; it.priority = Thread.MAX_PRIORITY; it.start() }
        }
        latch.await()
        return byteCounts.sum().toDouble() / (durationMs / 1000.0) / 1024.0 / 1024.0
    }

    /** Prevents JIT from eliminating benchmark loops. */
    @Volatile private var neverEliminate = 0L

    // ── DB save ───────────────────────────────────────────────────────────

    private suspend fun saveToDatabase(
        results: List<RamTestResult>,
        totalScore: Int,
        performanceMetricsJson: String,
        detailedJson: String
    ) {
        val repo = historyRepository ?: return
        try {
            val avgBW = results.filter { !it.test.isLowerBetter() }
                .map { it.value }.average().takeIf { it.isFinite() } ?: 0.0
            val detailsArrayJson = try {
                JSONObject(detailedJson).optJSONArray("detailed_results")?.toString() ?: "[]"
            } catch (e: Exception) { "[]" }
            val entity = BenchmarkResultEntity(
                type                   = "RAM",
                totalScore             = totalScore.toDouble(),
                timestamp              = System.currentTimeMillis(),
                deviceModel            = "${Build.MANUFACTURER} ${Build.MODEL}",
                singleCoreScore        = 0.0,
                multiCoreScore         = avgBW,
                normalizedScore        = totalScore.toDouble(),
                detailedResultsJson    = detailsArrayJson,
                performanceMetricsJson = performanceMetricsJson
            )
            val details = results.map { r ->
                GenericTestDetailEntity(
                    resultId    = 0,
                    testName    = r.displayName,
                    score       = r.value,
                    metricsJson = """{"score":${r.score},"value":${"%.2f".format(r.value)},"unit":"${r.unit}"}"""
                )
            }
            repo.saveGenericBenchmark(entity, details)
        } catch (e: Exception) {
            android.util.Log.e("RamBenchmarkVM", "DB save failed: ${e.message}", e)
        }
    }

    // ── Result JSON ───────────────────────────────────────────────────────

    private fun buildResultJson(
        results: List<RamTestResult>,
        totalScore: Int,
        preset: String,
        performanceMetricsJson: String
    ): String {
        val avgBW = results.filter { !it.test.isLowerBetter() }
            .map { it.value }.average().takeIf { it.isFinite() } ?: 0.0
        val detailedArray = JSONArray()
        results.forEach { r ->
            detailedArray.put(JSONObject().apply {
                put("name", r.displayName)
                put("opsPerSecond", r.value)
                put("executionTimeMs", if (r.unit == "ns/op") r.value else 0.0)
                put("isValid", true)
                put("metricsJson", """{"score":${r.score},"value":${"%.2f".format(r.value)},"unit":"${r.unit}"}""")
            })
        }
        val perfObj = try { JSONObject(performanceMetricsJson) } catch (e: Exception) { JSONObject() }
        return JSONObject().apply {
            put("type", "RAM")
            put("preset", preset)
            put("final_score", totalScore.toDouble())
            put("normalized_score", totalScore.toDouble())
            put("single_core_score", 0.0)
            put("multi_core_score", avgBW)
            put("detailed_results", detailedArray)
            put("timestamp", System.currentTimeMillis())
            put("performance_metrics", perfObj)
        }.toString()
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun mockCpuTemp(): Float =
        (baseCpuTemp + (-2f..4f).random()).coerceIn(30f, 85f)

    private fun mockMemUsage(): Int = try {
        val rt = Runtime.getRuntime()
        ((rt.totalMemory() - rt.freeMemory()) / 1024 / 1024).toInt()
    } catch (e: Exception) { 0 }

    private fun ClosedFloatingPointRange<Float>.random(): Float =
        start + Math.random().toFloat() * (endInclusive - start)

    // ── Factory ───────────────────────────────────────────────────────────

    companion object {
        fun factory(
            historyRepository: HistoryRepository?,
            application: Application
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                RamBenchmarkViewModel(application, historyRepository) as T
        }
    }
}
