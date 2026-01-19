package com.liuzhq.common.exception;

import com.liuzhq.common.response.ResultModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.servlet.http.HttpServletRequest;
import java.util.Objects;

/**
 * 全局异常处理器
 * @RestControllerAdvice：捕获所有@RestController的异常，返回JSON
 */
@Slf4j
@RestControllerAdvice // 等价于 @ControllerAdvice + @ResponseBody
public class GlobalExceptionHandler {

    /**
     * 处理自定义业务异常（优先级最高）
     */
    @ExceptionHandler(BusinessException.class)
    public ResultModel<?> handleBusinessException(BusinessException e, HttpServletRequest request) {
        // 打印异常日志（包含请求URL）
        log.error("业务异常 | URL: {} | 状态码: {} | 信息: {}",
                request.getRequestURI(), e.getCode(), e.getMessage());
        // 返回统一错误格式
        return ResultModel.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数校验异常（如@Valid注解验证失败）
     */
    @ExceptionHandler({BindException.class, MethodArgumentNotValidException.class})
    public ResultModel<?> handleParamException(Exception e, HttpServletRequest request) {
        String msg = "参数校验失败";
        // 提取具体的参数错误信息
        if (e instanceof BindException) {
            msg = Objects.requireNonNull(((BindException) e).getFieldError()).getDefaultMessage();
        } else if (e instanceof MethodArgumentNotValidException) {
            msg = e.getLocalizedMessage();
        }
        log.error("参数异常 | URL: {} | 信息: {}", request.getRequestURI(), msg);
        return ResultModel.error(400, msg);
    }

    /**
     * 处理参数类型不匹配异常（如传字符串给数字参数）
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResultModel<?> handleTypeMismatchException(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        assert e.getRequiredType() != null;
        String msg = String.format("参数类型错误：参数[%s]期望类型[%s]，实际值[%s]",
                e.getName(), e.getRequiredType().getSimpleName(), e.getValue());
        log.error("参数类型异常 | URL: {} | 信息: {}", request.getRequestURI(), msg);
        return ResultModel.error(400, msg);
    }

    /**
     * 处理所有未捕获的系统异常（兜底）
     */
    @ExceptionHandler(Exception.class)
    public ResultModel<?> handleSystemException(Exception e, HttpServletRequest request) {
        // 打印完整异常栈（便于排查问题）
        log.error("系统异常 | URL: {} | 信息: {}", request.getRequestURI(), e.getMessage(), e);
        // 返回统一错误提示（不暴露具体异常信息给前端）
        return ResultModel.error("系统异常，请稍后重试");
    }
}