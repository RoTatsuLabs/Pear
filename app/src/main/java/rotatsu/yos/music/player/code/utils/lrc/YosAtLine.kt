package rotatsu.yos.music.player.code.utils.lrc

// The timing and effect rules of ArchiveTune's lyric view, which draws its lines with Accompanist
// Lyrics UI, https://github.com/6xingyv/accompanist-lyrics-ui (Apache License 2.0, Copyright 2025
// 6xingyv). The rules and the numbers are the library's, taken from its LyricsProfile, PreparedGroup,
// LatinProfile, CjkProfile, RowRenderState and effects files. This is a Kotlin rewrite on Pear's own
// line model, without its text preparation, rasters or phonetics, and only for text read left to right.

/** A curve through the given points, the way Accompanist builds its Swell and Bounce easings. */
internal class YosAtCurve(points: List<Pair<Double, Double>>) {
    private val xs = points.map { it.first }
    private val divided: List<Double>

    init {
        val n = points.size
        val table = Array(n) { DoubleArray(n) }
        for (i in 0 until n) table[i][0] = points[i].second
        for (j in 1 until n) {
            for (i in j until n) {
                table[i][j] = (table[i][j - 1] - table[i - 1][j - 1]) / (xs[i] - xs[i - j])
            }
        }
        divided = List(n) { table[it][it] }
    }

    fun transform(fraction: Float): Float {
        val x = fraction.toDouble()
        val n = xs.size - 1
        var result = divided[n]
        for (i in (n - 1) downTo 0) result = result * (x - xs[i]) + divided[i]
        return result.toFloat()
    }
}

internal object YosAtEffects {
    /** How bright the text is where the sweep has not been, and on a row that has not started. */
    const val UNLIT_ALPHA = 0.2f

    /** Where a word starts, as far below its line, and how long it takes to come up. */
    const val LIFT_PX = 4f
    const val LIFT_MS = 700f

    /** The glow is drawn in this many steps. */
    const val GLOW_STEPS = 64

    val bounce = YosAtCurve(listOf(0.0 to 0.0, 0.7 to 1.0, 1.0 to 0.0))

    fun swell(peak: Double) = YosAtCurve(listOf(0.0 to 0.0, 0.5 to peak, 1.0 to 0.0))

    /** How far below its resting place a unit that started [startMs] is drawn at [timeMs]. */
    fun lift(timeMs: Int, startMs: Float): Float {
        val progress = ((timeMs.toDouble() - startMs) / LIFT_MS).toFloat().coerceIn(0f, 1f)
        return LIFT_PX * (1f - progress) * (1f - progress)
    }

    /** The glow step of a unit that is [progress] of the way through its animation. */
    fun glowStep(progress: Float): Int =
        (bounce.transform(progress).coerceIn(0f, 1f) * GLOW_STEPS).toInt()
}

/** One character, drawn and timed on its own. [animStart] and [animEnd] are its syllable. */
internal class YosAtUnit(
    val char: Int,
    val start: Int,
    val end: Int,
    val animStart: Int,
    val animEnd: Int,
) {
    val sourceStart: Int get() = animStart
}

/** A word and the whitespace after it. Its characters move together. */
internal class YosAtGroup(val units: List<YosAtUnit>, val cjk: Boolean) {
    val start = units.minOf { it.start }
    val end = units.maxOf { it.end }
    val duration = (end.toLong() - start).toFloat()
    val sourceStart = units.minOf { it.sourceStart }

    /** Every character comes from one syllable. */
    val commonSource = units.all { it.animStart == units[0].animStart && it.animEnd == units[0].animEnd }

    val scale: Boolean
    val glow: Boolean
    val glowAsGroup: Boolean

    init {
        val span = (units.maxOf { it.end }).toLong() - units.minOf { it.start }
        if (cjk) {
            scale = false
            glow = span >= 1000
            glowAsGroup = true
        } else {
            val enabled = units.size > 1 && span >= 1000 && span / units.size > 200
            scale = enabled
            glow = enabled
            glowAsGroup = false
        }
    }

    /** Scaling or glowing, so it is drawn a unit at a time for as long as that lasts. */
    val awesome = scale || glow
    val animationDuration = if (awesome) (duration * 0.8f).coerceAtLeast(1f) else 700f
    private val intensity = ((duration - 200f * units.size) / 1000f).coerceIn(0f, 1f)
    val swell = YosAtEffects.swell(0.1 * intensity)

    /** The scale and glow of the units begin one after the other, spread over the word. */
    private val unitEffects = scale || (glow && !glowAsGroup)

    fun unitStart(index: Int): Float =
        if (unitEffects && units.size > 1) {
            start + (duration - animationDuration) * index / (units.size - 1)
        } else {
            units[index].sourceStart.toFloat()
        }

    val groupGlowEnd = if (glow && glowAsGroup) start + animationDuration else Float.NEGATIVE_INFINITY

    /** When nothing is moving any more and the word can be drawn as it rests. */
    val effectsEnd: Float = maxOf(
        start.toFloat(),
        groupGlowEnd,
        units.indices.maxOf { index ->
            maxOf(
                if (unitEffects) unitStart(index) + animationDuration else Float.NEGATIVE_INFINITY,
                units[index].animStart + YosAtEffects.LIFT_MS,
            )
        },
    )
}

internal class YosAtLine(val text: String, val groups: List<YosAtGroup>) {
    val isAnimated: Boolean get() = groups.isNotEmpty()
}

/**
 * Where the lit edge of a row is at a time. The row is a list of timed character boxes in order of
 * their start. The edge crosses a box over the time it is sung, and the first and last box own the
 * extra half of the fade at either end of the row.
 */
internal class YosAtSweep(
    private val starts: IntArray,
    private val ends: IntArray,
    private val lefts: FloatArray,
    private val rights: FloatArray,
    private val fadeWidth: Float,
    private val rowLeft: Float,
) {
    private var cursor = 0
    private var previousTime = Int.MIN_VALUE

    fun center(now: Int): Float {
        if (starts.isEmpty()) return rowLeft
        if (now < previousTime || now.toLong() - previousTime > 1000L) {
            var low = 0
            var high = starts.size
            while (low < high) {
                val middle = (low + high) ushr 1
                if (starts[middle] <= now) low = middle + 1 else high = middle
            }
            cursor = (low - 1).coerceAtLeast(0)
        }
        while (cursor < starts.lastIndex && now >= starts[cursor + 1]) cursor++
        previousTime = now
        val i = cursor
        val p = if (ends[i] <= starts[i]) {
            if (now >= starts[i]) 1f else 0f
        } else {
            ((now.toLong() - starts[i]).toFloat() / (ends[i].toLong() - starts[i]).toFloat())
                .coerceIn(0f, 1f)
        }
        val half = fadeWidth / 2f
        val from = lefts[i] - if (i == 0) half else 0f
        val to = rights[i] + if (i == starts.lastIndex) half else 0f
        return from + (to - from) * p
    }
}
