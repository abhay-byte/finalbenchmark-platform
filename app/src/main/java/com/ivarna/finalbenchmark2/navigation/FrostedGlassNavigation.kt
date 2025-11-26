package com.ivarna.finalbenchmark2.navigation

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.navigation.NavController
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.TileMode
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.getValue
import com.ivarna.finalbenchmark2.ui.screens.*

@Composable
fun FrostedGlassNavigationBar(
    items: List<BottomNavigationItem>,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
    ) {
        NavigationBar(
            containerColor = Color.Transparent, // needed so background shows
            tonalElevation = 0.dp
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route

                NavigationBarItem(
                    selected = isSelected,
                    onClick = {
                        if (!isSelected) { // prevent re-clicking same tab
                            navController.navigate(item.route) {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                            }
                        }
                    },
                    icon = {
                        when (item.route) {
                            "device" -> Icon(
                                painter = painterResource(id = com.ivarna.finalbenchmark2.R.drawable.mobile_24),
                                contentDescription = item.label,
                                tint = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            "history" -> Icon(
                                painter = painterResource(id = com.ivarna.finalbenchmark2.R.drawable.history_24),
                                contentDescription = item.label,
                                tint = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            else -> Icon(item.icon, contentDescription = item.label)
                        }
                    },
                    label = { Text(item.label) }
                )
            }
        }
    }
}

// Import the BottomNavigationItem from the main navigation file
// The BottomNavigationItem data class is defined in MainNavigation.kt