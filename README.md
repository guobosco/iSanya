# iSanya 综合服务平台

iSanya 是一个跨平台的服务与社交应用，旨在通过服务发现、心愿单、实时消息等功能连接用户。本项目采用 Monorepo（单体仓库）结构进行管理，同时包含了统一的后端服务以及 Android、iOS 和微信小程序三个客户端端点。

## 📌 项目概览

- **多端覆盖**：支持原生 Android、原生 iOS 以及微信小程序。
- **统一后端**：基于 Python FastAPI 构建的高性能 RESTful 和 WebSocket 服务。
- **核心功能范围（MVP）**：
  - **服务发现与详情**：浏览、搜索及查看服务详细信息。
  - **发布服务**：用户可以发布自己提供的服务。
  - **心愿单**：收藏和管理感兴趣的服务。
  - **消息与聊天**：支持基于 WebSocket 的实时聊天。
  - **个人中心**：用户资料管理与媒体上传。
  - **地图能力**：集成高德地图 SDK（部分端适用）。

## 📁 目录结构

```text
iSanya/
├── backend/          # Python FastAPI 后端服务
├── android_isanya/   # 原生 Android 客户端 (Kotlin + Jetpack Compose)
├── ios_isanya/       # 原生 iOS 客户端 (SwiftUI)
└── wechat_isanya/    # 微信小程序端 (Taro + React)
```

## 🛠 技术栈介绍

### 1. 后端 (`backend/`)
- **Web 框架**：FastAPI
- **数据库**：SQLAlchemy ORM（默认 SQLite，支持切换 Postgres）
- **身份验证**：JWT Bearer、passlib (bcrypt) 密码哈希
- **功能特性**：覆盖用户、服务、心愿单、媒体上传的 REST API，以及支持实时聊天的 WebSocket 会话 (`/ws/chat?token=`)。

### 2. Android 端 (`android_isanya/`)
- **开发语言**：Kotlin
- **UI 框架**：Jetpack Compose + Navigation-Compose
- **网络请求**：Retrofit + OkHttp
- **本地存储**：Room 数据库
- **地图服务**：高德地图 SDK
- **实时通信**：WebSocket 客户端实现聊天功能

### 3. iOS 端 (`ios_isanya/`)
- **开发语言**：Swift
- **UI 框架**：SwiftUI
- **本地存储**：CoreData 持久化
- **地图服务**：高德地图能力
- **设计对齐**：UI 设计、信息架构和交互节奏全面对齐 Android 端。优先复刻首页、心愿单、消息、我的及详情页的关键交互。

### 4. 微信小程序 (`wechat_isanya/`)
- **开发框架**：Taro + React + TypeScript + CSS Modules
- **UI 结构**：MVP 骨架包含 4 个 TabBar（首页、心愿单、消息、我的），以及服务详情、登录、个人主页、发布服务等二级页面。
- **目标**：能力边界与后端现有接口对齐，提供轻量化的跨端访问体验。

## 🚀 本地开发与环境配置指南

三端统一通过公网 `nginx` 入口访问后端，客户端 API 地址默认使用 `https://123.57.67.153`，不再直接配置 `127.0.0.1`、`localhost` 或 `:8000`。

### 后端环境启动
```bash
cd backend
python3 -m venv .venv
source .venv/bin/activate  # Windows 用户使用: .venv\Scripts\activate
pip install -r requirements.txt
./run_dev.sh               # 或者使用 python main.py
```
*后端应用默认监听 `http://0.0.0.0:8000`，并由 `nginx` 对外统一转发到 `https://123.57.67.153`。联调时优先访问 `https://123.57.67.153/healthz` 检查是否启动成功。*

### Android 端开发
1. 使用 **Android Studio** 打开 `android_isanya` 目录。
2. **配置接口地址**：
   - 打开文件 `android_isanya/gradle.properties`。
   - 将 `DEV_API_BASE_URL` 修改为 `https://123.57.67.153/`。
3. 编译并运行在模拟器或真机上。

### iOS 端开发
1. 使用 **Xcode** 打开 iOS 工程（如 `ios_isanya/iSanya/iSanya.xcodeproj`）。
2. **配置接口地址**：
   - 打开配置文件 `ios_isanya/iSanya/Config/*.xcconfig` 或修改 `Info.plist` 中的相关配置。
   - 将 `ISANYA_BASE_URL` 修改为 `https://123.57.67.153`。
   - *当前客户端走 HTTPS 公网入口，通常不需要为 HTTP 联调额外放开 ATS。*
3. 编译并在 iOS 模拟器或 iPhone 真机上运行。

### 微信小程序开发
1. 确保安装了 **Node.js** 和 **Taro CLI**（版本 4.x）。
2. **安装依赖**：
   ```bash
   cd wechat_isanya
   npm install
   ```
3. **配置接口地址**：
   - 打开 `wechat_isanya/config/dev.ts` 文件。
   - 将 `TARO_APP_API_BASE_URL` 修改为 `https://123.57.67.153`。
4. **启动开发服务**：
   ```bash
   npm run dev:weapp
   ```
5. 打开 **微信开发者工具**，导入 `wechat_isanya` 目录，即可开始调试。

---

## 💡 多端本地 API 地址配置位置备忘

为了方便在多端同时调试，以下是各端 API 地址的集中配置位置清单：
- **Android**: `android_isanya/gradle.properties` 中的 `DEV_API_BASE_URL`
- **iOS**: `ios_isanya/iSanya/Config/*.xcconfig` 中的 `ISANYA_BASE_URL`
- **WeChat**: `wechat_isanya/config/dev.ts` 中的 `TARO_APP_API_BASE_URL`

> 当前推荐联调方式是统一走 `https://123.57.67.153`，避免客户端直接连 `:8000` 导致配置分叉。
