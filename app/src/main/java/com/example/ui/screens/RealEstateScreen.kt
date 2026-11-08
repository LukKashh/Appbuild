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
import androidx.compose.ui.window.Dialog
import com.example.model.GameState
import com.example.model.Property
import com.example.model.PropertyCategory
import com.example.ui.components.CurrencyFormatter
import com.example.ui.components.TycoonCard
import com.example.ui.theme.*

@Composable
fun RealEstateScreen(
    state: GameState,
    onBuyProperty: (String) -> Unit,
    onRenovateProperty: (String) -> Unit,
    onRenovateAll: () -> Unit,
    onBuyUpgrade: (String, String) -> Unit,
    onCollectRent: (String) -> Unit,
    onCollectAllRent: () -> Unit,
    onSellProperty: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf<PropertyCategory?>(null) }
    var detailProperty by remember { mutableStateOf<Property?>(null) }

    val filteredProperties = if (selectedCategory == null) {
        state.properties
    } else {
        state.properties.filter { it.category == selectedCategory }
    }

    val totalRent = state.properties.filter { it.isOwned }.sumOf { it.rentPerSec }
    val totalUncollected = state.availableRentWallet
    val damagedProperties = state.properties.filter { it.isOwned && it.conditionPercent < 100 }
    val totalRenovationCost = damagedProperties.sumOf { it.renovationCost }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Summary Banner
            item {
                TycoonCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = CyanTech.copy(alpha = 0.4f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "REAL ESTATE PORTFOLIO",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "+${CurrencyFormatter.format(totalRent)}/s",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = CyanTech
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Uncollected Rent: ${CurrencyFormatter.format(totalUncollected)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = GoldLight
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (damagedProperties.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = onRenovateAll,
                                    enabled = state.cash >= totalRenovationCost,
                                    border = BorderStroke(1.dp, CyanTech),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanTech),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "FIX ALL",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }

                            Button(
                                onClick = onCollectAllRent,
                                enabled = totalUncollected > 0,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyanTech,
                                    contentColor = Color.Black,
                                    disabledContainerColor = DarkSurfaceVariant,
                                    disabledContentColor = Color.Gray
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("collect_all_rent_button")
                            ) {
                                Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "COLLECT",
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }

            // Category Tabs
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
                        label = { Text("All Properties") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldPrimary,
                            selectedLabelColor = Color.Black
                        )
                    )
                    PropertyCategory.values().forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.title) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldPrimary,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
            }

            // Property Cards
            items(filteredProperties, key = { it.id }) { prop ->
                PropertyCard(
                    property = prop,
                    playerCash = state.cash,
                    onBuy = { onBuyProperty(prop.id) },
                    onRenovate = { onRenovateProperty(prop.id) },
                    onCollect = { onCollectRent(prop.id) },
                    onOpenDetail = { detailProperty = prop }
                )
            }
        }

        // Property Detail Dialog
        detailProperty?.let { activeProp ->
            val liveProp = state.properties.find { it.id == activeProp.id } ?: activeProp
            PropertyDetailDialog(
                property = liveProp,
                playerCash = state.cash,
                onDismiss = { detailProperty = null },
                onRenovate = { onRenovateProperty(liveProp.id) },
                onBuyUpgrade = { upgId -> onBuyUpgrade(liveProp.id, upgId) },
                onCollectRent = { onCollectRent(liveProp.id) },
                onSell = {
                    onSellProperty(liveProp.id)
                    detailProperty = null
                }
            )
        }
    }
}

@Composable
fun PropertyCard(
    property: Property,
    playerCash: Double,
    onBuy: () -> Unit,
    onRenovate: () -> Unit,
    onCollect: () -> Unit,
    onOpenDetail: () -> Unit
) {
    TycoonCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (property.isOwned) CyanTech.copy(alpha = 0.35f) else DarkCardBorder
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = property.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Surface(
                    color = DarkSurfaceVariant,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = property.category.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (property.isOwned) {
                val conditionColor = when {
                    property.conditionPercent >= 75 -> EmeraldProfit
                    property.conditionPercent >= 40 -> ChipGold
                    else -> CrimsonLoss
                }
                Surface(
                    color = conditionColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, conditionColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "${property.conditionPercent}% Condition",
                        style = MaterialTheme.typography.labelSmall,
                        color = conditionColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (!property.isOwned) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Base Rent: +${CurrencyFormatter.format(property.baseRentPerSec)}/s",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyanTech
                    )
                    Text(
                        text = "Deed Price: ${CurrencyFormatter.format(property.purchasePrice)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = GoldLight,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onBuy,
                    enabled = playerCash >= property.purchasePrice,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanTech,
                        contentColor = Color.Black,
                        disabledContainerColor = DarkSurfaceVariant,
                        disabledContentColor = Color.Gray
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("buy_property_${property.id}")
                ) {
                    Text("BUY DEED", fontWeight = FontWeight.ExtraBold)
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Rent: +${CurrencyFormatter.format(property.rentPerSec)}/s",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CyanTech
                    )
                    Text(
                        text = "Accumulated: ${CurrencyFormatter.format(property.accumulatedRent)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = GoldLight
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onCollect,
                        enabled = property.accumulatedRent > 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanTech,
                            contentColor = Color.Black,
                            disabledContainerColor = DarkSurfaceVariant,
                            disabledContentColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("collect_prop_${property.id}")
                    ) {
                        Text("COLLECT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onOpenDetail,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, CyanTech),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanTech),
                        modifier = Modifier.testTag("manage_prop_${property.id}")
                    ) {
                        Text("MANAGE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun PropertyDetailDialog(
    property: Property,
    playerCash: Double,
    onDismiss: () -> Unit,
    onRenovate: () -> Unit,
    onBuyUpgrade: (String) -> Unit,
    onCollectRent: () -> Unit,
    onSell: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = DarkSurface,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, CyanTech.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = property.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "${property.category.title} Property",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Divider(color = DarkCardBorder, modifier = Modifier.padding(vertical = 12.dp))

                // Stats
                TycoonCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Current Valuation:", style = MaterialTheme.typography.bodySmall, color = Color.White)
                        Text(CurrencyFormatter.format(property.currentValuation), style = MaterialTheme.typography.bodySmall, color = GoldLight)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Rental Yield:", style = MaterialTheme.typography.bodySmall, color = Color.White)
                        Text("+${CurrencyFormatter.format(property.rentPerSec)}/s", style = MaterialTheme.typography.bodySmall, color = CyanTech)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Condition Health: ${property.conditionPercent}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (property.conditionPercent > 70) EmeraldProfit else ChipGold
                    )
                    LinearProgressIndicator(
                        progress = { property.conditionPercent / 100f },
                        color = if (property.conditionPercent > 70) EmeraldProfit else ChipGold,
                        trackColor = DarkCardBorder,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Occupancy", style = MaterialTheme.typography.bodySmall, color = Color.White)
                        Text((property.occupancyRate * 100).toInt().toString() + "%", color = CyanTech, style = MaterialTheme.typography.bodySmall)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Demand index", style = MaterialTheme.typography.bodySmall, color = Color.White)
                        Text(String.format("%.2fx", property.demandMultiplier), color = CyanTech, style = MaterialTheme.typography.bodySmall)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Operating expense", style = MaterialTheme.typography.bodySmall, color = Color.White)
                        Text(CurrencyFormatter.format(property.operatingExpensePerSec) + "/s", color = CrimsonLight, style = MaterialTheme.typography.bodySmall)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Maintenance arrears", style = MaterialTheme.typography.bodySmall, color = Color.White)
                        Text(CurrencyFormatter.format(property.maintenanceDebt), color = CrimsonLight, style = MaterialTheme.typography.bodySmall)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Renovate button
                Button(
                    onClick = onRenovate,
                    enabled = property.conditionPercent < 100 && playerCash >= property.renovationCost,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanTech, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (property.conditionPercent >= 100) "PRISTINE CONDITION (100%)"
                        else "RENOVATE TO 100% (${CurrencyFormatter.format(property.renovationCost)})",
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Upgrades
                Text(
                    text = "AMENITIES & UPGRADES",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                property.upgrades.forEach { upg ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(upg.name, style = MaterialTheme.typography.bodySmall, color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text("+${((upg.rentBoost - 1.0) * 100).toInt()}% Rent Boost", style = MaterialTheme.typography.labelSmall, color = CyanTech)
                        }
                        if (upg.level >= upg.maxLevel) {
                            Text("MAX LEVEL", style = MaterialTheme.typography.labelSmall, color = EmeraldProfit, fontWeight = FontWeight.Bold)
                        } else {
                            Button(
                                onClick = { onBuyUpgrade(upg.id) },
                                enabled = playerCash >= upg.currentCost,
                                colors = ButtonDefaults.buttonColors(containerColor = CyanTech, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("LVL " + upg.level + " → " + (upg.level + 1) + " • " + CurrencyFormatter.format(upg.currentCost), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sell property button
                OutlinedButton(
                    onClick = onSell,
                    border = BorderStroke(1.dp, CrimsonLoss),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("SELL PROPERTY • 85% ESTIMATED VALUATION (${CurrencyFormatter.format((property.currentValuation - property.maintenanceDebt) * 0.85)})")
                }
            }
        }
    }
}
