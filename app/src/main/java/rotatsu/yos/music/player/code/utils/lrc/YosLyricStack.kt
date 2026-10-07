package rotatsu.yos.music.player.code.utils.lrc

/** How the lines around the sung one fade, blur, scroll and settle. Values follow BitChord (GPL-3.0). */
internal object YosLyricStack {

    const val SETTLE_MS = 400
    const val INACTIVE_SCALE = 0.98f
    const val UNSUNG_ALPHA = 0.5f
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

    private val ALPHA = floatArrayOf(1f, 0.8f, 0.7f, 0.58f, 0.46f)
    private val BLUR_DP = floatArrayOf(0f, 2.5f, 5f, 7.5f, 8f)

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

    /** Strength of the backdrop scrim for a cover of [lightness] 0..1, from BitChord's 0.34 to 0.64 and above. */
    fun scrimBoost(lightness: Float): Float = 1f + 0.45f * ((lightness - 0.55f) / 0.45f).coerceIn(0f, 1f)
}
