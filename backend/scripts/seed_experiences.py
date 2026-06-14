import json
import shutil
import sqlite3
import time
from pathlib import Path


BASE_DIR = Path(__file__).resolve().parents[1]
DB_PATH = BASE_DIR / "isanya.db"
SERVICE_DIR = BASE_DIR / "static" / "services"
EXPERIENCE_DIR = BASE_DIR / "static" / "experiences"
TOTAL_IMAGES = 200


EXPERIENCE_SEED_DATA = [
    {
        "category": "必游经典景区",
        "title": "蜈支洲岛环岛轻奢一日体验",
        "location": "海南省三亚市 海棠区 蜈支洲岛旅游区",
        "duration_text": "8小时",
        "badge_text": "人气打卡",
        "price_text": "¥698/位",
        "price_basis_text": "含船票接驳与岛内讲解",
        "summary": "主打蜈支洲经典打卡和轻松海岛节奏，适合第一次来三亚的游客。",
        "route": "早班船上岛，环岛观景台、情人桥、白沙滩和珊瑚酒店外景依次串联。",
        "includes": "含往返接驳、拍照点位建议、岛内节奏控时和午餐推荐。",
        "tips": "建议穿浅色速干服与凉鞋，避开正午暴晒，便于在海边和礁石区活动。",
        "tags": ["蜈支洲岛", "海岛拍照", "轻奢慢游", "第一次来三亚"],
    },
    {
        "category": "必游经典景区",
        "title": "亚龙湾热带天堂森林公园云端观景",
        "location": "海南省三亚市 吉阳区 亚龙湾热带天堂森林公园",
        "duration_text": "5小时",
        "badge_text": "山海视野",
        "price_text": "¥428/位",
        "price_basis_text": "含景区动线规划与观景拍照",
        "summary": "在山海之间看亚龙湾全景，适合情侣、闺蜜和亲子轻徒步。",
        "route": "从兰花谷入口上山，串联雨林步道、玻璃栈桥、山顶观景台和电影同款机位。",
        "includes": "含步道节奏带领、观景机位建议、接驳衔接与下山咖啡休憩推荐。",
        "tips": "山上湿度较高，建议备防蚊喷雾和薄外套，午后云雾更适合拍层次感照片。",
        "tags": ["亚龙湾", "森林公园", "雨林", "观景台"],
    },
    {
        "category": "必游经典景区",
        "title": "鹿回头半岛日落巡游体验",
        "location": "海南省三亚市 吉阳区 鹿回头风景区",
        "duration_text": "4小时",
        "badge_text": "日落限定",
        "price_text": "¥299/位",
        "price_basis_text": "含接送与日落点位安排",
        "summary": "以鹿回头山海日落为核心，适合想拍到城市海岸线与晚霞同框的人群。",
        "route": "下午上山避开旅行团高峰，依次走观海平台、雕塑广场、山顶灯火视角。",
        "includes": "含停车接驳、日落时刻提醒、观景路线安排和夜景补拍建议。",
        "tips": "建议傍晚前到场，晚霞后的蓝调时刻是最容易拍出层次的时间段。",
        "tags": ["鹿回头", "日落", "城市夜景", "半岛风光"],
    },
    {
        "category": "必游经典景区",
        "title": "南山文化旅游区晨游礼佛线",
        "location": "海南省三亚市 崖州区 南山文化旅游区",
        "duration_text": "6小时",
        "badge_text": "静心路线",
        "price_text": "¥368/位",
        "price_basis_text": "含园区动线与素斋建议",
        "summary": "主打早入园、低拥挤、沉浸式感受海上观音与园区禅意氛围。",
        "route": "从南山寺外围慢行入园，串联海上观音、佛教文化苑与素斋午餐时段。",
        "includes": "含静心路线规划、拍照避人时段建议、用餐与休息点位推荐。",
        "tips": "建议着装得体并备遮阳帽，早入园体感更舒适，也更容易拍到空镜。",
        "tags": ["南山寺", "海上观音", "礼佛", "晨游"],
    },
    {
        "category": "必游经典景区",
        "title": "西岛渔村白墙慢游与码头观海",
        "location": "海南省三亚市 天涯区 西岛旅游区",
        "duration_text": "6小时",
        "badge_text": "渔村漫步",
        "price_text": "¥458/位",
        "price_basis_text": "含上岛船票与渔村慢游路线",
        "summary": "把西岛的渔村生活、白墙彩巷和码头海景连成一条好逛好拍的慢游线。",
        "route": "上午上岛先走渔村巷弄，再去牛王岭海边、珊瑚墙和彩色店铺打卡。",
        "includes": "含上岛动线安排、避开人潮的巷口拍照点和码头用餐推荐。",
        "tips": "岛上路面有坡度，建议穿舒适平底鞋，午后海风较大可备薄衫。",
        "tags": ["西岛", "渔村", "白墙", "海边慢游"],
    },
    {
        "category": "必游经典景区",
        "title": "椰梦长廊到天涯小镇海岸半日游",
        "location": "海南省三亚市 天涯区 椰梦长廊与天涯小镇",
        "duration_text": "5小时",
        "badge_text": "海岸轻松逛",
        "price_text": "¥238/位",
        "price_basis_text": "含海岸交通衔接与机位规划",
        "summary": "把三亚湾海岸线和天涯小镇彩色街景串联起来，适合轻度步行体验。",
        "route": "从椰梦长廊海边木栈道开始，接着前往天涯小镇主街和巷内彩墙区域。",
        "includes": "含日落时段建议、街拍机位安排和返程交通建议。",
        "tips": "傍晚海风舒适，适合拍逆光剪影，建议带一双方便下沙滩的鞋。",
        "tags": ["椰梦长廊", "天涯小镇", "海岸线", "日落巡游"],
    },
    {
        "category": "潜水冲浪跳伞",
        "title": "后海村冲浪入门小班体验",
        "location": "海南省三亚市 海棠区 后海村",
        "duration_text": "3.5小时",
        "badge_text": "新手友好",
        "price_text": "¥468/位",
        "price_basis_text": "含板具、防磨衣和教练陪同",
        "summary": "适合零基础游客，从陆地讲解到下水起乘都有完整节奏。",
        "route": "先做岸上安全讲解与站板练习，再在后海内湾进行分组下水体验。",
        "includes": "含冲浪板、防磨衣、基础摄影记录和浪况评估。",
        "tips": "建议提前做防晒并避开饱餐后下水，初学者以退潮前后浪型更稳定。",
        "tags": ["后海冲浪", "零基础", "小班教学", "海边运动"],
    },
    {
        "category": "潜水冲浪跳伞",
        "title": "蜈支洲双船点体验潜水",
        "location": "海南省三亚市 海棠区 蜈支洲岛海域",
        "duration_text": "4小时",
        "badge_text": "玻璃海潜水",
        "price_text": "¥980/位",
        "price_basis_text": "含体验潜装备与教练带潜",
        "summary": "重点体验三亚高透明度海域，适合首次尝试水肺潜水的人群。",
        "route": "装备试穿后乘船到指定点位，完成岸上说明、浅水适应和正式下潜。",
        "includes": "含潜水装备、带潜教练、简短视频记录与淡水冲洗。",
        "tips": "前一晚避免饮酒，体验前保持充足睡眠，下水前不要涂太厚面霜。",
        "tags": ["潜水", "蜈支洲", "水肺", "玻璃海"],
    },
    {
        "category": "潜水冲浪跳伞",
        "title": "海棠湾海上拖伞高空环海",
        "location": "海南省三亚市 海棠区 海棠湾",
        "duration_text": "2小时",
        "badge_text": "高空海景",
        "price_text": "¥560/位",
        "price_basis_text": "含拖伞项目与安全护具",
        "summary": "从海上拉升俯瞰海棠湾海岸线，刺激程度适中，拍照出片率高。",
        "route": "码头集合后乘艇出海，完成设备穿戴与升空体验，返航看湾区全景。",
        "includes": "含海上快艇接送、安全装备和项目保险建议。",
        "tips": "建议轻装上阵，手机可交由船员代拍，风力偏大时会顺延体验时段。",
        "tags": ["拖伞", "海棠湾", "高空", "刺激体验"],
    },
    {
        "category": "潜水冲浪跳伞",
        "title": "亚龙湾自由潜水感知体验",
        "location": "海南省三亚市 吉阳区 亚龙湾",
        "duration_text": "4小时",
        "badge_text": "呼吸训练",
        "price_text": "¥688/位",
        "price_basis_text": "含呼吸课与水下适应训练",
        "summary": "以呼吸、放松和水感建立为核心，适合想尝试自由潜但不追求深度的人。",
        "route": "先在浅滩完成呼吸与平压基础，再到近岸清澈水域练习入水和漂浮。",
        "includes": "含面镜脚蹼、浮球、安全陪伴和水下体验照。",
        "tips": "自由潜更看重放松感，体验前一天不要熬夜，尽量保持轻松状态。",
        "tags": ["自由潜", "亚龙湾", "呼吸训练", "水感建立"],
    },
    {
        "category": "潜水冲浪跳伞",
        "title": "西岛珊瑚湾浮潜与海下观察",
        "location": "海南省三亚市 天涯区 西岛珊瑚湾",
        "duration_text": "3小时",
        "badge_text": "亲海轻体验",
        "price_text": "¥358/位",
        "price_basis_text": "含浮潜装备与岸边陪同",
        "summary": "以低门槛近海浮潜为主，适合情侣和亲子游客感受海下世界。",
        "route": "先做装备佩戴和呼吸适应，再进入浅海区域观察珊瑚与热带鱼群。",
        "includes": "含面镜呼吸管、防滑鞋和浅海陪同。",
        "tips": "近海礁石较多，下水请跟随教练路线，避免踩踏珊瑚区域。",
        "tags": ["浮潜", "西岛", "轻松入门", "珊瑚观察"],
    },
    {
        "category": "潜水冲浪跳伞",
        "title": "凤凰岛直升机观海短航线",
        "location": "海南省三亚市 天涯区 凤凰岛附近海域",
        "duration_text": "1.5小时",
        "badge_text": "空中俯瞰",
        "price_text": "¥1280/位",
        "price_basis_text": "含短航线飞行与地面安全说明",
        "summary": "适合想快速收获三亚湾海岸线空中视角与高辨识度旅拍内容的人。",
        "route": "地面简报后起飞，沿三亚湾、凤凰岛和城市海岸线进行短航线观景。",
        "includes": "含登机说明、短航拍照建议和乘机协助。",
        "tips": "建议提前到场完成证件核验，起飞时间受天气与空域调度影响较大。",
        "tags": ["直升机", "凤凰岛", "航拍视角", "高端体验"],
    },
    {
        "category": "海面运动",
        "title": "三亚湾落日帆船微醺巡航",
        "location": "海南省三亚市 天涯区 三亚湾海域",
        "duration_text": "2.5小时",
        "badge_text": "落日航海",
        "price_text": "¥520/位",
        "price_basis_text": "含帆船与基础饮品",
        "summary": "一边看三亚湾落日一边体验帆船航行，适合情侣和好友小团。",
        "route": "傍晚前出海，沿海岸线巡航，寻找最佳夕阳角度并返航看城市灯光。",
        "includes": "含帆船船员服务、基础饮品和海上拍照建议。",
        "tips": "傍晚海面风感明显，建议携带薄外套，手机注意防水防风沙。",
        "tags": ["帆船", "三亚湾", "落日", "轻微醺"],
    },
    {
        "category": "海面运动",
        "title": "海棠湾摩托艇穿浪体验",
        "location": "海南省三亚市 海棠区 海棠湾",
        "duration_text": "2小时",
        "badge_text": "速度感",
        "price_text": "¥420/位",
        "price_basis_text": "含摩托艇项目与安全装备",
        "summary": "主打海面速度和浪间转向，适合喜欢刺激但不想项目过长的人。",
        "route": "码头集结后完成安全说明，沿固定海域线路进行单人或双人体验。",
        "includes": "含救生衣、项目驾驶指引和基础岸拍记录。",
        "tips": "建议穿速干衣物，项目结束后可直接在岸边冲洗更衣。",
        "tags": ["摩托艇", "海棠湾", "速度", "穿浪"],
    },
    {
        "category": "海面运动",
        "title": "皇后湾晨光桨板巡游",
        "location": "海南省三亚市 海棠区 皇后湾",
        "duration_text": "3小时",
        "badge_text": "晨光治愈",
        "price_text": "¥399/位",
        "price_basis_text": "含桨板装备与随队教练",
        "summary": "在海面最平静的时间段出发，适合拍晨光、水面倒影和轻量运动感。",
        "route": "日出前后集合，完成岸上基础动作训练后沿皇后湾浅海慢速巡游。",
        "includes": "含桨板、救生衣、防水包与跟拍建议。",
        "tips": "晨场更适合新手，记得带换洗衣物和装手机的小型防水袋。",
        "tags": ["桨板", "皇后湾", "日出", "治愈系"],
    },
    {
        "category": "海面运动",
        "title": "亚龙湾双体游艇半日海上派对",
        "location": "海南省三亚市 吉阳区 亚龙湾海域",
        "duration_text": "4.5小时",
        "badge_text": "海上社交",
        "price_text": "¥880/位",
        "price_basis_text": "含游艇、果盘与基础海上项目",
        "summary": "更偏年轻社交和轻派对氛围，适合朋友组队出海拍照放松。",
        "route": "从亚龙湾游艇码头出海，停靠风平浪静海域，安排拍照、海上玩具和小食时间。",
        "includes": "含双体船、果盘饮料、海上休闲项目和基础音响氛围。",
        "tips": "建议轻便穿搭与备用泳装，海上日照强烈，防晒需要提前一小时完成。",
        "tags": ["游艇", "亚龙湾", "派对", "朋友局"],
    },
    {
        "category": "海面运动",
        "title": "崖州近海海钓轻体验",
        "location": "海南省三亚市 崖州区 近海海域",
        "duration_text": "4小时",
        "badge_text": "老少皆宜",
        "price_text": "¥498/位",
        "price_basis_text": "含钓具与船只出海",
        "summary": "主打轻松出海和基础海钓体验，不强调渔获成绩，更注重过程感。",
        "route": "清晨出海到近海点位，完成抛竿教学后进行轮换式海钓体验。",
        "includes": "含船只、钓具、基础饵料和船上海钓指导。",
        "tips": "建议吃好早餐并提前准备晕船贴，清晨海面状态通常更平稳。",
        "tags": ["海钓", "崖州", "出海", "轻体验"],
    },
    {
        "category": "海面运动",
        "title": "三亚湾透明皮划艇海上拍照",
        "location": "海南省三亚市 天涯区 三亚湾",
        "duration_text": "2.5小时",
        "badge_text": "拍照友好",
        "price_text": "¥338/位",
        "price_basis_text": "含透明艇与简易跟拍",
        "summary": "适合想要轻运动加高出片率的人，透明艇在海上拍照辨识度很高。",
        "route": "先在岸边练习平衡和划桨，再到近岸平静海域完成拍照与巡游。",
        "includes": "含透明皮划艇、救生衣和基础岸拍协助。",
        "tips": "建议选择浅色服装，照片会更干净，风浪较大时会改为近岸短线体验。",
        "tags": ["透明艇", "三亚湾", "皮划艇", "出片"],
    },
    {
        "category": "文化探索",
        "title": "崖州古城城墙慢走与街巷寻味",
        "location": "海南省三亚市 崖州区 崖州古城",
        "duration_text": "4小时",
        "badge_text": "古城漫游",
        "price_text": "¥268/位",
        "price_basis_text": "含古城讲解与本地小吃建议",
        "summary": "更适合喜欢街巷气息和历史故事的人，节奏从容，适合慢慢感受。",
        "route": "沿古城城墙旧址、骑楼街巷和老店铺穿行，穿插本地风味小吃停留。",
        "includes": "含街区讲解、打卡动线与老店点单建议。",
        "tips": "建议穿舒适步行鞋，古城内适合边走边拍，午后人流更舒缓。",
        "tags": ["崖州古城", "街巷", "本地风味", "慢走"],
    },
    {
        "category": "文化探索",
        "title": "中廖村田园咖啡与村落采风",
        "location": "海南省三亚市 吉阳区 中廖村",
        "duration_text": "4.5小时",
        "badge_text": "乡野松弛",
        "price_text": "¥288/位",
        "price_basis_text": "含村落走读与咖啡休憩建议",
        "summary": "主打离海岸线稍远的乡村慢生活感，适合想看不一样三亚的人。",
        "route": "从稻田边与村口装置开始，走读特色小院、咖啡馆与农作空间。",
        "includes": "含村落路线安排、店铺推荐和乡野拍照机位建议。",
        "tips": "午后树荫多、光线柔和，更适合村落散步和轻街拍。",
        "tags": ["中廖村", "田园", "咖啡", "乡村采风"],
    },
    {
        "category": "文化探索",
        "title": "黎苗织锦手作体验半日课",
        "location": "海南省三亚市 吉阳区 黎苗文化体验点",
        "duration_text": "3小时",
        "badge_text": "手作非遗",
        "price_text": "¥358/位",
        "price_basis_text": "含材料包与老师指导",
        "summary": "适合家庭、亲子和对海南本土文化感兴趣的游客，沉浸感强。",
        "route": "先听黎苗纹样故事，再进行配色、简易编织或手作纪念物制作。",
        "includes": "含材料包、老师示范和可带走的小件作品。",
        "tips": "课程节奏温和，适合避开午后暴晒时段，也适合作为雨天备选方案。",
        "tags": ["黎苗文化", "织锦", "手作", "非遗体验"],
    },
    {
        "category": "文化探索",
        "title": "青塘渔港赶海与渔市观察",
        "location": "海南省三亚市 崖州区 青塘渔港",
        "duration_text": "4小时",
        "badge_text": "本地生活",
        "price_text": "¥318/位",
        "price_basis_text": "含赶海工具与渔港讲解",
        "summary": "更贴近本地日常，能看到渔港运作、早市节奏和真实海边生活感。",
        "route": "看潮汐后进入滩涂体验赶海，再到渔港旁边看当天渔获上岸。",
        "includes": "含赶海小工具、潮汐时间规划与渔市观察路线。",
        "tips": "赶海一定要跟随安全范围，礁石区请穿防滑鞋并注意潮水变化。",
        "tags": ["赶海", "渔港", "本地生活", "潮汐"],
    },
    {
        "category": "文化探索",
        "title": "南山素斋与海边静修漫步",
        "location": "海南省三亚市 崖州区 南山周边",
        "duration_text": "4小时",
        "badge_text": "安静疗愈",
        "price_text": "¥328/位",
        "price_basis_text": "含素斋推荐与静修路线建议",
        "summary": "把园区外的海边步行与素斋体验结合起来，适合想慢下来的人。",
        "route": "上午走安静海边步道，中午体验素斋，随后在林荫区域静坐休憩。",
        "includes": "含轻度陪伴、用餐建议和静修点位指引。",
        "tips": "这条线更适合轻声慢行，不建议安排过于赶的行程，整体以松弛感为主。",
        "tags": ["素斋", "南山", "静修", "疗愈漫步"],
    },
    {
        "category": "文化探索",
        "title": "天涯小镇骑楼街拍与甜品巡味",
        "location": "海南省三亚市 天涯区 天涯小镇",
        "duration_text": "3.5小时",
        "badge_text": "文艺出片",
        "price_text": "¥248/位",
        "price_basis_text": "含巷口机位与甜品店推荐",
        "summary": "适合喜欢生活感街拍的人，把彩色巷子、海边转角和本地甜品串起来。",
        "route": "从主街走进彩色巷道，再到靠海的小店和甜品铺进行慢逛式打卡。",
        "includes": "含拍照点位建议、店铺清单和避开人群的时段提示。",
        "tips": "午后四点后光线更柔和，适合拍街景与人物半身照。",
        "tags": ["天涯小镇", "街拍", "甜品", "文艺路线"],
    },
    {
        "category": "夜生活",
        "title": "海棠湾露台酒吧夜色巡游",
        "location": "海南省三亚市 海棠区 海棠湾酒店带",
        "duration_text": "4小时",
        "badge_text": "夜色社交",
        "price_text": "¥488/位",
        "price_basis_text": "含路线安排与首杯推荐",
        "summary": "适合第一次在三亚体验夜生活的人，节奏轻松，不走嘈杂硬蹦路线。",
        "route": "从海边露台酒吧开始，转场到灯光氛围更强的屋顶空间和音乐吧。",
        "includes": "含店铺衔接、排队避峰建议和夜景拍照提醒。",
        "tips": "建议提前确认着装，海边夜风偏大，返程打车高峰建议预留时间。",
        "tags": ["酒吧", "海棠湾", "屋顶露台", "夜色"],
    },
    {
        "category": "夜生活",
        "title": "后海Livehouse与海边夜谈",
        "location": "海南省三亚市 海棠区 后海村",
        "duration_text": "4.5小时",
        "badge_text": "年轻氛围",
        "price_text": "¥368/位",
        "price_basis_text": "含店铺安排与音乐时段建议",
        "summary": "更偏年轻化的海边夜生活体验，适合朋友组队或社交型游客。",
        "route": "傍晚在海边小酒馆热身，随后进入 Livehouse，看驻唱后转场海边夜谈。",
        "includes": "含店铺路线、驻唱时间提醒和后海夜间活动建议。",
        "tips": "旺季后海夜间人流较密，建议轻装并注意保管随身物品。",
        "tags": ["后海", "Livehouse", "年轻社交", "海边夜晚"],
    },
    {
        "category": "夜生活",
        "title": "三亚湾海鲜夜市与清补凉收官",
        "location": "海南省三亚市 天涯区 三亚湾周边夜市",
        "duration_text": "3.5小时",
        "badge_text": "烟火气",
        "price_text": "¥228/位",
        "price_basis_text": "含夜市路线与点单建议",
        "summary": "以夜市和本地甜品收尾，适合不喝酒但想感受三亚夜间烟火气的人。",
        "route": "先逛海鲜夜市，再转去本地甜品摊和清补凉店完成夜宵收官。",
        "includes": "含夜市点单避坑建议、排队节奏和返程提醒。",
        "tips": "热门摊位排队时间长，建议错峰晚一点进入，体验更从容。",
        "tags": ["夜市", "海鲜", "清补凉", "本地烟火气"],
    },
    {
        "category": "夜生活",
        "title": "椰梦长廊夜骑与海风散步",
        "location": "海南省三亚市 天涯区 椰梦长廊",
        "duration_text": "3小时",
        "badge_text": "轻松夜游",
        "price_text": "¥198/位",
        "price_basis_text": "含单车与夜骑路线",
        "summary": "适合不想太闹腾的夜晚安排，以海风、夜骑和海岸散步为核心。",
        "route": "从椰梦长廊租车点出发，沿海岸线骑行，挑选海边停靠点步行拍夜景。",
        "includes": "含骑行路线、停车点建议和海边散步机位提醒。",
        "tips": "夜骑请以安全为先，尽量避免边骑边拍，拍照建议在固定停靠点完成。",
        "tags": ["夜骑", "椰梦长廊", "海风", "轻松路线"],
    },
    {
        "category": "夜生活",
        "title": "凤凰岛蓝调时刻城市夜景船拍",
        "location": "海南省三亚市 天涯区 凤凰岛周边海域",
        "duration_text": "2.5小时",
        "badge_text": "蓝调夜景",
        "price_text": "¥588/位",
        "price_basis_text": "含短时船拍与夜景点位",
        "summary": "以蓝调时刻和城市夜景反射为亮点，适合想拍高级感海边夜景的人。",
        "route": "傍晚登船，捕捉蓝调时间窗，再沿城市海岸线进行短线夜景巡游。",
        "includes": "含乘船、机位提示和夜拍曝光建议。",
        "tips": "蓝调时间很短，请提前完成妆造与设备准备，避免错过最佳时刻。",
        "tags": ["凤凰岛", "夜景船拍", "蓝调", "海边夜景"],
    },
    {
        "category": "夜生活",
        "title": "海昌不夜城灯光秀与夜拍散策",
        "location": "海南省三亚市 海棠区 海昌梦幻不夜城",
        "duration_text": "3.5小时",
        "badge_text": "灯光秀",
        "price_text": "¥268/位",
        "price_basis_text": "含灯光秀时段与夜拍路线",
        "summary": "适合情侣和亲子夜游，灯光秀、游乐氛围和夜拍都比较集中。",
        "route": "围绕园区主街、演艺广场和夜景装置走一圈，踩准灯光秀时段。",
        "includes": "含动线建议、灯光秀时刻提醒和夜拍机位安排。",
        "tips": "夜间游玩以轻便穿着为主，热门装置区建议拍完即走，避免久等。",
        "tags": ["不夜城", "灯光秀", "夜拍", "情侣夜游"],
    },
]


HOSTS = [
    ("seed-user-01", "阿舟"),
    ("seed-user-02", "林晚"),
    ("seed-user-03", "小满"),
    ("seed-user-04", "陈厨"),
    ("seed-user-05", "阿轻"),
    ("seed-user-06", "周周"),
]

def host_for_seed_index(index: int) -> tuple[str, str]:
    return HOSTS[(index - 1) % len(HOSTS)]


def chunk_image_counts(total_items: int, total_images: int) -> list[int]:
    counts = [6] * total_items
    extra = total_images - (total_items * 6)
    for index in range(extra):
        counts[index] += 1
    return counts


def ensure_host_users(conn: sqlite3.Connection) -> None:
    user_ids = {row[0] for row in conn.execute("select id from users").fetchall()}
    missing = [user_id for user_id, _ in HOSTS if user_id not in user_ids]
    if missing:
        raise RuntimeError(f"缺少宿主用户，无法写入体验数据: {missing}")


def build_description(item: dict) -> str:
    return "\n".join(
        [
            item["summary"],
            f"路线安排：{item['route']}",
            f"包含内容：{item['includes']}",
            f"体验提示：{item['tips']}",
        ]
    )


def build_service_image_pool(host_id: str) -> list[Path]:
    host_dir = SERVICE_DIR / host_id
    host_images = sorted(path for path in host_dir.glob("*.jpg") if path.is_file())
    if host_images:
        return host_images

    fallback_images = sorted(path for path in SERVICE_DIR.rglob("*.jpg") if path.is_file())
    return [path for path in fallback_images if path.parent != SERVICE_DIR]


def prepare_images() -> list[str]:
    EXPERIENCE_DIR.mkdir(parents=True, exist_ok=True)
    for old_file in EXPERIENCE_DIR.rglob("sanya_experience_*.jpg"):
        old_file.unlink()

    counts = chunk_image_counts(len(EXPERIENCE_SEED_DATA), TOTAL_IMAGES)
    filenames: list[str] = []
    cursor = 1

    for index, image_count in enumerate(counts, start=1):
        host_id, _ = host_for_seed_index(index)
        image_pool = build_service_image_pool(host_id)
        if not image_pool:
            raise RuntimeError(f"缺少可用 service 图片，无法为 {host_id} 生成体验图片文件")
        start_offset = ((index - 1) * 7) % len(image_pool)

        for offset in range(image_count):
            source = image_pool[(start_offset + offset) % len(image_pool)]
            relative_path = f"{host_id}/sanya_experience_{cursor:03d}.jpg"
            target = EXPERIENCE_DIR / relative_path
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(source, target)
            filenames.append(relative_path)
            cursor += 1

    if len(filenames) != TOTAL_IMAGES:
        raise RuntimeError(f"体验图片数量不正确: {len(filenames)}")
    return filenames


def replace_experiences(conn: sqlite3.Connection, filenames: list[str]) -> tuple[int, int]:
    ensure_host_users(conn)
    conn.execute("delete from experiences")
    deleted_services = conn.execute(
        """
        delete from services
        where id like 'seed-home-seed-user-001-%'
          and ifnull(trim(category), '') = ''
        """
    ).rowcount

    counts = chunk_image_counts(len(EXPERIENCE_SEED_DATA), TOTAL_IMAGES)
    image_cursor = 0
    now_ms = int(time.time() * 1000)
    inserted = 0

    for index, (item, image_count) in enumerate(zip(EXPERIENCE_SEED_DATA, counts), start=1):
        host_id, host_name = host_for_seed_index(index)
        image_paths = [f"static/experiences/{name}" for name in filenames[image_cursor:image_cursor + image_count]]
        image_cursor += image_count
        experience_id = f"seed-exp-{index:03d}"
        created_at = now_ms - ((len(EXPERIENCE_SEED_DATA) - index) * 600000)
        updated_at = created_at + 300000

        conn.execute(
            """
            insert into experiences (
                id, title, description, cover_image_url, image_urls, location,
                price_text, price_basis_text, category, duration_text, badge_text,
                tags, host_id, host_name, created_at, updated_at, is_deleted, is_synced
            ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 1)
            """,
            (
                experience_id,
                item["title"],
                build_description(item),
                image_paths[0],
                json.dumps(image_paths, ensure_ascii=False),
                item["location"],
                item["price_text"],
                item["price_basis_text"],
                item["category"],
                item["duration_text"],
                item["badge_text"],
                json.dumps(item["tags"], ensure_ascii=False),
                host_id,
                host_name,
                created_at,
                updated_at,
            ),
        )
        inserted += 1

    conn.commit()
    return inserted, deleted_services


def main() -> None:
    if not DB_PATH.exists():
        raise RuntimeError(f"数据库不存在: {DB_PATH}")

    filenames = prepare_images()
    with sqlite3.connect(DB_PATH) as conn:
        inserted, deleted_services = replace_experiences(conn, filenames)
        category_rows = conn.execute(
            "select category, count(*) from experiences group by category order by category"
        ).fetchall()

    print(f"体验图片已生成: {len(filenames)} 张")
    print(f"experiences 已写入: {inserted} 条")
    print(f"services 已删除占位体验数据: {deleted_services} 条")
    print("分类统计:")
    for category, count in category_rows:
        print(f"  - {category}: {count}")


if __name__ == "__main__":
    main()
