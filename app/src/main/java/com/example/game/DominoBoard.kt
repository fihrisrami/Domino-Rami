package com.example.game

enum class BoardEnd {
    LEFT,
    RIGHT
}

/**
 * A tile placed on the board chain.
 * [placedLeftValue] is the value pointing towards the left of the chain.
 * [placedRightValue] is the value pointing towards the right of the chain.
 */
data class PlacedTile(
    val tile: DominoTile,
    val playedByPlayerId: String,
    val placedLeftValue: Int,
    val placedRightValue: Int,
    val isPerpendicular: Boolean, // True for doubles
    val boardEnd: BoardEnd? = null // Which end it was placed on (null for the first lead tile)
)

data class DominoBoard(
    val tiles: List<PlacedTile> = emptyList(),
    val openLeftValue: Int? = null,
    val openRightValue: Int? = null
) {
    val isEmpty: Boolean get() = tiles.isEmpty()
    val size: Int get() = tiles.size

    /**
     * Checks if a tile can be played at all on this board.
     */
    fun canPlayTile(tile: DominoTile): Boolean {
        if (isEmpty) return true
        val left = openLeftValue ?: return true
        val right = openRightValue ?: return true
        return tile.hasValue(left) || tile.hasValue(right)
    }

    /**
     * Checks if a tile can be played on a specific end.
     */
    fun canPlayOnEnd(tile: DominoTile, end: BoardEnd): Boolean {
        if (isEmpty) return true
        val target = if (end == BoardEnd.LEFT) openLeftValue else openRightValue
        return target != null && tile.hasValue(target)
    }

    /**
     * Returns the playable ends for a given tile.
     */
    fun getPlayableEnds(tile: DominoTile): List<BoardEnd> {
        if (isEmpty) return listOf(BoardEnd.RIGHT) // or LEFT, either is fine for first
        val list = mutableListOf<BoardEnd>()
        if (openLeftValue != null && tile.hasValue(openLeftValue)) {
            list.add(BoardEnd.LEFT)
        }
        if (openRightValue != null && tile.hasValue(openRightValue) && !list.contains(BoardEnd.RIGHT)) {
            list.add(BoardEnd.RIGHT)
        }
        return list
    }

    /**
     * Places a tile on the board and returns an updated DominoBoard.
     */
    fun placeTile(tile: DominoTile, end: BoardEnd, playerId: String): DominoBoard {
        if (isEmpty) {
            val placed = PlacedTile(
                tile = tile,
                playedByPlayerId = playerId,
                placedLeftValue = tile.leftValue,
                placedRightValue = tile.rightValue,
                isPerpendicular = tile.isDouble,
                boardEnd = null
            )
            return DominoBoard(
                tiles = listOf(placed),
                openLeftValue = tile.leftValue,
                openRightValue = tile.rightValue
            )
        }

        val newTiles = tiles.toMutableList()
        var newLeft = openLeftValue!!
        var newRight = openRightValue!!

        if (end == BoardEnd.LEFT) {
            // The tile connects to openLeftValue.
            // If tile.rightValue matches newLeft: leftValue becomes new openLeft.
            // If tile.leftValue matches newLeft: rightValue becomes new openLeft, flipped.
            val (tileLeft, tileRight) = if (tile.rightValue == newLeft) {
                Pair(tile.leftValue, tile.rightValue)
            } else {
                Pair(tile.rightValue, tile.leftValue)
            }
            val placed = PlacedTile(
                tile = tile,
                playedByPlayerId = playerId,
                placedLeftValue = tileLeft,
                placedRightValue = tileRight,
                isPerpendicular = tile.isDouble,
                boardEnd = BoardEnd.LEFT
            )
            newTiles.add(0, placed)
            newLeft = tileLeft
        } else {
            // The tile connects to openRightValue on the right.
            // If tile.leftValue matches newRight: rightValue becomes new openRight.
            // If tile.rightValue matches newRight: leftValue becomes new openRight, flipped.
            val (tileLeft, tileRight) = if (tile.leftValue == newRight) {
                Pair(tile.leftValue, tile.rightValue)
            } else {
                Pair(tile.rightValue, tile.leftValue)
            }
            val placed = PlacedTile(
                tile = tile,
                playedByPlayerId = playerId,
                placedLeftValue = tileLeft,
                placedRightValue = tileRight,
                isPerpendicular = tile.isDouble,
                boardEnd = BoardEnd.RIGHT
            )
            newTiles.add(placed)
            newRight = tileRight
        }

        return DominoBoard(
            tiles = newTiles,
            openLeftValue = newLeft,
            openRightValue = newRight
        )
    }
}
