package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import com.example.game.BoardEnd
import com.example.game.DominoTile
import com.example.game.PlacedTile
import com.example.ui.components.AccentGreen
import com.example.ui.components.BoardTileComposable
import com.example.ui.components.DominoSelectionGold
import com.example.ui.components.DominoTileComposable
import com.example.ui.components.TableFeltCenter
import com.example.ui.components.TableFeltDark
import com.example.ui.components.TableWoodBorder

@Composable
fun TutorialScreen(
    onNavigateBack: () -> Unit
) {
    var selectedInteractiveTile by remember { mutableStateOf<DominoTile?>(null) }
    var interactiveSolved by remember { mutableStateOf(false) }

    val tutorialTiles = remember {
        listOf(
            DominoTile(1, 6, 2),
            DominoTile(2, 4, 1),
            DominoTile(3, 3, 0)
        )
    }

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
                .padding(16.dp)
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
                    text = "كيف تلعب دومينو رامي؟",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TutorialStepCard(
                    stepNumber = "1",
                    title = "مجموعة الأحجار والبداية",
                    description = "تتكون اللعبة من 28 حجراً تبدأ من (0-0) حتى (6-6). في لعبة 4 لاعبين بالشراكة، يحصل كل لاعب على 7 أحجار. في الجولة الأولى، يبدأ دائماً صاحب حجر (6-6 الدوشيش)."
                )

                TutorialStepCard(
                    stepNumber = "2",
                    title = "مطابقة أطراف السلسلة",
                    description = "عندما يحين دورك، يجب أن تضع حجراً يحتوي على نفس الرقم الموجود في أحد طرفي السلسلة (الطرف الأيمن أو الطرف الأيسر). وتوضع أحجار الدبل (المزدوجة) بشكل عرضي/متعامد."
                )

                TutorialStepCard(
                    stepNumber = "3",
                    title = "التمرير واللعب المغلق (Pass & Block)",
                    description = "إذا لم تمتلك أي حجر يطابق أحد الطرفين المفتوحين، يجب عليك الضغط على 'تمرير الدور (Pass)'. وإذا مرر جميع اللاعبين ولم يعد أحد يستطيع اللعب، تُغلق اللعبة ويفوز الفريق صاحب أقل مجموع نقاط في يده!"
                )

                TutorialStepCard(
                    stepNumber = "4",
                    title = "احتساب النقاط ونظام الشراكة",
                    description = "أنت وشريكك المتقابل تشكلان فريقاً واحداً! عندما يتخلص أي منكما من جميع أحجاره أولاً (Domino)، يحصل فريقكم على مجموع كل النقاط المتبقية في أيدي لاعبي الفريق المنافس."
                )

                // INTERACTIVE MINI-GAME
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xEE1E140F)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = DominoSelectionGold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "تدريب عملي: جرب بنفسك الآن!",
                                color = DominoSelectionGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "السلسلة على الطاولة مفتوحة على الرقم [ 6 ]:",
                            color = Color.White,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Current board tile [5|6]
                        BoardTileComposable(
                            placedTile = PlacedTile(
                                tile = DominoTile(20, 5, 6),
                                playedByPlayerId = "bot",
                                placedLeftValue = 5,
                                placedRightValue = 6,
                                isPerpendicular = false
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        if (!interactiveSolved) {
                            Text(
                                text = "المس الحجر المطابق من يدك لتضعه على الرقم 6:",
                                color = Color(0xCCFFFFFF),
                                fontSize = 12.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                tutorialTiles.forEach { tile ->
                                    DominoTileComposable(
                                        tile = tile,
                                        isSelected = selectedInteractiveTile?.id == tile.id,
                                        isPlayable = tile.hasValue(6),
                                        onClick = {
                                            selectedInteractiveTile = tile
                                            if (tile.hasValue(6)) {
                                                interactiveSolved = true
                                            }
                                        },
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                }
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(AccentGreen.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentGreen)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "أحسنت! وضعت الحجر [6|2] بنجاح وأكملت السلسلة!",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
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
                    .testTag("tutorial_done_button")
            ) {
                Text("فهمت القواعد! العودة للرئيسية", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
private fun TutorialStepCard(
    stepNumber: String,
    title: String,
    description: String
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xCC1A140F)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(DominoSelectionGold),
                contentAlignment = Alignment.Center
            ) {
                Text(stepNumber, color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    color = Color(0xCCFFFFFF),
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
