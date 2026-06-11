// 文件说明：从 Compose Dialog 内的 View 解析其宿主 Window（与 Activity 主窗口不同）。

package com.example.Lulu.ui.util

import android.content.Context
import android.content.ContextWrapper
import android.view.View
import android.view.ViewParent
import androidx.compose.ui.window.DialogWindowProvider

private fun ViewParent?.dialogWindowFromAncestors(): android.view.Window? {
    var p = this
    while (p != null) {
        if (p is DialogWindowProvider) return p.window
        p = if (p is View) {
            val child: View = p
            child.parent
        } else {
            null
        }
    }
    return null
}

fun View.findComposeDialogWindow(): android.view.Window? {
    return parent.dialogWindowFromAncestors()
        ?: (rootView as? View)?.parent?.dialogWindowFromAncestors()
        ?: context.findDialogWindowFromContext()
}

private tailrec fun Context.findDialogWindowFromContext(): android.view.Window? {
    return when (this) {
        is android.app.Dialog -> window
        is ContextWrapper -> baseContext.findDialogWindowFromContext()
        else -> null
    }
}
