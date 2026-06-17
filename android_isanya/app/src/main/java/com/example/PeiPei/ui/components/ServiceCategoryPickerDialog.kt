// 文件说明：发布服务前置的「选择服务类别」全屏弹窗。

package com.example.Lulu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.DirectionsBoat
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.ForkRight
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.LocalActivity
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.ModeNight
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.Lulu.data.model.ServiceCategories
import com.example.Lulu.data.model.experienceCategorySeeds
import com.example.Lulu.ui.theme.DialogTitleTopPadding

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
    val pageBackground = Color(0xFFF7F4F5)
    val heroSurface = scheme.surface
    val chipBackground = Color(0xFFF4EEF1)
    val borderColor = scheme.outlineVariant.copy(alpha = 0.6f)

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = pageBackground) {
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = DialogTitleTopPadding,
                                bottom = 16.dp
                            )
                        )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "选择发布类别",
                            color = scheme.onSurface,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = onDismissRequest,
                            modifier = Modifier
                                .background(Color.White, RoundedCornerShape(16.dp))
                                .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "关闭",
                                tint = scheme.onSurface
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .background(heroSurface, RoundedCornerShape(28.dp))
                            .border(1.dp, borderColor, RoundedCornerShape(28.dp))
                            .padding(horizontal = 20.dp, vertical = 20.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(chipBackground, RoundedCornerShape(999.dp))
                                .padding(6.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                PublishKindSegment(
                                    selected = selectedPublishTab == 0,
                                    label = "服务",
                                    onClick = { selectedPublishTab = 0 }
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                PublishKindSegment(
                                    selected = selectedPublishTab == 1,
                                    label = "体验",
                                    onClick = { selectedPublishTab = 1 }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(18.dp))
                        if (selectedPublishTab == 0) {
                            Text(
                                text = "发布服务赚钱",
                                color = scheme.onSurface,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "先选你的服务方向，再完善内容、价格和接单方式。",
                                color = scheme.onSurfaceVariant,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 22.sp
                            )
                        } else {
                            Text(
                                text = "发布体验赚钱",
                                color = scheme.onSurface,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "先选体验主题，再补充行程亮点、集合信息和价格设置。",
                                color = scheme.onSurfaceVariant,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    key(selectedPublishTab) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            if (selectedPublishTab == 0) {
                                items(categoryList, key = { it }) { cat ->
                                    CategoryPickerCard(
                                        title = cat,
                                        subtitle = serviceCategorySubtitle(cat),
                                        icon = serviceCategoryIcon(cat),
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
                                        subtitle = experienceCategorySubtitle(seed.title),
                                        icon = experienceCategoryIcon(seed.title),
                                        onPublish = { onPickCategory(ServiceCategories.normalize(seed.title)) }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onSkipGeneric,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, borderColor),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = scheme.onSurface
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
    subtitle: String,
    icon: ImageVector,
    onPublish: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPublish),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(Color(0xFFFFF2F6), RoundedCornerShape(14.dp))
                    .size(44.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "开始填写",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun RowScope.PublishKindSegment(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .weight(1f)
            .background(
                color = if (selected) scheme.primary else Color.White,
                shape = RoundedCornerShape(999.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else scheme.onSurface,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun serviceCategorySubtitle(category: String): String = when (category) {
    ServiceCategories.LOCAL_GUIDE -> "路线规划与陪同讲解"
    ServiceCategories.PHOTO -> "旅拍约拍与内容出片"
    ServiceCategories.DJ_ATMOSPHERE -> "派对暖场与活动氛围"
    ServiceCategories.FITNESS_COACH -> "健身训练与体能指导"
    ServiceCategories.PRIVATE_CHEF -> "上门做饭与聚会定制"
    ServiceCategories.MAKEUP -> "妆造跟妆与形象设计"
    ServiceCategories.SKILL_TEACHING -> "课程陪练与兴趣提升"
    else -> "暂未归类也能直接发布"
}

private fun experienceCategorySubtitle(category: String): String = when (category) {
    "必游经典景区" -> "热门景点导览与精华路线"
    "潜水冲浪跳伞" -> "海陆空运动体验"
    "海面运动" -> "出海巡航与轻户外玩法"
    "文化探索" -> "人文走读与非遗体验"
    "夜生活" -> "夜游路线与本地氛围推荐"
    else -> "个性化主题体验"
}

private fun serviceCategoryIcon(category: String): ImageVector = when (category) {
    ServiceCategories.LOCAL_GUIDE -> Icons.Outlined.Map
    ServiceCategories.PHOTO -> Icons.Outlined.CameraAlt
    ServiceCategories.DJ_ATMOSPHERE -> Icons.Outlined.MusicNote
    ServiceCategories.FITNESS_COACH -> Icons.Outlined.FitnessCenter
    ServiceCategories.PRIVATE_CHEF -> Icons.Outlined.ForkRight
    ServiceCategories.MAKEUP -> Icons.Outlined.Palette
    ServiceCategories.SKILL_TEACHING -> Icons.Outlined.School
    else -> Icons.Outlined.GridView
}

private fun experienceCategoryIcon(category: String): ImageVector = when (category) {
    "必游经典景区" -> Icons.Outlined.Explore
    "潜水冲浪跳伞" -> Icons.Outlined.LocalActivity
    "海面运动" -> Icons.Outlined.DirectionsBoat
    "文化探索" -> Icons.Outlined.AutoStories
    "夜生活" -> Icons.Outlined.ModeNight
    else -> Icons.Outlined.Groups
}
