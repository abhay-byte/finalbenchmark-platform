package com.ivarna.finalbenchmark2.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
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
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.ivarna.finalbenchmark2.R
import com.ivarna.finalbenchmark2.ui.theme.FinalBenchmark2Theme
import com.ivarna.finalbenchmark2.utils.CpuUtilizationUtils
import com.ivarna.finalbenchmark2.utils.DisplayUtils
import com.ivarna.finalbenchmark2.utils.formatBytes
import kotlin.math.roundToInt
import com.ivarna.finalbenchmark2.ui.components.CpuUtilizationGraph
import com.ivarna.finalbenchmark2.ui.components.GpuUtilizationGraph
import com.ivarna.finalbenchmark2.ui.components.GpuFrequencyCard
import com.ivarna.finalbenchmark2.ui.components.MemoryUsageGraph
import com.ivarna.finalbenchmark2.ui.components.PowerConsumptionGraph
import com.ivarna.finalbenchmark2.ui.components.SummaryCard
import com.ivarna.finalbenchmark2.ui.viewmodels.DeviceViewModel
import com.ivarna.finalbenchmark2.ui.viewmodels.GpuInfoViewModel
import com.ivarna.finalbenchmark2.ui.viewmodels.HardwareViewModel
import com.ivarna.finalbenchmark2.ui.viewmodels.OsViewModel

/**
 * Format bytes to megabytes
 */
fun formatBytesInMB(bytes: Long): String {
    if (bytes == 0L) return "0 MB"
    val mb = bytes.toDouble() / (1024.0 * 1024.0)
    return "${mb.roundToInt()} MB"
}

@OptIn(ExperimentalFoundationApi::class) // For Pager
@Composable
fun DeviceScreen(viewModel: DeviceViewModel = viewModel()) {
    val context = LocalContext.current
    val deviceInfo by viewModel.deviceInfo.collectAsState()
    
    // Initialize the ViewModel with context
    LaunchedEffect(context) {
        viewModel.updateDeviceInfo(context)
        viewModel.initialize(context)
    }
    
    // Cache the tabs list to prevent recreation on recomposition
    val tabs = remember {
        listOf(
            "Info",
            "CPU",
            "GPU",
            "Memory",
            "Screen",
            "OS",
            "Hardware",
            "Sensors"
        )
    }
    
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()
    
    FinalBenchmark2Theme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 1. Scrollable Tab Row
                ScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    edgePadding = 16.dp,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        if (pagerState.currentPage < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
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
                
                // 2. Swipeable Pager Area
                // Key Optimization: beyondViewportPageCount = 1 keeps memory usage low
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    beyondViewportPageCount = 1, 
                    userScrollEnabled = true
                ) { page ->
                    // Wrap content in Box to ensure proper layout within Pager
                    Box(modifier = Modifier.fillMaxSize()) {
                        when (page) {
                            0 -> InfoTab(deviceInfo, viewModel)
                            1 -> CpuTab(deviceInfo, viewModel)
                            2 -> GpuTab(deviceInfo)
                            3 -> MemoryTab(deviceInfo, viewModel)
                            4 -> ScreenTab(context)
                            5 -> OsTab(deviceInfo)
                            6 -> {
                                val hardwareViewModel: HardwareViewModel = viewModel()
                                HardwareTabContent(hardwareViewModel)
                            }
                            7 -> SensorsTab(context)
                        }
                    }
                }
            }
        }
    }
}

// --- Rest of your Composable functions (InfoTab, CpuTab, etc.) remain mostly identical ---
// --- They are implicitly optimized because HorizontalPager disposes of pages that are far away ---

@Composable
fun InfoTab(deviceInfo: com.ivarna.finalbenchmark2.utils.DeviceInfo, viewModel: DeviceViewModel? = null) {
    val context = LocalContext.current
    val powerHistory by viewModel?.powerHistory?.collectAsState() ?:
        remember { mutableStateOf(emptyList<com.ivarna.finalbenchmark2.ui.components.PowerDataPoint>()) }
    
    // Optimization: remember state to prevent unnecessary recreation
    val gpuInfoState by remember {
        mutableStateOf<com.ivarna.finalbenchmark2.utils.GpuInfoState>(com.ivarna.finalbenchmark2.utils.GpuInfoState.Loading)
    }
    
    LaunchedEffect(Unit) {
        val gpuInfoUtils = com.ivarna.finalbenchmark2.utils.GpuInfoUtils(context)
        gpuInfoUtils.getGpuInfo()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {
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
        
        if (viewModel != null) {
            PowerConsumptionGraph(
                dataPoints = powerHistory,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
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
            
            // GPU Section
            item {
                Text(
                    text = "GPU Information",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }
            
            item {
                val gpuInfoState by produceState<com.ivarna.finalbenchmark2.utils.GpuInfoState>(
                    initialValue = com.ivarna.finalbenchmark2.utils.GpuInfoState.Loading,
                    key1 = Unit
                ) {
                    val gpuInfoUtils = com.ivarna.finalbenchmark2.utils.GpuInfoUtils(context)
                    value = gpuInfoUtils.getGpuInfo()
                }
                
                when (gpuInfoState) {
                    is com.ivarna.finalbenchmark2.utils.GpuInfoState.Loading -> {
                        InfoRow("GPU Status", "Loading GPU information...")
                    }
                    is com.ivarna.finalbenchmark2.utils.GpuInfoState.Success -> {
                        val gpuInfo = (gpuInfoState as com.ivarna.finalbenchmark2.utils.GpuInfoState.Success).gpuInfo
                        InfoRow("GPU Name", gpuInfo.basicInfo.name)
                        InfoRow("GPU Vendor", gpuInfo.basicInfo.vendor)
                        InfoRow("OpenGL ES", gpuInfo.basicInfo.openGLVersion)
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
            
            // System Info
            item {
                Text(
                    text = "System Information",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }
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
    
    val cpuFreqUtils = remember { com.ivarna.finalbenchmark2.utils.CpuUtilizationUtils(context) }
    
    // Optimization: This producer now only runs when CpuTab is actually composed by the Pager
    val coreFrequencies by produceState<Map<Int, Pair<Long, Long>>>(
        initialValue = emptyMap(),
        key1 = cpuFreqUtils
    ) {
        while (true) {
            try {
                val freqs = cpuFreqUtils.getAllCoreFrequencies()
                value = freqs
            } catch (e: Exception) {
                value = emptyMap()
            }
            kotlinx.coroutines.delay(1000) 
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
            item {
                Text(
                    text = "CPU Architecture",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
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
            item {
                com.ivarna.finalbenchmark2.ui.components.InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Cluster Topology", deviceInfo.clusterTopology),
                    isLastItem = false
                )
            }
            
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
                deviceInfo.cpuFrequencies.forEach { (core, freq) ->
                    item {
                        com.ivarna.finalbenchmark2.ui.components.InformationRow(
                            itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Core $core", freq),
                            isLastItem = false
                        )
                    }
                }
            }
            
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
            
            val cpuNative = com.ivarna.finalbenchmark2.utils.CpuNativeBridge()
            val details = cpuNative.getCpuDetails()
            
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
            
            if (details.caches.isNotEmpty()) {
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
                    val name = "L${cache.level} ${cache.type.replaceFirstChar { it.uppercase() }}"
                    item {
                        com.ivarna.finalbenchmark2.ui.components.InformationRow(
                            itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text(name, cache.size),
                            isLastItem = false
                        )
                    }
                }
            }
            
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
            
            item {
                val governor = cpuFreqUtils.getCurrentCpuGovernor()
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

// ... (Rest of GpuTab, MemoryTab, ScreenTab, etc. remain the same as provided in your input, 
//      but will inherently perform better due to the HorizontalPager's lifecycle management) ...

@Composable
fun GpuTab(
    deviceInfo: com.ivarna.finalbenchmark2.utils.DeviceInfo,
    gpuViewModel: GpuInfoViewModel = viewModel()
) {
    val context = LocalContext.current
    
    val gpuHistory by gpuViewModel.gpuHistory.collectAsState()
    val gpuFrequencyState by gpuViewModel.gpuFrequencyState.collectAsState()
    
    val gpuInfoState by produceState<com.ivarna.finalbenchmark2.utils.GpuInfoState>(
        initialValue = com.ivarna.finalbenchmark2.utils.GpuInfoState.Loading,
        key1 = Unit
    ) {
        val gpuInfoUtils = com.ivarna.finalbenchmark2.utils.GpuInfoUtils(context)
        value = gpuInfoUtils.getGpuInfo()
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
        
        GpuUtilizationGraph(
            dataPoints = gpuHistory,
            modifier = Modifier.fillMaxWidth(),
            requiresRoot = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        GpuFrequencyCard(
            modifier = Modifier.fillMaxWidth(),
            viewModel = gpuViewModel
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
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

// Keep all your other existing Composable functions (GpuInfoContent, OpenGLInfoCard, MemoryTab, ScreenTab, etc.) 
// exactly as they were in your original code. They are correct, the Pager implementation above fixes the lag.
// I have omitted repeating the identical helper functions to save space, but ensure you keep them in the file.
// Specifically: GpuInfoContent, GpuOverviewCard, OpenGLInfoCard, VulkanInfoCard, AdvancedCapabilitiesCard,
// ScreenTab, DisplayMetricsCard, DisplayCapabilitiesCard, DisplaySystemStateCard, InfoRow, OsTab, MemoryTab,
// SensorsTab, VulkanFeaturesGrid, DeviceInfoCard