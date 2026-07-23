import { useLocation } from 'react-router-dom';
import { Home, Film, Tv, Clapperboard, Radio, Heart, Zap, Trophy, Clock, Coffee, type LucideIcon } from 'lucide-react';
import { APP_VERSION } from '../../utils/version';

interface MenuItem {
  icon: LucideIcon;
  label: string;
  path: string;
  /** 在该项前渲染分隔线（用于区分内容导航与功能入口） */
  divider?: boolean;
}

const menuItems: MenuItem[] = [
  { icon: Home, label: '首页', path: '/' },
  { icon: Film, label: '电影', path: '/movie' },
  { icon: Tv, label: '电视剧', path: '/tv' },
  { icon: Clapperboard, label: '综艺', path: '/variety' },
  { icon: Radio, label: '动漫', path: '/anime' },
  { icon: Zap, label: '短剧', path: '/short-drama' },
  { icon: Trophy, label: '体育', path: '/sports' },
  { icon: Heart, label: '收藏', path: '/favorites' },
  { icon: Clock, label: '历史', path: '/history' },
  { icon: Coffee, label: '捐赠', path: '/donate', divider: true },
];

interface Props { onNavigate: (path: string) => void; }

export default function Sidebar({ onNavigate }: Props) {
  const location = useLocation();
  const isActive = (path: string) => {
    if (path === '/') return location.pathname === '/';
    return location.pathname === path;
  };

  return (
    <aside className="w-60 bg-zinc-950/85 backdrop-blur-2xl border-r border-white/[0.04] flex flex-col shrink-0 relative overflow-hidden">

      {/* ====== 流体背景装饰 ====== */}
      <div className="absolute inset-0 pointer-events-none">
        {/* 上方暖光 */}
        <div className="absolute -top-20 -left-10 w-48 h-48 rounded-full bg-brand-500/[0.04] blur-3xl" />
        {/* 下方冷光 */}
        <div className="absolute -bottom-10 left-1/2 -translate-x-1/2 w-64 h-48 rounded-full bg-purple-500/[0.03] blur-3xl" />
      </div>

      {/* ====== Logo ====== */}
      <div
        className="relative h-16 flex items-center gap-3 px-5 cursor-pointer group border-b border-white/[0.04] overflow-hidden"
        onClick={() => onNavigate('/')}
      >
        {/* Logo 背景光晕 */}
        <div className="absolute inset-0 bg-gradient-to-r from-brand-500/[0.04] via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-500" />
        {/* Logo 图标 */}
        <div className="relative shrink-0">
          <div className="absolute inset-0 rounded-xl bg-brand-500/20 blur-md scale-90 opacity-0 group-hover:opacity-100 group-hover:scale-100 transition-all duration-500" />
          <img
            src="/icon.png"
            alt="RanNuan TV"
            className="relative h-11 w-11 rounded-xl object-cover ring-1 ring-white/[0.06] group-hover:ring-brand-500/40 transition-all duration-500 group-hover:shadow-lg group-hover:shadow-brand-500/10"
          />
        </div>
        {/* Logo 文字 */}
        <span className="relative text-lg font-bold tracking-wide text-white/80 group-hover:text-white transition-colors duration-300">
          RanNuan <span className="text-gradient-cyan">TV</span>
        </span>
      </div>

      {/* ====== 导航 ====== */}
      <nav className="relative flex-1 px-3 py-3 space-y-0.5 overflow-y-auto custom-scrollbar">
        {menuItems.map(({ icon: Icon, label, path, divider }, idx) => {
          const active = isActive(path);
          return (
            <div key={path} className="relative">
              {divider && <div className="my-2.5 mx-3 border-t border-white/[0.05]" />}
              <button
                onClick={() => onNavigate(path)}
                className={`group relative w-full flex items-center gap-3.5 px-3 py-3 rounded-2xl transition-all duration-500 ease-[cubic-bezier(0.4,0,0.2,1)] text-[15px]
                  ${active ? 'text-white font-semibold' : 'text-zinc-500 hover:text-zinc-200'}
                `}
                style={{ animationDelay: `${idx * 40}ms` }}
              >
                {/* 活跃光晕 */}
                {active && (
                  <>
                    {/* 背景玻璃卡片 */}
                    <span className="absolute inset-0 rounded-2xl bg-brand-500/[0.08] backdrop-blur-md border border-brand-500/[0.10] shadow-[0_0_20px] shadow-brand-500/[0.06]" />
                    {/* 左侧流光指示条 */}
                    <span className="absolute left-0 top-1/2 -translate-y-1/2 w-[3px] h-6 bg-brand-400 rounded-r-full shadow-[0_0_12px] shadow-brand-500/40 animate-slideIn" />
                  </>
                )}

                {/* 悬停玻璃效果 */}
                <span className="absolute inset-0 rounded-2xl bg-white/[0.02] backdrop-blur-sm border border-white/[0.02] opacity-0 group-hover:opacity-100 transition-all duration-300" />

                {/* 图标 */}
                <span className={`relative z-10 transition-all duration-500 ${
                  active ? 'scale-110' : 'group-hover:scale-110 group-hover:translate-x-0.5'
                }`}>
                  <Icon size={22} className={
                    active ? 'drop-shadow-[0_0_8px] drop-shadow-brand-500/40' : ''
                  } />
                </span>

                {/* 标签 */}
                <span className="relative z-10 tracking-[0.02em]">{label}</span>

                {/* 活跃脉冲圆点 */}
                {active && (
                  <span className="relative z-10 ml-auto flex items-center">
                    <span className="w-1.5 h-1.5 rounded-full bg-brand-400 shadow-[0_0_8px] shadow-brand-500/60" />
                    <span className="absolute w-2 h-2 rounded-full bg-brand-400/60 animate-ping" />
                  </span>
                )}
              </button>
            </div>
          );
        })}
      </nav>

      {/* ====== Footer ====== */}
      <div className="relative px-4 py-3 border-t border-white/[0.04]">
        <div className="absolute inset-0 bg-gradient-to-t from-brand-500/[0.02] to-transparent pointer-events-none" />
        <div className="relative flex items-center gap-2.5 text-[11px] text-zinc-600">
          <span className="flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-brand-500/40 shadow-[0_0_6px] shadow-brand-500/20" />
            <span className="tracking-wide">v{APP_VERSION}</span>
          </span>
          <span className="text-zinc-700">—</span>
          <span className="tracking-wider text-zinc-600">RanNuan TV</span>
        </div>
      </div>
    </aside>
  );
}
