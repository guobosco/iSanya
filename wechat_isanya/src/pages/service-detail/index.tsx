import React, { useEffect, useMemo, useState } from 'react';
import Taro, { useLoad } from '@tarojs/taro';
import { Image, ScrollView, Swiper, SwiperItem, Text, View } from '@tarojs/components';
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

type BookingTimeSlot = {
  start: string;
  end: string;
};

type BookingDayOption = {
  key: string;
  label: string;
  description: string;
  timeOptions: string[];
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
  prepaymentPercent: number;
  bookingTimeRangesJson: string;
  bookingLeadHours: number;
  bookingFutureOpenDays: number;
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

const bookingWeekdayLabels = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];

const padTwo = (value: number) => value.toString().padStart(2, '0');

const toDateKey = (date: Date) =>
  `${date.getFullYear()}-${padTwo(date.getMonth() + 1)}-${padTwo(date.getDate())}`;

const parseMinutes = (hhmm: string) => {
  const [hourText, minuteText] = hhmm.trim().split(':');
  const hour = Number(hourText);
  const minute = Number(minuteText);
  if (!Number.isInteger(hour) || !Number.isInteger(minute)) return null;
  if (hour < 0 || hour > 23 || minute < 0 || minute > 59) return null;
  return hour * 60 + minute;
};

const formatClockLabel = (hhmm: string) => {
  const minutes = parseMinutes(hhmm);
  if (minutes === null) return hhmm;
  const hour = Math.floor(minutes / 60);
  const minute = minutes % 60;
  return `${hour}:${padTwo(minute)}`;
};

const halfHourLabels = () =>
  Array.from({ length: 48 }, (_, index) => {
    const hour = Math.floor(index / 2);
    const minute = (index % 2) * 30;
    return `${padTwo(hour)}:${padTwo(minute)}`;
  });

const decodeBookingTimeRanges = (rawJson?: string) => {
  if (!rawJson?.trim()) return [];
  try {
    const parsed = JSON.parse(rawJson);
    if (!Array.isArray(parsed)) return [];
    return parsed
      .map((item) => ({
        start: typeof item?.start === 'string' ? item.start : '',
        end: typeof item?.end === 'string' ? item.end : '',
      }))
      .filter((item) => item.start && item.end);
  } catch {
    return [];
  }
};

const availableStartTimesForDay = (bookingTimeRangesJson?: string) => {
  const slots = decodeBookingTimeRanges(bookingTimeRangesJson);
  const ranges = slots.length ? slots : [{ start: '00:00', end: '24:00' } as BookingTimeSlot];
  const output = new Set<string>();

  ranges.forEach((slot) => {
    const startMinutes = parseMinutes(slot.start);
    const endMinutes = slot.end === '24:00' ? 24 * 60 : parseMinutes(slot.end);
    if (startMinutes === null || endMinutes === null || endMinutes <= startMinutes) return;

    let current = Math.ceil(startMinutes / 30) * 30;
    while (current < endMinutes && current < 24 * 60) {
      output.add(`${padTwo(Math.floor(current / 60))}:${padTwo(current % 60)}`);
      current += 30;
    }
  });

  return Array.from(output).sort();
};

const normalizeBookingFutureOpenDays = (value?: number) => {
  const days = Number(value ?? 30);
  if (!Number.isFinite(days) || days < 7) return 30;
  if (days > 180) return 180;
  return Math.floor(days);
};

const normalizeBookingLeadHours = (value?: number) => {
  const hours = Number(value ?? 0);
  if (!Number.isFinite(hours) || hours < 0) return 0;
  if (hours > 168) return 168;
  return hours;
};

const buildBookingDayLabel = (date: Date, today: Date) => {
  const diffDays = Math.round((date.getTime() - today.getTime()) / (24 * 60 * 60 * 1000));
  if (diffDays === 0) return '今天';
  if (diffDays === 1) return '明天';
  if (diffDays === 2) return '后天';
  return `${date.getMonth() + 1}/${date.getDate()}`;
};

const buildBookingDayOptions = (service: ServiceDetail | null): BookingDayOption[] => {
  if (!service) return [];

  const dayCount = Math.min(normalizeBookingFutureOpenDays(service.bookingFutureOpenDays), 14);
  const now = Date.now();
  const leadMillis = normalizeBookingLeadHours(service.bookingLeadHours) * 60 * 60 * 1000;
  const earliestTime = now + leadMillis;
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  const allTimes = availableStartTimesForDay(service.bookingTimeRangesJson);

  return Array.from({ length: dayCount }, (_, offset) => {
    const currentDate = new Date(today);
    currentDate.setDate(today.getDate() + offset);

    const timeOptions = allTimes.filter((time) => {
      const minutes = parseMinutes(time);
      if (minutes === null) return false;
      const slotDate = new Date(currentDate);
      slotDate.setHours(Math.floor(minutes / 60), minutes % 60, 0, 0);
      return slotDate.getTime() >= earliestTime;
    });

    return {
      key: toDateKey(currentDate),
      label: buildBookingDayLabel(currentDate, today),
      description: `${currentDate.getMonth() + 1}月${currentDate.getDate()}日 ${bookingWeekdayLabels[currentDate.getDay()]}`,
      timeOptions,
    };
  }).filter((item) => item.timeOptions.length > 0);
};

const buildPaymentPageUrl = (params: Record<string, string | number>) =>
  `/pages/payment/index?${Object.entries(params)
    .map(([key, value]) => `${key}=${encodeURIComponent(String(value))}`)
    .join('&')}`;

function ServiceDetailPage() {
  const serviceId = Taro.getCurrentInstance().router?.params?.id ?? '';
  const [service, setService] = useState<ServiceDetail | null>(null);
  const [selectedPlanIndex, setSelectedPlanIndex] = useState(0);
  const [currentImage, setCurrentImage] = useState(0);
  const [favorite, setFavorite] = useState(false);
  const [bookingSheetVisible, setBookingSheetVisible] = useState(false);
  const [selectedBookingDateKey, setSelectedBookingDateKey] = useState('');
  const [selectedBookingTime, setSelectedBookingTime] = useState('');

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
            prepaymentPercent: Number(data.prepayment_percent ?? 0),
            bookingTimeRangesJson: typeof data.booking_time_ranges_json === 'string' ? data.booking_time_ranges_json : '',
            bookingLeadHours: Number(data.booking_lead_hours ?? 0),
            bookingFutureOpenDays: Number(data.booking_future_open_days ?? 30),
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
  const bookingDayOptions = useMemo(() => buildBookingDayOptions(service), [service]);
  const selectedBookingDay = useMemo(
    () => bookingDayOptions.find((item) => item.key === selectedBookingDateKey) ?? bookingDayOptions[0],
    [bookingDayOptions, selectedBookingDateKey]
  );
  const currentPreviewImage = service.images[currentImage] ?? service.images[0] ?? '';

  useEffect(() => {
    if (!bookingDayOptions.length) {
      setSelectedBookingDateKey('');
      return;
    }
    if (!bookingDayOptions.some((item) => item.key === selectedBookingDateKey)) {
      setSelectedBookingDateKey(bookingDayOptions[0].key);
    }
  }, [bookingDayOptions, selectedBookingDateKey]);

  useEffect(() => {
    if (!selectedBookingDay) {
      setSelectedBookingTime('');
      return;
    }
    if (!selectedBookingDay.timeOptions.includes(selectedBookingTime)) {
      setSelectedBookingTime(selectedBookingDay.timeOptions[0] ?? '');
    }
  }, [selectedBookingDay, selectedBookingTime]);

  const handlePreviewImages = (index: number) => {
    if (!service.images.length) return;
    Taro.previewImage({
      current: service.images[index] ?? currentPreviewImage,
      urls: service.images,
    });
  };

  const handleBookingConfirm = () => {
    if (!service) return;
    if (!selectedBookingDay || !selectedBookingTime) {
      Taro.showToast({ title: '请选择预期时间', icon: 'none' });
      return;
    }

    setBookingSheetVisible(false);
    Taro.navigateTo({
      url: buildPaymentPageUrl({
        serviceId: service.id,
        title: service.title,
        planTitle: selectedPlan.title,
        planSubtitle: selectedPlan.subtitle,
        price: selectedPlan.price,
        duration: selectedPlan.duration,
        bookingDate: selectedBookingDay.description,
        bookingTime: formatClockLabel(selectedBookingTime),
        location: service.location || '与主理人沟通确认',
        prepaymentPercent: service.prepaymentPercent,
      }),
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
          <Text className={styles.sectionTitle}>服务特点</Text>
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
              <Text className={styles.bookingSectionTitle}>预期时间</Text>
              {bookingDayOptions.length ? (
                <>
                  <ScrollView className={styles.bookingDateScroll} scrollX enhanced showScrollbar={false}>
                    <View className={styles.bookingDateRow}>
                      {bookingDayOptions.map((day) => (
                        <View
                          key={day.key}
                          className={`${styles.bookingDateChip} ${selectedBookingDay?.key === day.key ? styles.bookingDateChipActive : ''}`}
                          onClick={() => setSelectedBookingDateKey(day.key)}
                        >
                          <Text className={`${styles.bookingDateChipLabel} ${selectedBookingDay?.key === day.key ? styles.bookingDateChipLabelActive : ''}`}>
                            {day.label}
                          </Text>
                          <Text className={`${styles.bookingDateChipDesc} ${selectedBookingDay?.key === day.key ? styles.bookingDateChipDescActive : ''}`}>
                            {day.description}
                          </Text>
                        </View>
                      ))}
                    </View>
                  </ScrollView>
                  <View className={styles.bookingTimeWrap}>
                    {selectedBookingDay?.timeOptions.map((time) => (
                      <View
                        key={time}
                        className={`${styles.bookingTimeChip} ${selectedBookingTime === time ? styles.bookingTimeChipActive : ''}`}
                        onClick={() => setSelectedBookingTime(time)}
                      >
                        <Text className={`${styles.bookingTimeChipText} ${selectedBookingTime === time ? styles.bookingTimeChipTextActive : ''}`}>
                          {formatClockLabel(time)}
                        </Text>
                      </View>
                    ))}
                  </View>
                </>
              ) : (
                <View className={styles.bookingInfoCard}>
                  <Text className={styles.bookingInfoText}>当前暂无可预约时段，请稍后再试或联系主理人确认。</Text>
                </View>
              )}
            </View>

            <View
              className={styles.bookingConfirmButton}
              onClick={handleBookingConfirm}
            >
              <Text className={styles.bookingConfirmButtonText}>完成，去支付</Text>
            </View>
          </View>
        </View>
      ) : null}
    </View>
  );
}

export default ServiceDetailPage;
