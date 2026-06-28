import type { MediaItem } from 'shared';

/** 站点 key → 中文名称映射 */
export const SITE_NAMES: Record<string, string> = {
  lzzy: '量子资源',
  ffzy: '非凡影视',
  bfzy: '暴风资源',
  suoni: '索尼资源',
  bdzy: '百度资源',
  kfzy: '快帆资源',
  lszy: '乐视资源',
};

/** 获取站点中文名 */
export function siteChineseName(siteKey: string): string {
  return SITE_NAMES[siteKey] || siteKey;
}

/** 获取项的中文站点名（优先用 site_name，否则从 site_key 映射） */
export function itemSiteName(item: MediaItem | { site_key: string; site_name?: string }): string {
  if (item.site_name && Object.values(SITE_NAMES).includes(item.site_name)) {
    return item.site_name;
  }
  return SITE_NAMES[item.site_key] || item.site_name || item.site_key;
}

/** 生成播放页链接（直接跳转播放器，支持断点续播） */
export function playerLink(item: MediaItem | { site_key: string; vod_id: string }): string {
  return `/player/${item.site_key}/${item.vod_id}`;
}

/** 生成详情页链接：多源走 /detail/source/:name?keys=...，单源走 /detail/:siteKey/:id */
export function detailLink(item: MediaItem): string {
  if (item.sites && item.sites.length > 1) {
    const keys = item.sites.map(s => `${s.key}:${s.id}`).join(',');
    return `/detail/source/${encodeURIComponent(item.vod_name)}?keys=${encodeURIComponent(keys)}`;
  }
  return `/detail/${item.site_key}/${item.vod_id}`;
}
