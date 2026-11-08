package com.example.engine

import android.content.Context
import com.example.data.ReferenceCatalogue
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class ReferenceOwnedProperty(
    val instanceId: String,
    val definitionId: String,
    val boughtAt: Long,
    val price: Double,
    val condition: Double = 100.0,
    val upgrades: Map<String, Int> = emptyMap(),
    val upgradeValue: Double = 0.0,
    val rentTotal: Double = 0.0,
    val uncollected: Double = 0.0
)

data class ReferenceSoldProperty(
    val definitionId: String,
    val name: String,
    val soldAt: Long,
    val payout: Double
)

data class ReferencePropertyEconomy(
    val occupancy: Double,
    val demand: Double,
    val gross: Double,
    val expenses: Double,
    val net: Double,
    val conditionFactor: Double
)

data class ReferencePropertySaleQuote(
    val valuation: Double,
    val payout: Double,
    val rentWallet: Double
)

/** Local port of property mechanics in the webapp engine.ts. */
object ReferencePropertyEngine {
    private const val HOUR_MS = 3_600_000.0
    private const val PROPERTY_RESALE_PCT = 0.75
    private const val PROPERTY_DECAY_PER_DAY = 6.0
    private const val RENT_OFFLINE_CAP_HOURS = 24.0

    fun buy(state: LocalSimulationState, context: Context, definitionId: String, now: Long): LocalGameResult {
        if (state.progress.level < 3) return reject(state, "Real estate unlocks at Player Level 3")
        val definition = ReferenceCatalogue.property(context, definitionId)
            ?: return reject(state, "Unknown property")
        if (state.referenceProperties.any { it.definitionId == definitionId }) {
            return reject(state, "You already own this property")
        }
        val price = definition.optDouble("price", 0.0)
        if (price <= 0.0 || state.cash - state.reservedCash + 1e-9 < price) {
            return reject(state, "Insufficient available funds")
        }
        val instance = ReferenceOwnedProperty(
            instanceId = "local-property-${state.nextSequence}",
            definitionId = definitionId,
            boughtAt = now,
            price = price
        )
        var next = state.copy(
            cash = state.cash - price,
            referenceProperties = state.referenceProperties + instance,
            progress = state.progress.copy(propertiesBought = state.progress.propertiesBought + 1)
        )
        next = awardXp(next, 30.0)
        return success(record(next, now, "property_purchase", "Purchased ${definition.optString("name")}", -price), "Property acquired")
    }

    fun occupancy(
        state: LocalSimulationState,
        context: Context,
        property: ReferenceOwnedProperty,
        now: Long
    ): Pair<Double, Double>? {
        val definition = ReferenceCatalogue.property(context, property.definitionId) ?: return null
        val demandWave = ReferenceCompanyEngine.deterministicWave(
            ReferenceCompanyEngine.hashText(definition.optString("category") + definition.optString("location")),
            now / HOUR_MS / 6.0
        )
        val occupancyWave = ReferenceCompanyEngine.deterministicWave(
            ReferenceCompanyEngine.hashText(property.instanceId),
            now / HOUR_MS / 4.0
        )
        val demand = definition.optDouble("demand", 1.0) * (1.0 + demandWave * 0.12)
        var occupancy = 0.72 + occupancyWave * 0.1 + (property.condition - 60.0) / 400.0
        for (upgrade in ReferenceCatalogue.propertyUpgrades(context)) {
            occupancy += (property.upgrades[upgrade.optString("id")] ?: 0) *
                upgrade.optDouble("occupancy", 0.0)
        }
        return occupancy.coerceIn(0.05, 1.0) to demand
    }

    fun economy(
        state: LocalSimulationState,
        context: Context,
        instanceId: String,
        now: Long
    ): ReferencePropertyEconomy? {
        val property = state.referenceProperties.firstOrNull { it.instanceId == instanceId } ?: return null
        val definition = ReferenceCatalogue.property(context, property.definitionId) ?: return null
        val (occupancy, demand) = occupancy(state, context, property, now) ?: return null
        var rentPercent = 0.0
        var expensePercent = 0.0
        for (upgrade in ReferenceCatalogue.propertyUpgrades(context)) {
            val count = property.upgrades[upgrade.optString("id")] ?: 0
            rentPercent += count * upgrade.optDouble("rentPct", 0.0)
            expensePercent += count * upgrade.optDouble("expensePct", 0.0)
        }
        val conditionFactor = when {
            property.condition < 20.0 -> 0.6
            property.condition < 40.0 -> 0.85
            else -> 1.0
        }
        val globalRentMultiplier = ReferenceCompanyEngine.globalMultiplier(state, context, "rentPct")
        val gross = definition.optDouble("rent", 0.0) * occupancy * demand *
            (1.0 + rentPercent) * conditionFactor * globalRentMultiplier
        val expenses = definition.optDouble("expense", 0.0) * (1.0 - expensePercent.coerceIn(0.0, 0.6))
        return ReferencePropertyEconomy(occupancy, demand, gross, expenses, gross - expenses, conditionFactor)
    }

    fun maintain(state: LocalSimulationState, instanceId: String, now: Long): LocalGameResult {
        val property = state.referenceProperties.firstOrNull { it.instanceId == instanceId }
            ?: return reject(state, "Property not found")
        if (property.condition >= 99.0) return reject(state, "Already in excellent condition")
        val cost = (property.price * 0.02 * ((100.0 - property.condition) / 100.0) + 50.0).roundToInt().toDouble()
        if (state.cash - state.reservedCash + 1e-9 < cost) return reject(state, "Insufficient available funds")
        val updated = property.copy(condition = 100.0)
        val next = state.copy(
            cash = state.cash - cost,
            referenceProperties = state.referenceProperties.replace(updated),
            progress = state.progress.copy(maintenance = state.progress.maintenance + 1)
        )
        return success(record(next, now, "maintenance", "Restored property condition", -cost), "Property maintained")
    }

    fun upgrade(
        state: LocalSimulationState,
        context: Context,
        instanceId: String,
        upgradeId: String,
        now: Long
    ): LocalGameResult {
        val property = state.referenceProperties.firstOrNull { it.instanceId == instanceId }
            ?: return reject(state, "Property not found")
        val definition = ReferenceCatalogue.property(context, property.definitionId)
            ?: return reject(state, "Property definition unavailable")
        val upgrade = ReferenceCatalogue.propertyUpgrades(context).firstOrNull { it.optString("id") == upgradeId }
            ?: return reject(state, "Unknown property upgrade")
        val categories = upgrade.optJSONArray("categories") ?: JSONArray()
        val available = (0 until categories.length()).any { categories.optString(it) == definition.optString("category") }
        if (!available) return reject(state, "Upgrade not available for this property")
        val level = property.upgrades[upgradeId] ?: 0
        val maxLevel = upgrade.optInt("max", 0)
        if (level >= maxLevel) return reject(state, "Upgrade already at maximum level")
        val requiredResearch = upgrade.optString("research").takeIf { it.isNotBlank() && it != "null" }
        if (requiredResearch != null && requiredResearch !in state.completedResearch) {
            val requiredName = ReferenceCatalogue.researchProject(context, requiredResearch)?.optString("name")
                ?: requiredResearch
            return reject(state, "Requires research: $requiredName")
        }
        val cost = (definition.optDouble("price", property.price) *
            upgrade.optDouble("costPct", 0.0) * 1.5.pow(level)).roundToInt().toDouble()
        if (state.cash - state.reservedCash + 1e-9 < cost) return reject(state, "Insufficient available funds")
        val levels = property.upgrades.toMutableMap().apply { this[upgradeId] = level + 1 }
        val updated = property.copy(upgrades = levels, upgradeValue = property.upgradeValue + cost)
        val next = state.copy(cash = state.cash - cost, referenceProperties = state.referenceProperties.replace(updated))
        return success(record(next, now, "property_upgrade", "Purchased ${upgrade.optString("name")}", -cost), "Property upgrade purchased")
    }

    fun collect(state: LocalSimulationState, instanceId: String? = null, now: Long): LocalGameResult {
        val selected = state.referenceProperties.filter { instanceId == null || it.instanceId == instanceId }
        val amount = selected.sumOf { it.uncollected }
        if (amount <= 0.0) return reject(state, "No rent available to collect")
        val updated = state.referenceProperties.map { property ->
            if (instanceId == null || property.instanceId == instanceId) property.copy(uncollected = 0.0) else property
        }
        val next = state.copy(
            cash = state.cash + amount,
            referenceProperties = updated,
            progress = state.progress.copy(
                rentEarned = state.progress.rentEarned + amount,
                lifetimeRevenue = state.progress.lifetimeRevenue + amount,
                lifetimeCashEarned = state.progress.lifetimeCashEarned + amount
            )
        )
        return success(record(next, now, "rent_collect", "Collected property rental income", amount), "Rent collected")
    }

    fun saleQuote(state: LocalSimulationState, instanceId: String): ReferencePropertySaleQuote? {
        val property = state.referenceProperties.firstOrNull { it.instanceId == instanceId } ?: return null
        val valuation = property.price + property.upgradeValue
        return ReferencePropertySaleQuote(valuation, valuation * PROPERTY_RESALE_PCT, property.uncollected)
    }

    fun sell(state: LocalSimulationState, context: Context, instanceId: String, now: Long): LocalGameResult {
        val property = state.referenceProperties.firstOrNull { it.instanceId == instanceId }
            ?: return reject(state, "Property not found")
        val definition = ReferenceCatalogue.property(context, property.definitionId)
            ?: return reject(state, "Property definition unavailable")
        val quote = saleQuote(state, instanceId) ?: return reject(state, "Unable to value property")
        val cashPayout = quote.payout + quote.rentWallet
        val next = state.copy(
            cash = state.cash + cashPayout,
            referenceProperties = state.referenceProperties.filterNot { it.instanceId == instanceId },
            soldProperties = (state.soldProperties + ReferenceSoldProperty(
                definitionId = property.definitionId,
                name = definition.optString("name"),
                soldAt = now,
                payout = quote.payout
            )).take(100),
            progress = state.progress.copy(
                propertiesSold = state.progress.propertiesSold + 1,
                rentEarned = state.progress.rentEarned + quote.rentWallet,
                lifetimeRevenue = state.progress.lifetimeRevenue + cashPayout
            )
        )
        val recorded = record(next, now, "property_sale", "Sold ${definition.optString("name")}", quote.payout)
        return success(recorded, "Property sold")
    }

    /** Rent is capped to 24h while away; condition decay uses the full elapsed duration. */
    fun tick(
        state: LocalSimulationState,
        context: Context,
        now: Long,
        offline: Boolean
    ): Pair<LocalSimulationState, LocalOfflineSummary> {
        val elapsed = (now - state.lastSimulationAt).coerceAtLeast(0L)
        if (elapsed <= 0L || state.referenceProperties.isEmpty()) {
            return state to LocalOfflineSummary(elapsed, emptyList(), 0.0, 0L)
        }
        val requestedHours = elapsed / HOUR_MS
        val rentHours = if (offline) min(requestedHours, RENT_OFFLINE_CAP_HOURS) else requestedHours
        val decayPerDay = ReferenceCatalogue.balance(context).optDouble(
            "propertyDecayPerDay",
            PROPERTY_DECAY_PER_DAY
        )
        var cash = state.cash
        var totalNet = 0.0
        val lines = mutableListOf<LocalOfflineLine>()
        val updated = state.referenceProperties.map { property ->
            val econ = economy(state, context, property.instanceId, now) ?: return@map property
            val gross = econ.gross * rentHours
            val expenses = econ.expenses * rentHours
            val net = econ.net * rentHours
            totalNet += net
            var wallet = property.uncollected + net
            if (wallet < 0.0) {
                cash = max(0.0, cash + wallet)
                wallet = 0.0
            }
            val name = ReferenceCatalogue.property(context, property.definitionId)?.optString("name")
                ?: property.definitionId
            lines += LocalOfflineLine(
                label = name,
                gross = gross,
                expenses = expenses,
                net = net,
                eligible = true,
                note = "Occupancy ${(econ.occupancy * 100.0).roundToInt()}% · condition ${property.condition.roundToInt()}%"
            )
            property.copy(
                rentTotal = property.rentTotal + net,
                uncollected = wallet,
                condition = max(0.0, property.condition - decayPerDay * requestedHours / 24.0)
            )
        }
        val progress = state.progress.copy(
            lifetimeRevenue = state.progress.lifetimeRevenue + lines.sumOf { it.gross },
            lifetimeExpenses = state.progress.lifetimeExpenses + lines.sumOf { it.expenses }
        )
        val next = state.copy(
            cash = cash,
            referenceProperties = updated,
            progress = progress
        )
        val summary = LocalOfflineSummary(
            elapsedMillis = elapsed,
            lines = lines,
            totalNet = totalNet,
            cappedMillis = if (offline) max(0.0, requestedHours - rentHours).times(HOUR_MS).toLong() else 0L
        )
        return next to summary
    }

    private fun awardXp(state: LocalSimulationState, amount: Double): LocalSimulationState {
        val award = PlayerProgression.award(state.progress, amount)
        return state.copy(progress = award.progress, cash = state.cash + award.cashReward)
    }

    private fun List<ReferenceOwnedProperty>.replace(updated: ReferenceOwnedProperty) =
        map { if (it.instanceId == updated.instanceId) updated else it }

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
