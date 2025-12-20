package com.itdg.analyzer.controller;

import com.itdg.common.dto.request.DbConnectionRequest;
import com.itdg.analyzer.service.SchemaAnalyzerService;
import com.itdg.common.dto.metadata.SchemaMetadata;
import com.itdg.common.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Analyzer", description = "데이터베이스/프로젝트 스키마 분석 API")
public class AnalyzerController {

    private final SchemaAnalyzerService schemaAnalyzerService;
    private final com.itdg.analyzer.service.SourceHelperService sourceHelperService;
    private final com.itdg.analyzer.service.ProjectAnalysisService projectAnalysisService;

    private final com.itdg.analyzer.service.EncryptionService encryptionService;

    @Operation(summary = "RSA 공개키 조회", description = "비밀번호 암호화에 사용할 RSA 공개키를 반환합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "공개키 반환 성공")
    @org.springframework.web.bind.annotation.GetMapping("/public-key")
    public ApiResponse<String> getPublicKey() {
        return ApiResponse.success(encryptionService.getPublicKey());
    }

    @Operation(summary = "데이터베이스 스키마 분석", description = "JDBC 연결 정보를 사용하여 데이터베이스 스키마를 분석합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "스키마 분석 성공", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "데이터베이스 연결 실패")
    })
    @PostMapping
    public ApiResponse<SchemaMetadata> analyzeSchema(
            @RequestBody @jakarta.validation.Valid DbConnectionRequest request) {
        log.info("Received analysis request for URL: {}", request.getUrl());

        // Decrypt password
        String decryptedPassword = encryptionService.decrypt(request.getPassword());

        // Create new request object with decrypted password (or modify if setter
        // exists)
        DbConnectionRequest decryptedRequest = new DbConnectionRequest();
        decryptedRequest.setUrl(request.getUrl());
        decryptedRequest.setUsername(request.getUsername());
        decryptedRequest.setPassword(decryptedPassword);
        decryptedRequest.setDriverClassName(request.getDriverClassName());

        return schemaAnalyzerService.analyze(decryptedRequest);
    }

    @Operation(summary = "Git 리포지토리 분석", description = "Git 리포지토리 URL을 클론하여 JPA Entity, SQL DDL 등을 파싱해 스키마를 추출합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "리포지토리 분석 성공", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "지원하지 않는 프로젝트 타입"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Git 클론 또는 분석 실패")
    })
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

    @Operation(summary = "ZIP 파일 업로드 분석", description = "프로젝트 소스코드가 포함된 ZIP 파일을 업로드하여 스키마를 분석합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "파일 분석 성공", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 파일 형식"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "파일 처리 실패")
    })
    @PostMapping("/upload")
    public ApiResponse<SchemaMetadata> analyzeUploadFile(
            @Parameter(description = "분석할 프로젝트 ZIP 파일") @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
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
