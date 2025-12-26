package com.ivarna.finalbenchmark2.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import com.ivarna.finalbenchmark2.data.database.entities.BenchmarkResultEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DisabledByDefault
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivarna.finalbenchmark2.R
import com.ivarna.finalbenchmark2.data.repository.HistoryRepository
import com.ivarna.finalbenchmark2.ui.theme.FinalBenchmark2Theme
import com.ivarna.finalbenchmark2.ui.theme.LocalThemeMode
import com.ivarna.finalbenchmark2.ui.theme.ThemeMode
import com.ivarna.finalbenchmark2.ui.viewmodels.PerformanceOptimizationStatus
import com.ivarna.finalbenchmark2.ui.viewmodels.RankingItem
import com.ivarna.finalbenchmark2.utils.CpuUtilizationUtils
import com.ivarna.finalbenchmark2.utils.PowerUtils
import com.ivarna.finalbenchmark2.utils.TemperatureUtils
import kotlinx.coroutines.delay
import kotlin.math.abs
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.HazeStyle
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
        onStartBenchmark: (String, String) -> Unit, // Updated signature: (preset, type)
        onNavigateToSettings: () -> Unit = {},
        historyRepository: HistoryRepository? = null,
        hazeState: dev.chrisbanes.haze.HazeState? = null
) {

        // --- State Holders for Data ---
        val context = LocalContext.current

        // Temperature State
        val tempUtils = remember { TemperatureUtils(context) }
        var cpuTemp by remember { mutableStateOf(0f) }
        var batteryTemp by remember { mutableStateOf(0f) }

        // CPU State
        val cpuUtilizationUtils = remember { CpuUtilizationUtils(context) }
        var cpuUtilization by remember { mutableStateOf(0f) }
        var coreUtilizations by remember { mutableStateOf<Map<Int, Float>>(emptyMap()) }
        var allCoreFrequencies by remember {
                mutableStateOf<Map<Int, Pair<Long, Long>>>(emptyMap())
        }

        // Power State
        val powerUtils = remember { PowerUtils(context) }
        var powerInfo by remember { mutableStateOf(powerUtils.getPowerConsumptionInfo()) }

        var isDataInitialized by remember { mutableStateOf(false) }

        // Workload Selection State
        val workloadOptions = listOf("Low Accuracy - Fastest", "Mid Accuracy - Fast", "High Accuracy - Slow")
        var selectedWorkload by remember { mutableStateOf("High Accuracy - Slow") }
        var isDropdownExpanded by remember { mutableStateOf(false) }

        // Swipe state for high score card
        var showHighScoreCard by remember { mutableStateOf(true) }
        var highestScoreEntity by remember { mutableStateOf<BenchmarkResultEntity?>(null) }
        
        // Dialog State
        var showDownloadDialog by remember { mutableStateOf(false) }

        // Load highest score from database
        LaunchedEffect(historyRepository) {
                if (historyRepository != null) {
                        historyRepository.getAllResults().collect { results ->
                                highestScoreEntity = results
                                        .filter { it.benchmarkResult.type.contains("CPU", ignoreCase = true) }
                                        .maxByOrNull { it.benchmarkResult.normalizedScore }
                                        ?.benchmarkResult
                        }
                }
        }

        // Single LaunchedEffect to manage data polling loops
        LaunchedEffect(Unit) {
                isDataInitialized = true
                while (true) {
                        // Update all data points
                        cpuTemp = tempUtils.getCpuTemperature()
                        batteryTemp = tempUtils.getBatteryTemperature()

                        cpuUtilization = cpuUtilizationUtils.getCpuUtilizationPercentage()
                        coreUtilizations = cpuUtilizationUtils.getCoreUtilizationPercentages()
                        allCoreFrequencies = cpuUtilizationUtils.getAllCoreFrequencies()

                        powerInfo = powerUtils.getPowerConsumptionInfo()

                        delay(1000) // 1 second update rate
                }
        }

        FinalBenchmark2Theme {
                Box(
                    modifier = Modifier.fillMaxSize()
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

                            // Main scrollable content
                            Column(
                                    modifier =
                                            Modifier.fillMaxSize()
                                                    .verticalScroll(rememberScrollState())
                                                    .padding(24.dp)
                                                    .padding(
                                                            top = 60.dp
                                                    ), // Add top padding for floating button
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                            ) {
                                // Swipeable section: High Score Card OR Logo/Title/Description
                                val density = LocalDensity.current
                                val swipeThreshold = with(density) { 100.dp.toPx() }

                                // Use Box with fixed height to prevent resizing during animation
                                Box(
                                        modifier = Modifier
                                                .fillMaxWidth()
                                                // Removed fixed height to allow wrapping
                                                .pointerInput(Unit) {
                                                        detectHorizontalDragGestures { _, dragAmount ->
                                                                if (abs(dragAmount) > swipeThreshold / 10) {
                                                                        if (dragAmount < 0) {
                                                                                // Left swipe - show high score card
                                                                                if (highestScoreEntity != null) {
                                                                                        showHighScoreCard = true
                                                                                }
                                                                        } else {
                                                                                // Right swipe - show logo/title/description
                                                                                showHighScoreCard = false
                                                                        }
                                                                }
                                                        }
                                                },
                                        contentAlignment = Alignment.Center
                                ) {
                                        AnimatedContent(
                                                targetState = showHighScoreCard && highestScoreEntity != null,
                                                label = "swipe_animation"
                                        ) { showScore ->
                                                if (showScore && highestScoreEntity != null) {
                                                        // High Score Card View
                                                        Column(
                                                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                                                horizontalAlignment = Alignment.CenterHorizontally
                                                        ) {
                                                                HighScoreCard(
                                                                        score = highestScoreEntity!!.normalizedScore,
                                                                        deviceModel = highestScoreEntity!!.deviceModel,
                                                                        timestamp = highestScoreEntity!!.timestamp,
                                                                        historyRepository = historyRepository
                                                                )
                                                        }
                                                } else {
                                                        // Logo/Title/Description
                                                        Column(
                                                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                                                horizontalAlignment = Alignment.CenterHorizontally
                                                        ) {
                                                                // App logo
                                                                Box(
                                                                        modifier = Modifier.size(120.dp),
                                                                        contentAlignment = Alignment.Center
                                                                ) {
                                                                        Box(
                                                                                modifier = Modifier
                                                                                        .size(110.dp)
                                                                                        .clip(CircleShape)
                                                                                        .background(Color(0xFF2A2A2A))
                                                                        )
                                                                        Image(
                                                                                painter = painterResource(id = R.drawable.logo_2),
                                                                                contentDescription = "Logo",
                                                                                modifier = Modifier.size(90.dp)
                                                                        )
                                                                }

                                                                Spacer(modifier = Modifier.height(16.dp))

                                                                Text(
                                                                        text = "FinalBenchmark2",
                                                                        fontSize = 32.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = MaterialTheme.colorScheme.primary,
                                                                        textAlign = TextAlign.Center,
                                                                        modifier = Modifier.padding(bottom = 16.dp)
                                                                )

                                                                Text(
                                                                        text = "A comprehensive benchmarking application that tests your device's performance across multiple components.",
                                                                        fontSize = 16.sp,
                                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                        textAlign = TextAlign.Center,
                                                                        modifier = Modifier.padding(bottom = 16.dp)
                                                                )
                                                        }
                                                }
                                        }
                                }

                                // Dot Navigation Indicators (only show if high score exists)
                                if (highestScoreEntity != null) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                        ) {
                                                // Dot for App Info (left)
                                                Box(
                                                        modifier = Modifier
                                                                .size(8.dp)
                                                                .clip(CircleShape)
                                                                .background(
                                                                        if (!showHighScoreCard) MaterialTheme.colorScheme.primary
                                                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                                                )
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                // Dot for High Score (right)
                                                Box(
                                                        modifier = Modifier
                                                                .size(8.dp)
                                                                .clip(CircleShape)
                                                                .background(
                                                                        if (showHighScoreCard) MaterialTheme.colorScheme.primary
                                                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                                                )
                                                )
                                        }
                                }

                                // =========================================================
                                // BENCHMARK CONTROLS
                                // =========================================================
                                // BENCHMARK CONTROLS
                                // =========================================================
                                var selectedBenchmarkCategory by remember { mutableStateOf(com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkCategory.CPU) }
                                val benchmarkCategories = com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkCategory.entries
                                var isTypeDropdownExpanded by remember { mutableStateOf(false) }

                                com.ivarna.finalbenchmark2.ui.components.AnimatedGlassCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    shape = RoundedCornerShape(24.dp),
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f),
                                    borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                    delayMillis = 0 // Immediate appearance
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        // Workload Dropdown
                                        ExposedDropdownMenuBox(
                                            expanded = isDropdownExpanded,
                                            onExpandedChange = { isDropdownExpanded = !isDropdownExpanded },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            OutlinedTextField(
                                                value = selectedWorkload,
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("Workload Intensity") },
                                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            ExposedDropdownMenu(
                                                expanded = isDropdownExpanded,
                                                onDismissRequest = { isDropdownExpanded = false }
                                            ) {
                                                workloadOptions.forEach { option ->
                                                    DropdownMenuItem(
                                                        text = { Text(option) },
                                                        onClick = {
                                                            selectedWorkload = option
                                                            isDropdownExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        // Benchmark Type Dropdown
                                        ExposedDropdownMenuBox(
                                            expanded = isTypeDropdownExpanded,
                                            onExpandedChange = { isTypeDropdownExpanded = !isTypeDropdownExpanded },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            OutlinedTextField(
                                                value = selectedBenchmarkCategory.name,
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("Benchmark Type") },
                                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isTypeDropdownExpanded) },
                                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            
                                            ExposedDropdownMenu(
                                                expanded = isTypeDropdownExpanded,
                                                onDismissRequest = { isTypeDropdownExpanded = false }
                                            ) {
                                                benchmarkCategories.forEach { category ->
                                                    DropdownMenuItem(
                                                        text = { Text(category.name) },
                                                        onClick = {
                                                            selectedBenchmarkCategory = category
                                                            isTypeDropdownExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        // Start Benchmark Button - Glassmorphic Style
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(56.dp)
                                                .shadow(
                                                    elevation = 8.dp,
                                                    shape = RoundedCornerShape(28.dp),
                                                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                                )
                                                .clip(RoundedCornerShape(28.dp))
                                                .clickable {
                                                    // Check if AI type is selected and models are missing
                                                    if (selectedBenchmarkCategory == com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkCategory.AI) {
                                                        val areModelsReady = com.ivarna.finalbenchmark2.aiBenchmark.ModelDownloader.areAllModelsDownloaded(context)
                                                        if (!areModelsReady) {
                                                            // Show download dialog instead of starting
                                                            showDownloadDialog = true
                                                            return@clickable
                                                        }
                                                    }

                                                    val activity = context as? com.ivarna.finalbenchmark2.MainActivity
                                                    activity?.startAllOptimizations()
                                                    val deviceTier = when (selectedWorkload) {
                                                        "Low Accuracy - Fastest" -> "slow"
                                                        "Mid Accuracy - Fast" -> "mid"
                                                        "High Accuracy - Slow" -> "flagship"
                                                        else -> "flagship"
                                                    }
                                                    onStartBenchmark(deviceTier, selectedBenchmarkCategory.name)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            // Content Overlay
                                            val infiniteTransition = rememberInfiniteTransition(label = "button_spin")
                                            val angle by infiniteTransition.animateFloat(
                                                initialValue = 0f,
                                                targetValue = 360f,
                                                animationSpec = infiniteRepeatable(
                                                    animation = tween(durationMillis = 4000, easing = LinearEasing),
                                                    repeatMode = RepeatMode.Restart
                                                ),
                                                label = "spin_angle"
                                            )
                                            
                                            val primary = MaterialTheme.colorScheme.primary
                                            val tertiary = MaterialTheme.colorScheme.tertiary
                                            
                                            // Rotating Background
                                            // Rotating Background - Doc Implementation
                                            Spacer(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .drawWithCache {
                                                        val brush = Brush.sweepGradient(
                                                            colors = listOf(
                                                                primary,
                                                                tertiary,
                                                                primary
                                                            )
                                                        )
                                                        onDrawBehind {
                                                            val radius = size.maxDimension
                                                            // pivot defaults to center
                                                            rotate(degrees = angle) {
                                                                drawCircle(
                                                                    brush = brush,
                                                                    radius = radius
                                                                )
                                                            }
                                                        }
                                                    }
                                            )
                                            // Inner Content Mask - Creates the Border Effect
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(2.dp) // Border Width
                                                    .clip(RoundedCornerShape(26.dp))
                                                    .background(MaterialTheme.colorScheme.background),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        painterResource(id = R.drawable.mobile_24),
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onBackground,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        "START BENCHMARK",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 16.sp,
                                                        color = MaterialTheme.colorScheme.onBackground
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // ROM Compatibility Warning Card (moved here, below swipeable section)
                                RomCompatibilityWarningCard()

                                Spacer(modifier = Modifier.height(16.dp))

                                // =========================================================
                                // SYSTEM STATS ROW (Split for Variety)
                                // =========================================================
                                if (isDataInitialized) {
                                    var isSystemStatsExpanded by remember { mutableStateOf(false) }

                                    // Row of 3 Cards with staggered animation
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp) // Gap between cards
                                    ) {
                                        // 1. Temperature Card (Delayed 100ms)
                                        Box(modifier = Modifier.weight(1f)) {
                                            com.ivarna.finalbenchmark2.ui.components.AnimatedGlassCard(
                                                delayMillis = 100,
                                                shape = RoundedCornerShape(24.dp)
                                            ) {
                                                SmallStatCard(
                                                    modifier = Modifier.fillMaxWidth(), // Inherit internal width from glass card
                                                    icon = Icons.Rounded.Thermostat,
                                                    label = "TEMP",
                                                    value = "${if(cpuTemp > 0) cpuTemp else "--"}°C",
                                                    color = MaterialTheme.colorScheme.error,
                                                    onClick = { isSystemStatsExpanded = !isSystemStatsExpanded }
                                                )
                                            }
                                        }

                                        // 2. CPU Load Card (Delayed 200ms)
                                        Box(modifier = Modifier.weight(1f)) {
                                            com.ivarna.finalbenchmark2.ui.components.AnimatedGlassCard(
                                                delayMillis = 200,
                                                shape = RoundedCornerShape(24.dp)
                                            ) {
                                                SmallStatCard(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    icon = Icons.Rounded.Memory,
                                                    label = "CPU",
                                                    value = "${String.format("%.0f", cpuUtilization)}%",
                                                    color = MaterialTheme.colorScheme.primary,
                                                    onClick = { isSystemStatsExpanded = !isSystemStatsExpanded }
                                                )
                                            }
                                        }

                                        // 3. Power Card (Delayed 300ms)
                                        Box(modifier = Modifier.weight(1f)) {
                                            com.ivarna.finalbenchmark2.ui.components.AnimatedGlassCard(
                                                delayMillis = 300,
                                                shape = RoundedCornerShape(24.dp)
                                            ) {
                                                SmallStatCard(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    icon = Icons.Rounded.Bolt,
                                                    label = "POWER",
                                                    value = "${String.format("%.1f", powerInfo.power)}W",
                                                    color = MaterialTheme.colorScheme.tertiary,
                                                    onClick = { isSystemStatsExpanded = !isSystemStatsExpanded }
                                                )
                                            }
                                        }
                                    }

                                    // Detailed Stats (Expandable Section)
                                    AnimatedVisibility(
                                        visible = isSystemStatsExpanded,
                                        enter = expandVertically(animationSpec = tween(300)) + fadeIn(),
                                        exit = shrinkVertically(animationSpec = tween(300)) + fadeOut()
                                    ) {
                                        com.ivarna.finalbenchmark2.ui.components.AnimatedGlassCard(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 16.dp),
                                            shape = RoundedCornerShape(24.dp),
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                                            borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                                            delayMillis = 0 // Expand animation handles timing
                                        ) {
                                            Column(modifier = Modifier.padding(20.dp)) {
                                                Text(
                                                    text = "DETAILED STATISTICS",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 1.sp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(bottom = 16.dp)
                                                )

                                                // Row 1: Detailed Values Grid
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    DetailIconPair(Icons.Rounded.Memory, "${cpuTemp}°C", "CPU")
                                                    DetailIconPair(Icons.Rounded.BatteryStd, "${batteryTemp}°C", "Batt")
                                                    DetailIconPair(Icons.Rounded.ElectricBolt, "${String.format("%.1f", powerInfo.voltage)}V", "Volts")
                                                    DetailIconPair(Icons.Rounded.Bolt, "${String.format("%.1f", powerInfo.current)}A", "Amps")
                                                }

                                                HorizontalDivider(
                                                    modifier = Modifier.padding(vertical = 16.dp),
                                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                                )

                                                // Row 2: CPU Cores Visualization
                                                Text(
                                                    text = "CORE UTILIZATION",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 1.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                                    modifier = Modifier.padding(bottom = 12.dp)
                                                )

                                                LazyVerticalGrid(
                                                    columns = GridCells.Fixed(4),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .heightIn(max = 240.dp),
                                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    items(coreUtilizations.size) { index ->
                                                        val utilization = coreUtilizations[index] ?: 0f
                                                        val (currentFreq, _) = allCoreFrequencies[index] ?: Pair(0L, 0L)

                                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                            Box(contentAlignment = Alignment.Center) {
                                                                CircularProgressIndicator(
                                                                    progress = { utilization / 100f },
                                                                    modifier = Modifier.size(48.dp),
                                                                    strokeWidth = 4.dp,
                                                                    strokeCap = StrokeCap.Round,
                                                                    color = MaterialTheme.colorScheme.primary,
                                                                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                                )
                                                                Text(
                                                                    text = "$index",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    fontWeight = FontWeight.ExtraBold,
                                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                                                )
                                                            }
                                                            Spacer(modifier = Modifier.height(6.dp))
                                                            Text(
                                                                text = "${currentFreq / 1000} GHz", // Changed to GHz for cleaner look if space is tight? No, stick to MHz or make it compact.
                                                                fontSize = 10.sp, // Or keep original formatting
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                maxLines = 1
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                // =========================================================

                                // =========================================================
                                // PERFORMANCE OPTIMIZATIONS CARD
                                // =========================================================
                                // PERFORMANCE OPTIMIZATIONS CARD
                                // =========================================================
                                // Access the MainActivity to get the sustained performance mode
                                // status
                                val context = LocalContext.current
                                val activity = context as? com.ivarna.finalbenchmark2.MainActivity
                                val sustainedPerformanceStatus =
                                        if (activity != null) {
                                                activity.isSustainedPerformanceModeActive()
                                        } else {
                                                false
                                        }

                                val wakeLockStatus =
                                        if (activity != null) {
                                                activity.isWakeLockActive()
                                        } else {
                                                false
                                        }

                                // Determine wake lock status text for display
                                val wakeLockStatusText =
                                        if (activity != null) {
                                                if (activity.isWakeLockActive()) {
                                                        "Active" // When benchmark is running
                                                } else if (activity.isWakeLockReady()) {
                                                        "Ready" // When initialized but not yet
                                                        // acquired
                                                } else {
                                                        "Disabled" // When not available
                                                }
                                        } else {
                                                "Unknown"
                                        }

                                val screenAlwaysOnStatus =
                                        if (activity != null) {
                                                activity.isScreenAlwaysOnActive()
                                        } else {
                                                false
                                        }

                                // NEW: Get CPU optimization statuses
                                val highPriorityThreadingStatus =
                                        if (activity != null) {
                                                activity.isHighPriorityThreadingActive()
                                        } else {
                                                false
                                        }

                                val performanceHintStatus =
                                        if (activity != null) {
                                                activity.isPerformanceHintActive()
                                        } else {
                                                false
                                        }

                                val cpuAffinityStatus =
                                        if (activity != null) {
                                                activity.isCpuAffinityActive()
                                        } else {
                                                false
                                        }

                                val bigCoreCount =
                                        if (activity != null) {
                                                activity.getBigCoreCount()
                                        } else {
                                                0
                                        }

                                val midCoreCount =
                                        if (activity != null) {
                                                activity.getMidCoreCount()
                                        } else {
                                                0
                                        }

                                val littleCoreCount =
                                        if (activity != null) {
                                                activity.getLittleCoreCount()
                                        } else {
                                                0
                                        }

                                // NEW: Get foreground service and governor hint statuses
                                val foregroundServiceStatus =
                                        if (activity != null) {
                                                activity.isForegroundServiceActive()
                                        } else {
                                                false
                                        }

                                val governorHintStatus =
                                        if (activity != null) {
                                                activity.isGovernorHintApplied()
                                        } else {
                                                false
                                        }

                                val originalGovernor =
                                        if (activity != null) {
                                                activity.getOriginalGovernor()
                                        } else {
                                                "Unknown"
                                        }

                                PerformanceOptimizationsCard(
                                        sustainedPerformanceStatus = sustainedPerformanceStatus,
                                        wakeLockStatus = wakeLockStatus,
                                        screenAlwaysOnStatus = screenAlwaysOnStatus,
                                        wakeLockStatusText = wakeLockStatusText,
                                        highPriorityThreadingStatus = highPriorityThreadingStatus,
                                        performanceHintStatus = performanceHintStatus,
                                        cpuAffinityStatus = cpuAffinityStatus,
                                        bigCoreCount = bigCoreCount,
                                        midCoreCount = midCoreCount,
                                        littleCoreCount = littleCoreCount,
                                        foregroundServiceStatus = foregroundServiceStatus,
                                        governorHintStatus = governorHintStatus,
                                        originalGovernor = originalGovernor
                                )


                                // Benchmark Tips Card
                                BenchmarkTipsCard()

                                Spacer(modifier = Modifier.height(16.dp))

                                // AI Resources Card
                                // var showDownloadDialog removed (hoisted)

                                com.ivarna.finalbenchmark2.ui.components.AnimatedGlassCard(
                                    modifier = Modifier.fillMaxWidth().clickable { showDownloadDialog = true },
                                    shape = RoundedCornerShape(24.dp),
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                                    borderColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f),
                                    delayMillis = 400
                                ) {
                                    Row(
                                        modifier = Modifier.padding(20.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.ArrowDropDown,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(
                                                text = "AI Models Missing?",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Tap here to download required AI models.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                if (showDownloadDialog) {
                                    AiDownloadDialog(
                                        onDismiss = { showDownloadDialog = false }
                                    )
                                }



                                
                                Spacer(modifier = Modifier.height(120.dp)) // Bottom padding for floating nav bar
                        }


                        // Floating Settings Icon in Top Right Corner
                        Box(
                                modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(16.dp)
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                        .border(
                                                BorderStroke(
                                                        1.dp,
                                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                                                ),
                                                CircleShape
                                        )
                                        .clickable(onClick = onNavigateToSettings),
                                contentAlignment = Alignment.Center
                        ) {
                                Icon(
                                        imageVector = Icons.Rounded.Settings,
                                        contentDescription = "Settings",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                        }
                }
        }
}

// --- Helper Composables for Clean UI ---

@Composable
fun CompactStatItem(icon: ImageVector, value: String, tint: Color) {
        Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                        text = value,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                )
        }
}

@Composable
fun PerformanceOptimizationsCard(
        sustainedPerformanceStatus: Boolean,
        wakeLockStatus: Boolean,
        screenAlwaysOnStatus: Boolean,
        wakeLockStatusText: String =
                if (wakeLockStatus) "Active" else "Ready", // Changed to "Ready" when not active
        highPriorityThreadingStatus: Boolean,
        performanceHintStatus: Boolean,
        cpuAffinityStatus: Boolean,
        bigCoreCount: Int,
        midCoreCount: Int,
        littleCoreCount: Int,
        foregroundServiceStatus: Boolean = false,
        governorHintStatus: Boolean = false,
        originalGovernor: String? = "Unknown"
) {
        var isExpanded by remember { mutableStateOf(false) }

        com.ivarna.finalbenchmark2.ui.components.AnimatedGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                delayMillis = 400,
                onClick = { isExpanded = !isExpanded }
        ) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                        // Header Row
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Text(
                                        text = "PERFORMANCE OPTIMIZATIONS",
                                        fontSize = 13.sp, // Reduced from titleMedium (approx 16sp)
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp,
                                        color = MaterialTheme.colorScheme.primary
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Count how many optimizations are active
                                        val activeCount =
                                                listOf(
                                                                sustainedPerformanceStatus,
                                                                wakeLockStatus,
                                                                screenAlwaysOnStatus,
                                                                highPriorityThreadingStatus,
                                                                performanceHintStatus,
                                                                cpuAffinityStatus,
                                                                foregroundServiceStatus,
                                                                governorHintStatus
                                                        )
                                                        .count { it }
                                        val totalOptimizations = 8

                                        // Badge for count
                                        Surface(
                                            color = if (activeCount > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(
                                                text = "$activeCount/$totalOptimizations",
                                                color = if (activeCount > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Icon(
                                                imageVector = Icons.Rounded.ArrowDropDown,
                                                contentDescription = "Expand",
                                                modifier =
                                                        Modifier.size(28.dp)
                                                                .rotate(
                                                                        if (isExpanded) 180f else 0f
                                                                ),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                }
                        }

                        // Expanded Details
                        AnimatedVisibility(
                                visible = isExpanded,
                                enter = expandVertically(animationSpec = tween(300)) + fadeIn(),
                                exit = shrinkVertically(animationSpec = tween(300)) + fadeOut()
                        ) {
                                Column(modifier = Modifier.padding(top = 20.dp)) {
                                        HorizontalDivider(
                                                color =
                                                        MaterialTheme.colorScheme.outlineVariant
                                                                .copy(alpha = 0.3f),
                                                modifier = Modifier.padding(bottom = 16.dp)
                                        )

                                        // Sustained Performance Mode Detail
                                        OptimizationDetailRow(
                                                title = "Sustained Performance Mode",
                                                description =
                                                        "Prevents thermal throttling during benchmarks",
                                                status =
                                                        if (sustainedPerformanceStatus) {
                                                                PerformanceOptimizationStatus
                                                                        .ENABLED
                                                        } else {
                                                                PerformanceOptimizationStatus
                                                                        .DISABLED
                                                        }
                                        )

                                        // Wake Lock Management Detail
                                        OptimizationDetailRow(
                                                title = "Wake Lock Management",
                                                description = "Keeps CPU running at full speed",
                                                status =
                                                        if (wakeLockStatus) {
                                                                PerformanceOptimizationStatus
                                                                        .ENABLED
                                                        } else if (wakeLockStatusText == "Ready") {
                                                                PerformanceOptimizationStatus
                                                                        .READY // Show READY when
                                                                // initialized but
                                                                // not
                                                                // acquired
                                                        } else {
                                                                PerformanceOptimizationStatus
                                                                        .DISABLED
                                                        },
                                                statusText =
                                                        wakeLockStatusText // Pass the custom status
                                                // text
                                                )

                                        // Screen Always On Detail
                                        OptimizationDetailRow(
                                                title = "Screen Always On",
                                                description =
                                                        "Prevents performance degradation from screen-off CPU throttling",
                                                status =
                                                        if (screenAlwaysOnStatus) {
                                                                PerformanceOptimizationStatus
                                                                        .ENABLED
                                                        } else {
                                                                PerformanceOptimizationStatus
                                                                        .DISABLED
                                                        }
                                        )

                                        // NEW: High Priority Threading Detail
                                        OptimizationDetailRow(
                                                title = "High Priority Threading",
                                                description =
                                                        "Maximum CPU time allocation for benchmark threads",
                                                status =
                                                        if (highPriorityThreadingStatus) {
                                                                PerformanceOptimizationStatus
                                                                        .ENABLED
                                                        } else {
                                                                PerformanceOptimizationStatus
                                                                        .DISABLED
                                                        }
                                        )

                                        // NEW: Performance Hint API Detail
                                        OptimizationDetailRow(
                                                title = "Performance Hint API",
                                                description =
                                                        "Guides scheduler for optimal core selection (Android 12+)",
                                                status =
                                                        if (performanceHintStatus) {
                                                                PerformanceOptimizationStatus
                                                                        .ENABLED
                                                        } else {
                                                                PerformanceOptimizationStatus
                                                                        .NOT_SUPPORTED // For older
                                                                // Android
                                                                // versions
                                                        }
                                        )

                                        // NEW: CPU Affinity Control Detail
                                        OptimizationDetailRow(
                                                title = "CPU Affinity Control",
                                                description = when {
                                                        midCoreCount > 0 -> "$bigCoreCount BIG, $midCoreCount Mid, $littleCoreCount LITTLE cores detected"
                                                        else -> "$bigCoreCount BIG, $littleCoreCount LITTLE cores detected"
                                                },
                                                status =
                                                        if (cpuAffinityStatus) {
                                                                PerformanceOptimizationStatus
                                                                        .ENABLED
                                                        } else {
                                                                PerformanceOptimizationStatus
                                                                        .DISABLED
                                                        }
                                        )

                                        // NEW: Foreground Service Detail
                                        OptimizationDetailRow(
                                                title = "Foreground Service",
                                                description =
                                                        "Maintains maximum priority during benchmark",
                                                status =
                                                        if (foregroundServiceStatus) {
                                                                PerformanceOptimizationStatus
                                                                        .ENABLED
                                                        } else {
                                                                PerformanceOptimizationStatus
                                                                        .DISABLED
                                                        }
                                        )

                                        // NEW: CPU Governor Hints Detail
                                        OptimizationDetailRow(
                                                title = "CPU Governor Hints",
                                                description =
                                                        "Current: ${originalGovernor ?: "Unknown"} (requires root to change)",
                                                status =
                                                        if (governorHintStatus) {
                                                                PerformanceOptimizationStatus
                                                                        .ENABLED
                                                        } else {
                                                                PerformanceOptimizationStatus
                                                                        .DISABLED
                                                        }
                                        )
                                }
                        }
                }
        }
}

@Composable
fun OptimizationDetailRow(
        title: String,
        description: String,
        status: PerformanceOptimizationStatus, // This should be defined in MainViewModel
        statusText: String? = null // New optional parameter for custom status text
) {
        val statusColor =
                when (status) {
                        PerformanceOptimizationStatus.ENABLED -> MaterialTheme.colorScheme.primary
                        PerformanceOptimizationStatus.DISABLED -> MaterialTheme.colorScheme.error
                        PerformanceOptimizationStatus.NOT_SUPPORTED ->
                                MaterialTheme.colorScheme.outline
                        PerformanceOptimizationStatus.READY ->
                                MaterialTheme.colorScheme.secondary // Added READY status
                }

        val displayStatusText =
                statusText
                        ?: when (status) {
                                PerformanceOptimizationStatus.ENABLED -> "Enabled"
                                PerformanceOptimizationStatus.DISABLED -> "Disabled"
                                PerformanceOptimizationStatus.NOT_SUPPORTED -> "Not Supported"
                                PerformanceOptimizationStatus.READY -> "Ready" // Added READY status
                        }

        val statusIcon =
                when (status) {
                        PerformanceOptimizationStatus.ENABLED -> Icons.Rounded.Check
                        PerformanceOptimizationStatus.DISABLED -> Icons.Rounded.Close
                        PerformanceOptimizationStatus.NOT_SUPPORTED ->
                                Icons.Rounded.DisabledByDefault
                        PerformanceOptimizationStatus.READY ->
                                Icons.Rounded.CheckCircle // Using check circle for ready state
                }

        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        Column(modifier = Modifier.weight(1f)) {
                                Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                        text = description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                        text = displayStatusText,
                                        color = statusColor,
                                        fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                        imageVector = statusIcon,
                                        contentDescription = null,
                                        tint = statusColor
                                )
                        }
                }
        }
}

@Composable
fun DetailIconPair(icon: ImageVector, value: String, label: String) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(22.dp)
                )
                Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
        }
}

@Composable
fun BenchmarkTipsCard() {
        com.ivarna.finalbenchmark2.ui.components.AnimatedGlassCard(
                modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.15f),
                borderColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                delayMillis = 500
        ) {
                Row(
                        modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        Icon(
                                imageVector = Icons.Rounded.Lightbulb,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                                text = "Tip: Keep device cool & close background apps for best results.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                                maxLines = 2, // Allow wrapping
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                }
        }
}

@Composable
fun TipRow(number: String, title: String, description: String) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.Top
        ) {
                // Tip Number Badge
                Box(
                        modifier =
                                Modifier.size(32.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                ) {
                        Text(
                                text = number,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Tip Content
                Column(modifier = Modifier.weight(1f)) {
                        Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                                text = description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                }
        }
}

// Track if ROM warning animation has been shown (persists across navigations)
private var romWarningAnimationShown = false

// ROM Compatibility Warning Card (Alert Style)
@Composable
fun RomCompatibilityWarningCard() {
        var isVisible by remember { mutableStateOf(romWarningAnimationShown) }

        LaunchedEffect(Unit) {
                if (!romWarningAnimationShown) {
                        delay(300)
                        isVisible = true
                        romWarningAnimationShown = true
                } else {
                        isVisible = true
                }
        }

        AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(600)) + expandVertically(),
                exit = fadeOut() + shrinkVertically()
        ) {
                // Redesigned as a "Warning Strip" rather than a full heavy card
                Card(
                        modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f) // Light red tint
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f)),
                        elevation = CardDefaults.cardElevation(0.dp)
                ) {
                        Row(
                                modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                        ) {
                                // Warning Icon
                                Icon(
                                        imageVector = Icons.Rounded.Warning,
                                        contentDescription = "Warning",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(24.dp)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                // Warning Text
                                Column(
                                        modifier = Modifier.weight(1f)
                                ) {
                                        Text(
                                                text = "ROM Compatibility Note",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                                text = "Known issues with OxygenOS, RealmeUI, ColorOS. Best performance on AOSP/HyperOS.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                                lineHeight = 14.sp
                                        )
                                }
                        }
                }
        }
}

// High Score Card
@Composable
fun HighScoreCard(
        score: Double,
        deviceModel: String,
        timestamp: Long,
        historyRepository: HistoryRepository?
) {
        var beatsPercentage by remember { mutableStateOf(0) }
        val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
        val formattedDate = remember(timestamp) { dateFormat.format(Date(timestamp)) }

        // Calculate percentage beaten using same logic as ResultScreen
        LaunchedEffect(score, historyRepository) {
                if (historyRepository != null && score > 0) {
                        // Get hardcoded reference devices from RankingViewModel
                        val hardcodedReferenceDevices = listOf(
                                RankingItem(
                                        name = "Snapdragon 8 Gen 3",
                                        normalizedScore = 313,
                                        singleCore = 100,
                                        multiCore = 420
                                ),
                                RankingItem(
                                        name = "MediaTek Dimensity 8300",
                                        normalizedScore = 229,
                                        singleCore = 78,
                                        multiCore = 308
                                ),
                                RankingItem(
                                        name = "Snapdragon 8s Gen 3",
                                        normalizedScore = 241,
                                        singleCore = 87,
                                        multiCore = 324
                                ),
                                RankingItem(
                                        name = "MediaTek Dimensity 6300",
                                        normalizedScore = 107,
                                        singleCore = 50,
                                        multiCore = 137
                                )
                        )

                        val userScore = RankingItem(
                                name = "Your Device",
                                normalizedScore = score.toInt(),
                                singleCore = 0,
                                multiCore = 0,
                                isCurrentUser = true
                        )

                        val allDevices = hardcodedReferenceDevices + userScore
                        val rankedItems = allDevices.sortedByDescending { it.normalizedScore }
                                .mapIndexed { index, item -> item.copy(rank = index + 1) }

                        val userRank = rankedItems.indexOfFirst { it.isCurrentUser }
                        val totalDevices = rankedItems.size
                        val devicesBeaten = totalDevices - userRank - 1
                        beatsPercentage = if (totalDevices > 1) {
                                (devicesBeaten.toFloat() / (totalDevices - 1) * 100).toInt()
                        } else {
                                100
                        }
                }
        }

        Card(
                modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                shape = RoundedCornerShape(24.dp), // Slightly more rounded for premium feel
                colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f) // Subtle glass-like feel
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // Flat for cleaner look
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
        ) {
                Box(
                        modifier = Modifier
                                .fillMaxWidth() // Fill width only
                                .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                                                Color.Transparent
                                            ),
                                            center = Offset(0f, 0f),
                                            radius = 500f
                                        )
                                )
                                .background(
                                        brush = Brush.verticalGradient(
                                                colors = listOf(
                                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.05f)
                                                )
                                        )
                                )
                ) {
                        Column(
                                modifier = Modifier
                                        .padding(24.dp)
                                        .fillMaxWidth(),
                                verticalArrangement = Arrangement.SpaceBetween
                        ) {
                                // Header: Label and Date
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        Surface(
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                shape = RoundedCornerShape(50),
                                                modifier = Modifier.height(24.dp)
                                        ) {
                                                Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(horizontal = 10.dp)
                                                ) {
                                                        Text(
                                                                text = "PERSONAL BEST",
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                letterSpacing = 1.sp,
                                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                                        )
                                                }
                                        }

                                        Text(
                                                text = formattedDate,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                                fontWeight = FontWeight.Medium
                                        )
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                // Main Content: Logo + Score
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        // App Logo Bubble
                                        Box(
                                                modifier = Modifier.size(64.dp),
                                                contentAlignment = Alignment.Center
                                        ) {
                                                Box(
                                                        modifier = Modifier
                                                                .size(64.dp)
                                                                .clip(CircleShape)
                                                                .background(
                                                                        brush = Brush.linearGradient(
                                                                                colors = listOf(
                                                                                        Color(0xFF2A2A2A),
                                                                                        Color(0xFF1A1A1A)
                                                                                )
                                                                        )
                                                                )
                                                )
                                                Image(
                                                        painter = painterResource(id = R.drawable.logo_2),
                                                        contentDescription = "Logo",
                                                        modifier = Modifier.size(48.dp)
                                                )
                                        }

                                        Spacer(modifier = Modifier.width(20.dp))

                                        // Score Info
                                        Column(
                                                horizontalAlignment = Alignment.Start
                                        ) {
                                                Text(
                                                        text = "${score.toInt()}",
                                                        fontSize = 48.sp,
                                                        fontWeight = FontWeight.ExtraBold, // More impactful
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        lineHeight = 48.sp,
                                                        letterSpacing = (-1).sp
                                                )
                                                Text(
                                                        text = "POINTS",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        letterSpacing = 1.sp
                                                )
                                        }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                // Footer: Device + Performance Stat
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        // Device Model
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                        imageVector = Icons.Rounded.Settings, // Using generic icon as placeholder for device
                                                        contentDescription = null,
                                                        modifier = Modifier.size(14.dp),
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                        text = deviceModel.ifEmpty { "Unknown Device" },
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontWeight = FontWeight.Medium,
                                                        maxLines = 1
                                                )
                                        }

                                        // Performance Badge
                                        Text(
                                                text = "Top $beatsPercentage% Performance",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                        )
                                }
                        }
                }
        }
}

@Composable
fun SmallStatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    onClick: () -> Unit
) {
    // Icon Pop Animation
    val iconScale = remember { androidx.compose.animation.core.Animatable(0.5f) }
    LaunchedEffect(Unit) {
        delay(100) // Slight delay for pop effect
        iconScale.animateTo(
            targetValue = 1f,
            animationSpec = androidx.compose.animation.core.spring(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
            )
        )
    }

    // Use Box/Column directly instead of nested Card to avoid double-backgrounds
    // The visual container is provided by the parent AnimatedGlassCard
    Column(
        modifier = modifier
            .height(110.dp) // Fixed height for square-ish aspect ratio
            .clickable { onClick() }
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.05f),
                        color.copy(alpha = 0.05f) // Very subtle tint
                    )
                ),
                shape = RoundedCornerShape(24.dp) // Match parent shape
            )
            .padding(10.dp), // Reduced padding to prevent cutoff
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icon Container
        Box(
            modifier = Modifier
                .size(40.dp) // Slightly larger touch/visual target
                .scale(iconScale.value)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)) // Glassy icon bg
                .border(1.dp, color.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp)) // Reduced spacer

        // Animated Value Update
        AnimatedContent(
            targetState = value,
            transitionSpec = {
                (fadeIn(animationSpec = tween(300)) + 
                 scaleIn(initialScale = 0.8f, animationSpec = tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing)))
                    .togetherWith(fadeOut(animationSpec = tween(200))) 
            },
            label = "stat_value_update"
        ) { targetValue ->
            Text(
                text = targetValue,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false
            )
        }

        Spacer(modifier = Modifier.height(2.dp)) // Reduced spacer

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            letterSpacing = 1.2.sp
        )
    }
}
@Composable
fun AiDownloadDialog(onDismiss: () -> Unit) {
    var downloadProgress by remember { mutableStateOf(0f) }
    var downloadStatus by remember { mutableStateOf("Ready to download") }
    var isDownloading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!isDownloading) onDismiss() },
        title = { Text("Download AI Models") },
        text = {
            Column {
                Text("To run AI Benchmarks offline, we need to download the following models (~350MB total):\n\n• MobileNet V3 (Image Classification)\n• EfficientDet (Object Detection)\n• MiniLM (Text Embedding)\n• Whisper (Speech-to-Text)\n• Gemma 3 (LLM Inference)")
                Spacer(modifier = Modifier.height(16.dp))
                
                if (isDownloading) {
                    LinearProgressIndicator(
                        progress = { downloadProgress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = downloadStatus,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            if (!isDownloading) {
                Button(
                    onClick = {
                        isDownloading = true
                        scope.launch {
                            try {
                                // 1. MobileNet (Approx 5%)
                                downloadStatus = "Downloading MobileNet V3..."
                                com.ivarna.finalbenchmark2.aiBenchmark.ModelDownloader.downloadModel(
                                    context,
                                    com.ivarna.finalbenchmark2.aiBenchmark.ModelRepository.MOBILENET_V3_URL,
                                    com.ivarna.finalbenchmark2.aiBenchmark.ModelRepository.MOBILENET_FILENAME
                                ) { p -> downloadProgress = p * 0.05f }
                                
                                val models = com.ivarna.finalbenchmark2.aiBenchmark.ModelRepository.models
                                val totalModels = models.size

                                models.forEachIndexed { index, model ->
                                    downloadStatus = "Downloading ${index + 1}/$totalModels: ${model.title} (${model.sizeMb})..."
                                    
                                    // Calculate progress range for this file
                                    val startProgress = index.toFloat() / totalModels
                                    val endProgress = (index + 1).toFloat() / totalModels
                                    
                                    com.ivarna.finalbenchmark2.aiBenchmark.ModelDownloader.downloadModel(
                                        context,
                                        model.url,
                                        model.filename
                                    ) { fileProgress ->
                                        // Map file progress (0..1) to total progress (start..end)
                                        downloadProgress = startProgress + (fileProgress * (endProgress - startProgress))
                                    }
                                }

                                downloadStatus = "All Downloads Complete!"
                                downloadProgress = 1.0f
                                delay(1000)
                                onDismiss()
                            } catch (e: Exception) {
                                downloadStatus = "Error: ${e.message}"
                                isDownloading = false
                            }
                        }
                    }
                ) {
                    Text("Download All")
                }
            }
        },
        dismissButton = {
            if (!isDownloading) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
