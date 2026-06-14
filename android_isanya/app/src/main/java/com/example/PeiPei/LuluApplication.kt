// 文件说明：Application 入口类，负责全局初始化（如依赖、主题等）。

package com.example.Lulu

/**
 * 应用入口与全局初始化文件。
 * 负责初始化仓库、数据同步、全局图片加载器、通知监听和前后台状态跟踪。
 */

import android.app.Activity
import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import com.example.Lulu.data.local.AppDatabase
import com.example.Lulu.data.local.AppDataStore
import com.example.Lulu.data.repository.LuluRepository
import com.example.Lulu.service.NotificationService
import com.example.Lulu.ui.MainActivity
import com.example.Lulu.util.BadgeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 应用程序类
 * 功能：应用的入口点，初始化应用级别的资源和配置
 */
import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import com.amap.api.maps.MapsInitializer

import com.example.Lulu.data.remote.AuthSession
import com.example.Lulu.data.remote.RetrofitClient
import com.example.Lulu.data.model.User
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.request.CachePolicy
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.io.File

class LuluApplication : Application(), Application.ActivityLifecycleCallbacks, ImageLoaderFactory {
    
    companion object {
        var isForeground = false
    }

    // 应用范围的协程作用域
    private val startupExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        android.util.Log.e("LuluApplication", "Unhandled startup coroutine exception", throwable)
    }
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var activityReferences = 0
    private var isActivityChangingConfigurations = false

    /**
     * 当应用创建时调用
     */
    override fun onCreate() {
        super.onCreate()

        // 高德地图隐私合规（须在创建任何 MapView 之前调用）
        MapsInitializer.updatePrivacyShow(this, true, true)
        MapsInitializer.updatePrivacyAgree(this, true)

        // 强制遵循系统深色模式设置
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        
        // 注册 Activity 生命周期回调以追踪应用前后台状态
        registerActivityLifecycleCallbacks(this)
        
        // Initialize Room Database and Repository
        val database = AppDatabase.getDatabase(this)
        val sharedPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        AuthSession.initialize(sharedPrefs)
        val repository = LuluRepository.getInstance(
            database.userDao(), 
            database.serviceDao(),
            database.conversationDao(),
            database.conversationMemberDao(),
            database.chatMessageDao(),
            sharedPrefs,
            this,
            RetrofitClient.apiService
        )
        
        // Initialize AppDataStore proxy
        AppDataStore.initialize(repository)

        // 无 token 时写入 demo 用户；首页瀑布流仅展示 Room 内真实同步数据（见 ServiceDao / fetchAndSyncDiscoveryServices）
        applicationScope.launch(startupExceptionHandler) {
            runCatching {
                ensureDefaultLoggedInUser(repository)
            }.onFailure { throwable ->
                android.util.Log.e("LuluApplication", "Startup initialization failed", throwable)
            }
        }

        applicationScope.launch(startupExceptionHandler) {
            repository.newChatMessageFlow.collect { message: com.example.Lulu.data.model.ChatMessage ->
                runCatching {
                    val conversation = repository.getConversationById(message.conversationId).first()
                    val title = conversation?.title?.ifBlank { "新消息" } ?: "新消息"
                    val content = when (message.type) {
                        "image" -> "[图片]"
                        "file" -> message.attachmentName.ifBlank { "[文件]" }
                        else -> message.content.ifBlank { "收到一条消息" }
                    }
                    val id = message.createdAt.toInt()
                    val intent = Intent(this@LuluApplication, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        putExtra("navigate_to", "message_thread")
                        putExtra("threadId", message.conversationId)
                    }
                    val pendingIntent = PendingIntent.getActivity(
                        this@LuluApplication,
                        id,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    NotificationService.showNotification(
                        context = this@LuluApplication,
                        title = title,
                        content = content,
                        id = id,
                        tag = "chat_message",
                        badgeCount = 1,
                        customPendingIntent = pendingIntent
                    )
                }.onFailure { throwable ->
                    android.util.Log.e("LuluApplication", "Handle chat notification failed", throwable)
                }
            }
        }
    }

    override fun newImageLoader(): ImageLoader {
        val avatarCacheInterceptor = Interceptor { chain ->
            val response = chain.proceed(chain.request())
            val contentType = response.header("Content-Type") ?: ""
            if (contentType.startsWith("image/")) {
                response.newBuilder()
                    .header("Cache-Control", "public, max-age=86400")
                    .build()
            } else {
                response
            }
        }
        val okHttpImageClient = OkHttpClient.Builder()
            .cache(Cache(File(cacheDir, "okhttp_image_cache"), 50L * 1024 * 1024))
            .addNetworkInterceptor(avatarCacheInterceptor)
            .build()
        return ImageLoader.Builder(this)
            .components {
                add(object : coil.map.Mapper<String, String> {
                    override fun map(data: String, options: coil.request.Options): String {
                        if (data.isEmpty()) {
                            return data
                        }

                        // Keep absolute/local schemes unchanged.
                        if (
                            data.startsWith("http://") ||
                            data.startsWith("https://") ||
                            data.startsWith("content://") ||
                            data.startsWith("file://") ||
                            data.startsWith("android.resource://") ||
                            data.startsWith("data:")
                        ) {
                            return data
                        }

                        // 站点根路径下的静态资源（如 static/avatars/…），避免 API BASE_URL 带路径前缀时拼错。
                        val cleanData = data.trimStart('/')
                        return RetrofitClient.resolveBackendRelativeMediaUrl(cleanData)
                    }
                })
            }
            .okHttpClient(okHttpImageClient)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    // --- Activity Lifecycle Callbacks ---

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        if (++activityReferences == 1 && !isActivityChangingConfigurations) {
            // App enters foreground
            isForeground = true
        }
    }

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {
        isActivityChangingConfigurations = activity.isChangingConfigurations
        if (--activityReferences == 0 && !isActivityChangingConfigurations) {
            // App enters background
            isForeground = false
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}

    private suspend fun ensureDefaultLoggedInUser(repository: LuluRepository) {
        if (repository.currentUserId.isNotEmpty()) {
            return
        }

        val defaultUser = User(
            id = "local_demo_user",
            name = "i三亚用户",
            peiPeiId = "pp_demo",
            phoneNumber = "13800000000",
            isPhoneVerified = true,
            isProfileCompleted = true,
            gender = "女",
            region = "杭州",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        repository.addUser(defaultUser)
        AppDataStore.replaceCurrentUser(defaultUser)
    }
}
