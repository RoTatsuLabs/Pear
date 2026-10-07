package rotatsu.yos.music.player.ui.pages.settings.performance

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import rotatsu.yos.music.player.R
import rotatsu.yos.music.player.data.libraries.SettingsLibrary
import rotatsu.yos.music.player.ui.pages.settings.GroupSpacer
import rotatsu.yos.music.player.ui.pages.settings.GroupSpacerMedium
import rotatsu.yos.music.player.ui.pages.settings.ListHeader
import rotatsu.yos.music.player.ui.pages.settings.SelectItem
import rotatsu.yos.music.player.ui.pages.settings.SettingBackground
import rotatsu.yos.music.player.ui.pages.settings.SwitchItem
import rotatsu.yos.music.player.ui.widgets.basic.RoundColumn
import rotatsu.yos.music.player.ui.widgets.basic.Title

@Composable
fun LyricSetting(navController: NavController) =
    SettingBackground {
        Title(title = stringResource(id = R.string.settings_performance_lyric_title),
            onBack = {
                navController.popBackStack()
            },
            content = {
                item("settings") {
                    Column(Modifier.fillMaxSize()) {
                        // GroupSpacerMedium()
                        ListHeader(content = stringResource(id = R.string.settings_performance_lyric_style))

                        RoundColumn {
                            SelectItem(
                                title = stringResource(id = R.string.settings_performance_lyric_style_font_weight),
                                // desc = stringResource(id = R.string.settings_performance_lyric_style_font_weight_desc),
                                items = listOf(
                                    "Thin",
                                    "ExtraLight",
                                    "Light",
                                    "Regular",
                                    "Medium",
                                    "SemiBold",
                                    "Bold",
                                    "ExtraBold",
                                    "Black"
                                ),
                                value = SettingsLibrary.LyricFontWeight,
                                onValueChange = {
                                    SettingsLibrary.LyricFontWeight = it
                                }
                            )
                        }
                        ListHeader(content = stringResource(id = R.string.settings_performance_lyric_style_font_weight_desc))

                        GroupSpacerMedium()

                        RoundColumn {
                            SwitchItem(
                                title = stringResource(id = R.string.settings_performance_lyric_line_balance),
                                // desc = stringResource(id = R.string.settings_performance_lyric_line_balance_desc),
                                onClick = {
                                    SettingsLibrary.LyricLineBalance =
                                        !SettingsLibrary.LyricLineBalance
                                },
                                checkedLambda = { SettingsLibrary.LyricLineBalance }
                            )
                        }
                        ListHeader(content = stringResource(id = R.string.settings_performance_lyric_line_balance_desc))

                        GroupSpacer()

                        ListHeader(content = stringResource(id = R.string.settings_performance_lyric_others))

                        RoundColumn {
                            SwitchItem(
                                title = stringResource(id = R.string.settings_performance_lyric_blur_effect),
                                // desc = stringResource(id = R.string.settings_performance_lyric_blur_effect_desc),
                                onClick = {
                                    SettingsLibrary.LyricBlurEffect =
                                        !SettingsLibrary.LyricBlurEffect
                                },
                                checkedLambda = { SettingsLibrary.LyricBlurEffect }
                            )
                        }

                        ListHeader(content = stringResource(id = R.string.settings_performance_lyric_blur_effect_desc))
                        GroupSpacer()
                    }
                }
            }
        )
    }