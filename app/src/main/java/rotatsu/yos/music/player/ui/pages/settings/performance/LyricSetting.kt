package rotatsu.yos.music.player.ui.pages.settings.performance

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import rotatsu.yos.music.player.R
import rotatsu.yos.music.player.data.libraries.SettingsLibrary
import rotatsu.yos.music.player.ui.pages.settings.Divider
import rotatsu.yos.music.player.ui.pages.settings.GroupSpacer
import rotatsu.yos.music.player.ui.pages.settings.GroupSpacerMedium
import rotatsu.yos.music.player.ui.pages.settings.LabelItem
import rotatsu.yos.music.player.ui.pages.settings.ListHeader
import rotatsu.yos.music.player.ui.pages.settings.SelectItem
import rotatsu.yos.music.player.ui.pages.settings.SettingBackground
import rotatsu.yos.music.player.ui.pages.settings.SliderItem
import rotatsu.yos.music.player.ui.pages.settings.SwitchItem
import rotatsu.yos.music.player.ui.widgets.LyricFontWeightNames
import rotatsu.yos.music.player.ui.widgets.basic.RoundColumn
import rotatsu.yos.music.player.ui.widgets.basic.Title
import java.io.File

private const val FONT_SCALE_MIN = 0.7f
private const val FONT_SCALE_MAX = 1.5f
private const val OFFSET_LIMIT = 2000f

@Composable
fun LyricSetting(navController: NavController) =
    SettingBackground {
        val context = LocalContext.current
        val fontPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) importFont(context, uri)
        }
        Title(title = stringResource(id = R.string.settings_performance_lyric_title),
            onBack = {
                navController.popBackStack()
            },
            content = {
                item("settings") {
                    Column(Modifier.fillMaxSize()) {
                        RoundColumn {
                            LyricPreview()
                        }

                        GroupSpacer()
                        ListHeader(content = stringResource(id = R.string.settings_performance_lyric_custom))
                        RoundColumn {
                            LabelItem(
                                title = stringResource(id = R.string.settings_performance_lyric_font_file),
                                desc = SettingsLibrary.LyricFontName.ifBlank {
                                    stringResource(id = R.string.settings_performance_lyric_font_file_default)
                                },
                                onClick = { fontPicker.launch(arrayOf("*/*")) }
                            )
                            if (SettingsLibrary.LyricFontPath.isNotBlank()) {
                                Divider()
                                LabelItem(
                                    title = stringResource(id = R.string.settings_performance_lyric_font_file_reset),
                                    superLink = true,
                                    onClick = { clearFont() }
                                )
                            }
                        }

                        GroupSpacer()
                        ListHeader(content = stringResource(id = R.string.settings_performance_lyric_font_size))
                        RoundColumn {
                            SliderItem(
                                value = SettingsLibrary.LyricFontScale,
                                valueRange = FONT_SCALE_MIN..FONT_SCALE_MAX,
                                step = 0.05f,
                                onValueChange = { SettingsLibrary.LyricFontScale = it }
                            )
                        }

                        GroupSpacerMedium()
                        RoundColumn {
                            LabelItem(
                                title = stringResource(id = R.string.settings_performance_lyric_font_size_reset),
                                superLink = true,
                                onClick = { SettingsLibrary.LyricFontScale = 1f }
                            )
                        }
                        ListHeader(content = stringResource(id = R.string.settings_performance_lyric_font_size_reset_desc))

                        GroupSpacer()
                        ListHeader(content = stringResource(id = R.string.settings_performance_lyric_style_font_weight))
                        RoundColumn {
                            SliderItem(
                                value = LyricFontWeightNames.indexOf(SettingsLibrary.LyricFontWeight)
                                    .coerceAtLeast(0).toFloat(),
                                valueRange = 0f..(LyricFontWeightNames.size - 1).toFloat(),
                                step = 1f,
                                onValueChange = {
                                    SettingsLibrary.LyricFontWeight = LyricFontWeightNames[it.toInt()]
                                }
                            )
                        }
                        ListHeader(content = stringResource(id = R.string.settings_performance_lyric_style_font_weight_desc))

                        GroupSpacerMedium()
                        RoundColumn {
                            SwitchItem(
                                title = stringResource(id = R.string.settings_performance_lyric_smart),
                                onClick = {
                                    SettingsLibrary.LyricSmartWordByWord =
                                        !SettingsLibrary.LyricSmartWordByWord
                                },
                                checkedLambda = { SettingsLibrary.LyricSmartWordByWord }
                            )
                        }
                        ListHeader(content = stringResource(id = R.string.settings_performance_lyric_smart_desc))

                        GroupSpacer()
                        ListHeader(content = stringResource(id = R.string.settings_performance_lyric_offset))
                        RoundColumn {
                            SliderItem(
                                value = SettingsLibrary.LyricTimingOffset.toFloat(),
                                valueRange = -OFFSET_LIMIT..OFFSET_LIMIT,
                                step = 50f,
                                onValueChange = { SettingsLibrary.LyricTimingOffset = it.toInt() },
                                showStepButtons = false,
                                label = "${SettingsLibrary.LyricTimingOffset} ms"
                            )
                        }
                        ListHeader(content = stringResource(id = R.string.settings_performance_lyric_offset_desc))

                        GroupSpacerMedium()
                        RoundColumn {
                            SwitchItem(
                                title = stringResource(id = R.string.settings_performance_lyric_line_balance),
                                onClick = {
                                    SettingsLibrary.LyricLineBalance =
                                        !SettingsLibrary.LyricLineBalance
                                },
                                checkedLambda = { SettingsLibrary.LyricLineBalance }
                            )
                        }
                        ListHeader(content = stringResource(id = R.string.settings_performance_lyric_line_balance_desc))

                        GroupSpacer()
                        ListHeader(content = stringResource(id = R.string.settings_performance_lyric_transliteration))
                        RoundColumn {
                            val above = stringResource(id = R.string.settings_performance_lyric_transliteration_above)
                            val below = stringResource(id = R.string.settings_performance_lyric_transliteration_below)
                            SelectItem(
                                title = stringResource(id = R.string.settings_performance_lyric_transliteration_position),
                                items = listOf(above, below),
                                value = if (SettingsLibrary.LyricTransliterationBelow) below else above,
                                onValueChange = { SettingsLibrary.LyricTransliterationBelow = it == below }
                            )
                        }
                        ListHeader(content = stringResource(id = R.string.settings_performance_lyric_transliteration_desc))

                        GroupSpacer()
                        ListHeader(content = stringResource(id = R.string.settings_performance_lyric_others))
                        RoundColumn {
                            SwitchItem(
                                title = stringResource(id = R.string.settings_performance_lyric_prefer_embedded),
                                onClick = {
                                    SettingsLibrary.LyricPreferEmbedded =
                                        !SettingsLibrary.LyricPreferEmbedded
                                },
                                checkedLambda = { SettingsLibrary.LyricPreferEmbedded }
                            )
                            Divider()
                            SwitchItem(
                                title = stringResource(id = R.string.settings_performance_lyric_blur_effect),
                                onClick = {
                                    SettingsLibrary.LyricBlurEffect =
                                        !SettingsLibrary.LyricBlurEffect
                                },
                                checkedLambda = { SettingsLibrary.LyricBlurEffect }
                            )
                        }
                        ListHeader(content = stringResource(id = R.string.settings_performance_lyric_prefer_embedded_desc))
                        ListHeader(content = stringResource(id = R.string.settings_performance_lyric_blur_effect_desc))
                        GroupSpacer()
                    }
                }
            }
        )
    }

private fun clearFont() {
    runCatching { File(SettingsLibrary.LyricFontPath).delete() }
    SettingsLibrary.LyricFontPath = ""
    SettingsLibrary.LyricFontName = ""
}

/** Copies a chosen font into the app files and switches the lyrics to it. */
private fun importFont(context: Context, uri: Uri) {
    val name = context.contentResolver
        .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { if (it.moveToFirst()) it.getString(0) else null }
        ?: "font.ttf"
    val extension = name.substringAfterLast('.', "").lowercase()
    val target = File(context.filesDir, "lyric_font_${System.currentTimeMillis()}.$extension")
    val usable = extension in setOf("ttf", "otf", "ttc") && runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { input.copyTo(it) }
        } != null && Typeface.createFromFile(target) != null
    }.getOrDefault(false)

    if (!usable) {
        target.delete()
        Toast.makeText(context, R.string.tip_lyric_font_invalid, Toast.LENGTH_SHORT).show()
        return
    }
    clearFont()
    SettingsLibrary.LyricFontPath = target.absolutePath
    SettingsLibrary.LyricFontName = name
}
