package com.example.engine

import android.content.Context
import java.util.Locale
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** Local exchange order book, reservations, partial fills, expiries and dividend settlement. */
object ReferenceMarketTrading {
    private const val ORDER_LIFETIME = 7L * 86_400_000L
    private const val MAX_TICKS_PER_EVAL = 600

    fun placeOrder(
        state: LocalSimulationState,
        context: Context,
        market: String,
        symbol: String,
        side: LocalOrderSide,
        kind: LocalOrderKind,
        quantityInput: Double,
        limitPrice: Double? = null,
        triggerPrice: Double? = null,
        now: Long
    ): LocalGameResult {
        val isStock = market.equals("stock", true) || market.equals("stocks", true)
        val normalizedMarket = if (isStock) "stock" else "crypto"
        val stock = if (isStock) ReferenceMarketSimulation.listStocks(now).firstOrNull { it.ticker == symbol } else null
        val crypto = if (!isStock) ReferenceMarketSimulation.listCrypto(now).firstOrNull { it.symbol == symbol } else null
        if (stock == null && crypto == null) return reject(state, "Unknown exchange asset")
        val requiredLevel = if (isStock) 2 else 4
        if (state.progress.level < requiredLevel) return reject(state, "${if (isStock) "Stock exchange" else "Crypto exchange"} unlocks at Player Level $requiredLevel")
        if (isStock && kind in setOf(LocalOrderKind.STOP, LocalOrderKind.TAKE_PROFIT)) {
            return reject(state, "Stop-loss and take-profit orders are only available for crypto")
        }
        if (kind in setOf(LocalOrderKind.STOP, LocalOrderKind.TAKE_PROFIT) && side != LocalOrderSide.SELL) {
            return reject(state, "Stop-loss and take-profit orders are sell-only")
        }
        val quantity = if (isStock) floor(quantityInput) else kotlin.math.round(quantityInput * 1e6) / 1e6
        if (!quantity.isFinite() || quantity <= 0.0) return reject(state, "Invalid order quantity")
        if (kind == LocalOrderKind.LIMIT && (limitPrice == null || limitPrice <= 0.0)) return reject(state, "Set a valid limit price")
        if (kind in setOf(LocalOrderKind.STOP, LocalOrderKind.TAKE_PROFIT) && (triggerPrice == null || triggerPrice <= 0.0)) {
            return reject(state, "Set a valid trigger price")
        }
        if (state.marketOrders.count { it.status in OPEN } >= 200) return reject(state, "Maximum 200 open market orders")
        val tick = if (isStock) ReferenceMarketSimulation.stockTick(now) else ReferenceMarketSimulation.cryptoTick(now)
        val price = stock?.let { ReferenceMarketSimulation.stockPriceAtTick(it, tick) }
            ?: ReferenceMarketSimulation.cryptoPriceAtTick(crypto!!, tick)
        val liquidity = stock?.let { ReferenceMarketSimulation.stockLiquidity(it, tick) }
            ?: ReferenceMarketSimulation.cryptoLiquidity(crypto!!, tick)
        val feeRate = ReferenceCompanyEngine.tradingFeeRate(state, context, normalizedMarket)
        val triggered = when (kind) {
            LocalOrderKind.MARKET -> true
            LocalOrderKind.LIMIT -> if (side == LocalOrderSide.BUY) price <= limitPrice!! else price >= limitPrice!!
            LocalOrderKind.STOP, LocalOrderKind.TAKE_PROFIT -> false
        }
        val id = "local-order-${state.nextSequence}"
        var order = LocalMarketOrder(
            id = id,
            symbol = symbol,
            market = normalizedMarket,
            side = side,
            kind = kind,
            quantity = quantity,
            limitPrice = limitPrice,
            triggerPrice = triggerPrice,
            createdAt = now,
            expiresAt = now + ORDER_LIFETIME,
            status = if (triggered && kind != LocalOrderKind.MARKET) LocalOrderStatus.TRIGGERED else LocalOrderStatus.OPEN
        )
        var next = state
        if (side == LocalOrderSide.BUY && kind != LocalOrderKind.MARKET) {
            val reservePrice = limitPrice ?: return reject(state, "Buy limit orders require a limit price")
            val reserve = quantity * reservePrice * (1.0 + feeRate)
            if (state.cash - state.reservedCash + 1e-9 < reserve) return reject(state, "Insufficient available funds")
            order = order.copy(reservedCash = reserve)
            next = next.copy(reservedCash = next.reservedCash + reserve)
        } else if (side == LocalOrderSide.SELL) {
            val holdings = if (isStock) next.stockHoldings else next.cryptoHoldings
            val holding = holdings[symbol] ?: return reject(state, "You do not own this asset")
            if (holding.quantity - holding.reservedQuantity + 1e-9 < quantity) {
                return reject(state, "Insufficient unreserved holdings")
            }
            if (kind != LocalOrderKind.MARKET) {
                val map = holdings + (symbol to holding.copy(reservedQuantity = holding.reservedQuantity + quantity))
                next = if (isStock) next.copy(stockHoldings = map) else next.copy(cryptoHoldings = map)
            }
        }
        next = next.copy(marketOrders = (next.marketOrders + order).takeLast(200), nextSequence = next.nextSequence + 1)
        if (kind == LocalOrderKind.MARKET || triggered) {
            val fillQuantity = min(quantity, liquidity)
            if (fillQuantity <= 1e-12) return reject(state, "No exchange liquidity available")
            if (side == LocalOrderSide.BUY && kind == LocalOrderKind.MARKET &&
                next.cash - next.reservedCash + 1e-9 < fillQuantity * price * (1.0 + feeRate)) {
                return reject(state, "Insufficient available funds")
            }
            val live = next.marketOrders.first { it.id == id }
            when (val fill = fill(next, live, fillQuantity, price, feeRate, now)) {
                is LocalGameResult.Rejected -> return fill
                is LocalGameResult.Success -> next = fill.state
            }
            val filledOrder = next.marketOrders.first { it.id == id }
            if (kind == LocalOrderKind.MARKET && filledOrder.status != LocalOrderStatus.EXECUTED) {
                next = closeOrder(next, filledOrder, LocalOrderStatus.CANCELLED)
            }
            return success(next, if (fillQuantity + 1e-9 >= quantity) "Order executed" else "Partial fill; unfilled market quantity cancelled")
        }
        return success(record(next, now, "order_placed", "Placed $normalizedMarket $kind $side order for $symbol", 0.0), "Order placed")
    }

    fun cancelOrder(state: LocalSimulationState, orderId: String, now: Long): LocalGameResult {
        val order = state.marketOrders.firstOrNull { it.id == orderId } ?: return reject(state, "Order not found")
        if (order.status !in OPEN) return reject(state, "Order is no longer open")
        val next = closeOrder(state, order, LocalOrderStatus.CANCELLED)
        return success(record(next, now, "order_cancel", "Cancelled order for ${order.symbol}", 0.0), "Order cancelled")
    }

    fun tick(state: LocalSimulationState, context: Context, now: Long): LocalSimulationState {
        var next = evaluate(state, context, true, now)
        next = evaluate(next, context, false, now)
        return payDividends(next, now)
    }

    private fun evaluate(
        input: LocalSimulationState,
        context: Context,
        stockMarket: Boolean,
        now: Long
    ): LocalSimulationState {
        val market = if (stockMarket) "stock" else "crypto"
        val currentTick = if (stockMarket) ReferenceMarketSimulation.stockTick(now) else ReferenceMarketSimulation.cryptoTick(now)
        val lastTick = if (stockMarket) input.stockLastEvalTick else input.cryptoLastEvalTick
        val relevant = input.marketOrders.any { it.market == market && it.status in OPEN }
        var state = input
        if (relevant) {
            val from = max(lastTick + 1L, currentTick - MAX_TICKS_PER_EVAL)
            val feeRate = ReferenceCompanyEngine.tradingFeeRate(input, context, market)
            val stocks = if (stockMarket) ReferenceMarketSimulation.listStocks(now).associateBy { it.ticker } else emptyMap()
            val cryptos = if (!stockMarket) ReferenceMarketSimulation.listCrypto(now).associateBy { it.symbol } else emptyMap()
            for (tick in from..currentTick) {
                val tickTime = if (stockMarket) ReferenceMarketSimulation.stockTickTime(tick) else ReferenceMarketSimulation.cryptoTickTime(tick)
                val openOrders = state.marketOrders.filter { it.market == market && it.status in OPEN }
                for (snapshot in openOrders) {
                    var order = state.marketOrders.firstOrNull { it.id == snapshot.id } ?: continue
                    if (order.status !in OPEN) continue
                    if (tickTime >= order.expiresAt) {
                        state = closeOrder(state, order, LocalOrderStatus.EXPIRED)
                        continue
                    }
                    if (tickTime < order.createdAt) continue
                    val stock = if (stockMarket) stocks[order.symbol] else null
                    val crypto = if (!stockMarket) cryptos[order.symbol] else null
                    if (stock == null && crypto == null) continue
                    val price = stock?.let { ReferenceMarketSimulation.stockPriceAtTick(it, tick) }
                        ?: ReferenceMarketSimulation.cryptoPriceAtTick(crypto!!, tick)
                    val liquidity = stock?.let { ReferenceMarketSimulation.stockLiquidity(it, tick) }
                        ?: ReferenceMarketSimulation.cryptoLiquidity(crypto!!, tick)
                    var shouldFill = false
                    when (order.kind) {
                        LocalOrderKind.MARKET -> shouldFill = true
                        LocalOrderKind.LIMIT -> shouldFill = if (order.side == LocalOrderSide.BUY) {
                            price <= (order.limitPrice ?: Double.NEGATIVE_INFINITY)
                        } else price >= (order.limitPrice ?: Double.POSITIVE_INFINITY)
                        LocalOrderKind.STOP -> {
                            if (order.status == LocalOrderStatus.OPEN && price <= (order.triggerPrice ?: Double.NEGATIVE_INFINITY)) {
                                state = updateOrder(state, order.id) { it.copy(status = LocalOrderStatus.TRIGGERED) }
                                order = state.marketOrders.first { it.id == order.id }
                            }
                            shouldFill = order.status == LocalOrderStatus.TRIGGERED
                        }
                        LocalOrderKind.TAKE_PROFIT -> {
                            if (order.status == LocalOrderStatus.OPEN && price >= (order.triggerPrice ?: Double.POSITIVE_INFINITY)) {
                                state = updateOrder(state, order.id) { it.copy(status = LocalOrderStatus.TRIGGERED) }
                                order = state.marketOrders.first { it.id == order.id }
                            }
                            shouldFill = order.status == LocalOrderStatus.TRIGGERED
                        }
                    }
                    if (!shouldFill) continue
                    val quantity = min(order.quantity - order.filledQuantity, liquidity)
                    if (quantity <= 1e-12) continue
                    when (val result = fill(state, order, quantity, price, feeRate, tickTime)) {
                        is LocalGameResult.Success -> state = result.state
                        is LocalGameResult.Rejected -> Unit
                    }
                }
            }
        }
        return if (stockMarket) state.copy(stockLastEvalTick = currentTick) else state.copy(cryptoLastEvalTick = currentTick)
    }

    private fun fill(
        input: LocalSimulationState,
        orderInput: LocalMarketOrder,
        quantityInput: Double,
        price: Double,
        feeRate: Double,
        now: Long
    ): LocalGameResult {
        val order = input.marketOrders.firstOrNull { it.id == orderInput.id } ?: return reject(input, "Order no longer exists")
        val quantity = min(quantityInput, order.quantity - order.filledQuantity)
        if (quantity <= 1e-12) return reject(input, "Nothing remains to fill")
        val gross = quantity * price
        val fee = gross * feeRate
        val isBuy = order.side == LocalOrderSide.BUY
        var cash = input.cash
        var reservedCash = input.reservedCash
        var stockHoldings = input.stockHoldings
        var cryptoHoldings = input.cryptoHoldings
        val holdings = if (order.market == "stock") stockHoldings else cryptoHoldings
        val old = holdings[order.symbol] ?: LocalHolding()
        val neededReserve = if (isBuy && order.kind != LocalOrderKind.MARKET) {
            min(order.reservedCash, quantity * (order.limitPrice ?: price) * (1.0 + feeRate))
        } else 0.0
        var nextHolding = old
        if (isBuy) {
            if (order.kind == LocalOrderKind.MARKET) {
                if (cash - reservedCash + 1e-9 < gross + fee) return reject(input, "Insufficient available funds")
                cash -= gross + fee
            } else {
                if (order.reservedCash + 1e-9 < neededReserve || reservedCash + 1e-9 < neededReserve) {
                    return reject(input, "Reserved funds are unavailable")
                }
                reservedCash = max(0.0, reservedCash - neededReserve)
                cash -= gross + fee
            }
            nextHolding = old.copy(quantity = old.quantity + quantity, costBasis = old.costBasis + gross + fee)
        } else {
            val requiredReserved = if (order.kind == LocalOrderKind.MARKET) 0.0 else quantity
            if (old.quantity + 1e-9 < quantity || old.reservedQuantity + 1e-9 < requiredReserved) {
                return reject(input, "Reserved holdings are unavailable")
            }
            val averageCost = if (old.quantity > 0.0) old.costBasis / old.quantity else 0.0
            nextHolding = old.copy(
                quantity = max(0.0, old.quantity - quantity),
                reservedQuantity = max(0.0, old.reservedQuantity - requiredReserved),
                costBasis = max(0.0, old.costBasis - averageCost * quantity)
            )
            cash += gross - fee
        }
        val map = if (nextHolding.quantity <= 1e-9) holdings - order.symbol else holdings + (order.symbol to nextHolding)
        if (order.market == "stock") stockHoldings = map else cryptoHoldings = map
        val filled = order.filledQuantity + quantity
        val average = if (filled <= 0.0) price else (order.averageFillPrice * order.filledQuantity + price * quantity) / filled
        val remainingReserve = if (isBuy) max(0.0, order.reservedCash - neededReserve) else 0.0
        val updatedOrder = order.copy(
            filledQuantity = filled,
            averageFillPrice = average,
            feePaid = order.feePaid + fee,
            reservedCash = remainingReserve,
            status = if (filled + 1e-9 >= order.quantity) LocalOrderStatus.EXECUTED else LocalOrderStatus.PARTIALLY_FILLED
        )
        var next = input.copy(
            cash = cash,
            reservedCash = reservedCash,
            stockHoldings = stockHoldings,
            cryptoHoldings = cryptoHoldings,
            marketOrders = input.marketOrders.map { if (it.id == order.id) updatedOrder else it },
            progress = if (order.market == "stock") input.progress.copy(stockTrades = input.progress.stockTrades + 1)
                else input.progress.copy(cryptoTrades = input.progress.cryptoTrades + 1)
        )
        if (updatedOrder.status == LocalOrderStatus.EXECUTED && remainingReserve > 1e-9) {
            next = next.copy(reservedCash = max(0.0, next.reservedCash - remainingReserve))
            next = updateOrder(next, order.id) { it.copy(reservedCash = 0.0) }
        }
        val amount = if (isBuy) -(gross + fee) else gross - fee
        return success(record(next, now, if (isBuy) "${order.market}_buy" else "${order.market}_sell",
            "${if (isBuy) "Bought" else "Sold"} ${formatQuantity(quantity)} ${order.symbol} @ %.2f".format(Locale.US, price), amount),
            "Order filled")
    }

    private fun payDividends(input: LocalSimulationState, now: Long): LocalSimulationState {
        val today = Math.floorDiv(now, ReferenceMarketSimulation.DAY_MS)
        if (input.lastDividendDay <= 0L) return input.copy(lastDividendDay = today)
        val start = max(input.lastDividendDay + 1L, today - 30L)
        var total = 0.0
        val definitions = ReferenceMarketSimulation.listStocks(now).associateBy { it.ticker }
        for (day in start..today) {
            for ((symbol, holding) in input.stockHoldings) {
                val stock = definitions[symbol] ?: continue
                if (stock.dividendYield <= 0.0 || !ReferenceMarketSimulation.stockProfitable(stock, day)) continue
                val price = ReferenceMarketSimulation.stockPriceAtTick(stock, ReferenceMarketSimulation.stockTick(day * ReferenceMarketSimulation.DAY_MS))
                total += (holding.quantity * price * stock.dividendYield) / 365.0
            }
        }
        if (total <= 0.0) return input.copy(lastDividendDay = today)
        val next = input.copy(
            cash = input.cash + total,
            lastDividendDay = today,
            progress = input.progress.copy(
                dividends = input.progress.dividends + total,
                lifetimeRevenue = input.progress.lifetimeRevenue + total,
                lifetimeCashEarned = input.progress.lifetimeCashEarned + total
            )
        )
        return record(next, now, "dividend", "Received stock dividends", total)
    }

    private fun closeOrder(state: LocalSimulationState, order: LocalMarketOrder, status: LocalOrderStatus): LocalSimulationState {
        var next = state
        if (order.side == LocalOrderSide.BUY && order.reservedCash > 0.0) {
            next = next.copy(reservedCash = max(0.0, next.reservedCash - order.reservedCash))
        } else if (order.side == LocalOrderSide.SELL && order.kind != LocalOrderKind.MARKET) {
            val holdings = if (order.market == "stock") next.stockHoldings else next.cryptoHoldings
            val current = holdings[order.symbol]
            if (current != null) {
                val remaining = max(0.0, order.quantity - order.filledQuantity)
                val updated = current.copy(reservedQuantity = max(0.0, current.reservedQuantity - remaining))
                val map = holdings + (order.symbol to updated)
                next = if (order.market == "stock") next.copy(stockHoldings = map) else next.copy(cryptoHoldings = map)
            }
        }
        return updateOrder(next, order.id) { it.copy(reservedCash = 0.0, status = status) }
    }

    private fun updateOrder(state: LocalSimulationState, id: String, transform: (LocalMarketOrder) -> LocalMarketOrder) =
        state.copy(marketOrders = state.marketOrders.map { if (it.id == id) transform(it) else it })

    private fun record(state: LocalSimulationState, now: Long, category: String, description: String, amount: Double): LocalSimulationState {
        val entry = LocalLedgerEntry("local-${state.nextSequence}", now, category, description, amount, state.cash)
        return state.copy(
            ledger = (state.ledger + entry).takeLast(5000),
            nextSequence = state.nextSequence + 1L
        )
    }

    private fun formatQuantity(quantity: Double): String =
        if (quantity >= 1.0) "%.2f".format(Locale.US, quantity) else "%.5g".format(Locale.US, quantity)

    private val OPEN = setOf(LocalOrderStatus.OPEN, LocalOrderStatus.TRIGGERED, LocalOrderStatus.PARTIALLY_FILLED)
    private fun success(state: LocalSimulationState, message: String) = LocalGameResult.Success(state, message)
    private fun reject(state: LocalSimulationState, message: String) = LocalGameResult.Rejected(state, message)
}
