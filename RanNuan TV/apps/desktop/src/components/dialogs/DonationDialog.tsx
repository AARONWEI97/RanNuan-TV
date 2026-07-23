import { useEffect, useState } from 'react';
import { Heart, Coffee, X } from 'lucide-react';

interface Props {
  /** 关闭弹窗（下次再说） */
  onClose: () => void;
  /** 前往捐赠页 */
  onGoDonate: () => void;
}

/**
 * 捐赠弹窗 —— 免责声明确认后弹出。
 * 温暖色调点缀，轻量展示，详细二维码见捐赠页。
 */
export default function DonationDialog({ onClose, onGoDonate }: Props) {
  const [mounted, setMounted] = useState(false);
  useEffect(() => {
    const t = setTimeout(() => setMounted(true), 10);
    // ESC 关闭
    const onKey = (e: KeyboardEvent) => { if (e.key === 'Escape') onClose(); };
    window.addEventListener('keydown', onKey);
    return () => { clearTimeout(t); window.removeEventListener('keydown', onKey); };
  }, [onClose]);

  return (
    <div className="fixed inset-0 z-[10000] flex items-center justify-center p-6">
      {/* 遮罩 */}
      <div
        className="absolute inset-0 bg-black/75 backdrop-blur-md animate-[fadeIn_0.3s_ease]"
        onClick={onClose}
      />

      {/* 卡片 */}
      <div
        className={`relative w-full max-w-md rounded-3xl border border-white/10 bg-zinc-950/95 shadow-2xl overflow-hidden transition-all duration-400 ${
          mounted ? 'opacity-100 translate-y-0 scale-100' : 'opacity-0 translate-y-6 scale-[0.97]'
        }`}
      >
        {/* 顶部温暖渐变 */}
        <div className="relative h-32 bg-gradient-to-br from-rose-500/25 via-amber-500/20 to-brand-500/15 flex items-center justify-center overflow-hidden">
          <div className="absolute inset-0 pointer-events-none">
            <div className="absolute -top-8 -left-4 w-32 h-32 rounded-full bg-rose-400/20 blur-2xl animate-pulse" />
            <div className="absolute -bottom-10 right-0 w-28 h-28 rounded-full bg-amber-400/20 blur-2xl animate-pulse" style={{ animationDelay: '0.6s' }} />
          </div>
          {/* 跳动爱心 */}
          <div className="relative">
            <div className="absolute inset-0 rounded-full bg-rose-500/30 blur-xl scale-150 animate-pulse" />
            <div className="relative w-16 h-16 rounded-2xl bg-zinc-950/60 backdrop-blur-md border border-white/15 flex items-center justify-center shadow-xl">
              <Heart size={30} className="text-rose-400 fill-rose-400/80" />
            </div>
          </div>
          {/* 关闭按钮 */}
          <button
            onClick={onClose}
            className="absolute top-3 right-3 w-8 h-8 rounded-full bg-black/30 hover:bg-black/50 backdrop-blur-md flex items-center justify-center text-zinc-400 hover:text-white transition-all"
          >
            <X size={16} />
          </button>
        </div>

        {/* 内容 */}
        <div className="px-7 py-6 text-center">
          <h2 className="text-xl font-bold text-white tracking-wide">支持一下开发者 ☕</h2>
          <p className="mt-3 text-[13.5px] leading-7 text-zinc-400">
            冉暖TV 永久免费、无广告、无内购。
            <br />
            如果你用得还顺手，欢迎请作者喝杯咖啡，
            <br />
            你的支持是我们持续更新的动力 ❤️
          </p>

          {/* 行动按钮 */}
          <div className="mt-6 flex items-center gap-3">
            <button
              onClick={onClose}
              className="flex-1 py-2.5 rounded-xl text-sm text-zinc-400 hover:text-zinc-200 hover:bg-white/[0.04] border border-white/[0.06] transition-all"
            >
              下次再说
            </button>
            <button
              onClick={onGoDonate}
              className="group flex-[1.4] flex items-center justify-center gap-2 py-2.5 rounded-xl text-sm font-semibold text-white bg-gradient-to-r from-rose-500 to-amber-500 hover:from-rose-400 hover:to-amber-400 shadow-lg shadow-rose-500/25 transition-all"
            >
              <Coffee size={16} className="group-hover:scale-110 transition-transform" />
              去支持开发者
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
