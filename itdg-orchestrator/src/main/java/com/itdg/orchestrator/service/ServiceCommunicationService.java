package com.itdg.orchestrator.service;

import com.itdg.common.dto.response.ApiResponse;
import com.itdg.common.dto.response.HealthCheckResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceCommunicationService {

    @Qualifier("analyzerWebClient")
    private final WebClient analyzerWebClient;

    @Qualifier("generatorWebClient")
    private final WebClient generatorWebClient;

    /**
     * Analyzer 서비스의 헬스체크 호출
     */
    public Mono<ApiResponse<HealthCheckResponse>> checkAnalyzerHealth() {
        log.info("Calling Analyzer health check");
        return analyzerWebClient.get()
                .uri("/api/health")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<HealthCheckResponse>>() {
                })
                .doOnSuccess(response -> log.info("Analyzer health check successful: {}", response))
                .doOnError(error -> log.error("Analyzer health check failed", error));
    }

    /**
     * Generator 서비스의 헬스체크 호출
     */
    public Mono<ApiResponse<HealthCheckResponse>> checkGeneratorHealth() {
        log.info("Calling Generator health check");
        return generatorWebClient.get()
                .uri("/api/health")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<HealthCheckResponse>>() {
                })
                .doOnSuccess(response -> log.info("Generator health check successful: {}", response))
                .doOnError(error -> log.error("Generator health check failed", error));
    }

    /**
     * 모든 서비스의 헬스체크를 동시에 호출
     */
    public Mono<Map<String, Object>> checkAllServicesHealth() {
        log.info("Checking all services health");

        Mono<ApiResponse<HealthCheckResponse>> analyzerHealth = checkAnalyzerHealth()
                .onErrorResume(error -> {
                    log.error("Analyzer service is down", error);
                    return Mono.just(ApiResponse.error("Analyzer service is unavailable"));
                });

        Mono<ApiResponse<HealthCheckResponse>> generatorHealth = checkGeneratorHealth()
                .onErrorResume(error -> {
                    log.error("Generator service is down", error);
                    return Mono.just(ApiResponse.error("Generator service is unavailable"));
                });

        return Mono.zip(analyzerHealth, generatorHealth)
                .map(tuple -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("analyzer", tuple.getT1());
                    result.put("generator", tuple.getT2());
                    return result;
                });
    }
}
