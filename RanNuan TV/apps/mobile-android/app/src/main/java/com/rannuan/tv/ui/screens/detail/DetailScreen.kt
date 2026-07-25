package com.rannuan.tv.ui.screens.detail

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.rannuan.tv.data.api.RanNuanApi
import com.rannuan.tv.data.api.SearchStreamRequest
import com.rannuan.tv.data.model.MediaDetail
import com.rannuan.tv.data.model.MediaItem
import com.rannuan.tv.ui.theme.*
import com.rannuan.tv.ui.util.FavoritesStore
import com.rannuan.tv.ui.util.ImageProxy
import com.rannuan.tv.ui.util.formatSourceName
import com.rannuan.tv.ui.util.stripHtml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext

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

object DetailPreviewCache {
    private const val MAX_ITEMS = 48
    private val cache = object : LinkedHashMap<String, MediaDetail>(MAX_ITEMS, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, MediaDetail>?): Boolean = size > MAX_ITEMS
    }

    private fun key(name: String, keys: String) = "${name.trim()}|${keys.trim()}"

    @Synchronized fun put(name: String, keys: String, detail: MediaDetail) {
        if (name.isNotBlank()) cache[key(name, keys)] = detail
    }

    @Synchronized fun get(name: String, keys: String): MediaDetail? =
        cache[key(name, keys)] ?: cache[key(name, "")]
}

fun putDetailPreview(item: MediaItem, title: String, keys: String) {
    val name = title.ifBlank { item.vodName.ifBlank { item.title } }.trim()
    if (name.isBlank()) return
    DetailPreviewCache.put(
        name,
        keys,
        MediaDetail(
            vodId = item.vodId,
            vodName = name,
            vodPic = item.vodPic ?: item.cover,
            typeName = item.typeName ?: item.type,
            vodActor = item.vodActor,
            vodYear = item.vodYear ?: item.year,
            vodArea = item.vodArea,
            vodRemarks = item.vodRemarks,
            siteKey = item.siteKey,
            siteName = item.siteName
        )
    )
}

private data class DetailAddress(val siteKey: String, val vodId: String)

private fun parseDetailAddresses(keys: String): List<DetailAddress> = keys
    .split(',')
    .mapNotNull { raw ->
        val parts = raw.trim().split(':', limit = 2)
        if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
            DetailAddress(parts[0], parts[1])
        } else {
            null
        }
    }
    .distinct()

private fun primaryFirst(
    list: List<MediaDetail>,
    primary: MediaDetail?,
    expectedTitle: String,
    keys: String
): List<MediaDetail> {
    val ranked = rankDetailResults(list, expectedTitle, keys)
    val primaryKey = primary?.let { "${it.siteKey}:${it.vodId}" }
    if (primaryKey.isNullOrBlank()) return ranked
    return ranked.sortedBy { if ("${it.siteKey}:${it.vodId}" == primaryKey) 0 else 1 }
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

    val preview = remember(wd, realKeys) { DetailPreviewCache.get(wd, realKeys) }
    var details by remember(wd, realKeys) { mutableStateOf(preview?.let { listOf(it) } ?: emptyList()) }
    var loading by remember(wd, realKeys) { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var activeIdx by remember { mutableStateOf(0) }
    var selectedSourceIdx by remember { mutableStateOf(0) }
    var favorited by remember {
        mutableStateOf(FavoritesStore.isFavorited(context, siteKey, id))
    }
    val contentVisible = remember { androidx.compose.animation.core.MutableTransitionState(false) }

    // 主线路先出首屏，其他站点随后在同一页面补齐。
    LaunchedEffect(wd, realKeys) {
        val addresses = parseDetailAddresses(realKeys)
        val cached = DetailMemoryCache.get(wd, realKeys)
        val warmed = listOfNotNull(
            DetailWarmCache.get(wd, realKeys),
            DetailWarmCache.get(wd, "")
        ).firstOrNull { it.isNotEmpty() }
        val seeded = cached ?: warmed
        seeded?.let {
            details = it
            loading = it.none { detail -> !detail.vodPlayUrl.isNullOrBlank() }
            error = null
        }

        val hasPlayableSeed = seeded?.any { !it.vodPlayUrl.isNullOrBlank() } == true
        loading = !hasPlayableSeed
        error = null
        val primaryAddress = when {
            siteKey.isNotBlank() && id.isNotBlank() -> DetailAddress(siteKey, id)
            else -> addresses.firstOrNull()
        }
        var primary: MediaDetail? = seeded?.firstOrNull()

        fun publish(resolved: List<MediaDetail>) {
            val realResolved = resolved.filter { it.siteKey.isNotBlank() && it.vodId.isNotBlank() }
            if (realResolved.isEmpty()) return
            val activeKey = details.getOrNull(activeIdx)?.let { "${it.siteKey}:${it.vodId}" }
            val completed = primaryFirst(
                realResolved.distinctBy { "${it.siteKey}:${it.vodId}" },
                primary,
                wd,
                realKeys
            )
            if (completed.isEmpty()) return
            details = completed
            activeIdx = activeKey
                ?.let { key -> completed.indexOfFirst { "${it.siteKey}:${it.vodId}" == key } }
                ?.takeIf { it >= 0 }
                ?: activeIdx.coerceIn(0, completed.lastIndex)
            loading = false
            DetailMemoryCache.put(wd, realKeys, completed)
            val nameForCache = wd.takeIf { it.isNotBlank() } ?: completed.first().vodName
            DetailWarmCache.put(nameForCache, realKeys, completed)
            DetailWarmCache.put(nameForCache, "", completed)
        }

        try {
            if (wd.isBlank() && addresses.size <= 1 && primaryAddress != null) {
                if (!hasPlayableSeed) {
                    primary = runCatching {
                        api.getDetail(primaryAddress.siteKey, primaryAddress.vodId).let { detail ->
                            detail.copy(
                                siteKey = detail.siteKey.ifBlank { primaryAddress.siteKey },
                                vodId = detail.vodId.ifBlank { primaryAddress.vodId }
                            )
                        }
                    }.getOrNull()
                    publish(listOfNotNull(primary))
                }
                return@LaunchedEffect
            }

            val result = supervisorScope {
                val collected = mutableListOf<MediaDetail>()
                val fullKeys = realKeys.takeIf { addresses.size > 1 }.orEmpty()

                suspend fun collectRequest(query: String, requestKeys: String) {
                    var response = runCatching {
                        api.getMultiDetail(wd = query, keys = requestKeys)
                    }.getOrNull()
                    collected.addAll(response?.list.orEmpty())
                    publish(collected + listOfNotNull(primary))

                    // 首次限时响应可能只是部分线路；服务端仍在聚合，稍后补拉缓存结果。
                    if (response?.complete == false) {
                        delay(1200L)
                        response = runCatching {
                            api.getMultiDetail(wd = query, keys = requestKeys)
                        }.getOrNull()
                        collected.addAll(response?.list.orEmpty())
                        publish(collected + listOfNotNull(primary))
                    }
                }

                // keys 只负责快速精确命中；片名请求负责继续发现其余站点。
                // 两者必须并行，否则分类卡片里已有的两站会反过来限制最终线路数量。
                val exactRequest = fullKeys.takeIf { it.isNotBlank() }?.let { requestKeys ->
                    async { collectRequest(query = "", requestKeys = requestKeys) }
                }
                val namedRequest = wd.takeIf { it.isNotBlank() }?.let { query ->
                    async { collectRequest(query = query, requestKeys = "") }
                }
                val primaryRequest = if (!hasPlayableSeed) async {
                    val first = runCatching {
                        if (primaryAddress != null) {
                            api.getDetail(primaryAddress.siteKey, primaryAddress.vodId).let { detail ->
                                detail.copy(
                                    siteKey = detail.siteKey.ifBlank { primaryAddress.siteKey },
                                    vodId = detail.vodId.ifBlank { primaryAddress.vodId }
                                )
                            }
                        } else {
                            api.getMultiDetail(wd = wd, fast = true).list.firstOrNull()
                        }
                    }.getOrNull()
                    if (first != null) {
                        primary = first
                        collected.add(first)
                        publish(collected)
                    }
                    first
                } else null

                primaryRequest?.await()
                exactRequest?.await()
                namedRequest?.await()
                collected
            }

            if (result.isEmpty() && wd.isNotBlank() && realKeys.isNotBlank()) {
                val fallbackResp = api.getMultiDetail(wd = wd, keys = "")
                result.addAll(rankDetailResults(fallbackResp.list, wd, ""))
            }
            if (result.isEmpty() && wd.isNotBlank()) {
                val candidateKeys = searchDetailCandidateKeys(api, wd)
                if (candidateKeys.isNotBlank()) {
                    val candidateResp = api.getMultiDetail(wd = wd, keys = candidateKeys)
                    result.addAll(rankDetailResults(candidateResp.list, wd, candidateKeys))
                }
            }

            publish(result + listOfNotNull(primary))
            if (details.isEmpty()) {
                error = "未找到影片详情"
            }
        } catch (e: Exception) {
            if (details.isEmpty()) error = e.message
        } finally {
            loading = false
        }
    }

    val current = details.getOrNull(activeIdx)
    val sources = remember(current) { current?.let { parseSources(it) } ?: emptyList() }
    val activeSource = sources.getOrNull(selectedSourceIdx)

    if (loading && current == null) {
        DetailLoadingScreen(onBack = onBack)
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
                    if (loading) {
                        Spacer(Modifier.height(10.dp))
                        DetailCompletingCard()
                    }

                    // ── 操作按钮 ──
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { onPlay(current.siteKey, current.vodId, selectedSourceIdx, 0, current.vodName, realKeys) },
                            enabled = sources.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = Brand500),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.PlayArrow, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (sources.isNotEmpty()) "立即播放" else if (loading) "加载播放源..." else "暂无播放源",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                favorited = FavoritesStore.toggle(context, current)
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
                    if (loading && sources.isEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            color = Brand400,
                            trackColor = Zinc800,
                            modifier = Modifier.fillMaxWidth().height(2.dp)
                        )
                    }

                    // ── 多源站点切换 ──
                    if (details.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        Text("播放源", color = Color.White, style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(8.dp))
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            details.forEachIndexed { idx, d ->
                                FilterChip(
                                    selected = idx == activeIdx,
                                    onClick = {
                                        if (details.size > 1) {
                                            activeIdx = idx
                                            selectedSourceIdx = 0
                                        }
                                    },
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
        }.filter { isM3u8Url(it.url) }
        val rawName = fromParts.getOrElse(i) { "线路${i + 1}" }
        PlaySource(formatSourceName(rawName.trim(), i, detail.siteKey), episodes)
    }.filter { it.episodes.isNotEmpty() }
}

private fun isM3u8Url(url: String): Boolean =
    url.substringBefore('?').substringBefore('#').endsWith(".m3u8", ignoreCase = true) ||
        url.contains(".m3u8?", ignoreCase = true) ||
        url.contains(".m3u8#", ignoreCase = true)

@Composable
private fun DetailLoadingScreen(onBack: () -> Unit) {
    val shimmer by rememberInfiniteTransition(label = "detailLoadingShimmer").animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "detailLoadingAlpha"
    )
    Column(Modifier.fillMaxSize().background(Zinc950)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Zinc900)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .size(36.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(44.dp)
                    .background(Brand400.copy(alpha = 0.12f * shimmer), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Brand400, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
            }
        }

        Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
            SkeletonLine(widthFraction = 0.72f, height = 24.dp, alpha = shimmer)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SkeletonLine(widthFraction = 0.18f, height = 18.dp, alpha = shimmer)
                SkeletonLine(widthFraction = 0.22f, height = 18.dp, alpha = shimmer)
                SkeletonLine(widthFraction = 0.20f, height = 18.dp, alpha = shimmer)
            }
            Spacer(Modifier.height(16.dp))
            Surface(
                color = Brand400.copy(alpha = 0.10f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(color = Brand400, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("正在加载影片详情和播放源...", color = Brand400, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            repeat(3) { idx ->
                SkeletonLine(
                    widthFraction = when (idx) {
                        0 -> 0.96f
                        1 -> 0.88f
                        else -> 0.62f
                    },
                    height = 13.dp,
                    alpha = shimmer
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun DetailCompletingCard() {
    val pulse by rememberInfiniteTransition(label = "detailCompletingPulse").animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850),
            repeatMode = RepeatMode.Reverse
        ),
        label = "detailCompletingAlpha"
    )
    Surface(
        color = Brand400.copy(alpha = 0.10f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(24.dp)
                    .background(Brand400.copy(alpha = 0.12f * pulse), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(15.dp), color = Brand400, strokeWidth = 2.dp)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("正在补全详情和播放源", color = Brand400, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text("页面可先浏览，播放源加载完成后即可播放", color = Zinc500, fontSize = 11.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun SkeletonLine(widthFraction: Float, height: androidx.compose.ui.unit.Dp, alpha: Float) {
    Box(
        Modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(RoundedCornerShape(6.dp))
            .background(Zinc800.copy(alpha = 0.45f + alpha * 0.28f))
    )
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

private suspend fun searchDetailCandidateKeys(api: RanNuanApi, title: String): String {
    val candidates = streamDetailSearchCandidates(api, title)
    if (candidates.isEmpty()) return ""
    return candidates
        .sortedWith(
            compareByDescending<MediaItem> { detailTitleScore(title, it.vodName.ifBlank { it.title }) }
                .thenByDescending { it.vodPic?.startsWith("http") == true || it.cover?.startsWith("http") == true }
                .thenByDescending { it.vodYear?.toIntOrNull() ?: it.year?.toIntOrNull() ?: 0 }
        )
        .filter { it.siteKey.isNotBlank() && it.vodId.isNotBlank() }
        .take(6)
        .joinToString(",") { "${it.siteKey}:${it.vodId}" }
}

private suspend fun streamDetailSearchCandidates(api: RanNuanApi, query: String): List<MediaItem> {
    val gson = Gson()
    val itemListType = object : TypeToken<List<MediaItem>>() {}.type
    val collected = mutableListOf<MediaItem>()
    var merged: List<MediaItem>? = null
    val body = withContext(Dispatchers.IO) { api.searchStream(SearchStreamRequest(query)) }

    try {
        withContext(Dispatchers.IO) {
            val reader = body.byteStream().bufferedReader(Charsets.UTF_8)
            while (true) {
                currentCoroutineContext().ensureActive()
                val line = reader.readLine() ?: break
                if (!line.startsWith("data: ")) continue
                val event = runCatching {
                    JsonParser().parse(line.removePrefix("data: ").trim()).asJsonObject
                }.getOrNull() ?: continue
                when (event.getString("type")) {
                    "videos" -> collected.addAll(event.readMediaItems(gson, itemListType))
                    "merged" -> merged = event.readMediaItems(gson, itemListType)
                }
            }
        }
    } finally {
        body.close()
    }

    return dedupeMediaItems(merged ?: collected)
}

private fun detailTitleScore(expected: String, actual: String): Int {
    val expectedNorm = normalizeDetailTitle(expected)
    val actualNorm = normalizeDetailTitle(actual)
    if (expectedNorm.isBlank() || actualNorm.isBlank()) return 0
    return when {
        expectedNorm == actualNorm -> 100
        actualNorm.startsWith(expectedNorm) || expectedNorm.startsWith(actualNorm) -> 76
        actualNorm.contains(expectedNorm) || expectedNorm.contains(actualNorm) -> 58
        else -> 0
    }
}

private fun dedupeMediaItems(list: List<MediaItem>): List<MediaItem> {
    val seen = HashSet<String>()
    return list.filter { item ->
        val key = "${item.siteKey.ifBlank { item.siteName }}:${item.vodId.ifBlank { item.vodName.ifBlank { item.title } }}"
        seen.add(key)
    }
}

private fun JsonObject.getString(key: String): String? =
    if (has(key) && !get(key).isJsonNull) get(key).asString else null

private fun JsonObject.readMediaItems(gson: Gson, itemListType: java.lang.reflect.Type): List<MediaItem> {
    if (!has("videos") || get("videos").isJsonNull) return emptyList()
    return runCatching { gson.fromJson<List<MediaItem>>(get("videos"), itemListType) }.getOrDefault(emptyList())
}

private fun normalizeDetailTitle(value: String?): String =
    value.orEmpty()
        .lowercase()
        .replace(Regex("[\\s:：·.。!！?？\\-_/\\\\（）()【】\\[\\]]+"), "")
        .trim()
