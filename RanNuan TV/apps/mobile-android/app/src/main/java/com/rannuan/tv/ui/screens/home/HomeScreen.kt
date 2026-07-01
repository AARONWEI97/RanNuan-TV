package com.rannuan.tv.ui.screens.home

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Animation
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.TheaterComedy
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.ViewModule
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.rannuan.tv.data.api.RanNuanApi
import com.rannuan.tv.data.model.DoubanHomeData
import com.rannuan.tv.data.model.MediaItem
import com.rannuan.tv.ui.screens.detail.putDetailPreview
import com.rannuan.tv.ui.theme.Brand400
import com.rannuan.tv.ui.theme.Brand500
import com.rannuan.tv.ui.theme.ShapeCard
import com.rannuan.tv.ui.theme.ShapeCardLarge
import com.rannuan.tv.ui.theme.ShapePill
import com.rannuan.tv.ui.theme.Zinc700
import com.rannuan.tv.ui.theme.Zinc800
import com.rannuan.tv.ui.theme.Zinc300
import com.rannuan.tv.ui.theme.Zinc400
import com.rannuan.tv.ui.theme.Zinc500
import com.rannuan.tv.ui.theme.Zinc950
import com.rannuan.tv.ui.theme.component.GlassCard
import com.rannuan.tv.ui.util.ImageProxy
import kotlinx.coroutines.delay
import java.net.URLEncoder
import kotlin.math.abs
import androidx.compose.ui.platform.LocalContext

// 分类配置数据
data class CategoryConfig(
    val label: String,
    val routeKey: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(api: RanNuanApi, onNavigate: (String) -> Unit) {
    var data by remember { mutableStateOf<DoubanHomeData?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            data = api.getDoubanHome()
        } catch (e: Exception) {
            error = e.message
        } finally {
            loading = false
        }
    }

    when {
        loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Brand400)
            }
        }
        error != null -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Zinc500
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        error ?: "",
                        color = Zinc400,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = { error = null }) {
                        Text("重试", color = Brand400)
                    }
                }
            }
        }
        else -> {
            val homeData = data!!
            val hot = homeData.hot.filter { it.cover != null }

            val categoryConfig = listOf(
                CategoryConfig("电视剧", "tv", Icons.Outlined.Tv),
                CategoryConfig("电影", "movie", Icons.Outlined.Movie),
                CategoryConfig("综艺", "variety", Icons.Outlined.TheaterComedy),
                CategoryConfig("动漫", "anime", Icons.Outlined.Animation),
                CategoryConfig("短剧", "shortDrama", Icons.Outlined.ViewModule),
                CategoryConfig("体育", "sports", Icons.Outlined.FitnessCenter),
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // ── 1. Banner ──
                if (hot.isNotEmpty()) {
                    BannerCarousel(
                        items = hot,
                        onPlay = { item ->
                            onNavigate(detailRouteFor(item))
                        },
                        onDetail = { item ->
                            onNavigate(detailRouteFor(item))
                        }
                    )
                }

                Spacer(Modifier.height(12.dp))

                // ── 2. 分类快捷入口（横滑，不怕挤）──
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(categoryConfig.size) { idx ->
                        val config = categoryConfig[idx]
                        CategoryEntry(
                            label = config.label,
                            icon = config.icon,
                            modifier = Modifier.width(72.dp),
                            onClick = { onNavigate("category/${config.routeKey}") }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── 3. 各分区横滑列表（更多跳对应分类页，热门跳搜索）──
                data class Section(val title: String, val items: List<MediaItem>, val moreRoute: String)
                val sections = listOf(
                    Section("热门推荐", hot, "category/movie"),
                    Section("电视剧", homeData.dianshiju, "category/tv"),
                    Section("电影", homeData.dianying, "category/movie"),
                    Section("综艺", homeData.zongyi, "category/variety"),
                    Section("动漫", homeData.dongman, "category/anime"),
                )

                sections.forEach { section ->
                    if (section.items.isNotEmpty()) {
                        SectionHeader(
                            title = section.title,
                            onMore = { onNavigate(section.moreRoute) }
                        )
                        MediaHorizontalList(
                            items = section.items,
                            onClick = { item ->
                                onNavigate(detailRouteFor(item))
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }

                // ── 4. 底部留白（避免被 NavigationBar 遮挡） ──
                Spacer(Modifier.height(128.dp))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Banner 轮播 — 视差缩放 + 动画 Indicator + 鲁棒自动播放
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BannerCarousel(items: List<MediaItem>, onPlay: (MediaItem) -> Unit, onDetail: (MediaItem) -> Unit) {
    val pagerState = rememberPagerState(pageCount = { items.size })

    // 自动轮播（独立协程循环，不被 pagerState 变化打断）
    LaunchedEffect(Unit) {
        if (items.size <= 1) return@LaunchedEffect
        while (true) {
            delay(4000)
            if (!pagerState.isScrollInProgress) {
                val next = (pagerState.currentPage + 1) % items.size
                pagerState.animateScrollToPage(next)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        Box(modifier = Modifier.clip(ShapeCardLarge)) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.aspectRatio(16f / 9f)
            ) { page ->
                val item = items[page]

                // 视差缩放
                val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).coerceIn(-1f, 1f)
                val scale = 1f - abs(pageOffset) * 0.08f

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                ) {
                    // 海报
                    AsyncImage(
                        model = ImageProxy.proxy(item.cover),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // 底部渐变蒙层（加深）
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color.Transparent,
                                        Color.Transparent,
                                        Zinc950.copy(alpha = 0.6f),
                                        Zinc950
                                    )
                                )
                            )
                    )

                    // 文字 + 按钮区（玻璃拟态包裹）
                    GlassCard(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp),
                        cornerRadius = 14.dp
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                item.title,
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (item.rating != null) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "⭐ ${item.rating}",
                                    color = Brand400,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onPlay(item) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Brand500),
                                    shape = ShapePill,
                                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("立即播放", fontSize = 13.sp)
                                }
                                OutlinedButton(
                                    onClick = { onDetail(item) },
                                    border = BorderStroke(1.dp, Brand400),
                                    shape = ShapePill,
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text("详情", color = Brand400, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Indicator（底部中央，动画过渡） ──
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(items.size) { idx ->
                val selected = idx == pagerState.currentPage
                val width by animateDpAsState(
                    targetValue = if (selected) 20.dp else 6.dp
                )
                Box(
                    modifier = Modifier
                        .size(width, 6.dp)
                        .clip(ShapePill)
                        .background(
                            if (selected) Brand400
                            else Color.White.copy(alpha = 0.4f)
                        )
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// 分类快捷入口 — 玻璃拟态 + 图标
// ═══════════════════════════════════════════════════════════════

@Composable
fun CategoryEntry(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        elevation = 2.dp,
        cornerRadius = 14.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                modifier = Modifier.size(26.dp),
                tint = Brand400
            )
            Spacer(Modifier.height(6.dp))
            Text(
                label,
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// 分区标题
// ═══════════════════════════════════════════════════════════════

@Composable
fun SectionHeader(title: String, onMore: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium
        )
        TextButton(onClick = onMore) {
            Text("更多 >", color = Brand400, style = MaterialTheme.typography.labelSmall)
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// 横滑列表
// ═══════════════════════════════════════════════════════════════

@Composable
fun MediaHorizontalList(items: List<MediaItem>, onClick: (MediaItem) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(items.size) { index ->
            MediaCard(item = items[index], onClick = { onClick(it) })
        }
    }
}

private fun detailRouteFor(item: MediaItem): String {
    val title = item.vodName.takeIf { it.isNotBlank() } ?: item.title
    val encodedTitle = URLEncoder.encode(title, "UTF-8")
    return when {
        !item.sites.isNullOrEmpty() -> {
            val keys = item.sites.joinToString(",") { "${it.key}:${it.id}" }
            putDetailPreview(item, title, keys)
            "detail/source/$encodedTitle?keys=$keys"
        }
        title.isNotBlank() -> {
            putDetailPreview(item, title, "")
            "detail/source/$encodedTitle?keys="
        }
        else -> "search"
    }
}

// ═══════════════════════════════════════════════════════════════
// 精致化媒体卡片 — 评分胶囊 + 阴影 + 按压缩放
// ═══════════════════════════════════════════════════════════════

@Composable
fun MediaCard(item: MediaItem, onClick: (MediaItem) -> Unit) {
    val context = LocalContext.current
    val cardWidth = 115.dp
    val title = item.title.ifBlank { item.vodName }
    val subTitle = buildString {
        item.year?.let { append(it) }
        if (!item.year.isNullOrBlank() && !item.type.isNullOrBlank()) append(" · ")
        item.type?.let { append(it) }
        if (isBlank()) {
            item.vodYear?.takeIf { it.isNotBlank() }?.let { append(it) }
            val typeName = item.typeName?.takeIf { it.isNotBlank() }
            if (isNotBlank() && typeName != null) append(" · ")
            typeName?.let { append(it) }
        }
    }
    val posterRequest = remember(item.cover, item.vodPic) {
        ImageRequest.Builder(context)
            .data(ImageProxy.proxy(item.cover ?: item.vodPic))
            .crossfade(false)
            .size(240, 360)
            .build()
    }

    Column(
        modifier = Modifier
            .width(cardWidth)
            .clickable { onClick(item) }
    ) {
        // 海报区
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(ShapeCard)
                .background(Zinc800)
                .shadow(4.dp, ShapeCard)
        ) {
            Icon(
                Icons.Outlined.Image,
                contentDescription = null,
                tint = Zinc700,
                modifier = Modifier
                    .size(30.dp)
                    .align(Alignment.Center)
            )
            AsyncImage(
                model = posterRequest,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // 评分胶囊（右上角）
            if (item.rating != null) {
                Surface(
                    color = Zinc950.copy(alpha = 0.75f),
                    shape = ShapePill,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    Text(
                        "⭐${item.rating}",
                        color = Brand400,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // 标题
        Text(
            text = title,
            color = Zinc300,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )

        // 副信息（年份/地区）
        if (subTitle.isNotBlank()) {
            Text(
                text = subTitle,
                color = Zinc500,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }
    }
}
