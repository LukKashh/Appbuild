package com.example.data

import android.content.Context
import com.example.engine.LocalSimulationState
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/**
 * Durable, device-only storage for the expanded local simulation.
 * Uses a dedicated save key so the existing legacy GameState can be migrated safely.
 */
class LocalSimulationRepository(context: Context) {
    private val preferences = context.getSharedPreferences("capital_fortune_local_simulation", Context.MODE_PRIVATE)
    private val adapter = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
        .adapter(LocalSimulationState::class.java)

    @Synchronized
    fun load(): LocalSimulationState {
        val json = preferences.getString(KEY_STATE, null) ?: return LocalSimulationState()
        return runCatching { adapter.fromJson(json) }.getOrNull() ?: LocalSimulationState()
    }

    @Synchronized
    fun save(state: LocalSimulationState): Boolean {
        val json = runCatching { adapter.toJson(state) }.getOrNull() ?: return false
        // commit() is intentional: a successful game action is not acknowledged until
        // the local save has reached persistent storage.
        return preferences.edit().putString(KEY_STATE, json).commit()
    }

    @Synchronized
    fun clear() {
        preferences.edit().remove(KEY_STATE).commit()
    }

    private companion object {
        const val KEY_STATE = "simulation_state_v1"
    }
}
