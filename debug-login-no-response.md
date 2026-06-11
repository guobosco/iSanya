# Debug Session: login-no-response

- Status: OPEN
- Symptom: iOS 端点击登录按钮后没有明显反应。
- Expected: 点击登录后应出现加载态，并发起 `/auth/token` 与 `/auth/me`，成功后进入主界面，失败则展示错误文案。

## Hypotheses

1. 登录按钮实际上处于不可点击状态，或因为某个条件被 `disabled`，用户看到像“没反应”。
2. 按钮点击已进入 `login()`，但 `isLoading`/`errorMessage` 没有正确更新到 UI，导致表面无反馈。
3. 网络请求已发出，但 BaseURL、ATS、后端返回错误或表单参数不匹配，且错误没有被正确展示。
4. 登录成功后会话状态没有写回 `AppSession`，所以界面没有切换到主 Tab。
5. 主线程/并发边界有问题，`@Published` 更新未稳定落到界面。

## Plan

- 为登录按钮点击、ViewModel 登录流程、API 请求与会话写入增加最小化调试日志。
- 复现一次点击登录，结合前端状态与后端日志确认链路断点。
- 只在证据明确后做最小修复。
