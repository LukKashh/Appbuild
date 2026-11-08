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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.engine.LocalGameViewModel
import com.example.engine.LocalOfflineSummary
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
fun MainScreen(viewModel: LocalGameViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val ready by viewModel.ready.collectAsStateWithLifecycle()
    val offlineSummary by viewModel.offlineSummary.collectAsStateWithLifecycle()
    val lastError by viewModel.lastError.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(MainTab.TAP) }
    var empireSection by remember { mutableStateOf(EmpireSection.BUSINESS) }
    var showMenu by remember { mutableStateOf(false) }
    var utilityScreen by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = utilityScreen != null || selectedTab != MainTab.TAP) {
        if (utilityScreen != null) utilityScreen = null else selectedTab = MainTab.TAP
    }

    Scaffold(
        topBar = {
            LocalPlayerHudBar(
                state = state,
                ready = ready,
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
                            Icon(tab.icon, contentDescription = tab.label, modifier = Modifier.size(if (selected) 25.dp else 22.dp))
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
            if (!lastError.isNullOrBlank()) {
                Surface(
                    color = CrimsonContainer,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(lastError!!, color = Color.White, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        IconButton(onClick = viewModel::dismissError, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, null, tint = Color.White)
                        }
                    }
                }
            }
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
                            )
                        )
                    }
                }
            }

            Box(Modifier.weight(1f)) {
                if (!ready) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            CircularProgressIndicator(color = PurpleCrypto)
                            Text("Loading local game save…", color = Color(0xFFBBA9CD))
                        }
                    }
                } else when {
                    utilityScreen == "Profile" -> ReferenceProfileScreen(state, viewModel)
                    utilityScreen == "Settings" -> ReferenceSettingsScreen(viewModel)
                    utilityScreen == "Achievements" -> ReferenceAchievementsScreen(state, viewModel)
                    utilityScreen == "Statistics" -> ReferenceStatisticsScreen(state, viewModel)
                    utilityScreen == null -> when (selectedTab) {
                        MainTab.ASSETS -> ReferenceAssetsScreen(state, viewModel)
                        MainTab.MARKETS -> ReferenceMarketScreen(state, viewModel)
                        MainTab.TAP -> ReferenceTapScreen(state, viewModel)
                        MainTab.EMPIRE -> when (empireSection) {
                            EmpireSection.BUSINESS -> ReferenceEmpireScreen(state, viewModel, "Business")
                            EmpireSection.REAL_ESTATE -> ReferenceEmpireScreen(state, viewModel, "Real Estate")
                            EmpireSection.RESEARCH -> ReferenceEmpireScreen(state, viewModel, "Research")
                            EmpireSection.LUXURY -> ReferenceEmpireScreen(state, viewModel, "Luxury")
                            EmpireSection.CASINO -> ReferenceCasinoScreen(state, viewModel)
                            EmpireSection.ACHIEVEMENTS -> ReferenceEmpireScreen(state, viewModel, "Awards")
                            EmpireSection.STATISTICS -> ReferenceEmpireScreen(state, viewModel, "Stats")
                        }
                        MainTab.CORP -> ReferenceCorporationScreen(state, viewModel)
                    }
                    else -> ReferenceSettingsScreen(viewModel)
                }
            }
        }
    }

    offlineSummary?.let { summary ->
        LocalOfflineProgressDialog(summary = summary, onDismiss = viewModel::dismissOfflineSummary)
    }
}

@Composable
private fun LocalPlayerHudBar(
    state: com.example.engine.LocalSimulationState,
    ready: Boolean,
    onMenuClick: () -> Unit,
    menuExpanded: Boolean,
    onMenuDismiss: () -> Unit,
    onMenuAction: (String) -> Unit
) {
    Surface(
        color = Color(0xFF12051F),
        shape = RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp),
        border = BorderStroke(1.dp, PurpleCrypto.copy(alpha = 0.8f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = PurpleContainer,
                border = BorderStroke(2.dp, PurpleCrypto),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, null, tint = GoldLight, modifier = Modifier.size(28.dp))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(state.playerName, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                    Surface(color = PurpleContainer, shape = RoundedCornerShape(7.dp)) {
                        Text(
                            "LVL ${state.progress.level}",
                            color = GoldLight,
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
                Text("CLICKER LVL ${state.progress.clickerLevel}", color = Color(0xFFBDA9D3), style = MaterialTheme.typography.labelSmall)
            }
            Surface(
                color = Color(0xFF071B16),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, EmeraldProfit.copy(alpha = 0.8f))
            ) {
                Text(
                    if (ready) hudMoney(state.cash) else "…",
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

private fun hudMoney(value: Double): String {
    val nf = java.text.NumberFormat.getCurrencyInstance(java.util.Locale.US)
    return if (kotlin.math.abs(value) >= 1_000_000_000.0) "$" + String.format(java.util.Locale.US, "%.2fB", value / 1_000_000_000.0)
    else if (kotlin.math.abs(value) >= 1_000_000.0) "$" + String.format(java.util.Locale.US, "%.2fM", value / 1_000_000.0)
    else nf.format(value)
}

@Composable
private fun LocalOfflineProgressDialog(summary: LocalOfflineSummary, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = { Text("Offline progress", color = GoldLight, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("Your local simulation advanced while the game was closed.", color = Color.White)
                Text("Elapsed: ${summary.elapsedMillis / 3_600_000L}h ${(summary.elapsedMillis % 3_600_000L) / 60_000L}m", color = Color(0xFFBBA9CD))
                summary.lines.take(6).forEach { line ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(line.label, color = Color.White, modifier = Modifier.weight(1f))
                        Text(hudMoney(line.net), color = if (line.net >= 0) EmeraldLight else CrimsonLight)
                    }
                }
                Text("Net generated: ${hudMoney(summary.totalNet)}", color = GoldLight, fontWeight = FontWeight.Bold)
                Text("Research points: ${summary.researchPoints}", color = CyanTech)
                if (summary.cappedMillis > 0) Text("Some offline systems reached their cap.", color = CrimsonLight, style = MaterialTheme.typography.labelSmall)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Continue", color = GoldLight) }
        }
    )
}

@Composable
private fun LocalCasinoMigrationNotice() {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Casino, null, tint = PurpleCrypto, modifier = Modifier.size(52.dp))
        Spacer(Modifier.height(14.dp))
        Text("Casino mechanics are the remaining major subsystem to port.", color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "The existing casino screens are intentionally not attached to a separate legacy wallet. This keeps the new local economy consistent while the reference casino rules are migrated.",
            color = Color(0xFFBBA9CD)
        )
    }
}
