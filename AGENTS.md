# Notiflab（息匣通知实验室）

给 **Quieta（息匣）** 做真机验收用的配套测试 App。只负责制造可控通知噪音（渠道 + 发送），不治理、不采集、不上传。

- 显示名：息匣通知实验室 / Notiflab
- 一句话：Notification farm for Quieta. / 给息匣制造可控通知噪音
- 仓库名：`NotifLab`
- `applicationId`：`app.quieta.notiflab`（debug 后缀 `.debug`）
- 作者：`1290000`（GitHub）
- 远程仓库：`https://github.com/1290000/NotifLab.git`
- 协议：与息匣同族，GPL-3.0-only（测试工具，不面向商店分发）
- 配套工程：`https://github.com/1290000/Quieta`（治理端）

---

## 1. 产品边界

**做：**

- 预置固定目录的 `lab.*` 通知渠道（多 importance、可区分语义）
- 每个渠道一键发送测试通知；可「连发全部」
- 「重置渠道」：删除本应用全部 `lab.*` 后按目录强制重建
- 「清空通知」：`cancelAll()` 本应用通知
- Android 13+ 运行时申请 `POST_NOTIFICATIONS`

**不做：**

- 不做通知治理（静音规则、批量改 importance、拦截新建）
- 不上传通知正文；无遥测、无网络
- 不依赖 Shizuku / Root / Dhizuku；只走标准 `NotificationChannel` / `notify`
- 不当通用压测工具或推送 SDK

**MVP 金路径：**

1. 预置 8 渠道  
2. 单渠道发送 + 连发全部  
3. 重置 / 清空  

**与 Quieta 的职责切分：**

| 项目 | 职责 |
|------|------|
| Notiflab | 制造噪音：建渠道、发通知 |
| Quieta | 观察与治理：盘点、静音/降级、自动拦截 |

**已交付（摘要）：** 8 渠道目录；发送 / 连发 / 重置 / 清空；`POST_NOTIFICATIONS`；K40s 真机联调（静音后发送不回弹、未静音渠道正常投递）。

**已约定、尚未完成 / 待验收：**

1. 场景剧本按钮（营销轰炸 / 运行时新增渠道等一键串）  
2. 可调发送数量与间隔  
3. 渠道目录导出（便于对照 Quieta 规则写回归用例）  

---

## 2. 技术栈与 SDK

| 项 | 约定 |
|----|------|
| 语言 | Kotlin |
| UI | Jetpack Compose + Material3（**不依赖 miuix**，与息匣 UI 解耦） |
| 构建 | Gradle Kotlin DSL + Version Catalog；AGP **9.4.0** · Gradle **9.6.0** · Kotlin **2.4.10** |
| minSdk | 26 |
| targetSdk / compileSdk | 37 |
| 架构 | 单模块 `:app`；清单/发送逻辑集中在 `LabNotifier` |
| 通知 API | `NotificationManager` + `NotificationChannel`；不引入推送 SDK |
| 依赖 | 仅 AndroidX Core / Lifecycle / Activity Compose / Compose BOM / Material3 |

工具链与息匣对齐，便于同机构建；**不 includeBuild miuix**，避免测试 App 被息匣 UI 改动拖垮。

---

## 3. 仓库与模块结构

```text
NotifLab/
├── app/
│   └── src/main/java/app/quieta/notiflab/
│       ├── MainActivity.kt      # UI：权限、动作条、渠道卡
│       ├── LabNotifier.kt       # LabCatalog + 渠道创建/发送/重置
│       ├── LabViewModel.kt      # UI 状态与操作入口
│       └── ui/theme/Theme.kt
├── gradle/                      # libs.versions.toml 等
├── README.md
├── AGENTS.md                    # 本文件
└── settings.gradle.kts
```

### 包名

```text
app.quieta.notiflab              # 唯一业务包
app.quieta.notiflab.ui.theme
```

### 依赖方向

```text
MainActivity → LabViewModel → LabNotifier → NotificationManager
```

无多模块、无仓库层、无提权抽象。目录数据只放在 `LabCatalog`，禁止散落在 Composable 里。

---

## 4. 渠道目录（固定契约）

群组：`quieta_lab`（名称「息匣通知实验室」）。

| id | 名称 | 目录 importance | 用途 |
|----|------|-----------------|------|
| `lab.marketing.promo` | 营销推送 | HIGH(4) | 静音主目标；名称含「推送」，可被默认/示例规则命中 |
| `lab.marketing.event` | 活动通知 | DEFAULT(3) | 未命中「推送」时的对照噪音 |
| `lab.order.status` | 订单状态 | HIGH(4) | 重要业务对照，应保持可提醒 |
| `lab.order.shipping` | 物流更新 | DEFAULT(3) | 中优先级噪音 |
| `lab.live.stream` | 直播开播 | DEFAULT(3) | 易与营销混淆 |
| `lab.system.info` | 系统信息 | LOW(2) | 降级对照源 |
| `lab.privacy.alert` | 隐私提醒 | MIN(1) | 最低可见性对照 |
| `lab.support.chat` | 客服消息 | DEFAULT(3) | 默认优先级噪音 |

规则：

- id 必须以 `lab.` 开头，便于 Quieta / 重置逻辑识别  
- 通知正文固定带 `Quieta Lab` 标识，避免被当成真实业务通知  
- 新增渠道先改本表，再改 `LabCatalog`，禁止只改代码不改文档  

---

## 5. 与 Quieta 的联调契约（强制）

### 不得覆盖外部写入的 importance

**发送 / 启动时只在渠道缺失时创建。** 对已存在的渠道调用 `createNotificationChannel()` 会把 Quieta 刚写入的 `importance=0/2` 冲回目录值，导致静音「假失败」。这是真机踩过的坑（K40s）。

| 操作 | 行为 |
|------|------|
| `ensureChannels()` / 发送前补建 | 仅 `getNotificationChannel(id) == null` 时创建 |
| 「重置渠道」 | **允许**删除全部 `lab.*` 后按目录强制重建（明确的清场动作） |

### 推荐验收序列

1. 安装 Notiflab debug → 授通知权限 → 「连发全部」  
2. Quieta 盘点：应看到 `app.quieta.notiflab.debug` 下 8 渠道及 importance 分布  
3. Quieta 规则（如 `nameContains=推送`）批量静音 → `lab.marketing.promo` 系统 importance=0  
4. Notiflab 再点该渠道「发送」：importance 应保持 0，且 **不产生** NotificationRecord  
5. 对照未命中渠道（如 `lab.marketing.event`）仍可投递  
6. 需要测「新建渠道自动静音」时再用「重置渠道」  

### 包名

联调默认使用 **debug 包**：`app.quieta.notiflab.debug`（与正式包可并存）。

### 测试时会自行调用息匣（代理义务）

本仓库任何真机联调，**不要**只操作 Notiflab 就宣称静音/拦截通过；必须拉起息匣完成治理侧步骤。

| 步骤 | 自调用对象 | 典型命令 / 动作 |
|------|------------|-----------------|
| 安装噪音源 | Notiflab | `adb install -r Notiflab/.../app-debug.apk` |
| 授权并拉起噪音源 | Notiflab | `pm grant ... POST_NOTIFICATIONS`；`am start -n app.quieta.notiflab.debug/app.quieta.notiflab.MainActivity` |
| 制造噪音 | Notiflab | UI「发送 / 连发全部 / 重置渠道」 |
| 安装治理端 | Quieta | `adb install -r Quieta/.../app-debug.apk` |
| 拉起并治理 | Quieta | `am start -n app.quieta.debug/app.quieta.MainActivity`；刷新 / 按规则静音 |
| 核验证据 | 系统 | `dumpsys notification` 查 `lab.*` 的 `mImportance` 与是否出现 NotificationRecord |

息匣本地路径约定：与本仓库同级 `../Quieta`（或文档中的 `C:\Users\i1290\Documents\ChatGPT\Quieta`）。远程：`https://github.com/1290000/Quieta`。

### 本应用为息匣测试提供的职能（摘要）

| 提供 | 不提供 |
|------|--------|
| 可复现的 `lab.*` 渠道目录 | 规则引擎、批量静音、提权 |
| 点击发送 / 连发 / 重置 / 清空 | 通知使用权监听、时间线采集 |
| 标准 API 噪音（无 Root） | 业务推送、网络上报 |

对等说明：息匣负责观察与改 importance；本仓库只负责把渠道建出来并把通知发出去。双方在测试时**互相调用对方 debug 包**，以系统状态为验收真相源。

---

## 6. UI / 交互

- 单页：状态卡（权限 + 最近操作）+ 动作条（连发 / 重置 / 清空）+ 渠道卡列表  
- 渠道卡：名称、id、目录 importance 说明、「发送」  
- 状态文案必须能看出「已发送某渠道 #n」或权限未授予  
- Material3 默认主题即可；深浅跟随系统  
- 不引入息匣底栏 / 液态玻璃 / miuix，保持测试工具可编译、可剥离  

---

## 7. 性能与权限

| 策略 | 要求 |
|------|------|
| 权限 | 仅 `POST_NOTIFICATIONS`；不要通知使用权、不要 QUERY_ALL_PACKAGES |
| 常驻 | 无前台服务、无 Listener、无 WorkManager |
| 发送 | 用户点击触发；禁止后台自动刷通知 |
| 重置 | 明确二次确认或文案标明会删除渠道（当前为直接执行，文案已说明） |

---

## 8. 工程约定

- 分支：`main`  
- **远程**：`origin` = `https://github.com/1290000/NotifLab.git`  
- **自动提交**：完成一步实质工作后 `git add` 相关文件并 commit（`feat:` / `fix:` / `docs:` / `chore:`）；远程可达时 `git push origin main`。禁止 force-push `main`；禁止提交密钥。  
- 版本：显示版本 `yy.MM.x`（与息匣节奏对齐，如 `26.09.1`）+ 递增 `versionCode`  
- 分发：本地 `assembleDebug` / 直接装机；**不以应用商店为路径**  
- 构建：`gradlew.bat :app:assembleDebug`  
- 产物：`app/build/outputs/apk/debug/app-debug.apk`  
- `local.properties` 不提交  
- 单测：目录与发送逻辑若抽纯函数再补；MVP 以真机联调为准  

### 卫生

- 不把 `build/`、`*.apk`、log、截图提交进 git  
- 临时 uiautomator dump、真机截图放仓库外或被忽略的 `artifacts/`  
- 与 Quieta **不得共享源码目录**；允许包名前缀同为 `app.quieta` 以便对照  

---

## 9. 代码规范

### 分层

```text
Compose UI → LabViewModel → LabNotifier → NotificationManager
```

- Composable 不直接调 `NotificationManager`  
- 渠道目录集中在 `LabCatalog`；发送文案模板用 `{{n}}` 占位序号  
- 一文件一主类型；命名：`Lab*` 前缀避免与业务 App 混淆  

### 禁止

- 在 `send()` 路径无条件 `createNotificationChannel`（见 §5）  
- 引入网络、统计、崩溃 SDK  
- 复制息匣的规则引擎 / 提权代码进本仓库  

### 样板

新能力优先加在 `LabNotifier` + `LabViewModel`，UI 只绑状态；避免为每个按钮新建架构层。

---

## 10. 文案与命名

- 应用名：息匣通知实验室；英文：Notiflab  
- **作者**：`1290000`  
- **本应用仓库**：`https://github.com/1290000/NotifLab`  
- 通知标题/正文带 `Quieta Lab` 前缀  
- 避免「清理 / 加速 / 管家」等词；定位是「通知实验室 / 测试噪音源」  
- 界面语言：`zh-CN` 优先；`values/` 作英文兜底  

---

## 11. 本文件（AGENTS.md）的维护规则

与息匣相同：本文件是**长期有效的工程与产品约定**，不是会话记录。

| 动作 | 做法 |
|------|------|
| 新增约定 | 插入所属章节，不优先新建章节 |
| 变更约定 | **原地改写**该条 |
| 废弃约定 | 删除或一行说明已由 X 替代 |
| 产品/架构变更 | **先改本文件，再改代码** |
| 仅实现细节 | 不写入本文件 |

### 何时必须更新本文件

| 触发 | 动作 |
|------|------|
| 渠道目录变更（增删 id / 改默认 importance） | 原地改 §4 |
| 与 Quieta 联调契约或自调用步骤变化 | 原地改 §5 |
| applicationId / SDK / 依赖方向变化 | 原地改 §2–§3 |
| 发现文档与代码不一致 | 以已合入代码与已拍板决策为准，禁止只改一边 |

### 与实现的关系

- 约定变更：先改本文件，再改代码（紧急修复可先改码，同一回合回写）  
- 本文件与代码冲突时：以产品意图 + 已验证可运行的代码为准，立刻收敛文档  

---

## 12. 与息匣 AGENTS.md 的类比对照

| 息匣章节 | 本文件 | 说明 |
|----------|--------|------|
| 产品边界 / MVP | §1 | 息匣做治理；本仓库只做噪音源 |
| 技术栈 | §2 | 对齐 AGP/Gradle/Kotlin；去掉 miuix/Shizuku |
| 模块结构 | §3 | 单模块，无 core/ui 拆分 |
| 提权 | —（不做） | 本仓库无特权后端 |
| ROM 适配 | §5 联调契约 | 只关心「写入不被发送路径覆盖」 |
| UI | §6 | Material3 最小可用 |
| 性能权限 | §7 | 仅 POST_NOTIFICATIONS |
| 工程约定 | §8 | 独立仓库与远程 |
| 代码规范 | §9 | Lab* 前缀与发送路径禁令 |
| 文案命名 | §10 | Quieta Lab 标识 |
| AGENTS 维护 | §11 | 同一维护纪律 |
