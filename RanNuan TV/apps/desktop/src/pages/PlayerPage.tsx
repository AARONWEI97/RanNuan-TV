import { useState, useEffect, useRef, useCallback, useMemo } from 'react';
import { useParams, useSearchParams, useNavigate } from 'react-router-dom';
import { getDetail, getMultiDetail, parsePlaySources, searchMedia, getCategory, useHistoryStore } from 'shared';
import type { MediaDetail, MediaItem, MultiDetailItem } from 'shared';
import VideoPlayer from '../components/player/VideoPlayer';
import { AlertTriangle, RefreshCw, Calendar, Globe, Play, ChevronRight, ChevronLeft, Users } from 'lucide-react';
import { detailLink, siteChineseName } from '../utils/navigate';
import { proxyImg } from '../utils/imageProxy';
import { stripHtml } from '../utils/text';

function normalizeSubType(tn: string): string { return tn.replace(/[片剧]$/, ''); }

function mapTypeToCategory(typeName: string): { category: string; subType: string } | null {
  const tn = typeName || '';
  if (/电影|动作|喜剧|爱情|科幻|恐怖|剧情|悬疑|犯罪|战争|纪录|动画片/.test(tn)) return { category: 'movie', subType: normalizeSubType(tn) };
  if (/剧|国产|港台|欧美|日韩|泰国|海外/.test(tn)) return { category: 'tv', subType: normalizeSubType(tn) };
  if (/综艺/.test(tn)) return { category: 'variety', subType: normalizeSubType(tn) };
  if (/动漫|动画/.test(tn)) return { category: 'anime', subType: normalizeSubType(tn) };
  if (/短剧/.test(tn)) return { category: 'short-drama', subType: normalizeSubType(tn) };
  if (/体育/.test(tn)) return { category: 'sports', subType: normalizeSubType(tn) };
  return null;
}

function normalizeName(name: string): string { return (name || '').substring(0, 15).replace(/\s+/g, ''); }

function NowPlayingBars({ className = '' }: { className?: string }) {
  return (
    <span className={`flex h-4 w-5 items-end justify-center gap-0.5 ${className}`} aria-hidden>
      <span className="w-1 rounded-full bg-white animate-now-playing-bar" style={{ animationDelay: '0ms' }} />
      <span className="w-1 rounded-full bg-white animate-now-playing-bar" style={{ animationDelay: '150ms' }} />
      <span className="w-1 rounded-full bg-white animate-now-playing-bar" style={{ animationDelay: '300ms' }} />
    </span>
  );
}

/**
 * 集数列表（分段 Tab 分页，默认每段 50 条）
 * - 显示 ep.title：纯数字（如"1"/"第01集"）与文字标题（如"20241001回顾1"）自动适配
 * - 超过 pageSize 时顶部显示分段 Tab（1-50 / 51-100 / ...），点击切换
 * - 当前播放集变化时自动选中其所在段（历史续播 / 切集后定位）
 */
function EpisodesList({
  episodes, activeIndex, onSelect, pageSize = 50,
}: {
  episodes: { title: string; url: string }[];
  activeIndex: number;
  onSelect: (index: number) => void;
  pageSize?: number;
}) {
  const [page, setPage] = useState(0);
  // 仅依赖 activeIndex 同步翻页（不依赖 episodes 引用，避免 re-render 时引用变化导致误 reset）：
  //  - 有效集数（>=0）：翻到该集所在页（历史续播 / 切集后自动定位）
  //  - 无当前集（<0，如查看非播放线路）：回到第 0 页
  // 用户手动翻页时 activeIndex 不变，本 effect 不触发，浏览位置得以保持
  useEffect(() => {
    setPage(activeIndex < 0 ? 0 : Math.floor(activeIndex / pageSize));
  }, [activeIndex, pageSize]);

  const totalPages = Math.ceil(episodes.length / pageSize);
  const start = page * pageSize;
  const end = Math.min(start + pageSize, episodes.length);
  const visible = episodes.slice(start, end);
  const showPager = episodes.length > pageSize;
  // 标题全是"第N集"等短数字格式时用 5 列等宽网格，保证每段布局一致；
  // 含长文字标题（如综艺"20241001回顾1"）时改用 flex-wrap 自适应避免截断
  const compact = episodes.length > 0 && episodes.every((ep) => /^第?\d{1,4}集?$/.test(ep.title.trim()));

  return (
    <div className="space-y-2">
      {/* 分段 Tab：1-50 / 51-100 / ... */}
      {showPager && (
        <div className="flex flex-wrap gap-1">
          {Array.from({ length: totalPages }).map((_, p) => {
            const s = p * pageSize;
            const e = Math.min(s + pageSize, episodes.length);
            return (
              <button
                key={p}
                onClick={() => setPage(p)}
                className={`px-2 py-0.5 rounded text-[11px] transition-all border ${
                  p === page
                    ? 'bg-brand-500/15 text-brand-400 border-brand-500/20'
                    : 'bg-white/[0.04] text-zinc-500 hover:bg-white/[0.08] hover:text-zinc-300 border-white/[0.03]'
                }`}
              >
                {s + 1}-{e}
              </button>
            );
          })}
        </div>
      )}
      {/* 集数按钮 */}
      <div className={`${compact ? 'grid grid-cols-5' : 'flex flex-wrap'} gap-1.5 max-h-72 overflow-y-auto custom-scrollbar p-0.5`}>
        {visible.map((ep, i) => {
          const realIdx = start + i;
          const active = realIdx === activeIndex;
          return (
            <button
              key={realIdx}
              onClick={() => onSelect(realIdx)}
              title={ep.title}
              className={`relative h-8 px-1.5 rounded-lg text-[11px] font-medium transition-all ${
                active
                  ? 'bg-brand-500 text-white shadow shadow-brand-500/20'
                  : 'bg-white/[0.04] text-zinc-400 hover:bg-white/[0.10] hover:text-zinc-200 border border-white/[0.03]'
              }`}
            >
              <span className={`block max-w-[10rem] truncate ${active ? 'opacity-20' : ''}`}>{ep.title}</span>
              {active && <NowPlayingBars className="absolute right-1.5 top-1/2 -translate-y-1/2" />}
            </button>
          );
        })}
      </div>
    </div>
  );
}

export default function PlayerPage() {
  const { siteKey, id } = useParams<{ siteKey?: string; id?: string }>();
  const [searchParams] = useSearchParams();
  const srcIdx = parseInt(searchParams.get('src') || '0', 10);
  const epIdx = parseInt(searchParams.get('ep') || '0', 10);
  const keysParam = searchParams.get('keys') || '';
  const navigate = useNavigate();

  const [detail, setDetail] = useState<MediaDetail | null>(null);
  const [loaded, setLoaded] = useState(false);

  const [relatedVideos, setRelatedVideos] = useState<MediaItem[]>([]);
  const [relatedLoading, setRelatedLoading] = useState(false);
  const [relatedError, setRelatedError] = useState('');
  const relatedAbortRef = useRef<AbortController | null>(null);

  const { entries, addToHistory, updatePlayPosition, setEpisodeContext } = useHistoryStore();
  const [multiSources, setMultiSources] = useState<{ siteKey: string; siteName: string; vodId: string; sources: ReturnType<typeof parsePlaySources> }[]>([]);
  const [activeTabSite, setActiveTabSite] = useState<string>(siteKey || '');
  const [activeTabSource, setActiveTabSource] = useState(srcIdx);

  // 历史续播或切换播放线路后，选集面板应展示当前实际播放的线路。
  useEffect(() => {
    setActiveTabSite(siteKey || '');
    setActiveTabSource(srcIdx);
  }, [siteKey, srcIdx]);

  const [sidebarOpen, setSidebarOpen] = useState(true);

  const [useProxy, setUseProxy] = useState(false);
  const [videoError, setVideoError] = useState('');
  const [retryCount, setRetryCount] = useState(0);
  const MAX_RETRIES = 20;

  // ---- 数据加载 ----
  useEffect(() => {
    if (!siteKey || !id) return;
    const loadAll = async () => {
      const d = await getDetail(siteKey, id);
      if (d) {
        setDetail(d);
        if (d.vod_name && keysParam) {
          getMultiDetail(d.vod_name, keysParam).then((multiDetails: MultiDetailItem[]) => {
            setMultiSources(multiDetails.map(md => ({ siteKey: md.site_key, siteName: md.site_name, vodId: md.vod_id, sources: parsePlaySources(md.vod_play_from || '', md.vod_play_url || '', md.site_key) })));
          }).catch(() => {});
        }
      }
      setLoaded(true);
    };
    loadAll();
  }, [siteKey, id, keysParam]);

  // ---- 推荐 ----
  useEffect(() => {
    if (!detail) return;
    relatedAbortRef.current?.abort();
    const controller = new AbortController();
    relatedAbortRef.current = controller;
    setRelatedLoading(true);
    setRelatedError('');
    const currentName = normalizeName(detail.vod_name || '');
    const loadRelated = async () => {
      const mapping = mapTypeToCategory(detail.type_name || '');
      if (mapping) {
        try {
          const res = await getCategory(mapping.category, detail.vod_name?.substring(0, 6) || '', 1, 12, controller.signal, mapping.subType);
          const filtered = res.list.filter(v => normalizeName(v.vod_name || '') !== currentName && v.vod_id !== detail.vod_id);
          if (filtered.length > 0) { setRelatedVideos(filtered.slice(0, 12)); setRelatedLoading(false); return; }
        } catch (e: any) { if (e.name === 'AbortError') return; }
      }
      const firstActor = (detail.vod_actor || '').split(/[,，、\s]+/).filter(Boolean)[0];
      if (firstActor) {
        try { setRelatedVideos((await searchMedia(firstActor)).filter(v => normalizeName(v.vod_name || '') !== currentName && v.vod_id !== detail.vod_id).slice(0, 12)); }
        catch { setRelatedError('推荐加载失败'); }
      }
      setRelatedLoading(false);
    };
    loadRelated();
    return () => controller.abort();
  }, [detail]);

  const handleVideoError = (error: string) => { if (!useProxy) { setUseProxy(true); setVideoError(''); return; } setVideoError(error); };
  const handleRetry = () => { if (retryCount >= MAX_RETRIES) return; setRetryCount(c => c + 1); setVideoError(''); setUseProxy(p => !p); };

  const sources = useMemo(() => detail ? parsePlaySources(detail.vod_play_from || '', detail.vod_play_url || '', siteKey) : [], [detail, siteKey]);
  const currentSource = sources[srcIdx];
  const episodes = currentSource?.episodes.map((ep, i) => ({ title: ep.title, url: ep.url, index: i })) || [];
  const currentEp = episodes[epIdx];
  const playUrl = currentEp?.url || '';
  const finalUrl = useProxy ? `http://localhost:3000/api/proxy?url=${encodeURIComponent(playUrl)}` : playUrl;

  const historyItem = useMemo<MediaItem | null>(() => {
    if (!detail) return null;
    return { vod_id: detail.vod_id, vod_name: detail.vod_name, vod_pic: detail.vod_pic, type_name: detail.type_name, vod_remarks: detail.vod_remarks, site_key: siteKey || '', site_name: '', latency: 0 } as MediaItem;
  }, [detail, siteKey]);

  const historyRecordedRef = useRef(false);
  useEffect(() => {
    if (loaded && playUrl && historyItem && !historyRecordedRef.current) {
      historyRecordedRef.current = true;
      addToHistory(historyItem, { sourceIndex: srcIdx, episodeIndex: epIdx, episodeTitle: currentEp?.title });
    }
    return () => { historyRecordedRef.current = false; };
  }, [loaded, playUrl, historyItem, addToHistory, srcIdx, epIdx, currentEp?.title]);

  const handleTimeUpdate = useCallback((time: number, duration: number) => {
    if (historyItem) updatePlayPosition(historyItem, time, duration, { sourceIndex: srcIdx, episodeIndex: epIdx, episodeTitle: currentEp?.title });
  }, [historyItem, updatePlayPosition, srcIdx, epIdx, currentEp?.title]);

  // 切换集数/线路时立即同步历史上下文（不等播放位置的 throttle）
  useEffect(() => {
    if (!historyItem) return;
    setEpisodeContext(historyItem, srcIdx, epIdx, currentEp?.title);
  }, [historyItem, srcIdx, epIdx, currentEp?.title, setEpisodeContext]);

  const actors = detail?.vod_actor ? detail.vod_actor.split(/[,，、\s]+/).filter(Boolean).slice(0, 6) : [];
  // 从历史记录恢复播放位置：仅当历史 entry 的集数/线路与当前完全一致时才恢复
  const historyEntry = entries.find(
    (e) => `${e.item.site_key || ''}_${e.item.vod_id || ''}` === `${siteKey || ''}_${id || ''}`
  );
  const savedPosition =
    historyEntry &&
    (historyEntry.episodeIndex ?? 0) === epIdx &&
    (historyEntry.sourceIndex ?? 0) === srcIdx &&
    historyEntry.playPosition > 0
      ? historyEntry.playPosition
      : 0;
  const showEpisodes = episodes.length > 1 || multiSources.length > 0;

  if (!loaded) {
    return (<div className="h-full flex items-center justify-center"><div className="w-10 h-10 border-2 border-brand-500 border-t-transparent rounded-full animate-spin" /></div>);
  }
  if (!currentEp?.url) {
    return (
      <div className="h-full flex flex-col items-center justify-center text-zinc-500 gap-4">
        <AlertTriangle size={40} className="opacity-30" /><p className="text-lg">暂无可播放的源</p>
        <button onClick={() => navigate(-1)} className="text-brand-400 hover:underline text-sm">返回详情页选择剧集</button>
      </div>
    );
  }

  // ================================================================
  // 播放页（Layout 内：左播放器 + 右玻璃侧栏）
  // ================================================================
  return (
    <div className="-m-6 h-[calc(100vh-64px)] bg-zinc-950 flex overflow-hidden">
      {/* ====== 播放器 ====== */}
      <div className="flex-1 min-w-0 relative bg-black">
        {videoError && (
          <div className="absolute inset-0 z-40 bg-black/90 backdrop-blur-sm flex flex-col items-center justify-center gap-4">
            <div className="w-16 h-16 rounded-full bg-red-500/10 flex items-center justify-center"><AlertTriangle size={32} className="text-red-400" /></div>
            <div className="text-center"><p className="text-white text-lg mb-1">播放失败</p><p className="text-zinc-500 text-sm max-w-md">{videoError}</p></div>
            <button onClick={handleRetry} disabled={retryCount >= MAX_RETRIES}
              className="flex items-center gap-2 px-6 py-2.5 bg-brand-500 hover:bg-brand-600 disabled:opacity-50 rounded-xl font-medium transition-colors">
              <RefreshCw size={18} />{retryCount >= MAX_RETRIES ? '已达最大重试次数' : `重试 (${retryCount}/${MAX_RETRIES})`}
            </button>
          </div>
        )}

        <VideoPlayer
          key={`${useProxy ? 'proxy' : 'direct'}-${retryCount}-${srcIdx}-${epIdx}`}
          src={finalUrl} title={detail?.vod_name}
          sources={sources.map((s, i) => ({ name: s.name, url: s.episodes[0]?.url || '' }))}
          episodes={episodes} currentEpisodeIndex={epIdx} currentSourceIndex={srcIdx} savedPosition={savedPosition}
          onBack={() => navigate(-1)}
          onSourceChange={(url) => { const si = sources.findIndex(s => s.episodes.some(e => e.url === url)); if (si >= 0) { const p = new URLSearchParams(searchParams); p.set('src', String(si)); p.set('ep', '0'); navigate(`/player/${siteKey}/${id}?${p}`, { replace: true }); } }}
          onEpisodeChange={(ei) => { const p = new URLSearchParams(searchParams); p.set('src', String(srcIdx)); p.set('ep', String(ei)); navigate(`/player/${siteKey}/${id}?${p}`, { replace: true }); }}
          onError={handleVideoError} onTimeUpdate={handleTimeUpdate}
        />
      </div>

      {/* ====== 右侧玻璃侧栏 ====== */}
      <div className={`relative shrink-0 group/sidebar`}>
        {/* 收起/展开按钮（统一位置：侧栏左边缘中部） */}
        <div className="absolute left-0 top-1/2 -translate-y-1/2 -translate-x-full z-50">
          {/* 收起 */}
          <button onClick={() => setSidebarOpen(false)}
            className={`w-7 h-14 flex items-center justify-center bg-zinc-900/60 hover:bg-zinc-800/80 backdrop-blur-xl border border-white/[0.06] hover:border-white/[0.14] rounded-l-lg transition-all duration-300 ${sidebarOpen ? 'opacity-0 group-hover/sidebar:opacity-100' : 'opacity-0 pointer-events-none'}`}
            title="收起侧栏">
            <ChevronRight size={14} className="text-white/50 group-hover/sidebar:text-white/80 transition-colors" />
          </button>
          {/* 展开（同一位置） */}
          <button onClick={() => setSidebarOpen(true)}
            className={`absolute inset-0 flex items-center justify-center bg-zinc-900/60 hover:bg-zinc-800/80 backdrop-blur-xl border border-white/[0.06] hover:border-white/[0.14] rounded-l-lg transition-all duration-300 ${sidebarOpen ? 'opacity-0 pointer-events-none' : 'opacity-100'}`}
            title="展开侧栏">
            <ChevronLeft size={14} className="text-white/50 hover:text-white/80 transition-colors" />
          </button>
        </div>

        {/* 侧栏本体 */}
        <div className={`h-full transition-all duration-500 ease-[cubic-bezier(0.4,0,0.2,1)] overflow-hidden ${sidebarOpen ? 'w-80 lg:w-96' : 'w-0'}`}>
          <div className="h-full w-80 lg:w-96 bg-zinc-950/80 backdrop-blur-3xl border-l border-white/[0.06] flex flex-col shadow-2xl shadow-black/40">

          {/* ====== 内容 ====== */}
          <div className="flex-1 overflow-y-auto custom-scrollbar p-4 space-y-5">

            {/* 封面图 + 标题 */}
            {detail && (
              <>
                <div className="relative rounded-2xl overflow-hidden">
                  {detail.vod_pic ? (
                    <img src={proxyImg(detail.vod_pic)} alt={detail.vod_name}
                      className="w-full aspect-[2/1] object-cover"
                      onError={(e) => { (e.target as HTMLImageElement).style.display = 'none'; }} />
                  ) : (
                    <div className="w-full aspect-[2/1] bg-gradient-to-br from-brand-500/20 to-purple-500/20 flex items-center justify-center">
                      <Play size={32} className="text-white/20" />
                    </div>
                  )}
                  <div className="absolute inset-0 bg-gradient-to-t from-zinc-950/80 via-transparent to-transparent pointer-events-none" />
                  <div className="absolute bottom-0 left-0 right-0 p-4">
                    <h2 className="text-white text-base font-bold leading-snug drop-shadow-lg">{detail.vod_name}</h2>
                  </div>
                </div>

                {/* 元数据 */}
                <div className="flex flex-wrap gap-1.5">
                  {detail.vod_year && <span className="flex items-center gap-1 px-2 py-0.5 bg-white/[0.04] backdrop-blur-sm border border-white/[0.04] rounded-lg text-zinc-400 text-xs"><Calendar size={11} />{detail.vod_year}</span>}
                  {detail.vod_area && <span className="flex items-center gap-1 px-2 py-0.5 bg-white/[0.04] backdrop-blur-sm border border-white/[0.04] rounded-lg text-zinc-400 text-xs"><Globe size={11} />{detail.vod_area}</span>}
                  {detail.type_name && <span className="px-2 py-0.5 bg-brand-500/15 backdrop-blur-sm border border-brand-500/15 rounded-lg text-brand-400 text-xs font-medium">{detail.type_name}</span>}
                  {detail.vod_remarks && <span className="px-2 py-0.5 bg-white/[0.04] backdrop-blur-sm border border-white/[0.04] rounded-lg text-zinc-500 text-xs">{detail.vod_remarks}</span>}
                  <span className="px-2 py-0.5 bg-white/[0.04] backdrop-blur-sm border border-white/[0.04] rounded-lg text-zinc-600 text-xs">{siteChineseName(siteKey || '')}</span>
                </div>

                {/* 简介 */}
                {detail.vod_content && (
                  <div className="bg-white/[0.03] backdrop-blur-md border border-white/[0.05] rounded-xl p-3">
                    <p className="text-zinc-400 text-xs leading-relaxed">{stripHtml(detail.vod_content)}</p>
                  </div>
                )}

                {/* 演员 */}
                {actors.length > 0 && (
                  <div className="bg-white/[0.03] backdrop-blur-md border border-white/[0.05] rounded-xl p-3 space-y-2">
                    <div className="flex items-center gap-1.5 text-zinc-500 text-[11px]"><Users size={12} /><span>主演</span></div>
                    <div className="flex flex-wrap gap-1.5">
                      {actors.map((a, i) => (
                        <button key={i} onClick={() => navigate(`/search?wd=${encodeURIComponent(a)}`)}
                          className="px-2.5 py-1 bg-white/[0.04] hover:bg-white/[0.10] border border-white/[0.04] hover:border-white/[0.08] text-zinc-400 hover:text-white rounded-lg text-xs transition-all">{a}</button>
                      ))}
                    </div>
                  </div>
                )}
              </>
            )}

            {/* 选集 */}
            {showEpisodes && (
              <div className="bg-white/[0.03] backdrop-blur-md border border-white/[0.05] rounded-xl p-3 space-y-3">
                <p className="text-zinc-400 text-xs font-medium">选集</p>
                <div className="flex flex-wrap gap-1">
                  {sources.length > 0 && (
                    <button onClick={() => { setActiveTabSite(siteKey || ''); setActiveTabSource(srcIdx); }}
                      className={`px-2.5 py-1 rounded-lg text-xs transition-all ${activeTabSite === (siteKey || '') ? 'bg-brand-500 text-white shadow shadow-brand-500/20' : 'bg-white/[0.04] text-zinc-400 hover:bg-white/[0.08]'}`}>{siteChineseName(siteKey || '')}</button>
                  )}
                  {multiSources.filter(ms => ms.siteKey !== siteKey).map(ms => (
                    <button key={ms.siteKey} onClick={() => { setActiveTabSite(ms.siteKey); setActiveTabSource(0); }}
                      className={`px-2.5 py-1 rounded-lg text-xs transition-all ${activeTabSite === ms.siteKey ? 'bg-brand-500 text-white shadow shadow-brand-500/20' : 'bg-white/[0.04] text-zinc-400 hover:bg-white/[0.08]'}`}>{siteChineseName(ms.siteKey)}</button>
                  ))}
                </div>
                {activeTabSite === (siteKey || '') && (() => {
                  const si = Math.min(activeTabSource, sources.length - 1);
                  const s = sources[si];
                  if (!s) return null;
                  return (
                    <>
                      {sources.length > 1 && (
                        <div className="flex flex-wrap gap-1">
                          {sources.map((src, i) => (
                            <button key={i} onClick={() => setActiveTabSource(i)}
                              className={`px-2 py-1 rounded-lg text-[11px] transition-all ${i === si ? 'bg-brand-500/15 text-brand-400 border border-brand-500/20' : 'bg-white/[0.04] text-zinc-500 hover:text-zinc-300'}`}>{src.name}</button>
                          ))}
                        </div>
                      )}
                      <EpisodesList
                        episodes={s.episodes}
                        activeIndex={si === srcIdx ? epIdx : -1}
                        onSelect={(ei) => { const p = new URLSearchParams(searchParams); p.set('src', String(si)); p.set('ep', String(ei)); navigate(`/player/${siteKey}/${id}?${p}`, { replace: true }); }}
                      />
                    </>
                  );
                })()}
                {activeTabSite !== (siteKey || '') && (() => {
                  const ms = multiSources.find(m => m.siteKey === activeTabSite);
                  if (!ms) return null;
                  const si = Math.min(activeTabSource, ms.sources.length - 1);
                  const s = ms.sources[si];
                  if (!s) return null;
                  return (
                    <>
                      {ms.sources.length > 1 && (
                        <div className="flex flex-wrap gap-1">
                          {ms.sources.map((src, i) => (
                            <button key={i} onClick={() => setActiveTabSource(i)}
                              className={`px-2 py-1 rounded-lg text-[11px] transition-all ${i === si ? 'bg-brand-500/15 text-brand-400 border border-brand-500/20' : 'bg-white/[0.04] text-zinc-500'}`}>{src.name}</button>
                          ))}
                        </div>
                      )}
                      <EpisodesList
                        episodes={s.episodes}
                        activeIndex={-1}
                        onSelect={(ei) => navigate(`/player/${ms.siteKey}/${ms.vodId}?src=${si}&ep=${ei}&keys=${encodeURIComponent(keysParam)}`, { replace: true })}
                      />
                    </>
                  );
                })()}
              </div>
            )}

            {/* ====== 相关推荐（横向网格，允许 wrap） ====== */}
            <div className="space-y-3">
              <p className="text-zinc-400 text-xs font-medium px-1">相关推荐</p>

              {relatedLoading && (
                <div className="grid grid-cols-2 gap-2">
                  {[1, 2, 3, 4].map(i => (
                    <div key={i} className="animate-pulse">
                      <div className="aspect-[2/3] bg-white/[0.04] rounded-xl" />
                      <div className="h-2.5 bg-white/[0.04] rounded mt-1.5 w-3/4" />
                      <div className="h-2 bg-white/[0.03] rounded mt-1 w-1/2" />
                    </div>
                  ))}
                </div>
              )}

              {!relatedLoading && relatedError && (
                <div className="text-center py-6">
                  <p className="text-zinc-500 text-xs mb-2">{relatedError}</p>
                  <button onClick={() => setDetail(d => d ? { ...d } : null)} className="px-3 py-1 bg-white/[0.05] hover:bg-white/[0.10] border border-white/[0.05] text-zinc-400 rounded-lg text-xs transition-all">重试</button>
                </div>
              )}

              {!relatedLoading && !relatedError && relatedVideos.length === 0 && (
                <p className="text-zinc-600 text-xs py-6 text-center">暂无推荐</p>
              )}

              {!relatedLoading && !relatedError && (
                <div className="grid grid-cols-2 gap-2">
                  {relatedVideos.map((v, i) => (
                    <button key={v.vod_id + i} onClick={() => navigate(detailLink(v))}
                      className="group rounded-xl overflow-hidden bg-white/[0.02] hover:bg-white/[0.06] backdrop-blur-md border border-white/[0.03] hover:border-white/[0.08] transition-all duration-300 hover:shadow-lg hover:shadow-white/5 active:scale-[0.98] text-left">
                      {/* 海报 */}
                      <div className="aspect-[2/3] relative overflow-hidden">
                        {v.vod_pic ? (
                          <img src={proxyImg(v.vod_pic)} alt={v.vod_name}
                            className="w-full h-full object-cover group-hover:scale-110 transition-transform duration-500"
                            loading="lazy" onError={(e) => { (e.target as HTMLImageElement).style.display = 'none'; }} />
                        ) : (
                          <div className="w-full h-full flex items-center justify-center bg-white/[0.03]"><Play size={16} className="text-zinc-600" /></div>
                        )}
                        <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent pointer-events-none" />
                        {v.vod_remarks && (
                          <span className="absolute top-1.5 left-1.5 px-1.5 py-0.5 bg-black/70 backdrop-blur-md text-white/80 text-[10px] rounded-md">{v.vod_remarks}</span>
                        )}
                        <div className="absolute inset-0 bg-black/0 group-hover:bg-black/20 transition-colors flex items-center justify-center">
                          <Play size={18} className="text-white opacity-0 group-hover:opacity-100 transition-opacity" fill="white" />
                        </div>
                      </div>
                      {/* 标题 */}
                      <div className="p-2">
                        <p className="text-zinc-300 text-xs leading-snug line-clamp-2 group-hover:text-white transition-colors">{v.vod_name}</p>
                        <p className="text-zinc-600 text-[10px] mt-1 truncate">{v.type_name || siteChineseName(v.site_key)}</p>
                      </div>
                    </button>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
        </div>
      </div>
    </div>
  );
}
