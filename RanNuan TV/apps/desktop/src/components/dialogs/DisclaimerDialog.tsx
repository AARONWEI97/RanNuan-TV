import { useEffect, useState } from 'react';
import { ShieldAlert, Check } from 'lucide-react';

interface Props {
  /** 用户点击「我已阅读并同意」 */
  onAgree: () => void;
  /** 用户点击「退出应用」（Tauri 关闭窗口 / 网页隐藏） */
  onExit: () => void;
}

/**
 * 免责声明弹窗 —— 首次使用 / 版本更新后弹出。
 * 不可点遮罩或按 ESC 关闭，必须明确「同意」或「退出」。
 */
export default function DisclaimerDialog({ onAgree, onExit }: Props) {
  const [mounted, setMounted] = useState(false);
  useEffect(() => {
    const t = setTimeout(() => setMounted(true), 10);
    return () => clearTimeout(t);
  }, []);

  return (
    <div className="fixed inset-0 z-[10000] flex items-center justify-center p-6">
      {/* 遮罩 */}
      <div className="absolute inset-0 bg-black/75 backdrop-blur-md animate-[fadeIn_0.3s_ease]" />

      {/* 卡片 */}
      <div
        className={`relative w-full max-w-2xl max-h-[88vh] flex flex-col rounded-3xl border border-white/10 bg-zinc-950/95 shadow-2xl overflow-hidden transition-all duration-400 ${
          mounted ? 'opacity-100 translate-y-0 scale-100' : 'opacity-0 translate-y-6 scale-[0.97]'
        }`}
      >
        {/* 顶部装饰光晕 */}
        <div className="absolute -top-24 left-1/2 -translate-x-1/2 w-72 h-48 rounded-full bg-amber-500/15 blur-3xl pointer-events-none" />
        <div className="absolute -bottom-20 -right-10 w-60 h-60 rounded-full bg-brand-500/10 blur-3xl pointer-events-none" />

        {/* 头部 */}
        <div className="relative flex items-center gap-3 px-7 pt-7 pb-5 border-b border-white/[0.06]">
          <div className="relative shrink-0">
            <div className="absolute inset-0 rounded-2xl bg-amber-500/30 blur-md" />
            <div className="relative w-11 h-11 rounded-2xl bg-amber-500/15 border border-amber-500/30 flex items-center justify-center">
              <ShieldAlert size={22} className="text-amber-400" />
            </div>
          </div>
          <div>
            <h2 className="text-lg font-bold text-white tracking-wide">免责声明</h2>
            <p className="text-xs text-zinc-500 mt-0.5">首次使用 / 更新后展示，请仔细阅读</p>
          </div>
        </div>

        {/* 内容（可滚动） */}
        <div className="relative flex-1 overflow-y-auto custom-scrollbar px-7 py-5">
          <div className="text-[13.5px] leading-7 text-zinc-300 space-y-3.5">
            <p>
              <span className="text-brand-400 font-semibold">冉暖TV</span>{' '}
              是一款基于第三方公开影视资源接口的视频聚合播放工具，仅供个人学习、研究和技术交流使用。
            </p>
            <div className="space-y-2.5">
              <Clause
                num="1"
                text="本应用不存储、不上传、不分发任何视频、音频、图片等媒体内容，所有影视资源均来自第三方公开接口（CMS 资源站），本应用不对任何第三方内容的合法性、准确性、完整性负责。"
              />
              <Clause
                num="2"
                text="本应用仅提供资源链接的聚合与播放功能，不参与任何资源的采集、存储与传播。所有内容的版权归原作者或原平台所有。"
              />
              <Clause
                num="3"
                text="用户在使用本应用时，应遵守所在国家 / 地区的法律法规，不得将本应用用于任何商业用途或违法违规用途。因用户使用不当产生的一切后果由用户自行承担。"
              />
              <Clause
                num="4"
                text="若本应用的内容侵犯了您的合法权益，请提供相关权属证明联系开发者，我们将在确认后及时处理与删除。"
              />
            </div>
            <p className="pt-1 text-zinc-400">
              继续使用本应用即表示您已阅读、理解并同意以上全部内容。
            </p>
          </div>
        </div>

        {/* 底部操作 */}
        <div className="relative flex items-center justify-end gap-3 px-7 py-5 border-t border-white/[0.06] bg-white/[0.015]">
          <button
            onClick={onExit}
            className="px-5 py-2.5 rounded-xl text-sm text-zinc-400 hover:text-zinc-200 hover:bg-white/[0.04] transition-all"
          >
            退出应用
          </button>
          <button
            onClick={onAgree}
            className="group flex items-center gap-2 px-6 py-2.5 rounded-xl text-sm font-semibold text-white bg-gradient-to-r from-brand-500 to-brand-600 hover:from-brand-400 hover:to-brand-500 shadow-lg shadow-brand-500/25 transition-all"
          >
            <Check size={16} className="group-hover:scale-110 transition-transform" />
            我已阅读并同意
          </button>
        </div>
      </div>
    </div>
  );
}

function Clause({ num, text }: { num: string; text: string }) {
  return (
    <div className="flex gap-2.5">
      <span className="shrink-0 mt-0.5 w-5 h-5 rounded-full bg-amber-500/10 border border-amber-500/20 flex items-center justify-center text-[11px] font-bold text-amber-400">
        {num}
      </span>
      <span className="text-zinc-300">{text}</span>
    </div>
  );
}
