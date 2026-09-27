package com.example.game

import kotlin.random.Random

enum class GameStatus {
    LOBBY,
    STARTING,
    IN_ROUND,
    ROUND_FINISHED,
    MATCH_FINISHED
}

enum class RoundFinishReason {
    DOMINO, // A player played their last tile
    BLOCKED // Game is blocked; nobody can play
}

data class RoundResult(
    val reason: RoundFinishReason,
    val winningPlayerId: String?,
    val winningTeam: Team?,
    val pointsEarned: Int,
    val isMatchOver: Boolean,
    val matchWinningTeam: Team?,
    val pipsBreakdown: Map<String, Int>,
    val description: String
)

data class LegalMove(
    val tile: DominoTile,
    val playableEnds: List<BoardEnd>
)

data class MoveResult(
    val success: Boolean,
    val errorMessage: String? = null,
    val playedTile: DominoTile? = null,
    val boardEnd: BoardEnd? = null,
    val isRoundOver: Boolean = false,
    val roundResult: RoundResult? = null
)

data class PassResult(
    val success: Boolean,
    val isBlocked: Boolean = false,
    val roundResult: RoundResult? = null
)

/**
 * Independent, pure Domain Game Engine for Domino Rami.
 * Enforces all game rules, turns, moves, validation, and scoring.
 */
class GameEngine(
    val rules: GameRules = GameRules.DEFAULT_FOUR_PLAYER_PARTNERSHIP,
    private val random: Random = Random.Default
) {
    var status: GameStatus = GameStatus.LOBBY
        private set

    var currentRound: Int = 1
        private set

    var activeSeatIndex: Int = 0
        private set

    var board: DominoBoard = DominoBoard()
        private set

    var players: List<Player> = emptyList()
        private set

    // Master hands held by the engine
    private val playerHands = mutableMapOf<String, MutableList<DominoTile>>()

    var teamScores = mutableMapOf<Team, Int>().apply {
        put(Team.TEAM_A, 0)
        put(Team.TEAM_B, 0)
    }
        private set

    var consecutivePasses: Int = 0
        private set

    var lastRoundResult: RoundResult? = null
        private set

    var previousRoundWinnerSeat: Int? = null
        private set

    /**
     * Initializes a new match with the given players.
     */
    fun startNewMatch(playerList: List<Player>) {
        require(playerList.size == rules.numberOfPlayers) {
            "Player count must match rules (${rules.numberOfPlayers})"
        }
        players = playerList
        teamScores[Team.TEAM_A] = 0
        teamScores[Team.TEAM_B] = 0
        currentRound = 1
        previousRoundWinnerSeat = null
        lastRoundResult = null
        startNewRound()
    }

    /**
     * Deals tiles and starts a new round.
     */
    fun startNewRound() {
        val fullSet = DominoSet.createFullSet().shuffled(random)
        playerHands.clear()
        board = DominoBoard()
        consecutivePasses = 0
        lastRoundResult = null

        // Deal 7 tiles per player (for 4 players, 28 tiles total)
        var tileIndex = 0
        players.forEach { player ->
            val hand = fullSet.subList(tileIndex, tileIndex + rules.tilesPerPlayer).toMutableList()
            playerHands[player.id] = hand
            tileIndex += rules.tilesPerPlayer
        }

        // Determine starting player
        activeSeatIndex = determineStartingPlayer()
        status = GameStatus.IN_ROUND
    }

    private fun determineStartingPlayer(): Int {
        if (currentRound == 1 && rules.startingRule == StartingRule.SIX_SIX_FIRST_ROUND) {
            // Player with 6-6 starts
            players.forEachIndexed { index, player ->
                val hand = playerHands[player.id] ?: emptyList()
                if (hand.any { DominoSet.isDoubleSix(it) }) {
                    return index
                }
            }
        }

        // In subsequent rounds or if no 6-6 found: previous round winner starts
        previousRoundWinnerSeat?.let { return it }

        // Fallback: player holding highest double or highest tile
        var highestDoublePip = -1
        var bestSeat = 0
        players.forEachIndexed { index, player ->
            val hand = playerHands[player.id] ?: emptyList()
            hand.filter { it.isDouble }.forEach { d ->
                if (d.leftValue > highestDoublePip) {
                    highestDoublePip = d.leftValue
                    bestSeat = index
                }
            }
        }
        if (highestDoublePip >= 0) return bestSeat

        // Fallback to highest total pips
        var highestPips = -1
        players.forEachIndexed { index, player ->
            val hand = playerHands[player.id] ?: emptyList()
            hand.forEach { t ->
                if (t.totalPips > highestPips) {
                    highestPips = t.totalPips
                    bestSeat = index
                }
            }
        }
        return bestSeat
    }

    fun getActivePlayer(): Player? = players.getOrNull(activeSeatIndex)

    fun getPlayerHand(playerId: String): List<DominoTile> = playerHands[playerId]?.toList() ?: emptyList()

    fun getPlayerTileCount(playerId: String): Int = playerHands[playerId]?.size ?: 0

    /**
     * Returns all legal moves for a given player based on current board state.
     */
    fun getLegalMoves(playerId: String): List<LegalMove> {
        val hand = playerHands[playerId] ?: return emptyList()
        val player = players.find { it.id == playerId } ?: return emptyList()
        if (player.seatIndex != activeSeatIndex || status != GameStatus.IN_ROUND) {
            return emptyList()
        }

        // Round 1, first move rule: if starting with 6-6, player must play 6-6!
        if (board.isEmpty && currentRound == 1 && rules.startingRule == StartingRule.SIX_SIX_FIRST_ROUND) {
            val doubleSix = hand.find { DominoSet.isDoubleSix(it) }
            if (doubleSix != null) {
                return listOf(LegalMove(doubleSix, listOf(BoardEnd.RIGHT)))
            }
        }

        val legal = mutableListOf<LegalMove>()
        for (tile in hand) {
            val playableEnds = board.getPlayableEnds(tile)
            if (playableEnds.isNotEmpty()) {
                legal.add(LegalMove(tile, playableEnds))
            }
        }
        return legal
    }

    /**
     * Checks if a move is valid.
     */
    fun isValidMove(playerId: String, tileId: Int, end: BoardEnd): Boolean {
        val player = players.find { it.id == playerId } ?: return false
        if (player.seatIndex != activeSeatIndex || status != GameStatus.IN_ROUND) return false

        val hand = playerHands[playerId] ?: return false
        val tile = hand.find { it.id == tileId } ?: return false

        // First move of first round constraint
        if (board.isEmpty && currentRound == 1 && rules.startingRule == StartingRule.SIX_SIX_FIRST_ROUND) {
            return DominoSet.isDoubleSix(tile)
        }

        return board.canPlayOnEnd(tile, end)
    }

    /**
     * Executes a move on behalf of the player.
     */
    fun applyMove(playerId: String, tileId: Int, end: BoardEnd): MoveResult {
        val player = players.find { it.id == playerId }
            ?: return MoveResult(false, "لاعب غير موجود")

        if (player.seatIndex != activeSeatIndex) {
            return MoveResult(false, "ليس دورك الآن")
        }

        if (status != GameStatus.IN_ROUND) {
            return MoveResult(false, "المباراة ليست في جولة نشطة")
        }

        val hand = playerHands[playerId]
            ?: return MoveResult(false, "يد اللاعب غير متوفرة")

        val tile = hand.find { it.id == tileId }
            ?: return MoveResult(false, "الحجر ليس في يد اللاعب")

        // First move of round 1 check
        if (board.isEmpty && currentRound == 1 && rules.startingRule == StartingRule.SIX_SIX_FIRST_ROUND) {
            if (!DominoSet.isDoubleSix(tile)) {
                return MoveResult(false, "يجب أن تبدأ بأول حجر 6-6 (الدوشيش)")
            }
        } else if (!board.canPlayOnEnd(tile, end)) {
            return MoveResult(false, "الحجر لا يطابق طرف السلسلة المحدد")
        }

        // Apply placement
        board = board.placeTile(tile, end, playerId)
        hand.remove(tile)
        consecutivePasses = 0

        // Check if player emptied their hand (Domino!)
        if (hand.isEmpty()) {
            val roundResult = resolveRoundDomino(player)
            return MoveResult(
                success = true,
                playedTile = tile,
                boardEnd = end,
                isRoundOver = true,
                roundResult = roundResult
            )
        }

        // Advance turn to next player
        advanceTurn()

        return MoveResult(
            success = true,
            playedTile = tile,
            boardEnd = end,
            isRoundOver = false
        )
    }

    /**
     * Called when active player passes (has no valid moves).
     */
    fun passTurn(playerId: String): PassResult {
        val player = players.find { it.id == playerId }
            ?: return PassResult(false)

        if (player.seatIndex != activeSeatIndex || status != GameStatus.IN_ROUND) {
            return PassResult(false)
        }

        // Verify player actually has no legal moves!
        val legalMoves = getLegalMoves(playerId)
        if (legalMoves.isNotEmpty()) {
            return PassResult(false) // Cannot pass when legal move exists!
        }

        consecutivePasses++

        // If all players passed consecutively (consecutivePasses >= numberOfPlayers) -> BLOCKED GAME!
        if (consecutivePasses >= rules.numberOfPlayers) {
            val roundResult = resolveRoundBlocked()
            return PassResult(
                success = true,
                isBlocked = true,
                roundResult = roundResult
            )
        }

        advanceTurn()
        return PassResult(success = true, isBlocked = false)
    }

    private fun advanceTurn() {
        activeSeatIndex = (activeSeatIndex + 1) % rules.numberOfPlayers
    }

    private fun resolveRoundDomino(winner: Player): RoundResult {
        val winningTeam = winner.team
        previousRoundWinnerSeat = winner.seatIndex

        // Calculate points: In partnership, winning team scores pips of opposing team
        val pipsBreakdown = mutableMapOf<String, Int>()
        var pointsEarned = 0

        players.forEach { p ->
            val pips = playerHands[p.id]?.sumOf { it.totalPips } ?: 0
            pipsBreakdown[p.id] = pips
            if (p.team != winningTeam) {
                pointsEarned += pips
            }
        }

        // Add to team score
        val currentScore = teamScores[winningTeam] ?: 0
        val newScore = currentScore + pointsEarned
        teamScores[winningTeam] = newScore

        val isMatchOver = newScore >= rules.targetScore
        val matchWinningTeam = if (isMatchOver) winningTeam else null
        status = if (isMatchOver) GameStatus.MATCH_FINISHED else GameStatus.ROUND_FINISHED

        val description = "${winner.name} تخلص من جميع أحجاره! فوز ${winningTeam.displayNameArabic} بـ $pointsEarned نقطة"

        val result = RoundResult(
            reason = RoundFinishReason.DOMINO,
            winningPlayerId = winner.id,
            winningTeam = winningTeam,
            pointsEarned = pointsEarned,
            isMatchOver = isMatchOver,
            matchWinningTeam = matchWinningTeam,
            pipsBreakdown = pipsBreakdown,
            description = description
        )
        lastRoundResult = result
        return result
    }

    private fun resolveRoundBlocked(): RoundResult {
        val pipsBreakdown = mutableMapOf<String, Int>()
        var teamAPips = 0
        var teamBPips = 0

        players.forEach { p ->
            val pips = playerHands[p.id]?.sumOf { it.totalPips } ?: 0
            pipsBreakdown[p.id] = pips
            if (p.team == Team.TEAM_A) teamAPips += pips else teamBPips += pips
        }

        val winningTeam: Team?
        val pointsEarned: Int
        val description: String

        if (teamAPips < teamBPips) {
            winningTeam = Team.TEAM_A
            // In classic domino, winner scores the difference or losing team pips
            pointsEarned = teamBPips
            val newScore = (teamScores[Team.TEAM_A] ?: 0) + pointsEarned
            teamScores[Team.TEAM_A] = newScore
            description = "أُغلقت اللعبة! فاز ${Team.TEAM_A.displayNameArabic} بأقل مجموع أحجار ($teamAPips مقابل $teamBPips) وحصل على $pointsEarned نقطة"
        } else if (teamBPips < teamAPips) {
            winningTeam = Team.TEAM_B
            pointsEarned = teamAPips
            val newScore = (teamScores[Team.TEAM_B] ?: 0) + pointsEarned
            teamScores[Team.TEAM_B] = newScore
            description = "أُغلقت اللعبة! فاز ${Team.TEAM_B.displayNameArabic} بأقل مجموع أحجار ($teamBPips مقابل $teamAPips) وحصل على $pointsEarned نقطة"
        } else {
            // Exact Tie!
            winningTeam = null
            pointsEarned = 0
            description = "أُغلقت اللعبة بتعادل مجموع الأحجار ($teamAPips مقابل $teamBPips)! لا نقاط لأي فريق"
        }

        val matchWinningTeam = when {
            (teamScores[Team.TEAM_A] ?: 0) >= rules.targetScore -> Team.TEAM_A
            (teamScores[Team.TEAM_B] ?: 0) >= rules.targetScore -> Team.TEAM_B
            else -> null
        }
        val isMatchOver = matchWinningTeam != null
        status = if (isMatchOver) GameStatus.MATCH_FINISHED else GameStatus.ROUND_FINISHED

        val result = RoundResult(
            reason = RoundFinishReason.BLOCKED,
            winningPlayerId = null,
            winningTeam = winningTeam,
            pointsEarned = pointsEarned,
            isMatchOver = isMatchOver,
            matchWinningTeam = matchWinningTeam,
            pipsBreakdown = pipsBreakdown,
            description = description
        )
        lastRoundResult = result
        return result
    }

    /**
     * Resets the entire match to zero scores.
     */
    fun resetMatch() {
        teamScores[Team.TEAM_A] = 0
        teamScores[Team.TEAM_B] = 0
        currentRound = 1
        previousRoundWinnerSeat = null
        lastRoundResult = null
        if (players.isNotEmpty()) {
            startNewRound()
        }
    }
}
