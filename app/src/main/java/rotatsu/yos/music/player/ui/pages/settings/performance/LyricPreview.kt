package rotatsu.yos.music.player.ui.pages.settings.performance

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import rotatsu.yos.music.player.R
import rotatsu.yos.music.player.data.libraries.SettingsLibrary
import rotatsu.yos.music.player.ui.widgets.lyricTextStyle
import rotatsu.yos.music.player.ui.widgets.rememberLyricFontFamily

/** A sample lyric line drawn with the current lyric settings. */
@Composable
fun LyricPreview() {
    val family = rememberLyricFontFamily()
    val scale = SettingsLibrary.LyricFontScale
    val style = lyricTextStyle(
        scale,
        SettingsLibrary.LyricFontWeight,
        SettingsLibrary.LyricLineBalance,
        family
    )
    val below = SettingsLibrary.LyricTransliterationBelow
    val color = MaterialTheme.colorScheme.onBackground

    val roman: @Composable () -> Unit = {
        Text(
            text = stringResource(id = R.string.lyric_preview_roman),
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .alpha(0.6f)
                .padding(vertical = 4.dp)
        )
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 22.dp)
    ) {
        if (!below) roman()
        Text(text = stringResource(id = R.string.lyric_preview_main), style = style, color = color)
        if (below) roman()
        Text(
            text = stringResource(id = R.string.lyric_preview_translation),
            color = color,
            fontFamily = family,
            fontSize = (20f * scale).sp,
            lineHeight = (25f * scale).sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier
                .alpha(0.5f)
                .padding(top = 8.dp)
        )
    }
}
