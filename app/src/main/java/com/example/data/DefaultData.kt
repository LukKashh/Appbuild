package com.example.data

import com.example.model.*

object DefaultData {

    fun initialClickUpgrades(): List<ClickUpgrade> = listOf(
        ClickUpgrade("click_1", "Swift Fingers", "Increases cash earned per tap by +$1", baseCost = 25.0, powerBoost = 1.0),
        ClickUpgrade("click_2", "Tactile Stylus", "Increases cash earned per tap by +$5", baseCost = 150.0, powerBoost = 5.0),
        ClickUpgrade("click_3", "Gold Touch", "Increases cash earned per tap by +$25", baseCost = 1200.0, powerBoost = 25.0),
        ClickUpgrade("click_4", "Micro-Haptic Grip", "Increases cash earned per tap by +$150", baseCost = 10000.0, powerBoost = 150.0),
        ClickUpgrade("click_5", "Quantum Accelerator", "Increases cash earned per tap by +$1,000", baseCost = 100000.0, powerBoost = 1000.0),
        ClickUpgrade("click_6", "Titanium Pressure Tap", "Increases cash earned per tap by +$8,000", baseCost = 1500000.0, powerBoost = 8000.0)
    )

    fun initialBusinesses(): List<Business> = listOf(
        // Food & Beverage
        Business(
            id = "biz_food_1",
            name = "Artisan Coffee Kiosk",
            industry = Industry.FOOD,
            baseCost = 100.0,
            baseRevenuePerSec = 4.0,
            salaryPerEmployee = 1.0,
            upgrades = listOf(
                BusinessUpgrade("bf1_u1", "Espresso Machine Upgrade", 250.0, 1.5),
                BusinessUpgrade("bf1_u2", "Mobile Ordering App", 800.0, 2.0),
                BusinessUpgrade("bf1_u3", "Organic Bean Sourcing", 3000.0, 2.5)
            )
        ),
        Business(
            id = "biz_food_2",
            name = "Gourmet Burger Chain",
            industry = Industry.FOOD,
            baseCost = 5000.0,
            baseRevenuePerSec = 45.0,
            salaryPerEmployee = 5.0,
            upgrades = listOf(
                BusinessUpgrade("bf2_u1", "Drive-Thru Lane Addition", 12000.0, 1.8),
                BusinessUpgrade("bf2_u2", "Automated Patty Grill", 35000.0, 2.2),
                BusinessUpgrade("bf2_u3", "Franchise Licensing Deal", 100000.0, 3.0)
            )
        ),

        // Retail & Commerce
        Business(
            id = "biz_retail_1",
            name = "Neighborhood Convenience Mart",
            industry = Industry.RETAIL,
            baseCost = 1500.0,
            baseRevenuePerSec = 18.0,
            salaryPerEmployee = 2.5,
            upgrades = listOf(
                BusinessUpgrade("br1_u1", "Self-Checkout Kiosks", 4000.0, 1.6),
                BusinessUpgrade("br1_u2", "ATM Partner Placement", 9000.0, 1.9),
                BusinessUpgrade("br1_u3", "24/7 Energy Operations", 25000.0, 2.4)
            )
        ),
        Business(
            id = "biz_retail_2",
            name = "Luxury Designer Boutique",
            industry = Industry.RETAIL,
            baseCost = 45000.0,
            baseRevenuePerSec = 280.0,
            salaryPerEmployee = 15.0,
            upgrades = listOf(
                BusinessUpgrade("br2_u1", "Runway Fashion Partnership", 90000.0, 2.0),
                BusinessUpgrade("br2_u2", "Private Client Lounge", 220000.0, 2.5),
                BusinessUpgrade("br2_u3", "Global Celebrity Endorsement", 600000.0, 3.5)
            )
        ),

        // Hospitality
        Business(
            id = "biz_hosp_1",
            name = "Urban Boutique Inn",
            industry = Industry.HOSPITALITY,
            baseCost = 120000.0,
            baseRevenuePerSec = 750.0,
            salaryPerEmployee = 30.0,
            upgrades = listOf(
                BusinessUpgrade("bh1_u1", "Rooftop Sky Lounge", 250000.0, 1.8),
                BusinessUpgrade("bh1_u2", "Infinity Spa & Sauna", 600000.0, 2.3),
                BusinessUpgrade("bh1_u3", "Michelin Guide Feature", 1500000.0, 3.0)
            )
        ),
        Business(
            id = "biz_hosp_2",
            name = "Grand Palace Resort & Casino",
            industry = Industry.HOSPITALITY,
            baseCost = 1200000.0,
            baseRevenuePerSec = 6500.0,
            salaryPerEmployee = 120.0,
            upgrades = listOf(
                BusinessUpgrade("bh2_u1", "High-Roller VIP Suite Wing", 2500000.0, 2.0),
                BusinessUpgrade("bh2_u2", "Helipad & Chauffeur Fleet", 6000000.0, 2.5),
                BusinessUpgrade("bh2_u3", "World Championship Arena", 18000000.0, 3.8)
            )
        ),

        // Transportation
        Business(
            id = "biz_trans_1",
            name = "Metro Express Fleet",
            industry = Industry.TRANSPORT,
            baseCost = 25000.0,
            baseRevenuePerSec = 175.0,
            salaryPerEmployee = 12.0,
            upgrades = listOf(
                BusinessUpgrade("bt1_u1", "Electric Vehicle Fleet Swap", 60000.0, 1.8),
                BusinessUpgrade("bt1_u2", "AI Dispatch Algorithm", 140000.0, 2.2),
                BusinessUpgrade("bt1_u3", "City Airport Transit Contract", 400000.0, 3.0)
            )
        ),
        Business(
            id = "biz_trans_2",
            name = "Trans-Continental Cargo Line",
            industry = Industry.TRANSPORT,
            baseCost = 850000.0,
            baseRevenuePerSec = 4800.0,
            salaryPerEmployee = 90.0,
            upgrades = listOf(
                BusinessUpgrade("bt2_u1", "Triple-Deck Container Vessels", 1800000.0, 2.0),
                BusinessUpgrade("bt2_u2", "Deep-Water Terminal Berth", 4500000.0, 2.4),
                BusinessUpgrade("bt2_u3", "Satellite Fleet Telemetry", 12000000.0, 3.2)
            )
        ),

        // Technology
        Business(
            id = "biz_tech_1",
            name = "Cloud SaaS Micro-Platform",
            industry = Industry.TECH,
            baseCost = 80000.0,
            baseRevenuePerSec = 520.0,
            salaryPerEmployee = 25.0,
            upgrades = listOf(
                BusinessUpgrade("tc1_u1", "Serverless Infrastructure", 180000.0, 1.9),
                BusinessUpgrade("tc1_u2", "Enterprise SSO & Compliance", 450000.0, 2.4),
                BusinessUpgrade("tc1_u3", "AI Agent Copilot Plugin", 1200000.0, 3.5)
            )
        ),
        Business(
            id = "biz_tech_2",
            name = "Quantum Neural Foundry",
            industry = Industry.TECH,
            baseCost = 3500000.0,
            baseRevenuePerSec = 22000.0,
            salaryPerEmployee = 300.0,
            upgrades = listOf(
                BusinessUpgrade("tc2_u1", "2-Nanometer Optical Photolithography", 8000000.0, 2.2),
                BusinessUpgrade("tc2_u2", "Cryogenic Cooled Core Substrates", 20000000.0, 2.8),
                BusinessUpgrade("tc2_u3", "AGI Model Training Cluster", 60000000.0, 4.0)
            )
        ),

        // Manufacturing
        Business(
            id = "biz_mfg_1",
            name = "Precision Tool & Die Works",
            industry = Industry.MANUFACTURING,
            baseCost = 200000.0,
            baseRevenuePerSec = 1200.0,
            salaryPerEmployee = 40.0,
            upgrades = listOf(
                BusinessUpgrade("mf1_u1", "5-Axis CNC Milling Cells", 450000.0, 1.8),
                BusinessUpgrade("mf1_u2", "Cobot Collaborative Robotic Arms", 1100000.0, 2.2),
                BusinessUpgrade("mf1_u3", "Aerospace Grade Certification", 3000000.0, 3.0)
            )
        ),
        Business(
            id = "biz_mfg_2",
            name = "Automated Giga-Assembly Plant",
            industry = Industry.MANUFACTURING,
            baseCost = 8000000.0,
            baseRevenuePerSec = 55000.0,
            salaryPerEmployee = 600.0,
            upgrades = listOf(
                BusinessUpgrade("mf2_u1", "Unified Cast Chassis Molding", 18000000.0, 2.1),
                BusinessUpgrade("mf2_u2", "Lights-Out Factory Shift", 45000000.0, 2.7),
                BusinessUpgrade("mf2_u3", "Solid-State Battery Production", 130000000.0, 3.8)
            )
        ),

        // Banking & Finance
        Business(
            id = "biz_bank_1",
            name = "Apex Wealth Advisory",
            industry = Industry.BANKING,
            baseCost = 500000.0,
            baseRevenuePerSec = 3200.0,
            salaryPerEmployee = 65.0,
            upgrades = listOf(
                BusinessUpgrade("bk1_u1", "Private Client Family Office", 1200000.0, 1.9),
                BusinessUpgrade("bk1_u2", "Hedge Fund Syndication Desk", 3200000.0, 2.5),
                BusinessUpgrade("bk1_u3", "Prime Brokerage Clearing License", 9000000.0, 3.4)
            )
        ),
        Business(
            id = "biz_bank_2",
            name = "Sovereign Reserve Bank",
            industry = Industry.BANKING,
            baseCost = 25000000.0,
            baseRevenuePerSec = 180000.0,
            salaryPerEmployee = 1500.0,
            upgrades = listOf(
                BusinessUpgrade("bk2_u1", "Interbank Central Liquidity Hub", 55000000.0, 2.2),
                BusinessUpgrade("bk2_u2", "Cross-Border Settlement Network", 140000000.0, 2.8),
                BusinessUpgrade("bk2_u3", "National Reserve Currency Issuer", 400000000.0, 4.2)
            )
        ),

        // Energy & Power
        Business(
            id = "biz_energy_1",
            name = "Helios Solar & Battery Array",
            industry = Industry.ENERGY,
            baseCost = 650000.0,
            baseRevenuePerSec = 4100.0,
            salaryPerEmployee = 50.0,
            upgrades = listOf(
                BusinessUpgrade("en1_u1", "Perovskite Photovoltaic Cells", 1500000.0, 1.9),
                BusinessUpgrade("en1_u2", "Megawatt Molten Salt Storage", 3800000.0, 2.4),
                BusinessUpgrade("en1_u3", "Grid Peaker Power Arbitrage", 11000000.0, 3.3)
            )
        ),
        Business(
            id = "biz_energy_2",
            name = "Tokamak Fusion Complex",
            industry = Industry.ENERGY,
            baseCost = 75000000.0,
            baseRevenuePerSec = 620000.0,
            salaryPerEmployee = 3500.0,
            upgrades = listOf(
                BusinessUpgrade("en2_u1", "Magnetic Superconducting Coils", 160000000.0, 2.3),
                BusinessUpgrade("en2_u2", "Tritium Breeding Reactor Blanket", 400000000.0, 3.0),
                BusinessUpgrade("en2_u3", "Limitless Net-Positive Clean Grid", 1200000000.0, 4.5)
            )
        ),

        // Entertainment
        Business(
            id = "biz_ent_1",
            name = "Indie Game & Media Studio",
            industry = Industry.ENTERTAINMENT,
            baseCost = 150000.0,
            baseRevenuePerSec = 950.0,
            salaryPerEmployee = 30.0,
            upgrades = listOf(
                BusinessUpgrade("et1_u1", "Global Viral Franchise Debut", 350000.0, 2.0),
                BusinessUpgrade("et1_u2", "Live-Service Battle Pass Ecosystem", 900000.0, 2.6),
                BusinessUpgrade("et1_u3", "Esports Arena World Tour", 2500000.0, 3.5)
            )
        ),
        Business(
            id = "biz_ent_2",
            name = "OmniMedia Cinema Studio & Network",
            industry = Industry.ENTERTAINMENT,
            baseCost = 15000000.0,
            baseRevenuePerSec = 110000.0,
            salaryPerEmployee = 1000.0,
            upgrades = listOf(
                BusinessUpgrade("et2_u1", "Global Box Office Blockbuster Series", 35000000.0, 2.1),
                BusinessUpgrade("et2_u2", "Worldwide Direct-to-Consumer Streaming", 90000000.0, 2.7),
                BusinessUpgrade("et2_u3", "Immersive Theme Park Attraction", 280000000.0, 4.0)
            )
        ),

        // Industrial Operations
        Business(
            id = "biz_ind_1",
            name = "Titan Rare Minerals Quarry",
            industry = Industry.INDUSTRIAL,
            baseCost = 2200000.0,
            baseRevenuePerSec = 14500.0,
            salaryPerEmployee = 180.0,
            upgrades = listOf(
                BusinessUpgrade("in1_u1", "Autonomous Heavy Hauler Fleet", 5000000.0, 2.0),
                BusinessUpgrade("in1_u2", "In-Situ Smelting Refinery", 13000000.0, 2.6),
                BusinessUpgrade("in1_u3", "Deep-Seam Lithium Extraction", 38000000.0, 3.6)
            )
        ),
        Business(
            id = "biz_ind_2",
            name = "Astra Orbital Shipyards",
            industry = Industry.INDUSTRIAL,
            baseCost = 180000000.0,
            baseRevenuePerSec = 1600000.0,
            salaryPerEmployee = 7000.0,
            upgrades = listOf(
                BusinessUpgrade("in2_u1", "Zero-G Vacuum Metal Printing", 400000000.0, 2.4),
                BusinessUpgrade("in2_u2", "Ion Propulsion Drive Assembly", 1000000000.0, 3.2),
                BusinessUpgrade("in2_u3", "Interplanetary Freight Carrier Line", 3000000000.0, 5.0)
            )
        )
    )

    fun initialProperties(): List<Property> = listOf(
        Property(
            id = "prop_1",
            name = "Suburban Family Duplex",
            category = PropertyCategory.RESIDENTIAL,
            purchasePrice = 12000.0,
            currentValuation = 12000.0,
            baseRentPerSec = 12.0,
            upgrades = listOf(
                PropertyUpgrade("pu_1_1", "Hardwood Floors & Modern Kitchen", 2500.0, 1.4),
                PropertyUpgrade("pu_1_2", "Rooftop Solar Integration", 6000.0, 1.8)
            )
        ),
        Property(
            id = "prop_2",
            name = "Downtown Studio Apartment",
            category = PropertyCategory.RESIDENTIAL,
            purchasePrice = 65000.0,
            currentValuation = 65000.0,
            baseRentPerSec = 68.0,
            upgrades = listOf(
                PropertyUpgrade("pu_2_1", "Smart Home Automation Hub", 14000.0, 1.5),
                PropertyUpgrade("pu_2_2", "Soundproof Panoramic Windows", 30000.0, 2.0)
            )
        ),
        Property(
            id = "prop_3",
            name = "Glass Penthouse Suite",
            category = PropertyCategory.LUXURY,
            purchasePrice = 380000.0,
            currentValuation = 380000.0,
            baseRentPerSec = 390.0,
            upgrades = listOf(
                PropertyUpgrade("pu_3_1", "Private Heated Infinity Pool", 85000.0, 1.7),
                PropertyUpgrade("pu_3_2", "Dedicated Elevator & Concierge", 180000.0, 2.2)
            )
        ),
        Property(
            id = "prop_4",
            name = "Silicon Hub Tech Office",
            category = PropertyCategory.COMMERCIAL,
            purchasePrice = 1250000.0,
            currentValuation = 1250000.0,
            baseRentPerSec = 1400.0,
            upgrades = listOf(
                PropertyUpgrade("pu_4_1", "Fiber Optic Data Center Node", 280000.0, 1.6),
                PropertyUpgrade("pu_4_2", "LEED Platinum Green Certification", 650000.0, 2.1)
            )
        ),
        Property(
            id = "prop_5",
            name = "Logistics Distribution Center",
            category = PropertyCategory.INDUSTRIAL,
            purchasePrice = 4500000.0,
            currentValuation = 4500000.0,
            baseRentPerSec = 5200.0,
            upgrades = listOf(
                PropertyUpgrade("pu_5_1", "Automated High-Bay Pallet Racks", 1000000.0, 1.7),
                PropertyUpgrade("pu_5_2", "Cross-Dock Drone Fleet Landing", 2400000.0, 2.3)
            )
        ),
        Property(
            id = "prop_6",
            name = "Azure Coast Seaside Resort",
            category = PropertyCategory.HOSPITALITY,
            purchasePrice = 18000000.0,
            currentValuation = 18000000.0,
            baseRentPerSec = 22000.0,
            upgrades = listOf(
                PropertyUpgrade("pu_6_1", "Overwater Bungalow Expansion", 4200000.0, 1.8),
                PropertyUpgrade("pu_6_2", "Private Yacht Marina & Coral Lagoon", 9500000.0, 2.4)
            )
        ),
        Property(
            id = "prop_7",
            name = "Metro Financial Center Skyscraper",
            category = PropertyCategory.COMMERCIAL,
            purchasePrice = 65000000.0,
            currentValuation = 65000000.0,
            baseRentPerSec = 82000.0,
            upgrades = listOf(
                PropertyUpgrade("pu_7_1", "High-Speed Maglev Elevators", 15000000.0, 1.7),
                PropertyUpgrade("pu_7_2", "Observation Deck & Heliport", 32000000.0, 2.2)
            )
        ),
        Property(
            id = "prop_8",
            name = "Alpine Peak Luxury Ski Chalet",
            category = PropertyCategory.LUXURY,
            purchasePrice = 140000000.0,
            currentValuation = 140000000.0,
            baseRentPerSec = 185000.0,
            upgrades = listOf(
                PropertyUpgrade("pu_8_1", "Private Heli-Skiing Concession", 32000000.0, 1.8),
                PropertyUpgrade("pu_8_2", "Subterranean Geothermal Spa", 70000000.0, 2.3)
            )
        ),
        Property(
            id = "prop_9",
            name = "Capri Cliffside Villa Estate",
            category = PropertyCategory.LUXURY,
            purchasePrice = 450000000.0,
            currentValuation = 450000000.0,
            baseRentPerSec = 620000.0,
            upgrades = listOf(
                PropertyUpgrade("pu_9_1", "Private Roman Aqueduct Vineyard", 100000000.0, 1.9),
                PropertyUpgrade("pu_9_2", "Deepwater Submarine Drydock", 220000000.0, 2.5)
            )
        ),
        Property(
            id = "prop_10",
            name = "Eden Atoll Private Island",
            category = PropertyCategory.LUXURY,
            purchasePrice = 1500000000.0,
            currentValuation = 1500000000.0,
            baseRentPerSec = 2200000.0,
            upgrades = listOf(
                PropertyUpgrade("pu_10_1", "Private Sovereign Runway & Hangar", 350000000.0, 2.0),
                PropertyUpgrade("pu_10_2", "Autonomous Solar-Desalination Grid", 750000000.0, 2.8)
            )
        )
    )

    fun initialStocks(): List<Stock> = listOf(
        Stock(
            symbol = "APEX",
            name = "Apex Technologies",
            sector = "Tech",
            currentPrice = 142.50,
            previousPrice = 140.00,
            dividendYieldPercent = 1.8,
            priceHistory = listOf(136.0, 138.5, 137.0, 140.0, 142.5)
        ),
        Stock(
            symbol = "NOVA",
            name = "Nova Clean Energy",
            sector = "Energy",
            currentPrice = 64.20,
            previousPrice = 63.80,
            dividendYieldPercent = 3.4,
            priceHistory = listOf(60.0, 62.1, 63.5, 63.8, 64.2)
        ),
        Stock(
            symbol = "TITN",
            name = "Titan Heavy Industries",
            sector = "Industrial",
            currentPrice = 88.75,
            previousPrice = 89.20,
            dividendYieldPercent = 4.2,
            priceHistory = listOf(92.0, 90.5, 91.0, 89.2, 88.75)
        ),
        Stock(
            symbol = "VALT",
            name = "Vault Financial Group",
            sector = "Banking",
            currentPrice = 210.30,
            previousPrice = 208.50,
            dividendYieldPercent = 3.9,
            priceHistory = listOf(202.0, 205.0, 206.5, 208.5, 210.3)
        ),
        Stock(
            symbol = "VITA",
            name = "VitaPharm Biotech",
            sector = "Healthcare",
            currentPrice = 45.10,
            previousPrice = 43.50,
            dividendYieldPercent = 1.2,
            priceHistory = listOf(39.0, 41.5, 42.0, 43.5, 45.1)
        ),
        Stock(
            symbol = "ORBT",
            name = "Orbit Galactic Aerospace",
            sector = "Aerospace",
            currentPrice = 320.00,
            previousPrice = 315.00,
            dividendYieldPercent = 0.8,
            priceHistory = listOf(295.0, 305.0, 310.0, 315.0, 320.0)
        )
    )

    fun initialCryptos(): List<Crypto> = listOf(
        Crypto(
            symbol = "BTF",
            name = "BitFortune",
            currentPrice = 64500.0,
            previousPrice = 63800.0,
            volatility = 0.05,
            sentiment = MarketSentiment.BULLISH,
            priceHistory = listOf(61000.0, 62400.0, 63100.0, 63800.0, 64500.0)
        ),
        Crypto(
            symbol = "ETW",
            name = "EtherWealth",
            currentPrice = 3450.0,
            previousPrice = 3480.0,
            volatility = 0.07,
            sentiment = MarketSentiment.BULLISH,
            priceHistory = listOf(3250.0, 3380.0, 3420.0, 3480.0, 3450.0)
        ),
        Crypto(
            symbol = "SLR",
            name = "Solarium",
            currentPrice = 175.20,
            previousPrice = 168.00,
            volatility = 0.09,
            sentiment = MarketSentiment.VOLATILE,
            priceHistory = listOf(152.0, 160.0, 165.0, 168.0, 175.2)
        ),
        Crypto(
            symbol = "DGG",
            name = "DogeGold",
            currentPrice = 0.42,
            previousPrice = 0.38,
            volatility = 0.15,
            sentiment = MarketSentiment.VOLATILE,
            priceHistory = listOf(0.32, 0.35, 0.37, 0.38, 0.42)
        ),
        Crypto(
            symbol = "QTC",
            name = "QuantumCoin",
            currentPrice = 18.50,
            previousPrice = 19.20,
            volatility = 0.11,
            sentiment = MarketSentiment.BEARISH,
            priceHistory = listOf(22.0, 20.8, 19.9, 19.2, 18.5)
        )
    )

    fun initialCollectibles(): List<Collectible> = listOf(
        // Common & Uncommon
        Collectible(
            id = "col_1",
            name = "Vintage 1968 Swiss Chronograph",
            category = "Watches",
            rarity = Rarity.COMMON,
            purchasePrice = 3500.0,
            currentValuation = 3500.0,
            bonusDescription = "+1% Click Power"
        ),
        Collectible(
            id = "col_2",
            name = "Rare Inverted Jenny Airmail Stamp",
            category = "Antiques",
            rarity = Rarity.COMMON,
            purchasePrice = 8500.0,
            currentValuation = 8500.0,
            bonusDescription = "+1% Real Estate Rent"
        ),
        Collectible(
            id = "col_3",
            name = "18th Century French Grandfather Clock",
            category = "Antiques",
            rarity = Rarity.UNCOMMON,
            purchasePrice = 24000.0,
            currentValuation = 24000.0,
            bonusDescription = "+2% Offline Progression Income"
        ),
        Collectible(
            id = "col_4",
            name = "1963 Shelby Cobra 289 Classic",
            category = "Cars",
            rarity = Rarity.UNCOMMON,
            purchasePrice = 85000.0,
            currentValuation = 85000.0,
            bonusDescription = "+3% Transport Business Income"
        ),

        // Rare
        Collectible(
            id = "col_5",
            name = "Rolex Cosmograph Daytona Rainbow",
            category = "Watches",
            rarity = Rarity.RARE,
            purchasePrice = 250000.0,
            currentValuation = 250000.0,
            bonusDescription = "+4% Casino Luck & Multiplier"
        ),
        Collectible(
            id = "col_6",
            name = "Ferrari 250 GTO Berlinetta",
            category = "Cars",
            rarity = Rarity.RARE,
            purchasePrice = 1200000.0,
            currentValuation = 1200000.0,
            bonusDescription = "+5% Global Net Cash Flow"
        ),
        Collectible(
            id = "col_7",
            name = "Flawless D-Color 10-Carat Diamond Ring",
            category = "Jewelry",
            rarity = Rarity.RARE,
            purchasePrice = 2800000.0,
            currentValuation = 2800000.0,
            bonusDescription = "+5% Net Worth Valuation Boost"
        ),

        // Epic
        Collectible(
            id = "col_8",
            name = "Patek Philippe Grandmaster Chime 6300G",
            category = "Watches",
            rarity = Rarity.EPIC,
            purchasePrice = 6500000.0,
            currentValuation = 6500000.0,
            bonusDescription = "+8% All Property Rental Yield"
        ),
        Collectible(
            id = "col_9",
            name = "Bugatti Chiron Super Sport 300+",
            category = "Cars",
            rarity = Rarity.EPIC,
            purchasePrice = 14000000.0,
            currentValuation = 14000000.0,
            bonusDescription = "+8% All Business Revenue"
        ),
        Collectible(
            id = "col_10",
            name = "Impressionist Monet Water Lilies Oil Canvas",
            category = "Art",
            rarity = Rarity.EPIC,
            purchasePrice = 32000000.0,
            currentValuation = 32000000.0,
            bonusDescription = "+10% Stock Dividend Yields"
        ),

        // Legendary
        Collectible(
            id = "col_11",
            name = "Gulfstream G700 Mach 0.925 Private Jet",
            category = "Aircraft",
            rarity = Rarity.LEGENDARY,
            purchasePrice = 78000000.0,
            currentValuation = 78000000.0,
            bonusDescription = "+12% Global Income & 2x Offline Cap"
        ),
        Collectible(
            id = "col_12",
            name = "120-Meter Luxury Superyacht 'Ocean Sovereign'",
            category = "Yachts",
            rarity = Rarity.LEGENDARY,
            purchasePrice = 280000000.0,
            currentValuation = 280000000.0,
            bonusDescription = "+15% Business Profits & Prestige"
        ),
        Collectible(
            id = "col_13",
            name = "The Oppenheimer Blue 14.6ct Diamond",
            category = "Jewelry",
            rarity = Rarity.LEGENDARY,
            purchasePrice = 620000000.0,
            currentValuation = 620000000.0,
            bonusDescription = "+20% Crypto Staking Return"
        ),

        // Mythical
        Collectible(
            id = "col_14",
            name = "Leonardo da Vinci Salvator Mundi Original",
            category = "Art",
            rarity = Rarity.MYTHICAL,
            purchasePrice = 1800000000.0,
            currentValuation = 1800000000.0,
            bonusDescription = "+25% Global Empire Income Multiplier"
        ),
        Collectible(
            id = "col_15",
            name = "The 100-Ton Sovereign Gold Reserve Bar",
            category = "Gold",
            rarity = Rarity.MYTHICAL,
            purchasePrice = 8500000000.0,
            currentValuation = 8500000000.0,
            bonusDescription = "+30% All Passive Income Streams"
        ),
        Collectible(
            id = "col_16",
            name = "Genesis Block #0 Satoshi Cryptographic NFT",
            category = "NFTs",
            rarity = Rarity.MYTHICAL,
            purchasePrice = 25000000000.0,
            currentValuation = 25000000000.0,
            bonusDescription = "50% Market Multiplier & Immortality"
        )
    )

    fun initialResearch(): List<ResearchNode> = listOf(
        ResearchNode(
            id = "res_1",
            name = "Management Science",
            category = "Business",
            cost = 1000.0,
            effectDescription = "Reduces business operating expenses by 20% across all sectors."
        ),
        ResearchNode(
            id = "res_2",
            name = "Clicking Efficiency",
            category = "Clicker",
            cost = 5000.0,
            effectDescription = "Grants a permanent 2x multiplier to all tap earnings."
        ),
        ResearchNode(
            id = "res_3",
            name = "Business Automation",
            category = "Business",
            cost = 25000.0,
            effectDescription = "Automatically collects accumulated profits from all business wallets.",
            requiredResearchId = "res_1"
        ),
        ResearchNode(
            id = "res_4",
            name = "Remote Operations",
            category = "Empire",
            cost = 120000.0,
            effectDescription = "Increases maximum offline progression earning duration to 12 hours.",
            requiredResearchId = "res_3"
        ),
        ResearchNode(
            id = "res_5",
            name = "Applied Laboratories",
            category = "Tech",
            cost = 500000.0,
            effectDescription = "Unlocks advanced R&D technology tiers and boosts tech revenue by 35%."
        ),
        ResearchNode(
            id = "res_6",
            name = "Property Analytics",
            category = "Real Estate",
            cost = 2000000.0,
            effectDescription = "Halves property condition decay rate and boosts rental income by 25%."
        ),
        ResearchNode(
            id = "res_7",
            name = "Algorithmic Trading",
            category = "Finance",
            cost = 8000000.0,
            effectDescription = "Increases stock dividend payouts by 50% and reveals market trends."
        ),
        ResearchNode(
            id = "res_8",
            name = "Artificial Intelligence",
            category = "Tech",
            cost = 35000000.0,
            effectDescription = "Enhances all automated business operations by +40% net profit.",
            requiredResearchId = "res_5"
        ),
        ResearchNode(
            id = "res_9",
            name = "Electric Mobility",
            category = "Transport",
            cost = 150000000.0,
            effectDescription = "Boosts transport and energy business profitability by 60%."
        ),
        ResearchNode(
            id = "res_10",
            name = "Corporate Learning",
            category = "Management",
            cost = 600000000.0,
            effectDescription = "Cuts employee hiring & training costs by 50% and doubles their efficiency."
        ),
        ResearchNode(
            id = "res_11",
            name = "Quantitative Finance",
            category = "Finance",
            cost = 2500000000.0,
            effectDescription = "Eliminates all trading slippage and maximizes crypto staking yield.",
            requiredResearchId = "res_7"
        ),
        ResearchNode(
            id = "res_12",
            name = "Fusion Energy",
            category = "Future Tech",
            cost = 15000000000.0,
            effectDescription = "Provides limitless clean power, boosting total empire income by +75%!",
            requiredResearchId = "res_8"
        )
    )

    fun initialAchievements(): List<Achievement> = listOf(
        // Wealth
        Achievement("ach_1", "First Dollar", "Generate your very first dollar in Capital Fortune", "Wealth", 0.0, 1.0, rewardCash = 100.0),
        Achievement("ach_2", "Seed Round", "Reach $1,000 in total net worth", "Wealth", 0.0, 1000.0, rewardCash = 500.0),
        Achievement("ach_3", "Six Figures", "Amass $100,000 in net worth", "Wealth", 0.0, 100000.0, rewardCash = 10000.0),
        Achievement("ach_4", "Millionaire Tycoon", "Reach the prestigious $1,000,000 net worth milestone", "Wealth", 0.0, 1000000.0, rewardCash = 100000.0),
        Achievement("ach_5", "Decamillionaire", "Surpass $10,000,000 net worth", "Wealth", 0.0, 10000000.0, rewardCash = 1000000.0),
        Achievement("ach_6", "Billionaire Elite", "Join the three-comma club with $1,000,000,000 net worth", "Wealth", 0.0, 1000000000.0, rewardCash = 50000000.0),
        Achievement("ach_7", "Trillionaire Overlord", "Conquer global capitalism with $1,000,000,000,000", "Wealth", 0.0, 1000000000000.0, rewardCash = 10000000000.0),

        // Business
        Achievement("ach_8", "Open for Business", "Purchase your first commercial enterprise", "Business", 0.0, 1.0, rewardCash = 500.0),
        Achievement("ach_9", "Branching Out", "Own a total of 10 business branches", "Business", 0.0, 10.0, rewardCash = 5000.0),
        Achievement("ach_10", "Job Creator", "Hire 25 employees across your enterprises", "Business", 0.0, 25.0, rewardCash = 25000.0),
        Achievement("ach_11", "Conglomerate", "Own at least one business in all 10 industries", "Business", 0.0, 10.0, rewardCash = 500000.0),
        Achievement("ach_12", "Industrial Giant", "Generate over $100,000/sec in passive business income", "Business", 0.0, 100000.0, rewardCash = 2000000.0),

        // Real Estate
        Achievement("ach_13", "First Landlord Deed", "Acquire your first real estate property", "Real Estate", 0.0, 1.0, rewardCash = 2500.0),
        Achievement("ach_14", "Property Baron", "Own 5 different real estate properties", "Real Estate", 0.0, 5.0, rewardCash = 150000.0),
        Achievement("ach_15", "Pristine Portfolio", "Renovate 5 properties to 100% condition", "Real Estate", 0.0, 5.0, rewardCash = 500000.0),
        Achievement("ach_16", "Island Sovereign", "Acquire the Eden Atoll Private Island", "Real Estate", 0.0, 1.0, rewardCash = 100000000.0),

        // Markets
        Achievement("ach_17", "Wall Street Debut", "Buy shares in any publicly traded company", "Markets", 0.0, 1.0, rewardCash = 1000.0),
        Achievement("ach_18", "Dividend Rain", "Earn $10,000 in passive stock dividends", "Markets", 0.0, 10000.0, rewardCash = 25000.0),
        Achievement("ach_19", "Crypto Whale", "Hold over $500,000 in cryptocurrency assets", "Markets", 0.0, 500000.0, rewardCash = 100000.0),
        Achievement("ach_20", "Diamond Hands", "Earn $50,000 profit on a single crypto or stock trade", "Markets", 0.0, 50000.0, rewardCash = 200000.0),

        // Collectibles & Casino
        Achievement("ach_21", "The Connoisseur", "Acquire your first luxury collectible item", "Luxury", 0.0, 1.0, rewardCash = 5000.0),
        Achievement("ach_22", "High Society", "Own an Epic, Legendary, or Mythical luxury asset", "Luxury", 0.0, 1.0, rewardCash = 1000000.0),
        Achievement("ach_23", "Casino Debut", "Exchange cash for casino chips and play a round", "Casino", 0.0, 1.0, rewardCash = 1000.0),
        Achievement("ach_24", "High Roller", "Win a casino payout of 10,000 chips or more in a single bet", "Casino", 0.0, 10000.0, rewardCash = 500000.0),
        Achievement("ach_25", "Master of Science", "Complete 6 distinct technological research programs", "Research", 0.0, 6.0, rewardCash = 5000000.0)
    )
}
