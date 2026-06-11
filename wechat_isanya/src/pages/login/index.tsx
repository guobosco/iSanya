import React, { useState } from 'react';
import Taro from '@tarojs/taro';
import { Button, Input, Text, View } from '@tarojs/components';
import styles from './index.module.scss';

function LoginPage() {
  const [phone, setPhone] = useState('13800000001');
  const [password, setPassword] = useState('123456');

  const handleLogin = () => {
    Taro.setStorageSync('isanya_demo_phone', phone);
    Taro.showToast({
      title: '已写入本地演示账号',
      icon: 'success',
    });
    void password;
    Taro.switchTab({ url: '/pages/mine/index' });
  };

  return (
    <View className={styles.page}>
      <View className={styles.card}>
        <Text className={styles.title}>登录 iSanya</Text>
        <Text className={styles.desc}>
          当前先用本地演示态打通流程，下一步可直接替换为 /auth/token + /auth/me。
        </Text>
        <Input
          className={styles.input}
          type='number'
          value={phone}
          placeholder='手机号'
          onInput={(event) => setPhone(event.detail.value)}
        />
        <Input
          className={styles.input}
          password
          value={password}
          placeholder='密码'
          onInput={(event) => setPassword(event.detail.value)}
        />
        <Button className={styles.button} onClick={handleLogin}>
          登录并进入我的
        </Button>
      </View>
    </View>
  );
}

export default LoginPage;
