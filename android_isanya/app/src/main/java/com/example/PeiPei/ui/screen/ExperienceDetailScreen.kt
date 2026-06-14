// 文件说明：体验详情展示与操作界面（布局与服务详情页一致）。

package com.example.Lulu.ui.screen

import androidx.activity.compose.BackHandler
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.core.view.WindowCompat
import androidx.core.graphics.ColorUtils
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.example.Lulu.data.local.AppDataStore
import com.example.Lulu.data.remote.RetrofitClient
import com.example.Lulu.data.model.Service
import com.example.Lulu.data.model.ServiceDeclarations
import com.example.Lulu.ui.navigation.Screen
import com.example.Lulu.ui.viewmodel.PendingHostProfileHint
import com.example.Lulu.ui.theme.BrandPink
import com.example.Lulu.ui.theme.DialogTitleTopPadding
import com.example.Lulu.ui.components.AllReviewsSheetContent
import com.example.Lulu.ui.components.ProfileReviewsSection
import com.example.Lulu.ui.components.ZoomableFitAsyncImage
import com.example.Lulu.ui.components.hostProfileReviewsFromSummaries
import kotlinx.coroutines.launch
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.view.WindowManager
import com.example.Lulu.ui.util.findComposeDialogWindow
import com.example.Lulu.util.serviceDetailBottomPriceHeadline
import com.example.Lulu.util.textBundleForWeekdayParsing
import com.example.Lulu.util.BookingTimeRangesCodec
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private tailrec fun Context.findActivityForFullscreenImage(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivityForFullscreenImage()
    else -> null
}

private val ServiceDetailPriceSubtitleGray = Color(0xFF666666)
private val ServiceDetailActionButtonColor = Color(0xFFE0115F)

private val ServiceDetailHeroPanelOverlap = 28.dp
private val ServiceDetailHeroPanelTopCorner = 28.dp

private val InquiryComposeSubtitleGray = Color(0xFF666666)
private val InquiryComposeBorderGray = Color(0xFFE5E5E5)
private val InquiryComposePlaceholderGray = Color(0xFFBDBDBD)
private val InquiryComposeModifyButtonBg = Color(0xFFF2F2F2)
private val InquiryComposeModifyButtonText = Color(0xFF888888)

@Composable
private fun ServiceDetailBottomPrice(
    priceText: String,
    durationSubtitle: String,
    modifier: Modifier = Modifier
) {
    val headline = remember(priceText) { serviceDetailBottomPriceHeadline(priceText) }
    Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
        Text(
            text = headline,
            color = Color.Black,
            fontSize = 26.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                textDecoration = TextDecoration.Underline,
                fontFeatureSettings = "tnum,lnum"
            )
        )
        if (durationSubtitle.isNotBlank()) {
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = durationSubtitle,
                color = ServiceDetailPriceSubtitleGray,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


private fun syntheticServiceForExperienceInquiry(
    item: ExperienceItemUi,
    categoryTitle: String,
    priceText: String,
    priceBasisText: String,
): Service {
    val cat = categoryTitle.ifBlank { "体验" }
    return Service(
        id = item.id,
        title = item.title,
        description = "「$cat」${item.metaLine}。",
        priceText = priceText,
        priceBasisText = priceBasisText.ifBlank { item.metaLine },
    )
}

private fun experienceDetailImageUrls(item: ExperienceItemUi): List<String> {
    return listOf(item.imageUrl) +
        listOf(1, 2).map { n -> "https://picsum.photos/seed/${item.id}_g$n/400/520" }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExperienceDetailScreen(
    rootNavController: NavController,
    experienceId: String?,
    mainShellNavController: NavController? = null,
) {
    val popNav = mainShellNavController ?: rootNavController
    val view = LocalView.current
    val cleanId = experienceId?.trim()
    val item = remember(cleanId) { cleanId?.let { experienceItemFromStableId(it) } }
    val currentUser by AppDataStore.currentUser.collectAsState()
    val isLoggedIn = currentUser.id.isNotEmpty()
    val contacts by AppDataStore.contacts.collectAsState()
    val chatRepository = AppDataStore.getRepository()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(chatRepository) {
        chatRepository?.wishlistRemoteSyncFailures?.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }
    var fullscreenImageIndex by remember { mutableIntStateOf(-1) }
    var isFavorite by remember { androidx.compose.runtime.mutableStateOf(false) }
    var isTogglingFavorite by remember { androidx.compose.runtime.mutableStateOf(false) }
    var showRemoveFavoriteConfirm by remember { androidx.compose.runtime.mutableStateOf(false) }
    var isHeroImageDark by remember { androidx.compose.runtime.mutableStateOf(true) }
    var showServiceInquirySheet by remember { androidx.compose.runtime.mutableStateOf(false) }
    var inquiryComposeMessage by remember { androidx.compose.runtime.mutableStateOf("") }
    var inquiryComposeDateMillis by remember { androidx.compose.runtime.mutableStateOf<Long?>(null) }
    var inquiryComposeGuestCount by remember { mutableIntStateOf(1) }
    var inquiryComposeLocation by remember { androidx.compose.runtime.mutableStateOf("") }
    var showInquiryDateSubpicker by remember { androidx.compose.runtime.mutableStateOf(false) }
    var showInquiryLocationDialog by remember { androidx.compose.runtime.mutableStateOf(false) }
    var inquiryLocationEditBuffer by remember { androidx.compose.runtime.mutableStateOf("") }
    var inquiryComposeSending by remember { androidx.compose.runtime.mutableStateOf(false) }
    var showAllExperienceReviewsSheet by remember { androidx.compose.runtime.mutableStateOf(false) }
    val allExperienceReviewsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun handleExperienceDetailBack() {
        popNav.popBackStack()
    }

    BackHandler(onBack = ::handleExperienceDetailBack)

    if (item == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("未找到体验详情")
        }
        return
    }

    val stableId = cleanId!!
    val categoryTitle = remember(stableId) { experienceCategoryTitleForStableId(stableId) }
    val imageUrls = remember(item) { experienceDetailImageUrls(item) }
    val creatorUser = remember(contacts, currentUser) {
        contacts.firstOrNull { it.id.isNotEmpty() && it.id != currentUser.id }
            ?: contacts.firstOrNull()
    }
    val creatorName = creatorUser?.remarkName?.ifBlank { creatorUser.name }
        ?.ifBlank { creatorUser.id }
        ?: "当地体验官"
    val targetHostUserId = creatorUser?.id?.trim()?.ifBlank { null }
    val onOpenCreatorProfile: () -> Unit = {
        if (targetHostUserId != null) {
            PendingHostProfileHint.offer(targetHostUserId, creatorName)
            rootNavController.navigate(Screen.ServiceHostProfile.createRoute(targetHostUserId))
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("暂无体验官主页")
            }
        }
        Unit
    }
    val tagList = buildList {
        add(item.badgeLabel)
        item.metaLine.split("·").map { it.trim() }.filter { it.isNotEmpty() }.forEach { add(it) }
        if (categoryTitle.isNotBlank()) add(categoryTitle)
    }
    val descriptionText = remember(item, categoryTitle) {
        val cat = categoryTitle.ifBlank { "体验" }
        "「$cat」${item.metaLine}。更多动线说明、集合地点与退改规则请在预订后与体验官确认。"
    }
    val experiencePriceBasis = "每人起"
    var selectedExperienceTierIndex by remember(stableId) { mutableIntStateOf(0) }
    val experienceContentTiers = remember(item.priceFromYuan, experiencePriceBasis) {
        buildExperienceContentPickOptions(item.priceFromYuan, experiencePriceBasis)
    }
    LaunchedEffect(experienceContentTiers.size) {
        if (selectedExperienceTierIndex >= experienceContentTiers.size) {
            selectedExperienceTierIndex = 0
        }
    }
    val selectedExperienceTier = remember(experienceContentTiers, selectedExperienceTierIndex) {
        experienceContentTiers[selectedExperienceTierIndex.coerceIn(0, experienceContentTiers.lastIndex.coerceAtLeast(0))]
    }
    val hostReviewRows = remember(creatorUser?.id, creatorUser?.reviewSummaries) {
        hostProfileReviewsFromSummaries(creatorUser?.reviewSummaries.orEmpty())
    }
    val hostReviewCount = creatorUser?.reviewCount ?: 0
    val showHostReviewsOnDetail = hostReviewCount > 0 && hostReviewRows.isNotEmpty()
    val reviewThemeDark = isSystemInDarkTheme()
    val inquiryService = remember(item, categoryTitle, selectedExperienceTier) {
        syntheticServiceForExperienceInquiry(
            item,
            categoryTitle,
            selectedExperienceTier.priceText,
            selectedExperienceTier.priceBasisText,
        )
    }
    val experienceScheduleWeekdays = remember(
        inquiryService.id,
        inquiryService.title,
        inquiryService.description,
        inquiryService.priceText,
        inquiryService.priceBasisText,
    ) {
        parseAllowedWeekdays(inquiryService)
    }

    if (showRemoveFavoriteConfirm) {
        AlertDialog(
            onDismissRequest = { showRemoveFavoriteConfirm = false },
            title = { Text(text = "移出心愿单") },
            text = { Text(text = "是否移出心愿单？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRemoveFavoriteConfirm = false
                        scope.launch {
                            isTogglingFavorite = true
                            try {
                                isFavorite = false
                                snackbarHostState.showSnackbar("已移出心愿单")
                            } finally {
                                isTogglingFavorite = false
                            }
                        }
                    }
                ) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveFavoriteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
    if (showServiceInquirySheet) {
        ServiceInquiryComposeBottomSheet(
            creatorDisplayName = creatorName,
            messageText = inquiryComposeMessage,
            onMessageChange = { inquiryComposeMessage = it },
            selectedDateMillis = inquiryComposeDateMillis,
            onOpenDatePicker = { showInquiryDateSubpicker = true },
            guestCount = inquiryComposeGuestCount,
            onGuestCountChange = { inquiryComposeGuestCount = it },
            locationText = inquiryComposeLocation,
            onOpenLocationEditor = {
                inquiryLocationEditBuffer = inquiryComposeLocation
                showInquiryLocationDialog = true
            },
            sending = inquiryComposeSending,
            onDismiss = {
                if (!inquiryComposeSending) {
                    showServiceInquirySheet = false
                }
            },
            onSend = {
                when {
                    targetHostUserId == null -> scope.launch {
                        snackbarHostState.showSnackbar("暂未找到体验官信息")
                    }
                    chatRepository == null -> scope.launch {
                        snackbarHostState.showSnackbar("聊天服务未就绪，请稍后重试")
                    }
                    else -> {
                        val body = buildServiceInquiryOutboundMessage(
                            mainMessage = inquiryComposeMessage.trim(),
                            dateMillis = inquiryComposeDateMillis,
                            guests = "${inquiryComposeGuestCount}人",
                            location = inquiryComposeLocation.trim()
                        )
                        inquiryComposeSending = true
                        scope.launch {
                            val repo = chatRepository
                            val conversation = repo.getOrCreateDirectConversation(targetHostUserId)
                            if (conversation == null) {
                                snackbarHostState.showSnackbar("打开聊天失败，请稍后重试")
                                inquiryComposeSending = false
                                return@launch
                            }
                            val sent = repo.sendChatMessage(
                                conversationId = conversation.id,
                                content = body
                            )
                            if (sent == null) {
                                snackbarHostState.showSnackbar("消息发送失败，请稍后重试")
                                inquiryComposeSending = false
                                return@launch
                            }
                            val inquiryDateForCard = inquiryComposeDateMillis?.let(::formatInquiryDate)
                                ?: formatInquiryDate(System.currentTimeMillis())
                            inquiryComposeSending = false
                            showServiceInquirySheet = false
                            inquiryComposeMessage = ""
                            inquiryComposeDateMillis = null
                            inquiryComposeGuestCount = 1
                            inquiryComposeLocation = ""
                            rootNavController.navigate(
                                Screen.MessageThread.createRoute(
                                    threadId = conversation.id,
                                    inquiryDate = inquiryDateForCard,
                                    serviceTitle = item.title,
                                    contextExperienceId = item.id,
                                    peerUserId = targetHostUserId
                                )
                            )
                        }
                    }
                }
            }
        )
    }
    if (showInquiryDateSubpicker) {
        ServiceInquiryDatePickerDialog(
            service = inquiryService,
            priceUnitLabel = item.metaLine.ifBlank { "体验时长待沟通" },
            initialSelectedDateMillis = inquiryComposeDateMillis,
            onDismiss = { showInquiryDateSubpicker = false },
            onClearDate = {
                inquiryComposeDateMillis = null
                showInquiryDateSubpicker = false
            },
            onConfirm = { selectedDateMillis ->
                inquiryComposeDateMillis = selectedDateMillis
                showInquiryDateSubpicker = false
            }
        )
    }
    if (showInquiryLocationDialog) {
        AlertDialog(
            onDismissRequest = { showInquiryLocationDialog = false },
            title = { Text("地点") },
            text = {
                OutlinedTextField(
                    value = inquiryLocationEditBuffer,
                    onValueChange = { inquiryLocationEditBuffer = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("例如：亚龙湾集合点") },
                    singleLine = false,
                    minLines = 2
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        inquiryComposeLocation = inquiryLocationEditBuffer.trim()
                        showInquiryLocationDialog = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInquiryLocationDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (!view.isInEditMode) {
        DisposableEffect(Unit) {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            val previousStatusBarColor = window.statusBarColor
            val previousLightStatusBars = insetsController.isAppearanceLightStatusBars
            window.statusBarColor = Color.Transparent.toArgb()
            onDispose {
                window.statusBarColor = previousStatusBarColor
                insetsController.isAppearanceLightStatusBars = previousLightStatusBars
            }
        }
        LaunchedEffect(Unit) {
            val window = (view.context as Activity).window
            repeat(3) {
                withFrameNanos { }
                window.statusBarColor = Color.Transparent.toArgb()
            }
        }
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            window.statusBarColor = Color.Transparent.toArgb()
            insetsController.isAppearanceLightStatusBars = !isHeroImageDark
        }
    }

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 10.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ServiceDetailBottomPrice(
                        priceText = selectedExperienceTier.priceText,
                        durationSubtitle = selectedExperienceTier.durationDisplay,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (!isLoggedIn) {
                                rootNavController.navigate(Screen.Login.createRoute()) { launchSingleTop = true }
                                return@Button
                            }
                            scope.launch {
                                snackbarHostState.showSnackbar("预订功能即将上线")
                            }
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ServiceDetailActionButtonColor),
                        modifier = Modifier
                            .height(48.dp)
                            .width(140.dp)
                    ) {
                        Text("预订", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                Column(Modifier.fillMaxWidth()) {
                    ServiceHeroSection(
                        imageUrls = imageUrls,
                        contentDescription = item.title,
                        onImageClick = { pageIndex ->
                            fullscreenImageIndex = pageIndex
                        },
                        onBackClick = ::handleExperienceDetailBack,
                        onShareClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("分享功能即将上线")
                            }
                        },
                        onImageDarknessChange = { dark ->
                            isHeroImageDark = dark
                        },
                        onFavoriteClick = {
                            if (isTogglingFavorite) {
                                return@ServiceHeroSection
                            }
                            if (!isLoggedIn) {
                                rootNavController.navigate(Screen.Login.createRoute()) { launchSingleTop = true }
                                return@ServiceHeroSection
                            }
                            if (isFavorite) {
                                showRemoveFavoriteConfirm = true
                            } else {
                                scope.launch {
                                    if (isTogglingFavorite) return@launch
                                    isTogglingFavorite = true
                                    try {
                                        isFavorite = true
                                        snackbarHostState.showSnackbar("已加入心愿单")
                                    } finally {
                                        isTogglingFavorite = false
                                    }
                                }
                            }
                        },
                        isFavorite = isFavorite,
                        pagerBottomExtraInset = ServiceDetailHeroPanelOverlap
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = -ServiceDetailHeroPanelOverlap)
                            .clip(
                                RoundedCornerShape(
                                    topStart = ServiceDetailHeroPanelTopCorner,
                                    topEnd = ServiceDetailHeroPanelTopCorner
                                )
                            )
                            .background(Color.White)
                            .padding(horizontal = 16.dp)
                            .padding(top = 20.dp, bottom = 8.dp)
                    ) {
                        Text(
                            text = item.title.ifBlank { "未命名体验" },
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            fontSize = 24.sp,
                            lineHeight = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        if (tagList.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            DetailTagTwoColumnGrid(tags = tagList)
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.wrapContentSize()) {
                                if (!creatorUser?.photoUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = RetrofitClient.normalizeBackendMediaUrlForDisplay(creatorUser!!.photoUrl),
                                        contentDescription = "体验官头像",
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .clickable(onClick = onOpenCreatorProfile),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Surface(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clickable(onClick = onOpenCreatorProfile),
                                        shape = CircleShape,
                                        color = Color(0xFFF0F0F0)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = "体验官",
                                                tint = Color(0xFF8A8A8A)
                                            )
                                        }
                                    }
                                }
                                if (creatorUser?.identityVerified == true) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .background(color = BrandPink, shape = CircleShape)
                                            .padding(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.VerifiedUser,
                                            contentDescription = "身份已认证",
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(onClick = onOpenCreatorProfile)
                            ) {
                                Text(
                                    text = creatorName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Button(
                                onClick = {
                                    if (!isLoggedIn) {
                                        rootNavController.navigate(Screen.Login.createRoute()) { launchSingleTop = true }
                                        return@Button
                                    }
                                    inquiryComposeMessage = ""
                                    inquiryComposeDateMillis = null
                                    inquiryComposeGuestCount = 1
                                    inquiryComposeLocation = ""
                                    showServiceInquirySheet = true
                                },
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0x33000000))
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ChatBubbleOutline,
                                    contentDescription = null,
                                    tint = Color(0xFFE0115F),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("聊一聊", color = Color(0xFFE0115F), fontSize = 13.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            thickness = 1.dp,
                            color = DetailScreenTagDividerColor
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        ServiceDetailBookingPolicySections(
                            introSectionTitle = "体验介绍",
                            contentSectionTitle = "方案选择",
                            bookingSectionTitle = "预定须知",
                            fullDescription = descriptionText,
                            contentOptions = experienceContentTiers,
                            selectedContentIndex = selectedExperienceTierIndex,
                            onSelectedContentIndex = { selectedExperienceTierIndex = it },
                            scheduleMillis = inquiryComposeDateMillis,
                            onScheduleMillisChange = { inquiryComposeDateMillis = it },
                            scheduleAllowedWeekdays = experienceScheduleWeekdays,
                            scheduleBookingTimeRangesJson = inquiryService.bookingTimeRangesJson,
                            scheduleBookingLeadHours = inquiryService.bookingLeadHours,
                            scheduleBookingFutureOpenDays = inquiryService.bookingFutureOpenDays,
                            scheduleHostCalendarServiceId = inquiryService.id,
                            serviceDeclarationsSubtitle = ServiceDeclarations.declarationSummaryExtraCount(
                                emptyList(),
                            ),
                            serviceDeclarationsReaderBody = ServiceDeclarations.declarationReaderBody(
                                emptyList(),
                            ),
                            serviceAreaLocationRaw = null,
                        )
                        if (showHostReviewsOnDetail) {
                            ProfileReviewsSection(
                                sectionTitle = "${hostReviewCount}条评价",
                                reviews = hostReviewRows,
                                isDarkTheme = reviewThemeDark,
                                onShowMore = { showAllExperienceReviewsSheet = true },
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    if (showAllExperienceReviewsSheet && hostReviewRows.isNotEmpty()) {
        ModalBottomSheet(
            onDismissRequest = { showAllExperienceReviewsSheet = false },
            sheetState = allExperienceReviewsSheetState,
            containerColor = if (reviewThemeDark) Color(0xFF1E1E1E) else Color(0xFFFFFFFF),
            dragHandle = null,
        ) {
            AllReviewsSheetContent(
                reviews = hostReviewRows,
                isDarkTheme = reviewThemeDark,
                onClose = { showAllExperienceReviewsSheet = false },
            )
        }
    }

    if (fullscreenImageIndex >= 0 && imageUrls.isNotEmpty()) {
        FullscreenImageViewer(
            imageUrls = imageUrls,
            initialPage = fullscreenImageIndex.coerceIn(0, imageUrls.lastIndex),
            contentDescription = item.title,
            onDismiss = { fullscreenImageIndex = -1 }
        )
    }
}


@Composable
private fun ServiceHeroSection(
    imageUrls: List<String>,
    contentDescription: String,
    onImageClick: (Int) -> Unit,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit,
    onImageDarknessChange: (Boolean) -> Unit,
    onFavoriteClick: () -> Unit,
    isFavorite: Boolean,
    pagerBottomExtraInset: Dp = 0.dp,
) {
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
    val heroHeight = (screenHeightDp * 0.5f).coerceIn(380.dp, 520.dp)
    val actionContainerColor = Color.White.copy(alpha = 0.95f)
    val defaultActionIconTint = Color(0xFF222222)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(heroHeight)
    ) {
        if (imageUrls.isNotEmpty()) {
            ServiceImageCarousel(
                imageUrls = imageUrls,
                contentDescription = contentDescription,
                onImageClick = onImageClick,
                onCurrentImageDarknessResolved = onImageDarknessChange,
                pagerBottomExtraInset = pagerBottomExtraInset
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFEDEDED))
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderCircleActionButton(
                icon = Icons.Default.ArrowBack,
                contentDescription = "返回",
                onClick = onBackClick,
                tint = defaultActionIconTint,
                containerColor = actionContainerColor
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeaderCircleActionButton(
                    icon = Icons.Default.Share,
                    contentDescription = "分享",
                    onClick = onShareClick,
                    tint = defaultActionIconTint,
                    containerColor = actionContainerColor
                )
                HeaderCircleActionButton(
                    icon = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (isFavorite) "移出心愿单" else "加入心愿单",
                    onClick = onFavoriteClick,
                    tint = if (isFavorite) Color(0xFFFF5A5F) else defaultActionIconTint,
                    containerColor = actionContainerColor
                )
            }
        }
    }
}

@Composable
private fun HeaderCircleActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color = Color(0xFF222222),
    containerColor: Color = Color.White.copy(alpha = 0.92f),
    buttonSize: Dp = 40.dp,
    iconSize: Dp = 24.dp,
) {
    Surface(
        shape = CircleShape,
        color = containerColor
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(buttonSize)) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
private fun ServiceImageCarousel(
    imageUrls: List<String>,
    contentDescription: String,
    onImageClick: (Int) -> Unit,
    onCurrentImageDarknessResolved: (Boolean) -> Unit,
    pagerBottomExtraInset: Dp = 0.dp,
) {
    val pagerState = rememberPagerState(pageCount = { imageUrls.size })

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxSize()
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val painter = rememberAsyncImagePainter(model = imageUrls[page])
            val painterState = painter.state
            if (page == pagerState.currentPage) {
                LaunchedEffect(painterState) {
                    val successState = painterState as? AsyncImagePainter.State.Success ?: return@LaunchedEffect
                    onCurrentImageDarknessResolved(isImageDrawableDark(successState.result.drawable))
                }
            }
            Image(
                painter = painter,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onImageClick(page) }
            )
        }

        Text(
            text = "${pagerState.currentPage + 1} / ${imageUrls.size}",
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 14.dp + pagerBottomExtraInset, end = 14.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0x99000000))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp + pagerBottomExtraInset),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(imageUrls.size) { index ->
                val selected = index == pagerState.currentPage
                Box(
                    modifier = Modifier
                        .size(if (selected) 7.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (selected) Color(0xFFFF3B30) else Color.White.copy(alpha = 0.7f))
                        .border(
                            width = if (selected) 0.dp else 0.5.dp,
                            color = Color.White.copy(alpha = 0.9f),
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

private fun isImageDrawableDark(drawable: Drawable): Boolean {
    val bitmap = drawable.toSoftwareBitmap(sampleSize = 24)
    var luminanceSum = 0.0
    var sampleCount = 0
    val stepX = (bitmap.width / 6).coerceAtLeast(1)
    val stepY = (bitmap.height / 6).coerceAtLeast(1)
    for (y in 0 until bitmap.height step stepY) {
        for (x in 0 until bitmap.width step stepX) {
            luminanceSum += ColorUtils.calculateLuminance(bitmap.getPixel(x, y))
            sampleCount++
        }
    }
    if (sampleCount == 0) return true
    val averageLuminance = luminanceSum / sampleCount
    return averageLuminance < 0.5
}

private fun Drawable.toSoftwareBitmap(sampleSize: Int): Bitmap {
    val targetSize = sampleSize.coerceAtLeast(1)

    if (this is BitmapDrawable) {
        val source = bitmap
        val softwareBitmap = if (source.config == Bitmap.Config.HARDWARE) {
            source.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            source
        }
        if (softwareBitmap.width == targetSize && softwareBitmap.height == targetSize) {
            return softwareBitmap
        }
        return Bitmap.createScaledBitmap(softwareBitmap, targetSize, targetSize, true)
    }

    val width = if (intrinsicWidth > 0) intrinsicWidth else targetSize
    val height = if (intrinsicHeight > 0) intrinsicHeight else targetSize
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, width, height)
    draw(canvas)
    if (width == targetSize && height == targetSize) {
        return bitmap
    }
    return Bitmap.createScaledBitmap(bitmap, targetSize, targetSize, true)
}

private fun formatInquiryDate(timestampMillis: Long): String {
    val c = Calendar.getInstance().apply { timeInMillis = timestampMillis }
    return "${c.get(Calendar.MONTH) + 1}月${c.get(Calendar.DAY_OF_MONTH)}日${c.get(Calendar.HOUR_OF_DAY)}点"
}

private fun buildServiceInquiryOutboundMessage(
    mainMessage: String,
    dateMillis: Long?,
    guests: String,
    location: String
): String {
    val extras = buildList {
        if (dateMillis != null) add("时间：${formatInquiryDate(dateMillis)}")
        if (guests.isNotBlank()) add("人员：$guests")
        if (location.isNotBlank()) add("地点：$location")
    }
    if (extras.isEmpty()) return mainMessage
    return buildString {
        append(mainMessage.trim())
        append("\n\n")
        append("【预订信息】")
        for (line in extras) {
            append('\n')
            append(line)
        }
    }
}

@Composable
private fun ServiceInquiryComposeBottomSheet(
    creatorDisplayName: String,
    messageText: String,
    onMessageChange: (String) -> Unit,
    selectedDateMillis: Long?,
    onOpenDatePicker: () -> Unit,
    guestCount: Int,
    onGuestCountChange: (Int) -> Unit,
    locationText: String,
    onOpenLocationEditor: () -> Unit,
    sending: Boolean,
    onDismiss: () -> Unit,
    onSend: () -> Unit
) {
    val timeSubtitle = selectedDateMillis?.let(::formatInquiryDate) ?: "选择日期"
    val locationSubtitle = locationText.ifBlank { "添加地点" }
    val charCount = messageText.length
    val messageValid = messageText.trim().isNotEmpty()
    val canClickSend = messageValid && !sending
    val scroll = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x33000000))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .clickable(enabled = false) { }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(start = 20.dp, end = 12.dp, top = DialogTitleTopPadding, bottom = 12.dp)
                        .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "给${creatorDisplayName}写消息",
                                color = Color.Black,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            enabled = !sending
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "关闭",
                                tint = Color(0xFF222222)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(scroll)
                    ) {
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = onMessageChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 132.dp),
                            minLines = 5,
                            maxLines = 12,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = InquiryComposeBorderGray,
                                unfocusedBorderColor = InquiryComposeBorderGray,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            placeholder = {
                                Text(
                                    text = "示例：您好！我打算举办一场生日聚会，一共5个人，想知道您在1月17日这个周末是否可以提供服务。",
                                    color = InquiryComposePlaceholderGray,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                )
                            },
                            textStyle = TextStyle(
                                color = Color.Black,
                                fontSize = 15.sp,
                                lineHeight = 22.sp
                            ),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "已输入 $charCount 个字符",
                            color = InquiryComposeSubtitleGray,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "添加选填信息",
                            color = Color.Black,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, InquiryComposeBorderGray, RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            InquiryOptionalRow(
                                title = "时间",
                                subtitle = timeSubtitle,
                                onModify = onOpenDatePicker
                            )
                            Divider(color = InquiryComposeBorderGray, thickness = 1.dp)
                            InquiryGuestCountRow(
                                count = guestCount,
                                onCountChange = onGuestCountChange,
                                enabled = !sending
                            )
                            Divider(color = InquiryComposeBorderGray, thickness = 1.dp)
                            InquiryOptionalRow(
                                title = "地点",
                                subtitle = locationSubtitle,
                                onModify = onOpenLocationEditor
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 8.dp)
                            .height(50.dp)
                            .then(
                                if (canClickSend) {
                                    Modifier.clickable(onClick = onSend)
                                } else {
                                    Modifier
                                }
                            ),
                        shape = RoundedCornerShape(25.dp),
                        color = when {
                            sending || messageValid -> Color(0xFF111111)
                            else -> Color(0xFFF0F0F0)
                        }
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (sending) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "发送消息",
                                    color = if (messageValid) Color.White else Color(0xFFC8C8C8),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
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
private fun InquiryOptionalRow(
    title: String,
    subtitle: String,
    onModify: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.Black,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = InquiryComposeSubtitleGray,
                fontSize = 13.sp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Surface(
            modifier = Modifier.clickable(onClick = onModify),
            shape = RoundedCornerShape(8.dp),
            color = InquiryComposeModifyButtonBg
        ) {
            Text(
                text = "修改",
                color = InquiryComposeModifyButtonText,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun InquiryGuestCountRow(
    count: Int,
    onCountChange: (Int) -> Unit,
    enabled: Boolean
) {
    val minCount = 1
    val maxCount = 99
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "人员",
                color = Color.Black,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "人数",
                color = InquiryComposeSubtitleGray,
                fontSize = 13.sp
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val canDec = enabled && count > minCount
            val canInc = enabled && count < maxCount
            Surface(
                modifier = Modifier
                    .size(36.dp)
                    .then(if (canDec) Modifier.clickable { onCountChange(count - 1) } else Modifier),
                shape = RoundedCornerShape(8.dp),
                color = InquiryComposeModifyButtonBg
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Remove,
                        contentDescription = "减少人数",
                        tint = if (canDec) InquiryComposeModifyButtonText else Color(0xFFCCCCCC),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Text(
                text = "$count",
                color = Color.Black,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.widthIn(min = 28.dp),
                textAlign = TextAlign.Center
            )
            Surface(
                modifier = Modifier
                    .size(36.dp)
                    .then(if (canInc) Modifier.clickable { onCountChange(count + 1) } else Modifier),
                shape = RoundedCornerShape(8.dp),
                color = InquiryComposeModifyButtonBg
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "增加人数",
                        tint = if (canInc) InquiryComposeModifyButtonText else Color(0xFFCCCCCC),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private fun floorToDayStartMillis(timestampMillis: Long): Long {
    return Calendar.getInstance().run {
        timeInMillis = timestampMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        timeInMillis
    }
}

private fun parseAllowedWeekdays(service: com.example.Lulu.data.model.Service): Set<Int>? {
    val text = textBundleForWeekdayParsing(
        service.title,
        service.description,
        service.priceText,
        service.priceBasisText,
    ).lowercase(Locale.getDefault())
    if (text.contains("周末")) {
        return setOf(Calendar.SATURDAY, Calendar.SUNDAY)
    }
    if (text.contains("工作日") || text.contains("周一到周五") || text.contains("周内")) {
        return setOf(
            Calendar.MONDAY,
            Calendar.TUESDAY,
            Calendar.WEDNESDAY,
            Calendar.THURSDAY,
            Calendar.FRIDAY
        )
    }
    val weekdayMap = mapOf(
        "周一" to Calendar.MONDAY,
        "周二" to Calendar.TUESDAY,
        "周三" to Calendar.WEDNESDAY,
        "周四" to Calendar.THURSDAY,
        "周五" to Calendar.FRIDAY,
        "周六" to Calendar.SATURDAY,
        "周日" to Calendar.SUNDAY,
        "周天" to Calendar.SUNDAY
    )
    val matched = weekdayMap
        .filterKeys { text.contains(it) }
        .values
        .toSet()
    return matched.ifEmpty { null }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServiceInquiryDatePickerDialog(
    service: com.example.Lulu.data.model.Service,
    priceUnitLabel: String,
    initialSelectedDateMillis: Long?,
    onDismiss: () -> Unit,
    onClearDate: () -> Unit,
    onConfirm: (Long?) -> Unit
) {
    val todayStart = remember { floorToDayStartMillis(System.currentTimeMillis()) }
    val maxDaySpan = BookingTimeRangesCodec.normalizeBookingFutureOpenDays(service.bookingFutureOpenDays)
    val maxBookableDate = remember(todayStart, maxDaySpan) {
        Calendar.getInstance().run {
            timeInMillis = todayStart
            add(Calendar.DAY_OF_YEAR, (maxDaySpan - 1).coerceAtLeast(0))
            timeInMillis
        }
    }
    val closureRoot by AppDataStore.hostCalendarDayClosures.collectAsState()
    val closuresForService = remember(closureRoot, service.id) {
        closureRoot[service.id].orEmpty()
    }
    val allowedWeekdays = remember(
        service.id,
        service.title,
        service.description,
        service.priceText,
        service.priceBasisText,
    ) {
        parseAllowedWeekdays(service)
    }
    val pickerLocale = remember {
        Locale.forLanguageTag("zh-CN")
    }
    val selectableDates = remember(todayStart, maxBookableDate, allowedWeekdays, closuresForService) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val dayStart = floorToDayStartMillis(utcTimeMillis)
                if (dayStart < todayStart || dayStart > maxBookableDate) {
                    return false
                }
                val iso = java.time.Instant.ofEpochMilli(dayStart)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate()
                    .toString()
                if (closuresForService[iso]?.allDay == true) return false
                if (allowedWeekdays.isNullOrEmpty()) {
                    return true
                }
                val dayOfWeek = Calendar.getInstance().run {
                    timeInMillis = dayStart
                    get(Calendar.DAY_OF_WEEK)
                }
                return dayOfWeek in allowedWeekdays
            }
        }
    }
    val datePickerState = remember(pickerLocale, initialSelectedDateMillis, selectableDates) {
        DatePickerState(
            locale = pickerLocale,
            initialSelectedDateMillis = initialSelectedDateMillis,
            selectableDates = selectableDates
        )
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x33000000)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.88f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 12.dp, end = 12.dp, top = DialogTitleTopPadding, bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "选择日期",
                            color = Color(0xFF111111),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "关闭",
                                tint = Color(0xFF222222)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Surface(
                            color = Color(0xFF1D1D1D),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = priceUnitLabel,
                                color = Color.White,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    DatePicker(
                        state = datePickerState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        colors = DatePickerDefaults.colors(
                            selectedDayContainerColor = Color(0xFF111111),
                            selectedDayContentColor = Color.White,
                            selectedYearContainerColor = Color(0xFF111111),
                            selectedYearContentColor = Color.White
                        ),
                        title = {},
                        headline = {},
                        showModeToggle = false
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp, top = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(0xFFF7F7F7),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.clickable {
                                onClearDate()
                                onDismiss()
                            }
                        ) {
                            Text(
                                text = "清除日期",
                                color = Color(0xFF8A8A8A),
                                fontSize = 17.sp,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                            )
                        }
                        val canSave = datePickerState.selectedDateMillis != null
                        Surface(
                            color = if (canSave) Color(0xFF111111) else Color(0xFFF1F1F1),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.then(
                                if (canSave) Modifier.clickable { onConfirm(datePickerState.selectedDateMillis) }
                                else Modifier
                            )
                        ) {
                            Text(
                                text = "确定",
                                color = if (canSave) Color.White else Color(0xFFBFBFBF),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold
                                ,
                                modifier = Modifier.padding(horizontal = 28.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FullscreenImageViewer(
    imageUrls: List<String>,
    initialPage: Int,
    contentDescription: String,
    onDismiss: () -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { imageUrls.size }
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        val hostView = LocalView.current
        if (!hostView.isInEditMode) {
            DisposableEffect(hostView) {
                val activity = hostView.context.findActivityForFullscreenImage()
                val activityWindow = activity?.window
                val dialogWindow = hostView.findComposeDialogWindow()
                val windowsToStyle = buildList {
                    activityWindow?.let { add(it to activityWindow.decorView) }
                    if (dialogWindow != null && dialogWindow !== activityWindow) {
                        add(dialogWindow to hostView)
                    }
                }
                if (windowsToStyle.isEmpty()) return@DisposableEffect onDispose { }

                data class Saved(
                    val window: android.view.Window,
                    val insetsAnchor: android.view.View,
                    val statusBarColor: Int,
                    val navigationBarColor: Int,
                    val lightStatusBars: Boolean,
                    val lightNavigationBars: Boolean,
                    val windowFlags: Int,
                )
                val saved = windowsToStyle.map { (w, anchor) ->
                    val c = WindowCompat.getInsetsController(w, anchor)
                    Saved(
                        window = w,
                        insetsAnchor = anchor,
                        statusBarColor = w.statusBarColor,
                        navigationBarColor = w.navigationBarColor,
                        lightStatusBars = c.isAppearanceLightStatusBars,
                        lightNavigationBars = c.isAppearanceLightNavigationBars,
                        windowFlags = w.attributes.flags,
                    )
                }
                for ((w, anchor) in windowsToStyle) {
                    w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                    w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                    w.statusBarColor = Color.Black.toArgb()
                    w.navigationBarColor = Color.Black.toArgb()
                    val c = WindowCompat.getInsetsController(w, anchor)
                    c.isAppearanceLightStatusBars = false
                    c.isAppearanceLightNavigationBars = false
                }
                onDispose {
                    for (s in saved) {
                        s.window.attributes = s.window.attributes.apply { flags = s.windowFlags }
                        s.window.statusBarColor = s.statusBarColor
                        s.window.navigationBarColor = s.navigationBarColor
                        val c = WindowCompat.getInsetsController(s.window, s.insetsAnchor)
                        c.isAppearanceLightStatusBars = s.lightStatusBars
                        c.isAppearanceLightNavigationBars = s.lightNavigationBars
                    }
                }
            }
            SideEffect {
                val activity = hostView.context.findActivityForFullscreenImage()
                val activityWindow = activity?.window
                val dialogWindow = hostView.findComposeDialogWindow()
                val windowsToStyle = buildList {
                    activityWindow?.let { add(it to activityWindow.decorView) }
                    if (dialogWindow != null && dialogWindow !== activityWindow) {
                        add(dialogWindow to hostView)
                    }
                }
                for ((w, anchor) in windowsToStyle) {
                    w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                    w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                    w.statusBarColor = Color.Black.toArgb()
                    w.navigationBarColor = Color.Black.toArgb()
                    val c = WindowCompat.getInsetsController(w, anchor)
                    c.isAppearanceLightStatusBars = false
                    c.isAppearanceLightNavigationBars = false
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                ZoomableFitAsyncImage(
                    model = imageUrls[page],
                    contentDescription = contentDescription,
                    embedInHorizontalPager = true,
                )
            }

            Text(
                text = "${pagerState.currentPage + 1}/${imageUrls.size}",
                color = Color.White,
                fontSize = 13.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 28.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0x66000000))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}
