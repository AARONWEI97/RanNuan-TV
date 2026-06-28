import { useState } from 'react';
import { Search } from 'lucide-react';

interface Props {
  onNavigate: (path: string) => void;
}

export default function TopBar({ onNavigate }: Props) {
  const [keyword, setKeyword] = useState('');

  const handleSearch = (e: { preventDefault: () => void }) => {
    e.preventDefault();
    if (keyword.trim()) {
      onNavigate(`/search?wd=${encodeURIComponent(keyword.trim())}`);
    }
  };

  return (
    <header className="h-16 bg-zinc-900/80 backdrop-blur border-b border-zinc-800 flex items-center px-6 gap-4">
      <form onSubmit={handleSearch} className="flex-1 max-w-xl">
        <div className="relative">
          <Search
            size={18}
            className="absolute left-3 top-1/2 -translate-y-1/2 text-zinc-500"
          />
          <input
            type="text"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            placeholder="搜索影片、剧集..."
            className="w-full h-10 pl-10 pr-4 bg-zinc-800 border border-zinc-700 rounded-lg text-sm text-white placeholder-zinc-500 focus:outline-none focus:border-brand-500 focus:ring-1 focus:ring-brand-500 transition-all"
          />
        </div>
      </form>
    </header>
  );
}
