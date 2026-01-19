package com.liuzhq.common.enums;

import com.liuzhq.common.annotation.Description;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Description("缓存key")
@Slf4j
@Getter
public enum CacheKey {
    HOLIDAY("holiday:", "节假日"),

    ;

    private final String key;
    private final String desc;

    CacheKey(String key, String desc) {
        this.key = key;
        this.desc = desc;
    }
}
