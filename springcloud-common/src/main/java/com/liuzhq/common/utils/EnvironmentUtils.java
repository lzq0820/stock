package com.liuzhq.common.utils;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.Environment;

/**
 * EnvironmentUtils
 * @Description 涉及环境变量的工具类
 * @Author liuzhq
 * @Date 2023/12/11 19:10
 * @Version 1.0
 */
public class EnvironmentUtils {

    private static Environment environment;

    static {
        environment = SpringUtils.getEnvironment();
    }

    /**
     * 获取服务的端口号
     * @return
     */
    public static String getServerPort() {
        return StringUtils.defaultIfBlank(environment.getProperty("server.port"), "端口为空");
    }
}
