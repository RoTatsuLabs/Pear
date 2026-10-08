package rotatsu.yos.music.player.code.utils.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CrossfadePlanTest {

    @Test
    fun theFadeStartsOneFadeLengthBeforeTheEnd() {
        val plan = CrossfadePlan.plan(durationMs = 200_000, positionMs = 10_000, fadeMs = 4_000)

        assertNotNull(plan)
        assertEquals(196_000, plan!!.fireAtMs)
        assertEquals(196_000 - CrossfadePlan.PREPARE_LEAD_MS, plan.prepareAtMs)
    }

    @Test
    fun aSongTooShortForTheFadeIsLeftAlone() {
        assertNull(CrossfadePlan.plan(durationMs = 9_000, positionMs = 0, fadeMs = 4_000))
        assertNull(CrossfadePlan.plan(durationMs = 0, positionMs = 0, fadeMs = 4_000))
    }

    @Test
    fun aSeekCloseToTheEndSkipsTheFade() {
        assertNull(CrossfadePlan.plan(durationMs = 200_000, positionMs = 194_000, fadeMs = 4_000))
    }

    @Test
    fun theVolumesCrossAtEqualPower() {
        assertEquals(0f, CrossfadePlan.gainIn(0f), 0.001f)
        assertEquals(1f, CrossfadePlan.gainOut(0f), 0.001f)
        assertEquals(1f, CrossfadePlan.gainIn(1f), 0.001f)
        assertEquals(0f, CrossfadePlan.gainOut(1f), 0.001f)
        for (step in 0..10) {
            val t = step / 10f
            val power = CrossfadePlan.gainIn(t) * CrossfadePlan.gainIn(t) +
                    CrossfadePlan.gainOut(t) * CrossfadePlan.gainOut(t)
            assertEquals(1f, power, 0.001f)
        }
    }
}
