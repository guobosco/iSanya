# wechat_isanya

iSanya 的微信小程序端，基于 Taro + React + TypeScript 规划。

## 当前范围

- 对齐现有 backend 已支持能力：登录、我的、服务发现、心愿单、消息。
- 首版 tabBar：首页、心愿单、消息、我的。
- 二级页：服务详情、登录、个人主页、发布服务。
- 当后端不可达时，页面会自动回落到本地 Mock 数据，方便先做视觉和交互开发。

## 本地启动

当前沙箱环境没有 Node，因此本次仅完成项目代码搭建，未执行模板初始化、依赖安装和预览。
你可以在本机安装 Node.js 18+ 后进入 `wechat_isanya/` 执行：

```bash
npm install
npm run dev:weapp
```

## 后端联调

默认读取 `TARO_APP_API_BASE_URL`，推荐统一配置为 `https://123.57.67.153`。

- 微信开发者工具内也统一使用 `https://123.57.67.153`
- 后端需先运行 `backend/main.py`

## 测试账号

- 手机号：`13800000001`
- 密码：`123456`
