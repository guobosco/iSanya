// 文件说明：发布服务——可预约时段多段选择（底部弹层样式与价格/接单等表单一致）。

package com.example.Lulu.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.Lulu.ui.theme.DialogTitleTopPadding
import com.example.Lulu.util.BookingTimeRangesCodec
import com.example.Lulu.util.BookingTimeSlot
import kotlin.math.abs

private fun ensureValidEnd(start: String, end: String): String {
    val options = BookingTimeRangesCodec.endOptionsForStart(start)
    if (options.isEmpty()) return start
    val em = BookingTimeRangesCodec.parseMinutes(end)
    val sm = BookingTimeRangesCodec.parseMinutes(start)
    if (em != null && sm != null && em > sm && end in options) return end
    return options.first()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingTimeRangesPickerBottomSheet(
    initialSlots: List<BookingTimeSlot>,
    initialLeadHours: Float = BookingTimeRangesCodec.DEFAULT_BOOKING_LEAD_HOURS,
    initialFutureOpenDays: Int = BookingTimeRangesCodec.DEFAULT_BOOKING_FUTURE_OPEN_DAYS,
    surfaceColor: Color,
    onBackgroundColor: Color,
    onSurfaceVariantColor: Color,
    dividerColor: Color,
    accentColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (List<BookingTimeSlot>, Float, Int) -> Unit,
) {
    val context = LocalContext.current
    var draft by remember(initialSlots) { mutableStateOf(initialSlots) }
    var draftLead by remember(initialLeadHours) { mutableStateOf(initialLeadHours) }
    var draftFutureDays by remember(initialFutureOpenDays) {
        mutableStateOf(BookingTimeRangesCodec.normalizeBookingFutureOpenDays(initialFutureOpenDays))
    }
    var timePick by remember { mutableStateOf<Pair<Int, Boolean>?>(null) }

    val chipColors = FilterChipDefaults.filterChipColors(
        containerColor = Color.Transparent,
        labelColor = onBackgroundColor,
        selectedContainerColor = accentColor.copy(alpha = 0.18f),
        selectedLabelColor = accentColor,
        iconColor = onSurfaceVariantColor,
        selectedLeadingIconColor = accentColor,
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.88f),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = surfaceColor,
                shadowElevation = 16.dp,
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding(),
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp)
                                    .padding(top = DialogTitleTopPadding),
                            ) {
                                Text(
                                    text = "时间设置",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = onBackgroundColor,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "设置最早可预定时间、每日开放接单时段，以及开放选择日期的范围。",
                                    fontSize = 14.sp,
                                    lineHeight = 22.sp,
                                    color = onSurfaceVariantColor,
                                )
                            }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(end = 4.dp, top = DialogTitleTopPadding),
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "关闭", tint = onBackgroundColor)
                            }
                        }

                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(0.dp),
                        ) {
                            item {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "至少需提前预定",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = onBackgroundColor,
                                )
                                Text(
                                    text = "用户发起预定须早于计划开始时间至少以下时长。默认可随时预定（无需提前）。",
                                    fontSize = 14.sp,
                                    lineHeight = 22.sp,
                                    color = onSurfaceVariantColor,
                                    modifier = Modifier.padding(top = 6.dp, bottom = 10.dp),
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    BookingTimeRangesCodec.BOOKING_LEAD_HOUR_PRESETS.forEach { preset ->
                                        val label = if (preset <= 0f) {
                                            "无需提前"
                                        } else {
                                            "${BookingTimeRangesCodec.formatLeadHoursValue(preset)}小时"
                                        }
                                        FilterChip(
                                            selected = abs(draftLead - preset) < 0.001f,
                                            onClick = { draftLead = preset },
                                            label = { Text(label, fontSize = 13.sp) },
                                            colors = chipColors,
                                            border = FilterChipDefaults.filterChipBorder(
                                                borderColor = dividerColor,
                                                selectedBorderColor = accentColor.copy(alpha = 0.55f),
                                                enabled = true,
                                                selected = abs(draftLead - preset) < 0.001f,
                                            ),
                                        )
                                    }
                                }
                            }

                            item {
                                HorizontalDivider(
                                    color = dividerColor,
                                    modifier = Modifier.padding(vertical = 16.dp),
                                )
                            }

                            item {
                                Text(
                                    text = "每日开放预定的时间段",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = onBackgroundColor,
                                )
                                Text(
                                    text = "默认全天24小时均可提供服务。添加时段后，仅在所选时间内接单。",
                                    fontSize = 14.sp,
                                    lineHeight = 22.sp,
                                    color = onSurfaceVariantColor,
                                    modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
                                )
                            }

                            items(
                                count = draft.size,
                                key = { index -> index },
                            ) { index ->
                                val slot = draft[index]
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    OutlinedButton(
                                        onClick = { timePick = index to true },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, dividerColor),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = onBackgroundColor,
                                        ),
                                    ) {
                                        Text(BookingTimeRangesCodec.formatClock(slot.start))
                                    }
                                    Text(
                                        text = "至",
                                        modifier = Modifier.padding(horizontal = 6.dp),
                                        fontSize = 14.sp,
                                        color = onSurfaceVariantColor,
                                    )
                                    OutlinedButton(
                                        onClick = { timePick = index to false },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, dividerColor),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = onBackgroundColor,
                                        ),
                                    ) {
                                        Text(BookingTimeRangesCodec.formatClock(slot.end))
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(onClick = {
                                        draft = draft.filterIndexed { i, _ -> i != index }
                                    }) {
                                        Icon(
                                            Icons.Outlined.DeleteOutline,
                                            contentDescription = "删除时段",
                                            tint = onSurfaceVariantColor,
                                        )
                                    }
                                }
                            }

                            item {
                                TextButton(
                                    onClick = {
                                        draft = draft + BookingTimeSlot("08:00", "22:00")
                                    },
                                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = accentColor)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("添加时段", color = accentColor, fontWeight = FontWeight.Medium)
                                }
                            }

                            item {
                                HorizontalDivider(
                                    color = dividerColor,
                                    modifier = Modifier.padding(vertical = 16.dp),
                                )
                            }

                            item {
                                Text(
                                    text = "未来开放预定的时间范围",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = onBackgroundColor,
                                )
                                Text(
                                    text = "用户仅可选择从今天起若干天内的服务开始日期（含今天）。可在 7～180 天之间调整，默认 30 天。",
                                    fontSize = 14.sp,
                                    lineHeight = 22.sp,
                                    color = onSurfaceVariantColor,
                                    modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    IconButton(
                                        onClick = {
                                            draftFutureDays =
                                                (draftFutureDays - 1).coerceAtLeast(
                                                    BookingTimeRangesCodec.MIN_BOOKING_FUTURE_OPEN_DAYS,
                                                )
                                        },
                                        enabled = draftFutureDays > BookingTimeRangesCodec.MIN_BOOKING_FUTURE_OPEN_DAYS,
                                    ) {
                                        Icon(
                                            Icons.Default.Remove,
                                            contentDescription = "减少天数",
                                            tint = if (draftFutureDays > BookingTimeRangesCodec.MIN_BOOKING_FUTURE_OPEN_DAYS) {
                                                accentColor
                                            } else {
                                                onSurfaceVariantColor.copy(alpha = 0.35f)
                                            },
                                        )
                                    }
                                    Text(
                                        text = "${draftFutureDays} 天",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = onBackgroundColor,
                                        modifier = Modifier.padding(horizontal = 20.dp),
                                    )
                                    IconButton(
                                        onClick = {
                                            draftFutureDays =
                                                (draftFutureDays + 1).coerceAtMost(
                                                    BookingTimeRangesCodec.MAX_BOOKING_FUTURE_OPEN_DAYS,
                                                )
                                        },
                                        enabled = draftFutureDays < BookingTimeRangesCodec.MAX_BOOKING_FUTURE_OPEN_DAYS,
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "增加天数",
                                            tint = if (draftFutureDays < BookingTimeRangesCodec.MAX_BOOKING_FUTURE_OPEN_DAYS) {
                                                accentColor
                                            } else {
                                                onSurfaceVariantColor.copy(alpha = 0.35f)
                                            },
                                        )
                                    }
                                }
                            }

                            item { Spacer(modifier = Modifier.height(4.dp)) }
                        }

                        HorizontalDivider(color = dividerColor)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("取消", color = onSurfaceVariantColor)
                            }
                            Button(
                                onClick = {
                                    val normalized = draft.map { s ->
                                        val start = s.start.ifBlank { "08:00" }
                                        val endRaw = s.end.ifBlank { "22:00" }
                                        val end = ensureValidEnd(start, endRaw)
                                        BookingTimeSlot(start = start, end = end)
                                    }
                                    val invalid = normalized.any { s ->
                                        val a = BookingTimeRangesCodec.parseMinutes(s.start)
                                        val b = BookingTimeRangesCodec.parseMinutes(s.end)
                                        a == null || b == null || b <= a
                                    }
                                    if (invalid) {
                                        Toast.makeText(
                                            context,
                                            "每个时段的结束时间须晚于开始时间",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    } else {
                                        onConfirm(
                                            normalized,
                                            BookingTimeRangesCodec.normalizeBookingLeadHours(draftLead),
                                            BookingTimeRangesCodec.normalizeBookingFutureOpenDays(draftFutureDays),
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = accentColor,
                                    contentColor = Color.White,
                                ),
                            ) {
                                Text("确定", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    val pick = timePick
                    if (pick != null) {
                        val (index, isStart) = pick
                        val slot = draft.getOrNull(index)
                        if (slot != null) {
                            val options = if (isStart) {
                                BookingTimeRangesCodec.halfHourStartChoices()
                            } else {
                                BookingTimeRangesCodec.endOptionsForStart(slot.start)
                            }
                            val current = if (isStart) slot.start else slot.end
                            Box(modifier = Modifier.matchParentSize()) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .background(Color.Black.copy(alpha = 0.45f))
                                        .clickable(
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() },
                                        ) { timePick = null },
                                )
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .fillMaxWidth(0.88f)
                                        .heightIn(max = 420.dp),
                                    shape = RoundedCornerShape(20.dp),
                                    color = surfaceColor,
                                    shadowElevation = 12.dp,
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 24.dp, end = 4.dp, top = DialogTitleTopPadding, bottom = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                text = if (isStart) "开始时间" else "结束时间",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = onBackgroundColor,
                                                modifier = Modifier.weight(1f),
                                            )
                                            IconButton(onClick = { timePick = null }) {
                                                Icon(Icons.Default.Close, contentDescription = "关闭", tint = onBackgroundColor)
                                            }
                                        }
                                        HorizontalDivider(color = dividerColor)
                                        LazyColumn(
                                            modifier = Modifier
                                                .heightIn(max = 320.dp)
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp),
                                        ) {
                                            items(options, key = { it }) { t ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            draft = draft.mapIndexed { i, s ->
                                                                if (i != index) s
                                                                else if (isStart) {
                                                                    val newStart = t
                                                                    val newEnd = ensureValidEnd(newStart, s.end)
                                                                    BookingTimeSlot(start = newStart, end = newEnd)
                                                                } else {
                                                                    BookingTimeSlot(start = s.start, end = t)
                                                                }
                                                            }
                                                            timePick = null
                                                        }
                                                        .padding(vertical = 6.dp, horizontal = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    RadioButton(
                                                        selected = t == current,
                                                        onClick = {
                                                            draft = draft.mapIndexed { i, s ->
                                                                if (i != index) s
                                                                else if (isStart) {
                                                                    val newStart = t
                                                                    val newEnd = ensureValidEnd(newStart, s.end)
                                                                    BookingTimeSlot(start = newStart, end = newEnd)
                                                                } else {
                                                                    BookingTimeSlot(start = s.start, end = t)
                                                                }
                                                            }
                                                            timePick = null
                                                        },
                                                        colors = RadioButtonDefaults.colors(
                                                            selectedColor = accentColor,
                                                            unselectedColor = onSurfaceVariantColor,
                                                        ),
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        BookingTimeRangesCodec.formatClock(t),
                                                        fontSize = 15.sp,
                                                        lineHeight = 22.sp,
                                                        color = onBackgroundColor,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
