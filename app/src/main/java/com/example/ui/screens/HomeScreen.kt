package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.DominoTile
import com.example.ui.components.DominoSelectionGold
import com.example.ui.components.DominoTileComposable
import com.example.ui.components.TableFeltCenter
import com.example.ui.components.TableFeltDark
import com.example.ui.components.TableWoodBorder

@Composable
fun HomeScreen(
    playerName: String,
    onPlayerNameChange: (String) -> Unit,
    onNavigateToHostLobby: () -> Unit,
    onNavigateToJoinLobby: () -> Unit,
    onNavigateToPractice: () -> Unit,
    onNavigateToTutorial: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    var showNameDialog by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf(playerName) }

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
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP BAR: Player Profile
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Player badge with edit button
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0x991E140E),
                    modifier = Modifier.clickable {
                        tempName = playerName
                        showNameDialog = true
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(DominoSelectionGold),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = playerName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.Edit, contentDescription = "تعديل الاسم", tint = Color(0xBBFFFFFF), modifier = Modifier.size(14.dp))
                    }
                }

                // Settings icon button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x77000000))
                        .clickable { onNavigateToSettings() }
                        .testTag("settings_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "الإعدادات", tint = Color.White)
                }
            }

            // CENTER HERO: Brand Logo & Domino Tiles
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                // Two overlapping decorative domino tiles
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DominoTileComposable(
                        tile = DominoTile(28, 6, 6),
                        width = 44.dp,
                        height = 88.dp,
                        modifier = Modifier.shadow(8.dp, RoundedCornerShape(6.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    DominoTileComposable(
                        tile = DominoTile(27, 5, 6),
                        width = 44.dp,
                        height = 88.dp,
                        modifier = Modifier.shadow(8.dp, RoundedCornerShape(6.dp))
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "دومينو رامي",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DominoSelectionGold,
                    letterSpacing = 1.sp
                )

                Text(
                    text = "Domino Rami • اللعب المحلي عبر نقطة الاتصال",
                    fontSize = 12.sp,
                    color = Color(0xCCFFFFFF),
                    fontWeight = FontWeight.Normal
                )
            }

            // ACTION BUTTONS: Clean, Luxury M3 Cards
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MenuActionButton(
                    title = "إنشاء غرفة محلية (Host)",
                    subtitle = "افتح نقطة الاتصال Hotspot وشارك الغرفة مع أصدقائك",
                    icon = Icons.Default.Wifi,
                    containerColor = Color(0xFF1B5E20),
                    onClick = onNavigateToHostLobby,
                    tag = "host_game_button"
                )

                MenuActionButton(
                    title = "الانضمام إلى غرفة (Join)",
                    subtitle = "اتصل بنفس نقطة الاتصال وانضم لغرفة أصدقائك",
                    icon = Icons.Default.Groups,
                    containerColor = Color(0xFF0D47A1),
                    onClick = onNavigateToJoinLobby,
                    tag = "join_game_button"
                )

                MenuActionButton(
                    title = "مباراة تدريبية (Offline Practice)",
                    subtitle = "العب بمفردك ضد روبوتات ذكية بدون شبكة",
                    icon = Icons.Default.Psychology,
                    containerColor = Color(0xFFE65100),
                    onClick = onNavigateToPractice,
                    tag = "practice_game_button"
                )

                MenuActionButton(
                    title = "كيف تلعب؟ (Tutorial)",
                    subtitle = "تعلم قواعد الدومينو وتوزيع الأحجار خطوة بخطوة",
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    containerColor = Color(0xFF4A148C),
                    onClick = onNavigateToTutorial,
                    tag = "tutorial_button"
                )
            }

            // Bottom Footer
            Text(
                text = "يعمل بدون إنترنت 100% عبر نقطة الاتصال Hotspot",
                color = Color(0x88FFFFFF),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
    }

    // Name Edit Dialog
    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("اسم اللاعب", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text("اكتب اسمك") },
                    singleLine = true,
                    modifier = Modifier.testTag("player_name_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempName.isNotBlank()) {
                            onPlayerNameChange(tempName.trim())
                        }
                        showNameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun MenuActionButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    onClick: () -> Unit,
    tag: String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0x33FFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = subtitle,
                    color = Color(0xCCFFFFFF),
                    fontSize = 11.sp
                )
            }
        }
    }
}
