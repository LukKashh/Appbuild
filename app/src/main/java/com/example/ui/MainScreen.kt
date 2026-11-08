package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.engine.GameViewModel
import com.example.ui.components.WealthHeaderBar
import com.example.ui.screens.*
import com.example.ui.theme.*

enum class NavDestination(val label: String, val icon: ImageVector) {
    HOME("Clicker", Icons.Default.TouchApp),
    BUSINESS("Business", Icons.Default.Business),
    ESTATE("Real Estate", Icons.Default.Apartment),
    MARKETS("Markets", Icons.Default.ShowChart),
    LUXURY("Luxury", Icons.Default.Diamond),
    RESEARCH("R&D", Icons.Default.Science),
    CASINO("Casino", Icons.Default.Casino),
    ACHIEVEMENTS("Awards", Icons.Default.EmojiEvents),
    STATS("Stats", Icons.Default.BarChart)
}

@Composable
fun MainScreen(
    viewModel: GameViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val offlineReport by viewModel.offlineReport.collectAsStateWithLifecycle()
    val newsTicker by viewModel.newsTicker.collectAsStateWithLifecycle()

    var currentNav by remember { mutableStateOf(NavDestination.HOME) }

    val hasClaimableAchievements = remember(state.achievements) {
        state.achievements.any { it.isUnlocked && !it.isClaimed }
    }

    // Android Back button handler: return to Home tab if on a sub-tab
    BackHandler(enabled = currentNav != NavDestination.HOME) {
        currentNav = NavDestination.HOME
    }

    Scaffold(
        topBar = {
            WealthHeaderBar(
                state = state,
                onOpenExchange = { currentNav = NavDestination.CASINO },
                modifier = Modifier.statusBarsPadding()
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                contentColor = GoldPrimary,
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("main_navigation_bar")
            ) {
                val primaryNavs = listOf(
                    NavDestination.HOME,
                    NavDestination.BUSINESS,
                    NavDestination.ESTATE,
                    NavDestination.MARKETS,
                    NavDestination.CASINO
                )

                primaryNavs.forEach { nav ->
                    val hasBadge = when (nav) {
                        NavDestination.BUSINESS -> state.availableBusinessWallet > 0
                        NavDestination.ESTATE -> state.availableRentWallet > 0
                        else -> false
                    }

                    NavigationBarItem(
                        selected = currentNav == nav,
                        onClick = { currentNav = nav },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (hasBadge) {
                                        Badge(containerColor = if (nav == NavDestination.BUSINESS) EmeraldProfit else CyanTech)
                                    }
                                }
                            ) {
                                Icon(nav.icon, contentDescription = nav.label)
                            }
                        },
                        label = { Text(nav.label, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = GoldPrimary,
                            indicatorColor = GoldPrimary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Secondary Quick Navigation Bar for Remaining Areas (Luxury, R&D, Awards, Stats)
            ScrollableTabRow(
                selectedTabIndex = NavDestination.values().indexOf(currentNav),
                containerColor = DarkSurfaceVariant,
                contentColor = GoldPrimary,
                edgePadding = 12.dp,
                divider = {}
            ) {
                NavDestination.values().forEach { destination ->
                    val showTabBadge = when (destination) {
                        NavDestination.ACHIEVEMENTS -> hasClaimableAchievements
                        NavDestination.BUSINESS -> state.availableBusinessWallet > 0
                        NavDestination.ESTATE -> state.availableRentWallet > 0
                        else -> false
                    }

                    Tab(
                        selected = currentNav == destination,
                        onClick = { currentNav = destination },
                        text = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (showTabBadge) {
                                            Badge(containerColor = GoldPrimary)
                                        }
                                    }
                                ) {
                                    Icon(destination.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                                Text(destination.label)
                            }
                        },
                        selectedContentColor = GoldPrimary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Screen Content
            Box(modifier = Modifier.weight(1f)) {
                when (currentNav) {
                    NavDestination.HOME -> ClickerScreen(
                        state = state,
                        newsTicker = newsTicker,
                        onTap = viewModel::onTapClicker,
                        onBuyUpgrade = viewModel::buyClickUpgrade
                    )
                    NavDestination.BUSINESS -> BusinessScreen(
                        state = state,
                        onBuyBusiness = viewModel::buyBusiness,
                        onExpandBranch = viewModel::expandBranch,
                        onHireEmployee = viewModel::hireEmployee,
                        onFireEmployee = viewModel::fireEmployee,
                        onBuyUpgrade = viewModel::buyBusinessUpgrade,
                        onCollectWallet = viewModel::collectBusinessWallet,
                        onCollectAll = viewModel::collectAllBusinessWallets,
                        onSellBusiness = viewModel::sellBusiness
                    )
                    NavDestination.ESTATE -> RealEstateScreen(
                        state = state,
                        onBuyProperty = viewModel::buyProperty,
                        onRenovateProperty = viewModel::renovateProperty,
                        onRenovateAll = viewModel::renovateAllProperties,
                        onBuyUpgrade = viewModel::buyPropertyUpgrade,
                        onCollectRent = viewModel::collectPropertyRent,
                        onCollectAllRent = viewModel::collectAllPropertyRent,
                        onSellProperty = viewModel::sellProperty
                    )
                    NavDestination.MARKETS -> MarketScreen(
                        state = state,
                        onBuyStock = viewModel::buyStock,
                        onSellStock = viewModel::sellStock,
                        onBuyCrypto = viewModel::buyCrypto,
                        onSellCrypto = viewModel::sellCrypto
                    )
                    NavDestination.LUXURY -> CollectiblesScreen(
                        state = state,
                        onBuyCollectible = viewModel::buyCollectible,
                        onSellCollectible = viewModel::sellCollectible
                    )
                    NavDestination.RESEARCH -> ResearchScreen(
                        state = state,
                        onUnlockResearch = viewModel::unlockResearch
                    )
                    NavDestination.CASINO -> CasinoScreen(
                        state = state,
                        onBuyChips = viewModel::buyChips,
                        onCashOutChips = viewModel::cashOutChips,
                        onRecordResult = viewModel::recordCasinoResult
                    )
                    NavDestination.ACHIEVEMENTS -> AchievementsScreen(
                        state = state,
                        onClaimReward = viewModel::claimAchievement
                    )
                    NavDestination.STATS -> StatisticsScreen(
                        state = state,
                        onResetGame = viewModel::resetGame
                    )
                }
            }
        }
    }

    // Offline progress dialog
    offlineReport?.let { report ->
        OfflineProgressDialog(
            report = report,
            onDismiss = viewModel::dismissOfflineReport
        )
    }
}
