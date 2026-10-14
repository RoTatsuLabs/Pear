package rotatsu.yos.music.player.ui.widgets.effects

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

class AmllMeshTest {
    private fun red(c: Int) = (c shr 16) and 0xFF
    private fun green(c: Int) = (c shr 8) and 0xFF
    private fun blue(c: Int) = c and 0xFF

    private fun regular(n: Int): AmllPreset {
        val points = FloatArray(n * n * 8)
        var at = 0
        for (j in 0 until n) {
            for (i in 0 until n) {
                val base = at * 8
                points[base] = i.toFloat()
                points[base + 1] = j.toFloat()
                points[base + 2] = i.toFloat() / (n - 1) * 2 - 1
                points[base + 3] = j.toFloat() / (n - 1) * 2 - 1
                points[base + 6] = 1f
                points[base + 7] = 1f
                at++
            }
        }
        return AmllPreset(n, n, points)
    }

    private fun holes(mesh: AmllMeshData, w: Int, h: Int): Int {
        val out = IntArray(w * h)
        amllRaster(mesh, w, h, out) { _, _ -> 1 }
        return out.count { it == 0 }
    }

    @Test
    fun theSixPresetsOfAmllAreAllThere() {
        assertEquals(listOf(5 to 5, 4 to 4, 4 to 4, 5 to 5, 5 to 5, 5 to 5), AMLL_PRESETS.map { it.width to it.height })
        assertTrue(AMLL_PRESETS.all { it.points.size == it.width * it.height * 8 })
    }

    @Test
    fun aRegularGridOfControlPointsGivesARegularMesh() {
        for (n in intArrayOf(4, 5)) {
            val sub = 8
            val mesh = amllMesh(regular(n), sub)
            assertEquals((n - 1) * sub, mesh.cols)
            for (gy in 0 until mesh.rows) {
                for (gx in 0 until mesh.cols) {
                    val i = gy * mesh.cols + gx
                    val cellX = gy / sub
                    val v = gy % sub
                    val cellY = gx / sub
                    val u = gx % sub
                    val ex = (cellX + v / (sub - 1f)) / (n - 1) * 2 - 1
                    val ey = (cellY + u / (sub - 1f)) / (n - 1) * 2 - 1
                    assertTrue(abs(mesh.x[i] - ex) < 1e-5f && abs(mesh.y[i] - ey) < 1e-5f)
                    // The texture is laid on as a plain grid, upside down in v as the screen is.
                    assertTrue(abs(mesh.u[i] - (cellX + v / (sub - 1f)) / (n - 1)) < 1e-5f)
                    assertTrue(abs(mesh.v[i] - (1f - (cellY + u / (sub - 1f)) / (n - 1))) < 1e-5f)
                }
            }
        }
    }

    @Test
    fun everyPresetFillsAPortraitAndALandscapeBuffer() {
        for (preset in AMLL_PRESETS) {
            val mesh = amllMesh(preset)
            assertEquals(0, holes(mesh, 72, 160))
            assertEquals(0, holes(mesh, 160, 72))
        }
    }

    @Test
    fun aMadeUpMeshKeepsItsBorderIsRepeatableAndFillsTheBuffer() {
        val a = amllGenerate(6, 6, Random(7))
        assertTrue(a.points.contentEquals(amllGenerate(6, 6, Random(7)).points))
        assertFalse(a.points.contentEquals(amllGenerate(6, 6, Random(8)).points))
        for (j in 0 until 6) {
            for (i in 0 until 6) {
                if (i == 0 || j == 0 || i == 5 || j == 5) {
                    val base = (j * 6 + i) * 8
                    assertEquals(i / 5f * 2 - 1, a.points[base + 2])
                    assertEquals(j / 5f * 2 - 1, a.points[base + 3])
                }
            }
        }
        for (seed in 0 until 40) assertEquals(0, holes(amllMesh(amllPick(Random(seed))), 72, 160))
    }

    @Test
    fun theCoverIsProcessedAsAmllDoes() {
        // Mid grey: contrast to 0.4 and back leaves 128, and brightness takes it to 96.
        val grey = amllTexture(IntArray(AMLL_TEXTURE * AMLL_TEXTURE) { 0xFF808080.toInt() })
        assertTrue(grey.all { red(it) == 96 && green(it) == 96 && blue(it) == 96 })
        // A red stays strongly red.
        val redTexture = amllTexture(IntArray(AMLL_TEXTURE * AMLL_TEXTURE) { 0xFFC83228.toInt() })
        val p = redTexture[500]
        assertTrue(red(p) > green(p) + 80 && red(p) > blue(p) + 80)
        // A hard edge is softened.
        val edge = amllTexture(IntArray(AMLL_TEXTURE * AMLL_TEXTURE) {
            if (it % AMLL_TEXTURE < 16) 0xFFFF0000.toInt() else 0xFF0000FF.toInt()
        })
        assertTrue(red(edge[15]) > red(edge[16]) && blue(edge[16]) > blue(edge[15]))
        assertTrue(red(edge[15 + 5 * AMLL_TEXTURE]) in 1..250)
        assertTrue(amllLightness(IntArray(4) { 0xFFFFFFFF.toInt() }) > 0.99f)
        assertTrue(amllLightness(IntArray(4) { 0xFF000000.toInt() }) < 0.01f)
    }

    @Test
    fun aSolidTextureKeepsItsColourAndIsDarkenedAtTheEdges() {
        val solid = IntArray(AMLL_TEXTURE * AMLL_TEXTURE) { 0xFF4080C0.toInt() }
        val mesh = amllMesh(AMLL_PRESETS[0])
        val still = IntArray(72 * 160)
        val turned = IntArray(72 * 160)
        amllRaster(mesh, 72, 160, still, AmllTextureShader(solid, AMLL_TEXTURE, 0f))
        amllRaster(mesh, 72, 160, turned, AmllTextureShader(solid, AMLL_TEXTURE, 2.3f))
        assertTrue(still.contentEquals(turned))
        val centre = still[80 * 72 + 36]
        val corner = still[0]
        assertTrue(red(centre) in 0x3C..0x41 && blue(centre) in 0xB4..0xC0)
        assertTrue(blue(corner) < blue(centre) * 0.85f && blue(corner) > blue(centre) * 0.55f)
    }

    @Test
    fun turningMovesTheTextureAndAFullTurnComesBack() {
        val ramp = IntArray(AMLL_TEXTURE * AMLL_TEXTURE) {
            val x = it % AMLL_TEXTURE
            (0xFF shl 24) or ((x * 8) shl 16) or (((AMLL_TEXTURE - 1 - x) * 8) shl 8) or 0x40
        }
        val mesh = amllMesh(AMLL_PRESETS[1])
        val a = IntArray(72 * 160)
        val b = IntArray(72 * 160)
        val c = IntArray(72 * 160)
        amllRaster(mesh, 72, 160, a, AmllTextureShader(ramp, AMLL_TEXTURE, 0f))
        amllRaster(mesh, 72, 160, b, AmllTextureShader(ramp, AMLL_TEXTURE, 1f))
        amllRaster(mesh, 72, 160, c, AmllTextureShader(ramp, AMLL_TEXTURE, (2 * Math.PI).toFloat()))
        assertFalse(a.contentEquals(b))
        var worst = 0
        for (i in a.indices) worst = maxOf(worst, abs(red(a[i]) - red(c[i])), abs(green(a[i]) - green(c[i])))
        assertTrue("worst $worst", worst <= 2)
    }
}
