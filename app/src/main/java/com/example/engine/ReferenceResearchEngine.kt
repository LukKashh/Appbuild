package com.example.engine

import android.content.Context
import com.example.data.ReferenceCatalogue
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max

/** Time-gated R&D department translated from the webapp into fully local progression. */
object ReferenceResearchEngine {
    fun buildDepartment(state: LocalSimulationState, context: Context, now: Long): LocalGameResult {
        if (state.progress.level < 5) return reject(state, "Research Department unlocks at Player Level 5")
        if (state.researchBuilt) return reject(state, "Research Department already constructed")
        val config = ReferenceCatalogue.balance(context).optJSONObject("research") ?: JSONObject()
        val cost = config.optDouble("buildCost", 100_000.0)
        if (state.cash - state.reservedCash + 1e-9 < cost) return reject(state, "Insufficient available funds")
        val next = awardXp(
            state.copy(cash = state.cash - cost, researchBuilt = true),
            context,
            100.0
        )
        return success(record(next, now, "research_build", "Constructed Research Department", -cost), "Research Department constructed")
    }

    fun addResearchSlot(state: LocalSimulationState, context: Context, now: Long): LocalGameResult {
        if (!state.researchBuilt) return reject(state, "Construct the Research Department first")
        val config = ReferenceCatalogue.balance(context).optJSONObject("research") ?: JSONObject()
        val costs = config.optJSONArray("slotCosts") ?: JSONArray()
        val costIndex = state.researchSlots - 1
        if (costIndex !in 0 until costs.length()) return reject(state, "Maximum research slots reached")
        val cost = costs.optDouble(costIndex, Double.POSITIVE_INFINITY)
        if (state.cash - state.reservedCash + 1e-9 < cost) return reject(state, "Insufficient available funds")
        val next = state.copy(cash = state.cash - cost, researchSlots = state.researchSlots + 1)
        return success(record(next, now, "research_slot", "Expanded research capacity to ${next.researchSlots} slots", -cost), "Research slot purchased")
    }

    fun start(
        state: LocalSimulationState,
        context: Context,
        researchId: String,
        now: Long
    ): LocalGameResult {
        if (!state.researchBuilt) return reject(state, "Construct the Research Department first")
        val definition = ReferenceCatalogue.researchProject(context, researchId)
            ?: return reject(state, "Unknown research project")
        if (researchId in state.completedResearch || state.researchProjects.any { it.id == researchId }) {
            return reject(state, "Research already completed or currently active")
        }
        if (state.researchProjects.size >= state.researchSlots) return reject(state, "No free research slot")
        val prerequisites = definition.optJSONArray("prereq") ?: JSONArray()
        val missing = (0 until prerequisites.length()).map { prerequisites.optString(it) }
            .firstOrNull { it !in state.completedResearch }
        if (missing != null) {
            val prerequisiteName = ReferenceCatalogue.researchProject(context, missing)?.optString("name") ?: missing
            return reject(state, "Requires completed research: $prerequisiteName")
        }
        val requiredPoints = definition.optDouble("rp", 0.0)
        if (state.progress.researchPoints + 1e-9 < requiredPoints) return reject(state, "Insufficient Research Points")
        val cost = definition.optDouble("cost", 0.0)
        if (state.cash - state.reservedCash + 1e-9 < cost) return reject(state, "Insufficient available funds")
        val baseHours = definition.optDouble("hours", 0.0)
        val researchSpeed = ReferenceCompanyEngine.bonusAmount(state, context, "researchSpeed").coerceIn(0.0, 0.5)
        val duration = (baseHours * (1.0 - researchSpeed) * 3_600_000.0).toLong().coerceAtLeast(1_000L)
        val effect = definition.optJSONObject("effect") ?: JSONObject()
        val effects = linkedMapOf<String, Double>()
        val iterator = effect.keys()
        while (iterator.hasNext()) {
            val key = iterator.next()
            effects[key] = effect.optDouble(key, 0.0)
        }
        val project = LocalResearchProject(
            id = researchId,
            name = definition.optString("name", researchId),
            costCash = cost,
            requiredPoints = requiredPoints,
            durationMillis = duration,
            startedAt = now,
            completesAt = now + duration,
            effects = effects
        )
        val next = state.copy(
            cash = state.cash - cost,
            researchProjects = state.researchProjects + project,
            progress = state.progress.copy(researchPoints = state.progress.researchPoints - requiredPoints)
        )
        val recorded = record(next, now, "research_start", "Started research: ${project.name}", -cost)
        val withPointsLedger = recorded.copy(
            ledger = (recorded.ledger + LocalLedgerEntry(
                id = "local-${recorded.nextSequence}",
                timestamp = now,
                category = "research_points_spent",
                description = "Spent $requiredPoints Research Points on ${project.name}",
                amount = -requiredPoints,
                balanceAfter = recorded.cash
            )).takeLast(5000),
            nextSequence = recorded.nextSequence + 1
        )
        return success(withPointsLedger, "Research project started")
    }

    fun completeDue(state: LocalSimulationState, context: Context, now: Long): LocalSimulationState {
        val completed = state.researchProjects.filter { it.completesAt <= now }
        if (completed.isEmpty()) return state
        val completedIds = completed.map { it.id }.toSet()
        val nextProjects = state.researchProjects.filterNot { it.id in completedIds }
        val baseXp = completed.size * 100.0
        val award = PlayerProgression.award(
            state.progress,
            baseXp * ReferenceCompanyEngine.globalMultiplier(state, context, "xpPct")
        )
        var next = state.copy(
            researchProjects = nextProjects,
            completedResearch = state.completedResearch + completedIds,
            progress = award.progress.copy(researchDone = award.progress.researchDone + completed.size),
            cash = state.cash + award.cashReward
        )
        completed.forEach { project ->
            next = record(next, now, "research_complete", "Completed research: ${project.name}", 0.0)
        }
        return next
    }

    fun remainingMillis(project: LocalResearchProject, now: Long): Long =
        max(0L, project.completesAt - now)

    private fun awardXp(state: LocalSimulationState, context: Context, baseXp: Double): LocalSimulationState {
        val award = PlayerProgression.award(
            state.progress,
            baseXp * ReferenceCompanyEngine.globalMultiplier(state, context, "xpPct")
        )
        return state.copy(progress = award.progress, cash = state.cash + award.cashReward)
    }

    private fun record(state: LocalSimulationState, now: Long, category: String, description: String, amount: Double): LocalSimulationState {
        val entry = LocalLedgerEntry("local-${state.nextSequence}", now, category, description, amount, state.cash)
        return state.copy(ledger = (state.ledger + entry).takeLast(5000), nextSequence = state.nextSequence + 1)
    }

    private fun success(state: LocalSimulationState, message: String) = LocalGameResult.Success(state, message)
    private fun reject(state: LocalSimulationState, message: String) = LocalGameResult.Rejected(state, message)
}
