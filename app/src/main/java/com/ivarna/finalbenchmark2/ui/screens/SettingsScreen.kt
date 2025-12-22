package com.ivarna.finalbenchmark2.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivarna.finalbenchmark2.MainActivity
import com.ivarna.finalbenchmark2.R
import com.ivarna.finalbenchmark2.ui.theme.FinalBenchmark2Theme
import com.ivarna.finalbenchmark2.ui.theme.ThemeMode
import com.ivarna.finalbenchmark2.ui.viewmodels.RootStatus
import com.ivarna.finalbenchmark2.utils.OnboardingPreferences
import com.ivarna.finalbenchmark2.utils.PowerConsumptionPreferences
import com.ivarna.finalbenchmark2.utils.RootAccessManager
import com.ivarna.finalbenchmark2.utils.RootAccessPreferences
import com.ivarna.finalbenchmark2.utils.ThemePreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
        rootStatus: RootStatus =
                RootStatus.NO_ROOT, // Root status from MainViewModel (legacy parameter)
        onBackClick: () -> Unit = {},
        onNavigateToOnboarding: () -> Unit = {}
) {
        val context = LocalContext.current
        val themePreferences = remember { ThemePreferences(context) }
        val rootAccessPreferences = remember { RootAccessPreferences(context) }
        val scope = rememberCoroutineScope()

        val themes =
                listOf(
                        "Light Monet",
                        "Dark Monet",
                        "Gruvbox",
                        "Nord",
                        "Dracula",
                        "Solarized",
                        "Monokai",
                        "Sky Breeze",
                        "Lavender Dream",
                        "Mint Fresh",
                        "AMOLED Black",
                        "System Default"
                )
        val currentThemeMode = themePreferences.getThemeMode()
        var selectedThemeIndex by remember { mutableStateOf(getThemeIndex(currentThemeMode)) }

        // Root access state - now using RootAccessManager for caching
        var useRootAccess by remember { mutableStateOf(rootAccessPreferences.getUseRootAccess()) }

        // FIX: Use RootAccessManager to prevent UI freezing during theme changes
        // This ensures the heavy root check happens only once per app session
        var isDeviceRooted by remember { mutableStateOf(false) }
        var isRootCheckLoading by remember { mutableStateOf(true) }

        // LaunchedEffect runs when the screen is first created and during Activity recreation
        // Because RootAccessManager is a Singleton, it returns cached results instantly
        LaunchedEffect(Unit) {
                isDeviceRooted = RootAccessManager.isRootGranted()
                isRootCheckLoading = false
        }

        val canExecuteRoot = isDeviceRooted // Simplified logic

        // Update theme when selection changes
        val onThemeChange: (Int) -> Unit = remember {
                { newIndex ->
                        if (newIndex != getThemeIndex(themePreferences.getThemeMode())) {
                                val themeMode =
                                        when (newIndex) {
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

                                // Use the activity's updateTheme method to handle theme change
                                // properly
                                val activity = context as? ComponentActivity
                                if (activity is MainActivity) {
                                        activity.updateTheme(themeMode)
                                } else {
                                        // Fallback to the default approach if not MainActivity
                                        when (themeMode) {
                                                ThemeMode.LIGHT ->
                                                        AppCompatDelegate.setDefaultNightMode(
                                                                AppCompatDelegate.MODE_NIGHT_NO
                                                        )
                                                ThemeMode.DARK ->
                                                        AppCompatDelegate.setDefaultNightMode(
                                                                AppCompatDelegate.MODE_NIGHT_YES
                                                        )
                                                ThemeMode.SYSTEM ->
                                                        AppCompatDelegate.setDefaultNightMode(
                                                                AppCompatDelegate
                                                                        .MODE_NIGHT_FOLLOW_SYSTEM
                                                        )
                                                // For custom themes, default to dark mode
                                                else ->
                                                        AppCompatDelegate.setDefaultNightMode(
                                                                AppCompatDelegate.MODE_NIGHT_YES
                                                        )
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

        // Entrance Animation
        var isVisible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { isVisible = true }

        FinalBenchmark2Theme {
                Box(
                    modifier = Modifier.fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .background(
                            androidx.compose.ui.graphics.Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                                    Color.Transparent
                                ),
                                center = androidx.compose.ui.geometry.Offset(0f, 0f),
                                radius = 1000f
                            )
                        )
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.background,
                                    MaterialTheme.colorScheme.surfaceContainerLowest
                                )
                            )
                        )
                ) {
                    Scaffold(
                        containerColor = Color.Transparent,
                        topBar = {
                            CenterAlignedTopAppBar(
                                title = {
                                    Text(
                                        "Settings",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                navigationIcon = {
                                    IconButton(onClick = onBackClick) {
                                        Icon(
                                            imageVector = Icons.Rounded.ArrowBack,
                                            contentDescription = "Back",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.background(
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.5f)
                                )
                            )
                        }
                    ) { innerPadding ->
                        AnimatedVisibility(
                            visible = isVisible,
                            enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(500)) + androidx.compose.animation.slideInVertically(androidx.compose.animation.core.tween(500)) { 100 }
                        ) {
                            val scrollState = rememberScrollState()

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                                    .padding(horizontal = 16.dp, vertical = 16.dp)
                                    .verticalScroll(scrollState),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {

                                // Root Access Card
                                GlassSettingCard {
                                    Column(modifier = Modifier.padding(20.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Use Root Access",
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )

                                                Text(
                                                    text = if (isDeviceRooted) {
                                                        if (canExecuteRoot) "Root access available and working"
                                                        else "Root access available but not working"
                                                    } else {
                                                        "No root access detected"
                                                    },
                                                    fontSize = 13.sp,
                                                    color = if (isDeviceRooted && canExecuteRoot)
                                                        MaterialTheme.colorScheme.primary
                                                    else
                                                        MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.padding(top = 4.dp)
                                                )
                                            }

                                            Switch(
                                                checked = useRootAccess,
                                                onCheckedChange = { onRootAccessChange(it) },
                                                enabled = !isRootCheckLoading && isDeviceRooted,
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                                )
                                            )
                                        }
                                    }
                                }

                                // Theme Settings Card
                                GlassSettingCard {
                                    Column(modifier = Modifier.padding(20.dp)) {
                                        Text(
                                            text = "Theme Settings",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(bottom = 12.dp)
                                        )

                                        Text(
                                            text = "App Theme",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )

                                        var expanded by remember { mutableStateOf(false) }

                                        ExposedDropdownMenuBox(
                                            expanded = expanded,
                                            onExpandedChange = { expanded = !expanded }
                                        ) {
                                            OutlinedTextField(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                                value = themes[selectedThemeIndex],
                                                onValueChange = {},
                                                readOnly = true,
                                                trailingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Default.ArrowDropDown,
                                                        contentDescription = "Dropdown arrow"
                                                    )
                                                },
                                                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                                )
                                            )

                                            ExposedDropdownMenu(
                                                expanded = expanded,
                                                onDismissRequest = { expanded = false },
                                                modifier = Modifier
                                                    .background(
                                                        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.95f),
                                                        androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                                                    )
                                                    .border(
                                                        1.dp,
                                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                                        androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                                                    )
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
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            modifier = Modifier.padding(top = 8.dp)
                                        )
                                    }
                                }

                                // Power Consumption Multiplier Settings Card
                                val powerConsumptionPrefs = remember { PowerConsumptionPreferences(context) }
                                var selectedMultiplier by remember { mutableStateOf(powerConsumptionPrefs.getMultiplier()) }

                                GlassSettingCard {
                                    Column(modifier = Modifier.padding(20.dp)) {
                                        Text(
                                            text = "Power Consumption Settings",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(bottom = 12.dp)
                                        )

                                        Text(
                                            text = "Power Multiplier",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )

                                        val multipliers = listOf(0.01f, 0.1f, 1.0f, 10f, 100f)
                                        val multiplierLabels = listOf("0.01x", "0.1x", "1x", "10x", "100x")
                                        var expanded by remember { mutableStateOf(false) }

                                        ExposedDropdownMenuBox(
                                            expanded = expanded,
                                            onExpandedChange = { expanded = !expanded }
                                        ) {
                                            OutlinedTextField(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                                value = "Current: ${String.format("%.2f", selectedMultiplier)}x",
                                                onValueChange = {},
                                                readOnly = true,
                                                trailingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Default.ArrowDropDown,
                                                        contentDescription = "Dropdown arrow"
                                                    )
                                                },
                                                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                                )
                                            )

                                            ExposedDropdownMenu(
                                                expanded = expanded,
                                                onDismissRequest = { expanded = false },
                                                modifier = Modifier
                                                    .background(
                                                        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.95f),
                                                        androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                                                    )
                                                    .border(
                                                        1.dp,
                                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                                        androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                                                    )
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
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            modifier = Modifier.padding(top = 8.dp)
                                        )
                                    }
                                }

                                // Onboarding Settings Card
                                GlassSettingCard {
                                    Column(modifier = Modifier.padding(20.dp)) {
                                        Text(
                                            text = "App Tour & Setup",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(bottom = 12.dp)
                                        )

                                        Text(
                                            text = "Revisit the welcome guide to verify root access, customize themes, and calibrate power settings.",
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(bottom = 20.dp),
                                            lineHeight = 20.sp
                                        )

                                        Button(
                                            onClick = { onNavigateToOnboarding() },
                                            modifier = Modifier.fillMaxWidth().height(50.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary
                                            ),
                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.RocketLaunch,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Restart Onboarding",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                // Horizontal line separator
                                androidx.compose.material3.HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                )

                                // About Section
                                AboutSection()
                            }
                        }
                    }
                }
        }
}

@Composable
fun GlassSettingCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
    val colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
    )
    val border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
    val elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            colors = colors,
            elevation = elevation,
            border = border
        ) {
            GlassCardContent(content)
        }
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            colors = colors,
            elevation = elevation,
            border = border
        ) {
            GlassCardContent(content)
        }
    }
}

@Composable
private fun GlassCardContent(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.05f)
                    )
                )
            )
    ) {
        content()
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
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
                // Card 1: App Info (FinalBenchmark 2 with logo_2.png and circular dark background)
                GlassSettingCard {
                        Column(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                                // Circular dark background with logo
                                Box(
                                        modifier =
                                                Modifier.size(80.dp)
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

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                        text = "Final Benchmark 2",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        Text(
                                                text = "v0.5.1", // Updated Version
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                textAlign = TextAlign.Center
                                        )
                                        Text(
                                                text = " • ",
                                                fontSize = 16.sp,
                                                color =
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                                .copy(alpha = 0.5f),
                                                textAlign = TextAlign.Center
                                        )
                                        Text(
                                                text = "Dec 22, 2025", // Updated Date
                                                fontSize = 16.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center
                                        )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                        text = "Made with ❤️ in Kotlin",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center
                                )
                        }
                }

                // Card 2: Special Thanks
                GlassSettingCard {
                        Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                                Text(
                                        text = "Special Thanks",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(bottom = 16.dp)
                                )

                                val credits =
                                        listOf(
                                                Credit(
                                                        "CPU Info",
                                                        "kamgurgul",
                                                        "https://github.com/kamgurgul/cpu-info"
                                                ),
                                                Credit(
                                                        "SmartPack Kernel Manager",
                                                        "SmartPack",
                                                        "https://github.com/SmartPack/SmartPack-Kernel-Manager"
                                                ),
                                                Credit(
                                                        "Wattz",
                                                        "dubrowgn",
                                                        "https://github.com/dubrowgn/wattz"
                                                )
                                        )

                                credits.forEach { credit ->
                                        Row(
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .padding(vertical = 8.dp)
                                                                .clickable {
                                                                        openUrl(context, credit.url)
                                                                },
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
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurface
                                                        )
                                                        Text(
                                                                text = "by ${credit.author}",
                                                                fontSize = 12.sp,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant
                                                        )
                                                }
                                        }
                                }
                        }
                }

                // Card 4: My Abhay Raj Card (with me.png)
GlassSettingCard(
    onClick = { openUrl(context, "https://github.com/abhay-byte") }
) {
    Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Picture using me.png
            Box(
                modifier =
                    Modifier.size(80.dp)
                        .clip(CircleShape)
                        .background(
                            MaterialTheme.colorScheme
                                .primary
                        ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter =
                        painterResource(id = R.drawable.me),
                    contentDescription =
                        "Abhay Raj Profile Picture",
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
                    text =
                        "Passionate about building software, exploring hardware, and all things Linux.",
                    fontSize = 14.sp,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                    lineHeight = 18.sp
                )
            }
        }
    }
}
                // Card 5: Connect With Me (with proper social media icons)
// Card 5: Connect With Me (with proper social media icons)
GlassSettingCard {
    Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
        Text(
            text = "Connect With Me",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        val socialLinks =
            listOf(
                SocialLink(
                    "GitHub",
                    "https://github.com/abhay-byte",
                    R.drawable.github_icon,
                    "View my repositories and projects"
                ),
                SocialLink(
                    "LinkedIn",
                    "https://www.linkedin.com/in/abhay-byte/",
                    R.drawable.linkedin_icon,
                    "Let's connect professionally"
                ),
                SocialLink(
                    "Portfolio",
                    "https://abhayraj-porfolio.web.app/",
                    R.drawable.ic_portfolio,
                    "Check out my work"
                ),
                SocialLink(
                    "Instagram",
                    "https://www.instagram.com/abhayrajx/",
                    R.drawable.ic_instagram,
                    "Follow my journey"
                ),
                SocialLink(
                    "X (Twitter)",
                    "https://x.com/arch_deve",
                    R.drawable.ic_twitter_x,
                    "Stay updated with me"
                )
            )

        socialLinks.forEach { link ->
            Row(
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable {
                            openUrl(context, link.url)
                        },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter =
                        painterResource(id = link.iconRes),
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
                        color =
                            MaterialTheme.colorScheme
                                .onSurface
                    )
                    Text(
                        text = link.description,
                        fontSize = 12.sp,
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )
                }
            }
        }
    }
}

                // Card 6: GitHub Star (with GitHub logo)
                GlassSettingCard(
                        onClick = {
                                openUrl(
                                        context,
                                        "https://github.com/abhay-byte/FinalBenchmark-Platform"
                                )
                        }
                ) {
                        Row(
                                modifier = Modifier.fillMaxWidth().padding(20.dp),
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
                                                text =
                                                        "Final Benchmark Platform - If you like this project, please give it a star!",
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

data class SocialLink(val name: String, val url: String, val iconRes: Int, val description: String)

data class Credit(val name: String, val author: String, val url: String)

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
