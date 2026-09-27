package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.BoardEnd
import com.example.game.DominoTile
import com.example.game.LegalMove
import com.example.game.Team
import com.example.network.NetworkPlayerSummary
import com.example.network.SanitizedGameState

// Rich luxury table colors
val TableFeltDark = Color(0xFF0C2B1E)
val TableFeltCenter = Color(0xFF154834)
val TableWoodBorder = Color(0xFF381B10)
val TableWoodHighlight = Color(0xFF5B3020)
val TeamAGold = Color(0xFFFFB300)
val TeamBOrange = Color(0xFFFF7043)
val AccentGreen = Color(0xFF4CAF50)

@Composable
fun DominoTableView(
    gameState: SanitizedGameState,
    myPlayerId: String,
    selectedTile: DominoTile?,
    onTileSelected: (DominoTile) -> Unit,
    onPlaceTile: (DominoTile, BoardEnd) -> Unit,
    onPassTurn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mySummary = gameState.players.find { it.id == myPlayerId }
    val mySeat = mySummary?.seatIndex ?: 0
    val isMyTurn = gameState.activeSeatIndex == mySeat

    // Arrange opponents relative to me in 4-player format:
    // Left = (mySeat + 1) % 4
    // Top (Partner) = (mySeat + 2) % 4
    // Right = (mySeat + 3) % 4
    val leftOpponent = gameState.players.find { it.seatIndex == (mySeat + 1) % 4 }
    val partner = gameState.players.find { it.seatIndex == (mySeat + 2) % 4 }
    val rightOpponent = gameState.players.find { it.seatIndex == (mySeat + 3) % 4 }

    val activePlayer = gameState.players.find { it.seatIndex == gameState.activeSeatIndex }

    // Check legal moves for my hand
    val playableEndsForSelected = if (selectedTile != null && isMyTurn) {
        if (gameState.boardTiles.isEmpty()) {
            listOf(BoardEnd.RIGHT)
        } else {
            val ends = mutableListOf<BoardEnd>()
            if (gameState.openLeftValue != null && selectedTile.hasValue(gameState.openLeftValue)) {
                ends.add(BoardEnd.LEFT)
            }
            if (gameState.openRightValue != null && selectedTile.hasValue(gameState.openRightValue) && !ends.contains(BoardEnd.RIGHT)) {
                ends.add(BoardEnd.RIGHT)
            }
            ends
        }
    } else emptyList()

    // Can I pass? If it is my turn, and NO tiles in my hand can be played
    val canPass = isMyTurn && gameState.myHand.isNotEmpty() && gameState.myHand.none { tile ->
        if (gameState.boardTiles.isEmpty()) true
        else {
            val left = gameState.openLeftValue ?: -1
            val right = gameState.openRightValue ?: -1
            tile.hasValue(left) || tile.hasValue(right)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(TableFeltCenter, TableFeltDark),
                    radius = 1200f
                )
            )
            .border(8.dp, TableWoodBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TOP HUD: Scoreboard & Partner
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ScoreHeader(
                    teamAScore = gameState.teamAScore,
                    teamBScore = gameState.teamBScore,
                    roundNumber = gameState.roundNumber
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Partner badge (Top)
                partner?.let {
                    PlayerAvatarBadge(
                        player = it,
                        isActive = gameState.activeSeatIndex == it.seatIndex,
                        isPartner = true
                    )
                }
            }

            // MIDDLE AREA: Left Opponent + Domino Chain Board + Right Opponent
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Opponent
                Box(modifier = Modifier.width(72.dp), contentAlignment = Alignment.Center) {
                    leftOpponent?.let {
                        PlayerAvatarBadge(
                            player = it,
                            isActive = gameState.activeSeatIndex == it.seatIndex,
                            isPartner = false
                        )
                    }
                }

                // Central Domino Chain View
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x22000000))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (gameState.boardTiles.isEmpty()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .then(
                                    if (selectedTile != null && isMyTurn) {
                                        Modifier.clickable { onPlaceTile(selectedTile, BoardEnd.RIGHT) }
                                    } else Modifier
                                )
                        ) {
                            Text(
                                text = "طاولة الدومينو جاهزة",
                                color = Color(0xAAFFFFFF),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (isMyTurn) {
                                Text(
                                    text = if (selectedTile != null) "انقر هنا أو اسحب لوضع الحجر في المنتصف" else "اختر حجراً من يدك أو اسحبه للبدء",
                                    color = DominoSelectionGold,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    } else {
                        val boardScrollState = rememberScrollState()
                        LaunchedEffect(gameState.boardTiles.size) {
                            boardScrollState.animateScrollTo(boardScrollState.maxValue)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(boardScrollState),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            // Left interactive drop target
                            if (isMyTurn && selectedTile != null && playableEndsForSelected.contains(BoardEnd.LEFT)) {
                                Surface(
                                    color = AccentGreen.copy(alpha = 0.85f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .clickable { onPlaceTile(selectedTile, BoardEnd.LEFT) }
                                ) {
                                    Text(
                                        text = "← ضع هنا (${gameState.openLeftValue})",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            gameState.boardTiles.forEachIndexed { index, placedTile ->
                                BoardTileComposable(
                                    placedTile = placedTile,
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                )
                            }

                            // Right interactive drop target
                            if (isMyTurn && selectedTile != null && playableEndsForSelected.contains(BoardEnd.RIGHT)) {
                                Surface(
                                    color = AccentGreen.copy(alpha = 0.85f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .clickable { onPlaceTile(selectedTile, BoardEnd.RIGHT) }
                                ) {
                                    Text(
                                        text = "(${gameState.openRightValue}) ضع هنا →",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Right Opponent
                Box(modifier = Modifier.width(72.dp), contentAlignment = Alignment.Center) {
                    rightOpponent?.let {
                        PlayerAvatarBadge(
                            player = it,
                            isActive = gameState.activeSeatIndex == it.seatIndex,
                            isPartner = false
                        )
                    }
                }
            }

            // ACTION & TURN INDICATOR BANNER
            TurnActionBanner(
                isMyTurn = isMyTurn,
                activePlayerName = activePlayer?.name ?: "",
                selectedTile = selectedTile,
                playableEnds = playableEndsForSelected,
                openLeft = gameState.openLeftValue,
                openRight = gameState.openRightValue,
                canPass = canPass,
                onPlaceTile = onPlaceTile,
                onPassTurn = onPassTurn
            )

            Spacer(modifier = Modifier.height(6.dp))

            // BOTTOM: PLAYER'S HAND RACK
            PlayerHandRack(
                hand = gameState.myHand,
                selectedTile = selectedTile,
                openLeft = gameState.openLeftValue,
                openRight = gameState.openRightValue,
                isMyTurn = isMyTurn,
                onTileClick = onTileSelected,
                onTileDrop = onPlaceTile
            )
        }
    }
}

@Composable
private fun ScoreHeader(
    teamAScore: Int,
    teamBScore: Int,
    roundNumber: Int
) {
    Surface(
        color = Color(0xDD1B120C),
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 6.dp,
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Team A
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(TeamAGold)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "فريق أ: $teamAScore",
                    color = TeamAGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            // Round Badge
            Text(
                text = "الجولة $roundNumber",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                modifier = Modifier
                    .background(Color(0x33FFFFFF), RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )

            // Team B
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(TeamBOrange)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "فريق ب: $teamBScore",
                    color = TeamBOrange,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun PlayerAvatarBadge(
    player: NetworkPlayerSummary,
    isActive: Boolean,
    isPartner: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .shadow(if (isActive) 8.dp else 2.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    if (player.team == Team.TEAM_A) Color(0xFF1E3A8A) else Color(0xFF9A3412)
                )
                .then(
                    if (isActive) {
                        Modifier.border(
                            2.5.dp,
                            DominoSelectionGold.copy(alpha = glowAlpha),
                            CircleShape
                        )
                    } else {
                        Modifier.border(1.dp, Color(0x44FFFFFF), CircleShape)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = player.name,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Name
        Text(
            text = player.name + if (isPartner) " (شريكك)" else "",
            color = if (isActive) DominoSelectionGold else Color.White,
            fontSize = 11.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            textAlign = TextAlign.Center
        )

        // Remaining tiles count badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(Color(0x99000000), RoundedCornerShape(8.dp))
                .padding(horizontal = 6.dp, vertical = 1.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp, 10.dp)
                    .background(Color(0xFFE5D5B8), RoundedCornerShape(1.dp))
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "${player.tileCount} قطع",
                color = Color(0xFFDDD0BA),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun TurnActionBanner(
    isMyTurn: Boolean,
    activePlayerName: String,
    selectedTile: DominoTile?,
    playableEnds: List<BoardEnd>,
    openLeft: Int?,
    openRight: Int?,
    canPass: Boolean,
    onPlaceTile: (DominoTile, BoardEnd) -> Unit,
    onPassTurn: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isMyTurn) {
            if (selectedTile != null) {
                // Interactive placement buttons for valid ends
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (playableEnds.contains(BoardEnd.LEFT)) {
                        Button(
                            onClick = { onPlaceTile(selectedTile, BoardEnd.LEFT) },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .testTag("place_tile_left_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ضع يساراً ($openLeft)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    if (playableEnds.contains(BoardEnd.RIGHT)) {
                        Button(
                            onClick = { onPlaceTile(selectedTile, BoardEnd.RIGHT) },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .testTag("place_tile_right_button")
                        ) {
                            Text("ضع يميناً ($openRight)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }

                    if (playableEnds.isEmpty()) {
                        Text(
                            text = "هذا الحجر لا يطابق أي طرف!",
                            color = Color(0xFFFF8A80),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else if (canPass) {
                // Player has no moves and must pass
                Button(
                    onClick = onPassTurn,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("pass_turn_button")
                ) {
                    Text("تمرير الدور (Pass)", fontWeight = FontWeight.Bold, color = Color.White)
                }
            } else {
                Text(
                    text = "دورك الآن! المس حجراً لوضعه",
                    color = DominoSelectionGold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Text(
                text = "في انتظار دور: $activePlayerName",
                color = Color(0xCCFFFFFF),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun PlayerHandRack(
    hand: List<DominoTile>,
    selectedTile: DominoTile?,
    openLeft: Int?,
    openRight: Int?,
    isMyTurn: Boolean,
    onTileClick: (DominoTile) -> Unit,
    onTileDrop: (DominoTile, BoardEnd) -> Unit
) {
    val scrollState = rememberScrollState()

    Surface(
        color = Color(0xBB1E140E),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        shadowElevation = 10.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "يدك (${hand.size} أحجار) • يمكنك النقر أو السحب لوضع الحجر",
                color = Color(0xFFD7CCC8),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom
            ) {
                hand.forEach { tile ->
                    val playableEnds = if (openLeft == null) {
                        listOf(BoardEnd.RIGHT)
                    } else {
                        val list = mutableListOf<BoardEnd>()
                        if (tile.hasValue(openLeft)) list.add(BoardEnd.LEFT)
                        if (openRight != null && tile.hasValue(openRight) && !list.contains(BoardEnd.RIGHT)) {
                            list.add(BoardEnd.RIGHT)
                        }
                        list
                    }
                    val isPlayable = isMyTurn && playableEnds.isNotEmpty()

                    DominoTileComposable(
                        tile = tile,
                        isSelected = selectedTile?.id == tile.id,
                        isPlayable = isPlayable,
                        enableDrag = isPlayable,
                        onClick = { onTileClick(tile) },
                        onDragStart = { onTileClick(tile) },
                        onDragRelease = { offset ->
                            if (offset.y < -100f && playableEnds.isNotEmpty()) {
                                if (playableEnds.size == 1) {
                                    onTileDrop(tile, playableEnds.first())
                                } else if (offset.x < -25f && playableEnds.contains(BoardEnd.LEFT)) {
                                    onTileDrop(tile, BoardEnd.LEFT)
                                } else if (offset.x > 25f && playableEnds.contains(BoardEnd.RIGHT)) {
                                    onTileDrop(tile, BoardEnd.RIGHT)
                                } else {
                                    onTileDrop(tile, playableEnds.first())
                                }
                            }
                        },
                        modifier = Modifier.padding(horizontal = 3.dp)
                    )
                }
            }
        }
    }
}
