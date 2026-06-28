# RanNuan TV — Android 原生端开发文档

> Kotlin + Jetpack Compose 原生应用，共享 `server/server.js` 后端
> v3.2 — 2026-06-28

---

## 一、项目隔离原则（重要！）

```
RanNuan TV/
├── server/                    ← 共享后端，桌面端/Android 共用
├── packages/shared/           ← 共享 TypeScript 类型（仅桌面端使用）
│
├── apps/
│   ├── desktop/               ← 桌面端（Tauri + React + Vite）
│   └── mobile-android/        ← Android 原生端（Kotlin + Compose）
│       ├── app/src/main/java/com/rannuan/tv/   ← 所有 Kotlin 源码
│       │   ├── data/          ← 数据层
│       │   └── ui/            ← UI 层（Compose 页面/主题/导航）
│       └── DEVELOPMENT_DOC.md ← 本文档
│
└── apps/mobile/               ← ⚠️ 旧 React Native 项目，已废弃
```

| 规则 | 说明 |
|------|------|
| 🔴 **绝不修改 desktop/src/** | 改移动端时不要动桌面端 React 代码 |
| 🟡 **谨慎修改 server/** | `server/server.js` 是桌面端和 Android 共用后端；移动端问题若必须改后端，需同时考虑桌面端兼容 |
| 🔴 **绝不修改 packages/shared/** | 那是桌面端 TS 类型，移动端有自己的 Kotlin 模型 |
| 🔴 **绝不使用 Kotlin 反射** | 不引入 `kotlin-reflect`，不用 `::class.sealedSubclasses` 等（详见 §八） |
| 🟡 **忽略 `apps/mobile/`（RN 废弃）** | Cursor 若仍报 `com.facebook.react.settings`：① 用根目录 **`RanNuan-Android.code-workspace`** 打开（不含 mobile）；② `Ctrl+Shift+P` → **Java: Clean Java Language Server Workspace** → Restart；③ 或 **File → Open Folder** 只开 `apps/mobile-android` |
| 🟢 **可以改 mobile-android 内任何文件** | 这是移动端专属目录 |

---

## 二、技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 语言 | Kotlin | 1.9.22 |
| UI | Jetpack Compose + Material 3 | BOM 2024.02 |
| 图标 | Material Icons Extended | — |
| 网络 | Retrofit + OkHttp + Gson | 2.9 / 4.12 |
| 图片 | Coil Compose | 2.5 |
| 播放器 | ExoPlayer (Media3) | 1.2.1 |
| 导航 | Navigation Compose | 2.7.7 |
| 构建 | AGP | 8.2.2 |
| **Compose Compiler** | — | **1.5.10**（必须匹配 Kotlin 1.9.22） |
| Min / Target SDK | API 24 / 35 | Android 7.0 / 14 |

---

## DLNA 投屏（v3.0）

### 协议原理

```
SSDP 多播 (239.255.255.250:1900)
  → 发现局域网 DLNA 设备
  → 获取设备描述 XML (friendlyName / manufacturer / AVTransport controlURL)
  → SOAP 调用 SetAVTransportURI → Play → 投屏开始

后续控制：Pause / Stop / Seek / GetPositionInfo / SetVolume
```

### DLNAController 核心 API

| 方法 | 协议 | 用途 |
|------|------|------|
| `discoverDevices()` | SSDP M-SEARCH | 扫描局域网 DLNA 设备，4 秒超时 |
| `setAVTransportURI(device, url)` | UPnP AVTransport | 推送视频地址到电视 |
| `play(device)` | UPnP AVTransport | 开始/恢复播放 |
| `pause(device)` | UPnP AVTransport | 暂停 |
| `stop(device)` | UPnP AVTransport | 停止 |
| `seek(device, seconds)` | UPnP AVTransport | 跳转到指定位置 |
| `getPositionInfo(device)` | UPnP AVTransport | 获取当前进度和总时长 |
| `setVolume(device, volume)` | UPnP RenderingControl | 调节音量 (0-100) |

### 支持的电视品牌

小米 / 海信 / TCL / 创维 / 长虹 / 康佳 / 索尼 / 三星 / LG / 松下 / 飞利浦 — 只要支持 DLNA/UPnP 协议即可。

### 使用条件

- 手机与电视必须在**同一局域网**（同一 Wi-Fi）
- 真机需将 `SERVER_URL` 配置为电脑的局域网 IP（模拟器 `10.0.2.2` 电视无法访问）

---

## 播放器设置项（v3.0+）

| 设置项 | 可选项 | 持久化 |
|--------|--------|:--:|
| 默认倍速 | 0.75× / 1× / 1.25× / 1.5× / 2× | ✅ |
| 快进/快退 | 5s / 10s / 15s / 30s | ✅ |
| 画面尺寸 | 适应（FIT）/ 拉伸（FILL）/ 裁剪（ZOOM） | ✅ |
| 播放方式 | 顺序播放 / 单集循环 / 列表循环 | ✅ |
| 镜像反转 | 正常 / 水平镜像 / 垂直镜像 | ✅ |

> 镜像反转使用 `Modifier.scale(scaleX, scaleY)` 在 Compose 层面实现，负值翻转视频画面。与画面尺寸独立工作，可组合使用。

## 播放器手势交互（v3.1 重写）

| 手势 | 区域 | 行为 | 实现要点 |
|------|------|------|---------|
| 单击 | 全屏 | 显隐控制栏 | `detectTapGestures.onTap` |
| 双击 | 全屏 | 播放/暂停 | `detectTapGestures.onDoubleTap` |
| 长按 | 全屏 | 临时 2× 倍速，松手恢复 | `detectTapGestures.onLongPress` + `onPress` |
| 水平拖拽 | 全屏 | Seek 进度（2× 加速系数） | `detectDragGestures` + 增量 `dragAmount.x` |
| 竖向拖拽 | 左半屏 | 调节亮度 | 基线亮度在 `onDragStart` 捕获，累计位移映射 |
| 竖向拖拽 | 右半屏 | 调节音量 | 基线音量 + `roundToInt` 浮点累加避免截断 |

> 模式判定逻辑（v3.1）：累计位移 > 30px 后，`dx > dy × 1.5` → Seek，否则按左右半屏判定亮度/音量。避免了旧版 `dy < 24px` 绝对阈值导致的斜向手势误判。

---

## 三、项目结构

```
mobile-android/app/src/main/java/com/rannuan/tv/
├── MainActivity.kt
├── data/
│   ├── model/Models.kt
│   └── api/RanNuanApi.kt
└── ui/
    ├── theme/Color.kt + Type.kt + Theme.kt   ← 颜色/排版/主题
    ├── util/
    │   ├── ImageProxy.kt
    │   ├── TextUtil.kt          ← stripHtml + formatSourceName（v2.8 新增）
    │   ├── FavoritesStore.kt
    │   ├── SearchHistoryStore.kt
    │   └── WatchingHistoryStore.kt
    ├── navigation/NavGraph.kt
    └── screens/
        ├── home/HomeScreen.kt        （含 Banner 轮播）
        ├── search/SearchScreen.kt
        ├── category/CategoryScreen.kt
        ├── detail/DetailScreen.kt
        ├── profile/ProfileScreen.kt  （收藏 + 历史播放入口）
        └── player/
            ├── PlayerScreen.kt       （播放器主页面 + 手势 + 设置面板）
            └── DLNAController.kt     （DLNA SSDP 发现 + AVTransport SOAP）
```

> ⚠️ `theme/Type.kt` 不可缺失：定义 `Typography`，被 `Theme.kt` 引用。删除会让编译器把 `Typography` 误解析成 `kotlin.text.Typography`，报类型不匹配。

---

## 四、后端 API 接口（共享，无需改动）

| 接口 | 方法 | 参数 | 返回 | 用途 |
|------|:--:|------|------|------|
| `/api/douban/home` | GET | - | `DoubanHomeData` | 首页数据 |
| `/api/search` | GET | `wd` | `{list: MediaItem[]}` | 聚合搜索（缓存 + 演员索引 + detail 补全） |
| `/api/category` | POST | **`category`**, `page`, `pageSize`, **`subType`** | `CategoryResponse` | 分类+分页+子分类；后端 `totalPages` 不完全可信 |
| `/api/multi-detail` | GET | `wd`, `keys` | `{list: MediaDetail[]}` | 多源详情 |
| `/api/img` | GET | `url` | 图片二进制 | 图片代理 |
| `/api/proxy` | GET | `url` | 视频流/m3u8 | 视频代理（解析分享页+防盗链） |
| `/api/douban/recommend` | GET | `type, tag, limit` | `MediaItem[]` | 豆瓣推荐 |

> ⚠️ `/api/category` 的分类字段名是 **`category`**（不是 `type`），合法值见 `server.js:145` 的 `CATEGORY_TYPE_NAMES`：`movie / tv / variety / anime / shortDrama / sports`。
> 
> ⚠️ `/api/category` 支持 **`subType`** 参数（v2.8），传子分类标签名（如 `"动作"`、`"国产"`），服务端走 `SUB_TYPE_MAP` 用精确 type_id 过滤，返回 `subCounts` 和 `serverFiltered` 标记。
> 
> ⚠️ `/api/multi-detail` 在 `wd` 非空且 `keys` 为空时，按片名跨站搜索所有站点同名影片；`keys` 非空时只按 keys 拉取。
>
> ⚠️ `server/server.js` 也是桌面端后端。改 `/api/search`、`/api/category`、`/api/multi-detail` 后需检查桌面端搜索页、分类页、详情页是否仍兼容。

---

## 五、开发流程

### 5.1 启动

```bash
cd "RanNuan TV/server" && node server.js    # 后端 http://localhost:3000
# AS 打开 apps/mobile-android → 🐘 Sync → ▶️ Run
```

### 5.2 命令行构建（不依赖 AS，已内置本地 Gradle 8.5）

```bash
export JAVA_HOME="/d/Android Studio/jbr"
export PATH="$JAVA_HOME/bin:$PATH"
cd "RanNuan TV/apps/mobile-android"
/c/gradle/gradle-8.5/bin/gradle :app:compileDebugKotlin --no-daemon   # 仅编译
/c/gradle/gradle-8.5/bin/gradle :app:installDebug      --no-daemon   # 编译+装机
```

### 5.3 抓崩溃日志（定位闪退必备）

```bash
ADB="/c/Users/<你>/AppData/Local/Android/Sdk/platform-tools/adb.exe"
$ADB logcat -c
$ADB shell am start -n com.rannuan.tv/.MainActivity
$ADB logcat -d -b crash | grep -A40 "FATAL EXCEPTION"
```

### 5.4 打包 APK

模拟器：Build → Build APK(s)；真机：Build → Generate Signed APK，改 `SERVER_URL` 为公网 IP。

---

## 六、关键配置

- **Compose Compiler**：`composeOptions { kotlinCompilerExtensionVersion = "1.5.10" }`，匹配 Kotlin 1.9.22。
- **图标**：v2.8 起播放器统一使用 **`Icons.Outlined.*`**（线性描边风格），方向敏感图标用 `Icons.AutoMirrored.Outlined.*`。播放/暂停用 `Icons.Rounded.PlayArrow/Pause`。
- **镜像**：`settings.gradle.kts` 已配阿里云（google/public/jcenter/gradle-plugin）。
- **API 基址**：`build.gradle.kts` → `SERVER_URL`，默认 `http://10.0.2.2:3000`。
- **⚠️ 禁止 Kotlin 反射**：不引入 `kotlin-reflect`（+2.5MB）。枚举 sealed 子类用 `companion object { val entries = listOf(...) }`，不要用 `::class.sealedSubclasses`。

---

## 七、常见构建错误（编译期）

| 错误 | 原因 | 解决 |
|------|------|------|
| `UnknownPluginException: kotlin.plugin.compose` | Kotlin 1.9.x 不支持独立 compose 插件 | 用 `composeOptions { kotlinCompilerExtensionVersion }` |
| `Unresolved reference: GridView` | 图标在基础包中不存在 | 已加 `material-icons-extended` |
| `Type mismatch ... Typography` | 缺 `theme/Type.kt` | 补 `Type.kt` 定义 `Typography` |
| `Classifier 'BuildConfig' does not have a companion object` | 字符串模板 `"$BuildConfig.X"` | 改 `"${BuildConfig.X}"` |
| `Assigning single elements to varargs in named form` | `@OptIn(markerClass = X::class)` 少方括号 | `@OptIn(markerClass = [X::class])` 或 `@OptIn(X::class)` |
| media3 `UnstableApi` 警告 | PlayerView/ExoPlayer 是实验 API | `@androidx.annotation.OptIn(markerClass = [UnstableApi::class])` |
| `ExperimentalFoundationApi` 警告 | Pager API 是实验性 | `@OptIn(ExperimentalFoundationApi::class)` |

---

## 八、常见运行时崩溃（闪退）

### 8.1 `KotlinReflectionNotSupportedError`（进首页即闪退）⭐【已修复】

**日志**：`KotlinReflectionNotSupportedError ... at NavGraphKt.MainNavHost(NavGraph.kt)`。
**根因**：`Screen::class.sealedSubclasses` 依赖 `kotlin-reflect`，本项目未引入。
**修复**：`companion object { val entries = listOf(Home, Category, Search, Profile) }`，两处调用改 `Screen.entries`。

### 8.2 被系统 `lowmemorykiller` 杀死（偶发）

**日志**：无 Java 异常，`lowmemorykiller: Kill 'com.rannuan.tv' ... min watermark is breached`。
**根因**：模拟器内存吃紧，非代码 bug。
**解决**：AVD 调高 RAM（≥2048MB）或精简页面同时持有的状态/图片。

---

## 九、注意事项

1. 不改 `apps/desktop/` / `server/` / `packages/shared/`
2. 不用 Kotlin 反射（见 §6 / §8.1）
3. 新增数据字段 → `data/model/Models.kt`；新增 API → `data/api/RanNuanApi.kt`
4. 底部 Tab → 改 `NavGraph.kt` 的 `Screen.entries`（勿改回反射）
5. 改完代码务必 `:app:compileDebugKotlin` 或真机 Run 验证

---

## 十、播放器直连与代理路由（v2.8）

### 10.1 线路类型判断

CMS 返回的 `vod_play_url` 中 URL 分两种：

| 类型 | 示例 | 特征 |
|------|------|------|
| **m3u8** | `.../index.m3u8` | 含 `.m3u8`，ExoPlayer 原生 HLS 支持 |
| **分享页/直链** | `.../share/b11b7e...` | 无 `.m3u8`，实际是 HTML 页面或非 m3u8 格式 |

> 分享页（如 `v.lfthirtytwo.com/share/xxx`）是 HTML 网页，内嵌播放器。直接喂给 ExoPlayer 会失败（拿到 HTML 不是视频流）。

### 10.2 播放路由策略（`tryPlayVideo`）

```
if URL 含 .m3u8:
    → 直连 ExoPlayer（快，原生 HLS）
    → 失败 → 自动切 /api/proxy 代理重试
else:
    → 直接走 /api/proxy 代理（不尝试直连）
```

服务端 `/api/proxy` 行为：
1. 检测 URL 类型（m3u8 / HTML 分享页 / 二进制）
2. 分享页 → `extractVideoUrlFromSharePage()` 提取真实视频地址
3. m3u8 → 重写内部分片 URL 为代理地址
4. TS/MP4 → 流式代理二进制数据

### 10.3 防盗链处理

| CDN | Referer 要求 |
|-----|-------------|
| bfzy / picbf | `https://bfzyapi.com` |
| 其他 | URL 自身的 origin（`scheme://host`） |

直连 m3u8 时通过 `dsFactory.setDefaultRequestProperties()` 动态注入 Referer。

---

## 十一、共享工具类

### 11.1 `TextUtil.kt`

| 函数 | 用途 |
|------|------|
| `stripHtml(html)` | HTML → 纯文本，解码 `&amp;` `&nbsp;` `&#xxx;` 等所有实体，`<br>`→换行 |
| `formatSourceName(rawName, idx, siteKey)` | 原始源名 → 中文名（`liangzi`→`量子.线路一`），对标桌面端 `client.ts` |

`formatSourceName` 映射表（与 `server.js:SUB_TYPE_MAP` 对应）：

| 站点 | 线路一 | 线路二 |
|------|--------|--------|
| 量子 lzzy | `liangzi` → 量子.线路一 | `lzm3u8` → 量子.线路二 |
| 非凡 ffzy | `feifan` → 非凡.线路一 | `ffm3u8` → 非凡.线路二 |
| 暴风 bfzy | `bfzym3u8` → 暴风.线路一 | — |
| 索尼 suoni | `sonim3u8` → 索尼.线路一 | — |
| 百度 bdzy | `bdm3u8` → 百度.线路一 | — |

---

## 十二、子分类筛选功能（v2.8）

分类页支持子分类标签，数据来源于 `SUB_CATEGORIES`（`CategoryScreen.kt`），与服务端 `SUB_TYPE_MAP` 标签名一致：

| 主分类 | 子分类标签 |
|--------|----------|
| movie 电影 | 动作、喜剧、爱情、科幻、恐怖、剧情、战争、动画 |
| tv 电视剧 | 国产、港剧、台剧、日剧、韩剧、美剧、泰剧 |
| anime 动漫 | 日漫、国漫、欧美、剧场版 |
| variety 综艺 | 大陆综艺、港台综艺、日韩综艺、欧美综艺 |
| sports 体育 | 足球、篮球、网球、斯诺克 |
| shortDrama 短剧 | 现代言情、古装仙侠、穿越年代、反转爽文、女频总裁、都市脑洞 |

选中子分类后传送 `subType` 参数，服务端用精确 `type_id` 过滤（不走前端 `typeMatch`），标签旁显示 `subCounts` 计数。

---

## 十三、v3.2 移动端阶段收尾记录（2026-06-28）

### 13.1 播放页优化

| 项目 | 状态 | 说明 |
|------|:--:|------|
| 播放完成自动切集 | ✅ | 顺序播放、单集循环、列表循环分离处理，不依赖 ExoPlayer 单 MediaItem repeat |
| 防息屏 | ✅ | 播放中设置 `FLAG_KEEP_SCREEN_ON`，避免看视频时屏幕变暗 |
| 非全屏进度条 | ✅ | 非全屏保持单行，全屏才显示更多控制；“下一集”仅全屏展示，减少拥挤 |
| 进度条 thumb 图标 | ✅ | thumb 独立布局计算，不再因裁剪/宽度计算导致开头不显示 |
| 当前集动画 | ✅ | 选集列表和播放页选集均用律动条动画标识当前播放集，无文字提示 |
| 播放源/线路切换 | ✅ | 播放页支持直接切换所有站点来源和播放线路，对齐桌面端多源体验 |
| 选集弹窗 | ✅ | 调整为更紧凑的移动端底部弹窗，减少占屏面积 |

### 13.2 导航与详情

| 项目 | 状态 | 说明 |
|------|:--:|------|
| 底部 Tab 卡死修复 | ✅ | 从推荐/搜索深层跳转后，“我的”菜单不可点的问题已修复 |
| 首页/分类直跳详情 | ✅ | 首页推荐、Banner、分类页卡片均直接跳详情页，不再先跳搜索 |
| 详情页缓存 | ✅ | 最近 32 个详情结果内存缓存，播放页返回详情时减少等待 |

### 13.3 分类与搜索

| 项目 | 状态 | 说明 |
|------|:--:|------|
| 分类分页缓存 | ✅ | Android 端只缓存最近 24 个分页，每页 20 条，TTL 5 分钟；避免整分类 3~4w 条进内存 |
| 分类继续加载 | ✅ | 移动端不完全依赖后端 `totalPages`，当前页满页时继续尝试下一页，直到空页/`outOfRange` |
| 搜索缓存 | ✅ | 最近 24 个关键词，每个关键词最多 120 条结果，TTL 10 分钟 |
| 搜索不清屏 | ✅ | 输入新词加载时保留旧结果，减少闪屏和等待感 |
| 演员名排序 | ✅ | 前端按演员精确/包含命中优先排序；真正召回依赖后端演员索引和 detail 补全 |
| 相似推荐 | ✅ | 播放页相似推荐改为演员/导演/分类/年份/地区多维召回和评分 |

### 13.4 共享后端同步改动

| 项目 | 状态 | 说明 |
|------|:--:|------|
| `/api/search` | ✅ | 增加搜索缓存、演员索引优先、CMS 标题搜索、批量 `detail` 补全、演员结果不足时有界扫描 |
| `actorIndex.js` | ✅ | 英文演员名不再按空格拆分；大小写不敏感；索引条目瘦身，不保存 `vod_content` |
| `/api/category` | ✅ | 恢复父+子 type_id 聚合，保证翻页；缓存 key 加版本号，绕开旧错误缓存 |

### 13.5 收尾结论

移动端当前阶段暂不继续扩展功能，后续优化重心转到桌面端。由于 `server/server.js` 是共享后端，桌面端优化时若继续调整搜索/分类/详情接口，需要同步验证 Android 端基础链路。

---
---

# ★ 待优化方案（v2.7 全部完成）

> 本节所有待办已于 **2026-06-24** 实施完毕，`:app:compileDebugKotlin` 构建通过。
> 各条标注 ✅ 表示已完成。

## 待办 0 · 修复已知 bug ✅

### 0.1 分类页 `400 Bad Request` ✅

**已修复**：`CategoryRequest` 字段已改为 `category`，`CATEGORY_NAMES` 已改为 `shortDrama`，`loadingMore` 补位已完成。

### 0.2 详情页选集渲染混乱 ✅

**已修复**：`DetailScreen.kt` 选集改用 `episodes[epIdx]` 真实下标，不再用 `indexOf`。

---

## 待办 1 · 重建设计系统 ✅

| 子项 | 状态 | 文件 |
|------|:--:|------|
| 1.1 扩展色板 | ✅ | `theme/Color.kt` — Brand300-600 / Zinc 全梯度 / Glass 系列 / 状态色 |
| 1.2 排版分级 | ✅ | `theme/Type.kt` — Display/Headline/Title/Body/Label 全部补齐 |
| 1.3 圆角 token | ✅ | `theme/Shape.kt` — ShapeCard/ShapeCardLarge/ShapeChip/ShapePill |
| 1.4 玻璃拟态 | ✅ | `theme/component/GlassCard.kt` — API≥31 blur / <31 半透明降级 |
| 1.5 主题扩展 | ✅ | `theme/Theme.kt` — surfaceVariant/outlineVariant/errorContainer 等已补全 |

---

## 待办 2 · Banner 轮播丝滑化 ✅

| 优化项 | 状态 | 实现 |
|--------|:--:|------|
| Banner 圆角 + 内边距 | ✅ | `clip(ShapeCardLarge)` + `padding(horizontal=12.dp)` |
| 视差缩放 | ✅ | `graphicsLayer { scaleX/scaleY }` 随 offset 变化 |
| Indicator 动画 | ✅ | `animateDpAsState` 6dp→20dp 平滑过渡 |
| 自动播放鲁棒性 | ✅ | `isScrollInProgress` 时暂停自动翻页 |
| 渐变蒙层升级 | ✅ | 底部透明→Zinc950 深渐变 + GlassCard 包裹文字 |

---

## 待办 3 · 首页布局重排 + 卡片精致化 ✅

| 优化项 | 状态 | 实现 |
|--------|:--:|------|
| 布局重排 | ✅ | Banner → 分类入口(4宫格) → LazyRow 横滑分区 → 底部留白 |
| 卡片精致化 | ✅ | 圆角 `ShapeCard` + `shadow(4dp)` + 评分胶囊 + 按压缩放 0.96f |
| 横滑分区 | ✅ | 推荐/电视剧/电影/综艺/动漫均用 `LazyRow` |

---

## 待办 4 · 分类页 UI 优化 ✅

- ✅ `ScrollableTabRow` → `FilterChip` 横滑条
- ✅ 错误态补图标 + 重试按钮
- ✅ 空态补占位图标 + 文案
- ✅ 卡片复用首页 `MediaCard`

---

## 待办 5 · 详情页优化 ✅

- ✅ 封面区毛玻璃背景（底层模糊放大封面 + 上层清晰海报 + 渐变遮罩）
- ✅ 收藏持久化（`FavoritesStore`，SharedPreferences，跨重启保留）
- ✅ 选集用 `LazyVerticalGrid(4列)` + 真实下标（不用 indexOf）

---

## 待办 6 · 搜索页优化 ✅

- ✅ 搜索框用 `GlassCard` 包裹
- ✅ 搜索历史（`SearchHistoryStore`，最多 10 条，可清除）
- ✅ 热门搜索词（10 个预置词，点击直接搜索）
- ✅ `item.sites!!` 双感叹号改为安全调用

---

## 待办 7 · 播放器 `PlayerScreen` ✅

详见 7.1/7.2/7.3 节，已于 v2.5~v2.6 完成。  
v2.7 追加：播放器 UI 重构（爱优腾风格参考）、投屏入口、播放设置面板、直链播放支持。

---

## 待办 8 · 「我的」Tab 完整页面 ✅

- ✅ 头部：App Logo + 收藏/历史统计数（v3.1 简化，移除设置/关于/缓存清理）
- ✅ 双 Tab 切换：收藏（3 列网格） + 历史（列表）
- ✅ 收藏通过 API 批量解析封面标题，`FavoriteCache` 内存缓存秒开
- ✅ 观看历史（`WatchingHistoryStore`，最多 50 条，含封面 + 剧集 + 续播位置）
- ✅ 历史卡片显示「续播 mm:ss」橙色标签，点击跳到上次观看位置

---

## 待办 9 · 全量验证 ✅

| 验证项 | 状态 |
|--------|:--:|
| `:app:compileDebugKotlin` 0 error / 0 warning | ✅ |
| 逐页点击无闪退 | 待装机实测 |
| 分类页不再 400 | ✅ |
| 详情选集点击正确 | ✅ |

---

## 实施顺序（全部完成）

```
待办 0（修 bug）   ✅ 2026-06-24
  ↓
待办 1（设计系统）  ✅ 2026-06-24
  ↓
待办 2/3（首页+Banner）  ✅ 2026-06-24
  ↓
待办 4/5（分类+详情）    ✅ 2026-06-24
  ↓
待办 6/7（搜索+播放器）  ✅ 2026-06-24
  ↓
待办 8（我的页）→ ✅ 2026-06-24
  ↓
待办 9（全量验证）→ ✅ 编译通过
```

---

## 新增文件清单

### v3.1

| 文件 | 用途 |
|------|------|
| `ui/screens/profile/ProfileScreen.kt` | 「我的」页面（收藏 + 历史双 Tab，App Logo 头部） |

### v3.0

| 文件 | 用途 |
|------|------|
| `ui/screens/player/DLNAController.kt` | DLNA 投屏控制器（SSDP 发现 + AVTransport SOAP 控制） |

### v2.7

| 文件 | 用途 |
|------|------|
| `ui/util/FavoritesStore.kt` | 收藏持久化（SharedPreferences，`siteKey:id` 为 key） |
| `ui/util/SearchHistoryStore.kt` | 搜索历史（JSON，最多 10 条，可清除） |
| `ui/util/WatchingHistoryStore.kt` | 观看历史（JSON，最多 50 条，含封面/剧集/续播位置） |

> 📅 文档版本：v3.2
> 📝 最后更新：2026-06-28
> 📋 变更：
> - **v3.2** — 移动端阶段收尾 + 共享后端优化记录：
>   - **播放页**：自动切集、防息屏、非全屏进度条修复、进度 thumb 图标修复、当前集律动动画、播放源/线路切换、紧凑选集弹窗。
>   - **导航/详情**：修复深层跳转后底部 Tab 不可点；首页/分类/Banner 直跳详情；详情页内存缓存。
>   - **分类/搜索**：分页级有限缓存、搜索有限缓存、搜索不清屏加载、分类翻页不完全依赖 `totalPages`。
>   - **共享后端**：`/api/search` 增加缓存 + 演员索引 + detail 补全；`actorIndex.js` 英文名解析/瘦身；`/api/category` 恢复稳定分页聚合。
> - **v3.1** — 续播 + 页面保活 + 手势优化 + 我的页重构：
>   - **续播（断点播放）**：`WatchingHistoryStore` 新增 `position` 字段和 `updatePosition()` 方法。PlayerScreen 在播放器就绪后 seek 到历史位置，App 切后台/退出页面时自动保存当前进度。历史卡片显示「续播 12:34」橙色标签。
>   - **页面保活**：NavGraph 底部导航改为 `saveState`/`restoreState`，切 Tab 不再重建页面。`FavoriteCache` 内存缓存收藏详情，二次进入秒开无 API 请求。
>   - **手势优化**：亮度/音量触摸基线在 `onDragStart` 精确捕获（不再漂移）；音量用 `roundToInt` 浮点累加避免截断；模式判定用 `dx > dy * 1.5` 比例替代绝对阈值，水平滑动稳定识别为 Seek。
>   - **「我的」页重构**：简化为收藏/历史双 Tab，收藏 3 列网格 + 角标移除，头部替换为 App Logo。移除设置/关于/缓存清理等冗余模块。
>   - **设置面板右滑**：从底部 `ModalBottomSheet` 改为播放器内部右侧滑出面板（`slideInHorizontally`），带半透明遮罩。
> - **v3.0** — DLNA 投屏 + 播放器设置扩展：
>   - **DLNA 完整投屏**：新增 `DLNAController.kt`，基于 SSDP 多播发现 + AVTransport SOAP 控制，支持设备发现/投屏/播放暂停停止/进度查询/音量控制。本地播放器暂停、视频推送到电视。覆盖小米/海信/TCL/创维/长虹/索尼/三星/LG 等国内主流品牌。
>   - **播放器设置扩展**：新增画面尺寸（适应/拉伸/裁剪）、播放方式（顺序播放/单集循环/列表循环）、镜像反转（正常/水平镜像/垂直镜像），使用 `rememberSaveable` 持久化。
> - **v2.9** — 直连播放 + 启动动画 + UI 溢出修复：
>   - **直连播放修复**：代理返回 m3u8 但 ExoPlayer 不知是 HLS → `setMimeType(MimeTypes.APPLICATION_M3U8)`，一行解决两天 bug。
>   - **启动动画**：新增 `SplashScreen.kt`，复刻桌面端影院主题（放映机光束、胶卷孔、漂浮🎞️📽️🍿、Logo 弹入动画），3 秒后淡出进入主界面。`MainActivity.kt` 集成。
>   - **UI 溢出修复**：`DetailScreen` + `PlayerScreen` 中所有演员/导演/元信息 Row 加 `horizontalScroll`，所有 Text 加 `maxLines=1` 或 `overflow=Ellipsis`，`FlowRow` 替换搜索页硬分行。
>   - **播放器功能**：投屏按钮打开系统 Cast 设置，设置弹窗（倍速+快进秒数），选集弹窗改为 5 列网格 + 圆角 Box 替代 FilterChip。
> - **v2.8** — 播放器 + 分类 + 详情页深度修复与优化：
>   - **分类页**：修复分页不加载第二页 bug（`complete` 字段误用 → `totalPages` 判断）；新增子分类筛选标签栏。
>   - **详情页**：单源路由增加跨站搜索补充多源；线路名称改用 `formatSourceName`。
>   - **播放器**：全量 UI 重构——图标 `Filled`→`Outlined`；底部栏单行合并。
>   - **新增 `TextUtil.kt`**：`stripHtml()` + `formatSourceName()` 共享工具函数。
> - **v2.7** — 全量优化落地：待办 0~8 全部完成 ✅，`:app:compileDebugKotlin` 构建通过（0 error）。新增 3 个存储工具类（Favorites/SearchHistory/WatchingHistory），分类/详情/搜索/我的四页 UI 全面升级，播放器 UI 重构（爱优腾风格参考）。投屏入口（TV 按钮）、播放设置面板（倍速+片头片尾）、直链播放支持。
> - **v2.6** — 待办 7 收尾：全屏横屏 + 沉浸式系统栏；修复 Compose 编译错误。
> - **v2.5** — 待办 7（播放器）✅ 完成。
> - **v2.4** — 待办 7 技术选型调研。
> - v2.3 — 新增待优化方案整节（待办 0~9）。
> - v2.2 — 运行时闪退排查、禁止反射、命令行构建流程。
> - v2.1 — Compose 编译器版本、图标依赖。
