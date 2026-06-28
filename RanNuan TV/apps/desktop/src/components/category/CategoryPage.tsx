import { useState, useEffect, useCallback, useRef, useMemo, type ComponentType } from 'react';
import { useNavigate } from 'react-router-dom';
import { getCategory } from 'shared';
import type { MediaItem } from 'shared';
import MediaGrid from '../media/MediaGrid';
import Pagination from '../common/Pagination';
import { Search, Filter, ArrowLeft, Loader2, RefreshCw, AlertCircle } from 'lucide-react';
import { detailLink } from '../../utils/navigate';

export interface SubCategory {
  label: string;
  typeMatch?: string;
}

export interface CategoryConfig {
  key: string;
  label: string;
  Icon: ComponentType<{ size?: number; className?: string }>;
  categoryId: string;
  searchKeyword: string;
  title?: string;
  subtitle?: string;
  subCategories?: SubCategory[];
}

type SortKey = 'default' | 'name' | 'source' | 'year';

// ===== 模块级缓存（key 含子分类标识） =====
const globalPageCache = new Map<string, { list: MediaItem[]; total: number; serverFiltered?: boolean }>();
const globalScrollPositions = new Map<string, number>();

function isValidCacheEntry(entry?: { list: MediaItem[]; total: number }) {
  return !!(entry && entry.list.length > 0);
}

function cacheKey(categoryId: string, subType: string | null, p: number, ps: number) {
  return `${categoryId}::${subType || 'all'}::${p}::${ps}`;
}

export default function CategoryPage({ config }: { config: CategoryConfig }) {
  const ck = cacheKey(config.categoryId, null, 1, 30);
  const rawCache = globalPageCache.get(ck);
  const hasValidCache = isValidCacheEntry(rawCache);

  const [list, setList] = useState<MediaItem[]>(hasValidCache ? rawCache!.list : []);
  const [total, setTotal] = useState(hasValidCache ? rawCache!.total : 0);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(30);
  const [loading, setLoading] = useState(!hasValidCache);
  const [error, setError] = useState<string | null>(null);
  const [outOfRange, setOutOfRange] = useState(false);
  const [activeSub, setActiveSub] = useState<string | null>(null);
  const [serverFiltered, setServerFiltered] = useState(false); // 当前结果是否由服务端 type_id 过滤
  const [subCounts, setSubCounts] = useState<Record<string, number> | null>(null); // 服务端返回的子分类计数
  const [sourceFilter, setSourceFilter] = useState<string | null>(null);
  const [sortBy, setSortBy] = useState<SortKey>('default');

  // === 渐进式分页状态 ===
  const loadedPagesRef = useRef<Set<number>>(new Set(hasValidCache ? [1] : []));
  const [hasNextPage, setHasNextPage] = useState(true);
  const [renderTick, setRenderTick] = useState(0);

  const navigate = useNavigate();
  const abortRef = useRef<AbortController | null>(null);
  const fetchGenRef = useRef(0);
  const activeSubRef = useRef<string | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    return () => {
      if (containerRef.current) globalScrollPositions.set(config.categoryId, window.scrollY);
    };
  }, [config.categoryId]);

  useEffect(() => {
    const saved = globalScrollPositions.get(config.categoryId);
    if (saved && hasValidCache) {
      requestAnimationFrame(() => window.scrollTo(0, saved));
    }
  }, [config.categoryId, hasValidCache]);

  const fetchPage = useCallback(async (p: number, ps: number, sub?: string | null) => {
    const subType = sub ?? activeSubRef.current;
    const ck = cacheKey(config.categoryId, subType, p, ps);
    const cached = globalPageCache.get(ck);
    if (isValidCacheEntry(cached)) {
      setList(cached!.list);
      setTotal(cached!.total);
      setServerFiltered(!!cached!.serverFiltered);
      setError(null);
      setOutOfRange(false);
      setLoading(false);
      return;
    }

    if (abortRef.current) abortRef.current.abort();
    const controller = new AbortController();
    abortRef.current = controller;
    const gen = ++fetchGenRef.current;

    setList([]);
    setError(null);
    setOutOfRange(false);
    setSourceFilter(null);
    setSortBy('default');
    setLoading(true);

    try {
      const data = await getCategory(config.categoryId, config.searchKeyword, p, ps, controller.signal, subType || undefined);
      if (gen !== fetchGenRef.current) return;

      if (data.list && data.list.length > 0) {
        globalPageCache.set(ck, { list: data.list, total: data.total, serverFiltered: !!data.serverFiltered });
        if (globalPageCache.size > 50) {
          const firstKey = globalPageCache.keys().next().value;
          if (firstKey) globalPageCache.delete(firstKey);
        }
        loadedPagesRef.current.add(p);
        setRenderTick(t => t + 1);
        setHasNextPage(data.list.length >= ps);
      } else if (p > 1) {
        setHasNextPage(false);
      }

      setList(data.list || []);
      if (data.total > 0) setTotal(data.total);
      setServerFiltered(!!data.serverFiltered);
      setOutOfRange(!!data.outOfRange || ((data.list?.length ?? 0) === 0 && p > 1 && data.total > 0));
      if (data.subCounts) setSubCounts(data.subCounts);
      setError(null);
    } catch (e: any) {
      if (e.name === 'AbortError' || gen !== fetchGenRef.current) return;
      setError(e?.message || '网络请求失败，请检查 API 服务是否正常运行');
      setList([]);
      if (p === 1) setTotal(0);
    } finally {
      if (gen === fetchGenRef.current) setLoading(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [config.categoryId, config.searchKeyword]);

  const maxLoadedPage = useMemo(() => {
    const pages = Array.from(loadedPagesRef.current);
    return pages.length > 0 ? Math.max(...pages) : 1;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [renderTick]);

  const maxClickablePage = hasNextPage ? maxLoadedPage + 1 : maxLoadedPage;

  const handlePageChange = useCallback((p: number) => {
    const target = Math.max(1, Math.min(p, maxClickablePage));
    setPage(target);
    fetchPage(target, pageSize);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }, [pageSize, fetchPage, maxClickablePage]);
  const handlePageSizeChange = useCallback((ps: number) => {
    setPageSize(ps); setPage(1);
    loadedPagesRef.current.clear();
    setHasNextPage(true);
    setRenderTick(t => t + 1);
    fetchPage(1, ps);
  }, [fetchPage]);
  // 子分类切换：重置页码 + 服务端重新请求
  const handleSubChange = useCallback((sub: string | null) => {
    activeSubRef.current = sub;
    setActiveSub(sub);
    setPage(1);
    setList([]);
    setTotal(0);
    setServerFiltered(false);
    setSubCounts(null);
    setHasNextPage(true);
    loadedPagesRef.current.clear();
    setRenderTick(t => t + 1);
    fetchPage(1, pageSize, sub);
  }, [pageSize, fetchPage]);

  // ---- 初始加载 ----
  useEffect(() => {
    if (hasValidCache) return;
    fetchPage(1, pageSize, null);
    return () => { if (abortRef.current) abortRef.current.abort(); };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  // ---- subCounts 延迟获取（第一次进页面时异步回来后才有值） ----
  useEffect(() => {
    if (loading || total === 0 || page !== 1 || activeSub) return;
    if (subCounts && Object.values(subCounts).some(v => v > 0)) return; // 已有真实值
    const timer = setTimeout(async () => {
      try {
        const data = await getCategory(config.categoryId, config.searchKeyword, 1, pageSize);
        if (data.subCounts && Object.values(data.subCounts).some(v => v > 0)) {
          setSubCounts(data.subCounts);
        }
      } catch {}
    }, 4000);
    return () => clearTimeout(timer);
  }, [loading, total, page, activeSub, pageSize, config.categoryId, config.searchKeyword, subCounts]);

  // ---- 智能预加载后续页面 ----
  const preloadRef = useRef<AbortController | null>(null);
  useEffect(() => {
    if (loading || total === 0) return;
    // 用已加载最大页而非 total/pageSize，避免向不存在的远页发起预加载
    const limit = Math.max(maxLoadedPage + 3, page + 3);
    const preloadPages = [page + 1, page + 2, page + 3].filter(p => p <= limit);
    if (preloadPages.length === 0) return;

    if (preloadRef.current) preloadRef.current.abort();
    const controller = new AbortController();
    preloadRef.current = controller;

    const preloadAll = async () => {
      for (const p of preloadPages) {
        const sub = activeSubRef.current; // 读取最新值，避免闭包过期
        const ck = cacheKey(config.categoryId, sub, p, pageSize);
        if (isValidCacheEntry(globalPageCache.get(ck))) continue;
        try {
          const data = await getCategory(config.categoryId, config.searchKeyword, p, pageSize, controller.signal, sub || undefined);
          if (data.list && data.list.length > 0) {
            globalPageCache.set(ck, { list: data.list, total: data.total, serverFiltered: !!data.serverFiltered });
            if (globalPageCache.size > 50) {
              const firstKey = globalPageCache.keys().next().value;
              if (firstKey) globalPageCache.delete(firstKey);
            }
            loadedPagesRef.current.add(p);
            setRenderTick(t => t + 1);
          }
        } catch { break; }
      }
    };

    preloadAll();
    return () => controller.abort();
  }, [page, total, pageSize, loading, config.categoryId, config.searchKeyword]);

  const sourceNames = useMemo(() => {
    const s = new Set<string>(); list.forEach(i => { if (i.site_name) s.add(i.site_name); }); return Array.from(s);
  }, [list]);

  // 显示列表：服务端已过滤时，前端只做 source/sort 处理
  const displayList = useMemo(() => {
    let r = list;
    // 服务端未过滤时，回退客户端 typeMatch 过滤
    if (activeSub && !serverFiltered) {
      const sc = config.subCategories?.find(s => s.label === activeSub);
      const mp = sc?.typeMatch || sc?.label || '';
      if (mp) {
        const ks = mp.split('|').map(k => k.trim()).filter(Boolean);
        // 同时查 type_name 和 vod_name（综艺等 type_name 是地区而非内容分类，需靠标题匹配）
        r = list.filter(item => {
          const tn = (item.type_name || '').toLowerCase();
          const vn = (item.vod_name || '').toLowerCase();
          return ks.some(kw => tn.includes(kw.toLowerCase()) || vn.includes(kw.toLowerCase()));
        });
      }
    }
    if (sourceFilter) r = r.filter(item => item.site_name === sourceFilter);
    if (sortBy === 'name') r = [...r].sort((a, b) => (a.vod_name || '').localeCompare(b.vod_name || '', 'zh'));
    else if (sortBy === 'source') r = [...r].sort((a, b) => (a.site_name || '').localeCompare(b.site_name || '', 'zh'));
    else if (sortBy === 'year') r = [...r].sort((a, b) => (b.vod_year || '').localeCompare(a.vod_year || '', 'zh'));
    return r;
  }, [list, activeSub, config.subCategories, sourceFilter, sortBy, serverFiltered]);

  const title = config.title || config.label;
  const displayTotal = total;

  return (
    <div ref={containerRef} className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <button onClick={() => navigate('/')} className="p-2 -ml-2 rounded-lg text-zinc-500 hover:text-white hover:bg-zinc-800 transition-colors" title="返回首页"><ArrowLeft size={20} /></button>
          <config.Icon size={28} className="text-brand-400" />
          <div><h1 className="text-2xl font-bold text-white/90 tracking-tight">{title}</h1><p className="text-sm text-zinc-500 mt-0.5">{config.subtitle || `浏览全部${config.label}内容`}</p></div>
        </div>
        {!loading && total > 0 && (
          <div className="flex items-center gap-2 text-sm">
            <span className="text-zinc-500">{activeSub ? `约 ${displayTotal} 条（${activeSub}）` : `约 ${displayTotal} 条`}</span>
          </div>
        )}
      </div>

      {/* ===== 标签栏（聚合统计） ===== */}
      {config.subCategories && config.subCategories.length > 0 && (
        <div className="flex flex-wrap items-center gap-2 -mt-2">
          <button onClick={() => handleSubChange(null)}
            className={`px-4 py-2 rounded-xl text-sm font-medium transition-all duration-200 ${activeSub === null ? 'bg-brand-500 text-white shadow-lg shadow-brand-500/20' : 'bg-zinc-800/80 text-zinc-400 hover:bg-zinc-700 hover:text-white'}`}>
            全部
          </button>
          {config.subCategories.map(sub => {
            const active = activeSub === sub.label;
            // 优先用服务端返回的计数，否则用客户端聚合
            const count = subCounts?.[sub.label] ?? 0;
            const showCount = count > 0;
            return (
              <button key={sub.label} onClick={() => handleSubChange(sub.label)}
                className={`px-4 py-2 rounded-xl text-sm font-medium transition-all duration-200 ${active ? 'bg-brand-500 text-white shadow-lg shadow-brand-500/20 scale-105' : 'bg-zinc-800/80 text-zinc-400 hover:bg-zinc-700 hover:text-white hover:scale-105'}`}>
                {sub.label}
                {showCount && (
                  <span className="ml-1.5 text-[11px] opacity-60">{count}</span>
                )}
              </button>
            );
          })}
        </div>
      )}

      {list.length > 0 && !loading && (
        <div className="flex flex-wrap items-center gap-2 pb-1">
          <Filter size={14} className="text-zinc-600 mr-1" />
          <button onClick={() => setSourceFilter(null)} className={`px-3 py-1 rounded-lg text-xs transition-colors ${sourceFilter === null ? 'bg-brand-500 text-white shadow-sm' : 'bg-zinc-800 text-zinc-400 hover:bg-zinc-700'}`}>全部</button>
          {sourceNames.map(name => { const c = list.filter(i => i.site_name === name).length; return <button key={name} onClick={() => setSourceFilter(name)} className={`px-3 py-1 rounded-lg text-xs transition-colors ${sourceFilter === name ? 'bg-brand-500 text-white shadow-sm' : 'bg-zinc-800 text-zinc-400 hover:bg-zinc-700'}`}>{name} ({c})</button>; })}
          <div className="flex-1" />
          <select value={sortBy} onChange={e => setSortBy(e.target.value as SortKey)} className="bg-zinc-800 border border-zinc-700 rounded-lg text-xs text-zinc-400 px-3 py-1.5 focus:outline-none focus:border-zinc-600 cursor-pointer">
            <option value="default">默认排序</option><option value="name">按名称排序</option><option value="year">按年份排序</option><option value="source">按来源排序</option>
          </select>
        </div>
      )}

      {/* 加载中（首次加载 / 翻页均显示，优先级高于空状态） */}
      {loading && (
        <div className="flex flex-col items-center justify-center py-32 gap-4">
          <div className="relative">
            <Loader2 size={44} className="text-brand-400 animate-spin" />
            <div className="absolute inset-0 rounded-full blur-2xl bg-brand-500/20 animate-pulse" />
          </div>
          <div className="text-center space-y-1">
            <p className="text-white/80 text-base font-medium">正在加载中</p>
            <p className="text-zinc-500 text-sm">
              {page > 1 ? `正在加载第 ${page} 页...` : '正在从多个站点获取数据，请稍候...'}
            </p>
          </div>
          <div className="flex gap-1 mt-2">
            {[0, 1, 2].map(i => (
              <div key={i} className="w-2 h-2 rounded-full bg-brand-500 animate-bounce" style={{ animationDelay: `${i * 0.15}s` }} />
            ))}
          </div>
        </div>
      )}

      {/* 数据列表 */}
      {!loading && displayList.length > 0 && (
        <MediaGrid items={displayList} onClick={item => navigate(detailLink(item))} />
      )}

      {!loading && activeSub && displayList.length === 0 && list.length > 0 && !serverFiltered && (
        <div className="flex flex-col items-center justify-center py-16 text-zinc-500 gap-3"><Search size={28} className="opacity-30" /><p className="text-sm">当前页暂无匹配「{activeSub}」的内容，试试翻页</p><button onClick={() => handleSubChange(null)} className="px-4 py-1.5 bg-zinc-800 hover:bg-zinc-700 rounded-lg text-xs transition-colors">显示全部</button></div>
      )}
      {!loading && total > 0 && (
        <Pagination
          page={page}
          pageSize={pageSize}
          total={total}
          onPageChange={handlePageChange}
          onPageSizeChange={handlePageSizeChange}
          progressive
          maxPage={maxLoadedPage}
          hasNext={hasNextPage}
          showTotalEstimate
        />
      )}

      {/* 翻页超出实际数据范围 */}
      {!loading && !error && outOfRange && list.length === 0 && total > 0 && (
        <div className="flex flex-col items-center justify-center py-16 text-zinc-500 gap-3">
          <Search size={28} className="opacity-30" />
          <p className="text-sm">第 {page} 页暂无数据，已超出可浏览范围</p>
          <button
            onClick={() => handlePageChange(1)}
            className="px-4 py-1.5 bg-zinc-800 hover:bg-zinc-700 rounded-lg text-xs transition-colors"
          >
            返回第 1 页
          </button>
        </div>
      )}
      {/* API 错误状态 */}
      {!loading && error && list.length === 0 && (
        <div className="flex flex-col items-center justify-center py-24 text-zinc-500 gap-4">
          <div className="w-20 h-20 rounded-full bg-red-900/20 flex items-center justify-center">
            <AlertCircle size={32} className="text-red-400 opacity-60" />
          </div>
          <div className="text-center space-y-2">
            <p className="text-lg font-medium text-white/70">加载失败</p>
            <p className="text-sm text-zinc-500 max-w-md leading-relaxed">{error}</p>
          </div>
          <button
            onClick={() => { setError(null); fetchPage(page, pageSize, activeSubRef.current); }}
            className="flex items-center gap-2 px-5 py-2.5 bg-red-500/10 hover:bg-red-500/20 text-red-400 rounded-xl text-sm font-medium transition-colors border border-red-500/20"
          >
            <RefreshCw size={15} /> 重新加载
          </button>
        </div>
      )}
      {/* 服务端子分类过滤后无数据 */}
      {!loading && !error && serverFiltered && activeSub && list.length === 0 && total === 0 && (
        <div className="flex flex-col items-center justify-center py-24 text-zinc-500 gap-4">
          <div className="w-20 h-20 rounded-full bg-zinc-900 flex items-center justify-center"><Search size={32} className="opacity-30" /></div>
          <div className="text-center space-y-1">
            <p className="text-lg">暂无「{activeSub}」内容</p>
            <p className="text-sm text-zinc-600">该子分类暂无数据，请尝试其他标签</p>
          </div>
          <button onClick={() => handleSubChange(null)} className="px-6 py-2 bg-zinc-800 hover:bg-zinc-700 rounded-xl text-sm transition-colors">返回全部</button>
        </div>
      )}
      {/* 真正无数据状态（已加载完成且 API 返回空） */}
      {!loading && !error && list.length === 0 && total === 0 && !serverFiltered && (
        <div className="flex flex-col items-center justify-center py-24 text-zinc-500 gap-4">
          <div className="w-20 h-20 rounded-full bg-zinc-900 flex items-center justify-center"><Search size={32} className="opacity-30" /></div>
          <div className="text-center space-y-1"><p className="text-lg">暂无{config.label}内容</p><p className="text-sm text-zinc-600">当前分类无可用数据，请尝试其他分类</p></div>
          <button onClick={() => { setError(null); fetchPage(1, pageSize); }} className="px-6 py-2 bg-zinc-800 hover:bg-zinc-700 rounded-xl text-sm transition-colors">重新加载</button>
        </div>
      )}
    </div>
  );
}
