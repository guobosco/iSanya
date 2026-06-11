// 文件说明：前台或后台通知服务，处理消息提醒与常驻通知。

package com.example.Lulu.service

/**
 * 通知能力封装文件。
 * 负责通知渠道初始化、消息通知展示、角标更新及通知取消等通用逻辑。
 */

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.Lulu.R
import com.example.Lulu.ui.MainActivity

/**
 * 通知服务
 * 功能：封装通知相关的业务逻辑，提供显示通知的静态方法
 */
object NotificationService {
    // 通知渠道ID - 更新版本以重置用户设置
    const val CHANNEL_MESSAGE_ID = "channel_message_v3"
    // 通知ID计数器 (保留用于非特定ID的通知)
    private var notificationId = 0
    
    /**
     * 显示通知
     * @param context 上下文
     * @param title 通知标题
     * @param content 通知内容
     * @param id 可选的通知ID，如果提供，将用于覆盖或取消特定通知
     * @param tag 可选的通知Tag，用于命名空间隔离
     * @param badgeCount 角标数字 (可选)
     * @param customPendingIntent 自定义点击跳转意图 (可选)
     */
    fun showNotification(
        context: Context, 
        title: String, 
        content: String, 
        id: Int? = null, 
        tag: String? = null,
        badgeCount: Int = 0,
        customPendingIntent: PendingIntent? = null
    ) {
        // 创建通知渠道
        createMessageChannel(context)
        
        // 尝试设置应用图标角标 (使用 BadgeUtils 统一处理，支持更多机型)
        try {
            com.example.Lulu.util.BadgeUtils.setBadgeCount(context, badgeCount)
        } catch (e: Exception) {
            // 忽略异常
        }

        // 点击通知后要启动的活动 (默认跳转首页)
        val defaultIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val defaultPendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, defaultIntent, PendingIntent.FLAG_IMMUTABLE)
        
        val finalPendingIntent = customPendingIntent ?: defaultPendingIntent

        // 构建通知
        val largeIcon = android.graphics.BitmapFactory.decodeResource(context.resources, R.drawable.ic_logo)
        val builder = NotificationCompat.Builder(context, CHANNEL_MESSAGE_ID)
            .setSmallIcon(R.drawable.ic_logo)
            .setLargeIcon(largeIcon) // 设置大图标，类似微信联系人头像
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content)) // 支持长文本
            .setPriority(NotificationCompat.PRIORITY_HIGH) // 提升优先级以确保弹出
            .setCategory(NotificationCompat.CATEGORY_MESSAGE) // 归类为消息
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // 锁屏可见
            .setContentIntent(finalPendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL) // 默认声音和振动
            .setNumber(badgeCount) // 设置角标数字 (Android 8.0+ 标准方式)

        val notification = builder.build()
        
        // 获取通知管理器并显示通知
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val finalId = id ?: notificationId++
        if (tag != null) {
            notificationManager.notify(tag, finalId, notification)
        } else {
            notificationManager.notify(finalId, notification)
        }
    }

    /**
     * 取消指定ID的通知
     */
    fun cancelNotification(context: Context, id: Int, tag: String? = null) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (tag != null) {
            notificationManager.cancel(tag, id)
        } else {
            notificationManager.cancel(id)
        }
    }

    /**
     * 取消所有通知
     */
    fun cancelAll(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
    }
    
    /**
     * 创建"新消息通知"渠道
     */
    fun createMessageChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "新消息通知" // 仿照微信命名
            val descriptionText = "用于显示聊天消息、服务动态等消息"
            val importance = NotificationManager.IMPORTANCE_HIGH 
            val channel = NotificationChannel(CHANNEL_MESSAGE_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(true) // 显式开启角标
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250) // 添加振动模式以增强横幅通知的触发概率
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC // 锁屏显示
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 初始化所有通知渠道
     */
    fun initChannels(context: Context) {
        createMessageChannel(context)
    }
}
