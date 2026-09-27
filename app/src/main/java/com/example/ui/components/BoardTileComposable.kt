package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.game.PlacedTile

/**
 * Renders a domino tile as placed in the table chain.
 * Doubles are placed perpendicularly (vertical), regular tiles horizontally.
 */
@Composable
fun BoardTileComposable(
    placedTile: PlacedTile,
    modifier: Modifier = Modifier
) {
    val isVertical = placedTile.isPerpendicular
    val width: Dp = if (isVertical) 34.dp else 68.dp
    val height: Dp = if (isVertical) 68.dp else 34.dp

    val shape = RoundedCornerShape(5.dp)

    Box(
        modifier = modifier
            .shadow(4.dp, shape, spotColor = Color(0x66000000))
            .size(width, height)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(DominoIvoryWhite, DominoIvoryDark)
                )
            )
            .border(1.dp, DominoBevelBorder, shape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(width, height)) {
            val w = size.width
            val h = size.height

            if (isVertical) {
                // Perpendicular Double
                val halfH = h / 2f
                drawLine(
                    color = DominoDivider,
                    start = Offset(3f, halfH),
                    end = Offset(w - 3f, halfH),
                    strokeWidth = 1.5f
                )
                drawCircle(
                    color = DominoSpinnerGold,
                    radius = 2.8f,
                    center = Offset(w / 2f, halfH)
                )
                drawPipGroupBoard(
                    pips = placedTile.placedLeftValue,
                    area = Size(w, halfH),
                    topOffset = 0f,
                    leftOffset = 0f
                )
                drawPipGroupBoard(
                    pips = placedTile.placedRightValue,
                    area = Size(w, halfH),
                    topOffset = halfH,
                    leftOffset = 0f
                )
            } else {
                // Horizontal regular tile: left side has placedLeftValue, right side has placedRightValue
                val halfW = w / 2f
                drawLine(
                    color = DominoDivider,
                    start = Offset(halfW, 3f),
                    end = Offset(halfW, h - 3f),
                    strokeWidth = 1.5f
                )
                drawCircle(
                    color = DominoSpinnerGold,
                    radius = 2.8f,
                    center = Offset(halfW, h / 2f)
                )
                drawPipGroupBoard(
                    pips = placedTile.placedLeftValue,
                    area = Size(halfW, h),
                    topOffset = 0f,
                    leftOffset = 0f
                )
                drawPipGroupBoard(
                    pips = placedTile.placedRightValue,
                    area = Size(halfW, h),
                    topOffset = 0f,
                    leftOffset = halfW
                )
            }
        }
    }
}

private fun DrawScope.drawPipGroupBoard(pips: Int, area: Size, topOffset: Float, leftOffset: Float) {
    val radius = minOf(area.width, area.height) * 0.088f
    val padX = area.width * 0.28f
    val padY = area.height * 0.28f

    val left = leftOffset + padX
    val right = leftOffset + area.width - padX
    val centerX = leftOffset + area.width / 2f

    val top = topOffset + padY
    val bottom = topOffset + area.height - padY
    val centerY = topOffset + area.height / 2f

    when (pips) {
        1 -> drawPipSimple(centerX, centerY, radius)
        2 -> {
            drawPipSimple(left, top, radius)
            drawPipSimple(right, bottom, radius)
        }
        3 -> {
            drawPipSimple(left, top, radius)
            drawPipSimple(centerX, centerY, radius)
            drawPipSimple(right, bottom, radius)
        }
        4 -> {
            drawPipSimple(left, top, radius)
            drawPipSimple(right, top, radius)
            drawPipSimple(left, bottom, radius)
            drawPipSimple(right, bottom, radius)
        }
        5 -> {
            drawPipSimple(left, top, radius)
            drawPipSimple(right, top, radius)
            drawPipSimple(centerX, centerY, radius)
            drawPipSimple(left, bottom, radius)
            drawPipSimple(right, bottom, radius)
        }
        6 -> {
            drawPipSimple(left, top, radius)
            drawPipSimple(right, top, radius)
            drawPipSimple(left, centerY, radius)
            drawPipSimple(right, centerY, radius)
            drawPipSimple(left, bottom, radius)
            drawPipSimple(right, bottom, radius)
        }
    }
}

private fun DrawScope.drawPipSimple(x: Float, y: Float, radius: Float) {
    drawCircle(
        color = DominoPipBlack,
        radius = radius,
        center = Offset(x, y)
    )
}
