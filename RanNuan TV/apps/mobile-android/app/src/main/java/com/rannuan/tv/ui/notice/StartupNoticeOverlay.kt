package com.rannuan.tv.ui.notice

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rannuan.tv.BuildConfig
import com.rannuan.tv.R
import com.rannuan.tv.ui.theme.Brand500
import com.rannuan.tv.ui.theme.Zinc300
import com.rannuan.tv.ui.theme.Zinc400
import com.rannuan.tv.ui.theme.Zinc500
import com.rannuan.tv.ui.theme.Zinc800
import com.rannuan.tv.ui.theme.Zinc900

private const val NOTICE_PREFS = "rannuan_startup_notice"
private const val CONFIRMED_VERSION = "confirmed_version_code"
private const val PROJECT_GITHUB_URL = "https://github.com/AARONWEI97/RanNuan-TV"

private enum class NoticeStage { Disclaimer, Donation, Done }

@Composable
fun StartupNoticeOverlay(
    onFinished: () -> Unit,
    onOpenDonation: () -> Unit
) {
    val context = LocalContext.current
    val preferences = remember(context) {
        context.getSharedPreferences(NOTICE_PREFS, Context.MODE_PRIVATE)
    }
    var stage by remember {
        mutableStateOf(
            if (preferences.getInt(CONFIRMED_VERSION, -1) == BuildConfig.VERSION_CODE) {
                NoticeStage.Done
            } else {
                NoticeStage.Disclaimer
            }
        )
    }

    fun completeNotices(openDonation: Boolean) {
        preferences.edit().putInt(CONFIRMED_VERSION, BuildConfig.VERSION_CODE).apply()
        stage = NoticeStage.Done
        if (openDonation) onOpenDonation()
    }

    LaunchedEffect(stage) {
        if (stage == NoticeStage.Done) onFinished()
    }

    when (stage) {
        NoticeStage.Disclaimer -> DisclaimerDialog(
            onAgree = { stage = NoticeStage.Donation },
            onExit = { (context as? Activity)?.finishAffinity() }
        )

        NoticeStage.Donation -> DonationDialog(
            onClose = { completeNotices(openDonation = false) },
            onOpenDonation = { completeNotices(openDonation = true) },
            onOpenGithub = { openExternalLink(context, PROJECT_GITHUB_URL) }
        )

        NoticeStage.Done -> Unit
    }
}

@Composable
private fun DisclaimerDialog(onAgree: () -> Unit, onExit: () -> Unit) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f).heightIn(max = 680.dp),
            color = Zinc900,
            shape = RoundedCornerShape(8.dp),
            shadowElevation = 20.dp
        ) {
            Column {
                NoticeHeader(
                    title = "免责声明",
                    subtitle = "首次使用或版本更新后展示",
                    icon = {
                        Icon(
                            Icons.Outlined.Security,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 430.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 22.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(13.dp)
                ) {
                    Text(
                        "冉暖TV 是一款基于第三方公开影视资源接口的视频聚合播放工具，仅供个人学习、研究和技术交流使用。",
                        color = Zinc300,
                        fontSize = 13.sp,
                        lineHeight = 22.sp
                    )
                    DisclaimerClause(
                        1,
                        "本应用不存储、不上传、不分发任何视频、音频或图片内容。所有影视资源来自第三方公开接口，本应用不对第三方内容的合法性、准确性和完整性负责。"
                    )
                    DisclaimerClause(
                        2,
                        "本应用仅提供资源链接的聚合与播放功能，不参与资源的采集、存储与传播。所有内容版权归原作者或原平台所有。"
                    )
                    DisclaimerClause(
                        3,
                        "用户应遵守所在国家或地区的法律法规，不得将本应用用于商业用途或违法违规用途。因使用不当产生的后果由用户自行承担。"
                    )
                    DisclaimerClause(
                        4,
                        "若相关内容侵犯您的合法权益，请提供权属证明联系开发者，我们将在确认后及时处理。"
                    )
                    Text(
                        "继续使用即表示您已阅读、理解并同意以上全部内容。",
                        color = Zinc400,
                        fontSize = 12.sp,
                        lineHeight = 20.sp
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.025f))
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onExit) {
                        Text("退出应用", color = Zinc400)
                    }
                    Spacer(Modifier.size(8.dp))
                    Button(
                        onClick = onAgree,
                        colors = ButtonDefaults.buttonColors(containerColor = Brand500),
                        shape = RoundedCornerShape(7.dp)
                    ) {
                        Text("我已阅读并同意", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DisclaimerClause(number: Int, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(Color(0xFFF59E0B).copy(alpha = 0.14f), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(number.toString(), color = Color(0xFFFBBF24), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            text,
            color = Zinc300,
            fontSize = 13.sp,
            lineHeight = 22.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DonationDialog(
    onClose: () -> Unit,
    onOpenDonation: () -> Unit,
    onOpenGithub: () -> Unit
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.88f),
            color = Zinc900,
            shape = RoundedCornerShape(8.dp),
            shadowElevation = 20.dp
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFB7185).copy(alpha = 0.10f))
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .background(Zinc900.copy(alpha = 0.88f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.mipmap.ic_launcher),
                            contentDescription = "冉暖TV",
                            modifier = Modifier.size(58.dp).clip(RoundedCornerShape(8.dp))
                        )
                    }
                }

                Column(
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            tint = Color(0xFFFB7185),
                            modifier = Modifier.size(21.dp)
                        )
                        Spacer(Modifier.size(8.dp))
                        Text("支持一下开发者", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "冉暖TV 永久免费、无广告、无内购。\n如果用得顺手，欢迎请作者喝杯咖啡，也请在 GitHub 为项目点亮一个 Star。",
                        color = Zinc400,
                        fontSize = 13.sp,
                        lineHeight = 22.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = onOpenGithub,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        shape = RoundedCornerShape(7.dp)
                    ) {
                        Icon(Icons.Outlined.StarBorder, contentDescription = null, tint = Color(0xFFFBBF24))
                        Spacer(Modifier.size(7.dp))
                        Text("去 GitHub 点亮 Star", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        "github.com/AARONWEI97/RanNuan-TV",
                        color = Zinc500,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 5.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onClose,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Zinc400),
                            shape = RoundedCornerShape(7.dp)
                        ) {
                            Text("下次再说", fontSize = 13.sp)
                        }
                        Button(
                            onClick = onOpenDonation,
                            modifier = Modifier.weight(1.35f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE85D75)),
                            shape = RoundedCornerShape(7.dp)
                        ) {
                            Icon(Icons.Outlined.LocalCafe, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.size(6.dp))
                            Text("支持开发者", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

private fun openExternalLink(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}

@Composable
private fun NoticeHeader(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.02f))
            .padding(horizontal = 22.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(46.dp).background(Zinc800, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Spacer(Modifier.size(13.dp))
        Column {
            Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = Zinc500, fontSize = 12.sp)
        }
    }
}
