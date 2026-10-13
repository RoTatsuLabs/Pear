package rotatsu.yos.music.player.ui.widgets.effects

// The backdrop under the full screen cover, after the one behind BitChord's player, from
// ArtworkMeshBackdrop.kt of https://github.com/kushagrasinghx/BitChord (GPL-3.0): the cover's own colours
// as a small mesh that continues from the cover's bottom edge, softened by a blur and a light scrim.
// Left out are its clip frames and its reduced motion settings.

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** Shown until a cover has been read. */
private val FallbackTone = Color(0xFF2B2B2B)

/** Where the cover's bottom edge sits under the full screen cover, as a share of the height. */
private const val SEAM = 0.56f

/** How long the backdrop takes to change colour when the song does. */
private const val FADE_MS = 900

/** How far the mesh is smeared. Narrow, because the mesh is already smooth. */
private const val BLUR_DP = 32

/** What the cover is read at before it is averaged down. A whole multiple of [MESH_GRID]. */
private const val READ_PX = 120

/** A session's worth of covers, at four kilobytes each. */
private val meshCache = object : LinkedHashMap<String, ImageBitmap>(0, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>?): Boolean = size > 32
}

/**
 * The colours of the cover at [dataLambda], as a mesh that continues from [seam], a share of the height.
 * [scrimTop] and [scrimBottom] are how dark the top and the bottom of it are left.
 */
@Composable
fun CoverBackdrop(
    dataLambda: () -> Any?,
    modifier: Modifier = Modifier,
    seam: Float = SEAM,
    scrimTop: Float = 0.06f,
    scrimBottom: Float = 0.30f
) {
    val context = LocalContext.current
    val data = dataLambda()
    val key = data?.toString()
    // The mesh on screen carries over to the next cover while it is read, so the backdrop never blanks.
    val mesh by produceState<ImageBitmap?>(key?.let { meshCache[it] }, key) {
        val cover = key ?: return@produceState
        meshCache[cover]?.let {
            value = it
            return@produceState
        }
        val found = withContext(Dispatchers.IO) { readMesh(context, data, cover.hashCode()) }
        if (found != null) {
            meshCache[cover] = found
            value = found
        }
    }
    MeshBackdrop(mesh, modifier, seam, scrimTop, scrimBottom)
}

@Composable
private fun MeshBackdrop(
    mesh: ImageBitmap?,
    modifier: Modifier,
    seam: Float,
    scrimTop: Float,
    scrimBottom: Float
) {
    // The mesh on screen, and the one fading in over it. Two bitmaps, so the outgoing one has to stay
    // drawable until the fade has finished with it.
    var shown by remember { mutableStateOf(mesh) }
    var incoming by remember { mutableStateOf<ImageBitmap?>(null) }
    val fade = remember { Animatable(0f) }

    LaunchedEffect(mesh) {
        val next = mesh ?: return@LaunchedEffect
        // A change arriving mid-fade fades from what was fading in.
        incoming?.let { shown = it }
        incoming = null
        val current = shown
        if (next === current) return@LaunchedEffect
        if (current == null) {
            shown = next
            return@LaunchedEffect
        }
        incoming = next
        fade.snapTo(0f)
        fade.animateTo(1f, tween(FADE_MS, easing = FastOutSlowInEasing))
        shown = next
        incoming = null
    }

    // Below Android 12 there is no blur, and it is not missed: the mesh is smoothed before it is drawn.
    val blur = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.blur(BLUR_DP.dp) else Modifier
    Canvas(modifier.fillMaxSize().background(FallbackTone).then(blur)) {
        val seamY = size.height * seam
        shown?.let { drawMesh(it, seamY, 1f) }
        incoming?.let { drawMesh(it, seamY, fade.value) }
        // Enough of a scrim to keep white text off a bright cover, and no more.
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color.Black.copy(alpha = scrimTop), Color.Black.copy(alpha = scrimBottom))
            )
        )
    }
}

/**
 * Paints [mesh] over the whole surface: the grid from [seamY] down, and its first row held from there
 * up. Two draws of one small texture, both stretched by the sampler.
 */
private fun DrawScope.drawMesh(mesh: ImageBitmap, seamY: Float, alpha: Float) {
    if (alpha <= 0.001f) return
    val width = size.width.roundToInt()
    if (seamY > 0.5f) {
        drawImage(
            image = mesh,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(mesh.width, 1),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(width, seamY.roundToInt()),
            alpha = alpha,
            filterQuality = FilterQuality.Low
        )
    }
    drawImage(
        image = mesh,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(mesh.width, mesh.height),
        dstOffset = IntOffset(0, seamY.roundToInt()),
        dstSize = IntSize(width, (size.height - seamY).roundToInt()),
        alpha = alpha,
        filterQuality = FilterQuality.Low
    )
}

/** Reads the cover at [data], averages it into a mesh, and returns it as a texture. Null if it cannot. */
private suspend fun readMesh(context: Context, data: Any?, seed: Int): ImageBitmap? {
    val loader = ImageLoader(context)
    return try {
        // Software, because the pixels are read back.
        val request = ImageRequest.Builder(context).data(data).allowHardware(false).build()
        val source = loader.execute(request).drawable?.toBitmap() ?: return null
        val side = minOf(source.width, source.height)
        if (side < 1) return null
        val square = Bitmap.createBitmap(source, (source.width - side) / 2, (source.height - side) / 2, side, side)
        val small = Bitmap.createScaledBitmap(square, READ_PX, READ_PX, true)
        val pixels = IntArray(READ_PX * READ_PX)
        small.getPixels(pixels, 0, READ_PX, 0, 0, READ_PX, READ_PX)
        val texels = coverMesh(pixels, READ_PX, READ_PX, seed) ?: return null
        Bitmap.createBitmap(texels, MESH_TEX, MESH_TEX, Bitmap.Config.ARGB_8888).asImageBitmap()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    } finally {
        loader.shutdown()
    }
}
