// 文件说明：服务可预约时段（多段 HH:mm）的 JSON 编解码与展示文案。

package com.example.Lulu.util

import kotlin.math.abs
import kotlin.math.round
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken

data class BookingTimeSlot(
    @SerializedName("start")
    val start: String,
    @SerializedName("end")
    val end: String,
)

object BookingTimeRangesCodec {
    private val gson = Gson()

    /** 新建服务时「至少需提前预定」的默认小时数；0 表示无需提前 */
    const val DEFAULT_BOOKING_LEAD_HOURS = 0f

    /** 用户可选定服务开始日期的最远天数（含今天为第 1 天），默认 30，允许 7–180 */
    const val DEFAULT_BOOKING_FUTURE_OPEN_DAYS = 30
    const val MIN_BOOKING_FUTURE_OPEN_DAYS = 7
    const val MAX_BOOKING_FUTURE_OPEN_DAYS = 180

    fun normalizeBookingFutureOpenDays(days: Int): Int = when {
        days < MIN_BOOKING_FUTURE_OPEN_DAYS -> DEFAULT_BOOKING_FUTURE_OPEN_DAYS
        days > MAX_BOOKING_FUTURE_OPEN_DAYS -> MAX_BOOKING_FUTURE_OPEN_DAYS
        else -> days
    }

    /** 发布页「时间设置」摘要一行 */
    fun futureOpenWindowSummary(days: Int): String {
        val n = normalizeBookingFutureOpenDays(days)
        return "未来${n}天内可预定"
    }

    /** 底部弹窗内可选的提前量（小时） */
    val BOOKING_LEAD_HOUR_PRESETS =
        listOf(0f, 0.5f, 1f, 1.5f, 2f, 2.5f, 3f, 3.5f, 4f, 6f, 12f, 24f)

    fun normalizeBookingLeadHours(value: Float): Float {
        if (value.isNaN()) return DEFAULT_BOOKING_LEAD_HOURS
        return value.coerceIn(0f, 168f)
    }

    fun formatLeadHoursValue(hours: Float): String {
        val h = normalizeBookingLeadHours(hours)
        if (h <= 0f) return "0"
        val scaled = round(h * 10f) / 10f
        val asInt = scaled.toInt()
        if (abs(scaled - asInt) < 1e-4f) return asInt.toString()
        return "%.1f".format(scaled).trimEnd('0').trimEnd('.')
    }

    fun leadHoursRequirementSummary(hours: Float): String {
        val h = normalizeBookingLeadHours(hours)
        if (h <= 0f) return "可随时预定（无需提前）"
        return "至少需提前${formatLeadHoursValue(h)}小时预定"
    }

    fun halfHourLabels(): List<String> =
        (0 until 48).map { i ->
            val h = i / 2
            val m = (i % 2) * 30
            "%02d:%02d".format(h, m)
        }

    /** 作为时段开始时使用，保证至少存在更晚的半点作为结束时间 */
    fun halfHourStartChoices(): List<String> = halfHourLabels().dropLast(1)

    fun parseMinutes(hhmm: String): Int? {
        val parts = hhmm.trim().split(':')
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return h * 60 + m
    }

    fun endOptionsForStart(start: String): List<String> {
        val sm = parseMinutes(start) ?: return halfHourLabels()
        return halfHourLabels().filter { t ->
            val em = parseMinutes(t) ?: return@filter false
            em > sm
        }
    }

    fun encode(slots: List<BookingTimeSlot>): String =
        gson.toJson(slots)

    fun decode(json: String): List<BookingTimeSlot> {
        if (json.isBlank()) return emptyList()
        return try {
            val type = object : TypeToken<List<BookingTimeSlot>>() {}.type
            gson.fromJson<List<BookingTimeSlot>>(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * 某日可选的「开始时间」半点刻度（分钟，从 0 到 23*60+30），由可预约时段 JSON 合并去重后排序。
     * JSON 为空表示全天 24 小时；时段为左闭右开 [start, end)，与发布页半点选择一致。
     */
    fun availableStartMinutesForDay(bookingTimeRangesJson: String): List<Int> {
        val slots = decode(bookingTimeRangesJson)
        val parsedRanges = slots.mapNotNull { slot ->
            val s = parseMinutes(slot.start) ?: return@mapNotNull null
            val e = parseMinutes(slot.end) ?: return@mapNotNull null
            if (e <= s) return@mapNotNull null
            s to e
        }
        val ranges: List<Pair<Int, Int>> =
            if (slots.isEmpty() || parsedRanges.isEmpty()) {
                listOf(0 to 24 * 60)
            } else {
                parsedRanges
            }
        val out = LinkedHashSet<Int>()
        for ((s, eExclusive) in ranges) {
            var m = ((s + 29) / 30) * 30
            while (m < eExclusive && m <= 23 * 60 + 30) {
                out.add(m)
                m += 30
            }
        }
        return out.sorted()
    }

    fun summary(slots: List<BookingTimeSlot>): String {
        if (slots.isEmpty()) return "24小时均可提供服务"
        val first = slots.first()
        val a = displayOne(first)
        return if (slots.size == 1) a else "$a 等${slots.size}个时段"
    }

    fun displayOne(slot: BookingTimeSlot): String {
        val s = formatClock(slot.start)
        val e = formatClock(slot.end)
        return "$s—$e"
    }

    fun formatClock(hhmm: String): String {
        val p = parseMinutes(hhmm) ?: return hhmm.trim()
        val h = p / 60
        val m = p % 60
        return "$h:${"%02d".format(m)}"
    }
}
