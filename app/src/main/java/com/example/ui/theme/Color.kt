package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/** Night palette switch. Snapshot state, so composables reading any color below recompose when it flips. */
object AppTheme {
    var isNight: Boolean by mutableStateOf(false)
}

// Warm Organic Serenity Design System Colors
// Each color is a getter: `if (AppTheme.isNight) <night> else <day>`.
// Night palette: dim, warm, low-glare (deep warm near-black surfaces, muted amber/olive/plum accents).
val SurfaceColor: Color get() = if (AppTheme.isNight) Color(0xFF17120F) else Color(0xFFF7FAF5)
val SurfaceDim: Color get() = if (AppTheme.isNight) Color(0xFF120E0B) else Color(0xFFD8DBD6)
val SurfaceBright: Color get() = if (AppTheme.isNight) Color(0xFF3A2F27) else Color(0xFFF7FAF5)
val SurfaceContainerLowest: Color get() = if (AppTheme.isNight) Color(0xFF1E1814) else Color(0xFFFFFFFF)
val SurfaceContainerLow: Color get() = if (AppTheme.isNight) Color(0xFF231C17) else Color(0xFFF1F4F0)
val SurfaceContainer: Color get() = if (AppTheme.isNight) Color(0xFF29211B) else Color(0xFFECEFEA)
val SurfaceContainerHigh: Color get() = if (AppTheme.isNight) Color(0xFF30271F) else Color(0xFFE6E9E4)
val SurfaceContainerHighest: Color get() = if (AppTheme.isNight) Color(0xFF372C24) else Color(0xFFE0E3DF)

val OnSurface: Color get() = if (AppTheme.isNight) Color(0xFFE8DCCF) else Color(0xFF191C1A)
val OnSurfaceVariant: Color get() = if (AppTheme.isNight) Color(0xFFBFAE9F) else Color(0xFF55423E)
val InverseSurface: Color get() = if (AppTheme.isNight) Color(0xFFE8DCCF) else Color(0xFF2D312E)
val InverseOnSurface: Color get() = if (AppTheme.isNight) Color(0xFF2E2620) else Color(0xFFEFF2ED)
val OutlineColor: Color get() = if (AppTheme.isNight) Color(0xFF8A7A6C) else Color(0xFF88726D)
val OutlineVariant: Color get() = if (AppTheme.isNight) Color(0xFF4A3F36) else Color(0xFFDBC1BA)

// Primary: Terracotta / Feeding (night: muted amber)
val PrimaryColor: Color get() = if (AppTheme.isNight) Color(0xFFC98B5A) else Color(0xFF9A442D)
val OnPrimary: Color get() = if (AppTheme.isNight) Color(0xFF2A1709) else Color(0xFFFFFFFF)
val PrimaryContainer: Color get() = if (AppTheme.isNight) Color(0xFF9A6440) else Color(0xFFE07A5F)
val OnPrimaryContainer: Color get() = if (AppTheme.isNight) Color(0xFFFFDCC2) else Color(0xFF5B1604)
val PrimaryFixed: Color get() = if (AppTheme.isNight) Color(0xFF4A3020) else Color(0xFFFFDBD2)
val PrimaryFixedDim: Color get() = if (AppTheme.isNight) Color(0xFF6B4529) else Color(0xFFFFB4A1)
val OnPrimaryFixed: Color get() = if (AppTheme.isNight) Color(0xFFFFDCC2) else Color(0xFF3C0800)
val OnPrimaryFixedVariant: Color get() = if (AppTheme.isNight) Color(0xFFE3B48F) else Color(0xFF7C2E19)

// Secondary: warm olive — "all good" states (synced, saved), from the same earthy family as the item colors
val SecondaryColor: Color get() = if (AppTheme.isNight) Color(0xFFAAB27A) else Color(0xFF5E6A2E)
val OnSecondary: Color get() = if (AppTheme.isNight) Color(0xFF1E2108) else Color(0xFFFFFFFF)
val SecondaryContainer: Color get() = if (AppTheme.isNight) Color(0xFF33361F) else Color(0xFFE6EACB)
val OnSecondaryContainer: Color get() = if (AppTheme.isNight) Color(0xFFDDE2B8) else Color(0xFF33361F)
val SecondaryFixed: Color get() = if (AppTheme.isNight) Color(0xFF2F321C) else Color(0xFFE6EACB)
val SecondaryFixedDim: Color get() = if (AppTheme.isNight) Color(0xFF4A4F2E) else Color(0xFFC9CF9F)
val OnSecondaryFixed: Color get() = if (AppTheme.isNight) Color(0xFFE2E6C3) else Color(0xFF1E2108)
val OnSecondaryFixedVariant: Color get() = if (AppTheme.isNight) Color(0xFFC2C894) else Color(0xFF464F1E)

// Tertiary: muted plum — accents that aren't a tracked item (item colors live in ItemColors.kt)
val TertiaryColor: Color get() = if (AppTheme.isNight) Color(0xFFC49AB0) else Color(0xFF7D4A66)
val OnTertiary: Color get() = if (AppTheme.isNight) Color(0xFF2E1121) else Color(0xFFFFFFFF)
val TertiaryContainer: Color get() = if (AppTheme.isNight) Color(0xFF5A3A4B) else Color(0xFFB98AA3)
val OnTertiaryContainer: Color get() = if (AppTheme.isNight) Color(0xFFF2DDE8) else Color(0xFF3A1C2C)
val TertiaryFixed: Color get() = if (AppTheme.isNight) Color(0xFF3D2A33) else Color(0xFFF2DDE8)
val TertiaryFixedDim: Color get() = if (AppTheme.isNight) Color(0xFF5A3A4B) else Color(0xFFDDB9CB)
val OnTertiaryFixed: Color get() = if (AppTheme.isNight) Color(0xFFF2DDE8) else Color(0xFF2E1121)
val OnTertiaryFixedVariant: Color get() = if (AppTheme.isNight) Color(0xFFDDB9CB) else Color(0xFF62344D)

val ErrorColor: Color get() = if (AppTheme.isNight) Color(0xFFE58F86) else Color(0xFFBA1A1A)
val OnError: Color get() = if (AppTheme.isNight) Color(0xFF3B0B07) else Color(0xFFFFFFFF)
val ErrorContainer: Color get() = if (AppTheme.isNight) Color(0xFF5C1F1A) else Color(0xFFFFDAD6)
val OnErrorContainer: Color get() = if (AppTheme.isNight) Color(0xFFF8D4CF) else Color(0xFF93000A)
