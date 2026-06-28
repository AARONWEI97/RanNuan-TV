import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { MediaItem } from '../types';

// ==================== 播放器状态 ====================
interface PlayerState {
  isPlaying: boolean;
  currentSrc: string;
  currentTitle: string;
  currentTime: number;
  duration: number;
  volume: number;
  playbackRate: number;
  isFullscreen: boolean;
  selectedSource: string | null;
  play: (src: string, title: string) => void;
  pause: () => void;
  seek: (time: number) => void;
  setVolume: (v: number) => void;
  setPlaybackRate: (rate: number) => void;
  setDuration: (d: number) => void;
  setCurrentTime: (t: number) => void;
  toggleFullscreen: () => void;
}

export const usePlayerStore = create<PlayerState>((set) => ({
  isPlaying: false,
  currentSrc: '',
  currentTitle: '',
  currentTime: 0,
  duration: 0,
  volume: 1,
  playbackRate: 1,
  isFullscreen: false,
  selectedSource: null,
  play: (src, title) => set({ currentSrc: src, currentTitle: title, isPlaying: true }),
  pause: () => set({ isPlaying: false }),
  seek: (time) => set({ currentTime: time }),
  setVolume: (v) => set({ volume: v }),
  setPlaybackRate: (rate) => set({ playbackRate: rate }),
  setDuration: (d) => set({ duration: d }),
  setCurrentTime: (t) => set({ currentTime: t }),
  toggleFullscreen: () => set((s) => ({ isFullscreen: !s.isFullscreen })),
}));

// ==================== 搜索状态 ====================
interface SearchState {
  query: string;
  results: MediaItem[];
  isLoading: boolean;
  error: string | null;
  setQuery: (q: string) => void;
  setResults: (r: MediaItem[]) => void;
  setLoading: (l: boolean) => void;
  setError: (e: string | null) => void;
}

export const useSearchStore = create<SearchState>((set) => ({
  query: '',
  results: [],
  isLoading: false,
  error: null,
  setQuery: (q) => set({ query: q }),
  setResults: (r) => set({ results: r }),
  setLoading: (l) => set({ isLoading: l }),
  setError: (e) => set({ error: e }),
}));

// ==================== 收藏状态（持久化 + 复合键） ====================
interface FavoriteState {
  items: MediaItem[];
  addToFavorites: (item: MediaItem) => void;
  removeFromFavorites: (item: MediaItem) => void;
  isFavorite: (item: MediaItem) => boolean;
  clearFavorites: () => void;
}

/** 收藏用复合键：vod_id + site_key，避免跨站 ID 碰撞 */
function favKey(item: MediaItem): string {
  return `${item.site_key || ''}_${item.vod_id || ''}`;
}

export const useFavoriteStore = create<FavoriteState>()(
  persist(
    (set, get) => ({
      items: [],
      addToFavorites: (item) =>
        set((s) => {
          const key = favKey(item);
          const filtered = s.items.filter((i) => favKey(i) !== key);
          return { items: [...filtered, item] };
        }),
      removeFromFavorites: (item) =>
        set((s) => {
          const key = favKey(item);
          return { items: s.items.filter((i) => favKey(i) !== key) };
        }),
      isFavorite: (item) => {
        const key = favKey(item);
        return get().items.some((i) => favKey(i) === key);
      },
      clearFavorites: () => set({ items: [] }),
    }),
    { name: 'rannuan-favorites' },
  ),
);

// ==================== 观看历史（持久化，最多 100 条，记录播放位置） ====================
export interface HistoryEntry {
  item: MediaItem;
  watchedAt: string;    // ISO 8601，首次播放时间
  playPosition: number; // 秒，最后播放位置
  duration: number;     // 秒，视频总时长
}

interface HistoryState {
  entries: HistoryEntry[];
  addToHistory: (item: MediaItem) => void;
  removeFromHistory: (item: MediaItem) => void;
  clearHistory: () => void;
  /** 更新播放位置（播放中周期性调用） */
  updatePlayPosition: (item: MediaItem, position: number, dur: number) => void;
}

const MAX_HISTORY = 100;

export const useHistoryStore = create<HistoryState>()(
  persist(
    (set, get) => ({
      entries: [],
      addToHistory: (item) =>
        set((s) => {
          const key = favKey(item);
          const filtered = s.entries.filter((e) => favKey(e.item) !== key);
          const newEntries: HistoryEntry[] = [
            { item, watchedAt: new Date().toISOString(), playPosition: 0, duration: 0 },
            ...filtered,
          ];
          if (newEntries.length > MAX_HISTORY) newEntries.length = MAX_HISTORY;
          return { entries: newEntries };
        }),
      removeFromHistory: (item) =>
        set((s) => {
          const key = favKey(item);
          return { entries: s.entries.filter((e) => favKey(e.item) !== key) };
        }),
      clearHistory: () => set({ entries: [] }),
      updatePlayPosition: (item, position, dur) =>
        set((s) => {
          const key = favKey(item);
          return {
            entries: s.entries.map((e) =>
              favKey(e.item) === key
                ? { ...e, playPosition: Math.floor(position), duration: Math.floor(dur) }
                : e,
            ),
          };
        }),
    }),
    { name: 'rannuan-history' },
  ),
);
