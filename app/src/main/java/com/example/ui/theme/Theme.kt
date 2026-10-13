package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Warm Organic Serenity Palette (Mandatory 1:1 match with user HTML specification).
// Built from the Color.kt getters, so it reflects the current AppTheme.isNight value when called.
fun lightAppColorScheme(): ColorScheme = lightColorScheme(
    primary = PrimaryColor,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimary,
    inversePrimary = PrimaryFixedDim,
    secondary = SecondaryColor,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = TertiaryColor,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    background = SurfaceColor,
    onBackground = OnSurface,
    surface = SurfaceColor,
    onSurface = OnSurface,
    surfaceVariant = SurfaceContainerHighest,
    onSurfaceVariant = OnSurfaceVariant,
    surfaceTint = PrimaryColor,
    inverseSurface = InverseSurface,
    inverseOnSurface = InverseOnSurface,
    surfaceBright = SurfaceBright,
    surfaceDim = SurfaceDim,
    surfaceContainerLowest = SurfaceContainerLowest,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
    outline = OutlineColor,
    outlineVariant = OutlineVariant,
    error = ErrorColor,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer
)

/** Kept for compatibility with the former top-level val; reflects the current palette. */
val LightColorScheme: ColorScheme get() = lightAppColorScheme()

// Night palette: dim, warm, low-glare. Call only while AppTheme.isNight is true so the getters return night values.
fun nightAppColorScheme(): ColorScheme = darkColorScheme(
    primary = PrimaryColor,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    inversePrimary = Color(0xFF9A442D),
    secondary = SecondaryColor,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = TertiaryColor,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    background = SurfaceColor,
    onBackground = OnSurface,
    surface = SurfaceColor,
    onSurface = OnSurface,
    surfaceVariant = SurfaceContainerHighest,
    onSurfaceVariant = OnSurfaceVariant,
    surfaceTint = PrimaryColor,
    inverseSurface = InverseSurface,
    inverseOnSurface = InverseOnSurface,
    surfaceBright = SurfaceBright,
    surfaceDim = SurfaceDim,
    surfaceContainerLowest = SurfaceContainerLowest,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
    outline = OutlineColor,
    outlineVariant = OutlineVariant,
    scrim = Color.Black,
    error = ErrorColor,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer
)

@Composable
fun MyApplicationTheme(
    // Kept for signature compatibility. Day/night is driven solely by AppTheme.isNight; dynamic color is never used.
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = false,
    @Suppress("UNUSED_PARAMETER") dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Reading AppTheme.isNight (snapshot state) here makes the theme recompose when it flips.
    val colorScheme = if (AppTheme.isNight) nightAppColorScheme() else lightAppColorScheme()
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
