package com.rannuan.tv.ui.util

import android.os.Build
import android.text.Html

/**
 * 将 HTML 内容（含实体、标签）转为干净的纯文本。
 *
 * 使用系统 [Html.fromHtml] 解码，比正则 `<[^>]+>` 更可靠：
 * - 正确解码 `&amp;` `&lt;` `&nbsp;` `&#xxx;` 等所有 HTML 实体
 * - `<br>` `<p>` 转为换行，`&nbsp;` 转为空格
 * - 折叠多余空白，保留段落结构
 */
fun stripHtml(html: String?): String {
    if (html.isNullOrBlank()) return ""
    @Suppress("DEPRECATION")
    val spanned = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT)
    } else {
        Html.fromHtml(html)
    }
    return spanned.toString().trim()
}

// ════════════════════════════════════════════════
// 播放源名称格式化（与桌面端 client.ts formatSourceName 一致）
// ════════════════════════════════════════════════

private val SITE_SOURCE_PREFIX = mapOf(
    "lzzy"  to ("量子" to listOf("liangzi", "lz")),
    "ffzy"  to ("非凡" to listOf("feifan", "ff")),
    "bfzy"  to ("暴风" to listOf("bfzy", "bf")),
    "suoni" to ("索尼" to listOf("suoni", "soni")),
    "bdzy"  to ("百度" to listOf("baidu", "bd"))
)

private val SOURCE_NAME_MAP = mapOf(
    "liangzi" to "量子.线路一",
    "lzm3u8"  to "量子.线路二",
    "feifan"  to "非凡.线路一",
    "ffm3u8"  to "非凡.线路二",
    "bfzym3u8" to "暴风.线路一",
    "sonim3u8" to "索尼.线路一",
    "bdm3u8"  to "百度.线路一"
)

/**
 * 将原始 vodPlayFrom 标识（如 liangzi、lzm3u8）转为友好中文名。
 */
fun formatSourceName(rawName: String, idx: Int, siteKey: String? = null): String {
    // 1. 精确映射
    SOURCE_NAME_MAP[rawName]?.let { return it }

    val lower = rawName.lowercase()
    val isM3u8 = lower.contains("m3u8")
    val tag = if (isM3u8) "m3u8" else "直连"

    // 2. 按站点前缀匹配
    for ((_, cfg) in SITE_SOURCE_PREFIX) {
        val (siteName, prefixes) = cfg
        for (prefix in prefixes) {
            if (lower.startsWith(prefix)) {
                val rest = rawName.removePrefix(prefix)
                    .replace(Regex("^[._-]+"), "")
                    .replace(Regex("(?i)m3u8$"), "")
                val label = if (rest.isBlank()) "线路" else rest
                return "${siteName}.${label}"
            }
        }
    }

    // 3. 知道站点 key → 站点中文名 + 标识
    val siteName = siteKey?.let { SITE_SOURCE_PREFIX[it]?.first }
    val cleanName = rawName.replace(Regex("(?i)\\.?m3u8$"), "").replace(Regex("^[._-]+"), "")
    val displayName = if (cleanName.isBlank()) "线路${idx + 1}" else cleanName

    if (siteName != null) return "${siteName}.${displayName}"

    // 4. 通用回退
    return "${displayName}.${tag}"
}
