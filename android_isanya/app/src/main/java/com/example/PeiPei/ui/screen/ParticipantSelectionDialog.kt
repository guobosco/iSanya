// 文件说明：选择聊天或活动参与者的对话框。

package com.example.Lulu.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.Lulu.data.local.AppDataStore
import com.example.Lulu.data.remote.RetrofitClient
import com.example.Lulu.data.model.User
import com.example.Lulu.ui.theme.DialogTitleTopPadding

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParticipantSelectionDialog(
    initialSelection: List<User>,
    onDismiss: () -> Unit,
    onConfirm: (List<User>) -> Unit
) {
    val contacts by AppDataStore.contacts.collectAsState()
    val tags by AppDataStore.tags.collectAsState()
    val currentUser by AppDataStore.currentUser.collectAsState()
    
    val selected = remember { mutableStateListOf<User>().apply { addAll(initialSelection) } }
    
    // 筛选模式
    var filterMode by remember { mutableStateOf(0) } // 0: 全部, 1: 标签
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var showSelectedList by remember { mutableStateOf(false) }
    
    // 计算当前显示的联系人列表
    val displayedContacts = remember(contacts, filterMode, selectedTag, currentUser) {
        val validContacts = contacts.filter { it.id != currentUser.id }

        val list = when (filterMode) {
            1 -> if (selectedTag != null) validContacts.filter { it.tags.contains(selectedTag) } else emptyList()
            else -> validContacts
        }
        
        if (filterMode == 0) {
            if (list.any { it.id == currentUser.id }) {
                list
            } else {
                listOf(currentUser) + list
            }
        } else {
            list
        }
    }
    
    // 全选/全不选逻辑
    val isAllSelected = displayedContacts.isNotEmpty() && displayedContacts.all { contact -> selected.any { it.id == contact.id } }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(600.dp),
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 标题
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = DialogTitleTopPadding, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "选择服务对象", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        text = "已选 ${selected.size} 人", 
                        fontSize = 14.sp, 
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { if (selected.isNotEmpty()) showSelectedList = true }
                    )
                }
                
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                
                // 筛选栏
                Column(modifier = Modifier.fillMaxWidth()) {
                    // 顶部 Tab
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        listOf("全部", "标签").forEachIndexed { index, title ->
                            Column(
                                modifier = Modifier.clickable { 
                                    filterMode = index 
                                    if (index == 1 && tags.isNotEmpty()) selectedTag = tags.first()
                                },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = title,
                                    color = if (filterMode == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (filterMode == index) FontWeight.Bold else FontWeight.Normal
                                )
                                if (filterMode == index) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 4.dp)
                                            .size(width = 20.dp, height = 2.dp)
                                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    )
                                }
                            }
                        }
                    }
                    
                    // 子筛选器
                    if (filterMode == 1) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(tags) { tag: String ->
                                FilterChip(
                                    selected = selectedTag == tag,
                                    onClick = { selectedTag = tag },
                                    label = { Text(text = tag) },
                                    enabled = true,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color.Transparent,
                                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                                        selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = selectedTag == tag,
                                        borderColor = MaterialTheme.colorScheme.outline,
                                        selectedBorderColor = MaterialTheme.colorScheme.primary,
                                        selectedBorderWidth = 1.dp
                                    )
                                )
                            }
                        }
                    }
                    
                    // 全选栏
                    if (filterMode != 0 || displayedContacts.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    if (isAllSelected) {
                                        // 全不选 (只移除当前显示的)
                                        displayedContacts.forEach { contact ->
                                            selected.removeAll { it.id == contact.id }
                                        }
                                    } else {
                                        // 全选 (添加当前显示且未选中的)
                                        displayedContacts.forEach { contact ->
                                            if (selected.none { it.id == contact.id }) {
                                                selected.add(contact)
                                            }
                                        }
                                    }
                                }
                            ) {
                                Text(if (isAllSelected) "取消全选" else "全选")
                            }
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                // 列表
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    if (displayedContacts.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("暂无联系人", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        items(displayedContacts) { user ->
                            val isSelected = selected.any { it.id == user.id }
                            val displayName = if (user.remarkName.isNotBlank()) user.remarkName else if (user.name.isNotBlank()) user.name else "未命名"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isSelected) {
                                            selected.removeAll { it.id == user.id }
                                        } else {
                                            selected.add(user)
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Checkbox
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                                        .border(
                                            width = 1.dp, 
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                // 头像
                                Surface(
                                    modifier = Modifier.size(40.dp),
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
                                                text = displayName.firstOrNull()?.toString() ?: "?",
                                                color = Color.White,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                Column {
                                    Text(text = displayName, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                                    // 显示标签 (可选)
                                    if (user.tags.isNotEmpty()) {
                                        Text(
                                            text = user.tags.joinToString(", "),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                
                // 按钮
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(selected) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("确定")
                    }
                }
            }
        }
    }

    if (showSelectedList) {
        Dialog(onDismissRequest = { showSelectedList = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = DialogTitleTopPadding, bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "已选联系人",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showSelectedList = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                    
                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        items(selected, key = { it.id }) { user ->
                            val displayName = if (user.remarkName.isNotBlank()) user.remarkName else if (user.name.isNotBlank()) user.name else "未命名"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier.size(32.dp),
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
                                                text = displayName.firstOrNull()?.toString() ?: "?", 
                                                fontSize = 14.sp, 
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                Text(
                                    text = displayName,
                                    modifier = Modifier.weight(1f),
                                    fontSize = 16.sp
                                )
                                
                                IconButton(onClick = { selected.remove(user) }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
