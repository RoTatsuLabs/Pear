package rotatsu.yos.music.player.code.utils.lrc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YosBcLinesTest {

    @Test
    fun syllablesOfOneWordBecomeOneWordWithSyllables() {
        val line = YosBcLines.fromPairs(listOf(0f to "", 300f to "Hel", 500f to "lo ", 800f to "world"))
        assertEquals("Hello world", line.text)
        assertEquals(listOf("Hello", "world"), line.words.map { it.text })
        assertEquals(2, line.words[0].syllables.size)
        assertEquals(3, line.words[0].syllables[0].endChar)
        assertEquals(0L, line.words[0].startMs)
        assertEquals(800L, line.words[1].endMs)
        assertEquals(listOf(0 until 5, 6 until 11), line.wordSpans)
    }

    @Test
    fun aPauseInsideTheLineIsNotPartOfTheNextWord() {
        val line = YosBcLines.fromPairs(listOf(1000f to "", 1200f to "Hi ", 1200f to "", 1800f to "there"))
        assertEquals(listOf("Hi", "there"), line.words.map { it.text })
        assertTrue(line.words[1].startMs >= 1200L)
    }

    @Test
    fun everySegmentOfABlockScriptIsAWordOfItsOwn() {
        val line = YosBcLines.fromPairs(listOf(0f to "", 200f to "愛", 500f to "し", 900f to "て"))
        assertEquals(listOf("愛", "し", "て"), line.words.map { it.text })
    }

    @Test
    fun aLineWithoutTextHasNoWords() {
        val line = YosBcLines.fromPairs(listOf(500f to ""))
        assertFalse(line.isWordSynced)
        assertEquals(500L, line.timeMs)
    }

    @Test
    fun theSweepReachesTheEndOfTheLineWhenItIsSung() {
        val line = YosBcLines.fromPairs(listOf(0f to "", 300f to "Hel", 500f to "lo ", 800f to "world"))
        assertEquals(0f, line.revealedChars(0L))
        assertEquals(line.text.length.toFloat(), line.revealedChars(900L))
        assertTrue(line.revealedChars(400L) > 3f && line.revealedChars(400L) < 5f)
    }

    @Test
    fun aWordLiftsWhileSungAndSettlesAfterwards() {
        val line = YosBcLines.fromPairs(listOf(0f to "", 1000f to "hold"))
        assertEquals(0f, line.wordLift(0, 0L))
        assertTrue(line.wordLift(0, 900L) > 0.2f)
        assertEquals(0f, line.wordLift(0, 1000L + 800L), 0.0001f)
        assertTrue(line.isLifted(1200L))
        assertFalse(line.isLifted(3000L))
    }

    @Test
    fun onlyAHeldWordGrowsLetterByLetter() {
        val quick = YosBcLines.fromPairs(listOf(0f to "", 200f to "oh"))
        val held = YosBcLines.fromPairs(listOf(0f to "", 2500f to "oh"))
        assertTrue(quick.growingWords.isEmpty())
        assertEquals(1, held.growingWords.size)
        assertTrue(held.animatesUntilMs > held.endMs)
    }
}
