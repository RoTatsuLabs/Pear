package rotatsu.yos.music.player.code.utils.lrc

/** Matches the syllables of a transliteration to the characters they belong to. */
internal object YosSyllables {

    /**
     * Pairs every character of [word] with its own syllable of [roman], so each syllable can
     * be drawn above its character and keep its place when the line wraps.
     *
     * Returns the index of each character in [word] with its syllable, or null when the
     * syllables cannot be matched one to one with the characters. That is the case for words
     * such as 안녕 (one syllable "annyeong" for two characters), where the transliteration
     * has to stay one piece. Whitespace in [word] has no syllable.
     */
    fun perCharacter(word: String, roman: String): List<Pair<Int, String>>? {
        if (word.any { it.isSurrogate() }) return null
        val indexes = word.indices.filter { !word[it].isWhitespace() }
        val syllables = roman.trim().split(WHITESPACE).filter { it.isNotEmpty() }
        if (indexes.isEmpty() || syllables.size != indexes.size) return null
        return indexes.zip(syllables)
    }

    private val WHITESPACE = Regex("\\s+")
}
