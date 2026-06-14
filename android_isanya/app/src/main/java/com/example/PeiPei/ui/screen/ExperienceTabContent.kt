// 文件说明：首页「体验」Tab — 使用真实后端体验数据按分类展示。

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.Lulu.R
import com.example.Lulu.data.model.Experience
import com.example.Lulu.data.remote.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val EXPERIENCE_SECTION_PREVIEW_LIMIT = 10

@Immutable
data class ExperienceSectionModel(
    val id: String,
    val title: String,
    val items: List<ExperienceItemUi>,
)

@Immutable
data class ExperienceItemUi(
    val id: String,
    val title: String,
    val metaLine: String,
    val priceFromYuan: Int,
    val priceText: String,
    val priceBasisText: String,
    val badgeLabel: String,
    val imageUrl: String,
    val imageUrls: List<String>,
    val description: String,
    val location: String,
    val categoryTitle: String,
    val hostId: String,
    val hostName: String,
    val tags: List<String>,
)

private suspend fun loadDiscoveryExperiences(): List<Experience> = withContext(Dispatchers.IO) {
    runCatching {
        RetrofitClient.apiService.getDiscoveryExperiences(limit = 200)
            .filter { !it.isDeleted }
            .sortedWith(compareByDescending<Experience> { it.updatedAt }.thenByDescending { it.createdAt })
    }.getOrElse { emptyList() }
}

fun normalizeExperienceCategory(raw: String): String {
    val trimmed = raw.trim()
    return if (trimmed.isEmpty()) "其他体验" else trimmed
}

private fun buildExperienceMetaLine(experience: Experience): String {
    val parts = listOf(
        experience.durationText.trim(),
        experience.location.trim(),
    ).filter { it.isNotEmpty() }
    return parts.joinToString(" · ").ifBlank { "当地热门体验" }
}

private fun extractPriceFromText(priceText: String): Int {
    val match = Regex("(\\d+)").find(priceText)
    return match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
}

private fun appendImageVersion(url: String, version: Long): String {
    if (url.isBlank() || version <= 0L) {
        return url
    }
    val separator = if (url.contains("?")) "&" else "?"
    return "${url}${separator}v=${version}"
}

fun Experience.toUiModel(): ExperienceItemUi {
    val resolvedCategory = normalizeExperienceCategory(category)
    val imageVersion = updatedAt.takeIf { it > 0L } ?: createdAt
    val normalizedImages = imageUrls
        .map { RetrofitClient.normalizeBackendMediaUrlForDisplay(it) }
        .filter { it.isNotBlank() }
    val resolvedImages = normalizedImages.map { appendImageVersion(it, imageVersion) }
    val normalizedCover = RetrofitClient.normalizeBackendMediaUrlForDisplay(
        coverImageUrl.ifBlank { normalizedImages.firstOrNull().orEmpty() }
    )
    val resolvedCover = appendImageVersion(normalizedCover, imageVersion)
    val safePriceText = priceText.trim()
    return ExperienceItemUi(
        id = id,
        title = title.ifBlank { "未命名体验" },
        metaLine = buildExperienceMetaLine(this),
        priceFromYuan = extractPriceFromText(safePriceText),
        priceText = if (safePriceText.isBlank()) "¥ 待定" else safePriceText,
        priceBasisText = priceBasisText.trim(),
        badgeLabel = badgeText.trim().ifBlank { resolvedCategory },
        imageUrl = resolvedCover,
        imageUrls = if (resolvedImages.isEmpty()) listOf(resolvedCover).filter { it.isNotBlank() } else resolvedImages,
        description = description.trim(),
        location = location.trim(),
        categoryTitle = resolvedCategory,
        hostId = hostId,
        hostName = hostName.trim(),
        tags = tags.filter { it.isNotBlank() }
    )
}

private fun buildSections(experiences: List<Experience>): List<ExperienceSectionModel> {
    return experiences
        .groupBy { normalizeExperienceCategory(it.category) }
        .toList()
        .sortedBy { it.first }
        .map { (category, items) ->
            ExperienceSectionModel(
                id = category,
                title = category,
                items = items.take(EXPERIENCE_SECTION_PREVIEW_LIMIT).map { it.toUiModel() }
            )
        }
}

@Composable
fun ExperienceTabContent(
    modifier: Modifier = Modifier,
    verticalListState: LazyListState? = null,
    refreshNonce: Int = 0,
    onOpenCategoryFeed: (categoryTitle: String) -> Unit = { _ -> },
    onOpenExperienceDetail: (ExperienceItemUi) -> Unit = {},
) {
    val lazyListState = verticalListState ?: rememberLazyListState()
    val experiences by produceState<List<Experience>>(initialValue = emptyList(), key1 = refreshNonce) {
        value = loadDiscoveryExperiences()
    }
    val sections = remember(experiences) { buildSections(experiences) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White),
        state = lazyListState,
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        if (sections.isEmpty()) {
            item(key = "empty_experience") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 28.dp)
                ) {
                    Text(
                        text = "体验",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "当前还没有可展示的真实体验数据。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                    )
                }
            }
        } else {
            items(
                items = sections,
                key = { it.id },
            ) { section ->
                ExperienceCategoryRow(
                    section = section,
                    isFirstSection = section.id == sections.firstOrNull()?.id,
                    onOpenCategory = { onOpenCategoryFeed(section.title) },
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
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(
                items = section.items,
                key = { it.id },
            ) { item ->
                ExperienceCard(
                    item = item,
                    onOpenDetail = { onOpenExperienceDetail(item) },
                )
            }
        }
    }
}

@Composable
private fun ExperienceCard(
    item: ExperienceItemUi,
    onOpenDetail: () -> Unit,
) {
    val context = LocalContext.current
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
                text = if (item.priceFromYuan > 0) "${stringResource(R.string.experience_yuan_symbol)}${item.priceFromYuan}" else "待定",
                color = boldColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
            if (item.priceFromYuan > 0) {
                Text(
                    text = stringResource(R.string.experience_price_from_suffix),
                    color = metaColor,
                    fontSize = 12.sp,
                )
            }
        }
    }
}
