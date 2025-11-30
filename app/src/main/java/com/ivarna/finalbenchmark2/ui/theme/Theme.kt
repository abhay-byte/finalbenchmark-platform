package com.ivarna.finalbenchmark2.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import android.view.View
import androidx.core.view.WindowInsetsControllerCompat

val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

// Gruvbox Color Schemes
val GruvboxDarkColorScheme = darkColorScheme(
    primary = GruvboxDarkPrimary,
    secondary = GruvboxDarkSecondary,
    tertiary = GruvboxDarkAccent,
    background = GruvboxDarkBg,
    surface = GruvboxDarkSurface,
    onPrimary = GruvboxDarkText,
    onSecondary = GruvboxDarkText,
    onTertiary = GruvboxDarkText,
    onBackground = GruvboxDarkText,
    onSurface = GruvboxDarkText,
    error = GruvboxDarkError,
    onError = GruvboxDarkText
)

val GruvboxLightColorScheme = lightColorScheme(
    primary = GruvboxLightPrimary,
    secondary = GruvboxLightSecondary,
    tertiary = GruvboxLightAccent,
    background = GruvboxLightBg,
    surface = GruvboxLightSurface,
    onPrimary = GruvboxLightText,
    onSecondary = GruvboxLightText,
    onTertiary = GruvboxLightText,
    onBackground = GruvboxLightText,
    onSurface = GruvboxLightText,
    error = GruvboxLightError,
    onError = GruvboxLightText
)

// Nord Color Schemes
val NordDarkColorScheme = darkColorScheme(
    primary = NordDarkPrimary,
    secondary = NordDarkSecondary,
    tertiary = NordDarkAccent,
    background = NordDarkBg,
    surface = NordDarkSurface,
    onPrimary = NordDarkText,
    onSecondary = NordDarkText,
    onTertiary = NordDarkText,
    onBackground = NordDarkText,
    onSurface = NordDarkText,
    error = NordDarkError,
    onError = NordDarkText
)

val NordLightColorScheme = lightColorScheme(
    primary = NordLightPrimary,
    secondary = NordLightSecondary,
    tertiary = NordLightAccent,
    background = NordLightBg,
    surface = NordLightSurface,
    onPrimary = NordLightText,
    onSecondary = NordLightText,
    onTertiary = NordLightText,
    onBackground = NordLightText,
    onSurface = NordLightText,
    error = NordLightError,
    onError = NordLightText
)

// Dracula Color Scheme
val DraculaColorScheme = darkColorScheme(
    primary = DraculaPrimary,
    secondary = DraculaSecondary,
    tertiary = DraculaAccent,
    background = DraculaBg,
    surface = DraculaSurface,
    onPrimary = DraculaText,
    onSecondary = DraculaText,
    onTertiary = DraculaText,
    onBackground = DraculaText,
    onSurface = DraculaText,
    error = DraculaError,
    onError = DraculaText
)

// Solarized Color Schemes
val SolarizedDarkColorScheme = darkColorScheme(
    primary = SolarizedDarkPrimary,
    secondary = SolarizedDarkSecondary,
    tertiary = SolarizedDarkAccent,
    background = SolarizedDarkBg,
    surface = SolarizedDarkSurface,
    onPrimary = SolarizedDarkText,
    onSecondary = SolarizedDarkText,
    onTertiary = SolarizedDarkText,
    onBackground = SolarizedDarkText,
    onSurface = SolarizedDarkText,
    error = SolarizedDarkError,
    onError = SolarizedDarkText
)

val SolarizedLightColorScheme = lightColorScheme(
    primary = SolarizedLightPrimary,
    secondary = SolarizedLightSecondary,
    tertiary = SolarizedLightAccent,
    background = SolarizedLightBg,
    surface = SolarizedLightSurface,
    onPrimary = SolarizedLightText,
    onSecondary = SolarizedLightText,
    onTertiary = SolarizedLightText,
    onBackground = SolarizedLightText,
    onSurface = SolarizedLightText,
    error = SolarizedLightError,
    onError = SolarizedLightText
)

// Monokai Color Scheme
val MonokaiColorScheme = darkColorScheme(
    primary = MonokaiPrimary,
    secondary = MonokaiSecondary,
    tertiary = MonokaiAccent,
    background = MonokaiBg,
    surface = MonokaiSurface,
    onPrimary = MonokaiText,
    onSecondary = MonokaiText,
    onTertiary = MonokaiText,
    onBackground = MonokaiText,
    onSurface = MonokaiText,
    error = MonokaiError,
    onError = MonokaiText
)

@Composable
fun FinalBenchmark2Theme(
    darkTheme: Boolean = shouldUseDarkTheme(), // Use our custom theme system
    themeMode: ThemeMode = ThemeMode.SYSTEM, // Add themeMode parameter
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Check for specific themes first
        themeMode == ThemeMode.GRUVBOX -> if (darkTheme) GruvboxDarkColorScheme else GruvboxLightColorScheme
        themeMode == ThemeMode.NORD -> if (darkTheme) NordDarkColorScheme else NordLightColorScheme
        themeMode == ThemeMode.DRACULA -> DraculaColorScheme  // Dracula is always dark
        themeMode == ThemeMode.SOLARIZED -> if (darkTheme) SolarizedDarkColorScheme else SolarizedLightColorScheme
        themeMode == ThemeMode.MONOKAI -> MonokaiColorScheme  // Monokai is always dark
        
        // Then check for dynamic colors
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}