package rotatsu.yos.music.player.data.libraries

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayHistoryListTest {

    @Test
    fun theNewestSongGoesFirst() {
        assertEquals(listOf("c", "a", "b"), PlayHistoryList.push(listOf("a", "b"), "c", 10))
    }

    @Test
    fun aSongPlayedAgainMovesUpInsteadOfRepeating() {
        assertEquals(listOf("b", "a", "c"), PlayHistoryList.push(listOf("a", "b", "c"), "b", 10))
    }

    @Test
    fun theListStopsAtTheLimit() {
        assertEquals(listOf("d", "c"), PlayHistoryList.push(listOf("c", "b", "a"), "d", 2))
    }

    @Test
    fun noSongsMeansNothingToShow() {
        assertEquals(emptyList<YosMediaItem>(), PlayHistoryList.resolve(emptyList(), listOf("a")))
    }
}
