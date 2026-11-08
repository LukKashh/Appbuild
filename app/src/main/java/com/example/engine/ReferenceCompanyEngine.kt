package com.example.engine

import android.content.Context
import com.example.data.ReferenceCatalogue
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round

/** Persistent ownership record matching the webapp's OwnedCompany data. */
data class ReferenceOwnedCompany(
    val instanceId: String,
    val definitionId: String,
    val boughtAt: Long,
    val price: Double,
    val staff: Map<String, Int> = emptyMap(),
    val capacityLevel: Int = 0,
    val branches: Int = 0,
    val upgrades: Map<String, Int> = emptyMap(),
    val upgradeValue: Double = 0.0,
    val reserves: Double = 0.0,
    val liabilities: Double = 0.0,
    val uncollected: Double = 0.0,
    val suspended: Boolean = false,
    val autoSuspended: Boolean = false,
    val revenueTotal: Double = 0.0,
    val expenseTotal: Double = 0.0,
    val history: List<ReferenceCompanyHistory> = emptyList()
)

data class ReferenceCompanyHistory(val timestamp: Long, val revenue: Double, val expenses: Double)
data class ReferenceCompanyEconomy(
    val revenue: Double,
    val expenses: Double,
    val net: Double,
    val staffCount: Int,
    val staffNeeded: Int,
    val staffingFactor: Double,
    val expenseBreakdown: Map<String, Double>,
    val status: String
)
data class ReferenceCompanySaleQuote(
    val terminationCost: Double,
    val obligations: Double,
    val injectionNeeded: Double,
    val remainingReserves: Double,
    val remainingUncollected: Double,
    val valuation: Double,
    val payout: Double,
    val eligibleAt: Long,
    val eligible: Boolean
)

/**
 * Local port of the webapp business-empire rules. The bundled catalogue is the only
 * source of company definitions and costs; this object owns all player mutations.
 */
object ReferenceCompanyEngine {
    private const val HOUR = 3_600_000L
    private const val DAY = 86_400_000L
    private const val COMPANY_RESALe_PCT = 0.80
    private const val MIN_HOLD_HOURS = 6L

    private val SHORTAGE = mapOf(
        "lemon" to ("prop" to 0.0),
        "vending" to ("prop" to 1.0),
        "cafe" to ("floor" to 0.3),
        "carwash" to ("floor" to 0.5),
        "studio" to ("floor" to 0.4),
        "bakery" to ("prop" to 0.0),
        "logistics" to ("prop" to 0.0),
        "hotel" to ("prop" to 0.0),
        "factory" to ("floor" to 0.25),
        "bank" to ("floor" to 0.2),
        "energy" to ("floor" to 0.5),
        "studio_film" to ("floor" to 0.3)
    )

    private data class Mods(
        val businessPct: Double = 0.0,
        val offlineHours: Double = 0.0,
        val rentPct: Double = 0.0,
        val xpPct: Double = 0.0,
        val rpPct: Double = 0.0,
        val feeCut: Double = 0.0,
        val clickPct: Double = 0.0,
        val researchSpeed: Double = 0.0,
        val clickCap: Double = 0.0
    )

    fun buyCompany(state: LocalSimulationState, context: Context, definitionId: String, now: Long): LocalGameResult {
        val definition = ReferenceCatalogue.company(context, definitionId)
            ?: return reject(state, "Unknown company definition")
        if (state.capacityUpgrade != null) return reject(state, "Cannot purchase companies during a capacity upgrade")
        if (state.referenceCompanies.size >= state.businessCapacitySlots) {
            return reject(state, "No free company slots — upgrade Business Management")
        }
        val price = definition.optDouble("price", 0.0)
        if (price <= 0.0 || state.cash - state.reservedCash + 1e-9 < price) {
            return reject(state, "Insufficient available funds")
        }
        val roles = definition.optJSONArray("roles") ?: JSONArray()
        val staff = linkedMapOf<String, Int>()
        var left = definition.optInt("startStaff", 0)
        for (index in 0 until roles.length()) {
            val role = roles.optJSONObject(index) ?: continue
            val requested = if (index == roles.length() - 1) left else ceil(definition.optInt("startStaff", 0) * 0.8).toInt()
            val assigned = min(left, requested).coerceAtLeast(0)
            staff[role.optString("role")] = assigned
            left -= assigned
        }
        val instanceId = "local-company-${state.nextSequence}"
        val owned = ReferenceOwnedCompany(
            instanceId = instanceId,
            definitionId = definitionId,
            boughtAt = now,
            price = price,
            staff = staff,
            reserves = definition.optDouble("reserves", 0.0),
            liabilities = definition.optDouble("liabilities", 0.0)
        )
        val purchased = state.copy(
            cash = state.cash - price,
            referenceCompanies = state.referenceCompanies + owned,
            progress = state.progress.copy(companiesBought = state.progress.companiesBought + 1)
        )
        val next = awardXp(purchased, context, 15.0)
        return success(record(next, now, "company_purchase", "Acquired ${definition.optString("name")}", -price), "Company acquired")
    }

    fun hire(
        state: LocalSimulationState,
        context: Context,
        instanceId: String,
        roleName: String,
        count: Int,
        packageIndex: Int? = null,
        now: Long
    ): LocalGameResult {
        val company = findCompany(state, instanceId) ?: return reject(state, "Company not found")
        val definition = ReferenceCatalogue.company(context, company.definitionId)
            ?: return reject(state, "Company definition unavailable")
        val roles = definition.optJSONArray("roles") ?: JSONArray()
        val role = roles.firstObject { it.optString("role") == roleName }
            ?: return reject(state, "Unknown employee role")
        var hires = count
        var discount = 0.0
        if (packageIndex != null) {
            val packages = definition.optJSONArray("packages") ?: JSONArray()
            val pack = packages.optJSONObject(packageIndex) ?: return reject(state, "Unknown recruitment package")
            hires = pack.optInt("count", 0)
            discount = pack.optDouble("discount", 0.0)
        }
        if (hires < 1) return reject(state, "Invalid staff count")
        val branch = definition.optJSONObject("branch")
        val capacityDef = definition.optJSONObject("capacityUpgrade")
        val capacity = definition.optInt("staffCapacity", 0) +
            company.capacityLevel * (capacityDef?.optInt("add", 0) ?: 0) +
            company.branches * (branch?.optInt("staff", 0) ?: 0)
        val currentStaff = company.staff.values.sum()
        if (currentStaff + hires > capacity) return reject(state, "Employee capacity exceeded")
        val recruitmentCost = role.optDouble("recruit", 0.0) * hires * (1.0 - discount)
        if (state.cash - state.reservedCash + 1e-9 < recruitmentCost) return reject(state, "Insufficient available funds")
        val staff = company.staff.toMutableMap()
        staff[roleName] = (staff[roleName] ?: 0) + hires
        val updated = company.copy(staff = staff)
        val next = state.copy(
            cash = state.cash - recruitmentCost,
            referenceCompanies = state.referenceCompanies.replaceCompany(updated),
            progress = state.progress.copy(hires = state.progress.hires + hires)
        )
        return success(record(next, now, "recruitment", "Recruited $hires $roleName for ${definition.optString("name")}", -recruitmentCost), "Staff recruited")
    }

    fun fire(state: LocalSimulationState, context: Context, instanceId: String, roleName: String, count: Int, now: Long): LocalGameResult {
        val company = findCompany(state, instanceId) ?: return reject(state, "Company not found")
        val definition = ReferenceCatalogue.company(context, company.definitionId)
            ?: return reject(state, "Company definition unavailable")
        val role = (definition.optJSONArray("roles") ?: JSONArray()).firstObject { it.optString("role") == roleName }
            ?: return reject(state, "Unknown employee role")
        val available = company.staff[roleName] ?: 0
        val amount = min(available, count)
        if (amount < 1) return reject(state, "No employees in this role")
        val terminationCost = amount * role.optDouble("salary", 0.0) * 8.0
        val fromReserves = min(company.reserves, terminationCost)
        val remaining = terminationCost - fromReserves
        if (state.cash - state.reservedCash + 1e-9 < remaining) return reject(state, "Insufficient funds for termination costs")
        val updatedStaff = company.staff.toMutableMap().apply { this[roleName] = available - amount }
        val updated = company.copy(staff = updatedStaff, reserves = company.reserves - fromReserves)
        val next = state.copy(
            cash = state.cash - remaining,
            referenceCompanies = state.referenceCompanies.replaceCompany(updated)
        )
        return success(record(next, now, "termination", "Released $amount $roleName from ${definition.optString("name")}", -terminationCost), "Employees released")
    }

    fun upgrade(state: LocalSimulationState, context: Context, instanceId: String, upgradeId: String, now: Long): LocalGameResult {
        val company = findCompany(state, instanceId) ?: return reject(state, "Company not found")
        val definition = ReferenceCatalogue.company(context, company.definitionId)
            ?: return reject(state, "Company definition unavailable")
        val upgrade = (definition.optJSONArray("upgrades") ?: JSONArray()).firstObject { it.optString("id") == upgradeId }
            ?: return reject(state, "Unknown company upgrade")
        val level = company.upgrades[upgradeId] ?: 0
        val maximum = upgrade.optInt("max", 1)
        if (level >= maximum) return reject(state, "Upgrade is already at maximum level")
        val requiredResearch = upgrade.optString("research").takeIf { it.isNotBlank() && it != "null" }
        if (requiredResearch != null && requiredResearch !in state.completedResearch) {
            val researchName = ReferenceCatalogue.researchProject(context, requiredResearch)?.optString("name") ?: requiredResearch
            return reject(state, "Requires research: $researchName")
        }
        val cost = upgrade.optDouble("cost", 0.0) * upgrade.optDouble("growth", 1.0).pow(level)
        if (state.cash - state.reservedCash + 1e-9 < cost) return reject(state, "Insufficient available funds")
        val levels = company.upgrades.toMutableMap().apply { this[upgradeId] = level + 1 }
        val updated = company.copy(upgrades = levels, upgradeValue = company.upgradeValue + cost)
        val purchased = state.copy(
            cash = state.cash - cost,
            referenceCompanies = state.referenceCompanies.replaceCompany(updated),
            progress = state.progress.copy(upgradesPurchased = state.progress.upgradesPurchased + 1)
        )
        val next = awardXp(purchased, context, 5.0)
        return success(record(next, now, "company_upgrade", "Upgraded ${upgrade.optString("name")}", -cost), "Upgrade purchased")
    }

    fun upgradeStaffCapacity(state: LocalSimulationState, context: Context, instanceId: String, now: Long): LocalGameResult {
        val company = findCompany(state, instanceId) ?: return reject(state, "Company not found")
        val definition = ReferenceCatalogue.company(context, company.definitionId)
            ?: return reject(state, "Company definition unavailable")
        val capacity = definition.optJSONObject("capacityUpgrade") ?: return reject(state, "Staff capacity cannot be upgraded")
        val level = company.capacityLevel
        if (level >= capacity.optInt("max", 0)) return reject(state, "Staff capacity is at maximum")
        val cost = capacity.optDouble("cost", 0.0) * capacity.optDouble("growth", 1.0).pow(level)
        if (state.cash - state.reservedCash + 1e-9 < cost) return reject(state, "Insufficient available funds")
        val updated = company.copy(capacityLevel = level + 1, upgradeValue = company.upgradeValue + cost)
        val next = state.copy(cash = state.cash - cost, referenceCompanies = state.referenceCompanies.replaceCompany(updated))
        return success(record(next, now, "company_capacity", "Expanded staff capacity for ${definition.optString("name")}", -cost), "Staff capacity expanded")
    }

    fun openBranch(state: LocalSimulationState, context: Context, instanceId: String, now: Long): LocalGameResult {
        val company = findCompany(state, instanceId) ?: return reject(state, "Company not found")
        val definition = ReferenceCatalogue.company(context, company.definitionId)
            ?: return reject(state, "Company definition unavailable")
        val branch = definition.optJSONObject("branch") ?: return reject(state, "This company cannot open branches")
        if (company.branches >= branch.optInt("max", 0)) return reject(state, "Maximum branches reached")
        val cost = branch.optDouble("cost", 0.0) * branch.optDouble("growth", 1.0).pow(company.branches)
        if (state.cash - state.reservedCash + 1e-9 < cost) return reject(state, "Insufficient available funds")
        val updated = company.copy(branches = company.branches + 1, upgradeValue = company.upgradeValue + cost)
        val purchased = state.copy(
            cash = state.cash - cost,
            referenceCompanies = state.referenceCompanies.replaceCompany(updated),
            progress = state.progress.copy(branchesOpened = state.progress.branchesOpened + 1)
        )
        val next = awardXp(purchased, context, 30.0)
        return success(record(next, now, "branch_purchase", "Opened branch for ${definition.optString("name")}", -cost), "Branch opened")
    }

    fun injectCapital(state: LocalSimulationState, context: Context, instanceId: String, amount: Double, now: Long): LocalGameResult {
        val company = findCompany(state, instanceId) ?: return reject(state, "Company not found")
        val definition = ReferenceCatalogue.company(context, company.definitionId)
            ?: return reject(state, "Company definition unavailable")
        val value = floor(amount)
        if (value <= 0.0 || state.cash - state.reservedCash + 1e-9 < value) return reject(state, "Invalid amount or insufficient funds")
        val debtPayment = min(company.liabilities, value)
        val reserveAddition = value - debtPayment
        val updatedDebt = company.liabilities - debtPayment
        val baseDebt = definition.optDouble("liabilities", 0.0)
        val resume = company.autoSuspended && updatedDebt <= baseDebt
        val updated = company.copy(
            liabilities = updatedDebt,
            reserves = company.reserves + reserveAddition,
            suspended = if (resume) false else company.suspended,
            autoSuspended = if (resume) false else company.autoSuspended
        )
        val next = state.copy(cash = state.cash - value, referenceCompanies = state.referenceCompanies.replaceCompany(updated))
        return success(record(next, now, "capital_injection", "Injected capital into ${definition.optString("name")}", -value), if (resume) "Company resumed operations" else "Capital injected")
    }

    fun setSuspended(state: LocalSimulationState, instanceId: String, suspended: Boolean, now: Long): LocalGameResult {
        val company = findCompany(state, instanceId) ?: return reject(state, "Company not found")
        val updated = company.copy(suspended = suspended, autoSuspended = if (suspended) company.autoSuspended else false)
        val next = state.copy(referenceCompanies = state.referenceCompanies.replaceCompany(updated))
        return success(record(next, now, "company_status", if (suspended) "Operations suspended" else "Operations resumed", 0.0), if (suspended) "Operations suspended" else "Operations resumed")
    }

    fun collect(state: LocalSimulationState, instanceId: String? = null, now: Long): LocalGameResult {
        val targets = state.referenceCompanies.filter { instanceId == null || it.instanceId == instanceId }
        val amount = targets.sumOf { it.uncollected }
        if (amount <= 0.0) return reject(state, "Nothing to collect")
        val updated = state.referenceCompanies.map { company ->
            if (instanceId == null || company.instanceId == instanceId) company.copy(uncollected = 0.0) else company
        }
        val next = state.copy(cash = state.cash + amount, referenceCompanies = updated)
        return success(record(next, now, "company_collect", "Collected company operating income", amount), "Income collected")
    }

    fun saleQuote(state: LocalSimulationState, context: Context, instanceId: String, now: Long): ReferenceCompanySaleQuote? {
        val company = findCompany(state, instanceId) ?: return null
        val definition = ReferenceCatalogue.company(context, company.definitionId) ?: return null
        val roles = definition.optJSONArray("roles") ?: JSONArray()
        var termination = 0.0
        for (i in 0 until roles.length()) {
            val role = roles.optJSONObject(i) ?: continue
            termination += (company.staff[role.optString("role")] ?: 0) * role.optDouble("salary", 0.0) * 8.0
        }
        var obligations = company.liabilities + termination
        var remainingReserves = company.reserves
        var remainingUncollected = company.uncollected
        val fromReserves = min(remainingReserves, obligations)
        remainingReserves -= fromReserves
        obligations -= fromReserves
        val fromUncollected = min(remainingUncollected, obligations)
        remainingUncollected -= fromUncollected
        obligations -= fromUncollected
        val valuation = company.price + company.upgradeValue + remainingReserves + remainingUncollected
        val payout = valuation * COMPANY_RESALe_PCT
        val eligibleAt = company.boughtAt + MIN_HOLD_HOURS * HOUR
        return ReferenceCompanySaleQuote(
            terminationCost = termination,
            obligations = company.liabilities + termination,
            injectionNeeded = obligations,
            remainingReserves = remainingReserves,
            remainingUncollected = remainingUncollected,
            valuation = valuation,
            payout = payout,
            eligibleAt = eligibleAt,
            eligible = now >= eligibleAt
        )
    }

    fun sell(state: LocalSimulationState, context: Context, instanceId: String, confirmed: Boolean, now: Long): LocalGameResult {
        if (!confirmed) return reject(state, "Sale not confirmed")
        val company = findCompany(state, instanceId) ?: return reject(state, "Company not found")
        val quote = saleQuote(state, context, instanceId, now) ?: return reject(state, "Company definition unavailable")
        if (!quote.eligible) return reject(state, "Minimum ownership period (6h) has not been reached")
        if (state.cash - state.reservedCash + 1e-9 < quote.injectionNeeded) return reject(state, "Company settlement requires additional capital")
        val definition = ReferenceCatalogue.company(context, company.definitionId)
        val name = definition?.optString("name") ?: company.definitionId
        val next = state.copy(
            cash = state.cash - quote.injectionNeeded + quote.payout,
            referenceCompanies = state.referenceCompanies.filterNot { it.instanceId == instanceId },
            progress = state.progress.copy(companiesSold = state.progress.companiesSold + 1)
        )
        return success(record(next, now, "company_sale", "Sold $name; valuation ${quote.valuation}", quote.payout), "Company sold")
    }

    fun economy(state: LocalSimulationState, context: Context, instanceId: String, now: Long): ReferenceCompanyEconomy? {
        val company = findCompany(state, instanceId) ?: return null
        val definition = ReferenceCatalogue.company(context, company.definitionId) ?: return null
        val (personal, corporate) = modifiers(state, context)
        return companyEconomy(company, definition, personal, corporate, now)
    }

    /** Advance local business operations. Per-company offline eligibility and caps mirror the web reference. */
    fun tick(state: LocalSimulationState, context: Context, now: Long, offline: Boolean): Pair<LocalSimulationState, LocalOfflineSummary> {
        val elapsed = (now - state.lastSimulationAt).coerceAtLeast(0L)
        if (elapsed <= 0L || state.referenceCompanies.isEmpty()) {
            return state to LocalOfflineSummary(elapsed, emptyList(), 0.0, 0L)
        }
        val (personal, corporate) = modifiers(state, context)
        val balance = ReferenceCatalogue.balance(context)
        val offlineConfig = balance.optJSONObject("offline") ?: JSONObject()
        val idleThresholdHours = offlineConfig.optDouble("idleThresholdSec", 120.0) / 3600.0
        val lines = mutableListOf<LocalOfflineLine>()
        var lifetimeRevenue = 0.0
        var lifetimeExpenses = 0.0
        var researchPoints = 0.0
        var totalNet = 0.0
        var maxCappedMillis = 0L
        val updatedCompanies = state.referenceCompanies.map { company ->
            val definition = ReferenceCatalogue.company(context, company.definitionId) ?: return@map company
            val hoursRequested = elapsed / HOUR.toDouble()
            val offlineEligible = definition.optBoolean("offlineEligible", false)
            val maxOfflineHours = if (offlineEligible) {
                definition.optDouble("offlineMaxHours", 0.0) +
                    upgradeSum(company, definition, "offlineHours") +
                    personal.offlineHours + corporate.offlineHours
            } else idleThresholdHours
            val hours = if (offline) min(hoursRequested, maxOfflineHours) else hoursRequested
            if (offline && hoursRequested > hours) {
                maxCappedMillis = max(maxCappedMillis, ((hoursRequested - hours) * HOUR).toLong())
            }
            if (hours <= 0.0) return@map company
            val econ = companyEconomy(company, definition, personal, corporate, now)
            val revenue = econ.revenue * hours
            val expense = econ.expenses * hours
            var net = revenue - expense
            lifetimeRevenue += revenue
            lifetimeExpenses += expense
            totalNet += net
            researchPoints += upgradeSum(company, definition, "rpPerHour") * hours
            var reserves = company.reserves
            var liabilities = company.liabilities
            var wallet = company.uncollected
            var suspended = company.suspended
            var autoSuspended = company.autoSuspended
            if (net >= 0.0) {
                val debtPayment = min(liabilities, net * 0.25)
                liabilities -= debtPayment
                net -= debtPayment
                wallet += net
            } else {
                var loss = -net
                val fromReserves = min(reserves, loss)
                reserves -= fromReserves
                loss -= fromReserves
                val fromWallet = min(wallet, loss)
                wallet -= fromWallet
                loss -= fromWallet
                if (loss > 0.0) liabilities += loss
                val distressThreshold = definition.optDouble("liabilities", 0.0) +
                    definition.optDouble("fixedExpense", 0.0) * 24.0
                if (!suspended && liabilities > distressThreshold) {
                    suspended = true
                    autoSuspended = true
                }
            }
            val hourKey = (now / HOUR) * HOUR
            val history = company.history.toMutableList()
            val last = history.lastOrNull()
            if (last?.timestamp == hourKey) {
                history[history.lastIndex] = last.copy(revenue = last.revenue + revenue, expenses = last.expenses + expense)
            } else {
                history += ReferenceCompanyHistory(hourKey, revenue, expense)
            }
            while (history.size > 72) history.removeAt(0)
            lines += LocalOfflineLine(
                label = definition.optString("name", company.definitionId),
                gross = revenue,
                expenses = expense,
                net = revenue - expense,
                eligible = offlineEligible,
                note = if (offline && hoursRequested > hours) "Capped at %.1fh".format(java.util.Locale.US, hours) else "Added to company wallet"
            )
            company.copy(
                reserves = reserves,
                liabilities = liabilities,
                uncollected = wallet,
                suspended = suspended,
                autoSuspended = autoSuspended,
                revenueTotal = company.revenueTotal + revenue,
                expenseTotal = company.expenseTotal + expense,
                history = history
            )
        }
        val elapsedHours = elapsed / HOUR.toDouble()
        val baseResearch = if (state.researchBuilt) {
            balance.optJSONObject("research")?.optDouble("rpPerHour", 30.0)?.times(elapsedHours) ?: 0.0
        } else 0.0
        val totalResearch = (baseResearch + researchPoints) *
            globalMultiplier(state, context, "rpPct")
        val updatedProgress = state.copy(
            referenceCompanies = updatedCompanies,
            progress = state.progress.copy(
                lifetimeRevenue = state.progress.lifetimeRevenue + lifetimeRevenue,
                lifetimeExpenses = state.progress.lifetimeExpenses + lifetimeExpenses,
                researchPoints = state.progress.researchPoints + totalResearch
            )
        )
        return updatedProgress to LocalOfflineSummary(
            elapsedMillis = elapsed,
            lines = lines,
            totalNet = totalNet,
            cappedMillis = maxCappedMillis,
            researchPoints = totalResearch
        )
    }

    private fun companyEconomy(
        company: ReferenceOwnedCompany,
        definition: JSONObject,
        personal: Mods,
        corporate: Mods,
        now: Long
    ): ReferenceCompanyEconomy {
        val roles = definition.optJSONArray("roles") ?: JSONArray()
        val staffCount = company.staff.values.sum()
        val need = max(0, definition.optInt("minStaff", 0) +
            (definition.optJSONObject("branch")?.optInt("staff", 0) ?: 0) * company.branches -
            upgradeSum(company, definition, "staffEff").toInt())
        val shortage = SHORTAGE[company.definitionId] ?: ("prop" to 0.0)
        val factor = if (need > 0 && staffCount < need) {
            if (shortage.first == "prop") max(if (shortage.second == 1.0) 1.0 else 0.0, staffCount.toDouble() / need)
            else shortage.second
        } else 1.0
        val amp = if (company.definitionId == "hotel") 0.15 else 0.06
        val market = 1.0 + wave(hash(company.definitionId), now / HOUR.toDouble()) * amp
        val branch = definition.optJSONObject("branch")
        val grossBase = definition.optDouble("revenue", 0.0) +
            (branch?.optDouble("revenue", 0.0) ?: 0.0) * company.branches +
            max(0, staffCount - need) * definition.optDouble("revenuePerEmployee", 0.0)
        val businessMultiplier = (1.0 + upgradeSum(company, definition, "revenuePct")) *
            (1.0 + corporate.businessPct) * (1.0 + personal.businessPct)
        val revenue = grossBase * businessMultiplier * factor * market
        val expenseCut = upgradeSum(company, definition, "expensePct").coerceIn(0.0, 0.7)
        val fixed = (definition.optDouble("fixedExpense", 0.0) +
            (branch?.optDouble("expense", 0.0) ?: 0.0) * company.branches) * (1.0 - expenseCut)
        var salaries = 0.0
        for (index in 0 until roles.length()) {
            val role = roles.optJSONObject(index) ?: continue
            salaries += (company.staff[role.optString("role")] ?: 0) * role.optDouble("salary", 0.0)
        }
        val effectiveRevenue: Double
        val effectiveFixed: Double
        val effectiveSalaries: Double
        if (company.suspended) {
            effectiveRevenue = 0.0
            effectiveFixed = fixed * 0.15
            effectiveSalaries = salaries * 0.5
        } else {
            effectiveRevenue = revenue
            effectiveFixed = fixed
            effectiveSalaries = salaries
        }
        val breakdown = linkedMapOf<String, Double>()
        val expenseBreakdown = definition.optJSONObject("expenseBreakdown") ?: JSONObject()
        val keys = expenseBreakdown.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            breakdown[key] = effectiveFixed * expenseBreakdown.optDouble(key, 0.0)
        }
        breakdown["Salaries"] = effectiveSalaries
        val expenses = effectiveFixed + effectiveSalaries
        val net = effectiveRevenue - expenses
        val status = when {
            company.suspended -> "Suspended"
            company.liabilities > definition.optDouble("liabilities", 0.0) + 1.0 && net < 0.0 -> "Distressed"
            kotlin.math.abs(net) < effectiveRevenue * 0.02 -> "Break-even"
            net < 0.0 -> "Unprofitable"
            else -> "Profitable"
        }
        return ReferenceCompanyEconomy(effectiveRevenue, expenses, net, staffCount, need, factor, breakdown, status)
    }

    private fun modifiers(state: LocalSimulationState, context: Context): Pair<Mods, Mods> {
        val personal = linkedMapOf<String, Double>()
        for (id in state.completedResearch) {
            val effect = ReferenceCatalogue.researchProject(context, id)?.optJSONObject("effect") ?: continue
            addEffects(personal, effect, 1.0)
        }
        val bestCollectible = linkedMapOf<String, Double>()
        val ownedItems = state.collectibles.map { it.definitionId }.toSet()
        val collectibleDefs = ReferenceCatalogue.collectibleDefinitions(context).associateBy { it.optString("id") }
        for (id in ownedItems) {
            val bonus = collectibleDefs[id]?.optJSONObject("bonus") ?: continue
            val key = bonus.optString("key")
            val value = bonus.optDouble("value", 0.0)
            if (key.isNotBlank()) bestCollectible[key] = max(bestCollectible[key] ?: 0.0, value)
        }
        bestCollectible.forEach { (key, value) -> personal[key] = (personal[key] ?: 0.0) + value }
        for (collection in ReferenceCatalogue.collections(context)) {
            val items = collection.optJSONArray("items") ?: JSONArray()
            val complete = (0 until items.length()).all { ownedItems.contains(items.optString(it)) }
            if (complete) {
                val bonus = collection.optJSONObject("bonus") ?: continue
                val key = bonus.optString("key")
                if (key.isNotBlank()) personal[key] = (personal[key] ?: 0.0) + bonus.optDouble("value", 0.0)
            }
        }
        val corp = linkedMapOf<String, Double>()
        val corporation = state.corporation
        if (corporation != null) {
            val facilities = ReferenceCatalogue.corporationFacilities(context)
            for (facility in facilities) {
                val id = facility.optString("id")
                val level = corporation.facilities[id] ?: 0
                val key = facility.optString("bonus")
                if (level > 0 && key.isNotBlank()) {
                    corp[key] = (corp[key] ?: 0.0) + level * facility.optDouble("perLevel", 0.0)
                }
            }
        }
        return Mods(
            businessPct = personal["businessPct"] ?: 0.0,
            offlineHours = personal["offlineHours"] ?: 0.0,
            rentPct = personal["rentPct"] ?: 0.0,
            xpPct = personal["xpPct"] ?: 0.0,
            rpPct = personal["rpPct"] ?: 0.0,
            feeCut = min(0.6, personal["feeCut"] ?: 0.0),
            clickPct = personal["clickPct"] ?: 0.0,
            researchSpeed = personal["researchSpeed"] ?: 0.0,
            clickCap = personal["clickCap"] ?: 0.0
        ) to Mods(
            businessPct = corp["businessPct"] ?: 0.0,
            offlineHours = corp["offlineHours"] ?: 0.0,
            rentPct = corp["rentPct"] ?: 0.0,
            xpPct = corp["xpPct"] ?: 0.0,
            rpPct = corp["rpPct"] ?: 0.0,
            feeCut = corp["feeCut"] ?: 0.0,
            clickPct = corp["clickPct"] ?: 0.0,
            researchSpeed = corp["researchSpeed"] ?: 0.0,
            clickCap = corp["clickCap"] ?: 0.0
        )
    }

    fun globalMultiplier(state: LocalSimulationState, context: Context, key: String): Double {
        val (personal, corporate) = modifiers(state, context)
        fun value(mods: Mods): Double = when (key) {
            "businessPct" -> mods.businessPct
            "offlineHours" -> mods.offlineHours
            "rentPct" -> mods.rentPct
            "xpPct" -> mods.xpPct
            "rpPct" -> mods.rpPct
            "feeCut" -> mods.feeCut
            "clickPct" -> mods.clickPct
            "researchSpeed" -> mods.researchSpeed
            "clickCap" -> mods.clickCap
            else -> 0.0
        }
        return (1.0 + value(personal)) * (1.0 + value(corporate))
    }

    fun hashText(value: String): Int = hash(value)
    fun deterministicWave(seed: Int, time: Double): Double = wave(seed, time)

    private fun upgradeSum(company: ReferenceOwnedCompany, definition: JSONObject, key: String): Double {
        val upgrades = definition.optJSONArray("upgrades") ?: JSONArray()
        var total = 0.0
        for (index in 0 until upgrades.length()) {
            val upgrade = upgrades.optJSONObject(index) ?: continue
            val effect = upgrade.optJSONObject("effect") ?: continue
            total += (company.upgrades[upgrade.optString("id")] ?: 0) * effect.optDouble(key, 0.0)
        }
        return total
    }

    private fun awardXp(state: LocalSimulationState, context: Context, baseXp: Double): LocalSimulationState {
        val award = PlayerProgression.award(
            state.progress,
            baseXp * globalMultiplier(state, context, "xpPct")
        )
        return state.copy(
            progress = award.progress,
            cash = state.cash + award.cashReward
        )
    }

    private fun record(state: LocalSimulationState, now: Long, category: String, description: String, amount: Double): LocalSimulationState {
        val entry = LocalLedgerEntry("local-${state.nextSequence}", now, category, description, amount, state.cash)
        return state.copy(ledger = (state.ledger + entry).takeLast(5000), nextSequence = state.nextSequence + 1)
    }

    private fun findCompany(state: LocalSimulationState, id: String) = state.referenceCompanies.firstOrNull { it.instanceId == id }
    private fun List<ReferenceOwnedCompany>.replaceCompany(updated: ReferenceOwnedCompany) =
        map { if (it.instanceId == updated.instanceId) updated else it }

    private fun success(state: LocalSimulationState, message: String) = LocalGameResult.Success(state, message)
    private fun reject(state: LocalSimulationState, message: String) = LocalGameResult.Rejected(state, message)

    private fun JSONArray.firstObject(predicate: (JSONObject) -> Boolean): JSONObject? {
        for (index in 0 until length()) {
            val value = optJSONObject(index) ?: continue
            if (predicate(value)) return value
        }
        return null
    }

    private fun addEffects(target: MutableMap<String, Double>, effect: JSONObject, multiplier: Double) {
        val keys = effect.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            target[key] = (target[key] ?: 0.0) + effect.optDouble(key, 0.0) * multiplier
        }
    }

    private fun hash(value: String): Int {
        var result = 2166136261L
        value.forEach { ch -> result = (result xor ch.code.toLong()) * 16777619L and 0xffffffffL }
        return result.toInt()
    }

    private fun wave(seed: Int, time: Double): Double {
        var state = seed.toLong() and 0xffffffffL
        fun random(): Double {
            state = (state + 0x6d2b79f5L) and 0xffffffffL
            var t = state
            t = ((t xor (t ushr 15)) * (t or 1L)) and 0xffffffffL
            t = (t xor (t + (((t xor (t ushr 7)) * (t or 61L)) and 0xffffffffL))) and 0xffffffffL
            return ((t xor (t ushr 14)) and 0xffffffffL).toDouble() / 4294967296.0
        }
        var sum = 0.0
        var norm = 0.0
        repeat(6) { index ->
            val period = 20.0 + random() * 600.0 * (index + 1)
            val phase = random() * Math.PI * 2.0
            val amplitude = 1.0 / (index + 1)
            sum += amplitude * kotlin.math.sin((time / period) * Math.PI * 2.0 + phase)
            norm += amplitude
        }
        // jitter is deterministic for this seed and time bucket.
        val jitterSeed = seed xor floor(time).toInt()
        state = jitterSeed.toLong() and 0xffffffffL
        val jitter = random()
        return (sum / norm) * 0.9 + (jitter - 0.5) * 0.2
    }
}
