package com.liuzhq.common.enums;

import lombok.Getter;
import lombok.Setter;

/**
 * ResponseCode
 * @Description 统一响应编码
 * @Author liuzhq
 * @Date 2023/12/12 10:12
 * @Version 1.0
 */
@Getter
public enum ResponseCode {

    SUCCESS(200, "成功"),
    FAIL(500, "失败"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),
    REQUEST_TIMEOUT(408, "请求超时"),

    ;

    private Integer code;
    private String message;

    ResponseCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public static ResponseCode getResponseCode(Integer code) {
        for (ResponseCode responseCode : ResponseCode.values()) {
            if (responseCode.getCode().equals(code)) {
                return responseCode;
            }
        }
        return null;
    }

    public static String getResponseMsg(Integer code) {
        for (ResponseCode responseCode : ResponseCode.values()) {
            if (responseCode.getCode().equals(code)) {
                return responseCode.getMessage();
            }
        }
        return null;
    }

    public static String setDefaultErrorMsg(String msg) {

        // todo
        return null;
    }
}
