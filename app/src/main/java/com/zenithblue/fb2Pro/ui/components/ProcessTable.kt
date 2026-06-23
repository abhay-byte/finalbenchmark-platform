package com.zenithblue.fb2Pro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProcessTable(
    processes: List<ProcessItem>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .background(Color.LightGray),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "App",
                    modifier = Modifier.weight(2f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "PID",
                    modifier = Modifier.weight(0.8f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "State",
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "RAM (MB)",
                    modifier = Modifier.weight(0.8f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    textAlign = TextAlign.End
                )
            }
            
            // Process Items
            LazyColumn {
                items(processes) { process ->
                    ProcessRow(process = process)
                }
            }
        }
    }
}

@Composable
fun ProcessRow(process: ProcessItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = process.name,
            modifier = Modifier.weight(2f),
            fontSize = 14.sp,
            maxLines = 1
        )
        Text(
            text = process.pid.toString(),
            modifier = Modifier.weight(0.8f),
            fontSize = 14.sp
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
            textAlign = TextAlign.End
        )
    }
    
    Divider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    )
}

@Composable
fun SummaryCard(
    summary: SystemInfoSummary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "System Summary",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryItem(
                    label = "Running Processes",
                    value = summary.runningProcesses.toString(),
                    color = MaterialTheme.colorScheme.primary
                )
                
                SummaryItem(
                    label = "Installed Packages",
                    value = summary.totalPackages.toString(),
                    color = MaterialTheme.colorScheme.secondary
                )
                
                SummaryItem(
                    label = "Total Services",
                    value = summary.totalServices.toString(),
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
fun SummaryItem(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}