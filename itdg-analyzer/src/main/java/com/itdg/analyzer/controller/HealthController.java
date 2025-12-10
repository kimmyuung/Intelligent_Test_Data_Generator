package com.itdg.analyzer.controller;

import com.itdg.common.dto.response.ApiResponse;
import com.itdg.common.dto.response.HealthCheckResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Value("${spring.application.name}")
    private String serviceName;

    @GetMapping
    public ApiResponse healthCheck() {
        log.info("Health check requested for {}", serviceName);

        HealthCheckResponse response = HealthCheckResponse.builder()
                .serviceName(serviceName)
                .status("UP")
                .version("1.0.0")
                .message("Analyzer service is running")
                .build();

        return ApiResponse.success(response);
    }
}