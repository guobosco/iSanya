import React, { useMemo, useState } from 'react';
import Taro from '@tarojs/taro';
import { Image, Swiper, SwiperItem, Text, View } from '@tarojs/components';
import styles from './index.module.scss';

type DetailPlan = {
  id: string;
  title: string;
  subtitle: string;
  price: string;
  duration: string;
  description: string;
};

type DetailReview = {
  id: string;
  user: string;
  date: string;
  content: string;
};

type ServiceDetail = {
  id: string;
  title: string;
  info: string;
  location: string;
  hostName: string;
  hostRole: string;
  hostStats: string;
  verified: boolean;
  favorite: boolean;
  tags: string[];
  images: string[];
  description: string[];
  notes: string[];
  plans: DetailPlan[];
  reviews: DetailReview[];
};

const buildImage = (prompt: string) =>
  `https://coresg-normal.trae.ai/api/ide/v1/text_to_image?prompt=${encodeURIComponent(prompt)}&image_size=portrait_4_3`;

const serviceDetails: Record<string, ServiceDetail> = {
  'svc-01': {
    id: 'svc-01',
    title: '海棠湾一日陪游',
    info: '陪游 · 三亚 · 中文/英语',
    location: '海棠湾 / 后海 / 蜈支洲接驳',
    hostName: 'Luna',
    hostRole: '在地向导',
    hostStats: '已服务 136 次 · 平均 8 分钟回复',
    verified: true,
    favorite: true,
    tags: ['可定制路线', '女生友好', '拍照带位', '轻松不赶场'],
    images: [
      buildImage('luxury tropical beach boardwalk at haitang bay sanya china, stylish female local guide walking with travelers, warm sunset light, realistic travel lifestyle photography, premium composition'),
      buildImage('sanya tropical beach lounge and clear sea water, relaxed one day guided trip, candid editorial travel photo, natural skin tones, high detail'),
      buildImage('young travelers exploring seaside market in sanya with local host, airy summer color palette, realistic documentary photo, high detail'),
    ],
    description: [
      '按安卓详情页节奏，这里保留大图、标题、主理人、介绍、方案和预订须知的完整层级。这个陪游路线主打松弛感，不走游客挤爆的打卡顺序，而是根据出发时间和你想要的氛围灵活调整。',
      '适合第一次来三亚、想少做攻略的人，也适合希望边玩边拍、顺路吃本地店的人。全程节奏不催促，可按聊天偏好切换成安静陪伴或高互动路线。',
    ],
    notes: [
      '默认集合点在海棠湾，可根据酒店位置协商。',
      '行程开始前 12 小时可免费改期一次。',
      '门票、餐饮、跨区交通等第三方费用需自理。',
    ],
    plans: [
      {
        id: 'svc-01-plan-1',
        title: '轻松半日',
        subtitle: '适合初次到三亚的慢节奏陪玩',
        price: '¥199起',
        duration: '4小时',
        description: '含路线规划、陪同游玩、基础拍照建议，适合海边散步和轻量逛吃。',
      },
      {
        id: 'svc-01-plan-2',
        title: '落日一日',
        subtitle: '加上日落机位与夜间放松行程',
        price: '¥299起',
        duration: '8小时',
        description: '适合想把白天到夜晚一口气串起来的人，覆盖拍照、餐食与夜游建议。',
      },
    ],
    reviews: [
      { id: 'r-01', user: '小羽', date: '2天前', content: '路线安排很顺，完全不像赶景点，更像被朋友带着玩。' },
      { id: 'r-02', user: 'Momo', date: '1周前', content: '拍照点位找得很准，海边风很大也能避开杂乱背景。' },
    ],
  },
  'svc-02': {
    id: 'svc-02',
    title: '蜈支洲包车出海线',
    info: '租车 · 海棠区 · 半日',
    location: '海棠湾码头 / 蜈支洲周边',
    hostName: '阿泽',
    hostRole: '线路主理人',
    hostStats: '已服务 82 次 · 平均 12 分钟回复',
    verified: true,
    favorite: false,
    tags: ['可早起上岛', '亲子友好', '接驳省心', '避开排队'],
    images: [
      buildImage('private car service arriving at tropical island pier in sanya china, clean premium vehicle, blue ocean, realistic editorial photography'),
      buildImage('sanya pier and island ferry route with luxury transport service, bright morning light, realistic travel photo'),
      buildImage('travelers entering tropical island scenic area with local driver support, calm premium summer look, realistic photography'),
    ],
    description: [
      '这个方案更偏向出海线接驳和当天节奏控制，核心不是单纯的车，而是把接送、上岛、回程的时间窗口尽量安排得更舒服。',
    ],
    notes: [
      '包车时长从见面开始计时。',
      '跨时段等待和夜间返程会有额外说明。',
      '如遇极端天气，可协商改期。',
    ],
    plans: [
      {
        id: 'svc-02-plan-1',
        title: '半日接驳',
        subtitle: '适合只想高效往返上岛',
        price: '¥388起',
        duration: '4小时',
        description: '含酒店接送与基础等候，适合轻装上岛。',
      },
      {
        id: 'svc-02-plan-2',
        title: '全天包车',
        subtitle: '适合串联海棠湾多个点位',
        price: '¥688起',
        duration: '10小时',
        description: '适合岛上和周边商圈一起走，整体更灵活。',
      },
    ],
    reviews: [
      { id: 'r-03', user: 'Kiki', date: '3天前', content: '接送特别稳，时间卡得很好，没有那种一直赶的压迫感。' },
    ],
  },
  'svc-03': {
    id: 'svc-03',
    title: '酒店上门按摩',
    info: '按摩 · 三亚湾 · 60分钟',
    location: '三亚湾 / 大东海 / 亚龙湾',
    hostName: 'Miya',
    hostRole: '理疗主理人',
    hostStats: '已服务 211 次 · 平均 6 分钟回复',
    verified: true,
    favorite: false,
    tags: ['酒店上门', '晚间可约', '舒缓放松', '一人也可约'],
    images: [
      buildImage('luxury hotel room massage service in sanya, elegant spa bed, warm ambient light, realistic wellness photography'),
      buildImage('professional relaxation massage service with tropical resort atmosphere, premium lifestyle editorial photo'),
      buildImage('calm spa details in ocean view hotel suite, folded towels and essential oils, realistic high-end photo'),
    ],
    description: [
      '更贴近安卓端详情页里的“服务介绍 + 方案选择”体验，用户会先看到整体氛围，再往下看具体时长和适合人群。这个按摩服务以恢复放松为主，适合行程走多了之后晚上回酒店安排。',
    ],
    notes: [
      '请提前确认酒店允许上门服务。',
      '孕期、术后或特殊体质需提前说明。',
      '默认携带基础用品，不额外占用房间设备。',
    ],
    plans: [
      {
        id: 'svc-03-plan-1',
        title: '舒缓单人',
        subtitle: '肩颈 + 背部放松',
        price: '¥168起',
        duration: '60分钟',
        description: '适合飞机落地或一天暴走后的基础恢复。',
      },
      {
        id: 'svc-03-plan-2',
        title: '深度修复',
        subtitle: '全身舒缓与腿部放松',
        price: '¥238起',
        duration: '90分钟',
        description: '整体节奏更完整，覆盖腰背、腿部和肩颈。',
      },
    ],
    reviews: [
      { id: 'r-04', user: '橙子', date: '昨天', content: '晚上回酒店直接按很省事，力度会先沟通，体验很稳。' },
    ],
  },
  'svc-04': {
    id: 'svc-04',
    title: '琼味私厨到店体验',
    info: '私厨 · 吉阳区 · 4-6人',
    location: '吉阳区社区小院',
    hostName: '阿玲',
    hostRole: '私厨主理人',
    hostStats: '已服务 59 次 · 平均 15 分钟回复',
    verified: true,
    favorite: true,
    tags: ['本地家常', '适合聚会', '可做忌口调整', '氛围感强'],
    images: [
      buildImage('intimate local private dining experience in sanya courtyard, hainan cuisine on wooden table, warm evening light, realistic editorial photo'),
      buildImage('chef preparing authentic hainan dishes in cozy boutique kitchen, realistic food and lifestyle photography'),
      buildImage('friends enjoying tropical courtyard dinner in sanya, soft lighting, premium realistic photo'),
    ],
    description: [
      '不是标准化餐厅套餐，而是更像去主理人家里吃一顿节奏舒服的琼味家常。更适合 4 到 6 人一起，边吃边聊，感受本地生活感。',
    ],
    notes: [
      '需至少提前一天预约食材。',
      '海鲜、忌口和儿童需求可提前备注。',
      '到店体验不含往返交通。',
    ],
    plans: [
      {
        id: 'svc-04-plan-1',
        title: '四人轻聚',
        subtitle: '适合朋友和情侣双双组队',
        price: '¥258起',
        duration: '2小时',
        description: '包含招牌琼味热菜、主食和甜汤。',
      },
      {
        id: 'svc-04-plan-2',
        title: '六人围桌',
        subtitle: '更完整的热菜和海鲜搭配',
        price: '¥428起',
        duration: '2.5小时',
        description: '适合生日、旅行小聚或家庭同行。',
      },
    ],
    reviews: [
      { id: 'r-05', user: '阿梨', date: '5天前', content: '很像去熟人家里吃饭，菜的味道和聊天氛围都很好。' },
    ],
  },
  'svc-05': {
    id: 'svc-05',
    title: '日落跟拍轻旅拍',
    info: '旅拍 · 椰梦长廊 · 90分钟',
    location: '椰梦长廊 / 小东海 / 后海',
    hostName: 'Cici',
    hostRole: '旅拍摄影师',
    hostStats: '已服务 173 次 · 平均 9 分钟回复',
    verified: true,
    favorite: false,
    tags: ['自然抓拍', '情侣友好', '日落时段', '不过度摆拍'],
    images: [
      buildImage('sunset beach photoshoot in sanya coconut dream corridor, stylish couple, golden hour, realistic editorial photography'),
      buildImage('female photographer guiding traveler on tropical beach at sunset, cinematic warm light, realistic photo'),
      buildImage('sanya seaside portrait session with orange sunset sky, natural candid pose, premium realistic image'),
    ],
    description: [
      '整体风格更偏自然感，不会让你一直摆动作，而是先通过散步和聊天找到状态，再在光线最好的时间点快速出片。',
    ],
    notes: [
      '建议避开正午，优先预订日落前 90 分钟。',
      '妆造和服装可提供搭配建议。',
      '原图与精修张数会在下单前确认。',
    ],
    plans: [
      {
        id: 'svc-05-plan-1',
        title: '单人轻旅拍',
        subtitle: '适合旅行纪念和头像更新',
        price: '¥299起',
        duration: '90分钟',
        description: '包含基础引导和精选成片交付。',
      },
      {
        id: 'svc-05-plan-2',
        title: '双人海边',
        subtitle: '情侣或闺蜜一起拍',
        price: '¥399起',
        duration: '120分钟',
        description: '双人画面更多，方便切换多个点位。',
      },
    ],
    reviews: [
      { id: 'r-06', user: 'Yoyo', date: '4天前', content: '完全不尴尬，像被带着散步，最后挑片惊喜很大。' },
    ],
  },
  'svc-06': {
    id: 'svc-06',
    title: '海边健身私教陪练',
    info: '健身 · 小东海 · 1小时',
    location: '小东海 / 鹿回头沿线',
    hostName: 'Neo',
    hostRole: '健身教练',
    hostStats: '已服务 48 次 · 平均 13 分钟回复',
    verified: true,
    favorite: false,
    tags: ['晨练友好', '户外训练', '动作纠正', '新手可上手'],
    images: [
      buildImage('personal training on tropical beach in sanya at sunrise, athletic coach guiding workout, realistic sports lifestyle photography'),
      buildImage('outdoor fitness session near the sea in sanya, premium healthy lifestyle photo, natural morning light'),
      buildImage('coach and traveler stretching on tropical coastline, calm minimal realistic image'),
    ],
    description: [
      '把旅行里的“动一下”做得不那么硬核，重点是利用海边场景做轻量训练和拉伸，让体能恢复、出汗和风景一起发生。',
    ],
    notes: [
      '建议穿运动鞋或稳固凉鞋。',
      '下雨会改为遮蔽点训练或改期。',
      '有旧伤请提前告知教练调整动作。',
    ],
    plans: [
      {
        id: 'svc-06-plan-1',
        title: '海边唤醒',
        subtitle: '晨间轻汗和拉伸',
        price: '¥220起',
        duration: '60分钟',
        description: '适合旅行期间保持状态，强度友好。',
      },
      {
        id: 'svc-06-plan-2',
        title: '进阶燃脂',
        subtitle: '加入核心与下肢训练',
        price: '¥320起',
        duration: '90分钟',
        description: '更适合平时有运动基础的人。',
      },
    ],
    reviews: [
      { id: 'r-07', user: 'Ryan', date: '1周前', content: '海边训练很舒服，动作盯得细，强度控制也比较科学。' },
    ],
  },
};

function ServiceDetailPage() {
  const serviceId = Taro.getCurrentInstance().router?.params?.id ?? 'svc-01';
  const service = useMemo(() => serviceDetails[serviceId] ?? serviceDetails['svc-01'], [serviceId]);
  const [selectedPlanIndex, setSelectedPlanIndex] = useState(0);
  const [currentImage, setCurrentImage] = useState(0);
  const [favorite, setFavorite] = useState(service.favorite);

  const selectedPlan = service.plans[selectedPlanIndex] ?? service.plans[0];
  const currentPreviewImage = service.images[currentImage] ?? service.images[0];

  const handlePreviewImages = (index: number) => {
    Taro.previewImage({
      current: service.images[index] ?? currentPreviewImage,
      urls: service.images,
    });
  };

  return (
    <View className={styles.page}>
      <View className={styles.hero}>
        <Swiper
          className={styles.heroSwiper}
          circular
          current={currentImage}
          indicatorDots={false}
          onChange={(event) => setCurrentImage(event.detail.current)}
        >
          {service.images.map((image, index) => (
            <SwiperItem key={image}>
              <View className={styles.heroSlide} onClick={() => handlePreviewImages(index)}>
                <Image className={styles.heroImage} src={image} mode="aspectFill" />
              </View>
            </SwiperItem>
          ))}
        </Swiper>
        <View className={styles.heroMask} />
        <View className={styles.heroActions}>
          <View className={styles.actionButton} onClick={() => Taro.navigateBack({ fail: () => Taro.switchTab({ url: '/pages/home/index' }) })}>
            <Text className={styles.actionIcon}>‹</Text>
          </View>
          <View className={styles.actionGroup}>
            <View className={styles.actionButton} onClick={() => Taro.showToast({ title: '分享待接入', icon: 'none' })}>
              <Text className={styles.actionIcon}>↗</Text>
            </View>
            <View
              className={styles.actionButton}
              onClick={() => {
                setFavorite((value) => !value);
                Taro.showToast({ title: favorite ? '已取消收藏' : '已加入心愿单', icon: 'none' });
              }}
            >
              <Text className={`${styles.actionIcon} ${favorite ? styles.actionIconActive : ''}`}>{favorite ? '♥' : '♡'}</Text>
            </View>
          </View>
        </View>
        <View className={styles.heroPager}>{currentImage + 1} / {service.images.length}</View>
        <View className={styles.heroDots}>
          {service.images.map((image, index) => (
            <View key={image} className={`${styles.heroDot} ${index === currentImage ? styles.heroDotActive : ''}`} />
          ))}
        </View>
      </View>

      <View className={styles.panel}>
        <Text className={styles.title}>{service.title}</Text>
        <Text className={styles.meta}>{service.info}</Text>

        <View className={styles.tagGrid}>
          {service.tags.map((tag) => (
            <View key={tag} className={styles.tagItem}>
              <Text className={styles.tagText}>{tag}</Text>
            </View>
          ))}
        </View>

        <View className={styles.hostRow}>
          <View className={styles.avatarWrap}>
            <View className={styles.avatar}>
              <Text className={styles.avatarText}>{service.hostName.slice(0, 1)}</Text>
            </View>
            {service.verified ? <View className={styles.verifiedBadge}>✓</View> : null}
          </View>
          <View className={styles.hostInfo}>
            <Text className={styles.hostName}>{service.hostName}</Text>
            <Text className={styles.hostMeta}>{service.hostRole} · {service.hostStats}</Text>
          </View>
          <View className={styles.chatButton} onClick={() => Taro.showToast({ title: '聊天待接入', icon: 'none' })}>
            <Text className={styles.chatButtonText}>聊一聊</Text>
          </View>
        </View>

        <View className={styles.divider} />

        <View className={styles.section}>
          <Text className={styles.sectionTitle}>服务介绍</Text>
          {service.description.map((item) => (
            <Text key={item} className={styles.sectionText}>{item}</Text>
          ))}
        </View>

        <View className={styles.section}>
          <Text className={styles.sectionTitle}>方案选择</Text>
          <View className={styles.planList}>
            {service.plans.map((plan, index) => (
              <View
                key={plan.id}
                className={`${styles.planCard} ${index === selectedPlanIndex ? styles.planCardActive : ''}`}
                onClick={() => setSelectedPlanIndex(index)}
              >
                <View className={styles.planHeader}>
                  <View>
                    <Text className={styles.planTitle}>{plan.title}</Text>
                    <Text className={styles.planSubtitle}>{plan.subtitle}</Text>
                  </View>
                  <View className={styles.planPriceWrap}>
                    <Text className={styles.planPrice}>{plan.price}</Text>
                    <Text className={styles.planDuration}>{plan.duration}</Text>
                  </View>
                </View>
                <Text className={styles.planDescription}>{plan.description}</Text>
              </View>
            ))}
          </View>
        </View>

        <View className={styles.section}>
          <Text className={styles.sectionTitle}>预订须知</Text>
          <View className={styles.noteList}>
            {service.notes.map((item) => (
              <View key={item} className={styles.noteRow}>
                <View className={styles.noteDot} />
                <Text className={styles.noteText}>{item}</Text>
              </View>
            ))}
          </View>
          <Text className={styles.locationText}>服务区域：{service.location}</Text>
        </View>

        <View className={styles.section}>
          <Text className={styles.sectionTitle}>{service.reviews.length}条评价</Text>
          <View className={styles.reviewList}>
            {service.reviews.map((review) => (
              <View key={review.id} className={styles.reviewCard}>
                <View className={styles.reviewHeader}>
                  <Text className={styles.reviewUser}>{review.user}</Text>
                  <Text className={styles.reviewDate}>{review.date}</Text>
                </View>
                <Text className={styles.reviewContent}>{review.content}</Text>
              </View>
            ))}
          </View>
        </View>

        <View className={styles.bottomSpacer} />
      </View>

      <View className={styles.bottomBar}>
        <View className={styles.bottomPriceWrap}>
          <Text className={styles.bottomPrice}>{selectedPlan.price}</Text>
          <Text className={styles.bottomPriceMeta}>{selectedPlan.duration}</Text>
        </View>
        <View className={styles.bookButton} onClick={() => Taro.showToast({ title: '预订功能即将上线', icon: 'none' })}>
          <Text className={styles.bookButtonText}>预订</Text>
        </View>
      </View>
    </View>
  );
}

export default ServiceDetailPage;
