import { useCallback, useEffect, useState } from 'react';
import { Routes, Route, useNavigate } from 'react-router-dom';
import Layout from './components/layout/Layout';
import HomePage from './pages/HomePage';
import SearchPage from './pages/SearchPage';
import DetailPage from './pages/DetailPage';
import PlayerPage from './pages/PlayerPage';
import FavoritesPage from './pages/FavoritesPage';
import HistoryPage from './pages/HistoryPage';
import MoviePage from './pages/MoviePage';
import TVPage from './pages/TVPage';
import VarietyPage from './pages/VarietyPage';
import AnimePage from './pages/AnimePage';
import ShortDramaPage from './pages/ShortDramaPage';
import SportsPage from './pages/SportsPage';
import DonatePage from './pages/DonatePage';
import SplashScreen from './components/SplashScreen';
import DisclaimerDialog from './components/dialogs/DisclaimerDialog';
import DonationDialog from './components/dialogs/DonationDialog';
import { shouldShowStartupNotice, acknowledgeStartupNotice } from './utils/version';

type DialogState = 'none' | 'disclaimer' | 'donation';

export default function App() {
  const navigate = useNavigate();
  const [showSplash, setShowSplash] = useState(true);
  const [dialog, setDialog] = useState<DialogState>('none');

  const handleSplashFinish = useCallback(() => {
    setShowSplash(false);
    // 启动动画结束后，检查是否需要展示免责声明 + 捐赠弹窗
    if (shouldShowStartupNotice()) {
      setDialog('disclaimer');
    }
  }, []);

  /** 免责声明 —— 同意 */
  const handleAgree = useCallback(() => setDialog('donation'), []);

  /** 免责声明 —— 退出应用（通过 Rust 同时清理后端进程） */
  const handleExit = useCallback(async () => {
    try {
      const { invoke } = await import('@tauri-apps/api/core');
      await invoke('exit_app');
    } catch {
      // 网页开发环境无法退出宿主进程，关闭弹窗以便继续调试。
      setDialog('none');
    }
  }, []);

  /** 捐赠弹窗 —— 前往捐赠页 */
  const handleGoDonate = useCallback(() => {
    acknowledgeStartupNotice();
    setDialog('none');
    navigate('/donate');
  }, [navigate]);

  /** 捐赠弹窗 —— 下次再说 */
  const handleDonationClose = useCallback(() => {
    acknowledgeStartupNotice();
    setDialog('none');
  }, []);

  // 防止在展示免责声明期间误触发路由/滚动
  useEffect(() => {
    if (dialog !== 'none') {
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = '';
    }
    return () => { document.body.style.overflow = ''; };
  }, [dialog]);

  return (
    <>
      <Routes>
        <Route element={<Layout />}>
          <Route path="/" element={<HomePage />} />
          <Route path="/search" element={<SearchPage />} />
          <Route path="/detail/:siteKey/:id" element={<DetailPage />} />
          <Route path="/detail/source/:name" element={<DetailPage />} />
          <Route path="/favorites" element={<FavoritesPage />} />
          <Route path="/history" element={<HistoryPage />} />
          <Route path="/movie" element={<MoviePage />} />
          <Route path="/tv" element={<TVPage />} />
          <Route path="/variety" element={<VarietyPage />} />
          <Route path="/anime" element={<AnimePage />} />
          <Route path="/short-drama" element={<ShortDramaPage />} />
          <Route path="/sports" element={<SportsPage />} />
          <Route path="/donate" element={<DonatePage />} />
          <Route path="/player/:siteKey/:id" element={<PlayerPage />} />
        </Route>
      </Routes>

      {showSplash && <SplashScreen onFinish={handleSplashFinish} />}

      {dialog === 'disclaimer' && (
        <DisclaimerDialog onAgree={handleAgree} onExit={handleExit} />
      )}
      {dialog === 'donation' && (
        <DonationDialog onClose={handleDonationClose} onGoDonate={handleGoDonate} />
      )}
    </>
  );
}
