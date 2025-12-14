package com.itdg.generator.controller;

import com.itdg.common.dto.request.GenerateDataRequest;
import com.itdg.common.dto.response.GenerateDataResponse;
import com.itdg.generator.service.DataGeneratorService;
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
public class GeneratorController {

    private final DataGeneratorService dataGeneratorService;

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
