import { Clapperboard } from 'lucide-react';
import CategoryPage, { type CategoryConfig } from '../components/category/CategoryPage';

const config: CategoryConfig = {
  key: 'variety', label: '综艺', Icon: Clapperboard,
  categoryId: 'variety', searchKeyword: '综艺',
  subtitle: '热门综艺 & 娱乐节目',
  // 走服务端 type_id 过滤（大陆/港台/日韩/欧美综艺）
  subCategories: [
    { label: '大陆综艺' }, { label: '港台综艺' }, { label: '日韩综艺' }, { label: '欧美综艺' },
  ],
};
export default function VarietyPage() { return <CategoryPage config={config} />; }
