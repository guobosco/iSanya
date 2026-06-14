// 文件说明：全局或业务内搜索界面（本地联系人/服务 + 键盘搜索远程用户）。

package com.example.Lulu.ui.screen

import android.app.Application
import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.Lulu.R
import com.example.Lulu.data.local.AppDataStore
import com.example.Lulu.data.model.Service
import com.example.Lulu.data.model.ServiceCategories
import com.example.Lulu.data.model.User
import com.example.Lulu.data.remote.RetrofitClient
import com.example.Lulu.data.repository.UserRepository
import com.example.Lulu.ui.navigation.Screen
import com.example.Lulu.ui.theme.SurfaceWhite
import com.example.Lulu.util.UserScopedPrefs
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

private const val SEARCH_HISTORY_PREFS_BASE = "search_history"
private const val SEARCH_HISTORY_JSON_KEY = "queries"
private const val SEARCH_HISTORY_MAX = 30

private fun loadSearchHistory(prefs: SharedPreferences): List<String> {
    val raw = prefs.getString(SEARCH_HISTORY_JSON_KEY, "[]") ?: "[]"
    return runCatching {
        val arr = JSONArray(raw)
        buildList {
            for (i in 0 until arr.length()) {
                val s = arr.optString(i, "").trim()
                if (s.isNotEmpty()) add(s)
            }
        }
    }.getOrElse { emptyList() }
}

private fun saveSearchHistory(prefs: SharedPreferences, items: List<String>) {
    val arr = JSONArray()
    items.forEach { arr.put(it) }
    prefs.edit().putString(SEARCH_HISTORY_JSON_KEY, arr.toString()).apply()
}

@Composable
fun SearchScreen(navController: NavController) {
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userRepository = remember {
        UserRepository(context.applicationContext as Application)
    }

    val serviceHostTerm = stringResource(R.string.service_host_term)

    // Data sources
    val contacts by AppDataStore.contacts.collectAsState()
    val services by AppDataStore.services.collectAsState()
    val currentUser by AppDataStore.currentUser.collectAsState()

    val historyPrefs = remember(context, currentUser.id) {
        UserScopedPrefs.get(context, SEARCH_HISTORY_PREFS_BASE, currentUser.id)
    }
    var searchHistory by remember(currentUser.id) {
        mutableStateOf(loadSearchHistory(historyPrefs))
    }
    var guessRefreshKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(currentUser.id) {
        searchHistory = loadSearchHistory(historyPrefs)
    }

    fun appendSearchHistory(term: String) {
        val t = term.trim()
        if (t.isEmpty()) return
        val next = buildList {
            add(t)
            searchHistory.forEach { if (it != t) add(it) }
        }.take(SEARCH_HISTORY_MAX)
        searchHistory = next
        saveSearchHistory(historyPrefs, next)
    }

    fun clearSearchHistory() {
        searchHistory = emptyList()
        historyPrefs.edit().remove(SEARCH_HISTORY_JSON_KEY).apply()
    }

    var networkUser by remember { mutableStateOf<User?>(null) }
    var networkSearchError by remember { mutableStateOf<String?>(null) }
    var isNetworkSearching by remember { mutableStateOf(false) }

    fun clearRemoteLookup() {
        networkUser = null
        networkSearchError = null
        isNetworkSearching = false
    }

    LaunchedEffect(query) {
        clearRemoteLookup()
    }

    // Filtered results
    val filteredContacts = remember(query, contacts, currentUser.id) {
        val q = query.trim()
        if (q.isBlank()) emptyList()
        else contacts.filter {
            it.id != currentUser.id &&
                (it.name.contains(q, ignoreCase = true) ||
                    it.remarkName.contains(q, ignoreCase = true) ||
                    it.peiPeiId.contains(q, ignoreCase = true) ||
                    it.phoneNumber.contains(q))
        }
    }

    val filteredServices = remember(query, services) {
        if (query.isBlank()) emptyList()
        else services.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.description.contains(query, ignoreCase = true) ||
            it.location.contains(query, ignoreCase = true) ||
            it.priceText.contains(query, ignoreCase = true)
        }
    }

    LaunchedEffect(networkUser, filteredContacts, currentUser.id) {
        val u = networkUser ?: return@LaunchedEffect
        if (u.id == currentUser.id) {
            clearRemoteLookup()
            return@LaunchedEffect
        }
        if (filteredContacts.any { it.id == u.id }) {
            clearRemoteLookup()
            return@LaunchedEffect
        }
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        navController.navigate(Screen.ServiceHostProfile.createRoute(u.id)) {
            launchSingleTop = true
        }
        clearRemoteLookup()
    }

    // Auto-focus the search bar when entering the screen
    LaunchedEffect(Unit) {
        delay(100)
        runCatching {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    val leaveSearchScreen: () -> Unit = remember(navController, focusManager, keyboardController) {
        {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
            navController.popBackStack()
            Unit
        }
    }

    val openRoute: (String) -> Unit = remember(navController, focusManager, keyboardController) {
        { route: String ->
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
            navController.navigate(route)
        }
    }

    val submitRemoteLookup: () -> Unit = {
        val q = query.trim()
        if (q.isNotEmpty()) {
            appendSearchHistory(q)
            scope.launch {
                isNetworkSearching = true
                networkUser = null
                networkSearchError = null
                try {
                    val outcome = userRepository.lookupRemoteUserAndPersist(q)
                    when {
                        outcome.user != null -> networkUser = outcome.user
                        outcome.errorMessage != null -> networkSearchError = outcome.errorMessage
                    }
                } finally {
                    isNetworkSearching = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            SearchTopBar(
                query = query,
                onQueryChange = { query = it },
                onBack = leaveSearchScreen,
                focusRequester = focusRequester,
                onSearchSubmit = submitRemoteLookup
            )
        },
        containerColor = SurfaceWhite
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(SurfaceWhite)
                .padding(paddingValues),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
        ) {
            if (query.isNotBlank()) {
                // Contacts Section
                if (filteredContacts.isNotEmpty()) {
                    item {
                        SectionHeader("联系人")
                    }
                    items(filteredContacts) { contact ->
                        ContactSearchItem(contact) {
                            openRoute(Screen.ServiceHostProfile.createRoute(contact.id))
                        }
                        Divider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(start = 72.dp)
                        )
                    }
                }

                // Services Section
                if (filteredServices.isNotEmpty()) {
                    item {
                        SectionHeader("服务")
                    }
                    items(filteredServices) { service ->
                        ServiceSearchItem(service) {
                            openRoute(Screen.ServiceDetail.createRoute(service.id))
                        }
                        Divider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(start = 88.dp)
                        )
                    }
                }

                if (isNetworkSearching) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp))
                        }
                    }
                }

                if (!isNetworkSearching && networkSearchError != null) {
                    item {
                        Text(
                            text = networkSearchError!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                }

                // No results
                if (filteredContacts.isEmpty() && filteredServices.isEmpty() &&
                    !isNetworkSearching && networkUser == null && networkSearchError == null
                ) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "未找到相关内容",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "按键盘「搜索」可通过账号查找${serviceHostTerm}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            } else {
                item {
                    SearchHistorySection(
                        terms = searchHistory,
                        onClearAll = { clearSearchHistory() },
                        onPickTerm = { picked ->
                            query = picked
                            focusRequester.requestFocus()
                            keyboardController?.show()
                        }
                    )
                }
                item {
                    SearchGuessSection(
                        refreshKey = guessRefreshKey,
                        onRequestRefresh = { guessRefreshKey++ },
                        onPickTerm = { picked ->
                            query = picked
                            focusRequester.requestFocus()
                            keyboardController?.show()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchHistorySection(
    terms: List<String>,
    onClearAll: () -> Unit,
    onPickTerm: (String) -> Unit
) {
    val chipShape = RoundedCornerShape(999.dp)
    val chipBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, top = 4.dp, end = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.search_history_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(
                onClick = onClearAll,
                enabled = terms.isNotEmpty(),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = stringResource(R.string.search_history_clear),
                    tint = if (terms.isNotEmpty()) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                    }
                )
            }
        }
        if (terms.isNotEmpty()) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                terms.forEach { term ->
                    Text(
                        text = term,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(chipShape)
                            .background(chipBg)
                            .clickable { onPickTerm(term) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchGuessSection(
    refreshKey: Int,
    onRequestRefresh: () -> Unit,
    onPickTerm: (String) -> Unit
) {
    val suggestions = remember(refreshKey) {
        ServiceCategories.PRESETS.shuffled(Random(refreshKey * 4999L + 101L))
    }
    var menuExpanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, top = 28.dp, end = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.search_guess_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = stringResource(R.string.search_more_options),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.search_refresh_suggestions)) },
                        onClick = {
                            onRequestRefresh()
                            menuExpanded = false
                        }
                    )
                }
            }
        }
        Column(modifier = Modifier.padding(top = 16.dp)) {
            val rows = suggestions.chunked(2)
            rows.forEach { rowTerms ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    rowTerms.forEach { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onPickTerm(label) }
                        )
                    }
                    if (rowTerms.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun SearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    focusRequester: FocusRequester,
    onSearchSubmit: () -> Unit = {}
) {
    val pillShape = RoundedCornerShape(22.dp)
    val pillBorder = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    val pillBg = MaterialTheme.colorScheme.surface
    val placeholder = stringResource(R.string.search_placeholder)
    val searchLabel = stringResource(R.string.search_action)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(52.dp)
            .background(SurfaceWhite)
            .padding(start = 4.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.search_back_cd),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .border(1.dp, pillBorder, pillShape)
                .clip(pillShape)
                .background(pillBg)
                .padding(start = 12.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                singleLine = true,
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                text = placeholder,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                fontSize = 15.sp,
                                modifier = Modifier.align(Alignment.CenterStart)
                            )
                        }
                        innerTextField()
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearchSubmit() })
            )
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(R.string.search_clear_query_cd),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(22.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
            )
            Text(
                text = searchLabel,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onSearchSubmit)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 13.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun ContactSearchItem(user: User, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = if (user.remarkName.isNotEmpty()) user.remarkName else user.name,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (user.remarkName.isNotEmpty()) {
                Text(
                    text = "昵称: ${user.name}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ServiceSearchItem(service: Service, onClick: () -> Unit) {
    val coverUrl = remember(service.coverImageUrl, service.imageUrls) {
        service.coverImageUrl.takeIf { it.isNotBlank() }
            ?: service.imageUrls.firstOrNull { it.isNotBlank() }
            ?: ""
    }
    val thumbShape = RoundedCornerShape(14.dp)
    val thumbSize = 56.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(thumbSize)
                .clip(thumbShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            if (coverUrl.isNotBlank()) {
                AsyncImage(
                    model = RetrofitClient.normalizeBackendMediaUrlForDisplay(coverUrl),
                    contentDescription = service.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Storefront,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = service.title,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            val formatter = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
            val subtitle = listOfNotNull(
                ServiceCategories.normalize(service.category).takeIf { it.isNotBlank() },
                service.priceText.takeIf { it.isNotBlank() },
                service.serviceMode.takeIf { it.isNotBlank() },
                com.example.Lulu.util.ServiceLocationPolygonCodec.displayLine(service.location).takeIf { it.isNotBlank() }
            ).joinToString(" · ")
            Text(
                text = subtitle.ifBlank { formatter.format(Date(service.createdAt)) },
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
