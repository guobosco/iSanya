// 文件说明：体验分类流 — 使用真实体验数据按分类展示。

package com.example.Lulu.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.Lulu.R
import com.example.Lulu.data.model.Experience
import com.example.Lulu.data.remote.RetrofitClient
import com.example.Lulu.ui.navigation.Screen
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val ExperienceFeedPageWhite = Color(0xFFFFFFFF)

private suspend fun loadCategoryExperiences(categoryTitle: String): List<ExperienceItemUi> = withContext(Dispatchers.IO) {
    runCatching {
        RetrofitClient.apiService.getDiscoveryExperiences(limit = 200)
            .filter { !it.isDeleted && normalizeExperienceCategory(it.category) == categoryTitle }
            .sortedWith(compareByDescending<Experience> { it.updatedAt }.thenByDescending { it.createdAt })
            .map { it.toUiModel() }
    }.getOrElse { emptyList() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExperienceCategoryFeedScreen(
    navController: NavController,
    categoryTitle: String,
) {
    val listState = rememberLazyListState()
    val items by produceState<List<ExperienceItemUi>>(initialValue = emptyList(), key1 = categoryTitle) {
        value = loadCategoryExperiences(categoryTitle)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(ExperienceFeedPageWhite),
        containerColor = ExperienceFeedPageWhite,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = categoryTitle,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        lineHeight = 22.sp,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.search_back_cd),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ExperienceFeedPageWhite,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { paddingValues ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "当前分类暂无真实体验数据",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(ExperienceFeedPageWhite),
                state = listState,
            ) {
                items(
                    items = items,
                    key = { it.id },
                ) { item ->
                    ExperienceFeedListRow(
                        item = item,
                        onOpenDetail = {
                            navController.navigate(Screen.ExperienceDetail.createRoute(item.id))
                        },
                    )
                    if (item.id != items.lastOrNull()?.id) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExperienceFeedListRow(
    item: ExperienceItemUi,
    onOpenDetail: () -> Unit,
) {
    val context = LocalContext.current
    val metaColor = MaterialTheme.colorScheme.onSurfaceVariant
    val boldColor = MaterialTheme.colorScheme.onBackground
    val thumbSize = 100.dp
    val corner = 12.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ExperienceFeedPageWhite)
            .clickable(onClick = onOpenDetail)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(thumbSize)
                .clip(RoundedCornerShape(corner)),
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(item.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp),
                shape = RoundedCornerShape(50),
                color = Color.White,
                shadowElevation = 0.dp,
                tonalElevation = 0.dp,
            ) {
                Text(
                    text = item.badgeLabel,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    color = Color.Black,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            Text(
                text = item.title,
                color = boldColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.metaLine,
                color = metaColor,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (item.priceFromYuan > 0) {
                        "${stringResource(R.string.experience_per_guest_prefix)} ${stringResource(R.string.experience_yuan_symbol)}${item.priceFromYuan}${stringResource(R.string.experience_price_from_suffix)}"
                    } else {
                        "价格待定"
                    },
                    color = boldColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
