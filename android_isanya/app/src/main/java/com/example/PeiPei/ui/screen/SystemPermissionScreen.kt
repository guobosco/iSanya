// 文件说明：系统权限说明与请求引导界面。

package com.example.Lulu.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SettingsPower
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.example.Lulu.service.PermissionService
import com.example.Lulu.ui.components.ProfileGroup
import com.example.Lulu.ui.components.ProfileItem

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemPermissionScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Refresh trigger
    var refreshTrigger by remember { mutableStateOf(0) }
    
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Check permissions
    val notificationGranted = remember(refreshTrigger) { PermissionService.hasNotificationPermission(context) }
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("权限设置", fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "为了保证消息和服务通知正常工作，请授予以下权限",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            ProfileGroup(backgroundColor = MaterialTheme.colorScheme.surface) {
                // 1. 通知权限
                PermissionItem(
                    label = "通知权限",
                    isGranted = notificationGranted,
                    icon = Icons.Default.Notifications,
                    warningText = "无法接收通知",
                    onClick = { PermissionService.openNotificationPermissionSettings(context) }
                )
                Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(start = 56.dp))

                // 自启动 (国产机)
                PermissionItem(
                    label = "自启动管理",
                    isGranted = false, // Always show "Go Setting" for this manual action
                    forceAction = true,
                    icon = Icons.Default.SettingsPower,
                    warningText = "重启后可能收不到通知",
                    actionLabel = "去设置",
                    onClick = { PermissionService.openAutoStartSettings(context) }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "提示：国产手机（小米、华为、OPPO、VIVO 等）建议开启自启动与通知权限，避免消息和服务通知延迟。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun PermissionItem(
    label: String,
    isGranted: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    warningText: String,
    actionLabel: String = "去授权",
    forceAction: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isGranted || forceAction, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isGranted && !forceAction) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!isGranted || forceAction) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = warningText,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        
        if (!isGranted || forceAction) {
            Button(
                onClick = onClick,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(text = actionLabel, fontSize = 12.sp)
            }
        } else {
            Text(
                text = "已开启",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
    }
}
