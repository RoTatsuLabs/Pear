package rotatsu.yos.music.player.code.utils.lrc

// The lyric line model of Apple Music-like Lyrics (AMLL), ported from packages/core/src/lyric-player of
// https://github.com/amll-dev/applemusic-like-lyrics (AGPL-3.0), Copyright the AMLL authors. Ported: the split of
// multi-word segments, the chunks emphasis is decided on, the emphasis and float animations, and the way a
// single soft edge sweeps the words of a line. Changed: it works on a Pear text layout, not on DOM elements.

import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** One timed word of a line: the characters [charStart] up to [charEnd] of its text, never whitespace. */
internal class YosAmllWord(
    val charStart: Int,
    val charEnd: Int,
    val start: Float,
    val end: Float,
    val cjk: Boolean,
    val chunk: Int,
)

/**
 * A run of words with no whitespace between them, which is what AMLL gives its emphasis. When [emphasized], each
 * letter in [chars] swells and glows in turn.
 */
internal class YosAmllChunk(
    val firstWord: Int,
    val lastWord: Int,
    val start: Float,
    val end: Float,
    val emphasized: Boolean,
    /** Where each animated letter starts in the text, and where it ends. */
    val chars: IntArray,
    val charEnds: IntArray,
    val amount: Float,
    val blur: Float,
    val duration: Float,
)

internal class YosAmllLine(
    val text: String,
    val start: Float,
    val end: Float,
    val words: List<YosAmllWord>,
    val chunks: List<YosAmllChunk>,
)

internal object YosAmllLines {

    private fun Char.isBlockScript(): Boolean =
        this in '一'..'鿿' || this in '぀'..'ゟ' ||
            this in '゠'..'ヿ' || this in '가'..'힯'

    /** An atom of a line: a word, or the whitespace between words, which only ends a chunk. */
    private class Atom(val space: Boolean, val charStart: Int, val charEnd: Int, val start: Float, val end: Float, val cjk: Boolean)

    /**
     * Builds the line from the pairs of a Pear lyric line. A segment with several words is split at its whitespace
     * and its time shared out by letters, as AMLL's chunkAndSplitLyricWords does.
     */
    fun fromPairs(mainLyric: List<Pair<Float, String>>): YosAmllLine {
        val lineStart = mainLyric.firstOrNull()?.first ?: 0f
        val segments = ArrayList<Triple<String, Float, Float>>()
        for (timing in YosLyricTiming.timeline(mainLyric)) segments += Triple(timing.text, timing.start, timing.end)
        val text = segments.joinToString("") { it.first }
        if (segments.isEmpty() || text.isBlank()) return YosAmllLine(text, lineStart, lineStart, emptyList(), emptyList())

        val atoms = ArrayList<Atom>()
        var at = 0
        for ((segmentText, start, end) in segments) {
            val letters = segmentText.count { !it.isWhitespace() }.coerceAtLeast(1)
            val perLetter = (end - start) / letters
            var offset = 0
            var i = 0
            while (i < segmentText.length) {
                val space = segmentText[i].isWhitespace()
                var j = i
                while (j < segmentText.length && segmentText[j].isWhitespace() == space) j++
                if (space) {
                    atoms += Atom(true, at + i, at + j, start + offset * perLetter, start + offset * perLetter, false)
                } else {
                    val from = start + offset * perLetter
                    val to = if (i == 0 && j == segmentText.length) end else from + (j - i) * perLetter
                    val cjk = (i until j).any { segmentText[it].isBlockScript() }
                    atoms += Atom(false, at + i, at + j, from, to, cjk)
                    offset += j - i
                }
                i = j
            }
            at += segmentText.length
        }

        val words = ArrayList<YosAmllWord>()
        val chunks = ArrayList<YosAmllChunk>()
        var run = ArrayList<Atom>()

        fun flush() {
            if (run.isEmpty()) return
            val first = words.size
            val index = chunks.size
            for (atom in run) words += YosAmllWord(atom.charStart, atom.charEnd, atom.start, atom.end, atom.cjk, index)
            val start = run.minOf { it.start }
            val end = run.maxOf { it.end }
            val cjk = run.first().cjk
            val mergedLetters = run.sumOf { it.charEnd - it.charStart }
            val emphasized = run.any { YosAmllEffects.shouldEmphasize(it.end - it.start, it.charEnd - it.charStart, it.cjk) } ||
                (!cjk && YosAmllEffects.shouldEmphasize(end - start, mergedLetters, false))
            val starts = ArrayList<Int>()
            val ends = ArrayList<Int>()
            if (emphasized) {
                for (atom in run) {
                    var c = atom.charStart
                    while (c < atom.charEnd) {
                        val next = c + Character.charCount(text.codePointAt(c))
                        starts += c
                        ends += next
                        c = next
                    }
                }
            }
            chunks += YosAmllChunk(
                firstWord = first,
                lastWord = words.size - 1,
                start = start,
                end = end,
                emphasized = emphasized,
                chars = starts.toIntArray(),
                charEnds = ends.toIntArray(),
                amount = 0f,
                blur = 0f,
                duration = 0f,
            )
            run = ArrayList()
        }

        for (atom in atoms) {
            when {
                atom.space -> flush()
                atom.cjk -> {
                    flush()
                    run += atom
                    flush()
                }
                else -> run += atom
            }
        }
        flush()

        // The last word gets stronger effects, so the parameters wait until it is known which chunk is last.
        val last = chunks.lastIndex
        val finished = chunks.mapIndexed { index, chunk ->
            if (!chunk.emphasized) {
                chunk
            } else {
                val p = YosAmllEffects.params(chunk.end - chunk.start, index == last)
                YosAmllChunk(chunk.firstWord, chunk.lastWord, chunk.start, chunk.end, true, chunk.chars, chunk.charEnds, p.amount, p.blur, p.duration)
            }
        }
        val end = words.maxOfOrNull { it.end } ?: lineStart
        return YosAmllLine(text, lineStart, end, words, finished)
    }
}

/** What the emphasis does to one letter, in ems of the font size. */
internal class YosAmllCharEffect {
    var scale = 1f
    var offsetX = 0f
    var offsetY = 0f
    var glowRadius = 0f
    var glowAlpha = 0f
}

internal class YosAmllEmphasis(val amount: Float, val blur: Float, val duration: Float)

internal object YosAmllEffects {
    /** How far a word rises as it is sung, in ems. */
    const val FLOAT_EM = 0.05f

    /** The share of the row height a soft edge is wide. */
    const val FADE_OF_HEIGHT = 0.5f

    /** A word of a second or more, and of two to seven letters when it is not Han, kana or Hangul. */
    fun shouldEmphasize(durationMs: Float, letters: Int, cjk: Boolean): Boolean {
        if (cjk) return durationMs >= 1000f
        return durationMs >= 1000f && letters in 2..7
    }

    fun params(durationMs: Float, last: Boolean): YosAmllEmphasis {
        var du = max(1000f, durationMs)
        var amount = du / 2000f
        amount = if (amount > 1f) sqrt(amount) else amount.pow(3)
        var blur = du / 3000f
        blur = if (blur > 1f) sqrt(blur) else blur.pow(3)
        amount *= 0.6f
        blur *= 0.5f
        if (last) {
            amount *= 1.6f
            blur *= 1.5f
            du *= 1.2f
        }
        return YosAmllEmphasis(min(1.2f, amount), min(0.8f, blur), du)
    }

    /** A CSS cubic-bezier easing: the progress along the curve whose x is [t]. */
    fun bezier(x1: Float, y1: Float, x2: Float, y2: Float, t: Float): Float {
        if (t <= 0f) return 0f
        if (t >= 1f) return 1f
        var low = 0f
        var high = 1f
        var s = t
        repeat(28) {
            s = (low + high) / 2f
            val x = 3 * (1 - s) * (1 - s) * s * x1 + 3 * (1 - s) * s * s * x2 + s * s * s
            if (x < t) low = s else high = s
        }
        return 3 * (1 - s) * (1 - s) * s * y1 + 3 * (1 - s) * s * s * y2 + s * s * s
    }

    /** AMLL's empEasing: up over the first half, back down over the second. */
    fun empEasing(x: Float): Float =
        if (x < 0.5f) bezier(0.2f, 0.4f, 0.58f, 1f, x / 0.5f) else 1f - bezier(0.3f, 0f, 0.58f, 1f, (x - 0.5f) / 0.5f)

    /** How high a word has risen by [time], in ems and negative. Every word floats up and stays there. */
    fun wordFloat(start: Float, end: Float, time: Float): Float {
        val duration = max(1000f, end - start)
        val progress = ((time - start) / duration).coerceIn(0f, 1f)
        return -FLOAT_EM * bezier(0f, 0f, 0.58f, 1f, progress)
    }

    /** The swell and glow of letter [index] of [chunk] at [time], and the float on top of it. Returns the float. */
    fun charEffect(chunk: YosAmllChunk, index: Int, time: Float, out: YosAmllCharEffect): Float {
        val total = chunk.chars.size
        val begin = chunk.start + chunk.duration / 2.5f / max(1, total) * index
        val x = ((time - begin) / chunk.duration).coerceIn(0f, 1f)
        val eased = empEasing(x)
        out.scale = 1f + eased * 0.1f * chunk.amount
        out.offsetX = -eased * 0.03f * chunk.amount * (total / 2f - index)
        out.offsetY = -eased * 0.025f * chunk.amount
        out.glowRadius = min(0.3f, chunk.blur * 0.3f)
        out.glowAlpha = eased * chunk.blur
        val floatX = ((time - (begin - 400f)) / (chunk.duration * 1.4f)).coerceIn(0f, 1f)
        return -sin(floatX * PI.toFloat()) * FLOAT_EM
    }
}

/**
 * The soft edge of AMLL's word mask. One edge sweeps all the words of a line in turn, crossing each over that
 * word's own time, so a short word is crossed fast and a long one slowly. It rests in a pause. The first word is
 * entered from a fade and a half before it, and the edge leaves the last a half fade past it.
 *
 * Every array is per word piece, in the order they are sung: where it starts on its row, how wide it is, and its
 * time. A word that wraps is two pieces.
 */
internal class YosAmllSweep(
    private val left: FloatArray,
    private val width: FloatArray,
    private val start: FloatArray,
    private val end: FloatArray,
    private val fade: Float,
) {
    private val count = left.size
    private val before = FloatArray(count)
    private val move = FloatArray(count)

    init {
        var sum = 0f
        for (k in 0 until count) {
            before[k] = sum
            sum += width[k]
            move[k] = width[k] + (if (k == 0) 1.5f * fade else 0f) + (if (k == count - 1) 0.5f * fade else 0f)
        }
    }

    /** Fills [out] with where the middle of the edge is on each piece at [time]. */
    fun centers(time: Float, out: FloatArray) {
        var travelled = 0f
        for (k in 0 until count) {
            val progress = if (end[k] > start[k]) ((time - start[k]) / (end[k] - start[k])).coerceIn(0f, 1f) else if (time >= start[k]) 1f else 0f
            travelled += progress * move[k]
        }
        for (k in 0 until count) {
            val raw = left[k] - 1.5f * fade + travelled - before[k]
            out[k] = raw.coerceIn(left[k] - fade / 2f, left[k] + width[k] + fade / 2f)
        }
    }
}
