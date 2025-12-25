# Bottom Navigation Bar Implementation

This document details the implementation of the Bottom Navigation Bar in `FinalBenchmark2`, focusing on the **Frosted Glass (Glassmorphism)** effect achieved using the `dev.chrisbanes.haze` library.

## Overview

The navigation bar creates a premium "floating glass" aesthetic by combining:
1.  **Haze Blur**: Real-time background blurring.
2.  **Transparency**: Semi-transparent surface colors.
3.  **Shape & Shadow**: Rounded pill shape with elevation.

## Core Dependencies

```kotlin
implementation("dev.chrisbanes.haze:haze:0.5.1")
```

## Implementation Details

The implementation is encapsulated in `FrostedGlassNavigation.kt`.

### 1. State Management (`MainActivity.kt`)

A shared `HazeState` is created at the top-level (Scaffold) and passed down. This allows the `hazeChild` (navigation bar) to blur the content of the `haze` (main screen content).

```kotlin
// MainActivity.kt
val hazeState = remember { HazeState() }

Scaffold(
    // ...
) { innerPadding ->
    // The content to be blurred must be wrapped in .haze(hazeState)
    MainNavigation(
         modifier = Modifier.haze(state = hazeState),
         hazeState = hazeState
    )
}
```

### 2. The Navigation Bar Component (`FrostedGlassNavigation.kt`)

The bar itself is a `Box` that applies the `hazeChild` modifier.

```kotlin
@Composable
fun FrostedGlassNavigationBar(
    navController: NavHostController,
    hazeState: HazeState // Received from parent
) {
    // 1. Define the Glass Color (Low Alpha)
    val surfaceColor = MaterialTheme.colorScheme.surface
    val blurBackgroundColor = remember(surfaceColor) {
        surfaceColor.copy(alpha = 0.2f) // Critical: High transparency for glass effect
    }

    Box(
        modifier = Modifier
            .padding(horizontal = 24.dp, vertical = 24.dp) // Float effect
            .fillMaxWidth()
            .height(72.dp)
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(percent = 50)) // Drop shadow
            .clip(RoundedCornerShape(percent = 50)) // Pill shape
            
            // --- THE GLASS EFFECT ---
            .hazeChild(state = hazeState) {
                backgroundColor = blurBackgroundColor
                blurRadius = 30.dp   // Strong blur
                noiseFactor = 0.05f  // Subtle noise for texture
            }
            // ------------------------
            
            .border(
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(percent = 50)
            )
    ) {
        // 3. Transparent Container
        NavigationBar(
            containerColor = Color.Transparent, // MUST be transparent
            // ...
        ) {
            // Navigation Items...
        }
    }
}
```

## Key Properties for the Look

| Property | Value | Purpose |
| :--- | :--- | :--- |
| **`alpha`** | `0.2f` | Makes the bar see-through. Higher values look like solid plastic. |
| **`blurRadius`** | `30.dp` | Creates the frost effect. Lower is sharper; higher is cloudier. |
| **`noiseFactor`** | `0.05f` | Adds grain to simulate real frosted glass and prevent banding. |
| **`containerColor`** | `Transparent` | Essential. If the inner `NavigationBar` has color, it blocks the blur. |
| **`shape`** | `RoundedCornerShape(50)` | Creates the pill/oval shape. |

## Usage

To use this component in a screen:

1.  Pass the `HazeState` down from your main scaffold.
2.  Ensure existing content behind the bar is marked with `.haze(state = hazeState)`.
3.  Place `FrostedGlassNavigationBar` in the `bottomBar` slot of your Scaffold.
