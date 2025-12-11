package com.itdg.orchestrator.controller;

import com.itdg.common.dto.response.ApiResponse;
import com.itdg.common.dto.response.HealthCheckResponse;
import com.itdg.orchestrator.service.ServiceCommunicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class ServiceTestController {

    private final ServiceCommunicationService serviceCommunicationService;

    /**
     * Analyzer 서비스 통신 테스트
     */
    @GetMapping("/analyzer")
    public Mono<ApiResponse<HealthCheckResponse>> testAnalyzer() {
        log.info("Testing communication with Analyzer service");
        return serviceCommunicationService.checkAnalyzerHealth()
                .onErrorResume(error -> {
                    log.error("Failed to communicate with Analyzer", error);
                    return Mono.just(ApiResponse.error("Failed to communicate with Analyzer: " + error.getMessage()));
                });
    }

    /**
     * Generator 서비스 통신 테스트
     */
    @GetMapping("/generator")
    public Mono<ApiResponse<HealthCheckResponse>> testGenerator() {
        log.info("Testing communication with Generator service");
        return serviceCommunicationService.checkGeneratorHealth()
                .onErrorResume(error -> {
                    log.error("Failed to communicate with Generator", error);
                    return Mono.just(ApiResponse.error("Failed to communicate with Generator: " + error.getMessage()));
                });
    }

    /**
     * 모든 서비스 통신 테스트
     */
    @GetMapping("/all")
    public Mono<ApiResponse<Map<String, Object>>> testAllServices() {
        log.info("Testing communication with all services");
        return serviceCommunicationService.checkAllServicesHealth()
                .map(result -> ApiResponse.success("All services communication test completed", result))
                .onErrorResume(error -> {
                    log.error("Failed to communicate with services", error);
                    return Mono.just(ApiResponse.error("Failed to communicate with services: " + error.getMessage()));
                });
    }
}
