import React, { useMemo, useState } from 'react';
import Taro, { useLoad } from '@tarojs/taro';
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
  serviceTypeDescription: string;
  serviceTypeKeywords: string[];
  hostName: string;
  hostRole: string;
  hostStats: string;
  verified: boolean;
  favorite: boolean;
  tags: string[];
  images: string[];
  description: string[];
  notes: string[];
  extraFeeDescription: string;
  plans: DetailPlan[];
  reviews: DetailReview[];
};

declare var process: any;

const apiBaseUrl = (() => {
  const baseStr = process.env.TARO_APP_API_BASE_URL || '';
  return baseStr.replace(/^['"](.*)['"]$/, '$1').replace(/\/$/, '');
})();

const resolveMediaUrl = (url?: string) => {
  if (!url) return '';
  if (url.startsWith('http://') || url.startsWith('https://')) return url;
  return `${apiBaseUrl}/${url.replace(/^\//, '')}`;
};

const normalizeText = (value?: string | null) => (value ?? '').trim();

const uniqueNonEmpty = (values: Array<string | null | undefined>) => {
  const result: string[] = [];
  values.forEach((value) => {
    const normalized = normalizeText(value);
    if (normalized && !result.includes(normalized)) {
      result.push(normalized);
    }
  });
  return result;
};

const keywordSignalGroups: Array<{ label: string; keywords: string[] }> = [
  { label: '旅拍', keywords: ['旅拍', '摄影', '拍照', '跟拍'] },
  { label: '潜水', keywords: ['潜水', '自由潜', '水肺'] },
  { label: '冲浪', keywords: ['冲浪', '桨板'] },
  { label: '游艇', keywords: ['游艇', '帆船', '出海', '海钓'] },
  { label: '按摩', keywords: ['按摩', 'spa', '理疗'] },
  { label: '妆造', keywords: ['化妆', '妆造', '造型'] },
  { label: '美食', keywords: ['私厨', '美食', '晚宴', '餐'] },
  { label: '亲子', keywords: ['亲子', '家庭'] },
  { label: '情侣', keywords: ['情侣', '约会'] },
  { label: '团建', keywords: ['团建', '聚会', '派对'] },
];

const buildSummaryTags = (data: any) =>
  uniqueNonEmpty([data.category, data.service_mode, data.price_basis_text]).slice(0, 4);

const buildServiceTypeDescription = (data: any) => {
  const category = normalizeText(data.category);
  const serviceMode = normalizeText(data.service_mode);
  const priceBasis = normalizeText(data.price_basis_text);
  const parts = [
    category ? `这是一个${category}服务` : '',
    serviceMode ? `通常以${serviceMode}方式提供` : '',
    priceBasis ? `计费说明为${priceBasis}` : '',
  ].filter(Boolean);
  return parts.length
    ? `${parts.join('，')}，具体安排请和主理人沟通确认。`
    : '服务内容、提供方式与计费规则请和主理人沟通确认。';
};

const buildServiceTypeKeywords = (data: any) => {
  const category = normalizeText(data.category);
  const serviceMode = normalizeText(data.service_mode);
  const priceBasis = normalizeText(data.price_basis_text);
  const searchableText = [
    data.title,
    data.description,
    category,
    serviceMode,
    priceBasis,
  ]
    .map((item) => normalizeText(item))
    .join(' ')
    .toLowerCase();
  const matchedSignals = keywordSignalGroups
    .filter(({ keywords }) => keywords.some((keyword) => searchableText.includes(keyword)))
    .map(({ label }) => label);
  return uniqueNonEmpty([category, serviceMode, priceBasis, ...matchedSignals]).slice(0, 8);
};

const buildExtraFeeDescription = (data: any) => {
  const lines: string[] = [];
  const prepaymentPercent = Number(data.prepayment_percent ?? 0);
  if (prepaymentPercent > 0) {
    lines.push(`当前预付款比例为${prepaymentPercent}%，最终金额与尾款以沟通确认为准。`);
  } else {
    lines.push('当前无需预付款，最终金额以沟通确认为准。');
  }

  const extraRules = Array.isArray(data.service_declarations_extra)
    ? data.service_declarations_extra
        .map((item: unknown) => normalizeText(typeof item === 'string' ? item : ''))
        .filter(Boolean)
    : [];
  if (extraRules.length > 0) {
    lines.push(`补充说明：${extraRules.slice(0, 2).join('；')}`);
  } else {
    lines.push('节假日、跨区交通、超时加购、门票或场地等额外费用如有发生，需提前与主理人确认。');
  }
  return lines.join('\n');
};

function ServiceDetailPage() {
  const serviceId = Taro.getCurrentInstance().router?.params?.id ?? '';
  const [service, setService] = useState<ServiceDetail | null>(null);
  const [selectedPlanIndex, setSelectedPlanIndex] = useState(0);
  const [currentImage, setCurrentImage] = useState(0);
  const [favorite, setFavorite] = useState(false);
  const [bookingSheetVisible, setBookingSheetVisible] = useState(false);

  useLoad(() => {
    if (!serviceId) return;
    Taro.request({
      url: `${apiBaseUrl}/services/${serviceId}`,
      method: 'GET',
      success: (res: any) => {
        if (res.statusCode === 200) {
          const data = res.data;
          const rawImages = data.image_urls?.length ? data.image_urls : [data.cover_image_url].filter(Boolean);
          const images = rawImages.map(resolveMediaUrl);
          const mappedService: ServiceDetail = {
            id: data.id,
            title: data.title,
            info: `${data.category || ''} · ${data.service_mode || ''} · ${data.service_time || 0}分钟`,
            location: data.location || '线上/线下',
            serviceTypeDescription: buildServiceTypeDescription(data),
            serviceTypeKeywords: buildServiceTypeKeywords(data),
            hostName: data.creator || '神秘主理人',
            hostRole: '主理人',
            hostStats: '',
            verified: true,
            favorite: false,
            tags: buildSummaryTags(data),
            images,
            description: data.description ? data.description.split('\n') : ['暂无介绍'],
            notes: [
              '请提前确认服务时间。',
              '如遇极端天气，可协商改期。'
            ],
            extraFeeDescription: buildExtraFeeDescription(data),
            plans: [
              {
                id: `${data.id}-plan-1`,
                title: '基础服务',
                subtitle: '标准时长服务',
                price: data.price_text || '面议',
                duration: `${data.service_time || 0}分钟`,
                description: '包含基础的服务内容。',
              }
            ],
            reviews: [],
          };
          setService(mappedService);
        } else {
          Taro.showToast({ title: '加载失败', icon: 'none' });
        }
      },
      fail: () => {
        Taro.showToast({ title: '网络错误', icon: 'none' });
      }
    });
  });

  if (!service) {
    return <View className={styles.page}><Text style={{ padding: '20px', display: 'block', textAlign: 'center' }}>加载中...</Text></View>;
  }

  const selectedPlan = useMemo(
    () => service.plans[selectedPlanIndex] ?? service.plans[0],
    [service.plans, selectedPlanIndex]
  );
  const currentPreviewImage = service.images[currentImage] ?? service.images[0] ?? '';

  const handlePreviewImages = (index: number) => {
    if (!service.images.length) return;
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
          {service.images.length ? (
            service.images.map((image, index) => (
              <SwiperItem key={image}>
                <View className={styles.heroSlide} onClick={() => handlePreviewImages(index)}>
                  <Image className={styles.heroImage} src={image} mode="aspectFill" />
                </View>
              </SwiperItem>
            ))
          ) : (
            <SwiperItem key='empty-image'>
              <View className={styles.heroSlide}>
                <View style={{ width: '100%', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#8d8d8d', background: '#f5f5f5' }}>
                  暂无真实图片
                </View>
              </View>
            </SwiperItem>
          )}
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
        <View className={styles.heroPager}>{service.images.length ? `${currentImage + 1} / ${service.images.length}` : '0 / 0'}</View>
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
          <Text className={styles.sectionTitle}>预订须知</Text>
          <View className={styles.noteList}>
            {service.notes.map((item) => (
              <View key={item} className={styles.noteRow}>
                <View className={styles.noteDot} />
                <Text className={styles.noteText}>{item}</Text>
              </View>
            ))}
          </View>
        </View>

        <View className={styles.section}>
          <Text className={styles.sectionTitle}>服务类型说明</Text>
          <Text className={styles.serviceTypeDesc}>{service.serviceTypeDescription}</Text>
          <View className={styles.keywordWrap}>
            {service.serviceTypeKeywords.map((keyword) => (
              <View key={keyword} className={styles.keywordChip}>
                <Text className={styles.keywordChipText}>{keyword}</Text>
              </View>
            ))}
          </View>
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
        <View className={styles.bookButton} onClick={() => setBookingSheetVisible(true)}>
          <Text className={styles.bookButtonText}>预订</Text>
        </View>
      </View>

      {bookingSheetVisible ? (
        <View className={styles.bookingSheetMask} onClick={() => setBookingSheetVisible(false)}>
          <View className={styles.bookingSheet} onClick={(event) => event.stopPropagation()}>
            <View className={styles.bookingSheetHeader}>
              <View>
                <Text className={styles.bookingSheetTitle}>选择预订信息</Text>
                <Text className={styles.bookingSheetSubtitle}>{service.title}</Text>
              </View>
              <View className={styles.bookingSheetClose} onClick={() => setBookingSheetVisible(false)}>
                <Text className={styles.bookingSheetCloseText}>×</Text>
              </View>
            </View>

            <View className={styles.bookingSection}>
              <Text className={styles.bookingSectionTitle}>方案选择</Text>
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

            <View className={styles.bookingSection}>
              <Text className={styles.bookingSectionTitle}>额外费用说明</Text>
              <View className={styles.bookingInfoCard}>
                <Text className={styles.bookingInfoText}>{service.extraFeeDescription}</Text>
              </View>
            </View>

            <View
              className={styles.bookingConfirmButton}
              onClick={() => {
                setBookingSheetVisible(false);
                Taro.showToast({ title: '预订功能即将上线', icon: 'none' });
              }}
            >
              <Text className={styles.bookingConfirmButtonText}>确认预订信息</Text>
            </View>
          </View>
        </View>
      ) : null}
    </View>
  );
}

export default ServiceDetailPage;
