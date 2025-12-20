package com.ivarna.finalbenchmark2.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        // Sort Dropdown
        Box {
            var expanded by remember { mutableStateOf(false) }
            AssistChip(
                    onClick = { expanded = true },
                    label = { Text("Sort: ${formatSortName(currentSort)}") },
                    leadingIcon = { Icon(Icons.Rounded.Sort, null) }
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                HistorySort.values().forEach { option ->
                    DropdownMenuItem(
                            text = { Text(formatSortName(option)) },
                            onClick = {
                                onSortSelect(option)
                                expanded = false
                            }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Category Chips
        val categories = listOf("All", "CPU", "GPU", "RAM", "Storage", "Full", "Stress")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories) { category ->
                FilterChip(
                        selected = selectedCategory == category,
                        onClick = { onCategorySelect(category) },
                        label = { Text(category) },
                        leadingIcon =
                                if (selectedCategory == category) {
                                    { Icon(Icons.Rounded.Check, null, Modifier.size(16.dp)) }
                                } else null
                )
            }
        }
    }
}

fun formatSortName(sort: HistorySort): String {
    return when (sort) {
        HistorySort.DATE_NEWEST -> "Date (Newest)"
        HistorySort.DATE_OLDEST -> "Date (Oldest)"
        HistorySort.SCORE_HIGH_TO_LOW -> "Score (High to Low)"
        HistorySort.SCORE_LOW_TO_HIGH -> "Score (Low to High)"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel, navController: NavController) {
    val screenState by viewModel.screenState.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    FinalBenchmark2Theme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                    modifier =
                            Modifier.fillMaxSize()
                                    .padding(
                                            top = 56.dp,
                                            start = 16.dp,
                                            end = 16.dp,
                                            bottom = 16.dp
                                    )
            ) {
                // Header
                Text(
                        text = "Benchmark History",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )

                // Filter bar
                HistoryFilterBar(
                        selectedCategory = selectedCategory,
                        onCategorySelect = { viewModel.updateCategory(it) },
                        currentSort = sortOption,
                        onSortSelect = { viewModel.updateSortOption(it) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                when (val state = screenState) {
                    is HistoryScreenState.Loading -> {
                        // Show loading indicator
                        Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator() }
                    }
                    is HistoryScreenState.Empty -> {
                        // Empty state
                        Box(
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                        imageVector = Icons.Default.List,
                                        contentDescription = "No history",
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                        text = "No benchmark history found",
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 16.dp)
                                )
                                Text(
                                        text = "Run your first benchmark to see results here",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    is HistoryScreenState.Success -> {
                        // Benchmark history list
                        LazyColumn(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.results) { result ->
                                BenchmarkHistoryItem(
                                        result = result,
                                        timestampFormatter = formatter,
                                        onItemClick = {
                                            // Convert detailed results to JSON using Gson
                                            val gson = Gson()
                                            val detailedResultsJson =
                                                    gson.toJson(result.detailedResults)

                                            // Construct summary JSON for ResultScreen
                                            // Include performance_metrics for graphs display
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

                                            // URL-encode the JSON to handle special characters
                                            // properly
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

@Composable
fun BenchmarkHistoryItem(
        result: HistoryUiModel,
        timestampFormatter: SimpleDateFormat,
        onItemClick: () -> Unit = {}
) {
    Card(
            modifier = Modifier.fillMaxWidth().clickable { onItemClick() },
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = result.testName, fontWeight = FontWeight.Medium, fontSize = 16.sp)

                Text(
                        text = timestampFormatter.format(Date(result.timestamp)),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                        text =
                                "Single: ${String.format("%.1f", result.singleCoreScore)} | Multi: ${String.format("%.1f", result.multiCoreScore)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Final score
            Column(horizontalAlignment = Alignment.End) {
                Text(
                        text = String.format("%.1f", result.finalScore),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.primary
                )
                Text(
                        text = "Score",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
