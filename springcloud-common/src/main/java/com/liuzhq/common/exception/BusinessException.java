package com.liuzhq.common.exception;

import lombok.Getter;

/**
 * 自定义业务异常（用于业务层主动抛出）
 */
@Getter // 提供code和msg的getter
public class BusinessException extends RuntimeException {
    // 异常状态码
    private final Integer code;

    /**
     * 构造：自定义提示
     */
    public BusinessException(String message) {
        super(message);
        this.code = 400; // 业务异常默认400
    }

    /**
     * 构造：自定义状态码+提示
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}