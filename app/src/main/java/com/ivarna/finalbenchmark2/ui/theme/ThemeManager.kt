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
    }
}