package rotatsu.yos.music.player.ui.widgets

// The line renderer of ArchiveTune's lyric view, which draws its lines with Accompanist Lyrics UI,
// https://github.com/6xingyv/accompanist-lyrics-ui (Apache License 2.0, Copyright 2025 6xingyv).
// Ported from its LyricsRowRenderer, RowRenderState and RowPaints. Changed: it draws from a Pear text
// layout instead of prepared text units, one visual row at a time, for text read left to right.

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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import rotatsu.yos.music.player.code.utils.lrc.YosAtEffects
import rotatsu.yos.music.player.code.utils.lrc.YosAtGroup
import rotatsu.yos.music.player.code.utils.lrc.YosAtLine
import rotatsu.yos.music.player.code.utils.lrc.YosAtSweep

/** The line is being sung, is behind the sung one, or has not been reached. */
internal const val AT_CURRENT = 0
internal const val AT_SUNG = 1
internal const val AT_UPCOMING = 2

/** The glow of every step, made once. */
private val GlowShadows = Array(YosAtEffects.GLOW_STEPS + 1) { index ->
    val strength = index / YosAtEffects.GLOW_STEPS.toFloat()
    Shadow(Color.White.copy(alpha = 0.4f * strength), Offset.Zero, 10f * strength)
}

/**
 * One word synced line drawn the way ArchiveTune draws it: every word starts a little low and
 * rises as it is sung, long slow words swell and glow letter by letter, and a soft edge sweeps the
 * row from dim to lit. [mode] says where the line is in the song. A line behind the sung one is
 * lit and one ahead of it is dim, which is what freezing the clock at either end does.
 */
@Composable
internal fun YosArchiveTuneLine(
    line: YosAtLine,
    clock: IntState,
    mode: Int,
    style: TextStyle,
    modifier: Modifier = Modifier,
    glowRoom: Dp = 12.dp,
) {
    val animated = remember(style) { style.copy(textMotion = TextMotion.Animated) }
    val measurer = rememberTextMeasurer()
    // The fade of the sweep is as wide as the letter M of the line's own style.
    val fadeWidth = remember(animated) {
        measurer.measure("M", animated, softWrap = false).size.width.toFloat()
            .takeIf { it.isFinite() && it > 0f } ?: 0.001f
    }
    var layout by remember(line) { mutableStateOf<TextLayoutResult?>(null) }
    val rows = remember(line, layout, fadeWidth) { layout?.let { buildRows(line, it, fadeWidth) } }

    val room = if (glowRoom > 0.dp) Modifier.padding(glowRoom) else Modifier

    Box(modifier.bleed(glowRoom), contentAlignment = Alignment.TopStart) {
        Text(
            text = line.text,
            style = animated,
            color = Color.White,
            onTextLayout = { layout = it },
            modifier = Modifier
                // The sweep erases into this layer, so it needs one of its own while it moves.
                .graphicsLayer {
                    compositingStrategy = if (mode == AT_CURRENT) {
                        CompositingStrategy.Offscreen
                    } else {
                        CompositingStrategy.Auto
                    }
                }
                .drawWithContent {
                    val measured = layout
                    val prepared = rows
                    if (measured == null || prepared == null) {
                        drawContent()
                        return@drawWithContent
                    }
                    val time = when (mode) {
                        AT_CURRENT -> clock.intValue
                        AT_SUNG -> Int.MAX_VALUE
                        else -> Int.MIN_VALUE
                    }
                    translate(left = glowRoom.toPx(), top = glowRoom.toPx()) {
                        for (row in prepared) drawRow(measured, row, time, fadeWidth)
                    }
                }
                .then(room),
        )
    }
}

/** What the text of one visual row is made of, found once for the layout. */
private class AtRow(
    val top: Float,
    val bottom: Float,
    val start: Int,
    val sweepEnd: Int,
    val slices: List<AtSlice>,
    val sweep: YosAtSweep,
)

/** The units of one group that sit on this row. [from] and [to] are indices into the group. */
private class AtSlice(val group: YosAtGroup, val from: Int, val to: Int, val left: Float, val right: Float)

private fun buildRows(line: YosAtLine, layout: TextLayoutResult, fadeWidth: Float): List<AtRow> {
    val length = layout.layoutInput.text.length
    val rows = ArrayList<AtRow>(layout.lineCount)
    for (visual in 0 until layout.lineCount) {
        val slices = ArrayList<AtSlice>()
        val starts = ArrayList<Int>()
        val ends = ArrayList<Int>()
        val boxes = ArrayList<Pair<Float, Float>>()
        var start = Int.MAX_VALUE
        var sweepEnd = Int.MIN_VALUE
        for (group in line.groups) {
            var from = -1
            var to = -1
            for ((index, unit) in group.units.withIndex()) {
                if (unit.char >= length || layout.getLineForOffset(unit.char) != visual) continue
                if (from < 0) from = index
                to = index
                val box = layout.getBoundingBox(unit.char)
                starts += unit.start
                ends += unit.end
                boxes += box.left to box.right
                start = minOf(start, unit.start, unit.animStart)
                sweepEnd = maxOf(sweepEnd, unit.end)
            }
            if (from < 0) continue
            slices += AtSlice(
                group,
                from,
                to + 1,
                layout.getBoundingBox(group.units[from].char).left,
                layout.getBoundingBox(group.units[to].char).right,
            )
        }
        if (slices.isEmpty()) continue
        val order = starts.indices.sortedBy { starts[it] }
        rows += AtRow(
            top = layout.getLineTop(visual),
            bottom = layout.getLineBottom(visual),
            start = start,
            sweepEnd = sweepEnd,
            slices = slices,
            sweep = YosAtSweep(
                IntArray(order.size) { starts[order[it]] },
                IntArray(order.size) { ends[order[it]] },
                FloatArray(order.size) { boxes[order[it]].first },
                FloatArray(order.size) { boxes[order[it]].second },
                fadeWidth,
                layout.getLineLeft(visual),
            ),
        )
    }
    return rows
}

private fun DrawScope.drawRow(layout: TextLayoutResult, row: AtRow, time: Int, fadeWidth: Float) {
    // A row that has not begun is dim, and one the sweep is crossing is masked.
    val inactive = time < row.start
    val color = Color.White.copy(alpha = if (inactive) YosAtEffects.UNLIT_ALPHA else 1f)
    val masked = !inactive && time < row.sweepEnd
    val text = layout.layoutInput.text.text

    for (slice in row.slices) {
        val group = slice.group
        val pivot = Offset((slice.left + slice.right) / 2f, row.bottom)
        val rests = time < group.sourceStart || time >= group.effectsEnd ||
            (!group.awesome && group.commonSource)
        if (rests) {
            val progress = ((time.toDouble() - group.sourceStart) / YosAtEffects.LIFT_MS)
                .toFloat().coerceIn(0f, 1f)
            val lift = if (time >= group.effectsEnd) {
                0f
            } else {
                YosAtEffects.LIFT_PX * (1f - progress) * (1f - progress)
            }
            drawChars(
                layout, row, group.units[slice.from].char, group.units[slice.to - 1].char + 1,
                lift, 1f, pivot, color, null,
            )
            continue
        }
        val groupStep = if (group.glow && group.glowAsGroup) {
            val progress = ((time.toDouble() - group.start) / group.animationDuration)
                .toFloat().coerceIn(0f, 1f)
            YosAtEffects.glowStep(progress)
        } else {
            0
        }
        for (index in slice.from until slice.to) {
            val unit = group.units[index]
            if (text[unit.char].isWhitespace()) continue
            val progress = ((time.toDouble() - group.unitStart(index)) / group.animationDuration)
                .toFloat().coerceIn(0f, 1f)
            val lift = YosAtEffects.lift(time, unit.animStart.toFloat())
            val scale = if (group.scale) 1f + group.swell.transform(progress) else 1f
            val step = when {
                !group.glow -> 0
                group.glowAsGroup -> groupStep
                else -> YosAtEffects.glowStep(progress)
            }
            drawChars(
                layout, row, unit.char, unit.char + 1, lift, scale, pivot, color,
                if (step > 0) GlowShadows[step] else null,
            )
        }
    }

    if (masked) {
        val center = row.sweep.center(time)
        val band = (row.bottom - row.top) / 2f
        clipRect(top = row.top - band, bottom = row.bottom + band) {
            drawRect(
                brush = Brush.horizontalGradient(
                    0f to Color.White,
                    1f to Color.White.copy(alpha = YosAtEffects.UNLIT_ALPHA),
                    startX = center - fadeWidth / 2f,
                    endX = center + fadeWidth / 2f,
                ),
                blendMode = BlendMode.DstIn,
            )
        }
    }
}

/**
 * Draws the characters from [from] up to [to] of the text on a row: cut out of the layout at their
 * own width, scaled about [pivot], and moved down by [lift]. The two ends of a row are cut wider so
 * a glow is not shaved off where nothing else is.
 */
private fun DrawScope.drawChars(
    layout: TextLayoutResult,
    row: AtRow,
    from: Int,
    to: Int,
    lift: Float,
    factor: Float,
    pivot: Offset,
    color: Color,
    shadow: Shadow?,
) {
    if (to <= from) return
    val height = row.bottom - row.top
    val left = layout.getBoundingBox(from).left
    val right = layout.getBoundingBox(to - 1).right
    if (right <= left) return
    val rowStart = layout.getLineStart(layout.getLineForOffset(from))
    val rowEnd = layout.getLineEnd(layout.getLineForOffset(from), visibleEnd = true)
    val clipLeft = if (from <= rowStart) left - height else left
    val clipRight = if (to >= rowEnd) right + height else right
    // The glow needs room above and below; plain text is cut to its own row.
    val bleed = if (shadow != null) 10f else 0f
    scale(factor, factor, pivot) {
        translate(top = lift) {
            clipRect(clipLeft, row.top - bleed, clipRight, row.bottom + bleed) {
                drawText(layout, color, shadow = shadow)
            }
        }
    }
}
