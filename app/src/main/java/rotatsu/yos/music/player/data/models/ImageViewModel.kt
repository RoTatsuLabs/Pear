package rotatsu.yos.music.player.data.models

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import rotatsu.yos.music.player.data.libraries.YosMediaItem

class ImageViewModel : ViewModel() {
    val recommendMusicList: MutableState<List<YosMediaItem>> = mutableStateOf(emptyList())
}
