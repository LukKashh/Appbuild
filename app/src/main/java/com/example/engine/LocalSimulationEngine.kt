package com.example.engine

import com.example.model.*
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

data class LocalBusinessReport(val gross: Double, val expenses: Double, val net: Double, val status: String)
data class LocalPropertyReport(val grossRent: Double, val expenses: Double, val netRent: Double, val occupancy: Double, val condition: Int)

/** Deterministic, device-only simulation. It never requires a network clock or remote function. */
object LocalSimulationEngine {
    fun xpRequired(level: Int): Double = if (level < 10) 250.0 + level * 175.0 else 1_825.0 * Math.pow(1.18, (level - 10).toDouble())

    fun addXp(progress: LocalPlayerProgress, amount: Double): LocalPlayerProgress {
        var level = progress.level.coerceAtLeast(1)
        var xp = progress.xp + amount.coerceAtLeast(0.0)
        while (xp >= xpRequired(level) && level < 1000) { xp -= xpRequired(level); level++ }
        return progress.copy(level = level, xp = xp)
    }

    private fun stableSeed(text: String): Int = text.fold(0x45d9f3b) { acc, ch -> acc * 31 + ch.code }
    private fun wave(seed: Int, time: Double): Double {
        val phase = (seed and 0x7fffffff).toDouble() / Int.MAX_VALUE.toDouble() * Math.PI * 2.0
        return sin(time * 0.61803398875 + phase)
    }

    fun businessReport(b: Business, at: Long): LocalBusinessReport {
        val staff = if (b.minimumEmployees <= 0) 1.0 else (b.employeeCount.toDouble() / b.minimumEmployees).coerceIn(0.0, 1.0)
        val cycle = 1.0 + wave(stableSeed(b.id), at / 3_600_000.0) * if (b.industry == Industry.HOSPITALITY) 0.12 else 0.055
        val upgradeRevenue = b.upgrades.sumOf { max(0, it.level) * it.revenueBonusPerLevel }
        val upgradeSavings = b.upgrades.sumOf { max(0, it.level) * it.expenseReductionPerLevel }.coerceIn(0.0, 0.65)
        val gross = b.grossRevenuePerSec * (1.0 + upgradeRevenue) * cycle
        val expenses = (b.operatingExpensePerSec + b.employeeCount * b.salaryPerEmployee) * (1.0 - upgradeSavings)
        val net = gross - expenses
        val status = when { b.isSuspended -> "Suspended"; net < 0.0 -> "Loss-making"; net < gross * 0.05 -> "Break-even"; else -> "Profitable" }
        return LocalBusinessReport(gross, expenses, net, status)
    }

    fun propertyReport(p: Property, at: Long): LocalPropertyReport {
        val seed = stableSeed(p.id + p.location)
        val demand = (p.demandMultiplier + wave(seed, at / 21_600_000.0) * 0.10).coerceIn(0.5, 1.5)
        val occupancy = (p.occupancyRate + wave(seed xor 0x6a09e667, at / 14_400_000.0) * 0.06).coerceIn(0.05, 1.0)
        val conditionFactor = when { p.conditionPercent < 20 -> 0.60; p.conditionPercent < 40 -> 0.82; else -> (p.conditionPercent / 100.0).coerceIn(0.4, 1.0) }
        val gross = p.baseRentPerSec * occupancy * demand * conditionFactor * p.calculateMultiplier()
        val savings = p.upgrades.sumOf { max(0, it.level) * it.expenseReduction }.coerceIn(0.0, 0.6)
        val expenses = p.operatingExpensePerSec * (1.0 - savings)
        return LocalPropertyReport(gross, expenses, gross - expenses, occupancy, p.conditionPercent)
    }

    fun advance(input: GameState, now: Long, offline: Boolean = false): GameState {
        val elapsedMs = (now - input.lastActiveTimestamp).coerceAtLeast(0L)
        if (elapsedMs < 500L) return input
        val elapsedHours = elapsedMs / 3_600_000.0
        val elapsedSeconds = elapsedMs / 1000.0
        val offlineHoursBonus = input.research.filter { it.isUnlocked }.sumOf { it.effects["offlineHours"] ?: 0.0 }
        val businessBonus = input.research.filter { it.isUnlocked }.sumOf { it.effects["businessPct"] ?: 0.0 }
        val rentBonus = input.research.filter { it.isUnlocked }.sumOf { it.effects["rentPct"] ?: 0.0 }
        val corpBusinessBonus = (input.corporation.facilities["operations"] ?: 0) * 0.01
        val corpRentBonus = (input.corporation.facilities["realty"] ?: 0) * 0.01
        val ownedBonuses = input.collectibles.filter { it.isOwned }.groupBy { it.bonusKey }.mapValues { entry -> entry.value.sumOf { it.bonusValue } }
        val globalBusiness = (1.0 + businessBonus + corpBusinessBonus + (ownedBonuses["businessPct"] ?: 0.0)).coerceAtMost(3.0)
        val globalRent = (1.0 + rentBonus + corpRentBonus + (ownedBonuses["rentPct"] ?: 0.0)).coerceAtMost(3.0)
        var businessSettlement = 0.0
        var researchFromBusiness = 0.0
        val businesses = input.businesses.map { business ->
            if (!business.isOwned) return@map business
            val cappedHours = if (!offline) elapsedHours else if (business.offlineCapHours > 0.0) min(elapsedHours, business.offlineCapHours + offlineHoursBonus) else 0.0
            if (cappedHours <= 0.0) return@map business
            val report = businessReport(business, now)
            val gross = report.gross * globalBusiness * cappedHours * 3600.0
            val expenses = report.expenses * cappedHours * 3600.0
            var net = gross - expenses
            var reserves = business.reserves
            var liabilities = business.liabilities
            var wallet = business.accumulatedWallet
            if (net >= 0.0) {
                val debtService = min(liabilities, net * 0.20)
                liabilities -= debtService
                net -= debtService
                wallet += net
            } else {
                var loss = -net
                val reserveDraw = min(reserves, loss)
                reserves -= reserveDraw
                loss -= reserveDraw
                val walletDraw = min(wallet, loss)
                wallet -= walletDraw
                loss -= walletDraw
                liabilities += loss
            }
            val autoSuspended = !business.isSuspended && liabilities > business.baseCost * 0.30
            val sample = LocalFinancialSample(now, gross, expenses)
            businessSettlement += net
            researchFromBusiness += cappedHours * 2.0
            business.copy(accumulatedWallet = wallet, reserves = reserves, liabilities = liabilities,
                totalRevenue = business.totalRevenue + gross, totalExpenses = business.totalExpenses + expenses,
                isSuspended = business.isSuspended || autoSuspended, financialHistory = (business.financialHistory + sample).takeLast(72))
        }
        var propertySettlement = 0.0
        val properties = input.properties.map { property ->
            if (!property.isOwned) return@map property
            val hours = if (offline) min(elapsedHours, 24.0) else elapsedHours
            val report = propertyReport(property, now)
            val net = (report.grossRent * globalRent - report.expenses) * hours * 3600.0
            var rentWallet = property.accumulatedRent + net
            var maintenanceDebt = property.maintenanceDebt
            if (rentWallet < 0.0) { maintenanceDebt += -rentWallet; rentWallet = 0.0 }
            else if (maintenanceDebt > 0.0) { val paid = min(maintenanceDebt, rentWallet); maintenanceDebt -= paid; rentWallet -= paid }
            val condition = (property.preciseCondition - (0.10 * hours)).coerceIn(0.0, 100.0)
            val appreciation = 1.0 + max(0.0, condition - 35.0) / 100.0 * 0.00004 * hours
            propertySettlement += net
            property.copy(accumulatedRent = rentWallet, maintenanceDebt = maintenanceDebt, preciseCondition = condition,
                conditionPercent = condition.toInt(), occupancyRate = report.occupancy,
                currentValuation = (property.currentValuation * appreciation).coerceAtLeast(property.purchasePrice * 0.20))
        }
        val labPoints = if (input.researchLabBuilt) 30.0 * elapsedHours else 0.0
        val researchEarned = labPoints + researchFromBusiness
        val completedProjects = input.activeResearch.filter { it.completesAt <= now }
        val activeResearch = input.activeResearch.filter { it.completesAt > now }
        val research = input.research.map { node -> if (completedProjects.any { it.researchId == node.id }) node.copy(isUnlocked = true) else node }
        val researchXp = completedProjects.size * 100.0
        val tokenRegen = elapsedSeconds * 22.0
        val progress = addXp(input.progression.copy(clickTokens = (input.progression.clickTokens + tokenRegen).coerceAtMost(150.0),
            tokenUpdatedAt = now, lifetimeEarned = input.progression.lifetimeEarned + max(0.0, businessSettlement + propertySettlement)),
            researchXp + max(0.0, businessSettlement + propertySettlement) * 0.001)
        val stocks = input.stocks.map { stock ->
            val price = marketPrice(stock.basePrice, stock.symbol, now, stock.volatility, false)
            val oldBucket = input.lastActiveTimestamp / 300_000L
            val newBucket = now / 300_000L
            val history = if (newBucket > oldBucket) (stock.priceHistory + price).takeLast(120) else stock.priceHistory
            stock.copy(previousPrice = stock.currentPrice, currentPrice = price, priceHistory = history)
        }
        val cryptos = input.cryptos.map { crypto ->
            val price = marketPrice(crypto.basePrice, crypto.symbol, now, crypto.volatility, true)
            val oldBucket = input.lastActiveTimestamp / 120_000L
            val newBucket = now / 120_000L
            val history = if (newBucket > oldBucket) (crypto.priceHistory + price).takeLast(180) else crypto.priceHistory
            val mood = when { price > crypto.previousPrice * 1.01 -> MarketSentiment.BULLISH; price < crypto.previousPrice * 0.99 -> MarketSentiment.BEARISH; else -> MarketSentiment.VOLATILE }
            crypto.copy(previousPrice = crypto.currentPrice, currentPrice = price, priceHistory = history, sentiment = mood)
        }
        val dividends = if (now / 86_400_000L > input.lastActiveTimestamp / 86_400_000L) stocks.sumOf { it.holdingsValue * it.dividendYieldPercent / 100.0 / 365.0 } else 0.0
        val collectibles = input.collectibles.map { item ->
            val ageDays = max(0L, now - item.releaseAt) / 86_400_000.0
            val oscillation = wave(stableSeed(item.id), now / 21_600_000.0) * item.volatility
            val scarcity = item.rarity.ordinal * 0.012 + if (item.limitedEdition) 0.035 else 0.0
            val value = item.purchasePrice / 1.05 * (1.0 + oscillation + scarcity + ageDays * item.appreciationRatePerSec * 100.0)
            item.copy(currentValuation = value.coerceAtLeast(item.purchasePrice * 0.20))
        }
        var next = input.copy(
            cash = input.cash + dividends, businesses = businesses, properties = properties, stocks = stocks, cryptos = cryptos,
            collectibles = collectibles, research = research, activeResearch = activeResearch,
            researchPoints = input.researchPoints + researchEarned, researchPointsEarned = input.researchPointsEarned + researchEarned,
            progression = progress, lastActiveTimestamp = now)
        next = LocalMarketEngine.advance(next, now)
        next = next.copy(corporation = LocalCorporationEngine.advance(next.corporation, elapsedSeconds, now))
        val history = if (now - input.lastFinancialSnapshotAt >= 60_000L) (next.netWorthHistory + next.totalNetWorth).takeLast(720) else next.netWorthHistory
        next = next.copy(netWorthHistory = history, lastFinancialSnapshotAt = if (history.size != input.netWorthHistory.size) now else input.lastFinancialSnapshotAt)
        return next
    }

    private fun marketPrice(base: Double, symbol: String, at: Long, volatility: Double, crypto: Boolean): Double {
        val seed = stableSeed(symbol)
        val days = at / 86_400_000.0
        val interval = if (crypto) 120_000.0 else 300_000.0
        val intraday = at / interval
        val slow = wave(seed, days * 0.18) * 0.10 + wave(seed xor 0x125, days * 0.047) * 0.14
        val fast = wave(seed xor 0x6a09e667, intraday * 0.08) * volatility * 0.55 + wave(seed xor 0xbb67ae85.toInt(), intraday * 0.021) * volatility * 0.45
        return (base * (1.0 + slow + fast)).coerceAtLeast(if (crypto) 0.0001 else 0.01)
    }
}