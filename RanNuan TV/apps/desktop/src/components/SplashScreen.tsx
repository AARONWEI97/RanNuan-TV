import { useState, useEffect, useRef } from 'react';

// ===== Web Audio 影院音效生成器 =====
function useCinemaSounds(playing: boolean) {
  const ctxRef = useRef<AudioContext | null>(null);

  useEffect(() => {
    if (!playing) return;
    const ctx = new AudioContext();
    ctxRef.current = ctx;

    const playTick = (time: number, freq: number, vol: number) => {
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.type = 'sine';
      osc.frequency.value = freq;
      gain.gain.setValueAtTime(vol, time);
      gain.gain.exponentialRampToValueAtTime(0.001, time + 0.06);
      osc.connect(gain); gain.connect(ctx.destination);
      osc.start(time); osc.stop(time + 0.1);
    };

    // 放映机嗡鸣背景
    {
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.type = 'sawtooth';
      osc.frequency.value = 55;
      gain.gain.setValueAtTime(0.015, ctx.currentTime);
      gain.gain.setValueAtTime(0.015, ctx.currentTime + 1.8);
      gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 2.5);
      const filter = ctx.createBiquadFilter();
      filter.type = 'lowpass';
      filter.frequency.value = 200;
      osc.connect(filter); filter.connect(gain); gain.connect(ctx.destination);
      osc.start(); osc.stop(ctx.currentTime + 2.6);
    }

    // 胶卷转动咔哒声（三连击）
    [0.3, 0.8, 1.3].forEach((t, i) => {
      playTick(ctx.currentTime + t, 800 + i * 50, 0.08);
    });

    // 开场钟声
    {
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.type = 'triangle';
      osc.frequency.setValueAtTime(440, ctx.currentTime + 0.1);
      osc.frequency.exponentialRampToValueAtTime(220, ctx.currentTime + 1.8);
      gain.gain.setValueAtTime(0.06, ctx.currentTime + 0.1);
      gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 2.0);
      osc.connect(gain); gain.connect(ctx.destination);
      osc.start(ctx.currentTime + 0.1); osc.stop(ctx.currentTime + 2.1);
    }

    return () => { ctx.close(); };
  }, [playing]);

  return ctxRef;
}

interface Props {
  onFinish: () => void;
}

export default function SplashScreen({ onFinish }: Props) {
  const [phase, setPhase] = useState(0);
  useCinemaSounds(phase === 0);

  useEffect(() => {
    const timers = [
      setTimeout(() => setPhase(1), 200),
      setTimeout(() => setPhase(2), 2500),
      setTimeout(() => onFinish(), 3100),
    ];
    return () => timers.forEach(clearTimeout);
  }, [onFinish]);

  return (
    <div className={`fixed inset-0 z-[9999] flex items-center justify-center bg-[#080810] overflow-hidden transition-opacity duration-700 ${phase >= 2 ? 'opacity-0 pointer-events-none' : 'opacity-100'}`}>
      {/* ====== 胶片颗粒叠加 ====== */}
      <div className="absolute inset-0 pointer-events-none animate-film-grain" />

      {/* ====== 放映机锥形光束（从底部光源射出） ====== */}
      <div className="absolute inset-0 pointer-events-none overflow-hidden">
        {/* 光源底座 */}
        <div className="absolute bottom-0 left-1/2 -translate-x-1/2 w-3 h-3 bg-amber-300 rounded-full shadow-[0_0_40px_12px_rgba(251,191,36,0.4)] animate-projector-glow z-10" />
        <div className="absolute bottom-0 left-1/2 -translate-x-1/2 w-2 h-2 bg-white rounded-full shadow-[0_0_20px_6px_rgba(255,255,255,0.5)] z-10 animate-projector-glow" style={{ animationDelay: '0.3s' }} />

        {/* 主光束：底部窄 → 向上扩散成锥形 */}
        <div className="absolute bottom-8 left-1/2 -translate-x-1/2 pointer-events-none"
          style={{
            width: 0, height: 0,
            borderLeft: '180px solid transparent',
            borderRight: '180px solid transparent',
            borderBottom: '85vh solid transparent',
          }}>
          <div className="absolute -bottom-[85vh] -left-[180px] w-[360px] h-[85vh] bg-gradient-to-t from-amber-400/[0.15] via-amber-400/[0.04] to-transparent animate-projector-cone"
            style={{
              clipPath: 'polygon(50% 100%, 0% 0%, 100% 0%)',
            }} />
        </div>

        {/* 光束中层：更窄，更亮 */}
        <div className="absolute bottom-8 left-1/2 -translate-x-1/2 pointer-events-none"
          style={{
            width: 0, height: 0,
            borderLeft: '120px solid transparent',
            borderRight: '120px solid transparent',
            borderBottom: '85vh solid transparent',
          }}>
          <div className="absolute -bottom-[85vh] -left-[120px] w-[240px] h-[85vh] bg-gradient-to-t from-amber-300/[0.1] via-amber-200/[0.02] to-transparent animate-projector-cone"
            style={{
              clipPath: 'polygon(50% 100%, 0% 0%, 100% 0%)',
              animationDelay: '0.4s',
            }} />
        </div>

        {/* 光束中飞舞的灰尘颗粒 */}
        <div className="absolute inset-0 pointer-events-none">
          {Array.from({ length: 15 }).map((_, i) => (
            <div key={i} className="absolute w-0.5 h-0.5 bg-amber-300/40 rounded-full animate-dust"
              style={{
                left: `${40 + Math.random() * 20}%`,
                bottom: `${Math.random() * 70}%`,
                animationDelay: `${Math.random() * 2}s`,
                animationDuration: `${2 + Math.random() * 3}s`,
              }} />
          ))}
        </div>
      </div>

      {/* ====== 观众席剪影 ====== */}
      <div className="absolute bottom-0 left-0 right-0 h-32 pointer-events-none">
        {Array.from({ length: 8 }).map((_, i) => (
          <div key={i} className="absolute bottom-0 bg-amber-900/40 rounded-t-lg"
            style={{
              left: `${5 + i * 12}%`,
              width: '8%',
              height: `${30 + Math.random() * 40}px`,
            }}>
            {/* 头部 */}
            <div className="absolute -top-3 left-1/2 -translate-x-1/2 w-5 h-5 bg-amber-900/30 rounded-full" />
          </div>
        ))}
      </div>

      {/* ====== 胶卷穿孔边 ====== */}
      <div className="absolute left-8 top-0 bottom-0 w-6 flex flex-col justify-between py-12 pointer-events-none">
        {Array.from({ length: 12 }).map((_, i) => (
          <div key={i} className="w-3 h-3 rounded-sm bg-amber-500/20 animate-film-hole" style={{ animationDelay: `${i * 0.1}s` }} />
        ))}
      </div>
      <div className="absolute right-8 top-0 bottom-0 w-6 flex flex-col justify-between py-12 pointer-events-none">
        {Array.from({ length: 12 }).map((_, i) => (
          <div key={i} className="w-3 h-3 rounded-sm bg-amber-500/20 animate-film-hole" style={{ animationDelay: `${i * 0.1 + 0.5}s` }} />
        ))}
      </div>

      {/* ====== 漂浮影院元素 ====== */}
      <div className="absolute inset-0 pointer-events-none overflow-hidden">
        {/* 爆米花 */}
        <span className="absolute text-3xl animate-float-popcorn" style={{ top: '15%', left: '20%' }}>🍿</span>
        {/* 可乐 */}
        <span className="absolute text-3xl animate-float-cola" style={{ top: '60%', left: '25%' }}>🥤</span>
        {/* 胶卷 */}
        <span className="absolute text-2xl animate-float-film" style={{ top: '25%', right: '22%' }}>🎞️</span>
        {/* 放映机 */}
        <span className="absolute text-3xl animate-float-projector" style={{ bottom: '25%', right: '20%' }}>📽️</span>
        {/* 爆米花2 */}
        <span className="absolute text-2xl animate-float-popcorn2" style={{ top: '45%', left: '12%' }}>🍿</span>
        {/* 饮料 */}
        <span className="absolute text-2xl animate-float-cola2" style={{ bottom: '35%', right: '28%' }}>🥤</span>
        {/* 星星 */}
        <span className="absolute text-lg animate-float-star" style={{ top: '10%', right: '15%' }}>⭐</span>
      </div>

      {/* ====== 烟雾氛围 ====== */}
      <div className="absolute -bottom-20 left-0 right-0 h-64 bg-gradient-to-t from-amber-500/[0.03] via-amber-400/[0.01] to-transparent pointer-events-none animate-smoke" />

      {/* ====== 中央 Logo 区 ====== */}
      <div className="relative z-10 flex flex-col items-center">
        {/* Logo 光晕 */}
        <div className="absolute -inset-20 bg-amber-500/20 blur-3xl rounded-full opacity-0 animate-glow-pulse" />

        {/* 放映机图形 */}
        <div className="relative mb-6">
          <div className="absolute -top-1 -left-1 -right-1 -bottom-1 bg-gradient-to-br from-amber-400/30 via-amber-500/10 to-transparent rounded-3xl blur-md animate-glow-pulse" style={{ animationDelay: '0.5s' }} />
          <img
            src="/icon.png"
            alt="冉暖TV"
            className="relative w-24 h-24 rounded-2xl object-cover ring-2 ring-amber-500/30 shadow-2xl shadow-amber-500/20 animate-logo-reveal"
          />
        </div>

        {/* 标题 */}
        <h1 className="text-4xl font-black tracking-widest bg-gradient-to-r from-amber-200 via-amber-400 to-amber-500 bg-clip-text text-transparent animate-text-reveal"
          style={{ fontFamily: "'Georgia', 'Times New Roman', serif" }}>
          冉暖TV
        </h1>

        {/* 副标题 */}
        <p className="mt-3 text-amber-500/50 text-sm tracking-[0.3em] uppercase animate-text-reveal"
          style={{ animationDelay: '0.3s', fontFamily: "'Georgia', 'Times New Roman', serif" }}>
          你的私人影院
        </p>

        {/* 加载指示线 */}
        <div className="mt-8 w-48 h-0.5 bg-amber-500/10 rounded-full overflow-hidden">
          <div className="h-full bg-gradient-to-r from-amber-500/0 via-amber-400/60 to-amber-500/0 animate-loading-bar" />
        </div>
      </div>

      {/* ====== CSS 动画（内联） ====== */}
      <style>{`
        @keyframes projector-cone { 0%, 100% { opacity: 0.5; } 50% { opacity: 0.85; } }
        @keyframes projector-glow { 0%, 100% { opacity: 0.6; transform: scale(1); } 50% { opacity: 1; transform: scale(1.3); } }
        @keyframes dust { 0% { transform: translateY(0) translateX(0); opacity: 0; } 20% { opacity: 0.8; } 80% { opacity: 0.3; } 100% { transform: translateY(-60vh) translateX(10px); opacity: 0; } }
        @keyframes film-grain { 0%, 100% { background: radial-gradient(ellipse at 50% 50%, transparent 50%, rgba(0,0,0,0.03) 80%); } 50% { background: radial-gradient(ellipse at 50% 50%, transparent 60%, rgba(0,0,0,0.06) 90%); } }
        @keyframes film-hole { 0%, 100% { opacity: 0.3; } 50% { opacity: 0.7; } }
        @keyframes logo-reveal { 0% { opacity: 0; transform: scale(0.3) rotate(-15deg); } 60% { transform: scale(1.05) rotate(2deg); } 100% { opacity: 1; transform: scale(1) rotate(0deg); } }
        @keyframes text-reveal { 0% { opacity: 0; transform: translateY(16px); filter: blur(8px); } 100% { opacity: 1; transform: translateY(0); filter: blur(0); } }
        @keyframes glow-pulse { 0%, 100% { opacity: 0.3; transform: scale(0.95); } 50% { opacity: 0.7; transform: scale(1.05); } }
        @keyframes loading-bar { 0% { transform: translateX(-100%); } 50% { transform: translateX(100%); } 100% { transform: translateX(-100%); } }
        @keyframes float-popcorn { 0% { transform: translateY(0) rotate(0deg); } 25% { transform: translateY(-20px) rotate(5deg); } 50% { transform: translateY(-10px) rotate(-5deg); } 75% { transform: translateY(-25px) rotate(3deg); } 100% { transform: translateY(0) rotate(0deg); } }
        @keyframes float-cola { 0% { transform: translateY(0) rotate(0deg); } 25% { transform: translateY(-15px) rotate(-3deg); } 50% { transform: translateY(-8px) rotate(4deg); } 75% { transform: translateY(-22px) rotate(-2deg); } 100% { transform: translateY(0) rotate(0deg); } }
        @keyframes float-film { 0% { transform: translateY(0) rotate(0deg); } 33% { transform: translateY(-18px) rotate(-6deg); } 66% { transform: translateY(-6px) rotate(3deg); } 100% { transform: translateY(0) rotate(0deg); } }
        @keyframes float-projector { 0% { transform: translateY(0) rotate(0deg); } 33% { transform: translateY(-12px) rotate(3deg); } 66% { transform: translateY(-20px) rotate(-4deg); } 100% { transform: translateY(0) rotate(0deg); } }
        @keyframes float-popcorn2 { 0% { transform: translateY(0) rotate(0deg); } 50% { transform: translateY(-16px) rotate(8deg); } 100% { transform: translateY(0) rotate(0deg); } }
        @keyframes float-cola2 { 0% { transform: translateY(0) rotate(0deg); } 50% { transform: translateY(-14px) rotate(-5deg); } 100% { transform: translateY(0) rotate(0deg); } }
        @keyframes float-star { 0% { transform: translateY(0) scale(1); opacity: 0.3; } 50% { transform: translateY(-30px) scale(1.5); opacity: 0.8; } 100% { transform: translateY(0) scale(1); opacity: 0.3; } }
        @keyframes smoke { 0%, 100% { opacity: 0.5; transform: translateY(0); } 50% { opacity: 0.8; transform: translateY(-10px); } }

        .animate-projector-cone { animation: projector-cone 3s ease-in-out infinite; }
        .animate-projector-glow { animation: projector-glow 2s ease-in-out infinite; }
        .animate-dust { animation: dust linear infinite; }
        .animate-film-grain { animation: film-grain 0.2s steps(2) infinite; }
        .animate-film-hole { animation: film-hole 1.5s ease-in-out infinite; }
        .animate-logo-reveal { animation: logo-reveal 0.8s cubic-bezier(0.34, 1.56, 0.64, 1) forwards; }
        .animate-text-reveal { animation: text-reveal 0.8s ease-out forwards; opacity: 0; }
        .animate-glow-pulse { animation: glow-pulse 2s ease-in-out infinite; }
        .animate-loading-bar { animation: loading-bar 1.5s ease-in-out infinite; }
        .animate-float-popcorn { animation: float-popcorn 3s ease-in-out infinite; }
        .animate-float-cola { animation: float-cola 3.5s ease-in-out infinite; }
        .animate-float-film { animation: float-film 4s ease-in-out infinite; }
        .animate-float-projector { animation: float-projector 3.8s ease-in-out infinite; }
        .animate-float-popcorn2 { animation: float-popcorn2 3.2s ease-in-out infinite; }
        .animate-float-cola2 { animation: float-cola2 3.6s ease-in-out infinite; }
        .animate-float-star { animation: float-star 2.5s ease-in-out infinite; }
        .animate-smoke { animation: smoke 4s ease-in-out infinite; }
      `}</style>
    </div>
  );
}
