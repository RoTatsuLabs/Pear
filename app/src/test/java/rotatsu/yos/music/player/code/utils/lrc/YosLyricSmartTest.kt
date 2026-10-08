package rotatsu.yos.music.player.code.utils.lrc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class YosLyricSmartTest {

    private fun lyrics(vararg lines: List<Pair<Float, String>>) = YosLyrics(
        format = YosLyricFormat.LRC,
        entries = lines.toList(),
        otherSide = lines.map { false },
        transliterations = lines.map { null },
        credits = emptyList()
    )

    private fun plain(begin: Float, text: String) =
        listOf(begin to "", begin to text, begin to "")

    @Test
    fun spacedWordsGetTimesUpToTheNextLine() {
        val smart = YosLyricSmart.expand(
            lyrics(plain(1000f, "Hello big world"), plain(5000f, "Next"))
        )

        val line = smart.entries[0]
        assertEquals(listOf("Hello ", "big ", "world"), YosLyrics.segmentsOf(line).map { it.second })
        val ends = YosLyrics.segmentsOf(line).map { it.first }
        assertEquals(ends.sorted(), ends)
        assertEquals(1000f + 4000f * 0.9f, ends.last(), 0.5f)
        assertEquals(1000f to "", line.first())
    }

    @Test
    fun everyChineseJapaneseOrKoreanCharacterStandsAlone() {
        assertEquals(listOf("笑", "顔", "に"), YosLyricSmart.split("笑顔に"))
        assertEquals(listOf("hi ", "笑", "顔"), YosLyricSmart.split("hi 笑顔"))
    }

    @Test
    fun linesWithWordTimingStayAsTheyAre() {
        val timed = listOf(1000f to "", 1500f to "Hi ", 2000f to "you", 1000f to "")
        val source = lyrics(timed, plain(5000f, "Next"))

        assertSame(timed, YosLyricSmart.expand(source).entries[0])
    }

    @Test
    fun aTranslationStaysAtTheEndAndTheOldTransliterationGoes() {
        val withTranslation = listOf(
            1000f to "", 1000f to "안녕", 1000f to "", 1000f to "", 1000f to "Hello"
        )
        val source = lyrics(withTranslation, plain(5000f, "Next")).copy(
            transliterations = listOf(listOf("annyeong"), null)
        )

        val smart = YosLyricSmart.expand(source)

        assertEquals(listOf("안", "녕"), YosLyrics.segmentsOf(smart.entries[0]).map { it.second })
        assertEquals("Hello", smart.entries[0].last().second)
        assertNull(smart.transliterations[0])
    }

    @Test
    fun theLastLineSweepsOverAFixedTime() {
        val smart = YosLyricSmart.expand(lyrics(plain(2000f, "Bye")))

        val end = YosLyrics.segmentsOf(smart.entries[0]).single().first
        assertTrue(end > 2000f && end <= 2000f + 7000f)
    }
}
