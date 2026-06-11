// 文件说明：发布者视角下，某服务在指定日期的预定占位数据（演示/本地）。

package com.example.Lulu.data.model

/**
 * 单个「已预定」日历格子对应的订单摘要。
 * [dateIso] 为 `yyyy-MM-dd`（设备本地日历日）。
 */
data class HostServiceBooking(
    val orderId: String,
    val serviceId: String,
    val dateIso: String,
    val guestUserId: String,
    val guestName: String,
    val guestPhotoUrl: String,
)
