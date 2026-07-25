package com.rannuan.tv.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import com.rannuan.tv.BuildConfig
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AppUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val releaseNotes: List<String>,
    val forceUpdate: Boolean,
    val sha256: String,
    val sizeBytes: Long,
    val publishedAt: String
)

data class DownloadProgress(
    val downloadedBytes: Long,
    val totalBytes: Long
) {
    val fraction: Float
        get() = if (totalBytes > 0L) {
            (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
}

class AppUpdateManager(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    suspend fun checkForUpdate(): AppUpdateInfo? = withContext(Dispatchers.IO) {
        val metadataUrl = "${BuildConfig.SERVER_URL.trimEnd('/')}/dataupdate/latest.json"
        val request = Request.Builder()
            .url(metadataUrl)
            .cacheControl(CacheControl.FORCE_NETWORK)
            .header("Accept", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("更新服务返回 ${response.code}")
            }
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) throw IOException("更新信息为空")

            val json = JSONObject(body)
            if (!json.optBoolean("enabled", true)) return@withContext null

            val versionCode = json.optInt("versionCode", 0)
            if (versionCode <= BuildConfig.VERSION_CODE) return@withContext null

            val rawDownloadUrl = json.optString("downloadUrl").trim()
            if (rawDownloadUrl.isBlank()) throw IOException("新版本缺少下载地址")

            val minSupported = json.optInt("minSupportedVersionCode", 0)
            AppUpdateInfo(
                versionCode = versionCode,
                versionName = json.optString("versionName", versionCode.toString()),
                downloadUrl = resolveUrl(rawDownloadUrl),
                releaseNotes = parseReleaseNotes(json.opt("changelog")),
                forceUpdate = json.optBoolean("forceUpdate", false) ||
                    BuildConfig.VERSION_CODE < minSupported,
                sha256 = json.optString("sha256").trim().lowercase(),
                sizeBytes = json.optLong("sizeBytes", 0L).coerceAtLeast(0L),
                publishedAt = json.optString("publishedAt").trim()
            )
        }
    }

    suspend fun downloadUpdate(
        info: AppUpdateInfo,
        onProgress: suspend (DownloadProgress) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(info.downloadUrl).build()
        val updatesDir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir,
            "updates"
        )
        if (!updatesDir.exists() && !updatesDir.mkdirs()) {
            throw IOException("无法创建更新下载目录")
        }

        val safeVersion = info.versionName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val target = File(updatesDir, "RanNuan-TV-$safeVersion.apk")
        val partial = File(updatesDir, "${target.name}.part")
        if (partial.exists()) partial.delete()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("安装包下载失败：${response.code}")
                }
                val body = response.body ?: throw IOException("安装包内容为空")
                val total = body.contentLength().takeIf { it > 0L } ?: info.sizeBytes
                var downloaded = 0L
                var lastReportedAt = 0L

                body.byteStream().use { input ->
                    partial.outputStream().buffered().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            val now = System.currentTimeMillis()
                            if (now - lastReportedAt >= 120L || (total > 0L && downloaded >= total)) {
                                withContext(Dispatchers.Main.immediate) {
                                    onProgress(DownloadProgress(downloaded, total))
                                }
                                lastReportedAt = now
                            }
                        }
                    }
                }
                withContext(Dispatchers.Main.immediate) {
                    onProgress(DownloadProgress(downloaded, total))
                }
            }

            if (!isApkArchive(partial)) {
                throw IOException("下载内容不是有效的 APK 安装包")
            }
            if (info.sha256.isNotBlank()) {
                val actual = sha256(partial)
                if (!actual.equals(info.sha256, ignoreCase = true)) {
                    throw IOException("安装包校验失败，请重新下载")
                }
            }

            if (target.exists() && !target.delete()) {
                throw IOException("无法替换旧安装包")
            }
            if (!partial.renameTo(target)) {
                partial.copyTo(target, overwrite = true)
                partial.delete()
            }
            target
        } catch (error: Throwable) {
            partial.delete()
            throw error
        }
    }

    fun canInstallPackages(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    fun unknownSourcesSettingsIntent(): Intent = Intent(
        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
        Uri.parse("package:${context.packageName}")
    )

    fun installIntent(apk: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apk
        )
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun resolveUrl(value: String): String {
        val url = if (value.startsWith("http://") || value.startsWith("https://")) {
            value
        } else {
            "${BuildConfig.SERVER_URL.trimEnd('/')}/${value.trimStart('/')}"
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw IOException("安装包下载地址无效")
        }
        return url
    }

    private fun parseReleaseNotes(value: Any?): List<String> = when (value) {
        is JSONArray -> buildList {
            for (index in 0 until value.length()) {
                value.optString(index).trim().takeIf { it.isNotEmpty() }?.let(::add)
            }
        }
        is String -> value.lines().map { it.trim() }.filter { it.isNotEmpty() }
        else -> emptyList()
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun isApkArchive(file: File): Boolean {
        if (file.length() < 4L) return false
        file.inputStream().use { input ->
            return input.read() == 0x50 && input.read() == 0x4B
        }
    }
}
