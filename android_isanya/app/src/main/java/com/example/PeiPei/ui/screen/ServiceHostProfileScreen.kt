// 文件说明：查看他人服务主主页的界面。

package com.example.Lulu.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import com.example.Lulu.data.local.AppDataStore
import com.example.Lulu.data.remote.RetrofitClient
import com.example.Lulu.data.model.Service
import com.example.Lulu.data.model.User
import com.example.Lulu.data.model.formatWeightKgForDisplay
import com.example.Lulu.ui.components.AllReviewsSheetContent
import com.example.Lulu.ui.components.AvatarIdentityShieldOverlay
import com.example.Lulu.ui.components.FullScreenImageDialog
import com.example.Lulu.ui.components.ProfileReviewsSection
import com.example.Lulu.ui.components.hostProfileReviewsFromSummaries
import com.example.Lulu.ui.navigation.Screen
import com.example.Lulu.ui.theme.BrandPink
import com.example.Lulu.ui.viewmodel.ServiceHostProfileViewModel

/** 主资料详情页 LazyColumn 与底栏按钮相对屏幕左右的留白（与「我的」页卡片边距对齐） */
private val ServiceHostProfileHorizontalInset = 24.dp
private val ProfilePhotoWallImageSpacing = 10.dp

private fun thirdPersonPronoun(gender: String): String = when (gender.trim()) {
    "女" -> "她"
    "男" -> "他"
    else -> "她（他）"
}

private fun profileAboutTitle(isSelfProfile: Boolean, gender: String): String =
    if (isSelfProfile) "关于我" else "关于${thirdPersonPronoun(gender)}"

private fun profilePublishedServicesTitle(isSelfProfile: Boolean, gender: String): String =
    if (isSelfProfile) "我发布的服务" else "${thirdPersonPronoun(gender)}发布的服务"

private fun profileReviewsTitle(isSelfProfile: Boolean, gender: String): String =
    if (isSelfProfile) "我的评价" else "${thirdPersonPronoun(gender)}的评价"

private fun platformYearsOnLulu(createdAt: Long): String {
    val elapsed = System.currentTimeMillis() - createdAt
    if (elapsed <= 0L) return "不足1年"
    val years = (elapsed / (365.25 * 24 * 60 * 60 * 1000)).toInt()
    return if (years <= 0) "不足1年" else "${years}年"
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalLayoutApi::class
)
@Composable
fun ServiceHostProfileScreen(
    navController: NavController,
    userId: String?,
    viewModel: ServiceHostProfileViewModel = viewModel()
) {
    LaunchedEffect(userId) {
        if (userId != null) {
            viewModel.loadUser(userId)
        }
    }

    val userState by viewModel.user.collectAsState()
    val publishedServices by viewModel.publishedServices.collectAsState()
    val loadError by viewModel.loadError.collectAsState()
    val currentUser by AppDataStore.currentUser.collectAsState()
    val isSelfProfile = !userId.isNullOrEmpty() && userId == currentUser.id

    if (userState == null) {
        val placeholderText = when {
            userId.isNullOrBlank() -> "缺少用户信息"
            else -> loadError ?: "正在加载..."
        }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = placeholderText,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
        return
    }

    val user = userState!!
    var fullScreenImageUrl by remember { mutableStateOf<String?>(null) }

    val isDarkTheme = isSystemInDarkTheme()
    val pageColor = if (isDarkTheme) Color(0xFF111111) else Color(0xFFFFFFFF)
    val cardColor = if (isDarkTheme) Color(0xFF222222) else Color(0xFFF8F8F8)
    val reviewData = remember(user.reviewSummaries) {
        hostProfileReviewsFromSummaries(user.reviewSummaries)
    }

    var showAllReviewsSheet by remember { mutableStateOf(false) }
    val allReviewsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (!fullScreenImageUrl.isNullOrEmpty()) {
        FullScreenImageDialog(
            imageUrl = fullScreenImageUrl.orEmpty(),
            onDismiss = { fullScreenImageUrl = null }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
        containerColor = pageColor,
        bottomBar = {
            if (!isSelfProfile && !userId.isNullOrBlank()) {
                ServiceHostProfileChatBottomBar(
                    peerId = userId,
                    navController = navController,
                    pageColor = pageColor,
                    isDarkTheme = isDarkTheme
                )
            }
        },
        topBar = {
            CenterAlignedTopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (isSelfProfile) {
                        TextButton(
                            onClick = {
                                navController.navigate(Screen.MyProfileEdit.route) {
                                    launchSingleTop = true
                                }
                            }
                        ) {
                            Text("编辑")
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = pageColor
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            // 顶栏与首卡之间留出空隙，避免上沿/阴影被顶栏压住
            contentPadding = PaddingValues(
                start = ServiceHostProfileHorizontalInset,
                end = ServiceHostProfileHorizontalInset,
                top = 20.dp,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ProfileHeroCard(
                    user = user,
                    cardColor = if (isDarkTheme) cardColor else Color(0xFFFFFFFF),
                    isDarkTheme = isDarkTheme,
                    onAvatarClick = {
                        if (user.photoUrl.isNotEmpty()) {
                            fullScreenImageUrl = user.photoUrl
                        }
                    }
                )
            }

            item {
                DetailInfoSection(
                    user = user,
                    cardColor = if (isDarkTheme) cardColor else Color(0xFFFFFFFF),
                    isDarkTheme = isDarkTheme,
                    hideEmptyFields = !isSelfProfile,
                    isSelfProfile = isSelfProfile,
                    onStartRealNameVerification = {
                        navController.navigate(Screen.RealNameVerification.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            item {
                PhotoWallSection(
                    photoUrls = user.profileImageUrls,
                    isSelfProfile = isSelfProfile,
                    displayName = user.name.ifEmpty { "Ta" },
                    isDarkTheme = isDarkTheme,
                    onPhotoClick = { photoUrl -> fullScreenImageUrl = photoUrl }
                )
            }

            item {
                PublishedServicesSection(
                    displayName = user.name.ifEmpty { "Ta" },
                    gender = user.gender,
                    services = publishedServices,
                    averageRating = user.averageRating,
                    reviewCount = user.reviewCount,
                    isSelfProfile = isSelfProfile,
                    isDarkTheme = isDarkTheme,
                    onServiceClick = { serviceId ->
                        navController.navigate(Screen.ServiceDetail.createRoute(serviceId)) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            item {
                ProfileReviewsSection(
                    sectionTitle = profileReviewsTitle(isSelfProfile, user.gender),
                    reviews = reviewData,
                    isDarkTheme = isDarkTheme,
                    onShowMore = { showAllReviewsSheet = true },
                )
            }
        }
        }

        if (showAllReviewsSheet && reviewData.isNotEmpty()) {
            ModalBottomSheet(
                onDismissRequest = { showAllReviewsSheet = false },
                sheetState = allReviewsSheetState,
                containerColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color(0xFFFFFFFF),
                dragHandle = null
            ) {
                AllReviewsSheetContent(
                    reviews = reviewData,
                    isDarkTheme = isDarkTheme,
                    onClose = { showAllReviewsSheet = false }
                )
            }
        }
    }
}

@Composable
private fun ServiceHostProfileChatBottomBar(
    peerId: String,
    navController: NavController,
    pageColor: Color,
    isDarkTheme: Boolean
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val borderColor = if (isDarkTheme) Color.White.copy(alpha = 0.88f) else Color(0xFF111111)
    val contentColor = if (isDarkTheme) Color.White else Color(0xFF111111)

    Surface(color = pageColor) {
        OutlinedButton(
            onClick = {
                scope.launch {
                    val repo = AppDataStore.getRepository()
                    if (repo == null) {
                        Toast.makeText(context, "聊天服务未就绪，请稍后重试", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    val conversation = repo.getOrCreateDirectConversation(peerId)
                    if (conversation != null) {
                        navController.navigate(
                            Screen.MessageThread.createRoute(
                                threadId = conversation.id,
                                from = "host_profile",
                                peerUserId = peerId
                            )
                        ) {
                            launchSingleTop = true
                        }
                    } else {
                        Toast.makeText(context, "打开聊天失败，请稍后重试", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ServiceHostProfileHorizontalInset, vertical = 12.dp)
                .navigationBarsPadding()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, borderColor),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = contentColor
            ),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ChatBubbleOutline,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = contentColor
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "聊一聊",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PhotoWallSection(
    photoUrls: List<String>,
    isSelfProfile: Boolean,
    displayName: String,
    isDarkTheme: Boolean,
    onPhotoClick: (String) -> Unit
) {
    val titleColor = if (isDarkTheme) Color.White else Color(0xFF222222)
    val mutedColor = if (isDarkTheme) Color(0xFFA8A8A8) else Color(0xFF777777)
    val sectionCardColor = if (isDarkTheme) Color(0xFF1B1B1B) else Color(0xFFFFFFFF)
    val dividerColor = if (isDarkTheme) Color(0xFF2D2D2D) else Color(0xFFE8E8E8)
    val emptyText = if (isSelfProfile) {
        "还没有上传照片，去编辑资料添加几张更容易让别人了解你。"
    } else {
        "${displayName} 还没有公开照片。"
    }

    Column {
        Divider(
            modifier = Modifier.padding(bottom = 12.dp),
            thickness = 1.dp,
            color = dividerColor
        )
        Text(
            text = "照片墙",
            color = titleColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        if (photoUrls.isEmpty()) {
            Text(
                text = emptyText,
                color = mutedColor,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        } else {
            Surface(
                color = sectionCardColor,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = if (isDarkTheme) 1.dp else 0.dp,
                tonalElevation = 0.dp
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    val itemSize = (maxWidth - ProfilePhotoWallImageSpacing * 2) / 3
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(ProfilePhotoWallImageSpacing),
                        verticalArrangement = Arrangement.spacedBy(ProfilePhotoWallImageSpacing),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        photoUrls.forEach { photoUrl ->
                            Surface(
                                modifier = Modifier
                                    .size(itemSize)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onPhotoClick(photoUrl) },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isDarkTheme) Color(0xFF2A2A2A) else Color(0xFFE8E8E8)
                            ) {
                                AsyncImage(
                                    model = RetrofitClient.normalizeBackendMediaUrlForDisplay(photoUrl),
                                    contentDescription = "资料照片",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileHeroCard(
    user: User,
    cardColor: Color,
    isDarkTheme: Boolean,
    onAvatarClick: () -> Unit
) {
    val textPrimary = if (isDarkTheme) Color.White else Color(0xFF202020)
    val textSecondary = if (isDarkTheme) Color(0xFFCCCCCC) else Color(0xFF666666)
    val statDivider = if (isDarkTheme) Color(0xFF2E2E2E) else Color(0xFFECECEC)
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = cardColor,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 10.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 6.dp, end = 26.dp, top = 40.dp, bottom = 30.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(0.6f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.padding(
                        bottom = if (user.identityVerified) 8.dp else 0.dp
                    )
                ) {
                            Surface(
                                modifier = Modifier
                                    .size(106.dp)
                                    .clip(CircleShape)
                            .clickable(enabled = user.photoUrl.isNotEmpty(), onClick = onAvatarClick),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        if (user.photoUrl.isNotEmpty()) {
                            AsyncImage(
                                model = RetrofitClient.normalizeBackendMediaUrlForDisplay(user.photoUrl),
                                contentDescription = "头像",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Text(
                                            text = user.name.take(1).ifEmpty { "?" },
                                            fontSize = 24.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.Bold
                                        )
                            }
                        }
                    }
                    AvatarIdentityShieldOverlay(
                        verified = user.identityVerified,
                        shieldSize = 23.dp,
                        markSize = 10.dp,
                        modifier = Modifier.align(Alignment.BottomEnd)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = user.name.ifEmpty { "Carole" },
                    color = textPrimary,
                    fontSize = 21.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = regionSubtitle,
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
                    value = user.peiPeiId.ifEmpty { "未设置" },
                    valueColor = textPrimary,
                    labelColor = textSecondary
                )
                HorizontalDivider(thickness = 1.dp, color = statDivider)
                MyProfileStatRow(
                    label = "性别",
                    value = user.gender.ifEmpty { "未设置" },
                    valueColor = textPrimary,
                    labelColor = textSecondary
                )
                HorizontalDivider(thickness = 1.dp, color = statDivider)
                MyProfileStatRow(
                    label = "加入 爱野",
                    value = platformYearsOnLulu(user.createdAt),
                    valueColor = textPrimary,
                    labelColor = textSecondary
                )
            }
        }
    }
}

/** 与 UserInfoScreen 资料卡右侧三联行一致 */
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

@Composable
private fun DetailInfoSection(
    user: User,
    cardColor: Color,
    isDarkTheme: Boolean,
    hideEmptyFields: Boolean = false,
    isSelfProfile: Boolean = false,
    onStartRealNameVerification: () -> Unit = {}
) {
    val textColor = if (isDarkTheme) Color.White else Color(0xFF222222)
    val mutedColor = if (isDarkTheme) Color(0xFFA8A8A8) else Color(0xFF777777)
    val verifiedAccent = BrandPink
    val unverifiedGray = Color(0xFF9E9E9E)
    val interestsText = user.tags.joinToString("、").ifEmpty { "未填写" }
    val aboutText = user.signature.ifBlank { "未填写" }
    val identityLabelColor = if (user.identityVerified) verifiedAccent else unverifiedGray
    val identityRowInteractive = isSelfProfile && !user.identityVerified
    val identityTitle = when {
        user.identityVerified && isSelfProfile -> "实名认证已通过"
        user.identityVerified -> "身份已认证"
        isSelfProfile -> "开始实名认证"
        else -> "身份未认证"
    }

    val showAbout = !hideEmptyFields || user.signature.isNotBlank()
    val showHobbiesLine = !hideEmptyFields || user.tags.isNotEmpty()
    val showWorkLine = !hideEmptyFields || user.jobTitle.isNotBlank()
    val showEducationLine = !hideEmptyFields || user.education.isNotBlank()
    val showBirthLine = !hideEmptyFields || user.birthDecade.isNotBlank()
    val showSongLine = !hideEmptyFields || user.middleSchoolFavoriteSong.isNotBlank()
    val showHeightWeightLine = !hideEmptyFields || user.heightCm > 0 || user.weightKg > 0f ||
        (user.heightWeightPrivate && !isSelfProfile)
    val heightWeightDetailText = when {
        !isSelfProfile && user.heightWeightPrivate -> "已隐藏"
        user.heightCm <= 0 && user.weightKg <= 0f -> if (hideEmptyFields) "" else "未填写"
        isSelfProfile -> {
            val parts = buildList {
                if (user.heightCm > 0) add("${user.heightCm}cm")
                if (user.weightKg > 0f) add(formatWeightKgForDisplay(user.weightKg))
            }.joinToString(" · ")
            when {
                parts.isEmpty() -> if (hideEmptyFields) "" else "未填写"
                user.heightWeightPrivate -> "$parts（对他人隐藏）"
                else -> parts
            }
        }
        else -> buildList {
            if (user.heightCm > 0) add("${user.heightCm}cm")
            if (user.weightKg > 0f) add(formatWeightKgForDisplay(user.weightKg))
        }.joinToString(" · ").ifEmpty { "未填写" }
    }
    val hasAboutSection = showAbout || showHobbiesLine || showWorkLine || showEducationLine ||
        showBirthLine || showSongLine || showHeightWeightLine
    val hasOtherSubsection = showWorkLine || showEducationLine || showBirthLine || showSongLine || showHeightWeightLine
    var otherDetailsExpanded by rememberSaveable { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            if (!user.identityVerified) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                        .then(
                            if (identityRowInteractive) {
                                Modifier.clickable(onClick = onStartRealNameVerification)
                            } else {
                                Modifier
                            }
                        )
                        .padding(horizontal = 10.dp, vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = identityTitle,
                        color = identityLabelColor,
                        fontSize = if (isSelfProfile) 10.sp else 16.sp,
                        lineHeight = if (isSelfProfile) 12.sp else 20.sp,
                        fontWeight = if (isSelfProfile) FontWeight.Medium else FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        style = TextStyle(
                            textDecoration = if (identityRowInteractive) {
                                TextDecoration.Underline
                            } else {
                                TextDecoration.None
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (hasAboutSection) {
                Text(
                    text = profileAboutTitle(isSelfProfile, user.gender),
                    color = textColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 2.dp, vertical = 8.dp)
                )
                if (showAbout) {
                    DetailLine(
                        Icons.AutoMirrored.Outlined.Article,
                        "自我介绍：${if (hideEmptyFields) user.signature.trim() else aboutText}",
                        textColor,
                        maxLines = 4
                    )
                }
                if (showHobbiesLine) {
                    DetailLine(
                        Icons.Outlined.FavoriteBorder,
                        "兴趣爱好：${if (hideEmptyFields) user.tags.joinToString("、") else interestsText}",
                        textColor,
                        maxLines = 4
                    )
                }
                if (hasOtherSubsection) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 6.dp),
                        thickness = 1.dp,
                        color = if (isDarkTheme) Color(0xFF2E2E2E) else Color(0xFFE8E8E8)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { otherDetailsExpanded = !otherDetailsExpanded }
                            .padding(vertical = 4.dp, horizontal = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "其他资料",
                            color = mutedColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            imageVector = if (otherDetailsExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (otherDetailsExpanded) "收起" else "展开",
                            tint = mutedColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    AnimatedVisibility(
                        visible = otherDetailsExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            if (showWorkLine) {
                                DetailLine(
                                    Icons.Outlined.WorkOutline,
                                    "我的工作：${if (hideEmptyFields) user.jobTitle else user.jobTitle.ifEmpty { "未填写" }}",
                                    textColor
                                )
                            }
                            if (showEducationLine) {
                                DetailLine(
                                    Icons.Outlined.School,
                                    "曾就读于：${if (hideEmptyFields) user.education else user.education.ifEmpty { "未填写" }}",
                                    textColor
                                )
                            }
                            if (showBirthLine) {
                                DetailLine(
                                    Icons.Filled.Cake,
                                    "我出生的年代：${if (hideEmptyFields) user.birthDecade else user.birthDecade.ifEmpty { "未填写" }}",
                                    textColor
                                )
                            }
                            if (showHeightWeightLine) {
                                DetailLine(
                                    Icons.Outlined.FitnessCenter,
                                    "身高体重：$heightWeightDetailText",
                                    textColor
                                )
                            }
                            if (showSongLine) {
                                DetailLine(
                                    Icons.Filled.MusicNote,
                                    "中学时最喜欢的歌曲：${if (hideEmptyFields) user.middleSchoolFavoriteSong else user.middleSchoolFavoriteSong.ifEmpty { "未填写" }}",
                                    textColor,
                                    maxLines = 3
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailAboutMeBlock(
    body: String,
    textColor: Color,
    secondaryColor: Color,
    isSelfProfile: Boolean,
    gender: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.Article,
            contentDescription = null,
            tint = textColor.copy(alpha = 0.9f),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = profileAboutTitle(isSelfProfile, gender),
                color = textColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = body,
                color = if (body == "未填写") secondaryColor else textColor,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
private fun DetailLine(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color,
    maxLines: Int = 1
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = if (maxLines == 1) Alignment.CenterVertically else Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color.copy(alpha = 0.9f),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            color = color,
            fontSize = 16.sp,
            lineHeight = if (maxLines > 1) 22.sp else 20.sp,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PublishedServicesSection(
    displayName: String,
    gender: String,
    services: List<Service>,
    averageRating: Double,
    reviewCount: Int,
    isSelfProfile: Boolean,
    isDarkTheme: Boolean,
    onServiceClick: (String) -> Unit
) {
    val titleColor = if (isDarkTheme) Color.White else Color(0xFF222222)
    val mutedColor = if (isDarkTheme) Color(0xFFA8A8A8) else Color(0xFF777777)
    val config = LocalConfiguration.current
    val cardWidth = ((config.screenWidthDp - 48) * 0.46f).coerceAtLeast(136f).dp

    Column(modifier = Modifier.padding(top = 8.dp)) {
        Divider(
            modifier = Modifier.padding(bottom = 12.dp),
            thickness = 1.dp,
            color = if (isDarkTheme) Color(0xFF2D2D2D) else Color(0xFFE8E8E8)
        )
        Text(
            text = profilePublishedServicesTitle(isSelfProfile, gender),
            color = titleColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        if (services.isEmpty()) {
            Text(
                text = if (displayName.isBlank()) "暂无发布的服务" else "${displayName} 暂无发布的服务",
                color = mutedColor,
                fontSize = 14.sp
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(end = 8.dp)
            ) {
                items(services, key = { it.id }) { service ->
                    PublishedServiceCard(
                        service = service,
                        cardWidth = cardWidth,
                        averageRating = averageRating,
                        reviewCount = reviewCount,
                        isDarkTheme = isDarkTheme,
                        onClick = { onServiceClick(service.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PublishedServiceCard(
    service: Service,
    cardWidth: Dp,
    averageRating: Double,
    reviewCount: Int,
    isDarkTheme: Boolean,
    onClick: () -> Unit
) {
    val titleColor = if (isDarkTheme) Color.White else Color(0xFF222222)
    val categoryColor = if (isDarkTheme) Color(0xFFCCCCCC) else Color(0xFF444444)
    val metaColor = if (isDarkTheme) Color(0xFF9E9E9E) else Color(0xFF888888)

    Column(
        modifier = Modifier
            .width(cardWidth)
            .clickable(onClick = onClick)
    ) {
        val imageShape = RoundedCornerShape(14.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(imageShape)
                .background(if (isDarkTheme) Color(0xFF2A2A2A) else Color(0xFFE8E8E8))
        ) {
            if (service.coverImageUrl.isNotEmpty()) {
                AsyncImage(
                    model = service.coverImageUrl,
                    contentDescription = service.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = service.category.ifEmpty { "服务" },
            color = categoryColor,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = service.title.ifEmpty { service.description },
            color = titleColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 18.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = Color(0xFFFFC107),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = buildString {
                    append(String.format("%.1f", averageRating))
                    append(" · ")
                    append(reviewCount)
                    append(" 条评价")
                },
                color = metaColor,
                fontSize = 12.sp
            )
        }
    }
}
