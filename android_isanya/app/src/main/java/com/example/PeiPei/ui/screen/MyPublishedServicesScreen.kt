// 文件说明：「我发布的」管理页（已发布 / 草稿 / 已取消）。

package com.example.Lulu.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as lazyGridItems
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyColumnItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.Lulu.data.local.AppDataStore
import com.example.Lulu.data.model.Service
import com.example.Lulu.data.repository.LuluRepository
import com.example.Lulu.ui.navigation.Screen
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.flowOf

private val PageBg = Color(0xFFFFFFFF)
private val TitleColor = Color(0xFF000000)
private val SubtitleColor = Color(0xFF888888)
private val SectionTitleColor = Color(0xFF000000)
private val IconCircleBg = Color(0xFFF0F0F0)
private val ActionStripBg = Color(0xFFF7F7F7)
private val ActionDangerColor = Color(0xFFE0115F)
private val HorizontalInset = 20.dp
private val SearchBarFontSize = 14.sp
private val SearchBarLineHeight = 18.sp
private val SearchBarHeight = 30.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPublishedServicesScreen(navController: NavController) {
    val repository = remember { runCatching { LuluRepository.get() }.getOrNull() }
    val hubFlow = remember(repository) {
        repository?.myCreatedListingsHub ?: flowOf(emptyList())
    }
    val rawListings by hubFlow.collectAsState(initial = emptyList())
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var gridMode by rememberSaveable { mutableStateOf(false) }
    var unpublishTargetId by remember { mutableStateOf<String?>(null) }
    var deleteRecordStep1Id by remember { mutableStateOf<String?>(null) }
    var deleteRecordStep2Id by remember { mutableStateOf<String?>(null) }

    val q = searchQuery.trim()
    val filtered = remember(rawListings, q) {
        if (q.isEmpty()) rawListings
        else {
            val needle = q.lowercase()
            rawListings.filter { s ->
                s.title.lowercase().contains(needle) ||
                    s.category.lowercase().contains(needle) ||
                    s.location.lowercase().contains(needle)
            }
        }
    }
    val published = remember(filtered) { filtered.filter { !it.isDeleted && !it.isDraft } }
    val drafts = remember(filtered) { filtered.filter { !it.isDeleted && it.isDraft } }
    val cancelled = remember(filtered) { filtered.filter { it.isDeleted } }

    Scaffold(
        containerColor = PageBg,
        topBar = {
            CenterAlignedTopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = TitleColor)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = PageBg)
            )
        }
    ) { padding ->
        if (gridMode) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(
                    start = HorizontalInset,
                    end = HorizontalInset,
                    top = 4.dp,
                    bottom = 24.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item(span = { GridItemSpan(this.maxLineSpan) }) {
                    HubHeader(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        gridMode = gridMode,
                        onToggleGrid = { gridMode = !gridMode },
                        onAdd = {
                            navController.navigate(Screen.CreateService.createRoute()) {
                                launchSingleTop = true
                            }
                        },
                    )
                }
                item(span = { GridItemSpan(this.maxLineSpan) }) {
                    SectionTitle("已发布")
                }
                if (published.isEmpty()) {
                    item(span = { GridItemSpan(this.maxLineSpan) }) {
                        EmptyLine("暂无已发布")
                    }
                } else {
                    lazyGridItems(published, key = { it.id }) { s ->
                        ListingCard(
                            service = s,
                            compact = true,
                            onClick = { onListingClick(navController, s) },
                            actions = {
                                ListingCardActionStrip(
                                    compact = true,
                                    actions = listOf(
                                        Triple("编辑修改", false) {
                                            navController.navigate(
                                                Screen.CreateService.createRoute(serviceId = s.id)
                                            ) { launchSingleTop = true }
                                        },
                                        Triple("日历设置", false) {
                                            navController.navigate(
                                                Screen.PublishedServiceCalendar.createRoute(serviceId = s.id)
                                            ) { launchSingleTop = true }
                                        },
                                        Triple("修改价格", false) {
                                            navController.navigate(
                                                Screen.ServiceDetail.createRoute(s.id, openCreatorPrice = true)
                                            ) { launchSingleTop = true }
                                        },
                                        Triple("取消发布", true) {
                                            unpublishTargetId = s.id
                                        },
                                    ),
                                )
                            },
                        )
                    }
                }
                item(span = { GridItemSpan(this.maxLineSpan) }) {
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionTitle("草稿箱")
                }
                if (drafts.isEmpty()) {
                    item(span = { GridItemSpan(this.maxLineSpan) }) {
                        EmptyLine("暂无草稿")
                    }
                } else {
                    lazyGridItems(drafts, key = { it.id }) { s ->
                        ListingCard(
                            service = s,
                            compact = true,
                            onClick = { onListingClick(navController, s) },
                            actions = {
                                ListingCardActionStrip(
                                    compact = true,
                                    actions = listOf(
                                        Triple("编辑发布", false) {
                                            navController.navigate(
                                                Screen.CreateService.createRoute(serviceId = s.id)
                                            ) { launchSingleTop = true }
                                        },
                                        Triple("删除记录", true) { deleteRecordStep1Id = s.id },
                                    ),
                                )
                            },
                        )
                    }
                }
                item(span = { GridItemSpan(this.maxLineSpan) }) {
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionTitle("已取消")
                }
                if (cancelled.isEmpty()) {
                    item(span = { GridItemSpan(this.maxLineSpan) }) {
                        EmptyLine("暂无已取消")
                    }
                } else {
                    lazyGridItems(cancelled, key = { it.id }) { s ->
                        ListingCard(
                            service = s,
                            compact = true,
                            onClick = { onListingClick(navController, s) },
                            actions = {
                                ListingCardActionStrip(
                                    compact = true,
                                    actions = listOf(
                                        Triple("编辑修改", false) {
                                            navController.navigate(
                                                Screen.CreateService.createRoute(serviceId = s.id)
                                            ) { launchSingleTop = true }
                                        },
                                        Triple("重新发布", false) { AppDataStore.republishService(s.id) },
                                        Triple("删除记录", true) { deleteRecordStep1Id = s.id },
                                    ),
                                )
                            },
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(
                    start = HorizontalInset,
                    end = HorizontalInset,
                    top = 4.dp,
                    bottom = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                item {
                    HubHeader(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        gridMode = gridMode,
                        onToggleGrid = { gridMode = !gridMode },
                        onAdd = {
                            navController.navigate(Screen.CreateService.createRoute()) {
                                launchSingleTop = true
                            }
                        },
                    )
                }
                item {
                    SectionTitle("已发布")
                }
                if (published.isEmpty()) {
                    item { EmptyLine("暂无已发布") }
                } else {
                    lazyColumnItems(published, key = { it.id }) { s ->
                        ListingCard(
                            service = s,
                            compact = false,
                            onClick = { onListingClick(navController, s) },
                            actions = {
                                ListingCardActionStrip(
                                    compact = false,
                                    actions = listOf(
                                        Triple("编辑修改", false) {
                                            navController.navigate(
                                                Screen.CreateService.createRoute(serviceId = s.id)
                                            ) { launchSingleTop = true }
                                        },
                                        Triple("日历设置", false) {
                                            navController.navigate(
                                                Screen.PublishedServiceCalendar.createRoute(serviceId = s.id)
                                            ) { launchSingleTop = true }
                                        },
                                        Triple("修改价格", false) {
                                            navController.navigate(
                                                Screen.ServiceDetail.createRoute(s.id, openCreatorPrice = true)
                                            ) { launchSingleTop = true }
                                        },
                                        Triple("取消发布", true) { unpublishTargetId = s.id },
                                    ),
                                )
                            },
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    SectionTitle("草稿箱")
                }
                if (drafts.isEmpty()) {
                    item { EmptyLine("暂无草稿") }
                } else {
                    lazyColumnItems(drafts, key = { it.id }) { s ->
                        ListingCard(
                            service = s,
                            compact = false,
                            onClick = { onListingClick(navController, s) },
                            actions = {
                                ListingCardActionStrip(
                                    compact = false,
                                    actions = listOf(
                                        Triple("编辑发布", false) {
                                            navController.navigate(
                                                Screen.CreateService.createRoute(serviceId = s.id)
                                            ) { launchSingleTop = true }
                                        },
                                        Triple("删除记录", true) { deleteRecordStep1Id = s.id },
                                    ),
                                )
                            },
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    SectionTitle("已取消")
                }
                if (cancelled.isEmpty()) {
                    item { EmptyLine("暂无已取消") }
                } else {
                    lazyColumnItems(cancelled, key = { it.id }) { s ->
                        ListingCard(
                            service = s,
                            compact = false,
                            onClick = { onListingClick(navController, s) },
                            actions = {
                                ListingCardActionStrip(
                                    compact = false,
                                    actions = listOf(
                                        Triple("编辑修改", false) {
                                            navController.navigate(
                                                Screen.CreateService.createRoute(serviceId = s.id)
                                            ) { launchSingleTop = true }
                                        },
                                        Triple("重新发布", false) { AppDataStore.republishService(s.id) },
                                        Triple("删除记录", true) { deleteRecordStep1Id = s.id },
                                    ),
                                )
                            },
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }
            }
        }
    }

    if (unpublishTargetId != null) {
        AlertDialog(
            onDismissRequest = { unpublishTargetId = null },
            title = { Text("取消发布") },
            text = { Text("下架后用户将无法再浏览或预订该服务，条目会出现在「我发布的 · 已取消」。确定吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        AppDataStore.deleteService(unpublishTargetId!!)
                        unpublishTargetId = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = ActionDangerColor),
                ) {
                    Text("确定下架")
                }
            },
            dismissButton = {
                TextButton(onClick = { unpublishTargetId = null }) {
                    Text("返回")
                }
            },
        )
    }
    if (deleteRecordStep1Id != null) {
        AlertDialog(
            onDismissRequest = { deleteRecordStep1Id = null },
            title = { Text("删除记录") },
            text = { Text("确定要删除这条记录吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteRecordStep2Id = deleteRecordStep1Id
                        deleteRecordStep1Id = null
                    }
                ) {
                    Text("继续")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteRecordStep1Id = null }) {
                    Text("取消")
                }
            },
        )
    }
    if (deleteRecordStep2Id != null) {
        AlertDialog(
            onDismissRequest = { deleteRecordStep2Id = null },
            title = { Text("再次确认") },
            text = { Text("删除后无法恢复，确定删除？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        AppDataStore.purgeCreatedServiceRecord(deleteRecordStep2Id!!)
                        deleteRecordStep2Id = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = ActionDangerColor),
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteRecordStep2Id = null }) {
                    Text("取消")
                }
            },
        )
    }
}

private fun onListingClick(navController: NavController, service: Service) {
    when {
        service.isDeleted -> navController.navigate(
            Screen.ServiceDetail.createRoute(service.id)
        ) { launchSingleTop = true }
        service.isDraft -> navController.navigate(
            Screen.CreateService.createRoute(serviceId = service.id)
        ) { launchSingleTop = true }
        else -> navController.navigate(
            Screen.ServiceDetail.createRoute(service.id)
        ) { launchSingleTop = true }
    }
}

@Composable
private fun HubHeader(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    gridMode: Boolean,
    onToggleGrid: () -> Unit,
    onAdd: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "我发布的",
                color = TitleColor,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp, bottom = 16.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircleIconButton(onClick = onToggleGrid) {
                    Icon(
                        imageVector = if (gridMode) Icons.Outlined.ViewList else Icons.Outlined.Apps,
                        contentDescription = if (gridMode) "列表" else "网格",
                        tint = TitleColor,
                        modifier = Modifier.size(20.dp),
                    )
                }
                CircleIconButton(onClick = onAdd) {
                    Icon(Icons.Default.Add, contentDescription = "新建", tint = TitleColor, modifier = Modifier.size(22.dp))
                }
            }
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(SearchBarHeight),
            shape = RoundedCornerShape(10.dp),
            color = IconCircleBg,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(horizontal = 10.dp, vertical = 0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = SubtitleColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                val searchTextStyle = TextStyle(
                    fontSize = SearchBarFontSize,
                    lineHeight = SearchBarLineHeight,
                    color = TitleColor,
                )
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    singleLine = true,
                    textStyle = searchTextStyle,
                    cursorBrush = SolidColor(TitleColor),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "搜索标题、类目或地点",
                                    color = SubtitleColor,
                                    fontSize = SearchBarFontSize,
                                    lineHeight = SearchBarLineHeight,
                                    maxLines = 1,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun CircleIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(IconCircleBg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = SectionTitleColor,
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
    )
}

@Composable
private fun EmptyLine(text: String) {
    Text(
        text = text,
        color = SubtitleColor,
        fontSize = 14.sp,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun ListingCardActionStrip(
    compact: Boolean,
    actions: List<Triple<String, Boolean, () -> Unit>>,
) {
    HorizontalDivider(thickness = 1.dp, color = Color(0xFFE8E8E8))
    val fontSize = if (compact) 11.sp else 13.sp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ActionStripBg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        actions.forEach { (label, danger, onClick) ->
            TextButton(
                onClick = onClick,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 36.dp),
                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
            ) {
                Text(
                    text = label,
                    fontSize = fontSize,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (danger) ActionDangerColor else TitleColor,
                )
            }
        }
    }
}

@Composable
private fun ListingCard(
    service: Service,
    compact: Boolean,
    onClick: () -> Unit,
    actions: (@Composable () -> Unit)? = null,
) {
    val thumb = service.coverImageUrl.ifBlank { service.imageUrls.firstOrNull().orEmpty() }
    val subtitle = buildString {
        append(service.category.ifBlank { "服务" })
        if (service.location.isNotBlank()) {
            if (isNotEmpty()) append(" · ")
            append(service.location)
        }
    }.ifBlank { "未填写地点" }

    val statusDotColor: Color? = when {
        service.isDeleted -> Color(0xFF9E9E9E)
        service.isDraft -> Color(0xFFFF9800)
        else -> null
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = if (compact) 0.dp else 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                AsyncImage(
                    model = thumb.ifBlank { null },
                    contentDescription = null,
                    modifier = Modifier
                        .size(if (compact) 72.dp else 88.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFECECEC)),
                    contentScale = ContentScale.Crop,
                )
                statusDotColor?.let { dot ->
                    Box(
                        modifier = Modifier
                            .padding(6.dp)
                            .size(10.dp)
                            .align(Alignment.TopStart)
                            .background(dot, CircleShape)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .then(if (compact) Modifier.height(72.dp) else Modifier),
                verticalArrangement = if (compact) Arrangement.Center else Arrangement.Top,
            ) {
                Text(
                    text = service.title.ifBlank { "未命名" },
                    color = TitleColor,
                    fontSize = if (compact) 14.sp else 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = if (compact) 2 else 3,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!compact) Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    color = SubtitleColor,
                    fontSize = if (compact) 12.sp else 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (actions != null) {
            actions()
        }
    }
}
