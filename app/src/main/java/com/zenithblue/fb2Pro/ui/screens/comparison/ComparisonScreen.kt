package com.zenithblue.fb2Pro.ui.screens.comparison

import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.zenithblue.fb2Pro.data.repository.HistoryRepository
import com.zenithblue.fb2Pro.ui.viewmodels.BenchmarkDetails
import com.zenithblue.fb2Pro.ui.viewmodels.RankingItem
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaseComparisonScreen(
    category: String,
    selectedDeviceJson: String,
    historyRepository: HistoryRepository,
    onBackClick: () -> Unit,
    benchmarkContent: @Composable (userDevice: RankingItem?, selectedDevice: RankingItem?) -> Unit = { _, _ -> }
) {
    val scope = rememberCoroutineScope()

    val selectedDevice = remember(selectedDeviceJson) {
        try { Gson().fromJson(selectedDeviceJson, RankingItem::class.java) }
        catch (e: Exception) { null }
    }

    var userDevice by remember { mutableStateOf<RankingItem?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(category) {
        scope.launch {
            try {
                val results = historyRepository.getAllResults().firstOrNull() ?: emptyList()
                val highestScore = results
                    .filter {
                        if (category == "FULL") it.benchmarkResult.type.equals("FULL", ignoreCase = true)
                        else it.benchmarkResult.type.equals(category, ignoreCase = true)
                    }
                    .maxByOrNull { it.benchmarkResult.normalizedScore }

                if (highestScore != null) {
                    val details = if (category == "CPU") parseCpuDetails(highestScore) else null
                    userDevice = RankingItem(
                        name = "Your Device (${Build.MODEL})",
                        normalizedScore = highestScore.benchmarkResult.normalizedScore.toInt(),
                        singleCore = highestScore.benchmarkResult.singleCoreScore.toInt(),
                        multiCore = highestScore.benchmarkResult.multiCoreScore.toInt(),
                        isCurrentUser = true,
                        benchmarkDetails = details
                    )
                }
            } catch (e: Exception) { }
            finally { isLoading = false }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceContainerLowest
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "$category Comparison",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (selectedDevice == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Unable to load device data", color = MaterialTheme.colorScheme.error)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    item {
                        ComparisonHeader(category = category, userDevice = userDevice, selectedDevice = selectedDevice)
                    }
                    item {
                        MainScoreComparison(category = category, userDevice = userDevice, selectedDevice = selectedDevice)
                    }
                    item {
                        benchmarkContent(userDevice, selectedDevice)
                    }
                }
            }
        }
    }
}

@Composable
private fun ComparisonHeader(
    category: String,
    userDevice: RankingItem?,
    selectedDevice: RankingItem,
    delayMillis: Int = 0
) {
    com.zenithblue.fb2Pro.ui.components.AnimatedGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
        delayMillis = delayMillis
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            val scoreDiff = (userDevice?.normalizedScore ?: 0) - selectedDevice.normalizedScore
            val percentDiff = if (selectedDevice.normalizedScore > 0) {
                (scoreDiff.toFloat() / selectedDevice.normalizedScore * 100).toInt()
            } else 0

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isAhead = scoreDiff > 0
                val diffColor = if (isAhead) Color(0xFF4CAF50) else Color(0xFFE53935)
                Surface(
                    shape = RoundedCornerShape(50),
                    color = diffColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, diffColor.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isAhead) Icons.AutoMirrored.Rounded.TrendingUp else Icons.AutoMirrored.Rounded.TrendingDown,
                            contentDescription = null, tint = diffColor, modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (percentDiff >= 0) "Better by $percentDiff%" else "Slower by ${-percentDiff}%",
                            fontSize = 14.sp, fontWeight = FontWeight.Bold, color = diffColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                DeviceColumn(
                    device = userDevice, fallbackName = "Your Device",
                    icon = Icons.Rounded.PhoneAndroid, color = MaterialTheme.colorScheme.primary, isUser = true
                )
                Column(modifier = Modifier.padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "VS", fontSize = 12.sp, fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
                DeviceColumn(
                    device = selectedDevice, fallbackName = "Unknown",
                    icon = Icons.Rounded.Memory, color = MaterialTheme.colorScheme.secondary, isUser = false
                )
            }
        }
    }
}

@Composable
private fun RowScope.DeviceColumn(
    device: RankingItem?, fallbackName: String, icon: ImageVector, color: Color, isUser: Boolean
) {
    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(48.dp).background(color.copy(alpha = 0.15f), CircleShape)
                .border(2.dp, color.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) { Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp)) }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isUser) "Your Device" else device?.name?.replace("Your Device ", "")?.trim('(', ')') ?: fallbackName,
            fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis
        )
        if (isUser) {
            Text(text = "(${Build.MODEL})", fontSize = 10.sp, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = "${device?.normalizedScore ?: 0}", fontSize = 24.sp, fontWeight = FontWeight.Black,
            color = color, letterSpacing = (-1).sp)
        Text(text = "POINTS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
            color = color.copy(alpha = 0.7f), fontSize = 8.sp)
    }
}

@Composable
fun MainScoreComparison(
    category: String,
    userDevice: RankingItem?,
    selectedDevice: RankingItem,
    startDelay: Int = 100
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (category == "CPU") {
            ScoreComparisonCard(title = "Single-Core", userScore = userDevice?.singleCore ?: 0,
                selectedScore = selectedDevice.singleCore,
                userColor = MaterialTheme.colorScheme.primary,
                selectedColor = MaterialTheme.colorScheme.secondary, delayMillis = startDelay)
            ScoreComparisonCard(title = "Multi-Core", userScore = userDevice?.multiCore ?: 0,
                selectedScore = selectedDevice.multiCore,
                userColor = MaterialTheme.colorScheme.primary,
                selectedColor = MaterialTheme.colorScheme.secondary, delayMillis = startDelay + 100)
        }
        ScoreComparisonCard(title = "Final Score", userScore = userDevice?.normalizedScore ?: 0,
            selectedScore = selectedDevice.normalizedScore,
            userColor = MaterialTheme.colorScheme.primary,
            selectedColor = MaterialTheme.colorScheme.secondary, delayMillis = startDelay + 200)
    }
}

@Composable
private fun ScoreComparisonCard(
    title: String, userScore: Int, selectedScore: Int,
    userColor: Color, selectedColor: Color, delayMillis: Int = 0
) {
    val maxScore = maxOf(userScore, selectedScore, 1)
    val userProgress by animateFloatAsState(
        targetValue = userScore.toFloat() / maxScore,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing), label = "userProgress")
    val selectedProgress by animateFloatAsState(
        targetValue = selectedScore.toFloat() / maxScore,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing), label = "selectedProgress")
    val scoreDiff = userScore - selectedScore
    val percentDiff = if (selectedScore > 0) (scoreDiff.toFloat() / selectedScore * 100).toInt()
        else if (userScore > 0) 100 else 0

    com.zenithblue.fb2Pro.ui.components.AnimatedGlassCard(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), delayMillis = delayMillis
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = title, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                if (percentDiff != 0) {
                    val diffColor = if (percentDiff > 0) Color(0xFF4CAF50) else Color(0xFFE53935)
                    Surface(shape = RoundedCornerShape(4.dp), color = diffColor.copy(alpha = 0.15f)) {
                        Text(text = if (percentDiff > 0) "+$percentDiff%" else "$percentDiff%",
                            fontSize = 11.sp, fontWeight = FontWeight.Bold, color = diffColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }
            LabeledScoreBar(label = "You", score = userScore, progress = userProgress, color = userColor)
            Spacer(modifier = Modifier.height(8.dp))
            LabeledScoreBar(label = "Ref", score = selectedScore, progress = selectedProgress, color = selectedColor)
        }
    }
}

@Composable
private fun LabeledScoreBar(label: String, score: Int, progress: Float, color: Color) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f), modifier = Modifier.width(36.dp))
        Box(modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
            Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(progress)
                .background(Brush.horizontalGradient(colors = listOf(color.copy(alpha = 0.8f), color))))
        }
        Column(modifier = Modifier.width(60.dp), horizontalAlignment = Alignment.End) {
            Text(text = formatScore(score), fontSize = 14.sp, fontWeight = FontWeight.Black,
                color = color, textAlign = TextAlign.End)
        }
    }
}

@Composable
fun BenchmarkComparisonCard(
    name: String, icon: ImageVector,
    userScore: Double, selectedScore: Double,
    userColor: Color, selectedColor: Color,
    delayMillis: Int = 0, animationDuration: Int = 500
) {
    val userWins = userScore > selectedScore
    val maxScore = maxOf(userScore, selectedScore, 1.0)
    val userProgress by animateFloatAsState(
        targetValue = (userScore / maxScore).toFloat(),
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing), label = "uProg")
    val selectedProgress by animateFloatAsState(
        targetValue = (selectedScore / maxScore).toFloat(),
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing), label = "sProg")

    com.zenithblue.fb2Pro.ui.components.AnimatedGlassCard(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
        delayMillis = delayMillis, animationDuration = animationDuration
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(32.dp).background(
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = name, style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                    val scoreDiff = userScore - selectedScore
                    val percentDiff = if (selectedScore > 0) (scoreDiff / selectedScore * 100).toInt() else 0
                    val diffText = if (userWins) "Faster by $percentDiff%" else "Slower by ${-percentDiff}%"
                    val diffColor = if (userWins) Color(0xFF4CAF50) else Color(0xFFE53935)
                    Text(text = diffText, fontSize = 11.sp, color = diffColor, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            BarWithLabel(label = "You", progress = userProgress, score = userScore, color = userColor)
            Spacer(modifier = Modifier.height(6.dp))
            BarWithLabel(label = "Ref", progress = selectedProgress, score = selectedScore, color = selectedColor)
        }
    }
}

@Composable
private fun BarWithLabel(label: String, progress: Float, score: Double, color: Color) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.width(36.dp), fontWeight = FontWeight.Bold)
        Box(modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
            Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(progress)
                .background(Brush.horizontalGradient(colors = listOf(color.copy(alpha = 0.7f), color))))
        }
        Text(text = String.format("%.0f", score), fontSize = 11.sp, fontWeight = FontWeight.Bold,
            color = color, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
    }
}

private fun parseCpuDetails(highestScore: com.zenithblue.fb2Pro.data.database.entities.BenchmarkWithCpuData): BenchmarkDetails? {
    return try {
        val gson = Gson()
        val results = gson.fromJson(
            highestScore.benchmarkResult.detailedResultsJson,
            Array<com.zenithblue.fb2Pro.cpuBenchmark.BenchmarkResult>::class.java
        ).toList()
        fun findMops(prefix: String, testName: String): Double {
            val ops = results.firstOrNull { it.name == "$prefix $testName" }?.opsPerSecond ?: 0.0
            return ops / 1_000_000.0
        }
        BenchmarkDetails(
            singleCorePrimeNumberMops = findMops("Single-Core", "Prime Generation"),
            singleCoreFibonacciMops = findMops("Single-Core", "Fibonacci Iterative"),
            singleCoreMatrixMultiplicationMops = findMops("Single-Core", "Matrix Multiplication"),
            singleCoreHashComputingMops = findMops("Single-Core", "Hash Computing"),
            singleCoreStringSortingMops = findMops("Single-Core", "String Sorting"),
            singleCoreRayTracingMops = findMops("Single-Core", "Ray Tracing"),
            singleCoreCompressionMops = findMops("Single-Core", "Compression"),
            singleCoreMonteCarloMops = findMops("Single-Core", "Monte Carlo π"),
            singleCoreJsonParsingMops = findMops("Single-Core", "JSON Parsing"),
            singleCoreNQueensMops = findMops("Single-Core", "N-Queens"),
            multiCorePrimeNumberMops = findMops("Multi-Core", "Prime Generation"),
            multiCoreFibonacciMops = findMops("Multi-Core", "Fibonacci Iterative"),
            multiCoreMatrixMultiplicationMops = findMops("Multi-Core", "Matrix Multiplication"),
            multiCoreHashComputingMops = findMops("Multi-Core", "Hash Computing"),
            multiCoreStringSortingMops = findMops("Multi-Core", "String Sorting"),
            multiCoreRayTracingMops = findMops("Multi-Core", "Ray Tracing"),
            multiCoreCompressionMops = findMops("Multi-Core", "Compression"),
            multiCoreMonteCarloMops = findMops("Multi-Core", "Monte Carlo π"),
            multiCoreJsonParsingMops = findMops("Multi-Core", "JSON Parsing"),
            multiCoreNQueensMops = findMops("Multi-Core", "N-Queens")
        )
    } catch (e: Exception) { null }
}

fun formatScore(score: Int): String = String.format("%,d", score)
