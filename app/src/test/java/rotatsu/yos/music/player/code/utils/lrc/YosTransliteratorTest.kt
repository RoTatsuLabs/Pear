package rotatsu.yos.music.player.code.utils.lrc

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class YosTransliteratorTest {

    private val originalHan = YosTransliterator.hanReader
    private val originalIcu = YosTransliterator.icuAvailable

    @Before
    fun useTheFallback() {
        YosTransliterator.icuAvailable = { false }
        // a tiny stand-in for the pinyin dictionary
        val pinyin = mapOf('我' to "wo", '们' to "men", '你' to "ni", '好' to "hao", '牌' to "pai", '玉' to "yu")
        YosTransliterator.hanReader = { pinyin[it] }
        YosTransliterator.clearCache()
    }

    @After
    fun restore() {
        YosTransliterator.hanReader = originalHan
        YosTransliterator.icuAvailable = originalIcu
        YosTransliterator.clearCache()
    }

    @Test
    fun latinTextIsLeftAlone() {
        assertNull(YosTransliterator.transliterate("Hello world, it's 3 o'clock"))
        assertNull(YosTransliterator.transliterate("Café déjà vu"))
        assertNull(YosTransliterator.transliterate("   "))
    }

    @Test
    fun hangulFollowsRevisedRomanization() {
        assertEquals("annyeonghaseyo", YosTransliterator.transliterate("안녕하세요"))
        assertEquals("saranghae", YosTransliterator.transliterate("사랑해"))
        assertEquals("hangugeo", YosTransliterator.transliterate("한국어"))
        assertEquals("sarangeul", YosTransliterator.transliterate("사랑을"))
        assertEquals("isseo", YosTransliterator.transliterate("있어"))
        assertEquals("eumak", YosTransliterator.transliterate("음악"))
        assertEquals("babeul", YosTransliterator.transliterate("밥을"))
        assertEquals("dalgi", YosTransliterator.transliterate("닭이")) // double final: the l stays
    }

    @Test
    fun kanaFollowsHepburn() {
        assertEquals("konnichiha", YosTransliterator.transliterate("こんにちは"))
        assertEquals("arigatou", YosTransliterator.transliterate("ありがとう"))
        assertEquals("kyou", YosTransliterator.transliterate("きょう"))
        assertEquals("gakkou", YosTransliterator.transliterate("がっこう"))
        assertEquals("sakura", YosTransliterator.transliterate("サクラ"))
        // the long vowel mark repeats the vowel before it
        assertEquals("koohii", YosTransliterator.transliterate("コーヒー"))
        assertEquals("fan", YosTransliterator.transliterate("ファン"))
    }

    @Test
    fun cyrillicAndGreek() {
        assertEquals("Privet mir", YosTransliterator.transliterate("Привет мир"))
        assertEquals("kalimera", YosTransliterator.transliterate("καλημέρα"))
    }

    @Test
    fun hanIsReadAsMandarinOneCharacterAtATime() {
        assertEquals("ni hao", YosTransliterator.transliterate("你好"))
        assertEquals("wo men", YosTransliterator.transliterate("我们"))
        assertEquals("ni hao world", YosTransliterator.transliterate("你好world"))
    }

    @Test
    fun hanWithoutAReadingIsLeftAlone() {
        assertNull(YosTransliterator.transliterate("你龘"))
    }

    @Test
    fun japaneseLinesWithKanjiAreNotReadAsChinese() {
        assertNull(YosTransliterator.transliterate("私は牌", "ja"))
        assertNull(YosTransliterator.transliterate("玉の牌"))
        // the same kanji in a Chinese line are read
        assertEquals("yu pai", YosTransliterator.transliterate("玉牌", "zh-Hans"))
    }

    @Test
    fun scriptsWithoutATableAreSkippedBelowApi29() {
        assertNull(YosTransliterator.transliterate("สวัสดี"))
        assertNull(YosTransliterator.transliterate("مرحبا"))
    }

    @Test
    fun fillOnlyAddsWhatTheSourceLacks() {
        val lyrics = YosLyrics(
            format = YosLyricFormat.LRC,
            entries = listOf(
                listOf(1000f to "", 1500f to "你", 2000f to "好", 1000f to ""),
                listOf(3000f to "", 3000f to "Plain English", 3000f to ""),
                listOf(5000f to "", 5000f to "안녕", 5000f to "")
            ),
            otherSide = listOf(false, false, false),
            transliterations = listOf(null, null, listOf("source gave this")),
            credits = emptyList()
        )

        val filled = YosTransliterator.fill(lyrics)

        assertEquals(listOf("ni", "hao"), filled.transliterations[0])
        assertNull(filled.transliterations[1])
        assertEquals(listOf("source gave this"), filled.transliterations[2])
        // the text and timing are not touched
        assertEquals(lyrics.entries, filled.entries)
    }

    @Test
    fun fillKeepsPausesAlignedWithTheirSlots() {
        val line = listOf(1000f to "", 1500f to "你", 2500f to "", 3000f to "好", 1000f to "")
        val lyrics = YosLyrics(YosLyricFormat.TTML, listOf(line), listOf(false), listOf(null), emptyList())

        val roman = YosTransliterator.fill(lyrics).transliterations.single()

        assertNotNull(roman)
        assertEquals(listOf("ni", "", "hao"), roman)
    }

    @Test
    fun resultsAreCached() {
        var reads = 0
        YosTransliterator.hanReader = { reads++; if (it == '你') "ni" else null }
        YosTransliterator.clearCache()

        YosTransliterator.transliterate("你")
        YosTransliterator.transliterate("你")

        assertEquals(1, reads)
    }

    @Test
    fun parsingDoesNotTransliterateOnItsOwn() {
        val lyrics = YosLyricsFactory.parse("[00:10.00]안녕\n")
        assertNull(lyrics.transliterations.single())
        assertNotNull(YosTransliterator.fill(lyrics).transliterations.single())
    }
}
