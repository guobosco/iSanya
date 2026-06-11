// 文件说明：首页「体验」Tab — 纵向分类 + 横向体验卡片（分页加载）。

@file:OptIn(kotlinx.coroutines.FlowPreview::class)

package com.example.Lulu.ui.screen

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.Lulu.R
import com.example.Lulu.data.model.experienceCategorySeeds
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlin.random.Random

private const val HORIZONTAL_PAGE_SIZE = 10
private const val HORIZONTAL_MAX_ITEMS = 100

@Immutable
data class ExperienceSectionModel(
    val id: String,
    val title: String,
    /** 与 [experienceCategorySeeds] 下标对应，用于生成该类下的体验标题与元信息 */
    val categoryIndex: Int,
)

@Immutable
data class ExperienceItemUi(
    val id: String,
    val title: String,
    val metaLine: String,
    val priceFromYuan: Int,
    val badgeLabel: String,
    val imageUrl: String,
)

/** 从列表卡片稳定 id（`{sectionId}_card_{index}`）解析出重建体验条目所需的参数。 */
internal fun parseExperienceStableId(experienceId: String): Triple<String, Int, Int>? {
    val trimmed = experienceId.trim()
    val match = Regex("^(.+)_card_(\\d+)$").matchEntire(trimmed) ?: return null
    val sectionId = match.groupValues[1]
    val index = match.groupValues[2].toIntOrNull() ?: return null
    val categoryIndex = when {
        sectionId.startsWith("exp_sec_") -> sectionId.removePrefix("exp_sec_").toIntOrNull()
        sectionId.startsWith("feed_") -> sectionId.removePrefix("feed_").toIntOrNull()
        else -> null
    } ?: return null
    if (categoryIndex !in experienceCategorySeeds.indices) return null
    return Triple(sectionId, categoryIndex, index)
}

internal fun experienceItemFromStableId(experienceId: String): ExperienceItemUi? {
    val (sectionId, categoryIndex, index) = parseExperienceStableId(experienceId) ?: return null
    return buildExperienceItems(sectionId, categoryIndex, index, 1).firstOrNull()
}

internal fun experienceCategoryTitleForStableId(experienceId: String): String {
    val (_, categoryIndex, _) = parseExperienceStableId(experienceId) ?: return ""
    return experienceCategorySeeds.getOrNull(categoryIndex)?.title.orEmpty()
}

internal fun buildExperienceItems(
    sectionId: String,
    categoryIndex: Int,
    startIndex: Int,
    count: Int,
): List<ExperienceItemUi> {
    val cat = experienceCategorySeeds[categoryIndex % experienceCategorySeeds.size]
    val rnd = Random(categoryIndex * 31_337 + startIndex)
    return List(count) { offset ->
        val index = startIndex + offset
        val title = cat.itemTitles[index % cat.itemTitles.size]
        val (kind, dur) = cat.metas[(index * 2 + categoryIndex) % cat.metas.size]
        val price = 120 + rnd.nextInt(680)
        val badge = if ((index + categoryIndex) % 4 == 0) "热门" else "精选"
        val imageKey = "${sectionId}_${index}_${categoryIndex}".hashCode().toString()
        val url = "https://picsum.photos/seed/$imageKey/400/520"
        ExperienceItemUi(
            id = "${sectionId}_card_$index",
            title = title,
            metaLine = "$kind · $dur",
            priceFromYuan = price,
            badgeLabel = badge,
            imageUrl = url,
        )
    }
}

private fun initialSections(): List<ExperienceSectionModel> =
    List(experienceCategorySeeds.size) { idx ->
        ExperienceSectionModel(
            id = "exp_sec_$idx",
            title = experienceCategorySeeds[idx].title,
            categoryIndex = idx,
        )
    }

@Composable
fun ExperienceTabContent(
    modifier: Modifier = Modifier,
    verticalListState: LazyListState? = null,
    refreshNonce: Int = 0,
    onOpenCategoryFeed: (categoryIndex: Int) -> Unit = { _ -> },
    onOpenExperienceDetail: (ExperienceItemUi) -> Unit = {},
) {
    val lazyListState = verticalListState ?: rememberLazyListState()

    key(refreshNonce) {
        val sections = remember { initialSections() }

        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(Color.White),
            state = lazyListState,
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            items(
                count = sections.size,
                key = { sections[it].id },
            ) { index ->
                ExperienceCategoryRow(
                    section = sections[index],
                    isFirstSection = index == 0,
                    onOpenCategory = { onOpenCategoryFeed(sections[index].categoryIndex) },
                    onOpenExperienceDetail = onOpenExperienceDetail,
                )
            }
        }
    }
}

@Composable
private fun ExperienceCategoryRow(
    section: ExperienceSectionModel,
    isFirstSection: Boolean,
    onOpenCategory: () -> Unit,
    onOpenExperienceDetail: (ExperienceItemUi) -> Unit,
) {
    var cards by remember(section.id) {
        mutableStateOf(
            buildExperienceItems(section.id, section.categoryIndex, 0, HORIZONTAL_PAGE_SIZE),
        )
    }
    var rowBusy by remember(section.id) { mutableStateOf(false) }
    val rowState = rememberLazyListState()

    LaunchedEffect(section.id, rowState, cards.size, rowBusy) {
        snapshotFlow {
            val info = rowState.layoutInfo
            if (info.visibleItemsInfo.isEmpty()) return@snapshotFlow false
            val lastVisible = info.visibleItemsInfo.last().index
            val total = info.totalItemsCount
            val nearEnd = lastVisible >= total - 2 && cards.size < HORIZONTAL_MAX_ITEMS
            nearEnd && !rowBusy
        }
            .distinctUntilChanged()
            .filter { it }
            .debounce(80)
            .collect {
                rowBusy = true
                val start = cards.size
                val add = HORIZONTAL_PAGE_SIZE.coerceAtMost(HORIZONTAL_MAX_ITEMS - start)
                if (add > 0) {
                    cards = cards + buildExperienceItems(section.id, section.categoryIndex, start, add)
                }
                rowBusy = false
            }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (isFirstSection) 10.dp else 22.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = section.title,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp, end = 4.dp)
                    .clickable(onClick = onOpenCategory),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 26.sp,
            )
            IconButton(onClick = onOpenCategory) {
                Icon(
                    imageVector = Icons.Filled.ArrowForwardIos,
                    contentDescription = stringResource(R.string.experience_open_category_cd),
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            state = rowState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(
                items = cards,
                key = { it.id },
            ) { item ->
                ExperienceCard(
                    item = item,
                    onOpenDetail = { onOpenExperienceDetail(item) },
                )
            }
            if (rowBusy && cards.size < HORIZONTAL_MAX_ITEMS) {
                item(key = "${section.id}_h_load") {
                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .height(200.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ExperienceCard(
    item: ExperienceItemUi,
    onOpenDetail: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var favorited by remember(item.id) { mutableStateOf(false) }
    val cardWidth = 158.dp
    val imageHeight = 200.dp
    val corner = 14.dp
    val metaColor = MaterialTheme.colorScheme.onSurfaceVariant
    val boldColor = MaterialTheme.colorScheme.onBackground

    Column(
        modifier = Modifier
            .width(cardWidth)
            .clickable(onClick = onOpenDetail),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(imageHeight)
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
                    .padding(8.dp),
                shape = RoundedCornerShape(50),
                color = Color.White,
                shadowElevation = 0.dp,
                tonalElevation = 0.dp,
            ) {
                Text(
                    text = item.badgeLabel,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = Color.Black,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.92f))
                    .clickable { favorited = !favorited },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (favorited) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    tint = if (favorited) Color(0xFFFF5A5F) else Color(0xFF333333),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = item.title,
            color = boldColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
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
