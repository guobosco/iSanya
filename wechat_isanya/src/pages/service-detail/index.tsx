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

declare var process: any;

const resolveMediaUrl = (url?: string) => {
  if (!url) return '';
  if (url.startsWith('http://') || url.startsWith('https://')) return url;
  const baseStr = process.env.TARO_APP_API_BASE_URL || '';
  const base = baseStr.replace(/^['"](.*)['"]$/, '$1');
  return `${base.replace(/\/$/, '')}/${url.replace(/^\//, '')}`;
};

function ServiceDetailPage() {
  const serviceId = Taro.getCurrentInstance().router?.params?.id ?? '';
  const [service, setService] = useState<ServiceDetail | null>(null);
  const [selectedPlanIndex, setSelectedPlanIndex] = useState(0);
  const [currentImage, setCurrentImage] = useState(0);
  const [favorite, setFavorite] = useState(false);

  useLoad(() => {
    if (!serviceId) return;
    Taro.request({
      url: `${process.env.TARO_APP_API_BASE_URL}/services/${serviceId}`,
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
            hostName: data.creator || '神秘主理人',
            hostRole: '主理人',
            hostStats: '',
            verified: true,
            favorite: false,
            tags: data.category ? [data.category] : [],
            images: images.length ? images : ['https://via.placeholder.com/600x800?text=No+Image'],
            description: data.description ? data.description.split('\n') : ['暂无介绍'],
            notes: [
              '请提前确认服务时间。',
              '如遇极端天气，可协商改期。'
            ],
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
