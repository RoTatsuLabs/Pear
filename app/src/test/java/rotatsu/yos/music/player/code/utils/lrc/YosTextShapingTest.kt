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
}
