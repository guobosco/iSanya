import React, { useState } from 'react';
import Taro, { usePullDownRefresh } from '@tarojs/taro';
import { ScrollView, Text, View } from '@tarojs/components';
import styles from './index.module.scss';

const tabs = ['全部', '问询我的', '我问询的', '用户支持', '已归档'];

function MessagesPage() {
  const [selectedTab, setSelectedTab] = useState('全部');

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
          <View className={styles.actionButton} onClick={() => Taro.showToast({ title: '设置待接入', icon: 'none' })}>
            设
          </View>
        </View>
        <Text className={styles.headerTitle}>消息</Text>

        <ScrollView className={styles.tabScroll} scrollX enhanced showScrollbar={false}>
          <View className={styles.tabRow}>
            {tabs.map((item: string) => (
              <View
                key={item}
                className={item === selectedTab ? styles.tabChipActive : styles.tabChip}
                onClick={() => setSelectedTab(item)}
              >
                {item}
              </View>
            ))}
          </View>
        </ScrollView>
      </View>

      <View className={styles.emptyWrap}>
        <Text className={styles.emptyIcon}>聊</Text>
        <Text className={styles.emptyTitle}>暂无真实消息数据</Text>
        <Text className={styles.emptyDesc}>本地 mock 消息已移除，后续接入真实会话接口后展示。</Text>
      </View>
    </View>
  );
}

export default MessagesPage;
