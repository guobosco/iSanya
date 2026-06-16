// 文件说明：当前用户编辑个人资料的界面。

package com.example.Lulu.ui.screen

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.example.Lulu.data.local.AppDataStore
import com.example.Lulu.data.model.MAX_PROFILE_WALL_IMAGE_COUNT
import com.example.Lulu.data.model.heightWeightEditRowValue
import com.example.Lulu.data.model.withAvatarIncludedInPhotoWall
import com.example.Lulu.ui.components.ProfileHeightWeightBottomSheet
import com.example.Lulu.ui.components.ProfileInterestsPickerDialog
import com.example.Lulu.ui.components.ProfileItem
import com.example.Lulu.ui.components.ProfileOptionPickerBottomSheet
import com.example.Lulu.ui.components.ProfileUniversityPickerBottomSheet
import com.example.Lulu.ui.components.ProfileTextEditBottomSheet
import com.example.Lulu.ui.components.defaultProfileInterestPresets
import com.example.Lulu.ui.components.RegionPickerDialog
import java.util.Calendar
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import coil.compose.AsyncImage
import com.example.Lulu.data.remote.RetrofitClient
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.launch
import com.yalantis.ucrop.UCrop
import android.content.Intent
import android.app.Activity
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.ui.graphics.toArgb
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProfileEditScreen(navController: NavController) {
    MyProfileEditContent(onClose = { navController.popBackStack() })
}

@Composable
fun EditProfileBottomDialog(onDismissRequest: () -> Unit) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = Color.White,
                shadowElevation = 16.dp
            ) {
                MyProfileEditContent(
                    onClose = onDismissRequest,
                    containerColor = Color.White
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun MyProfileEditContent(
    onClose: () -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.surface
) {
    val user by AppDataStore.currentUser.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val dismissKeyboard: () -> Unit = {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        Unit
    }

    // Dialog States
    var showNameDialog by remember { mutableStateOf(false) }
    var showIdDialog by remember { mutableStateOf(false) }
    var showGenderDialog by remember { mutableStateOf(false) }
    var showRegionDialog by remember { mutableStateOf(false) }
    var showPhoneDialog by remember { mutableStateOf(false) }
    var showBirthDecadeDialog by remember { mutableStateOf(false) }
    var showMiddleSchoolSongDialog by remember { mutableStateOf(false) }
    var showJobTitleDialog by remember { mutableStateOf(false) }
    var showEducationDialog by remember { mutableStateOf(false) }
    var showHeightWeightDialog by remember { mutableStateOf(false) }
    var showInterestsPicker by remember { mutableStateOf(false) }
    var profileEditTab by remember { mutableIntStateOf(0) }
    var isAvatarSaving by remember { mutableStateOf(false) }
    var isPhotoWallUploading by remember { mutableStateOf(false) }
    var isProfileSyncing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    // Theme colors for uCrop
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()

    val uCropLauncher = rememberLauncherForActivityResult(
        contract = object : ActivityResultContract<Uri, Uri?>() {
            override fun createIntent(context: android.content.Context, input: Uri): Intent {
                val destinationUri = Uri.fromFile(File(context.cacheDir, "cropped_avatar_${System.currentTimeMillis()}.jpg"))
                val options = UCrop.Options().apply {
                    setToolbarColor(android.graphics.Color.BLACK)
                    setStatusBarColor(android.graphics.Color.BLACK)
                    setToolbarWidgetColor(android.graphics.Color.WHITE)
                    setRootViewBackgroundColor(android.graphics.Color.BLACK)
                    setActiveControlsWidgetColor(primaryColor)
                    setToolbarTitle("裁剪图片")
                    withAspectRatio(1f, 1f)
                    withMaxResultSize(500, 500)
                    setCircleDimmedLayer(false)
                    setShowCropGrid(true)
                    setHideBottomControls(false) // Show bottom controls for rotation
                }
                return UCrop.of(input, destinationUri)
                    .withOptions(options)
                    .getIntent(context)
            }

            override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
                if (resultCode == Activity.RESULT_OK && intent != null) {
                    return UCrop.getOutput(intent)
                }
                return null
            }
        }
    ) { croppedUri: Uri? ->
        if (croppedUri != null) {
            coroutineScope.launch {
                isAvatarSaving = true
                try {
                    val repository = AppDataStore.getRepository()
                    if (repository != null) {
                        Toast.makeText(context, "正在上传头像...", Toast.LENGTH_SHORT).show()
                        val serverUrl = com.example.Lulu.util.AvatarUploadUtil.processAndUploadAvatar(context, croppedUri, repository)
                        if (serverUrl != null) {
                            val updatedUser = AppDataStore.currentUser.value
                                .withAvatarIncludedInPhotoWall(serverUrl)
                            val result = repository.syncCurrentUserProfile(updatedUser)
                            result.onSuccess { savedUser ->
                                AppDataStore.replaceCurrentUser(savedUser)
                                Toast.makeText(context, "头像已更新", Toast.LENGTH_SHORT).show()
                            }.onFailure { error ->
                                Toast.makeText(context, error.message ?: "头像保存失败，请重试", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "头像上传失败", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "初始化错误，无法上传", Toast.LENGTH_SHORT).show()
                    }
                } finally {
                    isAvatarSaving = false
                }
            }
        }
    }

    val avatarLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            uCropLauncher.launch(uri)
        }
    }

    val photoWallLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            val repository = AppDataStore.getRepository()
            if (repository == null) {
                Toast.makeText(context, "初始化错误，无法上传", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val currentUrls = AppDataStore.currentUser.value.profileImageUrls
            val remainingCount = MAX_PROFILE_WALL_IMAGE_COUNT - currentUrls.size
            if (remainingCount <= 0) {
                Toast.makeText(
                    context,
                    "照片墙最多上传 $MAX_PROFILE_WALL_IMAGE_COUNT 张",
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }
            val pendingUris = uris.distinct().take(remainingCount)
            if (pendingUris.size < uris.distinct().size) {
                Toast.makeText(
                    context,
                    "最多再添加 $remainingCount 张照片",
                    Toast.LENGTH_SHORT
                ).show()
            }
            isPhotoWallUploading = true
            try {
                Toast.makeText(context, "正在上传照片墙...", Toast.LENGTH_SHORT).show()
                val uploadedUrls = mutableListOf<String>()
                pendingUris.forEach { uri ->
                    val serverUrl =
                        com.example.Lulu.util.AvatarUploadUtil.processAndUploadProfilePhoto(
                            context = context,
                            uri = uri,
                            repository = repository
                        )
                    if (!serverUrl.isNullOrBlank()) {
                        uploadedUrls += serverUrl
                    }
                }
                if (uploadedUrls.isEmpty()) {
                    Toast.makeText(context, "照片上传失败，请重试", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val latest = AppDataStore.currentUser.value
                val mergedUrls = (latest.profileImageUrls + uploadedUrls)
                    .distinct()
                    .take(MAX_PROFILE_WALL_IMAGE_COUNT)
                val updatedUser = latest.copy(
                    profileImageUrls = mergedUrls,
                    updatedAt = System.currentTimeMillis()
                )
                val result = repository.syncCurrentUserProfile(updatedUser)
                result.onSuccess { savedUser ->
                    AppDataStore.replaceCurrentUser(savedUser)
                    val successCount = uploadedUrls.size.coerceAtMost(
                        savedUser.profileImageUrls.size - currentUrls.size
                    )
                    Toast.makeText(
                        context,
                        "已添加 ${successCount.coerceAtLeast(1)} 张照片",
                        Toast.LENGTH_SHORT
                    ).show()
                }.onFailure { error ->
                    Toast.makeText(
                        context,
                        error.message ?: "照片墙保存失败，请重试",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } finally {
                isPhotoWallUploading = false
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("编辑个人资料", fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            dismissKeyboard()
                            onClose()
                        },
                        enabled = !isProfileSyncing && !isAvatarSaving && !isPhotoWallUploading
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = containerColor
                )
            )
        },
        bottomBar = {
            Surface(
                color = containerColor,
                shadowElevation = 6.dp
            ) {
                Button(
                    onClick = {
                        dismissKeyboard()
                        coroutineScope.launch {
                            isProfileSyncing = true
                            try {
                                var u = AppDataStore.currentUser.value
                                val trimmed = u.signature.trim()
                                if (trimmed != u.signature) {
                                    u = u.copy(signature = trimmed, updatedAt = System.currentTimeMillis())
                                    AppDataStore.updateCurrentUser(u)
                                }
                                val latest = AppDataStore.currentUser.value
                                val repository = AppDataStore.getRepository()
                                val result = repository?.syncCurrentUserProfile(latest)
                                    ?: Result.success(latest)
                                result.onSuccess { savedUser ->
                                    AppDataStore.replaceCurrentUser(savedUser)
                                    Toast.makeText(context, "资料已保存", Toast.LENGTH_SHORT).show()
                                    onClose()
                                }.onFailure { error ->
                                    Toast.makeText(
                                        context,
                                        error.message ?: "保存失败，请检查网络后重试",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } finally {
                                isProfileSyncing = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !isProfileSyncing && !isAvatarSaving && !isPhotoWallUploading,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222))
                ) {
                    Text(
                        if (isProfileSyncing || isPhotoWallUploading) "保存中…" else "完成",
                        color = Color.White,
                        fontSize = 18.sp
                    )
                }
            }
        },
        containerColor = containerColor
    ) { paddingValues ->
        val tabInk = MaterialTheme.colorScheme.onSurface
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(
                selectedTabIndex = profileEditTab,
                modifier = Modifier.fillMaxWidth(),
                containerColor = containerColor,
                contentColor = tabInk,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[profileEditTab]),
                        height = 2.dp,
                        color = tabInk
                    )
                },
                divider = {}
            ) {
                Tab(
                    selected = profileEditTab == 0,
                    onClick = {
                        dismissKeyboard()
                        profileEditTab = 0
                    },
                    text = { Text("基本资料", fontSize = 14.sp, maxLines = 1) }
                )
                Tab(
                    selected = profileEditTab == 1,
                    onClick = {
                        dismissKeyboard()
                        profileEditTab = 1
                    },
                    text = { Text("工作与成长", fontSize = 14.sp, maxLines = 1) }
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp, horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (profileEditTab) {
                    0 -> {
                        ProfileEditTabIntroBlock(
                            title = "基本资料",
                            body = "完善头像、基本信息和自我介绍，让社区里的朋友更了解你。"
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Box(
                            contentAlignment = Alignment.BottomEnd,
                            modifier = Modifier
                                .size(168.dp)
                                .clickable(enabled = !isAvatarSaving && !isPhotoWallUploading) {
                                    avatarLauncher.launch("image/*")
                                }
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                if (user.photoUrl.isNotEmpty()) {
                                    AsyncImage(
                                        model = RetrofitClient.normalizeBackendMediaUrlForDisplay(user.photoUrl),
                                        contentDescription = "Avatar",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = user.name.take(1),
                                            style = MaterialTheme.typography.displayMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 4.dp,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .offset(y = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CameraAlt,
                                        contentDescription = "修改头像",
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "编辑",
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(22.dp))
                        EdgeToEdgeGroup(backgroundColor = MaterialTheme.colorScheme.surface) {
                            ProfileItem(
                                label = "名字",
                                value = user.name,
                                showArrow = true,
                                onClick = { showNameDialog = true }
                            )
                            Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                            ProfileItem(
                                label = "性别",
                                value = user.gender.ifEmpty { "未设置" },
                                showArrow = true,
                                onClick = { showGenderDialog = true }
                            )
                            Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                            ProfileItem(
                                label = "城市",
                                value = user.region.ifEmpty { "未设置" },
                                showArrow = true,
                                onClick = { showRegionDialog = true }
                            )
                            Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                            ProfileItem(
                                label = "爱野 ID",
                                value = user.peiPeiId,
                                showArrow = true,
                                onClick = {
                                    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                                    if (user.lastIdModificationYear == currentYear && user.idModificationCount >= 3) {
                                        Toast.makeText(context, "爱野 ID一年只能修改3次", Toast.LENGTH_SHORT).show()
                                    } else {
                                        showIdDialog = true
                                    }
                                }
                            )
                            Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                            ProfileItem(
                                label = "手机号",
                                value = user.phoneNumber.ifEmpty { "未绑定" },
                                showArrow = true,
                                onClick = { showPhoneDialog = true }
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        AboutMeSectionCard(
                            signature = user.signature,
                            signatureLength = user.signature.length,
                            photoUrls = user.profileImageUrls,
                            maxPhotoCount = MAX_PROFILE_WALL_IMAGE_COUNT,
                            isUploading = isPhotoWallUploading,
                            onUploadClick = { photoWallLauncher.launch("image/*") },
                            onRemovePhoto = { photoUrl ->
                                val latest = AppDataStore.currentUser.value
                                AppDataStore.updateCurrentUser(
                                    latest.copy(
                                        profileImageUrls = latest.profileImageUrls.filterNot { it == photoUrl },
                                        updatedAt = System.currentTimeMillis()
                                    )
                                )
                            },
                            onSignatureChange = { new ->
                                if (new.length <= 500) {
                                    val latest = AppDataStore.currentUser.value
                                    AppDataStore.updateCurrentUser(
                                        latest.copy(signature = new, updatedAt = System.currentTimeMillis())
                                    )
                                }
                            },
                            onDone = dismissKeyboard
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        InterestSectionCard(
                            tags = user.tags,
                            onEditClick = { showInterestsPicker = true }
                        )
                    }
                    1 -> {
                        ProfileEditTabIntroBlock(
                            title = "工作与成长",
                            body = "补充职业、求学与成长经历，让其他用户更了解你。"
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        EdgeToEdgeGroup(backgroundColor = MaterialTheme.colorScheme.surface) {
                            ProfileItem(
                                label = "我的工作",
                                value = user.jobTitle.ifEmpty { "未设置" },
                                icon = Icons.Default.Work,
                                showArrow = true,
                                onClick = { showJobTitleDialog = true }
                            )
                            Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                            ProfileItem(
                                label = "曾就读于",
                                value = user.education.ifEmpty { "未设置" },
                                icon = Icons.Default.School,
                                showArrow = true,
                                onClick = { showEducationDialog = true }
                            )
                            Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                            ProfileItem(
                                label = "我出生的年代",
                                value = user.birthDecade.ifEmpty { "未设置" },
                                icon = Icons.Default.Cake,
                                showArrow = true,
                                onClick = { showBirthDecadeDialog = true }
                            )
                            Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                            ProfileItem(
                                label = "身高体重",
                                value = user.heightWeightEditRowValue(),
                                icon = Icons.Default.FitnessCenter,
                                showArrow = true,
                                onClick = { showHeightWeightDialog = true }
                            )
                            Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                            ProfileItem(
                                label = "中学时最喜欢的歌曲",
                                value = user.middleSchoolFavoriteSong.ifEmpty { "未设置" },
                                icon = Icons.Default.MusicNote,
                                showArrow = true,
                                onClick = { showMiddleSchoolSongDialog = true }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showNameDialog) {
        ProfileTextEditBottomSheet(
            title = "你的名字是？",
            subtitle = "好名字更容易被记住。",
            fieldLabel = "名字",
            initialValue = user.name,
            maxLength = 10,
            validator = { input ->
                when {
                    input.isEmpty() -> "名字不能为空"
                    input.contains(" ") -> "名字不能包含空格"
                    else -> null
                }
            },
            onDismiss = { showNameDialog = false },
            onSave = {
                AppDataStore.updateCurrentUser(user.copy(name = it, updatedAt = System.currentTimeMillis()))
                showNameDialog = false
            }
        )
    }

    if (showIdDialog) {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        ProfileTextEditBottomSheet(
            title = "修改爱野 ID",
            subtitle = "爱野 ID是你在社区中的唯一标识，一年最多可修改 3 次。",
            fieldLabel = "爱野 ID（6–20 位字母、数字或下划线）",
            initialValue = user.peiPeiId,
            maxLength = 20,
            infoLinkText = "命名规则说明",
            onInfoLinkClick = { Toast.makeText(context, "6–20 位字母、数字或下划线", Toast.LENGTH_SHORT).show() },
            validator = {
                if (!it.matches("^[a-zA-Z0-9_]{6,20}$".toRegex()))
                    "格式错误：6-20位字母、数字或下划线"
                else null
            },
            onDismiss = { showIdDialog = false },
            onSave = {
                val newCount = if (user.lastIdModificationYear == currentYear) user.idModificationCount + 1 else 1
                AppDataStore.updateCurrentUser(
                    user.copy(
                        peiPeiId = it,
                        updatedAt = System.currentTimeMillis(),
                        idModificationCount = newCount,
                        lastIdModificationYear = currentYear
                    )
                )
                showIdDialog = false
                Toast.makeText(context, "爱野 ID修改成功", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showPhoneDialog) {
        ProfileTextEditBottomSheet(
            title = "绑定手机号",
            subtitle = "用于登录与安全验证。更新后需要重新完成短信验证。",
            fieldLabel = "手机号",
            initialValue = user.phoneNumber,
            maxLength = 11,
            isPhone = true,
            infoLinkText = "为什么需要手机号？",
            onInfoLinkClick = { Toast.makeText(context, "功能开发中", Toast.LENGTH_SHORT).show() },
            onDismiss = { showPhoneDialog = false },
            onSave = {
                AppDataStore.updateCurrentUser(
                    user.copy(phoneNumber = it, isPhoneVerified = false, updatedAt = System.currentTimeMillis())
                )
                showPhoneDialog = false
                Toast.makeText(context, "手机号已更新，请重新验证", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showRegionDialog) {
        RegionPickerDialog(
            initialRegion = user.region,
            onDismiss = { showRegionDialog = false },
            onConfirm = { 
                AppDataStore.updateCurrentUser(user.copy(region = it, updatedAt = System.currentTimeMillis()))
                showRegionDialog = false
            }
        )
    }

    if (showGenderDialog) {
        ProfileOptionPickerBottomSheet(
            title = "你的性别是？",
            subtitle = "可随时修改。",
            options = listOf("男", "女"),
            initialSelection = user.gender,
            onDismiss = { showGenderDialog = false },
            onPick = { picked ->
                AppDataStore.updateCurrentUser(user.copy(gender = picked, updatedAt = System.currentTimeMillis()))
                showGenderDialog = false
            }
        )
    }

    if (showBirthDecadeDialog) {
        val decadeValues = listOf(
            "50后", "55后", "60后", "65后", "70后", "75后", "80后", "85后",
            "90后", "95后", "00后", "05后", "10后", "15后", "20后", "25后", ""
        )
        ProfileOptionPickerBottomSheet(
            title = "你出生在哪个年代？",
            subtitle = "选填。约五年一档（如 90后、95后、00后）。",
            options = decadeValues,
            initialSelection = user.birthDecade,
            onDismiss = { showBirthDecadeDialog = false },
            onPick = { picked ->
                AppDataStore.updateCurrentUser(
                    user.copy(birthDecade = picked, updatedAt = System.currentTimeMillis())
                )
                showBirthDecadeDialog = false
            }
        )
    }

    if (showMiddleSchoolSongDialog) {
        ProfileTextEditBottomSheet(
            title = "中学时最喜欢的歌曲？",
            subtitle = "分享一首对你有意义的歌，让资料更有温度。",
            fieldLabel = "歌曲或歌手",
            initialValue = user.middleSchoolFavoriteSong,
            maxLength = 80,
            onDismiss = { showMiddleSchoolSongDialog = false },
            onSave = {
                AppDataStore.updateCurrentUser(
                    user.copy(middleSchoolFavoriteSong = it.trim(), updatedAt = System.currentTimeMillis())
                )
                showMiddleSchoolSongDialog = false
            }
        )
    }

    if (showJobTitleDialog) {
        ProfileTextEditBottomSheet(
            title = "你从事什么工作？",
            subtitle = "可以写职位、行业或一句概括。",
            fieldLabel = "我的职业或工作",
            initialValue = user.jobTitle,
            maxLength = 60,
            onDismiss = { showJobTitleDialog = false },
            onSave = {
                AppDataStore.updateCurrentUser(
                    user.copy(jobTitle = it.trim(), updatedAt = System.currentTimeMillis())
                )
                showJobTitleDialog = false
            }
        )
    }

    if (showInterestsPicker) {
        ProfileInterestsPickerDialog(
            initialSelected = user.tags.toSet(),
            onDismissRequest = { showInterestsPicker = false },
            onSave = { labels ->
                val latest = AppDataStore.currentUser.value
                AppDataStore.updateCurrentUser(
                    latest.copy(tags = labels, updatedAt = System.currentTimeMillis())
                )
                showInterestsPicker = false
                Toast.makeText(context, "兴趣已保存", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showEducationDialog) {
        ProfileUniversityPickerBottomSheet(
            initialValue = user.education,
            maxLength = 80,
            onDismiss = { showEducationDialog = false },
            onSave = { picked ->
                val latest = AppDataStore.currentUser.value
                AppDataStore.updateCurrentUser(
                    latest.copy(education = picked.trim(), updatedAt = System.currentTimeMillis())
                )
                showEducationDialog = false
            }
        )
    }

    if (showHeightWeightDialog) {
        val latest = AppDataStore.currentUser.value
        ProfileHeightWeightBottomSheet(
            heightCm = latest.heightCm,
            weightKg = latest.weightKg,
            heightWeightPrivate = latest.heightWeightPrivate,
            onDismiss = { showHeightWeightDialog = false },
            onSave = { h, w, p ->
                val u = AppDataStore.currentUser.value
                AppDataStore.updateCurrentUser(
                    u.copy(
                        heightCm = h,
                        weightKg = w,
                        heightWeightPrivate = p,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                showHeightWeightDialog = false
            }
        )
    }
}

@Composable
private fun ProfileEditTabIntroBlock(
    title: String,
    body: String,
    linkText: String? = null,
    onLinkClick: (() -> Unit)? = null
) {
    val titleColor = MaterialTheme.colorScheme.onSurface
    val bodyColor = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = titleColor,
            lineHeight = 30.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = body,
            fontSize = 14.sp,
            lineHeight = 22.sp,
            color = bodyColor
        )
        if (linkText != null && onLinkClick != null) {
            TextButton(
                onClick = onLinkClick,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = linkText,
                    fontSize = 13.sp,
                    color = bodyColor,
                    textDecoration = TextDecoration.Underline
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AboutMeSectionCard(
    signature: String,
    signatureLength: Int,
    photoUrls: List<String>,
    maxPhotoCount: Int,
    isUploading: Boolean,
    onUploadClick: () -> Unit,
    onRemovePhoto: (String) -> Unit,
    onSignatureChange: (String) -> Unit,
    onDone: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Text(
                "关于我",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "用照片、简介介绍自己，更容易遇到志同道合的人。",
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "照片墙",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "最多上传 $maxPhotoCount 张资料照片",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                photoUrls.forEach { photoUrl ->
                    ProfilePhotoWallItem(
                        photoUrl = photoUrl,
                        onRemove = { onRemovePhoto(photoUrl) }
                    )
                }
                if (photoUrls.size < maxPhotoCount) {
                    AddPhotoWallItem(
                        enabled = !isUploading,
                        onClick = onUploadClick
                    )
                }
            }
            if (photoUrls.isEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "还没有上传资料照片，添加几张更容易让别人了解你。",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                "个人简介",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = signature,
                onValueChange = onSignatureChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 140.dp),
                minLines = 5,
                maxLines = 12,
                singleLine = false,
                placeholder = {
                    Text(
                        "例如性格、爱好、日常或想认识的朋友类型…",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                supportingText = {
                    Text(
                        "$signatureLength/500",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onDone() }),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White
                )
            )
        }
    }
}

@Composable
private fun InterestSectionCard(
    tags: List<String>,
    onEditClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Text(
                "我的兴趣爱好",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(14.dp))
            val interestPresets = remember { defaultProfileInterestPresets() }
            val orderedTags = interestPresets.map { it.label }.filter { tags.contains(it) } +
                tags.filter { tag -> interestPresets.none { it.label == tag } }
            if (orderedTags.isEmpty()) {
                Text(
                    "尚未添加兴趣，点下方按钮选择。",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    orderedTags.forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE5E5E5))
                        ) {
                            Text(
                                text = tag,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onEditClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    if (tags.isEmpty()) "添加兴趣爱好" else "编辑兴趣爱好",
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun ProfilePhotoWallItem(
    photoUrl: String,
    onRemove: () -> Unit
) {
    Box(
        modifier = Modifier.size(100.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            AsyncImage(
                model = RetrofitClient.normalizeBackendMediaUrlForDisplay(photoUrl),
                contentDescription = "资料照片",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(22.dp)
                .clickable(onClick = onRemove),
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.55f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "删除照片",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun AddPhotoWallItem(
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(100.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "添加照片",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (enabled) "上传照片" else "上传中",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EdgeToEdgeGroup(
    backgroundColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = backgroundColor
    ) {
        Column {
            content()
        }
    }
}
