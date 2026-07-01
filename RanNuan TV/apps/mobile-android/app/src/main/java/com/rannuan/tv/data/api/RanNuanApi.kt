package com.rannuan.tv.data.api

import com.rannuan.tv.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import retrofit2.http.Streaming
import com.rannuan.tv.data.model.*
import java.util.concurrent.TimeUnit

interface RanNuanApi {

    // ========== 首页 ==========
    @GET("/api/douban/home")
    suspend fun getDoubanHome(): DoubanHomeData

    @GET("/api/home")
    suspend fun getHome(): HomeData

    // ========== 搜索 ==========
    @GET("/api/search")
    suspend fun search(@Query("wd") query: String): ApiListResponse<MediaItem>

    @Streaming
    @POST("/api/search-stream")
    suspend fun searchStream(@Body body: SearchStreamRequest): okhttp3.ResponseBody

    // ========== 分类 ==========
    @POST("/api/category")
    suspend fun getCategory(@Body body: CategoryRequest): CategoryResponse

    // ========== 详情 ==========
    @GET("/api/multi-detail")
    suspend fun getMultiDetail(
        @Query("wd") wd: String = "",
        @Query("keys") keys: String = ""
    ): ApiListResponse<MediaDetail>

    // ========== 图片代理 ==========
    @GET("/api/img")
    suspend fun proxyImage(@Query("url") url: String): okhttp3.ResponseBody

    // ========== 视频代理 ==========
    @GET("/api/proxy")
    suspend fun proxyVideo(@Query("url") url: String): okhttp3.ResponseBody

    // ========== 豆瓣推荐 ==========
    @GET("/api/douban/recommend")
    suspend fun getDoubanRecommend(
        @Query("type") type: String = "movie",
        @Query("tag") tag: String = "热门",
        @Query("limit") limit: Int = 20
    ): List<MediaItem>

    companion object {
        fun create(): RanNuanApi {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(BuildConfig.SERVER_URL + "/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(RanNuanApi::class.java)
        }
    }
}

data class HomeData(
    val hot: List<MediaItem> = emptyList(),
    val dianshiju: List<MediaItem> = emptyList(),
    val dianying: List<MediaItem> = emptyList(),
    val zongyi: List<MediaItem> = emptyList(),
    val dongman: List<MediaItem> = emptyList(),
    val duanju: List<MediaItem> = emptyList()
)

data class CategoryRequest(
    val category: String,
    val page: Int = 1,
    val pageSize: Int = 20,
    val subType: String? = null
)

data class SearchStreamRequest(
    val wd: String
)
