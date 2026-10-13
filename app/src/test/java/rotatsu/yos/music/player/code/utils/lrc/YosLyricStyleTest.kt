package rotatsu.yos.music.player.code.utils.lrc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YosLyricStyleTest {
    private val bitChord = YosLyricStyles.of(YosLyricStyle.BITCHORD)
    private val archiveTune = YosLyricStyles.of(YosLyricStyle.ARCHIVETUNE)

    @Test
    fun theStoredIdFindsItsLook() {
        assertEquals(YosLyricStyle.BITCHORD, YosLyricStyle.fromId("bitchord"))
        assertEquals(YosLyricStyle.ARCHIVETUNE, YosLyricStyle.fromId("archivetune"))
    }

    @Test
    fun anUnknownIdFallsBackToBitChord() {
        assertEquals(YosLyricStyle.BITCHORD, YosLyricStyle.fromId(""))
        assertEquals(YosLyricStyle.BITCHORD, YosLyricStyle.fromId("gone"))
        assertEquals(bitChord, YosLyricStyles.of("gone"))
    }

    @Test
    fun bitChordKeepsTheValuesTheAppAlwaysUsed() {
        for (d in -1..8) {
            assertEquals(YosLyricStack.alpha(d), bitChord.alpha(d))
            assertEquals(YosLyricStack.blurDp(d), bitChord.blurDp(d))
            assertEquals(YosLyricStack.lineAlpha(d, true), bitChord.lineAlpha(d, true))
            assertEquals(YosLyricStack.lineAlpha(d, false), bitChord.lineAlpha(d, false))
        }
        assertEquals(YosLyricStack.INACTIVE_SCALE, bitChord.inactiveScale)
        assertEquals(YosLyricStack.UNSUNG_ALPHA, bitChord.unsungAlpha)
        for (gap in listOf(0f, 420f, 9000f)) {
            assertEquals(YosLyricStack.scrollLead(gap), bitChord.scrollLead(gap))
        }
    }

    @Test
    fun everyLookFadesAndBlursFurtherFromTheSungLine() {
        for (spec in listOf(bitChord, archiveTune)) {
            assertEquals(1f, spec.lineAlpha(0, false))
            assertEquals(0f, spec.blurDp(0))
            for (d in 1..8) {
                assertTrue(spec.alpha(d) <= spec.alpha(d - 1))
                assertTrue(spec.blurDp(d) >= spec.blurDp(d - 1))
                assertTrue(spec.lineAlpha(d, true) <= spec.lineAlpha(d - 1, true))
            }
        }
    }

    @Test
    fun theTablesStayOnTheirLastValue() {
        for (spec in listOf(bitChord, archiveTune)) {
            assertEquals(spec.alpha(9), spec.alpha(60))
            assertEquals(spec.blurDp(9), spec.blurDp(60))
            assertEquals(spec.lineAlpha(9, true), spec.lineAlpha(60, true))
        }
    }

    @Test
    fun archiveTuneDimsEveryOtherLineTheSameAndBlursThemByDistance() {
        assertEquals(0.4f, archiveTune.unsungAlpha)
        for (d in 1..9) {
            assertEquals(1f, archiveTune.lineAlpha(d, false))
            assertEquals(1f, archiveTune.lineAlpha(d, true))
        }
        assertEquals(1f, archiveTune.blurDp(1))
        assertEquals(4f, archiveTune.blurDp(4))
        assertTrue(archiveTune.blurDp(5) > bitChord.blurDp(5))
    }

    @Test
    fun archiveTuneScalesTheSungLineUpSlowerThanItScalesDown() {
        assertEquals(0.98f, archiveTune.inactiveScale)
        assertEquals(600, archiveTune.scaleActiveMs)
        assertEquals(300, archiveTune.scaleInactiveMs)
        assertEquals(0.38f, archiveTune.anchor)
        assertEquals(0f, archiveTune.liftDp)
    }

    @Test
    fun archiveTuneScrollsForOneFixedTimeAndBitChordFollowsTheGap() {
        assertEquals(archiveTune.scrollLead(0f), archiveTune.scrollLead(9000f))
        assertEquals(350, bitChord.scrollLead(0f))
        assertEquals(500, bitChord.scrollLead(9000f))
    }

    @Test
    fun onlyBitChordTrailsTheRowsBehindTheScroll() {
        assertTrue(bitChord.stagger)
        assertFalse(archiveTune.stagger)
    }

    @Test
    fun theEasingPointsDescribeACubicBezier() {
        for (spec in listOf(bitChord, archiveTune)) {
            assertEquals(4, spec.easingPoints.size)
            assertTrue(spec.easingPoints[0] in 0f..1f && spec.easingPoints[2] in 0f..1f)
        }
    }

    @Test
    fun eachLookNamesTheRendererThatDrawsItsWordSyncedLines() {
        assertTrue(bitChord.bitChordWords && !bitChord.archiveTuneWords)
        assertTrue(archiveTune.archiveTuneWords && !archiveTune.bitChordWords)
        assertEquals(1f, archiveTune.scaleOriginY)
        assertEquals(0.5f, bitChord.scaleOriginY)
    }

    private val apple = YosLyricStyles.of(YosLyricStyle.APPLE)

    @Test
    fun theAppleLookIsFoundByItsStoredId() {
        assertEquals(YosLyricStyle.APPLE, YosLyricStyle.fromId("applemusic"))
    }

    @Test
    fun theAppleLookDimsEveryOtherLineTheSameAndNeverBlurs() {
        assertEquals(0.2f, apple.inactiveLineAlpha)
        assertEquals(0.2f, apple.unsungAlpha)
        for (d in 0..9) assertEquals(0f, apple.blurDp(d))
        assertTrue(apple.archiveTuneWords && apple.upcomingAsSung)
    }

    @Test
    fun theAppleLookFollowsEveryActiveLineAndASpring() {
        assertTrue(apple.multiActive && apple.scrollSpring && apple.pressHighlight)
        assertEquals(0.37f, apple.bottomFadeFrom)
        assertEquals(1f, YosLyricStyles.APPLE_SPRING_DAMPING)
        assertTrue(!bitChord.multiActive && !archiveTune.multiActive)
        assertEquals(1f, bitChord.bottomFadeFrom)
    }

    @Test
    fun archiveTuneMovesTheSungLineUpWhileTheControlsShow() {
        assertEquals(0.0618f, archiveTune.anchorFor(true))
        assertEquals(0.38f, archiveTune.anchorFor(false))
        // The others rest in one place either way.
        assertEquals(bitChord.anchor, bitChord.anchorFor(true))
        assertEquals(bitChord.anchor, bitChord.anchorFor(false))
        assertEquals(apple.anchor, apple.anchorFor(true))
        assertEquals(apple.anchor, apple.anchorFor(false))
    }
}
