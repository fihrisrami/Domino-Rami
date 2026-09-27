package com.example.game

enum class Team(val id: Int, val displayNameArabic: String, val displayNameEnglish: String) {
    TEAM_A(0, "فريق أ", "Team A"),
    TEAM_B(1, "فريق ب", "Team B");

    companion object {
        fun fromIndex(index: Int): Team = if (index % 2 == 0) TEAM_A else TEAM_B
    }
}

/**
 * Represents a player in the domino match.
 */
data class Player(
    val id: String,
    val name: String,
    val seatIndex: Int, // 0 to 3
    val team: Team,
    val isHost: Boolean = false,
    val isBot: Boolean = false,
    val isConnected: Boolean = true,
    val tileCount: Int = 0 // Used by clients when opponent tiles are hidden
) {
    val isPartnerWithSeat: (Int) -> Boolean = { otherSeat ->
        (seatIndex % 2) == (otherSeat % 2)
    }
}
