// 文件说明：他人主页与详情页共用的评价轮播、列表底栏样式（与 ServiceHostProfile 一致）。

package com.example.Lulu.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.Lulu.data.remote.RetrofitClient
import com.example.Lulu.ui.theme.DialogTitleTopPadding

data class HostProfileReviewUi(
    val reviewerName: String,
    val reviewerPhotoUrl: String,
    val reviewText: String,
    val timeAgoText: String,
    val locationLine: String?,
)

private val reviewSampleNames = listOf("鑫媛", "婷婷", "子涵", "思琪", "浩然", "雨桐")
private val reviewSampleTimeLabels = listOf("1周前", "2周前", "3天前", "5天前", "1个月前", "2个月前")
private val reviewSampleLocations = listOf("北京, 中国", "上海, 中国", "首尔, 韩国", "广州, 中国", null, "深圳, 中国")

fun hostProfileReviewsFromSummaries(summaries: List<String>): List<HostProfileReviewUi> {
    if (summaries.isEmpty()) return emptyList()
    return summaries.mapIndexed { index, summary ->
        HostProfileReviewUi(
            reviewerName = reviewSampleNames[index % reviewSampleNames.size],
            reviewerPhotoUrl = "",
            reviewText = summary,
            timeAgoText = reviewSampleTimeLabels[index % reviewSampleTimeLabels.size],
            locationLine = reviewSampleLocations[index % reviewSampleLocations.size],
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProfileReviewsSection(
    sectionTitle: String,
    reviews: List<HostProfileReviewUi>,
    isDarkTheme: Boolean,
    onShowMore: () -> Unit,
) {
    val titleColor = if (isDarkTheme) Color.White else Color(0xFF222222)
    val mutedColor = if (isDarkTheme) Color(0xFFA8A8A8) else Color(0xFF777777)
    val buttonBg = if (isDarkTheme) Color(0xFF333333) else Color(0xFFE8E8E8)
    val buttonText = if (isDarkTheme) Color.White else Color(0xFF222222)
    val pagerState = rememberPagerState(pageCount = { maxOf(1, reviews.size) })

    Column {
        HorizontalDivider(
            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
            thickness = 1.dp,
            color = if (isDarkTheme) Color(0xFF2D2D2D) else Color(0xFFE8E8E8),
        )
        Text(
            text = sectionTitle,
            color = titleColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(12.dp))
        if (reviews.isEmpty()) {
            Text(
                text = "暂无评价",
                color = mutedColor,
                fontSize = 14.sp,
            )
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(end = 40.dp),
                pageSpacing = 12.dp,
            ) { page ->
                ReviewCarouselCard(review = reviews[page], isDarkTheme = isDarkTheme)
            }
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onShowMore),
                shape = RoundedCornerShape(10.dp),
                color = buttonBg,
            ) {
                Text(
                    text = "显示更多评价",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    textAlign = TextAlign.Center,
                    color = buttonText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
fun ReviewCarouselCard(review: HostProfileReviewUi, isDarkTheme: Boolean) {
    val cardColor = if (isDarkTheme) Color(0xFF1B1B1B) else Color(0xFFFFFFFF)
    val textPrimary = if (isDarkTheme) Color.White else Color(0xFF222222)
    val textSecondary = if (isDarkTheme) Color(0xFFA8A8A8) else Color(0xFF777777)
    val starTint = Color(0xFF111111)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDarkTheme) 1.dp else 0.dp,
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ReviewAvatar(
                    photoUrl = review.reviewerPhotoUrl,
                    name = review.reviewerName,
                    size = 40.dp,
                    isDarkTheme = isDarkTheme,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = review.reviewerName,
                        color = textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        repeat(5) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = starTint,
                                modifier = Modifier.size(13.dp),
                            )
                        }
                        Text(
                            text = " · ${review.timeAgoText}",
                            color = textSecondary,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
            Text(
                text = review.reviewText,
                color = textPrimary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
        }
    }
}

@Composable
fun ReviewAvatar(
    photoUrl: String,
    name: String,
    size: Dp,
    isDarkTheme: Boolean,
) {
    val placeholder = if (isDarkTheme) Color(0xFF3D3D3D) else Color(0xFFD8D8D8)
    Surface(modifier = Modifier.size(size), shape = CircleShape, color = placeholder) {
        if (photoUrl.isNotEmpty()) {
            AsyncImage(
                model = RetrofitClient.normalizeBackendMediaUrlForDisplay(photoUrl),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
            )
        } else {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    text = name.take(1).ifEmpty { "?" },
                    color = if (isDarkTheme) Color(0xFFCCCCCC) else Color(0xFF555555),
                    fontSize = (size.value * 0.38f).sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
fun AllReviewsSheetContent(
    reviews: List<HostProfileReviewUi>,
    isDarkTheme: Boolean,
    onClose: () -> Unit,
) {
    val textPrimary = if (isDarkTheme) Color.White else Color(0xFF222222)
    val textSecondary = if (isDarkTheme) Color(0xFF9E9E9E) else Color(0xFF666666)
    val divider = if (isDarkTheme) Color(0xFF333333) else Color(0xFFE8E8E8)
    val starTint = Color(0xFF111111)

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = DialogTitleTopPadding, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${reviews.size} 条评价",
                    color = textPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "关闭",
                        tint = textPrimary,
                    )
                }
            }
        }
        items(count = reviews.size, key = { it }) { index ->
            val review = reviews[index]
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    ReviewAvatar(
                        photoUrl = review.reviewerPhotoUrl,
                        name = review.reviewerName,
                        size = 44.dp,
                        isDarkTheme = isDarkTheme,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = review.reviewerName,
                            color = textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        review.locationLine?.let { loc ->
                            if (loc.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = loc,
                                    color = textSecondary,
                                    fontSize = 13.sp,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            repeat(5) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = starTint,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                            Text(
                                text = " · ${review.timeAgoText}",
                                color = textSecondary,
                                fontSize = 12.sp,
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = review.reviewText,
                            color = textPrimary,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                        )
                    }
                }
            }
            if (index < reviews.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    thickness = 1.dp,
                    color = divider,
                )
            }
        }
    }
}
