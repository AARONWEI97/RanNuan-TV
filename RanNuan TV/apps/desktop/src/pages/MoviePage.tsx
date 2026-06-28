import { Film } from 'lucide-react';
import CategoryPage, { type CategoryConfig } from '../components/category/CategoryPage';

const config: CategoryConfig = {
  key: 'movie', label: '电影', Icon: Film,
  categoryId: 'movie', searchKeyword: '电影',
  subtitle: '院线大片 & 经典影片',
  subCategories: [
    { label: '动作', typeMatch: '动作' }, { label: '喜剧', typeMatch: '喜剧' },
    { label: '爱情', typeMatch: '爱情' }, { label: '科幻', typeMatch: '科幻' },
    { label: '恐怖', typeMatch: '恐怖|惊悚' }, { label: '剧情', typeMatch: '剧情' },
    { label: '战争', typeMatch: '战争' }, { label: '动画', typeMatch: '动画' },
  ],
};
export default function MoviePage() { return <CategoryPage config={config} />; }
