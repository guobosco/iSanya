// 文件说明：应用主题根 Composable（跟随系统深浅色），组合颜色、字体与形状。

package com.example.Lulu.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * 应用主题
 * 功能：定义应用的主题配置，包括颜色、字体和形状
 * @param darkTheme 是否使用暗色主题
 * @param content 主题内容
 */
@Composable
fun LuluTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) LuluDarkScheme else LuluLightScheme
    
    // 应用主题
    val view = LocalView.current
    // 不要用 SideEffect：会在每一次重组后把状态栏改回透明，与全屏看图、Pager 等
    // 里用 DisposableEffect 设置的纯色栏冲突。仅在深浅色或根 View 变化时恢复默认。
    if (!view.isInEditMode) {
        DisposableEffect(darkTheme, view) {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            onDispose { }
        }
    }

    MaterialTheme(
        // Force surfaceTint to Transparent to avoid automatic tonal elevation tinting on Dialogs/Cards
        colorScheme = colorScheme.copy(surfaceTint = Color.Transparent),
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
