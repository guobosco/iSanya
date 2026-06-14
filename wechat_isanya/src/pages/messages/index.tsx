import React, { useMemo, useState } from 'react';
import Taro, { usePullDownRefresh } from '@tarojs/taro';
import { ScrollView, Text, View } from '@tarojs/components';
import styles from './index.module.scss';

type MessageItem = {
  id: string;
  title: string;
  snippet: string;
  time: string;
  type: string;
  starred: boolean;
  archived: boolean;
};

const tabs = ['全部', '问询我的', '我问询的', '用户支持', '已归档'];

const allMessages: MessageItem[] = [
  { id: 'msg-01', title: '阿舟', snippet: '周六下午的地陪档期可以给你预留。', time: '12:30', type: '问询我的', starred: true, archived: false },
  { id: 'msg-02', title: '林晚', snippet: '日落旅拍建议你穿浅色衣服，出片会更干净。', time: '昨天', type: '我问询的', starred: false, archived: false },
  { id: 'msg-03', title: '平台客服', snippet: '你提交的资料已收到，我们会尽快审核。', time: '周一', type: '用户支持', starred: false, archived: false },
  { id: 'msg-04', title: '小满', snippet: '心愿单我看到了，可以帮你重新排个路线。', time: '06/06', type: '问询我的', starred: false, archived: true },
];

function MessagesPage() {
  const [selectedTab, setSelectedTab] = useState('全部');
  const filteredMessages = useMemo(() => {
    if (selectedTab === '全部') {
      return allMessages.filter((item: MessageItem) => !item.archived);
    }
    if (selectedTab === '已归档') {
      return allMessages.filter((item: MessageItem) => item.archived);
    }
    return allMessages.filter((item: MessageItem) => !item.archived && item.type === selectedTab);
  }, [selectedTab]);

  usePullDownRefresh(() => {
    // Simulate refresh delay since it's mock data right now
    setTimeout(() => {
      Taro.stopPullDownRefresh();
    }, 1000);
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

      {filteredMessages.length ? (
        <View className={styles.messageList}>
          {filteredMessages.map((item: MessageItem) => (
            <View key={item.id} className={styles.messageItem}>
              <View className={styles.avatar}>{item.title.slice(0, 1)}</View>
              <View className={styles.messageMain}>
                <View className={styles.messageTop}>
                  <Text className={styles.messageTitle}>{item.title}</Text>
                  <Text className={styles.messageTime}>{item.time}</Text>
                </View>
                <View className={styles.messageSnippetRow}>
                  <Text className={styles.messageSnippet}>{item.snippet}</Text>
                  {item.starred ? <Text className={styles.starMark}>星标</Text> : null}
                </View>
              </View>
            </View>
          ))}
        </View>
      ) : (
        <View className={styles.emptyWrap}>
          <Text className={styles.emptyIcon}>聊</Text>
          <Text className={styles.emptyTitle}>你没有任何消息</Text>
          <Text className={styles.emptyDesc}>你收到的新消息将显示在这里。</Text>
        </View>
      )}
    </View>
  );
}

export default MessagesPage;
