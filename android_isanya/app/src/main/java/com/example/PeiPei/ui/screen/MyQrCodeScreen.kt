// 文件说明：展示当前用户二维码（加好友等）的界面。

package com.example.Lulu.ui.screen

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.Lulu.R
import com.example.Lulu.data.local.AppDataStore
import com.example.Lulu.data.remote.RetrofitClient
import com.example.Lulu.ui.navigation.Screen
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.launch
import java.io.OutputStream
import java.io.File
import com.yalantis.ucrop.UCrop
import androidx.compose.ui.graphics.toArgb

import coil.imageLoader
import coil.request.ImageRequest
import com.example.Lulu.ui.theme.BrandPink
import com.example.Lulu.ui.theme.BrandSecondaryDeep
import com.example.Lulu.ui.theme.BrandPinkStrong
import com.example.Lulu.ui.theme.TextPrimary
import com.example.Lulu.ui.components.EditDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyQrCodeScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val user by AppDataStore.currentUser.collectAsState()
    
    val serviceHostTerm = stringResource(R.string.service_host_term)

    val qrCodeColor = remember { mutableStateOf(Color.Unspecified) }
    // 如果未指定颜色（初始状态），则使用当前主题的主色
    if (qrCodeColor.value == Color.Unspecified) {
        qrCodeColor.value = MaterialTheme.colorScheme.primary
    }

    // Load Avatar Bitmap for both UI and QR Code
    var avatarBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    
    LaunchedEffect(user.photoUrl) {
        if (user.photoUrl.isNotEmpty()) {
            try {
                val request = ImageRequest.Builder(context)
                    .data(RetrofitClient.normalizeBackendMediaUrlForDisplay(user.photoUrl))
                    .allowHardware(false)
                    .build()
                val result = context.imageLoader.execute(request)
                avatarBitmap = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
            } catch (e: Exception) {
                avatarBitmap = null
            }
        } else {
            avatarBitmap = null
        }
    }

    // Load default Logo Bitmap (Fallback)
    val logoBitmap = remember {
        try {
            android.graphics.BitmapFactory.decodeResource(context.resources, com.example.Lulu.R.drawable.ic_logo)
        } catch (e: Exception) {
            null
        }
    }

    val qrCodeBitmap = remember(user.peiPeiId, qrCodeColor.value, logoBitmap) {
        // Use app logo for QR center
        generateQRCode("lulu:${user.peiPeiId.ifEmpty { "pp12345678" }}", qrCodeColor.value, logoBitmap)
    }
    
    // GraphicsLayer for capturing the card
    val graphicsLayer = rememberGraphicsLayer()
    
    var showEditDialog by remember { mutableStateOf(false) }
    var editId by remember { mutableStateOf("") }
    var isAvatarSaving by remember { mutableStateOf(false) }

    // Theme colors for uCrop
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()

    val uCropLauncher = rememberLauncherForActivityResult(
        contract = object : androidx.activity.result.contract.ActivityResultContract<Uri, Uri?>() {
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
                if (resultCode == android.app.Activity.RESULT_OK && intent != null) {
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
                            val updatedUser = AppDataStore.currentUser.value.copy(
                                photoUrl = serverUrl,
                                updatedAt = System.currentTimeMillis()
                            )
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

    // Avatar Launcher
    val avatarLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            uCropLauncher.launch(uri)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("", fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Scan.route) }) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Center Content Area (Capture Target)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                Spacer(modifier = Modifier.height(30.dp)) // Increased top spacing slightly

                // Capture Container
                Column(
                    modifier = Modifier
                        .padding(top = 20.dp)
                        .drawWithContent {
                            graphicsLayer.record {
                                // Draw white background for saved image padding
                                drawRect(Color.White, size = this.size)
                                this@drawWithContent.drawContent()
                            }
                            drawLayer(graphicsLayer)
                        }
                        .padding(40.dp), // Increased internal padding for saved image white border
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Content Container
                    Column(
                        modifier = Modifier.width(280.dp) // Increased width for better layout
                    ) {
                        // User Info Row (Left Aligned)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar
                            Surface(
                                modifier = Modifier
                                    .size(54.dp) // Slightly larger avatar
                                    .clickable(enabled = !isAvatarSaving) { 
                                        avatarLauncher.launch("image/*")
                                    },
                                shape = RoundedCornerShape(4.dp),
                                color = if (avatarBitmap != null) Color.Transparent else MaterialTheme.colorScheme.primaryContainer
                            ) {
                                val currentAvatar = avatarBitmap
                                if (currentAvatar != null) {
                                    Image(
                                        bitmap = currentAvatar.asImageBitmap(),
                                        contentDescription = "Avatar",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = if (user.name.isNotEmpty()) user.name.take(1) else "我",
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            // Name and ID
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp), // Match avatar height to align vertically
                                verticalArrangement = Arrangement.SpaceEvenly // Distribute space evenly
                            ) {
                                Text(
                                    text = user.name,
                                    fontSize = 18.sp, // Slightly larger name
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.Black,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "爱野 ID: ${user.peiPeiId.ifEmpty { "未设置" }}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp)) // Increased spacing between info and QR

                        // QR Code
                        if (qrCodeBitmap != null) {
                            Image(
                                bitmap = qrCodeBitmap.asImageBitmap(),
                                contentDescription = "QR Code",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f),
                                contentScale = ContentScale.FillWidth
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp)) // Increased spacing

                        // Footer Text
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "扫一扫上面的二维码图案，查看我的主页",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // 底部操作按钮 (仿微信风格：文字链接)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 48.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 扫一扫
                TextButton(onClick = { navController.navigate(Screen.Scan.route) }) {
                    Text("扫一扫", color = MaterialTheme.colorScheme.secondary, fontSize = 14.sp)
                }
                
                VerticalDivider(height = 12.dp, color = MaterialTheme.colorScheme.outline)
                
                // 换个样式
                TextButton(onClick = {
                    // 随机切换颜色
                    val colors = listOf(TextPrimary, BrandPink, BrandPinkStrong, BrandSecondaryDeep)
                    val currentColor = qrCodeColor.value
                    var nextColor = colors.random()
                    while (nextColor == currentColor) {
                        nextColor = colors.random()
                    }
                    qrCodeColor.value = nextColor
                }) {
                    Text("换个样式", color = MaterialTheme.colorScheme.secondary, fontSize = 14.sp)
                }
                
                VerticalDivider(height = 12.dp, color = MaterialTheme.colorScheme.outline)

                // 保存图片
                TextButton(onClick = {
                    coroutineScope.launch {
                        val bitmap = graphicsLayer.toImageBitmap()
                        val androidBitmap = bitmap.asAndroidBitmap()
                        val uri = saveBitmapToGallery(context, androidBitmap, "lulu_card_${user.peiPeiId}")
                        if (uri != null) {
                            Toast.makeText(context, "图片已保存到相册", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "保存失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) {
                    Text("保存图片", color = MaterialTheme.colorScheme.secondary, fontSize = 14.sp)
                }
            }
        }
        
        // 修改对话框
        if (showEditDialog) {
            EditDialog(
                title = "修改爱野 ID",
                initialValue = editId,
                maxLength = 20,
                validator = { input ->
                    val regex = "^[a-zA-Z0-9_-]{6,20}$".toRegex()
                    if (!input.matches(regex)) {
                        "格式错误：6-20位字母、数字、下划线或减号"
                    } else {
                        null
                    }
                },
                onDismiss = { showEditDialog = false },
                onConfirm = { newId ->
                    val updatedUser = user.copy(peiPeiId = newId)
                    AppDataStore.updateCurrentUser(updatedUser)
                    Toast.makeText(context, "爱野 ID已更新", Toast.LENGTH_SHORT).show()
                    showEditDialog = false
                }
            )
        }
    }

    // EditDialog logic needs to be inside the Composable or called. 
    // Since I'm replacing the Scaffold, I need to make sure ShowEditDialog is handled.
    // However, in the new UI, I removed the "Edit" pencil icon from the main view to match the clean look.
    // The user can edit ID in Profile page.
    // If user wants to edit ID here, I should add the click listener back to the text or add an option.
    // The reference image doesn't show an edit icon. It's usually in Profile settings.
    // I will remove the inline edit feature here for a cleaner look, consistent with "Modern/WeChat".
}

@Composable
fun VerticalDivider(height: Dp, color: Color) {
    Box(
        modifier = Modifier
            .height(height)
            .width(1.dp)
            .background(color)
    )
}

private fun saveBitmapToGallery(context: Context, bitmap: Bitmap, title: String): Uri? {
    val filename = "$title-${System.currentTimeMillis()}.png"
    
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }

    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

    uri?.let {
        var stream: OutputStream? = null
        try {
            stream = resolver.openOutputStream(it)
            if (stream != null) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(it, contentValues, null, null)
            }
            return it
        } catch (e: Exception) {
            e.printStackTrace()
            resolver.delete(it, null, null)
        } finally {
            stream?.close()
        }
    }
    return null
}

private fun shareImageUri(context: Context, uri: Uri) {
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_STREAM, uri)
        type = "image/png"
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "分享图片到..."))
}

private fun generateQRCode(content: String, color: Color = Color.Black, logo: Bitmap? = null): Bitmap? {
    return try {
        // 1. 获取原始矩阵（不缩放）
        val hints = mutableMapOf<com.google.zxing.EncodeHintType, Any>()
        hints[com.google.zxing.EncodeHintType.MARGIN] = 1
        // 提高容错率到 H (30%) 以支持中间的 Logo 遮挡
        hints[com.google.zxing.EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.H
        // Force a slightly higher version to make dots smaller/denser (WeChat style)
        // Standard V2 is 25x25. V6 is 41x41.
        hints[com.google.zxing.EncodeHintType.QR_VERSION] = 6
        
        val matrix: BitMatrix = MultiFormatWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            0, // 请求最小尺寸
            0,
            hints
        )
        
        val matrixWidth = matrix.width
        val matrixHeight = matrix.height
        
        // 2. 确定目标位图大小
        // 为了清晰度，我们希望每个模块（cell）占多个像素
        val targetSize = 1000 // 高分辨率以支持平滑圆角
        val cellSize = targetSize / matrixWidth
        val finalSize = cellSize * matrixWidth // 调整最终大小以完全匹配网格
        
        val bitmap = Bitmap.createBitmap(finalSize, finalSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            this.color = android.graphics.Color.argb(
                (color.alpha * 255).toInt(),
                (color.red * 255).toInt(),
                (color.green * 255).toInt(),
                (color.blue * 255).toInt()
            )
        }

        // 3. 绘制每个模块
        for (x in 0 until matrixWidth) {
            for (y in 0 until matrixHeight) {
                // 如果有Logo，跳过Logo覆盖区域的绘制，避免边缘露出半个点
                if (logo != null) {
                    // 二维码矩阵通常是奇数大小（例如 V6 是 41x41），中心索引是 matrixWidth / 2
                    val centerX = matrixWidth / 2
                    val centerY = matrixHeight / 2
                    
                    // Logo大小约为20%，白边约为Logo的10%（即总大小的2%左右）
                    // 总覆盖直径约为 24% (0.2 + 2*0.02)
                    // 半径约为 12%
                    // 为了确保完全覆盖不露半个点，我们稍微放大范围到 14%
                    val exclusionRadius = (matrixWidth * 0.14).toInt()
                    
                    // 使用闭区间 [center - radius, center + radius] 确保对称性
                    if (x >= centerX - exclusionRadius && x <= centerX + exclusionRadius &&
                        y >= centerY - exclusionRadius && y <= centerY + exclusionRadius) {
                        continue
                    }
                }

                if (matrix[x, y]) {
                    // 检查是否在探测图形区域（三个角落的 7x7 区域）
                    val isFinder = (x < 7 && y < 7) || 
                                  (x >= matrixWidth - 7 && y < 7) || 
                                  (x < 7 && y >= matrixHeight - 7)
                                  
                    val left = (x * cellSize).toFloat()
                    val top = (y * cellSize).toFloat()
                    val right = left + cellSize
                    val bottom = top + cellSize
                    
                    if (isFinder) {
                        // Finder patterns are squares in WeChat
                        canvas.drawRect(left, top, right, bottom, paint)
                    } else {
                        // Data modules: Circular dots for modern WeChat style
                        // Add padding to make dots smaller (separated)
                        val padding = cellSize * 0.1f 
                        val rectF = android.graphics.RectF(
                            left + padding,
                            top + padding,
                            right - padding,
                            bottom - padding
                        )
                        // Draw Circle
                        canvas.drawOval(rectF, paint)
                    }
                }
            }
        }

        // 4. Draw Logo
        if (logo != null) {
            // 绘制圆角 Logo
            val logoSize = (finalSize * 0.2f).toInt() // Logo 占二维码大小的 20%
            val logoX = (finalSize - logoSize) / 2
            val logoY = (finalSize - logoSize) / 2
            
            // Draw White Background for Logo (Restore white border)
            val bgPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                this.color = android.graphics.Color.WHITE
                this.style = android.graphics.Paint.Style.FILL
            }
            // Add slight padding for the white border around logo
            val borderPadding = logoSize * 0.1f
            val bgRect = android.graphics.RectF(
                logoX - borderPadding, 
                logoY - borderPadding, 
                logoX + logoSize + borderPadding, 
                logoY + logoSize + borderPadding
            )
            // Circular border for avatar style logo
            canvas.drawRoundRect(bgRect, borderPadding * 2, borderPadding * 2, bgPaint)
            
            val scaledLogo = Bitmap.createScaledBitmap(logo, logoSize, logoSize, true)
            
            val logoPaint = android.graphics.Paint().apply {
                isAntiAlias = true
            }
            
            // Create a rounded rect path for the logo image
            val path = android.graphics.Path()
            val cornerRadius = logoSize * 0.15f // 圆角大小
            val logoRect = android.graphics.RectF(
                logoX.toFloat(), 
                logoY.toFloat(), 
                (logoX + logoSize).toFloat(), 
                (logoY + logoSize).toFloat()
            )
            path.addRoundRect(logoRect, cornerRadius, cornerRadius, android.graphics.Path.Direction.CW)
            
            canvas.save()
            canvas.clipPath(path)
            canvas.drawBitmap(scaledLogo, logoX.toFloat(), logoY.toFloat(), logoPaint)
            canvas.restore()
        }

        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
