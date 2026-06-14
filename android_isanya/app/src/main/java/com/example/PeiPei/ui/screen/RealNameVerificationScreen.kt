// 文件说明：实名认证引导页（演示身份证 OCR、人脸活体、人证比对流程）。

package com.example.Lulu.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.Lulu.data.local.AppDataStore
import com.example.Lulu.ui.components.PrimaryGradientButton
import com.example.Lulu.ui.theme.SuccessGreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val CardShape = RoundedCornerShape(20.dp)
private val ButtonShape = RoundedCornerShape(26.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealNameVerificationScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    var step by remember { mutableIntStateOf(0) }
    var ocrDone by remember { mutableStateOf(false) }
    var livenessDone by remember { mutableStateOf(false) }
    var comparing by remember { mutableStateOf(false) }
    var matchOk by remember { mutableStateOf(false) }
    val user by AppDataStore.currentUser.collectAsState()
    val scheme = MaterialTheme.colorScheme

    Scaffold(
        containerColor = scheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "实名认证",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = scheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = scheme.background,
                    scrolledContainerColor = scheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = "为保障平台安全，请完成身份证识别、人脸活体检测与人证一致性比对。以下为演示流程，正式环境将接入实人认证服务。",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                lineHeight = 22.sp
            )

            when (step) {
                0 -> StepIntroCard(onNext = { step = 1 })
                1 -> StepCard(
                    stepIndex = 1,
                    title = "身份证人像面 OCR",
                    subtitle = "请确保证件边框完整、文字清晰。演示中点击下方按钮即可模拟识别成功。",
                    icon = Icons.Default.CameraAlt,
                    done = ocrDone,
                    primaryLabel = if (ocrDone) "已识别" else "上传并识别身份证（演示）",
                    onPrimary = {
                        scope.launch {
                            delay(600)
                            ocrDone = true
                        }
                    },
                    enabled = !ocrDone,
                    onNext = { if (ocrDone) step = 2 }
                )
                2 -> StepCard(
                    stepIndex = 2,
                    title = "人脸活体检测",
                    subtitle = "请正对屏幕完成眨眼等动作。演示中点击下方按钮模拟活体通过。",
                    icon = Icons.Default.Face,
                    done = livenessDone,
                    primaryLabel = if (livenessDone) "活体检测已通过" else "开始人脸活体检测（演示）",
                    onPrimary = {
                        scope.launch {
                            delay(800)
                            livenessDone = true
                        }
                    },
                    enabled = !livenessDone,
                    onNext = { if (livenessDone) step = 3 }
                )
                else -> StepMatchCard(
                    comparing = comparing,
                    matchOk = matchOk,
                    onCompare = {
                        scope.launch {
                            comparing = true
                            delay(1200)
                            comparing = false
                            matchOk = true
                        }
                    },
                    onFinish = {
                        if (user.id.isNotEmpty()) {
                            AppDataStore.updateCurrentUser(
                                user.copy(identityVerified = true, updatedAt = System.currentTimeMillis())
                            )
                        }
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

@Composable
private fun StepIntroCard(onNext: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    AppStepSurface {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                "您将依次完成",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface
            )
            BulletLine("拍摄或上传身份证人像面，自动提取姓名与证件号")
            BulletLine("人脸活体检测，防止冒用照片")
            BulletLine("将活体采集照与证件照比对，确认人证一致")
            Spacer(Modifier.height(6.dp))
            PrimaryGradientButton(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = ButtonShape
            ) {
                Text("开始认证", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun BulletLine(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "·",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            lineHeight = 22.sp
        )
    }
}

@Composable
private fun AppStepSurface(content: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        color = scheme.surface,
        shadowElevation = 10.dp,
        tonalElevation = 0.dp
    ) {
        Column(Modifier.padding(20.dp)) {
            content()
        }
    }
}

@Composable
private fun StepCard(
    stepIndex: Int,
    title: String,
    subtitle: String,
    icon: ImageVector,
    done: Boolean,
    primaryLabel: String,
    onPrimary: () -> Unit,
    enabled: Boolean,
    onNext: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    AppStepSurface {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = scheme.primaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "$stepIndex",
                            color = scheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.onSurface
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(scheme.surfaceVariant.copy(alpha = 0.45f))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(26.dp)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    lineHeight = 22.sp,
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedButton(
                onClick = onPrimary,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = ButtonShape,
                border = BorderStroke(1.dp, scheme.primary),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.primary)
            ) {
                Text(primaryLabel, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
            if (done) {
                PrimaryGradientButton(
                    onClick = onNext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = ButtonShape
                ) {
                    Text("下一步", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun StepMatchCard(
    comparing: Boolean,
    matchOk: Boolean,
    onCompare: () -> Unit,
    onFinish: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    AppStepSurface {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = scheme.primaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = scheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Text(
                    text = "人证一致性比对",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                text = "系统将比对身份证芯片/证件照信息与活体采集的人脸特征。演示中点击下方模拟比对通过。",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                lineHeight = 22.sp,
                modifier = Modifier.fillMaxWidth()
            )
            when {
                comparing -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        color = scheme.primary,
                        strokeWidth = 3.dp
                    )
                    Text(
                        "正在比对…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant
                    )
                }
                matchOk -> {
                    Text(
                        text = "比对一致，实名认证已完成",
                        color = SuccessGreen,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    PrimaryGradientButton(
                        onClick = onFinish,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = ButtonShape
                    ) {
                        Text("完成并返回", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                else -> {
                    OutlinedButton(
                        onClick = onCompare,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = ButtonShape,
                        border = BorderStroke(1.dp, scheme.primary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.primary)
                    ) {
                        Text("开始人证比对（演示）", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
