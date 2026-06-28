import { Radio } from 'lucide-react';
import CategoryPage, { type CategoryConfig } from '../components/category/CategoryPage';

const config: CategoryConfig = {
  key: 'anime', label: '动漫', Icon: Radio,
  categoryId: 'anime', searchKeyword: '动漫',
  subtitle: '日漫 & 国漫 & 剧场版',
  subCategories: [
    { label: '日漫', typeMatch: '日韩|日本|日漫' }, { label: '国漫', typeMatch: '国产|国漫' },
    { label: '欧美', typeMatch: '欧美' }, { label: '剧场版', typeMatch: '剧场|电影' },
  ],
};
export default function AnimePage() { return <CategoryPage config={config} />; }
