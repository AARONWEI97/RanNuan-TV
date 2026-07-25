# RanNuan TV — 生产环境部署指南

> 服务器：阿里云 Ubuntu 22.04，IP: `47.108.80.234`
> 进程管理：PM2（完整路径 `/usr/local/node18/bin/pm2`）
> 项目路径：`/root/server/`
> 本机项目根目录：`c:\Users\AaronWei\Desktop\dongguaTV-main\RanNuan TV`

---

## 一、版本号管理（唯一真相源）

版本号统一在 `apps/mobile-android/app/build.gradle.kts` 第 14-15 行维护：

```kotlin
versionCode = 6          // 必须递增（每次发布 +1）
versionName = "2.1.3"    // 显示名称
```

构建 Release APK 时会自动触发 `syncLatestJson` Task，将版本号同步到 `server/public/dataupdate/latest.json`。

**规则：只需改上面两行，其余自动跟进。**

---

## 二、发布步骤（共 7 步）

### 步骤 1：改版本号

打开 `apps/mobile-android/app/build.gradle.kts`，递增 `versionCode`，更新 `versionName`。

### 步骤 2：打包 Release APK

1. Android Studio → **Build → Generate Signed Bundle / APK → APK**
2. Key store path：选 `rannuan-release.jks`
3. Key store password / Key alias / Key password：输入创建时的密码
4. 勾选 **V1 (Jar Signature)** + **V2 (Full APK Signature)**
5. `Build Variants` 选 `release`，点 **Finish**
6. 输出路径：`apps/mobile-android/app/release/`

### 步骤 3：查找 APK 文件

打包完成后，在**本机 Windows CMD** 中执行：

```cmd
dir "c:\Users\AaronWei\Desktop\dongguaTV-main\RanNuan TV\apps\mobile-android\app\release\RanNuanTV-v2.1.4.apk"
```

记录输出路径（如 `RanNuan-TV-v2.1.4.apk`），后续命令需要用到。

### 步骤 4：计算 SHA-256 和文件大小

把上一步的 APK 路径填入下面两条命令中的 `{APK路径}`，在**本机 Windows CMD** 中执行：

```cmd
certutil -hashfile "c:\Users\AaronWei\Desktop\dongguaTV-main\RanNuan TV\apps\mobile-android\app\release\RanNuanTV-v2.1.5.apk" SHA256
```

```cmd
for %i in ("c:\Users\AaronWei\Desktop\dongguaTV-main\RanNuan TV\apps\mobile-android\app\release\RanNuanTV-v2.1.5.apk") do @echo %~zi
```

**示例：**

```cmd
certutil -hashfile "c:\Users\AaronWei\Desktop\dongguaTV-main\RanNuan TV\apps\mobile-android\app\release\RanNuan-TV-v2.1.4.apk" SHA256
```

```cmd
for %i in ("c:\Users\AaronWei\Desktop\dongguaTV-main\RanNuan TV\apps\mobile-android\app\release\RanNuan-TV-v2.1.4.apk") do @echo %~zi
```

记录输出：
- SHA-256：一长串十六进制字符串（如 `f6fe9c067508474e74f87d91a37b02b8595e5286040a5f660c5da63a4ba0afe1`）
- 文件大小：纯数字（如 `8242231`），单位是**字节**

> **为什么每次都要算？** 只要 APK 内容有变化（即使代码没改，重新编译也有不同的时间戳和签名元数据），SHA-256 就会不同。所以每次打包后必须重新计算，不能复用旧值。

### 步骤 5：更新 `latest.json`

打开 `server/public/dataupdate/latest.json`，填入步骤 4 得到的 SHA-256 和文件大小：

```json
{
  "enabled": true,
  "versionCode": 6,
  "versionName": "2.1.3",
  "downloadUrl": "/dataupdate/apk/RanNuan-TV-v2.1.3.apk",
  "sizeBytes": 8242231,
  "sha256": "f6fe9c067508474e74f87d91a37b02b8595e5286040a5f660c5da63a4ba0afe1",
  "forceUpdate": false,
  "minSupportedVersionCode": 1,
  "publishedAt": "2026-07-24",
  "changelog": [
    "更新内容第一项",
    "更新内容第二项"
  ]
}
```

| 字段 | 说明 |
|------|------|
| `versionCode` | 与 `build.gradle.kts` 一致 |
| `versionName` | 与 `build.gradle.kts` 一致 |
| `downloadUrl` | APK 在服务器上的相对路径，格式 `/dataupdate/apk/RanNuan-TV-v{版本}.apk` |
| `sizeBytes` | 步骤 4 得到的文件大小 |
| `sha256` | 步骤 4 得到的 SHA-256（**小写**） |
| `forceUpdate` | `true` = 强制更新（用户不可关闭弹窗）；`false` = 可选更新 |
| `changelog` | 手动编写更新文案 |

### 步骤 6：上传到服务器

在**本机 Windows CMD** 中**依次**执行（每条跑完再跑下一条），`{版本}` 替换为当前版本号如 `2.1.3`：

**6.1 上传后端代码（如果 server.js 有改动）：**

```cmd
scp "c:\Users\AaronWei\Desktop\dongguaTV-main\RanNuan TV\server\server.js" root@47.108.80.234:/root/server/server.js
```

**6.2 上传版本信息：**

```cmd
scp "c:\Users\AaronWei\Desktop\dongguaTV-main\RanNuan TV\server\public\dataupdate\latest.json" root@47.108.80.234:/root/server/public/dataupdate/latest.json
```

**6.3 上传 APK 安装包（{APK本地路径} 替换为步骤 3 得到的实际路径，{版本} 替换为版本号如 2.1.3）：**

```cmd
scp "{APK本地路径}" root@47.108.80.234:/root/server/public/dataupdate/apk/RanNuan-TV-v{版本}.apk
```

**示例：**

```cmd
scp "c:\Users\AaronWei\Desktop\dongguaTV-main\RanNuan TV\apps\mobile-android\app\release\RanNuan-TV-v2.1.3.apk" root@47.108.80.234:/root/server/public/dataupdate/apk/RanNuan-TV-v2.1.3.apk
```

> **避免踩坑：APK 文件名不要有空格**。如果 SCP 报 `No such file or directory`，先检查文件名是否有空格，去掉再试。

### 步骤 7：重启服务并验证

在**本机 Windows CMD** 中执行：

**7.1 重启 PM2 进程：**

```cmd
ssh root@47.108.80.234 "/usr/local/node18/bin/pm2 restart rannuan-api"
```

**7.2 查看进程状态：**

```cmd
ssh root@47.108.80.234 "/usr/local/node18/bin/pm2 status"
```

**7.3 验证 latest.json 可访问且内容正确：**

```cmd
ssh root@47.108.80.234 "curl -s http://localhost:3000/dataupdate/latest.json"
```

确认返回的 `versionCode`、`sizeBytes`、`sha256` 无误。

---

## 三、常用命令速查

### 本机 Windows CMD

```cmd
:: 查找 APK 文件
dir "c:\Users\AaronWei\Desktop\dongguaTV-main\RanNuan TV\apps\mobile-android\app\release\*.apk"

:: 计算 SHA-256（替换 {APK路径}）
certutil -hashfile "{APK路径}" SHA256

:: 计算文件大小（字节，替换 {APK路径}）
for %i in ("{APK路径}") do @echo %~zi
```

### 服务器管理

```cmd
:: 查看 PM2 状态
ssh root@47.108.80.234 "/usr/local/node18/bin/pm2 status"

:: 查看后端日志（最近 50 行）
ssh root@47.108.80.234 "/usr/local/node18/bin/pm2 logs rannuan-api --lines 50"

:: 重启服务
ssh root@47.108.80.234 "/usr/local/node18/bin/pm2 restart rannuan-api"

:: 验证 latest.json
ssh root@47.108.80.234 "curl -s http://localhost:3000/dataupdate/latest.json"

:: SSH 登录服务器
ssh root@47.108.80.234
```

### 服务器内操作（SSH 登录后执行）

```bash
# 清理旧版本 APK（释放磁盘空间）
ls -la /root/server/public/dataupdate/apk/
rm /root/server/public/dataupdate/apk/RanNuan-TV-v2.1.0.apk
rm /root/server/public/dataupdate/apk/RanNuan-TV-v2.1.1.apk
```

---

## 四、踩坑记录

| # | 问题 | 根因 | 解决 |
|---|------|------|------|
| 1 | SSH 非交互模式 `pm2: command not found` | 非交互 SSH 不加载用户 PATH | 用完整路径 `/usr/local/node18/bin/pm2` |
| 2 | Release APK 启动闪退 `Class cannot be cast to ParameterizedType` | R8 混淆删了 Gson 泛型信息 | `proguard-rules.pro` 添加 `-keep class java.lang.reflect.**` 和 `-keep class com.google.gson.reflect.TypeToken` |
| 3 | SCP 报 `No such file or directory` | APK 文件名含空格，SCP 路径解析错误 | 文件名去掉所有空格 |
| 4 | 覆盖安装失败「应用未安装」 | 签名密钥与旧版不一致，或 `versionCode` 未递增 | 始终用同一个 `.jks` 签名；每次 `versionCode += 1` |
| 5 | Android Studio 创建 JKS 报 `Tag number over 30` | GUI 创建密钥库的 JDK bug | 改用命令行 `keytool -genkey -v -keystore xxx.jks -keyalg RSA -keysize 2048 -validity 9125 -alias rannuan -storetype JKS` |
| 6 | 第一次连接 SSH 提示 `authenticity can't be established` | 新 IP 未加入 known_hosts | 输入 `yes` 回车即可 |
| 7 | 在服务器 SSH 里执行了本机该跑的命令 | 搞混了终端窗口 | **所有 `scp`/`certutil`/`for` 命令在本地 CMD 跑，只有服务器管理命令用 SSH** |

---

> 📅 最后更新：2026-07-24
> 📝 v2.1
