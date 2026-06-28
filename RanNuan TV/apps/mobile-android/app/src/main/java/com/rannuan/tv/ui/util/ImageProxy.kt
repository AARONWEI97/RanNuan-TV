package com.rannuan.tv.ui.util

import com.rannuan.tv.BuildConfig

object ImageProxy {
    private val baseUrl get() = BuildConfig.SERVER_URL

    fun proxy(url: String?): String {
        if (url.isNullOrBlank()) return ""
        if (url.contains("/api/img")) return url
        if (url.contains("bfzy") || url.contains("picbf") ||
            url.contains("doubanio.com") || url.contains("douban.com")
        ) {
            return "$baseUrl/api/img?url=${java.net.URLEncoder.encode(url, "UTF-8")}"
        }
        return url
    }
}
