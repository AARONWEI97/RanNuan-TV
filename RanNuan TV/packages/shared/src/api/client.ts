import type { MediaItem, MediaDetail, CheckResponse } from '../types';

// API 基址：生产环境(WebView/同源)用当前 origin；dev 模式(vite dev server 1420)用 localhost:3000 跨端口调 API
// 注意：Android 模拟器 WebView 里 localhost 指向模拟器自身，必须用页面 origin(10.0.2.2:3000)
const API_BASE = (typeof window !== 'undefined' && window.location.port === '3000')
  ? window.location.origin
  : 'http://localhost:3000';

async function request<T>(url: string): Promise<T> {
  const res = await fetch(url);
  if (!res.ok) throw new Error(`API Error: ${res.status}`);
  return res.json();
}

/** 聚合搜索 */
export async function searchMedia(query: string): Promise<MediaItem[]> {
  const data = await request<{ list: MediaItem[] }>(
    `${API_BASE}/api/search?wd=${encodeURIComponent(query)}`
  );
  return data.list || [];
}

// SSE 搜索结果回调函数类型
interface SearchStreamEvent {
  type: 'start' | 'videos' | 'progress' | 'complete' | 'error' | 'merged';
  videos?: MediaItem[];
  completedSources?: number;
  totalSources?: number;
  totalVideosFound?: number;
  source?: string;
  sourceName?: string;
  latency?: number;
  message?: string;
  actorLike?: boolean;
  phase?: string;
  stats?: { indexHits: number; cmsHits: number; merged: number };
  indexHits?: number;
  indexCoverage?: number;
  uniqueActors?: number;
}

type VideoCallback = (videos: MediaItem[]) => void;
type ProgressCallback = (data: { completed: number; total: number; phase?: string }) => void;
type MergedCallback = (videos: MediaItem[], stats?: { indexHits: number; cmsHits: number; merged: number }) => void;
type StartCallback = (data: { actorLike: boolean; indexCoverage: number; uniqueActors: number }) => void;

/** SSE 流式搜索 - 结果实时返回（支持 merged 合并事件） */
export async function searchMediaStream(
  query: string,
  onVideos: VideoCallback,
  onProgress: ProgressCallback,
  signal?: AbortSignal,
  onMerged?: MergedCallback,
  onStart?: StartCallback,
): Promise<MediaItem[]> {
  const allVideos: MediaItem[] = [];
  
  const response = await fetch(`${API_BASE}/api/search-stream`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ wd: query }),
    signal,
  });

  if (!response.ok) throw new Error(`SSE Error: ${response.status}`);
  if (!response.body) throw new Error('No response body');

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;

    buffer += decoder.decode(value, { stream: true });
    const lines = buffer.split('\n');
    buffer = lines.pop() || '';

    for (const line of lines) {
      if (!line.startsWith('data: ')) continue;
      try {
        const event: SearchStreamEvent = JSON.parse(line.slice(6));
        switch (event.type) {
          case 'start':
            if (event.actorLike != null) {
              onStart?.({ actorLike: event.actorLike, indexCoverage: event.indexCoverage || 0, uniqueActors: event.uniqueActors || 0 });
            }
            break;
          case 'videos':
            if (event.videos) {
              allVideos.push(...event.videos);
              onVideos(event.videos);
            }
            break;
          case 'merged':
            if (event.videos) {
              onMerged?.(event.videos, event.stats);
            }
            break;
          case 'progress':
            if (event.completedSources != null && event.totalSources != null) {
              onProgress({ completed: event.completedSources, total: event.totalSources, phase: event.phase });
            }
            break;
          case 'complete':
          case 'error':
            break;
        }
      } catch {}
    }
  }

  return allVideos;
}

/** 获取影片详情（单站点） */
export async function getDetail(
  siteKey: string,
  id: string
): Promise<MediaDetail | null> {
  const data = await request<MediaDetail>(
    `${API_BASE}/api/detail?site_key=${siteKey}&id=${id}`
  );
  return data || null;
}

/** 跨站点详情（带站点信息的完整详情列表） */
export interface MultiDetailItem extends MediaDetail {
  site_key: string;
  site_name: string;
}

/** 获取跨站点多源详情：输入影片名和/或站点ID列表 */
export async function getMultiDetail(wd: string, keys?: string): Promise<MultiDetailItem[]> {
  let url = `${API_BASE}/api/multi-detail?wd=${encodeURIComponent(wd)}`;
  if (keys) url += `&keys=${encodeURIComponent(keys)}`;
  const data = await request<{ list: MultiDetailItem[] }>(url);
  return data.list || [];
}

/** 热门推荐 */
export async function getHot(): Promise<MediaItem[]> {
  const data = await request<{ list: MediaItem[] }>(`${API_BASE}/api/hot`);
  return data.list || [];
}

/** 首页批处理：一次返回热门+5个分类 */
export interface HomeData {
  hot: MediaItem[];
  dianshiju: MediaItem[];
  dianying: MediaItem[];
  zongyi: MediaItem[];
  dongman: MediaItem[];
  duanju: MediaItem[];
}

export async function getHome(): Promise<HomeData> {
  const res = await fetch(`${API_BASE}/api/home`);
  if (!res.ok) throw new Error(`API Error: ${res.status}`);
  return res.json();
}

// ==================== 分类浏览（服务端分页） ====================

export interface CategoryResponse {
  total: number;
  page: number;
  pageSize: number;
  totalPages: number;
  list: MediaItem[];
  /** 数据是否已全量加载完成（false 表示后台还在拉取更多页） */
  complete: boolean;
  /** 请求的页码超出 CMS 实际可返回范围 */
  outOfRange?: boolean;
  /** 是否由服务端按 type_id 做子分类过滤（而非前端 typeMatch） */
  serverFiltered?: boolean;
  /** 子分类计数（第 1 页返回） */
  subCounts?: Record<string, number>;
}

/** 分类浏览 — 按 type_id 拉全量，服务端分页返回
 * @param category 分类标识（movie|tv|variety|anime|short-drama|sports）
 * @param fallbackWd 降级关键词（分类ID发现失败时使用）
 * @param subType 子分类标签（如"动作"），服务端按 type_id 精准过滤
 */
export async function getCategory(
  category: string,
  fallbackWd: string,
  page: number = 1,
  pageSize: number = 30,
  signal?: AbortSignal,
  subType?: string,
): Promise<CategoryResponse> {
  const res = await fetch(`${API_BASE}/api/category`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ category, wd: fallbackWd, page, pageSize, subType }),
    signal,
  });
  if (!res.ok) throw new Error(`API Error: ${res.status}`);
  return res.json();
}

/** 豆瓣推荐元数据 */
export interface DoubanSubject {
  id: string;
  title: string;
  cover: string;
  rate: string;
  url: string;
  year: string;
  abstract?: string;    // 简介
  genres?: string[];     // 类型
  region?: string[];     // 地区
  director?: string;     // 导演
  cast?: string;         // 演员（前5位）
}

/** 豆瓣首页合并数据 */
export interface DoubanHomeData {
  hot: DoubanSubject[];
  dianshiju: DoubanSubject[];
  dianying: DoubanSubject[];
  zongyi: DoubanSubject[];
  dongman: DoubanSubject[];
}

/** 豆瓣推荐（单分类，纯元数据） */
export async function getDoubanRecommend(type: string = 'movie', tag: string = '热门'): Promise<DoubanSubject[]> {
  const res = await fetch(`${API_BASE}/api/douban/recommend?type=${type}&tag=${encodeURIComponent(tag)}&limit=20`);
  if (!res.ok) return [];
  const data = await res.json();
  return data.subjects || [];
}

/** 豆瓣首页合并数据（一次请求返回所有分类） */
export async function getDoubanHome(): Promise<DoubanHomeData> {
  const res = await fetch(`${API_BASE}/api/douban/home`);
  if (!res.ok) return { hot: [], dianshiju: [], dianying: [], zongyi: [], dongman: [] };
  return res.json();
}

/** 单站点测速 */
export async function checkSite(key: string): Promise<number> {
  const data = await request<CheckResponse>(
    `${API_BASE}/api/check?key=${key}`
  );
  return data.latency;
}

// ==================== 播放URL解析 ====================
// 格式: "集名$URL#集名$URL$$$源2集名$URL#源2集名$URL"
// - $$$ 分隔不同播放源
// - # 分隔同一源下的剧集
// - $ 分隔剧集名称和URL

export interface Episode {
  title: string;
  url: string;
}

export interface PlaySource {
  name: string;
  episodes: Episode[];
}

/**
 * 站点 key → 中文名 + 源名称前缀映射
 * 用于将英文源名（如 liangzi、lzm3u8）自动转换为中文名
 */
const SITE_SOURCE_PREFIX: Record<string, { name: string; prefixes: string[] }> = {
  lzzy:   { name: '量子', prefixes: ['liangzi', 'lz'] },
  ffzy:   { name: '非凡', prefixes: ['feifan', 'ff'] },
  bfzy:   { name: '暴风', prefixes: ['bfzy', 'bf'] },
  suoni:  { name: '索尼', prefixes: ['suoni', 'soni'] },
  bdzy:   { name: '百度', prefixes: ['baidu', 'bd'] },
};

/** 源名称中文化：原始名 → 中文显示名 */
const SOURCE_NAME_MAP: Record<string, string> = {
  // 量子资源
  'liangzi': '量子.线路一',
  'lzm3u8': '量子.线路二',
  // 非凡影视
  'feifan': '非凡.线路一',
  'ffm3u8': '非凡.线路二',
  // 暴风资源
  'bfzym3u8': '暴风.线路一',
  // 索尼资源
  'sonim3u8': '索尼.线路一',
  // 百度资源
  'bdm3u8': '百度.线路一',
};

function formatSourceName(rawName: string, idx: number, siteKey?: string): string {
  // 1. 精确映射
  if (SOURCE_NAME_MAP[rawName]) return SOURCE_NAME_MAP[rawName];
  
  const lower = rawName.toLowerCase();
  const isM3u8 = lower.includes('m3u8');
  const tag = isM3u8 ? 'm3u8' : '直连';
  
  // 2. 按站点前缀匹配
  for (const [key, cfg] of Object.entries(SITE_SOURCE_PREFIX)) {
    for (const prefix of cfg.prefixes) {
      if (lower.startsWith(prefix)) {
        const rest = rawName.slice(prefix.length).replace(/^[._-]+/, '').replace(/m3u8$/i, '');
        const label = rest || '线路';
        return `${cfg.name}.${label}`;
      }
    }
  }
  
  // 3. 如果知道站点 key，用站点中文名 + 序号
  const siteCfg = siteKey ? SITE_SOURCE_PREFIX[siteKey] : undefined;
  const siteName = siteCfg?.name || '';
  
  // 提取源名中除了 m3u8 标签之外的标识部分
  const cleanName = rawName.replace(/\.?m3u8$/i, '').replace(/^[._-]+/, '');
  const displayName = cleanName || `线路${idx + 1}`;
  
  if (siteName) {
    return `${siteName}.${displayName}`;
  }
  
  // 4. 通用回退
  return `${displayName}.${tag}`;
}

export function parsePlaySources(from: string, url: string, siteKey?: string): PlaySource[] {
  if (!url) return [];
  const sourceNames = from ? from.split('$$$') : [];
  const sourceUrls = url.split('$$$');

  return sourceUrls.map((sourceUrl, i) => ({
    name: sourceNames[i] ? formatSourceName(sourceNames[i], i, siteKey) : `线路${i + 1}`,
    episodes: sourceUrl
      .split('#')
      .filter(Boolean)
      .map((ep) => {
        const idx = ep.indexOf('$');
        if (idx === -1) return { title: ep, url: ep };
        return { title: ep.slice(0, idx), url: ep.slice(idx + 1) };
      }),
  }));
}
