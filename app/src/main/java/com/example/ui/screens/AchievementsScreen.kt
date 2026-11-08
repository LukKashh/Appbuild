package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Achievement
import com.example.model.GameState
import com.example.ui.components.CurrencyFormatter
import com.example.ui.components.TycoonCard
import com.example.ui.theme.*

@Composable
fun AchievementsScreen(
    state: GameState,
    onClaimReward: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    val categories = remember { state.achievements.map { it.category }.distinct() }

    val filteredAchievements = if (selectedCategory == null) {
        state.achievements
    } else {
        state.achievements.filter { it.category == selectedCategory }
    }

    val unlockedCount = state.achievements.count { it.isUnlocked }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Milestone Banner
        item {
            TycoonCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = GoldPrimary.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "MILESTONES & HONORS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "$unlockedCount / ${state.achievements.size} Unlocked",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = GoldLight
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Complete achievements to claim cash bounties",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }
        }

        // Category Filter
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text("All Milestones") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GoldPrimary,
                        selectedLabelColor = Color.Black
                    )
                )
                categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldPrimary,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }
        }

        // Achievements List
        items(filteredAchievements, key = { it.id }) { ach ->
            AchievementCard(
                achievement = ach,
                onClaim = { onClaimReward(ach.id) }
            )
        }
    }
}

@Composable
fun AchievementCard(
    achievement: Achievement,
    onClaim: () -> Unit
) {
    val progressRatio = (achievement.currentProgress / achievement.targetProgress).coerceIn(0.0, 1.0).toFloat()

    TycoonCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (achievement.isUnlocked) GoldPrimary.copy(alpha = 0.5f) else DarkCardBorder
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = achievement.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Surface(
                        color = DarkSurfaceVariant,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = achievement.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldLight,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = achievement.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (achievement.isUnlocked) {
                if (achievement.isClaimed) {
                    Surface(
                        color = DarkSurfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "CLAIMED",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    Button(
                        onClick = onClaim,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("claim_achievement_${achievement.id}")
                    ) {
                        Text("CLAIM +${CurrencyFormatter.format(achievement.rewardCash)}", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Progress bar
        LinearProgressIndicator(
            progress = { progressRatio },
            color = if (achievement.isUnlocked) EmeraldProfit else GoldPrimary,
            trackColor = DarkCardBorder,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${(progressRatio * 100).toInt()}% Complete",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Reward: ${CurrencyFormatter.format(achievement.rewardCash)}",
                style = MaterialTheme.typography.labelSmall,
                color = GoldLight,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
