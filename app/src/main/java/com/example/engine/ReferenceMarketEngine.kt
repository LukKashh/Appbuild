package com.example.engine

import android.content.Context
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.random.Random

data class ReferenceStockDefinition(
    val ticker: String,
    val name: String,
    val sector: String,
    val base: Double,
    val volatility: Double,
    val drift: Double,
    val shares: Double,
    val authorized: Double,
    val treasury: Double,
    val dividendYield: Double,
    val payoutRatio: Double,
    val revenue: Double,
    val margin: Double,
    val assets: Double,
    val liabilities: Double,
    val description: String,
    val listedAt: Long,
    val generated: Boolean = false
)

data class ReferenceCryptoDefinition(
    val symbol: String,
    val name: String,
    val base: Double,
    val profile: String,
    val supplyModel: String,
    val circulating: Double,
    val maximumSupply: Double?,
    val inflation: Double,
    val burn: Double,
    val listedAt: Long,
    val description: String = ""
)

data class ReferenceCryptoSupply(val circulating: Double, val burned: Double, val maximum: Double?)

/** Deterministic exchanges ported from webapp game/market.ts and engine.ts. */
object ReferenceMarketEngine {
    private const val EPOCH = 1_790_812_800_000L // 2026-10-01T00:00:00Z
    private const val DAY = 86_400_000L
    private const val STOCK_TICK_MS = 5 * 60_000L
    private const val CRYPTO_TICK_MS = 2 * 60_000L
    private const val MAX_TICKS_PER_EVAL = 600
    private const val ORDER_EXPIRY_DAYS = 7L
    private const val MIN_CRYPTO_PRICE = 0.0001
    private val sectors = listOf(
        "Technology", "Finance", "Energy", "Healthcare", "Manufacturing", "Consumer Goods",
        "Retail", "Transportation", "Real Estate", "Mining", "Entertainment", "Utilities"
    )
    private val stockPrefixes = listOf(
        "Apex","Blue","Cobalt","Delta","Ember","Falcon","Granite","Horizon","Ionic","Jade",
        "Kestrel","Lumen","Monarch","Nimbus","Onyx","Pioneer","Quartz","Redwood","Summit","Titan",
        "Unity","Vertex","Willow","Zenith"
    )
    private val stockSuffixes = listOf(
        "Systems","Group","Industries","Partners","Works","Corp","Holdings","Labs","Networks","Ventures"
    )
    private val cryptoPrefixes = listOf(
        "Astra","Byte","Chrono","Dash","Echo","Flux","Glyph","Hyper","Ion","Krypt",
        "Luna","Mint","Neo","Orbit","Pulse","Rune","Shard","Terra","Vox","Zeta"
    )
    private val cryptoSuffixes = listOf("coin","chain","swap","verse","net","fi","x","dao")

    private val curatedStocks = listOf(
        stock("NOVA","Nova Dynamics","Technology",182.0,0.025,0.12,2.1e9,0.012),
        stock("QBIT","Qubit Labs","Technology",64.0,0.04,0.2,9e8,0.0),
        stock("MRDN","Meridian Holdings","Finance",92.0,0.015,0.06,1.4e9,0.034),
        stock("CRSC","Crescent Bank","Finance",41.0,0.018,0.05,2.6e9,0.042),
        stock("HELX","Helix Energy","Energy",58.0,0.022,0.04,1.8e9,0.051),
        stock("SOLR","Solaris Renewables","Energy",27.0,0.035,0.15,1.1e9,0.008),
        stock("VITA","Vitalis Health","Healthcare",134.0,0.017,0.08,1.3e9,0.022),
        stock("GENX","GeneX Therapeutics","Healthcare",22.0,0.05,0.1,6e8,0.0),
        stock("IRON","Ironclad Industries","Manufacturing",76.0,0.02,0.05,1.2e9,0.028),
        stock("PURE","PureHome Brands","Consumer Goods",110.0,0.012,0.05,1.6e9,0.031),
        stock("MRKT","MarketSquare Retail","Retail",48.0,0.02,0.03,1.9e9,0.025),
        stock("SWFT","SwiftAir Transport","Transportation",35.0,0.028,0.04,1e9,0.012),
        stock("URBN","Urbana REIT","Real Estate",63.0,0.014,0.04,1.5e9,0.058),
        stock("ORE","Deepcore Mining","Mining",19.0,0.033,0.03,2e9,0.035),
        stock("STAR","Starlight Media","Entertainment",88.0,0.026,0.09,1e9,0.006),
        stock("GRID","Gridline Utilities","Utilities",52.0,0.01,0.03,2.2e9,0.047)
    )

    private val curatedCrypto = listOf(
        ReferenceCryptoDefinition("BTX","Bitex",42_000.0,"moderate","fixed",19.5e6,21e6,0.017,0.0,EPOCH),
        ReferenceCryptoDefinition("ETR","Etheron",2_400.0,"high","burn",120e6,null,0.01,0.012,EPOCH),
        ReferenceCryptoDefinition("SOLA","Solara",95.0,"very high","inflationary",430e6,null,0.05,0.0,EPOCH),
        ReferenceCryptoDefinition("STB","StableBit",1.0,"low","inflationary",80e9,null,0.1,0.0,EPOCH),
        ReferenceCryptoDefinition("DOGO","Dogo",0.12,"extreme","inflationary",140e9,null,0.04,0.0,EPOCH),
        ReferenceCryptoDefinition("LNK","Linkora",14.0,"high","fixed",550e6,1e9,0.03,0.0,EPOCH),
        ReferenceCryptoDefinition("AVX","Avalux",34.0,"very high","fixed",370e6,720e6,0.04,0.0,EPOCH),
        ReferenceCryptoDefinition("FLR","Flare Grid",0.8,"extreme","burn",2e9,5e9,0.0,0.02,EPOCH)
    )

    private fun stock(
        ticker: String, name: String, sector: String, base: Double,
        volatility: Double, drift: Double, shares: Double, dividendYield: Double
    ): ReferenceStockDefinition {
        val margin = 0.08 + (hash(ticker).toUInt().toLong() % 20L) / 100.0
        return ReferenceStockDefinition(
            ticker = ticker,
            name = name,
            sector = sector,
            base = base,
            volatility = volatility,
            drift = drift,
            shares = shares,
            authorized = shares * 1.5,
            treasury = shares * 0.05,
            dividendYield = dividendYield,
            payoutRatio = if (dividendYield > 0.0) 0.4 + dividendYield * 5.0 else 0.0,
            revenue = shares * base * 0.35,
            margin = margin,
            assets = shares * base * 0.9,
            liabilities = shares * base * 0.4,
            description = "$name is a ${sector.lowercase(Locale.US)} company listed on the Tycoon Exchange.",
            listedAt = EPOCH
        )
    }

    private fun hash(value: String): Int {
        var result = 2166136261L
        value.forEach { result = (result xor it.code.toLong()) * 16777619L and 0xffffffffL }
        return result.toInt()
    }

    private fun mulberry(seed: Int): () -> Double {
        var state = seed
        return {
            state += 0x6d2b79f5
            var t = state
            t = (t xor (t ushr 15)) * (t or 1)
            t = t xor (t + ((t xor (t ushr 7)) * (t or 61)))
            ((t xor (t ushr 14)).toUInt().toLong()).toDouble() / 4294967296.0
        }
    }

    private fun wave(seed: Int, time: Double): Double {
        val random = mulberry(seed)
        var sum = 0.0
        var norm = 0.0
        repeat(6) { index ->
            val period = 20.0 + random() * 600.0 * (index + 1)
            val phase = random() * Math.PI * 2.0
            val amplitude = 1.0 / (index + 1)
            sum += amplitude * sin((time / period) * Math.PI * 2.0 + phase)
            norm += amplitude
        }
        val jitter = mulberry(seed xor floor(time).toInt()).invoke()
        return (sum / norm) * 0.9 + (jitter - 0.5) * 0.2
    }
}
