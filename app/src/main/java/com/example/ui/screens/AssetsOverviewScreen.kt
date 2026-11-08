package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.sp
import com.example.model.GameState
import com.example.ui.components.CurrencyFormatter
import com.example.ui.theme.*

@Composable
fun AssetsOverviewScreen(state: GameState, modifier: Modifier = Modifier) {
    val businesses = state.businesses.filter { it.isOwned }.sumOf { it.valuation + it.accumulatedWallet }
    val properties = state.properties.filter { it.isOwned }.sumOf { it.currentValuation + it.accumulatedRent - it.maintenanceDebt }
    val stocks = state.stocks.sumOf { it.holdingsValue }
    val crypto = state.cryptos.sumOf { it.holdingsValue }
    val collectibles = state.collectibles.filter { it.isOwned }.sumOf { it.currentValuation }
    val chips = state.casinoChips * 100.0

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 14.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text("ASSETS", color = PurpleCrypto, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                Text("Your portfolio", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Text("A consolidated view of your empire's value.", color = Color(0xFFC8B4DD), style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            Surface(
                color = Color(0xFF19082B),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, PurpleCrypto),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("TOTAL NET WORTH", color = Color(0xFFCBB8E2), style = MaterialTheme.typography.labelMedium, letterSpacing = 1.3.sp)
                    Text(CurrencyFormatter.format(state.totalNetWorth), color = GoldLight, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text("+${CurrencyFormatter.format(state.netCashFlowPerSec)} / sec net operating flow", color = EmeraldLight, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item { AssetLine("Available Cash", state.cash, Icons.Default.AccountBalanceWallet, EmeraldLight) }
        item { AssetLine("Reserved Trading Funds", state.reservedCash, Icons.Default.Lock, GoldPrimary) }
        item { AssetLine("Businesses", businesses, Icons.Default.Business, PurpleCrypto) }
        item { AssetLine("Real Estate", properties, Icons.Default.Apartment, GoldLight) }
        item { AssetLine("Stocks", stocks, Icons.Default.ShowChart, EmeraldProfit) }
        item { AssetLine("Crypto", crypto, Icons.Default.CurrencyBitcoin, PurpleCrypto) }
        item { AssetLine("Collectibles", collectibles, Icons.Default.Diamond, GoldLight) }
        item { AssetLine("Casino Chips", chips, Icons.Default.Casino, GoldPrimary) }
        if (state.corporation.isFounded) item { AssetLine("Corporation Treasury (separate)", state.corporation.treasury, Icons.Default.Groups, PurpleCrypto) }
        item {
            Surface(color = DarkSurface, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, DarkCardBorder), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("ASSET COUNTS", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Text("Businesses: ${state.businesses.count { it.isOwned }}   •   Properties: ${state.properties.count { it.isOwned }}", color = Color(0xFFD3C3E4))
                    Text("Stocks held: ${state.stocks.count { it.sharesOwned > 0 }}   •   Crypto assets: ${state.cryptos.count { it.coinsOwned > 0 }}", color = Color(0xFFD3C3E4))
                    Text("Collectibles: ${state.collectibles.count { it.isOwned }}", color = Color(0xFFD3C3E4))
                    Text("Reserved cash: ${CurrencyFormatter.format(state.reservedCash)}", color = Color(0xFFD3C3E4))
                    Text("Business liabilities: ${CurrencyFormatter.format(state.businesses.filter { it.isOwned }.sumOf { it.liabilities })}", color = Color(0xFFD3C3E4))
                    Text("Property maintenance arrears: ${CurrencyFormatter.format(state.properties.filter { it.isOwned }.sumOf { it.maintenanceDebt })}", color = Color(0xFFD3C3E4))
                }
            }
        }
    }
}

@Composable
private fun AssetLine(label: String, amount: Double, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color) {
    Surface(
        color = Color(0xFF160824),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, DarkCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(25.dp))
            Spacer(Modifier.width(12.dp))
            Text(label, color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(CurrencyFormatter.format(amount), color = tint, fontWeight = FontWeight.Bold)
        }
    }
}
