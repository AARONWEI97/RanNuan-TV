import { Tv } from 'lucide-react';
import CategoryPage, { type CategoryConfig } from '../components/category/CategoryPage';

const config: CategoryConfig = {
  key: 'tv', label: '电视剧', Icon: Tv,
  categoryId: 'tv', searchKeyword: '电视剧',
  subtitle: '热门剧集 & 持续更新',
  subCategories: [
    { label: '国产', typeMatch: '国产|大陆|内地' }, { label: '韩剧', typeMatch: '韩国|韩剧' },
    { label: '美剧', typeMatch: '欧美|美国' }, { label: '日剧', typeMatch: '日本|日剧' },
    { label: '港剧', typeMatch: '香港|港剧' }, { label: '台剧', typeMatch: '台湾|台剧' },
  ],
};
export default function TVPage() { return <CategoryPage config={config} />; }
