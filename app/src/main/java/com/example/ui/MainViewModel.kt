package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.DominoAudioManager
import com.example.audio.DominoHapticManager
import com.example.game.BoardEnd
import com.example.game.GameRules
import com.example.network.ConnectionState
import com.example.network.DEFAULT_PORT
import com.example.network.DiscoveredRoom
import com.example.network.GameClient
import com.example.network.HostServer
import com.example.network.NetworkPlayerSummary
import com.example.network.NetworkUtils
import com.example.network.RoomScanner
import com.example.network.SanitizedGameState
import com.example.storage.GamePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

enum class AppScreen {
    SPLASH,
    HOME,
    HOST_LOBBY,
    JOIN_LOBBY,
    GAME,
    TUTORIAL,
    SETTINGS
}

enum class MatchMode {
    NONE,
    HOST,
    CLIENT,
    OFFLINE_PRACTICE
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val preferences = GamePreferences(application)
    val audioManager = DominoAudioManager(application, viewModelScope).apply {
        isSfxEnabled = preferences.isSfxEnabled
    }
    val hapticManager = DominoHapticManager(application).apply {
        isHapticsEnabled = preferences.isHapticsEnabled
    }

    private val _currentScreen = MutableStateFlow(AppScreen.SPLASH)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _matchMode = MutableStateFlow(MatchMode.NONE)
    val matchMode: StateFlow<MatchMode> = _matchMode.asStateFlow()

    // Host components
    var hostServer: HostServer? = null
        private set

    // Client components
    var gameClient: GameClient? = null
        private set

    val roomScanner = RoomScanner(viewModelScope, application)

    // Current Game State exposed to Compose
    private val _activeGameState = MutableStateFlow<SanitizedGameState?>(null)
    val activeGameState: StateFlow<SanitizedGameState?> = _activeGameState.asStateFlow()

    private val _hostLobbyPlayers = MutableStateFlow<List<NetworkPlayerSummary>>(emptyList())
    val hostLobbyPlayers: StateFlow<List<NetworkPlayerSummary>> = _hostLobbyPlayers.asStateFlow()

    private val _clientLobbyPlayers = MutableStateFlow<List<NetworkPlayerSummary>>(emptyList())
    val clientLobbyPlayers: StateFlow<List<NetworkPlayerSummary>> = _clientLobbyPlayers.asStateFlow()

    private val _clientConnectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val clientConnectionState: StateFlow<ConnectionState> = _clientConnectionState.asStateFlow()

    val discoveredRooms: StateFlow<List<DiscoveredRoom>> = roomScanner.discoveredRooms

    var currentRoomId: String = ""
        private set
    var currentRoomName: String = ""
        private set
    var currentHostIp: String = ""
        private set

    init {
        // Observe preferences changes
        viewModelScope.launch {
            // Splash transition after short delay
            kotlinx.coroutines.delay(1200)
            if (_currentScreen.value == AppScreen.SPLASH) {
                _currentScreen.value = AppScreen.HOME
            }
        }
    }

    fun navigateTo(screen: AppScreen) {
        if (screen == AppScreen.JOIN_LOBBY) {
            roomScanner.startScanning()
        } else if (_currentScreen.value == AppScreen.JOIN_LOBBY && screen != AppScreen.JOIN_LOBBY) {
            roomScanner.stopScanning()
        }
        _currentScreen.value = screen
    }

    fun updatePlayerName(name: String) {
        preferences.playerName = name
    }

    /**
     * Start hosting a local multiplayer room.
     */
    fun startHostingRoom() {
        cleanupNetwork()
        val randomSuffix = (1000..9999).random()
        currentRoomId = "RAMI-$randomSuffix"
        currentRoomName = "غرفة ${preferences.playerName}"
        currentHostIp = NetworkUtils.getLocalIpAddress()

        val rules = GameRules.DEFAULT_FOUR_PLAYER_PARTNERSHIP.copy(targetScore = preferences.targetScore)

        val server = HostServer(
            scope = viewModelScope,
            context = getApplication(),
            roomId = currentRoomId,
            roomName = currentRoomName,
            hostPlayerId = preferences.playerId,
            hostPlayerName = preferences.playerName,
            rules = rules
        )

        val started = server.startServer(DEFAULT_PORT)
        if (started) {
            hostServer = server
            _matchMode.value = MatchMode.HOST

            viewModelScope.launch {
                server.lobbyPlayers.collect { _hostLobbyPlayers.value = it }
            }
            viewModelScope.launch {
                server.hostLocalState.collect { state ->
                    _activeGameState.value = state
                    if (state != null && _currentScreen.value == AppScreen.HOST_LOBBY) {
                        _currentScreen.value = AppScreen.GAME
                    }
                }
            }

            _currentScreen.value = AppScreen.HOST_LOBBY
        }
    }

    /**
     * Start an offline practice match with 3 intelligent bots.
     */
    fun startPracticeMatch() {
        cleanupNetwork()
        currentRoomId = "PRACTICE"
        currentRoomName = "مباراة تدريبية"
        currentHostIp = "127.0.0.1"

        val rules = GameRules.DEFAULT_FOUR_PLAYER_PARTNERSHIP.copy(targetScore = 50)

        val server = HostServer(
            scope = viewModelScope,
            context = getApplication(),
            roomId = currentRoomId,
            roomName = currentRoomName,
            hostPlayerId = preferences.playerId,
            hostPlayerName = preferences.playerName,
            rules = rules
        )

        // Add 3 Bots
        server.addBotPlayer() // Seat 1
        server.addBotPlayer() // Seat 2 (Partner)
        server.addBotPlayer() // Seat 3

        hostServer = server
        _matchMode.value = MatchMode.OFFLINE_PRACTICE

        viewModelScope.launch {
            server.hostLocalState.collect { _activeGameState.value = it }
        }

        server.startMatch()
        _currentScreen.value = AppScreen.GAME
    }

    /**
     * Host adds a bot to fill a seat in multiplayer.
     */
    fun addBotToHostLobby() {
        hostServer?.addBotPlayer()
    }

    /**
     * Host starts the game.
     */
    fun startHostGame() {
        val started = hostServer?.startMatch() ?: false
        if (started) {
            _currentScreen.value = AppScreen.GAME
        }
    }

    /**
     * Client connects to host by IP.
     */
    fun connectAsClient(hostIp: String, port: Int = DEFAULT_PORT) {
        cleanupNetwork()
        _matchMode.value = MatchMode.CLIENT

        val client = GameClient(
            scope = viewModelScope,
            playerId = preferences.playerId,
            playerName = preferences.playerName
        )
        gameClient = client

        viewModelScope.launch {
            client.connectionState.collect { _clientConnectionState.value = it }
        }
        viewModelScope.launch {
            client.lobbyPlayers.collect { _clientLobbyPlayers.value = it }
        }
        viewModelScope.launch {
            client.gameState.collect { state ->
                _activeGameState.value = state
                if (state != null && _currentScreen.value == AppScreen.JOIN_LOBBY) {
                    _currentScreen.value = AppScreen.GAME
                }
            }
        }

        client.connectToHost(hostIp, port)
    }

    fun makeMove(tileId: Int, end: BoardEnd) {
        when (_matchMode.value) {
            MatchMode.HOST, MatchMode.OFFLINE_PRACTICE -> {
                hostServer?.executeHostMove(tileId, end)
            }
            MatchMode.CLIENT -> {
                gameClient?.sendMove(tileId, end)
            }
            else -> {}
        }
    }

    fun passTurn() {
        when (_matchMode.value) {
            MatchMode.HOST, MatchMode.OFFLINE_PRACTICE -> {
                hostServer?.executeHostPass()
            }
            MatchMode.CLIENT -> {
                gameClient?.sendPass()
            }
            else -> {}
        }
    }

    fun nextRound() {
        hostServer?.startNextRound()
    }

    fun leaveGame() {
        cleanupNetwork()
        _activeGameState.value = null
        _matchMode.value = MatchMode.NONE
        _currentScreen.value = AppScreen.HOME
    }

    private fun cleanupNetwork() {
        roomScanner.stopScanning()
        hostServer?.stopServer()
        hostServer = null
        gameClient?.disconnect()
        gameClient = null
    }

    override fun onCleared() {
        super.onCleared()
        cleanupNetwork()
    }
}
