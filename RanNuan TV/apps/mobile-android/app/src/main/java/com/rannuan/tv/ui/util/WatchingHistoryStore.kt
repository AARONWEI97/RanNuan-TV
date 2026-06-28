package com.rannuan.tv.ui.util

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * 观看历史持久化 — 基于 SharedPreferences，最多 50 条。
 */
object WatchingHistoryStore {
    private const val PREF_NAME = "rannuan_history"
    private const val KEY_HISTORY = "watch_list"

    data class HistoryItem(
        val siteKey: String,
        val id: String,
        val title: String,
        val cover: String,
        val episode: String,
        val timestamp: Long,
        val position: Long = 0L   // 播放位置（毫秒），用于续播
    )

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getHistory(ctx: Context): List<HistoryItem> {
        val json = prefs(ctx).getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                HistoryItem(
                    siteKey = obj.optString("siteKey"),
                    id = obj.optString("id"),
                    title = obj.optString("title"),
                    cover = obj.optString("cover"),
                    episode = obj.optString("episode"),
                    timestamp = obj.optLong("timestamp"),
                    position = obj.optLong("position")
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    fun addHistory(
        ctx: Context,
        siteKey: String, id: String,
        title: String, cover: String, episode: String,
        position: Long = 0L
    ) {
        val list = getHistory(ctx).toMutableList()
        list.removeAll { it.siteKey == siteKey && it.id == id }
        list.add(0, HistoryItem(siteKey, id, title, cover, episode, System.currentTimeMillis(), position))
        val trimmed = list.take(50)
        val json = JSONArray(trimmed.map { item ->
            JSONObject().apply {
                put("siteKey", item.siteKey)
                put("id", item.id)
                put("title", item.title)
                put("cover", item.cover)
                put("episode", item.episode)
                put("timestamp", item.timestamp)
                put("position", item.position)
            }
        }).toString()
        prefs(ctx).edit().putString(KEY_HISTORY, json).apply()
    }

    /** 仅更新最近一条记录的播放位置（不改变时间戳），用于实时保存进度 */
    fun updatePosition(
        ctx: Context,
        siteKey: String, id: String,
        position: Long
    ) {
        val list = getHistory(ctx).toMutableList()
        val idx = list.indexOfFirst { it.siteKey == siteKey && it.id == id }
        if (idx < 0) return
        val old = list[idx]
        list[idx] = old.copy(position = position)
        val json = JSONArray(list.map { item ->
            JSONObject().apply {
                put("siteKey", item.siteKey)
                put("id", item.id)
                put("title", item.title)
                put("cover", item.cover)
                put("episode", item.episode)
                put("timestamp", item.timestamp)
                put("position", item.position)
            }
        }).toString()
        prefs(ctx).edit().putString(KEY_HISTORY, json).apply()
    }

    fun clearHistory(ctx: Context) {
        prefs(ctx).edit().remove(KEY_HISTORY).apply()
    }
}
