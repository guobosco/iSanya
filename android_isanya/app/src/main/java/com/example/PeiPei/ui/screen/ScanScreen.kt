// 文件说明：扫码界面（相机解码、跳转等）。

package com.example.Lulu.ui.screen

import android.app.Application
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.Lulu.data.model.User
import com.example.Lulu.data.repository.UserRepository
import com.example.Lulu.ui.navigation.Screen
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import kotlinx.coroutines.launch

@Composable
fun ScanScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userRepository = remember {
        UserRepository(context.applicationContext as Application)
    }

    var searchResult by remember { mutableStateOf<User?>(null) }
    var searchError by remember { mutableStateOf<String?>(null) }

    fun clearLookup() {
        searchResult = null
        searchError = null
    }

    fun requestUserLookup(rawQuery: String) {
        scope.launch {
            val outcome = userRepository.lookupRemoteUserAndPersist(rawQuery)
            when {
                outcome.user != null -> searchResult = outcome.user
                outcome.errorMessage != null -> searchError = outcome.errorMessage
            }
        }
    }

    LaunchedEffect(searchResult) {
        searchResult?.let { user ->
            navController.navigate(Screen.ServiceHostProfile.createRoute(user.id)) {
                popUpTo(Screen.Scan.route) { inclusive = true }
            }
            clearLookup()
        }
    }

    LaunchedEffect(searchError) {
        searchError?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            clearLookup()
        }
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "需要相机权限来扫描二维码", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Gallery Picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                // 确保有读取权限 (特别是针对部分机型)
                val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flag)
            } catch (e: Exception) {
                // 忽略异常，部分 URI 可能不支持持久化权限
            }

            try {
                val image = InputImage.fromFilePath(context, uri)
                val scanner = BarcodeScanning.getClient()
                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        handleScanResult(barcodes) { id -> requestUserLookup(id) }
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "无法识别二维码", Toast.LENGTH_SHORT).show()
                    }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "读取图片失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Black
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (hasCameraPermission) {
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    onBarcodeDetected = { barcodes ->
                         handleScanResult(barcodes) { id -> requestUserLookup(id) }
                    }
                )
                
                // Overlay
                ScanOverlay()
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("需要相机权限", color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                            Text("授权")
                        }
                    }
                }
            }

            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text("扫一扫", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Normal)
                IconButton(onClick = { galleryLauncher.launch("image/*") }) {
                    Icon(Icons.Default.Image, contentDescription = "Gallery", tint = Color.White)
                }
            }
        }
    }
}

private fun handleScanResult(
    barcodes: List<Barcode>,
    onScan: (String) -> Unit
) {
    if (barcodes.isNotEmpty()) {
        val rawValue = barcodes[0].rawValue
        if (!rawValue.isNullOrEmpty()) {
            val peiPeiId = if (rawValue.startsWith("lulu:")) {
                rawValue.substringAfter("lulu:")
            } else if (rawValue.startsWith("Lulu:")) {
                 rawValue.substringAfter("Lulu:")
            } else {
                rawValue 
            }
            onScan(peiPeiId)
        }
    }
}

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onBarcodeDetected: (List<Barcode>) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var preview by remember { mutableStateOf<Preview?>(null) }
    var imageAnalysis by remember { mutableStateOf<ImageAnalysis?>(null) }
    
    var isProcessing by remember { mutableStateOf(false) }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = modifier,
        update = { previewView ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                
                preview = Preview.Builder().build()
                
                imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    
                imageAnalysis?.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                    if (isProcessing) {
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    
                    processImageProxy(imageProxy) { barcodes ->
                        if (barcodes.isNotEmpty()) {
                            isProcessing = true
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                onBarcodeDetected(barcodes)
                            }
                        }
                    }
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                    preview?.setSurfaceProvider(previewView.surfaceProvider)
                } catch (e: Exception) {
                    Log.e("ScanScreen", "Use case binding failed", e)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )
}

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun processImageProxy(
    imageProxy: ImageProxy,
    onSuccess: (List<Barcode>) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val scanner = BarcodeScanning.getClient()
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty()) {
                    onSuccess(barcodes)
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}

@Composable
fun ScanOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val scanSize = size.width * 0.7f
        val left = (size.width - scanSize) / 2
        val top = (size.height - scanSize) / 2
        val right = left + scanSize
        val bottom = top + scanSize

        // Draw 4 rects to create a "hole"
        // Top
        drawRect(
            color = Color.Black.copy(alpha = 0.5f),
            topLeft = Offset(0f, 0f),
            size = Size(size.width, top)
        )
        // Bottom
        drawRect(
            color = Color.Black.copy(alpha = 0.5f),
            topLeft = Offset(0f, bottom),
            size = Size(size.width, size.height - bottom)
        )
        // Left
        drawRect(
            color = Color.Black.copy(alpha = 0.5f),
            topLeft = Offset(0f, top),
            size = Size(left, scanSize)
        )
        // Right
        drawRect(
            color = Color.Black.copy(alpha = 0.5f),
            topLeft = Offset(right, top),
            size = Size(size.width - right, scanSize)
        )
        
        // Draw corners - 极简风格，仅保留白色细线边框的四角
        val cornerLength = 20.dp.toPx()
        val strokeWidth = 2.dp.toPx() // 变细
        val color = Color.White // 变白
        
        // Top Left
        drawLine(color, Offset(left, top), Offset(left + cornerLength, top), strokeWidth)
        drawLine(color, Offset(left, top), Offset(left, top + cornerLength), strokeWidth)
        
        // Top Right
        drawLine(color, Offset(right, top), Offset(right - cornerLength, top), strokeWidth)
        drawLine(color, Offset(right, top), Offset(right, top + cornerLength), strokeWidth)
        
        // Bottom Left
        drawLine(color, Offset(left, bottom), Offset(left + cornerLength, bottom), strokeWidth)
        drawLine(color, Offset(left, bottom), Offset(left, bottom - cornerLength), strokeWidth)
        
        // Bottom Right
        drawLine(color, Offset(right, bottom), Offset(right - cornerLength, bottom), strokeWidth)
        drawLine(color, Offset(right, bottom), Offset(right, bottom - cornerLength), strokeWidth)
    }
}
