package yos.music.player.code.utils.lrc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YosLyricStackTest {

    @Test
    fun theSungLineIsSharpAndFullyLit() {
        assertEquals(1f, YosLyricStack.alpha(0))
        assertEquals(0f, YosLyricStack.blurDp(0))
    }

    @Test
    fun linesFurtherAwayGetDimmerAndBlurrier() {
        for (d in 1..4) {
            assertTrue(YosLyricStack.alpha(d) < YosLyricStack.alpha(d - 1))
            assertTrue(YosLyricStack.blurDp(d) >= YosLyricStack.blurDp(d - 1))
        }
    }

    @Test
    fun theLastStepHoldsForEverythingFurtherOut() {
        assertEquals(YosLyricStack.alpha(4), YosLyricStack.alpha(40))
        assertEquals(YosLyricStack.blurDp(4), YosLyricStack.blurDp(40))
        assertEquals(YosLyricStack.alpha(0), YosLyricStack.alpha(-3))
    }

    @Test
    fun scrollLeadStaysInItsRange() {
        assertEquals(350, YosLyricStack.scrollLead(0f))
        assertEquals(420, YosLyricStack.scrollLead(420f))
        assertEquals(500, YosLyricStack.scrollLead(9000f))
    }

    @Test
    fun lineEndIsTheLatestTextTime() {
        val line = listOf(1000f to "", 1400f to "la ", 2100f to "la", 1000f to "")
        assertEquals(2100f, YosLyricStack.lineEnd(line))
        assertEquals(1000f, YosLyricStack.lineEnd(listOf(1000f to "", 1000f to "")))
        assertEquals(0f, YosLyricStack.lineEnd(emptyList()))
    }

    @Test
    fun focusIndexFollowsTime() {
        val starts = listOf(1000f, 2000f, 3000f)
        assertEquals(-1, YosLyricStack.focusIndex(starts, 500f))
        assertEquals(0, YosLyricStack.focusIndex(starts, 1000f))
        assertEquals(1, YosLyricStack.focusIndex(starts, 2999f))
        assertEquals(2, YosLyricStack.focusIndex(starts, 90000f))
    }

    @Test
    fun staggerDelaysRowsBehindTheFocusAndSettlesToZero() {
        val linear = { x: Float -> x }
        assertEquals(0f, YosLyricStack.stagger(100f, 100f, 400, 0, linear))
        assertEquals(0f, YosLyricStack.stagger(100f, 100f, 400, -2, linear))
        val one = YosLyricStack.stagger(100f, 100f, 400, 1, linear)
        val three = YosLyricStack.stagger(100f, 100f, 400, 3, linear)
        assertTrue(one > 0f && three > one)
        assertEquals(three, YosLyricStack.stagger(100f, 100f, 400, 9, linear))
        assertEquals(0f, YosLyricStack.stagger(100f, 1000f, 400, 3, linear))
        assertEquals(0f, YosLyricStack.stagger(100f, 0f, 400, 3, linear))
    }

    @Test
    fun browsingFlattensEveryLineButTheSungOne() {
        assertEquals(1f, YosLyricStack.lineAlpha(0, true))
        assertEquals(0.8f, YosLyricStack.lineAlpha(1, true))
        assertEquals(0.8f, YosLyricStack.lineAlpha(4, true))
        assertEquals(YosLyricStack.alpha(2), YosLyricStack.lineAlpha(2, false))
    }

    @Test
    fun gapDotsLightLeftToRight() {
        assertEquals(0.25f, YosLyricStack.gapDotAlpha(0f, 0))
        assertEquals(1f, YosLyricStack.gapDotAlpha(1f, 2))
        assertEquals(1f, YosLyricStack.gapDotAlpha(0.34f, 0))
        assertTrue(YosLyricStack.gapDotAlpha(0.34f, 1) < 0.3f)
        for (dot in 0..1) {
            assertTrue(YosLyricStack.gapDotAlpha(0.5f, dot) >= YosLyricStack.gapDotAlpha(0.5f, dot + 1))
        }
    }

    @Test
    fun gapBeforeSurvivesIndexesOutsideTheList() {
        val lines = listOf(
            listOf(1000f to "", 1500f to "a", 1000f to ""),
            listOf(2000f to "", 2600f to "b", 2000f to "")
        )
        assertEquals(500f, YosLyricStack.gapBefore(lines, 1))
        assertEquals(500f, YosLyricStack.gapBefore(lines, 0))
        assertEquals(500f, YosLyricStack.gapBefore(lines, -1))
        assertEquals(500f, YosLyricStack.gapBefore(lines, 2))
        assertEquals(500f, YosLyricStack.gapBefore(lines, 40))
        assertEquals(500f, YosLyricStack.gapBefore(listOf(emptyList(), emptyList()), 1))
        assertEquals(1f, YosLyricStack.gapBefore(listOf(listOf(0f to "", 1999f to "a", 0f to ""), listOf(2000f to "", 2100f to "b", 2000f to "")), 1))
    }

    @Test
    fun brightCoversGetAStrongerScrim() {
        assertEquals(1f, YosLyricStack.scrimBoost(0.2f))
        assertEquals(1f, YosLyricStack.scrimBoost(0.55f))
        assertTrue(YosLyricStack.scrimBoost(0.8f) in 1.01f..1.45f)
        assertEquals(1.45f, YosLyricStack.scrimBoost(1f))
    }
}
