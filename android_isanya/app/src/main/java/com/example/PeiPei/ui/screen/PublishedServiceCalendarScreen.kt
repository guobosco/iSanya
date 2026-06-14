// 文件说明：发布者「预定日历」——按月网格；在开放日期范围内点击某日弹出可订设置，格内展示可订/不可订摘要；超出开放天数显示「未开放预订」并跳转接单时段设置。

package com.example.Lulu.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.Lulu.data.local.AppDataStore
import com.example.Lulu.data.model.HostCalendarDayClosure
import com.example.Lulu.data.model.HostServiceBooking
import com.example.Lulu.ui.components.CommonAvatar
import com.example.Lulu.ui.navigation.Screen
import com.example.Lulu.ui.theme.BrandPink
import com.example.Lulu.ui.theme.DialogTitleTopPadding
import com.example.Lulu.util.BookingTimeRangesCodec
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PageBg = Color(0xFFFFFFFF)
private val CardBg = Color(0xFFF3F3F5)
private val Muted = Color(0xFF888888)
private val TitleInk = Color(0xFF000000)
private val WeekdayInk = Color(0xFF666666)
private val MonthCountAhead = 12

private enum class CalendarDayBookSheetMode { OPEN, ALL_DAY, WINDOW }

private fun extractDisplayPrice(priceText: String): String {
    val t = priceText.trim()
    if (t.isEmpty()) return "—"
    val m = Regex("""[¥￥]?\s*([\d,]+(?:\.\d+)?)""").find(t)
    return if (m != null) "¥${m.groupValues[1].replace(",", "")}" else t.take(10)
}

private fun sundayFirstOffset(firstOfMonth: LocalDate): Int {
    val dow = firstOfMonth.dayOfWeek
    return when (dow) {
        DayOfWeek.SUNDAY -> 0
        DayOfWeek.MONDAY -> 1
        DayOfWeek.TUESDAY -> 2
        DayOfWeek.WEDNESDAY -> 3
        DayOfWeek.THURSDAY -> 4
        DayOfWeek.FRIDAY -> 5
        DayOfWeek.SATURDAY -> 6
    }
}

/** 与访客预定日期选择一致：含今天共 [bookingFutureOpenDays] 天。 */
private fun lastBookableDateInclusive(today: LocalDate, bookingFutureOpenDays: Int): LocalDate {
    val n = BookingTimeRangesCodec.normalizeBookingFutureOpenDays(bookingFutureOpenDays).coerceAtLeast(1)
    return today.plusDays((n - 1).toLong())
}

private fun availabilityCaption(closure: HostCalendarDayClosure?): String {
    if (closure == null) return "可订"
    return if (closure.allDay) {
        "全天不可订"
    } else {
        val s = closure.windowStart.ifBlank { "?" }
        val e = closure.windowEnd.ifBlank { "?" }
        "${s}-${e}不可订"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishedServiceCalendarScreen(
    navController: NavController,
    serviceId: String,
) {
    val services by AppDataStore.services.collectAsState()
    val service = remember(services, serviceId) { services.find { it.id == serviceId } }
    val bookingMap by AppDataStore.hostServiceBookings.collectAsState()
    val bookingsByDate = remember(bookingMap, serviceId) {
        bookingMap[serviceId].orEmpty().associateBy { it.dateIso }
    }
    val closureRoot by AppDataStore.hostCalendarDayClosures.collectAsState()
    val closuresByDate = remember(closureRoot, serviceId) {
        closureRoot[serviceId].orEmpty()
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedDate by rememberSaveable { mutableStateOf<String?>(null) }
    var showDayBookSheet by remember { mutableStateOf(false) }
    var sheetMode by remember { mutableIntStateOf(CalendarDayBookSheetMode.OPEN.ordinal) }
    var sheetWindowStart by remember { mutableStateOf("09:00") }
    var sheetWindowEnd by remember { mutableStateOf("18:00") }
    var startMenuOpen by remember { mutableStateOf(false) }
    var endMenuOpen by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val today = remember { LocalDate.now() }
    LaunchedEffect(serviceId) {
        AppDataStore.ensureDemoHostBookingsIfEmpty(serviceId)
        selectedDate = today.toString()
    }

    if (service == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PageBg),
            contentAlignment = Alignment.Center,
        ) {
            Text("未找到该服务", color = Muted, fontSize = 15.sp)
        }
        return
    }

    val priceLabel = remember(service.priceText) { extractDisplayPrice(service.priceText) }
    val lastBookable = remember(today, service.bookingFutureOpenDays) {
        lastBookableDateInclusive(today, service.bookingFutureOpenDays)
    }
    val monthTitleFmt = remember {
        DateTimeFormatter.ofPattern("LLLL", Locale.forLanguageTag("zh-CN"))
    }
    val daySheetTitleFmt = remember {
        DateTimeFormatter.ofPattern("M月d日", Locale.forLanguageTag("zh-CN"))
    }

    LaunchedEffect(showDayBookSheet, selectedDate, closuresByDate) {
        if (!showDayBookSheet) return@LaunchedEffect
        val iso = selectedDate ?: return@LaunchedEffect
        val c = closuresByDate[iso]
        when {
            c == null -> {
                sheetMode = CalendarDayBookSheetMode.OPEN.ordinal
                sheetWindowStart = "09:00"
                sheetWindowEnd = "18:00"
            }
            c.allDay -> {
                sheetMode = CalendarDayBookSheetMode.ALL_DAY.ordinal
            }
            else -> {
                sheetMode = CalendarDayBookSheetMode.WINDOW.ordinal
                sheetWindowStart = c.windowStart.ifBlank { "09:00" }
                sheetWindowEnd = c.windowEnd.ifBlank { "18:00" }
            }
        }
    }

    Scaffold(
        containerColor = PageBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("预定日历", color = TitleInk, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = service.title.ifBlank { "未命名服务" },
                            color = Muted,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = TitleInk)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            navController.navigate(
                                Screen.CreateService.createRoute(serviceId = service.id, openBooking = true),
                            ) { launchSingleTop = true }
                        },
                    ) {
                        Icon(Icons.Outlined.Settings, contentDescription = "日历与接单时段设置", tint = TitleInk)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = PageBg),
            )
        },
        floatingActionButton = {
            SmallFloatingActionButton(
                onClick = {
                    scope.launch { listState.animateScrollToItem(0) }
                },
                containerColor = Color.White,
                contentColor = TitleInk,
            ) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "回到顶部")
            }
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(PageBg)
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(MonthCountAhead + 1) { mi ->
                val ym = YearMonth.from(today).plusMonths(mi.toLong())
                MonthCalendarBlock(
                    yearMonth = ym,
                    today = today,
                    lastBookable = lastBookable,
                    selectedDateIso = selectedDate,
                    priceLabel = priceLabel,
                    bookingsByDate = bookingsByDate,
                    closuresByDate = closuresByDate,
                    monthTitleFormatter = monthTitleFmt,
                    onCalendarDayClick = { d ->
                        when {
                            d.isBefore(today) -> Unit
                            d.isAfter(lastBookable) -> {
                                navController.navigate(
                                    Screen.CreateService.createRoute(serviceId = service.id, openBooking = true),
                                ) { launchSingleTop = true }
                            }
                            else -> {
                                selectedDate = d.toString()
                                showDayBookSheet = true
                            }
                        }
                    },
                    onAvatarClick = { orderId ->
                        navController.navigate(Screen.HostServiceOrderDetail.createRoute(orderId)) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }

    if (showDayBookSheet && selectedDate != null) {
        val iso = selectedDate!!
        val parsedDay = runCatching { LocalDate.parse(iso) }.getOrNull()
        ModalBottomSheet(
            onDismissRequest = { showDayBookSheet = false },
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp)
                    .padding(top = DialogTitleTopPadding, bottom = 28.dp),
            ) {
                Text(
                    text = if (parsedDay != null) {
                        "${parsedDay.format(daySheetTitleFmt)} · 可订设置"
                    } else {
                        "可订设置"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TitleInk,
                )
                Spacer(modifier = Modifier.height(16.dp))
                CalendarBookModeRow(
                    label = "可订",
                    selected = sheetMode == CalendarDayBookSheetMode.OPEN.ordinal,
                    onClick = { sheetMode = CalendarDayBookSheetMode.OPEN.ordinal },
                )
                CalendarBookModeRow(
                    label = "全天不可订",
                    selected = sheetMode == CalendarDayBookSheetMode.ALL_DAY.ordinal,
                    onClick = { sheetMode = CalendarDayBookSheetMode.ALL_DAY.ordinal },
                )
                CalendarBookModeRow(
                    label = "分时段不可订",
                    selected = sheetMode == CalendarDayBookSheetMode.WINDOW.ordinal,
                    onClick = { sheetMode = CalendarDayBookSheetMode.WINDOW.ordinal },
                )
                if (sheetMode == CalendarDayBookSheetMode.WINDOW.ordinal) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("不可订时段", fontSize = 13.sp, color = Muted)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("从", fontSize = 14.sp, color = TitleInk)
                        Box {
                            TextButton(onClick = { startMenuOpen = true }) { Text(sheetWindowStart) }
                            DropdownMenu(expanded = startMenuOpen, onDismissRequest = { startMenuOpen = false }) {
                                BookingTimeRangesCodec.halfHourStartChoices().forEach { t ->
                                    DropdownMenuItem(
                                        text = { Text(t) },
                                        onClick = {
                                            sheetWindowStart = t
                                            val ends = BookingTimeRangesCodec.endOptionsForStart(t)
                                            if (sheetWindowEnd !in ends) {
                                                sheetWindowEnd = ends.firstOrNull() ?: sheetWindowEnd
                                            }
                                            startMenuOpen = false
                                        },
                                    )
                                }
                            }
                        }
                        Text("至", fontSize = 14.sp, color = TitleInk)
                        Box {
                            TextButton(onClick = { endMenuOpen = true }) { Text(sheetWindowEnd) }
                            DropdownMenu(expanded = endMenuOpen, onDismissRequest = { endMenuOpen = false }) {
                                BookingTimeRangesCodec.endOptionsForStart(sheetWindowStart).forEach { t ->
                                    DropdownMenuItem(
                                        text = { Text(t) },
                                        onClick = {
                                            sheetWindowEnd = t
                                            endMenuOpen = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(22.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = { showDayBookSheet = false }) { Text("取消") }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            when (sheetMode) {
                                CalendarDayBookSheetMode.OPEN.ordinal -> {
                                    AppDataStore.setHostCalendarDayClosure(serviceId, iso, null)
                                }
                                CalendarDayBookSheetMode.ALL_DAY.ordinal -> {
                                    AppDataStore.setHostCalendarDayClosure(
                                        serviceId,
                                        iso,
                                        HostCalendarDayClosure(allDay = true),
                                    )
                                }
                                else -> {
                                    val sm = BookingTimeRangesCodec.parseMinutes(sheetWindowStart)
                                    val em = BookingTimeRangesCodec.parseMinutes(sheetWindowEnd)
                                    if (sm == null || em == null || em <= sm) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("请选择有效的开始与结束时间")
                                        }
                                        return@TextButton
                                    }
                                    AppDataStore.setHostCalendarDayClosure(
                                        serviceId,
                                        iso,
                                        HostCalendarDayClosure(
                                            allDay = false,
                                            windowStart = sheetWindowStart,
                                            windowEnd = sheetWindowEnd,
                                        ),
                                    )
                                }
                            }
                            showDayBookSheet = false
                        },
                    ) {
                        Text("确定", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarBookModeRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontSize = 15.sp, color = TitleInk)
    }
}

@Composable
private fun MonthCalendarBlock(
    yearMonth: YearMonth,
    today: LocalDate,
    lastBookable: LocalDate,
    selectedDateIso: String?,
    priceLabel: String,
    bookingsByDate: Map<String, HostServiceBooking>,
    closuresByDate: Map<String, HostCalendarDayClosure>,
    monthTitleFormatter: DateTimeFormatter,
    onCalendarDayClick: (LocalDate) -> Unit,
    onAvatarClick: (String) -> Unit,
) {
    val first = yearMonth.atDay(1)
    val lastDay = yearMonth.lengthOfMonth()
    val offset = sundayFirstOffset(first)
    val totalCells = ((offset + lastDay + 6) / 7) * 7
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = first.format(monthTitleFormatter),
                color = TitleInk,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(0.dp)) {
            val weekLabels = listOf("日", "一", "二", "三", "四", "五", "六")
            weekLabels.forEach { w ->
                Text(
                    text = w,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = WeekdayInk,
                    fontSize = 12.sp,
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        for (row in 0 until totalCells / 7) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                for (col in 0 until 7) {
                    val idx = row * 7 + col
                    val dayNum = idx - offset + 1
                    val inMonth = dayNum in 1..lastDay
                    val date = if (inMonth) yearMonth.atDay(dayNum) else null
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(102.dp),
                    ) {
                        if (date != null) {
                            val iso = date.toString()
                            val booking = bookingsByDate[iso]
                            val isPast = date.isBefore(today)
                            val isBeyondBooking = !isPast && date.isAfter(lastBookable)
                            val isSelected = iso == selectedDateIso
                            val closure = closuresByDate[iso]
                            HostDayPriceCell(
                                dayOfMonth = dayNum,
                                isPast = isPast,
                                isBeyondBookingWindow = isBeyondBooking,
                                isSelected = isSelected,
                                priceLabel = priceLabel,
                                availabilityCaption = availabilityCaption(closure),
                                booking = if (isPast) null else booking,
                                onCellClick = { onCalendarDayClick(date) },
                                onAvatarClick = booking?.takeIf { !isPast }?.let { b -> { onAvatarClick(b.orderId) } },
                            )
                        }
                    }
                }
            }
        }
        HorizontalDivider(color = Color(0xFFE8E8E8), thickness = 1.dp, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun HostDayPriceCell(
    dayOfMonth: Int,
    isPast: Boolean,
    isBeyondBookingWindow: Boolean,
    isSelected: Boolean,
    priceLabel: String,
    availabilityCaption: String,
    booking: HostServiceBooking?,
    onCellClick: () -> Unit,
    onAvatarClick: (() -> Unit)?,
) {
    val openGreen = Color(0xFF2E7D32)
    val closedInk = Color(0xFFB85C00)
    val beyondBg = Color(0xFFFFF6E8)
    val captionColor = when {
        isBeyondBookingWindow -> Color(0xFF8A5A00)
        availabilityCaption == "可订" -> openGreen
        else -> closedInk
    }
    val surfaceColor = when {
        isPast -> Color(0xFFF7F7F8)
        isBeyondBookingWindow -> beyondBg
        else -> CardBg
    }
    val cellModifier = Modifier
        .fillMaxSize()
        .clip(RoundedCornerShape(10.dp))
        .then(
            if (!isPast) {
                Modifier.clickable(onClick = onCellClick)
            } else {
                Modifier
            },
        )
    Surface(
        modifier = cellModifier,
        color = surfaceColor,
        shape = RoundedCornerShape(10.dp),
        shadowElevation = 0.dp,
    ) {
        if (isPast) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = dayOfMonth.toString(),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFBBBBBB),
                )
            }
        } else if (isBeyondBookingWindow) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 6.dp, horizontal = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = dayOfMonth.toString(),
                    fontSize = 15.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = TitleInk,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "未开放预订",
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                    color = captionColor,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 4.dp, horizontal = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier.size(30.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(BrandPink, CircleShape),
                        )
                    }
                    Text(
                        text = dayOfMonth.toString(),
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isSelected) Color.White else TitleInk,
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f, fill = true)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    if (booking != null) {
                        CommonAvatar(
                            imageUrl = booking.guestPhotoUrl.ifBlank { null },
                            name = booking.guestName,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .clickable(
                                    onClick = {
                                        onAvatarClick?.invoke()
                                    },
                                ),
                            shape = CircleShape,
                            textStyle = androidx.compose.material3.MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                        )
                    }
                }
                Text(
                    text = availabilityCaption,
                    fontSize = 9.sp,
                    lineHeight = 10.sp,
                    color = captionColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = priceLabel,
                    fontSize = 9.sp,
                    color = Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
