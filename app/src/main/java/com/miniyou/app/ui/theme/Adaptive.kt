package com.miniyou.app.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 针对折叠屏（如 OPPO Find N5：外屏约 380dp 宽，内屏约 850dp 宽）的布局档位。
 * 宽度 >= 600dp 视为展开态，使用侧边导航与多列布局。
 */
data class WindowLayout(val widthDp: Int, val heightDp: Int) {
    val expanded: Boolean get() = widthDp >= 600
    val gridColumns: Int get() = when {
        widthDp >= 1000 -> 3
        widthDp >= 600 -> 2
        else -> 1
    }
    val horizontalPadding: Dp get() = if (expanded) 24.dp else 16.dp
}

val LocalWindowLayout = compositionLocalOf { WindowLayout(400, 800) }

/** 宽屏下把内容限制在可读宽度内并居中。 */
@Composable
fun ContentWidth(
    maxWidth: Dp = 760.dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = maxWidth).fillMaxWidth(), content = content)
    }
}
