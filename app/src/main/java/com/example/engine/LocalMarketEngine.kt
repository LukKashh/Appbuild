package com.example.engine

import com.example.model.*
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

/** Offline exchange simulator: orders, escrow, partial liquidity fills, fees and expiries. */
object LocalMarketEngine {
    private const val STOCK_FEE = 0.005
    private const val CRYPTO_FEE = 0.0025
    private const val ORDER_TTL = 7L * 24L * 60L * 60L * 1000L
    private fun fee(state: GameState, market: String): Double {
        val base = if (market == "STOCK") STOCK_FEE else CRYPTO_FEE
        val discount = ((state.corporation.facilities["trading"] ?: 0) * 0.02).coerceIn(0.0, 0.50)
        return base * (1.0 - discount)
    }
    private fun price(s: GameState, market: String, symbol: String): Double =
        if (market == "STOCK") s.stocks.firstOrNull { it.symbol == symbol }?.currentPrice ?: 0.0
        else s.cryptos.firstOrNull { it.symbol == symbol }?.currentPrice ?: 0.0

    fun place(s: GameState, market: String, symbol: String, side: String, kind: String, quantity: Double, limitPrice: Double = 0.0, triggerPrice: Double = 0.0, now: Long = System.currentTimeMillis()): GameState {
        require(market == "STOCK" || market == "CRYPTO")
        require(side == "BUY" || side == "SELL")
        require(kind in listOf("MARKET", "LIMIT", "STOP", "TAKE_PROFIT"))
        require(quantity > 0.0 && quantity.isFinite())
        require(s.marketOrders.count { it.status == "OPEN" || it.status == "PARTIAL" } < 100)
        val current = price(s, market, symbol)
        require(current > 0.0)
        val qty = if (market == "STOCK") quantity.toLong().toDouble() else quantity
        require(qty > 0.0)
        if (side == "SELL") require(available(s, market, symbol) >= qty) { "Insufficient available holdings" }
        val reference = when { limitPrice > 0 -> limitPrice; triggerPrice > 0 -> triggerPrice; else -> current }
        val reserve = if (side == "BUY" && kind != "MARKET") qty * reference * (1.0 + fee(s, market)) else 0.0
        require(s.cash >= reserve) { "Insufficient cash to reserve this order" }
        val nowOrder = LocalMarketOrder(UUID.randomUUID().toString(), market, symbol, side, kind, qty,
            limitPrice = limitPrice, triggerPrice = triggerPrice, createdAt = now, updatedAt = now, expiresAt = now + ORDER_TTL,
            reservedCash = reserve, reservedQuantity = if (side == "SELL" && kind != "MARKET") qty else 0.0)
        var n = s.copy(cash = s.cash - reserve, reservedCash = s.reservedCash + reserve, marketOrders = s.marketOrders + nowOrder)
        if (side == "SELL" && market == "STOCK" && kind != "MARKET") n = n.copy(stocks = n.stocks.map { if (it.symbol == symbol) it.copy(reservedShares = it.reservedShares + qty.toLong()) else it })
        if (side == "SELL" && market == "CRYPTO" && kind != "MARKET") n = n.copy(cryptos = n.cryptos.map { if (it.symbol == symbol) it.copy(reservedCoins = it.reservedCoins + qty) else it })
        return if (kind == "MARKET") fill(n, nowOrder.id, qty, current, now) else advance(n, now)
    }

    fun cancel(s: GameState, id: String, now: Long = System.currentTimeMillis()): GameState {
        val order = s.marketOrders.firstOrNull { it.id == id } ?: return s
        if (order.status != "OPEN" && order.status != "PARTIAL") return s
        val stock = if (order.market == "STOCK" && order.side == "SELL") s.stocks.map { if (it.symbol == order.symbol) it.copy(reservedShares = (it.reservedShares - order.reservedQuantity.toLong()).coerceAtLeast(0)) else it } else s.stocks
        val crypto = if (order.market == "CRYPTO" && order.side == "SELL") s.cryptos.map { if (it.symbol == order.symbol) it.copy(reservedCoins = (it.reservedCoins - order.reservedQuantity).coerceAtLeast(0.0)) else it } else s.cryptos
        return s.copy(cash = s.cash + order.reservedCash, reservedCash = (s.reservedCash - order.reservedCash).coerceAtLeast(0.0), stocks = stock, cryptos = crypto,
            marketOrders = s.marketOrders.map { if (it.id == id) it.copy(status = "CANCELLED", reservedCash = 0.0, reservedQuantity = 0.0, updatedAt = now) else it })
    }

    fun advance(input: GameState, now: Long): GameState {
        var s = input
        val open = s.marketOrders.filter { it.status == "OPEN" || it.status == "PARTIAL" }
        open.forEach { order ->
            if (now >= order.expiresAt) {
                s = cancel(s, order.id, now)
                s = s.copy(marketOrders = s.marketOrders.map { if (it.id == order.id) it.copy(status = "EXPIRED", updatedAt = now) else it })
            } else {
                val p = price(s, order.market, order.symbol)
                val triggered = when (order.kind) {
                    "LIMIT" -> if (order.side == "BUY") p <= order.limitPrice else p >= order.limitPrice
                    "STOP" -> if (order.side == "BUY") p >= order.triggerPrice else p <= order.triggerPrice
                    "TAKE_PROFIT" -> if (order.side == "BUY") p <= order.triggerPrice else p >= order.triggerPrice
                    else -> true
                }
                if (triggered) {
                    val maxFill = if (order.market == "STOCK") 250.0 else 25000.0
                    val remaining = (order.quantity - order.filledQuantity).coerceAtLeast(0.0)
                    val quantity = min(remaining, maxFill)
                    if (quantity > 0.0) s = fill(s, order.id, quantity, p, now)
                }
            }
        }
        return s.copy(marketOrders = s.marketOrders.takeLast(200))
    }

    private fun available(s: GameState, market: String, symbol: String): Double =
        if (market == "STOCK") s.stocks.firstOrNull { it.symbol == symbol }?.let { (it.sharesOwned - it.reservedShares).coerceAtLeast(0).toDouble() } ?: 0.0
        else s.cryptos.firstOrNull { it.symbol == symbol }?.let { (it.coinsOwned - it.reservedCoins).coerceAtLeast(0.0) } ?: 0.0

    private fun fill(input: GameState, id: String, rawQty: Double, executionPrice: Double, now: Long): GameState {
        val index = input.marketOrders.indexOfFirst { it.id == id }
        if (index < 0) return input
        val orders = input.marketOrders.toMutableList()
        val order = orders[index]
        if (order.status != "OPEN" && order.status != "PARTIAL") return input
        val remaining = (order.quantity - order.filledQuantity).coerceAtLeast(0.0)
        val qty = min(remaining, if (order.market == "STOCK") rawQty.toLong().toDouble() else rawQty)
        if (qty <= 0.0) return input
        val gross = qty * executionPrice
        val feeAmount = gross * fee(input, order.market)
        var cash = input.cash
        var reservedCash = input.reservedCash
        var orderReserve = order.reservedCash
        var stocks = input.stocks
        var cryptos = input.cryptos
        if (order.side == "BUY") {
            val debit = gross + feeAmount
            if (order.kind == "MARKET") { if (cash < debit) return input else cash -= debit }
            else {
                val remainingBefore = (order.quantity - order.filledQuantity).coerceAtLeast(0.0000001)
                val portion = min(orderReserve, orderReserve * qty / remainingBefore)
                if (portion < debit && cash < debit - portion) return input
                cash -= max(0.0, debit - portion)
                cash += max(0.0, portion - debit)
                orderReserve -= portion
                reservedCash = (reservedCash - portion).coerceAtLeast(0.0)
            }
            if (order.market == "STOCK") stocks = stocks.map { if (it.symbol == order.symbol) it.copy(sharesOwned = it.sharesOwned + qty.toLong(), totalCostBasis = it.totalCostBasis + debit) else it }
            else cryptos = cryptos.map { if (it.symbol == order.symbol) it.copy(coinsOwned = it.coinsOwned + qty, totalCostBasis = it.totalCostBasis + debit) else it }
        } else {
            if (order.market == "STOCK") {
                val held = stocks.firstOrNull { it.symbol == order.symbol } ?: return input
                if (held.sharesOwned < qty.toLong()) return input
                val basis = if (held.sharesOwned > 0) held.totalCostBasis * qty / held.sharesOwned else 0.0
                stocks = stocks.map { if (it.symbol == order.symbol) it.copy(sharesOwned = it.sharesOwned - qty.toLong(), totalCostBasis = (it.totalCostBasis - basis).coerceAtLeast(0.0), reservedShares = (it.reservedShares - if (order.kind == "MARKET") 0L else qty.toLong()).coerceAtLeast(0L)) else it }
            } else {
                val held = cryptos.firstOrNull { it.symbol == order.symbol } ?: return input
                if (held.coinsOwned + 1e-9 < qty) return input
                val basis = if (held.coinsOwned > 0) held.totalCostBasis * qty / held.coinsOwned else 0.0
                cryptos = cryptos.map { if (it.symbol == order.symbol) it.copy(coinsOwned = it.coinsOwned - qty, totalCostBasis = (it.totalCostBasis - basis).coerceAtLeast(0.0), reservedCoins = (it.reservedCoins - if (order.kind == "MARKET") 0.0 else qty).coerceAtLeast(0.0)) else it }
            }
            cash += gross - feeAmount
        }
        val filled = (order.filledQuantity + qty).coerceAtMost(order.quantity)
        val avg = (order.averageFillPrice * order.filledQuantity + executionPrice * qty) / filled
        val complete = filled + 1e-8 >= order.quantity
        if (complete && order.side == "BUY" && orderReserve > 0.0) { cash += orderReserve; reservedCash = (reservedCash - orderReserve).coerceAtLeast(0.0); orderReserve = 0.0 }
        orders[index] = order.copy(filledQuantity = filled, averageFillPrice = avg, feesPaid = order.feesPaid + feeAmount,
            reservedCash = orderReserve, reservedQuantity = (order.reservedQuantity - if (order.kind == "MARKET") 0.0 else qty).coerceAtLeast(0.0),
            status = if (complete) "EXECUTED" else "PARTIAL", updatedAt = now)
        val type = if (order.market == "STOCK") TransactionType.STOCKS else TransactionType.CRYPTO
        val amount = if (order.side == "BUY") -(gross + feeAmount) else gross - feeAmount
        val tx = Transaction(UUID.randomUUID().toString(), now, type, order.side + " " + qty + " " + order.symbol + " " + order.kind, amount)
        val ledger = LocalLedgerEntry(UUID.randomUUID().toString(), now, "market", tx.description, debit = if (amount < 0) -amount else 0.0, credit = if (amount > 0) amount else 0.0, balanceAfter = cash)
        return input.copy(cash = cash, reservedCash = reservedCash, stocks = stocks, cryptos = cryptos, marketOrders = orders,
            transactions = (listOf(tx) + input.transactions).take(200), ledger = (listOf(ledger) + input.ledger).take(500))
    }
}