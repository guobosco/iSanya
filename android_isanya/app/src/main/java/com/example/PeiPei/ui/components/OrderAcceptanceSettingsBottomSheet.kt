// 文件说明：发布服务「接单设置」底部弹层：付款后自动接单 / 手动确认后接单。

package com.example.Lulu.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.Lulu.ui.theme.DialogTitleTopPadding

/** 列表行与弹窗共用的「自动接单」文案 */
const val ORDER_ACCEPTANCE_AUTO_LABEL: String = "用户付款后，自动接单"

/** 列表行与弹窗共用的「手动接单」文案 */
const val ORDER_ACCEPTANCE_MANUAL_LABEL: String = "用户付款后，手动确认订单信息接单"

fun orderAcceptanceSettingSummary(autoAcceptAfterPayment: Boolean): String =
    if (autoAcceptAfterPayment) ORDER_ACCEPTANCE_AUTO_LABEL else ORDER_ACCEPTANCE_MANUAL_LABEL

@Composable
fun OrderAcceptanceSettingsBottomSheet(
    initialAutoAcceptAfterPayment: Boolean,
    surfaceColor: Color,
    onBackgroundColor: Color,
    onSurfaceVariantColor: Color,
    dividerColor: Color,
    accentColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (autoAcceptAfterPayment: Boolean) -> Unit,
) {
    var draft by remember(initialAutoAcceptAfterPayment) {
        mutableStateOf(initialAutoAcceptAfterPayment)
    }

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
                    .fillMaxHeight(0.8f),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = surfaceColor,
                shadowElevation = 16.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding(),
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 24.dp, end = 24.dp, top = DialogTitleTopPadding),
                        ) {
                            Text(
                                text = "接单设置",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = onBackgroundColor,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "选择用户付款后你如何确认接单。",
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

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        OrderAcceptanceOptionRow(
                            selected = draft,
                            autoBranch = true,
                            label = ORDER_ACCEPTANCE_AUTO_LABEL,
                            onBackgroundColor = onBackgroundColor,
                            onPick = { draft = true },
                        )
                        OrderAcceptanceOptionRow(
                            selected = draft,
                            autoBranch = false,
                            label = ORDER_ACCEPTANCE_MANUAL_LABEL,
                            onBackgroundColor = onBackgroundColor,
                            onPick = { draft = false },
                        )
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
                            onClick = { onConfirm(draft) },
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
            }
        }
    }
}

@Composable
private fun OrderAcceptanceOptionRow(
    selected: Boolean,
    autoBranch: Boolean,
    label: String,
    onBackgroundColor: Color,
    onPick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected == autoBranch,
            onClick = onPick,
        )
        Text(
            text = label,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            color = onBackgroundColor,
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp),
        )
    }
}
