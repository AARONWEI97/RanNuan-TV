package com.rannuan.tv.ui.screens.category

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.*
import com.rannuan.tv.data.api.CategoryRequest
import com.rannuan.tv.data.api.RanNuanApi
import com.rannuan.tv.data.model.CategoryResponse
import com.rannuan.tv.data.model.MediaItem
import com.rannuan.tv.ui.screens.home.MediaCard
import com.rannuan.tv.ui.theme.*

private const val CATEGORY_PAGE_SIZE = 20
private const val CATEGORY_CACHE_MAX_PAGES = 24
private const val CATEGORY_CACHE_TTL_MS = 5 * 60 * 1000L

private data class CategoryRestore(
    val items: List<MediaItem>,
    val lastResponse: CategoryResponse?
)

private object CategoryPageCache {
    private data class Entry(val response: CategoryResponse, val savedAt: Long)

    private val pages = object : LinkedHashMap<String, Entry>(CATEGORY_CACHE_MAX_PAGES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Entry>?): Boolean {
            return size > CATEGORY_CACHE_MAX_PAGES
        }
    }

    private fun key(category: String, subType: String?, page: Int, pageSize: Int): String {
        return "$category|${subType.orEmpty()}|$page|$pageSize"
    }

    @Synchronized
    fun get(category: String, subType: String?, page: Int, pageSize: Int = CATEGORY_PAGE_SIZE): CategoryResponse? {
        val cacheKey = key(category, subType, page, pageSize)
        val entry = pages[cacheKey] ?: return null
        if (System.currentTimeMillis() - entry.savedAt > CATEGORY_CACHE_TTL_MS) {
            pages.remove(cacheKey)
            return null
        }
        return entry.response
    }

    @Synchronized
    fun put(category: String, subType: String?, page: Int, response: CategoryResponse, pageSize: Int = CATEGORY_PAGE_SIZE) {
        pages[key(category, subType, page, pageSize)] = Entry(response, System.currentTimeMillis())
    }

    @Synchronized
    fun restore(category: String, subType: String?, untilPage: Int, pageSize: Int = CATEGORY_PAGE_SIZE): CategoryRestore {
        val items = mutableListOf<MediaItem>()
        var lastResponse: CategoryResponse? = null
        for (page in 1..untilPage.coerceAtLeast(1)) {
            val response = get(category, subType, page, pageSize) ?: break
            items += response.list
            lastResponse = response
        }
        return CategoryRestore(items, lastResponse)
    }
}

val CATEGORY_NAMES = mapOf(
    "movie" to "电影", "tv" to "电视剧", "variety" to "综艺",
    "anime" to "动漫", "shortDrama" to "短剧", "sports" to "体育"
)

// 子分类标签（与服务端 SUB_TYPE_MAP 对应，subType 传标签名走服务端 type_id 过滤）
val SUB_CATEGORIES = mapOf(
    "movie" to listOf("动作", "喜剧", "爱情", "科幻", "恐怖", "剧情", "战争", "动画"),
    "tv" to listOf("国产", "港剧", "台剧", "日剧", "韩剧", "美剧", "泰剧"),
    "anime" to listOf("日漫", "国漫", "欧美", "剧场版"),
    "variety" to listOf("大陆综艺", "港台综艺", "日韩综艺", "欧美综艺"),
    "sports" to listOf("足球", "篮球", "网球", "斯诺克"),
    "shortDrama" to listOf("现代言情", "古装仙侠", "穿越年代", "反转爽文", "女频总裁", "都市脑洞")
)

@Composable
fun CategoryScreen(type: String, api: RanNuanApi, onNavigate: (String) -> Unit) {
    var currentType by rememberSaveable { mutableStateOf(type) }
    val label = CATEGORY_NAMES[currentType] ?: currentType
    var data by remember { mutableStateOf<CategoryResponse?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var page by rememberSaveable { mutableStateOf(1) }
    var loadingMore by remember { mutableStateOf(false) }
    var allItems by remember(currentType) { mutableStateOf<List<MediaItem>>(emptyList()) }
    // 当前选中的子分类标签（null = 全部）
    var activeSub by rememberSaveable { mutableStateOf<String?>(null) }
    val subList = SUB_CATEGORIES[currentType] ?: emptyList()
    val gridState = rememberLazyGridState()

    fun restoreCachedFirstPage(subType: String?) {
        val cached = CategoryPageCache.restore(currentType, subType, 1)
        data = cached.lastResponse
        allItems = cached.items
        loading = cached.items.isEmpty()
        loadingMore = false
        error = null
    }

    // 分类切换时重置
    LaunchedEffect(currentType) {
        activeSub = null
        page = 1
        restoreCachedFirstPage(null)
        if (currentType != type) {
            onNavigate("category/$currentType")
        }
    }

    // 加载分类数据
    LaunchedEffect(currentType, page, activeSub) {
        val cached = CategoryPageCache.get(currentType, activeSub, page)
        if (cached != null) {
            val restored = CategoryPageCache.restore(currentType, activeSub, page)
            allItems = restored.items.ifEmpty { cached.list }
            data = restored.lastResponse ?: cached
            loading = false
            loadingMore = false
            error = null
            return@LaunchedEffect
        }

        try {
            val resp = api.getCategory(
                CategoryRequest(
                    category = currentType,
                    page = page,
                    pageSize = CATEGORY_PAGE_SIZE,
                    subType = activeSub
                )
            )
            CategoryPageCache.put(currentType, activeSub, page, resp)
            if (page == 1) {
                data = resp
                allItems = resp.list
            } else {
                allItems = allItems + resp.list
            }
            data = resp
            loading = false
            loadingMore = false
        } catch (e: Exception) {
            if (page == 1) { error = e.message; loading = false }
            loadingMore = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 分类切换 FilterChip 横滑条（比 ScrollableTabRow 更轻量）
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(Zinc950)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(CATEGORY_NAMES.entries.toList()) { (key, name) ->
                FilterChip(
                    selected = currentType == key,
                    onClick = { if (currentType != key) currentType = key },
                    label = {
                        Text(
                            name,
                            fontSize = 13.sp,
                            fontWeight = if (currentType == key) FontWeight.SemiBold else FontWeight.Normal
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

        // ── 子分类筛选标签栏（仅当前分类有子分类时显示）──
        if (subList.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Zinc950)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = activeSub == null,
                        onClick = {
                            if (activeSub != null) {
                                activeSub = null
                                page = 1
                                restoreCachedFirstPage(null)
                            }
                        },
                        label = { Text("全部", fontSize = 12.sp, fontWeight = if (activeSub == null) FontWeight.SemiBold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Brand400.copy(alpha = 0.18f),
                            selectedLabelColor = Brand400,
                            containerColor = Zinc900,
                            labelColor = Zinc500
                        ),
                        shape = ShapeChip
                    )
                }
                items(subList) { sub ->
                    val count = data?.subCounts?.get(sub) ?: 0
                    FilterChip(
                        selected = activeSub == sub,
                        onClick = {
                            if (activeSub != sub) {
                                activeSub = sub
                                page = 1
                                restoreCachedFirstPage(sub)
                            }
                        },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    sub,
                                    fontSize = 12.sp,
                                    fontWeight = if (activeSub == sub) FontWeight.SemiBold else FontWeight.Normal
                                )
                                if (count > 0) {
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        count.toString(),
                                        fontSize = 10.sp,
                                        color = if (activeSub == sub) Brand400.copy(alpha = 0.7f) else Zinc600
                                    )
                                }
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Brand400.copy(alpha = 0.18f),
                            selectedLabelColor = Brand400,
                            containerColor = Zinc900,
                            labelColor = Zinc500
                        ),
                        shape = ShapeChip
                    )
                }
            }
        }

        if (loading && page == 1) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Brand400)
            }
        } else if (error != null && allItems.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.ErrorOutline, null,
                        tint = Zinc500, modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(error ?: "加载失败", color = Zinc400, fontSize = 14.sp)
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = { page = 1; loading = true; error = null }) {
                        Text("重试", color = Brand400)
                    }
                }
            }
        } else if (allItems.isEmpty() && !loading) {
            // 空态
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.Category, null,
                        tint = Zinc600, modifier = Modifier.size(56.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("暂无内容", color = Zinc500, fontSize = 14.sp)
                }
            }
        } else {
            // 标题行
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val subLabel = if (activeSub != null) " · $activeSub" else ""
                Text(
                    "$label$subLabel · ${data?.total ?: allItems.size} 部",
                    color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium
                )
            }

            // ── Grid + 滚动监听翻页 ──
            // 用 derivedStateOf 检测是否滚动到接近底部（最后 4 个 item）
            val shouldLoadMore by remember {
                derivedStateOf {
                    val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                    val total = gridState.layoutInfo.totalItemsCount
                    total > 0 && lastVisible >= total - 4
                }
            }

            // 触发翻页
            LaunchedEffect(shouldLoadMore, loadingMore, page, allItems.size, data?.totalPages, data?.outOfRange) {
                val totalPages = data?.totalPages ?: 0
                val currentPageHadItems = data?.list?.isNotEmpty() == true
                val hasMore = !(data?.outOfRange ?: false) &&
                    (page < totalPages || (currentPageHadItems && allItems.size >= page * CATEGORY_PAGE_SIZE))
                if (shouldLoadMore && !loadingMore && hasMore && allItems.isNotEmpty()) {
                    loadingMore = true
                    page++
                }
            }

            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(allItems.size) { idx ->
                    val item = allItems[idx]
                    MediaCard(item = item) {
                        val route = if (item.sites != null && item.sites.size > 1) {
                            val keys = item.sites.joinToString(",") { "${it.key}:${it.id}" }
                            "detail/source/${java.net.URLEncoder.encode(it.vodName, "UTF-8")}?keys=$keys"
                        } else {
                            "detail/${it.siteKey}/${it.vodId}"
                        }
                        onNavigate(route)
                    }
                }
                if (loadingMore) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Brand400, strokeWidth = 2.dp)
                        }
                    }
                }
            }
        }
    }
}
