package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GameState
import com.example.model.ResearchNode
import com.example.ui.components.CurrencyFormatter
import com.example.ui.components.TycoonCard
import com.example.ui.theme.*

@Composable
fun ResearchScreen(state: GameState, onUnlockResearch: (String) -> Unit, onBuildLab: () -> Unit = {}, onUpgradeSlot: () -> Unit = {}, modifier: Modifier = Modifier) {
    val completed = state.research.count { it.isUnlocked }
    val researching = state.activeResearch.mapNotNull { project -> state.research.firstOrNull { it.id == project.researchId }?.let { it to project } }
    LazyColumn(modifier.fillMaxSize().padding(horizontal = 16.dp), contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            TycoonCard(Modifier.fillMaxWidth(), borderColor = CyanTech.copy(alpha = 0.65f)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("RESEARCH & DEVELOPMENT", color = Color(0xFF9BE8F4), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.2.sp)
                        Text("$completed / ${state.research.size} patents", color = CyanTech, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                        Text("Research points: ${CurrencyFormatter.format(state.researchPoints)}", color = GoldLight, fontWeight = FontWeight.Bold)
                        Text("Research capacity: ${state.activeResearch.size} / ${state.researchSlots}", color = Color(0xFFC9B8D9), style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Default.Science, null, tint = CyanTech, modifier = Modifier.size(38.dp))
                }
                if (!state.researchLabBuilt) {
                    Text("The R&D Laboratory generates research points and unlocks timed projects. Construction requires Player Level 5 and $100,000.", color = Color(0xFFC9B8D9), style = MaterialTheme.typography.bodySmall)
                    Button(onClick = onBuildLab, enabled = state.progression.level >= 5 && state.cash >= 100000.0, modifier = Modifier.fillMaxWidth()) { Text("CONSTRUCT R&D LAB • $100,000") }
                } else {
                    Text("Laboratory operational • 30 research points per hour while the game is active or offline simulation is settled.", color = Color(0xFFC9B8D9), style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = onUpgradeSlot, enabled = state.researchSlots < 4, modifier = Modifier.fillMaxWidth()) { Text("EXPAND PARALLEL RESEARCH SLOTS") }
                }
            }
        }
        if (researching.isNotEmpty()) {
            item { Text("ACTIVE PROJECTS", color = GoldLight, fontWeight = FontWeight.ExtraBold) }
            items(researching, key = { it.first.id }) { pair ->
                val node = pair.first; val project = pair.second
                val remaining = (project.completesAt - System.currentTimeMillis()).coerceAtLeast(0L)
                val duration = (node.durationHours * 3600000.0).toLong().coerceAtLeast(1L)
                val progress = (1.0 - remaining.toDouble() / duration).toFloat().coerceIn(0f, 1f)
                TycoonCard(Modifier.fillMaxWidth(), borderColor = CyanTech.copy(alpha = 0.5f)) {
                    Text(node.name, color = Color.White, fontWeight = FontWeight.Bold)
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                    Text(if (remaining <= 0L) "Completing on next simulation tick" else "Time remaining: ${remaining / 3600000L}h ${(remaining / 60000L) % 60}m", color = Color(0xFFC9B8D9), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item { Text("PATENT CATALOGUE", color = GoldLight, fontWeight = FontWeight.ExtraBold) }
        items(state.research, key = { it.id }) { node ->
            val prereq = node.requiredResearchId?.let { req -> state.research.firstOrNull { it.id == req } }
            val prereqMet = prereq == null || prereq.isUnlocked
            val active = state.activeResearch.any { it.researchId == node.id }
            val canStart = state.researchLabBuilt && !node.isUnlocked && !active && state.cash >= node.cost && state.researchPoints >= node.researchPointsCost && state.progression.level >= node.minimumPlayerLevel && prereqMet && state.activeResearch.size < state.researchSlots
            ResearchCard(node, prereq?.name, prereqMet, state.cash, state.researchPoints, state.progression.level, active, canStart, onUnlock = { onUnlockResearch(node.id) })
        }
    }
}

@Composable
private fun ResearchCard(node: ResearchNode, prereqName: String?, prereqMet: Boolean, playerCash: Double, researchPoints: Double, playerLevel: Int, active: Boolean, canStart: Boolean, onUnlock: () -> Unit) {
    TycoonCard(Modifier.fillMaxWidth(), borderColor = when { node.isUnlocked -> EmeraldProfit.copy(alpha = 0.5f); canStart -> CyanTech.copy(alpha = 0.5f); else -> DarkCardBorder }) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(node.name, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(node.category.uppercase(), color = CyanTech, style = MaterialTheme.typography.labelSmall)
                Text(node.effectDescription, color = Color(0xFFC9B8D9), style = MaterialTheme.typography.bodySmall)
                Text("Duration: ${node.durationHours}h • Level ${node.minimumPlayerLevel}+ • ${CurrencyFormatter.format(node.researchPointsCost)} RP", color = Color(0xFFBDA8D2), style = MaterialTheme.typography.labelSmall)
                if (!prereqMet && prereqName != null) Text("Requires: $prereqName", color = CrimsonLight, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelSmall)
            }
            when { node.isUnlocked -> Text("RESEARCHED", color = EmeraldLight, fontWeight = FontWeight.ExtraBold); active -> Text("IN PROGRESS", color = CyanTech, fontWeight = FontWeight.ExtraBold) }
        }
        if (!node.isUnlocked && !active) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { Text(CurrencyFormatter.format(node.cost), color = GoldLight, fontWeight = FontWeight.Bold); Text("Cash investment", color = Color(0xFFBDA8D2), style = MaterialTheme.typography.labelSmall) }
                Button(onClick = onUnlock, enabled = canStart, modifier = Modifier.testTag("unlock_research_${node.id}")) { Text("START PROJECT") }
            }
            if (playerCash < node.cost || researchPoints < node.researchPointsCost || playerLevel < node.minimumPlayerLevel) Text("Insufficient requirements or laboratory capacity.", color = Color(0xFFBDA8D2), style = MaterialTheme.typography.labelSmall)
        }
    }
}