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
      {
        pagePath: 'pages/home/index',
        text: 'i三亚',
        iconPath: 'assets/tabbar/tab_home_normal.png',
        selectedIconPath: 'assets/tabbar/tab_home_selected.png',
      },
      {
        pagePath: 'pages/wishlist/index',
        text: '心愿单',
        iconPath: 'assets/tabbar/tab_wishlist_normal.png',
        selectedIconPath: 'assets/tabbar/tab_wishlist_selected.png',
      },
      {
        pagePath: 'pages/messages/index',
        text: '消息',
        iconPath: 'assets/tabbar/tab_messages_normal.png',
        selectedIconPath: 'assets/tabbar/tab_messages_selected.png',
      },
      {
        pagePath: 'pages/mine/index',
        text: '我的',
        iconPath: 'assets/tabbar/tab_mine_normal.png',
        selectedIconPath: 'assets/tabbar/tab_mine_selected.png',
      },
    ],
  },
});
