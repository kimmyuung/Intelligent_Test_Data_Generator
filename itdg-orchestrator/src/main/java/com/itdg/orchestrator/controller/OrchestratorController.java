package com.itdg.orchestrator.controller;

import com.itdg.common.dto.request.OrchestrationRequest;
import com.itdg.common.dto.response.ApiResponse;
import com.itdg.common.dto.response.GenerateDataResponse;
import com.itdg.orchestrator.service.OrchestrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@org.springframework.web.bind.annotation.CrossOrigin(origins = "*")
@Tag(name = "Orchestrator", description = "데이터 생성 워크플로우 조율 API")
public class OrchestratorController {

    private final OrchestrationService orchestrationService;

    @Operation(summary = "데이터 생성 요청 처리", description = "DB 분석 후 데이터 생성을 수행합니다. Analyzer → Generator 순으로 호출됩니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "데이터 생성 성공", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
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

    @Operation(summary = "스키마 기반 데이터 생성", description = "이미 분석된 스키마 메타데이터를 기반으로 데이터를 생성합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "데이터 생성 성공", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/process-metadata")
    public Mono<ApiResponse<GenerateDataResponse>> processMetadataRequest(
            @RequestBody com.itdg.common.dto.request.SchemaBasedOrchestrationRequest request) {
        log.info("Received schema-based orchestration request");
        return orchestrationService.processSchemaDataGeneration(request)
                .map(ApiResponse::success)
                .onErrorResume(error -> {
                    log.error("Schema orchestration request failed", error);
                    return Mono.just(ApiResponse.error("Generation failed: " + error.getMessage()));
                });
    }
}
