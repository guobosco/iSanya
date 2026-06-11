// 文件说明：认证会话、Token 或登录态的远程侧封装。

package com.example.Lulu.data.remote

import android.content.SharedPreferences

object AuthSession {
    private const val KEY_ACCESS_TOKEN = "access_token"

    @Volatile
    private var accessToken: String? = null
    private var sharedPreferences: SharedPreferences? = null

    fun initialize(sharedPreferences: SharedPreferences) {
        this.sharedPreferences = sharedPreferences
        accessToken = sharedPreferences.getString(KEY_ACCESS_TOKEN, null)
    }

    fun getAccessToken(): String? = accessToken

    fun updateAccessToken(token: String?) {
        accessToken = token
        sharedPreferences?.edit()?.apply {
            if (token.isNullOrEmpty()) {
                remove(KEY_ACCESS_TOKEN)
            } else {
                putString(KEY_ACCESS_TOKEN, token)
            }
        }?.apply()
    }
}
