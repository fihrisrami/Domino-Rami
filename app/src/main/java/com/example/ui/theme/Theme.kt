package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DominoDarkColorScheme = darkColorScheme(
    primary = AmberGold,
    onPrimary = Color.Black,
    secondary = AccentEmerald,
    onSecondary = Color.White,
    tertiary = AccentOrange,
    background = EmeraldFeltDark,
    onBackground = Color.White,
    surface = DarkSurface,
    onSurface = Color.White,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFDDD0BA)
)

@Composable
fun DominoRamiTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DominoDarkColorScheme,
        typography = Typography,
        content = content
    )
}
