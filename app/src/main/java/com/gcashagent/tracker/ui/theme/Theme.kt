package com.gcashagent.tracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = GCashBlue,
    onPrimary = Color.White,
    primaryContainer = GCashBlueLight,
    onPrimaryContainer = GCashBlueDark,
    secondary = GCashBlueDark,
    onSecondary = Color.White,
    background = NeutralSurface,
    onBackground = Color(0xFF161A20),
    surface = Color.White,
    onSurface = Color(0xFF161A20),
    surfaceVariant = Color(0xFFEDF1F7),
    onSurfaceVariant = Color(0xFF44505F),
    outline = NeutralOutline,
    error = CashOutRed,
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = GCashBlueLight,
    onPrimary = GCashBlueDark,
    primaryContainer = GCashBlueDark,
    onPrimaryContainer = GCashBlueLight,
    background = Color(0xFF101317),
    surface = Color(0xFF171B21),
    error = Color(0xFFFF8A80)
)

@Composable
fun GCashTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
