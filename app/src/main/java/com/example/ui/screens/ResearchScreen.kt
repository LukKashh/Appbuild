package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GameState
import com.example.model.ResearchNode
import com.example.ui.components.CurrencyFormatter
import com.example.ui.components.TycoonCard
import com.example.ui.theme.*

@Composable
fun ResearchScreen(
    state: GameState,
    onUnlockResearch: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val unlockedCount = state.research.count { it.isUnlocked }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // R&D Lab Header
        item {
            TycoonCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = CyanTech.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "R&D LABORATORY & PATENTS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "$unlockedCount / ${state.research.size} Discovered",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = CyanTech
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Permanent corporate multipliers",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.Science,
                        contentDescription = null,
                        tint = CyanTech,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        // Research nodes
        items(state.research, key = { it.id }) { node ->
            val prereq = node.requiredResearchId?.let { id -> state.research.find { it.id == id } }
            val prereqMet = prereq == null || prereq.isUnlocked

            ResearchCard(
                node = node,
                prereqName = prereq?.name,
                prereqMet = prereqMet,
                playerCash = state.cash,
                onUnlock = { onUnlockResearch(node.id) }
            )
        }
    }
}

@Composable
fun ResearchCard(
    node: ResearchNode,
    prereqName: String?,
    prereqMet: Boolean,
    playerCash: Double,
    onUnlock: () -> Unit
) {
    val canAfford = playerCash >= node.cost && prereqMet

    TycoonCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = when {
            node.isUnlocked -> EmeraldProfit.copy(alpha = 0.5f)
            canAfford -> CyanTech.copy(alpha = 0.5f)
            else -> DarkCardBorder
        }
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
                        text = node.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Surface(
                        color = DarkSurfaceVariant,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = node.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanTech,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = node.effectDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!prereqMet && prereqName != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Requires: $prereqName",
                        style = MaterialTheme.typography.labelSmall,
                        color = CrimsonLight,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (node.isUnlocked) {
                Surface(
                    color = EmeraldContainer,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, EmeraldProfit.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "RESEARCHED",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldLight,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        if (!node.isUnlocked) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Grant Cost: ${CurrencyFormatter.format(node.cost)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = GoldLight
                )

                Button(
                    onClick = onUnlock,
                    enabled = canAfford,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanTech,
                        contentColor = Color.Black,
                        disabledContainerColor = DarkSurfaceVariant,
                        disabledContentColor = Color.Gray
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("unlock_research_${node.id}")
                ) {
                    Text("RESEARCH", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
