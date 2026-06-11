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
import com.example.Lulu.util.BookingTimeRangesCodec
import com.google.gson.Gson
import java.io.File
import java.io.IOException
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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshMutex = Mutex()
    /**
     * 发现流「读ahead」游标：与首屏 `skip=0` 拉取衔接，用于滚动到底时预取下一页写入 Room。
     * 大厂信息流常见策略：首屏快显 + 提前拉下一屏，减少用户滑到底时的等待（仍受 [refreshMutex] 与网络约束）。
     */
    private val discoveryFeedPrefetchSkip = AtomicInteger(0)
    /** 心愿单远程 PUT 串行化，避免批量加入等场景下多个协程并发覆盖。 */
    private val favoriteRemoteSyncMutex = Mutex()

    private val _wishlistRemoteSyncFailures = MutableSharedFlow<String>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    /** 后台同步心愿单列表失败时发出文案，供界面 Snackbar 提示（本地收藏仍保留）。 */
    val wishlistRemoteSyncFailures: SharedFlow<String> = _wishlistRemoteSyncFailures.asSharedFlow()

    private fun wishlistSyncFailureUserMessage(throwable: Throwable?): String {
        return when (val t = throwable) {
            is IOException -> "网络异常，心愿单已保存在本机，联网后将自动同步"
            is HttpException -> when (t.code()) {
                404 -> "服务不存在或已下架，心愿单暂保存在本机"
                else -> "心愿单同步失败，已保存在本机，请稍后重试"
            }
            null -> "心愿单同步失败，已保存在本机，请稍后重试"
            else -> "心愿单同步失败，已保存在本机，请稍后重试"
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
            val seedPattern = homeFeedSeedIdPattern(userId)
            combine(
                serviceDao.getServicesFlow(userId),
                serviceDao.getSquareDiscoveryServicesFlow(),
                serviceDao.getHomeSeedInjectedServicesFlow(seedPattern)
            ) { mine, square, homeSeeds ->
                (mine + square + homeSeeds).distinctBy { it.id }
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
            ensureHomeFeedSeedDataIfEmptyFor(currentUserId)
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
        // 与广场 API 并行：若当前账号尚无首页种子，则写入三亚演示数据（见 [initTestData]）。
        ensureHomeFeedSeedDataIfEmptyFor(userId)
    }

    /**
     * 若本地尚无 `seed-home-{userId}-*` 服务，则执行 [initTestData]（需 Room 内已有该用户）。
     * [userId] 须与 [currentUserId] 一致，避免误为其他账号写种子。
     */
    private suspend fun ensureHomeFeedSeedDataIfEmptyFor(userId: String) {
        if (userId.isBlank()) {
            if (BuildConfig.DEBUG) Log.d(TAG, "ensureHomeFeedSeed: skip blank userId")
            return
        }
        if (userId != currentUserId) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "ensureHomeFeedSeed: skip userId mismatch param=$userId current=$currentUserId")
            }
            return
        }
        withContext(Dispatchers.IO) {
            if (userDao.getUserByIdSuspend(userId) == null) {
                if (BuildConfig.DEBUG) Log.d(TAG, "ensureHomeFeedSeed: no Room user row for $userId")
                return@withContext
            }
            val pattern = homeFeedSeedIdPattern(userId)
            val existing = serviceDao.countNonDeletedServicesByIdLike(pattern)
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "ensureHomeFeedSeed: pattern=$pattern existingCount=$existing")
            }
            if (existing == 0) {
                initTestData()
            }
        }
    }

    /** 供首页在冷启动竞态后补写：当前账号无首页种子时注入三亚演示数据。 */
    suspend fun ensureHomeFeedSeedDataIfEmpty() {
        ensureHomeFeedSeedDataIfEmptyFor(currentUserId)
    }

    /**
     * 仅同步首页发现流（写入 Room → [squareDiscoveryServices]），不拉当前用户资料、不拉心愿单收藏。
     * 用于首页下拉刷新等，避免连带刷新心愿单与「我的」资料。
     */
    suspend fun refreshHomeDiscoveryFeedOnly(userId: String) {
        if (userId.isBlank()) return
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
        val updatedIds = user.favoriteServiceIds.toMutableSet().apply {
            if (contains(serviceId)) remove(serviceId) else add(serviceId)
        }.toList()
        val localUpdated = user.copy(
            favoriteServiceIds = updatedIds,
            updatedAt = System.currentTimeMillis()
        )
        userDao.insertUser(localUpdated)
        val remoteResult = runCatching {
            apiService?.updateMyFavoriteServices(FavoriteServicesPayload(updatedIds))?.serviceIds ?: updatedIds
        }
        val remoteIds = remoteResult.getOrNull()
        if (remoteIds != null) {
            val remoteSet = remoteIds.toSet()
            userDao.insertUser(localUpdated.copy(favoriteServiceIds = remoteSet.toList()))
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
            FavoriteToggleResult(updatedIds.toSet(), syncFailed = true, errorType = errorType)
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
        val localUpdated = user.copy(
            favoriteServiceIds = updatedIds,
            updatedAt = System.currentTimeMillis()
        )
        userDao.insertUser(localUpdated)
        val api = apiService
        if (api == null) {
            return@withContext FavoriteToggleResult(updatedIds.toSet(), syncFailed = false, errorType = null)
        }
        // 先落库再立即返回，避免「保存到心愿单」弹窗长时间卡在「正在加入…」等待网络。
        scope.launch {
            val failureMessage = favoriteRemoteSyncMutex.withLock {
                val latestForSync = userDao.getUserByIdSuspend(userId) ?: return@withLock null
                val idsToSync = latestForSync.favoriteServiceIds
                val remoteResult = runCatching {
                    api.updateMyFavoriteServices(FavoriteServicesPayload(idsToSync)).serviceIds
                }
                val remoteIds = remoteResult.getOrNull()
                if (remoteIds != null) {
                    val remoteSet = remoteIds.toSet()
                    val latest = userDao.getUserByIdSuspend(userId) ?: return@withLock null
                    userDao.insertUser(
                        latest.copy(
                            favoriteServiceIds = remoteSet.toList(),
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                    null
                } else {
                    val ex = remoteResult.exceptionOrNull()
                    ex?.let { Log.w(TAG, "addFavoriteService: remote sync failed, keeping local wishlist", it) }
                    wishlistSyncFailureUserMessage(ex)
                }
            }
            if (failureMessage != null) {
                _wishlistRemoteSyncFailures.emit(failureMessage)
            }
        }
        FavoriteToggleResult(updatedIds.toSet(), syncFailed = false, errorType = null)
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
        if (userId.isBlank()) return
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
                val remote = api.getDiscoveryServices(skip = 0, limit = limit, updatedAfter = updatedAfter)
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
        val homeSeeds = serviceDao.getHomeSeedInjectedServices(homeFeedSeedIdPattern(userId))
        return (mine + square + homeSeeds).distinctBy { it.id }
            .sortedWith(
                compareByDescending<Service> { it.updatedAt }
                    .thenByDescending { it.createdAt }
            )
    }

    fun getServiceById(id: String): Flow<Service?> = serviceDao.getServiceById(id)

    suspend fun getServiceByIdSync(id: String): Service? = serviceDao.getServiceByIdSync(id)

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

    /**
     * 注入首页 `seed-home-{userId}-*` 演示服务：**共 100 条**，地点均为三亚；
     * 类目按 [ServiceCategories.PRESETS] 顺序轮询分配（每类至少 9 条，满足「每类至少 2 条」）；
     * 标题在 6–30 字之间按类目轮换。会先删本地同前缀服务及 `seed-service-host-{userId}-*` 假主理人再写入。
     * 需在已登录（[currentUserId] 非空）且 Room 内存在当前用户时调用，例如调试菜单或一次性迁移脚本。
     */
    suspend fun initTestData() {
        val userId = currentUserId
        if (userId.isBlank()) return
        userDao.getUserByIdSuspend(userId) ?: return
        val now = System.currentTimeMillis()
        val seedPrefix = "seed-home-$userId-"
        val serviceHostIdPrefix = "seed-service-host-$userId-"
        val service = apiService
        val drawablePhotoUris = (1..15).map { index ->
            "android.resource://${context.packageName}/drawable/service_photo_${index.toString().padStart(2, '0')}"
        }
        fun servicePhotoUri(slot1Based: Int): String {
            val n = ((slot1Based - 1) % 15 + 15) % 15 + 1
            return drawablePhotoUris[n - 1]
        }

        if (BuildConfig.DEBUG) {
            homeFeedSeedTitlePools.values.flatten().forEach { t ->
                require(t.length in 6..30) { "种子标题须 6–30 字，当前 len=${t.length}：$t" }
            }
        }

        // 先清理同前缀旧数据（服务 + 假主理人），保证每次重建为稳定批量种子。
        runCatching {
            service?.getMyServices()
                ?.filter { it.id.startsWith(seedPrefix) }
                ?.forEach { service.deleteService(it.id) }
        }

        data class SeedServiceHostProfile(
            val name: String,
            val region: String,
            val signature: String
        )

        /** 单主理人：100 条服务全部为三亚，类目轮询覆盖。 */
        val seedServiceHostProfiles = listOf(
            SeedServiceHostProfile("三亚集样", "海南省三亚市", "演示账号：首页种子 100 条，地点均为三亚")
        )

        val userIdAlphanum = userId.filter { it.isLetterOrDigit() }.ifEmpty { "user" }
        val birthDecadePool = listOf("85后", "90后", "95后", "00后", "05后", "10后")
        val middleSchoolSongs = listOf("晴天", "七里香", "后来", "遇见", "小幸运", "起风了", "海阔天空", "稻香")
        val seedServiceHosts = seedServiceHostProfiles.mapIndexed { index, profile ->
            val serial = index + 1
            val serviceHostId = "$serviceHostIdPrefix${serial.toString().padStart(2, '0')}"
            val avatar = drawablePhotoUris[index % drawablePhotoUris.size]
            User(
                id = serviceHostId,
                name = profile.name,
                peiPeiId = "d${userIdAlphanum.take(8)}${serial.toString().padStart(2, '0')}",
                photoUrl = avatar,
                region = profile.region,
                signature = profile.signature,
                birthDecade = birthDecadePool[index % birthDecadePool.size],
                heightCm = 158 + (index % 22),
                weightKg = 47f + (index % 18),
                heightWeightPrivate = index % 6 == 0,
                middleSchoolFavoriteSong = middleSchoolSongs[index % middleSchoolSongs.size],
                serviceYears = (index % 5) + 1,
                averageRating = 4.2 + (index % 8) * 0.05,
                reviewCount = 12 + index * 3,
                createdAt = now - (seedServiceHostProfiles.size - serial) * 86_400_000L,
                updatedAt = now
            )
        }

        data class SeedTemplate(
            val title: String,
            val description: String,
            val location: String,
            val priceText: String,
            val priceBasisText: String,
            val serviceMode: String,
            val syncToSquare: Boolean,
            val category: String,
            /** 与标题/类目对应的 `service_photo_XX` 序号（1–15），首项为封面，其余为图集 */
            val imageSlots: List<Int>
        )

        val baseSeedTemplates = listOf(
            SeedTemplate(
                title = "三亚一日轻动线陪游｜亚龙湾海滩+海棠湾免税错峰",
                description = "【适合谁】第一次来三亚、想一天把「海+逛」串起来又不想赶路。\n【你会得到】约8小时弹性动线：海滩踩水时段、阴凉休息与补水点穿插；cdf 或小镇逛街错峰建议（不含代购）。\n【服务细节】行前对齐忌口/步速/是否带娃；附当日简易清单（打车点、防晒与更衣提醒）。\n【温馨提示】门票、项目与餐饮自理；台风雷雨以景区开放为准。",
                location = "海南省三亚市",
                priceText = "¥588/天",
                priceBasisText = "每天（默认晚8点前结束）· 超时按小时另议 · 跨湾交通自理",
                serviceMode = "同城陪同",
                syncToSquare = true,
                category = ServiceCategories.LOCAL_GUIDE,
                imageSlots = listOf(1, 2, 3)
            ),
            SeedTemplate(
                title = "椰梦长廊日落氛围跟拍｜剪影·轻胶片·晚霞层次",
                description = "【适合谁】情侣、闺蜜、独自旅行——想发一条「三亚日落是真的」的朋友圈。\n【你会得到】黄金半小时踩点 + 逆光/剪影构图；可选轻复古胶片色调。\n【交付】精修12张 + 调色底片，椰林与晚霞层次保留。\n【温馨提示】日落时间随季节变化大，会提前一天确认集合时间；阴天改情绪风或改期一次。",
                location = "海南省三亚市",
                priceText = "¥520/场",
                priceBasisText = "单场覆盖日落前后时段 · 精修张数可按套餐加购",
                serviceMode = "同城拍摄",
                syncToSquare = true,
                category = ServiceCategories.PHOTO,
                imageSlots = listOf(4, 5, 6)
            ),
            SeedTemplate(
                title = "海棠湾海边派对 DJ｜House·Pop 稳接歌·音量友好",
                description = "【适合谁】生日 After Party、求婚派对、酒店露台小活动——要嗨但不吵邻房。\n【你会得到】提前歌单沟通（House/流行/复古），现场接歌与能量起伏控制；照顾聊天区音量。\n【服务细节】可与简单灯效节奏配合；「第一首歌」卡点可彩排。\n【温馨提示】音响与电源以场地为准；加时与设备升级另报价。",
                location = "海南省三亚市",
                priceText = "¥1580/场起",
                priceBasisText = "每场起价4小时档 · 加时与设备租赁另议",
                serviceMode = "上门演出",
                syncToSquare = true,
                category = ServiceCategories.DJ,
                imageSlots = listOf(7, 8)
            ),
            SeedTemplate(
                title = "亚龙湾别墅轰趴气氛组｜破冰游戏·惩罚环节不尬",
                description = "【适合谁】公司团建尾牙、老同学局、带娃家庭局——怕冷场、怕尬聊。\n【你会得到】签到破冰 → 分组小游戏 → 奖惩环节主持，可按人数与泳池/草坪场地调强度。\n【服务细节】内向朋友有「低压力」参与位；可配合背景音乐起伏。\n【温馨提示】道具与场地安全请主办方确认；建议最低预订2小时。",
                location = "海南省三亚市",
                priceText = "¥128/小时起",
                priceBasisText = "按小时起 · 建议≥2小时 · 大型团建另报打包价",
                serviceMode = "现场带玩",
                syncToSquare = true,
                category = ServiceCategories.ATMOSPHERE,
                imageSlots = listOf(9, 10, 11)
            ),
            SeedTemplate(
                title = "三亚湾酒店上门精油推拿｜肩颈背「松绑」60分钟",
                description = "【适合谁】度假仍久坐刷手机、飞行后肩颈「厚硬酸」想快速松一口气。\n【你会得到】60分钟肩颈上背重点放松：斜方肌、肩胛内侧分段处理。\n【服务细节】力度实时沟通；结束后给3分钟拉伸示范。\n【温馨提示】急性损伤或医嘱不宜按摩者请先咨询医生；孕妇请提前说明。",
                location = "海南省三亚市",
                priceText = "¥268/60分钟",
                priceBasisText = "单次60分钟 · 酒店客房为主 · 上门时段以预约为准",
                serviceMode = "理疗放松",
                syncToSquare = true,
                category = ServiceCategories.MASSAGE,
                imageSlots = listOf(12, 13)
            ),
            SeedTemplate(
                title = "海棠湾酒店健身房塑形私教｜体态评估·度假也不断训",
                description = "【适合谁】住店想保持训练、怕动作不标准或器械不会用。\n【你会得到】1v1体态与关节活动度快速评估；当堂训练+动作纠错；附一周徒手/弹力带巩固表。\n【服务细节】会看呼吸与核心募集；强度按作息与饮酒情况微调。\n【温馨提示】酒店健身房次卡或房客权益自理；心血管等慢性病史请课前说明。",
                location = "海南省三亚市",
                priceText = "¥399/节",
                priceBasisText = "每节约60分钟 · 场地费/次卡自理 · 首节含评估",
                serviceMode = "线下私教",
                syncToSquare = true,
                category = ServiceCategories.FITNESS_COACH,
                imageSlots = listOf(14, 15, 1)
            ),
            SeedTemplate(
                title = "三亚别墅/公寓上门私厨｜4–6人琼味海鲜家宴",
                description = "【适合谁】家庭小聚、纪念日——想在家吃出餐厅感又不想盯灶。\n【你会得到】菜单共创（清蒸/姜葱/椒盐等口味对齐），采购清单与预算区间；现场烹饪、摆盘与厨房基础收尾。\n【服务细节】提前48小时确认人数与忌口；可记录上菜顺序方便以后复刻。\n【温馨提示】食材费实报实销或自行采购二选一；贵重酒具请自备。",
                location = "海南省三亚市",
                priceText = "¥988/次",
                priceBasisText = "单次4–6人桌 · 含现场烹饪 · 食材费另计",
                serviceMode = "上门服务",
                syncToSquare = true,
                category = ServiceCategories.PRIVATE_CHEF,
                imageSlots = listOf(2, 3, 4)
            ),
            SeedTemplate(
                title = "旅拍度假妆发一体｜扛汗底妆·海边持久定妆",
                description = "【适合谁】约了摄影师、要下水前拍照，或晚宴前想「干净高级」不出油。\n【你会得到】妆发一体：底妆服帖 + 眉眼妆简化版，加强防脱汗与定妆细节。\n【服务细节】可带参考图对齐风格；附粉底/口红色号记录方便自购。\n【温馨提示】敏感肌请自带底妆；上门交通与停车费另议。",
                location = "海南省三亚市",
                priceText = "¥368/次",
                priceBasisText = "单次妆发一体 · 时长随造型复杂度浮动",
                serviceMode = "妆发造型",
                syncToSquare = true,
                category = ServiceCategories.MAKEUP,
                imageSlots = listOf(5, 6)
            ),
            SeedTemplate(
                title = "三亚 GL8 包车+游艇半日｜机场酒店点对点·出海安全简报",
                description = "【适合谁】家庭/小团想少折腾：落地接机、换酒店、半天出海一条线。\n【你会得到】别克 GL8 或同级包车动线建议；游艇时段含安全简报与晕船小贴士（具体船型以预约确认为准）。\n【服务细节】取还时间与行李空间提前对齐；儿童座椅需求请说明。\n【温馨提示】油费/泊位费/船长小费等以合同与码头规则为准；恶劣天气改期政策提前预约时确认。",
                location = "海南省三亚市",
                priceText = "¥1680/半天起",
                priceBasisText = "包车半天档起 · 游艇时段与船型另列报价 · 押金与保险以码头为准",
                serviceMode = "预约用车",
                syncToSquare = true,
                category = ServiceCategories.CAR_YACHT_RENTAL,
                imageSlots = listOf(7, 8, 9)
            ),
            SeedTemplate(
                title = "cdf 三亚免税动线陪同｜额度提货·楼层少折返",
                description = "【适合谁】第一次逛 cdf、品牌多楼层大、怕买完发现提错货或时间不够。\n【你会得到】额度与提货方式（离岛/邮寄）白话说明；热门品牌楼层动线与「先逛后买」顺序表。\n【服务细节】理性比价提醒；高峰排队策略。\n【温馨提示】不含代购、垫付与优惠券代办；购物决策仍以你本人为准。",
                location = "海南省三亚市",
                priceText = "¥228/3小时",
                priceBasisText = "任意连续3小时 · 跨店折返时间计入",
                serviceMode = "同城陪同",
                syncToSquare = true,
                category = ServiceCategories.OTHER,
                imageSlots = listOf(13, 14, 15)
            )
        )
        require(baseSeedTemplates.size == ServiceCategories.PRESETS.size) {
            "首页种子模板须与 PRESETS 一一对应，当前 ${baseSeedTemplates.size} 条，预设 ${ServiceCategories.PRESETS.size} 类"
        }
        val presetCount = ServiceCategories.PRESETS.size
        val seedTemplates = List(HOME_FEED_SEED_TOTAL) { i ->
            val catIdx = i % presetCount
            val category = ServiceCategories.PRESETS[catIdx]
            val variantOrdinal = i / presetCount
            baseSeedTemplates[catIdx].copy(
                title = homeFeedSeedTitle(category, variantOrdinal)
            )
        }
        val seedCount = seedTemplates.size
        val soleCreatorId = seedServiceHosts.first().id
        val creatorIdByTemplateIndex = List(seedCount) { soleCreatorId }
        val serviceHostById = seedServiceHosts.associateBy { it.id }
        val mockServices = seedTemplates.mapIndexed { index, template ->
            val serial = index + 1
            val createdAt = now - (seedCount - serial) * 60_000L
            val updatedAt = createdAt + (serial % 5) * 1_000L
            val slots = template.imageSlots.ifEmpty { listOf((index % 15) + 1) }
            val galleryImages = slots.distinct().map { servicePhotoUri(it) }
            val coverImage = galleryImages.first()
            val creatorServiceHostId = creatorIdByTemplateIndex[index]
            val creatorUser = serviceHostById.getValue(creatorServiceHostId)
            Service(
                id = "$seedPrefix${serial.toString().padStart(3, '0')}",
                title = template.title,
                description = template.description,
                coverImageUrl = coverImage,
                imageUrls = galleryImages,
                location = template.location,
                priceText = template.priceText,
                priceBasisText = template.priceBasisText,
                category = ServiceCategories.normalize(template.category),
                serviceMode = template.serviceMode,
                syncToSquare = template.syncToSquare,
                participantIds = emptyList(),
                creator = creatorUser.name,
                creatorId = creatorServiceHostId,
                isImportant = serial % 4 == 0,
                createdAt = createdAt,
                updatedAt = updatedAt,
                isDeleted = false,
                isSynced = false
            )
        }

        withContext(Dispatchers.IO) {
            serviceDao.deleteServicesByHomeSeedPattern(homeFeedSeedIdPattern(userId))
            userDao.deleteUsersByIdPrefix(serviceHostIdPrefix)
            userDao.insertUsers(seedServiceHosts)
            // 先批量写入本地，避免因远端串行请求慢导致首页卡片逐张延迟出现。
            mockServices.forEach { item ->
                serviceDao.insertService(item.copy(isSynced = false))
            }
        }
        // 远端同步改为后台执行，不阻塞首屏列表展示。
        scope.launch {
            mockServices.forEach { item ->
                val remote = runCatching { service?.createService(item.copy(isSynced = true)) }.getOrNull()
                if (remote != null) {
                    serviceDao.insertService(remote.copy(isSynced = true).withNormalizedCategory())
                }
            }
        }
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
        /** 首页注入的假服务条数（类目轮询，每类至少 floor(100/11)=9 条） */
        private const val HOME_FEED_SEED_TOTAL = 100

        /**
         * 每类目 11 条备选标题（6–30 字），按轮次 [variantOrdinal] 取用；
         * 与 [initTestData] 中 `i / PRESETS.size` 对齐，100 条时 variantOrdinal 最大为 9。
         */
        private val homeFeedSeedTitlePools: Map<String, List<String>> = mapOf(
            ServiceCategories.LOCAL_GUIDE to listOf(
                "三亚陪游半日",
                "三亚湾轻走半日陪",
                "亚龙湾亲子陪游轻松一日动线",
                "海棠湾免税错峰陪同讲解半日",
                "蜈支洲登岛接送集合动线陪同",
                "后海慢逛咖啡小店陪同二小时",
                "西岛渔村老街椰子鸡陪同探店",
                "南山寺礼佛节奏陪同讲解一日",
                "夜游觅食第一市场少排队陪同",
                "五天四晚家庭游全程陪同策划",
                "亚特兰蒂斯水世界错峰陪同半日含寄存与集合点讲解三小时档"
            ),
            ServiceCategories.PHOTO to listOf(
                "三亚跟拍快闪",
                "椰梦长廊日落跟拍一场",
                "三亚领证海边草坪跟拍半日",
                "海棠湾求婚酒店露台跟拍",
                "亲子泳池派对跟拍一小时",
                "亚龙湾比基尼度假风跟拍",
                "沙滩大合影年会团建跟拍半天",
                "夜市霓虹人像街拍跟拍快闪",
                "电商服装三亚外景寄修图跟拍",
                "婚礼接亲走廊快剪双机位跟拍",
                "海底餐厅玻璃舱约会纪念照跟拍精修返图含调色底片全送当场选片版"
            ),
            ServiceCategories.DJ to listOf(
                "三亚派对DJ一小时",
                "海边露台生日派对DJ四小时",
                "别墅轰趴House音量友好DJ",
                "三亚酒店年会宴会厅DJ控场",
                "草坪婚礼后场派对DJ套餐",
                "露台求婚派对接歌DJ两小时",
                "民宿泳池派对打碟DJ到场升级",
                "沙滩复古迪斯科DJ夕阳快闪",
                "亲子生日儿歌配乐穿插DJ温柔版",
                "海棠湾多边活动巡回扩声DJ全天",
                "五星酒店海边户外晚宴电音渐进DJ全场十小时含彩排走场对接"
            ),
            ServiceCategories.ATMOSPHERE to listOf(
                "三亚气氛组到场",
                "别墅破冰游戏气氛组三小时",
                "三亚团建桌游主持带气氛",
                "生日派对惩罚游戏主持不尬",
                "年会抽奖气氛组上台带动",
                "沙滩亲子游戏带队气氛两小时",
                "KTV包厢暖场点歌气氛组",
                "毕业旅行海边喊话送祝福气氛",
                "后备箱惊喜求婚递花气氛走位",
                "亚龙湾泳池人浪气氛组喊口号",
                "公司周年庆三亚户外拉练口号列队倒计时彩粉气氛组全天流程协助"
            ),
            ServiceCategories.MASSAGE to listOf(
                "三亚上门推拿体验",
                "三亚湾酒店精油肩颈推拿",
                "亚龙湾泰式拉伸上门六十分钟",
                "海棠湾客房足底按摩放松",
                "中式推拿一小时酒店客房",
                "Spa后肩胛深按放松加钟",
                "运动后腿臀乳酸上门推拿",
                "偏头痛太阳穴轻柔指压推拿",
                "腰背筋膜枪配合推拿八十分钟",
                "久坐办公族肩颈腰背套餐上门",
                "三亚湾全海景房露台精油推拿三小时含热敷颈椎重点加强肩井穴"
            ),
            ServiceCategories.FITNESS_COACH to listOf(
                "三亚酒店私教一节",
                "酒店健身房减脂私教一节",
                "亚龙湾晨跑陪跑配速提醒",
                "体态圆肩驼背纠正私教体验",
                "弹力带全身塑形客房私教",
                "核心强化节前突击私教",
                "游泳换气纠正酒店泳池教学",
                "潜水后肩周拉伸放松三十分钟",
                "垫上普拉提呼吸入门私教",
                "备婚线条雕刻私教周卡七天",
                "亲子体能闯关游戏化训练带孩子放电两小时草坪沙滩场选其一"
            ),
            ServiceCategories.PRIVATE_CHEF to listOf(
                "三亚私厨上门一桌",
                "三亚四人琼味家宴上门私厨",
                "别墅泳池烧烤私厨摆盘",
                "海鲜市场代买现场加工一桌",
                "轻食减脂周卡上门备餐",
                "生日茶歇冷餐台私厨一站式",
                "椰子鸡火锅上门炉具自带",
                "三十人自助餐上门保温撤场",
                "宝宝辅食少盐少糖单独小锅",
                "情人节双人道式法餐上门布置",
                "术后清淡一周食谱上门烹饪营养师短信随访七次含买菜小票整理"
            ),
            ServiceCategories.MAKEUP to listOf(
                "三亚旅拍快妆补妆",
                "旅拍扛汗底妆三亚跟妆",
                "新娘早妆一次含假睫毛",
                "晚宴烟熏妆发型一体上门",
                "领证登记处淡妆快手三十分钟",
                "音乐节亮片妆发全包海边场",
                "男士修眉遮瑕上镜妆证件照",
                "六一儿童舞台妆上门一小时",
                "汉服外景妆发两小时跟拍兼顾",
                "下水前防水睫毛快妆二十分",
                "新郎伴郎造型上门哑光定型过敏测妆前三小时整档"
            ),
            ServiceCategories.CAR_YACHT_RENTAL to listOf(
                "三亚包车点对点",
                "别克GL8三亚机场接送机",
                "游艇出海半天含安全简报",
                "敞篷沿海公路试驾陪同",
                "商务七座跨湾一日包车讲解",
                "埃尔法婚礼头车半天巡游",
                "电动冲浪板码头皮卡接送",
                "帆船体验晕船贴准备陪同半日",
                "中巴机场往返剧组酒店待命",
                "亲子婴儿座椅跨湾包车八小时",
                "三亚GL8全天包车含油路桥票儿童椅机场酒店各一次等超时另计"
            ),
            ServiceCategories.OTHER to listOf(
                "三亚跑腿代办半日",
                "cdf额度提货陪同三小时讲解",
                "三亚医院陪诊取号半天",
                "宠物寄养对接机场接送",
                "搬家小件打包陪同半日",
                "代缴水电拍照回传跑腿",
                "打印装订资料送酒店前台",
                "网红餐厅代排队取号一小时",
                "教老人手机打车健康码二小时",
                "办公用品代购整理报销袋封面备注齐全一次性交付跑腿拍照",
                "外地业主开窗通风拍照除湿巡查一周七次钥匙托管三亚湾区"
            )
        )

        private fun homeFeedSeedTitle(category: String, variantOrdinal: Int): String {
            val key = ServiceCategories.normalize(category)
            val pool = homeFeedSeedTitlePools[key]
                ?: homeFeedSeedTitlePools.getValue(ServiceCategories.OTHER)
            return pool[variantOrdinal % pool.size]
        }

        private const val KEY_FULL_REFRESH_TS_PREFIX = "full_refresh_ts_"
        private const val KEY_DISCOVERY_REFRESH_TS_PREFIX = "discovery_refresh_ts_"
        private const val DEFAULT_FULL_REFRESH_MIN_INTERVAL_MS = 90_000L
        /** 首页滚动预取：单页条数与累计 skip 上限，避免无限请求与单页过大 */
        private const val DISCOVERY_PREFETCH_PAGE_SIZE = 40
        private const val DISCOVERY_PREFETCH_SKIP_CAP = 2500

        /** 与 [initTestData] 注入的服务 id 前缀一致，供首页/心愿单等聚合本人服务与同账号下假数据卡片 */
        fun homeFeedSeedIdPattern(viewerUserId: String): String = "seed-home-$viewerUserId-%"

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
