/**
 * 应用版本号 —— 唯一数据源为 package.json 的 version 字段。
 * 直接 import package.json，Vite 会将其作为模块依赖监听：
 *   - dev 模式：改 package.json 自动 HMR，无需重启 dev server
 *   - build 模式：构建时打包，正确注入
 *
 * 发版时只需修改 package.json 的 version，以下引用处自动同步：
 *   - 启动弹窗触发判断（首次使用 / 版本更新）
 *   - Sidebar 底部版本显示
 *   - 捐赠页版本显示
 *
 * 用于控制「免责声明 + 捐赠」启动弹窗的触发：
 *   - 首次使用：localStorage 无记录 → 弹窗
 *   - 版本更新：记录版本与当前版本不一致 → 弹窗
 *   - 同版本再次进入：相等 → 不弹
 */
import { version } from '../../package.json';

export const APP_VERSION = version;
export const PROJECT_GITHUB_URL = 'https://github.com/AARONWEI97/RanNuan-TV';

const NOTICE_VERSION_KEY = 'rannuan-notice-version';

/**
 * 是否需要展示首启动/更新弹窗（免责声明 + 捐赠）。
 */
export function shouldShowStartupNotice(): boolean {
  try {
    const acknowledged = localStorage.getItem(NOTICE_VERSION_KEY);
    return acknowledged !== APP_VERSION;
  } catch {
    // localStorage 不可用时，保守起见每次都展示
    return true;
  }
}

/**
 * 用户已确认当前版本的所有启动弹窗，写入版本号。
 */
export function acknowledgeStartupNotice(): void {
  try {
    localStorage.setItem(NOTICE_VERSION_KEY, APP_VERSION);
  } catch {
    /* 忽略写入失败 */
  }
}

/**
 * 打开外部链接。
 * Tauri 环境使用 shell plugin 的 open（已配置 shell:allow-open 权限），
 * 网页开发环境回退到 window.open。
 */
export async function openExternal(url: string): Promise<void> {
  try {
    const { open } = await import('@tauri-apps/plugin-shell');
    await open(url);
  } catch {
    window.open(url, '_blank', 'noopener,noreferrer');
  }
}
