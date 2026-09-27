package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.network.ConnectionState
import com.example.network.DiscoveredRoom
import com.example.network.NetworkPlayerSummary
import com.example.ui.components.AccentGreen
import com.example.ui.components.DominoSelectionGold
import com.example.ui.components.TableFeltCenter
import com.example.ui.components.TableFeltDark
import com.example.ui.components.TableWoodBorder

@Composable
fun JoinLobbyScreen(
    connectionState: ConnectionState,
    discoveredRooms: List<DiscoveredRoom>,
    lobbyPlayers: List<NetworkPlayerSummary>,
    onConnectToIp: (String) -> Unit,
    onLeaveLobby: () -> Unit
) {
    var manualIp by remember { mutableStateOf("192.168.43.1") }

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
                Text(
                    text = "الانضمام إلى غرفة محلية",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }

            // CONTENT
            when (connectionState) {
                is ConnectionState.Connecting -> {
                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = DominoSelectionGold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "جاري الاتصال بالمضيف ومزامنة الغرفة...",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }

                is ConnectionState.Connected -> {
                    // Joined room waiting for host to start
                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(AccentGreen.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.HourglassTop, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "تم الاتصال بالغرفة: ${connectionState.roomId}",
                            color = DominoSelectionGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "مقعدك رقم: ${connectionState.assignedSeat + 1} (${connectionState.team.displayNameArabic})",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "في انتظار المضيف لبدء المباراة...",
                            color = Color(0xCCFFFFFF),
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Players inside room
                        Text(
                            text = "اللاعبون المتصلون (${lobbyPlayers.size} / 4):",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        lobbyPlayers.forEach { p ->
                            Text(
                                text = "• ${p.name} (${p.team.displayNameArabic})",
                                color = Color(0xFFDDD0BA),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                is ConnectionState.Failed, ConnectionState.Disconnected -> {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        // Error banner if failed
                        if (connectionState is ConnectionState.Failed) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFB71C1C)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = connectionState.reason,
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        // Auto-discovered rooms
                        Text(
                            text = "الغرف المكتشفة تلقائياً في نقطة الاتصال:",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        if (discoveredRooms.isEmpty()) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0x771E140E)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "جاري البحث عن غرف في نقطة الاتصال...",
                                        color = Color(0xBBFFFFFF),
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "إذا لم تظهر الغرفة تلقائياً، استخدم الاتصال المباشر بالأسفل.",
                                        color = Color(0x88FFFFFF),
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                items(discoveredRooms) { room ->
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xDD1B120C)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable { onConnectToIp(room.hostIp) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(
                                                    text = room.roomName,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = "المضيف: ${room.hostName} • رمز: ${room.roomId}",
                                                    color = DominoSelectionGold,
                                                    fontSize = 11.sp
                                                )
                                            }

                                            Button(
                                                onClick = { onConnectToIp(room.hostIp) },
                                                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text("انضمام (${room.currentPlayers}/4)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Manual IP Connect Section
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xAA1E140E)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "الاتصال المباشر عبر عنوان IP المضيف:",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = manualIp,
                                        onValueChange = { manualIp = it },
                                        label = { Text("IP المضيف", color = Color(0xAAFFFFFF)) },
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("direct_ip_input"),
                                        colors = TextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedContainerColor = Color(0x33000000),
                                            unfocusedContainerColor = Color(0x33000000)
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            if (manualIp.isNotBlank()) {
                                                onConnectToIp(manualIp.trim())
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("direct_connect_button")
                                    ) {
                                        Text("اتصال", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom cancel/back button
            Button(
                onClick = onLeaveLobby,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x44FFFFFF)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text("رجوع إلى القائمة الرئيسية", color = Color.White)
            }
        }
    }
}
