// 文件说明：服务/体验详情页「服务介绍 / 方案选择 / 预定须知」区块（仿预订类 App 的分组行样式）。

package com.example.Lulu.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.Lulu.data.local.AppDataStore
import com.example.Lulu.data.model.HostCalendarDayClosure
import com.example.Lulu.data.model.Service
import com.example.Lulu.ui.theme.DialogTitleTopPadding
import com.example.Lulu.util.BookingTimeRangesCodec
import com.example.Lulu.util.decodePublishPriceTiers
import com.example.Lulu.util.priceBasisTextForUiDisplay
import java.time.Instant
import java.time.ZoneId
import java.util.Calendar
import java.util.Locale

data class ServiceContentPickOption(
    val id: String,
    val label: String,
    val subtitle: String,
    val priceText: String,
    val priceBasisText: String,
    /** 底部价格栏副标题：仅服务时长，如「4小时」「90分钟」。 */
    val durationDisplay: String,
)

fun buildServiceContentPickOptions(service: Service): List<ServiceContentPickOption> {
    val basisUi = priceBasisTextForUiDisplay(service.priceBasisText, service.serviceMode)
        .ifBlank { "计费以沟通确认为准" }
    val rows = decodePublishPriceTiers(service.priceText, service.priceBasisText)
    val picks = rows.filter { it.price.isNotBlank() }.ifEmpty { rows }
    return picks.mapIndexed { i, t ->
        val dur = "${t.durationAmount}${t.durationUnit}"
        val label = t.name.trim().ifBlank { "档位 ${i + 1}" }
        val pt = t.price.trim().ifBlank { "价格面议" }
        ServiceContentPickOption(
            id = "tier_$i",
            label = label,
            subtitle = dur,
            priceText = pt,
            priceBasisText = basisUi,
            durationDisplay = dur,
        )
    }
}

fun buildExperienceContentPickOptions(
    priceFromYuan: Int,
    priceBasisText: String,
): List<ServiceContentPickOption> {
    val basis = priceBasisText.ifBlank { "每人起" }
    if (priceFromYuan <= 0) {
        return listOf(
            ServiceContentPickOption(
                id = "single",
                label = "标准体验",
                subtitle = "动线与集合以预订后确认为准",
                priceText = "价格面议",
                priceBasisText = basis,
                durationDisplay = "",
            ),
        )
    }
    val y = priceFromYuan
    return listOf(
        ServiceContentPickOption(
            id = "exp_a",
            label = "标准档",
            subtitle = "含核心体验动线",
            priceText = "¥ $y",
            priceBasisText = basis,
            durationDisplay = "",
        ),
        ServiceContentPickOption(
            id = "exp_b",
            label = "优选档",
            subtitle = "含加项与小团优先",
            priceText = "¥ ${y + (y / 5).coerceAtLeast(20)}",
            priceBasisText = basis,
            durationDisplay = "",
        ),
        ServiceContentPickOption(
            id = "exp_c",
            label = "尊享档",
            subtitle = "含深度讲解与定制时段",
            priceText = "¥ ${y + (y / 2).coerceAtLeast(50)}",
            priceBasisText = basis,
            durationDisplay = "",
        ),
    )
}

private fun floorToDayStartMillisSchedule(timestampMillis: Long): Long {
    return Calendar.getInstance().run {
        timeInMillis = timestampMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        timeInMillis
    }
}

private fun mergeDateAndMinutesSchedule(daySelectionMillis: Long, minutesFromMidnight: Int): Long {
    val m = minutesFromMidnight.coerceIn(0, 23 * 60 + 59)
    return Calendar.getInstance().run {
        timeInMillis = daySelectionMillis
        set(Calendar.HOUR_OF_DAY, m / 60)
        set(Calendar.MINUTE, m % 60)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        timeInMillis
    }
}

private fun minutesFromMidnightForMillis(millis: Long): Int {
    val c = Calendar.getInstance().apply { timeInMillis = millis }
    return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
}

private fun localDateIsoFromDayStartMillisSchedule(dayStartMillis: Long): String =
    Instant.ofEpochMilli(dayStartMillis).atZone(ZoneId.systemDefault()).toLocalDate().toString()

/** 与「预定日历」窗口不可订一致：起点落在 [windowStart, windowEnd) 的半点不可选。 */
private fun bookingStartMinuteInsideDayClosureWindow(startMinute: Int, closure: HostCalendarDayClosure): Boolean {
    if (closure.allDay) return true
    val ws = BookingTimeRangesCodec.parseMinutes(closure.windowStart) ?: return false
    val we = BookingTimeRangesCodec.parseMinutes(closure.windowEnd) ?: return false
    if (we <= ws) return false
    return startMinute >= ws && startMinute < we
}

private fun bookingStartMinuteAllowedForDayClosure(startMinute: Int, closure: HostCalendarDayClosure?): Boolean {
    val c = closure ?: return true
    if (c.allDay) return false
    return !bookingStartMinuteInsideDayClosureWindow(startMinute, c)
}

private fun hhmmFromMinutes(minutes: Int): String =
    "%02d:%02d".format(minutes / 60, minutes % 60)

/** 与已选日期同日时，从「上次确认的预约时刻」映射到当前仍可选的标签（精确或下一个半点）。 */
private fun serviceDetailScheduleLabelFromInitialSameDay(
    labels: List<String>,
    initialMillis: Long,
): String? {
    val wantM = minutesFromMidnightForMillis(initialMillis)
    val withParsed = labels.mapNotNull { lab ->
        val pm = BookingTimeRangesCodec.parseMinutes(lab) ?: return@mapNotNull null
        pm to lab
    }
    withParsed.firstOrNull { (pm, _) -> pm == wantM }?.second?.let { return it }
    return withParsed.firstOrNull { (pm, _) -> pm >= wantM }?.second
}

/** 详情页时间选择展示：如 5月8日 14:30 或 5月8日14点 */
fun formatScheduleSlotDisplay(millis: Long): String {
    val c = Calendar.getInstance().apply { timeInMillis = millis }
    val mo = c.get(Calendar.MONTH) + 1
    val d = c.get(Calendar.DAY_OF_MONTH)
    val h = c.get(Calendar.HOUR_OF_DAY)
    val min = c.get(Calendar.MINUTE)
    return if (min == 0) {
        "${mo}月${d}日${h}点"
    } else {
        "${mo}月${d}日${h}:${"%02d".format(min)}"
    }
}

private val SectionTitleColor = Color(0xFF222222)
private val RowTitleColor = Color(0xFF222222)
private val RowSubtitleColor = Color(0xFF777777)
private val ChevronTint = Color(0xFFBDBDBD)
private val IntroExpandButtonBg = Color(0xFFF2F2F2)
private const val IntroBodyMaxLinesCollapsed = 8
/** 服务介绍 / 方案选择 / 预定须知 等大区块之间的最小间距 */
private val SectionBlockVerticalGap = 20.dp
/** 预定须知每条行的上下内边距 */
private val BookingPolicyRowVerticalPadding = 18.dp

private const val ServiceDetailServiceTypeFallback =
    "服务内容、提供方式与计费规则请与主理人沟通确认。"

@Composable
private fun ServiceIntroExpandableText(fullDescription: String) {
    val body = fullDescription.trim().ifBlank { "暂无介绍" }
    var expanded by remember(fullDescription) { mutableStateOf(false) }
    var canExpand by remember(fullDescription) { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = body,
            color = Color(0xFF222222),
            fontSize = 15.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Normal,
            maxLines = if (expanded) Int.MAX_VALUE else IntroBodyMaxLinesCollapsed,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { layout ->
                if (!expanded) {
                    if (layout.hasVisualOverflow) {
                        canExpand = true
                    }
                }
            },
        )
        if (canExpand) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .clickable {
                        expanded = !expanded
                    },
                shape = RoundedCornerShape(22.dp),
                color = IntroExpandButtonBg,
            ) {
                Text(
                    text = if (expanded) "收起" else "显示更多",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    textAlign = TextAlign.Center,
                    color = Color(0xFF222222),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun ServiceDetailSectionHeader(title: String) {
    Text(
        text = title,
        color = SectionTitleColor,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 0.dp, bottom = 12.dp),
    )
}

@Composable
private fun ServiceDetailChevronRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    verticalPadding: Dp = 12.dp,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(PaddingValues(vertical = verticalPadding)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = RowTitleColor.copy(alpha = 0.88f),
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = RowTitleColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    color = RowSubtitleColor,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = ChevronTint,
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
private fun ServiceDetailInfoCard(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFFAFAFA),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
    ) {
        Text(
            text = text,
            color = RowSubtitleColor,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ServiceTypeKeywordTags(tags: List<String>) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tags.forEach { tag ->
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = Color(0xFFF7F7F7),
            ) {
                Text(
                    text = tag,
                    color = Color(0xFF333333),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServiceDetailSchedulePickerSheetContent(
    initialMillis: Long?,
    allowedWeekdays: Set<Int>?,
    bookingTimeRangesJson: String,
    bookingLeadHours: Float,
    /** 从今天起可预定的日历天数（含今天），由发布者设置 7–180，默认 30 */
    bookingFutureOpenDays: Int,
    /** 与「预定日历」闭店数据关联；null 时不读本地闭店映射。 */
    hostCalendarServiceId: String?,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    val pickerLocale = remember { Locale.forLanguageTag("zh-CN") }
    val todayStart = remember { floorToDayStartMillisSchedule(System.currentTimeMillis()) }
    val maxDaySpan = BookingTimeRangesCodec.normalizeBookingFutureOpenDays(bookingFutureOpenDays)
    val maxBookableEnd = remember(todayStart, maxDaySpan) {
        Calendar.getInstance().run {
            timeInMillis = todayStart
            add(Calendar.DAY_OF_YEAR, (maxDaySpan - 1).coerceAtLeast(0))
            timeInMillis
        }
    }
    val closureRoot by AppDataStore.hostCalendarDayClosures.collectAsState()
    val closuresForService = remember(closureRoot, hostCalendarServiceId) {
        if (hostCalendarServiceId.isNullOrBlank()) emptyMap()
        else closureRoot[hostCalendarServiceId].orEmpty()
    }
    val seedMillis = initialMillis ?: System.currentTimeMillis()
    val selectableDates = remember(todayStart, maxBookableEnd, allowedWeekdays, closuresForService) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val dayStart = floorToDayStartMillisSchedule(utcTimeMillis)
                if (dayStart < todayStart || dayStart > maxBookableEnd) return false
                val iso = localDateIsoFromDayStartMillisSchedule(dayStart)
                if (closuresForService[iso]?.allDay == true) return false
                if (allowedWeekdays.isNullOrEmpty()) return true
                val dow = Calendar.getInstance().apply { timeInMillis = dayStart }.get(Calendar.DAY_OF_WEEK)
                return dow in allowedWeekdays
            }
        }
    }
    val datePickerState = remember(initialMillis, selectableDates, bookingFutureOpenDays) {
        DatePickerState(
            locale = pickerLocale,
            initialSelectedDateMillis = seedMillis,
            selectableDates = selectableDates,
        )
    }
    val dayMillis = datePickerState.selectedDateMillis
    val dayClosure = remember(dayMillis, closuresForService) {
        if (dayMillis == null) null
        else {
            val ds = floorToDayStartMillisSchedule(dayMillis)
            closuresForService[localDateIsoFromDayStartMillisSchedule(ds)]
        }
    }
    val filteredStartMinutes = remember(dayMillis, bookingTimeRangesJson, bookingLeadHours, dayClosure) {
        if (dayMillis == null) {
            emptyList()
        } else {
            val minTs = System.currentTimeMillis() +
                (BookingTimeRangesCodec.normalizeBookingLeadHours(bookingLeadHours).toDouble() * 3_600_000.0).toLong()
            BookingTimeRangesCodec.availableStartMinutesForDay(bookingTimeRangesJson).filter { m ->
                mergeDateAndMinutesSchedule(dayMillis, m) >= minTs &&
                    bookingStartMinuteAllowedForDayClosure(m, dayClosure)
            }
        }
    }
    var selectedStartHhmm by remember { mutableStateOf<String?>(null) }
    /** 上一次在日历中选中的「日初」毫秒，用于区分「首次打开」与「用户换日」。 */
    var prevPickerDayStartMillis by remember { mutableStateOf<Long?>(null) }
    val timeChipListState = rememberLazyListState()
    LaunchedEffect(dayMillis, bookingTimeRangesJson, bookingLeadHours, initialMillis, filteredStartMinutes) {
        val dm = dayMillis
        if (dm == null || filteredStartMinutes.isEmpty()) {
            selectedStartHhmm = null
            prevPickerDayStartMillis = null
            return@LaunchedEffect
        }
        val labels = filteredStartMinutes.map(::hhmmFromMinutes)
        val dayStart = floorToDayStartMillisSchedule(dm)
        val prevDayStart = prevPickerDayStartMillis
        val isFirstRun = prevDayStart == null
        val dayChanged = !isFirstRun && prevDayStart != dayStart

        if (dayChanged) {
            // 换日后：今日 → 当前仍可选的最近一段；非今日 → 该日最早可订半点（列表已按时间升序且含提前量约束）
            selectedStartHhmm = labels.first()
            prevPickerDayStartMillis = dayStart
            return@LaunchedEffect
        }

        if (isFirstRun) {
            prevPickerDayStartMillis = dayStart
            val init = initialMillis
            if (init != null && floorToDayStartMillisSchedule(init) == dayStart) {
                val picked = serviceDetailScheduleLabelFromInitialSameDay(labels, init)
                if (picked != null) {
                    selectedStartHhmm = picked
                    return@LaunchedEffect
                }
            }
            selectedStartHhmm = labels.first()
            return@LaunchedEffect
        }

        prevPickerDayStartMillis = dayStart
        val cur = selectedStartHhmm
        if (cur != null && cur in labels) return@LaunchedEffect
        val init = initialMillis
        if (init != null && floorToDayStartMillisSchedule(init) == dayStart) {
            val picked = serviceDetailScheduleLabelFromInitialSameDay(labels, init)
            if (picked != null) {
                selectedStartHhmm = picked
                return@LaunchedEffect
            }
        }
        selectedStartHhmm = labels.first()
    }
    LaunchedEffect(selectedStartHhmm, filteredStartMinutes, dayMillis) {
        val sel = selectedStartHhmm ?: return@LaunchedEffect
        if (filteredStartMinutes.isEmpty()) return@LaunchedEffect
        val idx = filteredStartMinutes.indexOfFirst { hhmmFromMinutes(it) == sel }
        if (idx >= 0) {
            timeChipListState.scrollToItem(idx)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = DialogTitleTopPadding, bottom = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "选择日期与时间",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111111),
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "关闭",
                    tint = Color(0xFF222222),
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        DatePicker(
            state = datePickerState,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp),
            colors = DatePickerDefaults.colors(
                selectedDayContainerColor = Color(0xFF111111),
                selectedDayContentColor = Color.White,
                selectedYearContainerColor = Color(0xFF111111),
                selectedYearContentColor = Color.White,
            ),
            title = null,
            headline = null,
            showModeToggle = false,
        )
        Text(
            text = "可选时间",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF444444),
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
        )
        if (filteredStartMinutes.isEmpty()) {
            Text(
                text = "该日暂无可选时间，请更换日期或稍后再试",
                fontSize = 13.sp,
                color = RowSubtitleColor,
                modifier = Modifier.padding(vertical = 6.dp),
            )
        } else {
            LazyRow(
                state = timeChipListState,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(
                    items = filteredStartMinutes,
                    key = { it },
                ) { minutes ->
                    val label = hhmmFromMinutes(minutes)
                    val display = BookingTimeRangesCodec.formatClock(label)
                    val selected = label == selectedStartHhmm
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { selectedStartHhmm = label },
                        shape = RoundedCornerShape(20.dp),
                        color = if (selected) Color(0xFF111111) else Color(0xFFF2F2F2),
                    ) {
                        Text(
                            text = display,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            color = if (selected) Color.White else Color(0xFF222222),
                            fontSize = 14.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onClear) {
                Text("清除", color = Color(0xFF8A8A8A))
            }
            val pm = selectedStartHhmm?.let { BookingTimeRangesCodec.parseMinutes(it) }
            TextButton(
                onClick = {
                    if (dayMillis != null && pm != null) {
                        onConfirm(mergeDateAndMinutesSchedule(dayMillis, pm))
                    }
                },
                enabled = dayMillis != null && pm != null,
            ) {
                Text("确定", fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceDetailBookingPolicySections(
    introSectionTitle: String,
    contentSectionTitle: String,
    bookingSectionTitle: String,
    fullDescription: String,
    contentOptions: List<ServiceContentPickOption>,
    selectedContentIndex: Int,
    onSelectedContentIndex: (Int) -> Unit,
    scheduleMillis: Long?,
    onScheduleMillisChange: (Long?) -> Unit,
    scheduleAllowedWeekdays: Set<Int>? = null,
    /** 与发布页「可预约时段」一致；空字符串表示全天可订（半点刻度）。 */
    scheduleBookingTimeRangesJson: String = "",
    scheduleBookingLeadHours: Float = BookingTimeRangesCodec.DEFAULT_BOOKING_LEAD_HOURS,
    scheduleBookingFutureOpenDays: Int = BookingTimeRangesCodec.DEFAULT_BOOKING_FUTURE_OPEN_DAYS,
    /** 与 AppDataStore 预定日历闭店一致；null 表示不在此层过滤闭店。 */
    scheduleHostCalendarServiceId: String? = null,
    serviceDeclarationsSubtitle: String,
    serviceDeclarationsReaderBody: String,
    extraFeeDescription: String? = null,
    serviceTypeDescription: String? = null,
    serviceTypeKeywords: List<String> = emptyList(),
    showBookingSelectionInline: Boolean = true,
) {
    var showContentSheet by remember { mutableStateOf(false) }
    var showScheduleSheet by remember { mutableStateOf(false) }
    var showPolicyReader by remember { mutableStateOf(false) }
    var policyReaderTitle by remember { mutableStateOf("") }
    var policyReaderBody by remember { mutableStateOf("") }

    val contentSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scheduleSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val policySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val safeIndex = selectedContentIndex.coerceIn(0, (contentOptions.size - 1).coerceAtLeast(0))
    val picked = contentOptions.getOrNull(safeIndex)
        ?: ServiceContentPickOption(
            id = "fallback",
            label = "标准方案",
            subtitle = "",
            priceText = "价格面议",
            priceBasisText = "",
            durationDisplay = "",
        )

    Column(modifier = Modifier.fillMaxWidth()) {
        ServiceDetailSectionHeader(introSectionTitle)
        ServiceIntroExpandableText(fullDescription = fullDescription)
        Spacer(modifier = Modifier.height(SectionBlockVerticalGap))
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 1.dp,
            color = DetailScreenTagDividerColor,
        )
        Spacer(modifier = Modifier.height(SectionBlockVerticalGap))
        if (!serviceTypeDescription.isNullOrBlank() || serviceTypeKeywords.isNotEmpty()) {
            ServiceDetailSectionHeader(title = "服务类型说明")
            Text(
                text = serviceTypeDescription?.ifBlank { ServiceDetailServiceTypeFallback }
                    ?: ServiceDetailServiceTypeFallback,
                color = RowSubtitleColor,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.fillMaxWidth(),
            )
            if (serviceTypeKeywords.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                ServiceTypeKeywordTags(tags = serviceTypeKeywords)
            }
            Spacer(modifier = Modifier.height(SectionBlockVerticalGap))
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = DetailScreenTagDividerColor,
            )
            Spacer(modifier = Modifier.height(SectionBlockVerticalGap))
        }
        if (showBookingSelectionInline) {
            ServiceDetailSectionHeader(contentSectionTitle)
            ServiceDetailChevronRow(
                icon = Icons.Outlined.Apps,
                title = "当前选择",
                subtitle = "${picked.label} · ${picked.subtitle}",
                onClick = { showContentSheet = true },
            )
            Spacer(modifier = Modifier.height(SectionBlockVerticalGap))
            if (!extraFeeDescription.isNullOrBlank()) {
                ServiceDetailSectionHeader("额外费用说明")
                ServiceDetailInfoCard(text = extraFeeDescription)
            } else {
                ServiceDetailSectionHeader("时间选择")
                ServiceDetailChevronRow(
                    icon = Icons.Outlined.DateRange,
                    title = "期望开始时间",
                    subtitle = scheduleMillis?.let(::formatScheduleSlotDisplay) ?: "请选择日期与时间",
                    onClick = { showScheduleSheet = true },
                )
            }
            Spacer(modifier = Modifier.height(SectionBlockVerticalGap))
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = DetailScreenTagDividerColor,
            )
            Spacer(modifier = Modifier.height(SectionBlockVerticalGap))
        }
        ServiceDetailSectionHeader(bookingSectionTitle)
        ServiceDetailChevronRow(
            icon = Icons.Outlined.EventBusy,
            title = "取消政策",
            subtitle = "出发前取消与改期规则说明",
            verticalPadding = BookingPolicyRowVerticalPadding,
            onClick = {
                policyReaderTitle = "取消政策"
                policyReaderBody =
                    "订单确认后，如需取消或改期请尽早联系发布者协商。具体违约金、免费取消时限与改期次数以双方沟通及订单约定为准；未达成一致前不产生强制扣款。"
                showPolicyReader = true
            },
        )
        Spacer(modifier = Modifier.height(8.dp))
        ServiceDetailChevronRow(
            icon = Icons.Outlined.Schedule,
            title = "集合与行程",
            subtitle = "集合地点、动线时长与人数上限",
            verticalPadding = BookingPolicyRowVerticalPadding,
            onClick = {
                policyReaderTitle = "集合与行程"
                policyReaderBody =
                    "请在预订后与发布者确认集合地点、可接待时段与人数上限。若服务含上门或到店，请提前说明交通与场地要求；实际动线可能因天气或路况微调。"
                showPolicyReader = true
            },
        )
        Spacer(modifier = Modifier.height(8.dp))
        ServiceDetailChevronRow(
            icon = Icons.Outlined.Shield,
            title = "人身安全与服务环境",
            subtitle = "安全提示与场所规范",
            verticalPadding = BookingPolicyRowVerticalPadding,
            onClick = {
                policyReaderTitle = "人身安全与服务环境"
                policyReaderBody =
                    "参与服务请注意人身与财物安全，遵守当地法规及场所管理要求。未成年人请在监护人陪同下参与；如身体不适或有特殊禁忌，请提前告知发布者。"
                showPolicyReader = true
            },
        )
        Spacer(modifier = Modifier.height(8.dp))
        ServiceDetailChevronRow(
            icon = Icons.Outlined.Article,
            title = "服务声明",
            subtitle = serviceDeclarationsSubtitle,
            verticalPadding = BookingPolicyRowVerticalPadding,
            onClick = {
                policyReaderTitle = "服务声明"
                policyReaderBody = serviceDeclarationsReaderBody
                showPolicyReader = true
            },
        )
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 1.dp,
            color = DetailScreenTagDividerColor,
        )
        Spacer(modifier = Modifier.height(SectionBlockVerticalGap))
    }

    if (showContentSheet) {
        ModalBottomSheet(
            onDismissRequest = { showContentSheet = false },
            sheetState = contentSheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = DialogTitleTopPadding, bottom = 8.dp),
            ) {
                Text(
                    text = contentSectionTitle,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(12.dp))
                contentOptions.forEachIndexed { index, opt ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectedContentIndex(index)
                                showContentSheet = false
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = index == safeIndex,
                            onClick = {
                                onSelectedContentIndex(index)
                                showContentSheet = false
                            },
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = opt.label,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                            )
                            Text(
                                text = "${opt.subtitle} · ${opt.priceText}",
                                fontSize = 13.sp,
                                color = RowSubtitleColor,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showScheduleSheet) {
        ModalBottomSheet(
            onDismissRequest = { showScheduleSheet = false },
            sheetState = scheduleSheetState,
            dragHandle = null,
        ) {
            key(scheduleMillis ?: 0L) {
                ServiceDetailSchedulePickerSheetContent(
                    initialMillis = scheduleMillis,
                    allowedWeekdays = scheduleAllowedWeekdays,
                    bookingTimeRangesJson = scheduleBookingTimeRangesJson,
                    bookingLeadHours = scheduleBookingLeadHours,
                    bookingFutureOpenDays = scheduleBookingFutureOpenDays,
                    hostCalendarServiceId = scheduleHostCalendarServiceId,
                    onDismiss = { showScheduleSheet = false },
                    onClear = {
                        onScheduleMillisChange(null)
                        showScheduleSheet = false
                    },
                    onConfirm = { millis ->
                        onScheduleMillisChange(millis)
                        showScheduleSheet = false
                    },
                )
            }
        }
    }

    if (showPolicyReader) {
        ModalBottomSheet(
            onDismissRequest = { showPolicyReader = false },
            sheetState = policySheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = DialogTitleTopPadding, bottom = 8.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = policyReaderTitle,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = policyReaderBody,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = Color(0xFF444444),
                )
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(
                    onClick = { showPolicyReader = false },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("关闭")
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceBookingSelectionSheet(
    contentOptions: List<ServiceContentPickOption>,
    selectedContentIndex: Int,
    onSelectedContentIndex: (Int) -> Unit,
    scheduleMillis: Long?,
    onScheduleMillisChange: (Long?) -> Unit,
    scheduleAllowedWeekdays: Set<Int>? = null,
    scheduleBookingTimeRangesJson: String = "",
    scheduleBookingLeadHours: Float = BookingTimeRangesCodec.DEFAULT_BOOKING_LEAD_HOURS,
    scheduleBookingFutureOpenDays: Int = BookingTimeRangesCodec.DEFAULT_BOOKING_FUTURE_OPEN_DAYS,
    scheduleHostCalendarServiceId: String? = null,
    extraFeeDescription: String? = null,
    onDismiss: () -> Unit,
    onConfirmBooking: () -> Unit,
) {
    var showContentSheet by remember { mutableStateOf(false) }
    var showScheduleSheet by remember { mutableStateOf(false) }
    val rootSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val contentSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scheduleSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val safeIndex = selectedContentIndex.coerceIn(0, (contentOptions.size - 1).coerceAtLeast(0))
    val picked = contentOptions.getOrNull(safeIndex)
        ?: ServiceContentPickOption(
            id = "fallback",
            label = "标准方案",
            subtitle = "",
            priceText = "价格面议",
            priceBasisText = "",
            durationDisplay = "",
        )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rootSheetState,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = DialogTitleTopPadding, bottom = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "选择预订信息",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111111),
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "关闭",
                        tint = Color(0xFF222222),
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            ServiceDetailChevronRow(
                icon = Icons.Outlined.Apps,
                title = "方案选择",
                subtitle = listOf(picked.label, picked.subtitle, picked.priceText)
                    .filter { it.isNotBlank() }
                    .joinToString(" · "),
                onClick = { showContentSheet = true },
                verticalPadding = BookingPolicyRowVerticalPadding,
            )
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = DetailScreenTagDividerColor,
            )
            Spacer(modifier = Modifier.height(24.dp))
            if (!extraFeeDescription.isNullOrBlank()) {
                ServiceDetailSectionHeader("额外费用说明")
                ServiceDetailInfoCard(text = extraFeeDescription)
            } else {
                ServiceDetailChevronRow(
                    icon = Icons.Outlined.DateRange,
                    title = "时间选择",
                    subtitle = scheduleMillis?.let(::formatScheduleSlotDisplay) ?: "请选择日期与时间",
                    onClick = { showScheduleSheet = true },
                    verticalPadding = BookingPolicyRowVerticalPadding,
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .clickable(onClick = onConfirmBooking),
                shape = RoundedCornerShape(26.dp),
                color = Color(0xFF111111),
            ) {
                Text(
                    text = "确认预订信息",
                    modifier = Modifier.padding(vertical = 16.dp),
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }

    if (showContentSheet) {
        ModalBottomSheet(
            onDismissRequest = { showContentSheet = false },
            sheetState = contentSheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = DialogTitleTopPadding, bottom = 8.dp),
            ) {
                Text(
                    text = "方案选择",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(12.dp))
                contentOptions.forEachIndexed { index, opt ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectedContentIndex(index)
                                showContentSheet = false
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = index == safeIndex,
                            onClick = {
                                onSelectedContentIndex(index)
                                showContentSheet = false
                            },
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = opt.label,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                            )
                            Text(
                                text = listOf(opt.subtitle, opt.priceText)
                                    .filter { it.isNotBlank() }
                                    .joinToString(" · "),
                                fontSize = 13.sp,
                                color = RowSubtitleColor,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showScheduleSheet) {
        ModalBottomSheet(
            onDismissRequest = { showScheduleSheet = false },
            sheetState = scheduleSheetState,
            dragHandle = null,
        ) {
            key(scheduleMillis ?: 0L) {
                ServiceDetailSchedulePickerSheetContent(
                    initialMillis = scheduleMillis,
                    allowedWeekdays = scheduleAllowedWeekdays,
                    bookingTimeRangesJson = scheduleBookingTimeRangesJson,
                    bookingLeadHours = scheduleBookingLeadHours,
                    bookingFutureOpenDays = scheduleBookingFutureOpenDays,
                    hostCalendarServiceId = scheduleHostCalendarServiceId,
                    onDismiss = { showScheduleSheet = false },
                    onClear = {
                        onScheduleMillisChange(null)
                        showScheduleSheet = false
                    },
                    onConfirm = { millis ->
                        onScheduleMillisChange(millis)
                        showScheduleSheet = false
                    },
                )
            }
        }
    }
}
