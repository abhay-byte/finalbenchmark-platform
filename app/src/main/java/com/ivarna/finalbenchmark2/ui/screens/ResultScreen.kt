package com.ivarna.finalbenchmark2.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        val timestamp: Long = System.currentTimeMillis()
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
                    val detailedResultsArray = jsonObject.optJSONArray("detailed_results")
                    if (detailedResultsArray != null) {
                        for (i in 0 until detailedResultsArray.length()) {
                            val resultObj = detailedResultsArray.getJSONObject(i)
                            val result =
                                    BenchmarkResult(
                                            name = resultObj.optString("name", "Unknown"),
                                            executionTimeMs =
                                                    resultObj.optDouble("executionTimeMs", 0.0),
                                            opsPerSecond = resultObj.optDouble("opsPerSecond", 0.0),
                                            isValid = resultObj.optBoolean("isValid", false),
                                            metricsJson = resultObj.optString("metricsJson", "{}")
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
                                process.inputStream.bufferedReader().readLine() ?: "Unknown"
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
                            singleCoreScore = jsonObject.optDouble("single_core_score", 0.0),
                            multiCoreScore = jsonObject.optDouble("multi_core_score", 0.0),
                            finalScore = jsonObject.optDouble("final_score", 0.0),
                            normalizedScore = jsonObject.optDouble("normalized_score", 0.0),
                            detailedResults = detailedResults,
                            deviceSummary = deviceSummary,
                            timestamp = System.currentTimeMillis()
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
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = onBackToHome) {
                                    Icon(
                                            imageVector = Icons.Default.ArrowBack,
                                            contentDescription = "Back to Home"
                                    )
                                }
                            },
                            colors =
                                    TopAppBarDefaults.topAppBarColors(
                                            containerColor = MaterialTheme.colorScheme.surface,
                                            titleContentColor = MaterialTheme.colorScheme.onSurface
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
                                    coroutineScope.launch { pagerState.animateScrollToPage(index) }
                                },
                                text = { Text(text = title, maxLines = 1, fontSize = 14.sp) }
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
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
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
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                                text = "Single-Core",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                                text = String.format("%.2f", summary.singleCoreScore),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Multi-Core Score
                Card(
                        modifier = Modifier.weight(1f),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                                text = "Multi-Core",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                                text = String.format("%.2f", summary.multiCoreScore),
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
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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

                        Divider(modifier = Modifier.padding(vertical = 12.dp))

                        // CPU Info
                        Text(
                                text = "CPU",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                        )
                        SummaryInfoRow("Name", device.cpuName)
                        SummaryInfoRow("Cores", "${device.cpuCores}")
                        SummaryInfoRow("Architecture", device.cpuArchitecture)
                        SummaryInfoRow("Governor", device.cpuGovernor)

                        Divider(modifier = Modifier.padding(vertical = 12.dp))

                        // GPU Info
                        Text(
                                text = "GPU",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                        )
                        SummaryInfoRow("Name", device.gpuName)
                        SummaryInfoRow("Vendor", device.gpuVendor)
                        LongSummaryInfoRow("OpenGL ES", device.gpuDriver)
                        if (device.vulkanSupported) {
                            LongSummaryInfoRow("Vulkan", device.vulkanVersion ?: "Supported")
                        } else {
                            SummaryInfoRow("Vulkan", "Not Supported")
                        }
                        Divider(modifier = Modifier.padding(vertical = 12.dp))

                        // Battery Info
                        Text(
                                text = "Battery",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                        )
                        SummaryInfoRow(
                                "Level",
                                device.batteryLevel?.let { "${it.toInt()}%" } ?: "N/A"
                        )
                        SummaryInfoRow(
                                "Temperature",
                                device.batteryTemp?.let { String.format("%.1f°C", it) } ?: "N/A"
                        )

                        Divider(modifier = Modifier.padding(vertical = 12.dp))

                        // Memory Info
                        Text(
                                text = "Memory",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                        )
                        SummaryInfoRow("Total RAM", formatBytes(device.totalRam))
                        SummaryInfoRow(
                                "Total Swap",
                                if (device.totalSwap > 0) formatBytes(device.totalSwap) else "None"
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
        Text(text = label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
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
                            contentDescription = if (expanded) "Collapse" else "Expand"
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
    Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
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

        Spacer(modifier = Modifier.height(16.dp))

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
                allDevices.sortedByDescending { it.normalizedScore }.mapIndexed { index, item ->
                    item.copy(rank = index + 1)
                }

        // Rankings Content
        LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
        ) { items(rankedItems) { item -> RankingItemCard(item) } }
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
                                                color = rankColor.copy(alpha = 0.2f),
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
                            text = "Single: ${item.singleCore} | Multi: ${item.multiCore}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
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
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress Bar
            LinearProgressIndicator(
                    progress = scoreProgress,
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = GruvboxDarkAccent,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
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
