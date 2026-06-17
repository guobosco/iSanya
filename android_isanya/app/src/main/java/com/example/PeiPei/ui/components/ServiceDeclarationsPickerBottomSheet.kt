// 文件说明：发布/编辑服务「服务声明」弹层：平台默认四条不可删，用户可增删自定义条目（编号自 5 起）。

package com.example.Lulu.ui.components

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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.Lulu.data.model.ServiceDeclarations
import com.example.Lulu.ui.theme.DialogTitleTopPadding

private const val MAX_EXTRA_DECLARATIONS = 30

@Composable
fun ServiceDeclarationsPickerBottomSheet(
    initialExtra: List<String>,
    surfaceColor: Color,
    onBackgroundColor: Color,
    onSurfaceVariantColor: Color,
    dividerColor: Color,
    accentColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (extra: List<String>) -> Unit,
) {
    var draft by remember { mutableStateOf<List<String>>(initialExtra) }
    LaunchedEffect(initialExtra) {
        draft = initialExtra
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
                                .padding(start = 24.dp, end = 56.dp, top = DialogTitleTopPadding),
                        ) {
                            Text(
                                text = "服务声明",
                                color = onBackgroundColor,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "平台默认声明会自动展示，你也可以补充自己的接待约定和说明。",
                                color = onSurfaceVariantColor,
                                fontSize = 14.sp,
                                lineHeight = 22.sp,
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
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp),
                    ) {
                        ServiceDeclarations.BUILTIN.forEachIndexed { index, line ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text(
                                    text = "${index + 1}.",
                                    color = onSurfaceVariantColor,
                                    fontSize = 14.sp,
                                )
                                Text(
                                    text = line,
                                    color = onBackgroundColor,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }

                        val extraStartOrdinal = ServiceDeclarations.BUILTIN.size
                        val declarationFieldTextStyle = TextStyle(
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                        )
                        draft.forEachIndexed { rowIndex, declLine ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text(
                                    text = "${extraStartOrdinal + rowIndex + 1}.",
                                    color = onSurfaceVariantColor,
                                    fontSize = 14.sp,
                                )
                                OutlinedTextField(
                                    value = declLine,
                                    onValueChange = { v: String ->
                                        draft = draft.toMutableList().apply { this[rowIndex] = v }
                                    },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    textStyle = declarationFieldTextStyle,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = surfaceColor,
                                        unfocusedContainerColor = surfaceColor,
                                        focusedTextColor = onBackgroundColor,
                                        unfocusedTextColor = onBackgroundColor,
                                        focusedBorderColor = accentColor.copy(alpha = 0.65f),
                                        unfocusedBorderColor = dividerColor,
                                        cursorColor = accentColor,
                                    ),
                                )
                                IconButton(
                                    onClick = {
                                        draft = draft.filterIndexed { idx, _ -> idx != rowIndex }
                                    },
                                ) {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        contentDescription = "删除",
                                        tint = accentColor,
                                    )
                                }
                            }
                        }

                        TextButton(
                            onClick = {
                                if (draft.size < MAX_EXTRA_DECLARATIONS) {
                                    draft = draft + ""
                                }
                            },
                            enabled = draft.size < MAX_EXTRA_DECLARATIONS,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentPadding = PaddingValues(0.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Add, contentDescription = null, tint = accentColor)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "添加声明",
                                    color = accentColor,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    HorizontalDivider(color = dividerColor)
                    Row(
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
                            onClick = {
                                onConfirm(ServiceDeclarations.normalizeExtra(draft))
                            },
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
