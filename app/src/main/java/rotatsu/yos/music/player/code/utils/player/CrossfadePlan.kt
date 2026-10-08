package rotatsu.yos.music.player.code.utils.player

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** The timing and volume curves of a crossfade, kept apart from the player so they can be tested. */
object CrossfadePlan {
    /** How long before the fade the second player starts loading the tail of the song. */
    const val PREPARE_LEAD_MS = 3000L

    private const val LATE_SLACK_MS = 500L

    class Plan(val prepareAtMs: Long, val fireAtMs: Long)

    /**
     * Returns when to prepare and when to start a fade of [fadeMs] for a song of [durationMs]
     * that is at [positionMs], or null when the song is too short or already too close to its end.
     */
    fun plan(durationMs: Long, positionMs: Long, fadeMs: Long): Plan? {
        if (durationMs <= 0L || fadeMs <= 0L) return null
        if (durationMs < 2 * fadeMs + PREPARE_LEAD_MS) return null
        val fireAt = durationMs - fadeMs
        val prepareAt = fireAt - PREPARE_LEAD_MS
        if (positionMs > prepareAt - LATE_SLACK_MS) return null
        return Plan(prepareAt, fireAt)
    }

    /** Volume of the song coming in after [progress] of the fade, 0 to 1. Equal power with [gainOut]. */
    fun gainIn(progress: Float): Float = sin(progress.coerceIn(0f, 1f) * PI.toFloat() / 2f)

    /** Volume of the song going out after [progress] of the fade, 1 to 0. */
    fun gainOut(progress: Float): Float = cos(progress.coerceIn(0f, 1f) * PI.toFloat() / 2f)
}
