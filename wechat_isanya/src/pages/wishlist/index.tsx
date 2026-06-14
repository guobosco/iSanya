import React from 'react';
import Taro, { usePullDownRefresh } from '@tarojs/taro';
import { Text, View } from '@tarojs/components';
import styles from './index.module.scss';

function WishlistPage() {
  usePullDownRefresh(() => {
    Taro.stopPullDownRefresh();
  });

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

      <View style={{ padding: '220rpx 40rpx 0', textAlign: 'center' }}>
        <Text style={{ display: 'block', fontSize: '44rpx', fontWeight: 600, color: '#202020' }}>暂无真实心愿单数据</Text>
        <Text style={{ display: 'block', marginTop: '16rpx', fontSize: '28rpx', color: '#8d8d8d' }}>本地写死收藏分组已移除，后续接入真实心愿单接口后展示。</Text>
      </View>
    </View>
  );
}

export default WishlistPage;
