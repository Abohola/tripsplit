package com.tripsplit.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF48EAD9),
    onPrimary = Color(0xFF041213),
    primaryContainer = Color(0xFF106D65),
    onPrimaryContainer = Color(0xFFE6FFF9),
    secondary = Color(0xFF77B9FF),
    onSecondary = Color(0xFF081A30),
    tertiary = Color(0xFFFFC06E),
    onTertiary = Color(0xFF3B2100),
    error = Color(0xFFFF6B7A),
    background = Color(0xFF07131D),
    onBackground = Color(0xFFF7FBFF),
    surface = Color(0xFF0B2032),
    onSurface = Color(0xFFF7FBFF),
    surfaceVariant = Color(0xFF15354A),
    onSurfaceVariant = Color(0xFFD8E6F0),
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
