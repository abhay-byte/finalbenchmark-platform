package com.ivarna.finalbenchmark2.ui.screens

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.ComponentActivity
import com.ivarna.finalbenchmark2.MainActivity
import com.ivarna.finalbenchmark2.ui.theme.FinalBenchmark2Theme
import com.ivarna.finalbenchmark2.ui.theme.ThemeMode
import com.ivarna.finalbenchmark2.utils.ThemePreferences
import com.ivarna.finalbenchmark2.utils.PowerConsumptionPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val themePreferences = remember { ThemePreferences(context) }
    
    val themes = listOf("Light", "Dark", "System Default")
    val currentThemeMode = themePreferences.getThemeMode()
    var selectedThemeIndex by remember { mutableStateOf(getThemeIndex(currentThemeMode)) }
    
    // Update theme when selection changes
    val onThemeChange: (Int) -> Unit = remember {
        { newIndex ->
            if (newIndex != getThemeIndex(themePreferences.getThemeMode())) {
                val themeMode = when (newIndex) {
                    0 -> ThemeMode.LIGHT
                    1 -> ThemeMode.DARK
                    else -> ThemeMode.SYSTEM
                }
                themePreferences.setThemeMode(themeMode)
                
                // Use the activity's updateTheme method to handle theme change properly
                val activity = context as? ComponentActivity
                if (activity is MainActivity) {
                    activity.updateTheme(themeMode)
                } else {
                    // Fallback to the default approach if not MainActivity
                    when (themeMode) {
                        ThemeMode.LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                        ThemeMode.DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                        ThemeMode.SYSTEM -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    }
                    activity?.recreate()
                }
            }
        }
    }
    
    FinalBenchmark2Theme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Text(
                    text = "Settings",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
                
                // Theme Settings Card
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
                            text = "Theme Settings",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        // Theme Selection
                        Text(
                            text = "App Theme",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        var expanded by remember { mutableStateOf(false) }
                        
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            TextField(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                value = themes[selectedThemeIndex],
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("Select Theme") },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Dropdown arrow"
                                    )
                                }
                            )
                            
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                themes.forEachIndexed { index, theme ->
                                    DropdownMenuItem(
                                        text = { Text(theme) },
                                        onClick = {
                                            onThemeChange(index)
                                            selectedThemeIndex = index
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                        
                        Text(
                            text = "Theme will apply to the entire application",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
                
                // Power Consumption Multiplier Settings Card
                val powerConsumptionPrefs = remember { PowerConsumptionPreferences(context) }
                var selectedMultiplier by remember { mutableStateOf(powerConsumptionPrefs.getMultiplier()) }
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Power Consumption Settings",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        // Multiplier Selection
                        Text(
                            text = "Power Multiplier",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        val multipliers = listOf(0.01f, 0.1f, 1.0f, 10f, 100f)
                        val multiplierLabels = listOf("0.01x", "0.1x", "1x", "10x", "100x")
                        
                        var expanded by remember { mutableStateOf(false) }
                        
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            TextField(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                value = "Current: ${String.format("%.2f", selectedMultiplier)}x",
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("Select Multiplier") },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Dropdown arrow"
                                    )
                                }
                            )
                            
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                multipliers.forEachIndexed { index, multiplier ->
                                    DropdownMenuItem(
                                        text = { Text(multiplierLabels[index]) },
                                        onClick = {
                                            powerConsumptionPrefs.setMultiplier(multiplier)
                                            selectedMultiplier = multiplier
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                        
                        Text(
                            text = "Adjust power consumption readings by this multiplier",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
                
                // Additional settings can be added here in the future
                Spacer(modifier = Modifier.weight(1f))
                
                // Version info
                Text(
                    text = "FinalBenchmark2 v1.0",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}


// Helper function to get theme index
private fun getThemeIndex(themeMode: ThemeMode): Int {
    return when (themeMode) {
        ThemeMode.LIGHT -> 0
        ThemeMode.DARK -> 1
        ThemeMode.SYSTEM -> 2
    }
}