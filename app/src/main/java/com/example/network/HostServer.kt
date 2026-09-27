package com.example.network

import android.content.Context
import com.example.game.BoardEnd
import com.example.game.DominoBoard
import com.example.game.DominoTile
import com.example.game.GameEngine
import com.example.game.GameRules
import com.example.game.GameStatus
import com.example.game.LegalMove
import com.example.game.MoveResult
import com.example.game.PassResult
import com.example.game.Player
import com.example.game.RoundResult
import com.example.game.Team
import com.example.game.bot.DominoBot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

data class ConnectedClient(
    val playerId: String,
    val playerName: String,
    val seatIndex: Int,
    val team: Team,
    var socket: Socket?,
    var writer: PrintWriter?,
    var isConnected: Boolean = true,
    val isBot: Boolean = false
)

/**
 * Authoritative Host Server for local multiplayer Domino matches.
 */
class HostServer(
    private val scope: CoroutineScope,
    private val context: Context? = null,
    val roomId: String,
    val roomName: String,
    val hostPlayerId: String,
    val hostPlayerName: String,
    val rules: GameRules = GameRules.DEFAULT_FOUR_PLAYER_PARTNERSHIP
) {
    private var serverSocket: ServerSocket? = null
    private var acceptJob: Job? = null
    private var broadcaster: RoomBroadcaster? = null

    val gameEngine = GameEngine(rules)

    private val clients = ConcurrentHashMap<String, ConnectedClient>()

    // Local host's own view of the game
    private val _hostLocalState = MutableStateFlow<SanitizedGameState?>(null)
    val hostLocalState: StateFlow<SanitizedGameState?> = _hostLocalState.asStateFlow()

    private val _lobbyPlayers = MutableStateFlow<List<NetworkPlayerSummary>>(emptyList())
    val lobbyPlayers: StateFlow<List<NetworkPlayerSummary>> = _lobbyPlayers.asStateFlow()

    init {
        // Register host as Seat 0 (Team A)
        val hostClient = ConnectedClient(
            playerId = hostPlayerId,
            playerName = hostPlayerName,
            seatIndex = 0,
            team = Team.TEAM_A,
            socket = null,
            writer = null,
            isConnected = true
        )
        clients[hostPlayerId] = hostClient
        updateLobbySummary()
    }

    fun startServer(port: Int = DEFAULT_PORT): Boolean {
        return try {
            serverSocket = ServerSocket(port).apply {
                reuseAddress = true
            }
            startAcceptingClients()

            // Start UDP discovery broadcaster
            broadcaster = RoomBroadcaster(
                scope = scope,
                context = context,
                roomId = roomId,
                roomName = roomName,
                hostName = hostPlayerName,
                hostIp = NetworkUtils.getLocalIpAddress(),
                getPlayerCount = { clients.size }
            ).also { it.start() }

            true
        } catch (_: Exception) {
            false
        }
    }

    private fun startAcceptingClients() {
        acceptJob = scope.launch(Dispatchers.IO) {
            while (isActive && serverSocket != null && !serverSocket!!.isClosed) {
                try {
                    val socket = serverSocket?.accept() ?: break
                    handleNewSocket(socket)
                } catch (_: Exception) {
                    break
                }
            }
        }
    }

    private fun handleNewSocket(socket: Socket) {
        scope.launch(Dispatchers.IO) {
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
                val writer = PrintWriter(socket.getOutputStream(), true)

                val firstLine = reader.readLine() ?: return@launch
                val json = JSONObject(firstLine)
                if (json.optString("type") != MessageType.JOIN_REQUEST.name) {
                    writer.println(NetworkPacketCodec.createJoinRejected("طلب اتصال غير صالح"))
                    socket.close()
                    return@launch
                }

                val playerId = json.getString("playerId")
                val playerName = json.getString("playerName")

                synchronized(clients) {
                    // Check if reconnecting existing player
                    val existing = clients[playerId]
                    if (existing != null) {
                        existing.socket?.close()
                        existing.socket = socket
                        existing.writer = writer
                        existing.isConnected = true
                        writer.println(NetworkPacketCodec.createJoinAccepted(existing.seatIndex, existing.team, roomId))
                        syncPlayer(existing)
                        updateLobbySummary()
                        return@synchronized
                    }

                    // Check room full
                    if (clients.size >= rules.numberOfPlayers) {
                        writer.println(NetworkPacketCodec.createJoinRejected("الغرفة ممتلئة بالفعل"))
                        socket.close()
                        return@synchronized
                    }

                    // Assign next available seat
                    val takenSeats = clients.values.map { it.seatIndex }.toSet()
                    val availableSeat = (0 until rules.numberOfPlayers).first { it !in takenSeats }
                    val team = Team.fromIndex(availableSeat)

                    val newClient = ConnectedClient(
                        playerId = playerId,
                        playerName = playerName,
                        seatIndex = availableSeat,
                        team = team,
                        socket = socket,
                        writer = writer,
                        isConnected = true
                    )
                    clients[playerId] = newClient

                    writer.println(NetworkPacketCodec.createJoinAccepted(availableSeat, team, roomId))
                    updateLobbySummary()
                    broadcastRoomState()
                }

                // Listen for messages from this client
                var line: String? = null
                while (reader.readLine().also { line = it } != null) {
                    line?.let { handleClientMessage(playerId, it) }
                }
            } catch (_: Exception) {
            } finally {
                handleClientDisconnected(socket)
            }
        }
    }

    private fun handleClientDisconnected(socket: Socket) {
        val client = clients.values.find { it.socket == socket } ?: return
        client.isConnected = false
        client.socket = null
        client.writer = null
        updateLobbySummary()
        broadcastRoomState()
        broadcastGameState()
    }

    /**
     * Add bot players to fill empty seats for practice or fast testing.
     */
    fun addBotPlayer(): Boolean {
        synchronized(clients) {
            if (clients.size >= rules.numberOfPlayers) return false
            val takenSeats = clients.values.map { it.seatIndex }.toSet()
            val availableSeat = (0 until rules.numberOfPlayers).first { it !in takenSeats }
            val team = Team.fromIndex(availableSeat)
            val botNames = listOf("أحمد (روبوت)", "سامي (روبوت)", "عمر (روبوت)")
            val botName = botNames.getOrElse(availableSeat - 1) { "لاعب ذكي $availableSeat" }
            val botId = "bot_$availableSeat"

            val botClient = ConnectedClient(
                playerId = botId,
                playerName = botName,
                seatIndex = availableSeat,
                team = team,
                socket = null,
                writer = null,
                isConnected = true,
                isBot = true
            )
            clients[botId] = botClient
            updateLobbySummary()
            broadcastRoomState()
            return true
        }
    }

    fun canStartMatch(): Boolean = clients.size == rules.numberOfPlayers

    fun startMatch(): Boolean {
        if (!canStartMatch()) return false

        // Sort players by seat order: 0, 1, 2, 3
        val orderedPlayers = clients.values.sortedBy { it.seatIndex }.map { c ->
            Player(
                id = c.playerId,
                name = c.playerName,
                seatIndex = c.seatIndex,
                team = c.team,
                isHost = c.seatIndex == 0,
                isBot = c.isBot,
                isConnected = c.isConnected
            )
        }

        gameEngine.startNewMatch(orderedPlayers)
        broadcastGameState()
        triggerBotIfActive()
        return true
    }

    fun startNextRound() {
        if (gameEngine.status == GameStatus.ROUND_FINISHED) {
            gameEngine.startNewRound()
            broadcastGameState()
            triggerBotIfActive()
        }
    }

    private fun handleClientMessage(senderPlayerId: String, payload: String) {
        try {
            val json = JSONObject(payload)
            when (json.optString("type")) {
                MessageType.PLAY_MOVE_REQUEST.name -> {
                    val tileId = json.getInt("tileId")
                    val boardEnd = BoardEnd.valueOf(json.getString("boardEnd"))
                    executeMove(senderPlayerId, tileId, boardEnd)
                }
                MessageType.PASS_TURN_REQUEST.name -> {
                    executePass(senderPlayerId)
                }
            }
        } catch (_: Exception) {}
    }

    fun executeHostMove(tileId: Int, boardEnd: BoardEnd): MoveResult {
        return executeMove(hostPlayerId, tileId, boardEnd)
    }

    fun executeHostPass(): PassResult {
        return executePass(hostPlayerId)
    }

    private fun executeMove(playerId: String, tileId: Int, boardEnd: BoardEnd): MoveResult {
        synchronized(gameEngine) {
            val result = gameEngine.applyMove(playerId, tileId, boardEnd)
            if (result.success) {
                broadcastGameState()
                triggerBotIfActive()
            } else {
                sendToPlayer(playerId, NetworkPacketCodec.createMoveRejected(result.errorMessage ?: "حركة غير مقبولة"))
            }
            return result
        }
    }

    private fun executePass(playerId: String): PassResult {
        synchronized(gameEngine) {
            val result = gameEngine.passTurn(playerId)
            if (result.success) {
                broadcastGameState()
                triggerBotIfActive()
            }
            return result
        }
    }

    private fun triggerBotIfActive() {
        val activePlayer = gameEngine.getActivePlayer() ?: return
        val client = clients[activePlayer.id] ?: return
        if (client.isBot && gameEngine.status == GameStatus.IN_ROUND) {
            scope.launch(Dispatchers.Default) {
                delay(900) // Realistic domino think & play delay
                val hand = gameEngine.getPlayerHand(client.playerId)
                val legalMoves = gameEngine.getLegalMoves(client.playerId)
                if (legalMoves.isNotEmpty()) {
                    val decision = DominoBot.decideMove(hand, gameEngine.board, legalMoves)
                    if (decision != null) {
                        executeMove(client.playerId, decision.tileId, decision.end)
                    }
                } else {
                    executePass(client.playerId)
                }
            }
        }
    }

    private fun broadcastRoomState() {
        val playersList = getPlayerSummaries()
        val packet = NetworkPacketCodec.createRoomStateUpdate(
            roomId = roomId,
            roomName = roomName,
            hostId = hostPlayerId,
            players = playersList,
            canStart = canStartMatch()
        )
        clients.values.forEach { client ->
            client.writer?.println(packet)
        }
    }

    /**
     * Broadcasts sanitized game state to all players.
     * CRITICAL: Each player ONLY receives their own hand tiles.
     */
    fun broadcastGameState() {
        val playerSummaries = getPlayerSummaries()
        val status = gameEngine.status
        val round = gameEngine.currentRound
        val activeSeat = gameEngine.activeSeatIndex
        val boardTiles = gameEngine.board.tiles
        val openLeft = gameEngine.board.openLeftValue
        val openRight = gameEngine.board.openRightValue
        val teamA = gameEngine.teamScores[Team.TEAM_A] ?: 0
        val teamB = gameEngine.teamScores[Team.TEAM_B] ?: 0
        val lastRoundResult = gameEngine.lastRoundResult
        val activePlayer = gameEngine.getActivePlayer()

        val lastAction = when (status) {
            GameStatus.ROUND_FINISHED, GameStatus.MATCH_FINISHED -> lastRoundResult?.description ?: ""
            GameStatus.IN_ROUND -> "دور اللاعب: ${activePlayer?.name ?: ""}"
            else -> "في انتظار بدء الجولة"
        }

        // Send to each connected client
        clients.values.forEach { client ->
            val privateHand = gameEngine.getPlayerHand(client.playerId)
            val state = SanitizedGameState(
                status = status,
                roundNumber = round,
                activeSeatIndex = activeSeat,
                boardTiles = boardTiles,
                openLeftValue = openLeft,
                openRightValue = openRight,
                myHand = privateHand,
                players = playerSummaries,
                teamAScore = teamA,
                teamBScore = teamB,
                lastAction = lastAction,
                roundResult = lastRoundResult
            )

            if (client.playerId == hostPlayerId) {
                _hostLocalState.value = state
            } else {
                client.writer?.println(NetworkPacketCodec.createGameStateSync(state))
            }
        }
    }

    private fun syncPlayer(client: ConnectedClient) {
        val playerSummaries = getPlayerSummaries()
        val privateHand = gameEngine.getPlayerHand(client.playerId)
        val state = SanitizedGameState(
            status = gameEngine.status,
            roundNumber = gameEngine.currentRound,
            activeSeatIndex = gameEngine.activeSeatIndex,
            boardTiles = gameEngine.board.tiles,
            openLeftValue = gameEngine.board.openLeftValue,
            openRightValue = gameEngine.board.openRightValue,
            myHand = privateHand,
            players = playerSummaries,
            teamAScore = gameEngine.teamScores[Team.TEAM_A] ?: 0,
            teamBScore = gameEngine.teamScores[Team.TEAM_B] ?: 0,
            lastAction = "تم استرجاع حالة المباراة",
            roundResult = gameEngine.lastRoundResult
        )
        client.writer?.println(NetworkPacketCodec.createGameStateSync(state))
    }

    private fun sendToPlayer(playerId: String, packet: String) {
        clients[playerId]?.writer?.println(packet)
    }

    private fun getPlayerSummaries(): List<NetworkPlayerSummary> {
        return clients.values.sortedBy { it.seatIndex }.map { c ->
            NetworkPlayerSummary(
                id = c.playerId,
                name = c.playerName,
                seatIndex = c.seatIndex,
                team = c.team,
                isHost = c.seatIndex == 0,
                isConnected = c.isConnected,
                tileCount = gameEngine.getPlayerTileCount(c.playerId)
            )
        }
    }

    private fun updateLobbySummary() {
        _lobbyPlayers.value = getPlayerSummaries()
    }

    fun stopServer() {
        broadcaster?.stop()
        acceptJob?.cancel()
        clients.values.forEach {
            try {
                it.socket?.close()
            } catch (_: Exception) {}
        }
        clients.clear()
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
    }
}
