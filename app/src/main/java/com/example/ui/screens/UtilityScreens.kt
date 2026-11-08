package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.engine.LocalSimulationEngine
import com.example.model.GameState
import com.example.model.LocalGamePreferences
import com.example.model.LocalPlayerProfile
import com.example.ui.components.CurrencyFormatter
import com.example.ui.theme.*

@Composable
fun PlayerProfileScreen(state: GameState, onSaveProfile: (LocalPlayerProfile) -> Unit = {}) {
    var username by remember(state.profile.username) { mutableStateOf(state.profile.username) }
    var title by remember(state.profile.title) { mutableStateOf(state.profile.title) }
    var bio by remember(state.profile.bio) { mutableStateOf(state.profile.bio) }
    val progress = (state.progression.xp / LocalSimulationEngine.xpRequired(state.progression.level)).toFloat().coerceIn(0f, 1f)
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Surface(color = Color(0xFF19082B), shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, PurpleCrypto), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(shape = CircleShape, color = PurpleContainer, border = BorderStroke(2.dp, PurpleCrypto), modifier = Modifier.size(84.dp)) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, tint = GoldLight, modifier = Modifier.size(52.dp)) }
                    }
                    Text(username.ifBlank { "Local Player" }, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text(title, color = GoldLight, fontWeight = FontWeight.Bold)
                    Text("PLAYER LEVEL " + state.progression.level, color = Color(0xFFBDA8D2), style = MaterialTheme.typography.labelSmall)
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                    Text(CurrencyFormatter.format(state.progression.xp) + " / " + CurrencyFormatter.format(LocalSimulationEngine.xpRequired(state.progression.level)) + " XP", color = Color(0xFFC9B8D9), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        item {
            Surface(color = DarkSurface, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, DarkCardBorder), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("LOCAL PLAYER IDENTITY", color = GoldLight, fontWeight = FontWeight.Bold)
                    OutlinedTextField(username, { username = it.take(24) }, label = { Text("Player name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(title, { title = it.take(32) }, label = { Text("Player title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(bio, { bio = it.take(120) }, label = { Text("Biography") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                    Button(onClick = { onSaveProfile(state.profile.copy(username = username.trim().ifBlank { "Local Player" }, title = title.trim().ifBlank { "Aspiring Tycoon" }, bio = bio)) }, modifier = Modifier.fillMaxWidth()) { Text("SAVE PROFILE TO DEVICE") }
                }
            }
        }
        item { ProfileStat("Total net worth", CurrencyFormatter.format(state.totalNetWorth), Icons.Default.Diamond) }
        item { ProfileStat("Available cash", CurrencyFormatter.format(state.cash), Icons.Default.AccountBalanceWallet) }
        item { ProfileStat("Net operating cash flow", CurrencyFormatter.format(state.netCashFlowPerSec) + "/s", Icons.Default.TrendingUp) }
        item { ProfileStat("Lifetime earned", CurrencyFormatter.format(state.progression.lifetimeEarned), Icons.Default.Payments) }
        item { ProfileStat("Total clicks", state.progression.totalClicks.toString(), Icons.Default.TouchApp) }
        item { ProfileStat("Owned companies", state.businesses.count { it.isOwned }.toString(), Icons.Default.Business) }
        item { ProfileStat("Owned properties", state.properties.count { it.isOwned }.toString(), Icons.Default.Apartment) }
        item { ProfileStat("Portfolio positions", (state.stocks.count { it.sharesOwned > 0 } + state.cryptos.count { it.coinsOwned > 0 }).toString(), Icons.Default.ShowChart) }
        item { ProfileStat("Collectibles owned", state.collectibles.count { it.isOwned }.toString(), Icons.Default.Diamond) }
        item { ProfileStat("Research completed", state.research.count { it.isUnlocked }.toString(), Icons.Default.Science) }
        item { ProfileStat("Achievements", state.achievements.count { it.isUnlocked }.toString() + " / " + state.achievements.size, Icons.Default.EmojiEvents) }
        item { ProfileStat("Corporation", if (state.corporation.isFounded) state.corporation.name + " • LVL " + state.corporation.level else "Not founded", Icons.Default.Groups) }
    }
}

@Composable
private fun ProfileStat(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(color = DarkSurface, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, DarkCardBorder), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = PurpleCrypto); Spacer(Modifier.width(12.dp))
            Text(title, color = Color.White, modifier = Modifier.weight(1f))
            Text(value, color = GoldLight, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SettingsOverviewScreen(preferences: LocalGamePreferences, onChange: (LocalGamePreferences) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("SETTINGS", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold) }
        item { Text("These preferences are stored in the local game save and work without an account or connection.", color = Color(0xFFC9B8D9), style = MaterialTheme.typography.bodySmall) }
        item { PreferenceRow("Haptic feedback", "Tactile feedback for supported interactions", preferences.hapticsEnabled, { onChange(preferences.copy(hapticsEnabled = it)) }, Icons.Default.Vibration) }
        item { PreferenceRow("Sound effects", "Gameplay and interface sounds", preferences.soundEnabled, { onChange(preferences.copy(soundEnabled = it)) }, Icons.Default.VolumeUp) }
        item { PreferenceInfoRow("Background music", "Unavailable in this build: no music track is bundled.", Icons.Default.MusicNote) }
        item { PreferenceRow("Reduced motion", "Reduce nonessential animation effects", preferences.reducedMotion, { onChange(preferences.copy(reducedMotion = it)) }, Icons.Default.MotionPhotosOff) }
        item { PreferenceRow("Compact numbers", "Use abbreviated large-number formatting", preferences.compactNumbers, { onChange(preferences.copy(compactNumbers = it)) }, Icons.Default.Numbers) }
        item {
            Surface(color = DarkSurface, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, DarkCardBorder), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(15.dp)) {
                    Text("SAVE SYSTEM", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Room database • local device storage • autosaved every five seconds", color = Color(0xFFBDA8D2), style = MaterialTheme.typography.bodySmall)
                    Text("No Firebase dependency • no online account required", color = EmeraldLight, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun PreferenceRow(title: String, subtitle: String, checked: Boolean, onChecked: (Boolean) -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(color = DarkSurface, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, DarkCardBorder), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = PurpleCrypto); Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) { Text(title, color = Color.White, fontWeight = FontWeight.Bold); Text(subtitle, color = Color(0xFFBDA8D2), style = MaterialTheme.typography.bodySmall) }
            Switch(checked = checked, onCheckedChange = onChecked)
        }
    }
}

@Composable
private fun PreferenceInfoRow(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(
        color = DarkSurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, DarkCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color(0xFF8E7BA6))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Color(0xFFBDA8D2), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
