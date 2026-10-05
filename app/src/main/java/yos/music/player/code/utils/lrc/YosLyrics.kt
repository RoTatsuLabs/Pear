package yos.music.player.code.utils.lrc

/** The file format a lyric text was read from. */
enum class YosLyricFormat { LRC, TTML }

/**
 * One metadata entry from the lyric source, for example the songwriters.
 *
 * [field] is the name the source used, [values] are the texts exactly as the source
 * wrote them. Nothing is shortened or reworded.
 */
data class YosLyricCredit(val field: String, val values: List<String>) {
    /** The heading shown in front of [values]; the values themselves are never changed. */
    val label: String
        get() = when (this.field.lowercase()) {
            "songwriters", "songwriter" -> "Written by"
            else -> this.field.replaceFirstChar { it.uppercase() }
        }
}

/**
 * Parsed lyrics. Both LRC and TTML end up in this one shape.
 *
 * [entries] keeps the line model the lyric view and the status bar lyric already use.
 * Every line looks like this:
 *
 *  - `(start, "")` as the first pair.
 *  - One pair per lyric segment, as `(end time, text)`. A line without word timing has a
 *    single pair whose time equals the start. A pair with empty text only moves the clock
 *    forward, it marks a pause inside the line.
 *  - `(start, "")` as the translation slot. When a translation exists it is followed by
 *    `(start, "")` and `(start, translation)`, so the last pair is always the translation.
 *
 * [otherSide], [transliterations] and every other per line list have one item per entry.
 * [transliterations] holds one text per lyric segment of the line, or null when the line
 * has none, so a transliteration can be drawn above the word it belongs to.
 */
data class YosLyrics(
    val format: YosLyricFormat?,
    val entries: List<List<Pair<Float, String>>>,
    val otherSide: List<Boolean>,
    val transliterations: List<List<String>?>,
    val credits: List<YosLyricCredit>,
    /** Language of the lyrics when the source says so, e.g. "ja" or "zh-Hans". */
    val language: String? = null
) {
    val isEmpty: Boolean get() = entries.isEmpty()

    companion object {
        val EMPTY = YosLyrics(null, emptyList(), emptyList(), emptyList(), emptyList())

        /** The lyric segments of a line: its pairs without the start and translation slots. */
        fun segmentsOf(line: List<Pair<Float, String>>): List<Pair<Float, String>> {
            if (line.size < 2) return emptyList()
            val hasTranslation = line.size >= 5 && line[line.size - 1].second.isNotEmpty() &&
                    line[line.size - 2].second.isEmpty() && line[line.size - 3].second.isEmpty()
            val end = line.size - if (hasTranslation) 3 else 1
            return line.subList(1, end)
        }
    }
}
