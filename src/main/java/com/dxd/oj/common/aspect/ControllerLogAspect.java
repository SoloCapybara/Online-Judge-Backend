package com.dxd.oj.common.aspect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ControllerLogAspect {

    private static final int MAX_LOG_TEXT_LEN = 1200;

    private final ObjectMapper objectMapper;

    @Around("execution(* com.dxd.oj.controller..*(..))")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        String path = "";
        String method = "";
        String ip = "";
        if (attributes != null && attributes.getRequest() != null) {
            method = attributes.getRequest().getMethod();
            path = attributes.getRequest().getRequestURI();
            ip = getClientIp(attributes);
        }

        String classMethod = joinPoint.getSignature().toShortString();
        String argsText = truncate(toJsonSafe(filterArgs(joinPoint.getArgs())));
        log.info("接口请求开始：method={}, path={}, ip={}, handler={}, args={}", method, path, ip, classMethod, argsText);

        try {
            Object result = joinPoint.proceed();
            long costMs = System.currentTimeMillis() - startTime;
            String resultText = truncate(toJsonSafe(result));
            log.info("接口请求结束：method={}, path={}, handler={}, costMs={}, result={}", method, path, classMethod, costMs, resultText);
            return result;
        } catch (Exception ex) {
            long costMs = System.currentTimeMillis() - startTime;
            log.error("接口请求异常：method={}, path={}, handler={}, costMs={}, error={}",
                    method, path, classMethod, costMs, ex.getMessage(), ex);
            throw ex;
        }
    }

    private Map<String, Object> filterArgs(Object[] args) {
        Map<String, Object> argMap = new LinkedHashMap<>();
        if (args == null || args.length == 0) {
            return argMap;
        }
        int index = 0;
        for (Object arg : args) {
            if (shouldIgnoreArg(arg)) {
                continue;
            }
            argMap.put("arg" + index, arg);
            index++;
        }
        return argMap;
    }

    private boolean shouldIgnoreArg(Object arg) {
        if (arg == null) {
            return false;
        }
        if (arg instanceof ServletRequest || arg instanceof ServletResponse || arg instanceof MultipartFile) {
            return true;
        }
        if (arg instanceof MultipartFile[]) {
            return true;
        }
        if (arg instanceof byte[]) {
            return true;
        }
        if (arg instanceof Object[] objectArray) {
            return Arrays.stream(objectArray).allMatch(item -> item instanceof MultipartFile);
        }
        return false;
    }

    private String toJsonSafe(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return String.valueOf(obj);
        }
    }

    private String truncate(String text) {
        if (text == null) {
            return "null";
        }
        if (text.length() <= MAX_LOG_TEXT_LEN) {
            return text;
        }
        return text.substring(0, MAX_LOG_TEXT_LEN) + "...(truncated)";
    }

    private String getClientIp(ServletRequestAttributes attributes) {
        String xForwardedFor = attributes.getRequest().getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return Arrays.stream(xForwardedFor.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .findFirst()
                    .orElse(xForwardedFor);
        }
        String xRealIp = attributes.getRequest().getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        String remoteAddr = attributes.getRequest().getRemoteAddr();
        if ("0:0:0:0:0:0:0:1".equals(remoteAddr) || "::1".equals(remoteAddr)) {
            return "127.0.0.1";
        }
        return remoteAddr;
    }
}
