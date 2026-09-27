package com.example.game

/**
 * Orientation of a domino tile on the board or in hand.
 */
enum class DominoOrientation {
    VERTICAL,
    HORIZONTAL
}

/**
 * Represents a single standard Double-Six domino tile (0-0 to 6-6).
 * Values are between 0 and 6 inclusive.
 */
data class DominoTile(
    val id: Int,
    val leftValue: Int,
    val rightValue: Int,
    val orientation: DominoOrientation = DominoOrientation.VERTICAL
) {
    val isDouble: Boolean
        get() = leftValue == rightValue

    val totalPips: Int
        get() = leftValue + rightValue

    /**
     * Checks whether this tile contains the given value on either side.
     */
    fun hasValue(value: Int): Boolean = leftValue == value || rightValue == value

    /**
     * Returns a new tile with reversed ends if needed to match chain.
     */
    fun flipped(): DominoTile = copy(leftValue = rightValue, rightValue = leftValue)

    override fun toString(): String = "[$leftValue|$rightValue]"
}
