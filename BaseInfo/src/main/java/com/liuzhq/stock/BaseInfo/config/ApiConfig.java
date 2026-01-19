package com.liuzhq.stock.BaseInfo.config;

import lombok.Data;

/**
 * API配置实体
 */
@Data
public class ApiConfig {
    private String name;      // API名称
    private String method;    // HTTP方法
    private String path;      // API路径
    private String description; // 描述
    private String provider;  // 提供商(eastmoney/xuangubao)
    private String category;  // 分类
    private int timeout;      // 超时时间
    private boolean enabled;  // 是否启用
}
