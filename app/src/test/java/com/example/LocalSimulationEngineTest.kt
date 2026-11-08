package com.example

import com.example.engine.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalSimulationEngineTest {

    @Test
    fun playerProgressionUsesReferenceXpCurveAndOneTimeRewards() {
        assertEquals(200L, PlayerProgression.xpForLevel(1))
        assertEquals(350L, PlayerProgression.xpForLevel(2))
        assertEquals(61_450L, PlayerProgression.xpForLevel(49))

        val first = PlayerProgression.award(LocalPlayerProgress(), 199.0)
        assertEquals(1, first.progress.level)
        assertEquals(199.0, first.progress.xp, 0.0)
        assertEquals(0.0, first.cashReward, 0.0)

        val levelUp = PlayerProgression.award(first.progress, 1.0)
        assertEquals(2, levelUp.progress.level)
        assertEquals(0.0, levelUp.progress.xp, 0.0)
        assertEquals(25.0, levelUp.cashReward, 0.0)

        val levelFive = PlayerProgression.award(
            LocalPlayerProgress(xp = 200.0 + 350.0 + 750.0 + 1_000.0),
            0.0
        )
        assertEquals(5, levelFive.progress.level)
        assertEquals(2, levelFive.progress.clickCapBonus)
    }

    @Test
    fun localCompanyReportsOperatingLossInsteadOfMaskingIt() {
        val company = LocalCompanyInstance(
            id = "loss-maker",
            definitionId = "loss-maker",
            name = "Loss Maker",
            industry = "Industrial",
            purchasePrice = 1_000.0,
            branches = 1,
            hourlyBaseRevenue = 50.0,
            hourlyFixedExpense = 100.0,
            staff = listOf(LocalEmployeeRole("staff", "Worker", 2, 25.0, 0.0))
        )
        assertEquals(50.0, company.grossPerHour, 0.0)
        assertEquals(150.0, company.expensesPerHour, 0.0)
        assertEquals(-100.0, company.netPerHour, 0.0)
    }

    @Test
    fun marketPricesAreDeterministicForSameAssetAndTick() {
        val now = 1_799_000_000_000L
        val stock = ReferenceMarketEngine.listStocks(now).first()
        val stockTick = ReferenceMarketEngine.stockTick(now)
        assertEquals(
            ReferenceMarketEngine.stockPriceAtTick(stock, stockTick),
            ReferenceMarketEngine.stockPriceAtTick(stock, stockTick),
            0.0
        )

        val crypto = ReferenceMarketEngine.listCrypto(now).first()
        val cryptoTick = ReferenceMarketEngine.cryptoTick(now)
        assertEquals(
            ReferenceMarketEngine.cryptoPriceAtTick(crypto, cryptoTick),
            ReferenceMarketEngine.cryptoPriceAtTick(crypto, cryptoTick),
            0.0
        )
    }

    @Test
    fun casinoSettlementMathMatchesReference() {
        val safeOneWithThreeMines = ReferenceCasinoEngine.minesMultiplier(3, 1)
        assertEquals((25.0 / 22.0) * 0.97, safeOneWithThreeMines, 1e-10)

        val softNineteen = ReferenceCasinoEngine.handValue(
            listOf(LocalCasinoCard(1, 0), LocalCasinoCard(8, 1))
        )
        assertEquals(19, softNineteen.first)
        assertTrue(softNineteen.second)

        val hardNineteen = ReferenceCasinoEngine.handValue(
            listOf(LocalCasinoCard(10, 0), LocalCasinoCard(9, 1))
        )
        assertEquals(19, hardNineteen.first)
        assertFalse(hardNineteen.second)
    }

    @Test
    fun timedBusinessCapacityUpgradeConsumesCashAndAddsSlotOnlyWhenDue() {
        val now = 1_800_000_000_000L
        val start = LocalSimulationState(cash = 100_000.0, lastSimulationAt = now)
        val result = LocalGameEngine.upgradeBusinessCapacity(start, now)
        assertTrue(result is LocalGameResult.Success)
        val upgraded = (result as LocalGameResult.Success).state
        assertEquals(3, upgraded.businessCapacitySlots)
        assertEquals(50_000.0, upgraded.cash, 0.0)
        assertTrue(upgraded.capacityUpgrade != null)

        val finished = LocalGameEngine.tick(
            upgraded,
            upgraded.capacityUpgrade!!.completesAt + 1L,
            offline = false
        ).first
        assertEquals(4, finished.businessCapacitySlots)
        assertEquals(1, finished.capacityLevel)
        assertTrue(finished.capacityUpgrade == null)
    }
}
