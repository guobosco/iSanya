package com.example.Lulu.data.model

data class PublishCopyTemplate(
    val styleTitle: String,
    val styleSubtitle: String,
    val title: String,
    val description: String,
)

data class PublishFormGuidance(
    val titlePlaceholder: String,
    val descriptionPlaceholder: String,
    val categoryTip: String?,
    val templates: List<PublishCopyTemplate>,
)

object PublishFormContentCatalog {
    fun normalizeCategory(raw: String?): String? {
        val normalized = raw?.trim().orEmpty()
        if (normalized.isEmpty()) return null
        return if (isExperienceHomeCategoryTitle(normalized)) normalized else ServiceCategories.normalize(normalized)
    }

    fun guidanceFor(category: String?): PublishFormGuidance {
        val normalized = normalizeCategory(category)
        return when (normalized) {
            ServiceCategories.LOCAL_GUIDE -> PublishFormGuidance(
                titlePlaceholder = "例如：亚龙湾一日地陪｜含路线规划",
                descriptionPlaceholder = "写清服务城市、陪同时间、是否含路线规划与门票建议，以及适合的人群。",
                categoryTip = "地陪类建议标明集合地点、可预约时段和服务人数。",
                templates = listOf(
                    template("简洁直给", "先把核心信息写清楚", "三亚一日地陪｜路线规划 + 陪同出行", "可陪你在三亚轻松玩一天，提前一起确认路线、集合地点和时间安排。适合第一次来、想少走弯路、希望行程更省心的用户。"),
                    template("亮点种草", "突出轻松省心的体验感", "本地人带你轻松玩三亚｜少绕路更省心", "结合你的出行节奏帮你安排路线、拍照点和休息时间，不赶路也不乱逛。适合情侣、朋友结伴和带长辈出行。"),
                    template("专业说明", "适合讲清流程和边界", "三亚定制地陪｜按需求规划路线与陪同", "服务前会先沟通人数、偏好和可用时间，再给出建议路线与集合方案。可陪同游玩、协助安排节奏，不含门票和额外消费。"),
                    template("轻松邀请", "语气更自然亲近", "来三亚别自己做攻略了，我带你轻松玩", "如果你想玩得轻松一点，我可以帮你把路线、时间和集合安排都理顺。你只需要告诉我想去哪里、想怎么玩，我们一起把当天行程安排好。"),
                ),
            )
            ServiceCategories.PHOTO -> PublishFormGuidance(
                titlePlaceholder = "例如：三亚海边旅拍｜精修 9 张",
                descriptionPlaceholder = "写清拍摄场景、时长、交付张数、修图周期和是否支持加急。",
                categoryTip = "摄影类建议写明设备档位、精修数量和返图时效。",
                templates = listOf(
                    template("简洁直给", "适合快速起稿", "三亚海边旅拍｜精修 9 张", "适合情侣、闺蜜和个人出游拍摄，可提前沟通风格、时间和取景地点。包含基础引导与后期精修，帮助你轻松留下一组自然出片。"),
                    template("亮点种草", "更强调成片氛围", "海边松弛感旅拍｜自然抓拍更出片", "主打自然互动和轻松氛围，不会一直摆拍，边走边拍也能出状态。适合想要海边氛围感照片、又担心镜头尴尬的用户。"),
                    template("专业说明", "适合说明交付细节", "三亚旅拍跟拍｜含选片与精修交付", "可提前沟通拍摄风格、服装建议和时间安排，拍摄后按约定交付精修照片。是否支持加急、底片数量和修图周期，可在下单前确认。"),
                    template("轻松邀请", "语气更柔和", "把你的海边旅行，拍成一组会想反复看的照片", "想记录这次旅行又不想拍得太用力，可以来试试这种轻松自然的旅拍方式。我们会一起选更适合你的时间、光线和拍摄路线。"),
                ),
            )
            ServiceCategories.DJ_ATMOSPHERE -> PublishFormGuidance(
                titlePlaceholder = "例如：沙滩派对 DJ 暖场｜双人配置",
                descriptionPlaceholder = "介绍曲风、活动类型、人员分工、设备情况以及可承接时长。",
                categoryTip = "活动氛围类建议补充设备、主持或互动内容。",
                templates = listOf(
                    template("简洁直给", "先讲清活动和配置", "沙滩派对 DJ 暖场｜双人配置", "适合沙滩派对、生日聚会和小型活动暖场，可提前沟通曲风、设备和现场氛围需求。帮助你把活动前段和高点节奏带起来。"),
                    template("亮点种草", "强调现场情绪价值", "把你的派对氛围先点起来｜暖场 + 节奏带动", "从开场铺垫到人群升温，会根据现场节奏调整音乐和互动氛围。适合希望活动更热闹、更有记忆点的场景。"),
                    template("专业说明", "适合写清配合方式", "活动 DJ 氛围服务｜可沟通曲风与流程节点", "可在活动前确认时长、场地条件、设备准备和流程节点，根据现场情况调整音乐节奏。是否含主持、控场或额外人员支持，可提前沟通。"),
                    template("轻松邀请", "语气更自然", "想让现场没那么冷场？可以先把氛围交给我", "如果你担心活动前半段太安静，或者希望大家更快进入状态，可以提前把曲风和节奏想法告诉我，我来帮你把气氛慢慢带起来。"),
                ),
            )
            ServiceCategories.FITNESS_COACH -> PublishFormGuidance(
                titlePlaceholder = "例如：晨间燃脂私教｜适合新手",
                descriptionPlaceholder = "写清训练目标、课时长度、地点、适合人群和所需器材。",
                categoryTip = "教练类可标明认证背景和擅长方向。",
                templates = listOf(
                    template("简洁直给", "适合先说明目标", "晨间燃脂私教｜适合新手", "可根据你的体能基础和目标安排训练内容，适合减脂塑形、恢复状态或建立运动习惯。节奏循序渐进，适合刚开始运动的人。"),
                    template("亮点种草", "更强调陪伴感", "不硬撑的私教课｜从状态找回开始", "训练会结合你的身体状态做调整，不追求一开始就练很猛，更注重动作感受和节奏建立。适合久坐、想重新动起来的人。"),
                    template("专业说明", "适合讲清课程安排", "一对一训练指导｜按目标安排课程内容", "课前可先沟通目标、训练经历和身体情况，再安排适合的课程内容。可做燃脂、体态调整、基础力量训练等方向。"),
                    template("轻松邀请", "语气更友好", "想开始运动但总不知道怎么练？我陪你一起找节奏", "如果你总是想开始又坚持不下来，可以先从一节不那么有压力的课开始。我们先把动作和节奏找对，再慢慢往前走。"),
                ),
            )
            ServiceCategories.PRIVATE_CHEF -> PublishFormGuidance(
                titlePlaceholder = "例如：4 人海鲜家宴｜到家现做",
                descriptionPlaceholder = "说明菜系、份量、是否含采购、忌口处理和餐后清洁范围。",
                categoryTip = "私厨类建议写明人数、菜单风格和是否含食材费。",
                templates = listOf(
                    template("简洁直给", "先讲人数和形式", "4 人海鲜家宴｜到家现做", "适合朋友聚餐、家庭聚会和小型庆祝，可提前沟通口味、人数和是否需要代买食材。到场现做，尽量让整顿饭吃得更轻松。"),
                    template("亮点种草", "突出氛围和体验", "把聚餐交给我，你只管好好吃饭", "从口味沟通到上桌节奏都会提前安排，适合不想自己准备、又想吃得更有仪式感的聚会场景。海鲜、家常菜和轻宴会都可沟通。"),
                    template("专业说明", "适合写清服务边界", "上门私厨服务｜菜单沟通 + 现场制作", "下单前可确认人数、预算、菜系偏好和厨房条件，再安排菜单与采购方式。是否含食材、餐后清洁范围和特殊忌口处理，可提前说明。"),
                    template("轻松邀请", "语气更温和", "想在家好好吃顿饭，我来帮你把这一餐做好", "不管是朋友来家里小聚，还是想给家人准备一顿像样的晚餐，都可以提前把人数和口味告诉我，我来帮你把这一餐安排妥当。"),
                ),
            )
            ServiceCategories.MAKEUP -> PublishFormGuidance(
                titlePlaceholder = "例如：婚礼跟妆｜含试妆一次",
                descriptionPlaceholder = "写清妆容风格、服务时长、是否含饰品与上门区域。",
                categoryTip = "妆造类建议说明是否含补妆与跟场时长。",
                templates = listOf(
                    template("简洁直给", "先说明场景和时长", "婚礼跟妆｜含试妆一次", "适合婚礼、写真、活动和约会妆造，可提前沟通风格、发型和饰品需求。根据场景安排妆面细节，让整体状态更自然耐看。"),
                    template("亮点种草", "突出上镜和氛围感", "轻透上镜妆造｜自然又有精致感", "根据脸型、气质和场景做更适合你的妆造调整，不盲目套模板。适合想保留自己特点、又想更上镜更精神的用户。"),
                    template("专业说明", "适合写清服务内容", "妆造服务｜可沟通风格、跟妆与上门区域", "下单前可先确认服务场景、开始时间、是否需要试妆和上门范围。是否含饰品、补妆和跟场时长，可提前沟通后确定。"),
                    template("轻松邀请", "语气更亲近", "把妆造这件事交给我，你安心去见重要的人", "如果你不想在重要场合前手忙脚乱，可以提前把喜欢的风格和场景告诉我，我会帮你把妆面和整体状态准备得更稳妥。"),
                ),
            )
            ServiceCategories.SKILL_TEACHING -> PublishFormGuidance(
                titlePlaceholder = "例如：潜水入门理论课｜1 对 1 陪练",
                descriptionPlaceholder = "说明教学方向、单次时长、适合基础以及课后支持内容。",
                categoryTip = "教学类建议标注课程目标、适合人群与器材要求。",
                templates = listOf(
                    template("简洁直给", "先说明方向和适合人群", "潜水入门理论课｜1 对 1 陪练", "适合刚接触这项技能、想系统入门的人，可根据基础安排学习内容和练习节奏。帮助你先把关键动作和理解打稳。"),
                    template("亮点种草", "突出轻松上手", "从零开始也能学明白｜入门陪练课", "不需要一开始就懂很多，会结合你的基础一步步带你进入状态。适合怕学不会、想先体验再决定是否继续深入的人。"),
                    template("专业说明", "适合讲清课程结构", "一对一技能教学｜按基础制定练习重点", "可先沟通学习目标、已有基础和可安排时间，再确定课程节奏与练习重点。适合入门、纠正动作和阶段性提升。"),
                    template("轻松邀请", "语气更自然", "想认真学一项新技能？先从一节能听懂的课开始", "如果你已经想学很久，但一直没有真正开始，可以先来上一节更轻松的入门课。我们先把最重要的部分学明白，再慢慢往下走。"),
                ),
            )
            "必游经典景区" -> PublishFormGuidance(
                titlePlaceholder = "例如：南山文化旅游区半日精讲",
                descriptionPlaceholder = "写清体验路线、适合人群、停留时长、是否含接驳与门票建议。",
                categoryTip = "景区体验建议写明集合时间、步行强度和路线亮点。",
                templates = listOf(
                    template("简洁直给", "适合先讲清路线", "南山文化旅游区半日精讲", "适合第一次来、想把经典景区玩得更明白的人。会提前沟通集合时间、路线节奏和停留重点，帮助你省去临场做攻略的时间。"),
                    template("亮点种草", "突出体验感和画面感", "带你轻松打卡经典景区｜少走弯路更尽兴", "不只是到此一游，会结合景点亮点、拍照点和停留节奏来安排体验。适合想玩得顺、拍得好、又不想太赶的用户。"),
                    template("专业说明", "适合讲清流程和包含项", "经典景区体验｜路线讲解 + 节奏安排", "体验前可先确认集合地点、游览时长、步行强度和重点景点安排。是否含门票建议、接驳方式和额外消费说明，可提前沟通清楚。"),
                    template("轻松邀请", "语气更自然", "第一次来这个景区？我带你按更舒服的节奏慢慢玩", "如果你不想一边查路线一边赶景点，可以提前把出行偏好告诉我。我会帮你把集合、游览和停留重点安排得更清楚。"),
                ),
            )
            "潜水冲浪跳伞" -> PublishFormGuidance(
                titlePlaceholder = "例如：后海冲浪入门｜小班体验课",
                descriptionPlaceholder = "介绍项目类型、时长、教练配比、装备提供情况与安全要求。",
                categoryTip = "运动体验建议明确安全说明、装备包含项和天气限制。",
                templates = listOf(
                    template("简洁直给", "先说明项目和安全感", "后海冲浪入门｜小班体验课", "适合第一次体验水上项目的用户，会先说明流程、基础动作和注意事项，再根据现场情况安排体验节奏。"),
                    template("亮点种草", "更强调新鲜感和成就感", "第一次玩也能放心上手｜海边运动轻体验", "从下水前准备到正式体验都会有陪同和讲解，减少第一次尝试时的紧张感。适合想体验刺激感、又希望过程更稳妥的人。"),
                    template("专业说明", "适合讲清装备与条件", "潜水/冲浪体验｜含基础讲解与装备说明", "可提前确认项目类型、时长、装备提供情况、教练配比和天气要求。体验前会先做安全说明，确保流程更清楚。"),
                    template("轻松邀请", "语气更亲和", "想试试海上的新鲜玩法？从一场更安心的体验开始", "如果你一直想尝试潜水、冲浪或跳伞，但又担心自己没基础，可以先从更轻松的小班体验开始，慢慢找到感觉。"),
                ),
            )
            "海面运动" -> PublishFormGuidance(
                titlePlaceholder = "例如：日落双体帆船航次｜含饮品",
                descriptionPlaceholder = "写清出海流程、人数上限、船型配置、可选玩法和集合码头。",
                categoryTip = "海上体验建议写明出发码头、船程时长和成团人数。",
                templates = listOf(
                    template("简洁直给", "先讲清玩法和集合", "日落双体帆船航次｜含饮品", "适合朋友结伴、情侣出游和轻松看海的场景，可提前确认集合码头、船程时长和船上安排。整体节奏轻松，适合拍照和放空。"),
                    template("亮点种草", "突出海上氛围", "把海边傍晚过得更浪漫一点｜日落出海体验", "在更舒服的海风和光线里出发，适合想看日落、拍照或和朋友慢慢放松的行程。比起赶景点，更适合享受过程。"),
                    template("专业说明", "适合写清出海细节", "海面运动体验｜码头集合 + 船程安排", "可提前确认集合地点、出海时间、人数上限、船型配置和天气影响。是否包含饮品、浮潜或其他玩法，可在预约前说明清楚。"),
                    template("轻松邀请", "语气更柔和", "想去海上待一会儿，就把这段时间留给风景和海风", "如果你想安排一段更轻松的出海时间，不想太赶，也不想太复杂，这类体验会更适合。提前确认集合和玩法后，就可以安心出发。"),
                ),
            )
            "文化探索" -> PublishFormGuidance(
                titlePlaceholder = "例如：黎苗非遗手作半日体验",
                descriptionPlaceholder = "说明体验主题、讲解内容、互动环节、适合年龄和是否含材料。",
                categoryTip = "文化探索类建议补充讲解重点和参与感设计。",
                templates = listOf(
                    template("简洁直给", "先讲清主题和互动", "黎苗非遗手作半日体验", "适合想了解本地文化、又希望过程不枯燥的用户。体验中会包含主题讲解和互动环节，让你对当地文化有更具体的感受。"),
                    template("亮点种草", "突出故事感和参与感", "不只是看看而已｜把本地文化真正体验一遍", "从讲解到互动都会更有参与感，不只是走一圈拍张照。适合想把这段旅程玩得更有内容、更有记忆点的人。"),
                    template("专业说明", "适合说明流程边界", "文化探索体验｜讲解内容 + 互动环节可提前沟通", "可根据主题确认体验时长、互动方式、适合年龄和是否包含材料。适合亲子、朋友结伴或希望了解本地文化的用户。"),
                    template("轻松邀请", "语气更自然", "如果你想玩点不一样的，这类文化体验会很值得", "比起纯打卡，这样的体验会更像慢下来认识一个地方。可以提前告诉我你更想听故事、做手作，还是边走边了解当地文化。"),
                ),
            )
            "夜生活" -> PublishFormGuidance(
                titlePlaceholder = "例如：海湾夜游路线｜本地人带玩",
                descriptionPlaceholder = "写清夜游动线、亮点场景、时长、安全提示和返程建议。",
                categoryTip = "夜生活体验建议注明集合点、结束时间和安全提醒。",
                templates = listOf(
                    template("简洁直给", "先说明夜游节奏", "海湾夜游路线｜本地人带玩", "适合想体验本地夜生活、又不想临时做攻略的人。可提前沟通集合地点、结束时间和偏好的夜游氛围。"),
                    template("亮点种草", "突出夜晚氛围", "把三亚的夜晚玩得更有意思一点", "从夜景、音乐到小吃和散步路线，都可以根据你的节奏来安排。适合朋友结伴、情侣出游和想体验城市另一面的用户。"),
                    template("专业说明", "适合讲清动线与提醒", "夜游体验｜路线建议 + 安全提醒 + 返程建议", "体验前会先沟通集合点、夜游路线、结束时间和返程方式，帮助你把晚上安排得更顺。涉及酒吧或夜市场景时，也会提前说明注意事项。"),
                    template("轻松邀请", "语气更松弛", "白天玩景点，晚上就跟着我去感受这座城市的另一面", "如果你想把夜晚也安排得更有意思，可以提前告诉我你更喜欢微醺、散步、夜景还是夜市。我会帮你把路线和节奏理得更顺。"),
                ),
            )
            else -> if (isExperienceHomeCategoryTitle(normalized)) genericExperienceGuidance() else genericServiceGuidance()
        }
    }

    private fun genericServiceGuidance(): PublishFormGuidance = PublishFormGuidance(
        titlePlaceholder = "用一句话概括你的服务",
        descriptionPlaceholder = "详细介绍服务内容、流程和亮点，帮助用户快速了解并下单。",
        categoryTip = null,
        templates = listOf(
            template("简洁直给", "适合快速起稿", "用一句话概括你的服务", "先写清你提供什么、适合谁、通常怎么进行，帮助用户快速判断是否适合自己。"),
            template("亮点种草", "突出服务吸引力", "把你的服务亮点先说出来", "可以先从最有吸引力的部分开始写，比如体验感、结果感受或别人为什么愿意选择你。"),
            template("专业说明", "适合说明细节", "把服务流程和边界讲清楚", "建议补充服务时长、包含项、不包含项和适合人群，让用户在咨询前先了解大致内容。"),
            template("轻松邀请", "语气更自然", "先写一版给用户看的介绍", "不用一开始写得很正式，先把你想提供的内容、适合的用户和你希望传达的感觉写出来，再慢慢调整。"),
        ),
    )

    private fun genericExperienceGuidance(): PublishFormGuidance = PublishFormGuidance(
        titlePlaceholder = "用一句话概括你的体验",
        descriptionPlaceholder = "详细介绍体验流程、亮点安排和适合人群，帮助用户快速做决定。",
        categoryTip = null,
        templates = listOf(
            template("简洁直给", "适合快速起稿", "用一句话概括你的体验", "先写清这次体验的主题、适合人群和大致时长，让用户第一眼就知道这是怎样的一次安排。"),
            template("亮点种草", "突出体验感和氛围", "先把这次体验最吸引人的地方写出来", "可以先从路线亮点、现场氛围或最值得期待的部分写起，让用户更容易产生兴趣。"),
            template("专业说明", "适合说明流程边界", "把体验流程和注意事项讲清楚", "建议补充集合方式、流程安排、包含内容和需要提前知道的事项，方便用户更快做决定。"),
            template("轻松邀请", "语气更自然", "先写一版更像邀请的介绍", "你可以先把这次体验为什么值得参加、适合什么样的人、过程大概怎样，用更自然的语气写出来。"),
        ),
    )

    private fun template(
        styleTitle: String,
        styleSubtitle: String,
        title: String,
        description: String,
    ) = PublishCopyTemplate(styleTitle, styleSubtitle, title, description)
}
