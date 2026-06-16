// 文件说明：服务/商品详情展示与操作界面。

package com.example.Lulu.ui.screen

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
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
import com.example.Lulu.data.model.ServiceCategories
import com.example.Lulu.data.model.ServiceDeclarations
import com.example.Lulu.data.model.ServicePublishTaxonomy
import com.example.Lulu.data.repository.UserRepository
import com.example.Lulu.ui.navigation.Screen
import com.example.Lulu.ui.viewmodel.PendingHostProfileHint
import com.example.Lulu.ui.theme.BrandPink
import com.example.Lulu.ui.theme.BrandPinkSoft
import com.example.Lulu.ui.theme.DialogTitleTopPadding
import com.example.Lulu.ui.components.AllReviewsSheetContent
import com.example.Lulu.ui.components.ProfileReviewsSection
import com.example.Lulu.ui.components.PublishPriceSettingsBottomSheet
import com.example.Lulu.ui.components.ZoomableFitAsyncImage
import com.example.Lulu.ui.components.hostProfileReviewsFromSummaries
import kotlinx.coroutines.launch
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.view.WindowManager
import com.example.Lulu.ui.util.findComposeDialogWindow
import com.example.Lulu.util.BookingTimeRangesCodec
import com.example.Lulu.util.priceBasisTextForUiDisplay
import com.example.Lulu.util.serviceDetailBottomPriceHeadline
import com.example.Lulu.util.textBundleForWeekdayParsing
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private tailrec fun Context.findActivityForFullscreenImage(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivityForFullscreenImage()
    else -> null
}

private fun buildServiceExtraFeeDescription(service: Service): String {
    val lines = mutableListOf<String>()
    if (service.prepaymentPercent > 0) {
        lines += "当前预付款比例为${service.prepaymentPercent}%，最终金额与尾款以沟通确认为准。"
    } else {
        lines += "当前无需预付款，最终金额以沟通确认为准。"
    }
    val extraFeeTags = ServicePublishTaxonomy.normalizeExtraFeeTags(service.category, service.serviceExtraFeeTags)
    if (extraFeeTags.isNotEmpty()) {
        lines += "可能涉及：${extraFeeTags.joinToString("、")}。"
    }
    val extraRules = ServiceDeclarations.normalizeExtra(service.serviceDeclarationsExtra)
    if (extraFeeTags.isEmpty() && extraRules.isEmpty()) {
        lines += "节假日、跨区交通、超时加购、门票或场地等额外费用如有发生，需提前与主理人确认。"
    } else if (extraRules.isNotEmpty()) {
        lines += "补充说明：${extraRules.take(2).joinToString("；")}"
    }
    return lines.joinToString("\n")
}

private fun applyCreatorPriceAndRulesFromSettings(
    base: Service,
    newPriceText: String,
    newPriceBasisText: String,
    prepaymentPercent: Int,
    fullRefundCancelLeadDays: Int,
    serviceMode: String,
) {
    AppDataStore.updateService(
        id = base.id,
        title = base.title,
        note = base.description,
        participantIds = base.participantIds,
        isImportant = base.isImportant,
        location = base.location,
        priceText = newPriceText,
        priceBasisText = newPriceBasisText,
        category = base.category,
        serviceMode = serviceMode,
        coverImageUrl = base.coverImageUrl,
        imageUrls = base.imageUrls,
        syncToSquare = base.syncToSquare,
        bookingTimeRangesJson = base.bookingTimeRangesJson,
        bookingLeadHours = base.bookingLeadHours,
        bookingFutureOpenDays = base.bookingFutureOpenDays,
        prepaymentPercent = prepaymentPercent,
        fullRefundCancelLeadDays = fullRefundCancelLeadDays,
        autoAcceptAfterPayment = base.autoAcceptAfterPayment,
        serviceDeclarationsExtra = base.serviceDeclarationsExtra,
        isDraft = base.isDraft,
    )
}

private val ServiceDetailPriceSubtitleGray = Color(0xFF666666)
private val ServiceDetailActionButtonColor = Color(0xFFE0115F)

/** 头图与白底内容区重叠高度（圆角面板盖住图片底边） */
private val ServiceDetailHeroPanelOverlap = 28.dp
private val ServiceDetailHeroPanelTopCorner = 28.dp

private val InquiryComposeSubtitleGray = Color(0xFF666666)
private val InquiryComposeBorderGray = Color(0xFFE5E5E5)
private val InquiryComposePlaceholderGray = Color(0xFFBDBDBD)
private val InquiryComposeModifyButtonBg = Color(0xFFF2F2F2)
private val InquiryComposeModifyButtonText = Color(0xFF888888)

private val ServiceTypeKeywordSignals = listOf(
    "旅拍" to listOf("旅拍", "摄影", "拍照", "跟拍"),
    "潜水" to listOf("潜水", "自由潜", "水肺"),
    "冲浪" to listOf("冲浪", "桨板"),
    "游艇" to listOf("游艇", "帆船", "出海", "海钓"),
    "按摩" to listOf("按摩", "spa", "理疗"),
    "妆造" to listOf("化妆", "妆造", "造型"),
    "美食" to listOf("私厨", "美食", "餐", "晚宴"),
    "亲子" to listOf("亲子", "家庭"),
    "情侣" to listOf("情侣", "约会"),
    "团建" to listOf("团建", "聚会", "派对"),
)

private fun buildServiceTypeDescription(service: Service): String {
    val category = ServiceCategories.normalize(service.category)
    val serviceMode = service.serviceMode.trim()
    val priceBasis = priceBasisTextForUiDisplay(service.priceBasisText, service.serviceMode).trim()
    val featureTags = ServicePublishTaxonomy.normalizeFeatureTags(category, service.serviceFeatureTags)
    val parts = buildList {
        if (category.isNotBlank()) add("这是一个${category}服务")
        if (serviceMode.isNotBlank()) add("通常以${serviceMode}方式提供")
        if (priceBasis.isNotBlank()) add("计费说明为$priceBasis")
        if (featureTags.isNotEmpty()) add("特点包括${featureTags.joinToString("、")}")
    }
    return if (parts.isNotEmpty()) {
        parts.joinToString("，") + "，具体安排请与主理人沟通确认。"
    } else {
        "服务内容、提供方式与计费规则请与主理人沟通确认。"
    }
}

private fun buildServiceKeywordTags(service: Service): List<String> {
    val category = ServiceCategories.normalize(service.category)
    val serviceMode = service.serviceMode.trim()
    val priceBasis = priceBasisTextForUiDisplay(service.priceBasisText, service.serviceMode).trim()
    val selectedFeatureTags = ServicePublishTaxonomy.normalizeFeatureTags(category, service.serviceFeatureTags)
    val searchableText = listOf(
        service.title,
        service.description,
        category,
        serviceMode,
        priceBasis,
    ).joinToString(" ").lowercase(Locale.getDefault())
    val matchedSignals = ServiceTypeKeywordSignals.mapNotNull { (label, keywords) ->
        label.takeIf { keywords.any(searchableText::contains) }
    }
    return buildList {
        if (category.isNotBlank()) add(category)
        addAll(selectedFeatureTags)
        if (serviceMode.isNotBlank()) add(serviceMode)
        if (priceBasis.isNotBlank()) add(priceBasis)
        addAll(matchedSignals.filterNot(selectedFeatureTags::contains))
    }.distinct().take(8)
}

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceDetailScreen(
    /** 全局路由（登录、聊天、他人主页等仍压在外层栈上） */
    rootNavController: NavController,
    serviceId: String?,
    entrySource: String? = null,
    /** 主壳内层栈：非 null 时返回键只弹内层，避免首页离开组合 */
    mainShellNavController: NavController? = null,
    /** 从「我发布的」等入口直达改价半层时置 true，仅首次进入消费一次 */
    openCreatorPriceOnLaunch: Boolean = false,
) {
    val context = LocalContext.current
    val popNav = mainShellNavController ?: rootNavController
    val userRepository = remember(context) {
        UserRepository(context.applicationContext as Application)
    }
    val view = LocalView.current
    val cleanId = serviceId?.trim()
    val serviceFlow = remember(cleanId) {
        if (cleanId.isNullOrEmpty()) {
            kotlinx.coroutines.flow.flowOf<com.example.Lulu.data.model.Service?>(null)
        } else {
            AppDataStore.getServiceFlow(cleanId)
        }
    }
    val service by serviceFlow.collectAsState(initial = null)
    val currentUser by AppDataStore.currentUser.collectAsState()
    val isLoggedIn = currentUser.id.isNotEmpty()
    val contacts by AppDataStore.contacts.collectAsState()
    val favoriteServiceIds by AppDataStore.favoriteServiceIds.collectAsState()
    val chatRepository = AppDataStore.getRepository()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(chatRepository) {
        chatRepository?.wishlistRemoteSyncFailures?.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }
    var fullscreenImageIndex by remember { mutableIntStateOf(-1) }
    var isTogglingFavorite by remember { androidx.compose.runtime.mutableStateOf(false) }
    var showRemoveWishlistConfirm by remember { androidx.compose.runtime.mutableStateOf(false) }
    var isHeroImageDark by remember { androidx.compose.runtime.mutableStateOf(true) }
    var showBookingSelectionSheet by remember { androidx.compose.runtime.mutableStateOf(false) }
    var bookingSelectionDateMillis by remember { androidx.compose.runtime.mutableStateOf<Long?>(null) }
    var showServiceInquirySheet by remember { androidx.compose.runtime.mutableStateOf(false) }
    var inquiryComposeMessage by remember { androidx.compose.runtime.mutableStateOf("") }
    var inquiryComposeDateMillis by remember { androidx.compose.runtime.mutableStateOf<Long?>(null) }
    var inquiryComposeGuestCount by remember { mutableIntStateOf(1) }
    var inquiryComposeLocation by remember { androidx.compose.runtime.mutableStateOf("") }
    var showInquiryDateSubpicker by remember { androidx.compose.runtime.mutableStateOf(false) }
    var showInquiryLocationDialog by remember { androidx.compose.runtime.mutableStateOf(false) }
    var inquiryLocationEditBuffer by remember { androidx.compose.runtime.mutableStateOf("") }
    var inquiryComposeSending by remember { androidx.compose.runtime.mutableStateOf(false) }
    var showAllServiceReviewsSheet by remember { androidx.compose.runtime.mutableStateOf(false) }
    val allServiceReviewsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isFromWishlist = entrySource == "wishlist"

    fun handleServiceDetailBack() {
        if (isFromWishlist) {
            runCatching {
                rootNavController.getBackStackEntry(Screen.Home.route)
                    .savedStateHandle["force_main_tab"] = 1
            }
        }
        popNav.popBackStack()
    }

    BackHandler(onBack = ::handleServiceDetailBack)

    if (service == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("未找到服务详情")
        }
        return
    }

    val isCreator = service!!.creatorId == currentUser.id
    var showCreatorPriceSheet by remember { androidx.compose.runtime.mutableStateOf(false) }
    var showUnpublishConfirm by remember { androidx.compose.runtime.mutableStateOf(false) }
    var creatorPriceLaunchConsumed by remember(serviceId, openCreatorPriceOnLaunch) {
        androidx.compose.runtime.mutableStateOf(false)
    }
    LaunchedEffect(service, isCreator, openCreatorPriceOnLaunch, creatorPriceLaunchConsumed) {
        val s = service ?: return@LaunchedEffect
        if (
            openCreatorPriceOnLaunch &&
            isCreator &&
            !s.isDeleted &&
            !creatorPriceLaunchConsumed
        ) {
            creatorPriceLaunchConsumed = true
            showCreatorPriceSheet = true
        }
    }
    val isFavorite = favoriteServiceIds.contains(service!!.id)
    val imageUrls = service!!.imageUrls.ifEmpty {
        service!!.coverImageUrl.takeIf { it.isNotBlank() }?.let { listOf(it) } ?: emptyList()
    }
    val creatorUser = remember(service!!.creatorId, contacts, currentUser) {
        AppDataStore.getUserById(service!!.creatorId)
    }
    LaunchedEffect(service!!.creatorId, creatorUser?.photoUrl) {
        val creatorId = service!!.creatorId.trim()
        if (creatorId.isBlank()) return@LaunchedEffect
        if (!creatorUser?.photoUrl.isNullOrBlank()) return@LaunchedEffect
        userRepository.fetchAndCacheUserById(creatorId)
    }
    val creatorName = creatorUser?.remarkName?.ifBlank { creatorUser.name }
        ?.ifBlank { service!!.creator.ifBlank { "发布者" } }
        ?: service!!.creator.ifBlank { "发布者" }
    val onOpenCreatorProfile: () -> Unit = {
        val targetUserId = creatorUser?.id?.trim()?.ifBlank { null }
            ?: service!!.creatorId.trim().ifBlank { null }
        if (targetUserId != null) {
            PendingHostProfileHint.offer(targetUserId, creatorName)
            rootNavController.navigate(Screen.ServiceHostProfile.createRoute(targetUserId))
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("暂未找到发布者信息")
            }
        }
        Unit
    }
    val tagList = buildList {
        val cat = ServiceCategories.normalize(service!!.category)
        if (cat.isNotBlank()) add(cat)
        addAll(ServicePublishTaxonomy.normalizeFeatureTags(cat, service!!.serviceFeatureTags).take(2))
        if (service!!.serviceMode.isNotBlank()) add(service!!.serviceMode)
        val basis = priceBasisTextForUiDisplay(service!!.priceBasisText, service!!.serviceMode)
        if (basis.isNotBlank()) add(basis)
    }
    var selectedContentTierIndex by remember(service!!.id) { mutableIntStateOf(0) }
    val contentTierOptions = remember(service!!.id, service!!.priceText, service!!.priceBasisText) {
        buildServiceContentPickOptions(service!!)
    }
    LaunchedEffect(contentTierOptions.size) {
        if (selectedContentTierIndex >= contentTierOptions.size) {
            selectedContentTierIndex = 0
        }
    }
    val selectedContentTier = remember(contentTierOptions, selectedContentTierIndex) {
        contentTierOptions[selectedContentTierIndex.coerceIn(0, contentTierOptions.lastIndex.coerceAtLeast(0))]
    }
    val hostReviewRows = remember(creatorUser?.id, creatorUser?.reviewSummaries) {
        hostProfileReviewsFromSummaries(creatorUser?.reviewSummaries.orEmpty())
    }
    val hostReviewCount = creatorUser?.reviewCount ?: 0
    val showHostReviewsOnDetail = hostReviewCount > 0 && hostReviewRows.isNotEmpty()
    val reviewThemeDark = isSystemInDarkTheme()
    val scheduleWeekdays = remember(
        service!!.id,
        service!!.title,
        service!!.description,
        service!!.priceText,
        service!!.priceBasisText,
    ) {
        parseAllowedWeekdays(service!!)
    }
    if (showRemoveWishlistConfirm) {
        AlertDialog(
            onDismissRequest = { showRemoveWishlistConfirm = false },
            title = { Text(text = "移出心愿单") },
            text = { Text(text = "是否移出心愿单？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRemoveWishlistConfirm = false
                        scope.launch {
                            isTogglingFavorite = true
                            try {
                                val result = AppDataStore.toggleFavoriteService(service!!.id)
                                val message = if (result.syncFailed) {
                                    "心愿单同步失败，请重试"
                                } else if (result.favoriteIds.contains(service!!.id)) {
                                    "已加入心愿单"
                                } else {
                                    "已移出心愿单"
                                }
                                snackbarHostState.showSnackbar(message)
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
                TextButton(onClick = { showRemoveWishlistConfirm = false }) {
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
                val targetUserId = creatorUser?.id?.ifBlank { null } ?: service!!.creatorId.ifBlank { null }
                when {
                    targetUserId == null -> scope.launch {
                        snackbarHostState.showSnackbar("暂未找到发布者信息")
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
                            val conversation = repo.getOrCreateDirectConversation(targetUserId)
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
                                    serviceTitle = service!!.title,
                                    contextServiceId = service!!.id,
                                    peerUserId = targetUserId
                                )
                            )
                        }
                    }
                }
            }
        )
    }
    if (showBookingSelectionSheet) {
        ServiceBookingSelectionSheet(
            contentOptions = contentTierOptions,
            selectedContentIndex = selectedContentTierIndex,
            onSelectedContentIndex = { selectedContentTierIndex = it },
            scheduleMillis = bookingSelectionDateMillis,
            onScheduleMillisChange = { bookingSelectionDateMillis = it },
            scheduleAllowedWeekdays = scheduleWeekdays,
            scheduleBookingTimeRangesJson = service!!.bookingTimeRangesJson,
            scheduleBookingLeadHours = service!!.bookingLeadHours,
            scheduleBookingFutureOpenDays = service!!.bookingFutureOpenDays,
            scheduleHostCalendarServiceId = service!!.id,
            extraFeeDescription = buildServiceExtraFeeDescription(service!!),
            onDismiss = { showBookingSelectionSheet = false },
            onConfirmBooking = {
                showBookingSelectionSheet = false
                scope.launch {
                    snackbarHostState.showSnackbar("预订功能即将上线")
                }
            },
        )
    }
    if (showInquiryDateSubpicker) {
        ServiceInquiryDatePickerDialog(
            service = service!!,
            priceUnitLabel = priceBasisTextForUiDisplay(
                service!!.priceBasisText,
                service!!.serviceMode,
            ).ifBlank { "计费单位待沟通" },
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
                    placeholder = { Text("例如：朝阳区某路某号") },
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

    if (showUnpublishConfirm) {
        AlertDialog(
            onDismissRequest = { showUnpublishConfirm = false },
            title = { Text("取消发布") },
            text = { Text("下架后用户将无法再浏览或预订该服务，条目会出现在「我发布的 · 已取消」。确定吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        AppDataStore.deleteService(service!!.id)
                        showUnpublishConfirm = false
                        handleServiceDetailBack()
                    }
                ) {
                    Text("确定下架", color = Color(0xFFE0115F))
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnpublishConfirm = false }) {
                    Text("返回")
                }
            }
        )
    }

    if (showCreatorPriceSheet) {
        val s = service!!
        val isDark = isSystemInDarkTheme()
        val cardColor = if (isDark) Color(0xFF1C1C1E) else Color.White
        val sheetOnBg = if (isDark) Color.White else Color(0xFF111111)
        val sheetMuted = if (isDark) Color(0xFF8E8E8E) else Color(0xFF666666)
        val dividerColor = if (isDark) Color(0xFF2C2C2C) else Color(0xFFF0F0F0)
        PublishPriceSettingsBottomSheet(
            initialPriceText = s.priceText,
            initialPriceBasisText = s.priceBasisText,
            initialPrepaymentPercent = s.prepaymentPercent,
            initialFullRefundCancelLeadDays = s.fullRefundCancelLeadDays,
            surfaceColor = cardColor,
            onBackgroundColor = sheetOnBg,
            onSurfaceVariantColor = sheetMuted,
            dividerColor = dividerColor,
            accentColor = ServiceDetailActionButtonColor,
            onDismiss = { showCreatorPriceSheet = false },
            onConfirm = { newPriceText, newBasis, newPrepay, newRefundDays, durationUnit ->
                applyCreatorPriceAndRulesFromSettings(
                    base = s,
                    newPriceText = newPriceText,
                    newPriceBasisText = newBasis,
                    prepaymentPercent = newPrepay,
                    fullRefundCancelLeadDays = newRefundDays,
                    serviceMode = durationUnit,
                )
                showCreatorPriceSheet = false
                scope.launch {
                    snackbarHostState.showSnackbar("价格与规则已更新")
                }
            },
        )
    }

    if (!view.isInEditMode) {
        // Do not toggle WindowCompat.setDecorFitsSystemWindows here. Switching it false on this
        // screen and true on dispose leaves the activity in a bad inset state so MainScreen's
        // bottom bar (windowInsetsPadding + fixed height) lays out incorrectly after pop.
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
            // Re-apply across a few frames to avoid being overridden by previous screen disposal.
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
            // true = dark status-bar icons; false = light status-bar icons.
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
                when {
                    isCreator && service!!.isDeleted -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                                .navigationBarsPadding(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "该服务已下架",
                                color = ServiceDetailPriceSubtitleGray,
                                fontSize = 14.sp,
                            )
                        }
                    }
                    isCreator && !service!!.isDeleted -> {
                        val creatorActionOutline = Color(0xFFE0E0E0)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .navigationBarsPadding(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(
                                onClick = {
                                    rootNavController.navigate(
                                        Screen.PublishedServiceCalendar.createRoute(serviceId = service!!.id),
                                    ) { launchSingleTop = true }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    horizontal = 4.dp,
                                    vertical = 0.dp,
                                ),
                                border = BorderStroke(1.dp, creatorActionOutline),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF222222)),
                            ) {
                                Text("日历", fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            OutlinedButton(
                                onClick = { showCreatorPriceSheet = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    horizontal = 4.dp,
                                    vertical = 0.dp,
                                ),
                                border = BorderStroke(1.dp, creatorActionOutline),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF222222)),
                            ) {
                                Text("改价", fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            OutlinedButton(
                                onClick = { showUnpublishConfirm = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    horizontal = 4.dp,
                                    vertical = 0.dp,
                                ),
                                border = BorderStroke(1.dp, Color(0xFFE0115F)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE0115F)),
                            ) {
                                Text("下架", fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    else -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .navigationBarsPadding(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ServiceDetailBottomPrice(
                                priceText = selectedContentTier.priceText,
                                durationSubtitle = selectedContentTier.durationDisplay,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    if (!isLoggedIn) {
                                        rootNavController.navigate(Screen.Login.createRoute()) { launchSingleTop = true }
                                        return@Button
                                    }
                                    showBookingSelectionSheet = true
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
                        contentDescription = service!!.title,
                        onImageClick = { pageIndex ->
                            fullscreenImageIndex = pageIndex
                        },
                        onBackClick = ::handleServiceDetailBack,
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
                                showRemoveWishlistConfirm = true
                            } else {
                                scope.launch {
                                    if (isTogglingFavorite) return@launch
                                    isTogglingFavorite = true
                                    try {
                                        val group = ServiceCategories.normalize(service!!.category)
                                        val result = AppDataStore.addFavoriteServiceToGroup(
                                            service!!.id,
                                            group
                                        )
                                        val message = if (result.syncFailed) {
                                            "加入心愿单失败，请重试"
                                        } else if (result.favoriteIds.contains(service!!.id)) {
                                            "已加入心愿单"
                                        } else {
                                            "加入心愿单失败，请重试"
                                        }
                                        snackbarHostState.showSnackbar(message)
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
                            text = service!!.title.ifBlank { "未命名服务" },
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
                                        contentDescription = "发布者头像",
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
                                                contentDescription = "主理人",
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
                            introSectionTitle = "服务介绍",
                            contentSectionTitle = "方案选择",
                            bookingSectionTitle = "预定须知",
                            fullDescription = service!!.description.ifBlank { "暂无服务描述" },
                            contentOptions = contentTierOptions,
                            selectedContentIndex = selectedContentTierIndex,
                            onSelectedContentIndex = { selectedContentTierIndex = it },
                            scheduleMillis = bookingSelectionDateMillis,
                            onScheduleMillisChange = { bookingSelectionDateMillis = it },
                            scheduleAllowedWeekdays = scheduleWeekdays,
                            scheduleBookingTimeRangesJson = service!!.bookingTimeRangesJson,
                            scheduleBookingLeadHours = service!!.bookingLeadHours,
                            scheduleBookingFutureOpenDays = service!!.bookingFutureOpenDays,
                            scheduleHostCalendarServiceId = service!!.id,
                            serviceDeclarationsSubtitle = ServiceDeclarations.declarationSummaryExtraCount(
                                service!!.serviceDeclarationsExtra,
                            ),
                            serviceDeclarationsReaderBody = ServiceDeclarations.declarationReaderBody(
                                service!!.serviceDeclarationsExtra,
                            ),
                            extraFeeDescription = buildServiceExtraFeeDescription(service!!),
                            serviceTypeDescription = buildServiceTypeDescription(service!!),
                            serviceTypeKeywords = buildServiceKeywordTags(service!!),
                            showBookingSelectionInline = false,
                        )
                        if (showHostReviewsOnDetail) {
                            ProfileReviewsSection(
                                sectionTitle = "${hostReviewCount}条评价",
                                reviews = hostReviewRows,
                                isDarkTheme = reviewThemeDark,
                                onShowMore = { showAllServiceReviewsSheet = true },
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    if (showAllServiceReviewsSheet && hostReviewRows.isNotEmpty()) {
        ModalBottomSheet(
            onDismissRequest = { showAllServiceReviewsSheet = false },
            sheetState = allServiceReviewsSheetState,
            containerColor = if (reviewThemeDark) Color(0xFF1E1E1E) else Color(0xFFFFFFFF),
            dragHandle = null,
        ) {
            AllReviewsSheetContent(
                reviews = hostReviewRows,
                isDarkTheme = reviewThemeDark,
                onClose = { showAllServiceReviewsSheet = false },
            )
        }
    }

    if (fullscreenImageIndex >= 0 && imageUrls.isNotEmpty()) {
        FullscreenImageViewer(
            imageUrls = imageUrls,
            initialPage = fullscreenImageIndex.coerceIn(0, imageUrls.lastIndex),
            contentDescription = service!!.title,
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
    /** 为底部圆角白底留出空间，避免页码/指示点被遮挡 */
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
