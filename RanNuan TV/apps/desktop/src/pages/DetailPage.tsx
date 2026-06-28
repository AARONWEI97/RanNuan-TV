import { useState, useEffect, useMemo } from 'react';
import { useParams, useNavigate, useSearchParams } from 'react-router-dom';
import { getDetail, getMultiDetail, parsePlaySources, useFavoriteStore } from 'shared';
import type { MediaDetail, MultiDetailItem, MediaItem } from 'shared';
import { Clock, Users, Calendar, Globe, Heart, Search, ArrowLeft, Server } from 'lucide-react';
import { siteChineseName } from '../utils/navigate';
import { stripHtml } from '../utils/text';

function proxyImg(rawUrl: string): string {
  if (!rawUrl) return '';
  if (rawUrl.includes('/api/img')) return rawUrl;
  if (rawUrl.includes('bfzy') || rawUrl.includes('picbf')) return `http://localhost:3000/api/img?url=${encodeURIComponent(rawUrl)}`;
  return rawUrl;
}

export default function DetailPage() {
  const { siteKey, id, name } = useParams<{ siteKey?: string; id?: string; name?: string }>();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { isFavorite, addToFavorites, removeFromFavorites } = useFavoriteStore();

  // 单源模式
  const [detail, setDetail] = useState<MediaDetail | null>(null);
  // 多源模式
  const [multiDetails, setMultiDetails] = useState<MultiDetailItem[]>([]);
  const [activeSiteIdx, setActiveSiteIdx] = useState(0);

  const [loading, setLoading] = useState(true);
  const [sourceIdx, setSourceIdx] = useState(0);
  const [episodeFilter, setEpisodeFilter] = useState('');

  const isMultiMode = !!name;

  // ---- 数据加载 ----
  useEffect(() => {
    if (isMultiMode && name) {
      const keys = searchParams.get('keys') || undefined;
      getMultiDetail(decodeURIComponent(name), keys)
        .then(setMultiDetails)
        .finally(() => setLoading(false));
    } else if (siteKey && id) {
      getDetail(siteKey, id)
        .then(setDetail)
        .finally(() => setLoading(false));
    } else {
      setLoading(false);
    }
  }, [siteKey, id, name, isMultiMode]);

  // ---- 当前显示的详情 ----
  const currentDetail: MediaDetail | null = isMultiMode
    ? (multiDetails[activeSiteIdx] || null)
    : detail;

  // ---- 当前站点 key ----
  const realSiteKey: string | undefined = isMultiMode && multiDetails[activeSiteIdx]
    ? multiDetails[activeSiteIdx].site_key
    : siteKey;

  // ---- 收藏/历史用的 MediaItem ----
  const favItem = useMemo<MediaItem | null>(() => {
    if (!currentDetail) return null;
    return {
      vod_id: currentDetail.vod_id,
      vod_name: currentDetail.vod_name,
      vod_pic: currentDetail.vod_pic,
      type_name: currentDetail.type_name,
      vod_remarks: currentDetail.vod_remarks,
      site_key: realSiteKey || '',
      site_name: multiDetails[activeSiteIdx]?.site_name || '',
      latency: 0,
    } as MediaItem;
  }, [currentDetail, realSiteKey, multiDetails, activeSiteIdx]);

  // ---- 播放源解析 ----
  const sources = currentDetail
    ? parsePlaySources(currentDetail.vod_play_from || '', currentDetail.vod_play_url || '', realSiteKey)
    : [];
  const currentSource = sources[sourceIdx];

  const filteredEpisodes = useMemo(() => {
    if (!currentSource) return [];
    if (!episodeFilter.trim()) return currentSource.episodes;
    return currentSource.episodes.filter(ep => ep.title.includes(episodeFilter.trim()));
  }, [currentSource, episodeFilter]);

  const realId = isMultiMode && multiDetails[activeSiteIdx]
    ? multiDetails[activeSiteIdx].vod_id
    : id;

  // ---- 演员列表 ----
  const actors = currentDetail?.vod_actor
    ? currentDetail.vod_actor.split(/[,，、\s]+/).filter(Boolean).slice(0, 8)
    : [];

  const toggleFav = () => {
    if (!favItem) return;
    if (isFavorite(favItem)) {
      removeFromFavorites(favItem);
    } else {
      addToFavorites(favItem);
    }
  };

  // ---- 渲染 ----
  if (loading) {
    return (
      <div className="flex gap-8 animate-pulse">
        <div className="w-64 aspect-[2/3] bg-zinc-900 rounded-xl shrink-0" />
        <div className="flex-1 space-y-4">
          <div className="h-8 w-48 bg-zinc-900 rounded" />
          <div className="h-4 w-32 bg-zinc-900 rounded" />
          <div className="h-4 w-full bg-zinc-900 rounded" />
        </div>
      </div>
    );
  }

  if (!currentDetail) {
    return (
      <div className="flex flex-col items-center justify-center py-20 text-zinc-500 gap-2">
        <p className="text-lg">未找到影片信息</p>
        <button onClick={() => navigate(-1)} className="text-brand-400 hover:underline text-sm">返回上一页</button>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {/* 返回按钮 */}
      <button
        onClick={() => navigate(-1)}
        className="flex items-center gap-2 px-3 py-1.5 -ml-3 rounded-lg text-zinc-500 hover:text-white hover:bg-zinc-800 transition-colors text-sm"
      >
        <ArrowLeft size={18} />
        <span>返回</span>
      </button>

      <div className="flex flex-col lg:flex-row gap-8">
        {/* 海报 */}
        <div className="shrink-0 mx-auto lg:mx-0">
          {currentDetail.vod_pic ? (
            <img
              src={proxyImg(currentDetail.vod_pic)}
              alt={currentDetail.vod_name}
              className="w-48 lg:w-64 rounded-xl shadow-2xl bg-zinc-800"
              onError={(e) => { (e.target as HTMLImageElement).style.display = 'none'; }}
            />
          ) : (
            <div className="w-48 lg:w-64 aspect-[2/3] rounded-xl bg-gradient-to-br from-brand-500 to-orange-500 flex items-center justify-center">
              <span className="text-5xl font-bold text-white/80">{currentDetail.vod_name?.charAt(0) || '?'}</span>
            </div>
          )}
        </div>

        {/* 信息区 */}
        <div className="flex-1 min-w-0">
          {/* 标题 + 收藏 */}
          <div className="flex items-center gap-3 mb-3">
            <h1 className="text-2xl font-bold">{currentDetail.vod_name}</h1>
            <button
              onClick={toggleFav}
              className="p-1.5 rounded-full hover:bg-zinc-800 transition-colors shrink-0"
              title={favItem && isFavorite(favItem) ? '取消收藏' : '收藏'}
            >
              <Heart
                size={20}
                className={favItem && isFavorite(favItem) ? 'text-red-500 fill-red-500' : 'text-zinc-500'}
              />
            </button>
          </div>

          {/* 元数据 */}
          <div className="flex flex-wrap gap-3 mb-4 text-sm text-zinc-400">
            {currentDetail.vod_year && (
              <span className="flex items-center gap-1"><Calendar size={14} />{currentDetail.vod_year}</span>
            )}
            {currentDetail.vod_area && (
              <span className="flex items-center gap-1"><Globe size={14} />{currentDetail.vod_area}</span>
            )}
            {currentDetail.type_name && (
              <span className="px-2 py-0.5 bg-brand-500/20 text-brand-400 rounded text-xs font-medium">
                {currentDetail.type_name}
              </span>
            )}
            {currentDetail.vod_remarks && (
              <span className="px-2 py-0.5 bg-zinc-800 text-zinc-400 rounded text-xs">
                {currentDetail.vod_remarks}
              </span>
            )}
          </div>

          {/* 演员 */}
          {actors.length > 0 && (
            <p className="text-sm text-zinc-400 mb-2 flex items-start gap-1.5 flex-wrap">
              <Users size={14} className="mt-0.5 shrink-0" />
              {actors.map((actor, i) => (
                <span key={i}>
                  <button
                    onClick={() => navigate(`/search?wd=${encodeURIComponent(actor)}`)}
                    className="hover:text-brand-400 hover:underline transition-colors"
                  >
                    {actor}
                  </button>
                  {i < actors.length - 1 && <span className="text-zinc-600"> · </span>}
                </span>
              ))}
            </p>
          )}

          {/* 简介 */}
          {currentDetail.vod_content && (
            <p className="text-sm text-zinc-500 mb-6 leading-relaxed line-clamp-4">
              {stripHtml(currentDetail.vod_content)}
            </p>
          )}

          {/* ====== 多源模式：站点选择器 ====== */}
          {isMultiMode && multiDetails.length > 1 && (
            <div className="flex flex-wrap gap-2 mb-4 p-3 bg-zinc-900/50 rounded-xl border border-zinc-800/50">
              <div className="flex items-center gap-1.5 text-zinc-500 text-xs mr-2">
                <Server size={13} />
                <span>播放站点:</span>
              </div>
              {multiDetails.map((md, i) => (
                <button
                  key={md.site_key}
                  onClick={() => { setActiveSiteIdx(i); setSourceIdx(0); setEpisodeFilter(''); }}
                  className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${
                    i === activeSiteIdx
                      ? 'bg-brand-500 text-white shadow-lg shadow-brand-500/25'
                      : 'bg-zinc-800 text-zinc-400 hover:bg-zinc-700 hover:text-zinc-200'
                  }`}
                >
                  {siteChineseName(md.site_key)}
                </button>
              ))}
            </div>
          )}

          {/* ====== 播放源切换 ====== */}
          {sources.length > 1 && (
            <div className="flex flex-wrap gap-2 mb-4">
              {sources.map((s, i) => (
                <button
                  key={i}
                  onClick={() => { setSourceIdx(i); setEpisodeFilter(''); }}
                  className={`px-4 py-1.5 rounded-lg text-sm transition-colors ${
                    i === sourceIdx
                      ? 'bg-brand-500 text-white'
                      : 'bg-zinc-800 text-zinc-400 hover:bg-zinc-700'
                  }`}
                >
                  {s.name} ({s.episodes.length}集)
                </button>
              ))}
            </div>
          )}

          {/* ====== 剧集列表 ====== */}
          {currentSource && currentSource.episodes.length > 0 && (
            <div>
              <div className="flex items-center justify-between mb-3">
                <h3 className="text-sm font-medium text-zinc-300">
                  <Clock size={14} className="inline mr-1" />
                  {currentSource.name} · {currentSource.episodes.length} 集
                </h3>
                {currentSource.episodes.length > 20 && (
                  <div className="relative">
                    <Search size={14} className="absolute left-2.5 top-1/2 -translate-y-1/2 text-zinc-500" />
                    <input
                      type="text"
                      value={episodeFilter}
                      onChange={(e) => setEpisodeFilter(e.target.value)}
                      placeholder="搜索剧集..."
                      className="w-36 h-7 pl-8 pr-2 bg-zinc-800 border border-zinc-700 rounded-lg text-xs text-white placeholder-zinc-500 focus:outline-none focus:border-zinc-600"
                    />
                  </div>
                )}
              </div>
              <div className="flex flex-wrap gap-2 max-h-64 overflow-y-auto p-1">
                {filteredEpisodes.length > 0 ? (
                  filteredEpisodes.map((ep, idx) => {
                    const realIdx = currentSource.episodes.indexOf(ep);
                    return (
                      <button
                        key={realIdx}
                        onClick={() => {
                          const keys = searchParams.get('keys') || '';
                          navigate(`/player/${realSiteKey}/${realId}?src=${sourceIdx}&ep=${realIdx}${keys ? `&keys=${encodeURIComponent(keys)}` : ''}`);
                        }}
                        className="px-3 py-1.5 bg-zinc-800 hover:bg-brand-600 rounded-lg text-xs transition-colors"
                      >
                        {ep.title}
                      </button>
                    );
                  })
                ) : (
                  <p className="text-xs text-zinc-500 py-2">未找到匹配的剧集</p>
                )}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
