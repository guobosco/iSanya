import React, { useMemo, useState } from 'react';
import Taro, { useLoad, usePullDownRefresh } from '@tarojs/taro';
import { ScrollView, Text, View, Image } from '@tarojs/components';
import styles from './index.module.scss';

type HomeTab = 'service' | 'experience';

type ServiceData = {
  id: string;
  title: string;
  description: string;
  cover_image_url: string;
  image_urls?: string[];
  location: string;
  price_text: string;
  category: string;
  service_time: number;
};

type ExperienceData = {
  id: string;
  title: string;
  category: string;
  cover_image_url: string;
  image_urls?: string[];
  location: string;
  price_text: string;
  duration_text: string;
  badge_text: string;
};

type ExperienceItem = {
  id: string;
  title: string;
  meta: string;
  price: string;
  badge: string;
  imageUrl: string;
};

type ExperienceSection = {
  id: string;
  title: string;
  items: ExperienceItem[];
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

const normalizeText = (value?: string) => (value || '').trim();

function HomePage() {
  const [selectedTab, setSelectedTab] = useState<HomeTab>('service');
  const [selectedCategory, setSelectedCategory] = useState('全部');
  const [services, setServices] = useState<ServiceData[]>([]);
  const [experiences, setExperiences] = useState<ExperienceData[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  const fetchHomeData = async (isRefresh = false) => {
    setIsLoading(true);
    try {
      let nextServices: ServiceData[] = [];
      let nextExperiences: ExperienceData[] = [];

      try {
        const serviceResult: any = await Taro.request({
          url: `${apiBaseUrl}/services/discovery`,
          method: 'GET',
        });
        if (serviceResult.statusCode === 200 && Array.isArray(serviceResult.data)) {
          nextServices = serviceResult.data;
        }
      } catch {}

      try {
        const experienceResult: any = await Taro.request({
          url: `${apiBaseUrl}/experiences/discovery`,
          method: 'GET',
        });
        if (experienceResult.statusCode === 200 && Array.isArray(experienceResult.data)) {
          nextExperiences = experienceResult.data;
        }
      } catch {}

      setServices(nextServices);
      setExperiences(nextExperiences);
    } catch {
      setServices([]);
      setExperiences([]);
      Taro.showToast({ title: '网络错误', icon: 'none' });
    } finally {
      setIsLoading(false);
      if (isRefresh) {
        setTimeout(() => {
          Taro.stopPullDownRefresh();
        }, 300);
      }
    }
  };

  useLoad(() => {
    void fetchHomeData();
  });

  usePullDownRefresh(() => {
    void fetchHomeData(true);
  });

  const categories = useMemo(() => {
    const cats = new Set(services.map((s: ServiceData) => s.category).filter(Boolean));
    return ['全部', ...Array.from(cats)];
  }, [services]);

  const tabs = useMemo(
    () => [
      { key: 'service' as const, label: '服务' },
      { key: 'experience' as const, label: '体验' },
    ],
    [],
  );

  const serviceColumns = useMemo(() => {
    const filtered = selectedCategory === '全部'
      ? services
      : services.filter((item: ServiceData) => item.category === selectedCategory);
    return {
      left: filtered.filter((_: any, index: number) => index % 2 === 0),
      right: filtered.filter((_: any, index: number) => index % 2 === 1),
    };
  }, [services, selectedCategory]);

  const experienceSections = useMemo(() => {
    const groups = new Map<string, ExperienceItem[]>();
    experiences.forEach((item: ExperienceData) => {
      const title = normalizeText(item.category) || '其他体验';
      const imageUrl = resolveMediaUrl(
        normalizeText(item.cover_image_url) || item.image_urls?.find((url) => normalizeText(url)) || ''
      );
      const metaParts = [normalizeText(item.duration_text), normalizeText(item.location)].filter(Boolean);
      const card: ExperienceItem = {
        id: item.id,
        title: item.title,
        meta: metaParts.join(' · ') || '查看详情',
        price: normalizeText(item.price_text) || '价格待沟通',
        badge: normalizeText(item.badge_text) || title,
        imageUrl,
      };
      groups.set(title, [...(groups.get(title) || []), card]);
    });

    return Array.from(groups.entries()).map(([title, items]) => ({
      id: title,
      title,
      items,
    }));
  }, [experiences]);

  const showServiceEmpty = !isLoading && serviceColumns.left.length === 0 && serviceColumns.right.length === 0;
  const showExperienceEmpty = !isLoading && experienceSections.length === 0;

  return (
    <View className={styles.page}>
      <View className={styles.topBar}>
        <View className={styles.topBarPlaceholder} />
        <View className={styles.tabSwitch}>
          {tabs.map((tab: { key: HomeTab; label: string }) => (
            <View key={tab.key} className={styles.topTab} onClick={() => setSelectedTab(tab.key)}>
              <Text className={`${styles.topTabText} ${tab.key === selectedTab ? styles.topTabTextActive : ''}`}>
                {tab.label}
              </Text>
              <View className={`${styles.topTabLine} ${tab.key === selectedTab ? styles.topTabLineActive : ''}`} />
            </View>
          ))}
        </View>
        <View className={styles.topBarAction} onClick={() => Taro.showToast({ title: '搜索待接入', icon: 'none' })}>
          <View className={styles.searchIcon}>
            <View className={styles.searchIconCircle} />
            <View className={styles.searchIconHandle} />
          </View>
        </View>
      </View>

      {selectedTab === 'service' ? (
        <>
          <ScrollView className={styles.filterScroll} scrollX enhanced showScrollbar={false}>
            <View className={styles.filterRow}>
              {categories.map((item: string) => (
                <View
                  key={item}
                  className={item === selectedCategory ? styles.filterChipActive : styles.filterChip}
                  onClick={() => setSelectedCategory(item)}
                >
                  {item}
                </View>
              ))}
            </View>
          </ScrollView>

          {showServiceEmpty ? (
            <View style={{ padding: '120rpx 40rpx', textAlign: 'center', color: '#8d8d8d' }}>
              <Text style={{ display: 'block', fontSize: '32rpx', color: '#202020', fontWeight: 600 }}>暂无真实服务数据</Text>
              <Text style={{ display: 'block', marginTop: '16rpx', fontSize: '26rpx' }}>当前不会再加载本地假数据。</Text>
            </View>
          ) : (
            <View className={styles.waterfall}>
              <View className={styles.waterfallColumn}>
                {serviceColumns.left.map((item: ServiceData) => (
                  <View
                    key={item.id}
                    className={styles.feedCard}
                    onClick={() => Taro.navigateTo({ url: `/pages/service-detail/index?id=${item.id}` })}
                  >
                    <View className={styles.feedCover}>
                      {item.cover_image_url && <Image src={resolveMediaUrl(item.cover_image_url)} mode="aspectFill" style={{ width: '100%', height: '100%', position: 'absolute', top: 0, left: 0 }} />}
                      <View className={styles.coverFavorite}>♡</View>
                    </View>
                    <View className={styles.feedBody}>
                      <Text className={styles.feedTitle}>{item.title}</Text>
                      <Text className={styles.feedMeta}>{item.category} · {item.location}</Text>
                      <Text className={styles.feedPrice}>{item.price_text}</Text>
                    </View>
                  </View>
                ))}
              </View>

              <View className={styles.waterfallColumn}>
                {serviceColumns.right.map((item: ServiceData) => (
                  <View
                    key={item.id}
                    className={styles.feedCard}
                    onClick={() => Taro.navigateTo({ url: `/pages/service-detail/index?id=${item.id}` })}
                  >
                    <View className={styles.feedCover}>
                      {item.cover_image_url && <Image src={resolveMediaUrl(item.cover_image_url)} mode="aspectFill" style={{ width: '100%', height: '100%', position: 'absolute', top: 0, left: 0 }} />}
                      <View className={styles.coverFavorite}>♡</View>
                    </View>
                    <View className={styles.feedBody}>
                      <Text className={styles.feedTitle}>{item.title}</Text>
                      <Text className={styles.feedMeta}>{item.category} · {item.location}</Text>
                      <Text className={styles.feedPrice}>{item.price_text}</Text>
                    </View>
                  </View>
                ))}
              </View>
            </View>
          )}
        </>
      ) : (
        <>
          {showExperienceEmpty ? (
            <View style={{ padding: '120rpx 40rpx', textAlign: 'center', color: '#8d8d8d' }}>
              <Text style={{ display: 'block', fontSize: '32rpx', color: '#202020', fontWeight: 600 }}>暂无真实体验数据</Text>
              <Text style={{ display: 'block', marginTop: '16rpx', fontSize: '26rpx' }}>当前不会再显示本地写死体验内容。</Text>
            </View>
          ) : (
            <>
              {experienceSections.map((section: ExperienceSection) => (
                <View key={section.id} className={styles.experienceSection}>
                  <View className={styles.experienceHeader}>
                    <Text className={styles.experienceTitle}>{section.title}</Text>
                    <View className={styles.sectionMore}>
                      <Text className={styles.sectionMoreText}>›</Text>
                    </View>
                  </View>
                  <ScrollView scrollX enhanced showScrollbar={false}>
                    <View className={styles.experienceRow}>
                      {section.items.map((item: ExperienceItem) => (
                        <View
                          key={item.id}
                          className={styles.experienceCard}
                          onClick={() => Taro.showToast({ title: '体验详情待接入', icon: 'none' })}
                        >
                          <View className={styles.experienceCover}>
                            {item.imageUrl ? (
                              <Image src={item.imageUrl} mode="aspectFill" style={{ width: '100%', height: '100%' }} />
                            ) : null}
                            <View className={styles.experienceBadge}>{item.badge}</View>
                            <View className={styles.experienceFav}>♡</View>
                          </View>
                          <Text className={styles.experienceCardTitle}>{item.title}</Text>
                          <Text className={styles.experienceCardMeta}>{item.meta}</Text>
                          <Text className={styles.experienceCardPrice}>{item.price}</Text>
                        </View>
                      ))}
                    </View>
                  </ScrollView>
                </View>
              ))}
            </>
          )}
        </>
      )}
    </View>
  );
}

export default HomePage;
