// 文件说明：角标、未读数等展示相关的工具函数。

package com.example.Lulu.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import me.leolin.shortcutbadger.ShortcutBadger

/**
 * 工具类：设置应用图标角标 (Badge)
 * 适配不同厂商：华为、小米、三星、OPPO、VIVO、Sony 等
 * 
 * 升级：使用 ShortcutBadger 库作为主要实现，手动实现作为兜底。
 */
object BadgeUtils {

    private const val TAG = "BadgeUtils"

    /**
     * 设置角标数量
     * 优先使用 ShortcutBadger 库，失败则尝试手动适配
     */
    fun setBadgeCount(context: Context, count: Int) {
        if (count < 0) return

        var success = false
        try {
            // 1. 尝试使用 ShortcutBadger (最推荐的方式)
            success = ShortcutBadger.applyCount(context, count)
            if (success) {
                Log.d(TAG, "Badge set successfully via ShortcutBadger: $count")
            } else {
                Log.w(TAG, "ShortcutBadger failed, trying manual methods")
            }
        } catch (e: Exception) {
            Log.e(TAG, "ShortcutBadger exception", e)
        }

        // 2. 如果 ShortcutBadger 失败，尝试手动兜底方案
        if (!success) {
            // 尝试华为
            setHuaweiBadge(context, count)
            
            // 尝试小米 (MIUI 6 - 11)
            setXiaomiBadge(context, count)
            
            // 尝试三星
            setSamsungBadge(context, count)
            
            // 尝试 Sony
            setSonyBadge(context, count)
            
            // 尝试 OPPO
            setOPPOBadge(context, count)
            
            // 尝试 VIVO
            setVivoBadge(context, count)
            
            // 尝试 ZUK
            setZukBadge(context, count)
            
            // 尝试 HTC
            setHTCBadge(context, count)
        }
    }

    // --- 华为 ---
    private fun setHuaweiBadge(context: Context, count: Int) {
        try {
            val bundle = Bundle()
            bundle.putString("package", context.packageName)
            // 确保使用正确的主 Activity 类名
            bundle.putString("class", "com.example.Lulu.ui.MainActivity")
            bundle.putInt("badgenumber", count)
            context.contentResolver.call(
                Uri.parse("content://com.huawei.android.launcher.settings/badge/"),
                "change_badge",
                null,
                bundle
            )
        } catch (e: Exception) {
            // Log.e(TAG, "Failed to set Huawei badge", e)
        }
    }

    // --- 小米 ---
    // 小米通常依赖于 notification.number 或者 field 注入
    // 此方法尝试发送广播或反射（较旧的 MIUI）
    private fun setXiaomiBadge(context: Context, count: Int) {
        try {
            // 方法1：通过反射 Notification (针对发送通知时设置)
            // 这里是静态设置，比较难。通常小米推荐发送通知时带上 extraNotification.messageCount
            
            // 方法2：发送 Intent (旧版 MIUI)
            val intent = Intent("android.intent.action.APPLICATION_MESSAGE_UPDATE")
            intent.putExtra("android.intent.extra.update_application_component_name", "${context.packageName}/${getLauncherClassName(context)}")
            intent.putExtra("android.intent.extra.update_application_message_text", if (count == 0) "" else count.toString())
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            // Ignore
        }
    }

    // --- 三星 ---
    private fun setSamsungBadge(context: Context, count: Int) {
        try {
            // 方法1：ContentResolver (旧版)
            val contentUri = Uri.parse("content://com.sec.badge/apps?notify=true")
            val contentValues = ContentValues()
            contentValues.put("package", context.packageName)
            contentValues.put("class", getLauncherClassName(context))
            contentValues.put("badgecount", count)
            
            val cursor = context.contentResolver.query(contentUri, null, "package=?", arrayOf(context.packageName), null)
            if (cursor != null && cursor.moveToFirst()) {
                context.contentResolver.update(contentUri, contentValues, "package=?", arrayOf(context.packageName))
            } else {
                context.contentResolver.insert(contentUri, contentValues)
            }
            cursor?.close()
        } catch (e: Exception) {
            // 方法2：Intent (更旧版)
            try {
                val intent = Intent("android.intent.action.BADGE_COUNT_UPDATE")
                intent.putExtra("badge_count", count)
                intent.putExtra("badge_count_package_name", context.packageName)
                intent.putExtra("badge_count_class_name", getLauncherClassName(context))
                context.sendBroadcast(intent)
            } catch (e2: Exception) {
                // Ignore
            }
        }
    }

    // --- Sony ---
    private fun setSonyBadge(context: Context, count: Int) {
        try {
            val intent = Intent("com.sonyericsson.home.action.UPDATE_BADGE")
            intent.putExtra("com.sonyericsson.home.intent.extra.badge.ACTIVITY_NAME", getLauncherClassName(context))
            intent.putExtra("com.sonyericsson.home.intent.extra.badge.SHOW_MESSAGE", count > 0)
            intent.putExtra("com.sonyericsson.home.intent.extra.badge.MESSAGE", count.toString())
            intent.putExtra("com.sonyericsson.home.intent.extra.badge.PACKAGE_NAME", context.packageName)
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            // Ignore
        }
    }

    // --- OPPO ---
    private fun setOPPOBadge(context: Context, count: Int) {
        try {
            val extras = Bundle()
            extras.putInt("app_badge_count", count)
            context.contentResolver.call(
                Uri.parse("content://com.android.badge/badge"),
                "setAppBadgeCount",
                null,
                extras
            )
        } catch (e: Exception) {
            // Ignore
        }
        // 尝试发送广播 (针对旧版或 ColorOS 特定版本)
        try {
            val intent = Intent("com.oppo.unsettledevent")
            intent.putExtra("pakeageName", context.packageName)
            intent.putExtra("number", count)
            intent.putExtra("upgradeNumber", count)
            if (canResolveBroadcast(context, intent)) {
                context.sendBroadcast(intent)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }
    
    private fun canResolveBroadcast(context: Context, intent: Intent): Boolean {
        val packageManager = context.packageManager
        val receivers = packageManager.queryBroadcastReceivers(intent, 0)
        return receivers != null && receivers.size > 0
    }

    // --- VIVO ---
    private fun setVivoBadge(context: Context, count: Int) {
        try {
            val intent = Intent("launcher.action.CHANGE_APPLICATION_NOTIFICATION_NUM")
            intent.putExtra("packageName", context.packageName)
            intent.putExtra("className", getLauncherClassName(context))
            intent.putExtra("notificationNum", count)
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            // Ignore
        }
    }

    // --- ZUK ---
    private fun setZukBadge(context: Context, count: Int) {
        try {
            val bundle = Bundle()
            bundle.putStringArrayList("app_shortcut_custom_id", null)
            bundle.putInt("app_badge_count", count)
            context.contentResolver.call(
                Uri.parse("content://com.android.badge/badge"),
                "setAppBadgeCount",
                null,
                bundle
            )
        } catch (e: Exception) {
            // Ignore
        }
    }

    // --- HTC ---
    private fun setHTCBadge(context: Context, count: Int) {
        try {
            val intent = Intent("com.htc.launcher.action.SET_NOTIFICATION")
            intent.putExtra("com.htc.launcher.extra.COMPONENT", "${context.packageName}/${getLauncherClassName(context)}")
            intent.putExtra("com.htc.launcher.extra.COUNT", count)
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            // Ignore
        }
    }

    /**
     * 获取启动 Activity 的类名
     */
    private fun getLauncherClassName(context: Context): String {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfos = pm.queryIntentActivities(intent, 0)
        for (resolveInfo in resolveInfos) {
            val pkgName = resolveInfo.activityInfo.applicationInfo.packageName
            if (pkgName.equals(context.packageName, ignoreCase = true)) {
                return resolveInfo.activityInfo.name
            }
        }
        return ""
    }
}
