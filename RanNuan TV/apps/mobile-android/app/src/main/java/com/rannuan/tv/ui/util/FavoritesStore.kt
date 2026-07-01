package com.rannuan.tv.ui.util

import android.content.Context
import android.content.SharedPreferences
import com.rannuan.tv.data.model.MediaDetail
import org.json.JSONObject

/**
 * 收藏持久化 — 基于 SharedPreferences，以 `siteKey:id` 为 key 存储。
 */
object FavoritesStore {
    private const val PREF_NAME = "rannuan_favorites"
    private const val KEY_FAVORITES = "fav_ids"
    private const val KEY_DETAILS = "fav_details"

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
        val nowFav = if (key in ids) {
            ids.remove(key)
            removeSnapshot(ctx, key)
            false
        } else {
            ids.add(key)
            true
        }
        p.edit().putStringSet(KEY_FAVORITES, ids).apply()
        return nowFav
    }

    fun toggle(ctx: Context, detail: MediaDetail): Boolean {
        val nowFav = toggle(ctx, detail.siteKey, detail.vodId)
        if (nowFav) saveSnapshot(ctx, detail)
        return nowFav
    }

    fun getFavoriteKeys(ctx: Context): Set<String> =
        prefs(ctx).getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()

    fun saveSnapshot(ctx: Context, detail: MediaDetail) {
        if (detail.siteKey.isBlank() || detail.vodId.isBlank()) return
        val key = "${detail.siteKey}:${detail.vodId}"
        val root = readSnapshotRoot(ctx)
        root.put(key, JSONObject().apply {
            put("vodId", detail.vodId)
            put("vodName", detail.vodName)
            put("vodPic", detail.vodPic ?: "")
            put("typeName", detail.typeName ?: "")
            put("vodActor", detail.vodActor ?: "")
            put("vodDirector", detail.vodDirector ?: "")
            put("vodYear", detail.vodYear ?: "")
            put("vodArea", detail.vodArea ?: "")
            put("vodRemarks", detail.vodRemarks ?: "")
            put("siteKey", detail.siteKey)
            put("siteName", detail.siteName)
        })
        prefs(ctx).edit().putString(KEY_DETAILS, root.toString()).apply()
    }

    fun getFavoriteSnapshots(ctx: Context): List<MediaDetail> {
        val root = readSnapshotRoot(ctx)
        return getFavoriteKeys(ctx).mapNotNull { key ->
            root.optJSONObject(key)?.toMediaDetail()
        }
    }

    private fun removeSnapshot(ctx: Context, key: String) {
        val root = readSnapshotRoot(ctx)
        root.remove(key)
        prefs(ctx).edit().putString(KEY_DETAILS, root.toString()).apply()
    }

    private fun readSnapshotRoot(ctx: Context): JSONObject {
        val raw = prefs(ctx).getString(KEY_DETAILS, "{}") ?: "{}"
        return runCatching { JSONObject(raw) }.getOrElse { JSONObject() }
    }

    private fun JSONObject.toMediaDetail(): MediaDetail =
        MediaDetail(
            vodId = optString("vodId"),
            vodName = optString("vodName"),
            vodPic = optString("vodPic").takeIf { it.isNotBlank() },
            typeName = optString("typeName").takeIf { it.isNotBlank() },
            vodActor = optString("vodActor").takeIf { it.isNotBlank() },
            vodDirector = optString("vodDirector").takeIf { it.isNotBlank() },
            vodYear = optString("vodYear").takeIf { it.isNotBlank() },
            vodArea = optString("vodArea").takeIf { it.isNotBlank() },
            vodRemarks = optString("vodRemarks").takeIf { it.isNotBlank() },
            siteKey = optString("siteKey"),
            siteName = optString("siteName")
        )
}
