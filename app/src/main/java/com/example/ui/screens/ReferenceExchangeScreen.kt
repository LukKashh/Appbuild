package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
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
import java.util.Locale

private fun exchangeMoney(v: Double) = NumberFormat.getCurrencyInstance(Locale.US).format(v)
private fun exchangeCompact(v: Double) = when {
    kotlin.math.abs(v) >= 1e12 -> "$" + "%.2fT".format(Locale.US, v / 1e12)
    kotlin.math.abs(v) >= 1e9 -> "$" + "%.2fB".format(Locale.US, v / 1e9)
    kotlin.math.abs(v) >= 1e6 -> "$" + "%.2fM".format(Locale.US, v / 1e6)
    kotlin.math.abs(v) >= 1e3 -> "$" + "%.2fK".format(Locale.US, v / 1e3)
    else -> exchangeMoney(v)
}

@Composable
fun ReferenceExchangeScreen(state: LocalSimulationState, viewModel: LocalGameViewModel) {
    var cryptoTab by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    val now = System.currentTimeMillis()
    val stocks = remember(now / 60_000L) { viewModel.localMarketStocks() }
    val cryptos = remember(now / 60_000L) { viewModel.localMarketCrypto() }

    val stockValue = state.stockHoldings.entries.sumOf { (symbol, holding) ->
        (viewModel.localStockPrice(symbol) ?: 0.0) * holding.quantity
    }
    val cryptoValue = state.cryptoHoldings.entries.sumOf { (symbol, holding) ->
        (viewModel.localCryptoPrice(symbol) ?: 0.0) * holding.quantity
    }
    val openOrders = state.marketOrders.filter {
        it.status in setOf(LocalOrderStatus.OPEN, LocalOrderStatus.TRIGGERED, LocalOrderStatus.PARTIALLY_FILLED)
    }

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
                Surface(
                    color = DarkSurface,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("EXCHANGE PORTFOLIO", color = Color(0xFFBBA9CD), style = MaterialTheme.typography.labelMedium)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Portfolio value", color = Color(0xFF9F8BB4), style = MaterialTheme.typography.labelSmall)
                                Text(exchangeCompact(if (cryptoTab) cryptoValue else stockValue), color = EmeraldLight, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Open orders", color = Color(0xFF9F8BB4), style = MaterialTheme.typography.labelSmall)
                                Text(openOrders.size.toString(), color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            }
                        }
                        Text(
                            if (cryptoTab) "2-minute deterministic pricing · 7-day new listings · inflation and burn supply models · 0.25% fee"
                            else "5-minute deterministic pricing · IPOs every 3 days · liquidity and daily dividends · 0.50% fee",
                            color = Color(0xFFAA99BA), style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            if (cryptoTab) {
                val filtered = cryptos.filter { it.name.contains(search, true) || it.symbol.contains(search, true) }
                items(filtered, key = { "crypto:${it.symbol}" }) { asset ->
                    val price = viewModel.localCryptoPrice(asset.symbol) ?: asset.base
                    val holding = state.cryptoHoldings[asset.symbol]
                    ExchangeAssetCard(
                        name = asset.name,
                        symbol = asset.symbol,
                        category = "${asset.profile} volatility · ${asset.supplyModel} supply",
                        price = price,
                        quantity = holding?.quantity ?: 0.0,
                        costBasis = holding?.costBasis ?: 0.0,
                        market = "crypto",
                        onSubmit = viewModel::placeReferenceMarketOrder
                    )
                }
            } else {
                val filtered = stocks.filter { it.name.contains(search, true) || it.ticker.contains(search, true) }
                items(filtered, key = { "stock:${it.ticker}" }) { asset ->
                    val price = viewModel.localStockPrice(asset.ticker) ?: asset.base
                    val holding = state.stockHoldings[asset.ticker]
                    ExchangeAssetCard(
                        name = asset.name,
                        symbol = asset.ticker,
                        category = "${asset.sector} · dividend ${"%.2f".format(Locale.US, asset.dividendYield * 100)}%",
                        price = price,
                        quantity = holding?.quantity ?: 0.0,
                        costBasis = holding?.costBasis ?: 0.0,
                        market = "stock",
                        onSubmit = viewModel::placeReferenceMarketOrder
                    )
                }
            }
            item {
                Surface(
                    color = DarkSurface,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Text("OPEN ORDERS", color = Color.White, fontWeight = FontWeight.Bold)
                        if (openOrders.isEmpty()) {
                            Text("No pending orders.", color = Color(0xFFAA99BA), style = MaterialTheme.typography.bodySmall)
                        } else {
                            openOrders.forEach { order ->
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text("${order.side} ${order.symbol} · ${order.kind}", color = Color.White, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            "${order.filledQuantity}/${order.quantity} filled · ${order.status} · expires ${(order.expiresAt - now).coerceAtLeast(0L) / 3_600_000L}h",
                                            color = Color(0xFFAA99BA), style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                    TextButton(onClick = { viewModel.cancelReferenceMarketOrder(order.id) }) {
                                        Text("Cancel", color = CrimsonLight)
                                    }
                                }
                                HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.45f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExchangeAssetCard(
    name: String,
    symbol: String,
    category: String,
    price: Double,
    quantity: Double,
    costBasis: Double,
    market: String,
    onSubmit: (String, String, LocalOrderSide, LocalOrderKind, Double, Double?, Double?) -> Unit
) {
    var showTicket by remember { mutableStateOf(false) }
    var selectedSide by remember { mutableStateOf(LocalOrderSide.BUY) }

    Surface(
        color = DarkSurface,
        shape = RoundedCornerShape(17.dp),
        border = BorderStroke(1.dp, DarkCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(name, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(symbol, color = PurpleCrypto, style = MaterialTheme.typography.labelSmall)
                    Text(category, color = Color(0xFFAA99BA), style = MaterialTheme.typography.labelSmall)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(exchangeMoney(price), color = GoldLight, fontWeight = FontWeight.Bold)
                    if (quantity > 0.0) {
                        Text("Owned ${"%.5g".format(Locale.US, quantity)}", color = EmeraldLight, style = MaterialTheme.typography.labelSmall)
                        Text("Cost basis ${exchangeCompact(costBasis)}", color = Color(0xFFAA99BA), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { selectedSide = LocalOrderSide.BUY; showTicket = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = PurpleContainer, contentColor = GoldLight)
                ) { Text("Buy / Order") }
                OutlinedButton(
                    onClick = { selectedSide = LocalOrderSide.SELL; showTicket = true },
                    enabled = quantity > 0.0,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PurpleCrypto)
                ) { Text("Sell / Order") }
            }
        }
    }

    if (showTicket) {
        ExchangeTradeDialog(
            market = market,
            symbol = symbol,
            name = name,
            price = price,
            side = selectedSide,
            onDismiss = { showTicket = false },
            onSubmit = onSubmit
        )
    }
}

@Composable
private fun ExchangeTradeDialog(
    market: String,
    symbol: String,
    name: String,
    price: Double,
    side: LocalOrderSide,
    onDismiss: () -> Unit,
    onSubmit: (String, String, LocalOrderSide, LocalOrderKind, Double, Double?, Double?) -> Unit
) {
    var selectedSide by remember(side) { mutableStateOf(side) }
    var kind by remember { mutableStateOf(LocalOrderKind.MARKET) }
    var quantity by remember { mutableStateOf("1") }
    var trigger by remember { mutableStateOf("%.4f".format(Locale.US, price)) }
    val crypto = market == "crypto"
    val allowedKinds = if (crypto && selectedSide == LocalOrderSide.SELL) {
        listOf(LocalOrderKind.MARKET, LocalOrderKind.LIMIT, LocalOrderKind.STOP, LocalOrderKind.TAKE_PROFIT)
    } else listOf(LocalOrderKind.MARKET, LocalOrderKind.LIMIT)
    if (kind !in allowedKinds) kind = LocalOrderKind.MARKET
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = { Text("${if (selectedSide == LocalOrderSide.BUY) "Buy" else "Sell"} $name", color = GoldLight, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    FilterChip(selected = selectedSide == LocalOrderSide.BUY, onClick = { selectedSide = LocalOrderSide.BUY }, label = { Text("Buy") })
                    FilterChip(selected = selectedSide == LocalOrderSide.SELL, onClick = { selectedSide = LocalOrderSide.SELL }, label = { Text("Sell") })
                }
                Text("Current price: ${exchangeMoney(price)}", color = Color.White, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it.filter { ch -> ch.isDigit() || ch == '.' }.take(16) },
                    label = { Text(if (crypto) "Quantity" else "Whole shares") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = PurpleCrypto)
                )
                Text("Order type", color = Color(0xFFAA99BA), style = MaterialTheme.typography.labelSmall)
                allowedKinds.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { option ->
                            FilterChip(
                                selected = kind == option,
                                onClick = { kind = option },
                                label = { Text(option.name.replace('_',' ').lowercase().replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                }
                if (kind != LocalOrderKind.MARKET) {
                    OutlinedTextField(
                        value = trigger,
                        onValueChange = { trigger = it.filter { ch -> ch.isDigit() || ch == '.' }.take(18) },
                        label = { Text(if (kind == LocalOrderKind.LIMIT) "Limit price" else "Trigger price") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = PurpleCrypto)
                    )
                }
                Text(
                    when (kind) {
                        LocalOrderKind.MARKET -> "Executes against available simulated exchange liquidity immediately."
                        LocalOrderKind.LIMIT -> "Executes when the market reaches your limit price. Unfilled quantity remains reserved until filled or expired."
                        LocalOrderKind.STOP -> "Activates when the market reaches the stop price, then executes against available liquidity."
                        LocalOrderKind.TAKE_PROFIT -> "Activates when the market reaches your profit target, then executes against available liquidity."
                    },
                    color = Color(0xFFAA99BA), style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val qty = quantity.toDoubleOrNull() ?: 0.0
                val value = trigger.toDoubleOrNull()
                onSubmit(
                    market, symbol, selectedSide, kind, qty,
                    if (kind == LocalOrderKind.LIMIT) value else null,
                    if (kind == LocalOrderKind.STOP || kind == LocalOrderKind.TAKE_PROFIT) value else null
                )
                onDismiss()
            }) { Text("Submit order", color = GoldLight) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Color(0xFFAA99BA)) } }
    )
}
