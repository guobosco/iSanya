// 文件说明：服务主主页数据加载与交互（ViewModel）。

package com.example.Lulu.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.Lulu.data.local.AppDatabase
import com.example.Lulu.data.model.Service
import com.example.Lulu.data.model.User
import com.example.Lulu.data.repository.LuluRepository
import com.example.Lulu.data.repository.UserRepository
import java.io.IOException
import kotlinx.coroutines.Job
import retrofit2.HttpException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

/**
 * 服务主资料页状态管理：加载目标用户及其已发布服务列表。
 */
class ServiceHostProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val userRepository = UserRepository(application)
    private val serviceDao = AppDatabase.getDatabase(application).serviceDao()

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _publishedServices = MutableStateFlow<List<Service>>(emptyList())
    val publishedServices: StateFlow<List<Service>> = _publishedServices.asStateFlow()

    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    private var observeJob: Job? = null

    fun loadUser(rawUserId: String) {
        val userId = rawUserId.trim()
        observeJob?.cancel()
        if (userId.isEmpty()) {
            _user.value = null
            _publishedServices.value = emptyList()
            _loadError.value = "缺少用户信息"
            return
        }
        _loadError.value = null
        _user.value = null
        observeJob = viewModelScope.launch {
            val displayHint = PendingHostProfileHint.poll(userId)
            try {
                runCatching { LuluRepository.get().getUserByIdSuspend(userId) }
                val fetchResult = userRepository.fetchAndCacheUserByIdResult(userId)
                if (userRepository.getUserByIdSync(userId) == null && !displayHint.isNullOrBlank()) {
                    userRepository.insertUser(User(id = userId, name = displayHint.trim()))
                }
                if (userRepository.getUserByIdSync(userId) == null) {
                    _loadError.value = profileLoadErrorMessage(fetchResult)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (userRepository.getUserByIdSync(userId) == null && !displayHint.isNullOrBlank()) {
                    runCatching { userRepository.insertUser(User(id = userId, name = displayHint.trim())) }
                }
                if (userRepository.getUserByIdSync(userId) == null) {
                    _loadError.value = "加载失败，请检查网络后重试"
                }
            }
            // supervisorScope：服务列表 Flow 异常不应拖垮用户资料收集（此前会导致一直「正在加载」）
            try {
                supervisorScope {
                    launch {
                        try {
                            userRepository.getUserById(userId).collect {
                                _user.value = it
                                if (it != null) {
                                    _loadError.value = null
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            if (_user.value == null && _loadError.value == null) {
                                _loadError.value = "加载失败，请检查网络后重试"
                            }
                        }
                    }
                    launch {
                        try {
                            serviceDao.getServicesFlow(userId).collect {
                                _publishedServices.value = it
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (_user.value == null && _loadError.value == null) {
                    _loadError.value = "加载失败，请检查网络后重试"
                }
            }
        }
    }

    private fun profileLoadErrorMessage(fetchResult: Result<User>): String {
        val e = fetchResult.exceptionOrNull() ?: return "加载失败，请检查网络后重试"
        return when (e) {
            is HttpException -> when (e.code()) {
                404 -> "未找到该用户"
                in 500..599 -> "服务暂时不可用，请稍后重试"
                else -> "加载失败（${e.code()}），请稍后重试"
            }
            is IOException -> "网络异常，请检查网络后重试"
            else -> "加载失败，请检查网络后重试"
        }
    }
}
