// 文件说明：发布服务「价格设置」底部弹层：多档位（价格 + 时长数字与小时/分钟）+ 预付款与退订规则。

package com.example.Lulu.ui.components

import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.Lulu.ui.theme.DialogTitleTopPadding
import com.example.Lulu.ui.util.findComposeDialogWindow
import com.example.Lulu.util.DURATION_UNIT_HOURS
import com.example.Lulu.util.DURATION_UNIT_MINUTES
import com.example.Lulu.util.PublishPriceTierRow
import com.example.Lulu.util.decodePublishPriceTiers
import com.example.Lulu.util.encodePublishPriceTiers
import com.example.Lulu.util.normalizeDurationUnit
import com.example.Lulu.util.publishPriceTiersValidForSubmit
import kotlin.math.roundToInt

private const val MAX_TIERS = 8
private const val MAX_TIER_NAME_LENGTH = 20

@Composable
fun PublishPriceSettingsBottomSheet(
    initialPriceText: String,
    initialPriceBasisText: String,
    initialPrepaymentPercent: Int = 30,
    initialFullRefundCancelLeadDays: Int = 1,
    surfaceColor: Color,
    onBackgroundColor: Color,
    onSurfaceVariantColor: Color,
    dividerColor: Color,
    accentColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (priceText: String, priceBasisText: String, prepaymentPercent: Int, fullRefundCancelLeadDays: Int, durationUnitForServiceMode: String) -> Unit,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val dismissKeyboard: () -> Unit = {
        focusManager.clearFocus(force = true)
        keyboard?.hide()
    }

    var tiers by remember(initialPriceText, initialPriceBasisText) {
        mutableStateOf(decodePublishPriceTiers(initialPriceText, initialPriceBasisText).toMutableList())
    }
    var draftPrepaymentPercent by remember(initialPrepaymentPercent) {
        mutableStateOf(initialPrepaymentPercent.coerceIn(0, 100))
    }
    var draftRefundLeadDays by remember(initialFullRefundCancelLeadDays) {
        mutableStateOf(initialFullRefundCancelLeadDays.coerceIn(0, 10))
    }
    var renamingTierIndex by remember { mutableStateOf<Int?>(null) }
    var renameDraft by remember { mutableStateOf("") }

    fun commitTierRename(index: Int) {
        val row = tiers.getOrNull(index) ?: return
        tiers = tiers.toMutableList().also {
            it[index] = row.copy(name = renameDraft.trim().take(MAX_TIER_NAME_LENGTH))
        }
        renamingTierIndex = null
        dismissKeyboard()
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = surfaceColor,
        unfocusedContainerColor = surfaceColor,
        focusedTextColor = onBackgroundColor,
        unfocusedTextColor = onBackgroundColor,
        focusedBorderColor = accentColor.copy(alpha = 0.65f),
        unfocusedBorderColor = dividerColor,
        cursorColor = accentColor,
        focusedLabelColor = onSurfaceVariantColor,
        unfocusedLabelColor = onSurfaceVariantColor,
    )

    Dialog(
        onDismissRequest = {
            dismissKeyboard()
            onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        val hostView = LocalView.current
        DisposableEffect(hostView) {
            val window = hostView.findComposeDialogWindow()
            if (window != null) {
                val previousMode = window.attributes.softInputMode
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                onDispose { window.setSoftInputMode(previousMode) }
            } else {
                onDispose { }
            }
        }
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .imePadding(),
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .padding(top = DialogTitleTopPadding),
                        ) {
                            Text(
                                text = "价格设置",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = onBackgroundColor,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "可添加多档位套餐。",
                                fontSize = 14.sp,
                                lineHeight = 22.sp,
                                color = onSurfaceVariantColor,
                            )
                            Text(
                                text = "请填写各档价格与时长；时长默认为 4，单位可选「小时」或「分钟」。",
                                fontSize = 14.sp,
                                lineHeight = 22.sp,
                                color = onSurfaceVariantColor,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        IconButton(
                            onClick = {
                                dismissKeyboard()
                                onDismiss()
                            },
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
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(
                            count = tiers.size,
                            key = { index -> index },
                        ) { index ->
                            val tier = tiers[index]
                            Card(
                                colors = CardDefaults.cardColors(containerColor = surfaceColor),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                border = BorderStroke(1.dp, dividerColor),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        val titleText = tier.name.trim().ifBlank { "档位 ${index + 1}" }
                                        if (renamingTierIndex == index) {
                                            OutlinedTextField(
                                                value = renameDraft,
                                                onValueChange = { renameDraft = it.take(MAX_TIER_NAME_LENGTH) },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f),
                                                colors = fieldColors,
                                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                                keyboardActions = KeyboardActions(
                                                    onDone = { commitTierRename(index) },
                                                ),
                                                placeholder = { Text("档位名称", color = onSurfaceVariantColor) },
                                            )
                                        } else {
                                            Text(
                                                text = titleText,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = onBackgroundColor,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .pointerInput(index, tier.name) {
                                                        detectTapGestures(
                                                            onDoubleTap = {
                                                                renamingTierIndex = index
                                                                renameDraft = tier.name.trim()
                                                            },
                                                        )
                                                    },
                                            )
                                        }
                                        if (renamingTierIndex == index) {
                                            TextButton(onClick = { commitTierRename(index) }) {
                                                Text("完成", color = accentColor)
                                            }
                                        }
                                        if (tiers.size > 1) {
                                            IconButton(
                                                onClick = {
                                                    if (renamingTierIndex == index) {
                                                        renamingTierIndex = null
                                                    } else if (renamingTierIndex != null && renamingTierIndex!! > index) {
                                                        renamingTierIndex = renamingTierIndex!! - 1
                                                    }
                                                    tiers = tiers.toMutableList().also { it.removeAt(index) }
                                                },
                                                modifier = Modifier.size(40.dp),
                                            ) {
                                                Icon(
                                                    Icons.Outlined.DeleteOutline,
                                                    contentDescription = "删除此档",
                                                    tint = onSurfaceVariantColor,
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = tier.price,
                                        onValueChange = { v ->
                                            tiers = tiers.toMutableList().also { it[index] = tier.copy(price = v) }
                                        },
                                        label = { Text("价格") },
                                        placeholder = { Text("如：99", color = onSurfaceVariantColor) },
                                        suffix = { Text("￥", color = onSurfaceVariantColor) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = fieldColors,
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "时长",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = onSurfaceVariantColor,
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        OutlinedTextField(
                                            value = tier.durationAmount.toString(),
                                            onValueChange = { raw ->
                                                val digits = raw.filter { it.isDigit() }.take(5)
                                                val n = if (digits.isEmpty()) {
                                                    4
                                                } else {
                                                    digits.toInt().coerceIn(1, 99_999)
                                                }
                                                tiers = tiers.toMutableList().also {
                                                    it[index] = tier.copy(durationAmount = n)
                                                }
                                            },
                                            label = null,
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            colors = fieldColors,
                                        )
                                        Spacer(modifier = Modifier.size(10.dp))
                                        Column {
                                            FilterChip(
                                                selected = tier.durationUnit == DURATION_UNIT_HOURS,
                                                onClick = {
                                                    tiers = tiers.toMutableList().also {
                                                        it[index] = tier.copy(durationUnit = DURATION_UNIT_HOURS)
                                                    }
                                                },
                                                label = { Text(DURATION_UNIT_HOURS) },
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            FilterChip(
                                                selected = tier.durationUnit == DURATION_UNIT_MINUTES,
                                                onClick = {
                                                    tiers = tiers.toMutableList().also {
                                                        it[index] = tier.copy(durationUnit = DURATION_UNIT_MINUTES)
                                                    }
                                                },
                                                label = { Text(DURATION_UNIT_MINUTES) },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            OutlinedButton(
                                onClick = {
                                    if (tiers.size >= MAX_TIERS) {
                                        android.widget.Toast.makeText(
                                            context,
                                            "最多添加 $MAX_TIERS 个档位",
                                            android.widget.Toast.LENGTH_SHORT,
                                        ).show()
                                    } else {
                                        tiers = tiers.toMutableList().also { it.add(PublishPriceTierRow()) }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = accentColor)
                                Spacer(modifier = Modifier.size(8.dp))
                                Text("添加档位", color = accentColor)
                            }
                        }
                        item {
                            HorizontalDivider(color = dividerColor, modifier = Modifier.padding(vertical = 12.dp))
                            Text(
                                text = "预付款比例",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = onBackgroundColor,
                            )
                            Text(
                                text = "用户预定需先支付的定金占订单金额的比例。默认 30%，可拖动调整为 0%–100%。",
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                color = onSurfaceVariantColor,
                                modifier = Modifier.padding(top = 6.dp, bottom = 8.dp),
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("当前比例", fontSize = 14.sp, color = onSurfaceVariantColor)
                                Text(
                                    text = "${draftPrepaymentPercent}%",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = accentColor,
                                )
                            }
                            Slider(
                                value = draftPrepaymentPercent.toFloat(),
                                onValueChange = { v ->
                                    draftPrepaymentPercent = v.roundToInt().coerceIn(0, 100)
                                },
                                valueRange = 0f..100f,
                                steps = 99,
                                modifier = Modifier.fillMaxWidth(),
                                colors = SliderDefaults.colors(
                                    thumbColor = accentColor,
                                    activeTrackColor = accentColor.copy(alpha = 0.55f),
                                    inactiveTrackColor = dividerColor,
                                ),
                            )
                        }
                        item {
                            Text(
                                text = "取消预定与预付款退还",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = onBackgroundColor,
                            )
                            Text(
                                text = "用户需早于计划开始时间至少提前下方天数取消预定，预付款可全额退还。默认 1 天，可调整为 0–10 天。",
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                color = onSurfaceVariantColor,
                                modifier = Modifier.padding(top = 6.dp, bottom = 8.dp),
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("提前天数", fontSize = 14.sp, color = onSurfaceVariantColor)
                                Text(
                                    text = "${draftRefundLeadDays} 天",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = accentColor,
                                )
                            }
                            Slider(
                                value = draftRefundLeadDays.toFloat(),
                                onValueChange = { v ->
                                    draftRefundLeadDays = v.roundToInt().coerceIn(0, 10)
                                },
                                valueRange = 0f..10f,
                                steps = 9,
                                modifier = Modifier.fillMaxWidth(),
                                colors = SliderDefaults.colors(
                                    thumbColor = accentColor,
                                    activeTrackColor = accentColor.copy(alpha = 0.55f),
                                    inactiveTrackColor = dividerColor,
                                ),
                            )
                            Text(
                                text = "为 0 时表示不限制提前天数（开始服务前均可申请全额退还预付款）。",
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                color = onSurfaceVariantColor,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
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
                            onClick = {
                                dismissKeyboard()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("取消", color = onSurfaceVariantColor)
                        }
                        Button(
                            onClick = {
                                if (!publishPriceTiersValidForSubmit(tiers)) {
                                    android.widget.Toast.makeText(
                                        context,
                                        "请至少填写一档价格",
                                        android.widget.Toast.LENGTH_SHORT,
                                    ).show()
                                    return@Button
                                }
                                dismissKeyboard()
                                val (headline, basis) = encodePublishPriceTiers(tiers)
                                val durationMode = normalizeDurationUnit(
                                    tiers.firstOrNull()?.durationUnit ?: DURATION_UNIT_HOURS,
                                )
                                onConfirm(
                                    headline,
                                    basis,
                                    draftPrepaymentPercent.coerceIn(0, 100),
                                    draftRefundLeadDays.coerceIn(0, 10),
                                    durationMode,
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White),
                        ) {
                            Text("确定", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
