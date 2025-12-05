package com.ivarna.finalbenchmark2.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivarna.finalbenchmark2.R
import com.ivarna.finalbenchmark2.ui.viewmodels.OsViewModel
import com.ivarna.finalbenchmark2.utils.OsInfo

@Composable
fun OsTabContent(viewModel: OsViewModel) {
    val context = LocalContext.current
    val osInfo by viewModel.osInfo.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    // Load OS info when the composable is first displayed
    LaunchedEffect(context) {
        viewModel.loadOsInfo(context)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Operating System Information",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )
        
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (osInfo != null) {
            OsInfoContent(osInfo!!)
        } else {
            Text(
                text = "Failed to load OS information",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
fun OsInfoContent(osInfo: OsInfo) {
    // Card 1: Android System
    AndroidSystemCard(osInfo)
    
    Spacer(modifier = Modifier.height(16.dp))
    
    // Card 2: Firmware & Build
    FirmwareBuildCard(osInfo)
    
    Spacer(modifier = Modifier.height(16.dp))
    
    // Card 3: Kernel & Runtime
    KernelRuntimeCard(osInfo)
}

@Composable
fun AndroidSystemCard(osInfo: OsInfo) {
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
                    imageVector = Icons.Rounded.Android,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Android System",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            InfoRow("Android Version", "${osInfo.androidVersion} (${osInfo.androidCodeName})")
            InfoRow("API Level", osInfo.apiLevel)
            InfoRow("Security Patch", osInfo.securityPatch.takeIf { it.isNotEmpty() } ?: "Unknown")
            InfoRow(
                "Root Access",
                if (osInfo.isRooted) "Yes" else "No",
                isSupported = osInfo.isRooted
            )
        }
    }
}

@Composable
fun FirmwareBuildCard(osInfo: OsInfo) {
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
                    imageVector = Icons.Rounded.Build,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Firmware & Build",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            InfoRow("Build ID", osInfo.buildId)
            InfoRow("Baseband Version", osInfo.baseband.takeIf { it.isNotEmpty() } ?: "Unknown")
            InfoRow("Bootloader Version", osInfo.bootloader.takeIf { it.isNotEmpty() } ?: "Unknown")
            InfoRow("Google Play Services", osInfo.googlePlayServicesVersion)
        }
    }
}

@Composable
fun KernelRuntimeCard(osInfo: OsInfo) {
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
                    imageVector = Icons.Rounded.Memory,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Kernel & Runtime",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            InfoRow("Kernel Version", osInfo.kernelVersion)
            InfoRow("Kernel Architecture", osInfo.kernelArch)
            InfoRow("Java VM Version", osInfo.javaVmVersion)
            InfoRow("System Uptime", osInfo.systemUptime)
        }
    }
}