// 文件说明：全屏查看图片的对话框或覆盖层。

package com.example.Lulu.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.Window
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.Lulu.data.remote.RetrofitClient
import com.example.Lulu.ui.util.findComposeDialogWindow

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun FullScreenImageDialog(
    imageUrl: String,
    imageUrls: List<String> = listOf(imageUrl),
    initialPage: Int = 0,
    onDismiss: () -> Unit
) {
    val displayImageUrls = imageUrls
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .ifEmpty { imageUrl.trim().takeIf { it.isNotEmpty() }?.let(::listOf) ?: emptyList() }

    if (displayImageUrls.isNotEmpty()) {
        val pagerState = rememberPagerState(
            initialPage = initialPage.coerceIn(0, displayImageUrls.lastIndex),
            pageCount = { displayImageUrls.size }
        )
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = true,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            val hostView = LocalView.current
            if (!hostView.isInEditMode) {
                DisposableEffect(hostView) {
                    val activity = hostView.context.findActivity()
                    val activityWindow = activity?.window
                    val dialogWindow = hostView.findComposeDialogWindow()
                    // 系统状态栏由 Activity 主窗口决定；仅改 Dialog 自己的 Window 往往无效。
                    val windowsToStyle = buildList {
                        activityWindow?.let { add(it to activityWindow.decorView) }
                        if (dialogWindow != null && dialogWindow !== activityWindow) {
                            add(dialogWindow to hostView)
                        }
                    }
                    if (windowsToStyle.isEmpty()) return@DisposableEffect onDispose { }

                    data class Saved(
                        val window: Window,
                        val insetsAnchor: android.view.View,
                        val statusBarColor: Int,
                        val navigationBarColor: Int,
                        val lightStatusBars: Boolean,
                        val lightNavigationBars: Boolean,
                        val windowFlags: Int,
                    )
                    val saved = windowsToStyle.map { (w, anchor) ->
                        val c = WindowCompat.getInsetsController(w, anchor)
                        Saved(
                            window = w,
                            insetsAnchor = anchor,
                            statusBarColor = w.statusBarColor,
                            navigationBarColor = w.navigationBarColor,
                            lightStatusBars = c.isAppearanceLightStatusBars,
                            lightNavigationBars = c.isAppearanceLightNavigationBars,
                            windowFlags = w.attributes.flags,
                        )
                    }
                    for ((w, anchor) in windowsToStyle) {
                        w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                        w.statusBarColor = Color.Black.toArgb()
                        w.navigationBarColor = Color.Black.toArgb()
                        val c = WindowCompat.getInsetsController(w, anchor)
                        // 深色状态栏背景：使用浅色图标/文字（非 “light status bar” 外观）
                        c.isAppearanceLightStatusBars = false
                        c.isAppearanceLightNavigationBars = false
                    }
                    onDispose {
                        for (s in saved) {
                            s.window.attributes = s.window.attributes.apply { flags = s.windowFlags }
                            s.window.statusBarColor = s.statusBarColor
                            s.window.navigationBarColor = s.navigationBarColor
                            val c = WindowCompat.getInsetsController(s.window, s.insetsAnchor)
                            c.isAppearanceLightStatusBars = s.lightStatusBars
                            c.isAppearanceLightNavigationBars = s.lightNavigationBars
                        }
                    }
                }
                // LuluTheme 的 SideEffect 会在每次重组后把状态栏改回透明；滑动 Pager 等会触发重组，
                // 仅 DisposableEffect 无法再次覆盖。此处每帧强制纯黑，直到对话框从组合中移除。
                SideEffect {
                    val activity = hostView.context.findActivity()
                    val activityWindow = activity?.window
                    val dialogWindow = hostView.findComposeDialogWindow()
                    val windowsToStyle = buildList {
                        activityWindow?.let { add(it to activityWindow.decorView) }
                        if (dialogWindow != null && dialogWindow !== activityWindow) {
                            add(dialogWindow to hostView)
                        }
                    }
                    for ((w, anchor) in windowsToStyle) {
                        w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                        w.statusBarColor = Color.Black.toArgb()
                        w.navigationBarColor = Color.Black.toArgb()
                        val c = WindowCompat.getInsetsController(w, anchor)
                        c.isAppearanceLightStatusBars = false
                        c.isAppearanceLightNavigationBars = false
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    ZoomableFitAsyncImage(
                        model = RetrofitClient.normalizeBackendMediaUrlForDisplay(displayImageUrls[page]),
                        contentDescription = "Full Screen Image",
                        embedInHorizontalPager = displayImageUrls.size > 1,
                        onClickWhenNotZoomed = onDismiss,
                    )
                }

                if (displayImageUrls.size > 1) {
                    Text(
                        text = "${pagerState.currentPage + 1}/${displayImageUrls.size}",
                        color = Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 28.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0x66000000))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
