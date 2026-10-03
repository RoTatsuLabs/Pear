package yos.music.player.code.utils.lrc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YosLyricsParsingTest {

    // ---- LRC ----------------------------------------------------------------------

    @Test
    fun lrcPlainLinesKeepTheLineShape() {
        val lyrics = YosLrcFactory().parse("[00:10.00]Hello world\n[00:14.50]Second line\n")

        assertEquals(YosLyricFormat.LRC, lyrics.format)
        assertEquals(
            listOf(10000f to "", 10000f to "Hello world", 10000f to ""),
            lyrics.entries[0]
        )
        assertEquals(14500f, lyrics.entries[1].first().first)
        assertEquals(lyrics.entries.size, lyrics.otherSide.size)
        assertEquals(lyrics.entries.size, lyrics.transliterations.size)
    }

    @Test
    fun lrcTranslationIsTheLastPair() {
        val lyrics = YosLrcFactory().parse("[00:10.00]你好\n[00:10.00]Hello\n")

        assertEquals(1, lyrics.entries.size)
        assertEquals("Hello", lyrics.entries[0].last().second)
        assertEquals(listOf(10000f to "你好"), YosLyrics.segmentsOf(lyrics.entries[0]))
    }

    @Test
    fun lrcWordTimesBecomeSegments() {
        val lyrics = YosLrcFactory().parse("[00:10.00]<00:10.00>Hel<00:10.40>lo <00:10.80>world<00:11.30>\n")
        val segments = YosLyrics.segmentsOf(lyrics.entries.single())

        assertEquals("Hel", segments[0].second)
        assertEquals(10400f, segments[0].first)
        assertEquals("world", segments.last { it.second.isNotEmpty() }.second)
    }

    @Test
    fun lrcDuetSwitchesSides() {
        val lyrics = YosLrcFactory().parse(
            "[00:10.00]Alice:\n[00:11.00]Hi there\n[00:15.00]Bob:\n[00:16.00]Hello back\n"
        )

        // each "Singer:" line flips the side, the sung lines after it keep it
        assertEquals(listOf(true, true, false, false), lyrics.otherSide)
    }

    @Test
    fun lrcEmptyTextGivesEmptyLyrics() {
        assertTrue(YosLrcFactory().parse("").isEmpty)
        assertTrue(YosLyricsFactory.parse("   \n").isEmpty)
        assertTrue(YosLyricsFactory.parse(null).isEmpty)
    }

    // ---- format detection ----------------------------------------------------------

    @Test
    fun formatIsDetectedFromTheContent() {
        assertTrue(YosLyricsFactory.looksLikeTtml("\uFEFF<?xml version=\"1.0\"?>\n<tt xmlns=\"http://www.w3.org/ns/ttml\"></tt>"))
        assertTrue(YosLyricsFactory.looksLikeTtml("<tt:tt xmlns:tt=\"http://www.w3.org/ns/ttml\"/>"))
        assertEquals(false, YosLyricsFactory.looksLikeTtml("[00:10.00]<b>not ttml</b>"))
        assertEquals(false, YosLyricsFactory.looksLikeTtml("[00:10.00]Hello"))
    }

    // ---- TTML ---------------------------------------------------------------------

    /** Index of the entry whose lyric text is [text]; pause entries make fixed indexes fragile. */
    private fun YosLyrics.indexOfText(text: String) =
        entries.indexOfFirst { line -> YosLyrics.segmentsOf(line).joinToString("") { it.second } == text }

    private val appleStyle = """
        <tt xmlns="http://www.w3.org/ns/ttml" xmlns:itunes="http://music.apple.com/lyric-ttml-internal"
            xmlns:ttm="http://www.w3.org/ns/ttml#metadata" itunes:timing="Word" xml:lang="ja">
          <head><metadata>
            <ttm:agent type="person" xml:id="v1"/>
            <ttm:agent type="person" xml:id="v2"/>
            <iTunesMetadata xmlns="http://music.apple.com/lyric-ttml-internal">
              <translations>
                <translation type="subtitle" xml:lang="fr-FR"><text for="L1">Bonjour le monde</text></translation>
                <translation type="subtitle" xml:lang="en-US"><text for="L1">Hello world</text></translation>
              </translations>
              <songwriters><songwriter>Writer One</songwriter><songwriter>Writer Two</songwriter></songwriters>
              <transliterations><transliteration xml:lang="ja-Latn">
                <text for="L1">
                  <span begin="12.000" end="12.500" xmlns="http://www.w3.org/ns/ttml">kon</span>
                  <span begin="12.500" end="13.000" xmlns="http://www.w3.org/ns/ttml">nichi</span>
                  <span begin="14.000" end="14.500" xmlns="http://www.w3.org/ns/ttml">wa</span>
                </text>
              </transliteration></transliterations>
            </iTunesMetadata>
          </metadata></head>
          <body dur="1:00.000"><div begin="12.000" end="40.000">
            <p begin="12.000" end="14.500" ttm:agent="v1" itunes:key="L1"><span begin="12.000" end="12.500">こん</span><span begin="12.500" end="13.000">にち</span><span begin="14.000" end="14.500">は</span></p>
            <p begin="20.000" end="22.000" ttm:agent="v2" itunes:key="L2"><span begin="20.000" end="21.000">second</span> <span begin="21.000" end="22.000">voice</span></p>
          </div></body>
        </tt>
    """.trimIndent()

    @Test
    fun ttmlWordTimesAndPausesAreKeptExactly() {
        val lyrics = YosTtmlFactory("en").parse(appleStyle)
        assertEquals(YosLyricFormat.TTML, lyrics.format)

        // a lead-in blank comes first because the song starts with a long silence
        assertEquals(listOf(0f to "", 0f to "", 0f to ""), lyrics.entries[0])
        val first = lyrics.entries[1]
        assertEquals(12000f to "", first.first())
        assertEquals(
            listOf(
                12500f to "こん",
                13000f to "にち",
                14000f to "", // the pause before the last word
                14500f to "は"
            ),
            YosLyrics.segmentsOf(first)
        )
    }

    @Test
    fun ttmlTranslationFollowsThePreferredLanguage() {
        assertEquals("Hello world", YosTtmlFactory("en").parse(appleStyle).entries[1].last().second)
        assertEquals("Bonjour le monde", YosTtmlFactory("fr").parse(appleStyle).entries[1].last().second)
        // no match: the first translation in the file
        assertEquals("Bonjour le monde", YosTtmlFactory("de").parse(appleStyle).entries[1].last().second)
        // a line without a translation has none
        val lyrics = YosTtmlFactory("en").parse(appleStyle)
        assertEquals("", lyrics.entries[lyrics.indexOfText("second voice")].last().second)
    }

    @Test
    fun ttmlTransliterationLinesUpWithTheSegments() {
        val lyrics = YosTtmlFactory("en").parse(appleStyle)
        val segments = YosLyrics.segmentsOf(lyrics.entries[1])
        val roman = lyrics.transliterations[1]

        assertNotNull(roman)
        assertEquals(segments.size, roman!!.size)
        assertEquals(listOf("kon", "nichi", "", "wa"), roman)
        assertNull(lyrics.transliterations[lyrics.indexOfText("second voice")])
    }

    @Test
    fun ttmlVoicesAlternateSides() {
        val lyrics = YosTtmlFactory("en").parse(appleStyle)
        assertEquals(false, lyrics.otherSide[lyrics.indexOfText("こんにちは")])
        assertEquals(true, lyrics.otherSide[lyrics.indexOfText("second voice")])
    }

    @Test
    fun ttmlLongSilenceBecomesABlankLine() {
        val lyrics = YosTtmlFactory("en").parse(appleStyle)
        val firstEnd = 14500f
        val blank = lyrics.entries.first { it.first().first == firstEnd }

        assertTrue(blank.all { it.second.isEmpty() })
    }

    @Test
    fun ttmlCreditsAreKeptAsWritten() {
        val credits = YosTtmlFactory("en").parse(appleStyle).credits
        assertEquals(
            listOf(YosLyricCredit("songwriters", listOf("Writer One", "Writer Two"))),
            credits
        )
    }

    @Test
    fun ttmlLineWithoutWordTimesStaysAPlainLine() {
        val ttml = """
            <tt xmlns="http://www.w3.org/ns/ttml"><body><div>
              <p begin="00:00:05.000" end="00:00:08.000">Just a line of text</p>
            </div></body></tt>
        """.trimIndent()
        val lyrics = YosLyricsFactory.parse(ttml)

        // the song starts with a silence, so a blank line comes first
        assertEquals(2, lyrics.entries.size)
        assertEquals(
            listOf(5000f to "", 5000f to "Just a line of text", 5000f to ""),
            lyrics.entries[1]
        )
    }

    @Test
    fun ttmlTimeFormats() {
        assertEquals(12345f, YosTtmlFactory.parseTime("12.345")!!, 0.01f)
        assertEquals(72500f, YosTtmlFactory.parseTime("1:12.500")!!, 0.01f)
        assertEquals(3_672_500f, YosTtmlFactory.parseTime("01:01:12.5")!!, 0.01f)
        assertEquals(1500f, YosTtmlFactory.parseTime("1.5s")!!, 0.01f)
        assertEquals(250f, YosTtmlFactory.parseTime("250ms")!!, 0.01f)
        assertNull(YosTtmlFactory.parseTime("10:00:00:12"))
        assertNull(YosTtmlFactory.parseTime("soon"))
        assertNull(YosTtmlFactory.parseTime(null))
    }

    @Test
    fun brokenTtmlGivesEmptyLyrics() {
        assertTrue(YosTtmlFactory().parse("<tt><body><p begin=\"1\"").isEmpty)
        assertTrue(YosTtmlFactory().parse("<html></html>").isEmpty)
    }
}
