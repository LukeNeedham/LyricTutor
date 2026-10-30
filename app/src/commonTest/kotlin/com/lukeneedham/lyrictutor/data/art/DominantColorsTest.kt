package com.lukeneedham.lyrictutor.data.art

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DominantColorsTest {

    private fun argb(red: Int, green: Int, blue: Int, alpha: Int = 255) = (alpha shl 24) or (red shl 16) or (green shl 8) or blue

    private fun pixels(vararg parts: Pair<Int, Int>): IntArray = parts.flatMap { (color, count) -> List(count) { color } }.toIntArray()

    private fun near(actual: Float, expected: Int) = abs(actual * 255f - expected) <= 8f

    @Test
    fun theMostProminentColorIsPrimaryAndTheNextDifferentOneIsSecondary() {
        val colors = assertNotNull(DominantColors.extract(pixels(argb(200, 30, 30) to 600, argb(30, 30, 200) to 300, argb(30, 200, 30) to 100)))
        assertTrue(near(colors.primary.red, 200) && near(colors.primary.blue, 30))
        assertTrue(near(colors.secondary.blue, 200) && near(colors.secondary.red, 30))
    }

    @Test
    fun aShadeOfThePrimaryColorIsNotTheSecondaryColor() {
        // Dark red is the second most common colour, but it is too close to red: blue is the second colour
        val colors = assertNotNull(
            DominantColors.extract(pixels(argb(200, 30, 30) to 500, argb(170, 20, 20) to 300, argb(30, 30, 200) to 200)),
        )
        assertTrue(near(colors.secondary.blue, 200))
    }

    @Test
    fun anImageOfOneColorStillGetsASecondaryColorThatDiffers() {
        val colors = assertNotNull(DominantColors.extract(pixels(argb(40, 90, 160) to 1000)))
        assertTrue(near(colors.primary.blue, 160))
        assertTrue(colors.secondary != colors.primary)
    }

    @Test
    fun greysBlacksAndWhitesAreNeverPickedWhileThereIsAColor() {
        // Most of the cover is black, grey and white; the red and the blue are what the page is painted with
        val colors = assertNotNull(
            DominantColors.extract(
                pixels(argb(0, 0, 0) to 500, argb(128, 128, 128) to 300, argb(250, 250, 250) to 250, argb(200, 30, 30) to 60, argb(30, 30, 200) to 40),
            ),
        )
        assertTrue(near(colors.primary.red, 200) && near(colors.primary.blue, 30))
        assertTrue(near(colors.secondary.blue, 200) && near(colors.secondary.red, 30))
    }

    @Test
    fun withOnlyOneColorTheSecondaryIsAShadeOfItAndNotAGrey() {
        val colors = assertNotNull(DominantColors.extract(pixels(argb(0, 0, 0) to 600, argb(255, 255, 255) to 300, argb(200, 30, 30) to 100)))
        assertTrue(near(colors.primary.red, 200))
        // A lighter version of the red: still red, not grey
        assertTrue(colors.secondary.red > colors.secondary.green + 0.2f && colors.secondary.red > colors.secondary.blue + 0.2f)
    }

    @Test
    fun anImageOfOnlyGreysStillGetsColors() {
        val colors = assertNotNull(DominantColors.extract(pixels(argb(20, 20, 20) to 700, argb(240, 240, 240) to 300)))
        assertTrue(near(colors.primary.red, 20))
        assertTrue(near(colors.secondary.red, 240))
    }

    @Test
    fun transparentPixelsAreNotColors() {
        val colors = assertNotNull(DominantColors.extract(pixels(argb(255, 255, 255, alpha = 0) to 900, argb(200, 30, 30) to 100)))
        assertTrue(near(colors.primary.red, 200))
    }

    @Test
    fun anImageWithNoOpaquePixelHasNoColors() {
        assertNull(DominantColors.extract(pixels(argb(255, 255, 255, alpha = 0) to 100)))
        assertNull(DominantColors.extract(IntArray(0)))
    }
}
