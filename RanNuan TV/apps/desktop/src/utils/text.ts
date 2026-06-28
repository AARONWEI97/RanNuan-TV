/** 剥离 HTML 标签（CMS 的 vod_content 带 <p> 等标签） */
export function stripHtml(html: string): string {
  return html.replace(/<[^>]+>/g, '').replace(/&nbsp;/g, ' ').trim();
}
