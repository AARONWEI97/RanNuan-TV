import { useState, useRef, useCallback, useEffect } from 'react';
import {
  Play, Pause, Volume2, VolumeX, Maximize, Minimize,
  ArrowLeft, Gauge, PictureInPicture2, SkipForward, SkipBack,
  Loader2, ListVideo, X,
} from 'lucide-react';
import { useHlsPlayer } from './hlsPlayer';

interface Props {
  src: string;
  title?: string;
  sources?: { name: string; url: string }[];
  episodes?: { title: string; url: string; index?: number }[];
  currentEpisodeIndex?: number;
  currentSourceIndex?: number;
  savedPosition?: number; // 上次播放位置（秒），>30s 时弹出续播 Dialog
  onBack?: () => void;
  onSourceChange?: (url: string) => void;
  onEpisodeChange?: (index: number) => void;
  onError?: (message: string) => void;
  onTimeUpdate?: (time: number, duration: number) => void;
}

const SPEEDS = [0.5, 0.75, 1, 1.25, 1.5, 2];

function NowPlayingBars({ className = '' }: { className?: string }) {
  return (
    <span className={`flex h-4 w-5 items-end justify-center gap-0.5 ${className}`} aria-hidden>
      <span className="w-1 rounded-full bg-white animate-now-playing-bar" style={{ animationDelay: '0ms' }} />
      <span className="w-1 rounded-full bg-white animate-now-playing-bar" style={{ animationDelay: '150ms' }} />
      <span className="w-1 rounded-full bg-white animate-now-playing-bar" style={{ animationDelay: '300ms' }} />
    </span>
  );
}

export default function VideoPlayer({
  src, title, sources, episodes, currentEpisodeIndex = 0,
  currentSourceIndex = 0,
  savedPosition = 0, onBack, onSourceChange, onEpisodeChange,
  onError, onTimeUpdate,
}: Props) {
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const containerRef = useRef<HTMLDivElement | null>(null);

  // --- State ---
  const [playing, setPlaying] = useState(false);
  const [loading, setLoading] = useState(true);
  const [seeking, setSeeking] = useState(false);
  const [muted, setMuted] = useState(false);
  const [volume, setVolume] = useState(() => {
    try { return parseFloat(localStorage.getItem('player-volume') || '1'); } catch { return 1; }
  });
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(0);
  const [buffered, setBuffered] = useState(0);
  const [showControls, setShowControls] = useState(true);
  const [showSpeedMenu, setShowSpeedMenu] = useState(false);
  const [showEpisodeDrawer, setShowEpisodeDrawer] = useState(false);
  const [speed, setSpeed] = useState(() => {
    try { return parseFloat(localStorage.getItem('player-speed') || '1'); } catch { return 1; }
  });
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [hoverTime, setHoverTime] = useState<number | null>(null);
  const [hoverX, setHoverX] = useState(0);

  const [skipFeedback, setSkipFeedback] = useState<{ delta: number } | null>(null);
  const hideTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const speedMenuTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const skipTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const lastClickRef = useRef(0);
  const clickTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  // --- HLS Player ---
  useHlsPlayer({ videoRef: videoRef as React.RefObject<HTMLVideoElement>, src, autoPlay: true, onError });

  // --- 断点续播（静默 seek 到上次位置） ---
  useEffect(() => {
    const v = videoRef.current;
    if (v && savedPosition > 0) {
      const handler = () => { if (Math.abs(v.currentTime - savedPosition) > 0.5) v.currentTime = savedPosition; };
      v.addEventListener('loadedmetadata', handler, { once: true });
      return () => v.removeEventListener('loadedmetadata', handler);
    }
  }, [savedPosition]);

  // --- 音量/倍速持久化 ---
  useEffect(() => { try { localStorage.setItem('player-volume', String(volume)); } catch {} }, [volume]);
  useEffect(() => { try { localStorage.setItem('player-speed', String(speed)); } catch {} }, [speed]);

  // --- 同步视频属性 ---
  useEffect(() => {
    if (videoRef.current) {
      videoRef.current.volume = volume;
      videoRef.current.muted = muted;
      videoRef.current.playbackRate = speed;
    }
  }, [volume, muted, speed]);

  // --- 跳过反馈 ---
  const showSkipFeedback = (delta: number) => {
    setSkipFeedback({ delta });
    if (skipTimer.current) clearTimeout(skipTimer.current);
    skipTimer.current = setTimeout(() => setSkipFeedback(null), 800);
  };

  // --- 键盘快捷键 ---
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      const el = document.activeElement;
      if (el && (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA' || el.tagName === 'SELECT')) return;
      const v = videoRef.current;
      if (!v) return;
      switch (e.key) {
        case ' ': case 'k': e.preventDefault(); togglePlay(); break;
        case 'ArrowLeft': case 'j': e.preventDefault(); seekBy(-10, true); break;
        case 'ArrowRight': case 'l': e.preventDefault(); seekBy(10, true); break;
        case 'ArrowUp': e.preventDefault(); setVolume(vol => Math.min(1, vol + 0.05)); break;
        case 'ArrowDown': e.preventDefault(); setVolume(vol => Math.max(0, vol - 0.05)); break;
        case 'f': e.preventDefault(); toggleFullscreen(); break;
        case 'm': e.preventDefault(); setMuted(m => !m); break;
        case 'p': e.preventDefault(); togglePiP(); break;
        case 'n': e.preventDefault(); nextEpisode(); break;
      }
    };
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  // --- 全屏 ---
  useEffect(() => {
    const onFS = () => {
      const isFS = !!document.fullscreenElement;
      setIsFullscreen(isFS);
      if (!isFS) {
        // 退出全屏后强制重绘，修复 Android WebView 黑屏问题
        requestAnimationFrame(() => {
          const el = containerRef.current;
          if (el) {
            el.style.display = 'none';
            void el.offsetHeight; // force reflow
            el.style.display = '';
          }
        });
      }
    };
    document.addEventListener('fullscreenchange', onFS);
    return () => document.removeEventListener('fullscreenchange', onFS);
  }, []);

  // --- 下一集连播 ---
  const nextEpisode = useCallback(() => {
    if (episodes && currentEpisodeIndex < episodes.length - 1) {
      onEpisodeChange?.(currentEpisodeIndex + 1);
    }
  }, [episodes, currentEpisodeIndex, onEpisodeChange]);

  useEffect(() => {
    const v = videoRef.current;
    if (!v) return;
    v.addEventListener('ended', nextEpisode);
    return () => v.removeEventListener('ended', nextEpisode);
  }, [nextEpisode]);

  // --- 时间更新 ---
  const onTimeUpdateHandler = useCallback(() => {
    const v = videoRef.current;
    if (v) {
      setCurrentTime(v.currentTime);
      setDuration(v.duration || 0);
      if (v.buffered.length > 0) setBuffered(v.buffered.end(v.buffered.length - 1));
      onTimeUpdate?.(v.currentTime, v.duration || 0);
    }
  }, [onTimeUpdate]);

  // --- 播放控制 ---
  const togglePlay = () => {
    const v = videoRef.current;
    if (!v) return;
    if (v.paused) v.play().catch(() => {}); else v.pause();
  };

  const seekBy = (delta: number, showFeedback = false) => {
    const v = videoRef.current;
    if (!v) return;
    v.currentTime = Math.max(0, Math.min(v.duration || 0, v.currentTime + delta));
    if (showFeedback) showSkipFeedback(delta);
  };

  const seek = (e: React.MouseEvent<HTMLDivElement>) => {
    const v = videoRef.current;
    if (!v || !duration) return;
    const rect = e.currentTarget.getBoundingClientRect();
    v.currentTime = ((e.clientX - rect.left) / rect.width) * duration;
  };

  const handleProgressHover = (e: React.MouseEvent<HTMLDivElement>) => {
    if (!duration) return;
    const rect = e.currentTarget.getBoundingClientRect();
    const ratio = (e.clientX - rect.left) / rect.width;
    setHoverTime(ratio * duration);
    setHoverX(e.clientX - rect.left);
  };

  const handleProgressLeave = () => setHoverTime(null);

  // 触摸拖动进度条
  const handleProgressTouchMove = (e: React.TouchEvent<HTMLDivElement>) => {
    const v = videoRef.current;
    if (!v || !duration) return;
    const rect = e.currentTarget.getBoundingClientRect();
    const clientX = e.touches[0].clientX;
    v.currentTime = Math.max(0, Math.min(duration, ((clientX - rect.left) / rect.width) * duration));
    setHoverTime(null);
  };

  // --- 双击全屏 / 单击播放暂停 ---
  const handleVideoClick = (_e: React.MouseEvent) => {
    const now = Date.now();
    const elapsed = now - lastClickRef.current;
    if (elapsed < 300) {
      if (clickTimerRef.current) clearTimeout(clickTimerRef.current);
      toggleFullscreen();
    } else {
      lastClickRef.current = now;
      clickTimerRef.current = setTimeout(() => {
        if (Date.now() - lastClickRef.current >= 300) togglePlay();
      }, 300);
    }
  };

  const toggleFullscreen = () => {
    const el = containerRef.current;
    if (!el) return;
    if (document.fullscreenElement) document.exitFullscreen();
    else el.requestFullscreen().catch(() => {});
  };

  const togglePiP = async () => {
    const v = videoRef.current;
    if (!v) return;
    try {
      if (document.pictureInPictureElement) await document.exitPictureInPicture();
      else await v.requestPictureInPicture();
    } catch {}
  };

  const formatTime = (s: number) => {
    if (!isFinite(s) || s < 0) return '0:00';
    const h = Math.floor(s / 3600);
    const m = Math.floor((s % 3600) / 60);
    const sec = Math.floor(s % 60);
    if (h > 0) return `${h}:${m.toString().padStart(2, '0')}:${sec.toString().padStart(2, '0')}`;
    return `${m}:${sec.toString().padStart(2, '0')}`;
  };

  const playingRef = useRef(playing);
  const showSpeedMenuRef = useRef(showSpeedMenu);
  playingRef.current = playing;
  showSpeedMenuRef.current = showSpeedMenu;

  // --- 自动隐藏控制栏 ---
  const resetHideTimer = useCallback(() => {
    if (hideTimer.current) clearTimeout(hideTimer.current);
    setShowControls(true);
    if (playingRef.current) {
      hideTimer.current = setTimeout(() => {
        if (!showSpeedMenuRef.current) setShowControls(false);
      }, 4000);
    }
  }, []);

  const resetHideTimerRef = useRef(resetHideTimer);
  resetHideTimerRef.current = resetHideTimer;

  // --- 全屏/容器 mousemove：React onMouseMove 在全屏下不可靠，改用原生监听 + capture ---
  useEffect(() => {
    const el = containerRef.current;
    if (!el) return;
    const onMove = () => resetHideTimerRef.current();
    el.addEventListener('mousemove', onMove, { capture: true });
    return () => el.removeEventListener('mousemove', onMove, { capture: true });
  }, []);

  useEffect(() => {
    if (!isFullscreen) return;
    setShowControls(true);
    const onMove = () => resetHideTimerRef.current();
    document.addEventListener('mousemove', onMove, { capture: true });
    return () => document.removeEventListener('mousemove', onMove, { capture: true });
  }, [isFullscreen]);

  const currentEp = episodes?.[currentEpisodeIndex];
  const hasPrev = episodes && currentEpisodeIndex > 0;
  const hasNext = episodes && currentEpisodeIndex < episodes.length - 1;
  const hasEpisodeDrawer = !!episodes && episodes.length > 1;
  const playedPercent = duration > 0 ? Math.min(Math.max((currentTime / duration) * 100, 0), 100) : 0;
  const bufferedPercent = duration > 0 ? Math.min(Math.max((buffered / duration) * 100, 0), 100) : 0;
  const thumbPercent = Math.min(Math.max(playedPercent, 1), 99);

  useEffect(() => {
    if (!isFullscreen) setShowEpisodeDrawer(false);
  }, [isFullscreen]);

  // ================================================================
  // 渲染
  // ================================================================
  return (
    <div
      ref={containerRef}
      className="relative w-full h-full bg-black select-none"
      onMouseLeave={() => { if (playing && !showSpeedMenu && !isFullscreen) setShowControls(false); }}
    >
      <video
        ref={videoRef}
        className="w-full h-full object-contain"
        onPlay={() => { setPlaying(true); setLoading(false); }}
        onPause={() => setPlaying(false)}
        onWaiting={() => setLoading(true)}
        onCanPlay={() => setLoading(false)}
        onSeeking={() => setSeeking(true)}
        onSeeked={() => setSeeking(false)}
        onTimeUpdate={onTimeUpdateHandler}
        onLoadedMetadata={onTimeUpdateHandler}
        playsInline
        crossOrigin="anonymous"
      />

      {/* 触摸/鼠标捕获层 */}
      <div
        className="absolute inset-0 z-[5]"
        onMouseMove={resetHideTimer}
        onTouchStart={() => resetHideTimer()}
        onClick={handleVideoClick}
        aria-hidden
      />

      {/* ====== 中央加载指示 ====== */}
      {loading && playing && (
        <div className="absolute inset-0 flex items-center justify-center pointer-events-none z-20">
          <div className="relative">
            <Loader2 size={52} className="text-brand-400 animate-spin" />
            <div className="absolute inset-0 rounded-full blur-xl bg-brand-500/20 animate-pulse" />
          </div>
        </div>
      )}

      {/* ====== 快进/快退视觉反馈 ====== */}
      {skipFeedback && (
        <div
          className={`absolute z-30 flex items-center gap-2 pointer-events-none transition-opacity duration-200 ${skipFeedback ? 'opacity-100' : 'opacity-0'}`}
          style={{ top: '50%', left: skipFeedback.delta > 0 ? '60%' : '30%', transform: 'translate(-50%, -50%)' }}
        >
          {skipFeedback.delta > 0 ? (
            <SkipForward size={40} className="text-white drop-shadow-lg" />
          ) : (
            <SkipBack size={40} className="text-white drop-shadow-lg" />
          )}
          <span className="text-white text-xl font-bold drop-shadow-lg tabular-nums">
            {skipFeedback.delta > 0 ? '+' : ''}{skipFeedback.delta}s
          </span>
        </div>
      )}

      {/* ====== 左上返回按钮（非全屏时，跟随控制条显示） ====== */}
      {onBack && (
        <button onClick={onBack}
          className={`absolute top-4 left-4 z-20 p-2 rounded-xl bg-black/40 hover:bg-black/60 backdrop-blur-md border border-white/10 transition-all duration-300 ${showControls && !isFullscreen ? 'opacity-100 translate-y-0' : 'opacity-0 -translate-y-2 pointer-events-none'}`}>
          <ArrowLeft size={18} className="text-white/80" />
        </button>
      )}

      {/* ====== 顶部栏（仅全屏时显示） ====== */}
      <div className={`absolute top-0 left-0 right-0 p-4 z-10 transition-all duration-300 ${showControls && isFullscreen ? 'opacity-100 translate-y-0' : 'opacity-0 -translate-y-4 pointer-events-none'}`}>
        <div className="absolute inset-0 bg-gradient-to-b from-black/90 via-black/50 to-transparent" />
        <div className="relative">
          {title && <p className="text-white/90 font-medium truncate text-sm">{title}</p>}
          {currentEp && (
            <p className="text-white/40 text-xs truncate mt-0.5">{currentEp.title}</p>
          )}
        </div>
      </div>

      {/* ====== 全屏选集抽屉 ====== */}
      {isFullscreen && hasEpisodeDrawer && (
        <div className={`absolute inset-0 z-40 transition-all duration-300 ${showEpisodeDrawer ? 'pointer-events-auto' : 'pointer-events-none'}`}>
          <div
            className={`absolute inset-0 bg-black/45 backdrop-blur-[2px] transition-opacity duration-300 ${showEpisodeDrawer ? 'opacity-100' : 'opacity-0'}`}
            onClick={() => setShowEpisodeDrawer(false)}
          />
          <aside className={`absolute right-0 top-0 h-full w-[360px] max-w-[86vw] bg-zinc-950/92 backdrop-blur-2xl border-l border-white/10 shadow-2xl shadow-black/60 transition-transform duration-300 ease-out ${showEpisodeDrawer ? 'translate-x-0' : 'translate-x-full'}`}>
            <div className="flex h-full flex-col">
              <div className="flex items-center justify-between px-5 py-4 border-b border-white/[0.08]">
                <div className="min-w-0">
                  <p className="text-white text-sm font-medium">选集</p>
                  <p className="text-white/35 text-xs truncate mt-0.5">{title || '当前播放'} · {currentEpisodeIndex + 1}/{episodes?.length || 0}</p>
                </div>
                <button
                  onClick={() => setShowEpisodeDrawer(false)}
                  className="p-2 rounded-lg text-white/50 hover:text-white hover:bg-white/[0.08] transition-colors"
                  title="关闭"
                >
                  <X size={18} />
                </button>
              </div>

              <div className="flex-1 overflow-y-auto custom-scrollbar p-4">
                <div className="grid grid-cols-4 gap-2">
                  {episodes?.map((ep, index) => {
                    const active = index === currentEpisodeIndex;
                    return (
                      <button
                        key={`${ep.url}-${index}`}
                        onClick={() => {
                          onEpisodeChange?.(index);
                          setShowEpisodeDrawer(false);
                        }}
                        className={`relative h-11 rounded-xl border text-xs font-medium transition-all ${
                          active
                            ? 'bg-brand-500 text-white border-brand-300/40 shadow-lg shadow-brand-500/25'
                            : 'bg-white/[0.04] text-zinc-400 border-white/[0.05] hover:bg-white/[0.10] hover:text-white hover:border-white/10'
                        }`}
                        title={ep.title}
                      >
                        <span className={active ? 'opacity-20' : ''}>{index + 1}</span>
                        {active && <NowPlayingBars className="absolute inset-0 m-auto" />}
                      </button>
                    );
                  })}
                </div>
              </div>
            </div>
          </aside>
        </div>
      )}

      {/* ====== 底部控制栏 ====== */}
      <div
        className={`absolute bottom-0 left-0 right-0 z-10 transition-all duration-300 ${showControls ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-4 pointer-events-none'}`}
        onMouseMove={resetHideTimer}
        onTouchStart={resetHideTimer}
      >
        <div className="absolute inset-0 bg-gradient-to-t from-black/95 via-black/60 to-transparent" />

        {/* 进度条 */}
        <div className="relative px-4 pt-2 pb-1 cursor-pointer group/progress"
          onClick={seek} onMouseMove={handleProgressHover} onMouseLeave={handleProgressLeave}
          onTouchStart={handleProgressTouchMove} onTouchMove={handleProgressTouchMove}
        >
          {hoverTime !== null && (
            <div className="absolute -top-8 bg-zinc-900/95 text-white text-xs px-2 py-1 rounded-lg shadow-xl border border-zinc-700/50 whitespace-nowrap"
              style={{ left: `${Math.min(Math.max((hoverX / containerRef.current!.offsetWidth) * 100, 2), 98)}%`, transform: 'translateX(-50%)' }}
            >
              {formatTime(hoverTime)}
            </div>
          )}
          <div className="relative h-8">
            <div className="absolute left-0 right-0 top-1/2 h-1 -translate-y-1/2 rounded-full bg-white/15 transition-[height] duration-150 group-hover/progress:h-2.5">
              <div className="absolute inset-0 overflow-hidden rounded-full">
                <div
                  className="absolute left-0 top-0 h-full rounded-full bg-white/10"
                  style={{ width: `${bufferedPercent}%` }}
                />
                <div
                  className="absolute left-0 top-0 h-full rounded-full bg-brand-500 transition-[width] duration-100"
                  style={{ width: `${playedPercent}%` }}
                />
              </div>
            </div>
            <img
              src="/thumb_logo.png"
              alt=""
              className="absolute top-1/2 h-6 w-6 -translate-x-1/2 -translate-y-1/2 rounded-full object-cover shadow-lg shadow-black/50 transition-all duration-150 group-hover/progress:h-7 group-hover/progress:w-7 group-hover/progress:shadow-brand-500/30"
              style={{ left: `${thumbPercent}%` }}
              draggable={false}
            />
          </div>
        </div>

        {/* 按钮栏 */}
        <div className="relative flex items-center gap-1.5 sm:gap-2.5 px-3 sm:px-4 pb-3 sm:pb-4 pt-1">
          {/* 后退10秒 */}
          <button onClick={(e) => { e.stopPropagation(); seekBy(-10, true); }}
            className="p-1.5 text-white/60 active:text-white active:scale-110 transition-all inline-flex"
            title="后退10秒 (←)"
          >
            <SkipBack size={20} />
          </button>

          {/* 播放/暂停 */}
          <button onClick={(e) => { e.stopPropagation(); togglePlay(); }}
            className="p-2 text-white active:text-brand-400 active:scale-110 transition-all active:scale-90"
          >
            {playing ? <Pause size={24} /> : <Play size={24} className="ml-0.5" />}
          </button>

          {/* 前进10秒 */}
          <button onClick={(e) => { e.stopPropagation(); seekBy(10, true); }}
            className="p-1.5 text-white/60 active:text-white active:scale-110 transition-all inline-flex"
            title="前进10秒 (→)"
          >
            <SkipForward size={20} />
          </button>

          {/* 下一集 */}
          {hasNext && (
            <button onClick={nextEpisode}
              className="p-1.5 text-white/50 hover:text-white hover:scale-110 transition-all ml-1"
              title="下一集 (N)"
            >
              <SkipForward size={18} className="stroke-[2.5px]" />
            </button>
          )}

          {/* 音量 */}
          <div className="flex items-center gap-1 group/vol ml-1">
            <button onClick={() => setMuted(m => !m)}
              className="p-1.5 text-white/60 hover:text-white transition-colors"
            >
              {muted || volume === 0 ? <VolumeX size={19} /> : <Volume2 size={19} />}
            </button>
            <div className="overflow-hidden w-0 group-hover/vol:w-28 transition-all duration-300 flex items-center gap-2">
              <input type="range" min="0" max="1" step="0.05"
                value={muted ? 0 : volume}
                onChange={e => { const v = parseFloat(e.target.value); setVolume(v); setMuted(false); }}
                className="w-16 h-1 accent-brand-500 cursor-pointer"
              />
              <span className="text-white/50 text-[11px] tabular-nums w-8 text-right">
                {Math.round((muted ? 0 : volume) * 100)}%
              </span>
            </div>
          </div>

          {/* 时间 */}
          <span className="text-white/60 text-xs tabular-nums whitespace-nowrap ml-1">
            {formatTime(currentTime)}
            <span className="text-white/20 mx-1">/</span>
            {formatTime(duration)}
          </span>

          <div className="flex-1" />

          {/* 当前集数指示 */}
          {episodes && episodes.length > 1 && (
            <span className="text-white/30 text-xs tabular-nums">{currentEpisodeIndex + 1}/{episodes.length}</span>
          )}

          {/* 全屏选集 */}
          {isFullscreen && hasEpisodeDrawer && (
            <button onClick={() => { setShowEpisodeDrawer(true); setShowControls(true); }}
              className="px-2 py-1 rounded-lg text-xs transition-all flex items-center gap-1.5 text-white/55 hover:text-white hover:bg-white/5"
              title="选集"
            >
              <ListVideo size={15} />选集
            </button>
          )}

          {/* 倍速 */}
          <div className="relative">
            <button onClick={() => {
              setShowSpeedMenu(v => !v);
              if (speedMenuTimer.current) clearTimeout(speedMenuTimer.current);
            }}
              onMouseEnter={() => { if (speedMenuTimer.current) clearTimeout(speedMenuTimer.current); }}
              onMouseLeave={() => { speedMenuTimer.current = setTimeout(() => setShowSpeedMenu(false), 2000); }}
              className={`px-2 py-1 rounded-lg text-xs transition-all flex items-center gap-1 ${showSpeedMenu ? 'bg-brand-500/20 text-brand-400' : 'text-white/50 hover:text-white hover:bg-white/5'}`}
            >
              <Gauge size={13} />{speed}x
            </button>
            {showSpeedMenu && (
              <div className="absolute bottom-full right-0 mb-2 bg-zinc-950/95 backdrop-blur-2xl border border-zinc-700/50 rounded-xl overflow-hidden shadow-2xl"
                onMouseEnter={() => { if (speedMenuTimer.current) clearTimeout(speedMenuTimer.current); }}
                onMouseLeave={() => { speedMenuTimer.current = setTimeout(() => setShowSpeedMenu(false), 2000); }}
              >
                {SPEEDS.map(s => (
                  <button key={s}
                    onClick={() => { setSpeed(s); setShowSpeedMenu(false); }}
                    className={`w-full text-left px-4 py-2.5 text-sm hover:bg-white/5 transition-colors ${s === speed ? 'text-brand-400 font-semibold bg-brand-500/10' : 'text-white/70'}`}
                  >
                    {s}x {s === 1 ? '(默认)' : ''}
                  </button>
                ))}
              </div>
            )}
          </div>

          {/* PiP */}
          <button onClick={togglePiP}
            className="p-1.5 text-white/50 hover:text-white hover:bg-white/5 rounded-lg transition-all"
            title="画中画 (P)"
          >
            <PictureInPicture2 size={17} />
          </button>

          {/* 全屏 */}
          <button onClick={toggleFullscreen}
            className="p-1.5 text-white/60 hover:text-white hover:bg-white/5 rounded-lg transition-all"
          >
            {isFullscreen ? <Minimize size={19} /> : <Maximize size={19} />}
          </button>
        </div>
      </div>

    </div>
  );
}
