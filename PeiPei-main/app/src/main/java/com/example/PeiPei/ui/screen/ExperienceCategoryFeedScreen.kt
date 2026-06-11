// 文件说明：体验分类「信息瀑布流」— 纵向列表，左图右文，支持下滑加载更多。

@file:OptIn(kotlinx.coroutines.FlowPreview::class)

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import com.example.Lulu.ui.navigation.Screen
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.Lulu.R
import com.example.Lulu.data.model.experienceCategorySeeds
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import androidx.compose.ui.platform.LocalContext

private const val FEED_PAGE_SIZE = 12
private const val FEED_MAX_ITEMS = 300

private val ExperienceFeedPageWhite = Color(0xFFFFFFFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExperienceCategoryFeedScreen(
    navController: NavController,
    categoryIndex: Int,
) {
    val safeIndex = categoryIndex.coerceIn(0, experienceCategorySeeds.lastIndex)
    val listState = rememberLazyListState()
    var items by remember(safeIndex) {
        mutableStateOf(
            buildExperienceItems("feed_$safeIndex", safeIndex, 0, FEED_PAGE_SIZE),
        )
    }
    var loadBusy by remember(safeIndex) { mutableStateOf(false) }

    LaunchedEffect(listState, items.size, loadBusy, safeIndex) {
        snapshotFlow {
            val info = listState.layoutInfo
            if (info.visibleItemsInfo.isEmpty()) return@snapshotFlow false
            val last = info.visibleItemsInfo.last().index
            val total = info.totalItemsCount
            last >= total - 3 && items.size < FEED_MAX_ITEMS
        }
            .distinctUntilChanged()
            .filter { it }
            .debounce(100)
            .collect {
                if (loadBusy) return@collect
                loadBusy = true
                val start = items.size
                val add = FEED_PAGE_SIZE.coerceAtMost(FEED_MAX_ITEMS - start)
                if (add > 0) {
                    items = items + buildExperienceItems("feed_$safeIndex", safeIndex, start, add)
                }
                loadBusy = false
            }
    }

    val toolbarTitle = experienceCategorySeeds[safeIndex].title

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(ExperienceFeedPageWhite),
        containerColor = ExperienceFeedPageWhite,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = toolbarTitle,
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(ExperienceFeedPageWhite),
            state = listState,
        ) {
            items(
                count = items.size,
                key = { items[it].id },
            ) { index ->
                ExperienceFeedListRow(
                    item = items[index],
                    onOpenDetail = {
                        navController.navigate(Screen.ExperienceDetail.createRoute(items[index].id))
                    },
                )
                if (index < items.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                    )
                }
            }
            if (loadBusy && items.size < FEED_MAX_ITEMS) {
                item(key = "feed_loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
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
    var favorited by remember(item.id) { mutableStateOf(false) }
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
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.92f))
                    .clickable { favorited = !favorited },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (favorited) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    tint = if (favorited) Color(0xFFFF5A5F) else Color(0xFF333333),
                    modifier = Modifier.size(16.dp),
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
                    text = stringResource(R.string.experience_per_guest_prefix),
                    color = metaColor,
                    fontSize = 12.sp,
                )
                Text(
                    text = stringResource(R.string.experience_yuan_symbol),
                    color = boldColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = item.priceFromYuan.toString(),
                    color = boldColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.experience_price_from_suffix),
                    color = metaColor,
                    fontSize = 12.sp,
                )
            }
        }
    }
}
