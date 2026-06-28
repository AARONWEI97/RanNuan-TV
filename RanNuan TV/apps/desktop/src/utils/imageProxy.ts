/**
 * 统一的图片代理函数
 */
export function proxyImg(rawUrl: string): string {
  if (!rawUrl) return '';

  // 已经是代理 URL，直接返回
  if (rawUrl.includes('/api/img')) return rawUrl;

  // 暴风资源需要代理
  if (rawUrl.includes('bfzy') || rawUrl.includes('picbf')) {
    return `http://localhost:3000/api/img?url=${encodeURIComponent(rawUrl)}`;
  }

  // 豆瓣图片防盗链，需要代理
  if (rawUrl.includes('doubanio.com') || rawUrl.includes('douban.com')) {
    return `http://localhost:3000/api/img?url=${encodeURIComponent(rawUrl)}`;
  }

  // 其他直连
  return rawUrl;
}
