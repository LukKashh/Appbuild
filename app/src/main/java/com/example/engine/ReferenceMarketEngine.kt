package com.example.engine

import android.content.Context
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.random.Random

data class ReferenceStockDefinition(
    val ticker: String,
    val name: String,
    val sector: String,
    val base: Double,
    val volatility: Double,
    val drift: Double,
    val shares: Double,
    val dividendYield: Double,
    val margin: Double,
    val listedAt: Long
)

data class ReferenceCryptoDefinition(
    val symbol: String,
    val name: String,
    val base: Double,
    val profile: String,
    val supplyModel: String,
    val circulating: Double,
    val maximumSupply: Double?,
    val inflation: Double,
    val burn: Double,
    val listedAt: Long
)

data class ReferenceCryptoSupply(val circulating: Double, val burned: Double, val maximum: Double?)

/** Deterministic exchanges ported from webapp game/market.ts and engine.ts. */
object ReferenceMarketEngine {
    private const val EPOCH = 1_790_812_800_000L // 2026-10-01T00:00:00Z
    private const val DAY = 86_400_000L
    private const val STOCK_TICK_MS = 5 * 60_000L
    private const val CRYPTO_TICK_MS = 2 * 60_000L
    private const val MAX_TICKS_PER_EVAL = 600
    private const val ORDER_EXPIRY_DAYS = 7L
    private const val MIN_CRYPTO_PRICE = 0.0001
    private val sectors = listOf(
        "Technology", "Finance", "Energy", "Healthcare", "Manufacturing", "Consumer Goods",
        "Retail", "Transportation", "Real Estate", "Mining", "Entertainment", "Utilities"
    )
    private val stockPrefixes = listOf(
        "Apex","Blue","Cobalt","Delta","Ember","Falcon","Granite","Horizon","Ionic","Jade",
        "Kestrel","Lumen","Monarch","Nimbus","Onyx","Pioneer","Quartz","Redwood","Summit","Titan",
        "Unity","Vertex","Willow","Zenith"
    )
    private val stockSuffixes = listOf(
        "Systems","Group","Industries","Partners","Works","Corp","Holdings","Labs","Networks","Ventures"
    )
    private val cryptoPrefixes = listOf(
        "Astra","Byte","Chrono","Dash","Echo","Flux","Glyph","Hyper","Ion","Krypt",
        "Luna","Mint","Neo","Orbit","Pulse","Rune","Shard","Terra","Vox","Zeta"
    )
    private val cryptoSuffixes = listOf("coin","chain","swap","verse","net","fi","x","dao")

    private val curatedStocks = listOf(
        stock("NOVA","Nova Dynamics","Technology",182.0,0.025,0.12,2.1e9,0.012),
        stock("QBIT","Qubit Labs","Technology",64.0,0.04,0.2,9e8,0.0),
        stock("MRDN","Meridian Holdings","Finance",92.0,0.015,0.06,1.4e9,0.034),
        stock("CRSC","Crescent Bank","Finance",41.0,0.018,0.05,2.6e9,0.042),
        stock("HELX","Helix Energy","Energy",58.0,0.022,0.04,1.8e9,0.051),
        stock("SOLR","Solaris Renewables","Energy",27.0,0.035,0.15,1.1e9,0.008),
        stock("VITA","Vitalis Health","Healthcare",134.0,0.017,0.08,1.3e9,0.022),
        stock("GENX","GeneX Therapeutics","Healthcare",22.0,0.05,0.1,6e8,0.0),
        stock("IRON","Ironclad Industries","Manufacturing",76.0,0.02,0.05,1.2e9,0.028),
        stock("PURE","PureHome Brands","Consumer Goods",110.0,0.012,0.05,1.6e9,0.031),
        stock("MRKT","MarketSquare Retail","Retail",48.0,0.02,0.03,1.9e9,0.025),
        stock("SWFT","SwiftAir Transport","Transportation",35.0,0.028,0.04,1e9,0.012),
        stock("URBN","Urbana REIT","Real Estate",63.0,0.014,0.04,1.5e9,0.058),
        stock("ORE","Deepcore Mining","Mining",19.0,0.033,0.03,2e9,0.035),
        stock("STAR","Starlight Media","Entertainment",88.0,0.026,0.09,1e9,0.006),
        stock("GRID","Gridline Utilities","Utilities",52.0,0.01,0.03,2.2e9,0.047)
    )

    private val curatedCrypto = listOf(
        ReferenceCryptoDefinition("BTX","Bitex",42_000.0,"moderate","fixed",19.5e6,21e6,0.017,0.0,EPOCH),
        ReferenceCryptoDefinition("ETR","Etheron",2_400.0,"high","burn",120e6,null,0.01,0.012,EPOCH),
        ReferenceCryptoDefinition("SOLA","Solara",95.0,"very high","inflationary",430e6,null,0.05,0.0,EPOCH),
        ReferenceCryptoDefinition("STB","StableBit",1.0,"low","inflationary",80e9,null,0.1,0.0,EPOCH),
        ReferenceCryptoDefinition("DOGO","Dogo",0.12,"extreme","inflationary",140e9,null,0.04,0.0,EPOCH),
        ReferenceCryptoDefinition("LNK","Linkora",14.0,"high","fixed",550e6,1e9,0.03,0.0,EPOCH),
        ReferenceCryptoDefinition("AVX","Avalux",34.0,"very high","fixed",370e6,720e6,0.04,0.0,EPOCH),
        ReferenceCryptoDefinition("FLR","Flare Grid",0.8,"extreme","burn",2e9,5e9,0.0,0.02,EPOCH)
    )

    private fun stock(
        ticker: String, name: String, sector: String, base: Double,
        volatility: Double, drift: Double, shares: Double, dividendYield: Double
    ): ReferenceStockDefinition {
        val margin = 0.08 + (hash(ticker).toUInt().toLong() % 20L) / 100.0
        return ReferenceStockDefinition(ticker,name,sector,base,volatility,drift,shares,dividendYield,margin,EPOCH)
    }

    fun listStocks(now: Long): List<ReferenceStockDefinition> {
        val count = max(0L, Math.floorDiv(now - EPOCH, 3L * DAY)).coerceAtMost(60L).toInt()
        val result = curatedStocks.toMutableList()
        val seen = result.map { it.ticker }.toMutableSet()
        repeat(count) { index ->
            val random = mulberry(hash("stock-list-$index"))
            val displayName = "${stockPrefixes[(random() * stockPrefixes.size).toInt()]} ${stockSuffixes[(random() * stockSuffixes.size).toInt()]}"
            val tickerBase = displayName.filter { it in 'A'..'Z' }.take(3) + ('A'.code + index % 26).toChar()
            var ticker = tickerBase
            if (ticker in seen) ticker += index.toString()
            seen += ticker
            val shares = ((2.0 + random() * 15.0) * 1e8).roundToLong().toDouble()
            val yield = if (random() < 0.5) 0.0 else kotlin.math.round(random() * 50.0) / 1000.0
            result += ReferenceStockDefinition(
                ticker = ticker,
                name = "$displayName ${index + 1}",
                sector = sectors[(random() * sectors.size).toInt()],
                base = kotlin.math.round((5.0 + random() * 120.0) * 100.0) / 100.0,
                volatility = 0.02 + random() * 0.03,
                drift = -0.02 + random() * 0.15,
                shares = shares,
                dividendYield = yield,
                margin = -0.05 + random() * 0.25,
                listedAt = EPOCH + (index + 1) * 3L * DAY
            )
        }
        return result
    }

    fun listCrypto(now: Long): List<ReferenceCryptoDefinition> {
        val count = max(0L, Math.floorDiv(now - EPOCH, 7L * DAY)).coerceAtMost(40L).toInt()
        val result = curatedCrypto.toMutableList()
        val seenSymbols = result.map { it.symbol }.toMutableSet()
        val seenNames = result.map { it.name }.toMutableSet()
        repeat(count) { index ->
            val random = mulberry(hash("crypto-list-$index"))
            var name = cryptoPrefixes[(random() * cryptoPrefixes.size).toInt()] +
                cryptoSuffixes[(random() * cryptoSuffixes.size).toInt()]
            val symbolBase = (name.take(3) + ('A'.code + index % 26).toChar()).uppercase(Locale.US)
            var symbol = symbolBase
            while (symbol in seenSymbols) symbol += "X"
            if (name in seenNames) name += " ${index + 1}"
            seenSymbols += symbol
            seenNames += name
            val profile = listOf("high","very high","extreme")[(random() * 3).toInt()]
            val model = listOf("fixed","inflationary","burn")[(random() * 3).toInt()]
            val circulating = kotlin.math.round((0.1 + random() * 10.0) * 1e8)
            result += ReferenceCryptoDefinition(
                symbol = symbol,
                name = name,
                base = kotlin.math.round((0.05 + random() * 20.0) * 1000.0) / 1000.0,
                profile = profile,
                supplyModel = model,
                circulating = circulating,
                maximumSupply = if (model == "fixed") circulating * 2 else null,
                inflation = if (model == "inflationary") 0.05 + random() * 0.1 else 0.02,
                burn = if (model == "burn") 0.02 + random() * 0.03 else 0.0,
                listedAt = EPOCH + (index + 1) * 7L * DAY
            )
        }
        return result
    }

    fun stockTick(now: Long): Long = Math.floorDiv(now, STOCK_TICK_MS)
    fun cryptoTick(now: Long): Long = Math.floorDiv(now, CRYPTO_TICK_MS)

    fun stockPriceAtTick(stock: ReferenceStockDefinition, tick: Long): Double {
        val perDay = (24 * 60) / 5.0
        val seed = hash(stock.ticker)
        val years = (tick - stockTick(stock.listedAt)) / (perDay * 365.0)
        val waveTerm = wave(seed, tick / 6.0) * stock.volatility * 6.0 +
            wave(seed xor 99, tick / 60.0) * stock.volatility * 10.0
        val sectorTerm = wave(hash(stock.sector), tick / perDay) * 0.06
        val marketCondition = wave(777, tick / perDay) * 0.08
        val price = stock.base * exp(stock.drift * years + waveTerm + sectorTerm + marketCondition)
        return max(0.01, kotlin.math.round(price * 100.0) / 100.0)
    }

    fun stockLiquidity(stock: ReferenceStockDefinition, tick: Long): Double {
        val liquidityWave = wave(hash(stock.ticker) xor 5, tick.toDouble())
        return max(10.0, floor(
            stock.shares * 0.00002 * (0.6 + 0.4 * (liquidityWave + 1.0)) /
                (1.0 + stock.volatility * 10.0)
        ))
    }

    fun stockIsProfitable(stock: ReferenceStockDefinition, utcDay: Long): Boolean =
        stock.margin + wave(hash(stock.ticker) xor 31, utcDay.toDouble()) * 0.12 > 0.0

    fun cryptoPriceAtTick(crypto: ReferenceCryptoDefinition, tick: Long): Double {
        val seed = hash(crypto.symbol)
        val volatility = when (crypto.profile) {
            "low" -> 0.02
            "moderate" -> 0.04
            "high" -> 0.07
            "very high" -> 0.1
            else -> 0.15
        }
        val perDay = (24 * 60) / 2.0
        val sentiment = wave(4242, tick / perDay) * 0.15
        val fluctuation = wave(seed, tick / 4.0) * volatility * 4.0 +
            wave(seed xor 7, tick / 90.0) * volatility * 12.0
        val price = max(MIN_CRYPTO_PRICE, crypto.base * exp(fluctuation + sentiment))
        return BigDecimal(price, MathContext(6, RoundingMode.HALF_UP)).toDouble()
    }

    fun cryptoLiquidity(crypto: ReferenceCryptoDefinition, tick: Long): Double {
        val liquidityWave = wave(hash(crypto.symbol) xor 3, tick.toDouble())
        return crypto.circulating * 0.000002 * (0.5 + 0.5 * (liquidityWave + 1.0))
    }

    fun cryptoSupply(crypto: ReferenceCryptoDefinition, now: Long): ReferenceCryptoSupply {
        val years = max(0.0, (now - crypto.listedAt) / (365.0 * DAY))
        var circulating = crypto.circulating * (1.0 + crypto.inflation - crypto.burn).pow(years)
        crypto.maximumSupply?.let { circulating = min(circulating, it) }
        return ReferenceCryptoSupply(circulating, crypto.circulating * crypto.burn * years, crypto.maximumSupply)
    }

    fun priceStock(stock: ReferenceStockDefinition, now: Long): Double = stockPriceAtTick(stock, stockTick(now))
    fun priceCrypto(crypto: ReferenceCryptoDefinition, now: Long): Double = cryptoPriceAtTick(crypto, cryptoTick(now))

    fun placeOrder(
        state: LocalSimulationState,
        context: Context,
        market: String,
        symbol: String,
        side: LocalOrderSide,
        kind: LocalOrderKind,
        requestedQuantity: Double,
        limitPrice: Double? = null,
        triggerPrice: Double? = null,
        now: Long
    ): LocalGameResult {
        val stockMarket = market.equals("stock", true)
        val normalizedMarket = if (stockMarket) "stock" else "crypto"
        val minimumLevel = if (stockMarket) 2 else 4
        if (state.progress.level < minimumLevel) {
            return reject(state, "${if (stockMarket) "Stock market" else "Crypto exchange"} unlocks at Player Level $minimumLevel")
        }
        val stock = if (stockMarket) listStocks(now).firstOrNull { it.ticker == symbol } else null
        val crypto = if (!stockMarket) listCrypto(now).firstOrNull { it.symbol == symbol } else null
        if (stock == null && crypto == null) return reject(state, "Unknown market asset")
        if (stockMarket && kind in setOf(LocalOrderKind.STOP, LocalOrderKind.TAKE_PROFIT)) {
            return reject(state, "Stop-loss and take-profit orders are crypto-only")
        }
        if (kind in setOf(LocalOrderKind.STOP, LocalOrderKind.TAKE_PROFIT) && side != LocalOrderSide.SELL) {
            return reject(state, "Stop-loss and take-profit orders are sell-only")
        }
        val quantity = if (stockMarket) floor(requestedQuantity) else kotlin.math.round(requestedQuantity * 1e6) / 1e6
        if (quantity <= 0.0) return reject(state, "Invalid order quantity")
        if (kind == LocalOrderKind.LIMIT && (limitPrice == null || limitPrice <= 0.0)) {
            return reject(state, "Set a valid limit price")
        }
        if (kind in setOf(LocalOrderKind.STOP, LocalOrderKind.TAKE_PROFIT) && (triggerPrice == null || triggerPrice <= 0.0)) {
            return reject(state, "Set a valid trigger price")
        }
        val fee = ReferenceCompanyEngine.tradingFeeRate(state, context, normalizedMarket)
        val currentTick = if (stockMarket) stockTick(now) else cryptoTick(now)
        val price = stock?.let { stockPriceAtTick(it, currentTick) } ?: cryptoPriceAtTick(crypto!!, currentTick)
        val liquidity = stock?.let { floor(stockLiquidity(it, currentTick)) } ?: cryptoLiquidity(crypto!!, currentTick)
        val isMarket = kind == LocalOrderKind.MARKET
        val currentlyTriggered = when (kind) {
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
            expiresAt = now + ORDER_EXPIRY_DAYS * DAY,
            status = if (currentlyTriggered && !isMarket) LocalOrderStatus.TRIGGERED else LocalOrderStatus.OPEN
        )
        var next = state
        if (side == LocalOrderSide.BUY && !isMarket) {
            val reservePrice = limitPrice ?: return reject(state, "Set a limit price")
            val reserve = quantity * reservePrice * (1.0 + fee)
            if (state.cash - state.reservedCash + 1e-9 < reserve) return reject(state, "Insufficient available funds")
            order = order.copy(reservedCash = reserve)
            next = next.copy(reservedCash = next.reservedCash + reserve)
        } else if (side == LocalOrderSide.SELL) {
            val holdings = if (stockMarket) state.stockHoldings else state.cryptoHoldings
            val holding = holdings[symbol] ?: return reject(state, "You do not own this asset")
            if (holding.quantity - holding.reservedQuantity + 1e-9 < quantity) return reject(state, "Insufficient unreserved holdings")
            if (!isMarket) {
                val map = holdings + (symbol to holding.copy(reservedQuantity = holding.reservedQuantity + quantity))
                next = if (stockMarket) next.copy(stockHoldings = map) else next.copy(cryptoHoldings = map)
            }
        }
        next = next.copy(marketOrders = (next.marketOrders + order).takeLast(200))
        if (isMarket || currentlyTriggered) {
            val quantityToFill = min(quantity, liquidity)
            if (quantityToFill <= 0.0) return reject(state, "No market liquidity right now")
            if (side == LocalOrderSide.BUY && isMarket && next.cash - next.reservedCash + 1e-9 < quantityToFill * price * (1.0 + fee)) {
                return reject(state, "Insufficient available funds")
            }
            val liveOrder = next.marketOrders.last()
            val filled = fillOrder(next, context, liveOrder, quantityToFill, price, fee, now)
            if (filled is LocalGameResult.Rejected) return filled
            next = (filled as LocalGameResult.Success).state
            if (isMarket && quantityToFill + 1e-9 < quantity) {
                next = updateOrder(next, id) { it.copy(status = LocalOrderStatus.CANCELLED) }
            }
            return success(next, if (quantityToFill + 1e-9 >= quantity) "Order executed" else "Partial market fill; remaining quantity cancelled")
        }
        val ledger = record(next, now, "order_placed", "Placed $normalizedMarket $kind $side order for $symbol", 0.0)
        return success(ledger, "Order placed")
    }

    fun cancelOrder(state: LocalSimulationState, orderId: String, now: Long): LocalGameResult {
        val order = state.marketOrders.firstOrNull { it.id == orderId } ?: return reject(state, "Order not found")
        if (!isOpen(order.status)) return reject(state, "Order is no longer open")
        val released = releaseOrder(state, order, LocalOrderStatus.CANCELLED)
        return success(record(released, now, "order_cancel", "Cancelled order for ${order.symbol}", 0.0), "Order cancelled")
    }

    fun tick(state: LocalSimulationState, context: Context, now: Long): LocalSimulationState {
        var current = state
        current = evaluateMarketOrders(current, context, "stock", now)
        current = evaluateMarketOrders(current, context, "crypto", now)
        current = payDividends(current, now)
        return current
    }

    fun getStockPrice(state: LocalSimulationState, symbol: String, now: Long): Double? =
        listStocks(now).firstOrNull { it.ticker == symbol }?.let { priceStock(it, now) }

    fun getCryptoPrice(state: LocalSimulationState, symbol: String, now: Long): Double? =
        listCrypto(now).firstOrNull { it.symbol == symbol }?.let { priceCrypto(it, now) }

    private fun evaluateMarketOrders(
        input: LocalSimulationState,
        context: Context,
        market: String,
        now: Long
    ): LocalSimulationState {
        val stockMarket = market == "stock"
        val currentTick = if (stockMarket) stockTick(now) else cryptoTick(now)
        val lastEval = if (stockMarket) input.stockLastEvalTick else input.cryptoLastEvalTick
        val orders = input.marketOrders.filter { it.market == market && isOpen(it.status) }
        var current = input
        if (orders.isNotEmpty()) {
            val from = max(lastEval + 1L, currentTick - MAX_TICKS_PER_EVAL)
            val fee = orders.map { ReferenceCompanyEngine.tradingFeeRate(input, context, market) }.first()
            val stockDefs = if (stockMarket) listStocks(now).associateBy { it.ticker } else emptyMap()
            val cryptoDefs = if (!stockMarket) listCrypto(now).associateBy { it.symbol } else emptyMap()
            for (tick in from..currentTick) {
                val tickTime = tick * if (stockMarket) STOCK_TICK_MS else CRYPTO_TICK_MS
                for (snapshotOrder in current.marketOrders.filter { it.market == market && isOpen(it.status) }) {
                    var order = current.marketOrders.firstOrNull { it.id == snapshotOrder.id } ?: continue
                    if (!isOpen(order.status)) continue
                    if (tickTime >= order.expiresAt) {
                        current = releaseOrder(current, order, LocalOrderStatus.EXPIRED)
                        continue
                    }
                    if (tickTime < order.createdAt) continue
                    val stock = if (stockMarket) stockDefs[order.symbol] else null
                    val crypto = if (!stockMarket) cryptoDefs[order.symbol] else null
                    if (stock == null && crypto == null) continue
                    val price = stock?.let { stockPriceAtTick(it, tick) } ?: cryptoPriceAtTick(crypto!!, tick)
                    val liquidity = stock?.let { floor(stockLiquidity(it, tick)) } ?: cryptoLiquidity(crypto!!, tick)
                    var canFill = false
                    when (order.kind) {
                        LocalOrderKind.MARKET -> canFill = true
                        LocalOrderKind.LIMIT -> canFill = if (order.side == LocalOrderSide.BUY) {
                            price <= (order.limitPrice ?: Double.NEGATIVE_INFINITY)
                        } else {
                            price >= (order.limitPrice ?: Double.POSITIVE_INFINITY)
                        }
                        LocalOrderKind.STOP -> {
                            if (order.status == LocalOrderStatus.OPEN && price <= (order.triggerPrice ?: Double.NEGATIVE_INFINITY)) {
                                current = updateOrder(current, order.id) { it.copy(status = LocalOrderStatus.TRIGGERED) }
                                order = current.marketOrders.first { it.id == order.id }
                            }
                            canFill = order.status == LocalOrderStatus.TRIGGERED
                        }
                        LocalOrderKind.TAKE_PROFIT -> {
                            if (order.status == LocalOrderStatus.OPEN && price >= (order.triggerPrice ?: Double.POSITIVE_INFINITY)) {
                                current = updateOrder(current, order.id) { it.copy(status = LocalOrderStatus.TRIGGERED) }
                                order = current.marketOrders.first { it.id == order.id }
                            }
                            canFill = order.status == LocalOrderStatus.TRIGGERED
                        }
                    }
                    if (!canFill) continue
                    val quantity = min(order.quantity - order.filledQuantity, liquidity)
                    if (quantity <= 1e-12) continue
                    val result = fillOrder(current, context, order, quantity, price, fee, tickTime)
                    if (result is LocalGameResult.Success) current = result.state
                }
            }
        }
        return if (stockMarket) current.copy(stockLastEvalTick = currentTick) else current.copy(cryptoLastEvalTick = currentTick)
    }

    private fun fillOrder(
        state: LocalSimulationState,
        context: Context,
        order: LocalMarketOrder,
        quantity: Double,
        price: Double,
        feeRate: Double,
        now: Long
    ): LocalGameResult {
        val remaining = min(quantity, order.quantity - order.filledQuantity)
        if (remaining <= 0.0) return reject(state, "Nothing remains to fill")
        val cost = remaining * price
        val fee = cost * feeRate
        val buy = order.side == LocalOrderSide.BUY
        var cash = state.cash
        var reservedCash = state.reservedCash
        var stockHoldings = state.stockHoldings
        var cryptoHoldings = state.cryptoHoldings
        val holdings = if (order.market == "stock") stockHoldings else cryptoHoldings
        val oldHolding = holdings[order.symbol] ?: LocalHolding()
        var updatedHolding = oldHolding
        var updatedOrder = order
        if (buy) {
            if (order.kind == LocalOrderKind.MARKET) {
                if (cash - reservedCash + 1e-9 < cost + fee) return reject(state, "Insufficient available funds")
                cash -= cost + fee
            } else {
                val referencePrice = order.limitPrice ?: price
                val reservedForFill = min(order.reservedCash, remaining * referencePrice * (1.0 + feeRate))
                if (reservedCash + 1e-9 < reservedForFill || cash + 1e-9 < cost + fee) {
                    return reject(state, "Reserved cash is unavailable")
                }
                reservedCash = max(0.0, reservedCash - reservedForFill)
                cash -= cost + fee
                updatedOrder = updatedOrder.copy(reservedCash = max(0.0, updatedOrder.reservedCash - reservedForFill))
            }
            updatedHolding = oldHolding.copy(
                quantity = oldHolding.quantity + remaining,
                costBasis = oldHolding.costBasis + cost + fee
            )
        } else {
            val requiredReserved = if (order.kind == LocalOrderKind.MARKET) 0.0 else remaining
            if (oldHolding.quantity + 1e-9 < remaining || oldHolding.reservedQuantity + 1e-9 < requiredReserved) {
                return reject(state, "Reserved holdings are unavailable")
            }
            val averageCost = if (oldHolding.quantity > 0.0) oldHolding.costBasis / oldHolding.quantity else 0.0
            updatedHolding = oldHolding.copy(
                quantity = max(0.0, oldHolding.quantity - remaining),
                reservedQuantity = max(0.0, oldHolding.reservedQuantity - requiredReserved),
                costBasis = max(0.0, oldHolding.costBasis - averageCost * remaining)
            )
            cash += cost - fee
        }
        val map = holdings + if (updatedHolding.quantity <= 1e-9) emptyMap() else mapOf(order.symbol to updatedHolding)
        if (order.market == "stock") stockHoldings = map else cryptoHoldings = map
        val newFilled = updatedOrder.filledQuantity + remaining
        val averageFill = if (newFilled <= 0.0) price else
            (updatedOrder.averageFillPrice * updatedOrder.filledQuantity + price * remaining) / newFilled
        updatedOrder = updatedOrder.copy(
            filledQuantity = newFilled,
            averageFillPrice = averageFill,
            feePaid = updatedOrder.feePaid + fee,
            status = if (newFilled + 1e-9 >= updatedOrder.quantity) LocalOrderStatus.EXECUTED else LocalOrderStatus.PARTIALLY_FILLED
        )
        var next = state.copy(
            cash = cash,
            reservedCash = reservedCash,
            stockHoldings = stockHoldings,
            cryptoHoldings = cryptoHoldings,
            marketOrders = state.marketOrders.map { if (it.id == order.id) updatedOrder else it },
            progress = if (order.market == "stock") state.progress.copy(stockTrades = state.progress.stockTrades + 1)
                else state.progress.copy(cryptoTrades = state.progress.cryptoTrades + 1)
        )
        if (updatedOrder.status == LocalOrderStatus.EXECUTED && updatedOrder.reservedCash > 1e-9) {
            next = next.copy(reservedCash = max(0.0, next.reservedCash - updatedOrder.reservedCash))
            updatedOrder = updatedOrder.copy(reservedCash = 0.0)
            next = updateOrder(next, order.id) { updatedOrder }
        }
        return success(record(
            next,
            now,
            if (buy) "${order.market}_buy" else "${order.market}_sell",
            "${if (buy) "Bought" else "Sold"} ${formatQuantity(remaining)} ${order.symbol} @ ${price.toFixed2()}",
            if (buy) -(cost + fee) else cost - fee
        ), "Order filled")
    }

    private fun payDividends(state: LocalSimulationState, now: Long): LocalSimulationState {
        val today = Math.floorDiv(now, DAY)
        val start = max(state.lastDividendDay + 1L, today - 30L)
        var totalDividends = 0.0
        val definitions = listStocks(now).associateBy { it.ticker }
        for (day in start..today) {
            state.stockHoldings.forEach { (symbol, holding) ->
                val definition = definitions[symbol] ?: return@forEach
                if (definition.dividendYield <= 0.0 || !stockIsProfitable(definition, day)) return@forEach
                val price = stockPriceAtTick(definition, stockTick(day * DAY))
                totalDividends += (holding.quantity * price * definition.dividendYield) / 365.0
            }
        }
        if (totalDividends <= 0.0) return state.copy(lastDividendDay = today)
        val entry = LocalLedgerEntry(
            id = "local-${state.nextSequence}",
            timestamp = now,
            category = "dividend",
            description = "Stock dividends",
            amount = totalDividends,
            balanceAfter = state.cash + totalDividends
        )
        return state.copy(
            cash = state.cash + totalDividends,
            lastDividendDay = today,
            progress = state.progress.copy(
                dividends = state.progress.dividends + totalDividends,
                lifetimeRevenue = state.progress.lifetimeRevenue + totalDividends
            ),
            ledger = (state.ledger + entry).takeLast(5000),
            nextSequence = state.nextSequence + 1
        )
    }

    private fun releaseOrder(state: LocalSimulationState, order: LocalMarketOrder, status: LocalOrderStatus): LocalSimulationState {
        var next = state
        if (order.side == LocalOrderSide.BUY && order.reservedCash > 0.0) {
            next = next.copy(reservedCash = max(0.0, next.reservedCash - order.reservedCash))
        } else if (order.side == LocalOrderSide.SELL) {
            val holdings = if (order.market == "stock") next.stockHoldings else next.cryptoHoldings
            val holding = holdings[order.symbol]
            if (holding != null) {
                val remaining = order.quantity - order.filledQuantity
                val updated = holding.copy(reservedQuantity = max(0.0, holding.reservedQuantity - remaining))
                val map = holdings + (order.symbol to updated)
                next = if (order.market == "stock") next.copy(stockHoldings = map) else next.copy(cryptoHoldings = map)
            }
        }
        return updateOrder(next, order.id) { it.copy(reservedCash = 0.0, status = status) }
    }

    private fun updateOrder(state: LocalSimulationState, orderId: String, transform: (LocalMarketOrder) -> LocalMarketOrder): LocalSimulationState =
        state.copy(marketOrders = state.marketOrders.map { if (it.id == orderId) transform(it) else it })

    private fun isOpen(status: LocalOrderStatus) =
        status == LocalOrderStatus.OPEN || status == LocalOrderStatus.PARTIALLY_FILLED || status == LocalOrderStatus.TRIGGERED

    private fun record(state: LocalSimulationState, now: Long, category: String, description: String, amount: Double): LocalSimulationState {
        val entry = LocalLedgerEntry(
            id = "local-${state.nextSequence}",
            timestamp = now,
            category = category,
            description = description,
            amount = amount,
            balanceAfter = state.cash
        )
        return state.copy(ledger = (state.ledger + entry).takeLast(5000), nextSequence = state.nextSequence + 1)
    }

    private fun formatQuantity(value: Double): String =
        if (value >= 1.0) String.format(Locale.US, "%,.2f", value) else String.format(Locale.US, "%.5g", value)

    private fun Double.toFixed2(): String = String.format(Locale.US, "%.2f", this)

    private fun reject(state: LocalSimulationState, message: String) = LocalGameResult.Rejected(state, message)
    private fun success(state: LocalSimulationState, message: String) = LocalGameResult.Success(state, message)

    private fun stock(
        ticker: String, name: String, sector: String, base: Double,
        volatility: Double, drift: Double, shares: Double, dividendYield: Double
    ): ReferenceStockDefinition {
        val margin = 0.08 + (hash(ticker).toUInt().toLong() % 20L) / 100.0
        return ReferenceStockDefinition(ticker,name,sector,base,volatility,drift,shares,dividendYield,margin,EPOCH)
    }

    private fun hash(value: String): Int {
        var result = 2166136261L
        value.forEach { result = (result xor it.code.toLong()) * 16777619L and 0xffffffffL }
        return result.toInt()
    }

    private fun mulberry(seed: Int): () -> Double {
        var state = seed
        return {
            state += 0x6d2b79f5
            var t = state
            t = (t xor (t ushr 15)) * (t or 1)
            t = t xor (t + ((t xor (t ushr 7)) * (t or 61)))
            ((t xor (t ushr 14)).toUInt().toLong()).toDouble() / 4294967296.0
        }
    }

    private fun wave(seed: Int, time: Double): Double {
        val random = mulberry(seed)
        var sum = 0.0
        var norm = 0.0
        repeat(6) { index ->
            val period = 20.0 + random() * 600.0 * (index + 1)
            val phase = random() * Math.PI * 2.0
            val amplitude = 1.0 / (index + 1)
            sum += amplitude * sin((time / period) * Math.PI * 2.0 + phase)
            norm += amplitude
        }
        val jitter = mulberry(seed xor floor(time).toInt()).invoke()
        return (sum / norm) * 0.9 + (jitter - 0.5) * 0.2
    }
}
