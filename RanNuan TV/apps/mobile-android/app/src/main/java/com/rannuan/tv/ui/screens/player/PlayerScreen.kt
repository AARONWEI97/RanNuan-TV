package com.rannuan.tv.ui.screens.player

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.util.Log
import android.view.WindowManager
import android.os.SystemClock
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Brightness6
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.FastRewind
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PlayDisabled
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.rannuan.tv.R
import com.rannuan.tv.data.api.RanNuanApi
import com.rannuan.tv.data.model.MediaDetail
import com.rannuan.tv.data.model.MediaItem as RelatedMediaItem
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
import com.rannuan.tv.ui.util.FavoritesStore
import com.rannuan.tv.ui.util.WatchingHistoryStore
import com.rannuan.tv.ui.util.formatSourceName
import com.rannuan.tv.ui.util.stripHtml
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt
import androidx.annotation.OptIn as AndroidOptIn

/**
 * 播放器页面 —— 爱优腾级交互（Media3 + 自定义 Compose 手势层）。
 *
 * 手势交互（覆盖在 PlayerView 之上的透明手势层）：
 *  - 单击：显隐控制栏
 *  - 双击任意位置：播放 / 暂停
 *  - 左半屏竖向拖拽：调节屏幕亮度
 *  - 右半屏竖向拖拽：调节音量
 *  - 长按左半屏 / 右半屏：持续 2 倍速快退 / 快进，松手恢复
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
    name: String = "",
    keys: String = "",
    api: RanNuanApi,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val activity = remember { context.findActivity() }
    val playerPrefs = remember(context) {
        context.getSharedPreferences("rannuan_player_settings", Context.MODE_PRIVATE)
    }

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
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(15_000, 50_000, 800, 1_500)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dsFactory))
            .setLoadControl(loadControl)
            .build()
    }
    var showControls by remember { mutableStateOf(true) }
    var showEpisodeSheet by remember { mutableStateOf(false) }
    var showSourceSheet by remember { mutableStateOf(false) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var isFullscreen by rememberSaveable { mutableStateOf(false) }
    var resizeMode by rememberSaveable { mutableIntStateOf(playerPrefs.getInt("resize_mode", 0)) }
    var playMode by rememberSaveable { mutableIntStateOf(playerPrefs.getInt("play_mode", 0)) }
    var mirrorMode by rememberSaveable { mutableIntStateOf(playerPrefs.getInt("mirror_mode", 0)) }
    var autoSwitchSource by rememberSaveable { mutableStateOf(playerPrefs.getBoolean("auto_switch_source", true)) }
    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }
    val episodeSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sourceSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 播放状态
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }
    var buffered by remember { mutableLongStateOf(0L) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var usingProxy by remember { mutableStateOf(false) }
    var forceProxyHls by remember { mutableStateOf(false) }
    var selectedSpeed by rememberSaveable { mutableFloatStateOf(playerPrefs.getFloat("playback_speed", 1f)) }
    var isLocked by remember { mutableStateOf(false) }
    var buffering by remember { mutableStateOf(false) }
    var showBufferingHint by remember { mutableStateOf(false) }
    var bufferedAheadMs by remember { mutableLongStateOf(0L) }
    var transferBytes by remember { mutableLongStateOf(0L) }
    var transferRateText by remember { mutableStateOf("--") }
    var renderedFirstFrame by remember { mutableStateOf(false) }
    var playbackStartedAt by remember { mutableLongStateOf(0L) }
    var autoSwitchNotice by remember { mutableStateOf<String?>(null) }
    var favorited by remember { mutableStateOf(false) }

    // 相似推荐
    var relatedVideos by remember { mutableStateOf<List<RelatedMediaItem>>(emptyList()) }
    var relatedLoading by remember { mutableStateOf(false) }

    // 多站点播放源
    var sourceDetails by remember { mutableStateOf<List<MediaDetail>>(emptyList()) }

    // ── 手势临时状态 ──
    // 拖拽 seek
    var seekDragging by remember { mutableStateOf(false) }
    var seekPreviewMs by remember { mutableLongStateOf(0L) }
    // 长按左右区域持续 2 倍速快退 / 快进
    var longPressSpeedSide by remember { mutableStateOf<SeekSide?>(null) }
    var longPressWasPlaying by remember { mutableStateOf(false) }
    var longPressRewindJob by remember { mutableStateOf<Job?>(null) }
    // 亮度/音量
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
    val sourceOptions = remember(sourceDetails, detail) {
        buildPlayerSourceOptions(sourceDetails, detail)
    }
    val latestSources by rememberUpdatedState(sources)
    val latestSourceOptions by rememberUpdatedState(sourceOptions)
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

    fun targetEpisodeIndex(targetSource: PlaySource, preserveEpisode: Boolean): Int {
        if (!preserveEpisode || targetSource.episodes.isEmpty()) return 0
        val currentTitle = sources.getOrNull(currentSrc)?.episodes?.getOrNull(currentEp)?.title
        val exact = currentTitle?.let { title -> targetSource.episodes.indexOfFirst { it.title == title } } ?: -1
        return if (exact >= 0) exact else currentEp.coerceIn(0, targetSource.episodes.lastIndex)
    }

    fun switchToRoute(d: MediaDetail, sourceIndex: Int, preserveEpisode: Boolean = true) {
        val targetSources = parseSources(d)
        val safeSourceIndex = sourceIndex.coerceIn(0, (targetSources.size - 1).coerceAtLeast(0))
        val targetSource = targetSources.getOrNull(safeSourceIndex) ?: return
        if (detail?.siteKey == d.siteKey && detail?.vodId == d.vodId && currentSrc == safeSourceIndex) return
        val targetEpisode = targetEpisodeIndex(targetSource, preserveEpisode)
        savePlayPosition()
        detail = d
        currentSrc = safeSourceIndex
        currentEp = targetEpisode
        pendingResumePos = 0L
        showControls = true
    }

    fun switchToLine(idx: Int) {
        if (idx !in sources.indices) return
        val targetEpisode = targetEpisodeIndex(sources[idx], preserveEpisode = true)
        savePlayPosition()
        currentSrc = idx
        currentEp = targetEpisode
        pendingResumePos = 0L
        showControls = true
    }

    fun switchToNextRoute(): Boolean {
        val options = latestSourceOptions
        val currentIndex = options.indexOfFirst {
            it.detail.siteKey == detail?.siteKey &&
                it.detail.vodId == detail?.vodId &&
                it.sourceIndex == currentSrc
        }
        val next = options.getOrNull(currentIndex + 1) ?: return false
        switchToRoute(next.detail, next.sourceIndex, preserveEpisode = true)
        autoSwitchNotice = "当前线路不可用，已切换至 ${next.displayName}"
        return true
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

    fun openCastPanel() {
        showCastSheet = true
        castError = null
        if (castSession != null) return
        scope.launch {
            isDiscovering = true
            discoveredDevices = emptyList()
            try {
                discoveredDevices = dlnaController.discoverDevices()
            } catch (_: Exception) {
                discoveredDevices = emptyList()
            }
            isDiscovering = false
        }
    }

    fun shareCurrentVideo() {
        val d = detail ?: return
        val episode = sources.getOrNull(currentSrc)?.episodes?.getOrNull(currentEp)?.title.orEmpty()
        val text = buildString {
            append("我正在冉暖TV观看《${d.vodName}》")
            if (episode.isNotBlank()) append(" $episode")
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "分享影片"))
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

    // ── 播放辅助函数：真实媒体地址先直连，分享页/无扩展名地址直接走代理解析。
    fun tryPlayVideo(proxy: Boolean, fromError: Boolean = false, forceHlsMime: Boolean = false) {
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

        val isM3u8 = isM3u8Url(raw)
        val isTs = isTsUrl(raw)
        val looksLikeSharePage = !isM3u8 && !isTs && !hasDirectVideoFileExtension(raw)

        // 分享页/无扩展名播放地址通常返回 HTML，不能直喂 ExoPlayer，需要先交给后端解析。
        // 标准 m3u8、mp4、ts 等真实媒体地址仍先直连，失败后再切代理。
        val useProxy = proxy || looksLikeSharePage
        val uri = if (useProxy) {
            "${com.rannuan.tv.BuildConfig.SERVER_URL}/api/proxy?url=${Uri.encode(raw)}"
        } else raw

        Log.d("PlayerScreen", "播放: ${if(useProxy) "代理" else "直连"} | m3u8=$isM3u8 | ts=$isTs | share=$looksLikeSharePage | 原始=${raw.take(80)}")
        Log.d("PlayerScreen", "最终URI: ${uri.take(120)}")

        // 记录代理状态，避免 onPlayerError 里重复走代理
        if (useProxy) usingProxy = true
        renderedFirstFrame = false
        playbackStartedAt = SystemClock.elapsedRealtime()

        // 直连时设置 Referer 头绕过防盗链
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
        // 代理分享页通常返回 m3u8；TS 是二进制流，移动端显式标注 MPEG-TS。
        val shouldForceHls = isM3u8 || forceHlsMime || (useProxy && (looksLikeSharePage || forceProxyHls))
        val mediaItem = when {
            shouldForceHls -> MediaItem.Builder().setUri(uri).setMimeType(MimeTypes.APPLICATION_M3U8).build()
            isTs -> MediaItem.Builder().setUri(uri).setMimeType(MimeTypes.VIDEO_MP2T).build()
            else -> MediaItem.fromUri(uri)
        }
        // 记录观看历史
        detail?.let { d ->
            val epTitle = sources.getOrNull(currentSrc)?.episodes?.getOrNull(currentEp)?.title ?: ""
            WatchingHistoryStore.addHistory(
                context, d.siteKey, d.vodId,
                d.vodName, d.vodPic ?: "", epTitle,
                position = pendingResumePos,
                sourceIndex = currentSrc,
                episodeIndex = currentEp,
                preserveExistingPosition = fromError
            )
        }

        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        exoPlayer.play()

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
                    bufferedAheadMs = (exoPlayer.bufferedPosition - exoPlayer.currentPosition).coerceAtLeast(0L)
                    // 续播：首次就绪后跳到历史位置（仅执行一次）
                    if (pendingResumePos > 0) {
                        val target = if (duration > 0L) {
                            pendingResumePos.coerceAtMost(duration)
                        } else {
                            pendingResumePos
                        }
                        exoPlayer.seekTo(target)
                        pendingResumePos = 0
                    }
                } else if (state == Player.STATE_ENDED) {
                    isPlaying = false
                    handleEpisodeEnded()
                }
            }
            override fun onPlayerError(e: PlaybackException) {
                if (!usingProxy) {
                    // 直连失败 → 自动切代理重试。先按原样代理，再用 HLS 兜底。
                    usingProxy = true
                    tryPlayVideo(proxy = true, fromError = true)
                } else if (!forceProxyHls) {
                    // 代理 URL 无扩展名或后端解析结果与原 URL 后缀不一致时，再按 HLS 明确重试一次
                    forceProxyHls = true
                    tryPlayVideo(proxy = true, fromError = true, forceHlsMime = true)
                } else {
                    // 代理也失败：允许设置为自动尝试下一条线路。
                    if (!autoSwitchSource || !switchToNextRoute()) {
                        playbackError = "当前线路播放失败，请尝试切换其他线路"
                    }
                }
            }

            override fun onIsLoadingChanged(isLoading: Boolean) {
                buffering = isLoading || exoPlayer.playbackState == Player.STATE_BUFFERING
            }

            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                // no-op
            }

            override fun onRenderedFirstFrame() {
                renderedFirstFrame = true
            }
        }
        val analyticsListener = object : AnalyticsListener {
            override fun onBandwidthEstimate(
                eventTime: AnalyticsListener.EventTime,
                totalLoadTimeMs: Int,
                totalBytesLoaded: Long,
                bitrateEstimate: Long
            ) {
                if (totalBytesLoaded > 0) transferBytes += totalBytesLoaded
                if (bitrateEstimate > 0) transferRateText = formatNetworkSpeed(bitrateEstimate)
            }
        }
        exoPlayer.addListener(listener)
        exoPlayer.addAnalyticsListener(analyticsListener)
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
            exoPlayer.removeAnalyticsListener(analyticsListener)
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
                bufferedAheadMs = (buffered - currentPosition).coerceAtLeast(0L)
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

    // 菜单倍速是常驻设置；长按期间会临时覆盖，松手恢复该值。
    LaunchedEffect(selectedSpeed) { exoPlayer.setPlaybackSpeed(selectedSpeed) }

    LaunchedEffect(selectedSpeed, resizeMode, playMode, mirrorMode, autoSwitchSource) {
        playerPrefs.edit()
            .putFloat("playback_speed", selectedSpeed)
            .putInt("resize_mode", resizeMode)
            .putInt("play_mode", playMode)
            .putInt("mirror_mode", mirrorMode)
            .putBoolean("auto_switch_source", autoSwitchSource)
            .apply()
    }

    LaunchedEffect(autoSwitchNotice) {
        if (autoSwitchNotice != null) {
            delay(2600)
            autoSwitchNotice = null
        }
    }


    LaunchedEffect(detail?.siteKey, detail?.vodId) {
        val d = detail
        favorited = d != null && FavoritesStore.isFavorited(context, d.siteKey, d.vodId)
    }

    LaunchedEffect(buffering, isPlaying, playbackError) {
        if (buffering && !isPlaying && playbackError == null) {
            delay(900)
            showBufferingHint = buffering && !isPlaying && playbackError == null
        } else {
            showBufferingHint = false
        }
    }

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
    LaunchedEffect(siteKey, id, name, keys) {
        loading = true; error = null
        try {
            val titleQuery = name
            val direct = runCatching { api.getDetail(siteKey, id) }.getOrNull()?.let { item ->
                item.copy(
                    siteKey = item.siteKey.ifBlank { siteKey },
                    vodId = item.vodId.ifBlank { id }
                )
            }
            val fallback = if (direct == null) {
                val keyParam = keys.takeIf { it.isNotBlank() } ?: "$siteKey:$id"
                api.getMultiDetail(wd = titleQuery, keys = keyParam).list
            } else {
                emptyList()
            }
            val primary = direct ?: fallback.firstOrNull { it.siteKey == siteKey && it.vodId == id } ?: fallback.firstOrNull()
            detail = primary
            sourceDetails = listOfNotNull(primary)
            if (primary == null) {
                error = "未找到影片信息"
            } else {
                val loadedSources = parseSources(primary)
                if (loadedSources.isEmpty()) {
                    error = "当前站点没有可用播放线路"
                } else {
                    currentSrc = sourceIdx.coerceIn(0, loadedSources.lastIndex)
                    currentEp = epIdx.coerceIn(0, loadedSources[currentSrc].episodes.lastIndex)
                }
            }
        } catch (e: Exception) { error = e.message }
        finally { loading = false }
    }

    LaunchedEffect(detail?.vodName, keys) {
        val d = detail ?: return@LaunchedEffect
        val detailTitle = d.vodName.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        val preciseKeys = keys.takeIf { it.contains(',') }.orEmpty()
        if (preciseKeys.isNotBlank()) {
            delay(350)
        } else {
            delay(1200)
        }
        try {
            var resp = api.getMultiDetail(wd = detailTitle, keys = preciseKeys)
            val merged = mutableListOf(d)
            fun appendCandidates(candidates: List<MediaDetail>) {
                candidates.forEach { candidate ->
                    val exists = merged.any { existing ->
                        existing.siteKey == candidate.siteKey && existing.vodId == candidate.vodId
                    }
                    if (candidate.vodId.isNotBlank() && !exists) {
                        merged.add(candidate)
                    }
                }
            }
            appendCandidates(resp.list)
            sourceDetails = merged.toList()

            if (resp.complete == false) {
                delay(1200)
                resp = api.getMultiDetail(wd = detailTitle, keys = preciseKeys)
                appendCandidates(resp.list)
            }
            sourceDetails = merged
        } catch (_: Exception) {
            if (sourceDetails.isEmpty()) sourceDetails = listOf(d)
        }
    }

    // ── 相似推荐：优先走桌面端同款分类召回，失败再用演员搜索兜底。保证先有结果，再谈排序。
    LaunchedEffect(detail, renderedFirstFrame) {
        val d = detail ?: return@LaunchedEffect
        if (!renderedFirstFrame) return@LaunchedEffect
        delay(1500)
        relatedLoading = true
        relatedVideos = emptyList()
        try {
            val tn = (d.typeName ?: "").replace(Regex("[片剧]$"), "")
            val category = when {
                tn.contains("动漫") || tn.contains("漫") -> "anime" to mapAnimeSub(tn)
                tn.contains("综艺") -> "variety" to null
                tn.contains("剧") -> "tv" to mapTvSub(tn)
                else -> "movie" to mapMovieSub(tn)
            }

            fun clean(list: List<RelatedMediaItem>): List<RelatedMediaItem> =
                list.asSequence()
                    .filterNot { it.siteKey == d.siteKey && it.vodId == d.vodId }
                    .filterNot { it.vodName == d.vodName }
                    .distinctBy { "${it.siteKey}:${it.vodId}" }
                    .sortedWith(
                        compareByDescending<RelatedMediaItem> { relatedScore(d, it, category.second) }
                            .thenByDescending { it.rating ?: 0.0 }
                    )
                    .take(10)
                    .toList()

            suspend fun fillFromCategory(subType: String? = category.second, timeoutMs: Long = 5000L, pageSize: Int = 16): Boolean {
                val result = withTimeoutOrNull(timeoutMs) {
                    api.getCategory(
                        com.rannuan.tv.data.api.CategoryRequest(
                            category = category.first,
                            subType = subType,
                            page = 1,
                            pageSize = pageSize
                        )
                    )
                }?.list.orEmpty()
                val batch = clean(result)
                if (batch.isEmpty()) return false
                relatedVideos = batch
                return true
            }

            if (!fillFromCategory()) {
                fillFromCategory(subType = null, timeoutMs = 3200L, pageSize = 20)
            }

            if (relatedVideos.isEmpty()) {
                val searchKeywords = buildList {
                    addAll(splitPeople(d.vodActor).take(2))
                    d.vodDirector?.let { addAll(splitPeople(it).take(1)) }
                    d.vodName.takeIf { it.isNotBlank() }?.let { add(it) }
                }.distinct().filter { it.length >= 2 }.take(4)

                for (kw in searchKeywords) {
                    if (relatedVideos.isNotEmpty()) break
                    val searched = clean(withTimeoutOrNull(3000) { api.search(kw) }?.list.orEmpty())
                    if (searched.isNotEmpty()) relatedVideos = searched
                }
            }

            if (relatedVideos.isEmpty()) {
                val titleSeed = d.vodName.take(6).takeIf { it.isNotBlank() } ?: d.vodActor.orEmpty().take(6)
                if (!titleSeed.isNullOrBlank()) {
                    relatedVideos = clean(withTimeoutOrNull(2500) { api.search(titleSeed) }?.list.orEmpty())
                }
            }
        } catch (_: Exception) {
            relatedVideos = emptyList()
        }
        relatedLoading = false
    }

    // 切换线路/集数时：重置代理状态 + 错误信息，新线路从直连开始尝试
    LaunchedEffect(detail, currentSrc, currentEp) {
        if (detail == null) return@LaunchedEffect
        usingProxy = false
        forceProxyHls = false
        playbackError = null
        pendingResumePos = if (currentSrc == sourceIdx && currentEp == epIdx) pendingResumePos else 0L
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        tryPlayVideo(proxy = false)
    }

    LaunchedEffect(detail, currentSrc, currentEp, usingProxy, renderedFirstFrame, playbackStartedAt) {
        if (detail == null || usingProxy || renderedFirstFrame || playbackStartedAt <= 0L) return@LaunchedEffect
        delay(3500)
        val stillSameAttempt = !usingProxy && !renderedFirstFrame && playbackStartedAt > 0L
        val noProgress = exoPlayer.currentPosition <= 500L
        val readyButSilent = exoPlayer.playbackState == Player.STATE_READY && !exoPlayer.isPlaying
        if (stillSameAttempt && (readyButSilent || noProgress)) {
            Log.d("PlayerScreen", "直连已就绪但未渲染首帧/无进度，自动切代理兜底")
            usingProxy = true
            tryPlayVideo(proxy = true, fromError = true)
        }
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
        if (isPlaying) {
            exoPlayer.pause()
        } else {
            exoPlayer.playWhenReady = true
            exoPlayer.play()
        }
    }

    fun startLongPressSpeed(xPercent: Float) {
        if (longPressSpeedSide != null) return
        val side = if (xPercent < 0.5f) SeekSide.LEFT else SeekSide.RIGHT
        longPressWasPlaying = exoPlayer.isPlaying
        longPressSpeedSide = side
        showControls = false

        if (side == SeekSide.RIGHT) {
            exoPlayer.setPlaybackSpeed(2f)
            exoPlayer.playWhenReady = true
            exoPlayer.play()
        } else {
            // Media3 不支持负倍速，用高频 seek 模拟连续 2 倍速倒放。
            exoPlayer.pause()
            longPressRewindJob?.cancel()
            longPressRewindJob = scope.launch {
                while (true) {
                    val target = (exoPlayer.currentPosition - 200L).coerceAtLeast(0L)
                    exoPlayer.seekTo(target)
                    currentPosition = target
                    delay(100L)
                }
            }
        }
    }

    fun stopLongPressSpeed() {
        if (longPressSpeedSide == null) return
        longPressRewindJob?.cancel()
        longPressRewindJob = null
        longPressSpeedSide = null
        exoPlayer.setPlaybackSpeed(selectedSpeed)
        if (longPressWasPlaying) {
            exoPlayer.playWhenReady = true
            exoPlayer.play()
        } else {
            exoPlayer.pause()
        }
    }

    // 手势层：单击显隐；双击播放/暂停；长按左右区域持续快退/快进。
    val tapDetector = Modifier.pointerInput(isLocked, selectedSpeed) {
        if (isLocked) return@pointerInput
        detectTapGestures(
            onPress = {
                try {
                    tryAwaitRelease()
                } finally {
                    stopLongPressSpeed()
                }
            },
            onTap = { showControls = !showControls },
            onDoubleTap = { offset ->
                val w = size.width.toFloat().coerceAtLeast(1f)
                handleDoubleTap(offset.x / w)
            },
            onLongPress = { offset ->
                val w = size.width.toFloat().coerceAtLeast(1f)
                startLongPressSpeed(offset.x / w)
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
                                        modifier = Modifier.clickable { showSourceSheet = true }
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
                                        forceProxyHls = false
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
                    if (showBufferingHint) {
                        CircularProgressIndicator(
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.align(Alignment.Center).size(44.dp), strokeWidth = 3.dp
                        )
                    }
                    if (showBufferingHint) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 12.dp, bottom = if (isFullscreen) 76.dp else 62.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                Text("正在缓冲", color = Color.White, fontSize = 11.sp)
                                Text("已预载 ${formatBufferedAhead(bufferedAheadMs)}", color = Zinc300, fontSize = 10.sp)
                                Text("网速 $transferRateText", color = Brand400, fontSize = 11.sp)
                                if (transferBytes > 0) {
                                    Text("已加载 ${formatBytes(transferBytes)}", color = Zinc400, fontSize = 10.sp)
                                }
                            }
                        }
                    }

                    autoSwitchNotice?.let { notice ->
                        Surface(
                            color = Color.Black.copy(alpha = 0.72f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.align(Alignment.TopCenter).padding(top = 68.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.SwapHoriz, null, tint = Brand400, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(7.dp))
                                Text(notice, color = Color.White, fontSize = 12.sp)
                            }
                        }
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
                                    var baselineSeekPosition = 0L
                                    var volMax = 15
                                    var localGestureMode: GestureMode? = null

                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            localGestureMode = null
                                            startX = offset.x
                                            startY = offset.y
                                            baselineSeekPosition = exoPlayer.currentPosition.coerceAtLeast(0L)
                                            seekPreviewMs = baselineSeekPosition
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
                                            if (localGestureMode == GestureMode.SEEK && seekDragging) {
                                                exoPlayer.seekTo(seekPreviewMs)
                                                currentPosition = seekPreviewMs
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
                                        onDrag = { change, _ ->
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
                                                    localGestureMode = if (dx > dy * 1.5f) {
                                                        seekDragging = true
                                                        GestureMode.SEEK
                                                    } else {
                                                        if (leftHalf) GestureMode.BRIGHTNESS else GestureMode.VOLUME
                                                    }
                                                }
                                            }

                                            when (localGestureMode) {
                                                GestureMode.SEEK -> {
                                                    val knownDuration = exoPlayer.duration
                                                        .takeIf { it > 0L }
                                                        ?: duration.takeIf { it > 0L }
                                                    if (knownDuration != null) {
                                                        val deltaX = change.position.x - startX
                                                        val deltaMs = (deltaX / w * knownDuration).toLong()
                                                        seekPreviewMs = (baselineSeekPosition + deltaMs)
                                                            .coerceIn(0L, knownDuration)
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
                        longPressSpeedSide?.let { side ->
                            LongPressSpeedOverlay(
                                side = side,
                                modifier = Modifier.fillMaxSize()
                            )
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
                                    val episodeLabel = activeSource?.episodes?.getOrNull(currentEp)?.title.orEmpty()
                                    val sourceLabel = activeSource?.name.orEmpty()
                                    Text(
                                        buildString {
                                            append(d.vodName)
                                            if (episodeLabel.isNotBlank()) append("  $episodeLabel")
                                            if (sourceLabel.isNotBlank()) append("  [$sourceLabel]")
                                        },
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                                    )
                                    // 投屏
                                    IconButton(
                                        onClick = { openCastPanel() },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            CastIcon,
                                            null,
                                            tint = if (castSession != null) Brand400 else iconTint,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            showSourceSheet = false
                                            showSettings = true
                                        },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(Icons.Outlined.MoreHoriz, "更多设置", tint = iconTint, modifier = Modifier.size(24.dp))
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
                        if (isFullscreen && showControls && !isLocked) {
                            IconButton(
                                onClick = { isLocked = true },
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(start = 10.dp)
                                    .size(42.dp)
                            ) {
                                Icon(Icons.Outlined.Lock, "锁定控制", tint = iconTintSub, modifier = Modifier.size(22.dp))
                            }
                        }

                        // ═══ 底部控制栏：进度与操作分层，横屏显示快捷功能标签 ═══
                        OverlayAnimatedVisibility(
                            visible = showControls && !isLocked,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(bottomControlGradient())
                                    .padding(horizontal = if (isFullscreen) 22.dp else 12.dp)
                                    .padding(top = 8.dp, bottom = if (isFullscreen) 10.dp else 4.dp)
                            ) {
                                if (isFullscreen) {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            if (seekDragging) formatTime(seekPreviewMs) else formatTime(currentPosition),
                                            color = Color.White.copy(alpha = 0.86f),
                                            fontSize = 11.sp,
                                            modifier = Modifier.width(44.dp)
                                        )
                                        if (duration > 0) {
                                            Box(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                                                PlayerSeekBar(
                                                    position = if (seekDragging) seekPreviewMs else currentPosition,
                                                    duration = duration,
                                                    buffered = buffered,
                                                    onSeek = { ms -> seekDragging = true; seekPreviewMs = ms },
                                                    onSeekFinished = { ms ->
                                                        exoPlayer.seekTo(ms)
                                                        currentPosition = ms
                                                        seekDragging = false
                                                    }
                                                )
                                            }
                                        } else {
                                            Spacer(Modifier.weight(1f))
                                        }
                                        Text(
                                            formatTime(duration),
                                            color = Color.White.copy(alpha = 0.48f),
                                            fontSize = 11.sp,
                                            modifier = Modifier.width(44.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                                        )
                                    }
                                }
                                Row(
                                    Modifier.fillMaxWidth().height(46.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (isPlaying) {
                                                exoPlayer.pause()
                                            } else {
                                                exoPlayer.playWhenReady = true
                                                exoPlayer.play()
                                            }
                                        },
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Icon(
                                            if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                            null, tint = iconTint, modifier = Modifier.size(26.dp)
                                        )
                                    }
                                    if (!isFullscreen) {
                                        Text(
                                            if (seekDragging) formatTime(seekPreviewMs) else formatTime(currentPosition),
                                            color = Color.White.copy(alpha = 0.76f),
                                            fontSize = 10.sp,
                                            modifier = Modifier.width(40.dp)
                                        )
                                        if (duration > 0) {
                                            Box(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                                                PlayerSeekBar(
                                                    position = if (seekDragging) seekPreviewMs else currentPosition,
                                                    duration = duration,
                                                    buffered = buffered,
                                                    onSeek = { ms -> seekDragging = true; seekPreviewMs = ms },
                                                    onSeekFinished = { ms ->
                                                        exoPlayer.seekTo(ms)
                                                        currentPosition = ms
                                                        seekDragging = false
                                                    }
                                                )
                                            }
                                        } else {
                                            Spacer(Modifier.weight(1f))
                                        }
                                        Text(
                                            formatTime(duration),
                                            color = Color.White.copy(alpha = 0.48f),
                                            fontSize = 10.sp,
                                            modifier = Modifier.width(40.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                                        )
                                    }
                                    val hasNextEp = isFullscreen && (activeSource?.let { currentEp + 1 < it.episodes.size } ?: false)
                                    if (hasNextEp) {
                                        IconButton(
                                            onClick = { switchToEpisode(currentEp + 1) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                Icons.Outlined.SkipNext, null,
                                                tint = iconTint, modifier = Modifier.size(21.dp)
                                            )
                                        }
                                    }
                                    if (isFullscreen) Spacer(Modifier.weight(1f)) else Spacer(Modifier.width(2.dp))

                                    if (isFullscreen) {
                                        Box {
                                            PlayerTextAction(
                                                label = speedLabel(selectedSpeed),
                                                active = selectedSpeed != 1f,
                                                onClick = { showSpeedMenu = !showSpeedMenu }
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
                                        PlayerTextAction(
                                            label = "播放源",
                                            active = sourceOptions.size > 1,
                                            onClick = {
                                                showSettings = false
                                                showSourceSheet = true
                                            }
                                        )
                                        PlayerTextAction(
                                            label = "选集",
                                            onClick = { showEpisodeSheet = true },
                                            active = activeSource?.episodes?.size?.let { it > 1 } == true
                                        )
                                    }
                                    IconButton(
                                        onClick = { isFullscreen = !isFullscreen },
                                        modifier = Modifier.size(38.dp)
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
                                     resizeMode = resizeMode,
                                     playMode = playMode,
                                     mirrorMode = mirrorMode,
                                     autoSwitchSource = autoSwitchSource,
                                     usingProxy = usingProxy,
                                     transferRateText = transferRateText,
                                     onSpeedChange = { selectedSpeed = it },
                                     onResizeChange = { resizeMode = it },
                                     onPlayModeChange = { playMode = it },
                                     onMirrorChange = { mirrorMode = it },
                                     onAutoSwitchChange = { autoSwitchSource = it },
                                     onClose = { showSettings = false }
                                 )
                             }
                          }
                      }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = showSourceSheet && isFullscreen,
                        enter = slideInHorizontally(initialOffsetX = { it }),
                        exit = slideOutHorizontally(targetOffsetX = { it }),
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Box(Modifier.fillMaxSize()) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.28f))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { showSourceSheet = false }
                            )
                            Box(
                                Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(0.38f)
                                    .align(Alignment.CenterEnd)
                                    .background(Zinc950.copy(alpha = 0.97f))
                            ) {
                                SourceSheetContent(
                                    options = sourceOptions,
                                    currentDetail = detail,
                                    currentSourceIndex = currentSrc,
                                    currentEpisode = currentEp,
                                    usingProxy = usingProxy,
                                    onSelect = { option ->
                                        switchToRoute(option.detail, option.sourceIndex, preserveEpisode = true)
                                        showSourceSheet = false
                                    }
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

                    item {
                        Spacer(Modifier.height(16.dp))
                        PortraitPlayerActions(
                            favorited = favorited,
                            onSource = { showSourceSheet = true },
                            onEpisode = { showEpisodeSheet = true },
                            onCast = { openCastPanel() },
                            onFavorite = { favorited = FavoritesStore.toggle(context, d) },
                            onShare = { shareCurrentVideo() }
                        )
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
                                                val encodedTitle = java.net.URLEncoder.encode(video.vodName, "UTF-8")
                                                onNavigate("detail/${video.siteKey}/${video.vodId}?name=$encodedTitle")
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
                        activeSource = activeSource,
                        currentEp = currentEp,
                        onEpisodeSelect = { idx -> switchToEpisode(idx); showEpisodeSheet = false }
                    )
                }
            }
        }

        if (showSourceSheet && !isFullscreen) {
            ModalBottomSheet(
                onDismissRequest = { showSourceSheet = false },
                sheetState = sourceSheetState,
                containerColor = Zinc900,
                dragHandle = {
                    Box(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp), contentAlignment = Alignment.Center) {
                        Box(Modifier.width(34.dp).height(3.dp).background(Zinc700, RoundedCornerShape(2.dp)))
                    }
                }
            ) {
                SourceSheetContent(
                    options = sourceOptions,
                    currentDetail = detail,
                    currentSourceIndex = currentSrc,
                    currentEpisode = currentEp,
                    usingProxy = usingProxy,
                    onSelect = { option ->
                        switchToRoute(option.detail, option.sourceIndex, preserveEpisode = true)
                        showSourceSheet = false
                    }
                )
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
 * 设置项按使用场景分组：播放行为和画面。比连续 chip 更容易扫读，
 * 也能在横屏时保持足够触摸面积。
 */
@Composable
private fun PlayerSettingsPanel(
    selectedSpeed: Float,
    resizeMode: Int,
    playMode: Int,
    mirrorMode: Int,
    autoSwitchSource: Boolean,
    usingProxy: Boolean,
    transferRateText: String,
    onSpeedChange: (Float) -> Unit,
    onResizeChange: (Int) -> Unit,
    onPlayModeChange: (Int) -> Unit,
    onMirrorChange: (Int) -> Unit,
    onAutoSwitchChange: (Boolean) -> Unit,
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
                    Text("播放器设置", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text("播放偏好", color = Zinc500, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(7.dp).background(if (usingProxy) Brand400 else Color(0xFF5ED59A), CircleShape)
                )
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (usingProxy) "代理播放" else "源站直连", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Text("实时网速 $transferRateText", color = Zinc500, fontSize = 10.sp)
                }
            }
        }

        item {
            SettingGroup(title = "线路") {
                PlayerSettingToggleRow(
                    title = "失败自动换源",
                    checked = autoSwitchSource,
                    onCheckedChange = onAutoSwitchChange
                )
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

    }
}

@Composable
private fun SettingGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        content()
        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.06f)))
    }
}

@Composable
private fun PlayerSettingToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Brand400,
                uncheckedThumbColor = Zinc400,
                uncheckedTrackColor = Zinc700
            )
        )
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
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            options.forEach { (value, label) ->
                val isSelected = value == selected
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelect(value) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        label,
                        color = if (isSelected) Color.White else Zinc500,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .width(18.dp)
                            .height(2.dp)
                            .background(if (isSelected) Brand400 else Color.Transparent, CircleShape)
                    )
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

@Composable
private fun SourceSheetContent(
    options: List<PlayerSourceOption>,
    currentDetail: MediaDetail?,
    currentSourceIndex: Int,
    currentEpisode: Int,
    usingProxy: Boolean,
    onSelect: (PlayerSourceOption) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("播放线路", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "${options.size} 条可用线路 · 当前${if (usingProxy) "代理" else "直连"}",
                    color = Zinc500,
                    fontSize = 10.sp
                )
            }
            Text("第 ${currentEpisode + 1} 集", color = Brand400, fontSize = 11.sp)
        }
        Spacer(Modifier.height(10.dp))

        if (options.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(96.dp), contentAlignment = Alignment.Center) {
                Text("暂无其他线路", color = Zinc500, fontSize = 12.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                options.forEach { option ->
                    item(key = "${option.detail.siteKey}:${option.detail.vodId}:${option.sourceIndex}") {
                        val selected = option.detail.siteKey == currentDetail?.siteKey &&
                            option.detail.vodId == currentDetail.vodId &&
                            option.sourceIndex == currentSourceIndex
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(option) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier
                                        .size(8.dp)
                                        .background(if (selected) Brand400 else Zinc600, CircleShape)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        option.displayName,
                                        color = if (selected) Color.White else Zinc300,
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "${option.siteName} · ${option.episodeCount} 集",
                                        color = Zinc500,
                                        fontSize = 10.sp
                                    )
                                }
                                if (selected) {
                                    Text("播放中", color = Brand400, fontSize = 10.sp)
                                }
                            }
                            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.055f)))
                        }
                    }
                }
            }
        }
    }
}

/** 选集底部 Sheet 内容 — 移动端优化：紧凑行列、去标题、高触达 */
@Composable
private fun EpisodeSheetContent(
    activeSource: PlaySource,
    currentEp: Int,
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
        val thumbOffsetPx = ((barWidthPx * p) - thumbSizePx / 2f).roundToInt()
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

@Composable
private fun PortraitPlayerActions(
    favorited: Boolean,
    onSource: () -> Unit,
    onEpisode: () -> Unit,
    onCast: () -> Unit,
    onFavorite: () -> Unit,
    onShare: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        PortraitPlayerAction(Icons.Outlined.SwapHoriz, "换源", onClick = onSource)
        PortraitPlayerAction(Icons.AutoMirrored.Outlined.List, "选集", onClick = onEpisode)
        PortraitPlayerAction(CastIcon, "投屏", onClick = onCast)
        PortraitPlayerAction(
            if (favorited) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            if (favorited) "已收藏" else "收藏",
            active = favorited,
            onClick = onFavorite
        )
        PortraitPlayerAction(Icons.Outlined.Share, "分享", onClick = onShare)
    }
}

@Composable
private fun PortraitPlayerAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(58.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (active) Brand400 else Color.White.copy(alpha = 0.88f),
            modifier = Modifier.size(27.dp)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            color = if (active) Brand400 else Zinc500,
            fontSize = 11.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun PlayerTextAction(
    label: String,
    active: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(36.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (active) Brand400 else Color.White.copy(alpha = 0.86f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
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

/** 长按倍速反馈固定居中，避免在手指下方或屏幕边缘难以辨认。 */
@Composable
private fun LongPressSpeedOverlay(side: SeekSide, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.68f))
                .padding(horizontal = 22.dp, vertical = 14.dp),
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
            Spacer(Modifier.height(8.dp))
            Text(
                if (side == SeekSide.LEFT) "2.0×  快退" else "2.0×  快进",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
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

private fun formatBytes(bytes: Long): String {
    val value = bytes.toDouble().coerceAtLeast(0.0)
    return when {
        value >= 1024.0 * 1024.0 * 1024.0 -> String.format("%.2f GB", value / 1024.0 / 1024.0 / 1024.0)
        value >= 1024.0 * 1024.0 -> String.format("%.1f MB", value / 1024.0 / 1024.0)
        value >= 1024.0 -> String.format("%.0f KB", value / 1024.0)
        else -> "${value.toLong()} B"
    }
}

private fun formatBufferedAhead(ms: Long): String {
    val seconds = (ms / 1000).coerceAtLeast(0)
    return if (seconds >= 60) {
        "${seconds / 60}分${seconds % 60}秒"
    } else {
        "${seconds}秒"
    }
}

private fun formatNetworkSpeed(bitrateEstimate: Long): String {
    val bytesPerSecond = (bitrateEstimate / 8.0).coerceAtLeast(0.0)
    return when {
        bytesPerSecond >= 1024.0 * 1024.0 -> String.format("%.1f MB/s", bytesPerSecond / 1024.0 / 1024.0)
        bytesPerSecond >= 1024.0 -> String.format("%.1f KB/s", bytesPerSecond / 1024.0)
        else -> "${bytesPerSecond.toInt()} B/s"
    }
}

private fun speedLabel(speed: Float): String =
    if (speed == 1f) "倍速" else "${speed}×"

private fun isM3u8Url(url: String): Boolean =
    url.substringBefore('?').substringBefore('#').endsWith(".m3u8", ignoreCase = true) ||
        url.contains(".m3u8?", ignoreCase = true) ||
        url.contains(".m3u8#", ignoreCase = true)

private fun isTsUrl(url: String): Boolean {
    val path = url.substringBefore('?').substringBefore('#')
    return path.endsWith(".ts", ignoreCase = true) || url.contains(".ts?", ignoreCase = true)
}

private fun hasDirectVideoFileExtension(url: String): Boolean {
    val path = url.substringBefore('?').substringBefore('#')
    return listOf(".mp4", ".webm", ".flv", ".ts", ".mkv", ".avi", ".mov", ".wmv", ".m4v", ".ogg")
        .any { path.endsWith(it, ignoreCase = true) }
}

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
        }.filter { isM3u8Url(it.url) }
        val rawName = fromParts.getOrElse(i) { "线路${i + 1}" }.trim()
        PlaySource(formatSourceName(rawName, i, d.siteKey), eps)
    }.filter { it.episodes.isNotEmpty() }
}

private data class PlayerSourceOption(
    val detail: MediaDetail,
    val sourceIndex: Int,
    val displayName: String,
    val siteName: String,
    val episodeCount: Int
)

private fun buildPlayerSourceOptions(
    sourceDetails: List<MediaDetail>,
    currentDetail: MediaDetail?
): List<PlayerSourceOption> {
    val details = (listOfNotNull(currentDetail) + sourceDetails)
        .distinctBy { "${it.siteKey}:${it.vodId}" }
    return details.flatMap { detail ->
        val siteName = detail.siteName.takeIf { it.isNotBlank() } ?: detail.siteKey.ifBlank { "视频源" }
        parseSources(detail).mapIndexed { index, source ->
            PlayerSourceOption(
                detail = detail,
                sourceIndex = index,
                displayName = source.name.ifBlank { "$siteName 线路${index + 1}" },
                siteName = siteName,
                episodeCount = source.episodes.size
            )
        }
    }
}

private data class PlaySource(val name: String, val episodes: List<PlayEpisode>)
private data class PlayEpisode(val title: String, val url: String)
