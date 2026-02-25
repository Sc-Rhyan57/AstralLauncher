package com.astrallauncher.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Gold   = Color(0xFFFFD700)
val DarkBg = Color(0xFF0D0D1A)
val CardBg = Color(0xFF1A1A2E)
val AccentGreen = Color(0xFF00FF88)
val TextSecondary = Color(0x88FFFFFF)

private val darkColors = darkColorScheme(
    primary   = Gold,
    secondary = AccentGreen,
    background = DarkBg,
    surface   = CardBg,
    onPrimary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun AstralTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColors, content = content)
}
