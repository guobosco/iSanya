// 文件说明：服务分类、类目常量或枚举定义。

package com.example.Lulu.data.model

/**
 * 服务类别预设。新建与展示时应归一化到该集合之一。
 */
object ServiceCategories {
    const val LOCAL_GUIDE = "地陪"
    const val PHOTO = "摄影"
    const val DJ_ATMOSPHERE = "DJ气氛组"
    const val FITNESS_COACH = "运动教练"
    const val PRIVATE_CHEF = "私厨"
    const val MAKEUP = "化妆"
    const val OTHER = "其他服务"

    val PRESETS = listOf(
        LOCAL_GUIDE,
        PHOTO,
        DJ_ATMOSPHERE,
        FITNESS_COACH,
        PRIVATE_CHEF,
        MAKEUP,
        OTHER
    )

    /** 首页类别筛选：全部 + 预设类目 */
    const val FILTER_ALL = "全部"
    val HOME_FILTER_CHIPS: List<String> = listOf(FILTER_ALL) + PRESETS

    /**
     * 与首页分类胶囊中除「全部」外的选项一致（同 [PRESETS] 顺序）。
     * 发布、筛选等场景应使用此列表，避免与首页不一致。
     */
    val HOME_SERVICE_CATEGORIES_EXCLUDING_ALL: List<String> get() = PRESETS

    private val presetSet = PRESETS.toSet()

    const val DEFAULT = OTHER

    fun normalize(raw: String?): String {
        val t = raw?.trim().orEmpty()
        if (isExperienceHomeCategoryTitle(t)) return t
        return if (t in presetSet) t else DEFAULT
    }

    /** 各类目在发布表单中的差异化提示与默认计价单位。 */
    data class PublishFormHints(
        val titlePlaceholder: String,
        val descriptionPlaceholder: String,
        /** 新建选中类目时的默认计价单位（用户仍可在价格设置里改）。 */
        val defaultServiceMode: String,
        /** 展示在标题输入区下方的简短引导，可为 null。 */
        val categoryTip: String? = null
    )

    fun publishFormHints(normalizedCategory: String): PublishFormHints = when (normalize(normalizedCategory)) {
        LOCAL_GUIDE -> PublishFormHints(
            titlePlaceholder = "例如：北京三日深度地陪｜含路线规划",
            descriptionPlaceholder = "写清服务城市、每日大致时长、是否含交通/门票建议、语种与人数上限等",
            defaultServiceMode = "天",
            categoryTip = "地陪类建议标明集合地点、单日步行量与可预约时段。"
        )
        PHOTO -> PublishFormHints(
            titlePlaceholder = "例如：领证跟拍｜精修 9 张当日返图",
            descriptionPlaceholder = "写清拍摄场景、时长、交付张数/精修数、底片是否全送及加急政策",
            defaultServiceMode = "小时",
            categoryTip = "摄影类建议标明设备档位与是否含第二机位。"
        )
        DJ_ATMOSPHERE -> PublishFormHints(
            titlePlaceholder = "例如：派对DJ气氛组｜自带控制器与暖场互动",
            descriptionPlaceholder = "说明曲风、人数配置、设备是否自备、到场搭建与暖场互动时长等",
            defaultServiceMode = "小时",
            categoryTip = "DJ气氛组类请写清活动类型、设备分工以及是否含主持或暖场互动。"
        )
        FITNESS_COACH -> PublishFormHints(
            titlePlaceholder = "例如：居家减脂私教｜含饮食打卡",
            descriptionPlaceholder = "写清训练目标、单次课时长、是否需器械、试课政策与适用人群",
            defaultServiceMode = "小时",
            categoryTip = "教练类可补充认证与擅长领域（增肌/体态/产后等）。"
        )
        PRIVATE_CHEF -> PublishFormHints(
            titlePlaceholder = "例如：四人份家宴｜粤菜融合",
            descriptionPlaceholder = "说明菜系、是否含采购、厨房设备要求、忌口处理与餐后清洁范围",
            defaultServiceMode = "次",
            categoryTip = "私厨类建议写清「几人份」与是否含食材费。"
        )
        MAKEUP -> PublishFormHints(
            titlePlaceholder = "例如：新娘跟妆｜含试妆一次",
            descriptionPlaceholder = "写清妆容风格、跟妆时长、是否含假睫毛/饰品、上门或到店等",
            defaultServiceMode = "次",
            categoryTip = "化妆类可说明是否含补妆与跟场时间。"
        )
        else -> PublishFormHints(
            titlePlaceholder = "用一句话概括你的服务",
            descriptionPlaceholder = "详细介绍服务内容、流程和亮点，帮助用户快速了解并下单",
            defaultServiceMode = "次",
            categoryTip = null
        )
    }
}
