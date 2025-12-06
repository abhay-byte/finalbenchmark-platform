package com.ivarna.finalbenchmark2.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Observer
import com.ivarna.finalbenchmark2.ui.components.InformationRow
import com.ivarna.finalbenchmark2.ui.components.PhoneStatePermissionRequest
import com.ivarna.finalbenchmark2.ui.components.CameraPermissionRequest
import com.ivarna.finalbenchmark2.ui.viewmodels.HardwareViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HardwareTabContent(viewModel: HardwareViewModel) {
    var batterySpecs by remember { mutableStateOf<com.ivarna.finalbenchmark2.utils.BatterySpec?>(null) }
    var networkSpecs by remember { mutableStateOf<com.ivarna.finalbenchmark2.utils.NetworkSpec?>(null) }
    var cameraSpecs by remember { mutableStateOf<List<com.ivarna.finalbenchmark2.utils.CameraSpec>?>(null) }
    var memoryStorageSpecs by remember { mutableStateOf<com.ivarna.finalbenchmark2.utils.MemoryStorageSpec?>(null) }
    var audioMediaSpecs by remember { mutableStateOf<com.ivarna.finalbenchmark2.utils.AudioMediaSpec?>(null) }
    var peripheralsSpecs by remember { mutableStateOf<com.ivarna.finalbenchmark2.utils.PeripheralsSpec?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Observe LiveData and update state variables
    DisposableEffect(viewModel) {
        val batteryObserver = Observer<com.ivarna.finalbenchmark2.utils.BatterySpec?> { newValue ->
            batterySpecs = newValue
        }
        val networkObserver = Observer<com.ivarna.finalbenchmark2.utils.NetworkSpec?> { newValue ->
            networkSpecs = newValue
        }
        val cameraObserver = Observer<List<com.ivarna.finalbenchmark2.utils.CameraSpec>?> { newValue ->
            cameraSpecs = newValue
        }
        val memoryStorageObserver = Observer<com.ivarna.finalbenchmark2.utils.MemoryStorageSpec?> { newValue ->
            memoryStorageSpecs = newValue
        }
        val audioMediaObserver = Observer<com.ivarna.finalbenchmark2.utils.AudioMediaSpec?> { newValue ->
            audioMediaSpecs = newValue
        }
        val peripheralsObserver = Observer<com.ivarna.finalbenchmark2.utils.PeripheralsSpec?> { newValue ->
            peripheralsSpecs = newValue
        }
        val loadingObserver = Observer<Boolean> { newValue ->
            isLoading = newValue
        }
        val errorObserver = Observer<String?> { newValue ->
            error = newValue
        }

        viewModel.batterySpecs.observeForever(batteryObserver)
        viewModel.networkSpecs.observeForever(networkObserver)
        viewModel.cameraSpecs.observeForever(cameraObserver)
        viewModel.memoryStorageSpecs.observeForever(memoryStorageObserver)
        viewModel.audioMediaSpecs.observeForever(audioMediaObserver)
        viewModel.peripheralsSpecs.observeForever(peripheralsObserver)
        viewModel.isLoading.observeForever(loadingObserver)
        viewModel.error.observeForever(errorObserver)

        onDispose {
            viewModel.batterySpecs.removeObserver(batteryObserver)
            viewModel.networkSpecs.removeObserver(networkObserver)
            viewModel.cameraSpecs.removeObserver(cameraObserver)
            viewModel.memoryStorageSpecs.removeObserver(memoryStorageObserver)
            viewModel.audioMediaSpecs.removeObserver(audioMediaObserver)
            viewModel.peripheralsSpecs.removeObserver(peripheralsObserver)
            viewModel.isLoading.removeObserver(loadingObserver)
            viewModel.error.removeObserver(errorObserver)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadHardwareSpecs()
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (error != null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Text(text = "Error: $error")
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                BatteryCard(batterySpecs)
            }
            item {
                PhoneStatePermissionRequest {
                    ConnectivityCard(networkSpecs)
                }
            }
            item {
                CameraPermissionRequest {
                    CameraCard(cameraSpecs)
                }
            }
            item {
                MemoryStorageCard(memoryStorageSpecs)
            }
            item {
                AudioMediaCard(audioMediaSpecs)
            }
            item {
                PeripheralsCard(peripheralsSpecs)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryCard(batterySpecs: com.ivarna.finalbenchmark2.utils.BatterySpec?) {
    if (batterySpecs != null) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Battery Information",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Level", "${batterySpecs.level}%"),
                    isLastItem = false
                )
                InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Status", batterySpecs.status),
                    isLastItem = false
                )
                InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Technology", batterySpecs.technology),
                    isLastItem = false
                )
                InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Temperature", "${batterySpecs.temperature}°C"),
                    isLastItem = false
                )
                InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Voltage", "${batterySpecs.voltage}V"),
                    isLastItem = false
                )
                InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Health", batterySpecs.health),
                    isLastItem = false
                )
                InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Design Capacity", batterySpecs.designCapacity),
                    isLastItem = false
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectivityCard(networkSpecs: com.ivarna.finalbenchmark2.utils.NetworkSpec?) {
    if (networkSpecs != null) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Connectivity",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Network Type", networkSpecs.networkType),
                    isLastItem = false
                )
                InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Signal Strength", networkSpecs.signalStrength),
                    isLastItem = false
                )
                InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("WiFi Speed", networkSpecs.wifiSpeed),
                    isLastItem = false
                )
                InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("WiFi Frequency", networkSpecs.wifiFrequency),
                    isLastItem = false
                )
                InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("WiFi Standard", networkSpecs.wifiStandard),
                    isLastItem = false
                )
                InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Bluetooth Features", networkSpecs.bluetoothFeatures.joinToString(", ")),
                    isLastItem = false
                )
                InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("NFC Supported", if (networkSpecs.nfcSupported) "Yes" else "No"),
                    isLastItem = false
                )
                InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("IR Blaster", if (networkSpecs.irBlasterSupported) "Supported" else "Not Supported"),
                    isLastItem = false
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraCard(cameraSpecs: List<com.ivarna.finalbenchmark2.utils.CameraSpec>?) {
    if (cameraSpecs != null && cameraSpecs.isNotEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Camera Modules",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                cameraSpecs.forEachIndexed { index, camera ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "Camera ${camera.id} (${camera.direction})",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        InformationRow(
                            itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Resolution", camera.resolution),
                            isLastItem = false
                        )
                        InformationRow(
                            itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Aperture", camera.aperture),
                            isLastItem = false
                        )
                        InformationRow(
                            itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Focal Length", camera.focalLength),
                            isLastItem = false
                        )
                        InformationRow(
                            itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Capabilities", camera.capabilities.joinToString(", ")),
                            isLastItem = index == cameraSpecs.size - 1
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryStorageCard(memoryStorageSpecs: com.ivarna.finalbenchmark2.utils.MemoryStorageSpec?) {
    if (memoryStorageSpecs != null) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Memory & Storage",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Total RAM", memoryStorageSpecs.ramTotal),
                    isLastItem = false
                )
                InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Available RAM", memoryStorageSpecs.ramAvailable),
                    isLastItem = false
                )
                InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Total Storage", memoryStorageSpecs.storageTotal),
                    isLastItem = false
                )
                InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Available Storage", memoryStorageSpecs.storageAvailable),
                    isLastItem = false
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioMediaCard(audioMediaSpecs: com.ivarna.finalbenchmark2.utils.AudioMediaSpec?) {
    if (audioMediaSpecs != null) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Audio & Media",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Speakers", audioMediaSpecs.speakers.joinToString(", ")),
                    isLastItem = false
                )
                InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Widevine Level", audioMediaSpecs.widevineLevel),
                    isLastItem = false
                )
                
                // Supported codecs - show in a scrollable text area
                Text(
                    text = "Supported Codecs:",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
                Text(
                    text = audioMediaSpecs.supportedCodecs.joinToString(", "),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeripheralsCard(peripheralsSpecs: com.ivarna.finalbenchmark2.utils.PeripheralsSpec?) {
    if (peripheralsSpecs != null) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Peripherals",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Biometric Support", peripheralsSpecs.biometricSupport.joinToString(", ")),
                    isLastItem = false
                )
                InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("SIM Slots", "${peripheralsSpecs.simSlots}"),
                    isLastItem = false
                )
                InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Vibration Amplitude Control", if (peripheralsSpecs.vibrationSupport) "Supported" else "Not Supported"),
                    isLastItem = false
                )
                InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("USB OTG", if (peripheralsSpecs.usbOtg) "Supported" else "Not Supported"),
                    isLastItem = false
                )
                InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("Display HDR", peripheralsSpecs.displayHdr.joinToString(", ")),
                    isLastItem = false
                )
                InformationRow(
                    itemValue = com.ivarna.finalbenchmark2.domain.model.ItemValue.Text("System Architecture", peripheralsSpecs.systemArchitecture.joinToString(", ")),
                    isLastItem = true
                )
            }
        }
    }
}