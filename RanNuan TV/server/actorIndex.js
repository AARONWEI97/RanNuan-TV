/**
 * 演员/导演倒排索引模块 v2
 *
 * 修复清单：
 *   #4 isLikelyActor 支持 2-6 字中文 + 英文全名
 *   #5 演员名解析：去括号、英文全名不拆空格、过滤数字和超长名
 *   #6 searchByActor：精确名优先 + prefix 匹配，去掉 kw.includes(name) 误伤
 *   #7 slimItem 保留 sites 字段
 *   #9 每演员名最多 200 条，防止索引膨胀
 */

const fs = require('fs');

// ===== 配置 =====
const MAX_ITEMS_PER_ACTOR = 200; // 每个演员名下最多保留的条目数

// ===== 倒排索引 =====
const actorIndex = new Map();
const directorIndex = new Map();
const indexedIds = new Set(); // "site_key:vod_id"

// ===== 工具函数 =====

/** 单条记录的标识符 */
function itemFingerprint(item) {
    return `${item.site_key || ''}:${item.vod_id || ''}`;
}

function slimIndexItem(item) {
    return {
        vod_id: item.vod_id,
        vod_name: item.vod_name,
        vod_pic: item.vod_pic,
        vod_remarks: item.vod_remarks,
        vod_year: item.vod_year,
        vod_actor: item.vod_actor,
        vod_director: item.vod_director,
        type_name: item.type_name,
        site_key: item.site_key,
        site_name: item.site_name,
        sites: item.sites || (item.site_key ? [{ key: item.site_key, name: item.site_name, id: item.vod_id }] : []),
    };
}

/**
 * 解析演员/导演名列表
 * 规则：
 *   - 去括号内容：张三(饰小明) → 张三
 *   - 英文全名保留：Jackie Chan 不拆空格
 *   - 中文名按分隔符拆分
 *   - 过滤纯数字 / 超长名
 */
function parseNames(raw) {
    if (!raw) return [];
    // 1. 去括号及括号内容
    let cleaned = raw.replace(/[（(].*?[）)]/g, ' ').trim();
    // 2. 按显式分隔符拆分。不要按空格拆，否则 "Tom Hanks" 会被拆坏。
    const segments = cleaned.split(/[,，、/;；]+/).map(s => s.trim()).filter(Boolean);
    const result = [];
    for (const seg of segments) {
        // 英文全名（如 "Jackie Chan"）：不再二次拆分
        if (/^[A-Za-z\u00C0-\u024F][A-Za-z\u00C0-\u024F\s.\-']+$/.test(seg) && seg.includes(' ')) {
            if (seg.length >= 3 && seg.length <= 30) result.push(seg);
        } else {
            // 中文或其他：直接作为单个名字
            if (seg.length >= 2 && seg.length <= 10 && !/^\d+$/.test(seg)) {
                result.push(seg);
            }
        }
    }
    return [...new Set(result)]; // 去重
}

/**
 * 判断文本是否像演员/导演人名
 * #4 修复：中文 2-6 字 + 英文全名支持
 */
function isLikelyActor(wd) {
    const trimmed = (wd || '').trim();
    if (!trimmed) return false;

    // 纯中文 2~6 字
    const chineseOnly = /^[\u4e00-\u9fa5]{2,6}$/.test(trimmed);
    // 英文名如 "Jackie Chan" / "Tom Hanks"
    const englishLike = /^[A-Za-z\u00C0-\u024F][A-Za-z\u00C0-\u024F\s.\-']{2,30}$/.test(trimmed) && trimmed.length >= 3;

    if (!chineseOnly && !englishLike) return false;

    // 排除影视分类/类型关键词
    const excludePatterns = /(片|剧|综艺|动漫|体育|电影|电视|记录|短剧|动画|少儿|脱口|真人|纪[录实]|新[闻]|直[播]|音[乐]|纪录|纪实)/;
    if (excludePatterns.test(trimmed)) return false;

    return true;
}

// ===== 索引操作 =====

/**
 * 将一批 MediaItem 写入索引
 */
function indexItems(items) {
    if (!Array.isArray(items) || items.length === 0) return 0;
    let addedCount = 0;

    for (const item of items) {
        const fp = itemFingerprint(item);
        if (!fp || fp === ':') continue;
        if (indexedIds.has(fp)) continue;
        indexedIds.add(fp);

        const actors = parseNames(item.vod_actor);
        const directors = parseNames(item.vod_director);

        if (actors.length === 0 && directors.length === 0) continue;

        // 索引只保留搜索卡片字段；详情内容点击后再按 ID 获取，避免索引文件膨胀。
        const slimItem = slimIndexItem(item);

        // 写入演员索引
        for (const name of actors) {
            _addToIndex(actorIndex, name, slimItem);
            addedCount++;
        }

        // 写入导演索引
        for (const name of directors) {
            _addToIndex(directorIndex, name, slimItem);
            addedCount++;
        }
    }

    return addedCount;
}

function _addToIndex(map, name, item) {
    if (!map.has(name)) map.set(name, []);
    const list = map.get(name);
    // 避免重复
    if (list.some(e => e.vod_id === item.vod_id && e.site_key === item.site_key)) return;
    list.push(item);
    // #9 修复：每个演员名最多 200 条
    if (list.length > MAX_ITEMS_PER_ACTOR) {
        list.shift(); // FIFO 淘汰最旧条目
    }
}

/**
 * 按演员/导演名搜索索引
 * #6 修复：精确优先 → 前缀匹配 → contains 匹配
 */
function searchByActor(keyword) {
    const kw = keyword.trim();
    const lowerKw = kw.toLowerCase();
    const actorHits = [];
    const directorHits = [];
    let exactMatches = [];
    let prefixMatches = [];
    let containsMatches = [];

    // 演员索引查找
    for (const [name, items] of actorIndex) {
        const lowerName = name.toLowerCase();
        if (lowerName === lowerKw) {
            exactMatches.push({ name, items, score: 3 });
        } else if (lowerName.startsWith(lowerKw) && kw.length >= 2) {
            prefixMatches.push({ name, items, score: 2 });
        } else if (lowerName.includes(lowerKw) && kw.length >= 2) {
            containsMatches.push({ name, items, score: 1 });
        }
    }

    // 按分数排序，然后合并
    const sorted = [...exactMatches, ...prefixMatches, ...containsMatches];
    const seenActor = new Set();
    for (const { items } of sorted) {
        for (const item of items) {
            const key = itemFingerprint(item);
            if (!seenActor.has(key)) {
                seenActor.add(key);
                actorHits.push(item);
            }
        }
    }

    // 导演索引（同样逻辑）
    exactMatches = [];
    prefixMatches = [];
    containsMatches = [];
    for (const [name, items] of directorIndex) {
        const lowerName = name.toLowerCase();
        if (lowerName === lowerKw) {
            exactMatches.push({ name, items, score: 3 });
        } else if (lowerName.startsWith(lowerKw) && kw.length >= 2) {
            prefixMatches.push({ name, items, score: 2 });
        } else if (lowerName.includes(lowerKw) && kw.length >= 2) {
            containsMatches.push({ name, items, score: 1 });
        }
    }
    const sortedDir = [...exactMatches, ...prefixMatches, ...containsMatches];
    const seenDir = new Set(seenActor); // 演员已出现的跳过
    for (const { items } of sortedDir) {
        for (const item of items) {
            const key = itemFingerprint(item);
            if (!seenDir.has(key)) {
                seenDir.add(key);
                directorHits.push(item);
            }
        }
    }

    // 合并（演员优先）
    const allSeen = new Set();
    const all = [];
    for (const item of [...actorHits, ...directorHits]) {
        const key = itemFingerprint(item);
        if (!allSeen.has(key)) {
            allSeen.add(key);
            all.push(item);
        }
    }

    return { actorHits, directorHits, all };
}

/** 检查索引中是否有指定关键词的条目 */
function hasIndexFor(keyword) {
    const lowerKw = String(keyword || '').trim().toLowerCase();
    for (const [name] of actorIndex) {
        const lowerName = name.toLowerCase();
        if (lowerName === lowerKw || lowerName.includes(lowerKw)) return true;
    }
    for (const [name] of directorIndex) {
        const lowerName = name.toLowerCase();
        if (lowerName === lowerKw || lowerName.includes(lowerKw)) return true;
    }
    return false;
}

/** 获取索引统计 */
function getStats() {
    const actorNames = Array.from(actorIndex.keys());
    const directorNames = Array.from(directorIndex.keys());
    let totalEntries = 0;
    for (const items of actorIndex.values()) totalEntries += items.length;
    for (const items of directorIndex.values()) totalEntries += items.length;
    return {
        uniqueActors: actorNames.length,
        uniqueDirectors: directorNames.length,
        totalIndexedEntries: totalEntries,
        indexedVideoIds: indexedIds.size,
        sampleActors: actorNames.slice(0, 15),
        sampleDirectors: directorNames.slice(0, 15),
    };
}

// ===== 持久化 =====

function saveToDisk(filePath) {
    const data = {
        actors: Array.from(actorIndex.entries()),
        directors: Array.from(directorIndex.entries()),
        indexedIds: Array.from(indexedIds),
        savedAt: new Date().toISOString(),
    };
    try {
        fs.writeFileSync(filePath, JSON.stringify(data));
        console.log(`[ActorIndex] 已保存: ${actorIndex.size} 位演员, ${directorIndex.size} 位导演, ${indexedIds.size} 条记录`);
        return true;
    } catch (e) {
        console.error('[ActorIndex] 保存失败:', e.message);
        return false;
    }
}

function loadFromDisk(filePath) {
    if (!fs.existsSync(filePath)) {
        console.log('[ActorIndex] 索引文件不存在，将从头开始构建');
        return false;
    }
    try {
        const raw = fs.readFileSync(filePath, 'utf-8');
        const data = JSON.parse(raw);

        actorIndex.clear();
        directorIndex.clear();
        indexedIds.clear();

        if (Array.isArray(data.actors)) {
            for (const [name, items] of data.actors) {
                actorIndex.set(name, Array.isArray(items) ? items.map(slimIndexItem) : []);
            }
        }
        if (Array.isArray(data.directors)) {
            for (const [name, items] of data.directors) {
                directorIndex.set(name, Array.isArray(items) ? items.map(slimIndexItem) : []);
            }
        }
        if (Array.isArray(data.indexedIds)) {
            for (const id of data.indexedIds) {
                indexedIds.add(id);
            }
        }
        console.log(`[ActorIndex] 已恢复: ${actorIndex.size} 位演员, ${directorIndex.size} 位导演, ${indexedIds.size} 条记录 (${data.savedAt || '未知时间'})`);
        return true;
    } catch (e) {
        console.error('[ActorIndex] 恢复失败:', e.message);
        return false;
    }
}

module.exports = {
    indexItems,
    searchByActor,
    isLikelyActor,
    hasIndexFor,
    getStats,
    saveToDisk,
    loadFromDisk,
};
