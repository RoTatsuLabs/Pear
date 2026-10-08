package rotatsu.yos.music.player.ui.pages

import android.graphics.drawable.Drawable
import androidx.compose.foundation.clickable
import rotatsu.yos.music.player.data.libraries.PlayHistoryList
import rotatsu.yos.music.player.data.libraries.PlayHistory
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Icon
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.navigation.NavController
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.PersonCropCircle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import rotatsu.yos.music.player.R
import rotatsu.yos.music.player.code.MediaController
import rotatsu.yos.music.player.code.utils.others.BitmapResolver
import rotatsu.yos.music.player.data.libraries.MusicLibrary
import rotatsu.yos.music.player.data.libraries.YosMediaItem
import rotatsu.yos.music.player.data.libraries.artistsName
import rotatsu.yos.music.player.data.libraries.defaultAlbum
import rotatsu.yos.music.player.data.libraries.defaultArtistsName
import rotatsu.yos.music.player.data.libraries.defaultTitle
import rotatsu.yos.music.player.data.models.ImageViewModel
import rotatsu.yos.music.player.ui.UI
import rotatsu.yos.music.player.ui.theme.YosRoundedCornerShape
import rotatsu.yos.music.player.ui.toUI
import rotatsu.yos.music.player.ui.widgets.effects.imageResolve
import rotatsu.yos.music.player.ui.widgets.basic.Title
import rotatsu.yos.music.player.ui.widgets.basic.YosWrapper

@Composable
fun Home(
    navController: NavController,
    imageViewModel: ImageViewModel
) = Title(
    title = stringResource(id = R.string.page_home_title),
        rightIcon = CupertinoIcons.Default.PersonCropCircle,
        onRightIcon = {
            navController.toUI(UI.Settings.Main)
        },
        content = {
            item("RecommendCard") {
                RecommendCard(imageViewModel)
            }
            item("RecentlyPlayed") {
                RecentlyPlayedRow(navController)
            }
        })

@Composable
fun RecommendCard(imageViewModel: ImageViewModel) {
    val musicList = runCatching { MusicLibrary.songs }.getOrDefault(emptyList())

    val showRecommend = remember(musicList/*,"MainActivity_showRecommend"*/) {
        derivedStateOf { musicList.isNotEmpty() }
    }

    if (showRecommend.value) {
        val randomMusicList = remember("RecommendCard_randomMusicList") {
            imageViewModel.recommendMusicList
        }

        YosWrapper {
            LaunchedEffect(showRecommend.value) {
                if (randomMusicList.value.isNotEmpty()) return@LaunchedEffect
                randomMusicList.value = musicList.shuffled().take(5)
            }
        }

        val pagerState = rememberPagerState(pageCount = { randomMusicList.value.size })

        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        ) {
            Text(
                text = stringResource(id = R.string.home_recommend_title),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            val scope = rememberCoroutineScope()


            HorizontalPager(
                state = pagerState,
                pageSize = PageSize.Fixed(FeaturedCardWidth + 14.dp),
                contentPadding = PaddingValues(start = 20.dp, end = 120.dp),
                key = { randomMusicList.value[it] },
                beyondViewportPageCount = 5
            ) { page ->
                val music = randomMusicList.value[page]
                RecommendCardItem(subTitle = stringResource(id = R.string.home_recommend_subtitle),
                    music = music,
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            MediaController.prepare(
                                music,
                                randomMusicList.value
                            )
                        }
                    })
            }
        }
    }
}

@Composable
fun RecommendCardItem(subTitle: String, music: YosMediaItem, onClick: () -> Unit) {
    val drawable = remember(music.thumb) {
        mutableStateOf<Drawable?>(null)
    }

    val context = LocalContext.current
    val imageLoader = ImageLoader(context)
    YosWrapper {
        LaunchedEffect(Unit) {
            if (music.thumb == null) return@LaunchedEffect

            delay(200)
            val request = ImageRequest.Builder(context)
                .data(music.thumb)
                .build()
            val thisBitmap = imageLoader.execute(request).drawable?.toBitmap()?.run {
                BitmapResolver.bitmapCompress(this, lowQuality = true)
            }

            if (thisBitmap != null) {
                drawable.value = imageResolve(
                    thisBitmap
                ).toDrawable(context.resources)
                thisBitmap.recycle()
            }
            imageLoader.shutdown()
        }
    }

    val shape = YosRoundedCornerShape(20.dp)
    val density = LocalDensity.current
    Box(
        Modifier
            .width(FeaturedCardWidth)
            .height(FeaturedCardHeight)
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                clip = true
                this.shape = shape
            }
            .drawWithCache {
                onDrawWithContent {
                    drawContent()
                    val outline = shape.createOutline(
                        Size(size.width, size.height),
                        LayoutDirection.Ltr,
                        density
                    )
                    drawOutline(
                        outline = outline,
                        color = Color.DarkGray.copy(alpha = 0.08f),
                        style = Stroke(width = 8f)
                    )
                    drawOutline(
                        outline = outline,
                        color = Color.DarkGray.copy(alpha = 0.4f),
                        style = Stroke(width = 8f),
                        blendMode = BlendMode.Overlay
                    )
                }
            }
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(data = music.thumb)
                .crossfade(true).error(R.drawable.placeholder_music_default_artwork)
                .placeholder(R.drawable.placeholder_music_default_artwork)
                .fallback(R.drawable.placeholder_music_default_artwork).build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // The blurred colours of the cover fade in from the bottom, under the text.
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.55f)
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.55f to Color.Black
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }
        ) {
            YosWrapper {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(data = drawable.value).crossfade(true).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    colorFilter = ColorFilter.tint(Color(0x33000000), BlendMode.Overlay)
                )
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            1f to Color(0xAA000000)
                        )
                    )
            )
        }

        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Text(
                text = subTitle,
                fontSize = 15.sp,
                lineHeight = 15.sp,
                color = Color.White,
                modifier = Modifier.alpha(0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = music.title ?: defaultTitle,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                lineHeight = 22.sp,
                modifier = Modifier.padding(top = 4.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = music.artistsName ?: defaultArtistsName,
                color = Color.White,
                fontSize = 15.sp,
                lineHeight = 18.sp,
                modifier = Modifier
                    .alpha(0.6f)
                    .padding(top = 2.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun RecentlyPlayedRow(navController: NavController) {
    val songs = runCatching { MusicLibrary.songs }.getOrDefault(emptyList())
    val ids = PlayHistory.ids
    val recent = remember(songs, ids) { PlayHistoryList.resolve(songs, ids) }
    if (recent.isEmpty()) return

    val scope = rememberCoroutineScope()
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 18.dp, bottom = 10.dp)
    ) {
        Row(
            Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { navController.toUI(UI.RecentlyPlayed) }
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.home_recently_played_title),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                lineHeight = 24.sp
            )
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier
                    .padding(start = 2.dp)
                    .size(26.dp)
                    .alpha(0.6f)
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(top = 14.dp)
        ) {
            items(
                recent.take(RECENT_ROW_LIMIT),
                key = { it.mediaId ?: it.uri.toString() }
            ) { music ->
                val coverShape = YosRoundedCornerShape(14.dp)
                Column(
                    Modifier
                        .width(RecentCoverSize)
                        .clickable {
                            scope.launch(Dispatchers.IO) {
                                MediaController.prepare(music, recent)
                            }
                        }
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(data = music.thumb)
                            .crossfade(true).error(R.drawable.placeholder_music_default_artwork)
                            .placeholder(R.drawable.placeholder_music_default_artwork)
                            .fallback(R.drawable.placeholder_music_default_artwork).build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(RecentCoverSize)
                            .clip(coverShape)
                            .border(0.5.dp, Color.DarkGray.copy(alpha = 0.35f), coverShape)
                    )
                    Text(
                        text = music.title ?: defaultTitle,
                        fontWeight = FontWeight.Medium,
                        fontSize = 17.sp,
                        lineHeight = 21.sp,
                        modifier = Modifier.padding(top = 8.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = music.artistsName ?: defaultArtistsName,
                        fontSize = 15.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.alpha(0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private val FeaturedCardWidth = 238.dp
private val FeaturedCardHeight = 318.dp
private val RecentCoverSize = 155.dp
private const val RECENT_ROW_LIMIT = 12

/*
fun handleImage(
    image: Bitmap
): Bitmap {
    var saturationBitmap = image.copy(Bitmap.Config.ARGB_8888, true)
    saturationBitmap.applyCanvas {
        val paint = Paint()
        paint.isAntiAlias = true
        paint.isFilterBitmap = true
        paint.isDither = true
        val saturationMatrix = ColorMatrix()
        saturationMatrix.setSaturation(3f)
        paint.colorFilter = ColorMatrixColorFilter(saturationMatrix)
        drawBitmap(saturationBitmap, 0f, 0f, paint)
        //Color(0x40000000)
        drawColor((0x99000000).toInt(), PorterDuff.Mode.OVERLAY)
        drawColor((0x40000000).toInt())
    }
    saturationBitmap = saturationBitmap.scale(16, 16)
    //saturationBitmap = meshBitmap(saturationBitmap)
    saturationBitmap = Toolkit.blur(saturationBitmap, 25)
    return saturationBitmap
}*/
