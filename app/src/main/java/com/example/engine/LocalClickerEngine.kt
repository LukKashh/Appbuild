package com.example.engine

import android.content.Context
import com.example.data.ReferenceCatalogue
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Device-side click economy with regenerating burst tokens and player-level unlocks. */
object LocalClickerEngine {
    fun regenerate(state: LocalSimulationState, context: Context, now: Long): LocalSimulationState {
        val config = ReferenceCatalogue.balance(context).optJSONObject("clicker") ?: JSONObject()
        val capacity = config.optDouble("burstCapacity", 150.0)
        val regen = config.optDouble("burstRegenPerSec", 22.0)
        val elapsedSeconds = (now - state.clickTokenAt).coerceAtLeast(0L) / 1000.0
        val tokens = (state.clickTokens + elapsedSeconds * regen).coerceIn(0.0, capacity)
        return if (tokens == state.clickTokens && state.clickTokenAt == now) state
        else state.copy(clickTokens = tokens, clickTokenAt = now)
    }

    fun tap(
        stateInput: LocalSimulationState,
        context: Context,
        requestedCount: Int = 1,
        now: Long
    ): LocalGameResult {
        val config = ReferenceCatalogue.balance(context).optJSONObject("clicker") ?: JSONObject()
        var state = regenerate(stateInput, context, now)
        val burstCapacity = config.optInt("burstCapacity", 150)
        val available = min(state.clickTokens.toInt(), requestedCount.coerceAtLeast(0))
            .coerceAtMost(burstCapacity)
        if (available <= 0) return reject(state, "No click tokens available")
        val hardCap = config.optInt("hardCap", 50)
        val initialCap = config.optInt("initialCap", 10)
        val totalCapBonus = state.progress.clickCapBonus +
            ReferenceCompanyEngine.bonusAmount(state, context, "clickCap").roundToInt()
        val maxLevel = min(
            hardCap,
            initialCap + totalCapBonus.coerceAtLeast(0)
        )
        val baseValue = config.optDouble("baseValue", 0.5)
        val maxValue = config.optDouble("maxValue", 1000.0)
        val curve = config.optDouble("valueCurve", 6.0)
        val reqBase = config.optInt("reqBase", 5000)
        val reqStep = config.optInt("reqStep", 2500)
        val early = config.optJSONArray("earlyReqs") ?: JSONArray()
        var clickerLevel = state.progress.clickerLevel.coerceIn(1, hardCap)
        var clickerCount = state.clickerCount
        var earned = 0.0
        var xp = 0.0
        val clickMultiplier = ReferenceCompanyEngine.globalMultiplier(state, context, "clickPct")
        val playerXpMultiplier = ReferenceCompanyEngine.globalMultiplier(state, context, "xpPct")
        val sequenceSeed = ReferenceCompanyEngine.hashText(state.playerId) xor state.progress.lifetimeClicks.toInt()
        repeat(available) { index ->
            val level = clickerLevel.coerceIn(1, hardCap)
            val t = (level - 1).toDouble() / (hardCap - 1).coerceAtLeast(1)
            val value = baseValue + ((maxValue - baseValue) * (exp(curve * t) - 1.0)) /
                (exp(curve) - 1.0)
            earned += min(maxValue, value) * clickMultiplier
            if (level < hardCap) {
                clickerCount += 1
                val requirement = if (level >= hardCap) Int.MAX_VALUE else if (level <= early.length()) {
                    early.optInt(level - 1, 0)
                } else {
                    reqBase + reqStep * (level - 8)
                }
                if (clickerCount >= requirement) {
                    if (clickerLevel < maxLevel) {
                        clickerLevel += 1
                        clickerCount = 0
                    } else {
                        clickerCount = requirement
                    }
                }
            } else {
                clickerCount = 0
            }
            // The web version rolls 0.25–1.00 XP per click. A deterministic local sequence
            // preserves variance without depending on a server or non-replayable state.
            val roll = ReferenceCompanyEngine.deterministicWave(sequenceSeed xor (index * 31), now / 1000.0)
            xp += 0.25 + ((roll + 1.0) / 2.0) * 0.75
        }
        val award = PlayerProgression.award(state.progress, xp * playerXpMultiplier)
        state = state.copy(
            cash = state.cash + earned + award.cashReward,
            clickerCount = clickerCount,
            clickTokens = max(0.0, state.clickTokens - available),
            clickTokenAt = now,
            progress = award.progress.copy(
                clickerLevel = clickerLevel,
                lifetimeClicks = award.progress.lifetimeClicks + available,
                lifetimeRevenue = award.progress.lifetimeRevenue + earned,
                lifetimeCashEarned = award.progress.lifetimeCashEarned + earned
            )
        )
        return success(state, "Earned ${earned.toLong()} from $available taps")
    }

    fun tokenCapacity(context: Context): Double =
        ReferenceCatalogue.balance(context).optJSONObject("clicker")?.optDouble("burstCapacity", 150.0) ?: 150.0

    private fun success(state: LocalSimulationState, message: String) = LocalGameResult.Success(state, message)
    private fun reject(state: LocalSimulationState, message: String) = LocalGameResult.Rejected(state, message)
}
