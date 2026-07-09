package com.tripsplit.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF52E0C4),
    onPrimary = Color(0xFF04251F),
    primaryContainer = Color(0xFF0B594E),
    onPrimaryContainer = Color(0xFFE6FFF9),
    secondary = Color(0xFF9FA9FF),
    onSecondary = Color(0xFF101741),
    tertiary = Color(0xFFFFB45E),
    onTertiary = Color(0xFF3B2100),
    error = Color(0xFFFF6B7A),
    background = Color(0xFF05070B),
    onBackground = Color(0xFFF4F7F8),
    surface = Color(0xFF121820),
    onSurface = Color(0xFFF4F7F8),
    surfaceVariant = Color(0xFF26313C),
    onSurfaceVariant = Color(0xFFC0CCD5),
    outline = Color(0x66FFFFFF),
)

@Composable
fun TripSplitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content,
    )
}
