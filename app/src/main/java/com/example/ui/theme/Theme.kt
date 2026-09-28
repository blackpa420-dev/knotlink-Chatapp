package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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

private val BitChatLightColorScheme = lightColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEAF2FF),
    onPrimaryContainer = Color(0xFF0F172A),
    secondary = Color(0xFF0F9F6E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDF7EC),
    onSecondaryContainer = Color(0xFF064E3B),
    tertiary = Color(0xFF7C3AED),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF0E7FF),
    onTertiaryContainer = Color(0xFF2E1065),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFFCBD5E1),
    surfaceContainer = Color(0xFFF1F5F9),
    surfaceContainerHigh = Color(0xFFE2E8F0),
    surfaceContainerHighest = Color(0xFFDCE3EC),
    surfaceContainerLow = Color(0xFFF8FAFC),
    surfaceContainerLowest = Color.White
)

@Composable
fun BitChatTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) BitChatColorScheme else BitChatLightColorScheme,
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
    BitChatTheme(darkTheme = darkTheme, content = content)
}

