// 文件说明：编辑资料时选择兴趣爱好的底部面板，样式对齐设计稿。

package com.example.Lulu.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Museum
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SetMeal
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.Lulu.ui.theme.DialogTitleTopPadding
import androidx.compose.ui.window.DialogProperties

private val InkBlack = Color(0xFF000000)
private val SubtitleGrey = Color(0xFF8E8E8E)
private val BorderLight = Color(0xFFE5E5E5)
private val FooterDivider = Color(0xFFDCDCDC)

private const val MAX_INTERESTS = 20

/** 预设兴趣：面向中文用户与国内常见生活方式。 */
data class ProfileInterestPreset(
    val label: String,
    val icon: ImageVector
)

fun defaultProfileInterestPresets(): List<ProfileInterestPreset> = listOf(
    ProfileInterestPreset("电影追剧", Icons.Filled.Movie),
    ProfileInterestPreset("探店美食", Icons.Filled.Restaurant),
    ProfileInterestPreset("阅读听书", Icons.Filled.MenuBook),
    ProfileInterestPreset("咖啡奶茶", Icons.Filled.LocalCafe),
    ProfileInterestPreset("徒步登山", Icons.Filled.Hiking),
    ProfileInterestPreset("摄影拍照", Icons.Filled.PhotoCamera),
    ProfileInterestPreset("萌宠动物", Icons.Filled.Pets),
    ProfileInterestPreset("K歌聚会", Icons.Filled.Mic),
    ProfileInterestPreset("现场音乐", Icons.Filled.MusicNote),
    ProfileInterestPreset("公园户外", Icons.Filled.Park),
    ProfileInterestPreset("烹饪烘焙", Icons.Filled.SetMeal),
    ProfileInterestPreset("手游电竞", Icons.Filled.SportsEsports),
    ProfileInterestPreset("动漫番剧", Icons.Filled.AutoAwesome),
    ProfileInterestPreset("骑行", Icons.Filled.DirectionsBike),
    ProfileInterestPreset("跑步健身", Icons.Filled.DirectionsRun),
    ProfileInterestPreset("游泳瑜伽", Icons.Filled.Pool),
    ProfileInterestPreset("旅行出游", Icons.Filled.FlightTakeoff),
    ProfileInterestPreset("逛博物馆", Icons.Filled.Museum),
    ProfileInterestPreset("露营自驾", Icons.Filled.DirectionsCar),
    ProfileInterestPreset("密室剧本杀", Icons.Filled.Groups),
    ProfileInterestPreset("书法绘画", Icons.Filled.Brush),
    ProfileInterestPreset("广场舞太极", Icons.Filled.Spa),
    ProfileInterestPreset("理财投资", Icons.Filled.Savings),
    ProfileInterestPreset("短视频追剧", Icons.Filled.VideoLibrary),
)

private const val COLLAPSED_VISIBLE = 12

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileInterestsPickerDialog(
    initialSelected: Set<String>,
    allPresets: List<ProfileInterestPreset> = defaultProfileInterestPresets(),
    onDismissRequest: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf(initialSelected) }
    var showAll by remember { mutableStateOf(false) }

    LaunchedEffect(initialSelected) {
        selected = initialSelected
        showAll = false
    }

    val visiblePresets = if (showAll) allPresets else allPresets.take(COLLAPSED_VISIBLE)
    val canExpand = allPresets.size > COLLAPSED_VISIBLE

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val bottomInsetPx = rememberProfileEditDialogBottomInsetPx()
        val bottomInsetDp = with(LocalDensity.current) { bottomInsetPx.toDp() }
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = Color.White,
                shadowElevation = 16.dp
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(start = 24.dp, end = 52.dp, top = DialogTitleTopPadding)
                        ) {
                        Text(
                            text = "你有哪些兴趣爱好？",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = InkBlack,
                            lineHeight = 28.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "选择你想在个人资料中展示的兴趣爱好。",
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            color = SubtitleGrey
                        )
                        Spacer(modifier = Modifier.height(22.dp))
                        Text(
                            text = "兴趣",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = InkBlack
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            visiblePresets.forEach { preset ->
                                val isOn = selected.contains(preset.label)
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = Color.White,
                                    border = BorderStroke(
                                        width = if (isOn) 2.dp else 1.dp,
                                        color = if (isOn) InkBlack else BorderLight
                                    ),
                                    modifier = Modifier.clickable {
                                        selected = if (isOn) {
                                            selected - preset.label
                                        } else {
                                            if (selected.size >= MAX_INTERESTS) {
                                                Toast.makeText(
                                                    context,
                                                    "最多可选 $MAX_INTERESTS 项",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                selected
                                            } else {
                                                selected + preset.label
                                            }
                                        }
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            preset.icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = InkBlack
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = preset.label,
                                            fontSize = 14.sp,
                                            color = InkBlack
                                        )
                                    }
                                }
                            }
                        }

                            if (canExpand) {
                                TextButton(
                                    onClick = { showAll = !showAll },
                                    modifier = Modifier.padding(top = 6.dp, start = 0.dp)
                                ) {
                                    Text(
                                        text = if (showAll) "收起" else "显示全部",
                                        fontSize = 14.sp,
                                        color = SubtitleGrey,
                                        textDecoration = TextDecoration.Underline
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        IconButton(
                            onClick = onDismissRequest,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = DialogTitleTopPadding, end = 4.dp)
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "关闭",
                                tint = InkBlack
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = bottomInsetDp)
                    ) {
                        HorizontalDivider(thickness = 0.5.dp, color = FooterDivider)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "已选 ${selected.size} 项（最多可选 $MAX_INTERESTS 项）",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = InkBlack,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = {
                                    val ordered = allPresets.map { it.label }.filter { selected.contains(it) }
                                    onSave(ordered)
                                },
                                enabled = selected.isNotEmpty(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF222222),
                                    contentColor = Color.White,
                                    disabledContainerColor = Color(0xFFCCCCCC),
                                    disabledContentColor = Color.White
                                ),
                                modifier = Modifier.height(44.dp)
                            ) {
                                Text("保存", fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
