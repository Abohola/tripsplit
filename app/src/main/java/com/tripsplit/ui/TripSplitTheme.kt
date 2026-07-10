package com.tripsplit.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF6B35F),
    onPrimary = Color(0xFF172D32),
    primaryContainer = Color(0xFF7D5130),
    onPrimaryContainer = Color(0xFFFFF3D0),
    secondary = Color(0xFF84D9D3),
    onSecondary = Color(0xFF07343C),
    tertiary = Color(0xFFE77962),
    onTertiary = Color(0xFF331613),
    error = Color(0xFFFF8A73),
    background = Color(0xFFB6DED5),
    onBackground = Color(0xFF07343C),
    surface = Color(0xFF07343C),
    onSurface = Color(0xFFFFF3D0),
    surfaceVariant = Color(0xFF0A4650),
    onSurfaceVariant = Color(0xFFECDDBF),
    outline = Color(0x66FFF3D0),
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
