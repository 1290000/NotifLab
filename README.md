# Notiflab（息匣通知实验室）

给 **Quieta（息匣）** 做真机验收用的配套测试 App。只负责制造可控通知噪音，不做业务推送。

工程与产品约定见 [`AGENTS.md`](./AGENTS.md)。

## 能力（最小可用版）

- 预置 8 个 `lab.*` 通知渠道，覆盖 HIGH / DEFAULT / LOW / MIN
- 每个渠道一键发送测试通知
- 「连发全部」压测多渠道打扰
- 「重置渠道」：删除全部 `lab.*` 后重建
- 「清空通知」：取消本应用全部通知
- Android 13+ 运行时申请 `POST_NOTIFICATIONS`

## 与 Quieta 的联调

1. 安装本 App → 授予通知权限 → 点「连发全部」
2. 打开 Quieta 盘点，应看到 `app.quieta.notiflab.debug` 下 8 个渠道及 importance 分布
3. Quieta 批量静音 `lab.marketing.*` 后，再点营销渠道「发送」验证静音
4. Quieta 开启「新渠道自动静音」后，可用「重置渠道」触发重建做拦截回归

## 构建

```bat
gradlew.bat :app:assembleDebug
```

产物：`app/build/outputs/apk/debug/app-debug.apk`

## 包名

- 正式：`app.quieta.notiflab`
- Debug：`app.quieta.notiflab.debug`（便于与正式包并存）

## 边界

- 不上传任何数据
- 通知正文固定带 `Quieta Lab` 标识
- 不依赖 Shizuku / Root，走标准 NotificationChannel API
