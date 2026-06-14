export default defineAppConfig({
  pages: [
    'pages/home/index',
    'pages/wishlist/index',
    'pages/messages/index',
    'pages/mine/index',
    'pages/service-detail/index',
    'pages/login/index',
    'pages/profile/index',
    'pages/publish/index',
  ],
  window: {
    backgroundTextStyle: 'dark',
    navigationBarBackgroundColor: '#FFFFFF',
    navigationBarTitleText: 'i三亚',
    navigationBarTextStyle: 'black',
    backgroundColor: '#F7F7F7',
  },
  tabBar: {
    color: '#7B707A',
    selectedColor: '#E0115F',
    backgroundColor: '#FFFFFF',
    borderStyle: 'black',
    list: [
      { pagePath: 'pages/home/index', text: 'i三亚' },
      { pagePath: 'pages/wishlist/index', text: '心愿单' },
      { pagePath: 'pages/messages/index', text: '消息' },
      { pagePath: 'pages/mine/index', text: '我的' },
    ],
  },
});
