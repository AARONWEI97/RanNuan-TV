package com.rannuan.tv.ui.util

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

/**
 * 搜索历史持久化 — 基于 SharedPreferences，最多存储 10 条。
 */
object SearchHistoryStore {
    private const val PREF_NAME = "rannuan_search"
    private const val KEY_HISTORY = "history"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getHistory(ctx: Context): List<String> {
        val json = prefs(ctx).getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (_: Exception) { emptyList() }
    }

    fun addHistory(ctx: Context, keyword: String) {
        val list = getHistory(ctx).toMutableList()
        list.removeAll { it.equals(keyword, ignoreCase = true) }
        list.add(0, keyword)
        val trimmed = list.take(10)
        val json = JSONArray(trimmed).toString()
        prefs(ctx).edit().putString(KEY_HISTORY, json).apply()
    }

    fun clearHistory(ctx: Context) {
        prefs(ctx).edit().remove(KEY_HISTORY).apply()
    }
}
