// 文件说明：登录界面（手机号、第三方等登录方式）。

package com.example.Lulu.ui.screen

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.border
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.Lulu.R
import com.example.Lulu.ui.navigation.Screen
import com.example.Lulu.data.repository.LuluRepository
import com.example.Lulu.data.model.User
import com.example.Lulu.ui.components.FeiLingLoadingLabel
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController, initialPhone: String? = null) {
    val context = LocalContext.current
    val repository = remember { LuluRepository.get() }
    val scope = rememberCoroutineScope()
    val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val savedPhone = remember { sharedPrefs.getString("last_login_phone", "") }
    
    val isDarkTheme = isSystemInDarkTheme()
    val backgroundColor = if (isDarkTheme) Color.Black else Color.White
    
    // Login Stage: 2 = Input Phone/Code (Default for Real Backend Login)
    var loginStage by remember { mutableStateOf(2) }

    // Login Step for Input Mode (Stage 2): 0 = Input Phone (Default)
    var loginStep by remember { mutableStateOf(0) }
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }

    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()
    }
    
    var phoneNumber by remember { 
        mutableStateOf(
            (if (!initialPhone.isNullOrEmpty()) initialPhone else savedPhone ?: "")
                .filter { it.isDigit() } // Ensure only digits are initially set
        ) 
    }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }
    var verifyCode by remember { mutableStateOf("") }
    var isCodeSent by remember { mutableStateOf(false) }
    var countdown by remember { mutableStateOf(60) }
    
    var isAgreed by remember { mutableStateOf(false) }
    var showAgreementDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("", fontSize = 16.sp) }, 
                navigationIcon = {
                    if (navController.previousBackStackEntry != null) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = backgroundColor)
            )
        },
        containerColor = backgroundColor
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Directly show Input Mode (Stage 2)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(20.dp))
                
                // --- STEP 0: Input Phone & Password ---
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "登录",
                        fontSize = if (!isRegisterMode) 24.sp else 18.sp,
                        fontWeight = if (!isRegisterMode) FontWeight.Bold else FontWeight.Normal,
                        color = if (!isRegisterMode) MaterialTheme.colorScheme.onSurface else Color.Gray,
                        modifier = Modifier.clickable { isRegisterMode = false }
                    )
                    Spacer(modifier = Modifier.width(24.dp))
                    Text(
                        text = "注册",
                        fontSize = if (isRegisterMode) 24.sp else 18.sp,
                        fontWeight = if (isRegisterMode) FontWeight.Bold else FontWeight.Normal,
                        color = if (isRegisterMode) MaterialTheme.colorScheme.onSurface else Color.Gray,
                        modifier = Modifier.clickable { isRegisterMode = true }
                    )
                }
                
                // Phone Input
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { 
                        // Only allow digits and limit to 11 characters
                        val filtered = it.filter { char -> char.isDigit() }
                        if (filtered.length <= 11) {
                            phoneNumber = filtered
                        }
                    },
                    label = { Text("手机号码") },
                    prefix = { Text("+86 ", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(8.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                // Password Input
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("密码") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(8.dp)
                )

                if (isRegisterMode) {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("确认密码") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = {
                        if (phoneNumber.length != 11) {
                            Toast.makeText(context, "请输入11位手机号", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (password.isEmpty()) {
                            Toast.makeText(context, "请输入密码", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        if (isRegisterMode) {
                            // Register Logic
                            if (password != confirmPassword) {
                                Toast.makeText(context, "两次输入的密码不一致", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            scope.launch {
                                isLoading = true
                                try {
                                    val newUser = User(
                                        id = UUID.randomUUID().toString(),
                                        name = "用户_${phoneNumber.takeLast(4)}",
                                        phoneNumber = phoneNumber,
                                        // password = password, // User model doesn't have password field
                                        peiPeiId = "pp_${phoneNumber.takeLast(4)}",
                                        createdAt = System.currentTimeMillis(),
                                        updatedAt = System.currentTimeMillis()
                                    )
                                    val registered = repository.registerUser(newUser, password)
                                    registered.onSuccess {
                                        Toast.makeText(context, "注册成功", Toast.LENGTH_SHORT).show()
                                        repository.syncAfterLogin(context)
                                        navController.navigate(Screen.CompleteName.route)
                                    }.onFailure { e ->
                                        val msg = e.message ?: "注册失败，请重试"
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "注册出错：${e.message}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isLoading = false
                                }
                            }
                        } else {
                            // Login Logic
                            scope.launch {
                                isLoading = true
                                try {
                                    val user = repository.loginUser(phoneNumber, password)
                                    if (user != null) {
                                        Toast.makeText(context, "登录成功", Toast.LENGTH_SHORT).show()
                                        repository.syncAfterLogin(context)

                                        // Check if profile is actually completed (flag or sufficient data)
                                        // Gender and Region are considered minimum required fields
                                        val isProfileReady = user.isProfileCompleted ||
                                            (user.gender.isNotEmpty() && user.region.isNotEmpty())
                                            
                                        if (isProfileReady) {
                                            navController.navigate(Screen.Home.route) { popUpTo(0) { inclusive = true } }
                                        } else {
                                            navController.navigate(Screen.CompleteProfile.route)
                                        }
                                    } else {
                                        Toast.makeText(context, "登录失败，请检查账号密码或网络", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "登录出错：${e.message}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    AnimatedContent(
                        targetState = isLoading,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                        },
                        label = "loginButtonContent"
                    ) { loading ->
                        if (loading) {
                            FeiLingLoadingLabel(
                                text = if (isRegisterMode) "正在注册" else "正在登录",
                                textColor = MaterialTheme.colorScheme.onPrimary,
                                dotColor = MaterialTheme.colorScheme.onPrimary,
                                dotSize = 6.dp
                            )
                        } else {
                            Text(if (isRegisterMode) "注册" else "登录", fontSize = 16.sp)
                        }
                    }
                }
                
            }
            
            // Dialog
            if (showAgreementDialog) {
                AlertDialog(
                    onDismissRequest = { showAgreementDialog = false },
                    title = { Text("服务协议和隐私政策", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                    text = { 
                        Text("请您阅读并同意《服务协议》、《隐私政策》和《i三亚账号服务须知》，以便我们为您提供完整的服务。", lineHeight = 20.sp) 
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                isAgreed = true
                                showAgreementDialog = false
                            }
                        ) {
                            Text("同意", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showAgreementDialog = false }
                        ) {
                            Text("不同意", color = Color.Gray)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface
                )
            }
            

        }
    }
}
