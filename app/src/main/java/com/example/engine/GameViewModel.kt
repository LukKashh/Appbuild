package com.example.engine

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GameRepository
import com.example.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.random.Random

data class OfflineReport(
    val elapsedSeconds: Long,
    val businessEarned: Double,
    val rentEarned: Double,
    val totalEarned: Double
)

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GameRepository(application)

    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private val _offlineReport = MutableStateFlow<OfflineReport?>(null)
    val offlineReport: StateFlow<OfflineReport?> = _offlineReport.asStateFlow()

    private var tickCounter = 0L

    init {
        viewModelScope.launch {
            val restored = repository.loadGameState()
            _state.value = restored
            checkOfflineProgress()
            startGameLoop()
        }
    }

    private fun checkOfflineProgress() {
        val now = System.currentTimeMillis()
        val before = _state.value
        if (now - before.lastActiveTimestamp < 20000L) return
        val elapsed = ((now - before.lastActiveTimestamp) / 1000L).coerceAtLeast(0L)
        val advanced = LocalSimulationEngine.advance(before, now, offline = true)
        val business = advanced.businesses.sumOf { it.accumulatedWallet } - before.businesses.sumOf { it.accumulatedWallet }
        val rent = advanced.properties.sumOf { it.accumulatedRent } - before.properties.sumOf { it.accumulatedRent }
        _state.value = advanced
        val total = business + rent
        if (total > 0.0) {
            _offlineReport.value = OfflineReport(elapsed, business, rent, total)
            logTransaction(TransactionType.OFFLINE, "Offline business and property simulation", total)
        }
        viewModelScope.launch { repository.saveGameState(_state.value) }
    }
    fun dismissOfflineReport() {
        _offlineReport.value = null
    }

    private fun startGameLoop() {
        viewModelScope.launch {
            while (true) {
                delay(1000L)
                tickCounter++
                onGameTick()
            }
        }
    }

    private val simulatedHeadlines = listOf(
        "MARKETS: Federal Reserve signals accommodative liquidity framework.",
        "APEX TECH: Breakpoint achieved in optical tensor processor yields.",
        "BITFORTUNE: Network hash rate touches all-time peak amidst institutional accumulation.",
        "NOVA ENERGY: 800MW offshore wind grid commissioned ahead of timeline.",
        "VAULT FINANCIAL: Prime brokerage assets under management surge 24%.",
        "TITAN HEAVY: Space-dock structural casting contract finalized.",
        "REAL ESTATE: Prime commercial occupancy rates reach historic high.",
        "ETHERWEALTH: Layer-2 zero-knowledge rollups surpass 5,000 TPS.",
        "CAPITAL FORTUNE: Emerging conglomerate expands multi-sector holdings."
    )
    private val _newsTicker = MutableStateFlow(simulatedHeadlines.first())
    val newsTicker: StateFlow<String> = _newsTicker.asStateFlow()

    private fun onGameTick() {
        val now = System.currentTimeMillis()
        _state.update { current ->
            val advanced = LocalSimulationEngine.advance(current, now, offline = false)
            advanced.copy(achievements = evaluateAchievements(advanced))
        }
        if (tickCounter % 5L == 0L) viewModelScope.launch { repository.saveGameState(_state.value) }
        if (tickCounter % 30L == 0L) _newsTicker.value = simulatedHeadlines.random()
    }
    // --- Tap / Clicker Action ---
    fun onTapClicker() {
        _state.update { current ->
            val hasClickResearch = current.research.find { it.id == "res_2" }?.isUnlocked == true
            var power = current.clickPower * current.clickMultiplier
            if (hasClickResearch) power *= 2.0
            if (current.collectibles.find { it.id == "col_1" }?.isOwned == true) power *= 1.05

            val now = System.currentTimeMillis()
        }
    }

    fun buyClickUpgrade(upgradeId: String) {
        _state.update { current ->
            val upg = current.clickUpgrades.find { it.id == upgradeId } ?: return@update current
            if (current.cash < upg.currentCost) return@update current

            val newCash = current.cash - upg.currentCost
            val updatedUpgrades = current.clickUpgrades.map {
                if (it.id == upgradeId) it.copy(level = it.level + 1) else it
            }
            val newPower = 1.0 + updatedUpgrades.sumOf { it.level * it.powerBoost }

            val transaction = createTransaction(
                TransactionType.CLICKER,
                "Upgraded ${upg.name} (Lv. ${upg.level + 1})",
                -upg.currentCost
            )
            current.copy(
                cash = newCash,
                clickUpgrades = updatedUpgrades,
                clickPower = newPower,
                transactions = (listOf(transaction) + current.transactions).take(50)
            )
        }
    }

    // --- Business Actions ---
    fun buyBusiness(businessId: String) {
        _state.update { current ->
            val biz = current.businesses.find { it.id == businessId } ?: return@update current
            if (current.cash < biz.baseCost || biz.isOwned) return@update current

            val newCash = current.cash - biz.baseCost
            val updated = current.businesses.map {
                if (it.id == businessId) it.copy(isOwned = true, currentBranches = 1, acquiredAt = System.currentTimeMillis(), reserves = maxOf(it.reserves, it.baseCost * 0.03)) else it
            }
            logTransaction(TransactionType.BUSINESS, "Acquired ${biz.name}", -biz.baseCost)
            current.copy(cash = newCash, businesses = updated)
        }
    }

    fun expandBranch(businessId: String) {
        _state.update { current ->
            val biz = current.businesses.find { it.id == businessId } ?: return@update current
            if (!biz.isOwned || current.cash < biz.branchCost || biz.currentBranches >= biz.branchLimit) return@update current

            val cost = biz.branchCost
            val updated = current.businesses.map {
                if (it.id == businessId) it.copy(currentBranches = it.currentBranches + 1) else it
            }
            logTransaction(TransactionType.BUSINESS, "Opened branch of ${biz.name}", -cost)
            current.copy(cash = current.cash - cost, businesses = updated)
        }
    }

    fun hireEmployee(businessId: String) {
        _state.update { current ->
            val biz = current.businesses.find { it.id == businessId } ?: return@update current
            val hasCorpLearning = current.research.find { it.id == "res_10" }?.isUnlocked == true
            val cost = biz.calculateHireCost(if (hasCorpLearning) 0.50 else 1.0)
            if (!biz.isOwned || current.cash < cost || biz.employeeCount >= biz.employeeCapacity) return@update current

            val updated = current.businesses.map {
                if (it.id == businessId) it.copy(employeeCount = it.employeeCount + 1) else it
            }
            logTransaction(TransactionType.BUSINESS, "Hired employee for ${biz.name}", -cost)
            current.copy(cash = current.cash - cost, businesses = updated)
        }
    }

    fun fireEmployee(businessId: String) {
        _state.update { current ->
            val biz = current.businesses.find { it.id == businessId } ?: return@update current
            if (!biz.isOwned || biz.employeeCount <= 0) return@update current

            val updated = current.businesses.map {
                if (it.id == businessId) it.copy(employeeCount = it.employeeCount - 1) else it
            }
            current.copy(businesses = updated)
        }
    }

    fun buyBusinessUpgrade(businessId: String, upgradeId: String) {
        _state.update { current ->
            val biz = current.businesses.find { it.id == businessId } ?: return@update current
            val upg = biz.upgrades.find { it.id == upgradeId } ?: return@update current
            if (upg.isMaxed || current.cash < upg.currentCost) return@update current

            val updatedUpgrades = biz.upgrades.map {
                if (it.id == upgradeId) it.copy(isPurchased = true, level = it.level + 1) else it
            }
            val updatedBiz = biz.copy(upgrades = updatedUpgrades)
            val updatedList = current.businesses.map { if (it.id == businessId) updatedBiz else it }

            val transaction = createTransaction(
                TransactionType.BUSINESS,
                "Upgraded ${biz.name}: ${upg.name} (Level ${upg.level + 1})",
                -upg.currentCost
            )
            current.copy(
                cash = current.cash - upg.currentCost,
                businesses = updatedList,
                transactions = (listOf(transaction) + current.transactions).take(50)
            )
        }
    }

    fun collectBusinessWallet(businessId: String) {
        _state.update { current ->
            val biz = current.businesses.find { it.id == businessId } ?: return@update current
            if (biz.accumulatedWallet <= 0) return@update current

            val amount = biz.accumulatedWallet
            val updated = current.businesses.map {
                if (it.id == businessId) it.copy(accumulatedWallet = 0.0) else it
            }
            logTransaction(TransactionType.BUSINESS, "Collected profits from ${biz.name}", amount)
            current.copy(cash = current.cash + amount, businesses = updated)
        }
    }

    fun collectAllBusinessWallets() {
        _state.update { current ->
            val total = current.businesses.filter { it.isOwned }.sumOf { it.accumulatedWallet }
            if (total <= 0) return@update current

            val updated = current.businesses.map {
                if (it.isOwned) it.copy(accumulatedWallet = 0.0) else it
            }
            logTransaction(TransactionType.BUSINESS, "Collected all enterprise profits", total)
            current.copy(cash = current.cash + total, businesses = updated)
        }
    }

    fun sellBusiness(businessId: String) {
        _state.update { current ->
            val biz = current.businesses.find { it.id == businessId } ?: return@update current
            if (!biz.isOwned || System.currentTimeMillis() < biz.acquiredAt + 6L * 60L * 60L * 1000L) return@update current

            val payout = (biz.valuation * 0.80 + biz.accumulatedWallet + biz.reserves - biz.liabilities).coerceAtLeast(0.0)
            val updated = current.businesses.map {
                if (it.id == businessId) {
                    it.copy(
                        isOwned = false,
                        currentBranches = 0,
                        employeeCount = 0,
                        accumulatedWallet = 0.0,
                        upgrades = it.upgrades.map { u -> u.copy(isPurchased = false, level = 0) }, reserves = 0.0, liabilities = 0.0, isSuspended = false
                    )
                } else it
            }
            logTransaction(TransactionType.BUSINESS, "Sold ${biz.name}", payout)
            current.copy(cash = current.cash + payout, businesses = updated)
        }
    }

    // --- Real Estate Actions ---
    fun buyProperty(propertyId: String) {
        _state.update { current ->
            val prop = current.properties.find { it.id == propertyId } ?: return@update current
            if (current.cash < prop.purchasePrice || prop.isOwned) return@update current

            val updated = current.properties.map {
                if (it.id == propertyId) it.copy(isOwned = true, acquiredAt = System.currentTimeMillis(), preciseCondition = it.conditionPercent.toDouble()) else it
            }
            logTransaction(TransactionType.PROPERTY, "Purchased deed for ${prop.name}", -prop.purchasePrice)
            current.copy(cash = current.cash - prop.purchasePrice, properties = updated)
        }
    }

    fun renovateProperty(propertyId: String) {
        _state.update { current ->
            val prop = current.properties.find { it.id == propertyId } ?: return@update current
            if (!prop.isOwned || current.cash < prop.renovationCost || prop.conditionPercent >= 100) return@update current

            val cost = prop.renovationCost
            val updated = current.properties.map {
                if (it.id == propertyId) it.copy(conditionPercent = 100) else it
            }
            logTransaction(TransactionType.PROPERTY, "Renovated ${prop.name} to 100%", -cost)
            current.copy(cash = current.cash - cost, properties = updated)
        }
    }

    fun renovateAllProperties() {
        _state.update { current ->
            val owned = current.properties.filter { it.isOwned && it.conditionPercent < 100 }
            if (owned.isEmpty()) return@update current
            val totalCost = owned.sumOf { it.renovationCost }
            if (current.cash < totalCost) return@update current

            val updated = current.properties.map {
                if (it.isOwned) it.copy(conditionPercent = 100) else it
            }
            logTransaction(TransactionType.PROPERTY, "Renovated ${owned.size} properties to 100%", -totalCost)
            current.copy(cash = current.cash - totalCost, properties = updated)
        }
    }

    fun buyPropertyUpgrade(propertyId: String, upgradeId: String) {
        _state.update { current ->
            val prop = current.properties.find { it.id == propertyId } ?: return@update current
            val upg = prop.upgrades.find { it.id == upgradeId } ?: return@update current
            if (upg.level >= upg.maxLevel || current.cash < upg.currentCost) return@update current

            val updatedUpgrades = prop.upgrades.map {
                if (it.id == upgradeId) it.copy(isPurchased = true, level = it.level + 1) else it
            }
            val updatedProp = prop.copy(upgrades = updatedUpgrades)
            val updatedList = current.properties.map { if (it.id == propertyId) updatedProp else it }

            val transaction = createTransaction(
                TransactionType.PROPERTY,
                "Upgraded ${prop.name}: ${upg.name}",
                -upg.cost
            )
            current.copy(
                cash = current.cash - upg.currentCost,
                properties = updatedList,
                transactions = (listOf(transaction) + current.transactions).take(50)
            )
        }
    }

    fun collectPropertyRent(propertyId: String) {
        _state.update { current ->
            val prop = current.properties.find { it.id == propertyId } ?: return@update current
            if (prop.accumulatedRent <= 0) return@update current

            val amount = prop.accumulatedRent
            val updated = current.properties.map {
                if (it.id == propertyId) it.copy(accumulatedRent = 0.0) else it
            }
            logTransaction(TransactionType.PROPERTY, "Collected rent for ${prop.name}", amount)
            current.copy(cash = current.cash + amount, properties = updated)
        }
    }

    fun collectAllPropertyRent() {
        _state.update { current ->
            val total = current.properties.filter { it.isOwned }.sumOf { it.accumulatedRent }
            if (total <= 0) return@update current

            val updated = current.properties.map {
                if (it.isOwned) it.copy(accumulatedRent = 0.0) else it
            }
            logTransaction(TransactionType.PROPERTY, "Collected all real estate rent", total)
            current.copy(cash = current.cash + total, properties = updated)
        }
    }

    fun sellProperty(propertyId: String) {
        _state.update { current ->
            val prop = current.properties.find { it.id == propertyId } ?: return@update current
            if (!prop.isOwned) return@update current

            val payout = (prop.currentValuation * 0.85) + prop.accumulatedRent
            val updated = current.properties.map {
                if (it.id == propertyId) {
                    it.copy(
                        isOwned = false,
                        conditionPercent = 100,
                        accumulatedRent = 0.0,
                        upgrades = it.upgrades.map { u -> u.copy(isPurchased = false) }
                    )
                } else it
            }
            logTransaction(TransactionType.PROPERTY, "Sold ${prop.name}", payout)
            current.copy(cash = current.cash + payout, properties = updated)
        }
    }

    // --- Stock Market Actions ---
    fun buyStock(symbol: String, shares: Long) {
        if (shares <= 0) return
        _state.update { current ->
            val stock = current.stocks.find { it.symbol == symbol } ?: return@update current
            val totalCost = stock.currentPrice * shares
            if (current.cash < totalCost) return@update current

            val updatedStocks = current.stocks.map {
                if (it.symbol == symbol) {
                    it.copy(
                        sharesOwned = it.sharesOwned + shares,
                        totalCostBasis = it.totalCostBasis + totalCost
                    )
                } else it
            }
            logTransaction(TransactionType.STOCKS, "Bought $shares shares of $symbol", -totalCost)
            current.copy(cash = current.cash - totalCost, stocks = updatedStocks)
        }
    }

    fun sellStock(symbol: String, shares: Long) {
        if (shares <= 0) return
        _state.update { current ->
            val stock = current.stocks.find { it.symbol == symbol } ?: return@update current
            if (stock.sharesOwned < shares) return@update current

            val revenue = stock.currentPrice * shares
            val portionOfCostBasis = (shares.toDouble() / stock.sharesOwned) * stock.totalCostBasis
            val profit = revenue - portionOfCostBasis

            val updatedStocks = current.stocks.map {
                if (it.symbol == symbol) {
                    val remainingShares = it.sharesOwned - shares
                    val remainingCost = if (remainingShares > 0) it.totalCostBasis - portionOfCostBasis else 0.0
                    it.copy(sharesOwned = remainingShares, totalCostBasis = remainingCost)
                } else it
            }
            val desc = "Sold $shares shares of $symbol (P&L: ${if (profit >= 0) "+$" else "-$"}${Math.abs(profit).toInt()})"
            logTransaction(TransactionType.STOCKS, desc, revenue)
            current.copy(cash = current.cash + revenue, stocks = updatedStocks)
        }
    }

    // --- Crypto Market Actions ---
    fun buyCrypto(symbol: String, amountUsd: Double) {
        if (amountUsd <= 0) return
        _state.update { current ->
            val crypto = current.cryptos.find { it.symbol == symbol } ?: return@update current
            if (current.cash < amountUsd) return@update current

            val coinsPurchased = amountUsd / crypto.currentPrice
            val updatedCryptos = current.cryptos.map {
                if (it.symbol == symbol) {
                    it.copy(
                        coinsOwned = it.coinsOwned + coinsPurchased,
                        totalCostBasis = it.totalCostBasis + amountUsd
                    )
                } else it
            }
            logTransaction(TransactionType.CRYPTO, "Bought ${String.format("%.4f", coinsPurchased)} $symbol", -amountUsd)
            current.copy(cash = current.cash - amountUsd, cryptos = updatedCryptos)
        }
    }

    fun sellCrypto(symbol: String, percentOfHolding: Double) {
        _state.update { current ->
            val crypto = current.cryptos.find { it.symbol == symbol } ?: return@update current
            if (crypto.coinsOwned <= 0.0) return@update current

            val coinsToSell = crypto.coinsOwned * percentOfHolding.coerceIn(0.01, 1.0)
            val revenue = coinsToSell * crypto.currentPrice
            val portionOfBasis = (coinsToSell / crypto.coinsOwned) * crypto.totalCostBasis

            val updatedCryptos = current.cryptos.map {
                if (it.symbol == symbol) {
                    val remainingCoins = crypto.coinsOwned - coinsToSell
                    val remainingBasis = if (remainingCoins > 0.000001) crypto.totalCostBasis - portionOfBasis else 0.0
                    it.copy(coinsOwned = remainingCoins, totalCostBasis = remainingBasis)
                } else it
            }
            logTransaction(TransactionType.CRYPTO, "Sold ${String.format("%.4f", coinsToSell)} $symbol", revenue)
            current.copy(cash = current.cash + revenue, cryptos = updatedCryptos)
        }
    }

    // --- Collectibles Actions ---
    fun buyCollectible(id: String) {
        _state.update { current ->
            val item = current.collectibles.find { it.id == id } ?: return@update current
            if (item.isOwned || current.cash < item.purchasePrice) return@update current
            val holdHours = listOf(1L, 3L, 6L, 12L, 24L, 72L)[item.rarity.ordinal]
            val now = System.currentTimeMillis()
            val updated = current.collectibles.map { if (it.id == id) it.copy(isOwned = true, holdUntil = now + holdHours * 3600000L) else it }
            val tx = createTransaction(TransactionType.COLLECTIBLE, "Acquired " + item.name, -item.purchasePrice)
            current.copy(cash = current.cash - item.purchasePrice, collectibles = updated, transactions = (listOf(tx) + current.transactions).take(200))
        }
    }

    fun sellCollectible(id: String) {
        _state.update { current ->
            val item = current.collectibles.find { it.id == id } ?: return@update current
            if (!item.isOwned || System.currentTimeMillis() < item.holdUntil) return@update current
            val payout = item.currentValuation * item.resalePercent.coerceIn(0.50, 0.95)
            val updated = current.collectibles.map { if (it.id == id) it.copy(isOwned = false, holdUntil = 0L) else it }
            val tx = createTransaction(TransactionType.COLLECTIBLE, "Sold " + item.name + " (resale " + (item.resalePercent * 100).toInt() + "%)", payout)
            current.copy(cash = current.cash + payout, collectibles = updated, transactions = (listOf(tx) + current.transactions).take(200))
        }
    }

    // --- Research Actions ---
    fun buildResearchLab() {
        _state.update { current ->
            val cost = 100000.0
            if (current.researchLabBuilt || current.cash < cost || current.progression.level < 5) return@update current
            val tx = createTransaction(TransactionType.RESEARCH, "Constructed Research & Development Laboratory", -cost)
            current.copy(cash = current.cash - cost, researchLabBuilt = true, researchPoints = current.researchPoints + 50.0, transactions = (listOf(tx) + current.transactions).take(200))
        }
    }

    fun upgradeResearchSlot() {
        _state.update { current ->
            val cost = listOf(150000.0, 1500000.0, 15000000.0).getOrNull(current.researchSlots - 1) ?: return@update current
            if (!current.researchLabBuilt || current.researchSlots >= 4 || current.cash < cost) return@update current
            current.copy(cash = current.cash - cost, researchSlots = current.researchSlots + 1)
        }
    }

    fun unlockResearch(id: String) {
        _state.update { current ->
            val node = current.research.find { it.id == id } ?: return@update current
            if (!current.researchLabBuilt || node.isUnlocked || current.activeResearch.any { it.researchId == id }) return@update current
            if (current.activeResearch.size >= current.researchSlots || current.cash < node.cost || current.researchPoints < node.researchPointsCost || current.progression.level < node.minimumPlayerLevel) return@update current
            val prerequisite = node.requiredResearchId?.let { req -> current.research.any { it.id == req && it.isUnlocked } } ?: true
            if (!prerequisite) return@update current
            val now = System.currentTimeMillis()
            val project = LocalResearchProject(id, now, now + (node.durationHours * 3600000.0).toLong(), node.cost, node.researchPointsCost, current.activeResearch.size)
            val tx = createTransaction(TransactionType.RESEARCH, "Started research: " + node.name, -node.cost)
            current.copy(cash = current.cash - node.cost, researchPoints = current.researchPoints - node.researchPointsCost, activeResearch = current.activeResearch + project, transactions = (listOf(tx) + current.transactions).take(200))
        }
    }

    fun placeMarketOrder(market: String, symbol: String, side: String, kind: String, quantity: Double, limitPrice: Double = 0.0, triggerPrice: Double = 0.0) {
        _state.update { current -> runCatching { LocalMarketEngine.place(current, market, symbol, side, kind, quantity, limitPrice, triggerPrice) }.getOrElse { current } }
    }

    fun cancelMarketOrder(id: String) {
        _state.update { current -> LocalMarketEngine.cancel(current, id) }
    }

    fun foundCorporation(name: String, tag: String) {
        _state.update { current -> runCatching { LocalCorporationEngine.found(current, name, tag) }.getOrElse { current } }
    }

    fun contributeToCorporation(amount: Double) {
        _state.update { current -> runCatching { LocalCorporationEngine.contribute(current, amount) }.getOrElse { current } }
    }

    fun corporationManagementShift() {
        _state.update { current -> runCatching { LocalCorporationEngine.managementShift(current) }.getOrElse { current } }
    }

    fun upgradeCorporationFacility(id: String) {
        _state.update { current -> runCatching { LocalCorporationEngine.upgradeFacility(current, id) }.getOrElse { current } }
    }

    fun establishCorporationDepartment(id: String) {
        _state.update { current -> runCatching { LocalCorporationEngine.establishDepartment(current, id) }.getOrElse { current } }
    }

    fun acquireCorporationBusiness(id: String) {
        _state.update { current -> runCatching { LocalCorporationEngine.acquireBusiness(current, id) }.getOrElse { current } }
    }

    fun claimCorporationObjective(id: String) {
        _state.update { current -> LocalCorporationEngine.claimObjective(current, id) }
    }
    // --- Casino Chip Exchange & Games ---
    fun buyChips(cashAmount: Double) {
        _state.update { current ->
            if (cashAmount <= 0 || current.cash < cashAmount) return@update current
            val chipsToAdd = (cashAmount / 100.0).toLong()
            val actualCashSpent = chipsToAdd * 100.0
            logTransaction(TransactionType.CASINO, "Exchanged cash for $chipsToAdd chips", -actualCashSpent)
            current.copy(
                cash = current.cash - actualCashSpent,
                casinoChips = current.casinoChips + chipsToAdd
            )
        }
    }

    fun cashOutChips(chips: Long) {
        _state.update { current ->
            if (chips <= 0 || current.casinoChips < chips) return@update current
            val cashToAdd = chips * 100.0
            logTransaction(TransactionType.CASINO, "Cashed out $chips chips", cashToAdd)
            current.copy(
                cash = current.cash + cashToAdd,
                casinoChips = current.casinoChips - chips
            )
        }
    }

    fun recordCasinoResult(chipsWon: Long, chipsBet: Long) {
        _state.update { current ->
            val netChips = chipsWon - chipsBet
            val newChips = (current.casinoChips + netChips).coerceAtLeast(0)
            val stats = current.casinoStats
            val updatedStats = stats.copy(
                totalWonChips = if (netChips > 0) stats.totalWonChips + netChips else stats.totalWonChips,
                totalLostChips = if (netChips < 0) stats.totalLostChips + Math.abs(netChips) else stats.totalLostChips,
                gamesPlayed = stats.gamesPlayed + 1,
                biggestWinChips = if (chipsWon > stats.biggestWinChips) chipsWon else stats.biggestWinChips,
                totalWageredChips = stats.totalWageredChips + chipsBet,
                currentWinStreak = if (netChips > 0) stats.currentWinStreak + 1 else 0,
                bestWinStreak = maxOf(stats.bestWinStreak, if (netChips > 0) stats.currentWinStreak + 1 else stats.currentWinStreak),
                currentLossStreak = if (netChips < 0) stats.currentLossStreak + 1 else 0
            )
            current.copy(casinoChips = newChips, casinoStats = updatedStats)
        }
    }

    // --- Achievements Reward Claim ---
    fun claimAchievement(id: String) {
        _state.update { current ->
            val ach = current.achievements.find { it.id == id } ?: return@update current
            if (!ach.isUnlocked || ach.isClaimed) return@update current

            val updated = current.achievements.map {
                if (it.id == id) it.copy(isClaimed = true) else it
            }
            logTransaction(TransactionType.CLICKER, "Claimed reward for ${ach.title}", ach.rewardCash)
            current.copy(cash = current.cash + ach.rewardCash, achievements = updated)
        }
    }

    private fun evaluateAchievements(state: GameState): List<Achievement> {
        val totalNetWorth = state.totalNetWorth
        val ownedBizCount = state.businesses.count { it.isOwned }
        val totalBranches = state.businesses.filter { it.isOwned }.sumOf { it.currentBranches }
        val totalEmployees = state.businesses.filter { it.isOwned }.sumOf { it.employeeCount }
        val ownedProperties = state.properties.count { it.isOwned }
        val pristineProps = state.properties.count { it.isOwned && it.conditionPercent == 100 }
        val stocksOwned = state.stocks.sumOf { it.sharesOwned }
        val cryptoValue = state.cryptos.sumOf { it.holdingsValue }
        val ownedCollectibles = state.collectibles.count { it.isOwned }
        val highTierCollectibles = state.collectibles.count { it.isOwned && (it.rarity == Rarity.EPIC || it.rarity == Rarity.LEGENDARY || it.rarity == Rarity.MYTHICAL) }
        val completedResearch = state.research.count { it.isUnlocked }

        return state.achievements.map { ach ->
            val currentProgress = when (ach.id) {
                "ach_1", "ach_2", "ach_3", "ach_4", "ach_5", "ach_6", "ach_7" -> totalNetWorth
                "ach_8" -> ownedBizCount.toDouble()
                "ach_9" -> totalBranches.toDouble()
                "ach_10" -> totalEmployees.toDouble()
                "ach_11" -> state.businesses.filter { it.isOwned }.map { it.industry }.distinct().size.toDouble()
                "ach_12" -> state.businesses.filter { it.isOwned }.sumOf { it.grossRevenuePerSec }
                "ach_13", "ach_14" -> ownedProperties.toDouble()
                "ach_15" -> pristineProps.toDouble()
                "ach_16" -> if (state.properties.find { it.id == "prop_10" }?.isOwned == true) 1.0 else 0.0
                "ach_17" -> stocksOwned.toDouble()
                "ach_19" -> cryptoValue
                "ach_21" -> ownedCollectibles.toDouble()
                "ach_22" -> highTierCollectibles.toDouble()
                "ach_23" -> state.casinoStats.gamesPlayed.toDouble()
                "ach_24" -> state.casinoStats.biggestWinChips.toDouble()
                "ach_25" -> completedResearch.toDouble()
                else -> ach.currentProgress
            }
            val unlocked = currentProgress >= ach.targetProgress
            ach.copy(currentProgress = currentProgress, isUnlocked = unlocked || ach.isUnlocked)
        }
    }

    private fun createTransaction(type: TransactionType, desc: String, amount: Double): Transaction {
        return Transaction(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            type = type,
            description = desc,
            amount = amount
        )
    }

    private fun logTransaction(type: TransactionType, desc: String, amount: Double) {
        val tx = createTransaction(type, desc, amount)
        _state.update { current ->
            current.copy(transactions = (listOf(tx) + current.transactions).take(50))
        }
    }

    fun updatePlayerProfile(profile: LocalPlayerProfile) {
        _state.update { it.copy(profile = profile) }
    }

    fun updateGamePreferences(preferences: LocalGamePreferences) {
        _state.update { it.copy(preferences = preferences) }
    }
    fun resetGame() {
        viewModelScope.launch {
            val fresh = repository.resetGame()
            _state.value = fresh
        }
    }
}
