package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val BitChatColorScheme = darkColorScheme(
    primary = BitPrimary,
    onPrimary = BitOnPrimary,
    primaryContainer = BitPrimaryContainer,
    onPrimaryContainer = BitOnPrimaryContainer,
    secondary = BitSecondary,
    onSecondary = BitOnSecondary,
    secondaryContainer = BitSecondaryContainer,
    onSecondaryContainer = BitOnSecondaryContainer,
    tertiary = BitTertiary,
    onTertiary = BitOnTertiary,
    tertiaryContainer = BitTertiaryContainer,
    onTertiaryContainer = BitOnTertiaryContainer,
    error = BitError,
    onError = BitOnError,
    errorContainer = BitErrorContainer,
    onErrorContainer = BitOnErrorContainer,
    background = BitBackground,
    onBackground = BitOnSurface,
    surface = BitSurface,
    onSurface = BitOnSurface,
    surfaceVariant = BitSurfaceContainerHighest,
    onSurfaceVariant = BitOnSurfaceVariant,
    outline = BitOutline,
    outlineVariant = BitOutlineVariant,
    surfaceContainer = BitSurfaceContainer,
    surfaceContainerHigh = BitSurfaceContainerHigh,
    surfaceContainerHighest = BitSurfaceContainerHighest,
    surfaceContainerLow = BitSurfaceContainerLow,
    surfaceContainerLowest = BitSurfaceContainerLowest
)

@Composable
fun BitChatTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BitChatColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    BitChatTheme(content = content)
}

