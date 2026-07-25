package com.rannuan.tv.ui.screens.profile

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rannuan.tv.BuildConfig
import com.rannuan.tv.R
import com.rannuan.tv.ui.theme.Zinc300
import com.rannuan.tv.ui.theme.Zinc400
import com.rannuan.tv.ui.theme.Zinc500
import com.rannuan.tv.ui.theme.Zinc900

private const val PROJECT_GITHUB_URL = "https://github.com/AARONWEI97/RanNuan-TV"

@Composable
fun DonationTab() {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(R.mipmap.ic_launcher),
                    contentDescription = "冉暖TV",
                    modifier = Modifier.size(66.dp).clip(RoundedCornerShape(8.dp))
                )
                Spacer(Modifier.height(13.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.LocalCafe,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.size(7.dp))
                    Text("请作者喝杯咖啡", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "永久免费、无广告、无内购。你的支持会用于服务器、带宽和资源站接口的持续维护。",
                    color = Zinc400,
                    fontSize = 13.sp,
                    lineHeight = 21.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }

        item {
            Surface(
                onClick = { openGithub(context) },
                modifier = Modifier.fillMaxWidth(),
                color = Zinc900,
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).background(Color(0xFFFBBF24).copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.StarBorder,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.size(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("去 GitHub 点亮 Star", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("github.com/AARONWEI97/RanNuan-TV", color = Zinc500, fontSize = 11.sp)
                    }
                }
            }
        }

        item {
            DonationQrCard(
                title = "微信赞赏",
                subtitle = "使用微信扫描二维码",
                image = R.drawable.donate_wechat,
                accent = Color(0xFF34D399)
            )
        }
        item {
            DonationQrCard(
                title = "支付宝",
                subtitle = "使用支付宝扫描二维码",
                image = R.drawable.donate_alipay,
                accent = Color(0xFF38BDF8)
            )
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Zinc900,
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            tint = Color(0xFFFB7185),
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(Modifier.size(8.dp))
                        Text("为什么需要支持", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                    SupportReason("永久免费", "不设会员、不加广告，所有功能完整开放。")
                    SupportReason("服务器与带宽", "聚合接口、图片代理和服务端缓存均有持续成本。")
                    SupportReason("持续维护", "资源站经常变化，需要长期跟进、修复与优化。")
                }
            }
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 82.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("无论是否捐赠，感谢你的使用与反馈", color = Zinc500, fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                Text("v${BuildConfig.VERSION_NAME}", color = Zinc500.copy(alpha = 0.7f), fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun DonationQrCard(
    title: String,
    subtitle: String,
    @DrawableRes image: Int,
    accent: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Zinc900,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(accent, CircleShape))
                Spacer(Modifier.size(8.dp))
                Column {
                    Text(title, color = Zinc300, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(subtitle, color = Zinc500, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(15.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.68f)
                    .aspectRatio(1f)
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(image),
                    contentDescription = "$title 二维码",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun SupportReason(title: String, description: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier.padding(top = 7.dp).size(5.dp).background(Color(0xFFFB7185), CircleShape)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Zinc300, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(description, color = Zinc500, fontSize = 12.sp, lineHeight = 19.sp)
        }
    }
}

private fun openGithub(context: Context) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PROJECT_GITHUB_URL)))
    }
}
