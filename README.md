<p align="center">
  <img src="RanNuan%20TV/apps/desktop/public/icon.png" width="128" alt="RanNuan TV" />
</p>

<h1 align="center">RanNuan TV（冉暖TV）</h1>
<p align="center">基于 MACCMS 视频聚合 API 的跨平台视频播放器</p>
<p align="center">
  <strong>Windows 桌面端</strong> · <strong>Android 移动端</strong> · <strong>共享 Node.js 后端</strong>
</p>

---

## ✨ 特性

- 🎬 **5 站聚合** — 量子资源 / 非凡影视 / 暴风资源 / 索尼资源 / 百度资源，多源自动合并去重
- 🖥️ **Windows 桌面端** — Tauri + React 19 + hls.js/flv.js，原生桌面体验
- 📱 **Android 移动端** — Kotlin + Jetpack Compose + Media3 ExoPlayer，爱优腾级播放器交互
- ⚡ **SSE 流式搜索** — 毫秒级演员索引 + CMS 多策略 + 实时合并推送
- 🎞️ **多格式播放** — HLS / FLV / TS / MP4 / WebM / 分享页自动解析
- 📡 **DLNA 投屏** — 支持小米/海信/TCL/索尼等主流电视，SSDP 发现 + SOAP 控制
- 📊 **豆瓣数据首页** — 秒开渲染，点击直搜 CMS，无预加载白屏
- 🎨 **液态玻璃设计** — 毛玻璃侧边栏 + 流体动画 + 深色主题
- 🔄 **播放续播** — 自动保存进度，历史页一键跳回上次观看位置
- 📦 **Tauri 桌面包** — 自定义标题栏 + 系统托盘 + 后端自动启动 + 关闭隐藏

---

## 🏗️ 架构

```
                    ┌──────────────────┐
                    │  MACCMS CMS 站点   │
                    │  lzzy / ffzy /    │
                    │  bfzy / suoni /   │
                    │  bdzy (5站聚合)   │
                    └────────┬─────────┘
                             │ HTTP
                    ┌────────▼─────────┐
                    │  RanNuan Server   │
                    │  Node.js :3000    │
                    │  Express 网关     │
                    │  · 搜索/分类/详情  │
                    │  · 视频/图片代理   │
                    │  · 演员倒排索引    │
                    │  · 服务端缓存      │
                    └──┬────────────┬──┘
                       │            │
              ┌────────▼──┐  ┌─────▼──────────┐
              │ 桌面端      │  │ Android 移动端  │
              │ Tauri+React │  │ Kotlin+Compose │
              │ hls.js/flv  │  │ ExoPlayer      │
              └─────────────┘  └────────────────┘
```

---

## 🛠️ 技术栈

### 整体

| 层级 | 技术 |
|------|------|
| Monorepo | pnpm workspace |
| 后端 | Node.js + Express |
| 协议 | MACCMS v1 (`ac=videolist`) |
| 状态管理 | Zustand（桌面）/ Compose State（Android） |

### 桌面端

| 组件 | 技术 |
|------|------|
| 应用框架 | Tauri v2 |
| 前端 | React 19 + TypeScript + Vite |
| 路由 | React Router v7 |
| 播放器 | hls.js + flv.js + HTML5 Video |
| UI | TailwindCSS |
| 图标 | lucide-react |

### Android 端

| 组件 | 技术 |
|------|------|
| 语言 | Kotlin 1.9.22 |
| UI | Jetpack Compose + Material 3 |
| 播放器 | Media3 ExoPlayer 1.2.1 |
| 网络 | Retrofit + OkHttp + Gson |
| 图片 | Coil Compose |
| Min SDK | API 24 (Android 7.0) |

---

## 🚀 快速启动

### 桌面端

```bash
# 1. 启动后端
cd "RanNuan TV/server"
node server.js

# 2. 启动前端
cd "RanNuan TV/apps/desktop"
npx vite
# → http://localhost:1420

# 3. Tauri 桌面包（自动启动后端）
pnpm tauri dev
```

### Android 端

```bash
# 1. 启动后端
cd "RanNuan TV/server"
node server.js

# 2. Android Studio 打开
# File → Open → apps/mobile-android
# 🐘 Sync → ▶️ Run
```

> 模拟器默认 API 地址 `http://10.0.2.2:3000`，真机需改为电脑局域网 IP。

---

## 📂 目录结构

```
RanNuan TV/
├── server/                        # 共享后端
│   ├── server.js                  # Express 主入口 (~1300行)
│   └── actorIndex.js              # 演员倒排索引
├── packages/shared/               # 共享 TS 类型（桌面端）
├── apps/
│   ├── desktop/                   # Windows 桌面端
│   │   ├── src/                   # React 源码
│   │   │   ├── pages/             # 页面组件
│   │   │   └── components/        # UI 组件
│   │   └── src-tauri/             # Tauri Rust 壳
│   └── mobile-android/            # Android 移动端
│       └── app/src/main/java/com/rannuan/tv/
│           ├── data/api/          # API 层
│           ├── data/model/        # 数据模型
│           └── ui/screens/        # Compose 页面
└── README.md
```

---

## 🎯 功能矩阵

| 功能 | 桌面端 | Android |
|------|:---:|:---:|
| 首页（豆瓣推荐 + Banner 轮播） | ✅ | ✅ |
| 聚合搜索（SSE 流式 + 演员索引） | ✅ | ✅ |
| 分类浏览（6类 + 子分类过滤 + 分页） | ✅ | ✅ |
| 多源详情（站点选择 + 剧集列表） | ✅ | ✅ |
| 视频播放（HLS/FLV/TS/MP4） | ✅ | ✅ |
| 分享页自动解析 | ✅ | ✅ |
| 播放器手势交互（亮度/音量/进度） | — | ✅ |
| DLNA 投屏 | — | ✅ |
| 画面尺寸 / 镜像反转 / 播放方式 | — | ✅ |
| 收藏 + 观看历史 + 断点续播 | ✅ | ✅ |
| 液态玻璃侧边栏 | ✅ | — |
| 系统托盘 + 关闭隐藏 | ✅ | — |
| 影院启动动画 | ✅ | ✅ |

---

## 📡 API 接口

| 接口 | 用途 |
|------|------|
| `/api/douban/home` | 豆瓣首页（hot + 4 分类，10min 缓存） |
| `/api/search?wd=` | 聚合搜索（缓存 + 演员索引 + CMS） |
| `/api/category` | 分类浏览（分页 + 子分类过滤） |
| `/api/multi-detail?wd=&keys=` | 多源详情 |
| `/api/img?url=` | 图片代理（防盗链） |
| `/api/proxy?url=` | 视频代理（m3u8 重写 + 分享页解析） |
| `/api/douban/recommend` | 豆瓣推荐 |

---

## 📄 许可证

MIT License

---

<p align="center">
  <sub>Built with ❤️ by RanNuan TV 开发团队</sub>
</p>
