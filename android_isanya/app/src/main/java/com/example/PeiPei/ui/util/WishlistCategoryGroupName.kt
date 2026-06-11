// 文件说明：心愿单分组名 — 按服务业务类目（与首页筛选 / 发布预设一致）。

package com.example.Lulu.ui.util

import com.example.Lulu.data.model.ServiceCategories

/** 心愿单文件夹名：归一化到预设服务类目，未知类目归为「其他服务」。 */
fun resolveWishlistCategoryGroupName(serviceCategory: String?): String =
    ServiceCategories.normalize(serviceCategory)
