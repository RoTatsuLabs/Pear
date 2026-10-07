package rotatsu.yos.music.player.code.utils.lrc

import java.text.Normalizer

/**
 * Plain table based romanizers. They are the fallback below API 29, where the platform
 * has no ICU transliterator, and they need no Android classes.
 */
internal object YosRomanizers {

    // ---- Hangul (Revised Romanization, one syllable at a time) -----------------------

    private val INITIALS = arrayOf(
        "g", "kk", "n", "d", "tt", "r", "m", "b", "pp", "s", "ss", "", "j", "jj", "ch", "k", "t", "p", "h"
    )
    private val MEDIALS = arrayOf(
        "a", "ae", "ya", "yae", "eo", "e", "yeo", "ye", "o", "wa", "wae", "oe", "yo",
        "u", "wo", "we", "wi", "yu", "eu", "ui", "i"
    )
    private val FINALS = arrayOf(
        "", "k", "k", "k", "n", "n", "n", "t", "l", "k", "m", "l", "l", "l", "p", "l",
        "m", "p", "p", "t", "t", "ng", "t", "t", "k", "t", "p", "t"
    )

    // When a final consonant is followed by a syllable that starts with a silent ㅇ, it is
    // pronounced as the start of that syllable. For double finals only the second part moves.
    // Index is the final consonant, the pair is (stays on this syllable, moves to the next).
    private val LIAISON = arrayOf(
        "" to "", "" to "g", "" to "kk", "k" to "s", "" to "n", "n" to "j", "n" to "",
        "" to "d", "" to "r", "l" to "g", "l" to "m", "l" to "b", "l" to "s", "l" to "t",
        "l" to "p", "l" to "", "" to "m", "" to "b", "p" to "s", "" to "s", "" to "ss",
        "ng" to "", "" to "j", "" to "ch", "" to "k", "" to "t", "" to "p", "" to ""
    )

    /**
     * Romanizes Hangul syllables. A final consonant before a silent ㅇ moves to the next
     * syllable, as it is spoken (한국어 is hangugeo). Other sound changes between
     * syllables, such as nasalization, are not applied, so those words follow the spelling.
     */
    fun hangul(text: String): String {
        val out = StringBuilder()
        var carried = "" // the sound moved over from the previous syllable
        text.forEachIndexed { i, c ->
            val index = c.code - 0xAC00
            if (index !in 0..11171) {
                // Compatibility jamo and anything else: decompose through NFKD first.
                val decomposed = Normalizer.normalize(c.toString(), Normalizer.Form.NFKD)
                if (decomposed != c.toString()) out.append(hangul(decomposed)) else out.append(c)
                return@forEachIndexed
            }
            val initial = index / 588
            val medial = index % 588 / 28
            val final = index % 28

            // A carried sound takes the place of the silent ㅇ it was moved in front of.
            out.append(if (initial == 11) carried else INITIALS[initial])
            carried = ""
            out.append(MEDIALS[medial])

            val next = text.getOrNull(i + 1)?.let { it.code - 0xAC00 } ?: -1
            val nextIsSilent = next in 0..11171 && next / 588 == 11
            if (final != 0 && final != 21 && nextIsSilent) {
                out.append(LIAISON[final].first)
                carried = LIAISON[final].second
            } else {
                out.append(FINALS[final])
            }
        }
        return out.toString()
    }

    // ---- Cyrillic ----------------------------------------------------------------------

    private val CYRILLIC = mapOf(
        'а' to "a", 'б' to "b", 'в' to "v", 'г' to "g", 'д' to "d", 'е' to "e", 'ё' to "yo",
        'ж' to "zh", 'з' to "z", 'и' to "i", 'й' to "y", 'к' to "k", 'л' to "l", 'м' to "m",
        'н' to "n", 'о' to "o", 'п' to "p", 'р' to "r", 'с' to "s", 'т' to "t", 'у' to "u",
        'ф' to "f", 'х' to "kh", 'ц' to "ts", 'ч' to "ch", 'ш' to "sh", 'щ' to "shch",
        'ъ' to "", 'ы' to "y", 'ь' to "", 'э' to "e", 'ю' to "yu", 'я' to "ya",
        'і' to "i", 'ї' to "yi", 'є' to "ye", 'ґ' to "g", 'ў' to "u", 'ј' to "j", 'љ' to "lj",
        'њ' to "nj", 'ћ' to "c", 'ђ' to "dj", 'џ' to "dz"
    )

    fun cyrillic(text: String): String = mapLetters(text, CYRILLIC)

    // ---- Greek -------------------------------------------------------------------------

    private val GREEK = mapOf(
        'α' to "a", 'β' to "v", 'γ' to "g", 'δ' to "d", 'ε' to "e", 'ζ' to "z", 'η' to "i",
        'θ' to "th", 'ι' to "i", 'κ' to "k", 'λ' to "l", 'μ' to "m", 'ν' to "n", 'ξ' to "x",
        'ο' to "o", 'π' to "p", 'ρ' to "r", 'σ' to "s", 'ς' to "s", 'τ' to "t", 'υ' to "y",
        'φ' to "f", 'χ' to "ch", 'ψ' to "ps", 'ω' to "o"
    )

    fun greek(text: String): String {
        // Accents are dropped first, so each letter finds its table entry.
        val plain = Normalizer.normalize(text, Normalizer.Form.NFD)
            .filter { Character.getType(it) != Character.NON_SPACING_MARK.toInt() }
        return mapLetters(plain, GREEK)
    }

    private fun mapLetters(text: String, table: Map<Char, String>): String {
        val out = StringBuilder()
        text.forEach { c ->
            val lower = c.lowercaseChar()
            val mapped = table[lower]
            when {
                mapped == null -> out.append(c)
                c != lower && mapped.isNotEmpty() ->
                    out.append(mapped.replaceFirstChar { it.uppercaseChar() })

                else -> out.append(mapped)
            }
        }
        return out.toString()
    }

    // ---- Kana (Hepburn) ------------------------------------------------------------------

    private val KANA = mapOf(
        'あ' to "a", 'い' to "i", 'う' to "u", 'え' to "e", 'お' to "o",
        'か' to "ka", 'き' to "ki", 'く' to "ku", 'け' to "ke", 'こ' to "ko",
        'が' to "ga", 'ぎ' to "gi", 'ぐ' to "gu", 'げ' to "ge", 'ご' to "go",
        'さ' to "sa", 'し' to "shi", 'す' to "su", 'せ' to "se", 'そ' to "so",
        'ざ' to "za", 'じ' to "ji", 'ず' to "zu", 'ぜ' to "ze", 'ぞ' to "zo",
        'た' to "ta", 'ち' to "chi", 'つ' to "tsu", 'て' to "te", 'と' to "to",
        'だ' to "da", 'ぢ' to "ji", 'づ' to "zu", 'で' to "de", 'ど' to "do",
        'な' to "na", 'に' to "ni", 'ぬ' to "nu", 'ね' to "ne", 'の' to "no",
        'は' to "ha", 'ひ' to "hi", 'ふ' to "fu", 'へ' to "he", 'ほ' to "ho",
        'ば' to "ba", 'び' to "bi", 'ぶ' to "bu", 'べ' to "be", 'ぼ' to "bo",
        'ぱ' to "pa", 'ぴ' to "pi", 'ぷ' to "pu", 'ぺ' to "pe", 'ぽ' to "po",
        'ま' to "ma", 'み' to "mi", 'む' to "mu", 'め' to "me", 'も' to "mo",
        'や' to "ya", 'ゆ' to "yu", 'よ' to "yo",
        'ら' to "ra", 'り' to "ri", 'る' to "ru", 'れ' to "re", 'ろ' to "ro",
        'わ' to "wa", 'ゐ' to "i", 'ゑ' to "e", 'を' to "o", 'ん' to "n", 'ゔ' to "vu"
    )
    private val SMALL_YA = mapOf('ゃ' to 'a', 'ゅ' to 'u', 'ょ' to 'o')
    private val SMALL_VOWEL = mapOf('ぁ' to 'a', 'ぃ' to 'i', 'ぅ' to 'u', 'ぇ' to 'e', 'ぉ' to 'o')

    /** Katakana becomes hiragana first, so one table serves both. */
    private fun toHiragana(c: Char): Char =
        if (c.code in 0x30A1..0x30F6) (c.code - 0x60).toChar() else c

    fun kana(text: String): String {
        val chars = text.map { toHiragana(it) }
        val out = StringBuilder()
        var doubleNext = false
        var i = 0
        while (i < chars.size) {
            val c = chars[i]
            val next = chars.getOrNull(i + 1)

            if (c == 'っ') {
                doubleNext = true
                i++
                continue
            }
            if (c == 'ー') {
                out.lastOrNull { it in "aiueo" }?.let { out.append(it) }
                i++
                continue
            }

            var syllable = KANA[c]
            if (syllable == null) {
                // Small kana alone, punctuation, or something unknown: keep it as written.
                val vowel = SMALL_VOWEL[c]
                if (vowel != null) {
                    out.append(vowel)
                } else {
                    out.append(text[i])
                }
                doubleNext = false
                i++
                continue
            }

            var consumed = 1
            val yaVowel = next?.let { SMALL_YA[it] }
            val smallVowel = next?.let { SMALL_VOWEL[it] }
            if (yaVowel != null && syllable.endsWith("i") && syllable.length >= 2) {
                // きゃ -> kya, しゃ -> sha, ちゃ -> cha, じゃ -> ja
                val base = syllable.dropLast(1)
                syllable = if (base == "sh" || base == "ch" || base == "j") base + yaVowel
                else base + "y" + yaVowel
                consumed = 2
            } else if (smallVowel != null && syllable.length >= 2 && syllable.last() in "aiueo") {
                // ふぁ -> fa, てぃ -> ti, うぃ -> wi: the small vowel replaces the vowel
                val base = syllable.dropLast(1)
                syllable = when {
                    c == 'う' -> "w$smallVowel"
                    syllable == "tsu" || syllable == "chi" || syllable == "shi" ->
                        syllable.dropLast(1) + smallVowel
                    syllable == "fu" -> "f$smallVowel"
                    else -> base + smallVowel
                }
                consumed = 2
            }

            if (doubleNext && syllable.isNotEmpty() && syllable[0] !in "aiueon") {
                out.append(if (syllable.startsWith("ch")) "t" else syllable.substring(0, 1))
            }
            doubleNext = false
            out.append(syllable)
            i += consumed
        }
        return out.toString()
    }
}
