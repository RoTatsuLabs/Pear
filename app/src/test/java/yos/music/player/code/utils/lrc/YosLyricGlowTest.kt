package yos.music.player.code.utils.lrc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YosLyricGlowTest {

    private fun word(text: String, start: Float, end: Float) =
        YosGlowWord(YosWordTiming(1, text, start, end), text.trim(), 0)

    @Test
    fun onlyHeldWordsGlow() {
        assertEquals(false, YosLyricGlow.canGlow("la", 600f))
        assertEquals(true, YosLyricGlow.canGlow("la", 1600f))
        assertEquals(true, YosLyricGlow.canGlow("love ", 1100f))
        assertEquals(false, YosLyricGlow.canGlow("sunshine", 4000f))
        assertEquals(false, YosLyricGlow.canGlow("well-known", 4000f))
        assertEquals(false, YosLyricGlow.canGlow("", 4000f))
    }

    @Test
    fun aHeldNoteGlowsMoreThanAShortOne() {
        val short = word("love", 0f, 1100f)
        val long = word("love", 0f, 3000f)
        val shortPeak = (0..short.text.length * 40).maxOf { short.bloom(0, it * 50f) }
        val longPeak = (0..long.text.length * 80).maxOf { long.bloom(0, it * 50f) }
        assertTrue(longPeak > shortPeak)
    }

    @Test
    fun theGlowTravelsAlongTheCharacters() {
        val w = word("love", 1000f, 3000f)
        val firstPeakAt = (0..400).first { w.bloom(0, 1000f + it * 10f) >= w.bloom(0, 2000f) * 0.99f }
        val lastPeakAt = (0..400).first { w.bloom(3, 1000f + it * 10f) >= w.bloom(3, 2500f) * 0.99f }
        assertTrue(lastPeakAt > firstPeakAt)
    }

    @Test
    fun noGlowBeforeTheWordOrAfterItRests() {
        val w = word("love", 1000f, 3000f)
        w.text.indices.forEach {
            assertEquals(0f, w.bloom(it, 999f))
            assertEquals(0f, w.bloom(it, w.restsAt + 1f))
        }
    }

    @Test
    fun bloomStaysInRangeAndMovesSmoothly() {
        val w = word("ooh", 0f, 2500f)
        var previous = 0f
        for (t in 0..(w.restsAt.toInt() + 100) step 10) {
            val value = w.bloom(0, t.toFloat())
            assertTrue(value in 0f..1f)
            assertTrue("jump at $t", kotlin.math.abs(value - previous) < 0.05f)
            previous = value
        }
    }

    @Test
    fun wordsKeepTheirPositionInTheLine() {
        val line = listOf(1000f to "", 1500f to "I ", 4000f to " love", 5000f to " you", 1000f to "")
        val glowing = YosLyricGlow.words(YosLyricTiming.timeline(line))
        assertEquals(listOf("love"), glowing.map { it.text })
        assertEquals(3, glowing[0].offset)
    }
}
