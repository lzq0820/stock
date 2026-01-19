package com.liuzhq.common.annotation;

import java.lang.annotation.*;

/**
 * 标记该注解的接口/方法，全局异常处理器不会自动返回空数据，会抛出原始异常
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SkipExceptionHandle {
    /**
     * 可选：指定需要跳过处理的异常类型（默认全部跳过）
     */
    Class<? extends Exception>[] value() default {};
}