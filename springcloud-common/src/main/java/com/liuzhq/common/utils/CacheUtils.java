package com.liuzhq.common.utils;

import com.liuzhq.common.annotation.Description;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 通用缓存工具类（基于JDK ConcurrentHashMap实现）
 * 特性：
 * 1. 支持键值对的增删改查
 * 2. 支持过期时间设置
 * 3. 支持缓存大小限制
 * 4. 线程安全，基于ConcurrentHashMap实现
 * 5. 自动清理过期缓存（懒加载+定时清理可选）
 */
@Description("缓存工具类，基于JDK ConcurrentHashMap实现，支持过期时间、缓存清理")
@Slf4j
public class CacheUtils {

    // 原始的字符串缓存Map（保持向下兼容）
    public static final Map<String, String> cacheMap = new ConcurrentHashMap<>();

    // 增强版缓存容器：存储带过期时间的缓存项
    private static final Map<String, CacheEntry> ENHANCED_CACHE = new ConcurrentHashMap<>();

    // 缓存最大容量（默认无限制，0表示不限制）
    private static volatile long MAX_CAPACITY = 0;

    // 缓存命中数统计
    private static final AtomicLong HIT_COUNT = new AtomicLong(0);

    // 缓存未命中数统计
    private static final AtomicLong MISS_COUNT = new AtomicLong(0);

    // 缓存项实体类
    private static class CacheEntry {
        private Object value;          // 缓存值（支持任意类型）
        private long expireTimeMillis; // 过期时间戳（毫秒），0表示永不过期

        public CacheEntry(Object value, long expireTimeMillis) {
            this.value = value;
            this.expireTimeMillis = expireTimeMillis;
        }

        public boolean isExpired() {
            // 0表示永不过期
            if (expireTimeMillis == 0) {
                return false;
            }
            return System.currentTimeMillis() > expireTimeMillis;
        }
    }

    // ==================== 兼容原有字符串缓存的方法 ====================

    /**
     * 存入字符串缓存（永不过期）
     * @param key 缓存键
     * @param value 缓存值
     */
    public static void put(String key, String value) {
        if (key == null || value == null) {
            log.warn("缓存键或值不能为空");
            return;
        }
        cacheMap.put(key, value);
        log.debug("存入字符串缓存，key: {}, value: {}", key, value);
    }

    /**
     * 获取字符串缓存
     * @param key 缓存键
     * @return 缓存值，不存在返回null
     */
    public static String get(String key) {
        if (key == null) {
            return null;
        }
        String value = cacheMap.get(key);
        if (value != null) {
            HIT_COUNT.incrementAndGet();
            log.debug("获取字符串缓存命中，key: {}", key);
        } else {
            MISS_COUNT.incrementAndGet();
            log.debug("获取字符串缓存未命中，key: {}", key);
        }
        return value;
    }

    /**
     * 删除字符串缓存
     * @param key 缓存键
     * @return 被删除的值
     */
    public static String remove(String key) {
        if (key == null) {
            return null;
        }
        String removedValue = cacheMap.remove(key);
        log.debug("删除字符串缓存，key: {}, removedValue: {}", key, removedValue);
        return removedValue;
    }

    // ==================== 增强版缓存方法（支持任意类型+过期时间） ====================

    /**
     * 存入增强版缓存（永不过期）
     * @param key 缓存键
     * @param value 缓存值（支持任意类型）
     */
    public static <T> void putEnhanced(String key, T value) {
        putEnhanced(key, value, 0, TimeUnit.MILLISECONDS);
    }

    /**
     * 存入增强版缓存（带过期时间）
     * @param key 缓存键
     * @param value 缓存值（支持任意类型）
     * @param timeout 过期时间
     * @param timeUnit 时间单位
     */
    public static <T> void putEnhanced(String key, T value, long timeout, TimeUnit timeUnit) {
        if (key == null || value == null) {
            log.warn("增强版缓存键或值不能为空");
            return;
        }

        // 检查缓存容量限制
        if (MAX_CAPACITY > 0 && ENHANCED_CACHE.size() >= MAX_CAPACITY) {
            // 简单的缓存淘汰策略：清理所有过期缓存
            cleanExpiredCache();
            // 清理后仍满，则打印警告
            if (ENHANCED_CACHE.size() >= MAX_CAPACITY) {
                log.warn("缓存容量已达上限({})，无法存入新缓存，key: {}", MAX_CAPACITY, key);
                return;
            }
        }

        long expireTime = timeout <= 0 ? 0 : System.currentTimeMillis() + timeUnit.toMillis(timeout);
        ENHANCED_CACHE.put(key, new CacheEntry(value, expireTime));
        log.debug("存入增强版缓存，key: {}, value: {}, 过期时间: {}ms", key, value, timeout);
    }

    /**
     * 获取增强版缓存
     * @param key 缓存键
     * @param <T> 泛型类型
     * @return 缓存值，不存在或过期返回null
     */
    @SuppressWarnings("unchecked")
    public static <T> T getEnhanced(String key) {
        if (key == null) {
            return null;
        }

        CacheEntry entry = ENHANCED_CACHE.get(key);
        // 缓存不存在
        if (entry == null) {
            MISS_COUNT.incrementAndGet();
            log.debug("增强版缓存未命中，key: {}", key);
            return null;
        }

        // 缓存已过期
        if (entry.isExpired()) {
            ENHANCED_CACHE.remove(key);
            MISS_COUNT.incrementAndGet();
            log.debug("增强版缓存已过期，自动清理，key: {}", key);
            return null;
        }

        // 缓存命中
        HIT_COUNT.incrementAndGet();
        log.debug("增强版缓存命中，key: {}", key);
        return (T) entry.value;
    }

    /**
     * 删除增强版缓存
     * @param key 缓存键
     * @param <T> 泛型类型
     * @return 被删除的值
     */
    @SuppressWarnings("unchecked")
    public static <T> T removeEnhanced(String key) {
        if (key == null) {
            return null;
        }

        CacheEntry entry = ENHANCED_CACHE.remove(key);
        if (entry == null) {
            log.debug("增强版缓存删除失败，key不存在: {}", key);
            return null;
        }

        log.debug("删除增强版缓存，key: {}", key);
        return (T) entry.value;
    }

    // ==================== 缓存管理方法 ====================

    /**
     * 设置缓存最大容量
     * @param maxCapacity 最大容量，0表示不限制
     */
    public static void setMaxCapacity(long maxCapacity) {
        MAX_CAPACITY = maxCapacity;
        log.info("设置缓存最大容量为: {}", maxCapacity);
    }

    /**
     * 清理所有过期的增强版缓存
     * @return 清理的缓存数量
     */
    public static int cleanExpiredCache() {
        int cleanedCount = 0;
        for (Map.Entry<String, CacheEntry> entry : ENHANCED_CACHE.entrySet()) {
            if (entry.getValue().isExpired()) {
                ENHANCED_CACHE.remove(entry.getKey());
                cleanedCount++;
                log.debug("清理过期缓存，key: {}", entry.getKey());
            }
        }
        log.info("清理过期缓存完成，共清理{}条", cleanedCount);
        return cleanedCount;
    }

    /**
     * 清空所有字符串缓存
     */
    public static void clearStringCache() {
        cacheMap.clear();
        log.info("清空所有字符串缓存完成");
    }

    /**
     * 清空所有增强版缓存
     */
    public static void clearEnhancedCache() {
        ENHANCED_CACHE.clear();
        log.info("清空所有增强版缓存完成");
    }

    /**
     * 清空所有缓存（字符串+增强版）
     */
    public static void clearAllCache() {
        clearStringCache();
        clearEnhancedCache();
        log.info("清空所有缓存完成");
    }

    /**
     * 获取缓存统计信息
     * @return 统计信息Map
     */
    public static Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("stringCacheSize", cacheMap.size());
        stats.put("enhancedCacheSize", ENHANCED_CACHE.size());
        stats.put("hitCount", HIT_COUNT.get());
        stats.put("missCount", MISS_COUNT.get());
        stats.put("hitRate", HIT_COUNT.get() + MISS_COUNT.get() == 0
                ? 0.0
                : (double) HIT_COUNT.get() / (HIT_COUNT.get() + MISS_COUNT.get()));
        stats.put("maxCapacity", MAX_CAPACITY);
        return stats;
    }

    /**
     * 重置缓存统计信息
     */
    public static void resetCacheStats() {
        HIT_COUNT.set(0);
        MISS_COUNT.set(0);
        log.info("重置缓存统计信息完成");
    }
}