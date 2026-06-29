const express = require('express');
const axios = require('axios');
const bodyParser = require('body-parser');
const cors = require('cors');
const fs = require('fs');
const path = require('path');
const { indexItems, searchByActor, isLikelyActor, hasIndexFor, getStats, saveToDisk, loadFromDisk } = require('./actorIndex');

const app = express();
const PORT = 3000;
const DATA_FILE = path.join(__dirname, 'db.json');
const ADMIN_PASSWORD = "admin"; 
const FORCE_UPDATE = true; 
const ACTOR_INDEX_FILE = path.join(__dirname, 'actor_index.json'); 
const CATEGORY_CACHE_FILE = path.join(__dirname, 'category_cache.json');

app.use(cors());
app.use(bodyParser.json());
app.use(express.static('public'));

// ==================== 站点 API 配置 ====================
// api: CMS API 地址
// detail: 网站域名（用于爬虫回退，格式参考 MoonTVPlus）
const DEFAULT_SITES = [
    { key: "lzzy",  name: "量子资源", api: "https://cj.lziapi.com/api.php/provide/vod", detail: "https://cj.lziapi.com", active: true },
    { key: "ffzy",  name: "非凡影视", api: "http://cj.ffzyapi.com/api.php/provide/vod",  detail: "http://cj.ffzyapi.com",  active: true },
    { key: "bfzy",  name: "暴风资源", api: "https://bfzyapi.com/api.php/provide/vod",     detail: "https://bfzyapi.com",     active: true },
    { key: "suoni", name: "索尼资源", api: "https://suoniapi.com/api.php/provide/vod",    detail: "https://suoniapi.com",    active: true },
    { key: "kfzy",  name: "快帆资源", api: "https://api.kuaifan.tv/api.php/provide/vod",  detail: "https://www.kuaifan.tv",  active: true },
    { key: "lszy",  name: "乐视资源", api: "https://leshiapi.com/api.php/provide/vod",    detail: "https://leshiapi.com",    active: true },
    { key: "hwk",   name: "海外看",   api: "https://haiwaikan.com/api.php/provide/vod",   detail: "https://haiwaikan.com",   active: true },
    { key: "bdzy",  name: "百度资源", api: "https://api.apibdzy.com/api.php/provide/vod", detail: "https://apibdzy.com",     active: true },
];

if (!fs.existsSync(DATA_FILE) || FORCE_UPDATE) {
    // 只有在没有文件时，或者强制更新开启时，才重置配置
    // 但为了不覆盖你可能手动添加的，我们这里只在文件不存在时写入，或者你确认要重置
    if(!fs.existsSync(DATA_FILE)) {
        fs.writeFileSync(DATA_FILE, JSON.stringify({ sites: DEFAULT_SITES }, null, 2));
    }
}

function getDB() { 
    try {
        const data = JSON.parse(fs.readFileSync(DATA_FILE));
        // 简单的合并逻辑：确保代码里的30多个接口都在数据库里
        if(FORCE_UPDATE) {
            const dbSites = data.sites || [];
            DEFAULT_SITES.forEach(defSite => {
                if(!dbSites.find(s => s.key === defSite.key)) {
                    dbSites.push(defSite);
                }
            });
            return { sites: dbSites };
        }
        return data;
    } catch(e) {
        return { sites: DEFAULT_SITES };
    }
}
function saveDB(data) { fs.writeFileSync(DATA_FILE, JSON.stringify(data, null, 2)); }

// === ★ 新增：真实测速接口 ★ ===
app.get('/api/check', async (req, res) => {
    const { key } = req.query;
    const sites = getDB().sites;
    const site = sites.find(s => s.key === key);
    
    if (!site) return res.json({ latency: 9999 });

    const start = Date.now();
    try {
        // 尝试请求该接口的首页（只请求一页，极简模式）
        await axios.get(`${site.api}?ac=videolist&pg=1`, { timeout: 3000 });
        const latency = Date.now() - start;
        res.json({ latency: latency });
    } catch (e) {
        res.json({ latency: 9999 }); // 超时或错误
    }
});

// ==================== 工具函数 ====================

// 当前可正常响应的站点（快帆/乐视/海外已死）
const WORKING_SITES = ['lzzy', 'ffzy', 'bfzy', 'suoni', 'bdzy'];

// 被墙的 CDN 域名（量子资源三个 CDN 全部被墙：ECONNRESET/TIMEOUT）
const BLOCKED_CDNS = ['pic.lzzypic.com', 'img.lzzyimg.com', 'img.lzipic.com'];

// ---- 服务端内存缓存（参考 MoonTVPlus duanju/recommends 模式） ----
const serverCache = new Map();
const CACHE_TTL = {
    home: 5 * 60 * 1000,
    hot: 3 * 60 * 1000,
    category: 10 * 60 * 1000,
    search: 5 * 60 * 1000,
}; // 分类/搜索缓存：减少重复等待
const SEARCH_RESULT_LIMIT = 200;
const SEARCH_DETAIL_ENRICH_LIMIT_PER_SITE = 24;
const ACTOR_SCAN_PAGES = 2;
const ACTOR_SCAN_TYPE_IDS_PER_CATEGORY = 2;
const ACTOR_SCAN_DETAIL_LIMIT_PER_PAGE = 18;
const CATEGORY_CACHE_VERSION = 'v2';
const CATEGORY_DISK_CACHE_TTL = 24 * 60 * 60 * 1000;
const CATEGORY_PREWARM_CATEGORIES = ['movie', 'tv', 'variety', 'anime', 'shortDrama', 'sports'];
const CATEGORY_PREWARM_PAGE_SIZES = [30, 20];
const CATEGORY_FAST_FIRST_PAGE_TIMEOUT = 3200;
let categoryCacheSaveTimer = null;
const categoryFullRefreshInFlight = new Map();

// ---- 已知站点的分类 type_id 映射（父级 + 子级） ----
// ⚠️ CMS item 挂在子分类下（动作片=6），不是父级（电影片=1），
//    所以需要把所有子 type_id 都拉一遍再合并
const SITE_TYPE_IDS = {
  lzzy: {
    movie:      [1, 6,7,8,9,10,11,12,20,34,45,49],
    tv:         [2, 13,14,15,16,21,22,23,24],
    variety:    [3, 25,26,27,28],
    anime:      [4, 29,30,31,32,33],
    shortDrama: [46],                             // 量子短剧无子类
    sports:     [36, 37,38,39,40],                // 37=足球 38=篮球 39=网球 40=斯诺克
  },
  ffzy: {
    movie:      [1, 6,7,8,9,10,11,12,20,34],
    tv:         [2, 13,14,15,16,21,22,23,24,36],
    variety:    [3, 25,26,27,28],
    anime:      [4, 29,30,31,32,33],
    shortDrama: [36],
    sports:     [],
  },
  suoni: {
    movie:      [1, 6,7,8,9,10,11,12,20,34],
    // 索尼电视剧编号完全自定：13=国产 14=欧美 15=韩 16=日 17=港 18=台 19=泰 23=海外
    tv:         [2, 13,14,15,16,17,18,19,23],
    // 索尼综艺：26=日韩 27=港台（与标准互换）
    variety:    [3, 25,26,27,28],
    // 索尼动漫：32→44港台 33→45海外
    anime:      [4, 29,30,31,44,45],
    // 索尼短剧有子类：54爽文 64女频 65反转 66古装 67年代 68脑洞 69都市 73擦边
    shortDrama: [46, 54,64,65,66,67,68,69,73],
    // 索尼体育编号不标准：48=体育 49篮球 50足球 52斯诺克
    sports:     [48, 49,50,52],
  },
  bdzy: {
    movie:      [1, 6,7,8,9,10,11,12,20,34,45,49],
    tv:         [2, 13,14,15,16,21,22,23,24],
    variety:    [3, 25,26,27,28],
    anime:      [4, 29,30,31,32,33],
    shortDrama: [46],
    sports:     [],
  },
  bfzy: {
    movie:      [20, 21,22,23,24,25,26,27,28,29,50],
    tv:         [30, 31,32,33,34,35,36,37,38],
    variety:    [45, 46,47,48,49],
    anime:      [39, 40,41,42,43,44],
    // 暴风短剧有子类：65重生 66穿越 67言情 68反转 69总裁 70闪婚 71脑洞 72仙侠 74AI漫
    shortDrama: [58, 65,66,67,68,69,70,71,72,74],
    sports:     [53, 54,55,56,57],                   // 54=足球 55=篮球 56=网球 57=斯诺克
  },
};

// 父级 type_id 对照。部分资源站父级分页不稳定，分类页默认仍使用 SITE_TYPE_IDS 聚合；
// 这里只保留给后续快速首屏/预热策略使用，不能直接作为唯一分页来源。
const SITE_ROOT_TYPE_IDS = {
  lzzy:  { movie: 1,  tv: 2,  variety: 3,  anime: 4,  shortDrama: 46, sports: 36 },
  ffzy:  { movie: 1,  tv: 2,  variety: 3,  anime: 4,  shortDrama: 36 },
  suoni: { movie: 1,  tv: 2,  variety: 3,  anime: 4,  shortDrama: 46, sports: 48 },
  bdzy:  { movie: 1,  tv: 2,  variety: 3,  anime: 4,  shortDrama: 46 },
  bfzy:  { movie: 20, tv: 30, variety: 45, anime: 39, shortDrama: 58, sports: 53 },
};
// 分类名到站点 type_name 的映射（用于动态发现其他站点）
const CATEGORY_TYPE_NAMES = {
  movie:      ['电影片', '电影'],
  tv:         ['连续剧', '电视剧'],
  variety:    ['综艺片', '综艺'],
  anime:      ['动漫片', '动漫'],
  shortDrama: ['短剧', '短剧大全'],
  sports:     ['体育', '体育赛事'],
};

// ===== 子分类 → 各站点 type_id 映射（服务端精准过滤，替代前端 typeMatch） =====
// 对照 COLLECTION_RULES.md 的标准 MACCMS 编号：
//   标准站(lzzy/ffzy/suoni/bdzy) — 电影：6动作 7喜剧 8爱情 9科幻 10恐怖 11剧情 12战争 20记录
//   标准站 — 电视剧：13国产 14香港 15韩国 16欧美 21台湾 22日本 23海外 24泰国
//   标准站 — 动漫：29国产 30日韩 31欧美 32港台 33海外
//   标准站 — 综艺：25大陆 26港台 27日韩 28欧美
//   bfzy(自定义) — 电影：21动作 22喜剧 23恐怖 24科幻 25爱情 26剧情 27战争 50动画
//   bfzy — 电视剧：31国产 32欧美 33香港 34韩国 35台湾 36日本 37海外 38泰国
//   bfzy — 动漫：40国产 41日韩 42欧美 43港台 44海外
//   bfzy — 综艺：46大陆 47港台 48日韩 49欧美
const SUB_TYPE_MAP = {
  movie: {
    '动作': { lzzy: 6,  ffzy: 6,  suoni: 6,  bdzy: 6,  bfzy: 21 },
    '喜剧': { lzzy: 7,  ffzy: 7,  suoni: 7,  bdzy: 7,  bfzy: 22 },
    '爱情': { lzzy: 8,  ffzy: 8,  suoni: 8,  bdzy: 8,  bfzy: 25 },
    '科幻': { lzzy: 9,  ffzy: 9,  suoni: 9,  bdzy: 9,  bfzy: 24 },
    '恐怖': { lzzy: 10, ffzy: 10, suoni: 10, bdzy: 10, bfzy: 23 },
    '剧情': { lzzy: 11, ffzy: 11, suoni: 11, bdzy: 11, bfzy: 26 },
    '战争': { lzzy: 12, ffzy: 12, suoni: 12, bdzy: 12, bfzy: 27 },
    // 「动画」标准站无独立 movie 动画 type_id（20=纪录片），仅 bfzy 有 50=动画片
    '动画': { bfzy: 50 },
  },
  tv: {
    // 索尼编号不标准：13=国产 14=欧美 15=韩 16=日 17=港 18=台 19=泰
    '国产': { lzzy: 13, ffzy: 13, suoni: 13, bdzy: 13, bfzy: 31 },
    '港剧': { lzzy: 14, ffzy: 14, suoni: 17, bdzy: 14, bfzy: 33 },
    '台剧': { lzzy: 21, ffzy: 21, suoni: 18, bdzy: 21, bfzy: 35 },
    '日剧': { lzzy: 22, ffzy: 22, suoni: 16, bdzy: 22, bfzy: 36 },
    '韩剧': { lzzy: 15, ffzy: 15, suoni: 15, bdzy: 15, bfzy: 34 },
    '美剧': { lzzy: 16, ffzy: 16, suoni: 14, bdzy: 16, bfzy: 32 },
    '泰剧': { lzzy: 24, ffzy: 24, suoni: 19, bdzy: 24, bfzy: 38 },
  },
  anime: {
    // 索尼动漫：44=港台(标准32) 45=海外(标准33)，无独立剧场版 type_id
    '日漫':   { lzzy: 30, ffzy: 30, suoni: 30, bdzy: 30, bfzy: 41 },
    '国漫':   { lzzy: 29, ffzy: 29, suoni: 29, bdzy: 29, bfzy: 40 },
    '欧美':   { lzzy: 31, ffzy: 31, suoni: 31, bdzy: 31, bfzy: 42 },
    '剧场版': { lzzy: 29, ffzy: 29, bdzy: 29, bfzy: 39 },
  },
  variety: {
    // 索尼综艺编号不标准：26=日韩 27=港台（标准站 26=港台 27=日韩，互换！）
    '大陆综艺': { lzzy: 25, ffzy: 25, suoni: 25, bdzy: 25, bfzy: 46 },
    '港台综艺': { lzzy: 26, ffzy: 26, suoni: 27, bdzy: 26, bfzy: 47 },
    '日韩综艺': { lzzy: 27, ffzy: 27, suoni: 26, bdzy: 27, bfzy: 48 },
    '欧美综艺': { lzzy: 28, ffzy: 28, suoni: 28, bdzy: 28, bfzy: 49 },
  },
  // 体育：量子/索尼/暴风均有子类，标准站 lzzy=37-40, 索尼=49-52, bfzy=54-57
  sports: {
    '足球':   { lzzy: 37, bfzy: 54, suoni: 50 },
    '篮球':   { lzzy: 38, bfzy: 55, suoni: 49 },
    '网球':   { lzzy: 39, bfzy: 56 },
    '斯诺克': { lzzy: 40, bfzy: 57, suoni: 52 },
  },
  // 短剧：仅暴风和索尼有子类
  shortDrama: {
    '现代言情': { bfzy: 67, suoni: 69 },
    '古装仙侠': { bfzy: 72, suoni: 66 },
    '穿越年代': { bfzy: 66, suoni: 67 },
    '反转爽文': { bfzy: 68, suoni: 65 },
    '女频总裁': { bfzy: 69, suoni: 64 },
    '都市脑洞': { bfzy: 71, suoni: 68 },
  },
};

/** 根据 category + subLabel 获取各站点对应的 type_id，未匹配返回 null（回退前端过滤） */
function getSubTypeIds(category, subLabel) {
  const map = SUB_TYPE_MAP[category];
  if (!map) return null;
  return map[subLabel] || null;
}

/** 简单并发限制器：一次最多执行 limit 个 promise */
async function batchLimit(tasks, limit = 5) {
  const results = [];
  const executing = new Set();
  for (let i = 0; i < tasks.length; i++) {
    const p = tasks[i]().then(r => { results[i] = r; executing.delete(p); return r; });
    executing.add(p);
    if (executing.size >= limit) await Promise.race(executing);
  }
  await Promise.all(executing);
  return results;
}

/** 兜底扫描优先使用的 type_id（常见大分类，非父级 id） */
function getPriorityScanTypeIds(siteKey, category) {
    const PRIORITY = {
    movie:  { lzzy: [6,7,8,10,11,9,12], ffzy: [6,7,8,10,11,9,12], suoni: [6,7,8,10,11,9,12], bdzy: [6,7,8,10,11,9,12], bfzy: [21,22,25,26,23,24,27] },
    tv:     { lzzy: [13,15,16,14,21,22], ffzy: [13,15,16,14,21,22], suoni: [13,15,16,14,21,22], bdzy: [13,15,16,14,21,22], bfzy: [31,34,32,33,35,36] },
    variety:{ lzzy: [25,26,27,28], ffzy: [25,26,27,28], suoni: [25,26,27,28], bdzy: [25,26,27,28], bfzy: [46,47,48,49] },
    anime:  { lzzy: [29,30,31,32,33], ffzy: [29,30,31,32,33], suoni: [29,30,31,44,45], bdzy: [29,30,31,32,33], bfzy: [40,41,42,43,44] },
    sports: { lzzy: [37,38,39,40], ffzy: [], suoni: [49,50,52], bdzy: [], bfzy: [54,55,56,57] },
    };
    return PRIORITY[category]?.[siteKey] || [];
}
// 站点 class 列表缓存（key = site.key）
const siteClassCache = new Map();

/** 获取站点某分类的所有 type_id（父+子，优先内置映射） */
async function getSiteCategoryTypeIds(site, category) {
    // 1. 内置映射（返回完整子分类列表）
    if (SITE_TYPE_IDS[site.key] && SITE_TYPE_IDS[site.key][category]) {
        const ids = SITE_TYPE_IDS[site.key][category];
        if (ids.length === 0) return null; // sports=[] 表示不支持
        return ids;
    }
    // 2. 动态发现：取 class 列表中所有匹配 type_name 的 type_id（含子类）
    let classes = siteClassCache.get(site.key);
    if (!classes) {
        try {
            const r = await axios.get(`${site.api}?ac=videolist&pg=1&out=json`, { timeout: 6000 });
            classes = r.data.class || [];
            siteClassCache.set(site.key, classes);
        } catch { return null; }
    }
    const names = CATEGORY_TYPE_NAMES[category] || [];
    const matched = classes.filter(c => names.some(n => (c.type_name || '').includes(n)));
    if (matched.length === 0) return null;
    return matched.map(c => c.type_id);
}

function getCached(key) {
    const entry = serverCache.get(key);
    if (entry && Date.now() - entry.time < entry.ttl) return entry.data;
    serverCache.delete(key);
    return null;
}
/** 获取缓存条目（含 complete 标记） */
function getCachedEntry(key) {
    const entry = serverCache.get(key);
    if (entry && Date.now() - entry.time < entry.ttl) return entry;
    serverCache.delete(key);
    return null;
}
function setCache(key, data, ttl) {
    serverCache.set(key, { data, time: Date.now(), ttl, complete: true });
    if (isCategoryPageCacheKey(key)) scheduleCategoryCacheSave();
    cleanCache();
}
/** 设置不完整缓存（后台还在拉取中） */
function setCachePartial(key, data, ttl) {
    serverCache.set(key, { data, time: Date.now(), ttl, complete: false });
    cleanCache();
}
/** 标记缓存为完整 */
function markCacheComplete(key) {
    const entry = serverCache.get(key);
    if (entry) entry.complete = true;
}
function cleanCache() {
    if (serverCache.size > 100) {
        const oldest = [...serverCache.entries()].sort((a, b) => a[1].time - b[1].time)[0];
        if (oldest) serverCache.delete(oldest[0]);
    }
}

function buildCategoryMetaKey(categoryOrWd, sub) {
    const subLabel = sub ? `:${sub}` : '';
    return `${CATEGORY_CACHE_VERSION}:category-meta:${categoryOrWd}${subLabel}`;
}

function buildCategoryPageCacheKey(categoryOrWd, sub, page, pageSize) {
    const subLabel = sub ? `:${sub}` : '';
    return `${CATEGORY_CACHE_VERSION}:category:${categoryOrWd}${subLabel}:${page}:${pageSize}`;
}

function isCategoryPageCacheKey(key) {
    return typeof key === 'string' && key.startsWith(`${CATEGORY_CACHE_VERSION}:category:`);
}

function withTimeout(promise, timeoutMs, fallbackValue) {
    let timer = null;
    const timeout = new Promise(resolve => {
        timer = setTimeout(() => resolve(fallbackValue), timeoutMs);
    });
    return Promise.race([promise, timeout]).finally(() => {
        if (timer) clearTimeout(timer);
    });
}

function scheduleCategoryCacheSave() {
    if (categoryCacheSaveTimer) return;
    categoryCacheSaveTimer = setTimeout(() => {
        categoryCacheSaveTimer = null;
        saveCategoryCacheToDisk();
    }, 1200);
    if (categoryCacheSaveTimer.unref) categoryCacheSaveTimer.unref();
}

function getWorkingSites() {
  const all = getDB().sites.filter(s => s.active);
  return all.filter(s => WORKING_SITES.includes(s.key));
}

function getFastCategoryTypeIds(siteKey, category) {
    const rootId = SITE_ROOT_TYPE_IDS[siteKey]?.[category];
    const priorityIds = getPriorityScanTypeIds(siteKey, category).slice(0, 2);
    return [...new Set([rootId, ...priorityIds].filter(Boolean))];
}

function normalizeSearchKeyword(wd) {
    return String(wd || '').trim().replace(/\s+/g, ' ');
}

function itemKey(item) {
    return `${item.site_key || item.siteKey || ''}_${item.vod_id || item.vodId || ''}`;
}

function appendUnique(target, seen, items) {
    for (const item of items || []) {
        const key = itemKey(item);
        if (!key || key === '_') continue;
        if (seen.has(key)) continue;
        seen.add(key);
        target.push(item);
    }
}

async function fetchSiteDetails(site, ids, timeout = 5500) {
    const uniqueIds = [...new Set((ids || []).filter(Boolean))];
    if (uniqueIds.length === 0) return [];
    try {
        const resp = await axios.get(
            `${site.api}?ac=detail&ids=${uniqueIds.join(',')}&out=json`,
            { timeout }
        );
        const list = resp.data.list || resp.data.data || [];
        if (Array.isArray(list)) {
            return list.map(item => ({ ...item, site_key: site.key, site_name: site.name, latency: 0 }));
        }
        if (list && list.vod_id) return [{ ...list, site_key: site.key, site_name: site.name, latency: 0 }];
        if (resp.data?.vod_id) return [{ ...resp.data, site_key: site.key, site_name: site.name, latency: 0 }];
    } catch {}
    return [];
}

async function enrichSearchItemsWithDetails(items, sites, keyword, limitPerSite = SEARCH_DETAIL_ENRICH_LIMIT_PER_SITE) {
    if (!Array.isArray(items) || items.length === 0) return [];
    const siteMap = new Map(sites.map(site => [site.key, site]));
    const bySite = new Map();
    for (const item of items) {
        const siteKey = item.site_key || item.siteKey;
        if (!siteKey || !item.vod_id) continue;
        if (!bySite.has(siteKey)) bySite.set(siteKey, []);
        const list = bySite.get(siteKey);
        if (list.length < limitPerSite) list.push(item.vod_id);
    }

    const detailGroups = await Promise.all([...bySite.entries()].map(async ([siteKey, ids]) => {
        const site = siteMap.get(siteKey);
        if (!site) return [];
        return fetchSiteDetails(site, ids);
    }));
    const details = detailGroups.flat();
    if (details.length > 0) {
        const indexed = indexItems(details);
        if (indexed > 0) console.log(`[ActorIndex] 搜索"${keyword}"详情补全新增 ${indexed} 条索引`);
    }
    return details;
}

function hasPersonMatch(item, keyword) {
    const kw = normalizeSearchKeyword(keyword).toLowerCase();
    if (!kw) return false;
    return String(item.vod_actor || '').toLowerCase().includes(kw)
        || String(item.vod_director || '').toLowerCase().includes(kw);
}

async function actorFallbackScan(wd, sites, seenIds) {
    const keyword = normalizeSearchKeyword(wd);
    const results = [];
    const scanSeen = new Set(seenIds);
    const scanCategories = ['movie', 'tv'];
    const tasks = [];

    for (const site of sites) {
        for (const category of scanCategories) {
            const tids = getPriorityScanTypeIds(site.key, category).slice(0, ACTOR_SCAN_TYPE_IDS_PER_CATEGORY);
            for (const tid of tids) {
                for (let pg = 1; pg <= ACTOR_SCAN_PAGES; pg++) {
                    tasks.push(async () => {
                        try {
                            const r = await axios.get(`${site.api}?ac=videolist&t=${tid}&pg=${pg}&out=json`, { timeout: 5000 });
                            const list = r.data.list || r.data.data || [];
                            if (!Array.isArray(list) || list.length === 0) return [];
                            const candidates = list.slice(0, ACTOR_SCAN_DETAIL_LIMIT_PER_PAGE)
                                .filter(item => !seenIds.has(`${site.key}_${item.vod_id}`));
                            if (candidates.length === 0) return [];
                            const details = await fetchSiteDetails(site, candidates.map(item => item.vod_id), 5500);
                            return details.filter(item => hasPersonMatch(item, keyword));
                        } catch {
                            return [];
                        }
                    });
                }
            }
        }
    }

    const groups = await batchLimit(tasks, 6);
    for (const group of groups) appendUnique(results, scanSeen, group);
    if (results.length > 0) {
        const indexed = indexItems(results);
        if (indexed > 0) console.log(`[ActorIndex] 演员兜底扫描"${keyword}"新增 ${indexed} 条索引`);
    }
    return results;
}

/** 是否图片 URL 来自被墙的 CDN */
function isBlockedPic(url) {
    if (!url) return false;
    return BLOCKED_CDNS.some(d => url.includes(d));
}

/** 演员/导演搜索二次匹配：vod_actor/vod_director 包含关键词的条目排到前面 */
function boostActorMatches(items, keyword) {
    if (!keyword) return items;
    const kw = keyword.trim();
    if (kw.length < 2) return items;
    
    return [...items].sort((a, b) => {
        const aActor = (a.vod_actor || '').includes(kw) ? 1 : 0;
        const bActor = (b.vod_actor || '').includes(kw) ? 1 : 0;
        const aDirector = (a.vod_director || '').includes(kw) ? 1 : 0;
        const bDirector = (b.vod_director || '').includes(kw) ? 1 : 0;
        const aName = (a.vod_name || '').includes(kw) ? 2 : 0;
        const bName = (b.vod_name || '').includes(kw) ? 2 : 0;
        
        const scoreA = aName + aActor + aDirector;
        const scoreB = bName + bActor + bDirector;
        return scoreB - scoreA;
    });
}

/** 跨站点图片优选：同名视频，优先保留有可访问 vod_pic 的条目（bfzy > 有图非墙 > lzzy > 无图）
 *  每个合并条目附加 sites 字段记录所有可用站点 */
function crossSiteImagePicks(items, limit) {
    const groups = new Map();
    items.forEach(item => {
        // 激进归一化：去特殊字符+括号内容+年份季数+保留核心片名
        const raw = (item.vod_name || '').replace(/[·・\s\-\[\]【】]/g, '').toLowerCase();
        const key = raw
            .replace(/[\（\(].*?[\）\)]/g, '')        // 去括号内容（年份、备注等）
            .replace(/\d{4}/g, '')                    // 去4位年份
            .replace(/第[一二三四五六七八九十\d]+季/g, '');  // 去第X季
        const trimmed = key.substring(0, 15) || raw.substring(0, 15);
        if (!groups.has(trimmed)) groups.set(trimmed, []);
        groups.get(trimmed).push(item);
    });
    
    const picked = [];
    groups.forEach(group => {
        // 收集所有可用站点信息
        const siteSet = new Map();
        group.forEach(item => {
            const sk = item.site_key;
            if (sk && !siteSet.has(sk)) {
                siteSet.set(sk, { key: sk, name: item.site_name || sk, id: item.vod_id });
            }
        });
        
        // 排序：bfzy 有图 > 其他有图且非墙 > 有图被墙 > 无图
        group.sort((a, b) => {
            const aHas = a.vod_pic && a.vod_pic.startsWith('http');
            const bHas = b.vod_pic && b.vod_pic.startsWith('http');
            const aBfzy = a.site_key === 'bfzy';
            const bBfzy = b.site_key === 'bfzy';
            const aBlocked = isBlockedPic(a.vod_pic);
            const bBlocked = isBlockedPic(b.vod_pic);
            
            const scoreA = (aHas ? 10 : 0) + (aBfzy ? 5 : 0) - (aBlocked ? 3 : 0);
            const scoreB = (bHas ? 10 : 0) + (bBfzy ? 5 : 0) - (bBlocked ? 3 : 0);
            return scoreB - scoreA;
        });
        const best = { ...group[0], sites: Array.from(siteSet.values()) };
        picked.push(best);
    });
    
    return picked.slice(0, limit || picked.length);
}

// === 热门接口（只请求有效站点，坚决排除短剧） ===
app.get('/api/hot', async (req, res) => {
    const cached = getCached('hot');
    if (cached) return res.json(cached);
    
    const sites = getWorkingSites();
    const hotKeywords = ['电视剧', '电影', '综艺', '动漫'];
    
    const kwPromises = hotKeywords.map(async (kw) => {
        const results = [];
        for (const site of sites) {
            if (results.length >= 10) break;
            try {
                const resp = await axios.get(
                    `${site.api}?ac=videolist&wd=${encodeURIComponent(kw)}&out=json`,
                    { timeout: 5000 }
                );
                const list = (resp.data.list || resp.data.data || []);
                const filtered = list.filter(item => {
                    const t = (item.type_name || '').toLowerCase();
                    const r = (item.vod_remarks || '').toLowerCase();
                    return !t.includes('短剧') && !r.includes('短剧');
                });
                for (const item of filtered) {
                    results.push({ ...item, site_key: site.key, site_name: site.name, latency: 0 });
                }
            } catch {}
        }
        return results;
    });
    
    const allKwResults = await Promise.all(kwPromises);
    const merged = allKwResults.flat();
    const imageFirst = crossSiteImagePicks(merged);
    
    // 交错排列
    const buckets = [[], [], [], []];
    const labels = ['电视剧', '电影', '综艺', '动漫'];
    imageFirst.forEach(item => {
        for (let i = 0; i < labels.length; i++) {
            if ((item.type_name || '').includes(labels[i])) { buckets[i].push(item); return; }
        }
        buckets[0].push(item);
    });
    const interleaved = [];
    let idx = 0;
    while (interleaved.length < 24) {
        const bucket = buckets[idx % buckets.length];
        if (bucket.length > 0) interleaved.push(bucket.shift());
        idx++;
        if (buckets.every(b => b.length === 0)) break;
    }
    
    const result = { list: interleaved };
    setCache('hot', result, CACHE_TTL.hot);
    res.json(result);
});

// === 首页批处理接口（服务端缓存 5 分钟，第一人触发聚合，后续秒回） ===
app.get('/api/home', async (req, res) => {
    // 检查缓存
    const cached = getCached('home');
    if (cached) return res.json(cached);
    
    const sites = getWorkingSites(); // 5 个可用站点
    
    const hotKeywords = ['电视剧', '电影', '综艺', '动漫'];
    const fetchHot = async () => {
        const kwPromises = hotKeywords.map(async (kw) => {
            const results = [];
            for (const site of sites) {
                if (results.length >= 8) break;
                try {
                    const resp = await axios.get(
                        `${site.api}?ac=videolist&wd=${encodeURIComponent(kw)}&out=json`,
                        { timeout: 5000 }
                    );
                    const list = (resp.data.list || resp.data.data || []);
                    const filtered = list.filter(item => {
                        const t = (item.type_name || '').toLowerCase();
                        const r = (item.vod_remarks || '').toLowerCase();
                        return !t.includes('短剧') && !r.includes('短剧');
                    });
                    for (const item of filtered) {
                        results.push({ ...item, site_key: site.key, site_name: site.name, latency: 0 });
                    }
                } catch {}
            }
            return results;
        });
        const allKwResults = await Promise.all(kwPromises);
        const merged = allKwResults.flat();
        const imageFirst = crossSiteImagePicks(merged);
        
        const buckets = [[], [], [], []];
        const labels = ['电视剧', '电影', '综艺', '动漫'];
        imageFirst.forEach(item => {
            for (let i = 0; i < labels.length; i++) {
                if ((item.type_name || '').includes(labels[i])) { buckets[i].push(item); return; }
            }
            buckets[0].push(item);
        });
        const interleaved = [];
        let idx = 0;
        while (interleaved.length < 24) {
            const bucket = buckets[idx % buckets.length];
            if (bucket.length > 0) interleaved.push(bucket.shift());
            idx++;
            if (buckets.every(b => b.length === 0)) break;
        }
        return interleaved;
    };
    
    // 5 个分类 —— 每个取前 3 个站点（共 15 个请求，速度快且覆盖够）
    const catLabels = ['电视剧', '电影', '综艺', '动漫', '短剧'];
    const catSites = sites.slice(0, 3);
    const fetchCats = catLabels.map(type =>
        (async () => {
            const all = [];
            for (const site of catSites) {
                if (all.length >= 12) break;
                try {
                    const resp = await axios.get(
                        `${site.api}?ac=videolist&wd=${encodeURIComponent(type)}&out=json`,
                        { timeout: 5000 }
                    );
                    const list = (resp.data.list || resp.data.data || []).slice(0, 8);
                    all.push(...list.map(item => ({ ...item, site_key: site.key, site_name: site.name, latency: 0 })));
                } catch {}
            }
            return all.slice(0, 12);
        })()
    );
    
    const [hotResult, ...catResults] = await Promise.all([fetchHot(), ...fetchCats]);
    
    const result = {
        hot: hotResult,
        dianshiju: catResults[0],
        dianying: catResults[1],
        zongyi: catResults[2],
        dongman: catResults[3],
        duanju: catResults[4],
    };
    setCache('home', result, CACHE_TTL.home);
    res.json(result);
});

// ========== 豆瓣内存缓存 ==========
const doubanCache = new Map(); // key → { data, ts }
const DOUBAN_CACHE_TTL = 10 * 60 * 1000; // 10分钟

function getFromCache(key) {
    const item = doubanCache.get(key);
    if (item && Date.now() - item.ts < DOUBAN_CACHE_TTL) return item.data;
    doubanCache.delete(key);
    return null;
}
function setToCache(key, data) {
    doubanCache.set(key, { data, ts: Date.now() });
    if (doubanCache.size > 100) { const first = doubanCache.keys().next().value; doubanCache.delete(first); }
}

// === 豆瓣 API 请求（纯元数据，不做 CMS 匹配） ===
async function fetchDoubanRaw(type, tag, limit = 20) {
    const cacheKey = `${type}|${tag}|${limit}`;
    const cached = getFromCache(cacheKey);
    if (cached) { console.log(`[Douban Cache] 命中: ${cacheKey}`); return cached; }

    const doubanUrl = `https://movie.douban.com/j/search_subjects?type=${type}&tag=${encodeURIComponent(tag)}&sort=recommend&page_limit=${Math.min(Number(limit), 20)}`;
    console.log(`[Douban] 请求: ${doubanUrl}`);
    const resp = await axios.get(doubanUrl, {
        headers: {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
            'Referer': 'https://movie.douban.com/',
        },
        timeout: 8000,
    });

    const subjects = (resp.data.subjects || []).map(s => ({
        id: s.id,
        title: s.title.replace(/\s*\(\d{4}\)\s*$/, '').trim(),
        cover: s.cover,
        rate: s.rate,
        url: s.url,
        year: (s.title.match(/\((\d{4})\)/) || [])[1] || '',
    }));
    setToCache(cacheKey, subjects);
    return subjects;
}

// === 单分类豆瓣推荐（元数据） ===
app.get('/api/douban/recommend', async (req, res) => {
    try {
        const { type = 'movie', tag = '热门', limit = 20 } = req.query;
        const subjects = await fetchDoubanRaw(type, tag, Number(limit));
        console.log(`[Douban] 返回 ${subjects.length} 条元数据`);
        res.json({ subjects });
    } catch (e) {
        console.error('[Douban] 请求失败:', e.message);
        res.json({ subjects: [], error: e.message });
    }
});

// === 豆瓣影片简介获取（原生 j/subject_abstract） ===
async function fetchDoubanSubject(subjectId) {
    const cacheKey = `subject|${subjectId}`;
    const cached = getFromCache(cacheKey);
    if (cached !== null) { console.log(`[Subject Cache] 命中: ${subjectId}`); return cached; }

    console.log(`[Subject] 获取: ${subjectId}`);
    try {
        const resp = await axios.get(`https://movie.douban.com/j/subject_abstract?subject_id=${subjectId}`, {
            headers: {
                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
                'Referer': 'https://movie.douban.com/',
            },
            timeout: 6000,
        });
        const s = (resp.data && resp.data.subject) || {};
        const subject = {
            introduction: s.intro || '',
            rating: (s.rating && s.rating.value) ? String(s.rating.value) : '',
        };
        console.log(`[Subject] 成功: intro=${!!subject.introduction} rating=${subject.rating}`);
        setToCache(cacheKey, subject);
        return subject;
    } catch (e) {
        console.log(`[Subject] 失败: ${e.message}`);
        return null;
    }
}

// === 合并首页豆瓣数据（一次请求返回所有分类） ===
app.get('/api/douban/home', async (req, res) => {
    try {
        const categories = [
            { key: 'hot',       type: 'movie', tag: '热门', limit: 18 },
            { key: 'dianshiju', type: 'tv',    tag: '热门', limit: 12 },
            { key: 'dianying',  type: 'movie', tag: '最新', limit: 12 },
            { key: 'zongyi',    type: 'tv',    tag: '综艺', limit: 12 },
            { key: 'dongman',   type: 'movie', tag: '动画', limit: 12 },
        ];

        const results = {};
        await Promise.all(categories.map(async (c) => {
            results[c.key] = await fetchDoubanRaw(c.type, c.tag, c.limit).catch(() => []);
        }));

        // 为 hot 前5条并行拉简介
        const hotItems = results.hot || [];
        const subjects = await Promise.all(
            hotItems.slice(0, 5).map(s => fetchDoubanSubject(s.id).catch(() => null))
        );
        hotItems.slice(0, 5).forEach((s, i) => {
            const sub = subjects[i];
            if (sub) {
                if (sub.introduction) s.abstract = sub.introduction;
                if (sub.rating) s.rate = sub.rating;
            }
        });
        res.json(results);
    } catch (e) {
        console.error('[Douban Home] 失败:', e.message);
        res.json({ hot: [], dianshiju: [], dianying: [], zongyi: [], dongman: [] });
    }
});

// === 搜索接口（ac=videolist 返回完整字段含 vod_actor/vod_pic，跨站点合并去重） ===
app.get('/api/search', async (req, res) => {
    const wd = normalizeSearchKeyword(req.query.wd);
    console.log(`[Search] ${wd}`);
    if (!wd) return res.json({ list: [] });

    const cacheKey = `search:${wd}`;
    const cached = getCached(cacheKey);
    if (cached) return res.json(cached);

    const sites = getWorkingSites();
    const actorLike = isLikelyActor(wd);
    let allItems = [];

    // 演员搜索优先查本地索引，命中后可以立即减少后续扫描压力。
    if (actorLike) {
        const { all } = searchByActor(wd);
        if (all.length > 0) {
            console.log(`[Search] 索引命中 ${wd}: ${all.length} 条`);
            allItems.push(...all.slice(0, SEARCH_RESULT_LIMIT));
        }
    }

    // CMS 文字搜索
    const seenIds = new Set();
    for (const item of allItems) seenIds.add(itemKey(item));

    const searchKw = actorLike ? [wd, `${wd} 电影`, `${wd} 电视剧`] : [wd];
    const titleSearchItems = [];
    const promises = [];
    for (const site of sites) {
        for (const kw of searchKw) {
            promises.push((async () => {
                try {
                    const resp = await axios.get(
                        `${site.api}?ac=videolist&wd=${encodeURIComponent(kw)}&pg=1&out=json`,
                        { timeout: 8000 }
                    );
                    const list = resp.data.list || resp.data.data;
                    if (list && Array.isArray(list)) {
                        for (const item of list) {
                            const tagged = { ...item, site_key: site.key, site_name: site.name, latency: 0 };
                            titleSearchItems.push(tagged);
                            if (!seenIds.has(itemKey(tagged))) {
                                seenIds.add(itemKey(tagged));
                                allItems.push(tagged);
                            }
                        }
                    }
                } catch {}
            })());
        }
    }

    await Promise.all(promises);

    // 标题搜索返回的 list 往往没有 vod_actor，批量 detail 补全后才能建立演员索引。
    const enriched = await enrichSearchItemsWithDetails(titleSearchItems, sites, wd);
    if (enriched.length > 0) {
        const personMatched = actorLike ? enriched.filter(item => hasPersonMatch(item, wd)) : enriched;
        appendUnique(allItems, seenIds, personMatched);
    }

    if (actorLike) {
        const mergedPreview = crossSiteImagePicks(allItems);
        const actorMatchCount = mergedPreview.filter(item => hasPersonMatch(item, wd)).length;
        if (actorMatchCount < 10) {
            const scanned = await actorFallbackScan(wd, sites, seenIds);
            appendUnique(allItems, seenIds, scanned);
        }
    }

    if (allItems.length > 0) {
        allItems = boostActorMatches(allItems, wd);
        allItems = crossSiteImagePicks(allItems, SEARCH_RESULT_LIMIT);
    }
    const result = { list: allItems };
    setCache(cacheKey, result, CACHE_TTL.search);
    res.json(result);
});

// === 辅助：站点 CMS 搜索（多页） ===
async function searchSitePages(site, kw, maxPages, res) {
    const startTime = Date.now();
    let siteVideos = [];
    const seenIds = new Set();
    try {
        const r1 = await axios.get(`${site.api}?ac=videolist&wd=${encodeURIComponent(kw)}&pg=1&out=json`, { timeout: 6000 });
        const list1 = r1.data.list || r1.data.data || [];
        if (!Array.isArray(list1)) return [];
        const latency = Date.now() - startTime;
        for (const item of list1) {
            if (!seenIds.has(`${item.vod_id}`)) {
                seenIds.add(`${item.vod_id}`);
                siteVideos.push({ ...item, site_key: site.key, site_name: site.name, latency });
            }
        }
        const pagecount = Math.min(Number(r1.data.pagecount) || 1, maxPages);
        for (let pg = 2; pg <= pagecount; pg++) {
            if (res && res.writableEnded) break;
            try {
                const rn = await axios.get(`${site.api}?ac=videolist&wd=${encodeURIComponent(kw)}&pg=${pg}&out=json`, { timeout: 5000 });
                const ln = rn.data.list || rn.data.data || [];
                if (!Array.isArray(ln) || ln.length === 0) break;
                for (const item of ln) {
                    if (!seenIds.has(`${item.vod_id}`)) {
                        seenIds.add(`${item.vod_id}`);
                        siteVideos.push({ ...item, site_key: site.key, site_name: site.name, latency });
                    }
                }
            } catch { break; }
        }
    } catch { return []; }
    return siteVideos;
}

// === SSE 流式搜索（含演员倒排索引 + 轻量兜底扫描） ===
app.post('/api/search-stream', async (req, res) => {
    const { wd } = req.body;
    if (!wd) return res.status(400).json({ error: 'Missing wd' });

    const sites = getWorkingSites();
    const actorLike = isLikelyActor(wd);
    const indexStats = getStats();
    console.log(`[SSE-Search] "${wd}" actorLike=${actorLike} (${sites.length} sources, index=${indexStats.indexedVideoIds}条)`);

    res.writeHead(200, {
        'Content-Type': 'text/event-stream', 'Cache-Control': 'no-cache',
        'Connection': 'keep-alive', 'X-Accel-Buffering': 'no',
    });
    const send = (data) => { if (res.writableEnded) return; res.write(`data: ${JSON.stringify(data)}\n\n`); };
    req.on('close', () => { res.writableEnded = true; });
    send({ type: 'start', totalSources: sites.length, actorLike, indexCoverage: indexStats.indexedVideoIds, uniqueActors: indexStats.uniqueActors });

    // ---- 1. 演员索引查询（毫秒级） ----
    let indexHits = [];
    if (actorLike) {
        const { all } = searchByActor(wd);
        if (all.length > 0) {
            indexHits = all;
            console.log(`[SSE-Search] 索引命中 ${wd}: ${indexHits.length} 条`);
            send({ type: 'videos', videos: all.slice(0, 200), source: 'local', sourceName: `演员索引(${all.length}条)` });
        }
    }

    // ---- 2. CMS 关键字搜索 ----
    // #3 修复：索引命中充足的，只搜单个关键字，避免请求爆炸
    const globalResults = [];
    const searchKeywords = (actorLike && indexHits.length < 20)
        ? [wd, `${wd} 电影`, `${wd} 电视剧`]
        : [wd];

    const searchPromises = [];
    for (const site of sites) {
        for (const kw of searchKeywords) {
            searchPromises.push((async () => {
                if (res.writableEnded) return;
                const siteVideos = await searchSitePages(site, kw, 7, res);
                if (siteVideos.length > 0) {
                    const boosted = boostActorMatches(siteVideos, wd);
                    globalResults.push(...boosted);
                    send({ type: 'videos', videos: boosted.slice(0, 200), source: `${site.key}:${kw}`, sourceName: `${site.name} - ${kw}` });
                }
            })());
        }
    }

    await Promise.all(searchPromises);
    send({ type: 'progress', completedSources: sites.length, totalSources: sites.length, totalVideosFound: indexHits.length + globalResults.length });

    // ---- 3. 兜底扫描 ----
    // #1 修复：用 merged 后的 vod_actor 命中数判断，而非单看 indexHits.length
    // #2 修复：每站只扫 2 个 type_id，每类 3 页
    let needScan = false;
    if (actorLike && res.writableEnded === false) {
        const mergedPreview = crossSiteImagePicks([...indexHits, ...globalResults]);
        const actorMatchCount = mergedPreview.filter(item =>
            (item.vod_actor || '').includes(wd) || (item.vod_director || '').includes(wd)
        ).length;
        needScan = actorMatchCount < 10;
    }

    if (needScan) {
        send({ type: 'progress', completedSources: sites.length, totalSources: sites.length, totalVideosFound: indexHits.length + globalResults.length, phase: 'scanning' });
        console.log(`[SSE-Search] 有效演员结果不足(${indexHits.length + globalResults.length}条)，启动轻量扫描...`);

        const scanCategories = ['movie', 'tv'];
        const SCAN_PAGES = 3;
        const MAX_TYPE_IDS_PER_SITE = 3;

        for (const site of sites) {
            if (res.writableEnded) break;
            for (const cat of scanCategories) {
                if (res.writableEnded) break;
                // 优先使用常见大分类 type_id，避免选到父级 id（1,2）或冷门 id
                const priorityIds = getPriorityScanTypeIds(site.key, cat);
                const selectedTids = priorityIds.length > 0 ? priorityIds.slice(0, MAX_TYPE_IDS_PER_SITE) : [];
                for (const tid of selectedTids) {
                    if (res.writableEnded) break;
                    for (let pg = 1; pg <= SCAN_PAGES; pg++) {
                        if (res.writableEnded) break;
                        try {
                            const r = await axios.get(`${site.api}?ac=videolist&t=${tid}&pg=${pg}&out=json`, { timeout: 5000 });
                            const list = r.data.list || r.data.data || [];
                            if (!Array.isArray(list) || list.length === 0) break;

                            const matched = list.filter(item => {
                                const actor = (item.vod_actor || '').toLowerCase();
                                const director = (item.vod_director || '').toLowerCase();
                                return actor.includes(wd.toLowerCase()) || director.includes(wd.toLowerCase());
                            });

                            if (matched.length > 0) {
                                const tagged = matched.map(item => ({
                                    ...item,
                                    site_key: site.key, site_name: site.name, latency: 0,
                                }));
                                indexItems(tagged);
                                globalResults.push(...tagged);
                                send({ type: 'videos', videos: tagged.slice(0, 200), source: `scan:${site.key}`, sourceName: `扩展扫描 - ${site.name}` });
                                send({ type: 'scan-progress', site: site.key, matched: tagged.length, page: pg });
                            }
                        } catch { break; }
                    }
                }
            }
        }
    }

    // 搜索结果写入索引
    if (actorLike && globalResults.length > 0) {
        const idxCount = indexItems(globalResults);
        if (idxCount > 0) console.log(`[SSE-Search] 新增 ${idxCount} 条索引`);
    }

    // ---- 4. 最终合并 ----
    const allResults = [...indexHits, ...globalResults];

    if (allResults.length > 0 && !res.writableEnded) {
        const merged = crossSiteImagePicks(allResults);
        console.log(`[SSE-Search] Merged: ${allResults.length} -> ${merged.length} (索引${indexHits.length}+搜索${globalResults.length})`);
        send({ type: 'merged', videos: merged, stats: { indexHits: indexHits.length, cmsHits: globalResults.length, merged: merged.length } });
    }

    send({ type: 'complete', totalVideosFound: allResults.length, totalSources: sites.length, indexHits: indexHits.length });
    res.end();
});

// === 跨站点多源详情（输入影片名或直接给 site_key:id 列表，输出所有站点详情+播放源） ===
app.get('/api/multi-detail', async (req, res) => {
    const { wd, keys } = req.query;
    
    const sites = getWorkingSites();
    const results = [];
    
    // 如果提供了 keys（格式: key1:id1,key2:id2），直接按 ID 拉取
    if (keys) {
        const keyPairs = decodeURIComponent(keys).split(',').map(p => {
            const [key, id] = p.split(':');
            return { key, id };
        }).filter(p => p.key && p.id);
        
        await Promise.all(keyPairs.map(async ({ key, id }) => {
            const site = sites.find(s => s.key === key);
            if (!site) return;
            try {
                const detailResp = await axios.get(
                    `${site.api}?ac=detail&ids=${id}&out=json`,
                    { timeout: 6000 }
                );
                const detail = (detailResp.data.list && detailResp.data.list[0]) 
                    ? detailResp.data.list[0] 
                    : detailResp.data;
                if (detail && detail.vod_id) {
                    results.push({ ...detail, site_key: site.key, site_name: site.name });
                }
            } catch (e) {}
        }));
        
        results.sort((a, b) => {
            const aHas = a.vod_pic && a.vod_pic.startsWith('http') ? 1 : 0;
            const bHas = b.vod_pic && b.vod_pic.startsWith('http') ? 1 : 0;
            return bHas - aHas;
        });
        return res.json({ list: results });
    }
    
    // 回退：按名称搜索
    if (!wd) return res.status(400).json({ error: 'Missing wd or keys' });
    
    await Promise.all(sites.map(async (site) => {
        try {
            const searchResp = await axios.get(
                `${site.api}?ac=videolist&wd=${encodeURIComponent(wd)}&pg=1&out=json`,
                { timeout: 8000 }
            );
            const list = searchResp.data.list || searchResp.data.data || [];
            const match = list.find(item => {
                const name = (item.vod_name || '').replace(/[·・\s\-\[\]【】]/g, '').toLowerCase();
                const query = wd.replace(/[·・\s\-\[\]【】]/g, '').toLowerCase();
                return name === query || name.includes(query) || query.includes(name);
            });
            if (match) {
                const detailResp = await axios.get(
                    `${site.api}?ac=detail&ids=${match.vod_id}&out=json`,
                    { timeout: 6000 }
                );
                const detail = (detailResp.data.list && detailResp.data.list[0]) 
                    ? detailResp.data.list[0] 
                    : detailResp.data;
                if (detail && detail.vod_id) {
                    results.push({ ...detail, site_key: site.key, site_name: site.name });
                }
            }
        } catch (e) {}
    }));
    
    // 按站点图片质量排序
    results.sort((a, b) => {
        const aHas = a.vod_pic && a.vod_pic.startsWith('http') ? 1 : 0;
        const bHas = b.vod_pic && b.vod_pic.startsWith('http') ? 1 : 0;
        return bHas - aHas;
    });
    
    res.json({ list: results });
});

// === 分类浏览分页接口（按需分页：用户翻到第N页才拉第N页） ===
// 分类元数据缓存：第1页扫描后记录 maxPage / total，供后续翻页复用
const categoryMetaCache = new Map();

function saveCategoryCacheToDisk() {
    try {
        const pages = {};
        for (const [key, entry] of serverCache.entries()) {
            if (!isCategoryPageCacheKey(key)) continue;
            if (!entry.complete || !entry.data?.list?.length) continue;
            if (Date.now() - entry.time > CATEGORY_DISK_CACHE_TTL) continue;
            pages[key] = {
                data: entry.data,
                time: entry.time,
                ttl: CATEGORY_DISK_CACHE_TTL,
                complete: true,
            };
        }
        const meta = Object.fromEntries(categoryMetaCache.entries());
        fs.writeFileSync(
            CATEGORY_CACHE_FILE,
            JSON.stringify({ version: CATEGORY_CACHE_VERSION, savedAt: Date.now(), pages, meta }, null, 2)
        );
    } catch (e) {
        console.log('[CategoryCache] 保存失败:', e.message);
    }
}

function loadCategoryCacheFromDisk() {
    try {
        if (!fs.existsSync(CATEGORY_CACHE_FILE)) return;
        const raw = JSON.parse(fs.readFileSync(CATEGORY_CACHE_FILE, 'utf8'));
        if (raw.version !== CATEGORY_CACHE_VERSION) return;
        let restoredPages = 0;
        const now = Date.now();
        for (const [key, entry] of Object.entries(raw.pages || {})) {
            if (!isCategoryPageCacheKey(key)) continue;
            if (!entry?.data?.list?.length) continue;
            if (now - (entry.time || raw.savedAt || 0) > CATEGORY_DISK_CACHE_TTL) continue;
            serverCache.set(key, {
                data: entry.data,
                time: entry.time || raw.savedAt || now,
                ttl: CATEGORY_DISK_CACHE_TTL,
                complete: true,
            });
            restoredPages++;
        }
        for (const [key, value] of Object.entries(raw.meta || {})) {
            if (key.startsWith(`${CATEGORY_CACHE_VERSION}:category-meta:`)) categoryMetaCache.set(key, value);
        }
        if (restoredPages > 0) console.log(`[CategoryCache] 从磁盘恢复 ${restoredPages} 个分类页缓存`);
    } catch (e) {
        console.log('[CategoryCache] 恢复失败:', e.message);
    }
}

function startCategoryPrewarm() {
    const tasks = [];
    for (const category of CATEGORY_PREWARM_CATEGORIES) {
        for (const pageSize of CATEGORY_PREWARM_PAGE_SIZES) {
            tasks.push({ category, pageSize });
        }
    }

    (async () => {
        await new Promise(resolve => setTimeout(resolve, 2500));
        console.log(`[CategoryWarmup] 开始后台预热 ${tasks.length} 个分类首屏`);
        for (const task of tasks) {
            const key = buildCategoryPageCacheKey(task.category, null, 1, task.pageSize);
            const cached = getCachedEntry(key);
            const freshEnough = cached && cached.complete && Date.now() - cached.time < CACHE_TTL.category;
            if (freshEnough) continue;
            try {
                await startCategoryFullRefresh({
                    category: task.category,
                    page: 1,
                    pageSize: task.pageSize,
                }, key);
                console.log(`[CategoryWarmup] ${task.category} pageSize=${task.pageSize} 完成`);
            } catch (e) {
                console.log(`[CategoryWarmup] ${task.category} pageSize=${task.pageSize} 跳过: ${e.message}`);
            }
            await new Promise(resolve => setTimeout(resolve, 600));
        }
        saveCategoryCacheToDisk();
        console.log('[CategoryWarmup] 后台预热结束');
    })();
}

async function fetchFastCategoryFirstPage({ category, fallbackWd, pageSize, metaKey }) {
    const sites = getWorkingSites();
    const allResults = [];
    let maxPage = 1;
    let servedRequest = false;

    await Promise.all(sites.map(async (site) => {
        const typeIds = getFastCategoryTypeIds(site.key, category);
        const siteItems = [];

        const tasks = typeIds.length > 0
            ? typeIds.map(tid => async () => {
                try {
                    const url = `${site.api}?ac=videolist&t=${tid}&pg=1&out=json`;
                    const r = await axios.get(url, { timeout: 2600 });
                    const list = r.data.list || r.data.data || [];
                    if (!Array.isArray(list) || list.length === 0) return [];
                    servedRequest = true;
                    const pc = Number(r.data.pagecount) || 1;
                    maxPage = Math.max(maxPage, pc);
                    return list.map(item => ({ ...item, site_key: site.key, site_name: site.name, latency: 0 }));
                } catch { return []; }
            })
            : [async () => {
                try {
                    const url = `${site.api}?ac=videolist&wd=${encodeURIComponent(fallbackWd)}&pg=1&out=json`;
                    const r = await axios.get(url, { timeout: 2600 });
                    const list = r.data.list || r.data.data || [];
                    if (!Array.isArray(list) || list.length === 0) return [];
                    servedRequest = true;
                    const pc = Number(r.data.pagecount) || 1;
                    maxPage = Math.max(maxPage, pc);
                    return list.map(item => ({ ...item, site_key: site.key, site_name: site.name, latency: 0 }));
                } catch { return []; }
            }];

        const results = await batchLimit(tasks, 2);
        for (const items of results) {
            if (items && items.length > 0) siteItems.push(...items);
        }
        crossSiteImagePicks(siteItems).forEach(item => allResults.push(item));
    }));

    const merged = crossSiteImagePicks(allResults);
    if (!servedRequest || merged.length === 0) return null;

    const totalEstimate = maxPage * pageSize;
    const existingMeta = categoryMetaCache.get(metaKey) || {};
    categoryMetaCache.set(metaKey, { ...existingMeta, maxPage, total: totalEstimate });

    return {
        total: totalEstimate || merged.length,
        page: 1,
        pageSize,
        totalPages: Math.max(1, maxPage),
        list: merged.slice(0, pageSize),
        complete: false,
        warming: true,
        serverFiltered: false,
        subCounts: existingMeta.subCounts || undefined,
    };
}

function startCategoryFullRefresh(payload, cacheKey) {
    if (categoryFullRefreshInFlight.has(cacheKey)) return categoryFullRefreshInFlight.get(cacheKey);
    const promise = axios.post(`http://127.0.0.1:${PORT}/api/category`, {
        ...payload,
        refresh: true,
    }, { timeout: 45000 })
        .catch(e => console.log(`[Category] 后台完整缓存失败 ${cacheKey}: ${e.message}`))
        .finally(() => categoryFullRefreshInFlight.delete(cacheKey));
    categoryFullRefreshInFlight.set(cacheKey, promise);
    return promise;
}

app.post('/api/category', async (req, res) => {
    const { category, wd, subType, page = 1, pageSize = 30, refresh = false } = req.body;
    const fallbackWd = wd || (category ? CATEGORY_TYPE_NAMES[category]?.[0] : null);
    if (!fallbackWd && !category) return res.status(400).json({ error: 'Missing category or wd' });

    const p = Math.max(1, Number(page));
    const ps = Number(pageSize);
    const sub = subType || null;
    const subLabel = sub ? `:${sub}` : '';
    const categoryOrWd = category || wd;
    const metaKey = buildCategoryMetaKey(categoryOrWd, sub);
    const cacheKey = buildCategoryPageCacheKey(categoryOrWd, sub, p, ps);

    const cached = getCachedEntry(cacheKey);
    if (cached && cached.complete && !refresh) {
        // 缓存命中时，从 metaCache 补上 subCounts（异步写入的，不在 page 缓存里）
        const meta = categoryMetaCache.get(metaKey);
        const data = meta?.subCounts ? { ...cached.data, subCounts: meta.subCounts } : cached.data;
        return res.json(data);
    }

    const meta = categoryMetaCache.get(metaKey);
    if (meta && p > meta.maxPage) {
        return res.json({ total: meta.total, page: p, pageSize: ps, totalPages: meta.maxPage, list: [], complete: true, outOfRange: true, subCounts: meta.subCounts });
    }

    const fullRefresh = categoryFullRefreshInFlight.get(cacheKey);
    if (!refresh && fullRefresh) {
        await withTimeout(fullRefresh, 2500, null);
        const refreshed = getCachedEntry(cacheKey);
        if (refreshed && refreshed.complete) {
            const refreshedMeta = categoryMetaCache.get(metaKey);
            const data = refreshedMeta?.subCounts ? { ...refreshed.data, subCounts: refreshedMeta.subCounts } : refreshed.data;
            return res.json(data);
        }
        if (refreshed?.data?.list?.length) return res.json(refreshed.data);
    }

    if (!refresh && category && !sub && p === 1) {
        const fastResult = await withTimeout(
            fetchFastCategoryFirstPage({ category, fallbackWd, pageSize: ps, metaKey }),
            CATEGORY_FAST_FIRST_PAGE_TIMEOUT,
            null
        );
        if (fastResult?.list?.length) {
            setCachePartial(cacheKey, fastResult, Math.min(CACHE_TTL.category, 90 * 1000));
            startCategoryFullRefresh({ category, wd: fallbackWd, page: p, pageSize: ps, subType: undefined }, cacheKey);
            console.log(`[Category] "${category}" 首屏快速返回 ${fastResult.list.length} 条，完整缓存后台刷新中`);
            return res.json(fastResult);
        }
    }

    // === 子分类 type_id 解析 ===
    const sites = getWorkingSites();
    let siteTypeMap = null; // Map<siteKey, [typeId]> — 子分类模式下的精简 type_id 列表
    if (sub && category) {
        const subMap = getSubTypeIds(category, sub);
        if (subMap) {
            siteTypeMap = new Map();
            for (const site of sites) {
                const tid = subMap[site.key];
                if (tid) siteTypeMap.set(site.key, [tid]);
            }
        }
    }
    const serverSideFiltered = !!siteTypeMap;

    console.log(`[Category] 加载 "${category || wd}"${subLabel} 第${p}页 (${sites.length}站, serverFilter=${serverSideFiltered})`);

    // 构建站点查询列表
    const siteQueries = await Promise.all(sites.map(async (site) => {
        // 服务端子分类过滤：直接用映射的 type_id
        if (serverSideFiltered) {
            const tids = siteTypeMap.get(site.key);
            if (tids && tids.length > 0) return { site, typeIds: tids, useWd: false };
            return { site, typeIds: [], useWd: false }; // 该站点不支持此子分类
        }
        // 全部模式：使用父+子 type_id 聚合，保证各资源站翻页稳定。
        if (category) {
            const typeIds = await getSiteCategoryTypeIds(site, category);
            if (typeIds && typeIds.length > 0) return { site, typeIds, useWd: false };
            return { site, typeIds: [], useWd: true };
        }
        return { site, typeIds: [], useWd: true };
    }));

    // 拉取数据
    const allResults = [];
    let maxPage = 1;
    let servedRequest = false;

    // 第 1 页并发限制：最多 3 个 type_id 同时请求，避免 60 次同时打垮服务
    const REQUEST_CONCURRENCY = p === 1 ? 3 : 8;

    await Promise.all(siteQueries.map(async ({ site, typeIds, useWd }) => {
        if (useWd) {
            try {
                const url = `${site.api}?ac=videolist&wd=${encodeURIComponent(fallbackWd)}&pg=${p}&out=json`;
                const r = await axios.get(url, { timeout: 6000 });
                const list = r.data.list || r.data.data || [];
                if (Array.isArray(list) && list.length > 0) {
                    servedRequest = true;
                    const pc = Number(r.data.pagecount) || 1;
                    maxPage = Math.max(maxPage, pc);
                    list.forEach(item => allResults.push({ ...item, site_key: site.key, site_name: site.name, latency: 0 }));
                }
            } catch {}
            return;
        }
        if (typeIds.length === 0) return;
        const siteItems = [];
        // 并发受控的 type_id 请求
        const tasks = typeIds.map(tid => async () => {
            try {
                const url = `${site.api}?ac=videolist&t=${tid}&pg=${p}&out=json`;
                const r = await axios.get(url, { timeout: 6000 });
                const list = r.data.list || r.data.data || [];
                if (p === 1 && serverSideFiltered) {
                    const firstTn = list[0]?.type_name || '(无)';
                    console.log(`  [Diag] ${site.key} t=${tid} → type_name="${firstTn}" (${list.length}条)`);
                }
                if (!Array.isArray(list) || list.length === 0) return [];
                servedRequest = true;
                const pc = Number(r.data.pagecount) || 1;
                maxPage = Math.max(maxPage, pc);
                return list.map(item => ({ ...item, site_key: site.key, site_name: site.name, latency: 0 }));
            } catch { return []; }
        });
        const results = await batchLimit(tasks, REQUEST_CONCURRENCY);
        for (const items of results) {
            if (items && items.length > 0) siteItems.push(...items);
        }
        const deduped = crossSiteImagePicks(siteItems);
        deduped.forEach(item => allResults.push(item));
    }));

    const merged = crossSiteImagePicks(allResults);

    // total 估算（子分类模式下直接用 pagecount × pageSize，更准确）
    let totalEstimate = meta?.total || 0;
    let effectiveMaxPage = meta?.maxPage || maxPage;
    if (p === 1 && servedRequest && maxPage > 0) {
        effectiveMaxPage = maxPage;
        totalEstimate = effectiveMaxPage * ps;
        categoryMetaCache.set(metaKey, { maxPage: effectiveMaxPage, total: totalEstimate });
    } else if (meta) {
        effectiveMaxPage = meta.maxPage;
        totalEstimate = meta.total;
    } else if (merged.length > 0) {
        effectiveMaxPage = maxPage;
        totalEstimate = effectiveMaxPage * ps;
    }

    const outOfRange = !servedRequest && p > 1 && totalEstimate > 0;

    // === 子分类计数（全部模式第 1 页时，异步轻量查询真实 total，不阻塞响应） ===
    let subCounts = meta?.subCounts || null;
    if (!sub && !subType && p === 1 && category && SUB_TYPE_MAP[category] && !meta?.subCounts) {
        // 异步查询各子分类的 pagecount（pg=1 极轻），写入 metaCache 供后续复用
        (async () => {
            const counts = {};
            const map = SUB_TYPE_MAP[category];
            await Promise.all(Object.keys(map).map(async (label) => {
                let total = 0;
                for (const site of sites) {
                    const tid = map[label]?.[site.key];
                    if (!tid) continue;
                    try {
                        const r = await axios.get(`${site.api}?ac=videolist&t=${tid}&pg=1&out=json`, { timeout: 5000 });
                        const pc = Number(r.data.pagecount) || 0;
                        total += pc * 30; // pagecount × pageSize 估算该站该子类总量
                    } catch {}
                }
                counts[label] = total;
            }));
            const existing = categoryMetaCache.get(metaKey) || {};
            categoryMetaCache.set(metaKey, { ...existing, subCounts: counts });
        })();
    }

    const result = {
        total: totalEstimate || merged.length,
        page: p,
        pageSize: ps,
        totalPages: Math.max(1, effectiveMaxPage),
        list: merged.slice(0, ps),
        complete: true,
        outOfRange,
        serverFiltered: serverSideFiltered,
        subCounts: subCounts || undefined,
    };

    // 演员倒排索引
    if (allResults.length > 0) {
        const indexed = indexItems(allResults);
        if (indexed > 0) console.log(`[ActorIndex] 从分类"${category}"第${p}页新增 ${indexed} 条索引`);
    }

    if (merged.length > 0) setCache(cacheKey, result, CACHE_TTL.category);
    console.log(`[Category] "${category || wd}"${subLabel} 第${p}页: ${result.list.length}条 (maxPage=${effectiveMaxPage}, 约${result.total}条)`);
    res.json(result);
});

// === 分享页视频地址解析 ===
function extractVideoUrlFromSharePage(html, baseUrl) {
    // 通用视频 URL 模式（同时匹配 m3u8 和 mp4/webm/ts/flv/mkv 等直连格式）
    const videoExt = '(?:m3u8|mp4|webm|ts|flv|mkv|avi|mov|wmv|m4v|ogg)';
    
    // 模式 1: var main = "/path/to/video"（量子资源等）
    const mainMatch = html.match(new RegExp(`var\\s+main\\s*=\\s*["']([^"']+\\.${videoExt}[^"']*)["']`, 'i'));
    if (mainMatch) {
        try { return new URL(mainMatch[1], baseUrl).toString(); }
        catch { return mainMatch[1]; }
    }
    // 模式 2: var url / video / src / playurl / vUrl = "..."
    const varMatch = html.match(new RegExp(`var\\s+(?:url|video|src|playurl|vUrl)\\s*=\\s*["']([^"']+\\.${videoExt}[^"']*)["']`, 'i'));
    if (varMatch) {
        try { return new URL(varMatch[1], baseUrl).toString(); }
        catch { return varMatch[1]; }
    }
    // 模式 3: HTML 中任何绝对视频 URL
    const absMatch = html.match(new RegExp(`(?:https?:\\/\\/[^\\s"'<>]+\\.${videoExt}[^\\s"'<>]*)`, 'i'));
    if (absMatch) return absMatch[0];
    // 模式 4: 相对路径视频 URL
    const relMatch = html.match(new RegExp(`["']([^"']+\\.${videoExt}[^"']*)["']`, 'i'));
    if (relMatch) {
        try { return new URL(relMatch[1], baseUrl).toString(); }
        catch {}
    }
    // 模式 5: JSON 内嵌 video url（常见于 jsonPlayer / player_data）
    const jsonMatch = html.match(/["'](?:video|url|src|file)["']\s*[:=]\s*["']([^"']+)["']/i);
    if (jsonMatch && /\.[a-z0-9]+(?:\?|$)/i.test(jsonMatch[1])) {
        try { return new URL(jsonMatch[1], baseUrl).toString(); }
        catch { return jsonMatch[1]; }
    }
    return null;
}

// === 视频代理（参考 KVideo，重写 M3U8 + CORS + 流代理 + 分享页解析） ===
app.get('/api/proxy', async (req, res) => {
    const { url } = req.query;
    if (!url) return res.status(400).send('Missing url');
    
    try {
        let targetUrl = decodeURIComponent(url);
        let isM3u8 = targetUrl.endsWith('.m3u8') || targetUrl.includes('.m3u8');
        
        // 非 m3u8 URL：先尝试获取，检测是否为分享页
        if (!isM3u8) {
            try {
                const headResp = await axios.get(targetUrl, {
                    timeout: 15000,
                    headers: {
                        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)',
                        'Referer': new URL(targetUrl).origin,
                    },
                    validateStatus: s => s < 400,
                    responseType: 'text',
                });
                
                const contentType = headResp.headers['content-type'] || '';
                const content = headResp.data || '';
                
                // 如果是 HLS 内容（即使 URL 不以 .m3u8 结尾）
                if (typeof content === 'string' && content.trim().startsWith('#EXTM3U')) {
                    isM3u8 = true;
                }
                // 如果是分享页 HTML，提取真实视频地址
                else if (contentType.includes('text/html') && typeof content === 'string') {
                    const resolved = extractVideoUrlFromSharePage(content, targetUrl);
                    if (resolved) {
                        console.log('[Proxy] Share page resolved:', targetUrl.substring(0, 60) + '...', '->', resolved.substring(0, 80) + '...');
                        targetUrl = resolved;
                        isM3u8 = targetUrl.endsWith('.m3u8') || targetUrl.includes('.m3u8');
                    } else {
                        // 无法解析，返回错误
                        return res.status(502).json({ error: '无法解析分享页视频地址', message: '该链接是分享页而非直接视频地址' });
                    }
                }
            } catch (checkErr) {
                // 检查失败回退，直接按二进制流处理
                console.log('[Proxy] Pre-check failed:', checkErr.message.substring(0, 100));
            }
        }

        if (isM3u8) {
            // M3U8 文本：获取并重写内部 URL
            const resp = await axios.get(targetUrl, {
                timeout: 15000,
                headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)' },
                responseType: 'text',
            });
            const content = typeof resp.data === 'string' ? resp.data : '';
            
            // 如果返回的不是合法 m3u8（可能是重定向到 HTML）
            if (!content.trim().startsWith('#EXTM3U')) {
                const resolved = extractVideoUrlFromSharePage(content, targetUrl);
                if (resolved) {
                    targetUrl = resolved;
                    const retryResp = await axios.get(targetUrl, {
                        timeout: 15000,
                        headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)' },
                        responseType: 'text',
                    });
                    return res.send(retryResp.data);
                }
                return res.status(502).json({ error: 'Invalid M3U8 response', message: '返回内容不是合法的 M3U8' });
            }
            
            const lines = content.split('\n');
            const base = new URL(targetUrl);
            const origin = `${req.protocol}://${req.get('host')}`;
            
            const processed = lines.map(line => {
                const trimmed = line.trim();
                if (trimmed.match(/^#EXT-X-(KEY|MAP|MEDIA):/) && trimmed.includes('URI="')) {
                    return trimmed.replace(/URI="([^"]+)"/, (_, uri) => {
                        try { return `URI="${origin}/api/proxy?url=${encodeURIComponent(new URL(uri, base).toString())}"`; }
                        catch { return line; }
                    });
                }
                if (trimmed.startsWith('#') || !trimmed) return line;
                if (trimmed.includes('/api/proxy')) return line;
                try { return `${origin}/api/proxy?url=${encodeURIComponent(new URL(trimmed, base).toString())}`; }
                catch { return line; }
            });
            
            res.set('Content-Type', 'application/vnd.apple.mpegurl');
            res.set('Access-Control-Allow-Origin', '*');
            res.set('Cache-Control', 'no-cache');
            res.send(processed.join('\n'));
        } else {
            // TS/MP4 二进制：流式代理
            const resp = await axios.get(targetUrl, {
                responseType: 'stream',
                timeout: 30000,
                headers: {
                    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)',
                    'Range': req.headers.range || '',
                },
            });
            // 再次检查 Content-Type，以防返回了 HTML 错误页
            const ct = resp.headers['content-type'] || '';
            if (ct.includes('text/html')) {
                return res.status(502).json({ error: '视频源返回了 HTML 页面', message: '可能已过期或被拦截' });
            }
            res.set('Content-Type', ct || 'video/mp2t');
            res.set('Access-Control-Allow-Origin', '*');
            res.set('Cache-Control', 'no-cache');
            if (resp.headers['content-length']) {
                res.set('Content-Length', resp.headers['content-length']);
            }
            resp.data.pipe(res);
        }
    } catch (e) {
        console.error('[Proxy] Error:', e.message);
        res.status(502).json({ error: 'Proxy failed', message: e.message });
    }
});

// === 缩略图接口（API 优先 → 跨站匹配 → 爬站回退） ===
app.get('/api/thumbnail', async (req, res) => {
    const { site_key, id } = req.query;
    if (!site_key || !id) return res.status(400).json({ vod_pic: '' });
    const sites = getDB().sites.filter(s => s.active);
    const targetSite = sites.find(s => s.key === site_key);
    if (!targetSite) return res.status(404).json({ vod_pic: '' });
    
    let vodName = ''; // 获取剧名以供跨站搜索

    // 方案1：调 API 获取（最快），但排除被墙 CDN 的图片
    try {
        const dr = await axios.get(`${targetSite.api}?ac=detail&ids=${id}&out=json`, { timeout: 5000 });
        const dlist = dr.data.list || dr.data.data;
        const detail = (dlist && dlist[0]) ? dlist[0] : dr.data;
        vodName = detail?.vod_name || '';
        if (detail?.vod_pic && detail.vod_pic.startsWith('http') && !isBlockedPic(detail.vod_pic)) {
            return res.json({ vod_pic: detail.vod_pic });
        }
    } catch {}

    // 方案2：跨站匹配 —— 用剧名搜索 bfzy（唯一有可访问 CDN 的站点），找同名视频的封面
    if (vodName) {
        const bfzy = sites.find(s => s.key === 'bfzy');
        if (bfzy && bfzy.key !== targetSite.key) {
            try {
                const sr = await axios.get(
                    `${bfzy.api}?ac=videolist&wd=${encodeURIComponent(vodName)}&out=json`,
                    { timeout: 5000 }
                );
                const list = (sr.data.list || sr.data.data || []);
                const normName = vodName.replace(/[·・\s\-\[\]【】]/g, '').toLowerCase().substring(0, 15);
                const match = list.find(item => {
                    const itemName = (item.vod_name || '').replace(/[·・\s\-\[\]【】]/g, '').toLowerCase().substring(0, 15);
                    return itemName === normName && item.vod_pic && item.vod_pic.startsWith('http') && !isBlockedPic(item.vod_pic);
                });
                if (match) {
                    console.log(`[Thumbnail] Cross-site match for "${vodName}": ${bfzy.name}`);
                    return res.json({ vod_pic: match.vod_pic });
                }
            } catch {}
        }
    }

    // 方案3：爬网站 HTML 提取（回退方案，参考 MoonTVPlus）
    if (targetSite.detail) {
        try {
            const htmlUrl = `${targetSite.detail}/index.php/vod/detail/id/${id}.html`;
            const htmlResp = await axios.get(htmlUrl, {
                timeout: 8000,
                headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36' }
            });
            const html = htmlResp.data;

            const patterns = [
                /<img[^>]+src=["']([^"']*(?:vod_pic|pic|poster|cover|thumb)[^"']*)["']/i,
                /<meta\s+property=["']og:image["']\s+content=["']([^"']+)["']/i,
                /<img[^>]+alt=["'][^"']*(?:封面|海报|剧照)[^"']*["'][^>]+src=["']([^"']+)["']/i,
            ];
            for (const pattern of patterns) {
                const m = html.match(pattern);
                if (m && m[1]) {
                    const pic = m[1].startsWith('http') ? m[1] : new URL(m[1], targetSite.detail).toString();
                    return res.json({ vod_pic: pic });
                }
            }
        } catch {}
    }

    // 都没拿到
    res.json({ vod_pic: '' });
});

// === 详情接口 ===
app.get('/api/detail', async (req, res) => {
    const { site_key, id } = req.query;
    const targetSite = getDB().sites.find(s => s.key === site_key);
    if (!targetSite) return res.status(404).json({ error: "Site not found" });
    try {
        const response = await axios.get(`${targetSite.api}?ac=detail&ids=${id}&out=json`, { timeout: 6000 });
        // 外部 API 返回 { code, msg, list: [...] }，取 list[0]
        const data = response.data;
        const item = (data.list && data.list[0]) ? data.list[0] : data;
        res.json(item);
    } catch (e) { res.status(500).json({ error: "Source Error" }); }
});

// === 图片代理（服务端代理，加 Referer 防盗链头，失败返回 404 触发前端 onError fallback） ===
app.get('/api/img', async (req, res) => {
    const { url } = req.query;
    if (!url) return res.status(400).json({ error: 'Missing url' });
    
    const targetUrl = decodeURIComponent(url);
    let host = '';
    try { host = new URL(targetUrl).host; } catch {}
    
    // 暴风资源的所有 CDN 子域名都需要 Referer 防盗链
    let referer = '';
    const sites = getDB().sites;
    const bf = sites.find(s => s.key === 'bfzy');
    if (bf && (host.includes('picbf') || host.includes('bfzy'))) {
        referer = bf.detail || 'https://bfzyapi.com';
    }
    // 豆瓣图片防盗链
    if (host.includes('doubanio.com') || host.includes('douban.com')) {
        referer = 'https://movie.douban.com/';
    }
    
    try {
        const response = await axios.get(targetUrl, {
            responseType: 'stream',
            timeout: 8000,
            headers: {
                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
                ...(referer ? { 'Referer': referer } : {}),
            }
        });
        const contentType = response.headers['content-type'] || 'image/jpeg';
        res.set('Content-Type', contentType);
        res.set('Cache-Control', 'public, max-age=86400');
        res.set('Access-Control-Allow-Origin', '*');
        response.data.pipe(res);
    } catch (e) {
        // CDN 不可达 → 返回 404，触发前端 <img onError> → 渐变色 fallback
        res.status(404).end();
    }
});

app.post('/api/admin/login', (req, res) => req.body.password === ADMIN_PASSWORD ? res.json({ success: true }) : res.status(403).json({ success: false }));
app.get('/api/admin/sites', (req, res) => res.json(getDB().sites));
app.post('/api/admin/sites', (req, res) => { saveDB({sites: req.body.sites}); res.json({ success: true }); });

// 清除服务端缓存（debug 用）
app.post('/api/cache/clear', (req, res) => {
    serverCache.clear();
    res.json({ ok: true, message: 'Cache cleared' });
});

// === 演员索引统计 ===
app.get('/api/actor-index/stats', (req, res) => {
    res.json(getStats());
});

// === 演员索引手动保存 ===
app.post('/api/actor-index/save', (req, res) => {
    const ok = saveToDisk(ACTOR_INDEX_FILE);
    res.json({ ok, message: ok ? '已保存' : '保存失败' });
});

// ==================== 启动初始化 ====================
// 1. 从磁盘恢复演员索引
loadFromDisk(ACTOR_INDEX_FILE);
loadCategoryCacheFromDisk();

// 2. 定期自动保存索引（每 10 分钟）
const autoSaveInterval = setInterval(() => {
    saveToDisk(ACTOR_INDEX_FILE);
    saveCategoryCacheToDisk();
}, 10 * 60 * 1000);

// 3. 退出时保存索引
process.on('SIGINT', () => {
    console.log('\n[ActorIndex] 正在保存索引...');
    saveToDisk(ACTOR_INDEX_FILE);
    saveCategoryCacheToDisk();
    clearInterval(autoSaveInterval);
    process.exit(0);
});
process.on('SIGTERM', () => {
    saveToDisk(ACTOR_INDEX_FILE);
    saveCategoryCacheToDisk();
    clearInterval(autoSaveInterval);
    process.exit(0);
});

// ==================== 静态文件服务（前端构建产物，覆盖桌面/移动/TV 三端） ====================
const distPath = path.join(__dirname, '..', 'apps', 'desktop', 'dist');
if (fs.existsSync(distPath)) {
  console.log(`[Static] 静态文件目录: ${distPath}`);
  app.use(express.static(distPath, { maxAge: '1h' }));
  // SPA fallback：所有非 API 请求返回 index.html
  app.get('*', (req, res, next) => {
    if (req.path.startsWith('/api')) return next();
    const indexPath = path.join(distPath, 'index.html');
    if (fs.existsSync(indexPath)) res.sendFile(indexPath);
    else next();
  });
} else {
  console.log('[Static] dist/ 目录不存在，跳过静态文件服务（开发时用 vite dev server）');
}

// 显式绑定 0.0.0.0：确保 Android 模拟器(10.0.2.2 → 127.0.0.1)能连上
// （仅 app.listen(port) 时 Node 默认绑 IPv6 ::，部分 Windows 上模拟器 IPv4 连不上）
app.listen(PORT, '0.0.0.0', () => {
  console.log(`服务已启动: http://localhost:${PORT}`);
  console.log(`  桌面/浏览器访问: http://localhost:${PORT}`);
  console.log(`  Android 模拟器访问: http://10.0.2.2:${PORT}/mobile`);
  startCategoryPrewarm();
});
