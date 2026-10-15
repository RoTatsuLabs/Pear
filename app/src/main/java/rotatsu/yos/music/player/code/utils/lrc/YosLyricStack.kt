package rotatsu.yos.music.player.code.utils.lrc

import kotlin.math.pow

/** How the lines around the sung one fade, blur, scroll and settle. Values follow BitChord (GPL-3.0). */
internal object YosLyricStack {

    const val SETTLE_MS = 400
    const val INACTIVE_SCALE = 0.98f
    const val UNSUNG_ALPHA = 0.45f
    const val BROWSING_ALPHA = 0.8f
    const val PRESSED_SCALE = 0.96f
    const val PRESS_MS = 120
    const val LEAD_MIN_MS = 350
    const val LEAD_MAX_MS = 500
    const val STAGGER_STEPS = 3
    const val STAGGER_FRACTION = 0.06f
    const val GAP_DOTS = 3
    const val GAP_DOT_REST = 0.25f
    const val GAP_REST_SCALE = 0.76f

    internal val ALPHA = floatArrayOf(1f, 0.8f, 0.7f, 0.58f, 0.46f)
    internal val BLUR_DP = floatArrayOf(0f, 1f, 1f, 1.7f, 2.4f)

    fun alpha(distance: Int): Float = ALPHA[distance.coerceIn(0, ALPHA.lastIndex)]

    fun blurDp(distance: Int): Float = BLUR_DP[distance.coerceIn(0, BLUR_DP.lastIndex)]

    /** Brightness of a line [distance] lines from the sung one, flat while the list is read by hand. */
    fun lineAlpha(distance: Int, browsing: Boolean): Float =
        if (distance <= 0) 1f else if (browsing) BROWSING_ALPHA else alpha(distance)

    /** Scroll duration, and how far ahead of a line it starts, for a [gapMs] between two lines. */
    fun scrollLead(gapMs: Float): Int = gapMs.coerceIn(LEAD_MIN_MS.toFloat(), LEAD_MAX_MS.toFloat()).toInt()

    /** The latest time of a line's text, or its start when it has none. */
    fun lineEnd(entry: List<Pair<Float, String>>): Float {
        val start = entry.firstOrNull()?.first ?: return 0f
        return entry.dropLast(1).filter { it.second.isNotBlank() }.maxOfOrNull { it.first } ?: start
    }

    /**
     * The first and last line being sung at [time]: started, and not past their own end. Lines may
     * overlap, as echo lines do, so more than one can be active. Null when none is.
     */
    fun activeRange(starts: List<Float>, ends: List<Float>, time: Float): IntRange? {
        var first = -1
        var last = -1
        for (i in starts.indices) {
            if (starts[i] <= time && time <= ends.getOrElse(i) { starts[i] }) {
                if (first < 0) first = i
                last = i
            }
        }
        return if (first < 0) null else first..last
    }

    /** Index of the last line that has started at [time], or -1 before the first one. */
    fun focusIndex(starts: List<Float>, time: Float): Int = starts.indexOfLast { it <= time }

    /** Gap before line [index], or the longest lead when the line or its predecessor is missing. */
    fun gapBefore(entries: List<List<Pair<Float, String>>>, index: Int): Float {
        val line = entries.getOrNull(index)
        val previous = entries.getOrNull(index - 1)
        if (line == null || previous == null || line.isEmpty()) return LEAD_MAX_MS.toFloat()
        return line.first().first - lineEnd(previous)
    }

    /** Extra offset of a row [behind] the focus during a scroll of [delta] pixels. [ease] maps 0..1 to 0..1. */
    fun stagger(delta: Float, elapsedMs: Float, durationMs: Int, behind: Int, ease: (Float) -> Float): Float {
        if (behind <= 0 || delta == 0f || durationMs <= 0) return 0f
        val delay = behind.coerceAtMost(STAGGER_STEPS) * STAGGER_FRACTION * durationMs
        val own = ease(((elapsedMs - delay) / durationMs).coerceIn(0f, 1f))
        val lead = ease((elapsedMs / durationMs).coerceIn(0f, 1f))
        return delta * (lead - own)
    }

    /** Brightness of dot [dot] of a break that is [progress] of the way through. */
    fun gapDotAlpha(progress: Float, dot: Int): Float {
        val lit = (progress * GAP_DOTS - dot).coerceIn(0f, 1f)
        return GAP_DOT_REST + (1f - GAP_DOT_REST) * lit
    }

    /**
     * The spring AMLL scrolls its lyrics with: how stiff, and how far it is damped as a share of critical. In play it
     * follows the gap since the line before, from 220 for a quick succession down to 170, and is a little past
     * critical so it never overshoots. With no gap to go by, in a pause or when the position jumped, it is slower.
     */
    fun amllSpring(intervalMs: Float?, slow: Boolean = false): Pair<Float, Float> {
        if (slow || intervalMs == null) return AMLL_SLOW_STIFFNESS to AMLL_SLOW_DAMPING / (2f * kotlin.math.sqrt(AMLL_SLOW_STIFFNESS))
        val clamped = intervalMs.coerceIn(100f, 800f)
        val ratio = (1f - (clamped - 100f) / 700f).toDouble().pow(0.2).toFloat()
        val stiffness = 170f + ratio * 50f
        // AMLL's damping is the square root of the stiffness times 2.2, with a mass of 1.
        return stiffness to 1.1f
    }

    private const val AMLL_SLOW_STIFFNESS = 90f
    private const val AMLL_SLOW_DAMPING = 15f

    /** Strength of the backdrop scrim for a cover of [lightness] 0..1, from BitChord's 0.34 to 0.64 and above. */
    fun scrimBoost(lightness: Float): Float = 1f + 0.45f * ((lightness - 0.55f) / 0.45f).coerceIn(0f, 1f)
}
