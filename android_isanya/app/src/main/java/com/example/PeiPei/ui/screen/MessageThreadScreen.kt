// 文件说明：单会话聊天详情界面。

package com.example.Lulu.ui.screen

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.Lulu.data.local.AppDataStore
import com.example.Lulu.data.model.ChatMessage
import com.example.Lulu.data.model.Service
import com.example.Lulu.data.model.ServiceCategories
import com.example.Lulu.data.model.User
import com.example.Lulu.ui.components.ChatLocationMapPickerOverlay
import com.example.Lulu.ui.components.CommonAvatar
import com.example.Lulu.ui.navigation.Screen
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MessageThreadScreen(
    navController: NavController,
    threadId: String?,
    initialInquiryDate: String? = null,
    initialServiceTitle: String? = null,
    entrySource: String? = null,
    initialContextServiceId: String? = null,
    initialContextExperienceId: String? = null,
    initialPeerUserId: String? = null
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val repository = AppDataStore.getRepository()
    val coroutineScope = rememberCoroutineScope()
    val currentUser by AppDataStore.currentUser.collectAsState()
    val contacts by AppDataStore.contacts.collectAsState()
    val services by (
        if (repository != null) {
            repository.allServices.collectAsState(initial = emptyList())
        } else {
            remember { mutableStateOf(emptyList()) }
        }
    )
    val conversation by (
        if (repository != null && !threadId.isNullOrBlank()) {
            repository.getConversationById(threadId).collectAsState(initial = null)
        } else {
            remember { mutableStateOf(null) }
        }
    )
    val messages by (
        if (repository != null && !threadId.isNullOrBlank()) {
            repository.getMessagesFlow(threadId).collectAsState(initial = emptyList())
        } else {
            remember { mutableStateOf(emptyList()) }
        }
    )
    var draft by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var showLocationMapPicker by remember { mutableStateOf(false) }
    var showServiceCardDialog by remember { mutableStateOf(false) }
    var showMoreActions by remember { mutableStateOf(false) }
    var lastImeHeightDp by remember { mutableStateOf(260.dp) }
    val peerIdFromConversation = remember(conversation, currentUser.id) {
        conversation?.participantIds
            ?.firstOrNull { it != currentUser.id }
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }
    val peerIdFromMessages = remember(messages, currentUser.id) {
        messages.firstOrNull { it.senderId.isNotBlank() && it.senderId != currentUser.id }
            ?.senderId?.trim()?.takeIf { it.isNotEmpty() }
    }
    val decodedNavPeerUserId = remember(initialPeerUserId) {
        initialPeerUserId?.let(Uri::decode)?.trim().orEmpty()
    }
    val peerId = remember(decodedNavPeerUserId, peerIdFromConversation, peerIdFromMessages) {
        when {
            decodedNavPeerUserId.isNotEmpty() -> decodedNavPeerUserId
            !peerIdFromConversation.isNullOrBlank() -> peerIdFromConversation
            !peerIdFromMessages.isNullOrBlank() -> peerIdFromMessages
            else -> null
        }
    }
    val peerFromContacts = remember(peerId, contacts) {
        peerId?.let { id -> contacts.firstOrNull { it.id == id } }
    }
    var peerFromHydration by remember { mutableStateOf<User?>(null) }
    LaunchedEffect(threadId, peerId, peerFromContacts) {
        val id = peerId?.trim().orEmpty()
        if (id.isEmpty()) {
            peerFromHydration = null
            return@LaunchedEffect
        }
        if (peerFromContacts != null) {
            peerFromHydration = null
            return@LaunchedEffect
        }
        val known = AppDataStore.getUserById(id)
        if (known != null) {
            peerFromHydration = null
            return@LaunchedEffect
        }
        peerFromHydration = null
        val loaded = AppDataStore.getUserByIdSuspend(id)
        if (peerId?.trim() == id) {
            peerFromHydration = loaded
        }
    }
    val peer = remember(peerFromContacts, peerId, peerFromHydration) {
        peerFromContacts
            ?: peerId?.let { AppDataStore.getUserById(it) }
            ?: peerFromHydration
    }
    val topBarTitle = remember(peer, conversation, peerId) {
        peer?.remarkName?.ifBlank { peer.name }?.takeIf { it.isNotBlank() }
            ?: conversation?.title?.takeIf { it.isNotBlank() }
            ?: if (!peerId.isNullOrBlank()) "对方" else "消息"
    }
    val decodedInquiryDate = remember(initialInquiryDate) {
        initialInquiryDate?.let(Uri::decode).orEmpty()
    }
    val decodedServiceTitle = remember(initialServiceTitle) {
        initialServiceTitle?.let(Uri::decode).orEmpty()
    }
    val decodedContextServiceId = remember(initialContextServiceId) {
        initialContextServiceId?.let(Uri::decode)?.trim().orEmpty()
    }
    val decodedContextExperienceId = remember(initialContextExperienceId) {
        initialContextExperienceId?.let(Uri::decode)?.trim().orEmpty()
    }
    val isHostProfileChatEntry = remember(entrySource) {
        entrySource == "host_profile"
    }
    val inquiryDate = remember(conversation, messages, decodedInquiryDate) {
        buildInquiryDateText(
            conversationCreatedAt = conversation?.createdAt ?: 0L,
            messages = messages,
            initialInquiryDate = decodedInquiryDate
        )
    }
    val inquirySummary = remember(inquiryDate, decodedServiceTitle, messages) {
        buildInquirySummaryText(
            inquiryDate = inquiryDate,
            serviceTitle = resolveInquiryServiceTitle(messages, decodedServiceTitle)
        )
    }
    val serviceIdFromMessages = remember(messages) {
        messages.firstOrNull { it.type == "service_card" }
            ?.extraValue("service_id")
            ?.trim()
            .orEmpty()
    }
    val showServiceLink = remember(
        isHostProfileChatEntry,
        decodedContextServiceId,
        decodedContextExperienceId,
        serviceIdFromMessages
    ) {
        !isHostProfileChatEntry && (
            decodedContextServiceId.isNotEmpty() ||
                decodedContextExperienceId.isNotEmpty() ||
                serviceIdFromMessages.isNotEmpty()
        )
    }
    val serviceCandidates = remember(services) {
        services.sortedByDescending { it.updatedAt }
    }
    val pinnedServiceId = remember(decodedContextServiceId, isHostProfileChatEntry) {
        decodedContextServiceId.takeIf { it.isNotEmpty() && !isHostProfileChatEntry }.orEmpty()
    }
    val pinnedService = remember(pinnedServiceId, serviceCandidates) {
        serviceCandidates.firstOrNull { it.id == pinnedServiceId }
    }
    val showPinnedServiceCard = remember(pinnedServiceId) {
        pinnedServiceId.isNotEmpty()
    }

    fun openLinkedListingFromMeta() {
        when {
            decodedContextServiceId.isNotEmpty() -> {
                val popped = navController.popBackStack()
                if (!popped) {
                    navController.navigate(Screen.ServiceDetail.createRoute(decodedContextServiceId)) {
                        launchSingleTop = true
                    }
                }
            }
            decodedContextExperienceId.isNotEmpty() -> {
                val popped = navController.popBackStack()
                if (!popped) {
                    navController.navigate(Screen.ExperienceDetail.createRoute(decodedContextExperienceId)) {
                        launchSingleTop = true
                    }
                }
            }
            serviceIdFromMessages.isNotEmpty() -> {
                navController.navigate(Screen.ServiceDetail.createRoute(serviceIdFromMessages)) {
                    launchSingleTop = true
                }
            }
        }
    }
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    if (imeBottomPx > 0) {
        lastImeHeightDp = with(density) { imeBottomPx.toDp() }
    }

    suspend fun sendMessage(
        content: String,
        type: String = "text",
        attachmentUrl: String = "",
        attachmentName: String = "",
        attachmentSize: Long = 0L,
        objectKey: String = "",
        extra: Map<String, String> = emptyMap()
    ): Boolean {
        if (repository == null || threadId.isNullOrBlank()) return false
        val result = repository.sendChatMessage(
            conversationId = threadId,
            content = content,
            type = type,
            attachmentUrl = attachmentUrl,
            attachmentName = attachmentName,
            attachmentSize = attachmentSize,
            objectKey = objectKey,
            extra = extra
        )
        if (result != null) {
            repository.markConversationRead(threadId)
        }
        return result != null
    }

    val shouldBackToMessageList = remember(entrySource) {
        entrySource == "messages"
    }

    fun openPeerProfile() {
        val id = peerId?.trim().orEmpty()
        if (id.isNotEmpty()) {
            navController.navigate(Screen.ServiceHostProfile.createRoute(id)) {
                launchSingleTop = true
            }
        }
    }

    fun navigateBack() {
        // 从消息 Tab 进入时：先切回主壳消息 Tab，再立刻 pop，避免先全量拉会话阻塞在 IO 上导致返回「无反应」
        if (shouldBackToMessageList) {
            runCatching {
                navController.getBackStackEntry(Screen.Home.route)
                    .savedStateHandle["force_main_tab"] = 2
            }
        }
        val popped = navController.popBackStack()
        if (!popped) {
            navController.navigate(Screen.Home.route) {
                launchSingleTop = true
                popUpTo(Screen.Home.route) { inclusive = false }
            }
            return
        }
        if (shouldBackToMessageList && repository != null && currentUser.id.isNotBlank()) {
            coroutineScope.launch {
                repository.fetchAndSyncConversations()
            }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null || repository == null || threadId.isNullOrBlank() || sending) {
            return@rememberLauncherForActivityResult
        }
        sending = true
        coroutineScope.launch {
            val tempFile = copyUriToCacheFile(context, uri)
            if (tempFile == null) {
                Toast.makeText(context, "图片读取失败", Toast.LENGTH_SHORT).show()
                sending = false
                return@launch
            }
            val upload = withContext(Dispatchers.IO) {
                repository.uploadChatFile(tempFile)
            }
            val sent = if (upload != null) {
                sendMessage(
                    content = tempFile.nameWithoutExtension.ifBlank { "图片" },
                    type = "image",
                    attachmentUrl = upload.url,
                    attachmentName = tempFile.name,
                    attachmentSize = tempFile.length(),
                    objectKey = upload.objectKey,
                    extra = mapOf(
                        "mime_type" to (context.contentResolver.getType(uri) ?: "image/*")
                    )
                )
            } else {
                false
            }
            tempFile.delete()
            if (!sent) {
                Toast.makeText(context, "图片发送失败", Toast.LENGTH_SHORT).show()
            }
            sending = false
        }
    }

    LaunchedEffect(repository, threadId) {
        if (repository != null && !threadId.isNullOrBlank()) {
            repository.fetchAndSyncMessages(threadId)
            repository.markConversationRead(threadId)
        }
    }

    BackHandler(onBack = ::navigateBack)

    ChatLocationMapPickerOverlay(
        visible = showLocationMapPicker,
        onDismiss = { showLocationMapPicker = false },
        onConfirm = { title, address, latitude, longitude ->
            sending = true
            coroutineScope.launch {
                val sent = sendMessage(
                    content = title.ifBlank { address },
                    type = "location",
                    extra = mapOf(
                        "title" to title,
                        "address" to address,
                        "latitude" to latitude.toString(),
                        "longitude" to longitude.toString(),
                    )
                )
                if (!sent) {
                    Toast.makeText(context, "位置发送失败", Toast.LENGTH_SHORT).show()
                }
                sending = false
            }
            showLocationMapPicker = false
        },
    )

    if (showServiceCardDialog) {
        AlertDialog(
            onDismissRequest = { showServiceCardDialog = false },
            title = { Text("选择服务卡片") },
            text = {
                if (serviceCandidates.isEmpty()) {
                    Text(
                        text = "暂无可发送的服务，先去发布服务后再回来分享。",
                        color = Color(0xFF666666),
                        fontSize = 14.sp
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.height(320.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(serviceCandidates, key = { it.id }) { service ->
                            ServiceSelectorItem(
                                service = service,
                                onClick = {
                                    if (sending) return@ServiceSelectorItem
                                    sending = true
                                    coroutineScope.launch {
                                        val sent = sendMessage(
                                            content = service.title.ifBlank { "服务卡片" },
                                            type = "service_card",
                                            extra = service.toServiceCardExtra()
                                        )
                                        if (!sent) {
                                            Toast.makeText(context, "服务卡片发送失败", Toast.LENGTH_SHORT).show()
                                        }
                                        sending = false
                                    }
                                    showServiceCardDialog = false
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (serviceCandidates.isEmpty()) {
                    TextButton(onClick = {
                        showServiceCardDialog = false
                        navController.navigate(Screen.CreateService.createRoute())
                    }) {
                        Text("去发布")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showServiceCardDialog = false }) {
                    Text("关闭")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            MessageThreadTopBar(
                avatarUrl = peer?.photoUrl.orEmpty(),
                title = topBarTitle,
                onBack = ::navigateBack,
                onOpenPeerProfile = ::openPeerProfile,
                profileNavigationEnabled = !peerId.isNullOrBlank()
            )

            if (showPinnedServiceCard) {
                PinnedServiceContextCard(
                    service = pinnedService,
                    fallbackTitle = decodedServiceTitle,
                    inquirySummary = inquirySummary,
                    onOpen = ::openLinkedListingFromMeta
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                reverseLayout = false,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 0.dp,
                    top = if (showPinnedServiceCard) 12.dp else 14.dp,
                    end = 0.dp,
                    bottom = 14.dp
                )
            ) {
                if (!showPinnedServiceCard) {
                    item {
                        MessageServiceMetaCard(
                            inquiryDate = inquiryDate,
                            inquirySummary = inquirySummary,
                            showConsultationSummary = !isHostProfileChatEntry,
                            showServiceLink = showServiceLink,
                            onShowService = ::openLinkedListingFromMeta
                        )
                    }
                }
                items(messages, key = { it.id }) { message ->
                    MessageBubbleRow(
                        navController = navController,
                        currentUser = currentUser,
                        peer = peer,
                        peerIdForProfile = peerId,
                        message = message
                    )
                }
            }

            MessageInputBar(
                value = draft,
                sending = sending,
                showMoreActions = showMoreActions,
                onValueChange = {
                    if (showMoreActions) {
                        showMoreActions = false
                    }
                    draft = it
                },
                onInputFocus = { showMoreActions = false },
                onToggleMore = {
                    if (showMoreActions) {
                        showMoreActions = false
                    } else {
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                        showMoreActions = true
                    }
                },
                onSend = {
                    val text = draft.trim()
                    if (text.isEmpty() || sending) {
                        return@MessageInputBar
                    }
                    draft = ""
                    sending = true
                    coroutineScope.launch {
                        val sent = sendMessage(content = text)
                        if (!sent) {
                            Toast.makeText(context, "消息发送失败", Toast.LENGTH_SHORT).show()
                            draft = text
                        }
                        sending = false
                    }
                }
            )

            if (showMoreActions) {
                MessageMoreActionsPanel(
                    panelHeight = lastImeHeightDp,
                    onPickAlbum = {
                        showMoreActions = false
                        imagePickerLauncher.launch("image/*")
                    },
                    onTakePhoto = {
                        showMoreActions = false
                        imagePickerLauncher.launch("image/*")
                    },
                    onPickLocation = {
                        showMoreActions = false
                        showLocationMapPicker = true
                    },
                    onVoiceInput = {
                        showMoreActions = false
                        if (!sending) {
                            sending = true
                            coroutineScope.launch {
                                val sent = sendMessage(
                                    content = "【语音输入】",
                                    type = "text"
                                )
                                if (!sent) {
                                    Toast.makeText(context, "语音消息发送失败", Toast.LENGTH_SHORT).show()
                                }
                                sending = false
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PinnedServiceContextCard(
    service: Service?,
    fallbackTitle: String,
    inquirySummary: String,
    onOpen: () -> Unit
) {
    val title = service?.title?.trim().orEmpty().ifBlank {
        fallbackTitle.trim().ifBlank { "当前咨询服务" }
    }
    val coverImage = service?.coverImageUrl?.takeIf { it.isNotBlank() }
        ?: service?.imageUrls?.firstOrNull().orEmpty()
    val summary = buildList {
        val price = service?.priceText?.trim().orEmpty()
        if (price.isNotEmpty()) {
            add(price)
        } else {
            val priceBasis = service?.priceBasisText?.trim().orEmpty()
            if (priceBasis.isNotEmpty()) {
                add(priceBasis)
            }
        }
        val location = service?.location?.trim().orEmpty()
        if (location.isNotEmpty()) {
            add(com.example.Lulu.util.ServiceLocationPolygonCodec.displayLine(location))
        }
    }.joinToString(" · ")

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .clickable(onClick = onOpen),
        color = Color.White,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 10.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (coverImage.isNotBlank()) {
                AsyncImage(
                    model = coverImage,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
            } else {
                Surface(
                    color = Color(0xFFFFF0E4),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.size(68.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = "服务卡片",
                            tint = Color(0xFFFF7A00)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "当前咨询服务",
                    color = Color(0xFF8A8A8A),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = title,
                    color = Color(0xFF222222),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (summary.isNotBlank()) {
                    Text(
                        text = summary,
                        color = Color(0xFF666666),
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                } else if (inquirySummary.isNotBlank()) {
                    Text(
                        text = inquirySummary,
                        color = Color(0xFF666666),
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "查看",
                color = Color(0xFF3B3B3B),
                fontSize = 13.sp,
                textDecoration = TextDecoration.Underline
            )
        }
    }
}

@Composable
private fun MessageThreadTopBar(
    avatarUrl: String,
    title: String,
    onBack: () -> Unit,
    onOpenPeerProfile: () -> Unit,
    profileNavigationEnabled: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, top = 4.dp, end = 4.dp, bottom = 8.dp)
        ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                tint = Color(0xFF333333)
            )
        }

        Text(
            text = "详情",
            color = Color(0xFF333333),
            fontSize = 15.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .clickable(
                    enabled = profileNavigationEnabled,
                    onClick = onOpenPeerProfile
                )
                .padding(horizontal = 12.dp, vertical = 12.dp)
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CommonAvatar(
                imageUrl = avatarUrl,
                name = title,
                modifier = Modifier
                    .size(48.dp)
                    .clickable(
                        enabled = profileNavigationEnabled,
                        onClick = onOpenPeerProfile
                    ),
                shape = CircleShape,
                contentDescription = title
            )
            Text(
                text = title,
                color = Color(0xFF222222),
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
    }
}

@Composable
private fun MessageServiceMetaCard(
    inquiryDate: String,
    inquirySummary: String,
    showConsultationSummary: Boolean,
    showServiceLink: Boolean,
    onShowService: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = inquiryDate,
            color = Color(0xFF4F4F4F),
            fontSize = 14.sp
        )
        if (showConsultationSummary) {
            Text(
                text = inquirySummary,
                color = Color(0xFF6D6D6D),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        if (showServiceLink) {
            Text(
                text = "显示服务",
                color = Color(0xFF3B3B3B),
                fontSize = 14.sp,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clickable(onClick = onShowService)
            )
        }
    }
}

@Composable
private fun MessageBubbleRow(
    navController: NavController,
    currentUser: User,
    peer: User?,
    peerIdForProfile: String?,
    message: ChatMessage
) {
    val fromSelf = message.senderId == currentUser.id
    val avatarUrl = if (fromSelf) currentUser.photoUrl else peer?.photoUrl.orEmpty()
    val displayName = if (fromSelf) currentUser.name else peer?.name.orEmpty()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        horizontalArrangement = if (fromSelf) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!fromSelf) {
            val profileId = peerIdForProfile?.trim().orEmpty()
            CommonAvatar(
                imageUrl = avatarUrl,
                name = displayName,
                modifier = Modifier
                    .size(40.dp)
                    .clickable(
                        enabled = profileId.isNotEmpty(),
                        onClick = {
                            navController.navigate(Screen.ServiceHostProfile.createRoute(profileId)) {
                                launchSingleTop = true
                            }
                        }
                    ),
                shape = RoundedCornerShape(4.dp),
                contentDescription = displayName
            )
            Spacer(modifier = Modifier.size(10.dp))
        }

        Column(
            modifier = Modifier.width(260.dp),
            horizontalAlignment = if (fromSelf) Alignment.End else Alignment.Start
        ) {
            when (message.type) {
                "image" -> {
                    AsyncImage(
                        model = message.attachmentUrl,
                        contentDescription = "图片消息",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(width = 166.dp, height = 208.dp)
                            .clip(RoundedCornerShape(18.dp))
                    )
                }

                "location" -> {
                    LocationMessageCard(
                        message = message,
                        fromSelf = fromSelf
                    )
                }

                "service_card" -> {
                    ServiceCardMessage(
                        message = message,
                        fromSelf = fromSelf,
                        onOpen = {
                            val serviceId = message.extraValue("service_id")
                            if (serviceId.isNotBlank()) {
                                navController.navigate(Screen.ServiceDetail.createRoute(serviceId))
                            }
                        }
                    )
                }

                else -> {
                    Surface(
                        color = if (fromSelf) Color(0xFF3B3D43) else Color(0xFFEFEFEF),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text(
                            text = message.content,
                            color = if (fromSelf) Color.White else Color(0xFF222222),
                            fontSize = 16.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                }
            }
        }

        if (fromSelf) {
            Spacer(modifier = Modifier.size(10.dp))
            CommonAvatar(
                imageUrl = avatarUrl,
                name = displayName,
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(4.dp),
                contentDescription = displayName
            )
        }
    }
}

@Composable
private fun LocationMessageCard(
    message: ChatMessage,
    fromSelf: Boolean
) {
    Surface(
        color = if (fromSelf) Color(0xFFDDF5C8) else Color.White,
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color(0xFFFFEFE1),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "位置",
                    tint = Color(0xFFFF7A00),
                    modifier = Modifier.padding(10.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = message.extraValue("title").ifBlank { message.content.ifBlank { "位置" } },
                    color = Color(0xFF222222),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                val address = message.extraValue("address")
                if (address.isNotBlank()) {
                    Text(
                        text = address,
                        color = Color(0xFF7A7A7A),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ServiceCardMessage(
    message: ChatMessage,
    fromSelf: Boolean,
    onOpen: () -> Unit
) {
    Surface(
        color = if (fromSelf) Color(0xFFDDF5C8) else Color.White,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.clickable(onClick = onOpen)
    ) {
        Column(modifier = Modifier.width(220.dp)) {
            val coverImage = message.extraValue("cover_image")
            if (coverImage.isNotBlank()) {
                AsyncImage(
                    model = coverImage,
                    contentDescription = "服务封面",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(116.dp)
                        .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                )
            }
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = message.extraValue("title").ifBlank { message.content.ifBlank { "服务卡片" } },
                    color = Color(0xFF222222),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val price = message.extraValue("price_text")
                if (price.isNotBlank()) {
                    Text(
                        text = price,
                        color = Color(0xFFFF6A00),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                val category = message.extraValue("category")
                if (category.isNotBlank()) {
                    Text(
                        text = "类别：$category",
                        color = Color(0xFF666666),
                        fontSize = 12.sp
                    )
                }
                val serviceMode = message.extraValue("service_mode")
                if (serviceMode.isNotBlank()) {
                    Text(
                        text = "服务方式：$serviceMode",
                        color = Color(0xFF666666),
                        fontSize = 12.sp
                    )
                }
                val location = message.extraValue("location")
                if (location.isNotBlank()) {
                    Text(
                        text = location,
                        color = Color(0xFF666666),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "点击查看详情",
                    color = Color(0xFF9A9A9A),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun ServiceSelectorItem(
    service: Service,
    onClick: () -> Unit
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val coverImage = service.coverImageUrl
            if (coverImage.isNotBlank()) {
                AsyncImage(
                    model = coverImage,
                    contentDescription = service.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                Surface(
                    color = Color(0xFFFFF0E4),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(58.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = "服务",
                            tint = Color(0xFFFF7A00)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = service.title.ifBlank { "未命名服务" },
                    color = Color(0xFF222222),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (service.priceText.isNotBlank()) {
                    Text(
                        text = service.priceText,
                        color = Color(0xFFFF6A00),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                val summary = buildList {
                    val cat = ServiceCategories.normalize(service.category)
                    if (cat.isNotBlank()) add(cat)
                    if (service.serviceMode.isNotBlank()) add(service.serviceMode)
                    if (service.location.isNotBlank()) add(
                        com.example.Lulu.util.ServiceLocationPolygonCodec.displayLine(service.location)
                    )
                }.joinToString(" · ")
                if (summary.isNotBlank()) {
                    Text(
                        text = summary,
                        color = Color(0xFF777777),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageInputBar(
    value: String,
    sending: Boolean,
    showMoreActions: Boolean,
    onValueChange: (String) -> Unit,
    onInputFocus: () -> Unit,
    onToggleMore: () -> Unit,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = Color(0xFFF2F2F2),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.weight(1f)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                enabled = !sending,
                textStyle = TextStyle(
                    color = Color(0xFF222222),
                    fontSize = 16.sp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            onInputFocus()
                        }
                    },
                decorationBox = { innerTextField ->
                    if (value.isBlank()) {
                        Text(
                            text = "撰写消息......",
                            color = Color(0xFF9A9A9A),
                            fontSize = 16.sp
                        )
                    }
                    innerTextField()
                }
            )
        }
        Surface(
            color = if (showMoreActions) Color(0xFFDCDCDC) else Color(0xFFE8E8E8),
            shape = CircleShape,
            modifier = Modifier
                .padding(start = 10.dp)
                .size(32.dp)
                .clickable(enabled = !sending, onClick = onToggleMore)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "更多功能",
                    tint = Color(0xFF646464),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Surface(
            color = if (value.isBlank() || sending) Color(0xFFE8E8E8) else Color(0xFFB8B8B8),
            shape = CircleShape,
            modifier = Modifier
                .padding(start = 10.dp)
                .size(32.dp)
                .let {
                    if (value.isBlank() || sending) it else it.clickable(onClick = onSend)
                }
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (sending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF6F6F6F)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "发送",
                        tint = if (value.isBlank()) Color(0xFF757575) else Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageMoreActionsPanel(
    panelHeight: androidx.compose.ui.unit.Dp,
    onPickAlbum: () -> Unit,
    onTakePhoto: () -> Unit,
    onPickLocation: () -> Unit,
    onVoiceInput: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(panelHeight)
            .background(Color.White)
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MessageActionCard(
                icon = Icons.Default.PhotoLibrary,
                label = "发送图片",
                onClick = onPickAlbum
            )
            MessageActionCard(
                icon = Icons.Default.CameraAlt,
                label = "拍摄",
                onClick = onTakePhoto
            )
            MessageActionCard(
                icon = Icons.Default.LocationOn,
                label = "发送位置",
                onClick = onPickLocation
            )
            MessageActionCard(
                icon = Icons.Default.Mic,
                label = "语音输入",
                onClick = onVoiceInput
            )
        }
    }
}

@Composable
private fun MessageActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(78.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color(0xFF242424),
                    modifier = Modifier.size(30.dp)
                )
            }
        }
        Text(
            text = label,
            color = Color(0xFF5E5E5E),
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}

private fun buildInquiryDateText(
    conversationCreatedAt: Long,
    messages: List<ChatMessage>,
    initialInquiryDate: String = ""
): String {
    if (initialInquiryDate.isNotBlank()) {
        return initialInquiryDate
    }
    val firstServiceCard = messages.firstOrNull { it.type == "service_card" }
    val start = firstServiceCard?.extraValue("start_date").orEmpty()
    val end = firstServiceCard?.extraValue("end_date").orEmpty()
    if (start.isNotBlank() && end.isNotBlank()) {
        return "${start}至${end}"
    }
    if (conversationCreatedAt > 0L) {
        val formatter = SimpleDateFormat("yyyy年M月d日", Locale.getDefault())
        return formatter.format(Date(conversationCreatedAt))
    }
    return "2021年2月9日至19日"
}

private fun resolveInquiryServiceTitle(
    messages: List<ChatMessage>,
    initialServiceTitle: String
): String {
    if (initialServiceTitle.isNotBlank()) {
        return initialServiceTitle
    }
    return messages
        .firstOrNull { it.type == "service_card" }
        ?.extraValue("title")
        .orEmpty()
}

private fun buildInquirySummaryText(
    inquiryDate: String,
    serviceTitle: String
): String {
    val normalizedTitle = serviceTitle.trim()
    val serviceLabel = when {
        normalizedTitle.isBlank() -> "服务"
        normalizedTitle.endsWith("服务") -> normalizedTitle
        else -> "${normalizedTitle}服务"
    }
    return "关于${inquiryDate}${serviceLabel}的咨询"
}

private fun ChatMessage.extraValue(key: String): String = extra[key].orEmpty()

private fun Service.toServiceCardExtra(): Map<String, String> {
    return mapOf(
        "service_id" to id,
        "title" to title,
        "description" to description,
        "price_text" to priceText,
        "category" to ServiceCategories.normalize(category),
        "service_mode" to serviceMode,
        "location" to location,
        "cover_image" to coverImageUrl,
        "creator_id" to creatorId
    )
}

private suspend fun copyUriToCacheFile(
    context: Context,
    uri: Uri
): File? = withContext(Dispatchers.IO) {
    try {
        val mimeType = context.contentResolver.getType(uri).orEmpty()
        val extension = when {
            mimeType.contains("png") -> "png"
            mimeType.contains("webp") -> "webp"
            mimeType.contains("gif") -> "gif"
            else -> "jpg"
        }
        val file = File(context.cacheDir, "chat_${System.currentTimeMillis()}.$extension")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        } ?: return@withContext null
        file
    } catch (_: Exception) {
        null
    }
}
