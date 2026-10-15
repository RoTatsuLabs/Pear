package rotatsu.yos.music.player.code.utils.lrc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class YosAmllLinesTest {
    private fun near(a: Float, b: Float, e: Float = 1e-3f) = abs(a - b) < e

    private val pairs = listOf(
        0f to "", 200f to "As ", 500f to "they ", 1100f to "sunk ", 2200f to "into ",
        2700f to "each ", 3100f to "other's ", 3600f to "heart",
    )

    private fun YosAmllLine.wordTexts() = words.map { text.substring(it.charStart, it.charEnd) }

    @Test
    fun wordsAreCutAtTheirWhitespaceAndOnlyAHeldWordIsEmphasized() {
        val line = YosAmllLines.fromPairs(pairs)
        assertEquals("As they sunk into each other's heart", line.text)
        assertEquals(listOf("As", "they", "sunk", "into", "each", "other's", "heart"), line.wordTexts())
        assertEquals(listOf(false, false, false, true, false, false, false), line.chunks.map { it.emphasized })
        val into = line.chunks[3]
        assertTrue(near(into.start, 1100f) && near(into.end, 2200f) && into.chars.size == 4)
        assertTrue(into.amount > 0f && into.blur > 0f && near(into.duration, 1100f))
    }

    @Test
    fun aSegmentOfSeveralWordsIsSplitAndItsTimeSharedByLetters() {
        val line = YosAmllLines.fromPairs(
            listOf(0f to "", 200f to "As ", 500f to "they ", 1100f to "sunk ", 6000f to "into each other's heart"),
        )
        assertEquals(listOf("As", "they", "sunk", "into", "each", "other's", "heart"), line.wordTexts())
        val perLetter = (6000f - 1100f) / 20f
        val into = line.words[3]
        assertTrue(near(into.start, 1100f) && near(into.end, 1100f + 4 * perLetter, 0.5f))
        assertTrue(near(line.words[4].start, 1100f + 4 * perLetter, 0.5f))
        assertTrue(near(line.words[6].end, 6000f, 0.5f))
        // The two long ones pass a second and are short enough.
        assertEquals(listOf(false, false, false, false, false, true, true), line.chunks.map { it.emphasized })
    }

    @Test
    fun syllablesOfOneWordAreOneChunk() {
        val two = YosAmllLines.fromPairs(listOf(0f to "", 300f to "mo", 800f to "ther ", 2300f to "love"))
        assertEquals(2, two.chunks.size)
        assertEquals(1, two.chunks[0].lastWord - two.chunks[0].firstWord)
        assertFalse(two.chunks[0].emphasized)
        val held = YosAmllLines.fromPairs(listOf(0f to "", 300f to "mo", 1600f to "ther "))
        assertEquals(1, held.chunks.size)
        assertTrue(held.chunks[0].emphasized)
        assertEquals(6, held.chunks[0].chars.size)
        // The last word is stronger than the same word would be elsewhere.
        assertTrue(near(held.chunks[0].amount, YosAmllEffects.params(1600f, true).amount))
    }

    @Test
    fun emphasisNeedsASecondAndTwoToSevenLettersExceptForHanKanaAndHangul() {
        assertFalse(YosAmllEffects.shouldEmphasize(1500f, 8, false))
        assertFalse(YosAmllEffects.shouldEmphasize(1500f, 1, false))
        assertTrue(YosAmllEffects.shouldEmphasize(1000f, 7, false))
        assertFalse(YosAmllEffects.shouldEmphasize(999f, 4, false))
        assertTrue(YosAmllEffects.shouldEmphasize(1000f, 1, true))
        assertFalse(YosAmllEffects.shouldEmphasize(900f, 1, true))
        val line = YosAmllLines.fromPairs(listOf(0f to "", 600f to "你", 1900f to "好"))
        assertEquals(listOf(false, true), line.chunks.map { it.emphasized })
    }

    @Test
    fun theEmphasisParametersFollowAmll() {
        val p = YosAmllEffects.params(2000f, false)
        assertTrue(near(p.amount, 0.6f, 1e-4f))
        assertTrue(near(p.blur, (2000f / 3000f).let { it * it * it } * 0.5f, 1e-4f))
        val last = YosAmllEffects.params(2000f, true)
        assertTrue(near(last.duration, 2400f) && near(last.amount, 0.96f, 1e-4f))
        val big = YosAmllEffects.params(9000f, true)
        assertTrue(near(big.amount, 1.2f) && near(big.blur, 0.8f))
    }

    @Test
    fun theEasingsRunFromZeroToOneAndTheEmphasisReturns() {
        for (curve in listOf(listOf(0.2f, 0.4f, 0.58f, 1f), listOf(0f, 0f, 0.58f, 1f), listOf(0.3f, 0f, 0.58f, 1f))) {
            var previous = -1f
            for (i in 0..50) {
                val v = YosAmllEffects.bezier(curve[0], curve[1], curve[2], curve[3], i / 50f)
                assertTrue(v >= previous - 1e-4f)
                previous = v
            }
            assertTrue(near(YosAmllEffects.bezier(curve[0], curve[1], curve[2], curve[3], 0f), 0f))
            assertTrue(near(YosAmllEffects.bezier(curve[0], curve[1], curve[2], curve[3], 1f), 1f))
        }
        assertTrue(near(YosAmllEffects.bezier(0.25f, 0.25f, 0.75f, 0.75f, 0.37f), 0.37f))
        assertTrue(near(YosAmllEffects.empEasing(0f), 0f))
        assertTrue(near(YosAmllEffects.empEasing(0.5f), 1f))
        assertTrue(near(YosAmllEffects.empEasing(1f), 0f))
    }

    @Test
    fun aLetterSwellsAtItsTurnAndThenSettles() {
        val into = YosAmllLines.fromPairs(pairs).chunks[3]
        val effect = YosAmllCharEffect()
        YosAmllEffects.charEffect(into, 0, 0f, effect)
        assertTrue(near(effect.scale, 1f) && near(effect.glowAlpha, 0f))
        val begin = into.start + into.duration / 2.5f / 4f
        YosAmllEffects.charEffect(into, 1, begin + into.duration / 2f, effect)
        assertTrue(near(effect.scale, 1f + 0.1f * into.amount))
        assertTrue(near(effect.glowAlpha, into.blur))
        assertTrue(near(effect.offsetY, -0.025f * into.amount, 1e-4f))
        assertTrue(near(effect.glowRadius, minOf(0.3f, into.blur * 0.3f)))
        YosAmllEffects.charEffect(into, 1, begin + into.duration + 5f, effect)
        assertTrue(near(effect.scale, 1f) && near(effect.glowAlpha, 0f))
    }

    @Test
    fun everyWordFloatsUpAndStaysThere() {
        assertTrue(near(YosAmllEffects.wordFloat(1000f, 1300f, 900f), 0f))
        assertTrue(near(YosAmllEffects.wordFloat(1000f, 1300f, 3000f), -0.05f, 1e-4f))
        var last = 0f
        for (t in 1000..2000 step 50) {
            val f = YosAmllEffects.wordFloat(1000f, 1300f, t.toFloat())
            assertTrue(f <= last + 1e-5f)
            last = f
        }
    }

    @Test
    fun theEdgeCrossesEachWordInItsOwnTimeAndRestsInAPause() {
        val left = floatArrayOf(0f, 130f, 360f)
        val width = floatArrayOf(100f, 200f, 50f)
        val sweep = YosAmllSweep(left, width, floatArrayOf(0f, 1000f, 3000f), floatArrayOf(500f, 2000f, 3400f), 40f)
        val c = FloatArray(3)
        sweep.centers(-100f, c)
        assertEquals(listOf(-20f, 110f, 340f), c.toList())
        sweep.centers(500f, c)
        assertTrue(near(c[0], 100f) && near(c[1], left[1]))
        sweep.centers(1500f, c)
        assertTrue(near(c[1], left[1] + 100f, 1e-2f))
        sweep.centers(2500f, c)
        assertTrue(near(c[1], left[1] + width[1], 1e-2f) && near(c[2], left[2], 1e-2f))
        sweep.centers(3400f, c)
        assertTrue(near(c[2], left[2] + width[2] + 20f, 1e-2f))
        val previous = FloatArray(3) { -1e9f }
        for (t in -200..3600 step 25) {
            sweep.centers(t.toFloat(), c)
            for (k in 0..2) {
                assertTrue(c[k] >= previous[k] - 1e-3f)
                previous[k] = c[k]
            }
        }
    }
}
