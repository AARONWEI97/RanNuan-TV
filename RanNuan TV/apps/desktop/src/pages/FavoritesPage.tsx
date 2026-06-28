import { useNavigate } from 'react-router-dom';
import { useFavoriteStore } from 'shared';
import MediaGrid from '../components/media/MediaGrid';
import { Heart, Trash2 } from 'lucide-react';
import { useState } from 'react';
import { detailLink } from '../utils/navigate';

export default function FavoritesPage() {
  const { items, clearFavorites } = useFavoriteStore();
  const navigate = useNavigate();
  const [showClear, setShowClear] = useState(false);

  return (
    <div className="space-y-6">
      {/* 头部 */}
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-semibold">❤️ 我的收藏</h2>
        {items.length > 0 && (
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
          <span className="text-sm text-zinc-400">确定要清空所有收藏吗？此操作不可撤销。</span>
          <button
            onClick={() => { clearFavorites(); setShowClear(false); }}
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
      {items.length > 0 && (
        <p className="text-sm text-zinc-500">共 {items.length} 部影片</p>
      )}

      {/* 空状态 */}
      {items.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-24 text-zinc-500 gap-4">
          <div className="w-20 h-20 rounded-full bg-zinc-900 flex items-center justify-center">
            <Heart size={32} className="opacity-30" />
          </div>
          <div className="text-center">
            <p className="text-lg mb-1">还没有收藏任何影片</p>
            <p className="text-sm text-zinc-600">浏览影片时点击 ❤️ 即可收藏</p>
          </div>
          <button
            onClick={() => navigate('/')}
            className="px-6 py-2 bg-brand-500 hover:bg-brand-600 rounded-xl text-sm font-medium transition-colors"
          >
            去首页逛逛
          </button>
        </div>
      ) : (
        <MediaGrid
          items={items}
          onClick={(item) => navigate(detailLink(item))}
        />
      )}
    </div>
  );
}
