package com.rannuan.tv.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

/** 桌面端 SplashScreen.tsx 的 Compose 等价实现 */
@Composable
fun SplashScreen(onFinish: () -> Unit) {
    var phase by remember { mutableIntStateOf(0) } // 0=显示, 1=淡出
    val density = LocalDensity.current

    // 无限动画：光束闪烁、发光脉冲
    val infiniteTransition = rememberInfiniteTransition(label = "splash")
    val beamAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(3000, easing = EaseInOutCubic), RepeatMode.Reverse),
        label = "beam"
    )
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOutCubic), RepeatMode.Reverse),
        label = "glow"
    )

    // 初始动画
    val logoScale by animateFloatAsState(
        targetValue = if (phase == 0) 1f else 0.3f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 300f),
        label = "logo"
    )
    val logoRotation by animateFloatAsState(
        targetValue = if (phase == 0) 0f else -15f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 300f),
        label = "logoRotate"
    )

    // 自动终止
    LaunchedEffect(Unit) {
        delay(2600)
        phase = 1
        delay(700)
        onFinish()
    }

    // 整体淡出
    val containerAlpha by animateFloatAsState(
        targetValue = if (phase == 0) 1f else 0f,
        animationSpec = tween(700),
        label = "fadeOut"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080810))
            .alpha(containerAlpha)
    ) {
        // ===== 放映机光束 =====
        // 光源底座光晕
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-4).dp)
                .size(12.dp)
                .scale(glowScale)
                .background(Color(0xFFFFBF24).copy(alpha = 0.3f), CircleShape)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-3).dp)
                .size(8.dp)
                .scale(glowScale * 0.8f)
                .background(Color.White.copy(alpha = 0.4f), CircleShape)
        )

        // 锥形光束（Canvas 绘制）
        Canvas(modifier = Modifier.fillMaxSize().alpha(beamAlpha)) {
            val centerX = size.width / 2
            val bottomY = size.height - with(density) { 32.dp.toPx() }

            // 主光束
            val beamPath = Path().apply {
                moveTo(centerX - 180.dp.toPx(), bottomY)
                lineTo(centerX - 80.dp.toPx(), 0f)
                lineTo(centerX + 80.dp.toPx(), 0f)
                lineTo(centerX + 180.dp.toPx(), bottomY)
                close()
            }
            drawPath(
                beamPath,
                brush = Brush.verticalGradient(
                    listOf(Color.Transparent, Color(0x44FFBF24), Color(0x11FFBF24)),
                    startY = 0f, endY = bottomY
                )
            )

            // 内层窄光束
            val innerPath = Path().apply {
                moveTo(centerX - 120.dp.toPx(), bottomY)
                lineTo(centerX - 40.dp.toPx(), size.height * 0.2f)
                lineTo(centerX + 40.dp.toPx(), size.height * 0.2f)
                lineTo(centerX + 120.dp.toPx(), bottomY)
                close()
            }
            drawPath(
                innerPath,
                brush = Brush.verticalGradient(
                    listOf(Color.Transparent, Color(0x22FFBF24), Color(0x08FFBF24)),
                    startY = 0f, endY = bottomY
                )
            )
        }

        // ===== 胶卷穿孔边 =====
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 12.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            repeat(12) { i ->
                val holeAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f, targetValue = 0.7f,
                    animationSpec = infiniteRepeatable(
                        tween(1500, easing = EaseInOutCubic, delayMillis = i * 100),
                        RepeatMode.Reverse
                    ),
                    label = "hole$i"
                )
                Box(
                    modifier = Modifier
                        .size(12.dp, 10.dp)
                        .alpha(holeAlpha)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0x33FFBF24))
                )
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            repeat(12) { i ->
                val holeAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f, targetValue = 0.7f,
                    animationSpec = infiniteRepeatable(
                        tween(1500, easing = EaseInOutCubic, delayMillis = i * 100 + 500),
                        RepeatMode.Reverse
                    ),
                    label = "holeR$i"
                )
                Box(
                    modifier = Modifier
                        .size(12.dp, 10.dp)
                        .alpha(holeAlpha)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0x33FFBF24))
                )
            }
        }

        // ===== 漂浮影院元素 =====
        val floatingItems = remember {
            listOf(
                "🍿" to Offset(0.20f, 0.15f),
                "🥤" to Offset(0.25f, 0.60f),
                "🎞️" to Offset(0.78f, 0.25f),
                "📽️" to Offset(0.80f, 0.75f),
                "🍿" to Offset(0.12f, 0.45f),
                "🥤" to Offset(0.72f, 0.65f),
                "⭐" to Offset(0.85f, 0.10f),
            )
        }
        floatingItems.forEachIndexed { i, (emoji, offset) ->
            val floatY by infiniteTransition.animateFloat(
                initialValue = 0f, targetValue = -25f,
                animationSpec = infiniteRepeatable(
                    tween(3000 + Random.nextInt(500), easing = EaseInOutSine),
                    RepeatMode.Reverse
                ),
                label = "float$i"
            )
            val floatRot by infiniteTransition.animateFloat(
                initialValue = 0f, targetValue = if (i % 2 == 0) 5f else -5f,
                animationSpec = infiniteRepeatable(
                    tween(3200 + Random.nextInt(500), easing = EaseInOutSine),
                    RepeatMode.Reverse
                ),
                label = "rot$i"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = (offset.x * 100).dp,
                        top = (offset.y * 100).dp
                    )
            ) {
                Text(
                    emoji,
                    fontSize = 24.sp,
                    modifier = Modifier
                        .offset(y = floatY.dp)
                        .rotate(floatRot)
                )
            }
        }

        // ===== 烟雾氛围 =====
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0x08FFBF24), Color(0x02FFBF24), Color.Transparent)
                    )
                )
        )

        // ===== 中央 Logo 区 =====
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo 光晕
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(glowScale)
                    .alpha(0.4f)
                    .clip(CircleShape)
                    .background(Color(0x33FFBF24))
            )

            // App Logo（旋转弹入动画）
            Image(
                painter = painterResource(com.rannuan.tv.R.mipmap.ic_launcher),
                contentDescription = "冉暖TV",
                modifier = Modifier
                    .size(80.dp)
                    .scale(logoScale)
                    .rotate(logoRotation)
                    .clip(RoundedCornerShape(16.dp))
            )
            Spacer(Modifier.height(16.dp))

            // 标题
            Text(
                "冉暖TV",
                color = Color(0xFFFFD68A),
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 8.sp,
                modifier = Modifier.scale(logoScale)
            )

            Spacer(Modifier.height(8.dp))

            // 副标题
            Text(
                "你的私人影院",
                color = Color(0x80FFBF24),
                fontSize = 14.sp,
                letterSpacing = 4.sp
            )

            Spacer(Modifier.height(32.dp))

            // 加载指示线
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(Color(0x1AFFBF24))
            ) {
                val barOffset by infiniteTransition.animateFloat(
                    initialValue = -1f, targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        tween(1500, easing = LinearEasing),
                        RepeatMode.Restart
                    ),
                    label = "bar"
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.3f)
                        .offset(x = (barOffset * 100).dp * 2)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, Color(0x99FFBF24), Color.Transparent)
                            )
                        )
                )
            }
        }
    }
}
