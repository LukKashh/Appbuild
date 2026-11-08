package com.example.engine

import kotlin.math.pow
import kotlin.math.roundToLong

data class ProgressionAward(
    val progress: LocalPlayerProgress,
    val cashReward: Double,
    val levelsGained: Int
)

/** Port of the webapp's explicit XP curve and one-time level rewards. */
object PlayerProgression {
    private val xpTable = longArrayOf(
        200,350,750,1000,1250,1500,1750,2000,2500,2750,3000,3350,3700,4000,4500,5000,5750,
        6450,7000,7700,8400,9100,9800,10500,11200,12200,13200,14200,15200,16200,17700,19200,
        20700,22200,23700,25900,28100,30300,32500,34700,37450,40200,42950,45700,48450,51700,
        54950,58200,61450,64700,68700,72700,76700,80700,84700,90200,95700,101200,106700,
        112200,118950,125700,132450,139200,145950,153450,160950,168450,175950,183450,192200,
        200950,209700,218450,227200,237450,247700,257950,268200,278450,290950,303450,315950,
        328450,340950,358450,375950,393450,410950,428450,450950,473450,495950,518450,540950,
        565950,590950,615950,640950
    )

    fun xpForLevel(level: Int): Long {
        if (level <= 0) return 200L
        if (level <= xpTable.size) return xpTable[level - 1]
        return xpTable.last() + (level - xpTable.size) * 25_000L
    }

    fun rewardForLevel(level: Int): Double =
        (25.0 * (level / 2.0).pow(1.7)).roundToLong().toDouble()

    fun award(current: LocalPlayerProgress, rawAmount: Double): ProgressionAward {
        val gain = rawAmount.coerceAtLeast(0.0).roundToLong()
        var xp = current.xp + gain
        var level = current.level.coerceAtLeast(1)
        var cash = 0.0
        var rewarded = current.levelRewarded.coerceAtLeast(1)
        var capBonus = current.clickCapBonus
        var gainedLevels = 0
        while (xp >= xpForLevel(level) && level < 10_000) {
            xp -= xpForLevel(level)
            level++
            gainedLevels++
        }
        while (rewarded < level) {
            rewarded++
            cash += rewardForLevel(rewarded)
            if (rewarded % 5 == 0) capBonus += 2
        }
        return ProgressionAward(
            progress = current.copy(
                xp = xp,
                level = level,
                levelRewarded = rewarded,
                clickCapBonus = capBonus
            ),
            cashReward = cash,
            levelsGained = gainedLevels
        )
    }
}
