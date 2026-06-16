// 文件说明：发布服务通用多选弹层，用于服务特点、额外费用等固定标签选择。

package com.example.Lulu.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ServiceTagMultiSelectBottomSheet(
    title: String,
    subtitle: String,
    options: List<String>,
    initialSelected: List<String>,
    surfaceColor: Color,
    onBackgroundColor: Color,
    onSurfaceVariantColor: Color,
    dividerColor: Color,
    accentColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (selected: List<String>) -> Unit,
) {
    var draft by remember { mutableStateOf(initialSelected.distinct()) }
    LaunchedEffect(initialSelected) {
        draft = initialSelected.distinct()
    }
    val selectedSet = draft.toSet()
    val chipColors = FilterChipDefaults.filterChipColors(
        containerColor = surfaceColor,
        selectedContainerColor = accentColor.copy(alpha = 0.12f),
        labelColor = onBackgroundColor,
        selectedLabelColor = accentColor,
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
                    .fillMaxHeight(0.72f),
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
                                .padding(start = 24.dp, end = 56.dp, top = DialogTitleTopPadding),
                        ) {
                            Text(
                                text = title,
                                color = onBackgroundColor,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = subtitle,
                                color = onSurfaceVariantColor,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = DialogTitleTopPadding, end = 8.dp),
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "关闭", tint = onSurfaceVariantColor)
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 24.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (options.isEmpty()) {
                            Text(
                                text = "当前类目暂未配置可选项",
                                color = onSurfaceVariantColor,
                                fontSize = 14.sp,
                            )
                        } else {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                options.forEach { option ->
                                    val selected = selectedSet.contains(option)
                                    FilterChip(
                                        selected = selected,
                                        onClick = {
                                            draft = if (selected) {
                                                draft.filterNot { it == option }
                                            } else {
                                                draft + option
                                            }
                                        },
                                        label = { Text(option) },
                                        colors = chipColors,
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = selected,
                                            borderColor = dividerColor,
                                            selectedBorderColor = accentColor.copy(alpha = 0.4f),
                                        ),
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = dividerColor)
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("取消", color = onSurfaceVariantColor)
                        }
                        Button(
                            onClick = { onConfirm(draft.distinct()) },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        ) {
                            Text("确定", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
