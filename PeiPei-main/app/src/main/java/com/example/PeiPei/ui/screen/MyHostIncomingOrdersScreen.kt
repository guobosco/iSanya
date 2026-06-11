// 文件说明：「我的接单」列表页（今天 / 后续、空状态、聊一聊进会话）。

package com.example.Lulu.ui.screen

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.Lulu.data.model.HostIncomingOrder
import com.example.Lulu.data.repository.LuluRepository
import com.example.Lulu.ui.navigation.Screen
import com.example.Lulu.ui.theme.BrandPink
import com.example.Lulu.util.HostOrderSchedule
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

private val PageBg = Color(0xFFFFFFFF)
private val TitleColor = Color(0xFF000000)
private val SegmentTrack = Color(0xFFE8E8E8)
private val SegmentSelectedBg = Color(0xFF2F2F2F)
private val SegmentUnselectedText = Color(0xFF555555)
private val Muted = Color(0xFF888888)
private val CardOutline = Color(0xFFEFEFEF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyHostIncomingOrdersScreen(navController: NavController) {
    val repository = remember { runCatching { LuluRepository.get() }.getOrNull() }
    val ordersFlow = remember(repository) {
        repository?.hostIncomingOrders ?: flowOf(emptyList())
    }
    val allOrders by ordersFlow.collectAsState(initial = emptyList())

    var todayTab by rememberSaveable { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var openingChatForId by remember { mutableStateOf<String?>(null) }

    val filtered = remember(allOrders, todayTab) {
        allOrders.filter { o ->
            if (todayTab) HostOrderSchedule.isInTodayTab(o) else !HostOrderSchedule.isInTodayTab(o)
        }
    }

    Scaffold(
        containerColor = PageBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            HostOrdersSegmentedRow(
                todaySelected = todayTab,
                onSelectToday = { todayTab = true },
                onSelectLater = { todayTab = false },
            )
            if (filtered.isEmpty()) {
                HostOrdersEmptyState(modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    items(filtered, key = { it.id }) { order ->
                        HostIncomingOrderRow(
                            order = order,
                            chatBusy = openingChatForId == order.id,
                            onChatClick = {
                                val repo = repository ?: run {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("打开聊天失败，请稍后重试")
                                    }
                                    return@HostIncomingOrderRow
                                }
                                scope.launch {
                                    openingChatForId = order.id
                                    val conv = repo.getOrCreateDirectConversation(order.guestUserId)
                                    openingChatForId = null
                                    if (conv == null) {
                                        snackbarHostState.showSnackbar("打开聊天失败，请稍后重试")
                                    } else {
                                        navController.navigate(
                                            Screen.MessageThread.createRoute(
                                                threadId = conv.id,
                                                serviceTitle = order.serviceTitle,
                                                from = "my_host_orders",
                                                contextServiceId = order.serviceId,
                                                peerUserId = order.guestUserId,
                                            )
                                        ) { launchSingleTop = true }
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HostOrdersSegmentedRow(
    todaySelected: Boolean,
    onSelectToday: () -> Unit,
    onSelectLater: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(SegmentTrack)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SegmentChip(
            text = "今天",
            selected = todaySelected,
            onClick = onSelectToday,
            modifier = Modifier.weight(1f),
        )
        SegmentChip(
            text = "后续",
            selected = !todaySelected,
            onClick = onSelectLater,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SegmentChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (selected) SegmentSelectedBg else Color.Transparent
    val fg = if (selected) Color.White else SegmentUnselectedText
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = fg, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun HostOrdersEmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Outlined.MenuBook,
                contentDescription = null,
                modifier = Modifier.size(96.dp),
                tint = Color(0xFFCFCFCF),
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "你没有任何订单",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF303030),
            )
        }
    }
}

private fun formatServiceDate(millis: Long): String {
    val fmt = SimpleDateFormat("M月d日", Locale.CHINA)
    return fmt.format(Date(millis))
}

@Composable
private fun HostIncomingOrderRow(
    order: HostIncomingOrder,
    chatBusy: Boolean,
    onChatClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, CardOutline),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = order.serviceTitle,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TitleColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${order.guestDisplayName} · ${formatServiceDate(order.serviceDateMillis)}",
                fontSize = 13.sp,
                color = Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = order.statusLabel,
                fontSize = 13.sp,
                color = Color(0xFF444444),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = order.summaryLine,
                fontSize = 13.sp,
                color = Muted,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            order.priceLine?.let { line ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = line, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = BrandPink)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = onChatClick,
                    enabled = !chatBusy,
                ) {
                    Text("聊一聊", color = BrandPink, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
