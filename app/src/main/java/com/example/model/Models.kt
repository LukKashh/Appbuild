package com.example.model

enum class Industry(val title: String) {
    FOOD("Food & Beverage"),
    RETAIL("Retail & Commerce"),
    HOSPITALITY("Hospitality"),
    TRANSPORT("Transportation"),
    TECH("Technology"),
    MANUFACTURING("Manufacturing"),
    BANKING("Banking & Finance"),
    ENERGY("Energy & Power"),
    ENTERTAINMENT("Entertainment"),
    INDUSTRIAL("Industrial Operations")
}

data class BusinessUpgrade(
    val id: String,
    val name: String,
    val cost: Double,
    val multiplier: Double,
    val isPurchased: Boolean = false,
    val level: Int = if (isPurchased) 1 else 0,
    val maxLevel: Int = 5,
    val costGrowth: Double = 1.65,
    val revenueBonusPerLevel: Double = 0.0,
    val expenseReductionPerLevel: Double = 0.0,
    val staffEfficiencyPerLevel: Double = 0.0
) {
    val currentCost: Double get() = cost * Math.pow(costGrowth, level.toDouble())
    val isMaxed: Boolean get() = level >= maxLevel
}

data class PlayingCard(
    val rank: String,
    val suit: String,
    val value: Int
) {
    val isRed: Boolean get() = suit == "♥" || suit == "♦"
    val display: String get() = "$rank$suit"
}

data class Business(
    val id: String,
    val name: String,
    val industry: Industry,
    val baseCost: Double,
    val baseRevenuePerSec: Double,
    val currentBranches: Int = 0,
    val employeeCount: Int = 0,
    val salaryPerEmployee: Double = 5.0,
    val accumulatedWallet: Double = 0.0,
    val upgrades: List<BusinessUpgrade> = emptyList(),
    val isOwned: Boolean = false,
    val operatingExpensePerSec: Double = 0.0,
    val employeeCapacity: Int = 100,
    val minimumEmployees: Int = 0,
    val branchLimit: Int = 10,
    val reserves: Double = 0.0,
    val liabilities: Double = 0.0,
    val totalRevenue: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val isSuspended: Boolean = false,
    val acquiredAt: Long = 0L,
    val offlineCapHours: Double = 4.0,
    val staffRoles: Map<String, Int> = emptyMap(),
    val financialHistory: List<LocalFinancialSample> = emptyList()
) {
    val branchCost: Double
        get() = baseCost * Math.pow(1.15, currentBranches.toDouble())

    val employeeHireCost: Double
        get() = 50.0 * Math.pow(1.10, employeeCount.toDouble())

    fun calculateHireCost(discount: Double = 1.0): Double = employeeHireCost * discount

    fun calculateMultiplier(employeeEfficiency: Double = 0.10, industryBonus: Double = 1.0): Double {
        var m = 1.0
        upgrades.forEach { upgrade ->
            val effectiveLevel = maxOf(upgrade.level, if (upgrade.isPurchased) 1 else 0)
            if (effectiveLevel > 0) m *= Math.pow(upgrade.multiplier, effectiveLevel.toDouble())
        }
        m *= (1.0 + (employeeCount * employeeEfficiency))
        m *= industryBonus
        return m
    }

    val grossRevenuePerSec: Double
        get() = if (!isOwned || currentBranches == 0 || isSuspended) 0.0 else (baseRevenuePerSec * currentBranches * calculateMultiplier()) * (if (minimumEmployees <= 0) 1.0 else (employeeCount.toDouble() / minimumEmployees).coerceIn(0.0, 1.0))

    fun calculateGrossRevenue(employeeEfficiency: Double = 0.10, industryBonus: Double = 1.0): Double {
        return if (!isOwned || currentBranches == 0) 0.0 else (baseRevenuePerSec * currentBranches * calculateMultiplier(employeeEfficiency, industryBonus))
    }

    val expensesPerSec: Double
        get() = if (!isOwned || currentBranches == 0) 0.0 else operatingExpensePerSec + employeeCount * salaryPerEmployee

    fun calculateExpenses(expenseReduction: Double = 1.0): Double {
        return if (!isOwned || currentBranches == 0) 0.0 else (employeeCount * salaryPerEmployee * expenseReduction)
    }

    val netProfitPerSec: Double
        get() = grossRevenuePerSec - expensesPerSec

    fun calculateNetProfit(expenseReduction: Double = 1.0, employeeEfficiency: Double = 0.10, industryBonus: Double = 1.0): Double {
        val gross = calculateGrossRevenue(employeeEfficiency, industryBonus)
        val exp = calculateExpenses(expenseReduction)
        return gross - exp
    }

    val valuation: Double
        get() = if (!isOwned) 0.0 else (baseCost * currentBranches * 0.85 + upgrades.sumOf { it.cost * maxOf(it.level, if (it.isPurchased) 1 else 0) * 0.5 } + reserves - liabilities).coerceAtLeast(0.0)
}

enum class PropertyCategory(val title: String) {
    RESIDENTIAL("Residential"),
    COMMERCIAL("Commercial"),
    INDUSTRIAL("Industrial"),
    HOSPITALITY("Hospitality"),
    LUXURY("Luxury")
}

data class PropertyUpgrade(
    val id: String,
    val name: String,
    val cost: Double,
    val rentBoost: Double,
    val isPurchased: Boolean = false,
    val level: Int = if (isPurchased) 1 else 0,
    val maxLevel: Int = 3,
    val costGrowth: Double = 1.6,
    val occupancyBonus: Double = 0.0,
    val expenseReduction: Double = 0.0
) {
    val currentCost: Double get() = cost * Math.pow(costGrowth, level.toDouble())
}

data class Property(
    val id: String,
    val name: String,
    val category: PropertyCategory,
    val purchasePrice: Double,
    val currentValuation: Double,
    val baseRentPerSec: Double,
    val conditionPercent: Int = 100, // 0 - 100
    val accumulatedRent: Double = 0.0,
    val upgrades: List<PropertyUpgrade> = emptyList(),
    val isOwned: Boolean = false,
    val location: String = "Unassigned",
    val operatingExpensePerSec: Double = 0.0,
    val occupancyRate: Double = 0.75,
    val demandMultiplier: Double = 1.0,
    val maintenanceDebt: Double = 0.0,
    val preciseCondition: Double = conditionPercent.toDouble(),
    val acquiredAt: Long = 0L,
    val totalMaintenance: Double = 0.0
) {
    fun calculateMultiplier(): Double {
        var m = 1.0
        upgrades.forEach { upgrade ->
            val effectiveLevel = maxOf(upgrade.level, if (upgrade.isPurchased) 1 else 0)
            if (effectiveLevel > 0) m *= Math.pow(upgrade.rentBoost, effectiveLevel.toDouble())
        }
        // Condition impacts rent efficiency
        m *= (conditionPercent / 100.0).coerceIn(0.2, 1.0)
        return m
    }

    val grossRentPerSec: Double
        get() = if (!isOwned) 0.0 else baseRentPerSec * calculateMultiplier() * occupancyRate.coerceIn(0.05, 1.0) * demandMultiplier.coerceIn(0.5, 1.5)

    val rentPerSec: Double
        get() = grossRentPerSec - operatingExpensePerSec

    val renovationCost: Double
        get() = purchasePrice * 0.15 * ((100 - conditionPercent) / 100.0).coerceAtLeast(0.05)
}

data class Stock(
    val symbol: String,
    val name: String,
    val sector: String,
    val currentPrice: Double,
    val previousPrice: Double,
    val sharesOwned: Long = 0,
    val totalCostBasis: Double = 0.0,
    val dividendYieldPercent: Double = 2.5,
    val priceHistory: List<Double> = emptyList(),
    val basePrice: Double = currentPrice,
    val volatility: Double = 0.025,
    val liquidity: Double = 1000.0,
    val beta: Double = 1.0,
    val reservedShares: Long = 0L
) {
    val changePercent: Double
        get() = if (previousPrice > 0) ((currentPrice - previousPrice) / previousPrice) * 100 else 0.0

    val holdingsValue: Double
        get() = sharesOwned * currentPrice

    val avgCostPerShare: Double
        get() = if (sharesOwned > 0) totalCostBasis / sharesOwned else 0.0

    val unrealizedPnl: Double
        get() = holdingsValue - totalCostBasis
}

enum class MarketSentiment(val title: String) {
    BULLISH("Bullish"),
    BEARISH("Bearish"),
    VOLATILE("Extreme Volatility")
}

data class Crypto(
    val symbol: String,
    val name: String,
    val currentPrice: Double,
    val previousPrice: Double,
    val coinsOwned: Double = 0.0,
    val totalCostBasis: Double = 0.0,
    val volatility: Double = 0.08,
    val sentiment: MarketSentiment = MarketSentiment.BULLISH,
    val priceHistory: List<Double> = emptyList(),
    val basePrice: Double = currentPrice,
    val maxSupply: Double = 0.0,
    val circulatingSupply: Double = 0.0,
    val liquidity: Double = 10000.0,
    val networkScore: Double = 50.0,
    val projectType: String = "Utility",
    val reservedCoins: Double = 0.0
) {
    val changePercent: Double
        get() = if (previousPrice > 0) ((currentPrice - previousPrice) / previousPrice) * 100 else 0.0

    val holdingsValue: Double
        get() = coinsOwned * currentPrice

    val avgCostPerCoin: Double
        get() = if (coinsOwned > 0) totalCostBasis / coinsOwned else 0.0

    val unrealizedPnl: Double
        get() = holdingsValue - totalCostBasis
}

enum class Rarity(val title: String) {
    COMMON("Common"),
    UNCOMMON("Uncommon"),
    RARE("Rare"),
    EPIC("Epic"),
    LEGENDARY("Legendary"),
    MYTHICAL("Mythical")
}

data class Collectible(
    val id: String,
    val name: String,
    val category: String,
    val rarity: Rarity,
    val purchasePrice: Double,
    val currentValuation: Double,
    val appreciationRatePerSec: Double = 0.0001,
    val isOwned: Boolean = false,
    val bonusDescription: String = "",
    val subCategory: String = "",
    val limitedEdition: Boolean = false,
    val releaseAt: Long = 0L,
    val holdUntil: Long = 0L,
    val resalePercent: Double = 0.75,
    val volatility: Double = 0.10,
    val bonusKey: String = "",
    val bonusValue: Double = 0.0
)

data class ResearchNode(
    val id: String,
    val name: String,
    val category: String,
    val cost: Double,
    val effectDescription: String,
    val isUnlocked: Boolean = false,
    val requiredResearchId: String? = null,
    val durationHours: Double = 1.0,
    val researchPointsCost: Double = 10.0,
    val minimumPlayerLevel: Int = 1,
    val effects: Map<String, Double> = emptyMap()
)

data class CasinoStats(
    val totalWonChips: Long = 0,
    val totalLostChips: Long = 0,
    val gamesPlayed: Int = 0,
    val biggestWinChips: Long = 0,
    val totalWageredChips: Long = 0,
    val currentWinStreak: Int = 0,
    val bestWinStreak: Int = 0,
    val currentLossStreak: Int = 0,
    val gameCounts: Map<String, Int> = emptyMap()
)

enum class TransactionType {
    BUSINESS,
    PROPERTY,
    STOCKS,
    CRYPTO,
    COLLECTIBLE,
    RESEARCH,
    CASINO,
    CLICKER,
    DIVIDEND,
    OFFLINE
}

data class Transaction(
    val id: String,
    val timestamp: Long,
    val type: TransactionType,
    val description: String,
    val amount: Double
)

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val currentProgress: Double,
    val targetProgress: Double,
    val isUnlocked: Boolean = false,
    val isClaimed: Boolean = false,
    val rewardCash: Double = 1000.0
)

data class ClickUpgrade(
    val id: String,
    val name: String,
    val description: String,
    val baseCost: Double,
    val level: Int = 0,
    val powerBoost: Double = 1.0
) {
    val currentCost: Double
        get() = baseCost * Math.pow(1.35, level.toDouble())
}

data class GameState(
    val cash: Double = 50.0,
    val casinoChips: Long = 0,
    val clickLevel: Int = 1,
    val clickPower: Double = 1.0,
    val clickMultiplier: Double = 1.0,
    val clickUpgrades: List<ClickUpgrade> = emptyList(),
    val businesses: List<Business> = emptyList(),
    val properties: List<Property> = emptyList(),
    val stocks: List<Stock> = emptyList(),
    val cryptos: List<Crypto> = emptyList(),
    val collectibles: List<Collectible> = emptyList(),
    val research: List<ResearchNode> = emptyList(),
    val casinoStats: CasinoStats = CasinoStats(),
    val achievements: List<Achievement> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val netWorthHistory: List<Double> = listOf(50.0),
    val lastActiveTimestamp: Long = System.currentTimeMillis(),
    val profile: LocalPlayerProfile = LocalPlayerProfile(),
    val progression: LocalPlayerProgress = LocalPlayerProgress(),
    val marketOrders: List<LocalMarketOrder> = emptyList(),
    val reservedCash: Double = 0.0,
    val researchPoints: Double = 0.0,
    val researchPointsEarned: Double = 0.0,
    val researchLabBuilt: Boolean = false,
    val researchSlots: Int = 1,
    val activeResearch: List<LocalResearchProject> = emptyList(),
    val corporation: LocalCorporation = LocalCorporation(),
    val ledger: List<LocalLedgerEntry> = emptyList(),
    val notifications: List<LocalNotification> = emptyList(),
    val preferences: LocalGamePreferences = LocalGamePreferences(),
    val lastFinancialSnapshotAt: Long = System.currentTimeMillis()
) {
    val totalNetWorth: Double
        get() {
            var sum = cash + reservedCash
            sum += casinoChips * 100.0
            sum += businesses.filter { it.isOwned }.sumOf { it.valuation + it.accumulatedWallet }
            sum += properties.filter { it.isOwned }.sumOf { it.currentValuation + it.accumulatedRent - it.maintenanceDebt }
            sum += stocks.sumOf { it.holdingsValue }
            sum += cryptos.sumOf { it.holdingsValue }
            sum += collectibles.filter { it.isOwned }.sumOf { it.currentValuation }
            return sum
        }

    val totalIncomePerSec: Double
        get() {
            val biz = businesses.filter { it.isOwned }.sumOf { it.grossRevenuePerSec }
            val prop = properties.filter { it.isOwned }.sumOf { it.grossRentPerSec }
            return biz + prop
        }

    val totalExpensesPerSec: Double
        get() = businesses.filter { it.isOwned }.sumOf { it.expensesPerSec } + properties.filter { it.isOwned }.sumOf { it.operatingExpensePerSec }

    val netCashFlowPerSec: Double
        get() = totalIncomePerSec - totalExpensesPerSec

    val availableBusinessWallet: Double
        get() = businesses.filter { it.isOwned }.sumOf { it.accumulatedWallet }

    val availableRentWallet: Double
        get() = properties.filter { it.isOwned }.sumOf { it.accumulatedRent }
}
