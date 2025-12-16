package com.itdg.analyzer.controller;

import com.itdg.common.dto.request.DbConnectionRequest;
import com.itdg.analyzer.service.SchemaAnalyzerService;
import com.itdg.common.dto.metadata.SchemaMetadata;
import com.itdg.common.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/analyze")
@RequiredArgsConstructor
@CrossOrigin("*")
public class AnalyzerController {

    private final SchemaAnalyzerService schemaAnalyzerService;
    private final com.itdg.analyzer.service.SourceHelperService sourceHelperService;
    private final com.itdg.analyzer.service.ProjectAnalysisService projectAnalysisService;

    /*
     * @PostMapping
     * public ApiResponse<SchemaMetadata> analyzeSchema(@RequestBody
     * DbConnectionRequest request) {
     * log.info("Received analysis request for URL: {}", request.getUrl());
     * return schemaAnalyzerService.analyze(request);
     * }
     */

    @PostMapping("/git")
    public ApiResponse<SchemaMetadata> analyzeGitRepository(@RequestBody java.util.Map<String, String> payload) {
        String url = payload.get("url");
        log.info("Received git analysis request for URL: {}", url);
        java.io.File tempDir = null;
        try {
            tempDir = sourceHelperService.cloneRepository(url);
            SchemaMetadata metadata = projectAnalysisService.analyzeProject(tempDir);
            return ApiResponse.success(metadata);
        } catch (Exception e) {
            log.error("Git analysis failed", e);
            return ApiResponse.error(e.getMessage());
        } finally {
            sourceHelperService.cleanup(tempDir);
        }
    }

    @PostMapping("/upload")
    public ApiResponse<SchemaMetadata> analyzeUploadFile(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        log.info("Received file upload analysis request: {}", file.getOriginalFilename());
        java.io.File tempDir = null;
        try {
            tempDir = sourceHelperService.extractZipFile(file);
            SchemaMetadata metadata = projectAnalysisService.analyzeProject(tempDir);
            return ApiResponse.success(metadata);
        } catch (Exception e) {
            log.error("File analysis failed", e);
            return ApiResponse.error(e.getMessage());
        } finally {
            sourceHelperService.cleanup(tempDir);
        }
    }
}
