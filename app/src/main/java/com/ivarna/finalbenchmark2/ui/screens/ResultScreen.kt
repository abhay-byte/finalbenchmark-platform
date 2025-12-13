package com.ivarna.finalbenchmark2.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkName
import com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkResult
import com.ivarna.finalbenchmark2.ui.theme.FinalBenchmark2Theme
import com.ivarna.finalbenchmark2.ui.theme.GruvboxDarkAccent
import com.ivarna.finalbenchmark2.ui.viewmodels.RankingItem
import com.ivarna.finalbenchmark2.utils.DeviceInfoCollector
import com.ivarna.finalbenchmark2.utils.GpuInfoState
import com.ivarna.finalbenchmark2.utils.GpuInfoUtils
import com.ivarna.finalbenchmark2.utils.formatBytes
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

data class BenchmarkSummary(
        val singleCoreScore: Double,
        val multiCoreScore: Double,
        val finalScore: Double,
        val normalizedScore: Double,
        val detailedResults: List<BenchmarkResult> = emptyList(),
        val deviceSummary: BenchmarkDeviceSummary? = null,
        val timestamp: Long = System.currentTimeMillis(),
        val performanceMetricsJson: String = ""
)

// Scaling factors for converting performance (ops/s) to score
// These must match the factors in KotlinBenchmarkManager.kt
private val SINGLE_CORE_FACTORS =
        mapOf(
                BenchmarkName.PRIME_GENERATION to 6.78e-6,
                BenchmarkName.FIBONACCI_ITERATIVE to 0.836e-6, // 20 / 23.94e6
                BenchmarkName.MATRIX_MULTIPLICATION to 3.640e-8,   // 20 / 549.52e6 ops/s        BenchmarkName.HASH_COMPUTING to 2.70e-5,          // 20 / 0.74e6
                BenchmarkName.HASH_COMPUTING to 5.714e-5, 
                BenchmarkName.STRING_SORTING to 3.09e-7, // 20 / 64.71e6
                BenchmarkName.RAY_TRACING to 7.63e-6, // 20 / 2.62e6
                BenchmarkName.COMPRESSION to 2.82e-8, // 20 / 709.46e6
                BenchmarkName.MONTE_CARLO to 1.11e-6, // 20 / 18.05e6
                BenchmarkName.JSON_PARSING to 2.79e-6, // 20 / 7.16e6
                BenchmarkName.N_QUEENS to 2.83e-7 // 20 / 70.83e6
        )

private val MULTI_CORE_FACTORS =
        mapOf(
                BenchmarkName.PRIME_GENERATION to 8.77e-6, // 100 / 11.40e6
                BenchmarkName.FIBONACCI_ITERATIVE to 8.57e-7, // 100 / 116.71e6
                BenchmarkName.MATRIX_MULTIPLICATION to 1.80e-8, // 100 / 5563.97e6
                BenchmarkName.HASH_COMPUTING to 3.40e-5, // 100 / 2.94e6
                BenchmarkName.STRING_SORTING to 5.70e-7, // 100 / 175.43e6
                BenchmarkName.RAY_TRACING to 1.91e-5, // 100 / 5.24e6
                BenchmarkName.COMPRESSION to 4.27e-8, // 100 / 2342.86e6
                BenchmarkName.MONTE_CARLO to 1.49e-6, // 100 / 67.21e6
                BenchmarkName.JSON_PARSING to 4.26e-6, // 100 / 23.45e6
                BenchmarkName.N_QUEENS to 4.01e-7 // 100 / 249.28e6
        )

@OptIn(
        ExperimentalMaterial3Api::class,
        androidx.compose.foundation.ExperimentalFoundationApi::class
)
@Composable
fun ResultScreen(
        summaryJson: String,
        onRunAgain: () -> Unit,
        onBackToHome: () -> Unit,
        onShowDetailedResults: (List<BenchmarkResult>) -> Unit = {}
) {
        val context = LocalContext.current
        val summary =
                remember(summaryJson) {
                        try {
                                Log.d("ResultScreen", "Raw JSON: $summaryJson")

                                if (summaryJson.isBlank()) {
                                        Log.e("ResultScreen", "Received empty JSON string!")
                                        throw IllegalArgumentException("Empty JSON string")
                                }

                                val jsonObject = JSONObject(summaryJson)
                                val detailedResults = mutableListOf<BenchmarkResult>()

                                // Parse detailed results if available
                                val detailedResultsArray =
                                        jsonObject.optJSONArray("detailed_results")
                                if (detailedResultsArray != null) {
                                        for (i in 0 until detailedResultsArray.length()) {
                                                val resultObj =
                                                        detailedResultsArray.getJSONObject(i)
                                                val result =
                                                        BenchmarkResult(
                                                                name =
                                                                        resultObj.optString(
                                                                                "name",
                                                                                "Unknown"
                                                                        ),
                                                                executionTimeMs =
                                                                        resultObj.optDouble(
                                                                                "executionTimeMs",
                                                                                0.0
                                                                        ),
                                                                opsPerSecond =
                                                                        resultObj.optDouble(
                                                                                "opsPerSecond",
                                                                                0.0
                                                                        ),
                                                                isValid =
                                                                        resultObj.optBoolean(
                                                                                "isValid",
                                                                                false
                                                                        ),
                                                                metricsJson =
                                                                        resultObj.optString(
                                                                                "metricsJson",
                                                                                "{}"
                                                                        )
                                                        )
                                                detailedResults.add(result)
                                        }
                                }

                                // Get device info
                                val deviceInfo = DeviceInfoCollector.getDeviceInfo(context)
                                val cpuGovernor =
                                        try {
                                                val process =
                                                        Runtime.getRuntime()
                                                                .exec(
                                                                        "cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor"
                                                                )
                                                process.inputStream.bufferedReader().readLine()
                                                        ?: "Unknown"
                                        } catch (e: Exception) {
                                                "Unknown"
                                        }

                                // Get GPU info using GpuInfoUtils
                                val gpuInfoUtils = GpuInfoUtils(context)
                                val gpuInfoState = runBlocking { gpuInfoUtils.getGpuInfo() }

                                var gpuName = "Unknown"
                                var gpuVendor = "Unknown"
                                var gpuDriver = "Unknown"
                                var vulkanSupported = false
                                var vulkanVersion: String? = null

                                if (gpuInfoState is GpuInfoState.Success) {
                                        val gpuInfo = gpuInfoState.gpuInfo
                                        gpuName = gpuInfo.basicInfo.name
                                        gpuVendor = gpuInfo.basicInfo.vendor
                                        gpuDriver = gpuInfo.basicInfo.openGLVersion
                                        vulkanSupported = gpuInfo.vulkanInfo?.supported ?: false
                                        vulkanVersion =
                                                if (vulkanSupported) {
                                                        gpuInfo.vulkanInfo?.apiVersion
                                                                ?: gpuInfo.basicInfo.vulkanVersion
                                                } else {
                                                        null
                                                }
                                }

                                val deviceSummary =
                                        BenchmarkDeviceSummary(
                                                deviceName =
                                                        "${deviceInfo.manufacturer} ${deviceInfo.deviceModel}",
                                                os =
                                                        "Android ${deviceInfo.androidVersion} (API ${deviceInfo.apiLevel})",
                                                kernel = deviceInfo.kernelVersion,
                                                cpuName = deviceInfo.socName,
                                                cpuCores = deviceInfo.totalCores,
                                                cpuArchitecture = deviceInfo.cpuArchitecture,
                                                cpuGovernor = cpuGovernor,
                                                gpuName = gpuName,
                                                gpuVendor = gpuVendor,
                                                gpuDriver = gpuDriver,
                                                vulkanSupported = vulkanSupported,
                                                vulkanVersion = vulkanVersion,
                                                batteryLevel = deviceInfo.batteryCapacity,
                                                batteryTemp = deviceInfo.batteryTemperature,
                                                totalRam = deviceInfo.totalRam,
                                                totalSwap = deviceInfo.totalSwap,
                                                completedTimestamp = System.currentTimeMillis()
                                        )

                                BenchmarkSummary(
                                        singleCoreScore =
                                                jsonObject.optDouble("single_core_score", 0.0),
                                        multiCoreScore =
                                                jsonObject.optDouble("multi_core_score", 0.0),
                                        finalScore = jsonObject.optDouble("final_score", 0.0),
                                        normalizedScore =
                                                jsonObject.optDouble("normalized_score", 0.0),
                                        detailedResults = detailedResults,
                                        deviceSummary = deviceSummary,
                                        timestamp = System.currentTimeMillis(),
                                        // Handle performance_metrics - could be string or object
                                        performanceMetricsJson =
                                                run {
                                                        val metricsValue =
                                                                jsonObject.opt(
                                                                        "performance_metrics"
                                                                )
                                                        Log.d(
                                                                "ResultScreen",
                                                                "performance_metrics type: ${metricsValue?.javaClass?.simpleName}"
                                                        )
                                                        Log.d(
                                                                "ResultScreen",
                                                                "performance_metrics value: $metricsValue"
                                                        )

                                                        val result =
                                                                when {
                                                                        metricsValue == null -> "{}"
                                                                        metricsValue is String ->
                                                                                metricsValue
                                                                                        .ifBlank {
                                                                                                "{}"
                                                                                        }
                                                                        metricsValue is
                                                                                org.json.JSONObject ->
                                                                                metricsValue
                                                                                        .toString()
                                                                        else ->
                                                                                metricsValue
                                                                                        .toString()
                                                                }

                                                        Log.d(
                                                                "ResultScreen",
                                                                "Final performanceMetricsJson: ${result.take(200)}"
                                                        )
                                                        result
                                                }
                                )
                        } catch (e: Exception) {
                                Log.e("ResultScreen", "Error parsing summary JSON: ${e.message}", e)
                                BenchmarkSummary(
                                        singleCoreScore = 0.0,
                                        multiCoreScore = 0.0,
                                        finalScore = 0.0,
                                        normalizedScore = 0.0,
                                        detailedResults = emptyList(),
                                        deviceSummary = null
                                )
                        }
                }

        val tabs = remember { listOf("Summary", "Detailed Data", "Rankings") }
        val pagerState = rememberPagerState(pageCount = { tabs.size }, initialPage = 0)
        val coroutineScope = rememberCoroutineScope()

        // Format timestamp
        val formattedTimestamp =
                remember(summary.timestamp) {
                        val sdf = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
                        sdf.format(Date(summary.timestamp))
                }

        FinalBenchmark2Theme {
                Scaffold(
                        topBar = {
                                TopAppBar(
                                        title = {
                                                Column {
                                                        Text(
                                                                text = "CPU Benchmark",
                                                                fontSize = 20.sp,
                                                                fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                                text = formattedTimestamp,
                                                                fontSize = 12.sp,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant
                                                        )
                                                }
                                        },
                                        navigationIcon = {
                                                IconButton(onClick = onBackToHome) {
                                                        Icon(
                                                                imageVector =
                                                                        Icons.Default.ArrowBack,
                                                                contentDescription = "Back to Home"
                                                        )
                                                }
                                        },
                                        colors =
                                                TopAppBarDefaults.topAppBarColors(
                                                        containerColor =
                                                                MaterialTheme.colorScheme.surface,
                                                        titleContentColor =
                                                                MaterialTheme.colorScheme.onSurface
                                                )
                                )
                        }
                ) { paddingValues ->
                        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                                // Tab Row
                                TabRow(
                                        selectedTabIndex = pagerState.currentPage,
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        contentColor = MaterialTheme.colorScheme.primary
                                ) {
                                        tabs.forEachIndexed { index, title ->
                                                Tab(
                                                        selected = pagerState.currentPage == index,
                                                        onClick = {
                                                                coroutineScope.launch {
                                                                        pagerState
                                                                                .animateScrollToPage(
                                                                                        index
                                                                                )
                                                                }
                                                        },
                                                        text = {
                                                                Text(
                                                                        text = title,
                                                                        maxLines = 1,
                                                                        fontSize = 14.sp
                                                                )
                                                        }
                                                )
                                        }
                                }

                                // Pager Content
                                HorizontalPager(
                                        state = pagerState,
                                        modifier = Modifier.weight(1f).fillMaxWidth()
                                ) { page ->
                                        when (page) {
                                                0 -> SummaryTab(summary)
                                                1 -> DetailedDataTab(summary)
                                                2 ->
                                                        RankingsTab(
                                                                summary.finalScore,
                                                                summary.singleCoreScore,
                                                                summary.multiCoreScore
                                                        )
                                        }
                                }
                        }
                }
        }
}

@Composable
fun SummaryTab(summary: BenchmarkSummary) {
        LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
                // Final Score Card
                item {
                        Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors =
                                        CardDefaults.cardColors(
                                                containerColor =
                                                        MaterialTheme.colorScheme.primaryContainer
                                        ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                                Column(
                                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                        Text(
                                                text = "Final Score",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                                text = String.format("%.2f", summary.finalScore),
                                                fontSize = 48.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                        )
                                }
                        }
                }

                // Single-Core and Multi-Core Scores
                item {
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                                // Single-Core Score
                                Card(
                                        modifier = Modifier.weight(1f),
                                        elevation =
                                                CardDefaults.cardElevation(defaultElevation = 4.dp)
                                ) {
                                        Column(
                                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                                Text(
                                                        text = "Single-Core",
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                        text =
                                                                String.format(
                                                                        "%.2f",
                                                                        summary.singleCoreScore
                                                                ),
                                                        fontSize = 24.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                )
                                        }
                                }

                                // Multi-Core Score
                                Card(
                                        modifier = Modifier.weight(1f),
                                        elevation =
                                                CardDefaults.cardElevation(defaultElevation = 4.dp)
                                ) {
                                        Column(
                                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                                Text(
                                                        text = "Multi-Core",
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                        text =
                                                                String.format(
                                                                        "%.2f",
                                                                        summary.multiCoreScore
                                                                ),
                                                        fontSize = 24.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                )
                                        }
                                }
                        }
                }

                // Device Summary Card
                item {
                        summary.deviceSummary?.let { device ->
                                Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        elevation =
                                                CardDefaults.cardElevation(defaultElevation = 4.dp)
                                ) {
                                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                                Text(
                                                        text = "Device Summary",
                                                        fontSize = 18.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.padding(bottom = 12.dp)
                                                )

                                                // Basic Info
                                                SummaryInfoRow("Device", device.deviceName)
                                                SummaryInfoRow("OS", device.os)
                                                SummaryInfoRow("Kernel", device.kernel)

                                                Divider(
                                                        modifier =
                                                                Modifier.padding(vertical = 12.dp)
                                                )

                                                // CPU Info
                                                Text(
                                                        text = "CPU",
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant,
                                                        modifier = Modifier.padding(bottom = 8.dp)
                                                )
                                                SummaryInfoRow("Name", device.cpuName)
                                                SummaryInfoRow("Cores", "${device.cpuCores}")
                                                SummaryInfoRow(
                                                        "Architecture",
                                                        device.cpuArchitecture
                                                )
                                                SummaryInfoRow("Governor", device.cpuGovernor)

                                                Divider(
                                                        modifier =
                                                                Modifier.padding(vertical = 12.dp)
                                                )

                                                // GPU Info
                                                Text(
                                                        text = "GPU",
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant,
                                                        modifier = Modifier.padding(bottom = 8.dp)
                                                )
                                                SummaryInfoRow("Name", device.gpuName)
                                                SummaryInfoRow("Vendor", device.gpuVendor)
                                                SummaryInfoRow("OpenGL ES", device.gpuDriver)
                                                if (device.vulkanSupported) {
                                                        SummaryInfoRow(
                                                                "Vulkan Version",
                                                                device.vulkanVersion ?: "Supported"
                                                        )
                                                } else {
                                                        SummaryInfoRow(
                                                                "Vulkan Version",
                                                                "Not Supported"
                                                        )
                                                }
                                                Divider(
                                                        modifier =
                                                                Modifier.padding(vertical = 12.dp)
                                                )

                                                // Battery Info
                                                Text(
                                                        text = "Battery",
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant,
                                                        modifier = Modifier.padding(bottom = 8.dp)
                                                )
                                                SummaryInfoRow(
                                                        "Level",
                                                        device.batteryLevel?.let {
                                                                "${it.toInt()}%"
                                                        }
                                                                ?: "N/A"
                                                )
                                                SummaryInfoRow(
                                                        "Temperature",
                                                        device.batteryTemp?.let {
                                                                String.format("%.1f°C", it)
                                                        }
                                                                ?: "N/A"
                                                )

                                                Divider(
                                                        modifier =
                                                                Modifier.padding(vertical = 12.dp)
                                                )

                                                // Memory Info
                                                Text(
                                                        text = "Memory",
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant,
                                                        modifier = Modifier.padding(bottom = 8.dp)
                                                )
                                                SummaryInfoRow(
                                                        "Total RAM",
                                                        formatBytes(device.totalRam)
                                                )
                                                SummaryInfoRow(
                                                        "Total Swap",
                                                        if (device.totalSwap > 0)
                                                                formatBytes(device.totalSwap)
                                                        else "None"
                                                )
                                        }
                                }
                        }
                }
        }
}

@Composable
fun SummaryInfoRow(label: String, value: String) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
        ) {
                Text(
                        text = label,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                        text = value,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.End
                )
        }
}

@Composable
fun LongSummaryInfoRow(label: String, value: String) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(
                        text = label,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 2.dp)
                )
                Text(
                        text = value,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                )
        }
}

@Composable
fun DetailedDataTab(summary: BenchmarkSummary) {
        val singleCoreResults =
                remember(summary.detailedResults) {
                        summary.detailedResults.filter { it.name.startsWith("Single-Core") }
                }
        val multiCoreResults =
                remember(summary.detailedResults) {
                        summary.detailedResults.filter { it.name.startsWith("Multi-Core") }
                }

        LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
                // Performance Monitoring Section - FIRST
                item {
                        PerformanceMonitoringSection(
                                performanceMetricsJson = summary.performanceMetricsJson
                        )
                }

                // Single-Core Section
                item {
                        BenchmarkSection(
                                title = "Single-Core Benchmarks",
                                score = summary.singleCoreScore,
                                results = singleCoreResults
                        )
                }

                // Multi-Core Section
                item {
                        BenchmarkSection(
                                title = "Multi-Core Benchmarks",
                                score = summary.multiCoreScore,
                                results = multiCoreResults
                        )
                }
        }
}

@Composable
fun BenchmarkSection(title: String, score: Double, results: List<BenchmarkResult>) {
        var expanded by remember { mutableStateOf(true) }

        Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        // Section Header
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Column {
                                        Text(
                                                text = title,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                                text = "Score: ${String.format("%.2f", score)}",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                }
                                IconButton(onClick = { expanded = !expanded }) {
                                        Icon(
                                                imageVector =
                                                        if (expanded) Icons.Default.ExpandLess
                                                        else Icons.Default.ExpandMore,
                                                contentDescription =
                                                        if (expanded) "Collapse" else "Expand"
                                        )
                                }
                        }

                        // Benchmark List
                        if (expanded) {
                                Spacer(modifier = Modifier.height(12.dp))
                                results.forEach { result ->
                                        BenchmarkResultItem(result)
                                        Spacer(modifier = Modifier.height(8.dp))
                                }
                        }
                }
        }
}

@Composable
fun BenchmarkResultItem(result: BenchmarkResult) {
        val cleanName = result.name.replace("Single-Core ", "").replace("Multi-Core ", "")
        val mopsPerSecond = result.opsPerSecond / 1_000_000.0
        val timeInSeconds = result.executionTimeMs / 1000.0

        // Determine if this is a single-core or multi-core benchmark
        val isSingleCore = result.name.startsWith("Single-Core")
        val scalingFactors = if (isSingleCore) SINGLE_CORE_FACTORS else MULTI_CORE_FACTORS

        // Calculate individual score using enum-based lookup
        val benchmarkName = BenchmarkName.fromString(result.name)
        val individualScore =
                benchmarkName?.let { scalingFactors[it]?.times(result.opsPerSecond) } ?: 0.0

        Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                        CardDefaults.cardColors(
                                containerColor =
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
        ) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(
                                text = cleanName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                                Column {
                                        Text(
                                                text = "Performance",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                                text = String.format("%.2f Mops/s", mopsPerSecond),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.primary
                                        )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                                text = "Score",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                                text = String.format("%.2f", individualScore),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.secondary
                                        )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                                text = "Time",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                                text = String.format("%.2f s", timeInSeconds),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                        )
                                }
                        }
                }
        }
}

@Composable
private fun RankingsTab(finalScore: Double, singleCoreScore: Double, multiCoreScore: Double) {
        val scrollState = androidx.compose.foundation.rememberScrollState()
        Column(
                modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
                // Final Score Bar at top
                Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                                CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                ) {
                        Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Text(
                                        text = "Your Final Score",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                        text = String.format("%.0f", finalScore),
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                )
                        }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Create rankings with current score
                val hardcodedReferenceDevices =
                        listOf(
                                RankingItem(
                                        name = "Snapdragon 8 Elite",
                                        normalizedScore = 1200,
                                        singleCore = 2850,
                                        multiCore = 10200,
                                        isCurrentUser = false
                                ),
                                RankingItem(
                                        name = "Snapdragon 8 Gen 3",
                                        normalizedScore = 900,
                                        singleCore = 2600,
                                        multiCore = 8500,
                                        isCurrentUser = false
                                ),
                                RankingItem(
                                        name = "Snapdragon 8s Gen 3",
                                        normalizedScore = 750,
                                        singleCore = 2400,
                                        multiCore = 7200,
                                        isCurrentUser = false
                                ),
                                RankingItem(
                                        name = "Snapdragon 7+ Gen 3",
                                        normalizedScore = 720,
                                        singleCore = 2350,
                                        multiCore = 7000,
                                        isCurrentUser = false
                                ),
                                RankingItem(
                                        name = "Dimensity 8300",
                                        normalizedScore = 650,
                                        singleCore = 2200,
                                        multiCore = 6500,
                                        isCurrentUser = false
                                ),
                                RankingItem(
                                        name = "Helio G95",
                                        normalizedScore = 250,
                                        singleCore = 1100,
                                        multiCore = 3500,
                                        isCurrentUser = false
                                ),
                                RankingItem(
                                        name = "Snapdragon 845",
                                        normalizedScore = 200,
                                        singleCore = 900,
                                        multiCore = 3000,
                                        isCurrentUser = false
                                )
                        )

                // Add current device score
                val userDeviceName = "Your Device (${android.os.Build.MODEL})"
                val currentUserScore =
                        RankingItem(
                                name = userDeviceName,
                                normalizedScore = finalScore.toInt(),
                                singleCore = singleCoreScore.toInt(),
                                multiCore = multiCoreScore.toInt(),
                                isCurrentUser = true
                        )

                // Merge and sort
                val allDevices =
                        mutableListOf<RankingItem>().apply {
                                addAll(hardcodedReferenceDevices)
                                add(currentUserScore)
                        }

                // Sort by normalized score and assign ranks
                val rankedItems =
                        allDevices.sortedByDescending { it.normalizedScore }.mapIndexed {
                                index,
                                item ->
                                item.copy(rank = index + 1)
                        }

                // Calculate beats percentage and previous device comparison
                val userRank = rankedItems.indexOfFirst { it.isCurrentUser }
                val totalDevices = rankedItems.size
                val devicesBeaten = totalDevices - userRank - 1
                val beatsPercentage =
                        if (totalDevices > 1) {
                                (devicesBeaten.toFloat() / (totalDevices - 1) * 100).toInt()
                        } else {
                                100
                        }

                // Find next lower ranked device for comparison
                val nextLowerDevice =
                        if (userRank < rankedItems.size - 1) {
                                rankedItems[userRank + 1]
                        } else {
                                null
                        }

                val percentBetterThanNext =
                        if (nextLowerDevice != null && nextLowerDevice.normalizedScore > 0) {
                                ((finalScore - nextLowerDevice.normalizedScore) /
                                                nextLowerDevice.normalizedScore * 100)
                                        .toInt()
                        } else {
                                0
                        }

                // Modern Comparison Cards - Vertical Layout with Bar Graphs
                Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                        // Performance Percentile Card with Bar Graph
                        Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors =
                                        CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surface
                                        ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                                        Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                        ) {
                                                Text(
                                                        text = "Performance Ranking",
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant
                                                )
                                                Text(
                                                        text = "$beatsPercentage%",
                                                        fontSize = 24.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                )
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Horizontal Bar Graph
                                        Box(
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .height(12.dp)
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(
                                                                        MaterialTheme.colorScheme
                                                                                .surfaceVariant
                                                                )
                                        ) {
                                                Box(
                                                        modifier =
                                                                Modifier.fillMaxHeight()
                                                                        .fillMaxWidth(
                                                                                beatsPercentage /
                                                                                        100f
                                                                        )
                                                                        .clip(
                                                                                RoundedCornerShape(
                                                                                        6.dp
                                                                                )
                                                                        )
                                                                        .background(
                                                                                brush =
                                                                                        Brush.horizontalGradient(
                                                                                                colors =
                                                                                                        listOf(
                                                                                                                MaterialTheme
                                                                                                                        .colorScheme
                                                                                                                        .primary,
                                                                                                                MaterialTheme
                                                                                                                        .colorScheme
                                                                                                                        .tertiary
                                                                                                        )
                                                                                        )
                                                                        )
                                                )
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Text(
                                                text =
                                                        if (beatsPercentage >= 100) {
                                                                "Your device outperforms all tested devices."
                                                        } else if (beatsPercentage >= 75) {
                                                                "Your device is in the top tier of performance."
                                                        } else if (beatsPercentage >= 50) {
                                                                "Your device performs above average."
                                                        } else if (beatsPercentage >= 25) {
                                                                "Your device has moderate performance."
                                                        } else {
                                                                "Your device has entry-level performance."
                                                        },
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                lineHeight = 18.sp
                                        )
                                }
                        }

                        // Speed Comparison Card with Bar Graph
                        if (nextLowerDevice != null) {
                                Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors =
                                                CardDefaults.cardColors(
                                                        containerColor =
                                                                MaterialTheme.colorScheme.surface
                                                ),
                                        elevation =
                                                CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                                                Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement =
                                                                Arrangement.SpaceBetween,
                                                        verticalAlignment =
                                                                Alignment.CenterVertically
                                                ) {
                                                        Text(
                                                                text = "Speed Advantage",
                                                                fontSize = 14.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant
                                                        )
                                                        Text(
                                                                text =
                                                                        if (percentBetterThanNext >=
                                                                                        0
                                                                        )
                                                                                "+$percentBetterThanNext%"
                                                                        else
                                                                                "$percentBetterThanNext%",
                                                                fontSize = 24.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color =
                                                                        if (percentBetterThanNext >=
                                                                                        0
                                                                        )
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .tertiary
                                                                        else
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .error
                                                        )
                                                }

                                                Spacer(modifier = Modifier.height(12.dp))

                                                // Comparison Bar Graph - shows both devices
                                                Column(
                                                        verticalArrangement =
                                                                Arrangement.spacedBy(6.dp)
                                                ) {
                                                        // Your device bar
                                                        Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                verticalAlignment =
                                                                        Alignment.CenterVertically
                                                        ) {
                                                                Text(
                                                                        text = "You",
                                                                        fontSize = 11.sp,
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onSurfaceVariant,
                                                                        modifier =
                                                                                Modifier.width(
                                                                                        40.dp
                                                                                )
                                                                )
                                                                Box(
                                                                        modifier =
                                                                                Modifier.weight(1f)
                                                                                        .height(
                                                                                                8.dp
                                                                                        )
                                                                                        .clip(
                                                                                                RoundedCornerShape(
                                                                                                        4.dp
                                                                                                )
                                                                                        )
                                                                                        .background(
                                                                                                MaterialTheme
                                                                                                        .colorScheme
                                                                                                        .surfaceVariant
                                                                                        )
                                                                ) {
                                                                        val userBarWidth =
                                                                                if (finalScore >
                                                                                                nextLowerDevice
                                                                                                        .normalizedScore
                                                                                )
                                                                                        1f
                                                                                else
                                                                                        (finalScore /
                                                                                                        nextLowerDevice
                                                                                                                .normalizedScore
                                                                                                                .toDouble())
                                                                                                .toFloat()
                                                                                                .coerceIn(
                                                                                                        0f,
                                                                                                        1f
                                                                                                )
                                                                        Box(
                                                                                modifier =
                                                                                        Modifier.fillMaxHeight()
                                                                                                .fillMaxWidth(
                                                                                                        userBarWidth
                                                                                                )
                                                                                                .clip(
                                                                                                        RoundedCornerShape(
                                                                                                                4.dp
                                                                                                        )
                                                                                                )
                                                                                                .background(
                                                                                                        MaterialTheme
                                                                                                                .colorScheme
                                                                                                                .primary
                                                                                                )
                                                                        )
                                                                }
                                                        }

                                                        // Next device bar
                                                        Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                verticalAlignment =
                                                                        Alignment.CenterVertically
                                                        ) {
                                                                Text(
                                                                        text =
                                                                                nextLowerDevice.name
                                                                                        .take(4),
                                                                        fontSize = 11.sp,
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onSurfaceVariant,
                                                                        modifier =
                                                                                Modifier.width(
                                                                                        40.dp
                                                                                )
                                                                )
                                                                Box(
                                                                        modifier =
                                                                                Modifier.weight(1f)
                                                                                        .height(
                                                                                                8.dp
                                                                                        )
                                                                                        .clip(
                                                                                                RoundedCornerShape(
                                                                                                        4.dp
                                                                                                )
                                                                                        )
                                                                                        .background(
                                                                                                MaterialTheme
                                                                                                        .colorScheme
                                                                                                        .surfaceVariant
                                                                                        )
                                                                ) {
                                                                        val otherBarWidth =
                                                                                if (nextLowerDevice
                                                                                                .normalizedScore >=
                                                                                                finalScore
                                                                                )
                                                                                        1f
                                                                                else
                                                                                        (nextLowerDevice
                                                                                                        .normalizedScore /
                                                                                                        finalScore)
                                                                                                .toFloat()
                                                                                                .coerceIn(
                                                                                                        0f,
                                                                                                        1f
                                                                                                )
                                                                        Box(
                                                                                modifier =
                                                                                        Modifier.fillMaxHeight()
                                                                                                .fillMaxWidth(
                                                                                                        otherBarWidth
                                                                                                )
                                                                                                .clip(
                                                                                                        RoundedCornerShape(
                                                                                                                4.dp
                                                                                                        )
                                                                                                )
                                                                                                .background(
                                                                                                        MaterialTheme
                                                                                                                .colorScheme
                                                                                                                .outline
                                                                                                                .copy(
                                                                                                                        alpha =
                                                                                                                                0.6f
                                                                                                                )
                                                                                                )
                                                                        )
                                                                }
                                                        }
                                                }

                                                Spacer(modifier = Modifier.height(10.dp))

                                                Text(
                                                        text =
                                                                if (percentBetterThanNext > 0) {
                                                                        "Your device is $percentBetterThanNext% faster than ${nextLowerDevice.name}."
                                                                } else if (percentBetterThanNext < 0
                                                                ) {
                                                                        "Your device is ${kotlin.math.abs(percentBetterThanNext)}% slower than ${nextLowerDevice.name}."
                                                                } else {
                                                                        "Your device performs similarly to ${nextLowerDevice.name}."
                                                                },
                                                        fontSize = 13.sp,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant,
                                                        lineHeight = 18.sp
                                                )
                                        }
                                }
                        } else if (userRank == 0) {
                                // Top ranked - special card
                                Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors =
                                                CardDefaults.cardColors(
                                                        containerColor =
                                                                MaterialTheme.colorScheme
                                                                        .primaryContainer
                                                ),
                                        elevation =
                                                CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                        Row(
                                                modifier = Modifier.fillMaxWidth().padding(20.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                                Text(text = "🏆", fontSize = 32.sp)
                                                Column {
                                                        Text(
                                                                text = "Top Performance!",
                                                                fontSize = 16.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onPrimaryContainer
                                                        )
                                                        Text(
                                                                text =
                                                                        "Your device leads the performance rankings.",
                                                                fontSize = 13.sp,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onPrimaryContainer
                                                                                .copy(alpha = 0.8f)
                                                        )
                                                }
                                        }
                                }
                        }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Ranks Title
                Text(
                        text = "Ranks",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 12.dp)
                )

                // Rankings Content
                Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                ) { rankedItems.forEach { item -> RankingItemCard(item) } }
        }
}

@Composable
private fun RankingItemCard(item: RankingItem) {
        val topScoreMax = 1200
        val scoreProgress = (item.normalizedScore.toFloat() / topScoreMax).coerceIn(0f, 1f)

        val goldColor = Color(0xFFFFD700)
        val silverColor = Color(0xFFC0C0C0)
        val bronzeColor = Color(0xFFCD7F32)

        val rankColor =
                when (item.rank) {
                        1 -> goldColor
                        2 -> silverColor
                        3 -> bronzeColor
                        else -> MaterialTheme.colorScheme.onSurface
                }

        val containerColor =
                if (item.isCurrentUser) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                } else {
                        MaterialTheme.colorScheme.surfaceVariant
                }

        val borderModifier =
                if (item.isCurrentUser) {
                        Modifier.border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp)
                        )
                } else {
                        Modifier
                }

        Card(
                modifier = Modifier.fillMaxWidth().then(borderModifier),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = containerColor)
        ) {
                Column(modifier = Modifier.padding(12.dp)) {
                        // Header row: Rank, Name, Score
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                                // Left: Rank
                                Box(
                                        modifier =
                                                Modifier.width(50.dp)
                                                        .height(40.dp)
                                                        .background(
                                                                color =
                                                                        rankColor.copy(
                                                                                alpha = 0.2f
                                                                        ),
                                                                shape = RoundedCornerShape(8.dp)
                                                        ),
                                        contentAlignment = Alignment.Center
                                ) {
                                        Text(
                                                text = "#${item.rank}",
                                                fontWeight = FontWeight.Bold,
                                                color = rankColor,
                                                fontSize = 16.sp
                                        )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Center: Name and Scores
                                Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                                text = item.name,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                                text =
                                                        "Single: ${item.singleCore} | Multi: ${item.multiCore}",
                                                fontSize = 12.sp,
                                                color =
                                                        MaterialTheme.colorScheme.onSurface.copy(
                                                                alpha = 0.7f
                                                        ),
                                                modifier = Modifier.paddingFromBaseline(top = 4.dp)
                                        )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Right: Normalized Score
                                Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                                text = item.normalizedScore.toString(),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp,
                                                color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                                text = "Score",
                                                fontSize = 10.sp,
                                                color =
                                                        MaterialTheme.colorScheme.onSurface.copy(
                                                                alpha = 0.6f
                                                        )
                                        )
                                }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Progress Bar
                        LinearProgressIndicator(
                                progress = scoreProgress,
                                modifier = Modifier.fillMaxWidth().height(4.dp),
                                color = GruvboxDarkAccent,
                                trackColor =
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                }
        }
}

@Composable
fun ScoreItem(title: String, value: String) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
                Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                        text = value,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                )
        }
}
