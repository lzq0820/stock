package com.liuzhq.common.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * SpringUtils
 * @Description Spring工具类
 * @Author liuzhq
 * @Date 2023/12/15 12:01
 * @Version 1.0
 */
@Component
public class SpringUtils implements EnvironmentAware, ApplicationContextAware {
    private static Environment environment;
    private static ApplicationContext applicationContext;
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringUtils.applicationContext = applicationContext;
    }

    @Override
    public void setEnvironment(Environment environment) {
        SpringUtils.environment = environment;
    }

    public static ApplicationContext getApplicationContext() {
        return SpringUtils.applicationContext;
    }

    public static Environment getEnvironment() {
        return SpringUtils.environment;
    }
}
