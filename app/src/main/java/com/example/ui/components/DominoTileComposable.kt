package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.game.DominoTile
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

val DominoIvoryWhite = Color(0xFFFBF8F1)
val DominoIvoryDark = Color(0xFFEBE3D2)
val DominoBevelBorder = Color(0xFFC7BCAB)
val DominoDivider = Color(0xFF5A4D41)
val DominoSpinnerGold = Color(0xFFD49B28)
val DominoPipBlack = Color(0xFF1B1917)
val DominoSelectionGold = Color(0xFFFFD54F)

/**
 * Realistic luxury Domino tile with physics-based lift, drag tilt, and lighting effects.
 */
@Composable
fun DominoTileComposable(
    tile: DominoTile,
    modifier: Modifier = Modifier,
    width: Dp = 48.dp,
    height: Dp = 96.dp,
    isSelected: Boolean = false,
    isPlayable: Boolean = false,
    isFaceDown: Boolean = false,
    enableDrag: Boolean = false,
    onClick: (() -> Unit)? = null,
    onDragStart: (() -> Unit)? = null,
    onDragDelta: ((Offset) -> Unit)? = null,
    onDragRelease: ((totalDragOffset: Offset) -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val dragOffsetX = remember { Animatable(0f) }
    val dragOffsetY = remember { Animatable(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val elevation by animateDpAsState(
        targetValue = when {
            isDragging -> 20.dp
            isSelected -> 12.dp
            else -> 3.dp
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tileElevation"
    )

    val baseLiftY by animateDpAsState(
        targetValue = if (isSelected && !isDragging) (-14).dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "tileLift"
    )

    val dragTiltDegrees = if (isDragging) {
        (dragOffsetX.value * 0.06f).coerceIn(-16f, 16f)
    } else 0f

    val shape = RoundedCornerShape(7.dp)

    Box(
        modifier = modifier
            .offset {
                IntOffset(
                    x = dragOffsetX.value.roundToInt(),
                    y = (dragOffsetY.value + baseLiftY.toPx()).roundToInt()
                )
            }
            .rotate(dragTiltDegrees)
            .shadow(
                elevation = elevation,
                shape = shape,
                spotColor = if (isDragging) Color(0x99000000) else Color(0x66000000),
                ambientColor = Color(0x44000000)
            )
            .size(width, height)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        DominoIvoryWhite,
                        Color(0xFFF7F2E7),
                        DominoIvoryDark
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(width.value * 2f, height.value * 2f)
                )
            )
            .then(
                if (isSelected || isDragging) {
                    Modifier.border(2.5.dp, DominoSelectionGold, shape)
                } else if (isPlayable) {
                    Modifier.border(1.8.dp, Color(0xFF81C784), shape)
                } else {
                    Modifier.border(1.dp, DominoBevelBorder, shape)
                }
            )
            .then(
                if (enableDrag) {
                    Modifier.pointerInput(tile.id) {
                        detectDragGestures(
                            onDragStart = {
                                isDragging = true
                                onDragStart?.invoke()
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                coroutineScope.launch {
                                    dragOffsetX.snapTo(dragOffsetX.value + dragAmount.x)
                                    dragOffsetY.snapTo(dragOffsetY.value + dragAmount.y)
                                }
                                onDragDelta?.invoke(Offset(dragOffsetX.value, dragOffsetY.value))
                            },
                            onDragEnd = {
                                val finalOffset = Offset(dragOffsetX.value, dragOffsetY.value)
                                isDragging = false
                                onDragRelease?.invoke(finalOffset)
                                coroutineScope.launch {
                                    dragOffsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                }
                                coroutineScope.launch {
                                    dragOffsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                }
                            },
                            onDragCancel = {
                                isDragging = false
                                coroutineScope.launch {
                                    dragOffsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                }
                                coroutineScope.launch {
                                    dragOffsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                }
                            }
                        )
                    }
                } else Modifier
            )
            .then(
                if (onClick != null) {
                    Modifier
                        .clickable { onClick() }
                        .testTag("domino_tile_${tile.leftValue}_${tile.rightValue}")
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isFaceDown) {
            DominoFaceDownPattern()
        } else {
            Canvas(modifier = Modifier.size(width, height)) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val halfHeight = canvasHeight / 2f

                // Subtle diagonal surface specular sheen
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.White.copy(alpha = 0.5f), Color.Transparent),
                        start = Offset(0f, 0f),
                        end = Offset(canvasWidth * 0.8f, canvasHeight * 0.4f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(canvasWidth, canvasHeight * 0.5f),
                    strokeWidth = canvasWidth * 0.35f
                )

                // Inset bevel highlight along top & left edge
                drawLine(
                    color = Color.White.copy(alpha = 0.85f),
                    start = Offset(2f, 2f),
                    end = Offset(canvasWidth - 2f, 2f),
                    strokeWidth = 2f
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.85f),
                    start = Offset(2f, 2f),
                    end = Offset(2f, canvasHeight - 2f),
                    strokeWidth = 2f
                )

                // Bottom and right subtle bevel shadow
                drawLine(
                    color = Color(0x33000000),
                    start = Offset(2f, canvasHeight - 2f),
                    end = Offset(canvasWidth - 2f, canvasHeight - 2f),
                    strokeWidth = 1.5f
                )
                drawLine(
                    color = Color(0x33000000),
                    start = Offset(canvasWidth - 2f, 2f),
                    end = Offset(canvasWidth - 2f, canvasHeight - 2f),
                    strokeWidth = 1.5f
                )

                // Divider groove line
                drawLine(
                    color = DominoDivider.copy(alpha = 0.3f),
                    start = Offset(4f, halfHeight - 1.2f),
                    end = Offset(canvasWidth - 4f, halfHeight - 1.2f),
                    strokeWidth = 2.2f
                )
                drawLine(
                    color = DominoDivider,
                    start = Offset(5f, halfHeight),
                    end = Offset(canvasWidth - 5f, halfHeight),
                    strokeWidth = 1.8f
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.65f),
                    start = Offset(4f, halfHeight + 1.5f),
                    end = Offset(canvasWidth - 4f, halfHeight + 1.5f),
                    strokeWidth = 1.2f
                )

                // Center brass spinner pin with 3D reflection
                drawCircle(
                    color = Color(0x44000000),
                    radius = 4.2f,
                    center = Offset(canvasWidth / 2f + 0.5f, halfHeight + 0.8f)
                )
                drawCircle(
                    color = DominoSpinnerGold,
                    radius = 3.6f,
                    center = Offset(canvasWidth / 2f, halfHeight)
                )
                drawCircle(
                    color = Color(0xFFFFF9C4),
                    radius = 1.3f,
                    center = Offset(canvasWidth / 2f - 0.9f, halfHeight - 0.9f)
                )

                // Draw pips on Top Half
                drawPipGroup(
                    pips = tile.leftValue,
                    area = Size(canvasWidth, halfHeight),
                    topOffset = 0f
                )

                // Draw pips on Bottom Half
                drawPipGroup(
                    pips = tile.rightValue,
                    area = Size(canvasWidth, halfHeight),
                    topOffset = halfHeight
                )
            }
        }
    }
}

/**
 * Draws the luxury back pattern for face-down opponent dominoes.
 */
@Composable
private fun DominoFaceDownPattern() {
    Canvas(modifier = Modifier.size(48.dp, 96.dp)) {
        val w = size.width
        val h = size.height
        // Elegant dark mahogany with geometric diamond texture
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF2E1A11), Color(0xFF1E100A))
            )
        )
        drawRect(
            color = Color(0xFFD4AF37).copy(alpha = 0.4f),
            topLeft = Offset(3f, 3f),
            size = Size(w - 6f, h - 6f),
            style = Stroke(1.4f)
        )
        // Center brass crest
        drawCircle(
            color = Color(0xFFD4AF37).copy(alpha = 0.6f),
            radius = 6.5f,
            center = Offset(w / 2f, h / 2f)
        )
    }
}

private fun DrawScope.drawPipGroup(pips: Int, area: Size, topOffset: Float) {
    val radius = area.width * 0.082f
    val paddingX = area.width * 0.28f
    val paddingY = area.height * 0.28f

    val left = paddingX
    val right = area.width - paddingX
    val centerX = area.width / 2f

    val top = topOffset + paddingY
    val bottom = topOffset + area.height - paddingY
    val centerY = topOffset + area.height / 2f

    when (pips) {
        1 -> {
            drawPip(centerX, centerY, radius)
        }
        2 -> {
            drawPip(left, top, radius)
            drawPip(right, bottom, radius)
        }
        3 -> {
            drawPip(left, top, radius)
            drawPip(centerX, centerY, radius)
            drawPip(right, bottom, radius)
        }
        4 -> {
            drawPip(left, top, radius)
            drawPip(right, top, radius)
            drawPip(left, bottom, radius)
            drawPip(right, bottom, radius)
        }
        5 -> {
            drawPip(left, top, radius)
            drawPip(right, top, radius)
            drawPip(centerX, centerY, radius)
            drawPip(left, bottom, radius)
            drawPip(right, bottom, radius)
        }
        6 -> {
            drawPip(left, top, radius)
            drawPip(right, top, radius)
            drawPip(left, centerY, radius)
            drawPip(right, centerY, radius)
            drawPip(left, bottom, radius)
            drawPip(right, bottom, radius)
        }
    }
}

private fun DrawScope.drawPip(x: Float, y: Float, radius: Float) {
    // Inset pip with subtle 3D cavity shadow
    drawCircle(
        color = Color(0x33000000),
        radius = radius + 0.6f,
        center = Offset(x, y + 0.6f)
    )
    drawCircle(
        color = DominoPipBlack,
        radius = radius,
        center = Offset(x, y)
    )
    // Pip shine highlight
    drawCircle(
        color = Color.White.copy(alpha = 0.28f),
        radius = radius * 0.35f,
        center = Offset(x - radius * 0.3f, y - radius * 0.3f)
    )
}
