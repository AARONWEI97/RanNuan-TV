import { useState } from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';

interface Props {
  /** 当前页码（从 1 开始） */
  page: number;
  /** 每页条数 */
  pageSize: number;
  /** 总条数（第 1 页估算值，仅供参考显示，不参与页码生成） */
  total: number;
  /** 翻页回调 */
  onPageChange: (page: number) => void;
  /** 每页条数变更回调 */
  onPageSizeChange?: (size: number) => void;
  /** 可选每页条数选项 */
  pageSizeOptions?: number[];
  /** 渐进式分页：用户可点击的最大页码（已加载最大页或 +1 可探索页） */
  maxPage?: number;
  /** 是否还有下一页（控制「下一页」按钮和 maxPage+1 是否可点击） */
  hasNext?: boolean;
  /** 是否显示总条数估算（左侧） */
  showTotalEstimate?: boolean;
  /** 是否禁用跳页输入（未加载的远页不可点击） */
  progressive?: boolean;
}

const PAGE_SIZE_OPTIONS = [30, 60, 90];

export default function Pagination({
  page,
  pageSize,
  total,
  onPageChange,
  onPageSizeChange,
  pageSizeOptions = PAGE_SIZE_OPTIONS,
  maxPage,
  hasNext,
  showTotalEstimate = false,
  progressive = false,
}: Props) {
  // 传统模式：用 total 算尾页；渐进模式：用 maxPage 当尾页
  const totalPages = progressive && maxPage != null
    ? maxPage
    : Math.max(1, Math.ceil(total / pageSize));

  // 无数据时不显示
  if (total === 0 && !progressive) return null;
  if (progressive && maxPage == null) return null;

  // 下一页按钮是否可用
  const canGoNext = progressive ? !!hasNext : page < totalPages;

  // 生成页码按钮列表
  const pageNumbers = progressive
    ? buildProgressivePageNumbers(page, totalPages, hasNext)
    : buildPageNumbers(page, totalPages);

  // 跳页输入
  const [jumpInput, setJumpInput] = useState('');
  const handleJump = () => {
    const target = parseInt(jumpInput, 10);
    if (isNaN(target) || target < 1) return;
    if (progressive) {
      if (target <= totalPages) {
        onPageChange(target);
      } else if (target === totalPages + 1 && hasNext) {
        onPageChange(target);
      } else {
        // 超出可点击范围，提示用户
        alert(`尚未加载到第 ${target} 页，请逐页浏览`);
      }
    } else {
      if (target <= totalPages) onPageChange(target);
    }
    setJumpInput('');
  };

  return (
    <div className="flex flex-col sm:flex-row items-center justify-between gap-4 py-6 select-none">
      {/* 左侧：总条数 */}
      <div className="flex items-center text-sm text-zinc-500">
        {showTotalEstimate ? (
          <span>
            约 <span className="text-zinc-300 font-medium">{total.toLocaleString()}</span> 条
          </span>
        ) : (
          <span>
            共 <span className="text-zinc-300 font-medium">{total.toLocaleString()}</span> 条
          </span>
        )}
      </div>

      {/* 右侧：页码按钮 */}
      <div className="flex items-center gap-1">
        {/* 上一页 */}
        <button
          onClick={() => onPageChange(page - 1)}
          disabled={page <= 1}
          className="p-2 rounded-lg text-zinc-400 hover:text-white hover:bg-zinc-800 disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
          aria-label="上一页"
        >
          <ChevronLeft size={18} />
        </button>

        {/* 页码 */}
        {pageNumbers.map((p, idx) => {
          if (p === '...') {
            return (
              <span key={`dots-${idx}`} className="w-9 text-center text-zinc-600 text-sm">
                ...
              </span>
            );
          }
          if (p === '>') {
            // 渐进模式：可探索的下一页占位符
            return (
              <button
                key="explore-next"
                onClick={() => onPageChange(totalPages + 1)}
                disabled={!hasNext}
                className="w-9 h-9 rounded-lg text-sm font-medium text-zinc-500 hover:text-brand-400 hover:bg-zinc-800 disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
                title={hasNext ? '探索下一页' : '已到底'}
              >
                {totalPages + 1}
              </button>
            );
          }
          const isActive = p === page;
          return (
            <button
              key={p}
              onClick={() => onPageChange(p as number)}
              className={`w-9 h-9 rounded-lg text-sm font-medium transition-all duration-200 ${
                isActive
                  ? 'bg-brand-500 text-white shadow-lg shadow-brand-500/20'
                  : 'text-zinc-400 hover:text-white hover:bg-zinc-800'
              }`}
            >
              {p}
            </button>
          );
        })}

        {/* 下一页 */}
        <button
          onClick={() => onPageChange(page + 1)}
          disabled={!canGoNext}
          className="p-2 rounded-lg text-zinc-400 hover:text-white hover:bg-zinc-800 disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
          aria-label="下一页"
        >
          <ChevronRight size={18} />
        </button>
      </div>

      {/* 跳页输入 */}
      <div className="flex items-center gap-1.5">
        <span className="text-xs text-zinc-600">跳至</span>
        <input
          type="text"
          value={jumpInput}
          onChange={(e) => setJumpInput(e.target.value.replace(/\D/g, ''))}
          onKeyDown={(e) => { if (e.key === 'Enter') handleJump(); }}
          placeholder={progressive ? `1~${totalPages + (hasNext ? 1 : 0)}` : `1~${totalPages}`}
          className="w-14 h-7 bg-zinc-800 border border-zinc-700 rounded-md text-xs text-center text-zinc-300 placeholder-zinc-600 focus:outline-none focus:border-brand-500"
        />
        <button
          onClick={handleJump}
          className="h-7 px-2 bg-zinc-800 hover:bg-zinc-700 rounded-md text-xs text-zinc-400 transition-colors"
        >
          跳
        </button>
      </div>
    </div>
  );
}

/** 传统模式：用 total 生成页码，始终显示尾页 */
function buildPageNumbers(current: number, total: number): (number | '...')[] {
  if (total <= 7) {
    return Array.from({ length: total }, (_, i) => i + 1);
  }

  const pages: (number | '...')[] = [];
  pages.push(1);

  if (current > 3) {
    pages.push('...');
  }

  const start = Math.max(2, current - 1);
  const end = Math.min(total - 1, current + 1);

  for (let i = start; i <= end; i++) {
    pages.push(i);
  }

  if (current < total - 2) {
    pages.push('...');
  }

  pages.push(total);
  return pages;
}

/** 渐进式模式：不显示虚拟尾页，最后一页 = maxLoadedPage */
function buildProgressivePageNumbers(
  current: number,
  maxPage: number,
  hasNext?: boolean,
): (number | '...' | '>')[] {
  const pages: (number | '...' | '>')[] = [];

  if (maxPage <= 6) {
    // 少于等于 6 页，全部显示 + 可探索下一页
    for (let i = 1; i <= maxPage; i++) pages.push(i);
    if (hasNext) pages.push('>');
    return pages;
  }

  // 始终显示第 1 页
  pages.push(1);

  if (current > 3) {
    pages.push('...');
  }

  // 当前页附近
  const start = Math.max(2, current - 1);
  const end = Math.min(maxPage, current + 1);

  for (let i = start; i <= end; i++) {
    pages.push(i);
  }

  if (current < maxPage - 2) {
    pages.push('...');
  }

  // 最后显示 maxPage（不显示虚拟 total tail page）
  if (maxPage > end) {
    pages.push(maxPage);
  }

  // 可探索下一页
  if (hasNext) {
    pages.push('>');
  }

  return pages;
}
