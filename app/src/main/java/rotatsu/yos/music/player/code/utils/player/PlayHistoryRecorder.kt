package rotatsu.yos.music.player.code.utils.player

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import rotatsu.yos.music.player.data.libraries.PlayHistory
import rotatsu.yos.music.player.data.libraries.SettingsLibrary

/** Notes every song that actually plays, for the Recently Played lists. */
class PlayHistoryRecorder(private val player: Player) : Player.Listener {

    fun attach() = player.addListener(this)

    fun release() = player.removeListener(this)

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (isPlaying) record(player.currentMediaItem)
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        if (player.isPlaying) record(mediaItem)
    }

    private fun record(item: MediaItem?) {
        if (!SettingsLibrary.ListenHistory) return
        val id = item?.mediaId ?: return
        if (id.isEmpty() || id == MediaItem.DEFAULT_MEDIA_ID) return
        PlayHistory.record(id)
    }
}
