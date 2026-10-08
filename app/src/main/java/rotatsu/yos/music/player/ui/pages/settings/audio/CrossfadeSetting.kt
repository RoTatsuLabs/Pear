package rotatsu.yos.music.player.ui.pages.settings.audio

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import rotatsu.yos.music.player.R
import rotatsu.yos.music.player.data.libraries.SettingsLibrary
import rotatsu.yos.music.player.ui.pages.settings.GroupSpacer
import rotatsu.yos.music.player.ui.pages.settings.ListHeader
import rotatsu.yos.music.player.ui.pages.settings.SettingBackground
import rotatsu.yos.music.player.ui.pages.settings.SliderItem
import rotatsu.yos.music.player.ui.pages.settings.SwitchItem
import rotatsu.yos.music.player.ui.widgets.basic.RoundColumn
import rotatsu.yos.music.player.ui.widgets.basic.Title

private const val DURATION_MIN = 1f
private const val DURATION_MAX = 12f

@Composable
fun CrossfadeSetting(navController: NavController) =
    SettingBackground {
        Title(title = stringResource(id = R.string.settings_audio_crossfade),
            onBack = {
                navController.popBackStack()
            },
            content = {
                item("settings") {
                    Column(Modifier.fillMaxSize()) {
                        RoundColumn {
                            SwitchItem(
                                title = stringResource(id = R.string.settings_audio_crossfade_enable),
                                onClick = {
                                    SettingsLibrary.CrossfadeEnabled = !SettingsLibrary.CrossfadeEnabled
                                },
                                checkedLambda = { SettingsLibrary.CrossfadeEnabled }
                            )
                        }
                        ListHeader(content = stringResource(id = R.string.settings_audio_crossfade_enable_desc))

                        GroupSpacer()
                        RoundColumn {
                            Column(Modifier.fillMaxWidth()) {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, end = 16.dp, top = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.settings_audio_crossfade_duration),
                                        fontSize = 17.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${SettingsLibrary.CrossfadeDuration} s",
                                        fontSize = 17.sp,
                                        modifier = Modifier.alpha(0.5f)
                                    )
                                }
                                SliderItem(
                                    value = SettingsLibrary.CrossfadeDuration.toFloat(),
                                    valueRange = DURATION_MIN..DURATION_MAX,
                                    step = 1f,
                                    onValueChange = { SettingsLibrary.CrossfadeDuration = it.toInt() },
                                    showStepButtons = false
                                )
                            }
                        }
                        ListHeader(content = stringResource(id = R.string.settings_audio_crossfade_duration_desc))
                        GroupSpacer()
                    }
                }
            }
        )
    }
