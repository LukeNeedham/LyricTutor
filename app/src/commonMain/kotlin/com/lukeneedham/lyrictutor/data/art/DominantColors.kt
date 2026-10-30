package com.lukeneedham.lyrictutor.data.art

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.sqrt

/**
 * Finds the two colours of an image from its pixels, with no platform code. The pixels are counted in 4096 colour bins (four bits for each of
 * red, green and blue), and the neighbouring bins of a colour are merged into it, so a smooth shade is one colour and not many.
 */
object DominantColors {

    /** Bins closer than this (distance in RGB, out of about 441) to a colour count as that colour. */
    private const val SAME_COLOR_DISTANCE = 48.0

    /** The secondary colour is at least this far from the primary one. */
    private const val DIFFERENT_COLOR_DISTANCE = 100.0

    /** A colour needs at least this much saturation, and this much brightness, to count as a colour and not as a grey, black or white. */
    private const val MIN_SATURATION = 0.25
    private const val MIN_BRIGHTNESS = 0.25

    /** Pixels less opaque than this are the background of the image, not a colour of it. */
    private const val MIN_ALPHA = 128

    /** [pixels] are ARGB, as `Bitmap.getPixels` gives them. Null when there is no opaque pixel. */
    fun extract(pixels: IntArray): ArtColors? {
        val allBins = countBins(pixels)
        if (allBins.isEmpty()) return null

        // Greys, blacks and whites are never a colour of the page, unless the image has nothing else
        val bins = allBins.filter { it.color.isChromatic() }.ifEmpty { allBins }

        val primaryBin = bins.first()
        val primary = merged(around = primaryBin, bins = bins)

        // The most prominent bin that is far from the primary colour is the centre of the secondary one
        val secondaryBin = bins.firstOrNull { it.color.distanceTo(primary) >= DIFFERENT_COLOR_DISTANCE }
        val secondary = if (secondaryBin != null) {
            merged(around = secondaryBin, bins = bins.filter { it.color.distanceTo(primary) >= DIFFERENT_COLOR_DISTANCE })
        } else {
            // Only one colour (or shades of it) besides greys: a lighter or darker version of it is the best second colour there is
            lerp(primary, if (primary.luminance() > 0.5f) Color.Black else Color.White, 0.4f)
        }
        return ArtColors(primary = primary.toColor(), secondary = secondary.toColor())
    }

    /** The bins that have pixels, the most pixels first. */
    private fun countBins(pixels: IntArray): List<Bin> {
        val counts = IntArray(BIN_COUNT)
        val sums = Array(3) { LongArray(BIN_COUNT) }
        for (pixel in pixels) {
            if ((pixel ushr 24) < MIN_ALPHA) continue
            val red = (pixel shr 16) and 0xFF
            val green = (pixel shr 8) and 0xFF
            val blue = pixel and 0xFF
            val bin = ((red shr 4) shl 8) or ((green shr 4) shl 4) or (blue shr 4)
            counts[bin]++
            sums[0][bin] += red.toLong()
            sums[1][bin] += green.toLong()
            sums[2][bin] += blue.toLong()
        }
        return (0 until BIN_COUNT)
            .filter { counts[it] > 0 }
            .map { bin ->
                val count = counts[bin]
                Bin(
                    count = count,
                    color = Rgb(sums[0][bin].toDouble() / count, sums[1][bin].toDouble() / count, sums[2][bin].toDouble() / count),
                )
            }
            .sortedByDescending { it.count }
    }

    /** The average colour of the [bins] near [around], weighted by their pixels. */
    private fun merged(around: Bin, bins: List<Bin>): Rgb {
        var count = 0.0
        var red = 0.0
        var green = 0.0
        var blue = 0.0
        for (bin in bins) {
            if (bin.color.distanceTo(around.color) > SAME_COLOR_DISTANCE) continue
            count += bin.count
            red += bin.color.red * bin.count
            green += bin.color.green * bin.count
            blue += bin.color.blue * bin.count
        }
        return Rgb(red / count, green / count, blue / count)
    }

    private const val BIN_COUNT = 4096

    private class Bin(val count: Int, val color: Rgb)

    /** A colour with channels from 0 to 255, kept as numbers so that averaging loses nothing. */
    private class Rgb(val red: Double, val green: Double, val blue: Double) {
        /** Neither grey (too little saturation, which also rules out white) nor black (too dark). */
        fun isChromatic(): Boolean {
            val brightest = maxOf(red, green, blue)
            val darkest = minOf(red, green, blue)
            val brightness = brightest / 255.0
            val saturation = if (brightest == 0.0) 0.0 else (brightest - darkest) / brightest
            return saturation >= MIN_SATURATION && brightness >= MIN_BRIGHTNESS
        }

        fun distanceTo(other: Rgb): Double {
            val r = red - other.red
            val g = green - other.green
            val b = blue - other.blue
            return sqrt(r * r + g * g + b * b)
        }

        fun toColor() = Color(red = red.toInt().coerceIn(0, 255), green = green.toInt().coerceIn(0, 255), blue = blue.toInt().coerceIn(0, 255))
    }

    private fun lerp(from: Rgb, to: Color, fraction: Float): Rgb {
        val target = Rgb(to.red * 255.0, to.green * 255.0, to.blue * 255.0)
        return Rgb(
            from.red + (target.red - from.red) * fraction,
            from.green + (target.green - from.green) * fraction,
            from.blue + (target.blue - from.blue) * fraction,
        )
    }

    private fun Rgb.luminance(): Float = toColor().luminance()
}
