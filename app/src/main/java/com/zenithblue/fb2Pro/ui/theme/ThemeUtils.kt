package com.zenithblue.fb2Pro.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Extension functions for easier theme color access
 */
object ThemeColors {
    
    val primary: Color
        @Composable get() = MaterialTheme.colorScheme.primary
    
    val onPrimary: Color
        @Composable get() = MaterialTheme.colorScheme.onPrimary
    
    val primaryContainer: Color
        @Composable get() = MaterialTheme.colorScheme.primaryContainer
    
    val onPrimaryContainer: Color
        @Composable get() = MaterialTheme.colorScheme.onPrimaryContainer
    
    val secondary: Color
        @Composable get() = MaterialTheme.colorScheme.secondary
    
    val onSecondary: Color
        @Composable get() = MaterialTheme.colorScheme.onSecondary
    
    val background: Color
        @Composable get() = MaterialTheme.colorScheme.background
    
    val onBackground: Color
        @Composable get() = MaterialTheme.colorScheme.onBackground
    
    val surface: Color
        @Composable get() = MaterialTheme.colorScheme.surface
    
    val onSurface: Color
        @Composable get() = MaterialTheme.colorScheme.onSurface
    
    val surfaceVariant: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceVariant
    
    val onSurfaceVariant: Color
        @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
    
    val error: Color
        @Composable get() = MaterialTheme.colorScheme.error
    
    val onError: Color
        @Composable get() = MaterialTheme.colorScheme.onError
    
    val outline: Color
        @Composable get() = MaterialTheme.colorScheme.outline
}