package com.example.data

import android.content.Context
import androidx.room.Room
import com.example.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Local persistence only. Room stores the full deterministic player snapshot on-device. */
class GameRepository(context: Context) {
    private val appContext = context.applicationContext
    private val legacyPrefs = appContext.getSharedPreferences("capital_fortune_save", Context.MODE_PRIVATE)
    private val database = Room.databaseBuilder(appContext, LocalGameDatabase::class.java, "capital_fortune_local.db").build()
    private val dao = database.snapshots()
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(GameState::class.java)

    suspend fun loadGameState(): GameState = withContext(Dispatchers.IO) {
        val saved = dao.read()
        if (saved != null) {
            val parsed = runCatching { adapter.fromJson(saved.payload) }.getOrNull()
            if (parsed != null) return@withContext expandState(parsed)
        }
        // One-time migration from the previous SharedPreferences save format.
        val legacy = legacyPrefs.getString(KEY_GAME_STATE, null)
        val migrated = legacy?.let { runCatching { adapter.fromJson(it) }.getOrNull() }
        if (migrated != null) {
            val expanded = expandState(migrated)
            dao.write(GameSnapshotEntity(payload = adapter.toJson(expanded)))
            legacyPrefs.edit().remove(KEY_GAME_STATE).apply()
            return@withContext expanded
        }
        val fresh = createFreshGameState()
        dao.write(GameSnapshotEntity(payload = adapter.toJson(fresh)))
        fresh
    }

    suspend fun saveGameState(state: GameState) = withContext(Dispatchers.IO) {
        dao.write(GameSnapshotEntity(payload = adapter.toJson(expandState(state))))
    }

    suspend fun resetGame(): GameState = withContext(Dispatchers.IO) {
        dao.clear()
        legacyPrefs.edit().clear().apply()
        val fresh = createFreshGameState()
        dao.write(GameSnapshotEntity(payload = adapter.toJson(fresh)))
        fresh
    }

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

    private fun createFreshGameState(): GameState {
        val now = System.currentTimeMillis()
        return GameState(
            cash = 50.0, casinoChips = 0L, clickLevel = 1, clickPower = 1.0, clickMultiplier = 1.0,
            clickUpgrades = DefaultData.initialClickUpgrades(),
            businesses = LocalCatalogue.businesses(DefaultData.initialBusinesses()),
            properties = LocalCatalogue.properties(DefaultData.initialProperties()),
            stocks = LocalCatalogue.stocks(DefaultData.initialStocks()),
            cryptos = LocalCatalogue.cryptos(DefaultData.initialCryptos()),
            collectibles = LocalCatalogue.collectibles(DefaultData.initialCollectibles(), now),
            research = LocalCatalogue.research(DefaultData.initialResearch()),
            casinoStats = CasinoStats(), achievements = DefaultData.initialAchievements(),
            transactions = listOf(Transaction("tx_start", now, TransactionType.CLICKER, "Initial seed capital granted", 50.0)),
            netWorthHistory = listOf(50.0), lastActiveTimestamp = now,
            profile = LocalPlayerProfile(username = "GameDev", createdAt = now),
            progression = LocalPlayerProgress(tokenUpdatedAt = now), lastFinancialSnapshotAt = now
        )
    }

    companion object { private const val KEY_GAME_STATE = "key_game_state_v1" }
}