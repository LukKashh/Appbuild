package com.example.engine

import android.content.Context
import org.json.JSONObject
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.random.Random

/**
 * Device-local corporation simulation. Other members are explicitly marked AI/NPC members;
 * no online account, Firebase function or remote player identity is fabricated.
 */
object ReferenceCorporationEngine {
    private const val DAY_MS = 86_400_000L
    private const val HOUR_MS = 3_600_000L
    private const val CREATION_FEE = 10_000_000.0
    private const val LEAVE_COOLDOWN_HOURS = 12L
    private const val SHIFT_COOLDOWN_MINUTES = 30L
    private val rankOrder = listOf("Founder", "President", "Vice President", "Executive Officer", "Member")
    private val npcNames = listOf(
        "Alex Morgan", "Maya Bennett", "Noah Carter", "Olivia Reed", "Ethan Brooks",
        "Amelia Hayes", "Leo Foster", "Sofia Turner", "Lucas Ward", "Chloe Parker",
        "Mason Price", "Isla Cooper", "Daniel Ross", "Grace Miller", "Ava Collins"
    )

    fun create(
        state: LocalSimulationState,
        nameInput: String,
        tagInput: String,
        now: Long
    ): LocalGameResult {
        if (state.progress.level < 10) return reject(state, "Corporations unlock at Player Level 10")
        if (state.corporation != null) return reject(state, "You are already in a corporation")
        if (state.cash - state.reservedCash + 1e-9 < CREATION_FEE) return reject(state, "Insufficient funds for the corporation fee")
        val name = nameInput.trim()
        val tag = tagInput.trim().uppercase()
        if (name.length !in 3..30) return reject(state, "Corporation name must be 3–30 characters")
        if (!tag.matches(Regex("[A-Z0-9]{2,5}"))) return reject(state, "Tag must contain 2–5 letters or numbers")
        val corpId = "local-corp-${state.nextSequence}"
        val member = LocalCorporationMember(
            playerId = state.playerId,
            displayName = state.playerName,
            rank = "Founder",
            department = null,
            joinedAt = now
        )
        var corp = LocalCorporation(
            id = corpId,
            name = name,
            tag = tag,
            founderId = state.playerId,
            createdAt = now,
            members = listOf(member),
            lastTick = now,
            activityLog = listOf("[$now] ${state.playerName} founded $name")
        )
        corp = ensureObjectives(corp, now)
        val next = state.copy(
            cash = state.cash - CREATION_FEE,
            corporation = corp
        )
        return success(record(next, now, "corp_creation_fee", "Founded ${corp.name}", -CREATION_FEE), "Corporation founded")
    }

    /** Recruit a locally simulated member so the cooperative corporation loop remains playable offline. */
    fun recruitNpc(
        state: LocalSimulationState,
        npcName: String? = null,
        departmentId: String? = null,
        now: Long
    ): LocalGameResult {
        val corp = state.corporation ?: return reject(state, "You are not in a corporation")
        val actor = corp.members.firstOrNull { it.playerId == state.playerId } ?: return reject(state, "Local membership is missing")
        if (actor.rank !in setOf("Founder", "President", "Vice President")) return reject(state, "Your rank cannot recruit members")
        if (corp.members.size >= capacity(corp)) return reject(state, "Corporation member capacity reached")
        if (departmentId != null && departmentId !in corp.departments) return reject(state, "Establish the department first")
        val availableName = npcName?.trim()?.takeIf { it.isNotBlank() }
            ?: npcNames.firstOrNull { candidate -> corp.members.none { it.displayName == candidate } }
            ?: "Operations Member ${corp.members.count { it.isNpc } + 1}"
        val memberId = "npc-${corp.id}-${corp.members.count { it.isNpc } + 1}"
        if (corp.members.any { it.displayName.equals(availableName, true) }) return reject(state, "That member name is already used")
        val npc = LocalCorporationMember(
            playerId = memberId,
            displayName = availableName,
            rank = "Member",
            department = departmentId,
            joinedAt = now,
            isNpc = true
        )
        val updated = corp.copy(
            members = corp.members + npc,
            activityLog = log(corp.activityLog, now, "$availableName joined as a local AI member")
        )
        return success(state.copy(corporation = ensureObjectives(updated, now)), "AI member recruited")
    }

    fun removeMember(state: LocalSimulationState, targetId: String, now: Long): LocalGameResult {
        val corp = state.corporation ?: return reject(state, "You are not in a corporation")
        val actor = corp.members.firstOrNull { it.playerId == state.playerId } ?: return reject(state, "Not a corporation member")
        val target = corp.members.firstOrNull { it.playerId == targetId } ?: return reject(state, "Member not found")
        if (targetId == state.playerId || targetId == corp.founderId) return reject(state, "Cannot remove this member")
        if (actor.rank !in setOf("Founder", "President")) return reject(state, "Your rank cannot remove members")
        val actorRank = rankOrder.indexOf(actor.rank)
        val targetRank = rankOrder.indexOf(target.rank)
        if (targetRank <= actorRank) return reject(state, "Cannot remove an equal or higher rank")
        val updated = corp.copy(
            members = corp.members.filterNot { it.playerId == targetId },
            departments = corp.departments.mapValues { (_, d) ->
                if (d.officerPlayerId == targetId) d.copy(officerPlayerId = null) else d
            },
            activityLog = log(corp.activityLog, now, "${target.displayName} was removed")
        )
        return success(state.copy(corporation = updated), "Corporation member removed")
    }

    fun setRank(state: LocalSimulationState, targetId: String, newRank: String, now: Long): LocalGameResult {
        val corp = state.corporation ?: return reject(state, "You are not in a corporation")
        if (corp.founderId != state.playerId) return reject(state, "Only the Founder can change member ranks")
        if (newRank !in rankOrder.drop(1)) return reject(state, "Use leadership transfer to assign Founder")
        if (targetId == state.playerId) return reject(state, "You cannot change your own rank here")
        val target = corp.members.firstOrNull { it.playerId == targetId } ?: return reject(state, "Member not found")
        val updatedMembers = corp.members.map { if (it.playerId == targetId) it.copy(rank = newRank) else it }
        val updatedDepartments = if (newRank == "Executive Officer") corp.departments else
            corp.departments.mapValues { (_, d) -> if (d.officerPlayerId == targetId) d.copy(officerPlayerId = null) else d }
        val updated = corp.copy(
            members = updatedMembers,
            departments = updatedDepartments,
            activityLog = log(corp.activityLog, now, "${target.displayName} is now $newRank")
        )
        return success(state.copy(corporation = updated), "Member rank updated")
    }

    fun transferFounder(state: LocalSimulationState, newFounderId: String, now: Long): LocalGameResult {
        val corp = state.corporation ?: return reject(state, "You are not in a corporation")
        if (corp.founderId != state.playerId) return reject(state, "Only the Founder can transfer leadership")
        val target = corp.members.firstOrNull { it.playerId == newFounderId } ?: return reject(state, "New Founder not found")
        if (newFounderId == state.playerId) return reject(state, "Already the Founder")
        val members = corp.members.map {
            when (it.playerId) {
                newFounderId -> it.copy(rank = "Founder")
                state.playerId -> it.copy(rank = "President")
                else -> it
            }
        }
        val updated = corp.copy(
            founderId = newFounderId,
            members = members,
            departments = corp.departments.mapValues { (_, d) ->
                if (d.officerPlayerId == newFounderId && target.rank != "Executive Officer") d.copy(officerPlayerId = null) else d
            },
            activityLog = log(corp.activityLog, now, "${target.displayName} became Founder")
        )
        return success(state.copy(corporation = updated), "Leadership transferred")
    }

    fun leave(state: LocalSimulationState, now: Long): LocalGameResult {
        val corp = state.corporation ?: return reject(state, "You are not in a corporation")
        if (corp.founderId == state.playerId) return reject(state, "Founders must transfer leadership or dissolve the corporation")
        val me = corp.members.firstOrNull { it.playerId == state.playerId } ?: return reject(state, "Membership not found")
        val updated = corp.copy(
            members = corp.members.filterNot { it.playerId == state.playerId },
            departments = corp.departments.mapValues { (_, d) ->
                if (d.officerPlayerId == state.playerId) d.copy(officerPlayerId = null) else d
            },
            activityLog = log(corp.activityLog, now, "${me.displayName} left the corporation")
        )
        val next = state.copy(corporation = updated, corporationCooldownUntil = now + LEAVE_COOLDOWN_HOURS * HOUR_MS)
        return success(next, "Left corporation. Membership cooldown: 12 hours")
    }

    fun dissolve(state: LocalSimulationState, confirmName: String, now: Long): LocalGameResult {
        val corp = state.corporation ?: return reject(state, "You are not in a corporation")
        if (corp.founderId != state.playerId) return reject(state, "Only the Founder can dissolve the corporation")
        if (confirmName != corp.name) return reject(state, "Type the exact corporation name to confirm")
        val entry = LocalLedgerEntry(
            id = "local-${state.nextSequence}",
            timestamp = now,
            category = "corp_dissolution",
            description = "Dissolved ${corp.name}",
            amount = 0.0,
            balanceAfter = state.cash
        )
        return success(state.copy(corporation = null, ledger = (state.ledger + entry).takeLast(5000), nextSequence = state.nextSequence + 1), "Corporation dissolved")
    }

    fun contribute(state: LocalSimulationState, amountInput: Double, now: Long): LocalGameResult {
        val corp = state.corporation ?: return reject(state, "You are not in a corporation")
        val meIndex = corp.members.indexOfFirst { it.playerId == state.playerId }
        if (meIndex < 0) return reject(state, "Corporation membership not found")
        val amount = floor(amountInput)
        if (amount <= 0.0) return reject(state, "Enter a positive contribution")
        if (state.cash - state.reservedCash + 1e-9 < amount) return reject(state, "Insufficient funds")
        val day = Math.floorDiv(now, DAY_MS)
        val me = corp.members[meIndex]
        val todaysContribution = if (me.contributionDay == day) me.contributionToday else 0.0
        val limit = dailyLimit(corp)
        if (todaysContribution + amount > limit) {
            return reject(state, "Daily contribution limit: ${(limit - todaysContribution).toLong()} remaining")
        }
        val xp = contributionXp(limit, todaysContribution, amount)
        val updatedMember = me.copy(
            contributionDay = day,
            contributionToday = todaysContribution + amount,
            lifetimeContribution = me.lifetimeContribution + amount
        )
        val members = corp.members.toMutableList().apply { this[meIndex] = updatedMember }
        var updatedCorp = corp.copy(
            treasury = corp.treasury + amount,
            members = members,
            activityLog = log(corp.activityLog, now, "${me.displayName} contributed $${amount.toLong()}")
        )
        updatedCorp = addCorpXp(updatedCorp, xp.roundToLong(), now)
        updatedCorp = progressObjectives(updatedCorp, state.playerId, "funds", amount, now)
        val playerAward = PlayerProgression.award(
            state.progress,
            xp.roundToLong().toDouble() * ReferenceCompanyEngine.globalMultiplier(state, requireContextPlaceholder(), "xpPct")
        )
        val next = state.copy(
            cash = state.cash - amount,
            corporation = updatedCorp,
            progress = playerAward.progress.copy(corpContributed = state.progress.corpContributed + amount),
            playerName = state.playerName,
        )
        val rewarded = next.copy(cash = next.cash + playerAward.cashReward)
        return success(record(rewarded, now, "corp_contribution", "Contributed to ${corp.name}", -amount), "Contribution recorded")
    }

    fun shift(state: LocalSimulationState, now: Long): LocalGameResult {
        val corp = state.corporation ?: return reject(state, "You are not in a corporation")
        if (now - state.lastCorporationShiftAt < SHIFT_COOLDOWN_MINUTES * 60_000L) {
            return reject(state, "You need a break before your next corporation shift")
        }
        val index = corp.members.indexOfFirst { it.playerId == state.playerId }
        if (index < 0) return reject(state, "Corporation membership not found")
        val member = corp.members[index].copy(shiftsCompleted = corp.members[index].shiftsCompleted + 1)
        val members = corp.members.toMutableList().apply { this[index] = member }
        var updated = corp.copy(members = members, activityLog = log(corp.activityLog, now, "${member.displayName} completed a shift"))
        updated = addCorpXp(updated, 25L, now)
        updated = progressObjectives(updated, state.playerId, "shifts", 1.0, now)
        val award = PlayerProgression.award(
            state.progress,
            20.0 * (1.0 + ReferenceCompanyEngine.globalMultiplier(state, EmptyContext.context, "xpPct"))
        )
        val next = state.copy(
            corporation = updated,
            lastCorporationShiftAt = now,
            progress = award.progress
        )
        return success(record(next.copy(cash = next.cash + award.cashReward), now, "corp_shift", "Completed corporation shift", 0.0), "Shift completed")
    }

    fun upgradeFacility(state: LocalSimulationState, context: Context, facilityId: String, now: Long): LocalGameResult {
        val corp = state.corporation ?: return reject(state, "You are not in a corporation")
        if (corp.founderId != state.playerId) return reject(state, "Only the Founder can upgrade facilities")
        val definition = ReferenceCatalogue.corporationFacilities(context).firstOrNull { it.optString("id") == facilityId }
            ?: return reject(state, "Unknown corporation facility")
        val level = corp.facilities[facilityId] ?: 0
        if (level >= definition.optInt("max", 0)) return reject(state, "Facility is already at maximum level")
        val cost = definition.optDouble("cost", 0.0) * definition.optDouble("growth", 1.0).pow(level)
        if (corp.treasury + 1e-9 < cost) return reject(state, "Corporation treasury is too low")
        val levels = corp.facilities.toMutableMap().apply { this[facilityId] = level + 1 }
        var updated = corp.copy(
            treasury = corp.treasury - cost,
            facilities = levels,
            activityLog = log(corp.activityLog, now, "Built ${definition.optString("name")} level ${level + 1}")
        )
        updated = addCorpXp(updated, 200L, now)
        val next = state.copy(corporation = updated)
        return success(record(next, now, "corp_facility", "Upgraded ${definition.optString("name")}", -cost), "Corporation facility upgraded")
    }

    fun establishDepartment(state: LocalSimulationState, context: Context, departmentId: String, now: Long): LocalGameResult {
        val corp = state.corporation ?: return reject(state, "You are not in a corporation")
        if (corp.founderId != state.playerId) return reject(state, "Only the Founder can establish departments")
        if (departmentId in corp.departments) return reject(state, "Department already established")
        val definition = ReferenceCatalogue.corporationDepartments(context).firstOrNull { it.optString("id") == departmentId }
            ?: return reject(state, "Unknown corporation department")
        val cost = definition.optDouble("cost", 0.0)
        if (corp.treasury + 1e-9 < cost) return reject(state, "Corporation treasury is too low")
        val departments = corp.departments + (departmentId to LocalCorporationDepartment(departmentId, establishedAt = now))
        val updated = corp.copy(
            treasury = corp.treasury - cost,
            departments = departments,
            activityLog = log(corp.activityLog, now, "Established ${definition.optString("name")}")
        )
        return success(record(state.copy(corporation = updated), now, "corp_department", "Established ${definition.optString("name")}", -cost), "Department established")
    }

    fun assignOfficer(state: LocalSimulationState, departmentId: String, officerId: String?, now: Long): LocalGameResult {
        val corp = state.corporation ?: return reject(state, "You are not in a corporation")
        if (corp.founderId != state.playerId) return reject(state, "Only the Founder can assign officers")
        val department = corp.departments[departmentId] ?: return reject(state, "Establish the department first")
        if (officerId != null && corp.members.firstOrNull { it.playerId == officerId }?.rank != "Executive Officer") {
            return reject(state, "Only Executive Officers can be assigned")
        }
        val updated = corp.copy(
            departments = corp.departments + (departmentId to department.copy(officerPlayerId = officerId)),
            activityLog = log(corp.activityLog, now, "Updated officer for $departmentId")
        )
        return success(state.copy(corporation = updated), "Department officer assigned")
    }

    fun acquireBusiness(state: LocalSimulationState, context: Context, businessId: String, now: Long): LocalGameResult {
        val corp = state.corporation ?: return reject(state, "You are not in a corporation")
        if (corp.founderId != state.playerId) return reject(state, "Only the Founder can purchase corporation businesses")
        val definition = ReferenceCatalogue.corporationBusinesses(context).firstOrNull { it.optString("id") == businessId }
            ?: return reject(state, "Unknown corporation business")
        val departmentId = definition.optString("dept")
        if (departmentId !in corp.departments) return reject(state, "Establish the required department first")
        if (corp.businesses.any { it.definitionId == businessId }) return reject(state, "Corporation already owns this business")
        val cost = definition.optDouble("price", 0.0)
        if (corp.treasury + 1e-9 < cost) return reject(state, "Corporation treasury is too low")
        val instance = LocalCorporationBusiness("corp-business-${state.nextSequence}", businessId, now)
        val updated = corp.copy(
            treasury = corp.treasury - cost,
            businesses = corp.businesses + instance,
            activityLog = log(corp.activityLog, now, "Acquired ${definition.optString("name")}")
        )
        return success(record(state.copy(corporation = updated), now, "corp_business_purchase", "Acquired ${definition.optString("name")}", -cost), "Corporation business acquired")
    }

    fun setSettings(
        state: LocalSimulationState,
        recruitmentMode: String? = null,
        description: String? = null,
        now: Long
    ): LocalGameResult {
        val corp = state.corporation ?: return reject(state, "You are not in a corporation")
        if (corp.founderId != state.playerId) return reject(state, "Only the Founder can change corporation settings")
        if (recruitmentMode != null && recruitmentMode !in setOf("open", "application", "closed")) {
            return reject(state, "Invalid recruitment setting")
        }
        val updated = corp.copy(
            recruitmentMode = recruitmentMode ?: corp.recruitmentMode,
            description = description?.take(280) ?: corp.description,
            activityLog = log(corp.activityLog, now, "Corporation settings updated")
        )
        return success(state.copy(corporation = updated), "Corporation settings updated")
    }

    fun tick(state: LocalSimulationState, context: Context, now: Long): LocalSimulationState {
        var corp = state.corporation ?: return state
        if (corp.dissolved) return state.copy(corporation = null)
        val elapsedHours = ((now - corp.lastTick).coerceAtLeast(0L) / HOUR_MS).coerceAtMost(24.0)
        if (elapsedHours > 0.0) {
            var income = 0.0
            val businesses = corp.businesses.map { business ->
                val definition = ReferenceCatalogue.corporationBusinesses(context).firstOrNull {
                    it.optString("id") == business.definitionId
                } ?: return@map business
                val departmentId = definition.optString("dept")
                val department = corp.departments[departmentId]
                if (department == null) return@map business
                val officerBonus = if (department.officerPlayerId != null) 1.1 else 1.0
                val net = (definition.optDouble("revenue", 0.0) * officerBonus -
                    definition.optDouble("expense", 0.0)) * elapsedHours
                income += net
                business.copy(lifetimeNetIncome = business.lifetimeNetIncome + net)
            }
            corp = corp.copy(
                treasury = corp.treasury + income,
                businesses = businesses,
                lastTick = now,
                activityLog = if (income > 1.0) log(corp.activityLog, now, "Corporation businesses generated $${income.toLong()}") else corp.activityLog
            )
        }
        // Virtual staff complete scheduled shifts locally. This allows cooperative objectives
        // to make progress without claiming that external human players participated.
        val npcMembers = corp.members.filter { it.isNpc }
        if (npcMembers.isNotEmpty()) {
            val members = corp.members.toMutableList()
            npcMembers.forEach { npc ->
                val scheduledTotal = ((now - npc.joinedAt).coerceAtLeast(0L) / (8L * HOUR_MS)).toInt().coerceAtMost(1000)
                val due = min(3, max(0, scheduledTotal - npc.shiftsCompleted))
                if (due > 0) {
                    val index = members.indexOfFirst { it.playerId == npc.playerId }
                    if (index >= 0) members[index] = members[index].copy(shiftsCompleted = members[index].shiftsCompleted + due)
                    repeat(due) { corp = progressObjectives(corp, npc.playerId, "shifts", 1.0, now) }
                }
            }
            corp = corp.copy(members = members)
        }
        corp = corp.copy(
            objectives = corp.objectives.filter { it.expiresAt > now },
            completedObjectives = corp.completedObjectives + corp.objectives.filter { it.expiresAt <= now }.map {
                LocalCorporationCompletedObjective(it.title, now, false)
            },
            activityLog = if (corp.objectives.any { it.expiresAt <= now }) log(corp.activityLog, now, "Expired corporation objective") else corp.activityLog
        )
        if (corp.objectives.size < objectiveSlots(corp) && now >= corp.objectiveCooldownUntil) {
            corp = ensureObjectives(corp, now)
        }
        return state.copy(corporation = corp)
    }

    fun capacity(corp: LocalCorporation): Int = 10 + (corp.facilities["f_hq"] ?: 0) * 5

    fun dailyLimit(corp: LocalCorporation): Double =
        (10_000.0 * 1.6.pow(corp.level - 1)).roundToLong().toDouble()

    fun facilityBonus(corp: LocalCorporation, context: Context, key: String): Double {
        val total = ReferenceCatalogue.corporationFacilities(context)
            .filter { it.optString("bonus") == key && it.optString("id") != "f_hq" }
            .sumOf { definition -> (corp.facilities[definition.optString("id")] ?: 0) * definition.optDouble("perLevel", 0.0) }
        return min(0.1, total)
    }

    fun objectiveSlots(corp: LocalCorporation): Int = 1 + floor(corp.level / 2.0).toInt()

    private fun ensureObjectives(corpInput: LocalCorporation, now: Long): LocalCorporation {
        var corp = corpInput
        while (corp.objectives.size < objectiveSlots(corp)) {
            corp = corp.copy(objectives = corp.objectives + generateObjective(corp, now))
        }
        return corp
    }

    private fun generateObjective(corpInput: LocalCorporation, now: Long): LocalCorporationObjective {
        val corp = corpInput
        val sequence = corp.objectiveSequence + 1
        val base = (now xor corp.id.hashCode().toLong() xor (sequence * 1_103_515_245L))
        if (corp.milestoneIndex < 3 && corp.objectives.none { it.milestone }) {
            val milestone = when (corp.milestoneIndex) {
                0 -> LocalCorporationObjective(
                    id = "obj$sequence", title = "Founding Capital", type = "funds", target = 20_000.0,
                    expiresAt = now + 24L * HOUR_MS, minParticipants = 1,
                    corporationXpReward = 400, treasuryReward = 0.0, memberXpReward = 150.0, milestone = true
                )
                1 -> LocalCorporationObjective(
                    id = "obj$sequence", title = "Build the First Office", type = "shifts", target = 6.0,
                    expiresAt = now + 24L * HOUR_MS, minParticipants = min(2, corp.members.size.coerceAtLeast(1)),
                    corporationXpReward = 500, treasuryReward = 5_000.0, memberXpReward = 200.0, milestone = true
                )
                else -> LocalCorporationObjective(
                    id = "obj$sequence", title = "Expansion Fund", type = "funds", target = 150_000.0,
                    expiresAt = now + 24L * HOUR_MS, minParticipants = min(3, corp.members.size.coerceAtLeast(1)),
                    corporationXpReward = 1_200, treasuryReward = 0.0, memberXpReward = 400.0, milestone = true
                )
            }
            return sequence.copyObjectiveResult(milestone, milestoneIndex = corp.milestoneIndex + 1)
        }
        val random = Random(base)
        val kinds = corp.objectives.map { it.type }.toSet()
        val kind = when {
            "funds" in kinds && "shifts" !in kinds -> "shifts"
            "shifts" in kinds && "funds" !in kinds -> "funds"
            else -> if (random.nextBoolean()) "funds" else "shifts"
        }
        val memberCount = corp.members.size.coerceAtLeast(1)
        val minParticipants = min(memberCount, max(1, floor(corp.level / 2.0).toInt() + if (random.nextBoolean()) 1 else 0))
        val difficulty = 1.0 + random.nextDouble() * 2.0
        return if (kind == "funds") {
            LocalCorporationObjective(
                id = "obj$sequence",
                title = listOf("Capital Drive", "Treasury Boost", "Investment Round")[random.nextInt(3)],
                type = "funds",
                target = (dailyLimit(corp) * max(1, minParticipants) * 0.5 * difficulty).roundToLong().toDouble(),
                expiresAt = now + 24L * HOUR_MS,
                minParticipants = minParticipants,
                corporationXpReward = (300.0 * difficulty * corp.level).roundToLong(),
                treasuryReward = 0.0,
                memberXpReward = 100.0 * difficulty * corp.level
            )
        } else {
            LocalCorporationObjective(
                id = "obj$sequence",
                title = listOf("Construction Sprint", "Production Push", "Logistics Drill")[random.nextInt(3)],
                type = "shifts",
                target = ((3 + corp.level) * difficulty).roundToLong().toDouble(),
                expiresAt = now + 24L * HOUR_MS,
                minParticipants = minParticipants,
                corporationXpReward = (350.0 * difficulty * corp.level).roundToLong(),
                treasuryReward = (2_000.0 * difficulty * corp.level).roundToLong().toDouble(),
                memberXpReward = 120.0 * difficulty * corp.level
            )
        }
    }

    private fun LocalCorporation.copyObjectiveResult(
        objective: LocalCorporationObjective,
        milestoneIndex: Int
    ): LocalCorporation = copy(
        objectives = objectives + objective,
        objectiveSequence = objectiveSequence + 1,
        milestoneIndex = milestoneIndex
    )

    private fun progressObjectives(
        corpInput: LocalCorporation,
        memberId: String,
        kind: String,
        amount: Double,
        now: Long
    ): LocalCorporation {
        var corp = corpInput
        val remaining = mutableListOf<LocalCorporationObjective>()
        corp.objectives.forEach { objective ->
            if (objective.type != kind) {
                remaining += objective
            } else {
                val participants = objective.participants + memberId
                val progress = min(objective.target, objective.progress + amount)
                if (progress >= objective.target && participants.size >= objective.minParticipants) {
                    corp = addCorpXp(corp, objective.corporationXpReward, now)
                    val pending = corp.pendingMemberXp.toMutableMap()
                    participants.forEach { id -> pending[id] = (pending[id] ?: 0L) + objective.memberXpReward.toLong() }
                    corp = corp.copy(
                        treasury = corp.treasury + objective.treasuryReward,
                        pendingMemberXp = pending,
                        completedObjectives = (corp.completedObjectives + LocalCorporationCompletedObjective(objective.title, now, true)).takeLast(50),
                        objectiveCooldownUntil = now + 60L * 60_000L,
                        activityLog = log(corp.activityLog, now, "Completed objective: ${objective.title}")
                    )
                } else {
                    remaining += objective.copy(progress = progress, participants = participants)
                }
            }
        }
        return corp.copy(objectives = remaining)
    }

    private fun addCorpXp(corpInput: LocalCorporation, rawXp: Long, now: Long): LocalCorporation {
        var corp = corpInput.copy(experience = corpInput.experience + rawXp)
        var level = corp.level
        var xp = corp.experience
        while (xp >= (1_000.0 * 1.5.pow(level - 1)).roundToLong() && level < 1000) {
            xp -= (1_000.0 * 1.5.pow(level - 1)).roundToLong()
            level++
            corp = corp.copy(activityLog = log(corp.activityLog, now, "Corporation reached level $level"))
        }
        return corp.copy(level = level, experience = xp)
    }

    private fun contributionXp(limit: Double, already: Double, amount: Double): Double {
        val bands = listOf(1.0, 0.8, 0.6, 0.4, 0.2)
        var total = 0.0
        var position = already
        var remaining = amount
        while (remaining > 0.0 && position < limit) {
            val band = min(bands.lastIndex, floor(position / limit * bands.size).toInt())
            val bandEnd = (band + 1.0) / bands.size * limit
            val taken = min(remaining, bandEnd - position)
            total += taken * 0.01 * bands[band]
            position += taken
            remaining -= taken
        }
        return total
    }

    private fun log(previous: List<String>, now: Long, message: String): List<String> =
        (previous + "[$now] $message").takeLast(100)

    private fun awardXp(state: LocalSimulationState, baseXp: Double, context: Context): LocalSimulationState {
        val multiplier = ReferenceCompanyEngine.globalMultiplier(state, context, "xpPct")
        val award = PlayerProgression.award(state.progress, baseXp * multiplier)
        return state.copy(progress = award.progress, cash = state.cash + award.cashReward)
    }

    private fun record(
        state: LocalSimulationState,
        now: Long,
        category: String,
        description: String,
        amount: Double
    ): LocalSimulationState {
        val entry = LocalLedgerEntry(
            id = "local-${state.nextSequence}",
            timestamp = now,
            category = category,
            description = description,
            amount = amount,
            balanceAfter = state.cash
        )
        return state.copy(ledger = (state.ledger + entry).takeLast(5000), nextSequence = state.nextSequence + 1)
    }

    private fun success(state: LocalSimulationState, message: String) = LocalGameResult.Success(state, message)
    private fun reject(state: LocalSimulationState, message: String) = LocalGameResult.Rejected(state, message)
}
