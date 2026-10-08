package rotatsu.yos.music.player.data.libraries

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tencent.mmkv.MMKV

/** The list rules of the play history, kept apart from the storage so they can be tested. */
object PlayHistoryList {
    /** Puts [id] first, drops an earlier entry of it and keeps at most [limit] entries. */
    fun push(list: List<String>, id: String, limit: Int): List<String> =
        (listOf(id) + list.filter { it != id }).take(limit)

    /** The songs of [ids] in that order, skipping ids that are no longer in [songs]. */
    fun resolve(songs: List<YosMediaItem>, ids: List<String>): List<YosMediaItem> {
        val byId = HashMap<String, YosMediaItem>(songs.size)
        for (song in songs) song.mediaId?.let { byId.putIfAbsent(it, song) }
        return ids.mapNotNull { byId[it] }
    }
}

/** The songs played on this device, newest first, for the Recently Played lists. */
object PlayHistory {
    private const val LIMIT = 60
    private const val KEY = "yos_play_history_v1"
    private const val SEPARATOR = "\n"

    private val mmkv: MMKV by lazy { MMKV.mmkvWithID("yos_player_core") }

    var ids: List<String> by mutableStateOf(load())
        private set

    fun record(mediaId: String) {
        if (ids.firstOrNull() == mediaId) return
        ids = PlayHistoryList.push(ids, mediaId, LIMIT)
        save()
    }

    fun clear() {
        ids = emptyList()
        save()
    }

    private fun load(): List<String> =
        mmkv.decodeString(KEY, "").orEmpty().split(SEPARATOR).filter { it.isNotBlank() }

    private fun save() {
        mmkv.encodeString(KEY, ids.joinToString(SEPARATOR))
    }
}
