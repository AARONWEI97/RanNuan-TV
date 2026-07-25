package com.rannuan.tv.ui.update

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SystemUpdateAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rannuan.tv.BuildConfig
import com.rannuan.tv.R
import com.rannuan.tv.ui.theme.Brand300
import com.rannuan.tv.ui.theme.Brand400
import com.rannuan.tv.ui.theme.Brand500
import com.rannuan.tv.ui.theme.Error500
import com.rannuan.tv.ui.theme.Zinc300
import com.rannuan.tv.ui.theme.Zinc400
import com.rannuan.tv.ui.theme.Zinc500
import com.rannuan.tv.ui.theme.Zinc800
import com.rannuan.tv.ui.theme.Zinc900
import com.rannuan.tv.update.AppUpdateInfo
import com.rannuan.tv.update.AppUpdateManager
import com.rannuan.tv.update.DownloadProgress
import java.io.File
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private sealed interface UpdateUiState {
    data object Hidden : UpdateUiState
    data class Available(val info: AppUpdateInfo) : UpdateUiState
    data class Downloading(val info: AppUpdateInfo, val progress: DownloadProgress) : UpdateUiState
    data class PermissionRequired(val info: AppUpdateInfo, val apk: File) : UpdateUiState
    data class Failed(val info: AppUpdateInfo, val message: String) : UpdateUiState
}

@Composable
fun AppUpdateOverlay() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val manager = remember(context) { AppUpdateManager(context.applicationContext) }
    var state by remember { mutableStateOf<UpdateUiState>(UpdateUiState.Hidden) }
    var pendingInstall by remember { mutableStateOf<Pair<AppUpdateInfo, File>?>(null) }
    var downloadJob by remember { mutableStateOf<Job?>(null) }

    val unknownSourcesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        val pending = pendingInstall ?: return@rememberLauncherForActivityResult
        if (manager.canInstallPackages()) {
            try {
                context.startActivity(manager.installIntent(pending.second))
                state = UpdateUiState.Hidden
            } catch (_: ActivityNotFoundException) {
                state = UpdateUiState.Failed(pending.first, "系统中没有可用的安装器")
            } catch (error: Exception) {
                state = UpdateUiState.Failed(pending.first, error.message ?: "无法打开安装器")
            }
        } else {
            state = UpdateUiState.Failed(pending.first, "未获得安装应用权限")
        }
    }

    fun openInstaller(info: AppUpdateInfo, apk: File) {
        pendingInstall = info to apk
        if (manager.canInstallPackages()) {
            try {
                context.startActivity(manager.installIntent(apk))
                state = UpdateUiState.Hidden
            } catch (_: ActivityNotFoundException) {
                state = UpdateUiState.Failed(info, "系统中没有可用的安装器")
            } catch (error: Exception) {
                state = UpdateUiState.Failed(info, error.message ?: "无法打开安装器")
            }
        } else {
            state = UpdateUiState.PermissionRequired(info, apk)
        }
    }

    fun startDownload(info: AppUpdateInfo) {
        downloadJob?.cancel()
        downloadJob = scope.launch {
            state = UpdateUiState.Downloading(info, DownloadProgress(0L, info.sizeBytes))
            try {
                val apk = manager.downloadUpdate(info) { progress ->
                    state = UpdateUiState.Downloading(info, progress)
                }
                openInstaller(info, apk)
            } catch (_: CancellationException) {
                state = UpdateUiState.Available(info)
            } catch (error: Exception) {
                state = UpdateUiState.Failed(info, error.message ?: "下载更新失败")
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            manager.checkForUpdate()?.let { state = UpdateUiState.Available(it) }
        } catch (_: Exception) {
            // 自动检查静默失败，避免网络波动打断用户进入 App。
        }
    }

    DisposableEffect(Unit) {
        onDispose { downloadJob?.cancel() }
    }

    when (val current = state) {
        UpdateUiState.Hidden -> Unit
        is UpdateUiState.Available -> UpdateAvailableDialog(
            info = current.info,
            onDismiss = { if (!current.info.forceUpdate) state = UpdateUiState.Hidden },
            onUpdate = { startDownload(current.info) }
        )
        is UpdateUiState.Downloading -> UpdateDownloadingDialog(
            info = current.info,
            progress = current.progress,
            onCancel = {
                if (!current.info.forceUpdate) downloadJob?.cancel()
            }
        )
        is UpdateUiState.PermissionRequired -> InstallPermissionDialog(
            info = current.info,
            onDismiss = { if (!current.info.forceUpdate) state = UpdateUiState.Hidden },
            onAuthorize = {
                try {
                    unknownSourcesLauncher.launch(manager.unknownSourcesSettingsIntent())
                } catch (_: ActivityNotFoundException) {
                    state = UpdateUiState.Failed(current.info, "无法打开安装权限设置")
                }
            }
        )
        is UpdateUiState.Failed -> UpdateFailedDialog(
            info = current.info,
            message = current.message,
            onDismiss = { if (!current.info.forceUpdate) state = UpdateUiState.Hidden },
            onRetry = { startDownload(current.info) }
        )
    }
}

@Composable
private fun UpdateAvailableDialog(
    info: AppUpdateInfo,
    onDismiss: () -> Unit,
    onUpdate: () -> Unit
) {
    UpdateDialogFrame(forceUpdate = info.forceUpdate, onDismiss = onDismiss) {
        UpdateDialogHeader(
            icon = Icons.Outlined.SystemUpdateAlt,
            title = "发现新版本",
            subtitle = "RanNuan TV  v${info.versionName}",
            showAppLogo = true
        )

        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetaLabel("当前 v${BuildConfig.VERSION_NAME}")
            if (info.sizeBytes > 0L) MetaLabel(formatBytes(info.sizeBytes))
            if (info.forceUpdate) MetaLabel("重要更新", highlighted = true)
        }

        if (info.releaseNotes.isNotEmpty()) {
            Spacer(Modifier.height(22.dp))
            Text("本次更新", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            Column(
                modifier = Modifier.heightIn(max = 210.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                info.releaseNotes.forEach { note ->
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            Modifier
                                .padding(top = 7.dp)
                                .size(5.dp)
                                .background(Brand400, CircleShape)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(note, color = Zinc300, fontSize = 13.sp, lineHeight = 20.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        PrimaryUpdateButton(text = "立即更新", icon = Icons.Outlined.Download, onClick = onUpdate)
        if (!info.forceUpdate) {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("稍后再说", color = Zinc500, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun UpdateDownloadingDialog(
    info: AppUpdateInfo,
    progress: DownloadProgress,
    onCancel: () -> Unit
) {
    UpdateDialogFrame(forceUpdate = true, onDismiss = {}) {
        UpdateDialogHeader(
            icon = Icons.Outlined.Download,
            title = "正在下载更新",
            subtitle = "v${info.versionName}"
        )
        Spacer(Modifier.height(28.dp))

        if (progress.totalBytes > 0L) {
            LinearProgressIndicator(
                progress = { progress.fraction },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = Brand400,
                trackColor = Zinc800
            )
        } else {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = Brand400,
                trackColor = Zinc800
            )
        }

        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                if (progress.totalBytes > 0L) {
                    "${formatBytes(progress.downloadedBytes)} / ${formatBytes(progress.totalBytes)}"
                } else {
                    formatBytes(progress.downloadedBytes)
                },
                color = Zinc400,
                fontSize = 12.sp
            )
            Text(
                if (progress.totalBytes > 0L) "${(progress.fraction * 100).toInt()}%" else "下载中",
                color = Brand300,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "下载完成后将自动打开系统安装界面",
            color = Zinc500,
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth()
        )
        if (!info.forceUpdate) {
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                Text("取消下载", color = Zinc400)
            }
        }
    }
}

@Composable
private fun InstallPermissionDialog(
    info: AppUpdateInfo,
    onDismiss: () -> Unit,
    onAuthorize: () -> Unit
) {
    UpdateDialogFrame(forceUpdate = info.forceUpdate, onDismiss = onDismiss) {
        UpdateDialogHeader(
            icon = Icons.Outlined.Security,
            title = "允许安装更新",
            subtitle = "安装包已经下载并校验完成"
        )
        Spacer(Modifier.height(20.dp))
        Text(
            "Android 需要你允许 RanNuan TV 安装应用。授权后返回这里，会自动打开安装界面。",
            color = Zinc300,
            fontSize = 13.sp,
            lineHeight = 21.sp
        )
        Spacer(Modifier.height(24.dp))
        PrimaryUpdateButton(text = "前往授权", icon = Icons.Outlined.Security, onClick = onAuthorize)
        if (!info.forceUpdate) {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("稍后安装", color = Zinc500)
            }
        }
    }
}

@Composable
private fun UpdateFailedDialog(
    info: AppUpdateInfo,
    message: String,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    UpdateDialogFrame(forceUpdate = info.forceUpdate, onDismiss = onDismiss) {
        UpdateDialogHeader(
            icon = Icons.Outlined.ErrorOutline,
            title = "更新没有完成",
            subtitle = "请检查网络后重试",
            error = true
        )
        Spacer(Modifier.height(20.dp))
        Text(
            message,
            color = Zinc300,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(24.dp))
        PrimaryUpdateButton(text = "重新下载", icon = Icons.Outlined.Download, onClick = onRetry)
        if (!info.forceUpdate) {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("关闭", color = Zinc500)
            }
        }
    }
}

@Composable
private fun UpdateDialogFrame(
    forceUpdate: Boolean,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = { if (!forceUpdate) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !forceUpdate,
            dismissOnClickOutside = !forceUpdate,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.88f).widthIn(max = 440.dp),
            color = Zinc900,
            shape = RoundedCornerShape(8.dp),
            tonalElevation = 0.dp,
            shadowElevation = 18.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 24.dp), content = content)
        }
    }
}

@Composable
private fun UpdateDialogHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    error: Boolean = false,
    showAppLogo: Boolean = false
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    if (error) Error500.copy(alpha = 0.14f) else Brand500.copy(alpha = 0.16f),
                    RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (showAppLogo) {
                Image(
                    painter = painterResource(R.mipmap.ic_launcher),
                    contentDescription = "RanNuan TV",
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                )
            } else {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (error) Error500 else Brand400,
                    modifier = Modifier.size(25.dp)
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, color = Zinc400, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun MetaLabel(text: String, highlighted: Boolean = false) {
    Surface(
        color = if (highlighted) Brand500.copy(alpha = 0.14f) else Zinc800,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text,
            color = if (highlighted) Brand300 else Zinc400,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun PrimaryUpdateButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(46.dp),
        shape = RoundedCornerShape(7.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Brand500, contentColor = Color.White),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1.0) {
        String.format(Locale.getDefault(), "%.1f MB", mb)
    } else {
        String.format(Locale.getDefault(), "%.0f KB", bytes / 1024.0)
    }
}
