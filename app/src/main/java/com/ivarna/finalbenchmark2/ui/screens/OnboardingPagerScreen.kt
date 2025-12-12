package com.ivarna.finalbenchmark2.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingPagerScreen(onOnboardingComplete: () -> Unit, modifier: Modifier = Modifier) {
    val pagerState = rememberPagerState(pageCount = { 5 })
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Horizontal Pager for swipe navigation
            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                when (page) {
                    0 ->
                            WelcomeScreen(
                                    onNextClicked = {
                                        scope.launch { pagerState.animateScrollToPage(page + 1) }
                                    }
                            )
                    1 ->
                            RootCheckScreen(
                                    onNextClicked = {
                                        scope.launch { pagerState.animateScrollToPage(page + 1) }
                                    },
                                    onBackClicked = {
                                        scope.launch { pagerState.animateScrollToPage(page - 1) }
                                    }
                            )
                    2 ->
                            PermissionsScreen(
                                    onNextClicked = {
                                        scope.launch { pagerState.animateScrollToPage(page + 1) }
                                    },
                                    onBackClicked = {
                                        scope.launch { pagerState.animateScrollToPage(page - 1) }
                                    }
                            )
                    3 ->
                            ThemeSelectionScreen(
                                    onNextClicked = {
                                        scope.launch { pagerState.animateScrollToPage(page + 1) }
                                    },
                                    onBackClicked = {
                                        scope.launch { pagerState.animateScrollToPage(page - 1) }
                                    }
                            )
                    4 ->
                            PowerCalibrationScreen(
                                    onNextClicked = { onOnboardingComplete() },
                                    onBackClicked = {
                                        scope.launch { pagerState.animateScrollToPage(page - 1) }
                                    }
                            )
                }
            }

            // Page Indicators
            Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(5) { iteration ->
                    val color =
                            if (pagerState.currentPage == iteration) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            }

                    val size = if (pagerState.currentPage == iteration) 10.dp else 8.dp

                    Box(
                            modifier =
                                    Modifier.padding(horizontal = 4.dp)
                                            .size(size)
                                            .clip(CircleShape)
                                            .background(color)
                    )
                }
            }
        }
    }
}
