import { useCallback, useState } from 'react';
import { Routes, Route } from 'react-router-dom';
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
import SplashScreen from './components/SplashScreen';

export default function App() {
  const [showSplash, setShowSplash] = useState(true);
  const handleSplashFinish = useCallback(() => setShowSplash(false), []);

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
          <Route path="/player/:siteKey/:id" element={<PlayerPage />} />
        </Route>
      </Routes>
      {showSplash && <SplashScreen onFinish={handleSplashFinish} />}
    </>
  );
}
