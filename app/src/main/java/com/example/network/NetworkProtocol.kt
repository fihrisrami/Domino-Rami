package com.example.network

import com.example.game.BoardEnd
import com.example.game.DominoBoard
import com.example.game.DominoTile
import com.example.game.GameRules
import com.example.game.GameStatus
import com.example.game.PlacedTile
import com.example.game.RoundResult
import com.example.game.Team
import org.json.JSONArray
import org.json.JSONObject

/**
 * Protocol version for backwards-compatibility checks.
 */
const val PROTOCOL_VERSION = 1
const val DEFAULT_PORT = 44556
const val DISCOVERY_PORT = 44555

enum class MessageType {
    JOIN_REQUEST,
    JOIN_ACCEPTED,
    JOIN_REJECTED,
    ROOM_STATE_UPDATE,
    START_GAME_REQUEST,
    GAME_STATE_SYNC,
    PLAY_MOVE_REQUEST,
    PASS_TURN_REQUEST,
    MOVE_REJECTED,
    PLAYER_DISCONNECTED,
    PLAYER_RECONNECTED,
    PING,
    PONG,
    LEAVE_ROOM
}

data class DiscoveredRoom(
    val roomId: String,
    val roomName: String,
    val hostName: String,
    val hostIp: String,
    val port: Int = DEFAULT_PORT,
    val currentPlayers: Int,
    val maxPlayers: Int = 4,
    val lastSeenTimestamp: Long = System.currentTimeMillis()
)

data class NetworkPlayerSummary(
    val id: String,
    val name: String,
    val seatIndex: Int,
    val team: Team,
    val isHost: Boolean,
    val isConnected: Boolean,
    val tileCount: Int
)

/**
 * Sanitized game state sent to a specific player.
 * NEVER leaks other players' private tile values.
 */
data class SanitizedGameState(
    val status: GameStatus,
    val roundNumber: Int,
    val activeSeatIndex: Int,
    val boardTiles: List<PlacedTile>,
    val openLeftValue: Int?,
    val openRightValue: Int?,
    val myHand: List<DominoTile>,
    val players: List<NetworkPlayerSummary>,
    val teamAScore: Int,
    val teamBScore: Int,
    val lastAction: String,
    val roundResult: RoundResult? = null
)

/**
 * Robust JSON serialization and deserialization for Domino Rami network packets.
 */
object NetworkPacketCodec {

    fun createJoinRequest(playerId: String, playerName: String): String {
        return JSONObject().apply {
            put("type", MessageType.JOIN_REQUEST.name)
            put("version", PROTOCOL_VERSION)
            put("playerId", playerId)
            put("playerName", playerName)
        }.toString()
    }

    fun createJoinAccepted(assignedSeat: Int, team: Team, roomId: String): String {
        return JSONObject().apply {
            put("type", MessageType.JOIN_ACCEPTED.name)
            put("assignedSeat", assignedSeat)
            put("team", team.name)
            put("roomId", roomId)
        }.toString()
    }

    fun createJoinRejected(reason: String): String {
        return JSONObject().apply {
            put("type", MessageType.JOIN_REJECTED.name)
            put("reason", reason)
        }.toString()
    }

    fun createRoomStateUpdate(
        roomId: String,
        roomName: String,
        hostId: String,
        players: List<NetworkPlayerSummary>,
        canStart: Boolean
    ): String {
        val playersArray = JSONArray()
        players.forEach { p ->
            playersArray.put(JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("seat", p.seatIndex)
                put("team", p.team.name)
                put("isHost", p.isHost)
                put("isConnected", p.isConnected)
                put("tileCount", p.tileCount)
            })
        }
        return JSONObject().apply {
            put("type", MessageType.ROOM_STATE_UPDATE.name)
            put("roomId", roomId)
            put("roomName", roomName)
            put("hostId", hostId)
            put("players", playersArray)
            put("canStart", canStart)
        }.toString()
    }

    fun createStartGameRequest(): String {
        return JSONObject().apply {
            put("type", MessageType.START_GAME_REQUEST.name)
        }.toString()
    }

    fun createPlayMoveRequest(playerId: String, tileId: Int, boardEnd: BoardEnd): String {
        return JSONObject().apply {
            put("type", MessageType.PLAY_MOVE_REQUEST.name)
            put("playerId", playerId)
            put("tileId", tileId)
            put("boardEnd", boardEnd.name)
        }.toString()
    }

    fun createPassTurnRequest(playerId: String): String {
        return JSONObject().apply {
            put("type", MessageType.PASS_TURN_REQUEST.name)
            put("playerId", playerId)
        }.toString()
    }

    fun createMoveRejected(reason: String): String {
        return JSONObject().apply {
            put("type", MessageType.MOVE_REJECTED.name)
            put("reason", reason)
        }.toString()
    }

    fun createGameStateSync(state: SanitizedGameState): String {
        val json = JSONObject()
        json.put("type", MessageType.GAME_STATE_SYNC.name)
        json.put("status", state.status.name)
        json.put("roundNumber", state.roundNumber)
        json.put("activeSeatIndex", state.activeSeatIndex)
        json.put("openLeft", state.openLeftValue ?: -1)
        json.put("openRight", state.openRightValue ?: -1)
        json.put("teamAScore", state.teamAScore)
        json.put("teamBScore", state.teamBScore)
        json.put("lastAction", state.lastAction)

        // Board tiles
        val boardArr = JSONArray()
        state.boardTiles.forEach { pt ->
            boardArr.put(JSONObject().apply {
                put("id", pt.tile.id)
                put("left", pt.tile.leftValue)
                put("right", pt.tile.rightValue)
                put("placedLeft", pt.placedLeftValue)
                put("placedRight", pt.placedRightValue)
                put("isDouble", pt.isPerpendicular)
                put("playerId", pt.playedByPlayerId)
            })
        }
        json.put("board", boardArr)

        // Player's secret private hand
        val handArr = JSONArray()
        state.myHand.forEach { t ->
            handArr.put(JSONObject().apply {
                put("id", t.id)
                put("left", t.leftValue)
                put("right", t.rightValue)
            })
        }
        json.put("myHand", handArr)

        // Player summaries (public info only)
        val playersArr = JSONArray()
        state.players.forEach { p ->
            playersArr.put(JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("seat", p.seatIndex)
                put("team", p.team.name)
                put("isHost", p.isHost)
                put("isConnected", p.isConnected)
                put("tileCount", p.tileCount)
            })
        }
        json.put("players", playersArr)

        // Round result if ended
        state.roundResult?.let { rr ->
            val rrObj = JSONObject().apply {
                put("reason", rr.reason.name)
                put("winnerId", rr.winningPlayerId ?: "")
                put("winnerTeam", rr.winningTeam?.name ?: "")
                put("points", rr.pointsEarned)
                put("isMatchOver", rr.isMatchOver)
                put("description", rr.description)
            }
            json.put("roundResult", rrObj)
        }

        return json.toString()
    }

    fun parseSanitizedGameState(json: JSONObject): SanitizedGameState {
        val status = GameStatus.valueOf(json.optString("status", GameStatus.IN_ROUND.name))
        val roundNumber = json.optInt("roundNumber", 1)
        val activeSeatIndex = json.optInt("activeSeatIndex", 0)
        val openLeftRaw = json.optInt("openLeft", -1)
        val openRightRaw = json.optInt("openRight", -1)
        val openLeft = if (openLeftRaw >= 0) openLeftRaw else null
        val openRight = if (openRightRaw >= 0) openRightRaw else null
        val teamAScore = json.optInt("teamAScore", 0)
        val teamBScore = json.optInt("teamBScore", 0)
        val lastAction = json.optString("lastAction", "")

        val boardTiles = mutableListOf<PlacedTile>()
        val boardArr = json.optJSONArray("board") ?: JSONArray()
        for (i in 0 until boardArr.length()) {
            val item = boardArr.getJSONObject(i)
            val tile = DominoTile(
                id = item.getInt("id"),
                leftValue = item.getInt("left"),
                rightValue = item.getInt("right")
            )
            boardTiles.add(
                PlacedTile(
                    tile = tile,
                    playedByPlayerId = item.optString("playerId", ""),
                    placedLeftValue = item.getInt("placedLeft"),
                    placedRightValue = item.getInt("placedRight"),
                    isPerpendicular = item.optBoolean("isDouble", tile.isDouble)
                )
            )
        }

        val myHand = mutableListOf<DominoTile>()
        val handArr = json.optJSONArray("myHand") ?: JSONArray()
        for (i in 0 until handArr.length()) {
            val item = handArr.getJSONObject(i)
            myHand.add(
                DominoTile(
                    id = item.getInt("id"),
                    leftValue = item.getInt("left"),
                    rightValue = item.getInt("right")
                )
            )
        }

        val players = mutableListOf<NetworkPlayerSummary>()
        val playersArr = json.optJSONArray("players") ?: JSONArray()
        for (i in 0 until playersArr.length()) {
            val item = playersArr.getJSONObject(i)
            players.add(
                NetworkPlayerSummary(
                    id = item.getString("id"),
                    name = item.getString("name"),
                    seatIndex = item.getInt("seat"),
                    team = Team.valueOf(item.getString("team")),
                    isHost = item.optBoolean("isHost", false),
                    isConnected = item.optBoolean("isConnected", true),
                    tileCount = item.optInt("tileCount", 0)
                )
            )
        }

        var roundResult: RoundResult? = null
        if (json.has("roundResult")) {
            val rrObj = json.getJSONObject("roundResult")
            val reason = com.example.game.RoundFinishReason.valueOf(rrObj.getString("reason"))
            val winnerId = rrObj.optString("winnerId", "").ifEmpty { null }
            val winnerTeamRaw = rrObj.optString("winnerTeam", "")
            val winnerTeam = if (winnerTeamRaw.isNotEmpty()) Team.valueOf(winnerTeamRaw) else null
            val points = rrObj.optInt("points", 0)
            val isMatchOver = rrObj.optBoolean("isMatchOver", false)
            val desc = rrObj.optString("description", "")
            roundResult = RoundResult(
                reason = reason,
                winningPlayerId = winnerId,
                winningTeam = winnerTeam,
                pointsEarned = points,
                isMatchOver = isMatchOver,
                matchWinningTeam = if (isMatchOver) winnerTeam else null,
                pipsBreakdown = emptyMap(),
                description = desc
            )
        }

        return SanitizedGameState(
            status = status,
            roundNumber = roundNumber,
            activeSeatIndex = activeSeatIndex,
            boardTiles = boardTiles,
            openLeftValue = openLeft,
            openRightValue = openRight,
            myHand = myHand,
            players = players,
            teamAScore = teamAScore,
            teamBScore = teamBScore,
            lastAction = lastAction,
            roundResult = roundResult
        )
    }
}
