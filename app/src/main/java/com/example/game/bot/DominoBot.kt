package com.example.game.bot

import com.example.game.BoardEnd
import com.example.game.DominoBoard
import com.example.game.DominoTile
import com.example.game.LegalMove

enum class BotDifficulty {
    EASY,
    MEDIUM,
    HARD
}

data class BotDecision(
    val tileId: Int,
    val end: BoardEnd
)

/**
 * Intelligent Bot Engine for offline practice & tutorial mode.
 */
object DominoBot {

    fun decideMove(
        hand: List<DominoTile>,
        board: DominoBoard,
        legalMoves: List<LegalMove>,
        difficulty: BotDifficulty = BotDifficulty.MEDIUM
    ): BotDecision? {
        if (legalMoves.isEmpty()) return null

        return when (difficulty) {
            BotDifficulty.EASY -> {
                // Easy: picks first legal move or simple random legal move
                val move = legalMoves.first()
                val end = move.playableEnds.first()
                BotDecision(move.tile.id, end)
            }

            BotDifficulty.MEDIUM -> {
                // Medium: Prioritize playing doubles to avoid getting stuck with them,
                // otherwise dump highest pip tiles to minimize risk if blocked
                val doubleMove = legalMoves.filter { it.tile.isDouble }
                    .maxByOrNull { it.tile.totalPips }

                if (doubleMove != null) {
                    BotDecision(doubleMove.tile.id, doubleMove.playableEnds.first())
                } else {
                    val highestPipMove = legalMoves.maxByOrNull { it.tile.totalPips }!!
                    BotDecision(highestPipMove.tile.id, highestPipMove.playableEnds.first())
                }
            }

            BotDifficulty.HARD -> {
                // Hard: Evaluate moves strategically:
                // 1. Play high pips / high doubles.
                // 2. Prefer keeping ends open that the bot holds matching tiles for in hand!
                // 3. Avoid giving away ends the bot cannot match later.
                var bestScore = Int.MIN_VALUE
                var bestDecision: BotDecision? = null

                for (move in legalMoves) {
                    for (end in move.playableEnds) {
                        var score = 0
                        // Reward playing doubles early
                        if (move.tile.isDouble) score += 30

                        // Reward getting rid of heavy pips
                        score += move.tile.totalPips * 2

                        // Simulate what the new open end will be
                        val currentTarget = if (end == BoardEnd.LEFT) board.openLeftValue else board.openRightValue
                        val newOpenValue = if (move.tile.leftValue == currentTarget) {
                            move.tile.rightValue
                        } else {
                            move.tile.leftValue
                        }

                        // Check remaining hand: do we have another tile matching newOpenValue?
                        val remainingHand = hand.filter { it.id != move.tile.id }
                        val matchesInHand = remainingHand.count { it.hasValue(newOpenValue) }
                        score += matchesInHand * 15

                        if (score > bestScore) {
                            bestScore = score
                            bestDecision = BotDecision(move.tile.id, end)
                        }
                    }
                }
                bestDecision ?: BotDecision(legalMoves.first().tile.id, legalMoves.first().playableEnds.first())
            }
        }
    }
}
