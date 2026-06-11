// 文件说明：发布服务前置的「选择服务类别」全屏弹窗。

package com.example.Lulu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.Lulu.data.model.ServiceCategories
import com.example.Lulu.data.model.experienceCategorySeeds
import com.example.Lulu.ui.theme.Black
import com.example.Lulu.ui.theme.DialogTitleTopPadding
import com.example.Lulu.ui.theme.ThemeButtonGradientEnd
import com.example.Lulu.ui.theme.ThemeButtonGradientStart
import com.example.Lulu.ui.theme.White

/**
 * 发布流程第一步：选择服务类别。关闭前需点选类目或「跳过」。
 * @param onPickCategory 用户点卡片上的「去填写」
 * @param onSkipGeneric 跳过则按「其他服务」进入通用表单
 */
@Composable
fun ServiceCategoryPickerGateDialog(
    onDismissRequest: () -> Unit,
    onPickCategory: (category: String) -> Unit,
    onSkipGeneric: () -> Unit
) {
    var selectedPublishTab by remember { mutableIntStateOf(0) }
    val categoryList = ServiceCategories.HOME_SERVICE_CATEGORIES_EXCLUDING_ALL
    val scheme = MaterialTheme.colorScheme
    val headerGradient = Brush.linearGradient(
        listOf(ThemeButtonGradientStart, ThemeButtonGradientEnd)
    )
    val onHeader = scheme.onPrimary

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(headerGradient)
                        .padding(
                            PaddingValues(
                                start = 4.dp,
                                end = 8.dp,
                                top = DialogTitleTopPadding,
                                bottom = 20.dp
                            )
                        )
                ) {
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "关闭", tint = onHeader)
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(PaddingValues(start = 12.dp, end = 12.dp, bottom = 20.dp))
                    ) {
                        TabRow(
                            selectedTabIndex = selectedPublishTab,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 40.dp),
                            containerColor = Color.Transparent,
                            contentColor = onHeader,
                            divider = {},
                            indicator = @Composable { tabPositions ->
                                if (selectedPublishTab < tabPositions.size) {
                                    TabRowDefaults.SecondaryIndicator(
                                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedPublishTab]),
                                        color = onHeader
                                    )
                                }
                            },
                        ) {
                            Tab(
                                selected = selectedPublishTab == 0,
                                onClick = { selectedPublishTab = 0 },
                                text = {
                                    Text(
                                        "服务",
                                        fontWeight = if (selectedPublishTab == 0) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                },
                            )
                            Tab(
                                selected = selectedPublishTab == 1,
                                onClick = { selectedPublishTab = 1 },
                                text = {
                                    Text(
                                        "体验",
                                        fontWeight = if (selectedPublishTab == 1) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                },
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        if (selectedPublishTab == 0) {
                            Text(
                                text = "发布服务赚钱",
                                color = onHeader,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "上传照片，填写服务内容和计费信息，为客户提供专业的旅游服务。",
                                color = onHeader.copy(alpha = 0.95f),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 22.sp,
                            )
                        } else {
                            Text(
                                text = "发布体验赚钱",
                                color = onHeader,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "精心设计一段旅程，上传照片，填写体验内容和计费信息，为客户提供专业的旅游体验。",
                                color = onHeader.copy(alpha = 0.92f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                lineHeight = 20.sp,
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    key(selectedPublishTab) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 12.dp)
                        ) {
                            if (selectedPublishTab == 0) {
                                items(categoryList, key = { it }) { cat ->
                                    CategoryPickerCard(
                                        title = cat,
                                        onPublish = { onPickCategory(ServiceCategories.normalize(cat)) }
                                    )
                                }
                            } else {
                                items(
                                    experienceCategorySeeds,
                                    key = { it.title },
                                ) { seed ->
                                    CategoryPickerCard(
                                        title = seed.title,
                                        onPublish = { onPickCategory(ServiceCategories.normalize(seed.title)) }
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = onSkipGeneric,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Black,
                            contentColor = White
                        )
                    ) {
                        Text("跳过直接发布", fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryPickerCard(
    title: String,
    onPublish: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(Black, RoundedCornerShape(20.dp))
                        .clickable(onClick = onPublish)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "去填写",
                        color = White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
