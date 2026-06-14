import React, { useMemo, useState, useEffect } from 'react';
import Taro, { useLoad, usePullDownRefresh } from '@tarojs/taro';
import { ScrollView, Text, View, Image } from '@tarojs/components';
import styles from './index.module.scss';

type HomeTab = 'service' | 'experience';

type ServiceData = {
  id: string;
  title: string;
  description: string;
  cover_image_url: string;
  location: string;
  price_text: string;
  category: string;
  service_time: number;
};

type ExperienceItem = {
  id: string;
  title: string;
  meta: string;
  price: string;
  badge: string;
  coverClass: string;
};

type ExperienceSection = {
  id: string;
  title: string;
  items: ExperienceItem[];
};

const experienceSections: ExperienceSection[] = [
  {
    id: 'exp-01',
    title: '热门海岛',
    items: [
      { id: 'exp-01-1', title: '蜈支洲跳岛体验', meta: '热门 · 半日', price: '¥268起', badge: '热门', coverClass: styles.expCoverA },
      { id: 'exp-01-2', title: '后海冲浪入门', meta: '精选 · 2小时', price: '¥198起', badge: '精选', coverClass: styles.expCoverB },
      { id: 'exp-01-3', title: '分界洲轻潜打卡', meta: '热门 · 半日', price: '¥328起', badge: '热门', coverClass: styles.expCoverC },
    ],
  },
  {
    id: 'exp-02',
    title: '城市松弛',
    items: [
      { id: 'exp-02-1', title: '亚龙湾晨跑地图', meta: '精选 · 1小时', price: '¥128起', badge: '精选', coverClass: styles.expCoverD },
      { id: 'exp-02-2', title: '夜游市场吃喝路线', meta: '热门 · 晚间', price: '¥158起', badge: '热门', coverClass: styles.expCoverE },
      { id: 'exp-02-3', title: '小众咖啡馆巡游', meta: '精选 · 下午', price: '¥118起', badge: '精选', coverClass: styles.expCoverF },
    ],
  },
  {
    id: 'exp-03',
    title: '拍照出片',
    items: [
      { id: 'exp-03-1', title: '椰梦长廊日落大片', meta: '热门 · 90分钟', price: '¥299起', badge: '热门', coverClass: styles.expCoverB },
      { id: 'exp-03-2', title: '海边胶片感双人拍', meta: '精选 · 2小时', price: '¥368起', badge: '精选', coverClass: styles.expCoverA },
      { id: 'exp-03-3', title: '免道具轻写真', meta: '精选 · 60分钟', price: '¥228起', badge: '精选', coverClass: styles.expCoverD },
    ],
  },
];

declare var process: any;

const resolveMediaUrl = (url?: string) => {
  if (!url) return '';
  if (url.startsWith('http://') || url.startsWith('https://')) return url;
  const baseStr = process.env.TARO_APP_API_BASE_URL || '';
  const base = baseStr.replace(/^['"](.*)['"]$/, '$1');
  return `${base.replace(/\/$/, '')}/${url.replace(/^\//, '')}`;
};

function HomePage() {
  const [selectedTab, setSelectedTab] = useState<HomeTab>('service');
  const [selectedCategory, setSelectedCategory] = useState('全部');
  const [services, setServices] = useState<ServiceData[]>([]);

  const fetchServices = (isRefresh = false) => {
    Taro.request({
      url: `${process.env.TARO_APP_API_BASE_URL}/services/discovery`,
      method: 'GET',
      success: (res: any) => {
        if (res.statusCode === 200) {
          setServices(res.data);
        } else {
          Taro.showToast({ title: '加载失败', icon: 'none' });
        }
      },
      fail: () => {
        Taro.showToast({ title: '网络错误', icon: 'none' });
      },
      complete: () => {
        if (isRefresh) {
          setTimeout(() => {
            Taro.stopPullDownRefresh();
          }, 800);
        }
      }
    });
  };

  useLoad(() => {
    fetchServices();
  });

  usePullDownRefresh(() => {
    fetchServices(true);
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
        </>
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
                      <View className={`${styles.experienceCover} ${item.coverClass}`}>
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
    </View>
  );
}

export default HomePage;
