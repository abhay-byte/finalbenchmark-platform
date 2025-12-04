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
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ivarna.finalbenchmark2.ui.components.CpuUtilizationGraph
import com.ivarna.finalbenchmark2.ui.components.PowerConsumptionGraph
import com.ivarna.finalbenchmark2.ui.viewmodels.DeviceViewModel

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
    val powerHistory by viewModel?.powerHistory?.collectAsState() ?:
        remember { mutableStateOf(emptyList<com.ivarna.finalbenchmark2.ui.components.PowerDataPoint>()) }
    
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
        
        // Power Consumption Graph Card
        if (viewModel != null) {
            PowerConsumptionGraph(
                dataPoints = powerHistory,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
                
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
fun CpuTab(
    deviceInfo: com.ivarna.finalbenchmark2.utils.DeviceInfo,
    viewModel: DeviceViewModel
) {
    val cpuHistory by viewModel.cpuHistory.collectAsState()
    
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
                
        // CPU Utilization Graph Card
        CpuUtilizationGraph(
            dataPoints = cpuHistory,
            modifier = Modifier.fillMaxWidth()
        )
                
        Spacer(modifier = Modifier.height(16.dp))
                
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
        
        // Use the new GpuFrequencyCard component
        com.ivarna.finalbenchmark2.ui.components.GpuFrequencyCard()
        
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
                GpuInfoContent(gpuInfo)
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
fun GpuInfoContent(gpuInfo: com.ivarna.finalbenchmark2.utils.GpuInfo) {
    // GPU Overview Card
    GpuOverviewCard(gpuInfo.basicInfo, gpuInfo.frequencyInfo)
    
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
fun GpuOverviewCard(basicInfo: com.ivarna.finalbenchmark2.utils.GpuBasicInfo, frequencyInfo: com.ivarna.finalbenchmark2.utils.GpuFrequencyInfo?) {
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
                    text = "GPU Overview",
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
                
                InfoRow("GPU Name", basicInfo.name)
                InfoRow("Vendor", basicInfo.vendor)
                InfoRow("Driver Version", basicInfo.driverVersion)
                InfoRow("OpenGL ES", basicInfo.openGLVersion)
                InfoRow("Vulkan", basicInfo.vulkanVersion ?: "Not Supported")
                
                if (frequencyInfo != null) {
                    InfoRow("Current Frequency", if (frequencyInfo.currentFrequency != null) "${frequencyInfo.currentFrequency} MHz" else "N/A - Permission/Access Issue")
                    InfoRow("Max Frequency", if (frequencyInfo.maxFrequency != null) "${frequencyInfo.maxFrequency} MHz" else "N/A - Permission/Access Issue")
                } else {
                    InfoRow("Current Frequency", "N/A - Not Available")
                    InfoRow("Max Frequency", "N/A - Not Available")
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
                                InfoRow("Protected Memory", if (features.protectedMemory) "✓" else "✗")
                                InfoRow("Sampler YCbCr Conversion", if (features.samplerYcbcrConversion) "✓" else "✗")
                                InfoRow("Shader Draw Parameters", if (features.shaderDrawParameters) "✓" else "✗")
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
fun MemoryTab(deviceInfo: com.ivarna.finalbenchmark2.utils.DeviceInfo) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
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
                
        DeviceInfoCard("RAM Information") {
            InfoRow("Total RAM", formatBytes(deviceInfo.totalRam))
            InfoRow("Available RAM", formatBytes(deviceInfo.availableRam))
        }
                
        Spacer(modifier = Modifier.height(16.dp))
                
        DeviceInfoCard("Storage Information") {
            InfoRow("Total Storage", formatBytes(deviceInfo.totalStorage))
            InfoRow("Free Storage", formatBytes(deviceInfo.freeStorage))
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