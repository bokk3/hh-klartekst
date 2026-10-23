package com.klartext.simulator.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CncAccentOrange,
    secondary = CncFeedCyan,
    tertiary = CncPlungePurple,
    background = CncDarkBackground,
    surface = CncSurfaceDark,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = CncTextPrimary,
    onSurface = CncTextPrimary
)

@Composable
fun KlartextSimulatorTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
