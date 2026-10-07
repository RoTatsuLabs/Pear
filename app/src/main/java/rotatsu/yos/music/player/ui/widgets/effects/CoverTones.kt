package rotatsu.yos.music.player.ui.widgets.effects

internal class CoverTones(val edge: Int, val mid: Int, val end: Int)

/** Dark tones for the backdrop under a cover, from a square of [size] by [size] ARGB [pixels]. */
internal fun coverTones(pixels: IntArray, size: Int): CoverTones {
    val all = average(pixels, size, 0)
    val low = average(pixels, size, size - size / 4)
    val lightness = (0.299f * all[0] + 0.587f * all[1] + 0.114f * all[2]) / 255f
    val extra = 0.25f * ((lightness - 0.5f) / 0.5f).coerceIn(0f, 1f)
    return CoverTones(
        edge = shade(low, 0.35f + extra),
        mid = shade(all, 0.45f + extra),
        end = shade(all, 0.65f + extra)
    )
}

private fun average(pixels: IntArray, size: Int, fromRow: Int): FloatArray {
    var r = 0f
    var g = 0f
    var b = 0f
    var count = 0
    for (y in fromRow until size) {
        for (x in 0 until size) {
            val p = pixels[y * size + x]
            r += (p shr 16) and 0xFF
            g += (p shr 8) and 0xFF
            b += p and 0xFF
            count++
        }
    }
    if (count == 0) return floatArrayOf(0f, 0f, 0f)
    return floatArrayOf(r / count, g / count, b / count)
}

private fun shade(rgb: FloatArray, darkness: Float): Int {
    val keep = 1f - darkness.coerceIn(0f, 0.85f)
    val r = (rgb[0] * keep).toInt().coerceIn(0, 255)
    val g = (rgb[1] * keep).toInt().coerceIn(0, 255)
    val b = (rgb[2] * keep).toInt().coerceIn(0, 255)
    return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
}
