import { Zap } from 'lucide-react';
import CategoryPage, { type CategoryConfig } from '../components/category/CategoryPage';

const config: CategoryConfig = {
  key: 'short-drama', label: '短剧', Icon: Zap,
  categoryId: 'shortDrama', searchKeyword: '短剧',
  subtitle: '快手短剧 & 精品微剧',
  // 暴风+索尼有短剧子类，量子/非凡无 → 有映射的走服务端过滤，无映射的回退客户端
  subCategories: [
    { label: '古装仙侠' }, { label: '现代言情' }, { label: '穿越年代' },
    { label: '反转爽文' }, { label: '女频总裁' }, { label: '都市脑洞' },
  ],

};
export default function ShortDramaPage() { return <CategoryPage config={config} />; }
