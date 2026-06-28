/**
 * 多格式视频播放器 Hook
 * 参考 KVideo useHlsPlayer.ts 和 MoonTVPlus TVNativeVideo.tsx
 *
 * 支持：
 * 1. HLS 流 (.m3u8) → hls.js / 原生 HLS（Safari）
 * 2. FLV 流 (.flv)   → flv.js（动态加载）
 * 3. TS 流 (.ts)     → 包装为临时 M3U8 再通过 hls.js 播放
 * 4. 直连视频 (.mp4/.webm 等) → 原生 <video>
 */
import { useEffect, useRef } from 'react';
import Hls from 'hls.js';

interface UseHlsPlayerProps {
  videoRef: React.RefObject<HTMLVideoElement | null>;
  src: string;
  autoPlay?: boolean;
  onError?: (message: string) => void;
}

// 扩展 video 元素类型，存储第三方播放器实例
interface ExtendedVideoElement extends HTMLVideoElement {
  __flv?: { destroy: () => void; unload?: () => void };
  __m3u8_blob?: string;
}

type SourceType = 'm3u8' | 'flv' | 'ts' | 'native';

// ---- 格式检测 ----

/** 从代理 URL 中提取原始 URL（若为代理 URL），否则返回原 URL */
function resolveRealUrl(url: string): string {
  if (url.includes('/api/proxy')) {
    try {
      const params = new URL(url, 'http://localhost');
      const original = params.searchParams.get('url');
      if (original) return decodeURIComponent(original);
    } catch {}
  }
  return url;
}

/** 判断 URL 是否为代理 URL */
function isProxyUrl(url: string): boolean {
  return url.includes('/api/proxy');
}

/** 判断 URL 是否为 HLS 流（支持直连和代理 URL） */
function isHlsStream(url: string): boolean {
  // 直连 m3u8
  if (/\.m3u8([?#].*)?$/i.test(url) || url.includes('/m3u8') || url.includes('application/vnd.apple.mpegurl')) {
    return true;
  }
  // 代理 URL：检查原始 URL
  if (isProxyUrl(url)) {
    const realUrl = resolveRealUrl(url);
    const lower = realUrl.toLowerCase();
    // 已知的非 m3u8 视频扩展名 → 不走 HLS.js
    if (/\.(mp4|webm|ogg|flv|ts|mov|mkv|avi|wmv)([?#]|$)/i.test(lower)) return false;
    // 无扩展名 → 可能是分享页，代理会解析为 m3u8 → 走 HLS.js
    return true;
  }
  return false;
}

/** 检测源类型（代理 URL 检查原始 URL 的扩展名） */
function detectSourceType(url: string): SourceType {
  if (isHlsStream(url)) return 'm3u8';
  const checkUrl = isProxyUrl(url) ? resolveRealUrl(url) : url;
  const lower = checkUrl.toLowerCase();
  if (lower.includes('.flv') || lower.includes('.flv?')) return 'flv';
  if (lower.endsWith('.ts') || lower.includes('.ts?')) return 'ts';
  return 'native';
}

// ---- 辅助函数 ----

/** 为 TS 文件生成临时 M3U8 清单，通过 hls.js 播放 */
function buildTsM3u8(src: string): string {
  return [
    '#EXTM3U',
    '#EXT-X-VERSION:3',
    '#EXT-X-TARGETDURATION:10',
    '#EXT-X-MEDIA-SEQUENCE:0',
    '#EXTINF:10,',
    src,
    '#EXT-X-ENDLIST',
  ].join('\n');
}

/** 通用自动播放，静默失败 */
function safeAutoPlay(video: HTMLVideoElement) {
  video.play().catch(() => {});
}

// ---- 主 Hook ----

export function useHlsPlayer({ videoRef, src, autoPlay = true, onError }: UseHlsPlayerProps) {
  const playerRef = useRef<Hls | null>(null); // hls.js 实例
  const errorReportedRef = useRef(false);
  const blobUrlRef = useRef<string | null>(null); // TS 模式下生成的 Blob URL

  useEffect(() => {
    const video = videoRef.current as ExtendedVideoElement | null;
    if (!video || !src) return;

    // ---- 清理旧实例 ----
    // hls.js
    if (playerRef.current) {
      playerRef.current.destroy();
      playerRef.current = null;
    }
    // flv.js
    if (video.__flv) {
      try {
        video.__flv.unload?.();
        video.__flv.destroy();
      } catch {}
      video.__flv = undefined;
    }
    // TS Blob URL
    if (blobUrlRef.current) {
      URL.revokeObjectURL(blobUrlRef.current);
      blobUrlRef.current = null;
    }

    errorReportedRef.current = false;

    const sourceType = detectSourceType(src);
    const startPlay = () => safeAutoPlay(video);

    // ==================================================================
    // 类型 1: HLS 流
    // ==================================================================
    if (sourceType === 'm3u8') {
      const isMSESupported = Hls.isSupported();
      const isNativeHls = video.canPlayType('application/vnd.apple.mpegurl');

      if (isMSESupported) {
        const hls = new Hls({
          enableWorker: true,
          lowLatencyMode: false,
          maxBufferLength: 120,
          manifestLoadingMaxRetry: 4,
          manifestLoadingRetryDelay: 1000,
          fragLoadingMaxRetry: 6,
          fragLoadingRetryDelay: 1000,
          manifestLoadingTimeOut: 10000,
          fragLoadingTimeOut: 20000,
        });
        playerRef.current = hls;

        hls.loadSource(src);
        hls.attachMedia(video);

        hls.on(Hls.Events.MANIFEST_PARSED, () => {
          // H.264 优先
          const levels = hls.levels;
          if (levels?.length) {
            const h264Idx = levels.findIndex(l => {
              const c = l.videoCodec?.toLowerCase() || '';
              return !c.includes('hev') && !c.includes('h265') && !c.includes('hvc');
            });
            if (h264Idx > 0) hls.currentLevel = h264Idx;
          }
          if (autoPlay) startPlay();
        });

        // 错误处理
        let netRetries = 0, mediaRetries = 0;
        hls.on(Hls.Events.ERROR, (_event, data) => {
          if (!data.fatal) return;
          if (data.type === Hls.ErrorTypes.NETWORK_ERROR) {
            if (++netRetries <= 3) { hls.startLoad(); return; }
          }
          if (data.type === Hls.ErrorTypes.MEDIA_ERROR) {
            if (++mediaRetries <= 3) { hls.recoverMediaError(); return; }
          }
          errorReportedRef.current = true;
          onError?.(`HLS 播放失败: ${data.details || '未知错误'}`);
          hls.destroy();
        });

        return () => {
          hls.destroy();
          playerRef.current = null;
        };

      } else if (isNativeHls) {
        video.src = src;
        if (autoPlay) startPlay();
      } else {
        video.src = src;
        if (autoPlay) startPlay();
        errorReportedRef.current = true;
        onError?.('当前浏览器不支持 HLS，建议使用 Chrome');
      }
    }

    // ==================================================================
    // 类型 2: FLV 流 → flv.js（动态加载）
    // ==================================================================
    else if (sourceType === 'flv') {
      let cancelled = false;

      import('flv.js').then(flvModule => {
        if (cancelled) return;
        const flvjs = flvModule.default;

        if (!flvjs.isSupported()) {
          errorReportedRef.current = true;
          onError?.('当前浏览器不支持 FLV 播放');
          return;
        }

        try {
          const flv = flvjs.createPlayer({ type: 'flv', url: src, isLive: false });
          flv.attachMediaElement(video);
          flv.load();
          video.__flv = flv;

          if (autoPlay) {
            // FLV 加载完成后自动播放
            flv.on(flvjs.Events.METADATA_ARRIVED, () => {
              startPlay();
            });
          }

          flv.on(flvjs.Events.ERROR, (_type: string, detail: string) => {
            if (errorReportedRef.current) return;
            errorReportedRef.current = true;
            onError?.(`FLV 播放失败: ${detail || '未知错误'}`);
          });

        } catch (err: any) {
          errorReportedRef.current = true;
          onError?.(`FLV 初始化失败: ${err?.message || '未知错误'}`);
        }
      }).catch(() => {
        if (cancelled) return;
        errorReportedRef.current = true;
        onError?.('FLV.js 加载失败，请检查网络');
      });

      return () => {
        cancelled = true;
        if (video.__flv) {
          try {
            video.__flv.unload?.();
            video.__flv.destroy();
          } catch {}
          video.__flv = undefined;
        }
      };
    }

    // ==================================================================
    // 类型 3: TS 流 → 包装为 M3U8 通过 hls.js 播放
    // ==================================================================
    else if (sourceType === 'ts') {
      const m3u8Content = buildTsM3u8(src);
      const blob = new Blob([m3u8Content], { type: 'application/vnd.apple.mpegurl' });
      const blobUrl = URL.createObjectURL(blob);
      blobUrlRef.current = blobUrl;

      if (Hls.isSupported()) {
        const hls = new Hls({
          enableWorker: true,
          maxBufferLength: 60,
          fragLoadingMaxRetry: 6,
          fragLoadingRetryDelay: 1000,
          manifestLoadingTimeOut: 10000,
          fragLoadingTimeOut: 20000,
        });
        playerRef.current = hls;

        hls.loadSource(blobUrl);
        hls.attachMedia(video);

        hls.on(Hls.Events.MANIFEST_PARSED, () => {
          if (autoPlay) startPlay();
        });

        let retries = 0;
        hls.on(Hls.Events.ERROR, (_event, data) => {
          if (!data.fatal) return;
          if (data.type === Hls.ErrorTypes.NETWORK_ERROR && ++retries <= 5) {
            hls.startLoad(); return;
          }
          errorReportedRef.current = true;
          onError?.(`TS 播放失败: ${data.details || '未知错误'}`);
          hls.destroy();
        });

        return () => {
          hls.destroy();
          playerRef.current = null;
          if (blobUrlRef.current) {
            URL.revokeObjectURL(blobUrlRef.current);
            blobUrlRef.current = null;
          }
        };

      } else {
        // 无 hls.js 支持，尝试直连
        video.src = blobUrl;
        if (autoPlay) startPlay();

        const onVideoError = () => {
          if (errorReportedRef.current) return;
          errorReportedRef.current = true;
          onError?.('TS 格式视频播放失败');
        };
        video.addEventListener('error', onVideoError);

        return () => {
          video.removeEventListener('error', onVideoError);
          if (blobUrlRef.current) {
            URL.revokeObjectURL(blobUrlRef.current);
            blobUrlRef.current = null;
          }
        };
      }
    }

    // ==================================================================
    // 类型 4: 原生视频（.mp4/.webm/.ogg 等）
    // ==================================================================
    else {
      video.removeAttribute('crossOrigin');
      video.removeAttribute('src');
      video.load();
      video.src = src;

      const onMetadata = () => {
        if (autoPlay) startPlay();
        video.removeEventListener('loadedmetadata', onMetadata);
      };
      video.addEventListener('loadedmetadata', onMetadata);

      const onVideoError = () => {
        if (errorReportedRef.current) return;
        errorReportedRef.current = true;
        const err = video.error;
        const msg = err
          ? `视频加载失败 (${err.code}): ${err.message || '未知错误'}`
          : '视频加载失败';
        onError?.(msg);
      };
      video.addEventListener('error', onVideoError);

      return () => {
        video.removeEventListener('loadedmetadata', onMetadata);
        video.removeEventListener('error', onVideoError);
      };
    }

    // 非 MSE 路径没有额外清理
    return undefined;
  }, [src]);

  return playerRef;
}

export type { SourceType };
