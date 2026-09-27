package com.example.game

/**
 * Generates and manages the standard 28-piece Double-Six Domino set.
 * Each tile is unique with no duplicates (0-0 through 6-6).
 */
object DominoSet {
    const val MAX_PIP = 6
    const val TOTAL_TILES_COUNT = 28

    /**
     * Creates a pristine, un-shuffled list of all 28 tiles.
     */
    fun createFullSet(): List<DominoTile> {
        val tiles = mutableListOf<DominoTile>()
        var id = 1
        for (i in 0..MAX_PIP) {
            for (j in i..MAX_PIP) {
                tiles.add(
                    DominoTile(
                        id = id++,
                        leftValue = i,
                        rightValue = j,
                        orientation = if (i == j) DominoOrientation.HORIZONTAL else DominoOrientation.VERTICAL
                    )
                )
            }
        }
        return tiles
    }

    /**
     * Checks if a tile is the double-six [6|6].
     */
    fun isDoubleSix(tile: DominoTile): Boolean = tile.leftValue == 6 && tile.rightValue == 6
}
