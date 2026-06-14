// 文件说明：标签的创建、编辑与管理界面。

package com.example.Lulu.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Label
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import com.example.Lulu.data.local.AppDataStore
import com.example.Lulu.ui.navigation.Screen
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagManageScreen(navController: NavController) {
    val tags by AppDataStore.tags.collectAsState()
    val contacts by AppDataStore.contacts.collectAsState()
    val currentUser = AppDataStore.currentUser.collectAsState().value
    
    var showDialog by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var currentTag by remember { mutableStateOf("") }
    var originalTag by remember { mutableStateOf("") }

    val isDarkTheme = isSystemInDarkTheme()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("标签管理", fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        currentTag = ""
                        isEditing = false
                        showDialog = true
                    }) {
                        Icon(Icons.Default.AddCircle, contentDescription = "添加标签", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = if (isDarkTheme) MaterialTheme.colorScheme.background else Color.White
                ),
                modifier = Modifier.background(if (isDarkTheme) MaterialTheme.colorScheme.background else Color.White, RectangleShape)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 30.dp)
        ) {
            items(tags) { tag ->
                // 计算该标签下的用户人数（排除自己）
                val count = contacts.count { it.tags.contains(tag) && it.id != currentUser.id }
                
                TagItem(
                    tag = tag,
                    count = count,
                    navController = navController,
                    onEdit = {
                        originalTag = tag
                        currentTag = tag
                        isEditing = true
                        showDialog = true
                    },
                    onDelete = {
                        AppDataStore.deleteTag(tag)
                    }
                )
            }
        }

        if (showDialog) {
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                delay(100)
                focusRequester.requestFocus()
            }
            
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text(if (isEditing) "编辑标签" else "添加标签") },
                shape = RoundedCornerShape(4.dp),
                text = {
                    OutlinedTextField(
                        value = currentTag,
                        onValueChange = { currentTag = it },
                        label = { Text("标签名称") },
                        modifier = Modifier.focusRequester(focusRequester),
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (currentTag.isNotBlank()) {
                                if (isEditing) {
                                    AppDataStore.updateTag(originalTag, currentTag)
                                } else {
                                    AppDataStore.addTag(currentTag)
                                    // Navigate to tag detail (service host selection) after creation
                                    navController.navigate(Screen.TagDetail.createRoute(currentTag))
                                }
                                showDialog = false
                            }
                        }
                    ) {
                        Text("确定")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

@Composable
fun TagItem(
    tag: String, 
    count: Int, 
    navController: NavController, 
    onEdit: () -> Unit, 
    onDelete: () -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()
    val dividerColor = if (isDarkTheme) Color(0xFF38383A) else Color(0xFFE5E5EA)

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { navController.navigate(Screen.TagDetail.createRoute(tag)) }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标 (使用标签图标，保持蓝色背景)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF3578E5)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Label,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 文本区域
            Text(
                text = "$tag (${count}人)",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            // 操作按钮 (保持在右侧，使用较小的图标以避免破坏行高)
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "编辑",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Divider(
            color = dividerColor,
            thickness = 0.5.dp,
            modifier = Modifier.padding(start = 68.dp)
        )
    }
}
