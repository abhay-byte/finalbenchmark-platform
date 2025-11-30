package com.ivarna.finalbenchmark2.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivarna.finalbenchmark2.R
import com.ivarna.finalbenchmark2.ui.theme.FinalBenchmark2Theme
import com.ivarna.finalbenchmark2.utils.TemperatureUtils
import com.ivarna.finalbenchmark2.utils.PowerUtils
import com.ivarna.finalbenchmark2.utils.CpuUtilizationUtils
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStartBenchmark: (String) -> Unit,
    onNavigateToSettings: () -> Unit = {}
) {
    var selectedPreset by remember { mutableStateOf("Auto") }
    val presets = listOf("Auto", "Slow", "Mid", "Flagship")
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
                verticalArrangement = Arrangement.Center
            ) {
                // App logo - Fixed to be perfectly circular
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp)
                        .padding(bottom = 16.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "FinalBenchmark2 Logo",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                
                // App name
                Text(
                    text = "FinalBenchmark2",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Description
                Text(
                    text = "A comprehensive benchmarking application that tests your device's performance across multiple components.",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Temperature Card
                val context = LocalContext.current
                val tempUtils = remember { TemperatureUtils(context) }
                var cpuTemp by remember { mutableStateOf(-1f) }
                var batteryTemp by remember { mutableStateOf(-1f) }
                var isInitialized by remember { mutableStateOf(false) }
                
                LaunchedEffect(Unit) {
                    isInitialized = true
                    // Initial readings
                    cpuTemp = tempUtils.getCpuTemperature()
                    batteryTemp = tempUtils.getBatteryTemperature()
                    
                    // Update every 5 seconds
                    while (true) {
                        delay(5000)
                        cpuTemp = tempUtils.getCpuTemperature()
                        batteryTemp = tempUtils.getBatteryTemperature()
                    }
                }
                
                if (isInitialized) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.mobile_24), // Using a generic icon
                                    contentDescription = "Temperature",
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Device Temperatures",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "CPU: ${if (cpuTemp > 0) "${cpuTemp}°C" else "N/A"}",
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Battery: ${if (batteryTemp > 0) "${batteryTemp}°C" else "N/A"}",
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                
                // CPU Utilization Card - Added below temperature card
                val cpuUtilizationUtils = remember { CpuUtilizationUtils(context) }
                var cpuUtilization by remember { mutableStateOf(0f) }
                var cpuUtilizationInitialized by remember { mutableStateOf(false) }
                var isExpanded by remember { mutableStateOf(false) }
                
                LaunchedEffect(Unit) {
                    cpuUtilizationInitialized = true
                    
                    // Update every 1 second
                    while (true) {
                        delay(1000)
                        cpuUtilization = cpuUtilizationUtils.getCpuUtilizationPercentage()
                    }
                }
                
                if (cpuUtilizationInitialized) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable { isExpanded = !isExpanded },
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Start,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.cpu_24),
                                        contentDescription = "CPU Utilization",
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "CPU Utilization",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Icon(
                                    painter = painterResource(id = R.drawable.arrow_expand_24),
                                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                                    modifier = Modifier
                                        .size(24.dp)
                                        .rotate(if (isExpanded) 180f else 0f)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${String.format("%.1f", cpuUtilization)}%",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Current Usage",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            // Expanded content
                            if (isExpanded) {
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                // Get core utilization data
                                val coreUtilizations = remember { cpuUtilizationUtils.getCoreUtilizationPercentages() }
                                val allCoreFrequencies = remember { cpuUtilizationUtils.getAllCoreFrequencies() }
                                
                                // Display core utilization in a compact grid
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(4), // Show 4 cores in one row
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 300.dp) // Limit the height to avoid infinite constraints
                                ) {
                                    items(coreUtilizations.size) { index ->
                                        val coreIndex = index
                                        val utilization = coreUtilizations[coreIndex] ?: 0f
                                        val (currentFreq, maxFreq) = allCoreFrequencies[coreIndex] ?: Pair(0L, 0L)
                                        
                                        // Circular progress indicator for each core
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(4.dp) // Reduced padding
                                        ) {
                                            // Circular progress indicator
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.size(60.dp) // Reduced size
                                            ) {
                                                CircularProgressIndicator(
                                                    progress = { utilization / 100f },
                                                    modifier = Modifier.size(60.dp), // Reduced size
                                                    strokeWidth = 4.dp, // Reduced stroke width
                                                    color = MaterialTheme.colorScheme.primary,
                                                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                                )
                                                
                                                // Center text
                                                Text(
                                                    text = "${String.format("%.0f", utilization)}%",
                                                    fontSize = 10.sp, // Reduced font size
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            
                                            Spacer(modifier = Modifier.height(2.dp)) // Reduced space
                                            
                                            Text(
                                                text = "Core $coreIndex",
                                                fontSize = 10.sp, // Reduced font size
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            
                                            Text(
                                                text = "${currentFreq}M",
                                                fontSize = 8.sp, // Reduced font size
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                }
                
                // Power Consumption Card - Added below temperature card
                val powerUtils = remember { PowerUtils(context) }
                var powerConsumptionInfo by remember { mutableStateOf(powerUtils.getPowerConsumptionInfo()) }
                var powerConsumptionInitialized by remember { mutableStateOf(false) }
                
                LaunchedEffect(Unit) {
                    powerConsumptionInitialized = true
                    
                    // Update every 1 second
                    while (true) {
                        delay(1000)
                        powerConsumptionInfo = powerUtils.getPowerConsumptionInfo()
                    }
                }
                
                if (powerConsumptionInitialized) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable { onNavigateToSettings() }, // Make the card clickable
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.power_consumption_24),
                                    contentDescription = "Power Consumption",
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Power Consumption",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            // Power row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (powerConsumptionInfo.power != 0f) "${String.format("%.2f", powerConsumptionInfo.power)} W" else "N/A",
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (powerConsumptionInfo.power > 0) "Charging" else if (powerConsumptionInfo.power < 0) "Discharging" else "Current Usage",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            // Voltage and Current row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${String.format("%.2f", powerConsumptionInfo.voltage)} V",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${String.format("%.2f", powerConsumptionInfo.current)} A",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                // Preset Selection Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.mobile_24), 
                                contentDescription = "Benchmark Preset",
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Benchmark Preset",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        var expanded by remember { mutableStateOf(false) }
                        
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            TextField(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                value = selectedPreset,
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("Select Preset") },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Dropdown arrow"
                                    )
                                },
                                supportingText = {
                                    Text(
                                        text = when (selectedPreset) {
                                            "Auto" -> "Automatically detected based on device capabilities"
                                            "Slow" -> "Low-intensity workload for older/budget devices"
                                            "Mid" -> "Medium-intensity workload for standard devices"
                                            "Flagship" -> "High-intensity workload for premium devices"
                                            else -> ""
                                        }
                                    )
                                }
                            )
                            
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                presets.forEach { preset ->
                                    DropdownMenuItem(
                                        text = { 
                                            Text(
                                                text = when (preset) {
                                                    "Auto" -> "Auto (Recommended)"
                                                    "Slow" -> "Slow - Budget Devices"
                                                    "Mid" -> "Mid - Standard Devices"
                                                    "Flagship" -> "Flagship - Premium Devices"
                                                    else -> preset
                                                }
                                            ) 
                                        },
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
                ) {
                    Text(
                        text = "Start Benchmark",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Additional information
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