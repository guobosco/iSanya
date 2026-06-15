// 文件说明：启动闪屏与冷启动路由判断。

package com.example.Lulu.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import com.example.Lulu.R
import com.example.Lulu.data.local.AppDataStore
import com.example.Lulu.ui.navigation.Screen
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

import androidx.compose.runtime.rememberUpdatedState
import android.app.Activity

@Composable
fun SplashScreen(navController: NavController, nextRoute: String) {
    val currentNextRoute = rememberUpdatedState(nextRoute)
    val view = LocalView.current
    val repository = AppDataStore.getRepository()

    // 启动时优先预加载首页首屏关键数据，减少进入首页后空白等待。
    LaunchedEffect(Unit) {
        val minSplashDurationMs = 900L
        val preloadTimeoutMs = 3200L
        val startedAt = System.currentTimeMillis()

        withTimeoutOrNull(preloadTimeoutMs) {
            // 默认用户注入在 Application 中异步执行，冷启动时这里给一点时间拿到 userId。
            var userId = repository?.currentUserId.orEmpty()
            repeat(12) {
                if (userId.isNotBlank()) return@repeat
                delay(100)
                userId = repository?.currentUserId.orEmpty()
            }

            if (userId.isNotBlank()) {
                repository?.refreshHomeCriticalData(userId)
            }
            AppDataStore.reloadServicesFromDatabase()
        }

        val spent = System.currentTimeMillis() - startedAt
        val remain = (minSplashDurationMs - spent).coerceAtLeast(0L)
        if (remain > 0) delay(remain)

        navController.navigate(currentNextRoute.value) {
            popUpTo(Screen.Splash.route) { inclusive = true }
        }
    }

    DisposableEffect(view) {
        val window = (view.context as Activity).window
        val previousStatusBarColor = window.statusBarColor
        val previousLightStatusBars = WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars

        window.statusBarColor = android.graphics.Color.TRANSPARENT
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false

        onDispose {
            window.statusBarColor = previousStatusBarColor
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = previousLightStatusBars
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEB2C5F))
    ) {
        Image(
            painter = painterResource(id = R.drawable.launch),
            contentDescription = "启动画面",
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}
