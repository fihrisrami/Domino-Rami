package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.game.RoundResult
import com.example.game.Team

@Composable
fun RoundEndDialog(
    roundResult: RoundResult,
    teamAScore: Int,
    teamBScore: Int,
    isHost: Boolean,
    onNextRound: () -> Unit,
    onLeaveGame: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E140F)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Trophy icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(DominoSelectionGold.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = DominoSelectionGold,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (roundResult.isMatchOver) "انتهت المباراة!" else "انتهت الجولة!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = DominoSelectionGold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = roundResult.description,
                    fontSize = 13.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Scoreboard card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x33000000), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("فريق أ", color = TeamAGold, fontWeight = FontWeight.Bold)
                        Text("$teamAScore نقطة", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    }

                    Box(modifier = Modifier.size(1.dp, 30.dp).background(Color(0x33FFFFFF)))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("فريق ب", color = TeamBOrange, fontWeight = FontWeight.Bold)
                        Text("$teamBScore نقطة", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (isHost) {
                    Button(
                        onClick = onNextRound,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("next_round_button")
                    ) {
                        Text(
                            text = if (roundResult.isMatchOver) "بدء مباراة جديدة" else "الجولة التالية",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else {
                    Text(
                        text = "في انتظار المضيف لبدء الجولة التالية...",
                        color = Color(0xAAFFFFFF),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onLeaveGame,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("مغادرة إلى القائمة الرئيسية", color = Color.White)
                }
            }
        }
    }
}
