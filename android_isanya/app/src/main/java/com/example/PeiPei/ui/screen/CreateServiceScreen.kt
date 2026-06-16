// 文件说明：创建或编辑服务/陪玩单的表单界面。

package com.example.Lulu.ui.screen

import android.Manifest
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.Paint
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.foundation.text.BasicTextField
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.Lulu.R
import com.example.Lulu.ui.navigation.Screen
import com.example.Lulu.data.local.AppDataStore
import com.example.Lulu.data.model.ServiceCategories
import com.example.Lulu.data.model.ServicePublishTaxonomy
import com.example.Lulu.data.model.User
import com.example.Lulu.ui.components.PrimaryGradientButton
import com.example.Lulu.ui.theme.DialogTitleTopPadding
import com.example.Lulu.ui.theme.ErrorRed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb

import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import android.media.MediaPlayer
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import com.example.Lulu.util.BookingTimeRangesCodec
import com.example.Lulu.util.DURATION_UNIT_HOURS
import com.example.Lulu.util.decodePublishPriceTiers
import com.example.Lulu.util.normalizeDurationUnit
import com.example.Lulu.util.BookingTimeSlot
import com.example.Lulu.util.ServiceImageProcessingUtil
import com.example.Lulu.util.ServiceLocationPolygonCodec
import com.example.Lulu.ui.components.PublishPriceSettingsBottomSheet
import com.example.Lulu.ui.components.BookingTimeRangesPickerBottomSheet
import com.example.Lulu.ui.components.OrderAcceptanceSettingsBottomSheet
import com.example.Lulu.ui.components.orderAcceptanceSettingSummary
import com.example.Lulu.ui.components.ServiceDeclarationsPickerBottomSheet
import com.example.Lulu.ui.components.ServiceCategoryPickerGateDialog
import com.example.Lulu.ui.components.ServiceTagMultiSelectBottomSheet
import com.example.Lulu.ui.components.RegionPickerDialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateServiceScreen(
    navController: NavController, 
    serviceId: String? = null,
    initialUserIds: String? = null,
    initialTitle: String? = null,
    /** 若从外部带入类目（如深链），则不再显示前置类别弹窗。 */
    initialCategory: String? = null,
    /** 编辑模式下进入后自动打开接单时段（日历）设置半层 */
    openBookingOnLaunch: Boolean = false,
) {
    val context = LocalContext.current
    val navServiceId = serviceId?.trim()?.takeIf { it.isNotEmpty() && it != "{serviceId}" }
    /** 首次存草稿后写入本地，后续保存走更新 */
    var localDraftServiceId by remember { mutableStateOf<String?>(null) }
    val effectiveServiceId = navServiceId ?: localDraftServiceId
    val isEditMode = effectiveServiceId != null

    // 尽管我们可能不再主要依赖 LocalSoftwareKeyboardController，但保留它作为一个 fallback
    val keyboardController = LocalSoftwareKeyboardController.current
    val view = LocalView.current
    val currentUser by AppDataStore.currentUser.collectAsState()

    // Theme adaptation (follow system)
    val isDarkTheme = isSystemInDarkTheme()
    
    val backgroundColor = if (isDarkTheme) Color.Black else Color(0xFFF5F5F5)
    val cardColor = if (isDarkTheme) Color(0xFF1C1C1E) else Color.White
    val dividerColor = if (isDarkTheme) Color(0xFF2C2C2C) else Color(0xFFF0F0F0)
    val iconTint = if (isDarkTheme) Color.LightGray else Color.Gray
    val placeholderColor = if (isDarkTheme) Color.Gray else Color.Gray
    val themeAccentRed = Color(0xFFE0115F)
    val density = LocalDensity.current
    
    // 表单状态
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf(initialTitle ?: "") }
    var priceText by remember { mutableStateOf("") }
    var priceBasisText by remember { mutableStateOf("") }
    var prepaymentPercent by remember { mutableStateOf(30) }
    var fullRefundCancelLeadDays by remember { mutableStateOf(1) }
    /** 与后端 service_mode 字段对齐：存时长单位「小时」或「分钟」，用于展示兜底等 */
    var serviceMode by remember { mutableStateOf(DURATION_UNIT_HOURS) }
    /** 新建时为 null，表示尚未选择标签（必填）；编辑时由已有服务填充。 */
    var serviceCategory by remember { mutableStateOf<String?>(null) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    /** 新建且未通过路由传入类目时，先全屏选类目再填表。 */
    var showCategoryGate by remember(serviceId, initialCategory, isEditMode) {
        mutableStateOf(!isEditMode && initialCategory.isNullOrBlank())
    }
    var locationText by remember { mutableStateOf("") }
    var serviceFeatureTags by remember { mutableStateOf(listOf<String>()) }
    var showServiceFeaturePicker by remember { mutableStateOf(false) }
    var syncToSquare by remember { mutableStateOf(true) }
    var serviceImageUris by remember { mutableStateOf<List<String>>(emptyList()) }
    val maxServiceImageCount = 16
    var keepOriginalQuality by remember { mutableStateOf(false) }
    var enableFullscreenPreview by remember { mutableStateOf(false) }
    var previewImageUri by remember { mutableStateOf<String?>(null) }
    var showCustomImagePicker by remember { mutableStateOf(false) }
    var pickerSelectedUris by remember { mutableStateOf<List<String>>(emptyList()) }
    val requiredImagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val hasImagePermission = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, requiredImagePermission) == PackageManager.PERMISSION_GRANTED
        )
    }
    val imagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasImagePermission.value = granted
        if (granted) {
            pickerSelectedUris = serviceImageUris
            showCustomImagePicker = true
        } else {
            android.widget.Toast.makeText(context, "请授予相册访问权限后再选择图片", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    val localGalleryImages by produceState(initialValue = emptyList<String>(), showCustomImagePicker) {
        value = if (showCustomImagePicker) queryRecentImageUris(context) else emptyList()
    }

    // 是否重要
    var isImportant by remember { mutableStateOf(false) }
    
    // 参与者
    var showParticipantDialog by remember { mutableStateOf(false) }
    var selectedParticipants by remember { mutableStateOf(listOf<User>()) }
    
    // 错误信息
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // 状态：提交中（防抖与防重复）
    var isSubmitting by remember { mutableStateOf(false) }
    // 状态：编辑确认弹窗
    var showEditConfirmDialog by remember { mutableStateOf(false) }
    
    // 焦点控制
    val focusRequester = remember { FocusRequester() }
    val descriptionFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()

    var showPriceSettingsDialog by remember { mutableStateOf(false) }
    var showBookingTimeSheet by remember { mutableStateOf(false) }
    var bookingTimeSlots by remember { mutableStateOf(listOf<BookingTimeSlot>()) }
    var bookingLeadHours by remember {
        mutableStateOf(BookingTimeRangesCodec.DEFAULT_BOOKING_LEAD_HOURS)
    }
    var bookingFutureOpenDays by remember {
        mutableStateOf(BookingTimeRangesCodec.DEFAULT_BOOKING_FUTURE_OPEN_DAYS)
    }
    var showOrderAcceptanceSheet by remember { mutableStateOf(false) }
    /** true：付款后自动接单；false：付款后需手动确认订单信息再接单 */
    var autoAcceptAfterPayment by remember { mutableStateOf(true) }
    var showServiceCityPicker by remember { mutableStateOf(false) }
    var serviceExtraFeeTags by remember { mutableStateOf(listOf<String>()) }
    var showServiceExtraFeePicker by remember { mutableStateOf(false) }
    /** 用户补充的服务声明（不含平台默认四条） */
    var extraServiceDeclarations by remember { mutableStateOf(listOf<String>()) }
    var showServiceDeclarationsSheet by remember { mutableStateOf(false) }
    LaunchedEffect(initialCategory, isEditMode) {
        if (!isEditMode && !initialCategory.isNullOrBlank()) {
            serviceCategory = ServiceCategories.normalize(initialCategory)
        }
    }

    val formHints = remember(serviceCategory) {
        serviceCategory?.let { ServiceCategories.publishFormHints(it) }
    }

    // 前置类别弹窗关闭后再聚焦描述（新建）
    LaunchedEffect(isEditMode, showCategoryGate) {
        if (!isEditMode && !showCategoryGate) {
            kotlinx.coroutines.delay(120)
            descriptionFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }
    
    // 加载已有数据
    LaunchedEffect(serviceId, initialUserIds, openBookingOnLaunch) {
        if (navServiceId != null) {
            val service = AppDataStore.getServiceById(navServiceId)
            if (service != null) {
                title = service.title
                description = service.description.ifBlank { service.title }
                priceText = service.priceText
                priceBasisText = service.priceBasisText
                prepaymentPercent = service.prepaymentPercent.coerceIn(0, 100)
                fullRefundCancelLeadDays = service.fullRefundCancelLeadDays.coerceIn(0, 10)
                val decodedTiers = decodePublishPriceTiers(service.priceText, service.priceBasisText)
                serviceMode = decodedTiers.firstOrNull()?.let { normalizeDurationUnit(it.durationUnit) }
                    ?: service.serviceMode.ifBlank { DURATION_UNIT_HOURS }
                serviceCategory = ServiceCategories.normalize(service.category)
                locationText = service.location
                serviceFeatureTags = ServicePublishTaxonomy.normalizeFeatureTags(
                    service.category,
                    service.serviceFeatureTags
                )
                syncToSquare = service.syncToSquare
                serviceImageUris = service.imageUrls.ifEmpty {
                    service.coverImageUrl.takeIf { it.isNotBlank() }?.let { listOf(it) } ?: emptyList()
                }
                isImportant = service.isImportant
                bookingTimeSlots = BookingTimeRangesCodec.decode(service.bookingTimeRangesJson)
                bookingLeadHours = BookingTimeRangesCodec.normalizeBookingLeadHours(service.bookingLeadHours)
                bookingFutureOpenDays =
                    BookingTimeRangesCodec.normalizeBookingFutureOpenDays(service.bookingFutureOpenDays)
                autoAcceptAfterPayment = service.autoAcceptAfterPayment
                serviceExtraFeeTags = ServicePublishTaxonomy.normalizeExtraFeeTags(
                    service.category,
                    service.serviceExtraFeeTags
                )
                extraServiceDeclarations = service.serviceDeclarationsExtra

                // 恢复参与者 (包含自己)
                val participants = service.participantIds.mapNotNull { AppDataStore.getUserById(it) }
                selectedParticipants = participants

                if (openBookingOnLaunch) {
                    kotlinx.coroutines.delay(350)
                    showBookingTimeSheet = true
                }
            }
        } else if (initialUserIds != null) {
            // 处理预选联系人
            val ids = initialUserIds.split(",")
            val users = ids.mapNotNull { AppDataStore.getUserById(it) }
            if (users.any { it.id == currentUser.id }) {
                selectedParticipants = users
            } else {
                selectedParticipants = users + currentUser
            }
        } else {
            // 新建服务，默认选中自己
            selectedParticipants = listOf(currentUser)
        }
    }

    LaunchedEffect(serviceCategory) {
        val normalizedCategory = ServiceCategories.normalize(serviceCategory)
        serviceFeatureTags = ServicePublishTaxonomy.normalizeFeatureTags(
            normalizedCategory,
            serviceFeatureTags
        )
        serviceExtraFeeTags = ServicePublishTaxonomy.normalizeExtraFeeTags(
            normalizedCategory,
            serviceExtraFeeTags
        )
    }

    // 执行保存
    fun performSave() {
        keyboardController?.hide()
        isSubmitting = true
        val publishContent = description.trim()
        val publishTitle = title.trim()
        val category = checkNotNull(serviceCategory) { "category required" }
        val savedImageUrls = serviceImageUris.take(maxServiceImageCount)
        val savedCoverImage = savedImageUrls.firstOrNull().orEmpty()

        val finalParticipantIds = selectedParticipants.map { it.id }.distinct()
        val bookingJson = BookingTimeRangesCodec.encode(bookingTimeSlots)

        if (isEditMode && effectiveServiceId != null) {
             AppDataStore.updateService(
                id = effectiveServiceId,
                title = publishTitle,
                note = publishContent,
                participantIds = finalParticipantIds,
                isImportant = isImportant,
                location = locationText,
                priceText = priceText,
                priceBasisText = priceBasisText,
                category = category,
                serviceMode = serviceMode,
                coverImageUrl = savedCoverImage,
                imageUrls = savedImageUrls,
                syncToSquare = syncToSquare,
                bookingTimeRangesJson = bookingJson,
                bookingLeadHours = BookingTimeRangesCodec.normalizeBookingLeadHours(bookingLeadHours),
                bookingFutureOpenDays = BookingTimeRangesCodec.normalizeBookingFutureOpenDays(bookingFutureOpenDays),
                prepaymentPercent = prepaymentPercent.coerceIn(0, 100),
                fullRefundCancelLeadDays = fullRefundCancelLeadDays.coerceIn(0, 10),
                autoAcceptAfterPayment = autoAcceptAfterPayment,
                serviceFeatureTags = serviceFeatureTags,
                serviceExtraFeeTags = serviceExtraFeeTags,
                serviceDeclarationsExtra = extraServiceDeclarations,
                isDraft = false,
            )
             android.widget.Toast.makeText(context, "服务已更新", android.widget.Toast.LENGTH_SHORT).show()

             // 播放成功音效
             try {
                 val mediaPlayer = MediaPlayer.create(context, R.raw.success)
                 mediaPlayer.setOnCompletionListener { mp -> mp.release() }
                 mediaPlayer.start()
             } catch (e: Exception) {
                 e.printStackTrace()
             }

             navController.popBackStack()
        } else {
            // 创建服务
            val newService = AppDataStore.addService(
                title = publishTitle,
                note = publishContent,
                participantIds = finalParticipantIds,
                isImportant = isImportant,
                location = locationText,
                priceText = priceText,
                priceBasisText = priceBasisText,
                category = category,
                serviceMode = serviceMode,
                coverImageUrl = savedCoverImage,
                imageUrls = savedImageUrls,
                syncToSquare = syncToSquare,
                bookingTimeRangesJson = bookingJson,
                bookingLeadHours = BookingTimeRangesCodec.normalizeBookingLeadHours(bookingLeadHours),
                bookingFutureOpenDays = BookingTimeRangesCodec.normalizeBookingFutureOpenDays(bookingFutureOpenDays),
                prepaymentPercent = prepaymentPercent.coerceIn(0, 100),
                fullRefundCancelLeadDays = fullRefundCancelLeadDays.coerceIn(0, 10),
                autoAcceptAfterPayment = autoAcceptAfterPayment,
                serviceFeatureTags = serviceFeatureTags,
                serviceExtraFeeTags = serviceExtraFeeTags,
                serviceDeclarationsExtra = extraServiceDeclarations,
                isDraft = false,
            )
            android.widget.Toast.makeText(context, "服务已发布", android.widget.Toast.LENGTH_SHORT).show()

            // 播放成功音效
            try {
                val mediaPlayer = MediaPlayer.create(context, R.raw.success)
                mediaPlayer.setOnCompletionListener { mp -> mp.release() }
                mediaPlayer.start()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 创建成功后跳转到详情页，并移除当前的创建页面
            navController.navigate(Screen.ServiceDetail.createRoute(newService.id)) {
                popUpTo(Screen.CreateService.route) {
                    inclusive = true
                }
            }
        }
    }

    fun performSaveDraft() {
        keyboardController?.hide()
        if (isSubmitting) return
        isSubmitting = true
        val category = serviceCategory ?: ServiceCategories.OTHER
        val publishContent = description.trim()
        val publishTitle = title.trim().ifBlank { "未命名草稿" }
        val savedImageUrls = serviceImageUris.take(maxServiceImageCount)
        val savedCoverImage = savedImageUrls.firstOrNull().orEmpty()
        val finalParticipantIds = selectedParticipants.map { it.id }.distinct()
        val bookingJson = BookingTimeRangesCodec.encode(bookingTimeSlots)
        val id = effectiveServiceId
        try {
            if (id != null) {
                AppDataStore.updateService(
                    id = id,
                    title = publishTitle,
                    note = publishContent.ifBlank { publishTitle },
                    participantIds = finalParticipantIds,
                    isImportant = isImportant,
                    location = locationText,
                    priceText = priceText,
                    priceBasisText = priceBasisText,
                    category = category,
                    serviceMode = serviceMode,
                    coverImageUrl = savedCoverImage,
                    imageUrls = savedImageUrls,
                    syncToSquare = syncToSquare,
                    bookingTimeRangesJson = bookingJson,
                    bookingLeadHours = BookingTimeRangesCodec.normalizeBookingLeadHours(bookingLeadHours),
                    bookingFutureOpenDays = BookingTimeRangesCodec.normalizeBookingFutureOpenDays(bookingFutureOpenDays),
                    prepaymentPercent = prepaymentPercent.coerceIn(0, 100),
                    fullRefundCancelLeadDays = fullRefundCancelLeadDays.coerceIn(0, 10),
                    autoAcceptAfterPayment = autoAcceptAfterPayment,
                    serviceFeatureTags = serviceFeatureTags,
                    serviceExtraFeeTags = serviceExtraFeeTags,
                    serviceDeclarationsExtra = extraServiceDeclarations,
                    isDraft = true,
                )
            } else {
                val newService = AppDataStore.addService(
                    title = publishTitle,
                    note = publishContent.ifBlank { publishTitle },
                    participantIds = finalParticipantIds,
                    isImportant = isImportant,
                    location = locationText,
                    priceText = priceText,
                    priceBasisText = priceBasisText,
                    category = category,
                    serviceMode = serviceMode,
                    coverImageUrl = savedCoverImage,
                    imageUrls = savedImageUrls,
                    syncToSquare = syncToSquare,
                    bookingTimeRangesJson = bookingJson,
                    bookingLeadHours = BookingTimeRangesCodec.normalizeBookingLeadHours(bookingLeadHours),
                    bookingFutureOpenDays = BookingTimeRangesCodec.normalizeBookingFutureOpenDays(bookingFutureOpenDays),
                    prepaymentPercent = prepaymentPercent.coerceIn(0, 100),
                    fullRefundCancelLeadDays = fullRefundCancelLeadDays.coerceIn(0, 10),
                    autoAcceptAfterPayment = autoAcceptAfterPayment,
                    serviceFeatureTags = serviceFeatureTags,
                    serviceExtraFeeTags = serviceExtraFeeTags,
                    serviceDeclarationsExtra = extraServiceDeclarations,
                    isDraft = true,
                )
                localDraftServiceId = newService.id
            }
            android.widget.Toast.makeText(context, "已存为草稿", android.widget.Toast.LENGTH_SHORT).show()
            val previousRoute = navController.previousBackStackEntry?.destination?.route.orEmpty()
            if (previousRoute.startsWith(Screen.MyPublishedServices.baseRoute)) {
                navController.navigate(
                    Screen.MyPublishedServices.createRoute(initialSection = "drafts")
                ) {
                    popUpTo(Screen.MyPublishedServices.route) { inclusive = true }
                    launchSingleTop = true
                }
            } else {
                navController.navigate(
                    Screen.MyPublishedServices.createRoute(initialSection = "drafts")
                ) {
                    popUpTo(Screen.CreateService.route) { inclusive = true }
                }
            }
        } finally {
            isSubmitting = false
        }
    }

    // 提交处理
    fun handleSubmit() {
        if (isSubmitting) return
        
        if (description.isBlank()) {
            errorMessage = "请先填写服务描述"
            return
        }

        if (title.isBlank()) {
            errorMessage = "请先填写服务标题"
            return
        }

        if (serviceCategory == null) {
            errorMessage = "请先选择服务类别"
            return
        }

        if (serviceImageUris.isEmpty()) {
            errorMessage = "请至少上传1张服务图片"
            return
        }

        if (priceText.isBlank()) {
            errorMessage = "请先完成价格设置"
            return
        }
        
        if (!isEditMode) {
            // 新建服务直接保存，不需要二次确认
            performSave()
        } else {
            // 编辑已有服务需显示确认弹窗
            showEditConfirmDialog = true
        }
    }

    fun dismissInput() {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
    }

    fun moveImage(fromIndex: Int, toIndex: Int) {
        if (fromIndex !in serviceImageUris.indices || toIndex !in serviceImageUris.indices) return
        if (fromIndex == toIndex) return
        val mutable = serviceImageUris.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        serviceImageUris = mutable
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {},
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = backgroundColor
                )
            )
        },
        containerColor = backgroundColor
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    shape = RoundedCornerShape(18.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        val imageItemSize = 78.dp
                        val imageItemSpacing = 8.dp
                        val wechatStyleAccent = Color(0xFFE0115F)
                        var draggingImageIndex by remember { mutableStateOf<Int?>(null) }
                        var dragOffsetX by remember { mutableStateOf(0f) }
                        val dragThresholdPx = with(density) { (imageItemSize + imageItemSpacing).toPx() * 0.45f }

                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(imageItemSpacing)
                        ) {
                            items(
                                items = serviceImageUris,
                                key = { it }
                            ) { imageUri ->
                                val imageIndex = serviceImageUris.indexOf(imageUri)
                                Box(
                                    modifier = Modifier
                                        .size(imageItemSize)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isDarkTheme) Color(0xFF2A2A2A) else Color(0xFFF2F2F2))
                                        .pointerInput(serviceImageUris) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = {
                                                    draggingImageIndex = imageIndex
                                                    dragOffsetX = 0f
                                                },
                                                onDragEnd = {
                                                    draggingImageIndex = null
                                                    dragOffsetX = 0f
                                                },
                                                onDragCancel = {
                                                    draggingImageIndex = null
                                                    dragOffsetX = 0f
                                                }
                                            ) { change, dragAmount ->
                                                change.consume()
                                                if (draggingImageIndex != imageIndex) return@detectDragGesturesAfterLongPress
                                                dragOffsetX += dragAmount.x
                                                if (dragOffsetX > dragThresholdPx && imageIndex < serviceImageUris.lastIndex) {
                                                    moveImage(imageIndex, imageIndex + 1)
                                                    draggingImageIndex = imageIndex + 1
                                                    dragOffsetX = 0f
                                                } else if (dragOffsetX < -dragThresholdPx && imageIndex > 0) {
                                                    moveImage(imageIndex, imageIndex - 1)
                                                    draggingImageIndex = imageIndex - 1
                                                    dragOffsetX = 0f
                                                }
                                            }
                                        }
                                        .clickable {
                                            if (enableFullscreenPreview) {
                                                previewImageUri = imageUri
                                            }
                                        }
                                ) {
                                    coil.compose.AsyncImage(
                                        model = imageUri,
                                        contentDescription = "服务图片",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .size(22.dp),
                                        shape = CircleShape,
                                        color = wechatStyleAccent,
                                        border = BorderStroke(1.5.dp, Color.White)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = (imageIndex + 1).toString(),
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            if (serviceImageUris.size < maxServiceImageCount) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .size(imageItemSize)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (isDarkTheme) Color(0xFF2A2A2A) else Color(0xFFF2F2F2))
                                            .border(1.dp, if (isDarkTheme) Color(0xFF414141) else Color(0xFFE6E6E6), RoundedCornerShape(4.dp))
                                            .clickable {
                                                if (hasImagePermission.value) {
                                                    pickerSelectedUris = serviceImageUris
                                                    showCustomImagePicker = true
                                                } else {
                                                    imagePermissionLauncher.launch(requiredImagePermission)
                                                }
                                            }
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "添加图片",
                                                tint = if (isDarkTheme) Color(0xFFBEBEBE) else Color(0xFF8D8D8D),
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        BasicTextField(
                            value = title,
                            onValueChange = {
                                if (it.length <= 40) {
                                    title = it
                                    errorMessage = null
                                }
                            },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 20.sp,
                                lineHeight = 26.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { innerTextField ->
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    if (title.isEmpty()) {
                                        Text(
                                            text = formHints?.titlePlaceholder ?: "添加标题",
                                            fontSize = 20.sp,
                                            lineHeight = 26.sp,
                                            color = placeholderColor.copy(alpha = 0.7f),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        BasicTextField(
                            value = description,
                            onValueChange = {
                                if (it.length <= 300) {
                                    description = it
                                    errorMessage = null
                                }
                            },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 18.sp,
                                lineHeight = 28.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(descriptionFocusRequester),
                            minLines = 6,
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 180.dp)
                                ) {
                                    if (description.isEmpty()) {
                                        Text(
                                            text = formHints?.descriptionPlaceholder
                                                ?: "详细介绍服务内容、流程和亮点，帮助用户快速了解并下单",
                                            fontSize = 15.sp,
                                            lineHeight = 22.sp,
                                            color = placeholderColor.copy(alpha = 0.7f)
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (formHints?.categoryTip != null) {
                            Text(
                                text = formHints.categoryTip,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        PublishOptionRow(
                            label = "服务类别",
                            value = serviceCategory ?: "去选择",
                            valueColor = if (serviceCategory == null) placeholderColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = { showCategoryDialog = true }
                        )
                        HorizontalDivider(color = dividerColor, modifier = Modifier.padding(top = 4.dp))
                        PublishOptionRow(
                            label = "服务特点",
                            value = ServicePublishTaxonomy.selectionSummary(serviceFeatureTags),
                            valueColor = if (serviceFeatureTags.isEmpty()) placeholderColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = {
                                if (serviceCategory == null) {
                                    errorMessage = "请先选择服务类别"
                                } else {
                                    showServiceFeaturePicker = true
                                }
                            }
                        )
                        if (errorMessage != null) {
                            Text(
                                text = errorMessage!!,
                                color = ErrorRed,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        val priceSettingsSummary = if (priceText.isBlank()) {
                            "去设置" to placeholderColor
                        } else {
                            buildString {
                                append(priceText.trim())
                                append('\n')
                                append("预付款${prepaymentPercent}%")
                                append(" · ")
                                append(fullRefundCancelLeadSummaryLine(fullRefundCancelLeadDays))
                            } to Color(0xFFFF5A4F)
                        }
                        PublishOptionRow(
                            label = "价格设置",
                            value = priceSettingsSummary.first,
                            valueMaxLines = if (priceText.isBlank()) 1 else 3,
                            valueColor = priceSettingsSummary.second,
                            onClick = { showPriceSettingsDialog = true }
                        )
                        HorizontalDivider(color = dividerColor, modifier = Modifier.padding(start = 16.dp))
                        val bookingSummary = buildString {
                            append(BookingTimeRangesCodec.summary(bookingTimeSlots))
                            append('\n')
                            append(BookingTimeRangesCodec.leadHoursRequirementSummary(bookingLeadHours))
                            append('\n')
                            append(BookingTimeRangesCodec.futureOpenWindowSummary(bookingFutureOpenDays))
                        }
                        PublishOptionRow(
                            label = "时间设置",
                            value = bookingSummary,
                            valueMaxLines = 4,
                            valueColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = { showBookingTimeSheet = true }
                        )
                        HorizontalDivider(color = dividerColor, modifier = Modifier.padding(start = 16.dp))
                        PublishOptionRow(
                            label = "接单设置",
                            value = orderAcceptanceSettingSummary(autoAcceptAfterPayment),
                            valueColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = { showOrderAcceptanceSheet = true },
                            valueMaxLines = 3,
                        )
                        HorizontalDivider(color = dividerColor, modifier = Modifier.padding(start = 16.dp))
                        PublishOptionRow(
                            label = "服务城市",
                            value = ServiceLocationPolygonCodec.displayLine(locationText).ifBlank { "去设置" },
                            valueColor = if (locationText.isBlank()) placeholderColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = { showServiceCityPicker = true }
                        )
                        HorizontalDivider(color = dividerColor, modifier = Modifier.padding(start = 16.dp))
                        PublishOptionRow(
                            label = "额外费用",
                            value = ServicePublishTaxonomy.selectionSummary(serviceExtraFeeTags),
                            valueColor = if (serviceExtraFeeTags.isEmpty()) placeholderColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = {
                                if (serviceCategory == null) {
                                    errorMessage = "请先选择服务类别"
                                } else {
                                    showServiceExtraFeePicker = true
                                }
                            }
                        )
                        HorizontalDivider(color = dividerColor, modifier = Modifier.padding(start = 16.dp))
                        val extraDeclCount = extraServiceDeclarations.count { it.isNotBlank() }
                        val declarationsSummary = if (extraDeclCount == 0) {
                            "平台约定4条"
                        } else {
                            "平台约定4条 · 已补充${extraDeclCount}条"
                        }
                        PublishOptionRow(
                            label = "服务声明",
                            value = declarationsSummary,
                            valueColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = { showServiceDeclarationsSheet = true }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { performSaveDraft() },
                        modifier = Modifier
                            .weight(0.36f)
                            .height(50.dp),
                        shape = RoundedCornerShape(26.dp),
                        border = BorderStroke(1.dp, themeAccentRed),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = themeAccentRed
                        )
                    ) {
                        Text("存草稿")
                    }
                    PrimaryGradientButton(
                        onClick = { handleSubmit() },
                        enabled = !isSubmitting,
                        modifier = Modifier
                            .weight(0.64f)
                            .height(50.dp),
                        shape = RoundedCornerShape(26.dp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("发布服务", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }

    if (showCategoryGate) {
        ServiceCategoryPickerGateDialog(
            onDismissRequest = { navController.popBackStack() },
            onPickCategory = { cat ->
                serviceCategory = cat
                errorMessage = null
                showCategoryGate = false
            },
            onSkipGeneric = {
                serviceCategory = ServiceCategories.OTHER
                errorMessage = null
                showCategoryGate = false
            }
        )
    }

    if (showBookingTimeSheet) {
        val sheetOnBg = if (isDarkTheme) Color.White else Color(0xFF111111)
        val sheetMuted = if (isDarkTheme) Color(0xFF8E8E8E) else Color(0xFF666666)
        BookingTimeRangesPickerBottomSheet(
            initialSlots = bookingTimeSlots,
            initialLeadHours = bookingLeadHours,
            initialFutureOpenDays = bookingFutureOpenDays,
            surfaceColor = cardColor,
            onBackgroundColor = sheetOnBg,
            onSurfaceVariantColor = sheetMuted,
            dividerColor = dividerColor,
            accentColor = themeAccentRed,
            onDismiss = { showBookingTimeSheet = false },
            onConfirm = { slots, lead, futureDays ->
                bookingTimeSlots = slots
                bookingLeadHours = lead
                bookingFutureOpenDays = futureDays
                showBookingTimeSheet = false
            },
        )
    }

    if (showOrderAcceptanceSheet) {
        val sheetOnBg = if (isDarkTheme) Color.White else Color(0xFF111111)
        val sheetMuted = if (isDarkTheme) Color(0xFF8E8E8E) else Color(0xFF666666)
        OrderAcceptanceSettingsBottomSheet(
            initialAutoAcceptAfterPayment = autoAcceptAfterPayment,
            surfaceColor = cardColor,
            onBackgroundColor = sheetOnBg,
            onSurfaceVariantColor = sheetMuted,
            dividerColor = dividerColor,
            accentColor = themeAccentRed,
            onDismiss = { showOrderAcceptanceSheet = false },
            onConfirm = { v ->
                autoAcceptAfterPayment = v
                showOrderAcceptanceSheet = false
            },
        )
    }

    if (showPriceSettingsDialog) {
        val sheetOnBg = if (isDarkTheme) Color.White else Color(0xFF111111)
        val sheetMuted = if (isDarkTheme) Color(0xFF8E8E8E) else Color(0xFF666666)
        PublishPriceSettingsBottomSheet(
            initialPriceText = priceText,
            initialPriceBasisText = priceBasisText,
            initialPrepaymentPercent = prepaymentPercent,
            initialFullRefundCancelLeadDays = fullRefundCancelLeadDays,
            surfaceColor = cardColor,
            onBackgroundColor = sheetOnBg,
            onSurfaceVariantColor = sheetMuted,
            dividerColor = dividerColor,
            accentColor = themeAccentRed,
            onDismiss = { showPriceSettingsDialog = false },
            onConfirm = { newPriceText, newBasis, newPrepay, newRefundDays, durationUnitForServiceMode ->
                priceText = newPriceText
                priceBasisText = newBasis
                prepaymentPercent = newPrepay
                fullRefundCancelLeadDays = newRefundDays
                serviceMode = durationUnitForServiceMode
                showPriceSettingsDialog = false
            },
        )
    }

    if (showServiceCityPicker) {
        RegionPickerDialog(
            initialRegion = ServiceLocationPolygonCodec.displayLine(locationText),
            title = "选择服务城市",
            confirmSelection = { _, city -> city },
            onDismiss = { showServiceCityPicker = false },
            onConfirm = { city ->
                locationText = city
                showServiceCityPicker = false
            }
        )
    }

    if (showServiceFeaturePicker) {
        val currentCategory = ServiceCategories.normalize(serviceCategory)
        val options = ServicePublishTaxonomy.featureTagsFor(currentCategory)
        val sheetOnBg = if (isDarkTheme) Color.White else Color(0xFF111111)
        val sheetMuted = if (isDarkTheme) Color(0xFF8E8E8E) else Color(0xFF666666)
        ServiceTagMultiSelectBottomSheet(
            title = "服务特点",
            subtitle = "可多选，建议勾选最能代表这个${currentCategory}服务的标签",
            options = options,
            initialSelected = serviceFeatureTags,
            surfaceColor = cardColor,
            onBackgroundColor = sheetOnBg,
            onSurfaceVariantColor = sheetMuted,
            dividerColor = dividerColor,
            accentColor = themeAccentRed,
            onDismiss = { showServiceFeaturePicker = false },
            onConfirm = { selected ->
                serviceFeatureTags = ServicePublishTaxonomy.normalizeFeatureTags(currentCategory, selected)
                showServiceFeaturePicker = false
            },
        )
    }

    if (showServiceExtraFeePicker) {
        val currentCategory = ServiceCategories.normalize(serviceCategory)
        val options = ServicePublishTaxonomy.extraFeeTagsFor(currentCategory)
        val sheetOnBg = if (isDarkTheme) Color.White else Color(0xFF111111)
        val sheetMuted = if (isDarkTheme) Color(0xFF8E8E8E) else Color(0xFF666666)
        ServiceTagMultiSelectBottomSheet(
            title = "额外费用",
            subtitle = "可多选，勾选可能由用户另行承担或沟通确认的费用",
            options = options,
            initialSelected = serviceExtraFeeTags,
            surfaceColor = cardColor,
            onBackgroundColor = sheetOnBg,
            onSurfaceVariantColor = sheetMuted,
            dividerColor = dividerColor,
            accentColor = themeAccentRed,
            onDismiss = { showServiceExtraFeePicker = false },
            onConfirm = { selected ->
                serviceExtraFeeTags = ServicePublishTaxonomy.normalizeExtraFeeTags(currentCategory, selected)
                showServiceExtraFeePicker = false
            },
        )
    }

    if (showServiceDeclarationsSheet) {
        val sheetOnBg = if (isDarkTheme) Color.White else Color(0xFF111111)
        val sheetMuted = if (isDarkTheme) Color(0xFF8E8E8E) else Color(0xFF666666)
        ServiceDeclarationsPickerBottomSheet(
            initialExtra = extraServiceDeclarations,
            surfaceColor = cardColor,
            onBackgroundColor = sheetOnBg,
            onSurfaceVariantColor = sheetMuted,
            dividerColor = dividerColor,
            accentColor = themeAccentRed,
            onDismiss = { showServiceDeclarationsSheet = false },
            onConfirm = { normalized ->
                extraServiceDeclarations = normalized
                showServiceDeclarationsSheet = false
            },
        )
    }

    if (showCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text("选择标签") },
            text = {
                Column {
                    ServiceCategories.PRESETS.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    serviceCategory = option
                                    errorMessage = null
                                    showCategoryDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = serviceCategory == option,
                                onClick = {
                                    serviceCategory = option
                                    errorMessage = null
                                    showCategoryDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(option)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCategoryDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 编辑确认弹窗
    if (showEditConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showEditConfirmDialog = false },
            title = { Text(if (isEditMode) "确认保存" else "确认创建") },
            shape = RoundedCornerShape(4.dp),
            text = { 
                Text(
                    if (isEditMode) "修改后保存，服务信息将同步更新，是否继续？" 
                    else "即将发布服务，是否继续？"
                ) 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showEditConfirmDialog = false
                        performSave()
                    }
                ) {
                    Text("确定", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditConfirmDialog = false }) {
                    Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // 参与者选择弹窗
    if (showParticipantDialog) {
        ParticipantSelectionDialog(
            initialSelection = selectedParticipants,
            onDismiss = { showParticipantDialog = false },
            onConfirm = { 
                selectedParticipants = it
                showParticipantDialog = false
            }
        )
    }

    if (showCustomImagePicker) {
        Dialog(
            onDismissRequest = { showCustomImagePicker = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = if (isDarkTheme) Color(0xFF121212) else Color.White
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, top = DialogTitleTopPadding, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { showCustomImagePicker = false }) {
                            Text("取消", color = MaterialTheme.colorScheme.onSurface)
                        }
                        Text(
                            text = "选择照片",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(
                            onClick = {
                                serviceImageUris = pickerSelectedUris
                                showCustomImagePicker = false
                            }
                        ) {
                            Text("完成", color = themeAccentRed)
                        }
                    }

                    Text(
                        text = "已选 ${pickerSelectedUris.size}/$maxServiceImageCount",
                        color = if (isDarkTheme) Color(0xFFBBBBBB) else Color(0xFF6F6F6F),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        contentPadding = PaddingValues(2.dp)
                    ) {
                        items(localGalleryImages.size) { index ->
                            val imageUri = localGalleryImages[index]
                            val selectedOrder = pickerSelectedUris.indexOf(imageUri) + 1
                            val isSelected = selectedOrder > 0

                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clickable {
                                        if (isSelected) {
                                            pickerSelectedUris = pickerSelectedUris - imageUri
                                        } else if (pickerSelectedUris.size < maxServiceImageCount) {
                                            pickerSelectedUris = pickerSelectedUris + imageUri
                                        } else {
                                            android.widget.Toast.makeText(context, "最多上传16张图片", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            ) {
                                coil.compose.AsyncImage(
                                    model = imageUri,
                                    contentDescription = "相册图片",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .background(themeAccentRed.copy(alpha = 0.22f))
                                    )
                                }

                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .size(24.dp),
                                    shape = CircleShape,
                                    color = if (isSelected) themeAccentRed else Color.Black.copy(alpha = 0.28f),
                                    border = BorderStroke(1.dp, Color.White)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = if (isSelected) selectedOrder.toString() else "",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            serviceImageUris = pickerSelectedUris
                            showCustomImagePicker = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeAccentRed),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text("添加（${pickerSelectedUris.size}项）", color = Color.White)
                    }
                }
            }
        }
    }

    if (previewImageUri != null) {
        Dialog(
            onDismissRequest = { previewImageUri = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { previewImageUri = null }
            ) {
                coil.compose.AsyncImage(
                    model = previewImageUri,
                    contentDescription = "图片预览",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
                Text(
                    text = "点击任意位置关闭",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 13.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 30.dp)
                )
            }
        }
    }

}

private fun queryRecentImageUris(context: android.content.Context, limit: Int = 300): List<String> {
    val result = mutableListOf<String>()
    val projection = arrayOf(MediaStore.Images.Media._ID)
    // MediaStore on some OEMs rejects "LIMIT" in sortOrder; cap rows in code instead.
    val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
    val queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

    context.contentResolver.query(
        queryUri,
        projection,
        null,
        null,
        sortOrder
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        while (cursor.moveToNext() && result.size < limit) {
            val id = cursor.getLong(idColumn)
            val contentUri = Uri.withAppendedPath(queryUri, id.toString())
            result.add(contentUri.toString())
        }
    }
    return result
}

private data class RatioStroke(
    val colorArgb: Int,
    val widthRatio: Float,
    val points: List<Offset>
)

private data class RatioTextOverlay(
    val text: String,
    val xRatio: Float,
    val yRatio: Float,
    val sizeRatio: Float,
    val colorArgb: Int
)

@Composable
private fun ServiceImageEditorDialog(
    sourceUri: Uri,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val context = LocalContext.current
    var sourceBitmap by remember(sourceUri) { mutableStateOf<Bitmap?>(null) }
    var loading by remember(sourceUri) { mutableStateOf(true) }
    var brushMode by remember { mutableStateOf(false) }
    var showTextInput by remember { mutableStateOf(false) }
    var pendingText by remember { mutableStateOf("") }
    val strokeColor = Color(0xFFFF3B30)
    val overlays = remember { mutableStateListOf<RatioTextOverlay>() }
    val strokes = remember { mutableStateListOf<RatioStroke>() }

    LaunchedEffect(sourceUri) {
        loading = true
        sourceBitmap = ServiceImageProcessingUtil.loadBitmap(context, sourceUri, maxLongSide = 2560)
        loading = false
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.94f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp, top = DialogTitleTopPadding, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Spacer(modifier = Modifier.weight(1f))
                    Text("图片编辑", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = {
                            val bitmap = sourceBitmap ?: return@TextButton
                            val mutable = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                            val canvas = android.graphics.Canvas(mutable)
                            val width = mutable.width.toFloat()
                            val height = mutable.height.toFloat()

                            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                style = Paint.Style.STROKE
                                strokeCap = Paint.Cap.ROUND
                                strokeJoin = Paint.Join.ROUND
                            }
                            strokes.forEach { stroke ->
                                if (stroke.points.size < 2) return@forEach
                                strokePaint.color = stroke.colorArgb
                                strokePaint.strokeWidth = (stroke.widthRatio * width).coerceAtLeast(4f)
                                val path = android.graphics.Path()
                                val first = stroke.points.first()
                                path.moveTo(first.x * width, first.y * height)
                                stroke.points.drop(1).forEach { p ->
                                    path.lineTo(p.x * width, p.y * height)
                                }
                                canvas.drawPath(path, strokePaint)
                            }

                            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                style = Paint.Style.FILL
                                textAlign = Paint.Align.CENTER
                            }
                            overlays.forEach { item ->
                                textPaint.color = item.colorArgb
                                textPaint.textSize = (item.sizeRatio * width).coerceAtLeast(26f)
                                canvas.drawText(item.text, item.xRatio * width, item.yRatio * height, textPaint)
                            }

                            val savedUri = ServiceImageProcessingUtil.saveHighQualityJpeg(context, mutable, quality = 90)
                            if (savedUri != null) {
                                onSave(savedUri.toString())
                            } else {
                                android.widget.Toast.makeText(context, "图片保存失败，请重试", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) { Text("完成") }
                }

                HorizontalDivider()

                if (loading || sourceBitmap == null) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    val imageBitmap = sourceBitmap!!.asImageBitmap()
                    val ratio = imageBitmap.width.toFloat() / imageBitmap.height.toFloat()
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(12.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black.copy(alpha = 0.08f))
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxWidth()
                                .aspectRatio(ratio)
                        ) {
                            AndroidView(
                                factory = { android.widget.ImageView(it).apply { scaleType = android.widget.ImageView.ScaleType.CENTER_CROP } },
                                update = { view -> view.setImageBitmap(sourceBitmap) },
                                modifier = Modifier.fillMaxSize()
                            )

                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(brushMode) {
                                        if (!brushMode) return@pointerInput
                                        detectDragGestures(
                                            onDragStart = { start ->
                                                val w = size.width.coerceAtLeast(1).toFloat()
                                                val h = size.height.coerceAtLeast(1).toFloat()
                                                strokes.add(
                                                    RatioStroke(
                                                        colorArgb = strokeColor.toArgb(),
                                                        widthRatio = 0.008f,
                                                        points = listOf(Offset(start.x / w, start.y / h))
                                                    )
                                                )
                                            },
                                            onDrag = { change, _ ->
                                                change.consumeAllChanges()
                                                if (strokes.isEmpty()) return@detectDragGestures
                                                val w = size.width.coerceAtLeast(1).toFloat()
                                                val h = size.height.coerceAtLeast(1).toFloat()
                                                val last = strokes.removeAt(strokes.lastIndex)
                                                strokes.add(last.copy(points = last.points + Offset(change.position.x / w, change.position.y / h)))
                                            }
                                        )
                                    }
                            ) {
                                strokes.forEach { stroke ->
                                    if (stroke.points.size < 2) return@forEach
                                    val path = Path().apply {
                                        moveTo(stroke.points.first().x * size.width, stroke.points.first().y * size.height)
                                        stroke.points.drop(1).forEach { p ->
                                            lineTo(p.x * size.width, p.y * size.height)
                                        }
                                    }
                                    drawPath(
                                        path = path,
                                        color = Color(stroke.colorArgb),
                                        style = Stroke(width = (stroke.widthRatio * size.width).coerceAtLeast(3f))
                                    )
                                }
                            }

                            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                overlays.forEach { item ->
                                    Text(
                                        text = item.text,
                                        color = Color(item.colorArgb),
                                        fontSize = (item.sizeRatio * imageBitmap.width / 3f).sp,
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .offset(
                                                x = maxWidth * item.xRatio,
                                                y = maxHeight * item.yRatio
                                            )
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = { brushMode = !brushMode }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Brush, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (brushMode) "标记中" else "添加标记")
                    }
                    OutlinedButton(
                        onClick = { showTextInput = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.TextFields, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("添加文字")
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val stickers = listOf("😊", "❤️", "👍", "⭐", "🔥")
                    stickers.forEach { sticker ->
                        OutlinedButton(
                            onClick = {
                                overlays.add(
                                    RatioTextOverlay(
                                        text = sticker,
                                        xRatio = 0.5f,
                                        yRatio = (0.25f + overlays.size * 0.1f).coerceAtMost(0.8f),
                                        sizeRatio = 0.08f,
                                        colorArgb = Color.White.toArgb()
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.EmojiEmotions, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(sticker, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }

    if (showTextInput) {
        AlertDialog(
            onDismissRequest = { showTextInput = false },
            title = { Text("输入文字") },
            text = {
                OutlinedTextField(
                    value = pendingText,
                    onValueChange = { pendingText = it.take(30) },
                    placeholder = { Text("最多30字") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val text = pendingText.trim()
                        if (text.isNotEmpty()) {
                            overlays.add(
                                RatioTextOverlay(
                                    text = text,
                                    xRatio = 0.5f,
                                    yRatio = (0.3f + overlays.size * 0.08f).coerceAtMost(0.82f),
                                    sizeRatio = 0.055f,
                                    colorArgb = Color.White.toArgb()
                                )
                            )
                        }
                        pendingText = ""
                        showTextInput = false
                    }
                ) { Text("添加") }
            },
            dismissButton = {
                TextButton(onClick = { showTextInput = false }) {
                    Text("取消")
                }
            }
        )
    }
}

private fun fullRefundCancelLeadSummaryLine(days: Int): String =
    when {
        days <= 0 -> "开始服务前可取消并全额退预付款"
        days == 1 -> "提前1天可取消并全额退预付款"
        else -> "提前${days}天可取消并全额退预付款"
    }

@Composable
fun PublishOptionRow(
    label: String,
    value: String,
    valueColor: Color,
    onClick: () -> Unit,
    valueMaxLines: Int = 1,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = value,
            fontSize = 15.sp,
            color = valueColor,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
            maxLines = valueMaxLines,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = ">",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
fun ServiceTextEditDialog(
    title: String,
    initialValue: String,
    placeholder: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember(initialValue) { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                placeholder = { Text(placeholder) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value.trim()) }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSystemInDarkTheme()) Color.LightGray else Color.Gray,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            fontSize = 16.sp,
            color = textColor,
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke()
    }
}
