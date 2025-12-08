package com.ivarna.finalbenchmark2.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.ivarna.finalbenchmark2.utils.ThemePreferences
import com.ivarna.finalbenchmark2.ui.theme.ThemeMode
import kotlinx.coroutines.launch
import androidx.activity.ComponentActivity

// Data class for theme options
data class ThemeOption(
    val id: String,
    val label: String,
    val primaryColor: Color,
    val containerColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelectionScreen(
    onNextClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val themePreferences = remember { ThemePreferences(context) }
    var selectedThemeId by remember { mutableStateOf(themePreferences.getThemeMode().name) }
    val scope = rememberCoroutineScope()
    
    // Get the selected theme
    val selectedTheme = remember(selectedThemeId) { ThemeMode.valueOf(selectedThemeId) }
    
    // Update the theme when selection changes to provide immediate visual feedback
    LaunchedEffect(selectedThemeId) {
        themePreferences.setThemeMode(selectedTheme)
    }
    
    // Define the theme options with all available themes
    val themeOptions = remember {
        listOf(
            ThemeOption(
                id = "SYSTEM",
                label = "System Default",
                primaryColor = Color(0xFF6750A4),
                containerColor = Color(0xFFEADDFF)
            ),
            ThemeOption(
                id = "LIGHT",
                label = "Light",
                primaryColor = Color(0xFF6750A4),
                containerColor = Color(0xFFFFFBFE)
            ),
            ThemeOption(
                id = "DARK",
                label = "Dark",
                primaryColor = Color(0xFFD0BCFF),
                containerColor = Color(0xFF1C1B1F)
            ),
            ThemeOption(
                id = "GRUVBOX",
                label = "Gruvbox",
                primaryColor = Color(0xFFFBF1C7),
                containerColor = Color(0xFF282828)
            ),
            ThemeOption(
                id = "NORD",
                label = "Nord",
                primaryColor = Color(0xFF88C0D0),
                containerColor = Color(0xFF2E3440)
            ),
            ThemeOption(
                id = "DRACULA",
                label = "Dracula",
                primaryColor = Color(0xFFBD93F9),
                containerColor = Color(0xFF282A36)
            ),
            ThemeOption(
                id = "SOLARIZED",
                label = "Solarized",
                primaryColor = Color(0xFF268BD2),
                containerColor = Color(0xFF002B36)
            ),
            ThemeOption(
                id = "MONOKAI",
                label = "Monokai",
                primaryColor = Color(0xFFA6E22E),
                containerColor = Color(0xFF27282)
            ),
            ThemeOption(
                id = "SKY_BREEZE",
                label = "Sky Breeze",
                primaryColor = Color(0xFF3B82F6),
                containerColor = Color(0xFFFFFFFF)
            ),
            ThemeOption(
                id = "LAVENDER_DREAM",
                label = "Lavender Dream",
                primaryColor = Color(0xFF8B5CF6),
                containerColor = Color(0xFFFAFAF9)
            ),
            ThemeOption(
                id = "MINT_FRESH",
                label = "Mint Fresh",
                primaryColor = Color(0xFF059669),
                containerColor = Color(0xFFFFFFFF)
            ),
            ThemeOption(
                id = "AMOLED_BLACK",
                label = "AMOLED Black",
                primaryColor = Color(0xFF3B82F6),
                containerColor = Color(0xFF000000)
            )
        )
    }
    
    // Create a theme-aware MaterialTheme by using the current selected theme
    val currentTheme = remember(selectedTheme) { selectedTheme }
    val useDarkTheme = when (currentTheme) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
        ThemeMode.GRUVBOX -> true
        ThemeMode.NORD -> true
        ThemeMode.DRACULA -> true
        ThemeMode.SOLARIZED -> false
        ThemeMode.MONOKAI -> true
        ThemeMode.SKY_BREEZE -> false
        ThemeMode.LAVENDER_DREAM -> false
        ThemeMode.MINT_FRESH -> false
        ThemeMode.AMOLED_BLACK -> true
    }
    
    val colorScheme = when (currentTheme) {
        ThemeMode.GRUVBOX -> if (useDarkTheme) com.ivarna.finalbenchmark2.ui.theme.GruvboxDarkColorScheme else com.ivarna.finalbenchmark2.ui.theme.GruvboxLightColorScheme
        ThemeMode.NORD -> if (useDarkTheme) com.ivarna.finalbenchmark2.ui.theme.NordDarkColorScheme else com.ivarna.finalbenchmark2.ui.theme.NordLightColorScheme
        ThemeMode.DRACULA -> com.ivarna.finalbenchmark2.ui.theme.DraculaColorScheme
        ThemeMode.SOLARIZED -> if (useDarkTheme) com.ivarna.finalbenchmark2.ui.theme.SolarizedDarkColorScheme else com.ivarna.finalbenchmark2.ui.theme.SolarizedLightColorScheme
        ThemeMode.MONOKAI -> com.ivarna.finalbenchmark2.ui.theme.MonokaiColorScheme
        ThemeMode.SKY_BREEZE -> com.ivarna.finalbenchmark2.ui.theme.SkyBreezeColorScheme
        ThemeMode.LAVENDER_DREAM -> com.ivarna.finalbenchmark2.ui.theme.LavenderDreamColorScheme
        ThemeMode.MINT_FRESH -> com.ivarna.finalbenchmark2.ui.theme.MintFreshColorScheme
        ThemeMode.AMOLED_BLACK -> com.ivarna.finalbenchmark2.ui.theme.AmoledBlackColorScheme
        else -> if (useDarkTheme) com.ivarna.finalbenchmark2.ui.theme.DarkColorScheme else com.ivarna.finalbenchmark2.ui.theme.LightColorScheme
    }
    
    MaterialTheme(colorScheme = colorScheme) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header Section
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 20.dp)
                ) {
                    // Beautiful Header with Icon
                    Card(
                        modifier = Modifier.size(100.dp),
                        shape = CircleShape,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Palette,
                                contentDescription = "Theme Selection",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Beautiful Headlines
                    Text(
                        text = "Choose Your Style",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "Select a theme that matches your personality",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                }
                
                // Theme Grid - Increased to 3 columns for better layout with more padding
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .weight(1f, fill = false),
                    contentPadding = PaddingValues(
                        top = 16.dp,
                        bottom = 100.dp // Extra padding to account for the floating button
                    )
                ) {
                    items(themeOptions) { theme ->
                        EnhancedThemeCard(
                            theme = theme,
                            isSelected = selectedThemeId == theme.id,
                            onThemeSelected = { selectedThemeId = theme.id }
                        )
                    }
                }
                
                // Beautiful Action Button
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                // Mark onboarding as completed when continuing
                                val onboardingPreferences = com.ivarna.finalbenchmark2.utils.OnboardingPreferences(context)
                                onboardingPreferences.setOnboardingCompleted()
                            }
                            // Apply the selected theme when continuing to main app
                            val activity = context as? ComponentActivity
                            if (activity is com.ivarna.finalbenchmark2.MainActivity) {
                                activity.updateTheme(selectedTheme)
                            } else {
                                // Fallback to the default approach if not MainActivity
                                when (selectedTheme) {
                                    com.ivarna.finalbenchmark2.ui.theme.ThemeMode.LIGHT -> androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO)
                                    com.ivarna.finalbenchmark2.ui.theme.ThemeMode.DARK -> androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES)
                                    com.ivarna.finalbenchmark2.ui.theme.ThemeMode.SYSTEM -> androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                                    // For custom themes, default to dark mode
                                    else -> androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES)
                                }
                                activity?.recreate()
                            }
                            onNextClicked()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "Continue",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp
                            ),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EnhancedThemeCard(
    theme: ThemeOption,
    isSelected: Boolean,
    onThemeSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = tween(durationMillis = 200)
    )
    
    Card(
        onClick = onThemeSelected,
        modifier = modifier
            .scale(scale)
            .aspectRatio(0.85f),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            CardDefaults.outlinedCardBorder().copy(
                width = 3.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary
                    )
                )
            )
        } else {
            CardDefaults.outlinedCardBorder().copy(
                width = 1.dp,
                brush = androidx.compose.ui.graphics.SolidColor(
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )
        },
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 12.dp else 4.dp,
            pressedElevation = if (isSelected) 16.dp else 8.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                // Enhanced Color Preview with better styling
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(48.dp)
                ) {
                    // Background Circle
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(theme.containerColor)
                            .border(
                                width = 2.dp,
                                color = theme.primaryColor.copy(alpha = 0.6f),
                                shape = CircleShape
                            )
                    )
                    
                    // Primary Color Dot
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(theme.primaryColor)
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.8f),
                                shape = CircleShape
                            )
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Enhanced Theme Name with better typography
                Text(
                    text = theme.label,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp
                    ),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Enhanced Selection Indicator with better icon
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    // Selection Background Circle
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                    )
                                )
                            )
                            .shadow(8.dp, CircleShape)
                    ) {
                        // Check Icon with animation
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Selected",
                            modifier = Modifier
                                .size(18.dp)
                                .align(Alignment.Center),
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}