package rotatsu.yos.music.player.ui.widgets.effects

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rotatsu.yos.music.player.code.utils.others.BitmapResolver

private val FallbackTone = Color(0xFF2B2B2B)

@Composable
fun CoverBackdrop(dataLambda: () -> Any?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val data = dataLambda()
    val tones by produceState<CoverTones?>(null, data) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val loader = ImageLoader(context)
                val bitmap = loader.execute(ImageRequest.Builder(context).data(data).build())
                    .drawable?.toBitmap()?.let { BitmapResolver.bitmapCompress(it) }
                loader.shutdown()
                bitmap?.let {
                    val tiny = Bitmap.createScaledBitmap(it, 16, 16, true)
                    val pixels = IntArray(256)
                    tiny.getPixels(pixels, 0, 16, 0, 0, 16, 16)
                    coverTones(pixels, 16)
                }
            }.getOrNull()
        }
    }
    val edge by animateColorAsState(tones?.let { Color(it.edge) } ?: FallbackTone, tween(400))
    val mid by animateColorAsState(tones?.let { Color(it.mid) } ?: FallbackTone, tween(400))
    val end by animateColorAsState(tones?.let { Color(it.end) } ?: FallbackTone, tween(400))
    Spacer(
        modifier.drawBehind {
            drawRect(
                Brush.verticalGradient(
                    0f to edge,
                    0.56f to edge,
                    0.78f to mid,
                    1f to end
                )
            )
        }
    )
}
