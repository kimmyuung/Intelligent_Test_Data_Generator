package com.itdg.generator.controller;

import com.itdg.common.dto.request.GenerateDataRequest;
import com.itdg.common.dto.response.GenerateDataResponse;
import com.itdg.generator.service.DataGeneratorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/generator")
@RequiredArgsConstructor
@Tag(name = "Generator", description = "패턴 기반 테스트 데이터 생성 API")
public class GeneratorController {

    private final DataGeneratorService dataGeneratorService;

    @Operation(summary = "테스트 데이터 생성", description = "스키마 메타데이터를 기반으로 패턴 기반 테스트 데이터를 생성합니다.\n\n" +
            "지원 타입: NAME, EMAIL, PHONE, ADDRESS, DATE, NUMBER, BOOLEAN, URL, UUID")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "데이터 생성 성공", content = @Content(schema = @Schema(implementation = GenerateDataResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "데이터 생성 실패")
    })
    @PostMapping("/generate")
    public ResponseEntity<GenerateDataResponse> generateData(@RequestBody GenerateDataRequest request) {
        log.info("Received data generation request");
        try {
            GenerateDataResponse response = dataGeneratorService.generateData(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error generating data", e);
            return ResponseEntity.internalServerError().body(GenerateDataResponse.builder()
                    .success(false)
                    .message("Failed to generate data: " + e.getMessage())
                    .build());
        }
    }
}
