// 文件说明：Retrofit/OkHttp 客户端单例或工厂，统一网络配置。

package com.example.Lulu.data.remote

import com.example.Lulu.BuildConfig
import com.example.Lulu.data.model.User
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Type
import java.util.Date
import java.util.concurrent.TimeUnit

object RetrofitClient {
    val BASE_URL: String = if (BuildConfig.API_BASE_URL.endsWith("/")) {
        BuildConfig.API_BASE_URL
    } else {
        "${BuildConfig.API_BASE_URL}/"
    }

    /**
     * 将后端返回的站点相对媒体路径（如 `static/avatars/…`）拼成可请求的绝对 URL。
     * 使用「API 的 scheme/host/port + 根路径 `/`」，避免 BASE_URL 带 `/api/…` 前缀时拼成 `/api/static/…` 导致 404。
     */
    fun resolveBackendRelativeMediaUrl(relativePath: String): String {
        val clean = relativePath.trim().trimStart('/')
        if (clean.isEmpty()) return relativePath
        val originBase = BASE_URL.toHttpUrlOrNull()?.newBuilder()
            ?.encodedPath("/")
            ?.query(null)
            ?.fragment(null)
            ?.build()
            ?.toString()
            ?: BASE_URL
        return originBase.trimEnd('/') + "/" + clean
    }

    /**
     * 将后端返回的站点相对媒体路径转为可直接请求的绝对 URL，供 Coil / ImageRequest 使用。
     * 与 [LuluApplication] 中 Coil [coil.map.Mapper] 的规则一致，避免部分调用路径未走全局 Mapper 时头像无法加载。
     */
    fun normalizeBackendMediaUrlForDisplay(path: String): String {
        val data = path.trim()
        if (data.isEmpty()) return data
        val lower = data.lowercase()
        if (
            lower.startsWith("http://") ||
            lower.startsWith("https://") ||
            data.startsWith("content://") ||
            data.startsWith("file://") ||
            data.startsWith("android.resource://") ||
            lower.startsWith("data:")
        ) {
            return data
        }
        return resolveBackendRelativeMediaUrl(data.trimStart('/'))
    }

    /** 解析 User 本体（不含对 User 再注册 TypeAdapter，避免递归）。 */
    private val gsonUserBody: Gson = GsonBuilder()
        .registerTypeAdapter(Date::class.java, DateTypeAdapter())
        .setLenient()
        .create()

    /**
     * 将 JSON 中可能为 null 的列表字段规范为 []，避免 Gson 反序列化失败；
     * `pei_pei_id` 为 null 时写空串，避免非空 Kotlin 字段异常。
     */
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(Date::class.java, DateTypeAdapter())
        .registerTypeAdapter(
            User::class.java,
            object : JsonDeserializer<User> {
                override fun deserialize(
                    json: JsonElement,
                    @Suppress("UNUSED_PARAMETER") typeOfT: Type,
                    @Suppress("UNUSED_PARAMETER") context: JsonDeserializationContext
                ): User {
                    val o = json.asJsonObject.deepCopy()
                    fun ensureArray(name: String) {
                        val el = o.get(name)
                        if (el == null || el.isJsonNull) o.add(name, JsonArray())
                    }
                    ensureArray("tags")
                    ensureArray("favorite_service_ids")
                    ensureArray("spoken_languages")
                    ensureArray("review_summaries")
                    val pei = o.get("pei_pei_id")
                    if (pei == null || pei.isJsonNull) o.addProperty("pei_pei_id", "")
                    return gsonUserBody.fromJson(o, User::class.java)
                }
            }
        )
        .setLenient()
        .create()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.ENABLE_HTTP_LOGGING) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    private val authInterceptor = Interceptor { chain ->
        val token = AuthSession.getAccessToken()
        val request: Request = if (token.isNullOrEmpty()) {
            chain.request()
        } else {
            chain.request()
                .newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }
        chain.proceed(request)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    val apiService: ApiService = retrofit.create(ApiService::class.java)

    val wsBaseUrl: String = when {
        BASE_URL.startsWith("https://") -> BASE_URL.replaceFirst("https://", "wss://")
        BASE_URL.startsWith("http://") -> BASE_URL.replaceFirst("http://", "ws://")
        else -> BASE_URL
    }

    fun newWebSocket(request: Request, listener: WebSocketListener): WebSocket {
        return okHttpClient.newWebSocket(request, listener)
    }
}
