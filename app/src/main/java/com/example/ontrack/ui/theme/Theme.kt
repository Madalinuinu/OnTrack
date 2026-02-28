package com.example.ontrack.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Culoarea de brand principală (accent/butoane): #7889DF
private val BrandColor = Color(0xFF7889DF)
// Culoarea de fundal cerută pentru toată aplicația după onboarding: #8C96FF
private val BackgroundColor = Color(0xFF8C96FF)

private val AppColorScheme = lightColorScheme(
    primary = BrandColor,
    onPrimary = Color.White,
    primaryContainer = BrandColor,
    onPrimaryContainer = Color.White,
    secondary = BrandColor,
    onSecondary = Color.White,
    secondaryContainer = BrandColor,
    onSecondaryContainer = Color.White,
    background = BackgroundColor,
    onBackground = Color.White,
    surface = BackgroundColor,
    onSurface = Color.White,
    surfaceVariant = BackgroundColor.copy(alpha = 0.9f),
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