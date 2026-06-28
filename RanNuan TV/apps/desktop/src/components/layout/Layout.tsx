import { Outlet, useNavigate } from 'react-router-dom';
import Sidebar from './Sidebar';
import TopBar from './TopBar';
import TitleBar from './TitleBar';

export default function Layout() {
  const navigate = useNavigate();

  return (
    <div className="flex h-screen bg-zinc-950 text-white overflow-hidden">
      <Sidebar onNavigate={navigate} />
      <div className="flex-1 flex flex-col min-w-0">
        <TitleBar />
        <TopBar onNavigate={navigate} />
        <main className="flex-1 overflow-y-auto p-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
