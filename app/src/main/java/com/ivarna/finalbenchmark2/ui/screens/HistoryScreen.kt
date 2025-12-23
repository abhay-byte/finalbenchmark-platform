package com.ivarna.finalbenchmark2.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.gson.Gson
import com.ivarna.finalbenchmark2.ui.theme.FinalBenchmark2Theme
import com.ivarna.finalbenchmark2.ui.viewmodels.HistoryScreenState
import com.ivarna.finalbenchmark2.ui.viewmodels.HistorySort
import com.ivarna.finalbenchmark2.ui.viewmodels.HistoryUiModel
import com.ivarna.finalbenchmark2.ui.viewmodels.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryFilterBar(
        selectedCategory: String,
        onCategorySelect: (String) -> Unit,
        currentSort: HistorySort,
        onSortSelect: (HistorySort) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        // Sort Dropdown (Glass Style)
        Box {
            var expanded by remember { mutableStateOf(false) }
            
            // Glass Sort Button
            Surface(
                onClick = { expanded = true },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Sort, 
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sort: ${formatSortName(currentSort)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            DropdownMenu(
                expanded = expanded, 
                onDismissRequest = { expanded = false },
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                HistorySort.values().forEach { option ->
                    DropdownMenuItem(
                            text = { 
                                Text(
                                    formatSortName(option),
                                    fontWeight = if (option == currentSort) FontWeight.Bold else FontWeight.Normal
                                ) 
                            },
                            onClick = {
                                onSortSelect(option)
                                expanded = false
                            },
                            leadingIcon = if (option == currentSort) {
                                { Icon(Icons.Rounded.Check, null) }
                            } else null
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Category Chips (Horizontal Scroll)
        val categories = listOf("All", "CPU", "GPU", "RAM", "Storage", "Full", "Stress")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories) { category ->
                val isSelected = selectedCategory == category
                
                // Custom Glass Chip
                Surface(
                    onClick = { onCategorySelect(category) },
                    shape = RoundedCornerShape(50), // Pill shape
                    color = if (isSelected) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = if (isSelected) 
                        null 
                    else 
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = category,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) 
                                MaterialTheme.colorScheme.onPrimaryContainer 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

fun formatSortName(sort: HistorySort): String {
    return when (sort) {
        HistorySort.DATE_NEWEST -> "Newest First"
        HistorySort.DATE_OLDEST -> "Oldest First"
        HistorySort.SCORE_HIGH_TO_LOW -> "Highest Score"
        HistorySort.SCORE_LOW_TO_HIGH -> "Lowest Score"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel, navController: NavController) {
    val screenState by viewModel.screenState.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val formatter = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())

    FinalBenchmark2Theme {
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
                    modifier =
                            Modifier.fillMaxSize()
                                    .padding(top = 24.dp) // Adjusted top padding
            ) {
                // Large Modern Header
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                    Text(
                            text = "History",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-1).sp,
                            color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Track your device performance over time",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                // Filter bar
                HistoryFilterBar(
                        selectedCategory = selectedCategory,
                        onCategorySelect = { viewModel.updateCategory(it) },
                        currentSort = sortOption,
                        onSortSelect = { viewModel.updateSortOption(it) }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Box(modifier = Modifier.weight(1f)) {
                    when (val state = screenState) {
                        is HistoryScreenState.Loading -> {
                            Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                            ) { CircularProgressIndicator() }
                        }
                        is HistoryScreenState.Empty -> {
                            com.ivarna.finalbenchmark2.ui.components.EmptyStateView(
                                icon = Icons.Rounded.History,
                                title = "No Benchmarks Yet",
                                message = "Run a benchmark to see your results history here."
                            )
                        }
                        is HistoryScreenState.Success -> {
                            LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(bottom = 120.dp), // Bottom padding for list
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                itemsIndexed(
                                    items = state.results,
                                    key = { _, result -> result.id }
                                ) { index, result ->
                                    BenchmarkHistoryItem(
                                            result = result,
                                            timestampFormatter = formatter,
                                            delayMillis = index * 50, // Staggered delay
                                            onItemClick = {
                                                // Convert detailed results to JSON using Gson
                                                val gson = Gson()
                                                val detailedResultsJson =
                                                        gson.toJson(result.detailedResults)

                                                val summaryJson =
                                                        """
                                                {
                                                    "single_core_score": ${result.singleCoreScore},
                                                    "multi_core_score": ${result.multiCoreScore},
                                                    "final_score": ${result.finalScore},
                                                    "normalized_score": ${result.normalizedScore},
                                                    "timestamp": ${result.timestamp},
                                                    "benchmark_id": ${result.id},
                                                    "performance_metrics": ${if (result.performanceMetricsJson.isNotEmpty()) result.performanceMetricsJson else "{}"},
                                                    "detailed_results": $detailedResultsJson
                                                }
                                            """.trimIndent()

                                                val encodedJson =
                                                        java.net.URLEncoder.encode(summaryJson, "UTF-8")
                                                navController.navigate("result/$encodedJson")
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BenchmarkHistoryItem(
        result: HistoryUiModel,
        timestampFormatter: SimpleDateFormat,
        delayMillis: Int = 0,
        onItemClick: () -> Unit = {}
) {
    // Glass Card for History Item
    com.ivarna.finalbenchmark2.ui.components.AnimatedGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), // Maintain original translucent look
            borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
            delayMillis = delayMillis,
            onClick = onItemClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)
                        )
                    )
                )
        ) {
            Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Side: Type Icon + Details
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    // Type Icon Container
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val icon = when {
                            result.testName.contains("CPU", ignoreCase = true) -> Icons.Rounded.Memory
                            result.testName.contains("GPU", ignoreCase = true) -> Icons.Rounded.VideogameAsset
                            result.testName.contains("RAM", ignoreCase = true) -> Icons.Rounded.SdStorage
                            else -> Icons.Rounded.Speed
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = result.testName.replace("Benchmark", "").trim(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                    text = timestampFormatter.format(Date(result.timestamp)),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Right Side: Score
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                            text = String.format("%.0f", result.finalScore),
                            fontSize = 28.sp, // Large Score
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-1).sp,
                            color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                            text = "POINTS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}
