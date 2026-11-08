package com.example.engine

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GameRepository
import com.example.data.LocalSaveMigrator
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
                val initialState = if (loaded.lastSimulationAt <= 0L) {
                    LocalSaveMigrator.migrate(GameRepository(getApplication()).loadGameState(), now)
                } else loaded
                val initialized = if (initialState.lastSimulationAt <= 0L) initialState.copy(lastSimulationAt = now) else initialState
                val base = LocalClickerEngine.regenerate(initialized, getApplication(), now)
                val (businessAdvanced, businessSummary) =
                    ReferenceCompanyEngine.tick(base, getApplication(), now, offline = true)
                val (propertyAdvanced, propertySummary) =
                    ReferencePropertyEngine.tick(businessAdvanced, getApplication(), now, offline = true)
                val corporationAdvanced = ReferenceCorporationEngine.tick(propertyAdvanced, getApplication(), now)
                val marketAdvanced = ReferenceMarketEngine.tick(corporationAdvanced, getApplication(), now)
                val researchAdvanced = ReferenceResearchEngine.completeDue(marketAdvanced, getApplication(), now)
                val collectibleAdvanced = ReferenceCollectibleEngine.checkAchievements(researchAdvanced, getApplication(), now)
                val (advanced, generalSummary) =
                    LocalGameEngine.tick(collectibleAdvanced, now, offline = true)
                val summary = LocalOfflineSummary(
                    elapsedMillis = maxOf(businessSummary.elapsedMillis, propertySummary.elapsedMillis, generalSummary.elapsedMillis),
                    lines = businessSummary.lines + propertySummary.lines + generalSummary.lines,
                    totalNet = businessSummary.totalNet + propertySummary.totalNet + generalSummary.totalNet,
                    cappedMillis = maxOf(businessSummary.cappedMillis, propertySummary.cappedMillis, generalSummary.cappedMillis),
                    researchPoints = businessSummary.researchPoints + propertySummary.researchPoints + generalSummary.researchPoints
                )
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
                    val shouldSave = ticksSinceSave + 1 >= 5
                    val advanced = withContext(Dispatchers.Default) {
                        val clickerAdvanced = LocalClickerEngine.regenerate(_state.value, getApplication(), now)
                        val (businessAdvanced, _) =
                            ReferenceCompanyEngine.tick(clickerAdvanced, getApplication(), now, offline = false)
                        val (propertyAdvanced, _) =
                            ReferencePropertyEngine.tick(businessAdvanced, getApplication(), now, offline = false)
                        val corporationAdvanced = ReferenceCorporationEngine.tick(propertyAdvanced, getApplication(), now)
                        val marketAdvanced = ReferenceMarketEngine.tick(corporationAdvanced, getApplication(), now)
                        val researchAdvanced = ReferenceResearchEngine.completeDue(marketAdvanced, getApplication(), now)
                        val checked = if (shouldSave) {
                            ReferenceCollectibleEngine.checkAchievements(researchAdvanced, getApplication(), now)
                        } else researchAdvanced
                        LocalGameEngine.tick(checked, now, offline = false).first
                    }
                    _state.value = advanced
                    ticksSinceSave++
                    if (shouldSave) {
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
                val result = withContext(Dispatchers.Default) { command(_state.value) }
                when (result) {
                    is LocalGameResult.Rejected -> _lastError.value = result.reason
                    is LocalGameResult.Success -> {
                        val checkedState = withContext(Dispatchers.Default) {
                            ReferenceCollectibleEngine.checkAchievements(
                                result.state, getApplication(), System.currentTimeMillis()
                            )
                        }
                        runCatching {
                            withContext(Dispatchers.IO) { repository.save(checkedState) }
                        }.onSuccess {
                            _state.value = checkedState
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

    fun tapLocalClicker(count: Int = 1) =
        execute { LocalClickerEngine.tap(it, getApplication(), count, System.currentTimeMillis()) }

    fun buyReferenceCompany(definitionId: String) =
        execute { ReferenceCompanyEngine.buyCompany(it, getApplication(), definitionId, System.currentTimeMillis()) }

    fun upgradeBusinessCapacity() =
        execute { LocalGameEngine.upgradeBusinessCapacity(it, System.currentTimeMillis()) }

    fun hireReferenceStaff(instanceId: String, role: String, count: Int, packageIndex: Int? = null) =
        execute { ReferenceCompanyEngine.hire(it, getApplication(), instanceId, role, count, packageIndex, System.currentTimeMillis()) }

    fun fireReferenceStaff(instanceId: String, role: String, count: Int) =
        execute { ReferenceCompanyEngine.fire(it, getApplication(), instanceId, role, count, System.currentTimeMillis()) }

    fun upgradeReferenceCompany(instanceId: String, upgradeId: String) =
        execute { ReferenceCompanyEngine.upgrade(it, getApplication(), instanceId, upgradeId, System.currentTimeMillis()) }

    fun upgradeReferenceStaffCapacity(instanceId: String) =
        execute { ReferenceCompanyEngine.upgradeStaffCapacity(it, getApplication(), instanceId, System.currentTimeMillis()) }

    fun openReferenceBranch(instanceId: String) =
        execute { ReferenceCompanyEngine.openBranch(it, getApplication(), instanceId, System.currentTimeMillis()) }

    fun injectReferenceCapital(instanceId: String, amount: Double) =
        execute { ReferenceCompanyEngine.injectCapital(it, getApplication(), instanceId, amount, System.currentTimeMillis()) }

    fun setReferenceCompanySuspended(instanceId: String, suspended: Boolean) =
        execute { ReferenceCompanyEngine.setSuspended(it, instanceId, suspended, System.currentTimeMillis()) }

    fun collectReferenceCompanyIncome(instanceId: String? = null) =
        execute { ReferenceCompanyEngine.collect(it, instanceId, System.currentTimeMillis()) }

    fun sellReferenceCompany(instanceId: String, confirmed: Boolean) =
        execute { ReferenceCompanyEngine.sell(it, getApplication(), instanceId, confirmed, System.currentTimeMillis()) }

    fun placeReferenceMarketOrder(
        market: String,
        symbol: String,
        side: LocalOrderSide,
        kind: LocalOrderKind,
        quantity: Double,
        limitPrice: Double? = null,
        triggerPrice: Double? = null
    ) = execute {
        ReferenceMarketEngine.placeOrder(
            it, getApplication(), market, symbol, side, kind, quantity,
            limitPrice, triggerPrice, System.currentTimeMillis()
        )
    }

    fun cancelReferenceMarketOrder(orderId: String) =
        execute { ReferenceMarketEngine.cancelOrder(it, orderId, System.currentTimeMillis()) }

    fun buyCasinoChips(chips: Long) =
        execute { ReferenceCasinoEngine.buyChips(it, chips, System.currentTimeMillis()) }

    fun sellCasinoChips(chips: Long) =
        execute { ReferenceCasinoEngine.sellChips(it, chips, System.currentTimeMillis()) }

    fun playRoulette(bets: List<LocalRouletteBet>) =
        execute { ReferenceCasinoEngine.roulette(it, bets, System.currentTimeMillis()) }

    fun playKeno(bet: Double, picks: List<Int>) =
        execute { ReferenceCasinoEngine.keno(it, bet, picks, System.currentTimeMillis()) }

    fun startBlackjack(bet: Double) =
        execute { ReferenceCasinoEngine.blackjackStart(it, bet, System.currentTimeMillis()) }

    fun moveBlackjack(move: String) =
        execute { ReferenceCasinoEngine.blackjackMove(it, move, System.currentTimeMillis()) }

    fun startMines(bet: Double, mines: Int) =
        execute { ReferenceCasinoEngine.minesStart(it, bet, mines, System.currentTimeMillis()) }

    fun revealMine(tile: Int) =
        execute { ReferenceCasinoEngine.minesReveal(it, tile, System.currentTimeMillis()) }

    fun cashoutMines() =
        execute { ReferenceCasinoEngine.minesCashout(it, System.currentTimeMillis()) }

    fun startHiLo(bet: Double) =
        execute { ReferenceCasinoEngine.hiloStart(it, bet, System.currentTimeMillis()) }

    fun guessHiLo(higher: Boolean) =
        execute { ReferenceCasinoEngine.hiloGuess(it, higher, System.currentTimeMillis()) }

    fun cashoutHiLo() =
        execute { ReferenceCasinoEngine.hiloCashout(it, System.currentTimeMillis()) }

    fun buyReferenceCollectible(itemId: String) =
        execute { ReferenceCollectibleEngine.buy(it, getApplication(), itemId, System.currentTimeMillis()) }

    fun sellReferenceCollectible(instanceId: String) =
        execute { ReferenceCollectibleEngine.sell(it, getApplication(), instanceId, System.currentTimeMillis()) }

    fun refreshReferenceEconomy() {
        execute { gameState ->
            LocalGameResult.Success(
                ReferenceCollectibleEngine.checkAchievements(gameState, getApplication(), System.currentTimeMillis()),
                "Reference economy updated"
            )
        }
    }

    fun contributeToCorporation(playerId: String, amount: Double) =
        execute { LocalGameEngine.contributeToCorporation(it, playerId, amount, System.currentTimeMillis()) }

    fun startResearch(project: LocalResearchProject) =
        execute { LocalGameEngine.startResearch(it, project, System.currentTimeMillis()) }

    fun buildResearchDepartment() =
        execute { ReferenceResearchEngine.buildDepartment(it, getApplication(), System.currentTimeMillis()) }

    fun purchaseResearchSlot() =
        execute { ReferenceResearchEngine.addResearchSlot(it, getApplication(), System.currentTimeMillis()) }

    fun startReferenceResearch(researchId: String) =
        execute { ReferenceResearchEngine.start(it, getApplication(), researchId, System.currentTimeMillis()) }

    fun createLocalCorporation(name: String, tag: String) =
        execute { ReferenceCorporationEngine.create(it, name, tag, System.currentTimeMillis()) }

    fun recruitCorporationNpc(name: String? = null, departmentId: String? = null) =
        execute { ReferenceCorporationEngine.recruitNpc(it, name, departmentId, System.currentTimeMillis()) }

    fun removeCorporationMember(memberId: String) =
        execute { ReferenceCorporationEngine.removeMember(it, memberId, System.currentTimeMillis()) }

    fun setCorporationMemberRank(memberId: String, rank: String) =
        execute { ReferenceCorporationEngine.setRank(it, memberId, rank, System.currentTimeMillis()) }

    fun transferCorporationFounder(memberId: String) =
        execute { ReferenceCorporationEngine.transferFounder(it, memberId, System.currentTimeMillis()) }

    fun leaveCorporation() =
        execute { ReferenceCorporationEngine.leave(it, System.currentTimeMillis()) }

    fun dissolveCorporation(confirmName: String) =
        execute { ReferenceCorporationEngine.dissolve(it, confirmName, System.currentTimeMillis()) }

    fun contributeToLocalCorporation(amount: Double) =
        execute { ReferenceCorporationEngine.contribute(it, getApplication(), amount, System.currentTimeMillis()) }

    fun completeCorporationShift() =
        execute { ReferenceCorporationEngine.shift(it, getApplication(), System.currentTimeMillis()) }

    fun upgradeCorporationFacility(facilityId: String) =
        execute { ReferenceCorporationEngine.upgradeFacility(it, getApplication(), facilityId, System.currentTimeMillis()) }

    fun establishCorporationDepartment(departmentId: String) =
        execute { ReferenceCorporationEngine.establishDepartment(it, getApplication(), departmentId, System.currentTimeMillis()) }

    fun assignCorporationOfficer(departmentId: String, memberId: String?) =
        execute { ReferenceCorporationEngine.assignOfficer(it, departmentId, memberId, System.currentTimeMillis()) }

    fun acquireCorporationBusiness(businessId: String) =
        execute { ReferenceCorporationEngine.acquireBusiness(it, getApplication(), businessId, System.currentTimeMillis()) }

    fun updateCorporationSettings(recruitmentMode: String? = null, description: String? = null) =
        execute { ReferenceCorporationEngine.setSettings(it, recruitmentMode, description, System.currentTimeMillis()) }

    fun maintainProperty(propertyId: String) =
        execute { LocalGameEngine.maintainProperty(it, propertyId, System.currentTimeMillis()) }

    fun buyReferenceProperty(definitionId: String) =
        execute { ReferencePropertyEngine.buy(it, getApplication(), definitionId, System.currentTimeMillis()) }

    fun upgradeReferenceProperty(instanceId: String, upgradeId: String) =
        execute { ReferencePropertyEngine.upgrade(it, getApplication(), instanceId, upgradeId, System.currentTimeMillis()) }

    fun maintainReferenceProperty(instanceId: String) =
        execute { ReferencePropertyEngine.maintain(it, instanceId, System.currentTimeMillis()) }

    fun collectReferenceRent(instanceId: String? = null) =
        execute { ReferencePropertyEngine.collect(it, instanceId, System.currentTimeMillis()) }

    fun collectMigratedRent(propertyId: String? = null) =
        execute { LocalGameEngine.collectLegacyRent(it, propertyId, System.currentTimeMillis()) }

    fun sellReferenceProperty(instanceId: String) =
        execute { ReferencePropertyEngine.sell(it, getApplication(), instanceId, System.currentTimeMillis()) }

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

    fun companyEconomy(instanceId: String) =
        ReferenceCompanyEngine.economy(_state.value, getApplication(), instanceId, System.currentTimeMillis())

    fun companySaleQuote(instanceId: String) =
        ReferenceCompanyEngine.saleQuote(_state.value, getApplication(), instanceId, System.currentTimeMillis())

    fun propertyEconomy(instanceId: String) =
        ReferencePropertyEngine.economy(_state.value, getApplication(), instanceId, System.currentTimeMillis())

    fun collectibleValuation(itemId: String) =
        ReferenceCollectibleEngine.valuation(getApplication(), itemId, System.currentTimeMillis())

    fun localNetWorth(): Double =
        ReferenceCollectibleEngine.netWorth(_state.value, getApplication(), System.currentTimeMillis())

    fun localMarketStocks(): List<ReferenceStockDefinition> =
        ReferenceMarketEngine.listStocks(System.currentTimeMillis())

    fun localMarketCrypto(): List<ReferenceCryptoDefinition> =
        ReferenceMarketEngine.listCrypto(System.currentTimeMillis())

    fun localStockPrice(symbol: String): Double? =
        ReferenceMarketEngine.getStockPrice(_state.value, symbol, System.currentTimeMillis())

    fun localCryptoPrice(symbol: String): Double? =
        ReferenceMarketEngine.getCryptoPrice(_state.value, symbol, System.currentTimeMillis())

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

    fun propertyUpgradeDefinitions(): List<JSONObject> =
        runCatching { ReferenceCatalogue.propertyUpgrades(getApplication()) }.getOrDefault(emptyList())

    fun collectibleCategoryDefinitions(): List<JSONObject> =
        runCatching { ReferenceCatalogue.collectibleCategories(getApplication()) }.getOrDefault(emptyList())

    fun corporationDepartmentDefinitions(): List<JSONObject> =
        runCatching { ReferenceCatalogue.corporationDepartments(getApplication()) }.getOrDefault(emptyList())

    fun corporationFacilityDefinitions(): List<JSONObject> =
        runCatching { ReferenceCatalogue.corporationFacilities(getApplication()) }.getOrDefault(emptyList())

    fun corporationBusinessDefinitions(): List<JSONObject> =
        runCatching { ReferenceCatalogue.corporationBusinesses(getApplication()) }.getOrDefault(emptyList())
}
