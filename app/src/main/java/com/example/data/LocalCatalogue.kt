package com.example.data

import com.example.model.*
kotlin.math.pow

/** Expands the shipped, deterministic local catalogue. No remote catalogue fetch is used. */
object LocalCatalogue {
    fun businesses(source: List<Business>): List<Business> {
        val enriched = source.mapIndexed { index, b ->
            b.copy(operatingExpensePerSec = if (b.operatingExpensePerSec > 0.0) b.operatingExpensePerSec else b.baseRevenuePerSec * 0.12,
                employeeCapacity = maxOf(20, b.employeeCapacity), branchLimit = maxOf(10, b.branchLimit),
                reserves = if (b.reserves > 0.0) b.reserves else b.baseCost * 0.03,
                offlineCapHours = listOf(2.0, 4.0, 6.0, 8.0)[index % 4],
                staffRoles = if (b.staffRoles.isEmpty()) mapOf("Operations" to b.employeeCount) else b.staffRoles)
        }
        val definitions = listOf(
            arrayOf("gym", "IronPulse Fitness Clubs", "FOOD", "12000", "4.6", "1.7"),
            arrayOf("foodtruck", "Street Feast Food Trucks", "FOOD", "40000", "12", "4.2"),
            arrayOf("clinic", "BrightSmile Dental Clinics", "BANKING", "95000", "26", "10"),
            arrayOf("arena", "NovaPlay Esports Arena", "ENTERTAINMENT", "220000", "58", "22"),
            arrayOf("supermarket", "FreshCart Supermarkets", "RETAIL", "550000", "145", "60"),
            arrayOf("farm", "GreenAcre Agricultural Group", "INDUSTRIAL", "1500000", "380", "160"),
            arrayOf("refinery", "Crestline Energy Refinery", "ENERGY", "4500000", "1100", "470"),
            arrayOf("mine", "Deepvein Mining Corporation", "INDUSTRIAL", "15000000", "3600", "1580"),
            arrayOf("retail", "Vantage Retail Group", "RETAIL", "60000000", "13800", "6000"),
            arrayOf("aero", "Skyward Aerospace Systems", "MANUFACTURING", "250000000", "56000", "25000")
        )
        val extra = definitions.map { d ->
            val price = d[3].toDouble()
            val revenue = d[4].toDouble()
            Business(id = "biz_local_" + d[0], name = d[1], industry = Industry.valueOf(d[2]), baseCost = price,
                baseRevenuePerSec = revenue, salaryPerEmployee = maxOf(1.0, price / 500000.0), currentBranches = 0,
                operatingExpensePerSec = d[5].toDouble(), employeeCapacity = 30, minimumEmployees = 2, branchLimit = 12,
                reserves = price * 0.08, liabilities = price * 0.02, offlineCapHours = 4.0,
                upgrades = listOf(
                    BusinessUpgrade(d[0] + "_automation", "Automation Systems", price * 0.18, 1.12, maxLevel = 5, revenueBonusPerLevel = 0.025),
                    BusinessUpgrade(d[0] + "_procurement", "Strategic Procurement", price * 0.22, 1.08, maxLevel = 5, expenseReductionPerLevel = 0.035),
                    BusinessUpgrade(d[0] + "_training", "Workforce Development", price * 0.12, 1.06, maxLevel = 5, staffEfficiencyPerLevel = 0.02)
                ))
        }
        return enriched + extra.filterNot { item -> enriched.any { it.id == item.id } }
    }

    fun properties(source: List<Property>): List<Property> {
        val enriched = source.mapIndexed { i, p -> p.copy(
            location = if (p.location == "Unassigned") listOf("Central District", "Riverside", "Innovation Park", "Old Town")[i % 4] else p.location,
            operatingExpensePerSec = if (p.operatingExpensePerSec > 0.0) p.operatingExpensePerSec else p.baseRentPerSec * 0.22,
            occupancyRate = 0.68 + (i % 5) * 0.045, demandMultiplier = 0.94 + (i % 4) * 0.04, preciseCondition = p.conditionPercent.toDouble()) }
        val definitions = listOf(
            arrayOf("room", "Shared House Room", "RESIDENTIAL", "Student Quarter", "15000", "8.25", "1.9"),
            arrayOf("garage", "Lockup Garage Row", "INDUSTRIAL", "Backstreets", "22000", "12.1", "2.8"),
            arrayOf("kiosk", "Station Retail Kiosk", "COMMERCIAL", "Central Station", "30000", "16.5", "3.8"),
            arrayOf("cabin", "Lakeside Holiday Cabin", "HOSPITALITY", "Lakeshore", "55000", "30", "7"),
            arrayOf("loft", "Converted Brick Loft", "RESIDENTIAL", "Arts District", "80000", "44", "10"),
            arrayOf("clinic", "Medical Office Suites", "COMMERCIAL", "Uptown", "1000000", "550", "126"),
            arrayOf("motel", "Highway Business Motel", "HOSPITALITY", "Interstate", "2000000", "1100", "253"),
            arrayOf("marina", "Marina Townhomes", "LUXURY", "Marina", "5500000", "3025", "696"),
            arrayOf("tower", "Midtown Office Tower", "COMMERCIAL", "Midtown", "11000000", "6050", "1392")
        )
        val extras = definitions.map { d -> Property(id = "prop_local_" + d[0], name = d[1], category = PropertyCategory.valueOf(d[2]),
            location = d[3], purchasePrice = d[4].toDouble(), currentValuation = d[4].toDouble(),
            baseRentPerSec = d[5].toDouble(), operatingExpensePerSec = d[6].toDouble(), occupancyRate = 0.72)
        }
        return enriched + extras.filterNot { item -> enriched.any { it.id == item.id } }
    }

    fun stocks(source: List<Stock>): List<Stock> {
        val definitions = listOf(
            arrayOf("NSS", "Northstar Systems", "Technology", "124.5", "0.022"),
            arrayOf("MDH", "Meridian Health", "Healthcare", "86.2", "0.031"),
            arrayOf("APX", "Apex Energy", "Energy", "42.8", "0.044"),
            arrayOf("CRF", "Crown Financial", "Finance", "196.0", "0.028"),
            arrayOf("IGI", "IronGate Industries", "Industrials", "74.6", "0.019"),
            arrayOf("EVR", "Evergreen Retail", "Consumer", "31.7", "0.012"),
            arrayOf("ATM", "Atlas Mobility", "Transport", "58.9", "0.008"),
            arrayOf("HLM", "Helios Materials", "Materials", "112.3", "0.015"),
            arrayOf("VTX", "Vertex Cloud", "Technology", "255.0", "0.0"),
            arrayOf("BPR", "BluePeak Robotics", "Technology", "18.4", "0.0"),
            arrayOf("SML", "Summit Logistics", "Transport", "93.0", "0.024"),
            arrayOf("NVB", "Nova Biotech", "Healthcare", "12.8", "0.0"),
            arrayOf("RDU", "Redwood Utilities", "Utilities", "66.2", "0.038"),
            arrayOf("QNT", "Quantum Networks", "Technology", "304.0", "0.0"),
            arrayOf("PNF", "Pioneer Foods", "Consumer", "48.5", "0.029"),
            arrayOf("SLA", "Silverline Aerospace", "Industrials", "177.0", "0.006"),
            arrayOf("KSS", "Keystone Security", "Technology", "39.4", "0.0"),
            arrayOf("AUM", "Aurora Media", "Entertainment", "22.1", "0.014")
        )
        val current = source.map { if (it.basePrice == it.currentPrice) it.copy(basePrice = it.currentPrice) else it }
        val extra = definitions.map { d -> val p = d[3].toDouble(); Stock(d[0], d[1], d[2], p, p * 0.992,
            dividendYieldPercent = d[4].toDouble(), basePrice = p, volatility = 0.014 + (d[0].length * 0.003), liquidity = 1200.0) }
        return current + extra.filterNot { item -> current.any { it.symbol == item.symbol } }
    }

    fun cryptos(source: List<Crypto>): List<Crypto> {
        val definitions = listOf(
            arrayOf("SOLP", "Solstice Protocol", "0.014", "0.07", "Payments"), arrayOf("LMN", "Lumen Chain", "0.18", "0.06", "Infrastructure"),
            arrayOf("TRN", "TerraNova", "1.25", "0.10", "Data"), arrayOf("ORBF", "Orbit Finance", "4.8", "0.09", "DeFi"),
            arrayOf("NXD", "Nexus Data", "12", "0.05", "Data"), arrayOf("ARCA", "Arcadia Token", "0.065", "0.13", "Gaming"),
            arrayOf("PLS", "Pulse Network", "0.42", "0.11", "Infrastructure"), arrayOf("CBLT", "Cobalt Ledger", "2.2", "0.08", "Payments"),
            arrayOf("PRSM", "Prism Utility", "18", "0.07", "Utility"), arrayOf("AETH", "Aether Exchange", "0.008", "0.15", "DeFi"),
            arrayOf("CIND", "Cinder Compute", "0.92", "0.12", "Infrastructure"), arrayOf("MRDN", "Meridian DAO", "6.7", "0.09", "Governance"),
            arrayOf("VELA", "Vela Payments", "0.31", "0.08", "Payments"), arrayOf("HLX", "Helix Storage", "24", "0.06", "Data"),
            arrayOf("MNR", "Monarch Protocol", "3.4", "0.10", "DeFi")
        )
        val current = source.map { if (it.basePrice == it.currentPrice) it.copy(basePrice = it.currentPrice) else it }
        val extra = definitions.map { d -> val p = d[2].toDouble(); Crypto(d[0], d[1], p, p * 0.985,
            volatility = d[3].toDouble(), basePrice = p, maxSupply = 1_000_000_000.0, circulatingSupply = 150_000_000.0,
            liquidity = 100000.0, networkScore = 65.0, projectType = d[4]) }
        return current + extra.filterNot { item -> current.any { it.symbol == item.symbol } }
    }

    fun collectibles(source: List<Collectible>, now: Long = System.currentTimeMillis()): List<Collectible> {
        val items = source.toMutableList()
        val categories = listOf(
            Pair("Vehicles", listOf("Viper", "Comet", "Stallion", "Phantom", "Raptor", "Aurora", "Vortex", "Monza", "Spectre")),
            Pair("Aircraft", listOf("Skyhawk", "Albatross", "Condor", "Swift", "Falcon", "Heron", "Osprey")),
            Pair("Yachts", listOf("Serenity", "Odyssey", "Poseidon", "Marlin", "Coral", "Neptune")),
            Pair("Watches", listOf("Regent", "Oyster", "Meridian", "Calibre", "Heritage", "Admiral")),
            Pair("Fine Art", listOf("Twilight", "Blue Horizon", "The Passage", "Silent City", "Golden Hour")),
            Pair("Jewelry", listOf("Eternal", "Celestial", "Imperial", "Radiant", "Crown")),
            Pair("Antiques", listOf("Regency", "Victorian", "Art Deco", "Baroque", "Neoclassical")),
            Pair("Technology", listOf("Quantum", "Photon", "Neural", "Titanium", "Plasma")),
            Pair("Collector Editions", listOf("Founders", "Legacy", "Centennial", "Monument", "Sovereign")),
            Pair("Rare Timepieces", listOf("Chronos", "Meridian", "Eclipse", "Orbit", "Astra"))
        )
        val subtypes = listOf("Edition", "Series", "Prototype", "Reserve", "Signature")
        val rarity = Rarity.values()
        val weights = listOf(0.0, 0.42, 0.69, 0.85, 0.94, 0.985, 1.0)
        val priceBases = listOf(20000.0, 400000.0, 900000.0, 5000.0, 15000.0, 35000.0, 12000.0, 18000.0, 60000.0, 125000.0)
        val holdHours = listOf(1.0, 3.0, 6.0, 12.0, 24.0, 72.0)
        for (i in 0 until 300) {
            if (items.size >= 300) break
            val categoryIndex = i % categories.size
            val itemIndex = i / categories.size + 1
            val category = categories[categoryIndex]
            val name = category.second[(itemIndex - 1) % category.second.size] + " " + subtypes[itemIndex % subtypes.size]
            val seed = (category.first + "_" + itemIndex).hashCode() and 0x7fffffff
            val roll = (seed % 10000) / 10000.0
            var rarityIndex = 0
            for (r in 1 until weights.size) if (roll > weights[r - 1]) rarityIndex = r - 1
            rarityIndex = when { roll < 0.42 -> 0; roll < 0.69 -> 1; roll < 0.85 -> 2; roll < 0.94 -> 3; roll < 0.985 -> 4; else -> 5 }
            val multiplier = listOf(1.0, 3.0, 10.0, 40.0, 200.0, 1500.0)[rarityIndex]
            val value = priceBases[categoryIndex] * multiplier * (0.70 + (seed % 6000) / 10000.0)
            val id = "local_col_" + categoryIndex + "_" + itemIndex
            if (items.none { it.id == id }) items.add(Collectible(id, name, category.first, rarity[rarityIndex], value * 1.05, value,
                appreciationRatePerSec = 0.000015 + rarityIndex * 0.00001, bonusDescription = if (rarityIndex >= 2) "Portfolio collection benefit" else "Collectible asset",
                subCategory = subtypes[itemIndex % subtypes.size], limitedEdition = rarityIndex >= 4, releaseAt = now,
                holdUntil = now + (holdHours[rarityIndex] * 3600000.0).toLong(), resalePercent = listOf(0.70,0.72,0.75,0.78,0.80,0.85)[rarityIndex],
                volatility = 0.06 + rarityIndex * 0.018, bonusKey = listOf("businessPct","offlineHours","rentPct","clickPct","xpPct","feeCut")[categoryIndex % 6],
                bonusValue = if (rarityIndex >= 2) rarityIndex * 0.004 else 0.0))
        }
        return items.take(300)
    }

    fun research(source: List<ResearchNode>): List<ResearchNode> {
        val names = listOf(
            "Supply Chain Analytics" to "Business", "Predictive Maintenance" to "Industrial", "Workforce Optimisation" to "Business",
            "Advanced Procurement" to "Business", "Portfolio Risk Models" to "Finance", "Algorithmic Execution" to "Finance",
            "Property Demand Forecasting" to "Real Estate", "Structural Engineering" to "Real Estate", "Renewable Infrastructure" to "Energy",
            "Autonomous Logistics" to "Industrial", "Advanced Materials" to "Industrial", "Quantum Computing" to "Technology",
            "Machine Learning Operations" to "Technology", "Global Market Access" to "Finance", "Corporate Governance" to "Management",
            "Capital Allocation" to "Management", "Research Automation" to "Management", "Luxury Authentication" to "Collectibles",
            "Digital Asset Custody" to "Finance", "Strategic Intelligence" to "Management"
        )
        val extras = names.mapIndexed { i, pair -> ResearchNode("local_research_" + (i + 1), pair.first, pair.second,
            25000.0 * 1.72.pow(i.toDouble()), "Unlocks an advanced permanent improvement to this division.",
            requiredResearchId = if (i > 0) "local_research_" + i else null, durationHours = 0.5 + (i % 6) * 0.75,
            researchPointsCost = 20.0 + i * 8.0, minimumPlayerLevel = 2 + i / 3,
            effects = mapOf(listOf("businessPct", "feeCut", "rentPct", "researchSpeed", "offlineHours")[i % 5] to (0.01 + (i % 4) * 0.005))) }
        return source + extras.filterNot { extra -> source.any { it.id == extra.id } }
    }
}