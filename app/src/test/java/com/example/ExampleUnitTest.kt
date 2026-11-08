package com.example

import com.example.data.DefaultData
import com.example.model.*
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun testInitialGameData() {
    val clickUpgrades = DefaultData.initialClickUpgrades()
    val businesses = DefaultData.initialBusinesses()
    val properties = DefaultData.initialProperties()
    val stocks = DefaultData.initialStocks()
    val cryptos = DefaultData.initialCryptos()
    val collectibles = DefaultData.initialCollectibles()
    val research = DefaultData.initialResearch()
    val achievements = DefaultData.initialAchievements()

    assertEquals(6, clickUpgrades.size)
    assertEquals(20, businesses.size)
    assertEquals(10, properties.size)
    assertEquals(6, stocks.size)
    assertEquals(5, cryptos.size)
    assertEquals(16, collectibles.size)
    assertEquals(12, research.size)
    assertEquals(25, achievements.size)
  }

  @Test
  fun testBusinessProfitCalculations() {
    val coffeeKiosk = DefaultData.initialBusinesses().first()
    assertEquals(0.0, coffeeKiosk.netProfitPerSec, 0.001)

    val owned = coffeeKiosk.copy(
      isOwned = true,
      currentBranches = 2,
      employeeCount = 1
    )
    val multiplier = owned.calculateMultiplier() // 1.0 * (1 + 0.1) = 1.1
    assertEquals(1.1, multiplier, 0.001)

    val gross = owned.grossRevenuePerSec // 4.0 * 2 * 1.1 = 8.8
    assertEquals(8.8, gross, 0.001)

    val expenses = owned.expensesPerSec // 1 * 1.0 = 1.0
    assertEquals(1.0, expenses, 0.001)

    val net = owned.netProfitPerSec // 8.8 - 1.0 = 7.8
    assertEquals(7.8, net, 0.001)
  }

  @Test
  fun testResearchMultipliersOnBusiness() {
    val biz = DefaultData.initialBusinesses().find { it.id == "biz_trans_1" }!!
    val owned = biz.copy(isOwned = true, currentBranches = 1, employeeCount = 2)

    // With 50% hire cost discount
    val normalCost = owned.employeeHireCost
    val discountedCost = owned.calculateHireCost(0.50)
    assertEquals(normalCost * 0.50, discountedCost, 0.001)

    // With 20% expense reduction (Management Science)
    val normalExpenses = owned.expensesPerSec
    val reducedExpenses = owned.calculateExpenses(0.80)
    assertEquals(normalExpenses * 0.80, reducedExpenses, 0.001)

    // With Electric Mobility (+60% bonus on transport)
    val boostedProfit = owned.calculateNetProfit(expenseReduction = 0.80, employeeEfficiency = 0.20, industryBonus = 1.60)
    assertTrue(boostedProfit > owned.netProfitPerSec)
  }

  @Test
  fun testPropertyRentCalculations() {
    val duplex = DefaultData.initialProperties().first()
    assertEquals(0.0, duplex.rentPerSec, 0.001)

    val owned = duplex.copy(
      isOwned = true,
      conditionPercent = 100
    )
    assertEquals(12.0, owned.rentPerSec, 0.001)

    val degraded = owned.copy(conditionPercent = 50)
    assertEquals(6.0, degraded.rentPerSec, 0.001)
  }

  @Test
  fun testStockPnlCalculations() {
    val stock = Stock(
      symbol = "APEX",
      name = "Apex Technologies",
      sector = "Tech",
      currentPrice = 150.0,
      previousPrice = 140.0,
      sharesOwned = 10,
      totalCostBasis = 1400.0
    )
    assertEquals(1500.0, stock.holdingsValue, 0.001)
    assertEquals(100.0, stock.unrealizedPnl, 0.001)
    assertEquals(140.0, stock.avgCostPerShare, 0.001)
  }

  @Test
  fun testPlayingCardModel() {
    val cardRed = PlayingCard("K", "♥", 10)
    val cardBlack = PlayingCard("A", "♠", 1)

    assertTrue(cardRed.isRed)
    assertFalse(cardBlack.isRed)
    assertEquals("K♥", cardRed.display)
    assertEquals("A♠", cardBlack.display)
  }

  @Test
  fun testTotalNetWorthAggregation() {
    val state = GameState(
      cash = 5000.0,
      casinoChips = 20, // 20 * 100 = 2000
      businesses = listOf(
        DefaultData.initialBusinesses().first().copy(isOwned = true, currentBranches = 1, accumulatedWallet = 300.0)
      )
    )
    assertTrue(state.totalNetWorth > 7000.0)
  }
}
