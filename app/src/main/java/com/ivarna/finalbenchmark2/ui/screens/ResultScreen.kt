package com.ivarna.finalbenchmark2.ui.screens

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.rounded.*
import com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkName
import com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkResult
import com.ivarna.finalbenchmark2.cpuBenchmark.KotlinBenchmarkManager
import com.ivarna.finalbenchmark2.ui.theme.FinalBenchmark2Theme
import com.ivarna.finalbenchmark2.ui.theme.GruvboxDarkAccent
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.ivarna.finalbenchmark2.ui.viewmodels.RankingItem
import com.ivarna.finalbenchmark2.utils.DeviceInfoCollector
import com.ivarna.finalbenchmark2.utils.GpuInfoState
import com.ivarna.finalbenchmark2.utils.GpuInfoUtils
import com.ivarna.finalbenchmark2.utils.formatBytes
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import org.json.JSONObject

data class BenchmarkSummary(
        val singleCoreScore: Double,
        val multiCoreScore: Double,
        val type: String = "CPU",
        val finalScore: Double,
        val normalizedScore: Double,
        val detailedResults: List<BenchmarkResult> = emptyList(),
        val deviceSummary: BenchmarkDeviceSummary? = null,
        val timestamp: Long = System.currentTimeMillis(),
        val performanceMetricsJson: String = ""
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
        onShowDetailedResults: (List<BenchmarkResult>) -> Unit = {},
        historyRepository: com.ivarna.finalbenchmark2.data.repository.HistoryRepository? = null,
        benchmarkId: Long? = null,
        hazeState: dev.chrisbanes.haze.HazeState? = null
) {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        var showDeleteDialog by remember { mutableStateOf(false) }

        var summaryState by remember { mutableStateOf<BenchmarkSummary?>(null) }
        
        LaunchedEffect(summaryJson) {
            launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    Log.d("ResultScreen", "Starting async parsing of JSON")

                    if (summaryJson.isBlank()) {
                        Log.e("ResultScreen", "Received empty JSON string!")
                        throw IllegalArgumentException("Empty JSON string")
                    }

                    val jsonObject = JSONObject(summaryJson)
                    val detailedResults = mutableListOf<BenchmarkResult>()

                    // Parse detailed results
                    val detailedResultsArray = jsonObject.optJSONArray("detailed_results")
                    if (detailedResultsArray != null) {
                        for (i in 0 until detailedResultsArray.length()) {
                            val resultObj = detailedResultsArray.getJSONObject(i)
                            detailedResults.add(
                                BenchmarkResult(
                                    name = resultObj.optString("name", "Unknown"),
                                    executionTimeMs = resultObj.optDouble("executionTimeMs", 0.0),
                                    opsPerSecond = resultObj.optDouble("opsPerSecond", 0.0),
                                    isValid = resultObj.optBoolean("isValid", false),
                                    metricsJson = resultObj.optString("metricsJson", "{}"),
                                    accelerationMode = if (resultObj.has("acceleration_mode")) resultObj.getString("acceleration_mode") else null
                                )
                            )
                        }
                    }

                    // Get device info
                    val deviceInfo = DeviceInfoCollector.getDeviceInfo(context)
                    
                    // CPU Governor (Shell Command - Slow)
                    val cpuGovernor = try {
                        val process = Runtime.getRuntime().exec("cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor")
                        process.inputStream.bufferedReader().readText().trim().takeIf { it.isNotEmpty() } ?: "Unknown"
                    } catch (e: Exception) {
                        "Unknown"
                    }

                    // GPU Info (Suspected blocker - Heavy synchronous call now on IO thread)
                    val gpuInfoUtils = GpuInfoUtils(context)
                    val gpuInfoState = gpuInfoUtils.getGpuInfo()

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
                        vulkanVersion = if (vulkanSupported) {
                            gpuInfo.vulkanInfo?.apiVersion ?: gpuInfo.basicInfo.vulkanVersion
                        } else {
                            null
                        }
                    }

                    val deviceSummary = BenchmarkDeviceSummary(
                        deviceName = "${deviceInfo.manufacturer} ${deviceInfo.deviceModel}",
                        os = "Android ${deviceInfo.androidVersion} (API ${deviceInfo.apiLevel})",
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

                    // Parse Score metrics
                    val performanceMetricsJson = jsonObject.opt("performance_metrics")?.toString() ?: "{}"
                    
                    val parsedSummary = BenchmarkSummary(
                        singleCoreScore = jsonObject.optDouble("single_core_score", 0.0),
                        multiCoreScore = jsonObject.optDouble("multi_core_score", 0.0),
                        type = jsonObject.optString("type", "CPU"),
                        finalScore = jsonObject.optDouble("final_score", 0.0),
                        normalizedScore = jsonObject.optDouble("normalized_score", 0.0),
                        detailedResults = detailedResults,
                        deviceSummary = deviceSummary,
                        timestamp = jsonObject.optLong("timestamp", System.currentTimeMillis()),
                        performanceMetricsJson = performanceMetricsJson
                    )
                    
                    summaryState = parsedSummary
                    
                } catch (e: Exception) {
                    Log.e("ResultScreen", "Error parsing summary JSON async: ${e.message}", e)
                    // Fallback empty summary
                    summaryState = BenchmarkSummary(
                        singleCoreScore = 0.0,
                        multiCoreScore = 0.0,
                        finalScore = 0.0,
                        normalizedScore = 0.0
                    ) 
                }
            }
        }
        
        // Show Loading State while parsing
        if (summaryState == null) {
            FinalBenchmark2Theme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            return
        }

        val summary = summaryState!!


        // Share benchmark function
        val shareBenchmark: () -> Unit = {
                val deviceInfo = DeviceInfoCollector.getDeviceInfo(context)

                val shareText = buildString {
                        appendLine("FinalBenchmark Result")
                        appendLine(
                                "Date: ${SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()).format(Date(summary.timestamp))}"
                        )
                        appendLine()
                        appendLine("Device Info:")
                        appendLine("SOC: ${summary.deviceSummary?.cpuName ?: deviceInfo.socName}")
                        appendLine("CPU: ${deviceInfo.manufacturer} ${deviceInfo.deviceModel}")
                        appendLine(
                                "Cores: ${deviceInfo.totalCores} (${deviceInfo.bigCores} big + ${deviceInfo.smallCores} small)"
                        )
                        val gpuName =
                                summary.deviceSummary?.gpuName?.takeIf {
                                        it.isNotBlank() && it != "Unknown GPU"
                                }
                                        ?: deviceInfo.gpuModel
                        val gpuVendor =
                                summary.deviceSummary?.gpuVendor?.takeIf { it.isNotBlank() }
                                        ?: deviceInfo.gpuVendor
                        appendLine("GPU: $gpuName ($gpuVendor)")
                        appendLine()
                        appendLine("Scores:")
                        appendLine("Total Score: ${String.format("%.0f", summary.finalScore)}")
                        appendLine("Normalized: ${String.format("%.0f", summary.normalizedScore)}")
                        appendLine()
                        appendLine("CPU Scores:")
                        appendLine("Single-Core: ${String.format("%.0f", summary.singleCoreScore)}")
                        appendLine("Multi-Core: ${String.format("%.0f", summary.multiCoreScore)}")
                        appendLine()
                        appendLine("Individual Details:")
                        summary.detailedResults.forEach { result ->
                                // Calculate score for this benchmark
                                val benchmarkName =
                                        BenchmarkName.values().find {
                                                result.name.contains(
                                                        it.displayName(),
                                                        ignoreCase = true
                                                )
                                        }
                                val factor =
                                        if (result.name.startsWith("Single-Core")) {
                                                KotlinBenchmarkManager.SCORING_FACTORS[benchmarkName] ?: 0.0
                                        } else {
                                                KotlinBenchmarkManager.SCORING_FACTORS[benchmarkName] ?: 0.0
                                        }
                                val score = result.opsPerSecond * factor

                                appendLine(
                                        "- ${result.name}: ${String.format(Locale.US, "%.2f", result.opsPerSecond / 1_000_000.0)} Mops/s | Score: ${String.format("%.2f", score)} (${String.format(Locale.US, "%.2f s", result.executionTimeMs / 1000.0)})"
                                )
                        }
                }

                val sendIntent =
                        Intent(Intent.ACTION_SEND).apply {
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                        }
                val shareIntent = Intent.createChooser(sendIntent, "Share Benchmark")
                context.startActivity(shareIntent)
        }

        // Delete benchmark function
        val deleteBenchmark: () -> Unit = {
                // Get the benchmark ID from the summary (which includes parsed ID from JSON)
                val idToDelete = summary?.let { s ->
                        // Try to get ID from JSON first
                        try {
                                val jsonObject = JSONObject(summaryJson)
                                val parsedId = jsonObject.optLong("benchmark_id", -1L)
                                if (parsedId > 0) parsedId else benchmarkId
                        } catch (e: Exception) {
                                benchmarkId
                        }
                } ?: benchmarkId

                if (idToDelete != null && idToDelete > 0 && historyRepository != null) {
                        coroutineScope.launch {
                                try {
                                        android.util.Log.d("ResultScreen", "Deleting benchmark with ID: $idToDelete")
                                        historyRepository.deleteResultById(idToDelete)
                                        android.util.Log.d("ResultScreen", "Benchmark deleted successfully")
                                        onBackToHome()
                                } catch (e: Exception) {
                                        android.util.Log.e("ResultScreen", "Delete failed", e)
                                }
                        }
                } else {
                        android.util.Log.d("ResultScreen", "Delete skipped - ID: $idToDelete, hasRepo: ${historyRepository != null}")
                        // For fresh benchmarks without ID, just navigate back
                        onBackToHome()
                }
        }

        val tabs = remember { listOf("Summary", "Detailed Data", "Rankings") }
        val pagerState = rememberPagerState(pageCount = { tabs.size }, initialPage = 0)

        // Format timestamp
        val formattedTimestamp =
                remember(summary.timestamp) {
                        val sdf = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
                        sdf.format(Date(summary.timestamp))
                }

        FinalBenchmark2Theme {
            // Animations for visual polish
            val scoreAnim = remember { Animatable(0f) }
            val fadeAnim = remember { Animatable(0f) }
            
            LaunchedEffect(Unit) {
                launch { fadeAnim.animateTo(1f, animationSpec = tween(800)) }
                launch {
                    scoreAnim.animateTo(
                        targetValue = summary.finalScore.toFloat(),
                        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                                Color.Transparent
                            ),
                            center = Offset(0f, 0f),
                            radius = 1000f
                        )
                    )
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.surfaceContainerLowest
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Glassmorphic Top Bar
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .padding(top = 16.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.05f)
                                        )
                                    )
                                )
                                .padding(vertical = 12.dp, horizontal = 12.dp)
                        ) {
                            // Back Button
                            IconButton(
                                onClick = onBackToHome,
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .size(40.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            // Title & Timestamp
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "BENCHMARK RESULTS",
                                    style = MaterialTheme.typography.labelMedium,
                                    letterSpacing = 2.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    formattedTimestamp,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            
                            // Action buttons
                            Row(
                                modifier = Modifier.align(Alignment.CenterEnd)
                            ) {
                                IconButton(
                                    onClick = { showDeleteDialog = true },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                            CircleShape
                                        )
                                ) {
                                    Icon(
                                        Icons.Rounded.Delete,
                                        "Delete",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                IconButton(
                                    onClick = { 
                                        val shareText = formatBenchmarkShareData(context, summary)
                                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                            type = "text/plain"
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Share Benchmark Results").apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                                    },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                            CircleShape
                                        )
                                ) {
                                    Icon(
                                        Icons.Rounded.Share,
                                        "Share",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Delete confirmation dialog
                    if (showDeleteDialog) {
                        AlertDialog(
                            onDismissRequest = { showDeleteDialog = false },
                            title = { Text("Delete Benchmark") },
                            text = {
                                Text(
                                    "Are you sure you want to delete this benchmark? This action cannot be undone."
                                )
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        deleteBenchmark()
                                        showDeleteDialog = false
                                    }
                                ) { Text("Delete") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }


                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Glassmorphic Tab Row
                        com.ivarna.finalbenchmark2.ui.components.GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            shape = RoundedCornerShape(50)
                        ) {
                            TabRow(
                                selectedTabIndex = pagerState.currentPage,
                                containerColor = Color.Transparent,
                                contentColor = MaterialTheme.colorScheme.primary,
                                divider = {},
                                indicator = { tabPositions ->
                                    if (pagerState.currentPage < tabPositions.size) {
                                        TabRowDefaults.SecondaryIndicator(
                                            Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                                            color = MaterialTheme.colorScheme.primary,
                                            height = 3.dp // Matched height to DeviceScreen (3.dp)
                                        )
                                    }
                                }
                            ) {
                                tabs.forEachIndexed { index, title ->
                                    Tab(
                                        selected = pagerState.currentPage == index,
                                        onClick = {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(index)
                                            }
                                        },
                                        text = { 
                                            Text(
                                                text = title,
                                                fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 14.sp, // Slightly adjusted for better fit with pill shape
                                                color = if (pagerState.currentPage == index) 
                                                    MaterialTheme.colorScheme.primary 
                                                else 
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                            ) 
                                        }
                                    )
                                }
                            }
                        }

                        // Pager Content
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalAlignment = Alignment.Top
                        ) { page ->
                            when (page) {
                                0 -> SummaryTab(summary)
                                1 -> DetailedDataTab(summary)
                                2 -> RankingsTab(
                                        summary.finalScore,
                                        summary.singleCoreScore,
                                        summary.multiCoreScore,
                                        summary.type
                                    )
                            }
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
                // Final Score - Large Glassmorphic Card
                item {
                        var targetScore by remember { mutableStateOf(0f) }
                        LaunchedEffect(Unit) {
                            targetScore = summary.finalScore.toFloat()
                        }
                        val animatedScore by animateFloatAsState(
                            targetValue = targetScore,
                            animationSpec = tween(durationMillis = 2000, easing = FastOutSlowInEasing)
                        )
                        
                        AnimatedEntranceContainer(index = 0) {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                shape = RoundedCornerShape(32.dp),
                                colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                            ) {
                                Box(
                                        modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                        brush = Brush.radialGradient(
                                                                colors = listOf(
                                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.02f)
                                                                ),
                                                                center = Offset(0.5f, 0f),
                                                                radius = 600f
                                                        )
                                                )
                                ) {
                                        Column(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 24.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                                Text(
                                                        text = "${summary.type.uppercase()} BENCHMARK SCORE",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Black,
                                                        letterSpacing = 2.sp,
                                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                                )
                                                Spacer(modifier = Modifier.height(16.dp))
                                                Text(
                                                        text = String.format("%.0f", animatedScore),
                                                        style = MaterialTheme.typography.displayLarge,
                                                        fontWeight = FontWeight.Black,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        letterSpacing = (-2).sp
                                                )
                                                Text(
                                                        text = "PERFORMANCE POINTS",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Black,
                                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                                        letterSpacing = 1.sp
                                                )
                                        }
                                }
                            }
                        }
                }

                // Single-Core and Multi-Core Scores (Only for CPU)
                if (summary.type == "CPU") {
                    item {
                        var targetSingle by remember { mutableStateOf(0f) }
                        var targetMulti by remember { mutableStateOf(0f) }
                        LaunchedEffect(Unit) {
                            targetSingle = summary.singleCoreScore.toFloat()
                            targetMulti = summary.multiCoreScore.toFloat()
                        }
                        val animatedSingle by animateFloatAsState(
                            targetValue = targetSingle,
                            animationSpec = tween(durationMillis = 1500, delayMillis = 200, easing = FastOutSlowInEasing)
                        )
                        val animatedMulti by animateFloatAsState(
                            targetValue = targetMulti,
                            animationSpec = tween(durationMillis = 1500, delayMillis = 400, easing = FastOutSlowInEasing)
                        )

                        AnimatedEntranceContainer(index = 1) {
                            Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                    // Single-Core Score
                                    Card(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(28.dp),
                                            colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                            ),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                                    ) {
                                            Box(
                                                    modifier = Modifier
                                                            .fillMaxWidth()
                                                            .background(
                                                                    brush = Brush.verticalGradient(
                                                                            colors = listOf(
                                                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f),
                                                                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.02f)
                                                                            )
                                                                    )
                                                            )
                                            ) {
                                                    Column(
                                                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                                                            horizontalAlignment = Alignment.CenterHorizontally
                                                    ) {
                                                            Text(
                                                                    text = "SINGLE-CORE",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    fontWeight = FontWeight.Black,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                                    letterSpacing = 1.sp
                                                            )
                                                            Spacer(modifier = Modifier.height(10.dp))
                                                            Text(
                                                                    text = String.format("%.0f", animatedSingle),
                                                                    style = MaterialTheme.typography.headlineMedium,
                                                                    fontWeight = FontWeight.Black,
                                                                    color = MaterialTheme.colorScheme.onSurface,
                                                                    letterSpacing = (-1).sp
                                                            )
                                                    }
                                            }
                                    }

                                    // Multi-Core Score
                                    Card(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(28.dp),
                                            colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                            ),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                                    ) {
                                            Box(
                                                    modifier = Modifier
                                                            .fillMaxWidth()
                                                            .background(
                                                                    brush = Brush.verticalGradient(
                                                                            colors = listOf(
                                                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f),
                                                                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.02f)
                                                                            )
                                                                    )
                                                            )
                                            ) {
                                                    Column(
                                                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                                                            horizontalAlignment = Alignment.CenterHorizontally
                                                    ) {
                                                            Text(
                                                                    text = "MULTI-CORE",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    fontWeight = FontWeight.Black,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                                    letterSpacing = 1.sp
                                                            )
                                                            Spacer(modifier = Modifier.height(10.dp))
                                                            Text(
                                                                    text = String.format("%.0f", animatedMulti),
                                                                    style = MaterialTheme.typography.headlineMedium,
                                                                    fontWeight = FontWeight.Black,
                                                                    color = MaterialTheme.colorScheme.onSurface,
                                                                    letterSpacing = (-1).sp
                                                            )
                                                    }
                                            }
                                    }
                            }
                        }
                    }
                }

                // Efficiency Card - High-Fidelity Glassmorphism (CPU Only)
                if (summary.type == "CPU") {
                    item {
                            val mpRatio = if (summary.singleCoreScore > 0) {
                                    summary.multiCoreScore / summary.singleCoreScore
                            } else {
                                    1.0
                            }
                            
                            AnimatedEntranceContainer(index = 2) {
                                Card(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                        shape = RoundedCornerShape(24.dp),
                                        colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.12f)
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f))
                                ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    Brush.horizontalGradient(
                                                        listOf(
                                                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f),
                                                            Color.Transparent
                                                        )
                                                    )
                                                )
                                        ) {
                                            Row(
                                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Box(
                                                                    modifier = Modifier
                                                                            .size(48.dp)
                                                                            .clip(CircleShape)
                                                                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)),
                                                                    contentAlignment = Alignment.Center
                                                            ) {
                                                                    Icon(
                                                                            imageVector = Icons.Rounded.Speed,
                                                                            contentDescription = null,
                                                                            tint = MaterialTheme.colorScheme.tertiary,
                                                                            modifier = Modifier.size(26.dp)
                                                                    )
                                                            }
                                                            Spacer(modifier = Modifier.width(16.dp))
                                                            Column {
                                                                    Text(
                                                                            text = "MULTI-CORE SCALING",
                                                                            style = MaterialTheme.typography.labelSmall,
                                                                            fontWeight = FontWeight.Black,
                                                                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f),
                                                                            letterSpacing = 1.sp
                                                                    )
                                                                    Text(
                                                                            text = if (mpRatio > 4) "Excellent Parallelism" else "Standard Scaling",
                                                                            style = MaterialTheme.typography.bodySmall,
                                                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                                    )
                                                            }
                                                    }
                                                    Column(horizontalAlignment = Alignment.End) {
                                                        Text(
                                                                text = String.format("%.2fx", mpRatio),
                                                                style = MaterialTheme.typography.titleLarge,
                                                                fontWeight = FontWeight.Black,
                                                                color = MaterialTheme.colorScheme.tertiary
                                                        )
                                                        Text(
                                                            text = "FACTOR",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Black,
                                                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
                                                        )
                                                    }
                                            }
                                        }
                                }
                            }
                    }
                }

                // Device Info - High-Fidelity Glassmorphism
                item {
                        summary.deviceSummary?.let { device ->
                           AnimatedEntranceContainer(index = 3) {
                                Card(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                        shape = RoundedCornerShape(28.dp),
                                        colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                                ) {
                                        Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Info,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Text(
                                                            text = "Device Information",
                                                            style = MaterialTheme.typography.titleMedium,
                                                            fontWeight = FontWeight.Black,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            letterSpacing = 0.5.sp
                                                    )
                                                }

                                                // Basic Info
                                                SummaryInfoRow("Device Model", device.deviceName)
                                                SummaryInfoRow("OS Version", "${device.os} (API ${android.os.Build.VERSION.SDK_INT})")
                                                
                                                HorizontalDivider(
                                                        modifier = Modifier.padding(vertical = 16.dp),
                                                        thickness = 0.5.dp,
                                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                                                )

                                                // CPU Info Section
                                                DeviceInfoSectionHeader("Processor Architecture", device.cpuName)
                                                SummaryInfoRow("Core Count", "${device.cpuCores} Cores")
                                                SummaryInfoRow("CPU Governor", device.cpuGovernor)

                                                HorizontalDivider(
                                                        modifier = Modifier.padding(vertical = 16.dp),
                                                        thickness = 0.5.dp,
                                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                                                )

                                                // GPU Info Section
                                                DeviceInfoSectionHeader("Graphics Unit", device.gpuName)
                                                SummaryInfoRow("Vulkan API", if (device.vulkanSupported) "Supported (${device.vulkanVersion ?: "Yes"})" else "Not Supported")

                                                HorizontalDivider(
                                                        modifier = Modifier.padding(vertical = 16.dp),
                                                        thickness = 0.5.dp,
                                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                                                )

                                                // Memory Section
                                                DeviceInfoSectionHeader("Memory Resources", formatBytes(device.totalRam))
                                                SummaryInfoRow("Swap Available", if (device.totalSwap > 0) formatBytes(device.totalSwap) else "Inactive")
                                        }
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
    if (summary.type == "AI") {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Performance Monitoring Section (Optional/Shared)
            // AI Score Card (Prominent Top Placement)
            item {
                AnimatedEntranceContainer(index = 0) {
                     Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                        ),
                        elevation = CardDefaults.cardElevation(0.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "AI BENCHMARK SCORE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                    letterSpacing = 1.5.sp
                                )
                                Text(
                                    text = "Total Performance",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            Text(
                                text = String.format("%.0f", summary.finalScore),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = (-1).sp
                            )
                        }
                    }
                }
            }

            // AI Benchmarks Section
            item {
                AnimatedEntranceContainer(index = 1) {
                    BenchmarkSection(
                            title = "Detailed AI Results",
                            score = summary.finalScore,
                            results = summary.detailedResults,
                            isAi = true
                    )
                }
            }

            // Performance Monitoring Section (Moved to bottom)
            item {
                AnimatedEntranceContainer(index = 2) {
                    PerformanceMonitoringSection(
                            performanceMetricsJson = summary.performanceMetricsJson
                    )
                }
            }
        }
    } else if (summary.type == "GPU") {
        // ── GPU benchmark results ─────────────────────────────────────────
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // GPU Score Card
            item {
                AnimatedEntranceContainer(index = 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                        ),
                        elevation = CardDefaults.cardElevation(0.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "GPU BENCHMARK SCORE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                    letterSpacing = 1.5.sp
                                )
                                Text(
                                    text = "Rendering Performance",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Avg FPS: ${"%.1f".format(summary.multiCoreScore)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                            Text(
                                text = String.format("%.0f", summary.finalScore),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = (-1).sp
                            )
                        }
                    }
                }
            }

            // GPU FPS Per-Scene Graph
            item {
                AnimatedEntranceContainer(index = 1) {
                    if (summary.detailedResults.isNotEmpty()) {
                        GpuFpsBarChart(results = summary.detailedResults)
                    }
                }
            }

            // GPU Scene Results
            item {
                AnimatedEntranceContainer(index = 2) {
                    BenchmarkSection(
                        title = "GPU Rendering Results",
                        score = summary.finalScore,
                        results = summary.detailedResults,
                        isAi = false,
                        isGpu = true
                    )
                }
            }

            // Performance Monitoring Section (CPU/power/temp graphs)
            item {
                AnimatedEntranceContainer(index = 3) {
                    PerformanceMonitoringSection(
                        performanceMetricsJson = summary.performanceMetricsJson
                    )
                }
            }
        }
    } else if (summary.type == "RAM") {
        // ── RAM benchmark results ─────────────────────────────────────────
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // RAM Score Card
            item {
                AnimatedEntranceContainer(index = 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                        ),
                        elevation = CardDefaults.cardElevation(0.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "RAM BENCHMARK SCORE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                    letterSpacing = 1.5.sp
                                )
                                Text(
                                    text = "Memory Subsystem Performance",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Avg BW: ${String.format("%.0f", summary.multiCoreScore)} MB/s",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                            Text(
                                text = String.format("%.0f", summary.finalScore),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = (-1).sp
                            )
                        }
                    }
                }
            }

            // RAM Bandwidth Bar Chart
            item {
                AnimatedEntranceContainer(index = 1) {
                    if (summary.detailedResults.isNotEmpty()) {
                        RamBandwidthChart(results = summary.detailedResults)
                    }
                }
            }

            // RAM Test Results list
            item {
                AnimatedEntranceContainer(index = 2) {
                    BenchmarkSection(
                        title = "RAM Test Results",
                        score = summary.finalScore,
                        results = summary.detailedResults,
                        isAi = false,
                        isGpu = false,
                        isRam = true
                    )
                }
            }

            // Performance Monitoring
            item {
                AnimatedEntranceContainer(index = 3) {
                    PerformanceMonitoringSection(
                        performanceMetricsJson = summary.performanceMetricsJson
                    )
                }
            }
        }
    } else if (summary.type == "STORAGE") {
        // ── Storage benchmark results ─────────────────────────────────────
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Storage Score Card
            item {
                AnimatedEntranceContainer(index = 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                        ),
                        elevation = CardDefaults.cardElevation(0.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "STORAGE BENCHMARK SCORE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                    letterSpacing = 1.5.sp
                                )
                                Text(
                                    text = "Flash I/O Performance",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Avg BW: ${String.format("%.0f", summary.multiCoreScore)} MB/s",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                            Text(
                                text = String.format("%.0f", summary.finalScore),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = (-1).sp
                            )
                        }
                    }
                }
            }

            // Storage Throughput Bar Chart
            item {
                AnimatedEntranceContainer(index = 1) {
                    if (summary.detailedResults.isNotEmpty()) {
                        StorageThroughputChart(results = summary.detailedResults)
                    }
                }
            }

            // Storage Test Results list
            item {
                AnimatedEntranceContainer(index = 2) {
                    BenchmarkSection(
                        title = "Storage Test Results",
                        score = summary.finalScore,
                        results = summary.detailedResults,
                        isAi = false,
                        isGpu = false,
                        isRam = true  // same metricsJson format: {score, value, unit}
                    )
                }
            }

            // Performance Monitoring
            item {
                AnimatedEntranceContainer(index = 3) {
                    PerformanceMonitoringSection(
                        performanceMetricsJson = summary.performanceMetricsJson
                    )
                }
            }
        }
    } else if (summary.type == "PRODUCTIVITY") {
        // ── Productivity benchmark results ────────────────────────────────────
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Score Card
            item {
                AnimatedEntranceContainer(index = 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                        ),
                        elevation = CardDefaults.cardElevation(0.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "PRODUCTIVITY SCORE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                    letterSpacing = 1.5.sp
                                )
                                Text(
                                    text = "Canvas  •  Image  •  Text  •  JSON  •  Compression",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            Text(
                                text = String.format("%.0f", summary.finalScore),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = (-1).sp
                            )
                        }
                    }
                }
            }

            // Productivity Bar Chart
            item {
                AnimatedEntranceContainer(index = 1) {
                    if (summary.detailedResults.isNotEmpty()) {
                        ProductivityBarChart(results = summary.detailedResults)
                    }
                }
            }

            // Productivity Test Results list
            item {
                AnimatedEntranceContainer(index = 2) {
                    BenchmarkSection(
                        title = "Productivity Test Results",
                        score = summary.finalScore,
                        results = summary.detailedResults,
                        isAi = false,
                        isGpu = false,
                        isRam = true  // same metricsJson format: {score, value, unit}
                    )
                }
            }

            // Performance Monitoring
            item {
                AnimatedEntranceContainer(index = 3) {
                    PerformanceMonitoringSection(
                        performanceMetricsJson = summary.performanceMetricsJson
                    )
                }
            }
        }
    } else {
        // Default CPU Logic
        val singleCoreResults =
                remember(summary.detailedResults) {
                        summary.detailedResults.filter { it.name.startsWith("Single-Core") }
                }
        val multiCoreResults =
                remember(summary.detailedResults) {
                        summary.detailedResults.filter { it.name.startsWith("Multi-Core") }
                }

        LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
                // Performance Monitoring Section - FIRST
                item {
                        AnimatedEntranceContainer(index = 0) {
                            PerformanceMonitoringSection(
                                    performanceMetricsJson = summary.performanceMetricsJson
                            )
                        }
                }

                // Single-Core Section
                item {
                        AnimatedEntranceContainer(index = 1) {
                            BenchmarkSection(
                                    title = "Single-Core Benchmarks",
                                    score = summary.singleCoreScore,
                                    results = singleCoreResults
                            )
                        }
                }

                // Multi-Core Section
                item {
                        AnimatedEntranceContainer(index = 2) {
                            BenchmarkSection(
                                    title = "Multi-Core Benchmarks",
                                    score = summary.multiCoreScore,
                                    results = multiCoreResults
                            )
                        }
                }
        }
    }
}

@Composable
private fun GpuFpsBarChart(results: List<BenchmarkResult>) {
    val barColor       = androidx.compose.ui.graphics.Color(0xFF6CB4FF)
    val frameColor     = androidx.compose.ui.graphics.Color(0xFFFF8A65)
    val labelColor     = MaterialTheme.colorScheme.onSurface
    val gridColor      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val maxFps         = results.maxOfOrNull { it.opsPerSecond.toFloat() }?.coerceAtLeast(1f) ?: 60f

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)
        ),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = "FPS Per Scene",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Average frames per second for each rendering workload",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            val barCount = results.size
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                val W = size.width
                val H = size.height
                val labelH = 36f
                val chartH = H - labelH
                val barW = W / barCount

                // Grid lines at 25%, 50%, 75%, 100% of maxFps
                val gridLevels = listOf(0.25f, 0.5f, 0.75f, 1.0f)
                gridLevels.forEach { frac ->
                    val y = chartH - chartH * frac
                    drawLine(gridColor, Offset(0f, y), Offset(W, y), strokeWidth = 1f)
                }

                results.forEachIndexed { i, result ->
                    val fps    = result.opsPerSecond.toFloat()
                    val frac   = (fps / maxFps).coerceIn(0f, 1f)
                    val left   = i * barW + barW * 0.12f
                    val right  = (i + 1) * barW - barW * 0.12f
                    val top    = chartH - chartH * frac
                    val bottom = chartH

                    // Bar fill
                    val barAlpha = 0.55f + 0.45f * frac
                    drawRect(
                        color = barColor.copy(alpha = barAlpha),
                        topLeft = Offset(left, top),
                        size = Size(right - left, bottom - top)
                    )

                    // FPS value label on bar
                    if (fps > maxFps * 0.08f) {
                        drawContext.canvas.nativeCanvas.drawText(
                            "${"%.0f".format(fps)}",
                            (left + right) / 2f,
                            (top - 6f).coerceAtLeast(14f),
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.WHITE
                                textAlign = android.graphics.Paint.Align.CENTER
                                textSize = 24f
                                isFakeBoldText = true
                                isAntiAlias = true
                            }
                        )
                    }

                    // Scene index label below bar
                    drawContext.canvas.nativeCanvas.drawText(
                        "${i + 1}",
                        (left + right) / 2f,
                        H - 6f,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.argb(180, 200, 200, 200)
                            textAlign = android.graphics.Paint.Align.CENTER
                            textSize = 22f
                            isAntiAlias = true
                        }
                    )
                }
            }

            // Legend: scene name chips in two rows
            Spacer(modifier = Modifier.height(8.dp))
            val half = (results.size + 1) / 2
            listOf(results.take(half), results.drop(half)).forEachIndexed { rowIdx, row ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    row.forEachIndexed { colIdx, result ->
                        val sceneNum = rowIdx * half + colIdx + 1
                        val fps = result.opsPerSecond.toFloat()
                        val fpsColor = when {
                            fps < 10f -> androidx.compose.ui.graphics.Color(0xFFEF5350)
                            fps < 20f -> androidx.compose.ui.graphics.Color(0xFFFF9800)
                            fps < 40f -> androidx.compose.ui.graphics.Color(0xFF66BB6A)
                            else      -> androidx.compose.ui.graphics.Color(0xFF42A5F5)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
                                Text(
                                    text = "#$sceneNum  ${"%.1f".format(fps)} fps",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = fpsColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = result.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RamBandwidthChart(results: List<BenchmarkResult>) {
    val labelColor  = MaterialTheme.colorScheme.onSurface
    val gridColor   = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

    // Separate bandwidth tests from latency (RAND_ACCESS = ns/op, lower is better)
    val bwResults  = results.filter { r ->
        val unit = try { org.json.JSONObject(r.metricsJson).optString("unit", "MB/s") } catch (e: Exception) { "MB/s" }
        unit == "MB/s"
    }
    val maxBW = bwResults.maxOfOrNull { it.opsPerSecond.toFloat() }?.coerceAtLeast(1f) ?: 10_000f

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)
        ),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = "Memory Bandwidth",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "MB/s per test  •  Random Access shown separately as ns/op",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            val barCount = results.size.coerceAtLeast(1)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                val W = size.width
                val H = size.height
                val labelH = 36f
                val chartH = H - labelH
                val barW = W / barCount

                listOf(0.25f, 0.5f, 0.75f, 1.0f).forEach { frac ->
                    val y = chartH - chartH * frac
                    drawLine(gridColor, Offset(0f, y), Offset(W, y), strokeWidth = 1f)
                }

                results.forEachIndexed { i, result ->
                    val metricsObj = try { org.json.JSONObject(result.metricsJson) } catch (e: Exception) { org.json.JSONObject() }
                    val unit  = metricsObj.optString("unit", "MB/s")
                    val value = metricsObj.optDouble("value", result.opsPerSecond).toFloat()
                    val isLatency = unit == "ns/op"

                    // Normalise: latency bar is inverted (lower=better); scale to maxBW universe
                    val frac = if (isLatency) 0.5f  // fixed mid-height for contrast
                               else (value / maxBW).coerceIn(0f, 1f)

                    val barColor = when {
                        isLatency            -> androidx.compose.ui.graphics.Color(0xFFFF8A65)
                        value > maxBW * 0.7f -> androidx.compose.ui.graphics.Color(0xFF66BB6A)
                        value > maxBW * 0.4f -> androidx.compose.ui.graphics.Color(0xFF4FC3F7)
                        else                 -> androidx.compose.ui.graphics.Color(0xFFBA68C8)
                    }

                    val left   = i * barW + barW * 0.12f
                    val right  = (i + 1) * barW - barW * 0.12f
                    val top    = chartH - chartH * frac
                    val bottom = chartH

                    drawRect(
                        color = barColor.copy(alpha = 0.65f),
                        topLeft = Offset(left, top),
                        size = Size(right - left, bottom - top)
                    )

                    val label = if (isLatency) "${"%.0f".format(value)}ns" else "${"%.0f".format(value / 1000f)}G"
                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        (left + right) / 2f,
                        (top - 6f).coerceAtLeast(14f),
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            textAlign = android.graphics.Paint.Align.CENTER
                            textSize = 22f
                            isFakeBoldText = true
                            isAntiAlias = true
                        }
                    )

                    drawContext.canvas.nativeCanvas.drawText(
                        "${i + 1}",
                        (left + right) / 2f,
                        H - 6f,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.argb(180, 200, 200, 200)
                            textAlign = android.graphics.Paint.Align.CENTER
                            textSize = 22f
                            isAntiAlias = true
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            val half = (results.size + 1) / 2
            listOf(results.take(half), results.drop(half)).forEachIndexed { rowIdx, row ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    row.forEachIndexed { colIdx, result ->
                        val num = rowIdx * half + colIdx + 1
                        val metricsObj = try { org.json.JSONObject(result.metricsJson) } catch (e: Exception) { org.json.JSONObject() }
                        val unit  = metricsObj.optString("unit", "MB/s")
                        val value = metricsObj.optDouble("value", result.opsPerSecond)
                        val label = if (unit == "ns/op") "${"%.0f".format(value)} ns"
                                    else "${"%.0f".format(value)} MB/s"
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
                                Text(
                                    text = "#$num  $label",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (unit == "ns/op") androidx.compose.ui.graphics.Color(0xFFFF8A65)
                                            else androidx.compose.ui.graphics.Color(0xFF4FC3F7),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = result.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductivityBarChart(results: List<BenchmarkResult>) {
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

    val maxScore = results.maxOfOrNull { r ->
        try { org.json.JSONObject(r.metricsJson).optInt("score", 0) } catch (e: Exception) { 0 }
    }?.coerceAtLeast(1) ?: 1

    @Composable
    fun barColor(idx: Int): androidx.compose.ui.graphics.Color = when (idx % 6) {
        0    -> androidx.compose.ui.graphics.Color(0xFF7C4DFF)  // deep purple — Canvas
        1    -> androidx.compose.ui.graphics.Color(0xFF00BCD4)  // cyan — Image Filter
        2    -> androidx.compose.ui.graphics.Color(0xFF00E5FF)  // light cyan — Image Resize
        3    -> androidx.compose.ui.graphics.Color(0xFF69F0AE)  // green — Text
        4    -> androidx.compose.ui.graphics.Color(0xFFFFB300)  // amber — JSON
        else -> androidx.compose.ui.graphics.Color(0xFFFF4081)  // pink — Compression
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)
        ),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = "Productivity Scores",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Score per test  •  max = 100 pts  •  geometric mean → total",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            val barCount = results.size.coerceAtLeast(1)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                val W = size.width
                val H = size.height
                val labelH = 36f
                val chartH = H - labelH
                val barW = W / barCount

                // Grid lines at 25 / 50 / 75 / 100
                listOf(0.25f, 0.5f, 0.75f, 1.0f).forEach { frac ->
                    val y = chartH - chartH * frac
                    drawLine(gridColor, androidx.compose.ui.geometry.Offset(0f, y),
                        androidx.compose.ui.geometry.Offset(W, y), strokeWidth = 1f)
                }

                results.forEachIndexed { i, result ->
                    val metricsObj = try { org.json.JSONObject(result.metricsJson) } catch (e: Exception) { org.json.JSONObject() }
                    val score = metricsObj.optInt("score", 0).toFloat()
                    val frac  = (score / maxScore.toFloat()).coerceIn(0.05f, 1f)
                    // color not composable in Canvas; use raw color values matching barColor()
                    val rawColor = when (i % 6) {
                        0    -> android.graphics.Color.parseColor("#7C4DFF")
                        1    -> android.graphics.Color.parseColor("#00BCD4")
                        2    -> android.graphics.Color.parseColor("#00E5FF")
                        3    -> android.graphics.Color.parseColor("#69F0AE")
                        4    -> android.graphics.Color.parseColor("#FFB300")
                        else -> android.graphics.Color.parseColor("#FF4081")
                    }
                    val composeColor = androidx.compose.ui.graphics.Color(rawColor).copy(alpha = 0.75f)

                    val left   = i * barW + barW * 0.12f
                    val right  = (i + 1) * barW - barW * 0.12f
                    val top    = chartH - chartH * frac
                    val bottom = chartH

                    drawRect(
                        color = composeColor,
                        topLeft = androidx.compose.ui.geometry.Offset(left, top),
                        size = androidx.compose.ui.geometry.Size(right - left, bottom - top)
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        score.toInt().toString(),
                        (left + right) / 2f,
                        (top - 6f).coerceAtLeast(14f),
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            textAlign = android.graphics.Paint.Align.CENTER
                            textSize = 22f
                            isFakeBoldText = true
                            isAntiAlias = true
                        }
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        "${i + 1}",
                        (left + right) / 2f,
                        H - 6f,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.argb(180, 200, 200, 200)
                            textAlign = android.graphics.Paint.Align.CENTER
                            textSize = 22f
                            isAntiAlias = true
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            val half = (results.size + 1) / 2
            listOf(results.take(half), results.drop(half)).forEachIndexed { rowIdx, row ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    row.forEachIndexed { colIdx, result ->
                        val num = rowIdx * half + colIdx + 1
                        val metricsObj = try { org.json.JSONObject(result.metricsJson) } catch (e: Exception) { org.json.JSONObject() }
                        val score = metricsObj.optInt("score", 0)
                        val unit  = metricsObj.optString("unit", "")
                        val value = metricsObj.optDouble("value", result.opsPerSecond)
                        val label = when (unit) {
                            "Mchars/s" -> "${"%.1f".format(value)} Mc/s"
                            "docs/s"   -> "${"%.0f".format(value / 1_000)}k docs/s"
                            "MB/s"     -> "${"%.0f".format(value)} MB/s"
                            else       -> "${"%.0f".format(value)} $unit"
                        }
                        val rawColor = when ((num - 1) % 6) {
                            0    -> android.graphics.Color.parseColor("#7C4DFF")
                            1    -> android.graphics.Color.parseColor("#00BCD4")
                            2    -> android.graphics.Color.parseColor("#00E5FF")
                            3    -> android.graphics.Color.parseColor("#69F0AE")
                            4    -> android.graphics.Color.parseColor("#FFB300")
                            else -> android.graphics.Color.parseColor("#FF4081")
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
                                Text(
                                    text = "#$num  $score pts",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = androidx.compose.ui.graphics.Color(rawColor),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = result.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    if (row.size < half) { Box(modifier = Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun StorageThroughputChart(results: List<BenchmarkResult>) {
    val labelColor = MaterialTheme.colorScheme.onSurface
    val gridColor  = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

    // Normalise per-unit groups so bars are always comparable within their group
    val maxValue = results.maxOfOrNull { r ->
        try { org.json.JSONObject(r.metricsJson).optDouble("value", r.opsPerSecond) } catch (e: Exception) { r.opsPerSecond }
    }?.coerceAtLeast(1.0)?.toFloat() ?: 1f

    // Colour palette per unit type
    fun barColor(unit: String): androidx.compose.ui.graphics.Color = when (unit) {
        "MB/s"    -> androidx.compose.ui.graphics.Color(0xFF4FC3F7)   // light-blue
        "files/s" -> androidx.compose.ui.graphics.Color(0xFF4DB6AC)   // teal
        "txn/s"   -> androidx.compose.ui.graphics.Color(0xFFBA68C8)   // purple
        else      -> androidx.compose.ui.graphics.Color(0xFF66BB6A)   // green
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)
        ),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = "Storage Throughput",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Per-test performance  •  blue=MB/s  teal=files/s  purple=txn/s",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            val barCount = results.size.coerceAtLeast(1)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                val W = size.width
                val H = size.height
                val labelH = 36f
                val chartH = H - labelH
                val barW = W / barCount

                listOf(0.25f, 0.5f, 0.75f, 1.0f).forEach { frac ->
                    val y = chartH - chartH * frac
                    drawLine(gridColor, Offset(0f, y), Offset(W, y), strokeWidth = 1f)
                }

                results.forEachIndexed { i, result ->
                    val metricsObj = try { org.json.JSONObject(result.metricsJson) } catch (e: Exception) { org.json.JSONObject() }
                    val unit  = metricsObj.optString("unit", "MB/s")
                    val value = metricsObj.optDouble("value", result.opsPerSecond).toFloat()

                    val frac = (value / maxValue).coerceIn(0.05f, 1f)
                    val color = barColor(unit)

                    val left   = i * barW + barW * 0.12f
                    val right  = (i + 1) * barW - barW * 0.12f
                    val top    = chartH - chartH * frac
                    val bottom = chartH

                    drawRect(
                        color = color.copy(alpha = 0.65f),
                        topLeft = Offset(left, top),
                        size = Size(right - left, bottom - top)
                    )

                    val shortLabel = when (unit) {
                        "MB/s"    -> if (value >= 1000f) "${"%,.0f".format(value / 1000f)}G" else "${"%,.0f".format(value)}M"
                        "files/s" -> "${"%,.0f".format(value)}f"
                        "txn/s"   -> "${"%,.0f".format(value)}t"
                        else      -> "%,.0f".format(value)
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        shortLabel,
                        (left + right) / 2f,
                        (top - 6f).coerceAtLeast(14f),
                        android.graphics.Paint().apply {
                            this.color = android.graphics.Color.WHITE
                            textAlign = android.graphics.Paint.Align.CENTER
                            textSize = 22f
                            isFakeBoldText = true
                            isAntiAlias = true
                        }
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        "${i + 1}",
                        (left + right) / 2f,
                        H - 6f,
                        android.graphics.Paint().apply {
                            this.color = android.graphics.Color.argb(180, 200, 200, 200)
                            textAlign = android.graphics.Paint.Align.CENTER
                            textSize = 22f
                            isAntiAlias = true
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            val half = (results.size + 1) / 2
            listOf(results.take(half), results.drop(half)).forEachIndexed { rowIdx, row ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    row.forEachIndexed { colIdx, result ->
                        val num = rowIdx * half + colIdx + 1
                        val metricsObj = try { org.json.JSONObject(result.metricsJson) } catch (e: Exception) { org.json.JSONObject() }
                        val unit  = metricsObj.optString("unit", "MB/s")
                        val value = metricsObj.optDouble("value", result.opsPerSecond)
                        val label = "${"%,.0f".format(value)} $unit"
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
                                Text(
                                    text = "#$num  $label",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = androidx.compose.ui.graphics.Color(0xFF4FC3F7),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = result.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BenchmarkSection(title: String, score: Double, results: List<BenchmarkResult>, isAi: Boolean = false, isGpu: Boolean = false, isRam: Boolean = false) {
        var expanded by remember { mutableStateOf(true) }

        Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
        ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        // Section Header
                        Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { expanded = !expanded }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val icon = when {
                                                isAi -> Icons.Rounded.AutoAwesome
                                                results.firstOrNull()?.name?.startsWith("Single") == true -> Icons.Rounded.Person
                                                else -> Icons.Rounded.Star
                                            }
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                                Text(
                                                        text = title,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                        text = "Section Total: ${String.format("%.0f", score)} PTS",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                                )
                                        }
                                }
                                Icon(
                                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                        }

                        // Benchmark List
                        if (expanded) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    results.forEach { result ->
                                        BenchmarkResultItem(result, isAi, isGpu, isRam)
                                    }
                                }
                        }
                }
        }
}

@Composable
fun BenchmarkResultItem(result: BenchmarkResult, isAi: Boolean = false, isGpu: Boolean = false, isRam: Boolean = false) {
        val cleanName = result.name.replace("Single-Core ", "").replace("Multi-Core ", "")
        val timeInSeconds = result.executionTimeMs / 1000.0

        val displayThroughput: String
        val individualScore: Double

        if (isAi) {
             // For AI, opsPerSecond is tokens/sec or operations/sec
             displayThroughput = String.format("%.2f ops/s", result.opsPerSecond)
             // Use pre-calculated score if available, or calculate using factor 2.0
             val scalingFactors = KotlinBenchmarkManager.SCORING_FACTORS
             val benchmarkName = BenchmarkName.fromString(result.name)
             individualScore = benchmarkName?.let { scalingFactors[it]?.times(result.opsPerSecond) } ?: (result.opsPerSecond * 2.0)
        } else if (isGpu) {
             // For GPU, opsPerSecond holds avgFps; show FPS and read score from metricsJson
             displayThroughput = String.format("%.1f FPS  /  %.1f ms", result.opsPerSecond, result.executionTimeMs)
             individualScore = try {
                 org.json.JSONObject(result.metricsJson).optDouble("score", result.opsPerSecond * 10.0)
             } catch (e: Exception) { result.opsPerSecond * 10.0 }
        } else if (isRam) {
             // For RAM/STORAGE: opsPerSecond holds the value; read unit and score from metricsJson
             val metricsObj = try { org.json.JSONObject(result.metricsJson) } catch (e: Exception) { org.json.JSONObject() }
             val unit = metricsObj.optString("unit", "MB/s")
             val value = metricsObj.optDouble("value", result.opsPerSecond)
             displayThroughput = if (unit == "ns/op") String.format("%.1f ns/op", value)
                                 else String.format("%.0f %s", value, unit)
             individualScore = metricsObj.optDouble("score", 0.0)
        } else {
             // For CPU, opsPerSecond is usually raw ops, converted to Mops/s
             displayThroughput = String.format("%.2f Mops/s", result.opsPerSecond / 1_000_000.0)
             
             val scalingFactors = KotlinBenchmarkManager.SCORING_FACTORS
             val benchmarkName = BenchmarkName.fromString(result.name)
             individualScore = benchmarkName?.let { scalingFactors[it]?.times(result.opsPerSecond) } ?: 0.0
        }

        Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.08f)
                ),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f))
        ) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Text(
                                        text = cleanName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)
                                )
                                
                                // Acceleration Mode Badge
                                if (isAi && result.accelerationMode != null && result.accelerationMode.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = when(result.accelerationMode) {
                                                    "NPU" -> MaterialTheme.colorScheme.tertiaryContainer
                                                    "GPU" -> MaterialTheme.colorScheme.secondaryContainer
                                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                                },
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = result.accelerationMode,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Text(
                                        text = String.format("%.1f", individualScore),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary
                                )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ExpandLess,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                            text = displayThroughput,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f)
                                    )
                                }
                                
                                Text(
                                        text = String.format("%.3fs", timeInSeconds),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                        }
                }
        }
}

@Composable
private fun RankingsTab(finalScore: Double, singleCoreScore: Double, multiCoreScore: Double, type: String = "CPU") {
    val scrollState = androidx.compose.foundation.rememberScrollState()
    
    // If not CPU, show "Coming Soon" placeholder
    if (type != "CPU") {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Leaderboard,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "${type} Rankings Coming Soon",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "We are currently collecting data to build accurate rankings for this benchmark category.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    
    // Create rankings logic
    val hardcodedReferenceDevices = listOf(
        RankingItem(name = "Snapdragon 8 Gen 3", normalizedScore = 313, singleCore = 100, multiCore = 420, isCurrentUser = false, tag = "Baseline"),
        RankingItem(name = "MediaTek Dimensity 8300", normalizedScore = 229, singleCore = 78, multiCore = 308, isCurrentUser = false),
        RankingItem(name = "Snapdragon 8s Gen 3", normalizedScore = 241, singleCore = 87, multiCore = 324, isCurrentUser = false),
        RankingItem(name = "MediaTek Dimensity 6300", normalizedScore = 107, singleCore = 50, multiCore = 137, isCurrentUser = false)
    )

    val userDeviceName = "Your Device (${android.os.Build.MODEL})"
    val currentUserScore = RankingItem(
        name = userDeviceName, 
        normalizedScore = finalScore.toInt(), 
        singleCore = singleCoreScore.toInt(), 
        multiCore = multiCoreScore.toInt(), 
        isCurrentUser = true
    )
    
    val allDevices = mutableListOf<RankingItem>().apply {
        addAll(hardcodedReferenceDevices)
        add(currentUserScore)
    }

    val rankedItems = allDevices.sortedByDescending { it.normalizedScore }.mapIndexed { index, item ->
        item.copy(rank = index + 1)
    }

    val userRank = rankedItems.indexOfFirst { it.isCurrentUser }
    val totalDevices = rankedItems.size
    val beatsPercentage = if (totalDevices > 1) {
        ((totalDevices - userRank - 1).toFloat() / (totalDevices - 1) * 100).toInt()
    } else 100

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 24.dp).verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Final Score Card - Glassmorphic
        AnimatedEntranceContainer(index = 0) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                ),
                elevation = CardDefaults.cardElevation(0.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TOTAL BENCHMARK SCORE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "Current Hardware Rank",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    Text(
                        text = String.format("%.0f", finalScore),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = (-1).sp
                    )
                }
            }
        }

        // 2. Comparison Card (Percentile)
        AnimatedEntranceContainer(index = 1) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)
                ),
                elevation = CardDefaults.cardElevation(0.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.02f)
                                )
                            )
                        )
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = "PERFORMANCE PERCENTILE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f),
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Beats $beatsPercentage% of devices",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "#${userRank + 1}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Animating Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
                        ) {
                            var targetProgress by remember { mutableStateOf(0f) }
                            LaunchedEffect(Unit) {
                                delay(600)
                                targetProgress = beatsPercentage / 100f
                            }
                            val animatedProgress by animateFloatAsState(
                                targetValue = targetProgress,
                                animationSpec = tween(1500, easing = FastOutSlowInEasing)
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(animatedProgress)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.tertiary,
                                                MaterialTheme.colorScheme.primary
                                            )
                                        )
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = when {
                                beatsPercentage >= 90 -> "ELITE: Outperforming almost all reference systems."
                                beatsPercentage >= 70 -> "POWERHOUSE: Strong enough for heavy professional work."
                                beatsPercentage >= 50 -> "COMPETITIVE: Above average performance profile."
                                else -> "STANDARD: Capable hardware for daily operations."
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 3. Comparison Card (Adaptive)
        val nextDevice = if (userRank >= 0 && userRank < rankedItems.size - 1) rankedItems[userRank + 1] else null
        val gapToNext = if (nextDevice != null) {
            ((finalScore - nextDevice.normalizedScore) / nextDevice.normalizedScore * 100).toInt()
        } else 0

        if (userRank >= 0) {
            AnimatedEntranceContainer(index = 2) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.12f)
                    ),
                    elevation = CardDefaults.cardElevation(0.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (userRank == 0) Icons.Rounded.WorkspacePremium else Icons.Rounded.CompareArrows,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = if (userRank == 0) "TOP PERFORMANCE!" else "PERFORMANCE LEAD",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.secondary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = when {
                                    userRank == 0 -> "Your device is the fastest in this cohort."
                                    nextDevice != null -> "Leads ${nextDevice.name} by $gapToNext%"
                                    else -> "Ranking analysis complete."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        // 4. Global Ranking Head
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.Leaderboard,
                null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "GLOBAL LEADERBOARD",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }

        // 5. Rankings List
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            rankedItems.forEachIndexed { index, item ->
                AnimatedEntranceContainer(index = index + 3) {
                    RankingItemCard(item = item)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun RankingItemCard(item: RankingItem) {
    val topScoreMax = 1200
    val scoreProgress by animateFloatAsState(
        targetValue = (item.normalizedScore.toFloat() / topScoreMax).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "scoreProgress"
    )

    val goldColor = Color(0xFFFFD700)
    val silverColor = Color(0xFFC0C0C0)
    val bronzeColor = Color(0xFFCD7F32)

    val rankColor = when (item.rank) {
        1 -> goldColor
        2 -> silverColor
        3 -> bronzeColor
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    }

    val isTop3 = item.rank <= 3
    val containerColor = if (item.isCurrentUser) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
    }

    val borderColor = if (item.isCurrentUser) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rank Badge
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (isTop3) rankColor.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#${item.rank}",
                        fontWeight = FontWeight.Black,
                        color = if (isTop3) rankColor else MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Name & Metadata
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (item.isCurrentUser) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    "YOU",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                    Text(
                        text = "Score: ${item.normalizedScore} pts",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                
                // Trophy/Icon
                if (isTop3) {
                    Text(
                        text = when(item.rank) {
                            1 -> "🥇"
                            2 -> "🥈"
                            else -> "🥉"
                        },
                        fontSize = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { scoreProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = if (item.isCurrentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
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

@Composable
fun SubScoreItem(label: String, score: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Text(
            text = score.toString(),
            style = MaterialTheme.typography.headlineMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}
@Composable
fun DeviceInfoSectionHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            letterSpacing = 1.sp
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun AnimatedEntranceContainer(
    index: Int,
    content: @Composable () -> Unit
) {
    val visibleState = remember {
        MutableTransitionState(false).apply {
            // If the index is 0, we can start visible immediately to avoid perceived lag
            // Otherwise start false and animate in
            targetState = index == 0
        }
    }

    LaunchedEffect(Unit) {
        // Ensure even index 0 gets a trigger if it wasn't already set
        if (!visibleState.targetState) {
            delay(index * 50L) // Reduced delay for snappier feel
            visibleState.targetState = true
        }
    }
    
    AnimatedVisibility(
        visibleState = visibleState,
        enter = fadeIn(animationSpec = tween(400)) + 
                slideInVertically(
                    initialOffsetY = { it / 4 }, // Reduced slide distance
                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                )
    ) {
        content()
    }
}

private fun formatBenchmarkShareData(context: Context, summary: BenchmarkSummary): String {
    val builder = StringBuilder()
    builder.append("FinalBenchmark 2 Results\n")
    builder.append("========================\n\n")

    // Device Info
    summary.deviceSummary?.let { device ->
        builder.append("Device: ${device.deviceName}\n")
        builder.append("Model: ${android.os.Build.MODEL}\n") // Use Build.MODEL as fallback or explicit additional info
        builder.append("OS: ${device.os}\n")
        builder.append("Kernel: ${device.kernel}\n")
        builder.append("CPU: ${device.cpuName}\n")
        builder.append("Cores: ${device.cpuCores}\n")
        builder.append("GPU: ${device.gpuName}\n\n")
    }

    // Scores
    builder.append("TOTAL SCORE: ${String.format("%.0f", summary.finalScore)}\n")
    builder.append("(Normalized: ${String.format("%.0f", summary.normalizedScore)})\n\n")

    builder.append("Single-Core Score: ${String.format("%.0f", summary.singleCoreScore)}\n")
    builder.append("Multi-Core Score: ${String.format("%.0f", summary.multiCoreScore)}\n\n")

    // Detailed Results
    if (summary.detailedResults.isNotEmpty()) {
        builder.append("Detailed Results:\n")
        builder.append("--------------------------------\n")

        if (summary.type == "GPU") {
            // GPU scene results — show FPS and score per scene
            builder.append("[GPU Scene Results]\n")
            summary.detailedResults.forEach { result ->
                val fps = result.opsPerSecond
                val score = try {
                    org.json.JSONObject(result.metricsJson).optDouble("score", fps * 10.0)
                } catch (e: Exception) { fps * 10.0 }
                val frameMs = result.executionTimeMs
                builder.append("${result.name}: ${String.format("%.1f", score)} pts  |  ${String.format("%.1f", fps)} FPS  |  ${String.format("%.1f", frameMs)} ms\n")
            }
        } else if (summary.type == "RAM") {
            // RAM test results — show value + unit and score per test
            builder.append("[RAM Test Results]\n")
            summary.detailedResults.forEach { result ->
                val metricsObj = try { org.json.JSONObject(result.metricsJson) } catch (e: Exception) { org.json.JSONObject() }
                val unit  = metricsObj.optString("unit", "MB/s")
                val value = metricsObj.optDouble("value", result.opsPerSecond)
                val score = metricsObj.optDouble("score", 0.0)
                val valueStr = if (unit == "ns/op") String.format("%.1f ns/op", value)
                               else String.format("%.0f MB/s", value)
                builder.append("${result.name}: ${String.format("%.0f", score)} pts  |  $valueStr\n")
            }
        } else if (summary.type == "STORAGE") {
            // Storage test results — show value + unit and score per test
            builder.append("[Storage Test Results]\n")
            summary.detailedResults.forEach { result ->
                val metricsObj = try { org.json.JSONObject(result.metricsJson) } catch (e: Exception) { org.json.JSONObject() }
                val unit  = metricsObj.optString("unit", "MB/s")
                val value = metricsObj.optDouble("value", result.opsPerSecond)
                val score = metricsObj.optDouble("score", 0.0)
                val valueStr = String.format("%.0f %s", value, unit)
                builder.append("${result.name}: ${String.format("%.0f", score)} pts  |  $valueStr\n")
            }
        } else if (summary.type == "PRODUCTIVITY") {
            // Productivity test results — Canvas, Image, Text, JSON, Compression
            builder.append("[Productivity Test Results]\n")
            summary.detailedResults.forEach { result ->
                val metricsObj = try { org.json.JSONObject(result.metricsJson) } catch (e: Exception) { org.json.JSONObject() }
                val unit  = metricsObj.optString("unit", "ops/s")
                val value = metricsObj.optDouble("value", result.opsPerSecond)
                val score = metricsObj.optDouble("score", 0.0)
                val valueStr = when (unit) {
                    "Mchars/s" -> String.format("%.2f Mchars/s", value)
                    "docs/s"   -> String.format("%.0f docs/s", value)
                    else       -> String.format("%.0f %s", value, unit)
                }
                builder.append("${result.name}: ${String.format("%.0f", score)} pts  |  $valueStr\n")
            }
        } else {
            // Group by Single/Multi for CPU/AI
            val singleCoreResults = summary.detailedResults.filter { it.name.contains("Single-Core") }
            val multiCoreResults = summary.detailedResults.filter { it.name.contains("Multi-Core") }

            if (singleCoreResults.isNotEmpty()) {
                builder.append("[Single-Core Benchmarks]\n")
                singleCoreResults.forEach { result ->
                    val cleanName = result.name.replace("Single-Core ", "")
                    val mopsPerSecond = result.opsPerSecond / 1_000_000.0
                    val benchmarkName = BenchmarkName.fromString(result.name)
                    val scalingFactor = KotlinBenchmarkManager.SCORING_FACTORS[benchmarkName] ?: 0.0
                    val points = scalingFactor * result.opsPerSecond
                    builder.append("$cleanName: ${String.format("%.1f", points)} pts (${String.format("%.2f", mopsPerSecond)} Mops/s)\n")
                }
                builder.append("\n")
            }

            if (multiCoreResults.isNotEmpty()) {
                builder.append("[Multi-Core Benchmarks]\n")
                multiCoreResults.forEach { result ->
                    val cleanName = result.name.replace("Multi-Core ", "")
                    val mopsPerSecond = result.opsPerSecond / 1_000_000.0
                    val benchmarkName = BenchmarkName.fromString(result.name)
                    val scalingFactor = KotlinBenchmarkManager.SCORING_FACTORS[benchmarkName] ?: 0.0
                    val points = scalingFactor * result.opsPerSecond
                    builder.append("$cleanName: ${String.format("%.1f", points)} pts (${String.format("%.2f", mopsPerSecond)} Mops/s)\n")
                }
            }
        }
    }
    
    // Performance Link
    builder.append("\nGenerated by FinalBenchmark 2")
    
    return builder.toString()
}
