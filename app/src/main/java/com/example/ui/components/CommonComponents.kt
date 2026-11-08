package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GameState
import com.example.model.Rarity
import com.example.ui.theme.*
import java.text.DecimalFormat

object CurrencyFormatter {
    private val df = DecimalFormat("#,##0.00")
    private val dfShort = DecimalFormat("#,##0")

    fun format(amount: Double): String {
        val abs = Math.abs(amount)
        val sign = if (amount < 0) "-" else ""
        return when {
            abs >= 1_000_000_000_000.0 -> sign + "$" + String.format("%.2fT", abs / 1_000_000_000_000.0)
            abs >= 1_000_000_000.0 -> sign + "$" + String.format("%.2fB", abs / 1_000_000_000.0)
            abs >= 1_000_000.0 -> sign + "$" + String.format("%.2fM", abs / 1_000_000.0)
            abs >= 1_000.0 -> sign + "$" + String.format("%.2fK", abs / 1_000.0)
            else -> sign + "$" + df.format(abs)
        }
    }

    fun formatChips(chips: Long): String {
        return when {
            chips >= 1_000_000_000L -> String.format("%.2fB", chips / 1_000_000_000.0)
            chips >= 1_000_000L -> String.format("%.2fM", chips / 1_000_000.0)
            chips >= 1_000L -> String.format("%.1fK", chips / 1_000.0)
            else -> chips.toString()
        }
    }
}

@Composable
fun WealthHeaderBar(
    state: GameState,
    onOpenExchange: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = DarkSurface,
        tonalElevation = 6.dp,
        border = BorderStroke(1.dp, DarkCardBorder),
        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Net worth row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TOTAL NET WORTH",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = CurrencyFormatter.format(state.totalNetWorth),
                        style = MaterialTheme.typography.titleLarge,
                        color = GoldPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.testTag("total_net_worth_text")
                    )
                }

                // Casino chips pill
                Surface(
                    color = DarkSurfaceVariant,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, ChipGold.copy(alpha = 0.5f)),
                    onClick = onOpenExchange,
                    modifier = Modifier.testTag("casino_chips_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Casino,
                            contentDescription = "Casino Chips",
                            tint = ChipGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = CurrencyFormatter.formatChips(state.casinoChips) + " 🪙",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = ChipGold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Cash and Net Flow indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = "Cash Balance",
                        tint = EmeraldLight,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Cash: ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.format(state.cash),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("available_cash_text")
                    )
                }

                // Flow / sec
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = "Cash Flow",
                        tint = EmeraldProfit,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "+${CurrencyFormatter.format(state.netCashFlowPerSec)}/s",
                        style = MaterialTheme.typography.labelLarge,
                        color = EmeraldProfit,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("cash_flow_rate_text")
                    )
                }
            }
        }
    }
}

@Composable
fun RarityBadge(rarity: Rarity, modifier: Modifier = Modifier) {
    val (bgColor, textColor) = when (rarity) {
        Rarity.COMMON -> Pair(RarityCommon.copy(alpha = 0.2f), RarityCommon)
        Rarity.UNCOMMON -> Pair(RarityUncommon.copy(alpha = 0.2f), RarityUncommon)
        Rarity.RARE -> Pair(RarityRare.copy(alpha = 0.2f), RarityRare)
        Rarity.EPIC -> Pair(RarityEpic.copy(alpha = 0.2f), RarityEpic)
        Rarity.LEGENDARY -> Pair(RarityLegendary.copy(alpha = 0.25f), RarityLegendary)
        Rarity.MYTHICAL -> Pair(RarityMythical.copy(alpha = 0.25f), RarityMythical)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, textColor.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Text(
            text = rarity.title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun TycoonCard(
    modifier: Modifier = Modifier,
    borderColor: Color = DarkCardBorder,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (onClick != null) {
        Card(
            onClick = onClick,
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, borderColor),
            shape = RoundedCornerShape(16.dp),
            modifier = modifier
        ) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    } else {
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, borderColor),
            shape = RoundedCornerShape(16.dp),
            modifier = modifier
        ) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    }
}
