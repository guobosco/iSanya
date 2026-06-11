// 文件说明：接单列表按本地日历分段（今天 / 后续）。

package com.example.Lulu.util

import com.example.Lulu.data.model.HostIncomingOrder
import java.util.Calendar

object HostOrderSchedule {
    fun startOfLocalDay(millis: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    /**
     * 「今天」：履约日不晚于今日（含历史未完结）。「后续」：明日及以后。
     */
    fun isInTodayTab(order: HostIncomingOrder, nowMillis: Long = System.currentTimeMillis()): Boolean {
        val orderDay = startOfLocalDay(order.serviceDateMillis)
        val todayStart = startOfLocalDay(nowMillis)
        return orderDay <= todayStart
    }
}
