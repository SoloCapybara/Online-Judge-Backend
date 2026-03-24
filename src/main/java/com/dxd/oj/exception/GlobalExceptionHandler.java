package com.dxd.oj.exception;

import com.dxd.oj.common.BaseResponse;
import com.dxd.oj.common.ErrorCode;
import com.dxd.oj.common.ResultUtils;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<Object> handleBusinessException(BusinessException e) {
        log.warn("business exception: code={}, message={}", e.getCode(), e.getMessage());
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public BaseResponse<Object> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse(ErrorCode.PARAMS_ERROR.getMessage());
        return ResultUtils.error(ErrorCode.PARAMS_ERROR.getCode(), message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public BaseResponse<Object> handleConstraintViolationException(ConstraintViolationException e) {
        return ResultUtils.error(ErrorCode.PARAMS_ERROR.getCode(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public BaseResponse<Object> handleException(Exception e) {
        log.error("system exception", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR);
    }
}
