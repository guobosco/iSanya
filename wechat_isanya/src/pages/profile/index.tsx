import React from 'react';
import { Text, View } from '@tarojs/components';
import styles from './index.module.scss';

function ProfilePage() {
  return (
    <View className={styles.page}>
      <View className={styles.card}>
        <Text className={styles.title}>个人主页功能</Text>
        <Text className={styles.desc}>
          当前先保留页面壳子，后续可接入 /users/{'{'}id{'}'}、达人标签和服务列表展示。
        </Text>
      </View>
    </View>
  );
}

export default ProfilePage;
