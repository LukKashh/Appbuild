package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import com.example.model.Business
import com.example.model.GameState
import com.example.model.Industry
import com.example.ui.components.CurrencyFormatter
import com.example.ui.components.TycoonCard
import com.example.ui.theme.*

@Composable
fun BusinessScreen(
    state: GameState,
    onBuyBusiness: (String) -> Unit,
    onExpandBranch: (String) -> Unit,
    onHireEmployee: (String) -> Unit,
    onFireEmployee: (String) -> Unit,
    onBuyUpgrade: (String, String) -> Unit,
    onCollectWallet: (String) -> Unit,
    onCollectAll: () -> Unit,
    onSellBusiness: (String) -> Unit,
    onToggleSuspension: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedIndustry by remember { mutableStateOf<Industry?>(null) }
    var detailBusiness by remember { mutableStateOf<Business?>(null) }

    val filteredBusinesses = if (selectedIndustry == null) {
        state.businesses
    } else {
        state.businesses.filter { it.industry == selectedIndustry }
    }

    val totalWallet = state.availableBusinessWallet
    val totalIncome = state.businesses.filter { it.isOwned }.sumOf { it.netProfitPerSec }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Summary Empire Banner
            item {
                TycoonCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = EmeraldProfit.copy(alpha = 0.4f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "ENTERPRISE EMPIRE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "+${CurrencyFormatter.format(totalIncome)}/s",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldLight
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Uncollected: ${CurrencyFormatter.format(totalWallet)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = GoldLight
                            )
                        }

                        Button(
                            onClick = onCollectAll,
                            enabled = totalWallet > 0,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldProfit,
                                contentColor = Color.Black,
                                disabledContainerColor = DarkSurfaceVariant,
                                disabledContentColor = Color.Gray
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("collect_all_businesses_button")
                        ) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "COLLECT ALL",
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            // Industry Filter Tabs
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedIndustry == null,
                        onClick = { selectedIndustry = null },
                        label = { Text("All Industries") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldPrimary,
                            selectedLabelColor = Color.Black
                        )
                    )
                    Industry.values().forEach { ind ->
                        FilterChip(
                            selected = selectedIndustry == ind,
                            onClick = { selectedIndustry = ind },
                            label = { Text(ind.title) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldPrimary,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
            }

            // Business Cards
            items(filteredBusinesses, key = { it.id }) { biz ->
                BusinessCard(
                    business = biz,
                    playerCash = state.cash,
                    onBuy = { onBuyBusiness(biz.id) },
                    onExpand = { onExpandBranch(biz.id) },
                    onCollect = { onCollectWallet(biz.id) },
                    onOpenDetail = { detailBusiness = biz }
                )
            }
        }

        // Business Detail Dialog
        detailBusiness?.let { activeBiz ->
            val liveBiz = state.businesses.find { it.id == activeBiz.id } ?: activeBiz
            BusinessDetailDialog(
                business = liveBiz,
                playerCash = state.cash,
                onDismiss = { detailBusiness = null },
                onExpandBranch = { onExpandBranch(liveBiz.id) },
                onHireEmployee = { onHireEmployee(liveBiz.id) },
                onFireEmployee = { onFireEmployee(liveBiz.id) },
                onBuyUpgrade = { upgId -> onBuyUpgrade(liveBiz.id, upgId) },
                onCollectWallet = { onCollectWallet(liveBiz.id) },
                onToggleSuspension = { onToggleSuspension(liveBiz.id) },
                onSell = {
                    onSellBusiness(liveBiz.id)
                    detailBusiness = null
                }
            )
        }
    }
}

@Composable
fun BusinessCard(
    business: Business,
    playerCash: Double,
    onBuy: () -> Unit,
    onExpand: () -> Unit,
    onCollect: () -> Unit,
    onOpenDetail: () -> Unit
) {
    TycoonCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (business.isOwned) GoldPrimary.copy(alpha = 0.35f) else DarkCardBorder
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ArchiveArtwork(
                searchTerms = listOf(business.id, business.name, business.industry.title),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = business.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Surface(
                    color = DarkSurfaceVariant,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = business.industry.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (business.isOwned) {
                Surface(
                    color = GoldContainer,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "${business.currentBranches} Branches",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldLight,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (!business.isOwned) {
            // Acquisition section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Base Revenue: +${CurrencyFormatter.format(business.baseRevenuePerSec)}/s",
                        style = MaterialTheme.typography.bodySmall,
                        color = EmeraldLight
                    )
                    Text(
                        text = "Cost: ${CurrencyFormatter.format(business.baseCost)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = GoldLight,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onBuy,
                    enabled = playerCash >= business.baseCost,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldPrimary,
                        contentColor = Color.Black,
                        disabledContainerColor = DarkSurfaceVariant,
                        disabledContentColor = Color.Gray
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("buy_business_${business.id}")
                ) {
                    Text("ACQUIRE", fontWeight = FontWeight.ExtraBold)
                }
            }
        } else {
            // Owned operations section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Net: +${CurrencyFormatter.format(business.netProfitPerSec)}/s",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldProfit
                    )
                    Text(
                        text = "Vault: ${CurrencyFormatter.format(business.accumulatedWallet)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = GoldLight
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onCollect,
                        enabled = business.accumulatedWallet > 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldProfit,
                            contentColor = Color.Black,
                            disabledContainerColor = DarkSurfaceVariant,
                            disabledContentColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("collect_biz_${business.id}")
                    ) {
                        Text("COLLECT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onOpenDetail,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, GoldPrimary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldLight),
                        modifier = Modifier.testTag("manage_biz_${business.id}")
                    ) {
                        Text("MANAGE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun BusinessDetailDialog(
    business: Business,
    playerCash: Double,
    onDismiss: () -> Unit,
    onExpandBranch: () -> Unit,
    onHireEmployee: () -> Unit,
    onFireEmployee: () -> Unit,
    onBuyUpgrade: (String) -> Unit,
    onCollectWallet: () -> Unit,
    onToggleSuspension: () -> Unit = {},
    onSell: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = DarkSurface,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = business.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "${business.industry.title} | ${business.currentBranches} Branches",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Divider(color = DarkCardBorder, modifier = Modifier.padding(vertical = 12.dp))

                // Financial Breakdown
                TycoonCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "FINANCIAL PERFORMANCE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Gross Revenue:", style = MaterialTheme.typography.bodySmall, color = Color.White)
                        Text("+${CurrencyFormatter.format(business.grossRevenuePerSec)}/s", style = MaterialTheme.typography.bodySmall, color = EmeraldLight)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Salaries & Maintenance:", style = MaterialTheme.typography.bodySmall, color = Color.White)
                        Text("-${CurrencyFormatter.format(business.expensesPerSec)}/s", style = MaterialTheme.typography.bodySmall, color = CrimsonLight)
                    }
                    Divider(color = DarkCardBorder, modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Net Cash Flow:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("+${CurrencyFormatter.format(business.netProfitPerSec)}/s", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = EmeraldProfit)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Asset Valuation:", style = MaterialTheme.typography.bodySmall, color = Color.White)
                        Text(CurrencyFormatter.format(business.valuation), style = MaterialTheme.typography.bodySmall, color = GoldLight)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Operating reserves:", style = MaterialTheme.typography.bodySmall, color = Color.White)
                        Text(CurrencyFormatter.format(business.reserves), style = MaterialTheme.typography.bodySmall, color = GoldLight)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Liabilities:", style = MaterialTheme.typography.bodySmall, color = Color.White)
                        Text(CurrencyFormatter.format(business.liabilities), style = MaterialTheme.typography.bodySmall, color = CrimsonLight)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Operating status:", style = MaterialTheme.typography.bodySmall, color = Color.White)
                        Text(if (business.isSuspended) "SUSPENDED" else if (business.netProfitPerSec < 0) "LOSS-MAKING" else "ACTIVE", style = MaterialTheme.typography.bodySmall, color = if (business.isSuspended || business.netProfitPerSec < 0) CrimsonLight else EmeraldLight)
                    }
                    val reactivationCost = maxOf(500.0, (business.liabilities - business.baseCost * 0.20).coerceAtLeast(0.0) + business.baseCost * 0.05)
                    Button(
                        onClick = onToggleSuspension,
                        enabled = !business.isSuspended || playerCash >= reactivationCost,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = if (business.isSuspended) EmeraldProfit else DarkSurfaceVariant, contentColor = if (business.isSuspended) Color.Black else Color.White)
                    ) {
                        Text(if (business.isSuspended) "REACTIVATE OPERATIONS • " + CurrencyFormatter.format(reactivationCost) else "SUSPEND OPERATIONS")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Employees Management
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Employees: ${business.employeeCount}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Salary: ${CurrencyFormatter.format(business.salaryPerEmployee)}/s each (+10% Boost)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilledTonalButton(
                            onClick = onFireEmployee,
                            enabled = business.employeeCount > 0,
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = CrimsonContainer, contentColor = CrimsonLight)
                        ) {
                            Text("-")
                        }
                        Button(
                            onClick = onHireEmployee,
                            enabled = playerCash >= business.employeeHireCost && business.employeeCount < business.employeeCapacity,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black)
                        ) {
                            Text("+ (${CurrencyFormatter.format(business.employeeHireCost)})")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Branch Expansion
                Button(
                    onClick = onExpandBranch,
                    enabled = playerCash >= business.branchCost && business.currentBranches < business.branchLimit,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "ADD BRANCH (${CurrencyFormatter.format(business.branchCost)})",
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Upgrades
                Text(
                    text = "ENTERPRISE UPGRADES",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                business.upgrades.forEach { upg ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(upg.name, style = MaterialTheme.typography.bodySmall, color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text("${upg.multiplier}x Multiplier", style = MaterialTheme.typography.labelSmall, color = EmeraldLight)
                        }
                        if (upg.level >= upg.maxLevel) {
                            Text("MAX LEVEL", style = MaterialTheme.typography.labelSmall, color = EmeraldProfit, fontWeight = FontWeight.Bold)
                        } else {
                            Button(
                                onClick = { onBuyUpgrade(upg.id) },
                                enabled = playerCash >= upg.currentCost,
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("LVL " + upg.level + " → " + (upg.level + 1) + " • " + CurrencyFormatter.format(upg.currentCost), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sell enterprise button
                OutlinedButton(
                    onClick = onSell,
                    border = BorderStroke(1.dp, CrimsonLoss),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonLight),
                    enabled = System.currentTimeMillis() >= business.acquiredAt + 6L * 60L * 60L * 1000L,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("SELL ENTERPRISE FOR 80% NET VALUATION (${CurrencyFormatter.format(business.valuation * 0.80 + business.accumulatedWallet + business.reserves - business.liabilities)})")
                }
            }
        }
    }
}
