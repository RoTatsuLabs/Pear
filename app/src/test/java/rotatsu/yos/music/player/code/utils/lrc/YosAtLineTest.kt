package rotatsu.yos.music.player.code.utils.lrc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YosAtLineTest {

    @Test
    fun theBounceCurvePeaksAtSeventyPercentAndSwellAtHalf() {
        assertEquals(0f, YosAtEffects.bounce.transform(0f), 0.0001f)
        assertEquals(1f, YosAtEffects.bounce.transform(0.7f), 0.0001f)
        assertEquals(0f, YosAtEffects.bounce.transform(1f), 0.0001f)
        assertEquals(0.1f, YosAtEffects.swell(0.1).transform(0.5f), 0.0001f)
        assertEquals(64, YosAtEffects.glowStep(0.7f))
        assertEquals(0, YosAtEffects.glowStep(1f))
    }

    @Test
    fun aSyllableStartsFourPixelsLowAndSettlesOverSevenHundredMilliseconds() {
        assertEquals(4f, YosAtEffects.lift(-500, 0f), 0.0001f)
        assertEquals(4f, YosAtEffects.lift(0, 0f), 0.0001f)
        assertEquals(1f, YosAtEffects.lift(350, 0f), 0.0001f)
        assertEquals(0f, YosAtEffects.lift(700, 0f), 0.0001f)
        assertEquals(0f, YosAtEffects.lift(5000, 0f), 0.0001f)
    }

    @Test
    fun aLongSlowLatinWordSwellsAndGlowsLetterByLetter() {
        val line = YosAtLines.fromPairs(listOf(0f to "", 1200f to "love"))
        val group = line.groups.single()
        assertTrue(group.scale && group.glow && !group.glowAsGroup)
        assertEquals(960f, group.animationDuration, 0.001f)
        assertEquals(0f, group.unitStart(0), 0.001f)
        assertEquals(240f, group.unitStart(3), 0.001f)
        assertEquals(1200f, group.effectsEnd, 0.001f)
    }

    @Test
    fun aShortWordOnlyLifts() {
        val group = YosAtLines.fromPairs(listOf(0f to "", 400f to "hi")).groups.single()
        assertFalse(group.scale || group.glow)
        assertEquals(700f, group.animationDuration, 0.001f)
        assertTrue(group.commonSource)
        assertEquals(700f, group.effectsEnd, 0.001f)
    }

    @Test
    fun aHeldWordNeedsMoreThanTwoHundredMillisecondsPerLetter() {
        val group = YosAtLines.fromPairs(listOf(0f to "", 1000f to "abcdefg")).groups.single()
        assertFalse(group.scale || group.glow)
    }

    @Test
    fun aBlockScriptGroupGlowsAsOneAndNeverScales() {
        val groups = YosAtLines.fromPairs(listOf(0f to "", 1500f to "愛し", 3000f to "て")).groups
        assertEquals(2, groups.size)
        assertTrue(groups[0].cjk && groups[0].glow && groups[0].glowAsGroup && !groups[0].scale)
        assertEquals(1200f, groups[0].groupGlowEnd, 0.001f)
    }

    @Test
    fun aGroupIsAWordAndTheWhitespaceAfterIt() {
        val line = YosAtLines.fromPairs(listOf(0f to "", 300f to "Hel", 500f to "lo ", 800f to "world"))
        assertEquals("Hello world", line.text)
        assertEquals(2, line.groups.size)
        assertEquals(6, line.groups[0].units.size)
        assertEquals(5, line.groups[1].units.size)
        assertFalse(line.groups[0].commonSource)
        assertEquals(0, line.groups[0].units[0].animStart)
        assertEquals(300, line.groups[0].units[3].animStart)
    }

    @Test
    fun theSweepEdgeCrossesEachCharacterBoxAndOwnsTheEndsOfTheFade() {
        val sweep = YosAtSweep(
            starts = intArrayOf(0, 500), ends = intArrayOf(500, 1000),
            lefts = floatArrayOf(0f, 50f), rights = floatArrayOf(50f, 100f),
            fadeWidth = 20f, rowLeft = 0f,
        )
        assertEquals(-10f, sweep.center(0), 0.001f)
        assertEquals(20f, sweep.center(250), 0.001f)
        assertEquals(50f, sweep.center(500), 0.001f)
        assertEquals(110f, sweep.center(1000), 0.001f)
        assertEquals(110f, sweep.center(5000), 0.001f)
    }
}
