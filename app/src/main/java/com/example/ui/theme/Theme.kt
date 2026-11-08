package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = GoldPrimary,
    onPrimary = Color(0xFF1E1700),
    primaryContainer = GoldContainer,
    onPrimaryContainer = GoldLight,
    secondary = EmeraldProfit,
    onSecondary = Color.Black,
    secondaryContainer = EmeraldContainer,
    onSecondaryContainer = EmeraldLight,
    tertiary = CyanTech,
    onTertiary = Color.Black,
    tertiaryContainer = CyanContainer,
    onTertiaryContainer = Color(0xFFA5F3FC),
    background = DarkBackground,
    onBackground = Color(0xFFF1F5F9),
    surface = DarkSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFCBD5E1),
    error = CrimsonLoss,
    onError = Color.White,
    errorContainer = CrimsonContainer,
    onErrorContainer = CrimsonLight,
    outline = DarkCardBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
