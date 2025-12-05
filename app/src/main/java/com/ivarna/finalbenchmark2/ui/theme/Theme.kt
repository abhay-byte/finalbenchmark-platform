package com.ivarna.finalbenchmark2.ui.theme

import android.app.Activity
import androidx.compose.ui.graphics.Color
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
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4F378B), // Darker purple container
    onPrimaryContainer = Color.White,
    
    secondary = PurpleGrey80,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF4A4458), // Darker grey-purple container
    onSecondaryContainer = Color.White,
    
    tertiary = Pink80,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF633B48), // Darker pink container
    onTertiaryContainer = Color.White,
    
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E8),
    
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E8),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    surfaceTint = Purple80,
    
    inverseSurface = Color(0xFFE6E1E8),
    inverseOnSurface = Color(0xFF313033),
    inversePrimary = Color(0xFF6750A4),
    
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
    
    scrim = Color.Black.copy(alpha = 0.32f),
    
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

val LightColorScheme = lightColorScheme(
    primary = Purple40,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF), // Lighter purple container
    onPrimaryContainer = Color(0xFF22005D), // Darker purple container text
    
    secondary = PurpleGrey40,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8DEF8), // Lighter grey-purple container
    onSecondaryContainer = Color(0xFF1D192B), // Darker grey-purple container text
    
    tertiary = Pink40,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD8E4), // Lighter pink container
    onTertiaryContainer = Color(0xFF492D35), // Darker pink container text
    
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    surfaceTint = Purple40,
    
    inverseSurface = Color(0xFF313033),
    inverseOnSurface = Color(0xFFF4EFF4),
    inversePrimary = Color(0xFFD0BCFF),
    
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFC9C5D0),
    
    scrim = Color.Black.copy(alpha = 0.32f),
    
    error = Color(0xFFB3261E),
    onError = Color(0xFFFAFAFA),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B)
)

// Gruvbox Color Schemes
val GruvboxDarkColorScheme = darkColorScheme(
    // Primary colors
    primary = GruvboxDarkPrimary,
    onPrimary = GruvboxDarkBg,
    primaryContainer = GruvboxDarkAccent,
    onPrimaryContainer = GruvboxDarkBg,
    
    // Secondary colors
    secondary = GruvboxDarkSecondary,
    onSecondary = GruvboxDarkBg,
    secondaryContainer = Color(0xFFA7C7A9), // Lighter green container
    onSecondaryContainer = GruvboxDarkBg,
    
    // Tertiary colors
    tertiary = GruvboxDarkWarning,
    onTertiary = GruvboxDarkBg,
    tertiaryContainer = Color(0xFFD4A24E), // Lighter orange container
    onTertiaryContainer = GruvboxDarkBg,
    
    // Error colors
    error = GruvboxDarkError,
    onError = GruvboxDarkBg,
    errorContainer = Color(0xFFFC5863), // Lighter red container
    onErrorContainer = GruvboxDarkBg,
    
    // Background colors
    background = GruvboxDarkBg,
    onBackground = GruvboxDarkText,
    
    // Surface colors
    surface = GruvboxDarkSurface,
    onSurface = GruvboxDarkText,
    surfaceVariant = GruvboxDarkBorder,
    onSurfaceVariant = GruvboxDarkText,
    surfaceTint = GruvboxDarkPrimary,
    
    // Inverse colors
    inverseSurface = GruvboxDarkText,
    inverseOnSurface = GruvboxDarkBg,
    inversePrimary = GruvboxDarkPrimary,
    
    // Outline colors
    outline = GruvboxDarkBorder,
    outlineVariant = Color(0xFF7C6F64),
    
    // Scrim
    scrim = Color.Black.copy(alpha = 0.32f)
)

val GruvboxLightColorScheme = lightColorScheme(
    // Primary colors
    primary = GruvboxLightPrimary,
    onPrimary = GruvboxLightBg,
    primaryContainer = GruvboxLightAccent,
    onPrimaryContainer = GruvboxLightBg,
    
    // Secondary colors
    secondary = GruvboxLightSecondary,
    onSecondary = GruvboxLightBg,
    secondaryContainer = Color(0xFF7DAE84), // Darker green container
    onSecondaryContainer = GruvboxLightBg,
    
    // Tertiary colors
    tertiary = GruvboxLightWarning,
    onTertiary = GruvboxLightBg,
    tertiaryContainer = Color(0xFF9B6D0D), // Darker yellow container
    onTertiaryContainer = GruvboxLightBg,
    
    // Error colors
    error = GruvboxLightError,
    onError = GruvboxLightBg,
    errorContainer = Color(0xFFA52505), // Darker red container
    onErrorContainer = GruvboxLightBg,
    
    // Background colors
    background = GruvboxLightBg,
    onBackground = GruvboxLightText,
    
    // Surface colors
    surface = GruvboxLightSurface,
    onSurface = GruvboxLightText,
    surfaceVariant = GruvboxLightBorder,
    onSurfaceVariant = GruvboxLightText,
    surfaceTint = GruvboxLightPrimary,
    
    // Inverse colors
    inverseSurface = GruvboxLightText,
    inverseOnSurface = GruvboxLightBg,
    inversePrimary = GruvboxLightPrimary,
    
    // Outline colors
    outline = GruvboxLightBorder,
    outlineVariant = Color(0xFFBFAF95),
    
    // Scrim
    scrim = Color.Black.copy(alpha = 0.32f)
)

// Nord Color Schemes
val NordDarkColorScheme = darkColorScheme(
    // Primary colors
    primary = NordDarkPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF5D9DD5), // Lighter blue container
    onPrimaryContainer = Color.White,
    
    // Secondary colors
    secondary = NordDarkSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF6394AA), // Lighter blue-green container
    onSecondaryContainer = Color.White,
    
    // Tertiary colors
    tertiary = NordDarkAccent,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFFC7A370), // Lighter orange container
    onTertiaryContainer = Color.Black,
    
    // Error colors
    error = NordDarkError,
    onError = Color.White,
    errorContainer = Color(0xFF94754), // Darker red container
    onErrorContainer = Color.White,
    
    // Background colors
    background = NordDarkBg,
    onBackground = NordDarkText,
    
    // Surface colors
    surface = NordDarkSurface,
    onSurface = NordDarkText,
    surfaceVariant = NordDarkBorder,
    onSurfaceVariant = NordDarkText,
    surfaceTint = NordDarkPrimary,
    
    // Inverse colors
    inverseSurface = NordDarkText,
    inverseOnSurface = NordDarkBg,
    inversePrimary = NordDarkPrimary,
    
    // Outline colors
    outline = NordDarkBorder,
    outlineVariant = Color(0xFF5C6D84),
    
    // Scrim
    scrim = Color.Black.copy(alpha = 0.32f)
)

val NordLightColorScheme = lightColorScheme(
    // Primary colors
    primary = NordLightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3A5F7A), // Darker blue container
    onPrimaryContainer = Color.White,
    
    // Secondary colors
    secondary = NordLightSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF4A7B8C), // Darker blue-green container
    onSecondaryContainer = Color.White,
    
    // Tertiary colors
    tertiary = NordLightAccent,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFFA87D4D), // Darker orange container
    onTertiaryContainer = Color.Black,
    
    // Error colors
    error = NordLightError,
    onError = Color.White,
    errorContainer = Color(0xFF944754), // Darker red container
    onErrorContainer = Color.White,
    
    // Background colors
    background = NordLightBg,
    onBackground = NordLightText,
    
    // Surface colors
    surface = NordLightSurface,
    onSurface = NordLightText,
    surfaceVariant = NordLightBorder,
    onSurfaceVariant = NordLightText,
    surfaceTint = NordLightPrimary,
    
    // Inverse colors
    inverseSurface = NordLightText,
    inverseOnSurface = NordLightBg,
    inversePrimary = NordLightPrimary,
    
    // Outline colors
    outline = NordLightBorder,
    outlineVariant = Color(0xFFA3B8CC),
    
    // Scrim
    scrim = Color.Black.copy(alpha = 0.32f)
)

// Dracula Color Scheme
val DraculaColorScheme = darkColorScheme(
    // Primary colors
    primary = DraculaPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA37AC1), // Lighter purple container
    onPrimaryContainer = Color.White,
    
    // Secondary colors
    secondary = DraculaSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE066AF), // Lighter pink container
    onSecondaryContainer = Color.White,
    
    // Tertiary colors
    tertiary = DraculaAccent,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFFE2D378), // Lighter yellow container
    onTertiaryContainer = Color.Black,
    
    // Error colors
    error = DraculaError,
    onError = Color.White,
    errorContainer = Color(0xFFCC444B), // Darker red container
    onErrorContainer = Color.White,
    
    // Background colors
    background = DraculaBg,
    onBackground = DraculaText,
    
    // Surface colors
    surface = DraculaSurface,
    onSurface = DraculaText,
    surfaceVariant = DraculaBorder,
    onSurfaceVariant = DraculaText,
    surfaceTint = DraculaPrimary,
    
    // Inverse colors
    inverseSurface = DraculaText,
    inverseOnSurface = DraculaBg,
    inversePrimary = DraculaPrimary,
    
    // Outline colors
    outline = DraculaBorder,
    outlineVariant = Color(0xFF525577),
    
    // Scrim
    scrim = Color.Black.copy(alpha = 0.32f)
)

// Solarized Color Schemes
val SolarizedDarkColorScheme = darkColorScheme(
    // Primary colors
    primary = SolarizedDarkPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4E9EDD), // Lighter blue container
    onPrimaryContainer = Color.White,
    
    // Secondary colors
    secondary = SolarizedDarkSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF4CB2A8), // Lighter teal container
    onSecondaryContainer = Color.White,
    
    // Tertiary colors
    tertiary = SolarizedDarkAccent,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFFC99F21), // Lighter yellow container
    onTertiaryContainer = Color.Black,
    
    // Error colors
    error = SolarizedDarkError,
    onError = Color.White,
    errorContainer = Color(0xFFED554F), // Lighter red container
    onErrorContainer = Color.White,
    
    // Background colors
    background = SolarizedDarkBg,
    onBackground = SolarizedDarkText,
    
    // Surface colors
    surface = SolarizedDarkSurface,
    onSurface = SolarizedDarkText,
    surfaceVariant = SolarizedDarkBorder,
    onSurfaceVariant = SolarizedDarkText,
    surfaceTint = SolarizedDarkPrimary,
    
    // Inverse colors
    inverseSurface = SolarizedDarkText,
    inverseOnSurface = SolarizedDarkBg,
    inversePrimary = SolarizedDarkPrimary,
    
    // Outline colors
    outline = SolarizedDarkBorder,
    outlineVariant = Color(0xFF6C8593),
    
    // Scrim
    scrim = Color.Black.copy(alpha = 0.32f)
)

val SolarizedLightColorScheme = lightColorScheme(
    // Primary colors
    primary = SolarizedLightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E6B99), // Darker blue container
    onPrimaryContainer = Color.White,
    
    // Secondary colors
    secondary = SolarizedLightSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF1B7F74), // Darker teal container
    onSecondaryContainer = Color.White,
    
    // Tertiary colors
    tertiary = SolarizedLightAccent,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF856500), // Darker yellow container
    onTertiaryContainer = Color.Black,
    
    // Error colors
    error = SolarizedLightError,
    onError = Color.White,
    errorContainer = Color(0xFFA93226), // Darker red container
    onErrorContainer = Color.White,
    
    // Background colors
    background = SolarizedLightBg,
    onBackground = SolarizedLightText,
    
    // Surface colors
    surface = SolarizedLightSurface,
    onSurface = SolarizedLightText,
    surfaceVariant = SolarizedLightBorder,
    onSurfaceVariant = SolarizedLightText,
    surfaceTint = SolarizedLightPrimary,
    
    // Inverse colors
    inverseSurface = SolarizedLightText,
    inverseOnSurface = SolarizedLightBg,
    inversePrimary = SolarizedLightPrimary,
    
    // Outline colors
    outline = SolarizedLightBorder,
    outlineVariant = Color(0xFFA5A5A1),
    
    // Scrim
    scrim = Color.Black.copy(alpha = 0.32f)
)

// Monokai Color Scheme
val MonokaiColorScheme = darkColorScheme(
    // Primary colors
    primary = MonokaiPrimary,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFFC1E84D), // Lighter green container
    onPrimaryContainer = Color.Black,
    
    // Secondary colors
    secondary = MonokaiSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC8A2E1), // Lighter purple container
    onSecondaryContainer = Color.White,
    
    // Tertiary colors
    tertiary = MonokaiAccent,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFFFFE78E), // Lighter yellow container
    onTertiaryContainer = Color.Black,
    
    // Error colors
    error = MonokaiError,
    onError = Color.White,
    errorContainer = Color(0xFFFF5B7B), // Lighter red container
    onErrorContainer = Color.White,
    
    // Background colors
    background = MonokaiBg,
    onBackground = MonokaiText,
    
    // Surface colors
    surface = MonokaiSurface,
    onSurface = MonokaiText,
    surfaceVariant = MonokaiBorder,
    onSurfaceVariant = MonokaiText,
    surfaceTint = MonokaiPrimary,
    
    // Inverse colors
    inverseSurface = MonokaiText,
    inverseOnSurface = MonokaiBg,
    inversePrimary = MonokaiPrimary,
    
    // Outline colors
    outline = MonokaiBorder,
    outlineVariant = Color(0xFF65654D),
    
    // Scrim
    scrim = Color.Black.copy(alpha = 0.32f)
)

// Sky Breeze Color Scheme
val SkyBreezeColorScheme = lightColorScheme(
    // Primary colors
    primary = SkyBreezePrimary,
    onPrimary = Color.White,
    primaryContainer = SkyBreezePrimaryLight,
    onPrimaryContainer = SkyBreezePrimaryDark,
    
    // Secondary colors
    secondary = SkyBreezeInfo,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE), // Light cyan container
    onSecondaryContainer = Color(0xFF164E63), // Dark cyan
    
    // Tertiary colors
    tertiary = SkyBreezeSuccess,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD1FAE5), // Light green container
    onTertiaryContainer = Color(0xFF065F46), // Dark green
    
    // Error colors
    error = SkyBreezeError,
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2), // Light red container
    onErrorContainer = Color(0xFF991B1B), // Dark red
    
    // Background colors
    background = SkyBreezeBg,
    onBackground = SkyBreezeTextPrimary,
    
    // Surface colors
    surface = SkyBreezeSurface,
    onSurface = SkyBreezeTextPrimary,
    surfaceVariant = Color(0xFFF1F5F9), // Slight variation
    onSurfaceVariant = SkyBreezeTextSecondary,
    surfaceTint = SkyBreezePrimary,
    
    // Inverse colors (for contrast)
    inverseSurface = SkyBreezeTextPrimary,
    inverseOnSurface = SkyBreezeBg,
    inversePrimary = SkyBreezePrimaryLight,
    
    // Outline colors
    outline = SkyBreezeBorder,
    outlineVariant = Color(0xFFF1F5F9),
    
    // Scrim (for modals/dialogs)
    scrim = Color.Black.copy(alpha = 0.32f)
)

// Lavender Dream Color Scheme
val LavenderDreamColorScheme = lightColorScheme(
    // Primary colors
    primary = LavenderDreamPrimary,
    onPrimary = Color.White,
    primaryContainer = LavenderDreamPrimaryLight,
    onPrimaryContainer = LavenderDreamPrimaryDark,
    
    // Secondary colors
    secondary = LavenderDreamInfo,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3E8FF), // Light purple container
    onSecondaryContainer = Color(0xFF581C87), // Dark purple
    
    // Tertiary colors
    tertiary = LavenderDreamSuccess,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDCFCE7), // Light green container
    onTertiaryContainer = Color(0xFF166534), // Dark green
    
    // Error colors
    error = LavenderDreamError,
    onError = Color.White,
    errorContainer = Color(0xFFFFE4E6), // Light rose container
    onErrorContainer = Color(0xFF9F1239), // Dark rose
    
    // Background colors
    background = LavenderDreamBg,
    onBackground = LavenderDreamTextPrimary,
    
    // Surface colors
    surface = LavenderDreamSurface,
    onSurface = LavenderDreamTextPrimary,
    surfaceVariant = Color(0xFFF5F5F4), // Stone variant
    onSurfaceVariant = LavenderDreamTextSecondary,
    surfaceTint = LavenderDreamPrimary,
    
    // Inverse colors
    inverseSurface = LavenderDreamTextPrimary,
    inverseOnSurface = LavenderDreamBg,
    inversePrimary = LavenderDreamPrimaryLight,
    
    // Outline colors
    outline = LavenderDreamBorder,
    outlineVariant = Color(0xFFF5F5F4),
    
    // Scrim
    scrim = Color.Black.copy(alpha = 0.32f)
)

// Mint Fresh Color Scheme
val MintFreshColorScheme = lightColorScheme(
    // Primary colors
    primary = MintFreshPrimary,
    onPrimary = Color.White,
    primaryContainer = MintFreshPrimaryLight,
    onPrimaryContainer = MintFreshPrimaryDark,
    
    // Secondary colors
    secondary = MintFreshInfo,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDBEAFE), // Light blue container
    onSecondaryContainer = Color(0xFF1E40AF), // Dark blue
    
    // Tertiary colors
    tertiary = MintFreshSuccess,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD1FAE5), // Light emerald container
    onTertiaryContainer = Color(0xFF065F46), // Dark emerald
    
    // Error colors
    error = MintFreshError,
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2), // Light red container
    onErrorContainer = Color(0xFF991B1B), // Dark red
    
    // Background colors
    background = MintFreshBg,
    onBackground = MintFreshTextPrimary,
    
    // Surface colors
    surface = MintFreshSurface,
    onSurface = MintFreshTextPrimary,
    surfaceVariant = Color(0xFFF3F4F6), // Gray variant
    onSurfaceVariant = MintFreshTextSecondary,
    surfaceTint = MintFreshPrimary,
    
    // Inverse colors
    inverseSurface = MintFreshTextPrimary,
    inverseOnSurface = MintFreshBg,
    inversePrimary = MintFreshPrimaryLight,
    
    // Outline colors
    outline = MintFreshBorder,
    outlineVariant = Color(0xFFF3F4F6),
    
    // Scrim
    scrim = Color.Black.copy(alpha = 0.32f)
)

// AMOLED Black Color Scheme
val AmoledBlackColorScheme = darkColorScheme(
    // Primary colors
    primary = AmoledBlackPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E3A8A), // Dark blue container
    onPrimaryContainer = Color(0xFFD1E1FF), // Light blue container text
    
    // Secondary colors
    secondary = AmoledBlackSuccess,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF0A2D1F), // Dark green container
    onSecondaryContainer = Color(0xFFA3F0C8), // Light green container text
    
    // Tertiary colors
    tertiary = AmoledBlackWarning,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF5D4200), // Dark yellow container
    onTertiaryContainer = Color(0xFFFFE097), // Light yellow container text
    
    // Error colors
    error = AmoledBlackError,
    onError = Color.White,
    errorContainer = Color(0xFF410E0B), // Dark red container
    onErrorContainer = Color(0xFFFFB4AB), // Light red container text
    
    // Background colors
    background = Color.Black, // Pure black for AMOLED
    onBackground = AmoledBlackTextPrimary,
    
    // Surface colors
    surface = Color.Black, // Pure black for AMOLED
    onSurface = AmoledBlackTextPrimary,
    surfaceVariant = AmoledBlackBorder,
    onSurfaceVariant = AmoledBlackTextSecondary,
    surfaceTint = AmoledBlackPrimary,
    
    // Inverse colors
    inverseSurface = AmoledBlackTextPrimary,
    inverseOnSurface = Color.Black, // Pure black for AMOLED
    inversePrimary = AmoledBlackPrimary,
    
    // Outline colors
    outline = AmoledBlackBorder,
    outlineVariant = Color(0xFF41494F),
    
    // Scrim
    scrim = Color.Black.copy(alpha = 0.32f)
)

@Composable
fun FinalBenchmark2Theme(
    darkTheme: Boolean = shouldUseDarkTheme(),
    // We default to SYSTEM here, but we will check LocalThemeMode inside
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    // FIX: Resolve the actual mode.
    // If the parameter is SYSTEM (default), we check what is currently set in the CompositionLocal.
    // This ensures that when the user changes settings, this Composable reads the new value.
    val localMode = LocalThemeMode.current.value
    val activeThemeMode = if (themeMode == ThemeMode.SYSTEM && localMode != ThemeMode.SYSTEM) {
        localMode
    } else {
        // If local is SYSTEM, or if a specific mode was passed as a parameter (e.g. for Previews)
        if (themeMode != ThemeMode.SYSTEM) themeMode else localMode
    }

    val colorScheme = when {
        // Check for specific themes using the RESOLVED mode
        activeThemeMode == ThemeMode.GRUVBOX -> if (darkTheme) GruvboxDarkColorScheme else GruvboxLightColorScheme
        activeThemeMode == ThemeMode.NORD -> if (darkTheme) NordDarkColorScheme else NordLightColorScheme
        activeThemeMode == ThemeMode.DRACULA -> DraculaColorScheme
        activeThemeMode == ThemeMode.SOLARIZED -> if (darkTheme) SolarizedDarkColorScheme else SolarizedLightColorScheme
        activeThemeMode == ThemeMode.MONOKAI -> MonokaiColorScheme
        activeThemeMode == ThemeMode.SKY_BREEZE -> SkyBreezeColorScheme
        activeThemeMode == ThemeMode.LAVENDER_DREAM -> LavenderDreamColorScheme
        activeThemeMode == ThemeMode.MINT_FRESH -> MintFreshColorScheme
        activeThemeMode == ThemeMode.AMOLED_BLACK -> AmoledBlackColorScheme
        
        // Then check for dynamic colors (Only if activeMode is SYSTEM)
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
            
            // FIX: Use activeThemeMode here too
            val finalBackgroundColor = if (activeThemeMode == ThemeMode.AMOLED_BLACK) {
                Color.Black.toArgb()
            } else {
                colorScheme.background.toArgb()
            }
            
            window.statusBarColor = finalBackgroundColor
            window.navigationBarColor = finalBackgroundColor
            
            // Fix light/dark status bar icons based on the resolved theme
            val isLightTheme = when (activeThemeMode) {
                ThemeMode.SKY_BREEZE, ThemeMode.LAVENDER_DREAM,
                ThemeMode.MINT_FRESH -> true
                ThemeMode.DRACULA, ThemeMode.MONOKAI,
                ThemeMode.AMOLED_BLACK -> false
                ThemeMode.GRUVBOX, ThemeMode.NORD, ThemeMode.SOLARIZED -> !darkTheme
                else -> !darkTheme
            }
            
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.isAppearanceLightStatusBars = isLightTheme
            controller.isAppearanceLightNavigationBars = isLightTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}