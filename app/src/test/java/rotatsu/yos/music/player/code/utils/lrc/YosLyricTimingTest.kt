package rotatsu.yos.music.player.code.utils.lrc

import org.junit.Assert.assertEquals
import org.junit.Test

class YosLyricTimingTest {

    @Test
    fun timelineStartsEachWordWhereThePreviousOneEnded() {
        val line = listOf(1000f to "", 1400f to "Hel", 1800f to "lo ", 2500f to "world", 1000f to "")
        val words = YosLyricTiming.timeline(line)

        assertEquals(listOf("Hel", "lo ", "world"), words.map { it.text })
        assertEquals(listOf(1000f, 1400f, 1800f), words.map { it.start })
        assertEquals(listOf(1400f, 1800f, 2500f), words.map { it.end })
        assertEquals(listOf(1, 2, 3), words.map { it.pairIndex })
    }

    @Test
    fun aPausePairMovesTheClockSoTheNextWordStartsLate() {
        // sung 1000-1500, silent until 3000, then 3000-3500
        val line = listOf(1000f to "", 1500f to "one", 3000f to "", 3500f to "two", 1000f to "")
        val words = YosLyricTiming.timeline(line)

        assertEquals(3000f, words[1].start)
        assertEquals(3500f, words[1].end)
    }

    @Test
    fun timesNeverRunBackwards() {
        // the trailing slot pairs carry the line start, which is earlier than the clock
        val line = listOf(1000f to "", 2000f to "word", 1000f to "", 1000f to "")
        val words = YosLyricTiming.timeline(line)

        assertEquals(1, words.size)
        assertEquals(1000f, words[0].start)
        assertEquals(2000f, words[0].end)
    }

    @Test
    fun aLineLevelLineIsOneInstantWord() {
        val words = YosLyricTiming.timeline(listOf(5000f to "", 5000f to "whole line", 5000f to ""))

        assertEquals(1, words.size)
        assertEquals(5000f, words[0].start)
        assertEquals(5000f, words[0].end)
    }

    @Test
    fun charactersShareTheirWordEvenly() {
        val word = YosWordTiming(1, "abcd", 1000f, 2000f)

        assertEquals(1000f, word.charStart(0))
        assertEquals(1250f, word.charEnd(0))
        assertEquals(1750f, word.charStart(3))
        assertEquals(2000f, word.charEnd(3))
    }

    @Test
    fun progressIsClampedAndSafeForEmptySpans() {
        assertEquals(0f, YosLyricTiming.progress(500f, 1000f, 2000f))
        assertEquals(0.5f, YosLyricTiming.progress(1500f, 1000f, 2000f))
        assertEquals(1f, YosLyricTiming.progress(9000f, 1000f, 2000f))
        assertEquals(0f, YosLyricTiming.progress(999f, 1000f, 1000f))
        assertEquals(1f, YosLyricTiming.progress(1000f, 1000f, 1000f))
        assertEquals(-1f, YosLyricTiming.rawProgress(999f, 1000f, 1000f))
        assertEquals(1.5f, YosLyricTiming.rawProgress(2500f, 1000f, 2000f))
    }

    @Test
    fun creditLabelsAreOnlyHeadings() {
        val writers = YosLyricCredit("songwriters", listOf("A B", "C D"))
        assertEquals("Written by", writers.label)
        assertEquals(listOf("A B", "C D"), writers.values)
        assertEquals("Copyright", YosLyricCredit("copyright", listOf("x")).label)
        assertEquals("Producers", YosLyricCredit("producers", listOf("x")).label)
    }
}
