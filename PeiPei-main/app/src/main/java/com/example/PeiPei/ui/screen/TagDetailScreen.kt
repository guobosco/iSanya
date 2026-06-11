// 文件说明：单个标签下的内容或说明详情界面。

package com.example.Lulu.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import com.example.Lulu.data.local.MockDataStore
import com.example.Lulu.data.remote.RetrofitClient
import com.example.Lulu.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagDetailScreen(navController: NavController, tagName: String?) {
    if (tagName == null) return

    val allContacts by MockDataStore.contacts.collectAsState()
    val currentUser = MockDataStore.currentUser.collectAsState().value
    val tagContacts = allContacts.filter { it.tags.contains(tagName) && it.id != currentUser.id }
    
    var showAddDialog by remember { mutableStateOf(false) }
    
    // 搜索状态
    var searchQuery by remember { mutableStateOf("") }
    
    // 过滤后的联系人（供添加成员对话框使用）
    val filteredContacts = remember(allContacts, searchQuery, currentUser) {
        val contacts = allContacts.filter { it.id != currentUser.id }
        if (searchQuery.isBlank()) {
            contacts
        } else {
            contacts.filter {
                it.name.contains(searchQuery, ignoreCase = true) || 
                it.remarkName.contains(searchQuery, ignoreCase = true) 
            }
        }
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("$tagName (${tagContacts.size})", fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "添加/移除")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            items(tagContacts) { user ->
                ContactItem(user, MaterialTheme.colorScheme.primary, navController)
            }
        }
        
        if (showAddDialog) {
            // Multi-select dialog to add/remove contacts from this tag
            var selectedUserIds by remember { mutableStateOf(tagContacts.map { it.id }.toSet()) }
            
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("管理标签成员") },
                shape = RoundedCornerShape(4.dp),
                text = {
                    Column {
                        SearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                            items(filteredContacts) { user ->
                                val isSelected = selectedUserIds.contains(user.id)
                                val displayName = if (user.remarkName.isNotBlank()) user.remarkName else if (user.name.isNotBlank()) user.name else "未命名"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedUserIds = if (isSelected) {
                                                selectedUserIds - user.id
                                            } else {
                                                selectedUserIds + user.id
                                            }
                                        }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            selectedUserIds = if (checked) {
                                                selectedUserIds + user.id
                                            } else {
                                                selectedUserIds - user.id
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Surface(
                                        modifier = Modifier.size(36.dp),
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    ) {
                                        if (user.photoUrl.isNotEmpty()) {
                                            coil.compose.AsyncImage(
                                                model = RetrofitClient.normalizeBackendMediaUrlForDisplay(user.photoUrl),
                                                contentDescription = "Avatar",
                                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = displayName.take(1),
                                                    color = androidx.compose.ui.graphics.Color.White,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(displayName, fontSize = 16.sp)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        // Apply changes
                        allContacts.forEach { user ->
                            if (user.id == currentUser.id) return@forEach
                            val shouldHaveTag = selectedUserIds.contains(user.id)
                            val hasTag = user.tags.contains(tagName)
                            
                            if (shouldHaveTag && !hasTag) {
                                val updatedUser = user.copy(tags = user.tags + tagName)
                                MockDataStore.updateServiceHost(updatedUser)
                            } else if (!shouldHaveTag && hasTag) {
                                val updatedUser = user.copy(tags = user.tags - tagName)
                                MockDataStore.updateServiceHost(updatedUser)
                            }
                        }
                        searchQuery = ""
                        showAddDialog = false
                    }) {
                        Text("确定")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) { Text("取消") }
                }
            )
        }
    }
}
