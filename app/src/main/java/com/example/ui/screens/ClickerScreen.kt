package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.LocalSimulationEngine
import com.example.model.ClickUpgrade
import com.example.model.GameState
import com.example.ui.components.CurrencyFormatter
import com.example.ui.components.TycoonCard
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ClickerScreen(
    state: GameState,
    newsTicker: String = "",
    onTap: () -> Unit,
    onBuyUpgrade: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "coin_scale"
    )
    val collectibleClickBonus = state.collectibles.filter { it.isOwned && it.bonusKey == "clickPct" }.sumOf { it.bonusValue }
    val corporationClickBonus = (state.corporation.facilities["training"] ?: 0) * 0.01
    val earnings = state.clickPower * state.clickMultiplier *
        (if (state.research.any { it.id == "res_2" && it.isUnlocked }) 2.0 else 1.0) *
        (if (state.collectibles.any { it.id == "col_1" && it.isOwned }) 1.05 else 1.0) *
        (1.0 + collectibleClickBonus + corporationClickBonus)
    val autoIncome = state.netCashFlowPerSec
    val xpRequired = LocalSimulationEngine.xpRequired(state.progression.level)
    val xpProgress = (state.progression.xp / xpRequired).toFloat().coerceIn(0f, 1f)

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 14.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (newsTicker.isNotBlank()) {
            item {
                Surface(
                    color = DarkSurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, GoldDark.copy(alpha = 0.75f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = CrimsonLoss, shape = RoundedCornerShape(5.dp)) {
                            Text("WIRE", color = Color.White, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                        Spacer(Modifier.width(9.dp))
                        Text(newsTicker, color = Color.White, maxLines = 1, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        item {
            Box(Modifier.fillMaxWidth().height(360.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(330.dp).clip(CircleShape)
                        .background(Brush.radialGradient(listOf(PurpleCrypto.copy(alpha = 0.34f), Color.Transparent)))
                )
                Box(
                    Modifier.size(258.dp).clip(CircleShape)
                        .background(Brush.radialGradient(listOf(Color(0xFFFFF6B0), GoldPrimary, Color(0xFFDB7900), GoldDark))),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier.size(218.dp).clip(CircleShape)
                            .background(Brush.radialGradient(listOf(Color(0xFFFFEFA0), Color(0xFFFFB900), Color(0xFF8B3E00)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AttachMoney,
                            contentDescription = "Tap to earn",
                            tint = Color(0xFFFFF6D2),
                            modifier = Modifier.size(142.dp)
                        )
                    }
                    Box(
                        Modifier.size(272.dp).scale(scale)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                onTap()
                                scope.launch {
                                    pressed = true
                                    delay(110)
                                    pressed = false
                                }
                            }
                            .testTag("tap_generator_button")
                    )
                }
            }
        }

        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("+${CurrencyFormatter.format(earnings)}", color = EmeraldProfit, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text("per tap", color = Color(0xFFDCCBEF), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 5.dp))
                }
                Spacer(Modifier.height(10.dp))
                Text("PLAYER LEVEL ${state.progression.level} • CLICKER LEVEL ${state.clickLevel}", color = Color(0xFFD7C3F0), style = MaterialTheme.typography.labelLarge, letterSpacing = 1.1.sp, modifier = Modifier.padding(top = 7.dp))
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TapMetric("Earnings / Tap", CurrencyFormatter.format(earnings), Icons.Default.Paid, Modifier.weight(1f), GoldLight)
                TapMetric("Multiplier", "${String.format("%.2f", state.clickMultiplier)}x", Icons.Default.Bolt, Modifier.weight(1f), PurpleCrypto)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TapMetric("Passive Income", "${CurrencyFormatter.format(autoIncome)}/s", Icons.Default.Layers, Modifier.weight(1f), EmeraldLight)
                TapMetric("Net Worth", CurrencyFormatter.format(state.totalNetWorth), Icons.Default.TrendingUp, Modifier.weight(1f), GoldLight)
            }
        }

        item {
            TycoonCard(Modifier.fillMaxWidth(), borderColor = PurpleCrypto.copy(alpha = 0.55f)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("PLAYER PROGRESSION", color = Color(0xFFBDA8D2), style = MaterialTheme.typography.labelSmall)
                        Text("LEVEL ${state.progression.level}", color = GoldLight, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${state.progression.totalClicks} TOTAL CLICKS", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("LIFETIME EARNED ${CurrencyFormatter.format(state.progression.lifetimeEarned)}", color = EmeraldLight, style = MaterialTheme.typography.labelSmall)
                    }
                }
                LinearProgressIndicator(progress = { xpProgress }, modifier = Modifier.fillMaxWidth())
                Text("${CurrencyFormatter.format(state.progression.xp)} / ${CurrencyFormatter.format(xpRequired)} XP TO NEXT LEVEL", color = Color(0xFFC9B8D9), style = MaterialTheme.typography.labelSmall)
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Upgrade, null, tint = GoldPrimary)
                Spacer(Modifier.width(8.dp))
                Text("TAP UPGRADES & EFFICIENCY", color = Color.White, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.7.sp)
            }
        }
        items(state.clickUpgrades, key = { it.id }) { upgrade ->
            ClickUpgradeItem(upgrade, state.cash) { onBuyUpgrade(upgrade.id) }
        }
    }
}

@Composable
private fun TapMetric(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, tint: Color) {
    Surface(
        modifier = modifier,
        color = Color(0xFF170827),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, PurpleCrypto.copy(alpha = 0.75f))
    ) {
        Column(Modifier.padding(horizontal = 9.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(5.dp))
            Text(title, color = Color(0xFFD6C7E7), style = MaterialTheme.typography.labelSmall, maxLines = 1)
            Text(value, color = tint, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium, maxLines = 1)
        }
    }
}

@Composable
fun ClickUpgradeItem(upgrade: ClickUpgrade, playerCash: Double, onBuy: () -> Unit) {
    val canAfford = playerCash >= upgrade.currentCost
    TycoonCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (canAfford) GoldPrimary.copy(alpha = 0.7f) else DarkCardBorder
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(upgrade.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Surface(color = PurpleContainer, shape = RoundedCornerShape(6.dp)) {
                        Text("Lv. ${upgrade.level}", color = GoldLight, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
                    }
                }
                Text(upgrade.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(10.dp))
            Button(
                onClick = onBuy,
                enabled = canAfford,
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black, disabledContainerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("buy_upgrade_${upgrade.id}")
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("UPGRADE", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelSmall)
                    Text(CurrencyFormatter.format(upgrade.currentCost), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
