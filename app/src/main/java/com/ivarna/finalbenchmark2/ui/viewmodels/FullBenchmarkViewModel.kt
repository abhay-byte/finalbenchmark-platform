package com.ivarna.finalbenchmark2.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkCategory
import com.ivarna.finalbenchmark2.utils.PerformanceMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONObject
import kotlin.math.roundToInt

// ── Data models ──────────────────────────────────────────────────────────────

data class FullBenchmarkPhase(
    val category: BenchmarkCategory,
    val displayName: String,
    /** Fraction of the final 0-1000 score this category contributes (all sum to 1.0) */
    val weight: Float,
    /** Maximum raw `normalized_score` expected from this category (used to normalise to 0–100) */
    val maxScore: Double
)

/** Status of an individual phase shown in the phase-progress overlay. */
enum class PhaseStatus { PENDING, RUNNING, DONE }

data class FullBenchmarkState(
    val phases: List<FullBenchmarkPhase>,
    val currentPhaseIndex: Int = 0,
    /** Map of category → raw normalized_score collected from the sub-benchmark. */
    val scores: Map<BenchmarkCategory, Double> = emptyMap(),
    /** Map of category → full summary JSON from the sub-benchmark (for drill-down). */
    val phaseJsons: Map<BenchmarkCategory, String> = emptyMap(),
    val isComplete: Boolean = false,
    val overallScore: Int = 0,
    val grade: String = ""
) {
    val currentPhase: FullBenchmarkPhase? get() = phases.getOrNull(currentPhaseIndex)
    val totalPhases: Int get() = phases.size
    /** 0..1 overall progress (completed phases / total). */
    val overallProgress: Float
        get() = if (phases.isEmpty()) 0f else scores.size.toFloat() / phases.size.toFloat()

    fun statusOf(phase: FullBenchmarkPhase): PhaseStatus {
        val idx = phases.indexOf(phase)
        return when {
            scores.containsKey(phase.category) -> PhaseStatus.DONE
            idx == currentPhaseIndex -> PhaseStatus.RUNNING
            else -> PhaseStatus.PENDING
        }
    }
}

// ── ViewModel ────────────────────────────────────────────────────────────────

class FullBenchmarkViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        /**
         * Category weights: CPU 25%, AI 15%, GPU 25%, RAM 10%, Storage 10%, Productivity 15%.
         *
         * maxScore = reference value for each category.
         * Baseline scores: CPU=300, GPU=100, Storage=100, Productivity=100, AI=100, RAM=100.
         *
         * Formula: score = Σ( min(raw/maxScore, 1) × weight ) × 1000
         * Weights sum to 1.0 → max possible score = 1000 (perfect across all categories).
         */
        val PHASES = listOf(
            FullBenchmarkPhase(BenchmarkCategory.CPU,          "CPU Performance",     0.25f, 300.0),
            FullBenchmarkPhase(BenchmarkCategory.AI,           "AI / ML",             0.15f, 100.0),
            FullBenchmarkPhase(BenchmarkCategory.RAM,          "RAM Performance",     0.10f, 100.0),
            FullBenchmarkPhase(BenchmarkCategory.STORAGE,      "Storage Performance", 0.10f, 100.0),
            FullBenchmarkPhase(BenchmarkCategory.GPU,          "GPU Performance",     0.25f, 100.0),
            FullBenchmarkPhase(BenchmarkCategory.PRODUCTIVITY, "Productivity",        0.15f, 100.0),
        )

        /** Grade thresholds (0–1000 scale). */
        private fun gradeFor(score: Int): String = when {
            score >= 950 -> "A+"
            score >= 850 -> "A"
            score >= 750 -> "B+"
            score >= 650 -> "B"
            score >= 500 -> "C"
            score >= 300 -> "D"
            else         -> "F"
        }
    }

    private val _state = MutableStateFlow(FullBenchmarkState(phases = PHASES))
    val state: StateFlow<FullBenchmarkState> = _state.asStateFlow()

    /** Overall performance monitor spanning the entire full benchmark run. */
    private val performanceMonitor = PerformanceMonitor(application)

    // ── Public API ────────────────────────────────────────────────────────────

    /** Start overall performance monitoring — call when the full benchmark begins. */
    fun startMonitoring() {
        performanceMonitor.start()
    }

    /** Stop monitoring and return the metrics JSON string. */
    fun stopAndGetMetrics(): String = performanceMonitor.stop()

    /**
     * Record the result of one phase. Called by FullBenchmarkScreen when a sub-benchmark
     * emits its `onBenchmarkComplete` callback.
     */
    fun recordPhaseScore(category: BenchmarkCategory, summaryJson: String) {
        val rawScore = extractNormalizedScore(summaryJson)
        val newScores    = _state.value.scores    + (category to rawScore)
        val newPhaseJsons = _state.value.phaseJsons + (category to summaryJson)
        val nextIndex = _state.value.currentPhaseIndex + 1

        if (nextIndex >= PHASES.size) {
            val overall = calculateOverallScore(newScores)
            _state.update {
                it.copy(
                    scores       = newScores,
                    phaseJsons   = newPhaseJsons,
                    isComplete   = true,
                    overallScore = overall,
                    grade        = gradeFor(overall)
                )
            }
        } else {
            _state.update {
                it.copy(
                    scores           = newScores,
                    phaseJsons       = newPhaseJsons,
                    currentPhaseIndex = nextIndex
                )
            }
        }
    }

    /**
     * Build the final summary JSON that can be passed to ResultScreen or stored in history.
     * Includes `phase_details` (per-category summary JSONs) and `performance_metrics`.
     */
    fun buildFinalSummaryJson(performanceMetricsJson: String = "{}"): String {
        val s = _state.value
        val categoryScores = JSONObject()
        for ((cat, score) in s.scores) categoryScores.put(cat.name, score)

        // Per-phase summary JSONs for drill-down
        val phaseDetails = JSONObject()
        for ((cat, json) in s.phaseJsons) phaseDetails.put(cat.name, json)

        val perfMetricsObj = try {
            JSONObject(performanceMetricsJson)
        } catch (e: Exception) {
            JSONObject()
        }

        return JSONObject().apply {
            put("type",               "FULL")
            put("normalized_score",   s.overallScore.toDouble())
            put("final_score",        s.overallScore.toDouble())
            put("single_core_score",  0.0)
            put("multi_core_score",   s.overallScore.toDouble())
            put("grade",              s.grade)
            put("category_scores",    categoryScores)
            put("phase_details",      phaseDetails)
            put("performance_metrics", perfMetricsObj)
            put("timestamp",          System.currentTimeMillis())
        }.toString()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun extractNormalizedScore(json: String): Double = try {
        JSONObject(json).optDouble("normalized_score", 0.0)
    } catch (_: Exception) {
        0.0
    }

    /**
     * Normalise each category's raw score against its known maximum, apply weights,
     * and scale to a 0–1000 final score.
     */
    private fun calculateOverallScore(scores: Map<BenchmarkCategory, Double>): Int {
        var weightedSum = 0.0
        for (phase in PHASES) {
            val raw     = scores[phase.category] ?: 0.0
            val percent = (raw / phase.maxScore).coerceIn(0.0, 1.0)  // 0..1
            weightedSum += percent * phase.weight
        }
        // weightedSum is 0..1 (weights sum to 1); scale to 0-1000
        return (weightedSum * 1000.0).roundToInt().coerceAtLeast(0)
    }
}
