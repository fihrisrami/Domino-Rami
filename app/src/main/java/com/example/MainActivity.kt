package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.MatchMode
import com.example.ui.screens.GameScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.HostLobbyScreen
import com.example.ui.screens.JoinLobbyScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.TutorialScreen
import com.example.ui.theme.DominoRamiTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DominoRamiTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DominoRamiApp(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun DominoRamiApp(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val matchMode by viewModel.matchMode.collectAsState()
    val activeGameState by viewModel.activeGameState.collectAsState()
    val hostLobbyPlayers by viewModel.hostLobbyPlayers.collectAsState()
    val clientLobbyPlayers by viewModel.clientLobbyPlayers.collectAsState()
    val clientConnectionState by viewModel.clientConnectionState.collectAsState()
    val discoveredRooms by viewModel.discoveredRooms.collectAsState()

    when (currentScreen) {
        AppScreen.SPLASH -> {
            SplashScreen()
        }

        AppScreen.HOME -> {
            HomeScreen(
                playerName = viewModel.preferences.playerName,
                onPlayerNameChange = { viewModel.updatePlayerName(it) },
                onNavigateToHostLobby = { viewModel.startHostingRoom() },
                onNavigateToJoinLobby = { viewModel.navigateTo(AppScreen.JOIN_LOBBY) },
                onNavigateToPractice = { viewModel.startPracticeMatch() },
                onNavigateToTutorial = { viewModel.navigateTo(AppScreen.TUTORIAL) },
                onNavigateToSettings = { viewModel.navigateTo(AppScreen.SETTINGS) }
            )
        }

        AppScreen.HOST_LOBBY -> {
            BackHandler {
                viewModel.leaveGame()
            }
            HostLobbyScreen(
                roomId = viewModel.currentRoomId,
                roomName = viewModel.currentRoomName,
                hostIp = viewModel.currentHostIp,
                players = hostLobbyPlayers,
                canStart = hostLobbyPlayers.size == 4,
                onAddBot = { viewModel.addBotToHostLobby() },
                onStartGame = { viewModel.startHostGame() },
                onLeaveLobby = { viewModel.leaveGame() }
            )
        }

        AppScreen.JOIN_LOBBY -> {
            BackHandler {
                viewModel.navigateTo(AppScreen.HOME)
            }
            JoinLobbyScreen(
                connectionState = clientConnectionState,
                discoveredRooms = discoveredRooms,
                lobbyPlayers = clientLobbyPlayers,
                onConnectToIp = { ip -> viewModel.connectAsClient(ip) },
                onLeaveLobby = { viewModel.leaveGame() }
            )
        }

        AppScreen.GAME -> {
            val isHost = matchMode == MatchMode.HOST || matchMode == MatchMode.OFFLINE_PRACTICE
            GameScreen(
                gameState = activeGameState,
                myPlayerId = viewModel.preferences.playerId,
                isHost = isHost,
                audioManager = viewModel.audioManager,
                hapticManager = viewModel.hapticManager,
                onMoveRequested = { tileId, end -> viewModel.makeMove(tileId, end) },
                onPassRequested = { viewModel.passTurn() },
                onNextRound = { viewModel.nextRound() },
                onLeaveGame = { viewModel.leaveGame() }
            )
        }

        AppScreen.TUTORIAL -> {
            BackHandler {
                viewModel.navigateTo(AppScreen.HOME)
            }
            TutorialScreen(
                onNavigateBack = { viewModel.navigateTo(AppScreen.HOME) }
            )
        }

        AppScreen.SETTINGS -> {
            BackHandler {
                viewModel.navigateTo(AppScreen.HOME)
            }
            SettingsScreen(
                preferences = viewModel.preferences,
                onNavigateBack = { viewModel.navigateTo(AppScreen.HOME) }
            )
        }
    }
}
