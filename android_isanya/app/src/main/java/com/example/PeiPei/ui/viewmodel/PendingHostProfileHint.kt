// 文件说明：从服务详情进入他人主页时，单次传递发布者展示名，供接口失败时本地兜底展示。

package com.example.Lulu.ui.viewmodel

/**
 * 仅在「服务详情 → 服务主资料」链路使用：[offer] 在 navigate 前调用，[poll] 在 ViewModel.loadUser 开头消费一次。
 */
object PendingHostProfileHint {
    private val lock = Any()
    private var pendingUserId: String? = null
    private var pendingDisplayName: String? = null

    fun offer(forUserId: String, displayName: String) {
        val uid = forUserId.trim()
        val name = displayName.trim()
        if (uid.isEmpty() || name.isEmpty()) return
        synchronized(lock) {
            pendingUserId = uid
            pendingDisplayName = name
        }
    }

    fun poll(forUserId: String): String? = synchronized(lock) {
        val uid = forUserId.trim()
        if (pendingUserId != uid) return null
        val n = pendingDisplayName
        pendingUserId = null
        pendingDisplayName = null
        n
    }
}
