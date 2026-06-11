// 文件说明：广场/动态流等社区内容列表界面。

package com.example.Lulu.ui.screen

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.delay
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import com.example.Lulu.data.local.MockDataStore
import com.example.Lulu.data.remote.RetrofitClient
import com.example.Lulu.data.model.Service
import com.example.Lulu.data.repository.LuluRepository
import com.example.Lulu.ui.components.FeiLingPullRefreshHintIndicator
import com.example.Lulu.ui.components.FeiLingTopSyncIndicator
import com.example.Lulu.ui.navigation.Screen
import com.example.Lulu.ui.util.PullRefreshTokens
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.absoluteValue

private data class WishlistGroup(
    val key: String,
    val name: String,
    val services: List<Service>
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun SquareScreen(
    navController: NavController,
    mainShellNavController: NavController? = null
) {
    val view = LocalView.current
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()
    val pageColor = if (isDarkTheme) Color.Black else Color.White
    val cardBackground = if (isDarkTheme) Color(0xFF1E1E1E) else Color.White
    val primaryText = if (isDarkTheme) Color(0xFFF2F2F2) else Color(0xFF161616)
    val secondaryText = if (isDarkTheme) Color(0xFFA2A2A2) else Color(0xFF727272)

    val services by MockDataStore.services.collectAsState()
    val favoriteServiceIds by MockDataStore.favoriteServiceIds.collectAsState()
    val wishlistGroups by MockDataStore.wishlistGroups.collectAsState()
    val favoriteServiceGroups by MockDataStore.favoriteServiceGroups.collectAsState()
    val currentUser by MockDataStore.currentUser.collectAsState()
    val scope = rememberCoroutineScope()
    val serviceDetailNav = mainShellNavController ?: navController
    var showGroupEditor by remember { mutableStateOf(false) }
    var expandedWishlistGroupName by rememberSaveable { mutableStateOf<String?>(null) }
    var savedExpandedFirstVisibleIndex by rememberSaveable(expandedWishlistGroupName) { mutableIntStateOf(0) }
    var savedExpandedFirstVisibleOffset by rememberSaveable(expandedWishlistGroupName) { mutableIntStateOf(0) }
    var hasRestoredExpandedScroll by rememberSaveable(expandedWishlistGroupName) { mutableStateOf(false) }
    val expandedGridState = rememberLazyStaggeredGridState(
        initialFirstVisibleItemIndex = savedExpandedFirstVisibleIndex,
        initialFirstVisibleItemScrollOffset = savedExpandedFirstVisibleOffset
    )
    var savedRootFirstVisibleIndex by rememberSaveable { mutableIntStateOf(0) }
    var savedRootFirstVisibleOffset by rememberSaveable { mutableIntStateOf(0) }
    var hasRestoredRootScroll by rememberSaveable { mutableStateOf(false) }
    val rootGridState = rememberLazyGridState(
        initialFirstVisibleItemIndex = savedRootFirstVisibleIndex,
        initialFirstVisibleItemScrollOffset = savedRootFirstVisibleOffset
    )
    val chatRepository = remember { MockDataStore.getRepository() }
    val luluRepository = remember { runCatching { LuluRepository.get() }.getOrNull() }

    var isRefreshing by remember { mutableStateOf(false) }
    suspend fun runWishlistRefresh() {
        if (isRefreshing) return
        val uid = currentUser.id
        if (uid.isBlank()) {
            delay(400)
            return
        }
        isRefreshing = true
        try {
            luluRepository?.refreshWishlistTabData(uid)
            MockDataStore.reloadServicesFromDatabase()
        } catch (_: Exception) {
        } finally {
            delay(400)
            isRefreshing = false
        }
    }
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { scope.launch { runWishlistRefresh() } },
        refreshThreshold = PullRefreshTokens.triggerThreshold,
    )

    val wishedServices = services.filter { favoriteServiceIds.contains(it.id) && !it.isDeleted }
    val groupedWishlists = wishedServices
        .groupBy { service -> MockDataStore.getFavoriteGroupForService(service.id) }
        .map { (name, list) ->
            WishlistGroup(
                key = name + list.firstOrNull()?.id.orEmpty(),
                name = name,
                services = list
            )
        }
        .sortedByDescending { it.services.size }
    val expandedGroup = groupedWishlists.firstOrNull { it.name == expandedWishlistGroupName }
    val expandedServices = expandedGroup?.services.orEmpty()

    LaunchedEffect(expandedGridState, expandedWishlistGroupName) {
        if (expandedWishlistGroupName == null) return@LaunchedEffect
        snapshotFlow {
            expandedGridState.firstVisibleItemIndex to expandedGridState.firstVisibleItemScrollOffset
        }
            .distinctUntilChanged()
            .collect { (index, offset) ->
                savedExpandedFirstVisibleIndex = index
                savedExpandedFirstVisibleOffset = offset
            }
    }

    LaunchedEffect(rootGridState, expandedWishlistGroupName) {
        if (expandedWishlistGroupName != null) return@LaunchedEffect
        snapshotFlow {
            rootGridState.firstVisibleItemIndex to rootGridState.firstVisibleItemScrollOffset
        }
            .distinctUntilChanged()
            .collect { (index, offset) ->
                savedRootFirstVisibleIndex = index
                savedRootFirstVisibleOffset = offset
            }
    }

    LaunchedEffect(expandedWishlistGroupName) {
        if (expandedWishlistGroupName == null) {
            hasRestoredRootScroll = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            hasRestoredExpandedScroll = false
            hasRestoredRootScroll = false
        }
    }

    LaunchedEffect(expandedWishlistGroupName, expandedServices, hasRestoredExpandedScroll) {
        if (expandedWishlistGroupName == null || hasRestoredExpandedScroll || expandedServices.isEmpty()) {
            return@LaunchedEffect
        }
        val targetIndex = savedExpandedFirstVisibleIndex.coerceIn(0, expandedServices.lastIndex)
        runCatching {
            expandedGridState.scrollToItem(
                targetIndex,
                savedExpandedFirstVisibleOffset.coerceAtLeast(0)
            )
        }
        hasRestoredExpandedScroll = true
    }

    LaunchedEffect(groupedWishlists, expandedWishlistGroupName, hasRestoredRootScroll) {
        if (expandedWishlistGroupName != null || hasRestoredRootScroll || groupedWishlists.isEmpty()) {
            return@LaunchedEffect
        }
        val targetIndex = savedRootFirstVisibleIndex.coerceIn(0, groupedWishlists.lastIndex)
        runCatching {
            rootGridState.scrollToItem(
                targetIndex,
                savedRootFirstVisibleOffset.coerceAtLeast(0)
            )
        }
        hasRestoredRootScroll = true
    }

    // 分组内列表无独立路由，系统返回否则会冒泡到外层 Nav 并退出应用；先回到分组卡片页。
    BackHandler(enabled = expandedWishlistGroupName != null && !showGroupEditor) {
        expandedWishlistGroupName = null
    }

    if (!view.isInEditMode) {
        DisposableEffect(Unit) {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            val previousStatusBarColor = window.statusBarColor
            val previousLightStatusBars = insetsController.isAppearanceLightStatusBars
            onDispose {
                window.statusBarColor = previousStatusBarColor
                insetsController.isAppearanceLightStatusBars = previousLightStatusBars
            }
        }
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            window.statusBarColor = pageColor.toArgb()
            insetsController.isAppearanceLightStatusBars = !isDarkTheme
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pageColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(pageColor)
                .statusBarsPadding()
                .padding(top = 4.dp, start = 24.dp, end = 24.dp, bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.End
            ) {
                WishlistHeaderActionButton(
                    icon = Icons.Outlined.Search,
                    contentDescription = "搜索",
                    onClick = { navController.navigate(Screen.Search.route) }
                )
                Spacer(modifier = Modifier.size(10.dp))
                WishlistHeaderActionButton(
                    icon = Icons.Outlined.Settings,
                    contentDescription = "分组设置",
                    onClick = { showGroupEditor = true }
                )
            }
            Text(
                text = "心愿单",
                fontSize = 24.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.SemiBold,
                color = primaryText,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pullRefresh(pullRefreshState)
        ) {
            when {
                groupedWishlists.isEmpty() -> {
                    EmptyWishlistContent(
                        primaryText = primaryText,
                        secondaryText = secondaryText
                    )
                }
                expandedGroup != null -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 6.dp)
                                .clickable { expandedWishlistGroupName = null },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "返回分组",
                                tint = secondaryText
                            )
                            Text(
                                text = expandedGroup.name,
                                color = primaryText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "${expandedServices.size}个",
                                color = secondaryText,
                                fontSize = 13.sp
                            )
                        }
                        LazyVerticalStaggeredGrid(
                            columns = StaggeredGridCells.Fixed(1),
                            state = expandedGridState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            verticalItemSpacing = 14.dp
                        ) {
                            items(expandedServices, key = { it.id }) { service ->
                                WishlistExpandedServiceCard(
                                    service = service,
                                    cardBackground = cardBackground,
                                    primaryText = primaryText,
                                    secondaryText = secondaryText,
                                    onClick = {
                                        serviceDetailNav.navigate(Screen.ServiceDetail.createRoute(service.id, from = "wishlist"))
                                    },
                                    onChatClick = {
                                        val targetUserId = service.creatorId.ifBlank { return@WishlistExpandedServiceCard }
                                        if (currentUser.id.isBlank()) {
                                            navController.navigate(Screen.Login.createRoute()) { launchSingleTop = true }
                                            return@WishlistExpandedServiceCard
                                        }
                                        if (chatRepository == null) {
                                            Toast.makeText(context, "聊天服务未就绪，请稍后重试", Toast.LENGTH_SHORT).show()
                                            return@WishlistExpandedServiceCard
                                        }
                                        val isSelf = targetUserId == currentUser.id
                                        if (isSelf) {
                                            Toast.makeText(context, "这是你自己发布的服务", Toast.LENGTH_SHORT).show()
                                            return@WishlistExpandedServiceCard
                                        }
                                        scope.launch {
                                            val conversation = chatRepository.getOrCreateDirectConversation(targetUserId)
                                            if (conversation != null) {
                                                navController.navigate(
                                                    Screen.MessageThread.createRoute(
                                                        threadId = conversation.id,
                                                        peerUserId = targetUserId
                                                    )
                                                )
                                            } else {
                                                Toast.makeText(context, "打开聊天失败，请稍后重试", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        state = rootGridState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(groupedWishlists, key = { it.key }) { group ->
                            WishlistGroupCard(
                                group = group,
                                cardBackground = cardBackground,
                                primaryText = primaryText,
                                secondaryText = secondaryText,
                                onClick = {
                                    val service = group.services.firstOrNull() ?: return@WishlistGroupCard
                                    if (group.services.size == 1) {
                                        serviceDetailNav.navigate(Screen.ServiceDetail.createRoute(service.id, from = "wishlist"))
                                    } else {
                                        expandedWishlistGroupName = group.name
                                    }
                                }
                            )
                        }
                    }
                }
            }
            FeiLingTopSyncIndicator(
                visible = isRefreshing,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
                    .zIndex(50f)
            )
            val pullProgress = pullRefreshState.progress
            val showPullHint = !isRefreshing && pullProgress > 0f
            FeiLingPullRefreshHintIndicator(
                progress = pullProgress,
                visible = showPullHint,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
                    .zIndex(if (showPullHint) 100f else -1f)
            )
        }
    }

    if (showGroupEditor) {
        WishlistGroupEditorDialog(
            groups = wishlistGroups,
            favoriteServiceGroups = favoriteServiceGroups,
            allServices = services,
            favoriteServiceIds = favoriteServiceIds,
            onDismiss = { showGroupEditor = false }
        )
    }
}

@Composable
private fun WishlistExpandedServiceCard(
    service: Service,
    cardBackground: Color,
    primaryText: Color,
    secondaryText: Color,
    onClick: () -> Unit,
    onChatClick: () -> Unit
) {
    /** 与首页体验 Tab 封面一致；图片区独立 Surface，与下方文案 sibling，无衔接灰三角问题 */
    val coverShape = RoundedCornerShape(14.dp)
    val creator = remember(service.creatorId) { MockDataStore.getUserById(service.creatorId) }
    val imageUrls = remember(service.coverImageUrl, service.imageUrls) {
        buildList {
            service.coverImageUrl.takeIf { it.isNotBlank() }?.let(::add)
            service.imageUrls.filter { it.isNotBlank() }.forEach { url ->
                if (url !in this) add(url)
            }
        }
    }
    val creatorName = creator?.remarkName?.ifBlank { creator.name }
        ?.ifBlank { service.creator.ifBlank { "发布者" } }
        ?: service.creator.ifBlank { "发布者" }
    val imageAspect = remember(service.id) {
        val seed = service.id.hashCode().absoluteValue
        when (seed % 4) {
            0 -> 1.24f
            1 -> 1.12f
            2 -> 1.36f
            else -> 1.18f
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = coverShape,
            color = cardBackground,
            shadowElevation = 1.dp,
            tonalElevation = 0.dp,
            border = BorderStroke(0.5.dp, secondaryText.copy(alpha = 0.28f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(imageAspect)
                    .background(cardBackground)
            ) {
                if (imageUrls.isNotEmpty()) {
                    val pagerState = rememberPagerState(pageCount = { imageUrls.size })
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        AsyncImage(
                            model = imageUrls[page],
                            contentDescription = service.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    if (imageUrls.size > 1) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            repeat(imageUrls.size.coerceAtMost(6)) { index ->
                                val selected = index == pagerState.currentPage
                                Box(
                                    modifier = Modifier
                                        .size(if (selected) 7.dp else 6.dp)
                                        .clip(CircleShape)
                                        .background(if (selected) Color(0xFFFF3B30) else Color.White.copy(alpha = 0.75f))
                                )
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "暂无图片", color = Color(0xFF9A9A9A), fontSize = 13.sp)
                    }
                }
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 12.dp, top = 12.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.95f))
                        .clickable(onClick = onChatClick)
                        .padding(start = 6.dp, end = 10.dp, top = 5.dp, bottom = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!creator?.photoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = RetrofitClient.normalizeBackendMediaUrlForDisplay(creator!!.photoUrl),
                            contentDescription = "发布者头像",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFECECEC)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "发布者头像",
                                tint = Color(0xFF7A7A7A),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = "聊一聊",
                        color = Color(0xFF1B1B1B),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = Color(0xFFFF4D5E),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(24.dp)
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBackground)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = service.title.ifBlank { "未命名服务" },
                color = primaryText,
                fontSize = 17.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = listOf(
                    service.category,
                    com.example.Lulu.util.ServiceLocationPolygonCodec.displayLine(service.location)
                ).filter { it.isNotBlank() }.joinToString(" | ")
                    .ifBlank { creatorName },
                color = secondaryText,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (service.priceText.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = service.priceText,
                    color = secondaryText,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EmptyWishlistContent(
    primaryText: Color,
    secondaryText: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 140.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.FavoriteBorder,
            contentDescription = null,
            tint = Color(0xFFFF5A6B),
            modifier = Modifier.size(38.dp)
        )
        Text(
            text = "心愿单还是空的",
            color = primaryText,
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 26.dp)
        )
        Text(
            text = "去点红心添加吧",
            color = secondaryText,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun WishlistHeaderActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color(0xFFF0F0F0))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color(0xFF242424),
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun WishlistGroupCard(
    group: WishlistGroup,
    cardBackground: Color,
    primaryText: Color,
    secondaryText: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        if (group.services.size >= 4) {
            CollageImageCard(
                imageUrls = group.services.map { it.coverImageUrl },
                cardBackground = cardBackground
            )
        } else {
            SingleImageCard(
                imageUrl = group.services.firstOrNull()?.coverImageUrl.orEmpty(),
                groupName = group.name,
                cardBackground = cardBackground
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = group.name,
            color = primaryText,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "${group.services.size}个心愿单项目",
            color = secondaryText,
            fontSize = 15.sp
        )
    }
}

@Composable
private fun SingleImageCard(
    imageUrl: String,
    groupName: String,
    cardBackground: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(24.dp))
            .background(cardBackground)
    ) {
        if (imageUrl.isNotBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = groupName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "暂无图片", color = Color(0xFF9A9A9A), fontSize = 12.sp)
            }
        }
        Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = null,
            tint = Color(0xFFFF4D5E),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .size(24.dp)
        )
    }
}

@Composable
private fun CollageImageCard(
    imageUrls: List<String>,
    cardBackground: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(24.dp))
            .background(cardBackground)
            .padding(2.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            val leftItems = imageUrls.take(4)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    GridImage(leftItems.getOrNull(0))
                    GridImage(leftItems.getOrNull(1))
                }
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    GridImage(leftItems.getOrNull(2))
                    GridImage(leftItems.getOrNull(3))
                }
            }
        }
        Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = null,
            tint = Color(0xFFFF4D5E),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .size(24.dp)
        )
    }
}

@Composable
private fun RowScope.GridImage(url: String?) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxSize()
    ) {
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFE8E8E8))
            )
        }
    }
}

@Composable
private fun WishlistGroupEditorDialog(
    groups: List<String>,
    favoriteServiceGroups: Map<String, String>,
    allServices: List<Service>,
    favoriteServiceIds: Set<String>,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var newGroupName by remember { mutableStateOf("") }
    var renameFrom by remember { mutableStateOf<String?>(null) }
    var renameTo by remember { mutableStateOf("") }
    var inlineMessage by remember { mutableStateOf<String?>(null) }
    var bulkGroupName by remember(groups) {
        mutableStateOf(
            groups.firstOrNull { it == MockDataStore.DEFAULT_WISHLIST_GROUP }
                ?: groups.firstOrNull()
                ?: MockDataStore.DEFAULT_WISHLIST_GROUP
        )
    }
    var selectedServiceIds by remember { mutableStateOf(setOf<String>()) }
    val activeServices = remember(allServices) { allServices.filter { !it.isDeleted } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑分组") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                groups.forEach { group ->
                    val groupCount = favoriteServiceGroups.values.count { it == group }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$group（$groupCount）",
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    if (group != MockDataStore.DEFAULT_WISHLIST_GROUP) {
                                        renameFrom = group
                                        renameTo = group
                                    }
                                }
                        )
                        if (group != MockDataStore.DEFAULT_WISHLIST_GROUP) {
                            IconButton(onClick = {
                                val ok = MockDataStore.deleteWishlistGroup(group)
                                inlineMessage = if (ok) "已删除分组：$group" else "删除失败"
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "删除分组")
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = newGroupName,
                    onValueChange = {
                        newGroupName = it
                        inlineMessage = null
                    },
                    label = { Text("新增分组") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        val ok = MockDataStore.createWishlistGroup(newGroupName)
                        inlineMessage = if (ok) {
                            newGroupName = ""
                            "已新增分组"
                        } else {
                            "分组名为空或已存在"
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("新增")
                }

                if (!renameFrom.isNullOrBlank()) {
                    OutlinedTextField(
                        value = renameTo,
                        onValueChange = {
                            renameTo = it
                            inlineMessage = null
                        },
                        label = { Text("重命名分组") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            val source = renameFrom ?: return@Button
                            val ok = MockDataStore.renameWishlistGroup(source, renameTo)
                            inlineMessage = if (ok) {
                                renameFrom = null
                                renameTo = ""
                                "已重命名分组"
                            } else {
                                "重命名失败（可能重名）"
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("确认重命名")
                    }
                }

                Text(
                    text = "批量加入同一分组",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedTextField(
                    value = bulkGroupName,
                    onValueChange = {
                        bulkGroupName = it
                        inlineMessage = null
                    },
                    label = { Text("目标分组") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    activeServices.forEach { service ->
                        val checked = selectedServiceIds.contains(service.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedServiceIds = if (checked) {
                                        selectedServiceIds - service.id
                                    } else {
                                        selectedServiceIds + service.id
                                    }
                                }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { isChecked ->
                                    selectedServiceIds = if (isChecked) {
                                        selectedServiceIds + service.id
                                    } else {
                                        selectedServiceIds - service.id
                                    }
                                }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = service.title.ifBlank { "未命名服务" },
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (favoriteServiceIds.contains(service.id)) "已在心愿单" else "未加入心愿单",
                                    fontSize = 11.sp,
                                    color = Color(0xFF8A8A8A)
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { selectedServiceIds = activeServices.map { it.id }.toSet() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("全选")
                    }
                    OutlinedButton(
                        onClick = { selectedServiceIds = emptySet() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("清空")
                    }
                }
                Button(
                    onClick = {
                        val picked = selectedServiceIds.toList()
                        if (picked.isEmpty()) {
                            inlineMessage = "请先选择服务卡片"
                            return@Button
                        }
                        val targetGroup = bulkGroupName.trim()
                        if (targetGroup.isBlank()) {
                            inlineMessage = "请先输入目标分组"
                            return@Button
                        }
                        MockDataStore.createWishlistGroup(targetGroup)
                        scope.launch {
                            picked.forEach { serviceId ->
                                MockDataStore.addFavoriteServiceToGroup(serviceId, targetGroup)
                            }
                            inlineMessage = "已将${picked.size}个服务加入分组：$targetGroup"
                            selectedServiceIds = emptySet()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("批量加入分组")
                }

                inlineMessage?.let { Text(text = it) }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("完成")
            }
        }
    )
}
