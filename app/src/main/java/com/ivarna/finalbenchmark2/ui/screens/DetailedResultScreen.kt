package com.ivarna.finalbenchmark2.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkResult
import com.ivarna.finalbenchmark2.ui.theme.FinalBenchmark2Theme
import org.json.JSONObject
import org.json.JSONArray
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailedResultScreen(
    summaryJson: String,
    onBack: () -> Unit
) {
    // Parse the summary JSON to extract detailed results
    val detailedResults = remember(summaryJson) {
        try {
            Log.d("DetailedResultScreen", "Received summary JSON: $summaryJson")
            val jsonObject = JSONObject(summaryJson)
            val detailedResultsArray = jsonObject.optJSONArray("detailed_results")
            val results = mutableListOf<BenchmarkResult>()
            
            if (detailedResultsArray != null) {
                for (i in 0 until detailedResultsArray.length()) {
                    val resultObj = detailedResultsArray.getJSONObject(i)
                    results.add(
                        BenchmarkResult(
                            name = resultObj.optString("name", "Unknown"),
                            executionTimeMs = resultObj.optDouble("executionTimeMs", 0.0),
                            opsPerSecond = resultObj.optDouble("opsPerSecond", 0.0),
                            isValid = resultObj.optBoolean("isValid", false),
                            metricsJson = resultObj.optString("metricsJson", "{}")
                        )
                    )
                }
            }
            results
        } catch (e: Exception) {
            Log.e("DetailedResultScreen", "Error parsing summary JSON: ${e.message}", e)
            emptyList()
        }
    }

    FinalBenchmark2Theme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Detailed Benchmark Results (${detailedResults.size} tests)",
                            fontSize = 20.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(detailedResults) { result ->
                    DetailedResultListItem(result = result)
                }
            }
        }
    }
}

@Composable
fun DetailedResultListItem(result: BenchmarkResult) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = result.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Time: ${String.format("%.2f", result.executionTimeMs)} ms",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Score: ${String.format("%.2f", result.opsPerSecond)} ops/sec",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = if (result.isValid) "✓ Valid" else "✗ Invalid",
                    fontSize = 14.sp,
                    color = if (result.isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}