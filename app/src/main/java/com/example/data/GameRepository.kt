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
                    return expandState(state)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return createFreshGameState()
    }


    /** Adds the reference game's full catalogue to existing local saves without resetting ownership. */
    private fun expandState(state: GameState): GameState = state.copy(
        businesses = LocalCatalogue.businesses(state.businesses),
        properties = LocalCatalogue.properties(state.properties),
        stocks = LocalCatalogue.stocks(state.stocks),
        cryptos = LocalCatalogue.cryptos(state.cryptos),
        collectibles = LocalCatalogue.collectibles(state.collectibles),
        research = LocalCatalogue.research(state.research),
        profile = if (state.profile.username.isBlank()) state.profile.copy(username = "GameDev") else state.profile,
        progression = state.progression.copy(level = state.progression.level.coerceAtLeast(1), clickTokens = state.progression.clickTokens.coerceIn(0.0, 150.0))
    )

    fun saveGameState(state: GameState) {
        try {
            val json = adapter.toJson(expandState(state))
            prefs.edit().putString(KEY_GAME_STATE, json).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun resetGame(): GameState {
        prefs.edit().clear().apply()
        return expandState(createFreshGameState())
    }

    private fun createFreshGameState(): GameState {
        return GameState(
            cash = 50.0,
            casinoChips = 0L,
            clickLevel = 1,
            clickPower = 1.0,
            clickMultiplier = 1.0,
            clickUpgrades = DefaultData.initialClickUpgrades(),
            businesses = LocalCatalogue.businesses(DefaultData.initialBusinesses()),
            properties = LocalCatalogue.properties(DefaultData.initialProperties()),
            stocks = LocalCatalogue.stocks(DefaultData.initialStocks()),
            cryptos = LocalCatalogue.cryptos(DefaultData.initialCryptos()),
            collectibles = LocalCatalogue.collectibles(DefaultData.initialCollectibles()),
            research = LocalCatalogue.research(DefaultData.initialResearch()),
            profile = LocalPlayerProfile(username = "GameDev"),
            progression = LocalPlayerProgress(),
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
