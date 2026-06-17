// 文件说明：微信登录后绑定手机号的界面。

package com.example.Lulu.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.Lulu.data.local.AppDataStore
import com.example.Lulu.data.model.hasCompletedOnboardingProfile
import com.example.Lulu.ui.navigation.Screen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeChatBindPhoneScreen(navController: NavController) {
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()
    val backgroundColor = if (isDarkTheme) Color.Black else Color.White
    
    var phoneNumber by remember { mutableStateOf("") }
    var verifyCode by remember { mutableStateOf("") }
    var isCodeSent by remember { mutableStateOf(false) }
    var countdown by remember { mutableStateOf(60) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("绑定手机号", fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        // 跳过绑定，直接生成 ID（lulu_wx_*）并跳转到完善资料
                        val currentUser = AppDataStore.currentUser.value
                        if (currentUser.peiPeiId.isEmpty()) {
                            val randomSuffix = Random.nextInt(100000, 999999)
                            val newLuluId = "lulu_wx_$randomSuffix"
                            AppDataStore.updateCurrentUser(currentUser.copy(
                                peiPeiId = newLuluId,
                                updatedAt = System.currentTimeMillis()
                            ))
                        }
                        if (currentUser.name.isBlank()) {
                            navController.navigate(Screen.CompleteName.route)
                        } else {
                            navController.navigate(Screen.CompleteProfile.route)
                        }
                    }) {
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
                text = "绑定手机号",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Start)
            )
            
            Text(
                text = "绑定手机号可方便找回账号和同步通讯录好友",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(top = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // 手机号
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { if (it.length <= 11 && it.all { char -> char.isDigit() }) phoneNumber = it },
                label = { Text("手机号码") },
                prefix = { Text("+86 ", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(8.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 验证码
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = verifyCode,
                    onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) verifyCode = it },
                    label = { Text("验证码") },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(8.dp)
                )
                
                Button(
                    onClick = {
                        if (phoneNumber.length != 11) {
                            Toast.makeText(context, "请输入正确的手机号", Toast.LENGTH_SHORT).show()
                        } else {
                            isCodeSent = true
                            countdown = 60
                            Toast.makeText(context, "验证码已发送", Toast.LENGTH_SHORT).show()
                            scope.launch {
                                while (countdown > 0) {
                                    delay(1000)
                                    countdown--
                                }
                                isCodeSent = false
                            }
                        }
                    },
                    enabled = !isCodeSent && phoneNumber.length == 11,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(56.dp)
                ) {
                    Text(if (isCodeSent) "${countdown}s" else "获取验证码")
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 绑定按钮
            Button(
                onClick = {
                    if (phoneNumber.length == 11 && verifyCode.isNotEmpty()) {
                        // 绑定成功，更新用户信息
                        val currentUser = AppDataStore.currentUser.value
                        val updatedUser = currentUser.copy(
                            phoneNumber = phoneNumber,
                            isPhoneVerified = true,
                            peiPeiId = "lulu_$phoneNumber", // 绑定手机号后的 ID 规则
                            updatedAt = System.currentTimeMillis()
                        )
                        AppDataStore.updateCurrentUser(updatedUser)
                        
                        // Save binding info locally for mock persistence
                        val sharedPrefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                        sharedPrefs.edit().putString("wechat_bind_phone", phoneNumber).apply()
                        
                        Toast.makeText(context, "绑定成功", Toast.LENGTH_SHORT).show()
                        if (updatedUser.hasCompletedOnboardingProfile()) {
                            navController.navigate(Screen.Home.route) { popUpTo(0) { inclusive = true } }
                        } else if (updatedUser.name.isBlank()) {
                            navController.navigate(Screen.CompleteName.route)
                        } else {
                            navController.navigate(Screen.CompleteProfile.route)
                        }
                    } else {
                        Toast.makeText(context, "请填写完整信息", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp)
            ) {
                Text("绑定并下一步", fontSize = 16.sp)
            }
        }
    }
}
