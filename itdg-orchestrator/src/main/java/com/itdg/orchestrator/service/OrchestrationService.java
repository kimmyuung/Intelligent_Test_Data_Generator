package com.itdg.orchestrator.service;

import com.itdg.common.dto.metadata.SchemaMetadata;
import com.itdg.common.dto.request.GenerateDataRequest;
import com.itdg.common.dto.request.OrchestrationRequest;
import com.itdg.common.dto.response.ApiResponse;
import com.itdg.common.dto.response.GenerateDataResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrchestrationService {

    private final ServiceCommunicationService communicationService;

    public Mono<GenerateDataResponse> processDataGeneration(OrchestrationRequest request) {
        log.info("Starting orchestration process for URL: {}", request.getDbConnection().getUrl());

        // 1. Call Analyzer to get Schema
        return communicationService.extractSchema(request.getDbConnection())
                .flatMap(schemaResponse -> {
                    if (!schemaResponse.isSuccess() || schemaResponse.getData() == null) {
                        return Mono.error(
                                new RuntimeException("Failed to extract schema: " + schemaResponse.getMessage()));
                    }

                    SchemaMetadata schema = schemaResponse.getData();
                    log.info("Schema extracted successfully for database: {}", schema.getDatabaseName());

                    // 2. Prepare GenerateDataRequest
                    GenerateDataRequest generateRequest = GenerateDataRequest.builder()
                            .schema(schema)
                            .rowCount(request.getRowCount())
                            .seed(request.getSeed())
                            .build();

                    // 3. Call Generator to generate data
                    return communicationService.generateData(generateRequest);
                })
                .doOnSuccess(response -> log.info("Orchestration completed successfully"))
                .doOnError(error -> log.error("Orchestration failed", error));
    }
}
