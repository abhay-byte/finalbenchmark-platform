package com.ivarna.finalbenchmark2.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ivarna.finalbenchmark2.ui.components.InformationRow
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryDetailScreen(
    benchmarkId: Long,
    onBackClick: () -> Unit,
    historyRepository: com.ivarna.finalbenchmark2.data.repository.HistoryRepository
) {
    val resultState by historyRepository.getResultById(benchmarkId).collectAsState(initial = null)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Result Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        // Null check handles loading state
        resultState?.let { data ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Hero Score Card
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
                            text = String.format("%.0f", data.benchmarkResult.totalScore),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Total Score",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Date and Rating
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.CalendarToday, 
                                null, 
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                // Helper to format long timestamp
                                text = formatDate(data.benchmarkResult.timestamp), 
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Rating stars
                        val rating = calculateRating(data.benchmarkResult.totalScore)
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

                // 2. Performance Breakdown Card
                if (data.cpuTestDetail != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Single Core Card
                        DetailCard(
                            title = "Single-Core",
                            value = data.benchmarkResult.singleCoreScore,
                            icon = Icons.Rounded.LooksOne,
                            modifier = Modifier.weight(1f)
                        )
                        // Multi Core Card
                        DetailCard(
                            title = "Multi-Core",
                            value = data.benchmarkResult.multiCoreScore,
                            icon = Icons.Rounded.Groups,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                // 3. Metadata Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // Benchmark Type
                        InformationRow(
                            itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Benchmark Type", data.benchmarkResult.type),
                            isLastItem = false
                        )
                        
                        // Device Model
                        InformationRow(
                            itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Device Model", data.benchmarkResult.deviceModel),
                            isLastItem = true
                        )
                    }
                }
            }
        } ?: run {
            // Loading state
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

private fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(date)
}

private fun calculateRating(score: Double): Int {
    return when {
        score >= 90 -> 5
        score >= 70 -> 4
        score >= 50 -> 3
        score >= 30 -> 2
        else -> 1
    }
}