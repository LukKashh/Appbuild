package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.engine.OfflineReport
import com.example.ui.components.CurrencyFormatter
import com.example.ui.components.TycoonCard
import com.example.ui.theme.*

@Composable
fun OfflineProgressDialog(
    report: OfflineReport,
    onDismiss: () -> Unit
) {
    val hours = report.elapsedSeconds / 3600
    val minutes = (report.elapsedSeconds % 3600) / 60
    val timeLabel = when {
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m ${report.elapsedSeconds % 60}s"
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = DarkSurface,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(2.dp, GoldPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(GoldContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.HourglassTop,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "WELCOME BACK, TYCOON!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = GoldLight
                )

                Text(
                    text = "Your economic empire prospered while you were away for $timeLabel.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                TycoonCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Business Profits:", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                        Text("+${CurrencyFormatter.format(report.businessEarned)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = EmeraldProfit)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Property Rent:", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                        Text("+${CurrencyFormatter.format(report.rentEarned)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = CyanTech)
                    }
                    Divider(color = DarkCardBorder, modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Accumulated:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text("+${CurrencyFormatter.format(report.totalEarned)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = GoldLight)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("claim_offline_progress_button")
                ) {
                    Text(
                        text = "COLLECT FORTUNE",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
        }
    }
}
