import React, { useState } from 'react';
import Taro from '@tarojs/taro';
import { Text, View } from '@tarojs/components';
import styles from './index.module.scss';

type WishlistService = {
  id: string;
  title: string;
  meta: string;
  price: string;
};

type WishlistGroup = {
  id: string;
  name: string;
  count: number;
  coverClass: string;
  services: WishlistService[];
};

const wishlistGroups: WishlistGroup[] = [
  {
    id: 'grp-01',
    name: '海边漫游',
    count: 3,
    coverClass: styles.wishCoverA,
    services: [
      { id: 'svc-01', title: '亚龙湾陪游半日线', meta: '陪游 · 第一次来三亚', price: '¥158起' },
      { id: 'svc-02', title: '海棠湾包车出海', meta: '租车 · 半日路线', price: '¥388起' },
      { id: 'svc-03', title: '椰梦长廊落日旅拍', meta: '旅拍 · 90分钟', price: '¥299起' },
    ],
  },
  {
    id: 'grp-02',
    name: '松弛休息',
    count: 2,
    coverClass: styles.wishCoverB,
    services: [
      { id: 'svc-04', title: '酒店上门按摩', meta: '按摩 · 60分钟', price: '¥168起' },
      { id: 'svc-05', title: '海边健身私教陪练', meta: '健身 · 1小时', price: '¥220起' },
    ],
  },
  {
    id: 'grp-03',
    name: '好吃聚餐',
    count: 2,
    coverClass: styles.wishCoverC,
    services: [
      { id: 'svc-06', title: '琼味私厨到店体验', meta: '私厨 · 4-6人', price: '¥258起' },
      { id: 'svc-07', title: '夜市美食路线向导', meta: '陪吃 · 晚间', price: '¥128起' },
    ],
  },
  {
    id: 'grp-04',
    name: '想拍出片',
    count: 1,
    coverClass: styles.wishCoverD,
    services: [{ id: 'svc-08', title: '海边胶片感双人拍', meta: '旅拍 · 2小时', price: '¥368起' }],
  },
];

function WishlistPage() {
  const [selectedGroup, setSelectedGroup] = useState<WishlistGroup | null>(null);

  return (
    <View className={styles.page}>
      <View className={styles.header}>
        <View className={styles.headerActions}>
          <View className={styles.actionButton} onClick={() => Taro.showToast({ title: '搜索待接入', icon: 'none' })}>
            搜
          </View>
          <View className={styles.actionButton} onClick={() => Taro.showToast({ title: '分组设置待接入', icon: 'none' })}>
            设
          </View>
        </View>
        <Text className={styles.headerTitle}>心愿单</Text>
      </View>

      {selectedGroup ? (
        <>
          <View className={styles.groupBackBar} onClick={() => setSelectedGroup(null)}>
            <Text className={styles.backArrow}>‹</Text>
            <Text className={styles.groupBackTitle}>{selectedGroup.name}</Text>
            <Text className={styles.groupBackCount}>{selectedGroup.services.length}个</Text>
          </View>

          <View className={styles.serviceList}>
            {selectedGroup.services.map((item: WishlistService) => (
              <View key={item.id} className={styles.expandedCard}>
                <View className={styles.expandedCover}>
                  <View className={styles.chatPill} onClick={() => Taro.showToast({ title: '聊一聊待接入', icon: 'none' })}>
                    聊一聊
                  </View>
                  <View className={styles.favoriteMark}>收藏</View>
                </View>
                <View className={styles.expandedBody}>
                  <Text className={styles.expandedTitle}>{item.title}</Text>
                  <Text className={styles.expandedMeta}>{item.meta}</Text>
                  <Text className={styles.expandedPrice}>{item.price}</Text>
                </View>
              </View>
            ))}
          </View>
        </>
      ) : (
        <View className={styles.groupGrid}>
          {wishlistGroups.map((group: WishlistGroup) => (
            <View key={group.id} className={styles.groupCard} onClick={() => setSelectedGroup(group)}>
              <View className={`${styles.groupCover} ${group.coverClass}`} />
              <View className={styles.groupBody}>
                <Text className={styles.groupName}>{group.name}</Text>
                <Text className={styles.groupCount}>{group.count}个收藏</Text>
              </View>
            </View>
          ))}
        </View>
      )}
    </View>
  );
}

export default WishlistPage;
