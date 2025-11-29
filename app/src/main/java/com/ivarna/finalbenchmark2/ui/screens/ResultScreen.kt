package com.ivarna.finalbenchmark2.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ivarna.finalbenchmark2.ui.theme.FinalBenchmark2Theme
import com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkResult
import org.json.JSONObject
import android.util.Log
import org.json.JSONArray

data class BenchmarkSummary(
    val singleCoreScore: Double,
    val multiCoreScore: Double,
    val finalScore: Double,
    val normalizedScore: Double,
    val rating: String,
    val detailedResults: List<BenchmarkResult> = emptyList() // Added for detailed view
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    summaryJson: String,
    onRunAgain: () -> Unit,
    onBackToHome: () -> Unit,
    onShowDetailedResults: (List<BenchmarkResult>) -> Unit = {}
) {
    val summary = try {
        Log.d("ResultScreen", "Received summary JSON: $summaryJson")
        val jsonObject = JSONObject(summaryJson)
        val detailedResults = mutableListOf<BenchmarkResult>()
        
        // Parse detailed results if available
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
                        metricsJson = resultObj.optString("metricsJson", "{}")
                    )
                )
            }
        }
        
        val summary = BenchmarkSummary(
            singleCoreScore = jsonObject.optDouble("single_core_score", 0.0),
            multiCoreScore = jsonObject.optDouble("multi_core_score", 0.0),
            finalScore = jsonObject.optDouble("final_score", 0.0),
            normalizedScore = jsonObject.optDouble("normalized_score", 0.0),
            rating = jsonObject.optString("rating", "★"),
            detailedResults = detailedResults
        )
        Log.d("ResultScreen", "Parsed summary: $summary")
        summary
    } catch (e: Exception) {
        Log.e("ResultScreen", "Error parsing summary JSON: ${e.message}", e)
        // Fallback values in case of JSON parsing error
        BenchmarkSummary(
            singleCoreScore = 0.0,
            multiCoreScore = 0.0,
            finalScore = 0.0,
            normalizedScore = 0.0,
            rating = "★",
            detailedResults = emptyList()
        )
    }

    FinalBenchmark2Theme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()) // Make the entire screen scrollable
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    text = "Benchmark Complete!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                // Results Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Single Core Score
                        ScoreItem(
                            title = "Single-Core Score",
                            value = String.format("%.2f", summary.singleCoreScore)
                        )
                        
                        // Multi Core Score
                        ScoreItem(
                            title = "Multi-Core Score",
                            value = String.format("%.2f", summary.multiCoreScore)
                        )
                        
                        // Core Ratio (Multi-Core / Single-Core as ratio, not percentage)
                        val coreRatio = if (summary.singleCoreScore > 0) {
                            summary.multiCoreScore / summary.singleCoreScore
                        } else {
                            0.0
                        }
                        ScoreItem(
                            title = "Core Ratio (MC:SC)",
                            value = String.format("%.2fx", coreRatio)
                        )
                        
                        // Final Weighted Score
                        ScoreItem(
                            title = "Final Weighted Score",
                            value = String.format("%.2f", summary.finalScore)
                        )
                        
                        // Normalized Score
                        ScoreItem(
                            title = "Normalized Score",
                            value = String.format("%.2f", summary.normalizedScore)
                        )
                        
                        // Rating
                        Text(
                            text = "Rating",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                        Text(
                            text = summary.rating,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // Button to navigate to detailed results screen
                Button(
                    onClick = {
                        onShowDetailedResults(summary.detailedResults)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text("View All Detailed Results")
                }
                
                // Action Buttons - Now properly positioned to remain visible when scrolled
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = onRunAgain,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Run Again")
                    }
                    
                    Button(
                        onClick = onBackToHome,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Back to Home")
                    }
                }
            }
        }
    }
}

// These functions are not needed as they're already imported from androidx.compose.foundation.lazy

@Composable
fun ScoreItem(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
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