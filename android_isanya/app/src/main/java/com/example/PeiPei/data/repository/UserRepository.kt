// 文件说明：用户相关仓库，封装登录、资料等用户数据访问。

package com.example.Lulu.data.repository

/**
 * 用户数据访问仓库文件。
 * 提供用户相关的本地查询与网络搜索/匹配能力，并在可用时复用主仓库能力。
 */

import android.app.Application
import com.example.Lulu.data.local.AppDatabase
import com.example.Lulu.data.model.User
import com.example.Lulu.data.remote.ApiService
import com.example.Lulu.data.remote.RetrofitClient
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException

/** 远程按关键词查用户并 [insertUser] 后的结果（扫码、全局搜索等）。 */
data class RemoteUserLookupOutcome(
    val user: User?,
    val errorMessage: String?
)

class UserRepository(application: Application) {
    private val userDao = AppDatabase.getDatabase(application).userDao()
    private val apiService: ApiService = RetrofitClient.apiService

    fun getAllUsers(): Flow<List<User>> = userDao.getAllUsers()

    fun getUserById(userId: String): Flow<User?> = userDao.getUserById(userId)

    suspend fun getUserByIdSync(userId: String): User? = userDao.getUserByIdSuspend(userId)

    /** 从服务端拉取用户并写入本地，供资料页等场景（发布者可能不在通讯录缓存里）。 */
    suspend fun fetchAndCacheUserById(userId: String): User? =
        fetchAndCacheUserByIdResult(userId).getOrNull()

    /** 与 [fetchAndCacheUserById] 相同，保留失败原因供界面区分 404 / 网络等。 */
    suspend fun fetchAndCacheUserByIdResult(userId: String): Result<User> = runCatching {
        val u = apiService.getUser(userId)
        insertUser(u)
        u
    }.onFailure { e ->
        e.printStackTrace()
        if (e is HttpException) {
            android.util.Log.w("UserRepository", "getUser failed code=${e.code()} userId=$userId")
        }
    }

    // 从本地数据库查找 (保留，用于其他逻辑)
    suspend fun getUserByLuluId(peiPeiId: String): User? = userDao.getUserByLuluId(peiPeiId)

    suspend fun getUserByPhoneNumber(phoneNumber: String): User? = userDao.getUserByPhoneNumber(phoneNumber)

    /**
     * 调用 `users/search` 拉取用户并写入本地，供资料页等读取。
     */
    suspend fun lookupRemoteUserAndPersist(query: String): RemoteUserLookupOutcome {
        val q = query.trim()
        if (q.isEmpty()) {
            return RemoteUserLookupOutcome(user = null, errorMessage = "请输入搜索内容")
        }
        return try {
            val user = apiService.searchUser(q)
            insertUser(user)
            RemoteUserLookupOutcome(user = user, errorMessage = null)
        } catch (e: Exception) {
            e.printStackTrace()
            RemoteUserLookupOutcome(
                user = null,
                errorMessage = "搜索失败: ${e.message ?: "未知错误"}"
            )
        }
    }

    // 从网络批量匹配通讯录
    suspend fun matchContactsFromNetwork(phoneNumbers: List<String>): List<User> {
        return try {
            apiService.matchContacts(mapOf("phone_numbers" to phoneNumbers))
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    suspend fun insertUser(user: User) = userDao.insertUser(user)

    suspend fun updateUser(user: User) = userDao.updateUser(user)

    suspend fun deleteUser(user: User) = userDao.deleteUser(user)
}
