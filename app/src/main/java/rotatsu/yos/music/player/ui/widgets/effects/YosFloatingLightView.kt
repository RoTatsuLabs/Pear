package rotatsu.yos.music.player.ui.widgets.effects

import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import rotatsu.yos.music.player.code.utils.lrc.YosLyricStack
import rotatsu.yos.music.player.data.libraries.SettingsLibrary.NowplayingBackgroundEffect
import rotatsu.yos.music.player.ui.pages.NowPlayingPage

/**
 * The background of the playback page: the mesh gradient of Apple Music-like Lyrics, made from the cover.
 * It turns only while the dynamic effect setting is on and a song is playing on a visible page.
 */
@Composable
fun YosFloatingLight(
    modifier: Modifier,
    album: () -> Uri?,
    isPlaying: () -> Boolean,
    nowPage: () -> String,
    showMiniPlayer: () -> Boolean
) {
    val lightness = remember { mutableFloatStateOf(0f) }
    val lifecycleState = LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val active = lifecycleState.value.isAtLeast(Lifecycle.State.RESUMED) && !showMiniPlayer()
    val animated = NowplayingBackgroundEffect && isPlaying() && active

    // On the lyric page the text sits on the whole background, so it gets a light scrim.
    val scrim = animateFloatAsState(
        targetValue = if (nowPage() == NowPlayingPage.Lyric) 1f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "floatingLightScrim"
    )

    Box(modifier) {
        AmllBackground(
            dataLambda = album,
            modifier = Modifier.fillMaxSize(),
            animated = animated,
            onLightness = { lightness.floatValue = it }
        )
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind {
                    if (scrim.value <= 0f) return@drawBehind
                    val boost = YosLyricStack.scrimBoost(lightness.floatValue)
                    fun shade(alpha: Float) = Color.Black.copy((alpha * boost).coerceAtMost(0.6f))
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to shade(0.30f),
                            0.22f to shade(0.10f),
                            1f to shade(0.18f)
                        ),
                        alpha = scrim.value
                    )
                }
        )
    }
}
