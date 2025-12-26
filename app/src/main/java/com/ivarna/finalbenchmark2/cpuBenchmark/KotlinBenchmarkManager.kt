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
import com.ivarna.finalbenchmark2.aiBenchmark.AiBenchmarkManager
import com.ivarna.finalbenchmark2.aiBenchmark.ModelRepository
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import android.graphics.Bitmap

class KotlinBenchmarkManager(
    private val context: Context? = null,
    private val aiManager: AiBenchmarkManager? = null
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
                
                // AI Scoring Factors (Placeholder / TBD - Target similar point range)
                // AI Scoring Factors (Target ~100 points per test for typical mid-range 50 TPS)
                BenchmarkName.LLM_INFERENCE to 2.0,
                BenchmarkName.IMAGE_CLASSIFICATION to 2.0,
                BenchmarkName.OBJECT_DETECTION to 2.0,
                BenchmarkName.TEXT_EMBEDDING to 2.0,
                BenchmarkName.SPEECH_TO_TEXT to 2.0
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
             // Placeholder for AI benchmarks
             // Simulate work for now to prevent crashes until actual implementation
             val categoryName = BenchmarkCategory.AI.name

             runTestWorkload() // Warmup

             val results = mutableListOf<BenchmarkResult>()
             
            if (context == null || aiManager == null) {
                Log.e(TAG, "AI Benchmark failed: Context or AiManager is null")
                return
            }

            val modelsDir = File(context.filesDir, "models")
            Log.d(TAG, "AI Benchmark: Looking for models in ${modelsDir.absolutePath}")
            val filesCallback = modelsDir.listFiles()
            if (filesCallback != null) {
                Log.d(TAG, "Files found in models dir: ${filesCallback.joinToString { it.name }}")
            } else {
                Log.e(TAG, "Models directory is empty or does not exist!")
            }

            val aiBenchmarks = BenchmarkName.getByCategory(BenchmarkCategory.AI)
             
             aiBenchmarks.forEach { benchmark ->
                val testName = benchmark.displayName()

                // Determine filename based on benchmark type
                val fileName = when(benchmark) {
                    BenchmarkName.IMAGE_CLASSIFICATION -> ModelRepository.MOBILENET_FILENAME
                    BenchmarkName.OBJECT_DETECTION -> ModelRepository.EFFICIENTDET_FILENAME
                    BenchmarkName.LLM_INFERENCE -> ModelRepository.GEMMA_FILENAME 
                    BenchmarkName.TEXT_EMBEDDING -> ModelRepository.MINILM_FILENAME
                    BenchmarkName.SPEECH_TO_TEXT -> ModelRepository.WHISPER_FILENAME
                    else -> ""
                }

                if (fileName.isEmpty()) {
                    Log.d(TAG, "Skipping $testName (No filename mapped or not implemented)")
                    // Add skipped result
                    results.add(BenchmarkResult(testName, 0.0, 0.0, false, "{\"error\": \"Not implemented\"}"))
                    // Emit fake start/complete to resolve UI Pending state
                    emitBenchmarkStart(testName, categoryName)
                    delay(100)
                    emitBenchmarkComplete(testName, categoryName, 0, 0.0) 
                    return@forEach
                }

                val modelFile = File(modelsDir, fileName)
                Log.d(TAG, "Checking model file: ${modelFile.absolutePath}, exists=${modelFile.exists()}, size=${if(modelFile.exists()) modelFile.length() else 0}")

                if (!modelFile.exists()) {
                    Log.w(TAG, "Model file not found: $fileName")
                     Log.d(TAG, "Skipping $testName (Model missing)")
                     // Add skipped result
                     results.add(BenchmarkResult(testName, 0.0, 0.0, false, "{\"error\": \"Model missing\"}"))
                     // Emit fake start/complete to resolve UI Pending state
                     emitBenchmarkStart(testName, categoryName)
                     delay(100)
                     emitBenchmarkComplete(testName, categoryName, 0, 0.0)
                    return@forEach 
                }
                 
                  emitBenchmarkStart(testName, categoryName)
                  
                  val startTime = System.currentTimeMillis()

                  // Execute via AiBenchmarkManager
                  val result = when(benchmark) {
                    BenchmarkName.IMAGE_CLASSIFICATION -> {
                         val dummyInput = aiManager.createDummyMobileNetInput()
                         aiManager.runImageClassification(modelFile, dummyInput)
                    }
                    BenchmarkName.OBJECT_DETECTION -> {
                         val dummyInput = aiManager.createDummyEfficientDetInput()
                         aiManager.runObjectDetection(modelFile, dummyInput)
                    }
                    BenchmarkName.TEXT_EMBEDDING -> {
                         aiManager.runTextEmbedding(modelFile)
                    }
                    BenchmarkName.SPEECH_TO_TEXT -> {
                         aiManager.runAsr(modelFile)
                    }
                    BenchmarkName.LLM_INFERENCE -> {
                         aiManager.runLlmInference(modelFile)
                    }
                    else -> com.ivarna.finalbenchmark2.aiBenchmark.AiBenchmarkResult(modelFile.name, 0.0, 0.0, "Skipped", false, "Not implemented")
                  }
                  
                  val endTime = System.currentTimeMillis()
                  val totalDurationMs = endTime - startTime

                  if (result.success) {
                      val multiplier = SCORING_FACTORS[benchmark] ?: 2.0
                      val score = result.throughput * multiplier
                      Log.d(TAG, "AI Result - $testName: Throughput=${result.throughput}, Time=${result.inferenceTimeMs}, Duration=${totalDurationMs}ms, Score=$score")
                      
                      results.add(BenchmarkResult(
                          name = testName,
                          executionTimeMs = totalDurationMs.toDouble(), // Use total duration for UI
                          opsPerSecond = result.throughput, 
                          isValid = true,
                          metricsJson = "{ \"acceleration\": \"${result.accelerationMode}\", \"avgInferenceTimeMs\": ${result.inferenceTimeMs} }",
                          accelerationMode = result.accelerationMode
                      ))
                      emitBenchmarkComplete(testName, categoryName, totalDurationMs, score, result.accelerationMode) 
                  } else {
                       Log.e(TAG, "Benchmark $testName failed: ${result.errorMessage}")
                       results.add(BenchmarkResult(testName, 0.0, 0.0, false, "{\"error\": \"${result.errorMessage}\"}"))
                       emitBenchmarkComplete(testName, categoryName, 0, 0.0)
                  }

                  delay(500)
             }
             
             // Calculate generic score: Simple Sum * Factor (2.0)
             // Typical TPS sum ~ 100-200. Score ~ 200-400.
             val totalScore = results.sumOf { it.opsPerSecond } * 2.0
             
             val detailedResultsArray = JSONArray()
             results.forEach { result ->
                 detailedResultsArray.put(JSONObject().apply {
                     put("name", result.name)
                     put("opsPerSecond", result.opsPerSecond)
                     put("executionTimeMs", result.executionTimeMs)
                     put("isValid", result.isValid)
                     put("metricsJson", result.metricsJson)
                     put("acceleration_mode", result.accelerationMode) // Ensure UI receives this
                 })
             }

             val summaryJson = JSONObject().apply {
                 put("type", "AI")
                 put("single_core_score", 0.0)
                 put("multi_core_score", 0.0) // Not applicable for AI in this simplified view
                 put("final_score", totalScore)
                 put("normalized_score", totalScore) // No normalization yet
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
                                                        put("opsPerSecond", result.opsPerSecond)
                                                        put(
                                                                "executionTimeMs",
                                                                result.executionTimeMs
                                                        )
                                                        put("isValid", result.isValid)
                                                        put("metricsJson", result.metricsJson)
                                                }
                                        )
                                }
                                // Add multi core results
                                multiResults.forEach { result ->
                                        put(
                                                JSONObject().apply {
                                                        put("name", result.name)
                                                        put("opsPerSecond", result.opsPerSecond)
                                                        put(
                                                                "executionTimeMs",
                                                                result.executionTimeMs
                                                        )
                                                        put("isValid", result.isValid)
                                                        put("metricsJson", result.metricsJson)
                                                }
                                        )
                                }
                        }

                return JSONObject()
                        .apply {
                                put("single_core_score", calculatedSingleCoreScore)
                                put("multi_core_score", calculatedMultiCoreScore)
                                put("final_score", calculatedFinalScore)
                                put("normalized_score", calculatedNormalizedScore)
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
                                        fibonacciIterations = 5_208_333,  // 0.25x mid
                                        matrixSize = 128,
                                        matrixIterations = 375,  // 0.25x mid
                                        hashDataSizeMb = 8,
                                        hashIterations = 65_687_500,  // 0.25x mid
                                        stringSortIterations = 625,  // 0.25x mid
                                        rayTracingIterations = 100,  // 0.25x mid
                                        rayTracingResolution = Pair(256, 256),
                                        rayTracingDepth = 5,
                                        compressionDataSizeMb = 2,
                                        compressionIterations = 250,  // 0.25x mid
                                        monteCarloSamples = 6_250_000L,  // 0.25x mid
                                        jsonDataSizeMb = 1,
                                        jsonParsingIterations = 312,  // 0.25x mid (rounded from 312.5)
                                        nqueensSize = 12  // N-Queens: -1 from mid
                                )
                        "mid" ->
                                WorkloadParams(
                                        primeRange = 490_000_000,  // 0.5x flagship
                                        fibonacciNRange = Pair(92, 92),
                                        fibonacciIterations = 20_833_333,  // 0.5x flagship (rounded from 20833333.5)
                                        matrixSize = 128,
                                        matrixIterations = 1_500,  // 0.5x flagship
                                        hashDataSizeMb = 8,
                                        hashIterations = 262_750_000,  // 0.5x flagship
                                        stringSortIterations = 2_500,  // 0.5x flagship
                                        rayTracingIterations = 400,  // 0.5x flagship
                                        rayTracingResolution = Pair(256, 256),
                                        rayTracingDepth = 5,
                                        compressionDataSizeMb = 2,
                                        compressionIterations = 1_000,  // 0.5x flagship
                                        monteCarloSamples = 25_000_000L,  // 0.5x flagship
                                        jsonDataSizeMb = 1,
                                        jsonParsingIterations = 1_250,  // 0.5x flagship
                                        nqueensSize = 15  // N-Queens: -1 from flagship
                                )
                        "flagship" ->
                                WorkloadParams(
                                        // CACHE-RESIDENT STRATEGY: Small matrices with high
                                        // iterations

                                        primeRange = 980_000_000,  // Miller-Rabin: ~40-50s
                                        fibonacciNRange = Pair(92, 92), // Use fixed max safe value
                                        fibonacciIterations = 41_666_667,  // Reduced 3x for polynomial
                                        matrixSize =
                                                128, // CACHE-RESIDENT: Fixed small size for cache
                                        // efficiency
                                        matrixIterations =
                                                3000, // CACHE-RESIDENT: High iterations for
                                        // flagship devices
                                        hashDataSizeMb = 8,
                                        hashIterations =
                                                525_500_000, // FIXED WORK PER CORE: Target ~1.5-2.0
                                        // seconds

                                        stringSortIterations =
                                                5_000, // CACHE-RESIDENT: Explicit control - target
                                        // ~1.0-2.0s
                                        rayTracingIterations =
                                                800, // FIXED: Increased to 2000 to reach ~5s
                                        // target
                                        // duration for better thermal testing
                                        rayTracingResolution =
                                                Pair(
                                                        256, // REVERTED: 256×256 caused thermal
                                                        // throttling
                                                        256
                                                ), // OPTIMIZED: Increased from 100×100 to 256×256
                                        // for better multi-core scaling
                                        rayTracingDepth = 5,
                                        compressionDataSizeMb = 2,
                                        compressionIterations =
                                                2_000, // INCREASED: Target ~15s execution (was
                                        // 100)
                                        monteCarloSamples =
                                                50_000_000L, // Reduced 100x for Mandelbrot Set (was 5B)
                                        jsonDataSizeMb = 1,
                                        jsonParsingIterations = 2500,  // Reduced 10x for CPU-bound parsing
                                        nqueensSize =
                                                16 // INCREASED: 14,200 solutions, ~20s (was 10)
                                )
                        else -> WorkloadParams() // Default values
                }
        }
}
