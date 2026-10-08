package rotatsu.yos.music.player.ui.widgets

import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import rotatsu.yos.music.player.data.libraries.SettingsLibrary
import java.io.File

/** The lyric font weights from thin to black, in the order the settings slider steps through. */
val LyricFontWeightNames = listOf(
    "Thin", "ExtraLight", "Light", "Regular", "Medium", "SemiBold", "Bold", "ExtraBold", "Black"
)

fun lyricFontWeight(name: String): FontWeight = when (name) {
    "Thin" -> FontWeight.Thin
    "ExtraLight" -> FontWeight.ExtraLight
    "Light" -> FontWeight.Light
    "Regular" -> FontWeight.Normal
    "Medium" -> FontWeight.Medium
    "SemiBold" -> FontWeight.SemiBold
    "Bold" -> FontWeight.Bold
    "ExtraBold" -> FontWeight.ExtraBold
    "Black" -> FontWeight.Black
    else -> FontWeight.ExtraBold
}

/** The style of a main lyric line. [scale] multiplies the default size and line height. */
fun lyricTextStyle(
    scale: Float,
    weightName: String,
    balanced: Boolean,
    family: FontFamily?
) = TextStyle(
    fontSize = (30.5f * scale).sp,
    lineHeight = (40.5f * scale).sp,
    fontWeight = lyricFontWeight(weightName),
    fontFamily = family,
    letterSpacing = 0.05.sp,
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None
    ),
    lineBreak = LineBreak(
        strategy = if (balanced) LineBreak.Strategy.Balanced else LineBreak.Strategy.Simple,
        LineBreak.Strictness.Default,
        LineBreak.WordBreak.Default
    )
)

/** The custom lyric font chosen in settings, or null for the system font. */
@Composable
fun rememberLyricFontFamily(): FontFamily? {
    val path = SettingsLibrary.LyricFontPath
    return remember(path) {
        if (path.isBlank()) {
            null
        } else {
            runCatching { FontFamily(Typeface.createFromFile(File(path))) }.getOrNull()
        }
    }
}
