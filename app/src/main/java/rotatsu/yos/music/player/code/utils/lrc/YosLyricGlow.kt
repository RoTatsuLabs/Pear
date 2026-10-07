package rotatsu.yos.music.player.code.utils.lrc

/** One held segment whose characters glow one after another. */
internal class YosGlowWord(
    val timing: YosWordTiming,
    /** The segment text without surrounding spaces. */
    val text: String,
    /** Index of the first character of [text] in the whole line. */
    val offset: Int
) {
    private val held = (timing.end - timing.start).coerceAtLeast(1f)
    private val peaks = FloatArray(text.length)

    /** When the last character has finished fading. */
    val restsAt: Float

    init {
        val earned = ((held - RAMP_MIN_MS) / (RAMP_MAX_MS - RAMP_MIN_MS))
            .coerceIn(0f, 1f)
            .let { it * it * it }
        val decay = decayRate(text.length, held)
        val pace = minOf(PACE_MAX, held / PACE_MS)
        val spread = when {
            text.length <= 3 -> SPREAD_SHORT
            text.length >= 6 -> SPREAD_LONG
            else -> 1f
        }
        for (i in text.indices) {
            val place = if (text.length > 1) i.toFloat() / (text.length - 1) else 0f
            val reach = earned * (1f - place * decay)
            peaks[i] = (FLOOR + reach * RANGE) * pace * spread
        }
        restsAt = timing.start + held * ((text.length - 1).coerceAtLeast(0) * STAGGER + SPAN)
    }

    /** Glow of character [index] of [text] at [time], from 0 to 1. */
    fun bloom(index: Int, time: Float): Float {
        val phase = ((time - timing.start - index * held * STAGGER) / (held * SPAN)).coerceIn(0f, 1f)
        val peak = peaks[index]
        return when {
            phase <= 0f -> 0f
            phase < RISE_END -> peak * smooth(phase / RISE_END)
            phase < HOLD_END -> peak
            phase < FALL_END -> peak * (1f - smooth((phase - HOLD_END) / (FALL_END - HOLD_END)))
            else -> 0f
        }
    }

    private companion object {
        const val STAGGER = 0.09f
        const val SPAN = 1.5f
        const val RISE_END = 0.25f
        const val HOLD_END = 0.30f
        const val FALL_END = 0.75f
        const val RAMP_MIN_MS = 400f
        const val RAMP_MAX_MS = 3000f
        const val FLOOR = 0.35f
        const val RANGE = 0.45f
        const val PACE_MS = 1500f
        const val PACE_MAX = 1.1f
        const val SPREAD_SHORT = 0.85f
        const val SPREAD_LONG = 1.1f

        fun smooth(x: Float): Float {
            val p = x.coerceIn(0f, 1f)
            return p * p * (3f - 2f * p)
        }

        fun decayRate(length: Int, heldMs: Float): Float {
            val long = length > 5
            val quick = heldMs < 1200f
            if (!long && !quick) return 0f
            var strength = 0f
            if (long) strength += minOf((length - 5) / 5f, 1f) * 0.4f
            if (quick) {
                val short = maxOf(0f, 1f - (heldMs - 800f) / 400f)
                strength += short * if (length > 3) 0.3f else 0.1f
            }
            return minOf(strength, 0.7f)
        }
    }
}

/**
 * Glow of held words, after the lyric view of BitChord (GPL-3.0). Only words held long enough
 * glow, so quick syllables stay plain and a carried note lights up character by character.
 */
internal object YosLyricGlow {

    private const val MAX_CHARS = 7

    /** Whether a segment of [text] sung for [heldMs] is long enough to glow. */
    fun canGlow(text: String, heldMs: Float): Boolean {
        val core = text.trim()
        val length = core.length
        if (length == 0 || length > MAX_CHARS) return false
        if ('-' in core || core.any { it in '\u0590'..'\u08FF' }) return false
        return when {
            length == 1 -> heldMs >= 1100f
            length <= 3 -> heldMs >= 1360f + (length - 2) * 140f
            length == 4 -> heldMs >= 1050f
            else -> heldMs >= 900f && heldMs >= length * 200f
        }
    }

    /** The glowing words of a line, with their position in the line text. */
    fun words(timeline: List<YosWordTiming>): List<YosGlowWord> {
        val result = ArrayList<YosGlowWord>()
        var offset = 0
        timeline.forEach { word ->
            if (canGlow(word.text, word.end - word.start)) {
                val lead = word.text.length - word.text.trimStart().length
                result += YosGlowWord(word, word.text.trim(), offset + lead)
            }
            offset += word.text.length
        }
        return result
    }
}
