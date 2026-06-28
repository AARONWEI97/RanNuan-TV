import { Trophy } from 'lucide-react';
import CategoryPage, { type CategoryConfig } from '../components/category/CategoryPage';

const config: CategoryConfig = {
  key: 'sports', label: '体育', Icon: Trophy,
  categoryId: 'sports', searchKeyword: 'NBA',
  subtitle: 'NBA & 体育赛事',
  // 足球/篮球/网球/斯诺克 → 服务端 SUB_TYPE_MAP；综合 → 客户端 type_name 匹配
  subCategories: [
    { label: '足球' }, { label: '篮球' }, { label: '网球' }, { label: '斯诺克' },
    { label: '综合', typeMatch: '体育|赛事|运动' },
  ],
};
export default function SportsPage() { return <CategoryPage config={config} />; }
