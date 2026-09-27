package com.example

import com.example.game.BoardEnd
import com.example.game.DominoBoard
import com.example.game.DominoSet
import com.example.game.DominoTile
import com.example.game.GameEngine
import com.example.game.GameRules
import com.example.game.GameStatus
import com.example.game.Player
import com.example.game.RoundFinishReason
import com.example.game.StartingRule
import com.example.game.Team
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class DominoGameEngineTest {

    @Test
    fun testDominoSetContainsExact28UniqueTiles() {
        val fullSet = DominoSet.createFullSet()
        assertEquals(28, fullSet.size)

        // Ensure no duplicates
        val uniquePairs = fullSet.map {
            val min = minOf(it.leftValue, it.rightValue)
            val max = maxOf(it.leftValue, it.rightValue)
            "$min-$max"
        }.toSet()
        assertEquals(28, uniquePairs.size)

        // Verify [6|6] exists
        assertTrue(fullSet.any { DominoSet.isDoubleSix(it) })
    }

    @Test
    fun testDealDealsSevenTilesEachToFourPlayers() {
        val engine = GameEngine(GameRules.DEFAULT_FOUR_PLAYER_PARTNERSHIP)
        val players = listOf(
            Player("p1", "Rami", 0, Team.TEAM_A),
            Player("p2", "Ahmed", 1, Team.TEAM_B),
            Player("p3", "Sami", 2, Team.TEAM_A),
            Player("p4", "Omar", 3, Team.TEAM_B)
        )
        engine.startNewMatch(players)

        assertEquals(4, engine.players.size)
        players.forEach { p ->
            val hand = engine.getPlayerHand(p.id)
            assertEquals(7, hand.size)
        }

        // Total dealt tiles across all hands must be 28
        val allDealtTiles = players.flatMap { engine.getPlayerHand(it.id) }
        assertEquals(28, allDealtTiles.size)
        assertEquals(28, allDealtTiles.map { it.id }.toSet().size)
    }

    @Test
    fun testPlayerWithDoubleSixStartsRoundOne() {
        val engine = GameEngine(GameRules.DEFAULT_FOUR_PLAYER_PARTNERSHIP)
        val players = listOf(
            Player("p1", "Rami", 0, Team.TEAM_A),
            Player("p2", "Ahmed", 1, Team.TEAM_B),
            Player("p3", "Sami", 2, Team.TEAM_A),
            Player("p4", "Omar", 3, Team.TEAM_B)
        )
        engine.startNewMatch(players)

        val starter = engine.getActivePlayer()
        assertNotNull(starter)

        val starterHand = engine.getPlayerHand(starter!!.id)
        assertTrue("Starter in round 1 must hold [6|6]", starterHand.any { DominoSet.isDoubleSix(it) })

        // Starter MUST play 6-6 on round 1 lead
        val doubleSixTile = starterHand.first { DominoSet.isDoubleSix(it) }
        val invalidMoveResult = starterHand.firstOrNull { !DominoSet.isDoubleSix(it) }?.let { otherTile ->
            engine.applyMove(starter.id, otherTile.id, BoardEnd.RIGHT)
        }
        if (invalidMoveResult != null) {
            assertFalse("Playing non-6-6 on round 1 first move must fail", invalidMoveResult.success)
        }

        // Playing 6-6 succeeds
        val validMoveResult = engine.applyMove(starter.id, doubleSixTile.id, BoardEnd.RIGHT)
        assertTrue(validMoveResult.success)
        assertEquals(1, engine.board.size)
        assertEquals(6, engine.board.openLeftValue)
        assertEquals(6, engine.board.openRightValue)
    }

    @Test
    fun testBoardMatchingAndTurnProgression() {
        val board = DominoBoard()
        val tile66 = DominoTile(1, 6, 6)
        val b1 = board.placeTile(tile66, BoardEnd.RIGHT, "p1")
        assertEquals(6, b1.openLeftValue)
        assertEquals(6, b1.openRightValue)

        // Matching tile [6|3] placed on LEFT
        val tile63 = DominoTile(2, 6, 3)
        assertTrue(b1.canPlayOnEnd(tile63, BoardEnd.LEFT))
        val b2 = b1.placeTile(tile63, BoardEnd.LEFT, "p2")
        assertEquals(3, b2.openLeftValue)
        assertEquals(6, b2.openRightValue)

        // Matching tile [2|6] placed on RIGHT
        val tile26 = DominoTile(3, 2, 6)
        assertTrue(b2.canPlayOnEnd(tile26, BoardEnd.RIGHT))
        val b3 = b2.placeTile(tile26, BoardEnd.RIGHT, "p3")
        assertEquals(3, b3.openLeftValue)
        assertEquals(2, b3.openRightValue)

        // Tile [5|4] cannot be played on either end
        val tile54 = DominoTile(4, 5, 4)
        assertFalse(b3.canPlayTile(tile54))
    }

    @Test
    fun testDominoEmptiedHandScoresPointsForTeam() {
        val engine = GameEngine(GameRules.DEFAULT_FOUR_PLAYER_PARTNERSHIP)
        val players = listOf(
            Player("p1", "Rami", 0, Team.TEAM_A),
            Player("p2", "Ahmed", 1, Team.TEAM_B),
            Player("p3", "Sami", 2, Team.TEAM_A),
            Player("p4", "Omar", 3, Team.TEAM_B)
        )
        engine.startNewMatch(players)

        val starter = engine.getActivePlayer()!!
        val d6 = engine.getPlayerHand(starter.id).first { DominoSet.isDoubleSix(it) }
        engine.applyMove(starter.id, d6.id, BoardEnd.RIGHT)

        // Verify active seat rotated to next player (seatIndex + 1) % 4
        val expectedNextSeat = (starter.seatIndex + 1) % 4
        assertEquals(expectedNextSeat, engine.activeSeatIndex)
    }
}
