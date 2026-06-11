// 文件说明：首页体验 Tab 与发布弹窗共用的纵向分类种子（单一数据源）。

package com.example.Lulu.data.model

data class ExperienceCategorySeed(
    val title: String,
    val itemTitles: List<String>,
    val metas: List<Pair<String, String>>,
)

/** 体验页纵向分类：标题 + 该类下的体验名 +「类型 · 时长」模板（与首页体验 Tab 一致） */
val experienceCategorySeeds = listOf(
    ExperienceCategorySeed(
        title = "必游经典景区",
        itemTitles = listOf(
            "亚龙湾热带天堂｜玻璃栈道与雨林轻徒步",
            "南山文化旅游区｜海上观音精华半日游",
            "蜈支洲岛一日玩法｜早登岛少排队攻略",
            "天涯海角｜日落机位与小众取景路线",
            "大小洞天｜海岸栈道与礁石摄影线",
            "呀诺达雨林｜踏瀑戏水趣味线",
            "西岛｜渔村骑行 + 近岸珊瑚浮潜",
            "鹿回头｜山顶夜景与城市灯光漫步",
            "槟榔谷｜黎苗文化村寨深度讲解",
            "分界洲岛｜海岛风光一日打卡",
        ),
        metas = listOf(
            "景区导览" to "3 小时",
            "含门票" to "5 小时",
            "半日游" to "4 小时",
            "摄影跟拍" to "2 小时",
            "雨林徒步" to "3.5 小时",
        ),
    ),
    ExperienceCategorySeed(
        title = "潜水冲浪跳伞",
        itemTitles = listOf(
            "PADI 体验潜｜近岸礁盘小团教练跟带",
            "冲浪入门｜后海/日月湾逐浪体验课",
            "塔赫海景跳伞｜含手持/第三方剪辑可选",
            "自由潜基础｜泳池技巧 + 开放水域练习",
            "风筝冲浪体验｜风力合适日预约制",
            "水肺复习潜｜持证 Fun Dive 拼船",
            "浆板 + 浮潜组合｜平静海湾半日",
            "尾波冲浪初体验｜造浪艇小班",
            "夜潜观生物｜季节与潮汐限定",
            "冲浪进阶｜划水与转向小班课",
        ),
        metas = listOf(
            "潜水" to "2 小时",
            "冲浪" to "2.5 小时",
            "跳伞" to "半天",
            "水上运动" to "3 小时",
            "考证课程" to "1 天",
        ),
    ),
    ExperienceCategorySeed(
        title = "海面运动",
        itemTitles = listOf(
            "游艇出海｜海钓 + 浮潜套餐",
            "SUP 桨板｜海湾静音巡航",
            "海上摩托艇｜编队体验与安全讲解",
            "帆板初体验｜风平浪静场次优先",
            "香蕉船/沙发艇｜亲子合家欢套餐",
            "双体帆船日落航次｜含小食饮品",
            "深潜船潜｜外海潜点一日",
            "海上拖伞｜俯瞰海岸线",
            "赶海体验｜退潮时段本地向导",
            "包船海钓｜鱼获可加工推荐餐厅",
        ),
        metas = listOf(
            "出海" to "3 小时",
            "桨板" to "2 小时",
            "摩托艇" to "1 小时",
            "帆船" to "2.5 小时",
            "包船" to "4 小时",
        ),
    ),
    ExperienceCategorySeed(
        title = "文化探索",
        itemTitles = listOf(
            "黎苗非遗村寨｜织锦与竹编手作体验",
            "海上丝绸之路主题｜小型精讲走读",
            "崖州古城｜老街史迹与人文导览",
            "本地早市｜热带水果品鉴与挑选技巧",
            "疍家渔排故事｜水上人家文化探访",
            "东坡书院｜诗文与海南历史线索",
            "黎族长桌宴｜歌舞与饮食文化体验",
            "椰雕/贝雕工坊｜非遗匠人带做小品",
            "咖啡庄园｜从种子到杯测轻体验",
            "民族博物馆精讲｜小团预约场",
        ),
        metas = listOf(
            "文化走读" to "3 小时",
            "非遗体验" to "2 小时",
            "市集美食" to "2 小时",
            "人文讲解" to "2.5 小时",
            "手作课程" to "1.5 小时",
        ),
    ),
    ExperienceCategorySeed(
        title = "夜生活",
        itemTitles = listOf(
            "海湾酒吧街｜微醺路线与安全接送建议",
            "滨海夜市小吃｜隐藏摊位打卡清单",
            "沙滩派对季｜手环入场与动线说明",
            "Rooftop 鸡尾酒｜夜景机位与穿搭建议",
            "Livehouse 一晚｜本地乐队与订座技巧",
            "海鲜大排档夜宵｜防宰与点单攻略",
            "海湾灯光秀｜观赏位与散场交通",
            "深夜清补凉｜老字号与新品对比",
            "游艇夜航｜香槟与星空甲板",
            "电音节周边｜存包饮水与返程拼车",
        ),
        metas = listOf(
            "夜生活" to "3 小时",
            "夜市美食" to "2 小时",
            "夜间演艺" to "2.5 小时",
            "酒吧" to "2 小时",
            "夜宵" to "1.5 小时",
        ),
    ),
)

private val experienceHomeCategoryTitleSet = experienceCategorySeeds.map { it.title }.toSet()

/** 是否为首页体验 Tab 使用的分类标题（与 [experienceCategorySeeds] 一致） */
fun isExperienceHomeCategoryTitle(raw: String?): Boolean =
    raw?.trim().orEmpty() in experienceHomeCategoryTitleSet
