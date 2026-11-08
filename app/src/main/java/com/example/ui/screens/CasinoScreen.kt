package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GameState
import com.example.model.PlayingCard
import com.example.ui.components.CurrencyFormatter
import com.example.ui.components.TycoonCard
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

fun createRandomPlayingCard(): PlayingCard {
    val suits = listOf("♠", "♥", "♦", "♣")
    val ranks = listOf("A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K")
    val r = ranks.random()
    val s = suits.random()
    val v = when (r) {
        "A" -> 1
        "K", "Q", "J", "10" -> 10
        else -> r.toInt()
    }
    return PlayingCard(r, s, v)
}

@Composable
fun CasinoScreen(
    state: GameState,
    onBuyChips: (Double) -> Unit,
    onCashOutChips: (Long) -> Unit,
    onRecordResult: (Long, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedGameTab by remember { mutableStateOf(0) } // 0: Roulette, 1: Blackjack, 2: Mines, 3: Keno, 4: Hi-Lo
    var betAmount by remember { mutableStateOf(10L) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Exchange Cashier Counter
        item {
            TycoonCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = ChipGold.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ROYAL CASINO CASHIER",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${CurrencyFormatter.formatChips(state.casinoChips)} Chips 🪙",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = ChipGold
                        )
                        Text(
                            text = "1 Chip = $100 Cash",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Stats: ${state.casinoStats.gamesPlayed} Played",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Won: ${CurrencyFormatter.formatChips(state.casinoStats.totalWonChips)} 🪙",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldLight
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onBuyChips(1000.0) },
                        enabled = state.cash >= 1000.0,
                        colors = ButtonDefaults.buttonColors(containerColor = ChipGold, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+10 Chips", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { onBuyChips(10000.0) },
                        enabled = state.cash >= 10000.0,
                        colors = ButtonDefaults.buttonColors(containerColor = ChipGold, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+100 Chips", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { onCashOutChips(state.casinoChips) },
                        enabled = state.casinoChips > 0,
                        border = BorderStroke(1.dp, EmeraldProfit),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldLight),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Text("CASH OUT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Minigame Tabs
        item {
            ScrollableTabRow(
                selectedTabIndex = selectedGameTab,
                containerColor = DarkSurface,
                contentColor = ChipGold,
                edgePadding = 0.dp
            ) {
                listOf("Roulette", "Blackjack", "Mines", "Keno", "Hi-Lo").forEachIndexed { index, name ->
                    Tab(
                        selected = selectedGameTab == index,
                        onClick = { selectedGameTab = index },
                        text = {
                            Text(
                                name,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedGameTab == index) ChipGold else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }
        }

        // Bet Selector
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "BET: $betAmount CHIPS",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(5L, 10L, 50L, 250L, 1000L).forEach { amount ->
                        FilterChip(
                            selected = betAmount == amount,
                            onClick = { betAmount = amount },
                            label = { Text("$amount") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ChipGold,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
            }
        }

        // Game Container
        item {
            when (selectedGameTab) {
                0 -> RouletteGame(
                    chips = state.casinoChips,
                    betAmount = betAmount,
                    onResult = onRecordResult
                )
                1 -> BlackjackGame(
                    chips = state.casinoChips,
                    betAmount = betAmount,
                    onResult = onRecordResult
                )
                2 -> MinesGame(
                    chips = state.casinoChips,
                    betAmount = betAmount,
                    onResult = onRecordResult
                )
                3 -> KenoGame(
                    chips = state.casinoChips,
                    betAmount = betAmount,
                    onResult = onRecordResult
                )
                4 -> HiLoGame(
                    chips = state.casinoChips,
                    betAmount = betAmount,
                    onResult = onRecordResult
                )
            }
        }
    }
}

// -------------------------------------------------------------------------
// 1. ROULETTE GAME WITH ANIMATED WHEEL
// -------------------------------------------------------------------------
@Composable
fun RouletteGame(
    chips: Long,
    betAmount: Long,
    onResult: (Long, Long) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedBetType by remember { mutableStateOf("RED") } // RED, BLACK, EVEN, ODD, 1-18, 19-36, ZERO
    var resultNumber by remember { mutableStateOf<Int?>(null) }
    var displayedSpinNumber by remember { mutableStateOf(0) }
    var isSpinning by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf("") }

    val redNumbers = setOf(1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36)

    TycoonCard(modifier = Modifier.fillMaxWidth()) {
        Text("EUROPEAN ROULETTE", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(10.dp))

        // Outcome Display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (isSpinning) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(GoldPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("$displayedSpinNumber", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                    }
                    Text("SPINNING WHEEL...", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = GoldLight)
                }
            } else if (resultNumber != null) {
                val num = resultNumber!!
                val color = if (num == 0) EmeraldProfit else if (num in redNumbers) CrimsonLoss else Color.Black
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(color),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("$num", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                    Text(resultMessage, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
                }
            } else {
                Text("Select your bet option and spin!", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Betting Options Row 1
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("RED", "BLACK", "EVEN", "ODD").forEach { type ->
                FilterChip(
                    selected = selectedBetType == type,
                    onClick = { selectedBetType = type },
                    label = { Text(type) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = if (type == "RED") CrimsonLoss else if (type == "BLACK") Color.Black else ChipGold,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Betting Options Row 2
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("1-18", "19-36", "ZERO (36x)").forEach { type ->
                FilterChip(
                    selected = selectedBetType == type,
                    onClick = { selectedBetType = type },
                    label = { Text(type) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = if (type.startsWith("ZERO")) EmeraldProfit else ChipGold,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = {
                if (chips < betAmount || isSpinning) return@Button
                coroutineScope.launch {
                    isSpinning = true
                    resultMessage = ""
                    // Animated spinning ticker
                    repeat(12) {
                        displayedSpinNumber = Random.nextInt(0, 37)
                        delay(70L)
                    }
                    val outcome = Random.nextInt(0, 37)
                    resultNumber = outcome
                    isSpinning = false

                    val isRed = outcome in redNumbers
                    val isBlack = outcome != 0 && !isRed
                    val isEven = outcome != 0 && outcome % 2 == 0
                    val isOdd = outcome != 0 && outcome % 2 != 0
                    val isLow = outcome in 1..18
                    val isHigh = outcome in 19..36

                    val won = when (selectedBetType) {
                        "RED" -> isRed
                        "BLACK" -> isBlack
                        "EVEN" -> isEven
                        "ODD" -> isOdd
                        "1-18" -> isLow
                        "19-36" -> isHigh
                        "ZERO (36x)" -> outcome == 0
                        else -> false
                    }

                    val multiplier = if (selectedBetType.startsWith("ZERO")) 36L else 2L
                    if (won) {
                        val winnings = betAmount * multiplier
                        resultMessage = "WINNER! +$winnings Chips!"
                        onResult(winnings, betAmount)
                    } else {
                        resultMessage = "House wins! -$betAmount Chips"
                        onResult(0L, betAmount)
                    }
                }
            },
            enabled = chips >= betAmount && !isSpinning,
            colors = ButtonDefaults.buttonColors(containerColor = ChipGold, contentColor = Color.Black),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("spin_roulette_button")
        ) {
            Text(if (isSpinning) "SPINNING..." else "SPIN ROULETTE (${betAmount} CHIPS)", fontWeight = FontWeight.ExtraBold)
        }
    }
}

// -------------------------------------------------------------------------
// 2. BLACKJACK GAME WITH REAL CARDS & DOUBLE DOWN
// -------------------------------------------------------------------------
@Composable
fun BlackjackGame(
    chips: Long,
    betAmount: Long,
    onResult: (Long, Long) -> Unit
) {
    var playerCards by remember { mutableStateOf<List<PlayingCard>>(emptyList()) }
    var dealerCards by remember { mutableStateOf<List<PlayingCard>>(emptyList()) }
    var currentBet by remember { mutableStateOf(betAmount) }
    var gameState by remember { mutableStateOf("IDLE") } // IDLE, PLAYING, FINISHED
    var resultText by remember { mutableStateOf("") }

    fun score(cards: List<PlayingCard>): Int {
        var sum = 0
        var aces = 0
        for (c in cards) {
            if (c.rank == "A") aces++ else sum += c.value
        }
        for (i in 0 until aces) {
            if (sum + 11 + (aces - 1 - i) <= 21) sum += 11 else sum += 1
        }
        return sum
    }

    TycoonCard(modifier = Modifier.fillMaxWidth()) {
        Text("VEGAS STRIP BLACKJACK", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(10.dp))

        // Felt Table
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0D331E))
                .padding(14.dp)
        ) {
            // Dealer
            Text("DEALER HAND (${if (gameState == "PLAYING") "?" else score(dealerCards)})", style = MaterialTheme.typography.labelSmall, color = GoldLight)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                dealerCards.forEachIndexed { i, c ->
                    if (i == 1 && gameState == "PLAYING") {
                        CardBack()
                    } else {
                        PlayingCardView(c)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Player
            Text("YOUR HAND (${score(playerCards)})", style = MaterialTheme.typography.labelSmall, color = GoldLight)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                playerCards.forEach { c ->
                    PlayingCardView(c)
                }
            }

            if (resultText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(resultText, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = GoldLight)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (gameState != "PLAYING") {
            Button(
                onClick = {
                    if (chips < betAmount) return@Button
                    currentBet = betAmount
                    playerCards = listOf(createRandomPlayingCard(), createRandomPlayingCard())
                    dealerCards = listOf(createRandomPlayingCard(), createRandomPlayingCard())
                    gameState = "PLAYING"
                    resultText = ""

                    if (score(playerCards) == 21) {
                        gameState = "FINISHED"
                        val win = (currentBet * 2.5).toLong()
                        resultText = "NATURAL BLACKJACK! +$win Chips!"
                        onResult(win, currentBet)
                    }
                },
                enabled = chips >= betAmount,
                colors = ButtonDefaults.buttonColors(containerColor = ChipGold, contentColor = Color.Black),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("DEAL HAND (${betAmount} CHIPS)", fontWeight = FontWeight.Bold)
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = {
                        val next = (playerCards + createRandomPlayingCard())
                        playerCards = next
                        if (score(next) > 21) {
                            gameState = "FINISHED"
                            resultText = "BUST! You went over 21."
                            onResult(0L, currentBet)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldProfit, contentColor = Color.Black),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("HIT")
                }

                // Double Down (available on first 2 cards)
                if (playerCards.size == 2 && chips >= currentBet * 2) {
                    Button(
                        onClick = {
                            currentBet *= 2
                            val next = (playerCards + createRandomPlayingCard())
                            playerCards = next
                            // Draw 1 card then stand
                            resolveDealerTurn(playerCards, dealerCards, currentBet) { dCards, res, win ->
                                dealerCards = dCards
                                resultText = res
                                gameState = "FINISHED"
                                onResult(win, currentBet)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanTech, contentColor = Color.Black),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Text("2X DOUBLE")
                    }
                }

                Button(
                    onClick = {
                        resolveDealerTurn(playerCards, dealerCards, currentBet) { dCards, res, win ->
                            dealerCards = dCards
                            resultText = res
                            gameState = "FINISHED"
                            onResult(win, currentBet)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("STAND")
                }
            }
        }
    }
}

private fun resolveDealerTurn(
    playerHand: List<PlayingCard>,
    dealerHand: List<PlayingCard>,
    bet: Long,
    onFinish: (List<PlayingCard>, String, Long) -> Unit
) {
    fun scoreHand(cards: List<PlayingCard>): Int {
        var sum = 0
        var aces = 0
        for (c in cards) {
            if (c.rank == "A") aces++ else sum += c.value
        }
        for (i in 0 until aces) {
            if (sum + 11 + (aces - 1 - i) <= 21) sum += 11 else sum += 1
        }
        return sum
    }

    val dList = dealerHand.toMutableList()
    while (scoreHand(dList) < 17) {
        dList.add(createRandomPlayingCard())
    }
    val dScore = scoreHand(dList)
    val pScore = scoreHand(playerHand)

    when {
        pScore > 21 -> onFinish(dList, "Bust! -$bet Chips", 0L)
        dScore > 21 -> onFinish(dList, "Dealer Busts! You WIN +${bet * 2} Chips!", bet * 2)
        pScore > dScore -> onFinish(dList, "You WIN! $pScore vs $dScore (+${bet * 2} Chips)", bet * 2)
        pScore == dScore -> onFinish(dList, "PUSH! Refunded $bet Chips", bet)
        else -> onFinish(dList, "Dealer Wins: $dScore vs $pScore", 0L)
    }
}

@Composable
fun PlayingCardView(card: PlayingCard) {
    Surface(
        color = Color(0xFFFAF9F6),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, Color(0xFFD4D4D4)),
        modifier = Modifier.size(width = 44.dp, height = 62.dp)
    ) {
        Column(
            modifier = Modifier.padding(4.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = card.rank,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = if (card.isRed) CrimsonLoss else Color.Black
            )
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = card.suit,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (card.isRed) CrimsonLoss else Color.Black
                )
            }
            Text(
                text = card.rank,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = if (card.isRed) CrimsonLoss else Color.Black,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
fun CardBack() {
    Surface(
        color = Color(0xFF8B0000),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, Color.White),
        modifier = Modifier.size(width = 44.dp, height = 62.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text("🂠", fontSize = 24.sp, color = GoldLight)
        }
    }
}

// -------------------------------------------------------------------------
// 3. MINES GAME (5x5) WITH FULL REVEAL ON END
// -------------------------------------------------------------------------
@Composable
fun MinesGame(
    chips: Long,
    betAmount: Long,
    onResult: (Long, Long) -> Unit
) {
    var mineCount by remember { mutableStateOf(3) }
    var inGame by remember { mutableStateOf(false) }
    var minePositions by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var uncoveredTiles by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var explodedTile by remember { mutableStateOf<Int?>(null) }
    var currentMultiplier by remember { mutableStateOf(1.0) }
    var statusMessage by remember { mutableStateOf("") }

    TycoonCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("DIAMOND MINES (5x5)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Mines: $mineCount", style = MaterialTheme.typography.titleSmall, color = CrimsonLight, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Multiplier & Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Multiplier: ${String.format("%.2f", currentMultiplier)}x",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = EmeraldLight
            )
            if (statusMessage.isNotEmpty()) {
                Text(statusMessage, style = MaterialTheme.typography.bodySmall, color = GoldLight)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 5x5 Grid
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (r in 0 until 5) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (c in 0 until 5) {
                        val index = r * 5 + c
                        val isUncovered = index in uncoveredTiles
                        val isMine = index in minePositions
                        val isExploded = index == explodedTile

                        Surface(
                            color = when {
                                isExploded -> CrimsonLoss
                                isUncovered -> EmeraldProfit.copy(alpha = 0.85f)
                                !inGame && isMine -> CrimsonContainer
                                !inGame && minePositions.isNotEmpty() && !isMine -> DarkSurfaceVariant.copy(alpha = 0.5f)
                                else -> DarkSurfaceVariant
                            },
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clickable(enabled = inGame && !isUncovered) {
                                    if (isMine) {
                                        explodedTile = index
                                        inGame = false
                                        statusMessage = "MINE EXPLODED! Lost bet."
                                        onResult(0L, betAmount)
                                    } else {
                                        val nextSet = uncoveredTiles + index
                                        uncoveredTiles = nextSet
                                        currentMultiplier += 0.25 * (mineCount / 3.0)
                                        statusMessage = "Gem found! +${String.format("%.2f", currentMultiplier)}x"
                                    }
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isUncovered) {
                                    Text("💎", fontSize = 16.sp)
                                } else if (isExploded || (!inGame && isMine)) {
                                    Text("💣", fontSize = 16.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (!inGame) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(2, 3, 5, 8).forEach { m ->
                    FilterChip(
                        selected = mineCount == m,
                        onClick = { mineCount = m },
                        label = { Text("$m Mines") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CrimsonLoss, selectedLabelColor = Color.White),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    if (chips < betAmount) return@Button
                    val mines = mutableSetOf<Int>()
                    while (mines.size < mineCount) {
                        mines.add(Random.nextInt(0, 25))
                    }
                    minePositions = mines
                    uncoveredTiles = emptySet()
                    explodedTile = null
                    currentMultiplier = 1.0
                    inGame = true
                    statusMessage = "Tap tiles to uncover gems!"
                },
                enabled = chips >= betAmount,
                colors = ButtonDefaults.buttonColors(containerColor = ChipGold, contentColor = Color.Black),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("START MINES (${betAmount} CHIPS)", fontWeight = FontWeight.Bold)
            }
        } else {
            val cashoutAmount = (betAmount * currentMultiplier).toLong()
            Button(
                onClick = {
                    inGame = false
                    statusMessage = "CASHED OUT: +$cashoutAmount Chips!"
                    onResult(cashoutAmount, betAmount)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldProfit, contentColor = Color.Black),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("CASH OUT ($cashoutAmount CHIPS)", fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

// -------------------------------------------------------------------------
// 4. KENO GAME WITH ANIMATED DRAW
// -------------------------------------------------------------------------
@Composable
fun KenoGame(
    chips: Long,
    betAmount: Long,
    onResult: (Long, Long) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedNumbers by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var drawnNumbers by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var isDrawing by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf("") }

    TycoonCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("FORTUNE KENO (PICK UP TO 6)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            Text("${selectedNumbers.size}/6 Picked", style = MaterialTheme.typography.titleSmall, color = ChipGold)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Numbers Grid (1 to 24 for clean touch layout)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (r in 0 until 4) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (c in 1..6) {
                        val num = r * 6 + c
                        val isSelected = num in selectedNumbers
                        val isDrawn = num in drawnNumbers
                        val isHit = isSelected && isDrawn

                        Surface(
                            color = when {
                                isHit -> EmeraldProfit
                                isDrawn -> GoldPrimary
                                isSelected -> CyanTech
                                else -> DarkSurfaceVariant
                            },
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clickable(enabled = !isDrawing) {
                                    if (isSelected) {
                                        selectedNumbers = selectedNumbers - num
                                    } else if (selectedNumbers.size < 6) {
                                        selectedNumbers = selectedNumbers + num
                                    }
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "$num",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected || isDrawn) Color.Black else Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        if (resultText.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(resultText, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = GoldLight)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                if (chips < betAmount || selectedNumbers.isEmpty() || isDrawing) return@Button
                coroutineScope.launch {
                    isDrawing = true
                    drawnNumbers = emptySet()
                    resultText = "Drawing lucky balls..."

                    val pool = (1..24).toList().shuffled()
                    val targetDrawn = pool.take(8)

                    for (num in targetDrawn) {
                        drawnNumbers = drawnNumbers + num
                        delay(120L)
                    }

                    val hits = selectedNumbers.intersect(drawnNumbers).size
                    isDrawing = false

                    val mult = when (hits) {
                        2 -> 2L
                        3 -> 5L
                        4 -> 15L
                        5 -> 40L
                        6 -> 100L
                        else -> 0L
                    }

                    if (mult > 0) {
                        val win = betAmount * mult
                        resultText = "MATCHED $hits NUMBERS! +$win Chips!"
                        onResult(win, betAmount)
                    } else {
                        resultText = "Matched $hits numbers. No payout."
                        onResult(0L, betAmount)
                    }
                }
            },
            enabled = chips >= betAmount && selectedNumbers.isNotEmpty() && !isDrawing,
            colors = ButtonDefaults.buttonColors(containerColor = ChipGold, contentColor = Color.Black),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isDrawing) "DRAWING BALLS..." else "DRAW BALLS (${betAmount} CHIPS)", fontWeight = FontWeight.Bold)
        }
    }
}

// -------------------------------------------------------------------------
// 5. HI-LO GAME WITH PLAYING CARDS
// -------------------------------------------------------------------------
@Composable
fun HiLoGame(
    chips: Long,
    betAmount: Long,
    onResult: (Long, Long) -> Unit
) {
    var currentCard by remember { mutableStateOf(createRandomPlayingCard()) }
    var streak by remember { mutableStateOf(0) }
    var multiplier by remember { mutableStateOf(1.0) }
    var statusText by remember { mutableStateOf("Guess if next card is Higher or Lower!") }

    TycoonCard(modifier = Modifier.fillMaxWidth()) {
        Text("HI-LO CARD STREAK", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(10.dp))

        // Card Display
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlayingCardView(currentCard)

            Column {
                Text("STREAK: $streak", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = GoldLight)
                Text("Multiplier: ${String.format("%.2f", multiplier)}x", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = EmeraldProfit)
                Text(statusText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    if (chips < betAmount && streak == 0) return@Button
                    val next = createRandomPlayingCard()
                    if (next.value >= currentCard.value) {
                        streak++
                        multiplier += 0.45
                        currentCard = next
                        statusText = "Correct! Next was ${next.display}."
                    } else {
                        statusText = "Lost! Next was ${next.display}."
                        onResult(0L, betAmount)
                        streak = 0
                        multiplier = 1.0
                        currentCard = createRandomPlayingCard()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldProfit, contentColor = Color.Black),
                modifier = Modifier.weight(1f)
            ) {
                Text("HIGHER ↑", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    if (chips < betAmount && streak == 0) return@Button
                    val next = createRandomPlayingCard()
                    if (next.value <= currentCard.value) {
                        streak++
                        multiplier += 0.45
                        currentCard = next
                        statusText = "Correct! Next was ${next.display}."
                    } else {
                        statusText = "Lost! Next was ${next.display}."
                        onResult(0L, betAmount)
                        streak = 0
                        multiplier = 1.0
                        currentCard = createRandomPlayingCard()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonLoss, contentColor = Color.White),
                modifier = Modifier.weight(1f)
            ) {
                Text("LOWER ↓", fontWeight = FontWeight.Bold)
            }
        }

        if (streak > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            val win = (betAmount * multiplier).toLong()
            Button(
                onClick = {
                    onResult(win, betAmount)
                    statusText = "Cashed out +$win Chips!"
                    streak = 0
                    multiplier = 1.0
                    currentCard = createRandomPlayingCard()
                },
                colors = ButtonDefaults.buttonColors(containerColor = ChipGold, contentColor = Color.Black),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("CASH OUT ($win CHIPS)", fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}
