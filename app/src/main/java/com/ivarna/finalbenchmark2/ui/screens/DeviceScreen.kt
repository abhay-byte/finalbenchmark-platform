package com.ivarna.finalbenchmark2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivarna.finalbenchmark2.ui.theme.FinalBenchmark2Theme
import com.ivarna.finalbenchmark2.utils.DeviceInfoCollector
import com.ivarna.finalbenchmark2.utils.formatBytes
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ivarna.finalbenchmark2.ui.components.CpuUtilizationGraph
import com.ivarna.finalbenchmark2.ui.components.GpuUtilizationGraph
import com.ivarna.finalbenchmark2.ui.components.PowerConsumptionGraph
import com.ivarna.finalbenchmark2.ui.viewmodels.DeviceViewModel
import com.ivarna.finalbenchmark2.ui.viewmodels.GpuInfoViewModel

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
fun DeviceScreen(viewModel: DeviceViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val context = LocalContext.current
    val deviceInfo by viewModel.deviceInfo.collectAsState()
    
    // Initialize the ViewModel with context
    LaunchedEffect(context) {
        viewModel.updateDeviceInfo(context)
        viewModel.initialize(context)
    }
    
    val tabs = listOf(
        "Info",
        "CPU",
        "GPU",
        "Memory",
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
                        0 -> InfoTab(deviceInfo, viewModel)
                        1 -> CpuTab(deviceInfo, viewModel)
                        2 -> GpuTab(deviceInfo)
                        3 -> MemoryTab(deviceInfo)
                        4 -> ScreenTab(context)
                        5 -> OsTab(deviceInfo)
                        6 -> HardwareTab(deviceInfo)
                        7 -> SensorsTab(context)
                    }
                }
            }
        }
    }
}

@Composable
fun InfoTab(deviceInfo: com.ivarna.finalbenchmark2.utils.DeviceInfo, viewModel: DeviceViewModel? = null) {
    val context = LocalContext.current
    val powerHistory by viewModel?.powerHistory?.collectAsState() ?:
        remember { mutableStateOf(emptyList<com.ivarna.finalbenchmark2.ui.components.PowerDataPoint>()) }
    
    // State for GPU info
    var gpuInfoState by remember {
        mutableStateOf<com.ivarna.finalbenchmark2.utils.GpuInfoState>(com.ivarna.finalbenchmark2.utils.GpuInfoState.Loading)
    }
    
    // Load GPU info
    LaunchedEffect(Unit) {
        val gpuInfoUtils = com.ivarna.finalbenchmark2.utils.GpuInfoUtils(context)
        gpuInfoState = gpuInfoUtils.getGpuInfo()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
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
        
        // Power Consumption Graph (at the top, scrolls with content)
        if (viewModel != null) {
            PowerConsumptionGraph(
                dataPoints = powerHistory,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Scrollable list for all device info
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // Device info items
            item {
                com.ivarna.finalbenchmark2.ui.components.InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Model", "${deviceInfo.manufacturer} ${deviceInfo.deviceModel}"),
                    isLastItem = false
                )
            }
            item {
                com.ivarna.finalbenchmark2.ui.components.InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Board", deviceInfo.board),
                    isLastItem = false
                )
            }
            item {
                com.ivarna.finalbenchmark2.ui.components.InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("SoC", deviceInfo.socName),
                    isLastItem = false
                )
            }
            item {
                com.ivarna.finalbenchmark2.ui.components.InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Architecture", deviceInfo.cpuArchitecture),
                    isLastItem = false
                )
            }
            item {
                com.ivarna.finalbenchmark2.ui.components.InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Total Cores", deviceInfo.totalCores.toString()),
                    isLastItem = false
                )
            }
            item {
                com.ivarna.finalbenchmark2.ui.components.InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Big Cores", deviceInfo.bigCores.toString()),
                    isLastItem = false
                )
            }
            item {
                com.ivarna.finalbenchmark2.ui.components.InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Small Cores", deviceInfo.smallCores.toString()),
                    isLastItem = false
                )
            }
            // GPU Information Section Header
            item {
                Text(
                    text = "GPU Information",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(top = 16.dp, bottom = 8.dp)
                )
            }
            // GPU Info Content based on state
            item {
                when (gpuInfoState) {
                    is com.ivarna.finalbenchmark2.utils.GpuInfoState.Loading -> {
                        InfoRow("GPU Status", "Loading GPU information...")
                    }
                    is com.ivarna.finalbenchmark2.utils.GpuInfoState.Success -> {
                        val gpuInfo = (gpuInfoState as com.ivarna.finalbenchmark2.utils.GpuInfoState.Success).gpuInfo
                        InfoRow("GPU Name", gpuInfo.basicInfo.name)
                        InfoRow("GPU Vendor", gpuInfo.basicInfo.vendor)
                        InfoRow("OpenGL ES", gpuInfo.basicInfo.openGLVersion)
                        InfoRow("Vulkan", gpuInfo.basicInfo.vulkanVersion ?: "Not Supported")
                    }
                    is com.ivarna.finalbenchmark2.utils.GpuInfoState.Error -> {
                        InfoRow("GPU Status", "Error loading GPU info")
                        InfoRow("GPU Error", (gpuInfoState as com.ivarna.finalbenchmark2.utils.GpuInfoState.Error).message)
                    }
                }
            }
            item {
                com.ivarna.finalbenchmark2.ui.components.InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Total RAM", formatBytes(deviceInfo.totalRam)),
                    isLastItem = false
                )
            }
            item {
                com.ivarna.finalbenchmark2.ui.components.InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Total Storage", formatBytes(deviceInfo.totalStorage)),
                    isLastItem = false
                )
            }
            
            // System Information Section Header
            item {
                Text(
                    text = "System Information",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(top = 16.dp, bottom = 8.dp)
                )
            }
            
            // System info items
            item {
                com.ivarna.finalbenchmark2.ui.components.InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Android Version", "${deviceInfo.androidVersion} (API ${deviceInfo.apiLevel})"),
                    isLastItem = false
                )
            }
            item {
                com.ivarna.finalbenchmark2.ui.components.InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Kernel Version", deviceInfo.kernelVersion),
                    isLastItem = false
                )
            }
            item {
                com.ivarna.finalbenchmark2.ui.components.InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Thermal Status", deviceInfo.thermalStatus ?: "Not available"),
                    isLastItem = false
                )
            }
            item {
                com.ivarna.finalbenchmark2.ui.components.InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Battery Temperature", deviceInfo.batteryTemperature?.let { "${String.format("%.2f", it)}°C" } ?: "Not available"),
                    isLastItem = false
                )
            }
            item {
                com.ivarna.finalbenchmark2.ui.components.InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Battery Capacity", deviceInfo.batteryCapacity?.let { "${it.toInt()}%" } ?: "Not available"),
                    isLastItem = true
                )
            }
        }
    }
}
@Composable
fun CpuTab(
    deviceInfo: com.ivarna.finalbenchmark2.utils.DeviceInfo,
    viewModel: DeviceViewModel
) {
    val cpuHistory by viewModel.cpuHistory.collectAsState()
    val context = LocalContext.current
    var cpuFreqUtils by remember { mutableStateOf<com.ivarna.finalbenchmark2.utils.CpuUtilizationUtils?>(null) }
    var coreFrequencies by remember { mutableStateOf<Map<Int, Pair<Long, Long>>>(emptyMap()) }
    
    // Initialize CPU frequency utilities
    LaunchedEffect(context) {
        cpuFreqUtils = com.ivarna.finalbenchmark2.utils.CpuUtilizationUtils(context)
    }
    
    // Update frequencies periodically
    LaunchedEffect(cpuFreqUtils) {
        if (cpuFreqUtils != null) {
            // Update frequencies every 500ms
            kotlinx.coroutines.delay(500)
            while (true) {
                try {
                    coreFrequencies = cpuFreqUtils?.getAllCoreFrequencies() ?: emptyMap()
                } catch (e: Exception) {
                    // Handle any errors silently to avoid app crashes
                }
                kotlinx.coroutines.delay(500)
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
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
                
        // CPU Utilization Graph
        CpuUtilizationGraph(
            dataPoints = cpuHistory,
            modifier = Modifier.fillMaxWidth()
        )
                
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // CPU Architecture Section Header
            item {
                Text(
                    text = "CPU Architecture",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            // CPU Architecture items
            item {
                com.ivarna.finalbenchmark2.ui.components.InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Architecture", deviceInfo.cpuArchitecture),
                    isLastItem = false
                )
            }
            item {
                com.ivarna.finalbenchmark2.ui.components.InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Total Cores", deviceInfo.totalCores.toString()),
                    isLastItem = false
                )
            }
            item {
                com.ivarna.finalbenchmark2.ui.components.InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Big Cores", deviceInfo.bigCores.toString()),
                    isLastItem = false
                )
            }
            item {
                com.ivarna.finalbenchmark2.ui.components.InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Small Cores", deviceInfo.smallCores.toString()),
                    isLastItem = false
                )
            }
            item {
                com.ivarna.finalbenchmark2.ui.components.InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Cluster Topology", deviceInfo.clusterTopology),
                    isLastItem = false
                )
            }
            
            // Real-time CPU Frequencies Section Header
            item {
                Text(
                    text = "Real-time CPU Frequencies",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(top = 16.dp, bottom = 8.dp)
                )
            }
            
            // Real-time CPU Frequencies items
            if (coreFrequencies.isNotEmpty()) {
                coreFrequencies.forEach { (coreIndex, freqPair) ->
                    val (currentFreq, maxFreq) = freqPair
                    val currentFreqMhz = if (currentFreq > 0) "${currentFreq / 1000} MHz" else "Offline"
                    val maxFreqMhz = if (maxFreq > 0) "${maxFreq / 1000} MHz" else "N/A"
                    
                    item {
                        com.ivarna.finalbenchmark2.ui.components.InformationRow(
                            itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Core $coreIndex", "$currentFreqMhz / $maxFreqMhz"),
                            isLastItem = false
                        )
                    }
                }
            } else {
                // Fallback to static frequencies if real-time reading fails
                deviceInfo.cpuFrequencies.forEach { (core, freq) ->
                    item {
                        com.ivarna.finalbenchmark2.ui.components.InformationRow(
                            itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Core $core", freq),
                            isLastItem = false
                        )
                    }
                }
            }
            
            // CPU Performance Section Header
            item {
                Text(
                    text = "CPU Performance",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(top = 16.dp, bottom = 8.dp)
                )
            }
            
            // CPU Performance items
            item {
                com.ivarna.finalbenchmark2.ui.components.InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Performance Class", "To be implemented"),
                    isLastItem = false
                )
            }
            item {
                com.ivarna.finalbenchmark2.ui.components.InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Instruction Sets", "To be implemented"),
                    isLastItem = true
                )
            }
        }
    }
}

@Composable
fun GpuTab(
    deviceInfo: com.ivarna.finalbenchmark2.utils.DeviceInfo,
    gpuViewModel: GpuInfoViewModel = viewModel()
) {
    val context = LocalContext.current
    var gpuInfoState by remember {
        mutableStateOf<com.ivarna.finalbenchmark2.utils.GpuInfoState>(com.ivarna.finalbenchmark2.utils.GpuInfoState.Loading)
    }
    
    val gpuHistory by gpuViewModel.gpuHistory.collectAsState()
    val gpuFrequencyState by gpuViewModel.gpuFrequencyState.collectAsState()
    
    LaunchedEffect(Unit) {
        val gpuInfoUtils = com.ivarna.finalbenchmark2.utils.GpuInfoUtils(context)
        gpuInfoState = gpuInfoUtils.getGpuInfo()
    }
    
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
        
        // GPU Utilization Graph
        GpuUtilizationGraph(
            dataPoints = gpuHistory,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // GPU Info Content based on state
        when (gpuInfoState) {
            is com.ivarna.finalbenchmark2.utils.GpuInfoState.Loading -> {
                DeviceInfoCard("GPU Information") {
                    InfoRow("Status", "Loading GPU information...")
                }
            }
            is com.ivarna.finalbenchmark2.utils.GpuInfoState.Success -> {
                val gpuInfo = (gpuInfoState as com.ivarna.finalbenchmark2.utils.GpuInfoState.Success).gpuInfo
                GpuInfoContent(gpuInfo, gpuFrequencyState)
            }
            is com.ivarna.finalbenchmark2.utils.GpuInfoState.Error -> {
                DeviceInfoCard("GPU Information") {
                    InfoRow("Status", "Error loading GPU info")
                    InfoRow("Error", (gpuInfoState as com.ivarna.finalbenchmark2.utils.GpuInfoState.Error).message)
                }
            }
        }
    }
}

@Composable
fun GpuInfoContent(gpuInfo: com.ivarna.finalbenchmark2.utils.GpuInfo, gpuFrequencyState: com.ivarna.finalbenchmark2.utils.GpuFrequencyReader.GpuFrequencyState = com.ivarna.finalbenchmark2.utils.GpuFrequencyReader.GpuFrequencyState.NotSupported) {
    // GPU Overview Card
    GpuOverviewCard(gpuInfo.basicInfo, gpuInfo.frequencyInfo, gpuFrequencyState)
    
    Spacer(modifier = Modifier.height(16.dp))
    
    // OpenGL Information Card
    OpenGLInfoCard(gpuInfo.openGLInfo)
    
    Spacer(modifier = Modifier.height(16.dp))
    
    // Vulkan Information Card
    VulkanInfoCard(gpuInfo.vulkanInfo)
    
    Spacer(modifier = Modifier.height(16.dp))
    
    // Advanced Capabilities Card
    AdvancedCapabilitiesCard(gpuInfo.openGLInfo?.capabilities)
}

@Composable
fun GpuOverviewCard(basicInfo: com.ivarna.finalbenchmark2.utils.GpuBasicInfo, frequencyInfo: com.ivarna.finalbenchmark2.utils.GpuFrequencyInfo?, gpuFrequencyState: com.ivarna.finalbenchmark2.utils.GpuFrequencyReader.GpuFrequencyState = com.ivarna.finalbenchmark2.utils.GpuFrequencyReader.GpuFrequencyState.NotSupported) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = { expanded = !expanded }
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "GPU Overview",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    // Display max frequency in the header if available
                    when (gpuFrequencyState) {
                        is com.ivarna.finalbenchmark2.utils.GpuFrequencyReader.GpuFrequencyState.Available -> {
                            val maxFreq = gpuFrequencyState.data.maxFrequencyMhz
                            if (maxFreq != null) {
                                Text(
                                    text = "Max: ${maxFreq} MHz",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        com.ivarna.finalbenchmark2.utils.GpuFrequencyReader.GpuFrequencyState.NotSupported -> {
                            if (frequencyInfo != null && frequencyInfo.maxFrequency != null) {
                                Text(
                                    text = "Max: ${frequencyInfo.maxFrequency} MHz",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        else -> {
                            // No additional info in header for other states
                        }
                    }
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }
            
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                
                InfoRow("GPU Name", basicInfo.name)
                InfoRow("Vendor", basicInfo.vendor)
                InfoRow("Driver Version", basicInfo.driverVersion)
                InfoRow("OpenGL ES", basicInfo.openGLVersion)
                InfoRow("Vulkan", basicInfo.vulkanVersion ?: "Not Supported")
                
                // Display frequency info from the frequency state if available, otherwise use static info
                when (gpuFrequencyState) {
                    is com.ivarna.finalbenchmark2.utils.GpuFrequencyReader.GpuFrequencyState.Available -> {
                        val data = gpuFrequencyState.data
                        InfoRow("Current Frequency", "${data.currentFrequencyMhz} MHz")
                        data.maxFrequencyMhz?.let { maxFreq -> InfoRow("Max Frequency", "${maxFreq} MHz") }
                        data.minFrequencyMhz?.let { minFreq -> InfoRow("Min Frequency", "${minFreq} MHz") }
                        data.governor?.let { governor -> InfoRow("Governor", governor) }
                        data.utilizationPercent?.let { utilization -> InfoRow("Utilization", "${utilization}%") }
                    }
                    com.ivarna.finalbenchmark2.utils.GpuFrequencyReader.GpuFrequencyState.RequiresRoot -> {
                        InfoRow("Current Frequency", "Requires Root Access")
                        InfoRow("Max Frequency", "Requires Root Access")
                    }
                    com.ivarna.finalbenchmark2.utils.GpuFrequencyReader.GpuFrequencyState.NotSupported -> {
                        // If real-time data not available, fallback to static frequency info
                        if (frequencyInfo != null) {
                            InfoRow("Current Frequency", if (frequencyInfo.currentFrequency != null) "${frequencyInfo.currentFrequency} MHz" else "N/A - Permission/Access Issue")
                            InfoRow("Max Frequency", if (frequencyInfo.maxFrequency != null) "${frequencyInfo.maxFrequency} MHz" else "N/A - Permission/Access Issue")
                        } else {
                            InfoRow("Current Frequency", "N/A - Not Available")
                            InfoRow("Max Frequency", "N/A - Not Available")
                        }
                    }
                    is com.ivarna.finalbenchmark2.utils.GpuFrequencyReader.GpuFrequencyState.Error -> {
                        InfoRow("Current Frequency", "Error: ${gpuFrequencyState.message}")
                        InfoRow("Max Frequency", "Error: ${gpuFrequencyState.message}")
                    }
                }
            }
        }
    }
}

@Composable
fun OpenGLInfoCard(openGLInfo: com.ivarna.finalbenchmark2.utils.OpenGLInfo?) {
    if (openGLInfo == null) {
        DeviceInfoCard("OpenGL Information") {
            InfoRow("Status", "Information not available")
        }
        return
    }
    
    var expanded by remember { mutableStateOf(false) }
    var extensionsExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = { expanded = !expanded }
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
                Text(
                    text = "OpenGL Information",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }
            
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                
                InfoRow("Version", openGLInfo.version)
                InfoRow("GLSL Version", openGLInfo.glslVersion)
                InfoRow("Extensions", "${openGLInfo.extensions.size} extensions")
                
                // Extensions list
                if (openGLInfo.extensions.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "OpenGL Extensions (${openGLInfo.extensions.size}):",
                            fontWeight = FontWeight.Medium
                        )
                        IconButton(onClick = { extensionsExpanded = !extensionsExpanded }) {
                            Icon(
                                imageVector = if (extensionsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (extensionsExpanded) "Hide" else "Show"
                            )
                        }
                    }
                    
                    if (extensionsExpanded) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = MaterialTheme.shapes.small
                                )
                                .padding(8.dp)
                        ) {
                            items(openGLInfo.extensions.size) { index ->
                                Text(
                                    text = openGLInfo.extensions[index],
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VulkanInfoCard(vulkanInfo: com.ivarna.finalbenchmark2.utils.VulkanInfo?) {
    if (vulkanInfo == null) {
        DeviceInfoCard("Vulkan Information") {
            InfoRow("Status", "Information not available")
        }
        return
    }
    
    var expanded by remember { mutableStateOf(false) }
    var extensionsExpanded by remember { mutableStateOf(false) }
    var featuresExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = { expanded = !expanded }
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
                Text(
                    text = "Vulkan Information",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }
            
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                
                InfoRow("Supported", if (vulkanInfo.supported) "Yes" else "No")
                if (vulkanInfo.supported) {
                    vulkanInfo.apiVersion?.let { InfoRow("API Version", it) }
                    vulkanInfo.driverVersion?.let { InfoRow("Driver Version", it) }
                    vulkanInfo.physicalDeviceName?.let { InfoRow("Physical Device", it) }
                    vulkanInfo.physicalDeviceType?.let { InfoRow("Device Type", it) }
                    
                    // Extensions
                    val totalExtensions = vulkanInfo.instanceExtensions.size + vulkanInfo.deviceExtensions.size
                    InfoRow("Total Extensions", "$totalExtensions extensions")
                    
                    // Extensions list
                    if (totalExtensions > 0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Vulkan Extensions ($totalExtensions):",
                                fontWeight = FontWeight.Medium
                            )
                            IconButton(onClick = { extensionsExpanded = !extensionsExpanded }) {
                                Icon(
                                    imageVector = if (extensionsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (extensionsExpanded) "Hide" else "Show"
                                )
                            }
                        }
                        
                        if (extensionsExpanded) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = MaterialTheme.shapes.small
                                    )
                                    .padding(8.dp)
                            ) {
                                // Instance extensions
                                if (vulkanInfo.instanceExtensions.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "Instance Extensions:",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                    }
                                    items(vulkanInfo.instanceExtensions.size) { index ->
                                        Text(
                                            text = vulkanInfo.instanceExtensions[index],
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )
                                    }
                                }
                                
                                // Device extensions
                                if (vulkanInfo.deviceExtensions.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "Device Extensions:",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                        )
                                    }
                                    items(vulkanInfo.deviceExtensions.size) { index ->
                                        Text(
                                            text = vulkanInfo.deviceExtensions[index],
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Features (if available)
                    if (vulkanInfo.features != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Vulkan Features:",
                                fontWeight = FontWeight.Medium
                            )
                            IconButton(onClick = { featuresExpanded = !featuresExpanded }) {
                                Icon(
                                    imageVector = if (featuresExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (featuresExpanded) "Hide" else "Show"
                                )
                            }
                        }
                        
                        if (featuresExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = MaterialTheme.shapes.small
                                    )
                                    .padding(8.dp)
                            ) {
                                val features = vulkanInfo.features
                                InfoRow("Geometry Shader", if (features.geometryShader) "✓" else "✗")
                                InfoRow("Tessellation Shader", if (features.tessellationShader) "✓" else "✗")
                                InfoRow("Multi Viewport", if (features.multiViewport) "✓" else "✗")
                                InfoRow("Sparse Binding", if (features.sparseBinding) "✓" else "✗")
                                InfoRow("Variable Multisample Rate", if (features.variableMultisampleRate) "✓" else "✗")
                                InfoRow("Robust Buffer Access", if (features.robustBufferAccess) "✓" else "✗")
                                InfoRow("Full Draw Index Uint32", if (features.fullDrawIndexUint32) "✓" else "✗")
                                InfoRow("Image Cube Array", if (features.imageCubeArray) "✓" else "✗")
                                InfoRow("Independent Blend", if (features.independentBlend) "✓" else "✗")
                                InfoRow("Sample Rate Shading", if (features.sampleRateShading) "✓" else "✗")
                                InfoRow("Dual Src Blend", if (features.dualSrcBlend) "✓" else "✗")
                                InfoRow("Logic Op", if (features.logicOp) "✓" else "✗")
                                InfoRow("Multi Draw Indirect", if (features.multiDrawIndirect) "✓" else "✗")
                                InfoRow("Draw Indirect First Instance", if (features.drawIndirectFirstInstance) "✓" else "✗")
                                InfoRow("Depth Clamp", if (features.depthClamp) "✓" else "✗")
                                InfoRow("Depth Bias Clamp", if (features.depthBiasClamp) "✓" else "✗")
                                InfoRow("Fill Mode Non Solid", if (features.fillModeNonSolid) "✓" else "✗")
                                InfoRow("Depth Bounds", if (features.depthBounds) "✓" else "✗")
                                InfoRow("Wide Lines", if (features.wideLines) "✓" else "✗")
                                InfoRow("Large Points", if (features.largePoints) "✓" else "✗")
                                InfoRow("Alpha To One", if (features.alphaToOne) "✓" else "✗")
                                InfoRow("Sampler Anisotropy", if (features.samplerAnisotropy) "✓" else "✗")
                                InfoRow("Texture Compression ETC2", if (features.textureCompressionETC2) "✓" else "✗")
                                InfoRow("Texture Compression ASTC_LDR", if (features.textureCompressionASTC_LDR) "✓" else "✗")
                                InfoRow("Texture Compression BC", if (features.textureCompressionBC) "✓" else "✗")
                                InfoRow("Occlusion Query Precise", if (features.occlusionQueryPrecise) "✓" else "✗")
                                InfoRow("Pipeline Statistics Query", if (features.pipelineStatisticsQuery) "✓" else "✗")
                                InfoRow("Vertex Pipeline Stores And Atomics", if (features.vertexPipelineStoresAndAtomics) "✓" else "✗")
                                InfoRow("Fragment Stores And Atomics", if (features.fragmentStoresAndAtomics) "✓" else "✗")
                                InfoRow("Shader Tessellation And Geometry Point Size", if (features.shaderTessellationAndGeometryPointSize) "✓" else "✗")
                                InfoRow("Shader Image Gather Extended", if (features.shaderImageGatherExtended) "✓" else "✗")
                                InfoRow("Shader Storage Image Extended Formats", if (features.shaderStorageImageExtendedFormats) "✓" else "✗")
                                InfoRow("Shader Storage Image Multisample", if (features.shaderStorageImageMultisample) "✓" else "✗")
                                InfoRow("Shader Storage Image Read Without Format", if (features.shaderStorageImageReadWithoutFormat) "✓" else "✗")
                                InfoRow("Shader Storage Image Write Without Format", if (features.shaderStorageImageWriteWithoutFormat) "✓" else "✗")
                                InfoRow("Shader Uniform Buffer Array Dynamic Indexing", if (features.shaderUniformBufferArrayDynamicIndexing) "✓" else "✗")
                                InfoRow("Shader Sampled Image Array Dynamic Indexing", if (features.shaderSampledImageArrayDynamicIndexing) "✓" else "✗")
                                InfoRow("Shader Storage Buffer Array Dynamic Indexing", if (features.shaderStorageBufferArrayDynamicIndexing) "✓" else "✗")
                                InfoRow("Shader Storage Image Array Dynamic Indexing", if (features.shaderStorageImageArrayDynamicIndexing) "✓" else "✗")
                                InfoRow("Shader Clip Distance", if (features.shaderClipDistance) "✓" else "✗")
                                InfoRow("Shader Cull Distance", if (features.shaderCullDistance) "✓" else "✗")
                                InfoRow("Shader Float64", if (features.shaderFloat64) "✓" else "✗")
                                InfoRow("Shader Int64", if (features.shaderInt64) "✓" else "✗")
                                InfoRow("Shader Int16", if (features.shaderInt16) "✓" else "✗")
                                InfoRow("Shader Resource Residency", if (features.shaderResourceResidency) "✓" else "✗")
                                InfoRow("Shader Resource Min Lod", if (features.shaderResourceMinLod) "✓" else "✗")
                                InfoRow("Sparse Residency Buffer", if (features.sparseResidencyBuffer) "✓" else "✗")
                                InfoRow("Sparse Residency Image2D", if (features.sparseResidencyImage2D) "✓" else "✗")
                                InfoRow("Sparse Residency Image3D", if (features.sparseResidencyImage3D) "✓" else "✗")
                                InfoRow("Sparse Residency 2 Samples", if (features.sparseResidency2Samples) "✓" else "✗")
                                InfoRow("Sparse Residency 4 Samples", if (features.sparseResidency4Samples) "✓" else "✗")
                                InfoRow("Sparse Residency 8 Samples", if (features.sparseResidency8Samples) "✓" else "✗")
                                InfoRow("Sparse Residency 16 Samples", if (features.sparseResidency16Samples) "✓" else "✗")
                                InfoRow("Sparse Residency Aliased", if (features.sparseResidencyAliased) "✓" else "✗")
                                InfoRow("Inherited Queries", if (features.inheritedQueries) "✓" else "✗")
                            }
                        }
                    }
                    
                    // Memory heaps (if available)
                    if (vulkanInfo.memoryHeaps != null) {
                        Text(
                            text = "Memory Heaps:",
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        vulkanInfo.memoryHeaps.forEach { heap ->
                            InfoRow(
                                "Heap ${heap.index}",
                                "${formatBytes(heap.size)} (${heap.flags})"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdvancedCapabilitiesCard(capabilities: com.ivarna.finalbenchmark2.utils.OpenGLCapabilities?) {
    if (capabilities == null) {
        DeviceInfoCard("Advanced Capabilities") {
            InfoRow("Status", "Information not available")
        }
        return
    }
    
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = { expanded = !expanded }
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
                Text(
                    text = "Advanced Capabilities",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }
            
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                
                InfoRow("Max Texture Size", "${capabilities.maxTextureSize} x ${capabilities.maxTextureSize}")
                InfoRow("Max Renderbuffer Size", "${capabilities.maxRenderbufferSize} x ${capabilities.maxRenderbufferSize}")
                InfoRow("Max Viewport", "${capabilities.maxViewportWidth} x ${capabilities.maxViewportHeight}")
                InfoRow("Max Fragment Uniform Vectors", capabilities.maxFragmentUniformVectors.toString())
                InfoRow("Max Vertex Attributes", capabilities.maxVertexAttributes.toString())
                
                if (capabilities.supportedTextureCompressionFormats.isNotEmpty()) {
                    InfoRow("Texture Compression", capabilities.supportedTextureCompressionFormats.joinToString(", "))
                }
            }
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
    val context = LocalContext.current
    var gpuInfoState by remember {
        mutableStateOf<com.ivarna.finalbenchmark2.utils.GpuInfoState>(com.ivarna.finalbenchmark2.utils.GpuInfoState.Loading)
    }
    
    LaunchedEffect(Unit) {
        val gpuInfoUtils = com.ivarna.finalbenchmark2.utils.GpuInfoUtils(context)
        gpuInfoState = gpuInfoUtils.getGpuInfo()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
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
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // Main Hardware Section Header
            item {
                Text(
                    text = "Main Hardware",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            // Main hardware items
            item {
                com.ivarna.finalbenchmark2.ui.components.InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Model", deviceInfo.deviceModel),
                    isLastItem = false
                )
            }
            item {
                com.ivarna.finalbenchmark2.ui.components.InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Manufacturer", deviceInfo.manufacturer),
                    isLastItem = false
                )
            }
            item {
                com.ivarna.finalbenchmark2.ui.components.InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Board", deviceInfo.board),
                    isLastItem = false
                )
            }
            item {
                com.ivarna.finalbenchmark2.ui.components.InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("SoC", deviceInfo.socName),
                    isLastItem = false
                )
            }
            
            // GPU Information Section Header
            item {
                Text(
                    text = "GPU Information",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(top = 16.dp, bottom = 8.dp)
                )
            }
            
            // GPU Info Content based on state
            item {
                when (gpuInfoState) {
                    is com.ivarna.finalbenchmark2.utils.GpuInfoState.Loading -> {
                        InfoRow("Status", "Loading GPU information...")
                    }
                    is com.ivarna.finalbenchmark2.utils.GpuInfoState.Success -> {
                        val gpuInfo = (gpuInfoState as com.ivarna.finalbenchmark2.utils.GpuInfoState.Success).gpuInfo
                        GpuInfoInHardwareTab(gpuInfo)
                    }
                    is com.ivarna.finalbenchmark2.utils.GpuInfoState.Error -> {
                        InfoRow("Status", "Error loading GPU info")
                        InfoRow("Error", (gpuInfoState as com.ivarna.finalbenchmark2.utils.GpuInfoState.Error).message)
                    }
                }
            }
            
            // Memory Section Header
            item {
                Text(
                    text = "Memory",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(top = 16.dp, bottom = 8.dp)
                )
            }
            
            // Memory items
            item {
                com.ivarna.finalbenchmark2.ui.components.InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Total RAM", formatBytes(deviceInfo.totalRam)),
                    isLastItem = false
                )
            }
            item {
                com.ivarna.finalbenchmark2.ui.components.InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Available RAM", formatBytes(deviceInfo.availableRam)),
                    isLastItem = false
                )
            }
            item {
                com.ivarna.finalbenchmark2.ui.components.InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Storage Total", formatBytes(deviceInfo.totalStorage)),
                    isLastItem = false
                )
            }
            item {
                com.ivarna.finalbenchmark2.ui.components.InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Storage Free", formatBytes(deviceInfo.freeStorage)),
                    isLastItem = false
                )
            }
            
            // Power & Thermal Section Header
            item {
                Text(
                    text = "Power & Thermal",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(top = 16.dp, bottom = 8.dp)
                )
            }
            
            // Power & Thermal items
            item {
                com.ivarna.finalbenchmark2.ui.components.InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Battery Capacity", deviceInfo.batteryCapacity?.let { "${it.toInt()}%" } ?: "Not available"),
                    isLastItem = false
                )
            }
            item {
                com.ivarna.finalbenchmark2.ui.components.InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Battery Temperature", deviceInfo.batteryTemperature?.let { "${String.format("%.2f", it)}°C" } ?: "Not available"),
                    isLastItem = false
                )
            }
            item {
                com.ivarna.finalbenchmark2.ui.components.InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Thermal Status", deviceInfo.thermalStatus ?: "Not available"),
                    isLastItem = true
                )
            }
        }
    }
}

@Composable
fun MemoryTab(deviceInfo: com.ivarna.finalbenchmark2.utils.DeviceInfo) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Memory Information",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // RAM Information Section Header
            item {
                Text(
                    text = "RAM Information",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            // RAM items
            item {
                com.ivarna.finalbenchmark2.ui.components.InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Total RAM", formatBytes(deviceInfo.totalRam)),
                    isLastItem = false
                )
            }
            item {
                com.ivarna.finalbenchmark2.ui.components.InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Available RAM", formatBytes(deviceInfo.availableRam)),
                    isLastItem = false
                )
            }
            
            // Storage Information Section Header
            item {
                Text(
                    text = "Storage Information",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(top = 16.dp, bottom = 8.dp)
                )
            }
            
            // Storage items
            item {
                com.ivarna.finalbenchmark2.ui.components.InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Total Storage", formatBytes(deviceInfo.totalStorage)),
                    isLastItem = false
                )
            }
            item {
                com.ivarna.finalbenchmark2.ui.components.InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Free Storage", formatBytes(deviceInfo.freeStorage)),
                    isLastItem = true
                )
            }
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
fun GpuInfoInHardwareTab(gpuInfo: com.ivarna.finalbenchmark2.utils.GpuInfo) {
    // Basic GPU Info
    InfoRow("GPU Name", gpuInfo.basicInfo.name)
    InfoRow("Vendor", gpuInfo.basicInfo.vendor)
    InfoRow("Driver Version", gpuInfo.basicInfo.driverVersion)
    InfoRow("OpenGL ES", gpuInfo.basicInfo.openGLVersion)
    InfoRow("Vulkan", gpuInfo.basicInfo.vulkanVersion ?: "Not Supported")
    
    // GPU Frequency Info (if available)
    if (gpuInfo.frequencyInfo != null) {
        val currentFreq = gpuInfo.frequencyInfo.currentFrequency
        val maxFreq = gpuInfo.frequencyInfo.maxFrequency
        if (currentFreq != null) InfoRow("Current Frequency", "${currentFreq} MHz")
        if (maxFreq != null) InfoRow("Max Frequency", "${maxFreq} MHz")
    }
    
    // OpenGL Info (if available)
    gpuInfo.openGLInfo?.let { openGLInfo ->
        InfoRow("OpenGL Version", openGLInfo.version)
        InfoRow("GLSL Version", openGLInfo.glslVersion)
        InfoRow("OpenGL Extensions", "${openGLInfo.extensions.size} extensions")
    }
    
    // Vulkan Info (if available)
    gpuInfo.vulkanInfo?.let { vulkanInfo ->
        if (vulkanInfo.supported) {
            vulkanInfo.apiVersion?.let { InfoRow("Vulkan API Version", it) }
            vulkanInfo.driverVersion?.let { InfoRow("Vulkan Driver Version", it) }
            vulkanInfo.physicalDeviceName?.let { InfoRow("Vulkan Physical Device", it) }
            InfoRow("Vulkan Extensions", "${vulkanInfo.instanceExtensions.size + vulkanInfo.deviceExtensions.size} total extensions")
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