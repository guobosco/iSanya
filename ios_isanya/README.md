# iSanya iOS（SwiftUI）

本目录是 iSanya 的 iOS 版本源码骨架（SwiftUI 为主，CoreData 持久化，高德地图后续接入）。CoreData 目前使用代码定义的 Model（无需 .xcdatamodeld），后续也可按需要迁移为可视化模型文件。

## 1. 在 Xcode 创建工程

1. 打开 Xcode → Create a new Xcode project
2. 选择 iOS → App
3. Product Name：`iSanya`
4. Interface：SwiftUI
5. Language：Swift
6. 勾选 Use Core Data
7. 将工程创建在仓库目录：`ios_isanya/` 下（例如 `ios_isanya/iSanya/`）

## 2. 将本目录代码加入工程

将 `ios_isanya/iSanya/` 下的文件夹整体拖入 Xcode 工程（Create groups 勾选、并确保 Target Membership 选中 App target）。

目录结构（当前已生成）：
- `App/`：App 入口、RootView、Tab 壳、AppContainer
- `Core/`：Config、Network、Auth、Storage（CoreDataStack）
- `Domain/DTO/`：后端 DTO（Codable）
- `Features/`：各业务模块 View / ViewModel

## 3. 配置 BaseURL（开发环境）

在工程的 Info.plist 增加键：
- `ISANYA_BASE_URL`（String）
  - iOS 模拟器联调：`http://127.0.0.1:8000`
  - 真机联调：`http://{你的Mac局域网IP}:8000`（例如 `http://192.168.1.10:8000`）

不配置时默认使用 `http://127.0.0.1:8000`。

如果你希望按 Debug/Release 分环境配置，推荐用 Build Setting 注入：
1. 在 Target → Build Settings → User-Defined 新增 `ISANYA_BASE_URL`
2. 在 Debug/Release 分别填不同值
3. 在 Info.plist 的 `ISANYA_BASE_URL` 值填写 `$(ISANYA_BASE_URL)`

注意：后端是 http（非 https），开发环境需要配置 ATS（否则请求会被系统拦截）：
1. Info.plist 增加 `NSAppTransportSecurity`（Dictionary）
2. 其下增加 `NSAllowsArbitraryLoads`（Boolean）为 `YES`

## 4. 本地后端联调

后端目录在仓库 `backend/`。启动方式（macOS）：
1. `cd backend`
2. `python3 -m venv .venv`
3. `source .venv/bin/activate`
4. `pip install -r requirements.txt`
5. `python main.py`

验证服务是否可用：
- 浏览器打开 `http://127.0.0.1:8000/healthz`，应返回 `{"status":"ok", ...}`

iOS 模拟器联调：
- BaseURL 用 `http://127.0.0.1:8000`

真机联调：
- BaseURL 用 `http://{你的Mac局域网IP}:8000`
- 确保手机与 Mac 在同一 Wi-Fi
- macOS 防火墙允许 Python/uvicorn 监听端口（后端默认 host=0.0.0.0、port=8000）

## 5. 目前可用能力

- 已搭好 Root 路由：未登录 → 登录页；登录成功写入 Keychain，并拉取 /auth/me 写入 CoreData → 进入 Tab 壳
- 已实现 APIClient 雏形：
  - `GET /healthz`
  - `POST /auth/token`（form-urlencoded）
  - `GET /auth/me`（Bearer）

## 6. 下一步开发顺序

1. 用户资料落地（/auth/me、/users/{id} 更新、头像上传）
2. 服务发现与详情（/services/discovery、/services/{id}）
3. 心愿单（/me/wishlist/services）
4. 聊天（REST + /ws/chat）
5. 高德地图 SDK 接入
