package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class GameRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("capital_fortune_save", Context.MODE_PRIVATE)

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(GameState::class.java)

    fun loadGameState(): GameState {
        val json = prefs.getString(KEY_GAME_STATE, null)
        if (!json.isNullOrEmpty()) {
            try {
                val state = adapter.fromJson(json)
                if (state != null) {
                    return state
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return createFreshGameState()
    }

    fun saveGameState(state: GameState) {
        try {
            val json = adapter.toJson(state)
            prefs.edit().putString(KEY_GAME_STATE, json).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun resetGame(): GameState {
        prefs.edit().clear().apply()
        return createFreshGameState()
    }

    private fun createFreshGameState(): GameState {
        return GameState(
            cash = 50.0,
            casinoChips = 0L,
            clickLevel = 1,
            clickPower = 1.0,
            clickMultiplier = 1.0,
            clickUpgrades = DefaultData.initialClickUpgrades(),
            businesses = DefaultData.initialBusinesses(),
            properties = DefaultData.initialProperties(),
            stocks = DefaultData.initialStocks(),
            cryptos = DefaultData.initialCryptos(),
            collectibles = DefaultData.initialCollectibles(),
            research = DefaultData.initialResearch(),
            casinoStats = CasinoStats(),
            achievements = DefaultData.initialAchievements(),
            transactions = listOf(
                Transaction(
                    id = "tx_start",
                    timestamp = System.currentTimeMillis(),
                    type = TransactionType.CLICKER,
                    description = "Initial seed capital granted",
                    amount = 50.0
                )
            ),
            netWorthHistory = listOf(50.0),
            lastActiveTimestamp = System.currentTimeMillis()
        )
    }

    companion object {
        private const val KEY_GAME_STATE = "key_game_state_v1"
    }
}
