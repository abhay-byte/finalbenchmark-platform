package com.ivarna.finalbenchmark2.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Color
import androidx.activity.ComponentActivity
import com.ivarna.finalbenchmark2.MainActivity
import com.ivarna.finalbenchmark2.R
import com.ivarna.finalbenchmark2.ui.theme.FinalBenchmark2Theme
import com.ivarna.finalbenchmark2.ui.theme.ThemeMode
import com.ivarna.finalbenchmark2.ui.viewmodels.RootStatus
import com.ivarna.finalbenchmark2.utils.ThemePreferences
import com.ivarna.finalbenchmark2.utils.PowerConsumptionPreferences
import com.ivarna.finalbenchmark2.utils.RootAccessPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    rootStatus: RootStatus = RootStatus.NO_ROOT // Root status from MainViewModel
) {
    val context = LocalContext.current
    val themePreferences = remember { ThemePreferences(context) }
    val rootAccessPreferences = remember { RootAccessPreferences(context) }
    
    val themes = listOf("Light", "Dark", "Gruvbox", "Nord", "Dracula", "Solarized", "Monokai", "Sky Breeze", "Lavender Dream", "Mint Fresh", "AMOLED Black", "System Default")
    val currentThemeMode = themePreferences.getThemeMode()
    var selectedThemeIndex by remember { mutableStateOf(getThemeIndex(currentThemeMode)) }
    
    // Root access state - now comes from MainViewModel
    var useRootAccess by remember { mutableStateOf(rootAccessPreferences.getUseRootAccess()) }
    val isRootCheckLoading by remember { mutableStateOf(false) } // No longer loading since MainViewModel handles it
    
    // Get root status from the passed parameter instead of checking again
    val isDeviceRooted = rootStatus != RootStatus.NO_ROOT
    val canExecuteRoot = rootStatus == RootStatus.ROOT_WORKING
    
    // Update theme when selection changes
    val onThemeChange: (Int) -> Unit = remember {
        { newIndex ->
            if (newIndex != getThemeIndex(themePreferences.getThemeMode())) {
                val themeMode = when (newIndex) {
                    0 -> ThemeMode.LIGHT
                    1 -> ThemeMode.DARK
                    2 -> ThemeMode.GRUVBOX
                    3 -> ThemeMode.NORD
                    4 -> ThemeMode.DRACULA
                    5 -> ThemeMode.SOLARIZED
                    6 -> ThemeMode.MONOKAI
                    7 -> ThemeMode.SKY_BREEZE
                    8 -> ThemeMode.LAVENDER_DREAM
                    9 -> ThemeMode.MINT_FRESH
                    10 -> ThemeMode.AMOLED_BLACK
                    11 -> ThemeMode.SYSTEM
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
                        // For custom themes, default to dark mode
                        else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    }
                    activity?.recreate()
                }
            }
        }
    }
    
    // Handle root access toggle
    val onRootAccessChange: (Boolean) -> Unit = remember {
        { enabled ->
            useRootAccess = enabled
            rootAccessPreferences.setUseRootAccess(enabled)
        }
    }
    
    FinalBenchmark2Theme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val scrollState = rememberScrollState()
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 56.dp, // Add top padding to account for status bar area
                             start = 16.dp,
                             end = 16.dp,
                             bottom = 16.dp)
                    .verticalScroll(scrollState)
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
                
                // Root Access Card (at the top)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Use Root Access",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                
                                    Text(
                                        text = if (isDeviceRooted) {
                                            if (canExecuteRoot) "Root access available and working" else "Root access available but not working"
                                        } else {
                                            "No root access detected"
                                        },
                                        fontSize = 14.sp,
                                        color = if (isDeviceRooted && canExecuteRoot) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                            }
                            
                            Switch(
                                checked = useRootAccess,
                                onCheckedChange = { onRootAccessChange(it) },
                                enabled = !isRootCheckLoading && isDeviceRooted // Only enable if not loading and device is rooted
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
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
                
                // Horizontal line separator
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // About Section
                AboutSection()
            }
        }
    }
}


// Helper function to open URLs
private fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        Log.e("SettingsScreen", "Error opening URL: $url", e)
    }
}

@Composable
fun AboutSection() {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Card 1: App Info (FinalBenchmark 2 with logo_2.png and circular dark background)
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Circular dark background with logo
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1A1A1A)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_2),
                        contentDescription = "FinalBenchmark 2 Logo",
                        modifier = Modifier.size(60.dp),
                        contentScale = ContentScale.Crop
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Final Benchmark 2",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Version 1.0.0",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Made with ❤️ in Kotlin",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        // Card 2: Play Store Review Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            onClick = { openUrl(context, "https://play.google.com/store/apps/dev?id=8004929841101888920") }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.playstore_icon),
                    contentDescription = "Play Store",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Review on Play Store",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // Five stars
                    Row {
                        repeat(5) { index ->
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = "Star ${index + 1}",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            if (index < 4) {
                                Spacer(modifier = Modifier.width(2.dp))
                            }
                        }
                    }
                    Text(
                        text = "Share your feedback and help others discover Final Benchmark 2",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                        lineHeight = 18.sp
                    )
                }
            }
        }
        
        // Card 3: Special Thanks
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Special Thanks",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                val credits = listOf(
                    Credit("CPU Info", "kamgurgul", "https://github.com/kamgurgul/cpu-info"),
                    Credit("SmartPack Kernel Manager", "SmartPack", "https://github.com/SmartPack/SmartPack-Kernel-Manager"),
                    Credit("Wattz", "dubrowgn", "https://github.com/dubrowgn/wattz")
                )
                
                credits.forEach { credit ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable { openUrl(context, credit.url) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Favorite,
                            contentDescription = "Credit",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = credit.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "by ${credit.author}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        
        // Card 4: My Abhay Raj Card (with me.png)
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            onClick = { openUrl(context, "https://github.com/abhay-byte") }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile Picture using me.png
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.me),
                            contentDescription = "Abhay Raj Profile Picture",
                            modifier = Modifier.size(80.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(20.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Abhay Raj",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "@abhay-byte",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Text(
                            text = "Passionate about building software, exploring hardware, and all things Linux.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
        
        // Card 5: Connect With Me (with proper social media icons)
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Connect With Me",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                val socialLinks = listOf(
                    SocialLink("GitHub", "https://github.com/abhay-byte", R.drawable.github_icon, "View my repositories and projects"),
                    SocialLink("LinkedIn", "https://www.linkedin.com/in/abhay-byte/", R.drawable.linkedin_icon, "Let's connect professionally"),
                    SocialLink("Portfolio", "https://abhayraj-porfolio.web.app/", R.drawable.ic_portfolio, "Check out my work"),
                    SocialLink("Play Store", "https://play.google.com/store/apps/dev?id=8004929841101888920", R.drawable.playstore_icon, "Download my apps"),
                    SocialLink("Instagram", "https://www.instagram.com/abhayrajx/", R.drawable.ic_instagram, "Follow my journey"),
                    SocialLink("X (Twitter)", "https://x.com/arch_deve", R.drawable.ic_twitter_x, "Stay updated with me")
                )
                
                socialLinks.forEach { link ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable { openUrl(context, link.url) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = link.iconRes),
                            contentDescription = link.name,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = link.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = link.description,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        
        // Card 6: GitHub Star (with GitHub logo)
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            onClick = { openUrl(context, "https://github.com/abhay-byte/finalbenchmark-platform") }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.github_star_icon),
                    contentDescription = "GitHub Star",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Star on GitHub",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Final Benchmark Platform - If you like this project, please give it a star!",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

data class SocialLink(
    val name: String,
    val url: String,
    val iconRes: Int,
    val description: String
)

data class Credit(
    val name: String,
    val author: String,
    val url: String
)

// Helper function to get theme index
private fun getThemeIndex(themeMode: ThemeMode): Int {
    return when (themeMode) {
        ThemeMode.LIGHT -> 0
        ThemeMode.DARK -> 1
        ThemeMode.GRUVBOX -> 2
        ThemeMode.NORD -> 3
        ThemeMode.DRACULA -> 4
        ThemeMode.SOLARIZED -> 5
        ThemeMode.MONOKAI -> 6
        ThemeMode.SKY_BREEZE -> 7
        ThemeMode.LAVENDER_DREAM -> 8
        ThemeMode.MINT_FRESH -> 9
        ThemeMode.AMOLED_BLACK -> 10
        ThemeMode.SYSTEM -> 11
    }
}