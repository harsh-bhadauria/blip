package com.raven.blip.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PremiumDarkColorScheme = darkColorScheme(
    primary = Color(0xFFA6E3A1),
    onPrimary = Color(0xFF1E1E2E),
    secondary = Color(0xFF89B4FA),
    onSecondary = Color(0xFF1E1E2E),
    tertiary = Color(0xFFF38BA8),
    onTertiary = Color(0xFF1E1E2E),
    background = Color(0xFF11111B),
    onBackground = Color(0xFFCDD6F4),
    surface = Color(0xFF181825),
    onSurface = Color(0xFFCDD6F4),
    surfaceVariant = Color(0xFF313244),
    onSurfaceVariant = Color(0xFFA6ADC8),
    outline = Color(0xFF45475A)
)

@Composable
fun BlipTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PremiumDarkColorScheme,
        typography = Typography,
        content = content
    )
}