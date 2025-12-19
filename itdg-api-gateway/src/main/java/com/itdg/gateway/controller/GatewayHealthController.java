package com.itdg.gateway.controller;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 게이트웨이 헬스체크 컨트롤러
 */
@RestController
@RequestMapping("/gateway")
public class GatewayHealthController {

    @Autowired(required = false)
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @GetMapping("/health")
    public Mono<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "api-gateway");
        response.put("timestamp", LocalDateTime.now().toString());

        return Mono.just(response);
    }

    @GetMapping("/status")
    public Mono<Map<String, Object>> status() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "api-gateway");
        response.put("timestamp", LocalDateTime.now().toString());

        // Circuit Breaker 상태
        if (circuitBreakerRegistry != null) {
            Map<String, Object> circuitBreakers = new HashMap<>();
            circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb -> {
                circuitBreakers.put(cb.getName(), Map.of(
                        "state", cb.getState().toString(),
                        "failureRate", cb.getMetrics().getFailureRate(),
                        "bufferedCalls", cb.getMetrics().getNumberOfBufferedCalls()));
            });
            response.put("circuitBreakers", circuitBreakers);
        }

        return Mono.just(response);
    }

    @GetMapping("/routes")
    public Mono<Map<String, Object>> routes() {
        return Mono.just(Map.of(
                "routes", Map.of(
                        "/api/orchestrator/**", "Orchestrator Service (8080)",
                        "/api/orchestrator/stream/**", "Orchestrator Streaming (5min timeout)",
                        "/api/ml/**", "ML API (via Orchestrator proxy)",
                        "/api/analyze/**", "Analyzer Service (8081)",
                        "/api/generator/**", "Generator Service (8082)"),
                "features", Map.of(
                        "rateLimiting", "100 req/s per IP",
                        "circuitBreaker", "Enabled",
                        "loadBalancing", "Round-robin")));
    }
}
