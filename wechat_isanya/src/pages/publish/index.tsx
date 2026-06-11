import React from 'react';
import { Text, View } from '@tarojs/components';
import styles from './index.module.scss';

function PublishPage() {
  return (
    <View className={styles.page}>
      <View className={styles.card}>
        <Text className={styles.title}>发布服务</Text>
        <Text className={styles.desc}>
          后端已支持 /services/ 创建与更新。这个页面已留好入口，下一步可补表单、图片上传与价格配置。
        </Text>
      </View>
    </View>
  );
}

export default PublishPage;
