package com.rannuan.tv.ui.util

import android.content.Context
import android.content.SharedPreferences

/**
 * 收藏持久化 — 基于 SharedPreferences，以 `siteKey:id` 为 key 存储。
 */
object FavoritesStore {
    private const val PREF_NAME = "rannuan_favorites"
    private const val KEY_FAVORITES = "fav_ids"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun isFavorited(ctx: Context, siteKey: String, id: String): Boolean {
        val ids = prefs(ctx).getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
        return "$siteKey:$id" in ids
    }

    fun toggle(ctx: Context, siteKey: String, id: String): Boolean {
        val p = prefs(ctx)
        val ids = (p.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()).toMutableSet()
        val key = "$siteKey:$id"
        val nowFav = if (key in ids) { ids.remove(key); false } else { ids.add(key); true }
        p.edit().putStringSet(KEY_FAVORITES, ids).apply()
        return nowFav
    }

    fun getFavoriteKeys(ctx: Context): Set<String> =
        prefs(ctx).getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
}
