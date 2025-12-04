package com.ivarna.finalbenchmark2.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ivarna.finalbenchmark2.ui.viewmodels.GpuInfoViewModel
import com.ivarna.finalbenchmark2.utils.GpuFrequencyReader

@Composable
fun GpuFrequencyCard(
    modifier: Modifier = Modifier,
    viewModel: GpuInfoViewModel = viewModel()
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "GPU Frequency",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val gpuState by viewModel.gpuFrequencyState.collectAsState()
            
            when (gpuState) {
                is GpuFrequencyReader.GpuFrequencyState.Available -> {
                    val data = (gpuState as GpuFrequencyReader.GpuFrequencyState.Available).data
                    
                    // Current Frequency (Large, Prominent)
                    Text(
                        text = "${data.currentFrequencyMhz} MHz",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Min/Max Range
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        data.minFrequencyMhz?.let { minFreq ->
                            Text(
                                text = "Min: $minFreq MHz",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        data.maxFrequencyMhz?.let { maxFreq ->
                            Text(
                                text = "Max: $maxFreq MHz",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // Governor
                    data.governor?.let { governor ->
                        Text(
                            text = "Governor: $governor",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Source indicator
                    Text(
                        text = "Source: Root (${data.sourcePath})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Utilization (if available)
                    data.utilizationPercent?.let { utilization ->
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { utilization / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "GPU Load: $utilization%",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                
                GpuFrequencyReader.GpuFrequencyState.RequiresRoot -> {
                    Text(
                        text = "GPU frequency monitoring requires root access",
                        color = MaterialTheme.colorScheme.error
                    )
                    // Button to request root would go here if needed
                }
                
                GpuFrequencyReader.GpuFrequencyState.NotSupported -> {
                    Text(
                        text = "GPU frequency monitoring not supported on this device",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                is GpuFrequencyReader.GpuFrequencyState.Error -> {
                    Text(
                        text = "Error: ${(gpuState as GpuFrequencyReader.GpuFrequencyState.Error).message}",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}