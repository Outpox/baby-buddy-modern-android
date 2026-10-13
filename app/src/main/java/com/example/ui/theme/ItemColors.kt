package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.data.model.TrackedItem

/**
 * One color per tracked item, all from the same warm, earthy family as the terracotta primary, so items
 * are told apart without leaving the palette. [tint] is for icons and History dots, [soft] for tile and
 * icon backgrounds. Used the same way on Today, the Log screen and History.
 */
object ItemColors {
    private class Swatch(val day: Long, val daySoft: Long, val night: Long, val nightSoft: Long)

    private fun spec(item: TrackedItem): Swatch = when (item) {
        TrackedItem.BREASTFEEDING -> Swatch(0xFF9A442D, 0xFFFFDBD2, 0xFFC98B5A, 0xFF4A3020)

        // terracotta (primary)
        TrackedItem.BOTTLE -> Swatch(0xFFAD5F1A, 0xFFFFE1C4, 0xFFD9A066, 0xFF4A3418)

        // amber
        TrackedItem.DIAPERS -> Swatch(0xFF8A6A0E, 0xFFF5E7B8, 0xFFCDB062, 0xFF3F3518)

        // mustard
        TrackedItem.SLEEP -> Swatch(0xFF7D4A66, 0xFFF2DDE8, 0xFFC49AB0, 0xFF3D2A33)

        // plum
        TrackedItem.PUMPING, TrackedItem.MILK_STASH -> Swatch(0xFFA04556, 0xFFF9DADF, 0xFFD2909C, 0xFF472A2F)

        // dusty rose
        TrackedItem.TUMMY_TIME -> Swatch(0xFF7A5639, 0xFFEDDFD0, 0xFFBF9C7E, 0xFF3C2E22)

        // cocoa
        TrackedItem.BATH -> Swatch(0xFF5E6A2E, 0xFFE6EACB, 0xFFAAB27A, 0xFF33361F)

        // olive
        TrackedItem.GROWTH -> Swatch(0xFF6D5D52, 0xFFE9E0D9, 0xFFB5A496, 0xFF37302A)

        // taupe
        TrackedItem.QUICK_NOTES -> Swatch(0xFF55423E, 0xFFEDE3DF, 0xFFBFAE9F, 0xFF30271F) // warm grey
    }

    fun tint(item: TrackedItem): Color = spec(item).let { Color(if (AppTheme.isNight) it.night else it.day) }

    fun soft(item: TrackedItem): Color = spec(item).let { Color(if (AppTheme.isNight) it.nightSoft else it.daySoft) }
}
