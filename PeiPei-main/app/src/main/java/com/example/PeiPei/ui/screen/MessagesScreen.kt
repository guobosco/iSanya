// 文件说明：消息列表（会话列表）界面。

package com.example.Lulu.ui.screen

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.Lulu.data.local.MockDataStore
import com.example.Lulu.data.model.ChatConversation
import com.example.Lulu.ui.components.CommonAvatar
import com.example.Lulu.ui.components.FeiLingPullRefreshHintIndicator
import com.example.Lulu.ui.components.FeiLingTopSyncIndicator
import com.example.Lulu.ui.navigation.Screen
import com.example.Lulu.ui.util.PullRefreshTokens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MessagesScreen(navController: NavController) {
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val pageColor = Color.White
    val repository = remember { MockDataStore.getRepository() }
    val currentUser by MockDataStore.currentUser.collectAsState()
    val contacts by MockDataStore.contacts.collectAsState()
    val conversations by (
        repository?.allConversations?.collectAsState(initial = emptyList())
            ?: remember { mutableStateOf(emptyList()) }
    )
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val tabs = listOf("全部", "问询我的", "我问询的", "用户支持", "已归档")
    val allTabIndex = 0
    val askedMeTabIndex = 1
    val iAskedTabIndex = 2
    val archivedTabIndex = tabs.lastIndex
    var selectedTab by remember {
        mutableIntStateOf(allTabIndex)
    }
    var latestMessageFallbacks by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var starters by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var starredThreadIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var archivedOverrides by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    val messageRows = remember(
        conversations,
        contacts,
        currentUser.id,
        latestMessageFallbacks,
        starters,
        starredThreadIds,
        archivedOverrides
    ) {
        conversations
            .sortedByDescending { it.lastMessageAt }
            .map { conversation ->
            val peerUserId = conversation.participantIds
                .firstOrNull { it != currentUser.id }
                ?.trim()
                .orEmpty()
            val peer = peerUserId.takeIf { it.isNotEmpty() }
                ?.let { id -> contacts.firstOrNull { it.id == id } }
            val starterId = starters[conversation.id].orEmpty()
            val inquiryType = when {
                starterId.isBlank() -> InquiryType.Unknown
                starterId == currentUser.id -> InquiryType.IAsked
                else -> InquiryType.AskedMe
            }
            MessageRowData(
                threadId = conversation.id,
                peerUserId = peerUserId,
                avatarUrl = peer?.photoUrl.orEmpty(),
                title = peer?.remarkName?.ifBlank { peer.name }
                    ?.ifBlank { conversation.title.ifBlank { "新对话" } }
                    ?: conversation.title.ifBlank { "新对话" },
                snippet = conversation.lastMessagePreview.ifBlank {
                    latestMessageFallbacks[conversation.id].orEmpty().ifBlank { "暂无消息" }
                },
                time = formatConversationTime(
                    conversation.lastMessageAt
                        .takeIf { it > 0L }
                        ?: conversation.updatedAt.takeIf { it > 0L }
                        ?: conversation.createdAt
                ),
                isArchived = archivedOverrides[conversation.id] ?: conversation.isDeleted,
                isStarred = starredThreadIds.contains(conversation.id),
                inquiryType = inquiryType
            )
        }
    }
    val filteredMessages = remember(selectedTab, messageRows) {
        when (selectedTab) {
            allTabIndex -> messageRows.filter { !it.isArchived }
            askedMeTabIndex -> messageRows.filter { !it.isArchived && it.inquiryType == InquiryType.AskedMe }
            iAskedTabIndex -> messageRows.filter { !it.isArchived && it.inquiryType == InquiryType.IAsked }
            archivedTabIndex -> messageRows.filter { it.isArchived }
            else -> messageRows.filter { !it.isArchived }
        }
    }
    val showList = filteredMessages.isNotEmpty()

    var isPullRefreshing by remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isPullRefreshing,
        onRefresh = {
            scope.launch {
                if (isPullRefreshing) return@launch
                if (repository == null || currentUser.id.isBlank()) {
                    delay(400)
                    return@launch
                }
                isPullRefreshing = true
                try {
                    repository.refreshMessagesTabData()
                } catch (_: Exception) {
                } finally {
                    delay(400)
                    isPullRefreshing = false
                }
            }
        },
        refreshThreshold = PullRefreshTokens.triggerThreshold,
    )

    LaunchedEffect(repository, currentUser.id) {
        if (repository != null && currentUser.id.isNotBlank()) {
            repository.fetchAndSyncConversations()
        }
    }
    LaunchedEffect(conversations, repository, currentUser.id) {
        if (repository == null || currentUser.id.isBlank()) {
            latestMessageFallbacks = emptyMap()
            return@LaunchedEffect
        }
        latestMessageFallbacks = conversations
            .filter { it.lastMessagePreview.isBlank() }
            .associate { conversation ->
                conversation.id to repository.getLatestMessagePreview(conversation.id)
            }
    }
    LaunchedEffect(conversations, repository, currentUser.id) {
        if (repository == null || currentUser.id.isBlank()) {
            starters = emptyMap()
            return@LaunchedEffect
        }
        starters = conversations.associate { conversation ->
            conversation.id to repository.getFirstMessageSenderId(conversation.id)
        }
    }
    DisposableEffect(lifecycleOwner, repository, currentUser.id) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && repository != null && currentUser.id.isNotBlank()) {
                scope.launch {
                    repository.fetchAndSyncConversations()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
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
            insetsController.isAppearanceLightStatusBars = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pageColor)
    ) {
        MessageHeader(
            selectedTab = selectedTab,
            tabs = tabs,
            onTabSelected = { selectedTab = it }
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pullRefresh(pullRefreshState)
        ) {
            if (showList) {
                MessageListContent(
                    modifier = Modifier.fillMaxSize(),
                    items = filteredMessages,
                    onStar = { row ->
                        starredThreadIds = starredThreadIds.toMutableSet().apply {
                            if (contains(row.threadId)) remove(row.threadId) else add(row.threadId)
                        }
                    },
                    onArchive = { row ->
                        archivedOverrides = archivedOverrides + (row.threadId to !row.isArchived)
                    },
                    onClick = { row ->
                        if (row.threadId.isBlank()) {
                            scope.launch {
                                snackbarHostState.showSnackbar("会话尚未准备好，请稍后再试")
                            }
                        } else {
                            navController.navigate(
                                Screen.MessageThread.createRoute(
                                    threadId = row.threadId,
                                    from = "messages",
                                    peerUserId = row.peerUserId.takeIf { it.isNotBlank() }
                                )
                            )
                        }
                    }
                )
            } else {
                EmptyMessageContent(modifier = Modifier.fillMaxSize())
            }
            FeiLingTopSyncIndicator(
                visible = isPullRefreshing,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
                    .zIndex(50f)
            )
            val pullProgress = pullRefreshState.progress
            val showPullHint = !isPullRefreshing && pullProgress > 0f
            FeiLingPullRefreshHintIndicator(
                progress = pullProgress,
                visible = showPullHint,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
                    .zIndex(if (showPullHint) 100f else -1f)
            )
        }
        SnackbarHost(hostState = snackbarHostState)
    }
}

@Composable
private fun MessageHeader(
    selectedTab: Int,
    tabs: List<String>,
    onTabSelected: (Int) -> Unit
) {
    val tabScrollState = rememberScrollState()

    LaunchedEffect(selectedTab, tabs.size) {
        if (selectedTab == tabs.lastIndex) {
            tabScrollState.animateScrollTo(tabScrollState.maxValue)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .statusBarsPadding()
            .padding(top = 4.dp, start = 24.dp, end = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.End
        ) {
            HeaderActionButton(icon = Icons.Outlined.Search, contentDescription = "搜索")
            Spacer(modifier = Modifier.width(10.dp))
            HeaderActionButton(icon = Icons.Outlined.Settings, contentDescription = "设置")
        }

        Text(
            text = "消息",
            fontSize = 24.sp,
            lineHeight = 30.sp,
            color = Color(0xFF202020),
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 12.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(tabScrollState)
                .padding(top = 18.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            tabs.forEachIndexed { index, tab ->
                val selected = index == selectedTab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (selected) Color(0xFF1F1F1F) else Color(0xFFF1F1F1))
                        .clickable { onTabSelected(index) }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = tab,
                        color = if (selected) Color.White else Color(0xFF3B3B3B),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color(0xFFF0F0F0))
            .clickable { },
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
private fun EmptyMessageContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(top = 140.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.ChatBubbleOutline,
            contentDescription = null,
            tint = Color(0xFF2F2F2F),
            modifier = Modifier.size(38.dp)
        )
        Text(
            text = "你没有任何消息",
            color = Color(0xFF202020),
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 26.dp)
        )
        Text(
            text = "你收到的新消息将显示在这里。",
            color = Color(0xFF8D8D8D),
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun MessageListContent(
    items: List<MessageRowData>,
    onStar: (MessageRowData) -> Unit,
    onArchive: (MessageRowData) -> Unit,
    onClick: (MessageRowData) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize()
    ) {
        items(items.size) { index ->
            MessageListItem(
                item = items[index],
                onStar = { onStar(items[index]) },
                onArchive = { onArchive(items[index]) },
                onClick = { onClick(items[index]) }
            )
        }
    }
}

@Composable
private fun MessageListItem(
    item: MessageRowData,
    onStar: () -> Unit,
    onArchive: () -> Unit,
    onClick: () -> Unit
) {
    val rowHeight = 84.dp
    val actionWidth = 180.dp
    val actionWidthPx = with(androidx.compose.ui.platform.LocalDensity.current) { actionWidth.toPx() }
    var offsetPx by remember(item.threadId) { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(actionWidth)
                .height(rowHeight),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            ActionButton(
                label = if (item.isStarred) "取消星标" else "加星标",
                icon = Icons.Filled.Star,
                backgroundColor = Color(0xFF0F8A78),
                onClick = {
                    onStar()
                    offsetPx = 0f
                }
            )
            ActionButton(
                label = if (item.isArchived) "取消归档" else "归档",
                icon = Icons.Outlined.Settings,
                backgroundColor = Color(0xFF4A4A4A),
                onClick = {
                    onArchive()
                    offsetPx = 0f
                }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetPx.roundToInt(), 0) }
                .background(Color.White)
                .pointerInput(item.threadId) {
                    detectHorizontalDragGestures(
                        onDragStart = { _: Offset -> },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            offsetPx = (offsetPx + dragAmount).coerceIn(-actionWidthPx, 0f)
                        },
                        onDragEnd = {
                            offsetPx = if (offsetPx <= -actionWidthPx * 0.45f) -actionWidthPx else 0f
                        },
                        onDragCancel = {
                            offsetPx = 0f
                        }
                    )
                }
                .clickable(onClick = onClick)
                .height(rowHeight)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            CommonAvatar(
                imageUrl = item.avatarUrl,
                name = item.title,
                modifier = Modifier
                    .size(60.dp)
                    .border(1.dp, Color(0xFFE4E4E4), CircleShape),
                shape = CircleShape,
                contentDescription = item.title
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.title,
                        color = Color(0xFF1F1F1F),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (item.isStarred) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "已加星标",
                            tint = Color(0xFFFFC107),
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .size(16.dp)
                        )
                    }
                }
                Text(
                    text = item.snippet,
                    color = Color(0xFF777777),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (item.time.isNotBlank()) {
                Text(
                    text = item.time,
                    color = Color(0xFF8C8C8C),
                    fontSize = 13.sp,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 2.dp, start = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun ActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(90.dp)
            .fillMaxSize()
            .clip(RoundedCornerShape(0.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

private data class MessageRowData(
    val threadId: String,
    val peerUserId: String,
    val avatarUrl: String,
    val title: String,
    val snippet: String,
    val time: String,
    val isArchived: Boolean,
    val isStarred: Boolean,
    val inquiryType: InquiryType
)

private enum class InquiryType {
    Unknown,
    AskedMe,
    IAsked
}

private fun formatConversationTime(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    val formatter = SimpleDateFormat("yy/M/d", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
