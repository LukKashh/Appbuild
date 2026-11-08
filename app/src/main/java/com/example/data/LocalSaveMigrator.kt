package com.example.data

import com.example.engine.LocalCompanyInstance
import com.example.engine.LocalEmployeeRole
import com.example.engine.LocalGameEngine
import com.example.engine.LocalHolding
import com.example.engine.LocalPlayerProgress
import com.example.engine.LocalPropertyInstance
import com.example.engine.LocalSimulationState
import com.example.engine.LocalCompanyUpgrade
import com.example.engine.LocalCasinoState
import com.example.engine.LocalCasinoStats
import com.example.model.GameState
import kotlin.math.max

/**
 * One-time migration from the original SharedPreferences save into the new Room save.
 * Legacy systems are copied as local assets rather than discarded; subsequent play is
 * owned by the new local simulation and the old save is left untouched as a rollback source.
 */
object LocalSaveMigrator {
    fun migrate(legacy: GameState, now: Long = System.currentTimeMillis()): LocalSimulationState {
        val companies = legacy.businesses.filter { it.isOwned }.map { business ->
            val branches = max(1, business.currentBranches)
            val staff = if (business.employeeCount > 0) listOf(
                LocalEmployeeRole(
                    id = "legacy-general",
                    title = "Existing workforce",
                    headcount = business.employeeCount,
                    salaryPerHour = business.salaryPerEmployee * 3_600.0,
                    productivity = 10.0
                )
            ) else emptyList()
            val upgrades = business.upgrades.map { upgrade ->
                LocalCompanyUpgrade(
                    id = upgrade.id,
                    name = upgrade.name,
                    cost = upgrade.cost,
                    revenueMultiplier = upgrade.multiplier,
                    level = if (upgrade.isPurchased) 1 else 0,
                    maxLevel = if (upgrade.isPurchased) 1 else 5
                )
            }
            LocalCompanyInstance(
                id = "legacy-${business.id}",
                definitionId = business.id,
                name = business.name,
                industry = business.industry.title,
                purchasePrice = business.baseCost,
                branches = branches,
                branchCapacity = max(branches, 1),
                staff = staff,
                upgrades = upgrades,
                hourlyBaseRevenue = business.baseRevenuePerSec * 3_600.0,
                hourlyFixedExpense = 0.0,
                uncollected = business.accumulatedWallet,
                totalRevenue = 0.0,
                totalExpenses = 0.0,
                lastOperatingAt = legacy.lastActiveTimestamp
            )
        }

        val properties = legacy.properties.filter { it.isOwned }.map { property ->
            LocalPropertyInstance(
                id = "legacy-${property.id}",
                definitionId = property.id,
                name = property.name,
                category = property.category.title,
                purchasePrice = property.purchasePrice,
                marketValue = property.currentValuation,
                baseRentPerHour = property.baseRentPerSec * 3_600.0,
                condition = property.conditionPercent,
                maintenancePerDay = 0.0,
                upgrades = property.upgrades.count { it.isPurchased },
                accumulatedRent = property.accumulatedRent,
                lastEvaluatedAt = legacy.lastActiveTimestamp
            )
        }

        val stocks = legacy.stocks.filter { it.sharesOwned > 0L }.associate { stock ->
            stock.symbol to LocalHolding(
                quantity = stock.sharesOwned.toDouble(),
                costBasis = stock.totalCostBasis
            )
        }
        val crypto = legacy.cryptos.filter { it.coinsOwned > 0.0 }.associate { token ->
            token.symbol to LocalHolding(
                quantity = token.coinsOwned,
                costBasis = token.totalCostBasis
            )
        }
        val collectibles = legacy.collectibles.filter { it.isOwned }.mapIndexed { index, item ->
            com.example.engine.LocalCollectibleHolding(
                instanceId = "legacy-collectible-$index",
                definitionId = item.id,
                acquisitionPrice = item.purchasePrice,
                acquiredAt = legacy.lastActiveTimestamp,
                sellableAt = legacy.lastActiveTimestamp,
                currentValue = item.currentValuation
            )
        }
        val completedResearch = legacy.research.filter { it.isUnlocked }.map { it.id }.toSet()
        val transactions = legacy.transactions.takeLast(5000).mapIndexed { index, entry ->
            com.example.engine.LocalLedgerEntry(
                id = "legacy-${entry.id.ifBlank { index.toString() }}",
                timestamp = entry.timestamp,
                category = entry.type.name.lowercase(),
                description = entry.description,
                amount = entry.amount,
                balanceAfter = legacy.cash
            )
        }
        val lastDividendDay = Math.floorDiv(now, 86_400_000L)
        return LocalSimulationState(
            cash = legacy.cash,
            playerName = "Player",
            progress = LocalPlayerProgress(
                level = 1,
                clickerLevel = legacy.clickLevel,
                lifetimeRevenue = legacy.transactions.filter { it.amount > 0.0 }.sumOf { it.amount },
                lifetimeExpenses = legacy.transactions.filter { it.amount < 0.0 }.sumOf { -it.amount }
            ),
            companies = companies,
            properties = properties,
            stockHoldings = stocks,
            cryptoHoldings = crypto,
            stockLastEvalTick = ReferenceMarketMigration.stockTick(now),
            cryptoLastEvalTick = ReferenceMarketMigration.cryptoTick(now),
            lastDividendDay = lastDividendDay,
            completedResearch = completedResearch,
            collectibles = collectibles,
            casino = LocalCasinoState(
                chips = legacy.casinoChips.toDouble(),
                stats = LocalCasinoStats(
                    won = legacy.casinoStats.totalWonChips.toDouble(),
                    rounds = legacy.casinoStats.gamesPlayed.toLong(),
                    biggestWin = legacy.casinoStats.biggestWinChips.toDouble()
                )
            ),
            ledger = transactions,
            lastSimulationAt = legacy.lastActiveTimestamp.takeIf { it > 0L } ?: now,
            nextSequence = transactions.size.toLong() + 1L
        )
    }

    private object ReferenceMarketMigration {
        fun stockTick(now: Long) = Math.floorDiv(now, 5L * 60_000L)
        fun cryptoTick(now: Long) = Math.floorDiv(now, 2L * 60_000L)
    }
}
