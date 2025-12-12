package com.ivarna.finalbenchmark2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ivarna.finalbenchmark2.R
import com.ivarna.finalbenchmark2.utils.OnboardingPreferences

@Composable
fun WelcomeScreen(onNextClicked: () -> Unit, modifier: Modifier = Modifier) {
        val context = LocalContext.current
        val onboardingPreferences = remember { OnboardingPreferences(context) }
        Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                ) {
                        // Central content - centered vertically
                        Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.weight(1f)
                        ) {
                                // App Logo with circular background
                                Box(
                                        modifier = Modifier.size(200.dp),
                                        contentAlignment = Alignment.Center
                                ) {
                                        // Grey circular background
                                        Box(
                                                modifier =
                                                        Modifier.size(180.dp)
                                                                .clip(
                                                                        androidx.compose.foundation
                                                                                .shape.CircleShape
                                                                )
                                                                .background(
                                                                        androidx.compose.ui.graphics
                                                                                .Color(0xFF2A2A2A)
                                                                )
                                        )
                                        // Logo on top
                                        androidx.compose.foundation.Image(
                                                painter = painterResource(id = R.drawable.logo_2),
                                                contentDescription = "App Logo",
                                                modifier = Modifier.size(160.dp)
                                        )
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                // Headline Text
                                Text(
                                        text = "Welcome to FinalBenchmark 2",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Subtitle Text
                                Text(
                                        text =
                                                "The ultimate tool to push your device to its limits.",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                )
                        }

                        // Action Button - anchored at bottom
                        Button(
                                onClick = { onNextClicked() },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                        ) {
                                Text(
                                        text = "Get Started",
                                        style = MaterialTheme.typography.titleMedium
                                )
                        }
                }
        }
}
