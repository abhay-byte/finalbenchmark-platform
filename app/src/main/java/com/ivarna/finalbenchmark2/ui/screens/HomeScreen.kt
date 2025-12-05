package com.ivarna.finalbenchmark2.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivarna.finalbenchmark2.R
import com.ivarna.finalbenchmark2.ui.theme.FinalBenchmark2Theme
import com.ivarna.finalbenchmark2.ui.theme.LocalThemeMode
import com.ivarna.finalbenchmark2.ui.theme.ThemeMode
import com.ivarna.finalbenchmark2.utils.CpuUtilizationUtils
import com.ivarna.finalbenchmark2.utils.PowerUtils
import com.ivarna.finalbenchmark2.utils.TemperatureUtils
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStartBenchmark: (String) -> Unit,
    onNavigateToSettings: () -> Unit = {}
) {
    var selectedPreset by remember { mutableStateOf("Auto") }
    val presets = listOf("Auto", "Slow", "Mid", "Flagship")

    // --- State Holders for Data ---
    val context = LocalContext.current
    
    // Temperature State
    val tempUtils = remember { TemperatureUtils(context) }
    var cpuTemp by remember { mutableStateOf(0f) }
    var batteryTemp by remember { mutableStateOf(0f) }

    // CPU State
    val cpuUtilizationUtils = remember { CpuUtilizationUtils(context) }
    var cpuUtilization by remember { mutableStateOf(0f) }
    var coreUtilizations by remember { mutableStateOf<Map<Int, Float>>(emptyMap()) }
    var allCoreFrequencies by remember { mutableStateOf<Map<Int, Pair<Long, Long>>>(emptyMap()) }

    // Power State
    val powerUtils = remember { PowerUtils(context) }
    var powerInfo by remember { mutableStateOf(powerUtils.getPowerConsumptionInfo()) }

    var isDataInitialized by remember { mutableStateOf(false) }

    // Single LaunchedEffect to manage data polling loops
    LaunchedEffect(Unit) {
        isDataInitialized = true
        while (true) {
            // Update all data points
            cpuTemp = tempUtils.getCpuTemperature()
            batteryTemp = tempUtils.getBatteryTemperature()
            
            cpuUtilization = cpuUtilizationUtils.getCpuUtilizationPercentage()
            coreUtilizations = cpuUtilizationUtils.getCoreUtilizationPercentages()
            allCoreFrequencies = cpuUtilizationUtils.getAllCoreFrequencies()
            
            powerInfo = powerUtils.getPowerConsumptionInfo()
            
            delay(1000) // 1 second update rate
        }
    }

    FinalBenchmark2Theme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top // Changed to Top for better scrolling flow
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                
                // App logo
                val currentTheme by LocalThemeMode.current
                val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
                val logoBackgroundColor = when (currentTheme) {
                    ThemeMode.SKY_BREEZE, ThemeMode.LAVENDER_DREAM -> {
                        // Use a darker background for these specific light themes
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                    else -> {
                        if (isLightTheme) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent
                    }
                }
                
                Box(
                    modifier = Modifier
                        .size(140.dp) // Slightly smaller for better layout
                        .clip(CircleShape)
                        .background(logoBackgroundColor)
                        .padding(16.dp)
                        .padding(bottom = 8.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_2),
                        contentDescription = "Logo",
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(16.dp)) // Add spacing between logo and title

                Text(
                    text = "FinalBenchmark2",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "A comprehensive benchmarking application that tests your device's performance across multiple components.",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // =========================================================
                // CONSOLIDATED SYSTEM CARD
                // =========================================================
                if (isDataInitialized) {
                    var isExpanded by remember { mutableStateOf(false) }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable { isExpanded = !isExpanded },
                        shape = RoundedCornerShape(24.dp), // More modern rounded shape
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // --- SUMMARY ROW (Always Visible) ---
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 1. Temperature Summary
                                CompactStatItem(
                                    icon = Icons.Rounded.Thermostat,
                                    value = "${if(cpuTemp > 0) cpuTemp else "--"}°C",
                                    tint = MaterialTheme.colorScheme.error
                                )

                                // 2. CPU Load Summary
                                CompactStatItem(
                                    icon = Icons.Rounded.Memory,
                                    value = "${String.format("%.0f", cpuUtilization)}%",
                                    tint = MaterialTheme.colorScheme.primary
                                )

                                // 3. Power Summary
                                CompactStatItem(
                                    icon = Icons.Rounded.Bolt,
                                    value = "${String.format("%.1f", powerInfo.power)}W",
                                    tint = MaterialTheme.colorScheme.tertiary
                                )

                                // Expand Arrow
                                Icon(
                                    imageVector = Icons.Rounded.ArrowDropDown,
                                    contentDescription = "Expand",
                                    modifier = Modifier
                                        .size(28.dp)
                                        .rotate(if (isExpanded) 180f else 0f),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // --- EXPANDED DETAILS ---
                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = expandVertically(animationSpec = tween(300)) + fadeIn(),
                                exit = shrinkVertically(animationSpec = tween(300)) + fadeOut()
                            ) {
                                Column(modifier = Modifier.padding(top = 16.dp)) {
                                    
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.padding(bottom = 16.dp)
                                    )

                                    // Row 1: Detailed Stats Grid
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceAround
                                    ) {
                                        // CPU Temp
                                        DetailIconPair(Icons.Rounded.Memory, "${cpuTemp}°C", "CPU")
                                        // Battery Temp
                                        DetailIconPair(Icons.Rounded.BatteryStd, "${batteryTemp}°C", "Batt")
                                        // Voltage
                                        DetailIconPair(Icons.Rounded.ElectricBolt, "${String.format("%.1f", powerInfo.voltage)}V", "Volts")
                                        // Amperage
                                        DetailIconPair(Icons.Rounded.Bolt, "${String.format("%.1f", powerInfo.current)}A", "Amps")
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))

                                    // Row 2: CPU Cores Visualization
                                    Text(
                                        text = "Core Utilization",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
                                    )

                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(4),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 240.dp), // Limit height
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(coreUtilizations.size) { index ->
                                            val utilization = coreUtilizations[index] ?: 0f
                                            val (currentFreq, _) = allCoreFrequencies[index] ?: Pair(0L, 0L)
                                            
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    CircularProgressIndicator(
                                                        progress = { utilization / 100f },
                                                        modifier = Modifier.size(48.dp),
                                                        strokeWidth = 4.dp,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                                    )
                                                    Text(
                                                        text = "${index}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "${currentFreq / 1000} Mhz",
                                                    fontSize = 9.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                // =========================================================

                // Preset Selection Card
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Benchmark Configuration",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        var expanded by remember { mutableStateOf(false) }
                        
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                value = selectedPreset,
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("Performance Preset") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                            )
                            
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                presets.forEach { preset ->
                                    DropdownMenuItem(
                                        text = { Text(preset) },
                                        onClick = {
                                            selectedPreset = preset
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Start Benchmark Button
                Button(
                    onClick = { onStartBenchmark(selectedPreset) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(painterResource(id = R.drawable.mobile_24), contentDescription = null) // Generic icon
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "START BENCHMARK",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Text(
                    text = "Run comprehensive tests on CPU, GPU, RAM, and Storage performance",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

// --- Helper Composables for Clean UI ---

@Composable
fun CompactStatItem(icon: ImageVector, value: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun DetailIconPair(icon: ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 2.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}