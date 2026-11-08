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
    val isPurchased: Boolean = false
)

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
    val isOwned: Boolean = false
) {
    val branchCost: Double
        get() = baseCost * Math.pow(1.15, currentBranches.toDouble())

    val employeeHireCost: Double
        get() = 50.0 * Math.pow(1.10, employeeCount.toDouble())

    fun calculateHireCost(discount: Double = 1.0): Double = employeeHireCost * discount

    fun calculateMultiplier(employeeEfficiency: Double = 0.10, industryBonus: Double = 1.0): Double {
        var m = 1.0
        upgrades.forEach { if (it.isPurchased) m *= it.multiplier }
        m *= (1.0 + (employeeCount * employeeEfficiency))
        m *= industryBonus
        return m
    }

    val grossRevenuePerSec: Double
        get() = if (!isOwned || currentBranches == 0) 0.0 else (baseRevenuePerSec * currentBranches * calculateMultiplier())

    fun calculateGrossRevenue(employeeEfficiency: Double = 0.10, industryBonus: Double = 1.0): Double {
        return if (!isOwned || currentBranches == 0) 0.0 else (baseRevenuePerSec * currentBranches * calculateMultiplier(employeeEfficiency, industryBonus))
    }

    val expensesPerSec: Double
        get() = if (!isOwned || currentBranches == 0) 0.0 else (employeeCount * salaryPerEmployee)

    fun calculateExpenses(expenseReduction: Double = 1.0): Double {
        return if (!isOwned || currentBranches == 0) 0.0 else (employeeCount * salaryPerEmployee * expenseReduction)
    }

    val netProfitPerSec: Double
        get() = (grossRevenuePerSec - expensesPerSec).coerceAtLeast(0.0)

    fun calculateNetProfit(expenseReduction: Double = 1.0, employeeEfficiency: Double = 0.10, industryBonus: Double = 1.0): Double {
        val gross = calculateGrossRevenue(employeeEfficiency, industryBonus)
        val exp = calculateExpenses(expenseReduction)
        return (gross - exp).coerceAtLeast(0.0)
    }

    val valuation: Double
        get() = if (!isOwned) 0.0 else (baseCost * currentBranches * 0.85 + (upgrades.count { it.isPurchased } * 500.0))
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
    val isPurchased: Boolean = false
)

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
    val isOwned: Boolean = false
) {
    fun calculateMultiplier(): Double {
        var m = 1.0
        upgrades.forEach { if (it.isPurchased) m *= it.rentBoost }
        // Condition impacts rent efficiency
        m *= (conditionPercent / 100.0).coerceIn(0.2, 1.0)
        return m
    }

    val rentPerSec: Double
        get() = if (!isOwned) 0.0 else (baseRentPerSec * calculateMultiplier())

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
    val priceHistory: List<Double> = emptyList()
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
    val priceHistory: List<Double> = emptyList()
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
    val bonusDescription: String = ""
)

data class ResearchNode(
    val id: String,
    val name: String,
    val category: String,
    val cost: Double,
    val effectDescription: String,
    val isUnlocked: Boolean = false,
    val requiredResearchId: String? = null
)

data class CasinoStats(
    val totalWonChips: Long = 0,
    val totalLostChips: Long = 0,
    val gamesPlayed: Int = 0,
    val biggestWinChips: Long = 0
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
    val lastActiveTimestamp: Long = System.currentTimeMillis()
) {
    val totalNetWorth: Double
        get() {
            var sum = cash
            sum += casinoChips * 100.0
            sum += businesses.filter { it.isOwned }.sumOf { it.valuation + it.accumulatedWallet }
            sum += properties.filter { it.isOwned }.sumOf { it.currentValuation + it.accumulatedRent }
            sum += stocks.sumOf { it.holdingsValue }
            sum += cryptos.sumOf { it.holdingsValue }
            sum += collectibles.filter { it.isOwned }.sumOf { it.currentValuation }
            return sum
        }

    val totalIncomePerSec: Double
        get() {
            val biz = businesses.filter { it.isOwned }.sumOf { it.grossRevenuePerSec }
            val prop = properties.filter { it.isOwned }.sumOf { it.rentPerSec }
            return biz + prop
        }

    val totalExpensesPerSec: Double
        get() = businesses.filter { it.isOwned }.sumOf { it.expensesPerSec }

    val netCashFlowPerSec: Double
        get() = (totalIncomePerSec - totalExpensesPerSec).coerceAtLeast(0.0)

    val availableBusinessWallet: Double
        get() = businesses.filter { it.isOwned }.sumOf { it.accumulatedWallet }

    val availableRentWallet: Double
        get() = properties.filter { it.isOwned }.sumOf { it.accumulatedRent }
}
