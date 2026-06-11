// 文件说明：曾就读院校选择器——数据来自 assets JSON，搜索 + 分栏 + 虚拟列表，未收录可手动输入。

package com.example.Lulu.ui.components

import android.view.WindowManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.Lulu.ui.theme.DialogTitleTopPadding
import androidx.compose.ui.window.DialogProperties
import com.example.Lulu.ui.util.findComposeDialogWindow
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader

private val SheetTitleColor = Color(0xFF111111)
private val SheetBodyColor = Color(0xFF666666)
private val SheetPrimaryButton = Color(0xFF1F1F1F)

private const val MAX_LIST_RESULTS = 200

private data class UniversityCatalogDto(
    @SerializedName("hotChina") val hotChina: List<String>? = null,
    @SerializedName("china") val china: List<String>? = null,
    @SerializedName("international") val international: List<String>? = null
)

private data class UniversityCatalog(
    val hotChina: List<String>,
    val china: List<String>,
    val international: List<String>
)

private suspend fun loadUniversityCatalog(context: android.content.Context): UniversityCatalog =
    withContext(Dispatchers.IO) {
        context.assets.open("universities.json").use { stream ->
            val dto = Gson().fromJson(InputStreamReader(stream, Charsets.UTF_8), UniversityCatalogDto::class.java)
            UniversityCatalog(
                hotChina = dto.hotChina.orEmpty().distinct(),
                china = dto.china.orEmpty().distinct(),
                international = dto.international.orEmpty().distinct()
            )
        }
    }

private fun filterUniversities(
    catalog: UniversityCatalog,
    tabIndex: Int,
    query: String
): List<String> {
    val pool = when (tabIndex) {
        0 -> catalog.china
        else -> catalog.international
    }
    val q = query.trim()
    if (q.isEmpty()) {
        return if (tabIndex == 0) {
            val hot = catalog.hotChina.filter { it in pool.toSet() }
            if (hot.isNotEmpty()) hot else pool.take(40)
        } else {
            pool.take(50)
        }
    }
    return pool.filter { it.contains(q, ignoreCase = true) }.take(MAX_LIST_RESULTS)
}

/**
 * 大学选择器：分「中国及港澳台 / 国外院校」两栏，支持搜索（全文库在 JSON 中，可独立扩充）；
 * 空搜索时展示热门或列表前段，避免一次性渲染数千条；未收录可手动输入。
 */
@Composable
fun ProfileUniversityPickerBottomSheet(
    initialValue: String,
    maxLength: Int = 80,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val dismissKeyboard: () -> Unit = {
        focusManager.clearFocus(force = true)
        keyboard?.hide()
        Unit
    }
    var catalog by remember { mutableStateOf<UniversityCatalog?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var tabIndex by remember { mutableIntStateOf(0) }
    var search by remember { mutableStateOf("") }
    var customMode by remember { mutableStateOf(false) }
    var customText by remember { mutableStateOf(initialValue) }

    LaunchedEffect(Unit) {
        try {
            catalog = loadUniversityCatalog(context)
        } catch (e: Exception) {
            loadError = e.message ?: "加载失败"
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val hostView = LocalView.current
        DisposableEffect(hostView) {
            val window = hostView.findComposeDialogWindow()
            if (window != null) {
                val previousMode = window.attributes.softInputMode
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                onDispose { window.setSoftInputMode(previousMode) }
            } else {
                onDispose { }
            }
        }
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = Color.White,
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .imePadding()
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .padding(top = DialogTitleTopPadding)
                        ) {
                        Text(
                            text = "曾就读于哪里？",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = SheetTitleColor,
                            lineHeight = 34.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "从列表选择或搜索校名；库中未收录可手动填写。列表数据在应用中持续扩充。",
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            color = SheetBodyColor
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                        IconButton(
                            onClick = {
                                dismissKeyboard()
                                onDismiss()
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(end = 4.dp, top = DialogTitleTopPadding)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "关闭", tint = SheetTitleColor)
                        }
                    }

                    if (loadError != null) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = loadError!!, color = MaterialTheme.colorScheme.error)
                        }
                    } else if (catalog == null) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (customMode) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                        ) {
                            Text(
                                text = "手动输入学校名称",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SheetTitleColor
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = customText,
                                onValueChange = { if (it.length <= maxLength) customText = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("学校名称", color = SheetBodyColor) },
                                singleLine = true,
                                textStyle = TextStyle(fontSize = 16.sp, color = SheetTitleColor),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { dismissKeyboard() }),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SheetTitleColor,
                                    unfocusedBorderColor = Color(0xFFCCCCCC),
                                    cursorColor = SheetTitleColor
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { customMode = false }) {
                                Text("返回列表选择", color = SheetBodyColor, textDecoration = TextDecoration.Underline)
                            }
                        }
                    } else {
                        val cat = catalog!!
                        TabRow(selectedTabIndex = tabIndex) {
                            Tab(
                                selected = tabIndex == 0,
                                onClick = {
                                    dismissKeyboard()
                                    tabIndex = 0
                                },
                                text = { Text("中国及港澳台") }
                            )
                            Tab(
                                selected = tabIndex == 1,
                                onClick = {
                                    dismissKeyboard()
                                    tabIndex = 1
                                },
                                text = { Text("国外院校") }
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = search,
                            onValueChange = { search = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            placeholder = { Text(if (tabIndex == 0) "搜索大陆与港澳台高校…" else "搜索国外院校…") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 16.sp, color = SheetTitleColor),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { dismissKeyboard() }),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SheetTitleColor,
                                unfocusedBorderColor = Color(0xFFCCCCCC),
                                cursorColor = SheetTitleColor
                            )
                        )
                        val rows = remember(cat, tabIndex, search) {
                            filterUniversities(cat, tabIndex, search)
                        }
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            contentPadding = PaddingValues(bottom = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            items(rows, key = { it }) { name ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            dismissKeyboard()
                                            onSave(name.trim())
                                        }
                                        .padding(horizontal = 24.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = name,
                                        fontSize = 16.sp,
                                        color = SheetTitleColor,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 24.dp),
                                    color = Color(0xFFEDEDED)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFFEDEDED), thickness = 1.dp)
                    if (catalog != null && loadError == null) {
                        if (customMode) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                TextButton(
                                    onClick = {
                                        dismissKeyboard()
                                        customMode = false
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("取消", color = SheetBodyColor)
                                }
                                Button(
                                    onClick = {
                                        val t = customText.trim()
                                        if (t.isNotEmpty()) {
                                            dismissKeyboard()
                                            onSave(t)
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SheetPrimaryButton)
                                ) {
                                    Text("保存", color = Color.White, fontSize = 16.sp)
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 10.dp)
                            ) {
                                TextButton(
                                    onClick = {
                                        dismissKeyboard()
                                        customText = initialValue
                                        customMode = true
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    Text(
                                        "未找到？手动输入学校名称",
                                        color = SheetBodyColor,
                                        textDecoration = TextDecoration.Underline
                                    )
                                }
                                Button(
                                    onClick = {
                                        dismissKeyboard()
                                        onDismiss()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF0F0F0))
                                ) {
                                    Text("暂不选择", color = SheetTitleColor, fontSize = 16.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
