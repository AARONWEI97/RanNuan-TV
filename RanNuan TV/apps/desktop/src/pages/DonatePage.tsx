import { useState } from 'react';
import { Heart, Sparkles, ExternalLink, QrCode as QrIcon } from 'lucide-react';
import { openExternal, APP_VERSION } from '../utils/version';

/** 二维码卡片配置 */
const channels = [
  {
    key: 'wechat',
    name: '微信赞赏',
    desc: '扫描二维码',
    img: '/donate-wechat.png',
    accent: 'from-emerald-500/20 to-green-500/10',
    ring: 'ring-emerald-500/30',
    badge: 'bg-emerald-500/15 text-emerald-300 border-emerald-500/30',
    dot: 'bg-emerald-400',
  },
  {
    key: 'alipay',
    name: '支付宝',
    desc: '扫描二维码',
    img: '/donate-alipay.png',
    accent: 'from-sky-500/20 to-blue-500/10',
    ring: 'ring-sky-500/30',
    badge: 'bg-sky-500/15 text-sky-300 border-sky-500/30',
    dot: 'bg-sky-400',
  },
];

/** 外部捐赠平台（可选，留空则不显示） */
const externalLink = ''; // 例如：'https://afdian.net/xxx'

/** 伪二维码图案（确定性，仅用于真实图片缺失时的占位展示） */
const PLACEHOLDER_CELLS: boolean[] = (() => {
  const arr: boolean[] = [];
  let seed = 7;
  for (let i = 0; i < 121; i++) {
    seed = (seed * 1103515245 + 12345) & 0x7fffffff;
    arr.push((seed >> 16) % 100 < 48);
  }
  return arr;
})();

export default function DonatePage() {
  return (
    <div className="relative min-h-full">
      {/* ====== 背景流体光晕 ====== */}
      <div className="fixed inset-0 pointer-events-none overflow-hidden">
        <div className="absolute -top-32 left-1/4 w-96 h-96 rounded-full bg-rose-500/[0.07] blur-3xl" />
        <div className="absolute top-1/3 -right-20 w-80 h-80 rounded-full bg-amber-500/[0.06] blur-3xl" />
        <div className="absolute bottom-0 left-1/3 w-72 h-72 rounded-full bg-brand-500/[0.05] blur-3xl" />
      </div>

      <div className="relative max-w-3xl mx-auto pb-16">
        {/* ====== Hero ====== */}
        <section className="pt-2 pb-10 text-center animate-reveal">
          <div className="relative inline-flex mb-5">
            <div className="absolute inset-0 rounded-full bg-rose-500/25 blur-2xl scale-150 animate-pulse" />
            <div className="relative w-20 h-20 rounded-3xl overflow-hidden border border-white/10 shadow-2xl ring-1 ring-white/10">
              <img src="/icon.png" alt="冉暖TV" className="w-full h-full object-cover" />
            </div>
          </div>
          <h1 className="text-3xl font-black tracking-wide text-white">
            请作者喝杯<span className="bg-gradient-to-r from-amber-300 via-rose-400 to-amber-400 bg-clip-text text-transparent">咖啡</span> ☕
          </h1>
          <p className="mt-3 text-sm text-zinc-400 leading-7 max-w-lg mx-auto">
            冉暖TV 是一款完全免费、无广告、无内购的影视聚合工具。
            <br />
            如果它让你的生活多了一点便利，欢迎支持开发者继续前行。
          </p>
        </section>

        {/* ====== 二维码卡片 ====== */}
        <section className="grid grid-cols-1 sm:grid-cols-2 gap-5 animate-reveal animate-reveal-delay-1">
          {channels.map(({ key, ...c }) => (
            <QrCard key={key} {...c} />
          ))}
        </section>

        {/* ====== 外部平台入口（可选） ====== */}
        {externalLink && (
          <section className="mt-5 animate-reveal animate-reveal-delay-2">
            <button
              onClick={() => openExternal(externalLink)}
              className="group w-full flex items-center justify-between gap-4 p-5 rounded-2xl glass-card text-left"
            >
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-brand-500/15 border border-brand-500/30 flex items-center justify-center">
                  <Sparkles size={20} className="text-brand-300" />
                </div>
                <div>
                  <p className="text-sm font-semibold text-white">爱发电 · 持续赞助</p>
                  <p className="text-xs text-zinc-500 mt-0.5">按月支持，解锁更多可能</p>
                </div>
              </div>
              <ExternalLink size={18} className="text-zinc-500 group-hover:text-brand-300 group-hover:translate-x-0.5 transition-all" />
            </button>
          </section>
        )}

        {/* ====== 为什么捐赠 ====== */}
        <section className="mt-8 p-6 rounded-3xl glass animate-reveal animate-reveal-delay-2">
          <div className="flex items-center gap-2.5 mb-4">
            <Heart size={18} className="text-rose-400" />
            <h2 className="text-base font-bold text-white">为什么需要你的支持</h2>
          </div>
          <ul className="space-y-3 text-[13.5px] text-zinc-400 leading-6">
            {[
              ['永久免费', '不设会员、不加广告、不卖数据，所有人都能完整使用全部功能。'],
              ['服务器与带宽', '聚合接口、图片代理、视频代理均运行在开发者自费的服务器上。'],
              ['持续维护', '资源站接口时常变动，需要持续跟进修复，这是一项长期工作。'],
              ['独立开发', '一个人在设计、前端、后端、打包全栈维护，你的鼓励是最大的动力。'],
            ].map(([title, desc]) => (
              <li key={title} className="flex gap-3">
                <span className="shrink-0 mt-1.5 w-1.5 h-1.5 rounded-full bg-rose-400/70" />
                <span>
                  <span className="text-zinc-200 font-medium">{title}</span>
                  {'  —  '}
                  {desc}
                </span>
              </li>
            ))}
          </ul>
        </section>

        {/* ====== 开发者寄语 ====== */}
        <section className="mt-6 text-center animate-reveal animate-reveal-delay-3">
          <p className="text-sm text-zinc-500 leading-7 italic">
            「做一款自己每天都想用的播放器，顺便分享给同样爱看电影的你。」
          </p>
          <div className="mt-4 inline-flex items-center gap-2 text-xs text-zinc-600">
            <span className="w-1.5 h-1.5 rounded-full bg-brand-500/50" />
            <span>无论是否捐赠，感谢你的使用与反馈</span>
            <span className="text-zinc-700">·</span>
            <span>v{APP_VERSION}</span>
          </div>
        </section>
      </div>
    </div>
  );
}

/** 二维码卡片：优先加载真实图片，失败回退占位 */
function QrCard({
  name, desc, img, accent, ring, badge, dot,
}: {
  name: string; desc: string; img: string;
  accent: string; ring: string; badge: string; dot: string;
}) {
  const [failed, setFailed] = useState(false);

  return (
    <div className="group relative p-5 rounded-3xl glass-card overflow-hidden">
      {/* 顶部渐变装饰 */}
      <div className={`absolute -top-12 -right-12 w-40 h-40 rounded-full bg-gradient-to-br ${accent} blur-2xl opacity-60 group-hover:opacity-90 transition-opacity`} />

      <div className="relative flex items-center gap-2.5 mb-4">
        <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium border ${badge}`}>
          <span className={`w-1.5 h-1.5 rounded-full ${dot}`} />
          {name}
        </span>
      </div>

      {/* 二维码区域 */}
      <div className={`relative mx-auto w-44 h-44 rounded-2xl bg-white p-2.5 ring-1 ${ring} shadow-lg`}>
        {failed ? (
          <Placeholder />
        ) : (
          <img
            src={img}
            alt={`${name}二维码`}
            className="w-full h-full object-contain rounded-lg"
            onError={() => setFailed(true)}
          />
        )}
      </div>

      <p className="relative mt-4 text-center text-xs text-zinc-500">{desc}</p>
    </div>
  );
}

/** 二维码占位：当真实图片不存在时显示，提示开发者替换 */
function Placeholder() {
  return (
    <div className="relative w-full h-full rounded-lg bg-zinc-100 flex items-center justify-center overflow-hidden">
      {/* 伪二维码图案 */}
      <div
        className="grid gap-[2px] p-2"
        style={{ gridTemplateColumns: 'repeat(11, 1fr)', width: '100%', height: '100%' }}
      >
        {PLACEHOLDER_CELLS.map((on, i) => (
          <div key={i} className={on ? 'bg-zinc-900' : 'bg-transparent'} />
        ))}
      </div>
      {/* 占位提示 */}
      <div className="absolute inset-0 flex flex-col items-center justify-center bg-white/80 backdrop-blur-[1px]">
        <QrIcon size={28} className="text-zinc-400" />
        <span className="mt-1.5 text-[10px] text-zinc-400 text-center px-2 leading-tight">
          二维码占位
          <br />
          请替换为真实图片
        </span>
      </div>
    </div>
  );
}
