package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.*
import com.example.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

private fun localMoney(value: Double): String =
    NumberFormat.getCurrencyInstance(Locale.US).format(value)

private fun localCompact(value: Double): String = when {
    kotlin.math.abs(value) >= 1e12 -> "$" + String.format(Locale.US, "%.2fT", value / 1e12)
    kotlin.math.abs(value) >= 1e9 -> "$" + String.format(Locale.US, "%.2fB", value / 1e9)
    kotlin.math.abs(value) >= 1e6 -> "$" + String.format(Locale.US, "%.2fM", value / 1e6)
    kotlin.math.abs(value) >= 1e3 -> "$" + String.format(Locale.US, "%.2fK", value / 1e3)
    else -> localMoney(value)
}

@Composable
private fun ReferenceCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = DarkSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.75f)),
        tonalElevation = 2.dp
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

@Composable
private fun ReferenceSectionTitle(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        if (!subtitle.isNullOrBlank()) {
            Text(subtitle, color = Color(0xFFBBA9CD), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ReferenceMetric(label: String, value: String, accent: Color = GoldLight) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label.uppercase(Locale.US), color = Color(0xFF9F8BB4), style = MaterialTheme.typography.labelSmall)
        Text(value, color = accent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun ReferenceTag(text: String, color: Color = PurpleCrypto) {
    Surface(color = color.copy(alpha = 0.14f), shape = RoundedCornerShape(8.dp)) {
        Text(
            text,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ReferenceAction(
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 40.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = PurpleContainer,
            contentColor = GoldLight,
            disabledContainerColor = DarkSurfaceVariant,
            disabledContentColor = Color(0xFF81758F)
        ),
        shape = RoundedCornerShape(11.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun ReferenceSecondaryAction(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        border = BorderStroke(1.dp, PurpleCrypto.copy(alpha = 0.65f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = PurpleCrypto),
        shape = RoundedCornerShape(11.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
        modifier = Modifier.heightIn(min = 38.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ReferenceAssetsScreen(state: LocalSimulationState, viewModel: LocalGameViewModel) {
    val netWorth = remember(state) { viewModel.localNetWorth() }
    val businessValue = state.referenceCompanies.sumOf { it.price + it.upgradeValue + it.reserves + it.uncollected - it.liabilities }
    val propertyValue = state.referenceProperties.sumOf { it.price + it.upgradeValue + it.uncollected }
    val stocksValue = state.stockHoldings.entries.sumOf {
        (viewModel.localStockPrice(it.key) ?: 0.0) * it.value.quantity
    }
    val cryptoValue = state.cryptoHoldings.entries.sumOf {
        (viewModel.localCryptoPrice(it.key) ?: 0.0) * it.value.quantity
    }
    val collectibleValue = state.collectibles.sumOf { it.currentValue }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ReferenceCard {
                Text("TOTAL NET WORTH", color = Color(0xFFBBA9CD), style = MaterialTheme.typography.labelMedium)
                Text(localCompact(netWorth), color = GoldLight, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ReferenceMetric("Available cash", localCompact(state.cash), EmeraldLight)
                    ReferenceMetric("Player level", "LVL ${state.progress.level}")
                    ReferenceMetric("Research points", localCompact(state.progress.researchPoints), CyanTech)
                }
                LinearProgressIndicator(
                    progress = { (state.progress.xp / PlayerProgression.xpForLevel(state.progress.level).toDouble()).toFloat().coerceIn(0f, 1f) },
                    color = PurpleCrypto,
                    trackColor = DarkSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().height(6.dp)
                )
                Text(
                    "${state.progress.xp.toLong()} / ${PlayerProgression.xpForLevel(state.progress.level)} XP to next level",
                    color = Color(0xFFAA99BA),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        item {
            ReferenceSectionTitle("Portfolio allocation", "Live valuation from locally simulated markets")
        }
        item { AllocationRow("Businesses", businessValue, netWorth, PurpleCrypto) }
        item { AllocationRow("Real estate", propertyValue, netWorth, GoldPrimary) }
        item { AllocationRow("Stocks", stocksValue, netWorth, CyanTech) }
        item { AllocationRow("Cryptocurrency", cryptoValue, netWorth, Color(0xFFFF7F50)) }
        item { AllocationRow("Collectibles", collectibleValue, netWorth, RarityLegendary) }
        item {
            ReferenceCard {
                ReferenceSectionTitle("Empire activity")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ReferenceMetric("Companies", "${state.referenceCompanies.size}/${state.businessCapacitySlots}")
                    ReferenceMetric("Properties", state.referenceProperties.size.toString())
                    ReferenceMetric("Collectibles", state.collectibles.size.toString())
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ReferenceMetric("Research complete", state.completedResearch.size.toString())
                    ReferenceMetric("Corporation", if (state.corporation == null) "None" else "Level ${state.corporation.level}")
                    ReferenceMetric("Open orders", state.marketOrders.count { it.status in setOf(LocalOrderStatus.OPEN, LocalOrderStatus.PARTIALLY_FILLED, LocalOrderStatus.TRIGGERED) }.toString())
                }
            }
        }
        item {
            ReferenceCard {
                ReferenceSectionTitle("Operating cash flow")
                ReferenceMetric("Lifetime revenue", localCompact(state.progress.lifetimeRevenue), EmeraldLight)
                ReferenceMetric("Lifetime expenses", localCompact(state.progress.lifetimeExpenses), CrimsonLight)
                ReferenceMetric("Dividends received", localCompact(state.progress.dividends), GoldLight)
                ReferenceMetric("Rental income collected", localCompact(state.progress.rentEarned), GoldLight)
            }
        }
    }
}

@Composable
private fun AllocationRow(label: String, amount: Double, total: Double, accent: Color) {
    val fraction = if (total > 0) (amount / total).toFloat().coerceIn(0f, 1f) else 0f
    ReferenceCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(localCompact(amount), color = accent, fontWeight = FontWeight.Bold)
            }
            Text("${(fraction * 100).toInt()}%", color = Color(0xFFBBA9CD), fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { fraction },
            color = accent,
            trackColor = DarkSurfaceVariant,
            modifier = Modifier.fillMaxWidth().height(5.dp)
        )
    }
}

@Composable
fun ReferenceTapScreen(state: LocalSimulationState, viewModel: LocalGameViewModel) {
    val clickerConfig = remember { "tap" }
    val cap = minOf(
        50,
        10 + state.progress.clickCapBonus +
            ReferenceCompanyEngine.bonusAmount(state, androidx.compose.ui.platform.LocalContext.current, "clickCap").roundToInt()
    )
    val level = state.progress.clickerLevel.coerceAtLeast(1)
    val hardCap = 50
    val curve = 6.0
    val basePower = 0.5 + ((1000.0 - 0.5) * (kotlin.math.exp(curve * ((level - 1).toDouble() / (hardCap - 1))) - 1.0)) / (kotlin.math.exp(curve) - 1.0)
    val capacity = 150.0
    val clickRequirement = when {
        level >= 50 -> Int.MAX_VALUE
        level <= 7 -> listOf(250, 500, 750, 1000, 1500, 2000, 3500)[level - 1]
        else -> 5000 + 2500 * (level - 8)
    }
    val ready = state.clickTokens.toInt()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp, 18.dp, 20.dp, 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ReferenceCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ReferenceMetric("Clicker level", "LVL $level")
                    ReferenceMetric("Maximum cap", "LVL $cap")
                    ReferenceMetric("Tap value", localMoney(basePower), EmeraldLight)
                }
                LinearProgressIndicator(
                    progress = { if (clickRequirement == Int.MAX_VALUE) 1f else (state.clickerCount.toFloat() / clickRequirement).coerceIn(0f, 1f) },
                    color = GoldPrimary,
                    trackColor = DarkSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().height(7.dp)
                )
                Text(
                    if (level >= 50) "Hard cap reached" else "${state.clickerCount} / $clickRequirement taps to next clicker level",
                    color = Color(0xFFBBA9CD),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        item {
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    onClick = { viewModel.tapLocalClicker(1) },
                    shape = CircleShape,
                    color = PurpleContainer,
                    border = BorderStroke(4.dp, PurpleCrypto),
                    shadowElevation = 20.dp,
                    modifier = Modifier.size(218.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.TouchApp, null, tint = GoldLight, modifier = Modifier.size(46.dp))
                            Text("TAP", color = Color.White, fontWeight = FontWeight.Black, fontSize = 30.sp)
                            Text("+${localMoney(basePower)}", color = EmeraldLight, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        item {
            ReferenceCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ReferenceMetric("Burst tokens", "${ready}/${capacity.toInt()}", CyanTech)
                    ReferenceMetric("Tokens / second", "22", PurpleCrypto)
                    ReferenceMetric("Lifetime taps", state.progress.lifetimeClicks.toString())
                }
                LinearProgressIndicator(
                    progress = { (state.clickTokens / capacity).toFloat().coerceIn(0f, 1f) },
                    color = CyanTech,
                    trackColor = DarkSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().height(6.dp)
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReferenceAction("Tap ×10", Modifier.weight(1f), ready >= 10) { viewModel.tapLocalClicker(10) }
                    ReferenceAction("Tap ×25", Modifier.weight(1f), ready >= 25) { viewModel.tapLocalClicker(25) }
                    ReferenceAction("Tap ×50", Modifier.weight(1f), ready >= 50) { viewModel.tapLocalClicker(50) }
                }
            }
        }
        item {
            ReferenceCard {
                ReferenceSectionTitle("Player progression")
                ReferenceMetric("Player XP", "${state.progress.xp.toLong()} / ${PlayerProgression.xpForLevel(state.progress.level)}")
                Text("The clicker cap expands through player level rewards and applicable local research or collection bonuses.", color = Color(0xFFBBA9CD), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun ReferenceMarketScreen(state: LocalSimulationState, viewModel: LocalGameViewModel) {
    var cryptoTab by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    val now = System.currentTimeMillis()
    val stocks = remember(now / 60_000L) { ReferenceMarketEngine.listStocks(now) }
    val crypto = remember(now / 60_000L) { ReferenceMarketEngine.listCrypto(now) }
    val stockHoldings = state.stockHoldings
    val cryptoHoldings = state.cryptoHoldings
    val filteredStocks = stocks.filter { it.name.contains(search, true) || it.ticker.contains(search, true) }
    val filteredCrypto = crypto.filter { it.name.contains(search, true) || it.symbol.contains(search, true) }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(selected = !cryptoTab, onClick = { cryptoTab = false }, label = { Text("Stocks") })
            FilterChip(selected = cryptoTab, onClick = { cryptoTab = true }, label = { Text("Crypto") })
        }
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            singleLine = true,
            label = { Text("Search exchange") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PurpleCrypto,
                unfocusedBorderColor = DarkCardBorder,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 90.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                ReferenceCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        ReferenceMetric("Portfolio value", localCompact(
                            if (cryptoTab) cryptoHoldings.entries.sumOf { (sym, h) -> (viewModel.localCryptoPrice(sym) ?: 0.0) * h.quantity }
                            else stockHoldings.entries.sumOf { (sym, h) -> (viewModel.localStockPrice(sym) ?: 0.0) * h.quantity }
                        ), EmeraldLight)
                        ReferenceMetric("Orders", state.marketOrders.count { it.status in setOf(LocalOrderStatus.OPEN, LocalOrderStatus.PARTIALLY_FILLED, LocalOrderStatus.TRIGGERED) }.toString())
                        ReferenceMetric("Trading fee", if (cryptoTab) "0.25%" else "0.50%")
                    }
                    Text(
                        if (cryptoTab) "2-minute deterministic price ticks · seven-day IPO listings · supply and burn models"
                        else "5-minute deterministic price ticks · new IPO listings every three days · exchange liquidity and dividends",
                        color = Color(0xFFBBA9CD),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            if (cryptoTab) {
                items(filteredCrypto, key = { it.symbol }) { asset ->
                    val price = viewModel.localCryptoPrice(asset.symbol) ?: asset.base
                    val holding = cryptoHoldings[asset.symbol]
                    MarketAssetCard(
                        name = asset.name,
                        symbol = asset.symbol,
                        price = price,
                        holding = holding?.quantity ?: 0.0,
                        costBasis = holding?.costBasis ?: 0.0,
                        subtitle = "${asset.profile} volatility · ${asset.supplyModel} supply",
                        onBuy = { viewModel.placeReferenceMarketOrder("crypto", asset.symbol, LocalOrderSide.BUY, LocalOrderKind.MARKET, 1.0) },
                        onSell = { viewModel.placeReferenceMarketOrder("crypto", asset.symbol, LocalOrderSide.SELL, LocalOrderKind.MARKET, minOf(1.0, holding?.quantity ?: 0.0)) },
                        canSell = (holding?.quantity ?: 0.0) > 0.0
                    )
                }
            } else {
                items(filteredStocks, key = { it.ticker }) { asset ->
                    val price = viewModel.localStockPrice(asset.ticker) ?: asset.base
                    val holding = stockHoldings[asset.ticker]
                    MarketAssetCard(
                        name = asset.name,
                        symbol = asset.ticker,
                        price = price,
                        holding = holding?.quantity ?: 0.0,
                        costBasis = holding?.costBasis ?: 0.0,
                        subtitle = "${asset.sector} · dividend ${String.format(Locale.US, "%.2f", asset.dividendYield * 100)}%",
                        onBuy = { viewModel.placeReferenceMarketOrder("stock", asset.ticker, LocalOrderSide.BUY, LocalOrderKind.MARKET, 1.0) },
                        onSell = { viewModel.placeReferenceMarketOrder("stock", asset.ticker, LocalOrderSide.SELL, LocalOrderKind.MARKET, minOf(1.0, holding?.quantity ?: 0.0)) },
                        canSell = (holding?.quantity ?: 0.0) > 0.0
                    )
                }
            }
            item {
                ReferenceCard {
                    ReferenceSectionTitle("Open market orders")
                    val open = state.marketOrders.filter { it.status in setOf(LocalOrderStatus.OPEN, LocalOrderStatus.PARTIALLY_FILLED, LocalOrderStatus.TRIGGERED) }
                    if (open.isEmpty()) {
                        Text("No open orders.", color = Color(0xFFAA99BA))
                    } else {
                        open.forEach { order ->
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text("${order.side} ${order.symbol} · ${order.kind}", color = Color.White, fontWeight = FontWeight.SemiBold)
                                    Text("${order.filledQuantity}/${order.quantity} filled · ${order.status}", color = Color(0xFFAA99BA), style = MaterialTheme.typography.labelSmall)
                                }
                                ReferenceSecondaryAction("Cancel") { viewModel.cancelReferenceMarketOrder(order.id) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MarketAssetCard(
    name: String,
    symbol: String,
    price: Double,
    holding: Double,
    costBasis: Double,
    subtitle: String,
    onBuy: () -> Unit,
    onSell: () -> Unit,
    canSell: Boolean
) {
    ReferenceCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(name, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(symbol, color = PurpleCrypto, style = MaterialTheme.typography.labelSmall)
                Text(subtitle, color = Color(0xFFAA99BA), style = MaterialTheme.typography.labelSmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(localMoney(price), color = GoldLight, fontWeight = FontWeight.Bold)
                if (holding > 0.0) {
                    Text("Owned: ${String.format(Locale.US, "%.4f", holding)}", color = EmeraldLight, style = MaterialTheme.typography.labelSmall)
                    Text("Cost basis: ${localCompact(costBasis)}", color = Color(0xFFAA99BA), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReferenceAction("Buy 1", Modifier.weight(1f), onClick = onBuy)
            ReferenceSecondaryAction("Sell 1", onClick = onSell, enabled = canSell)
        }
    }
}

@Composable
fun ReferenceEmpireScreen(
    state: LocalSimulationState,
    viewModel: LocalGameViewModel,
    section: String
) {
    when (section) {
        "Real Estate" -> ReferencePropertyScreen(state, viewModel)
        "Research" -> ReferenceResearchScreen(state, viewModel)
        "Luxury" -> ReferenceCollectiblesScreen(state, viewModel)
        "Awards" -> ReferenceAchievementsScreen(state, viewModel)
        "Stats" -> ReferenceStatisticsScreen(state, viewModel)
        else -> ReferenceBusinessScreen(state, viewModel)
    }
}

@Composable
fun ReferenceBusinessScreen(state: LocalSimulationState, viewModel: LocalGameViewModel) {
    val definitions = remember { viewModel.companyDefinitions() }
    val ownedById = state.referenceCompanies.associateBy { it.definitionId }
    val capacityUpgrade = state.capacityUpgrade
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 10.dp, 16.dp, 100.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            ReferenceCard {
                ReferenceSectionTitle("Business Management", "22 company definitions · branch, staffing, upgrade, reserve and liability systems")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ReferenceMetric("Owned", "${state.referenceCompanies.size}/${state.businessCapacitySlots}")
                    ReferenceMetric("Available capital", localCompact(state.cash - state.reservedCash), EmeraldLight)
                    ReferenceMetric("Total reserves", localCompact(state.referenceCompanies.sumOf { it.reserves }))
                }
                val progress = state.capacityUpgrade
                if (progress != null) {
                    val remaining = (progress.completesAt - System.currentTimeMillis()).coerceAtLeast(0L)
                    Text("Business Management upgrade: ${remaining / 60_000L} min remaining", color = CyanTech)
                    LinearProgressIndicator(
                        progress = { (1.0 - remaining.toDouble() / (30.0 * 60_000.0)).toFloat().coerceIn(0f, 1f) },
                        color = CyanTech,
                        trackColor = DarkSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    ReferenceSecondaryAction("Expand capacity · ${localCompact(50_000.0 * 2.2.pow(state.capacityLevel))}") {
                        viewModel.upgradeBusinessCapacity()
                    }
                }
            }
        }
        item { ReferenceSectionTitle("Owned companies", "Operating wallets are collected manually; negative operations draw on reserves and can create liabilities.") }
        items(state.referenceCompanies, key = { it.instanceId }) { company ->
            val definition = definitions.firstOrNull { it.optString("id") == company.definitionId }
            val economy = viewModel.companyEconomy(company.instanceId)
            ReferenceCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(definition?.optString("name") ?: company.definitionId, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(definition?.optString("industry") ?: "Business", color = PurpleCrypto, style = MaterialTheme.typography.labelSmall)
                    }
                    ReferenceTag(economy?.status ?: if (company.suspended) "Suspended" else "Active", if (company.suspended) CrimsonLoss else EmeraldLight)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ReferenceMetric("Net / hour", localCompact(economy?.net ?: 0.0), if ((economy?.net ?: 0.0) >= 0) EmeraldLight else CrimsonLight)
                    ReferenceMetric("Wallet", localCompact(company.uncollected))
                    ReferenceMetric("Reserves", localCompact(company.reserves))
                    ReferenceMetric("Liabilities", localCompact(company.liabilities), CrimsonLight)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ReferenceMetric("Branches", "${company.branches}")
                    ReferenceMetric("Staff", company.staff.values.sum().toString())
                    ReferenceMetric("Capacity", "${definition?.optInt("staffCapacity", 0) ?: 0}")
                }
                Text(
                    "Gross ${localCompact(economy?.revenue ?: 0.0)}/h · Expenses ${localCompact(economy?.expenses ?: 0.0)}/h",
                    color = Color(0xFFAA99BA),
                    style = MaterialTheme.typography.labelSmall
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    ReferenceAction("Collect", Modifier.weight(1f), company.uncollected > 0) {
                        viewModel.collectReferenceCompanyIncome(company.instanceId)
                    }
                    ReferenceSecondaryAction("Branch") { viewModel.openReferenceBranch(company.instanceId) }
                    ReferenceSecondaryAction(if (company.suspended) "Resume" else "Pause") {
                        viewModel.setReferenceCompanySuspended(company.instanceId, !company.suspended)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    ReferenceSecondaryAction("Hire 1") {
                        val firstRole = definition?.optJSONArray("roles")?.optJSONObject(0)?.optString("role")
                        if (!firstRole.isNullOrBlank()) viewModel.hireReferenceStaff(company.instanceId, firstRole, 1)
                    }
                    ReferenceSecondaryAction("Staff capacity") { viewModel.upgradeReferenceStaffCapacity(company.instanceId) }
                    ReferenceSecondaryAction("Inject 10K") { viewModel.injectReferenceCapital(company.instanceId, 10_000.0) }
                    ReferenceSecondaryAction("Sell") { viewModel.sellReferenceCompany(company.instanceId, true) }
                }
                val upgrades = definition?.optJSONArray("upgrades")
                if (upgrades != null && upgrades.length() > 0) {
                    Text("Upgrades", color = GoldLight, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    (0 until upgrades.length()).forEach { index ->
                        val upgrade = upgrades.optJSONObject(index) ?: return@forEach
                        val id = upgrade.optString("id")
                        val level = company.upgrades[id] ?: 0
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(upgrade.optString("name"), color = Color.White, style = MaterialTheme.typography.bodySmall)
                                Text("Level $level / ${upgrade.optInt("max", 1)}", color = Color(0xFFAA99BA), style = MaterialTheme.typography.labelSmall)
                            }
                            ReferenceSecondaryAction("Upgrade") { viewModel.upgradeReferenceCompany(company.instanceId, id) }
                        }
                    }
                }
            }
        }
        item { ReferenceSectionTitle("Available acquisitions", "Company capacity is limited. Costs, staff plans and upgrades come from bundled reference data.") }
        items(definitions, key = { it.optString("id") }) { definition ->
            val id = definition.optString("id")
            if (id !in ownedById) {
                ReferenceCard {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(definition.optString("name"), color = Color.White, fontWeight = FontWeight.Bold)
                            Text(definition.optString("industry"), color = PurpleCrypto, style = MaterialTheme.typography.labelSmall)
                        }
                        ReferenceTag(localCompact(definition.optDouble("price", 0.0)), GoldPrimary)
                    }
                    Text(
                        "Revenue ${localCompact(definition.optDouble("revenue", 0.0))}/h · fixed costs ${localCompact(definition.optDouble("fixedExpense", 0.0))}/h · starting staff ${definition.optInt("startStaff", 0)}",
                        color = Color(0xFFAA99BA),
                        style = MaterialTheme.typography.bodySmall
                    )
                    ReferenceAction("Acquire company") { viewModel.buyReferenceCompany(id) }
                }
            }
        }
    }
}

@Composable
fun ReferencePropertyScreen(state: LocalSimulationState, viewModel: LocalGameViewModel) {
    val definitions = remember { viewModel.propertyDefinitions() }
    val ownedById = state.referenceProperties.associateBy { it.definitionId }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 10.dp, 16.dp, 100.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            ReferenceCard {
                ReferenceSectionTitle("Real Estate Portfolio", "Acquisitions, occupancy, market demand, property condition, upgrades and resale")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ReferenceMetric("Owned", state.referenceProperties.size.toString())
                    ReferenceMetric("Rent wallet", localCompact(state.referenceProperties.sumOf { it.uncollected }), EmeraldLight)
                    ReferenceMetric("Portfolio value", localCompact(state.referenceProperties.sumOf { it.price + it.upgradeValue }))
                }
                ReferenceAction("Collect all rent") { viewModel.collectReferenceRent() }
            }
        }
        item { ReferenceSectionTitle("Owned properties") }
        items(state.referenceProperties, key = { it.instanceId }) { property ->
            val definition = definitions.firstOrNull { it.optString("id") == property.definitionId }
            val economy = viewModel.propertyEconomy(property.instanceId)
            ReferenceCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(definition?.optString("name") ?: property.definitionId, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(definition?.optString("category") ?: "Property", color = PurpleCrypto, style = MaterialTheme.typography.labelSmall)
                    }
                    ReferenceTag("${property.condition.toInt()}% condition", if (property.condition < 40) CrimsonLoss else EmeraldLight)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ReferenceMetric("Net / hour", localCompact(economy?.net ?: 0.0), if ((economy?.net ?: 0.0) >= 0) EmeraldLight else CrimsonLight)
                    ReferenceMetric("Occupancy", "${((economy?.occupancy ?: 0.0) * 100).toInt()}%")
                    ReferenceMetric("Uncollected", localCompact(property.uncollected))
                }
                Text(
                    "Demand: ${(economy?.demand ?: 0.0).let { if (it > 1.05) "High" else if (it < 0.95) "Low" else "Moderate" }} · Expenses ${localCompact(economy?.expenses ?: 0.0)}/h",
                    color = Color(0xFFAA99BA),
                    style = MaterialTheme.typography.labelSmall
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    ReferenceAction("Collect rent", Modifier.weight(1f), property.uncollected > 0) {
                        viewModel.collectReferenceRent(property.instanceId)
                    }
                    ReferenceSecondaryAction("Maintain") { viewModel.maintainReferenceProperty(property.instanceId) }
                    ReferenceSecondaryAction("Sell · 75%") { viewModel.sellReferenceProperty(property.instanceId) }
                }
                val firstUpgrade = viewModel.propertyUpgradeDefinitions().firstOrNull { upgrade ->
                    val categories = upgrade.optJSONArray("categories") ?: JSONArray()
                    (0 until categories.length()).any { categories.optString(it) == definition?.optString("category") }
                }
                if (firstUpgrade != null) {
                    ReferenceSecondaryAction("Upgrade: ${firstUpgrade.optString("name")}") {
                        viewModel.upgradeReferenceProperty(property.instanceId, firstUpgrade.optString("id"))
                    }
                }
            }
        }
        item { ReferenceSectionTitle("Available properties", "The reference catalogue includes residential, commercial, industrial, hospitality and luxury assets.") }
        items(definitions, key = { it.optString("id") }) { definition ->
            val id = definition.optString("id")
            if (id !in ownedById) {
                ReferenceCard {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(definition.optString("name"), color = Color.White, fontWeight = FontWeight.Bold)
                            Text("${definition.optString("category")} · ${definition.optString("location")}", color = PurpleCrypto, style = MaterialTheme.typography.labelSmall)
                        }
                        ReferenceTag(localCompact(definition.optDouble("price", 0.0)), GoldPrimary)
                    }
                    Text(
                        "Base rent ${localCompact(definition.optDouble("rent", 0.0))}/h · operating expenses ${localCompact(definition.optDouble("expense", 0.0))}/h · demand ${(definition.optDouble("demand", 1.0) * 100).toInt()}%",
                        color = Color(0xFFAA99BA),
                        style = MaterialTheme.typography.bodySmall
                    )
                    ReferenceAction("Purchase property") { viewModel.buyReferenceProperty(id) }
                }
            }
        }
    }
}

@Composable
fun ReferenceResearchScreen(state: LocalSimulationState, viewModel: LocalGameViewModel) {
    val definitions = remember { viewModel.researchDefinitions() }
    val active = state.researchProjects.associateBy { it.id }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 10.dp, 16.dp, 100.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            ReferenceCard {
                ReferenceSectionTitle("Research & Development", "Research points, prerequisites, concurrent project slots and timed completion")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ReferenceMetric("Research points", localCompact(state.progress.researchPoints), CyanTech)
                    ReferenceMetric("Projects complete", state.completedResearch.size.toString())
                    ReferenceMetric("Slots", state.researchSlots.toString())
                }
                if (!state.researchBuilt) {
                    Text("Research Department construction unlocks at Player Level 5.", color = Color(0xFFBBA9CD))
                    ReferenceAction("Build Research Department · 100K") { viewModel.buildResearchDepartment() }
                } else {
                    ReferenceSecondaryAction("Purchase additional research slot") { viewModel.purchaseResearchSlot() }
                }
            }
        }
        if (active.isNotEmpty()) {
            item { ReferenceSectionTitle("Active projects") }
            items(active.values.toList(), key = { it.id }) { project ->
                val remaining = (project.completesAt - System.currentTimeMillis()).coerceAtLeast(0L)
                ReferenceCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(project.name, color = Color.White, fontWeight = FontWeight.Bold)
                        ReferenceTag("${remaining / 60_000L} min")
                    }
                    Text("Completion is processed locally when the project timer expires.", color = Color(0xFFAA99BA), style = MaterialTheme.typography.bodySmall)
                    LinearProgressIndicator(
                        progress = { (1.0 - remaining.toDouble() / project.durationMillis.coerceAtLeast(1L)).toFloat().coerceIn(0f, 1f) },
                        color = CyanTech,
                        trackColor = DarkSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        item { ReferenceSectionTitle("Research catalogue") }
        items(definitions, key = { it.optString("id") }) { definition ->
            val id = definition.optString("id")
            val done = id in state.completedResearch
            val running = id in active
            val prereq = definition.optJSONArray("prereq") ?: JSONArray()
            val missing = (0 until prereq.length()).map { prereq.optString(it) }.filter { it !in state.completedResearch }
            ReferenceCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(definition.optString("name"), color = Color.White, fontWeight = FontWeight.Bold)
                        Text(definition.optString("category"), color = PurpleCrypto, style = MaterialTheme.typography.labelSmall)
                    }
                    ReferenceTag(if (done) "COMPLETED" else if (running) "IN PROGRESS" else "LOCKED / READY",
                        if (done) EmeraldLight else if (running) CyanTech else GoldPrimary)
                }
                Text(definition.optString("description"), color = Color(0xFFAA99BA), style = MaterialTheme.typography.bodySmall)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ReferenceMetric("Investment", localCompact(definition.optDouble("cost", 0.0)))
                    ReferenceMetric("RP required", localCompact(definition.optDouble("rp", 0.0)), CyanTech)
                    ReferenceMetric("Duration", "${definition.optDouble("hours", 0.0)}h")
                }
                if (missing.isNotEmpty()) Text("Prerequisites: ${missing.joinToString()}", color = CrimsonLight, style = MaterialTheme.typography.labelSmall)
                if (!done && !running) {
                    ReferenceAction(
                        if (missing.isNotEmpty()) "Prerequisites required" else "Start research",
                        enabled = state.researchBuilt && missing.isEmpty() &&
                            state.progress.researchPoints >= definition.optDouble("rp", 0.0)
                    ) { viewModel.startReferenceResearch(id) }
                }
            }
        }
    }
}

@Composable
fun ReferenceCollectiblesScreen(state: LocalSimulationState, viewModel: LocalGameViewModel) {
    val definitions = remember { viewModel.collectibleDefinitions() }
    var category by remember { mutableStateOf("All") }
    var query by remember { mutableStateOf("") }
    val categories = remember { viewModel.collectibleCategoryDefinitions().map { it.optString("id") }.distinct() }
    val owned = state.collectibles.groupBy { it.definitionId }
    val filtered = definitions.filter {
        (category == "All" || it.optString("category") == category) &&
            (query.isBlank() || it.optString("name").contains(query, true) || it.optString("id").contains(query, true))
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 10.dp, 16.dp, 100.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            ReferenceCard {
                ReferenceSectionTitle("Luxury Assets & Collectibles", "300 generated catalogue entries · six rarities · category-specific value cycles and bonuses")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ReferenceMetric("Owned", state.collectibles.size.toString())
                    ReferenceMetric("Collections", "${state.collectionsRewarded.size}")
                    ReferenceMetric("Value", localCompact(state.collectibles.sumOf { it.currentValue }))
                }
                Text("Rarity holding locks: Common 1h · Uncommon 3h · Rare 6h · Epic 12h · Legendary 24h · Mythical 72h. Mythical items cannot be resold.", color = Color(0xFFAA99BA), style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search collectible catalogue") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PurpleCrypto, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = category == "All", onClick = { category = "All" }, label = { Text("All") })
                categories.take(4).forEach { id ->
                    FilterChip(selected = category == id, onClick = { category = id }, label = { Text(id.replaceFirstChar { it.uppercase() }) })
                }
            }
        }
        if (state.collectibles.isNotEmpty()) {
            item { ReferenceSectionTitle("Your collection") }
            items(state.collectibles, key = { it.instanceId }) { holding ->
                val definition = definitions.firstOrNull { it.optString("id") == holding.definitionId }
                ReferenceCard {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(definition?.optString("name") ?: holding.definitionId, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(definition?.optString("rarity") ?: "Collectible", color = rarityColor(definition?.optString("rarity") ?: "Common"))
                        }
                        ReferenceMetric("Current value", localCompact(viewModel.collectibleValuation(holding.definitionId)?.currentValue ?: holding.currentValue))
                    }
                    val remain = (holding.sellableAt - System.currentTimeMillis()).coerceAtLeast(0L)
                    val restricted = definition?.optBoolean("restrictedResale", false) == true
                    Text(
                        if (restricted) "Special edition · permanent resale restriction" else if (remain > 0) "Holding period: ${remain / 3_600_000L}h ${(remain % 3_600_000L) / 60_000L}m remaining" else "Eligible for resale",
                        color = if (restricted) RarityMythical else Color(0xFFAA99BA),
                        style = MaterialTheme.typography.labelSmall
                    )
                    ReferenceSecondaryAction("Sell collectible") { viewModel.sellReferenceCollectible(holding.instanceId) }
                }
            }
        }
        item { ReferenceSectionTitle("Market catalogue", "${filtered.size} of ${definitions.size} items") }
        items(filtered, key = { it.optString("id") }) { definition ->
            val id = definition.optString("id")
            val value = viewModel.collectibleValuation(id)
            val ownedCount = owned[id]?.size ?: 0
            ReferenceCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(definition.optString("name"), color = Color.White, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ReferenceTag(definition.optString("rarity"), rarityColor(definition.optString("rarity")))
                            ReferenceTag(definition.optString("category"))
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(localCompact(definition.optDouble("price", 0.0)), color = GoldLight, fontWeight = FontWeight.Bold)
                        Text("Value ${localCompact(value?.currentValue ?: 0.0)}", color = EmeraldLight, style = MaterialTheme.typography.labelSmall)
                    }
                }
                Text("Demand: ${value?.demand ?: "Moderate"} · ${if (definition.isNull("limited")) "Open edition" else "Limited edition"} · Owned: $ownedCount", color = Color(0xFFAA99BA), style = MaterialTheme.typography.labelSmall)
                ReferenceAction("Acquire collectible") { viewModel.buyReferenceCollectible(id) }
            }
        }
    }
}

private fun rarityColor(rarity: String): Color = when (rarity) {
    "Uncommon" -> RarityUncommon
    "Rare" -> RarityRare
    "Epic" -> RarityEpic
    "Legendary" -> RarityLegendary
    "Mythical" -> RarityMythical
    else -> RarityCommon
}

@Composable
fun ReferenceAchievementsScreen(state: LocalSimulationState, viewModel: LocalGameViewModel) {
    val definitions = remember { viewModel.achievementDefinitions() }
    val unlocked = state.unlockedAchievements.keys
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 10.dp, 16.dp, 100.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item {
            ReferenceCard {
                ReferenceSectionTitle("Achievements & Milestones", "Automatic evaluation against persistent player statistics")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ReferenceMetric("Unlocked", unlocked.size.toString(), EmeraldLight)
                    ReferenceMetric("Available", definitions.size.toString())
                    ReferenceMetric("Repeatable", state.achievementCycles.values.sum().toString(), GoldLight)
                }
                ReferenceSecondaryAction("Re-evaluate achievements") { viewModel.refreshReferenceEconomy() }
            }
        }
        items(definitions, key = { it.optString("id") }) { definition ->
            val id = definition.optString("id")
            val done = id in unlocked
            val cycle = state.achievementCycles[id] ?: 0
            val reward = definition.optJSONObject("reward")
            ReferenceCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(definition.optString("name"), color = if (done) GoldLight else Color.White, fontWeight = FontWeight.Bold)
                        Text("${definition.optString("category")} · ${definition.optString("sub")}", color = PurpleCrypto, style = MaterialTheme.typography.labelSmall)
                    }
                    ReferenceTag(if (done) "UNLOCKED" else if (definition.optBoolean("secret", false)) "SECRET" else definition.optString("tier"),
                        if (done) EmeraldLight else GoldPrimary)
                }
                Text("Target: ${localCompact(definition.optDouble("target", 0.0))} ${definition.optString("stat")}", color = Color(0xFFAA99BA), style = MaterialTheme.typography.bodySmall)
                if (definition.optJSONObject("repeatable") != null) Text("Completed cycles: $cycle", color = CyanTech, style = MaterialTheme.typography.labelSmall)
                Text(
                    "Reward: ${localCompact(reward?.optDouble("cash", 0.0) ?: 0.0)} cash · ${localCompact(reward?.optDouble("rp", 0.0) ?: 0.0)} RP",
                    color = EmeraldLight,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
fun ReferenceStatisticsScreen(state: LocalSimulationState, viewModel: LocalGameViewModel) {
    val netWorth = viewModel.localNetWorth()
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 10.dp, 16.dp, 100.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            ReferenceCard {
                ReferenceSectionTitle("Financial & Progression Statistics")
                ReferenceMetric("Net worth", localCompact(netWorth), GoldLight)
                ReferenceMetric("Cash balance", localCompact(state.cash), EmeraldLight)
                ReferenceMetric("Lifetime revenue", localCompact(state.progress.lifetimeRevenue), EmeraldLight)
                ReferenceMetric("Lifetime expenses", localCompact(state.progress.lifetimeExpenses), CrimsonLight)
                ReferenceMetric("Net operating cash flow", localCompact(state.progress.lifetimeRevenue - state.progress.lifetimeExpenses))
            }
        }
        item {
            ReferenceCard {
                ReferenceSectionTitle("Activity counters")
                StatsGrid(
                    listOf(
                        "Companies acquired" to state.progress.companiesBought.toString(),
                        "Companies sold" to state.progress.companiesSold.toString(),
                        "Branches opened" to state.progress.branchesOpened.toString(),
                        "Upgrades purchased" to state.progress.upgradesPurchased.toString(),
                        "Staff hired" to state.progress.hires.toString(),
                        "Properties purchased" to state.progress.propertiesBought.toString(),
                        "Properties sold" to state.progress.propertiesSold.toString(),
                        "Maintenance jobs" to state.progress.maintenance.toString(),
                        "Stock trades" to state.progress.stockTrades.toString(),
                        "Crypto trades" to state.progress.cryptoTrades.toString(),
                        "Dividends" to localCompact(state.progress.dividends),
                        "Collectibles acquired" to state.progress.collectiblesBought.toString(),
                        "Collections completed" to state.progress.collectionsDone.toString(),
                        "Research completed" to state.progress.researchDone.toString(),
                        "Clicks performed" to state.progress.lifetimeClicks.toString(),
                        "Player level" to state.progress.level.toString()
                    )
                )
            }
        }
        item {
            ReferenceCard {
                ReferenceSectionTitle("Recent transaction ledger")
                if (state.ledger.isEmpty()) Text("No transactions recorded yet.", color = Color(0xFFAA99BA))
                state.ledger.takeLast(60).asReversed().forEach { entry ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(entry.description, color = Color.White, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("${entry.category} · ${SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()).format(Date(entry.timestamp))}", color = Color(0xFF8F7CA4), style = MaterialTheme.typography.labelSmall)
                        }
                        Text(
                            (if (entry.amount >= 0) "+" else "") + localCompact(entry.amount),
                            color = if (entry.amount >= 0) EmeraldLight else CrimsonLight,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
private fun StatsGrid(entries: List<Pair<String, String>>) {
    entries.chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            row.forEach { (label, value) ->
                Column(Modifier.weight(1f)) {
                    Text(label, color = Color(0xFF9F8BB4), style = MaterialTheme.typography.labelSmall)
                    Text(value, color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
fun ReferenceCorporationScreen(state: LocalSimulationState, viewModel: LocalGameViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val corp = state.corporation
    var corpName by remember { mutableStateOf("Capital Syndicate") }
    var corpTag by remember { mutableStateOf("CFS") }
    var recruitName by remember { mutableStateOf("") }
    var contribution by remember { mutableStateOf("10000") }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 10.dp, 16.dp, 100.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (corp == null) {
            item {
                ReferenceCard {
                    ReferenceSectionTitle("Corporation Management", "Local corporation simulation with AI-controlled members; no online player service is required.")
                    ReferenceMetric("Player level", state.progress.level.toString())
                    ReferenceMetric("Creation fee", localCompact(10_000_000.0))
                    OutlinedTextField(
                        value = corpName,
                        onValueChange = { corpName = it },
                        label = { Text("Corporation name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = PurpleCrypto)
                    )
                    OutlinedTextField(
                        value = corpTag,
                        onValueChange = { corpTag = it.uppercase(Locale.US).take(5) },
                        label = { Text("Tag (2–5 characters)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = PurpleCrypto)
                    )
                    ReferenceAction("Found corporation") { viewModel.createLocalCorporation(corpName, corpTag) }
                    Text("Requires Player Level 10 and available capital. The first member is the device-local player; additional members can be simulated AI members.", color = Color(0xFFAA99BA), style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            item {
                ReferenceCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(corp.name, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                            Text("[${corp.tag}] · Local corporation", color = PurpleCrypto)
                        }
                        ReferenceTag("LEVEL ${corp.level}", GoldPrimary)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        ReferenceMetric("Treasury", localCompact(corp.treasury), EmeraldLight)
                        ReferenceMetric("Members", "${corp.members.size}/${ReferenceCorporationEngine.capacity(corp)}")
                        ReferenceMetric("Experience", corp.experience.toString())
                    }
                    Text(corp.description.ifBlank { "No corporation description set." }, color = Color(0xFFAA99BA), style = MaterialTheme.typography.bodySmall)
                    LinearProgressIndicator(
                        progress = { (corp.experience.toDouble() / (1000.0 * 1.5.pow(corp.level - 1))).toFloat().coerceIn(0f, 1f) },
                        color = GoldPrimary,
                        trackColor = DarkSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().height(5.dp)
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        ReferenceAction("Complete shift") { viewModel.completeCorporationShift() }
                        ReferenceSecondaryAction("Leave") { viewModel.leaveCorporation() }
                        ReferenceSecondaryAction("Dissolve") { viewModel.dissolveCorporation(corp.name) }
                    }
                }
            }
            item {
                ReferenceCard {
                    ReferenceSectionTitle("Treasury contribution")
                    OutlinedTextField(
                        value = contribution,
                        onValueChange = { contribution = it.filter { char -> char.isDigit() }.take(14) },
                        label = { Text("Contribution amount") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    ReferenceAction("Contribute to treasury") {
                        contribution.toDoubleOrNull()?.let(viewModel::contributeToLocalCorporation)
                    }
                    Text("Contribution limit today: ${localCompact(ReferenceCorporationEngine.dailyLimit(corp))}. Contributions earn diminishing-rate corporation XP by daily contribution band.", color = Color(0xFFAA99BA), style = MaterialTheme.typography.bodySmall)
                }
            }
            item {
                ReferenceCard {
                    ReferenceSectionTitle("Member roster")
                    corp.members.forEach { member ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = PurpleContainer, modifier = Modifier.size(34.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(if (member.isNpc) Icons.Default.SmartToy else Icons.Default.Person, null, tint = GoldLight, modifier = Modifier.size(18.dp))
                                }
                            }
                            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                                Text(member.displayName, color = Color.White, fontWeight = FontWeight.SemiBold)
                                Text("${member.rank} · ${member.department ?: "Unassigned"}${if (member.isNpc) " · AI" else ""}", color = Color(0xFFAA99BA), style = MaterialTheme.typography.labelSmall)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Shifts ${member.shiftsCompleted}", color = CyanTech, style = MaterialTheme.typography.labelSmall)
                                Text(localCompact(member.lifetimeContribution), color = EmeraldLight, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    OutlinedTextField(
                        value = recruitName,
                        onValueChange = { recruitName = it.take(24) },
                        label = { Text("AI member display name (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    ReferenceAction("Recruit local AI member") {
                        viewModel.recruitCorporationNpc(recruitName.ifBlank { null })
                        recruitName = ""
                    }
                }
            }
            item {
                ReferenceCard {
                    ReferenceSectionTitle("Departments")
                    viewModel.corporationDepartmentDefinitions().forEach { definition ->
                        val id = definition.optString("id")
                        val built = corp.departments[id]
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(definition.optString("name"), color = Color.White, fontWeight = FontWeight.SemiBold)
                                Text(definition.optString("description"), color = Color(0xFFAA99BA), style = MaterialTheme.typography.labelSmall)
                            }
                            if (built != null) ReferenceTag("ESTABLISHED", EmeraldLight)
                            else ReferenceSecondaryAction("Establish · ${localCompact(definition.optDouble("cost", 0.0))}") { viewModel.establishCorporationDepartment(id) }
                        }
                    }
                }
            }
            item {
                ReferenceCard {
                    ReferenceSectionTitle("Corporation facilities")
                    viewModel.corporationFacilityDefinitions().forEach { definition ->
                        val id = definition.optString("id")
                        val level = corp.facilities[id] ?: 0
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(definition.optString("name"), color = Color.White, fontWeight = FontWeight.SemiBold)
                                Text("Level $level / ${definition.optInt("max", 0)} · ${definition.optString("description")}", color = Color(0xFFAA99BA), style = MaterialTheme.typography.labelSmall)
                            }
                            ReferenceSecondaryAction("Upgrade") { viewModel.upgradeCorporationFacility(id) }
                        }
                    }
                }
            }
            item {
                ReferenceCard {
                    ReferenceSectionTitle("Corporation businesses")
                    viewModel.corporationBusinessDefinitions().forEach { definition ->
                        val id = definition.optString("id")
                        val owned = corp.businesses.any { it.definitionId == id }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(definition.optString("name"), color = Color.White, fontWeight = FontWeight.SemiBold)
                                Text("Department: ${definition.optString("dept")} · Revenue ${localCompact(definition.optDouble("revenue", 0.0))}/h", color = Color(0xFFAA99BA), style = MaterialTheme.typography.labelSmall)
                            }
                            if (owned) ReferenceTag("OWNED", EmeraldLight)
                            else ReferenceSecondaryAction("Acquire") { viewModel.acquireCorporationBusiness(id) }
                        }
                    }
                }
            }
            item {
                ReferenceCard {
                    ReferenceSectionTitle("Shared objectives")
                    if (corp.objectives.isEmpty()) Text("New objectives are generated by the local corporation cycle.", color = Color(0xFFAA99BA))
                    corp.objectives.forEach { objective ->
                        Text(objective.title, color = Color.White, fontWeight = FontWeight.Bold)
                        Text("${objective.type} · ${localCompact(objective.progress)} / ${localCompact(objective.target)} · ${objective.participants.size}/${objective.minParticipants} participants", color = Color(0xFFAA99BA), style = MaterialTheme.typography.labelSmall)
                        LinearProgressIndicator(
                            progress = { (objective.progress / objective.target).toFloat().coerceIn(0f, 1f) },
                            color = EmeraldProfit,
                            trackColor = DarkSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReferenceProfileScreen(state: LocalSimulationState, viewModel: LocalGameViewModel) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 10.dp, 16.dp, 100.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            ReferenceCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = PurpleContainer, modifier = Modifier.size(68.dp)) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, tint = GoldLight, modifier = Modifier.size(40.dp)) }
                    }
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(state.playerName, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp)
                        Text("Player Level ${state.progress.level} · Clicker Level ${state.progress.clickerLevel}", color = PurpleCrypto)
                        Text("Local device profile · offline progression", color = Color(0xFFAA99BA), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        item {
            ReferenceCard {
                ReferenceSectionTitle("Progression")
                ReferenceMetric("Experience", "${state.progress.xp.toLong()} / ${PlayerProgression.xpForLevel(state.progress.level)}")
                ReferenceMetric("Lifetime clicks", state.progress.lifetimeClicks.toString())
                ReferenceMetric("Companies acquired", state.progress.companiesBought.toString())
                ReferenceMetric("Trades", (state.progress.stockTrades + state.progress.cryptoTrades).toString())
                ReferenceMetric("Collections completed", state.progress.collectionsDone.toString())
            }
        }
    }
}

@Composable
fun ReferenceSettingsScreen(viewModel: LocalGameViewModel) {
    val error by viewModel.lastError.collectAsStateWithLifecycle()
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 10.dp, 16.dp, 100.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            ReferenceCard {
                ReferenceSectionTitle("Local game settings")
                Text("Capital Fortune stores its progression in the device Room database. Core simulation, markets, R&D, collectibles and corporation simulation do not require Firebase or an internet connection.", color = Color(0xFFAA99BA), style = MaterialTheme.typography.bodySmall)
                ReferenceAction("Save progress now") { viewModel.saveNow() }
                ReferenceSecondaryAction("Refresh progression and achievements") { viewModel.refreshReferenceEconomy() }
                if (!error.isNullOrBlank()) Text(error!!, color = CrimsonLight)
            }
        }
        item {
            ReferenceCard {
                ReferenceSectionTitle("Save architecture")
                Text("Primary store: capital_fortune_local.db · local Room database", color = Color.White)
                Text("Transactions and game state are persisted together as a single save snapshot. Network sign-in, cloud saves, remote config and callable functions are not required.", color = Color(0xFFAA99BA), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
