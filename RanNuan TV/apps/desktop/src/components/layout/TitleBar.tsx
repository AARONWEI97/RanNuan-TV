import { useState, useEffect } from 'react';
import { Minus, Square, X } from 'lucide-react';

function useWindowControls() {
  const [isMaximized, setIsMaximized] = useState(false);

  useEffect(() => {
    // @ts-ignore
    if (!window.__TAURI_INTERNALS__) return;
    const checkMax = async () => {
      try {
        const { getCurrentWindow } = await import('@tauri-apps/api/window');
        setIsMaximized(await getCurrentWindow().isMaximized());
      } catch {}
    };
    checkMax();
  }, []);

  const minimize = async () => {
    try { const { getCurrentWindow } = await import('@tauri-apps/api/window'); await getCurrentWindow().minimize(); } catch {}
  };
  const maximize = async () => {
    try {
      const { getCurrentWindow } = await import('@tauri-apps/api/window');
      const win = getCurrentWindow();
      if (await win.isMaximized()) { await win.unmaximize(); setIsMaximized(false); }
      else { await win.maximize(); setIsMaximized(true); }
    } catch {}
  };
  const close = async () => {
    try { const { getCurrentWindow } = await import('@tauri-apps/api/window'); await getCurrentWindow().close(); } catch {}
  };
  return { minimize, maximize, close, isMaximized };
}

export default function TitleBar() {
  const { minimize, maximize, close, isMaximized } = useWindowControls();

  return (
    <div
      data-tauri-drag-region
      className="h-8 flex items-center justify-end bg-zinc-950 border-b border-white/[0.06] shrink-0"
    >
      <div className="flex items-center h-full">
        <button onClick={minimize} className="w-11 h-full flex items-center justify-center hover:bg-white/[0.06] text-white/40 hover:text-white/70 transition-colors">
          <Minus size={14} />
        </button>
        <button onClick={maximize} className="w-11 h-full flex items-center justify-center hover:bg-white/[0.06] text-white/40 hover:text-white/70 transition-colors">
          <Square size={12} />
        </button>
        <button onClick={close} className="w-11 h-full flex items-center justify-center hover:bg-red-500/80 text-white/40 hover:text-white transition-colors">
          <X size={14} />
        </button>
      </div>
    </div>
  );
}
