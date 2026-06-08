package com.ccomet.ailock.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PandaOrange,
    onPrimary = AppSurface,
    primaryContainer = PandaBrown,
    onPrimaryContainer = PandaCream,
    secondary = PandaCream,
    tertiary = LeafGreen,
    background = NightBrown,
    surface = NightSurface,
    surfaceVariant = AppTextMuted,
    onBackground = AppSurface,
    onSurface = AppSurface,
    onSurfaceVariant = AppBorder,
)

private val LightColorScheme = lightColorScheme(
    primary = PandaOrange,
    onPrimary = AppSurface,
    primaryContainer = AppSurfaceMuted,
    onPrimaryContainer = PandaOrangeDark,
    secondary = PandaOrange,
    onSecondary = AppSurface,
    secondaryContainer = PandaCream,
    onSecondaryContainer = AppTextStrong,
    tertiary = LeafGreen,
    background = AppBackground,
    onBackground = AppText,
    surface = AppSurface,
    onSurface = AppText,
    surfaceVariant = AppSurfaceMuted,
    onSurfaceVariant = AppTextMuted,
    error = SoftRed,
    errorContainer = SoftRedContainer,
    outline = AppBorder,
    outlineVariant = AppBorderStrong,
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

