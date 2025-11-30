package com.ivarna.finalbenchmark2.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivarna.finalbenchmark2.ui.theme.FinalBenchmark2Theme
import com.ivarna.finalbenchmark2.utils.DeviceInfoCollector
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

// Utility functions unique to DeviceScreen
fun calculateScreenSize(displayMetrics: android.util.DisplayMetrics): Double {
    val widthInches = displayMetrics.widthPixels / displayMetrics.xdpi
    val heightInches = displayMetrics.heightPixels / displayMetrics.ydpi
    return kotlin.math.sqrt((widthInches * widthInches + heightInches * heightInches).toDouble())
}

fun calculateAspectRatio(width: Int, height: Int): String {
    val gcd = gcd(width, height)
    val aspectWidth = width / gcd
    val aspectHeight = height / gcd
    return "$aspectWidth:$aspectHeight"
}

fun gcd(a: Int, b: Int): Int {
    return if (b == 0) a else gcd(b, a % b)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceScreen() {
    val context = LocalContext.current
    val deviceInfo = DeviceInfoCollector.getDeviceInfo(context)
    
    val tabs = listOf(
        "Device Info",
        "CPU",
        "GPU",
        "Screen",
        "OS",
        "Hardware",
        "Sensors"
    )
    
    var selectedTabIndex by remember { mutableStateOf(0) }
    
    FinalBenchmark2Theme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Scrollable Tab Row - attached to top
                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier
                                .tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    text = title,
                                    maxLines = 1,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        )
                    }
                }
                
                // Tab Content with top padding
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(top = 8.dp)  // Add spacing at top of each tab view
                ) {
                    when (selectedTabIndex) {
                        0 -> DeviceInfoTab(deviceInfo)
                        1 -> CpuTab(deviceInfo)
                        2 -> GpuTab(deviceInfo)
                        3 -> ScreenTab(context)
                        4 -> OsTab(deviceInfo)
                        5 -> HardwareTab(deviceInfo)
                        6 -> SensorsTab(context)
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceInfoTab(deviceInfo: com.ivarna.finalbenchmark2.utils.DeviceInfo) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {
        // Header
        Text(
            text = "Device Information",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )
                
        // Device Model and Manufacturer
        DeviceInfoCard("Device Information") {
            InfoRow("Model", deviceInfo.deviceModel)
            InfoRow("Manufacturer", deviceInfo.manufacturer)
            InfoRow("Board", deviceInfo.board)
            InfoRow("SoC", deviceInfo.socName)
            InfoRow("Architecture", deviceInfo.cpuArchitecture)
        }
                
        Spacer(modifier = Modifier.height(16.dp))
                
        // CPU Information
        DeviceInfoCard("CPU Information") {
            InfoRow("Total Cores", deviceInfo.totalCores.toString())
            InfoRow("Big Cores", deviceInfo.bigCores.toString())
            InfoRow("Small Cores", deviceInfo.smallCores.toString())
            InfoRow("Cluster Topology", deviceInfo.clusterTopology)
                    
            // CPU Frequencies
            Text(
                text = "CPU Frequencies",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
            deviceInfo.cpuFrequencies.forEach { (core, freq) ->
                InfoRow("Core $core", freq)
            }
        }
                
        Spacer(modifier = Modifier.height(16.dp))
                
        // GPU Information
        DeviceInfoCard("GPU Information") {
            InfoRow("Model", deviceInfo.gpuModel)
            InfoRow("Vendor", deviceInfo.gpuVendor)
        }
                
        Spacer(modifier = Modifier.height(16.dp))
                
        // Memory Information
        DeviceInfoCard("Memory Information") {
            InfoRow("Total RAM", formatBytes(deviceInfo.totalRam))
            InfoRow("Available RAM", formatBytes(deviceInfo.availableRam))
        }
                
        Spacer(modifier = Modifier.height(16.dp))
                
        // Storage Information
        DeviceInfoCard("Storage Information") {
            InfoRow("Total Storage", formatBytes(deviceInfo.totalStorage))
            InfoRow("Free Storage", formatBytes(deviceInfo.freeStorage))
        }
                
        Spacer(modifier = Modifier.height(16.dp))
                
        // System Information
        DeviceInfoCard("System Information") {
            InfoRow("Android Version", "${deviceInfo.androidVersion} (API ${deviceInfo.apiLevel})")
            InfoRow("Kernel Version", deviceInfo.kernelVersion)
            InfoRow("Thermal Status", deviceInfo.thermalStatus ?: "Not available")
            InfoRow("Battery Temperature", deviceInfo.batteryTemperature?.let { "${String.format("%.2f", it)}°C" } ?: "Not available")
            InfoRow("Battery Capacity", deviceInfo.batteryCapacity?.let { "${it.toInt()}%" } ?: "Not available")
        }
    }
}

@Composable
fun CpuTab(deviceInfo: com.ivarna.finalbenchmark2.utils.DeviceInfo) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "CPU Information",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )
                
        DeviceInfoCard("CPU Architecture") {
            InfoRow("Architecture", deviceInfo.cpuArchitecture)
            InfoRow("Total Cores", deviceInfo.totalCores.toString())
            InfoRow("Big Cores", deviceInfo.bigCores.toString())
            InfoRow("Small Cores", deviceInfo.smallCores.toString())
            InfoRow("Cluster Topology", deviceInfo.clusterTopology)
        }
                
        Spacer(modifier = Modifier.height(16.dp))
                
        DeviceInfoCard("CPU Frequencies") {
            deviceInfo.cpuFrequencies.forEach { (core, freq) ->
                InfoRow("Core $core", freq)
            }
        }
                
        Spacer(modifier = Modifier.height(16.dp))
                
        DeviceInfoCard("CPU Performance") {
            // Placeholder for CPU performance metrics
            InfoRow("Performance Class", "To be implemented")
            InfoRow("Instruction Sets", "To be implemented")
        }
    }
}

@Composable
fun GpuTab(deviceInfo: com.ivarna.finalbenchmark2.utils.DeviceInfo) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "GPU Information",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )
                
        DeviceInfoCard("GPU Details") {
            InfoRow("Model", deviceInfo.gpuModel)
            InfoRow("Vendor", deviceInfo.gpuVendor)
        }
                
        Spacer(modifier = Modifier.height(16.dp))
                
        DeviceInfoCard("GPU Capabilities") {
            // Placeholder for GPU capabilities
            InfoRow("OpenGL Version", "To be implemented")
            InfoRow("Vulkan Support", "To be implemented")
            InfoRow("Compute Units", "To be implemented")
        }
                
        Spacer(modifier = Modifier.height(16.dp))
                
        DeviceInfoCard("GPU Performance") {
            // Placeholder for GPU performance metrics
            InfoRow("Graphics Score", "To be implemented")
            InfoRow("Compute Score", "To be implemented")
        }
    }
}

@Composable
fun ScreenTab(context: android.content.Context) {
    val displayMetrics = context.resources.displayMetrics
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Screen Information",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )
                
        DeviceInfoCard("Display Metrics") {
            InfoRow("Width", "${displayMetrics.widthPixels}px")
            InfoRow("Height", "${displayMetrics.heightPixels}px")
            InfoRow("Density", "${displayMetrics.density}x (${displayMetrics.densityDpi} dpi)")
            InfoRow("Size", "${String.format("%.1f", calculateScreenSize(displayMetrics))}\"")
        }
                
        Spacer(modifier = Modifier.height(16.dp))
                
        DeviceInfoCard("Screen Properties") {
            InfoRow("Refresh Rate", "To be implemented")
            InfoRow("Resolution", "${displayMetrics.widthPixels} x ${displayMetrics.heightPixels}")
            InfoRow("Aspect Ratio", calculateAspectRatio(displayMetrics.widthPixels, displayMetrics.heightPixels))
        }
                
        Spacer(modifier = Modifier.height(16.dp))
                
        DeviceInfoCard("Touch Information") {
            // Placeholder for touch-related info
            InfoRow("Touch Points", "To be implemented")
            InfoRow("Touch Technology", "To be implemented")
        }
    }
}

@Composable
fun OsTab(deviceInfo: com.ivarna.finalbenchmark2.utils.DeviceInfo) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Operating System Information",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )
                
        DeviceInfoCard("OS Version") {
            InfoRow("Android Version", deviceInfo.androidVersion)
            InfoRow("API Level", deviceInfo.apiLevel.toString())
            InfoRow("Security Patch", "To be implemented")
        }
                
        Spacer(modifier = Modifier.height(16.dp))
                
        DeviceInfoCard("System Properties") {
            InfoRow("Kernel Version", deviceInfo.kernelVersion)
            InfoRow("Build Number", android.os.Build.DISPLAY)
            InfoRow("Build Fingerprint", android.os.Build.FINGERPRINT)
        }
                
        Spacer(modifier = Modifier.height(16.dp))
                
        DeviceInfoCard("System Features") {
            InfoRow("SELinux Status", "To be implemented")
            InfoRow("OTA Updates", "To be implemented")
        }
    }
}

@Composable
fun HardwareTab(deviceInfo: com.ivarna.finalbenchmark2.utils.DeviceInfo) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Hardware Information",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )
                
        DeviceInfoCard("Main Hardware") {
            InfoRow("Model", deviceInfo.deviceModel)
            InfoRow("Manufacturer", deviceInfo.manufacturer)
            InfoRow("Board", deviceInfo.board)
            InfoRow("SoC", deviceInfo.socName)
        }
                
        Spacer(modifier = Modifier.height(16.dp))
                
        DeviceInfoCard("Memory") {
            InfoRow("Total RAM", formatBytes(deviceInfo.totalRam))
            InfoRow("Available RAM", formatBytes(deviceInfo.availableRam))
            InfoRow("Storage Total", formatBytes(deviceInfo.totalStorage))
            InfoRow("Storage Free", formatBytes(deviceInfo.freeStorage))
        }
                
        Spacer(modifier = Modifier.height(16.dp))
                
        DeviceInfoCard("Power & Thermal") {
            InfoRow("Battery Capacity", deviceInfo.batteryCapacity?.let { "${it.toInt()}%" } ?: "Not available")
            InfoRow("Battery Temperature", deviceInfo.batteryTemperature?.let { "${String.format("%.2f", it)}°C" } ?: "Not available")
            InfoRow("Thermal Status", deviceInfo.thermalStatus ?: "Not available")
        }
    }
}

@Composable
fun SensorsTab(context: android.content.Context) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Sensors Information",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )
                
        DeviceInfoCard("Available Sensors") {
            // Placeholder for sensor information
            InfoRow("Accelerometer", "To be implemented")
            InfoRow("Gyroscope", "To be implemented")
            InfoRow("Magnetometer", "To be implemented")
            InfoRow("Proximity", "To be implemented")
            InfoRow("Ambient Light", "To be implemented")
        }
                
        Spacer(modifier = Modifier.height(16.dp))
                
        DeviceInfoCard("Sensor Capabilities") {
            // Placeholder for sensor capabilities
            InfoRow("Max Sensors", "To be implemented")
            InfoRow("Highest Precision", "To be implemented")
        }
    }
}

@Composable
fun DeviceInfoCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            content()
        }
    }
}