package com.zenithblue.fb2Pro.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenithblue.fb2Pro.cpuBenchmark.BenchmarkName
import com.zenithblue.fb2Pro.cpuBenchmark.KotlinBenchmarkManager
import com.zenithblue.fb2Pro.data.repository.HistoryRepository
import com.zenithblue.fb2Pro.ui.screens.comparison.BaseComparisonScreen
import com.zenithblue.fb2Pro.ui.screens.comparison.BenchmarkComparisonCard
import com.zenithblue.fb2Pro.ui.viewmodels.RankingItem

data class BenchmarkComparisonItem(
    val name: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val userScore: Double,
    val selectedScore: Double
)

@Composable
fun CpuComparisonScreen(
    selectedDeviceJson: String,
    historyRepository: HistoryRepository,
    onBackClick: () -> Unit
) {
    BaseComparisonScreen(
        category = "CPU",
        selectedDeviceJson = selectedDeviceJson,
        historyRepository = historyRepository,
        onBackClick = onBackClick,
        benchmarkContent = { userDevice, selectedDevice ->
            CpuBenchmarkContent(userDevice, selectedDevice)
        }
    )
}

@Composable
private fun CpuBenchmarkContent(
    userDevice: RankingItem?,
    selectedDevice: RankingItem?
) {
    if (selectedDevice == null) return

    val userColor = MaterialTheme.colorScheme.primary
    val selectedColor = MaterialTheme.colorScheme.secondary

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Single-Core Benchmarks",
            fontSize = 20.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        )
        cpuSingleCoreItems(userDevice, selectedDevice).forEach { benchmark ->
            BenchmarkComparisonCard(
                name = benchmark.name, icon = benchmark.icon,
                userScore = benchmark.userScore, selectedScore = benchmark.selectedScore,
                userColor = userColor, selectedColor = selectedColor,
                delayMillis = 0, animationDuration = 250
            )
        }

        Text(
            text = "Multi-Core Benchmarks",
            fontSize = 20.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
        )
        cpuMultiCoreItems(userDevice, selectedDevice).forEach { benchmark ->
            BenchmarkComparisonCard(
                name = benchmark.name, icon = benchmark.icon,
                userScore = benchmark.userScore, selectedScore = benchmark.selectedScore,
                userColor = userColor, selectedColor = selectedColor,
                delayMillis = 0, animationDuration = 250
            )
        }
    }
}

private fun calcScore(mops: Double, name: BenchmarkName): Double {
    return mops * 1_000_000.0 * (KotlinBenchmarkManager.SCORING_FACTORS[name] ?: 0.0)
}

private fun cpuSingleCoreItems(
    userDevice: RankingItem?, selectedDevice: RankingItem?
): List<BenchmarkComparisonItem> {
    val u = userDevice?.benchmarkDetails
    val s = selectedDevice?.benchmarkDetails
    return listOf(
        BenchmarkComparisonItem("Prime Generation", Icons.Rounded.Calculate,
            calcScore(u?.singleCorePrimeNumberMops ?: 0.0, BenchmarkName.PRIME_GENERATION),
            calcScore(s?.singleCorePrimeNumberMops ?: 0.0, BenchmarkName.PRIME_GENERATION)),
        BenchmarkComparisonItem("Fibonacci", Icons.Rounded.Functions,
            calcScore(u?.singleCoreFibonacciMops ?: 0.0, BenchmarkName.FIBONACCI_ITERATIVE),
            calcScore(s?.singleCoreFibonacciMops ?: 0.0, BenchmarkName.FIBONACCI_ITERATIVE)),
        BenchmarkComparisonItem("Matrix Multiplication", Icons.Rounded.GridOn,
            calcScore(u?.singleCoreMatrixMultiplicationMops ?: 0.0, BenchmarkName.MATRIX_MULTIPLICATION),
            calcScore(s?.singleCoreMatrixMultiplicationMops ?: 0.0, BenchmarkName.MATRIX_MULTIPLICATION)),
        BenchmarkComparisonItem("Hash Computing", Icons.Rounded.Lock,
            calcScore(u?.singleCoreHashComputingMops ?: 0.0, BenchmarkName.HASH_COMPUTING),
            calcScore(s?.singleCoreHashComputingMops ?: 0.0, BenchmarkName.HASH_COMPUTING)),
        BenchmarkComparisonItem("String Sorting", Icons.Rounded.SortByAlpha,
            calcScore(u?.singleCoreStringSortingMops ?: 0.0, BenchmarkName.STRING_SORTING),
            calcScore(s?.singleCoreStringSortingMops ?: 0.0, BenchmarkName.STRING_SORTING)),
        BenchmarkComparisonItem("Ray Tracing", Icons.Rounded.Lightbulb,
            calcScore(u?.singleCoreRayTracingMops ?: 0.0, BenchmarkName.RAY_TRACING),
            calcScore(s?.singleCoreRayTracingMops ?: 0.0, BenchmarkName.RAY_TRACING)),
        BenchmarkComparisonItem("Compression", Icons.Rounded.Compress,
            calcScore(u?.singleCoreCompressionMops ?: 0.0, BenchmarkName.COMPRESSION),
            calcScore(s?.singleCoreCompressionMops ?: 0.0, BenchmarkName.COMPRESSION)),
        BenchmarkComparisonItem("Monte Carlo", Icons.Rounded.Casino,
            calcScore(u?.singleCoreMonteCarloMops ?: 0.0, BenchmarkName.MONTE_CARLO),
            calcScore(s?.singleCoreMonteCarloMops ?: 0.0, BenchmarkName.MONTE_CARLO)),
        BenchmarkComparisonItem("JSON Parsing", Icons.Rounded.Code,
            calcScore(u?.singleCoreJsonParsingMops ?: 0.0, BenchmarkName.JSON_PARSING),
            calcScore(s?.singleCoreJsonParsingMops ?: 0.0, BenchmarkName.JSON_PARSING)),
        BenchmarkComparisonItem("N-Queens", Icons.Rounded.Dashboard,
            calcScore(u?.singleCoreNQueensMops ?: 0.0, BenchmarkName.N_QUEENS),
            calcScore(s?.singleCoreNQueensMops ?: 0.0, BenchmarkName.N_QUEENS))
    )
}

private fun cpuMultiCoreItems(
    userDevice: RankingItem?, selectedDevice: RankingItem?
): List<BenchmarkComparisonItem> {
    val u = userDevice?.benchmarkDetails
    val s = selectedDevice?.benchmarkDetails
    return listOf(
        BenchmarkComparisonItem("Prime Generation", Icons.Rounded.Calculate,
            calcScore(u?.multiCorePrimeNumberMops ?: 0.0, BenchmarkName.PRIME_GENERATION),
            calcScore(s?.multiCorePrimeNumberMops ?: 0.0, BenchmarkName.PRIME_GENERATION)),
        BenchmarkComparisonItem("Fibonacci", Icons.Rounded.Functions,
            calcScore(u?.multiCoreFibonacciMops ?: 0.0, BenchmarkName.FIBONACCI_ITERATIVE),
            calcScore(s?.multiCoreFibonacciMops ?: 0.0, BenchmarkName.FIBONACCI_ITERATIVE)),
        BenchmarkComparisonItem("Matrix Multiplication", Icons.Rounded.GridOn,
            calcScore(u?.multiCoreMatrixMultiplicationMops ?: 0.0, BenchmarkName.MATRIX_MULTIPLICATION),
            calcScore(s?.multiCoreMatrixMultiplicationMops ?: 0.0, BenchmarkName.MATRIX_MULTIPLICATION)),
        BenchmarkComparisonItem("Hash Computing", Icons.Rounded.Lock,
            calcScore(u?.multiCoreHashComputingMops ?: 0.0, BenchmarkName.HASH_COMPUTING),
            calcScore(s?.multiCoreHashComputingMops ?: 0.0, BenchmarkName.HASH_COMPUTING)),
        BenchmarkComparisonItem("String Sorting", Icons.Rounded.SortByAlpha,
            calcScore(u?.multiCoreStringSortingMops ?: 0.0, BenchmarkName.STRING_SORTING),
            calcScore(s?.multiCoreStringSortingMops ?: 0.0, BenchmarkName.STRING_SORTING)),
        BenchmarkComparisonItem("Ray Tracing", Icons.Rounded.Lightbulb,
            calcScore(u?.multiCoreRayTracingMops ?: 0.0, BenchmarkName.RAY_TRACING),
            calcScore(s?.multiCoreRayTracingMops ?: 0.0, BenchmarkName.RAY_TRACING)),
        BenchmarkComparisonItem("Compression", Icons.Rounded.Compress,
            calcScore(u?.multiCoreCompressionMops ?: 0.0, BenchmarkName.COMPRESSION),
            calcScore(s?.multiCoreCompressionMops ?: 0.0, BenchmarkName.COMPRESSION)),
        BenchmarkComparisonItem("Monte Carlo", Icons.Rounded.Casino,
            calcScore(u?.multiCoreMonteCarloMops ?: 0.0, BenchmarkName.MONTE_CARLO),
            calcScore(s?.multiCoreMonteCarloMops ?: 0.0, BenchmarkName.MONTE_CARLO)),
        BenchmarkComparisonItem("JSON Parsing", Icons.Rounded.Code,
            calcScore(u?.multiCoreJsonParsingMops ?: 0.0, BenchmarkName.JSON_PARSING),
            calcScore(s?.multiCoreJsonParsingMops ?: 0.0, BenchmarkName.JSON_PARSING)),
        BenchmarkComparisonItem("N-Queens", Icons.Rounded.Dashboard,
            calcScore(u?.multiCoreNQueensMops ?: 0.0, BenchmarkName.N_QUEENS),
            calcScore(s?.multiCoreNQueensMops ?: 0.0, BenchmarkName.N_QUEENS))
    )
}
