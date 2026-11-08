package com.example.engine

import com.example.model.*
import java.util.UUID
import kotlin.math.max
import kotlin.math.pow

/** Single-device corporation simulation. Named operators are local NPCs, not connected players. */
object LocalCorporationEngine {
    data class Facility(val id: String, val title: String, val baseCost: Double, val description: String, val maxLevel: Int = 5)
    data class Department(val id: String, val title: String, val cost: Double)
    data class BusinessDefinition(val id: String, val title: String, val department: String, val cost: Double, val revenueHour: Double, val expensesHour: Double)

    val facilities = listOf(
        Facility("headquarters", "Corporate Headquarters", 200000.0, "+5 simulated staff capacity per level"),
        Facility("training", "Executive Training Centre", 150000.0, "+1% tap yield per level"),
        Facility("operations", "Operations Centre", 400000.0, "+1% corporate business revenue per level"),
        Facility("academy", "Management Academy", 250000.0, "+1% corporation XP per level"),
        Facility("research", "Shared Research Laboratory", 300000.0, "+2% research point generation per level"),
        Facility("realty", "Property Management Office", 350000.0, "+1% rental performance per level"),
        Facility("trading", "Investment & Trading Desk", 300000.0, "Reduced simulated trading fees")
    )
    val departments = listOf(
        Department("industrial", "Industrial Operations", 250000.0),
        Department("mining", "Mining & Resources", 300000.0),
        Department("logistics", "Logistics & Transportation", 250000.0),
        Department("energy", "Energy & Utilities", 400000.0),
        Department("technology", "Technology & Data", 350000.0),
        Department("commercial", "Commercial Operations", 200000.0)
    )
    val businesses = listOf(
        BusinessDefinition("quarry", "Granite Quarry", "mining", 300000.0, 9000.0, 4000.0),
        BusinessDefinition("depot", "Freight Distribution Depot", "logistics", 500000.0, 14000.0, 6000.0),
        BusinessDefinition("mill", "Integrated Steel Mill", "industrial", 1200000.0, 32000.0, 15000.0),
        BusinessDefinition("solar", "Solar Generation Park", "energy", 900000.0, 22000.0, 7000.0),
        BusinessDefinition("datacenter", "Enterprise Data Centre", "technology", 2000000.0, 52000.0, 22000.0),
        BusinessDefinition("trademarket", "Wholesale Trade Exchange", "commercial", 400000.0, 11000.0, 4500.0)
    )
    private val operatorNames = listOf("Ada Mercer", "Elias Voss", "Mira Chen", "Jonas Vale", "Sofia Hart", "Noah Beck", "Lea Novak", "Oskar Reed")

    fun found(s: GameState, name: String, tag: String, now: Long = System.currentTimeMillis()): GameState {
        require(!s.corporation.isFounded) { "A corporation already exists on this save." }
        require(s.progression.level >= 10) { "Corporations unlock at Player Level 10." }
        require(s.cash >= 10_000_000.0) { "You need $10,000,000 founding capital." }
        val n = name.trim().take(30)
        val t = tag.trim().uppercase().take(5)
        require(n.length in 3..30) { "Name must contain 3 to 30 characters." }
        require(t.matches(Regex("[A-Z0-9]{2,5}"))) { "Tag must contain 2 to 5 letters or numbers." }
        val staff = listOf(LocalCorpMember("player", s.profile.username, "Founder", "Chairperson", now)) +
            operatorNames.take(4).mapIndexed { index, operator -> LocalCorpMember("local_ai_" + index, operator, "Operator", listOf("Operations", "Finance", "Strategy", "Development")[index], now, true) }
        val objective = LocalCorpObjective("first_capital", "Build the Treasury", "Contribute $20,000 of personal capital.", 20000.0, rewardXp = 350.0, rewardTreasury = 5000.0, expiresAt = now + 48L * 3600L * 1000L)
        val corp = LocalCorporation(UUID.randomUUID().toString(), n, t, treasury = 0.0, foundedAt = now,
            members = staff, objectives = listOf(objective), activityLog = listOf("Corporation founded on this device. Simulated operators are local NPCs."))
        val tx = Transaction(UUID.randomUUID().toString(), now, TransactionType.BUSINESS, "Founded " + n, -10_000_000.0)
        return s.copy(cash = s.cash - 10_000_000.0, corporation = corp, transactions = (listOf(tx) + s.transactions).take(200))
    }

    fun contribute(s: GameState, amount: Double, now: Long = System.currentTimeMillis()): GameState {
        val corp = s.corporation
        require(corp.isFounded) { "Found a corporation first." }
        val value = amount.toLong().toDouble()
        require(value > 0.0 && value <= s.cash) { "Invalid amount or insufficient cash." }
        val day = now / 86400000L
        val today = if (corp.lastContributionDay == day) corp.contributedToday else 0.0
        val limit = 10000.0 * 1.6.pow((corp.level - 1).toDouble())
        require(today + value <= limit) { "Daily contribution cap reached." }
        val members = corp.members.map {
            if (it.id == "player") it.copy(totalContribution = it.totalContribution + value) else it
        }
        val objectives = corp.objectives.map { if (!it.isClaimed && it.id == "first_capital") it.copy(progress = (it.progress + value).coerceAtMost(it.target)) else it }
        val xp = value / limit * 1000.0
        val updated = levelUp(corp.copy(treasury = corp.treasury + value, xp = corp.xp + xp, members = members,
            contributedToday = today + value, lastContributionDay = day, objectives = objectives,
            activityLog = (listOf("Treasury contribution received: $" + value.toLong()) + corp.activityLog).take(100)))
        val tx = Transaction(UUID.randomUUID().toString(), now, TransactionType.BUSINESS, "Corporation treasury contribution", -value)
        return s.copy(cash = s.cash - value, corporation = updated, progression = LocalSimulationEngine.addXp(s.progression, xp * 0.25),
            transactions = (listOf(tx) + s.transactions).take(200))
    }

    fun managementShift(s: GameState, now: Long = System.currentTimeMillis()): GameState {
        val corp = s.corporation
        require(corp.isFounded) { "Found a corporation first." }
        require(now - corp.lastShiftAt >= 30L * 60L * 1000L) { "Your next management shift is not ready yet." }
        val members = corp.members.map { if (it.id == "player") it.copy(shiftsCompleted = it.shiftsCompleted + 1) else it }
        val objective = corp.objectives.map { if (it.id == "management_shifts" && !it.isClaimed) it.copy(progress = (it.progress + 1.0).coerceAtMost(it.target)) else it }
        return s.copy(corporation = levelUp(corp.copy(lastShiftAt = now, xp = corp.xp + 25.0, members = members, objectives = objective,
            activityLog = (listOf("Management shift completed. Planning, compliance and staffing reviewed.") + corp.activityLog).take(100))),
            progression = LocalSimulationEngine.addXp(s.progression, 20.0))
    }

    fun upgradeFacility(s: GameState, id: String): GameState {
        val corp = s.corporation
        require(corp.isFounded) { "Found a corporation first." }
        val def = facilities.firstOrNull { it.id == id } ?: throw IllegalArgumentException("Unknown facility.")
        val level = corp.facilities[id] ?: 0
        require(level < def.maxLevel) { "Facility is already at its maximum level." }
        val cost = def.baseCost * 1.9.pow(level.toDouble())
        require(corp.treasury >= cost) { "Insufficient corporation treasury." }
        return s.copy(corporation = corp.copy(treasury = corp.treasury - cost, facilities = corp.facilities + (id to (level + 1)),
            activityLog = (listOf(def.title + " upgraded to level " + (level + 1)) + corp.activityLog).take(100)))
    }

    fun establishDepartment(s: GameState, id: String): GameState {
        val corp = s.corporation
        val def = departments.firstOrNull { it.id == id } ?: throw IllegalArgumentException("Unknown department.")
        require(corp.isFounded && id !in corp.departments) { "Department unavailable or already built." }
        require(corp.treasury >= def.cost) { "Insufficient corporation treasury." }
        return s.copy(corporation = corp.copy(treasury = corp.treasury - def.cost, departments = corp.departments + id,
            activityLog = (listOf(def.title + " department established.") + corp.activityLog).take(100)))
    }

    fun acquireBusiness(s: GameState, id: String): GameState {
        val corp = s.corporation
        val def = businesses.firstOrNull { it.id == id } ?: throw IllegalArgumentException("Unknown corporation business.")
        require(corp.isFounded && def.department in corp.departments) { "Required department is not established." }
        require(corp.businesses.none { it.id == id }) { "Corporation already owns this business." }
        require(corp.treasury >= def.cost) { "Insufficient corporation treasury." }
        val owned = LocalCorpBusiness(id, def.title, def.department, def.cost, def.revenueHour, def.expensesHour)
        return s.copy(corporation = corp.copy(treasury = corp.treasury - def.cost, businesses = corp.businesses + owned,
            activityLog = (listOf("Acquired corporate subsidiary: " + def.title) + corp.activityLog).take(100)))
    }

    fun advance(corp: LocalCorporation, elapsedSeconds: Double, now: Long): LocalCorporation {
        if (!corp.isFounded || elapsedSeconds <= 0.0) return corp
        val hours = elapsedSeconds / 3600.0
        val revenue = corp.businesses.sumOf { it.revenuePerHour } * hours
        val costs = corp.businesses.sumOf { it.expensesPerHour } * hours
        val net = revenue - costs
        val businesses = corp.businesses.map { it.copy(accumulatedRevenue = it.accumulatedRevenue + it.revenuePerHour * hours) }
        return levelUp(corp.copy(treasury = (corp.treasury + net).coerceAtLeast(0.0), businesses = businesses,
            xp = corp.xp + max(0.0, net) * 0.0005))
    }

    fun claimObjective(s: GameState, id: String): GameState {
        val corp = s.corporation
        val objective = corp.objectives.firstOrNull { it.id == id } ?: return s
        if (objective.isClaimed || objective.progress < objective.target) return s
        val updated = corp.copy(treasury = corp.treasury + objective.rewardTreasury, xp = corp.xp + objective.rewardXp,
            completedObjectives = corp.completedObjectives + 1, objectives = corp.objectives.map { if (it.id == id) it.copy(isClaimed = true) else it },
            activityLog = (listOf("Objective reward claimed: " + objective.name) + corp.activityLog).take(100))
        return s.copy(corporation = levelUp(updated))
    }

    private fun levelUp(input: LocalCorporation): LocalCorporation {
        var level = input.level
        var xp = input.xp
        var required = 1000.0 * 1.5.pow((level - 1).toDouble())
        while (xp >= required && level < 1000) { xp -= required; level++; required = 1000.0 * 1.5.pow((level - 1).toDouble()) }
        return input.copy(level = level, xp = xp)
    }
}