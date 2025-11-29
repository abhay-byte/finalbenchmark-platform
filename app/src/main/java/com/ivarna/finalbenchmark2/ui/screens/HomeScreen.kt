package com.ivarna.finalbenchmark2.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStartBenchmark: (String) -> Unit
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