// 文件说明：用户资料展示或设置相关信息的界面。

package com.example.Lulu.ui.screen

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.Lulu.service.PermissionService
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.example.Lulu.R
import com.example.Lulu.data.local.AppDataStore
import com.example.Lulu.data.model.User
import com.example.Lulu.ui.navigation.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.Lulu.ui.components.EditDialog
import com.example.Lulu.ui.components.ProfileGroup
import com.example.Lulu.ui.components.ProfileItem
import com.example.Lulu.ui.components.RegionPickerDialog

import com.example.Lulu.data.remote.RetrofitClient
import com.example.Lulu.ui.components.FullScreenImageDialog
import com.example.Lulu.ui.components.AvatarIdentityShieldOverlay
import com.example.Lulu.ui.components.IdentityVerificationCallout
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat

/** 我的页：顶部资料卡、功能分组与底栏 FAB 相对屏幕左右的留白 */
private val ProfileMinePageCardHorizontalInset = 24.dp

private fun platformYearsOnLulu(createdAt: Long): String {
    val elapsed = System.currentTimeMillis() - createdAt
    if (elapsed <= 0L) return "不足1年"
    val years = (elapsed / (365.25 * 24 * 60 * 60 * 1000)).toInt()
    return if (years <= 0) "不足1年" else "${years}年"
}

/** 个人资料卡右侧：标签 + 数值（与左侧头像区高度对齐） */
@Composable
private fun MyProfileStatRow(
    label: String,
    value: String,
    valueColor: Color,
    labelColor: Color
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = label,
            color = labelColor,
            fontSize = 12.sp,
            lineHeight = 15.sp
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserInfoScreen(navController: NavController, showBackButton: Boolean = true) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val user by AppDataStore.currentUser.collectAsState()
    val isLoggedIn = user.id.isNotEmpty()
    val navigateToLogin: () -> Unit = {
        navController.navigate(Screen.Login.createRoute()) { launchSingleTop = true }
    }

    // State to trigger recomposition when permissions change
    var refreshTrigger by remember { mutableStateOf(0) }
    
    // Refresh on resume
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

    // Check important permissions
    val notificationGranted = remember(refreshTrigger) { PermissionService.hasNotificationPermission(context) }
    val hasPendingPermissions = !notificationGranted

    /** 页背景、分组卡片、顶部资料卡均为 #FFFFFF；正文字色按白底对比度固定为浅色方案 */
    val pureWhite = Color(0xFFFFFFFF)
    val backgroundColor = pureWhite
    val cardColor = pureWhite
    val profileHeroCardBg = pureWhite
    val profileListIconColor = Color.Black

    // 头像选择器
    val avatarLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            // 模拟更新头像
            Toast.makeText(context, "头像已更新 (模拟)", Toast.LENGTH_SHORT).show()
            // AppDataStore.updateCurrentUser(user.copy(photoUrl = uri.toString()))
        }
    }

    // Dialog States
    var showNameDialog by remember { mutableStateOf(false) }
    var showRegionDialog by remember { mutableStateOf(false) } // For Profile Region
    var showPhoneDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showVerifyPhoneDialog by remember { mutableStateOf(false) }
    
    // Edit Values
    var editValue by remember { mutableStateOf("") }
    
    var showFullScreenAvatar by remember { mutableStateOf(false) }
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            window.statusBarColor = backgroundColor.toArgb()
            insetsController.isAppearanceLightStatusBars = true
        }
    }

    if (showFullScreenAvatar && user.photoUrl.isNotEmpty()) {
        FullScreenImageDialog(
            imageUrl = user.photoUrl,
            onDismiss = { showFullScreenAvatar = false }
        )
    }

    Scaffold(
        containerColor = backgroundColor,
        // 与 MainScreen / MessagesScreen 一致：不把状态栏 inset 叠进 content，避免与下方 statusBarsPadding 重复导致整体偏下
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            Surface(
                color = Color(0xFFF7F7F7),
                shape = RoundedCornerShape(22.dp),
                shadowElevation = 0.dp,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ProfileMinePageCardHorizontalInset)
                    .padding(bottom = 20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (!isLoggedIn) {
                                navigateToLogin()
                            } else {
                                navController.navigate(Screen.CreateService.createRoute()) {
                                    launchSingleTop = true
                                }
                            }
                        }
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = Color.Black
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "发布「服务」或「体验」",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF202020)
                        )
                        Text(
                            text = "填写表单信息即可上架接单",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF717171),
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(pureWhite)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            val textPrimary = Color(0xFF202020)
            val textSecondary = Color(0xFF666666)
            val statDivider = Color(0xFFECECEC)
            val regionSubtitle = remember(user.region, user.livingCity, user.livingCountry) {
                when {
                    user.region.isNotBlank() -> user.region
                    user.livingCity.isNotBlank() -> {
                        val c = user.livingCountry
                        if (c.isNotBlank()) "${user.livingCity}, $c" else user.livingCity
                    }
                    user.livingCountry.isNotBlank() -> user.livingCountry
                    else -> "用户所在地区"
                }
            }

            // 与 MessagesScreen.MessageHeader 中「消息」标题：相同边距、字号、行高、字重与纵向节奏
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(pureWhite)
                    .statusBarsPadding()
                    .padding(top = 4.dp, start = 24.dp, end = 24.dp)
            ) {
                // 仅设置按钮：与 MessagesScreen.HeaderActionButton 同尺寸与配色，右对齐（消息页设置为最右一颗）
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    val actionBg = Color(0xFFF0F0F0)
                    val actionIconTint = Color(0xFF242424)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(actionBg)
                            .clickable {
                                if (isLoggedIn) {
                                    navController.navigate(Screen.SystemPermission.route) { launchSingleTop = true }
                                } else {
                                    navigateToLogin()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "设置",
                            tint = actionIconTint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Text(
                    text = "个人资料",
                    fontSize = 24.sp,
                    lineHeight = 30.sp,
                    color = Color(0xFF202020),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(38.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ProfileMinePageCardHorizontalInset)
                    .clickable {
                        if (isLoggedIn) {
                            navController.navigate(Screen.ServiceHostProfile.createRoute(user.id)) {
                                launchSingleTop = true
                            }
                        } else {
                            navigateToLogin()
                        }
                    },
                color = profileHeroCardBg,
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 10.dp,
                tonalElevation = 0.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                    // 左 0.6 / 右 0.4；右侧为 12sp 标签 + 16sp 数值，与左侧头像列高度对齐
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 6.dp, end = 26.dp, top = 40.dp, bottom = 14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                    Column(
                        modifier = Modifier.weight(0.6f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.padding(
                                bottom = if (isLoggedIn && user.identityVerified) 8.dp else 0.dp
                            )
                        ) {
                            Surface(
                                modifier = Modifier
                                    .size(106.dp)
                                    .clip(CircleShape)
                                    .clickable(enabled = isLoggedIn && user.photoUrl.isNotEmpty()) {
                                        if (user.photoUrl.isNotEmpty()) showFullScreenAvatar = true
                                    },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                if (isLoggedIn && user.photoUrl.isNotEmpty()) {
                                    coil.compose.AsyncImage(
                                        model = RetrofitClient.normalizeBackendMediaUrlForDisplay(user.photoUrl),
                                        contentDescription = "头像",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        if (isLoggedIn) {
                                            Text(
                                                text = user.name.take(1).ifEmpty { "我" },
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                modifier = Modifier.size(40.dp),
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                            if (isLoggedIn) {
                                AvatarIdentityShieldOverlay(
                                    verified = user.identityVerified,
                                    shieldSize = 23.dp,
                                    markSize = 10.dp,
                                    modifier = Modifier.align(Alignment.BottomEnd)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (isLoggedIn) user.name else "未登录",
                            color = textPrimary,
                            fontSize = 21.sp,
                            lineHeight = 26.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isLoggedIn) regionSubtitle else "点击登录",
                            color = textSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 3.dp, start = 4.dp, end = 4.dp, bottom = 2.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(0.4f)
                            .padding(start = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MyProfileStatRow(
                            label = "爱野 ID",
                            value = if (isLoggedIn) user.peiPeiId.ifEmpty { "未设置" } else "—",
                            valueColor = textPrimary,
                            labelColor = textSecondary
                        )
                        HorizontalDivider(thickness = 1.dp, color = statDivider)
                        MyProfileStatRow(
                            label = "性别",
                            value = if (isLoggedIn) user.gender.ifEmpty { "未设置" } else "—",
                            valueColor = textPrimary,
                            labelColor = textSecondary
                        )
                        HorizontalDivider(thickness = 1.dp, color = statDivider)
                        MyProfileStatRow(
                            label = "加入 爱野",
                            value = if (isLoggedIn) platformYearsOnLulu(user.createdAt) else "—",
                            valueColor = textPrimary,
                            labelColor = textSecondary
                        )
                    }
                    }
                    if (isLoggedIn) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = "我的二维码",
                            tint = Color(0xFF8B96A8),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 28.dp, end = 12.dp)
                                .size(22.dp)
                                .clickable {
                                    navController.navigate(Screen.MyQrCode.route) { launchSingleTop = true }
                                }
                        )
                    }
                    }
                    Text(
                        text = "更多资料",
                        fontSize = 9.sp,
                        lineHeight = 12.sp,
                        color = textSecondary,
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 6.dp, end = 26.dp, top = 4.dp, bottom = 18.dp)
                    )
                }
            }

            if (isLoggedIn && !user.identityVerified) {
                Spacer(modifier = Modifier.height(8.dp))
                IdentityVerificationCallout(
                    onStartVerification = {
                        navController.navigate(Screen.RealNameVerification.route) {
                            launchSingleTop = true
                        }
                    },
                    modifier = Modifier.padding(horizontal = ProfileMinePageCardHorizontalInset)
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Function List
            
            // Group 1: 我预定的、我浏览的
            ProfileGroup(
                backgroundColor = cardColor,
                shape = RoundedCornerShape(12.dp),
                elevation = 0.dp,
                horizontalOuterPadding = ProfileMinePageCardHorizontalInset
            ) {
                ProfileItem(
                    label = "我预定的",
                    icon = Icons.Default.DateRange,
                    showArrow = true,
                    iconColor = profileListIconColor,
                    labelFontSize = 14.sp,
                    onClick = { if (!isLoggedIn) navigateToLogin() }
                )
                Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(start = 56.dp))

                ProfileItem(
                    label = "我浏览的",
                    icon = Icons.Default.Search,
                    showArrow = true,
                    iconColor = profileListIconColor,
                    labelFontSize = 14.sp,
                    onClick = { if (!isLoggedIn) navigateToLogin() }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Group 2: 我发布的、我的收入、评价我的
            ProfileGroup(
                backgroundColor = cardColor,
                shape = RoundedCornerShape(12.dp),
                elevation = 0.dp,
                horizontalOuterPadding = ProfileMinePageCardHorizontalInset
            ) {
                ProfileItem(
                    label = "我发布的",
                    icon = Icons.Default.Storefront,
                    showArrow = true,
                    iconColor = profileListIconColor,
                    labelFontSize = 14.sp,
                    onClick = {
                        if (!isLoggedIn) navigateToLogin()
                        else {
                            navController.navigate(Screen.MyPublishedServices.createRoute()) {
                                launchSingleTop = true
                            }
                        }
                    }
                )
                Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(start = 56.dp))

                ProfileItem(
                    label = "我的接单",
                    icon = Icons.Default.Bookmark,
                    showArrow = true,
                    iconColor = profileListIconColor,
                    labelFontSize = 14.sp,
                    onClick = {
                        if (!isLoggedIn) navigateToLogin()
                        else {
                            navController.navigate(Screen.MyHostIncomingOrders.route) {
                                launchSingleTop = true
                            }
                        }
                    }
                )
                Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(start = 56.dp))

                ProfileItem(
                    label = "我的收入",
                    icon = Icons.Default.AttachMoney,
                    showArrow = true,
                    iconColor = profileListIconColor,
                    labelFontSize = 14.sp,
                    onClick = {
                        if (!isLoggedIn) navigateToLogin()
                        else {
                            navController.navigate(Screen.MyIncome.route) {
                                launchSingleTop = true
                            }
                        }
                    },
                )
                Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(start = 56.dp))

                ProfileItem(
                    label = "评价我的",
                    icon = Icons.Default.Star,
                    showArrow = true,
                    iconColor = profileListIconColor,
                    labelFontSize = 14.sp,
                    onClick = { if (!isLoggedIn) navigateToLogin() }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Group 3: 权限设置
            ProfileGroup(
                backgroundColor = cardColor,
                shape = RoundedCornerShape(12.dp),
                elevation = 0.dp,
                horizontalOuterPadding = ProfileMinePageCardHorizontalInset
            ) {
                ProfileItem(
                    label = "权限设置",
                    icon = Icons.Default.Settings,
                    showArrow = true,
                    iconColor = profileListIconColor,
                    labelFontSize = 14.sp,
                    showBadge = hasPendingPermissions,
                    onClick = {
                        if (!isLoggedIn) navigateToLogin()
                        else navController.navigate(Screen.SystemPermission.route)
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Group 4: Logout
            if (isLoggedIn) {
                ProfileGroup(
                    backgroundColor = cardColor,
                    shape = RoundedCornerShape(12.dp),
                    elevation = 0.dp,
                    horizontalOuterPadding = ProfileMinePageCardHorizontalInset
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showLogoutDialog = true }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "退出登录",
                            fontSize = 14.sp,
                            color = Color.Red,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            // 底部留白：大于 FAB 条高度 + bottom(64dp) + 系统手势区，避免最后几项被挡住
            Spacer(modifier = Modifier.height(200.dp))
        }
    }

    // --- Dialogs ---

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("提示") },
            text = { Text("确定要退出登录吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Save phone
                        val lastPhone = user.phoneNumber
                        val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                        if (lastPhone.isNotEmpty()) {
                            sharedPrefs.edit().putString("last_login_phone", lastPhone).apply()
                        }
                        
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            val database = com.example.Lulu.data.local.AppDatabase.getDatabase(context.applicationContext)
                            database.clearAllTables()
                            
                            listOf("time_ink_prefs", "wage_prefs", "focus_prefs", "menstrual_prefs", "sync_prefs").forEach { name ->
                                context.getSharedPreferences(name, android.content.Context.MODE_PRIVATE).edit().clear().apply()
                            }
                            context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE).edit()
                                .remove("unread_accepted_friends")
                                .apply()
                            
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                AppDataStore.logout()
                                showLogoutDialog = false
                            }
                        }
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showNameDialog) {
        EditDialog(
            title = "修改名字",
            initialValue = editValue,
            maxLength = 10,
            validator = { input ->
                when {
                    input.isEmpty() -> "名字不能为空"
                    else -> null
                }
            },
            onDismiss = { showNameDialog = false },
            onConfirm = { 
                AppDataStore.updateCurrentUser(user.copy(name = it))
                showNameDialog = false 
            }
        )
    }

    if (showPhoneDialog) {
        EditDialog(
            title = "修改手机号",
            initialValue = editValue,
            isPhone = true,
            onDismiss = { showPhoneDialog = false },
            onConfirm = { 
                // 修改手机号后，重置验证状态为 false
                AppDataStore.updateCurrentUser(user.copy(phoneNumber = it, isPhoneVerified = false))
                showPhoneDialog = false 
            }
        )
    }

    if (showVerifyPhoneDialog) {
        VerifyPhoneDialog(
            phoneNumber = user.phoneNumber,
            onDismiss = { showVerifyPhoneDialog = false },
            onConfirm = {
                AppDataStore.updateCurrentUser(user.copy(isPhoneVerified = true))
                showVerifyPhoneDialog = false
                Toast.makeText(context, "验证成功", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Region Picker for Profile
    if (showRegionDialog) {
        RegionPickerDialog(
            initialRegion = user.region,
            onDismiss = { showRegionDialog = false },
            onConfirm = { selectedRegion ->
                AppDataStore.updateCurrentUser(user.copy(region = selectedRegion))
                showRegionDialog = false
            }
        )
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressEditDialog(
    user: User,
    onDismiss: () -> Unit,
    onSave: (User) -> Unit
) {
    var region by remember { mutableStateOf(user.region) }
    var detail by remember { mutableStateOf(user.addressDetail) }
    var recipientName by remember { mutableStateOf(user.addressRecipientName) }
    var recipientPhone by remember { mutableStateOf(user.addressPhoneNumber) }
    
    var showRegionPicker by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val dismissKeyboard: () -> Unit = {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        Unit
    }

    Dialog(
        onDismissRequest = {
            dismissKeyboard()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("编辑地址", fontSize = 16.sp) },
                    navigationIcon = {
                        TextButton(onClick = {
                            dismissKeyboard()
                            onDismiss()
                        }) {
                            Text("取消", color = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    actions = {
                        TextButton(onClick = {
                            dismissKeyboard()
                            onSave(
                                user.copy(
                                    region = region,
                                    addressDetail = detail,
                                    addressRecipientName = recipientName,
                                    addressPhoneNumber = recipientPhone
                                )
                            )
                        }) {
                            Text("保存", color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            },
            containerColor = MaterialTheme.colorScheme.surface
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Region Selection
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = region,
                        onValueChange = {},
                        label = { Text("所在地区") },
                        modifier = Modifier
                            .fillMaxWidth(),
                        enabled = false, // Disable typing, force click
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        trailingIcon = {
                            Icon(Icons.Default.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                    // Overlay a clickable box because disabled textfield might not capture clicks well
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showRegionPicker = true }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Detail Address
                OutlinedTextField(
                    value = detail,
                    onValueChange = { detail = it },
                    label = { Text("详细地址 (街道、门牌号等)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                // Recipient Name
                OutlinedTextField(
                    value = recipientName,
                    onValueChange = { recipientName = it },
                    label = { Text("收货人姓名") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                // Recipient Phone
                OutlinedTextField(
                    value = recipientPhone,
                    onValueChange = { recipientPhone = it },
                    label = { Text("收货人手机号") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { dismissKeyboard() })
                )
            }
        }
        
        if (showRegionPicker) {
            RegionPickerDialog(
                initialRegion = region,
                onDismiss = { showRegionPicker = false },
                onConfirm = { 
                    region = it
                    showRegionPicker = false
                }
            )
        }
    }
}
