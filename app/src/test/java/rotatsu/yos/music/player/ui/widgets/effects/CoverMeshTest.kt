package rotatsu.yos.music.player.ui.widgets.effects

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoverMeshTest {
    private fun red(c: Int) = (c shr 16) and 0xFF
    private fun green(c: Int) = (c shr 8) and 0xFF
    private fun blue(c: Int) = c and 0xFF
    private fun luma(c: Int) = 0.299f * red(c) + 0.587f * green(c) + 0.114f * blue(c)
    private fun solid(color: Int, side: Int = 60) = IntArray(side * side) { color }

    @Test
    fun nothingToReadGivesNoMesh() {
        assertNull(coverMesh(IntArray(0), 0, 0, 1))
        assertNull(coverMesh(IntArray(10), 60, 60, 1))
    }

    @Test
    fun theMeshIsAThirtyTwoSquareOfOpaqueColours() {
        val mesh = coverMesh(solid(0xFF804020.toInt()), 60, 60, 3)
        assertNotNull(mesh)
        assertEquals(MESH_TEX * MESH_TEX, mesh!!.size)
        assertTrue(mesh.all { (it ushr 24) == 0xFF })
    }

    @Test
    fun aSolidCoverGivesOneColourWithItsSaturationLifted() {
        val mesh = coverMesh(solid(0xFF804020.toInt()), 60, 60, 3)!!
        assertTrue(mesh.all { it == mesh[0] })
        // Same hue, a little more saturated: red up or equal, blue down or equal.
        assertTrue(red(mesh[0]) >= 0x80 && blue(mesh[0]) <= 0x20)
    }

    @Test
    fun aMostlyBlackCoverStaysMostlyBlack() {
        val side = 60
        val pixels = IntArray(side * side) { 0xFF000000.toInt() }
        // One red stripe, a tenth of the cover.
        for (y in 0 until 6) for (x in 0 until side) pixels[y * side + x] = 0xFFE01010.toInt()
        val mesh = coverMesh(pixels, side, side, 5)!!
        val average = mesh.map { luma(it) }.average()
        assertTrue("average luma $average", average < 40.0)
    }

    @Test
    fun theTopRowIsTheCoversOwnBottomEdge() {
        val side = 60
        val pixels = IntArray(side * side) { 0xFF101010.toInt() }
        // The bottom tenth is a bright blue.
        for (y in side - 6 until side) for (x in 0 until side) pixels[y * side + x] = 0xFF2060F0.toInt()
        val mesh = coverMesh(pixels, side, side, 9)!!
        val top = mesh[0]
        assertTrue("blue ${blue(top)} red ${red(top)}", blue(top) > red(top) + 60)
    }

    @Test
    fun theSameCoverAlwaysLandsTheSameWay() {
        val side = 60
        val pixels = IntArray(side * side) { (0xFF000000.toInt()) or ((it * 7919) and 0xFFFFFF) }
        assertTrue(coverMesh(pixels, side, side, 42)!!.contentEquals(coverMesh(pixels, side, side, 42)!!))
    }

    @Test
    fun rotatingKeepsEveryCellsNeighbours() {
        val grid = IntArray(36) { it }
        val turned = grid.rotatedBelowSeam(6, 6, 11)
        // Row 0 is untouched and each later row is the same cells in the same cyclic order.
        for (x in 0 until 6) assertEquals(grid[x], turned[x])
        for (row in 1 until 6) {
            val before = (0 until 6).map { grid[row * 6 + it] }.toSet()
            val after = (0 until 6).map { turned[row * 6 + it] }.toSet()
            assertEquals(before, after)
        }
    }
}
