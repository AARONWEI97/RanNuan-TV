package com.rannuan.tv.ui.screens.search

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rannuan.tv.data.api.RanNuanApi
import com.rannuan.tv.data.model.MediaItem
import com.rannuan.tv.ui.screens.home.MediaCard
import com.rannuan.tv.ui.theme.*
import com.rannuan.tv.ui.theme.component.GlassCard
import com.rannuan.tv.ui.util.SearchHistoryStore
import kotlinx.coroutines.*
import java.net.URLEncoder

private const val SEARCH_CACHE_MAX_ITEMS = 24
private const val SEARCH_CACHE_MAX_RESULTS_PER_QUERY = 120
private const val SEARCH_CACHE_TTL_MS = 10 * 60 * 1000L

private object SearchMemoryCache {
    private data class Entry(val results: List<MediaItem>, val savedAt: Long)

    private val cache = object : LinkedHashMap<String, Entry>(SEARCH_CACHE_MAX_ITEMS, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Entry>?): Boolean {
            return size > SEARCH_CACHE_MAX_ITEMS
        }
    }

    private fun normalize(query: String): String = query.trim().lowercase()

    @Synchronized
    fun get(query: String): List<MediaItem>? {
        val cacheKey = normalize(query)
        val entry = cache[cacheKey] ?: return null
        if (System.currentTimeMillis() - entry.savedAt > SEARCH_CACHE_TTL_MS) {
            cache.remove(cacheKey)
            return null
        }
        return entry.results
    }

    @Synchronized
    fun put(query: String, results: List<MediaItem>) {
        cache[normalize(query)] = Entry(results.take(SEARCH_CACHE_MAX_RESULTS_PER_QUERY), System.currentTimeMillis())
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(api: RanNuanApi, onNavigate: (String) -> Unit, initialQuery: String = "") {
    val context = LocalContext.current
    var query by remember { mutableStateOf(initialQuery) }
    var results by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var progressText by remember { mutableStateOf("") }
    var selectedSite by remember { mutableStateOf<String?>(null) }
    var sortBy by remember { mutableStateOf("default") }
    var hasSearched by remember { mutableStateOf(initialQuery.isNotBlank()) }
    val scope = rememberCoroutineScope()
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var lastSearchedQuery by remember { mutableStateOf("") }
    var history by remember { mutableStateOf(SearchHistoryStore.getHistory(context)) }
    val normalizedQuery = remember(query) { query.trim() }

    // 热门搜索词
    val hotWords = remember {
        listOf("庆余年", "繁花", "周处除三害", "热辣滚烫", "飞驰人生2",
            "三大队", "狂飙", "莲花楼", "长相思", "少年白马醉春风")
    }

    // 自动搜索（仅当 query 改变且与上次不同）
    LaunchedEffect(normalizedQuery) {
        if (normalizedQuery.length >= 2 && normalizedQuery != lastSearchedQuery) {
            delay(400)
            if (normalizedQuery.length < 2 || normalizedQuery == lastSearchedQuery) return@LaunchedEffect
            searchJob?.cancel()
            lastSearchedQuery = normalizedQuery
            loading = true
            hasSearched = true
            progressText = "搜索中..."
            selectedSite = null

            val cached = SearchMemoryCache.get(normalizedQuery)
            if (cached != null) {
                results = cached
                loading = false
                progressText = "找到 ${cached.size} 个结果"
                SearchHistoryStore.addHistory(context, normalizedQuery)
                history = SearchHistoryStore.getHistory(context)
                return@LaunchedEffect
            }

            searchJob = scope.launch {
                try {
                    val data = api.search(normalizedQuery)
                    val ranked = rankSearchResults(normalizedQuery, data.list)
                    SearchMemoryCache.put(normalizedQuery, ranked)
                    results = ranked
                    progressText = "找到 ${ranked.size} 个结果"
                    // 记录搜索历史
                    SearchHistoryStore.addHistory(context, normalizedQuery)
                    history = SearchHistoryStore.getHistory(context)
                } catch (e: CancellationException) { }
                catch (e: Exception) { progressText = e.message ?: "搜索失败" }
                finally { loading = false }
            }
        }
    }

    fun cancelSearch() {
        searchJob?.cancel()
        loading = false
        progressText = "已取消"
    }

    // 提取所有来源（使用 siteName，回退到 siteKey）
    val sites = results.map { it.siteName.takeIf { n -> n.isNotBlank() } ?: it.siteKey }.distinct()
    val filtered = results
        .let { list -> if (selectedSite != null) list.filter { (it.siteName.takeIf { n -> n.isNotBlank() } ?: it.siteKey) == selectedSite } else list }
        .let { list ->
            when (sortBy) {
                "name" -> list.sortedBy { it.vodName }
                "site" -> list.sortedBy { it.siteKey }
                else -> list
            }
        }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── 搜索栏（玻璃拟态包裹）──
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            cornerRadius = 16.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Search, null, tint = Zinc500, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("搜索影片…", color = Zinc500, fontSize = 15.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )
                if (query.isNotEmpty()) {
                    IconButton(onClick = {
                        searchJob?.cancel()
                        lastSearchedQuery = ""
                        query = ""; results = emptyList(); progressText = ""; hasSearched = false
                    }) {
                        Icon(Icons.Filled.Close, null, tint = Zinc500)
                    }
                }
                if (loading) {
                    TextButton(onClick = { cancelSearch() }) {
                        Text("取消", color = Error500, fontSize = 13.sp)
                    }
                }
            }
        }

        // ── 进度 ──
        if (progressText.isNotBlank()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp), color = Brand400, strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(progressText, color = if (loading) Brand400 else Zinc500, fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                if (!loading && results.isNotEmpty()) {
                    Text("共 ${results.size} 个", color = Zinc600, fontSize = 12.sp)
                }
            }
        }

        // ── 来源过滤 ──
        if (sites.isNotEmpty()) {
            var selectedSiteIdx by remember { mutableStateOf(-1) }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // "全部" 按钮
                FilterChip(
                    selected = selectedSite == null,
                    onClick = { selectedSite = null; selectedSiteIdx = -1 },
                    label = { Text("全部", fontSize = 11.sp, color = if (selectedSite == null) Brand400 else Zinc400) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Brand400.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                sites.take(8).forEachIndexed { idx, site ->
                    FilterChip(
                        selected = selectedSite == site,
                        onClick = { selectedSite = site; selectedSiteIdx = idx + 1 },
                        label = { Text(site, fontSize = 11.sp, color = if (selectedSite == site) Brand400 else Zinc400) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Brand400.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }

        // ── 排序 ──
        if (results.isNotEmpty()) {
            Row(Modifier.padding(horizontal = 12.dp)) {
                listOf("默认" to "default", "名称" to "name", "来源" to "site").forEach { (label, key) ->
                    FilterChip(
                        selected = sortBy == key,
                        onClick = { sortBy = key },
                        label = { Text(label, fontSize = 11.sp) },
                        modifier = Modifier.padding(end = 4.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Brand400.copy(alpha = 0.15f),
                            selectedLabelColor = Brand400
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }

        // ── 结果 / 空状态 ──
        if (!hasSearched && query.isBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // 搜索历史
                if (history.isNotEmpty()) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.History, null, tint = Zinc500, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("搜索历史", color = Zinc400, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                        IconButton(onClick = {
                            SearchHistoryStore.clearHistory(context)
                            history = emptyList()
                        }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.DeleteSweep, null, tint = Zinc600, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        history.take(6).forEach { word ->
                            FilterChip(
                                selected = false,
                                onClick = { query = word },
                                label = { Text(word, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Zinc800,
                                    labelColor = Zinc400
                                ),
                                shape = ShapeChip
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
                // 热门搜索
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.TrendingUp, null, tint = Brand400, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("热门搜索", color = Zinc400, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    hotWords.forEach { word ->
                        FilterChip(
                            selected = false,
                            onClick = { query = word },
                            label = { Text(word, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Zinc800,
                                labelColor = Zinc400
                            ),
                            shape = ShapeChip
                        )
                    }
                }
            }
        } else if (loading && results.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Brand400)
            }
        } else if (hasSearched && results.isEmpty() && !loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Search, null, tint = Zinc700, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(10.dp))
                    Text("未找到相关内容", color = Zinc500, fontSize = 14.sp)
                    Text("换个关键词试试", color = Zinc600, fontSize = 12.sp)
                }
            }
        } else if (filtered.isNotEmpty()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered.size, key = { filtered[it].vodId + filtered[it].siteKey }) { idx ->
                    val item = filtered[idx]
                    val route = if (item.sites != null && (item.sites?.size ?: 0) > 1) {
                        val k = item.sites.joinToString(",") { "${it.key}:${it.id}" }
                        "detail/source/${URLEncoder.encode(item.vodName, "UTF-8")}?keys=$k"
                    } else {
                        "detail/${item.siteKey}/${item.vodId}"
                    }
                    MediaCard(item = item) { onNavigate(route) }
                }
            }
        }
    }
}

private fun rankSearchResults(query: String, list: List<MediaItem>): List<MediaItem> {
    val q = query.trim()
    if (q.isBlank()) return list

    fun contains(value: String?): Boolean = value?.contains(q, ignoreCase = true) == true
    fun equalsPart(value: String?): Boolean {
        return value
            ?.split(Regex("[,，、/\\s]+"))
            ?.any { it.equals(q, ignoreCase = true) } == true
    }

    return list.sortedWith(
        compareByDescending<MediaItem> { equalsPart(it.vodActor) }
            .thenByDescending { contains(it.vodActor) }
            .thenByDescending { it.vodName.equals(q, ignoreCase = true) }
            .thenByDescending { it.vodName.startsWith(q, ignoreCase = true) }
            .thenByDescending { contains(it.vodName) }
            .thenByDescending { it.vodYear?.toIntOrNull() ?: 0 }
    )
}
