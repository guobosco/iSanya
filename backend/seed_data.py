import time

import bcrypt
from sqlalchemy.orm import Session

try:
    from . import models
except ImportError:
    import models


SEED_LOGIN_PASSWORD = "123456"


def _now_ms() -> int:
    return int(time.time() * 1000)


def _static_url(relative_path: str) -> str:
    return f"static/{relative_path.lstrip('/')}"


def _hash_seed_password() -> str:
    return bcrypt.hashpw(SEED_LOGIN_PASSWORD.encode("utf-8"), bcrypt.gensalt()).decode("utf-8")


USER_SEEDS = [
    {
        "id": "seed-user-01",
        "pei_pei_id": "seed001",
        "name": "阿宁",
        "phone_number": "18800000001",
        "photo_url": _static_url("avatars/avatar_01.jpg"),
        "profile_image_urls": [
            _static_url("avatars/avatar_01.jpg"),
            _static_url("avatars/avatar_07.jpg"),
        ],
        "tags": ["地陪", "会拍照", "会规划路线"],
        "signature": "熟悉三亚湾到海棠湾路线，擅长安排轻松不赶的行程。",
        "gender": "女",
        "region": "海南 三亚",
        "job_title": "地陪主理人",
        "education": "旅游管理",
        "birth_decade": "95后",
        "spoken_languages": ["普通话", "英语"],
        "living_city": "三亚",
        "living_country": "中国",
        "identity_verified": True,
        "is_phone_verified": True,
        "is_profile_completed": True,
    },
    {
        "id": "seed-user-02",
        "pei_pei_id": "seed002",
        "name": "小岛",
        "phone_number": "18800000002",
        "photo_url": _static_url("avatars/avatar_02.jpg"),
        "profile_image_urls": [
            _static_url("avatars/avatar_02.jpg"),
            _static_url("avatars/avatar_08.jpg"),
        ],
        "tags": ["摄影", "情侣拍摄", "夜景擅长"],
        "signature": "擅长海边氛围感和纪实跟拍，返图节奏稳定。",
        "gender": "男",
        "region": "海南 三亚",
        "job_title": "旅拍摄影师",
        "education": "视觉传达",
        "birth_decade": "90后",
        "spoken_languages": ["普通话"],
        "living_city": "三亚",
        "living_country": "中国",
        "identity_verified": True,
        "is_phone_verified": True,
        "is_profile_completed": True,
    },
    {
        "id": "seed-user-03",
        "pei_pei_id": "seed003",
        "name": "Mika",
        "phone_number": "18800000003",
        "photo_url": _static_url("avatars/avatar_03.jpg"),
        "profile_image_urls": [
            _static_url("avatars/avatar_03.jpg"),
            _static_url("avatars/avatar_09.jpg"),
        ],
        "tags": ["DJ", "暖场互动", "婚礼经验"],
        "signature": "婚礼、生日派对和小型 club 包场都接，曲风可定制。",
        "gender": "女",
        "region": "海南 三亚",
        "job_title": "DJ / 气氛组",
        "education": "音乐制作",
        "birth_decade": "95后",
        "spoken_languages": ["普通话", "英语"],
        "living_city": "三亚",
        "living_country": "中国",
        "identity_verified": True,
        "is_phone_verified": True,
        "is_profile_completed": True,
    },
    {
        "id": "seed-user-04",
        "pei_pei_id": "seed004",
        "name": "Tina",
        "phone_number": "18800000004",
        "photo_url": _static_url("avatars/avatar_04.jpg"),
        "profile_image_urls": [
            _static_url("avatars/avatar_04.jpg"),
            _static_url("avatars/avatar_10.jpg"),
        ],
        "tags": ["私教", "体态改善", "新手友好"],
        "signature": "擅长海边晨练、减脂塑形和一对一陪练。",
        "gender": "女",
        "region": "海南 三亚",
        "job_title": "运动教练",
        "education": "运动康复",
        "birth_decade": "90后",
        "spoken_languages": ["普通话"],
        "living_city": "三亚",
        "living_country": "中国",
        "identity_verified": True,
        "is_phone_verified": True,
        "is_profile_completed": True,
    },
    {
        "id": "seed-user-05",
        "pei_pei_id": "seed005",
        "name": "Kiki",
        "phone_number": "18800000005",
        "photo_url": _static_url("avatars/avatar_05.jpg"),
        "profile_image_urls": [
            _static_url("avatars/avatar_05.jpg"),
            _static_url("avatars/avatar_11.jpg"),
        ],
        "tags": ["私厨", "上门制作", "可定制菜单"],
        "signature": "擅长 2-6 人家宴和海鲜轻食，沟通忌口会很细。",
        "gender": "女",
        "region": "海南 三亚",
        "job_title": "私厨",
        "education": "酒店管理",
        "birth_decade": "90后",
        "spoken_languages": ["普通话"],
        "living_city": "三亚",
        "living_country": "中国",
        "identity_verified": True,
        "is_phone_verified": True,
        "is_profile_completed": True,
    },
    {
        "id": "seed-user-06",
        "pei_pei_id": "seed006",
        "name": "Luna",
        "phone_number": "18800000006",
        "photo_url": _static_url("avatars/avatar_06.jpg"),
        "profile_image_urls": [
            _static_url("avatars/avatar_06.jpg"),
            _static_url("avatars/avatar_12.jpg"),
        ],
        "tags": ["化妆", "技能教学", "可上门"],
        "signature": "可做妆造，也能带零基础学简单日常妆和拍照动作。",
        "gender": "女",
        "region": "海南 三亚",
        "job_title": "妆造 / 技能教学",
        "education": "形象设计",
        "birth_decade": "00后",
        "spoken_languages": ["普通话"],
        "living_city": "三亚",
        "living_country": "中国",
        "identity_verified": True,
        "is_phone_verified": True,
        "is_profile_completed": True,
    },
]


SERVICE_SEEDS = [
    {
        "id": "seed-service-guide-01",
        "creator_id": "seed-user-01",
        "creator": "阿宁",
        "title": "亚龙湾轻松地陪半日线",
        "description": "适合第一次来三亚的用户，含路线建议、拍照点位和用餐安排，节奏不赶。",
        "cover_image_url": _static_url("services/seed-user-01/sanya_service_001.jpg"),
        "image_urls": [
            _static_url("services/seed-user-01/sanya_service_001.jpg"),
            _static_url("services/seed-user-01/sanya_service_002.jpg"),
            _static_url("services/seed-user-01/sanya_service_003.jpg"),
        ],
        "location": "三亚",
        "price_text": "398元",
        "price_basis_text": "任意4小时",
        "prepayment_percent": 30,
        "full_refund_cancel_lead_days": 1,
        "category": "地陪",
        "service_mode": "小时",
        "booking_time_ranges_json": '[{"start":"09:00","end":"22:00"}]',
        "booking_lead_hours": 6.0,
        "booking_future_open_days": 30,
        "auto_accept_after_payment": True,
        "sync_to_square": True,
        "service_declarations_extra": ["景区门票需自理", "默认 2 人内，更多人数请先沟通"],
        "service_feature_tags": ["熟悉路线", "会安排行程", "会拍照"],
        "service_extra_fee_tags": ["门票", "打车费"],
        "participant_ids": ["seed-user-01"],
        "is_important": True,
        "is_draft": False,
        "is_deleted": False,
    },
    {
        "id": "seed-service-photo-01",
        "creator_id": "seed-user-02",
        "creator": "小岛",
        "title": "椰梦长廊情侣旅拍",
        "description": "主打日落氛围感和轻纪实，底片全送，精修 9 张，适合情侣和闺蜜。",
        "cover_image_url": _static_url("services/seed-user-01/sanya_service_101.jpg"),
        "image_urls": [
            _static_url("services/seed-user-01/sanya_service_101.jpg"),
            _static_url("services/seed-user-01/sanya_service_102.jpg"),
            _static_url("services/seed-user-01/sanya_service_103.jpg"),
        ],
        "location": "三亚",
        "price_text": "699元",
        "price_basis_text": "每次（不超过2小时）",
        "prepayment_percent": 50,
        "full_refund_cancel_lead_days": 2,
        "category": "摄影",
        "service_mode": "次",
        "booking_time_ranges_json": '[{"start":"15:00","end":"20:00"}]',
        "booking_lead_hours": 12.0,
        "booking_future_open_days": 45,
        "auto_accept_after_payment": False,
        "sync_to_square": True,
        "service_declarations_extra": ["精修风格可提前沟通", "如需加急返图请单独确认"],
        "service_feature_tags": ["审美在线", "底片全送", "夜景擅长"],
        "service_extra_fee_tags": ["场地费", "加急修图费"],
        "participant_ids": ["seed-user-02"],
        "is_important": True,
        "is_draft": False,
        "is_deleted": False,
    },
    {
        "id": "seed-service-dj-01",
        "creator_id": "seed-user-03",
        "creator": "Mika",
        "title": "生日派对 DJ 气氛组",
        "description": "含暖场互动与歌单定制，小型派对、生日局、包场预热都可接。",
        "cover_image_url": _static_url("services/seed-user-01/sanya_service_201.jpg"),
        "image_urls": [
            _static_url("services/seed-user-01/sanya_service_201.jpg"),
            _static_url("services/seed-user-01/sanya_service_202.jpg"),
        ],
        "location": "三亚",
        "price_text": "1280元",
        "price_basis_text": "每晚（晚上10点前）",
        "prepayment_percent": 40,
        "full_refund_cancel_lead_days": 3,
        "category": "DJ气氛组",
        "service_mode": "晚",
        "booking_time_ranges_json": '[{"start":"18:00","end":"23:00"}]',
        "booking_lead_hours": 24.0,
        "booking_future_open_days": 60,
        "auto_accept_after_payment": False,
        "sync_to_square": True,
        "service_declarations_extra": ["设备进场时间需提前确认"],
        "service_feature_tags": ["氛围带动", "曲风可定制", "派对经验"],
        "service_extra_fee_tags": ["设备运输费", "超时费"],
        "participant_ids": ["seed-user-03"],
        "is_important": False,
        "is_draft": False,
        "is_deleted": False,
    },
    {
        "id": "seed-service-fitness-01",
        "creator_id": "seed-user-04",
        "creator": "Tina",
        "title": "海边晨练减脂私教",
        "description": "一对一晨练课程，适合零基础，含动作纠正和简单饮食建议。",
        "cover_image_url": _static_url("services/sanya_coach.jpg"),
        "image_urls": [
            _static_url("services/sanya_coach.jpg"),
            _static_url("services/seed-user-02/sanya_service_006.jpg"),
        ],
        "location": "三亚",
        "price_text": "299元",
        "price_basis_text": "每小时",
        "prepayment_percent": 30,
        "full_refund_cancel_lead_days": 1,
        "category": "运动教练",
        "service_mode": "小时",
        "booking_time_ranges_json": '[{"start":"06:30","end":"10:30"}]',
        "booking_lead_hours": 8.0,
        "booking_future_open_days": 30,
        "auto_accept_after_payment": True,
        "sync_to_square": True,
        "service_declarations_extra": ["训练强度会根据体能调整"],
        "service_feature_tags": ["纠正动作", "减脂塑形", "新手友好"],
        "service_extra_fee_tags": ["场地费", "超时费"],
        "participant_ids": ["seed-user-04"],
        "is_important": False,
        "is_draft": False,
        "is_deleted": False,
    },
    {
        "id": "seed-service-chef-01",
        "creator_id": "seed-user-05",
        "creator": "Kiki",
        "title": "4人份海鲜家宴私厨",
        "description": "上门采购和制作，适合家庭聚餐或朋友小聚，可按忌口调整菜单。",
        "cover_image_url": _static_url("services/sanya_chef.jpg"),
        "image_urls": [
            _static_url("services/sanya_chef.jpg"),
            _static_url("services/seed-user-02/sanya_service_107.jpg"),
        ],
        "location": "三亚",
        "price_text": "880元",
        "price_basis_text": "每次",
        "prepayment_percent": 35,
        "full_refund_cancel_lead_days": 2,
        "category": "私厨",
        "service_mode": "次",
        "booking_time_ranges_json": '[{"start":"10:00","end":"20:00"}]',
        "booking_lead_hours": 24.0,
        "booking_future_open_days": 30,
        "auto_accept_after_payment": False,
        "sync_to_square": True,
        "service_declarations_extra": ["默认不含食材费", "需可正常使用厨房"],
        "service_feature_tags": ["会买菜", "可定制菜单", "上门制作"],
        "service_extra_fee_tags": ["食材费", "交通费"],
        "participant_ids": ["seed-user-05"],
        "is_important": False,
        "is_draft": False,
        "is_deleted": False,
    },
    {
        "id": "seed-service-makeup-01",
        "creator_id": "seed-user-06",
        "creator": "Luna",
        "title": "海岛度假轻透妆造",
        "description": "适合旅拍、约会和晚餐局，支持上门，妆面偏自然干净。",
        "cover_image_url": _static_url("services/sanya_makeup.jpg"),
        "image_urls": [
            _static_url("services/sanya_makeup.jpg"),
            _static_url("services/seed-user-02/sanya_service_108.jpg"),
        ],
        "location": "三亚",
        "price_text": "368元",
        "price_basis_text": "每次",
        "prepayment_percent": 30,
        "full_refund_cancel_lead_days": 1,
        "category": "化妆",
        "service_mode": "次",
        "booking_time_ranges_json": '[{"start":"08:00","end":"21:00"}]',
        "booking_lead_hours": 4.0,
        "booking_future_open_days": 21,
        "auto_accept_after_payment": True,
        "sync_to_square": True,
        "service_declarations_extra": ["如需试妆请提前说明"],
        "service_feature_tags": ["自然妆感", "可上门", "证件照妆"],
        "service_extra_fee_tags": ["上门费", "早妆费"],
        "participant_ids": ["seed-user-06"],
        "is_important": False,
        "is_draft": False,
        "is_deleted": False,
    },
    {
        "id": "seed-service-teaching-01",
        "creator_id": "seed-user-06",
        "creator": "Luna",
        "title": "拍照动作和表情管理陪练",
        "description": "适合不会面对镜头的人，含动作示范、表情引导和现场复盘。",
        "cover_image_url": _static_url("services/sanya_photo.jpg"),
        "image_urls": [
            _static_url("services/sanya_photo.jpg"),
            _static_url("services/seed-user-02/sanya_service_109.jpg"),
        ],
        "location": "三亚",
        "price_text": "220元",
        "price_basis_text": "每小时",
        "prepayment_percent": 20,
        "full_refund_cancel_lead_days": 1,
        "category": "技能教学",
        "service_mode": "小时",
        "booking_time_ranges_json": '[{"start":"10:00","end":"18:00"}]',
        "booking_lead_hours": 2.0,
        "booking_future_open_days": 14,
        "auto_accept_after_payment": True,
        "sync_to_square": True,
        "service_declarations_extra": ["建议自带想模仿的参考照片"],
        "service_feature_tags": ["零基础友好", "一对一", "可陪练"],
        "service_extra_fee_tags": ["场地费"],
        "participant_ids": ["seed-user-06"],
        "is_important": False,
        "is_draft": False,
        "is_deleted": False,
    },
    {
        "id": "seed-service-other-01",
        "creator_id": "seed-user-01",
        "creator": "阿宁",
        "title": "海边求婚流程协助",
        "description": "帮忙踩点、卡时间、安排动线和拍照位，适合简单仪式和惊喜策划。",
        "cover_image_url": _static_url("services/sanya_misc.jpg"),
        "image_urls": [
            _static_url("services/sanya_misc.jpg"),
            _static_url("services/seed-user-01/sanya_service_304.jpg"),
        ],
        "location": "三亚",
        "price_text": "520元",
        "price_basis_text": "每次（不超过3小时）",
        "prepayment_percent": 30,
        "full_refund_cancel_lead_days": 2,
        "category": "其他服务",
        "service_mode": "次",
        "booking_time_ranges_json": '[{"start":"09:00","end":"22:00"}]',
        "booking_lead_hours": 12.0,
        "booking_future_open_days": 45,
        "auto_accept_after_payment": False,
        "sync_to_square": True,
        "service_declarations_extra": ["现场布置费用按实际另计"],
        "service_feature_tags": ["沟通顺畅", "可定制", "长期可约"],
        "service_extra_fee_tags": ["材料费", "交通费"],
        "participant_ids": ["seed-user-01", "seed-user-02"],
        "is_important": False,
        "is_draft": False,
        "is_deleted": False,
    },
    {
        "id": "seed-service-draft-01",
        "creator_id": "seed-user-01",
        "creator": "阿宁",
        "title": "蜈支洲一日轻安排草稿",
        "description": "草稿示例：还在补充集合方式、可约人数和交通建议。",
        "cover_image_url": _static_url("services/seed-user-01/sanya_service_401.jpg"),
        "image_urls": [
            _static_url("services/seed-user-01/sanya_service_401.jpg"),
            _static_url("services/seed-user-01/sanya_service_402.jpg"),
        ],
        "location": "三亚",
        "price_text": "待定",
        "price_basis_text": "",
        "prepayment_percent": 30,
        "full_refund_cancel_lead_days": 1,
        "category": "地陪",
        "service_mode": "天",
        "booking_time_ranges_json": "[]",
        "booking_lead_hours": 0.0,
        "booking_future_open_days": 30,
        "auto_accept_after_payment": False,
        "sync_to_square": False,
        "service_declarations_extra": ["草稿暂未公开"],
        "service_feature_tags": ["会安排行程"],
        "service_extra_fee_tags": [],
        "participant_ids": ["seed-user-01"],
        "is_important": False,
        "is_draft": True,
        "is_deleted": False,
    },
    {
        "id": "seed-service-offline-01",
        "creator_id": "seed-user-02",
        "creator": "小岛",
        "title": "已下架示例：清晨跟拍档期结束",
        "description": "用于测试下架记录持久化，不应出现在发现流。",
        "cover_image_url": _static_url("services/seed-user-01/sanya_service_403.jpg"),
        "image_urls": [
            _static_url("services/seed-user-01/sanya_service_403.jpg"),
        ],
        "location": "三亚",
        "price_text": "499元",
        "price_basis_text": "每次",
        "prepayment_percent": 30,
        "full_refund_cancel_lead_days": 1,
        "category": "摄影",
        "service_mode": "次",
        "booking_time_ranges_json": "[]",
        "booking_lead_hours": 0.0,
        "booking_future_open_days": 30,
        "auto_accept_after_payment": True,
        "sync_to_square": False,
        "service_declarations_extra": [],
        "service_feature_tags": ["当日返图"],
        "service_extra_fee_tags": [],
        "participant_ids": ["seed-user-02"],
        "is_important": False,
        "is_draft": False,
        "is_deleted": True,
    },
]


EXPERIENCE_SEEDS = [
    {
        "id": "seed-experience-01",
        "host_id": "seed-user-01",
        "host_name": "阿宁",
        "title": "三亚湾日落散步局",
        "description": "轻松看海、聊天和拍照，适合第一次落地三亚的松弛体验。",
        "cover_image_url": _static_url("experiences/seed-user-01/sanya_experience_001.jpg"),
        "image_urls": [
            _static_url("experiences/seed-user-01/sanya_experience_001.jpg"),
            _static_url("experiences/seed-user-01/sanya_experience_002.jpg"),
        ],
        "location": "三亚",
        "price_text": "168元",
        "price_basis_text": "每人",
        "category": "城市漫游",
        "duration_text": "约2小时",
        "badge_text": "轻松入门",
        "tags": ["海边", "拍照", "松弛感"],
    },
    {
        "id": "seed-experience-02",
        "host_id": "seed-user-02",
        "host_name": "小岛",
        "title": "椰林胶片感街拍体验",
        "description": "边走边拍，适合情侣和闺蜜，帮助快速进入镜头状态。",
        "cover_image_url": _static_url("experiences/seed-user-02/sanya_experience_008.jpg"),
        "image_urls": [
            _static_url("experiences/seed-user-02/sanya_experience_008.jpg"),
            _static_url("experiences/seed-user-02/sanya_experience_009.jpg"),
        ],
        "location": "三亚",
        "price_text": "199元",
        "price_basis_text": "每人",
        "category": "拍照体验",
        "duration_text": "约90分钟",
        "badge_text": "热门",
        "tags": ["旅拍", "情侣", "氛围感"],
    },
    {
        "id": "seed-experience-03",
        "host_id": "seed-user-03",
        "host_name": "Mika",
        "title": "海边微醺歌单共创",
        "description": "一起听歌、选歌和聊活动氛围，适合小型聚会前热身。",
        "cover_image_url": _static_url("experiences/seed-user-03/sanya_experience_015.jpg"),
        "image_urls": [
            _static_url("experiences/seed-user-03/sanya_experience_015.jpg"),
            _static_url("experiences/seed-user-03/sanya_experience_016.jpg"),
        ],
        "location": "三亚",
        "price_text": "129元",
        "price_basis_text": "每人",
        "category": "派对体验",
        "duration_text": "约2小时",
        "badge_text": "夜生活",
        "tags": ["音乐", "派对", "社交"],
    },
    {
        "id": "seed-experience-04",
        "host_id": "seed-user-04",
        "host_name": "Tina",
        "title": "清晨海边拉伸唤醒",
        "description": "日出前后进行轻量拉伸和呼吸练习，体验感轻，不卷强度。",
        "cover_image_url": _static_url("experiences/seed-user-04/sanya_experience_022.jpg"),
        "image_urls": [
            _static_url("experiences/seed-user-04/sanya_experience_022.jpg"),
            _static_url("experiences/seed-user-04/sanya_experience_023.jpg"),
        ],
        "location": "三亚",
        "price_text": "88元",
        "price_basis_text": "每人",
        "category": "运动体验",
        "duration_text": "约1小时",
        "badge_text": "清晨推荐",
        "tags": ["日出", "拉伸", "治愈"],
    },
    {
        "id": "seed-experience-05",
        "host_id": "seed-user-05",
        "host_name": "Kiki",
        "title": "海鲜市场挑食材体验",
        "description": "带你逛市场、挑食材、讲简单做法，适合喜欢本地生活感的人。",
        "cover_image_url": _static_url("experiences/seed-user-05/sanya_experience_029.jpg"),
        "image_urls": [
            _static_url("experiences/seed-user-05/sanya_experience_029.jpg"),
            _static_url("experiences/seed-user-05/sanya_experience_030.jpg"),
        ],
        "location": "三亚",
        "price_text": "108元",
        "price_basis_text": "每人",
        "category": "城市生活",
        "duration_text": "约2小时",
        "badge_text": "本地感",
        "tags": ["逛市场", "美食", "烟火气"],
    },
    {
        "id": "seed-experience-06",
        "host_id": "seed-user-06",
        "host_name": "Luna",
        "title": "度假妆造快速入门",
        "description": "适合不会化妆的新手，现场演示清透底妆和拍照更上镜的小技巧。",
        "cover_image_url": _static_url("experiences/seed-user-06/sanya_experience_036.jpg"),
        "image_urls": [
            _static_url("experiences/seed-user-06/sanya_experience_036.jpg"),
            _static_url("experiences/seed-user-06/sanya_experience_037.jpg"),
        ],
        "location": "三亚",
        "price_text": "118元",
        "price_basis_text": "每人",
        "category": "技能体验",
        "duration_text": "约90分钟",
        "badge_text": "新手友好",
        "tags": ["妆造", "拍照", "陪练"],
    },
]


def _upsert_record(db: Session, model_class, record_id: str, payload: dict):
    normalized_payload = {key: value for key, value in payload.items() if key != "id"}
    item = db.query(model_class).filter(model_class.id == record_id).first()
    if item is None:
        item = model_class(id=record_id, **normalized_payload)
        db.add(item)
        return item
    for key, value in normalized_payload.items():
        setattr(item, key, value)
    return item


def _seed_users(db: Session, now_ms: int) -> None:
    for index, payload in enumerate(USER_SEEDS):
        created_at = now_ms - (30 + index) * 86_400_000
        item = _upsert_record(
            db,
            models.User,
            payload["id"],
            {
                **payload,
                "favorite_service_ids": [],
                "review_count": 0,
                "average_rating": 0.0,
                "service_years": 0,
                "review_summaries": [],
                "memo": "",
                "remark_name": "",
                "height_cm": 0,
                "weight_kg": 0.0,
                "height_weight_private": False,
                "middle_school_favorite_song": "",
                "address_detail": "",
                "address_recipient_name": "",
                "address_phone_number": payload["phone_number"],
                "id_modification_count": 0,
                "last_id_modification_year": 0,
                "created_at": created_at,
                "updated_at": now_ms - index * 60_000,
                "is_synced": True,
            },
        )
        if not item.hashed_password:
            item.hashed_password = _hash_seed_password()


def _seed_services(db: Session, now_ms: int) -> None:
    for index, payload in enumerate(SERVICE_SEEDS):
        created_at = now_ms - (10 + index) * 3_600_000
        _upsert_record(
            db,
            models.Service,
            payload["id"],
            {
                **payload,
                "created_at": created_at,
                "updated_at": created_at + 1_800_000,
                "is_synced": True,
            },
        )


def _seed_experiences(db: Session, now_ms: int) -> None:
    for index, payload in enumerate(EXPERIENCE_SEEDS):
        created_at = now_ms - (20 + index) * 7_200_000
        _upsert_record(
            db,
            models.Experience,
            payload["id"],
            {
                **payload,
                "created_at": created_at,
                "updated_at": created_at + 1_200_000,
                "is_deleted": False,
                "is_synced": True,
            },
        )


def _seed_wishlist_profile(db: Session, now_ms: int) -> None:
    favorite_ids = [
        "seed-service-guide-01",
        "seed-service-photo-01",
        "seed-service-dj-01",
    ]
    user = db.query(models.User).filter(models.User.id == "seed-user-01").first()
    if user is None:
        return
    user.favorite_service_ids = favorite_ids
    user.updated_at = now_ms

    db.query(models.WishlistGroupItem).filter(
        models.WishlistGroupItem.user_id == user.id
    ).delete(synchronize_session=False)
    db.query(models.WishlistGroup).filter(
        models.WishlistGroup.user_id == user.id
    ).delete(synchronize_session=False)

    groups = [
        ("默认分组", ["seed-service-guide-01"]),
        ("旅拍收藏", ["seed-service-photo-01"]),
        ("派对备选", ["seed-service-dj-01"]),
    ]
    for index, (name, service_ids) in enumerate(groups):
        group_id = f"seed-wishlist-group-{index + 1:02d}"
        db.add(
            models.WishlistGroup(
                id=group_id,
                user_id=user.id,
                name=name,
                sort_order=index,
                created_at=now_ms,
                updated_at=now_ms,
            )
        )
        for item_index, service_id in enumerate(service_ids):
            db.add(
                models.WishlistGroupItem(
                    id=f"seed-wishlist-item-{index + 1:02d}-{item_index + 1:02d}",
                    user_id=user.id,
                    group_id=group_id,
                    service_id=service_id,
                    created_at=now_ms,
                    updated_at=now_ms,
                )
            )


def ensure_seed_data(db: Session) -> None:
    now_ms = _now_ms()
    _seed_users(db, now_ms)
    _seed_services(db, now_ms)
    _seed_experiences(db, now_ms)
    _seed_wishlist_profile(db, now_ms)
    db.commit()
