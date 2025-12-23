package com.ivarna.finalbenchmark2.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ivarna.finalbenchmark2.data.database.AppDatabase
import com.ivarna.finalbenchmark2.data.repository.HistoryRepository
import com.ivarna.finalbenchmark2.ui.theme.GruvboxDarkAccent
import com.ivarna.finalbenchmark2.ui.viewmodels.RankingItem
import com.ivarna.finalbenchmark2.ui.viewmodels.RankingScreenState
import com.ivarna.finalbenchmark2.ui.viewmodels.RankingViewModel
import com.ivarna.finalbenchmark2.ui.viewmodels.RankingViewModelFactory

@Composable
fun RankingsScreen(
    modifier: Modifier = Modifier,
    onDeviceClick: (RankingItem) -> Unit = {}
) {
    val context = LocalContext.current
    val historyRepository = remember {
        HistoryRepository(
            AppDatabase.getDatabase(context).benchmarkDao()
        )
    }
    val viewModel: RankingViewModel = viewModel(
        factory = RankingViewModelFactory(historyRepository)
    )

    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val screenState by viewModel.screenState.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = androidx.compose.ui.geometry.Offset(0f, 0f),
                    radius = 1000f
                )
            )
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceContainerLowest
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 24.dp)
        ) {
            // Large Modern Header
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                Text(
                    text = "Rankings",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1).sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Compare performance with other devices",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            // Filter Bar
            RankingFilterBar(
                selectedCategory = selectedCategory,
                onCategorySelect = { category ->
                    viewModel.selectCategory(category)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Content
            val state = screenState
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                when (state) {
                    is RankingScreenState.Loading -> {
                        LoadingContent()
                    }
                    is RankingScreenState.Success -> {
                        if (selectedCategory == "CPU") {
                            CpuRankingList(
                                rankings = state.rankings,
                                onItemClick = onDeviceClick
                            )
                        } else {
                            ComingSoonContent(category = selectedCategory)
                        }
                    }
                    is RankingScreenState.Error -> {
                        ErrorContent()
                    }
                }
            }
        }
    }
}

@Composable
private fun RankingFilterBar(
    selectedCategory: String,
    onCategorySelect: (String) -> Unit
) {
    val categories = listOf("Full", "CPU", "GPU", "RAM", "Storage", "Productivity", "AI")

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            val isSelected = selectedCategory == category
            
            // Custom Glass Chip
            Surface(
                onClick = { onCategorySelect(category) },
                shape = RoundedCornerShape(50), // Pill shape
                color = if (isSelected) 
                    MaterialTheme.colorScheme.primaryContainer 
                else 
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                border = if (isSelected) 
                    null 
                else 
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = category,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun CpuRankingList(
    rankings: List<RankingItem>,
    onItemClick: (RankingItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(rankings) { item ->
            RankingItemCard(
                item = item,
                onClick = { onItemClick(item) }
            )
        }
    }
}

@Composable
private fun RankingItemCard(
    item: RankingItem,
    onClick: () -> Unit
) {
    val topScoreMax = 1200
    val scoreProgress = (item.normalizedScore.toFloat() / topScoreMax).coerceIn(0f, 1f)
    
    val goldColor = Color(0xFFFFD700)
    val silverColor = Color(0xFFC0C0C0)
    val bronzeColor = Color(0xFFCD7F32)

    val rankColor = when (item.rank) {
        1 -> goldColor
        2 -> silverColor
        3 -> bronzeColor
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    }

    val isTop3 = item.rank <= 3

    // Glass Card Logic
    val containerColor = if (item.isCurrentUser) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }

    val borderColor = if (item.isCurrentUser) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            containerColor,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.05f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Header row: Rank, Name, Score
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rank Badge
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape) // Circle badge
                            .background(
                                if (isTop3) rankColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "#${item.rank}",
                            fontWeight = FontWeight.Black,
                            color = if (isTop3) rankColor else MaterialTheme.colorScheme.onSurface,
                            fontSize = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Center: Name and Tag
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        @OptIn(ExperimentalLayoutApi::class)
                        androidx.compose.foundation.layout.FlowRow(
                            verticalArrangement = Arrangement.Center,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                            // Display tag if present
                            item.tag?.let { tag ->
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                ) {
                                    Text(
                                        text = tag,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            // "You" Tag
                            if (item.isCurrentUser) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Rounded.Star,
                                            contentDescription = null,
                                            modifier = Modifier.size(10.dp),
                                            tint = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = "YOU",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Subtitle
                        Text(
                            text = "Single: ${item.singleCore} | Multi: ${item.multiCore}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Right: Score
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = item.normalizedScore.toString(),
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp,
                            letterSpacing = (-1).sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "PTS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Progress Bar (Sleek)
                LinearProgressIndicator(
                    progress = { scoreProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (isTop3) rankColor else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                )
            }
        }
    }
}

@Composable
private fun ComingSoonContent(
    category: String
) {
    com.ivarna.finalbenchmark2.ui.components.EmptyStateView(
        icon = Icons.Rounded.EmojiEvents,
        title = "Coming Soon",
        message = "$category rankings will be available in a future update."
    )
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ErrorContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.AccessTime, // Warning icon might be better but reusing existing import
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Error Loading Rankings",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Unable to fetch ranking data. Please try again later.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
