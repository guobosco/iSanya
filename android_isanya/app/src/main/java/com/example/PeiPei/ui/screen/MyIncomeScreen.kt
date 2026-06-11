// 文件说明：「我的收入」概述页（月/年视图、可滑动周期条、明细与对比区块）。

package com.example.Lulu.ui.screen

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ImageNotSupported
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.Lulu.data.repository.LuluRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import com.example.Lulu.data.model.Service
import java.time.LocalDate
import java.util.Locale

private val PageBg = Color(0xFFFFFFFF)
private val TitleInk = Color(0xFF000000)
private val Muted = Color(0xFF888888)
private val PaidPurple = Color(0xFF6B4EFF)
private val PendingPink = Color(0xFFE0115F)
private val IconCircleBg = Color(0xFFF0F0F0)
private val ChartLine = PaidPurple.copy(alpha = 0.85f)
private val HorizontalInset = 20.dp

private enum class IncomeGranularity { MONTH, YEAR }

private fun formatCny(amount: Double): String =
    String.format(Locale.CHINA, "¥%.2f CNY", amount)

@Composable
fun MyIncomeScreen(navController: NavController) {
    val today = remember { LocalDate.now() }
    val repository = remember { runCatching { LuluRepository.get() }.getOrNull() }
    val listingsFlow = remember(repository) { repository?.myCreatedListingsHub ?: flowOf(emptyList()) }
    val listings by listingsFlow.collectAsState(initial = emptyList())
    val publishedSamples = remember(listings) {
        listings.filter { !it.isDeleted && !it.isDraft }.take(6)
    }

    var granularity by rememberSaveable { mutableStateOf(IncomeGranularity.MONTH) }
    var year by rememberSaveable { mutableIntStateOf(today.year) }
    var month by rememberSaveable { mutableIntStateOf(today.monthValue) }

    var compareYearNew by rememberSaveable { mutableIntStateOf(today.year) }
    var compareYearOld by rememberSaveable { mutableIntStateOf(today.year - 1) }

    var projectSectionYear by rememberSaveable { mutableIntStateOf(today.year) }
    var projectSectionMonth by rememberSaveable { mutableIntStateOf(today.monthValue) }

    var showFutureIncome by rememberSaveable { mutableStateOf(true) }

    var paidDetailsExpanded by rememberSaveable { mutableStateOf(true) }
    var performanceExpanded by rememberSaveable { mutableStateOf(true) }

    var viewMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var compareMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var projectMonthMenuExpanded by rememberSaveable { mutableStateOf(false) }

    var periodPickerOpen by rememberSaveable { mutableStateOf(false) }
    var periodPickerWhich by rememberSaveable { mutableStateOf("overview") } // overview | project

    Scaffold(
        containerColor = PageBg,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item {
                IncomeTopHeader(
                    granularity = granularity,
                    viewMenuExpanded = viewMenuExpanded,
                    onViewMenuExpandedChange = { viewMenuExpanded = it },
                    onSelectMonthView = {
                        granularity = IncomeGranularity.MONTH
                        viewMenuExpanded = false
                    },
                    onSelectYearView = {
                        granularity = IncomeGranularity.YEAR
                        viewMenuExpanded = false
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            item {
                Spacer(Modifier.height(8.dp))
                IncomeChartPlaceholder(
                    modifier = Modifier.padding(horizontal = HorizontalInset),
                )
            }

            item {
                Spacer(Modifier.height(12.dp))
                PeriodScroller(
                    granularity = granularity,
                    year = year,
                    month = month,
                    onYearChange = { year = it },
                    onMonthChange = { month = it },
                    onOpenPicker = {
                        periodPickerWhich = "overview"
                        periodPickerOpen = true
                    },
                    syncListScrollToSelection = true,
                )
            }

            item {
                Spacer(Modifier.height(12.dp))
                OverviewSummaryCard(
                    modifier = Modifier.padding(horizontal = HorizontalInset),
                    year = year,
                    month = month,
                    granularity = granularity,
                    paidDetailsExpanded = paidDetailsExpanded,
                    performanceExpanded = performanceExpanded,
                    onTogglePaidDetails = { paidDetailsExpanded = !paidDetailsExpanded },
                    onTogglePerformance = { performanceExpanded = !performanceExpanded },
                )
            }

            item {
                Spacer(Modifier.height(28.dp))
                IncomeComparisonBlock(
                    modifier = Modifier.padding(horizontal = HorizontalInset),
                    compareYearNew = compareYearNew,
                    compareYearOld = compareYearOld,
                    compareMenuExpanded = compareMenuExpanded,
                    onCompareMenuExpandedChange = { compareMenuExpanded = it },
                    onPickCompare = { newer, older ->
                        compareYearNew = newer
                        compareYearOld = older
                        compareMenuExpanded = false
                    },
                    year = year,
                    month = month,
                    granularity = granularity,
                    onYearChange = { year = it },
                    onMonthChange = { month = it },
                    showFutureIncome = showFutureIncome,
                    onShowFutureIncomeChange = { showFutureIncome = it },
                    onOpenPeriodPicker = {
                        periodPickerWhich = "overview"
                        periodPickerOpen = true
                    },
                )
            }

            item {
                Spacer(Modifier.height(28.dp))
                IncomeByProjectSection(
                    modifier = Modifier.padding(horizontal = HorizontalInset),
                    year = projectSectionYear,
                    month = projectSectionMonth,
                    projectMonthMenuExpanded = projectMonthMenuExpanded,
                    onProjectMonthMenuExpandedChange = { projectMonthMenuExpanded = it },
                    onPickProjectMonth = { y, m ->
                        projectSectionYear = y
                        projectSectionMonth = m
                        projectMonthMenuExpanded = false
                    },
                    onOpenMonthPicker = {
                        periodPickerWhich = "project"
                        periodPickerOpen = true
                    },
                    samples = publishedSamples,
                )
            }
        }
    }

    if (periodPickerOpen) {
        PeriodPickDialog(
            granularity = granularity,
            initialYear = if (periodPickerWhich == "project") projectSectionYear else year,
            initialMonth = if (periodPickerWhich == "project") projectSectionMonth else month,
            onDismiss = { periodPickerOpen = false },
            onConfirm = { y, m ->
                if (periodPickerWhich == "project") {
                    projectSectionYear = y
                    projectSectionMonth = m
                } else {
                    year = y
                    month = m
                }
                periodPickerOpen = false
            },
        )
    }
}

@Composable
private fun IncomeTopHeader(
    granularity: IncomeGranularity,
    viewMenuExpanded: Boolean,
    onViewMenuExpandedChange: (Boolean) -> Unit,
    onSelectMonthView: () -> Unit,
    onSelectYearView: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, start = 4.dp, end = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = TitleInk)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = "概述",
                    color = TitleInk,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                )
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onViewMenuExpandedChange(true) }
                            .padding(vertical = 4.dp, horizontal = 0.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (granularity == IncomeGranularity.MONTH) "月视图" else "年视图",
                            color = TitleInk,
                            fontSize = 15.sp,
                        )
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = TitleInk,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    DropdownMenu(
                        expanded = viewMenuExpanded,
                        onDismissRequest = { onViewMenuExpandedChange(false) },
                    ) {
                        DropdownMenuItem(
                            text = { Text("月视图") },
                            onClick = onSelectMonthView,
                        )
                        DropdownMenuItem(
                            text = { Text("年视图") },
                            onClick = onSelectYearView,
                        )
                    }
                }
            }
            Row {
                IconButton(onClick = { /* TODO 筛选 */ }) {
                    Surface(shape = CircleShape, color = IconCircleBg, modifier = Modifier.size(40.dp)) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(Icons.Default.Tune, contentDescription = "筛选", tint = TitleInk, modifier = Modifier.size(20.dp))
                        }
                    }
                }
                IconButton(onClick = { /* TODO 分享 */ }) {
                    Surface(shape = CircleShape, color = IconCircleBg, modifier = Modifier.size(40.dp)) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(Icons.Default.Share, contentDescription = "分享", tint = TitleInk, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IncomeChartPlaceholder(modifier: Modifier = Modifier) {
    val grid = listOf(10, 5, 0)
    Row(modifier.height(140.dp)) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                grid.forEachIndexed { idx, _ ->
                    val y = h * (idx / 2f.coerceAtLeast(0.001f)) * 0.33f + h * 0.08f
                    drawLine(
                        color = Color(0x11000000),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                val y0 = h * 0.83f
                drawLine(
                    color = ChartLine,
                    start = Offset(0f, y0),
                    end = Offset(w, y0),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f),
                )
            }
        }
        Column(
            modifier = Modifier.width(72.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End,
        ) {
            grid.forEach { v ->
                Text(
                    text = "¥$v CNY",
                    fontSize = 11.sp,
                    color = Muted,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

@Composable
private fun PeriodScroller(
    granularity: IncomeGranularity,
    year: Int,
    month: Int,
    onYearChange: (Int) -> Unit,
    onMonthChange: (Int) -> Unit,
    onOpenPicker: () -> Unit,
    syncListScrollToSelection: Boolean = true,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val months = remember(year) { (1..12).toList() }
    val years = remember(year) {
        val c = year.coerceIn(2018, 2035)
        (c - 4..c + 2).toList()
    }

    if (syncListScrollToSelection) {
        LaunchedEffect(granularity, year, month) {
            when (granularity) {
                IncomeGranularity.MONTH -> {
                    val idx = (month - 1).coerceIn(0, 11)
                    scope.launch { listState.animateScrollToItem(idx) }
                }
                IncomeGranularity.YEAR -> {
                    val idx = years.indexOf(year).coerceAtLeast(0)
                    scope.launch { listState.animateScrollToItem(idx) }
                }
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = {
                when (granularity) {
                    IncomeGranularity.MONTH -> {
                        if (month <= 1) {
                            onYearChange(year - 1)
                            onMonthChange(12)
                        } else {
                            onMonthChange(month - 1)
                        }
                    }
                    IncomeGranularity.YEAR -> onYearChange(year - 1)
                }
            },
        ) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "上一段", tint = TitleInk)
        }

        LazyRow(
            state = listState,
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
        ) {
            when (granularity) {
                IncomeGranularity.MONTH -> {
                    itemsIndexed(months, key = { _, m -> year * 100 + m }) { _, m ->
                        val selected = m == month
                        PeriodChip(
                            label = "${m}月",
                            selected = selected,
                            onClick = {
                                onMonthChange(m)
                                scope.launch { listState.animateScrollToItem((m - 1).coerceIn(0, 11)) }
                            },
                        )
                    }
                }
                IncomeGranularity.YEAR -> {
                    itemsIndexed(years, key = { _, y -> y }) { _, y ->
                        val selected = y == year
                        PeriodChip(
                            label = "${y}年",
                            selected = selected,
                            onClick = {
                                onYearChange(y)
                                val idx = years.indexOf(y).coerceAtLeast(0)
                                scope.launch { listState.animateScrollToItem(idx) }
                            },
                        )
                    }
                }
            }
        }

        IconButton(onClick = onOpenPicker) {
            Icon(Icons.Outlined.CalendarMonth, contentDescription = "选择日期", tint = TitleInk)
        }

        IconButton(
            onClick = {
                when (granularity) {
                    IncomeGranularity.MONTH -> {
                        if (month >= 12) {
                            onYearChange(year + 1)
                            onMonthChange(1)
                        } else {
                            onMonthChange(month + 1)
                        }
                    }
                    IncomeGranularity.YEAR -> onYearChange(year + 1)
                }
            },
        ) {
            Icon(Icons.Default.ChevronRight, contentDescription = "下一段", tint = TitleInk)
        }
    }
}

@Composable
private fun PeriodChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) TitleInk else Color.Transparent
    val fg = if (selected) Color.White else TitleInk
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, color = fg, fontSize = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun OverviewSummaryCard(
    modifier: Modifier = Modifier,
    year: Int,
    month: Int,
    granularity: IncomeGranularity,
    paidDetailsExpanded: Boolean,
    performanceExpanded: Boolean,
    onTogglePaidDetails: () -> Unit,
    onTogglePerformance: () -> Unit,
) {
    val title = when (granularity) {
        IncomeGranularity.MONTH -> "${year}年${month}月"
        IncomeGranularity.YEAR -> "${year}年"
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        tonalElevation = 0.dp,
        shadowElevation = 1.dp,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = TitleInk)

            Spacer(Modifier.height(14.dp))

            MoneyRow(
                leadingSquareColor = PaidPurple,
                hollow = false,
                label = "已支付",
                value = formatCny(0.0),
            )
            Spacer(Modifier.height(10.dp))
            MoneyRow(
                leadingSquareColor = PendingPink,
                hollow = true,
                label = "待支付",
                value = formatCny(0.0),
            )

            HorizontalDivider(Modifier.padding(vertical = 14.dp), color = Color(0x14000000))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("总计 (CNY)", color = TitleInk, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text(formatCny(0.0), color = TitleInk, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(16.dp))

            CollapsibleHeader(
                title = "已支付款项明细",
                expanded = paidDetailsExpanded,
                onClick = onTogglePaidDetails,
            )
            if (paidDetailsExpanded) {
                Column(Modifier.animateContentSize()) {
                    Spacer(Modifier.height(8.dp))
                    SubMoneyRow("总收入", formatCny(0.0))
                    SubMoneyRow("调整", formatCny(0.0))
                    SubMoneyRow("服务费", formatCny(0.0))
                    SubMoneyRow("预扣税", formatCny(0.0))
                    HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Color(0x14000000))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("已支付合计 (CNY)", fontSize = 13.sp, color = Muted)
                        Text(formatCny(0.0), fontSize = 13.sp, color = TitleInk, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            CollapsibleHeader(
                title = "表现统计数据",
                expanded = performanceExpanded,
                onClick = onTogglePerformance,
            )
            if (performanceExpanded) {
                Column(Modifier.animateContentSize()) {
                    Text(
                        "未来日期不适用",
                        fontSize = 12.sp,
                        color = Muted,
                        modifier = Modifier.padding(top = 6.dp, bottom = 8.dp),
                    )
                    StatRow("预订晚数", "0")
                    StatRow("平均住宿晚数", "0")
                }
            }
        }
    }
}

@Composable
private fun CollapsibleHeader(title: String, expanded: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontWeight = FontWeight.Medium, color = TitleInk, fontSize = 15.sp)
        Icon(
            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            tint = TitleInk,
        )
    }
}

@Composable
private fun MoneyRow(
    leadingSquareColor: Color,
    hollow: Boolean,
    label: String,
    value: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .then(
                    if (hollow) Modifier.border(2.dp, leadingSquareColor, RoundedCornerShape(2.dp))
                    else Modifier.background(leadingSquareColor, RoundedCornerShape(2.dp)),
                ),
        )
        Spacer(Modifier.width(10.dp))
        Text(label, color = TitleInk, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Text(value, color = TitleInk, fontSize = 15.sp)
    }
}

@Composable
private fun SubMoneyRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 14.sp, color = Muted)
        Text(value, fontSize = 14.sp, color = TitleInk)
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 14.sp, color = TitleInk)
        Text(value, fontSize = 14.sp, color = TitleInk, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun IncomeComparisonBlock(
    modifier: Modifier = Modifier,
    compareYearNew: Int,
    compareYearOld: Int,
    compareMenuExpanded: Boolean,
    onCompareMenuExpandedChange: (Boolean) -> Unit,
    onPickCompare: (Int, Int) -> Unit,
    year: Int,
    month: Int,
    granularity: IncomeGranularity,
    onYearChange: (Int) -> Unit,
    onMonthChange: (Int) -> Unit,
    showFutureIncome: Boolean,
    onShowFutureIncomeChange: (Boolean) -> Unit,
    onOpenPeriodPicker: () -> Unit,
) {
    Column(modifier) {
        Text("收入比较", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TitleInk)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onCompareMenuExpandedChange(true) }
                        .padding(vertical = 4.dp),
                ) {
                    Text(
                        "$compareYearNew 对比 $compareYearOld",
                        color = TitleInk,
                        fontSize = 14.sp,
                    )
                    Icon(Icons.Default.ExpandMore, null, tint = TitleInk, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(
                    expanded = compareMenuExpanded,
                    onDismissRequest = { onCompareMenuExpandedChange(false) },
                ) {
                    DropdownMenuItem(
                        text = { Text("2026 对比 2025") },
                        onClick = { onPickCompare(2026, 2025) },
                    )
                    DropdownMenuItem(
                        text = { Text("2025 对比 2024") },
                        onClick = { onPickCompare(2025, 2024) },
                    )
                    DropdownMenuItem(
                        text = { Text("2024 对比 2023") },
                        onClick = { onPickCompare(2024, 2023) },
                    )
                }
            }
            Row {
                IconButton(onClick = { /* TODO */ }) {
                    Surface(shape = CircleShape, color = IconCircleBg, modifier = Modifier.size(36.dp)) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Apps, contentDescription = null, tint = TitleInk, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                IconButton(onClick = { /* TODO */ }) {
                    Surface(shape = CircleShape, color = IconCircleBg, modifier = Modifier.size(36.dp)) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = TitleInk, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        IncomeChartPlaceholder(Modifier.fillMaxWidth())

        Spacer(Modifier.height(8.dp))
        PeriodScroller(
            granularity = granularity,
            year = year,
            month = month,
            onYearChange = onYearChange,
            onMonthChange = onMonthChange,
            onOpenPicker = onOpenPeriodPicker,
            syncListScrollToSelection = false,
        )

        Spacer(Modifier.height(12.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(PendingPink))
                    Spacer(Modifier.width(8.dp))
                    Text("$compareYearNew", fontSize = 14.sp, color = TitleInk, modifier = Modifier.weight(1f))
                    Text(formatCny(0.0), fontSize = 14.sp, color = TitleInk)
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(PaidPurple))
                    Spacer(Modifier.width(8.dp))
                    Text("$compareYearOld", fontSize = 14.sp, color = TitleInk, modifier = Modifier.weight(1f))
                    Text(formatCny(0.0), fontSize = 14.sp, color = TitleInk)
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("显示未来收入", fontSize = 15.sp, color = TitleInk)
                        Text("仅含已确认订单", fontSize = 12.sp, color = Muted)
                    }
                    Switch(checked = showFutureIncome, onCheckedChange = onShowFutureIncomeChange)
                }
            }
        }
    }
}

@Composable
private fun IncomeByProjectSection(
    modifier: Modifier = Modifier,
    year: Int,
    month: Int,
    projectMonthMenuExpanded: Boolean,
    onProjectMonthMenuExpandedChange: (Boolean) -> Unit,
    onPickProjectMonth: (Int, Int) -> Unit,
    onOpenMonthPicker: () -> Unit,
    samples: List<Service>,
) {
    Column(modifier) {
        Text("按项目查看收入", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TitleInk)
        Spacer(Modifier.height(6.dp))
        Box {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onProjectMonthMenuExpandedChange(true) }
                    .padding(vertical = 4.dp),
            ) {
                Text("${year}年${month}月", fontSize = 14.sp, color = TitleInk)
                Icon(Icons.Default.ExpandMore, null, tint = TitleInk, modifier = Modifier.size(18.dp))
            }
            DropdownMenu(
                expanded = projectMonthMenuExpanded,
                onDismissRequest = { onProjectMonthMenuExpandedChange(false) },
            ) {
                (1..12).forEach { m ->
                    DropdownMenuItem(
                        text = { Text("${year}年${m}月") },
                        onClick = { onPickProjectMonth(year, m) },
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        TextButton(
            onClick = onOpenMonthPicker,
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
        ) {
            Text("更多年月…", fontSize = 13.sp)
        }

        Spacer(Modifier.height(4.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(vertical = 8.dp)) {
                if (samples.isEmpty()) {
                    Text(
                        "暂无已发布项目",
                        modifier = Modifier.padding(16.dp),
                        color = Muted,
                        fontSize = 14.sp,
                    )
                } else {
                    samples.forEach { s ->
                        ProjectIncomeRow(
                            title = s.title,
                            percentLabel = "0%",
                            amount = formatCny(0.0),
                            imageUrl = s.coverImageUrl.takeIf { it.isNotBlank() } ?: s.imageUrls.firstOrNull(),
                        )
                        HorizontalDivider(color = Color(0x0A000000), thickness = 0.5.dp)
                    }
                }
                ProjectIncomeRow(
                    title = "非项目收入",
                    percentLabel = "—",
                    amount = formatCny(0.0),
                    imageUrl = null,
                    placeholder = true,
                )
            }
        }
        Text(
            "仅包含所选期间内已支付的收入。",
            fontSize = 11.sp,
            color = Muted,
            modifier = Modifier.padding(top = 8.dp, start = 2.dp),
        )
    }
}

@Composable
private fun ProjectIncomeRow(
    title: String,
    percentLabel: String,
    amount: String,
    imageUrl: String?,
    placeholder: Boolean = false,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = false) { }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF2F2F2)),
            contentAlignment = Alignment.Center,
        ) {
            when {
                placeholder -> Icon(Icons.Outlined.ImageNotSupported, null, tint = Muted, modifier = Modifier.size(26.dp))
                !imageUrl.isNullOrBlank() -> AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                else -> Icon(Icons.Outlined.ImageNotSupported, null, tint = Muted, modifier = Modifier.size(26.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 15.sp, color = TitleInk)
            Text(percentLabel, fontSize = 12.sp, color = Muted)
        }
        Text(amount, fontSize = 14.sp, color = TitleInk, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun PeriodPickDialog(
    granularity: IncomeGranularity,
    initialYear: Int,
    initialMonth: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
) {
    var y by remember { mutableIntStateOf(initialYear) }
    var m by remember { mutableIntStateOf(initialMonth.coerceIn(1, 12)) }
    val monthScroll = rememberScrollState()

    LaunchedEffect(initialYear, initialMonth, granularity) {
        y = initialYear
        m = initialMonth.coerceIn(1, 12)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (granularity == IncomeGranularity.MONTH) "选择年月" else "选择年") },
        text = {
            Column(Modifier.heightIn(max = 400.dp)) {
                Text("年", fontSize = 13.sp, color = Muted)
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    (2018..2035).forEach { yearOption ->
                        val sel = yearOption == y
                        TextButton(onClick = { y = yearOption }) {
                            Text(
                                "$yearOption",
                                color = if (sel) MaterialTheme.colorScheme.primary else TitleInk,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }
                }
                if (granularity == IncomeGranularity.MONTH) {
                    Spacer(Modifier.height(12.dp))
                    Text("月", fontSize = 13.sp, color = Muted)
                    Column(Modifier.verticalScroll(monthScroll)) {
                        (1..12).chunked(3).forEach { rowMonths ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                rowMonths.forEach { monthOption ->
                                    val sel = monthOption == m
                                    TextButton(
                                        onClick = { m = monthOption },
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text(
                                            "${monthOption}月",
                                            color = if (sel) MaterialTheme.colorScheme.primary else TitleInk,
                                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                        )
                                    }
                                }
                                repeat(3 - rowMonths.size) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(y, if (granularity == IncomeGranularity.MONTH) m else 1)
                },
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}