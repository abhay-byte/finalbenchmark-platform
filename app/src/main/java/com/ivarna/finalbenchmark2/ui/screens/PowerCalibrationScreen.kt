package com.ivarna.finalbenchmark2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivarna.finalbenchmark2.utils.OnboardingPreferences
import com.ivarna.finalbenchmark2.utils.PowerConsumptionPreferences
import com.ivarna.finalbenchmark2.utils.PowerUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PowerCalibrationScreen(
        onNextClicked: () -> Unit,
        onBackClicked: () -> Unit = {},
        modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val powerPreferences = remember { PowerConsumptionPreferences(context) }
    val powerUtils = remember { PowerUtils(context) }
    var selectedMultiplier by remember { mutableStateOf(powerPreferences.getMultiplier()) }
    var currentPowerInfo by remember { mutableStateOf(powerUtils.getPowerConsumptionInfo()) }
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var hasCalibrated by remember { mutableStateOf(false) }

    // Save multiplier immediately when changed and update power reading
    LaunchedEffect(selectedMultiplier) {
        powerPreferences.setMultiplier(selectedMultiplier)
        // Update power reading immediately after multiplier change
        currentPowerInfo = powerUtils.getPowerConsumptionInfo()
    }

    // Auto-calibration on first load
    LaunchedEffect(Unit) {
        if (!hasCalibrated) {
            isLoading = true
            statusMessage = "Calibrating power sensor..."
            val calibratedMultiplier = powerPreferences.autoSelectMultiplier { progress ->
                statusMessage = progress
            }
            selectedMultiplier = calibratedMultiplier
            isLoading = false
            hasCalibrated = true
        }

        while (true) {
            delay(1000)
            currentPowerInfo = powerUtils.getPowerConsumptionInfo()
        }
    }

    // Define multiplier options
    val multiplierOptions =
            listOf("0.01x" to 0.01f, "0.1x" to 0.1f, "1.0x" to 1.0f, "10x" to 10f, "100x" to 100f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
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
            Spacer(modifier = Modifier.height(32.dp))
            
            // Header Section
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Icon Container
                com.ivarna.finalbenchmark2.ui.components.GlassCard(
                    modifier = Modifier.size(100.dp),
                    shape = RoundedCornerShape(50.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                                imageVector = Icons.Rounded.Bolt,
                                contentDescription = "Power Calibration",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Headline
                Text(
                        text = "Calibrate Power",
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
                        text = "Select a multiplier to ensure your power readings are accurate.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                )
            }

            // Current Reading Display (without card wrapper)
            Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 24.dp)
            ) {
                Text(
                        text = "Current Reading",
                        style =
                                MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                        text = "${String.format("%.2f", currentPowerInfo.power)}W",
                        style =
                                MaterialTheme.typography.displayMedium.copy(
                                        fontWeight = FontWeight.Bold
                                ),
                        color =
                                if (currentPowerInfo.power >= 0) {
                                    MaterialTheme.colorScheme.tertiary
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                        textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                        text = if (currentPowerInfo.power >= 0) "Charging" else "Discharging",
                        style =
                                MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium
                                ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Additional power details
                Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                            text = "${String.format("%.1f", currentPowerInfo.voltage)}V",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                    )
                    Text(
                            text =
                                    "${String.format("%.1f", kotlin.math.abs(currentPowerInfo.current))}A",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                        text = "If reading looks wrong, try a different multiplier",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                )
            }

            // Multiplier Selection
            Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 16.dp)
            ) {
                Text(
                        text = "Select Multiplier",
                        style =
                                MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                ),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                )

                FlowRow(
                        horizontalArrangement =
                                Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                ) {
                    multiplierOptions.forEach { (label, multiplier) ->
                        val isSelected = selectedMultiplier == multiplier
                        FilterChip(
                                onClick = { selectedMultiplier = multiplier },
                                label = {
                                    Text(
                                            text = label,
                                            style =
                                                    MaterialTheme.typography.labelLarge.copy(
                                                            fontWeight =
                                                                    if (isSelected)
                                                                            FontWeight.SemiBold
                                                                    else FontWeight.Normal
                                                    )
                                    )
                                },
                                selected = isSelected,
                                colors =
                                        FilterChipDefaults.filterChipColors(
                                                selectedContainerColor =
                                                        MaterialTheme.colorScheme.primaryContainer
                                                                .copy(alpha = 0.6f),
                                                selectedLabelColor =
                                                        MaterialTheme.colorScheme.primary,
                                                containerColor = MaterialTheme.colorScheme.surface,
                                                labelColor =
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                border =
                                        if (isSelected) {
                                            FilterChipDefaults.filterChipBorder(
                                                    enabled = true,
                                                    selected = true,
                                                    borderWidth = 2.dp,
                                                    borderColor = MaterialTheme.colorScheme.primary
                                            )
                                        } else {
                                            FilterChipDefaults.filterChipBorder(
                                                    enabled = true,
                                                    selected = false,
                                                    borderWidth = 1.dp,
                                                    borderColor =
                                                            MaterialTheme.colorScheme.outline.copy(
                                                                    alpha = 0.3f
                                                            )
                                            )
                                        }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Current selection display
                Text(
                        text = "Selected: ${selectedMultiplier}x multiplier",
                        style =
                                MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Medium
                                ),
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                )
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = statusMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                // Action Button - Simple style
                Button(
                    onClick = {
                        scope.launch {
                            // Save the selected multiplier
                            powerPreferences.setMultiplier(selectedMultiplier)

                            // Mark onboarding as completed
                            val onboardingPreferences = OnboardingPreferences(context)
                            onboardingPreferences.setOnboardingCompleted()
                        }
                        onNextClicked()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Finish Setup",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}
