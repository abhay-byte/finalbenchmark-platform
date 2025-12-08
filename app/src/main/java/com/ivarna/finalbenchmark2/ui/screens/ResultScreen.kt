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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.platform.LocalDensity
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
        Log.d("ResultScreen", "Raw JSON: $summaryJson")
        
        if (summaryJson.isBlank()) {
            Log.e("ResultScreen", "Received empty JSON string!")
            throw IllegalArgumentException("Empty JSON string")
        }
        
        val jsonObject = JSONObject(summaryJson)
        val detailedResults = mutableListOf<BenchmarkResult>()
        
        // Log the JSON structure for debugging
        Log.d("ResultScreen", "JSON keys: ${jsonObject.keys().asSequence().toList()}")
        
        // Parse detailed results if available
        val detailedResultsArray = jsonObject.optJSONArray("detailed_results")
        if (detailedResultsArray != null) {
            Log.d("ResultScreen", "Found detailed_results array with ${detailedResultsArray.length()} items")
            for (i in 0 until detailedResultsArray.length()) {
                val resultObj = detailedResultsArray.getJSONObject(i)
                val result = BenchmarkResult(
                    name = resultObj.optString("name", "Unknown"),
                    executionTimeMs = resultObj.optDouble("executionTimeMs", 0.0),
                    opsPerSecond = resultObj.optDouble("opsPerSecond", 0.0),
                    isValid = resultObj.optBoolean("isValid", false),
                    metricsJson = resultObj.optString("metricsJson", "{}")
                )
                detailedResults.add(result)
                Log.d("ResultScreen", "Parsed result $i: ${result.name} - ${result.opsPerSecond} ops/sec (valid: ${result.isValid})")
            }
        } else {
            Log.w("ResultScreen", "No detailed_results array found in JSON")
        }
        
        val singleCoreScore = jsonObject.optDouble("single_core_score", 0.0)
        val multiCoreScore = jsonObject.optDouble("multi_core_score", 0.0)
        val finalScore = jsonObject.optDouble("final_score", 0.0)
        val normalizedScore = jsonObject.optDouble("normalized_score", 0.0)
        
        Log.d("ResultScreen", "Scores - Single: $singleCoreScore, Multi: $multiCoreScore, Final: $finalScore, Normalized: $normalizedScore")
        
        // Validate that we have reasonable scores
        if (singleCoreScore == 0.0 && multiCoreScore == 0.0 && finalScore == 0.0) {
            Log.w("ResultScreen", "All scores are 0.0 - this might indicate a benchmark failure")
        }
        
        val summary = BenchmarkSummary(
            singleCoreScore = singleCoreScore,
            multiCoreScore = multiCoreScore,
            finalScore = finalScore,
            normalizedScore = normalizedScore,
            detailedResults = detailedResults
        )
        Log.d("ResultScreen", "Successfully parsed summary: $summary")
        summary
    } catch (e: Exception) {
        Log.e("ResultScreen", "Error parsing summary JSON: ${e.message}", e)
        Log.e("ResultScreen", "Failed JSON content: $summaryJson")
        // Fallback values in case of JSON parsing error
        BenchmarkSummary(
            singleCoreScore = 0.0,
            multiCoreScore = 0.0,
            finalScore = 0.0,
            normalizedScore = 0.0,
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
                    .padding(
                        top = WindowInsets.statusBars.getTop(LocalDensity.current).dp,
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 16.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))
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