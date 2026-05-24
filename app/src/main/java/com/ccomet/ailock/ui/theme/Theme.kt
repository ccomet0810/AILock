package com.ccomet.ailock.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = CreamDeep,
    onPrimary = NightBrown,
    secondary = PandaOrange,
    tertiary = LeafGreen,
    background = NightBrown,
    surface = NightSurface,
    surfaceVariant = BarkBrown,
    onBackground = Cream,
    onSurface = Cream,
    onSurfaceVariant = WarmSurfaceVariant,
)

private val LightColorScheme = lightColorScheme(
    primary = PandaOrange,
    onPrimary = WarmSurface,
    primaryContainer = CreamDeep,
    onPrimaryContainer = BarkBrown,
    secondary = BarkBrown,
    onSecondary = WarmSurface,
    secondaryContainer = WarmSurfaceVariant,
    onSecondaryContainer = BarkBrown,
    tertiary = LeafGreen,
    background = Cream,
    onBackground = Cocoa,
    surface = WarmSurface,
    onSurface = Cocoa,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = BarkBrown,
    error = SoftRed,
)

@Composable
fun AILockTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
