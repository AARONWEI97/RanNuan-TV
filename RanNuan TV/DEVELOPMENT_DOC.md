# RanNuan TV — 项目开发文档

> 基于 MACCMS 视频聚合 API 的跨平台视频播放器
> Windows 桌面端 + Android 移动端

---

## 一、项目概述

### 1.1 项目背景

基于 CMS（苹果CMS/MACCMS）视频聚合 API 服务（Node.js + Express，对接 5 个可用影视站点：量子资源 / 非凡影视 / 暴风资源 / 索尼资源 / 百度资源），打造现代化的跨平台视频播放器，覆盖 Windows 桌面端和 Android 移动端。

### 1.2 核心目标

- **Windows 桌面端**：Tauri v2 + React + hls.js/flv.js 播放器内核
- **Android 移动端**：Kotlin + Jetpack Compose + Media3 ExoPlayer 原生播放器
- **代码复用率**：共享 UI 层和业务逻辑层
- **API 服务**：Node.js Express 聚合网关，桌面端/移动端共享，多站点并发 + 服务端缓存 + 分页 + 跨站合并去重

### 1.3 项目命名

**RanNuan TV（冉暖TV）**

---

## 二、总体架构

### 2.1 系统架构图

```
┌──────────────────────────────────────────────────────┐
│          CMS 站点聚合 (5个可用站点)                     │
│  lzzy / ffzy / bfzy / suoni / bdzy                   │
│  MACCMS API: ac=videolist&wd/t/pg/out=json           │
└──────────────────────┬───────────────────────────────┘
                       │ HTTP (ac=videolist)
         ┌─────────────┴─────────────┐
         ▼                           ▼
┌──────────────────┐    ┌──────────────────────┐
│  RanNuan Server   │    │   共享 API 层         │
│  Node.js :3000    │    │   packages/shared     │
│  Express          │    │   TypeScript          │
├──────────────────┤    ├──────────────────────┤
│ /api/search       │    │ client.ts (API封装)   │
│ /api/search-stream│    │ media.ts (类型定义)    │
│ /api/category     │    │ store/index.ts (Zustand)
│ /api/detail       │    └──────────┬───────────┘
│ /api/home         │               │
│ /api/hot          │               ▼
│ /api/thumbnail    │    ┌──────────────────────┐
│ /api/img          │    │   桌面端 React        │
│ /api/proxy        │    │   Vite + TailwindCSS │
│ /api/multi-detail │    │   React Router v7    │
│ /api/check        │    │   hls.js + flv.js     │
│ /api/cache/clear  │    └──────────────────────┘
└──────────────────┘
```

### 2.2 数据拉取策略

- **分类浏览**：POST `/api/category` 按当前页聚合父类+子类 `type_id`，子分类用 `SUB_TYPE_MAP` 精准过滤
- **名称搜索**：GET `/api/search` 先查缓存/演员索引，再查 CMS 标题关键词，并批量 `detail` 补全演员字段
- **分页机制**：返回 `{ total, page, pageSize, totalPages, list, complete, outOfRange }`，前端不能完全信任 `totalPages`
- **首屏体验**：服务端分页缓存 + `category_cache.json` 磁盘热缓存 + 启动后台预热热门分类首屏；冷启动无缓存时 `/api/category` 先快速返回首屏临时结果，后台补全完整缓存；客户端有限缓存；不做全量分类拉取，避免 3~4w 条数据进入前端内存

### 2.3 跨站点合并策略

- **合并函数**：`crossSiteImagePicks()` — 按影片名去重，优选有图站点作为主条，附加 `sites` 字段记录所有可用站点
- **搜索接口**：GET `/api/search` 异步拉取后合并；SSE `/api/search-stream` 先逐源推送即时结果，所有站点完成后发 `merged` 事件替换为合并列表
- **首页/分类**：已使用 `crossSiteImagePicks` 合并，每条携带 `sites` 信息
- **详情页**：通过 `sites` 数组中的 `key:id` 对直接拉取各站详情，不再依赖名称模糊搜索

---

## 三、技术栈

### 3.1 整体技术选型

| 层级 | 技术 | 说明 |
|------|------|------|
| **Monorepo 管理** | pnpm workspace | 包管理 + 工作空间 |
| **共享层语言** | TypeScript | 类型安全 |
| **API 服务** | Node.js + Express | 聚合网关 |
| **CMS 协议** | MACCMS v1 | `ac=videolist` 返回完整字段含 vod_actor/vod_pic |
| **状态管理** | Zustand | 轻量级 |
| **HTTP 请求** | Axios (服务端) / Fetch (前端) | |
| **构建工具** | Vite | 快速开发服务器 |
| **CSS** | TailwindCSS | 深色主题 |

### 3.2 桌面端技术栈

| 组件 | 技术 |
|------|------|
| **应用框架** | Tauri v2（⏳待编译） |
| **前端框架** | React 19 + TypeScript |
| **路由** | React Router v7 |
| **播放器** | hls.js + flv.js + HTML5 Video（多格式自适应） |
| **图标** | lucide-react |

---

## 四、目录结构（当前实际）

```
RanNuan TV/
├── DEVELOPMENT_DOC.md          # 本文档
├── COLLECTION_RULES.md        # 五站采集规则文档（含索尼非标准映射）
├── server/
│   ├── server.js               # Express 主入口 (~1300行)
│   ├── actorIndex.js           # 演员/导演倒排索引模块
│   ├── db.json                 # 站点配置
│   └── ...
├── packages/
│   └── shared/
│       └── src/
│           ├── index.ts
│           ├── api/
│           │   ├── client.ts   # API客户端: search/searchStream/getMultiDetail/getCategory/getDetail/getHome
│           │   └── index.ts
│           ├── types/
│           │   ├── media.ts    # MediaItem(SiteInfo+sites) / MediaDetail
│           │   └── index.ts
│           └── store/
│               └── index.ts    # usePlayerStore / useSearchStore / useFavoriteStore
└── apps/
    └── desktop/
        ├── src/
        │   ├── main.tsx
        │   ├── App.tsx         # 路由配置（13条路由，含 /detail/source/:name）
        │   ├── styles/global.css
        │   ├── utils/
        │   │   ├── imageProxy.ts
        │   │   └── navigate.ts    # detailLink/siteChineseName/itemSiteName/SITE_NAMES
        │   ├── pages/
        │   │   ├── HomePage.tsx       # 首页
        │   │   ├── SearchPage.tsx     # SSE流式搜索 + onMerged
        │   │   ├── DetailPage.tsx     # 双模式：单站 / 多源站选择器
        │   │   ├── PlayerPage.tsx     # 播放页（代理+直连回退）
        │   │   ├── FavoritesPage.tsx  # 收藏页
        │   │   ├── MoviePage.tsx      # 电影分类 ↓
        │   │   ├── TVPage.tsx         # 电视剧
        │   │   ├── VarietyPage.tsx    # 综艺
        │   │   ├── AnimePage.tsx      # 动漫
        │   │   ├── ShortDramaPage.tsx # 短剧
        │   │   └── SportsPage.tsx     # 体育 ↑ 共享CategoryPage
        │   └── components/
        │       ├── layout/
        │       │   ├── Layout.tsx
        │       │   ├── Sidebar.tsx    # 侧边栏(升级版)
        │       │   └── TopBar.tsx
        │       ├── media/
        │       │   ├── Banner.tsx
        │       │   ├── MediaCard.tsx  # 多源角标 + 中文站点名
        │       │   └── MediaGrid.tsx
        │       ├── category/
        │       │   └── CategoryPage.tsx  # 共享分类页组件
        │       ├── common/
        │       │   └── Pagination.tsx    # 通用分页组件
        │       └── player/
        │           ├── VideoPlayer.tsx   # 播放器(currentSourceIndex)
        │           └── hlsPlayer.ts      # 多格式: HLS/FLV/TS/Native
        └── src-tauri/          # Tauri层 ⏳
```

---

## 五、API 接口完整清单

### 5.1 对外接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/search?wd=` | GET | 多源聚合搜索（含演员索引命中 + crossSiteImagePicks 合并去重） |
| `/api/search-stream` | POST | SSE 流式搜索（演员索引→CMS多策略→兜底扫描→merged） |
| `/api/category` | POST | 分类浏览（按需分页 + 父/子 type_id 聚合 + 子分类服务端过滤、支持 `subType`） |
| `/api/actor-index/stats` | GET | 演员索引统计 |
| `/api/actor-index/save` | POST | 手动持久化演员索引 |
| `/api/detail?site_key=&id=` | GET | 影片详情（ac=detail&ids=） |
| `/api/multi-detail?wd=&keys=` | GET | 跨站点多源详情（优先 keys: key:id,... 直拉，fallback 名称搜索） |
| `/api/home` | GET | 首页批处理（热门+5分类，缓存5分钟） |
| `/api/hot` | GET | 热门推荐（缓存3分钟） |
| `/api/check?key=` | GET | 单站点测速 |
| `/api/thumbnail?site_key=&id=` | GET | 缩略图懒加载（API优先→跨站匹配→爬站回退） |
| `/api/img?url=` | GET | 图片代理（Referer防盗链，bfzy + doubanio.com 专用） |
| `/api/proxy?url=` | GET | 视频代理（M3U8重写+CORS+流代理+**分享页解析**） |
| `/api/douban/recommend` | GET | 豆瓣单分类推荐（纯元数据，无CMS匹配，10分钟缓存） |
| `/api/douban/home` | GET | 豆瓣首页合并数据（hot+4分类，一次返回，hot前5条附加简介） |
| `/api/cache/clear` | POST | 清除服务端缓存（debug用） |
| `/api/admin/login` | POST | 管理员登录 |
| `/api/admin/sites` | GET/POST | 站点配置管理 |

### 5.2 分类接口请求格式

```json
POST /api/category
Request:  { "category": "movie", "wd": "电影", "page": 1, "pageSize": 30, "subType": "动作" }
Response: { "total": 520, "page": 1, "pageSize": 30, "totalPages": 18, "list": [...], "complete": true, "serverFiltered": true }
```

### 5.3 搜索SSE事件类型

| 事件类型 | 说明 |
|----------|------|
| `start` | 开始搜索，携带 totalSources + actorLike |
| `videos` | 逐源推送（source: "local"=索引 / "site:kw"=CMS / "scan:site"=兜底扫描） |
| `progress` | 进度更新 ({ completedSources, totalSources, phase: "scanning"\|undefined }) |
| `scan-progress` | 兜底扫描进度 |
| `merged` | **所有结果合并去重**（含 stats: { indexHits, cmsHits, merged }） |
| `complete` | 搜索完成 |

### 5.4 站点分类 type_id 映射

详见 `COLLECTION_RULES.md`

---

## 六、功能模块状态

### 6.1 已完成

| 模块 | 状态 | 说明 |
|------|:--:|------|
| monorepo 脚手架 | ✅ | pnpm workspace |
| server API 网关 | ✅ | 5站点聚合，服务端缓存，分类IDS映射 |
| shared 共享层 | ✅ | API客户端 + 类型定义 + Zustand Store |
| 首页 | ✅ | **豆瓣数据驱动**：Banner+5分类+热门推荐全部来自豆瓣API，秒开渲染；首页卡片解析后直跳详情，Banner 预解析后播放/详情直达 |
| 搜索页 | ✅ | SSE流式搜索 + merged合并去重 + 源筛选 + 排序 |
| **6个分类独立页面** | ✅ | 共用 CategoryPage 组件，子分类支持服务端 type_id 过滤 + 客户端回退 |
| **分页组件** | ✅ | Pagination（渐进式页码 + 跳页输入 + 每页条数） |
| **按需分页** | ✅ | 只拉当前请求页，smart预加载后续3页，突破原99页限制 |
| **分类首屏热缓存** | ✅ | `/api/category` 结果写入 `category_cache.json`，server 启动后恢复并后台预热桌面/移动第一页 |
| **分类冷启动快速首屏** | ✅ | 无完整缓存时先拉各站主分类/高优先级 type_id，返回 `complete:false` 临时首屏，后台继续刷新完整聚合缓存 |
| **详情页(多源模式)** | ✅ | 站点选择器 + 播放源标签(中文) + 剧集列表 + keys直拉详情 |
| 收藏功能 | ✅ | Zustand持久化 + 收藏页面 |
| **播放器多格式支持** | ✅ | HLS(m3u8) + FLV(flv.js动态加载) + TS(Blob M3U8包装) + Native(mp4/webm) |
| **分享页URL解析** | ✅ | 后端代理检测HTML → extractVideoUrlFromSharePage() → 4种模式提取 |
| **搜索/列表合并去重** | ✅ | crossSiteImagePicks附加sites字段，SSE末尾发merged事件 |
| **站点名中文化** | ✅ | siteChineseName/itemSiteName映射 + parsePlaySources三层层级匹配 |
| **播放器页重设计** | ✅ | 液态玻璃可折叠抽屉：左播放器 + 右信息/选集/推荐单页垂直布局，sticky吸顶播放器 |
| **演员搜索（倒排索引）** | ✅ | actorIndex.js 演员/导演倒排索引 + SSE三路搜索 + 兜底扫描 + 持久化 |
| **渐进式分页** | ✅ | 不显示虚拟尾页，只显示已加载页码 + hasNextPage控制 |
| **子分类服务端过滤** | ✅ | SUB_TYPE_MAP 按 type_id 精准过滤，total/分页正确，回退客户端 typeMatch |
| **侧边栏升级** | ✅ | 液态玻璃 + 流体动画 + 活跃发光指示器 + hover悬浮效果 |
| **播放器交互升级** | ✅ | 双击全屏切换 + 悬浮返回按钮 + 全屏控制条修复 + 断点静默seek续播 |
| **全局简介HTML剥离** | ✅ | stripHtml() 统一处理 CMS vod_content，覆盖首页Banner/详情页/播放页 |
| **豆瓣推荐集成** | ✅ | `/api/douban/home` 合并接口 + 内存缓存 + 首页纯豆瓣数据渲染 + 点击直跳搜索页 |
| **豆瓣图片代理** | ✅ | proxyImg 支持 doubanio.com → `/api/img` + Referer |
| **分页组件精简** | ✅ | 移除"每页N条"选择器（未接入后端） |
| **Tauri 桌面包** | ✅ | 自定义标题栏 + 系统托盘 + 后端 sidecar 自动启动 + 关闭隐藏托盘 |
| **启动动画** | ✅ | 影院风格 SplashScreen：放映机锥形光束 + Logo动画 + 漂浮元素 + Web Audio音效 + 观众席剪影；已在 App 入口重新挂载 |
| **播放器优化** | ✅ | 全屏去掉返回按钮、音量条显示百分比数值 |
| 页面keep-alive | ✅ | 模块级globalPageCache + 滚动位置恢复 |
| 图片懒加载 | ✅ | /api/thumbnail三方案 |
| 服务端视频代理 | ✅ | M3U8重写 + TS/MP4流代理 + 分享页解析 |
| **加载状态优化** | ✅ | 分类页翻页loading spinner + 空缓存不跳过加载 + API错误独立提示UI |

### 6.2 待完成

| 模块 | 状态 | 说明 |
|------|:--:|------|
| Tauri 壳 | ✅ | 自定义标题栏 + 系统托盘 + 关闭隐藏 + 后端自动启动 |
| 移动端 | ⏳ | Phase 3 |
| 播放历史 | ✅ | Zustand持久化 + 播放位置记录 + 历史页 + 直通播放器续播 |
| 设置页面 | ⏳ | 二期 |
| 弹幕系统 | ⏳ | 三期 |
| 子分类精准计数 | ⏳ | 当前 subCounts 为占位 0，需增加轻量 count 查询 |

### 6.3 已知问题（待修复）

| # | 问题 | 严重度 | 现象 |
|---|------|:---:|------|
| 1 | **Logo 404** | 🟡 中 | 嵌套路由下 sidebar logo 相对路径解析错误 |
| 2 | **全屏控制条偶发不响应** | 🟡 中 | 全屏时需 global mousemove 兜底，`onMouseLeave` 已禁用，偶发4s定时器不触发 |

---

## 七、架构决策记录

### 7.1 分类页架构

```
CategoryPage (共享组件)
  config: { categoryId, searchKeyword, Icon, subCategories }
    ↓ 6个薄包装器：MoviePage / TVPage / VarietyPage / AnimePage / ShortDramaPage / SportsPage

子分类过滤（双模式）：
  1. 服务端 type_id 过滤（优先）：命中 SUB_TYPE_MAP 时
     POST /api/category { subType: "动作" }
     → 各站只拉对应 type_id（如 lzzy:6, bfzy:21）
     → total/分页均为子分类专属
  2. 客户端 typeMatch 回退（兜底）：综艺/短剧/体育等无独立 type_id 的分类
     例：{ label: '日漫', typeMatch: '日韩|日本|日漫' } → items.filter(...)
```

### 7.1-b 渐进式分页

```
不显示虚拟尾页（如 1700），只显示已成功加载的页码

数据流：
  loadedPagesRef (Set<number>) — 已加载页码
  hasNextPage — 当前页返满 pageSize 条为 true
  maxLoadedPage = Math.max(...loadedPages)
  maxClickablePage = hasNextPage ? maxLoadedPage + 1 : maxLoadedPage

页码按钮规则：
  显示：1 ... (当前页附近) ... maxLoadedPage [+ 可探索下一页]
  不显示：尾页 totalPages（CMS 可能没有那么多页）
  下一页：仅 hasNextPage 为 true 时可用
  跳页：限制在 1 ~ maxClickablePage
```

### 7.1-c 演员搜索架构

```
actorIndex.js — 倒排索引模块
  建索引：indexItems(items) → actorIndex / directorIndex (Map<"周星驰", [MediaItem[]]>)
  持久化：每 10 分钟 → actor_index.json；SIGINT/SIGTERM → 保存
  数据源：/api/category 返回时顺带 indexItems(list)

/api/search-stream 三路搜索：
  1. 演员索引（毫秒级）→ source: "local"
  2. CMS 多关键字（索引 < 20 条时：wd + wd 电影 + wd 电视剧）
  3. 兜底扫描（有效演员结果 < 10 条时触发，限 2 type_id × 3 页）

isLikelyActor(wd)：中文 2~6 字 / 英文全名，排除分类关键词
演员名解析：去括号 + 英文全名保留不拆空格 + 过滤数字/超长名
每演员名最多 200 条（FIFO 淘汰）
```

### 7.2 数据加载策略

- **`ac=videolist` 替代 `ac=list`**：返回完整字段含 vod_actor/vod_pic（参考 MoonTVPlus）
- **分类拉取**：按子 type_id 并发拉取（非父级type_id，因CMS item挂在子分类下）
- **按需分页**：POST `/api/category` 只拉取当前请求页 `pg=p`，不再后台预拉全量（突破原99页限制）
- **冷启动快速首屏**：第 1 页无缓存且非子分类时，后端先拉各站主分类/高优先级 type_id，约 3 秒内返回 `complete:false` 临时结果；完整父+子 type_id 聚合在后台刷新并写入缓存
- **智能预加载**：分类页自动预取当前页+1/+2/+3 共3页存入 `globalPageCache`，首页空闲时预取电影/电视剧/综艺/动漫首屏，命中缓存直接秒开
- **加载状态**：首次/翻页无缓存时展示 loading；若拿到 `complete:false` 临时结果则先展示列表、不固化缓存，数秒后静默刷新完整结果；API 错误独立红色提示+重试按钮；真正无数据才显示"暂无内容"

### 7.2-a Android 移动端流畅度策略（v3.5）

桌面端本地运行时 CPU/内存/网络连接资源更充足，可以使用较积极的分类预取和 hover/后台解析；Android 端资源更紧，且 ExoPlayer 拉流、图片加载、API 请求共用移动网络与 OkHttp 资源。移动端后续采用与桌面端不同的策略：

| 模块 | 桌面端 | Android 移动端 |
|------|--------|----------------|
| 分类列表 | 可预取相邻页，依赖内存缓存提升翻页 | 改为按需分页，优先 Paging 3/ViewModel 分页状态机；禁止全局批量预热抢当前请求 |
| 首页推荐 | 可后台预解析部分 Banner/推荐 | 首页只展示元数据；点击时按 `siteKey:vodId` 或 `keys` 精准拉详情 |
| 详情匹配 | 多源 `keys` 直拉，名称搜索兜底 | Identity-first：`siteKey + vodId` 优先，标题/导演/年份只做校验与排序 |
| 播放链路 | hls.js/flv.js 与浏览器缓存处理拉流 | Media3 ExoPlayer 独立 LoadControl；播放拉流不与分类/详情预热抢资源 |
| 缓冲提示 | 可依赖 HTML5 player 事件 | Android 仅在 `!isPlaying` 且缓冲持续超过阈值时展示，不在流畅播放中弹层 |

> 结论：移动端不能继续通过“更多预热”解决卡顿，必须减少后台并发、按需加载、精准匹配、播放器链路隔离。

### 7.3 跨站点合并去重

- **`crossSiteImagePicks()`**：按 `vod_name` 前15字符小写去空格分组，组内按图片质量排序，保留最优作为显示项，附加 `sites: [{key, name, id}]` 记录所有来源站点
- **列表页**：GET `/api/search`、`/api/home`、`/api/category` 全部使用该函数合并
- **SSE搜索**：先逐源推送即时结果（保持流畅感），所有站点完成后发送 `merged` 事件替换列表
- **详情页**：从 URL `?keys=lzzy:81959,ffzy:64631` 直接按 ID 拉取各站详情，无需依赖名称模糊匹配

### 7.4 播放器页面设计

**液态玻璃可折叠抽屉布局**（Layout 内，保留全局导航）：
```
┌──Sidebar──┬───────────────────────┬──Glass Drawer──┐
│  首页     │                      │ ← [收起按钮]     │
│  电影     │   视频播放器          │   封面图         │
│  电视剧   │   (hls.js/flv.js)    │   标题 + 元数据   │
│  综艺     │   双击→全屏           │   简介(stripHtml) │
│  动漫     │   ← 悬浮返回按钮      │   演员           │
│  短剧     │                      │   选集(多集/多源)  │
│  体育     │                      │   相关推荐(2列网格) │
│  收藏     │                      │                  │
│  历史     │                      │                  │
└──────────┴───────────────────────┴──────────────────┘
        展开按钮(收起时) → [◀]    [▶] ← 收起按钮(hover浮现)
```

**核心特性**：
- **Layout 集成**：/player 路由在 Layout 内，保留侧边栏 + 顶部搜索栏
- **液态玻璃抽屉**：`backdrop-blur-3xl bg-zinc-950/80` + `border-white/[0.06]` + `shadow-2xl`
- **收起/展开**：统一位置（侧栏左边缘），收起时 hover 浮现，展开时常显半透明
- **单页垂直布局**：信息→选集→推荐，无 Tab 分离，自然滚动
- **双击全屏**：YouTube 风格交互，单击播放暂停，双击进入/退出全屏
- **悬浮返回按钮**：左上角跟随控制条，4 秒未动鼠标自动隐藏
- **断点静默续播**：历史记录点击直通播放器，自动 seek 到上次位置
- **选集条件显示**：仅多集剧集或多源时显示选集区块
- **相关推荐**：分类 API + 演员兜底，2列横向网格 + loading/空态/重试

### 7.5 图片处理规则

| 站点 | 策略 |
|------|------|
| bfzy | `/api/img` 代理 + Referer: https://bfzyapi.com |
| lzzy | 旧域名(pic.lzzypic.com/img.lzzyimg.com)被墙；新域名 img.lzipic.com 待验证 |
| 其他 | 直连 |

### 7.6 播放地址解析与多格式支持

**原始格式**：
```
格式: "源名$URL#源名$URL$$$源2$URL#源2$URL"
$$$ → 分隔不同播放源
#   → 分隔同一源下的剧集
$   → 分隔剧集名称和URL
```

**URL 类型识别与播放策略**：
| 类型 | 示例 | 处理方式 |
|------|------|---------|
| 直接 m3u8 | `.../index.m3u8` | hls.js 播放 |
| 分享页 HTML | `.../share/6364d...` | 代理提取 `var main="..."` → m3u8 |
| FLV 直连 | `.../video.flv` | flv.js 动态加载播放 |
| TS 直连 | `.../video.ts` | Blob M3U8 包装 → hls.js |
| 其他直连 | `.mp4` / `.webm` 等 | 原生 `<video>` 播放 |

**分享页解析流程**：
```
无扩展名 URL → 直连失败 → 自动切换代理
  → /api/proxy?url=share_url
    → 后端 detect: text/html
    → extractVideoUrlFromSharePage()
      ├─ 模式1: var main = "/path/index.m3u8?sign=xxx"
      ├─ 模式2: var url/video/src = "..."
      ├─ 模式3: 绝对 m3u8 URL
      └─ 模式4: 相对 m3u8 路径
    → 解析为完整 m3u8 → 获取并重写 → 返回
  → 前端 hls.js 接收 → 正常播放 ✅
```

**播放源名称中文化**（三层匹配）：
| 层级 | 策略 | 示例 |
|------|------|------|
| 精确映射 | 内置已知名直接转换 | `liangzi` → `量子.线路一` |
| 前缀匹配 | 按站点前缀识别 + 保留后缀 | `lz超清` → `量子.超清` |
| 站点上下文 | 知道siteKey，用中文站名+原名 | `zy.m3u8`(bdzy) → `百度.zy` |
| 通用回退 | 序号化 | `xyz` → `线路1.直连` |

---

## 八、开发环境

### 8.1 启动方式

```bash
# ===== 方式1：网页开发（分别启动前后端）=====
# 终端1：API 服务
cd "RanNuan TV/server"
node server.js
# → http://localhost:3000

# 终端2：前端
cd "RanNuan TV/apps/desktop"
npx vite
# → http://localhost:1420


# ===== 方式2：Tauri 桌面开发（自动启动后端）=====
cd "RanNuan TV/apps/desktop"
pnpm tauri dev
# → Tauri webview 窗口 + 自动编译 Rust + 自动启动 node server.js


# ===== 方式3：Tauri 生产打包 =====
cd "RanNuan TV/apps/desktop"
pnpm tauri build
# → 安装包在 src-tauri/target/release/bundle/
```

### 8.2 环境要求

| 工具 | 版本 | 用途 |
|------|------|------|
| Node.js | ≥ 18.0 | 后端 + 前端构建 |
| pnpm | ≥ 8.0 | 包管理 |
| Rust | ≥ 1.70 | Tauri 桌面壳编译 |

### 8.3 Tauri 桌面应用架构

```
RanNuan TV.exe 启动
  → SplashScreen: 影院启动动画（3s，放映机光束+Logo+音效）
  → Rust 壳: 自定义标题栏 + 系统托盘
  → setup() 自动 spawn: node.exe server.js (CREATE_NO_WINDOW 无黑窗口, 监听 :3000)
  → WebView 加载前端 (dist/)
  → 前端通过 localhost:3000 访问后端 API
  → 点 × → 隐藏到托盘（不退出）
  → 托盘右键菜单: 显示/隐藏 | 退出（退出时 taskkill 清理后端进程）
```

**启动动画（SplashScreen）设计**：
| 元素 | 实现 |
|------|------|
| 美学方向 | 黄金时代影院复古 — 琥珀金暖光 + 深黑背景 |
| Logo 入场 | 弹性旋转 + 光晕脉冲（cubic-bezier 弹簧曲线） |
| 放映机光束 | 底部光源 → 双层 clipPath 锥形向上扩散 + 灰尘颗粒漂浮 |
| 胶卷穿孔 | 左右两侧 12 个穿孔明暗交替 |
| 影院元素 | 🍿🥤🎞️⭐ 爆米花/可乐/胶卷/星星各自独立漂浮动画 |
| 氛围 | 底部烟雾层 + 观众席剪影 |
| 音效（Web Audio） | 放映机低频嗡鸣 + 胶卷咔哒三连击 + 开场钟声渐弱 |
| 消退 | 3 秒后淡出进入首页 |

**入口挂载说明**：`apps/desktop/src/App.tsx` 通过 `showSplash` 状态挂载 `SplashScreen`，`onFinish` 后卸载。该动画是前端覆盖层，不阻塞后端 sidecar 启动；分类后端预热仍由 `server/server.js` 在监听成功后后台执行。

**Tauri 关键配置** (`src-tauri/`)：

| 文件 | 作用 |
|------|------|
| `tauri.conf.json` | 窗口配置、`decorations: false`隐藏默认标题栏、trayIcon、resources 打包 server/ |
| `src/main.rs` | 托盘菜单、自动启动后端(CREATE_NO_WINDOW隐藏CMD)、关闭→隐藏、PID杀进程 |
| `capabilities/default.json` | 窗口操作/Shell 权限 |
| `Cargo.toml` | Rust 依赖（tauri + tray-icon + shell-plugin） |

### 8.4 打包检查清单

打包前确认以下项目全部就绪：

| # | 检查项 | 状态 | 说明 |
|---|--------|:---:|------|
| 1 | `icons/icon.ico` | ✅ | Windows 应用图标（tauri icon 生成） |
| 2 | `icons/icon.icns` | ✅ | macOS 图标 |
| 3 | `icons/icon.png` | ✅ | 主图标 (512x512) |
| 4 | `icons/32x32.png` ~ `128x128@2x.png` | ✅ | 多尺寸 PNG |
| 5 | `icons/Square*Logo.png` | ✅ | Windows Store 图标 |
| 6 | `icons/StoreLogo.png` | ✅ | Windows Store 标识 |
| 7 | `public/icon.png` | ✅ | Sidebar logo (1151x1151) |
| 8 | `dist/icon.png` | ⚠️ | 需与 public/ 同步（构建时自动） |
| 9 | `server/node_modules/` | ✅ | 后端依赖已安装（**必须用 npm，见下方说明**） |
| 10 | `tauri.conf.json` | ✅ | 窗口/资源/图标/构建命令配置 |
| 11 | `capabilities/default.json` | ✅ | 窗口操作/Shell 权限 |
| 12 | `src/main.rs` | ✅ | 托盘+后端启动(CREATE_NO_WINDOW)+关闭隐藏 |
| 13 | `Cargo.toml` | ✅ | Rust 依赖 |
| 14 | 系统 Node.js | ⚠️ | 生产环境需安装 Node.js（应用不内置） |
| 15 | 无其他进程占用 target/ | ⚠️ | dev 环境必须先关闭，否则 os error 32 文件锁定 |

> **注意**：第 14 项 — 因 `pkg` 二进制仓库故障，`node.exe` 未打包进应用。后端由 `main.rs` 通过 `Command::new("node")` 启动，要求用户系统安装了 Node.js 且 `node` 命令在 PATH 中。

#### ⚠️ pnpm 符号链接 vs NSIS 打包（重要）

**问题**：pnpm 安装的 `server/node_modules` 使用 Windows 符号链接（`SYMLINKD`），所有包指向上层 `../../node_modules/.pnpm/...`。NSIS 的 `File /r` 指令**不会解析 Windows 符号链接**，导致 `node_modules` 目录打不进安装包 → makensis 编译失败或安装包缺失依赖。

**解决方案**：打包前必须用 npm 在 `server/` 目录独立安装一份真实依赖：

```bash
# 1. 删除 pnpm 的符号链接 node_modules
cd "RanNuan TV/server"
rmdir /s /q node_modules

# 2. 用 npm 安装真实文件（非符号链接）
npm install --production

# 3. 验证无符号链接
dir node_modules | findstr /i "SYMLINK" && echo "FAIL!" || echo "OK"

# 4. 打包
cd ../apps/desktop
pnpm tauri build
```

> **注意**：每次在项目根目录执行 `pnpm install` 后，`server/node_modules` 会被 pnpm 重新创建为符号链接。**打包前务必重新执行上述步骤**。建议将此步骤加入打包脚本。

#### ⚠️ Windows 图标缓存问题

安装后桌面/开始菜单图标显示为默认图标而非自定义图标时，通常是 **Windows 图标缓存** 问题（图标文件本身没问题）。清理方法：

```cmd
ie4uinit.exe -show
taskkill /IM explorer.exe /F
DEL /A /Q "%localappdata%\IconCache.db"
DEL /A /F /Q "%localappdata%\Microsoft\Windows\Explorer\iconcache*"
start explorer.exe
```

> `installer.nsi` 是 Tauri 每次打包自动生成的，**不要手动修改**。图标配置统一通过 `tauri.conf.json` + `icons/` 目录控制。

### 8.5 搜索页客户端去重

搜索页使用 SSE 流式接收结果，`videos` 事件逐个源推送原始数据，`merged` 事件需等全量扫描结束。为避免搜索过程中显示未合并的重复卡片，前端在 `videos` 回调中实时执行客户端去重：

```typescript
// 每次收到 videos 事件，对累计结果做轻量去重
setResults(prev => dedupClientSide([...prev, ...videos]));

// dedupClientSide: 按标题前10字符归一化分组，优先保留有图版本
```

服务端 `crossSiteImagePicks` 的归一化也已增强（去括号+年份+季数）。

---

## 九、参考项目

| 项目 | 借鉴内容 |
|------|---------|
| **KVideo** | hls.js 播放器架构、SSE 流式搜索、相关性评分、**豆瓣数据驱动首页** |
| **MoonTVPlus** | ac=videolist 协议、服务端内存缓存（Map+TTL）、flv.js 多格式支持、图片代理、**首页纯豆瓣元数据策略** |
| **量子资源 lzizy.com** | MACCMS type_id 分类体系、分享页 DPlayer 提取 |

---

## 十、经验教训

| 教训 | 说明 |
|------|------|
| **CMS item挂在子分类** | type_id=1（电影片）几乎无item，需拉子type_id（6,7,8...） |
| **ac=list 不返回演员字段** | 必须用 `ac=videolist` 才能获取 vod_actor/vod_pic |
| **wd 只搜索 vod_name** | CMS API 的 wd 参数不搜索演员/导演字段 |
| **子分类过滤走前端** | 后端搜索"电影 动作片"返回0条，前端 type_name 匹配才是正解 |
| **CMS 源 URL 可能不是视频** | lzzy 的 `liangzi` 源返回 DPlayer 分享页 HTML，真实 m3u8 藏在 `var main` 里 |
| **代理模式下格式检测失效** | 代理 URL 无扩展名，需解析 `url` 参数获取原始 URL；无扩展名视为分享页走 hls.js |
| **多源详情不能靠名称搜索** | 各站点视频名不完全一致，应从搜索结果 `sites` 数组直接传 `key:id` 拉取详情 |
| **SSE 搜索需末尾合并** | 逐站推送即时结果，结束后发 `merged` 事件替换列表（带 sites 字段），前端 onMerged 回调处理 |
| **子分类过滤应走服务端** | 前端 typeMatch 过滤仅作用于当前页 30 条，常为空且分页错误；服务端按 type_id 拉取可精准分页 |
| **分类页尾页不可信** | CMS 聚合场景下 total 为各子分类 pagecount 求 max 的估算值，实际数据量远小于显示值；渐进式页码更合理 |
| **演员搜索需倒排索引** | CMS wd 参数只搜 vod_name，不搜 vod_actor/vod_director；浏览即建索引可实现毫秒级演员搜索 |
| **搜索列表需 detail 补全演员字段** | 部分资源站 `videolist/list` 不稳定返回 `vod_actor`，片名搜索结果应批量 `ac=detail&ids=` 补全后写入演员索引 |
| **演员索引不能无限膨胀** | `actor_index.json` 已可能达到数百 MB，索引只保存搜索卡片字段，不保存 `vod_content`，每演员最多 200 条 |
| **共享后端影响两端** | `server/server.js` 同时服务桌面端和 Android；改 `/api/search`、`/api/category`、`/api/multi-detail` 必须同时考虑两端调用契约 |
| **分类冷启动不能等完整聚合** | 电影/电视剧要跨 5 站拉父类+多个子类 type_id，冷启动完整聚合可能 10s+；首屏应快速返回部分高质量结果，再后台补完整缓存 |
| **父分类分页不完全可信** | 文档中父类 `t=1/t=2` 理论支持全类分页，但部分资源站返回 `pagecount`/数据不稳定；全部分类仍用父+子 type_id 聚合保证翻页 |
| **移动端缓存必须有限** | Android 端只缓存最近分页/搜索/详情，不能缓存整分类；电影/电视剧可达 3~4w 条，整类内存缓存会导致 OOM 风险 |
| **索尼编号不标准** | suoni 电视剧(14=欧美/17=港剧)、综艺(26↔27)、动漫(32→44)均与标准 MACCMS 不同，需单独映射 |
| **短剧/体育有子类** | bfzy 短剧9子类(65-74)、索尼短剧8子类(54-73)；体育量子37-40/bfzy54-57/索尼49-52均有子类 |
| **标签有效性需验证** | 无对应 type_id 的标签应删除而非保留（如综艺真人秀/短剧都市），避免误导用户 |
| **首页不要预匹配CMS** | 豆瓣20条×5站点CMS搜索=100次请求/分类，白屏5-10s；参考KVideo/MoonTVPlus只展示豆瓣元数据，点击才按需搜索 |
| **iyuns API不可靠** | 第三方豆瓣聚合API（api.iyuns.com）频繁`Internal Error`，降级为豆瓣原生`j/subject_abstract`获取简介 |
| **豆瓣图片需代理** | `doubanio.com`域名有防盗链，必须走`/api/img`+Referer头；img元素onError不能用innerHTML替换会覆盖文字 |
| **跨站合并标题归一化** | 不同CMS源标题格式不同（`南部档案`/`南部档案2026`/`南部档案(2026)`），合并key需去括号+年份+季数，仅保留核心片名 |
| **SSE搜索结果需客户端实时去重** | `merged`事件要等全量扫描结束才发，搜索过程中用户看到的是未合并原始列表；应在`videos`回调中客户端去做合并去重 |
| **Tauri NSIS快捷方式图标** | `installerIcon`只控制安装包exe文件图标，桌面/开始菜单快捷方式默认从`ran-nuan-tv.exe`提取；需将`.ico`复制到安装目录并在`CreateShortcut`中显式指定路径 |
| **pnpm符号链接NSIS不解析** | pnpm的`node_modules`是`SYMLINKD`指向上层`.pnpm`，NSIS的`File /r`不解析Windows符号链接，导致打包失败；打包前必须`npm install --production`安装真实文件 |
| **pnpm install会重建符号链接** | 每次根目录`pnpm install`后`server/node_modules`会变回符号链接，打包前需重新`npm install --production` |
| **Windows图标缓存坑** | 安装后图标显示不对不一定是配置问题，可能是Windows图标缓存；清`IconCache.db`+`iconcache*`即可 |
| **dev环境文件锁定** | 打包时dev环境必须先关闭，否则`target/release/`下的exe被占用导致os error 32 |
| **CREATE_NO_WINDOW隐藏CMD** | Rust spawn node进程时加`CREATE_NO_WINDOW`(0x08000000)标志+`Stdio::null()`，CMD黑窗口不再弹出；此时`WINDOWTITLE`过滤失效，需改用PID杀进程 |

---

### 7.7 观看历史 + 收藏架构

**历史记录**（`useHistoryStore`）：
- 持久化到 `rannuan-history` (localStorage, max 100)
- `HistoryEntry: { item, watchedAt, playPosition, duration }`
- 播放时 `addToHistory`，播放中 `updatePlayPosition` 周期性更新位置
- HistoryPage 点击直通播放器（`playerLink`），不中转详情页
- 历史页显示播放进度条 + 时间

**收藏**（`useFavoriteStore`）：
- 持久化到 `rannuan-favorites`，复合键 `site_key + vod_id`

### 7.8 Sidebar 液态玻璃设计

- 基础：`bg-zinc-900/85 backdrop-blur-2xl border-r border-white/[0.04]`
- 活跃项：发光背景 `bg-brand-500/10` + 左侧指示条 `shadow-brand-500/30` + 圆点脉冲
- Logo：`ring-1 ring-white/5 hover:ring-brand-500/40` 过渡
- 文字渐变：`text-gradient-cyan` 用于特殊强调
- 流体动画：`animate-slideIn` 指示条 + `hover:scale-110 translate-x-0.5` 图标

### 7.9 豆瓣推荐架构

**数据流**（参考 KVideo/MoonTVPlus，首页纯豆瓣数据，点击才搜 CMS）：
```
GET /api/douban/home（一次请求）
  → 并发 5 个豆瓣分类 + 内存缓存 10 分钟
    → hot: movie+热门×18, dianshiju: tv+热门×12
    → dianying: movie+最新×12, zongyi: tv+综艺×12, dongman: movie+动画×12
  → hot 前 5 条并发拉 j/subject_abstract 获取简介
  → 返回纯元数据 (title/cover/rate/year/abstract)
    ↓
首页秒开渲染豆瓣卡片（海报+⭐评分+标题+年份）
  → Banner：前5条 → MediaItem映射，轮播展示
  → 首页卡片 hover/focus 预解析 CMS 资源，点击命中后直跳详情页
  → Banner 前4条后台预解析，播放/详情点击优先复用缓存直达
```

**关键设计决策**：
- ❌ 不在首页预匹配 CMS（KVideo/MoonTVPlus 同理，避免白屏 5-10s）
- ✅ 首页纯豆瓣元数据，0.2-0.5s 秒开
- ✅ 客户端首页点击不再直跳搜索页：先复用预解析结果进详情页，未命中才兜底搜索
- ✅ 豆瓣图片走 `/api/img` 代理 + Referer 头解决防盗链
- ✅ 内存缓存 TTL 10 分钟，过期自动刷新

**客户端匹配**（已废弃）：之前客户端做 `norm(豆瓣标题) ≈ norm(CMS名称)` 匹配，但因标题差异大命中率低，已改为直跳搜索页方案。

---

### 7.10 分类首屏快速返回架构

**目标**：解决桌面端/移动端首次进入分类时，完整多站多 type_id 聚合导致的 10s+ 等待。

```
POST /api/category { category, page: 1, pageSize, subType: undefined }
  ├─ 命中完整缓存 → 直接返回 complete:true
  ├─ 后台完整刷新正在进行 → 最多等待约 2.5s，完成则返回完整缓存
  └─ 无缓存/未完成 → 快速首屏路径
      ├─ 每个资源站只拉 root type_id + 2 个高优先级 type_id
      ├─ 约 3.2s 内合并去重后返回 complete:false, warming:true
      └─ startCategoryFullRefresh() 后台执行完整父+子 type_id 聚合
          → 写入内存缓存 + category_cache.json
```

**前端处理**：
- `CategoryPage` 收到 `complete:false` 时先展示列表，但不写入 `globalPageCache`。
- 约 6.5 秒后保留当前列表静默再请求一次；如果后台完整缓存已生成，则替换成 `complete:true` 完整结果。
- 首页加载完成后延迟预取 `movie/tv/variety/anime` 首屏，用户点分类时更容易命中缓存。

**兼容性**：
- 子分类、非首页分页、`refresh:true` 仍走完整聚合路径。
- 响应仅新增 `warming:true` 和 `complete:false` 的临时状态；移动端如果暂未特殊处理，也能正常展示 `list`。

---


> 📅 文档版本：v5.8
> 📝 最后更新：2026-06-29（分类冷启动快速首屏、后台完整缓存刷新、桌面启动动画恢复挂载）
> 👤 作者：RanNuan TV 开发团队
