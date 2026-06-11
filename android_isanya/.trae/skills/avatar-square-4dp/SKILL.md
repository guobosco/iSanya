---
name: "avatar-square-4dp"
description: "Enforces 4dp rounded square avatars. Invoke when creating, updating, or reviewing any UI that shows user, group, or service avatars."
---

# Avatar Square 4dp

## 目标

统一项目中的头像样式：

- 所有头像都使用圆角为 `4.dp` 的方形样式
- 不再为头像使用圆形 `CircleShape`
- 适用于用户头像、群头像、服务号头像、消息会话头像、聊天头像

## 何时使用

在以下场景主动使用：

- 新增任何带头像的 UI
- 修改已有头像样式
- 做 UI 走查或代码 review，发现头像不是 `4.dp` 圆角方形

## 实施规则

1. 优先使用统一头像组件，例如 `CommonAvatar`
2. 如果直接写图片组件，头像外层必须使用：

```kotlin
RoundedCornerShape(4.dp)
```

3. 禁止把头像裁成：

```kotlin
CircleShape
```

4. 非头像元素不强制使用该规则，例如：

- 红点
- 状态指示器
- 装饰性圆形背景

## 推荐写法

```kotlin
Image(
    painter = painterResource(avatarRes),
    contentDescription = name,
    contentScale = ContentScale.Crop,
    modifier = Modifier
        .size(40.dp)
        .clip(RoundedCornerShape(4.dp))
)
```

## 检查清单

- 是否为头像而不是装饰图形
- 是否使用 `RoundedCornerShape(4.dp)`
- 是否误用了 `CircleShape`
- 同一页面里的头像样式是否一致
