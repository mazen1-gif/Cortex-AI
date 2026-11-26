package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CyberColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = CyberDarkBg,
    primaryContainer = CyberSurfaceVariant,
    onPrimaryContainer = NeonCyan,
    secondary = NeonMagenta,
    onSecondary = CyberDarkBg,
    secondaryContainer = CyberSurfaceVariant,
    onSecondaryContainer = NeonMagenta,
    tertiary = MatrixGreen,
    onTertiary = CyberDarkBg,
    tertiaryContainer = CyberSurfaceVariant,
    onTertiaryContainer = MatrixGreen,
    background = CyberDarkBg,
    onBackground = HologramTextPrimary,
    surface = CyberSurface,
    onSurface = HologramTextPrimary,
    surfaceVariant = CyberSurfaceVariant,
    onSurfaceVariant = HologramTextSecondary,
    outline = CyberSurfaceBorder,
    outlineVariant = NeonCyanDim
)

@Composable
fun CyberpunkAITheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CyberColorScheme,
        typography = CyberTypography,
        content = content
    )
}
