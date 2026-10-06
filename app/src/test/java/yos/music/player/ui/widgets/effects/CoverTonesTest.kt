package yos.music.player.ui.widgets.effects

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoverTonesTest {

    private fun solid(color: Int, size: Int = 4) = IntArray(size * size) { color }

    private fun red(c: Int) = (c shr 16) and 0xFF
    private fun green(c: Int) = (c shr 8) and 0xFF
    private fun blue(c: Int) = c and 0xFF

    @Test
    fun tonesAreOpaqueAndGetDarkerDownTheBackdrop() {
        val tones = coverTones(solid(0xFF806040.toInt()), 4)
        assertEquals(0xFF, tones.edge ushr 24)
        assertTrue(red(tones.edge) > red(tones.mid))
        assertTrue(red(tones.mid) > red(tones.end))
    }

    @Test
    fun theEdgeFollowsTheBottomOfTheCover() {
        val pixels = IntArray(16) { if (it < 12) 0xFF000000.toInt() else 0xFFC8C8C8.toInt() }
        val tones = coverTones(pixels, 4)
        assertTrue(red(tones.edge) > 100)
        assertTrue(red(tones.mid) < 100)
    }

    @Test
    fun aBrightCoverIsDarkenedMore() {
        val dark = coverTones(solid(0xFF404040.toInt()), 4)
        val bright = coverTones(solid(0xFFF0F0F0.toInt()), 4)
        val darkKept = red(dark.edge) / 64f
        val brightKept = red(bright.edge) / 240f
        assertTrue(brightKept < darkKept)
    }

    @Test
    fun anAllBlackCoverStaysBlack() {
        val tones = coverTones(solid(0xFF000000.toInt()), 4)
        assertEquals(0, red(tones.end) + green(tones.end) + blue(tones.end))
    }
}
