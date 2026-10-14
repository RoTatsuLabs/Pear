package rotatsu.yos.music.player.ui.widgets.effects

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * The colours of the cover at [dataLambda] as a still mesh gradient, from Apple Music-like Lyrics.
 * [scrimTop] and [scrimBottom] are how dark the top and the bottom of it are left.
 */
@Composable
fun CoverBackdrop(
    dataLambda: () -> Any?,
    modifier: Modifier = Modifier,
    scrimTop: Float = 0.06f,
    scrimBottom: Float = 0.30f
) {
    AmllBackground(
        dataLambda = dataLambda,
        modifier = modifier,
        animated = false,
        scrimTop = scrimTop,
        scrimBottom = scrimBottom
    )
}
