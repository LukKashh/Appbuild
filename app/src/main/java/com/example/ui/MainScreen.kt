package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.engine.GameViewModel
import com.example.model.GameState
import com.example.ui.components.CurrencyFormatter
import com.example.ui.screens.*
import com.example.ui.theme.*

private enum class MainTab(val label: String, val icon: ImageVector) {
    ASSETS("Assets", Icons.Default.Diamond),
    MARKETS("Markets", Icons.Default.ShowChart),
    TAP("Tap", Icons.Default.TouchApp),
    EMPIRE("Empire", Icons.Default.Business),
    CORP("Corp", Icons.Default.Groups)
}

private enum class EmpireSection(val label: String, val icon: ImageVector) {
    BUSINESS("Business", Icons.Default.Business),
    REAL_ESTATE("Real Estate", Icons.Default.Apartment),
    RESEARCH("Research", Icons.Default.Science),
    LUXURY("Luxury", Icons.Default.Diamond),
    CASINO("Casino", Icons.Default.Casino),
    ACHIEVEMENTS("Awards", Icons.Default.EmojiEvents),
    STATISTICS("Stats", Icons.Default.BarChart)
}

@Composable
fun MainScreen(viewModel: GameViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val offlineReport by viewModel.offlineReport.collectAsStateWithLifecycle()
    val newsTicker by viewModel.newsTicker.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(MainTab.TAP) }
    var empireSection by remember { mutableStateOf(EmpireSection.BUSINESS) }
    var showMenu by remember { mutableStateOf(false) }
    var utilityScreen by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = utilityScreen != null || selectedTab != MainTab.TAP) {
        if (utilityScreen != null) utilityScreen = null else selectedTab = MainTab.TAP
    }

    Scaffold(
        topBar = {
            PlayerHudBar(
                state = state,
                onMenuClick = { showMenu = true },
                menuExpanded = showMenu,
                onMenuDismiss = { showMenu = false },
                onMenuAction = { action ->
                    showMenu = false
                    utilityScreen = action
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF10051F),
                contentColor = PurpleCrypto,
                tonalElevation = 0.dp,
                modifier = Modifier.navigationBarsPadding().testTag("main_navigation_bar")
            ) {
                MainTab.values().forEach { tab ->
                    val selected = selectedTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            utilityScreen = null
                            selectedTab = tab
                        },
                        icon = {
                            Icon(
                                tab.icon,
                                contentDescription = tab.label,
                                modifier = Modifier.size(if (selected) 25.dp else 22.dp)
                            )
                        },
                        label = { Text(tab.label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GoldLight,
                            selectedTextColor = GoldLight,
                            indicatorColor = PurpleContainer,
                            unselectedIconColor = Color(0xFFBBA8D2),
                            unselectedTextColor = Color(0xFFBBA8D2)
                        )
                    )
                }
            }
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (utilityScreen == null && selectedTab == MainTab.EMPIRE) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    EmpireSection.values().forEach { section ->
                        FilterChip(
                            selected = empireSection == section,
                            onClick = { empireSection = section },
                            label = { Text(section.label) },
                            leadingIcon = { Icon(section.icon, null, Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PurpleContainer,
                                selectedLabelColor = GoldLight,
                                selectedLeadingIconColor = GoldLight
                            ),
                        )
                    }
                }
            }

            Box(Modifier.weight(1f)) {
                when {
                    utilityScreen == "Profile" -> PlayerProfileScreen(state, viewModel::updatePlayerProfile)
                    utilityScreen == "Settings" -> SettingsOverviewScreen(state.preferences, viewModel::updateGamePreferences)
                    utilityScreen == "Achievements" -> AchievementsScreen(state, viewModel::claimAchievement)
                    utilityScreen == "Statistics" -> StatisticsScreen(state, viewModel::resetGame)
                    utilityScreen == null -> when (selectedTab) {
                        MainTab.ASSETS -> AssetsOverviewScreen(state)
                        MainTab.MARKETS -> MarketScreen(
                            state = state,
                            onBuyStock = viewModel::buyStock,
                            onSellStock = viewModel::sellStock,
                            onBuyCrypto = viewModel::buyCrypto,
                            onSellCrypto = viewModel::sellCrypto,
                            onPlaceMarketOrder = viewModel::placeMarketOrder,
                            onCancelMarketOrder = viewModel::cancelMarketOrder
                        )
                        MainTab.TAP -> ClickerScreen(
                            state = state,
                            newsTicker = newsTicker,
                            onTap = viewModel::onTapClicker,
                            onBuyUpgrade = viewModel::buyClickUpgrade
                        )
                        MainTab.EMPIRE -> when (empireSection) {
                            EmpireSection.BUSINESS -> BusinessScreen(
                                state = state,
                                onBuyBusiness = viewModel::buyBusiness,
                                onExpandBranch = viewModel::expandBranch,
                                onHireEmployee = viewModel::hireEmployee,
                                onFireEmployee = viewModel::fireEmployee,
                                onBuyUpgrade = viewModel::buyBusinessUpgrade,
                                onCollectWallet = viewModel::collectBusinessWallet,
                                onCollectAll = viewModel::collectAllBusinessWallets,
                                onSellBusiness = viewModel::sellBusiness,
                                onToggleSuspension = viewModel::toggleBusinessSuspension
                            )
                            EmpireSection.REAL_ESTATE -> RealEstateScreen(
                                state = state,
                                onBuyProperty = viewModel::buyProperty,
                                onRenovateProperty = viewModel::renovateProperty,
                                onRenovateAll = viewModel::renovateAllProperties,
                                onBuyUpgrade = viewModel::buyPropertyUpgrade,
                                onCollectRent = viewModel::collectPropertyRent,
                                onCollectAllRent = viewModel::collectAllPropertyRent,
                                onSellProperty = viewModel::sellProperty
                            )
                            EmpireSection.RESEARCH -> ResearchScreen(state, viewModel::unlockResearch, viewModel::buildResearchLab, viewModel::upgradeResearchSlot)
                            EmpireSection.LUXURY -> CollectiblesScreen(state, viewModel::buyCollectible, viewModel::sellCollectible)
                            EmpireSection.CASINO -> CasinoScreen(state, viewModel::buyChips, viewModel::cashOutChips, viewModel::recordCasinoResult)
                            EmpireSection.ACHIEVEMENTS -> AchievementsScreen(state, viewModel::claimAchievement)
                            EmpireSection.STATISTICS -> StatisticsScreen(state, viewModel::resetGame)
                        }
                        MainTab.CORP -> LocalCorporationScreen(
                            state = state,
                            onFound = viewModel::foundCorporation,
                            onContribute = viewModel::contributeToCorporation,
                            onShift = viewModel::corporationManagementShift,
                            onUpgradeFacility = viewModel::upgradeCorporationFacility,
                            onEstablishDepartment = viewModel::establishCorporationDepartment,
                            onAcquireBusiness = viewModel::acquireCorporationBusiness,
                            onClaimObjective = viewModel::claimCorporationObjective
                        )
                    }
                    else -> TapSettingsFallback()
                }
            }
        }
    }

    offlineReport?.let { report ->
        OfflineProgressDialog(report = report, onDismiss = viewModel::dismissOfflineReport)
    }
}

@Composable
private fun PlayerHudBar(
    state: GameState,
    onMenuClick: () -> Unit,
    menuExpanded: Boolean,
    onMenuDismiss: () -> Unit,
    onMenuAction: (String) -> Unit
) {
    Surface(
        color = Color(0xFF12051F),
        shape = RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp),
        border = BorderStroke(1.dp, PurpleCrypto.copy(alpha = 0.8f)),
        modifier = Modifier.fillMaxWidth().statusBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = PurpleContainer,
                border = BorderStroke(2.dp, PurpleCrypto),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, null, tint = GoldLight, modifier = Modifier.size(30.dp))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("GameDev", color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                    Surface(color = PurpleContainer, shape = RoundedCornerShape(7.dp)) {
                        Text("LVL ${state.clickLevel}", color = GoldLight, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
                    }
                }
                Text("TAP LEVEL", color = Color(0xFFBDA9D3), style = MaterialTheme.typography.labelSmall)
            }
            Surface(
                color = Color(0xFF071B16),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, EmeraldProfit.copy(alpha = 0.8f))
            ) {
                Text(
                    CurrencyFormatter.format(state.cash),
                    color = EmeraldLight,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 11.dp)
                )
            }
            Box {
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Default.Menu, "Open menu", tint = PurpleCrypto, modifier = Modifier.size(28.dp))
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = onMenuDismiss) {
                    listOf("Profile", "Achievements", "Statistics", "Settings").forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item) },
                            leadingIcon = {
                                Icon(
                                    when (item) {
                                        "Profile" -> Icons.Default.Person
                                        "Achievements" -> Icons.Default.EmojiEvents
                                        "Statistics" -> Icons.Default.BarChart
                                        else -> Icons.Default.Settings
                                    },
                                    null
                                )
                            },
                            onClick = { onMenuAction(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TapSettingsFallback() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Select a section from the menu.", color = Color.White)
    }
}
