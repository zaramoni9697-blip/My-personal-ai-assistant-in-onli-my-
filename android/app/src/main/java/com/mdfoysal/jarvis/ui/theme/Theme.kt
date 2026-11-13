package com.mdfoysal.jarvis.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush

private val JarvisColorScheme = darkColorScheme(
    primary = JarvisHoloBlue,
    onPrimary = JarvisNavyDeep,
    primaryContainer = JarvisHoloBlueDim,
    onPrimaryContainer = JarvisTextPrimary,
    secondary = JarvisViolet,
    onSecondary = JarvisTextPrimary,
    tertiary = JarvisTeal,
    onTertiary = JarvisNavyDeep,
    background = JarvisBlack,
    onBackground = JarvisTextPrimary,
    surface = JarvisNavy,
    onSurface = JarvisTextPrimary,
    surfaceVariant = JarvisNavyDeep,
    onSurfaceVariant = JarvisTextSecondary,
    outline = JarvisHoloBlueDim,
    outlineVariant = JarvisStroke,
    error = JarvisDanger,
    onError = JarvisTextPrimary
)

/** The holographic background gradient used across the app. */
val JarvisBackgroundBrush: Brush
    get() = Brush.verticalGradient(
        colors = listOf(JarvisNavyDeep, JarvisBlack, JarvisNavyDeep)
    )

/** A radial "core glow" gradient. */
val JarvisCoreBrush: Brush
    get() = Brush.radialGradient(
        colors = listOf(
            JarvisHoloBlue.copy(alpha = 0.35f),
            JarvisViolet.copy(alpha = 0.18f),
            JarvisBlack.copy(alpha = 0.0f)
        )
    )

@Composable
fun JarvisTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // JARVIS is a dark-first interface; the light scheme is intentionally not provided.
    MaterialTheme(
        colorScheme = JarvisColorScheme,
        typography = JarvisTypography,
        content = content
    )
}
