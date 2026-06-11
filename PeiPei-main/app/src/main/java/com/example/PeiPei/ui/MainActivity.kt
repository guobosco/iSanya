// 文件说明：主 Activity，设置 Compose 内容与系统栏、深度链接等入口。

package com.example.Lulu.ui

import android.os.Bundle
import android.graphics.Color as AndroidColor
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import android.util.Log
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.Lulu.R
import com.example.Lulu.service.NotificationService
import com.example.Lulu.data.local.MockDataStore
import kotlinx.coroutines.launch

/**
 * 应用主活动
 * 功能：应用的入口点，设置Compose内容和主题
 */
import androidx.lifecycle.lifecycleScope
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.Lulu.ui.theme.LuluTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.core.view.WindowCompat

import com.example.Lulu.service.PermissionService
import com.example.Lulu.ui.navigation.AppNavHost

import com.example.Lulu.util.BadgeUtils

import kotlinx.coroutines.Dispatchers

/**
 * 应用主活动
 * 功能：应用的入口点，设置Compose内容和主题
 */
class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    private val REQUEST_PERMISSIONS_CODE = 100
    private val intentState = mutableStateOf<Intent?>(null)
    
    /**
     * 活动创建时调用
     * @param savedInstanceState 保存的实例状态
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        // Switch to App Theme before super.onCreate to replace Splash Theme
        setTheme(R.style.Theme_Lulu)
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = AndroidColor.TRANSPARENT
        
        intentState.value = intent
        
        // 检查并请求权限
        checkAndRequestPermissions()

        // 初始化通知渠道，确保权限检查能读取到渠道配置
        NotificationService.initChannels(this)
        
        // 设置Compose内容
        setContent {
            @OptIn(ExperimentalMaterial3Api::class)
            LuluTheme(darkTheme = isSystemInDarkTheme()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // 渲染应用导航主机
                    AppNavHost(intentState.value)
                    
                    // 全局权限监测
                    PermissionMonitor()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intentState.value = intent
        Log.d(TAG, "onNewIntent: received new intent with extras: ${intent.extras}")
    }
    
    private fun checkAndRequestPermissions() {
        val sharedPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val lastRequestDate = sharedPrefs.getString("last_permission_request_date", "")
        val todayDate = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(java.util.Date())

        // 如果用户没有给通知授权，每日都要申请1次
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != 
                PackageManager.PERMISSION_GRANTED) {
                
                // 检查是否今天已经请求过
                if (lastRequestDate != todayDate) {
                    ActivityCompat.requestPermissions(
                        this, 
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS), 
                        REQUEST_PERMISSIONS_CODE
                    )
                    // 记录请求日期
                    sharedPrefs.edit().putString("last_permission_request_date", todayDate).apply()
                }
            }
        }
        
    }
}

@Composable
fun PermissionMonitor() {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var missingPermissionType by remember { mutableStateOf("") }
    val lifecycleOwner = LocalLifecycleOwner.current

    // 定义检查权限的函数
    val checkPermissions = remember {
        {
            val hasNotification = PermissionService.hasNotificationPermission(context)
            if (!hasNotification) {
                missingPermissionType = "NOTIFICATION"
                showDialog = true
            } else {
                // 所有必要权限都已获取，确保对话框关闭
                showDialog = false
            }
        }
    }

    // 监听生命周期事件，在 ON_RESUME 时立即检查 (实现从设置页返回后的实时刷新)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 每分钟检查一次权限 (兜底)
    LaunchedEffect(Unit) {
        while (true) {
            checkPermissions()
            delay(60 * 1000L) // 1分钟
        }
    }

    if (showDialog) {
        val title = "需要权限"
        val message = when (missingPermissionType) {
            "NOTIFICATION" -> "为了接收消息和服务通知，请授予“通知”权限。"
            else -> "需要必要的权限以保证功能正常。"
        }
        
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        when (missingPermissionType) {
                            "NOTIFICATION" -> PermissionService.openNotificationPermissionSettings(context)
                        }
                    }
                ) {
                    Text("去设置")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}
