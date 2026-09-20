<p align="center">
  <img src="RanNuan%20TV/apps/desktop/public/icon.png" width="128" alt="RanNuan TV" />
</p>

<h1 align="center">RanNuan TV（冉暖TV）</h1>
<p align="center">基于 MACCMS 视频聚合 API 的跨平台视频播放器</p>
<p align="center">
  <strong>Windows 桌面端</strong> · <strong>Android 移动端</strong> · <strong>Android TV 端</strong> · <strong>共享 Node.js 后端</strong>
</p>

---

## ✨ 特性

- 🎬 **5 站聚合** — 量子资源 / 非凡影视 / 暴风资源 / 索尼资源 / 百度资源，多源自动合并去重
- 🖥️ **Windows 桌面端** — Tauri + React 19 + hls.js/flv.js，原生桌面体验
- 📱 **Android 移动端** — Kotlin + Jetpack Compose + Media3 ExoPlayer，爱优腾级播放器交互
- 📺 **Android TV 端** — 基于 TVBoxOS 改造，支持遥控器导航、TV 播放器控制栏和统一品牌视觉
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

                     ┌──────────────────────┐
                     │ Android TV 端         │
                     │ TVBoxOS APK           │
                     │ IJK / ExoPlayer       │
                     │ 内置本地代理 :9978     │
                     └──────────────────────┘
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

### Android TV 端

| 组件 | 技术 |
|------|------|
| 工程目录 | `TVBoxOS-main/` |
| 应用 ID | `com.github.tvbox.osc.rannuan` |
| 构建方式 | Android Gradle Plugin 7.2.2 + Gradle 7.5 |
| 播放器 | TVBox 内置 IJK / ExoPlayer |
| 支持架构 | `armeabi-v7a` + `arm64-v8a`（`java` flavor） |
| 最低系统 | Android API 19 |

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

### Android TV 端

TV 端可以直接打包成独立 APK 安装，不需要部署本项目的 Node.js 服务。TVBox 的播放器、解析引擎、QuickJS 和本地代理都会随 APK 打包；应用内部按需启动 `9978` 端口的本地服务，用于视频代理、缓存和远程控制，这不是需要部署到云服务器上的服务。

TV 端仍需要联网访问影视配置、资源站和解析地址。当前没有用户配置时，应用默认读取 `http://xhztv.top/4k.json`；正式发布前建议在 TVBox 设置中换成自己维护的稳定配置地址。配置地址失效只会导致内容或播放不可用，不影响 APK 安装。

#### Android Studio 打包

1. 用 Android Studio 打开 `TVBoxOS-main/`。
2. 选择 **Build → Generate Signed App Bundle / APK**。
3. 选择 **APK**，Build Variant 选择 **`javaRelease`**。
4. 选择已有签名文件或创建签名后生成 APK。

`javaRelease` 同时包含 `armeabi-v7a` 和 `arm64-v8a`，适合大多数 ARM 电视和电视盒子。`java64Release` 只适合确认是 ARM64 的设备；`pythonRelease` 仅在需要 Python 爬虫时使用；正式发布不要选择 Debug 变体。

#### 命令行打包

```powershell
cd TVBoxOS-main
$env:JAVA_HOME = "C:\Users\<用户名>\.jdks\jbr-17.0.14"
.\gradlew assembleJavaRelease
```

生成文件位于：

`TVBoxOS-main/app/build/outputs/apk/java/release/TVBox_release-java.apk`

Google TV/Android TV 可能缓存旧的桌面图标，升级测试时建议先卸载旧版再安装新 APK。当前 TV 工程只打包 ARM 架构，不支持 x86/x86_64 模拟器。

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

TVBoxOS-main/                      # Android TV 独立 APK 工程
├── app/src/main/                   # TV 页面、播放器、品牌资源
├── player/                         # TVBox 播放器模块
└── quickjs/                        # JS 解析运行时
```

---

## 🎯 功能矩阵

| 功能 | 桌面端 | Android 移动端 | Android TV |
|------|:---:|:---:|:---:|
| 首页（豆瓣推荐 + Banner 轮播） | ✅ | ✅ | ✅ |
| 聚合搜索（SSE 流式 + 演员索引） | ✅ | ✅ | ✅ |
| 分类浏览（6类 + 子分类过滤 + 分页） | ✅ | ✅ | ✅ |
| 多源详情（站点选择 + 剧集列表） | ✅ | ✅ | ✅ |
| 视频播放（HLS/FLV/TS/MP4） | ✅ | ✅ | ✅ |
| 分享页自动解析 | ✅ | ✅ | ✅ |
| 播放器手势交互（亮度/音量/进度） | — | ✅ | — |
| 遥控器导航与 TV 控制栏 | — | — | ✅ |
| DLNA 投屏 | — | ✅ | ✅ |
| 画面尺寸 / 镜像反转 / 播放方式 | — | ✅ | ✅ |
| 收藏 + 观看历史 + 断点续播 | ✅ | ✅ | ✅ |
| 液态玻璃侧边栏 | ✅ | — | — |
| 系统托盘 + 关闭隐藏 | ✅ | — | — |
| 影院启动动画 | ✅ | ✅ | ✅ |

---

## 📡 API 接口

桌面端和移动端通过共享 Node.js 服务访问下方接口。Android TV 端不依赖这个 Node.js 服务，而是直接加载 TVBox 配置 JSON，由 APK 内置的 TVBox 网络层访问各资源站和解析服务。

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
