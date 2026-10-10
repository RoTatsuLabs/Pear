package rotatsu.yos.music.player.ui.widgets

// The line renderer of BitChord's lyric panel, ported from PlayerLyrics.kt of
// https://github.com/kushagrasinghx/BitChord (GPL-3.0). The drawing, the constants and the order of
// the passes are BitChord's. Left out are its particles and its translation motion.

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.IntState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import rotatsu.yos.music.player.code.utils.lrc.YosBcCharGrowth
import rotatsu.yos.music.player.code.utils.lrc.YosBcLine

/** How far the sweep's leading edge fades out instead of ending on a cut. */
private val WIPE_FEATHER = 30.dp

/** How far the word being sung lifts off the line. */
private val WORD_RISE = 2.dp

/** Room above a row, in lifts, for a letter to swell into. */
private const val GROW_HEADROOM = 3f

private const val PHASE_BEFORE = 0
private const val PHASE_MOVING = 1
private const val PHASE_AFTER = 2

/**
 * One word synced line drawn the way BitChord draws it: a dim copy, a blurred copy that glows only
 * on the words being held, and a lit copy that a feathered sweep reveals. Words lift as they are
 * sung, and a held word swells letter by letter.
 *
 * [clock] is the song position in milliseconds. [dimAlpha] is how bright the unsung words are, and
 * 1 once the line is behind the sung one. [glowRoom] is the room the halo gets around the text, taken
 * back again so the line takes the space it did without it.
 */
@Composable
internal fun YosBitChordLine(
    line: YosBcLine,
    clock: IntState,
    style: TextStyle,
    dimAlpha: Float,
    modifier: Modifier = Modifier,
    glowAlpha: Float = 0f,
    glowRadius: Dp = 6.dp,
    glowRoom: Dp = 10.dp,
    feather: Boolean = false,
) {
    var layout by remember(line) { mutableStateOf<TextLayoutResult?>(null) }

    // One letter in hand at a time, shared by the three copies.
    val growth = remember { YosBcCharGrowth() }

    // Where the line is in its own animation: not started, moving or done. The draw passes read the
    // clock only while it is moving, so a line nobody is singing never redraws.
    val phase = remember(line, clock) {
        derivedStateOf(structuralEqualityPolicy()) {
            val now = clock.intValue.toLong()
            when {
                now < line.animatesFromMs -> PHASE_BEFORE
                now > line.animatesUntilMs -> PHASE_AFTER
                else -> PHASE_MOVING
            }
        }
    }
    val drawnAt: () -> Long = {
        when (phase.value) {
            PHASE_MOVING -> clock.intValue.toLong()
            PHASE_BEFORE -> line.animatesFromMs - 1
            else -> line.animatesUntilMs + 1
        }
    }

    // Carried by every copy, so they lay out identically. The inset gives the blurred copy's layer
    // somewhere to put the halo.
    val room = if (glowRoom > 0.dp) Modifier.padding(glowRoom) else Modifier

    // Outside the room and the sweep, so what it moves is the finished picture of the word.
    val riseAgainst: (Modifier) -> Modifier = { inner ->
        Modifier
            .drawWithContent {
                val measured = layout
                if (measured == null || line.words.isEmpty()) {
                    drawContent()
                } else {
                    riseWith(
                        layout = measured,
                        line = line,
                        positionMs = drawnAt(),
                        inset = glowRoom.toPx(),
                        peak = WORD_RISE.toPx(),
                        growth = growth,
                    )
                }
            }
            .then(inner)
    }

    val sweep = Modifier.drawWithContent {
        val position = drawnAt()
        when {
            // Sung and done with: all of it is lit.
            position >= line.endMs -> drawContent()
            // Not started: nothing lit, the dim copy is the whole of it.
            position <= line.timeMs -> Unit
            else -> layout?.let {
                sweepTo(it, line.revealedChars(position), line.sweepSpans, feather)
            }
        }
    }

    Box(
        modifier.bleed(glowRoom),
        contentAlignment = Alignment.TopStart,
    ) {
        Text(
            text = line.text,
            style = style,
            color = Color.White.copy(alpha = dimAlpha),
            onTextLayout = { layout = it },
            modifier = riseAgainst(room),
        )
        if (glowAlpha > 0.01f) {
            Text(
                text = line.text,
                style = style,
                color = Color.White,
                modifier = Modifier
                    .graphicsLayer { alpha = glowAlpha }
                    .blur(glowRadius, BlurredEdgeTreatment.Unbounded)
                    .then(room)
                    // Each letter is masked to its own brightness with DstIn, which needs a layer of
                    // its own to erase into.
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        // Lights only the words being held, not everything sung so far.
                        val measured = layout ?: return@drawWithContent
                        glowGrown(
                            layout = measured,
                            line = line,
                            positionMs = drawnAt(),
                            inset = glowRoom.toPx(),
                            peak = WORD_RISE.toPx(),
                            growth = growth,
                        )
                    },
            )
        }
        Text(
            text = line.text,
            style = style,
            color = Color.White,
            // The feather erases into this layer, so it exists only while it is being drawn.
            modifier = riseAgainst(
                Modifier
                    .graphicsLayer {
                        compositingStrategy = if (feather) {
                            CompositingStrategy.Offscreen
                        } else {
                            CompositingStrategy.Auto
                        }
                    }
                    .then(room)
                    .then(sweep),
            ),
        )
    }
}

/**
 * Lets a box draw [room] outside its own bounds. The box is measured that much larger on every
 * side, so its text wraps where it would without the room, and then reports its size without it.
 */
private fun Modifier.bleed(room: Dp): Modifier = layout { measurable, constraints ->
    val inset = room.roundToPx()
    val placeable = measurable.measure(constraints.offset(horizontal = inset * 2, vertical = inset * 2))
    layout(
        (placeable.width - inset * 2).coerceAtLeast(0),
        (placeable.height - inset * 2).coerceAtLeast(0),
    ) {
        placeable.place(-inset, -inset)
    }
}

/**
 * Draws this text clipped to the letters of the words being held, each at its own brightness.
 * Nothing at all on a line of ordinary syllables.
 */
private fun ContentDrawScope.glowGrown(
    layout: TextLayoutResult,
    line: YosBcLine,
    positionMs: Long,
    inset: Float,
    peak: Float,
    growth: YosBcCharGrowth,
) {
    if (!line.isGrowing(positionMs)) return
    val em = layout.layoutInput.style.fontSize.toPx()
    val length = layout.layoutInput.text.length
    for (word in line.growingWords) {
        if (positionMs < word.startMs || positionMs > word.restsAtMs) continue
        val span = line.wordSpans[word.index]
        val fall = line.wordFall(word.index, positionMs)
        for (char in span.first..minOf(span.last, length - 1)) {
            word.sampleInto(char - span.first, positionMs, growth)
            if (growth.bloom <= 0.01f) continue
            val visualLine = layout.getLineForOffset(char)
            val from = layout.xOn(char, visualLine, inset)
            val to = layout.xOn(char + 1, visualLine, inset)
            if (to <= from) continue
            val dx = growth.shift * em
            val dy = -growth.rise * peak * fall
            val rowTop = layout.getLineTop(visualLine) + inset
            val bottom = layout.getLineBottom(visualLine) + inset
            val overhang = (to - from) * (growth.scale - 1f) / 2f
            clipRect(
                left = from - overhang + dx,
                top = rowTop - peak * GROW_HEADROOM,
                right = to + overhang + dx,
                bottom = bottom,
            ) {
                translate(left = dx, top = dy) {
                    scale(
                        growth.scale,
                        growth.scale,
                        Offset((from + to) / 2f, (rowTop + bottom) / 2f),
                    ) {
                        this@glowGrown.drawContent()
                    }
                }
                // Scoped to this letter's own clip, so it takes this letter's brightness down and
                // leaves its neighbours where they are.
                drawRect(
                    color = Color.White.copy(alpha = growth.bloom),
                    blendMode = BlendMode.DstIn,
                )
            }
        }
    }
}

/**
 * Redraws this row with the word being sung lifted off the line, and the ones behind it settling
 * back down. The line is cut at word boundaries and each piece replayed at its own height. Costs
 * nothing until something is off the floor.
 */
private fun ContentDrawScope.riseWith(
    layout: TextLayoutResult,
    line: YosBcLine,
    positionMs: Long,
    inset: Float,
    peak: Float,
    growth: YosBcCharGrowth,
) {
    if (!line.isLifted(positionMs)) {
        drawContent()
        return
    }
    val em = layout.layoutInput.style.fontSize.toPx()
    for (visualLine in 0 until layout.lineCount) {
        val lineStart = layout.getLineStart(visualLine)
        val lineEnd = layout.getLineEnd(visualLine, visibleEnd = true)
        // The row's own box. Anything standing still is clipped to exactly this, so no row draws a
        // sliver of the one above it a second time.
        val top = layout.getLineTop(visualLine) + inset
        val bottom = layout.getLineBottom(visualLine) + inset
        var at = lineStart
        var edge = layout.getLineLeft(visualLine) + inset
        for (index in line.words.indices) {
            val span = line.wordSpans[index]
            val start = maxOf(span.first, lineStart)
            val end = minOf(span.last + 1, lineEnd)
            if (start >= end) continue
            // Only while it is actually moving. Once the last letter has come to rest the cheaper
            // single slice takes over without a step.
            val held = line.growingAt(index)?.takeIf { positionMs in it.startMs..it.restsAtMs }
            val lift = line.wordLift(index, positionMs)
            // A word with nothing happening to it is left to the flat run.
            if (held == null && lift <= 0.01f) continue
            val from = layout.xOn(start, visualLine, inset)
            val to = layout.xOn(end, visualLine, inset)
            if (to <= from) continue
            // Everything between the last risen word and this one is flat.
            if (start > at) sliceRisen(edge, top, from, bottom, 0f)
            if (held != null) {
                growEach(
                    layout, held, line, positionMs, visualLine,
                    start, end, top, bottom, inset, peak, em, growth,
                )
            } else {
                // Only what is off the floor gets room above the row to be off it in.
                sliceRisen(from, top - peak, to, bottom, -lift * peak)
            }
            at = end
            edge = to
        }
        if (at < lineEnd) {
            sliceRisen(edge, top, layout.getLineRight(visualLine) + inset, bottom, 0f)
        }
    }
}

/**
 * Redraws one held word a letter at a time, each at its own swell and height. Each piece is clipped
 * to where its letter is going rather than where it sits, so the swell is not shaved off.
 */
@Suppress("LongParameterList")
private fun ContentDrawScope.growEach(
    layout: TextLayoutResult,
    word: rotatsu.yos.music.player.code.utils.lrc.YosBcGrowingWord,
    line: YosBcLine,
    positionMs: Long,
    visualLine: Int,
    start: Int,
    end: Int,
    top: Float,
    bottom: Float,
    inset: Float,
    peak: Float,
    em: Float,
    growth: YosBcCharGrowth,
) {
    // The settle is shared with every other word.
    val fall = line.wordFall(word.index, positionMs)
    val first = line.wordSpans[word.index].first
    // Room to swell into, above the row. The pivot stays on the row's own middle.
    val ceiling = top - peak * GROW_HEADROOM
    val middle = (top + bottom) / 2f
    for (char in start until end) {
        word.sampleInto(char - first, positionMs, growth)
        val from = layout.xOn(char, visualLine, inset)
        val to = layout.xOn(char + 1, visualLine, inset)
        if (to <= from) continue
        val dx = growth.shift * em
        val dy = -growth.rise * peak * fall
        val overhang = (to - from) * (growth.scale - 1f) / 2f
        clipShiftedDown(
            left = from - overhang + dx,
            top = ceiling,
            right = to + overhang + dx,
            bottom = bottom,
            dy = dy,
        ) {
            translate(left = dx) {
                scale(growth.scale, growth.scale, Offset((from + to) / 2f, middle)) {
                    this@growEach.drawContent()
                }
            }
        }
    }
}

/**
 * Where an offset sits horizontally on the row it was cut out of. Both ends of a row are answered
 * with the row's own edges, so a word that runs up to a wrap does not land on the next row.
 */
private fun TextLayoutResult.xOn(offset: Int, visualLine: Int, inset: Float): Float {
    val left = getLineLeft(visualLine) + inset
    val right = getLineRight(visualLine) + inset
    return when {
        offset <= getLineStart(visualLine) -> left
        offset >= getLineEnd(visualLine, visibleEnd = true) -> right
        else -> (getHorizontalPosition(offset, usePrimaryDirection = true) + inset)
            .coerceIn(left, right)
    }
}

/** One piece of a line, clipped to its own width and drawn at its own height. */
private fun ContentDrawScope.sliceRisen(
    from: Float,
    top: Float,
    to: Float,
    bottom: Float,
    dy: Float,
) {
    if (to <= from) return
    clipShiftedDown(left = from, top = top, right = to, bottom = bottom, dy = dy) {
        this@sliceRisen.drawContent()
    }
}

/**
 * Where a fractional character index sits across a visual line, in pixels. Inside a timed piece
 * that sits whole on this row, the index is a share of the piece laid across its width, so the edge
 * crosses every letter of a syllable at one steady rate.
 */
private fun horizontalAt(
    layout: TextLayoutResult,
    chars: Float,
    visualLine: Int,
    spans: List<IntRange>,
): Float {
    val lineStart = layout.getLineStart(visualLine)
    val lineEnd = layout.getLineEnd(visualLine, visibleEnd = true)
    val word = spans.firstOrNull { chars >= it.first && chars < it.last + 1 }
    if (word != null && word.first >= lineStart && word.last + 1 <= lineEnd) {
        val from = layout.xOn(word.first, visualLine, 0f)
        val to = layout.xOn(word.last + 1, visualLine, 0f)
        return from + (to - from) * (chars - word.first) / (word.last + 1 - word.first)
    }
    // A word broken over a wrap, or the space between two words: letter by letter.
    val index = chars.toInt().coerceIn(lineStart, lineEnd)
    // Row-aware at both ends, so the edge does not jump back a letter before every wrap.
    val here = layout.xOn(index, visualLine, 0f)
    val next = layout.xOn((index + 1).coerceAtMost(lineEnd), visualLine, 0f)
    return here + (next - here) * (chars - index)
}

/**
 * Draws this text clipped to its first [revealedChars] characters, a visual line at a time. The
 * boundary is feathered over [WIPE_FEATHER], which needs the caller to give this an offscreen layer
 * to erase into. Only the line being sung carries one.
 */
private fun ContentDrawScope.sweepTo(
    layout: TextLayoutResult,
    revealedChars: Float,
    spans: List<IntRange>,
    feather: Boolean,
) {
    if (revealedChars <= 0f) return
    if (revealedChars >= layout.layoutInput.text.length) {
        drawContent()
        return
    }
    for (visualLine in 0 until layout.lineCount) {
        val start = layout.getLineStart(visualLine)
        // Lines beyond the boundary have nothing lit on them, and neither has anything after them.
        if (revealedChars <= start) return
        val end = layout.getLineEnd(visualLine, visibleEnd = true)
        val cut = revealedChars < end
        val right = if (cut) {
            horizontalAt(layout, revealedChars, visualLine, spans)
        } else {
            layout.getLineRight(visualLine)
        }
        val top = layout.getLineTop(visualLine)
        val bottom = layout.getLineBottom(visualLine)
        clipRect(
            left = layout.getLineLeft(visualLine),
            top = top,
            right = right,
            bottom = bottom,
        ) {
            this@sweepTo.drawContent()
        }
        // Only the visual line holding the boundary has an edge to soften.
        if (!feather || !cut) continue
        // Scoped to this line's band so the mask cannot reach the lines above and below it.
        clipRect(top = top, bottom = bottom) {
            drawRect(
                brush = Brush.horizontalGradient(
                    0f to Color.White,
                    1f to Color.Transparent,
                    startX = (right - WIPE_FEATHER.toPx())
                        .coerceAtLeast(layout.getLineLeft(visualLine)),
                    endX = right,
                ),
                blendMode = BlendMode.DstIn,
            )
        }
    }
}

/** Draws [block] clipped to the rectangle given, moved down by [dy] pixels. */
private fun DrawScope.clipShiftedDown(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    dy: Float,
    block: DrawScope.() -> Unit,
) {
    clipRect(left = left, top = top, right = right, bottom = bottom) {
        translate(top = dy) { block() }
    }
}
