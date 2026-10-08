package rotatsu.yos.music.player.ui.pages.settings.experimental

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import rotatsu.yos.music.player.R
import rotatsu.yos.music.player.data.libraries.SettingsLibrary
import rotatsu.yos.music.player.ui.pages.settings.ListHeader
import rotatsu.yos.music.player.ui.pages.settings.SettingBackground
import rotatsu.yos.music.player.ui.pages.settings.SwitchItem
import rotatsu.yos.music.player.ui.widgets.basic.RoundColumn
import rotatsu.yos.music.player.ui.widgets.basic.Title

@Composable
fun VocalSeparationSetting(navController: NavController) =
    SettingBackground {
        Title(title = stringResource(id = R.string.settings_experimental_vocal_separation),
            onBack = {
                navController.popBackStack()
            },
            content = {
                item("settings") {
                    Column(Modifier.fillMaxSize()) {
                        RoundColumn {
                            SwitchItem(
                                title = stringResource(id = R.string.settings_experimental_vocal_gpu),
                                onClick = {
                                    SettingsLibrary.VocalSeparationGpu = !SettingsLibrary.VocalSeparationGpu
                                },
                                checkedLambda = { SettingsLibrary.VocalSeparationGpu }
                            )
                        }
                        ListHeader(content = stringResource(id = R.string.settings_experimental_vocal_gpu_desc))
                        ListHeader(content = stringResource(id = R.string.settings_experimental_vocal_gpu_note))
                    }
                }
            }
        )
    }
