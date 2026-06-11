// 文件说明：服务发布者视角的接单/预订条目（待接入订单 API 前的 UI 模型）。

package com.example.Lulu.data.model

/**
 * @param serviceDateMillis 履约日代表时刻（通常取当地 0 点）；用于「今天 / 后续」分段与展示。
 */
data class HostIncomingOrder(
    val id: String,
    val serviceId: String,
    val serviceTitle: String,
    val guestUserId: String,
    val guestDisplayName: String,
    val serviceDateMillis: Long,
    val statusLabel: String,
    val summaryLine: String,
    val priceLine: String? = null,
)
