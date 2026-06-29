package com.rannuan.tv.data.model

import com.google.gson.annotations.SerializedName

data class SiteInfo(
    val key: String = "",
    val name: String = "",
    val id: String = ""
)

data class MediaItem(
    @SerializedName("vod_id") val vodId: String = "",
    @SerializedName("vod_name") val vodName: String = "",
    @SerializedName("vod_pic") val vodPic: String? = null,
    @SerializedName("vod_remarks") val vodRemarks: String? = null,
    @SerializedName("type_name") val typeName: String? = null,
    @SerializedName("vod_year") val vodYear: String? = null,
    @SerializedName("vod_area") val vodArea: String? = null,
    @SerializedName("vod_actor") val vodActor: String? = null,
    @SerializedName("vod_content") val vodContent: String? = null,
    @SerializedName("site_key") val siteKey: String = "",
    @SerializedName("site_name") val siteName: String = "",
    val latency: Int = 0,
    val sites: List<SiteInfo>? = null,
    // 豆瓣扩展
    val rating: Double? = null,
    val title: String = "",
    val cover: String? = null,
    val year: String? = null,
    val type: String? = null
)

data class MediaDetail(
    @SerializedName("vod_id") val vodId: String = "",
    @SerializedName("vod_name") val vodName: String = "",
    @SerializedName("vod_pic") val vodPic: String? = null,
    @SerializedName("type_name") val typeName: String? = null,
    @SerializedName("vod_content") val vodContent: String? = null,
    @SerializedName("vod_actor") val vodActor: String? = null,
    @SerializedName("vod_director") val vodDirector: String? = null,
    @SerializedName("vod_year") val vodYear: String? = null,
    @SerializedName("vod_area") val vodArea: String? = null,
    @SerializedName("vod_remarks") val vodRemarks: String? = null,
    @SerializedName("vod_play_from") val vodPlayFrom: String? = null,
    @SerializedName("vod_play_url") val vodPlayUrl: String? = null,
    @SerializedName("site_key") val siteKey: String = "",
    @SerializedName("site_name") val siteName: String = ""
)

data class ApiListResponse<T>(
    val list: List<T> = emptyList()
)

// 首页（豆瓣）响应
data class DoubanHomeData(
    val hot: List<MediaItem> = emptyList(),
    val dianshiju: List<MediaItem> = emptyList(),
    val dianying: List<MediaItem> = emptyList(),
    val zongyi: List<MediaItem> = emptyList(),
    val dongman: List<MediaItem> = emptyList()
)

// 分类响应
data class CategoryResponse(
    val total: Int = 0,
    val page: Int = 1,
    val pageSize: Int = 20,
    val totalPages: Int = 0,
    val list: List<MediaItem> = emptyList(),
    val complete: Boolean = false,
    val warming: Boolean = false,
    val outOfRange: Boolean = false,
    val serverFiltered: Boolean = false,
    val subCounts: Map<String, Int>? = null
)

// 播放源
data class PlaySource(
    val name: String,
    val episodes: List<Episode> = emptyList()
)

data class Episode(
    val title: String,
    val url: String
)
