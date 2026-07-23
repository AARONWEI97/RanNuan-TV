import { useNavigate } from 'react-router-dom';
import { useHistoryStore } from 'shared';
import type { HistoryEntry } from 'shared';
import { Clock, Trash2 } from 'lucide-react';
import { useState } from 'react';
import { playerLink, itemSiteName } from '../utils/navigate';
import { proxyImg } from '../utils/imageProxy';

/** 格式化秒数为 mm:ss 或 h:mm:ss */
function formatTime(seconds: number): string {
  if (!seconds || seconds <= 0) return '00:00';
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  const s = Math.floor(seconds % 60);
  if (h > 0) return `${h}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
  return `${m}:${String(s).padStart(2, '0')}`;
}

export default function HistoryPage() {
  const { entries, clearHistory } = useHistoryStore();
  const navigate = useNavigate();
  const [showClear, setShowClear] = useState(false);

  return (
    <div className="space-y-6">
      {/* 头部 */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Clock size={24} className="text-brand-400" />
          <h2 className="text-xl font-semibold text-white/90">观看历史</h2>
        </div>
        {entries.length > 0 && (
          <button
            onClick={() => setShowClear(true)}
            className="flex items-center gap-1.5 px-3 py-1.5 text-xs text-zinc-500 hover:text-red-400 transition-colors"
          >
            <Trash2 size={14} />
            清空
          </button>
        )}
      </div>

      {/* 确认清空 */}
      {showClear && (
        <div className="flex items-center gap-4 p-4 bg-zinc-900 border border-zinc-800 rounded-xl">
          <span className="text-sm text-zinc-400">确定要清空所有历史记录吗？此操作不可撤销。</span>
          <button
            onClick={() => { clearHistory(); setShowClear(false); }}
            className="px-4 py-1.5 bg-red-500/20 text-red-400 hover:bg-red-500/30 rounded-lg text-sm transition-colors"
          >
            确认清空
          </button>
          <button
            onClick={() => setShowClear(false)}
            className="px-4 py-1.5 bg-zinc-800 text-zinc-400 hover:bg-zinc-700 rounded-lg text-sm transition-colors"
          >
            取消
          </button>
        </div>
      )}

      {/* 统计 */}
      {entries.length > 0 && (
        <p className="text-sm text-zinc-500">共 {entries.length} 条记录（最近 {MAX_DISPLAY_COUNT} 条）</p>
      )}

      {/* 空状态 */}
      {entries.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-24 text-zinc-500 gap-4">
          <div className="w-20 h-20 rounded-full bg-zinc-900 flex items-center justify-center">
            <Clock size={32} className="opacity-30" />
          </div>
          <div className="text-center">
            <p className="text-lg mb-1">暂无观看历史</p>
            <p className="text-sm text-zinc-600">开始播放视频时会自动记录</p>
          </div>
          <button
            onClick={() => navigate('/')}
            className="px-6 py-2 bg-brand-500 hover:bg-brand-600 rounded-xl text-sm font-medium transition-colors"
          >
            去首页逛逛
          </button>
        </div>
      ) : (
        /* 历史网格（含播放进度） */
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-4">
          {entries.map((entry: HistoryEntry, idx: number) => {
            const progress = entry.duration > 0
              ? Math.round((entry.playPosition / entry.duration) * 100)
              : 0;
            const hasWatched = entry.playPosition > 0;

            return (
              <div
                key={`${entry.item.site_key}_${entry.item.vod_id}_${idx}`}
                onClick={() => navigate(playerLink(entry.item, entry.sourceIndex, entry.episodeIndex))}
                className="group cursor-pointer rounded-2xl overflow-hidden glass-card transition-transform hover:scale-[1.02]"
              >
                {/* 海报 */}
                <div className="relative aspect-[2/3] bg-zinc-900/50 overflow-hidden">
                  {entry.item.vod_pic ? (
                    <>
                      <img
                        src={proxyImg(entry.item.vod_pic)}
                        alt=""
                        className="absolute inset-0 w-full h-full object-cover scale-110 blur-xl opacity-30"
                        loading="lazy"
                      />
                      <img
                        src={proxyImg(entry.item.vod_pic)}
                        alt={entry.item.vod_name}
                        className="relative w-full h-full object-cover group-hover:scale-105 transition-transform duration-500 ease-out"
                        loading="lazy"
                      />
                    </>
                  ) : (
                    <div className="w-full h-full flex items-center justify-center bg-gradient-to-br from-brand-500/20 to-orange-500/20">
                      <span className="text-5xl font-bold text-white/10">
                        {entry.item.vod_name?.charAt(0) || '?'}
                      </span>
                    </div>
                  )}

                  {/* 站点角标 */}
                  <span className="absolute top-2.5 right-2.5 px-2 py-0.5 bg-zinc-700/40 backdrop-blur-md text-zinc-400 text-[10px] rounded-md font-medium border border-zinc-700/30">
                    {itemSiteName(entry.item)}
                  </span>

                  {/* 播放进度条 */}
                  {hasWatched && (
                    <div className="absolute bottom-0 inset-x-0 h-1 bg-zinc-800">
                      <div
                        className="h-full bg-brand-500 transition-all"
                        style={{ width: `${Math.min(progress, 100)}%` }}
                      />
                    </div>
                  )}
                </div>

                {/* 标题 + 进度信息 */}
                <div className="p-3">
                  <h3 className="text-sm font-medium text-white/90 truncate leading-tight" title={entry.item.vod_name}>
                    {entry.item.vod_name}
                  </h3>
                  {entry.episodeTitle && (
                    <p className="text-[11px] text-zinc-400 mt-1 truncate" title={entry.episodeTitle}>
                      {entry.episodeTitle}
                    </p>
                  )}
                  {hasWatched ? (
                    <p className="text-[11px] text-brand-400/70 mt-1 truncate">
                      已看 {formatTime(entry.playPosition)}{entry.duration > 0 ? ` / ${formatTime(entry.duration)}` : ''}
                    </p>
                  ) : (
                    <p className="text-[11px] text-zinc-500 mt-1 truncate">
                      {entry.item.type_name || itemSiteName(entry.item)}
                    </p>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

const MAX_DISPLAY_COUNT = 100;
