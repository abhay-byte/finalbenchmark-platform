package com.ivarna.finalbenchmark2.ui.screens

import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkName
import com.ivarna.finalbenchmark2.cpuBenchmark.KotlinBenchmarkManager
import com.ivarna.finalbenchmark2.data.repository.HistoryRepository
import com.ivarna.finalbenchmark2.ui.viewmodels.BenchmarkDetails
import com.ivarna.finalbenchmark2.ui.viewmodels.RankingItem
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CpuComparisonScreen(
    selectedDeviceJson: String,
    historyRepository: HistoryRepository,
    onBackClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    
    // Parse the selected device from JSON
    val selectedDevice = remember(selectedDeviceJson) {
        try {
            Gson().fromJson(selectedDeviceJson, RankingItem::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    // Load user's highest CPU score for comparison
    var userDevice by remember { mutableStateOf<RankingItem?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val results = historyRepository.getAllResults().firstOrNull() ?: emptyList()
                val highestCpuScore = results
                    .filter { it.benchmarkResult.type.contains("CPU", ignoreCase = true) }
                    .maxByOrNull { it.benchmarkResult.normalizedScore }
                
                if (highestCpuScore != null) {
                    // Parse detailed results JSON to extract separate single-core and multi-core Mops/s
                    val details = try {
                        val gson = Gson()
                        val detailedResultsJson = highestCpuScore.benchmarkResult.detailedResultsJson
                        val benchmarkResults = gson.fromJson(
                            detailedResultsJson,
                            Array<com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkResult>::class.java
                        ).toList()
                        
                        // Helper function to find Mops/s for a specific benchmark
                        // Note: Database stores opsPerSecond, need to convert to Mops/s
                        fun findMops(prefix: String, testName: String): Double {
                            val opsPerSecond = benchmarkResults
                                .firstOrNull { it.name == "$prefix $testName" }
                                ?.opsPerSecond ?: 0.0
                            return opsPerSecond / 1_000_000.0  // Convert ops/s to Mops/s
                        }
                        
                        BenchmarkDetails(
                            // Single-Core Mops/s values
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
                            // Multi-Core Mops/s values
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
                    } catch (e: Exception) {
                        null
                    }
                    
                    userDevice = RankingItem(
                        name = "Your Device (${Build.MODEL})",
                        normalizedScore = highestCpuScore.benchmarkResult.normalizedScore.toInt(),
                        singleCore = highestCpuScore.benchmarkResult.singleCoreScore.toInt(),
                        multiCore = highestCpuScore.benchmarkResult.multiCoreScore.toInt(),
                        isCurrentUser = true,
                        benchmarkDetails = details
                    )
                }
            } catch (e: Exception) {
                // Handle error silently
            } finally {
                isLoading = false
            }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Custom Glass Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Text(
                    text = "Comparison",
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
                    // Header with device names
                    item {
                        ComparisonHeader(
                            userDevice = userDevice,
                            selectedDevice = selectedDevice,
                            delayMillis = 0
                        )
                    }
                    
                    // Main score comparison cards
                    item {
                        MainScoreComparison(
                            userDevice = userDevice,
                            selectedDevice = selectedDevice,
                            startDelay = 100
                        )
                    }
                    
                    // Single-Core Benchmarks Section
                    item {
                        Text(
                            text = "Single-Core Benchmarks",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                    
                    val singleCoreBaseDelay = 200
                    val singleCoreBenchmarks = getSingleCoreBenchmarkItems(userDevice, selectedDevice)
                    itemsIndexed(singleCoreBenchmarks) { index, benchmark ->
                        val isInitiallyVisible = index < 4
                        val itemDelay = if (isInitiallyVisible) singleCoreBaseDelay + (index * 50) else 0
                        val itemDuration = if (isInitiallyVisible) 500 else 250
                        
                        BenchmarkComparisonCard(
                            benchmark = benchmark,
                            userDeviceName = userDevice?.name ?: "Your Device",
                            selectedDeviceName = selectedDevice.name,
                            delayMillis = itemDelay,
                            animationDuration = itemDuration
                        )
                    }
                    
                    // Multi-Core Benchmarks Section
                    item {
                        Text(
                            text = "Multi-Core Benchmarks",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                        )
                    }
                    
                    // Independent base delay, not waiting for entire single-core list
                    // Independent base delay, not waiting for entire single-core list
                    val multiCoreBaseDelay = 100 
                    val multiCoreBenchmarks = getMultiCoreBenchmarkItems(userDevice, selectedDevice)
                    itemsIndexed(multiCoreBenchmarks) { index, benchmark ->
                        val isInitiallyVisible = index < 4
                        val itemDelay = if (isInitiallyVisible) multiCoreBaseDelay + (index * 50) else 0
                        val itemDuration = if (isInitiallyVisible) 500 else 250

                        BenchmarkComparisonCard(
                            benchmark = benchmark,
                            userDeviceName = userDevice?.name ?: "Your Device",
                            selectedDeviceName = selectedDevice.name,
                            delayMillis = itemDelay,
                            animationDuration = itemDuration
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ComparisonHeader(
    userDevice: RankingItem?,
    selectedDevice: RankingItem,
    delayMillis: Int = 0
) {
    com.ivarna.finalbenchmark2.ui.components.AnimatedGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
        delayMillis = delayMillis
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp) // Reduced padding
        ) {
            // Percentage difference at top
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
                            contentDescription = null,
                            tint = diffColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (percentDiff >= 0) "Better by $percentDiff%" else "Slower by ${-percentDiff}%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = diffColor
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
                // User device
                DeviceColumn(
                    device = userDevice,
                    fallbackName = "Your Device",
                    icon = Icons.Rounded.PhoneAndroid,
                    color = MaterialTheme.colorScheme.primary,
                    isUser = true
                )
                
                // VS indicator
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "VS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                
                // Selected device
                DeviceColumn(
                    device = selectedDevice,
                    fallbackName = "Unknown",
                    icon = Icons.Rounded.Memory,
                    color = MaterialTheme.colorScheme.secondary,
                    isUser = false
                )
            }
        }
    }
}

@Composable
private fun RowScope.DeviceColumn(
    device: RankingItem?,
    fallbackName: String,
    icon: ImageVector,
    color: Color,
    isUser: Boolean
) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp) // Smaller icon box
                .background(
                    color.copy(alpha = 0.15f),
                    CircleShape
                )
                .border(2.dp, color.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = if (isUser) "Your Device" else device?.name?.replace("Your Device ", "")?.trim('(', ')') ?: fallbackName,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleSmall
        )
        
        if (isUser) {
             Text(
                text = "(${Build.MODEL})",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(2.dp))
        
        Text(
            text = "${device?.normalizedScore ?: 0}",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = color,
            letterSpacing = (-1).sp
        )
        Text(
            text = "POINTS",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color.copy(alpha = 0.7f),
            fontSize = 8.sp
        )
    }
}

@Composable
private fun MainScoreComparison(
    userDevice: RankingItem?,
    selectedDevice: RankingItem,
    startDelay: Int = 100
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp) // Less vertical spacing
    ) {
        // Single-Core Score
        ScoreComparisonCard(
            title = "Single-Core",
            userScore = userDevice?.singleCore ?: 0,
            selectedScore = selectedDevice.singleCore,
            userColor = MaterialTheme.colorScheme.primary,
            selectedColor = MaterialTheme.colorScheme.secondary,
            delayMillis = startDelay
        )
        
        // Multi-Core Score
        ScoreComparisonCard(
            title = "Multi-Core",
            userScore = userDevice?.multiCore ?: 0,
            selectedScore = selectedDevice.multiCore,
            userColor = MaterialTheme.colorScheme.primary,
            selectedColor = MaterialTheme.colorScheme.secondary,
            delayMillis = startDelay + 100
        )
        
        // Final Score
        ScoreComparisonCard(
            title = "Final Score",
            userScore = userDevice?.normalizedScore ?: 0,
            selectedScore = selectedDevice.normalizedScore,
            userColor = MaterialTheme.colorScheme.primary,
            selectedColor = MaterialTheme.colorScheme.secondary,
            delayMillis = startDelay + 200
        )
    }
}

@Composable
private fun ScoreComparisonCard(
    title: String,
    userScore: Int,
    selectedScore: Int,
    userColor: Color,
    selectedColor: Color,
    delayMillis: Int = 0
) {
    val maxScore = maxOf(userScore, selectedScore, 1)
    val userProgress by animateFloatAsState(
        targetValue = userScore.toFloat() / maxScore,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "userProgress"
    )
    val selectedProgress by animateFloatAsState(
        targetValue = selectedScore.toFloat() / maxScore,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "selectedProgress"
    )
    
    // Calculate percentage difference
    val scoreDiff = userScore - selectedScore
    val percentDiff = if (selectedScore > 0) {
        (scoreDiff.toFloat() / selectedScore * 100).toInt()
    } else if (userScore > 0) 100 else 0
    
    com.ivarna.finalbenchmark2.ui.components.AnimatedGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
        delayMillis = delayMillis
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp) // Compact padding
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp
                )
                
                // Percentage difference chip
                if (percentDiff != 0) {
                    val diffColor = if (percentDiff > 0) Color(0xFF4CAF50) else Color(0xFFE53935)
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = diffColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (percentDiff > 0) "+$percentDiff%" else "$percentDiff%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = diffColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            
            // User's progress bar with label on the same line
            LabeledScoreBar(
                label = "You",
                score = userScore,
                progress = userProgress,
                color = userColor,
                delta = if (scoreDiff > 0) "+$scoreDiff" else "$scoreDiff"
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Selected device's progress bar with label on the same line
            LabeledScoreBar(
                label = "Ref",
                score = selectedScore,
                progress = selectedProgress,
                color = selectedColor,
                delta = if (scoreDiff < 0) "+${-scoreDiff}" else "${-scoreDiff}"
            )
        }
    }
}

@Composable
private fun LabeledScoreBar(
    label: String,
    score: Int,
    progress: Float,
    color: Color,
    delta: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            modifier = Modifier.width(36.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp) // Thinner bar
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(color.copy(alpha = 0.8f), color)
                        )
                    )
            )
        }
        
        Column(
            modifier = Modifier.width(60.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = formatScore(score),
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = color,
                textAlign = TextAlign.End
            )
        }
    }
}


data class BenchmarkComparisonItem(
    val name: String,
    val icon: ImageVector,
    val userScore: Double,
    val selectedScore: Double
)

private fun getSingleCoreBenchmarkItems(
    userDevice: RankingItem?,
    selectedDevice: RankingItem
): List<BenchmarkComparisonItem> {
    val userDetails = userDevice?.benchmarkDetails
    val selectedDetails = selectedDevice.benchmarkDetails
    
    // Helper function to calculate score from Mops/s
    // Note: benchmarkDetails stores Mops/s, but SCORING_FACTORS expect ops/s
    fun calculateScore(mops: Double, benchmarkName: BenchmarkName): Double {
        val opsPerSecond = mops * 1_000_000.0  // Convert Mops/s to ops/s
        return opsPerSecond * (KotlinBenchmarkManager.SCORING_FACTORS[benchmarkName] ?: 0.0)
    }
    
    return listOf(
        BenchmarkComparisonItem(
            name = "Prime Generation",
            icon = Icons.Rounded.Calculate,
            userScore = calculateScore(userDetails?.singleCorePrimeNumberMops ?: 0.0, BenchmarkName.PRIME_GENERATION),
            selectedScore = calculateScore(selectedDetails?.singleCorePrimeNumberMops ?: 0.0, BenchmarkName.PRIME_GENERATION)
        ),
        BenchmarkComparisonItem(
            name = "Fibonacci",
            icon = Icons.Rounded.Functions,
            userScore = calculateScore(userDetails?.singleCoreFibonacciMops ?: 0.0, BenchmarkName.FIBONACCI_ITERATIVE),
            selectedScore = calculateScore(selectedDetails?.singleCoreFibonacciMops ?: 0.0, BenchmarkName.FIBONACCI_ITERATIVE)
        ),
        BenchmarkComparisonItem(
            name = "Matrix Multiplication",
            icon = Icons.Rounded.GridOn,
            userScore = calculateScore(userDetails?.singleCoreMatrixMultiplicationMops ?: 0.0, BenchmarkName.MATRIX_MULTIPLICATION),
            selectedScore = calculateScore(selectedDetails?.singleCoreMatrixMultiplicationMops ?: 0.0, BenchmarkName.MATRIX_MULTIPLICATION)
        ),
        BenchmarkComparisonItem(
            name = "Hash Computing",
            icon = Icons.Rounded.Lock,
            userScore = calculateScore(userDetails?.singleCoreHashComputingMops ?: 0.0, BenchmarkName.HASH_COMPUTING),
            selectedScore = calculateScore(selectedDetails?.singleCoreHashComputingMops ?: 0.0, BenchmarkName.HASH_COMPUTING)
        ),
        BenchmarkComparisonItem(
            name = "String Sorting",
            icon = Icons.Rounded.SortByAlpha,
            userScore = calculateScore(userDetails?.singleCoreStringSortingMops ?: 0.0, BenchmarkName.STRING_SORTING),
            selectedScore = calculateScore(selectedDetails?.singleCoreStringSortingMops ?: 0.0, BenchmarkName.STRING_SORTING)
        ),
        BenchmarkComparisonItem(
            name = "Ray Tracing",
            icon = Icons.Rounded.Lightbulb,
            userScore = calculateScore(userDetails?.singleCoreRayTracingMops ?: 0.0, BenchmarkName.RAY_TRACING),
            selectedScore = calculateScore(selectedDetails?.singleCoreRayTracingMops ?: 0.0, BenchmarkName.RAY_TRACING)
        ),
        BenchmarkComparisonItem(
            name = "Compression",
            icon = Icons.Rounded.Compress,
            userScore = calculateScore(userDetails?.singleCoreCompressionMops ?: 0.0, BenchmarkName.COMPRESSION),
            selectedScore = calculateScore(selectedDetails?.singleCoreCompressionMops ?: 0.0, BenchmarkName.COMPRESSION)
        ),
        BenchmarkComparisonItem(
            name = "Monte Carlo",
            icon = Icons.Rounded.Casino,
            userScore = calculateScore(userDetails?.singleCoreMonteCarloMops ?: 0.0, BenchmarkName.MONTE_CARLO),
            selectedScore = calculateScore(selectedDetails?.singleCoreMonteCarloMops ?: 0.0, BenchmarkName.MONTE_CARLO)
        ),
        BenchmarkComparisonItem(
            name = "JSON Parsing",
            icon = Icons.Rounded.Code,
            userScore = calculateScore(userDetails?.singleCoreJsonParsingMops ?: 0.0, BenchmarkName.JSON_PARSING),
            selectedScore = calculateScore(selectedDetails?.singleCoreJsonParsingMops ?: 0.0, BenchmarkName.JSON_PARSING)
        ),
        BenchmarkComparisonItem(
            name = "N-Queens",
            icon = Icons.Rounded.Dashboard,
            userScore = calculateScore(userDetails?.singleCoreNQueensMops ?: 0.0, BenchmarkName.N_QUEENS),
            selectedScore = calculateScore(selectedDetails?.singleCoreNQueensMops ?: 0.0, BenchmarkName.N_QUEENS)
        )
    )
}

private fun getMultiCoreBenchmarkItems(
    userDevice: RankingItem?,
    selectedDevice: RankingItem
): List<BenchmarkComparisonItem> {
    val userDetails = userDevice?.benchmarkDetails
    val selectedDetails = selectedDevice.benchmarkDetails
    
    // Helper function to calculate score from Mops/s
    // Note: benchmarkDetails stores Mops/s, but SCORING_FACTORS expect ops/s
    fun calculateScore(mops: Double, benchmarkName: BenchmarkName): Double {
        val opsPerSecond = mops * 1_000_000.0  // Convert Mops/s to ops/s
        return opsPerSecond * (KotlinBenchmarkManager.SCORING_FACTORS[benchmarkName] ?: 0.0)
    }
    
    return listOf(
        BenchmarkComparisonItem(
            name = "Prime Generation",
            icon = Icons.Rounded.Calculate,
            userScore = calculateScore(userDetails?.multiCorePrimeNumberMops ?: 0.0, BenchmarkName.PRIME_GENERATION),
            selectedScore = calculateScore(selectedDetails?.multiCorePrimeNumberMops ?: 0.0, BenchmarkName.PRIME_GENERATION)
        ),
        BenchmarkComparisonItem(
            name = "Fibonacci",
            icon = Icons.Rounded.Functions,
            userScore = calculateScore(userDetails?.multiCoreFibonacciMops ?: 0.0, BenchmarkName.FIBONACCI_ITERATIVE),
            selectedScore = calculateScore(selectedDetails?.multiCoreFibonacciMops ?: 0.0, BenchmarkName.FIBONACCI_ITERATIVE)
        ),
        BenchmarkComparisonItem(
            name = "Matrix Multiplication",
            icon = Icons.Rounded.GridOn,
            userScore = calculateScore(userDetails?.multiCoreMatrixMultiplicationMops ?: 0.0, BenchmarkName.MATRIX_MULTIPLICATION),
            selectedScore = calculateScore(selectedDetails?.multiCoreMatrixMultiplicationMops ?: 0.0, BenchmarkName.MATRIX_MULTIPLICATION)
        ),
        BenchmarkComparisonItem(
            name = "Hash Computing",
            icon = Icons.Rounded.Lock,
            userScore = calculateScore(userDetails?.multiCoreHashComputingMops ?: 0.0, BenchmarkName.HASH_COMPUTING),
            selectedScore = calculateScore(selectedDetails?.multiCoreHashComputingMops ?: 0.0, BenchmarkName.HASH_COMPUTING)
        ),
        BenchmarkComparisonItem(
            name = "String Sorting",
            icon = Icons.Rounded.SortByAlpha,
            userScore = calculateScore(userDetails?.multiCoreStringSortingMops ?: 0.0, BenchmarkName.STRING_SORTING),
            selectedScore = calculateScore(selectedDetails?.multiCoreStringSortingMops ?: 0.0, BenchmarkName.STRING_SORTING)
        ),
        BenchmarkComparisonItem(
            name = "Ray Tracing",
            icon = Icons.Rounded.Lightbulb,
            userScore = calculateScore(userDetails?.multiCoreRayTracingMops ?: 0.0, BenchmarkName.RAY_TRACING),
            selectedScore = calculateScore(selectedDetails?.multiCoreRayTracingMops ?: 0.0, BenchmarkName.RAY_TRACING)
        ),
        BenchmarkComparisonItem(
            name = "Compression",
            icon = Icons.Rounded.Compress,
            userScore = calculateScore(userDetails?.multiCoreCompressionMops ?: 0.0, BenchmarkName.COMPRESSION),
            selectedScore = calculateScore(selectedDetails?.multiCoreCompressionMops ?: 0.0, BenchmarkName.COMPRESSION)
        ),
        BenchmarkComparisonItem(
            name = "Monte Carlo",
            icon = Icons.Rounded.Casino,
            userScore = calculateScore(userDetails?.multiCoreMonteCarloMops ?: 0.0, BenchmarkName.MONTE_CARLO),
            selectedScore = calculateScore(selectedDetails?.multiCoreMonteCarloMops ?: 0.0, BenchmarkName.MONTE_CARLO)
        ),
        BenchmarkComparisonItem(
            name = "JSON Parsing",
            icon = Icons.Rounded.Code,
            userScore = calculateScore(userDetails?.multiCoreJsonParsingMops ?: 0.0, BenchmarkName.JSON_PARSING),
            selectedScore = calculateScore(selectedDetails?.multiCoreJsonParsingMops ?: 0.0, BenchmarkName.JSON_PARSING)
        ),
        BenchmarkComparisonItem(
            name = "N-Queens",
            icon = Icons.Rounded.Dashboard,
            userScore = calculateScore(userDetails?.multiCoreNQueensMops ?: 0.0, BenchmarkName.N_QUEENS),
            selectedScore = calculateScore(selectedDetails?.multiCoreNQueensMops ?: 0.0, BenchmarkName.N_QUEENS)
        )
    )
}

@Composable
private fun BenchmarkComparisonCard(
    benchmark: BenchmarkComparisonItem,
    userDeviceName: String,
    selectedDeviceName: String,
    delayMillis: Int = 0,
    animationDuration: Int = 500
) {
    val userWins = benchmark.userScore > benchmark.selectedScore
    val maxScore = maxOf(benchmark.userScore, benchmark.selectedScore, 1.0)
    
    val userProgress by animateFloatAsState(
        targetValue = (benchmark.userScore / maxScore).toFloat(),
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "benchmarkUserProgress"
    )
    val selectedProgress by animateFloatAsState(
        targetValue = (benchmark.selectedScore / maxScore).toFloat(),
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "benchmarkSelectedProgress"
    )
    
    com.ivarna.finalbenchmark2.ui.components.AnimatedGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
        delayMillis = delayMillis,
        animationDuration = animationDuration
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp) // Compact padding
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp) // Smaller icon
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = benchmark.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = benchmark.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp
                    )
                    
                    val scoreDiff = benchmark.userScore - benchmark.selectedScore
                    val percentDiff = if (benchmark.selectedScore > 0) {
                        (scoreDiff / benchmark.selectedScore * 100).toInt()
                    } else 0
                    
                    val diffText = if (userWins) "Faster by $percentDiff%" else "Slower by ${-percentDiff}%"
                    val diffColor = if (userWins) Color(0xFF4CAF50) else Color(0xFFE53935)
                    
                    Text(
                        text = diffText,
                        fontSize = 11.sp,
                        color = diffColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Labeled bars for better understanding
            // User Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "You",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.width(36.dp),
                    fontWeight = FontWeight.Bold
                )
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp) // Thinner bar
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(userProgress)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                        MaterialTheme.colorScheme.primary
                                    )
                                )
                            )
                    )
                }
                
                Text(
                    text = String.format("%.0f", benchmark.userScore),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(40.dp),
                    textAlign = TextAlign.End
                )
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            // Selected Device Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ref",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.width(36.dp),
                     fontWeight = FontWeight.Bold
                )
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp) // Thinner bar
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(selectedProgress)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                                        MaterialTheme.colorScheme.secondary
                                    )
                                )
                            )
                    )
                }
                
                Text(
                    text = String.format("%.0f", benchmark.selectedScore),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.width(40.dp),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

private fun formatScore(score: Int): String {
    return String.format("%,d", score)
}
