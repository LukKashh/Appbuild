package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.GameState
import com.example.ui.components.CurrencyFormatter
import com.example.ui.theme.*

@Composable
fun PlayerProfileScreen(state: GameState) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Surface(color = Color(0xFF19082B), shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, PurpleCrypto), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(shape = CircleShape, color = PurpleContainer, border = BorderStroke(2.dp, PurpleCrypto), modifier = Modifier.size(84.dp)) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, tint = GoldLight, modifier = Modifier.size(52.dp)) }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("GameDev", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text("LEVEL ${state.clickLevel}", color = GoldLight, fontWeight = FontWeight.Bold)
                    Text("Local game profile", color = Color(0xFFBDA8D2), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item { ProfileStat("Total Net Worth", CurrencyFormatter.format(state.totalNetWorth), Icons.Default.Diamond) }
        item { ProfileStat("Available Cash", CurrencyFormatter.format(state.cash), Icons.Default.AccountBalanceWallet) }
        item { ProfileStat("Operating Income", "${CurrencyFormatter.format(state.totalIncomePerSec)}/s", Icons.Default.TrendingUp) }
        item { ProfileStat("Owned Businesses", state.businesses.count { it.isOwned }.toString(), Icons.Default.Business) }
        item { ProfileStat("Owned Properties", state.properties.count { it.isOwned }.toString(), Icons.Default.Apartment) }
        item { ProfileStat("Achievements", "${state.achievements.count { it.isUnlocked }} / ${state.achievements.size}", Icons.Default.EmojiEvents) }
    }
}

@Composable
private fun ProfileStat(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(color = DarkSurface, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, DarkCardBorder), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = PurpleCrypto)
            Spacer(Modifier.width(12.dp))
            Text(title, color = Color.White, modifier = Modifier.weight(1f))
            Text(value, color = GoldLight, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun CorporationOverviewScreen(state: GameState) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column {
                Text("CORPORATION", color = PurpleCrypto, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, letterSpacing = 2.dp.value.sp)
                Text("Build together", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Text("The corporation interface is prepared for a connected corporation service. Membership and shared contributions are not yet backed by an online service.", color = Color(0xFFC9B8D9), style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            Surface(color = Color(0xFF1A0A2C), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, PurpleCrypto), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Groups, null, tint = GoldLight, modifier = Modifier.size(48.dp))
                    Text("CORPORATION NETWORK", color = Color.White, fontWeight = FontWeight.Black)
                    Text("ONLINE FEATURES NOT CONNECTED", color = GoldLight, style = MaterialTheme.typography.labelMedium)
                    Text("Your personal empire remains playable offline.", color = Color(0xFFC9B8D9), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item { ProfileStat("Personal net worth", CurrencyFormatter.format(state.totalNetWorth), Icons.Default.Diamond) }
        item { ProfileStat("Businesses available to contribute", state.businesses.count { it.isOwned }.toString(), Icons.Default.Business) }
    }
}

@Composable
fun SettingsOverviewScreen() {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("SETTINGS", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            Text("Local interface preferences", color = Color(0xFFC9B8D9), style = MaterialTheme.typography.bodySmall)
        }
        item { SettingsRow("Gameplay", "Haptics and tap feedback", Icons.Default.TouchApp) }
        item { SettingsRow("Graphics", "Visual effects and animation quality", Icons.Default.Palette) }
        item { SettingsRow("Audio", "Music and sound effects", Icons.Default.VolumeUp) }
        item { SettingsRow("Language", "Language selection", Icons.Default.Language) }
        item { SettingsRow("Support", "Help and game information", Icons.Default.HelpOutline) }
        item {
            Text("Settings controls will be connected to persistent preferences in a separate settings-data pass.", color = Color(0xFFBDA8D2), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SettingsRow(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(color = DarkSurface, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, DarkCardBorder), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = PurpleCrypto)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Color(0xFFBDA8D2), style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Default.ChevronRight, null, tint = GoldLight)
        }
    }
}
