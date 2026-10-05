package yos.music.player.code.utils.lrc

/** One sung segment of a line with the time span the lyric data gives it. */
internal class YosWordTiming(
    /** Index of the segment's pair in the line, so per segment data can be looked up. */
    val pairIndex: Int,
    val text: String,
    val start: Float,
    val end: Float
) {
    /** When character [index] of [text] starts, spreading the segment evenly over its characters. */
    fun charStart(index: Int): Float = start + (end - start) * index / text.length

    /** When character [index] of [text] ends. */
    fun charEnd(index: Int): Float = start + (end - start) * (index + 1) / text.length
}

/**
 * Timing helpers of the lyric view. They are plain functions so they can be tested without
 * a screen.
 */
internal object YosLyricTiming {

    /**
     * Turns the pairs of a line into timed segments. A segment starts when the previous one
     * ended, and a pair without text only moves that clock forward, so a pause inside the
     * line is a gap and not part of the next segment. Times never run backwards.
     *
     * @param mainLyric the line without its translation pair, first pair is the line start
     */
    fun timeline(mainLyric: List<Pair<Float, String>>): List<YosWordTiming> {
        if (mainLyric.isEmpty()) return emptyList()
        val result = ArrayList<YosWordTiming>(mainLyric.size)
        var clock = mainLyric.first().first
        mainLyric.forEachIndexed { index, pair ->
            if (index == 0) return@forEachIndexed
            if (pair.second.isEmpty()) {
                clock = maxOf(clock, pair.first)
                return@forEachIndexed
            }
            val end = maxOf(pair.first, clock)
            result += YosWordTiming(index, pair.second, clock, end)
            clock = end
        }
        return result
    }

    /** How far [time] is through a span, from 0 to 1. A span without length is 1 once reached. */
    fun progress(time: Float, start: Float, end: Float): Float {
        if (end <= start) return if (time >= start) 1f else 0f
        return ((time - start) / (end - start)).coerceIn(0f, 1f)
    }

    /**
     * The same as [progress] without the limits, so the caller can tell how long ago a span
     * ended. Below 0 the span has not started, above 1 it is over.
     */
    fun rawProgress(time: Float, start: Float, end: Float): Float {
        if (end <= start) return if (time >= start) 1f else -1f
        return (time - start) / (end - start)
    }
}
