package com.ivarna.finalbenchmark2.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@Composable
fun ContextualPermissionRequest(
        permission: String,
        rationaleText: String,
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit
) {
    val context = LocalContext.current
    var isGranted by remember {
        mutableStateOf(
                ContextCompat.checkSelfPermission(context, permission) ==
                        PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher =
            rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted
                ->
                isGranted = granted
            }

    if (isGranted) {
        content()
    } else {
        Card(
                colors =
                        CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = modifier.fillMaxWidth()
        ) {
            Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Rounded.Lock, null, tint = MaterialTheme.colorScheme.primary)
                Text(text = "Permission Required", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                        text = rationaleText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                )
                Button(onClick = { launcher.launch(permission) }) { Text("Grant Access") }
            }
        }
    }
}

// Convenience composables for specific permissions
@Composable
fun CameraPermissionRequest(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    ContextualPermissionRequest(
            permission = Manifest.permission.CAMERA,
            rationaleText = "Camera permission is needed to show camera capabilities",
            modifier = modifier,
            content = content
    )
}

@Composable
fun PhoneStatePermissionRequest(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    ContextualPermissionRequest(
            permission = Manifest.permission.READ_PHONE_STATE,
            rationaleText =
                    "Phone permission is needed to show Network Signal strength and SIM information",
            modifier = modifier,
            content = content
    )
}

@Composable
fun LocationPermissionRequest(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    ContextualPermissionRequest(
            permission = Manifest.permission.ACCESS_FINE_LOCATION,
            rationaleText =
                    "Location permission is needed to show WiFi SSID/BSSID and Cell Tower info",
            modifier = modifier,
            content = content
    )
}

@Composable
fun BodySensorsPermissionRequest(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    ContextualPermissionRequest(
            permission = Manifest.permission.BODY_SENSORS,
            rationaleText =
                    "Body sensors permission is needed to access heart rate and other sensor data",
            modifier = modifier,
            content = content
    )
}

@Composable
fun BluetoothPermissionRequest(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val permission =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                Manifest.permission.BLUETOOTH_CONNECT
            } else {
                Manifest.permission.BLUETOOTH
            }

    ContextualPermissionRequest(
            permission = permission,
            rationaleText = "Bluetooth permission is needed to access Bluetooth device information",
            modifier = modifier,
            content = content
    )
}
