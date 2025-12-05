package com.ivarna.finalbenchmark2.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkResult
import com.ivarna.finalbenchmark2.ui.components.InformationRow
import com.ivarna.finalbenchmark2.ui.viewmodels.HistoryUiModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryDetailScreen(
    benchmarkId: Long,
    initialDataJson: String = "",
    onBackClick: () -> Unit,
    historyRepository: com.ivarna.finalbenchmark2.data.repository.HistoryRepository
) {
    // Parse initial data if available
    val initialData = remember(initialDataJson) {
        if (initialDataJson.isNotEmpty()) {
            try {
                val gson = com.google.gson.Gson()
                gson.fromJson(initialDataJson, HistoryUiModel::class.java)
            } catch (e: Exception) {
                android.util.Log.e("HistoryDetailScreen", "Error parsing initial data JSON: ${e.message}")
                null
            }
        } else {
            null
        }
    }
    
    val resultState by historyRepository.getResultById(benchmarkId).collectAsState(initial = null)
    
    // Determine what to display: full result if loaded, initial data if available and result not loaded, or null if nothing available
    val displayData = resultState?.benchmarkResult ?: if (initialData != null) {
        // Create a minimal BenchmarkResult from initial data for immediate display
        com.ivarna.finalbenchmark2.data.database.entities.BenchmarkResultEntity(
            id = initialData.id,
            totalScore = initialData.finalScore,
            singleCoreScore = initialData.singleCoreScore,
            multiCoreScore = initialData.multiCoreScore,
            normalizedScore = initialData.normalizedScore,
            timestamp = initialData.timestamp,
            type = initialData.testName,
            detailedResultsJson = "" // Will be populated when full data loads
        )
    } else null

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Benchmark Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        // Use displayData which contains either full result or initial data
        displayData?.let { data ->
            // Parse detailed results from JSON string if available
            val detailedResults = try {
                if (resultState?.benchmarkResult?.detailedResultsJson?.isNotEmpty() == true) {
                    val gson = com.google.gson.Gson()
                    val listType = object : com.google.gson.reflect.TypeToken<List<com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkResult>>() {}.type
                    gson.fromJson(resultState?.benchmarkResult?.detailedResultsJson ?: "", listType) as List<com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkResult>
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                android.util.Log.e("HistoryDetailScreen", "Error parsing detailed results JSON: ${e.message}")
                emptyList()
            }

            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // 1. Hero Score Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = String.format("%.0f", data.totalScore),
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Normalized: ${String.format("%.0f", data.normalizedScore)}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Date and Rating
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.Event,
                                    null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    // Helper to format long timestamp
                                    text = formatDate(data.timestamp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Rating stars
                            val rating = calculateRating(data.normalizedScore)
                            Row {
                                repeat(5) { index ->
                                    Icon(
                                        imageVector = if (index < rating) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                        contentDescription = "Rating star",
                                        tint = if (index < rating) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Performance Breakdown Card
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Single Core Card
                        DetailCard(
                            title = "Single-Core",
                            value = data.singleCoreScore,
                            icon = Icons.Rounded.Memory,
                            modifier = Modifier.weight(1f)
                        )
                        // Multi Core Card
                        DetailCard(
                            title = "Multi-Core",
                            value = data.multiCoreScore,
                            icon = Icons.Rounded.Hub,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                // 3. Detailed Test Results Header
                item {
                    Text(
                        text = "Individual Test Results",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                
                // 4. Detailed Test Results
                items(detailedResults) { result ->
                    TestResultRow(result = result)
                }
            }
        } ?: run {
            // Show initial data card while loading detailed data, or loading indicator if no initial data
            if (initialData != null) {
                LazyColumn(
                    modifier = Modifier
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    // 1. Hero Score Card with initial data
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Column(
                                modifier = Modifier.padding(32.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = String.format("%.0f", initialData.finalScore),
                                    style = MaterialTheme.typography.displayMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Normalized: ${String.format("%.0f", initialData.normalizedScore)}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                // Date and Rating
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Rounded.Event,
                                        null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        // Helper to format long timestamp
                                        text = formatDate(initialData.timestamp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // Rating stars
                                val rating = calculateRating(initialData.normalizedScore)
                                Row {
                                    repeat(5) { index ->
                                        Icon(
                                            imageVector = if (index < rating) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                            contentDescription = "Rating star",
                                            tint = if (index < rating) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. Performance Breakdown Card
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Single Core Card
                            DetailCard(
                                title = "Single-Core",
                                value = initialData.singleCoreScore,
                                icon = Icons.Rounded.Memory,
                                modifier = Modifier.weight(1f)
                            )
                            // Multi Core Card
                            DetailCard(
                                title = "Multi-Core",
                                value = initialData.multiCoreScore,
                                icon = Icons.Rounded.Hub,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    // 3. Loading indicator for detailed results
                    item {
                        Text(
                            text = "Individual Test Results",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.padding(16.dp)
                                )
                                Text(
                                    text = "Loading detailed results...",
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // Show loading indicator if no initial data is available
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun DetailCard(title: String, value: Double, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(
                text = String.format("%.0f", value),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun TestResultRow(result: BenchmarkResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (result.isValid) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                null,
                tint = if (result.isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Time: ${String.format("%.2f", result.executionTimeMs)} ms",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = String.format("%.0f", result.opsPerSecond),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(date)
}

private fun calculateRating(score: Double): Int {
    return when {
        score >= 80000 -> 5
        score >= 60000 -> 4
        score >= 40000 -> 3
        score >= 20000 -> 2
        else -> 1
    }
}