// 文件说明：个人资料页相关的可复用 UI 片段。

package com.example.Lulu.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.LocationManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.Lulu.ui.theme.DialogTitleTopPadding
import com.example.Lulu.ui.util.findComposeDialogWindow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.Lulu.data.local.RegionData
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.delay

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush

@Composable
fun ProfileGroup(
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp),
    elevation: Dp = 0.dp,
    horizontalOuterPadding: Dp = 8.dp,
    verticalOuterPadding: Dp = 4.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalOuterPadding, vertical = verticalOuterPadding),
        shape = shape,
        color = backgroundColor,
        shadowElevation = elevation,
        tonalElevation = 0.dp
    ) {
        Column {
            content()
        }
    }
}

@Composable
fun ProfileItem(
    label: String,
    value: String? = null,
    icon: ImageVector? = null,
    showArrow: Boolean = false,
    showBadge: Boolean = false,
    labelColor: Color? = null,
    iconColor: Color? = null,
    iconBrush: Brush? = null,
    labelFontSize: TextUnit = 16.sp,
    onClick: () -> Unit,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val isDarkTheme = isSystemInDarkTheme()
    
    // 深色模式下，文本使用白色；浅色模式下，使用黑色
    val defaultTextColor = if (isDarkTheme) Color.White else Color.Black
    val textColor = labelColor ?: defaultTextColor
    val valueColor = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            if (iconBrush != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White, // Tint is ignored when using drawWithCache for gradient, but we need a base. Actually Icon doesn't support brush directly easily without modifier.
                    modifier = Modifier
                        .size(24.dp)
                        .drawWithCache {
                            onDrawWithContent {
                                drawContent()
                                drawRect(iconBrush, blendMode = BlendMode.SrcIn)
                            }
                        }
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor ?: (if (isDarkTheme) Color.LightGray else Color.DarkGray),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
        }
        
        Text(
            text = label,
            fontSize = labelFontSize,
            color = textColor,
            modifier = Modifier.weight(1f)
        )
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            if (showBadge) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color.Red, shape = androidx.compose.foundation.shape.CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            if (value != null) {
                Text(
                    text = value,
                    fontSize = 14.sp,
                    color = valueColor,
                    modifier = Modifier.padding(end = if (showArrow) 4.dp else 0.dp)
                )
            }
            
            if (trailingContent != null) {
                trailingContent()
                if (showArrow) Spacer(modifier = Modifier.width(4.dp))
            }

            if (showArrow) {
                Icon(
                    imageVector = Icons.Default.ArrowForwardIos,
                    contentDescription = null,
                    tint = valueColor.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun EditDialog(
    title: String,
    initialValue: String,
    isPhone: Boolean = false,
    maxLength: Int = Int.MAX_VALUE,
    validator: ((String) -> String?)? = null,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialValue) }
    var error by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val dismissKeyboard: () -> Unit = {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        Unit
    }

    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        containerColor = MaterialTheme.colorScheme.surface,
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { 
                        if (it.length <= maxLength) {
                            text = it 
                            error = null
                        }
                    },
                    modifier = Modifier.focusRequester(focusRequester),
                    label = { 
                        val labelText = if (isPhone) "请输入11位手机号" else "请输入内容"
                        Text(if (maxLength != Int.MAX_VALUE) "$labelText ($maxLength 字以内)" else labelText) 
                    },
                    singleLine = true,
                    isError = error != null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (isPhone) KeyboardType.Phone else KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { dismissKeyboard() }),
                    supportingText = if (maxLength != Int.MAX_VALUE) {
                        { Text("${text.length}/$maxLength") }
                    } else null
                )
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val validationError = validator?.invoke(text)
                    if (validationError != null) {
                        error = validationError
                    } else if (isPhone && (text.length != 11 || !text.all { it.isDigit() })) {
                        error = "手机号格式不正确"
                    } else {
                        onConfirm(text)
                        dismissKeyboard()
                    }
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                dismissKeyboard()
                onDismiss()
            }) { Text("取消") }
        }
    )
}

private val ProfileEditSheetTitleColor = Color(0xFF111111)
private val ProfileEditSheetBodyColor = Color(0xFF666666)
private val ProfileEditSheetPrimaryButton = Color(0xFF1F1F1F)

/**
 * 个人资料编辑用全屏 Dialog 底部需避让的高度（导航栏 + 键盘，px）。
 * Compose 的 [imePadding] 在独立 Dialog 窗口上经常为 0，故用 [ViewCompat] 监听 insets；
 * 并对 Dialog 窗口开启 [WindowCompat.setDecorFitsSystemWindows] + [SOFT_INPUT_ADJUST_RESIZE]。
 */
@Composable
internal fun rememberProfileEditDialogBottomInsetPx(): Int {
    val hostView = LocalView.current
    val insetState = remember { mutableIntStateOf(0) }
    DisposableEffect(hostView) {
        val window = hostView.findComposeDialogWindow()
        val previousSoftInputMode = window?.attributes?.softInputMode
        if (window != null) {
            WindowCompat.setDecorFitsSystemWindows(window, true)
            @Suppress("DEPRECATION")
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        val root = window?.decorView ?: hostView.rootView
        val listener = androidx.core.view.OnApplyWindowInsetsListener { _, insets ->
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val tap = insets.getInsets(WindowInsetsCompat.Type.tappableElement())
            insetState.intValue = maxOf(ime.bottom, nav.bottom, tap.bottom)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(root, listener)
        ViewCompat.requestApplyInsets(root)
        onDispose {
            ViewCompat.setOnApplyWindowInsetsListener(root, null)
            insetState.intValue = 0
            if (window != null) {
                WindowCompat.setDecorFitsSystemWindows(window, false)
                window.setSoftInputMode(
                    previousSoftInputMode ?: WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED
                )
            }
        }
    }
    return insetState.intValue
}

/**
 * 大号圆角自底部升起的文本编辑面板（个人资料编辑用），视觉对齐「关于你」等底部表单而非居中 AlertDialog。
 */
@Composable
fun ProfileTextEditBottomSheet(
    title: String,
    subtitle: String,
    fieldLabel: String,
    initialValue: String,
    maxLength: Int = Int.MAX_VALUE,
    isPhone: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else 12,
    infoLinkText: String? = null,
    onInfoLinkClick: (() -> Unit)? = null,
    validator: ((String) -> String?)? = null,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember(initialValue) { mutableStateOf(initialValue) }
    var error by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val dismissKeyboard: () -> Unit = {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        Unit
    }

    LaunchedEffect(Unit) {
        delay(50)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val bottomInsetPx = rememberProfileEditDialogBottomInsetPx()
        val bottomInsetDp = with(LocalDensity.current) { bottomInsetPx.toDp() }
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
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 24.dp)
                                .padding(top = DialogTitleTopPadding)
                        ) {
                        Text(
                            text = title,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = ProfileEditSheetTitleColor,
                            lineHeight = 34.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = subtitle,
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            color = ProfileEditSheetBodyColor
                        )
                        if (infoLinkText != null && onInfoLinkClick != null) {
                            TextButton(
                                onClick = onInfoLinkClick,
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(
                                    text = infoLinkText,
                                    fontSize = 13.sp,
                                    color = ProfileEditSheetBodyColor,
                                    textDecoration = TextDecoration.Underline
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = text,
                            onValueChange = {
                                if (it.length <= maxLength) {
                                    text = it
                                    error = null
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (singleLine) Modifier.heightIn(min = 56.dp) else Modifier.heightIn(min = 140.dp))
                                .focusRequester(focusRequester),
                            label = {
                                Text(
                                    fieldLabel,
                                    fontSize = 12.sp,
                                    color = ProfileEditSheetBodyColor
                                )
                            },
                            textStyle = TextStyle(
                                fontSize = 16.sp,
                                lineHeight = 22.sp,
                                color = ProfileEditSheetTitleColor
                            ),
                            singleLine = singleLine,
                            minLines = if (singleLine) 1 else minLines.coerceAtLeast(1),
                            maxLines = maxLines,
                            isError = error != null,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = if (isPhone) KeyboardType.Phone else KeyboardType.Text,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { dismissKeyboard() }),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ProfileEditSheetTitleColor,
                                unfocusedBorderColor = Color(0xFFCCCCCC),
                                cursorColor = ProfileEditSheetTitleColor,
                                focusedLabelColor = ProfileEditSheetBodyColor,
                                unfocusedLabelColor = ProfileEditSheetBodyColor,
                                errorBorderColor = MaterialTheme.colorScheme.error
                            )
                        )
                        if (maxLength != Int.MAX_VALUE) {
                            Text(
                                text = "${text.length}/$maxLength 个字符",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                color = ProfileEditSheetBodyColor,
                                fontSize = 12.sp,
                                textAlign = TextAlign.End
                            )
                        }
                        if (error != null) {
                            Text(
                                error!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
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
                            Icon(Icons.Default.Close, contentDescription = "关闭", tint = ProfileEditSheetTitleColor)
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = bottomInsetDp)
                    ) {
                        HorizontalDivider(color = Color(0xFFEDEDED), thickness = 1.dp)
                        Button(
                            onClick = {
                                val validationError = validator?.invoke(text)
                                if (validationError != null) {
                                    error = validationError
                                } else if (isPhone && (text.length != 11 || !text.all { it.isDigit() })) {
                                    error = "手机号格式不正确"
                                } else {
                                    dismissKeyboard()
                                    onSave(text)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 14.dp)
                                .height(52.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ProfileEditSheetPrimaryButton)
                        ) {
                            Text("保存", color = Color.White, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 底部单列选项面板（性别、年代等），与 [ProfileTextEditBottomSheet] 同一套视觉语言。
 */
@Composable
fun ProfileOptionPickerBottomSheet(
    title: String,
    subtitle: String,
    options: List<String>,
    initialSelection: String,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
    infoLinkText: String? = null,
    onInfoLinkClick: (() -> Unit)? = null
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = Color.White,
                shadowElevation = 16.dp
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .padding(top = DialogTitleTopPadding)
                        ) {
                        Text(
                            text = title,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = ProfileEditSheetTitleColor,
                            lineHeight = 34.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = subtitle,
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            color = ProfileEditSheetBodyColor
                        )
                        if (infoLinkText != null && onInfoLinkClick != null) {
                            TextButton(
                                onClick = onInfoLinkClick,
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(
                                    text = infoLinkText,
                                    fontSize = 13.sp,
                                    color = ProfileEditSheetBodyColor,
                                    textDecoration = TextDecoration.Underline
                                )
                            }
                        }
                    }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(end = 4.dp, top = DialogTitleTopPadding)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "关闭", tint = ProfileEditSheetTitleColor)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        items(options.size) { index ->
                            val option = options[index]
                            val displayEmptyAs = "未设置"
                            val label = if (option.isEmpty()) displayEmptyAs else option
                            val isSelected = if (initialSelection.isEmpty()) option.isEmpty() else option == initialSelection
                            Column {
                                Text(
                                    text = label,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onPick(option)
                                        }
                                        .padding(horizontal = 24.dp, vertical = 18.dp),
                                    fontSize = 16.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) ProfileEditSheetTitleColor else ProfileEditSheetBodyColor
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 24.dp),
                                    thickness = 0.5.dp,
                                    color = Color(0xFFEDEDED)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun initialWeightInputText(kg: Float): String {
    if (kg <= 0f) return ""
    val scaled = (kg * 10f).roundToInt() / 10f
    return if (abs(scaled - scaled.toInt()) < 1e-4f) scaled.toInt().toString()
    else String.format(Locale.US, "%.1f", scaled)
}

private fun sanitizeWeightTypingInput(raw: String): Boolean {
    if (raw.isEmpty()) return true
    if (!raw.all { it.isDigit() || it == '.' }) return false
    val parts = raw.split('.')
    if (parts.size > 2) return false
    if (parts[0].length > 3) return false
    if (parts.size == 2 && parts[1].length > 1) return false
    return true
}

@Composable
fun ProfileHeightWeightBottomSheet(
    heightCm: Int,
    weightKg: Float,
    heightWeightPrivate: Boolean,
    onDismiss: () -> Unit,
    onSave: (heightCm: Int, weightKg: Float, heightWeightPrivate: Boolean) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val dismissKeyboard: () -> Unit = {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        Unit
    }
    var heightText by remember(heightCm, weightKg, heightWeightPrivate) {
        mutableStateOf(if (heightCm > 0) heightCm.toString() else "")
    }
    var weightText by remember(heightCm, weightKg, heightWeightPrivate) {
        mutableStateOf(initialWeightInputText(weightKg))
    }
    var privateOthers by remember(heightCm, weightKg, heightWeightPrivate) {
        mutableStateOf(heightWeightPrivate)
    }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.88f),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = Color.White,
                shadowElevation = 16.dp
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 24.dp)
                                .padding(top = DialogTitleTopPadding)
                        ) {
                        Text(
                            text = "身高与体重",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = ProfileEditSheetTitleColor,
                            lineHeight = 34.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "选填。可只填其中一项。开启「对他人隐藏」后，只有自己能看到具体数字。",
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            color = ProfileEditSheetBodyColor
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        OutlinedTextField(
                            value = heightText,
                            onValueChange = {
                                if (it.length <= 3 && it.all { ch -> ch.isDigit() }) {
                                    heightText = it
                                    error = null
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text("身高（厘米）", fontSize = 12.sp, color = ProfileEditSheetBodyColor)
                            },
                            placeholder = {
                                Text("例如 168", color = ProfileEditSheetBodyColor.copy(alpha = 0.5f))
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ProfileEditSheetTitleColor,
                                unfocusedBorderColor = Color(0xFFCCCCCC),
                                cursorColor = ProfileEditSheetTitleColor,
                                focusedLabelColor = ProfileEditSheetBodyColor,
                                unfocusedLabelColor = ProfileEditSheetBodyColor
                            ),
                            textStyle = TextStyle(
                                fontSize = 16.sp,
                                lineHeight = 22.sp,
                                color = ProfileEditSheetTitleColor
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = weightText,
                            onValueChange = { new ->
                                if (new.length <= 6 && sanitizeWeightTypingInput(new)) {
                                    weightText = new
                                    error = null
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text("体重（千克）", fontSize = 12.sp, color = ProfileEditSheetBodyColor)
                            },
                            placeholder = {
                                Text("例如 58 或 58.5", color = ProfileEditSheetBodyColor.copy(alpha = 0.5f))
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { dismissKeyboard() }),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ProfileEditSheetTitleColor,
                                unfocusedBorderColor = Color(0xFFCCCCCC),
                                cursorColor = ProfileEditSheetTitleColor,
                                focusedLabelColor = ProfileEditSheetBodyColor,
                                unfocusedLabelColor = ProfileEditSheetBodyColor
                            ),
                            textStyle = TextStyle(
                                fontSize = 16.sp,
                                lineHeight = 22.sp,
                                color = ProfileEditSheetTitleColor
                            )
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("对他人隐藏", color = ProfileEditSheetTitleColor, fontSize = 16.sp)
                                Text(
                                    "其他人不会看到具体数字",
                                    color = ProfileEditSheetBodyColor,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            Switch(
                                checked = privateOthers,
                                onCheckedChange = { privateOthers = it }
                            )
                        }
                        if (error != null) {
                            Text(
                                error!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
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
                            Icon(Icons.Default.Close, contentDescription = "关闭", tint = ProfileEditSheetTitleColor)
                        }
                    }
                    HorizontalDivider(color = Color(0xFFEDEDED), thickness = 1.dp)
                    Button(
                        onClick = {
                            val h = if (heightText.isBlank()) {
                                0
                            } else {
                                heightText.trim().toIntOrNull() ?: run {
                                    error = "请输入有效身高"
                                    return@Button
                                }
                            }
                            val wNormalized = weightText.trim().replace(',', '.').trimEnd('.')
                            val w = if (wNormalized.isEmpty()) {
                                0f
                            } else {
                                wNormalized.toFloatOrNull() ?: run {
                                    error = "请输入有效体重"
                                    return@Button
                                }
                            }
                            if (heightText.isNotBlank() && (h < 50 || h > 250)) {
                                error = "身高需在 50–250 cm"
                                return@Button
                            }
                            if (weightText.isNotBlank() && (w < 20f || w > 300f)) {
                                error = "体重需在 20–300 kg"
                                return@Button
                            }
                            val finalH = if (heightText.isBlank()) 0 else h
                            val finalW = if (weightText.isBlank()) 0f else w
                            dismissKeyboard()
                            onSave(finalH, finalW, privateOthers)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 14.dp)
                            .height(52.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ProfileEditSheetPrimaryButton)
                    ) {
                        Text("保存", color = Color.White, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

/**
 * 选择地区弹层顶部「定位城市」一键选项（与下方省市区列表的确定回调格式一致）。
 */
data class RegionPickerLocatedCityOption(
    val cityLabel: String,
    val regionForConfirm: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegionPickerDialog(
    initialRegion: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    locatedCityOption: RegionPickerLocatedCityOption? = null,
    /** 逆地理成功时回调原始串（如省+市连写），用于刷新顶部「定位城市」等 */
    onDeviceLocationResolved: ((String) -> Unit)? = null,
) {
    val context = LocalContext.current
    
    val isDarkTheme = isSystemInDarkTheme()

    // Mock Data
    val provinces = RegionData.provinces
    val citiesMap = RegionData.cities
    
    // Picker States
    val provinceListState = rememberLazyListState()
    val cityListState = rememberLazyListState()

    var selectedProvince by remember { 
        mutableStateOf(
            provinces.find { initialRegion.startsWith(it) } ?: provinces.first()
        ) 
    }
    
    // Initialize selectedCity based on initialRegion or default to first city of selectedProvince
    var selectedCity by remember { 
        mutableStateOf(
            citiesMap[selectedProvince]?.find { initialRegion.endsWith(it) } 
            ?: citiesMap[selectedProvince]?.firstOrNull() 
            ?: ""
        )
    }

    // Scroll to selected items when they change
    LaunchedEffect(selectedProvince) {
        val index = provinces.indexOf(selectedProvince)
        if (index >= 0) {
            provinceListState.animateScrollToItem(index)
        }
    }

    LaunchedEffect(selectedCity) {
        val cities = citiesMap[selectedProvince] ?: emptyList()
        val index = cities.indexOf(selectedCity)
        if (index >= 0) {
            cityListState.animateScrollToItem(index)
        }
    }

    var isLoadingLocation by remember { mutableStateOf(false) }
    var locationResultText by remember { mutableStateOf<String?>(null) }

    // Permission Launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            isLoadingLocation = true
            getLocation(context) { locationStr ->
                isLoadingLocation = false
                if (locationStr != null) {
                    locationResultText = locationStr
                    // Try to match with our data
                    val matchedProvince = provinces.find { locationStr.contains(it) }
                    if (matchedProvince != null) {
                        selectedProvince = matchedProvince
                        val matchedCity = citiesMap[matchedProvince]?.find { locationStr.contains(it) }
                        if (matchedCity != null) {
                            selectedCity = matchedCity
                        }
                    } else {
                        Toast.makeText(context, "未能在列表中找到定位地点: $locationStr", Toast.LENGTH_SHORT).show()
                    }
                    onDeviceLocationResolved?.invoke(locationStr)
                } else {
                    Toast.makeText(context, "无法获取位置信息", Toast.LENGTH_SHORT).show()
                    locationResultText = "无法获取位置"
                }
            }
        } else {
            Toast.makeText(context, "需要定位权限才能自动获取位置", Toast.LENGTH_SHORT).show()
        }
    }

    val regionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = regionSheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = DialogTitleTopPadding, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) { Text("取消", color = Color.Gray) }
                Text("选择地区", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                TextButton(onClick = {
                    val region =
                        if (selectedProvince == selectedCity) selectedProvince
                        else "$selectedProvince $selectedCity"
                    onConfirm(region)
                }) {
                    Text("确定", color = MaterialTheme.colorScheme.primary)
                }
            }
            
            Divider()

            locatedCityOption?.let { opt ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onConfirm(opt.regionForConfirm) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "定位城市",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = opt.cityLabel,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Divider()
            }
            
            // Location Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                         locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                if (isLoadingLocation) {
                    Text("正在定位...", color = Color.Gray)
                    Spacer(modifier = Modifier.width(8.dp))
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else if (locationResultText != null) {
                    Text("已定位: $locationResultText", color = MaterialTheme.colorScheme.primary)
                } else {
                    Text("定位当前位置", color = MaterialTheme.colorScheme.primary)
                }
            }

            Divider(thickness = 8.dp, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

            // Picker Columns
            Row(modifier = Modifier.fillMaxSize()) {
                // Province List
                LazyColumn(
                    state = provinceListState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(if (isDarkTheme) Color(0xFF2C2C2E) else Color(0xFFF5F5F5))
                ) {
                    items(provinces.size) { index ->
                        val province = provinces[index]
                        val isSelected = province == selectedProvince
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    selectedProvince = province
                                    // Manually reset city when province changes
                                    selectedCity = citiesMap[province]?.firstOrNull() ?: ""
                                }
                                .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = province,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
                
                // City List
                LazyColumn(
                    state = cityListState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    val cities = citiesMap[selectedProvince] ?: emptyList()
                    items(cities.size) { index ->
                        val city = cities[index]
                        val isSelected = city == selectedCity
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedCity = city }
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = city,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 头像角标：盾牌 + 对勾 / 叉号；已认证为主题主色，未认证为弱化灰 */
@Composable
fun IdentityShieldWithMark(
    verified: Boolean,
    modifier: Modifier = Modifier,
    shieldSize: Dp = 28.dp,
    markSize: Dp = 11.dp,
    unverifiedGray: Color? = null
) {
    val scheme = MaterialTheme.colorScheme
    val muted = unverifiedGray ?: scheme.onSurfaceVariant.copy(alpha = 0.65f)
    val verifiedTint = scheme.primary
    Box(modifier = modifier.size(shieldSize + 2.dp), contentAlignment = Alignment.Center) {
        Icon(
            imageVector = Icons.Filled.Shield,
            contentDescription = if (verified) "身份已认证" else "身份未认证",
            tint = if (verified) verifiedTint else muted,
            modifier = Modifier.size(shieldSize)
        )
        Icon(
            imageVector = if (verified) Icons.Filled.Check else Icons.Filled.Close,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(markSize)
        )
    }
}

/** 叠在头像右下：盾牌角标；已通过时在盾牌正下方显示极小「已实名」。 */
@Composable
fun AvatarIdentityShieldOverlay(
    verified: Boolean,
    modifier: Modifier = Modifier,
    shieldSize: Dp = 28.dp,
    markSize: Dp = 11.dp,
    unverifiedGray: Color? = null
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IdentityShieldWithMark(
            verified = verified,
            shieldSize = shieldSize,
            markSize = markSize,
            unverifiedGray = unverifiedGray
        )
        if (verified) {
            Text(
                text = "已实名",
                fontSize = 7.sp,
                lineHeight = 8.sp,
                fontWeight = FontWeight.Medium,
                color = scheme.primary,
                maxLines = 1
            )
        }
    }
}

/**
 * 个人资料页实名入口条：无底色；「开始实名认证」带下划线可点击，配色跟随 Material 主题。
 * 仅在未认证时展示（由调用方控制显隐）。
 */
@Composable
fun IdentityVerificationCallout(
    onStartVerification: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onStartVerification)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "开始实名认证",
            color = scheme.primary,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            style = TextStyle(textDecoration = TextDecoration.Underline)
        )
    }
}

@SuppressLint("MissingPermission")
private fun getLocation(context: Context, onLocationFound: (String?) -> Unit) {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val providers = locationManager.getProviders(true)
    var location = if (providers.contains(LocationManager.GPS_PROVIDER)) {
        locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
    } else if (providers.contains(LocationManager.NETWORK_PROVIDER)) {
        locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
    } else {
        null
    }

    // Try Network provider if GPS returned null but Network is available
    if (location == null && providers.contains(LocationManager.NETWORK_PROVIDER)) {
        location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
    }

    if (location != null) {
        try {
            val geocoder = Geocoder(context, Locale.CHINA)
            Thread {
                try {
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    val address = addresses?.firstOrNull()
                    val result = if (address != null) {
                        // AdminArea is usually Province, Locality is City
                        val province = address.adminArea ?: ""
                        val city = address.locality ?: address.subAdminArea ?: ""
                        "$province$city"
                    } else {
                        null
                    }
                    // Callback on main thread
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        onLocationFound(result)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        onLocationFound(null)
                    }
                }
            }.start()
        } catch (e: Exception) {
            e.printStackTrace()
            onLocationFound(null)
        }
    } else {
        onLocationFound(null)
    }
}
