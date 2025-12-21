package com.ivarna.finalbenchmark2.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.ivarna.finalbenchmark2.utils.OnboardingPreferences
import androidx.compose.ui.graphics.Brush

sealed interface PermissionUiState {
    object Checking : PermissionUiState
    object NotGranted : PermissionUiState
    object Granted : PermissionUiState
    object NotRequired : PermissionUiState
}

@Composable
fun PermissionsScreen(
        onNextClicked: () -> Unit,
        onBackClicked: () -> Unit = {},
        modifier: Modifier = Modifier
) {
    var permissionUiState by remember {
        mutableStateOf<PermissionUiState>(PermissionUiState.Checking)
    }
    val context = LocalContext.current
    val onboardingPreferences = remember { OnboardingPreferences(context) }

    // Check permission status when screen loads
    val hasPermissionChecked = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!hasPermissionChecked.value) {
            permissionUiState =
                    if (Build.VERSION.SDK_INT >= 33) {
                        val hasPermission =
                                ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                ) == PackageManager.PERMISSION_GRANTED

                        if (hasPermission) {
                            PermissionUiState.Granted
                        } else {
                            PermissionUiState.NotGranted
                        }
                    } else {
                        PermissionUiState.NotRequired
                    }
            hasPermissionChecked.value = true
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                    )
                )
            )
    ) {
        Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Central content
            Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f)
            ) {
                // Icon Container
                com.ivarna.finalbenchmark2.ui.components.GlassCard(
                    modifier = Modifier.size(100.dp),
                    shape = RoundedCornerShape(50.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                                imageVector = Icons.Rounded.Notifications,
                                contentDescription = "Notifications Permission",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Headline
                Text(
                        text = "Stay Running",
                        style =
                                MaterialTheme.typography.headlineLarge.copy(
                                        fontWeight = FontWeight.Bold
                                ),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Subtext
                Text(
                        text =
                                "Benchmarks are heavy operations. Without proper permissions, the OS might kill the app before tests finish.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Explanation Card
                com.ivarna.finalbenchmark2.ui.components.GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                                text = "Why do we need this?",
                                style =
                                        MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.SemiBold
                                        ),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Start
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                                text =
                                        "We use a Foreground Service to mark the benchmark process as 'User Visible'. This prevents Android from killing the app to save battery while high-intensity tests are running. This requires Notification permissions to display the 'Benchmark Running' status.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Start
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Permission Status and Action
                when (permissionUiState) {
                    is PermissionUiState.Checking -> {
                        CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = MaterialTheme.colorScheme.primary
                        )
                    }
                    is PermissionUiState.NotGranted -> {
                        val permissionLauncher =
                                rememberLauncherForActivityResult(
                                        ActivityResultContracts.RequestPermission()
                                ) { isGranted ->
                                    permissionUiState =
                                            if (isGranted) {
                                                PermissionUiState.Granted
                                            } else {
                                                PermissionUiState.NotGranted
                                            }
                                }

                        Button(
                                onClick = {
                                    permissionLauncher.launch(
                                            Manifest.permission.POST_NOTIFICATIONS
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                    text = "Allow Notifications",
                                    style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    is PermissionUiState.Granted -> {
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                    imageVector = Icons.Rounded.Notifications,
                                    contentDescription = "Permission Granted",
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(24.dp)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                    text = "Permission Granted",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFF4CAF50)
                            )
                        }
                    }
                    is PermissionUiState.NotRequired -> {
                        Text(
                                text = "Auto-Granted (Android < 13)",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Action Button
            Button(
                    onClick = { onNextClicked() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors =
                            ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                            ),
                    shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                        text = "Next",
                        style =
                                MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                )
                )
            }
        }
    }
}
