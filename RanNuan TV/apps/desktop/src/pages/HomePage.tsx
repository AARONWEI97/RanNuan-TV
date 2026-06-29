import { useState, useEffect, useMemo, useCallback, useRef, type ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
import { getCategory, getDoubanHome, searchMedia } from 'shared';
import type { MediaItem, DoubanSubject, DoubanHomeData } from 'shared';
import Banner from '../components/media/Banner';
import { proxyImg } from '../utils/imageProxy';
import { detailLink } from '../utils/navigate';

const sections: { key: keyof DoubanHomeData; label: string; icon: string; path: string; count: number }[] = [
  { key: 'dianshiju', label: '电视剧', icon: '📺', path: '/tv', count: 12 },
  { key: 'dianying',  label: '电影',   icon: '🎬', path: '/movie', count: 12 },
  { key: 'zongyi',    label: '综艺',   icon: '🎪', path: '/variety', count: 12 },
  { key: 'dongman',   label: '动漫',   icon: '🐱', path: '/anime', count: 12 },
];

export default function HomePage() {
  const [doubanData, setDoubanData] = useState<DoubanHomeData | null>(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();
  const homeResolveCache = useRef<Map<string, Promise<MediaItem[]>>>(new Map());
  const homeResolvedTargets = useRef<Map<string, MediaItem[]>>(new Map());

  useEffect(() => {
    getDoubanHome()
      .then(setDoubanData)
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  const resolveHomeTarget = useCallback((title: string) => {
    const key = title.trim();
    if (!key) return Promise.resolve([] as MediaItem[]);
    const cached = homeResolveCache.current.get(key);
    if (cached) return cached;

    const pending = searchMedia(key)
      .then(results => {
        const list = results || [];
        homeResolvedTargets.current.set(key, list);
        return list;
      })
      .catch(() => []);
    homeResolveCache.current.set(key, pending);
    return pending;
  }, []);

  const sourceDetailPath = useCallback((title: string) => (
    `/detail/source/${encodeURIComponent(title.trim())}`
  ), []);

  const getResolvedTarget = useCallback((title: string) => {
    return homeResolvedTargets.current.get(title.trim()) || null;
  }, []);

  // 点击豆瓣卡片：已有预解析缓存就直达具体详情；无缓存时立刻进详情加载页，不在首页等待搜索完成。
  const handleClick = useCallback((title: string) => {
    const key = title.trim();
    if (!key) return;
    const resolved = getResolvedTarget(key);
    if (resolved?.length) {
      navigate(detailLink(resolved[0]));
      return;
    }
    void resolveHomeTarget(key);
    navigate(sourceDetailPath(key));
  }, [getResolvedTarget, navigate, resolveHomeTarget, sourceDetailPath]);

  // 豆瓣数据 → MediaItem 映射（供 Banner 使用）
  const bannerItems = useMemo<MediaItem[]>(() =>
    (doubanData?.hot || []).slice(0, 5).map(d => ({
      vod_id: d.id,
      vod_name: d.title,
      vod_pic: proxyImg(d.cover),
      type_name: '',
      vod_remarks: d.rate ? `⭐${d.rate}` : '',
      vod_content: d.abstract || (d.rate && d.year ? `豆瓣评分 ${d.rate} · ${d.year}年` : ''),
      vod_year: d.year || '',
      vod_area: '',
      vod_actor: '',
      site_key: '',
      site_name: '',
      latency: 0,
    } as MediaItem)),
  [doubanData]);

  useEffect(() => {
    if (!bannerItems.length) return;
    const targets = bannerItems.slice(0, 4);
    targets.forEach(item => {
      if (item.vod_name) void resolveHomeTarget(item.vod_name);
    });
  }, [bannerItems, resolveHomeTarget]);

  useEffect(() => {
    if (loading) return;
    const timer = window.setTimeout(() => {
      const prefetchList = [
        ['movie', '电影'],
        ['tv', '电视剧'],
        ['variety', '综艺'],
        ['anime', '动漫'],
      ] as const;
      prefetchList.forEach(([category, fallback]) => {
        void getCategory(category, fallback, 1, 30).catch(() => {});
      });
    }, 900);
    return () => window.clearTimeout(timer);
  }, [loading]);

  // Banner 回调（依赖稳定，用 useCallback）
  const handleBannerPlay = useCallback((item: MediaItem) => {
    const key = item.vod_name.trim();
    if (!key) return;
    const resolved = getResolvedTarget(key);
    if (resolved?.length) {
      navigate(`/player/${resolved[0].site_key}/${resolved[0].vod_id}`);
      return;
    }
    void resolveHomeTarget(key);
    navigate(sourceDetailPath(key));
  }, [getResolvedTarget, navigate, resolveHomeTarget, sourceDetailPath]);

  const handleBannerDetail = useCallback((item: MediaItem) => {
    handleClick(item.vod_name);
  }, [handleClick]);

  // Loading骨架屏
  if (loading) {
    return (
      <div className="space-y-10">
        <div className="h-[460px] rounded-3xl bg-white/[0.02] animate-pulse border border-white/5" />
        {[1,2,3].map(i => (
          <div key={i} className="space-y-4">
            <div className="h-5 w-28 rounded-full bg-white/[0.04] animate-pulse" />
            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-4">
              {Array.from({length: 6}).map((_, j) => (
                <div key={j} className="aspect-[2/3] rounded-2xl bg-white/[0.03] animate-pulse" />
              ))}
            </div>
          </div>
        ))}
      </div>
    );
  }

  // 空数据
  if (!doubanData || !doubanData.hot?.length) {
    return (
      <div className="flex flex-col items-center justify-center py-20 text-zinc-500 gap-3">
        <span className="text-4xl">🎬</span>
        <p>暂无推荐，检查 API 服务</p>
        <button onClick={() => window.location.reload()} className="px-4 py-2 bg-zinc-800 rounded-lg text-sm hover:bg-zinc-700">刷新</button>
      </div>
    );
  }

  return (
    <div className="relative pb-12">
      {/* 全局流体背景 */}
      <div className="fixed inset-0 pointer-events-none overflow-hidden -z-10">
        <div className="fluid-orb" />
        <div className="fluid-orb" />
        <div className="fluid-orb" />
      </div>

      {/* Banner */}
      <div className="animate-reveal -mx-2 sm:-mx-4 lg:-mx-8 mb-10">
        <Banner items={bannerItems} onPlay={handleBannerPlay} onDetail={handleBannerDetail} />
      </div>

      {/* 热门推荐 */}
      <Section title="热门推荐" icon="🔥" moreLink="/movie" delay={0}>
        <DoubanGrid items={doubanData.hot.slice(0, 18)} onClick={handleClick} onPreview={resolveHomeTarget} />
      </Section>

      {/* 各分类 */}
      {sections.map((section, idx) => {
        const items = doubanData[section.key] || [];
        if (!items.length) return null;
        return (
          <Section key={section.key} title={section.label} icon={section.icon} moreLink={section.path} delay={idx + 1}>
            <DoubanGrid items={items.slice(0, section.count)} onClick={handleClick} onPreview={resolveHomeTarget} />
          </Section>
        );
      })}
    </div>
  );
}

/** 豆瓣海报网格 */
function DoubanGrid({
  items,
  onClick,
  onPreview,
}: {
  items: DoubanSubject[];
  onClick: (title: string) => void;
  onPreview: (title: string) => Promise<MediaItem[]>;
}) {
  return (
    <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-4">
      {items.map((item) => {
        const posterUrl = proxyImg(item.cover);
        return (
          <button
            key={item.id}
            onClick={() => onClick(item.title)}
            onMouseEnter={() => { if (item.title) void onPreview(item.title); }}
            onFocus={() => { if (item.title) void onPreview(item.title); }}
            className="group relative rounded-2xl overflow-hidden bg-white/[0.03] hover:bg-white/[0.06] border border-white/[0.04] hover:border-white/[0.08] transition-all duration-300 text-left hover:scale-[1.02] hover:shadow-lg hover:shadow-white/5"
          >
            <div className="aspect-[2/3] relative overflow-hidden">
              <div className="absolute inset-0 bg-gradient-to-br from-brand-500/20 to-orange-500/20 flex items-center justify-center">
                <span className="text-5xl font-bold text-white/10">{item.title?.charAt(0) || '?'}</span>
              </div>
              <img
                src={posterUrl}
                alt={item.title}
                className="absolute inset-0 w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                loading="lazy"
                onError={(e) => { (e.target as HTMLImageElement).style.display = 'none'; }}
              />
              {item.rate && (
                <span className="absolute top-2 right-2 px-2 py-0.5 bg-black/60 backdrop-blur-md text-yellow-400 text-xs font-bold rounded-lg border border-yellow-500/20 z-10">
                  ★ {item.rate}
                </span>
              )}
              <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-transparent to-transparent pointer-events-none" />
              <div className="absolute bottom-0 left-0 right-0 p-2.5 z-10">
                <h3 className="text-xs font-medium text-white/90 line-clamp-2 leading-snug">{item.title}</h3>
                {item.year && <p className="text-[10px] text-white/40 mt-0.5">{item.year}</p>}
              </div>
            </div>
          </button>
        );
      })}
    </div>
  );
}

/** 带动画的区块 */
function Section({
  title, icon, moreLink, children, delay = 0,
}: {
  title: string; icon: string; moreLink?: string;
  children: ReactNode; delay?: number;
}) {
  const navigate = useNavigate();
  return (
    <section className="mb-10 animate-reveal" style={{ animationDelay: `${delay * 0.06}s` }}>
      <div className="flex items-center justify-between mb-5">
        <div className="flex items-center gap-3">
          <span className="text-xl">{icon}</span>
          <h2 className="text-xl font-bold text-white/90 tracking-tight">{title}</h2>
          <div className="h-px flex-1 min-w-8 bg-gradient-to-r from-white/10 to-transparent ml-3" />
        </div>
        {moreLink && (
          <button
            onClick={() => navigate(moreLink)}
            className="text-xs text-white/30 hover:text-cyan-400 transition-colors duration-300 font-medium tracking-wide"
          >
            查看更多 →
          </button>
        )}
      </div>
      {children}
    </section>
  );
}
