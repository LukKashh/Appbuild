package com.example.engine

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/**
 * Device-local simulation layer for the Capital Fortune Android edition.
 *
 * This module deliberately has no Firebase, HTTP, or account-service dependency.
 * All commands are deterministic when supplied with the same state, command and timestamp.
 * UI code should call this engine and persist the returned state as one transaction.
 */
data class LocalPlayerProgress(
    val xp: Double = 0.0,
    val level: Int = 1,
    val levelRewarded: Int = 1,
    val clickCapBonus: Int = 0,
    val researchPoints: Double = 0.0,
    val lifetimeRevenue: Double = 0.0,
    val lifetimeExpenses: Double = 0.0,
    val lifetimeTrades: Long = 0,
    val lifetimeClicks: Long = 0,
    val researchDone: Int = 0,
    val collectiblesBought: Int = 0,
    val collectionsDone: Int = 0,
    val clickerLevel: Int = 1,
    val companiesBought: Int = 0,
    val companiesSold: Int = 0,
    val branchesOpened: Int = 0,
    val upgradesPurchased: Int = 0,
    val hires: Int = 0,
    val propertiesBought: Int = 0,
    val propertiesSold: Int = 0,
    val maintenance: Int = 0,
    val corpContributed: Double = 0.0,
    val stockTrades: Long = 0,
    val cryptoTrades: Long = 0,
    val dividends: Double = 0.0,
    val rentEarned: Double = 0.0
)

data class LocalEmployeeRole(
    val id: String,
    val title: String,
    val headcount: Int,
    val salaryPerHour: Double,
    val productivity: Double
)

data class LocalCompanyUpgrade(
    val id: String,
    val name: String,
    val cost: Double,
    val revenueMultiplier: Double = 1.0,
    val expenseMultiplier: Double = 1.0,
    val level: Int = 0,
    val maxLevel: Int = 5,
    val costGrowth: Double = 1.65
)

data class LocalCompanyInstance(
    val id: String,
    val definitionId: String,
    val name: String,
    val industry: String,
    val purchasePrice: Double,
    val branches: Int = 1,
    val branchCapacity: Int = 1,
    val staff: List<LocalEmployeeRole> = emptyList(),
    val upgrades: List<LocalCompanyUpgrade> = emptyList(),
    val hourlyBaseRevenue: Double,
    val hourlyFixedExpense: Double,
    val reserves: Double = 0.0,
    val liabilities: Double = 0.0,
    val uncollected: Double = 0.0,
    val suspended: Boolean = false,
    val totalRevenue: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val lastOperatingAt: Long = 0L
) {
    val staffCount: Int get() = staff.sumOf { it.headcount }
    val hourlyPayroll: Double get() = staff.sumOf { it.headcount * it.salaryPerHour }
    val upgradeRevenueMultiplier: Double get() = upgrades.fold(1.0) { a, u -> a * u.revenueMultiplier.pow(u.level) }
    val upgradeExpenseMultiplier: Double get() = upgrades.fold(1.0) { a, u -> a * u.expenseMultiplier.pow(u.level) }
    val grossPerHour: Double get() = if (suspended) 0.0 else hourlyBaseRevenue * branches * upgradeRevenueMultiplier *
        (1.0 + staff.sumOf { it.headcount * it.productivity } * 0.01)
    val expensesPerHour: Double get() = if (suspended) 0.0 else
        (hourlyFixedExpense * branches + hourlyPayroll * branches) * upgradeExpenseMultiplier
    val netPerHour: Double get() = grossPerHour - expensesPerHour
}

data class LocalPropertyInstance(
    val id: String,
    val definitionId: String,
    val name: String,
    val category: String,
    val purchasePrice: Double,
    val marketValue: Double,
    val baseRentPerHour: Double,
    val condition: Int = 100,
    val maintenancePerDay: Double = 0.0,
    val upgrades: Int = 0,
    val accumulatedRent: Double = 0.0,
    val lastEvaluatedAt: Long = 0L
) {
    val conditionMultiplier: Double get() = (condition / 100.0).coerceIn(0.2, 1.0)
    val rentPerHour: Double get() = baseRentPerHour * conditionMultiplier * (1.0 + upgrades * 0.12)
}

enum class LocalOrderKind { MARKET, LIMIT, STOP, TAKE_PROFIT }
enum class LocalOrderSide { BUY, SELL }
enum class LocalOrderStatus { OPEN, TRIGGERED, PARTIALLY_FILLED, EXECUTED, CANCELLED, EXPIRED }

data class LocalMarketOrder(
    val id: String,
    val symbol: String,
    val market: String,
    val side: LocalOrderSide,
    val kind: LocalOrderKind,
    val quantity: Double,
    val filledQuantity: Double = 0.0,
    val limitPrice: Double? = null,
    val triggerPrice: Double? = null,
    val reservedCash: Double = 0.0,
    val averageFillPrice: Double = 0.0,
    val feePaid: Double = 0.0,
    val createdAt: Long,
    val expiresAt: Long,
    val status: LocalOrderStatus = LocalOrderStatus.OPEN
)

data class LocalHolding(val quantity: Double = 0.0, val reservedQuantity: Double = 0.0, val costBasis: Double = 0.0)

data class LocalCollectibleHolding(
    val instanceId: String,
    val definitionId: String,
    val acquisitionPrice: Double,
    val acquiredAt: Long,
    val sellableAt: Long,
    val currentValue: Double
)

data class LocalResearchProject(
    val id: String,
    val name: String,
    val costCash: Double,
    val requiredPoints: Double,
    val progressPoints: Double = 0.0,
    val durationMillis: Long,
    val startedAt: Long,
    val completesAt: Long,
    val effects: Map<String, Double> = emptyMap()
)

data class LocalCorporationMember(
    val playerId: String,
    val displayName: String,
    val rank: String,
    val department: String?,
    val joinedAt: Long,
    val contributionDay: Long = 0L,
    val contributionToday: Double = 0.0,
    val lifetimeContribution: Double = 0.0,
    val shiftsCompleted: Int = 0,
    val isNpc: Boolean = false
)

data class LocalCorporationDepartment(
    val id: String,
    val officerPlayerId: String? = null,
    val establishedAt: Long
)

data class LocalCorporationBusiness(
    val instanceId: String,
    val definitionId: String,
    val acquiredAt: Long,
    val lifetimeNetIncome: Double = 0.0
)

data class LocalCorporationObjective(
    val id: String,
    val title: String,
    val type: String,
    val target: Double,
    val progress: Double = 0.0,
    val expiresAt: Long,
    val minParticipants: Int = 1,
    val participants: Set<String> = emptySet(),
    val corporationXpReward: Long,
    val treasuryReward: Double,
    val memberXpReward: Double,
    val milestone: Boolean = false
)

data class LocalCorporationCompletedObjective(
    val title: String,
    val completedAt: Long,
    val succeeded: Boolean
)

data class LocalCorporation(
    val id: String,
    val name: String,
    val tag: String,
    val founderId: String,
    val recruitmentMode: String = "open",
    val description: String = "",
    val createdAt: Long,
    val level: Int = 1,
    val experience: Long = 0,
    val treasury: Double = 0.0,
    val members: List<LocalCorporationMember> = emptyList(),
    val facilities: Map<String, Int> = emptyMap(),
    val departments: Map<String, LocalCorporationDepartment> = emptyMap(),
    val businesses: List<LocalCorporationBusiness> = emptyList(),
    val objectives: List<LocalCorporationObjective> = emptyList(),
    val completedObjectives: List<LocalCorporationCompletedObjective> = emptyList(),
    val objectiveCooldownUntil: Long = 0L,
    val objectiveSequence: Long = 0L,
    val milestoneIndex: Int = 0,
    val lastTick: Long = 0L,
    val activityLog: List<String> = emptyList(),
    val pendingMemberXp: Map<String, Long> = emptyMap(),
    val dissolved: Boolean = false
)

data class LocalLedgerEntry(
    val id: String,
    val timestamp: Long,
    val category: String,
    val description: String,
    val amount: Double,
    val balanceAfter: Double
)

data class LocalCapacityUpgrade(val completesAt: Long, val cost: Double)

data class LocalSimulationState(
    val cash: Double = 50.0,
    val reservedCash: Double = 0.0,
    val playerId: String = "local-player",
    val playerName: String = "Player",
    val clickerCount: Int = 0,
    val clickTokens: Double = 150.0,
    val clickTokenAt: Long = 0L,
    val corporationCooldownUntil: Long = 0L,
    val lastCorporationShiftAt: Long = 0L,
    val progress: LocalPlayerProgress = LocalPlayerProgress(),
    val companies: List<LocalCompanyInstance> = emptyList(),
    val referenceCompanies: List<ReferenceOwnedCompany> = emptyList(),
    val businessCapacitySlots: Int = 3,
    val capacityLevel: Int = 0,
    val capacityUpgrade: LocalCapacityUpgrade? = null,
    val properties: List<LocalPropertyInstance> = emptyList(),
    val referenceProperties: List<ReferenceOwnedProperty> = emptyList(),
    val soldProperties: List<ReferenceSoldProperty> = emptyList(),
    val stockHoldings: Map<String, LocalHolding> = emptyMap(),
    val cryptoHoldings: Map<String, LocalHolding> = emptyMap(),
    val stockLastEvalTick: Long = 0L,
    val cryptoLastEvalTick: Long = 0L,
    val lastDividendDay: Long = 0L,
    val marketOrders: List<LocalMarketOrder> = emptyList(),
    val researchProjects: List<LocalResearchProject> = emptyList(),
    val researchBuilt: Boolean = false,
    val researchSlots: Int = 1,
    val completedResearch: Set<String> = emptySet(),
    val collectibles: List<LocalCollectibleHolding> = emptyList(),
    val collectionsRewarded: Set<String> = emptySet(),
    val achievementCycles: Map<String, Int> = emptyMap(),
    val unlockedAchievements: Map<String, Long> = emptyMap(),
    val limitedCollectibleCopies: Map<String, Int> = emptyMap(),
    val collectibleSales: List<ReferenceCollectibleSale> = emptyList(),
    val corporation: LocalCorporation? = null,
    val ledger: List<LocalLedgerEntry> = emptyList(),
    val lastSimulationAt: Long = 0L,
    val nextSequence: Long = 1L
)

data class LocalOfflineLine(val label: String, val gross: Double, val expenses: Double, val net: Double, val eligible: Boolean, val note: String = "")
data class LocalOfflineSummary(val elapsedMillis: Long, val lines: List<LocalOfflineLine>, val totalNet: Double, val cappedMillis: Long, val researchPoints: Double = 0.0)

sealed class LocalGameResult {
    data class Success(val state: LocalSimulationState, val message: String) : LocalGameResult()
    data class Rejected(val state: LocalSimulationState, val reason: String) : LocalGameResult()
}

/**
 * Core local simulation commands. UI must never mutate LocalSimulationState directly.
 * Callers should persist Success.state atomically before rendering it as committed.
 */
object LocalGameEngine {
    const val MAX_OFFLINE_HOURS = 12L
    const val COMPANY_RESERVE_TARGET_HOURS = 24.0
    const val COMPANY_MINIMUM_HOLD_HOURS = 6L
    const val PROPERTY_CONDITION_LOSS_PER_DAY = 0.20
    const val STOCK_FEE_RATE = 0.005
    const val CRYPTO_FEE_RATE = 0.0025
    const val DEFAULT_ORDER_LIFETIME_DAYS = 7L

    fun upgradeBusinessCapacity(state: LocalSimulationState, now: Long): LocalGameResult {
        if (state.capacityUpgrade != null) return reject(state, "A business capacity upgrade is already in progress")
        val cost = 50_000.0 * 2.2.pow(state.capacityLevel)
        if (state.cash - state.reservedCash < cost) return reject(state, "Insufficient available funds")
        val durationHours = 0.5 * 1.5.pow(state.capacityLevel)
        val upgrade = LocalCapacityUpgrade(now + (durationHours * 3_600_000.0).toLong(), cost)
        val next = state.copy(cash = state.cash - cost, capacityUpgrade = upgrade)
        return success(ledger(next, now, "CAPACITY_UPGRADE", "Started business management upgrade", -cost), "Business capacity upgrade started")
    }

    fun buyCompany(state: LocalSimulationState, company: LocalCompanyInstance, now: Long): LocalGameResult {
        if (state.companies.any { it.id == company.id }) return reject(state, "Company already owned")
        if (company.purchasePrice <= 0.0) return reject(state, "Invalid purchase price")
        if (state.cash - state.reservedCash < company.purchasePrice) return reject(state, "Insufficient available cash")
        val next = state.copy(cash = state.cash - company.purchasePrice, companies = state.companies + company.copy(lastOperatingAt = now))
        return success(ledger(next, now, "COMPANY_PURCHASE", "Acquired ${company.name}", -company.purchasePrice), "Company acquired")
    }

    fun openBranch(state: LocalSimulationState, companyId: String, cost: Double): LocalGameResult {
        val company = state.companies.firstOrNull { it.id == companyId } ?: return reject(state, "Company not found")
        if (company.branches >= company.branchCapacity) return reject(state, "Branch capacity reached")
        if (cost <= 0 || state.cash - state.reservedCash < cost) return reject(state, "Insufficient available cash")
        val updated = company.copy(branches = company.branches + 1)
        val next = state.copy(cash = state.cash - cost, companies = state.companies.map { if (it.id == companyId) updated else it })
        return success(ledger(next, System.currentTimeMillis(), "BRANCH_OPENED", "Opened branch for ${company.name}", -cost), "Branch opened")
    }

    fun hireStaff(state: LocalSimulationState, companyId: String, roleId: String, count: Int, salary: Double, productivity: Double, recruitmentCost: Double): LocalGameResult {
        val company = state.companies.firstOrNull { it.id == companyId } ?: return reject(state, "Company not found")
        if (count <= 0 || salary < 0 || productivity < 0 || recruitmentCost < 0) return reject(state, "Invalid staffing values")
        if (state.cash - state.reservedCash < recruitmentCost) return reject(state, "Insufficient available cash")
        val roles = company.staff.toMutableList()
        val index = roles.indexOfFirst { it.id == roleId }
        if (index >= 0) roles[index] = roles[index].copy(headcount = roles[index].headcount + count)
        else roles += LocalEmployeeRole(roleId, roleId, count, salary, productivity)
        val next = state.copy(cash = state.cash - recruitmentCost, companies = state.companies.map { if (it.id == companyId) it.copy(staff = roles) else it })
        return success(ledger(next, System.currentTimeMillis(), "STAFF_RECRUITED", "Recruited $count staff for ${company.name}", -recruitmentCost), "Staff recruited")
    }

    fun upgradeCompany(state: LocalSimulationState, companyId: String, upgradeId: String): LocalGameResult {
        val company = state.companies.firstOrNull { it.id == companyId } ?: return reject(state, "Company not found")
        val upgrade = company.upgrades.firstOrNull { it.id == upgradeId } ?: return reject(state, "Upgrade not found")
        if (upgrade.level >= upgrade.maxLevel) return reject(state, "Upgrade is at maximum level")
        val cost = upgrade.cost * upgrade.costGrowth.pow(upgrade.level)
        if (state.cash - state.reservedCash < cost) return reject(state, "Insufficient available cash")
        val updated = company.copy(upgrades = company.upgrades.map { if (it.id == upgradeId) it.copy(level = it.level + 1) else it })
        val next = state.copy(cash = state.cash - cost, companies = state.companies.map { if (it.id == companyId) updated else it })
        return success(ledger(next, System.currentTimeMillis(), "COMPANY_UPGRADE", "Upgraded ${upgrade.name}", -cost), "Upgrade purchased")
    }

    fun collectCompanyWallet(state: LocalSimulationState, companyId: String, now: Long): LocalGameResult {
        val company = state.companies.firstOrNull { it.id == companyId } ?: return reject(state, "Company not found")
        if (company.uncollected <= 0.0) return reject(state, "No funds available to collect")
        val amount = company.uncollected
        val next = state.copy(cash = state.cash + amount, companies = state.companies.map { if (it.id == companyId) it.copy(uncollected = 0.0) else it })
        return success(ledger(next, now, "COMPANY_COLLECTION", "Collected income from ${company.name}", amount), "Income collected")
    }

    fun tick(state: LocalSimulationState, now: Long, offline: Boolean = false): Pair<LocalSimulationState, LocalOfflineSummary> {
        val elapsed = (now - state.lastSimulationAt).coerceAtLeast(0L)
        val maxElapsed = if (offline) MAX_OFFLINE_HOURS * 3_600_000L else elapsed
        val applied = min(elapsed, maxElapsed)
        if (applied == 0L) return state.copy(lastSimulationAt = now) to LocalOfflineSummary(elapsed, emptyList(), 0.0, 0L)
        val hours = applied / 3_600_000.0
        val lines = mutableListOf<LocalOfflineLine>()
        var totalNet = 0.0
        val companies = state.companies.map { c ->
            if (c.suspended) {
                lines += LocalOfflineLine(c.name, 0.0, 0.0, 0.0, false); c
            } else {
                val gross = c.grossPerHour * hours
                val expenses = c.expensesPerHour * hours
                val net = gross - expenses
                totalNet += net
                lines += LocalOfflineLine(c.name, gross, expenses, net, true)
                c.copy(uncollected = c.uncollected + net, totalRevenue = c.totalRevenue + gross,
                    totalExpenses = c.totalExpenses + expenses, lastOperatingAt = now)
            }
        }
        val properties = state.properties.map { p ->
            val rent = p.rentPerHour * hours
            totalNet += rent
            lines += LocalOfflineLine(p.name, rent, 0.0, rent, true)
            val days = applied / 86_400_000.0
            p.copy(accumulatedRent = p.accumulatedRent + rent,
                condition = (p.condition - (days * PROPERTY_CONDITION_LOSS_PER_DAY).toInt()).coerceIn(0, 100),
                lastEvaluatedAt = now)
        }
        val progress = state.progress.copy(
            lifetimeRevenue = state.progress.lifetimeRevenue + lines.sumOf { it.gross },
            lifetimeExpenses = state.progress.lifetimeExpenses + lines.sumOf { it.expenses },
            xp = state.progress.xp
        )
        val capacityCompleted = state.capacityUpgrade?.let { it.completesAt <= now } == true
        val next = state.copy(
            companies = companies,
            properties = properties,
            progress = withLevel(progress),
            businessCapacitySlots = if (capacityCompleted) state.businessCapacitySlots + 1 else state.businessCapacitySlots,
            capacityLevel = if (capacityCompleted) state.capacityLevel + 1 else state.capacityLevel,
            capacityUpgrade = if (capacityCompleted) null else state.capacityUpgrade,
            lastSimulationAt = now
        )
        return next to LocalOfflineSummary(elapsed, lines, totalNet, elapsed - applied)
    }

    fun placeOrder(state: LocalSimulationState, order: LocalMarketOrder, currentPrice: Double, now: Long): LocalGameResult {
        if (order.quantity <= 0 || currentPrice <= 0) return reject(state, "Invalid order quantity or price")
        if (state.marketOrders.any { it.id == order.id }) return reject(state, "Duplicate order")
        val feeRate = if (order.market.equals("crypto", true)) CRYPTO_FEE_RATE else STOCK_FEE_RATE
        val isImmediate = order.kind == LocalOrderKind.MARKET ||
            (order.kind == LocalOrderKind.LIMIT && order.side == LocalOrderSide.BUY && currentPrice <= (order.limitPrice ?: 0.0)) ||
            (order.kind == LocalOrderKind.LIMIT && order.side == LocalOrderSide.SELL && currentPrice >= (order.limitPrice ?: Double.MAX_VALUE))
        if (order.side == LocalOrderSide.BUY) {
            val reserve = if (isImmediate) order.quantity * currentPrice * (1.0 + feeRate)
                else order.quantity * (order.limitPrice ?: order.triggerPrice ?: currentPrice) * (1.0 + feeRate)
            if (state.cash - state.reservedCash < reserve) return reject(state, "Insufficient available cash to reserve order")
            val pending = order.copy(reservedCash = reserve, createdAt = now,
                expiresAt = now + DEFAULT_ORDER_LIFETIME_DAYS * 86_400_000L)
            val reservedState = state.copy(reservedCash = state.reservedCash + reserve, marketOrders = state.marketOrders + pending)
            return if (isImmediate) executeOrder(reservedState, pending, currentPrice, now)
                else success(reservedState, "Buy order placed and funds reserved")
        }
        val holdings = if (order.market.equals("crypto", true)) state.cryptoHoldings else state.stockHoldings
        val holding = holdings[order.symbol] ?: return reject(state, "No holdings for sell order")
        if (holding.quantity - holding.reservedQuantity < order.quantity) return reject(state, "Insufficient unreserved holdings")
        val updatedHolding = holding.copy(reservedQuantity = holding.reservedQuantity + order.quantity)
        val map = holdings + (order.symbol to updatedHolding)
        val next = if (order.market.equals("crypto", true)) state.copy(cryptoHoldings = map, marketOrders = state.marketOrders + order.copy(createdAt = now, expiresAt = now + DEFAULT_ORDER_LIFETIME_DAYS * 86_400_000L))
            else state.copy(stockHoldings = map, marketOrders = state.marketOrders + order.copy(createdAt = now, expiresAt = now + DEFAULT_ORDER_LIFETIME_DAYS * 86_400_000L))
        return if (isImmediate) executeOrder(next, order, currentPrice, now) else success(next, "Sell order placed; holdings reserved")
    }

    fun evaluateOrders(state: LocalSimulationState, prices: Map<String, Double>, now: Long): LocalSimulationState {
        var current = state
        current.marketOrders.forEach { order ->
            if (order.status !in setOf(LocalOrderStatus.OPEN, LocalOrderStatus.TRIGGERED, LocalOrderStatus.PARTIALLY_FILLED)) return@forEach
            if (now >= order.expiresAt) { current = cancelOrder(current, order.id, expired = true); return@forEach }
            val price = prices[order.symbol] ?: return@forEach
            val triggered = when (order.kind) {
                LocalOrderKind.MARKET -> true
                LocalOrderKind.LIMIT -> if (order.side == LocalOrderSide.BUY) price <= (order.limitPrice ?: 0.0) else price >= (order.limitPrice ?: Double.MAX_VALUE)
                LocalOrderKind.STOP -> if (order.side == LocalOrderSide.BUY) price >= (order.triggerPrice ?: Double.MAX_VALUE) else price <= (order.triggerPrice ?: 0.0)
                LocalOrderKind.TAKE_PROFIT -> if (order.side == LocalOrderSide.BUY) price <= (order.triggerPrice ?: 0.0) else price >= (order.triggerPrice ?: Double.MAX_VALUE)
            }
            if (triggered) {
                val live = current.marketOrders.firstOrNull { it.id == order.id } ?: return@forEach
                current = executeOrder(current, live.copy(status = LocalOrderStatus.TRIGGERED), price, now).let { if (it is LocalGameResult.Success) it.state else current }
            }
        }
        return current
    }

    fun cancelOrder(state: LocalSimulationState, orderId: String, expired: Boolean = false): LocalSimulationState {
        val order = state.marketOrders.firstOrNull { it.id == orderId } ?: return state
        if (order.status !in setOf(LocalOrderStatus.OPEN, LocalOrderStatus.TRIGGERED, LocalOrderStatus.PARTIALLY_FILLED)) return state
        var next = state.copy(marketOrders = state.marketOrders.map { if (it.id == orderId) it.copy(status = if (expired) LocalOrderStatus.EXPIRED else LocalOrderStatus.CANCELLED) else it },
            reservedCash = (state.reservedCash - order.reservedCash).coerceAtLeast(0.0))
        if (order.side == LocalOrderSide.SELL) {
            val holdings = if (order.market.equals("crypto", true)) next.cryptoHoldings else next.stockHoldings
            val h = holdings[order.symbol]
            if (h != null) {
                val map = holdings + (order.symbol to h.copy(reservedQuantity = (h.reservedQuantity - (order.quantity - order.filledQuantity)).coerceAtLeast(0.0)))
                next = if (order.market.equals("crypto", true)) next.copy(cryptoHoldings = map) else next.copy(stockHoldings = map)
            }
        }
        return next
    }

    private fun executeOrder(state: LocalSimulationState, order: LocalMarketOrder, price: Double, now: Long): LocalGameResult {
        val feeRate = if (order.market.equals("crypto", true)) CRYPTO_FEE_RATE else STOCK_FEE_RATE
        val remaining = order.quantity - order.filledQuantity
        val fee = remaining * price * feeRate
        val value = remaining * price
        return if (order.side == LocalOrderSide.BUY) {
            val reserve = order.reservedCash
            val spend = value + fee
            if (spend > reserve + 1e-7) return reject(state, "Execution exceeds reserved cash")
            val old = if (order.market.equals("crypto", true)) state.cryptoHoldings[order.symbol] else state.stockHoldings[order.symbol]
            val prev = old ?: LocalHolding()
            val holding = prev.copy(quantity = prev.quantity + remaining, costBasis = prev.costBasis + spend)
            val map = (if (order.market.equals("crypto", true)) state.cryptoHoldings else state.stockHoldings) + (order.symbol to holding)
            val released = reserve - spend
            val updatedOrder = order.copy(filledQuantity = order.quantity, reservedCash = 0.0, averageFillPrice = price, feePaid = order.feePaid + fee, status = LocalOrderStatus.EXECUTED)
            var next = state.copy(cash = state.cash - spend, reservedCash = (state.reservedCash - reserve).coerceAtLeast(0.0),
                marketOrders = state.marketOrders.map { if (it.id == order.id) updatedOrder else it })
            next = if (order.market.equals("crypto", true)) next.copy(cryptoHoldings = map) else next.copy(stockHoldings = map)
            success(next, "Buy order executed")
        } else {
            val holdings = if (order.market.equals("crypto", true)) state.cryptoHoldings else state.stockHoldings
            val old = holdings[order.symbol] ?: return reject(state, "Holding disappeared before execution")
            if (old.quantity + 1e-9 < remaining || old.reservedQuantity + 1e-9 < remaining) return reject(state, "Reserved holding unavailable")
            val proceeds = value - fee
            val costRemoved = if (old.quantity <= 0) 0.0 else old.costBasis * (remaining / old.quantity)
            val updated = old.copy(quantity = old.quantity - remaining, reservedQuantity = (old.reservedQuantity - remaining).coerceAtLeast(0.0), costBasis = (old.costBasis - costRemoved).coerceAtLeast(0.0))
            val map = holdings + (order.symbol to updated)
            val updatedOrder = order.copy(filledQuantity = order.quantity, averageFillPrice = price, feePaid = order.feePaid + fee, status = LocalOrderStatus.EXECUTED)
            var next = state.copy(cash = state.cash + proceeds, marketOrders = state.marketOrders.map { if (it.id == order.id) updatedOrder else it })
            next = if (order.market.equals("crypto", true)) next.copy(cryptoHoldings = map) else next.copy(stockHoldings = map)
            success(ledger(next, now, "MARKET_SELL", "Sold $remaining ${order.symbol}", proceeds), "Sell order executed")
        }
    }

    fun maintainProperty(state: LocalSimulationState, propertyId: String, now: Long): LocalGameResult {
        val property = state.properties.firstOrNull { it.id == propertyId } ?: return reject(state, "Property not found")
        val missing = 100 - property.condition
        if (missing <= 0) return reject(state, "Property is already in perfect condition")
        val cost = property.purchasePrice * 0.0015 * missing
        if (state.cash - state.reservedCash < cost) return reject(state, "Insufficient available cash")
        val next = state.copy(cash = state.cash - cost, properties = state.properties.map { if (it.id == propertyId) it.copy(condition = 100, lastEvaluatedAt = now) else it })
        return success(ledger(next, now, "PROPERTY_MAINTENANCE", "Restored ${property.name} condition", -cost), "Property maintained")
    }

    fun startResearch(state: LocalSimulationState, project: LocalResearchProject, now: Long): LocalGameResult {
        if (project.id in state.completedResearch || state.researchProjects.any { it.id == project.id }) return reject(state, "Research already completed or active")
        if (state.cash - state.reservedCash < project.costCash) return reject(state, "Insufficient available cash")
        val next = state.copy(cash = state.cash - project.costCash, researchProjects = state.researchProjects + project.copy(startedAt = now, completesAt = now + project.durationMillis))
        return success(ledger(next, now, "RESEARCH_STARTED", "Started research: ${project.name}", -project.costCash), "Research started")
    }

    fun completeResearch(state: LocalSimulationState, now: Long): LocalSimulationState {
        val completed = state.researchProjects.filter { now >= it.completesAt }
        if (completed.isEmpty()) return state
        val remaining = state.researchProjects - completed.toSet()
        return state.copy(researchProjects = remaining, completedResearch = state.completedResearch + completed.map { it.id },
            progress = withLevel(state.progress.copy(researchPoints = state.progress.researchPoints + completed.sumOf { it.requiredPoints })))
    }

    fun contributeToCorporation(state: LocalSimulationState, playerId: String, amount: Double, now: Long): LocalGameResult {
        val corp = state.corporation ?: return reject(state, "You are not part of a corporation")
        if (amount <= 0 || state.cash - state.reservedCash < amount) return reject(state, "Invalid contribution or insufficient funds")
        val member = corp.members.firstOrNull { it.playerId == playerId } ?: return reject(state, "Corporation membership not found")
        val members = corp.members.map { if (it.playerId == playerId) it.copy(contributionToday = it.contributionToday + amount, lifetimeContribution = it.lifetimeContribution + amount) else it }
        val nextCorp = corp.copy(treasury = corp.treasury + amount, members = members,
            activityLog = (corp.activityLog + "[$now] ${member.displayName} contributed ${amount.money()}").takeLast(100))
        val next = state.copy(cash = state.cash - amount, corporation = nextCorp,
            progress = state.progress.copy(lifetimeExpenses = state.progress.lifetimeExpenses + amount))
        return success(ledger(next, now, "CORPORATION_CONTRIBUTION", "Contribution to ${corp.name}", -amount), "Contribution recorded")
    }

    private fun withLevel(p: LocalPlayerProgress): LocalPlayerProgress {
        var level = 1
        var remaining = p.xp
        while (remaining >= PlayerProgression.xpForLevel(level) && level < 10_000) {
            remaining -= PlayerProgression.xpForLevel(level).toDouble()
            level++
        }
        return p.copy(level = level)
    }

    private fun ledger(state: LocalSimulationState, now: Long, category: String, description: String, amount: Double): LocalSimulationState {
        val entry = LocalLedgerEntry("local-${state.nextSequence}", now, category, description, amount, state.cash)
        return state.copy(ledger = (state.ledger + entry).takeLast(5000), nextSequence = state.nextSequence + 1)
    }

    private fun success(state: LocalSimulationState, message: String) = LocalGameResult.Success(state, message)
    private fun reject(state: LocalSimulationState, reason: String) = LocalGameResult.Rejected(state, reason)
    private fun Double.money() = String.format(java.util.Locale.US, "%.2f", this)
}
