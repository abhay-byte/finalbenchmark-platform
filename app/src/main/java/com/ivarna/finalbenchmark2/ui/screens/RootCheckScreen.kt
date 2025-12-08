package com.ivarna.finalbenchmark2.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.ivarna.finalbenchmark2.utils.RootUtils
import com.ivarna.finalbenchmark2.utils.OnboardingPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface RootUiState {
    object Checking : RootUiState
    object Granted : RootUiState
    object NotAvailable : RootUiState
}

@Composable
fun RootCheckScreen(
    onNextClicked: () -> Unit,
    onRetryRootAccess: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var rootUiState by remember { mutableStateOf<RootUiState>(RootUiState.Checking) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val onboardingPreferences = remember { OnboardingPreferences(context) }
    
    // Perform automatic root check when screen loads, but only once
    // Using remember to cache the result so it doesn't re-run on recomposition
    val hasRootChecked = remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        if (!hasRootChecked.value) {
            scope.launch {
                val hasRoot = withContext(Dispatchers.IO) {
                    RootUtils.canExecuteRootCommandFast()
                }
                rootUiState = if (hasRoot) {
                    RootUiState.Granted
                } else {
                    RootUiState.NotAvailable
                }
                hasRootChecked.value = true
            }
        }
    }
    
    MaterialTheme {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Central content - centered vertically
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f)
                ) {
                    // Security Icon (no animation)
                    val iconColor = when (rootUiState) {
                        is RootUiState.Checking -> MaterialTheme.colorScheme.onSurfaceVariant
                        is RootUiState.Granted -> Color(0xFF4CAF50) // Green
                        is RootUiState.NotAvailable -> Color(0xFFFF9800) // Orange
                    }
                    
                    Icon(
                        imageVector = Icons.Rounded.Security,
                        contentDescription = "Root Access Status",
                        modifier = Modifier.size(120.dp),
                        tint = iconColor
                    )
                    
                    // Separate loading indicator
                    if (rootUiState is RootUiState.Checking) {
                        Spacer(modifier = Modifier.height(16.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Headline Text
                    val headlineText = when (rootUiState) {
                        is RootUiState.Checking -> "Checking Root Access..."
                        is RootUiState.Granted -> "Root Access Granted"
                        is RootUiState.NotAvailable -> "Root Access Not Detected"
                    }
                    
                    val headlineColor = when (rootUiState) {
                        is RootUiState.Checking -> MaterialTheme.colorScheme.onSurfaceVariant
                        is RootUiState.Granted -> Color(0xFF4CAF50) // Green
                        is RootUiState.NotAvailable -> Color(0xFFFF9800) // Orange
                    }
                    
                    Text(
                        text = headlineText,
                        style = MaterialTheme.typography.headlineMedium,
                        color = headlineColor,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Status Information Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            val statusText = when (rootUiState) {
                                is RootUiState.Checking -> "Checking your device for root access..."
                                is RootUiState.Granted -> "Full Control Enabled. You have access to advanced features like CPU Affinity, GPU Frequency monitoring, and Governor tuning."
                                is RootUiState.NotAvailable -> "Running in Standard Mode. Benchmarks will function correctly, but detailed hardware stats (GPU Load, Exact Frequencies) and optimizations will be disabled."
                            }
                            
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                    
                    // "Request Root Again" button for manual retry
                    if (rootUiState is RootUiState.NotAvailable) {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = {
                                // Reset to checking state and retry
                                rootUiState = RootUiState.Checking
                                scope.launch {
                                    // Add a small delay to show checking state
                                    kotlinx.coroutines.delay(500)
                                    val hasRoot = withContext(Dispatchers.IO) {
                                        RootUtils.canExecuteRootCommandFast()
                                    }
                                    rootUiState = if (hasRoot) {
                                        RootUiState.Granted
                                    } else {
                                        RootUiState.NotAvailable
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Request Root Again",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
                
                // Action Button - anchored at bottom
                Button(
                    onClick = {
                        // Simply navigate to next screen - onboarding will be marked completed in theme selection
                        onNextClicked()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "Next",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}