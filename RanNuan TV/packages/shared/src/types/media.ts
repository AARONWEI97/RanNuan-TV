// ==================== 影片/剧集类型 ====================

export interface SiteInfo {
  key: string;
  name: string;
  id: string;
}

export interface MediaItem {
  vod_id: string;
  vod_name: string;
  vod_pic?: string;
  vod_remarks?: string;
  type_name?: string;
  vod_year?: string;
  vod_area?: string;
  vod_lang?: string;
  vod_actor?: string;
  vod_director?: string;
  vod_content?: string;
  vod_play_url?: string;
  // 聚合字段
  site_key: string;
  site_name: string;
  latency: number;
  /** 跨站点合并后，记录所有可用站点信息 */
  sites?: SiteInfo[];
}

export interface MediaDetail {
  vod_id: string;
  vod_name: string;
  vod_pic?: string;
  vod_content?: string;
  vod_actor?: string;
  vod_director?: string;
  vod_year?: string;
  vod_area?: string;
  vod_lang?: string;
  type_name?: string;
  vod_remarks?: string;
  /** 播放来源名，$$$分隔 */
  vod_play_from?: string;
  /** 播放地址: 集名$URL#集名$URL$$$源2集名$URL */
  vod_play_url?: string;
}

// ==================== 站点类型 ====================

export interface Site {
  key: string;
  name: string;
  api: string;
  active: boolean;
}

// ==================== API 响应类型 ====================

export interface ApiResponse<T> {
  list: T[];
}

export interface SearchResponse {
  list: MediaItem[];
}

export interface HotResponse {
  list: MediaItem[];
}

export interface CheckResponse {
  latency: number;
}

export interface DetailResponse {
  list?: MediaDetail[];
  [key: string]: unknown;
}
