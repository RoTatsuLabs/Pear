package rotatsu.yos.music.player.code.utils.lrc

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YosTextShapingTest {

    @Test
    fun joiningAndMarkScriptsNeedOneShapedLayout() {
        assertTrue(YosTextShaping.needsShapedDraw("حبيب سنيني"))
        assertTrue(YosTextShaping.needsShapedDraw("שלום"))
        assertTrue(YosTextShaping.needsShapedDraw("สวัสดี"))
        assertTrue(YosTextShaping.needsShapedDraw("नमस्ते"))
    }

    @Test
    fun aMarkAfterALatinLetterCounts() {
        assertTrue(YosTextShaping.needsShapedDraw("e\u0301"))
    }

    @Test
    fun scriptsThatDrawOneCharacterAtATimeStayOnTheOldPath() {
        assertFalse(YosTextShaping.needsShapedDraw("Hello world"))
        assertFalse(YosTextShaping.needsShapedDraw("你好世界"))
        assertFalse(YosTextShaping.needsShapedDraw("こんにちは"))
        assertFalse(YosTextShaping.needsShapedDraw("привет"))
        assertFalse(YosTextShaping.needsShapedDraw(""))
    }

    @Test
    fun theFirstLetterWithADirectionDecidesTheLine() {
        assertTrue(YosTextShaping.isRtl("يا ليل يا ليلي"))
        assertTrue(YosTextShaping.isRtl("123 שלום"))
        assertTrue(YosTextShaping.isRtl("- حبيب hello"))
        assertFalse(YosTextShaping.isRtl("hello حبيب"))
        assertFalse(YosTextShaping.isRtl("你好"))
        assertFalse(YosTextShaping.isRtl("123 ..."))
        assertFalse(YosTextShaping.isRtl(""))
    }
}
