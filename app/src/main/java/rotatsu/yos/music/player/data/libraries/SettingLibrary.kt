package rotatsu.yos.music.player.data.libraries

import android.os.Build
import androidx.compose.runtime.Stable
import com.funny.data_saver.core.mutableDataSaverStateOf
import rotatsu.yos.music.player.data.SettingsSaver

@Stable
object SettingsLibrary {

    /**
     * 是否显示音量条
     */
    @Stable
    var NowPlayingShowVolumeBar by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_performance_ui_nowplaying_show_volume_bar",
        initialValue = true
    )

    /**
     * 应用主题
     */
    @Stable
    var CustomTheme by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_performance_ui_theme",
        initialValue = "Auto"
    )

    /**
     * 是否已设置过屏幕圆角大小
     */
    @Stable
    var ScreenCornerSet by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_performance_ui_corner_set",
        initialValue = false
    )

    /**
     * 屏幕圆角大小
     */
    @Stable
    var ScreenCorner by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_performance_ui_corner",
        initialValue = "30"
    )

    /**
     * 歌曲排序
     */
    @Stable
    var SongSort by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "yos_player_song_sort",
        initialValue = SongSortEnum.MUSIC_TITLE.ordinal
    )

    @Stable
    enum class SongSortEnum {
        MUSIC_TITLE, MUSIC_DURATION, ARTIST_NAME, MODIFIED_DATE
    }

    /**
     * 启用降序
     */
    @Stable
    var EnableDescending by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "yos_player_enable_descending",
        initialValue = false
    )

    /**
     * 歌词界面 - 翻译
     */
    @Stable
    var NowPlayingTranslation by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "now_playing_translation",
        initialValue = true
    )

    /**
     * 每次启动时刷新媒体库
     */
    @Stable
    var RefreshEveryTime by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_library_refresh_everytime",
        initialValue = true
    )

    /**
     * 歌词字体字重
     */
    @Stable
    var LyricFontWeight by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_performance_lyric_font_weight",
        initialValue = "ExtraBold"
    )

    /**
     * 歌词平衡行模式
     */
    @Stable
    var LyricLineBalance by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_performance_lyric_line_balance",
        initialValue = false
    )

    /**
     * 歌词模糊效果
     */
    @Stable
    var LyricBlurEffect by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_performance_lyric_blur_effect",
        initialValue = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    )

    /** Lyric text size as a multiple of the default. */
    @Stable
    var LyricFontScale by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_performance_lyric_font_scale",
        initialValue = 1f
    )

    /** Path of the copied custom lyric font, empty for the system font. */
    @Stable
    var LyricFontPath by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_performance_lyric_font_path",
        initialValue = ""
    )

    /** File name of the custom lyric font, shown in settings. */
    @Stable
    var LyricFontName by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_performance_lyric_font_name",
        initialValue = ""
    )

    /** Spread lines that have no word timing evenly across their time. */
    @Stable
    var LyricSmartWordByWord by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_performance_lyric_smart_word_by_word",
        initialValue = false
    )

    /** Shifts every lyric in milliseconds, positive values show lyrics earlier. */
    @Stable
    var LyricTimingOffset by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_performance_lyric_timing_offset",
        initialValue = 0
    )

    /** Read lyrics from the song's tags before a lyric file next to it. */
    @Stable
    var LyricPreferEmbedded by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_performance_lyric_prefer_embedded",
        initialValue = false
    )

    /** Draw the transliteration under the lyric line instead of above it. */
    @Stable
    var LyricTransliterationBelow by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_performance_lyric_transliteration_below",
        initialValue = false
    )

    /**
     * 播放界面背景动态效果
     */
    @Stable
    var NowplayingBackgroundEffect by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_performance_ui_nowplaying_background_effect",
        initialValue = false
    )

    @Stable
    var FullScreenCover by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_performance_ui_full_screen_cover",
        initialValue = false
    )

    /**
     * 界面工具栏模糊效果
     */
    @Stable
    var BarBlurEffect by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_performance_ui_blur_effect",
        initialValue = false
    )

    /**
     * 媒体通知-额外的媒体图标
     */
    @Stable
    var NotificationEnableIcon by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_performance_notification_enable_icon",
        initialValue = true
    )

    /**
     * 媒体通知-小一号图标
     */
    @Stable
    var NotificationSmallerIcon by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_performance_notification_smaller_icon",
        initialValue = false
    )

    /** Overlap the end of a song with the start of the next one. */
    @Stable
    var CrossfadeEnabled by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_audio_crossfade_enabled",
        initialValue = false
    )

    /** Length of the crossfade in seconds. */
    @Stable
    var CrossfadeDuration by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_audio_crossfade_duration",
        initialValue = 4
    )

    /**
     * 渐入渐出播放
     */
    @Stable
    var FadePlay by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_audio_fade_in_out",
        initialValue = true
    )

    /**
     * 播放历史
     */
    @Stable
    var ListenHistory by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_play_history",
        initialValue = true
    )

    /**
     * 状态栏歌词
     */
    @Stable
    var StatusBarLyricEnabled by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "statusBarLyricEnabled",
        initialValue = false
    )

    /**
     * 状态栏歌词 Hook 状态
     */
    @Stable
    var StatusBarLyricHooked by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "statusBarLyricHooked",
        initialValue = false
    )

    /**
     * ExoPlayer行为 - 音频属性
     */
    @Stable
    var AudioAttributes by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_audio_exoplayer_audio_attributes",
        initialValue = true
    )

    /**
     * ExoPlayer解码 - 编解码器
     */
    @Stable
    var Codec by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_audio_exoplayer_codec",
        initialValue = "Auto"
    )

    /**
     * ExoPlayer解码 - 硬件音频轨道播放参数
     */
    @Stable
    var HardwareAudioTrackPlayBackParams by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_audio_exoplayer_hardware_audio_track_playback_params",
        initialValue = false
    )

    /**
     * ExoPlayer解码 - 音频浮点输出
     */
    @Stable
    var AudioFloatOutput by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_audio_exoplayer_audio_float_output",
        initialValue = false
    )

    /**
     * 排除一分钟以内的歌曲
     */
    @Stable
    var EnableExcludeSongsUnderOneMinute by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_library_enable_exclude_songs_under_one_minute",
        initialValue = true
    )
}
