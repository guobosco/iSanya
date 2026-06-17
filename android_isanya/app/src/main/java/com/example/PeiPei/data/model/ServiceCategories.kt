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
    const val SKILL_TEACHING = "技能教学"
    const val OTHER = "其他服务"

    val PRESETS = listOf(
        LOCAL_GUIDE,
        PHOTO,
        DJ_ATMOSPHERE,
        FITNESS_COACH,
        PRIVATE_CHEF,
        MAKEUP,
        SKILL_TEACHING,
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
            titlePlaceholder = "例如：亚龙湾一日地陪｜含路线规划",
            descriptionPlaceholder = "写清服务城市、陪同时间、是否含路线规划与门票建议，以及适合的人群。",
            defaultServiceMode = "天",
            categoryTip = "地陪类建议标明集合地点、可预约时段和服务人数。"
        )
        PHOTO -> PublishFormHints(
            titlePlaceholder = "例如：三亚海边旅拍｜精修 9 张",
            descriptionPlaceholder = "写清拍摄场景、时长、交付张数、修图周期和是否支持加急。",
            defaultServiceMode = "小时",
            categoryTip = "摄影类建议写明设备档位、精修数量和返图时效。"
        )
        DJ_ATMOSPHERE -> PublishFormHints(
            titlePlaceholder = "例如：沙滩派对 DJ 暖场｜双人配置",
            descriptionPlaceholder = "介绍曲风、活动类型、人员分工、设备情况以及可承接时长。",
            defaultServiceMode = "小时",
            categoryTip = "活动氛围类建议补充设备、主持或互动内容。"
        )
        FITNESS_COACH -> PublishFormHints(
            titlePlaceholder = "例如：晨间燃脂私教｜适合新手",
            descriptionPlaceholder = "写清训练目标、课时长度、地点、适合人群和所需器材。",
            defaultServiceMode = "小时",
            categoryTip = "教练类可标明认证背景和擅长方向。"
        )
        PRIVATE_CHEF -> PublishFormHints(
            titlePlaceholder = "例如：4 人海鲜家宴｜到家现做",
            descriptionPlaceholder = "说明菜系、份量、是否含采购、忌口处理和餐后清洁范围。",
            defaultServiceMode = "次",
            categoryTip = "私厨类建议写明人数、菜单风格和是否含食材费。"
        )
        MAKEUP -> PublishFormHints(
            titlePlaceholder = "例如：婚礼跟妆｜含试妆一次",
            descriptionPlaceholder = "写清妆容风格、服务时长、是否含饰品与上门区域。",
            defaultServiceMode = "次",
            categoryTip = "妆造类建议说明是否含补妆与跟场时长。"
        )
        SKILL_TEACHING -> PublishFormHints(
            titlePlaceholder = "例如：潜水入门理论课｜1 对 1 陪练",
            descriptionPlaceholder = "说明教学方向、单次时长、适合基础以及课后支持内容。",
            defaultServiceMode = "小时",
            categoryTip = "教学类建议标注课程目标、适合人群与器材要求。"
        )
        else -> PublishFormHints(
            titlePlaceholder = "用一句话概括你的服务",
            descriptionPlaceholder = "详细介绍服务内容、流程和亮点，帮助用户快速了解并下单。",
            defaultServiceMode = "次",
            categoryTip = null
        )
    }
}
