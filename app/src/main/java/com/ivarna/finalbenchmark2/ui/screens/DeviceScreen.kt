package com.ivarna.finalbenchmark2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ivarna.finalbenchmark2.R
import com.ivarna.finalbenchmark2.ui.theme.FinalBenchmark2Theme
import com.ivarna.finalbenchmark2.utils.CpuNativeBridge
import com.ivarna.finalbenchmark2.utils.CpuUtilizationUtils
import com.ivarna.finalbenchmark2.utils.DeviceInfoCollector
// Remove the DisplayUtils import since the class doesn't exist
import com.ivarna.finalbenchmark2.utils.formatBytes
import kotlin.math.roundToInt
import com.ivarna.finalbenchmark2.ui.components.CpuUtilizationGraph
import com.ivarna.finalbenchmark2.ui.components.GpuUtilizationGraph
import com.ivarna.finalbenchmark2.ui.components.GpuFrequencyCard
import com.ivarna.finalbenchmark2.ui.components.MemoryUsageGraph
import com.ivarna.finalbenchmark2.ui.components.PowerConsumptionGraph
import com.ivarna.finalbenchmark2.ui.components.ProcessItem
import com.ivarna.finalbenchmark2.ui.components.ProcessTable
import com.ivarna.finalbenchmark2.ui.components.SummaryCard
import com.ivarna.finalbenchmark2.ui.components.SystemInfoSummary
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

/**
 * Format bytes to megabytes
 */
fun formatBytesInMB(bytes: Long): String {
    val mb = bytes.toDouble() / (1024.0 * 1024.0)
    return "${mb.roundToInt()} MB"
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
                        .padding(top = 8.dp) // Add spacing at top of each tab view
                ) {
                    when (selectedTabIndex) {
                        0 -> InfoTab(deviceInfo, viewModel)
                        1 -> CpuTab(deviceInfo, viewModel)
                        2 -> GpuTab(deviceInfo)
                        3 -> MemoryTab(deviceInfo, viewModel)
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
                        // Display more detailed Vulkan info if available
                        if (gpuInfo.vulkanInfo != null && gpuInfo.vulkanInfo.supported) {
                            val vulkanInfo = gpuInfo.vulkanInfo
                            val vulkanVersion = vulkanInfo.apiVersion ?: "Supported"
                            InfoRow("Vulkan", vulkanVersion)
                        } else if (gpuInfo.basicInfo.vulkanVersion != null) {
                            InfoRow("Vulkan", gpuInfo.basicInfo.vulkanVersion)
                        } else {
                            InfoRow("Vulkan", "Not Supported")
                        }
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
            
            // CPU Frequencies Section Header
            item {
                Text(
                    text = "CPU Frequencies",
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
            
            // Processor Details Section Header
            item {
                Text(
                    text = "Processor Details",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(top = 16.dp, bottom = 8.dp)
                )
            }
            
            // Get detailed CPU information from native bridge
            val cpuNative = com.ivarna.finalbenchmark2.utils.CpuNativeBridge()
            val details = cpuNative.getCpuDetails()
            
            // Add processor details
            item {
                com.ivarna.finalbenchmark2.ui.components.InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("SoC Name", details.socName),
                    isLastItem = false
                )
            }
            item {
                com.ivarna.finalbenchmark2.ui.components.InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("ABI", details.abi),
                    isLastItem = false
                )
            }
            item {
                com.ivarna.finalbenchmark2.ui.components.InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("ARM Neon", if(details.hasNeon) "Yes" else "No"),
                    isLastItem = false
                )
            }
            
            // Add cache configuration
            if (details.caches.isNotEmpty()) {
                // Cache Configuration Section Header
                item {
                    Text(
                        text = "Cache Configuration",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(top = 16.dp, bottom = 8.dp)
                    )
                }
                
                details.caches.forEach { cache ->
                    // Format: "L1 Instruction" -> "64KB"
                    val name = "L${cache.level} ${cache.type.replaceFirstChar { it.uppercase() }}"
                    item {
                        com.ivarna.finalbenchmark2.ui.components.InformationRow(
                            itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text(name, cache.size),
                            isLastItem = false
                        )
                    }
                }
            }
            
            // CPU Governor Section
            item {
                Text(
                    text = "CPU Governor",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(top = 16.dp, bottom = 8.dp)
                )
            }
            
            // Add CPU governor information
            item {
                val governor = cpuFreqUtils?.getCurrentCpuGovernor()
                if (governor != null) {
                    com.ivarna.finalbenchmark2.ui.components.InformationRow(
                        itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Current Governor", governor),
                        isLastItem = false
                    )
                } else {
                    com.ivarna.finalbenchmark2.ui.components.InformationRow(
                        itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Current Governor", "Not available"),
                        isLastItem = false
                    )
                }
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
        
        // GPU Frequency Card - Real-time frequency monitoring
        GpuFrequencyCard(
            modifier = Modifier.fillMaxWidth(),
            viewModel = gpuViewModel
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
    GpuOverviewCard(gpuInfo, gpuFrequencyState)
    
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
fun GpuOverviewCard(gpuInfo: com.ivarna.finalbenchmark2.utils.GpuInfo, gpuFrequencyState: com.ivarna.finalbenchmark2.utils.GpuFrequencyReader.GpuFrequencyState = com.ivarna.finalbenchmark2.utils.GpuFrequencyReader.GpuFrequencyState.NotSupported) {
    val basicInfo = gpuInfo.basicInfo
    val frequencyInfo = gpuInfo.frequencyInfo
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
                // Display more detailed Vulkan info if available
                if (gpuInfo.vulkanInfo != null && gpuInfo.vulkanInfo.supported) {
                    val vulkanInfo = gpuInfo.vulkanInfo
                    val vulkanVersion = vulkanInfo.apiVersion ?: "Supported"
                    InfoRow("Vulkan", vulkanVersion)
                } else if (basicInfo.vulkanVersion != null) {
                    InfoRow("Vulkan", basicInfo.vulkanVersion)
                } else {
                    InfoRow("Vulkan", "Not Supported")
                }
                
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
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = MaterialTheme.shapes.small
                                )
                                .padding(8.dp)
                        ) {
                            openGLInfo.extensions.forEach { extension ->
                                Text(
                                    text = extension,
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
                            Column(
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
                                    Text(
                                        text = "Instance Extensions:",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                    vulkanInfo.instanceExtensions.forEach { extension ->
                                        Text(
                                            text = extension,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )
                                    }
                                }
                                
                                // Device extensions
                                if (vulkanInfo.deviceExtensions.isNotEmpty() && vulkanInfo.instanceExtensions.isNotEmpty()) {
                                    Text(
                                        text = "Device Extensions:",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                    )
                                } else if (vulkanInfo.deviceExtensions.isNotEmpty()) {
                                    Text(
                                        text = "Vulkan Extensions:",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                                
                                vulkanInfo.deviceExtensions.forEach { extension ->
                                    Text(
                                        text = extension,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
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
                            VulkanFeaturesGrid(vulkanInfo.features)
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
    val displayMetrics = remember { context.resources.displayMetrics }
    val windowManager = context.getSystemService(android.content.Context.WINDOW_SERVICE) as android.view.WindowManager
    val display = windowManager.defaultDisplay
    val displaySize = android.graphics.Point().apply { display.getSize(this) }
    
    // Create a mock display info object with basic information
    val displayInfo = remember {
        DisplayInfo(
            resolution = "${displaySize.x} x ${displaySize.y}",
            density = "${displayMetrics.density}x (${displayMetrics.densityDpi} DPI)",
            physicalSize = "%.1f\"".format(kotlin.math.sqrt(((displaySize.x / displayMetrics.xdpi).toDouble() * (displaySize.x / displayMetrics.xdpi).toDouble()) + ((displaySize.y / displayMetrics.ydpi).toDouble() * (displaySize.y / displayMetrics.ydpi).toDouble()))),
            aspectRatio = calculateAspectRatio(displaySize.x, displaySize.y),
            exactDpiX = "${displayMetrics.xdpi} DPI",
            exactDpiY = "${displayMetrics.ydpi} DPI",
            realMetrics = "w${displaySize.x}dp x h${displaySize.y}dp",
            refreshRate = "${display.refreshRate} Hz",
            maxRefreshRate = "Unknown",
            hdrSupport = "Unknown",
            hdrTypes = emptyList(),
            wideColorGamut = false,
            orientation = when (context.resources.configuration.orientation) {
                android.content.res.Configuration.ORIENTATION_PORTRAIT -> "Portrait"
                android.content.res.Configuration.ORIENTATION_LANDSCAPE -> "Landscape"
                else -> "Unknown"
            },
            rotation = display.rotation,
            brightnessLevel = "Unknown",
            screenTimeout = "Unknown",
            safeAreaInsets = "Unknown",
            displayCutout = "Unknown"
        )
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
            text = "Screen Information",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )
        
        // Card 1: Display Metrics
        DisplayMetricsCard(displayInfo)
                
        Spacer(modifier = Modifier.height(16.dp))
        
        // Card 2: Capabilities
        DisplayCapabilitiesCard(displayInfo)
                
        Spacer(modifier = Modifier.height(16.dp))
        
        // Card 3: System State
        DisplaySystemStateCard(displayInfo)
    }
}

// Data class to represent display information
data class DisplayInfo(
    val resolution: String,
    val density: String,
    val physicalSize: String,
    val aspectRatio: String,
    val exactDpiX: String,
    val exactDpiY: String,
    val realMetrics: String,
    val refreshRate: String,
    val maxRefreshRate: String,
    val hdrSupport: String,
    val hdrTypes: List<String>,
    val wideColorGamut: Boolean,
    val orientation: String,
    val rotation: Int,
    val brightnessLevel: String?,
    val screenTimeout: String?,
    val safeAreaInsets: String,
    val displayCutout: String
)

@Composable
fun DisplayMetricsCard(displayInfo: DisplayInfo) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.AspectRatio,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Display Metrics",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            InfoRow("Resolution", displayInfo.resolution)
            InfoRow("Logical Density", displayInfo.density)
            InfoRow("Physical Size", displayInfo.physicalSize)
            InfoRow("Aspect Ratio", displayInfo.aspectRatio)
            InfoRow("Exact X DPI", displayInfo.exactDpiX)
            InfoRow("Exact Y DPI", displayInfo.exactDpiY)
            
            // Add real metrics info as collapsible section
            var expanded by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Real Metrics",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }
            
            if (expanded) {
                Text(
                    text = displayInfo.realMetrics,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun DisplayCapabilitiesCard(displayInfo: DisplayInfo) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.HdrOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Capabilities",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            InfoRow("Current Refresh Rate", displayInfo.refreshRate)
            InfoRow("Max Refresh Rate", displayInfo.maxRefreshRate)
            InfoRow("HDR Support", displayInfo.hdrSupport)
            
            if (displayInfo.hdrTypes.isNotEmpty()) {
                var hdrTypesExpanded by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "HDR Types",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { hdrTypesExpanded = !hdrTypesExpanded }) {
                        Icon(
                            imageVector = if (hdrTypesExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (hdrTypesExpanded) "Collapse" else "Expand"
                        )
                    }
                }
                
                if (hdrTypesExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        displayInfo.hdrTypes.forEach { hdrType ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = com.ivarna.finalbenchmark2.R.drawable.check_24),
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = hdrType,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            
            InfoRow(
                "Wide Color Gamut",
                if (displayInfo.wideColorGamut) "Yes" else "No",
                isSupported = displayInfo.wideColorGamut
            )
        }
    }
}

@Composable
fun DisplaySystemStateCard(displayInfo: DisplayInfo) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.ScreenRotation,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "System State",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            InfoRow("Orientation", displayInfo.orientation)
            InfoRow("Rotation", "${displayInfo.rotation * 90}°")
            
            displayInfo.brightnessLevel?.let { brightness ->
                InfoRow("Brightness Level", brightness)
            }
            
            displayInfo.screenTimeout?.let { timeout ->
                InfoRow("Screen Timeout", timeout)
            }
            
            // Safe area and cutout info
            var safeAreaExpanded by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Safe Area & Cutout",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { safeAreaExpanded = !safeAreaExpanded }) {
                    Icon(
                        imageVector = if (safeAreaExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (safeAreaExpanded) "Collapse" else "Expand"
                    )
                }
            }
            
            if (safeAreaExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Text(
                        text = "Safe Area: ${displayInfo.safeAreaInsets}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "Display Cutout: ${displayInfo.displayCutout}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String, isSupported: Boolean? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            isSupported?.let { supported ->
                Icon(
                    painter = if (supported) painterResource(id = com.ivarna.finalbenchmark2.R.drawable.check_24)
                              else painterResource(id = com.ivarna.finalbenchmark2.R.drawable.close_24),
                    contentDescription = if (supported) "Supported" else "Not Supported",
                    tint = if (supported) Color(0xFF4CAF50) else Color(0xFFEF5350),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = if (value.isEmpty()) MaterialTheme.colorScheme.tertiary
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Total RAM", formatBytesInMB(deviceInfo.totalRam)),
                    isLastItem = false
                )
            }
            item {
                com.ivarna.finalbenchmark2.ui.components.InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Available RAM", formatBytesInMB(deviceInfo.availableRam)),
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
fun MemoryTab(deviceInfo: com.ivarna.finalbenchmark2.utils.DeviceInfo, viewModel: DeviceViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val memoryHistory by viewModel.memoryHistory.collectAsState()
    val systemInfoSummary by viewModel.systemInfoSummary.collectAsState()
    val context = LocalContext.current
    
    // Fetch system process information when the tab is loaded
    LaunchedEffect(Unit) {
        viewModel.fetchSystemInfo(context)
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        item {
            Text(
                text = "Memory Information",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
            )
        }
        
        // Memory Usage Graph
        item {
            MemoryUsageGraph(
                dataPoints = memoryHistory,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // System Summary Card
        item {
            SummaryCard(
                summary = systemInfoSummary,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // RAM Information Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "RAM Information",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // RAM items
                    com.ivarna.finalbenchmark2.ui.components.InformationRow(
                        itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Total RAM", formatBytesInMB(deviceInfo.totalRam)),
                        isLastItem = false
                    )
                    com.ivarna.finalbenchmark2.ui.components.InformationRow(
                        itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Available RAM", formatBytesInMB(deviceInfo.availableRam)),
                        isLastItem = true
                    )
                }
            }
        }
        
        // Storage Information Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Storage Information",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // Storage items
                    com.ivarna.finalbenchmark2.ui.components.InformationRow(
                        itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Total Storage", formatBytes(deviceInfo.totalStorage)),
                        isLastItem = false
                    )
                    com.ivarna.finalbenchmark2.ui.components.InformationRow(
                        itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Free Storage", formatBytes(deviceInfo.freeStorage)),
                        isLastItem = true
                    )
                }
            }
        }
        
        // Process Table Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "App",
                    modifier = Modifier.weight(2f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "PID",
                    modifier = Modifier.weight(0.8f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "State",
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "RAM (MB)",
                    modifier = Modifier.weight(0.8f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    textAlign = TextAlign.End,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        
        // Process Items
        items(systemInfoSummary.processes) { process ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = process.name,
                    modifier = Modifier.weight(2f),
                    fontSize = 14.sp,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = process.pid.toString(),
                    modifier = Modifier.weight(0.8f),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = process.state,
                    modifier = Modifier.weight(1f),
                    fontSize = 14.sp,
                    color = when (process.state) {
                        "Foreground" -> Color.Green
                        "Service" -> Color.Blue
                        "Background" -> Color.Gray
                        else -> Color.Red
                    }
                )
                Text(
                    text = "${process.ramUsage} MB",
                    modifier = Modifier.weight(0.8f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.End,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
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
fun VulkanFeaturesGrid(features: com.ivarna.finalbenchmark2.utils.VulkanFeatures) {
    val sortedFeatures = remember(features) {
        listOf(
            "Alpha To One" to features.alphaToOne,
            "Depth Bias Clamp" to features.depthBiasClamp,
            "Depth Bounds" to features.depthBounds,
            "Depth Clamp" to features.depthClamp,
            "Draw Indirect First Instance" to features.drawIndirectFirstInstance,
            "Dual Src Blend" to features.dualSrcBlend,
            "Fill Mode Non Solid" to features.fillModeNonSolid,
            "Fragment Stores And Atomics" to features.fragmentStoresAndAtomics,
            "Full Draw Index Uint32" to features.fullDrawIndexUint32,
            "Geometry Shader" to features.geometryShader,
            "Image Cube Array" to features.imageCubeArray,
            "Independent Blend" to features.independentBlend,
            "Inherited Queries" to features.inheritedQueries,
            "Large Points" to features.largePoints,
            "Logic Op" to features.logicOp,
            "Multi Draw Indirect" to features.multiDrawIndirect,
            "Multi Viewport" to features.multiViewport,
            "Occlusion Query Precise" to features.occlusionQueryPrecise,
            "Pipeline Statistics Query" to features.pipelineStatisticsQuery,
            "Robust Buffer Access" to features.robustBufferAccess,
            "Sample Rate Shading" to features.sampleRateShading,
            "Sampler Anisotropy" to features.samplerAnisotropy,
            "Shader Clip Distance" to features.shaderClipDistance,
            "Shader Cull Distance" to features.shaderCullDistance,
            "Shader Float64" to features.shaderFloat64,
            "Shader Image Gather Extended" to features.shaderImageGatherExtended,
            "Shader Int16" to features.shaderInt16,
            "Shader Int64" to features.shaderInt64,
            "Shader Resource Min Lod" to features.shaderResourceMinLod,
            "Shader Resource Residency" to features.shaderResourceResidency,
            "Shader Sampled Image Array Dynamic Indexing" to features.shaderSampledImageArrayDynamicIndexing,
            "Shader Storage Buffer Array Dynamic Indexing" to features.shaderStorageBufferArrayDynamicIndexing,
            "Shader Storage Image Array Dynamic Indexing" to features.shaderStorageImageArrayDynamicIndexing,
            "Shader Storage Image Extended Formats" to features.shaderStorageImageExtendedFormats,
            "Shader Storage Image Multisample" to features.shaderStorageImageMultisample,
            "Shader Storage Image Read Without Format" to features.shaderStorageImageReadWithoutFormat,
            "Shader Storage Image Write Without Format" to features.shaderStorageImageWriteWithoutFormat,
            "Shader Tessellation And Geometry Point Size" to features.shaderTessellationAndGeometryPointSize,
            "Shader Uniform Buffer Array Dynamic Indexing" to features.shaderUniformBufferArrayDynamicIndexing,
            "Sparse Binding" to features.sparseBinding,
            "Sparse Residency 2 Samples" to features.sparseResidency2Samples,
            "Sparse Residency 4 Samples" to features.sparseResidency4Samples,
            "Sparse Residency 8 Samples" to features.sparseResidency8Samples,
            "Sparse Residency 16 Samples" to features.sparseResidency16Samples,
            "Sparse Residency Aliased" to features.sparseResidencyAliased,
            "Sparse Residency Buffer" to features.sparseResidencyBuffer,
            "Sparse Residency Image2D" to features.sparseResidencyImage2D,
            "Sparse Residency Image3D" to features.sparseResidencyImage3D,
            "Tessellation Shader" to features.tessellationShader,
            "Texture Compression ASTC_LDR" to features.textureCompressionASTC_LDR,
            "Texture Compression BC" to features.textureCompressionBC,
            "Texture Compression ETC2" to features.textureCompressionETC2,
            "Variable Multisample Rate" to features.variableMultisampleRate,
            "Vertex Pipeline Stores And Atomics" to features.vertexPipelineStoresAndAtomics,
            "Wide Lines" to features.wideLines
        ).sortedBy { it.first }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                "Vulkan Capabilities",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Single Column List Layout
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sortedFeatures.forEach { featurePair ->
                    val name = featurePair.component1()
                    val isSupported = featurePair.component2()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = if (isSupported) painterResource(id = com.ivarna.finalbenchmark2.R.drawable.check_24)
                                     else painterResource(id = com.ivarna.finalbenchmark2.R.drawable.close_24),
                            contentDescription = if (isSupported) "Supported" else "Not Supported",
                            tint = if (isSupported) Color(0xFF4CAF50) else Color(0xFFEF5350),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
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