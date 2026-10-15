package rotatsu.yos.music.player.ui.widgets

// The line renderer of Apple Music-like Lyrics (AMLL), https://github.com/amll-dev/applemusic-like-lyrics (AGPL-3.0),
// Copyright the AMLL authors. Ported from packages/core/src/lyric-player/dom: the dim line and the bright line behind a
// soft edge mask, every word floating up as it is sung, and the swell and glow of long words letter by letter.
// Changed: it draws from a Pear text layout, one word piece at a time, for text read left to right.

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.IntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import rotatsu.yos.music.player.code.utils.lrc.YosAmllCharEffect
import rotatsu.yos.music.player.code.utils.lrc.YosAmllChunk
import rotatsu.yos.music.player.code.utils.lrc.YosAmllEffects
import rotatsu.yos.music.player.code.utils.lrc.YosAmllLine
import rotatsu.yos.music.player.code.utils.lrc.YosAmllSweep
import rotatsu.yos.music.player.code.utils.lrc.YosAmllWord

/** How long a line takes to go from dim to lit, and back. AMLL's brightness transition is 0.3 s. */
private const val LIVE_MS = 300

/**
 * One word synced line drawn the way AMLL draws it. Every line is dim, at [inactiveAlpha]. The sung line is at
 * [activeAlpha] and its words are lit by an edge that crosses each in its own time, the words not reached yet at
 * [unsungAlpha] of the lit ones. [mode] says where the line is in the song; a line behind or ahead of the sung one
 * is dim, and a line that has just been left fades back to dim.
 */
@Composable
internal fun YosAmllLine(
    line: YosAmllLine,
    clock: IntState,
    mode: Int,
    style: TextStyle,
    inactiveAlpha: Float,
    activeAlpha: Float,
    unsungAlpha: Float,
    modifier: Modifier = Modifier,
    glowRoom: Dp = 12.dp,
) {
    val animated = remember(style) { style.copy(textMotion = TextMotion.Animated) }
    val measurer = rememberTextMeasurer()
    var layout by remember(line) { mutableStateOf<TextLayoutResult?>(null) }
    val rows = remember(line, layout) { layout?.let { buildAmllRows(line, it) } }
    // The letters that swell are drawn from layouts of their own, so nothing cuts their glow at a neighbour.
    val letters = remember(animated) { HashMap<String, TextLayoutResult>() }
    val live = animateFloatAsState(
        targetValue = if (mode == AT_CURRENT) 1f else 0f,
        animationSpec = tween(durationMillis = LIVE_MS, easing = FastOutSlowInEasing),
        label = "amllLive",
    )

    val room = if (glowRoom > 0.dp) Modifier.padding(glowRoom) else Modifier

    Box(modifier.bleed(glowRoom), contentAlignment = Alignment.TopStart) {
        Text(
            text = line.text,
            style = animated,
            color = Color.White,
            onTextLayout = { layout = it },
            modifier = Modifier
                // The edge erases into this layer, so it needs one of its own while it is there.
                .graphicsLayer {
                    val lit = live.value
                    compositingStrategy = if (mode == AT_CURRENT || lit > 0.001f) {
                        CompositingStrategy.Offscreen
                    } else {
                        CompositingStrategy.Auto
                    }
                    alpha = inactiveAlpha + (activeAlpha - inactiveAlpha) * lit
                }
                .drawWithContent {
                    val measured = layout
                    val prepared = rows
                    val lit = live.value
                    if (measured == null || prepared == null || (mode != AT_CURRENT && lit <= 0.001f)) {
                        drawContent()
                        return@drawWithContent
                    }
                    val current = mode == AT_CURRENT
                    val time = when (mode) {
                        AT_CURRENT -> clock.intValue.toFloat()
                        AT_SUNG -> Float.MAX_VALUE
                        else -> -Float.MAX_VALUE
                    }
                    translate(left = glowRoom.toPx(), top = glowRoom.toPx()) {
                        drawAmllRows(measured, prepared, time, current, lit, unsungAlpha, letters, measurer, animated)
                    }
                }
                .then(room),
        )
    }
}

/** A word, or the part of it on one visual row. [from] and [to] are text indices. */
private class AmllPiece(
    val index: Int,
    val word: YosAmllWord,
    val chunk: YosAmllChunk,
    val from: Int,
    val to: Int,
    var boundLeft: Float,
    var boundRight: Float,
)

private class AmllRow(val top: Float, val bottom: Float, val baseline: Float, val pieces: List<AmllPiece>)

private class AmllRows(val rows: List<AmllRow>, val sweep: YosAmllSweep, val pieceCount: Int, val fade: Float)

private class AmllRecord(
    val visual: Int,
    val word: YosAmllWord,
    val from: Int,
    val to: Int,
    val left: Float,
    val right: Float,
    val start: Float,
    val end: Float,
)

private fun buildAmllRows(line: YosAmllLine, layout: TextLayoutResult): AmllRows {
    val length = layout.layoutInput.text.length
    val records = ArrayList<AmllRecord>()
    for (word in line.words) {
        if (word.charStart >= length) continue
        val total = (word.charEnd - word.charStart).coerceAtLeast(1)
        var from = word.charStart
        while (from < word.charEnd && from < length) {
            val visual = layout.getLineForOffset(from)
            var to = from + 1
            while (to < word.charEnd && to < length && layout.getLineForOffset(to) == visual) to++
            // A word that wraps is crossed in two parts, the time shared by letters.
            val start = word.start + (word.end - word.start) * (from - word.charStart) / total
            val end = word.start + (word.end - word.start) * (to - word.charStart) / total
            records += AmllRecord(
                visual, word, from, to,
                layout.getBoundingBox(from).left, layout.getBoundingBox(to - 1).right, start, end,
            )
            from = to
        }
    }

    val height = if (layout.lineCount > 0) layout.getLineBottom(0) - layout.getLineTop(0) else 0f
    val fade = (YosAmllEffects.FADE_OF_HEIGHT * height).coerceAtLeast(1f)
    val pieces = records.mapIndexed { index, r ->
        AmllPiece(index, r.word, line.chunks[r.word.chunk], r.from, r.to, r.left, r.right)
    }
    val rows = ArrayList<AmllRow>()
    for (visual in 0 until layout.lineCount) {
        val onRow = pieces.filter { records[it.index].visual == visual }.sortedBy { records[it.index].left }
        if (onRow.isEmpty()) continue
        val rowHeight = layout.getLineBottom(visual) - layout.getLineTop(visual)
        for ((i, piece) in onRow.withIndex()) {
            val r = records[piece.index]
            piece.boundLeft = if (i == 0) layout.getLineLeft(visual) - rowHeight else (records[onRow[i - 1].index].right + r.left) / 2f
            piece.boundRight = if (i == onRow.lastIndex) {
                layout.getLineRight(visual) + rowHeight
            } else {
                (r.right + records[onRow[i + 1].index].left) / 2f
            }
        }
        rows += AmllRow(layout.getLineTop(visual), layout.getLineBottom(visual), layout.getLineBaseline(visual), onRow)
    }
    val sweep = YosAmllSweep(
        FloatArray(records.size) { records[it].left },
        FloatArray(records.size) { (records[it].right - records[it].left).coerceAtLeast(0f) },
        FloatArray(records.size) { records[it].start },
        FloatArray(records.size) { records[it].end },
        fade,
    )
    return AmllRows(rows, sweep, records.size, fade)
}

private fun DrawScope.drawAmllRows(
    layout: TextLayoutResult,
    rows: AmllRows,
    time: Float,
    current: Boolean,
    live: Float,
    unsungAlpha: Float,
    letters: HashMap<String, TextLayoutResult>,
    measurer: TextMeasurer,
    style: TextStyle,
) {
    val size = layout.layoutInput.style.fontSize
    val fontPx = (if (size.isSpecified) size else 16.sp).toPx()
    val centers = FloatArray(rows.pieceCount)
    rows.sweep.centers(time, centers)
    // As the line goes dim again the unsung share goes back to 1, so the whole line settles on one brightness.
    val dark = 1f + (unsungAlpha - 1f) * live
    val effect = YosAmllCharEffect()

    for (row in rows.rows) {
        for (piece in row.pieces) {
            if (current && piece.chunk.emphasized) {
                drawLetters(layout, row, piece, time, fontPx, effect, letters, measurer, style)
            } else {
                val lift = if (current) YosAmllEffects.wordFloat(piece.word.start, piece.word.end, time) * fontPx else 0f
                translate(top = lift) {
                    clipRect(piece.boundLeft, row.top, piece.boundRight, row.bottom) {
                        drawText(layout, Color.White)
                    }
                }
            }
        }
        // The edge. Cut to the row, so it never dims the row above.
        for (piece in row.pieces) {
            val center = centers[piece.index]
            clipRect(piece.boundLeft, row.top, piece.boundRight, row.bottom + 4f) {
                drawRect(
                    brush = Brush.horizontalGradient(
                        0f to Color.White,
                        1f to Color.White.copy(alpha = dark),
                        startX = center - rows.fade / 2f,
                        endX = center + rows.fade / 2f,
                    ),
                    blendMode = BlendMode.DstIn,
                )
            }
        }
    }
}

private fun DrawScope.drawLetters(
    layout: TextLayoutResult,
    row: AmllRow,
    piece: AmllPiece,
    time: Float,
    fontPx: Float,
    effect: YosAmllCharEffect,
    letters: HashMap<String, TextLayoutResult>,
    measurer: TextMeasurer,
    style: TextStyle,
) {
    val chunk = piece.chunk
    val text = layout.layoutInput.text.text
    val wordLift = YosAmllEffects.wordFloat(piece.word.start, piece.word.end, time)
    val middle = (row.top + row.bottom) / 2f
    for (j in chunk.chars.indices) {
        val from = chunk.chars[j]
        if (from < piece.from || from >= piece.to) continue
        val floatEm = YosAmllEffects.charEffect(chunk, j, time, effect)
        val box = layout.getBoundingBox(from)
        val letter = text.substring(from, chunk.charEnds[j])
        val letterLayout = letters.getOrPut(letter) { measurer.measure(letter, style, softWrap = false) }
        val glow = if (effect.glowAlpha > 0.003f) {
            Shadow(Color.White.copy(alpha = effect.glowAlpha.coerceAtMost(1f)), Offset.Zero, effect.glowRadius * fontPx)
        } else {
            null
        }
        scale(effect.scale, effect.scale, Offset((box.left + box.right) / 2f, middle)) {
            translate(left = effect.offsetX * fontPx, top = (effect.offsetY + floatEm + wordLift) * fontPx) {
                drawText(
                    letterLayout,
                    color = Color.White,
                    topLeft = Offset(box.left, row.baseline - letterLayout.firstBaseline),
                    shadow = glow,
                )
            }
        }
    }
}
