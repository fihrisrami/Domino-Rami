package com.example.network

import com.example.game.BoardEnd
import com.example.game.Team
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket

sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data object Connecting : ConnectionState()
    data class Connected(val assignedSeat: Int, val team: Team, val roomId: String) : ConnectionState()
    data class Failed(val reason: String) : ConnectionState()
}

/**
 * TCP Client connecting to the Host for Domino Rami multiplayer.
 */
class GameClient(
    private val scope: CoroutineScope,
    val playerId: String,
    val playerName: String
) {
    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private var readJob: Job? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _lobbyPlayers = MutableStateFlow<List<NetworkPlayerSummary>>(emptyList())
    val lobbyPlayers: StateFlow<List<NetworkPlayerSummary>> = _lobbyPlayers.asStateFlow()

    private val _gameState = MutableStateFlow<SanitizedGameState?>(null)
    val gameState: StateFlow<SanitizedGameState?> = _gameState.asStateFlow()

    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage: SharedFlow<String> = _errorMessage.asSharedFlow()

    fun connectToHost(hostIp: String, port: Int = DEFAULT_PORT) {
        _connectionState.value = ConnectionState.Connecting
        scope.launch(Dispatchers.IO) {
            try {
                val s = Socket()
                s.connect(InetSocketAddress(hostIp, port), 5000)
                socket = s

                val w = PrintWriter(s.getOutputStream(), true)
                writer = w
                val reader = BufferedReader(InputStreamReader(s.getInputStream(), Charsets.UTF_8))

                // Send Join Request
                w.println(NetworkPacketCodec.createJoinRequest(playerId, playerName))

                // Start reading responses
                readJob = scope.launch(Dispatchers.IO) {
                    try {
                        var line: String? = null
                        while (isActive && reader.readLine().also { line = it } != null) {
                            line?.let { parseIncomingMessage(it) }
                        }
                    } catch (_: Exception) {
                    } finally {
                        _connectionState.value = ConnectionState.Failed("انقطع الاتصال بالمضيف")
                    }
                }
            } catch (e: Exception) {
                _connectionState.value = ConnectionState.Failed(
                    "تعذر الاتصال بالمضيف ($hostIp). تأكد من الاتصال بنفس نقطة الاتصال Hotspot."
                )
            }
        }
    }

    private fun parseIncomingMessage(payload: String) {
        try {
            val json = JSONObject(payload)
            when (json.optString("type")) {
                MessageType.JOIN_ACCEPTED.name -> {
                    val seat = json.getInt("assignedSeat")
                    val team = Team.valueOf(json.getString("team"))
                    val roomId = json.getString("roomId")
                    _connectionState.value = ConnectionState.Connected(seat, team, roomId)
                }

                MessageType.JOIN_REJECTED.name -> {
                    val reason = json.getString("reason")
                    _connectionState.value = ConnectionState.Failed(reason)
                    disconnect()
                }

                MessageType.ROOM_STATE_UPDATE.name -> {
                    val playersArr = json.getJSONArray("players")
                    val list = mutableListOf<NetworkPlayerSummary>()
                    for (i in 0 until playersArr.length()) {
                        val p = playersArr.getJSONObject(i)
                        list.add(
                            NetworkPlayerSummary(
                                id = p.getString("id"),
                                name = p.getString("name"),
                                seatIndex = p.getInt("seat"),
                                team = Team.valueOf(p.getString("team")),
                                isHost = p.getBoolean("isHost"),
                                isConnected = p.getBoolean("isConnected"),
                                tileCount = p.optInt("tileCount", 0)
                            )
                        )
                    }
                    _lobbyPlayers.value = list
                }

                MessageType.GAME_STATE_SYNC.name -> {
                    val parsed = NetworkPacketCodec.parseSanitizedGameState(json)
                    _gameState.value = parsed
                }

                MessageType.MOVE_REJECTED.name -> {
                    val reason = json.optString("reason", "حركة غير مقبولة")
                    scope.launch { _errorMessage.emit(reason) }
                }
            }
        } catch (_: Exception) {}
    }

    fun sendMove(tileId: Int, boardEnd: BoardEnd) {
        scope.launch(Dispatchers.IO) {
            val packet = NetworkPacketCodec.createPlayMoveRequest(playerId, tileId, boardEnd)
            writer?.println(packet)
        }
    }

    fun sendPass() {
        scope.launch(Dispatchers.IO) {
            val packet = NetworkPacketCodec.createPassTurnRequest(playerId)
            writer?.println(packet)
        }
    }

    fun disconnect() {
        readJob?.cancel()
        readJob = null
        try {
            socket?.close()
        } catch (_: Exception) {}
        socket = null
        writer = null
        _connectionState.value = ConnectionState.Disconnected
    }
}
