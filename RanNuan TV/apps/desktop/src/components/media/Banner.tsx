import { useState, useEffect, useCallback } from 'react';
import type { MediaItem } from 'shared';
import { ChevronLeft, ChevronRight, Play, Info } from 'lucide-react';
import { stripHtml } from '../../utils/text';

interface Props {
  items: MediaItem[];
  onPlay: (item: MediaItem) => void;
  onDetail: (item: MediaItem) => void;
}

function proxyImg(rawUrl: string): string {
  if (!rawUrl) return '';
  if (rawUrl.includes('/api/img')) return rawUrl;
  if (rawUrl.includes('bfzy') || rawUrl.includes('picbf')) return `http://localhost:3000/api/img?url=${encodeURIComponent(rawUrl)}`;
  return rawUrl;
}

export default function Banner({ items, onPlay, onDetail }: Props) {
  const [current, setCurrent] = useState(0);
  const [imgLoaded, setImgLoaded] = useState(false);
  const [lazyPics, setLazyPics] = useState<Record<number, string>>({});

  const goNext = useCallback(() => {
    setImgLoaded(false);
    setCurrent((c) => (c + 1) % items.length);
  }, [current, items.length]);

  const goPrev = useCallback(() => {
    setImgLoaded(false);
    setCurrent((c) => (c - 1 + items.length) % items.length);
  }, [current, items.length]);

  useEffect(() => {
    if (items.length <= 1) return;
    const timer = setInterval(goNext, 5000);
    return () => clearInterval(timer);
  }, [goNext, items.length]);

  // 懒加载当前 Banner 项的图片
  useEffect(() => {
    const item = items[current];
    if (!item || item.vod_pic || lazyPics[current]) return;
    if (!item.site_key || !item.vod_id) return;
    
    let cancelled = false;
    fetch(`http://localhost:3000/api/thumbnail?site_key=${item.site_key}&id=${item.vod_id}`)
      .then(r => r.json())
      .then(data => {
        if (!cancelled && data.vod_pic) {
          setLazyPics(prev => ({ ...prev, [current]: data.vod_pic }));
        }
      })
      .catch(() => {});
    return () => { cancelled = true; };
  }, [current, items, lazyPics]);

  const item = items[current];
  if (!item) return null;

  const picUrl = item.vod_pic || lazyPics[current] || '';

  return (
    <div className="relative h-[460px] rounded-3xl overflow-hidden group">
      {/* 流体渐变底层 */}
      <div className="absolute inset-0 fluid-gradient-bg" />

      {/* 动态光球装饰 */}
      <div className="absolute inset-0 overflow-hidden">
        <div className="absolute -top-32 -right-32 w-96 h-96 rounded-full bg-cyan-500/10 blur-3xl animate-pulse" style={{ animationDuration: '8s' }} />
        <div className="absolute -bottom-20 -left-20 w-80 h-80 rounded-full bg-violet-500/8 blur-3xl" style={{ animation: 'pulseGlow 6s ease-in-out infinite', animationDelay: '3s' }} />
      </div>

      {/* 背景海报 */}
      <div className="absolute inset-0">
        {picUrl && (
          <img
            src={proxyImg(picUrl)}
            alt=""
            onLoad={() => setImgLoaded(true)}
            className={`absolute inset-0 w-full h-full object-cover scale-110 transition-all duration-1500 ease-out ${
              imgLoaded ? 'opacity-30 blur-2xl' : 'opacity-0'
            }`}
          />
        )}
      </div>

      {/* 多层渐变遮罩 */}
      <div className="absolute inset-0 bg-gradient-to-r from-zinc-950 via-zinc-950/90 via-50% to-transparent" />
      <div className="absolute inset-0 bg-gradient-to-t from-zinc-950/80 via-transparent to-transparent" />

      {/* 主内容 */}
      <div className="relative z-10 h-full flex items-center px-8 md:px-16">
        <div className="max-w-xl">
          {/* 类型标签 */}
          <div className="flex items-center gap-2 mb-4">
            {item.type_name && (
              <span className="px-3 py-1 text-xs font-semibold rounded-full glass border-cyan-500/30 text-cyan-300 tracking-wide">
                {item.type_name}
              </span>
            )}
            {item.vod_remarks && (
              <span className="px-3 py-1 text-xs rounded-full bg-white/5 text-white/50 border border-white/10">
                {item.vod_remarks}
              </span>
            )}
          </div>

          {/* 标题 */}
          <h2
            className={`text-4xl md:text-5xl lg:text-6xl font-bold mb-4 line-clamp-2 tracking-tight transition-all duration-1000 ease-out ${
              imgLoaded || !picUrl ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-6'
            }`}
            style={{
              background: 'linear-gradient(180deg, #ffffff 0%, rgba(255,255,255,0.85) 100%)',
              WebkitBackgroundClip: 'text',
              WebkitTextFillColor: 'transparent',
            }}
          >
            {item.vod_name}
          </h2>

          {/* 信息行 */}
          {item.vod_year && (
            <p className="text-sm text-white/40 mb-3 tracking-wide">
              {item.vod_year}
              {item.vod_area && ` · ${item.vod_area}`}
              {item.vod_actor && ` · ${item.vod_actor.slice(0, 30)}`}
            </p>
          )}

          {/* 简介 */}
          {item.vod_content && (
            <p className="text-sm text-white/30 mb-8 line-clamp-2 max-w-lg leading-relaxed">
              {stripHtml(item.vod_content)}
            </p>
          )}

          {/* 按钮组 */}
          <div className="flex items-center gap-3">
            <button
              onClick={() => onPlay(item)}
              className="flex items-center gap-2.5 px-8 py-3.5 bg-cyan-500 hover:bg-cyan-400 rounded-2xl font-semibold text-sm text-black transition-all duration-300 hover:scale-[1.03] hover:shadow-xl hover:shadow-cyan-500/30 active:scale-95"
            >
              <Play size={18} className="fill-black" />
              立即播放
            </button>
            <button
              onClick={() => onDetail(item)}
              className="flex items-center gap-2.5 px-7 py-3.5 rounded-2xl font-medium text-sm text-white/70 hover:text-white transition-all duration-300 hover:bg-white/5 glass active:scale-95"
            >
              <Info size={18} />
              详情
            </button>
          </div>
        </div>
      </div>

      {/* 右侧海报卡片（大屏） */}
      {picUrl && (
        <div className={`absolute right-[6%] top-[10%] bottom-[10%] w-[260px] xl:w-[300px] hidden lg:block transition-all duration-1000 ease-out ${
          imgLoaded ? 'opacity-100 translate-x-0 rotate-0' : 'opacity-0 translate-x-12 rotate-2'
        }`}>
          <div className="relative w-full h-full rounded-2xl overflow-hidden glass-glow">
            <img
              src={proxyImg(picUrl)}
              alt={item.vod_name}
              className="w-full h-full object-cover"
            />
            <div className="absolute inset-0 ring-1 ring-inset ring-white/10 rounded-2xl pointer-events-none" />
          </div>
        </div>
      )}

      {/* 箭头 */}
      {items.length > 1 && (
        <>
          <button
            onClick={goPrev}
            className="absolute left-4 top-1/2 -translate-y-1/2 z-20 p-3.5 rounded-full glass hover:bg-white/10 text-white/60 hover:text-white opacity-0 group-hover:opacity-100 transition-all duration-300"
          >
            <ChevronLeft size={22} />
          </button>
          <button
            onClick={goNext}
            className="absolute right-4 top-1/2 -translate-y-1/2 z-20 p-3.5 rounded-full glass hover:bg-white/10 text-white/60 hover:text-white opacity-0 group-hover:opacity-100 transition-all duration-300"
          >
            <ChevronRight size={22} />
          </button>
        </>
      )}

      {/* 底部指示器 */}
      {items.length > 1 && (
        <div className="absolute bottom-6 left-1/2 -translate-x-1/2 z-20 flex gap-2.5">
          {items.map((_, i) => (
            <button
              key={i}
              onClick={() => {
                setImgLoaded(false);
                setCurrent(i);
              }}
              className={`rounded-full transition-all duration-500 ease-out ${
                i === current
                  ? 'bg-cyan-400 w-8 h-2'
                  : 'bg-white/20 hover:bg-white/35 w-2 h-2'
              }`}
            />
          ))}
        </div>
      )}
    </div>
  );
}
