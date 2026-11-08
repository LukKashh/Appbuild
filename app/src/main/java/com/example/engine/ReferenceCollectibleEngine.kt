package com.example.engine

import android.content.Context
import com.example.data.ReferenceCatalogue
import org.json.JSONObject
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

data class ReferenceCollectibleSale(
    val itemId: String,
    val soldAt: Long,
    val acquisitionPrice: Double,
    val payout: Double
)

data class ReferenceCollectibleValuation(
    val currentValue: Double,
    val multiplier: Double,
    val demand: String
)

/** Port of webapp collectible inventory, rarity rules, timed resale and collections. */
object ReferenceCollectibleEngine {
    private const val HOUR_MS = 3_600_000L
    private const val DAY_MS = 86_400_000L
    private val rarities = listOf("Common", "Uncommon", "Rare", "Epic", "Legendary", "Mythical")
    private val resale = mapOf(
        "Common" to 0.70,
        "Uncommon" to 0.72,
        "Rare" to 0.75,
        "Epic" to 0.78,
        "Legendary" to 0.80,
        "Mythical" to 0.85
    )
    private val holdHours = mapOf(
        "Common" to 1L,
        "Uncommon" to 3L,
        "Rare" to 6L,
        "Epic" to 12L,
        "Legendary" to 24L,
        "Mythical" to 72L
    )

    fun valuation(context: Context, itemId: String, now: Long): ReferenceCollectibleValuation? {
        val item = item(context, itemId) ?: return null
        val category = category(context, item.optString("category")) ?: return null
        val updateHours = category.optDouble("updateHours", 24.0).coerceAtLeast(1.0)
        val tick = floor(now / (updateHours * HOUR_MS))
        val rarity = item.optString("rarity", "Common")
        val rarityIndex = rarities.indexOf(rarity).coerceAtLeast(0)
        val volatility = category.optDouble("volatility", 0.1)
        val itemWave = ReferenceCompanyEngine.deterministicWave(
            ReferenceCompanyEngine.hashText(itemId),
            tick / 3.0
        ) * volatility
        val categoryWave = ReferenceCompanyEngine.deterministicWave(
            ReferenceCompanyEngine.hashText(category.optString("id")),
            tick / 10.0
        ) * volatility * 0.5
        val limited = !item.isNull("limited") && item.optInt("limited", 0) > 0
        val multiplier = max(
            0.2,
            1.0 + itemWave + categoryWave + rarityIndex * 0.01 + if (limited) 0.05 else 0.0
        )
        val value = kotlin.math.round(item.optDouble("baseValue", item.optDouble("price", 0.0)) * multiplier)
        val demandWave = ReferenceCompanyEngine.deterministicWave(
            ReferenceCompanyEngine.hashText(itemId) xor 11,
            now / DAY_MS.toDouble()
        )
        val demand = when {
            demandWave > 0.3 -> "High"
            demandWave < -0.3 -> "Low"
            else -> "Moderate"
        }
        return ReferenceCollectibleValuation(value, multiplier, demand)
    }

    fun buy(state: LocalSimulationState, context: Context, itemId: String, now: Long): LocalGameResult {
        val definition = item(context, itemId) ?: return reject(state, "Unknown collectible")
        val releaseAt = if (definition.isNull("releaseAt")) 0L else definition.optLong("releaseAt", 0L)
        if (releaseAt > now) return reject(state, "This limited edition has not been released yet")
        val rarity = definition.optString("rarity", "Common")
        val limited = definition.optInt("limited", 0)
        val alreadyOwnedCopies = state.collectibles.count { it.definitionId == itemId }
        if (limited > 0 && alreadyOwnedCopies >= limited) return reject(state, "Local limited-edition allocation exhausted")
        val price = definition.optDouble("price", 0.0)
        if (price <= 0.0 || state.cash - state.reservedCash + 1e-9 < price) {
            return reject(state, "Insufficient available funds")
        }
        val instanceId = "collectible-${state.nextSequence}"
        val holding = LocalCollectibleHolding(
            instanceId = instanceId,
            definitionId = itemId,
            acquisitionPrice = price,
            acquiredAt = now,
            sellableAt = now + (holdHours[rarity] ?: 1L) * HOUR_MS,
            currentValue = definition.optDouble("baseValue", price)
        )
        val award = PlayerProgression.award(
            state.progress,
            10.0 * ReferenceCompanyEngine.globalMultiplier(state, context, "xpPct")
        )
        var next = state.copy(
            cash = state.cash - price + award.cashReward,
            collectibles = state.collectibles + holding,
            limitedCollectibleCopies = if (limited > 0)
                state.limitedCollectibleCopies + (itemId to ((state.limitedCollectibleCopies[itemId] ?: 0) + 1))
            else state.limitedCollectibleCopies,
            progress = award.progress.copy(collectiblesBought = award.progress.collectiblesBought + 1)
        )
        next = record(next, now, "collectible_purchase", "Purchased ${definition.optString("name")}", -price)
        next = checkCollections(next, context, now)
        return success(checkAchievements(next, context, now), "Collectible acquired")
    }

    fun sell(state: LocalSimulationState, context: Context, instanceId: String, now: Long): LocalGameResult {
        val holding = state.collectibles.firstOrNull { it.instanceId == instanceId }
            ?: return reject(state, "Collectible not found")
        val definition = item(context, holding.definitionId) ?: return reject(state, "Collectible definition unavailable")
        if (definition.optBoolean("restrictedResale", false)) {
            return reject(state, "Mythical editions are permanently bound and cannot be resold")
        }
        if (now < holding.sellableAt) {
            val minutes = ((holding.sellableAt - now) / 60_000L).coerceAtLeast(1L)
            return reject(state, "Holding period active: $minutes minutes remaining")
        }
        val value = valuation(context, holding.definitionId, now)?.currentValue ?: holding.currentValue
        val percentage = resale[definition.optString("rarity", "Common")] ?: 0.7
        val payout = value * percentage
        val sale = ReferenceCollectibleSale(holding.definitionId, now, holding.acquisitionPrice, payout)
        val remaining = state.collectibles.filterNot { it.instanceId == instanceId }
        val next = state.copy(
            cash = state.cash + payout,
            collectibles = remaining,
            collectibleSales = (state.collectibleSales + sale).takeLast(100)
        )
        val recorded = record(next, now, "collectible_sale", "Sold ${definition.optString("name")}", payout)
        return success(checkAchievements(checkCollections(recorded, context, now), context, now), "Collectible sold")
    }

    fun updateValues(state: LocalSimulationState, context: Context, now: Long): LocalSimulationState {
        if (state.collectibles.isEmpty()) return state
        val updated = state.collectibles.map { holding ->
            val current = valuation(context, holding.definitionId, now)?.currentValue ?: holding.currentValue
            holding.copy(currentValue = current)
        }
        return if (updated == state.collectibles) state else state.copy(collectibles = updated)
    }

    fun checkCollections(stateInput: LocalSimulationState, context: Context, now: Long): LocalSimulationState {
        var state = stateInput
        val owned = state.collectibles.map { it.definitionId }.toSet()
        for (collection in ReferenceCatalogue.collections(context)) {
            val id = collection.optString("id")
            if (id.isBlank() || id in state.collectionsRewarded) continue
            val items = collection.optJSONArray("items") ?: continue
            val complete = (0 until items.length()).all { owned.contains(items.optString(it)) }
            if (!complete) continue
            val reward = collection.optJSONObject("reward") ?: JSONObject()
            val cash = reward.optDouble("cash", 0.0)
            val xp = reward.optDouble("xp", 0.0) *
                ReferenceCompanyEngine.globalMultiplier(state, context, "xpPct")
            val rp = reward.optDouble("rp", 0.0)
            val award = PlayerProgression.award(state.progress, xp)
            state = state.copy(
                cash = state.cash + cash + award.cashReward,
                progress = award.progress.copy(
                    lifetimeCashEarned = award.progress.lifetimeCashEarned + cash + award.cashReward,
                    researchPoints = award.progress.researchPoints + rp,
                    collectionsDone = award.progress.collectionsDone + 1
                ),
                collectionsRewarded = state.collectionsRewarded + id
            )
            state = record(state, now, "collection_reward", "Completed collection: ${collection.optString("name")}", cash)
        }
        return state
    }

    fun checkAchievements(stateInput: LocalSimulationState, context: Context, now: Long): LocalSimulationState {
        var state = stateInput
        state = checkCollections(state, context, now)
        state = updateValues(state, context, now)
        val stats = statValues(state, context, now)
        repeat(3) {
            var changed = false
            for (achievement in ReferenceCatalogue.achievementDefinitions(context)) {
                val id = achievement.optString("id")
                if (id.isBlank()) continue
                val stat = achievement.optString("stat")
                val value = stats[stat] ?: 0.0
                val target = achievement.optDouble("target", Double.POSITIVE_INFINITY)
                val repeatable = achievement.optJSONObject("repeatable")
                if (repeatable != null) {
                    val cycle = state.achievementCycles[id] ?: 0
                    val growth = repeatable.optDouble("growth", 1.0)
                    val cycleTarget = ceil(target * growth.pow(cycle) * (cycle + 1))
                    if (value + 1e-9 < cycleTarget) continue
                    state = grantAchievement(state, achievement, context, now, cycle)
                    state = state.copy(achievementCycles = state.achievementCycles + (id to cycle + 1))
                    changed = true
                    continue
                }
                if (id in state.unlockedAchievements) continue
                val minLevel = achievement.optInt("minLevel", 0)
                if (minLevel > state.progress.level || value + 1e-9 < target) continue
                state = grantAchievement(state, achievement, context, now, null)
                state = state.copy(unlockedAchievements = state.unlockedAchievements + (id to now))
                changed = true
            }
            if (!changed) return state
        }
        return state
    }

    fun netWorth(state: LocalSimulationState, context: Context, now: Long): Double {
        val businessValue = state.referenceCompanies.sumOf { company ->
            company.price + company.upgradeValue + company.reserves + company.uncollected - company.liabilities
        }
        val propertyValue = state.referenceProperties.sumOf { it.price + it.upgradeValue + it.uncollected }
        val stockValue = state.stockHoldings.entries.sumOf { (symbol, holding) ->
            (ReferenceMarketEngine.getStockPrice(state, symbol, now) ?: 0.0) * holding.quantity
        }
        val cryptoValue = state.cryptoHoldings.entries.sumOf { (symbol, holding) ->
            (ReferenceMarketEngine.getCryptoPrice(state, symbol, now) ?: 0.0) * holding.quantity
        }
        val collectiblesValue = state.collectibles.sumOf {
            valuation(context, it.definitionId, now)?.currentValue ?: it.currentValue
        }
        val legacyBusinessValue = state.companies.sumOf {
            it.purchasePrice * it.branches + it.reserves + it.uncollected - it.liabilities
        }
        val legacyPropertyValue = state.properties.sumOf { it.marketValue + it.accumulatedRent }
        return state.cash + state.reservedCash + businessValue + legacyBusinessValue +
            propertyValue + legacyPropertyValue + stockValue + cryptoValue + collectiblesValue +
            (state.corporation?.treasury ?: 0.0) + state.casino.chips * ReferenceCasinoEngine.CHIP_VALUE
    }

    private fun statValues(state: LocalSimulationState, context: Context, now: Long): Map<String, Double> =
        mapOf(
            "clicks" to state.progress.lifetimeClicks.toDouble(),
            "lifetimeEarned" to state.progress.lifetimeCashEarned,
            "companiesBought" to state.progress.companiesBought.toDouble(),
            "companiesSold" to state.progress.companiesSold.toDouble(),
            "branches" to state.progress.branchesOpened.toDouble(),
            "upgrades" to state.progress.upgradesPurchased.toDouble(),
            "hires" to state.progress.hires.toDouble(),
            "stockTrades" to state.progress.stockTrades.toDouble(),
            "cryptoTrades" to state.progress.cryptoTrades.toDouble(),
            "dividends" to state.progress.dividends,
            "propertiesBought" to state.progress.propertiesBought.toDouble(),
            "rentEarned" to state.progress.rentEarned,
            "collectiblesBought" to state.progress.collectiblesBought.toDouble(),
            "collectionsDone" to state.progress.collectionsDone.toDouble(),
            "researchDone" to state.progress.researchDone.toDouble(),
            "level" to state.progress.level.toDouble(),
            "netWorth" to netWorth(state, context, now),
            "corpContributed" to state.progress.corpContributed,
            "maintenance" to state.progress.maintenance.toDouble(),
            "clickerLevel" to state.progress.clickerLevel.toDouble()
        )

    private fun grantAchievement(
        stateInput: LocalSimulationState,
        achievement: JSONObject,
        context: Context,
        now: Long,
        cycle: Int?
    ): LocalSimulationState {
        val reward = achievement.optJSONObject("reward") ?: JSONObject()
        val cash = reward.optDouble("cash", 0.0)
        val xp = reward.optDouble("xp", 0.0) *
            ReferenceCompanyEngine.globalMultiplier(stateInput, context, "xpPct")
        val rp = reward.optDouble("rp", 0.0)
        val award = PlayerProgression.award(stateInput.progress, xp)
        val label = achievement.optString("name", achievement.optString("id"))
        val suffix = cycle?.let { " (cycle ${it + 1})" } ?: ""
        val next = stateInput.copy(
            cash = stateInput.cash + cash + award.cashReward,
            progress = award.progress.copy(
                researchPoints = award.progress.researchPoints + rp,
                lifetimeCashEarned = award.progress.lifetimeCashEarned + cash + award.cashReward
            )
        )
        return record(next, now, "achievement_reward", "Achievement: $label$suffix", cash)
    }

    private fun item(context: Context, id: String): JSONObject? =
        ReferenceCatalogue.collectibleDefinitions(context).firstOrNull { it.optString("id") == id }

    private fun category(context: Context, id: String): JSONObject? =
        ReferenceCatalogue.collectibleCategories(context).firstOrNull { it.optString("id") == id }

    private fun record(
        state: LocalSimulationState,
        now: Long,
        category: String,
        description: String,
        amount: Double
    ): LocalSimulationState {
        val entry = LocalLedgerEntry(
            id = "local-${state.nextSequence}",
            timestamp = now,
            category = category,
            description = description,
            amount = amount,
            balanceAfter = state.cash
        )
        return state.copy(
            ledger = (state.ledger + entry).takeLast(5000),
            nextSequence = state.nextSequence + 1
        )
    }

    private fun success(state: LocalSimulationState, message: String) = LocalGameResult.Success(state, message)
    private fun reject(state: LocalSimulationState, message: String) = LocalGameResult.Rejected(state, message)
}
