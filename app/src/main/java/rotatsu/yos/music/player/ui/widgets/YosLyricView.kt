package rotatsu.yos.music.player.ui.widgets

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseInOutQuad
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import rotatsu.yos.music.player.code.utils.lrc.YosGlowWord
import rotatsu.yos.music.player.code.utils.lrc.YosLyricCredit
import rotatsu.yos.music.player.code.utils.lrc.YosLyricGlow
import rotatsu.yos.music.player.code.utils.lrc.YosLyricStack
import rotatsu.yos.music.player.code.utils.lrc.YosLyricStyleSpec
import rotatsu.yos.music.player.code.utils.lrc.YosLyricStyles
import rotatsu.yos.music.player.code.utils.lrc.YosLyricTiming
import rotatsu.yos.music.player.code.utils.lrc.YosTextShaping
import rotatsu.yos.music.player.code.utils.lrc.YosWordTiming
import rotatsu.yos.music.player.code.utils.lrc.YosSyllables
import rotatsu.yos.music.player.code.utils.lrc.YosMediaEvent
import rotatsu.yos.music.player.code.utils.lrc.YosUIConfig
import rotatsu.yos.music.player.code.utils.others.Vibrator
import rotatsu.yos.music.player.data.libraries.SettingsLibrary
import rotatsu.yos.music.player.data.objects.MainViewModelObject
import rotatsu.yos.music.player.data.objects.MediaViewModelObject
import rotatsu.yos.music.player.ui.widgets.basic.YosWrapper
import kotlin.math.abs
import kotlin.math.roundToInt

val yosEasing = CubicBezierEasing(0.75f, 0.0f, 0.25f, 1.0f)
private val LyricEasing = CubicBezierEasing(0.41f, 0f, 0.12f, 0.99f)

private class ScrollRun(val id: Int, val delta: Float, val durationMs: Int)

/**
 * YosLyricView 主控件
 * @param lrcEntriesLambda 处理完毕的 Lrc 文本
 * @param liveTimeLambda 当前歌曲进度
 * @param mediaEvent YosLyricView 媒体事件
 * @param translationLambda 是否开启翻译
 * @param blurLambda 是否启用模糊效果
 * @param uiConfig YosLyricView UI 控制，仅管理在日常使用中不经常调节的选项
 */
@Composable
fun YosLyricView(
    //mediaViewModel: MediaViewModel,
    lrcEntriesLambda: () -> List<List<Pair<Float, String>>>,
    liveTimeLambda: () -> Int,
    mediaEvent: YosMediaEvent,
    translationLambda: () -> Boolean = { true },
    blurLambda: () -> Boolean = { false },
    //animationConfig: YosAnimationConfig = YosAnimationConfig(),
    uiConfig: YosUIConfig = YosUIConfig(),
    weightLambda: () -> Boolean,
    modifier: Modifier,
    onBackClick: () -> Unit
) {
    println("重组：YosLyricView")
    val context = LocalContext.current
    val mainTextBasicColor = Color(uiConfig.mainTextBasicColor)
    val subTextBasicColor = Color(uiConfig.subTextBasicColor)
    //Color(0xFF919191)
    val otherSideForLines = MediaViewModelObject.otherSideForLines

    val lrcEntries = lrcEntriesLambda()

    // Transliterations and credits are only used together with the entries they were published with.
    val lyricsData = MediaViewModelObject.lyrics.value
    val lyricsMatch = lyricsData.entries === lrcEntries
    val credits = if (lyricsMatch) lyricsData.credits else emptyList()

    val style = YosLyricStyles.of(SettingsLibrary.LyricStyle)
    val styleEasing = remember(style) { style.easing() }

    //val thisLyricLines = MediaViewModelObject.mainLyricLines
    if (lrcEntries.isEmpty() || otherSideForLines.isEmpty() /*|| thisLyricLines.isEmpty()*/) {
        println(
            lrcEntries.isEmpty()
                .toString() + otherSideForLines.isEmpty()/* + thisLyricLines.isEmpty()*/
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxHeight(if (weightLambda()) 0.56f else 1f)
                .fillMaxWidth()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }) {
                    onBackClick()
                }
        ) {
            Text(
                text = uiConfig.noLrcText,
                fontSize = 18.sp,
                color = Color(uiConfig.mainTextBasicColor)
            )
        }
    } else {
        val scrollState = rememberLazyListState()
        val currentLyricIndex =
            remember("YosLyricView_currentLyricIndex") { MainViewModelObject.syncLyricIndex }
        /*val noAnimateItems by remember {
            derivedStateOf { scrollState.layoutInfo.totalItemsCount - scrollState.layoutInfo.visibleItemsInfo.size - 1 }
        }
        val showAnimate by remember {
            derivedStateOf {
                currentLyricIndex in scrollState.layoutInfo.visibleItemsInfo.map { it.index - 1 } && currentLyricIndex > 0 && currentLyricIndex < noAnimateItems
            }
        }*/
        val blankSpacer: (LazyListScope.() -> Unit) = {
            item {
                Box(
                    modifier = Modifier
                        .height(uiConfig.blankHeight.dp)
                ) {
                }
            }
        }
        //val coroutineScope = rememberCoroutineScope()
        val enableLyricScroll = remember("YosLyricView_enableLyricScroll") {
            mutableStateOf(true)
        }
        /*val lastClickTime = rememberSaveable(key = "YosLyricView_lastClickTime") {
            mutableLongStateOf(0L)
        }*/

        /*YosWrapper {
            LaunchedEffect(enableLyricScroll.value, lastClickTime.longValue) {
                if (!enableLyricScroll.value) {
                    val time = 1500L
                    delay(time)
                    withContext(Dispatchers.Main) {
                        if (TimeUtils.getNowMills() - lastClickTime.longValue >= time) {
                            enableLyricScroll.value = true
                        }
                    }
                }
            }
        }*/

        val height = rememberSaveable(key = "YosLyricView_height") { mutableIntStateOf(0) }

        val targetWeight = style.anchor
        val targetOffset = rememberSaveable(height.intValue, targetWeight, key = "YosLyricView_targetOffset") {
            //println("计算边距使用：${height.intValue}")
            //println("计算边距为：${height.intValue * targetWeight}")
            height.intValue * targetWeight
        }
        // 顶部边距

        // 行距

        val measurer = rememberTextMeasurer(
            cacheSize = 32
        )

        val scrollRun = remember { mutableStateOf(ScrollRun(0, 0f, YosLyricStack.SETTLE_MS)) }
        val sinceRun = remember { Animatable(0f) }
        val focusIndex = remember { mutableIntStateOf(currentLyricIndex.intValue) }

        val visibleItems = remember("YosLyricView_visibleItems") {
            derivedStateOf {
                scrollState.layoutInfo.visibleItemsInfo
            }
        }
        val targetItem = remember("YosLyricView_targetItem") {
            derivedStateOf {
                visibleItems.value.find {
                    it.index == focusIndex.intValue + 1
                }
            }
        }
        val currentOffset = remember("YosLyricView_currentOffset", targetOffset) {
            derivedStateOf {
                targetItem.value?.offset ?: targetOffset.toInt()
            }
        }
        val scrollDistance = remember("YosLyricView_scrollDistance", targetOffset) {
            derivedStateOf {
                currentOffset.value - targetOffset
            }
        }
        val supportBlur = rememberSaveable(key = "supportBlur") {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        }

        val isUserScrolling = remember { mutableStateOf(false) }
        val nestedScrollConnection = remember {
            @Stable
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    isUserScrolling.value = true
                    return Offset.Zero
                }

                override suspend fun onPostFling(
                    consumed: Velocity,
                    available: Velocity
                ): Velocity {
                    isUserScrolling.value = false
                    return super.onPostFling(consumed, available)
                }
            }
        }

        YosWrapper {
            LaunchedEffect(isUserScrolling.value) {
                if (isUserScrolling.value) {
                    enableLyricScroll.value = false
                } else {
                    delay(1600)
                    enableLyricScroll.value = true
                }
            }
        }

        YosWrapper {
            LazyColumn(
                state = scrollState,
                contentPadding = PaddingValues(vertical = 16.dp),/*
            verticalArrangement = Arrangement.spacedBy(5.dp),*/
                modifier =
                modifier
                    .fillMaxSize()
                    /*.drawWithCache {
                        onDrawWithContent {
                            val colors = if (weightLambda()) {
                                listOf(
                                    Color.Transparent,
                                    Color(0x59000000),
                                    Color.Black,
                                    Color.Black,
                                    Color.Black,
                                    Color.Black,
                                    Color.Black,
                                    Color.Black,
                                    Color(0x59000000),
                                    Color(0x21000000),
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Transparent
                                )
                            } else {
                                listOf(
                                    Color.Transparent,
                                    Color(0x59000000),
                                    Color.Black,
                                    Color.Black,
                                    Color.Black,
                                    Color.Black,
                                    Color.Black,
                                    Color.Black,
                                    Color.Black,
                                    Color.Black,
                                    Color.Black,
                                    Color.Black,
                                    Color.Black,
                                    Color.Black,
                                    Color.Black,
                                    Color.Black,
                                    Color(0x59000000),
                                    Color(0x3F000000),
                                    Color(0x21000000),
                                )
                            }

                            drawContent()

                            drawRect(
                                brush = Brush.verticalGradient(colors),
                                blendMode = BlendMode.DstIn
                            )
                        }
                    }*/
                    /*.scrollable(state = rememberScrollableState {
                        enableLyricScroll.value = false
                        lastClickTime.longValue =
                            TimeUtils.getNowMills()
                        it
                    }, orientation = Orientation.Vertical)*/
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }) {
                        onBackClick()
                    }
                    .nestedScroll(nestedScrollConnection)
                    .onSizeChanged {
                        if (height.intValue == 0 && it.height != 0) {
                            height.intValue = it.height
                            //println("计算歌词视图高度：${height.intValue}")
                        }
                    }
            ) {
                //println("重组：歌词列表")
                blankSpacer()
                itemsIndexed(
                    items = lrcEntries,
                    key = { _, lines -> lines }/*,
                contentType = { _, _ -> "YosLyricView_item" }*/
                ) { index, lines ->
                    val isCurrent = remember(lines) {
                        derivedStateOf {
                            index == currentLyricIndex.intValue
                        }
                    }

                    val isTop = remember(lines) {
                        derivedStateOf {
                            index == (currentLyricIndex.intValue - 1)
                        }
                    }

                    val showStateAnimation = remember(index) {
                        derivedStateOf {
                            (currentLyricIndex.intValue in scrollState.layoutInfo.visibleItemsInfo.map { it.index - 1 } && currentLyricIndex.intValue >= 0) && enableLyricScroll.value
                        }
                    }

                    val isLyricEmpty = rememberSaveable(lines) {
                        mutableStateOf(
                            lines.all { it.second.isBlank() }
                        )
                    }

                    key(lines) {
                        val translation = remember(index) {
                            val str = lines.last().second
                            str.ifBlank { null }
                        }

                        val blur = remember(index, style) {
                            derivedStateOf {
                                if (!showStateAnimation.value || !enableLyricScroll.value || index == currentLyricIndex.intValue || !blurLambda() || !supportBlur) {
                                    0f
                                } else {
                                    style.blurDp(abs(index - currentLyricIndex.intValue))
                                }
                            }
                        }

                        val otherSide = remember(index) {
                            otherSideForLines.getOrElse(index) { false }
                        }

                        YosWrapper {
                            LyricItem(
                                isCurrentLambda = {
                                    isCurrent.value
                                },
                                isTopLambda = {
                                    isTop.value
                                },
                                mainLyric = lines.dropLast(1),
                                translation,
                                translationLambda(),
                                transliteration = if (lyricsMatch) {
                                    lyricsData.transliterations.getOrNull(index)
                                } else {
                                    null
                                },
                                //mainTextSize = uiConfig.mainTextSize,
                                subTextSize = uiConfig.subTextSize,
                                blur = { blur.value },
                                mainTextBasicColor,
                                subTextBasicColor,
                                otherSide = otherSide,
                                liveTimeLambda = liveTimeLambda,
                                measurer = measurer,
                                isLyricEmpty = { isLyricEmpty.value },
                                distanceLambda = { abs(index - currentLyricIndex.intValue) },
                                browsingLambda = { !enableLyricScroll.value },
                                staggerLambda = {
                                    if (!style.stagger) {
                                        0f
                                    } else {
                                        val run = scrollRun.value
                                        val behind = if (run.delta >= 0f) {
                                            index - focusIndex.intValue
                                        } else {
                                            focusIndex.intValue - index
                                        }
                                        YosLyricStack.stagger(
                                            run.delta,
                                            sinceRun.value,
                                            run.durationMs,
                                            behind,
                                            styleEasing::transform
                                        )
                                    }
                                },
                                style = style,
                                nextTime = {
                                    if (index + 1 > lrcEntries.size - 1) {
                                        0f
                                    } else {
                                        lrcEntries[(index + 1)].first().first
                                    }
                                }
                            ) {
                                Vibrator.doubleClick(context)
                                currentLyricIndex.intValue = index
                                focusIndex.intValue = index
                                mediaEvent.onSeek(lines.first().first.toInt())
                            }
                        }
                    }
                }
                if (credits.isNotEmpty()) {
                    item("yos_credits") {
                        LyricCredits(credits, mainTextBasicColor)
                    }
                }
                blankSpacer()
                item("extra_blank") {
                    Spacer(modifier = Modifier.height(500.dp))
                }
            }
        }

        YosWrapper {
            LaunchedEffect(lrcEntries, style) {
                val starts = lrcEntries.map { it.first().first }
                while (true) {
                    val real = currentLyricIndex.intValue
                    val next = lrcEntries.getOrNull(real + 1)
                    val ahead = if (next == null) {
                        real
                    } else {
                        val gap = YosLyricStack.gapBefore(lrcEntries, real + 1)
                        YosLyricStack.focusIndex(
                            starts,
                            liveTimeLambda() + style.scrollLead(gap).toFloat()
                        )
                    }
                    focusIndex.intValue = if (ahead == real + 1) ahead else real
                    delay(30)
                }
            }
        }

        YosWrapper {
            LaunchedEffect(scrollRun.value.id) {
                val span = scrollRun.value.durationMs * 1.3f
                sinceRun.snapTo(0f)
                sinceRun.animateTo(span, tween(span.toInt(), easing = LinearEasing))
            }
        }

        YosWrapper {
            LaunchedEffect(focusIndex.intValue, translationLambda()) {
                try {
                    if (enableLyricScroll.value) {
                        /*visibleItems = scrollState.layoutInfo.visibleItemsInfo
                        targetItem =
                            visibleItems.find { it.index == currentLyricIndex.intValue */
                        /** 2*/
                        /** 2*//* + 1 }*/
                        if (
                            try {
                                if (focusIndex.intValue - 1 < 0) false
                                else (
                                        (lrcEntries[(focusIndex.intValue - 1)][1].second.isBlank())
                                        /*&&
                                        (lrcEntries[(currentLyricIndex.intValue).coerceAtLeast(
                                            0
                                        )].first().first - lrcEntries[(currentLyricIndex.intValue - 1)].first().first > 900f)*/)
                                // 这里有一个特殊的更改，因为AppleMusic歌词转过来会有两个连续一样的时间轴，在LrcFactory有更改，下面的那个900不用管
                                // 已经作了规范处理

                            } catch (_: Exception) {
                                false
                            }
                        ) {
                            return@LaunchedEffect
                        }

                        if (targetItem.value != null /*|| lifecycleState.value.isAtLeast(Lifecycle.State.RESUMED)*/) {
                            /*currentOffset.value = targetItem.value?.offset?:targetOffset.toInt()
                            scrollDistance.value = currentOffset - targetOffset*/
                            val focus = focusIndex.intValue
                            val span = style.scrollLead(YosLyricStack.gapBefore(lrcEntries, focus))
                            val delta = scrollDistance.value
                            scrollRun.value = ScrollRun(scrollRun.value.id + 1, delta, span)
                            scrollState.animateScrollBy(
                                delta,
                                animationSpec = tween(durationMillis = span, easing = styleEasing)
                            )
                        } else {
                            scrollState.animateScrollToItem(
                                index = (focusIndex.intValue + 1).coerceAtLeast(0),
                                scrollOffset = -targetOffset.toInt()
                            )
                        }
                    }
                } catch (_: Exception) {
                }
            }
        }

        /*YosWrapper {
            LaunchedEffect(Unit) {
                while (true) {
                    val liveTime = liveTimeLambda()
                    val nextIndex = lrcEntries.indexOfFirst { line ->
                        line.first().first > liveTime
                    }

                    if (nextIndex != -1 && nextIndex - 1 != currentLyricIndex.intValue) {
                        currentLyricIndex.intValue = nextIndex - 1
                    } else if (nextIndex == -1 && currentLyricIndex.intValue != lrcEntries.size - 1) {
                        currentLyricIndex.intValue = lrcEntries.size - 1
                    }

                    delay(100)
                }
            }
        }*/

        YosWrapper {
            //val lifecycleState = LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
            LaunchedEffect(Unit) {
                /*if (!lifecycleState.value.isAtLeast(Lifecycle.State.RESUMED)) {
                    return@LaunchedEffect
                }*/
                try {
                    if (currentLyricIndex.intValue != -1) {
                        return@LaunchedEffect
                    }
                    val liveTime = liveTimeLambda()
                    val nextIndex = lrcEntries.indexOfFirst { line ->
                        line.first().first > liveTime
                    }

                    if (nextIndex != -1 && nextIndex - 1 != currentLyricIndex.intValue) {
                        scrollState.scrollToItem(
                            index = (nextIndex).coerceAtLeast(0),
                            scrollOffset = -targetOffset.toInt()
                        )
                        currentLyricIndex.intValue = nextIndex - 1
                    } else if (nextIndex == -1 && currentLyricIndex.intValue != lrcEntries.size - 1) {
                        scrollState.scrollToItem(
                            index = (lrcEntries.size).coerceAtLeast(0),
                            scrollOffset = -targetOffset.toInt()
                        )
                        currentLyricIndex.intValue = lrcEntries.size - 1
                    }
                } catch (_: Exception) {
                }

            }
        }
    }
}

/*@Composable
fun Dp.toPx(): Float {
    val density = LocalDensity.current
    return this.value * density.density
}*/

@Composable
fun Float.toDp(): Dp {
    val density = LocalDensity.current
    return (this / density.density).dp
}

@Composable
private fun LazyItemScope.Line(
    lines: List<Pair<Float, String>>,
    style: TextStyle,
    measurer: TextMeasurer,
    modifier: Modifier,
    viewAlign: Alignment.Horizontal,
    glowRadius: Dp = GlowRadius,
    glow: (CacheDrawScope.(TextLayoutResult) -> DrawResult)? = null,
    draw: CacheDrawScope.(Constraints, TextLayoutResult) -> DrawResult
) =
    YosWrapper {
        /*val styledString = remember(style, lines) {
            buildAnnotatedString {
                lines.forEachIndexed { _, char ->
                    if (char.second.isNotEmpty()) {
                        withStyle(style.toSpanStyle()) {
                            append(char.second)
                        }
                    }
                }
            }
        }*/

        val styledString = remember(style, lines) {
            buildString {
                lines.forEach { char ->
                    if (char.second.isNotEmpty()) {
                        append(char.second)
                    }
                }
            }
        }


        Column(
            horizontalAlignment = viewAlign,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.ModulateAlpha
                }
        ) {
            SubcomposeLayout(modifier = modifier) { constraints ->

                val rtl = YosTextShaping.isRtl(styledString)
                val measureResult = measurer.measure(
                    text = styledString,
                    style = style,
                    // A right to left line is measured over the full width, so its right
                    // alignment has room to work in.
                    constraints = Constraints(
                        minWidth = if (rtl && constraints.hasBoundedWidth) constraints.maxWidth else 0,
                        maxWidth = constraints.maxWidth,
                    ),
                    layoutDirection = if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                )

                val height = (style.lineHeight * measureResult.lineCount)

                // The last character is not the right edge of a line that has right to left
                // letters in it, so such a line is measured over all of its characters.
                val width = when {
                    rtl -> constraints.maxWidth.toFloat()
                    YosTextShaping.needsShapedDraw(styledString) -> runCatching {
                        (0 until measureResult.lineCount).maxOf { line ->
                            (measureResult.getLineStart(line) until
                                    measureResult.getLineEnd(line, visibleEnd = true))
                                .maxOf { measureResult.getBoundingBox(it).right }
                        }
                    }.getOrDefault(constraints.maxWidth.toFloat())

                    else -> runCatching {
                        (0 until measureResult.lineCount).maxOf {
                            measureResult.getBoundingBox(
                                measureResult.getLineEnd(it, visibleEnd = true) - 1
                            ).right
                        }
                    }.getOrDefault(constraints.maxWidth.toFloat())
                }

                val content = subcompose(lines) {
                    Spacer(
                        Modifier
                            .fillMaxSize()
                            .drawWithCache { draw(constraints, measureResult) }
                    )
                }.first()


                val placeable = content.measure(
                    Constraints.fixed(width.roundToInt(), height.roundToPx())
                )

                val room = GlowRoom.roundToPx()
                val glowPlaceable = glow?.let { drawGlow ->
                    subcompose(GlowSlot) {
                        Spacer(
                            Modifier
                                .fillMaxSize()
                                .blur(glowRadius, BlurredEdgeTreatment.Unbounded)
                                .drawWithCache { drawGlow(measureResult) }
                        )
                    }.first().measure(
                        Constraints.fixed(placeable.width + room * 2, placeable.height + room * 2)
                    )
                }

                layout(placeable.width, placeable.height) {
                    glowPlaceable?.place(-room, -room)
                    placeable.place(0, 0)
                }

                /*layout(placeable.width, placeable.height) {
                    placeable.placeRelative(0, 0)
                }*/
            }
        }
    }

/*@Composable
private fun LazyItemScope.Line(
    lines: List<Pair<Float, String>>,
    style: TextStyle,
    measurer: TextMeasurer,
    modifier: Modifier,
    viewAlign: Alignment.Horizontal,
    draw: CacheDrawScope.(Constraints, TextLayoutResult) -> DrawResult
) =
    YosWrapper {
        val styledString = remember(style, lines) {
            buildString {
                lines.forEach { char ->
                    if (char.second.isNotEmpty()) {
                        append(char.second)
                    }
                }
            }
        }

        Column(
            modifier = modifier,
            horizontalAlignment = viewAlign
        ) {
            Layout(
                content = {
                    Spacer(
                        Modifier
                            .fillMaxSize()
                            .drawWithCache {
                                val constraints = Constraints(
                                    minWidth = 0,
                                    maxWidth = size.width.toInt()
                                )
                                val measureResult = measurer.measure(
                                    text = styledString,
                                    style = style,
                                    constraints = constraints
                                )
                                draw(constraints, measureResult)
                            }
                    )
                }
            ) { measurables, constraints ->

                val measureResult = measurer.measure(
                    text = styledString,
                    style = style,
                    constraints = Constraints(
                        minWidth = 0,
                        maxWidth = constraints.maxWidth
                    )
                )

                // 确保高度计算正确，包含所有文本行
                val height = measureResult.size.height

                val width = runCatching {
                    (0 until measureResult.lineCount).maxOf {
                        measureResult.getBoundingBox(
                            measureResult.getLineEnd(it, visibleEnd = true) - 1
                        ).right
                    }
                }.getOrDefault(constraints.maxWidth.toFloat()).roundToInt()

                val placeable = measurables.first().measure(
                    Constraints.fixed(width, height)
                )

                layout(width, height) {
                    placeable.placeRelative(0, 0)
                }
            }
        }
    }*/

val easing: Easing = EaseInOutQuad

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun LazyItemScope.LyricItem(
    isCurrentLambda: () -> Boolean,
    isTopLambda: () -> Boolean,
    mainLyric: List<Pair<Float, String>>,
    translation: String?,
    showTranslation: Boolean,
    transliteration: List<String>? = null,
    //mainTextSize: Int,
    subTextSize: Int,
    blur: () -> Float,
    /*showBlur: Boolean,*/
    mainTextBasicColor: Color,
    subTextBasicColor: Color,
    measurer: TextMeasurer,
    isLyricEmpty: () -> Boolean,
    nextTime: () -> Float,
    distanceLambda: () -> Int = { 0 },
    browsingLambda: () -> Boolean = { false },
    staggerLambda: () -> Float = { 0f },
    otherSide: Boolean,
    liveTimeLambda: () -> Int,
    style: YosLyricStyleSpec,
    onClick: () -> Unit
) {
    println("重组：歌词 $mainLyric")

    // A right to left line takes the side its script reads from, so it hugs the right edge.
    val rtlLine = remember(mainLyric) {
        YosTextShaping.isRtl(mainLyric.joinToString("") { it.second })
    }
    val atEnd = otherSide != rtlLine
    val viewAlign = if (atEnd) Alignment.End else Alignment.Start

    val focusedColor = Color(0xFFFFFFFF)
    val unfocusedColor = Color.White.copy(alpha = style.unsungAlpha)
    //Color(0x33FFFFFF)

    //val focusedSolidBrush = SolidColor(focusedColor)

    val unfocusedSolidBrush = SolidColor(unfocusedColor)

    val isNotOneByOne = rememberSaveable(mainLyric) {
        mutableStateOf(
            mainLyric.all { it.first == mainLyric.firstOrNull()?.first }
        )

    }

    val romanLine = remember(transliteration) {
        transliteration?.takeIf { list -> list.any { it.isNotBlank() } }
    }
    val romanPerWord = romanLine != null && !isNotOneByOne.value
    val fontScale = SettingsLibrary.LyricFontScale
    val fontWeightName = SettingsLibrary.LyricFontWeight
    val balanced = SettingsLibrary.LyricLineBalance
    val romanBelow = SettingsLibrary.LyricTransliterationBelow
    val fontFamily = rememberLyricFontFamily()
    val lineStyle = remember(
        otherSide, romanPerWord, rtlLine, fontScale, fontWeightName, balanced, romanBelow, fontFamily
    ) {
        val main = lyricTextStyle(fontScale, fontWeightName, balanced, fontFamily)
        val base = when {
            rtlLine -> main.copy(
                textAlign = if (otherSide) TextAlign.Left else TextAlign.Right
            )

            otherSide -> main.copy(textAlign = TextAlign.End)
            else -> main
        }
        if (romanPerWord) {
            base.copy(
                lineHeight = (base.lineHeight.value + TransliterationBand.value).sp,
                lineHeightStyle = LineHeightStyle(
                    alignment = if (romanBelow) {
                        LineHeightStyle.Alignment.Top
                    } else {
                        LineHeightStyle.Alignment.Bottom
                    },
                    trim = LineHeightStyle.Trim.None
                )
            )
        } else {
            base
        }
    }

    val liveTime = remember(mainLyric) { mutableIntStateOf(liveTimeLambda()) }

    val glowWords = remember(mainLyric) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !isNotOneByOne.value) {
            YosLyricGlow.words(YosLyricTiming.timeline(mainLyric))
        } else {
            emptyList()
        }
    }

    YosWrapper {
        val launch = remember(mainLyric) {
            derivedStateOf {
                isLyricEmpty() || !isNotOneByOne.value
            }
        }
        if (launch.value) {
            LaunchedEffect(Unit) {
                while (true) {
                    withContext(Dispatchers.Main) {
                        liveTime.intValue = liveTimeLambda()
                    }
                    delay(10L)
                }
            }
        }
    }

    YosWrapper {
        Column(
            Modifier
                .padding(horizontal = 9.dp),
            horizontalAlignment = viewAlign
        ) {
            val otherSideAnimate = if (atEnd) {
                TransformOrigin(1f, 0.25f)
            } else {
                TransformOrigin(0f, 0.25f)
            }
            //println("重组：倒计时 "+ mainLyric.isBlank()+ " "+ isCurrentLambda() + " " + (progress() != 0f))

            val otherSideTransformOrigin =
                if (atEnd) TransformOrigin(
                    1f,
                    0.5f
                ) else TransformOrigin(
                    0f,
                    0.5f
                )

            /*val otherSideThisLine = remember(mainLyric) {
                mainLyric.last().second.endsWith(":") || mainLyric.last().second.endsWith(
                    "："
                )
            }*/

            val styleEase = remember(style) { style.easing() }
            val settleSpec: AnimationSpec<Float> = remember(style) {
                TweenSpec(durationMillis = style.settleMs, easing = styleEase)
            }
            val scaleInSpec: AnimationSpec<Float> = remember(style) {
                TweenSpec(durationMillis = style.scaleActiveMs, easing = styleEase)
            }
            val scaleOutSpec: AnimationSpec<Float> = remember(style) {
                TweenSpec(durationMillis = style.scaleInactiveMs, easing = styleEase)
            }
            val alphaInSpec: AnimationSpec<Float> = remember(style) {
                TweenSpec(durationMillis = style.alphaActiveMs, easing = styleEase)
            }
            val alphaOutSpec: AnimationSpec<Float> = remember(style) {
                TweenSpec(durationMillis = style.alphaInactiveMs, easing = styleEase)
            }

            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            val scale = animateFloatAsState(
                targetValue = when {
                    pressed -> YosLyricStack.PRESSED_SCALE
                    isCurrentLambda() -> 1f
                    else -> style.inactiveScale
                },
                animationSpec = if (pressed) {
                    tween(YosLyricStack.PRESS_MS, easing = LyricEasing)
                } else if (isCurrentLambda()) {
                    scaleInSpec
                } else {
                    scaleOutSpec
                }
            )

            /*val blurValue = remember(mainLyric) {
                derivedStateOf {
                    if (blur() == 0f || !showBlur) 0f else blur()
                }
            }*/

            val cardPadding = if (atEnd) {
                Modifier.padding(start = 28.dp)
            } else {
                Modifier.padding(end = 28.dp)
            }

            if (isLyricEmpty()) {
                Column(Modifier.animateContentSize()) {
                    val percent = remember(mainLyric) {
                        derivedStateOf {
                            val m = mainLyric.first().first
                            /*(if ((nextTime() - m) < 900f) {
                                0f
                            } else {
                                */((liveTime.intValue - m).coerceAtLeast(0f) / (nextTime() - m))
                            /*})*/.coerceAtMost(1f)
                        }
                    }
                    val show = remember(mainLyric) {
                        derivedStateOf { (isLyricEmpty() && isCurrentLambda() && percent.value != 0f) }
                    }
                    AnimatedVisibility(
                        show.value,
                        enter = fadeIn(animationSpec = TweenSpec(400, easing = LyricEasing)) + scaleIn(
                            initialScale = YosLyricStack.GAP_REST_SCALE,
                            transformOrigin = otherSideAnimate,
                            animationSpec = TweenSpec(400, easing = LyricEasing)
                        ),
                        exit = fadeOut(TweenSpec(350, easing = LyricEasing)) + scaleOut(
                            targetScale = YosLyricStack.GAP_REST_SCALE,
                            transformOrigin = otherSideAnimate,
                            animationSpec = TweenSpec(350, easing = LyricEasing)
                        )
                    ) {
                        YosWrapper {
                            LyricCard(
                                { scale.value },
                                cardPadding,
                                otherSideTransformOrigin,
                                viewAlign,
                                staggerLambda,
                                //{ otherSideThisLine },
                                //onClick
                            ) {

                                Column(
                                    Modifier
                                        .padding(start = 20.dp, end = 20.dp)
                                        .padding(top = 8.dp, bottom = 10.dp),
                                    horizontalAlignment = viewAlign
                                ) {
                                    CountdownAnimation(
                                        { percent.value },
                                        colorLambda = { mainTextBasicColor })
                                }

                            }
                        }
                    }
                }
            } else {
                YosWrapper {
                    LyricCard(
                        { scale.value },
                        cardPadding,
                        otherSideTransformOrigin,
                        viewAlign,
                        staggerLambda,
                        //{ otherSideThisLine },
                        //onClick
                    ) {

                        val blurValue = animateDpAsState(
                            blur().dp,
                            tween(style.blurMs, easing = styleEase)
                        )

                        val blurModifier = remember(mainLyric) {
                            derivedStateOf {
                                val thisBlur = blur()
                                if (thisBlur == 0f) {
                                    Modifier
                                } else {
                                    Modifier.blur(
                                        blurValue.value,
                                        /*thisBlur.dp*/
                                        edgeTreatment = BlurredEdgeTreatment.Unbounded,
                                    )
                                }
                            }
                        }

                        YosWrapper {
                            Column(
                                Modifier
                                    .then(blurModifier.value)
                                    .fillMaxWidth(),
                                horizontalAlignment = viewAlign
                            ) {
                                val textAlign = when {
                                    rtlLine -> if (otherSide) TextAlign.Left else TextAlign.Right
                                    otherSide -> TextAlign.End
                                    else -> TextAlign.Start
                                }

                                YosWrapper {
                                    val thisAlphaAnimated = animateFloatAsState(
                                        targetValue = style.lineAlpha(
                                            if (isCurrentLambda()) 0 else distanceLambda().coerceAtLeast(1),
                                            browsingLambda()
                                        ),
                                        animationSpec = if (isCurrentLambda()) alphaInSpec else alphaOutSpec
                                    )

                                    val thisAlpha = remember(mainLyric) {
                                        derivedStateOf {
                                            thisAlphaAnimated.value
                                        }
                                    }

                                    val otherSidePadding = remember(mainLyric) {
                                        derivedStateOf {
                                            if (otherSide) {
                                                Modifier.padding(
                                                    start = 20.dp,
                                                    end = if (mainLyric.last().second.endsWith("：")) 3.dp else 20.dp
                                                )
                                            } else {
                                                Modifier.padding(
                                                    start = 20.dp,
                                                    end = 20.dp
                                                )
                                            }
                                        }
                                    }

                                    val pastFade = animateFloatAsState(
                                        targetValue = if (isCurrentLambda()) 0f else 1f,
                                        animationSpec = tween(
                                            durationMillis = 600,
                                            easing = yosEasing
                                        )
                                    )

                                    val showHighLight = remember(mainLyric) {
                                        derivedStateOf {
                                            if (isNotOneByOne.value) {
                                                true
                                            } else {
                                                liveTime.intValue >= mainLyric[mainLyric.size - (if (translation != null) 3 else 1)].first
                                            }
                                        }
                                    }

                                    val lineRoman: @Composable () -> Unit = {
                                        Text(
                                            text = romanLine.orEmpty().joinToString(" ") { it.trim() }
                                                .trim(),
                                            fontSize = TransliterationStyle.fontSize,
                                            fontWeight = TransliterationStyle.fontWeight,
                                            letterSpacing = TransliterationStyle.letterSpacing,
                                            lineHeight = TransliterationStyle.lineHeight,
                                            color = focusedColor,
                                            textAlign = textAlign,
                                            modifier = Modifier
                                                .graphicsLayer {
                                                    this.alpha = thisAlpha.value *
                                                            (1f - (1f - style.unsungAlpha) * pastFade.value)
                                                    compositingStrategy =
                                                        CompositingStrategy.ModulateAlpha
                                                }
                                                .then(otherSidePadding.value)
                                                .padding(top = 4.dp)
                                        )
                                    }

                                    if (romanLine != null && isNotOneByOne.value && !romanBelow) lineRoman()

                                    Line(
                                        lines = mainLyric,
                                        style = lineStyle,
                                        measurer = measurer,
                                        modifier = Modifier
                                            .graphicsLayer {
                                                this.alpha = thisAlpha.value
                                                compositingStrategy =
                                                    CompositingStrategy.ModulateAlpha
                                            }
                                            .padding(vertical = 4.dp)
                                            .then(otherSidePadding.value)
                                            .clickable(
                                                indication = null,
                                                interactionSource = interaction
                                            ) {
                                                onClick()
                                            },
                                        viewAlign = viewAlign,
                                        glowRadius = style.glowRadiusDp.dp,
                                        glow = if (glowWords.isEmpty()) {
                                            null
                                        } else {
                                            { layout ->
                                                val room = GlowRoom.toPx()
                                                val chars = glowWords.flatMap { word ->
                                                    word.text.indices.mapNotNull { i ->
                                                        val index = word.offset + i
                                                        if (word.text[i].isWhitespace() ||
                                                            index >= layout.layoutInput.text.length
                                                        ) {
                                                            null
                                                        } else {
                                                            GlowChar(word, i, layout.getBoundingBox(index))
                                                        }
                                                    }
                                                }
                                                onDrawBehind {
                                                    val time = liveTime.intValue.toFloat()
                                                    chars.fastForEach { c ->
                                                        val bloom = c.word.bloom(c.index, time)
                                                        if (bloom <= 0.01f) return@fastForEach
                                                        val lift = style.liftPx * easing.transform(
                                                            YosLyricTiming.progress(
                                                                time,
                                                                c.word.timing.start,
                                                                c.word.timing.end
                                                            )
                                                        )
                                                        translate(room, room - lift) {
                                                            clipRect(
                                                                c.box.left,
                                                                c.box.top,
                                                                c.box.right,
                                                                c.box.bottom
                                                            ) {
                                                                drawText(
                                                                    textLayoutResult = layout,
                                                                    color = focusedColor.copy(alpha = style.glowAlpha * bloom)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    ) { parentConstraints, measureResult ->

                                        val shaped =
                                            YosTextShaping.needsShapedDraw(measureResult.layoutInput.text)

                                        if (isNotOneByOne.value) {
                                            return@Line onDrawWithContent {
                                                drawLineText(
                                                    measureResult,
                                                    lerp(focusedColor, unfocusedColor, pastFade.value),
                                                    shaped
                                                )
                                            }
                                        }

                                        // 以下为逐字处理

                                        val romans = if (romanPerWord && romanLine != null) {
                                            layoutTransliteration(
                                                mainLyric = mainLyric,
                                                transliteration = romanLine,
                                                measureResult = measureResult,
                                                measurer = measurer,
                                                below = romanBelow,
                                                bandPx = TransliterationBand.toPx()
                                            )
                                        } else {
                                            emptyList()
                                        }

                                        val isCurrent = isCurrentLambda()
                                        val fade = if (isCurrent) 0f else pastFade.value

                                        if (!isCurrent) {
                                            if (!showHighLight.value) {
                                                // 是逐字 但不是当前行，且还没唱到：灰色
                                                return@Line onDrawWithContent {
                                                    drawLineText(measureResult, unfocusedColor, shaped)
                                                    drawTransliterationFlat(romans, unfocusedColor)
                                                }
                                            }
                                            if (fade >= 1f) {
                                                // 已唱完，且已经从白色淡回灰色
                                                return@Line onDrawWithContent {
                                                    drawLineText(
                                                        measureResult,
                                                        unfocusedColor,
                                                        shaped,
                                                        Offset(0F, -style.liftPx)
                                                    )
                                                    drawTransliterationFlat(romans, unfocusedColor)
                                                }
                                            }
                                        }

                                        val timeline = YosLyricTiming.timeline(mainLyric)
                                        val totalChars = mainLyric.sumOf { it.second.length }
                                        val wordsToDraw = arrayListOf<DrawWord>()
                                        var sum = 0

                                        val finishedColor = lerp(focusedColor, unfocusedColor, fade)

                                        if (shaped) {
                                            val shapedWords =
                                                layoutShapedWords(timeline, measureResult, totalChars)
                                            return@Line onDrawBehind {
                                                val time = liveTime.intValue.toFloat()
                                                drawShapedSweep(
                                                    measureResult,
                                                    shapedWords,
                                                    time,
                                                    finishedColor,
                                                    unfocusedColor,
                                                    style.liftPx
                                                )
                                                drawTransliterationProgress(
                                                    items = romans,
                                                    time = time,
                                                    finishedColor = finishedColor,
                                                    unfocusedColor = unfocusedColor
                                                )
                                            }
                                        }

                                        timeline.fastForEach { word ->
                                            val thisWord = word.text

                                            val groupPercent = YosLyricTiming.progress(
                                                liveTime.intValue.toFloat(),
                                                word.start,
                                                word.end
                                            )
                                            val easedPercent = easing.transform(groupPercent)
                                            val topLeftWeight = style.liftPx * easedPercent

                                            thisWord.forEachIndexed { charIndex, char ->
                                                val charWord = char.toString()

                                                val charStart = word.charStart(charIndex)
                                                val charEnd = word.charEnd(charIndex)

                                                val layout = measurer.measure(
                                                    text = charWord,
                                                    style = lineStyle,
                                                    constraints = Constraints(maxWidth = measureResult.layoutInput.constraints.maxWidth)
                                                )

                                                wordsToDraw += DrawWord(
                                                    time = charEnd,
                                                    word = charWord,
                                                    layout = layout,
                                                    topLeft = measureResult.getBoundingBox(
                                                        sum.coerceAtMost(totalChars - 1)
                                                            .coerceAtLeast(0)
                                                    ).topLeft.minus(
                                                        Offset(
                                                            0F,
                                                            topLeftWeight
                                                        )
                                                    ),
                                                    brush = { px, percent ->
                                                        if (thisWord == " ") {
                                                            return@DrawWord unfocusedSolidBrush
                                                        }
                                                        sweepBrush(
                                                            percent,
                                                            px,
                                                            finishedColor,
                                                            unfocusedColor
                                                        )
                                                    },
                                                    percent = {
                                                        if (thisWord == " ") {
                                                            return@DrawWord 0f
                                                        }

                                                        YosLyricTiming.rawProgress(
                                                            liveTime.intValue.toFloat(),
                                                            charStart,
                                                            charEnd
                                                        )
                                                    }
                                                ).also {
                                                    sum += charWord.length
                                                }
                                            }
                                        }

                                        onDrawBehind {
                                            wordsToDraw.fastForEach { l ->
                                                drawText(
                                                    textLayoutResult = l.layout,
                                                    topLeft = l.topLeft,
                                                    brush = l.brush(
                                                        0.3f,
                                                        l.percent()
                                                    )
                                                )
                                            }
                                            drawTransliterationProgress(
                                                items = romans,
                                                time = liveTime.intValue.toFloat(),
                                                finishedColor = finishedColor,
                                                unfocusedColor = unfocusedColor
                                            )
                                        }
                                    }
                                    if (romanLine != null && isNotOneByOne.value && romanBelow) lineRoman()
                                }
                                YosWrapper {
                                    AnimatedVisibility(showTranslation && translation != null) {
                                        translation?.let {
                                            val translationAlpha = animateFloatAsState(
                                                targetValue = if (isCurrentLambda()) 0.5f else 0.14f,
                                                animationSpec = settleSpec
                                            )

                                            val translationOtherSidePadding = if (otherSide) {
                                                Modifier.padding(
                                                    start = 20.dp,
                                                    end = 20.dp
                                                )
                                            } else {
                                                Modifier.padding(
                                                    start = 20.dp,
                                                    end = 20.dp
                                                )
                                            }

                                            Text(
                                                text = it,
                                                fontSize = (subTextSize * fontScale).sp,
                                                fontFamily = fontFamily,
                                                color = subTextBasicColor,
                                                fontWeight = FontWeight.Normal,
                                                modifier = Modifier
                                                    .graphicsLayer {
                                                        this.alpha =
                                                            translationAlpha.value
                                                        compositingStrategy =
                                                            CompositingStrategy.ModulateAlpha
                                                    }
                                                    .then(translationOtherSidePadding)
                                                    .padding(top = 5.dp),
                                                lineHeight = (subTextSize * fontScale + 5).sp,
                                                letterSpacing = 0.3.sp,
                                                textAlign = textAlign
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LyricCard(
    scale: () -> Float,
    cardPadding: Modifier,
    otherSideTransformOrigin: TransformOrigin,
    viewAlign: Alignment.Horizontal,
    translationY: () -> Float = { 0f },
    //otherSideThisLine: () -> Boolean,
    //onClick: () -> Unit,
    content: @Composable () -> Unit,
) =
    YosWrapper {
        Column(
            modifier = Modifier
                .graphicsLayer {
                    //compositingStrategy = CompositingStrategy.ModulateAlpha
                    val scaleValue = scale()
                    scaleX = scaleValue
                    scaleY = scaleValue
                    this.translationY = translationY()
                    transformOrigin = otherSideTransformOrigin
                }
                .fillMaxWidth()
                .then(cardPadding)
                .padding(top = 9.dp, bottom = 9.dp),
            horizontalAlignment = viewAlign
        ) {
            content()
        }
    }

@Composable
fun CountdownAnimation(progress: () -> Float, colorLambda: () -> Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier.padding(horizontal = 5.dp)
    ) {
        repeat(YosLyricStack.GAP_DOTS) { dot ->
            Box(
                modifier = Modifier
                    .size(13.dp)
                    .drawBehind {
                        drawCircle(
                            color = colorLambda().copy(alpha = YosLyricStack.gapDotAlpha(progress(), dot))
                        )
                    }
            )
        }
    }
}
/*val BackgroundTextStyle = TextStyle(
    fontSize = 34.sp,
    lineHeight = 42.sp,
    fontWeight = FontWeight.Bold
).copy(
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None
    )
)*/

// ---- transliteration and glow ----

/** Height added above every line that carries a transliteration. */
private val TransliterationBand = 17.sp

private object TransliterationStyle {
    val fontSize = 14.sp
    val fontWeight = FontWeight.Bold
    val letterSpacing = 0.1.sp
    val lineHeight = 17.sp
}

private val TransliterationTextStyle = TextStyle(
    fontSize = TransliterationStyle.fontSize,
    fontWeight = TransliterationStyle.fontWeight,
    letterSpacing = TransliterationStyle.letterSpacing,
    lineHeight = TransliterationStyle.lineHeight
)

private val GlowRadius = 6.dp
private val GlowRoom = 12.dp
private const val GlowSlot = "glow"

/** A sweep from [startX] to [endX]: [finished] before [percent], [unfocused] after, blended over [softness]. */
private fun sweepBrush(
    percent: Float,
    softness: Float,
    finished: Color,
    unfocused: Color,
    startX: Float = 0f,
    endX: Float = Float.POSITIVE_INFINITY
): Brush {
    val beforeColor = if (percent <= -0.5f) unfocused else finished
    val afterColor = if (percent >= 1f) finished else unfocused
    return Brush.horizontalGradient(
        0f to beforeColor,
        (percent - softness).coerceIn(0f, 1f) to beforeColor,
        (percent + softness).coerceIn(0f, 1f) to afterColor,
        startX = startX,
        endX = endX
    )
}

/** A piece of transliteration with its place, its text row and the time span of its lyric. */
private class RomanItem(
    val layout: TextLayoutResult,
    var x: Float,
    val top: Float,
    val line: Int,
    val start: Float,
    val end: Float
) {
    val width: Float get() = layout.size.width.toFloat()
    val topLeft: Offset get() = Offset(x, top)
}

/**
 * Places the transliteration in the height above the line. A word with one syllable per
 * character gets each syllable above its own character, also when the line wraps. Any other
 * word keeps its transliteration in one piece. Pieces that would touch are spread apart.
 */
private fun layoutTransliteration(
    mainLyric: List<Pair<Float, String>>,
    transliteration: List<String>,
    measureResult: TextLayoutResult,
    measurer: TextMeasurer,
    below: Boolean = false,
    bandPx: Float = 0f
): List<RomanItem> {
    val items = ArrayList<RomanItem>()
    val text = measureResult.layoutInput.text.text
    if (text.isEmpty()) return items

    fun measure(roman: String) = measurer.measure(
        text = roman,
        style = TransliterationTextStyle,
        softWrap = false,
        maxLines = 1
    )

    var offset = 0
    YosLyricTiming.timeline(mainLyric).forEach { word ->
        val first = offset
        offset += word.text.length

        val roman = transliteration.getOrNull(word.pairIndex - 1)?.trim().orEmpty()
        if (roman.isEmpty() || word.text.isBlank()) return@forEach

        val syllables = YosSyllables.perCharacter(word.text, roman)
        if (syllables != null) {
            syllables.forEach { (charIndex, syllable) ->
                val at = (first + charIndex).coerceIn(0, text.length - 1)
                val box = measureResult.getBoundingBox(at)
                val layout = measure(syllable)
                items += RomanItem(
                    layout = layout,
                    x = box.left + (box.right - box.left - layout.size.width) / 2f,
                    top = if (below) box.bottom - bandPx else box.top,
                    line = measureResult.getLineForOffset(at),
                    start = word.charStart(charIndex),
                    end = word.charEnd(charIndex)
                )
            }
            return@forEach
        }

        var from = first.coerceIn(0, text.length - 1)
        var to = (first + word.text.length - 1).coerceIn(0, text.length - 1)
        while (from < to && text[from].isWhitespace()) from++
        while (to > from && text[to].isWhitespace()) to--

        val line = measureResult.getLineForOffset(from)
        val lastOnLine = (measureResult.getLineEnd(line, visibleEnd = true) - 1).coerceAtLeast(from)
        val startBox = measureResult.getBoundingBox(from)
        val endBox = measureResult.getBoundingBox(minOf(to, lastOnLine))
        val left = minOf(startBox.left, endBox.left)
        val right = maxOf(startBox.right, endBox.right)
        val layout = measure(roman)
        items += RomanItem(
            layout = layout,
            x = left + (right - left - layout.size.width) / 2f,
            top = if (below) startBox.bottom - bandPx else startBox.top,
            line = line,
            start = word.start,
            end = word.end
        )
    }

    val gap = 6f
    val width = measureResult.size.width.toFloat()
    items.groupBy { it.line }.values.map { row -> row.sortedBy { it.x } }.forEach { row ->
        row.forEachIndexed { i, item ->
            if (i == 0) {
                item.x = item.x.coerceAtLeast(0f)
            } else {
                val previous = row[i - 1]
                item.x = maxOf(item.x, previous.x + previous.width + gap)
            }
        }
        val overflow = row.last().x + row.last().width - width
        if (overflow > 0f) {
            row.last().x -= overflow
            for (i in row.size - 2 downTo 0) {
                val next = row[i + 1]
                row[i].x = minOf(row[i].x, next.x - row[i].width - gap)
            }
        }
    }
    return items
}

/** Draws the transliteration in one color. */
private fun DrawScope.drawTransliterationFlat(items: List<RomanItem>, color: Color) {
    items.fastForEach { item ->
        drawText(textLayoutResult = item.layout, color = color, topLeft = item.topLeft)
    }
}

/** Draws the transliteration lit from left to right with its lyric: [finishedColor] once sung. */
private fun DrawScope.drawTransliterationProgress(
    items: List<RomanItem>,
    time: Float,
    finishedColor: Color,
    unfocusedColor: Color
) {
    items.fastForEach { item ->
        val percent = YosLyricTiming.progress(time, item.start, item.end)
        val left = item.x
        val top = item.top
        val width = item.width
        val height = item.layout.size.height.toFloat()
        val edge = left + width * percent

        if (percent < 1f) {
            clipRect(left = edge, top = top, right = left + width + 1f, bottom = top + height) {
                drawText(textLayoutResult = item.layout, color = unfocusedColor, topLeft = item.topLeft)
            }
        }
        if (percent > 0f) {
            clipRect(left = left - 1f, top = top, right = edge, bottom = top + height) {
                drawText(textLayoutResult = item.layout, color = finishedColor, topLeft = item.topLeft)
            }
        }
    }
}

/** The credits of the lyrics, such as the songwriters, shown as the source wrote them. */
@Composable
private fun LyricCredits(credits: List<YosLyricCredit>, textColor: Color) {
    val text = remember(credits) {
        buildAnnotatedString {
            credits.forEachIndexed { index, credit ->
                if (index > 0) append("\n\n")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(credit.label)
                    append(": ")
                }
                append(credit.values.joinToString(", "))
            }
        }
    }
    Text(
        text = text,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        color = textColor.copy(alpha = 0.6f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 29.dp, end = 29.dp, top = 28.dp, bottom = 8.dp)
    )
}

private class GlowChar(
    val word: YosGlowWord,
    val index: Int,
    val box: Rect
)

private class ShapedChar(val box: Rect, val start: Float, val end: Float, val rtl: Boolean, val ink: Boolean)

private class ShapedWord(val start: Float, val end: Float, val box: Rect, val chars: List<ShapedChar>)

/** Boxes of every sung segment, taken from the one layout of the whole line. */
private fun layoutShapedWords(
    timeline: List<YosWordTiming>,
    layout: TextLayoutResult,
    totalChars: Int
): List<ShapedWord> {
    val result = ArrayList<ShapedWord>(timeline.size)
    var sum = 0
    timeline.fastForEach { word ->
        val chars = ArrayList<ShapedChar>(word.text.length)
        word.text.forEachIndexed { i, c ->
            val at = (sum + i).coerceAtMost(totalChars - 1).coerceAtLeast(0)
            chars += ShapedChar(
                box = layout.getBoundingBox(at),
                start = word.charStart(i),
                end = word.charEnd(i),
                rtl = layout.getBidiRunDirection(at) == ResolvedTextDirection.Rtl,
                ink = !c.isWhitespace()
            )
        }
        sum += word.text.length
        if (chars.isEmpty()) return@fastForEach
        val box = Rect(
            left = chars.minOf { it.box.left },
            top = chars.minOf { it.box.top },
            right = chars.maxOf { it.box.right },
            bottom = chars.maxOf { it.box.bottom }
        )
        result += ShapedWord(word.start, word.end, box, chars)
    }
    return result
}

/**
 * Sweeps a line whose letters join. The whole line is drawn once as a solid mask, each word
 * clipped and lifted on its own, then every character box is coloured over the mask. The
 * translucent colour lands once per pixel, so joins do not show as brighter spots.
 */
private fun DrawScope.drawShapedSweep(
    layout: TextLayoutResult,
    words: List<ShapedWord>,
    time: Float,
    finishedColor: Color,
    unfocusedColor: Color,
    liftPx: Float
) {
    val pad = 24f
    drawIntoCanvas { canvas ->
        canvas.saveLayer(Rect(-pad, -pad, size.width + pad, size.height + pad), Paint())
        words.fastForEach { word ->
            val lift = liftPx * easing.transform(YosLyricTiming.progress(time, word.start, word.end))
            translate(0f, -lift) {
                clipRect(word.box.left, word.box.top, word.box.right, word.box.bottom) {
                    drawText(textLayoutResult = layout, color = Color.White)
                }
            }
        }
        words.fastForEach { word ->
            val lift = liftPx * easing.transform(YosLyricTiming.progress(time, word.start, word.end))
            translate(0f, -lift) {
                word.chars.fastForEach { c ->
                    if (!c.ink) return@fastForEach
                    val brush = sweepBrush(
                        YosLyricTiming.rawProgress(time, c.start, c.end),
                        0.3f,
                        finishedColor,
                        unfocusedColor,
                        startX = if (c.rtl) c.box.right else c.box.left,
                        endX = if (c.rtl) c.box.left else c.box.right
                    )
                    drawRect(
                        brush = brush,
                        topLeft = c.box.topLeft,
                        size = c.box.size,
                        blendMode = BlendMode.SrcIn
                    )
                }
            }
        }
        canvas.restore()
    }
}

/**
 * Draws a line in one colour. A line that joins letters is drawn opaque into a layer that
 * carries the alpha, because overlapping letter ends would otherwise show brighter.
 */
private fun DrawScope.drawLineText(
    layout: TextLayoutResult,
    color: Color,
    shaped: Boolean,
    topLeft: Offset = Offset.Zero
) {
    if (!shaped || color.alpha >= 0.99f) {
        drawText(textLayoutResult = layout, color = color, topLeft = topLeft)
        return
    }
    val pad = 24f
    drawIntoCanvas { canvas ->
        val paint = Paint().apply { alpha = color.alpha }
        canvas.saveLayer(
            Rect(
                topLeft.x - pad,
                topLeft.y - pad,
                topLeft.x + layout.size.width + pad,
                topLeft.y + layout.size.height + pad
            ),
            paint
        )
        drawText(textLayoutResult = layout, color = color.copy(alpha = 1f), topLeft = topLeft)
        canvas.restore()
    }
}

@Stable
private data class DrawWord(
    val time: Float,
    val word: String,
    val layout: TextLayoutResult,
    val topLeft: Offset,
    val brush: (px: Float, percent: Float) -> Brush,
    val percent: () -> Float
)

/*
fun processWords(input: String): List<String> {
    val result = mutableListOf<String>()
    var word = ""
    for (char in input) {
        if (char == ' ') {
            if (word.isNotEmpty()) {
                result.add(word)
                word = ""
            }
            result.add(" ")
        } else {
            word += char
        }
    }
    if (word.isNotEmpty()) {
        result.add(word)
    }
    return result
}*/

/** The cubic bezier easing of a look. */
private fun YosLyricStyleSpec.easing(): CubicBezierEasing =
    CubicBezierEasing(easingPoints[0], easingPoints[1], easingPoints[2], easingPoints[3])
