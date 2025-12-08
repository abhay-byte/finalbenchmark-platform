package com.ivarna.finalbenchmark2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import android.content.Context
import androidx.compose.ui.platform.LocalContext

@Composable
fun RankingsScreen(
    modifier: Modifier = Modifier
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = 8.dp)
    ) {
        // Filter Bar
        RankingFilterBar(
            selectedCategory = selectedCategory,
            onCategorySelect = { category ->
                viewModel.selectCategory(category)
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Content
        when (screenState) {
            is RankingScreenState.Loading -> {
                LoadingContent()
            }
            is RankingScreenState.Success -> {
                if (selectedCategory == "CPU") {
                    CpuRankingList(
                        rankings = (screenState as RankingScreenState.Success).rankings
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

@Composable
private fun RankingFilterBar(
    selectedCategory: String,
    onCategorySelect: (String) -> Unit
) {
    val categories = listOf("Full", "CPU", "GPU", "RAM", "Storage", "Productivity", "AI")

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelect(category) },
                label = { Text(category) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
private fun CpuRankingList(
    rankings: List<RankingItem>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(rankings) { item ->
            RankingItemCard(item)
        }
    }
}

@Composable
private fun RankingItemCard(
    item: RankingItem
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
        else -> MaterialTheme.colorScheme.onSurface
    }

    val containerColor = if (item.isCurrentUser) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val borderModifier = if (item.isCurrentUser) {
        Modifier.border(
            width = 2.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
            shape = RoundedCornerShape(12.dp)
        )
    } else {
        Modifier
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(borderModifier),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Header row: Rank, Name, Score
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Rank
                Box(
                    modifier = Modifier
                        .width(50.dp)
                        .height(40.dp)
                        .background(
                            color = rankColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#${item.rank}",
                        fontWeight = FontWeight.Bold,
                        color = rankColor,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Center: Name and Scores
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Single: ${item.singleCore} | Multi: ${item.multiCore}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.paddingFromBaseline(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Right: Normalized Score
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = item.normalizedScore.toString(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Score",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = scoreProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = GruvboxDarkAccent,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun ComingSoonContent(
    category: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.AccessTime,
            contentDescription = "Coming Soon",
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Coming Soon",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "$category rankings will be available soon.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Loading rankings...",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
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
        Text(
            text = "Error Loading Rankings",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Unable to fetch ranking data. Please try again later.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}
