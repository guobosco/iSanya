// 文件说明：业务聚合仓库，协调本地数据库与远程 API（主业务数据）。

package com.example.Lulu.data.repository

/**
 * 应用核心仓库实现文件。
 * 负责整合本地数据库与远程接口，并统一处理用户、好友、服务和聊天同步逻辑。
 */

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.Lulu.BuildConfig
import com.example.Lulu.data.local.dao.ChatMessageDao
import com.example.Lulu.data.local.dao.ConversationDao
import com.example.Lulu.data.local.dao.ConversationMemberDao
import com.example.Lulu.data.local.dao.ServiceDao
import com.example.Lulu.data.local.dao.UserDao
import com.example.Lulu.data.model.ChatConversation
import com.example.Lulu.data.model.ChatConversationMember
import com.example.Lulu.data.model.ChatMessage
import com.example.Lulu.data.model.ChatMessageCreateRequest
import com.example.Lulu.data.model.ChatReadRequest
import com.example.Lulu.data.model.ChatSocketEvent
import com.example.Lulu.data.model.ChatUploadResponse
import com.example.Lulu.data.model.HostIncomingOrder
import com.example.Lulu.data.model.Service
import com.example.Lulu.data.model.ServiceCategories
import com.example.Lulu.data.model.ServiceDeclarations
import com.example.Lulu.data.model.User
import com.example.Lulu.data.remote.ApiService
import com.example.Lulu.data.remote.AuthSession
import com.example.Lulu.data.remote.ChatWebSocketClient
import com.example.Lulu.data.remote.FavoriteServicesPayload
import com.example.Lulu.data.remote.RegisterRequest
import com.example.Lulu.data.remote.WishlistGroupPayload
import com.example.Lulu.data.remote.WishlistProfilePayload
import com.example.Lulu.util.BookingTimeRangesCodec
import com.google.gson.Gson
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.HttpException

class LuluRepository(
    private val userDao: UserDao,
    private val serviceDao: ServiceDao,
    private val conversationDao: ConversationDao,
    private val conversationMemberDao: ConversationMemberDao,
    private val chatMessageDao: ChatMessageDao,
    private val sharedPrefs: SharedPreferences,
    private val context: Context,
    private val apiService: ApiService? = null
) {
    // #region debug-point A:android-home-empty-reporter
    private fun reportAndroidHomeEmptyDebug(
        hypothesisId: String,
        location: String,
        msg: String,
        data: Map<String, Any?> = emptyMap()
    ) {
        runCatching {
            Thread {
                runCatching {
                    val connection = (URL("http://192.168.43.160:7777/event").openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        doOutput = true
                        connectTimeout = 1500
                        readTimeout = 1500
                        setRequestProperty("Content-Type", "application/json")
                    }
                    val payload = JSONObject().apply {
                        put("sessionId", "android-home-empty")
                        put("runId", "pre-fix")
                        put("hypothesisId", hypothesisId)
                        put("location", location)
                        put("msg", "[DEBUG] $msg")
                        put("ts", System.currentTimeMillis())
                        put("data", JSONObject(data.filterValues { it != null }))
                    }
                    connection.outputStream.use { it.write(payload.toString().toByteArray()) }
                    connection.inputStream.close()
                    connection.disconnect()
                }
            }.start()
        }
    }
    // #endregion

    enum class FavoriteSyncError {
        NETWORK,
        SERVICE_NOT_FOUND,
        SERVER
    }

    data class FavoriteToggleResult(
        val favoriteIds: Set<String>,
        val syncFailed: Boolean,
        val errorType: FavoriteSyncError? = null
    )

    data class WishlistProfileState(
        val favoriteIds: Set<String>,
        val groups: List<String>,
        val favoriteServiceGroups: Map<String, String>
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshMutex = Mutex()
    /**
     * 发现流「读ahead」游标：与首屏 `skip=0` 拉取衔接，用于滚动到底时预取下一页写入 Room。
     * 大厂信息流常见策略：首屏快显 + 提前拉下一屏，减少用户滑到底时的等待（仍受 [refreshMutex] 与网络约束）。
     */
    private val discoveryFeedPrefetchSkip = AtomicInteger(0)
    /** 心愿单远程写操作串行化，避免批量加入等场景下多个协程并发覆盖。 */
    private val favoriteRemoteSyncMutex = Mutex()

    private val _wishlistRemoteSyncFailures = MutableSharedFlow<String>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    /** 心愿单远程写失败时发出文案，供界面 Snackbar/Toast 提示。 */
    val wishlistRemoteSyncFailures: SharedFlow<String> = _wishlistRemoteSyncFailures.asSharedFlow()

    private fun wishlistSyncFailureUserMessage(throwable: Throwable?): String {
        return when (val t = throwable) {
            is IOException -> "网络异常，心愿单同步失败，请稍后重试"
            is HttpException -> when (t.code()) {
                404 -> "服务不存在或已下架，心愿单同步失败"
                else -> "心愿单同步失败，请稍后重试"
            }
            null -> "心愿单同步失败，请稍后重试"
            else -> "心愿单同步失败，请稍后重试"
        }
    }
    private val gson = Gson()
    private var chatSocketConnected = false
    private val chatSocketClient = ChatWebSocketClient(
        onTextMessage = { text -> scope.launch { handleChatSocketEvent(text) } },
        onConnectedChanged = { connected -> chatSocketConnected = connected }
    )

    private val _currentUserId = MutableStateFlow(sharedPrefs.getString("current_user_id", "") ?: "")
    val currentUserIdFlow: StateFlow<String> = _currentUserId
    val currentUserId: String
        get() = _currentUserId.value

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentUser: Flow<User?> = _currentUserId.flatMapLatest { userId ->
        if (userId.isBlank()) kotlinx.coroutines.flow.flowOf(null) else userDao.getUserById(userId)
    }
    val favoriteServiceIds: Flow<Set<String>> = currentUser.map { user ->
        user?.favoriteServiceIds?.toSet() ?: emptySet()
    }

    val allUsers: Flow<List<User>> = userDao.getAllUsers()

    /**
     * 心愿单/服务查找等：本人全部服务 + 广场缓存 + 当前用户首页注入种子（`seed-home-{userId}-%`），按 id 去重。
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val allServices: Flow<List<Service>> = _currentUserId.flatMapLatest { userId ->
        if (userId.isBlank()) {
            serviceDao.getSquareDiscoveryServicesFlow()
        } else {
            combine(
                serviceDao.getServicesFlow(userId),
                serviceDao.getSquareDiscoveryServicesFlow()
            ) { mine, square ->
                (mine + square).distinctBy { it.id }
                    .sortedWith(
                        compareByDescending<Service> { it.updatedAt }
                            .thenByDescending { it.createdAt }
                    )
            }
        }
    }

    /** 当前用户创建的全部服务（已发布 / 草稿 / 已下架），用于「我发布的服务」管理页 */
    @OptIn(ExperimentalCoroutinesApi::class)
    val myCreatedListingsHub: Flow<List<Service>> = _currentUserId.flatMapLatest { userId ->
        if (userId.isBlank()) flowOf(emptyList())
        else serviceDao.getMyCreatedServicesHubFlow(userId)
    }

    /**
     * 作为服务发布者的接单列表。尚无后端订单接口时保持为空；接入后在此写入/合并。
     */
    private val _hostIncomingOrders = MutableStateFlow<List<HostIncomingOrder>>(emptyList())
    val hostIncomingOrders: StateFlow<List<HostIncomingOrder>> = _hostIncomingOrders.asStateFlow()

    /** 首页「发现」瀑布流：最近更新的本地缓存子集（见 [ServiceDao.getHomeDiscoveryFeedServicesFlow]） */
    val squareDiscoveryServices: Flow<List<Service>> = serviceDao.getHomeDiscoveryFeedServicesFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val allConversations: Flow<List<ChatConversation>> = _currentUserId.flatMapLatest { userId ->
        if (userId.isBlank()) kotlinx.coroutines.flow.flowOf(emptyList()) else conversationDao.getAllConversations(userId)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val unreadChatCount: Flow<Int> = _currentUserId.flatMapLatest { userId ->
        if (userId.isBlank()) {
            kotlinx.coroutines.flow.flowOf(0)
        } else {
            conversationMemberDao.getMembersByUser(userId).map { members ->
                members.sumOf { it.unreadCount.coerceAtLeast(0) }
            }
        }
    }

    private val _newChatMessageFlow = MutableSharedFlow<ChatMessage>()
    val newChatMessageFlow: SharedFlow<ChatMessage> = _newChatMessageFlow.asSharedFlow()

    fun getSharedPreferences(): SharedPreferences = sharedPrefs

    fun setCurrentUserId(userId: String) {
        sharedPrefs.edit().putString("current_user_id", userId).apply()
        _currentUserId.value = userId
        if (userId.isBlank()) {
            _hostIncomingOrders.value = emptyList()
            disconnectChatSocket()
        } else {
            connectChatSocket()
        }
    }

    suspend fun registerUser(user: User, password: String): Result<User> = withContext(Dispatchers.IO) {
        runCatching {
            val registered = apiService?.registerUser(
                RegisterRequest(
                    id = user.id,
                    password = password,
                    phoneNumber = user.phoneNumber,
                    name = user.name,
                    peiPeiId = user.peiPeiId,
                    photoUrl = user.photoUrl,
                    isProfileCompleted = user.isProfileCompleted,
                    createdAt = user.createdAt,
                    updatedAt = user.updatedAt
                )
            ) ?: error("ApiService is not available")
            val authenticated = loginUser(user.phoneNumber, password)
            authenticated ?: registered
        }
    }

    suspend fun loginUser(phoneNumber: String, password: String): User? = withContext(Dispatchers.IO) {
        val service = apiService ?: return@withContext null
        try {
            val token = service.loginForAccessToken(phoneNumber, password)
            AuthSession.updateAccessToken(token.accessToken)
            val user = service.getCurrentUserProfile()
            userDao.insertUser(user)
            setCurrentUserId(user.id)
            user
        } catch (e: Exception) {
            Log.e(TAG, "loginUser failed", e)
            null
        }
    }

    suspend fun syncAfterLogin(context: Context) {
        if (currentUserId.isNotBlank()) {
            refreshUserDataIfStale(currentUserId, force = true)
        }
    }

    suspend fun refreshHomeCriticalData(userId: String) {
        if (userId.isBlank()) return
        refreshMutex.withLock {
            supervisorScope {
                listOf(
                    launch { runRefreshStep("syncCurrentUser") { syncCurrentUser() } },
                    launch { runRefreshStep("syncFavoriteServices") { syncFavoriteServices() } },
                    launch {
                        runRefreshStep("fetchAndSyncDiscoveryServices") {
                            fetchAndSyncDiscoveryServices(limit = 50)
                        }
                    }
                ).joinAll()
            }
        }
    }

    /**
     * 仅同步首页发现流（写入 Room → [squareDiscoveryServices]），不拉当前用户资料、不拉心愿单收藏。
     * 用于首页下拉刷新等，避免连带刷新心愿单与「我的」资料。
     */
    suspend fun refreshHomeDiscoveryFeedOnly(userId: String) {
        refreshMutex.withLock {
            runRefreshStep("fetchAndSyncDiscoveryServices") {
                fetchAndSyncDiscoveryServices(limit = 50, forceFullWindowFetch = true)
            }
        }
    }

    /**
     * 预取发现流下一页（`skip` = 已累计条数），写入 Room 后首页 Flow 自动变长。
     * 与下拉刷新共用 [refreshMutex]，避免与首屏写入并发冲突。
     */
    suspend fun prefetchDiscoveryFeedNextPage(userId: String) {
        if (userId.isBlank()) return
        val api = apiService ?: return
        refreshMutex.withLock {
            withContext(Dispatchers.IO) {
                runCatching {
                    val skip = discoveryFeedPrefetchSkip.get()
                    if (skip >= DISCOVERY_PREFETCH_SKIP_CAP) return@withContext
                    val remote = api.getDiscoveryServices(
                        skip = skip,
                        limit = DISCOVERY_PREFETCH_PAGE_SIZE,
                        updatedAfter = null
                    )
                    if (remote.isEmpty()) return@withContext
                    val batch = remote.map { it.copy(isSynced = true).withNormalizedCategory() }
                    serviceDao.insertServices(batch)
                    discoveryFeedPrefetchSkip.addAndGet(remote.size)
                }.onFailure { e ->
                    Log.w(TAG, "prefetchDiscoveryFeedNextPage failed", e)
                }
            }
        }
    }

    /**
     * 底部「心愿单」Tab 下拉刷新：同步本人资料、心愿单 ID、我的服务与广场服务缓存；不同步会话列表。
     */
    suspend fun refreshWishlistTabData(userId: String) {
        if (userId.isBlank()) return
        refreshMutex.withLock {
            supervisorScope {
                listOf(
                    launch { runRefreshStep("syncCurrentUser") { syncCurrentUser() } },
                    launch { runRefreshStep("syncFavoriteServices") { syncFavoriteServices() } },
                    launch { runRefreshStep("fetchAndSyncServices") { fetchAndSyncServices() } },
                    launch {
                        runRefreshStep("fetchAndSyncDiscoveryServices") {
                            fetchAndSyncDiscoveryServices(limit = 200)
                        }
                    }
                ).joinAll()
            }
        }
    }

    /** 底部「消息」Tab 下拉刷新：仅重新拉取会话列表。 */
    suspend fun refreshMessagesTabData() {
        refreshMutex.withLock {
            runRefreshStep("fetchAndSyncConversations") { fetchAndSyncConversations() }
        }
    }

    /** 底部「我的」Tab 下拉刷新：仅同步当前用户资料。 */
    suspend fun refreshMyProfileTabData(userId: String) {
        if (userId.isBlank()) return
        refreshMutex.withLock {
            runRefreshStep("syncCurrentUser") { syncCurrentUser() }
        }
    }

    suspend fun refreshUserData(userId: String) {
        if (userId.isBlank()) return
        refreshMutex.withLock {
            supervisorScope {
                listOf(
                    launch { runRefreshStep("syncCurrentUser") { syncCurrentUser() } },
                    launch { runRefreshStep("syncDiscoveryUsers") { syncDiscoveryUsers() } },
                    launch { runRefreshStep("syncFavoriteServices") { syncFavoriteServices() } },
                    launch { runRefreshStep("fetchAndSyncServices") { fetchAndSyncServices() } },
                    launch {
                        runRefreshStep("fetchAndSyncDiscoveryServices") {
                            fetchAndSyncDiscoveryServices(limit = 200)
                        }
                    },
                    launch { runRefreshStep("fetchAndSyncConversations") { fetchAndSyncConversations() } }
                ).joinAll()
            }
        }
    }

    suspend fun refreshUserDataIfStale(
        userId: String,
        minIntervalMs: Long = DEFAULT_FULL_REFRESH_MIN_INTERVAL_MS,
        force: Boolean = false
    ): Boolean {
        if (userId.isBlank()) return false
        val now = System.currentTimeMillis()
        val key = buildFullRefreshTimestampKey(userId)
        val lastRefreshAt = sharedPrefs.getLong(key, 0L)
        val isStale = now - lastRefreshAt >= minIntervalMs
        if (!force && !isStale) return false
        refreshUserData(userId)
        sharedPrefs.edit().putLong(key, System.currentTimeMillis()).apply()
        return true
    }

    private fun buildFullRefreshTimestampKey(userId: String): String {
        return "$KEY_FULL_REFRESH_TS_PREFIX$userId"
    }

    private fun buildDiscoveryRefreshTimestampKey(userId: String): String {
        return "$KEY_DISCOVERY_REFRESH_TS_PREFIX$userId"
    }

    private suspend fun runRefreshStep(stepName: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: HttpException) {
            if (e.code() == 401) {
                Log.w(TAG, "$stepName unauthorized, skip this refresh step", e)
                return
            }
            Log.e(TAG, "$stepName failed with http ${e.code()}", e)
        } catch (e: IOException) {
            Log.w(TAG, "$stepName skipped due to network connectivity issue", e)
        } catch (e: Exception) {
            Log.e(TAG, "$stepName failed unexpectedly", e)
        }
    }

    private suspend fun syncCurrentUser() {
        val service = apiService ?: return
        val current = service.getCurrentUserProfile()
        userDao.insertUser(current)
    }

    private suspend fun syncFavoriteServices() {
        val service = apiService ?: return
        val userId = currentUserId
        if (userId.isBlank()) return
        val local = userDao.getUserByIdSuspend(userId) ?: return
        val remoteIds = runCatching { service.getMyFavoriteServices().serviceIds }.getOrElse { local.favoriteServiceIds }
        userDao.insertUser(local.copy(favoriteServiceIds = remoteIds, updatedAt = System.currentTimeMillis()))
    }

    suspend fun refreshWishlistProfile(): WishlistProfileState? = withContext(Dispatchers.IO) {
        val api = apiService ?: return@withContext null
        val userId = currentUserId
        if (userId.isBlank()) return@withContext null
        val local = userDao.getUserByIdSuspend(userId) ?: return@withContext null
        val payload = runCatching { api.getMyWishlistProfile() }.getOrNull() ?: return@withContext null
        persistWishlistProfileState(local, payload)
    }

    private suspend fun syncDiscoveryUsers() {
        val service = apiService ?: return
        val remoteUsers = runCatching { service.getDiscoveryUsers(limit = 20) }.getOrNull() ?: return
        remoteUsers.forEach { remote ->
            userDao.insertUser(remote)
        }
    }

    suspend fun addUser(user: User) {
        userDao.insertUser(user)
    }

    suspend fun updateServiceHostProfile(user: User) {
        userDao.insertUser(user)
        if (user.id == currentUserId) {
            syncCurrentUserProfile(user)
        }
    }

    suspend fun updateUserTags(userId: String, tags: List<String>) {
        val user = userDao.getUserByIdSuspend(userId) ?: return
        if (userId == currentUserId) {
            syncCurrentUserProfile(user.copy(tags = tags))
        } else {
            updateServiceHostProfile(user.copy(tags = tags))
        }
    }

    suspend fun deleteUser(userId: String) {
        withContext(Dispatchers.IO) {
            userDao.deleteUserById(userId)
        }
    }

    suspend fun syncCurrentUserProfile(updatedUser: User): Result<User> = withContext(Dispatchers.IO) {
        val token = AuthSession.getAccessToken()
        if (token.isNullOrBlank() || apiService == null) {
            // Demo/offline mode: persist locally to keep profile edits usable.
            userDao.insertUser(updatedUser)
            return@withContext Result.success(updatedUser)
        }
        runCatching {
            val saved = apiService.updateUser(updatedUser.id, updatedUser)
            userDao.insertUser(saved)
            saved
        }
    }

    suspend fun getUserByIdSuspend(userId: String): User? {
        var user = userDao.getUserByIdSuspend(userId)
        if (user == null) {
            user = runCatching {
                if (userId == currentUserId) apiService?.getCurrentUserProfile() else apiService?.getUser(userId)
            }.getOrNull()
            if (user != null) {
                userDao.insertUser(user)
            }
        }
        return user
    }

    suspend fun updateServiceHostRemark(userId: String, remark: String) {
        val user = userDao.getUserByIdSuspend(userId) ?: return
        updateServiceHostProfile(user.copy(remarkName = remark))
    }

    suspend fun toggleFavoriteService(serviceId: String): FavoriteToggleResult = withContext(Dispatchers.IO) {
        val userId = currentUserId
        if (userId.isBlank() || serviceId.isBlank()) {
            return@withContext FavoriteToggleResult(emptySet(), syncFailed = true, errorType = FavoriteSyncError.SERVER)
        }
        val user = userDao.getUserByIdSuspend(userId)
            ?: return@withContext FavoriteToggleResult(emptySet(), syncFailed = true, errorType = FavoriteSyncError.SERVER)
        val currentIds = user.favoriteServiceIds.toSet()
        val updatedIds = user.favoriteServiceIds.toMutableSet().apply {
            if (contains(serviceId)) remove(serviceId) else add(serviceId)
        }.toList()
        val api = apiService
            ?: return@withContext FavoriteToggleResult(currentIds, syncFailed = true, errorType = FavoriteSyncError.SERVER)
        val remoteResult = runCatching {
            api.updateMyFavoriteServices(FavoriteServicesPayload(updatedIds)).serviceIds
        }
        val remoteIds = remoteResult.getOrNull()
        if (remoteIds != null) {
            val remoteSet = remoteIds.toSet()
            userDao.insertUser(
                user.copy(
                    favoriteServiceIds = remoteSet.toList(),
                    updatedAt = System.currentTimeMillis()
                )
            )
            FavoriteToggleResult(remoteSet, syncFailed = false, errorType = null)
        } else {
            val throwable = remoteResult.exceptionOrNull()
            val errorType = when (throwable) {
                is java.io.IOException -> FavoriteSyncError.NETWORK
                is retrofit2.HttpException -> {
                    if (throwable.code() == 404) FavoriteSyncError.SERVICE_NOT_FOUND else FavoriteSyncError.SERVER
                }
                else -> FavoriteSyncError.SERVER
            }
            FavoriteToggleResult(currentIds, syncFailed = true, errorType = errorType)
        }
    }

    /** 仅添加收藏；已存在时直接返回当前列表，不发起「翻转」请求。 */
    suspend fun addFavoriteService(serviceId: String): FavoriteToggleResult = withContext(Dispatchers.IO) {
        val userId = currentUserId
        if (userId.isBlank() || serviceId.isBlank()) {
            return@withContext FavoriteToggleResult(emptySet(), syncFailed = true, errorType = FavoriteSyncError.SERVER)
        }
        val user = userDao.getUserByIdSuspend(userId)
            ?: return@withContext FavoriteToggleResult(emptySet(), syncFailed = true, errorType = FavoriteSyncError.SERVER)
        if (user.favoriteServiceIds.contains(serviceId)) {
            return@withContext FavoriteToggleResult(
                favoriteIds = user.favoriteServiceIds.toSet(),
                syncFailed = false,
                errorType = null
            )
        }
        val updatedIds = (user.favoriteServiceIds + serviceId).toList()
        val api = apiService
        if (api == null) {
            return@withContext FavoriteToggleResult(
                favoriteIds = user.favoriteServiceIds.toSet(),
                syncFailed = true,
                errorType = FavoriteSyncError.SERVER
            )
        }
        val remoteResult = runCatching {
            api.updateMyFavoriteServices(FavoriteServicesPayload(updatedIds)).serviceIds
        }
        val remoteIds = remoteResult.getOrNull()
        if (remoteIds != null) {
            val remoteSet = remoteIds.toSet()
            userDao.insertUser(
                user.copy(
                    favoriteServiceIds = remoteSet.toList(),
                    updatedAt = System.currentTimeMillis()
                )
            )
            FavoriteToggleResult(remoteSet, syncFailed = false, errorType = null)
        } else {
            val throwable = remoteResult.exceptionOrNull()
            val errorType = when (throwable) {
                is java.io.IOException -> FavoriteSyncError.NETWORK
                is retrofit2.HttpException -> {
                    if (throwable.code() == 404) FavoriteSyncError.SERVICE_NOT_FOUND else FavoriteSyncError.SERVER
                }
                else -> FavoriteSyncError.SERVER
            }
            FavoriteToggleResult(
                favoriteIds = user.favoriteServiceIds.toSet(),
                syncFailed = true,
                errorType = errorType
            )
        }
    }

    suspend fun updateWishlistProfile(
        favoriteIds: Set<String>,
        groups: List<String>,
        favoriteServiceGroups: Map<String, String>
    ): WishlistProfileState? = withContext(Dispatchers.IO) {
        val api = apiService ?: return@withContext null
        val userId = currentUserId
        if (userId.isBlank()) return@withContext null
        val local = userDao.getUserByIdSuspend(userId) ?: return@withContext null
        val payload = favoriteRemoteSyncMutex.withLock {
            runCatching {
                api.updateMyWishlistProfile(
                    buildWishlistProfilePayload(
                        favoriteIds = favoriteIds,
                        groups = groups,
                        favoriteServiceGroups = favoriteServiceGroups
                    )
                )
            }.getOrNull()
        } ?: return@withContext null
        persistWishlistProfileState(local, payload)
    }

    suspend fun uploadAvatar(file: File): String? = withContext(Dispatchers.IO) {
        val token = AuthSession.getAccessToken()
        if (token.isNullOrBlank()) {
            // No auth token available (e.g. local demo user), keep avatar usable locally.
            return@withContext file.toURI().toString()
        }
        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
        val userIdPart = currentUserId
            .takeIf { it.isNotBlank() }
            ?.toRequestBody("text/plain".toMediaTypeOrNull())
        runCatching { apiService?.uploadAvatar(body, userIdPart)?.url?.trim() }
            .onFailure { Log.e(TAG, "uploadAvatar failed", it) }
            .getOrNull()
    }

    suspend fun uploadServiceFile(file: File): ChatUploadResponse? = withContext(Dispatchers.IO) {
        val requestFile = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
        runCatching { apiService?.uploadServiceFile(body) }.getOrNull()
    }

    suspend fun createService(service: Service): Service? = withContext(Dispatchers.IO) {
        val local = service.copy(creatorId = currentUserId, isSynced = false).withNormalizedCategory()
        serviceDao.insertService(local)
        runCatching {
            val created = apiService?.createService(local.copy(isSynced = true))
            if (created != null) {
                serviceDao.insertService(created.copy(isSynced = true).withNormalizedCategory())
                created.withNormalizedCategory()
            } else {
                local
            }
        }.getOrElse {
            Log.e(TAG, "createService failed", it)
            local
        }
    }

    suspend fun updateService(service: Service): Service? = withContext(Dispatchers.IO) {
        val local = service.copy(creatorId = currentUserId, isSynced = false).withNormalizedCategory()
        serviceDao.updateService(local)
        runCatching {
            val updated = apiService?.updateService(service.id, local.copy(isSynced = true))
            if (updated != null) {
                serviceDao.updateService(updated.copy(isSynced = true).withNormalizedCategory())
                updated.withNormalizedCategory()
            } else {
                local
            }
        }.getOrElse {
            Log.e(TAG, "updateService failed", it)
            local
        }
    }

    suspend fun deleteService(serviceId: String) {
        withContext(Dispatchers.IO) {
            val existing = serviceDao.getServiceByIdSync(serviceId) ?: return@withContext
            serviceDao.updateService(existing.copy(isDeleted = true, updatedAt = System.currentTimeMillis()))
            try {
                apiService?.deleteService(serviceId)
            } catch (e: Exception) {
                Log.e(TAG, "deleteService failed", e)
            }
        }
    }

    /** 从「我发布的」中永久移除本地记录（草稿 / 已取消）；不调用后端硬删接口。 */
    suspend fun purgeMyCreatedServiceRecord(serviceId: String) {
        withContext(Dispatchers.IO) {
            val uid = currentUserId
            if (uid.isBlank()) return@withContext
            val existing = serviceDao.getServiceByIdSync(serviceId) ?: return@withContext
            if (existing.creatorId != uid) return@withContext
            serviceDao.deleteServiceById(serviceId)
        }
    }

    suspend fun fetchAndSyncServices() {
        val userId = currentUserId
        if (userId.isBlank()) return
        withContext(Dispatchers.IO) {
            try {
                val remote = apiService?.getMyServices() ?: emptyList()
                val local = serviceDao.getServices(userId).associateBy { it.id }
                remote.forEach { serviceDao.insertService(it.copy(isSynced = true).withNormalizedCategory()) }
                local.keys.filter { id -> remote.none { it.id == id } }.forEach { serviceDao.deleteServiceById(it) }
            } catch (e: Exception) {
                Log.e(TAG, "fetchAndSyncServices failed", e)
            }
        }
    }

    private suspend fun fetchAndSyncDiscoveryServices(
        limit: Int = 200,
        forceFullWindowFetch: Boolean = false
    ) {
        val api = apiService ?: return
        val userId = currentUserId
        withContext(Dispatchers.IO) {
            runCatching {
                if (forceFullWindowFetch) {
                    discoveryFeedPrefetchSkip.set(0)
                }
                val key = buildDiscoveryRefreshTimestampKey(userId)
                val lastSyncAt = sharedPrefs.getLong(key, 0L)
                val hasLocalDiscovery = serviceDao.hasAnyNonDeletedService()
                val updatedAfter = when {
                    forceFullWindowFetch -> null
                    hasLocalDiscovery && lastSyncAt > 0L -> lastSyncAt
                    else -> null
                }
                // #region debug-point B:discovery-request-start
                reportAndroidHomeEmptyDebug(
                    hypothesisId = "B",
                    location = "LuluRepository.fetchAndSyncDiscoveryServices:start",
                    msg = "Discovery request started",
                    data = mapOf(
                        "apiBaseUrl" to BuildConfig.API_BASE_URL,
                        "limit" to limit,
                        "forceFullWindowFetch" to forceFullWindowFetch,
                        "updatedAfter" to updatedAfter,
                        "hasLocalDiscovery" to hasLocalDiscovery,
                        "currentUserIdBlank" to userId.isBlank()
                    )
                )
                // #endregion
                val remote = api.getDiscoveryServices(skip = 0, limit = limit, updatedAfter = updatedAfter)
                // #region debug-point C:discovery-response
                reportAndroidHomeEmptyDebug(
                    hypothesisId = "C",
                    location = "LuluRepository.fetchAndSyncDiscoveryServices:response",
                    msg = "Discovery response received",
                    data = mapOf(
                        "remoteCount" to remote.size,
                        "firstId" to remote.firstOrNull()?.id,
                        "firstCategory" to remote.firstOrNull()?.category
                    )
                )
                // #endregion
                if (BuildConfig.DEBUG) {
                    Log.d(
                        TAG,
                        "discovery fetch ok: count=${remote.size} limit=$limit forceFull=$forceFullWindowFetch " +
                            "updatedAfter=$updatedAfter hasLocal=$hasLocalDiscovery api=${BuildConfig.API_BASE_URL}"
                    )
                }
                if (remote.isNotEmpty()) {
                    val batch = remote.map { it.copy(isSynced = true).withNormalizedCategory() }
                    serviceDao.insertServices(batch)
                    // #region debug-point D:discovery-room-after-insert
                    reportAndroidHomeEmptyDebug(
                        hypothesisId = "D",
                        location = "LuluRepository.fetchAndSyncDiscoveryServices:room",
                        msg = "Discovery services inserted into Room",
                        data = mapOf(
                            "insertedCount" to batch.size,
                            "squareDiscoveryCount" to serviceDao.getSquareDiscoveryServices().size
                        )
                    )
                    // #endregion
                } else if (updatedAfter == null) {
                    // 全量窗口仍为空：多为后端无数据、BASE_URL 不可达（此前已 IOException）或路径错误
                    Log.w(
                        TAG,
                        "discovery API returned 0 rows (full window). Check backend /services/discovery " +
                            "and BuildConfig.API_BASE_URL=${BuildConfig.API_BASE_URL}"
                    )
                }
                if (forceFullWindowFetch) {
                    discoveryFeedPrefetchSkip.set(remote.size)
                }
                if (remote.isNotEmpty()) {
                    val newestUpdatedAt = remote.maxOf { it.updatedAt }
                    sharedPrefs.edit().putLong(key, newestUpdatedAt).apply()
                } else if (!hasLocalDiscovery) {
                    sharedPrefs.edit().putLong(key, System.currentTimeMillis()).apply()
                }
            }.onFailure { e ->
                // #region debug-point E:discovery-request-failed
                reportAndroidHomeEmptyDebug(
                    hypothesisId = "E",
                    location = "LuluRepository.fetchAndSyncDiscoveryServices:failure",
                    msg = "Discovery request failed",
                    data = mapOf(
                        "errorType" to e::class.java.simpleName,
                        "errorMessage" to (e.message ?: "")
                    )
                )
                // #endregion
                Log.e(
                    TAG,
                    "fetchAndSyncDiscoveryServices failed (check network & API_BASE_URL=${BuildConfig.API_BASE_URL})",
                    e
                )
            }
        }
    }

    suspend fun getAllServicesList(): List<Service> {
        val userId = currentUserId
        if (userId.isBlank()) return serviceDao.getSquareDiscoveryServices()
        val mine = serviceDao.getServices(userId)
        val square = serviceDao.getSquareDiscoveryServices()
        return (mine + square).distinctBy { it.id }
            .sortedWith(
                compareByDescending<Service> { it.updatedAt }
                    .thenByDescending { it.createdAt }
            )
    }

    fun getServiceById(id: String): Flow<Service?> {
        scope.launch {
            getServiceByIdSync(id)
        }
        return serviceDao.getServiceById(id)
    }

    suspend fun getServiceByIdSync(id: String): Service? {
        var service = serviceDao.getServiceByIdSync(id)
        if (service == null) {
            service = runCatching {
                apiService?.getServiceById(id)
            }.getOrNull()
            if (service != null) {
                serviceDao.insertService(service.copy(isSynced = true).withNormalizedCategory())
            }
        }
        return service
    }

    suspend fun fetchAndSyncConversations() {
        val userId = currentUserId
        if (userId.isBlank()) return
        withContext(Dispatchers.IO) {
            try {
                val items = apiService?.getChatConversations() ?: emptyList()
                conversationDao.deleteByUser(userId)
                conversationMemberDao.deleteByUser(userId)
                items.forEach { item ->
                    conversationDao.insertConversation(item.conversation.copy(currentUserId = userId))
                    item.member?.let { conversationMemberDao.insertMember(it.copy(currentUserId = userId)) }
                    item.peer?.let { peer ->
                        userDao.insertUser(peer)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "fetchAndSyncConversations failed", e)
            }
        }
    }

    suspend fun createDirectConversation(peerId: String): ChatConversation? = withContext(Dispatchers.IO) {
        try {
            val item = apiService?.createDirectConversation(peerId) ?: return@withContext null
            val userId = currentUserId
            conversationDao.insertConversation(item.conversation.copy(currentUserId = userId))
            item.member?.let { conversationMemberDao.insertMember(it.copy(currentUserId = userId)) }
            item.peer?.let { peer ->
                userDao.insertUser(peer)
            }
            item.conversation.copy(currentUserId = userId)
        } catch (e: Exception) {
            Log.e(TAG, "createDirectConversation failed", e)
            null
        }
    }

    suspend fun getOrCreateDirectConversation(peerId: String): ChatConversation? = withContext(Dispatchers.IO) {
        val userId = currentUserId
        if (userId.isBlank() || peerId.isBlank()) {
            return@withContext null
        }

        val existing = allConversations.first().firstOrNull { conversation ->
            conversation.type == "direct" &&
                conversation.participantIds.contains(userId) &&
                conversation.participantIds.contains(peerId)
        }
        if (existing != null) {
            return@withContext existing
        }

        val createdByRemote = createDirectConversation(peerId)
        if (createdByRemote != null) {
            return@withContext createdByRemote
        }

        val now = System.currentTimeMillis()
        val localConversation = ChatConversation(
            id = "local-${UUID.randomUUID()}",
            type = "direct",
            participantIds = listOf(userId, peerId),
            createdAt = now,
            updatedAt = now,
            currentUserId = userId
        )
        conversationDao.insertConversation(localConversation)
        conversationMemberDao.insertMember(
            ChatConversationMember(
                id = "${localConversation.id}:$userId",
                conversationId = localConversation.id,
                userId = userId,
                unreadCount = 0,
                joinedAt = now,
                updatedAt = now,
                currentUserId = userId
            )
        )
        localConversation
    }

    fun getConversationById(conversationId: String): Flow<ChatConversation?> {
        return conversationDao.getConversationById(conversationId, currentUserId)
    }

    fun getMessagesFlow(conversationId: String): Flow<List<ChatMessage>> {
        return chatMessageDao.getMessages(conversationId, currentUserId)
    }

    suspend fun getLatestMessagePreview(conversationId: String): String {
        val userId = currentUserId
        if (userId.isBlank() || conversationId.isBlank()) return ""
        val message = chatMessageDao.getLatestMessageByConversation(conversationId, userId) ?: return ""
        return when (message.type) {
            "image" -> "[图片]"
            "file" -> message.attachmentName.ifBlank { "[文件]" }
            else -> message.content.ifBlank { "收到一条消息" }
        }
    }

    suspend fun getFirstMessageSenderId(conversationId: String): String {
        val userId = currentUserId
        if (userId.isBlank() || conversationId.isBlank()) return ""
        return chatMessageDao.getFirstMessageByConversation(conversationId, userId)?.senderId.orEmpty()
    }

    suspend fun fetchAndSyncMessages(conversationId: String, before: Long? = null) {
        val userId = currentUserId
        if (userId.isBlank()) return
        withContext(Dispatchers.IO) {
            try {
                val items = apiService?.getChatMessages(conversationId, before = before) ?: emptyList()
                if (before == null) {
                    chatMessageDao.deleteMessagesByConversation(conversationId, userId)
                }
                chatMessageDao.insertMessages(items.map { it.copy(currentUserId = userId) })
            } catch (e: Exception) {
                Log.e(TAG, "fetchAndSyncMessages failed", e)
            }
        }
    }

    suspend fun sendChatMessage(
        conversationId: String,
        content: String,
        type: String = "text",
        attachmentUrl: String = "",
        attachmentName: String = "",
        attachmentSize: Long = 0L,
        objectKey: String = "",
        extra: Map<String, String> = emptyMap()
    ): ChatMessage? = withContext(Dispatchers.IO) {
        try {
            val request = ChatMessageCreateRequest(
                type = type,
                content = content,
                attachmentUrl = attachmentUrl,
                attachmentName = attachmentName,
                attachmentSize = attachmentSize,
                objectKey = objectKey,
                extra = extra
            )
            val message = apiService?.sendChatMessage(
                conversationId = conversationId,
                body = request
            ) ?: buildLocalFallbackMessage(
                conversationId = conversationId,
                content = content,
                type = type,
                attachmentUrl = attachmentUrl,
                attachmentName = attachmentName,
                attachmentSize = attachmentSize,
                objectKey = objectKey,
                extra = extra
            )
            chatMessageDao.insertMessage(message.copy(currentUserId = currentUserId))
            fetchAndSyncConversations()
            message
        } catch (e: Exception) {
            Log.e(TAG, "sendChatMessage failed", e)
            val fallback = buildLocalFallbackMessage(
                conversationId = conversationId,
                content = content,
                type = type,
                attachmentUrl = attachmentUrl,
                attachmentName = attachmentName,
                attachmentSize = attachmentSize,
                objectKey = objectKey,
                extra = extra
            )
            chatMessageDao.insertMessage(fallback.copy(currentUserId = currentUserId))
            fallback
        }
    }

    private fun buildLocalFallbackMessage(
        conversationId: String,
        content: String,
        type: String,
        attachmentUrl: String,
        attachmentName: String,
        attachmentSize: Long,
        objectKey: String,
        extra: Map<String, String>
    ): ChatMessage {
        val now = System.currentTimeMillis()
        return ChatMessage(
            id = "local-${UUID.randomUUID()}",
            conversationId = conversationId,
            senderId = currentUserId,
            type = type,
            content = content,
            attachmentUrl = attachmentUrl,
            attachmentName = attachmentName,
            attachmentSize = attachmentSize,
            objectKey = objectKey,
            extra = extra,
            clientMessageId = "local-$now",
            status = "sent",
            createdAt = now,
            updatedAt = now,
            isDeleted = false,
            currentUserId = currentUserId
        )
    }

    suspend fun markConversationRead(conversationId: String) {
        withContext(Dispatchers.IO) {
            try {
                val member = apiService?.markConversationRead(conversationId, ChatReadRequest()) ?: return@withContext
                conversationMemberDao.insertMember(member.copy(currentUserId = currentUserId))
            } catch (e: Exception) {
                Log.e(TAG, "markConversationRead failed", e)
            }
        }
    }

    suspend fun uploadChatFile(file: File): ChatUploadResponse? = withContext(Dispatchers.IO) {
        val requestFile = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
        runCatching { apiService?.uploadChatFile(body) }.getOrNull()
    }

    suspend fun logout() {
        val userId = currentUserId
        AuthSession.updateAccessToken(null)
        disconnectChatSocket()
        setCurrentUserId("")
        withContext(Dispatchers.IO) {
            serviceDao.deleteAllServices()
            conversationDao.deleteByUser(userId)
            conversationMemberDao.deleteByUser(userId)
            chatMessageDao.deleteByUser(userId)
            userDao.deleteAllUsers()
        }
    }

    private fun normalizeWishlistGroupName(groupName: String?): String {
        val trimmed = groupName?.trim().orEmpty()
        return if (trimmed.isBlank()) DEFAULT_WISHLIST_GROUP else trimmed
    }

    private fun buildWishlistProfileState(payload: WishlistProfilePayload): WishlistProfileState {
        val favoriteIds = payload.serviceIds.filter { it.isNotBlank() }.distinct()
        val favoriteIdSet = favoriteIds.toSet()
        val groups = mutableListOf<String>()
        val mapping = linkedMapOf<String, String>()

        payload.groups.forEach { group ->
            val normalizedName = normalizeWishlistGroupName(group.name)
            if (groups.none { it.equals(normalizedName, ignoreCase = true) }) {
                groups += normalizedName
            }
            group.serviceIds.forEach { serviceId ->
                if (serviceId in favoriteIdSet && mapping.containsKey(serviceId).not()) {
                    mapping[serviceId] = normalizedName
                }
            }
        }

        if (groups.none { it == DEFAULT_WISHLIST_GROUP }) {
            groups.add(0, DEFAULT_WISHLIST_GROUP)
        }
        favoriteIds.forEach { serviceId ->
            if (mapping.containsKey(serviceId).not()) {
                mapping[serviceId] = DEFAULT_WISHLIST_GROUP
            }
        }
        return WishlistProfileState(
            favoriteIds = favoriteIdSet,
            groups = groups.distinct(),
            favoriteServiceGroups = mapping
        )
    }

    private fun buildWishlistProfilePayload(
        favoriteIds: Set<String>,
        groups: List<String>,
        favoriteServiceGroups: Map<String, String>
    ): WishlistProfilePayload {
        val normalizedFavoriteIds = favoriteIds.filter { it.isNotBlank() }.distinct()
        val orderedGroups = buildList {
            add(DEFAULT_WISHLIST_GROUP)
            groups.forEach { group ->
                val normalizedName = normalizeWishlistGroupName(group)
                if (contains(normalizedName).not()) {
                    add(normalizedName)
                }
            }
        }
        val idsByGroup = linkedMapOf<String, MutableList<String>>()
        orderedGroups.forEach { idsByGroup[it] = mutableListOf() }

        normalizedFavoriteIds.forEach { serviceId ->
            val targetGroup = normalizeWishlistGroupName(favoriteServiceGroups[serviceId])
            val ensuredGroup = if (idsByGroup.containsKey(targetGroup)) targetGroup else DEFAULT_WISHLIST_GROUP
            idsByGroup.getValue(ensuredGroup).add(serviceId)
        }

        val payloadGroups = orderedGroups.map { groupName ->
            WishlistGroupPayload(
                name = groupName,
                serviceIds = idsByGroup[groupName].orEmpty().distinct()
            )
        }
        return WishlistProfilePayload(
            serviceIds = normalizedFavoriteIds,
            groups = payloadGroups
        )
    }

    private suspend fun persistWishlistProfileState(
        localUser: User,
        payload: WishlistProfilePayload
    ): WishlistProfileState {
        val state = buildWishlistProfileState(payload)
        userDao.insertUser(
            localUser.copy(
                favoriteServiceIds = state.favoriteIds.toList(),
                updatedAt = System.currentTimeMillis()
            )
        )
        return state
    }


    private fun connectChatSocket() {
        if (!chatSocketConnected && AuthSession.getAccessToken() != null) {
            chatSocketClient.connect()
        }
    }

    private fun disconnectChatSocket() {
        chatSocketClient.disconnect()
    }

    private suspend fun handleChatSocketEvent(text: String) {
        val event = runCatching { gson.fromJson(text, ChatSocketEvent::class.java) }.getOrNull() ?: return
        when (event.type) {
            "chat_message" -> {
                val message = event.message ?: return
                chatMessageDao.insertMessage(message.copy(currentUserId = currentUserId))
                fetchAndSyncConversations()
                _newChatMessageFlow.emit(message)
            }
            "conversation_read" -> {
                fetchAndSyncConversations()
            }
        }
    }

    private fun Service.withNormalizedCategory(): Service =
        copy(
            category = ServiceCategories.normalize(category),
            bookingLeadHours = BookingTimeRangesCodec.normalizeBookingLeadHours(bookingLeadHours),
            bookingFutureOpenDays = BookingTimeRangesCodec.normalizeBookingFutureOpenDays(bookingFutureOpenDays),
            prepaymentPercent = prepaymentPercent.coerceIn(0, 100),
            fullRefundCancelLeadDays = fullRefundCancelLeadDays.coerceIn(0, 10),
            serviceDeclarationsExtra = ServiceDeclarations.normalizeExtra(serviceDeclarationsExtra),
        )

    companion object {
        private const val TAG = "LuluRepository"
        private const val DEFAULT_WISHLIST_GROUP = "默认分组"
        /** 首页注入的假服务条数（类目轮询，每类至少 floor(100/11)=9 条） */

        private const val KEY_FULL_REFRESH_TS_PREFIX = "full_refresh_ts_"
        private const val KEY_DISCOVERY_REFRESH_TS_PREFIX = "discovery_refresh_ts_"
        private const val DEFAULT_FULL_REFRESH_MIN_INTERVAL_MS = 90_000L
        /** 首页滚动预取：单页条数与累计 skip 上限，避免无限请求与单页过大 */
        private const val DISCOVERY_PREFETCH_PAGE_SIZE = 40
        private const val DISCOVERY_PREFETCH_SKIP_CAP = 2500


        @Volatile
        private var INSTANCE: LuluRepository? = null

        fun getInstance(
            userDao: UserDao,
            serviceDao: ServiceDao,
            conversationDao: ConversationDao,
            conversationMemberDao: ConversationMemberDao,
            chatMessageDao: ChatMessageDao,
            sharedPrefs: SharedPreferences,
            context: Context,
            apiService: ApiService? = null
        ): LuluRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = LuluRepository(
                    userDao = userDao,
                    serviceDao = serviceDao,
                    conversationDao = conversationDao,
                    conversationMemberDao = conversationMemberDao,
                    chatMessageDao = chatMessageDao,
                    sharedPrefs = sharedPrefs,
                    context = context,
                    apiService = apiService
                )
                INSTANCE = instance
                instance
            }
        }

        fun get(): LuluRepository {
            return INSTANCE ?: error("Repository not initialized")
        }
    }
}
