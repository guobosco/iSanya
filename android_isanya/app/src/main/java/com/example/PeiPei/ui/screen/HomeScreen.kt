// 文件说明：应用首页（推荐、入口 Tab、主要信息流等）。

package com.example.Lulu.ui.screen

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Size
import com.example.Lulu.R
import com.example.Lulu.data.local.AppDataStore
import com.example.Lulu.data.model.Service
import com.example.Lulu.data.model.ServiceCategories
import com.example.Lulu.data.repository.LuluRepository
import com.example.Lulu.service.PermissionService
import com.example.Lulu.ui.components.FeiLingPullRefreshHintIndicator
import com.example.Lulu.ui.components.FeiLingTopSyncIndicator
import com.example.Lulu.ui.navigation.Screen
import com.example.Lulu.ui.theme.*
import com.example.Lulu.ui.util.PullRefreshTokens
import com.example.Lulu.ui.util.matchAdminLocation
import com.example.Lulu.ui.util.resolveWishlistCategoryGroupName
import com.example.Lulu.util.NetworkMonitor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.random.Random
import retrofit2.HttpException

private fun compactDisplayFromAdminMatch(m: MatchResult): String =
    m.groupValues.drop(1).filter { it.isNotBlank() }.joinToString("·") { abbrevChineseAdminSegment(it) }

private val HomeWishlistHeartColor = Color(0xFFFF5A5F)

private val EthnicGroupSuffix = Regex("""[\u4E00-\u9FFF]{1,10}族$""")

private fun peelTrailingEthnicGroups(s: String): String {
    var t = s
    while (t.isNotEmpty()) {
        val next = t.replaceFirst(EthnicGroupSuffix, "")
        if (next == t) break
        t = next
    }
    return t
}

private fun abbrevAutonomousPrefectureName(s: String): String {
    if (!s.endsWith("自治州")) return s
    val core = peelTrailingEthnicGroups(s.removeSuffix("自治州"))
    return core.ifBlank { s.removeSuffix("自治州") }
}

private fun isTaiwanLocationText(s: String): Boolean =
    s.contains("台湾") || s.contains("臺灣") || s.contains("台灣")

/** 台湾地区：卡片上固定为「台湾省xx市／xx县…」，不使用「·」缩写。 */
private val TaiwanLeadingPrefixStrip = Regex("""^中国?(台灣省|臺灣省|台湾省|台湾|臺灣|台灣)""")

private fun formatTaiwanProvinceStyleDisplay(raw: String): String? {
    val source = raw.trim()
    if (!isTaiwanLocationText(source)) return null
    val rest = source.replaceFirst(TaiwanLeadingPrefixStrip, "").trim()
    return if (rest.isEmpty()) "台湾省" else "台湾省$rest"
}

private fun formatLocationLabelCompact(raw: String): String {
    val source = raw.trim()
    if (source.isBlank()) return ""
    formatTaiwanProvinceStyleDisplay(source)?.let { return it }
    matchAdminLocation(source)?.let { return compactDisplayFromAdminMatch(it) }
    if ('·' in source) return source

    listOf("北京", "上海", "天津", "重庆").forEach { name ->
        if (source.startsWith(name)) return name
    }

    return abbrevChineseAdminSegment(source).ifBlank { source }
}

private fun abbrevChineseAdminSegment(segment: String): String {
    val s = segment.trim()
    if (s.isEmpty()) return s
    return when {
        s.endsWith("特别行政区") -> s.removeSuffix("特别行政区")
        s.endsWith("自治州") -> abbrevAutonomousPrefectureName(s)
        s.endsWith("自治区") -> when {
            s.startsWith("内蒙古") -> "内蒙古"
            s.startsWith("广西") -> "广西"
            s.startsWith("西藏") -> "西藏"
            s.startsWith("宁夏") -> "宁夏"
            s.startsWith("新疆") -> "新疆"
            else -> s.removeSuffix("自治区")
        }
        s.endsWith("盟") -> s.removeSuffix("盟")
        s.endsWith("林区") -> s.removeSuffix("林区")
        s.endsWith("特区") -> s.removeSuffix("特区")
        s.endsWith("自治县") -> peelTrailingEthnicGroups(s.removeSuffix("自治县"))
        s.endsWith("自治旗") -> s.removeSuffix("自治旗")
        s.endsWith("县") -> s.removeSuffix("县")
        s.endsWith("区") -> s.removeSuffix("区")
        s.endsWith("旗") -> s.removeSuffix("旗")
        s.endsWith("省") -> s.removeSuffix("省")
        s.endsWith("市") -> s.removeSuffix("市")
        else -> s
    }
}

// 模拟数据模型 (UI 层)：瀑布流卡片与操作弹窗所需字段
@Immutable
data class ServiceEvent(
    val id: String,
    val title: String,
    val creatorId: String,
    val coverImageUri: String? = null,
    val publishCity: String = "",
    val priceText: String = "",
    val category: String = "",
)

private fun extractCityLevelLocation(rawLocation: String, fallbackRegion: String): String {
    val delimiters = charArrayOf('/', '|', '·', ',', '，', ' ')
    val candidates = listOf(rawLocation, fallbackRegion)
    for (source in candidates) {
        if (source.isBlank()) continue
        val trimmed = source.trim()
        matchAdminLocation(trimmed)?.let { return it.value }
        val cityLike = Regex("""([\u4E00-\u9FFF]{2,}市)""").findAll(trimmed).lastOrNull()?.groupValues?.get(1)
        if (!cityLike.isNullOrBlank()) return cityLike
        if (source.contains("北京")) return "北京市"
        if (source.contains("上海")) return "上海市"
        if (source.contains("天津")) return "天津市"
        if (source.contains("重庆")) return "重庆市"
        val firstPart = source
            .split(*delimiters)
            .map { it.trim() }
            .firstOrNull { it.isNotBlank() }
        if (!firstPart.isNullOrBlank()) return firstPart
    }
    return ""
}

@Immutable
data class HomeFeedCardItem(
    val event: ServiceEvent,
    val imageModel: Any?,
    val imageHeight: Dp,
    val infoText: String,
    val footerText: String
)

private val homeFeedImagePool = listOf(R.drawable.ic_logo)

private val homeFeedImageHeights = listOf(168.dp, 192.dp, 216.dp, 238.dp)

private fun resolveCoverImageModel(
    coverImageUri: String?,
    resources: Resources,
    appPackageName: String
): Any? {
    val rawUri = coverImageUri?.trim().orEmpty()
    // Keep encoded android_asset URI as-is. Decoding (%20 -> space, etc.) may break
    // asset file lookup for names containing special characters.
    val uri = rawUri
    if (uri.isBlank()) return null
    if (uri.startsWith("android.resource://")) {
        val rest = uri.removePrefix("android.resource://")
        val parts = rest.split('/').filter { it.isNotBlank() }
        if (parts.size >= 3) {
            val pkg = parts[0]
            val resType = parts[1]
            val name = parts[2]
            if (resType == "drawable" && name.isNotBlank()) {
                val pkgCandidates = listOf(pkg, appPackageName).distinct()
                for (candidate in pkgCandidates) {
                    val id = resources.getIdentifier(name, "drawable", candidate)
                    if (id != 0) return id
                }
            }
        }
        val name = uri.substringAfterLast("/")
        if (name.isNotBlank()) {
            val pkgFromUri = rest.substringBefore('/')
            val pkgCandidates = listOf(pkgFromUri, appPackageName).filter { it.isNotBlank() }.distinct()
            for (candidate in pkgCandidates) {
                val id = resources.getIdentifier(name, "drawable", candidate)
                if (id != 0) return id
            }
        }
        // android.resource 未能解析时返回 null，让上层走默认兜底图。
        return null
    }
    return uri
}

private fun ServiceEvent.toHomeFeedCardItem(resources: Resources): HomeFeedCardItem {
    val safeIndex = id.hashCode().absoluteValue
    val categoryLabel = ServiceCategories.normalize(category)
    val locationLabel = formatLocationLabelCompact(publishCity).ifBlank { "发布地未知" }
    val infoText = listOf(categoryLabel, locationLabel).filter { it.isNotBlank() }.joinToString(" · ")
        .ifBlank { "发布地未知" }
    val footerText = priceText.ifBlank { "价格待沟通" }
    return HomeFeedCardItem(
        event = this,
        imageModel = resolveCoverImageModel(
            coverImageUri = coverImageUri,
            resources = resources,
            appPackageName = resources.getResourcePackageName(R.string.app_name)
        ) ?: homeFeedImagePool[safeIndex % homeFeedImagePool.size],
        imageHeight = homeFeedImageHeights[safeIndex % homeFeedImageHeights.size],
        infoText = infoText,
        footerText = footerText
    )
}

enum class HomeDiscoverTab(val label: String) {
    Discover("服务"),
    Nearby("体验")
}
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, androidx.compose.material.ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    mainShellNavController: NavController? = null,
    homeTabReselectTrigger: Int = 0
) {
    // 观察模拟数据
    val currentUser by AppDataStore.currentUser.collectAsState()
    val allServices by AppDataStore.services.collectAsState()
    val favoriteServiceIds by AppDataStore.favoriteServiceIds.collectAsState()

    val context = LocalContext.current
    val resources = context.resources
    val isNetworkConnected by NetworkMonitor.observeNetworkStatus(context).collectAsState(initial = true)

    
    // Theme state
    val isDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    val backgroundColor = if (isDarkTheme) Color.Black else Color.White

    // Pull to Refresh State
    val repository = remember { LuluRepository.get() }
    val squareDiscoveryServices by repository.squareDiscoveryServices.collectAsState(initial = emptyList())
    val roomUsers by repository.allUsers.collectAsState(initial = emptyList())
    val chatRepository = remember { AppDataStore.getRepository() }
    val scope = rememberCoroutineScope()
    /** 本人 Room 全量 + 广场发现流；含 `seed-home-` 种子以便卡片进详情能解析 */
    val serviceByIdForHome = remember(squareDiscoveryServices, allServices) {
        buildMap<String, Service> {
            allServices.forEach { put(it.id, it) }
            squareDiscoveryServices.forEach { put(it.id, it) }
        }
    }
    val serviceDetailNav = mainShellNavController ?: navController
    val currentUserId = currentUser.id.ifEmpty { repository.currentUserId }
    var isRefreshing by remember { mutableStateOf(false) }
    var showRefreshIndicator by remember { mutableStateOf(false) }
    var manualRefreshNonce by rememberSaveable { mutableIntStateOf(0) }
    var manualShuffleSeed by rememberSaveable { mutableIntStateOf(0) }
    var previousFirstPageIds by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var currentFeedOrderIds by remember { mutableStateOf<List<String>>(emptyList()) }
    val autoRefreshDoneByUser = rememberSaveable(
        saver = androidx.compose.runtime.saveable.mapSaver(
            save = { state ->
                state.entries.associate { (k, v) -> k to v }
            },
            restore = { restored ->
                mutableStateMapOf<String, Boolean>().apply {
                    restored.forEach { (key, value) ->
                        this[key] = value as? Boolean == true
                    }
                }
            }
        )
    ) {
        mutableStateMapOf<String, Boolean>()
    }
    var togglingFavoriteIds by remember { mutableStateOf(setOf<String>()) }
    var selectedTab by rememberSaveable { mutableStateOf(HomeDiscoverTab.Discover) }
    var selectedCategoryFilter by rememberSaveable { mutableStateOf(ServiceCategories.FILTER_ALL) }

    val initialFeedPageSize = 20
    val feedPageSize = 10
    var visibleFeedCount by rememberSaveable(selectedTab, currentUserId, selectedCategoryFilter) {
        mutableStateOf(initialFeedPageSize)
    }
    // 滚动位置由 rememberLazyStaggeredGridState 内置 Saver 与导航保存状态恢复；勿再并行 scrollToItem，否则会覆盖正确偏移。
    val feedGridState = rememberLazyStaggeredGridState()
    val experienceVerticalListState = rememberLazyListState()
    var experienceRefreshNonce by rememberSaveable { mutableIntStateOf(0) }

    suspend fun refreshHomeData(
        showIndicator: Boolean,
        rotateFeedOrder: Boolean = false
    ) {
        if (selectedTab == HomeDiscoverTab.Nearby) {
            return
        }
        if (isRefreshing) {
            return
        }
        if (currentUserId.isEmpty()) {
            return
        }
        showRefreshIndicator = showIndicator
        isRefreshing = true
        try {
            // 只刷新首页发现流，不同步当前用户、心愿单；冷启动等仍用 refreshHomeCriticalData（见 Splash）
            repository.refreshHomeDiscoveryFeedOnly(currentUserId)
            if (rotateFeedOrder) {
                previousFirstPageIds = currentFeedOrderIds.take(initialFeedPageSize)
                manualShuffleSeed = Random.nextInt()
                manualRefreshNonce++
            }
            visibleFeedCount = initialFeedPageSize
        } catch (_: HttpException) {
            // 刷新时仅保留动画提示，不弹文字提示
        } catch (_: Exception) {
            // 刷新时仅保留动画提示，不弹文字提示
        } finally {
            kotlinx.coroutines.delay(120)
            isRefreshing = false
            showRefreshIndicator = false
        }
    }

    suspend fun refreshExperienceTabData(showIndicator: Boolean) {
        if (isRefreshing) return
        showRefreshIndicator = showIndicator
        isRefreshing = true
        try {
            experienceRefreshNonce++
            runCatching { experienceVerticalListState.scrollToItem(0) }
        } finally {
            kotlinx.coroutines.delay(120)
            isRefreshing = false
            showRefreshIndicator = false
        }
    }

    fun scrollToTopAndRefresh() {
        scope.launch {
            when (selectedTab) {
                HomeDiscoverTab.Discover -> {
                    runCatching { feedGridState.animateScrollToItem(0) }
                    refreshHomeData(
                        showIndicator = true,
                        rotateFeedOrder = true
                    )
                }
                HomeDiscoverTab.Nearby -> {
                    runCatching { experienceVerticalListState.animateScrollToItem(0) }
                    refreshExperienceTabData(showIndicator = true)
                }
            }
        }
    }
    
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                when (selectedTab) {
                    HomeDiscoverTab.Discover -> refreshHomeData(
                        showIndicator = true,
                        rotateFeedOrder = true
                    )
                    HomeDiscoverTab.Nearby -> refreshExperienceTabData(showIndicator = true)
                }
            }
        },
        refreshThreshold = PullRefreshTokens.triggerThreshold,
    )

    // 冷启动补拉：只按 userId / 网络变化跑一次；用 Flow.first() 取 Room 首帧，避免
    // collectAsState(initial=emptyList) 在从详情返回时短暂为空而误触发整页 refresh。
    LaunchedEffect(currentUserId, isNetworkConnected) {
        if (currentUserId.isEmpty() || !isNetworkConnected) return@LaunchedEffect
        if (autoRefreshDoneByUser[currentUserId] == true) return@LaunchedEffect

        val discoverySnapshot = repository.squareDiscoveryServices.first()
        if (discoverySnapshot.isNotEmpty()) {
            autoRefreshDoneByUser[currentUserId] = true
            return@LaunchedEffect
        }
        refreshHomeData(
            showIndicator = false,
            rotateFeedOrder = false
        )
        // 仅在有数据时标记「已补拉」，避免首次请求失败（或误判联网）后网络恢复也不再重试。
        if (repository.squareDiscoveryServices.first().isNotEmpty()) {
            autoRefreshDoneByUser[currentUserId] = true
        }
    }

    // Application 异步写入用户时，Splash 可能早于 Room 用户完成；进入首页后再补写种子并刷新列表。
    LaunchedEffect(currentUserId) {
        if (currentUserId.isEmpty()) return@LaunchedEffect
        runCatching {
            AppDataStore.reloadServicesFromDatabase()
        }
    }

    // 底部 Tab 连点「首页」会递增 homeTabReselectTrigger；从详情返回时 Home 会重新进入组合，
    // LaunchedEffect 会再跑一遍。若仅判断 trigger > 0，会把「上次已处理过的值」误判为新一次连点并整页刷新。
    var lastHandledHomeTabReselect by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(homeTabReselectTrigger) {
        if (homeTabReselectTrigger > lastHandledHomeTabReselect) {
            lastHandledHomeTabReselect = homeTabReselectTrigger
            scrollToTopAndRefresh()
        }
    }

    // 长按操作对话框
    var showActionDialog by remember { mutableStateOf(false) }
    var selectedEventForAction by remember { mutableStateOf<ServiceEvent?>(null) }
    if (showActionDialog && selectedEventForAction != null) {
        val event = selectedEventForAction!!
        AlertDialog(
            onDismissRequest = { showActionDialog = false },
            title = { Text(event.title) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick = {
                            showActionDialog = false
                            scope.launch {
                                if (favoriteServiceIds.contains(event.id)) {
                                    Toast.makeText(context, "已在心愿单中", Toast.LENGTH_SHORT).show()
                                } else {
                                    scope.launch {
                                        if (togglingFavoriteIds.contains(event.id)) return@launch
                                        togglingFavoriteIds = togglingFavoriteIds + event.id
                                        try {
                                            val group =
                                                resolveWishlistCategoryGroupName(event.category)
                                            val result =
                                                AppDataStore.addFavoriteServiceToGroup(event.id, group)
                                            if (result.syncFailed) {
                                                Toast.makeText(
                                                    context,
                                                    "加入心愿单失败，请重试",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else if (result.favoriteIds.contains(event.id)) {
                                                Toast.makeText(context, "已加入心愿单", Toast.LENGTH_SHORT)
                                                    .show()
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "加入心愿单失败，请重试",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        } finally {
                                            togglingFavoriteIds = togglingFavoriteIds - event.id
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(
                            Icons.Filled.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = BrandPink
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("加心愿单", color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    TextButton(
                        onClick = {
                            showActionDialog = false
                            scope.launch {
                                val creator = AppDataStore.getUserById(event.creatorId)
                                if (creator == null) {
                                    Toast.makeText(context, "暂未找到发布者信息", Toast.LENGTH_SHORT).show()
                                    return@launch
                                }
                                if (chatRepository == null) {
                                    Toast.makeText(context, "聊天服务未就绪，请稍后重试", Toast.LENGTH_SHORT).show()
                                    return@launch
                                }
                                val conversation = chatRepository.createDirectConversation(creator.id)
                                if (conversation != null) {
                                    navController.navigate(
                                        Screen.MessageThread.createRoute(
                                            threadId = conversation.id,
                                            peerUserId = creator.id
                                        )
                                    )
                                } else {
                                    Toast.makeText(context, "打开聊天失败，请稍后重试", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(
                            Icons.Filled.ChatBubbleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("去聊一聊", color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    TextButton(
                        onClick = {
                            showActionDialog = false
                            serviceDetailNav.navigate(Screen.ServiceDetail.createRoute(event.id))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(
                            Icons.Filled.EventAvailable,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = BrandPink
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("现在预定", color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showActionDialog = false }) {
                    Text(stringResource(R.string.cancel), color = Color.Gray)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    val feedItems = remember(
        squareDiscoveryServices,
        roomUsers,
        resources,
        manualRefreshNonce,
        manualShuffleSeed,
        previousFirstPageIds
    ) {
        val sortedServices = squareDiscoveryServices.sortedBy { it.id }
        val regionByUserId = roomUsers.associate { it.id to it.region.orEmpty() }
        val creatorRegionById = sortedServices
            .asSequence()
            .map { it.creatorId }
            .distinct()
            .associateWith { creatorId ->
                regionByUserId[creatorId].orEmpty().ifBlank {
                    AppDataStore.getUserById(creatorId)?.region.orEmpty()
                }
            }
        val baseSeed = sortedServices.map { it.id }.joinToString().hashCode()
        val minDifferentCount = 4
        val firstPageSize = initialFeedPageSize

        fun buildListWithSeed(seed: Int): List<HomeFeedCardItem> =
            sortedServices
                .shuffled(Random(seed))
                .map { svc ->
                    val region = creatorRegionById[svc.creatorId].orEmpty()
                    svc.toServiceEvent(creatorRegionFallback = region).toHomeFeedCardItem(resources)
                }

        val startSeed = if (manualShuffleSeed != 0) manualShuffleSeed else baseSeed
        var candidate = buildListWithSeed(startSeed)

        if (previousFirstPageIds.isNotEmpty() && candidate.isNotEmpty()) {
            val previousSet = previousFirstPageIds.toSet()
            var attempt = 0
            while (attempt < 8) {
                val firstPageIds = candidate.take(firstPageSize).map { it.event.id }
                val overlapCount = firstPageIds.count { it in previousSet }
                val differentCount = firstPageIds.size - overlapCount
                if (differentCount >= minDifferentCount) {
                    break
                }
                candidate = buildListWithSeed(startSeed + attempt + 1)
                attempt++
            }
        }
        candidate
    }
    LaunchedEffect(feedItems) {
        currentFeedOrderIds = feedItems.map { it.event.id }
    }
    val displayedFeedItems = remember(feedItems, selectedTab, selectedCategoryFilter) {
        if (selectedTab == HomeDiscoverTab.Nearby) {
            emptyList()
        } else {
            var list = feedItems
            if (selectedCategoryFilter != ServiceCategories.FILTER_ALL) {
                val want = ServiceCategories.normalize(selectedCategoryFilter)
                // event.category 已在 toServiceEvent 中规范化，避免每条重复 normalize
                list = list.filter { it.event.category == want }
            }
            list
        }
    }
    val visibleFeedItems = remember(displayedFeedItems, visibleFeedCount) {
        displayedFeedItems.take(visibleFeedCount.coerceAtMost(displayedFeedItems.size))
    }

    LaunchedEffect(displayedFeedItems) {
        if (displayedFeedItems.isEmpty()) {
            visibleFeedCount = initialFeedPageSize
        } else {
            visibleFeedCount = visibleFeedCount
                .coerceAtMost(displayedFeedItems.size)
                .coerceAtLeast(initialFeedPageSize.coerceAtMost(displayedFeedItems.size))
        }
    }

    val feedContextKey = remember(selectedCategoryFilter, selectedTab) {
        "${selectedCategoryFilter}|${selectedTab}"
    }
    var lastFeedContextKey by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(feedContextKey) {
        val previousKey = lastFeedContextKey
        if (previousKey != null && previousKey != feedContextKey) {
            runCatching { feedGridState.scrollToItem(0) }
        }
        lastFeedContextKey = feedContextKey
    }

    // 从详情返回时 grid state 已恢复 index，但 visibleFeedItems 可能仍较短；补足截取长度避免视口错位。
    LaunchedEffect(displayedFeedItems, feedGridState.firstVisibleItemIndex) {
        if (displayedFeedItems.isEmpty()) return@LaunchedEffect
        val needed = feedGridState.firstVisibleItemIndex + 1
        if (visibleFeedCount < needed) {
            visibleFeedCount = needed.coerceAtMost(displayedFeedItems.size)
        }
    }

    val displayedFeedItemsSize = displayedFeedItems.size
    LaunchedEffect(feedGridState, displayedFeedItemsSize) {
        snapshotFlow {
            val capCount = visibleFeedCount.coerceAtMost(displayedFeedItemsSize).coerceAtLeast(0)
            val lastVisibleIndex =
                feedGridState.layoutInfo.visibleItemsInfo.maxOfOrNull { it.index } ?: -1
            lastVisibleIndex >= capCount - 2 && capCount > 0
        }
            .distinctUntilChanged()
            .filter { it }
            .collect {
                if (visibleFeedCount < displayedFeedItemsSize) {
                    visibleFeedCount =
                        (visibleFeedCount + feedPageSize).coerceAtMost(displayedFeedItemsSize)
                }
            }
    }

    // 读ahead：已展开到当前本地全部卡片且视口接近末尾时，后台预取下一页写入 Room（抖音/小红书类「滑到底再拉」的轻量版）
    LaunchedEffect(feedGridState, displayedFeedItemsSize, visibleFeedCount, currentUserId, isNetworkConnected, selectedTab) {
        if (selectedTab != HomeDiscoverTab.Discover) return@LaunchedEffect
        if (currentUserId.isEmpty() || !isNetworkConnected) return@LaunchedEffect
        snapshotFlow {
            val cap = visibleFeedCount.coerceAtMost(displayedFeedItemsSize).coerceAtLeast(0)
            val lastVisible = feedGridState.layoutInfo.visibleItemsInfo.maxOfOrNull { it.index } ?: -1
            val showingAllLoaded =
                displayedFeedItemsSize > 0 && visibleFeedCount >= displayedFeedItemsSize
            val nearEndOfViewport = lastVisible >= cap - 2 && cap > 5
            showingAllLoaded && nearEndOfViewport
        }
            .distinctUntilChanged()
            .debounce(400)
            .filter { it }
            .collect {
                scope.launch { repository.prefetchDiscoveryFeedNextPage(currentUserId) }
            }
    }

    val latestFavoriteIds by rememberUpdatedState(favoriteServiceIds)
    val latestTogglingFavoriteIds by rememberUpdatedState(togglingFavoriteIds)
    val latestServicesById by rememberUpdatedState(serviceByIdForHome)
    val toggleFavoriteForServiceId = remember(scope) {
        toggleFavorite@{ serviceId: String ->
            if (latestTogglingFavoriteIds.contains(serviceId)) {
                return@toggleFavorite
            }
            if (latestFavoriteIds.contains(serviceId)) {
                scope.launch {
                    togglingFavoriteIds = togglingFavoriteIds + serviceId
                    try {
                        val result = AppDataStore.toggleFavoriteService(serviceId)
                        val toastText = if (result.syncFailed) {
                            "心愿单同步失败，请重试"
                        } else if (result.favoriteIds.contains(serviceId)) {
                            "已加入心愿单"
                        } else {
                            "已移出心愿单"
                        }
                        Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()
                    } finally {
                        togglingFavoriteIds = togglingFavoriteIds - serviceId
                    }
                }
            } else {
                scope.launch {
                    if (latestTogglingFavoriteIds.contains(serviceId)) return@launch
                    togglingFavoriteIds = togglingFavoriteIds + serviceId
                    try {
                        val group = resolveWishlistCategoryGroupName(
                            latestServicesById[serviceId]?.category
                        )
                        val result =
                            AppDataStore.addFavoriteServiceToGroup(serviceId, group)
                        val toastText = if (result.syncFailed) {
                            "加入心愿单失败，请重试"
                        } else if (result.favoriteIds.contains(serviceId)) {
                            "已加入心愿单"
                        } else {
                            "加入心愿单失败，请重试"
                        }
                        Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()
                    } finally {
                        togglingFavoriteIds = togglingFavoriteIds - serviceId
                    }
                }
            }
        }
    }

    // 整体背景设为纯色
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // 主体纯色背景
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 0.dp),
            shape = RectangleShape,
            color = backgroundColor
        ) {
            Scaffold(
                topBar = { 
                    Column {
                        HomeTopBar(
                            navController = navController,
                            selectedTab = selectedTab,
                            nearbyTitle = HomeDiscoverTab.Nearby.label,
                            onTabChange = { tab ->
                                if (tab == HomeDiscoverTab.Discover && selectedTab == HomeDiscoverTab.Discover) {
                                    scrollToTopAndRefresh()
                                } else if (tab == HomeDiscoverTab.Nearby && selectedTab == HomeDiscoverTab.Nearby) {
                                    scrollToTopAndRefresh()
                                } else if (tab == HomeDiscoverTab.Discover) {
                                    selectedTab = tab
                                } else {
                                    selectedTab = HomeDiscoverTab.Nearby
                                }
                            },
                            onTitleRefreshTap = {
                                scrollToTopAndRefresh()
                            },
                        )
                        if (selectedTab == HomeDiscoverTab.Discover) {
                            HomeCategoryFilterChips(
                                selectedLabel = selectedCategoryFilter,
                                onSelectionChange = { selectedCategoryFilter = it },
                                isDarkTheme = isDarkTheme
                            )
                        }
                        if (!isNetworkConnected) {
                            NetworkDisconnectedBanner()
                        }
                    }
                },
                containerColor = Color.Transparent
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .pullRefresh(pullRefreshState)
                ) {
                    // 刷新中且尚无卡片时：仍须占位，否则 isRefreshing 会压住缺省占位，整片内容区空白。
                    when {
                        selectedTab == HomeDiscoverTab.Nearby -> ExperienceTabContent(
                            modifier = Modifier.fillMaxSize(),
                            verticalListState = experienceVerticalListState,
                            refreshNonce = experienceRefreshNonce,
                            onOpenCategoryFeed = { categoryTitle ->
                                serviceDetailNav.navigate(
                                    Screen.ExperienceCategoryFeed.createRoute(categoryTitle)
                                )
                            },
                            onOpenExperienceDetail = { item ->
                                serviceDetailNav.navigate(
                                    Screen.ExperienceDetail.createRoute(item.id)
                                )
                            },
                        )

                        visibleFeedItems.isNotEmpty() -> LazyVerticalStaggeredGrid(
                            columns = StaggeredGridCells.Fixed(2),
                            state = feedGridState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 10.dp,
                                top = 10.dp,
                                end = 10.dp,
                                bottom = 12.dp
                            ),
                            verticalItemSpacing = 10.dp,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(
                                items = visibleFeedItems,
                                key = { it.event.id },
                                contentType = { _ -> "home_feed_card" }
                            ) { item ->
                                HomeFeedCard(
                                    item = item,
                                    navController = serviceDetailNav,
                                    isFavorite = favoriteServiceIds.contains(item.event.id),
                                    isTogglingFavorite = togglingFavoriteIds.contains(item.event.id),
                                    onToggleFavorite = {
                                        if (togglingFavoriteIds.contains(item.event.id)) {
                                            return@HomeFeedCard
                                        }
                                        if (favoriteServiceIds.contains(item.event.id)) {
                                            scope.launch {
                                                togglingFavoriteIds = togglingFavoriteIds + item.event.id
                                                try {
                                                    val result = AppDataStore.toggleFavoriteService(item.event.id)
                                                    val toastText = if (result.syncFailed) {
                                                        "心愿单同步失败，请重试"
                                                    } else if (result.favoriteIds.contains(item.event.id)) {
                                                        "已加入心愿单"
                                                    } else {
                                                        "已移出心愿单"
                                                    }
                                                    Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()
                                                } finally {
                                                    togglingFavoriteIds = togglingFavoriteIds - item.event.id
                                                }
                                            }
                                        } else {
                                            scope.launch {
                                                if (togglingFavoriteIds.contains(item.event.id)) return@launch
                                                togglingFavoriteIds = togglingFavoriteIds + item.event.id
                                                try {
                                                    val group = resolveWishlistCategoryGroupName(
                                                        item.event.category
                                                    )
                                                    val result = AppDataStore.addFavoriteServiceToGroup(
                                                        item.event.id,
                                                        group
                                                    )
                                                    val toastText =
                                                        if (result.syncFailed) {
                                                            "加入心愿单失败，请重试"
                                                        } else if (result.favoriteIds.contains(item.event.id)) {
                                                            "已加入心愿单"
                                                        } else {
                                                            "加入心愿单失败，请重试"
                                                        }
                                                    Toast.makeText(context, toastText, Toast.LENGTH_SHORT)
                                                        .show()
                                                } finally {
                                                    togglingFavoriteIds =
                                                        togglingFavoriteIds - item.event.id
                                                }
                                            }
                                        }
                                    },
                                    onLongClick = {
                                        selectedEventForAction = item.event
                                        showActionDialog = true
                                    }
                                )
                            }
                        }

                        isRefreshing -> Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(PaddingValues(vertical = 48.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }

                        else -> {
                            val emptyPrimary = if (isDarkTheme) Color(0xFFF2F2F2) else Color(0xFF161616)
                            val emptySecondary = if (isDarkTheme) Color(0xFFA2A2A2) else Color(0xFF727272)
                            val emptyIconTint = if (isDarkTheme) Color(0xFFB8B8B8) else Color(0xFF2F2F2F)
                            HomeDiscoverEmptyState(
                                modifier = Modifier.fillMaxSize(),
                                title = stringResource(R.string.home_discover_empty_title),
                                subtitle = stringResource(R.string.home_discover_empty_subtitle),
                                primaryText = emptyPrimary,
                                secondaryText = emptySecondary,
                                iconTint = emptyIconTint
                            )
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
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeFeedCard(
    item: HomeFeedCardItem,
    navController: NavController,
    isFavorite: Boolean,
    isTogglingFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onLongClick: () -> Unit
) {
    val event = item.event
    val isDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    val cardBackground = if (isDarkTheme) Color(0xFF151515) else Color.White
    val coverContentColor = if (isDarkTheme) Color.White else Color(0xFF333333)
    val cardMetaColor = MaterialTheme.colorScheme.onSurfaceVariant

    val titleColor =
        if (isDarkTheme) MaterialTheme.colorScheme.onSurface else Color(0xFF222222)
    val metaColor =
        if (isDarkTheme) cardMetaColor else Color(0xFF717171)
    val titleStyle = TextStyle(
        fontSize = 15.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
        color = titleColor,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.FirstLineTop
        )
    )
    val metaStyle = TextStyle(
        fontSize = 14.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Normal,
        color = metaColor,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.Both
        )
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { navController.navigate(Screen.ServiceDetail.createRoute(event.id)) },
                onLongClick = onLongClick
            )
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = cardBackground,
            shadowElevation = 1.dp,
            tonalElevation = 0.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(item.imageHeight)
            ) {
                if (item.imageModel != null) {
                    HomeFeedCoverImage(
                        imageModel = item.imageModel,
                        contentDescription = event.title,
                        isDarkTheme = isDarkTheme,
                        title = event.title,
                        cardContentColor = coverContentColor
                    )
                } else {
                    HomeFeedImagePlaceholder(
                        title = event.title,
                        isDarkTheme = isDarkTheme,
                        cardContentColor = coverContentColor
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                        .size(30.dp)
                        .clickable(
                            enabled = !isTogglingFavorite,
                            onClick = onToggleFavorite
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isTogglingFavorite) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 1.8.dp,
                            color = HomeWishlistHeartColor
                        )
                    } else {
                        if (isFavorite) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = "移出心愿单",
                                tint = HomeWishlistHeartColor,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Favorite,
                                    contentDescription = "加入心愿单",
                                    tint = Color(0x8C8A8A8A),
                                    modifier = Modifier.size(24.dp)
                                )
                                Icon(
                                    imageVector = Icons.Outlined.FavoriteBorder,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isDarkTheme) cardBackground else Color.White)
                .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 4.dp)
        ) {
            Text(
                text = event.title,
                style = titleStyle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (item.infoText.isNotBlank()) {
                Text(
                    text = item.infoText,
                    style = metaStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(18.dp))
            }

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = item.footerText,
                style = metaStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
            )
        }
    }
}

@Composable
private fun HomeFeedImagePlaceholder(
    title: String,
    isDarkTheme: Boolean,
    cardContentColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(
                Brush.verticalGradient(
                    if (isDarkTheme) {
                        listOf(Color(0xFF2A2A2A), Color(0xFF161616))
                    } else {
                        listOf(Color(0xFFF2F5FF), Color(0xFFE7ECF9))
                    }
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            fontSize = 15.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = cardContentColor,
            textAlign = TextAlign.Center,
            maxLines = 5,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HomeFeedCoverImage(
    imageModel: Any,
    contentDescription: String,
    isDarkTheme: Boolean,
    title: String,
    cardContentColor: Color
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val decodeSize = remember(configuration.screenWidthDp, density) {
        val halfScreenDp = configuration.screenWidthDp / 2f - 24f
        val w = (halfScreenDp * density.density).roundToInt().coerceIn(200, 640)
        val h = with(density) { 252.dp.roundToPx().coerceIn(280, 720) }
        Size(w, h)
    }
    val retryModel = remember(imageModel) {
        val url = imageModel as? String
        if (url != null && url.startsWith("file:///android_asset/")) {
            Uri.decode(url).takeIf { it != url }
        } else {
            null
        }
    }
    var activeModel by remember(imageModel) { mutableStateOf(imageModel) }
    var hasRetried by remember(imageModel) { mutableStateOf(false) }
    val imageRequest = remember(activeModel, decodeSize) {
        ImageRequest.Builder(context)
            .data(activeModel)
            .size(decodeSize)
            .crossfade(false)
            .build()
    }
    val painter = rememberAsyncImagePainter(model = imageRequest)
    val painterState = painter.state
    val successState = painterState as? AsyncImagePainter.State.Success
    val isExtremeWide = remember(successState) {
        val drawable = successState?.result?.drawable
        drawable != null && drawable.intrinsicWidth > 0 && drawable.intrinsicHeight > 0 &&
            drawable.intrinsicWidth.toFloat() / drawable.intrinsicHeight.toFloat() > 1.9f
    }

    if (painterState is AsyncImagePainter.State.Error) {
        if (!hasRetried && retryModel != null) {
            hasRetried = true
            activeModel = retryModel
            HomeFeedImagePlaceholder(
                title = title,
                isDarkTheme = isDarkTheme,
                cardContentColor = cardContentColor
            )
            return
        }
        Image(
            painter = painterResource(id = R.drawable.ic_logo),
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        return
    }

    val isLoadingOrEmpty =
        painterState is AsyncImagePainter.State.Loading || painterState is AsyncImagePainter.State.Empty

    if (isExtremeWide) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 极端横图：裁切底层 + 半透明遮罩 + 前景 Fit（避免 blur，滚动时 GPU 压力大）
            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.42f },
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isDarkTheme) Color(0x33000000) else Color(0x26FFFFFF))
            )
            Image(
                painter = painter,
                contentDescription = contentDescription,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentScale = ContentScale.Fit
            )
            if (isLoadingOrEmpty) {
                HomeFeedImagePlaceholder(
                    title = title,
                    isDarkTheme = isDarkTheme,
                    cardContentColor = cardContentColor
                )
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painter,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            if (isLoadingOrEmpty) {
                HomeFeedImagePlaceholder(
                    title = title,
                    isDarkTheme = isDarkTheme,
                    cardContentColor = cardContentColor
                )
            }
        }
    }
}
// 辅助扩展函数：将服务数据转换为首页瀑布流卡片模型（creator 地区由上层批量解析后传入，避免每条服务查用户）
fun com.example.Lulu.data.model.Service.toServiceEvent(creatorRegionFallback: String): ServiceEvent {
    val publishCity = extractCityLevelLocation(
        com.example.Lulu.util.ServiceLocationPolygonCodec.displayLine(this.location),
        creatorRegionFallback
    )
    val resolvedCover = coverImageUrl.ifBlank {
        imageUrls.firstOrNull { it.isNotBlank() }.orEmpty()
    }
    return ServiceEvent(
        id = this.id,
        title = this.title,
        creatorId = this.creatorId,
        coverImageUri = resolvedCover.takeIf { it.isNotBlank() },
        publishCity = publishCity,
        priceText = this.priceText,
        category = ServiceCategories.normalize(this.category),
    )
}

/** 首页发现流无数据时占位，版式与心愿单 / 消息列表缺省页一致（顶图标 + 大字 + 小字）。 */
@Composable
private fun HomeDiscoverEmptyState(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    primaryText: Color,
    secondaryText: Color,
    iconTint: Color
) {
    Column(
        modifier = modifier
            .padding(top = 140.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.Explore,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(38.dp)
        )
        Text(
            text = title,
            color = primaryText,
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 26.dp)
        )
        Text(
            text = subtitle,
            color = secondaryText,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

/** 首页发现 Tab 顶部分类胶囊（浅色参考：浅灰轨道、白丸、细灰边 / 选中粗黑边黑字） */
private val HomeCategoryChipTrackLight = Color(0xFFF7F7F7)
private val HomeCategoryChipBorderIdleLight = Color(0xFFDDDDDD)
private val HomeCategoryChipTextIdleLight = Color(0xFF222222)

@Composable
private fun HomeCategoryFilterChips(
    selectedLabel: String,
    onSelectionChange: (String) -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val rowBg = if (isDarkTheme) MaterialTheme.colorScheme.surface else HomeCategoryChipTrackLight
    val shape = RoundedCornerShape(18.dp)
    val listState = rememberLazyListState()
    val chipLabels = ServiceCategories.HOME_FILTER_CHIPS
    val lastChipIndex = chipLabels.lastIndex

    LaunchedEffect(listState, selectedLabel) {
        snapshotFlow {
            val selectedIndex = chipLabels.indexOf(selectedLabel)
            val visibleItems = listState.layoutInfo.visibleItemsInfo
            if (selectedIndex < 0 || visibleItems.isEmpty()) {
                null
            } else {
                Triple(selectedIndex, visibleItems.first().index, visibleItems.last().index)
            }
        }
            .distinctUntilChanged()
            .collect { indexTriple ->
                indexTriple?.let { (selectedIndex, firstVisibleIndex, lastVisibleIndex) ->
                    if (
                        selectedIndex == lastVisibleIndex &&
                        selectedIndex in 0 until lastChipIndex
                    ) {
                        listState.animateScrollToItem((selectedIndex + 1).coerceAtMost(lastChipIndex))
                    } else if (selectedIndex == firstVisibleIndex && selectedIndex > 0) {
                        listState.animateScrollToItem((selectedIndex - 1).coerceAtLeast(0))
                    }
                }
            }
    }

    LazyRow(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .background(rowBg),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        items(chipLabels, key = { it }) { label ->
            val selected = label == selectedLabel
            HomeCategoryFilterChip(
                label = label,
                selected = selected,
                isDarkTheme = isDarkTheme,
                shape = shape,
                onClick = { onSelectionChange(label) }
            )
        }
    }
}

@Composable
private fun HomeCategoryFilterChip(
    label: String,
    selected: Boolean,
    isDarkTheme: Boolean,
    shape: RoundedCornerShape,
    onClick: () -> Unit
) {
    val backgroundColor = if (selected) Color(0xFF1F1F1F) else Color(0xFFF1F1F1)
    val textColor = if (selected) Color.White else Color(0xFF3B3B3B)
    val fontWeight = FontWeight.Normal
    val chipTextStyle = TextStyle(
        fontSize = 12.sp,
        fontWeight = fontWeight,
        color = textColor,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeight = 18.sp,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.Both
        )
    )
    val interactionSource = remember(label) { MutableInteractionSource() }
    Surface(
        modifier = Modifier
            .height(26.dp)
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(bounded = true),
                onClick = onClick
            ),
        shape = shape,
        color = backgroundColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .wrapContentWidth()
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = chipTextStyle,
                maxLines = 1,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Clip
            )
        }
    }
}

@Composable
private fun HomeTopBar(
    navController: NavController,
    selectedTab: HomeDiscoverTab,
    nearbyTitle: String,
    onTabChange: (HomeDiscoverTab) -> Unit,
    onTitleRefreshTap: () -> Unit
) {
    val isDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    
    val topBarColor = MaterialTheme.colorScheme.surface
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            window.statusBarColor = topBarColor.toArgb()
            insetsController.isAppearanceLightStatusBars = !isDarkTheme
        }
    }

    // 权限检测逻辑
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var refreshTrigger by remember { mutableStateOf(0) }
    
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val hasAllPermissions = remember(refreshTrigger) {
        PermissionService.hasNotificationPermission(context)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .background(topBarColor)
            .padding(horizontal = 8.dp)
    ) {
        // 左上角权限按钮
        if (!hasAllPermissions) {
            IconButton(
                onClick = { navController.navigate(Screen.SystemPermission.route) },
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                // 圆形红色警告按钮
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = stringResource(R.string.permission_required),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .pointerInput(selectedTab, nearbyTitle) {
                    detectTapGestures(
                        onDoubleTap = { onTitleRefreshTap() }
                    )
                },
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HomeDiscoverTab.entries.forEach { tab ->
                val isSelected = tab == selectedTab
                val tabLabel = if (tab == HomeDiscoverTab.Nearby) nearbyTitle else tab.label
                val animatedFontSize by animateFloatAsState(
                    targetValue = if (isSelected) 19f else 16f,
                    animationSpec = tween(durationMillis = 180),
                    label = "homeTabFontSize"
                )
                val animatedTextColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    animationSpec = tween(durationMillis = 180),
                    label = "homeTabTextColor"
                )
                val animatedUnderlineWidth by animateDpAsState(
                    targetValue = if (isSelected) 22.dp else 0.dp,
                    animationSpec = tween(durationMillis = 180),
                    label = "homeTabUnderlineWidth"
                )
                val animatedUnderlineAlpha by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0f,
                    animationSpec = tween(durationMillis = 180),
                    label = "homeTabUnderlineAlpha"
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onTabChange(tab) }
                ) {
                    Text(
                        text = tabLabel,
                        fontSize = animatedFontSize.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = animatedTextColor
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .width(animatedUnderlineWidth)
                            .height(3.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFFE0115F).copy(alpha = animatedUnderlineAlpha))
                    )
                }
            }
        }
            
        // 右上角菜单
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.navigate(Screen.Search.route) }) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "搜索",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun NetworkDisconnectedBanner() {
    val isDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()

    // 模仿微信断网提示样式
    val bannerColor = if (isDarkTheme) Color(0xFF8B2B2B) else Color(0xFFFEEAEA)
    val textColor = if (isDarkTheme) Color(0xFFD4D4D4) else Color(0xFF666666)
    val iconColor = if (isDarkTheme) Color(0xFFF25656) else Color(0xFFE54D42)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bannerColor)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = stringResource(R.string.network_disconnected_message),
            color = textColor,
            fontSize = 14.sp
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen(rememberNavController())
}
