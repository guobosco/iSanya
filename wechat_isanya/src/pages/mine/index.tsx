import React from 'react';
import Taro from '@tarojs/taro';
import { Text, View } from '@tarojs/components';
import styles from './index.module.scss';

type MineCell = {
  title: string;
  icon: string;
  badge?: boolean;
};

const groups: MineCell[][] = [
  [
    { title: '我预定的', icon: '订' },
    { title: '我浏览的', icon: '览' },
  ],
  [
    { title: '我发布的', icon: '发' },
    { title: '我的接单', icon: '单' },
    { title: '我的收入', icon: '收' },
    { title: '评价我的', icon: '评' },
  ],
  [{ title: '权限设置', icon: '设', badge: true }],
];

function MinePage() {
  const phone = Taro.getStorageSync<string>('isanya_demo_phone') || '未登录';

  return (
    <View className={styles.page}>
      <View className={styles.header}>
        <View className={styles.headerActions}>
          <View className={styles.actionButton} onClick={() => Taro.showToast({ title: '设置待接入', icon: 'none' })}>
            设
          </View>
        </View>
        <Text className={styles.headerTitle}>个人资料</Text>
      </View>

      <View className={styles.profileCard}>
        <View className={styles.profileMain}>
          <View className={styles.profileLeft}>
            <View className={styles.avatar}>P</View>
            <Text className={styles.profileName}>PeiPei Demo</Text>
            <Text className={styles.profileRegion}>当前账号：{phone}</Text>
          </View>
          <View className={styles.profileRight}>
            <View className={styles.statBlock}>
              <Text className={styles.statLabel}>i三亚 ID</Text>
              <Text className={styles.statValue}>ISY-1024</Text>
            </View>
            <View className={styles.divider} />
            <View className={styles.statBlock}>
              <Text className={styles.statLabel}>性别</Text>
              <Text className={styles.statValue}>女</Text>
            </View>
            <View className={styles.divider} />
            <View className={styles.statBlock}>
              <Text className={styles.statLabel}>加入 i三亚</Text>
              <Text className={styles.statValue}>2年</Text>
            </View>
          </View>
        </View>
        <Text className={styles.moreHint}>更多资料</Text>
      </View>

      {groups.map((group: MineCell[], index: number) => (
        <View key={`group-${index}`} className={styles.cellGroup}>
          {group.map((item: MineCell) => (
            <View
              key={item.title}
              className={styles.cellItem}
              onClick={() => Taro.showToast({ title: `${item.title}待接入`, icon: 'none' })}
            >
              <View className={styles.cellLeft}>
                <View className={styles.cellIcon}>{item.icon}</View>
                <Text className={styles.cellTitle}>{item.title}</Text>
              </View>
              <View className={styles.cellRight}>
                {item.badge ? <View className={styles.cellBadge} /> : null}
                <Text className={styles.cellArrow}>›</Text>
              </View>
            </View>
          ))}
        </View>
      ))}

      <View className={styles.publishBar} onClick={() => Taro.navigateTo({ url: '/pages/publish/index' })}>
        <View className={styles.publishIcon}>发</View>
        <View className={styles.publishText}>
          <Text className={styles.publishTitle}>发布「服务」或「体验」</Text>
          <Text className={styles.publishDesc}>填写表单信息即可上架接单</Text>
        </View>
      </View>
    </View>
  );
}

export default MinePage;
