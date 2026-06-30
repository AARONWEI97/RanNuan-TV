package com.rannuan.tv.ui.screens.detail

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import android.os.Build
import com.rannuan.tv.data.api.RanNuanApi
import com.rannuan.tv.data.model.MediaDetail
import com.rannuan.tv.ui.theme.*
import com.rannuan.tv.ui.util.FavoritesStore
import com.rannuan.tv.ui.util.ImageProxy
import com.rannuan.tv.ui.util.formatSourceName
import com.rannuan.tv.ui.util.stripHtml

private const val DETAIL_CACHE_MAX_ITEMS = 32
private const val DETAIL_CACHE_TTL_MS = 10 * 60 * 1000L

private object DetailMemoryCache {
    private data class Entry(val details: List<MediaDetail>, val savedAt: Long)

    private val cache = object : LinkedHashMap<String, Entry>(DETAIL_CACHE_MAX_ITEMS, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Entry>?): Boolean {
            return size > DETAIL_CACHE_MAX_ITEMS
        }
    }

    private fun key(wd: String, keys: String): String = "${wd.trim()}|${keys.trim()}"

    @Synchronized
    fun get(wd: String, keys: String): List<MediaDetail>? {
        val cacheKey = key(wd, keys)
        val entry = cache[cacheKey] ?: return null
        if (System.currentTimeMillis() - entry.savedAt > DETAIL_CACHE_TTL_MS) {
            cache.remove(cacheKey)
            return null
        }
        return entry.details
    }

    @Synchronized
    fun put(wd: String, keys: String, details: List<MediaDetail>) {
        if (details.isNotEmpty()) {
            cache[key(wd, keys)] = Entry(details, System.currentTimeMillis())
        }
    }
}

object DetailWarmCache {
    private const val MAX_ITEMS = 24
    private const val TTL_MS = 10 * 60 * 1000L

    private data class Entry(val details: List<MediaDetail>, val savedAt: Long)
    private val cache = object : LinkedHashMap<String, Entry>(MAX_ITEMS, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Entry>?): Boolean = size > MAX_ITEMS
    }

    private fun key(name: String, keys: String) = "${name.trim()}|${keys.trim()}"

    @Synchronized fun get(name: String, keys: String): List<MediaDetail>? {
        val e = cache[key(name, keys)] ?: return null
        if (System.currentTimeMillis() - e.savedAt > TTL_MS) {
            cache.remove(key(name, keys))
            return null
        }
        return e.details
    }

    @Synchronized fun put(name: String, keys: String, details: List<MediaDetail>) {
        if (details.isNotEmpty()) cache[key(name, keys)] = Entry(details, System.currentTimeMillis())
    }
}

suspend fun warmDetailCache(api: RanNuanApi, wd: String, keys: String) {
    val cacheKey = keys.trim()
    val query = wd.trim()
    if (cacheKey.isBlank() && query.isBlank()) return
    if (DetailMemoryCache.get(query, cacheKey) != null || DetailWarmCache.get(query, cacheKey) != null) return
    runCatching {
        val resp = api.getMultiDetail(wd = query, keys = cacheKey)
        val result = resp.list.toMutableList()
        if (result.isEmpty() && query.isNotBlank()) {
            val multi = api.getMultiDetail(wd = query, keys = "")
            result.addAll(multi.list)
        }
        if (result.isNotEmpty()) {
            DetailWarmCache.put(query, cacheKey, result)
            if (query.isNotBlank() && cacheKey.isNotBlank()) {
                DetailWarmCache.put(query, "", result)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    siteKey: String = "", id: String = "",
    name: String = "", keys: String = "",
    api: RanNuanApi,
    onBack: () -> Unit,
    onPlay: (siteKey: String, id: String, sourceIdx: Int, epIdx: Int, name: String, keys: String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    // 单源模式: siteKey+id → keys="siteKey:id"; 多源模式: name+keys → 直接用
    val realKeys = keys.takeIf { it.isNotBlank() }
        ?: (if (siteKey.isNotBlank() && id.isNotBlank()) "$siteKey:$id" else "")
    val wd = name

    var details by remember { mutableStateOf<List<MediaDetail>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var activeIdx by remember { mutableStateOf(0) }
    var selectedSourceIdx by remember { mutableStateOf(0) }
    var favorited by remember {
        mutableStateOf(FavoritesStore.isFavorited(context, siteKey, id))
    }
    val contentVisible = remember { androidx.compose.animation.core.MutableTransitionState(false) }

    // 加载数据
    LaunchedEffect(wd, realKeys) {
        val cached = DetailMemoryCache.get(wd, realKeys)
        if (cached != null) {
            details = cached
            loading = false
            error = null
            return@LaunchedEffect
        }

        val warmCandidates = listOfNotNull(
            DetailWarmCache.get(wd, realKeys),
            DetailWarmCache.get(wd, "")
        )
        warmCandidates.firstOrNull { it.isNotEmpty() }?.let { warmed ->
            details = warmed
            loading = false
            error = null
            return@LaunchedEffect
        }

        loading = true
        error = null
        try {
            // 1. 优先用 keys 精准拉取（快）
            val resp = api.getMultiDetail(wd = wd, keys = realKeys)
            val result = rankDetailResults(resp.list, wd, realKeys).toMutableList()

            details = result
            DetailMemoryCache.put(wd, realKeys, result)
            val nameForCache = wd.takeIf { it.isNotBlank() } ?: result.firstOrNull()?.vodName.orEmpty()
            if (nameForCache.isNotBlank()) {
                DetailWarmCache.put(nameForCache, realKeys, result)
                DetailWarmCache.put(nameForCache, "", result)
            }
            if (result.isEmpty()) error = "未找到影片详情"
        } catch (e: Exception) {
            error = e.message
        } finally {
            loading = false
        }
    }

    val current = details.getOrNull(activeIdx)
    val sources = remember(current) { current?.let { parseSources(it) } ?: emptyList() }
    val activeSource = sources.getOrNull(selectedSourceIdx)

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Brand400)
        }
    } else if (error != null || current == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                Icon(Icons.Filled.Warning, null, tint = Zinc500, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(12.dp))
                Text(error ?: "未找到详情", color = Zinc400, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onBack) { Text("返回", color = Brand400) }
            }
        }
    } else {
        LaunchedEffect(current.vodId) {
            contentVisible.targetState = true
        }
        AnimatedVisibility(
            visibleState = contentVisible,
            enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow), initialOffsetY = { it / 8 }) +
                scaleIn(initialScale = 0.985f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
            exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                slideOutVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow), targetOffsetY = { it / 8 }) +
                scaleOut(targetScale = 0.985f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
        ) {
            val scrollState = rememberScrollState()
            Column(modifier = Modifier.verticalScroll(scrollState)) {
                // ── 封面 ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Zinc900)
                ) {
                    var imgError by remember { mutableStateOf(false) }
                    if (!imgError) {
                        // 底部毛玻璃背景层：放大 + 低透明度 + 模糊（API≥31）
                        AsyncImage(
                            model = ImageProxy.proxy(current!!.vodPic),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .scale(1.2f)
                                .then(
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                                        Modifier.blur(25.dp)
                                    else Modifier
                                )
                                .alpha(0.35f)
                                .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                        )
                        // 半透明渐变遮罩
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(Brush.verticalGradient(
                                    listOf(Color.Black.copy(alpha = 0.3f), Color.Transparent, Zinc950.copy(alpha = 0.5f))
                                ))
                        )
                        // 上层清晰海报（居中，小一点）
                        AsyncImage(
                            model = ImageProxy.proxy(current!!.vodPic),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 48.dp, vertical = 16.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            onError = { imgError = true }
                        )
                    }
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, Zinc950)))
                    )
                    // 返回按钮
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.align(Alignment.TopStart).padding(12.dp)
                            .size(36.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }

                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Spacer(Modifier.height(14.dp))

                    // ── 标题 + 元信息 ──
                    Text(current!!.vodName, color = Color.White, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        current.vodYear?.takeIf { it.isNotBlank() }?.let {
                            Icon(Icons.Filled.CalendarMonth, null, tint = Zinc500, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(2.dp))
                            Text(it, color = Zinc400, fontSize = 13.sp, maxLines = 1)
                        }
                        current.vodArea?.takeIf { it.isNotBlank() }?.let {
                            Icon(Icons.Filled.Public, null, tint = Zinc500, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(2.dp))
                            Text(it, color = Zinc400, fontSize = 13.sp, maxLines = 1)
                        }
                        current.vodRemarks?.takeIf { it.isNotBlank() }?.let {
                            Surface(color = Brand400.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                                Text(it, color = Brand400, fontSize = 11.sp, maxLines = 1, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }

                    // ── 操作按钮 ──
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { onPlay(current.siteKey, current.vodId, selectedSourceIdx, 0, current.vodName, realKeys) },
                            colors = ButtonDefaults.buttonColors(containerColor = Brand500),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.PlayArrow, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("立即播放", style = MaterialTheme.typography.labelLarge)
                        }
                        OutlinedButton(
                            onClick = {
                                favorited = FavoritesStore.toggle(context, current.siteKey, current.vodId)
                            },
                            border = BorderStroke(1.dp, if (favorited) Brand400 else Zinc600),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                if (favorited) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                null, tint = if (favorited) Brand400 else Zinc400, modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(if (favorited) "已收藏" else "收藏", color = if (favorited) Brand400 else Zinc400)
                        }
                    }

                    // ── 多源站点切换 ──
                    if (details.size > 1) {
                        Spacer(Modifier.height(14.dp))
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            details.forEachIndexed { idx, d ->
                                FilterChip(
                                    selected = idx == activeIdx,
                                    onClick = { activeIdx = idx; selectedSourceIdx = 0 },
                                    label = {
                                        Text(
                                            d.siteName.takeIf { it.isNotBlank() } ?: d.siteKey,
                                            fontSize = 12.sp,
                                            color = if (idx == activeIdx) Brand400 else Zinc400
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Brand400.copy(alpha = 0.15f)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }

                    // ── 演员 ──
                    current.vodActor?.takeIf { it.isNotBlank() }?.let { actorStr ->
                        Spacer(Modifier.height(12.dp))
                        val actors = actorStr.split(Regex("[,，、\\s]+")).filter { it.isNotBlank() }.take(8)
                        Text("演员", color = Color.White, style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            actors.forEach { actor ->
                                Surface(
                                    onClick = { /* TODO: 跳搜索演员名 */ },
                                    color = White04,
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text(actor, color = Zinc300, fontSize = 12.sp, maxLines = 1, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                                }
                            }
                        }
                    }

                    // ── 简介 ──
                    val content = stripHtml(current.vodContent)
                    if (content.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        var expanded by remember { mutableStateOf(false) }
                        Text(
                            content,
                            color = Zinc400,
                            fontSize = 13.sp,
                            maxLines = if (expanded) Int.MAX_VALUE else 4,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 20.sp
                        )
                        if (content.length > 200) {
                            TextButton(onClick = { expanded = !expanded }) {
                                Text(if (expanded) "收起" else "展开全文", color = Brand400, fontSize = 12.sp)
                            }
                        }
                    }

                    // ── 播放源选择 ──
                    if (sources.size > 1) {
                        Spacer(Modifier.height(14.dp))
                        Text("播放线路", color = Color.White, style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(8.dp))
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            sources.forEachIndexed { idx, src ->
                                FilterChip(
                                    selected = idx == selectedSourceIdx,
                                    onClick = { selectedSourceIdx = idx },
                                    label = {
                                        Text(
                                            "${src.name} (${src.episodes.size}集)",
                                            fontSize = 12.sp,
                                            color = if (idx == selectedSourceIdx) Brand400 else Zinc400
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Brand400.copy(alpha = 0.15f)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }

                    // ── 选集 ──
                    if (activeSource != null) {
                        Spacer(Modifier.height(12.dp))
                        Text("选集", color = Color.White, style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(8.dp))

                        val episodes = activeSource.episodes
                        val showLimit = 20
                        var showAll by remember(selectedSourceIdx) { mutableStateOf(false) }
                        val displayCount = if (showAll) episodes.size else minOf(episodes.size, showLimit)

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            modifier = Modifier.heightIn(max = 400.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(displayCount) { epIdx ->
                                val ep = episodes[epIdx]
                                FilterChip(
                                    selected = false,
                                    onClick = { onPlay(current.siteKey, current.vodId, selectedSourceIdx, epIdx, current.vodName, realKeys) },
                                    label = { Text(ep.title, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    modifier = Modifier.width(72.dp),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }

                        if (episodes.size > showLimit) {
                            TextButton(onClick = { showAll = !showAll }) {
                                Text(
                                    if (showAll) "收起" else "展开全部 ${episodes.size} 集",
                                    color = Brand400, fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(80.dp))
                }
            }
        }
    }
}

// ── 选集解析（与桌面端 share 包逻辑一致）──
data class PlaySource(val name: String, val episodes: List<PlayEpisode>)
data class PlayEpisode(val title: String, val url: String)

private fun parseSources(detail: MediaDetail): List<PlaySource> {
    val urlStr = detail.vodPlayUrl ?: return emptyList()
    val fromStr = detail.vodPlayFrom ?: ""
    val urlParts = urlStr.split("\$\$\$")
    val fromParts = fromStr.split("\$\$\$")
    return urlParts.mapIndexed { i, part ->
        val episodes = part.split("#").filter { it.isNotBlank() }.map { ep ->
            val idx = ep.indexOf("$")
            if (idx == -1) PlayEpisode(ep, ep) else PlayEpisode(ep.substring(0, idx), ep.substring(idx + 1))
        }
        val rawName = fromParts.getOrElse(i) { "线路${i + 1}" }
        PlaySource(formatSourceName(rawName.trim(), i, detail.siteKey), episodes)
    }.filter { it.episodes.isNotEmpty() }
}

private fun rankDetailResults(list: List<MediaDetail>, expectedTitle: String, keys: String): List<MediaDetail> {
    if (list.isEmpty()) return list
    val keySet = keys.split(",")
        .map { it.trim() }
        .filter { it.contains(":") }
        .toSet()
    val expectedNorm = normalizeDetailTitle(expectedTitle)

    return list.sortedWith(
        compareByDescending<MediaDetail> { "${it.siteKey}:${it.vodId}" in keySet }
            .thenByDescending { item ->
                val itemNorm = normalizeDetailTitle(item.vodName)
                when {
                    expectedNorm.isBlank() -> 0
                    itemNorm == expectedNorm -> 4
                    itemNorm.contains(expectedNorm) || expectedNorm.contains(itemNorm) -> 2
                    else -> 0
                }
            }
            .thenByDescending { !it.vodDirector.isNullOrBlank() }
            .thenByDescending { !it.vodYear.isNullOrBlank() }
            .thenByDescending { !it.vodPlayUrl.isNullOrBlank() }
    )
}

private fun normalizeDetailTitle(value: String?): String =
    value.orEmpty()
        .lowercase()
        .replace(Regex("第[一二三四五六七八九十0-9]+季"), "")
        .replace(Regex("[\\s:：·.。!！?？\\-_/\\\\（）()【】\\[\\]]+"), "")
        .trim()
