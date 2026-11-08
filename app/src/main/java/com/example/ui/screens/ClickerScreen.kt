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
import com.example.model.ClickUpgrade
import com.example.model.GameState
import com.example.ui.components.CurrencyFormatter
import com.example.ui.components.TycoonCard
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class TapParticle(
    val id: Long,
    val text: String,
    val xOffset: Float,
    val yOffset: Float
)

@Composable
fun ClickerScreen(
    state: GameState,
    newsTicker: String = "",
    onTap: () -> Unit,
    onBuyUpgrade: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    val particles = remember { mutableStateListOf<TapParticle>() }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "click_scale"
    )

    val currentTapEarnings = state.clickPower * state.clickMultiplier * (if (state.research.find { it.id == "res_2" }?.isUnlocked == true) 2.0 else 1.0)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Live News Ticker Wire
        if (newsTicker.isNotEmpty()) {
            item {
                Surface(
                    color = DarkSurfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = CrimsonLoss,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "WIRE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = newsTicker,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Hero Clicker Area
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                contentAlignment = Alignment.Center
            ) {
                // Floating particles
                particles.forEach { particle ->
                    key(particle.id) {
                        FloatingText(particle.text, particle.xOffset, particle.yOffset)
                    }
                }

                // Golden Vault Button
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(GoldLight, GoldPrimary, GoldDark, DarkSurface)
                            )
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onTap()
                            coroutineScope.launch {
                                isPressed = true
                                val id = System.currentTimeMillis() + (0..1000).random()
                                val x = (-50..50).random().toFloat()
                                val y = (-30..30).random().toFloat()
                                particles.add(TapParticle(id, "+${CurrencyFormatter.format(currentTapEarnings)}", x, y))
                                delay(90)
                                isPressed = false
                                delay(700)
                                particles.removeAll { it.id == id }
                            }
                        }
                        .testTag("tap_generator_button"),
                    contentAlignment = Alignment.Center
                ) {
                    // Inner ring
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFF2E2405), Color(0xFF130F02))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = "Tap to Earn Cash",
                                tint = GoldPrimary,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "TAP TO EARN",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = GoldLight,
                                letterSpacing = 1.2.sp
                            )
                        }
                    }
                }
            }
        }

        // Tap Stats Card
        item {
            TycoonCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "EARNINGS PER TAP",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = CurrencyFormatter.format(currentTapEarnings),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = GoldLight,
                            modifier = Modifier.testTag("cash_per_tap_text")
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp)
                            .background(DarkCardBorder)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "TAP MULTIPLIER",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${state.clickMultiplier}x",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldLight
                        )
                    }
                }
            }
        }

        // Click Upgrades Section Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Upgrade,
                    contentDescription = null,
                    tint = GoldPrimary
                )
                Text(
                    text = "TAP UPGRADES & EFFICIENCY",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = Color.White
                )
            }
        }

        // Click Upgrades List
        items(state.clickUpgrades, key = { it.id }) { upg ->
            ClickUpgradeItem(
                upgrade = upg,
                playerCash = state.cash,
                onBuy = { onBuyUpgrade(upg.id) }
            )
        }
    }
}

@Composable
fun ClickUpgradeItem(
    upgrade: ClickUpgrade,
    playerCash: Double,
    onBuy: () -> Unit
) {
    val canAfford = playerCash >= upgrade.currentCost

    TycoonCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (canAfford) GoldPrimary.copy(alpha = 0.3f) else DarkCardBorder
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = upgrade.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Surface(
                        color = DarkSurfaceVariant,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "Lv. ${upgrade.level}",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldPrimary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = upgrade.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Button(
                onClick = onBuy,
                enabled = canAfford,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldPrimary,
                    contentColor = Color.Black,
                    disabledContainerColor = DarkSurfaceVariant,
                    disabledContentColor = Color.Gray
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("buy_upgrade_${upgrade.id}")
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "UPGRADE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = CurrencyFormatter.format(upgrade.currentCost),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun FloatingText(text: String, xOffset: Float, yOffset: Float) {
    val transition = rememberInfiniteTransition(label = "particle")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    Text(
        text = text,
        color = GoldPrimary,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier
            .offset(x = xOffset.dp, y = (yOffset - 40).dp)
    )
}
