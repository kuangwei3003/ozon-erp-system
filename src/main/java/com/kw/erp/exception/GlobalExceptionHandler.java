package com.kw.erp.exception;

import com.kw.erp.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * REST 接口全局异常处理：统一返回 Result JSON
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.kw.erp.controller.api")
public class GlobalExceptionHandler {

    @ExceptionHandler(OzonApiException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public Result<Void> handleOzonApi(OzonApiException e, HttpServletRequest request) {
        log.error("Ozon API 调用失败 uri={} : {}", request.getRequestURI(), e.getMessage());
        return Result.fail(502, "Ozon接口错误: " + e.getMessage());
    }

    @ExceptionHandler(DomainException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleDomain(DomainException e, HttpServletRequest request) {
        log.warn("业务异常 uri={} : {}", request.getRequestURI(), e.getMessage());
        return Result.fail(400, e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class,
            MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidation(Exception e) {
        String msg = switch (e) {
            case BindException be -> be.getBindingResult().getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .findFirst().orElse("参数校验失败");
            case MissingServletRequestParameterException pe -> "缺少必填参数：" + pe.getParameterName();
            case MethodArgumentTypeMismatchException te -> "参数类型不正确：" + te.getName();
            default -> "参数校验失败";
        };
        return Result.fail(400, msg);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleOther(Exception e, HttpServletRequest request) {
        log.error("系统异常 uri={}", request.getRequestURI(), e);
        return Result.fail(500, "系统内部错误: " + e.getMessage());
    }
}
