package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Pro Bettor / Casino Luxury Sportsbook Theme
private val BettorDarkColorScheme = darkColorScheme(
    primary = CasinoGold,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF2C2010),
    onPrimaryContainer = CasinoGoldLight,
    secondary = ElectricGreen,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF063323),
    onSecondaryContainer = ElectricGreenGlow,
    tertiary = ElectricCyan,
    onTertiary = Color.Black,
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = DarkCard,
    onSurfaceVariant = TextSecondary,
    outline = DarkCardBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Pro bettor theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BettorDarkColorScheme,
        typography = Typography,
        content = content
    )
}
