package com.jipzeongit.arcsync.ui.components.liquid

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
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
        Color(0xFFEEEEEE).copy(alpha = 0.85f)
    } else {
        Color(0xFF1E1E1E).copy(alpha = 0.85f)
    }

    BoxWithConstraints(
        modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        // 容器 - 简化版，不使用 Backdrop 效果
        Row(
            Modifier
                .clip(Capsule())
                .background(containerColor)
                .height(56f.dp)
                .fillMaxWidth()
                .padding(horizontal = 4f.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )

        // 选中指示器
        Box(
            Modifier
                .padding(horizontal = 4f.dp)
                .graphicsLayer {
                    translationX = selectedTabIndex().toFloat() * (constraints.maxWidth.toFloat() / tabsCount)
                }
                .clip(Capsule())
                .background(
                    if (isLightTheme) Color.White.copy(alpha = 0.7f)
                    else Color(0xFF333333).copy(alpha = 0.7f)
                )
                .height(56f.dp)
                .fillMaxWidth(1f / tabsCount)
        )
    }
}
