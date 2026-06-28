package com.rannuan.tv.ui.screens.player

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.util.Log
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Brightness6
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.FastRewind
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PlayDisabled
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.rannuan.tv.R
import com.rannuan.tv.data.api.RanNuanApi
import com.rannuan.tv.data.model.MediaDetail
import com.rannuan.tv.data.model.MediaItem as RelatedMediaItem
import com.rannuan.tv.ui.theme.Brand300
import com.rannuan.tv.ui.theme.Brand400
import com.rannuan.tv.ui.theme.Error500
import com.rannuan.tv.ui.theme.ShapeCard
import com.rannuan.tv.ui.theme.ShapeChip
import com.rannuan.tv.ui.theme.White06
import com.rannuan.tv.ui.theme.Zinc300
import com.rannuan.tv.ui.theme.Zinc400
import com.rannuan.tv.ui.theme.Zinc500
import com.rannuan.tv.ui.theme.Zinc600
import com.rannuan.tv.ui.theme.Zinc700
import com.rannuan.tv.ui.theme.Zinc800
import com.rannuan.tv.ui.theme.Zinc900
import com.rannuan.tv.ui.theme.Zinc950
import com.rannuan.tv.ui.theme.component.GlassCard
import com.rannuan.tv.ui.util.ImageProxy
import com.rannuan.tv.ui.util.WatchingHistoryStore
import com.rannuan.tv.ui.util.formatSourceName
import com.rannuan.tv.ui.util.stripHtml
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.annotation.OptIn as AndroidOptIn

/**
 * 播放器页面 —— 爱优腾级交互（Media3 + 自定义 Compose 手势层）。
 *
 * 手势交互（覆盖在 PlayerView 之上的透明手势层）：
 *  - 单击：显隐控制栏
 *  - 双击左半屏 / 右半屏：快退 / 快进 10 秒（YouTube 式扇形动画）
 *  - 水平拖拽：拖动进度，松手 seek（带预览时间弹窗）
 *  - 左半屏竖向拖拽：调节屏幕亮度
 *  - 右半屏竖向拖拽：调节音量
 *  - 长按：进入 2x 倍速，松手恢复
 *  - 全屏：横屏 + 隐藏系统栏（再点或返回键退出）
 *
 * 控件层采用顶部/底部渐变蒙层 + 半透明图标（对标爱优腾）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@AndroidOptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])
@Composable
fun PlayerScreen(
    siteKey: String, id: String,
    sourceIdx: Int = 0, epIdx: Int = 0,
    resumePos: Long = 0L,   // 续播位置（毫秒），来自历史记录
    api: RanNuanApi,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val activity = remember { context.findActivity() }

    // ── 数据状态 ──
    var detail by remember { mutableStateOf<MediaDetail?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var currentSrc by rememberSaveable { mutableIntStateOf(sourceIdx) }
    var currentEp by rememberSaveable { mutableIntStateOf(epIdx) }

    // ── 续播 ──
    var pendingResumePos by remember { mutableLongStateOf(resumePos) }

    // ── 播放器（注入 User-Agent + 动态 Referer 解决 CDN 防盗链拒绝）──
    val dsFactory = remember {
        DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.6422.165 Mobile Safari/537.36")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)
    }
    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dsFactory))
            .build()
    }
    var showControls by remember { mutableStateOf(true) }
    var showEpisodeSheet by remember { mutableStateOf(false) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var isFullscreen by rememberSaveable { mutableStateOf(false) }
    var skipSeconds by rememberSaveable { mutableIntStateOf(10) }
    var resizeMode by rememberSaveable { mutableIntStateOf(0) }    // 0=适应 1=拉伸 2=裁剪
    var playMode by rememberSaveable { mutableIntStateOf(0) }      // 0=顺序 1=单集循环 2=列表循环
    var mirrorMode by rememberSaveable { mutableIntStateOf(0) }   // 0=正常 1=水平镜像 2=垂直镜像
    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }
    val episodeSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 播放状态
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }
    var buffered by remember { mutableLongStateOf(0L) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var usingProxy by remember { mutableStateOf(false) }
    var selectedSpeed by remember { mutableFloatStateOf(1f) }
    var longPressActive by remember { mutableStateOf(false) }
    val playbackSpeed = if (longPressActive) 2f else selectedSpeed
    var isLocked by remember { mutableStateOf(false) }
    var buffering by remember { mutableStateOf(false) }

    // 相似推荐
    var relatedVideos by remember { mutableStateOf<List<RelatedMediaItem>>(emptyList()) }
    var relatedLoading by remember { mutableStateOf(false) }

    // 多站点播放源
    var sourceDetails by remember { mutableStateOf<List<MediaDetail>>(emptyList()) }

    // ── 手势临时状态 ──
    // 拖拽 seek
    var seekDragging by remember { mutableStateOf(false) }
    var seekPreviewMs by remember { mutableLongStateOf(0L) }
    // 双击 seek 动画
    var doubleTapSide by remember { mutableStateOf<SeekSide?>(null) }  // null=隐藏
    var doubleTapCount by remember { mutableIntStateOf(0) }
    // 亮度/音量
    var gestureMode by remember { mutableStateOf<GestureMode?>(null) }
    var brightnessValue by remember { mutableFloatStateOf(-1f) }       // -1 表示系统默认
    var volumeValue by remember { mutableIntStateOf(-1) }
    var showVolumeUi by remember { mutableStateOf(false) }
    var showBrightnessUi by remember { mutableStateOf(false) }

    // ── DLNA 投屏 ──
    val scope = rememberCoroutineScope()
    val dlnaController = remember { DLNAController() }
    var showCastSheet by remember { mutableStateOf(false) }
    var discoveredDevices by remember { mutableStateOf<List<DLNADevice>>(emptyList()) }
    var isDiscovering by remember { mutableStateOf(false) }
    var castSession by remember { mutableStateOf<DLNACastSession?>(null) }
    var castError by remember { mutableStateOf<String?>(null) }

    // ── 选集解析 ──
    val sources = remember(detail) {
        if (detail == null) emptyList()
        else parseSources(detail!!)
    }
    val activeSource = sources.getOrNull(currentSrc)
    val latestSources by rememberUpdatedState(sources)
    val latestCurrentSrc by rememberUpdatedState(currentSrc)
    val latestCurrentEp by rememberUpdatedState(currentEp)
    val latestPlayMode by rememberUpdatedState(playMode)

    // ── 保存播放位置（用于续播）──
    fun savePlayPosition() {
        detail?.let { d ->
            val pos = exoPlayer.currentPosition
            if (pos > 2000) {
                WatchingHistoryStore.updatePosition(context, d.siteKey, d.vodId, pos)
            }
        }
    }

    fun switchToEpisode(ep: Int) {
        val src = latestSources.getOrNull(latestCurrentSrc) ?: return
        if (ep !in src.episodes.indices) return
        savePlayPosition()
        pendingResumePos = 0L
        currentEp = ep
        showControls = true
    }

    fun switchToDetail(d: MediaDetail) {
        if (detail?.siteKey == d.siteKey && detail?.vodId == d.vodId) return
        savePlayPosition()
        detail = d
        currentSrc = 0
        currentEp = 0
        pendingResumePos = 0L
        showControls = true
    }

    fun switchToLine(idx: Int) {
        if (idx !in sources.indices) return
        savePlayPosition()
        currentSrc = idx
        currentEp = 0
        pendingResumePos = 0L
        showControls = true
    }

    fun handleEpisodeEnded() {
        val src = latestSources.getOrNull(latestCurrentSrc) ?: return
        val ep = latestCurrentEp
        when (latestPlayMode) {
            1 -> {
                pendingResumePos = 0L
                exoPlayer.seekTo(0L)
                exoPlayer.playWhenReady = true
                exoPlayer.play()
            }
            2 -> {
                val nextEp = if (ep + 1 < src.episodes.size) ep + 1 else 0
                switchToEpisode(nextEp)
            }
            else -> {
                if (ep + 1 < src.episodes.size) {
                    switchToEpisode(ep + 1)
                } else {
                    showControls = true
                }
            }
        }
    }

    // ── DLNA 投屏操作函数 ──
    fun doCastToDevice(device: DLNADevice) {
        val src = sources.getOrNull(currentSrc) ?: return
        val ep = src.episodes.getOrNull(currentEp) ?: return
        val videoUrl = ep.url.trim().let { url ->
            when {
                url.startsWith("http://") || url.startsWith("https://") -> url
                url.startsWith("//") -> "https:$url"
                else -> "https://$url"
            }
        }
        scope.launch {
            castSession = DLNACastSession(device)
            castError = null
            try {
                val ok = dlnaController.setAVTransportURI(device, videoUrl, title = ep.title)
                if (ok) {
                    dlnaController.play(device)
                    exoPlayer.pause()
                    castSession = castSession?.copy(state = DLNACastState.PLAYING)
                } else {
                    castSession = null
                    castError = "无法连接设备「${device.displayName}」，请重试"
                }
            } catch (e: Exception) {
                castSession = null
                castError = "投屏失败: ${e.message}"
            }
        }
    }

    fun stopCasting() {
        castSession?.let { session ->
            scope.launch {
                dlnaController.stop(session.device)
                castSession = null
            }
        }
    }

    fun toggleCastPlayPause() {
        val session = castSession ?: return
        scope.launch {
            if (session.state == DLNACastState.PLAYING) {
                dlnaController.pause(session.device)
                castSession = session.copy(state = DLNACastState.PAUSED)
            } else {
                dlnaController.play(session.device)
                castSession = session.copy(state = DLNACastState.PLAYING)
            }
        }
    }

    // ── 播放辅助函数：直链 & m3u8 统一入口 ──
    // 切换到新线路时已由 LaunchedEffect 调用 exoPlayer.stop() + 重置 usingProxy
    fun tryPlayVideo(proxy: Boolean, fromError: Boolean = false) {
        val s = sources.getOrNull(currentSrc) ?: return
        val e = s.episodes.getOrNull(currentEp) ?: return
        // 规范化 URL（补协议头、去掉空格）
        val raw = e.url.trim().let { url ->
            when {
                url.startsWith("http://") || url.startsWith("https://") -> url
                url.startsWith("//") -> "https:$url"
                else -> "https://$url"
            }
        }

        // 判断是否为 m3u8（ExoPlayer 原生支持，可直连）
        val isM3u8 = raw.contains(".m3u8", ignoreCase = true)

        // 非 m3u8 的直链（mp4/ts/flv/分享页等）→ 直接走代理
        // 服务端代理会：1) 解析分享页提取真实视频地址 2) 处理防盗链 Referer 3) 重写 m3u8 内部分片 URL
        // m3u8 → 先直连（快），失败自动回退代理
        val useProxy = proxy || !isM3u8
        val uri = if (useProxy) {
            "${com.rannuan.tv.BuildConfig.SERVER_URL}/api/proxy?url=${Uri.encode(raw)}"
        } else raw

        Log.d("PlayerScreen", "播放: ${if(useProxy) "代理" else "直连"} | m3u8=$isM3u8 | 原始=${raw.take(80)}")
        Log.d("PlayerScreen", "最终URI: ${uri.take(120)}")

        // 记录代理状态，避免 onPlayerError 里重复走代理
        if (useProxy) usingProxy = true

        // 直连 m3u8 时设置 Referer 头绕过防盗链
        if (!useProxy) {
            val referer = when {
                raw.contains("bfzy") || raw.contains("picbf") -> "https://bfzyapi.com"
                else -> {
                    val u = Uri.parse(raw)
                    val scheme = u.scheme ?: "https"
                    val host = u.host ?: ""
                    if (host.isNotBlank()) "$scheme://$host" else raw
                }
            }
            dsFactory.setDefaultRequestProperties(mapOf("Referer" to referer))
        }

        exoPlayer.stop()
        // 代理模式：强制 HLS MIME 类型，否则 ExoPlayer 认不出代理返回的 m3u8
        val mediaItem = if (useProxy) {
            MediaItem.Builder().setUri(uri).setMimeType(MimeTypes.APPLICATION_M3U8).build()
        } else {
            MediaItem.fromUri(uri)
        }
        // 记录观看历史
        detail?.let { d ->
            val epTitle = sources.getOrNull(currentSrc)?.episodes?.getOrNull(currentEp)?.title ?: ""
            WatchingHistoryStore.addHistory(
                context, d.siteKey, d.vodId,
                d.vodName, d.vodPic ?: "", epTitle
            )
        }

        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true

        if (fromError) playbackError = null
    }

    // ── 播放器监听 + 生命周期（合并释放，避免 observer 泄漏）──
    val lifecycle = LocalLifecycleOwner.current
    DisposableEffect(lifecycle, exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(p: Boolean) { isPlaying = p }
            override fun onPlaybackStateChanged(state: Int) {
                buffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) {
                    duration = exoPlayer.duration.coerceAtLeast(0)
                    playbackError = null
                    // 续播：首次就绪后跳到历史位置（仅执行一次）
                    if (pendingResumePos > 0) {
                        exoPlayer.seekTo(pendingResumePos.coerceAtMost(duration))
                        pendingResumePos = 0
                    }
                } else if (state == Player.STATE_ENDED) {
                    isPlaying = false
                    handleEpisodeEnded()
                }
            }
            override fun onPlayerError(e: PlaybackException) {
                if (!usingProxy) {
                    // 直连 m3u8 失败 → 自动切代理重试
                    usingProxy = true
                    tryPlayVideo(proxy = true, fromError = true)
                } else {
                    // 代理也失败 → 显示友好错误信息
                    playbackError = "当前线路播放失败，请尝试切换其他线路"
                }
            }
        }
        exoPlayer.addListener(listener)
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                exoPlayer.pause()
                // 暂停时保存播放位置用于续播
                savePlayPosition()
            }
        }
        lifecycle.lifecycle.addObserver(obs)
        onDispose {
            savePlayPosition()
            lifecycle.lifecycle.removeObserver(obs)
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // 进度定时器（拖拽时暂停轮询，避免与手势 seek 冲突）
    LaunchedEffect(Unit) {
        while (true) {
            if (!seekDragging && exoPlayer.playbackState == Player.STATE_READY) {
                currentPosition = exoPlayer.currentPosition
                duration = exoPlayer.duration.coerceAtLeast(0)
                buffered = exoPlayer.bufferedPosition.coerceAtLeast(0)
            }
            delay(300)
        }
    }

    // 控制栏自动隐藏（拖拽/锁定/暂停时保持显示）
    LaunchedEffect(showControls, isPlaying, isLocked, seekDragging) {
        if (showControls && isPlaying && !isLocked && !seekDragging) {
            delay(4500); showControls = false
        }
    }

    // 双击 seek 动画：1秒后自动消失；累积叠加跳秒数
    LaunchedEffect(doubleTapSide) {
        if (doubleTapSide != null) {
            delay(800)
            doubleTapSide = null
            doubleTapCount = 0
        }
    }

    // 速度同步（菜单选中倍速 + 长按临时 2x）
    LaunchedEffect(playbackSpeed) { exoPlayer.setPlaybackSpeed(playbackSpeed) }

    // 画面尺寸
    LaunchedEffect(resizeMode) {
        playerViewRef?.resizeMode = when (resizeMode) {
            0 -> AspectRatioFrameLayout.RESIZE_MODE_FIT
            1 -> AspectRatioFrameLayout.RESIZE_MODE_FILL
            2 -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
    }

    // 镜像翻转已通过 Composable 的 graphicsLayer 实现（见 AndroidView 处），此处无需额外处理

    // 播放方式由业务层在 STATE_ENDED 中处理，不能交给 ExoPlayer 的单 MediaItem repeatMode。
    LaunchedEffect(playMode) {
        exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
    }

    // 应用亮度（实时）
    LaunchedEffect(brightnessValue, activity) {
        activity?.window?.let { w ->
            val attrs = w.attributes
            attrs.screenBrightness = brightnessValue
            w.attributes = attrs
        }
    }

    // ── 加载详情 ──
    LaunchedEffect(siteKey, id) {
        loading = true; error = null
        try {
            val resp = api.getMultiDetail(wd = "", keys = "$siteKey:$id")
            val primary = resp.list.firstOrNull()
            detail = primary
            sourceDetails = resp.list
            if (detail == null) error = "未找到影片信息"
        } catch (e: Exception) { error = e.message }
        finally { loading = false }
    }

    LaunchedEffect(detail?.vodName) {
        val d = detail ?: return@LaunchedEffect
        val name = d.vodName.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        try {
            val resp = api.getMultiDetail(wd = name, keys = "")
            val merged = mutableListOf(d)
            resp.list.forEach { candidate ->
                val exists = merged.any { existing ->
                    existing.siteKey == candidate.siteKey && existing.vodId == candidate.vodId
                }
                if (candidate.vodId.isNotBlank() && !exists) {
                    merged.add(candidate)
                }
            }
            sourceDetails = merged
        } catch (_: Exception) {
            if (sourceDetails.isEmpty()) sourceDetails = listOf(d)
        }
    }

    // ── 相似推荐：多路召回 + 相关性打分，避免只拿同分类首页导致结果泛化 ──
    LaunchedEffect(detail) {
        val d = detail ?: return@LaunchedEffect
        relatedLoading = true
        try {
            val tn = (d.typeName ?: "").replace(Regex("[片剧]$"), "")
            val category = when {
                tn.contains("动漫") || tn.contains("漫") -> "anime" to mapAnimeSub(tn)
                tn.contains("综艺") -> "variety" to null
                tn.contains("剧") -> "tv" to mapTvSub(tn)
                else -> "movie" to mapMovieSub(tn)
            }

            val candidates = linkedMapOf<String, RelatedMediaItem>()
            suspend fun addSearch(keyword: String) {
                if (keyword.length < 2) return
                api.search(keyword).list.forEach { item ->
                    candidates.putIfAbsent("${item.siteKey}:${item.vodId}", item)
                }
            }
            suspend fun addCategory(subType: String?, pageSize: Int) {
                api.getCategory(
                    com.rannuan.tv.data.api.CategoryRequest(
                        category = category.first,
                        subType = subType,
                        page = 1,
                        pageSize = pageSize
                    )
                ).list.forEach { item ->
                    candidates.putIfAbsent("${item.siteKey}:${item.vodId}", item)
                }
            }

            splitPeople(d.vodActor).take(4).forEach { addSearch(it) }
            splitPeople(d.vodDirector).take(2).forEach { addSearch(it) }
            category.second?.let { addCategory(it, 28) }
            addCategory(null, 28)

            relatedVideos = candidates.values
                .filterNot { it.siteKey == d.siteKey && it.vodId == d.vodId }
                .filterNot { it.vodName == d.vodName }
                .map { item -> item to relatedScore(d, item, category.second) }
                .filter { (_, score) -> score > 0 }
                .sortedWith(
                    compareByDescending<Pair<RelatedMediaItem, Int>> { it.second }
                        .thenByDescending { it.first.rating ?: 0.0 }
                        .thenBy { it.first.vodName.length }
                )
                .map { it.first }
                .take(10)
        } catch (_: Exception) {
            relatedVideos = emptyList()
        }
        relatedLoading = false
    }

    // 切换线路/集数时：重置代理状态 + 错误信息，新线路从直连开始尝试
    LaunchedEffect(detail, currentSrc, currentEp) {
        usingProxy = false
        playbackError = null
        pendingResumePos = if (currentSrc == sourceIdx && currentEp == epIdx) pendingResumePos else 0L
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        tryPlayVideo(proxy = false)
    }

    // 全屏：横屏 + 沉浸式系统栏
    LaunchedEffect(isFullscreen, activity) {
        activity?.let { if (isFullscreen) it.enterPlayerFullscreen() else it.exitPlayerFullscreen() }
    }
    DisposableEffect(activity) {
        onDispose { activity?.exitPlayerFullscreen() }
    }
    DisposableEffect(activity, isPlaying) {
        val window = activity?.window
        if (isPlaying) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
    BackHandler(enabled = isFullscreen) { isFullscreen = false }

    fun handleBack() {
        if (isFullscreen) isFullscreen = false else onBack()
    }

    // ════════════════════════════════════════════════
    // 手势处理函数（统一在一处，避免多 gesture detector 冲突）
    // ════════════════════════════════════════════════

    // 双击：播放/暂停切换
    fun handleDoubleTap(@Suppress("UNUSED_PARAMETER") xPercent: Float) {
        if (isPlaying) exoPlayer.pause() else { exoPlayer.playWhenReady = true }
    }

    // 手势层 PointerInput：单击/双击/长按（onPress 感知松手恢复倍速）
    val tapDetector = Modifier.pointerInput(isLocked) {
        if (isLocked) return@pointerInput
        detectTapGestures(
            onTap = { showControls = !showControls },
            onDoubleTap = { offset ->
                val w = size.width.toFloat().coerceAtLeast(1f)
                handleDoubleTap(offset.x / w)
            },
            onLongPress = { longPressActive = true },
            onPress = {
                tryAwaitRelease()
                longPressActive = false
            }
        )
    }

    val iconTint = Color.White.copy(alpha = 0.92f)
    val iconTintSub = Color.White.copy(alpha = 0.7f)

    // ── 渲染 ──
    if (loading) {
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Brand400, strokeWidth = 2.5.dp)
        }
    } else if (error != null || detail == null) {
        ErrorState(error, onBack)
    } else {
        val d = detail!!

        Column(modifier = Modifier.fillMaxSize().background(if (isFullscreen) Color.Black else Zinc950)) {
            // ═══ 播放器（竖屏 16:9 固定；全屏铺满）═══
            Box(
                modifier = if (isFullscreen) {
                    Modifier.fillMaxSize().background(Color.Black)
                } else {
                    Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black)
                }
            ) {
                if (sources.isEmpty()) {
                    NoSourcePlaceholder()
                } else {
                    // ExoPlayer View
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                playerViewRef = this
                                this.player = exoPlayer
                                useController = false
                                keepScreenOn = true
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                // 黑色背景，避免视频未填充时露白
                                setShutterBackgroundColor(android.graphics.Color.BLACK)
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(
                                scaleX = if (mirrorMode == 1) -1f else 1f,
                                scaleY = if (mirrorMode == 2) -1f else 1f
                            )
                    )

                    // 错误遮罩：简洁设计，主打「换线路」
                    playbackError?.let { msg ->
                        Box(
                            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.72f)),
                            contentAlignment = Alignment.Center
                        ) {
                            GlassCard(
                                cornerRadius = 18.dp,
                                tint = Color.Black.copy(alpha = 0.6f),
                                modifier = Modifier.padding(horizontal = 32.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // 错误图标
                                    Icon(
                                        Icons.Outlined.PlayDisabled, null,
                                        tint = Color.White.copy(alpha = 0.45f),
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        msg,
                                        color = Color.White.copy(alpha = 0.75f),
                                        fontSize = 14.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Spacer(Modifier.height(20.dp))
                                    // 主操作：切换线路（打开选集面板）
                                    Surface(
                                        color = Brand400,
                                        shape = RoundedCornerShape(24.dp),
                                        modifier = Modifier.clickable { showEpisodeSheet = true }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
                                        ) {
                                            Icon(
                                                Icons.AutoMirrored.Outlined.List, null,
                                                tint = Color.White, modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text("切换线路", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    // 次操作：重试
                                    TextButton(onClick = {
                                        usingProxy = false
                                        playbackError = null
                                        exoPlayer.stop()
                                        tryPlayVideo(proxy = false)
                                    }) {
                                        Text("重试", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }

                    // 缓冲圈
                    if (buffering && playbackError == null) {
                        CircularProgressIndicator(
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.align(Alignment.Center).size(44.dp), strokeWidth = 3.dp
                        )
                    }

                    // ── 手势层（非锁定时接管所有手势）──
                    if (!isLocked) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .pointerInput(isLocked) {
                                    // ── 本地变量，闭包内跨 onDragStart/onDrag 共享 ──
                                    var startX = 0f
                                    var startY = 0f
                                    var baselineBrightness = 0f
                                    var baselineVolume = 0
                                    var volMax = 15
                                    var localGestureMode: GestureMode? = null

                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            localGestureMode = null
                                            startX = offset.x
                                            startY = offset.y
                                            seekDragging = false

                                            // 捕获当前亮度基线
                                            baselineBrightness = if (brightnessValue < 0f)
                                                activity?.window?.attributes?.screenBrightness ?: 0.5f
                                            else brightnessValue

                                            // 捕获当前音量基线
                                            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                                            if (am != null) {
                                                volMax = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                                baselineVolume = if (volumeValue < 0) am.getStreamVolume(AudioManager.STREAM_MUSIC) else volumeValue
                                            }
                                        },
                                        onDragEnd = {
                                            when (localGestureMode) {
                                                GestureMode.SEEK -> {
                                                    exoPlayer.seekTo(seekPreviewMs)
                                                    currentPosition = seekPreviewMs
                                                }
                                                else -> {}
                                            }
                                            seekDragging = false
                                            localGestureMode = null
                                            showVolumeUi = false
                                            showBrightnessUi = false
                                        },
                                        onDragCancel = {
                                            seekDragging = false
                                            localGestureMode = null
                                            startX = 0f; startY = 0f
                                            showVolumeUi = false
                                            showBrightnessUi = false
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            val w = size.width.toFloat().coerceAtLeast(1f)
                                            val h = size.height.toFloat().coerceAtLeast(1f)

                                            // 首次：通过累计位移判定手势模式（降低阈值，提升跟手度）
                                            if (localGestureMode == null) {
                                                val dx = kotlin.math.abs(change.position.x - startX)
                                                val dy = kotlin.math.abs(change.position.y - startY)
                                                val totalDrag = kotlin.math.sqrt(dx * dx + dy * dy)
                                                // ≈5dp 即可判定，几乎无感知延迟
                                                if (totalDrag > 16f) {
                                                    val leftHalf = startX < w * 0.5f
                                                    // 水平位移 > 垂直位移 × 1.5 → seek；否则亮度/音量
                                                    localGestureMode = if (dx > dy * 1.5f) {
                                                        if (duration > 0) { seekDragging = true; seekPreviewMs = currentPosition; GestureMode.SEEK }
                                                        else GestureMode.VOLUME
                                                    } else {
                                                        if (leftHalf) GestureMode.BRIGHTNESS else GestureMode.VOLUME
                                                    }
                                                    gestureMode = localGestureMode
                                                }
                                            }

                                            when (localGestureMode) {
                                                GestureMode.SEEK -> {
                                                    if (duration > 0) {
                                                        // 增量式 seek：每帧位移 × 比例，2x 加速更跟手
                                                        val deltaMs = (dragAmount.x / w) * duration * 2f
                                                        seekPreviewMs = (seekPreviewMs + deltaMs.toLong()).coerceIn(0, duration)
                                                        showControls = true
                                                    }
                                                }
                                                GestureMode.BRIGHTNESS -> {
                                                    // 向上滑 position.y ↓ → deltaY 为负 → brightness 增大
                                                    val deltaY = (change.position.y - startY) / h
                                                    val next = (baselineBrightness - deltaY).coerceIn(0.01f, 1f)
                                                    brightnessValue = next
                                                    showBrightnessUi = true
                                                }
                                                GestureMode.VOLUME -> {
                                                    val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                                                    if (am != null) {
                                                        val deltaY = (change.position.y - startY) / h
                                                        // 浮点累加：半屏高度 = 150% 音量区间（提供上下边界余量）
                                                        val volFloatAccum = baselineVolume - deltaY * volMax * 1.5f
                                                        val next = volFloatAccum.roundToInt().coerceIn(0, volMax)
                                                        if (next != volumeValue) {
                                                            am.setStreamVolume(AudioManager.STREAM_MUSIC, next, 0)
                                                            volumeValue = next
                                                        }
                                                        showVolumeUi = true
                                                    }
                                                }
                                                null -> {}
                                            }
                                        }
                                    )
                                }
                                .then(tapDetector)
                        )
                    }

                    // ── 浮层控件（统一 BoxScope，避免 align 作用域错误）──
                    Box(Modifier.fillMaxSize()) {
                        doubleTapSide?.let { side ->
                            DoubleTapSeekOverlay(
                                side = side,
                                count = doubleTapCount,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // 长按加速提示
                        if (longPressActive) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 24.dp, vertical = 14.dp)
                            ) {
                                Text("⚡ 长按 2× 加速中", color = Brand400, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // 手势 seek 时的时间预览
                        OverlayAnimatedVisibility(
                            visible = seekDragging,
                            modifier = Modifier.align(Alignment.Center)
                        ) {
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (seekPreviewMs >= currentPosition) Icons.Outlined.FastForward else Icons.Outlined.FastRewind,
                                        null, tint = Brand400, modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "${formatTime(seekPreviewMs)} / ${formatTime(duration)}",
                                        color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        // 亮度指示器 — 左侧竖条
                        OverlayAnimatedVisibility(
                            visible = showBrightnessUi,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 8.dp)
                        ) {
                            SideGestureBar(
                                icon = Icons.Outlined.Brightness6,
                                progress = brightnessValue.coerceIn(0.01f, 1f),
                                label = "${(brightnessValue * 100).toInt().coerceIn(1, 100)}%"
                            )
                        }
                        // 音量指示器 — 右侧竖条
                        OverlayAnimatedVisibility(
                            visible = showVolumeUi,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 8.dp)
                        ) {
                            SideGestureBar(
                                icon = Icons.AutoMirrored.Outlined.VolumeUp,
                                progress = run {
                                    val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                                    val max = am?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
                                    if (max > 0) volumeValue.toFloat() / max else 0f
                                },
                                label = "${volumeValue.coerceAtLeast(0)}"
                            )
                        }

                        // ── 顶部控制栏 ──
                        OverlayAnimatedVisibility(
                            visible = showControls && !isLocked,
                            modifier = Modifier.align(Alignment.TopCenter)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(topControlGradient())
                                    .padding(top = if (isFullscreen) 8.dp else 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 4.dp, end = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 返回
                                    IconButton(onClick = { handleBack() }) {
                                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, tint = iconTint, modifier = Modifier.size(24.dp))
                                    }
                                    // 标题
                                    Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                                        Text(
                                            d.vodName,
                                            color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis
                                        )
                                        activeSource?.episodes?.getOrNull(currentEp)?.title?.let { ep ->
                                            Text(ep,
                                                color = Color.White.copy(alpha = 0.45f),
                                                fontSize = 11.sp, maxLines = 1,
                                                modifier = Modifier.padding(top = 2.dp))
                                        }
                                    }
                                    // 投屏
                                    IconButton(
                                        onClick = {
                                            if (castSession != null) {
                                                // 已投屏中：打开控制面板
                                                showCastSheet = true
                                            } else {
                                                // 未投屏：搜索设备
                                                showCastSheet = true
                                                castError = null
                                                scope.launch {
                                                    isDiscovering = true
                                                    discoveredDevices = emptyList()
                                                    try {
                                                        discoveredDevices = dlnaController.discoverDevices()
                                                    } catch (_: Exception) {}
                                                    isDiscovering = false
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            CastIcon,
                                            null,
                                            tint = if (castSession != null) Brand400 else iconTint,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    // 设置
                                    IconButton(
                                        onClick = { showSettings = true },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Settings,
                                            null, tint = iconTint, modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // ── 锁定层 ──
                        if (isLocked) {
                            Box(
                                Modifier.fillMaxSize().clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { isLocked = false }
                            ) {
                                IconButton(
                                    onClick = { isLocked = false },
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(56.dp)
                                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                ) {
                                    Icon(Icons.Outlined.Lock, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(28.dp))
                                }
                            }
                        }

                        // ═══ 底部控制栏（单行布局：播放/暂停 + 进度条 + 时间 + 全屏）═══
                        OverlayAnimatedVisibility(
                            visible = showControls && !isLocked,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(bottomControlGradient())
                            ) {
                                Row(
                                    Modifier.fillMaxWidth()
                                        .padding(horizontal = if (isFullscreen) 20.dp else 12.dp)
                                        .padding(bottom = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 播放/暂停
                                    IconButton(
                                        onClick = { if (isPlaying) exoPlayer.pause() else { exoPlayer.playWhenReady = true } },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                            null, tint = iconTint, modifier = Modifier.size(26.dp)
                                        )
                                    }
                                    // 下一集（有下一集就固定显示）
                                    val hasNextEp = isFullscreen && (activeSource?.let { currentEp + 1 < it.episodes.size } ?: false)
                                    if (hasNextEp) {
                                        IconButton(
                                            onClick = { switchToEpisode(currentEp + 1) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                Icons.Outlined.SkipNext, null,
                                                tint = Brand400, modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                    // 时间（当前）
                                    Text(
                                        if (seekDragging) formatTime(seekPreviewMs) else formatTime(currentPosition),
                                        color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp,
                                        modifier = Modifier.padding(start = 4.dp, end = 8.dp)
                                    )
                                    // 进度条（weight 撑满中间）
                                    if (duration > 0) {
                                        Box(Modifier.weight(1f)) {
                                            PlayerSeekBar(
                                                position = if (seekDragging) seekPreviewMs else currentPosition,
                                                duration = duration,
                                                buffered = buffered,
                                                onSeek = { ms -> seekDragging = true; seekPreviewMs = ms },
                                                onSeekFinished = { ms ->
                                                    exoPlayer.seekTo(ms); currentPosition = ms
                                                    seekDragging = false
                                                }
                                            )
                                        }
                                    } else {
                                        Spacer(Modifier.weight(1f))
                                    }
                                    // 时间（总时长）
                                    Text(
                                        formatTime(duration),
                                        color = Color.White.copy(alpha = 0.45f), fontSize = 11.sp,
                                        modifier = Modifier.padding(start = 8.dp, end = 4.dp)
                                    )
                                    // 倍速
                                    Box {
                                        Text(
                                            if (longPressActive) "2×" else speedLabel(selectedSpeed),
                                            color = if (selectedSpeed != 1f || longPressActive) Brand400 else iconTintSub,
                                            fontSize = 12.sp, fontWeight = FontWeight.Medium,
                                            modifier = Modifier
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null
                                                ) { showSpeedMenu = !showSpeedMenu }
                                                .padding(horizontal = 8.dp, vertical = 8.dp)
                                        )
                                        DropdownMenu(
                                            expanded = showSpeedMenu,
                                            onDismissRequest = { showSpeedMenu = false },
                                            offset = DpOffset(0.dp, (-180).dp)
                                        ) {
                                            listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).forEach { s ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            "${s}×" + if (s == 1f) "（默认）" else "",
                                                            color = if (s == selectedSpeed) Brand400 else Color.White
                                                        )
                                                    },
                                                    onClick = { selectedSpeed = s; showSpeedMenu = false }
                                                )
                                            }
                                        }
                                    }
                                    // 选集
                                    IconButton(
                                        onClick = { showEpisodeSheet = true },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Outlined.List, null, tint = iconTint, modifier = Modifier.size(22.dp))
                                    }
                                    // 全屏
                                    IconButton(
                                        onClick = { isFullscreen = !isFullscreen },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            if (isFullscreen) Icons.Outlined.FullscreenExit else Icons.Outlined.Fullscreen,
                                            null, tint = iconTint, modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── 右侧设置面板（滑入动画）──
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showSettings,
                        enter = slideInHorizontally(initialOffsetX = { it }),
                        exit = slideOutHorizontally(targetOffsetX = { it }),
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Box(Modifier.fillMaxSize()) {
                            // 半透明遮罩（点击关闭）
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.35f))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { showSettings = false }
                            )
                             // 设置内容面板（右侧滑出，占宽 62%）
                             Box(
                                 Modifier
                                     .fillMaxHeight()
                                     .fillMaxWidth(if (isFullscreen) 0.42f else 0.86f)
                                     .align(Alignment.CenterEnd)
                                     .background(Zinc950.copy(alpha = 0.97f))
                             ) {
                                 PlayerSettingsPanel(
                                     selectedSpeed = selectedSpeed,
                                     skipSeconds = skipSeconds,
                                     resizeMode = resizeMode,
                                     playMode = playMode,
                                     mirrorMode = mirrorMode,
                                     onSpeedChange = { selectedSpeed = it },
                                     onSkipChange = { skipSeconds = it },
                                     onResizeChange = { resizeMode = it },
                                     onPlayModeChange = { playMode = it },
                                     onMirrorChange = { mirrorMode = it },
                                     onClose = { showSettings = false }
                                 )
                             }
                         }
                     }
                }
            }

            // ═══ 详情区域（全屏时隐藏，精简布局：标题→线路→选集横滑→推荐）═══
            if (!isFullscreen) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 40.dp)
                ) {
                    // ── 标题 + 元信息 ──
                    item {
                        Spacer(Modifier.height(16.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(d.vodName, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            // 简介 & 详情按钮
                            TextButton(onClick = { showInfoDialog = true }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                                Icon(Icons.Outlined.Info, null, tint = Brand400, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("简介", color = Brand400, fontSize = 13.sp)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            d.vodYear?.takeIf { it.isNotBlank() }?.let { MetaTag(it) }
                            d.vodArea?.takeIf { it.isNotBlank() }?.let { MetaTag(it) }
                            d.vodRemarks?.takeIf { it.isNotBlank() }?.let {
                                Surface(color = Brand400.copy(alpha = 0.16f), shape = RoundedCornerShape(6.dp)) {
                                    Text(it, color = Brand400, fontSize = 11.sp, maxLines = 1, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                }
                            }
                        }
                    }

                    // ── 播放源（站点）──
                    if (sourceDetails.size > 1) {
                        item {
                            Spacer(Modifier.height(12.dp))
                            Text("播放源", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                sourceDetails.forEach { sourceDetail ->
                                    val selected = sourceDetail.siteKey == d.siteKey && sourceDetail.vodId == d.vodId
                                    FilterChip(
                                        selected = selected,
                                        onClick = { switchToDetail(sourceDetail) },
                                        label = {
                                            Text(
                                                sourceDetail.siteName.takeIf { it.isNotBlank() } ?: sourceDetail.siteKey,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Brand400.copy(alpha = 0.18f),
                                            selectedLabelColor = Brand400,
                                            containerColor = Zinc800,
                                            labelColor = Zinc400
                                        ),
                                        shape = ShapeChip
                                    )
                                }
                            }
                        }
                    }

                    // ── 播放线路 ──
                    if (sources.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(12.dp))
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("播放线路", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.weight(1f))
                                Text("${currentSrc + 1}/${sources.size}", color = Zinc500, fontSize = 11.sp)
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                sources.forEachIndexed { idx, src ->
                                    CompactEpisodeChip(
                                        text = src.name,
                                        selected = idx == currentSrc,
                                        onClick = { switchToLine(idx) },
                                        minWidth = 86.dp
                                    )
                                }
                            }
                        }
                    }

                    // ── 选集横滑条（单行，左滑查看更多）──
                    if (activeSource != null) {
                        item {
                            Spacer(Modifier.height(12.dp))
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("选集", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.weight(1f))
                                TextButton(onClick = { showEpisodeSheet = true }, contentPadding = PaddingValues(0.dp)) {
                                    Text("全部 ${activeSource.episodes.size}集 >", color = Brand400, fontSize = 12.sp)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                activeSource.episodes.take(30).forEachIndexed { eIdx, ep ->
                                    EpisodeButton(
                                        title = ep.title,
                                        selected = eIdx == currentEp,
                                        modifier = Modifier.width(58.dp).height(34.dp),
                                        fontSize = 12,
                                        onClick = { switchToEpisode(eIdx) }
                                    )
                                }
                            }
                        }
                    }

                    // ── 相似推荐（占位）──
                    item {
                        Spacer(Modifier.height(16.dp))
                        Text("相似推荐", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        // 加载中骨架
                        if (relatedLoading) {
                            Row(
                                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                repeat(4) {
                                    Box(
                                        Modifier.width(100.dp).height(140.dp).clip(RoundedCornerShape(10.dp)).background(Zinc800)
                                    )
                                }
                            }
                        } else if (relatedVideos.isNotEmpty()) {
                            // 海报横滑
                            Row(
                                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                relatedVideos.take(10).forEach { video ->
                                    Column(
                                        modifier = Modifier.width(105.dp)
                                            .clip(ShapeCard)
                                            .clickable {
                                                onNavigate("detail/${video.siteKey}/${video.vodId}")
                                            }
                                    ) {
                                        Box(Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(RoundedCornerShape(8.dp)).background(Zinc800)) {
                                            AsyncImage(
                                                model = ImageProxy.proxy(video.vodPic),
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            video.vodName, color = Zinc300, fontSize = 12.sp,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis
                                        )
                                        video.vodRemarks?.takeIf { it.isNotBlank() }?.let {
                                            Text(it, color = Zinc500, fontSize = 11.sp, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        } else {
                            Text("暂无推荐", color = Zinc600, fontSize = 12.sp, modifier = Modifier.padding(vertical = 12.dp))
                        }
                    }
                    item { Spacer(Modifier.height(40.dp)) }
                }
            }
        }

        // ── 简介 & 详情弹窗 ──
        if (showInfoDialog) {
            val content = stripHtml(d.vodContent)
            val actors = d.vodActor?.takeIf { it.isNotBlank() }
                ?.split(Regex("[,，、\\s]+"))?.filter { it.isNotBlank() }?.take(8)
            val director = d.vodDirector?.takeIf { it.isNotBlank() }
                ?.split(Regex("[,，、\\s]+"))?.filter { it.isNotBlank() }?.take(3)
            AlertDialog(
                onDismissRequest = { showInfoDialog = false },
                containerColor = Zinc900,
                title = {
                    Text("影片详情", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                },
                text = {
                    Column {
                        if (content.isNotBlank()) {
                            Text("剧情简介", color = Zinc400, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(4.dp))
                            Text(content, color = Zinc300, fontSize = 13.sp, lineHeight = 20.sp)
                        }
                        if (!director.isNullOrEmpty()) {
                            Spacer(Modifier.height(14.dp))
                            Text("导演", color = Zinc400, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(4.dp))
                            Text(director.joinToString(" / "), color = Zinc300, fontSize = 13.sp)
                        }
                        if (!actors.isNullOrEmpty()) {
                            Spacer(Modifier.height(14.dp))
                            Text("演员", color = Zinc400, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(4.dp))
                            Text(actors.joinToString(" / "), color = Zinc300, fontSize = 13.sp, lineHeight = 20.sp)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showInfoDialog = false }) { Text("关闭", color = Brand400) }
                }
            )
        }

        // ── 选集全部弹窗（紧凑版）──
        if (showEpisodeSheet && activeSource != null) {
            ModalBottomSheet(
                onDismissRequest = { showEpisodeSheet = false },
                sheetState = episodeSheetState,
                containerColor = Zinc900,
                dragHandle = {
                    Box(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp), contentAlignment = Alignment.Center) {
                        Box(Modifier.width(34.dp).height(3.dp).background(Zinc700, RoundedCornerShape(2.dp)))
                    }
                }
            ) {
                Box(Modifier.heightIn(max = 242.dp)) {
                    EpisodeSheetContent(
                        sources = sources,
                        activeSource = activeSource,
                        currentSrc = currentSrc,
                        currentEp = currentEp,
                        onSourceSelect = { idx -> switchToLine(idx) },
                        onEpisodeSelect = { idx -> switchToEpisode(idx); showEpisodeSheet = false }
                    )
                }
            }
        }

        // ── DLNA 投屏面板 ──
        if (showCastSheet) {
            ModalBottomSheet(
                onDismissRequest = { showCastSheet = false },
                containerColor = Zinc900,
                dragHandle = {
                    Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                        Box(Modifier.width(40.dp).height(4.dp).background(Zinc600, RoundedCornerShape(2.dp)))
                    }
                }
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        .heightIn(min = 200.dp, max = 400.dp)
                ) {
                    Text(
                        "投屏到电视", color = Color.White,
                        fontSize = 17.sp, fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))

                    // 当前投屏状态
                    castSession?.let { session ->
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            color = Brand400.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(CastIcon, null, tint = Brand400, modifier = Modifier.size(22.dp))
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        session.device.displayName,
                                        color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        when (session.state) {
                                            DLNACastState.CONNECTING -> "连接中..."
                                            DLNACastState.PLAYING -> "播放中"
                                            DLNACastState.PAUSED -> "已暂停"
                                            DLNACastState.STOPPED -> "已停止"
                                            DLNACastState.ERROR -> "出错"
                                            else -> ""
                                        },
                                        color = Brand400,
                                        fontSize = 12.sp
                                    )
                                }
                                // 播放/暂停
                                IconButton(
                                    onClick = { toggleCastPlayPause() },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        if (session.state == DLNACastState.PLAYING) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        null,
                                        tint = iconTint,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                // 断开
                                IconButton(
                                    onClick = { stopCasting(); showCastSheet = false },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.PlayDisabled, null,
                                        tint = Error500,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 错误提示
                    castError?.let {
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            color = Error500.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                it,
                                color = Error500, fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // 搜索中
                    if (isDiscovering) {
                        Box(
                            Modifier.fillMaxWidth().padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = Brand400, strokeWidth = 2.dp, modifier = Modifier.size(32.dp))
                                Spacer(Modifier.height(12.dp))
                                Text("正在搜索局域网设备...", color = Zinc400, fontSize = 13.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "请确保手机和电视连接同一 Wi-Fi",
                                    color = Zinc600, fontSize = 11.sp
                                )
                            }
                        }
                    } else {
                        // 设备列表
                        if (discoveredDevices.isEmpty()) {
                            Box(
                                Modifier.fillMaxWidth().padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    // 空态图标
                                    Box(
                                        Modifier.size(52.dp).background(White06, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(CastIcon, null, tint = Zinc500, modifier = Modifier.size(26.dp))
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    Text("未发现 DLNA 设备", color = Zinc400, fontSize = 14.sp)
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "请确保电视支持 DLNA 投屏\n并与手机连接同一网络",
                                        color = Zinc600, fontSize = 12.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        lineHeight = 18.sp
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    // 重新搜索按钮
                                    Surface(
                                        color = Brand400.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(20.dp),
                                        modifier = Modifier.clickable {
                                            castError = null
                                            scope.launch {
                                                isDiscovering = true
                                                discoveredDevices = emptyList()
                                                try {
                                                    discoveredDevices = dlnaController.discoverDevices()
                                                } catch (_: Exception) {}
                                                isDiscovering = false
                                            }
                                        }
                                    ) {
                                        Row(
                                            Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Outlined.Settings, null,
                                                tint = Brand400, modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text("重新搜索", color = Brand400, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            }
                        } else {
                            // 标题
                            Text(
                                "发现 ${discoveredDevices.size} 个设备",
                                color = Zinc400, fontSize = 13.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            // 设备列表
                            discoveredDevices.forEach { device ->
                                val isActive = castSession?.device?.uuid == device.uuid
                                Surface(
                                    color = if (isActive) Brand400.copy(alpha = 0.12f) else Zinc800,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                        .clickable {
                                            if (!isActive) {
                                                doCastToDevice(device)
                                                showCastSheet = false
                                            }
                                        }
                                ) {
                                    Row(
                                        Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // 设备图标
                                        Box(
                                            Modifier
                                                .size(44.dp)
                                                .background(
                                                    if (isActive) Brand400.copy(alpha = 0.25f) else Zinc700,
                                                    RoundedCornerShape(10.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                CastIcon, null,
                                                tint = if (isActive) Brand400 else Zinc300,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                device.displayName,
                                                color = if (isActive) Brand400 else Color.White,
                                                fontSize = 14.sp, fontWeight = FontWeight.Medium,
                                                maxLines = 1, overflow = TextOverflow.Ellipsis
                                            )
                                            if (device.manufacturer.isNotBlank()) {
                                                Text(
                                                    device.manufacturer,
                                                    color = Zinc500, fontSize = 11.sp,
                                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        if (isActive) {
                                            Surface(
                                                color = Brand400,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    "已连接",
                                                    color = Color.White,
                                                    fontSize = 11.sp, fontWeight = FontWeight.Medium,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                )
                                            }
                                        } else {
                                            Text(
                                                "投屏 >",
                                                color = Zinc500, fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

// ════════════════════════════════════════════════
// 辅助 Composable
// ════════════════════════════════════════════════

/**
 * 播放设置侧滑面板。
 *
 * 设置项按使用场景分组：播放行为、画面、手势跳秒。比连续 chip 更容易扫读，
 * 也能在横屏时保持足够触摸面积。
 */
@Composable
private fun PlayerSettingsPanel(
    selectedSpeed: Float,
    skipSeconds: Int,
    resizeMode: Int,
    playMode: Int,
    mirrorMode: Int,
    onSpeedChange: (Float) -> Unit,
    onSkipChange: (Int) -> Unit,
    onResizeChange: (Int) -> Unit,
    onPlayModeChange: (Int) -> Unit,
    onMirrorChange: (Int) -> Unit,
    onClose: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 14.dp, top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("播放设置", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text("仅影响当前播放器", color = Zinc500, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                }
                IconButton(onClick = onClose, modifier = Modifier.size(34.dp)) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        null,
                        tint = Zinc300,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        item {
            SettingGroup(title = "播放") {
                SettingsSegmentedRow(
                    title = "默认倍速",
                    options = listOf(0.75f to "0.75x", 1f to "1x", 1.25f to "1.25x", 1.5f to "1.5x", 2f to "2x"),
                    selected = selectedSpeed,
                    onSelect = onSpeedChange
                )
                Spacer(Modifier.height(12.dp))
                SettingsSegmentedRow(
                    title = "播放方式",
                    options = listOf(0 to "顺序", 1 to "单集循环", 2 to "列表循环"),
                    selected = playMode,
                    onSelect = onPlayModeChange
                )
            }
        }

        item {
            SettingGroup(title = "画面") {
                SettingsSegmentedRow(
                    title = "画面尺寸",
                    options = listOf(0 to "适应", 1 to "拉伸", 2 to "裁剪"),
                    selected = resizeMode,
                    onSelect = onResizeChange
                )
                Spacer(Modifier.height(12.dp))
                SettingsSegmentedRow(
                    title = "镜像反转",
                    options = listOf(0 to "正常", 1 to "水平", 2 to "垂直"),
                    selected = mirrorMode,
                    onSelect = onMirrorChange
                )
            }
        }

        item {
            SettingGroup(title = "手势") {
                SettingsSegmentedRow(
                    title = "快进/快退",
                    options = listOf(5 to "5s", 10 to "10s", 15 to "15s", 30 to "30s"),
                    selected = skipSeconds,
                    onSelect = onSkipChange
                )
            }
        }
    }
}

@Composable
private fun SettingGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        color = Zinc900.copy(alpha = 0.92f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
            Text(title, color = Zinc300, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun <T> SettingsSegmentedRow(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    Column {
        Text(title, color = Zinc500, fontSize = 11.sp)
        Spacer(Modifier.height(7.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEach { (value, label) ->
                val isSelected = value == selected
                Surface(
                    color = if (isSelected) Brand400.copy(alpha = 0.20f) else Zinc800,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clickable { onSelect(value) }
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            label,
                            color = if (isSelected) Brand400 else Zinc300,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 浮层 AnimatedVisibility 包装。
 *
 * 显式调用 standalone `androidx.compose.animation.AnimatedVisibility`，避免在
 * Box（嵌套于 Column）内因 ColumnScope 隐式接收者被 material3 的
 * ColumnScope.AnimatedVisibility 扩展抢占而报 "can't be called in this
 * context by implicit receiver" 编译错误。浮层统一 fadeIn/fadeOut。
 */
@Composable
private fun OverlayAnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(),
        exit = fadeOut(),
        content = content
    )
}

/** 选集底部 Sheet 内容 — 移动端优化：紧凑行列、去标题、高触达 */
@Composable
private fun EpisodeSheetContent(
    sources: List<PlaySource>,
    activeSource: PlaySource,
    currentSrc: Int,
    currentEp: Int,
    onSourceSelect: (Int) -> Unit,
    onEpisodeSelect: (Int) -> Unit
) {
    val gridState = rememberLazyGridState()
    val chunkSize = 30
    val pageCount = ((activeSource.episodes.size + chunkSize - 1) / chunkSize).coerceAtLeast(1)
    val currentEpisodePage = (currentEp / chunkSize).coerceIn(0, pageCount - 1)
    var selectedPage by remember(activeSource) { mutableIntStateOf(currentEpisodePage) }
    val pageStart = selectedPage * chunkSize
    val pageEpisodes = activeSource.episodes.drop(pageStart).take(chunkSize)

    LaunchedEffect(activeSource, currentEp) {
        selectedPage = currentEpisodePage
    }

    LaunchedEffect(activeSource, selectedPage, currentEp) {
        val targetIndex = (currentEp - pageStart).coerceAtLeast(0)
        if (selectedPage == currentEpisodePage && targetIndex < pageEpisodes.size) {
            gridState.scrollToItem(targetIndex / 5)
        } else {
            gridState.scrollToItem(0)
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("选集", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    activeSource.episodes.getOrNull(currentEp)?.title ?: "当前播放",
                    color = Zinc500,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text("${currentEp + 1}/${activeSource.episodes.size}", color = Brand400, fontSize = 11.sp)
        }

        // ── 播放线路（横滑）──
        if (sources.size > 1) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                sources.forEachIndexed { idx, src ->
                    CompactEpisodeChip(
                        text = src.name,
                        selected = idx == currentSrc,
                        onClick = { onSourceSelect(idx) },
                        minWidth = 72.dp
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
        }

        if (pageCount > 1) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(pageCount) { page ->
                    val selected = page == selectedPage
                    CompactEpisodeChip(
                        text = "${page * chunkSize + 1}-${minOf((page + 1) * chunkSize, activeSource.episodes.size)}",
                        selected = selected,
                        onClick = { selectedPage = page },
                        minWidth = 54.dp
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            state = gridState,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 132.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
            contentPadding = PaddingValues(bottom = 6.dp)
        ) {
            items(
                count = pageEpisodes.size,
                key = { idx -> pageStart + idx }
            ) { idx ->
                val eIdx = pageStart + idx
                val ep = pageEpisodes[idx]
                EpisodeButton(
                    title = ep.title,
                    selected = eIdx == currentEp,
                    modifier = Modifier.fillMaxWidth().height(30.dp),
                    fontSize = 11,
                    onClick = { onEpisodeSelect(eIdx) }
                )
            }
        }
    }
}

@Composable
private fun EpisodeButton(
    title: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    fontSize: Int = 11,
    onClick: () -> Unit
) {
    val cellShape = RoundedCornerShape(8.dp)
    val pulse by if (selected) {
        rememberInfiniteTransition(label = "episodePlayingPulse").animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 900),
                repeatMode = RepeatMode.Reverse
            ),
            label = "episodePlayingPulseValue"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }
    Box(
        modifier = modifier
            .clip(cellShape)
            .background(if (selected) Brand400.copy(alpha = 0.18f + pulse * 0.08f) else Zinc800)
            .border(
                width = if (selected) 1.dp else 0.dp,
                color = if (selected) Brand400.copy(alpha = 0.45f + pulse * 0.45f) else Color.Transparent,
                shape = cellShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            title,
            fontSize = fontSize.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = if (selected) Brand400 else Zinc300,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(start = 6.dp, end = if (selected) 16.dp else 6.dp)
        )
        if (selected) {
            PlayingBars(
                pulse = pulse,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 5.dp)
            )
        }
    }
}

@Composable
private fun PlayingBars(
    pulse: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.height(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(1.5.dp)
    ) {
        listOf(
            0.45f + pulse * 0.45f,
            0.95f - pulse * 0.35f,
            0.55f + pulse * 0.35f
        ).forEach { heightRatio ->
            Box(
                Modifier
                    .width(2.dp)
                    .height((4 + heightRatio.coerceIn(0f, 1f) * 8).dp)
                    .clip(CircleShape)
                    .background(Brand400.copy(alpha = 0.75f + pulse * 0.25f))
            )
        }
    }
}

@Composable
private fun CompactEpisodeChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    minWidth: androidx.compose.ui.unit.Dp
) {
    Surface(
        color = if (selected) Brand400.copy(alpha = 0.2f) else Zinc800,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .height(26.dp)
            .width(minWidth)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text,
                color = if (selected) Brand400 else Zinc400,
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
        }
    }
}

/**
 * 现代风格进度条：更粗轨道、大圆点 thumb、品牌色播放进度。
 *
 * 交互：点击跳转 + 拖拽 seek；thumb 分层渲染避免被 track clip 裁剪。
 */
@AndroidOptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])
@Composable
private fun PlayerSeekBar(
    position: Long,
    duration: Long,
    buffered: Long,
    onSeek: (Long) -> Unit,
    onSeekFinished: (Long) -> Unit
) {
    val progress = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
    val bufferedProgress = if (duration > 0) (buffered.toFloat() / duration).coerceIn(0f, 1f) else 0f
    var dragProgress by remember { mutableStateOf<Float?>(null) }
    val p = dragProgress ?: progress
    val isDragging = dragProgress != null
    var barWidthPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    // 拖拽时 thumb 放大，常态保持可见
    val thumbSize by animateDpAsState(if (isDragging) 32.dp else 24.dp, label = "thumb")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { barWidthPx = it.width }
            .height(44.dp) // 足够的触摸区域
            .pointerInput(duration) {
                detectTapGestures { offset ->
                    if (duration > 0) {
                        val ratio = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeekFinished((ratio * duration).toLong())
                    }
                }
            }
            .pointerInput(duration) {
                detectDragGestures(
                    onDragStart = { offset ->
                        if (duration > 0) dragProgress = (offset.x / size.width).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        dragProgress?.let { pp -> onSeekFinished((pp * duration).toLong()) }
                        dragProgress = null
                    },
                    onDragCancel = { dragProgress = null },
                    onDrag = { change, _ ->
                        change.consume()
                        if (duration > 0) {
                            val np = (change.position.x / size.width).coerceIn(0f, 1f)
                            dragProgress = np
                            onSeek((np * duration).toLong())
                        }
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        // ── 轨道层（3dp 高，圆角）──
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.18f))
        ) {
            // 缓冲进度
            Box(
                Modifier.fillMaxWidth(bufferedProgress).fillMaxHeight()
                    .background(Color.White.copy(alpha = 0.28f))
            )
            // 播放进度（品牌橙色）
            Box(
                Modifier.fillMaxWidth(p).fillMaxHeight().background(Brand400)
            )
        }
        // ── thumb 层：App Logo 图标 ──
        val thumbSizePx = with(density) { thumbSize.roundToPx() }
        val thumbOffsetPx = ((barWidthPx - thumbSizePx) * p).roundToInt().coerceAtLeast(0)
        Icon(
            painter = painterResource(R.drawable.thumb_logo),
            contentDescription = "进度",
            tint = Color.Unspecified,
            modifier = Modifier
                .offset { androidx.compose.ui.unit.IntOffset(thumbOffsetPx, 0) }
                .size(thumbSize)
                .shadow(if (isDragging) 6.dp else 2.dp, CircleShape)
                .clip(CircleShape)
                .background(if (isDragging) Brand400.copy(alpha = 0.12f) else Color.Transparent, CircleShape)
                .padding(1.dp)
        )
    }
}

/** 统一风格播放器控制按钮：半透明圆形背景 + 白色图标 */
@Composable
private fun PlayerCtrlIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    size: Int = 40
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size((size * 0.55).toInt().dp))
    }
}

/** 侧边手势指示器（MX Player 风格竖条：图标 + 纵向进度 + 数值） */
@Composable
private fun SideGestureBar(icon: androidx.compose.ui.graphics.vector.ImageVector, progress: Float, label: String) {
    Column(
        modifier = Modifier
            .width(40.dp)
            .height(180.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 图标
        Icon(icon, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(18.dp))
        // 纵向进度条
        Box(
            Modifier
                .width(4.dp)
                .height(100.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.15f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(progress.coerceIn(0f, 1f))
                    .clip(CircleShape)
                    .background(Brand400)
            )
        }
        // 数值
        Text(
            label, color = Color.White.copy(alpha = 0.8f),
            fontSize = 11.sp, fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

/** YouTube 式双击 seek 扇形叠加动画 */
@Composable
private fun DoubleTapSeekOverlay(side: SeekSide, count: Int, modifier: Modifier = Modifier) {
    val alignment = if (side == SeekSide.LEFT) Alignment.CenterStart else Alignment.CenterEnd
    Box(modifier = modifier) {
        Column(
            modifier = Modifier.align(alignment).padding(horizontal = 52.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.size(56.dp).background(Color.White.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (side == SeekSide.LEFT) Icons.Outlined.FastRewind else Icons.Outlined.FastForward,
                    null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(34.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
            Surface(color = Color.Black.copy(alpha = 0.55f), shape = RoundedCornerShape(8.dp)) {
                Text(
                    if (side == SeekSide.LEFT) "-${count * 10}s" else "+${count * 10}s",
                    color = Brand300, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun ErrorState(error: String?, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Zinc950), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(72.dp).background(Error500.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.ErrorOutline, null, tint = Error500, modifier = Modifier.size(36.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(error ?: "加载失败", color = Zinc300, fontSize = 14.sp)
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onBack) { Text("返回", color = Brand400) }
        }
    }
}

@Composable
private fun NoSourcePlaceholder() {
    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(72.dp).background(Zinc700.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.PlayDisabled, null, tint = Zinc500, modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text("暂无播放源", color = Zinc500, fontSize = 14.sp)
        }
    }
}

/** 详情页元信息小标签（年份/地区） */
@Composable
private fun MetaTag(text: String) {
    Surface(color = White06, shape = ShapeChip) {
        Text(text, color = Zinc300, fontSize = 12.sp, maxLines = 1, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

// ════════════════════════════════════════════════
// 自定义图标 / 枚举 / 工具函数
// ════════════════════════════════════════════════

/** Bootstrap cast 图标（16x16 矢量） */
private val CastIcon: androidx.compose.ui.graphics.vector.ImageVector by lazy {
    androidx.compose.ui.graphics.vector.ImageVector.Builder(
        name = "Cast", defaultWidth = 16.dp, defaultHeight = 16.dp,
        viewportWidth = 16f, viewportHeight = 16f
    ).apply {
        addPath(
            pathData = androidx.compose.ui.graphics.vector.PathParser().parsePathString(
                "M7.646 9.354l-3.792 3.792a0.5 0.5 0 0 0 0.353 0.854h7.586a0.5 0.5 0 0 0 0.354-0.854L8.354 9.354a0.5 0.5 0 0 0-0.708 0z"
            ).toNodes(),
            fill = androidx.compose.ui.graphics.SolidColor(androidx.compose.ui.graphics.Color.Black)
        )
        addPath(
            pathData = androidx.compose.ui.graphics.vector.PathParser().parsePathString(
                "M11.414 11H14.5a0.5 0.5 0 0 0 0.5-0.5v-7a0.5 0.5 0 0 0-0.5-0.5h-13a0.5 0.5 0 0 0-0.5 0.5v7a0.5 0.5 0 0 0 0.5 0.5h3.086l-1 1H1.5A1.5 1.5 0 0 1 0 10.5v-7A1.5 1.5 0 0 1 1.5 2h13A1.5 1.5 0 0 1 16 3.5v7a1.5 1.5 0 0 1-1.5 1.5h-2.086l-1-1z"
            ).toNodes(),
            fill = androidx.compose.ui.graphics.SolidColor(androidx.compose.ui.graphics.Color.Black)
        )
    }.build()
}

private enum class SeekSide { LEFT, RIGHT }
private enum class GestureMode { SEEK, BRIGHTNESS, VOLUME }

private fun formatTime(ms: Long): String {
    val t = (ms / 1000).coerceAtLeast(0)
    val h = t / 3600
    val m = (t % 3600) / 60
    val s = t % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

private fun speedLabel(speed: Float): String =
    if (speed == 1f) "倍速" else "${speed}×"

// ── 相似推荐类型映射（type_name → subType，与服务端 SUB_TYPE_MAP 对应）──
private fun mapMovieSub(tn: String): String? = when {
    tn.contains("动作") -> "动作"
    tn.contains("喜剧") -> "喜剧"
    tn.contains("爱情") -> "爱情"
    tn.contains("科幻") -> "科幻"
    tn.contains("恐怖") || tn.contains("惊悚") -> "恐怖"
    tn.contains("剧情") -> "剧情"
    tn.contains("战争") -> "战争"
    tn.contains("动画") -> "动画"
    tn.contains("纪录") -> "纪录片"
    else -> null
}
private fun mapTvSub(tn: String): String? = when {
    tn.contains("国产") || tn.contains("大陆") || tn.contains("内地") -> "国产"
    tn.contains("香港") -> "港剧"
    tn.contains("台湾") -> "台剧"
    tn.contains("韩国") || tn.contains("韩") -> "韩剧"
    tn.contains("日本") || tn.contains("日") -> "日剧"
    tn.contains("美国") || tn.contains("欧美") -> "美剧"
    tn.contains("泰国") || tn.contains("泰") -> "泰剧"
    else -> null
}
private fun mapAnimeSub(tn: String): String? = when {
    tn.contains("国产") || tn.contains("国漫") -> "国漫"
    tn.contains("日本") || tn.contains("日韩") || tn.contains("日漫") -> "日漫"
    tn.contains("欧美") -> "欧美"
    else -> null
}

private fun splitPeople(raw: String?): List<String> =
    raw.orEmpty()
        .split(Regex("[,，、/\\s]+"))
        .map { it.trim() }
        .filter { it.length >= 2 && it !in listOf("未知", "内详", "暂无") }
        .distinct()

private fun titleTokens(title: String): Set<String> {
    val clean = title
        .replace(Regex("第[一二三四五六七八九十0-9]+季"), "")
        .replace(Regex("[\\s:：·.。!！?？-]+"), "")
    val tokens = mutableSetOf<String>()
    for (len in 2..3) {
        if (clean.length >= len) {
            for (i in 0..clean.length - len) tokens.add(clean.substring(i, i + len))
        }
    }
    return tokens
}

private fun relatedScore(current: MediaDetail, item: RelatedMediaItem, expectedSubType: String?): Int {
    var score = 0
    val itemType = item.typeName.orEmpty()
    val currentType = current.typeName.orEmpty()
    if (expectedSubType != null && itemType.contains(expectedSubType)) score += 35
    if (currentType.isNotBlank() && itemType.isNotBlank() && currentType == itemType) score += 24
    if (!current.vodYear.isNullOrBlank() && current.vodYear == item.vodYear) score += 8
    if (!current.vodArea.isNullOrBlank() && current.vodArea == item.vodArea) score += 8

    val people = splitPeople(current.vodActor).take(4).toSet()
    val itemPeople = splitPeople(item.vodActor).toSet()
    val sharedPeople = people.intersect(itemPeople).size
    score += sharedPeople * 34

    val sharedTitleTokens = titleTokens(current.vodName).intersect(titleTokens(item.vodName)).size
    score += (sharedTitleTokens * 3).coerceAtMost(18)

    if (item.vodRemarks?.contains("完结") == true || item.vodRemarks?.contains("更新") == true) score += 2
    return score
}

private fun topControlGradient(): Brush = Brush.verticalGradient(
    listOf(Color.Black.copy(alpha = 0.65f), Color.Black.copy(alpha = 0.0f))
)

private fun bottomControlGradient(): Brush = Brush.verticalGradient(
    listOf(Color.Black.copy(alpha = 0.0f), Color.Black.copy(alpha = 0.72f))
)

/** 进入播放器全屏：横屏 + 隐藏系统栏 */
@Suppress("SourceLockedOrientationActivity")
private fun Activity.enterPlayerFullscreen() {
    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    WindowCompat.setDecorFitsSystemWindows(window, false)
    WindowInsetsControllerCompat(window, window.decorView).apply {
        hide(WindowInsetsCompat.Type.systemBars())
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}

/** 退出播放器全屏：恢复竖屏 + 显示系统栏 */
@Suppress("SourceLockedOrientationActivity")
private fun Activity.exitPlayerFullscreen() {
    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    WindowCompat.setDecorFitsSystemWindows(window, false)
    WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
}

/** 递归查找 Activity（用于设置亮度 / 全屏） */
private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * 解析视频源列表。
 *
 * 线路命名使用 formatSourceName（对标桌面端），显示"量子.线路一"等中文名，
 * 不再透传 vodPlayFrom 中的原始标识（如 m3u8、ff 等）。
 */
private fun parseSources(d: MediaDetail): List<PlaySource> {
    val urlStr = d.vodPlayUrl ?: return emptyList()
    val fromStr = d.vodPlayFrom ?: ""
    val urlParts = urlStr.split("$$$")
    val fromParts = fromStr.split("$$$")
    return urlParts.mapIndexed { i, part ->
        val eps = part.split("#").filter { it.isNotBlank() }.map { ep ->
            val idx = ep.indexOf("$")
            if (idx == -1) PlayEpisode(ep, ep) else PlayEpisode(ep.substring(0, idx), ep.substring(idx + 1))
        }
        val rawName = fromParts.getOrElse(i) { "线路${i + 1}" }.trim()
        PlaySource(formatSourceName(rawName, i, d.siteKey), eps)
    }.filter { it.episodes.isNotEmpty() }
}

private data class PlaySource(val name: String, val episodes: List<PlayEpisode>)
private data class PlayEpisode(val title: String, val url: String)
