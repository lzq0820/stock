// src/utils/api.js
const API_BASE = '/baseInfo/baseInfo/stockPool/query';

// 节假日数据缓存对象
const holidaysCache = {};

// 添加showSt参数
export async function fetchStockPool(tradeDate, poolType, notShowSt = 0) {
    const response = await fetch(`${API_BASE}?tradeDate=${tradeDate}&poolType=${poolType}&notShowSt=${notShowSt}`);
    if (!response.ok) {
        throw new Error('Failed to fetch stock pool data');
    }
    const data = await response.json();
    return data.data || [];
}

/**
 * 获取指定年份和月份的节假日数据
 * @param {number} year - 年份，例如：2026
 * @param {number} month - 月份，范围：1-12
 * @returns {Promise<Array>} 节假日数据数组，包含日期、是否为节假日等信息
 * @throws {Error} 如果请求失败或响应状态码不为200
 */
export async function fetchHolidays(year) {
    // 创建缓存键（使用year-month格式）
    const cacheKey = `holiday`;

    // 检查缓存中是否已有数据
    if (holidaysCache[cacheKey]) {
        return holidaysCache[cacheKey];
    }

    // 如果缓存中没有，则调用API获取数据
    const response = await fetch(`/baseInfo/api/holiday/sync/query?year=${year}`);
    if (!response.ok) {
        throw new Error('Failed to fetch holidays');
    }
    const data = await response.json();
    const holidaysData = data.data || [];

    // 将数据存储到缓存中
    holidaysCache[cacheKey] = holidaysData;

    return holidaysData;
}
