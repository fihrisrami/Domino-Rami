package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.game.Team
import com.example.network.NetworkPlayerSummary
import com.example.ui.components.AccentGreen
import com.example.ui.components.DominoSelectionGold
import com.example.ui.components.TableFeltCenter
import com.example.ui.components.TableFeltDark
import com.example.ui.components.TableWoodBorder
import com.example.ui.components.TeamAGold
import com.example.ui.components.TeamBOrange

@Composable
fun HostLobbyScreen(
    roomId: String,
    roomName: String,
    hostIp: String,
    players: List<NetworkPlayerSummary>,
    canStart: Boolean,
    onAddBot: () -> Unit,
    onStartGame: () -> Unit,
    onLeaveLobby: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(TableFeltCenter, TableFeltDark),
                    radius = 1100f
                )
            )
            .border(6.dp, TableWoodBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP BAR
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onLeaveLobby) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "الرجوع",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "غرفة المضيف: $roomName",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "رمز الغرفة: $roomId",
                        color = DominoSelectionGold,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp
                    )
                }
            }

            // CONNECTION INSTRUCTIONS CARD
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xDD1B120C)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Wifi, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "عنوان الاتصال المحلي (IP): $hostIp",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "تأكد من فتح نقطة الاتصال Hotspot وأن بقية اللاعبين متصلون بها.",
                            color = Color(0xCCFFFFFF),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // PLAYERS LIST (4 SEATS)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 6.dp)
            ) {
                Text(
                    text = "اللاعبون المتصلون (${players.size} / 4):",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                for (seatIndex in 0..3) {
                    val p = players.find { it.seatIndex == seatIndex }
                    val team = Team.fromIndex(seatIndex)
                    val seatLabel = when (seatIndex) {
                        0 -> "المضيف (أنت) - شريكك اللاعب 3"
                        1 -> "اللاعب 2 - شريكه اللاعب 4"
                        2 -> "اللاعب 3 - شريكك (فريقك)"
                        else -> "اللاعب 4 - شريك اللاعب 2"
                    }

                    PlayerSlotCard(
                        seatIndex = seatIndex,
                        seatLabel = seatLabel,
                        team = team,
                        player = p
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            // BOTTOM CONTROLS
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (players.size < 4) {
                    OutlinedButton(
                        onClick = onAddBot,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_bot_button")
                    ) {
                        Icon(Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إضافة لاعب ذكي (Bot) لملء المقعد", fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = onStartGame,
                    enabled = canStart,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentGreen,
                        disabledContainerColor = Color(0x551B5E20)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("start_game_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (canStart) "ابدأ اللعب الآن" else "في انتظار اكتمال 4 لاعبين (${players.size}/4)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerSlotCard(
    seatIndex: Int,
    seatLabel: String,
    team: Team,
    player: NetworkPlayerSummary?
) {
    val teamColor = if (team == Team.TEAM_A) TeamAGold else TeamBOrange

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (player != null) Color(0xCC1A1612) else Color(0x661A1612)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (player != null) teamColor.copy(alpha = 0.25f) else Color(0x22FFFFFF)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (player != null) Icons.Default.Person else Icons.Default.HourglassEmpty,
                    contentDescription = null,
                    tint = if (player != null) teamColor else Color(0x88FFFFFF),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = player?.name ?: "في انتظار لاعب...",
                    color = if (player != null) Color.White else Color(0x99FFFFFF),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "${team.displayNameArabic} • $seatLabel",
                    color = teamColor,
                    fontSize = 11.sp
                )
            }

            if (player != null) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "جاهز",
                    tint = AccentGreen,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
