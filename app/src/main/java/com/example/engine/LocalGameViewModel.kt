package com.example.engine

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.LocalSimulationRepository
import com.example.data.ReferenceCatalogue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Local-first game state coordinator for the expanded simulation.
 *
 * Legacy screens can migrate one feature at a time to this ViewModel. Its state is
 * loaded from Room, advanced locally, and persisted without Firebase or network calls.
 */
class LocalGameViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LocalSimulationRepository(application)
    private val commandMutex = Mutex()

    private val _state = MutableStateFlow(LocalSimulationState())
    val state: StateFlow<LocalSimulationState> = _state.asStateFlow()

    private val _offlineSummary = MutableStateFlow<LocalOfflineSummary?>(null)
    val offlineSummary: StateFlow<LocalOfflineSummary?> = _offlineSummary.asStateFlow()

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val _catalogueCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val catalogueCounts: StateFlow<Map<String, Int>> = _catalogueCounts.asStateFlow()

    private var ticksSinceSave = 0

    init {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val loaded = repository.load()
                val now = System.currentTimeMillis()
                val base = if (loaded.lastSimulationAt <= 0L) loaded.copy(lastSimulationAt = now) else loaded
                val (advanced, summary) = LocalGameEngine.tick(base, now, offline = true)
                repository.save(advanced)
                _state.value = advanced
                if (summary.elapsedMillis >= 120_000L) _offlineSummary.value = summary
                _catalogueCounts.value = runCatching {
                    mapOf(
                        "companies" to ReferenceCatalogue.companies(getApplication()).size,
                        "properties" to ReferenceCatalogue.properties(getApplication()).size,
                        "research" to ReferenceCatalogue.research(getApplication()).size,
                        "collectibles" to ReferenceCatalogue.collectibleDefinitions(getApplication()).size,
                        "collections" to ReferenceCatalogue.collections(getApplication()).size,
                        "achievements" to ReferenceCatalogue.achievementDefinitions(getApplication()).size,
                        "corpFacilities" to ReferenceCatalogue.corporationFacilities(getApplication()).size
                    )
                }.getOrDefault(emptyMap())
                _ready.value = true
            }
            startLocalClock()
        }
    }

    private fun startLocalClock() {
        viewModelScope.launch {
            while (true) {
                delay(1_000L)
                if (!_ready.value) continue
                commandMutex.withLock {
                    val now = System.currentTimeMillis()
                    val (advanced, _) = LocalGameEngine.tick(_state.value, now, offline = false)
                    _state.value = advanced
                    ticksSinceSave++
                    if (ticksSinceSave >= 5) {
                        withContext(Dispatchers.IO) { repository.save(advanced) }
                        ticksSinceSave = 0
                    }
                }
            }
        }
    }

    /**
     * Run a validated domain command. A successful command is committed to Room before
     * observers receive the new state, avoiding UI/state divergence after a failed save.
     */
    fun execute(command: (LocalSimulationState) -> LocalGameResult) {
        viewModelScope.launch {
            if (!_ready.value) {
                _lastError.value = "Game data is still loading"
                return@launch
            }
            commandMutex.withLock {
                when (val result = command(_state.value)) {
                    is LocalGameResult.Rejected -> _lastError.value = result.reason
                    is LocalGameResult.Success -> {
                        runCatching {
                            withContext(Dispatchers.IO) { repository.save(result.state) }
                        }.onSuccess {
                            _state.value = result.state
                            _lastError.value = null
                            ticksSinceSave = 0
                        }.onFailure {
                            _lastError.value = "Could not save this action locally: ${it.message ?: "storage error"}"
                        }
                    }
                }
            }
        }
    }

    fun collectCompany(companyId: String) =
        execute { LocalGameEngine.collectCompanyWallet(it, companyId, System.currentTimeMillis()) }

    fun contributeToCorporation(playerId: String, amount: Double) =
        execute { LocalGameEngine.contributeToCorporation(it, playerId, amount, System.currentTimeMillis()) }

    fun startResearch(project: LocalResearchProject) =
        execute { LocalGameEngine.startResearch(it, project, System.currentTimeMillis()) }

    fun maintainProperty(propertyId: String) =
        execute { LocalGameEngine.maintainProperty(it, propertyId, System.currentTimeMillis()) }

    fun dismissOfflineSummary() { _offlineSummary.value = null }
    fun dismissError() { _lastError.value = null }

    fun saveNow() {
        viewModelScope.launch {
            commandMutex.withLock {
                withContext(Dispatchers.IO) { repository.save(_state.value) }
                ticksSinceSave = 0
            }
        }
    }

    /** Read-only reference definitions for feature screens; all data is bundled locally. */
    fun companyDefinitions(): List<JSONObject> =
        runCatching { ReferenceCatalogue.companies(getApplication()) }.getOrDefault(emptyList())

    fun propertyDefinitions(): List<JSONObject> =
        runCatching { ReferenceCatalogue.properties(getApplication()) }.getOrDefault(emptyList())

    fun researchDefinitions(): List<JSONObject> =
        runCatching { ReferenceCatalogue.research(getApplication()) }.getOrDefault(emptyList())

    fun collectibleDefinitions(): List<JSONObject> =
        runCatching { ReferenceCatalogue.collectibleDefinitions(getApplication()) }.getOrDefault(emptyList())

    fun achievementDefinitions(): List<JSONObject> =
        runCatching { ReferenceCatalogue.achievementDefinitions(getApplication()) }.getOrDefault(emptyList())
}
