package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AnkitaDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = OnPrimaryDark,
    primaryContainer = CardSurfaceElevated,
    onPrimaryContainer = NeonCyan,
    secondary = ElectricViolet,
    onSecondary = OnSecondaryDark,
    secondaryContainer = CardSurfaceElevated,
    onSecondaryContainer = ElectricViolet,
    tertiary = NeonPink,
    background = VoidBlack,
    onBackground = TextPrimary,
    surface = CardSurface,
    onSurface = TextPrimary,
    surfaceVariant = CardSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = CardBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our signature Ankita cyberpunk luxury dark scheme by default
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = AnkitaDarkColorScheme,
        typography = Typography,
        content = content
    )
}
