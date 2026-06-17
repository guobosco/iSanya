// 文件说明：完善昵称/真实姓名等名称信息的界面。

package com.example.Lulu.ui.screen

import android.widget.Toast
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.Lulu.data.local.AppDataStore
import com.example.Lulu.ui.navigation.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompleteNameScreen(navController: NavController) {
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()
    val backgroundColor = if (isDarkTheme) Color.Black else Color.White
    
    var name by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("完善资料 (1/2)", fontSize = 16.sp) },
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
            Spacer(modifier = Modifier.height(40.dp))
            
            Text(
                text = "请填写您的名字",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Start)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("名字") },
                placeholder = { Text("例如：张三") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        coroutineScope.launch {
                            isSaving = true
                            val user = AppDataStore.currentUser.value
                            val updatedUser = user.copy(name = name, updatedAt = System.currentTimeMillis())
                            val repository = AppDataStore.getRepository()
                            val result = repository?.syncCurrentUserProfile(updatedUser) ?: Result.success(updatedUser)
                            result.onSuccess { savedUser ->
                                AppDataStore.replaceCurrentUser(savedUser)
                                Toast.makeText(context, "昵称设置成功", Toast.LENGTH_SHORT).show()
                                navController.navigate(Screen.CompleteProfile.route)
                            }.onFailure { error ->
                                Toast.makeText(context, error.message ?: "昵称保存失败，请重试", Toast.LENGTH_SHORT).show()
                            }
                            isSaving = false
                        }
                    } else {
                        Toast.makeText(context, "请输入昵称", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                enabled = !isSaving
            ) {
                Text(if (isSaving) "保存中..." else "下一步", fontSize = 16.sp)
            }
        }
    }
}
