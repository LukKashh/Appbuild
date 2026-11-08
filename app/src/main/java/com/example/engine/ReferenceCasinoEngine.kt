package com.example.engine

import java.security.SecureRandom
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.random.Random

data class LocalCasinoCard(val rank: Int, val suit: Int) {
    val display: String
        get() = "${when (rank) { 1 -> "A"; 11 -> "J"; 12 -> "Q"; 13 -> "K"; else -> rank.toString() }}${listOf("♠", "♥", "♦", "♣")[suit.coerceIn(0, 3)]}"
}

data class LocalCasinoActive(
    val game: String,
    val round: Long,
    val seed: Long,
    val bet: Double,
    val playerCards: List<LocalCasinoCard> = emptyList(),
    val dealerCards: List<LocalCasinoCard> = emptyList(),
    val drawn: Int = 0,
    val doubled: Boolean = false,
    val mines: Int = 0,
    val revealed: List<Int> = emptyList(),
    val currentCard: LocalCasinoCard? = null,
    val multiplier: Double = 1.0,
    val history: List<LocalCasinoCard> = emptyList()
)

data class LocalCasinoLast(
    val game: String,
    val bet: Double,
    val payout: Double,
    val timestamp: Long,
    val detail: String
)

data class LocalCasinoStats(
    val wagered: Double = 0.0,
    val won: Double = 0.0,
    val rounds: Long = 0L,
    val biggestWin: Double = 0.0
)

data class LocalCasinoState(
    val chips: Double = 0.0,
    val round: Long = 0L,
    val active: LocalCasinoActive? = null,
    val last: LocalCasinoLast? = null,
    val stats: LocalCasinoStats = LocalCasinoStats()
)

data class LocalRouletteBet(
    val kind: String,
    val amount: Double,
    val value: Int = 0
)

data class LocalCasinoResult(
    val game: String,
    val bet: Double,
    val payout: Double,
    val details: String,
    val timestamp: Long
)

/** Local port of casino.ts. The seed is device-generated and stored only for an active round. */
object ReferenceCasinoEngine {
    const val CHIP_VALUE = 100.0
    const val MIN_BET = 1.0
    const val MAX_BET = 1_000_000.0
    const val HOUSE_EDGE = 0.03
    const val HILO_MAX_MULTIPLIER = 10_000.0

    private val redNumbers = setOf(1,3,5,7,9,12,14,16,18,19,21,23,25,27,30,32,34,36)
    private val kenoPay = mapOf(
        1 to listOf(0.0,3.8),
        2 to listOf(0.0,1.7,5.2),
        3 to listOf(0.0,0.0,2.5,40.0),
        4 to listOf(0.0,0.0,1.5,9.0,70.0),
        5 to listOf(0.0,0.0,1.2,3.0,12.0,300.0),
        6 to listOf(0.0,0.0,0.0,2.5,8.0,100.0,500.0),
        7 to listOf(0.0,0.0,0.0,1.5,5.0,25.0,250.0,700.0),
        8 to listOf(0.0,0.0,0.0,1.5,3.5,10.0,50.0,300.0,800.0),
        9 to listOf(0.0,0.0,0.0,1.5,2.5,5.0,12.0,80.0,400.0,900.0),
        10 to listOf(0.0,0.0,0.0,1.2,2.0,3.5,7.0,24.0,90.0,400.0,1000.0)
    )

    fun buyChips(state: LocalSimulationState, chips: Long, now: Long): LocalGameResult {
        if (chips < 1) return reject(state, "Enter at least 1 chip")
        val cost = chips * CHIP_VALUE
        if (state.cash - state.reservedCash + 1e-9 < cost) return reject(state, "Insufficient available cash")
        val next = state.copy(cash = state.cash - cost, casino = state.casino.copy(chips = round2(state.casino.chips + chips)))
        return success(record(next, now, "casino_chips_buy", "Exchanged cash for $chips casino chips", -cost), "Chips purchased")
    }

    fun sellChips(state: LocalSimulationState, chips: Long, now: Long): LocalGameResult {
        if (chips < 1) return reject(state, "Enter at least 1 chip")
        if (state.casino.chips + 1e-9 < chips) return reject(state, "Not enough casino chips")
        val proceeds = chips * CHIP_VALUE
        val next = state.copy(
            cash = state.cash + proceeds,
            casino = state.casino.copy(chips = round2(state.casino.chips - chips))
        )
        return success(record(next, now, "casino_chips_sell", "Exchanged $chips casino chips for cash", proceeds), "Chips exchanged")
    }

    fun roulette(state: LocalSimulationState, bets: List<LocalRouletteBet>, now: Long): LocalGameResult {
        if (state.casino.active != null) return reject(state, "Finish your current game first")
        if (bets.isEmpty() || bets.size > 40) return reject(state, "Place between 1 and 40 bets")
        if (bets.any { it.amount < MIN_BET }) return reject(state, "Each bet must be at least 1 chip")
        val total = bets.sumOf { it.amount }
        if (total > MAX_BET) return reject(state, "Maximum round bet is 1,000,000 chips")
        var next = stake(state, total, now, "roulette") ?: return reject(state, "Not enough chips")
        val number = Random(newSeed(state, now)).nextInt(37)
        var payout = 0.0
        for (bet in bets) {
            val value = bet.value
            if (bet.kind == "straight" && value !in 0..36) return reject(state, "Invalid roulette number")
            if ((bet.kind == "dozen" || bet.kind == "column") && value !in 1..3) return reject(state, "Invalid roulette selection")
            val hit = when (bet.kind) {
                "straight" -> number == value
                "red" -> number != 0 && number in redNumbers
                "black" -> number != 0 && number !in redNumbers
                "even" -> number != 0 && number % 2 == 0
                "odd" -> number % 2 == 1
                "low" -> number in 1..18
                "high" -> number in 19..36
                "dozen" -> number != 0 && ((number - 1) / 12 + 1) == value
                "column" -> number != 0 && ((number - 1) % 3 + 1) == value
                else -> return reject(state, "Unknown roulette bet")
            }
            val odds = when (bet.kind) {
                "straight" -> 35.0
                "dozen", "column" -> 2.0
                else -> 1.0
            }
            if (hit) payout += bet.amount * (odds + 1.0)
        }
        next = settle(next, now, "roulette", total, payout, "Wheel landed on $number")
        return success(record(next, now, "casino_round", "Roulette settled: $number", payout - total), "Roulette round settled")
    }

    fun keno(state: LocalSimulationState, bet: Double, picksInput: List<Int>, now: Long): LocalGameResult {
        if (state.casino.active != null) return reject(state, "Finish your current game first")
        val picks = picksInput.distinct()
        if (picks.size !in 1..10 || picks.size != picksInput.size || picks.any { it !in 1..80 }) {
            return reject(state, "Pick 1 to 10 unique numbers from 1 to 80")
        }
        val staked = stake(state, bet, now, "keno") ?: return reject(state, "Invalid bet or insufficient chips")
        val draw = shuffled((1..80).toList(), Random(newSeed(state, now))).take(20)
        val hits = picks.count { it in draw }
        val multiplier = kenoPay[picks.size]?.getOrNull(hits) ?: 0.0
        val payout = bet * multiplier
        val next = settle(staked, now, "keno", bet, payout, "$hits of ${picks.size} numbers matched")
        return success(record(next, now, "casino_round", "Keno settled: $hits matches", payout - bet), "Keno round settled")
    }

    fun blackjackStart(state: LocalSimulationState, bet: Double, now: Long): LocalGameResult {
        if (state.casino.active != null) return reject(state, "Finish your current game first")
        val staked = stake(state, bet, now, "blackjack") ?: return reject(state, "Invalid bet or insufficient chips")
        val seed = newSeed(state, now)
        val cards = deck(seed)
        val active = LocalCasinoActive(
            game = "blackjack",
            round = staked.casino.round,
            seed = seed,
            bet = bet,
            playerCards = listOf(cards[0], cards[2]),
            dealerCards = listOf(cards[1]),
            drawn = 4
        )
        var next = staked.copy(casino = staked.casino.copy(active = active))
        val player = handValue(active.playerCards).first
        val dealer = handValue(listOf(cards[1], cards[3])).first
        if (player == 21 || dealer == 21) next = finishBlackjack(next, active, now)
        return success(next, if (next.casino.active == null) "Blackjack settled" else "Blackjack hand started")
    }

    fun blackjackMove(state: LocalSimulationState, move: String, now: Long): LocalGameResult {
        val active = state.casino.active ?: return reject(state, "No blackjack hand is active")
        if (active.game != "blackjack") return reject(state, "Finish your current game first")
        val cards = deck(active.seed)
        var updated = active
        when (move) {
            "hit" -> {
                val card = cards.getOrNull(updated.drawn) ?: return reject(state, "No cards remain")
                updated = updated.copy(playerCards = updated.playerCards + card, drawn = updated.drawn + 1)
                if (handValue(updated.playerCards).first >= 21) {
                    return success(finishBlackjack(state.copy(casino = state.casino.copy(active = updated)), updated, now), "Blackjack hand settled")
                }
                return success(state.copy(casino = state.casino.copy(active = updated)), "Card drawn")
            }
            "stand" -> return success(finishBlackjack(state, active, now), "Blackjack hand settled")
            "double" -> {
                if (active.playerCards.size != 2) return reject(state, "Double is only allowed on the first two cards")
                val staked = stake(state, active.bet, now, "blackjack-double")
                    ?: return reject(state, "Not enough chips to double")
                val card = cards.getOrNull(active.drawn) ?: return reject(state, "No cards remain")
                updated = active.copy(bet = active.bet * 2.0, doubled = true, playerCards = active.playerCards + card, drawn = active.drawn + 1)
                return success(finishBlackjack(staked.copy(casino = staked.casino.copy(active = updated)), updated, now), "Blackjack doubled and settled")
            }
            else -> return reject(state, "Unknown blackjack move")
        }
    }

    fun minesStart(state: LocalSimulationState, bet: Double, mines: Int, now: Long): LocalGameResult {
        if (state.casino.active != null) return reject(state, "Finish your current game first")
        if (mines !in 1..24) return reject(state, "Choose between 1 and 24 mines")
        val staked = stake(state, bet, now, "mines") ?: return reject(state, "Invalid bet or insufficient chips")
        val active = LocalCasinoActive(
            game = "mines",
            round = staked.casino.round,
            seed = newSeed(state, now),
            bet = bet,
            mines = mines
        )
        return success(staked.copy(casino = staked.casino.copy(active = active)), "Mines round started")
    }

    fun minesReveal(state: LocalSimulationState, tile: Int, now: Long): LocalGameResult {
        val active = state.casino.active ?: return reject(state, "No mines game is active")
        if (active.game != "mines") return reject(state, "Finish your current game first")
        if (tile !in 0..24 || tile in active.revealed) return reject(state, "Invalid tile")
        val field = shuffled((0..24).toList(), Random(active.seed)).take(active.mines).toSet()
        if (tile in field) {
            val settled = settle(state, now, "mines", active.bet, 0.0, "Mine hit at tile ${tile + 1}")
            return success(record(settled, now, "casino_round", "Mines bust", -active.bet), "Mine hit")
        }
        val revealed = active.revealed + tile
        val updated = active.copy(revealed = revealed)
        val next = state.copy(casino = state.casino.copy(active = updated))
        if (revealed.size == 25 - active.mines) {
            val multiplier = minesMultiplier(active.mines, revealed.size)
            val settled = settle(next, now, "mines", active.bet, active.bet * multiplier, "All safe tiles revealed")
            return success(record(settled, now, "casino_round", "Mines cleared", active.bet * multiplier - active.bet), "Mines cleared")
        }
        return success(next, "Safe tile")
    }

    fun minesCashout(state: LocalSimulationState, now: Long): LocalGameResult {
        val active = state.casino.active ?: return reject(state, "No mines game is active")
        if (active.game != "mines") return reject(state, "Finish your current game first")
        if (active.revealed.isEmpty()) return reject(state, "Reveal at least one tile before cashing out")
        val multiplier = minesMultiplier(active.mines, active.revealed.size)
        val payout = active.bet * multiplier
        val next = settle(state, now, "mines", active.bet, payout, "Cashed out at ${String.format(java.util.Locale.US, "%.4f", multiplier)}x")
        return success(record(next, now, "casino_round", "Mines cashout", payout - active.bet), "Mines payout collected")
    }

    fun hiloStart(state: LocalSimulationState, bet: Double, now: Long): LocalGameResult {
        if (state.casino.active != null) return reject(state, "Finish your current game first")
        val staked = stake(state, bet, now, "hilo") ?: return reject(state, "Invalid bet or insufficient chips")
        val seed = newSeed(state, now)
        val cards = deck(seed)
        val first = cards[0]
        val active = LocalCasinoActive(
            game = "hilo",
            round = staked.casino.round,
            seed = seed,
            bet = bet,
            drawn = 1,
            currentCard = first,
            history = listOf(first)
        )
        return success(staked.copy(casino = staked.casino.copy(active = active)), "Hi-Lo round started")
    }

    fun hiloGuess(state: LocalSimulationState, higher: Boolean, now: Long): LocalGameResult {
        val active = state.casino.active ?: return reject(state, "No Hi-Lo round is active")
        if (active.game != "hilo") return reject(state, "Finish your current game first")
        val current = active.currentCard ?: return reject(state, "Current card unavailable")
        val next = deck(active.seed).getOrNull(active.drawn) ?: return reject(state, "No cards remain")
        val probability = if (higher) (14.0 - current.rank) / 13.0 else current.rank / 13.0
        val won = if (higher) next.rank >= current.rank else next.rank <= current.rank
        val history = active.history + next
        if (!won) {
            val settled = settle(state.copy(casino = state.casino.copy(active = active.copy(history = history))), now, "hilo", active.bet, 0.0, "Wrong guess; next card was ${next.display}")
            return success(record(settled, now, "casino_round", "Hi-Lo lost", -active.bet), "Wrong guess")
        }
        val multiplier = round4(active.multiplier * ((1.0 - HOUSE_EDGE) / probability))
        if (multiplier >= HILO_MAX_MULTIPLIER) {
            val payout = active.bet * HILO_MAX_MULTIPLIER
            val settled = settle(state.copy(casino = state.casino.copy(active = active.copy(history = history))), now, "hilo", active.bet, payout, "Maximum multiplier reached")
            return success(record(settled, now, "casino_round", "Hi-Lo maximum payout", payout - active.bet), "Maximum payout")
        }
        val updated = active.copy(currentCard = next, drawn = active.drawn + 1, multiplier = multiplier, history = history)
        return success(state.copy(casino = state.casino.copy(active = updated)), "Correct guess · ${String.format(java.util.Locale.US, "%.4f", multiplier)}x")
    }

    fun hiloCashout(state: LocalSimulationState, now: Long): LocalGameResult {
        val active = state.casino.active ?: return reject(state, "No Hi-Lo round is active")
        if (active.game != "hilo") return reject(state, "Finish your current game first")
        if (active.multiplier <= 1.0) return reject(state, "Make at least one correct guess first")
        val payout = active.bet * active.multiplier
        val settled = settle(state, now, "hilo", active.bet, payout, "Cashed out at ${String.format(java.util.Locale.US, "%.4f", active.multiplier)}x")
        return success(record(settled, now, "casino_round", "Hi-Lo cashout", payout - active.bet), "Payout collected")
    }

    fun handValue(cards: List<LocalCasinoCard>): Pair<Int, Boolean> {
        var total = 0
        var aces = 0
        cards.forEach { card ->
            if (card.rank == 1) {
                aces++
                total += 11
            } else total += min(10, card.rank)
        }
        while (total > 21 && aces > 0) {
            total -= 10
            aces--
        }
        return total to (aces > 0)
    }

    fun minesMultiplier(mines: Int, safe: Int): Double {
        var multiplier = 1.0
        repeat(safe) { index ->
            multiplier *= (25.0 - index) / (25.0 - mines - index)
        }
        return if (safe == 0) 1.0 else multiplier * (1.0 - HOUSE_EDGE)
    }

    private fun stake(state: LocalSimulationState, bet: Double, now: Long, game: String): LocalSimulationState? {
        if (!bet.isFinite() || bet < MIN_BET || bet > MAX_BET || state.casino.chips + 1e-9 < bet) return null
        return state.copy(casino = state.casino.copy(
            chips = round2(state.casino.chips - bet),
            stats = state.casino.stats.copy(wagered = state.casino.stats.wagered + bet)
        ))
    }

    private fun settle(
        state: LocalSimulationState,
        now: Long,
        game: String,
        bet: Double,
        payout: Double,
        detail: String
    ): LocalSimulationState {
        val paid = max(0.0, payout)
        val casino = state.casino
        val profit = paid - bet
        val stats = casino.stats.copy(
            won = casino.stats.won + paid,
            rounds = casino.stats.rounds + 1,
            biggestWin = max(casino.stats.biggestWin, profit)
        )
        val last = LocalCasinoLast(game, bet, paid, now, detail)
        return state.copy(casino = casino.copy(
            chips = round2(casino.chips + paid),
            round = casino.round + 1,
            active = null,
            last = last,
            stats = stats
        ))
    }

    private fun finishBlackjack(state: LocalSimulationState, active: LocalCasinoActive, now: Long): LocalSimulationState {
        val cards = deck(active.seed)
        val dealer = mutableListOf(active.dealerCards.first(), cards[3])
        val playerValue = handValue(active.playerCards).first
        val playerNatural = active.playerCards.size == 2 && playerValue == 21
        val dealerNatural = handValue(dealer).first == 21
        var nextIndex = active.drawn
        if (playerValue <= 21 && !playerNatural) {
            while (true) {
                val dealerValue = handValue(dealer).first
                if (dealerValue >= 17) break
                val nextCard = cards.getOrNull(nextIndex) ?: break
                dealer += nextCard
                nextIndex++
            }
        }
        val dealerValue = handValue(dealer).first
        val payout: Double
        val outcome: String
        when {
            playerValue > 21 -> { payout = 0.0; outcome = "Player bust" }
            playerNatural && !dealerNatural -> { payout = active.bet * 2.5; outcome = "Blackjack" }
            dealerNatural && !playerNatural -> { payout = 0.0; outcome = "Dealer blackjack" }
            dealerValue > 21 || playerValue > dealerValue -> { payout = active.bet * 2.0; outcome = "Player wins" }
            playerValue == dealerValue -> { payout = active.bet; outcome = "Push" }
            else -> { payout = 0.0; outcome = "Dealer wins" }
        }
        val stateWithActive = state.copy(casino = state.casino.copy(active = active))
        val settled = settle(stateWithActive, now, "blackjack", active.bet, payout,
            "$outcome · player $playerValue · dealer $dealerValue")
        return record(settled, now, "casino_round", "Blackjack: $outcome", payout - active.bet)
    }

    private fun deck(seed: Long): List<LocalCasinoCard> {
        val random = Random(seed)
        val cards = (0..3).flatMap { suit ->
            (1..13).map { rank -> LocalCasinoCard(rank, suit) }
        }.toMutableList()
        for (i in cards.lastIndex downTo 1) {
            val j = random.nextInt(i + 1)
            val temp = cards[i]
            cards[i] = cards[j]
            cards[j] = temp
        }
        return cards
    }

    private fun <T> shuffled(values: List<T>, random: Random): List<T> {
        val result = values.toMutableList()
        for (i in result.lastIndex downTo 1) {
            val j = random.nextInt(i + 1)
            val temp = result[i]
            result[i] = result[j]
            result[j] = temp
        }
        return result
    }

    private fun newSeed(state: LocalSimulationState, now: Long): Long =
        SecureRandom().nextLong() xor now xor state.casino.round xor state.nextSequence

    private fun round2(value: Double) = round(value * 100.0) / 100.0
    private fun round4(value: Double) = round(value * 10_000.0) / 10_000.0

    private fun record(state: LocalSimulationState, now: Long, category: String, description: String, amount: Double): LocalSimulationState {
        val entry = LocalLedgerEntry(
            id = "local-${state.nextSequence}",
            timestamp = now,
            category = category,
            description = description,
            amount = amount,
            balanceAfter = state.cash
        )
        return state.copy(ledger = (state.ledger + entry).takeLast(5000), nextSequence = state.nextSequence + 1)
    }

    private fun reject(state: LocalSimulationState, reason: String) = LocalGameResult.Rejected(state, reason)
    private fun success(state: LocalSimulationState, message: String) = LocalGameResult.Success(state, message)
}
