import React, { useState } from 'react';
import Taro from '@tarojs/taro';
import { Button, Input, Text, View } from '@tarojs/components';
import styles from './index.module.scss';

function LoginPage() {
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');

  const handleLogin = () => {
    Taro.showToast({
      title: '真实登录待接入',
      icon: 'none',
    });
    void phone;
    void password;
  };

  return (
    <View className={styles.page}>
      <View className={styles.card}>
        <Text className={styles.title}>登录 爱野</Text>
        <Text className={styles.desc}>
          本地演示账号已移除；后续请直接接入真实 `/auth/token` 与用户信息接口。
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
          真实登录待接入
        </Button>
      </View>
    </View>
  );
}

export default LoginPage;
