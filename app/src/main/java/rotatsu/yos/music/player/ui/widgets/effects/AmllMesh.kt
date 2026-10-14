package rotatsu.yos.music.player.ui.widgets.effects

// The mesh gradient background of Apple Music-like Lyrics (AMLL), ported from
// packages/core/src/bg-render of https://github.com/amll-dev/applemusic-like-lyrics (AGPL-3.0),
// Copyright the AMLL authors. Ported: the colour processing and blur of the cover, the control point
// presets and generator, the Hermite patches the grid is warped by, the rotating texture lookup with
// mirrored repeat, and the vignette. Changed: the mesh is drawn into a small pixel buffer on the CPU
// instead of with WebGL, and the buffer is stretched to the screen, which suits a gradient this soft.

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/** Control points of a mesh: [points] holds cx, cy, x, y, ur, vr, up, vp for each, in that order. */
internal class AmllPreset(val width: Int, val height: Int, val points: FloatArray)

/** The side of the texture the cover is reduced to. */
internal const val AMLL_TEXTURE = 32

/** Vertices computed per grid cell along each axis. AMLL uses 50 for a full screen canvas. */
internal const val AMLL_SUBDIVISIONS = 10

/** A grid of vertices, [cols] across and [rows] down, row by row, with positions from -1 to 1. */
internal class AmllMeshData(
    val cols: Int,
    val rows: Int,
    val x: FloatArray,
    val y: FloatArray,
    val u: FloatArray,
    val v: FloatArray
)

/** Colours a pixel from the texture coordinates the mesh puts there. */
internal fun interface AmllShader {
    fun shade(u: Float, v: Float): Int
}

/**
 * Prepares a [size] square of ARGB cover pixels as AMLL does: contrast down to 0.4, saturation up to
 * 3, contrast up to 1.7, brightness 0.75, then a box blur of radius 2 four times over.
 */
internal fun amllTexture(pixels: IntArray, size: Int = AMLL_TEXTURE): IntArray {
    val n = size * size
    val r = IntArray(n)
    val g = IntArray(n)
    val b = IntArray(n)
    for (i in 0 until n) {
        var red = ((pixels[i] shr 16) and 0xFF).toFloat()
        var green = ((pixels[i] shr 8) and 0xFF).toFloat()
        var blue = (pixels[i] and 0xFF).toFloat()

        red = (red - 128f) * 0.4f + 128f
        green = (green - 128f) * 0.4f + 128f
        blue = (blue - 128f) * 0.4f + 128f

        val gray = red * 0.3f + green * 0.59f + blue * 0.11f
        red = gray * -2f + red * 3f
        green = gray * -2f + green * 3f
        blue = gray * -2f + blue * 3f

        red = (red - 128f) * 1.7f + 128f
        green = (green - 128f) * 1.7f + 128f
        blue = (blue - 128f) * 1.7f + 128f

        r[i] = (red * 0.75f).roundToInt().coerceIn(0, 255)
        g[i] = (green * 0.75f).roundToInt().coerceIn(0, 255)
        b[i] = (blue * 0.75f).roundToInt().coerceIn(0, 255)
    }
    boxBlur(r, size, 2, 4)
    boxBlur(g, size, 2, 4)
    boxBlur(b, size, 2, 4)
    return IntArray(n) { (0xFF shl 24) or (r[it] shl 16) or (g[it] shl 8) or b[it] }
}

/** AMLL's blurImage: a box of [radius] either side, repeated [passes] times, edges held. */
private fun boxBlur(channel: IntArray, size: Int, radius: Int, passes: Int) {
    val sums = IntArray(size * size)
    val divisor = ((2 * radius + 1) * (2 * radius + 1)).toFloat()
    repeat(passes) {
        for (y in 0 until size) {
            for (x in 0 until size) {
                var sum = 0
                for (k in -radius..radius) sum += channel[y * size + (x + k).coerceIn(0, size - 1)]
                sums[y * size + x] = sum
            }
        }
        for (x in 0 until size) {
            for (y in 0 until size) {
                var sum = 0
                for (k in -radius..radius) sum += sums[(y + k).coerceIn(0, size - 1) * size + x]
                channel[y * size + x] = (sum / divisor + 0.5f).toInt()
            }
        }
    }
}

/** The average lightness of [texture] from 0 to 1. */
internal fun amllLightness(texture: IntArray): Float {
    var sum = 0f
    for (p in texture) {
        sum += 0.299f * ((p shr 16) and 0xFF) + 0.587f * ((p shr 8) and 0xFF) + 0.114f * (p and 0xFF)
    }
    return sum / (texture.size * 255f)
}

/**
 * Picks a mesh for a cover: one of the six presets, or one made up on the spot a fifth of the time, as
 * AMLL does. Seeded, so the same cover always gets the same shape.
 */
internal fun amllPick(random: Random): AmllPreset =
    if (random.nextFloat() > 0.8f) {
        amllGenerate(6, 6, random)
    } else {
        AMLL_PRESETS[random.nextInt(AMLL_PRESETS.size)]
    }

/**
 * Evaluates the Hermite patches between the control points of [preset]. The positions are warped by the
 * points and their tangents. The texture coordinates are a plain grid, so the picture is stretched with
 * the mesh and not moved across it.
 */
internal fun amllMesh(preset: AmllPreset, subdivisions: Int = AMLL_SUBDIVISIONS): AmllMeshData {
    val w = preset.width
    val h = preset.height
    val count = w * h
    val px = FloatArray(count)
    val py = FloatArray(count)
    val ux = FloatArray(count)
    val uy = FloatArray(count)
    val vx = FloatArray(count)
    val vy = FloatArray(count)
    val uPower = 2f / (w - 1)
    val vPower = 2f / (h - 1)
    for (k in 0 until count) {
        val base = k * 8
        val at = preset.points[base + 1].toInt() * w + preset.points[base].toInt()
        px[at] = preset.points[base + 2]
        py[at] = preset.points[base + 3]
        val uRot = Math.toRadians(preset.points[base + 4].toDouble())
        val vRot = Math.toRadians(preset.points[base + 5].toDouble())
        val uScale = uPower * preset.points[base + 6]
        val vScale = vPower * preset.points[base + 7]
        ux[at] = (cos(uRot) * uScale).toFloat()
        uy[at] = (sin(uRot) * uScale).toFloat()
        vx[at] = (-sin(vRot) * vScale).toFloat()
        vy[at] = (cos(vRot) * vScale).toFloat()
    }

    val sub = subdivisions
    val subM1 = (sub - 1).toFloat()
    val cols = (h - 1) * sub
    val rows = (w - 1) * sub
    val ox = FloatArray(cols * rows)
    val oy = FloatArray(cols * rows)
    val ou = FloatArray(cols * rows)
    val ov = FloatArray(cols * rows)

    for (cx in 0 until w - 1) {
        for (cy in 0 until h - 1) {
            val i00 = cy * w + cx
            val i01 = (cy + 1) * w + cx
            val i10 = cy * w + cx + 1
            val i11 = (cy + 1) * w + cx + 1
            val sX = cx.toFloat() / (w - 1)
            val sY = cy.toFloat() / (h - 1)
            for (u in 0 until sub) {
                val tu = u / subM1
                val a0 = hermite0(tu)
                val a1 = hermite1(tu)
                val a2 = hermite2(tu)
                val a3 = hermite3(tu)
                for (v in 0 until sub) {
                    val tv = v / subM1
                    val b0 = hermite0(tv)
                    val b1 = hermite1(tv)
                    val b2 = hermite2(tv)
                    val b3 = hermite3(tv)
                    // u runs along the control point rows, v along the columns. The tangent of a point
                    // across v is its u tangent, and across u its v tangent.
                    val x = b0 * (a0 * px[i00] + a1 * px[i01] + a2 * vx[i00] + a3 * vx[i01]) +
                        b1 * (a0 * px[i10] + a1 * px[i11] + a2 * vx[i10] + a3 * vx[i11]) +
                        b2 * (a0 * ux[i00] + a1 * ux[i01]) +
                        b3 * (a0 * ux[i10] + a1 * ux[i11])
                    val y = b0 * (a0 * py[i00] + a1 * py[i01] + a2 * vy[i00] + a3 * vy[i01]) +
                        b1 * (a0 * py[i10] + a1 * py[i11] + a2 * vy[i10] + a3 * vy[i11]) +
                        b2 * (a0 * uy[i00] + a1 * uy[i01]) +
                        b3 * (a0 * uy[i10] + a1 * uy[i11])
                    val gx = cy * sub + u
                    val gy = cx * sub + v
                    val at = gx + gy * cols
                    ox[at] = x
                    oy[at] = y
                    ou[at] = sX + v / (subM1 * (w - 1))
                    ov[at] = 1f - sY - u / (subM1 * (h - 1))
                }
            }
        }
    }
    return AmllMeshData(cols, rows, ox, oy, ou, ov)
}

private fun hermite0(t: Float) = 2 * t * t * t - 3 * t * t + 1
private fun hermite1(t: Float) = -2 * t * t * t + 3 * t * t
private fun hermite2(t: Float) = t * t * t - 2 * t * t + t
private fun hermite3(t: Float) = t * t * t - t * t

/**
 * Draws [mesh] into [out], an [outW] by [outH] buffer of ARGB ints, with [shader] colouring each pixel.
 * The mesh is a square as wide as the longer side of the buffer, centred, as AMLL fits it to the screen.
 * Pixels the mesh does not reach are left alone.
 */
internal fun amllRaster(mesh: AmllMeshData, outW: Int, outH: Int, out: IntArray, shader: AmllShader) {
    val side = max(outW, outH).toFloat()
    val count = mesh.cols * mesh.rows
    val sx = FloatArray(count) { outW / 2f + mesh.x[it] * side / 2f }
    // Up is positive in the mesh and down on the screen.
    val sy = FloatArray(count) { outH / 2f - mesh.y[it] * side / 2f }

    fun triangle(i0: Int, i1: Int, i2: Int) {
        val x0 = sx[i0]
        val y0 = sy[i0]
        val x1 = sx[i1]
        val y1 = sy[i1]
        val x2 = sx[i2]
        val y2 = sy[i2]
        val area = (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0)
        if (abs(area) < 1e-6f) return
        val left = max(0, floor(min(x0, min(x1, x2)) - 0.5f).toInt())
        val right = min(outW - 1, ceil(max(x0, max(x1, x2)) - 0.5f).toInt())
        val top = max(0, floor(min(y0, min(y1, y2)) - 0.5f).toInt())
        val bottom = min(outH - 1, ceil(max(y0, max(y1, y2)) - 0.5f).toInt())
        val eps = -1e-4f
        for (py in top..bottom) {
            val cy = py + 0.5f
            for (px in left..right) {
                val cx = px + 0.5f
                val w0 = ((x1 - cx) * (y2 - cy) - (x2 - cx) * (y1 - cy)) / area
                val w1 = ((x2 - cx) * (y0 - cy) - (x0 - cx) * (y2 - cy)) / area
                val w2 = 1f - w0 - w1
                if (w0 < eps || w1 < eps || w2 < eps) continue
                val u = w0 * mesh.u[i0] + w1 * mesh.u[i1] + w2 * mesh.u[i2]
                val v = w0 * mesh.v[i0] + w1 * mesh.v[i1] + w2 * mesh.v[i2]
                out[py * outW + px] = shader.shade(u, v)
            }
        }
    }

    for (gy in 0 until mesh.rows - 1) {
        for (gx in 0 until mesh.cols - 1) {
            val a = gy * mesh.cols + gx
            val b = a + 1
            val c = a + mesh.cols
            val d = c + 1
            triangle(a, b, c)
            triangle(b, d, c)
        }
    }
}

/**
 * The look of the background at [angle] radians. The texture turns about a point near its corner and
 * repeats mirrored past its edge. The mesh position darkens the corners, as AMLL's vignette does.
 */
internal class AmllTextureShader(private val texture: IntArray, private val size: Int, angle: Float) : AmllShader {
    private val sinAngle = sin(angle)
    private val cosAngle = cos(angle)

    override fun shade(u: Float, v: Float): Int {
        val cu = u - 0.2f
        val cv = v - 0.2f
        val fu = cosAngle * cu - sinAngle * cv + 0.5f
        val fv = sinAngle * cu + cosAngle * cv + 0.5f

        val tx = fu * size - 0.5f
        val ty = fv * size - 0.5f
        val x0 = floor(tx).toInt()
        val y0 = floor(ty).toInt()
        val fx = tx - x0
        val fy = ty - y0
        val c00 = texel(x0, y0)
        val c10 = texel(x0 + 1, y0)
        val c01 = texel(x0, y0 + 1)
        val c11 = texel(x0 + 1, y0 + 1)

        val dist = sqrt((u - 0.5f) * (u - 0.5f) + (v - 0.5f) * (v - 0.5f))
        val t = ((dist - 0.8f) / (0.3f - 0.8f)).coerceIn(0f, 1f)
        val mask = 0.6f + t * t * (3f - 2f * t) * 0.4f

        fun channel(shift: Int): Int {
            val top = ((c00 shr shift) and 0xFF) * (1f - fx) + ((c10 shr shift) and 0xFF) * fx
            val bottom = ((c01 shr shift) and 0xFF) * (1f - fx) + ((c11 shr shift) and 0xFF) * fx
            return ((top * (1f - fy) + bottom * fy) * mask).roundToInt().coerceIn(0, 255)
        }
        return (0xFF shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }

    private fun texel(x: Int, y: Int): Int = texture[mirror(y) * size + mirror(x)]

    private fun mirror(i: Int): Int {
        val period = 2 * size
        val m = ((i % period) + period) % period
        return if (m < size) m else period - 1 - m
    }
}

private fun smoothstep01(x: Float): Float {
    val t = x.coerceIn(0f, 1f)
    return t * t * (3 - 2 * t)
}

private fun fract(x: Double): Double = x - floor(x)

private fun hashNoise(x: Double, y: Double): Double = fract(sin(x * 12.9898 + y * 78.233) * 43758.5453)

private fun smoothNoise(x: Double, y: Double): Double {
    val x0 = floor(x)
    val y0 = floor(y)
    val xf = x - x0
    val yf = y - y0
    val u = xf * xf * (3 - 2 * xf)
    val v = yf * yf * (3 - 2 * yf)
    val n00 = hashNoise(x0, y0)
    val n10 = hashNoise(x0 + 1, y0)
    val n01 = hashNoise(x0, y0 + 1)
    val n11 = hashNoise(x0 + 1, y0 + 1)
    val nx0 = n00 * (1 - u) + n10 * u
    val nx1 = n01 * (1 - u) + n11 * u
    return nx0 * (1 - v) + nx1 * v
}

private fun noiseGradient(x: Double, y: Double, epsilon: Double = 0.001): Pair<Double, Double> {
    val dx = (smoothNoise(x + epsilon, y) - smoothNoise(x - epsilon, y)) / (2 * epsilon)
    val dy = (smoothNoise(x, y + epsilon) - smoothNoise(x, y - epsilon)) / (2 * epsilon)
    val len = sqrt(dx * dx + dy * dy).let { if (it == 0.0) 1.0 else it }
    return dx / len to dy / len
}

private fun Random.range(min: Float, max: Float): Float = nextFloat() * (max - min) + min

/** AMLL's generateControlPoints: a grid nudged along a noise field, then smoothed. */
internal fun amllGenerate(width: Int, height: Int, random: Random): AmllPreset {
    val variation = random.range(0.4f, 0.6f)
    val normalOffset = random.range(0.3f, 0.6f)
    val blend = 0.8f
    val smoothIterations = floor(random.range(3f, 5f)).toInt()
    val smoothFactor = random.range(0.2f, 0.3f)
    val smoothModifier = random.range(-0.1f, -0.05f)

    val w = width
    val h = height
    val dx = 2f / (w - 1)
    val dy = 2f / (h - 1)
    val pts = FloatArray(w * h * 8)
    var at = 0
    for (j in 0 until h) {
        for (i in 0 until w) {
            val baseX = i.toFloat() / (w - 1) * 2 - 1
            val baseY = j.toFloat() / (h - 1) * 2 - 1
            val border = i == 0 || i == w - 1 || j == 0 || j == h - 1
            val pertX = if (border) 0f else random.range(-variation * dx, variation * dx)
            val pertY = if (border) 0f else random.range(-variation * dy, variation * dy)
            var x = baseX + pertX
            var y = baseY + pertY
            val ur = if (border) 0f else random.range(-60f, 60f)
            val vr = if (border) 0f else random.range(-60f, 60f)
            val up = if (border) 1f else random.range(0.8f, 1.2f)
            val vp = if (border) 1f else random.range(0.8f, 1.2f)
            if (!border) {
                val uNorm = (baseX + 1) / 2
                val vNorm = (baseY + 1) / 2
                val (nx, ny) = noiseGradient(uNorm.toDouble(), vNorm.toDouble())
                var offsetX = (nx * normalOffset).toFloat()
                var offsetY = (ny * normalOffset).toFloat()
                val distToBorder = min(min(uNorm, 1 - uNorm), min(vNorm, 1 - vNorm))
                val weight = smoothstep01(distToBorder)
                offsetX *= weight
                offsetY *= weight
                x = x * (1 - blend) + (x + offsetX) * blend
                y = y * (1 - blend) + (y + offsetY) * blend
            }
            val base = at * 8
            pts[base] = i.toFloat()
            pts[base + 1] = j.toFloat()
            pts[base + 2] = x
            pts[base + 3] = y
            pts[base + 4] = ur
            pts[base + 5] = vr
            pts[base + 6] = up
            pts[base + 7] = vp
            at++
        }
    }
    smoothifyControlPoints(pts, w, h, smoothIterations, smoothFactor, smoothModifier)
    return AmllPreset(w, h, pts)
}

private fun smoothifyControlPoints(pts: FloatArray, w: Int, h: Int, iterations: Int, factor: Float, modifier: Float) {
    var f = factor
    val kernel = intArrayOf(1, 2, 1, 2, 4, 2, 1, 2, 1)
    var grid = pts.copyOf()
    repeat(iterations) {
        val next = grid.copyOf()
        for (j in 1 until h - 1) {
            for (i in 1 until w - 1) {
                // x, y, ur, vr, up, vp
                for (field in intArrayOf(2, 3, 4, 5, 6, 7)) {
                    var sum = 0f
                    for (dj in -1..1) {
                        for (di in -1..1) {
                            sum += grid[((j + dj) * w + (i + di)) * 8 + field] * kernel[(dj + 1) * 3 + (di + 1)]
                        }
                    }
                    val current = grid[(j * w + i) * 8 + field]
                    next[(j * w + i) * 8 + field] = current * (1 - f) + sum / 16f * f
                }
            }
        }
        grid = next
        f = (f + modifier).coerceIn(0f, 1f)
    }
    grid.copyInto(pts)
}
