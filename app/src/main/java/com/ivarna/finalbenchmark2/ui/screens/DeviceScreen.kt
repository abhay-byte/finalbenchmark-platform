package com.ivarna.finalbenchmark2.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.haze
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.produceState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ivarna.finalbenchmark2.R
import com.ivarna.finalbenchmark2.ui.components.CpuUtilizationGraph
import com.ivarna.finalbenchmark2.ui.components.GpuFrequencyCard
import com.ivarna.finalbenchmark2.ui.components.GpuUtilizationGraph
import com.ivarna.finalbenchmark2.ui.components.MemoryUsageGraph
import com.ivarna.finalbenchmark2.ui.components.PowerConsumptionGraph
import com.ivarna.finalbenchmark2.ui.components.SummaryCard
import com.ivarna.finalbenchmark2.ui.theme.FinalBenchmark2Theme
import com.ivarna.finalbenchmark2.ui.viewmodels.DeviceViewModel
import com.ivarna.finalbenchmark2.ui.viewmodels.GpuInfoViewModel
import com.ivarna.finalbenchmark2.ui.viewmodels.HardwareViewModel
import com.ivarna.finalbenchmark2.ui.viewmodels.OsViewModel
import com.ivarna.finalbenchmark2.utils.CpuUtilizationUtils
import com.ivarna.finalbenchmark2.utils.DisplayUtils
import com.ivarna.finalbenchmark2.utils.formatBytes
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Format bytes to megabytes */
fun formatBytesInMB(bytes: Long): String {
        if (bytes == 0L) return "0 MB"
        val mb = bytes.toDouble() / (1024.0 * 1024.0)
        return "${mb.roundToInt()} MB"
}

@OptIn(
        ExperimentalMaterial3Api::class,
        androidx.compose.foundation.ExperimentalFoundationApi::class
)
@Composable
fun DeviceScreen(
    viewModel: DeviceViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
        val context = LocalContext.current
        val deviceInfo by viewModel.deviceInfo.collectAsState()
        
        // Local HazeState for this screen to avoid conflict with parent HazeState
        // Local HazeState for this screen to avoid conflict with parent HazeState
        val deviceScreenHazeState = remember { HazeState() }
        val blurBackgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)

        // Initialize the ViewModel with context
        LaunchedEffect(context) {
                viewModel.updateDeviceInfo(context)
                viewModel.initialize(context)
        }

        // Wrap tabs list in remember to prevent recreation on every recomposition
        val tabs = remember {
                listOf("Info", "CPU", "GPU", "Memory", "Screen", "OS", "Hardware", "Sensors")
        }

        val pagerState = rememberPagerState(pageCount = { tabs.size }, initialPage = 0)
        val coroutineScope = rememberCoroutineScope()

        FinalBenchmark2Theme {
                Box(
                        modifier =
                                Modifier.fillMaxSize()
                                        .background(MaterialTheme.colorScheme.background)
                                        .background(
                                                androidx.compose.ui.graphics.Brush.radialGradient(
                                                        colors =
                                                                listOf(
                                                                        MaterialTheme.colorScheme
                                                                                .primary.copy(
                                                                                alpha = 0.05f
                                                                        ),
                                                                        Color.Transparent
                                                                ),
                                                        center =
                                                                androidx.compose.ui.geometry.Offset(
                                                                        0f,
                                                                        0f
                                                                ),
                                                        radius = 1000f
                                                )
                                )) {
                        // Main Content Layer (Source for Blur)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .haze(state = deviceScreenHazeState)
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                    // Add spacer at top to push content below the floating tab bar
                                    Spacer(modifier = Modifier.height(90.dp))
                                    
                                    HorizontalPager(
                                            state = pagerState,
                                            userScrollEnabled = true,
                                            modifier = Modifier.weight(1f) // Fill remaining space
                                    ) { page ->
                                            when (page) {
                                                    0 -> InfoTab(deviceInfo, viewModel)
                                                    1 -> CpuTab(deviceInfo, viewModel)
                                                    2 -> GpuTab(deviceInfo)
                                                    3 -> MemoryTab(deviceInfo, viewModel)
                                                    4 -> ScreenTab(context)
                                                    5 -> OsTab(deviceInfo)
                                                    6 -> {
                                                            val hardwareViewModel: HardwareViewModel =
                                                                    viewModel()
                                                            HardwareTabContent(hardwareViewModel)
                                                    }
                                                    7 -> SensorsTab(context)
                                            }
                                    }
                            }
                        }

                        // Floating Tab Bar Layer (The Blur)
                        // Placed AFTER the content layout in the Box so it z-indexes on top
                        // Frosted Glass Tab Row Container
                        Box(
                                modifier = Modifier
                                        .align(Alignment.TopCenter) // Align to top
                                        .fillMaxWidth()
                                        .padding(
                                                start = 16.dp,
                                                end = 16.dp,
                                                top = 24.dp, 
                                                bottom = 8.dp
                                        )
                                        .shadow(
                                                elevation = 8.dp,
                                                shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                                                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                        )
                                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                                        .hazeChild(state = deviceScreenHazeState) {
                                                backgroundColor = blurBackgroundColor
                                                blurRadius = 30.dp
                                                noiseFactor = 0.05f
                                        }
                                        .border(
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
                                                shape = androidx.compose.foundation.shape.RoundedCornerShape(50)
                                        )
                        ) {
                                ScrollableTabRow(
                                                selectedTabIndex = pagerState.currentPage,
                                                edgePadding = 16.dp,
                                                containerColor = Color.Transparent,
                                                contentColor = MaterialTheme.colorScheme.primary,
                                                divider = {},
                                                indicator = { tabPositions ->
                                                        if (pagerState.currentPage <
                                                                         tabPositions.size
                                                        ) {
                                                                TabRowDefaults.SecondaryIndicator(
                                                                        modifier =
                                                                                Modifier.tabIndicatorOffset(
                                                                                        tabPositions[
                                                                                                 pagerState
                                                                                                         .currentPage]
                                                                                ),
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .primary,
                                                                        height = 3.dp
                                                                )
                                                        }
                                                }
                                        ) {
                                                tabs.forEachIndexed { index, title ->
                                                        Tab(
                                                                modifier = Modifier.height(58.dp),
                                                                selected = pagerState.currentPage == index,
                                                                onClick = {
                                                                    coroutineScope.launch {
                                                                        pagerState.animateScrollToPage(index)
                                                                    }
                                                                },
                                                                text = {
                                                                    val isSelected = pagerState.currentPage == index
                                                                    Text(
                                                                        text = title,
                                                                        maxLines = 1,
                                                                        style = if (isSelected)
                                                                            MaterialTheme.typography.titleSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                                                        else
                                                                            MaterialTheme.typography.bodyMedium,
                                                                        color = if (isSelected)
                                                                            MaterialTheme.colorScheme.primary
                                                                        else
                                                                            MaterialTheme.colorScheme.onSurfaceVariant
                                                                    )
                                                                }
                                                        )

                                        }
                                }


                        }
                }
        }
}

@Composable
fun InfoTab(
        deviceInfo: com.ivarna.finalbenchmark2.utils.DeviceInfo,
        viewModel: DeviceViewModel? = null
) {
        val context = LocalContext.current
        val powerHistory by
                viewModel?.powerHistory?.collectAsState()
                        ?: remember {
                                mutableStateOf(
                                        emptyList<
                                                com.ivarna.finalbenchmark2.ui.components.PowerDataPoint>()
                                )
                        }

        // Cache GPU info utilities to prevent recreation on every recomposition
        val gpuInfoUtils =
                remember(context) { com.ivarna.finalbenchmark2.utils.GpuInfoUtils(context) }

        // State for GPU info - use remember to prevent unnecessary recreation
        val gpuInfoState by remember {
                mutableStateOf<com.ivarna.finalbenchmark2.utils.GpuInfoState>(
                        com.ivarna.finalbenchmark2.utils.GpuInfoState.Loading
                )
        }

        // Load GPU info once when this composable enters composition
        LaunchedEffect(gpuInfoUtils) {
                // Use the cached gpuInfoUtils instance
                gpuInfoUtils.getGpuInfo()
        }

        Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Top
        ) {
                // Power Consumption Graph (at the top, scrolls with content)
                if (viewModel != null) {
                        PowerConsumptionGraph(
                                dataPoints = powerHistory,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        )
                }

                // Scrollable list for all device info
                LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 120.dp),
                        modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                        // --- Device Overview Section ---
                        item {
                                com.ivarna.finalbenchmark2.ui.components.GlassCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape =
                                                androidx.compose.foundation.shape
                                                        .RoundedCornerShape(24.dp)
                                ) {
                                        Column(modifier = Modifier.padding(20.dp)) {
                                                Text(
                                                        text = "Device Overview",
                                                        style =
                                                                MaterialTheme.typography.titleLarge
                                                                        .copy(
                                                                                fontWeight =
                                                                                        FontWeight
                                                                                                .Bold
                                                                        ),
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.padding(bottom = 16.dp)
                                                )

                                                com.ivarna.finalbenchmark2.ui.components
                                                        .InformationRow(
                                                                itemValue =
                                                                        com.ivarna.finalbenchmark2
                                                                                .domain.model
                                                                                .ItemValue.Text(
                                                                                "Model",
                                                                                "${deviceInfo.manufacturer} ${deviceInfo.deviceModel}"
                                                                        ),
                                                                isLastItem = false
                                                        )
                                                com.ivarna.finalbenchmark2.ui.components
                                                        .InformationRow(
                                                                itemValue =
                                                                        com.ivarna.finalbenchmark2
                                                                                .domain.model
                                                                                .ItemValue.Text(
                                                                                "Board",
                                                                                deviceInfo.board
                                                                        ),
                                                                isLastItem = false
                                                        )
                                                com.ivarna.finalbenchmark2.ui.components
                                                        .InformationRow(
                                                                itemValue =
                                                                        com.ivarna.finalbenchmark2
                                                                                .domain.model
                                                                                .ItemValue.Text(
                                                                                "SoC",
                                                                                deviceInfo.socName
                                                                        ),
                                                                isLastItem = false
                                                        )
                                                com.ivarna.finalbenchmark2.ui.components
                                                        .InformationRow(
                                                                itemValue =
                                                                        com.ivarna.finalbenchmark2
                                                                                .domain.model
                                                                                .ItemValue.Text(
                                                                                "Total RAM",
                                                                                formatBytes(
                                                                                        deviceInfo
                                                                                                .totalRam
                                                                                )
                                                                        ),
                                                                isLastItem = false
                                                        )
                                                com.ivarna.finalbenchmark2.ui.components
                                                        .InformationRow(
                                                                itemValue =
                                                                        com.ivarna.finalbenchmark2
                                                                                .domain.model
                                                                                .ItemValue.Text(
                                                                                "Total Storage",
                                                                                formatBytes(
                                                                                        deviceInfo
                                                                                                .totalStorage
                                                                                )
                                                                        ),
                                                                isLastItem = true
                                                        )
                                        }
                                }
                        }

                        // --- CPU Details Section ---
                        // Re-define CPU topology variables needed here
                        val cpuAffinityManager =
                                com.ivarna.finalbenchmark2.cpuBenchmark.CpuAffinityManager
                        val cpuCores = cpuAffinityManager.detectCpuTopology()
                        val bigCores =
                                cpuCores.filter {
                                        it.coreType ==
                                                com.ivarna
                                                        .finalbenchmark2
                                                        .cpuBenchmark
                                                        .CpuAffinityManager
                                                        .CoreType
                                                        .BIG
                                }
                        val midCores =
                                cpuCores.filter {
                                        it.coreType ==
                                                com.ivarna
                                                        .finalbenchmark2
                                                        .cpuBenchmark
                                                        .CpuAffinityManager
                                                        .CoreType
                                                        .MID
                                }
                        val littleCores =
                                cpuCores.filter {
                                        it.coreType ==
                                                com.ivarna
                                                        .finalbenchmark2
                                                        .cpuBenchmark
                                                        .CpuAffinityManager
                                                        .CoreType
                                                        .LITTLE
                                }

                        item {
                                com.ivarna.finalbenchmark2.ui.components.GlassCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape =
                                                androidx.compose.foundation.shape
                                                        .RoundedCornerShape(24.dp)
                                ) {
                                        Column(modifier = Modifier.padding(20.dp)) {
                                                Text(
                                                        text = "CPU Details",
                                                        style =
                                                                MaterialTheme.typography.titleLarge
                                                                        .copy(
                                                                                fontWeight =
                                                                                        FontWeight
                                                                                                .Bold
                                                                        ),
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.padding(bottom = 16.dp)
                                                )

                                                com.ivarna.finalbenchmark2.ui.components
                                                        .InformationRow(
                                                                itemValue =
                                                                        com.ivarna.finalbenchmark2
                                                                                .domain.model
                                                                                .ItemValue.Text(
                                                                                "Architecture",
                                                                                deviceInfo
                                                                                        .cpuArchitecture
                                                                        ),
                                                                isLastItem = false
                                                        )
                                                com.ivarna.finalbenchmark2.ui.components
                                                        .InformationRow(
                                                                itemValue =
                                                                        com.ivarna.finalbenchmark2
                                                                                .domain.model
                                                                                .ItemValue.Text(
                                                                                "Total Cores",
                                                                                deviceInfo
                                                                                        .totalCores
                                                                                        .toString()
                                                                        ),
                                                                isLastItem = false
                                                        )

                                                // Cluster topology logic included here for
                                                // completeness in "Info"
                                                // BIG cores
                                                if (bigCores.isNotEmpty()) {
                                                        val maxFreq =
                                                                bigCores.maxOfOrNull {
                                                                        it.maxFreqKhz
                                                                }
                                                                        ?: 0
                                                        com.ivarna.finalbenchmark2.ui.components
                                                                .InformationRow(
                                                                        itemValue =
                                                                                com.ivarna
                                                                                        .finalbenchmark2
                                                                                        .domain
                                                                                        .model
                                                                                        .ItemValue
                                                                                        .Text(
                                                                                                "BIG Cores",
                                                                                                "${bigCores.size} cores (${maxFreq/1000}MHz)"
                                                                                        ),
                                                                        isLastItem = false
                                                                )
                                                }

                                                // Mid cores
                                                if (midCores.isNotEmpty()) {
                                                        val maxFreq =
                                                                midCores.maxOfOrNull {
                                                                        it.maxFreqKhz
                                                                }
                                                                        ?: 0
                                                        com.ivarna.finalbenchmark2.ui.components
                                                                .InformationRow(
                                                                        itemValue =
                                                                                com.ivarna
                                                                                        .finalbenchmark2
                                                                                        .domain
                                                                                        .model
                                                                                        .ItemValue
                                                                                        .Text(
                                                                                                "Mid Cores",
                                                                                                "${midCores.size} cores (${maxFreq/1000}MHz)"
                                                                                        ),
                                                                        isLastItem = false
                                                                )
                                                }
                                                // LITTLE cores
                                                if (littleCores.isNotEmpty()) {
                                                        val maxFreq =
                                                                littleCores.maxOfOrNull {
                                                                        it.maxFreqKhz
                                                                }
                                                                        ?: 0
                                                        com.ivarna.finalbenchmark2.ui.components
                                                                .InformationRow(
                                                                        itemValue =
                                                                                com.ivarna
                                                                                        .finalbenchmark2
                                                                                        .domain
                                                                                        .model
                                                                                        .ItemValue
                                                                                        .Text(
                                                                                                "LITTLE Cores",
                                                                                                "${littleCores.size} cores (${maxFreq/1000}MHz)"
                                                                                        ),
                                                                        isLastItem = true
                                                                )
                                                }
                                        }
                                }
                        }

                        // --- GPU Details Section ---
                        item {
                                com.ivarna.finalbenchmark2.ui.components.GlassCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape =
                                                androidx.compose.foundation.shape
                                                        .RoundedCornerShape(24.dp)
                                ) {
                                        Column(modifier = Modifier.padding(20.dp)) {
                                                Text(
                                                        text = "GPU Information",
                                                        style =
                                                                MaterialTheme.typography.titleLarge
                                                                        .copy(
                                                                                fontWeight =
                                                                                        FontWeight
                                                                                                .Bold
                                                                        ),
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.padding(bottom = 16.dp)
                                                )

                                                // Load GPU info on demand for this specific tab
                                                // with proper caching
                                                // Use the shared gpuInfoState from parent scope

                                                when (gpuInfoState) {
                                                        is com.ivarna.finalbenchmark2.utils.GpuInfoState.Loading -> {
                                                                InfoRow(
                                                                        "GPU Status",
                                                                        "Loading GPU information..."
                                                                )
                                                        }
                                                        is com.ivarna.finalbenchmark2.utils.GpuInfoState.Success -> {
                                                                val gpuInfo =
                                                                        (gpuInfoState as
                                                                                        com.ivarna.finalbenchmark2.utils.GpuInfoState.Success)
                                                                                .gpuInfo
                                                                InfoRow(
                                                                        "GPU Name",
                                                                        gpuInfo.basicInfo.name
                                                                )
                                                                InfoRow(
                                                                        "GPU Vendor",
                                                                        gpuInfo.basicInfo.vendor
                                                                )
                                                                LongInfoRow(
                                                                        "OpenGL ES",
                                                                        gpuInfo.basicInfo
                                                                                .openGLVersion
                                                                )
                                                                // Display more detailed Vulkan info
                                                                // if available
                                                                if (gpuInfo.vulkanInfo != null &&
                                                                                gpuInfo.vulkanInfo
                                                                                        .supported
                                                                ) {
                                                                        val vulkanInfo =
                                                                                gpuInfo.vulkanInfo
                                                                        val vulkanVersion =
                                                                                vulkanInfo
                                                                                        .apiVersion
                                                                                        ?: "Supported"
                                                                        LongInfoRow(
                                                                                "Vulkan",
                                                                                vulkanVersion
                                                                        )
                                                                } else if (gpuInfo.basicInfo
                                                                                .vulkanVersion !=
                                                                                null
                                                                ) {
                                                                        LongInfoRow(
                                                                                "Vulkan",
                                                                                gpuInfo.basicInfo
                                                                                        .vulkanVersion
                                                                        )
                                                                } else {
                                                                        InfoRow(
                                                                                "Vulkan",
                                                                                "Not Supported"
                                                                        )
                                                                }
                                                        }
                                                        is com.ivarna.finalbenchmark2.utils.GpuInfoState.Error -> {
                                                                InfoRow(
                                                                        "GPU Status",
                                                                        "Error loading GPU info"
                                                                )
                                                                InfoRow(
                                                                        "GPU Error",
                                                                        (gpuInfoState as
                                                                                        com.ivarna.finalbenchmark2.utils.GpuInfoState.Error)
                                                                                .message
                                                                )
                                                        }
                                                }
                                        }
                                }
                        }

                        // --- System Information Section ---
                        item {
                                com.ivarna.finalbenchmark2.ui.components.GlassCard(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                        shape =
                                                androidx.compose.foundation.shape
                                                        .RoundedCornerShape(24.dp)
                                ) {
                                        Column(modifier = Modifier.padding(20.dp)) {
                                                Text(
                                                        text = "System Information",
                                                        style =
                                                                MaterialTheme.typography.titleLarge
                                                                        .copy(
                                                                                fontWeight =
                                                                                        FontWeight
                                                                                                .Bold
                                                                        ),
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.padding(bottom = 16.dp)
                                                )

                                                com.ivarna.finalbenchmark2.ui.components
                                                        .InformationRow(
                                                                itemValue =
                                                                        com.ivarna.finalbenchmark2
                                                                                .domain.model
                                                                                .ItemValue.Text(
                                                                                "Android Version",
                                                                                "${deviceInfo.androidVersion} (API ${deviceInfo.apiLevel})"
                                                                        ),
                                                                isLastItem = false
                                                        )
                                                com.ivarna.finalbenchmark2.ui.components
                                                        .InformationRow(
                                                                itemValue =
                                                                        com.ivarna.finalbenchmark2
                                                                                .domain.model
                                                                                .ItemValue.Text(
                                                                                "Kernel Version",
                                                                                deviceInfo
                                                                                        .kernelVersion
                                                                        ),
                                                                isLastItem = false
                                                        )
                                                com.ivarna.finalbenchmark2.ui.components
                                                        .InformationRow(
                                                                itemValue =
                                                                        com.ivarna.finalbenchmark2
                                                                                .domain.model
                                                                                .ItemValue.Text(
                                                                                "Thermal Status",
                                                                                deviceInfo
                                                                                        .thermalStatus
                                                                                        ?: "Not available"
                                                                        ),
                                                                isLastItem = false
                                                        )
                                                com.ivarna.finalbenchmark2.ui.components
                                                        .InformationRow(
                                                                itemValue =
                                                                        com.ivarna.finalbenchmark2
                                                                                .domain.model
                                                                                .ItemValue.Text(
                                                                                "Battery Temperature",
                                                                                deviceInfo
                                                                                        .batteryTemperature
                                                                                        ?.let {
                                                                                                "${String.format("%.2f", it)}°C"
                                                                                        }
                                                                                        ?: "Not available"
                                                                        ),
                                                                isLastItem = false
                                                        )
                                                com.ivarna.finalbenchmark2.ui.components
                                                        .InformationRow(
                                                                itemValue =
                                                                        com.ivarna.finalbenchmark2
                                                                                .domain.model
                                                                                .ItemValue.Text(
                                                                                "Battery Capacity",
                                                                                deviceInfo
                                                                                        .batteryCapacity
                                                                                        ?.let {
                                                                                                "${it.toInt()}%"
                                                                                        }
                                                                                        ?: "Not available"
                                                                        ),
                                                                isLastItem = true
                                                        )
                                        }
                                }
                        }
                }
        }
}

@Composable
fun CpuTab(deviceInfo: com.ivarna.finalbenchmark2.utils.DeviceInfo, viewModel: DeviceViewModel) {
        val cpuHistory by viewModel.cpuHistory.collectAsState()
        val context = LocalContext.current

        // Initialize CPU frequency utilities once and cache them
        val cpuFreqUtils = remember {
                com.ivarna.finalbenchmark2.utils.CpuUtilizationUtils(context)
        }

        val cpuAffinityManager = com.ivarna.finalbenchmark2.cpuBenchmark.CpuAffinityManager
        val cpuCores = cpuAffinityManager.detectCpuTopology()
        val bigCores =
                cpuCores.filter {
                        it.coreType ==
                                com.ivarna
                                        .finalbenchmark2
                                        .cpuBenchmark
                                        .CpuAffinityManager
                                        .CoreType
                                        .BIG
                }
        val midCores =
                cpuCores.filter {
                        it.coreType ==
                                com.ivarna
                                        .finalbenchmark2
                                        .cpuBenchmark
                                        .CpuAffinityManager
                                        .CoreType
                                        .MID
                }
        val littleCores =
                cpuCores.filter {
                        it.coreType ==
                                com.ivarna
                                        .finalbenchmark2
                                        .cpuBenchmark
                                        .CpuAffinityManager
                                        .CoreType
                                        .LITTLE
                }

        // Optimized data fetching scoped to this specific tab - stops when tab is not visible
        val coreFrequencies by
                produceState<Map<Int, Pair<Long, Long>>>(
                        initialValue = emptyMap(),
                        key1 = cpuFreqUtils,
                        key2 = context // Include context as key for proper scoping
                ) {
                        // Create a dedicated coroutine scope for this tab's data fetching
                        val scope =
                                kotlinx.coroutines.CoroutineScope(
                                        kotlinx.coroutines.Dispatchers.IO +
                                                kotlinx.coroutines.SupervisorJob()
                                )

                        // Start data fetching with proper cleanup when composable leaves
                        // composition
                        scope.launch {
                                try {
                                        // Update frequencies periodically with shorter delay for
                                        // better
                                        // responsiveness
                                        while (isActive) {
                                                try {
                                                        val freqs =
                                                                cpuFreqUtils.getAllCoreFrequencies()
                                                        value = freqs
                                                } catch (e: Exception) {
                                                        // Handle any errors silently to avoid app
                                                        // crashes
                                                        value = emptyMap()
                                                }
                                                kotlinx.coroutines.delay(
                                                        2000
                                                ) // Increased interval to reduce CPU usage
                                        }
                                } catch (e: CancellationException) {
                                        // Properly handle cancellation when tab is no longer
                                        // visible
                                        throw e
                                }
                        }

                        // Cleanup when composable is disposed
                        awaitDispose { scope.cancel() }
                }

        Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Top
        ) {
                Text(
                        text = "CPU Information",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )

                // CPU Utilization Graph
                CpuUtilizationGraph(dataPoints = cpuHistory, modifier = Modifier.fillMaxWidth())

                Spacer(modifier = Modifier.height(16.dp))
                
                Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                ) {
                        // CPU Architecture Section

                        com.ivarna.finalbenchmark2.ui.components.GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
                        ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                        Text(
                                                text = "CPU Architecture",
                                                style =
                                                        MaterialTheme.typography.titleLarge.copy(
                                                                fontWeight = FontWeight.Bold
                                                        ),
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(bottom = 16.dp)
                                        )

                                        com.ivarna.finalbenchmark2.ui.components.InformationRow(
                                                itemValue =
                                                        com.ivarna.finalbenchmark2.domain.model
                                                                .ItemValue.Text(
                                                                "Architecture",
                                                                deviceInfo.cpuArchitecture
                                                        ),
                                                isLastItem = false
                                        )
                                        com.ivarna.finalbenchmark2.ui.components.InformationRow(
                                                itemValue =
                                                        com.ivarna.finalbenchmark2.domain.model
                                                                .ItemValue.Text(
                                                                "Total Cores",
                                                                deviceInfo.totalCores.toString()
                                                        ),
                                                isLastItem = true
                                        )
                                }
                        }
                        }

                        // Cluster Topology

                        com.ivarna.finalbenchmark2.ui.components.GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
                        ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                        Text(
                                                text = "Core Configuration",
                                                style =
                                                        MaterialTheme.typography.titleLarge.copy(
                                                                fontWeight = FontWeight.Bold
                                                        ),
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(bottom = 16.dp)
                                        )

                                        // BIG cores
                                        if (bigCores.isNotEmpty()) {
                                                val maxFreq =
                                                        bigCores.maxOfOrNull { it.maxFreqKhz } ?: 0
                                                val coreIds =
                                                        bigCores.map { it.id }.joinToString(", ")
                                                com.ivarna.finalbenchmark2.ui.components
                                                        .InformationRow(
                                                                itemValue =
                                                                        com.ivarna.finalbenchmark2
                                                                                .domain.model
                                                                                .ItemValue.Text(
                                                                                "BIG Cores",
                                                                                "${bigCores.size} cores (${maxFreq/1000}MHz) - IDs: $coreIds"
                                                                        ),
                                                                isLastItem = false
                                                        )
                                        }

                                        // Mid cores (if available)
                                        if (midCores.isNotEmpty()) {
                                                val maxFreq =
                                                        midCores.maxOfOrNull { it.maxFreqKhz } ?: 0
                                                val coreIds =
                                                        midCores.map { it.id }.joinToString(", ")
                                                com.ivarna.finalbenchmark2.ui.components
                                                        .InformationRow(
                                                                itemValue =
                                                                        com.ivarna.finalbenchmark2
                                                                                .domain.model
                                                                                .ItemValue.Text(
                                                                                "Mid Cores",
                                                                                "${midCores.size} cores (${maxFreq/1000}MHz) - IDs: $coreIds"
                                                                        ),
                                                                isLastItem = false
                                                        )
                                        }

                                        // LITTLE cores
                                        if (littleCores.isNotEmpty()) {
                                                val maxFreq =
                                                        littleCores.maxOfOrNull { it.maxFreqKhz }
                                                                ?: 0
                                                val coreIds =
                                                        littleCores.map { it.id }.joinToString(", ")
                                                com.ivarna.finalbenchmark2.ui.components
                                                        .InformationRow(
                                                                itemValue =
                                                                        com.ivarna.finalbenchmark2
                                                                                .domain.model
                                                                                .ItemValue.Text(
                                                                                "LITTLE Cores",
                                                                                "${littleCores.size} cores (${maxFreq/1000}MHz) - IDs: $coreIds"
                                                                        ),
                                                                isLastItem = false
                                                        )
                                        }

                                        // Topology text
                                        Column(
                                                modifier =
                                                        Modifier.fillMaxWidth().padding(top = 8.dp)
                                        ) {
                                                Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement =
                                                                Arrangement.SpaceBetween
                                                ) {
                                                        Text(
                                                                text = "Topology",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodyLarge,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurface,
                                                                modifier = Modifier.weight(0.4f)
                                                        )
                                                        Text(
                                                                text = deviceInfo.clusterTopology,
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodyLarge,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onBackground,
                                                                modifier =
                                                                        Modifier.weight(0.6f)
                                                                                .padding(
                                                                                        start =
                                                                                                16.dp
                                                                                ),
                                                                textAlign =
                                                                        androidx.compose.ui.text
                                                                                .style.TextAlign
                                                                                .End,
                                                                softWrap = true
                                                        )
                                                }
                                        }
                                }
                        }

                        // CPU Frequencies Section

                        com.ivarna.finalbenchmark2.ui.components.GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
                        ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                        Text(
                                                text = "Real-time Frequencies",
                                                style =
                                                        MaterialTheme.typography.titleLarge.copy(
                                                                fontWeight = FontWeight.Bold
                                                        ),
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(bottom = 16.dp)
                                        )

                                        // Real-time CPU Frequencies items
                                        if (coreFrequencies.isNotEmpty()) {
                                                coreFrequencies.forEach { (coreIndex, freqPair) ->
                                                        val (currentFreq, maxFreq) = freqPair
                                                        val currentFreqMhz =
                                                                if (currentFreq > 0)
                                                                        "${currentFreq / 1000} MHz"
                                                                else "Offline"
                                                        val maxFreqMhz =
                                                                if (maxFreq > 0)
                                                                        "${maxFreq / 1000} MHz"
                                                                else "N/A"

                                                        com.ivarna.finalbenchmark2.ui.components
                                                                .InformationRow(
                                                                        itemValue =
                                                                                com.ivarna
                                                                                        .finalbenchmark2
                                                                                        .domain
                                                                                        .model
                                                                                        .ItemValue
                                                                                        .Text(
                                                                                                "Core $coreIndex",
                                                                                                "$currentFreqMhz / $maxFreqMhz"
                                                                                        ),
                                                                        isLastItem =
                                                                                coreIndex ==
                                                                                        coreFrequencies
                                                                                                .size -
                                                                                                1
                                                                )
                                                }
                                        } else {
                                                // Fallback to static frequencies
                                                val sortedKeys =
                                                        deviceInfo.cpuFrequencies.keys.sorted()
                                                sortedKeys.forEachIndexed { index, core ->
                                                        val freq =
                                                                deviceInfo.cpuFrequencies[core]
                                                                        ?: ""
                                                        com.ivarna.finalbenchmark2.ui.components
                                                                .InformationRow(
                                                                        itemValue =
                                                                                com.ivarna
                                                                                        .finalbenchmark2
                                                                                        .domain
                                                                                        .model
                                                                                        .ItemValue
                                                                                        .Text(
                                                                                                "Core $core",
                                                                                                freq
                                                                                        ),
                                                                        isLastItem =
                                                                                index ==
                                                                                        sortedKeys
                                                                                                .size -
                                                                                                1
                                                                )
                                                }
                                        }
                                }
                        }

                        // Processor Details Section

                        com.ivarna.finalbenchmark2.ui.components.GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
                        ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                        Text(
                                                text = "Processor Details",
                                                style =
                                                        MaterialTheme.typography.titleLarge.copy(
                                                                fontWeight = FontWeight.Bold
                                                        ),
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(bottom = 16.dp)
                                        )

                                        com.ivarna.finalbenchmark2.ui.components.InformationRow(
                                                itemValue =
                                                        com.ivarna.finalbenchmark2.domain.model
                                                                .ItemValue.Text(
                                                                "SoC Name",
                                                                deviceInfo.socName
                                                        ),
                                                isLastItem = false
                                        )
                                        com.ivarna.finalbenchmark2.ui.components.InformationRow(
                                                itemValue =
                                                        com.ivarna.finalbenchmark2.domain.model
                                                                .ItemValue.Text(
                                                                "Architecture",
                                                                deviceInfo.cpuArchitecture
                                                        ),
                                                isLastItem = false
                                        )
                                        com.ivarna.finalbenchmark2.ui.components.InformationRow(
                                                itemValue =
                                                        com.ivarna.finalbenchmark2.domain.model
                                                                .ItemValue.Text(
                                                                "Manufacturer",
                                                                deviceInfo.manufacturer
                                                        ),
                                                isLastItem = false
                                        )
                                        com.ivarna.finalbenchmark2.ui.components.InformationRow(
                                                itemValue =
                                                        com.ivarna.finalbenchmark2.domain.model
                                                                .ItemValue.Text(
                                                                "Board",
                                                                deviceInfo.board
                                                        ),
                                                isLastItem = true
                                        )
                                }
                        }

                        // CPU Governor Section

                        com.ivarna.finalbenchmark2.ui.components.GlassCard(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
                        ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                        text = "Power Management",
                                        style =
                                                MaterialTheme.typography.titleLarge.copy(
                                                        fontWeight = FontWeight.Bold
                                                ),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(bottom = 16.dp)
                                )

                                val governor = cpuFreqUtils.getCurrentCpuGovernor()

                                com.ivarna.finalbenchmark2.ui.components.InformationRow(
                                        itemValue =
                                                com.ivarna.finalbenchmark2.domain.model.ItemValue
                                                        .Text(
                                                                "Current Governor",
                                                                governor ?: "Not available"
                                                        ),
                                        isLastItem = true
                                )
                                }
                        }

                        Spacer(modifier = Modifier.height(120.dp)) // Bottom padding for floating nav bar


        }
}

@Composable
fun GpuTab(
        deviceInfo: com.ivarna.finalbenchmark2.utils.DeviceInfo,
        gpuViewModel: GpuInfoViewModel = viewModel()
) {
        val context = LocalContext.current

        val gpuHistory by gpuViewModel.gpuHistory.collectAsState()
        val gpuFrequencyState by gpuViewModel.gpuFrequencyState.collectAsState()

        // Cache GPU info utilities to prevent recreation on every recomposition
        val gpuInfoUtils =
                remember(context) { com.ivarna.finalbenchmark2.utils.GpuInfoUtils(context) }

        // Load GPU info once when this composable enters composition with proper caching
        val gpuInfoState by
                produceState<com.ivarna.finalbenchmark2.utils.GpuInfoState>(
                        initialValue = com.ivarna.finalbenchmark2.utils.GpuInfoState.Loading,
                        key1 = gpuInfoUtils
                ) {
                        // Use the cached gpuInfoUtils instance
                        value = gpuInfoUtils.getGpuInfo()
                }

        Column(
                modifier =
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Top
        ) {
                Text(
                        text = "GPU Information",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )

                // GPU Utilization Graph
                GpuUtilizationGraph(
                        dataPoints = gpuHistory,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        requiresRoot = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // GPU Frequency Card - Real-time frequency monitoring
                GpuFrequencyCard(modifier = Modifier.fillMaxWidth(), viewModel = gpuViewModel)

                Spacer(modifier = Modifier.height(16.dp))

                // GPU Info Content based on state
                when (gpuInfoState) {
                        is com.ivarna.finalbenchmark2.utils.GpuInfoState.Loading -> {
                                DeviceInfoCard("GPU Information") {
                                        InfoRow("Status", "Loading GPU information...")
                                }
                        }
                        is com.ivarna.finalbenchmark2.utils.GpuInfoState.Success -> {
                                val gpuInfo =
                                        (gpuInfoState as
                                                        com.ivarna.finalbenchmark2.utils.GpuInfoState.Success)
                                                .gpuInfo
                                GpuInfoContent(gpuInfo, gpuFrequencyState)
                        }
                        is com.ivarna.finalbenchmark2.utils.GpuInfoState.Error -> {
                                DeviceInfoCard("GPU Information") {
                                        InfoRow("Status", "Error loading GPU info")
                                        InfoRow(
                                                "Error",
                                                (gpuInfoState as
                                                                com.ivarna.finalbenchmark2.utils.GpuInfoState.Error)
                                                        .message
                                        )
                                }
                        }
                }

                Spacer(modifier = Modifier.height(120.dp))
        }
}

@Composable
fun GpuInfoContent(
        gpuInfo: com.ivarna.finalbenchmark2.utils.GpuInfo,
        gpuFrequencyState: com.ivarna.finalbenchmark2.utils.GpuFrequencyReader.GpuFrequencyState =
                com.ivarna.finalbenchmark2.utils.GpuFrequencyReader.GpuFrequencyState.NotSupported
) {
        Column(modifier = Modifier.fillMaxWidth()) {
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
}

@Composable
fun GpuOverviewCard(
        gpuInfo: com.ivarna.finalbenchmark2.utils.GpuInfo,
        gpuFrequencyState: com.ivarna.finalbenchmark2.utils.GpuFrequencyReader.GpuFrequencyState =
                com.ivarna.finalbenchmark2.utils.GpuFrequencyReader.GpuFrequencyState.NotSupported
) {
        val basicInfo = gpuInfo.basicInfo
        val frequencyInfo = gpuInfo.frequencyInfo
        var expanded by remember { mutableStateOf(false) }

        com.ivarna.finalbenchmark2.ui.components.GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                onClick = { expanded = !expanded }
        ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
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
                                                        val maxFreq =
                                                                gpuFrequencyState
                                                                        .data
                                                                        .maxFrequencyMhz
                                                        if (maxFreq != null) {
                                                                Text(
                                                                        text =
                                                                                "Max: ${maxFreq} MHz",
                                                                        fontSize = 14.sp,
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onSurfaceVariant
                                                                )
                                                        }
                                                }
                                                com.ivarna.finalbenchmark2.utils.GpuFrequencyReader
                                                        .GpuFrequencyState.NotSupported -> {
                                                        if (frequencyInfo != null &&
                                                                        frequencyInfo
                                                                                .maxFrequency !=
                                                                                null
                                                        ) {
                                                                Text(
                                                                        text =
                                                                                "Max: ${frequencyInfo.maxFrequency} MHz",
                                                                        fontSize = 14.sp,
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onSurfaceVariant
                                                                )
                                                        }
                                                }
                                                else -> {
                                                        // No additional info in header for other
                                                        // states
                                                }
                                        }
                                }
                                IconButton(onClick = { expanded = !expanded }) {
                                        Icon(
                                                imageVector =
                                                        if (expanded) Icons.Default.ExpandLess
                                                        else Icons.Default.ExpandMore,
                                                contentDescription =
                                                        if (expanded) "Collapse" else "Expand"
                                        )
                                }
                        }

                        if (expanded) {
                                Spacer(modifier = Modifier.height(8.dp))

                                InfoRow("GPU Name", basicInfo.name)
                                InfoRow("Vendor", basicInfo.vendor)
                                InfoRow("Driver Version", basicInfo.driverVersion)
                                LongInfoRow("OpenGL ES", basicInfo.openGLVersion)
                                // Display more detailed Vulkan info if available
                                if (gpuInfo.vulkanInfo != null && gpuInfo.vulkanInfo.supported) {
                                        val vulkanInfo = gpuInfo.vulkanInfo
                                        val vulkanVersion = vulkanInfo.apiVersion ?: "Supported"
                                        LongInfoRow("Vulkan", vulkanVersion)
                                } else if (basicInfo.vulkanVersion != null) {
                                        LongInfoRow("Vulkan", basicInfo.vulkanVersion)
                                } else {
                                        InfoRow("Vulkan", "Not Supported")
                                }

                                // Display frequency info from the frequency state if available,
                                // otherwise use
                                // static info
                                when (gpuFrequencyState) {
                                        is com.ivarna.finalbenchmark2.utils.GpuFrequencyReader.GpuFrequencyState.Available -> {
                                                val data = gpuFrequencyState.data
                                                InfoRow(
                                                        "Current Frequency",
                                                        "${data.currentFrequencyMhz} MHz"
                                                )
                                                data.maxFrequencyMhz?.let { maxFreq ->
                                                        InfoRow("Max Frequency", "${maxFreq} MHz")
                                                }
                                                data.minFrequencyMhz?.let { minFreq ->
                                                        InfoRow("Min Frequency", "${minFreq} MHz")
                                                }
                                                data.governor?.let { governor ->
                                                        InfoRow("Governor", governor)
                                                }
                                                data.utilizationPercent?.let { utilization ->
                                                        InfoRow("Utilization", "${utilization}%")
                                                }
                                        }
                                        com.ivarna.finalbenchmark2.utils.GpuFrequencyReader
                                                .GpuFrequencyState.RequiresRoot -> {
                                                InfoRow("Current Frequency", "Requires Root Access")
                                                InfoRow("Max Frequency", "Requires Root Access")
                                        }
                                        com.ivarna.finalbenchmark2.utils.GpuFrequencyReader
                                                .GpuFrequencyState.NotSupported -> {
                                                // If real-time data not available, fallback to
                                                // static frequency info
                                                if (frequencyInfo != null) {
                                                        InfoRow(
                                                                "Current Frequency",
                                                                if (frequencyInfo
                                                                                .currentFrequency !=
                                                                                null
                                                                )
                                                                        "${frequencyInfo.currentFrequency} MHz"
                                                                else "N/A - Permission/Access Issue"
                                                        )
                                                        InfoRow(
                                                                "Max Frequency",
                                                                if (frequencyInfo.maxFrequency !=
                                                                                null
                                                                )
                                                                        "${frequencyInfo.maxFrequency} MHz"
                                                                else "N/A - Permission/Access Issue"
                                                        )
                                                } else {
                                                        InfoRow(
                                                                "Current Frequency",
                                                                "N/A - Not Available"
                                                        )
                                                        InfoRow(
                                                                "Max Frequency",
                                                                "N/A - Not Available"
                                                        )
                                                }
                                        }
                                        is com.ivarna.finalbenchmark2.utils.GpuFrequencyReader.GpuFrequencyState.Error -> {
                                                InfoRow(
                                                        "Current Frequency",
                                                        "Error: ${gpuFrequencyState.message}"
                                                )
                                                InfoRow(
                                                        "Max Frequency",
                                                        "Error: ${gpuFrequencyState.message}"
                                                )
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

        com.ivarna.finalbenchmark2.ui.components.GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                onClick = { expanded = !expanded }
        ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
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
                                                imageVector =
                                                        if (expanded) Icons.Default.ExpandLess
                                                        else Icons.Default.ExpandMore,
                                                contentDescription =
                                                        if (expanded) "Collapse" else "Expand"
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
                                                modifier =
                                                        Modifier.fillMaxWidth().padding(top = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                        ) {
                                                Row(
                                                        verticalAlignment =
                                                                Alignment.CenterVertically,
                                                        horizontalArrangement =
                                                                Arrangement.spacedBy(8.dp)
                                                ) {
                                                        Text(
                                                                text = "OpenGL Extensions:",
                                                                fontWeight = FontWeight.Medium
                                                        )
                                                        Surface(
                                                                shape = MaterialTheme.shapes.small,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .primaryContainer
                                                        ) {
                                                                Text(
                                                                        text =
                                                                                "${openGLInfo.extensions.size}",
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .labelSmall,
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onPrimaryContainer,
                                                                        modifier =
                                                                                Modifier.padding(
                                                                                        horizontal =
                                                                                                8.dp,
                                                                                        vertical =
                                                                                                2.dp
                                                                                ),
                                                                        fontWeight =
                                                                                FontWeight.SemiBold
                                                                )
                                                        }
                                                }
                                                IconButton(
                                                        onClick = {
                                                                extensionsExpanded =
                                                                        !extensionsExpanded
                                                        }
                                                ) {
                                                        Icon(
                                                                imageVector =
                                                                        if (extensionsExpanded)
                                                                                Icons.Default
                                                                                        .ExpandLess
                                                                        else
                                                                                Icons.Default
                                                                                        .ExpandMore,
                                                                contentDescription =
                                                                        if (extensionsExpanded)
                                                                                "Hide"
                                                                        else "Show"
                                                        )
                                                }
                                        }

                                        if (extensionsExpanded) {
                                                Column(
                                                        modifier =
                                                                Modifier.fillMaxWidth()
                                                                        .heightIn(max = 200.dp)
                                                                        .verticalScroll(
                                                                                rememberScrollState()
                                                                        )
                                                                        .background(
                                                                                color =
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .surface,
                                                                                shape =
                                                                                        MaterialTheme
                                                                                                .shapes
                                                                                                .small
                                                                        )
                                                                        .padding(8.dp)
                                                ) {
                                                        openGLInfo.extensions.forEach { extension ->
                                                                Text(
                                                                        text = extension,
                                                                        fontFamily =
                                                                                androidx.compose.ui
                                                                                        .text.font
                                                                                        .FontFamily
                                                                                        .Monospace,
                                                                        fontSize = 12.sp,
                                                                        modifier =
                                                                                Modifier.padding(
                                                                                        vertical =
                                                                                                2.dp
                                                                                )
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

        com.ivarna.finalbenchmark2.ui.components.GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                onClick = { expanded = !expanded }
        ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
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
                                                imageVector =
                                                        if (expanded) Icons.Default.ExpandLess
                                                        else Icons.Default.ExpandMore,
                                                contentDescription =
                                                        if (expanded) "Collapse" else "Expand"
                                        )
                                }
                        }

                        if (expanded) {
                                Spacer(modifier = Modifier.height(8.dp))

                                InfoRow("Supported", if (vulkanInfo.supported) "Yes" else "No")
                                if (vulkanInfo.supported) {
                                        vulkanInfo.apiVersion?.let { InfoRow("API Version", it) }
                                        vulkanInfo.driverVersion?.let {
                                                InfoRow("Driver Version", it)
                                        }
                                        vulkanInfo.physicalDeviceName?.let {
                                                InfoRow("Physical Device", it)
                                        }
                                        vulkanInfo.physicalDeviceType?.let {
                                                InfoRow("Device Type", it)
                                        }

                                        // Extensions
                                        val totalExtensions =
                                                vulkanInfo.instanceExtensions.size +
                                                        vulkanInfo.deviceExtensions.size
                                        InfoRow("Total Extensions", "$totalExtensions extensions")

                                        // Extensions list
                                        if (totalExtensions > 0) {
                                                Row(
                                                        modifier =
                                                                Modifier.fillMaxWidth()
                                                                        .padding(top = 8.dp),
                                                        horizontalArrangement =
                                                                Arrangement.SpaceBetween,
                                                        verticalAlignment =
                                                                Alignment.CenterVertically
                                                ) {
                                                        Row(
                                                                verticalAlignment =
                                                                        Alignment.CenterVertically,
                                                                horizontalArrangement =
                                                                        Arrangement.spacedBy(8.dp)
                                                        ) {
                                                                Text(
                                                                        text = "Vulkan Extensions:",
                                                                        fontWeight =
                                                                                FontWeight.Medium
                                                                )
                                                                Surface(
                                                                        shape =
                                                                                MaterialTheme.shapes
                                                                                        .small,
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .primaryContainer
                                                                ) {
                                                                        Text(
                                                                                text =
                                                                                        "$totalExtensions",
                                                                                style =
                                                                                        MaterialTheme
                                                                                                .typography
                                                                                                .labelSmall,
                                                                                color =
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .onPrimaryContainer,
                                                                                modifier =
                                                                                        Modifier.padding(
                                                                                                horizontal =
                                                                                                        8.dp,
                                                                                                vertical =
                                                                                                        2.dp
                                                                                        ),
                                                                                fontWeight =
                                                                                        FontWeight
                                                                                                .SemiBold
                                                                        )
                                                                }
                                                        }
                                                        IconButton(
                                                                onClick = {
                                                                        extensionsExpanded =
                                                                                !extensionsExpanded
                                                                }
                                                        ) {
                                                                Icon(
                                                                        imageVector =
                                                                                if (extensionsExpanded
                                                                                )
                                                                                        Icons.Default
                                                                                                .ExpandLess
                                                                                else
                                                                                        Icons.Default
                                                                                                .ExpandMore,
                                                                        contentDescription =
                                                                                if (extensionsExpanded
                                                                                )
                                                                                        "Hide"
                                                                                else "Show"
                                                                )
                                                        }
                                                }

                                                if (extensionsExpanded) {
                                                        Column(
                                                                modifier =
                                                                        Modifier.fillMaxWidth()
                                                                                .heightIn(
                                                                                        max = 200.dp
                                                                                )
                                                                                .verticalScroll(
                                                                                        rememberScrollState()
                                                                                )
                                                                                .background(
                                                                                        color =
                                                                                                MaterialTheme
                                                                                                        .colorScheme
                                                                                                        .surface,
                                                                                        shape =
                                                                                                MaterialTheme
                                                                                                        .shapes
                                                                                                        .small
                                                                                )
                                                                                .padding(8.dp)
                                                        ) {
                                                                // Instance extensions
                                                                if (vulkanInfo.instanceExtensions
                                                                                .isNotEmpty()
                                                                ) {
                                                                        Text(
                                                                                text =
                                                                                        "Instance Extensions:",
                                                                                fontWeight =
                                                                                        FontWeight
                                                                                                .Bold,
                                                                                color =
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .primary,
                                                                                modifier =
                                                                                        Modifier.padding(
                                                                                                bottom =
                                                                                                        4.dp
                                                                                        )
                                                                        )
                                                                        vulkanInfo
                                                                                .instanceExtensions
                                                                                .forEach { extension
                                                                                        ->
                                                                                        Text(
                                                                                                text =
                                                                                                        extension,
                                                                                                fontFamily =
                                                                                                        androidx.compose
                                                                                                                .ui
                                                                                                                .text
                                                                                                                .font
                                                                                                                .FontFamily
                                                                                                                .Monospace,
                                                                                                fontSize =
                                                                                                        12.sp,
                                                                                                modifier =
                                                                                                        Modifier.padding(
                                                                                                                vertical =
                                                                                                                        2.dp
                                                                                                        )
                                                                                        )
                                                                                }
                                                                }

                                                                // Device extensions
                                                                if (vulkanInfo.deviceExtensions
                                                                                .isNotEmpty() &&
                                                                                vulkanInfo
                                                                                        .instanceExtensions
                                                                                        .isNotEmpty()
                                                                ) {
                                                                        Text(
                                                                                text =
                                                                                        "Device Extensions:",
                                                                                fontWeight =
                                                                                        FontWeight
                                                                                                .Bold,
                                                                                color =
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .primary,
                                                                                modifier =
                                                                                        Modifier.padding(
                                                                                                top =
                                                                                                        8.dp,
                                                                                                bottom =
                                                                                                        4.dp
                                                                                        )
                                                                        )
                                                                } else if (vulkanInfo
                                                                                .deviceExtensions
                                                                                .isNotEmpty()
                                                                ) {
                                                                        Text(
                                                                                text =
                                                                                        "Vulkan Extensions:",
                                                                                fontWeight =
                                                                                        FontWeight
                                                                                                .Bold,
                                                                                color =
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .primary,
                                                                                modifier =
                                                                                        Modifier.padding(
                                                                                                bottom =
                                                                                                        4.dp
                                                                                        )
                                                                        )
                                                                }

                                                                vulkanInfo.deviceExtensions
                                                                        .forEach { extension ->
                                                                                Text(
                                                                                        text =
                                                                                                extension,
                                                                                        fontFamily =
                                                                                                androidx.compose
                                                                                                        .ui
                                                                                                        .text
                                                                                                        .font
                                                                                                        .FontFamily
                                                                                                        .Monospace,
                                                                                        fontSize =
                                                                                                12.sp,
                                                                                        modifier =
                                                                                                Modifier.padding(
                                                                                                        vertical =
                                                                                                                2.dp
                                                                                                )
                                                                                )
                                                                        }
                                                        }
                                                }
                                        }

                                        // Features (if available)
                                        if (vulkanInfo.features != null) {
                                                // Calculate feature count
                                                val featuresList =
                                                        listOf(
                                                                vulkanInfo.features.alphaToOne,
                                                                vulkanInfo.features.depthBiasClamp,
                                                                vulkanInfo.features.depthBounds,
                                                                vulkanInfo.features.depthClamp,
                                                                vulkanInfo
                                                                        .features
                                                                        .drawIndirectFirstInstance,
                                                                vulkanInfo.features.dualSrcBlend,
                                                                vulkanInfo
                                                                        .features
                                                                        .fillModeNonSolid,
                                                                vulkanInfo
                                                                        .features
                                                                        .fragmentStoresAndAtomics,
                                                                vulkanInfo
                                                                        .features
                                                                        .fullDrawIndexUint32,
                                                                vulkanInfo.features.geometryShader,
                                                                vulkanInfo.features.imageCubeArray,
                                                                vulkanInfo
                                                                        .features
                                                                        .independentBlend,
                                                                vulkanInfo
                                                                        .features
                                                                        .inheritedQueries,
                                                                vulkanInfo.features.largePoints,
                                                                vulkanInfo.features.logicOp,
                                                                vulkanInfo
                                                                        .features
                                                                        .multiDrawIndirect,
                                                                vulkanInfo.features.multiViewport,
                                                                vulkanInfo
                                                                        .features
                                                                        .occlusionQueryPrecise,
                                                                vulkanInfo
                                                                        .features
                                                                        .pipelineStatisticsQuery,
                                                                vulkanInfo
                                                                        .features
                                                                        .robustBufferAccess,
                                                                vulkanInfo
                                                                        .features
                                                                        .sampleRateShading,
                                                                vulkanInfo
                                                                        .features
                                                                        .samplerAnisotropy,
                                                                vulkanInfo
                                                                        .features
                                                                        .shaderClipDistance,
                                                                vulkanInfo
                                                                        .features
                                                                        .shaderCullDistance,
                                                                vulkanInfo.features.shaderFloat64,
                                                                vulkanInfo
                                                                        .features
                                                                        .shaderImageGatherExtended,
                                                                vulkanInfo.features.shaderInt16,
                                                                vulkanInfo.features.shaderInt64,
                                                                vulkanInfo
                                                                        .features
                                                                        .shaderResourceMinLod,
                                                                vulkanInfo
                                                                        .features
                                                                        .shaderResourceResidency,
                                                                vulkanInfo
                                                                        .features
                                                                        .shaderSampledImageArrayDynamicIndexing,
                                                                vulkanInfo
                                                                        .features
                                                                        .shaderStorageBufferArrayDynamicIndexing,
                                                                vulkanInfo
                                                                        .features
                                                                        .shaderStorageImageArrayDynamicIndexing,
                                                                vulkanInfo
                                                                        .features
                                                                        .shaderStorageImageExtendedFormats,
                                                                vulkanInfo
                                                                        .features
                                                                        .shaderStorageImageMultisample,
                                                                vulkanInfo
                                                                        .features
                                                                        .shaderStorageImageReadWithoutFormat,
                                                                vulkanInfo
                                                                        .features
                                                                        .shaderStorageImageWriteWithoutFormat,
                                                                vulkanInfo
                                                                        .features
                                                                        .shaderTessellationAndGeometryPointSize,
                                                                vulkanInfo
                                                                        .features
                                                                        .shaderUniformBufferArrayDynamicIndexing,
                                                                vulkanInfo.features.sparseBinding,
                                                                vulkanInfo
                                                                        .features
                                                                        .sparseResidency2Samples,
                                                                vulkanInfo
                                                                        .features
                                                                        .sparseResidency4Samples,
                                                                vulkanInfo
                                                                        .features
                                                                        .sparseResidency8Samples,
                                                                vulkanInfo
                                                                        .features
                                                                        .sparseResidency16Samples,
                                                                vulkanInfo
                                                                        .features
                                                                        .sparseResidencyAliased,
                                                                vulkanInfo
                                                                        .features
                                                                        .sparseResidencyBuffer,
                                                                vulkanInfo
                                                                        .features
                                                                        .sparseResidencyImage2D,
                                                                vulkanInfo
                                                                        .features
                                                                        .sparseResidencyImage3D,
                                                                vulkanInfo
                                                                        .features
                                                                        .tessellationShader,
                                                                vulkanInfo
                                                                        .features
                                                                        .textureCompressionASTC_LDR,
                                                                vulkanInfo
                                                                        .features
                                                                        .textureCompressionBC,
                                                                vulkanInfo
                                                                        .features
                                                                        .textureCompressionETC2,
                                                                vulkanInfo
                                                                        .features
                                                                        .variableMultisampleRate,
                                                                vulkanInfo
                                                                        .features
                                                                        .vertexPipelineStoresAndAtomics,
                                                                vulkanInfo.features.wideLines
                                                        )
                                                val supportedCount = featuresList.count { it }
                                                val totalCount = featuresList.size

                                                Row(
                                                        modifier =
                                                                Modifier.fillMaxWidth()
                                                                        .padding(top = 8.dp),
                                                        horizontalArrangement =
                                                                Arrangement.SpaceBetween,
                                                        verticalAlignment =
                                                                Alignment.CenterVertically
                                                ) {
                                                        Row(
                                                                verticalAlignment =
                                                                        Alignment.CenterVertically,
                                                                horizontalArrangement =
                                                                        Arrangement.spacedBy(8.dp)
                                                        ) {
                                                                Text(
                                                                        text = "Vulkan Features:",
                                                                        fontWeight =
                                                                                FontWeight.Medium
                                                                )
                                                                Surface(
                                                                        shape =
                                                                                MaterialTheme.shapes
                                                                                        .small,
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .primaryContainer
                                                                ) {
                                                                        Text(
                                                                                text =
                                                                                        "$supportedCount / $totalCount",
                                                                                style =
                                                                                        MaterialTheme
                                                                                                .typography
                                                                                                .labelSmall,
                                                                                color =
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .onPrimaryContainer,
                                                                                modifier =
                                                                                        Modifier.padding(
                                                                                                horizontal =
                                                                                                        8.dp,
                                                                                                vertical =
                                                                                                        2.dp
                                                                                        ),
                                                                                fontWeight =
                                                                                        FontWeight
                                                                                                .SemiBold
                                                                        )
                                                                }
                                                        }
                                                        IconButton(
                                                                onClick = {
                                                                        featuresExpanded =
                                                                                !featuresExpanded
                                                                }
                                                        ) {
                                                                Icon(
                                                                        imageVector =
                                                                                if (featuresExpanded
                                                                                )
                                                                                        Icons.Default
                                                                                                .ExpandLess
                                                                                else
                                                                                        Icons.Default
                                                                                                .ExpandMore,
                                                                        contentDescription =
                                                                                if (featuresExpanded
                                                                                )
                                                                                        "Hide"
                                                                                else "Show"
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

        com.ivarna.finalbenchmark2.ui.components.GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                onClick = { expanded = !expanded }
        ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
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
                                                imageVector =
                                                        if (expanded) Icons.Default.ExpandLess
                                                        else Icons.Default.ExpandMore,
                                                contentDescription =
                                                        if (expanded) "Collapse" else "Expand"
                                        )
                                }
                        }

                        if (expanded) {
                                Spacer(modifier = Modifier.height(8.dp))

                                InfoRow(
                                        "Max Texture Size",
                                        "${capabilities.maxTextureSize} x ${capabilities.maxTextureSize}"
                                )
                                InfoRow(
                                        "Max Renderbuffer Size",
                                        "${capabilities.maxRenderbufferSize} x ${capabilities.maxRenderbufferSize}"
                                )
                                InfoRow(
                                        "Max Viewport",
                                        "${capabilities.maxViewportWidth} x ${capabilities.maxViewportHeight}"
                                )
                                InfoRow(
                                        "Max Fragment Uniform Vectors",
                                        capabilities.maxFragmentUniformVectors.toString()
                                )
                                InfoRow(
                                        "Max Vertex Attributes",
                                        capabilities.maxVertexAttributes.toString()
                                )

                                if (capabilities.supportedTextureCompressionFormats.isNotEmpty()) {
                                        InfoRow(
                                                "Texture Compression",
                                                capabilities.supportedTextureCompressionFormats
                                                        .joinToString(", ")
                                        )
                                }
                        }
                }
        }
}

@Composable
fun ScreenTab(context: android.content.Context) {
        // Cache DisplayUtils and display info to prevent recreation on every recomposition
        val displayUtils = remember(context) { DisplayUtils(context) }
        val displayInfo = remember(displayUtils) { displayUtils.getDisplayInfo() }

        Column(
                modifier =
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Top
        ) {
                Text(
                        text = "Screen Information",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )

                // Card 1: Display Metrics
                DisplayMetricsCard(displayInfo)

                Spacer(modifier = Modifier.height(16.dp))

                // Card 2: Capabilities
                DisplayCapabilitiesCard(displayInfo)

                Spacer(modifier = Modifier.height(16.dp))

                // Card 3: System State
                DisplaySystemStateCard(displayInfo)

                Spacer(modifier = Modifier.height(120.dp))
        }
}

@Composable
fun DisplayMetricsCard(displayInfo: com.ivarna.finalbenchmark2.utils.DisplayInfo) {
        com.ivarna.finalbenchmark2.ui.components.GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
        ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
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
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
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
                                                imageVector =
                                                        if (expanded) Icons.Default.ExpandLess
                                                        else Icons.Default.ExpandMore,
                                                contentDescription =
                                                        if (expanded) "Collapse" else "Expand"
                                        )
                                }
                        }

                        if (expanded) {
                                Text(
                                        text = displayInfo.realMetrics,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                )
                        }
                }
        }
}

@Composable
fun DisplayCapabilitiesCard(displayInfo: com.ivarna.finalbenchmark2.utils.DisplayInfo) {
        com.ivarna.finalbenchmark2.ui.components.GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
        ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
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
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        Text(
                                                text = "HDR Types",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                                onClick = { hdrTypesExpanded = !hdrTypesExpanded }
                                        ) {
                                                Icon(
                                                        imageVector =
                                                                if (hdrTypesExpanded)
                                                                        Icons.Default.ExpandLess
                                                                else Icons.Default.ExpandMore,
                                                        contentDescription =
                                                                if (hdrTypesExpanded) "Collapse"
                                                                else "Expand"
                                                )
                                        }
                                }

                                if (hdrTypesExpanded) {
                                        Column(
                                                modifier =
                                                        Modifier.fillMaxWidth().padding(top = 4.dp)
                                        ) {
                                                displayInfo.hdrTypes.forEach { hdrType ->
                                                        Row(
                                                                modifier =
                                                                        Modifier.fillMaxWidth()
                                                                                .padding(
                                                                                        vertical =
                                                                                                2.dp
                                                                                ),
                                                                verticalAlignment =
                                                                        Alignment.CenterVertically
                                                        ) {
                                                                Icon(
                                                                        painter =
                                                                                painterResource(
                                                                                        id =
                                                                                                com.ivarna
                                                                                                        .finalbenchmark2
                                                                                                        .R
                                                                                                        .drawable
                                                                                                        .check_24
                                                                                ),
                                                                        contentDescription = null,
                                                                        tint = Color(0xFF4CAF50),
                                                                        modifier =
                                                                                Modifier.size(16.dp)
                                                                )
                                                                Spacer(
                                                                        modifier =
                                                                                Modifier.width(8.dp)
                                                                )
                                                                Text(
                                                                        text = hdrType,
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .bodySmall,
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onSurfaceVariant
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
fun DisplaySystemStateCard(displayInfo: com.ivarna.finalbenchmark2.utils.DisplayInfo) {
        com.ivarna.finalbenchmark2.ui.components.GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
        ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
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
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
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
                                                imageVector =
                                                        if (safeAreaExpanded)
                                                                Icons.Default.ExpandLess
                                                        else Icons.Default.ExpandMore,
                                                contentDescription =
                                                        if (safeAreaExpanded) "Collapse"
                                                        else "Expand"
                                        )
                                }
                        }

                        if (safeAreaExpanded) {
                                Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                                        Text(
                                                text = "Safe Area: ${displayInfo.safeAreaInsets}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                        Text(
                                                text =
                                                        "Display Cutout: ${displayInfo.displayCutout}",
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
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
                Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(0.4f) // Give label 40% width max
                )

                Row(
                        modifier = Modifier.weight(0.6f), // Give value 60% width
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        isSupported?.let { supported ->
                                Icon(
                                        painter =
                                                if (supported)
                                                        painterResource(
                                                                id =
                                                                        com.ivarna
                                                                                .finalbenchmark2
                                                                                .R
                                                                                .drawable
                                                                                .check_24
                                                        )
                                                else
                                                        painterResource(
                                                                id =
                                                                        com.ivarna
                                                                                .finalbenchmark2
                                                                                .R
                                                                                .drawable
                                                                                .close_24
                                                        ),
                                        contentDescription =
                                                if (supported) "Supported" else "Not Supported",
                                        tint =
                                                if (supported) Color(0xFF4CAF50)
                                                else Color(0xFFEF5350),
                                        modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                        }

                        Text(
                                text = value,
                                style = MaterialTheme.typography.bodyMedium,
                                color =
                                        if (value.isEmpty()) MaterialTheme.colorScheme.tertiary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.End,
                                softWrap = true,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                        )
                }
        }
}

/** Enhanced InfoRow specifically for long text values like GPU OpenGL/Vulkan versions */
@Composable
fun LongInfoRow(label: String, value: String, isSupported: Boolean? = null) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(0.4f) // Give label 40% width max
                        )

                        Row(
                                modifier = Modifier.weight(0.6f), // Give value 60% width
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                isSupported?.let { supported ->
                                        Icon(
                                                painter =
                                                        if (supported)
                                                                painterResource(
                                                                        id =
                                                                                com.ivarna
                                                                                        .finalbenchmark2
                                                                                        .R
                                                                                        .drawable
                                                                                        .check_24
                                                                )
                                                        else
                                                                painterResource(
                                                                        id =
                                                                                com.ivarna
                                                                                        .finalbenchmark2
                                                                                        .R
                                                                                        .drawable
                                                                                        .close_24
                                                                ),
                                                contentDescription =
                                                        if (supported) "Supported"
                                                        else "Not Supported",
                                                tint =
                                                        if (supported) Color(0xFF4CAF50)
                                                        else Color(0xFFEF5350),
                                                modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                }
                        }
                }

                // Value text in a separate row below the label and icon
                Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        color =
                                if (value.isEmpty()) MaterialTheme.colorScheme.tertiary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Start,
                        softWrap = true,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                )
        }
}

@Composable
fun OsTab(
        deviceInfo: com.ivarna.finalbenchmark2.utils.DeviceInfo,
        osViewModel: OsViewModel = viewModel()
) {
        OsTabContent(osViewModel)
}

@Composable
fun MemoryTab(
        deviceInfo: com.ivarna.finalbenchmark2.utils.DeviceInfo,
        viewModel: DeviceViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
        val context = LocalContext.current
        val memoryHistory by viewModel.memoryHistory.collectAsState()
        val systemInfoSummary by viewModel.systemInfoSummary.collectAsState()

        // Fetch system process information when the tab is loaded
        LaunchedEffect(Unit) { viewModel.fetchSystemInfo(context) }

        LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
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
                                modifier = Modifier.fillMaxWidth()
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
                        SummaryCard(summary = systemInfoSummary, modifier = Modifier.fillMaxWidth())
                }

                // RAM Information Card
                item {
                        com.ivarna.finalbenchmark2.ui.components.GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
                        ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                        Text(
                                                text = "RAM Information",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(bottom = 8.dp)
                                        )

                                        // RAM items
                                        com.ivarna.finalbenchmark2.ui.components.InformationRow(
                                                itemValue =
                                                        com.ivarna.finalbenchmark2.domain.model
                                                                .ItemValue.Text(
                                                                "Total RAM",
                                                                formatBytesInMB(deviceInfo.totalRam)
                                                        ),
                                                isLastItem = false
                                        )
                                        com.ivarna.finalbenchmark2.ui.components.InformationRow(
                                                itemValue =
                                                        com.ivarna.finalbenchmark2.domain.model
                                                                .ItemValue.Text(
                                                                "Available RAM",
                                                                formatBytesInMB(
                                                                        deviceInfo.availableRam
                                                                )
                                                        ),
                                                isLastItem = false
                                        )

                                        // Swap information
                                        if (deviceInfo.totalSwap > 0) {
                                                com.ivarna.finalbenchmark2.ui.components
                                                        .InformationRow(
                                                                itemValue =
                                                                        com.ivarna.finalbenchmark2
                                                                                .domain.model
                                                                                .ItemValue.Text(
                                                                                "Total Swap",
                                                                                formatBytesInMB(
                                                                                        deviceInfo
                                                                                                .totalSwap
                                                                                )
                                                                        ),
                                                                isLastItem = false
                                                        )
                                                com.ivarna.finalbenchmark2.ui.components
                                                        .InformationRow(
                                                                itemValue =
                                                                        com.ivarna.finalbenchmark2
                                                                                .domain.model
                                                                                .ItemValue.Text(
                                                                                "Used Swap",
                                                                                formatBytesInMB(
                                                                                        deviceInfo
                                                                                                .usedSwap
                                                                                )
                                                                        ),
                                                                isLastItem = true
                                                        )
                                        } else {
                                                com.ivarna.finalbenchmark2.ui.components
                                                        .InformationRow(
                                                                itemValue =
                                                                        com.ivarna.finalbenchmark2
                                                                                .domain.model
                                                                                .ItemValue.Text(
                                                                                "Swap",
                                                                                "Not available"
                                                                        ),
                                                                isLastItem = true
                                                        )
                                        }
                                }
                        }
                }

                // Storage Information Card
                item {
                        com.ivarna.finalbenchmark2.ui.components.GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
                        ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                        Text(
                                                text = "Storage Information",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(bottom = 8.dp)
                                        )

                                        // Storage items
                                        com.ivarna.finalbenchmark2.ui.components.InformationRow(
                                                itemValue =
                                                        com.ivarna.finalbenchmark2.domain.model
                                                                .ItemValue.Text(
                                                                "Total Storage",
                                                                formatBytes(deviceInfo.totalStorage)
                                                        ),
                                                isLastItem = false
                                        )
                                        com.ivarna.finalbenchmark2.ui.components.InformationRow(
                                                itemValue =
                                                        com.ivarna.finalbenchmark2.domain.model
                                                                .ItemValue.Text(
                                                                "Free Storage",
                                                                formatBytes(deviceInfo.freeStorage)
                                                        ),
                                                isLastItem = true
                                        )
                                }
                        }
                }

                // Process Table
                item {
                        com.ivarna.finalbenchmark2.ui.components.GlassCard(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
                        ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .background(
                                                                        MaterialTheme.colorScheme
                                                                                .surface.copy(
                                                                                alpha = 0.3f
                                                                        )
                                                                )
                                                                .padding(
                                                                        vertical = 12.dp,
                                                                        horizontal = 16.dp
                                                                ),
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
                                                        modifier = Modifier.weight(0.7f),
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
                                                        text = "RAM",
                                                        modifier = Modifier.weight(1f),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp,
                                                        textAlign = TextAlign.End,
                                                )
                                        }

                                        // Process Items
                                        systemInfoSummary.processes.forEach { process ->
                                                Row(
                                                        modifier =
                                                                Modifier.fillMaxWidth()
                                                                        .padding(
                                                                                horizontal = 16.dp,
                                                                                vertical = 8.dp
                                                                        ),
                                                        verticalAlignment =
                                                                Alignment.CenterVertically
                                                ) {
                                                        Text(
                                                                text = process.name,
                                                                modifier = Modifier.weight(2f),
                                                                fontSize = 14.sp,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurface
                                                        )
                                                        Text(
                                                                text = process.pid.toString(),
                                                                modifier = Modifier.weight(0.7f),
                                                                fontSize = 14.sp,
                                                                maxLines = 1,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurface
                                                        )
                                                        Text(
                                                                text = process.state,
                                                                modifier = Modifier.weight(1f),
                                                                fontSize = 14.sp,
                                                                maxLines = 1,
                                                                color =
                                                                        when (process.state) {
                                                                                "Foreground" ->
                                                                                        Color.Green
                                                                                "Service" ->
                                                                                        Color.Blue
                                                                                "Background" ->
                                                                                        Color.Gray
                                                                                else -> Color.Red
                                                                        }
                                                        )
                                                        Text(
                                                                text = "${process.ramUsage} MB",
                                                                modifier = Modifier.weight(1f),
                                                                fontSize = 14.sp,
                                                                maxLines = 1,
                                                                textAlign = TextAlign.End,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurface
                                                        )
                                                }

                                                HorizontalDivider(
                                                        modifier =
                                                                Modifier.fillMaxWidth()
                                                                        .padding(
                                                                                horizontal = 16.dp
                                                                        ),
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .outlineVariant.copy(
                                                                        alpha = 0.3f
                                                                )
                                                )
                                        }
                                }
                        }
                }
        }
}

@Composable
fun SensorsTab(context: android.content.Context) {
        SensorsTabContent()
}

@Composable
fun VulkanFeaturesGrid(features: com.ivarna.finalbenchmark2.utils.VulkanFeatures) {
        val sortedFeatures =
                remember(features) {
                        listOf(
                                        "Alpha To One" to features.alphaToOne,
                                        "Depth Bias Clamp" to features.depthBiasClamp,
                                        "Depth Bounds" to features.depthBounds,
                                        "Depth Clamp" to features.depthClamp,
                                        "Draw Indirect First Instance" to
                                                features.drawIndirectFirstInstance,
                                        "Dual Src Blend" to features.dualSrcBlend,
                                        "Fill Mode Non Solid" to features.fillModeNonSolid,
                                        "Fragment Stores And Atomics" to
                                                features.fragmentStoresAndAtomics,
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
                                        "Pipeline Statistics Query" to
                                                features.pipelineStatisticsQuery,
                                        "Robust Buffer Access" to features.robustBufferAccess,
                                        "Sample Rate Shading" to features.sampleRateShading,
                                        "Sampler Anisotropy" to features.samplerAnisotropy,
                                        "Shader Clip Distance" to features.shaderClipDistance,
                                        "Shader Cull Distance" to features.shaderCullDistance,
                                        "Shader Float64" to features.shaderFloat64,
                                        "Shader Image Gather Extended" to
                                                features.shaderImageGatherExtended,
                                        "Shader Int16" to features.shaderInt16,
                                        "Shader Int64" to features.shaderInt64,
                                        "Shader Resource Min Lod" to features.shaderResourceMinLod,
                                        "Shader Resource Residency" to
                                                features.shaderResourceResidency,
                                        "Shader Sampled Image Array Dynamic Indexing" to
                                                features.shaderSampledImageArrayDynamicIndexing,
                                        "Shader Storage Buffer Array Dynamic Indexing" to
                                                features.shaderStorageBufferArrayDynamicIndexing,
                                        "Shader Storage Image Array Dynamic Indexing" to
                                                features.shaderStorageImageArrayDynamicIndexing,
                                        "Shader Storage Image Extended Formats" to
                                                features.shaderStorageImageExtendedFormats,
                                        "Shader Storage Image Multisample" to
                                                features.shaderStorageImageMultisample,
                                        "Shader Storage Image Read Without Format" to
                                                features.shaderStorageImageReadWithoutFormat,
                                        "Shader Storage Image Write Without Format" to
                                                features.shaderStorageImageWriteWithoutFormat,
                                        "Shader Tessellation And Geometry Point Size" to
                                                features.shaderTessellationAndGeometryPointSize,
                                        "Shader Uniform Buffer Array Dynamic Indexing" to
                                                features.shaderUniformBufferArrayDynamicIndexing,
                                        "Sparse Binding" to features.sparseBinding,
                                        "Sparse Residency 2 Samples" to
                                                features.sparseResidency2Samples,
                                        "Sparse Residency 4 Samples" to
                                                features.sparseResidency4Samples,
                                        "Sparse Residency 8 Samples" to
                                                features.sparseResidency8Samples,
                                        "Sparse Residency 16 Samples" to
                                                features.sparseResidency16Samples,
                                        "Sparse Residency Aliased" to
                                                features.sparseResidencyAliased,
                                        "Sparse Residency Buffer" to features.sparseResidencyBuffer,
                                        "Sparse Residency Image2D" to
                                                features.sparseResidencyImage2D,
                                        "Sparse Residency Image3D" to
                                                features.sparseResidencyImage3D,
                                        "Tessellation Shader" to features.tessellationShader,
                                        "Texture Compression ASTC_LDR" to
                                                features.textureCompressionASTC_LDR,
                                        "Texture Compression BC" to features.textureCompressionBC,
                                        "Texture Compression ETC2" to
                                                features.textureCompressionETC2,
                                        "Variable Multisample Rate" to
                                                features.variableMultisampleRate,
                                        "Vertex Pipeline Stores And Atomics" to
                                                features.vertexPipelineStoresAndAtomics,
                                        "Wide Lines" to features.wideLines
                                )
                                .sortedBy { it.first }
                }

        // Count supported features
        val supportedCount = sortedFeatures.count { it.second }
        val totalCount = sortedFeatures.size

        Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Spacer(modifier = Modifier.height(4.dp))

                // Scrollable list container
                Column(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .heightIn(max = 400.dp)
                                        .verticalScroll(rememberScrollState())
                                        .background(
                                                color =
                                                        MaterialTheme.colorScheme.surface.copy(
                                                                alpha = 0.5f
                                                        ),
                                                shape = MaterialTheme.shapes.medium
                                        )
                                        .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                        // Single column list
                        sortedFeatures.forEach { (name, isSupported) ->
                                // Feature chip
                                Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = MaterialTheme.shapes.small,
                                        color =
                                                if (isSupported)
                                                        MaterialTheme.colorScheme.primaryContainer
                                                                .copy(alpha = 0.3f)
                                                else
                                                        MaterialTheme.colorScheme.errorContainer
                                                                .copy(alpha = 0.2f),
                                        border =
                                                androidx.compose.foundation.BorderStroke(
                                                        width = 1.dp,
                                                        color =
                                                                if (isSupported)
                                                                        MaterialTheme.colorScheme
                                                                                .primary.copy(
                                                                                alpha = 0.3f
                                                                        )
                                                                else
                                                                        MaterialTheme.colorScheme
                                                                                .error.copy(
                                                                                alpha = 0.2f
                                                                        )
                                                )
                                ) {
                                        Row(
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .padding(
                                                                        horizontal = 10.dp,
                                                                        vertical = 8.dp
                                                                ),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Start
                                        ) {
                                                // Status icon
                                                Icon(
                                                        painter =
                                                                if (isSupported)
                                                                        painterResource(
                                                                                id =
                                                                                        com.ivarna
                                                                                                .finalbenchmark2
                                                                                                .R
                                                                                                .drawable
                                                                                                .check_24
                                                                        )
                                                                else
                                                                        painterResource(
                                                                                id =
                                                                                        com.ivarna
                                                                                                .finalbenchmark2
                                                                                                .R
                                                                                                .drawable
                                                                                                .close_24
                                                                        ),
                                                        contentDescription =
                                                                if (isSupported) "Supported"
                                                                else "Not Supported",
                                                        tint =
                                                                if (isSupported) Color(0xFF4CAF50)
                                                                else Color(0xFFEF5350),
                                                        modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                // Feature name
                                                Text(
                                                        text = name,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontSize = 13.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
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
                colors =
                        CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
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

@Composable
fun InfoRow(label: String, value: String) {
        com.ivarna.finalbenchmark2.ui.components.InformationRow(
                itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text(label, value),
                isLastItem = false
        )
}

@Composable
fun LongInfoRow(label: String, value: String) {
        com.ivarna.finalbenchmark2.ui.components.InformationRow(
                itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text(label, value),
                isLastItem = false
        )
}
