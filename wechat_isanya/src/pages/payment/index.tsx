import React, { useMemo, useState } from 'react';
import Taro from '@tarojs/taro';
import { Text, View } from '@tarojs/components';
import styles from './index.module.scss';

type PaymentMethod = 'wechat' | 'alipay' | 'offline';

type PaymentMethodOption = {
  id: PaymentMethod;
  title: string;
  subtitle: string;
  badge?: string;
};

const paymentMethods: PaymentMethodOption[] = [
  {
    id: 'wechat',
    title: '微信支付',
    subtitle: '推荐使用微信完成支付',
    badge: '推荐',
  },
  {
    id: 'alipay',
    title: '支付宝',
    subtitle: '支持支付宝安全付款',
  },
  {
    id: 'offline',
    title: '线下确认',
    subtitle: '先提交预约，由主理人确认后再支付',
  },
];

function PaymentPage() {
  const params = Taro.getCurrentInstance().router?.params ?? {};
  const [selectedMethod, setSelectedMethod] = useState<PaymentMethod>('wechat');

  const bookingTimeText = useMemo(() => {
    const date = typeof params.bookingDate === 'string' ? params.bookingDate : '';
    const time = typeof params.bookingTime === 'string' ? params.bookingTime : '';
    return [date, time].filter(Boolean).join(' ');
  }, [params.bookingDate, params.bookingTime]);

  const paymentSummaryText = useMemo(() => {
    const prepaymentPercent = Number(params.prepaymentPercent ?? 0);
    if (prepaymentPercent > 0) {
      return `当前需支付 ${prepaymentPercent}% 预付款，尾款以和主理人最终确认为准。`;
    }
    return '当前为全额支付，最终金额以页面展示为准。';
  }, [params.prepaymentPercent]);

  return (
    <View className={styles.page}>
      <View className={styles.header}>
        <View className={styles.backButton} onClick={() => Taro.navigateBack()}>
          <Text className={styles.backButtonText}>‹</Text>
        </View>
        <Text className={styles.headerTitle}>支付确认</Text>
        <View className={styles.headerPlaceholder} />
      </View>

      <View className={styles.section}>
        <Text className={styles.sectionTitle}>订单信息</Text>
        <View className={styles.summaryCard}>
          <View className={styles.summaryRow}>
            <Text className={styles.summaryLabel}>服务</Text>
            <Text className={styles.summaryValue}>{params.title || '未命名服务'}</Text>
          </View>
          <View className={styles.summaryRow}>
            <Text className={styles.summaryLabel}>方案</Text>
            <Text className={styles.summaryValue}>{params.planTitle || '标准方案'}</Text>
          </View>
          {params.planSubtitle ? (
            <View className={styles.summaryRow}>
              <Text className={styles.summaryLabel}>说明</Text>
              <Text className={styles.summaryValue}>{params.planSubtitle}</Text>
            </View>
          ) : null}
          {bookingTimeText ? (
            <View className={styles.summaryRow}>
              <Text className={styles.summaryLabel}>预期时间</Text>
              <Text className={styles.summaryValue}>{bookingTimeText}</Text>
            </View>
          ) : null}
          {params.location ? (
            <View className={styles.summaryRow}>
              <Text className={styles.summaryLabel}>地点</Text>
              <Text className={styles.summaryValue}>{params.location}</Text>
            </View>
          ) : null}
        </View>
      </View>

      <View className={styles.section}>
        <Text className={styles.sectionTitle}>支付方式</Text>
        <View className={styles.methodList}>
          {paymentMethods.map((method) => (
            <View
              key={method.id}
              className={`${styles.methodCard} ${selectedMethod === method.id ? styles.methodCardActive : ''}`}
              onClick={() => setSelectedMethod(method.id)}
            >
              <View className={styles.methodMain}>
                <View>
                  <View className={styles.methodTitleRow}>
                    <Text className={styles.methodTitle}>{method.title}</Text>
                    {method.badge ? <Text className={styles.methodBadge}>{method.badge}</Text> : null}
                  </View>
                  <Text className={styles.methodSubtitle}>{method.subtitle}</Text>
                </View>
                <View className={`${styles.methodRadio} ${selectedMethod === method.id ? styles.methodRadioActive : ''}`}>
                  {selectedMethod === method.id ? <View className={styles.methodRadioDot} /> : null}
                </View>
              </View>
            </View>
          ))}
        </View>
      </View>

      <View className={styles.section}>
        <Text className={styles.sectionTitle}>支付说明</Text>
        <View className={styles.noticeCard}>
          <Text className={styles.noticeText}>{paymentSummaryText}</Text>
          <Text className={styles.noticeText}>如遇改期、加项或额外费用，支付前请先和主理人确认。</Text>
        </View>
      </View>

      <View className={styles.bottomSpacer} />

      <View className={styles.bottomBar}>
        <View className={styles.amountWrap}>
          <Text className={styles.amountLabel}>待支付</Text>
          <Text className={styles.amountValue}>{params.price || '面议'}</Text>
          {params.duration ? <Text className={styles.amountMeta}>{params.duration}</Text> : null}
        </View>
        <View
          className={styles.payButton}
          onClick={() =>
            Taro.showToast({
              title: `已选择${paymentMethods.find((item) => item.id === selectedMethod)?.title ?? '支付方式'}`,
              icon: 'none',
            })
          }
        >
          <Text className={styles.payButtonText}>确认支付</Text>
        </View>
      </View>
    </View>
  );
}

export default PaymentPage;
