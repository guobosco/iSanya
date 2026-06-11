// 文件说明：按用户隔离的 SharedPreferences 或 DataStore 封装。

package com.example.Lulu.util

import android.content.Context
import android.content.SharedPreferences

object UserScopedPrefs {
    fun get(
        context: Context,
        baseName: String,
        userId: String
    ): SharedPreferences {
        val scopedUserId = userId.ifBlank { "guest" }
        return context.getSharedPreferences("${baseName}_$scopedUserId", Context.MODE_PRIVATE)
    }
}
