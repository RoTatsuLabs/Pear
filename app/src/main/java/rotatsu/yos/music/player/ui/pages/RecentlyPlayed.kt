package rotatsu.yos.music.player.ui.pages

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import rotatsu.yos.music.player.R
import rotatsu.yos.music.player.code.MediaController
import rotatsu.yos.music.player.data.libraries.MusicLibrary
import rotatsu.yos.music.player.data.libraries.PlayHistory
import rotatsu.yos.music.player.data.libraries.PlayHistoryList
import rotatsu.yos.music.player.ui.pages.library.MusicList
import rotatsu.yos.music.player.ui.widgets.basic.Title

/** Every song in the play history, newest first. */
@Composable
fun RecentlyPlayed(navController: NavController) {
    val songs = runCatching { MusicLibrary.songs }.getOrDefault(emptyList())
    val ids = PlayHistory.ids
    val recent = remember(songs, ids) { PlayHistoryList.resolve(songs, ids) }
    val scope = rememberCoroutineScope()

    Title(
        title = stringResource(id = R.string.home_recently_played_title),
        onBack = { navController.popBackStack() }
    ) {
        if (recent.isEmpty()) {
            item("empty") {
                Text(
                    text = stringResource(id = R.string.home_recently_played_empty),
                    fontSize = 18.sp,
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .alpha(0.6f)
                )
            }
        } else {
            itemsIndexed(
                recent,
                key = { _, music -> music.mediaId ?: music.uri.toString() }
            ) { _, music ->
                MusicList(music) {
                    scope.launch(Dispatchers.IO) {
                        MediaController.prepare(music, recent)
                    }
                }
            }
        }
    }
}
