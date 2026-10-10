package rotatsu.yos.music.player.code.utils.lrc

/** The lyric looks to pick from. [id] is what the setting stores. */
internal enum class YosLyricStyle(val id: String) {
    BITCHORD("bitchord"),
    ARCHIVETUNE("archivetune");

    companion object {
        fun fromId(id: String): YosLyricStyle = values().firstOrNull { it.id == id } ?: BITCHORD
    }
}

/**
 * How one look fades, blurs, scrolls and glows. The tables are indexed by how many lines a line
 * sits from the sung one and stay on their last value after that.
 */
class YosLyricStyleSpec(
    private val alphaByDistance: FloatArray,
    private val browsingAlphaByDistance: FloatArray,
    private val blurByDistance: FloatArray,
    val inactiveScale: Float,
    /** Brightness of words that are not sung yet. */
    val unsungAlpha: Float,
    val alphaActiveMs: Int,
    val alphaInactiveMs: Int,
    val scaleActiveMs: Int,
    val scaleInactiveMs: Int,
    val blurMs: Int,
    /** Duration of the remaining fades, such as the translation. */
    val settleMs: Int,
    /** Control points of the cubic bezier easing: x1, y1, x2, y2. */
    val easingPoints: FloatArray,
    /** Where the sung line rests, as a share of the view height. */
    val anchor: Float,
    /** One fixed scroll time and lead, or null to follow the gap before each line. */
    val fixedScrollMs: Int?,
    /** Whether rows behind the focus trail the scroll a little. */
    val stagger: Boolean,
    val glowAlpha: Float,
    val glowRadiusDp: Float,
    /** How far a sung word rises, in pixels. */
    val liftPx: Float
) {
    fun alpha(distance: Int): Float = alphaByDistance[distance.coerceIn(0, alphaByDistance.lastIndex)]

    fun blurDp(distance: Int): Float = blurByDistance[distance.coerceIn(0, blurByDistance.lastIndex)]

    /** Brightness of a line [distance] lines from the sung one, with its own table while the list is read by hand. */
    fun lineAlpha(distance: Int, browsing: Boolean): Float = when {
        distance <= 0 -> 1f
        browsing -> browsingAlphaByDistance[distance.coerceIn(0, browsingAlphaByDistance.lastIndex)]
        else -> alpha(distance)
    }

    /** Scroll duration, and how far ahead of a line it starts, for a [gapMs] between two lines. */
    fun scrollLead(gapMs: Float): Int = fixedScrollMs ?: YosLyricStack.scrollLead(gapMs)
}

/**
 * The looks themselves. BitChord keeps the values the app always used (GPL-3.0). The ArchiveTune
 * look follows its Enhanced lyrics view (GPL-3.0, Copyright Rukamori), which draws its lines with
 * Accompanist Lyrics UI (Apache-2.0, Copyright 6xingyv): every line away from the sung one is dimmed
 * to the same 0.4 and blurred a little more per line, the sung line scales up over 600 ms, and
 * it rests 38% down the view.
 */
internal object YosLyricStyles {
    private const val ARCHIVETUNE_SCROLL_MS = 500

    private val bitChord = YosLyricStyleSpec(
        alphaByDistance = YosLyricStack.ALPHA,
        browsingAlphaByDistance = floatArrayOf(1f, YosLyricStack.BROWSING_ALPHA),
        blurByDistance = YosLyricStack.BLUR_DP,
        inactiveScale = YosLyricStack.INACTIVE_SCALE,
        unsungAlpha = YosLyricStack.UNSUNG_ALPHA,
        alphaActiveMs = YosLyricStack.SETTLE_MS,
        alphaInactiveMs = YosLyricStack.SETTLE_MS,
        scaleActiveMs = YosLyricStack.SETTLE_MS,
        scaleInactiveMs = YosLyricStack.SETTLE_MS,
        blurMs = YosLyricStack.SETTLE_MS,
        settleMs = YosLyricStack.SETTLE_MS,
        easingPoints = floatArrayOf(0.41f, 0f, 0.12f, 0.99f),
        anchor = 0.0618f,
        fixedScrollMs = null,
        stagger = true,
        glowAlpha = 0.62f,
        glowRadiusDp = 6f,
        liftPx = 4f
    )

    private val archiveTune = YosLyricStyleSpec(
        alphaByDistance = floatArrayOf(1f),
        browsingAlphaByDistance = floatArrayOf(1f),
        blurByDistance = floatArrayOf(0f, 1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f),
        inactiveScale = 0.98f,
        unsungAlpha = 0.4f,
        alphaActiveMs = 400,
        alphaInactiveMs = 400,
        scaleActiveMs = 600,
        scaleInactiveMs = 300,
        blurMs = 300,
        settleMs = 300,
        easingPoints = floatArrayOf(0.4f, 0f, 0.2f, 1f),
        anchor = 0.38f,
        fixedScrollMs = ARCHIVETUNE_SCROLL_MS,
        stagger = false,
        glowAlpha = 0.4f,
        glowRadiusDp = 4f,
        liftPx = 0f
    )

    fun of(style: YosLyricStyle): YosLyricStyleSpec = when (style) {
        YosLyricStyle.BITCHORD -> bitChord
        YosLyricStyle.ARCHIVETUNE -> archiveTune
    }

    /** The look stored under [id], BitChord when it is unknown. */
    fun of(id: String): YosLyricStyleSpec = of(YosLyricStyle.fromId(id))
}
