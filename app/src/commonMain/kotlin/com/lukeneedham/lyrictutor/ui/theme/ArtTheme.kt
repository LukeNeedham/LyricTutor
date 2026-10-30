package com.lukeneedham.lyrictutor.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.lukeneedham.lyrictutor.data.art.ArtColors

/**
 * The theme of a song's page: [ArtColors.primary] and [ArtColors.secondary] take the place of the app's own accents. Album art can be any
 * colour, so each is first pulled toward white or black until it can be seen against the page, and the text on it is chosen for it.
 */
internal fun AppThemeValues.withArtColors(art: ArtColors): AppThemeValues {
    val isDark = background.luminance() < 0.5f
    val containerStrength = if (isDark) 0.35f else 0.2f
    val primary = art.primary.ensureContrast(against = background, minRatio = 3.0)
    val secondary = art.secondary.ensureContrast(against = background, minRatio = 3.0)
    val primaryContainer = lerp(background, art.primary, containerStrength)
    val secondaryContainer = lerp(background, art.secondary, containerStrength)
    return copy(
        primary = primary,
        onPrimary = primary.readableOn(),
        primaryContainer = primaryContainer,
        onPrimaryContainer = art.primary.ensureContrast(against = primaryContainer, minRatio = 4.5),
        secondary = secondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = art.secondary.ensureContrast(against = secondaryContainer, minRatio = 4.5),
        gradientStart = art.primary,
        gradientEnd = art.secondary,
    )
}

/** Black or white, whichever reads better on this colour. */
private fun Color.readableOn(): Color = if (luminance() > 0.35f) Color(0xFF111111) else Color.White

/** This colour moved toward white (on a dark [against]) or black (on a light one) in steps, until it has [minRatio] of contrast with it. */
private fun Color.ensureContrast(against: Color, minRatio: Double): Color {
    val target = if (against.luminance() < 0.5f) Color.White else Color.Black
    var result = this
    repeat(12) {
        if (result.contrastWith(against) >= minRatio) return result
        result = lerp(result, target, 0.15f)
    }
    return result
}

private fun Color.contrastWith(other: Color): Double {
    val a = luminance().toDouble()
    val b = other.luminance().toDouble()
    return (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)
}
