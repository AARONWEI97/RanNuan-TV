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

// ==================== 观看历史（持久化，最多 100 条，记录播放位置 + 集数） ====================
export interface HistoryEntry {
  item: MediaItem;
  watchedAt: string;    // ISO 8601，首次播放时间
  playPosition: number; // 秒，最后播放位置
  duration: number;     // 秒，视频总时长
  /** 上次播放的源/线路索引（多线路时） */
  sourceIndex: number;
  /** 上次播放的集数索引 */
  episodeIndex: number;
  /** 上次播放的剧集名（冗余存储，历史页可直接显示，无需重新拉详情） */
  episodeTitle?: string;
}

/** 记录播放进度的可选上下文（哪一集的哪个时间点） */
export interface HistoryProgress {
  sourceIndex?: number;
  episodeIndex?: number;
  episodeTitle?: string;
}

interface HistoryState {
  entries: HistoryEntry[];
  addToHistory: (item: MediaItem, ctx?: HistoryProgress) => void;
  removeFromHistory: (item: MediaItem) => void;
  clearHistory: () => void;
  /** 更新播放位置（播放中周期性调用），可同时更新集数上下文 */
  updatePlayPosition: (item: MediaItem, position: number, dur: number, ctx?: HistoryProgress) => void;
  /** 切换集数/线路时同步上下文，并清除上一集的播放位置 */
  setEpisodeContext: (item: MediaItem, sourceIndex: number, episodeIndex: number, episodeTitle?: string) => void;
}

const MAX_HISTORY = 100;

export const useHistoryStore = create<HistoryState>()(
  persist(
    (set, get) => ({
      entries: [],
      addToHistory: (item, ctx) =>
        set((s) => {
          const key = favKey(item);
          const filtered = s.entries.filter((e) => favKey(e.item) !== key);
          const old = s.entries.find((e) => favKey(e.item) === key);
          const nextSourceIndex = ctx?.sourceIndex ?? old?.sourceIndex ?? 0;
          const nextEpisodeIndex = ctx?.episodeIndex ?? old?.episodeIndex ?? 0;
          const contextChanged = !!old && (
            (old.sourceIndex ?? 0) !== nextSourceIndex ||
            (old.episodeIndex ?? 0) !== nextEpisodeIndex
          );
          const newEntries: HistoryEntry[] = [
            {
              item,
              watchedAt: new Date().toISOString(),
              playPosition: contextChanged ? 0 : old?.playPosition ?? 0,
              duration: contextChanged ? 0 : old?.duration ?? 0,
              sourceIndex: nextSourceIndex,
              episodeIndex: nextEpisodeIndex,
              episodeTitle: ctx?.episodeTitle ?? old?.episodeTitle,
            },
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
      updatePlayPosition: (item, position, dur, ctx) =>
        set((s) => {
          const key = favKey(item);
          return {
            entries: s.entries.map((e) =>
              favKey(e.item) === key
                ? {
                    ...e,
                    playPosition: Math.floor(position),
                    duration: Math.floor(dur),
                    ...(ctx?.sourceIndex !== undefined && { sourceIndex: ctx.sourceIndex }),
                    ...(ctx?.episodeIndex !== undefined && { episodeIndex: ctx.episodeIndex }),
                    ...(ctx?.episodeTitle !== undefined && { episodeTitle: ctx.episodeTitle }),
                  }
                : e,
            ),
          };
        }),
      setEpisodeContext: (item, sourceIndex, episodeIndex, episodeTitle) =>
        set((s) => {
          const key = favKey(item);
          return {
            entries: s.entries.map((e) => {
              if (favKey(e.item) !== key) return e;
              const contextChanged =
                (e.sourceIndex ?? 0) !== sourceIndex ||
                (e.episodeIndex ?? 0) !== episodeIndex;
              return {
                ...e,
                ...(contextChanged && { playPosition: 0, duration: 0 }),
                sourceIndex,
                episodeIndex,
                ...(episodeTitle !== undefined && { episodeTitle }),
              };
            }),
          };
        }),
    }),
    { name: 'rannuan-history' },
  ),
);
