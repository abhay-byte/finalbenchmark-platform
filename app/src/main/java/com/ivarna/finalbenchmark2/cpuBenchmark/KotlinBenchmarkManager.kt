package com.ivarna.finalbenchmark2.cpuBenchmark

import android.util.Log
import com.ivarna.finalbenchmark2.cpuBenchmark.algorithms.MultiCoreBenchmarks
import com.ivarna.finalbenchmark2.cpuBenchmark.algorithms.SingleCoreBenchmarks
import com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

import android.content.Context
import com.ivarna.finalbenchmark2.aiBenchmark.AiBenchmarkNative
import com.ivarna.finalbenchmark2.aiBenchmark.AiBenchmarkResult
import com.ivarna.finalbenchmark2.utils.sanitizeDouble
import java.io.File
import android.graphics.Bitmap

class KotlinBenchmarkManager(
    private val context: Context? = null
) {
        private val _benchmarkEvents = MutableSharedFlow<BenchmarkEvent>(replay = 1)
        val benchmarkEvents: SharedFlow<BenchmarkEvent> = _benchmarkEvents.asSharedFlow()

        private val _benchmarkComplete = MutableSharedFlow<String>(replay = 1)
        val benchmarkComplete: SharedFlow<String> = _benchmarkComplete.asSharedFlow()

        companion object {
                private const val TAG = "KotlinBenchmarkManager"


        // Reference device: Snapdragon 8 Gen 3 (OnePlus Pad 2)
        // These are the baseline ops/s values used for geometric mean calculation
        // Note: Values are in ops/s, not Mops/s, to match benchmark result format
        val REFERENCE_MOPS = mapOf(
                BenchmarkName.PRIME_GENERATION to 757_430_000.0,           // 571.43 Mops/s (Pollard's Rho)
                BenchmarkName.FIBONACCI_ITERATIVE to 4_560_000.0,          // 4.56 Mops/s
                BenchmarkName.MATRIX_MULTIPLICATION to 3_876_440_000.0,    // 3876.44 Mops/s
                BenchmarkName.HASH_COMPUTING to 138_370_000.0,             // 138.37 Mops/s
                BenchmarkName.STRING_SORTING to 125_200_000.0,             // 125.20 Mops/s
                BenchmarkName.RAY_TRACING to 8_820_000.0,                 // 8.82 Mops/s (Perlin Noise)
                BenchmarkName.COMPRESSION to 758_880_000.0,                // 758.88 Mops/s
                BenchmarkName.MONTE_CARLO to 280_460_000.0,               // 229.46 Mops/s (Mandelbrot Set)
                BenchmarkName.JSON_PARSING to 188_503_800_000.0,            // 91503.80 Mops/s
                BenchmarkName.N_QUEENS to 166_790_000.0                    // 166.79 Mops/s
        )

        val SCORING_FACTORS =
        mapOf(
                // Target 20 / Performance (Mops/s)
                BenchmarkName.PRIME_GENERATION to 1.7985e-6/132.6,        // 20 / 2.90e6 ops/s        
                BenchmarkName.FIBONACCI_ITERATIVE to 4.365e-7*5,     // 20 / 22.91 Mops/s
                BenchmarkName.MATRIX_MULTIPLICATION to 1.56465e-8/7.2,  // 20 / 639.13 Mops/s
                BenchmarkName.HASH_COMPUTING to 2.778e-5/384,          // 20 / 0.36 Mops/s
                BenchmarkName.STRING_SORTING to 1.602e-7/2,          // 20 / 62.42 Mops/s
                BenchmarkName.RAY_TRACING to 4.902e-6/4.2,             // 20 / 2.04 Mops/s
                BenchmarkName.COMPRESSION to 1.5243e-8*0.92,            // 20 / 656.04 Mops/s
                BenchmarkName.MONTE_CARLO to 0.6125e-6/20,             // 20 / 16.32 Mops/s
                BenchmarkName.JSON_PARSING to 1.56e-6/28500,            // 20 / 6.41 Mops/s
                BenchmarkName.N_QUEENS to 2.011e-7/3.2,                // 20 / 66.18e6 ops/s
                
        // AI Scoring: Reference TPS values from Snapdragon 8 Gen 3 (OnePlus Pad 2)
                // Used for geometric mean calculation (same approach as CPU)
                BenchmarkName.LLM_INFERENCE to 2.0,             // placeholder – SD8G3 ~10 tok/s; will tune
                BenchmarkName.IMAGE_CLASSIFICATION to 2.0,
                BenchmarkName.OBJECT_DETECTION to 2.0,
                BenchmarkName.TEXT_EMBEDDING to 2.0,
                BenchmarkName.SPEECH_TO_TEXT to 2.0
        )

        // AI Baseline TPS: Calibrated from SD 8 Gen 3 (Adreno 750) NEON CPU measurements.
        // Reference device scores exactly 100. Faster devices (GPU path) score >100.
        // Calibrated 2026-05-31 with actual N values: Conv=224, Det=304, Text=384,
        // ASR=1024, LLM=512, YOLO=640, BERT=768, DTLN=512.
        val AI_REFERENCE_TPS = mapOf(
                BenchmarkName.IMAGE_CLASSIFICATION              to 1.4567e10, // Conv 224:   14567 MOPS/s
                BenchmarkName.OBJECT_DETECTION                  to 3.1734e10, // Det  320:   31734 MOPS/s
                BenchmarkName.TEXT_EMBEDDING                    to 5.6300e10, // Text 384:   56300 MOPS/s
                BenchmarkName.SPEECH_TO_TEXT                    to 5.4147e10, // ASR  1024:  54147 MOPS/s
                BenchmarkName.LLM_INFERENCE                     to 6.1562e10, // LLM  512:   61562 MOPS/s
                BenchmarkName.IMAGE_CLASSIFICATION_MOBILENET_V1 to 2.9203e10, // Conv 224v2: 29203 MOPS/s
                BenchmarkName.OBJECT_DETECTION_YOLO_V8          to 6.4350e10, // YOLO 640:   64350 MOPS/s
                BenchmarkName.TEXT_CLASSIFICATION_MOBILEBERT    to 6.2641e10, // BERT 768:   62641 MOPS/s
                BenchmarkName.AUDIO_NOISE_SUPPRESSION_DTLN      to 6.2280e10  // DTLN 512:   62280 MOPS/s
        )

        // Per-test scoring factors: factor = 100 / refTps → score = tps * factor
        // Baseline = SD 8 Gen 3 (Adreno 750) observed NEON CPU measurements.
        // Each test scores exactly 100 on the reference device.
        // Calibrated: 2026-05-31 with N=512 LLM, N=1024 ASR (actual sizes used in nativeRunBenchmark).
        val AI_PER_TEST_SCORING_FACTORS = mapOf(
                BenchmarkName.IMAGE_CLASSIFICATION              to (100.0 / 1.4567e10),  // Conv 224:   14567 MOPS/s
                BenchmarkName.OBJECT_DETECTION                  to (100.0 / 3.1734e10),  // Det  320:   31734 MOPS/s
                BenchmarkName.TEXT_EMBEDDING                    to (100.0 / 5.6300e10),  // Text 384:   56300 MOPS/s
                BenchmarkName.SPEECH_TO_TEXT                    to (100.0 / 5.4147e10),  // ASR  1024:  54147 MOPS/s
                BenchmarkName.LLM_INFERENCE                     to (100.0 / 6.1562e10),  // LLM  512:   61562 MOPS/s (N=512)
                BenchmarkName.IMAGE_CLASSIFICATION_MOBILENET_V1 to (100.0 / 2.9203e10),  // Conv 224v2: 29203 MOPS/s
                BenchmarkName.OBJECT_DETECTION_YOLO_V8          to (100.0 / 6.4350e10),  // YOLO 640:   64350 MOPS/s
                BenchmarkName.TEXT_CLASSIFICATION_MOBILEBERT    to (100.0 / 6.2641e10),  // BERT 768:   62641 MOPS/s
                BenchmarkName.AUDIO_NOISE_SUPPRESSION_DTLN      to (100.0 / 6.2280e10)   // DTLN 512:   62280 MOPS/s
        )

        }

        /**
         * Run test workload before actual benchmarks
         * - Uses minimal "test" workload parameters
         * - No delays between benchmarks
         * - Results are NOT recorded
         * - Purpose: Warm up device and stabilize performance
         */
        private suspend fun runTestWorkload() {
                Log.d(TAG, "=== STARTING TEST WORKLOAD (Warm-up) ===")
                val testParams = getWorkloadParams("test")
                val isTestRun = true

                // Emit test workload start event
                _benchmarkEvents.emit(
                        BenchmarkEvent(
                                testName = "Test Workload",
                                mode = "TEST",
                                state = "STARTED",
                                timeMs = 0,
                                score = 0.0
                        )
                )

                // Run all benchmarks with test parameters (no recording, no delays)
                try {
                        // Single-core test benchmarks
                        SingleCoreBenchmarks.primeGeneration(testParams, isTestRun)
                        SingleCoreBenchmarks.fibonacciRecursive(testParams, isTestRun)
                        SingleCoreBenchmarks.matrixMultiplication(testParams, isTestRun)
                        SingleCoreBenchmarks.hashComputing(testParams, isTestRun)
                        SingleCoreBenchmarks.stringSorting(testParams, isTestRun)
                        SingleCoreBenchmarks.rayTracing(testParams, isTestRun)
                        SingleCoreBenchmarks.compression(testParams, isTestRun)
                        SingleCoreBenchmarks.monteCarloPi(testParams, isTestRun)
                        SingleCoreBenchmarks.jsonParsing(testParams, isTestRun)
                        SingleCoreBenchmarks.nqueens(testParams, isTestRun)

                        // Multi-core test benchmarks
                        MultiCoreBenchmarks.primeGeneration(testParams, isTestRun)
                        MultiCoreBenchmarks.fibonacciRecursive(testParams, isTestRun)
                        MultiCoreBenchmarks.matrixMultiplication(testParams, isTestRun)
                        MultiCoreBenchmarks.hashComputing(testParams, isTestRun)
                        MultiCoreBenchmarks.stringSorting(testParams, isTestRun)
                        MultiCoreBenchmarks.rayTracing(testParams, isTestRun)
                        MultiCoreBenchmarks.compression(testParams, isTestRun)
                        MultiCoreBenchmarks.monteCarloPi(testParams, isTestRun)
                        MultiCoreBenchmarks.jsonParsing(testParams, isTestRun)
                        MultiCoreBenchmarks.nqueens(testParams, isTestRun)

                        Log.d(TAG, "=== TEST WORKLOAD COMPLETE ===")
                } catch (e: Exception) {
                        Log.w(TAG, "Test workload encountered error (non-critical): ${e.message}")
                }

                // Emit test workload complete event
                _benchmarkEvents.emit(
                        BenchmarkEvent(
                                testName = "Test Workload",
                                mode = "TEST",
                                state = "COMPLETED",
                                timeMs = 0,
                                score = 0.0
                        )
                )
        }

        suspend fun runBenchmarks(deviceTier: String = "Flagship", category: BenchmarkCategory = BenchmarkCategory.CPU) {
                Log.d(
                        TAG,
                        "SINGLE_SOURCE_OF_TRUTH: Starting benchmark execution with device tier: $deviceTier, Category: $category"
                )

                if (category == BenchmarkCategory.AI) {
                    Log.d(TAG, "Running AI Benchmarks")
                    runAiBenchmarks(deviceTier)
                } else {
                    Log.d(TAG, "Running CPU Benchmarks")
                    runCpuBenchmarks(deviceTier)
                }
        }

        private suspend fun runAiBenchmarks(deviceTier: String) {
            val categoryName = BenchmarkCategory.AI.name
            runTestWorkload() // Warmup
            val results = mutableListOf<BenchmarkResult>()

            // Init native AI benchmark engine (Vulkan→OpenCL→GLES→CPU)
            AiBenchmarkNative.init()

            // P3 FIX: Silent warmup run to trigger CPU/GPU boost state before timed tests.
            // Prevents first result (Conv 224×224) being skewed cold vs. warmed tests.
            // Result is discarded — no emit, no score impact.
            Log.d(TAG, "AI: running silent GPU warmup...")
            AiBenchmarkNative.runBenchmark(AiBenchmarkNative.IMAGE_CLASSIFICATION, 1, 1, "_warmup_")
            Log.d(TAG, "AI: warmup done, starting timed benchmarks")

            // Native C++ self-calibrates iterations to fill TARGET_MS per test.
            // iters/warmup params passed to JNI are ignored — kept for API compatibility.
            val iters = 1; val warmup = 1; val llmIters = 1; val asrIters = 1

            // Benchmark map
            data class AiConfig(val id: Int, val iters: Int, val warmup: Int)
            val benchmarks = listOf<Pair<BenchmarkName, AiConfig>>(
                BenchmarkName.IMAGE_CLASSIFICATION              to AiConfig(AiBenchmarkNative.IMAGE_CLASSIFICATION, iters, warmup),
                BenchmarkName.OBJECT_DETECTION                  to AiConfig(AiBenchmarkNative.OBJECT_DETECTION,     iters, warmup),
                BenchmarkName.TEXT_EMBEDDING                    to AiConfig(AiBenchmarkNative.TEXT_EMBEDDING,       iters, warmup),
                BenchmarkName.SPEECH_TO_TEXT                    to AiConfig(AiBenchmarkNative.SPEECH_TO_TEXT,       asrIters, warmup),
                BenchmarkName.LLM_INFERENCE                     to AiConfig(AiBenchmarkNative.LLM_INFERENCE,        llmIters, warmup),
                BenchmarkName.IMAGE_CLASSIFICATION_MOBILENET_V1 to AiConfig(AiBenchmarkNative.IMAGE_CLASSIFICATION, iters, warmup),
                BenchmarkName.OBJECT_DETECTION_YOLO_V8          to AiConfig(AiBenchmarkNative.YOLO_DETECTION,       iters, warmup),
                BenchmarkName.TEXT_CLASSIFICATION_MOBILEBERT    to AiConfig(AiBenchmarkNative.MOBILE_BERT,          iters, warmup),
                BenchmarkName.AUDIO_NOISE_SUPPRESSION_DTLN      to AiConfig(AiBenchmarkNative.DTLN,                 iters, warmup),
            )

            for ((benchmark, cfg) in benchmarks) {
                val testName = benchmark.displayName()
                emitBenchmarkStart(testName, categoryName)

                val result = AiBenchmarkNative.runBenchmark(cfg.id, cfg.iters, cfg.warmup, testName)

                // Sanitize before ANY use — native may return 0 on CPU fallback edge cases
                val safeMs  = if (result.inferenceTimeMs.isFinite() && result.inferenceTimeMs >= 0) result.inferenceTimeMs else 0.0
                val safeTps = if (result.computeFlops.isFinite()      && result.computeFlops      >  0) result.computeFlops      else 0.0
                val isOk    = result.success && safeTps > 0.0

                if (isOk) {
                    Log.i("FinalBenchmark", "PASS: $testName | TPS=$safeTps | ${result.accelerationMode}")
                    emitBenchmarkComplete(testName, categoryName, safeMs.toLong(), safeTps, result.accelerationMode)
                } else {
                    Log.e("FinalBenchmark", "FAIL: $testName | ${result.errorMessage ?: "tps=0"}")
                    emitBenchmarkComplete(testName, categoryName, 0, 0.0)
                }

                // metricsJson uses sanitizeDouble() — NEVER embed raw Double (risk of Infinity in JSON text)
                val safeAvgMs = sanitizeDouble(safeMs)
                results.add(BenchmarkResult(
                    name = testName,
                    executionTimeMs = safeMs,
                    opsPerSecond = safeTps,
                    isValid = isOk,
                    metricsJson = "{\"acceleration\":\"${result.accelerationMode}\",\"avgMs\":$safeAvgMs}",
                    accelerationMode = result.accelerationMode
                ))
                delay(50)
            }
             
              // Calculate AI score using geometric mean (same approach as CPU)
              // Ratio = deviceTPS / referenceTPS, then geometricMean * 100 = final score
              val validResults = results.filter { it.isValid && it.opsPerSecond > 0.0 && it.opsPerSecond.isFinite() }
              val totalScore = if (validResults.isNotEmpty()) {
                  val ratios = validResults.mapNotNull { result ->
                      val benchmarkName = BenchmarkName.fromString(result.name)
                      val refTps = benchmarkName?.let { AI_REFERENCE_TPS[it] }
                      if (refTps != null && refTps > 0.0) {
                          result.opsPerSecond / refTps
                      } else {
                          Log.w(TAG, "No AI reference TPS for ${result.name}, skipping in geometric mean")
                          null
                      }
                  }
                  if (ratios.isNotEmpty()) {
                      var product = 1.0
                      for (r in ratios) product *= r
                      val geometricMean = Math.pow(product, 1.0 / ratios.size)
                      geometricMean * 100.0  // Scale: SD8Gen3 = 100
                  } else {
                      validResults.sumOf { it.opsPerSecond } // Fallback if no reference values
                  }
              } else {
                  0.0
              }
             Log.d(TAG, "AI Score (geometric mean × 1000): $totalScore from ${validResults.size} valid results")
             
             val detailedResultsArray = JSONArray()
             results.forEach { result ->
                 detailedResultsArray.put(JSONObject().apply {
                     put("name", result.name)
                     put("opsPerSecond", sanitizeDouble(result.opsPerSecond))
                     put("executionTimeMs", sanitizeDouble(result.executionTimeMs))
                     put("isValid", result.isValid)
                     put("metricsJson", result.metricsJson)
                     put("acceleration_mode", result.accelerationMode)
                 })
             }

             val summaryJson = JSONObject().apply {
                 put("type", "AI")
                 put("single_core_score", 0.0)
                 put("multi_core_score", 0.0)
                 put("final_score", sanitizeDouble(totalScore))
                 put("normalized_score", sanitizeDouble(totalScore))
                 put("detailed_results", detailedResultsArray)
             }.toString()

             _benchmarkComplete.emit(summaryJson)
        }

        private suspend fun runCpuBenchmarks(deviceTier: String) {
                // Run test workload first (warm-up)
                runTestWorkload()

                val params = getWorkloadParams(deviceTier)

                // Log CPU topology
                CpuAffinityManager.logTopology()

                // Run single-core benchmarks
                val singleResults = mutableListOf<BenchmarkResult>()

                // Prime Generation
                emitBenchmarkStart(BenchmarkName.PRIME_GENERATION.singleCore(), "SINGLE")
                val singlePrimeResult =
                        safeBenchmarkRun(BenchmarkName.PRIME_GENERATION.singleCore()) {
                                SingleCoreBenchmarks.primeGeneration(params)
                        }
                singleResults.add(singlePrimeResult)
                emitBenchmarkComplete(
                        BenchmarkName.PRIME_GENERATION.singleCore(),
                        "SINGLE",
                        singlePrimeResult.executionTimeMs.toLong(),
                        singlePrimeResult.opsPerSecond
                )

                // Fibonacci Iterative
                emitBenchmarkStart(BenchmarkName.FIBONACCI_ITERATIVE.singleCore(), "SINGLE")
                val singleFibResult =
                        safeBenchmarkRun(BenchmarkName.FIBONACCI_ITERATIVE.singleCore()) {
                                SingleCoreBenchmarks.fibonacciRecursive(params)
                        }
                singleResults.add(singleFibResult)
                emitBenchmarkComplete(
                        BenchmarkName.FIBONACCI_ITERATIVE.singleCore(),
                        "SINGLE",
                        singleFibResult.executionTimeMs.toLong(),
                        singleFibResult.opsPerSecond
                )

                // Matrix Multiplication
                emitBenchmarkStart(BenchmarkName.MATRIX_MULTIPLICATION.singleCore(), "SINGLE")
                val singleMatrixResult =
                        safeBenchmarkRun(BenchmarkName.MATRIX_MULTIPLICATION.singleCore()) {
                                SingleCoreBenchmarks.matrixMultiplication(params)
                        }
                singleResults.add(singleMatrixResult)
                emitBenchmarkComplete(
                        BenchmarkName.MATRIX_MULTIPLICATION.singleCore(),
                        "SINGLE",
                        singleMatrixResult.executionTimeMs.toLong(),
                        singleMatrixResult.opsPerSecond
                )

                // Hash Computing
                emitBenchmarkStart(BenchmarkName.HASH_COMPUTING.singleCore(), "SINGLE")
                val singleHashResult =
                        safeBenchmarkRun(BenchmarkName.HASH_COMPUTING.singleCore()) {
                                SingleCoreBenchmarks.hashComputing(params)
                        }
                singleResults.add(singleHashResult)
                emitBenchmarkComplete(
                        BenchmarkName.HASH_COMPUTING.singleCore(),
                        "SINGLE",
                        singleHashResult.executionTimeMs.toLong(),
                        singleHashResult.opsPerSecond
                )

                // String Sorting
                emitBenchmarkStart(BenchmarkName.STRING_SORTING.singleCore(), "SINGLE")
                val singleStringResult =
                        safeBenchmarkRun(BenchmarkName.STRING_SORTING.singleCore()) {
                                SingleCoreBenchmarks.stringSorting(params)
                        }
                singleResults.add(singleStringResult)
                emitBenchmarkComplete(
                        BenchmarkName.STRING_SORTING.singleCore(),
                        "SINGLE",
                        singleStringResult.executionTimeMs.toLong(),
                        singleStringResult.opsPerSecond
                )

                // Ray Tracing
                emitBenchmarkStart(BenchmarkName.RAY_TRACING.singleCore(), "SINGLE")
                val singleRayResult =
                        safeBenchmarkRun(BenchmarkName.RAY_TRACING.singleCore()) {
                                SingleCoreBenchmarks.rayTracing(params)
                        }
                singleResults.add(singleRayResult)
                emitBenchmarkComplete(
                        BenchmarkName.RAY_TRACING.singleCore(),
                        "SINGLE",
                        singleRayResult.executionTimeMs.toLong(),
                        singleRayResult.opsPerSecond
                )

                // Compression
                emitBenchmarkStart(BenchmarkName.COMPRESSION.singleCore(), "SINGLE")
                val singleCompressionResult =
                        safeBenchmarkRun(BenchmarkName.COMPRESSION.singleCore()) {
                                SingleCoreBenchmarks.compression(params)
                        }
                singleResults.add(singleCompressionResult)
                emitBenchmarkComplete(
                        BenchmarkName.COMPRESSION.singleCore(),
                        "SINGLE",
                        singleCompressionResult.executionTimeMs.toLong(),
                        singleCompressionResult.opsPerSecond
                )

                // Monte Carlo Pi
                emitBenchmarkStart(BenchmarkName.MONTE_CARLO.singleCore(), "SINGLE")
                val singleMonteResult =
                        safeBenchmarkRun(BenchmarkName.MONTE_CARLO.singleCore()) {
                                SingleCoreBenchmarks.monteCarloPi(params)
                        }
                singleResults.add(singleMonteResult)
                emitBenchmarkComplete(
                        BenchmarkName.MONTE_CARLO.singleCore(),
                        "SINGLE",
                        singleMonteResult.executionTimeMs.toLong(),
                        singleMonteResult.opsPerSecond
                )

                // JSON Parsing
                emitBenchmarkStart(BenchmarkName.JSON_PARSING.singleCore(), "SINGLE")
                val singleJsonResult =
                        safeBenchmarkRun(BenchmarkName.JSON_PARSING.singleCore()) {
                                SingleCoreBenchmarks.jsonParsing(params)
                        }
                singleResults.add(singleJsonResult)
                emitBenchmarkComplete(
                        BenchmarkName.JSON_PARSING.singleCore(),
                        "SINGLE",
                        singleJsonResult.executionTimeMs.toLong(),
                        singleJsonResult.opsPerSecond
                )

                // N-Queens
                emitBenchmarkStart(BenchmarkName.N_QUEENS.singleCore(), "SINGLE")
                val singleNqueensResult =
                        safeBenchmarkRun(BenchmarkName.N_QUEENS.singleCore()) {
                                SingleCoreBenchmarks.nqueens(params)
                        }
                singleResults.add(singleNqueensResult)
                emitBenchmarkComplete(
                        BenchmarkName.N_QUEENS.singleCore(),
                        "SINGLE",
                        singleNqueensResult.executionTimeMs.toLong(),
                        singleNqueensResult.opsPerSecond
                )

                // Run multi-core benchmarks
                val multiResults = mutableListOf<BenchmarkResult>()

                // Prime Generation
                emitBenchmarkStart(BenchmarkName.PRIME_GENERATION.multiCore(), "MULTI")
                val multiPrimeResult =
                        safeBenchmarkRun(BenchmarkName.PRIME_GENERATION.multiCore()) {
                                MultiCoreBenchmarks.primeGeneration(params)
                        }
                multiResults.add(multiPrimeResult)
                emitBenchmarkComplete(
                        BenchmarkName.PRIME_GENERATION.multiCore(),
                        "MULTI",
                        multiPrimeResult.executionTimeMs.toLong(),
                        multiPrimeResult.opsPerSecond
                )

                // Fibonacci Iterative
                emitBenchmarkStart(BenchmarkName.FIBONACCI_ITERATIVE.multiCore(), "MULTI")
                val multiFibResult =
                        safeBenchmarkRun(BenchmarkName.FIBONACCI_ITERATIVE.multiCore()) {
                                MultiCoreBenchmarks.fibonacciRecursive(params)
                        }
                multiResults.add(multiFibResult)
                emitBenchmarkComplete(
                        BenchmarkName.FIBONACCI_ITERATIVE.multiCore(),
                        "MULTI",
                        multiFibResult.executionTimeMs.toLong(),
                        multiFibResult.opsPerSecond
                )

                // Matrix Multiplication
                emitBenchmarkStart(BenchmarkName.MATRIX_MULTIPLICATION.multiCore(), "MULTI")
                val multiMatrixResult =
                        safeBenchmarkRun(BenchmarkName.MATRIX_MULTIPLICATION.multiCore()) {
                                MultiCoreBenchmarks.matrixMultiplication(params)
                        }
                multiResults.add(multiMatrixResult)
                emitBenchmarkComplete(
                        BenchmarkName.MATRIX_MULTIPLICATION.multiCore(),
                        "MULTI",
                        multiMatrixResult.executionTimeMs.toLong(),
                        multiMatrixResult.opsPerSecond
                )

                // Hash Computing
                emitBenchmarkStart(BenchmarkName.HASH_COMPUTING.multiCore(), "MULTI")
                val multiHashResult =
                        safeBenchmarkRun(BenchmarkName.HASH_COMPUTING.multiCore()) {
                                MultiCoreBenchmarks.hashComputing(params)
                        }
                multiResults.add(multiHashResult)
                emitBenchmarkComplete(
                        BenchmarkName.HASH_COMPUTING.multiCore(),
                        "MULTI",
                        multiHashResult.executionTimeMs.toLong(),
                        multiHashResult.opsPerSecond
                )

                // String Sorting
                emitBenchmarkStart(BenchmarkName.STRING_SORTING.multiCore(), "MULTI")
                val multiStringResult =
                        safeBenchmarkRun(BenchmarkName.STRING_SORTING.multiCore()) {
                                MultiCoreBenchmarks.stringSorting(params)
                        }
                multiResults.add(multiStringResult)
                emitBenchmarkComplete(
                        BenchmarkName.STRING_SORTING.multiCore(),
                        "MULTI",
                        multiStringResult.executionTimeMs.toLong(),
                        multiStringResult.opsPerSecond
                )

                // Ray Tracing
                emitBenchmarkStart(BenchmarkName.RAY_TRACING.multiCore(), "MULTI")
                val multiRayResult =
                        safeBenchmarkRun(BenchmarkName.RAY_TRACING.multiCore()) {
                                MultiCoreBenchmarks.rayTracing(params)
                        }
                multiResults.add(multiRayResult)
                emitBenchmarkComplete(
                        BenchmarkName.RAY_TRACING.multiCore(),
                        "MULTI",
                        multiRayResult.executionTimeMs.toLong(),
                        multiRayResult.opsPerSecond
                )

                // Compression
                emitBenchmarkStart(BenchmarkName.COMPRESSION.multiCore(), "MULTI")
                val multiCompressionResult =
                        safeBenchmarkRun(BenchmarkName.COMPRESSION.multiCore()) {
                                MultiCoreBenchmarks.compression(params)
                        }
                multiResults.add(multiCompressionResult)
                emitBenchmarkComplete(
                        BenchmarkName.COMPRESSION.multiCore(),
                        "MULTI",
                        multiCompressionResult.executionTimeMs.toLong(),
                        multiCompressionResult.opsPerSecond
                )

                // Monte Carlo Pi
                emitBenchmarkStart(BenchmarkName.MONTE_CARLO.multiCore(), "MULTI")
                val multiMonteResult =
                        safeBenchmarkRun(BenchmarkName.MONTE_CARLO.multiCore()) {
                                MultiCoreBenchmarks.monteCarloPi(params)
                        }
                multiResults.add(multiMonteResult)
                emitBenchmarkComplete(
                        BenchmarkName.MONTE_CARLO.multiCore(),
                        "MULTI",
                        multiMonteResult.executionTimeMs.toLong(),
                        multiMonteResult.opsPerSecond
                )

                // JSON Parsing
                emitBenchmarkStart(BenchmarkName.JSON_PARSING.multiCore(), "MULTI")
                val multiJsonResult =
                        safeBenchmarkRun(BenchmarkName.JSON_PARSING.multiCore()) {
                                MultiCoreBenchmarks.jsonParsing(params)
                        }
                multiResults.add(multiJsonResult)
                emitBenchmarkComplete(
                        BenchmarkName.JSON_PARSING.multiCore(),
                        "MULTI",
                        multiJsonResult.executionTimeMs.toLong(),
                        multiJsonResult.opsPerSecond
                )

                // N-Queens
                emitBenchmarkStart(BenchmarkName.N_QUEENS.multiCore(), "MULTI")
                val multiNqueensResult =
                        safeBenchmarkRun(BenchmarkName.N_QUEENS.multiCore()) {
                                MultiCoreBenchmarks.nqueens(params)
                        }
                multiResults.add(multiNqueensResult)
                emitBenchmarkComplete(
                        BenchmarkName.N_QUEENS.multiCore(),
                        "MULTI",
                        multiNqueensResult.executionTimeMs.toLong(),
                        multiNqueensResult.opsPerSecond
                )

                // Calculate and emit final results
                val summaryJson = calculateSummary(singleResults, multiResults)
                Log.d(TAG, "SINGLE_SOURCE_OF_TRUTH: Generated summary JSON: $summaryJson")
                Log.d(
                        TAG,
                        "SINGLE_SOURCE_OF_TRUTH: Emitting completion signal with calculated scores"
                )
                _benchmarkComplete.emit(summaryJson)
                Log.d(TAG, "SINGLE_SOURCE_OF_TRUTH: Completion signal emitted successfully")
        }

        private suspend fun safeBenchmarkRun(
                testName: String,
                block: suspend () -> BenchmarkResult
        ): BenchmarkResult {
                return try {
                        withContext(Dispatchers.Default) {
                                val result = block()
                                Log.d(
                                        TAG,
                                        "✓ $testName completed successfully: ${result.opsPerSecond} ops/sec"
                                )
                                result
                        }
                } catch (e: Exception) {
                        Log.e(TAG, "✗ $testName failed with exception: ${e.message}", e)
                        // Return a dummy result so the benchmark suite can continue
                        BenchmarkResult(
                                name = testName,
                                executionTimeMs = 0.0,
                                opsPerSecond = 0.0,
                                isValid = false,
                                metricsJson = "{\"error\": \"${e.message}\"}"
                        )
                }
        }

        /**
         * Calculate geometric mean score for benchmark results.
         * 
         * Formula: GeometricMean = (∏ ratios)^(1/n)
         * where ratio = SUT_Mops / Reference_Mops
         * 
         * This prevents one fast benchmark from dominating the score and provides
         * fair comparison across different device architectures.
         * 
         * @param results List of benchmark results
         * @return Geometric mean score scaled to 100-point baseline (SD 8 Gen 3 = 100)
         */
        private fun calculateGeometricMean(results: List<BenchmarkResult>): Double {
                if (results.isEmpty()) return 0.0
                
                // Calculate performance ratios for each benchmark
                val ratios = results.mapNotNull { result ->
                        val benchmarkName = BenchmarkName.fromString(result.name)
                        val refMops = benchmarkName?.let { REFERENCE_MOPS[it] }
                        
                        if (refMops != null && refMops > 0.0) {
                                // Ratio = SUT performance / Reference performance
                                result.opsPerSecond / refMops
                        } else {
                                Log.w(TAG, "No reference value for ${result.name}, skipping in geometric mean")
                                null
                        }
                }
                
                if (ratios.isEmpty()) {
                        Log.e(TAG, "No valid ratios calculated for geometric mean")
                        return 0.0
                }
                
                // Calculate geometric mean: (product)^(1/n)
                var product = 1.0
                for (ratio in ratios) {
                        product *= ratio
                }
                
                val geometricMean = Math.pow(product, 1.0 / ratios.size)
                
                // Scale to 100-point baseline (SD 8 Gen 3 = 100)
                val score = geometricMean * 100.0
                
                Log.d(TAG, "Geometric mean calculation: ${ratios.size} benchmarks, GM=${geometricMean}, Score=${score}")
                
                return score
        }

        private fun calculateSummary(
                singleResults: List<BenchmarkResult>,
                multiResults: List<BenchmarkResult>
        ): String {
                // Calculate single-core score using geometric mean
                val calculatedSingleCoreScore = calculateGeometricMean(singleResults)

                // Calculate multi-core score using geometric mean
                val calculatedMultiCoreScore = calculateGeometricMean(multiResults)

                // Calculate final weighted score (35% single, 65% multi)
                val calculatedFinalScore =
                        (calculatedSingleCoreScore * 0.35) + (calculatedMultiCoreScore * 0.65)

                // Normalize the score to a reasonable range
                val calculatedNormalizedScore = calculatedFinalScore

                // Determine rating based on normalized score
                val rating =
                        when {
                                calculatedNormalizedScore >= 1600.0 ->
                                        "★★★★★ (Exceptional Performance)"
                                calculatedNormalizedScore >= 1200.0 -> "★★★★☆ (High Performance)"
                                calculatedNormalizedScore >= 800.0 -> "★★★☆☆ (Good Performance)"
                                calculatedNormalizedScore >= 500.0 -> "★★☆☆☆ (Moderate Performance)"
                                calculatedNormalizedScore >= 250.0 -> "★☆☆☆☆ (Basic Performance)"
                                else -> "☆☆☆☆☆ (Low Performance)"
                        }

                Log.d(
                        TAG,
                        "Final scoring - Single: $calculatedSingleCoreScore, Multi: $calculatedMultiCoreScore, Final: $calculatedFinalScore, Normalized: $calculatedNormalizedScore"
                )

                // CRITICAL FIX: Validation to ensure multi-core scores are higher than single-core
                // scores
                if (calculatedMultiCoreScore <= calculatedSingleCoreScore) {
                        Log.w(
                                TAG,
                                "WARNING: Multi-core score ($calculatedMultiCoreScore) is not higher than single-core score ($calculatedSingleCoreScore)"
                        )
                        Log.w(
                                TAG,
                                "This indicates a critical issue with benchmark implementations or scaling factors"
                        )
                } else {
                        Log.i(
                                TAG,
                                "✓ VALIDATED: Multi-core score ($calculatedMultiCoreScore) is higher than single-core score ($calculatedSingleCoreScore)"
                        )
                        Log.i(
                                TAG,
                                "Multi-core advantage: ${String.format("%.2fx", calculatedMultiCoreScore / calculatedSingleCoreScore)}"
                        )
                }

                // CRITICAL FIX: Include detailed_results array that ResultScreen expects
                val detailedResultsArray =
                        JSONArray().apply {
                                // Add single core results
                                singleResults.forEach { result ->
                                        put(
                                                JSONObject().apply {
                                                        put("name", result.name)
                                                        put("opsPerSecond", sanitizeDouble(result.opsPerSecond))
                                                        put(
                                                                "executionTimeMs",
                                                                sanitizeDouble(result.executionTimeMs)
                                                        )
                                                        put("isValid", result.isValid)
                                                        put("metricsJson", result.metricsJson)
                                                        put("acceleration_mode", result.accelerationMode ?: "Unknown")
                                                }
                                        )
                                }
                                // Add multi core results
                                multiResults.forEach { result ->
                                        put(
                                                JSONObject().apply {
                                                        put("name", result.name)
                                                        put("opsPerSecond", sanitizeDouble(result.opsPerSecond))
                                                        put(
                                                                "executionTimeMs",
                                                                sanitizeDouble(result.executionTimeMs)
                                                        )
                                                        put("isValid", result.isValid)
                                                        put("metricsJson", result.metricsJson)
                                                        put("acceleration_mode", result.accelerationMode ?: "Unknown")
                                                }
                                        )
                                }
                        }

                return JSONObject()
                        .apply {
                                put("single_core_score", sanitizeDouble(calculatedSingleCoreScore))
                                put("multi_core_score", sanitizeDouble(calculatedMultiCoreScore))
                                put("final_score", sanitizeDouble(calculatedFinalScore))
                                put("normalized_score", sanitizeDouble(calculatedNormalizedScore))
                                put("rating", rating)
                                put("detailed_results", detailedResultsArray)
                        }
                        .toString()
        }

        private suspend fun emitBenchmarkStart(testName: String, mode: String) {
                _benchmarkEvents.emit(
                        BenchmarkEvent(
                                testName = testName,
                                mode = mode,
                                state = "STARTED",
                                timeMs = 0,
                                score = 0.0
                        )
                )
        }

        private suspend fun emitBenchmarkComplete(
                testName: String,
                mode: String,
                timeMs: Long,
                score: Double,
                accelerationMode: String? = null
        ) {
                _benchmarkEvents.emit(
                        BenchmarkEvent(
                                testName = testName,
                                mode = mode,
                                state = "COMPLETED",
                                timeMs = timeMs,
                                score = score,
                                accelerationMode = accelerationMode
                        )
                )
        }

        private fun getWorkloadParams(deviceTier: String): WorkloadParams {
                return when (deviceTier.lowercase()) {
                        "test" ->
                                WorkloadParams(
                                        primeRange = 12_250_000,  // 0.1x slow
                                        fibonacciNRange = Pair(92, 92),
                                        fibonacciIterations = 520_833,  // 0.1x slow
                                        matrixSize = 128,
                                        matrixIterations = 37,  // 0.1x slow (rounded from 37.5)
                                        hashDataSizeMb = 8,
                                        hashIterations = 6_568_750,  // 0.1x slow
                                        stringSortIterations = 62,  // 0.1x slow (rounded from 62.5)
                                        rayTracingIterations = 10,  // 0.1x slow
                                        rayTracingResolution = Pair(256, 256),
                                        rayTracingDepth = 5,
                                        compressionDataSizeMb = 2,
                                        compressionIterations = 25,  // 0.1x slow
                                        monteCarloSamples = 625_000L,  // 0.1x slow
                                        jsonDataSizeMb = 1,
                                        jsonParsingIterations = 50,  // 0.1x slow (rounded from 31.25)
                                        nqueensSize = 12  // N-Queens: keep same as slow
                                )
                        "slow" ->
                                WorkloadParams(
                                        primeRange = 122_500_000,  // 0.25x mid
                                        fibonacciNRange = Pair(92, 92),
                                        fibonacciIterations = 10_416_667,  // 2×
                                        matrixSize = 128,
                                        matrixIterations = 750,  // 2×
                                        hashDataSizeMb = 8,
                                        hashIterations = 131_375_000,  // 2×
                                        stringSortIterations = 1_250,  // 2×
                                        rayTracingIterations = 200,  // 2×
                                        rayTracingResolution = Pair(256, 256),
                                        rayTracingDepth = 5,
                                        compressionDataSizeMb = 2,
                                        compressionIterations = 500,  // 2×
                                        monteCarloSamples = 12_500_000L,  // 2×
                                        jsonDataSizeMb = 1,
                                        jsonParsingIterations = 625,  // 2×
                                        nqueensSize = 13  // +1
                                )
                        "mid" ->
                                WorkloadParams(
                                        primeRange = 490_000_000,  // 0.5x flagship
                                        fibonacciNRange = Pair(92, 92),
                                        fibonacciIterations = 10_416_666,  // Restored
                                        matrixSize = 128,
                                        matrixIterations = 750,  // Restored
                                        hashDataSizeMb = 8,
                                        hashIterations = 131_375_000,  // Restored
                                        stringSortIterations = 1_250,  // Restored
                                        rayTracingIterations = 200,  // Restored
                                        rayTracingResolution = Pair(256, 256),
                                        rayTracingDepth = 5,
                                        compressionDataSizeMb = 2,
                                        compressionIterations = 500,  // Restored
                                        monteCarloSamples = 12_500_000L,  // Restored
                                        jsonDataSizeMb = 1,
                                        jsonParsingIterations = 625,  // Restored
                                        nqueensSize = 13  // Restored
                                )
                        "flagship" ->
                                WorkloadParams(
                                        primeRange = 980_000_000,  // Miller-Rabin: ~40-50s
                                        fibonacciNRange = Pair(92, 92),
                                        fibonacciIterations = 41_666_667,  // Restored
                                        matrixSize = 128,
                                        matrixIterations = 3000,  // Restored
                                        hashDataSizeMb = 8,
                                        hashIterations = 525_500_000,  // Restored
                                        stringSortIterations = 5_000,  // Restored
                                        rayTracingIterations = 800,  // Restored
                                        rayTracingResolution = Pair(256, 256),
                                        rayTracingDepth = 5,
                                        compressionDataSizeMb = 2,
                                        compressionIterations = 2_000,  // Restored
                                        monteCarloSamples = 50_000_000L,  // Restored
                                        jsonDataSizeMb = 1,
                                        jsonParsingIterations = 2500,  // Restored
                                        nqueensSize = 16  // Restored
                                )
                        else -> WorkloadParams() // Default values
                }
        }
}
