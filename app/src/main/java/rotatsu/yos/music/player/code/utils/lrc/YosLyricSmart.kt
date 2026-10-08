package rotatsu.yos.music.player.code.utils.lrc

/**
 * Gives lines that have no word timing an even word by word timing, so they sweep from left
 * to right across the time until the next line. Lines that already have word timing stay as
 * they are.
 */
object YosLyricSmart {
    private const val LAST_LINE_MS = 4000f
    private const val LONGEST_SWEEP_MS = 7000f
    private const val SWEEP_SHARE = 0.9f

    fun expand(lyrics: YosLyrics): YosLyrics {
        var changed = false
        val entries = ArrayList<List<Pair<Float, String>>>(lyrics.entries.size)
        val transliterations = ArrayList<List<String>?>(lyrics.entries.size)
        lyrics.entries.forEachIndexed { index, line ->
            val next = lyrics.entries.getOrNull(index + 1)?.firstOrNull()?.first
            val rebuilt = expandLine(line, next)
            if (rebuilt != null) {
                changed = true
                entries += rebuilt
                // The words are new, so a transliteration per old word no longer fits.
                transliterations += null
            } else {
                entries += line
                transliterations += lyrics.transliterations.getOrNull(index)
            }
        }
        return if (changed) lyrics.copy(entries = entries, transliterations = transliterations) else lyrics
    }

    private fun expandLine(line: List<Pair<Float, String>>, nextBegin: Float?): List<Pair<Float, String>>? {
        if (line.size < 3) return null
        val begin = line.first().first
        if (line.any { it.first != begin }) return null

        val text = YosLyrics.segmentsOf(line).joinToString("") { it.second }.trim()
        if (text.isEmpty()) return null

        val words = split(text)
        val weights = words.map { weight(it) }
        val total = weights.sum()
        val gap = if (nextBegin != null && nextBegin > begin) nextBegin - begin else LAST_LINE_MS
        val duration = minOf(gap, LONGEST_SWEEP_MS) * SWEEP_SHARE

        val tail = line.takeLast(if (YosLyrics.carriesTranslation(line)) 3 else 1)
        val rebuilt = ArrayList<Pair<Float, String>>(words.size + 1 + tail.size)
        rebuilt += line.first()
        var sofar = 0f
        words.forEachIndexed { i, word ->
            sofar += weights[i]
            rebuilt += (begin + duration * sofar / total) to word
        }
        rebuilt += tail
        return rebuilt
    }

    /** Words end at a space, and every Chinese, Japanese or Korean character stands alone. */
    internal fun split(text: String): List<String> {
        val words = ArrayList<String>()
        val current = StringBuilder()
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            val size = Character.charCount(cp)
            val piece = text.substring(i, i + size)
            when {
                Character.isWhitespace(cp) -> {
                    if (words.isEmpty() && current.isEmpty()) {
                        // a leading space has no word to join
                    } else if (current.isNotEmpty()) {
                        current.append(piece)
                        words += current.toString()
                        current.clear()
                    } else {
                        words[words.lastIndex] = words.last() + piece
                    }
                }

                isStandalone(cp) -> {
                    if (current.isNotEmpty()) {
                        words += current.toString()
                        current.clear()
                    }
                    words += piece
                }

                else -> current.append(piece)
            }
            i += size
        }
        if (current.isNotEmpty()) words += current.toString()
        return words
    }

    private fun isStandalone(codePoint: Int): Boolean =
        when (Character.UnicodeScript.of(codePoint)) {
            Character.UnicodeScript.HAN,
            Character.UnicodeScript.HIRAGANA,
            Character.UnicodeScript.KATAKANA,
            Character.UnicodeScript.HANGUL -> true

            else -> false
        }

    private fun weight(word: String): Float {
        var total = 0f
        var i = 0
        while (i < word.length) {
            val cp = word.codePointAt(i)
            if (!Character.isWhitespace(cp)) total += if (isStandalone(cp)) 2f else 1f
            i += Character.charCount(cp)
        }
        return total.coerceAtLeast(1f)
    }
}
