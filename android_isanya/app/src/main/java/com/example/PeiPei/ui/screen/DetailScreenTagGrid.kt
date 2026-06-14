// 文件说明：服务/体验详情页标题下方的两列「图标 + 文案」标签网格。

package com.example.Lulu.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FaceRetouchingNatural
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.Luggage
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Pool
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Train
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.Lulu.data.model.ServiceCategories
import java.util.Locale

private val DetailTagIconTint = Color(0xFF222222)

/** 服务/体验详情页标签区上下边线颜色，与其它详情区块分隔线共用。 */
val DetailScreenTagDividerColor = Color(0xFFE8E8E8)

/** 根据标签文案选择最贴切的图标（关键词 + 类目精确匹配）。 */
fun iconVectorForDetailTagLabel(text: String): ImageVector {
    val c = text.trim()
    if (c.isEmpty()) return Icons.Outlined.Label
    val lower = c.lowercase(Locale.ROOT)

    when (c) {
        ServiceCategories.LOCAL_GUIDE -> return Icons.Outlined.Map
        ServiceCategories.PHOTO -> return Icons.Outlined.PhotoCamera
        ServiceCategories.DJ_ATMOSPHERE -> return Icons.Outlined.GraphicEq
        ServiceCategories.FITNESS_COACH -> return Icons.Outlined.FitnessCenter
        ServiceCategories.PRIVATE_CHEF -> return Icons.Outlined.Restaurant
        ServiceCategories.MAKEUP -> return Icons.Outlined.FaceRetouchingNatural
        ServiceCategories.OTHER -> return Icons.Outlined.Apps
    }

    return when {
        c.contains("认证") || c.contains("实名") || c.contains("已验") || c.contains("验真") ->
            Icons.Outlined.VerifiedUser
        c.contains("排名") || c.contains("前") && (c.contains("%") || c.contains("百分")) ||
            c.contains("精选") || c.contains("口碑") || c.contains("金牌") ->
            Icons.Outlined.EmojiEvents
        c.contains("热门") || c.contains("爆款") || c.contains("人气") -> Icons.Outlined.Whatshot
        c.contains("星级") || c.contains("五星") || c.contains("好评") -> Icons.Outlined.Star
        c.contains("中文") || c.contains("英文") || c.contains("日文") || c.contains("韩文") ||
            c.contains("语言") || c.contains("翻译") || c.contains("沟通") ->
            Icons.Outlined.Translate
        c.contains("站") || c.contains("地铁") || c.contains("高铁") || c.contains("火车") ||
            c.contains("轨交") || c.contains("枢纽") || c.contains("交通") ->
            Icons.Outlined.Train
        c.contains("空调") || c.contains("暖气") || c.contains("新风") || c.contains("地暖") ->
            Icons.Outlined.AcUnit
        c.contains("行李") || c.contains("寄存") || c.contains("托运") || c.contains("存放") ->
            Icons.Outlined.Luggage
        c.contains("网络") || c.contains("Wi") || c.contains("wifi") || lower.contains("wifi") ->
            Icons.Outlined.Wifi
        c.contains("上门") || c.contains("到府") || lower.contains("door") ->
            Icons.Outlined.Home
        c.contains("到店") || c.contains("门店") || c.contains("店面") || c.contains("线下店") ->
            Icons.Outlined.Storefront
        c.contains("小班") || c.contains("人数") || c.contains("成团") || c.contains("人团") ||
            c.contains("名额") || c.contains("席位") ->
            Icons.Outlined.People
        c.contains("私教") || c.contains("团课") || c.contains("教练") ->
            Icons.Outlined.FitnessCenter
        c.contains("小时") || c.contains("分钟") || c.contains("时长") || c.contains("半日") ||
            c.contains("全日") || c == "天" || c == "次" || c == "小时" || c.contains("课时") ->
            Icons.Outlined.Schedule
        c.contains("¥") || c.contains("元") || c.contains("价") || c.contains("费用") ->
            Icons.Outlined.AttachMoney
        c.contains("订金") || c.contains("预付") || c.contains("支付") -> Icons.Outlined.Payments
        c.contains("区") || c.contains("路") || c.contains("街") || c.contains("巷") ||
            c.contains("公里") || lower.contains("km") || c.contains("坐标") || c.contains("集合") ||
            c.contains("地点") || c.contains("位置") || c.contains("区域") || c.contains("范围") ||
            c.contains("地图") || c.contains("商圈") ->
            Icons.Outlined.Place
        c.contains("摄影") || c.contains("跟拍") || c.contains("拍照") -> Icons.Outlined.PhotoCamera
        c.contains("美食") || c.contains("餐饮") || c.contains("私厨") || c.contains("饭局") ->
            Icons.Outlined.Restaurant
        c.contains("潜水") || c.contains("冲浪") || c.contains("泳池") || c.contains("游泳") ->
            Icons.Outlined.Pool
        c.contains("租车") || c.contains("游艇") || c.contains("包车") || c.contains("司机") ->
            Icons.Outlined.DirectionsCar
        c.contains("按摩") || c.contains("推拿") || c.contains("SPA") || lower.contains("spa") ->
            Icons.Outlined.Spa
        c.contains("化妆") || c.contains("妆造") -> Icons.Outlined.FaceRetouchingNatural
        c.contains("教学") || c.contains("课程") || c.contains("陪练") || c.contains("技能") ->
            Icons.Outlined.School
        c.contains("DJ") || c.contains("打碟") -> Icons.Outlined.GraphicEq
        c.contains("气氛") || c.contains("暖场") || c.contains("派对") -> Icons.Outlined.Celebration
        c.contains("陪游") || c.contains("向导") || c.contains("地陪") || c.contains("导览") ->
            Icons.Outlined.Map
        c.contains("装备") || c.contains("物料") || c.contains("道具") || c.contains("器材") ->
            Icons.Outlined.Inventory2
        c.contains("剧本") || c.contains("桌游") || c.contains("聚会") -> Icons.Outlined.Groups
        else -> Icons.Outlined.Label
    }
}

@Composable
fun DetailTagTwoColumnGrid(
    tags: List<String>,
    modifier: Modifier = Modifier,
) {
    val rows = tags.map { it.trim() }.filter { it.isNotEmpty() }
    if (rows.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 1.dp,
            color = DetailScreenTagDividerColor
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            rows.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    pair.forEach { label ->
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = iconVectorForDetailTagLabel(label),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = DetailTagIconTint
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = label,
                                modifier = Modifier.weight(1f),
                                color = Color.Black,
                                fontSize = 13.sp,
                                lineHeight = 17.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 1.dp,
            color = DetailScreenTagDividerColor
        )
    }
}
