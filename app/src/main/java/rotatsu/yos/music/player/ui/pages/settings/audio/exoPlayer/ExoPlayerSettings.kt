package rotatsu.yos.music.player.ui.pages.settings.audio.exoPlayer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import rotatsu.yos.music.player.R
import rotatsu.yos.music.player.data.libraries.SettingsLibrary
import rotatsu.yos.music.player.ui.UI
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
fun ExoPlayerSettings(navController: NavController) =
    SettingBackground {
        Title(title = stringResource(id = R.string.settings_audio_exoplayer),
            subTitle = stringResource(id = R.string.settings_audio_exoplayer_sub),
            onBack = {
                navController.popBackStack()
            },
            content = {
                item("settings") {
                    Column(Modifier.fillMaxSize()) {

                        ListHeader(stringResource(id = R.string.settings_audio_exoplayer_behaviors))
                        RoundColumn {
                            SwitchItem(
                                title = stringResource(id = R.string.settings_audio_exoplayer_behaviors_audio_attributes),
                                // desc = stringResource(id = R.string.settings_audio_exoplayer_behaviors_audio_attributes_desc),
                                onClick = {
                                    SettingsLibrary.AudioAttributes =
                                        !SettingsLibrary.AudioAttributes
                                },
                                checkedLambda = { SettingsLibrary.AudioAttributes }
                            )
                        }
                        ListHeader(content = stringResource(id = R.string.settings_audio_exoplayer_behaviors_audio_attributes_desc))

                        GroupSpacer()

                        ListHeader(stringResource(id = R.string.settings_audio_exoplayer_decode))
                        RoundColumn {
                            SelectItem(
                                title = stringResource(id = R.string.settings_audio_exoplayer_decode_codec),
                                items = listOf(
                                    "Auto",
                                    "FFmpeg",
                                    "System"
                                ),
                                value = SettingsLibrary.Codec,
                                onValueChange = {
                                    SettingsLibrary.Codec = it
                                }
                            )

                            Divider()

                            LabelItem(title = stringResource(id = R.string.settings_audio_exoplayer_support_mediacodec)) {
                                navController.navigate(UI.Settings.MediaCodec)
                            }

                            Divider()

                            SwitchItem(
                                title = stringResource(id = R.string.settings_audio_exoplayer_decode_hardware_audio_track_playback_params),
                                onClick = {
                                    SettingsLibrary.HardwareAudioTrackPlayBackParams =
                                        !SettingsLibrary.HardwareAudioTrackPlayBackParams
                                },
                                checkedLambda = { SettingsLibrary.HardwareAudioTrackPlayBackParams }
                            )
                        }

                        GroupSpacerMedium()

                        RoundColumn {
                            SwitchItem(
                                title = stringResource(id = R.string.settings_audio_exoplayer_decode_audio_float_output),
                                // desc = stringResource(id = R.string.settings_audio_exoplayer_decode_audio_float_output_desc),
                                onClick = {
                                    SettingsLibrary.AudioFloatOutput =
                                        !SettingsLibrary.AudioFloatOutput
                                },
                                checkedLambda = { SettingsLibrary.AudioFloatOutput }
                            )
                        }
                        ListHeader(content = stringResource(id = R.string.settings_audio_exoplayer_decode_audio_float_output_desc))

                        GroupSpacer()
                    }

                }
            }
        )
    }
