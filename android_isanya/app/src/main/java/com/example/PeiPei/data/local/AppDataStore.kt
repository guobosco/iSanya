// 文件说明：本地模拟/种子数据存储，用于开发或离线演示数据。

package com.example.Lulu.data.local

/**
 * 本地内存数据门面。
 * 负责桥接 UI 与主仓库，提供当前用户、联系人、服务列表等状态流及常用写操作。
 */

import android.util.Log
import com.example.Lulu.data.model.HostCalendarDayClosure
import com.example.Lulu.data.model.HostServiceBooking
import com.example.Lulu.data.model.Service
import com.example.Lulu.data.model.ServiceCategories
import com.example.Lulu.data.model.ServiceDeclarations
import com.example.Lulu.data.model.User
import com.example.Lulu.data.repository.LuluRepository
import com.example.Lulu.util.BookingTimeRangesCodec
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

object AppDataStore {
    const val DEFAULT_WISHLIST_GROUP = "默认分组"
    private const val WISHLIST_GROUPS_KEY = "wishlist_groups"
    private const val WISHLIST_GROUP_MAPPING_KEY = "wishlist_group_mapping"

    private var repository: LuluRepository? = null

    fun getRepository(): LuluRepository? = repository

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e("AppDataStore", "Coroutine exception", throwable)
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main + exceptionHandler)

    private val _currentUser = MutableStateFlow(
        User(
            id = "",
            name = "",
            region = "",
            signature = "",
            gender = ""
        )
    )
    val currentUser: StateFlow<User> = _currentUser.asStateFlow()

    private val _contacts = MutableStateFlow<List<User>>(emptyList())
    val contacts: StateFlow<List<User>> = _contacts.asStateFlow()

    private val _services = MutableStateFlow<List<Service>>(emptyList())
    val services: StateFlow<List<Service>> = _services.asStateFlow()
    private val _favoriteServiceIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteServiceIds: StateFlow<Set<String>> = _favoriteServiceIds.asStateFlow()
    private val _wishlistGroups = MutableStateFlow(listOf(DEFAULT_WISHLIST_GROUP))
    val wishlistGroups: StateFlow<List<String>> = _wishlistGroups.asStateFlow()
    private val _favoriteServiceGroups = MutableStateFlow<Map<String, String>>(emptyMap())
    val favoriteServiceGroups: StateFlow<Map<String, String>> = _favoriteServiceGroups.asStateFlow()

    val tags: StateFlow<List<String>> = _currentUser.map { it.tags }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    private val _tempUsers = mutableMapOf<String, User>()

    /** 发布者日历：按服务 ID 分组的本地演示订单（无后端时用于 UI）。 */
    private val _hostServiceBookings = MutableStateFlow<Map<String, List<HostServiceBooking>>>(emptyMap())
    val hostServiceBookings: StateFlow<Map<String, List<HostServiceBooking>>> = _hostServiceBookings.asStateFlow()

    fun hostBookingByOrderId(orderId: String): HostServiceBooking? =
        _hostServiceBookings.value.values.asSequence().flatten().firstOrNull { it.orderId == orderId }

    fun ensureDemoHostBookingsIfEmpty(serviceId: String) {
        // Removed fake data generation.
    }

    /** 发布者日历：按服务、按日的「不可订」规则（本地）。 */
    private val _hostCalendarDayClosures =
        MutableStateFlow<Map<String, Map<String, HostCalendarDayClosure>>>(emptyMap())
    val hostCalendarDayClosures: StateFlow<Map<String, Map<String, HostCalendarDayClosure>>> =
        _hostCalendarDayClosures.asStateFlow()

    fun setHostCalendarDayClosure(serviceId: String, dateIso: String, closure: HostCalendarDayClosure?) {
        if (serviceId.isBlank() || dateIso.isBlank()) return
        val outer = _hostCalendarDayClosures.value.toMutableMap()
        val inner = outer[serviceId].orEmpty().toMutableMap()
        if (closure == null) {
            inner.remove(dateIso)
        } else {
            inner[dateIso] = closure
        }
        if (inner.isEmpty()) {
            outer.remove(serviceId)
        } else {
            outer[serviceId] = inner
        }
        _hostCalendarDayClosures.value = outer
    }

    fun initialize(repo: LuluRepository) {
        repository = repo
        loadWishlistGroupSettings()

        scope.launch {
            repo.currentUser.collect { user ->
                if (user != null) {
                    _currentUser.value = user
                    _favoriteServiceIds.value = user.favoriteServiceIds.toSet()
                    cleanupFavoriteGroupMappingForCurrentFavorites()
                }
            }
        }
        scope.launch {
            repo.allUsers.collect { users ->
                _contacts.value = users
            }
        }
        scope.launch {
            repo.allServices.collect { list ->
                _services.value = list
            }
        }
    }

    fun addService(
        title: String,
        note: String,
        participantIds: List<String>,
        isImportant: Boolean = false,
        location: String = "",
        priceText: String = "",
        priceBasisText: String = "",
        category: String = ServiceCategories.DEFAULT,
        serviceMode: String = "",
        coverImageUrl: String = "",
        imageUrls: List<String> = emptyList(),
        syncToSquare: Boolean = false,
        bookingTimeRangesJson: String = "",
        bookingLeadHours: Float = BookingTimeRangesCodec.DEFAULT_BOOKING_LEAD_HOURS,
        bookingFutureOpenDays: Int = BookingTimeRangesCodec.DEFAULT_BOOKING_FUTURE_OPEN_DAYS,
        prepaymentPercent: Int = 30,
        fullRefundCancelLeadDays: Int = 1,
        autoAcceptAfterPayment: Boolean = true,
        serviceDeclarationsExtra: List<String> = emptyList(),
        isDraft: Boolean = false,
    ): Service {
        val item = Service(
            id = UUID.randomUUID().toString(),
            title = title,
            description = note,
            coverImageUrl = coverImageUrl,
            imageUrls = imageUrls,
            location = location,
            priceText = priceText,
            priceBasisText = priceBasisText,
            prepaymentPercent = prepaymentPercent.coerceIn(0, 100),
            fullRefundCancelLeadDays = fullRefundCancelLeadDays.coerceIn(0, 10),
            category = ServiceCategories.normalize(category),
            serviceMode = serviceMode,
            bookingTimeRangesJson = bookingTimeRangesJson,
            bookingLeadHours = BookingTimeRangesCodec.normalizeBookingLeadHours(bookingLeadHours),
            bookingFutureOpenDays = BookingTimeRangesCodec.normalizeBookingFutureOpenDays(bookingFutureOpenDays),
            autoAcceptAfterPayment = autoAcceptAfterPayment,
            serviceDeclarationsExtra = ServiceDeclarations.normalizeExtra(serviceDeclarationsExtra),
            syncToSquare = syncToSquare,
            creator = _currentUser.value.name,
            creatorId = _currentUser.value.id,
            participantIds = participantIds,
            isImportant = isImportant,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            isSynced = false,
            isDraft = isDraft,
        )
        scope.launch { repository?.createService(item) }
        return item
    }

    fun updateService(
        id: String,
        title: String,
        note: String,
        participantIds: List<String>,
        isImportant: Boolean = false,
        location: String = "",
        priceText: String = "",
        priceBasisText: String = "",
        category: String = ServiceCategories.DEFAULT,
        serviceMode: String = "",
        coverImageUrl: String = "",
        imageUrls: List<String> = emptyList(),
        syncToSquare: Boolean = false,
        bookingTimeRangesJson: String = "",
        bookingLeadHours: Float = BookingTimeRangesCodec.DEFAULT_BOOKING_LEAD_HOURS,
        bookingFutureOpenDays: Int? = null,
        prepaymentPercent: Int = 30,
        fullRefundCancelLeadDays: Int = 1,
        autoAcceptAfterPayment: Boolean = true,
        /** null 表示保留原有补充声明 */
        serviceDeclarationsExtra: List<String>? = null,
        /** null 表示保留原草稿/已发布标记 */
        isDraft: Boolean? = null,
    ) {
        scope.launch {
            val existing = getServiceById(id) ?: return@launch
            val resolvedExtra = serviceDeclarationsExtra?.let { ServiceDeclarations.normalizeExtra(it) }
                ?: existing.serviceDeclarationsExtra
            val updated = existing.copy(
                title = title,
                description = note,
                coverImageUrl = coverImageUrl,
                imageUrls = imageUrls,
                location = location,
                priceText = priceText,
                priceBasisText = priceBasisText,
                prepaymentPercent = prepaymentPercent.coerceIn(0, 100),
                fullRefundCancelLeadDays = fullRefundCancelLeadDays.coerceIn(0, 10),
                category = ServiceCategories.normalize(category),
                serviceMode = serviceMode,
                bookingTimeRangesJson = bookingTimeRangesJson,
                bookingLeadHours = BookingTimeRangesCodec.normalizeBookingLeadHours(bookingLeadHours),
                bookingFutureOpenDays = bookingFutureOpenDays?.let { BookingTimeRangesCodec.normalizeBookingFutureOpenDays(it) }
                    ?: existing.bookingFutureOpenDays,
                autoAcceptAfterPayment = autoAcceptAfterPayment,
                serviceDeclarationsExtra = resolvedExtra,
                syncToSquare = syncToSquare,
                participantIds = participantIds,
                isImportant = isImportant,
                updatedAt = System.currentTimeMillis(),
                isSynced = false,
                isDraft = isDraft ?: existing.isDraft,
            )
            repository?.updateService(updated)
        }
    }

    fun deleteService(id: String) {
        scope.launch { repository?.deleteService(id) }
    }

    /** 已取消（下架）条目重新上架为已发布。 */
    fun republishService(id: String) {
        scope.launch {
            val existing = getServiceById(id) ?: return@launch
            if (!existing.isDeleted) return@launch
            repository?.updateService(
                existing.copy(
                    isDeleted = false,
                    isDraft = false,
                    updatedAt = System.currentTimeMillis(),
                    isSynced = false,
                )
            )
        }
    }

    /** 从本地数据库删除该条服务记录（草稿或已取消）。 */
    fun purgeCreatedServiceRecord(id: String) {
        scope.launch { repository?.purgeMyCreatedServiceRecord(id) }
    }

    fun toggleServiceImportant(id: String) {
        scope.launch {
            val existing = getServiceById(id) ?: return@launch
            repository?.updateService(
                existing.copy(
                    isImportant = !existing.isImportant,
                    updatedAt = System.currentTimeMillis(),
                    isSynced = false
                )
            )
        }
    }

    suspend fun toggleFavoriteService(id: String): LuluRepository.FavoriteToggleResult {
        val result = repository?.toggleFavoriteService(id)
            ?: LuluRepository.FavoriteToggleResult(
                favoriteIds = _favoriteServiceIds.value,
                syncFailed = true,
                errorType = LuluRepository.FavoriteSyncError.SERVER
            )
        _favoriteServiceIds.value = result.favoriteIds
        if (result.favoriteIds.contains(id)) {
            val existingGroup = _favoriteServiceGroups.value[id]
            val normalizedGroup = normalizeGroupName(existingGroup ?: DEFAULT_WISHLIST_GROUP)
            _favoriteServiceGroups.value = _favoriteServiceGroups.value + (id to normalizedGroup)
        } else {
            _favoriteServiceGroups.value = _favoriteServiceGroups.value - id
        }
        cleanupFavoriteGroupMappingForCurrentFavorites()
        persistWishlistGroupSettings()
        return result
    }

    suspend fun addFavoriteService(id: String): LuluRepository.FavoriteToggleResult {
        val result = repository?.addFavoriteService(id)
            ?: LuluRepository.FavoriteToggleResult(
                favoriteIds = _favoriteServiceIds.value,
                syncFailed = true,
                errorType = LuluRepository.FavoriteSyncError.SERVER
            )
        _favoriteServiceIds.value = result.favoriteIds
        if (result.favoriteIds.contains(id)) {
            val existingGroup = _favoriteServiceGroups.value[id]
            val normalizedGroup = normalizeGroupName(existingGroup ?: DEFAULT_WISHLIST_GROUP)
            _favoriteServiceGroups.value = _favoriteServiceGroups.value + (id to normalizedGroup)
        } else {
            _favoriteServiceGroups.value = _favoriteServiceGroups.value - id
        }
        cleanupFavoriteGroupMappingForCurrentFavorites()
        persistWishlistGroupSettings()
        return result
    }

    suspend fun addFavoriteServiceToGroup(
        serviceId: String,
        groupName: String
    ): LuluRepository.FavoriteToggleResult {
        val normalizedGroup = normalizeGroupName(groupName)
        ensureGroupExists(normalizedGroup)
        val isAlreadyFavorite = _favoriteServiceIds.value.contains(serviceId)
        val result = if (isAlreadyFavorite) {
            LuluRepository.FavoriteToggleResult(
                favoriteIds = _favoriteServiceIds.value,
                syncFailed = false,
                errorType = null
            )
        } else {
            addFavoriteService(serviceId)
        }
        if (result.favoriteIds.contains(serviceId)) {
            _favoriteServiceGroups.value = _favoriteServiceGroups.value + (serviceId to normalizedGroup)
        } else {
            _favoriteServiceGroups.value = _favoriteServiceGroups.value - serviceId
        }
        cleanupFavoriteGroupMappingForCurrentFavorites()
        persistWishlistGroupSettings()
        return result
    }

    fun getFavoriteGroupForService(serviceId: String): String {
        return normalizeGroupName(_favoriteServiceGroups.value[serviceId])
    }

    fun createWishlistGroup(groupName: String): Boolean {
        val normalized = normalizeGroupName(groupName)
        if (_wishlistGroups.value.any { it.equals(normalized, ignoreCase = true) }) {
            return false
        }
        _wishlistGroups.value = (_wishlistGroups.value + normalized).distinct()
        persistWishlistGroupSettings()
        return true
    }

    fun renameWishlistGroup(oldName: String, newName: String): Boolean {
        val oldNormalized = normalizeGroupName(oldName)
        val newNormalized = normalizeGroupName(newName)
        if (oldNormalized == DEFAULT_WISHLIST_GROUP) return false
        if (_wishlistGroups.value.none { it == oldNormalized }) return false
        if (_wishlistGroups.value.any { it.equals(newNormalized, ignoreCase = true) && it != oldNormalized }) {
            return false
        }
        _wishlistGroups.value = _wishlistGroups.value.map { if (it == oldNormalized) newNormalized else it }
        _favoriteServiceGroups.value = _favoriteServiceGroups.value.mapValues { (_, value) ->
            if (value == oldNormalized) newNormalized else value
        }
        persistWishlistGroupSettings()
        return true
    }

    fun deleteWishlistGroup(groupName: String): Boolean {
        val normalized = normalizeGroupName(groupName)
        if (normalized == DEFAULT_WISHLIST_GROUP) return false
        if (_wishlistGroups.value.none { it == normalized }) return false
        _wishlistGroups.value = _wishlistGroups.value.filter { it != normalized }
        _favoriteServiceGroups.value = _favoriteServiceGroups.value.mapValues { (_, value) ->
            if (value == normalized) DEFAULT_WISHLIST_GROUP else value
        }
        persistWishlistGroupSettings()
        return true
    }

    /**
     * 解析任意本人服务（含草稿/已下架）。内存 [_services] 仅含广场合并后的「在架」子集，
     * 与「我发布的」列表数据源不一致时，回退到 Room。
     */
    suspend fun getServiceById(id: String): Service? =
        _services.value.find { it.id == id } ?: repository?.getServiceByIdSync(id)

    suspend fun getAllServicesList(): List<Service> = repository?.getAllServicesList() ?: emptyList()

    suspend fun reloadServicesFromDatabase() {
        _services.value = repository?.getAllServicesList() ?: emptyList()
    }

    fun getServiceFlow(id: String): Flow<Service?> {
        return repository?.getServiceById(id) ?: _services.map { list -> list.find { it.id == id } }
    }

    fun getUserById(id: String): User? {
        if (_currentUser.value.id == id) return _currentUser.value
        return _contacts.value.find { it.id == id } ?: _tempUsers[id]
    }

    suspend fun getUserByIdSuspend(id: String): User? {
        return getUserById(id) ?: repository?.getUserByIdSuspend(id)?.also { _tempUsers[id] = it }
    }

    fun updateServiceHostRemark(userId: String, remark: String) {
        scope.launch { repository?.updateServiceHostRemark(userId, remark) }
    }

    fun addTag(tag: String) {
        val normalized = tag.trim()
        if (normalized.isBlank()) return
        val user = _currentUser.value
        if (user.tags.none { it.equals(normalized, ignoreCase = true) }) {
            val updated = user.copy(tags = user.tags + normalized)
            _currentUser.value = updated
            scope.launch {
                repository?.addUser(updated)
                repository?.updateUserTags(updated.id, updated.tags)
            }
        }
    }

    fun deleteTag(tag: String) {
        val user = _currentUser.value
        if (!user.tags.contains(tag)) return
        val updated = user.copy(tags = user.tags - tag)
        _currentUser.value = updated
        scope.launch {
            repository?.addUser(updated)
            repository?.updateUserTags(updated.id, updated.tags)
        }
        _contacts.value.filter { it.tags.contains(tag) }.forEach { contact ->
            updateServiceHost(contact.copy(tags = contact.tags - tag))
        }
    }

    fun updateTag(oldTag: String, newTag: String) {
        val user = _currentUser.value
        if (!user.tags.contains(oldTag)) return
        val updated = user.copy(tags = user.tags.map { if (it == oldTag) newTag else it }.distinct())
        _currentUser.value = updated
        scope.launch {
            repository?.addUser(updated)
            repository?.updateUserTags(updated.id, updated.tags)
        }
        _contacts.value.filter { it.tags.contains(oldTag) }.forEach { contact ->
            updateServiceHost(contact.copy(tags = contact.tags.map { if (it == oldTag) newTag else it }.distinct()))
        }
    }

    fun deleteServiceHost(userId: String) {
        scope.launch { repository?.deleteUser(userId) }
    }

    fun updateServiceHost(serviceHost: User) {
        scope.launch { repository?.updateServiceHostProfile(serviceHost) }
    }

    fun updateCurrentUser(user: User) {
        _currentUser.value = user
        scope.launch {
            repository?.addUser(user)
            repository?.setCurrentUserId(user.id)
        }
    }

    fun replaceCurrentUser(user: User) {
        _currentUser.value = user
        if (user.id.isNotBlank()) {
            repository?.setCurrentUserId(user.id)
        }
    }

    fun logout() {
        _currentUser.value = User(id = "", name = "", phoneNumber = "", peiPeiId = "", createdAt = 0)
        _contacts.value = emptyList()
        _services.value = emptyList()
        _favoriteServiceIds.value = emptySet()
        _wishlistGroups.value = listOf(DEFAULT_WISHLIST_GROUP)
        _favoriteServiceGroups.value = emptyMap()
        _hostServiceBookings.value = emptyMap()
        _hostCalendarDayClosures.value = emptyMap()
        scope.launch { repository?.logout() }
    }

    private fun ensureGroupExists(groupName: String) {
        if (_wishlistGroups.value.none { it.equals(groupName, ignoreCase = true) }) {
            _wishlistGroups.value = (_wishlistGroups.value + groupName).distinct()
        }
    }

    private fun normalizeGroupName(groupName: String?): String {
        val normalized = groupName?.trim().orEmpty()
        if (normalized.isBlank()) return DEFAULT_WISHLIST_GROUP
        val collapsedWs = normalized.replace(Regex("""\s+"""), "")
        for (m in listOf("北京市", "上海市", "天津市", "重庆市")) {
            if (collapsedWs == m + m) return m
        }
        return normalized
    }

    private fun cleanupFavoriteGroupMappingForCurrentFavorites() {
        val favoriteIds = _favoriteServiceIds.value
        _favoriteServiceGroups.value = _favoriteServiceGroups.value
            .filterKeys { favoriteIds.contains(it) }
            .mapValues { (_, group) -> normalizeGroupName(group) }
    }

    private fun loadWishlistGroupSettings() {
        val prefs = repository?.getSharedPreferences() ?: return
        runCatching {
            val groupJson = prefs.getString(WISHLIST_GROUPS_KEY, null).orEmpty()
            val mappingJson = prefs.getString(WISHLIST_GROUP_MAPPING_KEY, null).orEmpty()
            val loadedGroups = mutableListOf<String>()
            if (groupJson.isNotBlank()) {
                val array = JSONArray(groupJson)
                for (i in 0 until array.length()) {
                    loadedGroups += normalizeGroupName(array.optString(i))
                }
            }
            val finalGroups = (loadedGroups + DEFAULT_WISHLIST_GROUP).distinct()
            _wishlistGroups.value = finalGroups

            if (mappingJson.isNotBlank()) {
                val jsonObject = JSONObject(mappingJson)
                val keys = jsonObject.keys()
                val loadedMapping = mutableMapOf<String, String>()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val value = normalizeGroupName(jsonObject.optString(key))
                    loadedMapping[key] = value
                    ensureGroupExists(value)
                }
                _favoriteServiceGroups.value = loadedMapping
            } else {
                _favoriteServiceGroups.value = emptyMap()
            }
        }.onFailure {
            _wishlistGroups.value = listOf(DEFAULT_WISHLIST_GROUP)
            _favoriteServiceGroups.value = emptyMap()
        }
    }

    private fun persistWishlistGroupSettings() {
        val prefs = repository?.getSharedPreferences() ?: return
        runCatching {
            val groupsJson = JSONArray(_wishlistGroups.value).toString()
            val mappingObject = JSONObject()
            _favoriteServiceGroups.value.forEach { (serviceId, groupName) ->
                mappingObject.put(serviceId, normalizeGroupName(groupName))
            }
            prefs.edit()
                .putString(WISHLIST_GROUPS_KEY, groupsJson)
                .putString(WISHLIST_GROUP_MAPPING_KEY, mappingObject.toString())
                .apply()
        }
    }
}
