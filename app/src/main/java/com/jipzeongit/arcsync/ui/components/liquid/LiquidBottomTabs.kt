package com.jipzeongit.arcsync.ui.components.liquid

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule

@Composable
fun LiquidBottomTabs(
    selectedTabIndex: () -> Int,
    onTabSelected: (index: Int) -> Unit,
    backdrop: LayerBackdrop,
    tabsCount: Int,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val isLightTheme = !isSystemInDarkTheme()
    val containerColor = if (isLightTheme) {
        Color(0xFFFAFAFA).copy(alpha = 0.4f)
    } else {
        Color(0xFF121212).copy(alpha = 0.4f)
    }

    BoxWithConstraints(
        modifier,
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current
        val maxWidthPx = constraints.maxWidth.toFloat()
        val horizontalPaddingPx = maxWidthPx * 0.25f
        val horizontalPaddingDp = with(density) { horizontalPaddingPx.toDp() }
        val tabWidthPx = (maxWidthPx - horizontalPaddingPx * 2) / tabsCount

        Box(
            modifier = Modifier
                .padding(horizontal = horizontalPaddingDp)
        ) {
            // 背景层 - 透明模糊，固定高度
            Box(
                Modifier
                    .height(60.dp)
                    .fillMaxWidth()
                    .alpha(0f)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { Capsule() },
                        effects = {
                            blur(12f.dp.toPx())
                        },
                        onDrawSurface = { drawRect(containerColor) }
                    )
            )

            // 内容层 - 正常显示图标和文字
            Row(
                Modifier
                    .height(60.dp)
                    .fillMaxWidth()
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )

            // 选中指示器 - 使用 backdrop
            Box(
                Modifier
                    .padding(4.dp)
                    .graphicsLayer {
                        translationX = selectedTabIndex().toFloat() * tabWidthPx
                    }
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { Capsule() },
                        effects = {
                            blur(8f.dp.toPx())
                        },
                        highlight = { Highlight.Default.copy(alpha = 0.5f) },
                        shadow = { Shadow(alpha = 0.3f) },
                        onDrawSurface = {
                            drawRect(
                                if (isLightTheme) Color.Black.copy(0.08f)
                                else Color.White.copy(0.08f)
                            )
                        }
                    )
                    .height(52.dp)
                    .fillMaxWidth(1f / tabsCount)
            )
        }
    }
}
