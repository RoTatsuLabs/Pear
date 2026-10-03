package yos.music.player.code.utils.lrc

import android.os.Build
import com.github.promeg.pinyinhelper.Pinyin

/**
 * Automatic transliteration of lyrics into Latin letters.
 *
 * A line keeps the transliteration its source gave it (TTML can carry one). Only lines
 * without one are filled in here. Every lyric segment is converted on its own, so the
 * result stays lined up with the segment it belongs to.
 *
 * Engines, in order:
 *  - the platform ICU Transliterator on API 29 and above, for every script it knows;
 *  - below API 29, built in tables for Hangul, kana, Cyrillic and Greek, and TinyPinyin
 *    for Han characters. Other scripts get no transliteration there.
 *
 * Han characters are read as Mandarin, one character at a time, so a character with
 * several readings gets its most common one. Japanese kanji have readings of their own
 * that no table can pick, so a Japanese line that contains kanji is left alone instead of
 * showing pinyin for it. Results are cached by text.
 */
object YosTransliterator {

    /** Reads one Han character in the fallback. Replaceable so tests need no dictionary. */
    internal var hanReader: (Char) -> String? = { c ->
        Pinyin.toPinyin(c).takeIf { it.isNotEmpty() && it[0] != c }?.lowercase()
    }

    /** Whether the ICU Transliterator is used. Tests turn it off to run the fallback. */
    internal var icuAvailable: () -> Boolean = { Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q }

    private const val CACHE_SIZE = 4096
    private val cache = object : LinkedHashMap<String, String>(256, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?) =
            size > CACHE_SIZE
    }

    /** Returns [lyrics] with a transliteration for every line that has none and needs one. */
    fun fill(lyrics: YosLyrics): YosLyrics {
        if (lyrics.isEmpty) return lyrics
        var changed = false
        val filled = lyrics.entries.mapIndexed { index, line ->
            val existing = lyrics.transliterations.getOrNull(index)
            if (existing != null) return@mapIndexed existing

            val segments = YosLyrics.segmentsOf(line)
            val converted = segments.map { transliterate(it.second, lyrics.language) }
            // A Japanese line that is only partly readable would mix two scripts, so it
            // shows nothing instead.
            if (converted.all { it == null } || isPartial(segments, converted)) {
                null
            } else {
                changed = true
                converted.map { it.orEmpty() }
            }
        }
        return if (changed) lyrics.copy(transliterations = filled) else lyrics
    }

    private fun isPartial(segments: List<Pair<Float, String>>, converted: List<String?>): Boolean =
        segments.indices.any { i ->
            converted[i] == null && YosScripts.split(segments[i].second).any { it.script.needsConversion }
        }

    /**
     * Converts [text] to Latin letters. Returns null when it needs no conversion, when it
     * cannot be converted on this device, or when the language makes the result unreliable.
     *
     * @param language language code of the lyrics when known, e.g. "ja" or "zh-Hans"
     */
    fun transliterate(text: String, language: String? = null): String? {
        if (text.isBlank()) return null
        val key = (language?.take(2).orEmpty()) + "|" + text
        synchronized(cache) {
            cache[key]?.let { return it.ifEmpty { null } }
        }
        val result = convert(text, language)
        synchronized(cache) { cache[key] = result.orEmpty() }
        return result
    }

    private fun convert(text: String, language: String?): String? {
        val runs = YosScripts.split(text)
        if (runs.none { it.script.needsConversion }) return null

        val japanese = runs.any { it.script == YosScript.KANA } ||
                language?.startsWith("ja", ignoreCase = true) == true
        val useIcu = icuAvailable()

        val out = StringBuilder()
        var previousNeededConversion = false
        runs.forEach { run ->
            val converted: String = when {
                !run.script.needsConversion -> run.text
                run.script == YosScript.HAN -> {
                    if (japanese) return null // kanji readings cannot be picked from a table
                    readHan(run.text, useIcu) ?: return null
                }

                else -> convertRun(run, useIcu) ?: return null
            }
            // "你好world" reads better as "ni hao world" than "ni haoworld"
            val joinsWords = out.isNotEmpty() && out.last().isLetterOrDigit() &&
                    converted.firstOrNull()?.isLetterOrDigit() == true
            if (joinsWords && (run.script.needsConversion || previousNeededConversion)) out.append(' ')
            previousNeededConversion = run.script.needsConversion
            out.append(converted)
        }

        val result = out.toString().replace(Regex("\\s+"), " ").trim()
        return result.takeIf { it.isNotEmpty() && !it.equals(text.trim(), ignoreCase = true) }
    }

    /** One syllable per character, written with spaces between them. */
    private fun readHan(text: String, useIcu: Boolean): String? {
        val syllables = mutableListOf<String>()
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            val piece = String(Character.toChars(cp))
            val reading = if (useIcu) IcuEngine.convert(piece) else if (cp <= 0xFFFF) hanReader(piece[0]) else null
            if (reading.isNullOrBlank()) return null
            syllables += reading.trim()
            i += Character.charCount(cp)
        }
        return syllables.joinToString(" ")
    }

    private fun convertRun(run: YosScriptRun, useIcu: Boolean): String? {
        if (useIcu) return IcuEngine.convert(run.text)
        return when (run.script) {
            YosScript.HANGUL -> YosRomanizers.hangul(run.text)
            YosScript.KANA -> YosRomanizers.kana(run.text)
            YosScript.CYRILLIC -> YosRomanizers.cyrillic(run.text)
            YosScript.GREEK -> YosRomanizers.greek(run.text)
            else -> null // no table for it, and no ICU on this device
        }
    }

    /** Clears the cache. Used by tests. */
    internal fun clearCache() = synchronized(cache) { cache.clear() }
}

/**
 * The platform transliterator. It lives in its own object so devices below API 29 never
 * load a class that refers to android.icu.
 */
private object IcuEngine {
    // "Any-Latin" reads every script ICU knows, "Latin-ASCII" then drops tone marks and
    // other diacritics so "wǒ" becomes "wo".
    private val transliterator: android.icu.text.Transliterator? by lazy {
        runCatching {
            android.icu.text.Transliterator.getInstance("Any-Latin; Latin-ASCII")
        }.getOrNull()
    }

    fun convert(text: String): String? =
        runCatching { transliterator?.transliterate(text) }.getOrNull()?.takeIf { it.isNotBlank() }
}
