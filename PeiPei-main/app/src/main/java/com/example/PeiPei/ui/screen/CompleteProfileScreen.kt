// 文件说明：引导用户补全个人资料的流程界面。

package com.example.Lulu.ui.screen

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.Lulu.data.local.MockDataStore
import com.example.Lulu.ui.navigation.Screen
import com.example.Lulu.ui.components.RegionPickerDialog
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
fun CompleteProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val dismissKeyboard: () -> Unit = {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        Unit
    }
    val isDarkTheme = isSystemInDarkTheme()
    val backgroundColor = if (isDarkTheme) Color.Black else Color.White
    
    val currentUser = MockDataStore.currentUser.collectAsState().value
    val avatarInitial = currentUser.name.trim().firstOrNull()?.toString() ?: "?"
    
    var gender by remember { mutableStateOf(currentUser.gender) }
    var region by remember { mutableStateOf(currentUser.region) }
    var signature by remember { mutableStateOf(currentUser.signature) }
    
    var showGenderDialog by remember { mutableStateOf(false) }
    var showRegionDialog by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
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
                    setHideBottomControls(false)
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
                isSaving = true
                try {
                    val repository = MockDataStore.getRepository()
                    if (repository != null) {
                        Toast.makeText(context, "正在上传头像...", Toast.LENGTH_SHORT).show()
                        val serverUrl = com.example.Lulu.util.AvatarUploadUtil.processAndUploadAvatar(context, croppedUri, repository)
                        if (serverUrl != null) {
                            val updatedUser = MockDataStore.currentUser.value.copy(
                                photoUrl = serverUrl,
                                updatedAt = System.currentTimeMillis()
                            )
                            val result = repository.syncCurrentUserProfile(updatedUser)
                            result.onSuccess { savedUser ->
                                MockDataStore.replaceCurrentUser(savedUser)
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
                    isSaving = false
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

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("完善资料", fontSize = 16.sp) },
                actions = {
                    TextButton(
                        onClick = {
                        dismissKeyboard()
                        // 跳过 -> 去首页
                        navController.navigate(Screen.Home.route) {
                            popUpTo(0) { inclusive = true } // 清空所有栈
                        }
                        },
                        enabled = !isSaving
                    ) {
                        Text("跳过")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = backgroundColor)
            )
        },
        containerColor = backgroundColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                text = "设置头像和其他资料",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Start)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 头像设置
            Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier
                    .size(100.dp)
                    .clickable(enabled = !isSaving) {
                        avatarLauncher.launch("image/*")
                    }
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    if (currentUser.photoUrl.isNotEmpty()) {
                        AsyncImage(
                            model = RetrofitClient.normalizeBackendMediaUrlForDisplay(currentUser.photoUrl),
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = avatarInitial,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "上传头像",
                        modifier = Modifier.padding(6.dp),
                        tint = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Gender - ReadOnly, Click to show dialog
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = gender,
                    onValueChange = {},
                    label = { Text("性别") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    readOnly = true, // 禁止输入，只读
                    shape = RoundedCornerShape(8.dp),
                    trailingIcon = {
                        Icon(Icons.Default.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                // Overlay a clickable box
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { showGenderDialog = true }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Region - ReadOnly, Click to show dialog
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = region,
                    onValueChange = {},
                    label = { Text("地区") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    readOnly = true, // 禁止输入
                    shape = RoundedCornerShape(8.dp),
                    trailingIcon = {
                        Icon(Icons.Default.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { showRegionDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = signature,
                onValueChange = { 
                    if (it.length <= 30) {
                        signature = it 
                    }
                },
                label = { Text("个性签名") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                supportingText = {
                     Text("${signature.length}/30")
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { dismissKeyboard() })
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    dismissKeyboard()
                    if (gender.isEmpty() || region.isEmpty()) {
                        Toast.makeText(context, "请完善性别和地区信息", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    coroutineScope.launch {
                        isSaving = true
                        val user = MockDataStore.currentUser.value
                        val updatedUser = user.copy(
                            gender = gender,
                            region = region,
                            signature = signature,
                            isProfileCompleted = true,
                            updatedAt = System.currentTimeMillis()
                        )
                        val repository = MockDataStore.getRepository()
                        val result = repository?.syncCurrentUserProfile(updatedUser) ?: Result.success(updatedUser)
                        result.onSuccess { savedUser ->
                            MockDataStore.replaceCurrentUser(savedUser)
                            val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                            val phone = savedUser.phoneNumber
                            if (phone.isNotEmpty()) {
                                sharedPrefs.edit().putBoolean("is_profile_completed_$phone", true).apply()
                            }
                            Toast.makeText(context, "资料设置成功", Toast.LENGTH_SHORT).show()
                            navController.navigate(Screen.Home.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }.onFailure { error ->
                            Toast.makeText(context, error.message ?: "资料保存失败，请重试", Toast.LENGTH_SHORT).show()
                        }
                        isSaving = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                enabled = !isSaving
            ) {
                Text(if (isSaving) "保存中..." else "完成", fontSize = 16.sp)
            }
        }
    }
    
    // Dialogs
    if (showGenderDialog) {
        AlertDialog(
            onDismissRequest = { showGenderDialog = false },
            shape = RoundedCornerShape(4.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("选择性别") },
            text = {
                Column {
                    Text("男", modifier = Modifier.fillMaxWidth().clickable { 
                        gender = "男"
                        showGenderDialog = false 
                    }.padding(16.dp))
                    Text("女", modifier = Modifier.fillMaxWidth().clickable { 
                        gender = "女"
                        showGenderDialog = false 
                    }.padding(16.dp))
                }
            },
            confirmButton = {}
        )
    }

    if (showRegionDialog) {
        RegionPickerDialog(
            initialRegion = region,
            onDismiss = { showRegionDialog = false },
            onConfirm = { selectedRegion ->
                region = selectedRegion
                showRegionDialog = false
            }
        )
    }
}
