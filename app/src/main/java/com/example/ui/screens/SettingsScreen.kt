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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.storage.GamePreferences
import com.example.ui.components.AccentGreen
import com.example.ui.components.DominoSelectionGold
import com.example.ui.components.TableFeltCenter
import com.example.ui.components.TableFeltDark
import com.example.ui.components.TableWoodBorder

@Composable
fun SettingsScreen(
    preferences: GamePreferences,
    onNavigateBack: () -> Unit
) {
    var name by remember { mutableStateOf(preferences.playerName) }
    var sfx by remember { mutableStateOf(preferences.isSfxEnabled) }
    var haptics by remember { mutableStateOf(preferences.isHapticsEnabled) }
    var targetScore by remember { mutableStateOf(preferences.targetScore) }

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
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "الرجوع",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "الإعدادات",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }

            // SETTINGS ITEMS
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Player Name Setting
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xDD1B120C)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = DominoSelectionGold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("اسم اللاعب في المباريات", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = name,
                            onValueChange = {
                                name = it
                                preferences.playerName = it
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_name_input"),
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0x33000000),
                                unfocusedContainerColor = Color(0x33000000)
                            )
                        )
                    }
                }

                // Sound & Music
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xDD1B120C)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(14.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = DominoSelectionGold)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("المؤثرات الصوتية (SFX)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("صوت وضع الأحجار والنقر الواقعي", color = Color(0xAAFFFFFF), fontSize = 11.sp)
                            }
                        }
                        Switch(
                            checked = sfx,
                            onCheckedChange = {
                                sfx = it
                                preferences.isSfxEnabled = it
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = AccentGreen)
                        )
                    }
                }

                // Haptics
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xDD1B120C)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(14.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = DominoSelectionGold)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("الاهتزاز اللمسي (Haptic Feedback)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("اهتزاز خفيف عند لمس ووضع الحجر", color = Color(0xAAFFFFFF), fontSize = 11.sp)
                            }
                        }
                        Switch(
                            checked = haptics,
                            onCheckedChange = {
                                haptics = it
                                preferences.isHapticsEnabled = it
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = AccentGreen)
                        )
                    }
                }

                // About card
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0x881B120C)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xBBFFFFFF))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("دومينو رامي • Domino Rami", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("الإصدار 1.0 • لعبة محلية أصلية بالكامل تعمل بدون إنترنت", color = Color(0xAAFFFFFF), fontSize = 11.sp)
                        }
                    }
                }
            }

            Button(
                onClick = onNavigateBack,
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_save_button")
            ) {
                Text("حفظ والعودة", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
