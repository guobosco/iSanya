import React, { useMemo, useState } from 'react';
import Taro from '@tarojs/taro';
import { ScrollView, Text, View } from '@tarojs/components';
import styles from './index.module.scss';

type HomeTab = 'service' | 'experience';
type ServiceCard = {
  id: string;
  title: string;
  info: string;
  price: string;
  coverClass: string;
  favorite: boolean;
  category: string;
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

const categories = ['全部', '陪游', '旅拍', '按摩', '私厨', '租车', '健身', '派对'];

const allServices: ServiceCard[] = [
  { id: 'svc-01', title: '海棠湾一日陪游', info: '陪游 · 三亚 · 中文/英语', price: '¥199起', coverClass: styles.coverA, favorite: true, category: '陪游' },
  { id: 'svc-02', title: '蜈支洲包车出海线', info: '租车 · 海棠区 · 半日', price: '¥388起', coverClass: styles.coverB, favorite: false, category: '租车' },
  { id: 'svc-03', title: '酒店上门按摩', info: '按摩 · 三亚湾 · 60分钟', price: '¥168起', coverClass: styles.coverC, favorite: false, category: '按摩' },
  { id: 'svc-04', title: '琼味私厨到店体验', info: '私厨 · 吉阳区 · 4-6人', price: '¥258起', coverClass: styles.coverD, favorite: true, category: '私厨' },
  { id: 'svc-05', title: '日落跟拍轻旅拍', info: '旅拍 · 椰梦长廊 · 90分钟', price: '¥299起', coverClass: styles.coverE, favorite: false, category: '旅拍' },
  { id: 'svc-06', title: '海边健身私教陪练', info: '健身 · 小东海 · 1小时', price: '¥220起', coverClass: styles.coverF, favorite: false, category: '健身' },
];

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

function HomePage() {
  const [selectedTab, setSelectedTab] = useState<HomeTab>('service');
  const [selectedCategory, setSelectedCategory] = useState('全部');

  const tabs = useMemo(
    () => [
      { key: 'service' as const, label: '服务' },
      { key: 'experience' as const, label: '体验' },
    ],
    [],
  );

  const serviceColumns = useMemo(() => {
    const filtered = selectedCategory === '全部'
      ? allServices
      : allServices.filter((item: ServiceCard) => item.category === selectedCategory);
    return {
      left: filtered.filter((_, index) => index % 2 === 0),
      right: filtered.filter((_, index) => index % 2 === 1),
    };
  }, [selectedCategory]);

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
              {categories.map((item) => (
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
              {serviceColumns.left.map((item: ServiceCard) => (
                <View
                  key={item.id}
                  className={styles.feedCard}
                  onClick={() => Taro.navigateTo({ url: `/pages/service-detail/index?id=${item.id}` })}
                >
                  <View className={`${styles.feedCover} ${item.coverClass}`}>
                    <View className={`${styles.coverFavorite} ${item.favorite ? styles.coverFavoriteActive : ''}`}>
                      {item.favorite ? '♥' : '♡'}
                    </View>
                  </View>
                  <View className={styles.feedBody}>
                    <Text className={styles.feedTitle}>{item.title}</Text>
                    <Text className={styles.feedMeta}>{item.info}</Text>
                    <Text className={styles.feedPrice}>{item.price}</Text>
                  </View>
                </View>
              ))}
            </View>

            <View className={styles.waterfallColumn}>
              {serviceColumns.right.map((item: ServiceCard) => (
                <View
                  key={item.id}
                  className={styles.feedCard}
                  onClick={() => Taro.navigateTo({ url: `/pages/service-detail/index?id=${item.id}` })}
                >
                  <View className={`${styles.feedCover} ${item.coverClass}`}>
                    <View className={`${styles.coverFavorite} ${item.favorite ? styles.coverFavoriteActive : ''}`}>
                      {item.favorite ? '♥' : '♡'}
                    </View>
                  </View>
                  <View className={styles.feedBody}>
                    <Text className={styles.feedTitle}>{item.title}</Text>
                    <Text className={styles.feedMeta}>{item.info}</Text>
                    <Text className={styles.feedPrice}>{item.price}</Text>
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
