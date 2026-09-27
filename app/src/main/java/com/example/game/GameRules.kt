package com.example.game

enum class StartingRule {
    SIX_SIX_FIRST_ROUND, // Standard: [6|6] starts round 1; subsequent rounds winner starts
    HIGHEST_DOUBLE,      // Player with highest double starts
    PREVIOUS_WINNER      // Winner of previous round starts
}

data class GameRules(
    val numberOfPlayers: Int = 4,
    val isPartnership: Boolean = true,
    val startingRule: StartingRule = StartingRule.SIX_SIX_FIRST_ROUND,
    val targetScore: Int = 100,
    val tilesPerPlayer: Int = 7
) {
    companion object {
        val DEFAULT_FOUR_PLAYER_PARTNERSHIP = GameRules(
            numberOfPlayers = 4,
            isPartnership = true,
            startingRule = StartingRule.SIX_SIX_FIRST_ROUND,
            targetScore = 100,
            tilesPerPlayer = 7
        )

        val TWO_PLAYER_PRACTICE = GameRules(
            numberOfPlayers = 2,
            isPartnership = false,
            startingRule = StartingRule.SIX_SIX_FIRST_ROUND,
            targetScore = 50,
            tilesPerPlayer = 7
        )
    }
}
