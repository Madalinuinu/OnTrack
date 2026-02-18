package com.example.ontrack.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Culoarea de brand cerută: #4D57C8
private val BrandColor = Color(0xFF4D57C8)

private val AppColorScheme = lightColorScheme(
    primary = BrandColor,
    onPrimary = Color.White,
    primaryContainer = BrandColor,
    onPrimaryContainer = Color.White,
    secondary = BrandColor,
    onSecondary = Color.White,
    secondaryContainer = BrandColor,
    onSecondaryContainer = Color.White,
    background = BrandColor,
    onBackground = Color.White,
    surface = BrandColor,
    onSurface = Color.White,
    surfaceVariant = BrandColor.copy(alpha = 0.9f),
    onSurfaceVariant = Color.White,
    outline = Color(0xFFCBD5FF)
)

@Composable
fun OnTrackTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = Typography,
        content = content
    )
}