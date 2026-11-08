package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.engine.LocalCorporationEngine
import com.example.model.GameState
import com.example.ui.components.CurrencyFormatter
import com.example.ui.theme.*

@Composable
fun LocalCorporationScreen(
    state: GameState,
    onFound: (String, String) -> Unit,
    onContribute: (Double) -> Unit,
    onShift: () -> Unit,
    onUpgradeFacility: (String) -> Unit,
    onEstablishDepartment: (String) -> Unit,
    onAcquireBusiness: (String) -> Unit,
    onClaimObjective: (String) -> Unit
) {
    val corp = state.corporation
    var name by remember { mutableStateOf("") }
    var tag by remember { mutableStateOf("") }
    var contribution by remember { mutableStateOf("10000") }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("CORPORATE GROUP", color = PurpleCrypto, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
                Text(if (corp.isFounded) corp.name else "Build a second empire", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Text("A complete offline corporate simulation. All other members shown here are local simulated operators, not connected players.", color = Color(0xFFC9B8D9), style = MaterialTheme.typography.bodySmall)
            }
        }
        if (!corp.isFounded) {
            item {
                CorpCard("Found your corporation", "Corporations unlock at Player Level 10 and require $10,000,000 founding capital.") {
                    OutlinedTextField(name, { name = it }, label = { Text("Corporation name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(tag, { tag = it.uppercase().take(5) }, label = { Text("Ticker / tag (2–5 characters)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Button(onClick = { onFound(name, tag) }, enabled = state.progression.level >= 10 && state.cash >= 10_000_000.0 && name.trim().length >= 3 && tag.length >= 2, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Groups, null); Spacer(Modifier.width(8.dp)); Text("FOUND CORPORATION")
                    }
                    Text("Player level: " + state.progression.level + " / 10     Available capital: " + CurrencyFormatter.format(state.cash), color = Color(0xFFC9B8D9), style = MaterialTheme.typography.labelSmall)
                }
            }
        } else {
            item {
                CorpCard(corp.name + "  [" + corp.tag + "]", "LEVEL " + corp.level + "  •  " + corp.rank) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column { Text("TREASURY", color = Color(0xFFBDA8D2), style = MaterialTheme.typography.labelSmall); Text(CurrencyFormatter.format(corp.treasury), color = GoldLight, fontWeight = FontWeight.ExtraBold) }
                        Column(horizontalAlignment = Alignment.End) { Text("MEMBERS", color = Color(0xFFBDA8D2), style = MaterialTheme.typography.labelSmall); Text(corp.members.size.toString(), color = Color.White, fontWeight = FontWeight.Bold) }
                    }
                    LinearProgressIndicator(progress = { (corp.xp / (1000.0 * Math.pow(1.5, (corp.level - 1).toDouble()))).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                    Text("Corporation XP: " + CurrencyFormatter.format(corp.xp), color = Color(0xFFC9B8D9), style = MaterialTheme.typography.labelSmall)
                }
            }
            item {
                CorpCard("Capital contribution", "Daily contribution cap scales with corporation level.") {
                    OutlinedTextField(contribution, { contribution = it.filter { ch -> ch.isDigit() || ch == '.' } }, label = { Text("Contribution amount") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Button(onClick = { contribution.toDoubleOrNull()?.let(onContribute) }, enabled = (contribution.toDoubleOrNull() ?: 0.0) > 0.0 && (contribution.toDoubleOrNull() ?: Double.MAX_VALUE) <= state.cash, modifier = Modifier.fillMaxWidth()) { Text("CONTRIBUTE TO TREASURY") }
                    Button(onClick = onShift, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = PurpleCrypto)) { Text("COMPLETE MANAGEMENT SHIFT") }
                }
            }
            item { SectionTitle("Corporate objectives") }
            items(corp.objectives, key = { it.id }) { objective ->
                CorpCard(objective.name, objective.description) {
                    LinearProgressIndicator(progress = { (objective.progress / objective.target.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                    Text(CurrencyFormatter.format(objective.progress) + " / " + CurrencyFormatter.format(objective.target), color = Color(0xFFC9B8D9))
                    if (objective.progress >= objective.target && !objective.isClaimed) Button(onClick = { onClaimObjective(objective.id) }, modifier = Modifier.fillMaxWidth()) { Text("CLAIM OBJECTIVE REWARD") }
                    if (objective.isClaimed) Text("COMPLETED", color = EmeraldLight, fontWeight = FontWeight.Bold)
                }
            }
            item { SectionTitle("Facilities") }
            items(LocalCorporationEngine.facilities, key = { it.id }) { facility ->
                val level = corp.facilities[facility.id] ?: 0
                val cost = facility.baseCost * Math.pow(1.9, level.toDouble())
                CorpCard(facility.title + "  •  LVL " + level, facility.description) {
                    Button(onClick = { onUpgradeFacility(facility.id) }, enabled = level < facility.maxLevel && corp.treasury >= cost, modifier = Modifier.fillMaxWidth()) { Text("UPGRADE • " + CurrencyFormatter.format(cost)) }
                }
            }
            item { SectionTitle("Departments & subsidiaries") }
            items(LocalCorporationEngine.departments, key = { it.id }) { department ->
                val built = department.id in corp.departments
                CorpCard(department.title, if (built) "Established department" else "Establish this division to unlock its subsidiaries.") {
                    if (!built) Button(onClick = { onEstablishDepartment(department.id) }, enabled = corp.treasury >= department.cost, modifier = Modifier.fillMaxWidth()) { Text("ESTABLISH • " + CurrencyFormatter.format(department.cost)) }
                }
            }
            items(LocalCorporationEngine.businesses.filter { it.department in corp.departments }, key = { it.id }) { business ->
                val owned = corp.businesses.firstOrNull { it.id == business.id }
                CorpCard(business.title, if (owned == null) "Acquisition cost " + CurrencyFormatter.format(business.cost) else "Revenue " + CurrencyFormatter.format(owned.revenuePerHour) + "/h • expenses " + CurrencyFormatter.format(owned.expensesPerHour) + "/h") {
                    if (owned == null) Button(onClick = { onAcquireBusiness(business.id) }, enabled = corp.treasury >= business.cost, modifier = Modifier.fillMaxWidth()) { Text("ACQUIRE SUBSIDIARY") }
                    else Text("Accumulated subsidiary revenue: " + CurrencyFormatter.format(owned.accumulatedRevenue), color = EmeraldLight)
                }
            }
            item { SectionTitle("Management team") }
            items(corp.members, key = { it.id }) { member ->
                CorpCard(member.name, member.role + " • " + member.rank) {
                    Text(if (member.isSimulatedOperator) "LOCAL SIMULATED OPERATOR" else "PLAYER • DEVICE OWNER", color = if (member.isSimulatedOperator) Color(0xFFBDA8D2) else GoldLight, style = MaterialTheme.typography.labelSmall)
                    Text("Contribution: " + CurrencyFormatter.format(member.totalContribution) + " • Shifts: " + member.shiftsCompleted, color = Color(0xFFC9B8D9), style = MaterialTheme.typography.bodySmall)
                }
            }
            item { SectionTitle("Corporate activity") }
            items(corp.activityLog.take(12)) { entry -> Text("• " + entry, color = Color(0xFFC9B8D9), style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun CorpCard(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(color = DarkSurface, shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Color(0xFFBDA8D2), style = MaterialTheme.typography.bodySmall)
            content()
        }
    }
}

@Composable
private fun SectionTitle(title: String) { Text(title, color = GoldLight, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)) }