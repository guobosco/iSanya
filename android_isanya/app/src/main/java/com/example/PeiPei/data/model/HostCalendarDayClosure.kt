// 文件说明：发布者在日历上为某日设置的「不可订」规则（本地演示，可接后端替换）。

package com.example.Lulu.data.model

/**
 * 某日非「可订」时的规则。未出现在本地映射中即视为可订。
 *
 * @param allDay true：全天不可订；false：仅 [windowStart]–[windowEnd] 不可订（HH:mm，半点档）。
 */
data class HostCalendarDayClosure(
    val allDay: Boolean,
    val windowStart: String = "",
    val windowEnd: String = "",
)
