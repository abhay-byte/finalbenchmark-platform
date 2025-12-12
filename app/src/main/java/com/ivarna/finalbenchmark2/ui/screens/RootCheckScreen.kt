package com.ivarna.finalbenchmark2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ivarna.finalbenchmark2.utils.OnboardingPreferences
import com.ivarna.finalbenchmark2.utils.RootAccessManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed interface RootUiState {
        object Checking : RootUiState
        object Granted : RootUiState
        object NotAvailable : RootUiState
}

@Composable
fun RootCheckScreen(
        onNextClicked: () -> Unit,
        onBackClicked: () -> Unit = {},
        onRetryRootAccess: () -> Unit = {},
        modifier: Modifier = Modifier
) {
        var rootUiState by remember { mutableStateOf<RootUiState>(RootUiState.Checking) }
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        val onboardingPreferences = remember { OnboardingPreferences(context) }

        // Perform automatic root check when screen loads
        val hasRootChecked = remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
                if (!hasRootChecked.value) {
                        scope.launch {
                                // Use RootAccessManager.isRootGranted() - this will use cached
                                // result if available
                                val hasRoot = RootAccessManager.isRootGranted()
                                rootUiState =
                                        if (hasRoot) {
                                                RootUiState.Granted
                                        } else {
                                                RootUiState.NotAvailable
                                        }
                                hasRootChecked.value = true
                        }
                }
        }

        Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                ) {
                        // Central content
                        Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.weight(1f)
                        ) {
                                // Icon Container with status color
                                val iconColor =
                                        when (rootUiState) {
                                                is RootUiState.Checking ->
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                is RootUiState.Granted ->
                                                        MaterialTheme.colorScheme.primary
                                                is RootUiState.NotAvailable ->
                                                        MaterialTheme.colorScheme.error
                                        }

                                Card(
                                        modifier = Modifier.size(100.dp),
                                        shape = RoundedCornerShape(50.dp),
                                        colors =
                                                CardDefaults.cardColors(
                                                        containerColor =
                                                                MaterialTheme.colorScheme
                                                                        .primaryContainer.copy(
                                                                        alpha = 0.8f
                                                                )
                                                ),
                                        elevation =
                                                CardDefaults.cardElevation(defaultElevation = 12.dp)
                                ) {
                                        Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.fillMaxSize()
                                        ) {
                                                Icon(
                                                        imageVector = Icons.Rounded.Security,
                                                        contentDescription = "Root Access Status",
                                                        modifier = Modifier.size(48.dp),
                                                        tint = iconColor
                                                )
                                        }
                                }

                                // Separate loading indicator
                                if (rootUiState is RootUiState.Checking) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        CircularProgressIndicator(
                                                modifier = Modifier.size(32.dp),
                                                color = MaterialTheme.colorScheme.primary
                                        )
                                }

                                Spacer(modifier = Modifier.height(32.dp))

                                // Headline
                                val headlineText =
                                        when (rootUiState) {
                                                is RootUiState.Checking -> "Checking Root Access..."
                                                is RootUiState.Granted -> "Root Access Granted"
                                                is RootUiState.NotAvailable ->
                                                        "Root Access Not Detected"
                                        }

                                val headlineColor =
                                        when (rootUiState) {
                                                is RootUiState.Checking ->
                                                        MaterialTheme.colorScheme.onSurface
                                                is RootUiState.Granted ->
                                                        MaterialTheme.colorScheme.primary
                                                is RootUiState.NotAvailable ->
                                                        MaterialTheme.colorScheme.error
                                        }

                                Text(
                                        text = headlineText,
                                        style =
                                                MaterialTheme.typography.headlineLarge.copy(
                                                        fontWeight = FontWeight.Bold
                                                ),
                                        color = headlineColor,
                                        textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                // Status Information Card
                                Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(20.dp),
                                        colors =
                                                CardDefaults.cardColors(
                                                        containerColor =
                                                                MaterialTheme.colorScheme.surface
                                                ),
                                        border =
                                                androidx.compose.foundation.BorderStroke(
                                                        1.dp,
                                                        MaterialTheme.colorScheme.outline.copy(
                                                                alpha = 0.1f
                                                        )
                                                ),
                                        elevation =
                                                CardDefaults.cardElevation(defaultElevation = 4.dp)
                                ) {
                                        Column(modifier = Modifier.padding(20.dp)) {
                                                val statusText =
                                                        when (rootUiState) {
                                                                is RootUiState.Checking ->
                                                                        "Checking your device for root access..."
                                                                is RootUiState.Granted ->
                                                                        "Full Control Enabled. You have access to advanced features like CPU Affinity, GPU Frequency monitoring, and Governor tuning."
                                                                is RootUiState.NotAvailable ->
                                                                        "Running in Standard Mode. Benchmarks will function correctly, but detailed hardware stats (GPU Load, Exact Frequencies) and optimizations will be disabled."
                                                        }

                                                Text(
                                                        text = statusText,
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant,
                                                        textAlign = TextAlign.Start
                                                )
                                        }
                                }

                                // Root Warning Card - shown when root is granted
                                if (rootUiState is RootUiState.Granted) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(20.dp),
                                                colors =
                                                        CardDefaults.cardColors(
                                                                containerColor =
                                                                        Color(0xFFFF9800)
                                                                                .copy(alpha = 0.1f)
                                                        ),
                                                border =
                                                        androidx.compose.foundation.BorderStroke(
                                                                2.dp,
                                                                Color(0xFFFF9800)
                                                        ),
                                                elevation =
                                                        CardDefaults.cardElevation(
                                                                defaultElevation = 4.dp
                                                        )
                                        ) {
                                                Column(modifier = Modifier.padding(20.dp)) {
                                                        Row(
                                                                verticalAlignment =
                                                                        Alignment.CenterVertically
                                                        ) {
                                                                Icon(
                                                                        imageVector =
                                                                                Icons.Rounded
                                                                                        .Security,
                                                                        contentDescription =
                                                                                "Warning",
                                                                        tint =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .error,
                                                                        modifier =
                                                                                Modifier.size(24.dp)
                                                                )
                                                                Spacer(
                                                                        modifier =
                                                                                Modifier.width(
                                                                                        12.dp
                                                                                )
                                                                )
                                                                Text(
                                                                        text =
                                                                                "⚠️ Root Access Warning",
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .titleMedium
                                                                                        .copy(
                                                                                                fontWeight =
                                                                                                        FontWeight
                                                                                                                .Bold
                                                                                        ),
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .error
                                                                )
                                                        }

                                                        Spacer(modifier = Modifier.height(12.dp))

                                                        Text(
                                                                text =
                                                                        "Running benchmarks with root access will push your device to extreme limits. Your device may run significantly hotter than normal, which could potentially cause hardware damage.",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodyMedium,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurface,
                                                                textAlign = TextAlign.Start
                                                        )

                                                        Spacer(modifier = Modifier.height(8.dp))

                                                        Text(
                                                                text =
                                                                        "⚠️ We are NOT responsible for any damage that may occur to your device during or after benchmarking with root access enabled.",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodyMedium.copy(
                                                                                fontWeight =
                                                                                        FontWeight
                                                                                                .SemiBold
                                                                        ),
                                                                color = Color(0xFFFF6B00),
                                                                textAlign = TextAlign.Start
                                                        )
                                                }
                                        }
                                }

                                // "Request Root Again" button for manual retry
                                if (rootUiState is RootUiState.NotAvailable) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        OutlinedButton(
                                                onClick = {
                                                        rootUiState = RootUiState.Checking
                                                        // Use forceRefresh() to clear cache and
                                                        // perform new check
                                                        scope.launch {
                                                                delay(
                                                                        500
                                                                ) // Small delay for better UX
                                                                val hasRoot =
                                                                        RootAccessManager
                                                                                .forceRefresh()
                                                                rootUiState =
                                                                        if (hasRoot) {
                                                                                RootUiState.Granted
                                                                        } else {
                                                                                RootUiState
                                                                                        .NotAvailable
                                                                        }
                                                        }
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp)
                                        ) {
                                                Text(
                                                        text = "Request Root Again",
                                                        style = MaterialTheme.typography.titleMedium
                                                )
                                        }
                                }
                        }

                        // Action Button
                        Button(
                                onClick = { onNextClicked() },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                colors =
                                        ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary
                                        ),
                                shape = RoundedCornerShape(16.dp)
                        ) {
                                Text(
                                        text = "Next",
                                        style =
                                                MaterialTheme.typography.titleMedium.copy(
                                                        fontWeight = FontWeight.SemiBold
                                                )
                                )
                        }
                }
        }
}
