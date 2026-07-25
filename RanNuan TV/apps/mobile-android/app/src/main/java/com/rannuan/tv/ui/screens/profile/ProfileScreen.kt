package com.rannuan.tv.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.rannuan.tv.R
import com.rannuan.tv.data.api.RanNuanApi
import com.rannuan.tv.data.model.MediaDetail
import com.rannuan.tv.ui.theme.Brand400
import com.rannuan.tv.ui.theme.Error500
import com.rannuan.tv.ui.theme.Zinc400
import com.rannuan.tv.ui.theme.Zinc500
import com.rannuan.tv.ui.theme.Zinc600
import com.rannuan.tv.ui.theme.Zinc800
import com.rannuan.tv.ui.theme.Zinc900
import com.rannuan.tv.ui.theme.component.GlassCard
import com.rannuan.tv.ui.util.FavoritesStore
import com.rannuan.tv.ui.util.ImageProxy
import com.rannuan.tv.ui.util.WatchingHistoryStore

// ── 收藏详情内存缓存（key → details 的映射，key 为收藏 id 集的 hash）──
private object FavoriteCache {
    private var cachedKeysHash = 0
    private var cachedDetails: List<MediaDetail> = emptyList()

    fun get(keys: Set<String>): List<MediaDetail>? {
        val hash = keys.hashCode()
        return if (hash == cachedKeysHash) cachedDetails else null
    }

    fun put(keys: Set<String>, details: List<MediaDetail>) {
        cachedKeysHash = keys.hashCode()
        cachedDetails = details
    }

    fun invalidate() {
        cachedKeysHash = 0
        cachedDetails = emptyList()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember { RanNuanApi.create() }

    // ── 观看历史 ──
    var history by remember { mutableStateOf(WatchingHistoryStore.getHistory(context)) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    // ── 收藏 ──
    var favoriteKeys by remember { mutableStateOf(FavoritesStore.getFavoriteKeys(context)) }
    var favoriteDetails by remember {
        mutableStateOf(FavoriteCache.get(favoriteKeys) ?: FavoritesStore.getFavoriteSnapshots(context))
    }
    var favLoading by remember { mutableStateOf(false) }

    // 每次进入刷新（有缓存则跳过 API）
    LaunchedEffect(Unit) {
        history = WatchingHistoryStore.getHistory(context)
        val keys = FavoritesStore.getFavoriteKeys(context)
        favoriteKeys = keys

        // 先检查缓存
        val cached = FavoriteCache.get(keys)
        if (cached != null) {
            favoriteDetails = cached
            favLoading = false
        } else if (keys.isNotEmpty()) {
            val snapshots = FavoritesStore.getFavoriteSnapshots(context)
            favoriteDetails = snapshots.ifEmpty { keys.mapNotNull { it.toPlaceholderDetail() } }
            favLoading = false
            try {
                val keysParam = keys.joinToString(",")
                val resp = api.getMultiDetail(wd = "", keys = keysParam)
                if (resp.list.isNotEmpty()) {
                    favoriteDetails = resp.list
                    resp.list.forEach { FavoritesStore.saveSnapshot(context, it) }
                    FavoriteCache.put(keys, resp.list)
                } else if (snapshots.isNotEmpty()) {
                    FavoriteCache.put(keys, snapshots)
                }
            } catch (_: Exception) { }
            favLoading = false
        } else {
            favoriteDetails = emptyList()
            favLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // ── 头部 ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.thumb_logo),
                    contentDescription = "RanNuan TV",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(48.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text("我的", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(
                    "收藏 ${favoriteKeys.size} · 历史 ${history.size}",
                    color = Zinc400, fontSize = 13.sp
                )
            }
        }

        // ── Tab 切换 ──
        var selectedTab by remember { mutableIntStateOf(0) }
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = Brand400,
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Brand400
                    )
                }
            },
            divider = { Spacer(Modifier.height(0.dp)) }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.FavoriteBorder, null, tint = if (selectedTab == 0) Brand400 else Zinc500, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("收藏", fontSize = 14.sp)
                    }
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Schedule, null, tint = if (selectedTab == 1) Brand400 else Zinc500, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("历史", fontSize = 14.sp)
                    }
                }
            )
        }

        when (selectedTab) {
            0 -> FavoritesTab(favLoading, favoriteDetails, favoriteKeys, onNavigate, context, scope, api)
            1 -> HistoryTab(history, showClearHistoryDialog, onNavigate, context, {
                showClearHistoryDialog = true
            }, {
                showClearHistoryDialog = false
            }, {
                WatchingHistoryStore.clearHistory(context)
                history = emptyList()
                showClearHistoryDialog = false
            })
        }
    }
}

private fun String.toPlaceholderDetail(): MediaDetail? {
    val parts = split(":", limit = 2)
    if (parts.size != 2 || parts[0].isBlank() || parts[1].isBlank()) return null
    return MediaDetail(
        vodId = parts[1],
        vodName = "收藏影片",
        siteKey = parts[0],
        siteName = parts[0]
    )
}

@Composable
private fun FavoritesTab(
    loading: Boolean,
    details: List<MediaDetail>,
    keys: Set<String>,
    onNavigate: (String) -> Unit,
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope,
    api: RanNuanApi
) {
    var localDetails by remember(keys) { mutableStateOf(details) }
    var localLoading by remember(keys) { mutableStateOf(loading) }

    // 同步外部刷新
    LaunchedEffect(keys, details) {
        localDetails = details
        localLoading = loading
    }

    if (localLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Brand400, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                Spacer(Modifier.height(12.dp))
                Text("加载收藏中...", color = Zinc400, fontSize = 13.sp)
            }
        }
    } else if (localDetails.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(72.dp).background(Zinc800, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.FavoriteBorder, null, tint = Zinc500, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text("还没有收藏", color = Zinc400, fontSize = 15.sp)
                Text("去探索更多内容吧", color = Zinc600, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(localDetails, key = { "${it.siteKey}:${it.vodId}" }) { detail ->
                FavoriteCard(
                    detail = detail,
                    onClick = {
                        onNavigate("player/${detail.siteKey}/${detail.vodId}?src=0&ep=0")
                    },
                    onRemove = {
                        FavoritesStore.toggle(context, detail.siteKey, detail.vodId)
                        // 本地移除 + 失效缓存（下次进入会重新拉取）
                        localDetails = localDetails.filter {
                            !(it.siteKey == detail.siteKey && it.vodId == detail.vodId)
                        }
                        FavoriteCache.invalidate()
                    }
                )
            }
        }
    }
}

@Composable
private fun FavoriteCard(
    detail: MediaDetail,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f)) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(10.dp),
            color = Zinc800,
            modifier = Modifier.fillMaxSize()
        ) {
            Box {
                AsyncImage(
                    model = ImageProxy.proxy(detail.vodPic),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // 底部渐变蒙层 + 标题
                Box(
                    Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .height(48.dp)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                            )
                        )
                )
                Text(
                    detail.vodName,
                    color = Color.White,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                        .fillMaxWidth()
                )
            }
        }
        // 移除按钮
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(28.dp)
                .padding(2.dp)
                .background(Color.Black.copy(alpha = 0.55f), CircleShape)
        ) {
            Icon(
                Icons.Outlined.Close, null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun HistoryTab(
    history: List<WatchingHistoryStore.HistoryItem>,
    showClearDialog: Boolean,
    onNavigate: (String) -> Unit,
    context: android.content.Context,
    onShowClear: () -> Unit,
    onDismissClear: () -> Unit,
    onConfirmClear: () -> Unit
) {
    if (history.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(72.dp).background(Zinc800, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Schedule, null, tint = Zinc500, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text("暂无观看记录", color = Zinc400, fontSize = 15.sp)
                Text("开始观看视频吧", color = Zinc600, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "共 ${history.size} 条记录",
                        color = Zinc400, fontSize = 12.sp
                    )
                    TextButton(onClick = onShowClear) {
                        Text("清空", color = Zinc500, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            items(history, key = { "${it.siteKey}:${it.id}" }) { item ->
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clickable {
                            val sourceIndex = item.sourceIndex.coerceAtLeast(0)
                            val episodeIndex = if (item.episodeIndex >= 0) {
                                item.episodeIndex
                            } else {
                                inferLegacyEpisodeIndex(item.episode)
                            }
                            val posParam = if (item.position > 0) "&pos=${item.position}" else ""
                            onNavigate(
                                "player/${item.siteKey}/${item.id}?src=$sourceIndex&ep=$episodeIndex$posParam"
                            )
                        },
                    cornerRadius = 12.dp
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)).background(Zinc800)) {
                            AsyncImage(
                                model = ImageProxy.proxy(item.cover),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                item.title,
                                color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (item.episode.isNotBlank()) {
                                    Text(
                                        item.episode,
                                        color = Zinc500, fontSize = 12.sp, maxLines = 1,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                }
                                if (item.position > 0) {
                                    Text(
                                        "续播 ${formatPosition(item.position)} ",
                                        color = Brand400, fontSize = 11.sp
                                    )
                                }
                                Text(
                                    formatTimestamp(item.timestamp),
                                    color = Zinc600, fontSize = 11.sp
                                )
                            }
                        }
                        Icon(Icons.Filled.ChevronRight, null, tint = Zinc600, modifier = Modifier.size(18.dp))
                    }
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    // 清空确认弹窗
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = onDismissClear,
            containerColor = Zinc900,
            title = { Text("清空观看历史", color = Color.White) },
            text = { Text("确定清空所有观看记录？", color = Zinc400) },
            confirmButton = {
                TextButton(onClick = onConfirmClear) {
                    Text("清空", color = Error500)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissClear) {
                    Text("取消", color = Zinc500)
                }
            }
        )
    }
}

private fun inferLegacyEpisodeIndex(label: String): Int {
    val match = Regex("(?:第\\s*)?(\\d+)\\s*集?").find(label) ?: return 0
    return (match.groupValues[1].toIntOrNull()?.minus(1) ?: 0).coerceAtLeast(0)
}

private fun formatTimestamp(ts: Long): String {
    val delta = System.currentTimeMillis() - ts
    return when {
        delta < 60_000 -> "刚刚"
        delta < 3600_000 -> "${delta / 60_000}分钟前"
        delta < 86400_000 -> "${delta / 3600_000}小时前"
        delta < 604800_000 -> "${delta / 86400_000}天前"
        else -> {
            val sdf = java.text.SimpleDateFormat("MM-dd", java.util.Locale.getDefault())
            sdf.format(java.util.Date(ts))
        }
    }
}

private fun formatPosition(ms: Long): String {
    val t = ms / 1000
    val h = t / 3600
    val m = (t % 3600) / 60
    val s = t % 60
    return if (h > 0) "${h}:${String.format("%02d:%02d", m, s)}"
    else "${m}:${String.format("%02d", s)}"
}
