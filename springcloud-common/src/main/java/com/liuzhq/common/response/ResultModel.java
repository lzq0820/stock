package com.liuzhq.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.liuzhq.common.enums.ResponseCode;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

/**
 * ResultModel
 * @Description 统一返回类型（自动计算集合类型data的长度到count）
 * @Author liuzhq
 * @Date 2023/12/12 10:09
 * @Version 1.0
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResultModel<T> implements Serializable {

    private static final long serialVersionUID = 1L; // 序列化版本号，避免警告

    @ApiModelProperty(value = "返回码", example = "200")
    private Integer code;

    @ApiModelProperty(value = "返回信息", example = "成功")
    private String message;

    @ApiModelProperty(value = "返回数据")
    private T data;

    @ApiModelProperty(value = "是否成功")
    private Boolean isSuccess;

    @ApiModelProperty(value = "数据长度（集合类型自动计算，非集合为0）")
    private Integer count;

    // ==================== 私有构造器（外部通过静态方法创建） ====================
    /**
     * 基础构造器：仅初始化默认成功状态
     */
    private ResultModel() {
        this.code = ResponseCode.SUCCESS.getCode();
        this.message = ResponseCode.SUCCESS.getMessage();
        this.isSuccess = true;
        this.data = null;
        this.count = 0;
    }

    /**
     * 带数据的构造器（成功状态）
     */
    private ResultModel(T data) {
        this.code = ResponseCode.SUCCESS.getCode();
        this.message = ResponseCode.SUCCESS.getMessage();
        this.data = data;
        this.isSuccess = true;
        this.count = calculateCount(data); // 计算count
    }

    /**
     * 带消息的构造器（成功状态）
     */
    private ResultModel(String message) {
        this.code = ResponseCode.SUCCESS.getCode();
        this.message = message;
        this.isSuccess = true;
        this.data = null;
        this.count = 0;
    }

    /**
     * 全参构造器（支持成功/失败状态）
     */
    private ResultModel(Integer code, String message, T data, Boolean isSuccess) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.isSuccess = isSuccess;
        this.count = calculateCount(data); // 核心：自动计算count
    }

    /**
     * 核心方法：计算数据长度
     * @param data 待计算的数据
     * @return 集合长度（非集合返回0，null返回0）
     */
    private int calculateCount(T data) {
        if (data == null) {
            return 0;
        }
        // 1. 处理Collection类型（List/Set/Queue等）
        if (data instanceof Collection<?>) {
            return ((Collection<?>) data).size();
        }
        // 2. 处理Map类型
        if (data instanceof Map<?, ?>) {
            return ((Map<?, ?>) data).size();
        }
        // 3. 处理数组类型（可选扩展）
        if (data.getClass().isArray()) {
            return ((Object[]) data).length;
        }
        // 4. 非集合类型返回0
        return 0;
    }

    // ==================== 成功返回方法 ====================
    /**
     * 成功：无数据，默认消息
     */
    public static <T> ResultModel<T> success() {
        return new ResultModel<>();
    }

    /**
     * 成功：带数据，默认消息
     */
    public static <T> ResultModel<T> success(T data) {
        return new ResultModel<>(data);
    }

    /**
     * 成功：自定义码+消息，无数据
     */
    public static <T> ResultModel<T> successMsg(Integer code, String message) {
        return new ResultModel<>(code, message, null, true);
    }

    /**
     * 成功：自定义码+数据，默认消息
     */
    public static <T> ResultModel<T> success(T data, String message) {
        return new ResultModel<>(ResponseCode.SUCCESS.getCode(), message, data, true);
    }

    /**
     * 成功：自定义码+数据，默认消息
     */
    public static <T> ResultModel<T> success(Integer code, T data) {
        return new ResultModel<>(code, ResponseCode.SUCCESS.getMessage(), data, true);
    }

    /**
     * 成功：自定义码+数据+消息
     */
    public static <T> ResultModel<T> success(Integer code, T data, String message) {
        return new ResultModel<>(code, message, data, true);
    }

    // ==================== 失败返回方法 ====================
    public static <T> ResultModel<T> error() {
        return new ResultModel<>(
                ResponseCode.FAIL.getCode(),
                ResponseCode.FAIL.getMessage(),
                null,
                false
        );
    }

    /**
     * 失败：默认码+默认消息，无数据
     */
    public static <T> ResultModel<T> error(T data) {
        return new ResultModel<>(
                ResponseCode.FAIL.getCode(),
                ResponseCode.FAIL.getMessage(),
                data,
                false
        );
    }

    /**
     * 失败：默认码+自定义消息，无数据
     */
    public static <T> ResultModel<T> error(String message) {
        return new ResultModel<>(
                ResponseCode.FAIL.getCode(),
                message,
                null,
                false
        );
    }

    /**
     * 失败：自定义码+自定义消息，无数据
     */
    public static <T> ResultModel<T> error(Integer code, String message) {
        return new ResultModel<>(
                code,
                message,
                null,
                false
        );
    }

    public static <T> ResultModel<T> error(T data, String message) {
        return new ResultModel<>(
                ResponseCode.FAIL.getCode(),
                message,
                data,
                false
        );
    }

    /**
     * 扩展：失败带数据（可选，比如返回错误详情）
     */
    public static <T> ResultModel<T> error(Integer code, String message, T data) {
        return new ResultModel<>(code, message, data, false);
    }
}