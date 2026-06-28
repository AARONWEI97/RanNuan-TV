import { useState, useEffect } from 'react';
import type { MediaItem } from 'shared';
import { useFavoriteStore } from 'shared';
import { Heart } from 'lucide-react';
import { proxyImg } from '../../utils/imageProxy';
import { itemSiteName } from '../../utils/navigate';

interface Props {
  item: MediaItem;
  onClick: () => void;
}

const COLORS = [
  'from-brand-500 to-orange-500',
  'from-blue-500 to-cyan-500',
  'from-purple-500 to-pink-500',
  'from-green-500 to-emerald-500',
  'from-red-500 to-rose-500',
  'from-yellow-500 to-amber-500',
];

export default function MediaCard({ item, onClick }: Props) {
  const [imgError, setImgError] = useState(false);
  const [lazyPic, setLazyPic] = useState(item.vod_pic || '');
  const { isFavorite, addToFavorites, removeFromFavorites } = useFavoriteStore();
  const favorited = isFavorite(item);
  const colorIdx = (item.vod_name || '').length % COLORS.length;
  const showImg = !!lazyPic && !imgError;

  // 懒加载图片：列表接口可能没返回 vod_pic，客户端异步查详情获取
  useEffect(() => {
    if (lazyPic) return; // 已有图，不需要加载
    if (!item.site_key || !item.vod_id) return;
    
    let cancelled = false;
    fetch(`http://localhost:3000/api/thumbnail?site_key=${item.site_key}&id=${item.vod_id}`)
      .then(r => r.json())
      .then(data => {
        if (!cancelled && data.vod_pic) {
          setLazyPic(data.vod_pic);
        }
      })
      .catch(() => {});
    
    return () => { cancelled = true; };
  }, [lazyPic, item.site_key, item.vod_id, item.vod_name, item.vod_pic]);

  const toggleFav = (e: React.MouseEvent) => {
    e.stopPropagation();
    if (favorited) {
      removeFromFavorites(item);
    } else {
      addToFavorites(item);
    }
  };

  return (
    <div
      onClick={onClick}
      className="group cursor-pointer rounded-2xl overflow-hidden glass-card"
    >
      {/* 海报 */}
      <div className="relative aspect-[2/3] bg-zinc-900/50 overflow-hidden">
        {showImg ? (
          <>
            {/* 模糊背景填充 —— 消除黑边 */}
            <img
              src={proxyImg(lazyPic)}
              alt=""
              className="absolute inset-0 w-full h-full object-cover scale-110 blur-xl opacity-30"
              loading="lazy"
            />
            <img
              src={proxyImg(lazyPic)}
              alt={item.vod_name}
              className="relative w-full h-full object-cover group-hover:scale-105 transition-transform duration-500 ease-out"
              loading="lazy"
              onError={() => setImgError(true)}
            />
          </>
        ) : (
          /* 无图时的玻璃质感占位 */
          <div className="w-full h-full flex items-center justify-center relative overflow-hidden">
            <div
              className="absolute inset-0 opacity-60"
              style={{
                background: `radial-gradient(ellipse at 30% 20%, ${COLORS[colorIdx].split(' ')[1].replace('to-','')}40, transparent 60%)`,
              }}
            />
            <div className="relative z-10 flex flex-col items-center gap-2">
              <span className="text-5xl font-bold text-white/20 drop-shadow-lg">
                {item.vod_name?.charAt(0) || '?'}
              </span>
              <span className="text-[10px] text-white/10 uppercase tracking-widest">no cover</span>
            </div>
          </div>
        )}

        {/* 顶部渐变遮罩 */}
        <div className="absolute inset-x-0 top-0 h-16 bg-gradient-to-b from-black/50 to-transparent pointer-events-none" />
        {/* 底部渐变遮罩 */}
        <div className="absolute inset-x-0 bottom-0 h-24 bg-gradient-to-t from-black/80 via-black/40 to-transparent pointer-events-none" />

        {/* 备注角标 */}
        {item.vod_remarks && (
          <span className="absolute top-2.5 left-2.5 px-2 py-0.5 bg-black/50 backdrop-blur-md text-white/90 text-[10px] rounded-md font-medium border border-white/10">
            {item.vod_remarks}
          </span>
        )}

        {/* 多源角标 */}
        {item.sites && item.sites.length > 1 ? (
          <span className="absolute top-2.5 right-2.5 px-2 py-0.5 bg-cyan-500/20 backdrop-blur-md text-cyan-300 text-[10px] rounded-md font-medium border border-cyan-500/20">
            {item.sites.length}个源
          </span>
        ) : (
          <span className="absolute top-2.5 right-2.5 px-2 py-0.5 bg-zinc-700/40 backdrop-blur-md text-zinc-400 text-[10px] rounded-md font-medium border border-zinc-700/30">
            {itemSiteName(item)}
          </span>
        )}

        {/* 收藏按钮 */}
        <button
          onClick={toggleFav}
          className="absolute bottom-3 right-3 p-2 rounded-full bg-black/40 backdrop-blur-md hover:bg-black/60 opacity-0 group-hover:opacity-100 transition-all duration-300 border border-white/5"
          title={favorited ? '取消收藏' : '收藏'}
        >
          <Heart
            size={15}
            className={favorited ? 'text-red-400 fill-red-400' : 'text-white/80'}
          />
        </button>

      </div>

      {/* 标题区 */}
      <div className="p-3">
        <h3 className="text-sm font-medium text-white/90 truncate leading-tight" title={item.vod_name}>
          {item.vod_name}
        </h3>
        <p className="text-[11px] text-white/35 mt-1 truncate">
          {item.type_name || itemSiteName(item)}
        </p>
      </div>
    </div>
  );
}
