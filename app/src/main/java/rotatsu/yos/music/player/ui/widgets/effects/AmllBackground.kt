package rotatsu.yos.music.player.ui.widgets.effects

// The mesh gradient background of Apple Music-like Lyrics (AMLL), from packages/core/src/bg-render of
// https://github.com/amll-dev/applemusic-like-lyrics (AGPL-3.0), Copyright the AMLL authors. See AmllMesh.kt
// for what was ported. Left out: audio reactivity, the other two AMLL renderers, and the dither.

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.random.Random

/** Shown until a cover has been read. */
private val BlackFloor = Color(0xFF000000)

/** How long the background takes to change when the song does. AMLL's alpha ramp runs 500 ms. */
private const val FADE_MS = 550

/** Pixels across the buffer the mesh is drawn into. The screen stretches it, and the picture is soft. */
private const val RENDER_WIDTH = 72

/** Frames a second while the background moves. AMLL runs at 60, but the flow is slow. */
private const val FRAME_NANOS = 33_000_000L

/** The texture turns once in about 31 s: AMLL's tick time over 5000, in radians. */
private const val SECONDS_PER_RADIAN = 5.0

/** What is known of a cover once it has been read, apart from the screen it is drawn on. */
private class AmllPrepared(val texture: IntArray, val mesh: AmllMeshData, val lightness: Float, val seed: Int)

/** A session's worth of covers, at about 30 kilobytes each. */
private val preparedCache = object : LinkedHashMap<String, AmllPrepared>(0, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, AmllPrepared>?): Boolean = size > 16
}

/**
 * One cover drawn at one size. Two bitmaps are taken in turns, so the one on screen is never written to
 * while it is being drawn.
 */
private class AmllLayer(val prepared: AmllPrepared, val width: Int, val height: Int) {
    private val bitmaps = arrayOf(
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888),
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    )
    private val pixels = IntArray(width * height)
    private var next = 1

    @Volatile
    var front: Bitmap = bitmaps[0]
        private set

    fun render(angle: Float) {
        java.util.Arrays.fill(pixels, 0xFF000000.toInt())
        amllRaster(prepared.mesh, width, height, pixels, AmllTextureShader(prepared.texture, AMLL_TEXTURE, angle))
        val target = bitmaps[next]
        target.setPixels(pixels, 0, width, 0, 0, width, height)
        front = target
        next = 1 - next
    }
}

/**
 * The colours of the cover at [dataLambda] as a slowly turning mesh gradient. It moves only while
 * [animated] is true; otherwise it is drawn once, at an angle the cover picks. [onLightness] is told how
 * light the processed cover is, from 0 to 1. [scrimTop] and [scrimBottom] are how dark the ends are left.
 */
@Composable
fun AmllBackground(
    dataLambda: () -> Any?,
    modifier: Modifier = Modifier,
    animated: Boolean = false,
    scrimTop: Float = 0f,
    scrimBottom: Float = 0f,
    onLightness: (Float) -> Unit = {}
) {
    val context = LocalContext.current
    val data = dataLambda()
    val key = data?.toString()
    // The cover being read does not blank the screen: the one before stays until the new one is ready.
    val prepared by produceState<AmllPrepared?>(key?.let { preparedCache[it] }, key) {
        val cover = key ?: return@produceState
        preparedCache[cover]?.let {
            value = it
            return@produceState
        }
        val found = withContext(Dispatchers.IO) { readCover(context, data, cover.hashCode()) }
        if (found != null) {
            preparedCache[cover] = found
            value = found
        }
    }

    var size by remember { mutableStateOf(IntSize.Zero) }
    var shown by remember { mutableStateOf<AmllLayer?>(null) }
    var incoming by remember { mutableStateOf<AmllLayer?>(null) }
    val fade = remember { Animatable(0f) }
    val tick = remember { mutableIntStateOf(0) }

    LaunchedEffect(prepared, size) {
        val cover = prepared ?: return@LaunchedEffect
        if (size.width <= 0 || size.height <= 0) return@LaunchedEffect
        onLightness(cover.lightness)
        val width = RENDER_WIDTH
        val height = (RENDER_WIDTH.toFloat() * size.height / size.width).toInt().coerceAtLeast(8)
        val layer = withContext(Dispatchers.Default) {
            AmllLayer(cover, width, height).also { it.render(restingAngle(cover.seed)) }
        }
        val current = shown
        when {
            current == null || (current.prepared === cover) -> {
                // First cover, or the same one at a new size: no fade.
                shown = layer
                incoming = null
            }
            else -> {
                // A change arriving mid-fade fades from what was fading in.
                incoming?.let { shown = it }
                incoming = layer
                fade.snapTo(0f)
                fade.animateTo(1f, tween(FADE_MS, easing = LinearEasing))
                shown = layer
                incoming = null
            }
        }
        tick.intValue++
    }

    LaunchedEffect(animated, shown, incoming) {
        if (!animated) return@LaunchedEffect
        var last = 0L
        while (isActive) {
            val now = withFrameNanos { it }
            if (now - last < FRAME_NANOS) continue
            last = now
            val angle = movingAngle(now)
            val layers = listOfNotNull(shown, incoming)
            if (layers.isEmpty()) continue
            withContext(Dispatchers.Default) { layers.forEach { it.render(angle) } }
            tick.intValue++
        }
    }

    val paint = remember { Paint(Paint.FILTER_BITMAP_FLAG) }
    Canvas(
        modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
            .background(BlackFloor)
    ) {
        // Read here, so each new frame draws again.
        tick.intValue
        val target = Rect(0, 0, this.size.width.toInt(), this.size.height.toInt())
        drawIntoCanvas { canvas ->
            shown?.let { canvas.nativeCanvas.drawBitmap(it.front, null, target, paint) }
            incoming?.let {
                paint.alpha = (easeInOutSine(fade.value) * 255f).toInt().coerceIn(0, 255)
                canvas.nativeCanvas.drawBitmap(it.front, null, target, paint)
                paint.alpha = 255
            }
        }
        if (scrimTop > 0f || scrimBottom > 0f) {
            drawRect(
                Brush.verticalGradient(
                    listOf(Color.Black.copy(alpha = scrimTop), Color.Black.copy(alpha = scrimBottom))
                )
            )
        }
    }
}

private fun easeInOutSine(x: Float): Float = (-(cos(PI * x.coerceIn(0f, 1f)) - 1) / 2).toFloat()

/** Where a still background rests: any angle, but the same one for the same cover. */
private fun restingAngle(seed: Int): Float =
    (Random(seed.toLong() xor 0x5DEECE66DL).nextFloat() * 2f * PI).toFloat()

/** The angle at frame time [nanos], kept small so it stays exact in a float. */
private fun movingAngle(nanos: Long): Float =
    ((nanos / 1_000_000_000.0 / SECONDS_PER_RADIAN) % (2 * PI)).toFloat()

/** Reads the cover at [data], reduces it to a texture, and picks a mesh for it. Null if it cannot. */
private suspend fun readCover(context: Context, data: Any?, seed: Int): AmllPrepared? {
    val loader = ImageLoader(context)
    return try {
        // Software, because the pixels are read back.
        val request = ImageRequest.Builder(context).data(data).allowHardware(false).build()
        val source = loader.execute(request).drawable?.toBitmap() ?: return null
        val side = minOf(source.width, source.height)
        if (side < 1) return null
        val square = Bitmap.createBitmap(source, (source.width - side) / 2, (source.height - side) / 2, side, side)
        // In two steps, so a large cover is averaged and not sampled.
        val middle = if (side > AMLL_TEXTURE * 4) Bitmap.createScaledBitmap(square, AMLL_TEXTURE * 4, AMLL_TEXTURE * 4, true) else square
        val small = Bitmap.createScaledBitmap(middle, AMLL_TEXTURE, AMLL_TEXTURE, true)
        val pixels = IntArray(AMLL_TEXTURE * AMLL_TEXTURE)
        small.getPixels(pixels, 0, AMLL_TEXTURE, 0, 0, AMLL_TEXTURE, AMLL_TEXTURE)
        val texture = amllTexture(pixels)
        AmllPrepared(texture, amllMesh(amllPick(Random(seed))), amllLightness(texture), seed)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    } finally {
        loader.shutdown()
    }
}
