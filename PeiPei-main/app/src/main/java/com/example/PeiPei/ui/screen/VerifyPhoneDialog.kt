// 文件说明：手机号验证弹窗（验证码输入等）。

package com.example.Lulu.ui.screen

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VerifyPhoneDialog(
    phoneNumber: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var verifyCode by remember { mutableStateOf("") }
    var isCodeSent by remember { mutableStateOf(false) }
    var countdown by remember { mutableStateOf(60) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("验证手机号") },
        text = {
            Column {
                Text("验证码将发送至: $phoneNumber", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = verifyCode,
                        onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) verifyCode = it },
                        label = { Text("验证码") },
                        modifier = Modifier.weight(1f).padding(end = 8.dp).focusRequester(focusRequester),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    
                    Button(
                        onClick = {
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
                        },
                        enabled = !isCodeSent,
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text(if (isCodeSent) "${countdown}s" else "获取验证码", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (verifyCode.length >= 4) { // Simple validation
                        onConfirm()
                    } else {
                        Toast.makeText(context, "请输入正确的验证码", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Text("验证")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
