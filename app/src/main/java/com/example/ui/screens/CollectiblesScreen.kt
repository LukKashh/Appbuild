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
import com.example.model.Collectible
import com.example.model.GameState
import com.example.model.Rarity
import com.example.ui.components.CurrencyFormatter
import com.example.ui.components.RarityBadge
import com.example.ui.components.TycoonCard
import com.example.ui.theme.*

@Composable
fun CollectiblesScreen(
    state: GameState,
    onBuyCollectible: (String) -> Unit,
    onSellCollectible: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedRarity by remember { mutableStateOf<Rarity?>(null) }

    val categories = remember { state.collectibles.map { it.category }.distinct() }

    val filteredItems = state.collectibles.filter { item ->
        (selectedCategory == null || item.category == selectedCategory) &&
        (selectedRarity == null || item.rarity == selectedRarity)
    }

    val ownedCount = state.collectibles.count { it.isOwned }
    val totalValuation = state.collectibles.filter { it.isOwned }.sumOf { it.currentValuation }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Vault Prestige Banner
        item {
            TycoonCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = GoldPrimary.copy(alpha = 0.45f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "LUXURY & PRESTIGE VAULT",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = CurrencyFormatter.format(totalValuation),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = GoldLight
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$ownedCount of ${state.collectibles.size} Acquired",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        color = GoldContainer,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(imageVector = Icons.Default.Diamond, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(24.dp))
                            Text(
                                text = "MUSEUM",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = GoldLight
                            )
                        }
                    }
                }
            }
        }

        // Category Filter Tabs
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
                    label = { Text("All Categories") },
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

        // Collectibles Cards
        items(filteredItems, key = { it.id }) { item ->
            CollectibleCard(
                item = item,
                playerCash = state.cash,
                onBuy = { onBuyCollectible(item.id) },
                onSell = { onSellCollectible(item.id) }
            )
        }
    }
}

@Composable
fun CollectibleCard(
    item: Collectible,
    playerCash: Double,
    onBuy: () -> Unit,
    onSell: () -> Unit
) {
    val rarityBorder = when (item.rarity) {
        Rarity.COMMON -> RarityCommon.copy(alpha = 0.3f)
        Rarity.UNCOMMON -> RarityUncommon.copy(alpha = 0.4f)
        Rarity.RARE -> RarityRare.copy(alpha = 0.5f)
        Rarity.EPIC -> RarityEpic.copy(alpha = 0.6f)
        Rarity.LEGENDARY -> RarityLegendary.copy(alpha = 0.7f)
        Rarity.MYTHICAL -> RarityMythical.copy(alpha = 0.8f)
    }

    TycoonCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (item.isOwned) rarityBorder else DarkCardBorder
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
                    RarityBadge(rarity = item.rarity)
                    Text(
                        text = item.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                    color = DarkSurfaceVariant,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Bonus: ${item.bonusDescription}",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldLight,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (item.isOwned) {
                Surface(
                    color = EmeraldContainer,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, EmeraldProfit.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "IN VAULT",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldLight,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (item.isOwned) "Current Valuation" else "Acquisition Price",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = CurrencyFormatter.format(if (item.isOwned) item.currentValuation else item.purchasePrice),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = GoldLight
                )
            }

            if (!item.isOwned) {
                Button(
                    onClick = onBuy,
                    enabled = playerCash >= item.purchasePrice,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldPrimary,
                        contentColor = Color.Black,
                        disabledContainerColor = DarkSurfaceVariant,
                        disabledContentColor = Color.Gray
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("buy_collectible_${item.id}")
                ) {
                    Text("PURCHASE", fontWeight = FontWeight.ExtraBold)
                }
            } else {
                val remainingMs = (item.holdUntil - System.currentTimeMillis()).coerceAtLeast(0L)
                Column(horizontalAlignment = Alignment.End) {
                    if (remainingMs > 0L) {
                        Text("LOCKED • " + (remainingMs / 3600000L) + "h " + ((remainingMs / 60000L) % 60) + "m", color = Color(0xFFBDA8D2), style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = onSell,
                        enabled = remainingMs <= 0L,
                        border = BorderStroke(1.dp, CrimsonLoss),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonLight),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("SELL (" + (item.resalePercent * 100).toInt() + "%)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
