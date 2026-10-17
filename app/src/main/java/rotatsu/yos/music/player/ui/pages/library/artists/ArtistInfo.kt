package rotatsu.yos.music.player.ui.pages.library.artists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import rotatsu.yos.music.player.R
import rotatsu.yos.music.player.code.MediaController
import rotatsu.yos.music.player.data.libraries.MusicLibrary
import rotatsu.yos.music.player.data.libraries.YosMediaItem
import rotatsu.yos.music.player.data.libraries.defaultAlbum
import rotatsu.yos.music.player.data.libraries.defaultTitle
import rotatsu.yos.music.player.data.objects.LibraryObject
import rotatsu.yos.music.player.ui.UI
import rotatsu.yos.music.player.ui.pages.library.albums.NormalButton
import rotatsu.yos.music.player.ui.toUI
import rotatsu.yos.music.player.ui.widgets.basic.ImageQuality
import rotatsu.yos.music.player.ui.widgets.basic.ShadowImage
import rotatsu.yos.music.player.ui.widgets.basic.Title
import rotatsu.yos.music.player.ui.widgets.effects.ShadowType
import rotatsu.yos.music.player.ui.theme.withNight

private data class ArtistAlbum(val name: String, val thumb: Any?, val year: Int?)

@Composable
fun ArtistInfo(navController: NavController) {
    val artistName = rememberSaveable(key = "ArtistInfo_artistName") {
        mutableStateOf(LibraryObject.getTargetArtistName())
    }
    val library = MusicLibrary.songs
    val songs = remember(library, artistName.value) {
        MusicLibrary.Artist[artistName.value]
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title ?: defaultTitle })
    }

    if (artistName.value.isEmpty() || songs.isEmpty()) {
        Title(
            title = artistName.value.ifEmpty { stringResource(id = R.string.page_library_artists) },
            onBack = { navController.popBackStack() }
        ) {
            item("tip_no_song") {
                Text(
                    text = stringResource(id = R.string.tip_no_song),
                    fontSize = 18.sp,
                    modifier = Modifier
                        .alpha(0.6f)
                        .padding(horizontal = 20.dp)
                )
            }
        }
        return
    }

    val albums = remember(songs) {
        songs.groupBy { it.album ?: defaultAlbum }.map { (name, items) ->
            ArtistAlbum(
                name = name,
                thumb = items.firstNotNullOfOrNull { it.thumb },
                year = items.firstNotNullOfOrNull { it.releaseYear ?: it.recordingYear }
            )
        }.sortedWith(compareBy({ -(it.year ?: 0) }, { it.name }))
    }
    val scope = rememberCoroutineScope()

    Title(
        title = artistName.value,
        onBack = { navController.popBackStack() }
    ) {
        item("ArtistHeader") {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .padding(top = 4.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ArtistAvatar(data = songs.firstNotNullOfOrNull { it.thumb }, size = 132.dp)
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(
                        id = R.string.page_library_artist_desc,
                        songs.size,
                        albums.size
                    ),
                    fontSize = 15.sp,
                    modifier = Modifier.alpha(0.5f)
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                ) {
                    NormalButton(
                        icon = painterResource(id = R.drawable.button_icon_play),
                        label = stringResource(id = R.string.normal_button_play),
                        modifier = Modifier.weight(1f)
                    ) {
                        scope.launch(Dispatchers.IO) {
                            MediaController.prepare(songs.first(), songs)
                        }
                    }
                    Spacer(Modifier.width(15.dp))
                    NormalButton(
                        icon = painterResource(id = R.drawable.button_icon_shuffle),
                        label = stringResource(id = R.string.normal_button_shuffle),
                        modifier = Modifier.weight(1f)
                    ) {
                        MediaController.mediaControl?.shuffleModeEnabled = true
                        scope.launch(Dispatchers.IO) {
                            MediaController.prepare(songs.random(), songs)
                        }
                    }
                }
            }
        }

        item("AlbumsHeader") {
            SectionHeader(stringResource(id = R.string.page_library_albums))
        }
        item("Albums") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(albums, key = { it.name }) { album ->
                    Column(
                        Modifier
                            .width(132.dp)
                            .clickable {
                                LibraryObject.setTargetAlbumName(album.name)
                                navController.toUI(UI.AlbumInfo)
                            }
                    ) {
                        ShadowImage(
                            dataLambda = { album.thumb },
                            contentDescription = null,
                            modifier = Modifier
                                .size(132.dp)
                                .aspectRatio(1f),
                            shadowAlpha = 0.12f,
                            shadowType = ShadowType.Small,
                            cornerRadius = 8.dp,
                            imageQuality = ImageQuality.LOW
                        )
                        Text(
                            text = album.name,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                        album.year?.takeIf { it > 0 }?.let {
                            Text(
                                text = it.toString(),
                                fontSize = 12.sp,
                                modifier = Modifier.alpha(0.4f)
                            )
                        }
                    }
                }
            }
        }

        item("SongsHeader") {
            SectionHeader(stringResource(id = R.string.page_library_songs))
        }
        itemsIndexed(songs, key = { _, music -> music }) { index, music ->
            ArtistSongItem(music = music) {
                scope.launch(Dispatchers.IO) {
                    MediaController.prepare(music, songs)
                }
            }
            if (index < songs.size - 1) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 76.dp, end = 18.dp)
                        .alpha(0.2f)
                        .height(0.5.dp)
                        .background(Color.Black withNight Color.White)
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        fontSize = 20.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 10.dp)
    )
}

@Composable
private fun ArtistSongItem(music: YosMediaItem, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShadowImage(
            dataLambda = { music.thumb },
            contentDescription = null,
            modifier = Modifier.size(44.dp),
            shadowAlpha = 0.08f,
            shadowType = ShadowType.Small,
            cornerRadius = 6.dp,
            imageQuality = ImageQuality.LOW
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = music.title ?: defaultTitle,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )
            Text(
                text = music.album ?: defaultAlbum,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp,
                modifier = Modifier.alpha(0.4f)
            )
        }
    }
}

@Composable
fun ArtistAvatar(data: Any?, size: Dp, modifier: Modifier = Modifier) {
    val shape = CircleShape
    val density = LocalDensity.current
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(data = data).crossfade(true)
            .error(R.drawable.songcredits_monogram_person)
            .placeholder(R.drawable.songcredits_monogram_person)
            .fallback(R.drawable.songcredits_monogram_person)
            .allowHardware(true)
            .precision(Precision.INEXACT)
            .size(if (size > 100.dp) 512 else 192)
            .build(),
        contentDescription = "Artist_Image",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .aspectRatio(1f)
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                clip = true
                this.shape = shape
            }
            .drawWithCache {
                onDrawWithContent {
                    drawContent()
                    val outline = shape.createOutline(
                        Size(this.size.width, this.size.height),
                        LayoutDirection.Ltr,
                        density
                    )
                    drawOutline(
                        outline = outline,
                        color = Color.DarkGray.copy(alpha = 0.08f),
                        style = Stroke(width = 6f)
                    )
                    drawOutline(
                        outline = outline,
                        color = Color.DarkGray.copy(alpha = 0.4f),
                        style = Stroke(width = 6f),
                        blendMode = BlendMode.Overlay
                    )
                }
            }
    )
}
