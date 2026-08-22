package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing

// Premium, ultra-high-contrast Dark Palette designed for dark trailheads, tents, and night navigation
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF00FFE0),           // Solar glowing cyan for main actions
    onPrimary = Color(0xFF001B1A),         // Pure deep dark teal for readability
    primaryContainer = Color(0xFF00443D),  // Dark accent containers
    onPrimaryContainer = Color(0xFFB3FFF7),
    background = Color(0xFF080C14),        // Deep space obsidian background
    onBackground = Color(0xFFE2E8F0),      // Pure crisp readable white
    surface = Color(0xFF0C101A),           // Dark elements / cards
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0xFF131722),    // Secondary layout containers
    onSurfaceVariant = Color(0xFF94A3B8),  // Midtone steel gray for auxiliary items
    outline = Color(0xFF1E293B),           // High-contrast divider borders
    error = Color(0xFFFF453A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFF4C1010),
    onErrorContainer = Color(0xFFFFD6D6)
)

// Premium, ultra-high-contrast Light Palette designed for daytime direct sunlight mountain ascents
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF005F55),           // Intense deep teal for crisp readability
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF80FFF1),
    onPrimaryContainer = Color(0xFF00221E),
    background = Color(0xFFF1F5F9),        // Light weather-chart blue-gray base background
    onBackground = Color(0xFF0F172A),      // Deep obsidian-slate for maximum contrast ratio
    surface = Color(0xFFFFFFFF),           // Crisp white card containers
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),    // Secondary background container
    onSurfaceVariant = Color(0xFF475569),  // Slate gray for labels and details
    outline = Color(0xFF94A3B8),           // Visible outlines for borders
    error = Color(0xFFB91C1C),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D)
)

@Composable
fun animateColorScheme(target: androidx.compose.material3.ColorScheme): androidx.compose.material3.ColorScheme {
    val duration = 450 // smooth transition duration
    val animationSpec = tween<Color>(durationMillis = duration, easing = FastOutSlowInEasing)
    
    val primary by animateColorAsState(target.primary, animationSpec, label = "primary")
    val onPrimary by animateColorAsState(target.onPrimary, animationSpec, label = "onPrimary")
    val primaryContainer by animateColorAsState(target.primaryContainer, animationSpec, label = "primaryContainer")
    val onPrimaryContainer by animateColorAsState(target.onPrimaryContainer, animationSpec, label = "onPrimaryContainer")
    val inversePrimary by animateColorAsState(target.inversePrimary, animationSpec, label = "inversePrimary")
    val secondary by animateColorAsState(target.secondary, animationSpec, label = "secondary")
    val onSecondary by animateColorAsState(target.onSecondary, animationSpec, label = "onSecondary")
    val secondaryContainer by animateColorAsState(target.secondaryContainer, animationSpec, label = "secondaryContainer")
    val onSecondaryContainer by animateColorAsState(target.onSecondaryContainer, animationSpec, label = "onSecondaryContainer")
    val tertiary by animateColorAsState(target.tertiary, animationSpec, label = "tertiary")
    val onTertiary by animateColorAsState(target.onTertiary, animationSpec, label = "onTertiary")
    val tertiaryContainer by animateColorAsState(target.tertiaryContainer, animationSpec, label = "tertiaryContainer")
    val onTertiaryContainer by animateColorAsState(target.onTertiaryContainer, animationSpec, label = "onTertiaryContainer")
    val background by animateColorAsState(target.background, animationSpec, label = "background")
    val onBackground by animateColorAsState(target.onBackground, animationSpec, label = "onBackground")
    val surface by animateColorAsState(target.surface, animationSpec, label = "surface")
    val onSurface by animateColorAsState(target.onSurface, animationSpec, label = "onSurface")
    val surfaceVariant by animateColorAsState(target.surfaceVariant, animationSpec, label = "surfaceVariant")
    val onSurfaceVariant by animateColorAsState(target.onSurfaceVariant, animationSpec, label = "onSurfaceVariant")
    val surfaceTint by animateColorAsState(target.surfaceTint, animationSpec, label = "surfaceTint")
    val outline by animateColorAsState(target.outline, animationSpec, label = "outline")
    val outlineVariant by animateColorAsState(target.outlineVariant, animationSpec, label = "outlineVariant")
    val scrim by animateColorAsState(target.scrim, animationSpec, label = "scrim")
    val error by animateColorAsState(target.error, animationSpec, label = "error")
    val onError by animateColorAsState(target.onError, animationSpec, label = "onError")
    val errorContainer by animateColorAsState(target.errorContainer, animationSpec, label = "errorContainer")
    val onErrorContainer by animateColorAsState(target.onErrorContainer, animationSpec, label = "onErrorContainer")
    val inverseOnSurface by animateColorAsState(target.inverseOnSurface, animationSpec, label = "inverseOnSurface")
    val inverseSurface by animateColorAsState(target.inverseSurface, animationSpec, label = "inverseSurface")

    return target.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        inversePrimary = inversePrimary,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceTint = surfaceTint,
        outline = outline,
        outlineVariant = outlineVariant,
        scrim = scrim,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        inverseOnSurface = inverseOnSurface,
        inverseSurface = inverseSurface
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val baseColorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val colorScheme = animateColorScheme(baseColorScheme)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Uses our mountain-standard fixed typography
        content = content
    )
}

val Color.isDark: Boolean
    get() = (red + green + blue) / 3f < 0.5f
