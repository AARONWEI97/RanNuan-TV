package com.rannuan.tv.ui.theme.component

import android.graphics.RenderEffect
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rannuan.tv.ui.theme.GlassMedium
import com.rannuan.tv.ui.theme.GlassStroke

/**
 * 液态玻璃卡片（Apple Liquid Glass 风格）。
 *
 * 分层渲染 —— 背景层 blur + 半透明色，前景内容保持清晰：
 * - 底层 Box（先绘制）：用 matchParentSize() 铺满，负责 blur + 底色 + 光泽 + 描边
 * - 顶层 content（后绘制）：自然 wrapContent 决定尺寸，文字/图标/按钮完全清晰
 *
 * 外层 Box 的尺寸由 content 自然撑开，底层 matchParentSize() 跟随。
 *
 * - API ≥ 31：RenderEffect.createBlurEffect（真实毛玻璃）
 * - API < 31：退化为纯半透明底色（无 blur）
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    elevation: Dp = 0.dp,
    cornerRadius: Dp = 14.dp,
    tint: Color = GlassMedium,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Box(modifier = modifier.shadow(elevation = elevation, shape = shape, clip = false)) {
        // ── 底层：模糊背景 + 半透明底色 + 光泽渐变 + 描边 ──
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .then(
                    if (canBlur) {
                        Modifier.graphicsLayer {
                            renderEffect = RenderEffect
                                .createBlurEffect(25f, 25f, android.graphics.Shader.TileMode.CLAMP)
                                .asComposeRenderEffect()
                        }
                    } else {
                        Modifier
                    }
                )
                .background(tint)
                // 液态玻璃顶部光泽
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.08f),
                            Color.Transparent,
                            Color.Transparent,
                            Color.White.copy(alpha = 0.02f)
                        )
                    )
                )
                .border(
                    width = 0.5.dp,
                    color = GlassStroke,
                    shape = shape
                )
                // 底层不接收触摸事件，全部穿透到 content
        )

        // ── 前景：清晰内容（自然 wrapContent 撑开外层 Box 尺寸）──
        content()
    }
}
