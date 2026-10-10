package rotatsu.yos.music.player.code.utils.lrc

/** Builds the lines BitChord's renderer draws from the pairs of a Pear lyric line. */
internal object YosBcLines {

    /**
     * Turns [mainLyric], the pairs of one line without its translation, into a line of whole
     * words. A word is a run of characters without whitespace. Where a word is spread over
     * several timed segments, as the syllables of a TTML word are, each segment keeps its own
     * timing as a syllable of that word. Scripts that draw as blocks, such as Han, kana and
     * Hangul, have no spaces to split on, so every segment is a word of its own there.
     *
     * Inside a segment the time is spread evenly over its characters, the same as the rest of
     * Pear's timing does it, see [YosWordTiming.charStart].
     */
    fun fromPairs(mainLyric: List<Pair<Float, String>>): YosBcLine {
        val lineStart = mainLyric.firstOrNull()?.first ?: 0f
        val segments = ArrayList<Segment>()
        // Text on the first pair has no time of its own, so it is sung the moment the line starts.
        mainLyric.firstOrNull()?.second?.takeIf { it.isNotEmpty() }?.let {
            segments += Segment(it, lineStart, lineStart)
        }
        for (timing in YosLyricTiming.timeline(mainLyric)) {
            segments += Segment(timing.text, timing.start, timing.end)
        }
        val text = segments.joinToString("") { it.text }
        if (segments.isEmpty() || text.isBlank()) {
            return YosBcLine(timeMs = lineStart.toLong(), text = text)
        }

        // The segment every character belongs to, and where in that segment it sits.
        val owner = IntArray(text.length)
        val within = IntArray(text.length)
        var at = 0
        segments.forEachIndexed { index, segment ->
            for (i in segment.text.indices) {
                owner[at] = index
                within[at] = i
                at++
            }
        }

        val words = ArrayList<YosBcWord>()
        var start = 0
        while (start < text.length) {
            if (text[start].isWhitespace()) {
                start++
                continue
            }
            var end = start
            while (end < text.length && !text[end].isWhitespace()) end++
            val blocks = (start until end).any { text[it].isBlockScript() }
            // A run is cut into the pieces its segments make; block scripts are cut at every one.
            var from = start
            while (from < end) {
                var to = from + 1
                if (blocks) {
                    while (to < end && owner[to] == owner[from]) to++
                } else {
                    to = end
                }
                words += wordOf(text, segments, owner, within, from, to)
                from = to
            }
            start = end
        }
        return YosBcLine(timeMs = lineStart.toLong(), text = text, words = words)
    }

    private fun wordOf(
        text: String,
        segments: List<Segment>,
        owner: IntArray,
        within: IntArray,
        from: Int,
        to: Int,
    ): YosBcWord {
        val parts = ArrayList<YosBcSyllable>()
        var piece = from
        while (piece < to) {
            var next = piece + 1
            while (next < to && owner[next] == owner[piece]) next++
            val segment = segments[owner[piece]]
            val length = segment.text.length.coerceAtLeast(1)
            val begin = segment.start + (segment.end - segment.start) * within[piece] / length
            val finish = segment.start + (segment.end - segment.start) * (within[next - 1] + 1) / length
            parts += YosBcSyllable(
                startMs = begin.toLong(),
                endMs = finish.toLong(),
                startChar = piece - from,
                endChar = next - from,
            )
            piece = next
        }
        val word = text.substring(from, to)
        return YosBcWord(
            startMs = parts.first().startMs,
            endMs = parts.last().endMs,
            text = word,
            syllables = YosBcSyllable.normalized(parts, word.length),
        )
    }

    private class Segment(val text: String, val start: Float, val end: Float)

    private fun Char.isBlockScript(): Boolean =
        this in '一'..'鿿' || this in '぀'..'ゟ' ||
            this in '゠'..'ヿ' || this in '가'..'힯'
}
