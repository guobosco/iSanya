// 文件说明：发布服务时按类目提供「服务特点」与「额外费用」多选预设。

package com.example.Lulu.data.model

data class ServicePublishOptionGroup(
    val featureTags: List<String>,
    val extraFeeTags: List<String>,
)

object ServicePublishTaxonomy {
    private val optionMap: Map<String, ServicePublishOptionGroup> = mapOf(
        ServiceCategories.LOCAL_GUIDE to ServicePublishOptionGroup(
            featureTags = listOf(
                "情绪价值", "纯绿", "可拉手", "熟悉路线", "会安排行程", "会拍照",
                "会聊天", "英语沟通"
            ),
            extraFeeTags = listOf(
                "油费", "打车费", "门票", "游玩过程产生的费用",
                "停车费", "跨区费", "餐饮AA", "超时费"
            )
        ),
        ServiceCategories.PHOTO to ServicePublishOptionGroup(
            featureTags = listOf(
                "审美在线", "会引导动作", "可修图", "当日返图",
                "底片全送", "情侣拍摄", "闺蜜拍摄", "夜景擅长"
            ),
            extraFeeTags = listOf(
                "门票", "场地费", "交通费", "服装道具费",
                "妆造费", "加急修图费", "超时费", "异地差旅费"
            )
        ),
        ServiceCategories.DJ_ATMOSPHERE to ServicePublishOptionGroup(
            featureTags = listOf(
                "氛围带动", "可控场", "会暖场互动", "自带设备",
                "曲风可定制", "会喊麦", "派对经验", "婚礼经验"
            ),
            extraFeeTags = listOf(
                "设备运输费", "搭建费", "场地费", "灯光设备费",
                "打车费", "超时费", "异地差旅费", "住宿费"
            )
        ),
        ServiceCategories.FITNESS_COACH to ServicePublishOptionGroup(
            featureTags = listOf(
                "纠正动作", "减脂塑形", "增肌力量", "体态改善",
                "饮食建议", "新手友好", "一对一", "可上门"
            ),
            extraFeeTags = listOf(
                "场地费", "器械费", "打车费", "停车费",
                "异地上门费", "超时费", "课程资料费"
            )
        ),
        ServiceCategories.PRIVATE_CHEF to ServicePublishOptionGroup(
            featureTags = listOf(
                "会买菜", "可定制菜单", "摆盘好看", "忌口可调",
                "可做家宴", "可做轻食", "厨房整理", "上门制作"
            ),
            extraFeeTags = listOf(
                "食材费", "打车费", "停车费", "厨具耗材费",
                "场地清洁费", "节假日加价", "超时费"
            )
        ),
        ServiceCategories.MAKEUP to ServicePublishOptionGroup(
            featureTags = listOf(
                "自然妆感", "韩系", "新娘跟妆", "可试妆",
                "假睫毛含", "可上门", "男士妆发", "证件照妆"
            ),
            extraFeeTags = listOf(
                "上门费", "打车费", "假发饰品费", "试妆费",
                "节假日加价", "超时费", "早妆费"
            )
        ),
        ServiceCategories.SKILL_TEACHING to ServicePublishOptionGroup(
            featureTags = listOf(
                "耐心细致", "零基础友好", "课后答疑", "一对一",
                "可上门", "可线上", "系统训练", "可陪练"
            ),
            extraFeeTags = listOf(
                "教材费", "场地费", "设备耗材费", "打车费",
                "停车费", "超时费", "考试报名费"
            )
        ),
        ServiceCategories.OTHER to ServicePublishOptionGroup(
            featureTags = listOf(
                "沟通顺畅", "时间灵活", "可上门", "可定制",
                "新手友好", "长期可约"
            ),
            extraFeeTags = listOf(
                "交通费", "场地费", "材料费", "超时费",
                "异地差旅费", "节假日加价"
            )
        )
    )

    fun featureTagsFor(category: String?): List<String> =
        optionMap[ServiceCategories.normalize(category)]?.featureTags.orEmpty()

    fun extraFeeTagsFor(category: String?): List<String> =
        optionMap[ServiceCategories.normalize(category)]?.extraFeeTags.orEmpty()

    fun normalizeFeatureTags(category: String?, selected: List<String>): List<String> =
        normalizeSelection(selected, featureTagsFor(category))

    fun normalizeExtraFeeTags(category: String?, selected: List<String>): List<String> =
        normalizeSelection(selected, extraFeeTagsFor(category))

    fun selectionSummary(selected: List<String>): String {
        val normalized = selected.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        return when {
            normalized.isEmpty() -> "去选择"
            normalized.size <= 2 -> normalized.joinToString("、")
            else -> "${normalized.take(2).joinToString("、")} 等${normalized.size}项"
        }
    }

    private fun normalizeSelection(selected: List<String>, available: List<String>): List<String> {
        if (available.isEmpty()) return emptyList()
        val selectedSet = selected.map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        return available.filter(selectedSet::contains)
    }
}
