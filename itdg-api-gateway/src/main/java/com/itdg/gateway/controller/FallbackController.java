package com.itdg.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Circuit Breaker Fallback 컨트롤러
 * 
 * 서비스 장애 시 사용자 친화적 응답 제공
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/orchestrator")
    public Mono<Map<String, Object>> orchestratorFallback() {
        return Mono.just(createFallbackResponse(
                "Orchestrator",
                "워크플로우 조정 서비스가 일시적으로 사용 불가합니다. 잠시 후 다시 시도해주세요."));
    }

    @GetMapping("/analyzer")
    public Mono<Map<String, Object>> analyzerFallback() {
        return Mono.just(createFallbackResponse(
                "Analyzer",
                "스키마 분석 서비스가 일시적으로 사용 불가합니다. 잠시 후 다시 시도해주세요."));
    }

    @GetMapping("/generator")
    public Mono<Map<String, Object>> generatorFallback() {
        return Mono.just(createFallbackResponse(
                "Generator",
                "데이터 생성 서비스가 일시적으로 사용 불가합니다. 잠시 후 다시 시도해주세요."));
    }

    @GetMapping("/ml")
    public Mono<Map<String, Object>> mlFallback() {
        return Mono.just(createFallbackResponse(
                "ML Server",
                "ML 합성 데이터 서비스가 일시적으로 사용 불가합니다. 잠시 후 다시 시도해주세요."));
    }

    private Map<String, Object> createFallbackResponse(String service, String message) {
        return Map.of(
                "success", false,
                "error", Map.of(
                        "status", HttpStatus.SERVICE_UNAVAILABLE.value(),
                        "code", "SVC_001",
                        "service", service,
                        "message", message,
                        "timestamp", LocalDateTime.now().toString()));
    }
}
