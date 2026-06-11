// 文件说明：编辑服务主资料或关系的界面。

package com.example.Lulu.ui.screen

import android.widget.Toast
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.Lulu.R
import com.example.Lulu.data.local.MockDataStore
import com.example.Lulu.data.model.User
import com.example.Lulu.ui.components.CommonAvatar
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceHostEditScreen(navController: NavController, userId: String?, focusField: String? = null) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var user by remember { mutableStateOf<User?>(null) }
    
    val serviceHostTerm = stringResource(R.string.service_host_term)

    // Fetch global tags
    val allTags by MockDataStore.tags.collectAsState()
    
    // Focus requesters
    val remarkNameFocus = remember { FocusRequester() }
    val phoneFocus = remember { FocusRequester() }
    val memoFocus = remember { FocusRequester() }
    val tagFocus = remember { FocusRequester() } // Just for logic, not a real focusable field
    
    // Fetch user
    LaunchedEffect(userId) {
        userId?.let {
            user = MockDataStore.getUserById(it)
        }
    }

    // Edit states
    var remarkName by remember(user) { mutableStateOf(user?.remarkName ?: "") }
    var phoneNumber by remember(user) { mutableStateOf(user?.phoneNumber ?: "") }
    var memo by remember(user) { mutableStateOf(user?.memo ?: "") }
    var selectedTags by remember(user) { mutableStateOf(user?.tags ?: emptyList()) }
    var showTagDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Auto focus
    LaunchedEffect(user, focusField) {
        if (user != null && focusField != null) {
            kotlinx.coroutines.delay(300) // Delay to ensure UI is drawn
            when(focusField) {
                "remarkName" -> remarkNameFocus.requestFocus()
                "phoneNumber" -> phoneFocus.requestFocus()
                "memo" -> memoFocus.requestFocus()
                "tags" -> showTagDialog = true
            }
        }
    }

    // If user not found yet
    if (user == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("编辑${serviceHostTerm}资料", fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "删除", tint = Color.Gray)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar (Read-only)
            CommonAvatar(
                imageUrl = user!!.photoUrl,
                name = user!!.remarkName.ifBlank { user!!.name },
                modifier = Modifier.size(100.dp),
                shape = RoundedCornerShape(4.dp),
                textStyle = MaterialTheme.typography.displayMedium,
                contentDescription = "${serviceHostTerm}头像"
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            // Read-only Fields
            OutlinedTextField(
                value = user!!.name,
                onValueChange = { },
                label = { Text("昵称") },
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = Color.Gray,
                    disabledLabelColor = Color.Gray,
                    disabledBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = user!!.peiPeiId,
                onValueChange = { },
                label = { Text("i三亚 ID") },
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = Color.Gray,
                    disabledLabelColor = Color.Gray,
                    disabledBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Editable Fields
            OutlinedTextField(
                value = remarkName,
                onValueChange = { remarkName = it },
                label = { Text("备注名") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(remarkNameFocus),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))
            
            // Phone (Removed)
            /*
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { if (it.length <= 11 && it.all { char -> char.isDigit() }) phoneNumber = it },
                label = { Text("电话") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(phoneFocus),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            */
            
            // Tags Selection
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = if (selectedTags.isEmpty()) "无" else selectedTags.joinToString(", "),
                    onValueChange = {},
                    label = { Text("标签") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                // Overlay for click
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { showTagDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = memo,
                onValueChange = { memo = it },
                label = { Text("描述/备注") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(memoFocus),
                minLines = 3
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    // Validate phone (Removed validation since field is hidden)
                    /*
                    if (phoneNumber.isNotEmpty() && phoneNumber.length != 11) {
                        Toast.makeText(context, "请输入11位手机号", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    */
                    
                    user?.let { u ->
                        val updatedUser = u.copy(
                            remarkName = remarkName,
                            phoneNumber = phoneNumber,
                            memo = memo,
                            tags = selectedTags,
                            updatedAt = System.currentTimeMillis()
                        )
                        MockDataStore.updateServiceHost(updatedUser)
                        keyboardController?.hide()
                        Toast.makeText(context, "保存成功", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("保存")
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
        
        if (showTagDialog) {
            var newTagText by remember { mutableStateOf("") }
            val normalizedNewTag = newTagText.trim()
            
            AlertDialog(
                onDismissRequest = { showTagDialog = false },
                title = { Text("选择标签") },
                shape = RoundedCornerShape(4.dp),
                text = {
                    Column {
                        // Create New Tag Input
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            OutlinedTextField(
                                value = newTagText,
                                onValueChange = { newTagText = it },
                                label = { Text("新建标签") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            IconButton(
                                enabled = normalizedNewTag.isNotEmpty(),
                                onClick = {
                                    val existingTag = allTags.firstOrNull {
                                        it.equals(normalizedNewTag, ignoreCase = true)
                                    }
                                    if (existingTag != null) {
                                        val alreadySelected = selectedTags.contains(existingTag)
                                        if (!alreadySelected) {
                                            selectedTags = selectedTags + existingTag
                                        }
                                        newTagText = ""
                                        keyboardController?.hide()
                                        Toast.makeText(
                                            context,
                                            if (alreadySelected) "标签已存在" else "已添加已有标签",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        MockDataStore.addTag(normalizedNewTag)
                                        selectedTags = selectedTags + normalizedNewTag
                                        newTagText = ""
                                        keyboardController?.hide()
                                        Toast.makeText(context, "已新增标签", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.AddCircle,
                                    contentDescription = "Add",
                                    tint = if (normalizedNewTag.isNotEmpty()) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                        
                        Divider()
                        
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 200.dp)
                        ) {
                            items(allTags) { tag ->
                                val isSelected = selectedTags.contains(tag)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedTags = if (isSelected) {
                                                selectedTags - tag
                                            } else {
                                                selectedTags + tag
                                            }
                                        }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            selectedTags = if (checked) {
                                                selectedTags + tag
                                            } else {
                                                selectedTags - tag
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = tag)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showTagDialog = false }) {
                        Text("确定")
                    }
                }
            )
        }
        
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("删除$serviceHostTerm") },
                text = { Text("确定要删除该${serviceHostTerm}吗？此操作不可撤销。") },
                shape = RoundedCornerShape(4.dp),
                confirmButton = {
                    TextButton(
                        onClick = {
                            userId?.let { 
                                MockDataStore.deleteServiceHost(it)
                                Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show()
                                navController.popBackStack() // Back to list
                                navController.popBackStack() // Back to home (service host list / add flow)
                                // Ideally just pop to a safe place
                            }
                            showDeleteDialog = false
                        }
                    ) {
                        Text("删除", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}
