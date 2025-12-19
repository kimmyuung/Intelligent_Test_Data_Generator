package com.itdg.orchestrator.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * API 로깅 AOP
 * 
 * 모든 Controller 메서드의 요청/응답을 자동 로깅
 */
@Aspect
@Component
@Slf4j
public class ApiLoggingAspect {

    private static final org.slf4j.Logger API_LOGGER = LoggerFactory.getLogger("API_LOGGER");

    /**
     * Controller 메서드 포인트컷
     */
    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void restControllerMethods() {
    }

    /**
     * API 요청/응답 로깅
     */
    @Around("restControllerMethods()")
    public Object logApiCall(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        String methodName = method.getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String httpMethod = extractHttpMethod(method);

        // 요청 로깅
        Object[] args = joinPoint.getArgs();
        log.debug(">>> {}.{} - Args: {}", className, methodName, summarizeArgs(args));

        Object result = null;
        String status = "SUCCESS";

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable ex) {
            status = "FAILED";
            log.error("<<< {}.{} - Exception: {}", className, methodName, ex.getMessage());
            throw ex;
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            // MDC에 상태 및 duration 추가
            MDC.put("status", status);
            MDC.put("duration", String.valueOf(duration));

            // API 로거에 기록
            API_LOGGER.info("{} {} - {} {}ms",
                    httpMethod,
                    MDC.get("uri"),
                    status,
                    duration);

            // 상세 응답 로깅 (DEBUG 레벨)
            log.debug("<<< {}.{} - Result: {} ({}ms)",
                    className, methodName, summarizeResult(result), duration);

            // 느린 API 경고 (1초 이상)
            if (duration > 1000) {
                log.warn("SLOW API: {}.{} took {}ms", className, methodName, duration);
            }
        }
    }

    private String extractHttpMethod(Method method) {
        if (method.isAnnotationPresent(GetMapping.class))
            return "GET";
        if (method.isAnnotationPresent(PostMapping.class))
            return "POST";
        if (method.isAnnotationPresent(PutMapping.class))
            return "PUT";
        if (method.isAnnotationPresent(DeleteMapping.class))
            return "DELETE";
        if (method.isAnnotationPresent(PatchMapping.class))
            return "PATCH";
        return "UNKNOWN";
    }

    private String summarizeArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        // 대용량 데이터는 요약만
        return Arrays.stream(args)
                .map(arg -> {
                    if (arg == null)
                        return "null";
                    String str = arg.toString();
                    return str.length() > 100 ? str.substring(0, 100) + "..." : str;
                })
                .toList()
                .toString();
    }

    private String summarizeResult(Object result) {
        if (result == null)
            return "null";
        String str = result.toString();
        return str.length() > 200 ? str.substring(0, 200) + "..." : str;
    }
}
