// 文件说明：登录后主框架（底部导航、多 Tab 容器）。

package com.example.Lulu.ui.screen

/**
 * 主页面容器文件。
 * 负责底部导航分发、未读角标显示，以及全局顶部菜单入口的渲染与交互。
 */

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import androidx.compose.ui.platform.LocalContext
import com.example.Lulu.R
import com.example.Lulu.data.local.AppDataStore
import com.example.Lulu.ui.navigation.Screen

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.zIndex
import com.example.Lulu.util.BadgeUtils

private data class BottomNavItem(
    val label: String,
    val normalIconRes: Int,
    val selectedIconRes: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavController,
    /** 主壳内层导航（首页/心愿单内打开详情）；为 null 时详情走外层 navController。 */
    mainShellNavController: NavController? = null,
    /** 外层 `home` 路由条目：用于心愿单详情返回时切 Tab（主壳未重建时仍生效） */
    homeBackStackEntry: NavBackStackEntry? = null,
    initialSelectedItem: Int = 0
) {
    // 勿用 LaunchedEffect(initialSelectedItem) 同步 Tab：从外层详情 pop 回 home 时参数多为 0，会覆盖已保存的底部 Tab。
    var selectedItem by rememberSaveable(initialSelectedItem) { mutableIntStateOf(initialSelectedItem) }
    var homeTabReselectTrigger by rememberSaveable { mutableIntStateOf(0) }

    val forceMainTabFlow = remember(homeBackStackEntry) {
        homeBackStackEntry?.savedStateHandle?.getStateFlow<Int?>("force_main_tab", null)
            ?: flowOf(null)
    }
    val forceMainTabSignal by forceMainTabFlow.collectAsState(initial = null)
    LaunchedEffect(forceMainTabSignal) {
        val tab = forceMainTabSignal ?: return@LaunchedEffect
        homeBackStackEntry?.savedStateHandle?.remove<Int>("force_main_tab")
        selectedItem = tab.coerceIn(0, 3)
    }
    
    val context = LocalContext.current
    val repository = remember { AppDataStore.getRepository() }
    val snackbarHostState = remember { SnackbarHostState() }
    val unreadChatCount by (repository?.unreadChatCount?.collectAsState(initial = 0)
        ?: remember { mutableStateOf(0) })

    LaunchedEffect(repository) {
        repository?.wishlistRemoteSyncFailures?.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Sync App Icon Badge with unread count
    LaunchedEffect(unreadChatCount) {
        BadgeUtils.setBadgeCount(context, unreadChatCount)
    }

    val items = listOf(
        BottomNavItem(
            label = "爱野",
            normalIconRes = R.drawable.tab_home_normal,
            selectedIconRes = R.drawable.tab_home_selected
        ),
        BottomNavItem(
            label = "心愿单",
            normalIconRes = R.drawable.tab_wishlist_normal,
            selectedIconRes = R.drawable.tab_wishlist_selected
        ),
        BottomNavItem(
            label = "消息",
            normalIconRes = R.drawable.tab_messages_normal,
            selectedIconRes = R.drawable.tab_messages_selected
        ),
        BottomNavItem(
            label = "我的",
            normalIconRes = R.drawable.tab_mine_normal,
            selectedIconRes = R.drawable.tab_mine_selected
        )
    )

    val themeColor = MaterialTheme.colorScheme.primary

    val navBarColor = MaterialTheme.colorScheme.surface

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            val navBarInsets = NavigationBarDefaults.windowInsets
            Surface(
                color = navBarColor,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(navBarInsets)
                        .height(52.dp)
                        .selectableGroup(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEachIndexed { index, item ->
                        val selected = selectedItem == index
                        val tint =
                            if (selected) themeColor
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        val iconRes = if (selected) item.selectedIconRes else item.normalIconRes
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .selectable(
                                    selected = selected,
                                    onClick = {
                                        if (selectedItem == index && index == 0) {
                                            homeTabReselectTrigger++
                                        } else if (selectedItem != index) {
                                            selectedItem = index
                                        }
                                    },
                                    role = Role.Tab,
                                    interactionSource = remember(index) { MutableInteractionSource() },
                                    indication = null,
                                )
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (index == 2 && unreadChatCount > 0) {
                                    BadgedBox(
                                        badge = {
                                            Badge(
                                                containerColor = Color.Red,
                                                contentColor = Color.White
                                            ) {
                                                Text(
                                                    text = if (unreadChatCount > 99) "99+" else unreadChatCount.toString(),
                                                    fontSize = 9.sp
                                                )
                                            }
                                        }
                                    ) {
                                        Icon(
                                            painter = painterResource(id = iconRes),
                                            contentDescription = item.label,
                                            modifier = Modifier.size(23.dp),
                                            tint = Color.Unspecified
                                        )
                                    }
                                } else {
                                    Icon(
                                        painter = painterResource(id = iconRes),
                                        contentDescription = item.label,
                                        modifier = Modifier.size(23.dp),
                                        tint = Color.Unspecified
                                    )
                                }
                            }
                            Text(
                                text = item.label,
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                                color = tint,
                                maxLines = 1,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        // Scaffold 主体按全屏测量，底部栏叠在内容之上；须应用传入的 PaddingValues，否则列表最后一行会被遮挡。
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .padding(paddingValues)
        ) {
            // 主页始终保留在组合中，避免从其它底部 Tab 切回时整页重建与重新拉数；
            // 非选中时透明并拦截触摸，其它 Tab 叠在更高 zIndex 上。
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(if (selectedItem == 0) 1f else 0f)
                    .graphicsLayer {
                        alpha = if (selectedItem == 0) 1f else 0f
                    }
                    .pointerInput(selectedItem) {
                        if (selectedItem == 0) return@pointerInput
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                            }
                        }
                    }
            ) {
                HomeScreen(
                    navController = navController,
                    mainShellNavController = mainShellNavController,
                    homeTabReselectTrigger = homeTabReselectTrigger
                )
            }
            when (selectedItem) {
                0 -> Unit
                1 -> Box(
                    Modifier
                        .fillMaxSize()
                        .zIndex(1f)
                ) {
                    SquareScreen(
                        navController = navController,
                        mainShellNavController = mainShellNavController
                    )
                }
                2 -> Box(
                    Modifier
                        .fillMaxSize()
                        .zIndex(1f)
                ) {
                    MessagesScreen(navController)
                }
                3 -> Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color(0xFFFFFFFF))
                        .zIndex(1f)
                ) {
                    UserInfoScreen(navController, showBackButton = false)
                }
            }
        }
    }
}

@Composable
fun SharedTopBarMenu(navController: NavController) {
    val isDarkTheme = isSystemInDarkTheme()
    var menuExpanded by remember { mutableStateOf(false) }

    val menuContainerColor = if (isDarkTheme) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary
    val menuItemColor = if (isDarkTheme) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
    val menuDotColor = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.primary

    Box {
        // 自定义 2x2 四点图标按钮
        IconButton(
            onClick = { menuExpanded = true },
            modifier = Modifier.size(48.dp) // 优化点击热区
        ) {
            Canvas(modifier = Modifier.size(24.dp)) { // 保持视觉大小不变
                val dotSize = 4.dp.toPx() // 点的大小
                val gap = 4.dp.toPx()     // 间距
                
                // 计算起始位置以居中
                val totalSize = dotSize * 2 + gap
                val startX = (size.width - totalSize) / 2
                val startY = (size.height - totalSize) / 2

                // 左上
                drawCircle(
                    color = menuDotColor, 
                    radius = dotSize / 2, 
                    center = Offset(startX + dotSize / 2, startY + dotSize / 2)
                )
                // 右上
                drawCircle(
                    color = menuDotColor, 
                    radius = dotSize / 2, 
                    center = Offset(startX + dotSize * 1.5f + gap, startY + dotSize / 2)
                )
                // 左下
                drawCircle(
                    color = menuDotColor, 
                    radius = dotSize / 2, 
                    center = Offset(startX + dotSize / 2, startY + dotSize * 1.5f + gap)
                )
                // 右下
                drawCircle(
                    color = menuDotColor, 
                    radius = dotSize / 2, 
                    center = Offset(startX + dotSize * 1.5f + gap, startY + dotSize * 1.5f + gap)
                )
            }
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            modifier = Modifier.background(menuContainerColor)
        ) {
            val itemPadding = PaddingValues(start = 12.dp, end = 30.dp)

            DropdownMenuItem(
                text = { Text("扫一扫", color = menuItemColor) },
                onClick = { 
                    menuExpanded = false
                    navController.navigate(Screen.Scan.route)
                },
                leadingIcon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = menuItemColor) },
                contentPadding = itemPadding
            )
            DropdownMenuItem(
                text = { Text("搜一搜", color = menuItemColor) },
                onClick = { 
                    menuExpanded = false
                    navController.navigate(Screen.Search.route)
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = menuItemColor) },
                contentPadding = itemPadding
            )
        }
    }
}
