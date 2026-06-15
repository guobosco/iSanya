# [OPEN] android-home-empty

## 症状
- Android 客户端首页“服务”页显示“暂无内容”
- 已切换到 `https://123.57.67.153`，编译通过，但页面仍无数据
- 期望结果：首页能展示后端 `/services/discovery` 返回的公开服务数据

## 可证伪假设
- 假设 1：安卓端实际请求的 `BuildConfig.API_BASE_URL` 不是 `https://123.57.67.153/`
- 假设 2：首页确实发起了 `/services/discovery`，但响应为空数组
- 假设 3：首页请求成功，但返回数据在写入 Room 前后被过滤掉了
- 假设 4：首页没有走到刷新链路，导致根本没有触发发现流请求
- 假设 5：发现流请求失败了，但 UI 吞掉了异常，只显示空态

## 调试计划
- 先在首页刷新入口、发现流请求、Room 写入前后增加埋点
- 复现一次首页空数据场景
- 根据日志确认请求地址、返回数量、写库数量和 UI 读取数量
- 仅在证据明确后再做最小修复

## 调试服务器
- URL: `http://192.168.43.160:7777/event`
- 日志文件: `.dbg/trae-debug-log-android-home-empty.ndjson`

## 当前进度
- 已启动远程 Debug Server
- 已在首页刷新入口、发现接口、Room 写入后、UI 读取数量处加入埋点
- 已读取 pre-fix 日志，确认首页确实发起请求，基地址为 `https://123.57.67.153/`
- 已确认失败原因是 `SSLHandshakeException: Trust anchor for certification path not found`
- 已增加 Android 调试环境下的临时 TLS 放宽修复，等待 post-fix 复现验证
- 已读取 post-fix 日志，确认 TLS 已通过，但真实接口返回 `HTTP 500 Internal Server Error`
- 已本地复现 `schemas.Service` 对 `NULL` 服务字段的校验失败，并补上服务响应模型空值兜底
- 已在公网再次验证 `https://123.57.67.153/services/discovery?skip=0&limit=5`，当前仍返回 `500`
- 用户已在服务器本机验证 `http://127.0.0.1:8000/services/discovery?skip=0&limit=5`，结果仍为 `500`
- 服务器本机再次验证 `http://127.0.0.1:8000/services/discovery?skip=0&limit=5`，现已返回 `200` 和服务数据
- 公网再次验证 `https://123.57.67.153/services/discovery?skip=0&limit=5`，现已返回 `200` 和服务数据
- 已确认图片公网静态资源 `https://123.57.67.153/static/...` 可直接访问并返回 `200 image/jpeg`
- 已修复 Android 图片加载链路未复用调试期 TLS 放宽的问题，并完成编译验证
