package com.itdg.orchestrator.controller;

import com.itdg.common.dto.request.OrchestrationRequest;
import com.itdg.common.dto.response.ApiResponse;
import com.itdg.common.dto.response.GenerateDataResponse;
import com.itdg.orchestrator.service.OrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/orchestrator")
@RequiredArgsConstructor
public class OrchestratorController {

    private final OrchestrationService orchestrationService;

    @PostMapping("/process")
    public Mono<ApiResponse<GenerateDataResponse>> processRequest(@RequestBody OrchestrationRequest request) {
        log.info("Received orchestration request");
        return orchestrationService.processDataGeneration(request)
                .map(ApiResponse::success)
                .onErrorResume(error -> {
                    log.error("Orchestration request failed", error);
                    return Mono.just(ApiResponse.error("Orchestration failed: " + error.getMessage()));
                });
    }
}
