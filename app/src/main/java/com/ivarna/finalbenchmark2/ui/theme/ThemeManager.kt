package com.ivarna.finalbenchmark2.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.foundation.isSystemInDarkTheme

// Define theme modes
enum class ThemeMode {
    LIGHT,
    DARK,
    GRUVBOX,
    NORD,
    DRACULA,
    SOLARIZED,
    MONOKAI,
    SKY_BREEZE,
    LAVENDER_DREAM,
    MINT_FRESH,
    AMOLED_BLACK,
    SYSTEM
}

// CompositionLocal to hold the current theme mode
val LocalThemeMode = staticCompositionLocalOf { mutableStateOf(ThemeMode.SYSTEM) }

@Composable
fun provideThemeMode(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val themeModeState = remember(themeMode) { mutableStateOf(themeMode) }
    CompositionLocalProvider(LocalThemeMode provides themeModeState) {
        content()
    }
}

@Composable
fun shouldUseDarkTheme(): Boolean {
    val themeMode by LocalThemeMode.current
    return when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.GRUVBOX -> true                    // Gruvbox defaults to dark (traditional)
        ThemeMode.NORD -> true                       // Nord defaults to dark (traditional)
        ThemeMode.DRACULA -> true                    // Dracula is typically dark
        ThemeMode.SOLARIZED -> false                 // Solarized defaults to light (traditional)
        ThemeMode.MONOKAI -> true                    // Monokai is typically dark
        ThemeMode.SKY_BREEZE -> false                // Sky Breeze is light
        ThemeMode.LAVENDER_DREAM -> false            // Lavender Dream is light
        ThemeMode.MINT_FRESH -> false                // Mint Fresh is light
        ThemeMode.AMOLED_BLACK -> true               // AMOLED Black is dark
    }
}