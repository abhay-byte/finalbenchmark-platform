package com.ivarna.finalbenchmark2.ui.screens

import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
            android.util.Log.d("CpuComparison", "Raw JSON: $selectedDeviceJson")
            val device = Gson().fromJson(selectedDeviceJson, RankingItem::class.java)
            android.util.Log.d("CpuComparison", "Parsed device: ${device.name}")
            android.util.Log.d("CpuComparison", "BenchmarkDetails: ${device.benchmarkDetails}")
            android.util.Log.d("CpuComparison", "Single-core Prime Mops: ${device.benchmarkDetails?.singleCorePrimeNumberMops}")
            android.util.Log.d("CpuComparison", "Single-core Fibonacci Mops: ${device.benchmarkDetails?.singleCoreFibonacciMops}")
            android.util.Log.d("CpuComparison", "Multi-core Prime Mops: ${device.benchmarkDetails?.multiCorePrimeNumberMops}")
            device
        } catch (e: Exception) {
            android.util.Log.e("CpuComparison", "Error parsing device JSON", e)
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
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "CPU Comparison",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else if (selectedDevice == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Unable to load device data",
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with device names
                item {
                    ComparisonHeader(
                        userDevice = userDevice,
                        selectedDevice = selectedDevice
                    )
                }
                
                // Main score comparison cards
                item {
                    MainScoreComparison(
                        userDevice = userDevice,
                        selectedDevice = selectedDevice
                    )
                }
                
                // Single-Core Benchmarks Section
                item {
                    Text(
                        text = "Single-Core Benchmarks",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                
                val singleCoreBenchmarks = getSingleCoreBenchmarkItems(userDevice, selectedDevice)
                items(singleCoreBenchmarks) { benchmark ->
                    BenchmarkComparisonCard(
                        benchmark = benchmark,
                        userDeviceName = userDevice?.name ?: "Your Device",
                        selectedDeviceName = selectedDevice.name
                    )
                }
                
                // Multi-Core Benchmarks Section
                item {
                    Text(
                        text = "Multi-Core Benchmarks",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                    )
                }
                
                val multiCoreBenchmarks = getMultiCoreBenchmarkItems(userDevice, selectedDevice)
                items(multiCoreBenchmarks) { benchmark ->
                    BenchmarkComparisonCard(
                        benchmark = benchmark,
                        userDeviceName = userDevice?.name ?: "Your Device",
                        selectedDeviceName = selectedDevice.name
                    )
                }
                
                // Bottom spacing
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ComparisonHeader(
    userDevice: RankingItem?,
    selectedDevice: RankingItem
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
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
                    shape = RoundedCornerShape(12.dp),
                    color = diffColor.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isAhead) Icons.AutoMirrored.Rounded.TrendingUp else Icons.AutoMirrored.Rounded.TrendingDown,
                            contentDescription = null,
                            tint = diffColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (percentDiff >= 0) "+$percentDiff%" else "$percentDiff%",
                            fontSize = 24.sp,
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                // User device
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PhoneAndroid,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = userDevice?.name?.replace("Your Device ", "")?.trim('(', ')') ?: "Your Device",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${userDevice?.normalizedScore ?: 0}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                // VS indicator
                Box(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "VS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Selected device
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Memory,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = selectedDevice.name.replace("Your Device ", "").trim('(', ')'),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${selectedDevice.normalizedScore}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
private fun MainScoreComparison(
    userDevice: RankingItem?,
    selectedDevice: RankingItem
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Single-Core Score
        ScoreComparisonCard(
            title = "Single-Core Score",
            userScore = userDevice?.singleCore ?: 0,
            selectedScore = selectedDevice.singleCore,
            userColor = MaterialTheme.colorScheme.primary,
            selectedColor = MaterialTheme.colorScheme.secondary
        )
        
        // Multi-Core Score
        ScoreComparisonCard(
            title = "Multi-Core Score",
            userScore = userDevice?.multiCore ?: 0,
            selectedScore = selectedDevice.multiCore,
            userColor = MaterialTheme.colorScheme.primary,
            selectedColor = MaterialTheme.colorScheme.secondary
        )
        
        // Final Score
        ScoreComparisonCard(
            title = "Final Score",
            userScore = userDevice?.normalizedScore ?: 0,
            selectedScore = selectedDevice.normalizedScore,
            userColor = MaterialTheme.colorScheme.primary,
            selectedColor = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
private fun ScoreComparisonCard(
    title: String,
    userScore: Int,
    selectedScore: Int,
    userColor: Color,
    selectedColor: Color
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
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // Percentage difference chip
                if (percentDiff != 0) {
                    val diffColor = if (percentDiff > 0) Color(0xFF4CAF50) else Color(0xFFE53935)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = diffColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (percentDiff > 0) "+$percentDiff%" else "$percentDiff%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = diffColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            
            // User's progress bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "You",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.width(50.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(userColor.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(userProgress)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(userColor, userColor.copy(alpha = 0.7f))
                                )
                            )
                    )
                }
                Text(
                    text = formatScore(userScore),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = userColor,
                    modifier = Modifier.width(60.dp),
                    textAlign = TextAlign.End
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Selected device's progress bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Other",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.width(50.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(selectedColor.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(selectedProgress)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(selectedColor, selectedColor.copy(alpha = 0.7f))
                                )
                            )
                    )
                }
                Text(
                    text = formatScore(selectedScore),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = selectedColor,
                    modifier = Modifier.width(60.dp),
                    textAlign = TextAlign.End
                )
            }
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
    selectedDeviceName: String
) {
    val userWins = benchmark.userScore > benchmark.selectedScore
    val isTie = benchmark.userScore == benchmark.selectedScore
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
    
    val winnerColor = if (userWins) Color(0xFF4CAF50) else Color(0xFFE53935)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = benchmark.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = benchmark.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                if (!isTie && (benchmark.userScore > 0 || benchmark.selectedScore > 0)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = winnerColor.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (userWins) Icons.Rounded.EmojiEvents else Icons.Rounded.Close,
                                contentDescription = null,
                                tint = winnerColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (userWins) "You win" else "Behind",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = winnerColor
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Score comparison bars
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // User score
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(userProgress)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format("%.2f pts", benchmark.userScore),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Difference chip
                val diff = benchmark.userScore - benchmark.selectedScore
                val percentDiff = if (benchmark.selectedScore > 0) {
                    (diff / benchmark.selectedScore * 100).toInt()
                } else if (benchmark.userScore > 0) 100 else 0
                
                Text(
                    text = if (percentDiff >= 0) "+$percentDiff%" else "$percentDiff%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (percentDiff >= 0) Color(0xFF4CAF50) else Color(0xFFE53935),
                    modifier = Modifier.width(48.dp),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Selected device score
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(selectedProgress)
                                .background(MaterialTheme.colorScheme.secondary)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format("%.2f pts", benchmark.selectedScore),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

private fun formatScore(score: Int): String {
    return when {
        score >= 1_000_000 -> String.format("%.1fM", score / 1_000_000.0)
        score >= 1_000 -> String.format("%.1fK", score / 1_000.0)
        else -> score.toString()
    }
}
