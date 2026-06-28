import type { MediaItem } from 'shared';
import MediaCard from './MediaCard';

interface Props {
  items: MediaItem[];
  onClick: (item: MediaItem) => void;
}

export default function MediaGrid({ items, onClick }: Props) {
  return (
    <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-4">
      {items.map((item, idx) => (
        <MediaCard
          key={`${item.site_key}-${item.vod_id}-${idx}`}
          item={item}
          onClick={() => onClick(item)}
        />
      ))}
    </div>
  );
}
