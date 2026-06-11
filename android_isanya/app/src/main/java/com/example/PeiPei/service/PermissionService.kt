// 文件说明：与权限引导、检测或设置跳转相关的服务逻辑。

package com.example.Lulu.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationManagerCompat

/**
 * 权限服务
 * 功能：处理应用所需的权限相关操作
 */
object PermissionService {
    
    /**
     * 检查是否有通知权限
     * @param context 上下文
     * @return 是否有权限
     */
    fun hasNotificationPermission(context: Context): Boolean {
        // 1. 先检查系统层面的通知开关（适用于所有版本）
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            return false
        }
        
        // 2. 针对 Android 13+ 检查具体的运行时权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        }
        
        return true
    }

    /**
     * 打开应用通知权限设置页面
     * @param context 上下文
     */
    fun openNotificationPermissionSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
        context.startActivity(intent)
    }
    
    /**
     * 打开应用设置页面
     * @param context 上下文
     */
    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }

    /**
     * 打开自启动管理页面 (针对国产ROM)
     * @param context 上下文
     */
    fun openAutoStartSettings(context: Context) {
        val intent = Intent()
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val manufacturer = Build.MANUFACTURER.lowercase()
            when {
                manufacturer.contains("xiaomi") || manufacturer.contains("redmi") -> {
                    intent.component = android.content.ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
                }
                manufacturer.contains("oppo") || manufacturer.contains("oneplus") || manufacturer.contains("realme") -> {
                    intent.component = android.content.ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")
                }
                manufacturer.contains("vivo") || manufacturer.contains("iqoo") -> {
                    intent.component = android.content.ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")
                }
                manufacturer.contains("huawei") || manufacturer.contains("honor") -> {
                    intent.component = android.content.ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")
                }
                manufacturer.contains("samsung") -> {
                    intent.component = android.content.ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity")
                }
                manufacturer.contains("meizu") -> {
                    intent.component = android.content.ComponentName("com.meizu.safe", "com.meizu.safe.permission.SmartBGActivity")
                }
                else -> {
                    // 默认跳转应用详情页
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    intent.data = Uri.fromParts("package", context.packageName, null)
                }
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                // 备用方案：跳转应用详情页
                intent.component = null
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                intent.data = Uri.fromParts("package", context.packageName, null)
                context.startActivity(intent)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
}
