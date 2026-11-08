package com.example.model

data class LocalPlayerProfile(
    val username: String = "GameDev",
    val title: String = "Aspiring Tycoon",
    val bio: String = "",
    val avatarId: String = "founder",
    val themeId: String = "violet",
    val createdAt: Long = System.currentTimeMillis()
)

data class LocalPlayerProgress(
    val level: Int = 1,
    val xp: Double = 0.0,
    val totalClicks: Long = 0L,
    val lifetimeEarned: Double = 50.0,
    val clickTokens: Double = 150.0,
    val tokenUpdatedAt: Long = System.currentTimeMillis(),
    val prestigeCount: Int = 0
)

data class LocalMarketOrder(
    val id: String,
    val market: String,
    val symbol: String,
    val side: String,
    val kind: String,
    val quantity: Double,
    val filledQuantity: Double = 0.0,
    val limitPrice: Double = 0.0,
    val triggerPrice: Double = 0.0,
    val status: String = "OPEN",
    val createdAt: Long,
    val updatedAt: Long,
    val expiresAt: Long,
    val reservedCash: Double = 0.0,
    val reservedQuantity: Double = 0.0,
    val averageFillPrice: Double = 0.0,
    val feesPaid: Double = 0.0
)

data class LocalResearchProject(
    val researchId: String,
    val startedAt: Long,
    val completesAt: Long,
    val cashInvested: Double,
    val pointsInvested: Double,
    val slot: Int
)

data class LocalCorpMember(
    val id: String,
    val name: String,
    val rank: String,
    val role: String,
    val joinedAt: Long,
    val isSimulatedOperator: Boolean = false,
    val totalContribution: Double = 0.0,
    val shiftsCompleted: Int = 0
)

data class LocalCorpObjective(
    val id: String,
    val name: String,
    val description: String,
    val target: Double,
    val progress: Double = 0.0,
    val rewardXp: Double,
    val rewardTreasury: Double,
    val expiresAt: Long,
    val isClaimed: Boolean = false
)

data class LocalCorpBusiness(
    val id: String,
    val name: String,
    val departmentId: String,
    val acquisitionCost: Double,
    val revenuePerHour: Double,
    val expensesPerHour: Double,
    val accumulatedRevenue: Double = 0.0
) {
    val netPerHour: Double get() = revenuePerHour - expensesPerHour
}

data class LocalCorporation(
    val id: String = "",
    val name: String = "",
    val tag: String = "",
    val level: Int = 1,
    val xp: Double = 0.0,
    val treasury: Double = 0.0,
    val rank: String = "Founder",
    val foundedAt: Long = 0L,
    val lastShiftAt: Long = 0L,
    val lastContributionDay: Long = 0L,
    val contributedToday: Double = 0.0,
    val members: List<LocalCorpMember> = emptyList(),
    val facilities: Map<String, Int> = emptyMap(),
    val departments: List<String> = emptyList(),
    val businesses: List<LocalCorpBusiness> = emptyList(),
    val objectives: List<LocalCorpObjective> = emptyList(),
    val completedObjectives: Int = 0,
    val activityLog: List<String> = emptyList()
) {
    val isFounded: Boolean get() = id.isNotBlank()
}

data class LocalLedgerEntry(
    val id: String,
    val timestamp: Long,
    val category: String,
    val description: String,
    val debit: Double = 0.0,
    val credit: Double = 0.0,
    val balanceAfter: Double = 0.0
)

data class LocalNotification(
    val id: String,
    val timestamp: Long,
    val category: String,
    val title: String,
    val message: String,
    val isRead: Boolean = false
)

data class LocalGamePreferences(
    val hapticsEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = false,
    val reducedMotion: Boolean = false,
    val compactNumbers: Boolean = true,
    val languageCode: String = "en"
)