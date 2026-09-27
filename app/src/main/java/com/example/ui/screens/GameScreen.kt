package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.audio.DominoAudioManager
import com.example.audio.DominoHapticManager
import com.example.audio.DominoSound
import com.example.audio.HapticType
import com.example.game.BoardEnd
import com.example.game.DominoTile
import com.example.game.GameStatus
import com.example.network.SanitizedGameState
import com.example.ui.components.DominoTableView
import com.example.ui.components.RoundEndDialog

@Composable
fun GameScreen(
    gameState: SanitizedGameState?,
    myPlayerId: String,
    isHost: Boolean,
    audioManager: DominoAudioManager,
    hapticManager: DominoHapticManager,
    onMoveRequested: (tileId: Int, boardEnd: BoardEnd) -> Unit,
    onPassRequested: () -> Unit,
    onNextRound: () -> Unit,
    onLeaveGame: () -> Unit
) {
    if (gameState == null) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0C2B1E)))
        return
    }

    var selectedTile by remember { mutableStateOf<DominoTile?>(null) }
    var showExitDialog by remember { mutableStateOf(false) }

    // Intercept back button to prompt confirmation
    BackHandler {
        showExitDialog = true
    }

    // Play cues on turn or status changes
    val mySummary = gameState.players.find { it.id == myPlayerId }
    val isMyTurn = mySummary != null && gameState.activeSeatIndex == mySummary.seatIndex

    LaunchedEffect(gameState.activeSeatIndex) {
        if (isMyTurn && gameState.status == GameStatus.IN_ROUND) {
            audioManager.playSound(DominoSound.YOUR_TURN)
            hapticManager.trigger(HapticType.MEDIUM_TAP)
        }
    }

    LaunchedEffect(gameState.status) {
        if (gameState.status == GameStatus.ROUND_FINISHED || gameState.status == GameStatus.MATCH_FINISHED) {
            val winTeam = gameState.roundResult?.winningTeam
            if (winTeam != null && mySummary != null && winTeam == mySummary.team) {
                audioManager.playSound(DominoSound.ROUND_WIN)
                hapticManager.trigger(HapticType.SUCCESS_BUZZ)
            } else {
                audioManager.playSound(DominoSound.ROUND_LOSS)
                hapticManager.trigger(HapticType.ERROR_WARN)
            }
        }
    }

    // Deselect tile if turn changes or tile is no longer in hand
    LaunchedEffect(gameState.myHand) {
        if (selectedTile != null && gameState.myHand.none { it.id == selectedTile!!.id }) {
            selectedTile = null
        }
    }

    DominoTableView(
        gameState = gameState,
        myPlayerId = myPlayerId,
        selectedTile = selectedTile,
        onTileSelected = { tile ->
            selectedTile = if (selectedTile?.id == tile.id) null else tile
            audioManager.playSound(DominoSound.TILE_SELECT)
            hapticManager.trigger(HapticType.LIGHT_TICK)
        },
        onPlaceTile = { tile, end ->
            if (tile.isDouble) {
                audioManager.playSound(DominoSound.TILE_PLACE_DOUBLE)
                hapticManager.trigger(HapticType.HEAVY_CLICK)
            } else {
                audioManager.playSound(DominoSound.TILE_PLACE)
                hapticManager.trigger(HapticType.MEDIUM_TAP)
            }
            onMoveRequested(tile.id, end)
            selectedTile = null
        },
        onPassTurn = {
            audioManager.playSound(DominoSound.ERROR_BUMP)
            hapticManager.trigger(HapticType.MEDIUM_TAP)
            onPassRequested()
            selectedTile = null
        }
    )

    // Round / Match Over Dialog
    if (gameState.status == GameStatus.ROUND_FINISHED || gameState.status == GameStatus.MATCH_FINISHED) {
        gameState.roundResult?.let { rr ->
            RoundEndDialog(
                roundResult = rr,
                teamAScore = gameState.teamAScore,
                teamBScore = gameState.teamBScore,
                isHost = isHost,
                onNextRound = onNextRound,
                onLeaveGame = onLeaveGame
            )
        }
    }

    // Confirm Exit Dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("مغادرة المباراة") },
            text = { Text("هل أنت متأكد من رغبتك في مغادرة طاولة اللعب الحالية؟") },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        onLeaveGame()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) {
                    Text("نعم، مغادرة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
