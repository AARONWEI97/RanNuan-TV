import { useState, useEffect, useCallback, useRef, useMemo } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { searchMediaStream } from 'shared';
import type { MediaItem } from 'shared';
import MediaGrid from '../components/media/MediaGrid';
import { X, Search } from 'lucide-react';
import { detailLink } from '../utils/navigate';

type SortKey = 'default' | 'name' | 'source';

type SearchCacheEntry = {
  results: MediaItem[];
  stats: { indexHits: number; cmsHits: number; merged: number } | null;
  indexCoverage: number;
  actorLike: boolean;
};

const searchCache = new Map<string, SearchCacheEntry>();

function dedupeResults(list: MediaItem[]) {
  const seen = new Set<string>();
  return list.filter(item => {
    const key = `${item.site_key || item.site_name || ''}:${item.vod_id || item.vod_name}`;
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  });
}

export default function SearchPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const wd = searchParams.get('wd') || '';
  const type = searchParams.get('type') || '';

  const [results, setResults] = useState<MediaItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [progress, setProgress] = useState({ completed: 0, total: 0, phase: '' });
  const [scanning, setScanning] = useState(false);
  const [searchStats, setSearchStats] = useState<{ indexHits: number; cmsHits: number; merged: number } | null>(null);
  const [indexCoverage, setIndexCoverage] = useState(0);
  const [actorLike, setActorLike] = useState(false);
  const [keyword, setKeyword] = useState(wd || type);
  const [sourceFilter, setSourceFilter] = useState<string | null>(null);
  const [sortBy, setSortBy] = useState<SortKey>('default');
  const navigate = useNavigate();
  const abortRef = useRef<AbortController | null>(null);
  const mergedRef = useRef(false); // 防止 merged 后被后续 videos 污染
  const freshChunkRef = useRef(false);
  const actorMetaRef = useRef({ actorLike: false, indexCoverage: 0 });
  const inputRef = useRef<HTMLInputElement>(null);

  const doSearch = useCallback(async (q: string) => {
    const query = q.trim();
    if (!query) return;

    if (abortRef.current) abortRef.current.abort();
    const controller = new AbortController();
    abortRef.current = controller;
    const cached = searchCache.get(query);

    setLoading(true);
    if (cached) {
      setResults(cached.results);
      setSearchStats(cached.stats);
      setIndexCoverage(cached.indexCoverage);
      setActorLike(cached.actorLike);
    }
    setScanning(false);
    if (!cached) {
      setSearchStats(null);
      setIndexCoverage(0);
      setActorLike(false);
    }
    mergedRef.current = false;
    freshChunkRef.current = !!cached;
    setKeyword(query);
    setProgress({ completed: 0, total: 0, phase: '' });
    setSourceFilter(null);
    setSortBy('default');

    try {
      await searchMediaStream(
        query,
        (videos) => {
          // merged 后忽略后续 videos，防止将合并结果重新污染为未合并状态
          if (mergedRef.current) return;
          setResults(prev => {
            const next = freshChunkRef.current ? dedupeResults([...prev, ...videos]) : dedupeResults(videos);
            freshChunkRef.current = true;
            return next;
          });
        },
        ({ completed, total, phase }) => {
          setProgress({ completed, total, phase: phase || '' });
          if (phase === 'scanning') setScanning(true);
        },
        controller.signal,
        (merged, stats) => {
          mergedRef.current = true;
          const next = dedupeResults(merged);
          setResults(next);
          if (stats) setSearchStats(stats);
          setScanning(false);
          searchCache.set(query, {
            results: next,
            stats: stats || null,
            indexCoverage: actorMetaRef.current.indexCoverage,
            actorLike: actorMetaRef.current.actorLike,
          });
          if (searchCache.size > 20) {
            const firstKey = searchCache.keys().next().value;
            if (firstKey) searchCache.delete(firstKey);
          }
        },
        ({ actorLike: al, indexCoverage: ic }) => {
          actorMetaRef.current = { actorLike: al, indexCoverage: ic };
          setActorLike(al);
          setIndexCoverage(ic);
        },
      );
    } catch (e: any) {
      if (e.name === 'AbortError') return;
    } finally {
      setLoading(false);
      if (!freshChunkRef.current && !controller.signal.aborted) setResults([]);
    }
  }, []);

  useEffect(() => {
    const query = wd || type;
    if (query) doSearch(query);
    return () => { if (abortRef.current) abortRef.current.abort(); };
  }, [wd, type, doSearch]);

  const handleSearch = (e: { preventDefault: () => void }) => {
    e.preventDefault();
    if (keyword.trim()) setSearchParams({ wd: keyword.trim() });
  };

  const cancelSearch = () => {
    if (abortRef.current) abortRef.current.abort();
    setLoading(false);
    setProgress({ completed: 0, total: 0, phase: '' });
    setScanning(false);
    setSearchStats(null);
  };

  // 源列表
  const sourceNames = useMemo(() => {
    const set = new Set<string>();
    results.forEach(item => { if (item.site_name) set.add(item.site_name); });
    return Array.from(set);
  }, [results]);

  // 过滤 + 排序
  const filtered = useMemo(() => {
    let list = sourceFilter
      ? results.filter(item => item.site_name === sourceFilter)
      : results;

    if (sortBy === 'name') {
      list = [...list].sort((a, b) => (a.vod_name || '').localeCompare(b.vod_name || '', 'zh'));
    } else if (sortBy === 'source') {
      list = [...list].sort((a, b) => (a.site_name || '').localeCompare(b.site_name || '', 'zh'));
    }
    return list;
  }, [results, sourceFilter, sortBy]);

  const isStreaming = loading && progress.total > 0;

  return (
    <div className="space-y-5">
      {/* 搜索框 */}
      <form onSubmit={handleSearch} className="flex gap-3">
        <div className="relative flex-1">
          <Search size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-zinc-500" />
          <input
            ref={inputRef}
            type="text"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            placeholder={type ? `浏览${type}...` : '输入关键词搜索...'}
            autoFocus
            className="w-full h-12 pl-11 pr-4 bg-zinc-900 border border-zinc-700 rounded-xl text-white placeholder-zinc-500 text-lg focus:outline-none focus:border-brand-500 focus:ring-1 focus:ring-brand-500 transition-all"
          />
          {keyword && (
            <button
              type="button"
              onClick={() => { setKeyword(''); inputRef.current?.focus(); }}
              className="absolute right-3 top-1/2 -translate-y-1/2 p-1 text-zinc-500 hover:text-white"
            >
              <X size={16} />
            </button>
          )}
        </div>
        {loading ? (
          <button
            type="button"
            onClick={cancelSearch}
            className="px-5 h-12 bg-red-500/20 hover:bg-red-500/30 text-red-400 rounded-xl text-sm font-medium transition-colors shrink-0"
          >
            取消
          </button>
        ) : (
          <button
            type="submit"
            className="px-5 h-12 bg-brand-500 hover:bg-brand-600 rounded-xl text-sm font-medium transition-colors shrink-0"
          >
            搜索
          </button>
        )}
      </form>

      {/* 搜索状态提示 */}
      {scanning && (
        <div className="flex items-center gap-2 text-xs text-amber-400 bg-amber-500/10 px-3 py-2 rounded-lg">
          <div className="w-3 h-3 border-2 border-amber-400 border-t-transparent rounded-full animate-spin" />
          正在扩大搜索范围，扫描更多页面中...
        </div>
      )}
      {/* SSE 进度 */}
      {isStreaming && (
        <div className="space-y-2">
          <div className="flex items-center justify-between text-xs text-zinc-400">
            <span>正在搜索 {progress.total} 个源...</span>
            <span>{progress.completed}/{progress.total}</span>
          </div>
          <div className="h-1 bg-zinc-800 rounded-full overflow-hidden">
            <div
              className="h-full bg-brand-500 rounded-full transition-all duration-300"
              style={{ width: `${progress.total > 0 ? (progress.completed / progress.total) * 100 : 0}%` }}
            />
          </div>
        </div>
      )}

      {/* 筛选 + 排序 */}
      {results.length > 0 && !isStreaming && (
        <div className="flex flex-wrap items-center gap-2">
          <button
            onClick={() => setSourceFilter(null)}
            className={`px-3 py-1 rounded-lg text-xs transition-colors ${
              sourceFilter === null ? 'bg-brand-500 text-white' : 'bg-zinc-800 text-zinc-400 hover:bg-zinc-700'
            }`}
          >
            全部 ({results.length})
          </button>
          {sourceNames.map(name => {
            const count = results.filter(item => item.site_name === name).length;
            return (
              <button
                key={name}
                onClick={() => setSourceFilter(name)}
                className={`px-3 py-1 rounded-lg text-xs transition-colors ${
                  sourceFilter === name ? 'bg-brand-500 text-white' : 'bg-zinc-800 text-zinc-400 hover:bg-zinc-700'
                }`}
              >
                {name} ({count})
              </button>
            );
          })}
          <div className="flex-1" />
          <select
            value={sortBy}
            onChange={(e) => setSortBy(e.target.value as SortKey)}
            className="bg-zinc-800 border border-zinc-700 rounded-lg text-xs text-zinc-400 px-3 py-1.5 focus:outline-none focus:border-zinc-600"
          >
            <option value="default">默认排序</option>
            <option value="name">按名称</option>
            <option value="source">按来源</option>
          </select>
        </div>
      )}

      {/* 非 SSE loading */}
      {loading && !isStreaming && (
        <div className="flex items-center justify-center py-20">
          <div className="w-8 h-8 border-2 border-brand-500 border-t-transparent rounded-full animate-spin" />
        </div>
      )}

      {/* 结果 */}
      {(isStreaming || filtered.length > 0) && (
        <>
          <p className="text-sm text-zinc-500">
            {isStreaming
              ? `已找到 ${results.length} 个结果，搜索中...`
              : `共 ${filtered.length} 个结果`}
            {searchStats && !isStreaming && (
              <span className="ml-2 text-xs text-zinc-600">
                {searchStats.indexHits > 0 && `演员索引 ${searchStats.indexHits} 条`}
                {searchStats.indexHits > 0 && searchStats.cmsHits > 0 && ` + `}
                {searchStats.cmsHits > 0 && `站点搜索 ${searchStats.cmsHits} 条`}
              </span>
            )}
          </p>
          {actorLike && indexCoverage > 0 && (
            <p className="text-xs text-zinc-600 mt-1">
              索引已覆盖 {indexCoverage.toLocaleString()} 条记录
              {indexCoverage < 10000 && '，结果可能不完整，浏览分类可扩展索引'}
            </p>
          )}
          <MediaGrid
            items={isStreaming ? results : filtered}
            onClick={(item) => navigate(detailLink(item))}
          />
        </>
      )}

      {/* 空结果 */}
      {!loading && results.length === 0 && (wd || type) && (
        <div className="flex flex-col items-center justify-center py-20 text-zinc-500 gap-2">
          <Search size={40} className="opacity-30" />
          <p className="text-lg">未找到相关内容</p>
          <p className="text-sm">换个关键词试试</p>
        </div>
      )}

      {!loading && results.length === 0 && !wd && !type && (
        <div className="flex flex-col items-center justify-center py-20 text-zinc-500 gap-2">
          <Search size={40} className="opacity-30" />
          <p className="text-lg">搜索你想看的内容</p>
        </div>
      )}
    </div>
  );
}
