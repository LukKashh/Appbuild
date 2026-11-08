package com.example.engine

import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import java.util.Locale

/** Exact deterministic local implementation of the reference market.ts listing and price model. */
object ReferenceMarketSimulation {
    const val EPOCH = 1_790_812_800_000L
    const val DAY_MS = 86_400_000L
    const val STOCK_TICK_MS = 5L * 60_000L
    const val CRYPTO_TICK_MS = 2L * 60_000L
    const val CRYPTO_MIN_PRICE = 0.0001

    val sectors = listOf(
        "Technology","Finance","Energy","Healthcare","Manufacturing","Consumer Goods",
        "Retail","Transportation","Real Estate","Mining","Entertainment","Utilities"
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

    private fun stock(
        ticker: String, name: String, sector: String, base: Double,
        volatility: Double, drift: Double, shares: Double, dividendYield: Double
    ): ReferenceStockDefinition {
        val margin = 0.08 + (hashStr(ticker).toUInt().toLong() % 20L) / 100.0
        return ReferenceStockDefinition(
            ticker=ticker, name=name, sector=sector, base=base,
            volatility=volatility, drift=drift, shares=shares,
            authorized=shares*1.5, treasury=shares*0.05,
            dividendYield=dividendYield,
            payoutRatio=if (dividendYield > 0.0) 0.4 + dividendYield*5.0 else 0.0,
            revenue=shares*base*0.35, margin=margin,
            assets=shares*base*0.9, liabilities=shares*base*0.4,
            description="$name is a ${sector.lowercase(Locale.US)} company listed on the Tycoon Exchange.",
            listedAt=EPOCH
        )
    }

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
        ReferenceCryptoDefinition("BTX","Bitex",42_000.0,"moderate","fixed",19.5e6,21e6,0.017,0.0,EPOCH,"Store-of-value chain with a fixed supply."),
        ReferenceCryptoDefinition("ETR","Etheron",2_400.0,"high","burn",120e6,null,0.01,0.012,EPOCH,"Smart-contract platform with fee burning."),
        ReferenceCryptoDefinition("SOLA","Solara",95.0,"very high","inflationary",430e6,null,0.05,0.0,EPOCH,"High-throughput chain with inflationary rewards."),
        ReferenceCryptoDefinition("STB","StableBit",1.0,"low","inflationary",80e9,null,0.1,0.0,EPOCH,"Algorithmically managed low-volatility token."),
        ReferenceCryptoDefinition("DOGO","Dogo",0.12,"extreme","inflationary",140e9,null,0.04,0.0,EPOCH,"Meme token driven by sentiment."),
        ReferenceCryptoDefinition("LNK","Linkora",14.0,"high","fixed",550e6,1e9,0.03,0.0,EPOCH,"Oracle network bridging off-chain data."),
        ReferenceCryptoDefinition("AVX","Avalux",34.0,"very high","fixed",370e6,720e6,0.04,0.0,EPOCH,"Subnet platform for enterprise chains."),
        ReferenceCryptoDefinition("FLR","Flare Grid",0.8,"extreme","burn",2e9,5e9,0.0,0.02,EPOCH,"Energy-trading token with scheduled burns.")
    )

    fun hashStr(value: String): Int {
        var h = 0x811c9dc5.toInt()
        value.forEach { h = (h xor it.code) * 16777619 }
        return h
    }

    fun mulberry(seed: Int): () -> Double {
        var a = seed
        return {
            a += 0x6d2b79f5
            var t = a
            t = (t xor (t ushr 15)) * (t or 1)
            t = t xor (t + ((t xor (t ushr 7)) * (t or 61)))
            (t xor (t ushr 14)).toUInt().toLong().toDouble() / 4294967296.0
        }
    }

    fun wave(seed: Int, time: Double): Double {
        val r = mulberry(seed)
        var sum = 0.0
        var norm = 0.0
        repeat(6) { index ->
            val period = 20.0 + r() * 600.0 * (index + 1)
            val phase = r() * Math.PI * 2.0
            val amplitude = 1.0 / (index + 1)
            sum += amplitude * kotlin.math.sin((time / period) * Math.PI * 2.0 + phase)
            norm += amplitude
        }
        val jitter = mulberry(seed xor floor(time).toInt()).invoke()
        return (sum / norm) * 0.9 + (jitter - 0.5) * 0.2
    }

    fun listStocks(now: Long): List<ReferenceStockDefinition> {
        val count = floor((now - EPOCH).toDouble() / (3.0 * DAY_MS)).toInt().coerceAtLeast(0).coerceAtMost(60)
        val result = curatedStocks.toMutableList()
        val seen = result.map { it.ticker }.toMutableSet()
        repeat(count) { index ->
            val random = mulberry(hashStr("stock-list-$index"))
            val companyName = "${stockPrefixes[(random() * stockPrefixes.size).toInt()]} ${stockSuffixes[(random() * stockSuffixes.size).toInt()]}"
            val sector = sectors[(random() * sectors.size).toInt()]
            val base = round((5.0 + random() * 120.0) * 100.0) / 100.0
            val shares = round((2.0 + random() * 15.0) * 1e8)
            val dy = if (random() < 0.5) 0.0 else round(random() * 50.0) / 1000.0
            val tickerBase = companyName.filter { it in 'A'..'Z' }.take(3) + ('A'.code + index % 26).toChar()
            val ticker = if (tickerBase in seen) tickerBase + index else tickerBase
            seen += ticker
            result += ReferenceStockDefinition(
                ticker=ticker, name="$companyName ${index+1}", sector=sector, base=base,
                volatility=0.02+random()*0.03, drift=-0.02+random()*0.15, shares=shares,
                authorized=shares*1.5, treasury=shares*0.05, dividendYield=dy,
                payoutRatio=if (dy > 0) 0.5 else 0.0,
                revenue=shares*base*0.3, margin=-0.05+random()*0.25,
                assets=shares*base*0.8, liabilities=shares*base*0.45,
                description="Newly listed through the Tycoon Exchange IPO programme (listing event #${index+1}).",
                listedAt=EPOCH+(index+1)*3L*DAY_MS, generated=true
            )
        }
        return result
    }

    fun listCrypto(now: Long): List<ReferenceCryptoDefinition> {
        val count = floor((now - EPOCH).toDouble() / (7.0 * DAY_MS)).toInt().coerceAtLeast(0).coerceAtMost(40)
        val result = curatedCrypto.toMutableList()
        val seen = result.map { it.symbol }.toMutableSet()
        val names = result.map { it.name }.toMutableSet()
        repeat(count) { index ->
            val random = mulberry(hashStr("crypto-list-$index"))
            var name = cryptoPrefixes[(random()*cryptoPrefixes.size).toInt()] + cryptoSuffixes[(random()*cryptoSuffixes.size).toInt()]
            val profiles = listOf("high","very high","extreme")
            val model = listOf("fixed","inflationary","burn")[(random()*3).toInt()]
            val circulating = round((0.1+random()*10.0)*1e8)
            val symbol = (name.take(3)+('A'.code+index%26).toChar()).uppercase(Locale.US)
            val finalSymbol = if (symbol in seen) symbol + "X" else symbol
            if (name in names) name += " ${index+1}"
            seen += finalSymbol
            names += name
            result += ReferenceCryptoDefinition(
                symbol=finalSymbol, name=name,
                base=round((0.05+random()*20.0)*1000.0)/1000.0,
                profile=profiles[(random()*3).toInt()], supplyModel=model,
                circulating=circulating, maximumSupply=if (model=="fixed") circulating*2 else null,
                inflation=if (model=="inflationary") 0.05+random()*0.1 else 0.02,
                burn=if (model=="burn") 0.02+random()*0.03 else 0.0,
                listedAt=EPOCH+(index+1)*7L*DAY_MS,
                description="Listed via Crypto Launchpad event #${index+1}."
            )
        }
        return result
    }

    fun stockTick(now: Long): Long = Math.floorDiv(now, STOCK_TICK_MS)
    fun cryptoTick(now: Long): Long = Math.floorDiv(now, CRYPTO_TICK_MS)
    fun stockTickTime(tick: Long): Long = tick * STOCK_TICK_MS
    fun cryptoTickTime(tick: Long): Long = tick * CRYPTO_TICK_MS

    fun stockPriceAtTick(stock: ReferenceStockDefinition, tick: Long): Double {
        val perDay = (24.0 * 60.0) / 5.0
        val years = (tick - stockTick(stock.listedAt)) / (perDay * 365.0)
        val seed = hashStr(stock.ticker)
        val w = wave(seed, tick / 6.0)*stock.volatility*6.0 + wave(seed xor 99, tick/60.0)*stock.volatility*10.0
        val sector = wave(hashStr(stock.sector), tick/perDay)*0.06
        val market = wave(777, tick/perDay)*0.08
        val price = stock.base * exp(stock.drift*years + w + sector + market)
        return max(0.01, round(price*100.0)/100.0)
    }

    fun stockLiquidity(stock: ReferenceStockDefinition, tick: Long): Double =
        max(10.0, floor((stock.shares*0.00002*(0.6+0.4*(wave(hashStr(stock.ticker) xor 5,tick.toDouble())+1.0))) /
            (1.0+stock.volatility*10.0)))

    fun stockProfitable(stock: ReferenceStockDefinition, utcDay: Long): Boolean =
        stock.margin + wave(hashStr(stock.ticker) xor 31, utcDay.toDouble())*0.12 > 0.0

    fun cryptoPriceAtTick(crypto: ReferenceCryptoDefinition, tick: Long): Double {
        val volatility = when(crypto.profile) {
            "low" -> 0.02
            "moderate" -> 0.04
            "high" -> 0.07
            "very high" -> 0.1
            else -> 0.15
        }
        val perDay = (24.0*60.0)/2.0
        val sentiment = wave(4242,tick/perDay)*0.15
        val w = wave(hashStr(crypto.symbol),tick/4.0)*volatility*4.0 + wave(hashStr(crypto.symbol) xor 7,tick/90.0)*volatility*12.0
        val p = crypto.base*exp(w+sentiment)
        return max(CRYPTO_MIN_PRICE, String.format(Locale.US,"%.6g",p).toDouble())
    }

    fun cryptoSupply(crypto: ReferenceCryptoDefinition, now: Long): ReferenceCryptoSupply {
        val years = max(0.0,(now-crypto.listedAt)/ (365.0*DAY_MS))
        var circ = crypto.circulating * (1.0+crypto.inflation-crypto.burn).pow(years)
        crypto.maximumSupply?.let { circ=min(circ,it) }
        return ReferenceCryptoSupply(circ, crypto.circulating*crypto.burn*years, crypto.maximumSupply)
    }

    fun cryptoLiquidity(crypto: ReferenceCryptoDefinition, tick: Long): Double =
        crypto.circulating*0.000002*(0.5+0.5*(wave(hashStr(crypto.symbol) xor 3,tick.toDouble())+1.0))

    fun priceStock(stock: ReferenceStockDefinition, now: Long) = stockPriceAtTick(stock, stockTick(now))
    fun priceCrypto(crypto: ReferenceCryptoDefinition, now: Long) = cryptoPriceAtTick(crypto, cryptoTick(now))
    fun getStockPrice(symbol: String, now: Long): Double? = listStocks(now).firstOrNull { it.ticker == symbol }?.let { priceStock(it,now) }
    fun getCryptoPrice(symbol: String, now: Long): Double? = listCrypto(now).firstOrNull { it.symbol == symbol }?.let { priceCrypto(it,now) }
}
