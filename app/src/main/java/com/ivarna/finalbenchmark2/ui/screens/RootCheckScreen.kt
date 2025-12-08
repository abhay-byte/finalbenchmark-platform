package com.ivarna.finalbenchmark2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivarna.finalbenchmark2.utils.OnboardingPreferences
import com.ivarna.finalbenchmark2.utils.RootAccessManager
import com.ivarna.finalbenchmark2.utils.RootUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
    onBackClicked: () -> Unit = {},
    onRetryRootAccess: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var rootUiState by remember { mutableStateOf<RootUiState>(RootUiState.Checking) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val onboardingPreferences = remember { OnboardingPreferences(context) }
    val isDarkTheme = isSystemInDarkTheme()

    // Perform automatic root check when screen loads
    val hasRootChecked = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!hasRootChecked.value) {
            scope.launch {
                // First check if we have a cached result
                val cachedResult = RootAccessManager.getCachedRootAccess()
                val hasRoot = if (cachedResult != null) {
                    cachedResult
                } else {
                    // No cached result, perform the check and cache it
                    withContext(Dispatchers.IO) {
                        RootAccessManager.hasRootAccess()
                    }
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isDarkTheme) {
                        listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    } else {
                        listOf(
                            Color(0xFFFFFFFF),
                            Color(0xFFF5F5F5)
                        )
                    }
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Navigation Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClicked) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                TextButton(onClick = onNextClicked) {
                    Text(
                        text = "Skip",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Central content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                // Icon Container with status color
                val iconColor = when (rootUiState) {
                    is RootUiState.Checking -> MaterialTheme.colorScheme.onSurfaceVariant
                    is RootUiState.Granted -> Color(0xFF4CAF50)
                    is RootUiState.NotAvailable -> Color(0xFFFF9800)
                }

                Card(
                    modifier = Modifier.size(100.dp),
                    shape = RoundedCornerShape(50.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Security,
                            contentDescription = "Root Access Status",
                            modifier = Modifier.size(48.dp),
                            tint = iconColor
                        )
                    }
                }

                // Separate loading indicator
                if (rootUiState is RootUiState.Checking) {
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Headline
                val headlineText = when (rootUiState) {
                    is RootUiState.Checking -> "Checking Root Access..."
                    is RootUiState.Granted -> "Root Access Granted"
                    is RootUiState.NotAvailable -> "Root Access Not Detected"
                }

                val headlineColor = when (rootUiState) {
                    is RootUiState.Checking -> MaterialTheme.colorScheme.onSurface
                    is RootUiState.Granted -> Color(0xFF4CAF50)
                    is RootUiState.NotAvailable -> Color(0xFFFF9800)
                }

                Text(
                    text = headlineText,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = headlineColor,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Status Information Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                            rootUiState = RootUiState.Checking
                            // Reset the cached result to force a new check
                            RootAccessManager.reset()
                            scope.launch {
                                delay(500)
                                val hasRoot = withContext(Dispatchers.IO) {
                                    RootAccessManager.hasRootAccess()
                                }
                                rootUiState = if (hasRoot) {
                                    RootUiState.Granted
                                } else {
                                    RootUiState.NotAvailable
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Request Root Again",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            // Action Button
            Button(
                onClick = { onNextClicked() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .shadow(8.dp, RoundedCornerShape(16.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Next",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
                    ),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}
