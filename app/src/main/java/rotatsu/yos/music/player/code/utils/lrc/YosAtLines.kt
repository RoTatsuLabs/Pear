package rotatsu.yos.music.player.code.utils.lrc

/** Builds the lines ArchiveTune's renderer draws from the pairs of a Pear lyric line. */
internal object YosAtLines {

    /**
     * Cuts [mainLyric] into groups. A group is a word and the whitespace after it, so a word
     * of several syllables stays one group. Han, kana and Hangul have no spaces to cut at, so there
     * every timed segment is a group. Inside a segment the time is spread evenly over its
     * characters, as Accompanist spreads a syllable over its letters.
     */
    fun fromPairs(mainLyric: List<Pair<Float, String>>): YosAtLine {
        val lineStart = mainLyric.firstOrNull()?.first ?: 0f
        val segments = ArrayList<Segment>()
        mainLyric.firstOrNull()?.second?.takeIf { it.isNotEmpty() }?.let {
            segments += Segment(it, lineStart.toInt(), lineStart.toInt())
        }
        for (timing in YosLyricTiming.timeline(mainLyric)) {
            segments += Segment(timing.text, timing.start.toInt(), timing.end.toInt())
        }
        val text = segments.joinToString("") { it.text }
        if (segments.isEmpty() || text.isBlank()) return YosAtLine(text, emptyList())

        val units = ArrayList<YosAtUnit>(text.length)
        var at = 0
        for (segment in segments) {
            val length = segment.text.length
            for (i in 0 until length) {
                val span = (segment.end.toLong() - segment.start)
                units += YosAtUnit(
                    char = at + i,
                    start = segment.start + (span * i / length).toInt(),
                    end = segment.start + (span * (i + 1) / length).toInt(),
                    animStart = segment.start,
                    animEnd = segment.end,
                )
            }
            at += length
        }

        val groups = ArrayList<YosAtGroup>()
        var begin = 0
        val segmentOf = IntArray(text.length)
        run {
            var c = 0
            segments.forEachIndexed { index, segment -> repeat(segment.text.length) { segmentOf[c++] = index } }
        }
        while (begin < text.length) {
            var end = begin
            while (end < text.length && !text[end].isWhitespace()) end++
            while (end < text.length && text[end].isWhitespace()) end++
            val blocks = (begin until end).any { text[it].isBlockScript() }
            if (blocks) {
                // Every segment of a run in a block script is a group of its own.
                var from = begin
                while (from < end) {
                    var to = from + 1
                    while (to < end && segmentOf[to] == segmentOf[from]) to++
                    groups += YosAtGroup(units.subList(from, to), cjk = true)
                    from = to
                }
            } else {
                groups += YosAtGroup(units.subList(begin, end), cjk = false)
            }
            begin = end
        }
        return YosAtLine(text, groups)
    }

    private class Segment(val text: String, val start: Int, val end: Int)

    private fun Char.isBlockScript(): Boolean =
        this in '一'..'鿿' || this in '぀'..'ゟ' ||
            this in '゠'..'ヿ' || this in '가'..'힯'
}
