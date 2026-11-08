package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.Crypto
import com.example.model.GameState
import com.example.model.MarketSentiment
import com.example.model.Stock
import com.example.ui.components.CurrencyFormatter
import com.example.ui.components.TycoonCard
import com.example.ui.theme.*

@Composable
fun MarketScreen(
    state: GameState,
    onBuyStock: (String, Long) -> Unit,
    onSellStock: (String, Long) -> Unit,
    onBuyCrypto: (String, Double) -> Unit,
    onSellCrypto: (String, Double) -> Unit,
    onPlaceMarketOrder: (String, String, String, String, Double, Double, Double) -> Unit = { _, _, _, _, _, _, _ -> },
    onCancelMarketOrder: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Stocks, 1: Crypto, 2: Orders
    var activeStockForTrade by remember { mutableStateOf<Stock?>(null) }
    var activeCryptoForTrade by remember { mutableStateOf<Crypto?>(null) }

    val totalStockVal = state.stocks.sumOf { it.holdingsValue }
    val totalStockPnl = state.stocks.sumOf { it.unrealizedPnl }

    val totalCryptoVal = state.cryptos.sumOf { it.holdingsValue }
    val totalCryptoPnl = state.cryptos.sumOf { it.unrealizedPnl }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Market Tabs (Stocks vs Crypto)
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = DarkSurface,
                    contentColor = GoldPrimary,
                    divider = {},
                    indicator = { tabPositions ->
                        // Standard M3 tab indicator
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                "STOCK EXCHANGE",
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == 0) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                "CRYPTO ASSETS",
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == 1) PurpleCrypto else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("ORDERS (" + state.marketOrders.count { it.status == "OPEN" || it.status == "PARTIAL" } + ")", fontWeight = FontWeight.Bold, color = if (selectedTab == 2) GoldLight else MaterialTheme.colorScheme.onSurfaceVariant) }
                    )
                }
            }

            // Portfolio Overview Card
            if (selectedTab != 2) item {
                val isStocks = selectedTab == 0
                val portVal = if (isStocks) totalStockVal else totalCryptoVal
                val portPnl = if (isStocks) totalStockPnl else totalCryptoPnl
                val themeColor = if (isStocks) GoldPrimary else PurpleCrypto

                TycoonCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = themeColor.copy(alpha = 0.4f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isStocks) "EQUITY PORTFOLIO" else "CRYPTO PORTFOLIO",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = CurrencyFormatter.format(portVal),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "TOTAL UNREALIZED P&L",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = (if (portPnl >= 0) "+$" else "-$") + CurrencyFormatter.format(Math.abs(portPnl)).removePrefix("$"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (portPnl >= 0) EmeraldProfit else CrimsonLoss
                            )
                        }
                    }
                }
            }

            if (selectedTab == 0) {
                // Stocks List
                items(state.stocks, key = { it.symbol }) { stock ->
                    StockCard(
                        stock = stock,
                        onTrade = { activeStockForTrade = stock }
                    )
                }
            } else if (selectedTab == 1) {
                // Crypto List
                items(state.cryptos, key = { it.symbol }) { crypto ->
                    CryptoCard(
                        crypto = crypto,
                        onTrade = { activeCryptoForTrade = crypto }
                    )
                }
            } else {
                item {
                    TycoonCard(Modifier.fillMaxWidth(), borderColor = PurpleCrypto.copy(alpha = 0.5f)) {
                        Text("OPEN & RECENT ORDERS", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Unfilled orders reserve cash or holdings until execution, cancellation or expiry.", color = Color(0xFFBDA8D2), style = MaterialTheme.typography.bodySmall)
                        Text("Reserved cash: " + CurrencyFormatter.format(state.reservedCash), color = GoldLight, fontWeight = FontWeight.Bold)
                    }
                }
                items(state.marketOrders.sortedByDescending { it.createdAt }, key = { it.id }) { order ->
                    TycoonCard(Modifier.fillMaxWidth(), borderColor = if (order.status == "OPEN" || order.status == "PARTIAL") CyanTech.copy(alpha = 0.5f) else DarkCardBorder) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(order.side + " " + order.symbol + " • " + order.kind, color = Color.White, fontWeight = FontWeight.Bold)
                                Text(order.market + " • " + order.status, color = CyanTech, style = MaterialTheme.typography.labelSmall)
                                Text("Filled " + order.filledQuantity + " / " + order.quantity + " • avg " + CurrencyFormatter.format(order.averageFillPrice), color = Color(0xFFBDA8D2), style = MaterialTheme.typography.bodySmall)
                                Text("Limit " + CurrencyFormatter.format(order.limitPrice) + " • Trigger " + CurrencyFormatter.format(order.triggerPrice), color = Color(0xFFBDA8D2), style = MaterialTheme.typography.bodySmall)
                            }
                            if (order.status == "OPEN" || order.status == "PARTIAL") {
                                TextButton(onClick = { onCancelMarketOrder(order.id) }) { Text("CANCEL", color = CrimsonLight) }
                            }
                        }
                    }
                }
            }
        }

        // Stock Trade Dialog
        activeStockForTrade?.let { stock ->
            val liveStock = state.stocks.find { it.symbol == stock.symbol } ?: stock
            StockTradeDialog(
                stock = liveStock,
                playerCash = state.cash,
                onDismiss = { activeStockForTrade = null },
                onBuy = { shares -> onBuyStock(liveStock.symbol, shares) },
                onSell = { shares -> onSellStock(liveStock.symbol, shares) },
                onPlaceOrder = { side, kind, quantity, price -> onPlaceMarketOrder("STOCK", liveStock.symbol, side, kind, quantity.toDouble(), if (kind == "LIMIT") price else 0.0, if (kind == "LIMIT") 0.0 else price) }
            )
        }

        // Crypto Trade Dialog
        activeCryptoForTrade?.let { crypto ->
            val liveCrypto = state.cryptos.find { it.symbol == crypto.symbol } ?: crypto
            CryptoTradeDialog(
                crypto = liveCrypto,
                playerCash = state.cash,
                onDismiss = { activeCryptoForTrade = null },
                onBuyUsd = { usd -> onBuyCrypto(liveCrypto.symbol, usd) },
                onSellPercent = { pct -> onSellCrypto(liveCrypto.symbol, pct) },
                onPlaceOrder = { side, kind, quantity, price -> onPlaceMarketOrder("CRYPTO", liveCrypto.symbol, side, kind, quantity, if (kind == "LIMIT") price else 0.0, if (kind == "LIMIT") 0.0 else price) }
            )
        }
    }
}

@Composable
fun StockCard(stock: Stock, onTrade: () -> Unit) {
    val isPositive = stock.changePercent >= 0

    TycoonCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (stock.sharesOwned > 0) GoldPrimary.copy(alpha = 0.3f) else DarkCardBorder
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stock.symbol,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Surface(
                        color = DarkSurfaceVariant,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = stock.sector,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = stock.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = CurrencyFormatter.format(stock.currentPrice),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = (if (isPositive) "+" else "") + String.format("%.2f%%", stock.changePercent),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isPositive) EmeraldProfit else CrimsonLoss
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Mini Sparkline Chart
        PriceSparkline(
            points = stock.priceHistory,
            color = if (isPositive) EmeraldProfit else CrimsonLoss,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (stock.sharesOwned > 0) {
                Column {
                    Text(
                        text = "${stock.sharesOwned} Shares (${CurrencyFormatter.format(stock.holdingsValue)})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = GoldLight
                    )
                    Text(
                        text = "Div. Yield: ${stock.dividendYieldPercent}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = EmeraldLight
                    )
                }
            } else {
                Text(
                    text = "Div. Yield: ${stock.dividendYieldPercent}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = EmeraldLight
                )
            }

            Button(
                onClick = onTrade,
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("trade_stock_${stock.symbol}")
            ) {
                Text("TRADE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CryptoCard(crypto: Crypto, onTrade: () -> Unit) {
    val isPositive = crypto.changePercent >= 0

    TycoonCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (crypto.coinsOwned > 0) PurpleCrypto.copy(alpha = 0.35f) else DarkCardBorder
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = crypto.symbol,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Surface(
                        color = PurpleContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = crypto.sentiment.title,
                            style = MaterialTheme.typography.labelSmall,
                            color = PurpleCrypto,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = crypto.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = CurrencyFormatter.format(crypto.currentPrice),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = (if (isPositive) "+" else "") + String.format("%.2f%%", crypto.changePercent),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isPositive) EmeraldProfit else CrimsonLoss
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Mini Sparkline Chart
        PriceSparkline(
            points = crypto.priceHistory,
            color = if (isPositive) EmeraldProfit else CrimsonLoss,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (crypto.coinsOwned > 0) {
                Column {
                    Text(
                        text = "${String.format("%.4f", crypto.coinsOwned)} ${crypto.symbol}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = PurpleCrypto
                    )
                    Text(
                        text = "Value: ${CurrencyFormatter.format(crypto.holdingsValue)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
            } else {
                Text(
                    text = "Volatility: ${(crypto.volatility * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onTrade,
                colors = ButtonDefaults.buttonColors(containerColor = PurpleCrypto, contentColor = Color.White),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("trade_crypto_${crypto.symbol}")
            ) {
                Text("TRADE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PriceSparkline(
    points: List<Double>,
    color: Color,
    modifier: Modifier = Modifier
) {
    if (points.size < 2) return

    Canvas(modifier = modifier) {
        val min = points.minOrNull() ?: 0.0
        val max = points.maxOrNull() ?: 1.0
        val range = if (max - min == 0.0) 1.0 else max - min

        val stepX = size.width / (points.size - 1)
        val strokePath = Path()
        val fillPath = Path()

        points.forEachIndexed { i, p ->
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

        // Draw gradient fill under curve
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(color.copy(alpha = 0.25f), Color.Transparent)
            )
        )

        // Draw stroke line
        drawPath(
            path = strokePath,
            color = color,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

@Composable
fun StockTradeDialog(
    stock: Stock,
    playerCash: Double,
    onDismiss: () -> Unit,
    onBuy: (Long) -> Unit,
    onSell: (Long) -> Unit,
    onPlaceOrder: (String, String, Long, Double) -> Unit = { _, _, _, _ -> }
) {
    var orderKind by remember { mutableStateOf("LIMIT") }
    var orderQuantity by remember { mutableStateOf("1") }
    var orderPrice by remember { mutableStateOf(stock.currentPrice.toString()) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = DarkSurface,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f)),
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Trade ${stock.name} (${stock.symbol})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Current Price: ${CurrencyFormatter.format(stock.currentPrice)} | Owned: ${stock.sharesOwned}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Divider(color = DarkCardBorder, modifier = Modifier.padding(vertical = 12.dp))

                Text("BUY SHARES", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = EmeraldProfit)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1L, 10L, 50L).forEach { qty ->
                        val cost = stock.currentPrice * qty
                        Button(
                            onClick = { onBuy(qty) },
                            enabled = playerCash >= cost,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldProfit, contentColor = Color.Black),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("+$qty")
                        }
                    }
                    val maxQty = (playerCash / stock.currentPrice).toLong()
                    Button(
                        onClick = { if (maxQty > 0) onBuy(maxQty) },
                        enabled = maxQty > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("MAX")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("SELL SHARES", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = CrimsonLight)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1L, 10L, 50L).forEach { qty ->
                        Button(
                            onClick = { onSell(qty) },
                            enabled = stock.sharesOwned >= qty,
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonLoss, contentColor = Color.White),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("-$qty")
                        }
                    }
                    Button(
                        onClick = { onSell(stock.sharesOwned) },
                        enabled = stock.sharesOwned > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonLight, contentColor = Color.Black),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("ALL")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Divider(color = DarkCardBorder, modifier = Modifier.padding(vertical = 10.dp))
                Text("ADVANCED ORDERS", color = GoldLight, fontWeight = FontWeight.Bold)
                TextButton(onClick = { orderKind = when (orderKind) { "LIMIT" -> "STOP"; "STOP" -> "TAKE_PROFIT"; else -> "LIMIT" } }) { Text("Order type: " + orderKind + "  •  CHANGE") }
                OutlinedTextField(orderQuantity, { orderQuantity = it.filter { ch -> ch.isDigit() } }, label = { Text("Share quantity") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(orderPrice, { orderPrice = it.filter { ch -> ch.isDigit() || ch == '.' } }, label = { Text(if (orderKind == "LIMIT") "Limit price" else "Trigger price") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { val q = orderQuantity.toLongOrNull() ?: 0L; val price = orderPrice.toDoubleOrNull() ?: 0.0; if (q > 0 && price > 0) onPlaceOrder("BUY", orderKind, q, price) }, enabled = (orderQuantity.toLongOrNull() ?: 0L) > 0 && (orderPrice.toDoubleOrNull() ?: 0.0) > 0.0, modifier = Modifier.weight(1f)) { Text("QUEUE BUY") }
                    Button(onClick = { val q = orderQuantity.toLongOrNull() ?: 0L; val price = orderPrice.toDoubleOrNull() ?: 0.0; if (q > 0 && price > 0) onPlaceOrder("SELL", orderKind, q, price) }, enabled = (orderQuantity.toLongOrNull() ?: 0L) > 0 && stock.sharesOwned >= (orderQuantity.toLongOrNull() ?: Long.MAX_VALUE) && (orderPrice.toDoubleOrNull() ?: 0.0) > 0.0, modifier = Modifier.weight(1f)) { Text("QUEUE SELL") }
                }
                OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("CLOSE") }
            }
        }
    }
}

@Composable
fun CryptoTradeDialog(
    crypto: Crypto,
    playerCash: Double,
    onDismiss: () -> Unit,
    onBuyUsd: (Double) -> Unit,
    onSellPercent: (Double) -> Unit,
    onPlaceOrder: (String, String, Double, Double) -> Unit = { _, _, _, _ -> }
) {
    var orderKind by remember { mutableStateOf("LIMIT") }
    var orderQuantity by remember { mutableStateOf("1") }
    var orderPrice by remember { mutableStateOf(crypto.currentPrice.toString()) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = DarkSurface,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, PurpleCrypto.copy(alpha = 0.5f)),
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Trade ${crypto.name} (${crypto.symbol})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Price: ${CurrencyFormatter.format(crypto.currentPrice)} | Holdings: ${String.format("%.4f", crypto.coinsOwned)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Divider(color = DarkCardBorder, modifier = Modifier.padding(vertical = 12.dp))

                Text("BUY USD AMOUNT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = PurpleCrypto)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(100.0, 500.0, 2500.0).forEach { usd ->
                        Button(
                            onClick = { onBuyUsd(usd) },
                            enabled = playerCash >= usd,
                            colors = ButtonDefaults.buttonColors(containerColor = PurpleCrypto, contentColor = Color.White),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("+$${usd.toInt()}")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("SELL HOLDINGS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = CrimsonLight)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0.25 to "25%", 0.50 to "50%", 1.0 to "100%").forEach { (pct, label) ->
                        Button(
                            onClick = { onSellPercent(pct) },
                            enabled = crypto.coinsOwned > 0,
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonLoss, contentColor = Color.White),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(label)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Divider(color = DarkCardBorder, modifier = Modifier.padding(vertical = 10.dp))
                Text("ADVANCED ORDERS", color = GoldLight, fontWeight = FontWeight.Bold)
                TextButton(onClick = { orderKind = when (orderKind) { "LIMIT" -> "STOP"; "STOP" -> "TAKE_PROFIT"; else -> "LIMIT" } }) { Text("Order type: " + orderKind + "  •  CHANGE") }
                OutlinedTextField(orderQuantity, { orderQuantity = it.filter { ch -> ch.isDigit() || ch == '.' } }, label = { Text("Coin quantity") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(orderPrice, { orderPrice = it.filter { ch -> ch.isDigit() || ch == '.' } }, label = { Text(if (orderKind == "LIMIT") "Limit price" else "Trigger price") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { val q = orderQuantity.toDoubleOrNull() ?: 0.0; val price = orderPrice.toDoubleOrNull() ?: 0.0; if (q > 0 && price > 0) onPlaceOrder("BUY", orderKind, q, price) }, enabled = (orderQuantity.toDoubleOrNull() ?: 0.0) > 0.0 && (orderPrice.toDoubleOrNull() ?: 0.0) > 0.0, modifier = Modifier.weight(1f)) { Text("QUEUE BUY") }
                    Button(onClick = { val q = orderQuantity.toDoubleOrNull() ?: 0.0; val price = orderPrice.toDoubleOrNull() ?: 0.0; if (q > 0 && price > 0) onPlaceOrder("SELL", orderKind, q, price) }, enabled = (orderQuantity.toDoubleOrNull() ?: 0.0) > 0.0 && crypto.coinsOwned >= (orderQuantity.toDoubleOrNull() ?: Double.MAX_VALUE) && (orderPrice.toDoubleOrNull() ?: 0.0) > 0.0, modifier = Modifier.weight(1f)) { Text("QUEUE SELL") }
                }
                OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("CLOSE") }
            }
        }
    }
}
