package rotatsu.yos.music.player.ui.pages.settings.performance.userinterface

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
import rotatsu.yos.music.player.ui.pages.settings.SwitchItem
import rotatsu.yos.music.player.ui.widgets.basic.RoundColumn
import rotatsu.yos.music.player.ui.widgets.basic.Title

@Composable
fun UserInterfaceSetting(navController: NavController) =
    SettingBackground {
        Title(title = stringResource(id = R.string.settings_performance_ui_title),
            onBack = {
                navController.popBackStack()
            },
            content = {
                item("settings") {
                    Column(Modifier.fillMaxSize()) {
                        // ListHeader(content = stringResource(id = R.string.settings_performance_ui_basic))

                        RoundColumn {
                            SelectItem(
                                title = stringResource(id = R.string.settings_performance_ui_theme),
                                items = listOf(
                                    "Auto",
                                    "Dark",
                                    "Light"
                                ),
                                value = SettingsLibrary.CustomTheme,
                                onValueChange = {
                                    SettingsLibrary.CustomTheme = it
                                }
                            )

                            Divider()

                            SwitchItem(
                                title = stringResource(id = R.string.settings_performance_ui_blur_effect_title),
                                // desc = stringResource(id = R.string.settings_performance_ui_blur_effect_desc),
                                onClick = {
                                    SettingsLibrary.BarBlurEffect = !SettingsLibrary.BarBlurEffect
                                },
                                checkedLambda = { SettingsLibrary.BarBlurEffect }
                            )
                        }
                        ListHeader(content = stringResource(id = R.string.settings_performance_ui_blur_effect_desc))

                        GroupSpacerMedium()

                        val showCornerSetDialog =
                        remember("UserInterfaceSetting_showCornerSetDialog") {
                            mutableStateOf(false)
                        }

                        RoundColumn {
                            LabelItem(
                                title = stringResource(id = R.string.settings_performance_ui_screen_corner_title),
                                // desc = stringResource(id = R.string.settings_performance_ui_screen_corner_desc),
                                superLink = true
                            ) {
                                showCornerSetDialog.value = true
                            }
                        }
                        ListHeader(content = stringResource(id = R.string.settings_performance_ui_screen_corner_desc))

                        if (showCornerSetDialog.value) {
                            ScreenCornerSetDialog {
                                showCornerSetDialog.value = false
                            }
                        }

                        GroupSpacerMedium()

                        RoundColumn {
                            SwitchItem(
                                title = stringResource(id = R.string.settings_performance_ui_nowplaying_show_volume_bar),
                                // desc = stringResource(id = R.string.settings_performance_ui_nowplaying_show_volume_bar_desc),
                                onClick = {
                                    SettingsLibrary.NowPlayingShowVolumeBar =
                                        !SettingsLibrary.NowPlayingShowVolumeBar
                                },
                                checkedLambda = { SettingsLibrary.NowPlayingShowVolumeBar }
                            )
                        }

                        ListHeader(content = stringResource(id = R.string.settings_performance_ui_nowplaying_show_volume_bar_desc))
                        GroupSpacerMedium()

                        RoundColumn {
                            SwitchItem(
                                title = stringResource(id = R.string.settings_performance_ui_nowplaying_background_effect),
                                // desc = stringResource(id = R.string.settings_performance_ui_nowplaying_background_effect_desc),
                                onClick = {
                                    SettingsLibrary.NowplayingBackgroundEffect =
                                        !SettingsLibrary.NowplayingBackgroundEffect
                                },
                                checkedLambda = { SettingsLibrary.NowplayingBackgroundEffect }
                            )
                        }

                        ListHeader(content = stringResource(id = R.string.settings_performance_ui_nowplaying_background_effect_desc))

                        GroupSpacerMedium()

                        RoundColumn {
                            SwitchItem(
                                title = stringResource(id = R.string.settings_performance_ui_full_screen_cover),
                                onClick = {
                                    SettingsLibrary.FullScreenCover = !SettingsLibrary.FullScreenCover
                                },
                                checkedLambda = { SettingsLibrary.FullScreenCover }
                            )
                        }

                        ListHeader(content = stringResource(id = R.string.settings_performance_ui_full_screen_cover_desc))

                        GroupSpacer()
                    }
                }
            }
        )
    }