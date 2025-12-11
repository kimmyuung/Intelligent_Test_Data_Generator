package com.itdg.analyzer.controller;

import com.itdg.analyzer.dto.request.DbConnectionRequest;
import com.itdg.analyzer.service.SchemaAnalyzerService;
import com.itdg.common.dto.metadata.SchemaMetadata;
import com.itdg.common.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/analyze")
@RequiredArgsConstructor
public class AnalyzerController {

    private final SchemaAnalyzerService schemaAnalyzerService;

    @PostMapping
    public ApiResponse<SchemaMetadata> analyzeSchema(@RequestBody DbConnectionRequest request) {
        log.info("Received analysis request for URL: {}", request.getUrl());
        return schemaAnalyzerService.analyze(request);
    }
}
