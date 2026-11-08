package com.example.engine

import com.example.data.DefaultData
import com.example.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalSimulationEngineTest {
    @Test
    fun suspendedBusinessesStopRevenueAndRetainOnlyIdleCosts() {
        val base = DefaultData.initialBusinesses().first()
        val suspended = base.copy(
            isOwned = true,
            isSuspended = true,
            currentBranches = 1,
            employeeCount = 2,
            operatingExpensePerSec = 100.0
        )

        val report = LocalSimulationEngine.businessReport(suspended, 1_000_000L)

        assertEquals(0.0, report.gross, 0.0)
        assertEquals(suspended.expensesPerSec, report.expenses, 1e-9)
        assertEquals(-suspended.expensesPerSec, report.net, 1e-9)
        assertEquals("Suspended", report.status)
    }

    @Test
    fun suspendedBusinessDoesNotGenerateResearchPoints() {
        val now = 3_600_000L
        val suspended = DefaultData.initialBusinesses().first().copy(
            isOwned = true,
            isSuspended = true,
            currentBranches = 1,
            employeeCount = 2,
            operatingExpensePerSec = 100.0,
            offlineCapHours = 0.0
        )
        val initial = GameState(
            businesses = listOf(suspended),
            lastActiveTimestamp = 0L,
            lastFinancialSnapshotAt = 0L
        )

        val next = LocalSimulationEngine.advance(initial, now)

        assertEquals(0.0, next.researchPoints, 0.0)
    }

    @Test
    fun cryptoStakingUsesAnnualizedYieldInsteadOfPerTickApRate() {
        val now = 30_000L
        val initialCrypto = Crypto(
            symbol = "TEST",
            name = "Test Token",
            currentPrice = 100.0,
            previousPrice = 100.0,
            basePrice = 100.0,
            coinsOwned = 100.0,
            volatility = 0.0
        )
        val stakingResearch = ResearchNode(
            id = "res_11",
            name = "Crypto Staking",
            category = "Finance",
            cost = 0.0,
            effectDescription = "Enable crypto staking.",
            isUnlocked = true
        )
        val initial = GameState(
            cash = 10.0,
            cryptos = listOf(initialCrypto),
            research = listOf(stakingResearch),
            lastActiveTimestamp = 0L,
            lastFinancialSnapshotAt = 0L
        )

        val next = LocalSimulationEngine.advance(initial, now)
        val credited = next.cash - initial.cash
        val expected = next.cryptos.sumOf { it.holdingsValue } * 0.05 / (365.0 * 24.0 * 60.0 * 2.0)

        assertTrue("One 30-second staking interval must produce a positive yield.", credited > 0.0)
        assertTrue("A single staking interval must not produce a windfall.", credited < 0.01)
        assertEquals(expected, credited, 1e-9)
        assertTrue(next.transactions.any { it.description.contains("staking yield", ignoreCase = true) })
    }

    @Test
    fun passiveIncomeLedgerTracksTheBalanceAfterEachCredit() {
        val stock = Stock(
            symbol = "TEST",
            name = "Dividend Holdings",
            sector = "Test",
            currentPrice = 100.0,
            previousPrice = 100.0,
            basePrice = 100.0,
            volatility = 0.0,
            sharesOwned = 10L,
            dividendYieldPercent = 5.0
        )
        val crypto = Crypto(
            symbol = "YIELD",
            name = "Staking Token",
            currentPrice = 100.0,
            previousPrice = 100.0,
            basePrice = 100.0,
            coinsOwned = 100.0,
            volatility = 0.0
        )
        val stakingResearch = ResearchNode(
            id = "res_11",
            name = "Crypto Staking",
            category = "Finance",
            cost = 0.0,
            effectDescription = "Enable crypto staking.",
            isUnlocked = true
        )
        val initial = GameState(
            cash = 100.0,
            stocks = listOf(stock),
            cryptos = listOf(crypto),
            research = listOf(stakingResearch),
            lastActiveTimestamp = 0L,
            lastFinancialSnapshotAt = 0L
        )

        val next = LocalSimulationEngine.advance(initial, 86_400_000L)
        val dividend = next.ledger.first { it.description.contains("dividends", ignoreCase = true) }
        val staking = next.ledger.first { it.description.contains("staking yield", ignoreCase = true) }

        assertEquals(initial.cash + dividend.credit, dividend.balanceAfter, 1e-8)
        assertEquals(dividend.balanceAfter + staking.credit, staking.balanceAfter, 1e-8)
        assertEquals(next.cash, staking.balanceAfter, 1e-8)
    }

    @Test
    fun dividendsAreRecordedWhenAFullDayElapsed() {
        val now = 86_400_000L
        val stock = Stock(
            symbol = "TEST",
            name = "Test Holdings",
            sector = "Test",
            currentPrice = 100.0,
            previousPrice = 100.0,
            basePrice = 100.0,
            volatility = 0.0,
            sharesOwned = 10L,
            dividendYieldPercent = 5.0
        )
        val initial = GameState(
            cash = 100.0,
            stocks = listOf(stock),
            lastActiveTimestamp = 0L,
            lastFinancialSnapshotAt = 0L
        )

        val next = LocalSimulationEngine.advance(initial, now)
        val credited = next.cash - initial.cash
        val expected = next.stocks.sumOf { it.holdingsValue } * 0.05 / 365.0

        assertEquals(expected, credited, 1e-8)
        assertTrue(next.transactions.any { it.description.contains("dividends", ignoreCase = true) })
    }
}