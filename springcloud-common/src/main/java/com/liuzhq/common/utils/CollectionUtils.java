package com.liuzhq.common.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CollectionUtils
 * @Description 集合类工具
 * @Author liuzhq
 * @Date 2023/12/11 15:24
 * @Version 1.0
 */
public class CollectionUtils extends org.springframework.util.CollectionUtils {

    private static final Integer DEFAULT_INITIAL_CAPACITY = 16;

    /**
     * 返回线程安全的Map
     * @return
     */
    public static Map<String, Object> newCurrentHashMap() {
        return new ConcurrentHashMap<>(DEFAULT_INITIAL_CAPACITY);
    }
}
