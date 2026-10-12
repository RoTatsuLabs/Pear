package rotatsu.yos.music.player.ui.widgets.effects

// The colour mesh behind BitChord's player, ported from ArtworkMeshBackdrop.kt and ColorUtils.kt of
// https://github.com/kushagrasinghx/BitChord (GPL-3.0). The grid, the flip, the sideways rotation, the
// smoothing and the colour lift are BitChord's. Changed: it reads a plain pixel array instead of a
// Compose image, so it can be tested without a device.

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

/** How many cells across the mesh is. Coarse on purpose, so no one cell is recognisable. */
internal const val MESH_GRID = 6

/** The side of the texture the grid is smoothed up to before the GPU stretches it. */
internal const val MESH_TEX = 32

/** The most rows and columns of the cover that are looked at. */
private const val MESH_SAMPLE = 128

private const val MESH_VIBRANCE = 1.12f
private const val MESH_FLOOR = 0.045f

/**
 * Averages [pixels], a [width] by [height] square of ARGB ints, into a [MESH_GRID] square of means,
 * flips it top to bottom so row 0 is the cover's own bottom edge, turns every row below that one
 * sideways, and smooths the result up to a [MESH_TEX] square. Null when there is nothing to read.
 *
 * A mean per cell and not a swatch: what sits under the cover is what a blur of it would leave
 * there, so a cover that is mostly black stays mostly black.
 */
internal fun coverMesh(pixels: IntArray, width: Int, height: Int, seed: Int): IntArray? {
    if (width < 1 || height < 1 || pixels.size < width * height) return null
    val cols = MESH_GRID.coerceAtMost(width)
    val rows = MESH_GRID.coerceAtMost(height)
    val rowStep = (height / MESH_SAMPLE).coerceAtLeast(1)
    val colStep = (width / MESH_SAMPLE).coerceAtLeast(1)

    val cells = rows * cols
    val red = LongArray(cells)
    val green = LongArray(cells)
    val blue = LongArray(cells)
    val count = IntArray(cells)

    var y = 0
    while (y < height) {
        // Flipped as it is read.
        val rowBase = ((height - 1 - y) * rows / height) * cols
        var x = 0
        while (x < width) {
            val cell = rowBase + x * cols / width
            val pixel = pixels[y * width + x]
            red[cell] += ((pixel shr 16) and 0xFF).toLong()
            green[cell] += ((pixel shr 8) and 0xFF).toLong()
            blue[cell] += (pixel and 0xFF).toLong()
            count[cell]++
            x += colStep
        }
        y += rowStep
    }

    val grid = IntArray(cells) { cell ->
        val n = count[cell].coerceAtLeast(1)
        argb((red[cell] / n).toInt(), (green[cell] / n).toInt(), (blue[cell] / n).toInt()).lifted()
    }
    return grid.rotatedBelowSeam(cols, rows, seed).resampled(cols, rows, MESH_TEX)
}

/**
 * Every row but row 0 rotated sideways by one random amount, and mirrored half the time. A flip alone
 * reads as a reflection on a cover with a face or a horizon in it. A cyclic shift keeps every cell's
 * neighbours exactly as they were, so the gradient between them survives. Seeded, so the same cover
 * always lands the same way.
 */
internal fun IntArray.rotatedBelowSeam(cols: Int, rows: Int, seed: Int): IntArray {
    if (rows <= 1) return this
    val random = Random(seed)
    val mirror = random.nextBoolean()
    val shift = random.nextInt(cols)
    val out = copyOf()
    for (row in 1 until rows) {
        val base = row * cols
        for (x in 0 until cols) {
            val src = if (mirror) cols - 1 - x else x
            out[base + x] = this[base + (src + shift) % cols]
        }
    }
    return out
}

/** Smoothly resamples a [cols] by [rows] grid up to a [size] square, flat at every control point. */
internal fun IntArray.resampled(cols: Int, rows: Int, size: Int): IntArray {
    val out = IntArray(size * size)
    for (ty in 0 until size) {
        val fy = (ty + 0.5f) / size * rows - 0.5f
        val y0 = floor(fy).toInt().coerceIn(0, rows - 1)
        val y1 = (y0 + 1).coerceAtMost(rows - 1)
        val wy = smoothstep(fy - y0)
        for (tx in 0 until size) {
            val fx = (tx + 0.5f) / size * cols - 0.5f
            val x0 = floor(fx).toInt().coerceIn(0, cols - 1)
            val x1 = (x0 + 1).coerceAtMost(cols - 1)
            val wx = smoothstep(fx - x0)
            val top = lerpArgb(this[y0 * cols + x0], this[y0 * cols + x1], wx)
            val bottom = lerpArgb(this[y1 * cols + x0], this[y1 * cols + x1], wx)
            out[ty * size + tx] = lerpArgb(top, bottom, wy)
        }
    }
    return out
}

private fun smoothstep(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    return x * x * (3f - 2f * x)
}

private fun lerpArgb(from: Int, to: Int, t: Float): Int {
    if (t <= 0f) return from
    if (t >= 1f) return to
    fun channel(shift: Int): Int {
        val a = (from shr shift) and 0xFF
        val b = (to shr shift) and 0xFF
        return (a + ((b - a) * t)).roundToInt().coerceIn(0, 255)
    }
    return argb(channel(16), channel(8), channel(0))
}

private fun argb(red: Int, green: Int, blue: Int): Int =
    (0xFF shl 24) or (red shl 16) or (green shl 8) or blue

/**
 * The one liberty taken with the cover's colours. Averaging a block greys it, so saturation is nudged
 * back by about what was lost, and lightness is only floored off pure black. Neither touches the
 * proportions.
 */
private fun Int.lifted(): Int {
    val hsl = FloatArray(3)
    rgbToHsl((this shr 16) and 0xFF, (this shr 8) and 0xFF, this and 0xFF, hsl)
    hsl[1] = (hsl[1] * MESH_VIBRANCE).coerceAtMost(1f)
    hsl[2] = hsl[2].coerceAtLeast(MESH_FLOOR)
    return hslToColor(hsl)
}

private fun rgbToHsl(rf: Int, gf: Int, bf: Int, out: FloatArray) {
    val r = rf / 255f
    val g = gf / 255f
    val b = bf / 255f
    val max = max(r, max(g, b))
    val min = min(r, min(g, b))
    val delta = max - min
    var h: Float
    val s: Float
    val l = (max + min) / 2f
    if (max == min) {
        h = 0f
        s = 0f
    } else {
        h = when (max) {
            r -> ((g - b) / delta) % 6f
            g -> ((b - r) / delta) + 2f
            else -> ((r - g) / delta) + 4f
        }
        s = delta / (1f - abs(2f * l - 1f))
    }
    h = (h * 60f) % 360f
    if (h < 0) h += 360f
    out[0] = h.coerceIn(0f, 360f)
    out[1] = s.coerceIn(0f, 1f)
    out[2] = l.coerceIn(0f, 1f)
}

private fun hslToColor(hsl: FloatArray): Int {
    val h = hsl[0]
    val s = hsl[1]
    val l = hsl[2]
    val c = (1f - abs(2 * l - 1f)) * s
    val m = l - 0.5f * c
    val x = c * (1f - abs((h / 60f % 2f) - 1f))
    var r = 0
    var g = 0
    var b = 0
    when (h.toInt() / 60) {
        0 -> { r = (255 * (c + m)).roundToInt(); g = (255 * (x + m)).roundToInt(); b = (255 * m).roundToInt() }
        1 -> { r = (255 * (x + m)).roundToInt(); g = (255 * (c + m)).roundToInt(); b = (255 * m).roundToInt() }
        2 -> { r = (255 * m).roundToInt(); g = (255 * (c + m)).roundToInt(); b = (255 * (x + m)).roundToInt() }
        3 -> { r = (255 * m).roundToInt(); g = (255 * (x + m)).roundToInt(); b = (255 * (c + m)).roundToInt() }
        4 -> { r = (255 * (x + m)).roundToInt(); g = (255 * m).roundToInt(); b = (255 * (c + m)).roundToInt() }
        5, 6 -> { r = (255 * (c + m)).roundToInt(); g = (255 * m).roundToInt(); b = (255 * (x + m)).roundToInt() }
    }
    return argb(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
}
