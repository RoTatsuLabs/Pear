package yos.music.player.code.utils.lrc

/** Writing systems the transliteration tells apart. */
internal enum class YosScript {
    /** Letters, digits and punctuation that already read as Latin text. */
    LATIN,
    HAN, KANA, HANGUL, CYRILLIC, GREEK, THAI, ARABIC, HEBREW, DEVANAGARI,

    /** Symbols and anything else that is left alone. */
    OTHER;

    /** Whether text in this script has to be converted to be read as Latin. */
    val needsConversion: Boolean get() = this != LATIN && this != OTHER
}

internal class YosScriptRun(val script: YosScript, val text: String)

/**
 * Script detection by code point range. Character.UnicodeScript would be shorter, but it
 * needs API 24 and the app still supports API 23.
 */
internal object YosScripts {
    fun of(cp: Int): YosScript = when {
        cp in 0x4E00..0x9FFF || cp in 0x3400..0x4DBF || cp in 0x20000..0x2A6DF ||
                cp in 0xF900..0xFAFF || cp == 0x3007 -> YosScript.HAN

        cp in 0x3040..0x309F || cp in 0x30A0..0x30FF || cp in 0x31F0..0x31FF ||
                cp in 0xFF66..0xFF9F -> YosScript.KANA

        cp in 0xAC00..0xD7AF || cp in 0x1100..0x11FF || cp in 0x3130..0x318F -> YosScript.HANGUL
        cp in 0x0400..0x052F -> YosScript.CYRILLIC
        cp in 0x0370..0x03FF || cp in 0x1F00..0x1FFF -> YosScript.GREEK
        cp in 0x0E00..0x0E7F -> YosScript.THAI
        cp in 0x0600..0x06FF || cp in 0x0750..0x077F || cp in 0xFB50..0xFDFF ||
                cp in 0xFE70..0xFEFF -> YosScript.ARABIC

        cp in 0x0590..0x05FF -> YosScript.HEBREW
        cp in 0x0900..0x097F -> YosScript.DEVANAGARI
        cp < 0x0250 || cp in 0x1E00..0x1EFF || cp in 0x2000..0x206F ||
                cp in 0x3000..0x303F || cp in 0xFF00..0xFFEF -> YosScript.LATIN

        else -> YosScript.OTHER
    }

    /** Splits [text] into runs of one script each, in order. */
    fun split(text: String): List<YosScriptRun> {
        val runs = mutableListOf<YosScriptRun>()
        var current: YosScript? = null
        val buffer = StringBuilder()
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            val script = of(cp)
            if (script != current && buffer.isNotEmpty()) {
                runs += YosScriptRun(current!!, buffer.toString())
                buffer.setLength(0)
            }
            current = script
            buffer.appendCodePoint(cp)
            i += Character.charCount(cp)
        }
        if (buffer.isNotEmpty()) runs += YosScriptRun(current!!, buffer.toString())
        return runs
    }
}
