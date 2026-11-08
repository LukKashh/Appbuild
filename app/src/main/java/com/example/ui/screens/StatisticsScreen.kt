package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GameState
import com.example.model.Transaction
import com.example.model.TransactionType
import com.example.ui.components.CurrencyFormatter
import com.example.ui.components.TycoonCard
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StatisticsScreen(
    state: GameState,
    onResetGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showResetConfirm by remember { mutableStateOf(false) }

    val bizVal = state.businesses.filter { it.isOwned }.sumOf { it.valuation }
    val propVal = state.properties.filter { it.isOwned }.sumOf { it.currentValuation }
    val stockVal = state.stocks.sumOf { it.holdingsValue }
    val cryptoVal = state.cryptos.sumOf { it.holdingsValue }
    val colVal = state.collectibles.filter { it.isOwned }.sumOf { it.currentValuation }
    val chipVal = state.casinoChips * 100.0

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Net Worth Chart
        item {
            TycoonCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = GoldPrimary.copy(alpha = 0.5f)
            ) {
                Text(
                    text = "HISTORICAL NET WORTH TRAJECTORY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = CurrencyFormatter.format(state.totalNetWorth),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = GoldLight
                )

                Spacer(modifier = Modifier.height(10.dp))

                NetWorthChart(
                    history = state.netWorthHistory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                )
            }
        }

        // Asset Portfolio Allocation
        item {
            TycoonCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "ASSET ALLOCATION BREAKDOWN",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                AssetBreakdownRow("Available Liquid Cash", state.cash, EmeraldLight)
                AssetBreakdownRow("Enterprise Valuations", bizVal, GoldLight)
                AssetBreakdownRow("Real Estate Portfolios", propVal, CyanTech)
                AssetBreakdownRow("Stock Exchange Equities", stockVal, GoldPrimary)
                AssetBreakdownRow("Crypto Digital Assets", cryptoVal, PurpleCrypto)
                AssetBreakdownRow("Luxury & Museum Vault", colVal, RarityMythical)
                AssetBreakdownRow("Casino Chips Balance", chipVal, ChipGold)
            }
        }

        // Cash Flow Statement
        item {
            TycoonCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "CASH FLOW STATEMENT (PER SECOND)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Gross Inflow:", style = MaterialTheme.typography.bodySmall, color = Color.White)
                    Text("+${CurrencyFormatter.format(state.totalIncomePerSec)}/s", style = MaterialTheme.typography.bodySmall, color = EmeraldProfit, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Operating Expenses:", style = MaterialTheme.typography.bodySmall, color = Color.White)
                    Text("-${CurrencyFormatter.format(state.totalExpensesPerSec)}/s", style = MaterialTheme.typography.bodySmall, color = CrimsonLight, fontWeight = FontWeight.Bold)
                }
                Divider(color = DarkCardBorder, modifier = Modifier.padding(vertical = 4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Net Operating Cash Flow:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("+${CurrencyFormatter.format(state.netCashFlowPerSec)}/s", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = EmeraldLight)
                }
            }
        }

        // Transaction Audit Trail
        item {
            Text(
                text = "RECENT TRANSACTION AUDIT LOG",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 1.sp
            )
        }

        items(state.transactions.take(15), key = { it.id }) { tx ->
            TransactionRow(tx = tx)
        }

        // Reset game option
        item {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = { showResetConfirm = true },
                border = BorderStroke(1.dp, CrimsonLoss),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonLight),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reset_game_button")
            ) {
                Text("RESET GAME PROGRESS & DATA")
            }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset Empire Data?", color = Color.White) },
            text = { Text("Are you sure you want to erase all game progress? This action will restore your seed capital of $50.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                Button(
                    onClick = {
                        onResetGame()
                        showResetConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonLoss)
                ) {
                    Text("RESET ALL")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetConfirm = false }) {
                    Text("CANCEL")
                }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
fun AssetBreakdownRow(label: String, value: Double, dotColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(label, style = MaterialTheme.typography.bodySmall, color = Color.White)
        }
        Text(CurrencyFormatter.format(value), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = dotColor)
    }
}

@Composable
fun TransactionRow(tx: Transaction) {
    val isCredit = tx.amount >= 0
    val timeStr = remember(tx.timestamp) {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(tx.timestamp))
    }

    TycoonCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.description,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = "$timeStr • ${tx.type.name}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = (if (isCredit) "+$" else "-$") + CurrencyFormatter.format(Math.abs(tx.amount)).removePrefix("$"),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (isCredit) EmeraldProfit else CrimsonLight
            )
        }
    }
}

@Composable
fun NetWorthChart(
    history: List<Double>,
    modifier: Modifier = Modifier
) {
    if (history.size < 2) return

    Canvas(modifier = modifier) {
        val min = history.minOrNull() ?: 0.0
        val max = history.maxOrNull() ?: 1.0
        val range = if (max - min == 0.0) 1.0 else max - min

        val stepX = size.width / (history.size - 1)
        val strokePath = Path()
        val fillPath = Path()

        history.forEachIndexed { i, p ->
            val x = i * stepX
            val y = size.height - (((p - min) / range) * size.height * 0.85f).toFloat() - 4.dp.toPx()

            if (i == 0) {
                strokePath.moveTo(x, y)
                fillPath.moveTo(x, size.height)
                fillPath.lineTo(x, y)
            } else {
                strokePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }

        fillPath.lineTo(size.width, size.height)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(GoldPrimary.copy(alpha = 0.30f), Color.Transparent)
            )
        )

        drawPath(
            path = strokePath,
            color = GoldPrimary,
            style = Stroke(width = 3.dp.toPx())
        )
    }
}
